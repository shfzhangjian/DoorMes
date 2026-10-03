package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 设备保养执行记录 Response VO")
@Data
public class ResourceDeviceMaintRecordRespVO {

    private Long id;
    private String recordNo;
    private Long orderId;
    @ExcelProperty("任务单号")
    private String taskNo;
    private Long deviceId;
    @ExcelProperty("设备编号")
    private String deviceCode;
    @ExcelProperty("设备名称")
    private String deviceName;
    private Long categoryId;
    @ExcelProperty("设备分类")
    private String categoryName;
    private Long standardId;
    @ExcelProperty("应用标准")
    private String standardName;
    @ExcelProperty("维保类型")
    private String maintType;
    private String frequency;
    @ExcelProperty("执行人")
    private String executor;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("应检日期")
    private LocalDate dueDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime planTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("实际完成时间")
    private LocalDateTime actualTime;
    @ExcelProperty("确认人")
    private String confirmer;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("确认时间")
    private LocalDateTime confirmTime;
    @ExcelProperty("保养附件")
    private String photos;
    @ExcelProperty("结果")
    private String resultStatus;
    private Long exceptionId;
    private String exceptionNo;
    private String remark;

    @Schema(description = "设备保养月度汇总")
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlySummary {

        @Schema(description = "月份 yyyy-MM")
        private String month;

        @Schema(description = "设备分类ID")
        private Long categoryId;

        @Schema(description = "设备分类名称")
        private String categoryName;

        @Schema(description = "保养类型")
        private String maintType;

        @Schema(description = "应保养任务数")
        private Long taskCount;

        @Schema(description = "已完成记录数")
        private Long recordCount;

        @Schema(description = "正常完成数")
        private Long normalCount;

        @Schema(description = "异常完成数")
        private Long abnormalCount;

        @Schema(description = "逾期完成数")
        private Long overdueCompletedCount;

        @Schema(description = "完成率")
        private BigDecimal completionRate;

    }

}
