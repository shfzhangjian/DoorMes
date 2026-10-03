package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmAttachmentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmAttachmentVersionUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmDocumentSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmOnboardingApplySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFilePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierFileSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveyPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSurveySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmDocumentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSupplierFileDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSurveyDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSurveyReviewDO;
import jakarta.validation.Valid;
import java.util.List;

public interface SrmService {

    Long createAttachment(@Valid SrmAttachmentSaveReqVO reqVO);

    Long updateAttachmentVersion(@Valid SrmAttachmentVersionUpdateReqVO reqVO);

    void deleteAttachment(Long id);

    List<SrmAttachmentDO> getAttachmentList(String bizType, Long bizId, boolean includeHistory);

    Long createSupplierFile(@Valid SrmSupplierFileSaveReqVO reqVO);

    void updateSupplierFile(@Valid SrmSupplierFileSaveReqVO reqVO);

    void deleteSupplierFile(Long id);

    SrmSupplierFileDO getSupplierFile(Long id);

    PageResult<SrmSupplierFileDO> getSupplierFilePage(SrmSupplierFilePageReqVO reqVO);

    Long createOnboardingApply(@Valid SrmOnboardingApplySaveReqVO reqVO);

    void updateOnboardingApply(@Valid SrmOnboardingApplySaveReqVO reqVO);

    void deleteOnboardingApply(Long id);

    SrmOnboardingApplyRespVO getOnboardingApply(Long id);

    PageResult<SrmOnboardingApplyRespVO> getOnboardingApplyPage(SrmOnboardingApplyPageReqVO reqVO);

    void submitOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Submit reqVO);

    void purchaseIntakeOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.PurchaseIntake reqVO);

    void signOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Sign reqVO);

    void purchaseTransferOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.PurchaseTransfer reqVO);

    void entryOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Entry reqVO);

    void useDeptReviewOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Review reqVO);

    void qualityReviewOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Review reqVO);

    void techReviewOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Review reqVO);

    void purchaseReviewOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Review reqVO);

    void generalManagerReviewOnboardingApply(@Valid SrmOnboardingApplyActionReqVO.Review reqVO);

    Long createSurvey(@Valid SrmSurveySaveReqVO reqVO);

    void updateSurvey(@Valid SrmSurveySaveReqVO reqVO);

    void deleteSurvey(Long id);

    SrmSurveyDO getSurvey(Long id);

    PageResult<SrmSurveyDO> getSurveyPage(SrmSurveyPageReqVO reqVO);

    List<SrmSurveyReviewDO> getSurveyReviewList(Long surveyId);

    Long createDocument(@Valid SrmDocumentSaveReqVO reqVO);

    void updateDocument(@Valid SrmDocumentSaveReqVO reqVO);

    void deleteDocument(Long id);

    SrmDocumentDO getDocument(Long id);

    PageResult<SrmDocumentDO> getDocumentPage(SrmDocumentPageReqVO reqVO);

}
