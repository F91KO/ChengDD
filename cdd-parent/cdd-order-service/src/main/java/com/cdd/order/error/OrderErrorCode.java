package com.cdd.order.error;

import com.cdd.common.core.error.ErrorCode;

public enum OrderErrorCode implements ErrorCode {
    CART_EMPTY(40031, "购物车中没有可结算商品"),
    CHECKOUT_SNAPSHOT_EXPIRED(40032, "结算快照已过期"),
    CHECKOUT_SNAPSHOT_INVALID(40033, "结算快照数据异常"),
    ORDER_STATUS_INVALID(40034, "当前订单状态不允许该操作"),
    DELIVERY_STATUS_INVALID(40035, "履约状态不合法"),
    PAY_CALLBACK_STATUS_INVALID(40036, "支付回调状态不合法"),
    REFUND_STATUS_INVALID(40037, "退款状态不合法"),
    REFUND_AMOUNT_INVALID(40038, "退款金额不合法"),
    AFTER_SALE_STATUS_INVALID(40039, "售后状态不合法"),
    AFTER_SALE_TYPE_INVALID(40040, "售后类型不合法"),
    REFUND_ITEM_AMOUNT_INVALID(40041, "订单项退款金额不合法"),
    REFUND_ITEM_QUANTITY_INVALID(40042, "订单项退款数量不合法"),
    RETURN_LOGISTICS_REQUIRED(40043, "退货物流信息不能为空"),
    CHECKOUT_SNAPSHOT_NOT_FOUND(40431, "结算快照不存在"),
    ORDER_NOT_FOUND(40432, "订单不存在"),
    PAY_RECORD_NOT_FOUND(40433, "支付流水不存在"),
    REFUND_RECORD_NOT_FOUND(40434, "退款流水不存在"),
    ORDER_ITEM_NOT_FOUND(40435, "订单项不存在"),
    AFTER_SALE_NOT_FOUND(40436, "售后单不存在"),
    ORDER_ALREADY_PAID(40931, "订单已支付");

    private final int code;
    private final String message;

    OrderErrorCode(int code, String message) {
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
