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
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - QMS异常事件 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ExcelIgnoreUnannotated
public class QmsExceptionEventRespVO extends QmsExceptionEventBaseVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "临时调查小组")
    private List<TeamMember> teamMembers;

    @Schema(description = "围堵/根因临时小组任务")
    private List<GroupTask> groupTasks;

    @Schema(description = "关联对象")
    private List<Relation> relations;

    @Schema(description = "流程日志")
    private List<FlowLog> flowLogs;

    @Schema(description = "当前登录人是否有待提交子任务")
    private Boolean currentUserTaskTodo;

    @Schema(description = "当前登录人待提交子任务数量")
    private Integer currentUserTaskTodoCount;

    @Schema(description = "当前登录人待提交子任务类型：INVESTIGATION_GROUP/ROOT_CAUSE_PREVENTIVE/MIXED")
    private String currentUserTaskTodoType;

    @Schema(description = "当前登录人待提交子任务操作文案")
    private String currentUserTaskTodoLabel;

    @Schema(description = "当前登录人是否有已处理子任务")
    private Boolean currentUserTaskDone;

    @Schema(description = "当前登录人已处理子任务数量")
    private Integer currentUserTaskDoneCount;

    @Schema(description = "当前登录人已处理子任务类型")
    private String currentUserTaskDoneType;

    @Schema(description = "当前登录人已处理子任务文案")
    private String currentUserTaskDoneLabel;

    @Schema(description = "当前登录人是否可办理当前异常事件")
    private Boolean canHandle;

    @Schema(description = "当前登录人是否可撤回上一环节办理")
    private Boolean canWithdraw;

    @Schema(description = "列表操作编码")
    private String listActionCode;

    @Schema(description = "列表操作文案")
    private String listActionName;

    @Schema(description = "是否属于待我处理")
    private Boolean minePending;

    @Schema(description = "是否属于我发现的")
    private Boolean mineDiscovered;

    @Schema(description = "是否属于我参与的")
    private Boolean mineParticipated;

    @Schema(description = "管理后台 - QMS异常事件小组成员 Response VO")
    @Data
    public static class TeamMember {
        private Long id;
        private Long deptId;
        private String deptName;
        private Long userId;
        private String userName;
        private String memberRole;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate joinDate;
        private String signStatus;
    }

    @Schema(description = "管理后台 - QMS异常事件临时小组任务 Response VO")
    @Data
    public static class GroupTask {
        private Long id;
        private Long exceptionId;
        private String exceptionNo;
        private String groupName;
        private String taskType;
        private String taskStatus;
        private Integer replyCount;
        private Long dispatcherUserId;
        private String dispatcherUserName;
        private Long executorUserId;
        private String executorUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime dispatchTime;
        private String containmentSuggestion;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime containmentDeadline;
        private Long containmentDeptId;
        private String containmentDeptName;
        private Long rootCauseOwnerId;
        private String rootCauseOwnerName;
        private String rootCauseAssignment;
        private String preventiveAssignment;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime actualFinishTime;
        private String actionDescription;
        private String rootCauseCategory;
        private String rootCause;
        private String preventiveAction;
        private List<String> attachmentUrls;
        private Long submitterUserId;
        private String submitterUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime submitTime;
        private Long reviewerUserId;
        private String reviewerUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime reviewTime;
        private String reviewOpinion;
        private String remark;
        private List<GroupTaskMember> members;
        private List<GroupTaskReply> replies;
        private List<GroupTaskConfirmLog> confirmLogs;
        private Boolean currentUserCanSubmit;
        private Boolean currentUserCanConfirm;
        private Boolean currentUserCanReview;
    }

    @Schema(description = "管理后台 - QMS异常事件临时小组任务成员 Response VO")
    @Data
    public static class GroupTaskMember {
        private Long id;
        private Long exceptionId;
        private Long taskId;
        private Long deptId;
        private String deptName;
        private Long userId;
        private String userName;
        private Long delegateUserId;
        private String delegateUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime delegateTime;
        private Long actualHandlerUserId;
        private String actualHandlerUserName;
        private String memberRole;
        private Integer sortNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime actualFinishTime;
        private String actionDescription;
        private String rootCauseCategory;
        private String rootCause;
        private String preventiveAction;
        private List<String> attachmentUrls;
        private String confirmStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime confirmTime;
        private String confirmRemark;
        private Boolean overdueFlag;
    }

    @Schema(description = "管理后台 - QMS异常事件任务回复历史 Response VO")
    @Data
    public static class GroupTaskReply {
        private Long id;
        private Long exceptionId;
        private Long taskId;
        private String taskType;
        private Integer replyNo;
        private String replyStatus;
        private Long memberId;
        private Long memberUserId;
        private String memberUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime actualFinishTime;
        private String actionDescription;
        private String rootCauseCategory;
        private String rootCause;
        private String preventiveAction;
        private List<String> attachmentUrls;
        private Long submitterUserId;
        private String submitterUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime submitTime;
        private Long reviewerUserId;
        private String reviewerUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime reviewTime;
        private String reviewOpinion;
        private String remark;
    }

    @Schema(description = "管理后台 - QMS异常事件任务确认历史 Response VO")
    @Data
    public static class GroupTaskConfirmLog {
        private Long id;
        private Long exceptionId;
        private String exceptionNo;
        private Long taskId;
        private String taskType;
        private Long replyId;
        private Integer replyNo;
        private Long memberId;
        private Long userId;
        private String userName;
        private String confirmAction;
        private String confirmStatus;
        private String confirmOpinion;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime confirmTime;
        private String taskStatusBefore;
        private String taskStatusAfter;
        private String remark;
    }

    @Schema(description = "管理后台 - QMS异常事件关联对象 Response VO")
    @Data
    public static class Relation {
        private Long id;
        private Long exceptionId;
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

    @Schema(description = "管理后台 - QMS异常事件流程日志 Response VO")
    @Data
    public static class FlowLog {
        private Long id;
        private Long exceptionId;
        private String exceptionNo;
        private String actionCode;
        private String actionName;
        private String fromStatus;
        private String toStatus;
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
