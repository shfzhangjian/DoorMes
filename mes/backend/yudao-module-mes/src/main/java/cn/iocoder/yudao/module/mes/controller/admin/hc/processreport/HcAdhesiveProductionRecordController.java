package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive1ProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2ProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcAdhesiveProductionRecordService;
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

@Tag(name = "管理后台 - 粘胶生产记录表")
@RestController
@RequestMapping("/mes/hc/plan/adhesive-production-record")
@Validated
public class HcAdhesiveProductionRecordController {

    @Resource
    private HcAdhesiveProductionRecordService productionRecordService;

    @GetMapping("/adhesive1/page")
    @Operation(summary = "获得粘胶1生产记录表分页")
    public CommonResult<PageResult<HcAdhesiveProductionRecordRespVO>> getAdhesive1Page(
            @Valid HcAdhesiveProductionRecordPageReqVO reqVO) {
        return success(productionRecordService.getAdhesive1Page(reqVO));
    }

    @GetMapping("/adhesive1/export-excel")
    @ApiAccessLog(operateType = EXPORT)
    @Operation(summary = "导出粘胶1生产记录表")
    public void exportAdhesive1(@Valid HcAdhesiveProductionRecordPageReqVO reqVO,
                                HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "粘胶1生产记录表.xlsx", "粘胶1生产记录表",
                HcAdhesive1ProductionRecordExcelVO.class,
                productionRecordService.getAdhesive1List(reqVO).stream().map(this::toAdhesive1Excel).toList());
    }

    @GetMapping("/adhesive2/page")
    @Operation(summary = "获得粘胶2生产记录表分页")
    public CommonResult<PageResult<HcAdhesiveProductionRecordRespVO>> getAdhesive2Page(
            @Valid HcAdhesiveProductionRecordPageReqVO reqVO) {
        return success(productionRecordService.getAdhesive2Page(reqVO));
    }

    @GetMapping("/adhesive2/export-excel")
    @ApiAccessLog(operateType = EXPORT)
    @Operation(summary = "导出粘胶2生产记录表")
    public void exportAdhesive2(@Valid HcAdhesiveProductionRecordPageReqVO reqVO,
                                HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "粘胶2生产记录表.xlsx", "粘胶2生产记录表",
                HcAdhesive2ProductionRecordExcelVO.class,
                productionRecordService.getAdhesive2List(reqVO).stream().map(this::toAdhesive2Excel).toList());
    }

    private HcAdhesive1ProductionRecordExcelVO toAdhesive1Excel(HcAdhesiveProductionRecordRespVO source) {
        HcAdhesive1ProductionRecordExcelVO target = new HcAdhesive1ProductionRecordExcelVO();
        copyExcelFields(source, target);
        return target;
    }

    private HcAdhesive2ProductionRecordExcelVO toAdhesive2Excel(HcAdhesiveProductionRecordRespVO source) {
        HcAdhesive2ProductionRecordExcelVO target = new HcAdhesive2ProductionRecordExcelVO();
        target.setReportDate(formatProductionDate(source));
        target.setModelCode(source.getModelCode());
        target.setPadType(source.getPadType());
        target.setMaterialCode(source.getMaterialCode());
        target.setBatchNo(source.getBatchNo());
        target.setInputQty(source.getInputQty());
        target.setOutputQty(source.getOutputQty());
        target.setGlueBoardMaterialCode(source.getGlueBoardMaterialCode());
        target.setGlueBoardBatchNo(source.getGlueBoardBatchNo());
        target.setGlueBoardConsumeQty(source.getGlueBoardConsumeQty());
        target.setRecordSource(source.getRecordSource());
        target.setRecorderName(source.getRecorderName());
        target.setRemark(source.getRemark());
        return target;
    }

    private void copyExcelFields(HcAdhesiveProductionRecordRespVO source,
                                 HcAdhesive1ProductionRecordExcelVO target) {
        target.setReportDate(formatProductionDate(source));
        target.setModelCode(source.getModelCode());
        target.setPadType(source.getPadType());
        target.setMaterialCode(source.getMaterialCode());
        target.setBatchNo(source.getBatchNo());
        target.setInputQty(source.getInputQty());
        target.setOutputQty(source.getOutputQty());
        target.setGlueBoardMaterialCode(source.getGlueBoardMaterialCode());
        target.setGlueBoardBatchNo(source.getGlueBoardBatchNo());
        target.setGlueBoardConsumeQty(source.getGlueBoardConsumeQty());
        target.setRecordSource(source.getRecordSource());
        target.setRecorderName(source.getRecorderName());
        target.setRemark(source.getRemark());
    }

    private String formatProductionDate(HcAdhesiveProductionRecordRespVO source) {
        if (source.getRecordTime() != null) {
            return source.getRecordTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        return source.getReportDate() == null ? "未记录具体时间"
                : source.getReportDate() + "（未记录具体时间）";
    }

}
