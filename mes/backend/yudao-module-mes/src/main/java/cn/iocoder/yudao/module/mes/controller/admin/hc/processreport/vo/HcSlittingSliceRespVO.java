package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - 分切切片记录 Response VO")
@Data
public class HcSlittingSliceRespVO {

    @Schema(description = "上游湿法/二磨留样异常原因；允许加工，裁切报检及完工受限")
    private String upstreamSampleLockReason;

    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long sourceAdhesiveReportId;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private BigDecimal sourceLength;
    private BigDecimal startPosition;
    private BigDecimal endPosition;
    private BigDecimal sliceLength;
    private String cutMode;
    private String sliceSerialNo;
    private Integer sliceIndex;
    private String sizeCode;
    private String sizeName;
    private String printStatus;
    private Integer printCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastPrintTime;

    private String scanStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;

    private String scannerName;
    private String visualResultJson;
    private String selfCheck;
    private String remark;
    private Boolean downstreamFeedbackAbnormal;
    private Long downstreamFeedbackReportId;
    private String downstreamFeedbackProcessCode;
    private String downstreamFeedbackProcessName;
    private String downstreamFeedbackReason;
    private String editBlockedReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
