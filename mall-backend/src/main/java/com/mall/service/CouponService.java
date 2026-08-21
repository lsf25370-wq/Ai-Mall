package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.Coupon;
import com.mall.entity.UserCoupon;
import com.mall.mapper.CouponMapper;
import com.mall.mapper.UserCouponMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 优惠券服务：领取 / 我的券 / 过期检测
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponMapper couponMapper;
    private final UserCouponMapper userCouponMapper;

    /**
     * 可领取的优惠券列表
     */
    public List<Coupon> listAvailable() {
        return couponMapper.selectList(new LambdaQueryWrapper<Coupon>()
                .eq(Coupon::getStatus, 1)
                .apply("claimed_count < total_count")
                .orderByAsc(Coupon::getId));
    }

    /**
     * 领取优惠券：校验库存 + 限领 + 原子更新已领取数
     */
    @Transactional(rollbackFor = Exception.class)
    public void claim(Long userId, Long couponId) {
        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || coupon.getStatus() != 1) {
            throw new BusinessException("优惠券不存在或已下架");
        }
        if (coupon.getClaimedCount() >= coupon.getTotalCount()) {
            throw new BusinessException("优惠券已被领完");
        }
        Long claimed = userCouponMapper.selectCount(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .eq(UserCoupon::getCouponId, couponId));
        if (claimed >= coupon.getPerUserLimit()) {
            throw new BusinessException("您已领取过该优惠券");
        }

        // 原子扣减剩余库存（防并发超发）
        int rows = couponMapper.update(null, new UpdateWrapper<Coupon>()
                .eq("id", couponId)
                .apply("claimed_count < total_count")
                .setSql("claimed_count = claimed_count + 1"));
        if (rows == 0) {
            throw new BusinessException("优惠券已被领完");
        }

        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponId(couponId);
        uc.setStatus(0);
        uc.setExpireTime(LocalDateTime.now().plusDays(coupon.getValidDays()));
        userCouponMapper.insert(uc);
        log.info("用户 {} 领取优惠券 {}", userId, coupon.getName());
    }

    /**
     * 我的优惠券（未使用 + 已使用 + 已过期）
     */
    public List<Map<String, Object>> myCoupons(Long userId) {
        // 先自动过期：将已过期的未使用券标记为已过期
        userCouponMapper.update(null, new UpdateWrapper<UserCoupon>()
                .eq("user_id", userId)
                .eq("status", 0)
                .lt("expire_time", LocalDateTime.now())
                .set("status", 2));

        List<UserCoupon> ucs = userCouponMapper.selectList(new LambdaQueryWrapper<UserCoupon>()
                .eq(UserCoupon::getUserId, userId)
                .orderByDesc(UserCoupon::getCreatedAt));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCoupon uc : ucs) {
            Coupon coupon = couponMapper.selectById(uc.getCouponId());
            if (coupon == null) {
                continue;
            }
            Map<String, Object> vo = new HashMap<>();
            vo.put("id", uc.getId());
            vo.put("couponId", coupon.getId());
            vo.put("name", coupon.getName());
            vo.put("type", coupon.getType());
            vo.put("threshold", coupon.getThreshold());
            vo.put("amount", coupon.getAmount());
            vo.put("discount", coupon.getDiscount());
            vo.put("shopId", coupon.getShopId());
            vo.put("status", uc.getStatus());
            vo.put("expireTime", uc.getExpireTime());
            vo.put("orderId", uc.getOrderId());
            result.add(vo);
        }
        return result;
    }

    /**
     * 校验用户优惠券可用性，返回券信息（供下单结算使用）
     */
    public Coupon checkUsable(Long userId, Long userCouponId) {
        UserCoupon uc = userCouponMapper.selectById(userCouponId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new BusinessException("优惠券不存在");
        }
        if (uc.getStatus() != 0) {
            throw new BusinessException("优惠券已使用或已过期");
        }
        if (uc.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("优惠券已过期");
        }
        return couponMapper.selectById(uc.getCouponId());
    }

    /**
     * 标记优惠券已使用
     */
    public void markUsed(Long userCouponId, Long orderId) {
        UserCoupon uc = new UserCoupon();
        uc.setId(userCouponId);
        uc.setStatus(1);
        uc.setOrderId(orderId);
        userCouponMapper.updateById(uc);
    }
}
