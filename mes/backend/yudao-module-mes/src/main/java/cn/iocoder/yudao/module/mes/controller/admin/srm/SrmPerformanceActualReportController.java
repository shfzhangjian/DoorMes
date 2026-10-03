package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportAvailableSupplierRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmPerformanceActualReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM供应商绩效实际上报")
@RestController
@RequestMapping("/mes/srm/performance-actual-report")
@Validated
public class SrmPerformanceActualReportController {

    @Resource
    private SrmPerformanceActualReportService reportService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商绩效实际上报单")
    public CommonResult<Long> create(@Valid @RequestBody SrmPerformanceActualReportSaveReqVO reqVO) {
        return success(reportService.createReport(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商绩效实际上报单")
    public CommonResult<Boolean> update(@Valid @RequestBody SrmPerformanceActualReportSaveReqVO reqVO) {
        reportService.updateReport(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商绩效实际上报单")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        reportService.deleteReport(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商绩效实际上报单详情")
    public CommonResult<SrmPerformanceActualReportRespVO> get(@RequestParam("id") Long id) {
        return success(reportService.getReport(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商绩效实际上报单分页")
    public CommonResult<PageResult<SrmPerformanceActualReportRespVO>> page(
            @Valid SrmPerformanceActualReportPageReqVO reqVO) {
        return success(reportService.getReportPage(reqVO));
    }

    @GetMapping("/available-suppliers")
    @Operation(summary = "获得可生成绩效实际上报单供应商")
    public CommonResult<List<SrmPerformanceActualReportAvailableSupplierRespVO>> availableSuppliers(
            @RequestParam("periodType") String periodType,
            @RequestParam("evalYear") Integer evalYear,
            @RequestParam(value = "evalQuarter", required = false) Integer evalQuarter,
            @RequestParam(value = "evalMonth", required = false) Integer evalMonth,
            @RequestParam(value = "supplierInfo", required = false) String supplierInfo) {
        return success(reportService.getAvailableSuppliers(periodType, evalYear, evalQuarter, evalMonth, supplierInfo));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量创建供应商绩效实际上报单")
    public CommonResult<List<Long>> batchCreate(@Valid @RequestBody SrmPerformanceActualReportBatchCreateReqVO reqVO) {
        return success(reportService.batchCreateReports(reqVO));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交绩效实际值")
    public CommonResult<Boolean> submit(@RequestParam("id") Long id) {
        reportService.submitReport(id);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认绩效实际值")
    public CommonResult<Boolean> confirm(@Valid @RequestBody SrmPerformanceActualReportActionReqVO.Confirm reqVO) {
        reportService.confirmReport(reqVO);
        return success(true);
    }

    @PutMapping("/reject")
    @Operation(summary = "退回绩效实际值")
    public CommonResult<Boolean> reject(@Valid @RequestBody SrmPerformanceActualReportActionReqVO.Confirm reqVO) {
        reportService.rejectReport(reqVO);
        return success(true);
    }

    @PutMapping("/pull-monthly-values")
    @Operation(summary = "季度汇总上报单批量获取月度实际值")
    public CommonResult<Boolean> pullMonthlyValues(@RequestParam("id") Long id) {
        reportService.pullMonthlyValues(id);
        return success(true);
    }

}
