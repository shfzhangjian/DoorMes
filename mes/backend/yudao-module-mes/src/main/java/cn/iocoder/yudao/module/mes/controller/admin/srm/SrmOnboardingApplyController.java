package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplySaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
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

@Tag(name = "管理后台 - SRM供应商导入申请")
@RestController
@RequestMapping("/mes/srm/onboarding-apply")
@Validated
public class SrmOnboardingApplyController {

    @Resource
    private SrmService srmService;

    @PostMapping("/create")
    @Operation(summary = "创建供应商导入申请")
    public CommonResult<Long> createOnboardingApply(@Valid @RequestBody SrmOnboardingApplySaveReqVO reqVO) {
        return success(srmService.createOnboardingApply(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供应商导入申请")
    public CommonResult<Boolean> updateOnboardingApply(
            @Validated({Default.class, SrmOnboardingApplySaveReqVO.Update.class})
            @RequestBody SrmOnboardingApplySaveReqVO reqVO) {
        srmService.updateOnboardingApply(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供应商导入申请")
    public CommonResult<Boolean> deleteOnboardingApply(@RequestParam("id") Long id) {
        srmService.deleteOnboardingApply(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供应商导入申请")
    public CommonResult<SrmOnboardingApplyRespVO> getOnboardingApply(@RequestParam("id") Long id) {
        return success(srmService.getOnboardingApply(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供应商导入申请分页")
    public CommonResult<PageResult<SrmOnboardingApplyRespVO>> getOnboardingApplyPage(
            @Valid SrmOnboardingApplyPageReqVO reqVO) {
        return success(srmService.getOnboardingApplyPage(reqVO));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交导入申请并启动审批流程")
    public CommonResult<Boolean> submit(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Submit reqVO) {
        srmService.submitOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/purchase-intake")
    @Operation(summary = "采购部门办理导入申请")
    public CommonResult<Boolean> purchaseIntake(
            @Valid @RequestBody SrmOnboardingApplyActionReqVO.PurchaseIntake reqVO) {
        srmService.purchaseIntakeOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/sign")
    @Operation(summary = "部门会签导入申请")
    public CommonResult<Boolean> sign(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Sign reqVO) {
        srmService.signOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/purchase-transfer")
    @Operation(summary = "采购部转办导入申请")
    public CommonResult<Boolean> purchaseTransfer(
            @Valid @RequestBody SrmOnboardingApplyActionReqVO.PurchaseTransfer reqVO) {
        srmService.purchaseTransferOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/entry")
    @Operation(summary = "交办录入导入申请")
    public CommonResult<Boolean> entry(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Entry reqVO) {
        srmService.entryOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/use-dept-review")
    @Operation(summary = "使用部门审批导入申请")
    public CommonResult<Boolean> useDeptReview(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Review reqVO) {
        srmService.useDeptReviewOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/quality-review")
    @Operation(summary = "品质部审批导入申请")
    public CommonResult<Boolean> qualityReview(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Review reqVO) {
        srmService.qualityReviewOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/tech-review")
    @Operation(summary = "技术研发部审批导入申请")
    public CommonResult<Boolean> techReview(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Review reqVO) {
        srmService.techReviewOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/purchase-review")
    @Operation(summary = "采购部审批导入申请")
    public CommonResult<Boolean> purchaseReview(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Review reqVO) {
        srmService.purchaseReviewOnboardingApply(reqVO);
        return success(true);
    }

    @PutMapping("/general-manager-review")
    @Operation(summary = "总经理审批导入申请")
    public CommonResult<Boolean> generalManagerReview(@Valid @RequestBody SrmOnboardingApplyActionReqVO.Review reqVO) {
        srmService.generalManagerReviewOnboardingApply(reqVO);
        return success(true);
    }

}
