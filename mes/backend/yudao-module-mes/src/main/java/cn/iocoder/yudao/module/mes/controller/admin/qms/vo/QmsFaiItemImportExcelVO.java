package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@ExcelIgnoreUnannotated
@Data
public class QmsFaiItemImportExcelVO {

    @ExcelProperty("FAI ID")
    private Long faiId;

    @ExcelProperty("FAI单号")
    private String faiNo;

    @ExcelProperty("模板校验码")
    private String templateVersionHash;

    @ExcelProperty("检验项ID")
    private Long faiItemId;

    @ExcelProperty("样本序号")
    private Integer sampleSeq;

    @ExcelProperty("检验项编码")
    private String inspectionItemCode;

    @ExcelProperty("检验项目")
    private String inspectionItem;

    @ExcelProperty("项目类型")
    private String itemType;

    @ExcelProperty("录入模板")
    private String valueTemplate;

    @ExcelProperty("位置编码")
    private String positionCode;

    @ExcelProperty("位置")
    private String positionName;

    @ExcelProperty("组次")
    private Integer repeatSeq;

    @ExcelProperty("目标值")
    private BigDecimal targetValue;

    @ExcelProperty("平均值内控")
    private String avgLimitText;

    @ExcelProperty("标准差内控")
    private String stdLimitText;

    @ExcelProperty("实测值")
    private BigDecimal measuredValue;

    @ExcelProperty("厚度")
    private BigDecimal thicknessMm;

    @ExcelProperty("重量")
    private BigDecimal weightG;

    @ExcelProperty("T1")
    private BigDecimal t1Mm;

    @ExcelProperty("T2")
    private BigDecimal t2Mm;

    @ExcelProperty("T3")
    private BigDecimal t3Mm;

    @ExcelProperty("定性判定")
    private String qualitativeValue;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("样本判定")
    private String sampleResult;

    @ExcelProperty("平均值")
    private BigDecimal calculatedAvg;

    @ExcelProperty("标准差")
    private BigDecimal calculatedStd;

    @ExcelProperty("值来源")
    private String valueSource;

    @ExcelProperty("导入批次")
    private String importBatchNo;
}
