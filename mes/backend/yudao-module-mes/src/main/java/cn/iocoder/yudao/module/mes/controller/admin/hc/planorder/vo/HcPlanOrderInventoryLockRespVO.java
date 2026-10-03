package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - HC 生产计划库存锁定 Response VO")
@Data
@ExcelIgnoreUnannotated
public class HcPlanOrderInventoryLockRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "关联工序ID")
    private Long planOperationId;

    @Schema(description = "目标计划号快照")
    private String targetPlanNo;

    @Schema(description = "目标工序编码快照")
    private String targetOpCode;

    @Schema(description = "目标工序名称快照")
    private String targetOpName;

    @Schema(description = "锁定类型")
    private String lockType;

    @Schema(description = "来源库存ID")
    private Long stockId;

    @Schema(description = "库存类型")
    private String stockType;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "来源事实表名")
    private String sourceTable;

    @Schema(description = "来源事实表主键ID")
    private Long sourceId;

    @Schema(description = "来源计划ID")
    private Long sourcePlanId;

    @Schema(description = "来源计划号")
    private String sourcePlanNo;

    @Schema(description = "来源计划工序ID")
    private Long sourcePlanOperationId;

    @Schema(description = "来源半成品批号")
    private String sourceBatchNo;

    @Schema(description = "来源库存所在工序顺序")
    private Integer opSeq;

    @Schema(description = "来源库存所在工序编码")
    private String opCode;

    @Schema(description = "来源库存所在工序名称")
    private String opName;

    @Schema(description = "段位编码")
    private String segmentCode;

    @Schema(description = "段位名称")
    private String segmentName;

    @Schema(description = "厚度")
    private BigDecimal thickness;

    @Schema(description = "批号快照")
    private String lotNo;

    @Schema(description = "批次号快照")
    private String batchNo;

    @Schema(description = "物料ID")
    private Long materialId;

    @Schema(description = "物料编码快照")
    private String materialCode;

    @Schema(description = "物料名称快照")
    private String materialName;

    @Schema(description = "型号快照")
    private String modelNo;

    @Schema(description = "配方编码快照")
    private String recipeCode;

    @Schema(description = "尺寸规格快照")
    private String sizeSpec;

    @Schema(description = "生产日期快照")
    private java.time.LocalDate productionDate;

    @Schema(description = "失效日期快照")
    private java.time.LocalDate expiryDate;

    @Schema(description = "锁定时余效月份快照")
    private Integer remainMonths;

    @Schema(description = "库位ID")
    private Long locationId;

    @Schema(description = "库位编码快照")
    private String locationCode;

    @Schema(description = "库位名称快照")
    private String locationName;

    @Schema(description = "货主ID")
    private Long ownerId;

    @Schema(description = "货主编码快照")
    private String ownerCode;

    @Schema(description = "货主名称快照")
    private String ownerName;

    @Schema(description = "锁定时可用量快照")
    private BigDecimal availableQty;

    @Schema(description = "本次锁定量")
    private BigDecimal lockQty;

    @Schema(description = "已消耗数量")
    private BigDecimal consumedQty;

    @Schema(description = "已释放数量")
    private BigDecimal releasedQty;

    @Schema(description = "剩余锁定数量")
    private BigDecimal remainingQty;

    @Schema(description = "最近一次消耗报工ID")
    private Long consumeReportId;

    @Schema(description = "最近一次消耗时间")
    private LocalDateTime consumeTime;

    @Schema(description = "释放时间")
    private LocalDateTime releaseTime;

    @Schema(description = "释放原因")
    private String releaseReason;

    @Schema(description = "计划锁定库存流水号")
    private String lockTxnNo;

    @Schema(description = "消耗库存流水号")
    private String consumeTxnNo;

    @Schema(description = "释放库存流水号")
    private String releaseTxnNo;

    @Schema(description = "单位ID")
    private Long unitId;

    @Schema(description = "单位符号")
    private String unitCode;

    @Schema(description = "单位名称")
    private String unitName;

    @Schema(description = "单位")
    private String uom;

    @Schema(description = "FIFO 顺位快照")
    private Integer fifoRank;

    @Schema(description = "锁定状态")
    private String lockStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
