package com.mall.controller;

import com.mall.common.Result;
import com.mall.security.UserContext;
import com.mall.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 评价接口（买家）
 */
@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 发表评价（需登录）
     */
    @PostMapping("/create")
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Long orderId = Long.valueOf(body.get("orderId").toString());
        Long orderItemId = Long.valueOf(body.get("orderItemId").toString());
        Integer rating = Integer.valueOf(body.get("rating").toString());
        String content = (String) body.getOrDefault("content", "");
        reviewService.create(UserContext.getUserId(), orderId, orderItemId, rating, content);
        return Result.ok();
    }

    /**
     * 商品评价列表（公开，挂在商品路径下随 /api/product/** 放行）
     */
    @GetMapping("/product/{productId}")
    public Result<List<Map<String, Object>>> listByProduct(@PathVariable Long productId) {
        return Result.ok(reviewService.listByProduct(productId));
    }

    /**
     * 我的评价（需登录）
     */
    @GetMapping("/mine")
    public Result<List<Map<String, Object>>> mine() {
        return Result.ok(reviewService.listMine(UserContext.getUserId()));
    }
}
