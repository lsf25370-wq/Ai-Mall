package com.mall.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 客服聊天请求
 */
@Data
public class AiChatRequest {

    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    @NotBlank(message = "消息不能为空")
    private String message;
}
