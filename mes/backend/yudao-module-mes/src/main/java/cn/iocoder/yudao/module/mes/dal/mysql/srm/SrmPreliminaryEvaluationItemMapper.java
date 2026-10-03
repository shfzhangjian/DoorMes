package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationItemDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPreliminaryEvaluationItemMapper extends BaseMapperX<SrmPreliminaryEvaluationItemDO> {

    default List<SrmPreliminaryEvaluationItemDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmPreliminaryEvaluationItemDO>()
                .eq(SrmPreliminaryEvaluationItemDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmPreliminaryEvaluationItemDO::getGroupSort)
                .orderByAsc(SrmPreliminaryEvaluationItemDO::getIndicatorSort)
                .orderByAsc(SrmPreliminaryEvaluationItemDO::getId));
    }

    default List<SrmPreliminaryEvaluationItemDO> selectListByScorerId(Long scorerUserId, String scoreStatus) {
        return selectList(new LambdaQueryWrapperX<SrmPreliminaryEvaluationItemDO>()
                .eq(SrmPreliminaryEvaluationItemDO::getScorerUserId, scorerUserId)
                .eqIfPresent(SrmPreliminaryEvaluationItemDO::getScoreStatus, scoreStatus));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmPreliminaryEvaluationItemDO::getEvaluationId, evaluationId);
    }

}
