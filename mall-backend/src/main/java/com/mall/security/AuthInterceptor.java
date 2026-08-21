package com.mall.security;

import com.mall.common.Result;
import com.mall.common.BusinessException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 登录拦截器
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 放行 CORS 预检请求
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Claims claims = StringUtils.hasText(token) ? jwtUtil.parse(token) : null;
        if (claims == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        Long userId = claims.get("userId", Long.class);
        Integer role = claims.get("role", Integer.class);
        UserContext.setUserId(userId);
        UserContext.setRole(role);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
