package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingAllocationModeDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcGrindingAllocationModeMapper extends BaseMapperX<HcGrindingAllocationModeDO> {

    default HcGrindingAllocationModeDO selectByMotherBatch(Long planOperationId, String motherBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingAllocationModeDO>()
                .eq(HcGrindingAllocationModeDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingAllocationModeDO::getMotherBatchNo, motherBatchNo)
                .eq(HcGrindingAllocationModeDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    @Select("""
            SELECT *
            FROM mes_sfc_grinding_allocation_mode
            WHERE plan_operation_id = #{planOperationId}
              AND mother_batch_no = #{motherBatchNo}
              AND deleted = 0
            LIMIT 1
            FOR UPDATE
            """)
    HcGrindingAllocationModeDO selectByMotherBatchForUpdate(@Param("planOperationId") Long planOperationId,
                                                             @Param("motherBatchNo") String motherBatchNo);
}
