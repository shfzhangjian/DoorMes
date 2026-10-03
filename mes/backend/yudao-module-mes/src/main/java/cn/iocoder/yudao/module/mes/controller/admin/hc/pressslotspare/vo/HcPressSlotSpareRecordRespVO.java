package cn.iocoder.yudao.module.mes.controller.admin.hc.pressslotspare.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 压槽备件流水 Response VO")
@Data
public class HcPressSlotSpareRecordRespVO {

    private Long id;
    private Long spareId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String spareType;
    private String eventType;
    private String recordSource;
    private String planNo;
    private String productionBatchNo;
    private String prodType;
    private String modelCode;
    private String productMaterialCode;
    private String productMaterialName;
    private Long planOperationId;
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
    private BigDecimal pressSlotInputPcs;
    private BigDecimal pressSlotOutputPcs;
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
