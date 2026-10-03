package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPreliminaryEvaluationLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmPreliminaryEvaluationLogMapper extends BaseMapperX<SrmPreliminaryEvaluationLogDO> {

    default List<SrmPreliminaryEvaluationLogDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmPreliminaryEvaluationLogDO>()
                .eq(SrmPreliminaryEvaluationLogDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmPreliminaryEvaluationLogDO::getCreateTime)
                .orderByAsc(SrmPreliminaryEvaluationLogDO::getId));
    }

}
