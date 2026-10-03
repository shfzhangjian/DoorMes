package cn.iocoder.yudao.module.mes.controller.admin.srm;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierNameCheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierResourceStatusAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierResourceStatusLogRespVO;
import cn.iocoder.yudao.module.mes.service.srm.SrmSupplierCandidateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - SRM供应商统一候选")
@RestController
@RequestMapping("/mes/srm/supplier-candidate")
@Validated
public class SrmSupplierCandidateController {

    @Resource
    private SrmSupplierCandidateService supplierCandidateService;

    @GetMapping("/page")
    @Operation(summary = "分页查询供应商资源")
    public CommonResult<PageResult<SrmSupplierCandidateRespVO>> getCandidatePage(
            @Valid SrmSupplierCandidatePageReqVO reqVO) {
        return success(supplierCandidateService.getCandidatePage(reqVO));
    }

    @GetMapping("/select-page")
    @Operation(summary = "分页查询供应商选择项")
    public CommonResult<PageResult<SrmSupplierCandidateRespVO>> getCandidateSelectPage(
            @Valid SrmSupplierCandidatePageReqVO reqVO) {
        return success(supplierCandidateService.getCandidateSelectPage(reqVO));
    }

    @GetMapping("/name-check")
    @Operation(summary = "检查供应商资源名称重复")
    public CommonResult<SrmSupplierNameCheckRespVO> checkSupplierName(
            @RequestParam("supplierName") String supplierName) {
        return success(supplierCandidateService.checkSupplierName(supplierName));
    }

    @PutMapping("/adjust-resource-status")
    @Operation(summary = "调整供应商资源状态")
    public CommonResult<Boolean> adjustResourceStatus(
            @Valid @RequestBody SrmSupplierResourceStatusAdjustReqVO reqVO) {
        supplierCandidateService.adjustResourceStatus(reqVO);
        return success(true);
    }

    @GetMapping("/resource-status-log-list")
    @Operation(summary = "查询供应商资源状态调整日志")
    public CommonResult<List<SrmSupplierResourceStatusLogRespVO>> getResourceStatusLogs(
            @RequestParam("supplierId") Long supplierId) {
        return success(supplierCandidateService.getResourceStatusLogs(supplierId));
    }

}
