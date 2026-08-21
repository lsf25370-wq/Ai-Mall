-- =====================================================
-- 电商商城 mall 数据库初始化脚本
-- 执行方式：mysql -uroot -p < schema.sql
-- =====================================================
CREATE DATABASE IF NOT EXISTS `mall` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `mall`;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`   VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `level`      INT          NOT NULL DEFAULT 1 COMMENT '会员等级',
    `points`     INT          NOT NULL DEFAULT 0 COMMENT '当前积分',
    `role`       TINYINT      NOT NULL DEFAULT 0 COMMENT '角色 0普通用户 1管理员',
    `deleted`    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0正常 1删除',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT ='用户表';

-- ----------------------------
-- 2. 商品分类表
-- ----------------------------
DROP TABLE IF EXISTS `product_category`;
CREATE TABLE `product_category` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`       VARCHAR(50)  NOT NULL COMMENT '分类名称',
    `icon`       VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
    `sort`       INT          NOT NULL DEFAULT 0 COMMENT '排序',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='商品分类表';

-- ----------------------------
-- 3. 商品表
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    `shop_id`     BIGINT         NOT NULL DEFAULT 1 COMMENT '所属店铺ID',
    `category_id` BIGINT         NOT NULL COMMENT '分类ID',
    `name`        VARCHAR(100)   NOT NULL COMMENT '商品名称',
    `subtitle`    VARCHAR(255)   DEFAULT NULL COMMENT '副标题',
    `main_image`  VARCHAR(255)   DEFAULT NULL COMMENT '主图',
    `detail`      TEXT COMMENT '商品详情',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '售价',
    `stock`       INT            NOT NULL DEFAULT 0 COMMENT '库存',
    `sales`       INT            NOT NULL DEFAULT 0 COMMENT '销量',
    `status`      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
    `deleted`     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    `created_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`)
) ENGINE = InnoDB COMMENT ='商品表';

-- ----------------------------
-- 4. 收货地址表
-- ----------------------------
DROP TABLE IF EXISTS `address`;
CREATE TABLE `address` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '地址ID',
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `receiver`   VARCHAR(50)  NOT NULL COMMENT '收货人',
    `phone`      VARCHAR(20)  NOT NULL COMMENT '联系电话',
    `province`   VARCHAR(50)  NOT NULL COMMENT '省',
    `city`       VARCHAR(50)  NOT NULL COMMENT '市',
    `district`   VARCHAR(50)  NOT NULL COMMENT '区',
    `detail`     VARCHAR(255) NOT NULL COMMENT '详细地址',
    `is_default` TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认 0否 1是',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='收货地址表';

-- ----------------------------
-- 5. 购物车表
-- ----------------------------
DROP TABLE IF EXISTS `cart_item`;
CREATE TABLE `cart_item` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '购物车项ID',
    `user_id`       BIGINT         NOT NULL COMMENT '用户ID',
    `product_id`    BIGINT         NOT NULL COMMENT '商品ID',
    `product_name`  VARCHAR(100)   NOT NULL COMMENT '商品名称(快照)',
    `product_image` VARCHAR(255)   DEFAULT NULL COMMENT '商品图片(快照)',
    `price`         DECIMAL(10, 2) NOT NULL COMMENT '单价(快照)',
    `quantity`      INT            NOT NULL DEFAULT 1 COMMENT '数量',
    `checked`       TINYINT        NOT NULL DEFAULT 1 COMMENT '是否选中 0否 1是',
    `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE = InnoDB COMMENT ='购物车表';

-- ----------------------------
-- 6. 订单表
-- ----------------------------
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id`               BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`         VARCHAR(32)    NOT NULL COMMENT '订单号',
    `user_id`          BIGINT         NOT NULL COMMENT '用户ID',
    `shop_id`          BIGINT         DEFAULT NULL COMMENT '订单归属店铺ID(0=平台自营)',
    `coupon_id`        BIGINT         DEFAULT NULL COMMENT '使用的优惠券ID',
    `total_amount`     DECIMAL(10, 2) NOT NULL COMMENT '商品总额',
    `discount_amount`  DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
    `points_earned`    INT            NOT NULL DEFAULT 0 COMMENT '可获得积分',
    `pay_amount`       DECIMAL(10, 2) NOT NULL COMMENT '实付金额',
    `status`           TINYINT        NOT NULL DEFAULT 0 COMMENT '状态 0待付款 1已付款 2已发货 3已完成 4已取消 5退款中 6已退款',
    `address_snapshot` VARCHAR(500)   NOT NULL COMMENT '收货地址快照',
    `pay_time`         DATETIME       DEFAULT NULL COMMENT '支付时间',
    `ship_time`        DATETIME       DEFAULT NULL COMMENT '发货时间',
    `confirm_time`     DATETIME       DEFAULT NULL COMMENT '确认收货时间',
    `cancel_time`      DATETIME       DEFAULT NULL COMMENT '取消时间',
    `refund_time`      DATETIME       DEFAULT NULL COMMENT '退款时间',
    `created_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='订单表';

