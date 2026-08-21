package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.Coupon;
import com.mall.security.UserContext;
import com.mall.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 优惠券接口
 */
@RestController
@RequestMapping("/api/coupon")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** 可领取的优惠券 */
    @GetMapping("/available")
    public Result<List<Coupon>> available() {
        return Result.ok(couponService.listAvailable());
    }

    /** 领取优惠券 */
    @PostMapping("/{couponId}/claim")
    public Result<Void> claim(@PathVariable Long couponId) {
        couponService.claim(UserContext.getUserId(), couponId);
        return Result.ok();
    }

    /** 我的优惠券 */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> my() {
        return Result.ok(couponService.myCoupons(UserContext.getUserId()));
    }
}
