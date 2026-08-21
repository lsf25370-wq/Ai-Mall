package com.mall.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.Result;
import com.mall.entity.Product;
import com.mall.entity.ProductReview;
import com.mall.entity.Shop;
import com.mall.entity.User;
import com.mall.mapper.UserMapper;
import com.mall.security.JwtUtil;
import com.mall.security.UserContext;
import com.mall.service.SellerService;
import com.mall.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 卖家中心接口（千牛风格）：店铺 / 商品 / 订单 / 发货 / 数据 / 评价回复
 * 除开店接口外均要求角色为卖家(2)，见 SellerAuthInterceptor
 */
@RestController
@RequestMapping("/api/seller")
@RequiredArgsConstructor
public class SellerController {

    private final ShopService shopService;
    private final SellerService sellerService;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    // ==================== 店铺 ====================

    /**
     * 我的店铺信息（无店铺返回 data=null，前端据此进入开店流程）
     */
    @GetMapping("/shop")
    public Result<Shop> myShop() {
        return Result.ok(shopService.findMyShop(UserContext.getUserId()));
    }

    /**
     * 申请开店 / 创建店铺，成功返回店铺与新 token（角色已升级为卖家）
     */
    @PostMapping("/shop")
    public Result<Map<String, Object>> applyShop(@RequestBody Shop req) {
        Long userId = UserContext.getUserId();
        Shop shop = shopService.apply(userId, req.getName(), req.getLogo(), req.getDescription());
        User user = userMapper.selectById(userId);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> vo = new HashMap<>();
        vo.put("shop", shop);
        vo.put("token", token);
        return Result.ok(vo);
    }

    /**
     * 更新店铺信息
     */
    @PutMapping("/shop")
    public Result<Shop> updateShop(@RequestBody Shop req) {
        return Result.ok(shopService.update(UserContext.getUserId(),
                req.getName(), req.getLogo(), req.getDescription()));
    }

    // ==================== 商品 ====================

    @GetMapping("/product/list")
    public Result<Page<Product>> products(@RequestParam(defaultValue = "1") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(sellerService.listProducts(UserContext.getUserId(), page, size, keyword));
    }

    @PostMapping("/product")
    public Result<Void> createProduct(@RequestBody Product product) {
        sellerService.createProduct(UserContext.getUserId(), product);
        return Result.ok();
    }

    @PutMapping("/product/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        sellerService.updateProduct(UserContext.getUserId(), product);
        return Result.ok();
    }

    @PutMapping("/product/{id}/status")
    public Result<Void> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        sellerService.updateProductStatus(UserContext.getUserId(), id, status);
        return Result.ok();
    }

    @DeleteMapping("/product/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        sellerService.deleteProduct(UserContext.getUserId(), id);
        return Result.ok();
    }

    // ==================== 订单 ====================

    @GetMapping("/order/list")
    public Result<List<Map<String, Object>>> orders(@RequestParam(required = false) Integer status) {
        return Result.ok(sellerService.listOrders(UserContext.getUserId(), status));
    }

    @PostMapping("/order/{orderId}/ship")
    public Result<Void> ship(@PathVariable Long orderId) {
        sellerService.ship(UserContext.getUserId(), orderId);
        return Result.ok();
    }

    @PostMapping("/order/{orderId}/refund/approve")
    public Result<Void> approveRefund(@PathVariable Long orderId) {
        sellerService.approveRefund(UserContext.getUserId(), orderId);
        return Result.ok();
    }

    @PostMapping("/order/{orderId}/refund/reject")
    public Result<Void> rejectRefund(@PathVariable Long orderId) {
        sellerService.rejectRefund(UserContext.getUserId(), orderId);
        return Result.ok();
    }

    // ==================== 数据统计 ====================

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(sellerService.stats(UserContext.getUserId()));
    }

    // ==================== 评价 ====================

    @GetMapping("/review/list")
    public Result<List<ProductReview>> reviews() {
        return Result.ok(sellerService.listReviews(UserContext.getUserId()));
    }

    @PostMapping("/review/{reviewId}/reply")
    public Result<Void> replyReview(@PathVariable Long reviewId, @RequestBody Map<String, String> body) {
        sellerService.replyReview(UserContext.getUserId(), reviewId, body.get("reply"));
        return Result.ok();
    }

    /**
     * AI 生成评价回复草稿
     */
    @PostMapping("/review/{reviewId}/ai-reply")
    public Result<Map<String, String>> aiReplyReview(@PathVariable Long reviewId) {
        String reply = sellerService.aiReplyReview(UserContext.getUserId(), reviewId);
        return Result.ok(Map.of("reply", reply));
    }

    // ==================== AI 经营分析 ====================

    @GetMapping("/stats/ai-analysis")
    public Result<Map<String, String>> aiAnalysis() {
        String analysis = sellerService.aiAnalysis(UserContext.getUserId());
        return Result.ok(Map.of("analysis", analysis));
    }
}
