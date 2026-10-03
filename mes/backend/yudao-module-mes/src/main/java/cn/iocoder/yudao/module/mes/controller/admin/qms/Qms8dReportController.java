package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dActionItemDoneReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportLinkSourceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportSaveReqVO;
import cn.iocoder.yudao.module.mes.service.qms.Qms8dReportService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 8D改善报告(CAPA)")
@RestController
@RequestMapping("/mes/qms-8d-report")
@Validated
public class Qms8dReportController {

    @Resource
    private Qms8dReportService qms8dReportService;

    @GetMapping("/page")
    @Operation(summary = "获得 8D 改善报告分页")
    public CommonResult<PageResult<Qms8dReportRespVO>> getReportPage(@Valid Qms8dReportPageReqVO pageReqVO) {
        return success(qms8dReportService.getReportPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得 8D 改善报告详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Qms8dReportRespVO> getReport(@RequestParam("id") Long id) {
        return success(qms8dReportService.getReport(id));
    }

    @PostMapping("/create")
    @Operation(summary = "新建 8D 改善报告")
    public CommonResult<Long> createReport(@Valid @RequestBody Qms8dReportSaveReqVO createReqVO) {
        return success(qms8dReportService.createReport(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新 8D 改善报告")
    public CommonResult<Boolean> updateReport(@Valid @RequestBody Qms8dReportSaveReqVO updateReqVO) {
        qms8dReportService.updateReport(updateReqVO);
        return success(true);
    }

    @PostMapping("/handle")
    @Operation(summary = "办理 8D 改善报告")
    public CommonResult<Boolean> handleReport(@Valid @RequestBody Qms8dReportHandleReqVO handleReqVO) {
        qms8dReportService.handleReport(handleReqVO);
        return success(true);
    }

    @PostMapping("/return")
    @Operation(summary = "退回 8D 改善报告")
    public CommonResult<Boolean> returnReport(@Valid @RequestBody Qms8dReportReturnReqVO returnReqVO) {
        qms8dReportService.returnReport(returnReqVO);
        return success(true);
    }

    @PostMapping("/close")
    @Operation(summary = "关闭 8D 改善报告")
    public CommonResult<Boolean> closeReport(@Valid @RequestBody Qms8dReportCloseReqVO closeReqVO) {
        qms8dReportService.closeReport(closeReqVO);
        return success(true);
    }

    @PostMapping("/link-source")
    @Operation(summary = "关联 8D 来源对象")
    public CommonResult<Boolean> linkSource(@Valid @RequestBody Qms8dReportLinkSourceReqVO linkReqVO) {
        qms8dReportService.linkSource(linkReqVO);
        return success(true);
    }

    @PostMapping({"/action-item-done", "/action-item/done"})
    @Operation(summary = "完成 8D CAPA 行动项")
    public CommonResult<Boolean> actionItemDone(@Valid @RequestBody Qms8dActionItemDoneReqVO doneReqVO) {
        qms8dReportService.actionItemDone(doneReqVO);
        return success(true);
    }

    @GetMapping("/flow-log/list")
    @Operation(summary = "获得 8D 流程日志")
    @Parameter(name = "reportId", description = "8D 报告ID", required = true)
    public CommonResult<List<Qms8dReportRespVO.FlowLog>> getFlowLogList(@RequestParam("reportId") Long reportId) {
        return success(qms8dReportService.getFlowLogList(reportId));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出 8D 改善报告 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportReportExcel(@Valid Qms8dReportPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<Qms8dReportRespVO> list = qms8dReportService.getReportPage(pageReqVO).getList();
        ExcelUtils.write(response, "8D改善报告台账.xls", "数据", Qms8dReportRespVO.class, list);
    }
}
