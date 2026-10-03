package cn.iocoder.yudao.module.mes.service.hc.ocaptemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.ocaptemplate.HcOcapTemplateDO;

import java.util.List;

public interface HcOcapTemplateService {
    Long createHcOcapTemplate(HcOcapTemplateSaveReqVO createReqVO);
    void updateHcOcapTemplate(HcOcapTemplateSaveReqVO updateReqVO);
    void deleteHcOcapTemplate(Long id);
    void deleteHcOcapTemplateListByIds(List<Long> ids);
    HcOcapTemplateDO getHcOcapTemplate(Long id);
    List<HcOcapTemplateDO> getHcOcapTemplateSimpleList();
    List<HcOcapTemplateDO> getHcOcapTemplateList(HcOcapTemplatePageReqVO reqVO);
    PageResult<HcOcapTemplateDO> getHcOcapTemplatePage(HcOcapTemplatePageReqVO pageReqVO);
}