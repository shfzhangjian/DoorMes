package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleEvaluationLogMapper extends BaseMapperX<SrmSampleEvaluationLogDO> {

    default List<SrmSampleEvaluationLogDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmSampleEvaluationLogDO>()
                .eq(SrmSampleEvaluationLogDO::getEvaluationId, evaluationId)
                .orderByDesc(SrmSampleEvaluationLogDO::getCreateTime)
                .orderByDesc(SrmSampleEvaluationLogDO::getId));
    }

}
