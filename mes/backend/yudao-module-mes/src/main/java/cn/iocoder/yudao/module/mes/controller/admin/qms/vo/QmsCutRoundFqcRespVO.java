package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeEvaluationRespVO;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验 Response VO")
@Data
public class QmsCutRoundFqcRespVO {

    private Long id;
    private String fqcNo;
    private String reportNo;
    private String workOrderNo;
    private Long sourceReportId;
    private String sourceReportNo;
    private String sourceModule;
    private String sourceOperationCode;
    private String sourceOperationName;
    private Long planOrderId;
    private String operationCode;
    private String operationName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String productModel;
    private String productBatchNo;
    private String batchNo;
    private BigDecimal produceQty;
    private String unitCode;
    private String unitName;
    private Integer sampleQty;
    private Integer submissionDetailCount;
    private Integer okQty;
    private Integer ngQty;
    private String submissionType;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime submissionTime;
    private String submitterName;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    private String status;
    private String judgment;
    private Long inspectorId;
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;
    private Long qaInspectorId;
    private String qaInspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime qaTime;
    private String releaseResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;
    private String remark;
    private String entryMode;
    private String entryLayout;
    private Integer entryProgress;
    private Integer requiredItemCount;
    private Integer completedItemCount;
    private Integer abnormalItemCount;
    private Boolean sheetLocked;
    private String lastReturnReason;
    private Boolean recheckFlag;
    private String originalInspectionNo;
    private String rejectRootInspectionNo;
    private String rejectPrevInspectionNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    /**
     * 本批裁切完工至成品检验开工的 QTIME。该值属于整张检验单，不属于单片明细。
     */
    private HcQtimeEvaluationRespVO qtime;
    private List<SubmissionDetail> submissionDetails;
    private List<QmsFqcRespVO.FqcItem> items;
    private List<QmsFqcRespVO.FqcAbnormal> abnormals;

    @Data
    public static class SubmissionDetail {
        private Long id;
        private Long fqcId;
        private String fqcNo;
        private Long cutRoundInspectionTaskId;
        private String cutRoundInspectionTaskNo;
        private Long cutRoundInspectionDetailId;
        private Long cutRoundReportId;
        private Integer seqNo;
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String operationCode;
        private String operationName;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String sizeRule;
        private String productionBatchNo;
        private String parentProductionBatchNo;
        private String qualityRiskFlag;
        private String qualityRiskSnapshotJson;
        private List<String> photoUrls;
        private String rowJudgment;
        private String defectCode;
        private String defectName;
        private String ngReason;
        private Integer entryProgress;
        private Integer requiredItemCount;
        private Integer completedItemCount;
        private Integer abnormalItemCount;
        private Long inspectorId;
        private String inspectorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;
        private String remark;
        private Boolean recheckDetailFlag;
        private List<QmsFqcRespVO.FqcItem> items;
    }
}
