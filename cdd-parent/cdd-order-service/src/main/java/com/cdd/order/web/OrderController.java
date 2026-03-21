package com.cdd.order.web;

import com.cdd.api.order.model.CartItemResponse;
import com.cdd.api.order.model.CartItemUpsertRequest;
import com.cdd.api.order.model.CheckoutRequest;
import com.cdd.api.order.model.CheckoutResponse;
import com.cdd.api.order.model.CreateOrderRequest;
import com.cdd.api.order.model.CreateOrderResponse;
import com.cdd.api.order.model.OrderCancelRequest;
import com.cdd.api.order.model.OrderAfterSaleCreateRequest;
import com.cdd.api.order.model.OrderAfterSaleLifecycleResponse;
import com.cdd.api.order.model.OrderAfterSaleReturnRequest;
import com.cdd.api.order.model.OrderAfterSaleReviewRequest;
import com.cdd.api.order.model.OrderDeliveryUpdateRequest;
import com.cdd.api.order.model.OrderDetailResponse;
import com.cdd.api.order.model.OrderLifecycleResponse;
import com.cdd.api.order.model.OrderPayCallbackRequest;
import com.cdd.api.order.model.OrderPayCallbackResponse;
import com.cdd.api.order.model.OrderPaySuccessRequest;
import com.cdd.api.order.model.OrderPayingRequest;
import com.cdd.api.order.model.OrderPayingResponse;
import com.cdd.api.order.model.OrderRefundCallbackRequest;
import com.cdd.api.order.model.OrderRefundCreateRequest;
import com.cdd.api.order.model.OrderRefundLifecycleResponse;
import com.cdd.api.order.model.OrderStatusLogResponse;
import com.cdd.api.order.model.OrderSummaryResponse;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.order.service.OrderApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/order")
public class OrderController {

    private final OrderApplicationService orderApplicationService;

    public OrderController(OrderApplicationService orderApplicationService) {
        this.orderApplicationService = orderApplicationService;
    }

    @PostMapping("/cart/items")
    public ApiResponse<CartItemResponse> upsertCartItem(@Valid @RequestBody CartItemUpsertRequest request) {
        return ApiResponses.success(orderApplicationService.upsertCartItem(request));
    }

    @GetMapping("/cart/items")
    public ApiResponse<List<CartItemResponse>> listCartItems(
            @RequestParam(name = "merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam(name = "store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(name = "user_id") @NotNull(message = "用户ID不能为空") Long userId) {
        return ApiResponses.success(orderApplicationService.listCartItems(merchantId, storeId, userId));
    }

    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponses.success(orderApplicationService.checkout(request));
    }

    @PostMapping("/orders")
    public ApiResponse<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponses.success(orderApplicationService.createOrder(request));
    }

    @PostMapping("/orders/{order_no}/paying")
    public ApiResponse<OrderPayingResponse> markOrderPaying(@PathVariable(name = "order_no") String orderNo,
                                                            @Valid @RequestBody OrderPayingRequest request) {
        return ApiResponses.success(orderApplicationService.markOrderPaying(orderNo, request));
    }

    @PostMapping("/orders/{order_no}/paid")
    public ApiResponse<OrderLifecycleResponse> markOrderPaid(@PathVariable(name = "order_no") String orderNo,
                                                             @Valid @RequestBody OrderPaySuccessRequest request) {
        return ApiResponses.success(orderApplicationService.markOrderPaid(orderNo, request));
    }

    @PostMapping("/pay/callbacks")
    public ApiResponse<OrderPayCallbackResponse> handlePayCallback(@Valid @RequestBody OrderPayCallbackRequest request) {
        return ApiResponses.success(orderApplicationService.handlePayCallback(request));
    }

    @PostMapping("/orders/{order_no}/refunds")
    public ApiResponse<OrderRefundLifecycleResponse> createRefund(@PathVariable(name = "order_no") String orderNo,
                                                                  @Valid @RequestBody OrderRefundCreateRequest request) {
        return ApiResponses.success(orderApplicationService.createRefund(orderNo, request));
    }

    @PostMapping("/refunds/{refund_no}/callbacks")
    public ApiResponse<OrderRefundLifecycleResponse> handleRefundCallback(@PathVariable(name = "refund_no") String refundNo,
                                                                          @Valid @RequestBody OrderRefundCallbackRequest request) {
        return ApiResponses.success(orderApplicationService.handleRefundCallback(refundNo, request));
    }

    @PostMapping("/orders/{order_no}/after-sales")
    public ApiResponse<OrderAfterSaleLifecycleResponse> createAfterSale(@PathVariable(name = "order_no") String orderNo,
                                                                        @Valid @RequestBody OrderAfterSaleCreateRequest request) {
        return ApiResponses.success(orderApplicationService.createAfterSale(orderNo, request));
    }

    @PostMapping("/after-sales/{after_sale_no}/review")
    public ApiResponse<OrderAfterSaleLifecycleResponse> reviewAfterSale(@PathVariable(name = "after_sale_no") String afterSaleNo,
                                                                        @Valid @RequestBody OrderAfterSaleReviewRequest request) {
        return ApiResponses.success(orderApplicationService.reviewAfterSale(afterSaleNo, request));
    }

    @PostMapping("/after-sales/{after_sale_no}/return")
    public ApiResponse<OrderAfterSaleLifecycleResponse> submitAfterSaleReturn(@PathVariable(name = "after_sale_no") String afterSaleNo,
                                                                              @Valid @RequestBody OrderAfterSaleReturnRequest request) {
        return ApiResponses.success(orderApplicationService.submitAfterSaleReturn(afterSaleNo, request));
    }

    @PostMapping("/orders/{order_no}/cancel")
    public ApiResponse<OrderLifecycleResponse> cancelOrder(@PathVariable(name = "order_no") String orderNo,
                                                           @Valid @RequestBody OrderCancelRequest request) {
        return ApiResponses.success(orderApplicationService.cancelOrder(orderNo, request));
    }

    @PostMapping("/orders/{order_no}/delivery")
    public ApiResponse<OrderLifecycleResponse> updateDelivery(@PathVariable(name = "order_no") String orderNo,
                                                              @Valid @RequestBody OrderDeliveryUpdateRequest request) {
        return ApiResponses.success(orderApplicationService.updateDelivery(orderNo, request));
    }

    @GetMapping("/orders/{order_no}")
    public ApiResponse<OrderDetailResponse> getOrder(
            @PathVariable(name = "order_no") String orderNo,
            @RequestParam(name = "merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam(name = "store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(name = "user_id") @NotNull(message = "用户ID不能为空") Long userId) {
        return ApiResponses.success(orderApplicationService.getOrder(orderNo, merchantId, storeId, userId));
    }

    @GetMapping("/orders")
    public ApiResponse<List<OrderSummaryResponse>> listOrders(
            @RequestParam(name = "merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam(name = "store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(name = "user_id", required = false) Long userId,
            @RequestParam(name = "order_status", required = false) String orderStatus) {
        return ApiResponses.success(orderApplicationService.listOrders(merchantId, storeId, userId, orderStatus));
    }

    @GetMapping("/orders/{order_no}/status-logs")
    public ApiResponse<List<OrderStatusLogResponse>> listOrderStatusLogs(
            @PathVariable(name = "order_no") String orderNo,
            @RequestParam(name = "merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam(name = "store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(name = "user_id") @NotNull(message = "用户ID不能为空") Long userId) {
        return ApiResponses.success(orderApplicationService.listOrderStatusLogs(orderNo, merchantId, storeId, userId));
    }
}
