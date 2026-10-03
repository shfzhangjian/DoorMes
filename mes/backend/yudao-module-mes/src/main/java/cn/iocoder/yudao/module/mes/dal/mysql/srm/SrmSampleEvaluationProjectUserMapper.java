package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationProjectUserDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleEvaluationProjectUserMapper extends BaseMapperX<SrmSampleEvaluationProjectUserDO> {

    default List<SrmSampleEvaluationProjectUserDO> selectListByProjectId(Long projectId) {
        return selectList(new LambdaQueryWrapperX<SrmSampleEvaluationProjectUserDO>()
                .eq(SrmSampleEvaluationProjectUserDO::getProjectId, projectId)
                .orderByAsc(SrmSampleEvaluationProjectUserDO::getSortNo)
                .orderByAsc(SrmSampleEvaluationProjectUserDO::getId));
    }

    default void deleteByProjectId(Long projectId) {
        delete(new LambdaQueryWrapperX<SrmSampleEvaluationProjectUserDO>()
                .eq(SrmSampleEvaluationProjectUserDO::getProjectId, projectId));
    }

    default SrmSampleEvaluationProjectUserDO selectInitiator(Long projectId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmSampleEvaluationProjectUserDO>()
                .eq(SrmSampleEvaluationProjectUserDO::getProjectId, projectId)
                .eq(SrmSampleEvaluationProjectUserDO::getUserId, userId)
                .eq(SrmSampleEvaluationProjectUserDO::getCanInitiate, Boolean.TRUE)
                .last("LIMIT 1"));
    }

    default SrmSampleEvaluationProjectUserDO selectAssigner(Long projectId, Long userId) {
        return selectOne(new LambdaQueryWrapperX<SrmSampleEvaluationProjectUserDO>()
                .eq(SrmSampleEvaluationProjectUserDO::getProjectId, projectId)
                .eq(SrmSampleEvaluationProjectUserDO::getUserId, userId)
                .eq(SrmSampleEvaluationProjectUserDO::getCanInitiate, Boolean.TRUE)
                .eq(SrmSampleEvaluationProjectUserDO::getCanAssign, Boolean.TRUE)
                .last("LIMIT 1"));
    }

}
