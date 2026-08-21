package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收货地址实体
 */
@Data
@TableName("address")
public class Address {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String receiver;

    private String phone;

    private String province;

    private String city;

    private String district;

    private String detail;

    private Integer isDefault;

    private LocalDateTime createdAt;
}
