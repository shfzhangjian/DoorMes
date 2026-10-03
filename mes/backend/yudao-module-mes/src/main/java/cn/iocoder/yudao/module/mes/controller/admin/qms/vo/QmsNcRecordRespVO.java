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

@Schema(description = "管理后台 - 不合格品处理单 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ExcelIgnoreUnannotated
public class QmsNcRecordRespVO extends QmsNcRecordBaseVO {

    @Schema(description = "主键ID")
    @ExcelProperty("主键ID")
    private Long id;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "MRB会签明细")
    private List<Review> reviews;

    @Schema(description = "其他缺陷明细")
    private List<Defect> defects;

    @Schema(description = "关联对象")
    private List<Relation> relations;

    @Schema(description = "流程日志")
    private List<FlowLog> flowLogs;

    @Schema(description = "处置执行分派通知人回复")
    private List<DispositionNotify> dispositionNotifies;

    @Schema(description = "当前登录人是否可办理")
    private Boolean canHandle;

    @Schema(description = "当前登录人是否可撤回修改")
    private Boolean canWithdraw;

    @Schema(description = "列表操作编码")
    private String listActionCode;

    @Schema(description = "列表操作名称")
    private String listActionName;

    @Schema(description = "当前登录人是否待处理")
    private Boolean minePending;

    @Schema(description = "当前登录人是否发现")
    private Boolean mineDiscovered;

    @Schema(description = "当前登录人是否参与")
    private Boolean mineParticipated;

    @Schema(description = "管理后台 - NCR MRB会签 Response VO")
    @Data
    public static class Review {
        private Long id;
        private Long ncRecordId;
        private String ncNo;
        private Long deptId;
        private String deptName;
        private Long handlerUserId;
        private String handlerUserName;
        private Long delegateUserId;
        private String delegateUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime delegateTime;
        private Long actualHandlerUserId;
        private String actualHandlerUserName;
        private String suggestedDisposition;
        private String dispositionDetail;
        private String rootCauseCategory;
        private String causeAnalysis;
        private String reviewOpinion;
        private String reviewStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime handleTime;
        private Integer sort;
    }

    @Schema(description = "管理后台 - NCR 处置执行通知人 Response VO")
    @Data
    public static class DispositionNotify {
        private Long id;
        private Long ncRecordId;
        private String ncNo;
        private Long executionId;
        private String executionNo;
        private String sourceNodeCode;
        private String sourceNodeName;
        private String dispositionType;
        private Long notifyUserId;
        private String notifyUserName;
        private String notifyStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime notifyTime;
        private String replyConclusion;
        private Long replyUserId;
        private String replyUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime replyTime;
        private String remark;
    }

    @Schema(description = "管理后台 - NCR缺陷明细 Response VO")
    @Data
    public static class Defect {
        private Long id;
        private Long ncRecordId;
        private String ncNo;
        private Long defectCodeId;
        private String defectCode;
        private String defectName;
        private String defectPath;
        private String sourceSectionName;
        private String sourceInspectionItem;
        private String sourceResult;
        private Boolean primaryFlag;
        private Integer sort;
    }

    @Schema(description = "管理后台 - NCR关联对象 Response VO")
    @Data
    public static class Relation {
        private Long id;
        private Long ncRecordId;
        private String ncNo;
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
        @Schema(description = "关联 IQC 物料编码")
        private String materialCode;
        @Schema(description = "关联 IQC 物料名称")
        private String materialName;
        @Schema(description = "关联 IQC 规格型号")
        private String specification;
        @Schema(description = "关联 IQC 批次号")
        private String batchNo;
        @Schema(description = "关联 IQC 来料日期")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate arrivalDate;
    }

    @Schema(description = "管理后台 - NCR流程日志 Response VO")
    @Data
    public static class FlowLog {
        private Long id;
        private Long ncRecordId;
        private String ncNo;
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
