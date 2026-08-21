package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动实体
 */
@Data
@TableName("seckill_activity")
public class SeckillActivity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long productId;

    private String productName;

    private String productImage;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 秒杀总库存 */
    private Integer totalStock;

    /** 已秒数量 */
    private Integer soldCount;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    /** 状态 0未开始 1进行中 2已结束 3已下架 */
    private Integer status;

    private LocalDateTime createdAt;
}
