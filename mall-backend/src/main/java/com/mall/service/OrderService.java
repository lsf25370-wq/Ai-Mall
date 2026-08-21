package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.common.BusinessException;
import com.mall.dto.OrderCreateRequest;
import com.mall.entity.Address;
import com.mall.entity.CartItem;
import com.mall.entity.Coupon;
import com.mall.entity.Order;
import com.mall.entity.OrderItem;
import com.mall.mapper.AddressMapper;
import com.mall.mapper.CartItemMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单服务：下单 / 支付 / 取消 / 退款 / 查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final CartItemMapper cartItemMapper;
    private final AddressMapper addressMapper;
    private final ProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheService cacheService;
    private final CouponService couponService;
    private final PointsService pointsService;

    /** 待付款订单超时集合（ZSet，score 为过期时间戳） */
    private static final String ORDER_TIMEOUT_ZSET = "mall:order:timeout";
    /** 防重复下单分布式锁前缀 */
    private static final String ORDER_LOCK_PREFIX = "mall:lock:order:";

    @Value("${mall.order.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    @Value("${mall.order.auto-confirm-days:7}")
    private long autoConfirmDays;

    /**
     * 提交订单：事务保证一致性，库存原子扣减防止超卖，Redis 分布式锁防止重复提交
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> create(Long userId, OrderCreateRequest req) {
        // 0. 分布式锁防重复提交（SETNX + TTL，同一用户并发下单只放行一次）
        String lockKey = ORDER_LOCK_PREFIX + userId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(10));
        if (locked == null || !locked) {
            throw new BusinessException("正在处理您的订单，请勿重复提交");
        }
        try {
            return doCreate(userId, req);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private Map<String, Object> doCreate(Long userId, OrderCreateRequest req) {
        Address address = addressMapper.selectById(req.getAddressId());
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("收货地址不存在");
        }

        // 1. 组装待下单的购物车项（指定项或全部选中项）
        List<CartItem> items;
        if (req.getCartItemIds() != null && !req.getCartItemIds().isEmpty()) {
            items = cartItemMapper.selectBatchIds(req.getCartItemIds()).stream()
                    .filter(i -> i.getUserId().equals(userId) && i.getChecked() == 1)
                    .collect(Collectors.toList());
        } else {
            items = cartItemMapper.selectList(new LambdaQueryWrapper<CartItem>()
                    .eq(CartItem::getUserId, userId)
                    .eq(CartItem::getChecked, 1));
        }
        if (items.isEmpty()) {
            throw new BusinessException("请先选择要购买的商品");
        }

        // 2. 原子扣减库存（WHERE stock >= quantity，防超卖）
        for (CartItem item : items) {
            int rows = productMapper.update(null, new UpdateWrapper<com.mall.entity.Product>()
                    .eq("id", item.getProductId())
                    .ge("stock", item.getQuantity())
                    .setSql("stock = stock - " + item.getQuantity())
                    .setSql("sales = sales + " + item.getQuantity()));
            if (rows == 0) {
                throw new BusinessException("商品【" + item.getProductName() + "】库存不足");
            }
            cacheService.evictProduct(item.getProductId());
        }

        // 3. 多店铺拆单：按商品所属店铺分组，每个店铺生成一个独立订单
        Map<Long, List<CartItem>> shopGroups = new HashMap<>();
        for (CartItem item : items) {
            com.mall.entity.Product product = productMapper.selectById(item.getProductId());
            Long shopId = product != null && product.getShopId() != null ? product.getShopId() : 0L;
            shopGroups.computeIfAbsent(shopId, k -> new ArrayList<>()).add(item);
        }

        // 3.1 校验优惠券（可选），并确定其适用的店铺
        Coupon coupon = null;
        Long couponShopId = null;
        if (req.getUserCouponId() != null) {
            coupon = couponService.checkUsable(userId, req.getUserCouponId());
            couponShopId = coupon.getShopId(); // NULL=全场通用
            if (couponShopId != null && !shopGroups.containsKey(couponShopId)) {
                throw new BusinessException("该优惠券仅限指定店铺商品使用");
            }
        }
        Long couponTargetShop = couponShopId != null ? couponShopId : shopGroups.keySet().iterator().next();
        BigDecimal couponDiscount = BigDecimal.ZERO;

        String addressSnapshot = address.getReceiver() + " " + address.getPhone() + " "
                + address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail();

        List<Map<String, Object>> orders = new ArrayList<>();
        int index = 0;
        long baseTime = System.currentTimeMillis();
        for (Map.Entry<Long, List<CartItem>> entry : shopGroups.entrySet()) {
            Long shopId = entry.getKey();
            List<CartItem> shopItems = entry.getValue();

            // 订单号：ORD + 时间戳 + 序号（保证一单多店的唯一性）
            String orderNo = "ORD" + (baseTime + index);
            BigDecimal total = shopItems.stream()
                    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 优惠券应用到目标店铺子订单
            BigDecimal discount = BigDecimal.ZERO;
            if (coupon != null && shopId.equals(couponTargetShop)) {
                discount = calcCouponDiscount(coupon, total);
                couponDiscount = discount;
            }

            Order order = new Order();
            order.setOrderNo(orderNo);
            order.setUserId(userId);
            order.setShopId(shopId);
            order.setTotalAmount(total);
            order.setDiscountAmount(discount);
            order.setPayAmount(total.subtract(discount));
            order.setPointsEarned(total.subtract(discount).intValue());
            order.setStatus(0);
            order.setAddressSnapshot(addressSnapshot);
            orderMapper.insert(order);

            if (coupon != null && shopId.equals(couponTargetShop) && discount.compareTo(BigDecimal.ZERO) > 0) {
                order.setCouponId(coupon.getId());
                orderMapper.updateById(order);
                couponService.markUsed(req.getUserCouponId(), order.getId());
            }

            for (CartItem item : shopItems) {
                OrderItem oi = new OrderItem();
                oi.setOrderId(order.getId());
                oi.setOrderNo(orderNo);
                oi.setProductId(item.getProductId());
                oi.setProductName(item.getProductName());
                oi.setProductImage(item.getProductImage());
                oi.setPrice(item.getPrice());
                oi.setQuantity(item.getQuantity());
                oi.setTotalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                orderItemMapper.insert(oi);
            }

            // 登记订单超时任务（ZSet，score=过期时间戳），由定时任务自动关闭
            long expireAt = System.currentTimeMillis() + payTimeoutMinutes * 60 * 1000;
            redisTemplate.opsForZSet().add(ORDER_TIMEOUT_ZSET, order.getId(), expireAt);

            Map<String, Object> orderVo = new HashMap<>();
            orderVo.put("orderId", order.getId());
            orderVo.put("orderNo", orderNo);
            orderVo.put("payAmount", order.getPayAmount());
            orderVo.put("discountAmount", discount);
            orderVo.put("shopId", shopId);
            orders.add(orderVo);
            index++;
        }
        if (coupon != null && couponDiscount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("优惠券未满足使用门槛");
        }

        // 4. 清理已下单的购物车项
        List<Long> cartIds = items.stream().map(CartItem::getId).collect(Collectors.toList());
        cartItemMapper.deleteBatchIds(cartIds);

        Map<String, Object> result = new HashMap<>();
        result.put("count", orders.size());
        result.put("orders", orders);
        result.put("orderId", orders.get(0).get("orderId"));
        result.put("orderNo", orders.get(0).get("orderNo"));
        return result;
    }

    /**
     * 计算优惠券折扣金额（满减券/折扣券），未达门槛返回 0
     */
    private BigDecimal calcCouponDiscount(Coupon coupon, BigDecimal total) {
        if (coupon.getThreshold() != null && total.compareTo(coupon.getThreshold()) < 0) {
            return BigDecimal.ZERO;
        }
        if (coupon.getType() == 1 && coupon.getAmount() != null) {
            return coupon.getAmount();
        }
        if (coupon.getType() == 2 && coupon.getDiscount() != null) {
            return total.multiply(BigDecimal.ONE.subtract(coupon.getDiscount()));
        }
        return BigDecimal.ZERO;
    }

    /**
     * 模拟支付：待付款 -> 已付款，并发放订单积分
     */
    public void pay(Long userId, Long orderId) {
        Order order = getOwned(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许支付");
        }
        order.setStatus(1);
        order.setPayTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 已支付，移除超时任务
        redisTemplate.opsForZSet().remove(ORDER_TIMEOUT_ZSET, orderId);
        // 支付成功发放积分（1 元 = 1 积分）
        if (order.getPointsEarned() != null && order.getPointsEarned() > 0) {
            pointsService.change(userId, order.getPointsEarned(), 1, "订单支付获得积分：" + order.getOrderNo());
        }
    }

    /**
     * 取消订单：待付款 -> 已取消，并恢复库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Order order = getOwned(userId, orderId);
        if (order.getStatus() != 0) {
            throw new BusinessException("仅待付款订单可取消");
        }
        // 恢复库存
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            productMapper.update(null, new UpdateWrapper<com.mall.entity.Product>()
                    .eq("id", item.getProductId())
                    .setSql("stock = stock + " + item.getQuantity()));
            cacheService.evictProduct(item.getProductId());
        }
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        // 移除超时任务
        redisTemplate.opsForZSet().remove(ORDER_TIMEOUT_ZSET, orderId);
    }

    /**
     * 确认收货：已发货 -> 已完成
     */
    public void confirm(Long userId, Long orderId) {
        Order order = getOwned(userId, orderId);
        if (order.getStatus() != 2) {
            throw new BusinessException("仅已发货订单可确认收货");
        }
        order.setStatus(3);
        order.setConfirmTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 申请退款：已付款/已发货 -> 退款中（等待卖家审批）
     */
    public void applyRefund(Long userId, Long orderId) {
        Order order = getOwned(userId, orderId);
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new BusinessException("当前订单状态不支持退款");
        }
        order.setStatus(5);
        orderMapper.updateById(order);
    }

    /**
     * 卖家审批通过退款：退款中 -> 已退款，并恢复库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 5) {
            throw new BusinessException("仅退款中的订单可审批通过");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            productMapper.update(null, new UpdateWrapper<com.mall.entity.Product>()
                    .eq("id", item.getProductId())
                    .setSql("stock = stock + " + item.getQuantity()));
            cacheService.evictProduct(item.getProductId());
        }
        order.setStatus(6);
        order.setRefundTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单 {} 退款已通过", order.getOrderNo());
    }

    /**
     * 卖家拒绝退款：退款中 -> 回到原状态（未发货回已付款，已发货保持已发货）
     */
    public void rejectRefund(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 5) {
            throw new BusinessException("仅退款中的订单可拒绝");
        }
        order.setStatus(order.getShipTime() == null ? 1 : 2);
        orderMapper.updateById(order);
        log.info("订单 {} 退款被拒绝", order.getOrderNo());
    }

    /**
     * 超时自动确认收货（由定时任务调用）：已发货且超过 N 天未确认 -> 已完成
     */
    public int autoConfirmOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(autoConfirmDays);
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, 2)
                .isNotNull(Order::getShipTime)
                .le(Order::getShipTime, deadline));
        int count = 0;
        for (Order order : orders) {
            order.setStatus(3);
            order.setConfirmTime(LocalDateTime.now());
            orderMapper.updateById(order);
            count++;
            log.info("订单 {} 发货超时，已自动确认收货", order.getOrderNo());
        }
        return count;
    }

    /**
     * 超时订单自动关闭（由定时任务调用，跨 Bean 保证事务生效）
     * 仅关闭仍处于待付款状态的订单，并恢复库存
     */
    @Transactional(rollbackFor = Exception.class)
    public void closeTimeoutOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || order.getStatus() != 0) {
            return;
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        for (OrderItem item : items) {
            productMapper.update(null, new UpdateWrapper<com.mall.entity.Product>()
                    .eq("id", item.getProductId())
                    .setSql("stock = stock + " + item.getQuantity()));
            cacheService.evictProduct(item.getProductId());
        }
        order.setStatus(4);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        log.info("订单 {} 超时未支付，已自动关闭", order.getOrderNo());
    }

    /**
     * 我的订单列表
     */
    public List<Map<String, Object>> list(Long userId, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        List<Order> orders = orderMapper.selectList(wrapper);
        return orders.stream().map(o -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("order", o);
            vo.put("items", orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId())));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 订单详情
     */
    public Map<String, Object> detail(Long userId, Long orderId) {
        Order order = getOwned(userId, orderId);
        Map<String, Object> vo = new HashMap<>();
        vo.put("order", order);
        vo.put("items", orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId)));
        return vo;
    }

    /**
     * 获取属于当前用户的订单
     */
    public Order getOwned(Long userId, Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }
}
