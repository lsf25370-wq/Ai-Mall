package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.CartItem;
import com.mall.entity.Product;
import com.mall.mapper.CartItemMapper;
import com.mall.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 购物车服务
 */
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;

    /**
     * 加入购物车：已存在则累加数量
     */
    public void add(Long userId, Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || product.getStatus() == 0) {
            throw new BusinessException("商品不存在或已下架");
        }
        CartItem exist = cartItemMapper.selectOne(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getProductId, productId));
        if (exist != null) {
            exist.setQuantity(exist.getQuantity() + quantity);
            cartItemMapper.updateById(exist);
        } else {
            CartItem item = new CartItem();
            item.setUserId(userId);
            item.setProductId(productId);
            item.setProductName(product.getName());
            item.setProductImage(product.getMainImage());
            item.setPrice(product.getPrice());
            item.setQuantity(quantity);
            item.setChecked(1);
            cartItemMapper.insert(item);
        }
    }

    /**
     * 购物车列表
     */
    public List<CartItem> list(Long userId) {
        return cartItemMapper.selectList(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .orderByDesc(CartItem::getUpdatedAt));
    }

    /**
     * 更新数量
     */
    public void updateQuantity(Long userId, Long cartItemId, Integer quantity) {
        CartItem item = getOwned(userId, cartItemId);
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    /**
     * 更新选中状态
     */
    public void updateChecked(Long userId, Long cartItemId, Integer checked) {
        CartItem item = getOwned(userId, cartItemId);
        item.setChecked(checked);
        cartItemMapper.updateById(item);
    }

    /**
     * 删除购物车项
     */
    public void remove(Long userId, Long cartItemId) {
        cartItemMapper.delete(
                new LambdaQueryWrapper<CartItem>()
                        .eq(CartItem::getUserId, userId)
                        .eq(CartItem::getId, cartItemId));
    }

    /**
     * 获取属于当前用户的购物车项
     */
    private CartItem getOwned(Long userId, Long cartItemId) {
        CartItem item = cartItemMapper.selectById(cartItemId);
        if (item == null || !item.getUserId().equals(userId)) {
            throw new BusinessException("购物车项不存在");
        }
        return item;
    }
}
