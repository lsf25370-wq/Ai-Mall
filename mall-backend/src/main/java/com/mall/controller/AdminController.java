package com.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.entity.Order;
import com.mall.entity.Product;
import com.mall.entity.ProductCategory;
import com.mall.entity.Shop;
import com.mall.entity.User;
import com.mall.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理后台接口（管理员角色）
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserMapper userMapper;
    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ShopMapper shopMapper;

    /**
     * 数据概览：核心指标 + 近7日销售趋势 + 分类销量占比
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        Map<String, Object> vo = new HashMap<>();

        // 核心指标
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<>());
        long productCount = productMapper.selectCount(new LambdaQueryWrapper<>());
        List<Order> paidOrders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .in(Order::getStatus, 1, 2, 3, 5, 6));
        long orderCount = paidOrders.size();
        BigDecimal gmv = paidOrders.stream()
                .map(Order::getPayAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        vo.put("userCount", userCount);
        vo.put("productCount", productCount);
        vo.put("orderCount", orderCount);
        vo.put("gmv", gmv);

        // 近7日销售趋势
        LocalDate today = LocalDate.now();
        Map<String, BigDecimal> trendMap = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            trendMap.put(today.minusDays(i).format(DateTimeFormatter.ofPattern("MM-dd")), BigDecimal.ZERO);
        }
        for (Order o : paidOrders) {
            if (o.getPayTime() != null) {
                String key = o.getPayTime().toLocalDate().format(DateTimeFormatter.ofPattern("MM-dd"));
                trendMap.merge(key, o.getPayAmount(), BigDecimal::add);
            }
        }
        vo.put("trend", trendMap.entrySet().stream()
                .map(e -> Map.of("date", e.getKey(), "amount", e.getValue()))
                .collect(Collectors.toList()));

        // 分类销量占比（按商品销量聚合）
        List<Product> products = productMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, Long> salesByCategory = products.stream()
                .collect(Collectors.groupingBy(Product::getCategoryId,
                        Collectors.summingLong(p -> p.getSales() == null ? 0L : p.getSales())));
        List<Map<String, Object>> categorySales = new ArrayList<>();
        for (ProductCategory c : categoryMapper.selectList(new LambdaQueryWrapper<>())) {
            categorySales.add(Map.of(
                    "name", c.getName(),
                    "value", salesByCategory.getOrDefault(c.getId(), 0L)));
        }
        vo.put("categorySales", categorySales);
        return Result.ok(vo);
    }

    /**
     * 商品管理（分页）
     */
    @GetMapping("/products")
    public Result<Page<Product>> products(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<Product>()
                .orderByDesc(Product::getId);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Product::getName, keyword);
        }
        return Result.ok(productMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /**
     * 商品上下架
     */
    @PutMapping("/product/{id}/status")
    public Result<Void> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        Product product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        product.setStatus(status);
        productMapper.updateById(product);
        return Result.ok();
    }

    /**
     * 订单管理（分页，可按状态筛选）
     */
    @GetMapping("/orders")
    public Result<Page<Order>> orders(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreatedAt);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        return Result.ok(orderMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /**
     * 订单发货：已付款 -> 已发货
     */
    @PostMapping("/order/{id}/ship")
    public Result<Void> ship(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 1) {
            throw new BusinessException("仅已付款订单可发货");
        }
        order.setStatus(2);
        order.setShipTime(LocalDateTime.now());
        orderMapper.updateById(order);
        return Result.ok();
    }

    /**
     * 用户管理（分页）
     */
    @GetMapping("/users")
    public Result<Page<User>> users(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size) {
        Page<User> result = userMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<User>().orderByDesc(User::getId));
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(result);
    }

    // ==================== 店铺审核 ====================

    /**
     * 店铺列表（分页，可按状态筛选）
     */
    @GetMapping("/shops")
    public Result<Page<Shop>> shops(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int size,
                                    @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Shop> wrapper = new LambdaQueryWrapper<Shop>()
                .orderByDesc(Shop::getId);
        if (status != null) {
            wrapper.eq(Shop::getStatus, status);
        }
        return Result.ok(shopMapper.selectPage(new Page<>(page, size), wrapper));
    }

    /**
     * 店铺审核/状态变更：0待审核 1营业中 2已停业
     */
    @PutMapping("/shop/{id}/status")
    public Result<Void> updateShopStatus(@PathVariable Long id, @RequestParam Integer status) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null) {
            throw new BusinessException("店铺不存在");
        }
        shop.setStatus(status);
        shopMapper.updateById(shop);
        return Result.ok();
    }
}
