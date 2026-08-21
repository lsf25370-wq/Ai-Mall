-- ============================================================
-- 迁移 V4：多店铺拆单支持
-- 1. order 表新增 shop_id（订单归属店铺）
-- 2. 按历史订单明细回填 shop_id
-- ============================================================

ALTER TABLE `order` ADD COLUMN `shop_id` BIGINT DEFAULT NULL COMMENT '订单归属店铺ID(0=平台自营)' AFTER `user_id`;

UPDATE `order` o
SET o.shop_id = (
    SELECT p.shop_id
    FROM order_item oi
    JOIN product p ON p.id = oi.product_id
    WHERE oi.order_id = o.id
    LIMIT 1
);
