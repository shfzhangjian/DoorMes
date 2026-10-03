package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

public class HcFinishedPackagingVO {

    @Data
    @Schema(description = "管理后台 - 包装箱台账查询 Request VO")
    public static class PackageBoxQueryReqVO {
        @Schema(description = "包装箱类型：INBOUND/OUTBOUND")
        private String boxType;
        @Schema(description = "关键字：包装箱号/单号/计划号/批号/型号/料号")
        private String keyword;
    }

    @Data
    @Schema(description = "管理后台 - 包装箱台账 Response VO")
    public static class PackageBoxRespVO {
        private Long id;
        private String boxType;
        private String boxTypeName;
        private String boxNo;
        private String bizNo;
        private String sourceNo;
        private String motherSegmentBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private Integer targetQty;
        private Integer currentQty;
        private String status;
        private String labelNo;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastPrintTime;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recorderTime;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装箱台账打印 Request VO")
    public static class PackageBoxPrintReqVO {
        @NotBlank(message = "包装箱类型不能为空")
        private String boxType;
        @NotNull(message = "包装箱ID不能为空")
        private Long id;
        private String operatorName;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库库位九宫格 Response VO")
    public static class FgLocationGridRespVO {
        private Long id;
        private Long warehouseId;
        private Long rackId;
        private Integer rackNo;
        private String rackName;
        private Long layerId;
        private Integer layerNo;
        private String layerName;
        private Integer areaNo;
        private Integer gridNo;
        private String locationCode;
        private String locationName;
        private String warehouseCode;
        private String warehouseName;
        private String locationType;
        @Schema(description = "包装成品库质量用途：QUALIFIED/QUARANTINE/UNASSIGNED")
        private String qualityScope;
        private String positionDesc;
        private Boolean mixBatchFlag;
        private Boolean mixModelFlag;
        private Integer capacityQty;
        private Integer occupiedQty;
        private Integer occupiedPieceQty;
        private Integer availableQty;
        private Boolean coaFrozen;
        private String coaFreezeReason;
        private Integer frozenQty;
        private String status;
        private String qrCode;
        private Boolean occupied;
        private String occupiedSliceBatchNo;
        private List<String> occupiedSliceBatchNos;
        private List<FgLocationPieceRespVO> pieces;
        private String occupiedStockNo;
        private String occupiedInnerUnitNo;
        private Boolean deletable;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库库位片位 Response VO")
    public static class FgLocationPieceRespVO {
        private Long stockId;
        private String stockNo;
        private String sliceBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String batchNo;
        private String productSize;
        private Integer qty;
        private String qualityStatus;
        private String stockStatus;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inboundTime;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库库位保存 Request VO")
    public static class FgLocationSaveReqVO {
        private Long id;
        @NotNull(message = "货架不能为空")
        private Long rackId;
        @NotNull(message = "层不能为空")
        private Long layerId;
        @NotNull(message = "区域不能为空")
        private Integer areaNo;
        @Schema(description = "库位名称；为空时由系统按货架、层、区生成")
        private String locationName;
        private String positionDesc;
        @NotNull(message = "库容量不能为空")
        private Integer capacityQty;
        @Schema(description = "包装成品库质量用途：QUALIFIED 合格品库位、QUARANTINE 不合格品隔离库位")
        private String qualityScope;
        private String status;
        private Boolean mixBatchFlag;
        private Boolean mixModelFlag;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库货架保存 Request VO")
    public static class FgRackSaveReqVO {
        private Long id;
        @NotNull(message = "仓库不能为空")
        private Long warehouseId;
        @NotNull(message = "货架编号不能为空")
        @Min(value = 1, message = "货架编号必须大于0")
        private Integer rackNo;
        @NotBlank(message = "货架名称不能为空")
        @Size(max = 100, message = "货架名称不能超过100个字符")
        private String rackName;
        private String status;
        private Integer sortNo;
        @Size(max = 200, message = "备注不能超过200个字符")
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库货架层保存 Request VO")
    public static class FgLayerSaveReqVO {
        private Long id;
        @NotNull(message = "货架不能为空")
        private Long rackId;
        @NotNull(message = "层号不能为空")
        @Min(value = 1, message = "层号必须大于0")
        private Integer layerNo;
        @NotBlank(message = "层名称不能为空")
        @Size(max = 100, message = "层名称不能超过100个字符")
        private String layerName;
        private String status;
        private Integer sortNo;
        @Size(max = 200, message = "备注不能超过200个字符")
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品仓库保存 Request VO")
    public static class FgWarehouseSaveReqVO {
        private Long id;
        @NotBlank(message = "仓库编码不能为空")
        @Size(max = 32, message = "仓库编码不能超过32个字符")
        private String warehouseCode;
        @NotBlank(message = "仓库名称不能为空")
        @Size(max = 100, message = "仓库名称不能超过100个字符")
        private String warehouseName;
        private String status;
        private Integer sortNo;
        @Size(max = 200, message = "备注不能超过200个字符")
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品仓库质量用途批量设置 Request VO")
    public static class FgWarehouseQualityScopeUpdateReqVO {
        @NotNull(message = "仓库不能为空")
        private Long warehouseId;
        @NotBlank(message = "质量用途不能为空")
        @Schema(description = "仓库质量用途：QUALIFIED 合格品仓、QUARANTINE 不合格品隔离仓")
        private String qualityScope;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品仓库质量用途批量设置 Response VO")
    public static class FgWarehouseQualityScopeUpdateRespVO {
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private String qualityScope;
        private Integer locationCount;
        private Integer occupiedLocationCount;
        private Long activePieceCount;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库仓库树 Response VO")
    public static class FgLocationTreeWarehouseRespVO {
        private Long id;
        private String warehouseCode;
        private String warehouseName;
        private String status;
        private Integer sortNo;
        private String remark;
        private List<FgLocationTreeRackRespVO> racks;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库货架层树 Response VO")
    public static class FgLocationTreeRackRespVO {
        private Long id;
        private Long warehouseId;
        private String warehouseCode;
        private String warehouseName;
        private Integer rackNo;
        private String rackName;
        private String status;
        private Integer sortNo;
        private String remark;
        private List<FgLocationTreeLayerRespVO> layers;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库货架层树层 Response VO")
    public static class FgLocationTreeLayerRespVO {
        private Long id;
        private Long rackId;
        private Integer layerNo;
        private String layerName;
        private String status;
        private Integer sortNo;
        private String remark;
        private List<FgLocationGridRespVO> areas;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库库位打印 Request VO")
    public static class FgLocationPrintReqVO {
        @NotNull(message = "库位ID不能为空")
        private Long id;
    }

