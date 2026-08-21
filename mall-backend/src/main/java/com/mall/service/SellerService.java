package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.entity.*;
import com.mall.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 卖家服务：店铺商品管理 / 店铺订单 / 发货 / 数据统计 / 评价回复 / AI 辅助
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerService {

    private final ShopService shopService;
    private final ProductMapper productMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductReviewMapper reviewMapper;
    private final ShopMapper shopMapper;
    private final CacheService cacheService;
    private final OrderService orderService;

    private final RestClient restClient = RestClient.create();

    @Value("${mall.ai.service-url}")
    private String aiServiceUrl;

    // ==================== 商品管理 ====================

    public Page<Product> listProducts(Long userId, int page, int size, String keyword) {
        Shop shop = shopService.getMyShop(userId);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getShopId, shop.getId())
                .orderByDesc(Product::getId);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        return productMapper.selectPage(new Page<>(page, size), wrapper);
    }

    public void createProduct(Long userId, Product product) {
        Shop shop = shopService.getMyShop(userId);
        product.setId(null);
        product.setShopId(shop.getId());
        product.setStatus(product.getStatus() == null ? 1 : product.getStatus());
        product.setSales(0);
        productMapper.insert(product);
        cacheService.evictProduct(product.getId());
        cacheService.evictCategories();
    }

    public void updateProduct(Long userId, Product product) {
        Product exist = getOwnedProduct(userId, product.getId());
        product.setShopId(exist.getShopId());
        product.setSales(exist.getSales());
        product.setCreatedAt(exist.getCreatedAt());
        productMapper.updateById(product);
        cacheService.evictProduct(product.getId());
    }

    public void updateProductStatus(Long userId, Long productId, Integer status) {
        Product product = getOwnedProduct(userId, productId);
        product.setStatus(status);
        productMapper.updateById(product);
        cacheService.evictProduct(productId);
    }

    public void deleteProduct(Long userId, Long productId) {
        Product product = getOwnedProduct(userId, productId);
        productMapper.deleteById(product.getId());
        cacheService.evictProduct(productId);
    }

    private Product getOwnedProduct(Long userId, Long productId) {
        Shop shop = shopService.getMyShop(userId);
        Product product = productMapper.selectById(productId);
        if (product == null || !shop.getId().equals(product.getShopId())) {
            throw new BusinessException("商品不存在或不属于您的店铺");
        }
        return product;
    }

    // ==================== 订单管理 ====================

    /**
     * 店铺订单：订单中任意商品属于本店铺即归入店铺订单
     */
    public List<Map<String, Object>> listOrders(Long userId, Integer status) {
        Shop shop = shopService.getMyShop(userId);
        List<Product> myProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getShopId, shop.getId()));
        if (myProducts.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> myProductIds = myProducts.stream().map(Product::getId).collect(Collectors.toSet());
        List<OrderItem> myItems = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, myProductIds));
        Set<Long> orderIds = myItems.stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getId, orderIds)
                .orderByDesc(Order::getCreatedAt));
        if (status != null) {
            orders = orders.stream().filter(o -> o.getStatus().equals(status)).collect(Collectors.toList());
        }
        return orders.stream().map(o -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("order", o);
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
            vo.put("items", items);
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 店铺订单发货：校验订单包含本店铺商品，且订单已付款
     */
    public void ship(Long userId, Long orderId) {
        Order order = getShopOrder(userId, orderId);
        if (order.getStatus() != 1) {
            throw new BusinessException("仅已付款订单可发货");
        }
        order.setStatus(2);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
    }

    /**
     * 退款审批：通过（退款中 -> 已退款，恢复库存）
     */
    public void approveRefund(Long userId, Long orderId) {
        getShopOrder(userId, orderId);
        orderService.approveRefund(orderId);
    }

    /**
     * 退款审批：拒绝（退款中 -> 回到原状态）
     */
    public void rejectRefund(Long userId, Long orderId) {
        getShopOrder(userId, orderId);
        orderService.rejectRefund(orderId);
    }

    /**
     * 校验订单归属本店铺并返回（不存在/无本店商品则抛异常）
     */
    private Order getShopOrder(Long userId, Long orderId) {
        Shop shop = shopService.getMyShop(userId);
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        List<Product> myProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getShopId, shop.getId()));
        Set<Long> myProductIds = myProducts.stream().map(Product::getId).collect(Collectors.toSet());
        long cnt = orderItemMapper.selectCount(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, orderId)
                .in(OrderItem::getProductId, myProductIds));
        if (cnt == 0) {
            throw new BusinessException("订单不属于您的店铺");
        }
        return order;
    }

    // ==================== 数据统计 ====================

    public Map<String, Object> stats(Long userId) {
        Shop shop = shopService.getMyShop(userId);
        List<Product> myProducts = productMapper.selectList(
                new LambdaQueryWrapper<Product>().eq(Product::getShopId, shop.getId()));
        Map<String, Object> vo = new HashMap<>();
        vo.put("productCount", myProducts.size());
        vo.put("stockTotal", myProducts.stream().mapToInt(p -> p.getStock() == null ? 0 : p.getStock()).sum());
        vo.put("salesTotal", myProducts.stream().mapToLong(p -> p.getSales() == null ? 0L : p.getSales()).sum());

        if (myProducts.isEmpty()) {
            vo.put("gmv", BigDecimal.ZERO);
            vo.put("orderCount", 0);
            vo.put("pendingShip", 0);
            vo.put("trend", emptyTrend());
            return vo;
        }
        Set<Long> ids = myProducts.stream().map(Product::getId).collect(Collectors.toSet());
        Set<Long> orderIds = orderItemMapper.selectList(
                        new LambdaQueryWrapper<OrderItem>().in(OrderItem::getProductId, ids))
                .stream().map(OrderItem::getOrderId).collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            vo.put("gmv", BigDecimal.ZERO);
            vo.put("orderCount", 0);
            vo.put("pendingShip", 0);
            vo.put("trend", emptyTrend());
            return vo;
        }
        List<Order> paidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getId, orderIds)
                .in(Order::getStatus, 1, 2, 3, 5, 6));
        vo.put("orderCount", paidOrders.size());
        vo.put("pendingShip", paidOrders.stream().filter(o -> o.getStatus() == 1).count());
        vo.put("gmv", paidOrders.stream().map(Order::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 近7日趋势
        Map<String, BigDecimal> trend = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            trend.put(today.minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")), BigDecimal.ZERO);
        }
        for (Order o : paidOrders) {
            if (o.getPayTime() != null) {
                String key = o.getPayTime().toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd"));
                trend.merge(key, o.getPayAmount(), BigDecimal::add);
            }
        }
        vo.put("trend", trend.entrySet().stream()
                .map(e -> Map.of("date", e.getKey(), "amount", e.getValue()))
                .collect(Collectors.toList()));
        return vo;
    }

    private List<Map<String, Object>> emptyTrend() {
        List<Map<String, Object>> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            list.add(Map.of("date", today.minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")),
                    "amount", BigDecimal.ZERO));
        }
        return list;
    }

    // ==================== 评价管理 ====================

    public List<ProductReview> listReviews(Long userId) {
        Shop shop = shopService.getMyShop(userId);
        return reviewMapper.selectList(new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getShopId, shop.getId())
                .orderByDesc(ProductReview::getCreatedAt));
    }

    public void replyReview(Long userId, Long reviewId, String reply) {
        Shop shop = shopService.getMyShop(userId);
        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null || !shop.getId().equals(review.getShopId())) {
            throw new BusinessException("评价不存在或不属于您的店铺");
        }
        review.setReply(reply);
        review.setReplyTime(LocalDateTime.now());
        reviewMapper.updateById(review);
    }

    // ==================== AI 辅助 ====================

    /**
     * AI 生成评价回复：基于评价内容调用 AI 服务生成回复草稿
     */
    public String aiReplyReview(Long userId, Long reviewId) {
        Shop shop = shopService.getMyShop(userId);
        ProductReview review = reviewMapper.selectById(reviewId);
        if (review == null || !shop.getId().equals(review.getShopId())) {
            throw new BusinessException("评价不存在或不属于您的店铺");
        }
        Product product = productMapper.selectById(review.getProductId());
        Map<String, Object> body = new HashMap<>();
        body.put("productName", product == null ? "本店商品" : product.getName());
        body.put("reviewContent", review.getContent());
        body.put("rating", review.getRating());
        try {
            Map<?, ?> resp = restClient.post()
                    .uri(aiServiceUrl + "/ai/review-reply")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp != null && resp.get("reply") != null) {
                return String.valueOf(resp.get("reply"));
            }
        } catch (Exception e) {
            log.error("AI 生成评价回复失败", e);
        }
        throw new BusinessException("AI 服务暂时不可用，请稍后再试");
    }

    /**
     * AI 经营分析：基于店铺统计数据调用 AI 服务生成经营洞察
     */
    public String aiAnalysis(Long userId) {
        Map<String, Object> stats = stats(userId);
        Map<String, Object> body = new HashMap<>();
        body.put("stats", stats);
        try {
            Map<?, ?> resp = restClient.post()
                    .uri(aiServiceUrl + "/ai/business-analysis")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp != null && resp.get("reply") != null) {
                return String.valueOf(resp.get("reply"));
            }
        } catch (Exception e) {
            log.error("AI 经营分析失败", e);
        }
        throw new BusinessException("AI 服务暂时不可用，请稍后再试");
    }
}
