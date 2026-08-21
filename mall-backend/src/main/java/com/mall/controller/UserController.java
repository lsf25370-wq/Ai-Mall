package com.mall.controller;

import com.mall.common.Result;
import com.mall.dto.LoginRequest;
import com.mall.dto.RegisterRequest;
import com.mall.security.UserContext;
import com.mall.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest req) {
        userService.register(req);
        return Result.ok();
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(userService.login(req));
    }

    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.ok(userService.getCurrentUser(UserContext.getUserId()));
    }
}
