package cn.iocoder.yudao.module.mes.controller.admin.resource.device.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Data
public class ResourceDeviceExceptionSaveReqVO {

    public interface Update {
    }

    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    private String exceptionNo;
    private String orderNo;
    private Long deviceId;
    @NotBlank(message = "设备编号不能为空")
    private String deviceCode;
    @NotBlank(message = "设备名称不能为空")
    private String deviceName;
    private String exceptionLevel;
    @NotBlank(message = "异常现象不能为空")
    private String faultDesc;
    private Long reporterId;
    private String reporter;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime reportTime;

    private Long dispatcherId;
    private String dispatcher;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime dispatchTime;

    private String responseResult;
    private String responseRemark;
    private Long assigneeId;
    private String assignee;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime repairPlanTime;

    private String repairPlanRemark;
    private String faultReason;
    private String repairAction;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime repairTime;

    private String repairStatus;
    private String partChangeDesc;
    private String confirmResult;
    private String confirmRemark;
    private String unfixReason;
    private Long confirmerId;
    private String confirmer;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime confirmTime;

    private Long archiverId;
    private String archiver;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime archiveTime;

    private String archiveReason;
    private String faultCategory;
    private String faultSubCategory;
    private String impactScope;
    private String rootCause;
    private String preventiveAction;
    private String attachments;
    private String status;
    private String remark;
    private Integer version;

    @Valid
    private List<RepairPart> parts;

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

}