-- ----------------------------
-- 7. 订单明细表
-- ----------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`      BIGINT         NOT NULL COMMENT '订单ID',
    `order_no`      VARCHAR(32)    NOT NULL COMMENT '订单号',
    `product_id`    BIGINT         NOT NULL COMMENT '商品ID',
    `product_name`  VARCHAR(100)   NOT NULL COMMENT '商品名称(快照)',
    `product_image` VARCHAR(255)   DEFAULT NULL COMMENT '商品图片(快照)',
    `price`         DECIMAL(10, 2) NOT NULL COMMENT '成交单价(快照)',
    `quantity`      INT            NOT NULL COMMENT '数量',
    `total_price`   DECIMAL(10, 2) NOT NULL COMMENT '小计(快照)',
    `created_at`    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`)
) ENGINE = InnoDB COMMENT ='订单明细表';

-- ----------------------------
-- 8. AI 客服会话表
-- ----------------------------
DROP TABLE IF EXISTS `ai_session`;
CREATE TABLE `ai_session` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    `session_id` VARCHAR(64)  NOT NULL COMMENT '会话编号',
    `user_id`    BIGINT       NOT NULL COMMENT '用户ID',
    `title`      VARCHAR(100) DEFAULT '新会话' COMMENT '会话标题',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session` (`session_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='AI客服会话表';

-- ----------------------------
-- 9. AI 消息表
-- ----------------------------
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `session_id` VARCHAR(64) NOT NULL COMMENT '会话编号',
    `user_id`    BIGINT      NOT NULL COMMENT '用户ID',
    `role`       VARCHAR(10) NOT NULL COMMENT '角色 user/assistant',
    `content`    TEXT        NOT NULL COMMENT '消息内容',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_session` (`session_id`)
) ENGINE = InnoDB COMMENT ='AI客服消息表';

-- ----------------------------
-- 10. FAQ 知识库表
-- ----------------------------
DROP TABLE IF EXISTS `faq_doc`;
CREATE TABLE `faq_doc` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `title`      VARCHAR(200) NOT NULL COMMENT '标题',
    `content`    TEXT        NOT NULL COMMENT '内容',
    `category`   VARCHAR(50) DEFAULT '售后' COMMENT '分类',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='FAQ知识库表';

-- ----------------------------
-- 11. 店铺表（卖家）
-- ----------------------------
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '店铺ID',
    `owner_user_id` BIGINT       NOT NULL COMMENT '店主用户ID',
    `name`          VARCHAR(100) NOT NULL COMMENT '店铺名称',
    `logo`          VARCHAR(255) DEFAULT NULL COMMENT '店铺LOGO',
    `description`   VARCHAR(500) DEFAULT NULL COMMENT '店铺简介',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1营业中 2已停业',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_user_id`)
) ENGINE = InnoDB COMMENT ='店铺表';

-- ----------------------------
-- 12. 商品评价表
-- ----------------------------
DROP TABLE IF EXISTS `product_review`;
CREATE TABLE `product_review` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`      BIGINT       NOT NULL COMMENT '订单ID',
    `order_item_id` BIGINT       NOT NULL COMMENT '订单明细ID',
    `user_id`       BIGINT       NOT NULL COMMENT '评价用户ID',
    `product_id`    BIGINT       NOT NULL COMMENT '商品ID',
    `shop_id`       BIGINT       NOT NULL COMMENT '店铺ID',
    `rating`        INT          NOT NULL DEFAULT 5 COMMENT '评分 1-5',
    `content`       VARCHAR(500) DEFAULT NULL COMMENT '评价内容',
    `reply`         VARCHAR(500) DEFAULT NULL COMMENT '卖家回复',
    `reply_time`    DATETIME     DEFAULT NULL COMMENT '回复时间',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    KEY `idx_product` (`product_id`),
    KEY `idx_shop` (`shop_id`),
    KEY `idx_user` (`user_id`)
) ENGINE = InnoDB COMMENT ='商品评价表';

-- ----------------------------
-- 13. 商品收藏表（买家）
-- ----------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
    `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
    `product_id` BIGINT   NOT NULL COMMENT '商品ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_product` (`user_id`, `product_id`)
) ENGINE = InnoDB COMMENT ='商品收藏表';

