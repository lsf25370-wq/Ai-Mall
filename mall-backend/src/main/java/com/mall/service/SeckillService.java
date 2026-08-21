package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.Address;
import com.mall.entity.Order;
import com.mall.entity.OrderItem;
import com.mall.entity.Product;
import com.mall.entity.SeckillActivity;
import com.mall.mapper.AddressMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.SeckillActivityMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀服务：Redis 预扣库存防超卖 + SETNX 限购防重复 + DB 原子扣减兜底
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillService {

    private final SeckillActivityMapper seckillMapper;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final AddressMapper addressMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheService cacheService;

    /** 秒杀库存预扣 key：mall:seckill:stock:{activityId}（value=剩余库存） */
    private static final String STOCK_KEY = "mall:seckill:stock:";
    /** 用户限购标记 key：mall:seckill:user:{activityId}:{userId} */
    private static final String USER_KEY = "mall:seckill:user:";
    /** 订单超时集合（与 OrderService 共用） */
    private static final String ORDER_TIMEOUT_ZSET = "mall:order:timeout";

    private final long payTimeoutMinutes = 15;

    /**
     * 进行中的秒杀活动列表
     */
    public List<SeckillActivity> listActive() {
        LocalDateTime now = LocalDateTime.now();
        return seckillMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 1)
                .le(SeckillActivity::getStartTime, now)
                .ge(SeckillActivity::getEndTime, now)
                .orderByAsc(SeckillActivity::getId));
    }

    /**
     * 秒杀下单：Redis 预扣库存 + 每人限购一次 + 生成待付款订单
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> buy(Long userId, Long activityId, Long addressId) {
        SeckillActivity activity = seckillMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("秒杀活动不存在");
        }
        LocalDateTime now = LocalDateTime.now();
        if (activity.getStatus() != 1
                || now.isBefore(activity.getStartTime())
                || now.isAfter(activity.getEndTime())) {
            throw new BusinessException("活动未开始或已结束");
        }
        if (activity.getSoldCount() >= activity.getTotalStock()) {
            throw new BusinessException("手慢了，已被抢光");
        }

        // 1. 每人限购一次（SETNX，TTL 到活动结束）
        String userKey = USER_KEY + activityId + ":" + userId;
        long ttlSeconds = Duration.between(now, activity.getEndTime()).getSeconds() + 60;
        Boolean first = redisTemplate.opsForValue()
                .setIfAbsent(userKey, "1", Duration.ofSeconds(ttlSeconds));
        if (first == null || !first) {
            throw new BusinessException("您已参与过本场秒杀，每人限购 1 件");
        }

        // 2. Redis 原子预扣库存（防超卖第一道防线）
        String stockKey = STOCK_KEY + activityId;
        if (redisTemplate.opsForValue().get(stockKey) == null) {
            redisTemplate.opsForValue().set(stockKey, activity.getTotalStock(),
                    Duration.ofSeconds(ttlSeconds));
        }
        Long remain = redisTemplate.opsForValue().increment(stockKey, -1);
        if (remain == null || remain < 0) {
            redisTemplate.opsForValue().increment(stockKey, 1); // 回滚
            redisTemplate.delete(userKey);
            throw new BusinessException("手慢了，已被抢光");
        }

        // 3. DB 原子扣减商品库存兜底（防超卖第二道防线）
        int rows = productMapper.update(null, new UpdateWrapper<Product>()
                .eq("id", activity.getProductId())
                .ge("stock", 1)
                .setSql("stock = stock - 1"));
        if (rows == 0) {
            redisTemplate.opsForValue().increment(stockKey, 1);
            redisTemplate.delete(userKey);
            throw new BusinessException("手慢了，已被抢光");
        }
        cacheService.evictProduct(activity.getProductId());

        // 4. 活动已秒数 +1
        seckillMapper.update(null, new UpdateWrapper<SeckillActivity>()
                .eq("id", activityId)
                .setSql("sold_count = sold_count + 1"));

        // 5. 生成待付款订单（秒杀价）
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            redisTemplate.opsForValue().increment(stockKey, 1);
            redisTemplate.delete(userKey);
            throw new BusinessException("收货地址不存在");
        }
        Product product = productMapper.selectById(activity.getProductId());

        String orderNo = "SK" + System.currentTimeMillis();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShopId(product.getShopId());
        order.setTotalAmount(activity.getSeckillPrice());
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setPayAmount(activity.getSeckillPrice());
        order.setPointsEarned(activity.getSeckillPrice().intValue());
        order.setStatus(0);
        order.setAddressSnapshot(address.getReceiver() + " " + address.getPhone() + " "
                + address.getProvince() + address.getCity() + address.getDistrict() + address.getDetail());
        orderMapper.insert(order);

        OrderItem oi = new OrderItem();
        oi.setOrderId(order.getId());
        oi.setOrderNo(orderNo);
        oi.setProductId(product.getId());
        oi.setProductName(activity.getProductName() != null ? activity.getProductName() : product.getName());
        oi.setProductImage(activity.getProductImage() != null ? activity.getProductImage() : product.getMainImage());
        oi.setPrice(activity.getSeckillPrice());
        oi.setQuantity(1);
        oi.setTotalPrice(activity.getSeckillPrice());
        orderItemMapper.insert(oi);

        // 6. 登记超时自动关闭任务
        long expireAt = System.currentTimeMillis() + payTimeoutMinutes * 60 * 1000;
        redisTemplate.opsForZSet().add(ORDER_TIMEOUT_ZSET, order.getId(), expireAt);

        log.info("用户 {} 秒杀成功 activity={} orderNo={}", userId, activityId, orderNo);
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", orderNo);
        result.put("payAmount", activity.getSeckillPrice());
        return result;
    }

    /**
     * 秒杀活动详情（含实时剩余库存）
     */
    public Map<String, Object> detail(Long activityId) {
        SeckillActivity activity = seckillMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException("秒杀活动不存在");
        }
        Object remainObj = redisTemplate.opsForValue().get(STOCK_KEY + activityId);
        int remain = remainObj == null ? activity.getTotalStock() - activity.getSoldCount()
                : ((Number) remainObj).intValue();
        Map<String, Object> vo = new HashMap<>();
        vo.put("activity", activity);
        vo.put("remainStock", Math.max(remain, 0));
        return vo;
    }
}
