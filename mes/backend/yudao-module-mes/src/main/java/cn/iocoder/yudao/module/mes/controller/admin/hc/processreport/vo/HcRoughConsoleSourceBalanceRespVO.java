package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮来源余额 Response VO")
@Data
public class HcRoughConsoleSourceBalanceRespVO {

    private Long id;
    private String balanceKey;
    private String sourceType;
    private Long sourcePlanId;
    private String sourcePlanNo;
    private Long sourcePlanOperationId;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String modelName;
    private BigDecimal totalLength;
    private BigDecimal usedFirstLength;
    private BigDecimal usedSecondLength;
    private BigDecimal reservedLength;
    private BigDecimal availableLength;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastReportTime;

    private String status;
    private String remark;
}
