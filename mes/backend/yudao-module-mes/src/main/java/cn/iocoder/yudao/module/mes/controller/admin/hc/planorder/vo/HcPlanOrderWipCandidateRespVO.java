package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo.HcInvStockRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 生产计划挂接半成品候选 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcPlanOrderWipCandidateRespVO extends HcInvStockRespVO {

    @Schema(description = "库存状态：AVAILABLE/LOCKED/FROZEN/CONSUMED")
    private String stockStatus;

    @Schema(description = "锁定合计")
    private BigDecimal lockedQty;

    @Schema(description = "已消耗合计")
    private BigDecimal consumedQty;

    @Schema(description = "已释放合计")
    private BigDecimal releasedQty;

    @Schema(description = "锁定剩余合计")
    private BigDecimal lockRemainingQty;

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

    @Schema(description = "锁定/消耗摘要")
    private String txnSummary;

}
