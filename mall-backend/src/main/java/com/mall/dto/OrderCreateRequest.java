package com.mall.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交订单请求
 */
@Data
public class OrderCreateRequest {

    /** 购物车项ID列表，为空则下单全部选中项 */
    private java.util.List<Long> cartItemIds;

    @NotNull(message = "收货地址ID不能为空")
    private Long addressId;

    /** 使用的用户优惠券ID（user_coupon.id，可选） */
    private Long userCouponId;
}
