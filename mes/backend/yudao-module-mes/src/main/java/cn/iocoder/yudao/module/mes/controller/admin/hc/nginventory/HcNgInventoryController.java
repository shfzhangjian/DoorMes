package cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgManualPieceImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationGridRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationStockReportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationStockReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationTreeWarehouseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceDeleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceUnfreezeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualOutboundReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPiecePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchQueryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgScrapReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgRackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgShelfReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgTransferReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgUnshelfReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgWarehouseSaveReqVO;
import cn.iocoder.yudao.module.mes.service.hc.nginventory.HcNgInventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 分切压槽不合格品库存")
@RestController
@RequestMapping("/mes/hc/ng-inventory")
@Validated
public class HcNgInventoryController {

    @Resource
    private HcNgInventoryService hcNgInventoryService;

    @GetMapping("/wait-shelf/page")
    @Operation(summary = "分页查询待上架分切压槽不合格品")
    public CommonResult<PageResult<NgPieceRespVO>> getWaitShelfPage(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getWaitShelfPage(reqVO));
    }

    @GetMapping("/wait-shelf/segment/page")
    @Operation(summary = "分页查询待上架不合格品段批次")
    public CommonResult<PageResult<NgPieceSegmentRespVO>> getWaitShelfSegmentPage(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getWaitShelfSegmentPage(reqVO));
    }

    @GetMapping("/wait-shelf/segment/piece-list")
    @Operation(summary = "查询待上架不合格品段内片号")
    public CommonResult<List<NgPieceRespVO>> getWaitShelfSegmentPieceList(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getWaitShelfSegmentPieceList(reqVO));
    }

    @GetMapping("/wait-freeze-shelf/page")
    @Operation(summary = "分页查询待上架冻结品")
    public CommonResult<PageResult<NgPieceRespVO>> getWaitFreezeShelfPage(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getWaitFreezeShelfPage(reqVO));
    }

    @GetMapping("/wait-freeze-shelf/segment/page")
    @Operation(summary = "分页查询待上架冻结品段批次")
    public CommonResult<PageResult<NgPieceSegmentRespVO>> getWaitFreezeShelfSegmentPage(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getWaitFreezeShelfSegmentPage(reqVO));
    }

    @GetMapping("/wait-freeze-shelf/segment/piece-list")
    @Operation(summary = "查询待上架冻结品段内片号")
    public CommonResult<List<NgPieceRespVO>> getWaitFreezeShelfSegmentPieceList(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getWaitFreezeShelfSegmentPieceList(reqVO));
    }

    @GetMapping("/piece/page")
    @Operation(summary = "分页查询分切压槽不合格品库存")
    public CommonResult<PageResult<NgPieceRespVO>> getPiecePage(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getPiecePage(reqVO));
    }

    @GetMapping("/history-ledger/page")
    @Operation(summary = "分页查询分切压槽不合格品历史台账")
    public CommonResult<PageResult<NgHistoryLedgerRespVO>> getHistoryLedgerPage(
            @Valid NgHistoryLedgerPageReqVO reqVO) {
        return success(hcNgInventoryService.getHistoryLedgerPage(reqVO));
    }

    @GetMapping("/unqualified-history-ledger/page")
    @Operation(summary = "分页查询不合格品出入库记录")
    public CommonResult<PageResult<UnqualifiedHistoryLedgerRespVO>> getUnqualifiedHistoryLedgerPage(
            @Valid UnqualifiedHistoryLedgerPageReqVO reqVO) {
        return success(hcNgInventoryService.getUnqualifiedHistoryLedgerPage(reqVO));
    }

    @GetMapping("/piece/segment/page")
    @Operation(summary = "分页查询不合格品库存段批次")
    public CommonResult<PageResult<NgPieceSegmentRespVO>> getInventorySegmentPage(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getInventorySegmentPage(reqVO));
    }

    @GetMapping("/piece/segment/piece-list")
    @Operation(summary = "查询不合格品库存段内片号")
    public CommonResult<List<NgPieceRespVO>> getInventorySegmentPieceList(@Valid NgPiecePageReqVO reqVO) {
        return success(hcNgInventoryService.getInventorySegmentPieceList(reqVO));
    }

    @PostMapping("/piece-label/batch-query")
    @Operation(summary = "批量获得不合格品工艺流转单标签数据")
    public CommonResult<NgPieceLabelBatchQueryRespVO> getPieceLabels(
            @Valid @RequestBody NgPieceLabelBatchQueryReqVO reqVO) {
        return success(hcNgInventoryService.getPieceLabels(reqVO));
    }

    @PostMapping("/piece-label/batch-mark-printed")
    @Operation(summary = "批量回写不合格品工艺流转单打印成功")
    public CommonResult<List<NgPieceLabelRespVO>> markPieceLabelsPrinted(
            @Valid @RequestBody NgPieceLabelBatchPrintedReqVO reqVO) {
        return success(hcNgInventoryService.markPieceLabelsPrinted(reqVO));
    }

    @GetMapping("/location/grid")
    @Operation(summary = "查询不合格品精确库位网格")
    public CommonResult<List<NgLocationGridRespVO>> getLocationGrid() {
        return success(hcNgInventoryService.getLocationGrid());
    }

    @GetMapping("/location/stock-report")
    @Operation(summary = "查询线边仓货架库位型号库存报表")
    public CommonResult<List<NgLocationStockReportRespVO>> getLocationStockReport(
            @Valid @ModelAttribute NgLocationStockReportReqVO reqVO) {
        return success(hcNgInventoryService.getLocationStockReport(reqVO));
    }

    @GetMapping("/location/tree")
    @Operation(summary = "查询黑白垫不合格品仓库货架库位树")
    public CommonResult<List<NgLocationTreeWarehouseRespVO>> getLocationTree() {
        return success(hcNgInventoryService.getLocationTree());
    }

    @PostMapping("/location/warehouse/save")
    @Operation(summary = "保存不合格品仓库")
    public CommonResult<Long> saveWarehouse(@Valid @RequestBody NgWarehouseSaveReqVO reqVO) {
        return success(hcNgInventoryService.saveWarehouse(reqVO));
    }

    @DeleteMapping("/location/warehouse/delete")
    @Operation(summary = "删除不合格品仓库")
    public CommonResult<Boolean> deleteWarehouse(@RequestParam("id") Long id) {
        return success(hcNgInventoryService.deleteWarehouse(id));
    }

    @PostMapping("/location/rack/save")
    @Operation(summary = "保存不合格品货架")
    public CommonResult<Long> saveRack(@Valid @RequestBody NgRackSaveReqVO reqVO) {
        return success(hcNgInventoryService.saveRack(reqVO));
    }

    @DeleteMapping("/location/rack/delete")
    @Operation(summary = "删除不合格品货架")
    public CommonResult<Boolean> deleteRack(@RequestParam("id") Long id) {
        return success(hcNgInventoryService.deleteRack(id));
    }

    @PostMapping("/manual-piece/create")
    @Operation(summary = "新增历史分切压槽不合格品")
    public CommonResult<NgPieceRespVO> createManualPiece(@Valid @RequestBody NgManualPieceCreateReqVO reqVO) {
        return success(hcNgInventoryService.createManualPiece(reqVO));
    }

    @PostMapping("/manual-piece/update")
    @Operation(summary = "编辑未上架历史分切压槽不合格品")
    public CommonResult<NgPieceRespVO> updateManualPiece(@Valid @RequestBody NgManualPieceUpdateReqVO reqVO) {
        return success(hcNgInventoryService.updateManualPiece(reqVO));
    }

    @PostMapping("/manual-piece/delete")
    @Operation(summary = "删除未上架历史分切压槽不合格品")
    public CommonResult<Boolean> deleteManualPiece(@Valid @RequestBody NgManualPieceDeleteReqVO reqVO) {
        hcNgInventoryService.deleteManualPiece(reqVO);
        return success(true);
    }

    @GetMapping("/manual-piece/import-template")
    @Operation(summary = "下载历史分切压槽不合格品导入模板")
    @ApiAccessLog(operateType = EXPORT)
    public void exportManualPieceImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.write(response, "分切压槽不合格品历史片导入模板.xlsx", "历史片补录",
                HcNgManualPieceImportExcelVO.class, Collections.emptyList());
    }

    @PostMapping(value = "/manual-piece/import-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "批量导入历史分切压槽不合格品")
    public CommonResult<NgManualPieceImportRespVO> importManualPieces(@RequestParam("file") MultipartFile file)
            throws IOException {
        return success(hcNgInventoryService.importManualPieces(file));
    }

    @PostMapping("/manual-piece/unfreeze")
    @Operation(summary = "人工解除并关闭历史冻结不合格品")
    public CommonResult<Boolean> unfreezeManualPiece(@Valid @RequestBody NgManualPieceUnfreezeReqVO reqVO) {
        hcNgInventoryService.unfreezeManualPiece(reqVO);
        return success(true);
    }

    @PostMapping("/shelf")
    @Operation(summary = "不合格品直接上架")
    public CommonResult<Boolean> shelf(@Valid @RequestBody NgShelfReqVO reqVO) {
        hcNgInventoryService.shelf(reqVO);
        return success(true);
    }

    @PostMapping("/unshelf")
    @Operation(summary = "下架不合格品；历史冻结片退回待上架冻结品")
    public CommonResult<Boolean> unshelf(@Valid @RequestBody NgUnshelfReqVO reqVO) {
        hcNgInventoryService.unshelf(reqVO);
        return success(true);
    }

    @PostMapping("/manual-outbound")
    @Operation(summary = "分切压槽不合格品批量出库")
    public CommonResult<Boolean> manualOutbound(@Valid @RequestBody NgManualOutboundReqVO reqVO) {
        hcNgInventoryService.manualOutbound(reqVO);
        return success(true);
    }

    @PostMapping("/transfer")
    @Operation(summary = "不合格品批量移库")
    public CommonResult<Boolean> transfer(@Valid @RequestBody NgTransferReqVO reqVO) {
        hcNgInventoryService.transfer(reqVO);
        return success(true);
    }

    @PostMapping("/scrap")
    @Operation(summary = "报废不合格品")
    public CommonResult<Boolean> scrap(@Valid @RequestBody NgScrapReqVO reqVO) {
        hcNgInventoryService.scrap(reqVO);
        return success(true);
    }

}
