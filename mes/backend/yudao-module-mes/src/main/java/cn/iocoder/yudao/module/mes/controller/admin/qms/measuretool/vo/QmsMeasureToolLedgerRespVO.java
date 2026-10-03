package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 量检具台账 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsMeasureToolLedgerRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @ExcelProperty(value = "本厂编号", index = 1)
    private String toolCode;

    @ExcelProperty(value = "机身号", index = 0)
    private String bodyNo;

    @ExcelProperty(value = "设备名称", index = 2)
    private String toolName;

    private Long categoryId;

    @ExcelProperty(value = "区域", index = 14)
    private String categoryName;

    @ExcelProperty(value = "规格型号", index = 3)
    private String model;

    /** 兼容旧接口字段，导出时按“规格型号”主字段输出。 */
    private String specification;

    /** 精度为扩展台账字段。 */
    private String accuracy;

    /** 量程为扩展台账字段。 */
    private String measureRange;

    @ExcelProperty(value = "厂家名称", index = 5)
    private String manufacturer;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty(value = "购入日期", index = 6)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate purchaseDate;

    @ExcelProperty(value = "校准周期", index = 7)
    private Integer calibrationCycleMonths;

    /** 预警提前天数是系统配置字段，不单独作为客户模板导出列。 */
    private Integer warningDays;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty(value = "上次校准日期", index = 9)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate lastCalibrationDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty(value = "下次校准日期", index = 10)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate nextCalibrationDate;

    /** 使用部门保留为系统查询字段。 */
    private String usingDepartment;

    /** 兼容旧校准任务、历史字段。 */
    private String keeperName;

    /** 原始设备状态，用于页面显示和筛选。 */
    private String status;

    @ExcelProperty(value = "校准提醒", index = 11)
    private String calibrationStatus;

    @ExcelProperty(value = "校准逾期", index = 26)
    private Boolean calibrationOverdue;

    @ExcelProperty(value = "校准类型", index = 8)
    private String calibrationType;
    private String calibrationMethod;
    private String calibrationOrg;
    private String calibrator;
    @ExcelProperty(value = "校准结果", index = 16)
    private String calibrationResult;
    private String certificateNo;
    @ExcelProperty(value = "校准报告", index = 17)
    private String calibrationReport;

    @ExcelProperty(value = "保养人", index = 12)
    private String maintainerName;
    @ExcelProperty(value = "位置", index = 13)
    private String storageLocation;
    @ExcelProperty(value = "设备状态", index = 15)
    private String displayStatus;

    @ExcelProperty(value = "MSA", index = 18)
    private Integer msaEnabled;
    private Integer msaCycleMonths;
    private Integer msaWarningDays;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty(value = "上次分析日期", index = 19)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate lastMsaDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty(value = "下次分析日期", index = 20)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate nextMsaDate;
    @ExcelProperty(value = "到期提醒", index = 21)
    private String msaStatus;
    @ExcelProperty(value = "分析结果", index = 22)
    private String msaResult;
    @ExcelProperty(value = "MSA分析报告", index = 23)
    private String msaReport;
    private String msaAnalyst;
    @ExcelProperty(value = "责任人", index = 24)
    private String responsiblePerson;

    /** 仅内部计量管理员返回真实值，避免普通用户通过接口感知内部台账标识。 */
    private Integer externalOpen;

    private Integer calibrationMissedCount;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recentCalibrationMissedDate;
    private Integer msaMissedCount;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recentMsaMissedDate;

    @ExcelProperty(value = "备注", index = 25)
    private String remark;
    private Integer version;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

}
