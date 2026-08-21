package com.mall.service;

import com.mall.entity.Product;
import com.mall.entity.ProductCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Redis 缓存服务：热点商品详情缓存 / 商品分类缓存
 * 缓存策略：读多写少，TTL 短，写操作主动失效保证一致性
 */
@Service
@RequiredArgsConstructor
public class CacheService {

    private static final String PRODUCT_KEY_PREFIX = "mall:product:";
    private static final String CATEGORY_KEY = "mall:category:list";
    private static final long PRODUCT_TTL_MINUTES = 5;
    private static final long CATEGORY_TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    // ==================== 商品详情缓存 ====================

    public Product getProduct(Long id) {
        Object cached = redisTemplate.opsForValue().get(PRODUCT_KEY_PREFIX + id);
        return cached instanceof Product ? (Product) cached : null;
    }

    public void setProduct(Product product) {
        redisTemplate.opsForValue().set(PRODUCT_KEY_PREFIX + product.getId(),
                product, PRODUCT_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void evictProduct(Long id) {
        if (id != null) {
            redisTemplate.delete(PRODUCT_KEY_PREFIX + id);
        }
    }

    public void evictProducts(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(this::evictProduct);
    }

    // ==================== 商品分类缓存 ====================

    @SuppressWarnings("unchecked")
    public List<ProductCategory> getCategories() {
        Object cached = redisTemplate.opsForValue().get(CATEGORY_KEY);
        return cached instanceof List ? (List<ProductCategory>) cached : null;
    }

    public void setCategories(List<ProductCategory> categories) {
        redisTemplate.opsForValue().set(CATEGORY_KEY, categories, CATEGORY_TTL_MINUTES, TimeUnit.MINUTES);
    }

    public void evictCategories() {
        redisTemplate.delete(CATEGORY_KEY);
    }
}
