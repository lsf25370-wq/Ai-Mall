package com.mall.controller;

import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.security.UserContext;
import com.mall.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏接口（买家，需登录）
 */
@RestController
@RequestMapping("/api/favorite")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/list")
    public Result<List<Product>> list() {
        return Result.ok(favoriteService.list(UserContext.getUserId()));
    }

    @PostMapping("/add")
    public Result<Void> add(@RequestBody Map<String, Long> body) {
        favoriteService.add(UserContext.getUserId(), body.get("productId"));
        return Result.ok();
    }

    @DeleteMapping("/remove")
    public Result<Void> remove(@RequestParam Long productId) {
        favoriteService.remove(UserContext.getUserId(), productId);
        return Result.ok();
    }

    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam Long productId) {
        return Result.ok(Map.of("favorite", favoriteService.isFavorite(UserContext.getUserId(), productId)));
    }
}
