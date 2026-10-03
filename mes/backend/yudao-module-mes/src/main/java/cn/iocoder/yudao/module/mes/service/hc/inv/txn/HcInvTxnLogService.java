package cn.iocoder.yudao.module.mes.service.hc.inv.txn;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.txn.vo.HcInvTxnLogPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;

import java.util.List;

/**
 * 库存流水 Service 接口
 */
public interface HcInvTxnLogService {

    PageResult<HcInvTxnLogDO> getInvTxnLogPage(HcInvTxnLogPageReqVO pageReqVO);

    List<HcInvTxnLogDO> getInvTxnLogList(HcInvTxnLogPageReqVO reqVO);

}
