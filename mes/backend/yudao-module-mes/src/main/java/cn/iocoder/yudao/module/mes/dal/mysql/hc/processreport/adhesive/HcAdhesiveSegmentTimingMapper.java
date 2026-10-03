package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveSegmentTimingDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcAdhesiveSegmentTimingMapper extends BaseMapperX<HcAdhesiveSegmentTimingDO> {

    default List<HcAdhesiveSegmentTimingDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveSegmentTimingDO>()
                .eq(HcAdhesiveSegmentTimingDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveSegmentTimingDO::getDeleted, false)
                .orderByAsc(HcAdhesiveSegmentTimingDO::getSegmentMark)
                .orderByAsc(HcAdhesiveSegmentTimingDO::getId));
    }

    default HcAdhesiveSegmentTimingDO selectBySegment(Long planOperationId, Long sourceGrindingSecondDetailId) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesiveSegmentTimingDO>()
                .eq(HcAdhesiveSegmentTimingDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveSegmentTimingDO::getSourceGrindingSecondDetailId, sourceGrindingSecondDetailId)
                .eq(HcAdhesiveSegmentTimingDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    @Update("""
            UPDATE mes_sfc_adhesive_segment_timing
            SET end_time = #{endTime},
                end_operator_id = #{operatorId},
                end_operator_name = #{operatorName}
            WHERE id = #{id}
              AND start_time IS NOT NULL
              AND end_time IS NULL
              AND deleted = 0
              AND tenant_id = #{tenantId}
            """)
    int markEndOnce(@Param("id") Long id,
                    @Param("endTime") LocalDateTime endTime,
                    @Param("operatorId") Long operatorId,
                    @Param("operatorName") String operatorName,
                    @Param("tenantId") Long tenantId);

}
