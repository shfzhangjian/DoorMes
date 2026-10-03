package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmTrialValidationRespVO;
import jakarta.validation.Valid;

public interface SrmTrialValidationService {

    Long issueFromSampleEvaluation(Long sampleEvaluationId);

    SrmTrialValidationRespVO getTrialValidation(Long id);

    PageResult<SrmTrialValidationRespVO> getTrialValidationPage(SrmTrialValidationPageReqVO reqVO);

    void trialExecute(@Valid SrmTrialValidationActionReqVO.TrialExecute reqVO);

    void completeProduction(@Valid SrmTrialValidationActionReqVO.ProductionComplete reqVO);

    void archiveConfirm(@Valid SrmTrialValidationActionReqVO.ArchiveConfirm reqVO);

}
