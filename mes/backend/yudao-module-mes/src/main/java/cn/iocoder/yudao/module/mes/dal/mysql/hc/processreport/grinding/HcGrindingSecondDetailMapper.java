package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcGrindingSecondDetailMapper extends BaseMapperX<HcGrindingSecondDetailDO> {

    default List<HcGrindingSecondDetailDO> selectListByReportId(Long grindingReportId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getGrindingReportId, grindingReportId)
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingSecondDetailDO::getId));
    }

    default List<HcGrindingSecondDetailDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingSecondDetailDO::getId));
    }

    default List<HcGrindingSecondDetailDO> selectListByEquipmentAndDate(Long equipmentId, LocalDate reportDate) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getEquipmentId, equipmentId)
                .eqIfPresent(HcGrindingSecondDetailDO::getReportDate, reportDate)
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingSecondDetailDO::getStartTime)
                .orderByAsc(HcGrindingSecondDetailDO::getId));
    }

    default List<HcGrindingSecondDetailDO> selectListBySourceRowUid(Long planOperationId, String sourceRowUid) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingSecondDetailDO::getSourceRowUid, sourceRowUid)
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingSecondDetailDO::getId));
    }

    default HcGrindingSecondDetailDO selectByFirstAllocationId(Long firstAllocationId) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getFirstAllocationId, firstAllocationId)
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcGrindingSecondDetailDO selectByRowUid(Long planOperationId, String rowUid) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingSecondDetailDO::getRowUid, rowUid)
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByDesc(HcGrindingSecondDetailDO::getId)
                .last("LIMIT 1"));
    }

    default HcGrindingSecondDetailDO selectConfirmedByBatchNo(String batchNo) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .and(wrapper -> wrapper
                        .eq(HcGrindingSecondDetailDO::getProductionBatchNo, batchNo)
                        .or()
                        .eq(HcGrindingSecondDetailDO::getConfirmedBatchNo, batchNo)
                        .or()
                        .eq(HcGrindingSecondDetailDO::getMotherBatchNo, batchNo))
                .eq(HcGrindingSecondDetailDO::getConfirmStatus, "CONFIRMED")
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByDesc(HcGrindingSecondDetailDO::getConfirmTime)
                .orderByDesc(HcGrindingSecondDetailDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcGrindingSecondDetailDO> selectConfirmedListByPlanId(Long planId, Long downstreamPlanOperationId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getPlanId, planId)
                .eq(HcGrindingSecondDetailDO::getConfirmStatus, "CONFIRMED")
                .and(wrapper -> wrapper
                        .isNull(HcGrindingSecondDetailDO::getDownstreamPlanOperationId)
                        .or()
                        .eq(HcGrindingSecondDetailDO::getDownstreamPlanOperationId, downstreamPlanOperationId))
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingSecondDetailDO::getMotherBatchNo)
                .orderByAsc(HcGrindingSecondDetailDO::getSegmentMark)
                .orderByAsc(HcGrindingSecondDetailDO::getId));
    }

    @Update("""
            UPDATE mes_sfc_grinding_second_detail
            SET downstream_status = NULL,
                downstream_plan_id = NULL,
                downstream_plan_operation_id = NULL,
                downstream_report_id = NULL
            WHERE downstream_report_id = #{downstreamReportId}
              AND deleted = 0
            """)
    void releaseDownstreamByReportId(Long downstreamReportId);

    @Update("""
            UPDATE mes_sfc_grinding_second_detail
            SET downstream_status = NULL,
                downstream_plan_id = NULL,
                downstream_plan_operation_id = NULL,
                downstream_report_id = NULL
            WHERE id = #{id}
              AND deleted = 0
            """)
    void clearDownstreamStateById(Long id);

    @Update("""
            UPDATE mes_sfc_grinding_second_detail
            SET downstream_status = #{downstreamStatus},
                downstream_plan_id = #{downstreamPlanId},
                downstream_plan_operation_id = #{downstreamPlanOperationId},
                downstream_report_id = NULL
            WHERE id = #{id}
              AND deleted = 0
            """)
    void updateDownstreamStateWithoutReportId(@Param("id") Long id,
                                              @Param("downstreamStatus") String downstreamStatus,
                                              @Param("downstreamPlanId") Long downstreamPlanId,
                                              @Param("downstreamPlanOperationId") Long downstreamPlanOperationId);

    @Delete("DELETE FROM mes_sfc_grinding_second_detail WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Update("""
            UPDATE mes_sfc_grinding_second_detail
            SET report_date = #{reportDate},
                start_time = #{startTime},
                end_time = #{endTime}
            WHERE id = #{id}
              AND deleted = 0
            """)
    int updateReportTime(@Param("id") Long id,
                         @Param("reportDate") LocalDate reportDate,
                         @Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);
}
