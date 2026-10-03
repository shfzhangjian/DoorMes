package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventGenerate8dReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventLinkNcrReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskBatchSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberConfirmRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberDelegateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskSubmitReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsExceptionEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - QMS异常事件提报")
@RestController
@RequestMapping("/mes/qms-exception-event")
@Validated
public class QmsExceptionEventController {

    @Resource
    private QmsExceptionEventService qmsExceptionEventService;

    @GetMapping("/page")
    @Operation(summary = "获得异常事件分页")
    public CommonResult<PageResult<QmsExceptionEventRespVO>> getExceptionEventPage(@Valid QmsExceptionEventPageReqVO pageReqVO) {
        return success(qmsExceptionEventService.getExceptionEventPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得异常事件详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsExceptionEventRespVO> getExceptionEvent(@RequestParam("id") Long id) {
        return success(qmsExceptionEventService.getExceptionEvent(id));
    }

    @PostMapping("/create")
    @Operation(summary = "提报异常事件")
    public CommonResult<QmsExceptionEventRespVO> createExceptionEvent(@Valid @RequestBody QmsExceptionEventCreateReqVO createReqVO) {
        return success(qmsExceptionEventService.createExceptionEvent(createReqVO));
    }

    @PostMapping("/handle")
    @Operation(summary = "办理异常事件")
    public CommonResult<Boolean> handleExceptionEvent(@Valid @RequestBody QmsExceptionEventHandleReqVO handleReqVO) {
        qmsExceptionEventService.handleExceptionEvent(handleReqVO);
        return success(true);
    }

    @PostMapping("/return")
    @Operation(summary = "退回异常事件")
    public CommonResult<Boolean> returnExceptionEvent(@Valid @RequestBody QmsExceptionEventReturnReqVO returnReqVO) {
        qmsExceptionEventService.returnExceptionEvent(returnReqVO);
        return success(true);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "撤回异常事件至上一环节修改")
    public CommonResult<Boolean> withdrawExceptionEvent(@Valid @RequestBody QmsExceptionEventWithdrawReqVO withdrawReqVO) {
        qmsExceptionEventService.withdrawExceptionEvent(withdrawReqVO);
        return success(true);
    }

    @PostMapping("/close")
    @Operation(summary = "关闭异常事件")
    public CommonResult<Boolean> closeExceptionEvent(@Valid @RequestBody QmsExceptionEventCloseReqVO closeReqVO) {
        qmsExceptionEventService.closeExceptionEvent(closeReqVO);
        return success(true);
    }

    @PostMapping("/group-task/save-dispatch")
    @Operation(summary = "下达异常事件临时小组任务")
    public CommonResult<Boolean> saveGroupTasks(@Valid @RequestBody QmsExceptionGroupTaskBatchSaveReqVO saveReqVO) {
        qmsExceptionEventService.saveGroupTasks(saveReqVO);
        return success(true);
    }

    @PostMapping("/group-task/submit")
    @Operation(summary = "提交异常事件临时小组任务")
    public CommonResult<Boolean> submitGroupTask(@Valid @RequestBody QmsExceptionGroupTaskSubmitReqVO submitReqVO) {
        qmsExceptionEventService.submitGroupTask(submitReqVO);
        return success(true);
    }

    @PostMapping("/group-task/member-confirm")
    @Operation(summary = "提交异常事件部门/责任单位办理内容")
    public CommonResult<QmsExceptionGroupTaskMemberConfirmRespVO> confirmGroupTaskMember(
            @Valid @RequestBody QmsExceptionGroupTaskMemberConfirmReqVO confirmReqVO) {
        return success(qmsExceptionEventService.confirmGroupTaskMember(confirmReqVO));
    }

    @PostMapping("/group-task/member-delegate")
    @Operation(summary = "设置异常事件会签办理委托")
    public CommonResult<Boolean> delegateGroupTaskMember(
            @Valid @RequestBody QmsExceptionGroupTaskMemberDelegateReqVO delegateReqVO) {
        qmsExceptionEventService.delegateGroupTaskMember(delegateReqVO);
        return success(true);
    }

    @PostMapping("/group-task/review")
    @Operation(summary = "复核异常事件临时小组任务")
    public CommonResult<Boolean> reviewGroupTask(@Valid @RequestBody QmsExceptionGroupTaskReviewReqVO reviewReqVO) {
        qmsExceptionEventService.reviewGroupTask(reviewReqVO);
        return success(true);
    }

    @PostMapping("/link-ncr")
    @Operation(summary = "关联 NCR")
    public CommonResult<Boolean> linkNcr(@Valid @RequestBody QmsExceptionEventLinkNcrReqVO linkReqVO) {
        qmsExceptionEventService.linkNcr(linkReqVO);
        return success(true);
    }

    @PostMapping("/generate-8d")
    @Operation(summary = "生成 8D 改善报告")
    public CommonResult<Qms8dReportRespVO> generate8d(@Valid @RequestBody QmsExceptionEventGenerate8dReqVO generateReqVO) {
        return success(qmsExceptionEventService.generate8d(generateReqVO));
    }

    @GetMapping("/flow-log/list")
    @Operation(summary = "获得异常事件流程日志")
    @Parameter(name = "exceptionId", description = "异常ID", required = true)
    public CommonResult<List<QmsExceptionEventRespVO.FlowLog>> getFlowLogList(@RequestParam("exceptionId") Long exceptionId) {
        return success(qmsExceptionEventService.getFlowLogList(exceptionId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出异常事件 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExceptionEventExcel(@Valid QmsExceptionEventPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsExceptionEventRespVO> list = qmsExceptionEventService.getExceptionEventPage(pageReqVO).getList();
        ExcelUtils.write(response, "QMS异常事件台账.xls", "数据", QmsExceptionEventRespVO.class, list);
    }
}
