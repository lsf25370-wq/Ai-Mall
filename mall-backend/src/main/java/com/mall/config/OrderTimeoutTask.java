package com.mall.config;

import com.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 订单超时自动关闭定时任务
 * 每 30 秒扫描 Redis 延迟队列（ZSet），关闭已超时仍未支付的待付款订单
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private static final String ORDER_TIMEOUT_ZSET = "mall:order:timeout";

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderService orderService;

    @Scheduled(fixedDelay = 30000)
    public void closeTimeoutOrders() {
        long now = System.currentTimeMillis();
        // 取出所有 score <= 当前时间（已过期）的订单
        Set<Object> expired = redisTemplate.opsForZSet().rangeByScore(ORDER_TIMEOUT_ZSET, 0, now);
        if (expired == null || expired.isEmpty()) {
            return;
        }
        for (Object idObj : expired) {
            Long orderId = Long.valueOf(String.valueOf(idObj));
            try {
                orderService.closeTimeoutOrder(orderId);
            } catch (Exception e) {
                log.error("关闭超时订单 {} 失败", orderId, e);
            } finally {
                redisTemplate.opsForZSet().remove(ORDER_TIMEOUT_ZSET, orderId);
            }
        }
    }

    /**
     * 自动确认收货：发货超过 N 天未确认，自动标记为已完成
     */
    @Scheduled(fixedDelay = 60000)
    public void autoConfirmOrders() {
        try {
            int count = orderService.autoConfirmOrders();
            if (count > 0) {
                log.info("自动确认收货 {} 笔订单", count);
            }
        } catch (Exception e) {
            log.error("自动确认收货任务执行失败", e);
        }
    }
}
