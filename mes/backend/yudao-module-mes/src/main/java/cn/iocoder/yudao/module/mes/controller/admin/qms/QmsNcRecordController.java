package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcPackagingPieceRespVO;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyReplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcMrbReviewDelegateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordBatchCreateFromProductEventReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordCreateFromProductEventReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordCreateFromProductEventRespVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductNcRecordRestoreReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcRecordService;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcDispositionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcClosedCorrectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcClosedCorrectionRespVO;
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

@Tag(name = "管理后台 - 不合格品处理单(NCR)")
@RestController
@RequestMapping("/mes/qms-nc-record")
@Validated
public class QmsNcRecordController {

    @Resource
    private QmsNcRecordService qmsNcRecordService;

    @Resource
    private QmsNcDispositionService qmsNcDispositionService;

    @GetMapping("/packaging-flow/get")
    @Operation(summary = "只读查看产品处置流转，不同步流程状态")
    public CommonResult<QmsNcRecordRespVO> getPackagingFlowRecord(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.getPackagingFlowRecord(id));
    }

    @GetMapping("/packaging-flow/pieces")
    @Operation(summary = "只读查看包装相关片号处置范围及包装情况")
    public CommonResult<List<QmsNcPackagingPieceRespVO>>
            getPackagingFlowPieces(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.getPackagingFlowPieces(id));
    }

    @GetMapping("/closed-correction")
    @Operation(summary = "查询已关闭产品处置单可修改片号")
    public CommonResult<QmsNcClosedCorrectionRespVO> getClosedCorrection(@RequestParam Long id) {
        return success(qmsNcDispositionService.getClosedCorrection(id));
    }

    @PostMapping("/closed-correction/preview")
    @Operation(summary = "预览未上架片号处置变更供再次确认")
    public CommonResult<QmsNcClosedCorrectionRespVO> previewClosedCorrection(
            @Valid @RequestBody QmsNcClosedCorrectionReqVO reqVO) {
        return success(qmsNcDispositionService.previewClosedCorrection(reqVO));
    }

    @PostMapping("/closed-correction/save")
    @Operation(summary = "确认保存已关闭产品处置单片号更正")
    public CommonResult<Boolean> saveClosedCorrection(
            @Valid @RequestBody QmsNcClosedCorrectionReqVO reqVO) {
        qmsNcDispositionService.saveClosedCorrection(reqVO);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得不合格品处理单分页")
    public CommonResult<PageResult<QmsNcRecordRespVO>> getNcRecordPage(@Valid QmsNcRecordPageReqVO pageVO) {
        markProductNc(pageVO);
        return success(qmsNcRecordService.getNcRecordPage(pageVO));
    }

    @GetMapping("/disposition-notify/page")
    @Operation(summary = "获得当前登录人的 NCR 处置执行通知分页")
    public CommonResult<PageResult<QmsNcDispositionNotifyRespVO>> getDispositionNotifyPage(
            @Valid QmsNcDispositionNotifyPageReqVO pageVO) {
        return success(qmsNcRecordService.getDispositionNotifyPage(pageVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得不合格品处理单详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsNcRecordRespVO> getNcRecord(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.getNcRecord(id));
    }

    @GetMapping("/disposition-context")
    @Operation(summary = "获得产品 NCR 终审处置范围上下文")
    @Parameter(name = "id", description = "NCR 编号", required = true)
    public CommonResult<QmsNcDispositionContextRespVO> getDispositionContext(@RequestParam("id") Long id) {
        return success(qmsNcDispositionService.getDispositionContext(id));
    }

    @PostMapping("/create")
    @Operation(summary = "开立不合格品处理单")
    public CommonResult<Long> createNcRecord(@Valid @RequestBody QmsNcRecordSaveReqVO createReqVO) {
        return success(qmsNcRecordService.createNcRecord(createReqVO));
    }

    @PostMapping("/relaunch")
    @Operation(summary = "基于旧 NCR 重新发起新单")
    @Parameter(name = "id", description = "旧 NCR 编号", required = true)
    public CommonResult<Long> relaunchNcRecord(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.relaunchNcRecord(id));
    }

    @PostMapping("/create-from-product-event")
    @Operation(summary = "从产品异常事件开立或打开 NCR")
    public CommonResult<Long> createFromProductEvent(@Valid @RequestBody QmsNcRecordCreateFromProductEventReqVO createReqVO) {
        return success(qmsNcRecordService.createOrGetFromProductEvent(createReqVO));
    }

    @PostMapping("/batch-create-from-product-event")
    @Operation(summary = "批量从产品异常事件生成 NCR 草稿")
    public CommonResult<List<QmsNcRecordCreateFromProductEventRespVO>> batchCreateFromProductEvent(
            @Valid @RequestBody QmsNcRecordBatchCreateFromProductEventReqVO createReqVO) {
        return success(qmsNcRecordService.batchCreateFromProductEvent(createReqVO));
    }

    @PostMapping("/restore")
    @Operation(summary = "管理员还原产品不合格处置单及产品异常来源关联")
    @PreAuthorize("@ss.hasPermission('mes:qms-nc-record:restore')")
    public CommonResult<Boolean> restoreProductNcRecord(
            @Valid @RequestBody QmsProductNcRecordRestoreReqVO restoreReqVO) {
        qmsNcRecordService.restoreProductNcRecord(restoreReqVO);
        return success(true);
    }

    @GetMapping("/restore-preview")
    @Operation(summary = "获得产品不合格处置单还原风险预览")
    public CommonResult<QmsNcRecordRestorePreviewRespVO> getProductNcRestorePreview(@RequestParam("id") Long id) {
        return success(qmsNcRecordService.getProductNcRestorePreview(id));
    }

    @PutMapping("/update")
    @Operation(summary = "更新不合格品处理单")
    public CommonResult<Boolean> updateNcRecord(@Valid @RequestBody QmsNcRecordSaveReqVO updateReqVO) {
        qmsNcRecordService.updateNcRecord(updateReqVO);
        return success(true);
    }

    @PostMapping("/submit")
    @Operation(summary = "提交 NCR MRB")
    public CommonResult<Boolean> submitNcRecord(@Valid @RequestBody QmsNcRecordSubmitReqVO submitReqVO) {
        qmsNcRecordService.submitNcRecord(submitReqVO);
        return success(true);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回 NCR 至上一环节修改")
    public CommonResult<Boolean> withdrawNcRecord(@Valid @RequestBody QmsNcRecordWithdrawReqVO withdrawReqVO) {
        qmsNcRecordService.withdrawNcRecord(withdrawReqVO);
        return success(true);
    }

    @PostMapping("/handle")
    @Operation(summary = "办理 NCR")
    public CommonResult<Boolean> handleNcRecord(@Valid @RequestBody QmsNcRecordHandleReqVO handleReqVO) {
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
    @Operation(summary = "设置 NCR 会签办理委托")
    public CommonResult<Boolean> delegateMrbReview(@Valid @RequestBody QmsNcMrbReviewDelegateReqVO delegateReqVO) {
        qmsNcRecordService.delegateMrbReview(delegateReqVO);
        return success(true);
    }

    @PostMapping("/return")
    @Operation(summary = "退回 NCR")
    public CommonResult<Boolean> returnNcRecord(@Valid @RequestBody QmsNcRecordReturnReqVO returnReqVO) {
        qmsNcRecordService.returnNcRecord(returnReqVO);
        return success(true);
    }

    @PostMapping("/final-approve")
    @Operation(summary = "NCR 终审")
    public CommonResult<Boolean> finalApproveNcRecord(@Valid @RequestBody QmsNcRecordFinalApproveReqVO approveReqVO) {
        qmsNcRecordService.finalApproveNcRecord(approveReqVO);
        return success(true);
    }

    @PostMapping("/disposition-scope/confirm")
    @Operation(summary = "确认产品 NCR 处置范围并提交执行")
    public CommonResult<QmsNcDispositionExecutionRespVO> confirmDispositionScope(
            @Valid @RequestBody QmsNcDispositionConfirmReqVO confirmReqVO) {
        return success(qmsNcDispositionService.confirmDispositionScope(confirmReqVO));
    }

    @PostMapping("/stock-dispose")
    @Operation(summary = "NCR 库存处置确认")
    public CommonResult<Boolean> stockDisposeNcRecord(@Valid @RequestBody QmsNcRecordStockDisposeReqVO disposeReqVO) {
        qmsNcRecordService.stockDisposeNcRecord(disposeReqVO);
        return success(true);
    }

    @PostMapping("/disposition-notify/reply")
    @Operation(summary = "NCR 处置执行通知人回复")
    public CommonResult<Boolean> replyDispositionNotify(
            @Valid @RequestBody QmsNcDispositionNotifyReplyReqVO replyReqVO) {
        qmsNcRecordService.replyDispositionNotify(replyReqVO);
        return success(true);
    }

    @GetMapping("/flow-log/list")
    @Operation(summary = "获得 NCR 流程日志")
    @Parameter(name = "ncRecordId", description = "NCR ID", required = true)
    public CommonResult<List<QmsNcRecordRespVO.FlowLog>> getFlowLogList(@RequestParam("ncRecordId") Long ncRecordId) {
        return success(qmsNcRecordService.getFlowLogList(ncRecordId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出 NCR Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportNcRecordExcel(@Valid QmsNcRecordPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        markProductNc(pageReqVO);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsNcRecordRespVO> list = qmsNcRecordService.getNcRecordPage(pageReqVO).getList();
        ExcelUtils.write(response, "NCR不合格处置台账.xls", "数据", QmsNcRecordRespVO.class, list);
    }

    private void markProductNc(QmsNcRecordPageReqVO pageVO) {
        pageVO.setExcludeRawMaterial(true);
    }
}
