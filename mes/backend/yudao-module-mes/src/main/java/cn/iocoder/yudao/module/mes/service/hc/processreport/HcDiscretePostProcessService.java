package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcDiscretePostProcessVO;
import java.util.List;

public interface HcDiscretePostProcessService {

    List<HcDiscretePostProcessVO.StockRespVO> getCandidateList(HcDiscretePostProcessVO.SourceQueryReqVO reqVO);

    Long createPlan(HcDiscretePostProcessVO.CreatePlanReqVO reqVO);

    List<HcDiscretePostProcessVO.TaskRespVO> getTaskList(HcDiscretePostProcessVO.TaskQueryReqVO reqVO);

    List<HcDiscretePostProcessVO.SourceRespVO> getSourceList(Long planOperationId, String taskStatus, String keyword);

    HcDiscretePostProcessVO.SourceRespVO scanSource(Long planOperationId, String pieceNo);

    HcDiscretePostProcessVO.SourceRespVO scanSourceByTargetOp(String targetOpCode, String pieceNo);

    HcDiscretePostProcessVO.SourceRespVO report(HcDiscretePostProcessVO.ReportReqVO reqVO);

    List<HcDiscretePostProcessVO.SourceRespVO> getInspectionTaskList(Long planOperationId, String inspectionType, String keyword);

    HcDiscretePostProcessVO.InspectionRespVO createInspectionTask(HcDiscretePostProcessVO.InspectionReqVO reqVO);
}
