package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HcCutRoundProductionRecordExcelVO {

    @ExcelProperty("日期")
    private String reportDate;

    @ExcelProperty("型号")
    private String modelCode;

    @ExcelProperty("类型")
    private String padType;

    @ExcelProperty("生产批号")
    private String productionBatchNo;

    @ExcelProperty("裁切尺寸(mm)")
    private String cutSizeMm;

    @ExcelProperty("投入(pcs)")
    private BigDecimal inputQty;

    @ExcelProperty("产出(pcs)")
    private BigDecimal outputQty;

    @ExcelProperty("刀片型号")
    private String bladeModel;

    @ExcelProperty("刀片批号")
    private String bladeBatchNo;

    @ExcelProperty("刀片累计裁切(pcs)")
    private Integer bladeUseCount;

    @ExcelProperty("毛毡型号")
    private String feltModel;

    @ExcelProperty("毛毡批号")
    private String feltBatchNo;

    @ExcelProperty("裁切片数累计(<=2000pcs)(pcs)")
    private Integer feltUseCount;

    @ExcelProperty("毛毡累计使用天数(<=3个月/90天)")
    private Integer feltUseDays;

    @ExcelProperty("刀片更换原因")
    private String bladeReplaceReason;

    @ExcelProperty("记录人")
    private String recorderName;

    @ExcelProperty("数据来源")
    private String recordSource;

    @ExcelProperty("记录时间")
    private String recordTime;

    @ExcelIgnore
    private String confirmerName;

    @ExcelIgnore
    private String confirmTime;

    @ExcelProperty("备注")
    private String remark;

}
