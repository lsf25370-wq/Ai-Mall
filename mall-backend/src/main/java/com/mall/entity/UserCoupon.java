package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户优惠券实体
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long couponId;

    /** 使用订单ID */
    private Long orderId;

    /** 状态 0未使用 1已使用 2已过期 */
    private Integer status;

    private LocalDateTime expireTime;

    private LocalDateTime createdAt;
}
