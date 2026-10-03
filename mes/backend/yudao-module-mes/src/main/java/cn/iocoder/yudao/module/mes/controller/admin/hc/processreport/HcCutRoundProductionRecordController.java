package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcCutRoundConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 裁切生产记录表")
@RestController
@RequestMapping("/mes/hc/plan/production-record")
@Validated
public class HcCutRoundProductionRecordController {

    @Resource
    private HcCutRoundConsoleService cutRoundConsoleService;

    @GetMapping("/page")
    @Operation(summary = "获得裁切生产记录分页")
    public CommonResult<PageResult<HcCutRoundProductionRecordRespVO>> getProductionRecordPage(
            HcCutRoundProductionRecordPageReqVO pageReqVO) {
        pageReqVO.setStatus(null);
        return success(cutRoundConsoleService.getProductionRecordPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出裁切生产记录表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportProductionRecordExcel(@Valid HcCutRoundProductionRecordPageReqVO pageReqVO,
                                             HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        pageReqVO.setStatus(null);
        ExcelUtils.write(response, "裁切生产记录表.xlsx", "裁切生产记录表",
                HcCutRoundProductionRecordExcelVO.class,
                cutRoundConsoleService.getProductionRecordList(pageReqVO).stream().map(this::toExcel).toList());
    }

    private HcCutRoundProductionRecordExcelVO toExcel(HcCutRoundProductionRecordRespVO source) {
        HcCutRoundProductionRecordExcelVO target = new HcCutRoundProductionRecordExcelVO();
        target.setReportDate(source.getRecordTime() != null
                ? source.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : source.getReportDate() == null ? "未记录具体时间"
                : source.getReportDate() + "（未记录具体时间）");
        target.setModelCode(source.getModelCode());
        target.setPadType(source.getPadType());
        target.setProductionBatchNo(source.getProductionBatchNo());
        target.setCutSizeMm(source.getCutSizeMm());
        target.setInputQty(source.getInputQty());
        target.setOutputQty(source.getOutputQty());
        target.setBladeModel(source.getBladeModel());
        target.setBladeBatchNo(source.getBladeBatchNo());
        target.setBladeUseCount(source.getBladeUseCount());
        target.setFeltModel(source.getFeltModel());
        target.setFeltBatchNo(source.getFeltBatchNo());
        target.setFeltUseCount(source.getFeltUseCount());
        target.setFeltUseDays(source.getFeltUseDays());
        target.setBladeReplaceReason(source.getBladeReplaceReason());
        target.setRecorderName(source.getRecorderName());
        target.setRecordSource(source.getRecordSource());
        target.setRecordTime(source.getRecordTime() == null ? null
                : source.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        target.setRemark(source.getRemark());
        return target;
    }
}
