package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationSignDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleEvaluationSignMapper extends BaseMapperX<SrmSampleEvaluationSignDO> {

    default List<SrmSampleEvaluationSignDO> selectListByEvaluationId(Long evaluationId) {
        return selectList(new LambdaQueryWrapperX<SrmSampleEvaluationSignDO>()
                .eq(SrmSampleEvaluationSignDO::getEvaluationId, evaluationId)
                .orderByAsc(SrmSampleEvaluationSignDO::getDeptName)
                .orderByAsc(SrmSampleEvaluationSignDO::getUserName)
                .orderByAsc(SrmSampleEvaluationSignDO::getId));
    }

    default SrmSampleEvaluationSignDO selectByEvaluationIdAndUserId(Long evaluationId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmSampleEvaluationSignDO>()
                .eq(SrmSampleEvaluationSignDO::getEvaluationId, evaluationId)
                .eq(SrmSampleEvaluationSignDO::getUserId, userId)
                .eq(SrmSampleEvaluationSignDO::getSignStatus, "PENDING")
                .last("LIMIT 1"));
    }

    default List<SrmSampleEvaluationSignDO> selectPendingListByEvaluationIdAndUserId(Long evaluationId, Long userId) {
        return selectList(new LambdaQueryWrapperX<SrmSampleEvaluationSignDO>()
                .eq(SrmSampleEvaluationSignDO::getEvaluationId, evaluationId)
                .eq(SrmSampleEvaluationSignDO::getUserId, userId)
                .eq(SrmSampleEvaluationSignDO::getSignStatus, "PENDING")
                .orderByAsc(SrmSampleEvaluationSignDO::getId));
    }

    default void deleteByEvaluationId(Long evaluationId) {
        delete(SrmSampleEvaluationSignDO::getEvaluationId, evaluationId);
    }

}
