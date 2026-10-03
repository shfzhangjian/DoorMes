package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HcAdhesive1ProductionRecordExcelVO {

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

    @ExcelProperty("投入米数(m)")
    private BigDecimal inputQty;

    @ExcelProperty("产出米数(m)")
    private BigDecimal outputQty;

    @ExcelProperty("胶板料号")
    private String glueBoardMaterialCode;

    @ExcelProperty("胶板批号")
    private String glueBoardBatchNo;

    @ExcelProperty("胶板消耗量(m)")
    private BigDecimal glueBoardConsumeQty;

    @ExcelProperty("数据来源")
    private String recordSource;

    @ExcelProperty("记录人")
    private String recorderName;

    @ExcelProperty("备注")
    private String remark;

}
