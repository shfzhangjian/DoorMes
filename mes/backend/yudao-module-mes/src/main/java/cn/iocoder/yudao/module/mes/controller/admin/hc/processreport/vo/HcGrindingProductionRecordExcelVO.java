package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HcGrindingProductionRecordExcelVO {
    @ExcelProperty("砂纸本次消耗量")
    private java.math.BigDecimal sandpaperConsumeQty;
    @ExcelProperty("砂纸消耗单位")
    private String sandpaperConsumeUnit;
    @ExcelProperty("导布本次消耗量")
    private java.math.BigDecimal guideClothConsumeQty;
    @ExcelProperty("导布消耗单位")
    private String guideClothConsumeUnit;


    @ExcelProperty("完工日期")
    private String completionTime;

    @ExcelProperty("型号")
    private String modelCode;

    @ExcelProperty("类型")
    private String padType;

    @ExcelProperty("料号")
    private String materialCode;

    @ExcelProperty("加工母批号")
    private String motherBatchNo;

    @ExcelProperty("批号")
    private String batchNo;

    @ExcelProperty("记录角色")
    private String recordRole;

    @ExcelProperty("来源业务类型")
    private String sourceBizType;

    @ExcelProperty("加工单元")
    private String segmentMark;

    @ExcelProperty("起米位置(m)")
    private BigDecimal startPosition;

    @ExcelProperty("一磨分配记录ID")
    private Long firstAllocationId;

    @ExcelProperty("投入米数(m)")
    private BigDecimal inputLength;

    @ExcelProperty("产出米数(m)")
    private BigDecimal outputLength;

    @ExcelProperty("磨皮次数")
    private String passName;

    @ExcelProperty("砂纸累计寿命(m)")
    private BigDecimal sandpaperLife;

    @ExcelProperty("砂纸累计天数")
    private Integer sandpaperLifeDays;

    @ExcelProperty("砂纸批号")
    private String sandpaperBatchNo;

    @ExcelProperty("砂纸记录序号")
    private Integer sourceSegmentNo;

    @ExcelProperty("导布累计寿命(次)")
    private Integer guideClothLife;

    @ExcelProperty("导布批号")
    private String guideClothBatchNo;

    @ExcelProperty("更换原因")
    private String replaceReason;

    @ExcelProperty("记录人")
    private String recorderName;

    @ExcelProperty("确认人")
    private String confirmerName;

    @ExcelProperty("备注")
    private String remark;

}
