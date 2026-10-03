package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

@ExcelIgnoreUnannotated
@Data
public class ResourceDeviceMaintPlanImportExcelVO {

    @ExcelProperty("计划年份")
    private Integer planYear;

    @ExcelProperty("设备编号")
    private String deviceCode;

    @ExcelProperty("设备名称")
    private String deviceName;

    @ExcelProperty("月份")
    private Integer monthNo;

    @ExcelProperty("周次")
    private Integer weekNo;

    @ExcelProperty("保养项目")
    private String itemGroup;

    @ExcelProperty("保养部位")
    private String itemName;

    @ExcelProperty("保养方法")
    private String method;

    @ExcelProperty("保养标准")
    private String requirement;

    @ExcelProperty("保养周期")
    private String frequency;

    @ExcelProperty("保养类型")
    private String maintType;

    @ExcelProperty("来源依据")
    private String sourceRule;

    @ExcelProperty("备注")
    private String remark;

}
