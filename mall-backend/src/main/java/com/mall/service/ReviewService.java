package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.Order;
import com.mall.entity.OrderItem;
import com.mall.entity.Product;
import com.mall.entity.ProductReview;
import com.mall.entity.User;
import com.mall.entity.Shop;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ProductReviewMapper;
import com.mall.mapper.ShopMapper;
import com.mall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 评价服务（买家）：订单评价 / 商品评价列表 / 我的评价
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductReviewMapper reviewMapper;
    private final ShopMapper shopMapper;
    private final UserMapper userMapper;

    /**
     * 发表评价：校验订单归属与商品归属，一个商品一个订单只能评价一次
     */
    public void create(Long userId, Long orderId, Long orderItemId, Integer rating, String content) {
        Order order = orderMapper.selectById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() < 2) {
            throw new BusinessException("订单发货后才能评价");
        }
        OrderItem item = orderItemMapper.selectById(orderItemId);
        if (item == null || !item.getOrderId().equals(orderId)) {
            throw new BusinessException("订单商品不存在");
        }
        Long dup = reviewMapper.selectCount(new LambdaQueryWrapper<ProductReview>()
                .eq(ProductReview::getOrderId, orderId)
                .eq(ProductReview::getOrderItemId, orderItemId));
        if (dup > 0) {
            throw new BusinessException("该商品已评价过");
        }
        Product product = productMapper.selectById(item.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BusinessException("评分需在 1-5 之间");
        }

        ProductReview review = new ProductReview();
        review.setOrderId(orderId);
        review.setOrderItemId(orderItemId);
        review.setUserId(userId);
        review.setProductId(product.getId());
        review.setShopId(product.getShopId());
        review.setRating(rating);
        review.setContent(content);
        review.setCreatedAt(LocalDateTime.now());
        reviewMapper.insert(review);
    }

    /**
     * 商品评价列表（公开）
     */
    public List<Map<String, Object>> listByProduct(Long productId) {
        List<ProductReview> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getProductId, productId)
                        .orderByDesc(ProductReview::getCreatedAt));
        return reviews.stream().map(r -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("review", r);
            User user = userMapper.selectById(r.getUserId());
            vo.put("nickname", user != null ? user.getNickname() : "用户" + r.getUserId());
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 我的评价列表
     */
    public List<Map<String, Object>> listMine(Long userId) {
        List<ProductReview> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<ProductReview>()
                        .eq(ProductReview::getUserId, userId)
                        .orderByDesc(ProductReview::getCreatedAt));
        return reviews.stream().map(r -> {
            Map<String, Object> vo = new HashMap<>();
            vo.put("review", r);
            Product product = productMapper.selectById(r.getProductId());
            vo.put("product", product);
            Shop shop = r.getShopId() != null ? shopMapper.selectById(r.getShopId()) : null;
            vo.put("shopName", shop != null ? shop.getName() : "");
            return vo;
        }).collect(Collectors.toList());
    }
}
