package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.entity.ProductCategory;
import com.mall.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品接口
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/category/list")
    public Result<List<ProductCategory>> categories() {
        return Result.ok(productService.listCategories());
    }

    @GetMapping("/product/list")
    public Result<Page<Product>> list(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "12") int size) {
        return Result.ok(productService.listProducts(categoryId, keyword, page, size));
    }

    @GetMapping("/product/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.getProduct(id));
    }
}
