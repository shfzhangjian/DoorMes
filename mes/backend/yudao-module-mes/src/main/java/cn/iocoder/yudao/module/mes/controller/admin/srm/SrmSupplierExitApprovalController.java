package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalSaveReqVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmSupplierExitApprovalService;
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

@Tag(name = "管理后台 - SRM供方退出审批")
@RestController
@RequestMapping("/mes/srm/supplier-exit-approval")
@Validated
public class SrmSupplierExitApprovalController {

    @Resource
    private SrmSupplierExitApprovalService supplierExitApprovalService;

    @PostMapping("/create")
    @Operation(summary = "创建供方退出审批")
    public CommonResult<Long> createSupplierExitApproval(@Valid @RequestBody SrmSupplierExitApprovalSaveReqVO reqVO) {
        return success(supplierExitApprovalService.createSupplierExitApproval(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新供方退出审批")
    public CommonResult<Boolean> updateSupplierExitApproval(
            @Validated({Default.class, SrmSupplierExitApprovalSaveReqVO.Update.class})
            @RequestBody SrmSupplierExitApprovalSaveReqVO reqVO) {
        supplierExitApprovalService.updateSupplierExitApproval(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除供方退出审批")
    public CommonResult<Boolean> deleteSupplierExitApproval(@RequestParam("id") Long id) {
        supplierExitApprovalService.deleteSupplierExitApproval(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得供方退出审批")
    public CommonResult<SrmSupplierExitApprovalRespVO> getSupplierExitApproval(@RequestParam("id") Long id) {
        return success(supplierExitApprovalService.getSupplierExitApproval(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得供方退出审批分页")
    public CommonResult<PageResult<SrmSupplierExitApprovalRespVO>> getSupplierExitApprovalPage(
            @Valid SrmSupplierExitApprovalPageReqVO reqVO) {
        return success(supplierExitApprovalService.getSupplierExitApprovalPage(reqVO));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交退出审批并启动审批流程")
    public CommonResult<Boolean> submit(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Submit reqVO) {
        supplierExitApprovalService.submitSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/purchase-intake")
    @Operation(summary = "采购部门办理退出审批")
    public CommonResult<Boolean> purchaseIntake(
            @Valid @RequestBody SrmSupplierExitApprovalActionReqVO.PurchaseIntake reqVO) {
        supplierExitApprovalService.purchaseIntakeSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/sign")
    @Operation(summary = "部门会签退出审批")
    public CommonResult<Boolean> sign(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Sign reqVO) {
        supplierExitApprovalService.signSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/purchase-transfer")
    @Operation(summary = "采购部转办退出审批")
    public CommonResult<Boolean> purchaseTransfer(
            @Valid @RequestBody SrmSupplierExitApprovalActionReqVO.PurchaseTransfer reqVO) {
        supplierExitApprovalService.purchaseTransferSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/entry")
    @Operation(summary = "交办录入退出审批")
    public CommonResult<Boolean> entry(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Entry reqVO) {
        supplierExitApprovalService.entrySupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/use-dept-review")
    @Operation(summary = "使用部门审批退出审批")
    public CommonResult<Boolean> useDeptReview(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        supplierExitApprovalService.useDeptReviewSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/quality-review")
    @Operation(summary = "品质部审批退出审批")
    public CommonResult<Boolean> qualityReview(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        supplierExitApprovalService.qualityReviewSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/tech-review")
    @Operation(summary = "技术研发部审批退出审批")
    public CommonResult<Boolean> techReview(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        supplierExitApprovalService.techReviewSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/purchase-review")
    @Operation(summary = "采购部审批退出审批")
    public CommonResult<Boolean> purchaseReview(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        supplierExitApprovalService.purchaseReviewSupplierExitApproval(reqVO);
        return success(true);
    }

    @PutMapping("/general-manager-review")
    @Operation(summary = "总经理审批退出审批")
    public CommonResult<Boolean> generalManagerReview(@Valid @RequestBody SrmSupplierExitApprovalActionReqVO.Review reqVO) {
        supplierExitApprovalService.generalManagerReviewSupplierExitApproval(reqVO);
        return success(true);
    }

}
