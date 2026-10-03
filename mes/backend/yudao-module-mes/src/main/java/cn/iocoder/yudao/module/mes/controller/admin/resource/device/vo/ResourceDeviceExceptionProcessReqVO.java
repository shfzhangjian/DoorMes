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
public class ResourceDeviceExceptionProcessReqVO {

    @NotNull(message = "设备异常ID不能为空")
    private Long id;

    @NotBlank(message = "流程动作不能为空")
    private String action;

    private String responseResult;
    private String responseRemark;
    private Long assigneeId;
    private String assignee;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime repairPlanTime;

    private String repairPlanRemark;
    private String repairAction;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime repairTime;

    private String repairStatus;
    private String partChangeDesc;
    private String unfixReason;
    private String confirmResult;
    private String confirmRemark;
    private String rootCause;
    private String preventiveAction;
    private String archiveReason;

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
