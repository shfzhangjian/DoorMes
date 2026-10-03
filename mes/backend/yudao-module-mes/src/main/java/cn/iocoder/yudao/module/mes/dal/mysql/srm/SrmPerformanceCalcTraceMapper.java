package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceCalcTraceDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceCalcTraceMapper extends BaseMapperX<SrmPerformanceCalcTraceDO> {

    default List<SrmPerformanceCalcTraceDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceCalcTraceDO>()
                .eq(SrmPerformanceCalcTraceDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmPerformanceCalcTraceDO::getEvaluationItemId)
                .orderByAsc(SrmPerformanceCalcTraceDO::getPeriodMonth)
                .orderByAsc(SrmPerformanceCalcTraceDO::getId));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmPerformanceCalcTraceDO::getEvaluationId, evaluationId);
    }

    default void deleteByEvaluationItemId(Long evaluationItemId) {
        delete(SrmPerformanceCalcTraceDO::getEvaluationItemId, evaluationItemId);
    }

}
