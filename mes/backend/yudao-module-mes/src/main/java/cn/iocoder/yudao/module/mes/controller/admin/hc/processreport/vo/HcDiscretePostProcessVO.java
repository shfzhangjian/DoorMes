package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 离散后加工工作台 VO。
 */
public class HcDiscretePostProcessVO {

    @Schema(description = "管理后台 - 离散后加工创建计划 Request VO")
    @Data
    public static class CreatePlanReqVO {

        @Schema(description = "目标工序编码列表；按压槽、粘胶2、裁切顺序加工", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "请选择离散后加工工序")
        private List<String> operationCodes;

        @Schema(description = "来源中间品库存ID列表")
        private List<Long> stockIds;

        @Schema(description = "来源NG逐片库存ID列表")
        private List<Long> ngPieceIds;

        @Schema(description = "来源首工序未报工锁定明细ID列表")
        private List<Long> sourceLockIds;

        @Schema(description = "计划型号；离散后加工人工填写，前三位需与已选片号型号一致", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "计划型号不能为空")
        private String planModel;

        @Schema(description = "执行要求；离散后加工人工填写", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "执行要求不能为空")
        private String executionRequirement;

        @Schema(description = "计划开始日期")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate productionStartDate;

        @Schema(description = "计划结束日期")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate productionEndDate;

        @Schema(description = "是否创建后立即下达")
        private Boolean releaseNow;

        @Schema(description = "备注")
        private String remark;
    }

    @Schema(description = "管理后台 - 离散后加工来源库存查询 Request VO")
    @Data
    public static class SourceQueryReqVO {

        @Schema(description = "首道目标工序编码")
        private String targetOpCode;

        @Schema(description = "来源工序编码")
        private String sourceOpCode;

        @Schema(description = "来源类型列表")
        private List<String> sourceTypes;

        @Schema(description = "来源池：NG/WIP/ALL，默认NG")
        private String sourcePool;

        @Schema(description = "NG片号状态列表：WAIT_SHELF/STORED/FROZEN等")
        private List<String> ngStatuses;

        @Schema(description = "库位关键词：库位编码/库位名称/仓库编码/仓库名称")
        private String locationKeyword;

        @Schema(description = "物料编码")
        private String materialCode;

        @Schema(description = "型号")
        private String modelNo;

        @Schema(description = "尺寸规格")
        private String specSize;

        @Schema(description = "质量状态")
        private String qualityStatus;

        @Schema(description = "扫码确认日期，格式 yyyy-MM-dd")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        private LocalDate reportDate;

        @Schema(description = "关键词：片号/来源计划/来源母批/物料/型号")
        private String keyword;

        @Schema(description = "返回行数上限")
        private Integer limit;
    }

    @Schema(description = "管理后台 - 离散后加工任务查询 Request VO")
    @Data
    public static class TaskQueryReqVO {

        @Schema(description = "目标工序编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "目标工序编码不能为空")
        private String opCode;

        @Schema(description = "任务状态：ALL/PENDING/COMPLETED")
        private String taskStatus;

        @Schema(description = "关键词：计划号/片号/来源计划/物料/型号")
        private String keyword;
    }

    @Schema(description = "管理后台 - 离散后加工报工 Request VO")
    @Data
    public static class ReportReqVO {

        @Schema(description = "计划工序ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "计划工序ID不能为空")
        private Long planOperationId;

        @Schema(description = "来源锁定明细ID")
        private Long lockId;

        @Schema(description = "扫码片号；未传锁定明细ID时按片号匹配")
        private String pieceNo;

        @Schema(description = "报工结果：OK/NG")
        private String reportResult;

        @Schema(description = "自检结果")
        private String selfCheck;

        @Schema(description = "不良代码")
        private String defectCode;

        @Schema(description = "投入数量")
        private BigDecimal inputQty;

        @Schema(description = "产出数量")
        private BigDecimal outputQty;

        @Schema(description = "损耗数量")
        private BigDecimal lossQty;

        @Schema(description = "粘胶2实际尺寸规则")
        private String actualSizeRule;

        @Schema(description = "粘胶2实际片号尾号")
        private String actualSizeSuffix;

        @Schema(description = "记录人")
        private String recorderName;

        @Schema(description = "确认人")
        private String confirmerName;

        @Schema(description = "备注")
        private String remark;
    }

    @Schema(description = "管理后台 - 离散后加工送检 Request VO")
    @Data
    public static class InspectionReqVO {