    @Data
    @Schema(description = "管理后台 - 包装辅材边库分页 Request VO")
    public static class PackageAuxStockPageReqVO extends PageParam {
        @Schema(description = "辅材分类")
        private String auxCategory;
        @Schema(description = "辅材料号")
        private String materialCode;
        @Schema(description = "辅材名称")
        private String materialName;
        @Schema(description = "辅材规格")
        private String auxSpec;
        @Schema(description = "辅材批次")
        private String batchNo;
        @Schema(description = "库存状态")
        private String stockStatus;
        @Schema(description = "领料日期")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate receiveDate;
    }

    @Data
    @Schema(description = "管理后台 - 包装辅材边库保存 Request VO")
    public static class PackageAuxStockSaveReqVO {
        private Long id;
        private String auxCategory;
        private String auxCategoryName;
        @NotBlank(message = "辅材料号不能为空")
        private String materialCode;
        @NotBlank(message = "辅材名称不能为空")
        private String materialName;
        private String auxSpec;
        @NotBlank(message = "辅材批次不能为空")
        private String batchNo;
        private String sourceWarehouseCode;
        private String sourceWarehouseName;
        private String edgeWarehouseCode;
        private String edgeWarehouseName;
        @NotNull(message = "入边库数量不能为空")
        private BigDecimal receiveQty;
        private String receiverName;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate receiveDate;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        private LocalDateTime receiveTime;
        private String erpTransferNo;
        private BigDecimal transferQty;
        private String transferUnit;
        private BigDecimal unpackQty;
        private String unpackUnit;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装辅材边库 Response VO")
    public static class PackageAuxStockRespVO {
        private Long id;
        private String auxCategory;
        private String auxCategoryName;
        private String materialCode;
        private String materialName;
        private String auxSpec;
        private String batchNo;
        private String sourceWarehouseCode;
        private String sourceWarehouseName;
        private String edgeWarehouseCode;
        private String edgeWarehouseName;
        private String stockMeasureMode;
        private BigDecimal receiveQty;
        private BigDecimal usedQty;
        private BigDecimal availableQty;
        private String stockStatus;
        private String receiverName;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate receiveDate;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime receiveTime;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime printTime;
        private String erpTransferNo;
        private BigDecimal transferQty;
        private String transferUnit;
        private BigDecimal unpackQty;
        private String unpackUnit;
        private String erpTransferStatus;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装辅材消耗登记 Request VO")
    public static class PackageAuxConsumeReqVO {
        private Long stockId;
        private String batchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String auxSpec;
        private String bizType;
        private String bizNo;
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate recordDate;
        @NotNull(message = "消耗数量不能为空")
        private BigDecimal consumeQty;
        private String recorderName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装辅材批次领用明细 Request VO")
    public static class PackageAuxConsumeItemReqVO {
        @NotNull(message = "包装工序耗材领用台账不能为空")
        private Long ledgerId;
        @Schema(description = "操作员填写的包装辅材本次领用量")
        @NotNull(message = "包装辅材本次领用量不能为空")
        @DecimalMin(value = "0", inclusive = false, message = "包装辅材本次领用量必须大于0")
        private BigDecimal consumeQty;
    }

    @Data
    @Schema(description = "管理后台 - 包装辅材当日消耗 Response VO")
    public static class PackageAuxConsumeRecordRespVO {
        private Long id;
        private Long stockId;
        private String auxCategory;
        private String auxCategoryName;
        private String materialCode;
        private String materialName;
        private String auxSpec;
        private String batchNo;
        private String bizType;
        private String bizNo;
        private BigDecimal consumeQty;
        private BigDecimal beforeAvailableQty;
        private BigDecimal afterAvailableQty;
        private String consumeStatus;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate recordDate;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recorderTime;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 包装辅材当日消耗汇总 Response VO")
    public static class PackageAuxConsumeSummaryRespVO {
        private String auxCategory;
        private String auxCategoryName;
        private String materialCode;
        private String materialName;
        private String auxSpec;
        private String batchNo;
        private BigDecimal totalConsumeQty;
        private Integer recordCount;
    }

    @Data
    @Schema(description = "管理后台 - 成品库存台账分页 Request VO")
    public static class FgStockLedgerPageReqVO extends PageParam {
        @Schema(description = "关键词")
        private String keyword;
        @Schema(description = "库位编号")
        private String locationCode;
        @Schema(description = "包装编号")
        private String packageNo;
        @Schema(description = "片号")
        private String sliceBatchNo;
        @Schema(description = "片号前缀")
        private String sliceBatchNoPrefix;
        @Schema(description = "分段批次")
        private String batchNo;
        @Schema(description = "料号")
        private String materialCode;
        @Schema(description = "产品型号")
        private String modelCode;
        @Schema(description = "检验结果/质量状态")
        private String qualityStatus;
        @Schema(description = "库存状态")
        private String stockStatus;
        @Schema(description = "所属库位关键字")
        private String locationKeyword;
        @Schema(description = "生产日期起")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateStart;
        @Schema(description = "生产日期止")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateEnd;
        @Schema(description = "入库日期起")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate inboundDateStart;
        @Schema(description = "入库日期止")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate inboundDateEnd;
    }

    @Data
    @Schema(description = "管理后台 - 成品库存出入库记录分页 Request VO")
    public static class FgStockHistoryLedgerPageReqVO extends PageParam {
        @Schema(description = "关键词：库存号/包装编号/片号/批次/关联单据")
        private String keyword;
        @Schema(description = "操作类型")
        private String txnType;
        @Schema(description = "操作后库存状态")
        private String afterStockStatus;
        @Schema(description = "检验结果/质量状态")
        private String qualityStatus;
        @Schema(description = "操作仓库编码")
        private String warehouseCode;
        @Schema(description = "操作库位编码")
        private String locationCode;
        @Schema(description = "料号")
        private String materialCode;
        @Schema(description = "型号")
        private String modelCode;
        @Schema(description = "分段批号")
        private String batchNo;
        @Schema(description = "片号")
        private String sliceBatchNo;
        @Schema(description = "操作时间起")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime txnTimeStart;
        @Schema(description = "操作时间止")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime txnTimeEnd;
    }

    @Data
    @Schema(description = "管理后台 - 成品库存库位总览 Response VO")
    public static class FgStockLocationOverviewRespVO {
        private Long id;
        private Integer gridNo;
        private String locationCode;
        private String locationName;
        private String warehouseCode;
        private String warehouseName;
        private String positionDesc;
        private Integer capacityQty;
        private Integer occupiedQty;
        private Integer availableQty;
        private Boolean coaFrozen;
        private String coaFreezeReason;
        private Integer frozenQty;
        private Integer packageCount;
        private Integer pieceCount;
        private Integer percent;
        private String status;
    }

