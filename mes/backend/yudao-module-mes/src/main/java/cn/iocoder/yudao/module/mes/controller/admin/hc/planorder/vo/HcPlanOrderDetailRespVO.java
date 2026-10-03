package cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - HC 生产计划详情 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcPlanOrderDetailRespVO extends HcPlanOrderRespVO {

    @Schema(description = "工序计划列表")
    private List<HcPlanOrderOperationRespVO> operations;

    @Schema(description = "库存锁定列表")
    private List<HcPlanOrderInventoryLockRespVO> inventoryLocks;

}
