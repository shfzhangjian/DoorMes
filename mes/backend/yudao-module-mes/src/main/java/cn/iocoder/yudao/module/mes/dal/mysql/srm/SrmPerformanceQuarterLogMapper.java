package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPerformanceQuarterLogMapper extends BaseMapperX<SrmPerformanceQuarterLogDO> {

    default List<SrmPerformanceQuarterLogDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmPerformanceQuarterLogDO>()
                .eq(SrmPerformanceQuarterLogDO::getEvaluationId, evaluationId)
                .orderByDesc(SrmPerformanceQuarterLogDO::getCreateTime)
                .orderByDesc(SrmPerformanceQuarterLogDO::getId));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmPerformanceQuarterLogDO::getEvaluationId, evaluationId);
    }

}
