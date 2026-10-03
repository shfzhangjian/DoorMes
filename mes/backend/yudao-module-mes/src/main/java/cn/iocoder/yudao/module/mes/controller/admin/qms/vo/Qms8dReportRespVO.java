package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS 8D报告 Response VO")
@Data
@ExcelIgnoreUnannotated
public class Qms8dReportRespVO {

    @ExcelProperty("主键ID")
    private Long id;
    @ExcelProperty("8D报告号")
    private String reportNo;
    @ExcelProperty("来源类型")
    private String sourceType;
    private Long sourceId;
    @ExcelProperty("来源单号")
    private String sourceNo;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ExcelProperty("立案日期")
    private LocalDate issueDate;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @ExcelProperty("要求结案日")
    private LocalDate targetDate;
    @ExcelProperty("当前阶段")
    private String currentStep;
    @ExcelProperty("状态")
    private String status;
    private String currentNodeCode;
    private String currentNodeName;
    private Long currentHandlerUserId;
    @ExcelProperty("当前处理人")
    private String currentHandlerUserName;
    private Long initiatorUserId;
    @ExcelProperty("发起人")
    private String initiatorUserName;
    private Long initiatorDeptId;
    private String initiatorDeptName;
    @ExcelProperty("问题描述")
    private String problemDesc;
    private String containmentAction;
    private Long containmentOwnerId;
    private String containmentOwnerName;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate containmentDate;
    private String rootCauseCategory;
    private String rootCauseAnalysis;
    private String correctiveAction;
    private Long actionOwnerId;
    private String actionOwnerName;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actionPlanDate;
    private String validationResult;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validationDate;
    private Boolean updateSop;
    private Boolean updateFmea;
    private Boolean updateControlPlan;
    private String standardizeDesc;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;
    private Long closeUserId;
    private String closeUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private List<TeamMember> teamMembers;
    private List<Relation> relations;
    private List<ActionItem> actionItems;
    private List<FlowLog> flowLogs;

    @Data
    public static class TeamMember {
        private Long id;
        private Long reportId;
        private String memberRole;
        private Long deptId;
        private String deptName;
        private Long userId;
        private String userName;
        private String responsibility;
        private Integer sort;
    }

    @Data
    public static class Relation {
        private Long id;
        private Long reportId;
        private String reportNo;
        private String relationType;
        private Long relatedObjectId;
        private String relatedObjectNo;
        private String relatedObjectName;
        private String relationStatus;
        private Boolean primaryFlag;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime relationTime;
        private Long relationUserId;
        private String relationUserName;
        private String remark;
    }

    @Data
    public static class ActionItem {
        private Long id;
        private Long reportId;
        private String reportNo;
        private String actionType;
        private String actionDesc;
        private String rootCauseCategory;
        private Long ownerUserId;
        private String ownerUserName;
        private Long ownerDeptId;
        private String ownerDeptName;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate planFinishDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate actualFinishDate;
        private String itemStatus;
        private String finishDesc;
        private String verificationResult;
        private Integer sort;
        private String remark;
    }

    @Data
    public static class FlowLog {
        private Long id;
        private Long reportId;
        private String reportNo;
        private String actionCode;
        private String actionName;
        private String fromStatus;
        private String toStatus;
        private String fromStep;
        private String toStep;
        private String fromNodeCode;
        private String fromNodeName;
        private String toNodeCode;
        private String toNodeName;
        private String opinion;
        private Long handlerUserId;
        private String handlerUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime handleTime;
        private Map<String, Object> businessSnapshot;
    }
}
