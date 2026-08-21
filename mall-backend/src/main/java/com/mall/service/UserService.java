package com.mall.service;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.dto.LoginRequest;
import com.mall.dto.RegisterRequest;
import com.mall.entity.User;
import com.mall.mapper.UserMapper;
import com.mall.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 用户服务
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    /**
     * 注册
     */
    public void register(RegisterRequest req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(BCrypt.hashpw(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : req.getUsername());
        user.setPhone(req.getPhone());
        user.setLevel(1);
        userMapper.insert(user);
    }

    /**
     * 登录，返回 token 和用户信息
     */
    public Map<String, Object> login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (user == null || !BCrypt.checkpw(req.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", toUserVo(user));
        return result;
    }

    /**
     * 获取当前用户信息
     */
    public Map<String, Object> getCurrentUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return toUserVo(user);
    }

    private Map<String, Object> toUserVo(User user) {
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", user.getId());
        vo.put("username", user.getUsername());
        vo.put("nickname", user.getNickname());
        vo.put("phone", user.getPhone());
        vo.put("avatar", user.getAvatar());
        vo.put("level", user.getLevel());
        vo.put("points", user.getPoints() != null ? user.getPoints() : 0);
        vo.put("role", user.getRole());
        return vo;
    }
}
