package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM样品需求 Response VO")
@Data
public class SrmSampleRequestRespVO {

    private Long id;
    private String requestNo;
    private String materialName;
    private String materialModel;
    private String applyType;
    private String applyDept;
    private String usedProduct;
    private BigDecimal requireQty;
    private Integer sampleEvaluationCount;
    private Long applicantId;
    private String applicantName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate applyDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate requireDate;

    private String specifiedSupplierType;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String technicalRequirement;
    private String purchaseDifficulty;
    private String rdSampleNecessity;
    private String oaApprovalUrl;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long projectLeaderUserId;
    private String projectLeaderUserName;
    private String projectLeaderOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime projectLeaderHandleTime;

    private Long purchaseOwnerUserId;
    private String purchaseOwnerUserName;
    private String purchaseOwnerOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime purchaseOwnerHandleTime;

    private Long finalApproverUserId;
    private String finalApproverUserName;
    private String finalApproverOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime finalApproverHandleTime;

    private String archiveOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime archiveTime;

    private String remark;
    private Integer version;
    private Boolean canEdit;
    private Boolean canSubmit;
    private Boolean canProjectReview;
    private Boolean canPurchaseReview;
    private Boolean canInitiatorDecision;
    private Boolean canFinalApprove;
    private Boolean canArchiveConfirm;
    private List<TrialValidation> trialValidations;
    private List<Log> logs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Log {
        private Long id;
        private String action;
        private String actionName;
        private String fromStatus;
        private String toStatus;
        private Long operatorId;
        private String operatorName;
        private String actionDescription;
        private String detailJson;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }

    @Data
    public static class TrialValidation {
        private Long id;
        private String trialNo;
        private Long sourceSampleEvaluationId;
        private String sourceSampleEvaluationNo;
        private Long supplierId;
        private String supplierCode;
        private String supplierName;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String materialModel;
        private String materialBatchNo;
        private BigDecimal quantity;
        private String status;
        private String currentNodeName;
        private Long initiatorUserId;
        private String initiatorUserName;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime noticeTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime trialExecutionTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime productionCompleteTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime archiveTime;
    }

}
