package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceActualValueDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceActualValueMapper extends BaseMapperX<SrmPerformanceActualValueDO> {

    default List<SrmPerformanceActualValueDO> selectListByReportId(Long reportId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceActualValueDO>()
                .eq(SrmPerformanceActualValueDO::getReportId, reportId)
                .orderByAsc(SrmPerformanceActualValueDO::getMetricCode)
                .orderByAsc(SrmPerformanceActualValueDO::getId));
    }

    default List<SrmPerformanceActualValueDO> selectListByReportIds(List<Long> reportIds) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceActualValueDO>()
                .in(SrmPerformanceActualValueDO::getReportId, reportIds)
                .orderByAsc(SrmPerformanceActualValueDO::getMetricCode)
                .orderByAsc(SrmPerformanceActualValueDO::getId));
    }

    default void deleteByReportId(Long reportId) {
        delete(SrmPerformanceActualValueDO::getReportId, reportId);
    }

}
