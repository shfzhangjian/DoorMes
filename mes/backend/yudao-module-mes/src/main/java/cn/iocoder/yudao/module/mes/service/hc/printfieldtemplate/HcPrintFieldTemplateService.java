package cn.iocoder.yudao.module.mes.service.hc.printfieldtemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printfieldtemplate.HcPrintFieldTemplateDO;
import java.util.List;

public interface HcPrintFieldTemplateService {

    Long createHcPrintFieldTemplate(HcPrintFieldTemplateSaveReqVO createReqVO);

    void updateHcPrintFieldTemplate(HcPrintFieldTemplateSaveReqVO updateReqVO);

    void deleteHcPrintFieldTemplate(Long id);

    HcPrintFieldTemplateDO getHcPrintFieldTemplate(Long id);

    HcPrintFieldTemplateRespVO getHcPrintFieldTemplateDetail(Long id);

    PageResult<HcPrintFieldTemplateDO> getHcPrintFieldTemplatePage(HcPrintFieldTemplatePageReqVO pageReqVO);

    List<HcPrintFieldTemplateItemRespVO> getItemsByTemplateId(Long templateId);

    void updateItems(Long templateId, List<HcPrintFieldTemplateItemSaveReqVO> items);

    HcPrintFieldTemplateRespVO getActiveTemplate(String templateCode);

}
