package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceQuarterItemMapper extends BaseMapperX<SrmPerformanceQuarterItemDO> {

    default List<SrmPerformanceQuarterItemDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceQuarterItemDO>()
                .eq(SrmPerformanceQuarterItemDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmPerformanceQuarterItemDO::getGroupSort)
                .orderByAsc(SrmPerformanceQuarterItemDO::getIndicatorSort)
                .orderByAsc(SrmPerformanceQuarterItemDO::getId));
    }

    default List<SrmPerformanceQuarterItemDO> selectListByScorerId(Long scorerUserId, String scoreStatus) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceQuarterItemDO>()
                .eq(SrmPerformanceQuarterItemDO::getScorerUserId, scorerUserId)
                .eqIfPresent(SrmPerformanceQuarterItemDO::getScoreStatus, scoreStatus));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmPerformanceQuarterItemDO::getEvaluationId, evaluationId);
    }

}
