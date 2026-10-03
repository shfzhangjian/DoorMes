package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewExecutionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewPlanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierReviewPlanRespVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmSupplierReviewPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

@Tag(name = "管理后台 - SRM供方评审计划")
@RestController
@RequestMapping("/mes/srm/supplier-review-plan")
@Validated
public class SrmSupplierReviewPlanController {

    @Resource
    private SrmSupplierReviewPlanService supplierReviewPlanService;

    @PostMapping("/init-year")
    @Operation(summary = "初始化供方年度评审计划")
    public CommonResult<Long> initYearPlan(@RequestParam("planYear") Integer planYear) {
        return success(supplierReviewPlanService.initYearPlan(planYear));
    }

    @GetMapping("/year")
    @Operation(summary = "获得供方年度评审计划")
    public CommonResult<SrmSupplierReviewPlanRespVO.YearPlan> getYearPlan(
            @RequestParam("planYear") Integer planYear) {
        return success(supplierReviewPlanService.getYearPlan(planYear));
    }

    @PostMapping("/line/add-supplier")
    @Operation(summary = "追加供应商年度评审计划行")
    public CommonResult<Long> addSupplierLine(@Valid @RequestBody SrmSupplierReviewPlanReqVO.AddSupplierLine reqVO) {
        return success(supplierReviewPlanService.addSupplierLine(reqVO));
    }

    @PutMapping("/line/update-contact")
    @Operation(summary = "更新供方评审计划行联系人")
    public CommonResult<Boolean> updateLineContact(
            @Valid @RequestBody SrmSupplierReviewPlanReqVO.UpdateLineContact reqVO) {
        supplierReviewPlanService.updateLineContact(reqVO);
        return success(true);
    }

    @PostMapping("/line/delete-annual")
    @Operation(summary = "按年度评审日历行删除供方评审计划")
    public CommonResult<SrmSupplierReviewPlanRespVO.DeleteAnnualPlansResult> deleteAnnualPlans(
            @Valid @RequestBody SrmSupplierReviewPlanReqVO.DeleteAnnualPlans reqVO) {
        return success(supplierReviewPlanService.deleteAnnualPlans(reqVO));
    }

    @GetMapping("/month/get")
    @Operation(summary = "获得供方评审月份计划")
    public CommonResult<SrmSupplierReviewPlanRespVO.MonthPlan> getMonthPlan(@RequestParam("id") Long id) {
        return success(supplierReviewPlanService.getMonthPlan(id));
    }

    @PutMapping("/month/update")
    @Operation(summary = "更新供方评审月份计划")
    public CommonResult<Boolean> updateMonthPlan(@Valid @RequestBody SrmSupplierReviewPlanReqVO.MonthSave reqVO) {
        supplierReviewPlanService.updateMonthPlan(reqVO);
        return success(true);
    }

    @PutMapping("/month/site-inspection/save")
    @Operation(summary = "保存现场考察记录（审核类别/审定日期/审核说明/审核附件）")
    public CommonResult<Boolean> saveSiteInspection(
            @Valid @RequestBody SrmSupplierReviewPlanReqVO.SiteInspectionSave reqVO) {
        supplierReviewPlanService.saveSiteInspection(reqVO);
        return success(true);
    }

    @DeleteMapping("/month/clear")
    @Operation(summary = "清空供方评审月份计划")
    public CommonResult<Boolean> clearMonthPlan(@RequestParam("id") Long id) {
        supplierReviewPlanService.clearMonthPlan(id);
        return success(true);
    }

    @PutMapping("/month/adjust-status")
    @Operation(summary = "调整供方评审月份计划状态")
    public CommonResult<Boolean> adjustMonthStatus(
            @Valid @RequestBody SrmSupplierReviewPlanReqVO.StatusAdjust reqVO) {
        supplierReviewPlanService.adjustMonthStatus(reqVO);
        return success(true);
    }

    @PostMapping("/reply/create")
    @Operation(summary = "新增供方评审执行回复")
    public CommonResult<Long> createReply(@Valid @RequestBody SrmSupplierReviewPlanReqVO.ReplyCreate reqVO) {
        return success(supplierReviewPlanService.createReply(reqVO));
    }

    @GetMapping("/execution-page")
    @Operation(summary = "获得当前用户可见的供方评审计划执行分页")
    public CommonResult<PageResult<SrmSupplierReviewPlanRespVO.ExecutionItem>> getExecutionPage(
            @Valid SrmSupplierReviewExecutionPageReqVO reqVO) {
        return success(supplierReviewPlanService.getExecutionPage(reqVO));
    }

}
