package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderBatchPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderBatchPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderInventoryLockReleaseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderOperationStatusReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderStatusReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderWipCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderWipCandidateRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderStatusLogDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface HcPlanOrderService {

    Long createHcPlanOrder(HcPlanOrderSaveReqVO createReqVO);

    void updateHcPlanOrder(HcPlanOrderSaveReqVO updateReqVO);

    void deleteHcPlanOrder(Long id);

    void deleteHcPlanOrderListByIds(List<Long> ids);

    void destroyHcPlanOrder(Long id);

    HcPlanOrderDO getHcPlanOrder(Long id);

    List<HcPlanOrderDO> getHcPlanOrderList(HcPlanOrderPageReqVO reqVO);

    PageResult<HcPlanOrderDO> getHcPlanOrderPage(HcPlanOrderPageReqVO pageReqVO);

    HcPlanOrderBatchPreviewRespVO previewRootBatchNo(HcPlanOrderBatchPreviewReqVO reqVO);

    PageResult<HcPlanProcessPivotRespVO> getPlanProcessPivotPage(HcPlanProcessPivotPageReqVO pageReqVO);

    List<HcPlanProcessPivotRespVO> getPlanProcessPivotList(HcPlanProcessPivotPageReqVO pageReqVO);

    PageResult<HcPlanOrderWipCandidateRespVO> getWipCandidatePage(HcPlanOrderWipCandidatePageReqVO pageReqVO);

    List<HcPlanOrderOperationDO> getOperationListByPlanId(Long planId);

    Map<Long, Map<String, BigDecimal>> getOperationDailyReportQtyMap(Long planId);

    Map<Long, HcProcessReportMapper.OperationLatestReportRow> getOperationLatestReportMap(Long planId);

    List<HcPlanOrderInventoryLockDO> getInventoryLockListByPlanId(Long planId);

    void updateOperationStatus(HcPlanOrderOperationStatusReqVO reqVO);

    void updatePlanStatus(HcPlanOrderStatusReqVO reqVO);

    void withdrawPlanOrder(Long id);

    void releaseInventoryLock(HcPlanOrderInventoryLockReleaseReqVO reqVO);

    List<HcPlanOrderStatusLogDO> getStatusLogListByPlanId(Long planId);

}
