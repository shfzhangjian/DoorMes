package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSegmentTimingDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcGrindingSegmentTimingMapper extends BaseMapperX<HcGrindingSegmentTimingDO> {

    default List<HcGrindingSegmentTimingDO> selectListByPlanOperationId(Long planOperationId, String motherBatchNo,
                                                                         String passType) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSegmentTimingDO>()
                .eq(HcGrindingSegmentTimingDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingSegmentTimingDO::getMotherBatchNo, motherBatchNo)
                .eq(HcGrindingSegmentTimingDO::getPassType, passType)
                .eq(HcGrindingSegmentTimingDO::getDeleted, false)
                .orderByAsc(HcGrindingSegmentTimingDO::getSegmentMark)
                .orderByAsc(HcGrindingSegmentTimingDO::getId));
    }

    default HcGrindingSegmentTimingDO selectBySegment(Long planOperationId, String motherBatchNo, String passType,
                                                       String segmentMark) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingSegmentTimingDO>()
                .eq(HcGrindingSegmentTimingDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingSegmentTimingDO::getMotherBatchNo, motherBatchNo)
                .eq(HcGrindingSegmentTimingDO::getPassType, passType)
                .eq(HcGrindingSegmentTimingDO::getSegmentMark, segmentMark)
                .eq(HcGrindingSegmentTimingDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<HcGrindingSegmentTimingDO> selectListByFirstDetailId(Long firstDetailId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSegmentTimingDO>()
                .eq(HcGrindingSegmentTimingDO::getFirstDetailId, firstDetailId)
                .eq(HcGrindingSegmentTimingDO::getDeleted, false)
                .orderByAsc(HcGrindingSegmentTimingDO::getSegmentMark)
                .orderByAsc(HcGrindingSegmentTimingDO::getId));
    }

    @Update("""
            UPDATE mes_sfc_grinding_segment_timing
            SET start_time = #{startTime},
                start_operator_id = #{operatorId},
                start_operator_name = #{operatorName}
            WHERE id = #{id}
              AND start_time IS NULL
              AND deleted = 0
              AND tenant_id = #{tenantId}
            """)
    int markStartOnce(@Param("id") Long id,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("operatorId") Long operatorId,
                      @Param("operatorName") String operatorName,
                      @Param("tenantId") Long tenantId);

    @Update("""
            UPDATE mes_sfc_grinding_segment_timing
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

    @Update("""
            UPDATE mes_sfc_grinding_segment_timing
            SET first_detail_id = NULL
            WHERE first_detail_id = #{firstDetailId}
              AND deleted = 0
            """)
    int clearFirstDetailId(@Param("firstDetailId") Long firstDetailId);
}
