package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationAvailableSupplierRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceTrendRespVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SrmPerformanceQuarterEvaluationService {

    Long createEvaluation(@Valid SrmPerformanceQuarterEvaluationSaveReqVO reqVO);

    List<SrmPerformanceQuarterEvaluationAvailableSupplierRespVO> getAvailableSuppliers(
            Integer evalYear, Integer evalQuarter, Long templateVersionId, String supplierInfo, String level);

    List<Long> batchCreateEvaluations(@Valid SrmPerformanceQuarterEvaluationBatchCreateReqVO reqVO);

    void updateEvaluation(@Valid SrmPerformanceQuarterEvaluationSaveReqVO reqVO);

    void deleteEvaluation(Long id);

    SrmPerformanceQuarterEvaluationRespVO getEvaluation(Long id);

    PageResult<SrmPerformanceQuarterEvaluationRespVO> getEvaluationPage(
            SrmPerformanceQuarterEvaluationPageReqVO reqVO);

    SrmPerformanceTrendRespVO getPerformanceTrend(Long evaluationId, Long supplierId, Integer evalYear,
                                                  String indicatorCodes);

    void pullActuals(Long id);

    void sendForScoring(Long id);

    void submitScore(@Valid SrmPerformanceQuarterEvaluationActionReqVO.Score reqVO);

    void calculateTotal(Long id);

    void startSign(@Valid SrmPerformanceQuarterEvaluationActionReqVO.StartSign reqVO);

    void submitSign(@Valid SrmPerformanceQuarterEvaluationActionReqVO.Sign reqVO);

}
