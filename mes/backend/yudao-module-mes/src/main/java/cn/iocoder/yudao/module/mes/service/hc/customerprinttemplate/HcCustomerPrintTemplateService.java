package cn.iocoder.yudao.module.mes.service.hc.customerprinttemplate;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateFieldOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateParseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.customerprinttemplate.vo.HcCustomerPrintTemplateVarRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.customerprinttemplate.HcCustomerPrintTemplateDO;
import java.util.List;

public interface HcCustomerPrintTemplateService {

    Long createHcCustomerPrintTemplate(HcCustomerPrintTemplateSaveReqVO createReqVO);

    void updateHcCustomerPrintTemplate(HcCustomerPrintTemplateSaveReqVO updateReqVO);

    void deleteHcCustomerPrintTemplate(Long id);

    HcCustomerPrintTemplateDO getHcCustomerPrintTemplate(Long id);

    HcCustomerPrintTemplateRespVO getHcCustomerPrintTemplateDetail(Long id);

    PageResult<HcCustomerPrintTemplateDO> getHcCustomerPrintTemplatePage(HcCustomerPrintTemplatePageReqVO pageReqVO);

    List<HcCustomerPrintTemplateVarRespVO> getVarsByTemplateId(Long templateId);

    List<HcCustomerPrintTemplateRespVO> getActiveTemplates(String templateType, String customerCode, String customerName);

    List<HcCustomerPrintTemplateFieldOptionRespVO> getFieldOptions(String scope);

    List<HcCustomerPrintTemplateVarRespVO> parseVariables(HcCustomerPrintTemplateParseReqVO reqVO);

}
