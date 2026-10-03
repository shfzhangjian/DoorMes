package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 设备异常台账 Response VO")
@Data
public class ResourceDeviceExceptionRespVO {

    private Long id;
    @ExcelProperty("异常单号")
    private String exceptionNo;
    private String orderNo;
    private Long deviceId;
    @ExcelProperty("设备编号")
    private String deviceCode;
    @ExcelProperty("设备名称")
    private String deviceName;
    @ExcelProperty("异常等级")
    private String exceptionLevel;
    @ExcelProperty("异常现象")
    private String faultDesc;
    private Long reporterId;
    @ExcelProperty("提报人")
    private String reporter;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ExcelProperty("提报时间")
    private LocalDateTime reportTime;
    private Long dispatcherId;
    private String dispatcher;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime dispatchTime;
    private String responseResult;
    private String responseRemark;
    private Long assigneeId;
    @ExcelProperty("维修人")
    private String assignee;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime repairPlanTime;
    private String repairPlanRemark;
    @ExcelProperty("故障原因")
    private String faultReason;
    @ExcelProperty("维修措施")
    private String repairAction;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime repairTime;
    private String repairStatus;
    private String partChangeDesc;
    private String confirmResult;
    private String confirmRemark;
    private String unfixReason;
    private Long confirmerId;
    private String confirmer;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;
    private Long archiverId;
    private String archiver;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime archiveTime;
    private String archiveReason;
    private String faultCategory;
    private String faultSubCategory;
    private String impactScope;
    private String rootCause;
    private String preventiveAction;
    private String attachments;
    @ExcelProperty("状态")
    private String status;
    private String remark;
    private Integer version;
    private String tabType;
    private String processInstanceId;
    private String currentNodeCode;
    private String currentNodeName;
    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private Boolean currentUserTaskTodo;
    private Integer currentUserTaskTodoCount;
    private String currentUserTaskTodoType;
    private String currentUserTaskTodoLabel;
    private Boolean currentUserTaskDone;
    private Integer currentUserTaskDoneCount;
    private String currentUserTaskDoneType;
    private String currentUserTaskDoneLabel;
    private Boolean canHandle;
    private String listActionCode;
    private String listActionName;
    private List<RepairPart> parts;
    private List<FlowLog> flowLogs;

    @Data
    public static class RepairPart {
        private Long id;
        private Long exceptionId;
        private String partCode;
        private String partName;
        private BigDecimal quantity;
        private String unit;
        private String remark;
        private Integer sort;
    }

    @Data
    public static class FlowLog {
        private String actionCode;
        private String actionName;
        private String fromStatus;
        private String toStatus;
        private String fromNodeCode;
        private String fromNodeName;
        private String toNodeCode;
        private String toNodeName;
        private String opinion;
        private Long handlerUserId;
        private String handlerUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime handleTime;
    }

}
