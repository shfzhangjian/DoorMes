package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商导入申请 Response VO")
@Data
public class SrmOnboardingApplyRespVO {

    private Long id;
    private String applyNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String materialName;
    private String materialCode;
    private String materialModel;
    private String applicableProduct;
    private String importType;
    private String replacedMaterialCode;
    private String replacedMaterialName;
    private String customerMaterialCode;
    private String customerMaterialName;
    private String applyReason;
    private String supplierAdvantageDesc;
    private String supplementDesc;
    private Boolean materialCodeCreated;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate materialCodeCompleteDate;

    private Boolean supplierRosterCreated;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate supplierRosterCompleteDate;

    private String specReq;
    private String natureRequirement;
    private String certRequirement;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long purchaseHandlerUserId;
    private String purchaseHandlerUserName;
    private String purchaseIntakeOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime purchaseIntakeTime;

    private Long useDeptReviewerUserId;
    private String useDeptReviewerUserName;
    private String useDeptResult;
    private String useDeptOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime useDeptHandleTime;

    private Long qualityReviewerUserId;
    private String qualityReviewerUserName;
    private String qualityResult;
    private String qualityOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime qualityHandleTime;

    private Long techReviewerUserId;
    private String techReviewerUserName;
    private String techResult;
    private String techOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime techHandleTime;

    private Long purchaseReviewerUserId;
    private String purchaseReviewerUserName;
    private String purchaseResult;
    private String purchaseOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime purchaseHandleTime;

    private Long generalManagerUserId;
    private String generalManagerUserName;
    private String generalManagerResult;
    private String generalManagerOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime generalManagerHandleTime;

    private Long materialEntryUserId;
    private String materialEntryUserName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate materialEntryRequiredDate;

    private String materialEntryCode;
    private String materialEntryOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime materialEntryHandleTime;

    private Long supplierRosterEntryUserId;
    private String supplierRosterEntryUserName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate supplierRosterEntryRequiredDate;

    private String supplierRosterEntryCode;
    private String supplierRosterEntryOpinion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime supplierRosterEntryHandleTime;

    private Long applicantId;
    private String applicantName;
    private String applyDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime applyTime;

    private String remark;
    private Integer version;
    private Boolean canEdit;
    private Boolean canSubmit;
    private Boolean canPurchaseIntake;
    private Boolean canUseDeptReview;
    private Boolean canQualityReview;
    private Boolean canTechReview;
    private Boolean canPurchaseReview;
    private Boolean canGeneralManagerReview;
    private Boolean canSign;
    private Long currentSignId;
    private Boolean canPurchaseTransfer;
    private Boolean canMaterialEntry;
    private Boolean canSupplierRosterEntry;
    private Boolean canArchive;
    private List<Sign> signs;
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
    public static class Sign {
        private Long id;
        private Long applyId;
        private String deptCode;
        private String deptName;
        private Long userId;
        private String userName;
        private String signStatus;
        private String signResult;
        private String signOpinion;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime signTime;
    }

}
