package com.mall.security;

import cn.hutool.json.JSONUtil;
import com.mall.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流拦截器：基于 Redis 固定窗口计数（INCR + TTL）
 * 以登录用户 ID 为维度，未登录则以 IP 为维度，防止刷接口与恶意请求
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String KEY_PREFIX = "mall:ratelimit:";

    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${mall.rate-limit.max:30}")
    private int maxRequests;

    @Value("${mall.rate-limit.window-seconds:1}")
    private int windowSeconds;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String client = resolveClient(request);
        String key = KEY_PREFIX + request.getRequestURI() + ":" + client;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        if (count != null && count > maxRequests) {
            log.warn("接口限流触发: {} client={} count={}", request.getRequestURI(), client, count);
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(Result.fail(429, "请求过于频繁，请稍后再试")));
            return false;
        }
        return true;
    }

    private String resolveClient(HttpServletRequest request) {
        Long userId = UserContext.getUserId();
        if (userId != null) {
            return "u" + userId;
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }
}
