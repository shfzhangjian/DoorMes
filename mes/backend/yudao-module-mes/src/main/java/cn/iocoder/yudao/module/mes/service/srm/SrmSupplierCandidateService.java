package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierNameCheckRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierResourceStatusAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmSupplierResourceStatusLogRespVO;
import java.util.List;

public interface SrmSupplierCandidateService {

    PageResult<SrmSupplierCandidateRespVO> getCandidatePage(SrmSupplierCandidatePageReqVO reqVO);

    PageResult<SrmSupplierCandidateRespVO> getCandidateSelectPage(SrmSupplierCandidatePageReqVO reqVO);

    SrmSupplierNameCheckRespVO checkSupplierName(String supplierName);

    void adjustResourceStatus(SrmSupplierResourceStatusAdjustReqVO reqVO);

    List<SrmSupplierResourceStatusLogRespVO> getResourceStatusLogs(Long supplierId);

    SupplierResolution resolveSurveySupplier(Boolean unregisteredSupplier, Long supplierId,
            String supplierName, Boolean confirmInitialize,
            String sourceSurveyNo);

    void bindSupplierSourceToSurvey(Long supplierId, Long surveyId);

    record SupplierResolution(Long supplierId, String supplierCode, String supplierName,
                              String sourceType, Boolean unregisteredSupplier) {
    }

}
