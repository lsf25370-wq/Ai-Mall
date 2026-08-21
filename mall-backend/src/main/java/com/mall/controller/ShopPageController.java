package com.mall.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.entity.Shop;
import com.mall.mapper.ProductMapper;
import com.mall.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 买家端公开接口：店铺主页 / 店铺商品
 */
@RestController
@RequestMapping("/api/shop")
@RequiredArgsConstructor
public class ShopPageController {

    private final ShopMapper shopMapper;
    private final ProductMapper productMapper;

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Shop shop = shopMapper.selectById(id);
        if (shop == null || shop.getStatus() == null || shop.getStatus() != 1) {
            throw new BusinessException("店铺不存在或已停业");
        }
        Map<String, Object> vo = new HashMap<>();
        vo.put("id", shop.getId());
        vo.put("name", shop.getName());
        vo.put("logo", shop.getLogo());
        vo.put("description", shop.getDescription());
        vo.put("createdAt", shop.getCreatedAt());
        return Result.ok(vo);
    }

    @GetMapping("/{id}/products")
    public Result<Page<Product>> products(@PathVariable Long id,
                                          @RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "12") int size) {
        Page<Product> result = productMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<Product>()
                        .eq(Product::getShopId, id)
                        .eq(Product::getStatus, 1)
                        .orderByDesc(Product::getSales));
        return Result.ok(result);
    }
}
