-- ============================================================
-- 迁移 V5：秒杀活动 + 优惠券 + 积分/会员等级
-- 说明：新表用 CREATE TABLE IF NOT EXISTS，新列用存储过程保证可重复执行
-- ============================================================
USE `mall`;

-- 1. 秒杀活动表
CREATE TABLE IF NOT EXISTS `seckill_activity` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    `product_id`    BIGINT         NOT NULL COMMENT '秒杀商品ID',
    `product_name`  VARCHAR(100)   DEFAULT NULL COMMENT '商品名称(冗余)',
    `product_image` VARCHAR(255)   DEFAULT NULL COMMENT '商品主图(冗余)',
    `seckill_price` DECIMAL(10, 2) NOT NULL COMMENT '秒杀价',
    `total_stock`   INT            NOT NULL DEFAULT 0 COMMENT '秒杀总库存',
    `sold_count`    INT            NOT NULL DEFAULT 0 COMMENT '已秒数量',
    `start_time`    DATETIME       NOT NULL COMMENT '开始时间',
    `end_time`      DATETIME       NOT NULL COMMENT '结束时间',
    `status`        TINYINT        NOT NULL DEFAULT 0 COMMENT '状态 0未开始 1进行中 2已结束 3已下架',
    `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_time` (`start_time`, `end_time`)
) ENGINE = InnoDB COMMENT ='秒杀活动表';

-- 2. 优惠券模板表
CREATE TABLE IF NOT EXISTS `coupon` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `name`            VARCHAR(100)   NOT NULL COMMENT '券名称',
    `type`            TINYINT        NOT NULL DEFAULT 1 COMMENT '类型 1满减券 2折扣券',
    `threshold`       DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '使用门槛金额(满X可用)',
    `amount`          DECIMAL(10, 2) DEFAULT NULL COMMENT '满减金额(满减券)',
    `discount`        DECIMAL(3, 2)  DEFAULT NULL COMMENT '折扣率 0.95=95折(折扣券)',
    `shop_id`         BIGINT         DEFAULT NULL COMMENT '适用店铺ID(NULL=全场通用)',
    `total_count`     INT            NOT NULL DEFAULT 0 COMMENT '发行总量',
    `claimed_count`   INT            NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `per_user_limit`  INT            NOT NULL DEFAULT 1 COMMENT '每人限领',
    `valid_days`      INT            NOT NULL DEFAULT 7 COMMENT '领取后有效天数',
    `status`          TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='优惠券模板表';

-- 3. 用户优惠券表
CREATE TABLE IF NOT EXISTS `user_coupon` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `coupon_id`   BIGINT      NOT NULL COMMENT '优惠券ID',
    `order_id`    BIGINT      DEFAULT NULL COMMENT '使用订单ID',
    `status`      TINYINT     NOT NULL DEFAULT 0 COMMENT '状态 0未使用 1已使用 2已过期',
    `expire_time` DATETIME    NOT NULL COMMENT '过期时间',
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_coupon` (`coupon_id`)
) ENGINE = InnoDB COMMENT ='用户优惠券表';

-- 4. 积分流水表
CREATE TABLE IF NOT EXISTS `points_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `change`     INT          NOT NULL COMMENT '积分变动(正=获得 负=消耗)',
    `balance`    INT          NOT NULL DEFAULT 0 COMMENT '变动后余额',
    `type`       TINYINT      NOT NULL DEFAULT 1 COMMENT '类型 1订单获得 2消费抵扣 3手动调整',
    `reason`     VARCHAR(255) DEFAULT NULL COMMENT '变动原因',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变动时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='积分流水表';

-- 5. user 表增加积分字段（存储过程保证幂等）
DROP PROCEDURE IF EXISTS `mall_add_col_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `mall_add_col_if_not_exists`(
    IN tbl VARCHAR(64), IN col VARCHAR(64), IN ddl VARCHAR(255))
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = tbl AND COLUMN_NAME = col) THEN
        SET @s = CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN ', ddl);
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL `mall_add_col_if_not_exists`('user', 'points', '`points` INT NOT NULL DEFAULT 0 COMMENT ''当前积分'' AFTER `level`');

-- 6. order 表增加优惠券/积分相关字段
CALL `mall_add_col_if_not_exists`('order', 'coupon_id', '`coupon_id` BIGINT DEFAULT NULL COMMENT ''使用的优惠券ID'' AFTER `shop_id`');
CALL `mall_add_col_if_not_exists`('order', 'discount_amount', '`discount_amount` DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT ''优惠金额'' AFTER `total_amount`');
CALL `mall_add_col_if_not_exists`('order', 'points_earned', '`points_earned` INT NOT NULL DEFAULT 0 COMMENT ''可获得积分'' AFTER `discount_amount`');

DROP PROCEDURE IF EXISTS `mall_add_col_if_not_exists`;

-- ============================================================
-- 演示数据
-- ============================================================

-- 秒杀活动（示例：耳机秒杀，开始时间早于当前、结束于一天后，保证演示时处于进行中）
INSERT INTO `seckill_activity`
    (`product_id`, `product_name`, `product_image`, `seckill_price`, `total_stock`, `sold_count`, `start_time`, `end_time`, `status`)
SELECT 3, name, main_image, 499.00, 50, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), 1
FROM product WHERE id = 3
AND NOT EXISTS (SELECT 1 FROM seckill_activity WHERE product_id = 3);

-- 优惠券
INSERT INTO `coupon` (`name`, `type`, `threshold`, `amount`, `discount`, `shop_id`, `total_count`, `claimed_count`, `per_user_limit`, `valid_days`, `status`) VALUES
('新人立减 10 元券',   1, 50.00,  10.00, NULL, NULL, 1000, 0, 1, 7, 1),
('满 300 减 30 券',    1, 300.00, 30.00, NULL, NULL, 500,  0, 1, 7, 1),
('全场 95 折券',       2, 0,      NULL,  0.95, NULL, 800,  0, 1, 3, 1),
('星耀数码满 1000 减 100', 1, 1000.00, 100.00, NULL, 1, 200, 0, 1, 7, 1);
