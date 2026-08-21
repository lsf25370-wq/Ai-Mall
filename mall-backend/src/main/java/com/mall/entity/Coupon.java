package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板实体
 */
@Data
@TableName("coupon")
public class Coupon {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    /** 类型 1满减券 2折扣券 */
    private Integer type;

    /** 使用门槛金额（满 X 可用） */
    private BigDecimal threshold;

    /** 满减金额（满减券） */
    private BigDecimal amount;

    /** 折扣率 0.95=95折（折扣券） */
    private BigDecimal discount;

    /** 适用店铺ID（NULL=全场通用） */
    private Long shopId;

    /** 发行总量 */
    private Integer totalCount;

    /** 已领取数量 */
    private Integer claimedCount;

    /** 每人限领 */
    private Integer perUserLimit;

    /** 领取后有效天数 */
    private Integer validDays;

    /** 状态 1上架 0下架 */
    private Integer status;

    private LocalDateTime createdAt;
}
