package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板耗材状态 Response VO")
@Data
public class HcRoughConsoleConsumableRespVO {

    private Long id;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
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
    private String lastOperatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastEventTime;
}
