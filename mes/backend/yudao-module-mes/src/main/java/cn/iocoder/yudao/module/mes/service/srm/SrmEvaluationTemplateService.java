package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmEvaluationTemplateSaveReqVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SrmEvaluationTemplateService {

    Long createTemplate(@Valid SrmEvaluationTemplateSaveReqVO reqVO);

    void updateTemplate(@Valid SrmEvaluationTemplateSaveReqVO reqVO);

    SrmEvaluationTemplateRespVO getTemplate(Long id, Long versionId);

    PageResult<SrmEvaluationTemplateRespVO> getTemplatePage(SrmEvaluationTemplatePageReqVO reqVO);

    List<SrmEvaluationTemplateRespVO> getPublishedTemplateList(String sceneType);

    void submitAudit(@Valid SrmEvaluationTemplateActionReqVO reqVO);

    void audit(@Valid SrmEvaluationTemplateActionReqVO reqVO);

    void publish(@Valid SrmEvaluationTemplateActionReqVO reqVO);

    Long upgrade(@Valid SrmEvaluationTemplateActionReqVO reqVO);

}
