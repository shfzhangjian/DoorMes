package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.Qms8dReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventCloseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventGenerate8dReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventLinkNcrReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionEventWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskBatchSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberConfirmRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskMemberDelegateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskReviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsExceptionGroupTaskSubmitReqVO;
import java.util.List;

public interface QmsExceptionEventService {

    PageResult<QmsExceptionEventRespVO> getExceptionEventPage(QmsExceptionEventPageReqVO pageReqVO);

    QmsExceptionEventRespVO getExceptionEvent(Long id);

    QmsExceptionEventRespVO createExceptionEvent(QmsExceptionEventCreateReqVO createReqVO);

    void handleExceptionEvent(QmsExceptionEventHandleReqVO handleReqVO);

    void returnExceptionEvent(QmsExceptionEventReturnReqVO returnReqVO);

    void withdrawExceptionEvent(QmsExceptionEventWithdrawReqVO withdrawReqVO);

    void closeExceptionEvent(QmsExceptionEventCloseReqVO closeReqVO);

    void saveGroupTasks(QmsExceptionGroupTaskBatchSaveReqVO saveReqVO);

    void submitGroupTask(QmsExceptionGroupTaskSubmitReqVO submitReqVO);

    QmsExceptionGroupTaskMemberConfirmRespVO confirmGroupTaskMember(QmsExceptionGroupTaskMemberConfirmReqVO confirmReqVO);

    void delegateGroupTaskMember(QmsExceptionGroupTaskMemberDelegateReqVO delegateReqVO);

    void reviewGroupTask(QmsExceptionGroupTaskReviewReqVO reviewReqVO);

    void syncBpmProcessStatus(String businessKey, String processInstanceId, Integer bpmStatus, String reason);

    void syncConfirmTaskTransfer(String businessKey, String processInstanceId, String taskId, Long fromUserId,
                                 Long toUserId, String reason);

    void linkNcr(QmsExceptionEventLinkNcrReqVO linkReqVO);

    Qms8dReportRespVO generate8d(QmsExceptionEventGenerate8dReqVO generateReqVO);

    List<QmsExceptionEventRespVO.FlowLog> getFlowLogList(Long exceptionId);
}
