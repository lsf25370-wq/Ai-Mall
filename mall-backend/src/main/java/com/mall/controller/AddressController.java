package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.Address;
import com.mall.security.UserContext;
import com.mall.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 收货地址接口
 */
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping("/list")
    public Result<List<Address>> list() {
        return Result.ok(addressService.list(UserContext.getUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Address address) {
        addressService.add(UserContext.getUserId(), address);
        return Result.ok();
    }

    @DeleteMapping("/{addressId}")
    public Result<Void> delete(@PathVariable Long addressId) {
        addressService.delete(UserContext.getUserId(), addressId);
        return Result.ok();
    }
}
