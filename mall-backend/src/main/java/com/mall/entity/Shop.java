package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 店铺实体（卖家）
 */
@Data
@TableName("shop")
public class Shop {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ownerUserId;

    private String name;

    private String logo;

    private String description;

    /** 状态 0待审核 1营业中 2已停业 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
