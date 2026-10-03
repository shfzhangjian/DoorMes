package cn.iocoder.yudao.module.mes.service.hc.workcenter;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo.HcWorkCenterSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.workcenter.HcWorkCenterDO;

import java.util.List;

public interface HcWorkCenterService {
    Long createHcWorkCenter(HcWorkCenterSaveReqVO createReqVO);
    void updateHcWorkCenter(HcWorkCenterSaveReqVO updateReqVO);
    void deleteHcWorkCenter(Long id);
    void deleteHcWorkCenterListByIds(List<Long> ids);
    HcWorkCenterDO getHcWorkCenter(Long id);
    HcWorkCenterDO getHcWorkCenterByTerminalIp(String ip);
    List<HcWorkCenterDO> getHcWorkCenterSimpleList();
    List<HcWorkCenterDO> getHcWorkCenterList(HcWorkCenterPageReqVO reqVO);
    PageResult<HcWorkCenterDO> getHcWorkCenterPage(HcWorkCenterPageReqVO pageReqVO);
}
