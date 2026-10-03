package cn.iocoder.yudao.module.mes.service.hc.owner;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.owner.vo.HcOwnerSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.owner.HcOwnerDO;

import java.util.List;

public interface HcOwnerService {
    Long createHcOwner(HcOwnerSaveReqVO createReqVO);
    void updateHcOwner(HcOwnerSaveReqVO updateReqVO);
    void deleteHcOwner(Long id);
    void deleteHcOwnerListByIds(List<Long> ids);
    HcOwnerDO getHcOwner(Long id);
    List<HcOwnerDO> getHcOwnerSimpleList();
    List<HcOwnerDO> getHcOwnerList(HcOwnerPageReqVO reqVO);
    PageResult<HcOwnerDO> getHcOwnerPage(HcOwnerPageReqVO pageReqVO);
}