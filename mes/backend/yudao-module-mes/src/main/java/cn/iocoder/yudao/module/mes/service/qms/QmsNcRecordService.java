package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcPackagingPieceRespVO;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcMrbReviewDelegateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordBatchCreateFromProductEventReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordCreateFromProductEventReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordCreateFromProductEventRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyReplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionNotifyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordFinalApproveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordLinkExceptionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordRestorePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordStockDisposeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductNcRecordRestoreReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcInspectionPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordCreateFromInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordCreateFromInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsRawMaterialNcRecordRestoreReqVO;
import jakarta.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface QmsNcRecordService {

    Long createNcRecord(@Valid QmsNcRecordSaveReqVO createReqVO);

    Long relaunchNcRecord(Long sourceId);

    Long createOrGetFromProductEvent(@Valid QmsNcRecordCreateFromProductEventReqVO createReqVO);

    List<QmsNcRecordCreateFromProductEventRespVO> batchCreateFromProductEvent(
            @Valid QmsNcRecordBatchCreateFromProductEventReqVO createReqVO);

    PageResult<QmsRawMaterialNcInspectionRespVO> getRawMaterialInspectionPage(
            QmsRawMaterialNcInspectionPageReqVO pageReqVO);

    Long createOrGetRawMaterialFromInspection(@Valid QmsRawMaterialNcRecordCreateFromInspectionReqVO createReqVO);

    List<QmsRawMaterialNcRecordCreateFromInspectionRespVO> batchCreateRawMaterialFromInspection(
            @Valid QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO createReqVO);

    Long mergeCreateRawMaterialFromInspection(@Valid QmsRawMaterialNcRecordBatchCreateFromInspectionReqVO createReqVO);

    void restoreRawMaterialNcRecord(@Valid QmsRawMaterialNcRecordRestoreReqVO restoreReqVO);

    void restoreProductNcRecord(@Valid QmsProductNcRecordRestoreReqVO restoreReqVO);

    QmsNcRecordRestorePreviewRespVO getRawMaterialNcRestorePreview(Long id);

    QmsNcRecordRestorePreviewRespVO getProductNcRestorePreview(Long id);

    void updateNcRecord(@Valid QmsNcRecordSaveReqVO updateReqVO);

    PageResult<QmsNcRecordRespVO> getNcRecordPage(QmsNcRecordPageReqVO pageReqVO);

    PageResult<QmsNcDispositionNotifyRespVO> getDispositionNotifyPage(QmsNcDispositionNotifyPageReqVO pageReqVO);

    QmsNcRecordRespVO getNcRecord(Long id);

    QmsNcRecordRespVO getPackagingFlowRecord(Long id);

    List<QmsNcPackagingPieceRespVO> getPackagingFlowPieces(Long id);

    void submitNcRecord(@Valid QmsNcRecordSubmitReqVO submitReqVO);

    void withdrawNcRecord(@Valid QmsNcRecordWithdrawReqVO withdrawReqVO);

    void handleNcRecord(@Valid QmsNcRecordHandleReqVO handleReqVO);

    void linkException(@Valid QmsNcRecordLinkExceptionReqVO linkReqVO);

    void delegateMrbReview(@Valid QmsNcMrbReviewDelegateReqVO delegateReqVO);

    void returnNcRecord(@Valid QmsNcRecordReturnReqVO returnReqVO);

    void finalApproveNcRecord(@Valid QmsNcRecordFinalApproveReqVO approveReqVO);

    void stockDisposeNcRecord(@Valid QmsNcRecordStockDisposeReqVO disposeReqVO);

    void replyDispositionNotify(@Valid QmsNcDispositionNotifyReplyReqVO replyReqVO);

    void autoCompleteProductNcrAfterPackaging(Collection<String> pieceNos);

    void syncBpmProcessStatus(String businessKey, String processInstanceId, Integer bpmStatus, String reason);

    List<QmsNcRecordRespVO.FlowLog> getFlowLogList(Long ncRecordId);
}
