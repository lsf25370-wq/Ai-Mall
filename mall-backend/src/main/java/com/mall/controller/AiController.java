package com.mall.controller;

import com.mall.common.Result;
import com.mall.dto.AiChatRequest;
import com.mall.entity.AiMessage;
import com.mall.entity.AiSession;
import com.mall.security.UserContext;
import com.mall.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 客服接口
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/session/create")
    public Result<Map<String, Object>> createSession() {
        return Result.ok(aiService.createSession(UserContext.getUserId()));
    }

    @GetMapping("/session/list")
    public Result<List<AiSession>> listSessions() {
        return Result.ok(aiService.listSessions(UserContext.getUserId()));
    }

    @GetMapping("/history")
    public Result<List<AiMessage>> history(@RequestParam String sessionId) {
        return Result.ok(aiService.getMessages(UserContext.getUserId(), sessionId));
    }

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@Valid @RequestBody AiChatRequest req) {
        String reply = aiService.chat(UserContext.getUserId(), req.getSessionId(), req.getMessage());
        Map<String, Object> vo = new HashMap<>();
        vo.put("reply", reply);
        return Result.ok(vo);
    }
}
