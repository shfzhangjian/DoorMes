package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Data
public class SrmSampleEvaluationRespVO {

    private Long id;
    private String evaluationNo;
    private Long sampleRequestId;
    private String sampleRequestNo;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String materialName;
    private String materialModel;
    private BigDecimal sampleQty;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate evaluationDate;

    private Integer sampleSendCount;
    private List<String> verificationTypes;
    private List<String> inspectionTypes;
    private String applyDept;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate applyDate;

    private Long approvedByUserId;
    private String approvedByName;
    private Long initiatorUserId;
    private String initiatorUserName;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long assignedInspectorUserId;
    private String assignedInspectorUserName;
    private Long assignedInspectorDeptId;
    private String assignedInspectorDeptName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime assignedTime;

    private Long reporterUserId;
    private String reporterUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;

    private Long confirmUserId;
    private String confirmUserName;
    private String confirmResult;
    private String confirmOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private String initiatorDecisionOpinion;
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
    private List<Item> items;
    private List<Sign> signs;
    private List<SrmSampleEvaluationProjectRespVO.UserConfig> projectUsers;
    private List<Log> logs;
    private Boolean canEdit;
    private Boolean canSubmit;
    private Boolean canInspectionReport;
    private Boolean canValueConfirm;
    private Boolean canSign;
    private Long currentSignId;
    private Boolean canWithdrawConfirm;
    private Boolean canInitiatorDecision;
    private Boolean canFinalApprove;
    private Boolean canArchiveConfirm;
    private Boolean canIssueTrialValidation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Item {
        private Long id;
        private Integer rowNo;
        private String itemName;
        private String technicalRequirement;
        private String testData1;
        private String testData2;
        private String testData3;
        private String testData4;
        private String testData5;
        private String itemJudgement;
        private String itemStatus;
    }

    @Data
    public static class Sign {
        private Long id;
        private String deptCode;
        private String deptName;
        private Long userId;
        private String userName;
        private String signStatus;
        private String signResult;
        private String signOpinion;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime signTime;

        private Boolean requireAttachment;
    }

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

}
