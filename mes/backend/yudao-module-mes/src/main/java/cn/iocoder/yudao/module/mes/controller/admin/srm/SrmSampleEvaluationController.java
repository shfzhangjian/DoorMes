package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationSampleRequestPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestRespVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmSampleEvaluationService;
import cn.iocoder.yudao.module.mes.service.srm.SrmTrialValidationService;
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

@Tag(name = "管理后台 - SRM样品评价")
@RestController
@RequestMapping("/mes/srm/sample-evaluation")
@Validated
public class SrmSampleEvaluationController {

    @Resource
    private SrmSampleEvaluationService sampleEvaluationService;
    @Resource
    private SrmTrialValidationService trialValidationService;

    @PostMapping("/create")
    @Operation(summary = "创建样品评价表草稿")
    public CommonResult<Long> create(@Valid @RequestBody SrmSampleEvaluationSaveReqVO reqVO) {
        return success(sampleEvaluationService.createSampleEvaluation(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样品评价表草稿")
    public CommonResult<Boolean> update(
            @Validated({SrmSampleEvaluationSaveReqVO.Update.class})
            @RequestBody SrmSampleEvaluationSaveReqVO reqVO) {
        sampleEvaluationService.updateSampleEvaluation(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样品评价表草稿")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        sampleEvaluationService.deleteSampleEvaluation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样品评价表详情")
    public CommonResult<SrmSampleEvaluationRespVO> get(@RequestParam("id") Long id) {
        return success(sampleEvaluationService.getSampleEvaluation(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样品评价表分页")
    public CommonResult<PageResult<SrmSampleEvaluationRespVO>> page(@Valid SrmSampleEvaluationPageReqVO reqVO) {
        return success(sampleEvaluationService.getSampleEvaluationPage(reqVO));
    }

    @GetMapping("/project/enabled-list")
    @Operation(summary = "获得启用的样品评价项目配置")
    public CommonResult<List<SrmSampleEvaluationProjectRespVO>> enabledProjectList() {
        return success(sampleEvaluationService.getEnabledProjects());
    }

    @GetMapping("/project/get")
    @Operation(summary = "获得样品评价项目配置详情")
    public CommonResult<SrmSampleEvaluationProjectRespVO> projectConfig(@RequestParam("projectId") Long projectId) {
        return success(sampleEvaluationService.getProjectConfig(projectId));
    }

    @GetMapping("/assignable-inspectors")
    @Operation(summary = "获得当前项目可分配的检测办理人")
    public CommonResult<SrmSampleEvaluationProjectRespVO.AssignableInspectors> assignableInspectors(
            @RequestParam("projectId") Long projectId) {
        return success(sampleEvaluationService.getAssignableInspectors(projectId));
    }

    @GetMapping("/sample-request-page")
    @Operation(summary = "获得可选择的样品需求单分页")
    public CommonResult<PageResult<SrmSampleRequestRespVO>> sampleRequestPage(
            @Valid SrmSampleEvaluationSampleRequestPageReqVO reqVO) {
        return success(sampleEvaluationService.getSelectableSampleRequestPage(reqVO));
    }

    @GetMapping("/latest-items")
    @Operation(summary = "获得同一样品需求单最近一次检验项目")
    public CommonResult<List<SrmSampleEvaluationRespVO.Item>> latestItems(@RequestParam("sampleRequestId") Long sampleRequestId) {
        return success(sampleEvaluationService.getLatestItemsBySampleRequestId(sampleRequestId));
    }

    @GetMapping("/history-page")
    @Operation(summary = "获得同一样品需求单历史样品评价分页")
    public CommonResult<PageResult<SrmSampleEvaluationRespVO>> historyPage(@RequestParam("sampleRequestId") Long sampleRequestId,
                                                                          @Valid SrmSampleEvaluationPageReqVO reqVO) {
        return success(sampleEvaluationService.getHistoryPage(sampleRequestId, reqVO));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交样品评价表并启动流程")
    public CommonResult<Boolean> submit(@Valid @RequestBody SrmSampleEvaluationActionReqVO.Submit reqVO) {
        sampleEvaluationService.submit(reqVO);
        return success(true);
    }

    @PutMapping("/inspection-report")
    @Operation(summary = "样品检验上报")
    public CommonResult<Boolean> inspectionReport(
            @Valid @RequestBody SrmSampleEvaluationActionReqVO.InspectionReport reqVO) {
        sampleEvaluationService.inspectionReport(reqVO);
        return success(true);
    }

    @PutMapping("/value-confirm")
    @Operation(summary = "检验值确认")
    public CommonResult<Boolean> valueConfirm(@Valid @RequestBody SrmSampleEvaluationActionReqVO.ValueConfirm reqVO) {
        sampleEvaluationService.valueConfirm(reqVO);
        return success(true);
    }

    @PutMapping("/sign")
    @Operation(summary = "部门确认")
    public CommonResult<Boolean> sign(@Valid @RequestBody SrmSampleEvaluationActionReqVO.Sign reqVO) {
        sampleEvaluationService.sign(reqVO);
        return success(true);
    }

    @PutMapping("/initiator-decision")
    @Operation(summary = "发起人提交最终审批")
    public CommonResult<Boolean> initiatorDecision(
            @Valid @RequestBody SrmSampleEvaluationActionReqVO.InitiatorDecision reqVO) {
        sampleEvaluationService.initiatorDecision(reqVO);
        return success(true);
    }

    @PutMapping("/final-approve")
    @Operation(summary = "最终批准")
    public CommonResult<Boolean> finalApprove(@Valid @RequestBody SrmSampleEvaluationActionReqVO.FinalApprove reqVO) {
        sampleEvaluationService.finalApprove(reqVO);
        return success(true);
    }

    @PutMapping("/archive-confirm")
    @Operation(summary = "样品评价完成归档")
    public CommonResult<Boolean> archiveConfirm(
            @Valid @RequestBody SrmSampleEvaluationActionReqVO.ArchiveConfirm reqVO) {
        sampleEvaluationService.archiveConfirm(reqVO);
        return success(true);
    }

    @PutMapping("/withdraw-confirm")
    @Operation(summary = "会签前撤回到检测结果确认")
    public CommonResult<Boolean> withdrawConfirm(
            @Valid @RequestBody SrmSampleEvaluationActionReqVO.WithdrawConfirm reqVO) {
        sampleEvaluationService.withdrawConfirm(reqVO);
        return success(true);
    }

    @PutMapping("/issue-trial-validation")
    @Operation(summary = "从样品评价下达试生产通知")
    public CommonResult<Long> issueTrialValidation(@RequestParam("id") Long id) {
        return success(trialValidationService.issueFromSampleEvaluation(id));
    }

}
