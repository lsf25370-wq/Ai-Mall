package com.mall.controller;

import com.mall.common.Result;
import com.mall.dto.OrderCreateRequest;
import com.mall.security.UserContext;
import com.mall.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    public Result<Map<String, Object>> create(@Valid @RequestBody OrderCreateRequest req) {
        return Result.ok(orderService.create(UserContext.getUserId(), req));
    }

    @PostMapping("/pay/{orderId}")
    public Result<Void> pay(@PathVariable Long orderId) {
        orderService.pay(UserContext.getUserId(), orderId);
        return Result.ok();
    }

    @PostMapping("/cancel/{orderId}")
    public Result<Void> cancel(@PathVariable Long orderId) {
        orderService.cancel(UserContext.getUserId(), orderId);
        return Result.ok();
    }

    @PostMapping("/confirm/{orderId}")
    public Result<Void> confirm(@PathVariable Long orderId) {
        orderService.confirm(UserContext.getUserId(), orderId);
        return Result.ok();
    }

    @PostMapping("/refund/{orderId}")
    public Result<Void> refund(@PathVariable Long orderId) {
        orderService.applyRefund(UserContext.getUserId(), orderId);
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(orderService.list(UserContext.getUserId(), status));
    }

    @GetMapping("/{orderId}")
    public Result<Map<String, Object>> detail(@PathVariable Long orderId) {
        return Result.ok(orderService.detail(UserContext.getUserId(), orderId));
    }
}
