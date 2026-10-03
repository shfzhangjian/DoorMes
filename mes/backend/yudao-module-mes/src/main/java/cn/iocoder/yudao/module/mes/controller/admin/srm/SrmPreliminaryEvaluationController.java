package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmPreliminaryEvaluationService;
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

@Tag(name = "管理后台 - SRM供应商选择初评")
@RestController
@RequestMapping("/mes/srm/preliminary-evaluation")
@Validated
public class SrmPreliminaryEvaluationController {

    @Resource
    private SrmPreliminaryEvaluationService evaluationService;

    @PostMapping("/create")
    @Operation(summary = "创建选择初评并固化模板快照")
    public CommonResult<Long> create(@Valid @RequestBody SrmPreliminaryEvaluationSaveReqVO reqVO) {
        return success(evaluationService.createEvaluation(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新草稿初评单")
    public CommonResult<Boolean> update(@Valid @RequestBody SrmPreliminaryEvaluationSaveReqVO reqVO) {
        evaluationService.updateEvaluation(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除草稿初评单")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        evaluationService.deleteEvaluation(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得选择初评详情（服务端按当前用户脱敏）")
    public CommonResult<SrmPreliminaryEvaluationRespVO> get(@RequestParam("id") Long id) {
        return success(evaluationService.getEvaluation(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得选择初评分页")
    public CommonResult<PageResult<SrmPreliminaryEvaluationRespVO>> page(
            @Valid SrmPreliminaryEvaluationPageReqVO reqVO) {
        return success(evaluationService.getEvaluationPage(reqVO));
    }

    @PutMapping("/assign-scorers")
    @Operation(summary = "管理员/发起人维护指标评分人")
    public CommonResult<Boolean> assignScorers(
            @Valid @RequestBody SrmPreliminaryEvaluationActionReqVO.AssignScorer reqVO) {
        evaluationService.assignScorers(reqVO);
        return success(true);
    }

    @PutMapping("/send")
    @Operation(summary = "确认发送并启动初评 BPM 流程")
    public CommonResult<Boolean> send(@RequestParam("id") Long id) {
        evaluationService.sendForScoring(id);
        return success(true);
    }

    @PutMapping("/score")
    @Operation(summary = "评分人提交本人评分")
    public CommonResult<Boolean> score(@Valid @RequestBody SrmPreliminaryEvaluationActionReqVO.Score reqVO) {
        evaluationService.submitScore(reqVO);
        return success(true);
    }

    @PutMapping("/calculate")
    @Operation(summary = "发起人计算总分与自动判定")
    public CommonResult<Boolean> calculate(@RequestParam("id") Long id) {
        evaluationService.calculateScore(id);
        return success(true);
    }

    @PutMapping("/decision")
    @Operation(summary = "填写最终导入判定并选择是否送总经理")
    public CommonResult<Boolean> decision(
            @Valid @RequestBody SrmPreliminaryEvaluationActionReqVO.Decision reqVO) {
        evaluationService.submitDecision(reqVO);
        return success(true);
    }

    @PutMapping("/general-manager-opinion")
    @Operation(summary = "总经理办理并填写意见")
    public CommonResult<Boolean> generalManagerOpinion(
            @Valid @RequestBody SrmPreliminaryEvaluationActionReqVO.GeneralManagerOpinion reqVO) {
        evaluationService.submitGeneralManagerOpinion(reqVO);
        return success(true);
    }

    @PutMapping("/publish")
    @Operation(summary = "发布评估表并抄送")
    public CommonResult<Boolean> publish(@Valid @RequestBody SrmPreliminaryEvaluationActionReqVO.Publish reqVO) {
        evaluationService.publish(reqVO);
        return success(true);
    }

}
