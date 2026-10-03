package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationAvailableSupplierRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceTrendRespVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmPerformanceQuarterEvaluationService;
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

@Tag(name = "管理后台 - SRM供应商季度绩效评定")
@RestController
@RequestMapping("/mes/srm/performance-quarter-evaluation")
@Validated
public class SrmPerformanceQuarterEvaluationController {

    @Resource
    private SrmPerformanceQuarterEvaluationService evaluationService;

    @PostMapping("/create")
    @Operation(summary = "生成供应商季度绩效评价单")
    public CommonResult<Long> create(@Valid @RequestBody SrmPerformanceQuarterEvaluationSaveReqVO reqVO) {
        return success(evaluationService.createEvaluation(reqVO));
    }

    @GetMapping("/available-suppliers")
    @Operation(summary = "获得当前季度未生成评价单的供应商")
    public CommonResult<List<SrmPerformanceQuarterEvaluationAvailableSupplierRespVO>> availableSuppliers(
            @RequestParam("evalYear") Integer evalYear,
            @RequestParam("evalQuarter") Integer evalQuarter,
            @RequestParam(value = "templateVersionId", required = false) Long templateVersionId,
            @RequestParam(value = "supplierInfo", required = false) String supplierInfo,
            @RequestParam(value = "level", required = false) String level) {
        return success(evaluationService.getAvailableSuppliers(evalYear, evalQuarter, templateVersionId,
                supplierInfo, level));
    }

    @PostMapping("/batch-create")
    @Operation(summary = "批量生成供应商季度绩效评价单")
    public CommonResult<List<Long>> batchCreate(
            @Valid @RequestBody SrmPerformanceQuarterEvaluationBatchCreateReqVO reqVO) {
        return success(evaluationService.batchCreateEvaluations(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商季度绩效评价草稿")
    public CommonResult<Boolean> update(@Valid @RequestBody SrmPerformanceQuarterEvaluationSaveReqVO reqVO) {
        evaluationService.updateEvaluation(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商季度绩效评价草稿")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        evaluationService.deleteEvaluation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商季度绩效评价详情")
    public CommonResult<SrmPerformanceQuarterEvaluationRespVO> get(@RequestParam("id") Long id) {
        return success(evaluationService.getEvaluation(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商季度绩效评价分页")
    public CommonResult<PageResult<SrmPerformanceQuarterEvaluationRespVO>> page(
            @Valid SrmPerformanceQuarterEvaluationPageReqVO reqVO) {
        return success(evaluationService.getEvaluationPage(reqVO));
    }

    @GetMapping("/trend")
    @Operation(summary = "获得供应商绩效年度趋势")
    public CommonResult<SrmPerformanceTrendRespVO> trend(
            @RequestParam(value = "evaluationId", required = false) Long evaluationId,
            @RequestParam(value = "supplierId", required = false) Long supplierId,
            @RequestParam(value = "evalYear", required = false) Integer evalYear,
            @RequestParam(value = "indicatorCodes", required = false) String indicatorCodes) {
        return success(evaluationService.getPerformanceTrend(evaluationId, supplierId, evalYear, indicatorCodes));
    }

    @PutMapping("/pull-actuals")
    @Operation(summary = "拉取已确认实际值并计算指标")
    public CommonResult<Boolean> pullActuals(@RequestParam("id") Long id) {
        evaluationService.pullActuals(id);
        return success(true);
    }

    @PutMapping("/send")
    @Operation(summary = "发送季度评价人工评分")
    public CommonResult<Boolean> send(@RequestParam("id") Long id) {
        evaluationService.sendForScoring(id);
        return success(true);
    }

    @PutMapping("/score")
    @Operation(summary = "提交本人季度评价评分")
    public CommonResult<Boolean> score(@Valid @RequestBody SrmPerformanceQuarterEvaluationActionReqVO.Score reqVO) {
        evaluationService.submitScore(reqVO);
        return success(true);
    }

    @PutMapping("/calculate")
    @Operation(summary = "计算季度总分与等级")
    public CommonResult<Boolean> calculate(@RequestParam("id") Long id) {
        evaluationService.calculateTotal(id);
        return success(true);
    }

    @PutMapping("/start-sign")
    @Operation(summary = "发起季度评价会签")
    public CommonResult<Boolean> startSign(@Valid @RequestBody SrmPerformanceQuarterEvaluationActionReqVO.StartSign reqVO) {
        evaluationService.startSign(reqVO);
        return success(true);
    }

    @PutMapping("/sign")
    @Operation(summary = "提交季度评价会签意见")
    public CommonResult<Boolean> sign(@Valid @RequestBody SrmPerformanceQuarterEvaluationActionReqVO.Sign reqVO) {
        evaluationService.submitSign(reqVO);
        return success(true);
    }

}
