package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.Favorite;
import com.mall.entity.Product;
import com.mall.mapper.FavoriteMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 收藏服务（买家）
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final ProductMapper productMapper;

    public void add(Long userId, Long productId) {
        if (productMapper.selectById(productId) == null) {
            throw new BusinessException("商品不存在");
        }
        Long cnt = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
        if (cnt > 0) {
            throw new BusinessException("已收藏过该商品");
        }
        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setProductId(productId);
        favorite.setCreatedAt(LocalDateTime.now());
        favoriteMapper.insert(favorite);
    }

    public void remove(Long userId, Long productId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId));
    }

    public boolean isFavorite(Long userId, Long productId) {
        if (userId == null) {
            return false;
        }
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getProductId, productId)) > 0;
    }

    /**
     * 我的收藏（带商品信息）
     */
    public List<Product> list(Long userId) {
        List<Favorite> favorites = favoriteMapper.selectList(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreatedAt));
        return favorites.stream()
                .map(f -> productMapper.selectById(f.getProductId()))
                .filter(p -> p != null && p.getStatus() != null && p.getStatus() == 1)
                .collect(Collectors.toList());
    }
}
