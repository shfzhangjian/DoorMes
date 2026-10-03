package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationRespVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmTrialValidationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM试产验证跟踪")
@RestController
@RequestMapping("/mes/srm/trial-validation")
@Validated
public class SrmTrialValidationController {

    @Resource
    private SrmTrialValidationService trialValidationService;

    @GetMapping("/get")
    @Operation(summary = "获得试产验证跟踪详情")
    public CommonResult<SrmTrialValidationRespVO> get(@RequestParam("id") Long id) {
        return success(trialValidationService.getTrialValidation(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得试产验证跟踪分页")
    public CommonResult<PageResult<SrmTrialValidationRespVO>> page(@Valid SrmTrialValidationPageReqVO reqVO) {
        return success(trialValidationService.getTrialValidationPage(reqVO));
    }

    @PutMapping("/trial-execute")
    @Operation(summary = "试生产执行")
    public CommonResult<Boolean> trialExecute(@Valid @RequestBody SrmTrialValidationActionReqVO.TrialExecute reqVO) {
        trialValidationService.trialExecute(reqVO);
        return success(true);
    }

    @PutMapping("/complete-production")
    @Operation(summary = "完成生产")
    public CommonResult<Boolean> completeProduction(
            @Valid @RequestBody SrmTrialValidationActionReqVO.ProductionComplete reqVO) {
        trialValidationService.completeProduction(reqVO);
        return success(true);
    }

    @PutMapping("/archive-confirm")
    @Operation(summary = "发起人确认归档")
    public CommonResult<Boolean> archiveConfirm(
            @Valid @RequestBody SrmTrialValidationActionReqVO.ArchiveConfirm reqVO) {
        trialValidationService.archiveConfirm(reqVO);
        return success(true);
    }

}
