package cn.iocoder.yudao.module.mes.controller.admin.hc.equipmentconsumable.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 设备耗材事件 Response VO")
@Data
public class HcEquipmentConsumableEventRespVO {

    private Long id;
    private Long stateId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String processCode;
    private String processName;
    private String consumableType;
    private String eventType;
    private String recordSource;
    private Long guideClothRecordId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String grindingStage;
    private Long grindingDetailId;
    private String bizType;
    private Long bizId;
    private String recordGroupNo;
    private String productModelCode;
    private String productMaterialCode;
    private String productBatchNo;
    private String petModel;
    private String petBatchNo;
    private BigDecimal wetInputKg;
    private BigDecimal wetOutputMeter;
    private String beforeBatchNo;
    private String afterBatchNo;
    private Integer beforeUseCount;
    private Integer afterUseCount;
    private Integer changeUseCount;
    private BigDecimal beforeUsedLength;
    private BigDecimal afterUsedLength;
    private BigDecimal changeLength;
    private String replaceReason;
    private Long operatorId;
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime eventTime;

    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
