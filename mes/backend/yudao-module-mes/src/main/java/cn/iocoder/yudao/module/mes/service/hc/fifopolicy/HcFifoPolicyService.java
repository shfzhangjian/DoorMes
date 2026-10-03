package cn.iocoder.yudao.module.mes.service.hc.fifopolicy;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo.HcFifoPolicySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.fifopolicy.HcFifoPolicyDO;

import java.util.List;

public interface HcFifoPolicyService {
    Long createHcFifoPolicy(HcFifoPolicySaveReqVO createReqVO);
    void updateHcFifoPolicy(HcFifoPolicySaveReqVO updateReqVO);
    void deleteHcFifoPolicy(Long id);
    void deleteHcFifoPolicyListByIds(List<Long> ids);
    HcFifoPolicyDO getHcFifoPolicy(Long id);
    List<HcFifoPolicyDO> getHcFifoPolicySimpleList();
    List<HcFifoPolicyDO> getHcFifoPolicyList(HcFifoPolicyPageReqVO reqVO);
    PageResult<HcFifoPolicyDO> getHcFifoPolicyPage(HcFifoPolicyPageReqVO pageReqVO);
}