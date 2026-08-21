package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品评价实体
 */
@Data
@TableName("product_review")
public class ProductReview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;

    private Long orderItemId;

    private Long userId;

    private Long productId;

    private Long shopId;

    /** 评分 1-5 */
    private Integer rating;

    private String content;

    /** 卖家回复 */
    private String reply;

    private LocalDateTime replyTime;

    private LocalDateTime createdAt;
}
