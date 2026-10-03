package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcPendingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFgShippingFqcShippingDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsFgShippingFqcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 发货成品检验")
@RestController
@RequestMapping("/mes/quality/fg-shipping-fqc")
@Validated
public class QmsFgShippingFqcController {

    @Resource
    private QmsFgShippingFqcService qmsFgShippingFqcService;

    @GetMapping("/pending-list")
    @Operation(summary = "获取发货成品检验待检发货通知单")
    public CommonResult<List<QmsFgShippingFqcPendingRespVO>> getPendingList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(qmsFgShippingFqcService.getPendingList(keyword));
    }

    @PostMapping("/create-from-shipping-notice")
    @Operation(summary = "按发货通知单生成发货成品检验单")
    public CommonResult<QmsFgShippingFqcRespVO> createFromShippingNotice(
            @RequestParam("shippingNoticeId") Long shippingNoticeId) {
        return success(qmsFgShippingFqcService.createFromShippingNotice(shippingNoticeId));
    }

    @GetMapping("/page")
    @Operation(summary = "获取发货成品检验分页")
    public CommonResult<PageResult<QmsFgShippingFqcRespVO>> getPage(@Valid QmsFgShippingFqcPageReqVO pageReqVO) {
        return success(qmsFgShippingFqcService.getPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获取发货成品检验详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsFgShippingFqcRespVO> get(@RequestParam("id") Long id) {
        return success(qmsFgShippingFqcService.get(id));
    }

    @GetMapping("/defect-code-options")
    @Operation(summary = "获取发货成品检验缺陷代码选项")
    public CommonResult<List<QmsDefectCodeRespVO>> getDefectCodeOptions() {
        return success(qmsFgShippingFqcService.getDefectCodeOptions());
    }

    @PutMapping("/program-entry/save")
    @Operation(summary = "保存发货成品检验程序化录入草稿")
    public CommonResult<QmsFgShippingFqcRespVO> saveProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFgShippingFqcService.saveProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/submit")
    @Operation(summary = "提交发货成品检验检测结果")
    public CommonResult<QmsFgShippingFqcRespVO> submitProgramEntry(@RequestBody QmsFqcSaveReqVO saveReqVO) {
        return success(qmsFgShippingFqcService.submitProgramEntry(saveReqVO));
    }

    @PutMapping("/shipping-detail/save")
    @Operation(summary = "保存发货成品检验片级判定")
    public CommonResult<QmsFgShippingFqcRespVO> saveShippingDetails(
            @Valid @RequestBody QmsFgShippingFqcShippingDetailSaveReqVO reqVO) {
        return success(qmsFgShippingFqcService.saveShippingDetails(reqVO));
    }

    @PutMapping("/program-entry/audit")
    @Operation(summary = "审核发货成品检验并自动退回NG配货片")
    public CommonResult<QmsFgShippingFqcRespVO> audit(@Valid @RequestBody QmsFqcAuditReqVO auditReqVO) {
        return success(qmsFgShippingFqcService.audit(auditReqVO));
    }

    @PostMapping("/scan/resolve")
    @Operation(summary = "解析发货成品检验扫码并定位发货明细")
    public CommonResult<QmsFgShippingFqcScanRespVO> resolveScan(@Valid @RequestBody QmsFqcScanReqVO scanReqVO) {
        return success(qmsFgShippingFqcService.resolveScan(scanReqVO));
    }
}
