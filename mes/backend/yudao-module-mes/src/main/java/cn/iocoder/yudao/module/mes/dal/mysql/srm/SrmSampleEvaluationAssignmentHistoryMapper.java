package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmSampleEvaluationAssignmentHistoryDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmSampleEvaluationAssignmentHistoryMapper extends BaseMapperX<SrmSampleEvaluationAssignmentHistoryDO> {

    default SrmSampleEvaluationAssignmentHistoryDO selectLatest(Long projectId, Long initiatorUserId,
                                                                Long initiatorDeptId) {
        LambdaQueryWrapperX<SrmSampleEvaluationAssignmentHistoryDO> query =
                new LambdaQueryWrapperX<SrmSampleEvaluationAssignmentHistoryDO>()
                .eq(SrmSampleEvaluationAssignmentHistoryDO::getProjectId, projectId)
                .eq(SrmSampleEvaluationAssignmentHistoryDO::getInitiatorUserId, initiatorUserId)
                .orderByDesc(SrmSampleEvaluationAssignmentHistoryDO::getAssignTime)
                .orderByDesc(SrmSampleEvaluationAssignmentHistoryDO::getId)
                .last("LIMIT 1");
        List<SrmSampleEvaluationAssignmentHistoryDO> histories = selectList(query);
        if (!histories.isEmpty() || initiatorDeptId == null) {
            return histories.isEmpty() ? null : histories.get(0);
        }
        return selectList(new LambdaQueryWrapperX<SrmSampleEvaluationAssignmentHistoryDO>()
                .eq(SrmSampleEvaluationAssignmentHistoryDO::getProjectId, projectId)
                .eq(SrmSampleEvaluationAssignmentHistoryDO::getInitiatorDeptId, initiatorDeptId)
                .orderByDesc(SrmSampleEvaluationAssignmentHistoryDO::getAssignTime)
                .orderByDesc(SrmSampleEvaluationAssignmentHistoryDO::getId)
                .last("LIMIT 1")).stream().findFirst().orElse(null);
    }

}
