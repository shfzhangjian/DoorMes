package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - CMP软垫翘曲片号统计 Response VO")
@Data
public class QmsCmpWarpageSliceStatRespVO {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    private String modelCode;
    private String parentBatchNo;
    private String segmentSliceNo;
    private String productionSliceNo;
    private String customerSliceNo;
    private String customerCode;
    private String customerName;
    private String shippingNoticeNo;
    private BigDecimal warpageValueMm;
    private String inspectionResult;
    private Boolean manualOverride;
    private String sourceActualValue;
    private String sourceItemResult;
    private Long sourceFqcId;
    private String sourceFqcNo;
    private Long sourceSubmissionDetailId;
    private Long sourceFqcItemId;
    private Long shippingFqcDetailId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastSyncTime;

    private String syncMessage;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
}
