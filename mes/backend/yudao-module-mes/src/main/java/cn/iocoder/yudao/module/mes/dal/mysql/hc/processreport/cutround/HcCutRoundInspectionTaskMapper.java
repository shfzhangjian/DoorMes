package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionTaskDO;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcCutRoundInspectionTaskMapper extends BaseMapperX<HcCutRoundInspectionTaskDO> {

    default List<HcCutRoundInspectionTaskDO> selectListByPlanOperationId(Long planOperationId) {
        if (planOperationId == null) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcCutRoundInspectionTaskDO>()
                .eq(HcCutRoundInspectionTaskDO::getPlanOperationId, planOperationId)
                .eq(HcCutRoundInspectionTaskDO::getDeleted, false)
                .orderByDesc(HcCutRoundInspectionTaskDO::getId));
    }
}
