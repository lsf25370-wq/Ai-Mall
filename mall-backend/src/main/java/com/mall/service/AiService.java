package com.mall.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.AiMessage;
import com.mall.entity.AiSession;
import com.mall.mapper.AiMessageMapper;
import com.mall.mapper.AiSessionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 客服服务：会话管理 + 转发到 Python AI 服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final AiSessionMapper sessionMapper;
    private final AiMessageMapper messageMapper;

    @Value("${mall.ai.service-url}")
    private String aiServiceUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * 创建会话
     */
    public Map<String, Object> createSession(Long userId) {
        AiSession session = new AiSession();
        session.setSessionId(IdUtil.simpleUUID());
        session.setUserId(userId);
        session.setTitle("新会话");
        sessionMapper.insert(session);
        Map<String, Object> vo = new HashMap<>();
        vo.put("sessionId", session.getSessionId());
        return vo;
    }

    /**
     * 会话列表
     */
    public List<AiSession> listSessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<AiSession>()
                .eq(AiSession::getUserId, userId)
                .orderByDesc(AiSession::getUpdatedAt));
    }

    /**
     * 获取会话消息历史
     */
    public List<AiMessage> getMessages(Long userId, String sessionId) {
        getOwnedSession(userId, sessionId);
        return messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getSessionId, sessionId)
                .orderByAsc(AiMessage::getId));
    }

    /**
     * 聊天：保存用户消息 -> 调用 AI 服务 -> 保存助手回复
     */
    public String chat(Long userId, String sessionId, String message) {
        AiSession session = getOwnedSession(userId, sessionId);

        // 1. 保存用户消息
        saveMessage(sessionId, userId, "user", message);

        // 2. 调 AI 服务
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        body.put("sessionId", sessionId);
        body.put("message", message);
        String reply;
        try {
            Map<?, ?> resp = restClient.post()
                    .uri(aiServiceUrl + "/ai/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            reply = resp != null && resp.get("reply") != null ? String.valueOf(resp.get("reply")) : "抱歉，AI 服务暂时不可用，请稍后再试。";
        } catch (Exception e) {
            log.error("调用 AI 服务失败", e);
            reply = "抱歉，AI 服务暂时不可用，请稍后再试。";
        }

        // 3. 保存助手回复并更新会话标题
        saveMessage(sessionId, userId, "assistant", reply);
        if ("新会话".equals(session.getTitle())) {
            session.setTitle(message.length() > 20 ? message.substring(0, 20) : message);
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);
        return reply;
    }

    private void saveMessage(String sessionId, Long userId, String role, String content) {
        AiMessage msg = new AiMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setRole(role);
        msg.setContent(content);
        messageMapper.insert(msg);
    }

    /**
     * 校验会话归属
     */
    private AiSession getOwnedSession(Long userId, String sessionId) {
        AiSession session = sessionMapper.selectOne(
                new LambdaQueryWrapper<AiSession>().eq(AiSession::getSessionId, sessionId));
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException("会话不存在");
        }
        return session;
    }
}
