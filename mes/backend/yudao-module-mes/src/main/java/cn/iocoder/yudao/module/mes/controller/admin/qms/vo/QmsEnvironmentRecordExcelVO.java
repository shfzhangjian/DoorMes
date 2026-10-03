package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

@Data
@ExcelIgnoreUnannotated
public class QmsEnvironmentRecordExcelVO {

    @ExcelProperty(value = "日期", index = 0)
    private String recordDate;

    @ExcelProperty(value = "日", index = 1)
    private Integer day;

    @ExcelProperty(value = "车间编码", index = 2)
    private String workshopCode;

    @ExcelProperty(value = "车间", index = 3)
    private String workshopName;

    @ExcelProperty(value = "温度℃", index = 4)
    private BigDecimal temperatureValue;

    @ExcelProperty(value = "湿度%RH", index = 5)
    private BigDecimal humidityValue;

    @ExcelProperty(value = "温度判定", index = 6)
    private String temperatureStatus;

    @ExcelProperty(value = "湿度判定", index = 7)
    private String humidityStatus;

    @ExcelProperty(value = "综合判定", index = 8)
    private String overallStatus;

    @ExcelProperty(value = "确认状态", index = 9)
    private String recordStatus;

    @ExcelProperty(value = "记录人", index = 10)
    private String recorderName;

    @ExcelProperty(value = "记录时间", index = 11)
    private String recordTime;

    @ExcelProperty(value = "确认人", index = 12)
    private String confirmerName;

    @ExcelProperty(value = "确认时间", index = 13)
    private String confirmTime;

    @ExcelProperty(value = "备注", index = 14)
    private String remark;
}
