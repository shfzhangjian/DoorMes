package cn.iocoder.yudao.module.mes.service.hc.processoutputbalance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processoutputbalance.vo.HcProcessOutputBalancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processoutputbalance.HcProcessOutputBalanceDO;

/**
 * 工序产出物结存台账 Service.
 */
public interface HcProcessOutputBalanceService {

    PageResult<HcProcessOutputBalanceDO> getProcessOutputBalancePage(HcProcessOutputBalancePageReqVO pageReqVO);

}
