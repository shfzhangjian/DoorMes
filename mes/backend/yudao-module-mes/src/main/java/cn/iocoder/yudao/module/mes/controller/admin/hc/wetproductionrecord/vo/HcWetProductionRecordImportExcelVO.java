package cn.iocoder.yudao.module.mes.controller.admin.hc.wetproductionrecord.vo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.ExcelIgnore;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class HcWetProductionRecordImportExcelVO {

    @ExcelProperty("数据来源")
    private String dataSource;

    @ExcelProperty("日期")
    private String recordDate;

    @ExcelProperty("型号")
    private String modelCode;

    @ExcelProperty("类型")
    private String padType;

    @ExcelProperty("料号")
    private String materialCode;

    @ExcelProperty("批次")
    private String batchNo;

    @ExcelProperty("投入(kg)")
    private BigDecimal inputKg;

    @ExcelProperty("产出(m)")
    private BigDecimal outputMeter;

    @ExcelProperty("PET型号")
    private String petModel;

    @ExcelProperty("PET批号")
    private String petBatchNo;

    @ExcelProperty("导布批号")
    private String guideClothBatchNo;

    @ExcelProperty("导布累计使用次数")
    private Integer guideClothUseCount;

    @ExcelProperty("导布更换（是否）")
    private String guideClothChangedName;

    @ExcelProperty("更换说明")
    private String changeDesc;

    @ExcelProperty("记录人")
    private String recorderName;

    @ExcelProperty("记录时间")
    private String recordTime;

    @ExcelIgnore
    private String confirmerName;

    @ExcelIgnore
    private String confirmTime;

    @ExcelProperty("备注")
    private String remark;
}
