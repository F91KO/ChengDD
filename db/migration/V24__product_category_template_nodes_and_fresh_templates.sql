SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `cdd_product_category_template_node` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `template_id` bigint NOT NULL COMMENT '模板ID',
  `template_category_code` varchar(64) NOT NULL COMMENT '模板分类编码',
  `parent_template_category_code` varchar(64) DEFAULT NULL COMMENT '父级模板分类编码',
  `category_name` varchar(128) NOT NULL COMMENT '分类名称',
  `category_level` int NOT NULL DEFAULT '1' COMMENT '分类层级',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `is_enabled` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否启用',
  `is_visible` tinyint(1) NOT NULL DEFAULT '1' COMMENT '前台是否可见',
  `status` varchar(32) NOT NULL DEFAULT 'enabled' COMMENT '节点状态',
  `created_by` bigint DEFAULT NULL COMMENT '创建人',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标记',
  `version` bigint NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_node_code_deleted` (`template_id`, `template_category_code`, `deleted`),
  KEY `idx_template_parent_sort` (`template_id`, `parent_template_category_code`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台分类模板节点表';

INSERT INTO `cdd_product_category_template` (
  `id`, `template_name`, `industry_code`, `template_version`, `max_level`, `status`,
  `template_desc`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  2000002, '品质精选生鲜模板', 'fresh_retail', 'v1.0.0', 3, 'recommended',
  '适合品质生鲜、鲜切半成品和餐桌场景经营的即时零售模板', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_category_template` WHERE `id` = 2000002 AND `deleted` = 0
);

