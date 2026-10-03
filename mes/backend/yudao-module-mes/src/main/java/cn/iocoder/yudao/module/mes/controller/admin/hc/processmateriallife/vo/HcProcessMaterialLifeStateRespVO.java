package cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工序耗材在用状态 Response VO")
@Data
public class HcProcessMaterialLifeStateRespVO {

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
    private String consumableTypeName;
    private String batchNo;
    private Integer useCount;
    private BigDecimal usedLength;
    private Integer limitCount;
    private BigDecimal limitLength;
    private Integer warningFlag;
    private String status;
    private Long lastOperatorId;
    private String lastOperatorName;
    private String lastReplacePlanNo;
    private String lastReplaceReason;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReplaceTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastEventTime;
}
