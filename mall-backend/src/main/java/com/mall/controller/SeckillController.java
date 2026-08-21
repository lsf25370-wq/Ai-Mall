package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.SeckillActivity;
import com.mall.security.UserContext;
import com.mall.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 秒杀接口
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /** 进行中的秒杀活动 */
    @GetMapping("/list")
    public Result<List<SeckillActivity>> list() {
        return Result.ok(seckillService.listActive());
    }

    /** 活动详情（含实时剩余库存） */
    @GetMapping("/{activityId}")
    public Result<Map<String, Object>> detail(@PathVariable Long activityId) {
        return Result.ok(seckillService.detail(activityId));
    }

    /** 秒杀下单 */
    @PostMapping("/{activityId}/buy")
    public Result<Map<String, Object>> buy(@PathVariable Long activityId,
                                           @RequestParam Long addressId) {
        return Result.ok(seckillService.buy(UserContext.getUserId(), activityId, addressId));
    }
}
