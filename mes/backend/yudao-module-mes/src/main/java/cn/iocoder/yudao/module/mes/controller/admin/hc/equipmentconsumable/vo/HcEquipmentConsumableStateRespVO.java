package cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 设备耗材状态 Response VO")
@Data
public class HcEquipmentConsumableStateRespVO {

    private Long id;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String processCode;
    private String processName;
    private String consumableType;
    private String batchNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReplaceTime;

    private String lastReplacePlanNo;
    private String lastReplaceReason;
    private Integer useCount;
    private BigDecimal usedLength;
    private Integer limitCount;
    private BigDecimal limitLength;
    private Integer warningFlag;
    private String status;
    private Long lastOperatorId;
    private String lastOperatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastEventTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
