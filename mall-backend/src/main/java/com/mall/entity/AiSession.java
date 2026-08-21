package com.mall.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 客服会话实体
 */
@Data
@TableName("ai_session")
public class AiSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long userId;

    private String title;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