    @Data
    @Schema(description = "管理后台 - 成品库存库位详情 Response VO")
    public static class FgStockLocationDetailRespVO {
        private FgStockLocationOverviewRespVO location;
        private List<InboundBoxRespVO> packages;
        private List<FgStockLedgerRespVO> pieces;
    }

    @Data
    @Schema(description = "管理后台 - 成品库存片台账 Response VO")
    public static class FgStockLedgerRespVO {
        @Schema(description = "配货候选来源：WAREHOUSE_STOCK/PACKAGING_DIRECT")
        private String candidateType;
        @Schema(description = "配货候选唯一键")
        private String candidateKey;
        @Schema(description = "来源内包装明细ID，仅包装直发候选返回")
        private Long sourceInnerPackItemId;
        @Schema(description = "来源裁切报工ID，仅待包装片直发候选返回")
        private Long sourceCutRoundReportId;
        @Schema(description = "库存包装来源类型：CUT_ROUND_REPORT/MANUAL_HISTORY")
        private String sourceType;
        @Schema(description = "历史导入来源片ID；非历史导入库存为空")
        private Long sourceManualPieceId;
        @Schema(description = "当前库存行是否可发起历史导入数据更正")
        private Boolean importedDataEditable;
        @Schema(description = "该片的历史标签是否需要按更正后的数据补打")
        private Boolean labelReprintRequired;
        private Long id;
        private String stockNo;
        private String packageNo;
        private String innerUnitNo;
        private String sliceBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String batchNo;
        private String inspectionTaskNo;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        private String inspectionStatus;
        private String inspectionResult;
        private String coaInspectionResult;
        private String productSize;
        private Integer qty;
        private Integer lockedQty;
        private Integer availableQty;
        private Boolean coaFrozen;
        private String coaFreezeReason;
        private Integer frozenQty;
        private String qualityStatus;
        private String warehouseCode;
        private String warehouseName;
        private String locationCode;
        private String locationName;
        private String stockStatus;
        private String inboundNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inboundTime;
        private String inboundUserName;
        private String outboundLocation;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime outboundTime;
        private String outboundRecorderName;
        private String outboundQualityNo;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货配货候选分段批号分组 Response VO")
    public static class ShippingNoticePickCandidateSegmentRespVO {
        @Schema(description = "分段批号")
        private String segmentBatchNo;
        @Schema(description = "产品型号")
        private String modelCode;
        @Schema(description = "本段可配货片数")
        private Integer totalPieceCount;
        @Schema(description = "已上架库存片数")
        private Integer warehousePieceCount;
        @Schema(description = "包装直发片数")
        private Integer packagingDirectPieceCount;
        @Schema(description = "片号示例，最多3个")
        private List<String> sampleSliceBatchNos;
    }

    @Data
    @Schema(description = "管理后台 - 成品库存出入库记录 Response VO")
    public static class FgStockHistoryLedgerRespVO {
        private Long id;
        private Long finishedStockId;
        private String txnNo;
        private String txnType;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime txnTime;
        private String stockNo;
        private String outerBoxNo;
        private String innerUnitNo;
        private String sliceBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String batchNo;
        private Integer qty;
        private String qualityStatus;
        private String warehouseCode;
        private String warehouseName;
        private String locationCode;
        private String locationName;
        private String beforeStockStatus;
        private String afterStockStatus;
        private String refDocType;
        private Long refDocId;
        private String refDocNo;
        private String operatorName;
        private String remark;
        /**
         * 当前流水是否仍可将对应片号退回待重新包装。
         * 仅用于历史台账的行操作显隐，服务端执行时仍会在行锁内二次校验。
         */
        private Boolean manualOutboundRepackReturnable;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单内部编号候选分页 Request VO")
    public static class FgShippingBatchCandidatePageReqVO extends PageParam {
        @Schema(description = "关键词：型号/母批段/料号")
        private String keyword;
        @Schema(description = "产品型号")
        private String modelCode;
        @Schema(description = "产品料号")
        private String materialCode;
        @Schema(description = "内部编号（母批段号）")
        private String batchNo;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单内部编号候选 Response VO")
    public static class FgShippingBatchCandidateRespVO {
        private String modelCode;
        private String batchNo;
        private String materialCode;
        private String materialName;
        private Integer availableQty;
        private Boolean coaFrozen;
        private String coaFreezeReason;
        private Integer frozenQty;
        private Integer stockAvailableQty;
        private Integer packagingReadyQty;
        private Integer lockedQty;
        private Integer totalQty;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装入库待包装查询 Request VO")
    public static class InboundTaskQueryReqVO {
        @Schema(description = "关键字：计划号/分段批次号/型号/料号")
        private String keyword;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装入库待包装 Response VO")
    public static class InboundTaskRespVO {
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String motherSegmentBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private Integer totalPieceCount;
        private Integer packedPieceCount;
        private Integer waitPackPieceCount;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装确认 Request VO")
    public static class LockInboundPackageReqVO {
        @Schema(description = "单片包装编号；仅选择一片时允许手工填写，批量包装由系统生成连续编号")
        private String packageNo;
        @Schema(description = "兼容历史入库锁定请求，包装确认阶段不再使用库位")
        private String locationCode;
        private List<Long> cutRoundReportIds;
        private List<Long> manualPieceIds;
        private String operatorName;
        private String remark;
        @Valid
        @NotEmpty(message = "请至少选择一条包装辅材")
        private List<PackageAuxConsumeItemReqVO> auxConsumeItems;
    }

    @Data
    @Schema(description = "管理后台 - 成品批量单片包装 Response VO")
    public static class InboundPackageBatchRespVO {
        @Schema(description = "本次生成的单片包装数量")
        private Integer packageCount;
        @Schema(description = "本次成功包装的片数")
        private Integer pieceCount;
        @Schema(description = "本次生成的包装编号列表")
        private List<String> packageNos;
    }

