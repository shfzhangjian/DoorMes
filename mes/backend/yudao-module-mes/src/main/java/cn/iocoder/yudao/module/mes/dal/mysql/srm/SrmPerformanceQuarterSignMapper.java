package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterSignDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceQuarterSignMapper extends BaseMapperX<SrmPerformanceQuarterSignDO> {

    default List<SrmPerformanceQuarterSignDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceQuarterSignDO>()
                .eq(SrmPerformanceQuarterSignDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmPerformanceQuarterSignDO::getId));
    }

    default List<SrmPerformanceQuarterSignDO> selectPendingByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceQuarterSignDO>()
                .eq(SrmPerformanceQuarterSignDO::getUserId, userId)
                .eq(SrmPerformanceQuarterSignDO::getSignStatus, "PENDING"));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmPerformanceQuarterSignDO::getEvaluationId, evaluationId);
    }

}
