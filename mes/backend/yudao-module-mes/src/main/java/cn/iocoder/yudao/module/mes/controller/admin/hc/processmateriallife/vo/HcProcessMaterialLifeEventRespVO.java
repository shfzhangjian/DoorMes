package cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 工序耗材使用更换流水 Response VO")
@Data
public class HcProcessMaterialLifeEventRespVO {

    private Long id;
    private Long stateId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String processCode;
    private String processName;
    private String consumableType;
    private String consumableTypeName;
    private String eventType;
    private String planNo;
    private String motherBatchNo;
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
}
