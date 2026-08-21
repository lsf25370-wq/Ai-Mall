package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String orderNo;

    private Long userId;

    /** 订单归属店铺ID（多店铺拆单后每个订单属于单一店铺，0=平台自营） */
    private Long shopId;

    /** 使用的优惠券ID */
    private Long couponId;

    private BigDecimal totalAmount;

    /** 优惠金额（券+积分抵扣） */
    private BigDecimal discountAmount;

    /** 可获得积分 */
    private Integer pointsEarned;

    private BigDecimal payAmount;

    /** 状态 0待付款 1已付款 2已发货 3已完成 4已取消 5退款中 6已退款 */
    private Integer status;

    private String addressSnapshot;

    private LocalDateTime payTime;

    private LocalDateTime shipTime;

    private LocalDateTime confirmTime;

    private LocalDateTime cancelTime;

    private LocalDateTime refundTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
