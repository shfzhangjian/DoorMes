package cn.iocoder.yudao.module.mes.service.hc.lotinstance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotinstance.vo.HcLotInstancePageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance.HcLotInstanceDO;

public interface HcLotInstanceService {

    PageResult<HcLotInstanceDO> getLotInstancePage(HcLotInstancePageReqVO pageReqVO);

    HcLotInstanceDO getLotInstance(Long id);
}
