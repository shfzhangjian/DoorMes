package cn.iocoder.yudao.module.mes.controller.admin.hc.intermediatestockledger.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 工序中间品库台账 Response VO")
@Data
public class HcIntermediateStockLedgerRespVO {

    @Schema(description = "库存ID")
    private Long id;

    @Schema(description = "行类型：DETAIL 明细，GROUP 聚合")
    private String rowType;

    @Schema(description = "母卷批次号")
    private String motherBatchNo;

    @Schema(description = "聚合包含的明细片数")
    private Integer stockCount;

    @Schema(description = "库存类型")
    private String stockType;

    @Schema(description = "工序/来源类型")
    private String sourceType;

    @Schema(description = "来源事实表")
    private String sourceTable;

    @Schema(description = "来源事实表ID")
    private Long sourceId;

    @Schema(description = "来源报工主记录ID")
    private Long sourceReportId;

    @Schema(description = "来源计划ID")
    private Long sourcePlanId;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "来源计划工序ID")
    private Long sourcePlanOperationId;

    @Schema(description = "中间品批号")
    private String batchNo;

    @Schema(description = "来源批号")
    private String sourceBatchNo;

    @Schema(description = "母批/上游批号")
    private String sourceParentBatchNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "产品型号")
    private String modelNo;

    @Schema(description = "工序编码")
    private String opCode;

    @Schema(description = "工序名称")
    private String opName;

    @Schema(description = "工序顺序")
    private Integer opSeq;

    @Schema(description = "段位编码")
    private String segmentCode;

    @Schema(description = "段位名称")
    private String segmentName;

    @Schema(description = "在库数量")
    private BigDecimal onHandQty;

    @Schema(description = "可用数量")
    private BigDecimal availableQty;

    @Schema(description = "可利库量")
    private BigDecimal shareableQty;

    @Schema(description = "冻结数量")
    private BigDecimal frozenQty;

    @Schema(description = "计划锁定量")
    private BigDecimal planLockedQty;

    @Schema(description = "锁定合计")
    private BigDecimal lockedQty;

    @Schema(description = "已消耗合计")
    private BigDecimal consumedQty;

    @Schema(description = "已释放合计")
    private BigDecimal releasedQty;

    @Schema(description = "锁定剩余合计")
    private BigDecimal lockRemainingQty;

    @Schema(description = "单位")
    private String uom;

    @Schema(description = "库存状态")
    private String stockStatus;

    @Schema(description = "活动锁ID")
    private Long activeLockId;

    @Schema(description = "活动锁状态")
    private String activeLockStatus;

    @Schema(description = "活动锁目标计划")
    private String activeLockTargetPlanNo;

    @Schema(description = "活动锁目标工序")
    private String activeLockTargetOpName;

    @Schema(description = "活动锁剩余数量")
    private BigDecimal activeLockRemainingQty;

    @Schema(description = "质量状态")
    private String qualityStatus;

    @Schema(description = "业务状态")
    private String bizStatus;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "库位名称")
    private String locationName;

    @Schema(description = "生产日期")
    private LocalDate productionDate;

    @Schema(description = "最近流水号")
    private String lastTxnNo;

    @Schema(description = "最近过账时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastTxnTime;

    @Schema(description = "消耗/锁定说明")
    private String txnSummary;

    @Schema(description = "消耗/锁定流水明细")
    private List<TxnDetailRespVO> txnDetails;

    @Schema(description = "库存业务备注")
    private String businessRemark;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Schema(description = "消耗/锁定流水明细")
    @Data
    public static class TxnDetailRespVO {

        @Schema(description = "事件类型")
        private String eventType;

        @Schema(description = "事件类型名称")
        private String eventTypeName;

        @Schema(description = "流水ID")
        private Long txnId;

        @Schema(description = "流水号")
        private String txnNo;

        @Schema(description = "流水类型")
        private String txnType;

        @Schema(description = "流水时间")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime txnTime;

        @Schema(description = "库存ID")
        private Long stockId;

        @Schema(description = "锁定ID")
        private Long lockId;

        @Schema(description = "使用计划号")
        private String targetPlanNo;

        @Schema(description = "使用计划工序ID")
        private Long targetPlanOperationId;

        @Schema(description = "使用工序名称")
        private String targetOpName;

        @Schema(description = "数量")
        private BigDecimal qty;

        @Schema(description = "单位")
        private String uom;

        @Schema(description = "来源单据类型")
        private String refDocType;

        @Schema(description = "来源单据ID")
        private Long refDocId;

        @Schema(description = "来源单据号")
        private String refDocNo;

        @Schema(description = "展示批号/片号")
        private String displayBatchNo;

        @Schema(description = "说明")
        private String summary;

        @Schema(description = "备注")
        private String remark;

    }

}
