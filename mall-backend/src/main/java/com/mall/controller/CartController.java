package com.mall.controller;

import com.mall.common.Result;
import com.mall.dto.CartAddRequest;
import com.mall.entity.CartItem;
import com.mall.security.UserContext;
import com.mall.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 购物车接口
 */
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping("/add")
    public Result<Void> add(@Valid @RequestBody CartAddRequest req) {
        cartService.add(UserContext.getUserId(), req.getProductId(), req.getQuantity());
        return Result.ok();
    }

    @GetMapping("/list")
    public Result<List<CartItem>> list() {
        return Result.ok(cartService.list(UserContext.getUserId()));
    }

    @PutMapping("/quantity/{cartItemId}")
    public Result<Void> updateQuantity(@PathVariable Long cartItemId, @RequestParam Integer quantity) {
        cartService.updateQuantity(UserContext.getUserId(), cartItemId, quantity);
        return Result.ok();
    }

    @PutMapping("/checked/{cartItemId}")
    public Result<Void> updateChecked(@PathVariable Long cartItemId, @RequestParam Integer checked) {
        cartService.updateChecked(UserContext.getUserId(), cartItemId, checked);
        return Result.ok();
    }

    @DeleteMapping("/{cartItemId}")
    public Result<Void> remove(@PathVariable Long cartItemId) {
        cartService.remove(UserContext.getUserId(), cartItemId);
        return Result.ok();
    }
}
