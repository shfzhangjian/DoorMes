package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;

@Schema(description = "管理后台 - 设备年度保养计划 Response VO")
@Data
public class ResourceDeviceMaintPlanRespVO {

    private Long id;
    @ExcelProperty("计划年份")
    private Integer planYear;
    @ExcelProperty("计划周期")
    private String planPeriod;
    @ExcelProperty("月份")
    private Integer monthNo;
    @ExcelProperty("周次")
    private Integer weekNo;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("计划周开始")
    private LocalDate planStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("计划周结束")
    private LocalDate planEndDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("提醒日期")
    private LocalDate planDate;
    private Long deviceId;
    @ExcelProperty("设备编号")
    private String deviceCode;
    @ExcelProperty("设备名称")
    private String deviceName;
    private Long categoryId;
    @ExcelProperty("设备分类")
    private String categoryName;
    private Long standardId;
    private String standardCode;
    @ExcelProperty("应用标准")
    private String standardName;
    private Long standardItemId;
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
    @ExcelProperty("是否已发布")
    private Boolean published;
    private Long generatedOrderId;
    @ExcelProperty("生成工单")
    private String generatedTaskNo;
    @ExcelProperty("执行状态")
    private String executeStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("完成时间")
    private LocalDateTime actualDate;
    @ExcelProperty("维保人")
    private String executor;
    @ExcelProperty("完成情况")
    private String executeRemark;
    @ExcelProperty("确认人")
    private String confirmer;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("确认时间")
    private LocalDateTime confirmTime;
    @ExcelProperty("状态")
    private String status;
    @ExcelProperty("备注")
    private String remark;
    private Integer version;

    @Schema(description = "年度计划矩阵")
    @Data
    public static class Matrix {
        private List<MatrixRow> list;
        private Long total;
    }

    @Schema(description = "年度计划矩阵行")
    @Data
    public static class MatrixRow {
        private Long id;
        private Integer planYear;
        private Long deviceId;
        private String deviceCode;
        private String deviceName;
        private String categoryName;
        private Long standardId;
        private String standardCode;
        private String standardName;
        private String lastMaintDate;
        private List<MatrixItem> items;
    }

    @Schema(description = "年度计划矩阵明细行")
    @Data
    public static class MatrixItem {
        private Long id;
        private Long standardItemId;
        private String itemGroup;
        private String itemName;
        private String method;
        private String requirement;
        private String frequency;
        private String maintType;
        private Integer sort;
        private Map<Integer, CellItem> months;
    }

    @Schema(description = "年度计划矩阵月单元格")
    @Data
    public static class CellItem {
        private Long id;
        private Integer monthNo;
        private Integer weekNo;
        private String weekLabel;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate planStartDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate planEndDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate planDate;
        private Long generatedOrderId;
        private String generatedTaskNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime actualDate;
        private String executor;
        private String executeRemark;
        private String confirmer;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime confirmTime;
        private String itemGroup;
        private String itemName;
        private String maintType;
        private String frequency;
        private Boolean published;
        private String status;
        private String orderStatus;
        private String executeStatus;
        private String cellStatus;
        private Boolean executed;
        private Boolean overdue;
    }

}
