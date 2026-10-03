package cn.iocoder.yudao.module.mes.service.hc.processmateriallife;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeEventSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeStatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processmateriallife.vo.HcProcessMaterialLifeStateRespVO;

public interface HcProcessMaterialLifeService {

    PageResult<HcProcessMaterialLifeStateRespVO> getStatePage(HcProcessMaterialLifeStatePageReqVO reqVO);

    PageResult<HcProcessMaterialLifeEventRespVO> getEventPage(HcProcessMaterialLifeEventPageReqVO reqVO);

    Long createEvent(HcProcessMaterialLifeEventSaveReqVO reqVO);
}
