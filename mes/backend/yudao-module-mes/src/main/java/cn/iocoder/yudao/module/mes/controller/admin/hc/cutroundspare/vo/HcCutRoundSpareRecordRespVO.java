package cn.iocoder.yudao.module.mes.controller.admin.hc.cutroundspare.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 裁切备件流水 Response VO")
@Data
public class HcCutRoundSpareRecordRespVO {

    private Long id;
    private Long ledgerId;
    private Long consumeId;
    private BigDecimal replaceQuantity;
    private Long spareId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String spareType;
    private String eventType;
    private String recordSource;
    private Long planId;
    private String planNo;
    private String productionBatchNo;
    private String modelCode;
    private String cutSizeMm;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String bizType;
    private Long bizId;
    private String recordGroupNo;
    private String beforeMaterialCode;
    private String beforeBatchNo;
    private String afterMaterialCode;
    private String afterBatchNo;
    private Integer beforeUseCount;
    private Integer afterUseCount;
    private Integer changeUseCount;
    private BigDecimal cutInputPcs;
    private BigDecimal cutOutputPcs;
    private Integer feltUseDays;
    private BigDecimal beforeAvailableQuantity;
    private BigDecimal afterAvailableQuantity;
    private BigDecimal changeQuantity;
    private BigDecimal onlineQuantity;
    private BigDecimal offlineQuantity;
    private Integer finalUseCount;
    private String replaceReason;
    private Long operatorId;
    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime eventTime;

    private String remark;
}
