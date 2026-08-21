package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.PointsLog;
import com.mall.security.UserContext;
import com.mall.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 积分接口
 */
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointsController {

    private final PointsService pointsService;

    /** 我的积分与等级概览 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(pointsService.overview(UserContext.getUserId()));
    }

    /** 积分明细 */
    @GetMapping("/logs")
    public Result<List<PointsLog>> logs() {
        return Result.ok(pointsService.logs(UserContext.getUserId()));
    }
}
