package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceActualReportPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceActualReportDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceActualReportMapper extends BaseMapperX<SrmPerformanceActualReportDO> {

    default PageResult<SrmPerformanceActualReportDO> selectPage(SrmPerformanceActualReportPageReqVO reqVO) {
        LambdaQueryWrapperX<SrmPerformanceActualReportDO> query = new LambdaQueryWrapperX<SrmPerformanceActualReportDO>()
                .likeIfPresent(SrmPerformanceActualReportDO::getReportNo, reqVO.getReportNo())
                .eqIfPresent(SrmPerformanceActualReportDO::getSupplierId, reqVO.getSupplierId())
                .likeIfPresent(SrmPerformanceActualReportDO::getSupplierCode, reqVO.getSupplierCode())
                .likeIfPresent(SrmPerformanceActualReportDO::getSupplierName, reqVO.getSupplierName())
                .eqIfPresent(SrmPerformanceActualReportDO::getPeriodType, reqVO.getPeriodType())
                .eqIfPresent(SrmPerformanceActualReportDO::getEvalYear, reqVO.getEvalYear())
                .eqIfPresent(SrmPerformanceActualReportDO::getEvalQuarter, reqVO.getEvalQuarter())
                .eqIfPresent(SrmPerformanceActualReportDO::getEvalMonth, reqVO.getEvalMonth())
                .eqIfPresent(SrmPerformanceActualReportDO::getStatus, reqVO.getStatus())
                .eqIfPresent(SrmPerformanceActualReportDO::getReporterUserId, reqVO.getReporterUserId());
        if (StrUtil.isNotBlank(reqVO.getSupplierInfo())) {
            query.and(q -> q.like(SrmPerformanceActualReportDO::getSupplierCode, reqVO.getSupplierInfo())
                    .or()
                    .like(SrmPerformanceActualReportDO::getSupplierName, reqVO.getSupplierInfo()));
        }
        query.orderByDesc(SrmPerformanceActualReportDO::getUpdateTime)
                .orderByDesc(SrmPerformanceActualReportDO::getId);
        return selectPage(reqVO, query);
    }

    default SrmPerformanceActualReportDO selectByReportNo(String reportNo) {
        return selectOne(SrmPerformanceActualReportDO::getReportNo, reportNo);
    }

    default List<SrmPerformanceActualReportDO> selectConfirmedByQuarter(Long supplierId, Integer evalYear,
                                                                        Integer evalQuarter) {
        List<SrmPerformanceActualReportDO> quarterReports = selectList(new LambdaQueryWrapperX<SrmPerformanceActualReportDO>()
                .eq(SrmPerformanceActualReportDO::getSupplierId, supplierId)
                .eq(SrmPerformanceActualReportDO::getPeriodType, "QUARTER")
                .eq(SrmPerformanceActualReportDO::getEvalYear, evalYear)
                .eq(SrmPerformanceActualReportDO::getEvalQuarter, evalQuarter)
                .eq(SrmPerformanceActualReportDO::getStatus, "CONFIRMED")
                .orderByAsc(SrmPerformanceActualReportDO::getId));
        if (!quarterReports.isEmpty()) {
            return quarterReports;
        }
        return selectConfirmedMonthlyByQuarter(supplierId, evalYear, evalQuarter);
    }

    default List<SrmPerformanceActualReportDO> selectConfirmedMonthlyByQuarter(Long supplierId, Integer evalYear,
                                                                               Integer evalQuarter) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceActualReportDO>()
                .eq(SrmPerformanceActualReportDO::getSupplierId, supplierId)
                .eq(SrmPerformanceActualReportDO::getPeriodType, "MONTH")
                .eq(SrmPerformanceActualReportDO::getEvalYear, evalYear)
                .eq(SrmPerformanceActualReportDO::getEvalQuarter, evalQuarter)
                .eq(SrmPerformanceActualReportDO::getStatus, "CONFIRMED")
                .orderByAsc(SrmPerformanceActualReportDO::getEvalMonth)
                .orderByAsc(SrmPerformanceActualReportDO::getId));
    }

    default List<SrmPerformanceActualReportDO> selectMonthlyBySupplierYear(Long supplierId, Integer evalYear) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceActualReportDO>()
                .eq(SrmPerformanceActualReportDO::getSupplierId, supplierId)
                .eq(SrmPerformanceActualReportDO::getPeriodType, "MONTH")
                .eq(SrmPerformanceActualReportDO::getEvalYear, evalYear)
                .orderByAsc(SrmPerformanceActualReportDO::getEvalMonth)
                .orderByAsc(SrmPerformanceActualReportDO::getId));
    }

    default List<SrmPerformanceActualReportDO> selectListByPeriod(String periodType, Integer evalYear,
                                                                  Integer evalQuarter, Integer evalMonth) {
        LambdaQueryWrapperX<SrmPerformanceActualReportDO> query = new LambdaQueryWrapperX<SrmPerformanceActualReportDO>()
                .eq(SrmPerformanceActualReportDO::getPeriodType, periodType)
                .eq(SrmPerformanceActualReportDO::getEvalYear, evalYear)
                .eqIfPresent(SrmPerformanceActualReportDO::getEvalQuarter, evalQuarter);
        if ("MONTH".equals(periodType)) {
            query.eq(SrmPerformanceActualReportDO::getEvalMonth, evalMonth);
        } else {
            query.isNull(SrmPerformanceActualReportDO::getEvalMonth);
        }
        return selectList(query.orderByDesc(SrmPerformanceActualReportDO::getUpdateTime)
                .orderByDesc(SrmPerformanceActualReportDO::getId));
    }

}
