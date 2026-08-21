package com.mall.security;

import com.mall.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 管理端鉴权拦截器：要求登录且 role=1（管理员）
 */
@Component
@RequiredArgsConstructor
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = StringUtils.hasText(token) ? jwtUtil.parse(token) : null;
        if (claims == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Integer role = claims.get("role", Integer.class);
        if (role == null || role != 1) {
            throw new BusinessException(403, "无管理员权限");
        }
        UserContext.setUserId(claims.get("userId", Long.class));
        UserContext.setRole(role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