INSERT INTO `cdd_product_category_template` (
  `id`, `template_name`, `industry_code`, `template_version`, `max_level`, `status`,
  `template_desc`, `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  2000003, '社区民生到家模板', 'community_fresh', 'v1.0.0', 3, 'recommended',
  '适合社区到家、家庭高频补货和民生商品经营的分类模板', 1001, 1001, 0, 0
FROM DUAL
WHERE NOT EXISTS (
  SELECT 1 FROM `cdd_product_category_template` WHERE `id` = 2000003 AND `deleted` = 0
);

INSERT INTO `cdd_product_category_template_node` (
  `id`, `template_id`, `template_category_code`, `parent_template_category_code`, `category_name`,
  `category_level`, `sort_order`, `is_enabled`, `is_visible`, `status`,
  `created_by`, `updated_by`, `deleted`, `version`
)
SELECT
  `seed`.`id`, `seed`.`template_id`, `seed`.`template_category_code`, `seed`.`parent_template_category_code`, `seed`.`category_name`,
  `seed`.`category_level`, `seed`.`sort_order`, `seed`.`is_enabled`, `seed`.`is_visible`, `seed`.`status`,
  1001, 1001, 0, 0
FROM (
  SELECT 2050001 AS `id`, 2000001 AS `template_id`, 'fresh' AS `template_category_code`, NULL AS `parent_template_category_code`, '生鲜' AS `category_name`, 1 AS `category_level`, 10 AS `sort_order`, 1 AS `is_enabled`, 1 AS `is_visible`, 'enabled' AS `status`
  UNION ALL SELECT 2050002, 2000001, 'fresh-fruit', 'fresh', '水果', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050003, 2000001, 'fresh-vegetable', 'fresh', '蔬菜', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050004, 2000001, 'drink', NULL, '酒水饮料', 1, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050005, 2000001, 'drink-tea', 'drink', '茶饮', 2, 10, 1, 1, 'enabled'

  UNION ALL SELECT 2050101, 2000002, 'hema_vegetable', NULL, '蔬菜豆制品', 1, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050102, 2000002, 'hema_vegetable_leaf', 'hema_vegetable', '叶菜类', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050103, 2000002, 'hema_vegetable_root', 'hema_vegetable', '根茎类', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050104, 2000002, 'hema_vegetable_mushroom', 'hema_vegetable', '菌菇类', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050105, 2000002, 'hema_vegetable_tofu', 'hema_vegetable', '豆制品', 2, 40, 1, 1, 'enabled'
  UNION ALL SELECT 2050106, 2000002, 'hema_fruit', NULL, '水果鲜切', 1, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050107, 2000002, 'hema_fruit_seasonal', 'hema_fruit', '应季水果', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050108, 2000002, 'hema_fruit_import', 'hema_fruit', '进口水果', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050109, 2000002, 'hema_fruit_cut', 'hema_fruit', '鲜切果盒', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050110, 2000002, 'hema_meat', NULL, '肉禽蛋品', 1, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050111, 2000002, 'hema_meat_pork', 'hema_meat', '猪肉', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050112, 2000002, 'hema_meat_beef', 'hema_meat', '牛羊肉', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050113, 2000002, 'hema_meat_poultry', 'hema_meat', '鸡鸭禽类', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050114, 2000002, 'hema_meat_egg', 'hema_meat', '鲜蛋蛋品', 2, 40, 1, 1, 'enabled'
  UNION ALL SELECT 2050115, 2000002, 'hema_seafood', NULL, '海鲜水产', 1, 40, 1, 1, 'enabled'
  UNION ALL SELECT 2050116, 2000002, 'hema_seafood_live', 'hema_seafood', '活鲜水产', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050117, 2000002, 'hema_seafood_frozen', 'hema_seafood', '冷鲜海产', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050118, 2000002, 'hema_seafood_shell', 'hema_seafood', '贝类虾蟹', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050119, 2000002, 'hema_bakery', NULL, '乳品烘焙', 1, 50, 1, 1, 'enabled'
  UNION ALL SELECT 2050120, 2000002, 'hema_bakery_milk', 'hema_bakery', '鲜奶酸奶', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050121, 2000002, 'hema_bakery_bread', 'hema_bakery', '面包蛋糕', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050122, 2000002, 'hema_bakery_breakfast', 'hema_bakery', '早餐轻食', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050123, 2000002, 'hema_drink', NULL, '酒水饮料', 1, 60, 1, 1, 'enabled'
  UNION ALL SELECT 2050124, 2000002, 'hema_drink_ready', 'hema_drink', '果汁饮品', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050125, 2000002, 'hema_drink_tea', 'hema_drink', '茶咖冲调', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050126, 2000002, 'hema_drink_wine', 'hema_drink', '啤酒葡萄酒', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050127, 2000002, 'hema_grain', NULL, '粮油调味', 1, 70, 1, 1, 'enabled'
  UNION ALL SELECT 2050128, 2000002, 'hema_grain_rice', 'hema_grain', '大米杂粮', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050129, 2000002, 'hema_grain_oil', 'hema_grain', '食用油', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050130, 2000002, 'hema_grain_sauce', 'hema_grain', '调味酱料', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050131, 2000002, 'hema_fastfood', NULL, '方便速食', 1, 80, 1, 1, 'enabled'
  UNION ALL SELECT 2050132, 2000002, 'hema_fastfood_frozen', 'hema_fastfood', '速冻面点', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050133, 2000002, 'hema_fastfood_prepared', 'hema_fastfood', '预制菜', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050134, 2000002, 'hema_fastfood_ready', 'hema_fastfood', '即食熟食', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050135, 2000002, 'hema_snack', NULL, '休闲零食', 1, 90, 1, 1, 'enabled'
  UNION ALL SELECT 2050136, 2000002, 'hema_snack_nut', 'hema_snack', '坚果果干', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050137, 2000002, 'hema_snack_cookie', 'hema_snack', '糖巧饼干', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050138, 2000002, 'hema_snack_puff', 'hema_snack', '膨化零食', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050139, 2000002, 'hema_daily', NULL, '日清百货', 1, 100, 1, 1, 'enabled'
  UNION ALL SELECT 2050140, 2000002, 'hema_daily_kitchen', 'hema_daily', '厨房用品', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050141, 2000002, 'hema_daily_clean', 'hema_daily', '清洁纸品', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050142, 2000002, 'hema_daily_care', 'hema_daily', '日化洗护', 2, 30, 1, 1, 'enabled'

  UNION ALL SELECT 2050201, 2000003, 'qixian_vegetable', NULL, '新鲜蔬菜', 1, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050202, 2000003, 'qixian_vegetable_leaf', 'qixian_vegetable', '家常叶菜', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050203, 2000003, 'qixian_vegetable_root', 'qixian_vegetable', '根茎瓜果', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050204, 2000003, 'qixian_vegetable_mushroom', 'qixian_vegetable', '菌菇豆品', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050205, 2000003, 'qixian_vegetable_spice', 'qixian_vegetable', '葱姜蒜椒', 2, 40, 1, 1, 'enabled'
  UNION ALL SELECT 2050206, 2000003, 'qixian_fruit', NULL, '水果优选', 1, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050207, 2000003, 'qixian_fruit_seasonal', 'qixian_fruit', '当季水果', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050208, 2000003, 'qixian_fruit_daily', 'qixian_fruit', '日常常备水果', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050209, 2000003, 'qixian_fruit_import', 'qixian_fruit', '高端进口水果', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050210, 2000003, 'qixian_meat', NULL, '肉蛋家禽', 1, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050211, 2000003, 'qixian_meat_pork', 'qixian_meat', '猪肉', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050212, 2000003, 'qixian_meat_beef', 'qixian_meat', '牛羊肉', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050213, 2000003, 'qixian_meat_poultry', 'qixian_meat', '鸡鸭禽类', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050214, 2000003, 'qixian_meat_egg', 'qixian_meat', '鸡蛋鸭蛋', 2, 40, 1, 1, 'enabled'
  UNION ALL SELECT 2050215, 2000003, 'qixian_seafood', NULL, '水产海鲜', 1, 40, 1, 1, 'enabled'
  UNION ALL SELECT 2050216, 2000003, 'qixian_seafood_live', 'qixian_seafood', '鲜活水产', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050217, 2000003, 'qixian_seafood_frozen', 'qixian_seafood', '冷冻海鲜', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050218, 2000003, 'qixian_seafood_shell', 'qixian_seafood', '虾蟹贝类', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050219, 2000003, 'qixian_breakfast', NULL, '早餐乳品', 1, 50, 1, 1, 'enabled'
  UNION ALL SELECT 2050220, 2000003, 'qixian_breakfast_milk', 'qixian_breakfast', '牛奶酸奶', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050221, 2000003, 'qixian_breakfast_bread', 'qixian_breakfast', '面包吐司', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050222, 2000003, 'qixian_breakfast_dimsum', 'qixian_breakfast', '包点面食', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050223, 2000003, 'qixian_grain', NULL, '米面粮油', 1, 60, 1, 1, 'enabled'
  UNION ALL SELECT 2050224, 2000003, 'qixian_grain_rice', 'qixian_grain', '大米面粉', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050225, 2000003, 'qixian_grain_dry', 'qixian_grain', '杂粮干货', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050226, 2000003, 'qixian_grain_oil', 'qixian_grain', '食用油', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050227, 2000003, 'qixian_fastfood', NULL, '速食熟食', 1, 70, 1, 1, 'enabled'
  UNION ALL SELECT 2050228, 2000003, 'qixian_fastfood_frozen', 'qixian_fastfood', '速冻食品', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050229, 2000003, 'qixian_fastfood_ready', 'qixian_fastfood', '即食熟食', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050230, 2000003, 'qixian_fastfood_prepared', 'qixian_fastfood', '预制快手菜', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050231, 2000003, 'qixian_drink', NULL, '酒水饮料', 1, 80, 1, 1, 'enabled'
  UNION ALL SELECT 2050232, 2000003, 'qixian_drink_family', 'qixian_drink', '家庭饮料', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050233, 2000003, 'qixian_drink_tea', 'qixian_drink', '茶饮冲调', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050234, 2000003, 'qixian_drink_wine', 'qixian_drink', '啤酒洋酒', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050235, 2000003, 'qixian_snack', NULL, '零食百货', 1, 90, 1, 1, 'enabled'
  UNION ALL SELECT 2050236, 2000003, 'qixian_snack_food', 'qixian_snack', '休闲零食', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050237, 2000003, 'qixian_snack_home', 'qixian_snack', '家庭囤货', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050238, 2000003, 'qixian_snack_clean', 'qixian_snack', '清洁纸品', 2, 30, 1, 1, 'enabled'
  UNION ALL SELECT 2050239, 2000003, 'qixian_kitchen', NULL, '厨房日用', 1, 100, 1, 1, 'enabled'
  UNION ALL SELECT 2050240, 2000003, 'qixian_kitchen_storage', 'qixian_kitchen', '保鲜收纳', 2, 10, 1, 1, 'enabled'
  UNION ALL SELECT 2050241, 2000003, 'qixian_kitchen_tool', 'qixian_kitchen', '厨房小工具', 2, 20, 1, 1, 'enabled'
  UNION ALL SELECT 2050242, 2000003, 'qixian_kitchen_daily', 'qixian_kitchen', '日常消耗品', 2, 30, 1, 1, 'enabled'
) AS `seed`
WHERE NOT EXISTS (
  SELECT 1
  FROM `cdd_product_category_template_node`
  WHERE `template_id` = `seed`.`template_id`
    AND `template_category_code` = `seed`.`template_category_code`
    AND `deleted` = 0
);
