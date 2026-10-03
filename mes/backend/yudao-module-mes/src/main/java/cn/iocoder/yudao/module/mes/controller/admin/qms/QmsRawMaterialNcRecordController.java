package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcMrbReviewDelegateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyReplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordFinalApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordLinkExceptionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordRestorePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordStockDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcInspectionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordCreateFromInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordCreateFromInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordRestoreReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 原物料不合格处置单")
@RestController
@RequestMapping("/mes/qms-raw-material-nc-record")
@Validated
public class QmsRawMaterialNcRecordController {

    private static final String SOURCE_TYPE_RAW_MATERIAL = "RAW_MATERIAL";
    private static final String SOURCE_TYPE_RAW_MATERIAL_NAME = "原物料";

    @Resource
    private QmsNcRecordService qmsNcRecordService;

    @GetMapping("/page")
    @Operation(summary = "获得原物料不合格处置单分页")
    public CommonResult<PageResult<QmsNcRecordRespVO>> getRawMaterialNcRecordPage(
            @Valid QmsNcRecordPageReqVO pageVO) {
        markRawMaterial(pageVO);
        return success(qmsNcRecordService.getNcRecordPage(pageVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得原物料不合格处置单详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsNcRecordRespVO> getRawMaterialNcRecord(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.getNcRecord(id));
    }

    @PostMapping("/create")
    @Operation(summary = "开立原物料不合格处置单")
    public CommonResult<Long> createRawMaterialNcRecord(@Valid @RequestBody QmsNcRecordSaveReqVO createReqVO) {
        markRawMaterial(createReqVO);
        return success(qmsNcRecordService.createNcRecord(createReqVO));
    }

    @PostMapping("/relaunch")
    @Operation(summary = "基于旧原物料不合格处置单重新发起新单")
    @Parameter(name = "id", description = "旧单编号", required = true)
    public CommonResult<Long> relaunchRawMaterialNcRecord(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.relaunchNcRecord(id));
    }

    @PutMapping("/update")
    @Operation(summary = "更新原物料不合格处置单")
    public CommonResult<Boolean> updateRawMaterialNcRecord(@Valid @RequestBody QmsNcRecordSaveReqVO updateReqVO) {
        markRawMaterial(updateReqVO);
        qmsNcRecordService.updateNcRecord(updateReqVO);
        return success(true);
    }

    @GetMapping("/source-inspection/page")
    @Operation(summary = "获得可生成原物料不合格处置单的来源检验分页")
    public CommonResult<PageResult<QmsRawMaterialNcInspectionRespVO>> getSourceInspectionPage(
            @Valid QmsRawMaterialNcInspectionPageReqVO pageVO) {
        return success(qmsNcRecordService.getRawMaterialInspectionPage(pageVO));
    }

    @PostMapping("/create-from-inspection")
    @Operation(summary = "从检验单开立或打开原物料不合格处置单")
    public CommonResult<Long> createFromInspection(
            @Valid @RequestBody QmsRawMaterialNcRecordCreateFromInspectionReqVO createReqVO) {
        return success(qmsNcRecordService.createOrGetRawMaterialFromInspection(createReqVO));
    }

    @PostMapping("/batch-create-from-inspection")
    @Operation(summary = "批量从检验单生成原物料不合格处置单")
    public CommonResult<List<QmsRawMaterialNcRecordCreateFromInspectionRespVO>> batchCreateFromInspection(
            @Valid @RequestBody QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO createReqVO) {
        return success(qmsNcRecordService.batchCreateRawMaterialFromInspection(createReqVO));
    }

    @PostMapping("/merge-create-from-inspection")
    @Operation(summary = "批量从检验单合并生成一张原物料不合格处置单")
    public CommonResult<Long> mergeCreateFromInspection(
            @Valid @RequestBody QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO createReqVO) {
        return success(qmsNcRecordService.mergeCreateRawMaterialFromInspection(createReqVO));
    }

    @PostMapping("/restore")
    @Operation(summary = "管理员还原原材料不合格处置单及来源IQC关联")
    @PreAuthorize("@ss.hasPermission('mes:qms-raw-material-nc-record:restore')")
    public CommonResult<Boolean> restoreRawMaterialNcRecord(
            @Valid @RequestBody QmsRawMaterialNcRecordRestoreReqVO restoreReqVO) {
        qmsNcRecordService.restoreRawMaterialNcRecord(restoreReqVO);
        return success(true);
    }

    @GetMapping("/restore-preview")
    @Operation(summary = "获得原材料不合格处置单还原风险预览")
    public CommonResult<QmsNcRecordRestorePreviewRespVO> getRawMaterialNcRestorePreview(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.getRawMaterialNcRestorePreview(id));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交原物料不合格处置单")
    public CommonResult<Boolean> submitRawMaterialNcRecord(@Valid @RequestBody QmsNcRecordSubmitReqVO submitReqVO) {
        qmsNcRecordService.submitNcRecord(submitReqVO);
        return success(true);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回原物料不合格处置单至上一环节修改")
    public CommonResult<Boolean> withdrawRawMaterialNcRecord(
            @Valid @RequestBody QmsNcRecordWithdrawReqVO withdrawReqVO) {
        qmsNcRecordService.withdrawNcRecord(withdrawReqVO);
        return success(true);
    }

    @PostMapping("/handle")
    @Operation(summary = "办理原物料不合格处置单")
    public CommonResult<Boolean> handleRawMaterialNcRecord(@Valid @RequestBody QmsNcRecordHandleReqVO handleReqVO) {
        qmsNcRecordService.handleNcRecord(handleReqVO);
        return success(true);
    }

    @PostMapping("/link-exception")
    @Operation(summary = "关联已有异常事件")
    public CommonResult<Boolean> linkException(@Valid @RequestBody QmsNcRecordLinkExceptionReqVO linkReqVO) {
        qmsNcRecordService.linkException(linkReqVO);
        return success(true);
    }

    @PostMapping("/mrb-review/delegate")
    @Operation(summary = "设置原物料不合格处置单会签办理委托")
    public CommonResult<Boolean> delegateRawMaterialMrbReview(
            @Valid @RequestBody QmsNcMrbReviewDelegateReqVO delegateReqVO) {
        qmsNcRecordService.delegateMrbReview(delegateReqVO);
        return success(true);
    }

    @PostMapping("/return")
    @Operation(summary = "退回原物料不合格处置单")
    public CommonResult<Boolean> returnRawMaterialNcRecord(@Valid @RequestBody QmsNcRecordReturnReqVO returnReqVO) {
        qmsNcRecordService.returnNcRecord(returnReqVO);
        return success(true);
    }

    @PostMapping("/final-approve")
    @Operation(summary = "原物料不合格处置单终审")
    public CommonResult<Boolean> finalApproveRawMaterialNcRecord(
            @Valid @RequestBody QmsNcRecordFinalApproveReqVO approveReqVO) {
        qmsNcRecordService.finalApproveNcRecord(approveReqVO);
        return success(true);
    }

    @PostMapping("/stock-dispose")
    @Operation(summary = "原物料不合格处置单处置执行确认")
    public CommonResult<Boolean> stockDisposeRawMaterialNcRecord(
            @Valid @RequestBody QmsNcRecordStockDisposeReqVO disposeReqVO) {
        qmsNcRecordService.stockDisposeNcRecord(disposeReqVO);
        return success(true);
    }

    @PostMapping("/disposition-notify/reply")
    @Operation(summary = "原物料不合格处置单处置执行通知人回复")
    public CommonResult<Boolean> replyRawMaterialDispositionNotify(
            @Valid @RequestBody QmsNcDispositionNotifyReplyReqVO replyReqVO) {
        qmsNcRecordService.replyDispositionNotify(replyReqVO);
        return success(true);
    }

    @GetMapping("/flow-log/list")
    @Operation(summary = "获得原物料不合格处置单流程日志")
    @Parameter(name = "ncRecordId", description = "单据 ID", required = true)
    public CommonResult<List<QmsNcRecordRespVO.FlowLog>> getFlowLogList(
            @RequestParam("ncRecordId") Long ncRecordId) {
        return success(qmsNcRecordService.getFlowLogList(ncRecordId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出原物料不合格处置单 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportRawMaterialNcRecordExcel(@Valid QmsNcRecordPageReqVO pageReqVO,
                                               HttpServletResponse response) throws IOException {
        markRawMaterial(pageReqVO);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsNcRecordRespVO> list = qmsNcRecordService.getNcRecordPage(pageReqVO).getList();
        ExcelUtils.write(response, "原物料不合格处置单台账.xls", "数据", QmsNcRecordRespVO.class, list);
    }

    private void markRawMaterial(QmsNcRecordPageReqVO pageVO) {
        pageVO.setSourceType(SOURCE_TYPE_RAW_MATERIAL);
        pageVO.setExcludeRawMaterial(false);
    }

    private void markRawMaterial(QmsNcRecordSaveReqVO reqVO) {
        reqVO.setSourceType(SOURCE_TYPE_RAW_MATERIAL);
        reqVO.setSourceTypeName(SOURCE_TYPE_RAW_MATERIAL_NAME);
    }
}
