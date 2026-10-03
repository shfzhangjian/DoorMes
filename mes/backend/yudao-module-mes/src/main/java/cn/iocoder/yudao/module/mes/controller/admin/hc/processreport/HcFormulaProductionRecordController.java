package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcFormulaProductionRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 配料生产记录表")
@RestController
@RequestMapping("/mes/hc/plan/formula-production-record")
@Validated
public class HcFormulaProductionRecordController {

    @Resource
    private HcFormulaProductionRecordService hcFormulaProductionRecordService;

    @GetMapping("/page")
    @Operation(summary = "获得配料生产记录表分页")
    public CommonResult<PageResult<HcFormulaProductionRecordRespVO>> getPage(
            @Valid HcFormulaProductionRecordPageReqVO pageReqVO) {
        return success(hcFormulaProductionRecordService.getPage(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出配料生产记录表")
    @ApiAccessLog(operateType = EXPORT)
    public void exportExcel(@Valid HcFormulaProductionRecordPageReqVO pageReqVO,
                            HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "配料生产记录表.xlsx", "配料生产记录表",
                HcFormulaProductionRecordExcelVO.class,
                hcFormulaProductionRecordService.getList(pageReqVO).stream().map(this::toExcel).toList());
    }

    private HcFormulaProductionRecordExcelVO toExcel(HcFormulaProductionRecordRespVO source) {
        HcFormulaProductionRecordExcelVO target = new HcFormulaProductionRecordExcelVO();
        target.setReportDate(source.getReportDate() == null ? null : source.getReportDate().toString());
        target.setModelCode(source.getModelCode());
        target.setPadType(source.getPadType());
        target.setMaterialCode(source.getMaterialCode());
        target.setBatchNo(source.getBatchNo());
        target.setFilterBatchNo(source.getFilterBatchNo());
        target.setInputWeight(source.getInputWeight());
        target.setOutputWeight(source.getOutputWeight());
        target.setMixerEquipmentCode(source.getMixerEquipmentCode());
        target.setBatchingTankNo(source.getBatchingTankNo());
        target.setFoamingEquipmentCode(source.getFoamingEquipmentCode());
        target.setDefoamingTankNo(source.getDefoamingTankNo());
        target.setRecorderName(source.getRecorderName());
        target.setRemark(source.getRemark());
        return target;
    }

}
