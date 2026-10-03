package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingManualPieceImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.BoxActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.CancelInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ConfirmInboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ConfirmInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.DownShelfInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.DirectOutboundPendingPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationGridRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationTreeWarehouseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLayerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgRackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgWarehouseQualityScopeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgWarehouseQualityScopeUpdateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgWarehouseSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgShippingBatchCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgShippingBatchCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ImportedStockDataUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLocationDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLocationOverviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageBatchActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageRemarkUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InitInboundBoxesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InitOutboundBoxesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InitOutboundBoxesFromNoticeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSlicePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.LockInboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundRepackReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundRepackReturnRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundStockBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundStockBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.MockInspectionCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.OutboundBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.OutboundOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageBoxPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxStockSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackagingManualPieceCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackagingManualPieceDeleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackagingManualPieceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelBatchPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelBatchQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelBatchQueryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ScanInboundPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ScanOutboundPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeChangeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeDeliverySubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeExcelImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeIssueReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeOutboundConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePackageConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidateSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeReturnPickReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeSaveLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcFinishedPackagingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - 包装成品库")
@RestController
@RequestMapping("/mes/hc/package-fg")
@Validated
public class HcFinishedPackagingController {

    @Resource
    private HcFinishedPackagingService hcFinishedPackagingService;

    @GetMapping("/box/list")
    @Operation(summary = "获得包装箱台账列表")
    public CommonResult<List<PackageBoxRespVO>> getPackageBoxList(@RequestParam(value = "boxType", required = false) String boxType,
                                                                  @RequestParam(value = "keyword", required = false) String keyword) {
        return success(hcFinishedPackagingService.getPackageBoxList(boxType, keyword));
    }

    @GetMapping("/box/inbound-detail")
    @Operation(summary = "获得成品入库包装箱明细")
    public CommonResult<InboundBoxRespVO> getInboundBoxDetail(@RequestParam("id") Long id) {
        return success(hcFinishedPackagingService.getInboundBoxDetail(id));
    }

    @GetMapping("/box/outbound-detail")
    @Operation(summary = "获得发货出库包装箱明细")
    public CommonResult<OutboundBoxRespVO> getOutboundBoxDetail(@RequestParam("id") Long id) {
        return success(hcFinishedPackagingService.getOutboundBoxDetail(id));
    }

    @PostMapping("/box/print")
    @Operation(summary = "包装箱台账补打二维码")
    public CommonResult<Long> printPackageBox(@Valid @RequestBody PackageBoxPrintReqVO reqVO) {
        return success(hcFinishedPackagingService.printPackageBox(reqVO));
    }

    @GetMapping("/location/grid")
    @Operation(summary = "获得包装成品库库位九宫格")
    public CommonResult<List<FgLocationGridRespVO>> getFgLocationGrid() {
        return success(hcFinishedPackagingService.getFgLocationGrid());
    }

    @GetMapping("/location/tree")
    @Operation(summary = "获得包装成品库仓库货架层区域树")
    public CommonResult<List<FgLocationTreeWarehouseRespVO>> getFgLocationTree() {
        return success(hcFinishedPackagingService.getFgLocationTree());
    }

    @PostMapping("/location/warehouse/save")
    @Operation(summary = "保存包装成品仓库")
    public CommonResult<Long> saveFgWarehouse(@Valid @RequestBody FgWarehouseSaveReqVO reqVO) {
        return success(hcFinishedPackagingService.saveFgWarehouse(reqVO));
    }

    @PostMapping("/location/warehouse/quality-scope/update")
    @Operation(summary = "批量设置包装成品仓库质量用途")
    public CommonResult<FgWarehouseQualityScopeUpdateRespVO> updateFgWarehouseQualityScope(
            @Valid @RequestBody FgWarehouseQualityScopeUpdateReqVO reqVO) {
        return success(hcFinishedPackagingService.updateFgWarehouseQualityScope(reqVO));
    }

    @DeleteMapping("/location/warehouse/delete")
    @Operation(summary = "删除包装成品仓库")
    public CommonResult<Boolean> deleteFgWarehouse(@RequestParam("id") Long id) {
        return success(hcFinishedPackagingService.deleteFgWarehouse(id));
    }

    @PostMapping("/location/rack/save")
    @Operation(summary = "保存包装成品库货架")
    public CommonResult<Long> saveFgRack(@Valid @RequestBody FgRackSaveReqVO reqVO) {
        return success(hcFinishedPackagingService.saveFgRack(reqVO));
    }

