package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSupplierPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSupplierRespVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmSampleRequestService;
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

@Tag(name = "管理后台 - SRM样品需求")
@RestController
@RequestMapping("/mes/srm/sample-request")
@Validated
public class SrmSampleRequestController {

    @Resource
    private SrmSampleRequestService sampleRequestService;

    @PostMapping("/create")
    @Operation(summary = "创建样品需求单草稿")
    public CommonResult<Long> create(@Valid @RequestBody SrmSampleRequestSaveReqVO reqVO) {
        return success(sampleRequestService.createSampleRequest(reqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新样品需求单草稿")
    public CommonResult<Boolean> update(
            @Validated({SrmSampleRequestSaveReqVO.Update.class})
            @RequestBody SrmSampleRequestSaveReqVO reqVO) {
        sampleRequestService.updateSampleRequest(reqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除样品需求单草稿")
    public CommonResult<Boolean> delete(@RequestParam("id") Long id) {
        sampleRequestService.deleteSampleRequest(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得样品需求单详情")
    public CommonResult<SrmSampleRequestRespVO> get(@RequestParam("id") Long id) {
        return success(sampleRequestService.getSampleRequest(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得样品需求单分页")
    public CommonResult<PageResult<SrmSampleRequestRespVO>> page(@Valid SrmSampleRequestPageReqVO reqVO) {
        return success(sampleRequestService.getSampleRequestPage(reqVO));
    }

    @GetMapping("/supplier-page")
    @Operation(summary = "获得样品需求单可选供应商分页")
    public CommonResult<PageResult<SrmSampleRequestSupplierRespVO>> supplierPage(
            @Valid SrmSampleRequestSupplierPageReqVO reqVO) {
        return success(sampleRequestService.getSupplierPage(reqVO));
    }

    @PutMapping("/submit")
    @Operation(summary = "提交样品需求单并启动审批流程")
    public CommonResult<Boolean> submit(@Valid @RequestBody SrmSampleRequestActionReqVO.Submit reqVO) {
        sampleRequestService.submit(reqVO);
        return success(true);
    }

    @PutMapping("/project-review")
    @Operation(summary = "项目负责人审核样品需求单")
    public CommonResult<Boolean> projectReview(@Valid @RequestBody SrmSampleRequestActionReqVO.Review reqVO) {
        sampleRequestService.projectReview(reqVO);
        return success(true);
    }

    @PutMapping("/purchase-review")
    @Operation(summary = "采购负责人审核样品需求单")
    public CommonResult<Boolean> purchaseReview(@Valid @RequestBody SrmSampleRequestActionReqVO.Review reqVO) {
        sampleRequestService.purchaseReview(reqVO);
        return success(true);
    }

    @PutMapping("/initiator-decision")
    @Operation(summary = "发起人确认归档或选择最终批准人")
    public CommonResult<Boolean> initiatorDecision(
            @Valid @RequestBody SrmSampleRequestActionReqVO.InitiatorDecision reqVO) {
        sampleRequestService.initiatorDecision(reqVO);
        return success(true);
    }

    @PutMapping("/final-approve")
    @Operation(summary = "最终批准人审核样品需求单")
    public CommonResult<Boolean> finalApprove(@Valid @RequestBody SrmSampleRequestActionReqVO.Review reqVO) {
        sampleRequestService.finalApprove(reqVO);
        return success(true);
    }

    @PutMapping("/archive-confirm")
    @Operation(summary = "发起人确认归档样品需求单")
    public CommonResult<Boolean> archiveConfirm(
            @Valid @RequestBody SrmSampleRequestActionReqVO.ArchiveConfirm reqVO) {
        sampleRequestService.archiveConfirm(reqVO);
        return success(true);
    }

}
