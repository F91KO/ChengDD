package com.cdd.product.error;

import com.cdd.common.core.error.ErrorCode;

public enum ProductErrorCode implements ErrorCode {
    CATEGORY_NOT_FOUND(40421, "商品分类不存在"),
    CATEGORY_NAME_DUPLICATE(40921, "同级分类名称已存在"),
    CATEGORY_DISABLED(40024, "分类已禁用，不能挂载商品"),
    CATEGORY_HAS_CHILDREN(40025, "非叶子分类不能挂载商品"),
    CATEGORY_HAS_PRODUCTS(40924, "分类下存在商品，不能禁用"),
    CATEGORY_UPDATE_EMPTY(40026, "分类更新内容不能为空"),
    CATEGORY_TEMPLATE_NOT_FOUND(40424, "分类模板不存在"),
    CATEGORY_TEMPLATE_DUPLICATE(40923, "分类模板名称与版本已存在"),
    CATEGORY_TEMPLATE_NODE_INVALID(40027, "分类模板节点配置不合法"),
    PRODUCT_NOT_FOUND(40422, "商品不存在"),
    SKU_NOT_FOUND(40423, "SKU不存在"),
    SKU_CODE_DUPLICATE(40922, "SKU编码已存在"),
    PRODUCT_CATEGORY_MISMATCH(40021, "商品与分类不匹配"),
    INVALID_STOCK_CHANGE(40022, "库存调整量非法"),
    PRODUCT_STATUS_INVALID(40023, "商品状态不允许当前操作");

    private final int code;
    private final String message;

    ProductErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
