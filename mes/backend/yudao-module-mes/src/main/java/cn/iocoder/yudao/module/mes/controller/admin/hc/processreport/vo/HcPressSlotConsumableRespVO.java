package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 压槽工位耗材状态 Response VO")
@Data
public class HcPressSlotConsumableRespVO {

    private Long stateId;
    private Long stockId;
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
    private String materialCode;
    private String materialName;
    private String batchNo;
    private BigDecimal onlineQuantity;
    private BigDecimal availableCount;
    private Integer useCount;
    private Integer limitCount;
    private Integer limitDays;
    private Integer useDays;
    private Integer warningFlag;
    private String status;
    private String message;
    private Long lastOperatorId;
    private String lastOperatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReplaceTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastCleanTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastEventTime;
}
