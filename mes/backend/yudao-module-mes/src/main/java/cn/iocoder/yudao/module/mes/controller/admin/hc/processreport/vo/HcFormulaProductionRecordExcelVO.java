package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HcFormulaProductionRecordExcelVO {

    @ExcelProperty("日期")
    private String reportDate;

    @ExcelProperty("型号")
    private String modelCode;

    @ExcelProperty("类型")
    private String padType;

    @ExcelProperty("料号")
    private String materialCode;

    @ExcelProperty("批号")
    private String batchNo;

    @ExcelProperty("滤网批号")
    private String filterBatchNo;

    @ExcelProperty("投料重量（kg）")
    private BigDecimal inputWeight;

    @ExcelProperty("产出重量（kg）")
    private BigDecimal outputWeight;

    @ExcelProperty("搅拌机机台编号")
    private String mixerEquipmentCode;

    @ExcelProperty("配料罐罐号")
    private String batchingTankNo;

    @ExcelProperty("脱泡机机台编号")
    private String foamingEquipmentCode;

    @ExcelProperty("脱泡罐罐号")
    private String defoamingTankNo;

    @ExcelProperty("记录人")
    private String recorderName;

    @ExcelProperty("备注")
    private String remark;

}