-- ----------------------------
-- 14. 秒杀活动表
-- ----------------------------
DROP TABLE IF EXISTS `seckill_activity`;
CREATE TABLE `seckill_activity` (
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

-- ----------------------------
-- 15. 优惠券模板表
-- ----------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    `name`           VARCHAR(100)   NOT NULL COMMENT '券名称',
    `type`           TINYINT        NOT NULL DEFAULT 1 COMMENT '类型 1满减券 2折扣券',
    `threshold`      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '使用门槛金额(满X可用)',
    `amount`         DECIMAL(10, 2) DEFAULT NULL COMMENT '满减金额(满减券)',
    `discount`       DECIMAL(3, 2)  DEFAULT NULL COMMENT '折扣率 0.95=95折(折扣券)',
    `shop_id`        BIGINT         DEFAULT NULL COMMENT '适用店铺ID(NULL=全场通用)',
    `total_count`    INT            NOT NULL DEFAULT 0 COMMENT '发行总量',
    `claimed_count`  INT            NOT NULL DEFAULT 0 COMMENT '已领取数量',
    `per_user_limit` INT            NOT NULL DEFAULT 1 COMMENT '每人限领',
    `valid_days`     INT            NOT NULL DEFAULT 7 COMMENT '领取后有效天数',
    `status`         TINYINT        NOT NULL DEFAULT 1 COMMENT '状态 1上架 0下架',
    `created_at`     DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB COMMENT ='优惠券模板表';

-- ----------------------------
-- 16. 用户优惠券表
-- ----------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
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

-- ----------------------------
-- 17. 积分流水表
-- ----------------------------
DROP TABLE IF EXISTS `points_log`;
CREATE TABLE `points_log` (
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

-- =====================================================
-- 示例数据
-- =====================================================

-- 测试用户（密码占位符 {INIT} 由后端启动时自动加密为 123456）
INSERT INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `level`, `role`) VALUES
(1, 'zhangsan', '{INIT}', '张三', '13800000001', 2, 0),
(2, 'lisi',     '{INIT}', '李四', '13800000002', 1, 0),
(3, 'admin',    '{INIT}', '商城管理员', '13900000000', 3, 1),
(4, 'seller1',  '{INIT}', '星耀掌柜', '13700000001', 2, 2),
(5, 'seller2',  '{INIT}', '云裳掌柜', '13700000002', 2, 2);

-- 店铺
INSERT INTO `shop` (`id`, `owner_user_id`, `name`, `logo`, `description`, `status`) VALUES
(1, 4, '星耀数码旗舰店', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=store%20logo%20digital%20tech&image_size=square', '专注数码 3C 二十年，正品保障，全国联保。', 1),
(2, 5, '云裳美物生活馆', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=store%20logo%20fashion%20lifestyle&image_size=square', '服饰、食品、个护一站式生活好物，源头直供。', 1);

-- 商品分类
INSERT INTO `product_category` (`id`, `name`, `sort`) VALUES
(1, '手机数码', 1),
(2, '服饰鞋包', 2),
(3, '食品生鲜', 3),
(4, '美妆个护', 4);

-- 商品
INSERT INTO `product` (`id`, `shop_id`, `category_id`, `name`, `subtitle`, `main_image`, `detail`, `price`, `stock`, `sales`) VALUES
(1, 1, 1, 'AI智能手机 Pro Max', '旗舰影像，AI 智能助手', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=smartphone%20product%20photo%20white%20background&image_size=square', '2026 年度旗舰机型，搭载自研 AI 芯片，支持端侧大模型推理。', 6999.00, 100, 520),
(2, 1, 1, '轻薄笔记本电脑 14', '2.8K 屏，超长续航', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=laptop%20product%20photo%20white%20background&image_size=square', '14 英寸 2.8K 高刷屏，56Wh 大电池，支持 AI 会议纪要功能。', 5499.00, 80, 310),
(3, 1, 1, '真无线降噪耳机', '主动降噪，40 小时续航', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=wireless%20earbuds%20product%20photo%20white%20background&image_size=square', '自适应主动降噪，支持 AI 通话降噪，单次续航 10 小时。', 899.00, 200, 1200),
(4, 2, 2, '纯棉基础款 T 恤', '新疆长绒棉，透气亲肤', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=t-shirt%20product%20photo%20white%20background&image_size=square', '100% 新疆长绒棉，舒适透气，多色可选。', 79.00, 500, 2300),
(5, 2, 2, '轻便跑步运动鞋', '回弹缓震，透气网面', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=running%20shoes%20product%20photo%20white%20background&image_size=square', '中底回弹科技，人体工学设计，日常通勤跑步皆宜。', 299.00, 300, 860),
(6, 2, 3, '云南高山咖啡豆 500g', '阿拉比卡，中度烘焙', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=coffee%20beans%20product%20photo%20white%20background&image_size=square', '云南保山高海拔产区，中度烘焙，坚果与焦糖风味。', 128.00, 1000, 4500),
(7, 2, 4, '玻尿酸保湿精华 30ml', '深层补水，敏感肌适用', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=serum%20bottle%20product%20photo%20white%20background&image_size=square', '4 重玻尿酸分子，锁水保湿，舒缓敏感肌。', 259.00, 400, 1800);

-- 示例收货地址
INSERT INTO `address` (`user_id`, `receiver`, `phone`, `province`, `city`, `district`, `detail`, `is_default`) VALUES
(1, '张三', '13800000001', '广东省', '深圳市', '南山区', '科技园南区 1 栋 202', 1),
(1, '张三', '13800000001', '广东省', '深圳市', '福田区', '福田中心区 88 号', 0);

-- 示例订单（给 AI 客服演示用）
INSERT INTO `order` (`id`, `order_no`, `user_id`, `total_amount`, `pay_amount`, `status`, `address_snapshot`, `pay_time`, `ship_time`) VALUES
(1, 'ORD202608180001', 1, 7898.00, 7898.00, 2, '张三 13800000001 广东省深圳市南山区科技园南区 1 栋 202', '2026-08-18 10:20:00', '2026-08-18 15:30:00'),
(2, 'ORD202608180002', 1, 128.00, 128.00, 1, '张三 13800000001 广东省深圳市南山区科技园南区 1 栋 202', '2026-08-18 11:05:00', NULL),
(3, 'ORD202608170003', 1, 899.00, 899.00, 3, '张三 13800000001 广东省深圳市福田区福田中心区 88 号', '2026-08-17 09:00:00', '2026-08-17 14:00:00');

INSERT INTO `order_item` (`order_id`, `order_no`, `product_id`, `product_name`, `product_image`, `price`, `quantity`, `total_price`) VALUES
(1, 'ORD202608180001', 1, 'AI智能手机 Pro Max', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=smartphone%20product%20photo%20white%20background&image_size=square', 6999.00, 1, 6999.00),
(1, 'ORD202608180001', 3, '真无线降噪耳机', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=wireless%20earbuds%20product%20photo%20white%20background&image_size=square', 899.00, 1, 899.00),
(2, 'ORD202608180002', 6, '云南高山咖啡豆 500g', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=coffee%20beans%20product%20photo%20white%20background&image_size=square', 128.00, 1, 128.00),
(3, 'ORD202608170003', 3, '真无线降噪耳机', 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=wireless%20earbuds%20product%20photo%20white%20background&image_size=square', 899.00, 1, 899.00);

-- FAQ 知识库
INSERT INTO `faq_doc` (`title`, `content`, `category`) VALUES
('七天无理由退换货政策', '自签收之日起 7 天内，商品不影响二次销售（吊牌完整、包装完好、未使用），支持无理由退换货。部分商品（如定制类、生鲜类）不支持无理由退货。', '售后'),
('退换货申请流程', '在订单详情页点击"申请售后"，选择退换货类型并填写原因，审核通过后可将商品寄回指定地址，我们将在收到商品并验收后 1-3 个工作日内完成退款原路返回。', '售后'),
('如何查询订单物流', '在"我的订单"页面点击订单进入详情，即可查看物流轨迹。您也可以直接询问 AI 客服，说出订单号即可为您查询最新物流状态。', '售后'),
('配送时间说明', '现货商品一般下单后 48 小时内发货，偏远地区到货时间可能延迟 1-2 天。生鲜类商品使用冷链配送，工作日 9:00-18:00 派送。', '配送'),
('发票开具说明', '下单时可在结算页选择开具电子发票，订单完成后将自动发送至您的邮箱；如需纸质发票请联系客服补开。', '售后'),
('会员等级权益', '普通会员享 99 折；银卡会员享 98 折并赠送生日礼包；金卡会员享 95 折、专属客服及每月免运费券。', '会员');

-- 秒杀活动（示例：耳机秒杀，开始时间早于当前、结束于一天后，保证演示时处于进行中）
INSERT INTO `seckill_activity`
    (`product_id`, `product_name`, `product_image`, `seckill_price`, `total_stock`, `sold_count`, `start_time`, `end_time`, `status`)
SELECT 3, name, main_image, 499.00, 50, 0, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 DAY), 1
FROM product WHERE id = 3;

-- 优惠券
INSERT INTO `coupon` (`name`, `type`, `threshold`, `amount`, `discount`, `shop_id`, `total_count`, `claimed_count`, `per_user_limit`, `valid_days`, `status`) VALUES
('新人立减 10 元券',   1, 50.00,  10.00, NULL, NULL, 1000, 0, 1, 7, 1),
('满 300 减 30 券',    1, 300.00, 30.00, NULL, NULL, 500,  0, 1, 7, 1),
('全场 95 折券',       2, 0,      NULL,  0.95, NULL, 800,  0, 1, 3, 1),
('星耀数码满 1000 减 100', 1, 1000.00, 100.00, NULL, 1, 200, 0, 1, 7, 1);
