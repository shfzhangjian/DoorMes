package cn.iocoder.yudao.module.mes.service.srm;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportAvailableSupplierRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportSaveReqVO;
import jakarta.validation.Valid;
import java.util.List;

public interface SrmPerformanceActualReportService {

    Long createReport(@Valid SrmPerformanceActualReportSaveReqVO reqVO);

    void updateReport(@Valid SrmPerformanceActualReportSaveReqVO reqVO);

    void deleteReport(Long id);

    SrmPerformanceActualReportRespVO getReport(Long id);

    PageResult<SrmPerformanceActualReportRespVO> getReportPage(SrmPerformanceActualReportPageReqVO reqVO);

    List<SrmPerformanceActualReportAvailableSupplierRespVO> getAvailableSuppliers(
            String periodType, Integer evalYear, Integer evalQuarter, Integer evalMonth, String supplierInfo);

    List<Long> batchCreateReports(@Valid SrmPerformanceActualReportBatchCreateReqVO reqVO);

    void submitReport(Long id);

    void confirmReport(@Valid SrmPerformanceActualReportActionReqVO.Confirm reqVO);

    void rejectReport(@Valid SrmPerformanceActualReportActionReqVO.Confirm reqVO);

    void pullMonthlyValues(Long id);

}
