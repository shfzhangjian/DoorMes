package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcPendingRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcScanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import cn.iocoder.yudao.module.mes.service.qms.QmsOqcService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
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

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - OQC出货检验")
@RestController
@RequestMapping("/mes/quality/oqc")
@Validated
public class QmsOqcController {

    @Resource
    private QmsOqcService qmsOqcService;

    @PostMapping("/create")
    @Operation(summary = "创建OQC出货检验单")
    public CommonResult<Long> createOqc(@Valid @RequestBody QmsOqcSaveReqVO createReqVO) {
        return success(qmsOqcService.createOqc(createReqVO));
    }

    @PostMapping("/create-from-shipping-notice")
    @Operation(summary = "按发货通知单生成OQC出货检验单")
    public CommonResult<Long> createOqcFromShippingNotice(@RequestParam("shippingNoticeItemId") Long shippingNoticeItemId,
                                                          @RequestParam(value = "standardId", required = false) Long standardId) {
        return success(qmsOqcService.createOqcFromShippingNotice(shippingNoticeItemId, standardId));
    }

    @PutMapping("/update")
    @Operation(summary = "更新OQC出货检验单")
    public CommonResult<Boolean> updateOqc(@Valid @RequestBody QmsOqcSaveReqVO updateReqVO) {
        qmsOqcService.updateOqc(updateReqVO);
        return success(true);
    }

    @PutMapping("/suspend")
    @Operation(summary = "挂起OQC出货检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> suspendOqc(@RequestParam("id") Long id) {
        qmsOqcService.suspendOqc(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除OQC出货检验单")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteOqc(@RequestParam("id") Long id) {
        qmsOqcService.deleteOqc(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除OQC出货检验单")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteOqcList(@RequestParam("ids") List<Long> ids) {
        qmsOqcService.deleteOqcList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取OQC出货检验详情")
    public CommonResult<QmsOqcRespVO> getOqc(@RequestParam("id") Long id) {
        return success(qmsOqcService.getOqcResp(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获取OQC出货检验分页")
    public CommonResult<PageResult<QmsOqcRespVO>> getOqcPage(@Valid QmsOqcPageReqVO pageReqVO) {
        PageResult<QmsOqcOrderDO> pageResult = qmsOqcService.getOqcPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, QmsOqcRespVO.class));
    }

    @GetMapping("/pending-list")
    @Operation(summary = "获取OQC待检发货通知单")
    public CommonResult<List<QmsOqcPendingRespVO>> getPendingOqcList(
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(qmsOqcService.getPendingOqcList(keyword));
    }

    @PostMapping("/scan/resolve")
    @Operation(summary = "解析OQC扫码并定位出货检验单")
    public CommonResult<QmsOqcScanRespVO> resolveScan(@Valid @RequestBody QmsOqcScanReqVO scanReqVO) {
        return success(qmsOqcService.resolveScan(scanReqVO));
    }

    @GetMapping("/standard-by-product")
    @Operation(summary = "按物料/型号带出OQC出货检验标准")
    public CommonResult<QmsOqcStandardRespVO> getOqcStandardByProduct(
            @RequestParam(value = "materialCode", required = false) String materialCode,
            @RequestParam(value = "modelCode", required = false) String modelCode,
            @RequestParam(value = "standardId", required = false) Long standardId) {
        return success(qmsOqcService.getOqcStandard(materialCode, modelCode, standardId));
    }

    @PutMapping("/program-entry/save")
    @Operation(summary = "保存OQC程序化录入草稿")
    public CommonResult<QmsOqcRespVO> saveProgramEntry(@RequestBody QmsOqcSaveReqVO saveReqVO) {
        return success(qmsOqcService.saveProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/recalculate")
    @Operation(summary = "重算OQC程序化录入判定")
    public CommonResult<QmsOqcRespVO> recalculateProgramEntry(@RequestBody QmsOqcSaveReqVO saveReqVO) {
        return success(qmsOqcService.recalculateProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/submit")
    @Operation(summary = "提交OQC概览确认")
    public CommonResult<QmsOqcRespVO> submitProgramEntry(@RequestBody QmsOqcSaveReqVO saveReqVO) {
        return success(qmsOqcService.submitProgramEntry(saveReqVO));
    }

    @PutMapping("/program-entry/audit")
    @Operation(summary = "审核OQC出货放行/拦截")
    public CommonResult<QmsOqcRespVO> auditProgramEntry(@Valid @RequestBody QmsOqcAuditReqVO auditReqVO) {
        return success(qmsOqcService.auditProgramEntry(auditReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出OQC出货检验 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportOqcExcel(@Valid QmsOqcPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsOqcOrderDO> list = qmsOqcService.getOqcPage(pageReqVO).getList();
        ExcelUtils.write(response, "OQC出货检验.xls", "数据", QmsOqcRespVO.class,
                BeanUtils.toBean(list, QmsOqcRespVO.class));
    }
}
