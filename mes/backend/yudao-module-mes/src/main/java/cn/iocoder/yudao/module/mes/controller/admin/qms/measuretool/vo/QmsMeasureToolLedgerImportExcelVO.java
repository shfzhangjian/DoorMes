package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import java.time.LocalDate;
import lombok.Data;

/** 客户量检具台账导入行，列名与客户台账字段一致。 */
@Data
@ExcelIgnoreUnannotated
public class QmsMeasureToolLedgerImportExcelVO {
    @ExcelProperty("机身号") private String bodyNo;
    @ExcelProperty("本厂编号") private String toolCode;
    @ExcelProperty("设备名称") private String toolName;
    @ExcelProperty("规格型号") private String model;
    /** 兼容旧导入模板。 */
    @ExcelProperty("类别") private String categoryName;
    @ExcelProperty("区域") private String areaName;
    @ExcelProperty("厂家名称") private String manufacturer;
    @ExcelProperty("购入日期") @DateTimeFormat("yyyy-MM-dd") private LocalDate purchaseDate;
    @ExcelProperty("校准周期") private Integer calibrationCycleMonths;
    @ExcelProperty("校准类型") private String calibrationType;
    @ExcelProperty("上次校准日期") @DateTimeFormat("yyyy-MM-dd") private LocalDate lastCalibrationDate;
    @ExcelProperty("下次校准日期") @DateTimeFormat("yyyy-MM-dd") private LocalDate nextCalibrationDate;
    @ExcelProperty("保养人") private String maintainerName;
    /** 兼容旧导入模板。 */
    @ExcelProperty("所在位置") private String storageLocation;
    @ExcelProperty("位置") private String positionName;
    @ExcelProperty("设备状态") private String status;
    @ExcelProperty("校准结果") private String calibrationResult;
    @ExcelProperty("校准报告") private String calibrationReport;
    @ExcelProperty("MSA") private String msaEnabled;
    @ExcelProperty("上次分析日期") @DateTimeFormat("yyyy-MM-dd") private LocalDate lastMsaDate;
    @ExcelProperty("下次分析日期") @DateTimeFormat("yyyy-MM-dd") private LocalDate nextMsaDate;
    @ExcelProperty("分析结果") private String msaResult;
    @ExcelProperty("MSA分析报告") private String msaReport;
    @ExcelProperty("责任人") private String responsiblePerson;
    @ExcelProperty("备注") private String remark;
}