    @Data
    @Schema(description = "管理后台 - 历史待包装片新增 Request VO")
    public static class PackagingManualPieceCreateReqVO {
        @NotBlank(message = "片号不能为空")
        @Size(max = 100, message = "片号不能超过100个字符")
        private String sliceBatchNo;
        @NotBlank(message = "分段批号不能为空")
        @Size(max = 100, message = "分段批号不能超过100个字符")
        private String segmentBatchNo;
        @NotBlank(message = "产品型号不能为空")
        @Size(max = 64, message = "产品型号不能超过64个字符")
        private String modelCode;
        @NotBlank(message = "产品料号不能为空")
        @Size(max = 64, message = "产品料号不能超过64个字符")
        private String materialCode;
        @NotNull(message = "生产日期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDate;
        @NotNull(message = "有效期不能为空")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        @NotBlank(message = "裁切FQC结果不能为空")
        @Size(max = 16, message = "裁切FQC结果不能超过16个字符")
        private String inspectionResult;
        @NotBlank(message = "COA送检结果不能为空")
        @Size(max = 16, message = "COA送检结果不能超过16个字符")
        private String coaInspectionResult;
        @NotBlank(message = "补录原因不能为空")
        @Size(max = 500, message = "补录原因不能超过500个字符")
        private String backfillReason;
        @Size(max = 64, message = "补录人不能超过64个字符")
        private String recorderName;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 历史待包装片删除 Request VO")
    public static class PackagingManualPieceDeleteReqVO {
        @NotNull(message = "历史补录片ID不能为空")
        private Long id;
        @NotBlank(message = "删除原因不能为空")
        @Size(max = 500, message = "删除原因不能超过500个字符")
        private String deleteReason;
    }

    @Data
    @Schema(description = "管理后台 - 已上架历史导入成品片数据更正 Request VO")
    public static class ImportedStockDataUpdateReqVO {
        @NotNull(message = "成品库存ID不能为空")
        private Long stockId;
        @NotBlank(message = "片号不能为空")
        @Size(max = 100, message = "片号不能超过100个字符")
        private String sliceBatchNo;
        @NotBlank(message = "分段批号不能为空")
        @Size(max = 100, message = "分段批号不能超过100个字符")
        private String segmentBatchNo;
        @NotBlank(message = "产品型号不能为空")
        @Size(max = 64, message = "产品型号不能超过64个字符")
        private String modelCode;
        @NotBlank(message = "产品料号不能为空")
        @Size(max = 64, message = "产品料号不能超过64个字符")
        private String materialCode;
        @NotNull(message = "生产日期不能为空")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDate;
        @NotBlank(message = "裁切FQC结果不能为空")
        @Size(max = 16, message = "裁切FQC结果不能超过16个字符")
        private String inspectionResult;
        @NotBlank(message = "COA送检结果不能为空")
        @Size(max = 16, message = "COA送检结果不能超过16个字符")
        private String coaInspectionResult;
        @Size(max = 500, message = "备注不能超过500个字符")
        private String remark;
        @Size(max = 500, message = "修改原因不能超过500个字符")
        private String correctionReason;
    }

    @Data
    @Schema(description = "管理后台 - 历史待包装片Excel导入 Response VO")
    public static class PackagingManualPieceImportRespVO {
        private Integer totalRows = 0;
        private Integer skippedRows = 0;
        private Integer successCount = 0;
        private Integer failureCount = 0;
        private List<String> messages = new java.util.ArrayList<>();
        private List<String> failures = new java.util.ArrayList<>();
    }

    @Data
    @Schema(description = "管理后台 - 可打印片号候选分页 Request VO")
    public static class PieceLabelCandidatePageReqVO extends PageParam {
        @Size(max = 100, message = "搜索片号不能超过100个字符")
        private String sliceBatchNo;
        @Size(max = 32, message = "片号来源类型不能超过32个字符")
        private String sourceType;
        @Size(max = 16, message = "打印状态不能超过16个字符")
        private String printStatus;
        @Size(max = 32, message = "业务状态不能超过32个字符")
        private String businessStatus;
    }

    @Data
    @Schema(description = "管理后台 - 片号标签批量查询 Request VO")
    public static class PieceLabelBatchQueryReqVO {
        @NotEmpty(message = "请至少输入一个片号")
        @Size(max = 50, message = "单次最多查询50个片号")
        private List<@NotBlank(message = "片号不能为空") @Size(max = 100, message = "片号不能超过100个字符") String> sliceBatchNos;
    }

    @Data
    @Schema(description = "管理后台 - 片号标签批量查询 Response VO")
    public static class PieceLabelBatchQueryRespVO {
        private List<PieceLabelRespVO> labels = new java.util.ArrayList<>();
        private List<String> failures = new java.util.ArrayList<>();
    }

    @Data
    @Schema(description = "管理后台 - 片号标签打印成功回写 Request VO")
    public static class PieceLabelPrintedReqVO {
        @NotBlank(message = "片号来源类型不能为空")
        @Size(max = 32, message = "片号来源类型不能超过32个字符")
        private String sourceType;
        @NotNull(message = "片号来源ID不能为空")
        private Long sourceId;
        @Size(max = 64, message = "打印机名称不能超过64个字符")
        private String printerName;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
        private String labelContentJson;
    }

    @Data
    @Schema(description = "管理后台 - 片号标签批量打印成功回写 Request VO")
    public static class PieceLabelBatchPrintedReqVO {
        @Valid
        @NotEmpty(message = "请至少提交一条打印成功记录")
        @Size(max = 50, message = "单次最多回写50条打印记录")
        private List<PieceLabelPrintedReqVO> items;
    }

    @Data
    @Schema(description = "管理后台 - 片号标签 Response VO")
    public static class PieceLabelRespVO {
        private String sourceType;
        private Long sourceId;
        private String sliceBatchNo;
        private String segmentBatchNo;
        private String planNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        private String inspectionResult;
        private String coaInspectionResult;
        @Schema(description = "包装片最终质量状态：OK/NG/FROZEN；库存优先、待上架包装次之、待包装按当前检验结论计算")
        private String qualityStatus;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime workTime;
        private String printStatus;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastPrintTime;
        private String recordStatus;
        private Boolean historical;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装入库确认 Request VO")
    public static class ConfirmInboundPackageReqVO {
        @NotBlank(message = "包装编号不能为空")
        private String packageNo;
        private String locationCode;
        private String operatorName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装批量上架 Request VO")
    public static class ConfirmInboundPackageBatchReqVO {
        @NotEmpty(message = "请至少选择一个待上架包装单")
        @Size(max = 100, message = "单次最多上架100个包装单")
        private List<@NotNull(message = "包装盒ID不能为空") Long> packageIds;
        @NotBlank(message = "库位不能为空")
        @Size(max = 100, message = "库位编码不能超过100个字符")
        private String locationCode;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
        @Size(max = 200, message = "备注不能超过200个字符")
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装盒初始化 Request VO")
    public static class InitInboundBoxesReqVO {
        @NotNull(message = "计划ID不能为空")
        private Long planId;
        @NotNull(message = "包装工序ID不能为空")
        private Long planOperationId;
        @NotBlank(message = "分段批次号不能为空")
        private String motherSegmentBatchNo;
        @NotNull(message = "包装盒数不能为空")
        private Integer boxCount;
        @NotNull(message = "一盒片数不能为空")
        private Integer packageSpec;
        private String recorderName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装扫码 Request VO")
    public static class ScanInboundPieceReqVO {
        @NotNull(message = "包装盒ID不能为空")
        private Long boxId;
        @NotBlank(message = "成品片号不能为空")
        private String sliceBatchNo;
        private String scanUserName;
    }

    @Data
    @Schema(description = "管理后台 - 包装盒动作 Request VO")
    public static class BoxActionReqVO {
        @NotNull(message = "包装盒ID不能为空")
        private Long id;
        private String operatorName;
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 待上架成品包装批量取消 Request VO")
    public static class CancelInboundPackageBatchReqVO {
        @NotEmpty(message = "请至少选择一个待上架包装单")
        @Size(max = 100, message = "单次最多取消100个包装单")
        private List<@NotNull(message = "包装盒ID不能为空") Long> packageIds;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
        @Size(max = 200, message = "取消原因不能超过200个字符")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 已上架成品包装手工出库 Request VO")
    public static class ManualOutboundPackageReqVO {
        @NotNull(message = "包装盒ID不能为空")
        private Long id;
        private String operatorName;
        @NotBlank(message = "出库原因不能为空")
        @Size(max = 200, message = "出库原因不能超过200个字符")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装批量下架 Request VO")
    public static class DownShelfInboundPackageBatchReqVO {
        @NotEmpty(message = "请至少选择一个已上架包装单")
        @Size(max = 100, message = "单次最多下架100个包装单")
        private List<@NotNull(message = "包装盒ID不能为空") Long> packageIds;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
        @Size(max = 200, message = "下架原因不能超过200个字符")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 已上架成品包装批量手工出库 Request VO")
    public static class ManualOutboundPackageBatchReqVO {
        @NotEmpty(message = "请至少选择一个已上架包装单")
        @Size(max = 100, message = "单次最多出库100个包装单")
        private List<@NotNull(message = "包装盒ID不能为空") Long> packageIds;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
        @NotBlank(message = "出库原因不能为空")
        @Size(max = 200, message = "出库原因不能超过200个字符")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 合格品待上架包装批量直接出库 Request VO")
    public static class DirectOutboundPendingPackageBatchReqVO {
        @NotEmpty(message = "请至少选择一个合格品待上架包装单")
        @Size(max = 100, message = "单次最多直接出库100个包装单")
        private List<@NotNull(message = "包装盒ID不能为空") Long> packageIds;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
        @NotBlank(message = "出库原因不能为空")
        @Size(max = 200, message = "出库原因不能超过200个字符")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装批量操作 Response VO")
    public static class InboundPackageBatchActionRespVO {
        private Integer packageCount;
        private List<String> packageNos = new java.util.ArrayList<>();
    }

    @Data
    @Schema(description = "管理后台 - 成品库存批量手工出库 Request VO")
    public static class ManualOutboundStockBatchReqVO {
        @NotEmpty(message = "请至少选择一片库存")
        @Size(max = 200, message = "单次最多出库200片库存")
        private List<@NotNull(message = "库存ID不能为空") Long> stockIds;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
        @NotBlank(message = "出库原因不能为空")
        @Size(max = 200, message = "出库原因不能超过200个字符")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 成品库存批量手工出库 Response VO")
    public static class ManualOutboundStockBatchRespVO {
        private Integer stockCount;
        private Integer packageCount;
        private List<String> sliceBatchNos = new java.util.ArrayList<>();
    }

    @Data
    @Schema(description = "管理后台 - 手工成品出库单片退回待重新包装 Request VO")
    public static class ManualOutboundRepackReturnReqVO {
        @NotNull(message = "手工出库历史流水ID不能为空")
        private Long txnLogId;
        @NotBlank(message = "退回原因不能为空")
        @Size(max = 200, message = "退回原因不能超过200个字符")
        private String reason;
        @NotNull(message = "请确认实物已退回且已拆开原包装")
        private Boolean physicalReturned;
        @Size(max = 64, message = "操作人不能超过64个字符")
        private String operatorName;
    }

    @Data
    @Schema(description = "管理后台 - 手工成品出库单片退回待重新包装 Response VO")
    public static class ManualOutboundRepackReturnRespVO {
        private String sliceBatchNo;
        private String sourceInnerUnitNo;
        private Integer sourcePackageRemainingPieceCount;
    }

    @Data
    @Schema(description = "管理后台 - 待上架包装单备注更新 Request VO")
    public static class InboundPackageRemarkUpdateReqVO {
        @NotNull(message = "包装盒ID不能为空")
        private Long id;
        @Size(max = 12, message = "备注不能超过12个字符")
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装盒 Response VO")
    public static class InboundBoxRespVO {
        private Long id;
        private String boxNo;
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private Integer packageSpec;
        private Integer targetQty;
        private Integer currentQty;
        private String materialCode;
        private String materialName;
        private String modelCode;
        @Schema(description = "包装综合质量：任一片为 NG 时返回 NG，否则返回 OK")
        private String qualityStatus;
        /**
         * 二维码标签有效期：按内包装明细 ID 升序取第一张片的有效期。
         */
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        private String motherSegmentBatchNo;
        private String warehouseCode;
        private String warehouseName;
        private String locationCode;
        private String locationName;
        private String labelNo;
        private String status;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastPrintTime;
        private String lockUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lockTime;
        private String inboundUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inboundTime;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recorderTime;
        private String remark;
        private String extraJson;
        private Integer inboundLockedQty;
        private Integer outboundLockedQty;
        private List<InboundBoxItemRespVO> items;
        /**
         * 二维码标签片明细：按内包装明细 ID 升序，仅保留前两片。
         */
        private List<InboundBoxItemRespVO> printItems;
    }

    @Data
    @Schema(description = "管理后台 - 待上架包装段批次 Response VO")
    public static class InboundPackageSegmentRespVO {
        @Schema(description = "段批次号")
        private String segmentBatchNo;
        @Schema(description = "包装质量状态：OK/NG/FROZEN")
        private String qualityStatus;
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String materialCode;
        private String materialName;
        private String modelCode;
        @Schema(description = "段内待上架包装数量")
        private Integer packageCount;
        @Schema(description = "段内待上架片数")
        private Integer totalPieceCount;
        @Schema(description = "样例片号列表")
        private List<String> sampleSliceBatchNos;
    }

    @Data
    @Schema(description = "管理后台 - 成品包装盒片号 Response VO")
    public static class InboundBoxItemRespVO {
        private Long id;
        private Long boxId;
        private String boxNo;
        private Long sourceCutRoundReportId;
        private String sourceType;
        private Long sourceManualPieceId;
        private String sliceBatchNo;
        private String productionBatchNo;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        private String qualityStatus;
        @Schema(description = "包装前 FQC 结果：OK/NG")
        private String inspectionResult;
        @Schema(description = "包装前 FQC 检验备注")
        private String inspectionRemark;
        @Schema(description = "包装前 COA 结果：OK/NG/PENDING/UNKNOWN")
        private String coaInspectionResult;
        @Schema(description = "包装前 COA 检验状态")
        private String coaInspectionStatus;
        @Schema(description = "包装前 COA 不合格原因")
        private String coaNgReason;
        @Schema(description = "包装前过程质量风险标记")
        private String qualityRiskFlag;
        @Schema(description = "包装前过程质量风险快照 JSON")
        private String qualityRiskSnapshotJson;
        @Schema(description = "包装前具体不合格原因")
        private String ngReason;
        private String scanUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime scanTime;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单分页 Request VO")
    public static class ShippingNoticePageReqVO extends PageParam {
        @Schema(description = "关键词")
        private String keyword;
        @Schema(description = "发货需求单号")
        private String noticeNo;
        @Schema(description = "客户名称")
        private String customerName;
        @Schema(description = "产品类型：MASS/RND")
        private String productType;
        @Schema(description = "料号")
        private String materialCode;
        @Schema(description = "产品型号")
        private String modelCode;
        @Schema(description = "订单编号")
        private String orderNo;
        @Schema(description = "需求单状态")
        private String noticeStatus;
        @Schema(description = "发货日期起")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate shippingDateStart;
        @Schema(description = "发货日期止")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate shippingDateEnd;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单保存草稿 Request VO")
    public static class ShippingNoticeSaveLockReqVO {
        private Long id;
        private String noticeNo;
        private Long customerId;
        private String customerCode;
        private String customerName;
        @AssertTrue(message = "发货客户不能为空")
        @JsonIgnore
        public boolean isShippingCustomerValid() {
            return "SAMPLE".equalsIgnoreCase(productType == null ? "" : productType.trim())
                    || (customerName != null && !customerName.trim().isEmpty());
        }
        @NotBlank(message = "产品类型不能为空")
        private String productType;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productSize;
        private String orderNo;
        private String erpOrderNo;
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingTime;
        private String externalProductModel;
        private String externalProductCode;
        private String externalProductInfo;
        private Integer requiredShipQty;
        private String requiredSliceRange;
        private String requiredBatchNo;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate requiredProductionDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate requiredExpiryDate;
        private String packingRequirement;
        private String shippingConfirmName;
        @NotNull(message = "发货数量不能为空")
        private Integer noticeQty;
        private String recorderName;
        private String remark;
        private List<ShippingNoticeSaveItemReqVO> items;
        private List<Long> stockIds;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单变更 Request VO")
    public static class ShippingNoticeChangeReqVO extends ShippingNoticeSaveLockReqVO {
        @NotBlank(message = "变更原因不能为空")
        @Size(max = 200, message = "变更原因不能超过200个字符")
        private String changeReason;

        @NotNull(message = "发货需求单变更版本不能为空")
        private Integer expectedChangeVersion;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单Excel导入 Response VO")
    public static class ShippingNoticeExcelImportRespVO {
        private Integer importCount;
        private String fileName;
        private String fileUrl;
        private List<ShippingNoticeRespVO> notices;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单下达执行 Request VO")
    public static class ShippingNoticeIssueReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long id;
        private String operatorName;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单取消 Request VO")
    public static class ShippingNoticeCancelReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long id;
        private String operatorName;
        @NotBlank(message = "取消原因不能为空")
        private String reason;
        @Schema(description = "是否已确认整单实物退回包装工位并拆除原包装")
        private Boolean physicalReturned;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单客户批号明细保存 Request VO")
    public static class ShippingNoticeSaveItemReqVO {
        private Long id;
        private Long stockId;
        private String internalModelCode;
        private String internalItemCode;
        private String customerProductBatchNo;
        private String packageSliceNo;
        private String customerModelCode;
        private String customerSliceBatchNo;
        private String batchNo;
        private String sliceBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        @Min(value = 1, message = "本次发货数量必须大于0")
        private Integer shipQty;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单实际发货提交 Request VO")
    public static class ShippingNoticeDeliverySubmitReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long id;
        private String operatorName;
        @NotEmpty(message = "请维护实际发货明细")
        private List<ShippingNoticeDeliveryItemReqVO> items;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单实际发货明细 Request VO")
    public static class ShippingNoticeDeliveryItemReqVO {
        @NotNull(message = "发货需求单明细ID不能为空")
        private Long id;
        @NotBlank(message = "实际发货片号不能为空")
        private String actualSliceBatchNo;
        private String customerSliceBatchNo;
        private String customerModelCode;
        private String shippingQualityNo;
        private Long oqcOrderId;
        private String oqcStatus;
        private String shippingInspectorName;
        private String mismatchReason;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单配货库存查询 Request VO")
    public static class ShippingNoticePickCandidatePageReqVO extends FgStockLedgerPageReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long noticeId;
        @Schema(description = "是否包含未上架包装直发候选；默认false以兼容仅支持库存ID的终端")
        private Boolean includePackagingDirect;
        @Schema(description = "分段批号；仅查询段内片号时使用")
        private String segmentBatchNo;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单配货 Request VO")
    public static class ShippingNoticePickReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long noticeId;
        private String operatorName;
        @NotEmpty(message = "请选择配货库存")
        private List<ShippingNoticePickItemReqVO> items;
        private Boolean confirmPicked;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单配货明细 Request VO")
    public static class ShippingNoticePickItemReqVO {
        private Long noticeItemId;
        @Schema(description = "配货候选来源：WAREHOUSE_STOCK/PACKAGING_DIRECT；为空时按普通库存兼容")
        private String candidateType;
        @Schema(description = "配货候选唯一键")
        private String candidateKey;
        @Schema(description = "来源内包装明细ID，已包装片直发时填写")
        private Long sourceInnerPackItemId;
        @Schema(description = "来源裁切报工ID，待包装片直发时填写；与来源内包装明细ID二选一")
        private Long sourceCutRoundReportId;
        @Schema(description = "成品库存ID，普通库存配货时必填")
        private Long stockId;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单退回配货 Request VO")
    public static class ShippingNoticeReturnPickReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long noticeId;
        @NotEmpty(message = "请选择需要退回的配货明细")
        private List<Long> pickItemIds;
        private String operatorName;
        @NotBlank(message = "退回原因不能为空")
        private String reason;
    }

    @Data
    @Schema(description = "管理后台 - 出货管理确认 Request VO")
    public static class ShippingNoticeOutboundConfirmReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long noticeId;
        private String operatorName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货检验扫码确认 Request VO")
    public static class ShippingNoticeInspectionReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long noticeId;
        @NotNull(message = "客户要求明细不能为空")
        private Long noticeItemId;
        @NotBlank(message = "实际片号不能为空")
        private String actualSliceBatchNo;
        @NotBlank(message = "检验结论不能为空")
        private String inspectionResult;
        private String inspectorName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货包装完成 Request VO")
    public static class ShippingNoticePackReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long noticeId;
        @NotEmpty(message = "请选择客户批号明细")
        private List<Long> noticeItemIds;
        @NotBlank(message = "包装方式不能为空")
        private String packageMethod;
        @Valid
        @NotEmpty(message = "请至少选择一条外包装辅材")
        private List<PackageAuxConsumeItemReqVO> auxConsumeItems;
        private String operatorName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货包装推送出货检验 Request VO")
    public static class ShippingNoticePackageConfirmReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long noticeId;
        private String operatorName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单 Response VO")
    public static class ShippingNoticeRespVO {
        private Long id;
        private String noticeNo;
        private Long customerId;
        private String customerCode;
        private String customerName;
        private String productType;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productSize;
        private String orderNo;
        private String erpOrderNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingTime;
        private String externalProductModel;
        private String externalProductCode;
        private String externalProductInfo;
        private Integer requiredShipQty;
        private String requiredSliceRange;
        private String requiredBatchNo;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate requiredProductionDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate requiredExpiryDate;
        private String packingRequirement;
        private String shippingConfirmName;
        private String outboundConfirmName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime outboundConfirmTime;
        private String outboundConfirmRemark;
        private Integer noticeQty;
        private Integer lockedQty;
        private Integer inspectionCompletedQty;
        private Integer changeVersion;
        private String noticeStatus;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recorderTime;
        private String shippingPackageName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingPackageTime;
        private String cancelName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime cancelTime;
        private String cancelReason;
        private String remark;
        private List<ShippingNoticeItemRespVO> items;
        private List<ShippingNoticePickItemRespVO> pickItems;
        private List<ShippingNoticeAttachmentRespVO> attachments;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单明细 Response VO")
    public static class ShippingNoticeItemRespVO {
        private Long id;
        private Long noticeId;
        private String noticeNo;
        private Long finishedStockId;
        private Long actualFinishedStockId;
        private String stockNo;
        private String actualStockNo;
        private String outerBoxNo;
        private String innerUnitNo;
        private String packageNo;
        private String sliceBatchNo;
        private String actualSliceBatchNo;
        private String batchNo;
        private String internalModelCode;
        private String internalItemCode;
        private String customerProductBatchNo;
        private String packageSliceNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productSize;
        private Integer stockQty;
        private Integer availableQty;
        private Boolean coaFrozen;
        private String coaFreezeReason;
        private Integer frozenQty;
        private Integer lockedQty;
        private String qualityStatus;
        private String warehouseCode;
        private String warehouseName;
        private String locationCode;
        private String locationName;
        private String actualLocationCode;
        private String actualLocationName;
        private String inboundNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inboundTime;
        private String lockStatus;
        private String lockName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lockTime;
        private String cancelName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime cancelTime;
        private Integer actualShipQty;
        private String customerSliceBatchNo;
        private String customerModelCode;
        private String shippingQualityNo;
        private Long oqcOrderId;
        private String oqcStatus;
        private String shippingInspectorName;
        private String shippingInspectionResult;
        private String shippingInspectionRemark;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingInspectionTime;
        private String shippingPackageName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingPackageTime;
        private String shippingPackageRemark;
        private String shippedName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippedTime;
        private String mismatchReason;
        private String actualRemark;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单附件 Response VO")
    public static class ShippingNoticeAttachmentRespVO {
        private Long id;
        private Long noticeId;
        private String noticeNo;
        private String attachmentName;
        private String attachmentUrl;
        private String attachmentType;
        private String sourceFileName;
        private String sourceSheetName;
        private Integer sourceSheetIndex;
        private Integer sourceSheetTotal;
        private Long previousNoticeId;
        private String previousSheetName;
        private Long nextNoticeId;
        private String nextSheetName;
        private Long fileSize;
        private String remark;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime createTime;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单配货领用明细 Response VO")
    public static class ShippingNoticePickItemRespVO {
        private Long id;
        private Long noticeId;
        private String noticeNo;
        private Long sourceNoticeItemId;
        @Schema(description = "配货来源：WAREHOUSE_STOCK/PACKAGING_DIRECT")
        private String pickSourceType;
        @Schema(description = "来源内包装明细ID，仅包装直发时有值")
        private Long sourceInnerPackItemId;
        @Schema(description = "来源裁切报工ID，仅待包装片直发时有值")
        private Long sourceCutRoundReportId;
        private Long finishedStockId;
        private String stockNo;
        private String outerBoxNo;
        private String innerUnitNo;
        private String packageNo;
        private String sliceBatchNo;
        private String actualSliceBatchNo;
        private String batchNo;
        private String internalModelCode;
        private String internalItemCode;
        private String customerProductBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productSize;
        private Integer stockQty;
        private Integer availableQty;
        private Boolean coaFrozen;
        private String coaFreezeReason;
        private Integer frozenQty;
        private Integer lockedQty;
        private String qualityStatus;
        private String warehouseCode;
        private String warehouseName;
        private String locationCode;
        private String locationName;
        private String inboundNo;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inboundTime;
        private String lockStatus;
        private String lockName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lockTime;
        private String cancelName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime cancelTime;
        private Integer actualShipQty;
        private String shippingQualityNo;
        private String shippingInspectorName;
        private String shippingInspectionResult;
        private String shippingInspectionRemark;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingInspectionTime;
        private String shippingPackageName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingPackageTime;
        private String shippingPackageRemark;
        private String shippedName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippedTime;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货单 Response VO")
    public static class ShippingOrderRespVO {
        private Long sourceSaleOrderId;
        private String shippingOrderNo;
        private String erpOrderNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productSize;
        private Integer shipQty;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingTime;
    }

    @Data
    @Schema(description = "管理后台 - 发货包装盒初始化 Request VO")
    public static class InitOutboundBoxesReqVO {
        private Long sourceSaleOrderId;
        @NotBlank(message = "发货单号不能为空")
        private String shippingOrderNo;
        private String erpOrderNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productSize;
        @NotNull(message = "发货数量不能为空")
        private Integer shipQty;
        @NotNull(message = "包装盒数不能为空")
        private Integer boxCount;
        @NotNull(message = "一盒片数不能为空")
        private Integer packageSpec;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        private LocalDateTime shippingTime;
        private String recorderName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货需求单初始化发货包装盒 Request VO")
    public static class InitOutboundBoxesFromNoticeReqVO {
        @NotNull(message = "发货需求单ID不能为空")
        private Long sourceNoticeId;
        @NotNull(message = "包装盒数不能为空")
        private Integer boxCount;
        @NotNull(message = "一盒片数不能为空")
        private Integer packageSpec;
        private String recorderName;
        private String remark;
    }

    @Data
    @Schema(description = "管理后台 - 发货包装扫码 Request VO")
    public static class ScanOutboundPieceReqVO {
        @NotNull(message = "发货包装盒ID不能为空")
        private Long boxId;
        @NotBlank(message = "成品片号不能为空")
        private String sliceBatchNo;
        private String scanUserName;
    }

    @Data
    @Schema(description = "管理后台 - 发货包装单 Response VO")
    public static class OutboundOrderRespVO {
        private Long id;
        private String outboundNo;
        private Long sourceSaleOrderId;
        private Long sourceNoticeId;
        private String shippingOrderNo;
        private String shippingNoticeNo;
        private String customerCode;
        private String customerName;
        private String erpOrderNo;
        private String orderNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private String productType;
        private String productSize;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime shippingTime;
        private Integer shipQty;
        private Integer boxCount;
        private Integer pieceCount;
        private String outboundStatus;
        private List<OutboundBoxRespVO> boxes;
    }

    @Data
    @Schema(description = "管理后台 - 发货包装盒 Response VO")
    public static class OutboundBoxRespVO {
        private Long id;
        private Long outboundOrderId;
        private String outboundNo;
        private String boxNo;
        private String erpOrderNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        private Integer targetQty;
        private Integer currentQty;
        private String status;
        private String labelNo;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastPrintTime;
        private List<OutboundBoxItemRespVO> items;
    }

    @Data
    @Schema(description = "管理后台 - 发货包装盒片号 Response VO")
    public static class OutboundBoxItemRespVO {
        private Long id;
        private Long outboundBoxId;
        private String outboundBoxNo;
        private Long finishedStockId;
        private String inboundNo;
        private String inboundBoxNo;
        private String inboundInnerUnitNo;
        private String sliceBatchNo;
        private String materialCode;
        private String modelCode;
        private String batchNo;
        private String qualityStatus;
        private String scanUserName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime scanTime;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库检验片分页 Request VO")
    public static class InspectionSlicePageReqVO extends PageParam {
        @Schema(description = "关键词")
        private String keyword;
        @Schema(description = "报检单号")
        private String inspectionTaskNo;
        @Schema(description = "分段批次")
        private String parentProductionBatchNo;
        @Schema(description = "片号")
        private String productionBatchNo;
        @Schema(description = "段批次号")
        private String segmentBatchNo;
        @Schema(description = "生产日期起")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateStart;
        @Schema(description = "生产日期止")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateEnd;
        @Schema(description = "检验结果：OK/NG")
        private String inspectionResult;
        @Schema(description = "包装待包装综合质量：FQC NG为NG；FQC OK且COA未放行为FROZEN；COA完成OK为OK")
        private String packagingQualityStatus;
        @Schema(description = "库存状态：WAIT_INBOUND/INBOUND_LOCKED/INBOUNDED/OUTBOUND_LOCKED/SHIPPED")
        private String stockStatus;
        @Schema(description = "当前库位关键字")
        private String locationKeyword;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库待包装段批次 Response VO")
    public static class InspectionSliceSegmentRespVO {
        @Schema(description = "段批次号")
        private String segmentBatchNo;
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String materialCode;
        private String materialName;
        private String modelCode;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateStart;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDateEnd;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        @Schema(description = "段内待包装片数")
        private Integer totalPieceCount;
        @Schema(description = "段内综合质量 OK 片数")
        private Integer okPieceCount;
        @Schema(description = "段内综合质量 NG 片数")
        private Integer ngPieceCount;
        @Schema(description = "包装待包装综合质量：OK/NG/FROZEN")
        private String packagingQualityStatus;
        private String inspectionStatus;
        private String stockStatus;
        @Schema(description = "样例片号")
        private String sampleSliceBatchNo;
        @Schema(description = "样例片号列表")
        private List<String> sampleSliceBatchNos;
    }

    @Data
    @Schema(description = "管理后台 - 模拟检测批量完成 Request VO")
    public static class MockInspectionCompleteReqVO {
        @NotEmpty(message = "请选择待检测裁切片")
        private List<Long> cutRoundReportIds;
        @NotBlank(message = "检验结果不能为空")
        private String inspectionResult;
        private String inspectionRemark;
        private String inspectorName;
    }

    @Data
    @Schema(description = "管理后台 - 包装成品库检验片 Response VO")
    public static class InspectionSliceRespVO {
        private String sourceType;
        private Long sourceCutRoundReportId;
        private Long sourceManualPieceId;
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String parentProductionBatchNo;
        private String inspectionTaskNo;
        private String sliceBatchNo;
        private String productionBatchNo;
        private String segmentBatchNo;
        private String materialCode;
        private String materialName;
        private String modelCode;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate productionDate;
        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        private LocalDate expiryDate;
        private String inspectionStatus;
        private String inspectionResult;
        private String inspectionRemark;
        private String inspectorName;
        private String packagingQualityStatus;
        @Schema(description = "过程风险标记：NONE/ADHESIVE2_NG/CUT_ROUND_NG/BOTH_NG，仅用于追溯展示")
        private String qualityRiskFlag;
        @Schema(description = "过程风险快照 JSON")
        private String qualityRiskSnapshotJson;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inspectionTime;
        private String coaInspectionResult;
        private String coaInspectionStatus;
        private String coaInspectionNo;
        private String coaScopeBatchNo;
        private String coaSampleBatchNo;
        private String coaNgReason;
        private String printStatus;
        private Integer printCount;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime lastPrintTime;
        private String recorderName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime recorderTime;
        private String stockStatus;
        private String stockNo;
        private String currentLocationCode;
        private String currentLocationName;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime inboundTime;
        private String inboundUserName;
        private String outboundLocation;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime outboundTime;
        private String outboundRecorderName;
        private String outboundQualityNo;
    }
}
