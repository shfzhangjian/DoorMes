package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPreliminaryEvaluationSaveReqVO;
import jakarta.validation.Valid;

public interface SrmPreliminaryEvaluationService {

    Long createEvaluation(@Valid SrmPreliminaryEvaluationSaveReqVO reqVO);

    void updateEvaluation(@Valid SrmPreliminaryEvaluationSaveReqVO reqVO);

    void deleteEvaluation(Long id);

    SrmPreliminaryEvaluationRespVO getEvaluation(Long id);

    PageResult<SrmPreliminaryEvaluationRespVO> getEvaluationPage(SrmPreliminaryEvaluationPageReqVO reqVO);

    void assignScorers(@Valid SrmPreliminaryEvaluationActionReqVO.AssignScorer reqVO);

    void sendForScoring(Long id);

    void submitScore(@Valid SrmPreliminaryEvaluationActionReqVO.Score reqVO);

    void calculateScore(Long id);

    void submitDecision(@Valid SrmPreliminaryEvaluationActionReqVO.Decision reqVO);

    void submitGeneralManagerOpinion(@Valid SrmPreliminaryEvaluationActionReqVO.GeneralManagerOpinion reqVO);

    void publish(@Valid SrmPreliminaryEvaluationActionReqVO.Publish reqVO);

}