    @DeleteMapping("/location/rack/delete")
    @Operation(summary = "删除包装成品库货架")
    public CommonResult<Boolean> deleteFgRack(@RequestParam("id") Long id) {
        return success(hcFinishedPackagingService.deleteFgRack(id));
    }

    @PostMapping("/location/layer/save")
    @Operation(summary = "保存包装成品库货架层")
    public CommonResult<Long> saveFgLayer(@Valid @RequestBody FgLayerSaveReqVO reqVO) {
        return success(hcFinishedPackagingService.saveFgLayer(reqVO));
    }

    @DeleteMapping("/location/layer/delete")
    @Operation(summary = "删除包装成品库货架层")
    public CommonResult<Boolean> deleteFgLayer(@RequestParam("id") Long id) {
        return success(hcFinishedPackagingService.deleteFgLayer(id));
    }

    @PostMapping("/location/save")
    @Operation(summary = "保存包装成品库库位")
    public CommonResult<Long> saveFgLocation(@Valid @RequestBody FgLocationSaveReqVO reqVO) {
        return success(hcFinishedPackagingService.saveFgLocation(reqVO));
    }

    @DeleteMapping("/location/delete")
    @Operation(summary = "删除包装成品库库位")
    public CommonResult<Boolean> deleteFgLocation(@RequestParam("id") Long id) {
        return success(hcFinishedPackagingService.deleteFgLocation(id));
    }

    @PostMapping("/location/print")
    @Operation(summary = "打印包装成品库库位二维码")
    public CommonResult<FgLocationGridRespVO> printFgLocation(@Valid @RequestBody FgLocationPrintReqVO reqVO) {
        return success(hcFinishedPackagingService.printFgLocation(reqVO));
    }

    @GetMapping("/stock/location-overview")
    @Operation(summary = "获得成品库存库位总览")
    public CommonResult<List<FgStockLocationOverviewRespVO>> getFgStockLocationOverview() {
        return success(hcFinishedPackagingService.getFgStockLocationOverview());
    }

    @GetMapping("/stock/location-detail")
    @Operation(summary = "获得成品库存库位详情")
    public CommonResult<FgStockLocationDetailRespVO> getFgStockLocationDetail(@RequestParam("locationCode") String locationCode) {
        return success(hcFinishedPackagingService.getFgStockLocationDetail(locationCode));
    }

    @GetMapping("/stock/ledger/page")
    @Operation(summary = "分页获得现有成品库存片台账")
    public CommonResult<PageResult<FgStockLedgerRespVO>> getFgStockLedgerPage(@Valid FgStockLedgerPageReqVO reqVO) {
        return success(hcFinishedPackagingService.getFgStockLedgerPage(reqVO));
    }

    @PostMapping("/stock/imported-data/update")
    @Operation(summary = "更正已上架历史导入成品库存数据")
    public CommonResult<FgStockLedgerRespVO> updateImportedStockData(
            @Valid @RequestBody ImportedStockDataUpdateReqVO reqVO) {
        return success(hcFinishedPackagingService.updateImportedStockData(reqVO));
    }

    @GetMapping("/stock/history-ledger/page")
    @Operation(summary = "分页获得成品库存出入库记录")
    public CommonResult<PageResult<FgStockHistoryLedgerRespVO>> getFgStockHistoryLedgerPage(
            @Valid FgStockHistoryLedgerPageReqVO reqVO) {
        return success(hcFinishedPackagingService.getFgStockHistoryLedgerPage(reqVO));
    }

    @GetMapping("/aux-stock/page")
    @Operation(summary = "分页获得包装辅材边库库存")
    public CommonResult<PageResult<PackageAuxStockRespVO>> getPackageAuxStockPage(@Valid PackageAuxStockPageReqVO reqVO) {
        return success(hcFinishedPackagingService.getPackageAuxStockPage(reqVO));
    }

    @PostMapping("/aux-stock/save")
    @Operation(summary = "保存包装辅材边库库存")
    public CommonResult<Long> savePackageAuxStock(@Valid @RequestBody PackageAuxStockSaveReqVO reqVO) {
        return success(hcFinishedPackagingService.savePackageAuxStock(reqVO));
    }

    @GetMapping("/aux-stock/available-list")
    @Operation(summary = "获得包装出入库可用辅材边库批次")
    public CommonResult<List<PackageAuxStockRespVO>> getPackageAuxAvailableList() {
        return success(hcFinishedPackagingService.getPackageAuxAvailableList());
    }

