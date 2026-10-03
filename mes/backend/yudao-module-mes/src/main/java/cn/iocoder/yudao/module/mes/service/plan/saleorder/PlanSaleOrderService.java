package cn.iocoder.yudao.module.mes.service.plan.saleorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo.PlanSaleOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.saleorder.PlanSaleOrderDO;

public interface PlanSaleOrderService {

    Long createSaleOrder(PlanSaleOrderSaveReqVO createReqVO);

    boolean updateSaleOrder(PlanSaleOrderSaveReqVO updateReqVO);

    boolean auditSaleOrder(Long id);

    boolean deleteSaleOrder(Long id);

    PlanSaleOrderDO getSaleOrder(Long id);

    PageResult<PlanSaleOrderDO> getSaleOrderPage(PlanSaleOrderPageReqVO pageReqVO);

}
