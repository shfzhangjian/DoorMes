package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierExitApprovalSaveReqVO;

public interface SrmSupplierExitApprovalService {

    Long createSupplierExitApproval(SrmSupplierExitApprovalSaveReqVO reqVO);

    void updateSupplierExitApproval(SrmSupplierExitApprovalSaveReqVO reqVO);

    void deleteSupplierExitApproval(Long id);

    SrmSupplierExitApprovalRespVO getSupplierExitApproval(Long id);

    PageResult<SrmSupplierExitApprovalRespVO> getSupplierExitApprovalPage(SrmSupplierExitApprovalPageReqVO reqVO);

    void submitSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Submit reqVO);

    void purchaseIntakeSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.PurchaseIntake reqVO);

    void signSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Sign reqVO);

    void purchaseTransferSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.PurchaseTransfer reqVO);

    void entrySupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Entry reqVO);

    void useDeptReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO);

    void qualityReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO);

    void techReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO);

    void purchaseReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO);

    void generalManagerReviewSupplierExitApproval(SrmSupplierExitApprovalActionReqVO.Review reqVO);

}
