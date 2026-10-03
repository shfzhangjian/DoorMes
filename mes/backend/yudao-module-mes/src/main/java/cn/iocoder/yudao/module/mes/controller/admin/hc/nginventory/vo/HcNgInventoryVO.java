package cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

/** 分切、压槽不合格品库存接口 VO。 */
public final class HcNgInventoryVO {

    private HcNgInventoryVO() {
    }

    @Data
    @Schema(description = "管理后台 - NG 库存片分页 Request VO")
    public static class NgPiecePageReqVO extends PageParam {
        /** 不合格品查询：排除合格冻结片，历史空质量沿用既有 NG 口径；限定当前未处置状态。 */
        private Boolean unqualifiedOnly;
        private String processType;
        private String status;
        /** 当前仓库编码。 */
        private String warehouseCode;
        private String locationKey;
        private String keyword;
        /** 产品型号，支持模糊匹配。 */
        private String modelNo;
        /** 物料编码，支持模糊匹配。 */
        private String materialCode;
        private String padType;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate shelvedDateStart;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate shelvedDateEnd;
        /** 段批次号：优先来源母批，空时回退来源批号。 */
        private String segmentBatchNo;
        private Long sourcePlanOperationId;
        private Long freezeInstructionId;
        private Boolean includeScrapped;
    }

    @Data
    @Schema(description = "管理后台 - NG 库存片 Response VO")
    public static class NgPieceRespVO {
        private Long id;
        private String sourceType;
        private String processType;
        private String processName;
        private String pieceNo;
        private String sourcePlanNo;
        private Long sourcePlanId;
        private Long sourcePlanOperationId;
        private String sourceBatchNo;
        private String sourceParentBatchNo;
        private String materialCode;
        private String materialName;
        private String modelNo;
        private String padType;
        private BigDecimal pieceQty;
        private String entryReason;
        private String qualityResult;
        private String defectSummary;
        private String defectDetailJson;
        private String status;
        private String currentWarehouseName;
        /** 当前库位的系统内部唯一键，不在页面展示。 */
        private String currentLocationKey;
        private String currentLocationCode;
        private String currentLocationName;
        private String originalLocationName;
        private Long freezeInstructionId;
        private String freezeInstructionNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime freezeEffectiveTime;
        private Long unfreezeInstructionId;
        private String scrapReason;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shelvedTime;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime scrappedTime;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastPrintTime;
    }

    @Data
    @Schema(description = "管理后台 - NG 历史台账分页 Request VO")
    public static class NgHistoryLedgerPageReqVO extends PageParam {
        /** 片号、来源批号、母批/段批次或来源计划号关键词。 */
        private String keyword;
        private String warehouseCode;
        private String processType;
        private String txnType;
        /** 当前逐片状态；用于定位已报废、待上架等最终状态。 */
        private String currentStatus;
        private String modelNo;
        private String materialCode;
        private String padType;
        /** 仅保留当前已离开 NG 货架或已关闭的逐片记录。 */
        private Boolean archivedOnly;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime txnTimeStart;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime txnTimeEnd;
    }

    @Data
    @Schema(description = "管理后台 - NG 历史台账 Response VO")
    public static class NgHistoryLedgerRespVO {
        private Long id;
        private String txnNo;
        private String txnType;
        /** 由后端按交易类型映射的中文操作名称。 */
        private String operationName;
        /** IN/OUT；移库分别保留移出、移入两条事实记录。 */
        private String txnDirection;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime txnTime;
        private String warehouseCode;
        private String warehouseName;
        /** 本次操作发生时的货架/库位，不表示当前库位。 */
        private String locationCode;
        private String pieceNo;
        private String sourcePlanNo;
        private String sourceBatchNo;
        private String sourceParentBatchNo;
        private String processType;
        private String processName;
        private String materialCode;
        private String materialName;
        private String modelNo;
        private String padType;
        private BigDecimal txnQty;
        private String uom;
        /** 当前逐片最终状态，不表示本次操作发生当时的状态。 */
        private String currentStatus;
        private String scrapReason;
        private String creatorName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 不合格品出入库记录分页 Request VO")
    public static class UnqualifiedHistoryLedgerPageReqVO extends PageParam {
        /** 片号、批次、计划、单据或包装编号关键词。 */
        private String keyword;
        /** NG_WAREHOUSE / WAIT_PACKAGING / FG_WAREHOUSE。 */
        private String inventoryArea;
        private String eventType;
        /** IN / OUT / NONE / UNKNOWN；留空查询全部。 */
        private String inventoryDirection;
        private String afterStatus;
        private String qualityStatus;
        private String materialCode;
        private String modelCode;
        private String sliceBatchNo;
        private String segmentBatchNo;
        private String planNo;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime eventTimeStart;
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime eventTimeEnd;
    }

