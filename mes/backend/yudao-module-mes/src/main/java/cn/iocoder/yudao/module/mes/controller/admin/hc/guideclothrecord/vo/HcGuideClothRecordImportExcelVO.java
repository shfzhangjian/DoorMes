package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class HcGuideClothRecordImportExcelVO {

    @ExcelProperty("产线名")
    private String lineName;

    @ExcelProperty("产线编号")
    private String lineCode;

    @ExcelProperty("上次更换时间")
    private String replaceTime;

    @ExcelProperty("上次更换计划号")
    private String replacePlanNo;

    @ExcelProperty("上次PET批号")
    private String petBatchNo;

    @ExcelProperty("上次导布批号")
    private String guideClothBatchNo;

    @ExcelProperty("上次PET型号")
    private String petModel;

    @ExcelProperty("上次更换原因")
    private String replaceReason;

    @ExcelProperty("累计使用次数")
    private Integer useCount;

    @ExcelProperty("当前标记")
    private Integer currentFlag;

    @ExcelProperty("当前标记名称")
    private String currentFlagName;

    @ExcelProperty("备注")
    private String remark;
}
