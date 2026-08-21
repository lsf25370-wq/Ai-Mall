package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.mall.entity.PointsLog;
import com.mall.entity.User;
import com.mall.mapper.PointsLogMapper;
import com.mall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分服务：积分流水 + 会员等级自动升级
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsService {

    private final PointsLogMapper pointsLogMapper;
    private final UserMapper userMapper;

    /** 等级升级所需的累计积分阈值（下标=等级-1） */
    private static final int[] LEVEL_POINTS = {0, 1000, 3000, 8000, 20000};

    /**
     * 给用户增加积分（正数）或扣减（负数），并自动升级会员等级
     */
    @Transactional(rollbackFor = Exception.class)
    public void change(Long userId, int change, int type, String reason) {
        if (change == 0) {
            return;
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        int newBalance = Math.max(user.getPoints() + change, 0);
        user.setPoints(newBalance);
        userMapper.updateById(user);

        // 根据累计获得积分自动升级等级
        int newLevel = levelFor(newBalance);
        if (newLevel > user.getLevel()) {
            user.setLevel(newLevel);
            userMapper.updateById(user);
            log.info("用户 {} 升级为 Lv.{}", userId, newLevel);
        }

        PointsLog log_ = new PointsLog();
        log_.setUserId(userId);
        log_.setChange(change);
        log_.setBalance(newBalance);
        log_.setType(type);
        log_.setReason(reason);
        pointsLogMapper.insert(log_);
    }

    /**
     * 积分明细
     */
    public List<PointsLog> logs(Long userId) {
        return pointsLogMapper.selectList(new LambdaQueryWrapper<PointsLog>()
                .eq(PointsLog::getUserId, userId)
                .orderByDesc(PointsLog::getId));
    }

    /**
     * 我的积分与等级概览
     */
    public Map<String, Object> overview(Long userId) {
        User user = userMapper.selectById(userId);
        Map<String, Object> vo = new HashMap<>();
        vo.put("points", user.getPoints());
        vo.put("level", user.getLevel());
        vo.put("nextLevelPoints", nextLevelPoints(user.getLevel()));
        vo.put("progressPercent", progressPercent(user.getPoints(), user.getLevel()));
        return vo;
    }

    private int levelFor(int points) {
        int level = 1;
        for (int i = 0; i < LEVEL_POINTS.length; i++) {
            if (points >= LEVEL_POINTS[i]) {
                level = i + 1;
            }
        }
        return level;
    }

    private int nextLevelPoints(int level) {
        if (level >= LEVEL_POINTS.length) {
            return -1; // 已是最高等级
        }
        return LEVEL_POINTS[level];
    }

    private int progressPercent(int points, int level) {
        if (level >= LEVEL_POINTS.length) {
            return 100;
        }
        int cur = LEVEL_POINTS[level - 1];
        int next = LEVEL_POINTS[level];
        return (int) ((points - cur) * 100.0 / (next - cur));
    }
}