    @Data
    @Schema(description = "管理后台 - 不合格品出入库记录 Response VO")
    public static class UnqualifiedHistoryLedgerRespVO {
        private String id;
        private String inventoryArea;
        private String eventType;
        /** IN 入库 / OUT 出库 / NONE 非出入库操作 / UNKNOWN 待识别。 */
        private String inventoryDirection;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime eventTime;
        private String sliceBatchNo;
        private String segmentBatchNo;
        private String planNo;
        private String processName;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private BigDecimal qty;
        private String fqcResult;
        private String coaResult;
        private String qualityStatus;
        private String warehouseCode;
        private String warehouseName;
        private String locationCode;
        private String afterStatus;
        private String operatorName;
        private String refDocNo;
        private String txnNo;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - NG 逐片标签批量查询 Request VO")
    public static class NgPieceLabelBatchQueryReqVO {
        @NotEmpty(message = "请至少选择一片不合格品")
        @Size(max = 50, message = "单次最多查询50片不合格品")
        private List<@NotNull(message = "不合格品逐片ID不能为空") Long> pieceIds;
    }

    @Data
    @Schema(description = "管理后台 - NG 逐片标签批量查询 Response VO")
    public static class NgPieceLabelBatchQueryRespVO {
        private List<NgPieceLabelRespVO> labels = new ArrayList<>();
        private List<String> failures = new ArrayList<>();
    }

    @Data
    @Schema(description = "管理后台 - NG 逐片标签打印成功回写 Request VO")
    public static class NgPieceLabelPrintedReqVO {
        @NotNull(message = "不合格品逐片ID不能为空")
        private Long pieceId;
        @Size(max = 64, message = "打印机名称不能超过64个字符")
        private String printerName;
        private String labelContentJson;
    }

    @Data
    @Schema(description = "管理后台 - NG 逐片标签批量打印成功回写 Request VO")
    public static class NgPieceLabelBatchPrintedReqVO {
        @Valid
        @NotEmpty(message = "请至少提交一条打印成功记录")
        @Size(max = 50, message = "单次最多回写50条打印记录")
        private List<NgPieceLabelPrintedReqVO> items;
    }

    @Data
    @Schema(description = "管理后台 - NG 逐片工艺流转单标签 Response VO")
    public static class NgPieceLabelRespVO {
        private Long pieceId;
        private String sourceType;
        private Long sourceId;
        private Long planId;
        private Long planOperationId;
        private String planNo;
        private String materialCode;
        private String modelCode;
        private String segmentBatchNo;
        private String pieceNo;
        private String processName;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime workTime;
        private String recordStatus;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastPrintTime;
    }

    @Data
    @Schema(description = "管理后台 - NG 段批次分组 Response VO")
    public static class NgPieceSegmentRespVO {
        private String segmentBatchNo;
        private String processType;
        private String processName;
        private Long sourcePlanId;
        private String sourcePlanNo;
        private Long sourcePlanOperationId;
        private String materialCode;
        private String materialName;
        private String modelNo;
        private Integer totalPieceCount;
        private Integer ngPieceCount;
        private Integer okPieceCount;
        private List<String> samplePieceNos;
        private Long freezeInstructionId;
        private String freezeInstructionNo;
    }

