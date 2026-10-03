package cn.iocoder.yudao.module.mes.controller.admin.hc.guideclothrecord.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 导布更换记录运行时 Response VO")
@Data
public class HcGuideClothRuntimeRespVO {

    private String lineName;
    private String lineCode;
    private Integer nextUseCount;
    private Integer currentUseCount;
    private String replacePlanNo;
    private String petBatchNo;
    private String guideClothBatchNo;
    private String petModel;
    private String replaceReason;
    private Long consumableStateId;
    private BigDecimal currentUsedLength;
    private BigDecimal nextUsedLength;
    private BigDecimal limitLength;
    private Integer warningFlag;
    private String warningText;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime replaceTime;
}
