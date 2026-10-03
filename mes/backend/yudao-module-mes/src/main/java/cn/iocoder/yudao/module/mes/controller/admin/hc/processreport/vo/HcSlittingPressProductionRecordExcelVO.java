package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class HcSlittingPressProductionRecordExcelVO {

    @ExcelProperty({"基础信息", "日期"})
    private String reportDate;

    @ExcelProperty({"基础信息", "型号"})
    private String modelCode;

    @ExcelProperty({"基础信息", "类型"})
    private String padType;

    @ExcelProperty({"基础信息", "料号"})
    private String materialCode;

    @ExcelProperty({"基础信息", "批号"})
    private String batchNo;

    @ExcelProperty({"分切", "分切投入（m）"})
    private BigDecimal slittingInputM;

    @ExcelProperty({"分切", "确认产出（pcs）"})
    private Integer slittingOutputPcs;

    @ExcelProperty({"分切", "NG（pcs）"})
    private Integer slittingNgPcs;

    @ExcelProperty({"压槽", "实际报工投入（pcs）"})
    private BigDecimal pressSlotActualInputPcs;

    @ExcelProperty({"压槽", "实际报工产出（pcs）"})
    private BigDecimal pressSlotActualOutputPcs;

    @ExcelProperty({"压槽", "合格产出（pcs）"})
    private BigDecimal pressSlotOutputPcs;

    @ExcelProperty({"压槽辊清洗", "累计片数（pcs）<2000pcs"})
    private Integer rollerCleanAccumulatedPcs;

    @ExcelProperty({"压槽辊清洗", "累计使用时长≤60天"})
    private Integer rollerCleanUseDays;

    @ExcelProperty({"轴承更换", "累计片数（pcs）<5000pcs"})
    private Integer bearingReplaceAccumulatedPcs;

    @ExcelProperty({"轴承更换", "累计使用时长≤150天"})
    private Integer bearingReplaceUseDays;

    @ExcelProperty({"记录", "记录人"})
    private String recorderName;

    @ExcelProperty({"记录", "数据来源"})
    private String recordSource;

    @ExcelProperty({"记录", "记录时间"})
    private String recordTime;

    @ExcelIgnore
    private String confirmerName;

    @ExcelIgnore
    private String confirmTime;

    @ExcelProperty({"记录", "备注"})
    private String remark;

}
