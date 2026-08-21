package com.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.FaqDoc;
import com.mall.entity.Order;
import com.mall.entity.OrderItem;
import com.mall.entity.Product;
import com.mall.mapper.FaqDocMapper;
import com.mall.mapper.OrderItemMapper;
import com.mall.mapper.OrderMapper;
import com.mall.mapper.ProductMapper;
import com.mall.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 内部服务接口：仅供 AI 客服服务调用（InternalAuthInterceptor 鉴权）
 * AI 客服通过 Tool Calling 调用这些接口获取真实业务数据
 */
@RestController
@RequestMapping("/internal/ai")
@RequiredArgsConstructor
public class InternalAiController {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final OrderService orderService;
    private final FaqDocMapper faqDocMapper;

    /**
     * 按订单号查询订单状态（AI 客服："我的订单到哪了"）
     * 仅返回该 userId 名下的订单，防止越权
     */
    @GetMapping("/order")
    public Map<String, Object> queryOrder(@RequestParam String orderNo, @RequestParam Long userId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException("未查询到该订单，请确认订单号是否正确");
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, order.getId()));
        Map<String, Object> vo = new HashMap<>();
        vo.put("orderNo", order.getOrderNo());
        vo.put("status", order.getStatus());
        vo.put("statusDesc", statusDesc(order.getStatus()));
        vo.put("payAmount", order.getPayAmount());
        vo.put("address", order.getAddressSnapshot());
        vo.put("payTime", order.getPayTime());
        vo.put("shipTime", order.getShipTime());
        vo.put("logistics", logisticsDesc(order));
        vo.put("items", items.stream().map(i -> Map.of(
                "name", i.getProductName(),
                "quantity", i.getQuantity(),
                "price", i.getPrice()
        )).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 查询用户全部订单（AI 客服："我有哪些订单"）
     */
    @GetMapping("/orders")
    public Map<String, Object> listOrders(@RequestParam Long userId) {
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .orderByDesc(Order::getCreatedAt));
        Map<String, Object> vo = new HashMap<>();
        vo.put("count", orders.size());
        vo.put("orders", orders.stream().map(o -> {
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
            Map<String, Object> m = new HashMap<>();
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("statusDesc", statusDesc(o.getStatus()));
            m.put("payAmount", o.getPayAmount());
            m.put("createdAt", o.getCreatedAt());
            m.put("itemSummary", items.stream().map(OrderItem::getProductName).collect(Collectors.joining("、")));
            return m;
        }).collect(Collectors.toList()));
        return vo;
    }

    /**
     * 商品搜索（AI 客服："推荐一下手机"）
     */
    @GetMapping("/products")
    public List<Product> searchProducts(@RequestParam(required = false) String keyword,
                                        @RequestParam(required = false) BigDecimal maxPrice) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }
        wrapper.last("limit 5");
        return productMapper.selectList(wrapper);
    }

    /**
     * 商品推荐（AI 客服："预算 3000 以内推荐手机"）：关键词 + 价格上限 + 销量排序
     */
    @GetMapping("/recommend")
    public List<Product> recommend(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) BigDecimal maxPrice,
                                   @RequestParam(required = false) Long categoryId) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, 1)
                .orderByDesc(Product::getSales);
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        if (maxPrice != null) {
            wrapper.le(Product::getPrice, maxPrice);
        }
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        wrapper.last("limit 5");
        return productMapper.selectList(wrapper);
    }

    /**
     * 订单物流轨迹（AI 客服："物流到哪了"）
     * 仅返回该 userId 名下的订单，防止越权
     */
    @GetMapping("/logistics")
    public Map<String, Object> queryLogistics(@RequestParam String orderNo, @RequestParam Long userId) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
                .eq(Order::getUserId, userId));
        if (order == null) {
            throw new BusinessException("未查询到该订单，请确认订单号是否正确");
        }
        Map<String, Object> vo = new HashMap<>();
        vo.put("orderNo", order.getOrderNo());
        vo.put("statusDesc", statusDesc(order.getStatus()));
        List<Map<String, Object>> track = new java.util.ArrayList<>();
        track.add(Map.of("time", String.valueOf(order.getCreatedAt()), "desc", "订单创建"));
        if (order.getPayTime() != null) {
            track.add(Map.of("time", String.valueOf(order.getPayTime()), "desc", "买家已付款"));
        }
        if (order.getShipTime() != null) {
            track.add(Map.of("time", String.valueOf(order.getShipTime()), "desc", "商家已发货"));
            track.add(Map.of("time", "运输中", "desc", "包裹已交由物流公司，正在运往收货地址"));
        }
        if (order.getConfirmTime() != null) {
            track.add(Map.of("time", String.valueOf(order.getConfirmTime()), "desc", "买家已签收，订单完成"));
        }
        vo.put("track", track);
        vo.put("summary", logisticsDesc(order));
        return vo;
    }

    /**
     * FAQ 知识库（AI 客服 RAG 检索的数据源）
     */
    @GetMapping("/faq")
    public List<FaqDoc> listFaq() {
        return faqDocMapper.selectList(new LambdaQueryWrapper<FaqDoc>().orderByAsc(FaqDoc::getId));
    }

    private String statusDesc(int status) {
        return switch (status) {
            case 0 -> "待付款";
            case 1 -> "已付款，待发货";
            case 2 -> "已发货，运输中";
            case 3 -> "已完成";
            case 4 -> "已取消";
            case 5 -> "退款中";
            case 6 -> "已退款";
            default -> "未知状态";
        };
    }

    private String logisticsDesc(Order order) {
        return switch (order.getStatus()) {
            case 0 -> "订单还未支付，请先完成付款。";
            case 1 -> "商家正在备货，将尽快为您发货。";
            case 2 -> "包裹已发出，正在运输途中，预计 1-3 天内送达，请您保持电话畅通。";
            case 3 -> "订单已完成，感谢您的购买。";
            case 4 -> "订单已取消，若有疑问请联系客服。";
            default -> "订单状态处理中，请稍后在订单页查看。";
        };
    }
}
