package cn.iocoder.yudao.module.mes.service.hc.inv.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo.HcInvStockPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.dto.HcWipOutputPostReq;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 实时库存余额 Service 接口
 */
public interface HcInvStockService {

    /**
     * 查询实时库存余额分页
     */
    PageResult<HcInvStockDO> getInvStockPage(HcInvStockPageReqVO pageReqVO);

    /**
     * 查询实时库存余额列表（用于导出）
     */
    List<HcInvStockDO> getInvStockList(HcInvStockPageReqVO reqVO);

    /**
     * 磨皮二磨确认后，将分段产出过账为 WIP 半成品库存。
     */
    HcInvStockDO postGrindingSecondWip(HcGrindingSecondDetailDO detail,
                                       HcPlanOrderDO plan,
                                       HcPlanOrderOperationDO operation,
                                       LocalDateTime postTime,
                                       Long operatorId,
                                       String operatorName);

    /**
     * 工序确认产出后，将产出物过账为 WIP 中间品库存。
     */
    HcInvStockDO postProcessOutputWip(HcWipOutputPostReq req);

    /**
     * 计划挂接 WIP 半成品时，将可用库存转为计划锁定库存。
     */
    HcInvStockDO lockPlanWip(HcPlanOrderInventoryLockDO lock,
                             BigDecimal lockQty,
                             LocalDateTime lockTime,
                             Long operatorId,
                             String operatorName,
                             String remark);

    /**
     * 人工释放计划挂接 WIP 半成品，将未消耗锁定量退回可用库存。
     */
    HcInvStockDO releasePlanLockedWip(HcPlanOrderInventoryLockDO lock,
                                      BigDecimal releaseQty,
                                      String releaseReason,
                                      LocalDateTime releaseTime,
                                      Long operatorId,
                                      String operatorName);

    /**
     * 下游工序确认时，按生产计划利库锁消耗 WIP 半成品库存。
     */
    HcInvStockDO consumePlanLockedWip(HcPlanOrderInventoryLockDO lock,
                                      BigDecimal consumeQty,
                                      String refDocType,
                                      Long refDocId,
                                      String refDocNo,
                                      LocalDateTime consumeTime,
                                      Long operatorId,
                                      String operatorName,
                                      String remark);

}
