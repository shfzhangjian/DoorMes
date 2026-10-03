package cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 设备耗材调整 Request VO")
@Data
public class HcEquipmentConsumableAdjustReqVO {

    private Long id;

    @NotNull(message = "设备不能为空")
    private Long equipmentId;

    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;

    private String processCode;
    private String processName;

    @NotBlank(message = "耗材类型不能为空")
    private String consumableType;

    @NotBlank(message = "批号不能为空")
    private String batchNo;

    private String lastReplacePlanNo;
    private String replaceReason;
    private Integer useCount;
    private BigDecimal usedLength;
    private Integer limitCount;
    private BigDecimal limitLength;
    private String status;
    private String eventType;
    private Long operatorId;
    private String operatorName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime eventTime;

    private String remark;
}
