package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2TailSelectionDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcAdhesive2TailSelectionMapper extends BaseMapperX<HcAdhesive2TailSelectionDO> {

    default List<HcAdhesive2TailSelectionDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2TailSelectionDO>()
                .eq(HcAdhesive2TailSelectionDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesive2TailSelectionDO::getDeleted, false)
                .orderByAsc(HcAdhesive2TailSelectionDO::getId));
    }

    default HcAdhesive2TailSelectionDO selectByPlanOperationIdAndSourceBatchNo(Long planOperationId,
                                                                                 String sourceProductionBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesive2TailSelectionDO>()
                .eq(HcAdhesive2TailSelectionDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesive2TailSelectionDO::getSourceProductionBatchNo, sourceProductionBatchNo)
                .eq(HcAdhesive2TailSelectionDO::getDeleted, false)
                .last("LIMIT 1"));
    }
}
