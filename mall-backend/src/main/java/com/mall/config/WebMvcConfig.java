package com.mall.config;

import com.mall.security.AdminAuthInterceptor;
import com.mall.security.AuthInterceptor;
import com.mall.security.InternalAuthInterceptor;
import com.mall.security.RateLimitInterceptor;
import com.mall.security.SellerAuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置：跨域 + 登录拦截器 + 内部接口鉴权 + 管理端鉴权 + 卖家鉴权 + 接口限流
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;
    private final InternalAuthInterceptor internalAuthInterceptor;
    private final AdminAuthInterceptor adminAuthInterceptor;
    private final SellerAuthInterceptor sellerAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 内部服务接口：独立鉴权
        registry.addInterceptor(internalAuthInterceptor)
                .addPathPatterns("/internal/**");
        // 管理端接口：管理员角色校验
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**");
        // 卖家中心接口：卖家角色校验
        registry.addInterceptor(sellerAuthInterceptor)
                .addPathPatterns("/api/seller/**");
        // 对外接口：JWT 登录拦截，白名单放行
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/product/**",
                        "/api/category/**",
                        "/api/shop/**",
                        "/api/review/product/**",
                        "/api/seckill/list",
                        "/api/seckill/*",
                        "/api/coupon/available",
                        "/api/admin/**",
                        "/api/seller/**"
                );
        // 接口限流：防止刷接口（AI 聊天、商品搜索等高频/易被打爆接口）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/ai/**", "/api/product/list");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);
    }
}
