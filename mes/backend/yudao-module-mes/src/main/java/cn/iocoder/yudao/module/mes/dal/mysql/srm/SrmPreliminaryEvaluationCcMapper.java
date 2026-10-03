package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationCcDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPreliminaryEvaluationCcMapper extends BaseMapperX<SrmPreliminaryEvaluationCcDO> {

    default List<SrmPreliminaryEvaluationCcDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmPreliminaryEvaluationCcDO>()
                .eq(SrmPreliminaryEvaluationCcDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmPreliminaryEvaluationCcDO::getId));
    }

    default SrmPreliminaryEvaluationCcDO selectByEvaluationIdAndUserId(Long evaluationId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmPreliminaryEvaluationCcDO>()
                .eq(SrmPreliminaryEvaluationCcDO::getEvaluationId, evaluationId)
                .eq(SrmPreliminaryEvaluationCcDO::getUserId, userId));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmPreliminaryEvaluationCcDO::getEvaluationId, evaluationId);
    }

}
