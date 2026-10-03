package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanChangeoverLogDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPlanChangeoverLogMapper extends BaseMapperX<HcPlanChangeoverLogDO> {

    default HcPlanChangeoverLogDO selectLatestByPlanOperation(Long planOperationId) {
        return selectOne(new LambdaQueryWrapperX<HcPlanChangeoverLogDO>()
                .eq(HcPlanChangeoverLogDO::getPlanOperationId, planOperationId)
                .eq(HcPlanChangeoverLogDO::getChangeoverFlag, true)
                .eq(HcPlanChangeoverLogDO::getDeleted, false)
                .orderByDesc(HcPlanChangeoverLogDO::getChangeoverTime)
                .orderByDesc(HcPlanChangeoverLogDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcPlanChangeoverLogDO> selectListByPlanOperation(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcPlanChangeoverLogDO>()
                .eq(HcPlanChangeoverLogDO::getPlanOperationId, planOperationId)
                .eq(HcPlanChangeoverLogDO::getDeleted, false)
                .orderByDesc(HcPlanChangeoverLogDO::getChangeoverTime)
                .orderByDesc(HcPlanChangeoverLogDO::getId));
    }
}
