package cn.iocoder.yudao.module.mes.service.hc.location;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.location.vo.HcLocationSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcLocationDO;

import java.util.List;

public interface HcLocationService {
    Long createHcLocation(HcLocationSaveReqVO createReqVO);
    void updateHcLocation(HcLocationSaveReqVO updateReqVO);
    void deleteHcLocation(Long id);
    void deleteHcLocationListByIds(List<Long> ids);
    HcLocationDO getHcLocation(Long id);
    List<HcLocationDO> getHcLocationSimpleList();
    List<HcLocationDO> getHcLocationList(HcLocationPageReqVO reqVO);
    PageResult<HcLocationDO> getHcLocationPage(HcLocationPageReqVO pageReqVO);
}