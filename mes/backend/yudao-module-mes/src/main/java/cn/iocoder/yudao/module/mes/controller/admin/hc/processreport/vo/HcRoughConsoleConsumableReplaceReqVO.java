package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
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

@Schema(description = "管理后台 - 磨皮看板耗材更换 Request VO")
@Data
public class HcRoughConsoleConsumableReplaceReqVO {
    private HcGrindingConsumptionVO consumption;


    private Long stateId;

    @NotNull(message = "设备ID不能为空")
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;

    @NotBlank(message = "耗材类型不能为空")
    private String consumableType;

    @NotBlank(message = "新批号不能为空")
    private String batchNo;

    private Integer initialUseCount;
    private BigDecimal initialUsedLength;
    private Integer limitCount;
    private BigDecimal limitLength;
    private String replaceReason;
    private Long operatorId;
    private String operatorName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime replaceTime;
}
