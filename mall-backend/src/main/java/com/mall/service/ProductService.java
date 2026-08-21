package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.BusinessException;
import com.mall.entity.Product;
import com.mall.entity.ProductCategory;
import com.mall.mapper.ProductCategoryMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品服务
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper categoryMapper;
    private final CacheService cacheService;

    /**
     * 分类列表（Redis 缓存 30 分钟）
     */
    public List<ProductCategory> listCategories() {
        List<ProductCategory> cached = cacheService.getCategories();
        if (cached != null) {
            return cached;
        }
        List<ProductCategory> list = categoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>()
                        .orderByAsc(ProductCategory::getSort));
        cacheService.setCategories(list);
        return list;
    }

    /**
     * 商品分页列表（支持分类筛选、关键词搜索）
     */
    public Page<Product> listProducts(Long categoryId, String keyword, int page, int size) {
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Product::getStatus, 1);
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Product::getName, keyword);
        }
        wrapper.orderByDesc(Product::getSales);
        return productMapper.selectPage(new Page<>(page, size), wrapper);
    }

    /**
     * 商品详情（Redis 热点缓存，先查缓存再回源数据库，缓解高并发读压力）
     */
    public Product getProduct(Long id) {
        Product cached = cacheService.getProduct(id);
        if (cached != null) {
            return cached;
        }
        Product product = productMapper.selectById(id);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }
        cacheService.setProduct(product);
        return product;
    }
}
