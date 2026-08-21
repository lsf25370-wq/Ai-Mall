package com.mall.security;

import com.mall.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 内部服务鉴权拦截器：仅允许携带 X-Internal-Key 的服务间调用
 * 用于 AI 客服服务查询订单等内部接口，防止外部直接越权访问
 */
@Component
public class InternalAuthInterceptor implements HandlerInterceptor {

    @Value("${mall.internal.key:mall-internal-key-2026}")
    private String internalKey;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String key = request.getHeader("X-Internal-Key");
        if (!internalKey.equals(key)) {
            throw new BusinessException(403, "禁止访问内部接口");
        }
        return true;
    }
}
