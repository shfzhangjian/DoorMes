package cn.iocoder.yudao.module.mes.dal.mysql.srm;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmTrialValidationLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SrmTrialValidationLogMapper extends BaseMapperX<SrmTrialValidationLogDO> {

    default List<SrmTrialValidationLogDO> selectListByTrialId(Long trialId) {
        return selectList(new LambdaQueryWrapperX<SrmTrialValidationLogDO>()
                .eq(SrmTrialValidationLogDO::getTrialId, trialId)
                .orderByAsc(SrmTrialValidationLogDO::getCreateTime)
                .orderByAsc(SrmTrialValidationLogDO::getId));
    }

}
