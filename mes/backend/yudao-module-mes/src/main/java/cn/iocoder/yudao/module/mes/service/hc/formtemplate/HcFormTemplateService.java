package cn.iocoder.yudao.module.mes.service.hc.formtemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateVersionDO;

import java.util.List;

public interface HcFormTemplateService {
    Long createHcFormTemplate(HcFormTemplateSaveReqVO createReqVO);
    void updateHcFormTemplate(HcFormTemplateSaveReqVO updateReqVO);
    void deleteHcFormTemplate(Long id);
    void deleteHcFormTemplateListByIds(List<Long> ids);
    HcFormTemplateDO getHcFormTemplate(Long id);
    List<HcFormTemplateDO> getHcFormTemplateSimpleList();
    List<HcFormTemplateDO> getHcFormTemplateList(HcFormTemplatePageReqVO reqVO);
    PageResult<HcFormTemplateDO> getHcFormTemplatePage(HcFormTemplatePageReqVO pageReqVO);
    List<HcFormTemplateVersionDO> getHcFormTemplateVersionListByParentId(Long parentId);
}