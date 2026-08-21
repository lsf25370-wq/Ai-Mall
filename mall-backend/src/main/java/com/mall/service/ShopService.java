package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.Shop;
import com.mall.entity.User;
import com.mall.mapper.ShopMapper;
import com.mall.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 店铺服务：开店申请 / 店铺信息 / 审核
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final ShopMapper shopMapper;
    private final UserMapper userMapper;

    /**
     * 申请开店：创建待审核店铺，用户角色升级为卖家(2)
     */
    public Shop apply(Long userId, String name, String logo, String description) {
        Shop exist = shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>().eq(Shop::getOwnerUserId, userId));
        if (exist != null) {
            throw new BusinessException("您已拥有店铺，无需重复申请");
        }
        Shop shop = new Shop();
        shop.setOwnerUserId(userId);
        shop.setName(name);
        shop.setLogo(logo);
        shop.setDescription(description);
        shop.setStatus(0);
        shopMapper.insert(shop);

        // 升级为卖家角色
        User user = userMapper.selectById(userId);
        if (user != null && user.getRole() == 0) {
            user.setRole(2);
            userMapper.updateById(user);
        }
        return shop;
    }

    /**
     * 获取我的店铺（无店铺返回 null，供开店页判断）
     */
    public Shop findMyShop(Long userId) {
        return shopMapper.selectOne(
                new LambdaQueryWrapper<Shop>().eq(Shop::getOwnerUserId, userId));
    }

    /**
     * 获取我的店铺
     */
    public Shop getMyShop(Long userId) {
        Shop shop = findMyShop(userId);
        if (shop == null) {
            throw new BusinessException("您还没有店铺，请先申请开店");
        }
        return shop;
    }

    /**
     * 更新店铺信息
     */
    public Shop update(Long userId, String name, String logo, String description) {
        Shop shop = getMyShop(userId);
        if (name != null && !name.isBlank()) {
            shop.setName(name);
        }
        if (logo != null) {
            shop.setLogo(logo);
        }
        if (description != null) {
            shop.setDescription(description);
        }
        shopMapper.updateById(shop);
        return shop;
    }
}