    @Data
    @Schema(description = "管理后台 - NG 精确库位格 Response VO")
    public static class NgLocationGridRespVO {
        private Long id;
        /** 系统内部库位唯一键，不在页面展示。 */
        private String locationKey;
        private String locationCode;
        private String locationName;
        private String warehouseName;
        private String warehouseCode;
        private String padType;
        private Long warehouseId;
        private Long rackId;
        private String rackNo;
        private String storagePurpose;
        private Integer locationNo;
        private Integer capacityQty;
        private Integer occupiedQty;
        private Integer availableQty;
        private String status;
        private List<String> pieceNos;
    }

    @Data
    @Schema(description = "管理后台 - NG 线边仓库位库存报表 Request VO")
    public static class NgLocationStockReportReqVO {
        /** BLACK_PAD/WHITE_PAD；为空时查询全部垫型。 */
        private String padType;
        /** SLITTING_NG/PRESS_SLOT_NG/FREEZE；为空时查询全部库位用途。 */
        private String storagePurpose;
        /** 产品型号，支持模糊匹配。 */
        private String modelNo;
        /** 仓库、货架或库位编码关键词，支持模糊匹配。 */
        private String locationKeyword;
    }

    @Data
    @Schema(description = "管理后台 - NG 线边仓库位库存报表 Response VO")
    public static class NgLocationStockReportRespVO {
        private String padType;
        private String warehouseCode;
        private String warehouseName;
        private Integer rackNo;
        private Integer locationNo;
        private String storagePurpose;
        private String locationCode;
        private String locationName;
        private String modelNo;
        private String materialCode;
        private String materialName;
        private String stockStatus;
        private BigDecimal quantity;
        private Integer pieceCount;
    }

    @Data
    @Schema(description = "管理后台 - NG 仓库保存 Request VO")
    public static class NgWarehouseSaveReqVO {
        private Long id;
        @NotBlank(message = "仓库编码不能为空")
        private String warehouseCode;
        @NotBlank(message = "仓库名称不能为空")
        private String warehouseName;
        @NotBlank(message = "请选择垫型")
        private String padType;
        private String status;
        private Integer sortNo;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - NG 货架保存 Request VO")
    public static class NgRackSaveReqVO {
        private Long id;
        @NotNull(message = "请选择所属仓库")
        private Long warehouseId;
        @NotNull(message = "货架编号不能为空")
        private Integer rackNo;
        @NotBlank(message = "货架编码不能为空")
        private String rackCode;
        @NotBlank(message = "货架名称不能为空")
        private String rackName;
        @NotNull(message = "货架容量不能为空")
        private Integer capacityQty;
        private String status;
        private Integer sortNo;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - NG 仓库货架容量树 Response VO")
    public static class NgLocationTreeWarehouseRespVO {
        private Long id;
        private String warehouseCode;
        private String warehouseName;
        private String padType;
        private String status;
        private Integer sortNo;
        private String remark;
        private List<NgLocationTreeRackRespVO> racks;
    }

    @Data
    @Schema(description = "管理后台 - NG 货架容量 Response VO")
    public static class NgLocationTreeRackRespVO {
        private Long id;
        private Long warehouseId;
        private Integer rackNo;
        private String rackCode;
        private String rackName;
        private String status;
        private Integer sortNo;
        private String remark;
        /** 货架容量；库存内部定位键不在页面展示。 */
        private Integer capacityQty;
        private Integer occupiedQty;
        private Integer availableQty;
        private String storageKey;
        private List<NgLocationGridRespVO> locations;
    }

    @Data
    @Schema(description = "管理后台 - NG 上架 Request VO")
    public static class NgShelfReqVO {
        @NotEmpty(message = "请选择待上架不合格品")
        private List<Long> pieceIds;
        @NotBlank(message = "请选择目标货架")
        private String locationKey;
    }

