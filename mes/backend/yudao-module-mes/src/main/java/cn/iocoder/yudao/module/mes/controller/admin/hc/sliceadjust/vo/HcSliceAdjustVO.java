package cn.iocoder.yudao.module.mes.controller.admin.hc.sliceadjust.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

public class HcSliceAdjustVO {

    @Schema(description = "管理后台 - 片号调账候选查询 Request VO")
    @Data
    public static class CandidateQueryReqVO {

        @Schema(description = "分段/母批批号")
        private String segmentBatchNo;

        @Schema(description = "关键词：片号/计划号/物料/型号/工序")
        private String keyword;
    }

    @Schema(description = "管理后台 - 片号调账候选 Response VO")
    @Data
    public static class CandidateRespVO {

        @Schema(description = "片号")
        private String sliceNo;

        @Schema(description = "分段/母批批号")
        private String segmentBatchNo;

        @Schema(description = "最后报工工序编码")
        private String lastProcessCode;

        @Schema(description = "最后报工工序名称")
        private String lastProcessName;

        @Schema(description = "计划号")
        private String planNo;

        @Schema(description = "物料编码")
        private String materialCode;

        @Schema(description = "物料名称")
        private String materialName;

        @Schema(description = "型号")
        private String modelCode;

        @Schema(description = "最近状态")
        private String statusText;

        @Schema(description = "最近结果")
        private String resultText;

        @Schema(description = "关联报工/检验/库存记录数")
        private Integer relatedCount;

        @Schema(description = "最后报工时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastReportTime;
    }

    @Schema(description = "管理后台 - 片号调账关联单据查询 Request VO")
    @Data
    public static class TraceQueryReqVO {

        @Schema(description = "分段/母批批号")
        private String segmentBatchNo;

        @Schema(description = "左侧片号")
        private String leftSliceNo;

        @Schema(description = "右侧片号")
        private String rightSliceNo;
    }

    @Schema(description = "管理后台 - 片号调账关联单据 Response VO")
    @Data
    public static class TraceRespVO {

        @Schema(description = "所属侧：left/right")
        private String side;

        @Schema(description = "片号")
        private String sliceNo;

        @Schema(description = "分段/母批批号")
        private String segmentBatchNo;

        @Schema(description = "工序编码")
        private String processCode;

        @Schema(description = "工序名称")
        private String processName;

        @Schema(description = "排序")
        private Integer stageSort;

        @Schema(description = "来源表")
        private String sourceTable;

        @Schema(description = "来源ID")
        private Long sourceId;

        @Schema(description = "状态")
        private String statusText;

        @Schema(description = "结果")
        private String resultText;

        @Schema(description = "报工/业务时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime reportTime;
    }

    @Schema(description = "管理后台 - 片号调账审计查询 Request VO")
    @Data
    public static class AuditQueryReqVO {

        @Schema(description = "关键词：调账单号/片号/分段/操作人")
        private String keyword;
    }

    @Schema(description = "管理后台 - 片号调账审计 Response VO")
    @Data
    public static class AuditRecordRespVO {

        private Long id;
        private String adjustNo;
        private String adjustType;
        private String segmentBatchNo;
        private String leftSliceNo;
        private String rightSliceNo;
        private String leftLastProcessName;
        private String rightLastProcessName;
        private String operatorName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime adjustTime;
        private String adjustReason;
        private Integer affectedRows;
    }

    @Schema(description = "管理后台 - 片号直接修改 Request VO")
    @Data
    public static class RenameReqVO {

        @Schema(description = "分段/母批批号；不传时后端按原片号自动识别")
        private String segmentBatchNo;

        @Schema(description = "原片号")
        @NotBlank(message = "原片号不能为空")
        private String sourceSliceNo;

        @Schema(description = "新片号")
        @NotBlank(message = "新片号不能为空")
        private String targetSliceNo;

        @Schema(description = "操作人")
        private String operatorName;

        @Schema(description = "调账原因")
        @NotBlank(message = "调账原因不能为空")
        private String reason;
    }

    @Schema(description = "管理后台 - 片号调账提交 Request VO")
    @Data
    public static class SwapReqVO {

        @Schema(description = "分段/母批批号；不传时后端按两片号共同分段自动识别")
        private String segmentBatchNo;

        @Schema(description = "左侧片号")
        @NotBlank(message = "左侧片号不能为空")
        private String leftSliceNo;

        @Schema(description = "右侧片号")
        @NotBlank(message = "右侧片号不能为空")
        private String rightSliceNo;

        @Schema(description = "操作人")
        private String operatorName;

        @Schema(description = "调账原因")
        @NotBlank(message = "调账原因不能为空")
        private String reason;
    }

    @Schema(description = "管理后台 - 片号调账提交 Response VO")
    @Data
    public static class SwapRespVO {

        @Schema(description = "调账单号")
        private String adjustNo;

        @Schema(description = "分段/母批批号")
        private String segmentBatchNo;

        @Schema(description = "左侧片号")
        private String leftSliceNo;

        @Schema(description = "右侧片号")
        private String rightSliceNo;

        @Schema(description = "影响行数")
        private Integer affectedRows;

        @Schema(description = "提示信息")
        private String message;

        @Schema(description = "影响字段明细")
        private List<ColumnAffectedVO> affectedColumns;
    }

    @Schema(description = "管理后台 - 片号调账影响字段明细 VO")
    @Data
    public static class ColumnAffectedVO {

        @Schema(description = "表名")
        private String tableName;

        @Schema(description = "字段名")
        private String columnName;

        @Schema(description = "字段说明")
        private String columnComment;

        @Schema(description = "影响行数")
        private Integer affectedRows;
    }
}
