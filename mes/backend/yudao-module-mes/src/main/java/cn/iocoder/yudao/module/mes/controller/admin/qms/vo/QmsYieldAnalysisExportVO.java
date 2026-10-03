package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class QmsYieldAnalysisExportVO {

    @ExcelProperty("产品型号")
    private String modelCode;

    @ExcelProperty("母卷批号")
    private String motherRollNo;

    @ExcelProperty("分段")
    private String segmentNo;

    @ExcelProperty("工序")
    private String processName;

    @ExcelProperty("投入数")
    private BigDecimal inputTotal;

    @ExcelProperty("产出良品数")
    private BigDecimal outputGoodTotal;

    @ExcelProperty("目标合格")
    private BigDecimal targetQualifiedQty;

    @ExcelProperty("计量单位")
    private String targetUnit;

    @ExcelProperty("目标类型")
    private String targetTypeName;

    @ExcelProperty("目标达成率")
    private BigDecimal targetAchievementRate;

    @ExcelProperty("目标差异")
    private BigDecimal targetDifference;

    @ExcelProperty("达标结果")
    private String targetResult;

    @ExcelProperty("产出不良品数")
    private BigDecimal outputNgTotal;

    @ExcelProperty("黑点")
    private Integer blackDotCount;

    @ExcelProperty("蓝点")
    private Integer blueDotCount;

    @ExcelProperty("黄点")
    private Integer yellowDotCount;

    @ExcelProperty("红点")
    private Integer redDotCount;

    @ExcelProperty("针孔")
    private Integer pinholeCount;

    @ExcelProperty("条纹")
    private Integer stripeCount;

    @ExcelProperty("褶皱")
    private Integer wrinkleCount;

    @ExcelProperty("波浪纹")
    private Integer waveCount;

    @ExcelProperty("其他")
    private Integer otherCount;

    @ExcelProperty("送检次数")
    private Integer inspectionTotal;

    @ExcelProperty("良品率")
    private BigDecimal yieldRate;
}
