package com.mall.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mall.common.BusinessException;
import com.mall.entity.Address;
import com.mall.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 收货地址服务
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressMapper addressMapper;

    /**
     * 地址列表
     */
    public List<Address> list(Long userId) {
        return addressMapper.selectList(new LambdaQueryWrapper<Address>()
                .eq(Address::getUserId, userId)
                .orderByDesc(Address::getIsDefault));
    }

    /**
     * 新增地址
     */
    @Transactional(rollbackFor = Exception.class)
    public void add(Long userId, Address address) {
        address.setId(null);
        address.setUserId(userId);
        if (address.getIsDefault() == null) {
            address.setIsDefault(0);
        }
        // 设置默认地址时，清除其他默认
        if (address.getIsDefault() == 1) {
            clearDefault(userId);
        } else if (list(userId).isEmpty()) {
            address.setIsDefault(1);
        }
        addressMapper.insert(address);
    }

    /**
     * 删除地址
     */
    public void delete(Long userId, Long addressId) {
        Address address = addressMapper.selectById(addressId);
        if (address == null || !address.getUserId().equals(userId)) {
            throw new BusinessException("地址不存在");
        }
        addressMapper.deleteById(addressId);
    }

    private void clearDefault(Long userId) {
        List<Address> list = list(userId);
        for (Address a : list) {
            if (a.getIsDefault() == 1) {
                a.setIsDefault(0);
                addressMapper.updateById(a);
            }
        }
    }
}
