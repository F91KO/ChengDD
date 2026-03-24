SET NAMES utf8mb4;

UPDATE `cdd_product_category_template`
SET
  `template_name` = '品质精选生鲜模板',
  `template_desc` = '适合品质生鲜、鲜切半成品和餐桌场景经营的即时零售模板'
WHERE `id` = 2000002
  AND `deleted` = 0;

UPDATE `cdd_product_category_template`
SET
  `template_name` = '社区民生到家模板',
  `template_desc` = '适合社区到家、家庭高频补货和民生商品经营的分类模板'
WHERE `id` = 2000003
  AND `deleted` = 0;
