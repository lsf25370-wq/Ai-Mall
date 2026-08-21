package com.mall.config;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.entity.User;
import com.mall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动初始化器：将示例用户占位密码 {INIT} 替换为真实 BCrypt 密文（明文 123456）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;

    @Override
    public void run(ApplicationArguments args) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getPassword, "{INIT}"));
        for (User user : users) {
            user.setPassword(BCrypt.hashpw("123456"));
            userMapper.updateById(user);
            log.info("初始化示例用户密码: username={}", user.getUsername());
        }
    }
}
