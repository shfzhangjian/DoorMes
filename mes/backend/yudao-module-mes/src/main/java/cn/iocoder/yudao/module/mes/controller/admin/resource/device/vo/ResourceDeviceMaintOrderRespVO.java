package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备保养工单 Response VO")
@Data
public class ResourceDeviceMaintOrderRespVO {

    private Long id;
    @ExcelProperty("任务单号")
    private String taskNo;
    private String planPeriod;
    private Long deviceId;
    @ExcelProperty("设备编号")
    private String deviceCode;
    @ExcelProperty("设备名称")
    private String deviceName;
    private Long categoryId;
    private String categoryName;
    private Long standardId;
    private String standardCode;
    @ExcelProperty("应用标准")
    private String standardName;
    private String frequency;
    @ExcelProperty("维保类型")
    private String maintType;
    private String taskDesc;
    @ExcelProperty("执行人")
    private String executor;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate planDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty("应检日期")
    private LocalDate dueDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("计划时间")
    private LocalDateTime planTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("实际完成时间")
    private LocalDateTime actualDate;
    private String executeRemark;
    @ExcelProperty("确认人")
    private String confirmer;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("确认时间")
    private LocalDateTime confirmTime;
    private String photos;
    @ExcelProperty("状态")
    private String status;
    private Long exceptionId;
    private String exceptionNo;
    private String remark;
    private Integer version;
    private String tabType;
    private List<OrderItem> items;
    private List<OrderPart> parts;

    @Data
    public static class OrderItem {
        private Long id;
        private Long taskId;
        private Long standardItemId;
        private String itemName;
        private String method;
        private String requirement;
        private String result;
        private String remark;
        private Integer sort;
    }

    @Data
    public static class OrderPart {
        private Long id;
        private Long taskId;
        private String partCode;
        private String partName;
        private BigDecimal quantity;
        private String unit;
        private String remark;
        private Integer sort;
    }

}
