package cn.iocoder.yudao.module.mes.service.hc.qtimeconfig;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.qtimeconfig.vo.HcQtimeConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.qtimeconfig.HcQtimeConfigDO;
import java.util.List;

public interface HcQtimeConfigService {

    Long createHcQtimeConfig(HcQtimeConfigSaveReqVO createReqVO);

    void updateHcQtimeConfig(HcQtimeConfigSaveReqVO updateReqVO);

    void deleteHcQtimeConfig(Long id);

    HcQtimeConfigDO getHcQtimeConfig(Long id);

    List<HcQtimeConfigDO> getHcQtimeConfigList(HcQtimeConfigPageReqVO reqVO);

    PageResult<HcQtimeConfigDO> getHcQtimeConfigPage(HcQtimeConfigPageReqVO reqVO);

}
