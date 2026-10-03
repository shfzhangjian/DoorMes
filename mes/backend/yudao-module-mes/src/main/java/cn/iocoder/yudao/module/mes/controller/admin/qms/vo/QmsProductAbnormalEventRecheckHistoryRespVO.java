package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 产品异常事件驳回复检历史 Response VO")
@Data
public class QmsProductAbnormalEventRecheckHistoryRespVO {

    @Schema(description = "复检链路ID")
    private Long groupId;

    @Schema(description = "统一质量任务ID；为空表示历史独立复检链")
    private Long dispatchTaskId;

    @Schema(description = "检验来源类型")
    private String sourceType;

    @Schema(description = "根检验单ID")
    private Long rootInspectionId;

    @Schema(description = "根检验单号")
    private String rootInspectionNo;

    @Schema(description = "最新检验单ID")
    private Long latestInspectionId;

    @Schema(description = "最新检验单号")
    private String latestInspectionNo;

    @Schema(description = "链路状态：RECHECKING、RECHECK_OK、RECHECK_NG")
    private String chainStatus;

    @Schema(description = "驳回复检次数")
    private Integer totalRecheckCount;

    @Schema(description = "驳回复检明细")
    private List<Detail> details;

    @Schema(description = "驳回复检历史明细")
    @Data
    public static class Detail {

        @Schema(description = "轮次；统一任务链从1开始，历史独立链可能从0开始")
        private Integer roundNo;

        @Schema(description = "统一质量任务轮次ID")
        private Long dispatchTaskRoundId;

        @Schema(description = "检验单ID")
        private Long inspectionId;

        @Schema(description = "检验单号")
        private String inspectionNo;

        @Schema(description = "上一轮检验单ID")
        private Long prevInspectionId;

        @Schema(description = "上一轮检验单号")
        private String prevInspectionNo;

        @Schema(description = "检验状态")
        private String inspectionStatus;

        @Schema(description = "判定结果")
        private String inspectionJudgment;

        @Schema(description = "驳回说明")
        private String rejectReason;

        @Schema(description = "驳回人ID")
        private Long rejectUserId;

        @Schema(description = "驳回人")
        private String rejectUserName;

        @Schema(description = "驳回时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime rejectTime;

        @Schema(description = "检验完成时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime resultTime;
    }
}
