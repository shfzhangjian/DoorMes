package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationProjectRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationSampleRequestPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestRespVO;
import java.util.List;

public interface SrmSampleEvaluationService {

    Long createSampleEvaluation(SrmSampleEvaluationSaveReqVO reqVO);

    void updateSampleEvaluation(SrmSampleEvaluationSaveReqVO reqVO);

    void deleteSampleEvaluation(Long id);

    SrmSampleEvaluationRespVO getSampleEvaluation(Long id);

    PageResult<SrmSampleEvaluationRespVO> getSampleEvaluationPage(SrmSampleEvaluationPageReqVO reqVO);

    PageResult<SrmSampleRequestRespVO> getSelectableSampleRequestPage(SrmSampleEvaluationSampleRequestPageReqVO reqVO);

    List<SrmSampleEvaluationRespVO.Item> getLatestItemsBySampleRequestId(Long sampleRequestId);

    PageResult<SrmSampleEvaluationRespVO> getHistoryPage(Long sampleRequestId, SrmSampleEvaluationPageReqVO reqVO);

    List<SrmSampleEvaluationProjectRespVO> getEnabledProjects();

    SrmSampleEvaluationProjectRespVO getProjectConfig(Long projectId);

    SrmSampleEvaluationProjectRespVO.AssignableInspectors getAssignableInspectors(Long projectId);

    void submit(SrmSampleEvaluationActionReqVO.Submit reqVO);

    void inspectionReport(SrmSampleEvaluationActionReqVO.InspectionReport reqVO);

    void valueConfirm(SrmSampleEvaluationActionReqVO.ValueConfirm reqVO);

    void sign(SrmSampleEvaluationActionReqVO.Sign reqVO);

    void initiatorDecision(SrmSampleEvaluationActionReqVO.InitiatorDecision reqVO);

    void finalApprove(SrmSampleEvaluationActionReqVO.FinalApprove reqVO);

    void archiveConfirm(SrmSampleEvaluationActionReqVO.ArchiveConfirm reqVO);

    void withdrawConfirm(SrmSampleEvaluationActionReqVO.WithdrawConfirm reqVO);

}
