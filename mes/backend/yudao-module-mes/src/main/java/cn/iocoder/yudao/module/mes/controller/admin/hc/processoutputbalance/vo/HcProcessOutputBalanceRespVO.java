package cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 工序产出物结存台账 Response VO")
@Data
public class HcProcessOutputBalanceRespVO {

    @Schema(description = "台账行ID")
    private String ledgerId;

    @Schema(description = "工序编码")
    private String stageCode;

    @Schema(description = "工序名称")
    private String stageName;

    @Schema(description = "工序排序")
    private Integer stageSort;

    @Schema(description = "来源事实表")
    private String sourceTable;

    @Schema(description = "来源事实表ID")
    private Long sourceId;

    @Schema(description = "来源报工ID")
    private Long sourceReportId;

    @Schema(description = "计划ID")
    private Long planId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "计划工序ID")
    private Long planOperationId;

    @Schema(description = "工序代码")
    private String operationCode;

    @Schema(description = "工序名称")
    private String operationName;

    @Schema(description = "产出批号")
    private String outputBatchNo;

    @Schema(description = "母批/上游批号")
    private String parentBatchNo;

    @Schema(description = "来源批号")
    private String sourceBatchNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "型号")
    private String modelCode;

    @Schema(description = "产出数量")
    private BigDecimal outputQty;

    @Schema(description = "已消耗数量")
    private BigDecimal consumedQty;

    @Schema(description = "剩余数量")
    private BigDecimal remainingQty;

    @Schema(description = "单位")
    private String uom;

    @Schema(description = "结存状态")
    private String balanceStatus;

    @Schema(description = "报工状态")
    private String reportStatus;

    @Schema(description = "报工确认时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;

    @Schema(description = "消耗说明")
    private String consumeSummary;

    @Schema(description = "来源计划锁定明细ID")
    private Long sourcePlanLockId;

    @Schema(description = "来源锁定状态")
    private String sourceLockStatus;

    @Schema(description = "来源锁定数量")
    private BigDecimal sourceLockedQty;

    @Schema(description = "来源锁定剩余数量")
    private BigDecimal sourceLockRemainingQty;

    @Schema(description = "来源锁定目标计划")
    private String sourceLockTargetPlanNo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
