package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 积分流水实体
 */
@Data
@TableName("points_log")
public class PointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 积分变动（正=获得 负=消耗），change 为 MySQL 保留字需转义 */
    @TableField("`change`")
    private Integer change;

    /** 变动后余额 */
    private Integer balance;

    /** 类型 1订单获得 2消费抵扣 3手动调整 */
    private Integer type;

    private String reason;

    private LocalDateTime createdAt;
}
