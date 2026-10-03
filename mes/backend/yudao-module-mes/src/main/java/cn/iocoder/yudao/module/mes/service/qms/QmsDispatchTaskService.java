package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCandidateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskDispatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskProcessSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskRecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskResultSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskWizardCreateReqVO;
import java.util.List;

public interface QmsDispatchTaskService {

    Long createFromExecution(QmsDispatchTaskCreateReqVO reqVO);

    Long createAndDispatch(QmsDispatchTaskWizardCreateReqVO reqVO);

    void dispatch(QmsDispatchTaskDispatchReqVO reqVO);

    void cancel(QmsDispatchTaskCancelReqVO reqVO);

    void cancelForDeletedIqc(Long iqcId);

    QmsDispatchTaskRespVO get(Long id);

    QmsDispatchTaskDetailRespVO getDetail(Long id);

    void saveResult(QmsDispatchTaskResultSaveReqVO reqVO);

    void submitProcessNode(QmsDispatchTaskProcessSubmitReqVO reqVO);

    void returnForRecheck(QmsDispatchTaskRecheckReqVO reqVO);

    void syncBpmProcessStatus(String businessKey, String processInstanceId, Integer status, String reason);

    PageResult<QmsDispatchTaskSourceRespVO> getSourcePage(QmsDispatchTaskSourcePageReqVO reqVO);

    List<QmsDispatchTaskCandidateItemRespVO> getSourceItems(String checkType, Long executionId);

    List<QmsDispatchTaskCandidateItemRespVO> getSourceItemTree(String checkType, Long executionId);

    PageResult<QmsDispatchTaskStandardRespVO> getStandardPage(QmsDispatchTaskStandardPageReqVO reqVO);

    List<QmsDispatchTaskCandidateItemRespVO> getStandardItems(Long standardId);

    PageResult<QmsDispatchTaskRespVO> getPage(QmsDispatchTaskPageReqVO reqVO);

    List<QmsDispatchTaskLogRespVO> getLogs(Long taskId);
}
