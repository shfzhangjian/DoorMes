package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM供方年度评审计划 Response VO")
public class SrmSupplierReviewPlanRespVO {

    @Data
    public static class YearPlan {

        private Long id;
        private String planNo;
        private Integer planYear;
        private String planTitle;
        private String completionSummary;
        private String preparedDept;
        private String preparedBy;
        private String confirmedBy;
        private String approvedBy;
        private String remark;
        private Integer version;
        private List<Line> lines = new ArrayList<>();

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime updateTime;

    }

    @Data
    public static class Line {

        private Long id;
        private Long yearPlanId;
        private Integer planYear;
        private Integer rowNo;
        private Long supplierId;
        private String supplierCode;
        private String supplierName;
        private String contactPerson;
        private String materialCode;
        private List<String> materialCodes = new ArrayList<>();
        private String materialName;
        private String model;
        private String applicableProduct;
        private String providedProduct;
        private String completionStatus;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate latestAuditDate;

        private String remark;
        private Integer version;
        private List<MonthPlan> months = new ArrayList<>();

    }

    @Data
    public static class DeleteAnnualPlansResult {

        private Long lineId;
        private Integer planYear;
        private String materialCode;
        private String supplierCode;
        private Long yearPlanId;
        private Integer deletedLineCount;
        private Integer deletedMonthCount;
        private Integer deletedParticipantCount;
        private Integer deletedReplyCount;
        private Integer deletedStatusLogCount;
        private Integer deletedAttachmentCount;

    }

    @Data
    public static class MonthPlan {

        private Long id;
        private Long yearPlanId;
        private Long lineId;
        private Integer planYear;
        private Integer planMonth;
        private Boolean plannedFlag;
        private String executionStatus;
        private String executionStatusName;
        private String planDesc;
        private Long leadUserId;
        private String leadUserName;
        private String relatedUserIds;
        private String relatedUserNames;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate auditDate;

        /** 审核类别：认证审核/年度审核/不定期审核 */
        private String auditCategory;

        /** 审核类别名称 */
        private String auditCategoryName;

        /** 审核说明 */
        private String auditDesc;

        /** 审核附件（现场考察资料快照说明） */
        private String auditAttachment;

        private Long approverUserId;
        private String approverUserName;
        private String approvalOpinion;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime approvalTime;

        private String statusRemark;
        private String updateDescription;
        private String remark;
        private Integer version;
        private List<Participant> participants = new ArrayList<>();
        private List<Reply> replies = new ArrayList<>();
        private List<StatusLog> statusLogs = new ArrayList<>();

    }

    @Data
    public static class Participant {

        private Long id;
        private Long monthPlanId;
        private String relationType;
        private Long userId;
        private String userName;
        private String deptName;

    }

    @Data
    public static class Reply {

        private Long id;
        private Long monthPlanId;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime replyTime;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate reviewDate;

        private Long recorderUserId;
        private String recorderUserName;
        private String reviewResult;
        private String remark;

    }

    @Data
    public static class StatusLog {

        private Long id;
        private Long monthPlanId;
        private String fromStatus;
        private String fromStatusName;
        private String toStatus;
        private String toStatusName;
        private String reason;
        private String updateDescription;
        private Long operatorUserId;
        private String operatorUserName;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;

    }

    @Data
    public static class ExecutionItem {

        private Long monthPlanId;
        private Long lineId;
        private Integer planYear;
        private Integer planMonth;
        private String supplierCode;
        private String supplierName;
        /** 使用部门（来自供应商主数据 mes_supplier.using_department） */
        private String useDepartment;
        private String contactPerson;
        private String materialCode;
        private String materialName;
        private String model;
        private String applicableProduct;
        private String providedProduct;
        private String executionStatus;
        private String executionStatusName;
        private String planDesc;
        private String leadUserName;
        private String relatedUserNames;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate auditDate;

        /** 审核类别：认证审核/年度审核/不定期审核 */
        private String auditCategory;

        /** 审核类别名称 */
        private String auditCategoryName;

        /** 审核说明 */
        private String auditDesc;

        /** 审核附件（现场考察资料快照说明） */
        private String auditAttachment;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime updateTime;

    }

}