        @Schema(description = "来源锁定明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "来源锁定明细ID不能为空")
        private Long lockId;

        @Schema(description = "送检类型")
        private String inspectionType;

        @Schema(description = "送检备注")
        private String remark;
    }

    @Schema(description = "管理后台 - 离散后加工任务 Response VO")
    @Data
    public static class TaskRespVO {
        private Long planId;
        private String planNo;
        private String planStatus;
        private String planMode;
        private Long planOperationId;
        private Integer opSeq;
        private String opCode;
        private String opName;
        private Long workCenterId;
        private String workCenterCode;
        private String workCenterName;
        private Long equipmentId;
        private String equipmentCode;
        private String equipmentName;
        private String operationStatus;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String sizeSpec;
        private BigDecimal requiredQty;
        private Integer sourceCount;
        private Integer pendingCount;
        private Integer finishedCount;
        private Integer releasedCount;
        private String firstSourceBatchNo;
        private String sourcePlanNos;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastReportTime;
    }

    @Schema(description = "管理后台 - 离散后加工来源库存 Response VO")
    @Data
    public static class StockRespVO {
        private String candidateKey;
        private String candidateType;
        private String candidateStatus;
        private Long sourceLockId;
        private Long sourceLockPlanId;
        private String sourceLockPlanNo;
        private Long sourceLockOperationId;
        private Long stockId;
        private Long ngPieceId;
        private String stockType;
        private String sourceType;
        private String sourceTable;
        private Long sourceId;
        private Long sourceReportId;
        private Long sourcePlanId;
        private String sourcePlanNo;
        private Long sourcePlanOperationId;
        private String sourceBatchNo;
        private String sourceParentBatchNo;
        private String batchNo;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String recipeCode;
        private String recipeName;
        private String modelNo;
        private String specSize;
        private Integer opSeq;
        private String opCode;
        private String opName;
        private String segmentCode;
        private String segmentName;
        private BigDecimal thickness;
        private BigDecimal onHandQty;
        private BigDecimal availableQty;
        private BigDecimal shareableQty;
        private BigDecimal frozenQty;
        private BigDecimal planLockedQty;
        private String qualityStatus;
        private String bizStatus;
        private String ngStatus;
        private String ngStatusText;
        private String entryReason;
        private String qualityResult;
        private String uom;
        private String locationCode;
        private String locationName;
        private String currentWarehouseCode;
        private String currentWarehouseName;
        private String currentLocationCode;
        private String currentLocationName;
        private Long freezeInstructionId;
        private String freezeInstructionNo;
        private LocalDate productionDate;
        private LocalDate expiryDate;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastTxnTime;
        @Schema(description = "上游扫码确认/报工时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime reportTime;
    }

    @Schema(description = "管理后台 - 离散后加工片号任务 Response VO")
    @Data
    public static class SourceRespVO {
        private Long lockId;
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String targetOpCode;
        private String targetOpName;
        private Long stockId;
        private Long ngPieceId;
        private String stockType;
        private String sourceType;
        private String sourceTable;
        private Long sourceId;
        private Long sourceReportId;
        private Long sourcePlanId;
        private String sourcePlanNo;
        private Long sourcePlanOperationId;
        private String sourceBatchNo;
        private String sourceParentBatchNo;
        private String batchNo;
        private String pieceNo;
        private Long materialId;
        private String materialCode;
        private String materialName;
        private String modelNo;
        private String recipeCode;
        private String sizeSpec;
        private String opCode;
        private String opName;
        private BigDecimal lockQty;
        private BigDecimal consumedQty;
        private BigDecimal releasedQty;
        private BigDecimal remainingQty;
        private String lockStatus;
        private String ngStatus;
        private String ngStatusText;
        private String currentWarehouseCode;
        private String currentWarehouseName;
        private String currentLocationCode;
        private String currentLocationName;
        private Long consumeReportId;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime consumeTime;
        private String consumeTxnNo;
        private Long outputStockId;
        private String outputStockBatchNo;
        private String outputStockPostStatus;
        private Long inspectionTaskId;
        private String inspectionTaskNo;
        private String inspectionType;
        private String inspectionStatus;
        private String inspectionResult;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionReportTime;
        private String inspectionReporterName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;
        private String remark;
    }

    @Schema(description = "管理后台 - 离散后加工送检 Response VO")
    @Data
    public static class InspectionRespVO {
        private Long id;
        private String inspectionTaskNo;
        private String inspectionStatus;
    }
}
