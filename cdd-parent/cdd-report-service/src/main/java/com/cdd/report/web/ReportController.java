package com.cdd.report.web;

import com.cdd.api.report.model.MerchantDashboardSnapshotResponse;
import com.cdd.api.report.model.MerchantDashboardSnapshotUpsertRequest;
import com.cdd.api.report.model.PlatformDashboardSnapshotResponse;
import com.cdd.api.report.model.PlatformDashboardSnapshotUpsertRequest;
import com.cdd.api.report.model.ReportHomeEventDailyResponse;
import com.cdd.api.report.model.ReportHomeEventDailyUpsertRequest;
import com.cdd.api.report.model.ReportOrderDailyResponse;
import com.cdd.api.report.model.ReportOrderDailyUpsertRequest;
import com.cdd.api.report.model.ReportProductDailyResponse;
import com.cdd.api.report.model.ReportProductDailyUpsertRequest;
import com.cdd.common.web.ApiResponse;
import com.cdd.common.web.ApiResponses;
import com.cdd.report.service.ReportApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    public ReportController(ReportApplicationService reportApplicationService) {
        this.reportApplicationService = reportApplicationService;
    }

    @PostMapping("/home-events/daily")
    public ApiResponse<ReportHomeEventDailyResponse> upsertHomeEventDaily(
            @Valid @RequestBody ReportHomeEventDailyUpsertRequest request) {
        return ApiResponses.success(reportApplicationService.upsertHomeEventDaily(request));
    }

    @GetMapping("/home-events/daily")
    public ApiResponse<List<ReportHomeEventDailyResponse>> listHomeEventDaily(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        return ApiResponses.success(reportApplicationService.listHomeEventDaily(merchantId, storeId, startDate, endDate));
    }

    @PostMapping("/orders/daily")
    public ApiResponse<ReportOrderDailyResponse> upsertOrderDaily(
            @Valid @RequestBody ReportOrderDailyUpsertRequest request) {
        return ApiResponses.success(reportApplicationService.upsertOrderDaily(request));
    }

    @GetMapping("/orders/daily")
    public ApiResponse<List<ReportOrderDailyResponse>> listOrderDaily(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        return ApiResponses.success(reportApplicationService.listOrderDaily(merchantId, storeId, startDate, endDate));
    }

    @PostMapping("/products/daily")
    public ApiResponse<ReportProductDailyResponse> upsertProductDaily(
            @Valid @RequestBody ReportProductDailyUpsertRequest request) {
        return ApiResponses.success(reportApplicationService.upsertProductDaily(request));
    }

    @GetMapping("/products/daily")
    public ApiResponse<List<ReportProductDailyResponse>> listProductDaily(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId,
            @RequestParam(value = "product_id", required = false) Long productId,
            @RequestParam(value = "start_date", required = false) String startDate,
            @RequestParam(value = "end_date", required = false) String endDate) {
        return ApiResponses.success(reportApplicationService.listProductDaily(merchantId, storeId, productId, startDate, endDate));
    }

    @PostMapping("/merchant-dashboard/snapshots")
    public ApiResponse<MerchantDashboardSnapshotResponse> createMerchantDashboardSnapshot(
            @Valid @RequestBody MerchantDashboardSnapshotUpsertRequest request) {
        return ApiResponses.success(reportApplicationService.createMerchantDashboardSnapshot(request));
    }

    @GetMapping("/merchant-dashboard/latest")
    public ApiResponse<MerchantDashboardSnapshotResponse> getLatestMerchantDashboardSnapshot(
            @RequestParam("merchant_id") @NotNull(message = "商家ID不能为空") Long merchantId,
            @RequestParam("store_id") @NotNull(message = "店铺ID不能为空") Long storeId) {
        return ApiResponses.success(reportApplicationService.getLatestMerchantDashboardSnapshot(merchantId, storeId));
    }

    @PostMapping("/platform-dashboard/snapshots")
    public ApiResponse<PlatformDashboardSnapshotResponse> createPlatformDashboardSnapshot(
            @Valid @RequestBody PlatformDashboardSnapshotUpsertRequest request) {
        return ApiResponses.success(reportApplicationService.createPlatformDashboardSnapshot(request));
    }

    @GetMapping("/platform-dashboard/latest")
    public ApiResponse<PlatformDashboardSnapshotResponse> getLatestPlatformDashboardSnapshot() {
        return ApiResponses.success(reportApplicationService.getLatestPlatformDashboardSnapshot());
    }
}