    @Data
    @Schema(description = "管理后台 - NG 不合格品下架 Request VO")
    public static class NgUnshelfReqVO {
        @NotEmpty(message = "请选择待下架不合格品")
        private List<Long> pieceIds;
        @NotBlank(message = "请填写下架原因")
        private String unshelfReason;
    }

    @Data
    @Schema(description = "管理后台 - NG 不合格品批量出库 Request VO")
    public static class NgManualOutboundReqVO {
        @NotEmpty(message = "请选择待出库不合格品")
        private List<Long> pieceIds;
        @NotBlank(message = "请填写出库原因")
        @Size(max = 500, message = "出库原因不能超过500个字符")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - NG 不合格品移库 Request VO")
    public static class NgTransferReqVO {
        @NotEmpty(message = "请选择待移库不合格品")
        private List<Long> pieceIds;
        @NotBlank(message = "请选择目标货架")
        private String targetLocationKey;
    }

    @Data
    @Schema(description = "管理后台 - NG 报废 Request VO")
    public static class NgScrapReqVO {
        @NotEmpty(message = "请选择待报废不合格品")
        private List<Long> pieceIds;
        @NotBlank(message = "请填写报废原因")
        private String scrapReason;
    }

    @Data
    @Schema(description = "管理后台 - 新增分切压槽不合格品历史片 Request VO")
    public static class NgManualPieceCreateReqVO {
        @NotBlank(message = "段批次不能为空")
        private String segmentBatchNo;
        @NotBlank(message = "片号不能为空")
        private String pieceNo;
        @NotBlank(message = "工序不能为空")
        private String processType;
        @NotBlank(message = "来源批号不能为空")
        private String sourceBatchNo;
        @Schema(description = "料号；历史补录可不填写")
        private String materialCode;
        @NotBlank(message = "型号不能为空")
        private String modelNo;
        @NotBlank(message = "垫型不能为空")
        private String padType;
        @NotBlank(message = "入库类型不能为空")
        private String storageTarget;
        @NotBlank(message = "NG原因不能为空")
        private String defectSummary;
        @NotBlank(message = "补录原因不能为空")
        private String backfillReason;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 编辑未上架分切压槽不合格品历史片 Request VO")
    public static class NgManualPieceUpdateReqVO {
        @NotNull(message = "历史片ID不能为空")
        private Long pieceId;
        @NotBlank(message = "段批次不能为空")
        private String segmentBatchNo;
        @NotBlank(message = "片号不能为空")
        private String pieceNo;
        @NotBlank(message = "工序不能为空")
        private String processType;
        @NotBlank(message = "来源批号不能为空")
        private String sourceBatchNo;
        @Schema(description = "料号；历史补录可不填写")
        private String materialCode;
        @NotBlank(message = "型号不能为空")
        private String modelNo;
        @NotBlank(message = "垫型不能为空")
        private String padType;
        @NotBlank(message = "入库类型不能为空")
        private String storageTarget;
        @NotBlank(message = "NG原因不能为空")
        private String defectSummary;
    }

    @Data
    @Schema(description = "管理后台 - 删除未上架历史不合格品 Request VO")
    public static class NgManualPieceDeleteReqVO {
        @NotNull(message = "历史片ID不能为空")
        private Long pieceId;
        @NotBlank(message = "删除原因不能为空")
        private String deleteReason;
    }

    @Data
    @Schema(description = "管理后台 - 人工解除并关闭历史冻结片 Request VO")
    public static class NgManualPieceUnfreezeReqVO {
        @NotNull(message = "历史冻结片ID不能为空")
        private Long pieceId;
        @NotBlank(message = "解除冻结原因不能为空")
        private String unfreezeReason;
    }

    @Data
    @Schema(description = "管理后台 - 历史不合格品 Excel 导入结果 Response VO")
    public static class NgManualPieceImportRespVO {
        private Integer totalRows = 0;
        private Integer skippedRows = 0;
        private Integer successCount = 0;
        private Integer failureCount = 0;
        private List<String> failures = new ArrayList<>();
        private List<String> messages = new ArrayList<>();
    }

}
