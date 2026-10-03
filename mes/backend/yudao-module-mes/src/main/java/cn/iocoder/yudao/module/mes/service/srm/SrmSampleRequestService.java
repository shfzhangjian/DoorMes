package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSupplierPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSampleRequestSupplierRespVO;
import jakarta.validation.Valid;

public interface SrmSampleRequestService {

    Long createSampleRequest(@Valid SrmSampleRequestSaveReqVO reqVO);

    void updateSampleRequest(@Valid SrmSampleRequestSaveReqVO reqVO);

    void deleteSampleRequest(Long id);

    SrmSampleRequestRespVO getSampleRequest(Long id);

    PageResult<SrmSampleRequestRespVO> getSampleRequestPage(SrmSampleRequestPageReqVO reqVO);

    PageResult<SrmSampleRequestSupplierRespVO> getSupplierPage(SrmSampleRequestSupplierPageReqVO reqVO);

    void submit(@Valid SrmSampleRequestActionReqVO.Submit reqVO);

    void projectReview(@Valid SrmSampleRequestActionReqVO.Review reqVO);

    void purchaseReview(@Valid SrmSampleRequestActionReqVO.Review reqVO);

    void initiatorDecision(@Valid SrmSampleRequestActionReqVO.InitiatorDecision reqVO);

    void finalApprove(@Valid SrmSampleRequestActionReqVO.Review reqVO);

    void archiveConfirm(@Valid SrmSampleRequestActionReqVO.ArchiveConfirm reqVO);

}