    @PostMapping("/aux-stock/print")
    @Operation(summary = "打印包装辅材边库二维码")
    public CommonResult<PackageAuxStockRespVO> printPackageAuxStock(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.printPackageAuxStock(reqVO));
    }

    @PostMapping("/aux-usage/create")
    @Operation(summary = "登记包装辅材当日消耗")
    public CommonResult<PackageAuxConsumeRecordRespVO> consumePackageAux(@Valid @RequestBody PackageAuxConsumeReqVO reqVO) {
        return success(hcFinishedPackagingService.consumePackageAux(reqVO));
    }

    @GetMapping("/aux-usage/today-summary")
    @Operation(summary = "获得包装辅材本日消耗汇总")
    public CommonResult<List<PackageAuxConsumeSummaryRespVO>> getPackageAuxTodaySummary(
            @RequestParam(value = "recordDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcFinishedPackagingService.getPackageAuxTodaySummary(recordDate));
    }

    @GetMapping("/aux-usage/today-list")
    @Operation(summary = "获得包装辅材本日消耗记录")
    public CommonResult<List<PackageAuxConsumeRecordRespVO>> getPackageAuxTodayList(
            @RequestParam(value = "recordDate", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcFinishedPackagingService.getPackageAuxTodayList(recordDate));
    }

    @GetMapping("/aux-usage/biz-list")
    @Operation(summary = "获得包装辅材业务消耗清单")
    public CommonResult<List<PackageAuxConsumeRecordRespVO>> getPackageAuxBizList(
            @RequestParam("bizType") String bizType,
            @RequestParam("bizNo") String bizNo) {
        return success(hcFinishedPackagingService.getPackageAuxBizList(bizType, bizNo));
    }

    @GetMapping("/inspection/mock/page")
    @Operation(summary = "获得模拟检测待检测裁切片分页")
    public CommonResult<PageResult<InspectionSliceRespVO>> getMockInspectionPage(@Valid InspectionSlicePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getMockInspectionPage(reqVO));
    }

    @PostMapping("/inspection/mock/complete")
    @Operation(summary = "模拟检测批量完成检验")
    public CommonResult<Integer> completeMockInspection(@Valid @RequestBody MockInspectionCompleteReqVO reqVO) {
        return success(hcFinishedPackagingService.completeMockInspection(reqVO));
    }

    @GetMapping("/inspection/completed/page")
    @Operation(summary = "获得检验完成区裁切片分页")
    public CommonResult<PageResult<InspectionSliceRespVO>> getCompletedInspectionPage(@Valid InspectionSlicePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getCompletedInspectionPage(reqVO));
    }

    @GetMapping("/inbound/wait-piece/page")
    @Operation(summary = "获得成品包装入库待包装片分页")
    public CommonResult<PageResult<InspectionSliceRespVO>> getInboundWaitPiecePage(@Valid InspectionSlicePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getInboundWaitPiecePage(reqVO));
    }

    @GetMapping("/inbound/wait-segment/page")
    @Operation(summary = "获得成品包装入库待包装段批次分页")
    public CommonResult<PageResult<InspectionSliceSegmentRespVO>> getInboundWaitSegmentPage(@Valid InspectionSlicePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getInboundWaitSegmentPage(reqVO));
    }

    @GetMapping("/inbound/wait-segment/piece-list")
    @Operation(summary = "获得成品包装入库待包装段内片号")
    public CommonResult<List<InspectionSliceRespVO>> getInboundWaitSegmentPieceList(@Valid InspectionSlicePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getInboundWaitSegmentPieceList(reqVO));
    }

    @PostMapping("/inbound/manual-piece/create")
    @Operation(summary = "新增历史待包装片")
    public CommonResult<InspectionSliceRespVO> createPackagingManualPiece(
            @Valid @RequestBody PackagingManualPieceCreateReqVO reqVO) {
        return success(hcFinishedPackagingService.createPackagingManualPiece(reqVO));
    }

    @PostMapping("/inbound/manual-piece/delete")
    @Operation(summary = "删除未包装的历史补录片")
    public CommonResult<Boolean> deletePackagingManualPiece(
            @Valid @RequestBody PackagingManualPieceDeleteReqVO reqVO) {
        return success(hcFinishedPackagingService.deletePackagingManualPiece(reqVO));
    }

    @GetMapping("/inbound/manual-piece/import-template")
    @Operation(summary = "下载历史待包装片导入模板")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPackagingManualPieceImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "成品包装历史片导入模板.xlsx", "历史片补录",
                HcPackagingManualPieceImportExcelVO.class, Collections.emptyList());
    }

    @PostMapping(value = "/inbound/manual-piece/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量导入历史待包装片")
    public CommonResult<PackagingManualPieceImportRespVO> importPackagingManualPieces(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "operatorName", required = false) String operatorName) throws IOException {
        return success(hcFinishedPackagingService.importPackagingManualPieces(file, operatorName));
    }

    @GetMapping("/inbound/piece-label/candidate/page")
    @Operation(summary = "获得可打印片号候选分页")
    public CommonResult<PageResult<PieceLabelRespVO>> getPieceLabelCandidatePage(
            @Valid PieceLabelCandidatePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getPieceLabelCandidatePage(reqVO));
    }

    @GetMapping("/inbound/piece-label")
    @Operation(summary = "按片号获得分切流转单标签数据")
    public CommonResult<PieceLabelRespVO> getPieceLabel(@RequestParam("sliceBatchNo") String sliceBatchNo) {
        return success(hcFinishedPackagingService.getPieceLabel(sliceBatchNo));
    }

    @PostMapping("/inbound/piece-label/batch-query")
    @Operation(summary = "批量获得分切流转单标签数据")
    public CommonResult<PieceLabelBatchQueryRespVO> getPieceLabels(
            @Valid @RequestBody PieceLabelBatchQueryReqVO reqVO) {
        return success(hcFinishedPackagingService.getPieceLabels(reqVO));
    }

    @PostMapping("/inbound/piece-label/mark-printed")
    @Operation(summary = "回写片号标签打印成功")
    public CommonResult<PieceLabelRespVO> markPieceLabelPrinted(@Valid @RequestBody PieceLabelPrintedReqVO reqVO) {
        return success(hcFinishedPackagingService.markPieceLabelPrinted(reqVO));
    }

    @PostMapping("/inbound/piece-label/batch-mark-printed")
    @Operation(summary = "批量回写片号标签打印成功")
    public CommonResult<List<PieceLabelRespVO>> markPieceLabelsPrinted(
            @Valid @RequestBody PieceLabelBatchPrintedReqVO reqVO) {
        return success(hcFinishedPackagingService.markPieceLabelsPrinted(reqVO));
    }

    @GetMapping("/inbound/package-list")
    @Operation(summary = "获得成品包装入库包装卡列表")
    public CommonResult<List<InboundBoxRespVO>> getInboundPackageList(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(hcFinishedPackagingService.getInboundPackageList(keyword));
    }

    @GetMapping("/inbound/package/page")
    @Operation(summary = "分页获得成品包装入库包装卡")
    public CommonResult<PageResult<InboundBoxRespVO>> getInboundPackagePage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam("shelfStatus") String shelfStatus,
            @RequestParam(value = "qualityStatus", required = false) String qualityStatus,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return success(hcFinishedPackagingService.getInboundPackagePage(keyword, shelfStatus, qualityStatus, pageNo, pageSize));
    }

    @GetMapping("/inbound/package-segment/page")
    @Operation(summary = "分页获得成品包装入库分段包装单")
    public CommonResult<PageResult<InboundPackageSegmentRespVO>> getInboundPackageSegmentPage(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam("shelfStatus") String shelfStatus,
            @RequestParam(value = "qualityStatus", required = false) String qualityStatus,
            @RequestParam(value = "pageNo", defaultValue = "1") Integer pageNo,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        return success(hcFinishedPackagingService.getInboundPackageSegmentPage(keyword, shelfStatus, qualityStatus, pageNo, pageSize));
    }

    @GetMapping("/inbound/package-segment/package-list")
    @Operation(summary = "获得成品包装入库分段内包装单")
    public CommonResult<List<InboundBoxRespVO>> getInboundPackageSegmentPackageList(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam("shelfStatus") String shelfStatus,
            @RequestParam(value = "qualityStatus", required = false) String qualityStatus,
            @RequestParam("segmentBatchNo") String segmentBatchNo) {
        return success(hcFinishedPackagingService.getInboundPackageSegmentPackageList(keyword, shelfStatus, qualityStatus, segmentBatchNo));
    }

    @GetMapping("/inbound/packed-segment/page")
    @Operation(summary = "获得待上架包装段批次分页")
    public CommonResult<PageResult<InboundPackageSegmentRespVO>> getInboundPackedSegmentPage(@Valid InspectionSlicePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getInboundPackedSegmentPage(reqVO));
    }

    @GetMapping("/inbound/packed-segment/package-list")
    @Operation(summary = "获得待上架包装段内包装单")
    public CommonResult<List<InboundBoxRespVO>> getInboundPackedSegmentPackageList(@Valid InspectionSlicePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getInboundPackedSegmentPackageList(reqVO));
    }

    @PostMapping("/inbound/lock-package")
    @Operation(summary = "确认成品批量单片包装并生成待上架包装单")
    public CommonResult<InboundPackageBatchRespVO> lockInboundPackage(@Valid @RequestBody LockInboundPackageReqVO reqVO) {
        return success(hcFinishedPackagingService.lockInboundPackage(reqVO));
    }

    @PostMapping("/inbound/print-card")
    @Operation(summary = "打印成品包装入库库存卡")
    public CommonResult<InboundBoxRespVO> printInboundPackageCard(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.printInboundPackageCard(reqVO));
    }

    @PostMapping("/inbound/update-remark")
    @Operation(summary = "更新待上架成品包装备注")
    public CommonResult<InboundBoxRespVO> updateInboundPackageRemark(
            @Valid @RequestBody InboundPackageRemarkUpdateReqVO reqVO) {
        return success(hcFinishedPackagingService.updateInboundPackageRemark(reqVO));
    }

    @PostMapping("/inbound/cancel-lock")
    @Operation(summary = "撤销待上架成品包装")
    public CommonResult<Long> cancelInboundPackageLock(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.cancelInboundPackageLock(reqVO));
    }

    @PostMapping("/inbound/cancel-lock-batch")
    @Operation(summary = "批量撤销待上架成品包装")
    public CommonResult<InboundPackageBatchActionRespVO> cancelInboundPackageLocks(
            @Valid @RequestBody CancelInboundPackageBatchReqVO reqVO) {
        return success(hcFinishedPackagingService.cancelInboundPackageLocks(reqVO));
    }

    @PostMapping("/inbound/confirm-package")
    @Operation(summary = "确认成品包装入库上架")
    public CommonResult<InboundBoxRespVO> confirmInboundPackage(@Valid @RequestBody ConfirmInboundPackageReqVO reqVO) {
        return success(hcFinishedPackagingService.confirmInboundPackage(reqVO));
    }

    @PostMapping("/inbound/confirm-package-batch")
    @Operation(summary = "批量确认成品包装入库上架")
    public CommonResult<InboundPackageBatchActionRespVO> confirmInboundPackages(
            @Valid @RequestBody ConfirmInboundPackageBatchReqVO reqVO) {
        return success(hcFinishedPackagingService.confirmInboundPackages(reqVO));
    }

    @PostMapping("/inbound/down-shelf")
    @Operation(summary = "成品包装下架待重新上架")
    public CommonResult<InboundBoxRespVO> downShelfInboundPackage(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.downShelfInboundPackage(reqVO));
    }

    @PostMapping("/inbound/down-shelf-batch")
    @Operation(summary = "批量成品包装下架待重新上架")
    public CommonResult<InboundPackageBatchActionRespVO> downShelfInboundPackages(
            @Valid @RequestBody DownShelfInboundPackageBatchReqVO reqVO) {
        return success(hcFinishedPackagingService.downShelfInboundPackages(reqVO));
    }

    @PostMapping("/inbound/manual-outbound")
    @Operation(summary = "已上架成品包装手工出库")
    public CommonResult<InboundBoxRespVO> manualOutboundInboundPackage(
            @Valid @RequestBody ManualOutboundPackageReqVO reqVO) {
        return success(hcFinishedPackagingService.manualOutboundInboundPackage(reqVO));
    }

    @PostMapping("/inbound/manual-outbound-batch")
    @Operation(summary = "已上架成品包装批量手工出库")
    public CommonResult<InboundPackageBatchActionRespVO> manualOutboundInboundPackages(
            @Valid @RequestBody ManualOutboundPackageBatchReqVO reqVO) {
        return success(hcFinishedPackagingService.manualOutboundInboundPackages(reqVO));
    }

    @PostMapping("/inbound/manual-outbound-pending-batch")
    @Operation(summary = "未上架不合格成品包装批量手工出库")
    public CommonResult<InboundPackageBatchActionRespVO> manualOutboundPendingInboundPackages(
            @Valid @RequestBody ManualOutboundPackageBatchReqVO reqVO) {
        return success(hcFinishedPackagingService.manualOutboundPendingInboundPackages(reqVO));
    }

    @PostMapping("/inbound/direct-outbound-pending-batch")
    @Operation(summary = "合格品待上架包装批量直接出库")
    public CommonResult<InboundPackageBatchActionRespVO> directOutboundPendingQualifiedInboundPackages(
            @Valid @RequestBody DirectOutboundPendingPackageBatchReqVO reqVO) {
        return success(hcFinishedPackagingService.directOutboundPendingQualifiedInboundPackages(reqVO));
    }

    @PostMapping("/stock/manual-outbound")
    @Operation(summary = "按库存片批量手工出库")
    public CommonResult<ManualOutboundStockBatchRespVO> manualOutboundFgStocks(
            @Valid @RequestBody ManualOutboundStockBatchReqVO reqVO) {
        return success(hcFinishedPackagingService.manualOutboundFgStocks(reqVO));
    }

    @PostMapping("/stock/history-ledger/manual-outbound-repack-return")
    @Operation(summary = "手工成品出库单片退回待重新包装")
    public CommonResult<ManualOutboundRepackReturnRespVO> returnManualOutboundStockForRepack(
            @Valid @RequestBody ManualOutboundRepackReturnReqVO reqVO) {
        return success(hcFinishedPackagingService.returnManualOutboundStockForRepack(reqVO));
    }

    @GetMapping("/inbound/task-list")
    @Operation(summary = "获得成品包装入库待包装列表")
    public CommonResult<List<InboundTaskRespVO>> getInboundTaskList(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(hcFinishedPackagingService.getInboundTaskList(keyword));
    }

    @GetMapping("/inbound/source-list")
    @Operation(summary = "获得成品包装入库来源片号")
    public CommonResult<List<HcPackagingSourceRespVO>> getInboundSourceList(@RequestParam("planId") Long planId,
                                                                            @RequestParam("motherSegmentBatchNo") String motherSegmentBatchNo) {
        return success(hcFinishedPackagingService.getInboundSourceList(planId, motherSegmentBatchNo));
    }

    @GetMapping("/inbound/box-list")
    @Operation(summary = "获得成品包装盒列表")
    public CommonResult<List<InboundBoxRespVO>> getInboundBoxList(@RequestParam("planOperationId") Long planOperationId,
                                                                  @RequestParam("motherSegmentBatchNo") String motherSegmentBatchNo) {
        return success(hcFinishedPackagingService.getInboundBoxList(planOperationId, motherSegmentBatchNo));
    }

    @PostMapping("/inbound/init-boxes")
    @Operation(summary = "初始化成品包装盒")
    public CommonResult<List<Long>> initInboundBoxes(@Valid @RequestBody InitInboundBoxesReqVO reqVO) {
        return success(hcFinishedPackagingService.initInboundBoxes(reqVO));
    }

    @PostMapping("/inbound/scan-piece")
    @Operation(summary = "成品包装入库扫码片号")
    public CommonResult<Long> scanInboundPiece(@Valid @RequestBody ScanInboundPieceReqVO reqVO) {
        return success(hcFinishedPackagingService.scanInboundPiece(reqVO));
    }

    @PostMapping("/inbound/print-box")
    @Operation(summary = "打印成品包装盒二维码")
    public CommonResult<Long> printInboundBox(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.printInboundBox(reqVO));
    }

    @PostMapping("/inbound/confirm-box")
    @Operation(summary = "确认成品包装盒入库")
    public CommonResult<Long> confirmInboundBox(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.confirmInboundBox(reqVO));
    }

    @GetMapping("/shipping-notice/page")
    @Operation(summary = "分页获得发货需求单")
    public CommonResult<PageResult<ShippingNoticeRespVO>> getShippingNoticePage(@Valid ShippingNoticePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getShippingNoticePage(reqVO));
    }

    @GetMapping("/shipping-notice/get")
    @Operation(summary = "获得发货需求单详情")
    public CommonResult<ShippingNoticeRespVO> getShippingNotice(@RequestParam("id") Long id) {
        return success(hcFinishedPackagingService.getShippingNotice(id));
    }

    @GetMapping("/shipping-notice/stock-candidate/page")
    @Operation(summary = "分页获得发货需求单可选成品库存")
    public CommonResult<PageResult<FgStockLedgerRespVO>> getShippingNoticeStockCandidatePage(@Valid FgStockLedgerPageReqVO reqVO) {
        return success(hcFinishedPackagingService.getShippingNoticeStockCandidatePage(reqVO));
    }

    @GetMapping("/shipping-notice/batch-candidate/page")
    @Operation(summary = "分页获得发货需求单内部编号候选")
    public CommonResult<PageResult<FgShippingBatchCandidateRespVO>> getShippingNoticeBatchCandidatePage(@Valid FgShippingBatchCandidatePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getShippingNoticeBatchCandidatePage(reqVO));
    }

    @GetMapping("/shipping-notice/pick-candidate/page")
    @Operation(summary = "分页获得发货需求单配货可选库存")
    public CommonResult<PageResult<FgStockLedgerRespVO>> getShippingNoticePickCandidatePage(@Valid ShippingNoticePickCandidatePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getShippingNoticePickCandidatePage(reqVO));
    }

    @GetMapping("/shipping-notice/pick-candidate/segment/page")
    @Operation(summary = "按分段批号分页获得发货配货候选")
    public CommonResult<PageResult<ShippingNoticePickCandidateSegmentRespVO>>
            getShippingNoticePickCandidateSegmentPage(@Valid ShippingNoticePickCandidatePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getShippingNoticePickCandidateSegmentPage(reqVO));
    }

    @GetMapping("/shipping-notice/pick-candidate/segment-piece-list")
    @Operation(summary = "获得发货配货指定分段批号下的片号")
    public CommonResult<List<FgStockLedgerRespVO>> getShippingNoticePickCandidateSegmentPieceList(
            @Valid ShippingNoticePickCandidatePageReqVO reqVO) {
        return success(hcFinishedPackagingService.getShippingNoticePickCandidateSegmentPieceList(reqVO));
    }

    @PostMapping("/shipping-notice/save-lock")
    @Operation(summary = "保存发货需求单草稿")
    public CommonResult<ShippingNoticeRespVO> saveAndLockShippingNotice(@Valid @RequestBody ShippingNoticeSaveLockReqVO reqVO) {
        return success(hcFinishedPackagingService.saveAndLockShippingNotice(reqVO));
    }

    @PostMapping("/shipping-notice/change")
    @Operation(summary = "变更发货需求单并同步后续发货流程")
    public CommonResult<ShippingNoticeRespVO> changeShippingNotice(@Valid @RequestBody ShippingNoticeChangeReqVO reqVO) {
        return success(hcFinishedPackagingService.changeShippingNotice(reqVO));
    }

    @PostMapping(value = "/shipping-notice/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入Excel生成发货需求单")
    public CommonResult<ShippingNoticeExcelImportRespVO> importShippingNoticeExcel(@RequestParam("file") MultipartFile file,
                                                                                  @RequestParam("fileUrl") String fileUrl,
                                                                                  @RequestParam(value = "operatorName", required = false) String operatorName) {
        return success(hcFinishedPackagingService.importShippingNoticeExcel(file, fileUrl, operatorName));
    }

    @PostMapping("/shipping-notice/issue")
    @Operation(summary = "下达执行发货需求单")
    public CommonResult<ShippingNoticeRespVO> issueShippingNotice(@Valid @RequestBody ShippingNoticeIssueReqVO reqVO) {
        return success(hcFinishedPackagingService.issueShippingNotice(reqVO));
    }

    @PostMapping("/shipping-notice/cancel")
    @Operation(summary = "取消发货需求单")
    public CommonResult<ShippingNoticeRespVO> cancelShippingNotice(@Valid @RequestBody ShippingNoticeCancelReqVO reqVO) {
        return success(hcFinishedPackagingService.cancelShippingNotice(reqVO));
    }

    @DeleteMapping("/shipping-notice/delete")
    @Operation(summary = "物理删除未下达发货需求单")
    public CommonResult<Boolean> deleteShippingNotice(@RequestParam("id") Long id) {
        hcFinishedPackagingService.deleteShippingNotice(id);
        return success(true);
    }

    @PostMapping("/shipping-notice/pick")
    @Operation(summary = "发货需求单配货领用")
    public CommonResult<ShippingNoticeRespVO> pickShippingNotice(@Valid @RequestBody ShippingNoticePickReqVO reqVO) {
        return success(hcFinishedPackagingService.pickShippingNotice(reqVO));
    }

    @PostMapping("/shipping-notice/release-pick")
    @Operation(summary = "发货需求单退回配货片")
    public CommonResult<ShippingNoticeRespVO> returnShippingNoticePick(@Valid @RequestBody ShippingNoticeReturnPickReqVO reqVO) {
        return success(hcFinishedPackagingService.returnShippingNoticePick(reqVO));
    }

    @PostMapping("/shipping-notice/confirm-shipping")
    @Operation(summary = "出货管理确认并推送发货成品检验")
    public CommonResult<ShippingNoticeRespVO> confirmShippingNoticeForFqc(@Valid @RequestBody ShippingNoticeOutboundConfirmReqVO reqVO) {
        return success(hcFinishedPackagingService.confirmShippingNoticeForFqc(reqVO));
    }

    @PostMapping("/shipping-notice/inspect")
    @Operation(summary = "发货需求单发货检验")
    public CommonResult<ShippingNoticeRespVO> inspectShippingNoticeItem(@Valid @RequestBody ShippingNoticeInspectionReqVO reqVO) {
        return success(hcFinishedPackagingService.inspectShippingNoticeItem(reqVO));
    }

    @PostMapping("/shipping-notice/pack")
    @Operation(summary = "发货需求单打包")
    public CommonResult<ShippingNoticeRespVO> packShippingNotice(@Valid @RequestBody ShippingNoticePackReqVO reqVO) {
        return success(hcFinishedPackagingService.packShippingNotice(reqVO));
    }

    @PostMapping({"/shipping-notice/push-oqc", "/shipping-notice/confirm-package"})
    @Operation(summary = "发货需求单推送出货检验")
    public CommonResult<ShippingNoticeRespVO> pushShippingNoticeOqc(@Valid @RequestBody ShippingNoticePackageConfirmReqVO reqVO) {
        return success(hcFinishedPackagingService.pushShippingNoticeOqc(reqVO));
    }

    @PostMapping("/shipping-notice/complete-shipping")
    @Operation(summary = "发货需求单发货完成")
    public CommonResult<ShippingNoticeRespVO> completeShippingNotice(@Valid @RequestBody ShippingNoticePackageConfirmReqVO reqVO) {
        return success(hcFinishedPackagingService.completeShippingNotice(reqVO));
    }

    @GetMapping("/outbound/shipping-notice-list")
    @Operation(summary = "获得发货包装出库可选发货需求单")
    public CommonResult<List<ShippingNoticeRespVO>> getOutboundShippingNoticeList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(hcFinishedPackagingService.getOutboundShippingNoticeList(keyword));
    }

    @GetMapping("/outbound/shipping-order-list")
    @Operation(summary = "获得发货单列表")
    public CommonResult<List<ShippingOrderRespVO>> getShippingOrderList(@RequestParam(value = "keyword", required = false) String keyword) {
        return success(hcFinishedPackagingService.getShippingOrderList(keyword));
    }

    @PostMapping("/outbound/init-boxes-from-notice")
    @Operation(summary = "按发货需求单初始化发货包装盒")
    public CommonResult<OutboundOrderRespVO> initOutboundBoxesFromNotice(@Valid @RequestBody InitOutboundBoxesFromNoticeReqVO reqVO) {
        return success(hcFinishedPackagingService.initOutboundBoxesFromNotice(reqVO));
    }

    @PostMapping("/outbound/init-boxes")
    @Operation(summary = "初始化发货包装盒")
    public CommonResult<OutboundOrderRespVO> initOutboundBoxes(@Valid @RequestBody InitOutboundBoxesReqVO reqVO) {
        return success(hcFinishedPackagingService.initOutboundBoxes(reqVO));
    }

    @GetMapping("/outbound/order")
    @Operation(summary = "获得发货包装出库单")
    public CommonResult<OutboundOrderRespVO> getOutboundOrder(@RequestParam("outboundNo") String outboundNo) {
        return success(hcFinishedPackagingService.getOutboundOrder(outboundNo));
    }

    @GetMapping("/outbound/order-by-notice")
    @Operation(summary = "按发货需求单获得发货包装出库单")
    public CommonResult<OutboundOrderRespVO> getOutboundOrderByNotice(@RequestParam("sourceNoticeId") Long sourceNoticeId) {
        return success(hcFinishedPackagingService.getOutboundOrderByNotice(sourceNoticeId));
    }

    @GetMapping("/outbound/box-list")
    @Operation(summary = "获得发货包装盒列表")
    public CommonResult<List<OutboundBoxRespVO>> getOutboundBoxList(@RequestParam("outboundOrderId") Long outboundOrderId) {
        return success(hcFinishedPackagingService.getOutboundBoxList(outboundOrderId));
    }

    @PostMapping("/outbound/scan-piece")
    @Operation(summary = "发货包装扫码片号")
    public CommonResult<Long> scanOutboundPiece(@Valid @RequestBody ScanOutboundPieceReqVO reqVO) {
        return success(hcFinishedPackagingService.scanOutboundPiece(reqVO));
    }

    @PostMapping("/outbound/print-box")
    @Operation(summary = "打印发货包装盒二维码")
    public CommonResult<Long> printOutboundBox(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.printOutboundBox(reqVO));
    }

    @PostMapping("/outbound/confirm-order")
    @Operation(summary = "确认发货出库")
    public CommonResult<Long> confirmOutboundOrder(@Valid @RequestBody BoxActionReqVO reqVO) {
        return success(hcFinishedPackagingService.confirmOutboundOrder(reqVO));
    }

    @PostMapping("/outbound/submit-delivery")
    @Operation(summary = "提交发货需求单实际发货")
    public CommonResult<ShippingNoticeRespVO> submitShippingNoticeDelivery(@Valid @RequestBody ShippingNoticeDeliverySubmitReqVO reqVO) {
        return success(hcFinishedPackagingService.submitShippingNoticeDelivery(reqVO));
    }
}
