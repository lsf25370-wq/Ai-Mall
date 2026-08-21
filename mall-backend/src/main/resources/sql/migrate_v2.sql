-- ===== mall 增量迁移 v2：三角色体系 =====
USE mall;

-- 1. 商品表加店铺ID
ALTER TABLE product ADD COLUMN shop_id BIGINT NOT NULL DEFAULT 1 COMMENT '所属店铺ID' AFTER id;

-- 2. 店铺表
CREATE TABLE IF NOT EXISTS shop (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    logo VARCHAR(255) DEFAULT NULL,
    description VARCHAR(500) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1营业中 2已停业',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_owner (owner_user_id)
) ENGINE = InnoDB COMMENT ='店铺表';

-- 3. 评价表
CREATE TABLE IF NOT EXISTS product_review (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    order_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    shop_id BIGINT NOT NULL,
    rating INT NOT NULL DEFAULT 5,
    content VARCHAR(500) DEFAULT NULL,
    reply VARCHAR(500) DEFAULT NULL,
    reply_time DATETIME DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product (product_id),
    KEY idx_shop (shop_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT ='商品评价表';

-- 4. 收藏表
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE = InnoDB COMMENT ='商品收藏表';

-- 5. 卖家账号（{INIT} 由后端启动时加密为 123456）
INSERT INTO user (username, password, nickname, phone, level, role) VALUES
('seller1', '{INIT}', '星耀掌柜', '13700000001', 2, 2),
('seller2', '{INIT}', '云裳掌柜', '13700000002', 2, 2);

-- 6. 店铺
INSERT INTO shop (id, owner_user_id, name, logo, description, status) VALUES
(1, 4, '星耀数码旗舰店', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=store%20logo%20digital%20tech&image_size=square', '专注数码 3C 二十年，正品保障，全国联保。', 1),
(2, 5, '云裳美物生活馆', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=store%20logo%20fashion%20lifestyle&image_size=square', '服饰、食品、个护一站式生活好物，源头直供。', 1);

-- 7. 商品挂店铺（1-3 数码归店铺1，4-7 归店铺2）
UPDATE product SET shop_id = 1 WHERE id IN (1, 2, 3);
UPDATE product SET shop_id = 2 WHERE id IN (4, 5, 6, 7);

-- 8. 示例评价
INSERT INTO product_review (order_id, order_item_id, user_id, product_id, shop_id, rating, content, reply, reply_time, created_at) VALUES
(1, 1, 1, 1, 1, 5, '手机用了一周，性能很强，AI 助手很好用，物流也快。', '感谢您的支持，祝您使用愉快！', '2026-08-18 20:00:00', '2026-08-18 19:30:00'),
(1, 2, 1, 3, 1, 4, '耳机降噪效果不错，就是充电盒有点大。', NULL, NULL, '2026-08-18 19:32:00'),
(3, 4, 1, 3, 1, 5, '第二次购买了，音质一如既往的好。', NULL, NULL, '2026-08-17 16:00:00'),
(2, 3, 1, 6, 2, 5, '咖啡豆很新鲜，香气浓郁，回购！', '感谢认可，小店会持续上新优质豆子~', '2026-08-18 21:00:00', '2026-08-18 18:00:00');
