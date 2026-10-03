package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcItemPhotoRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcItemPhotoSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcScanRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcSubmissionDetailPhotoSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsCutRoundFqcSubmissionDetailSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDefectCodeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcAuditRevokeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsFqcScanReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcSubmissionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import java.util.List;
import java.util.Map;

public interface QmsCutRoundFqcService {

    QmsFqcOrderDO createFromCutRoundInspectionTask(HcPlanOrderDO planOrder,
                                                   HcPlanOrderOperationDO operation,
                                                   HcCutRoundInspectionTaskDO task,
                                                   List<HcCutRoundInspectionDetailDO> details,
                                                   Map<Long, HcCutRoundReportDO> reportMap);

    PageResult<QmsCutRoundFqcRespVO> getPage(QmsCutRoundFqcPageReqVO pageReqVO);

    QmsCutRoundFqcRespVO get(Long id);

    QmsCutRoundFqcRespVO getLight(Long id);

    List<QmsFqcRespVO.FqcItem> getSubmissionDetailItems(Long id, Long submissionDetailId);

    List<QmsCutRoundFqcItemPhotoRespVO> getItemPhotos(Long id, Long submissionDetailId, Long fqcItemId);

    QmsCutRoundFqcItemPhotoRespVO saveItemPhoto(QmsCutRoundFqcItemPhotoSaveReqVO reqVO);

    void deleteItemPhoto(Long id, Long photoId);

    List<QmsDefectCodeRespVO> getDefectCodeOptions();

    QmsCutRoundFqcRespVO startProgramEntry(Long id);

    QmsCutRoundFqcRespVO saveProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsCutRoundFqcRespVO submitProgramEntry(QmsFqcSaveReqVO saveReqVO);

    QmsCutRoundFqcRespVO saveSubmissionDetails(QmsCutRoundFqcSubmissionDetailSaveReqVO reqVO);

    QmsCutRoundFqcRespVO saveSubmissionDetailPhotos(QmsCutRoundFqcSubmissionDetailPhotoSaveReqVO reqVO);

    QmsCutRoundFqcRespVO audit(QmsFqcAuditReqVO auditReqVO);

    QmsCutRoundFqcRespVO revokeAudit(QmsFqcAuditRevokeReqVO revokeReqVO);

    QmsCutRoundFqcScanRespVO resolveScan(QmsFqcScanReqVO scanReqVO);

    QmsFqcSubmissionDetailDO getPackagingSubmissionDetail(String productionBatchNo);

    void applyNcrDispositionResult(QmsNcRecordDO ncr,
                                   QmsNcDispositionExecutionDO execution,
                                   List<QmsNcDispositionScopeDO> scopes);
}
