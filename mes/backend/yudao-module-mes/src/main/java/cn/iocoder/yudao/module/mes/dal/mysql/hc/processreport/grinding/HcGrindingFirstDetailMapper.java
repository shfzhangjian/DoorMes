package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstDetailDO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcGrindingFirstDetailMapper extends BaseMapperX<HcGrindingFirstDetailDO> {

    default List<HcGrindingFirstDetailDO> selectListByReportId(Long grindingReportId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingFirstDetailDO>()
                .eq(HcGrindingFirstDetailDO::getGrindingReportId, grindingReportId)
                .eq(HcGrindingFirstDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingFirstDetailDO::getId));
    }

    default List<HcGrindingFirstDetailDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcGrindingFirstDetailDO>()
                .eq(HcGrindingFirstDetailDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingFirstDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingFirstDetailDO::getId));
    }

    default List<HcGrindingFirstDetailDO> selectListByEquipmentAndDate(Long equipmentId, LocalDate reportDate) {
        return selectList(new LambdaQueryWrapperX<HcGrindingFirstDetailDO>()
                .eq(HcGrindingFirstDetailDO::getEquipmentId, equipmentId)
                .eqIfPresent(HcGrindingFirstDetailDO::getReportDate, reportDate)
                .eq(HcGrindingFirstDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingFirstDetailDO::getStartTime)
                .orderByAsc(HcGrindingFirstDetailDO::getId));
    }

    default HcGrindingFirstDetailDO selectByRowUid(Long planOperationId, String rowUid) {
        return selectOne(new LambdaQueryWrapperX<HcGrindingFirstDetailDO>()
                .eq(HcGrindingFirstDetailDO::getPlanOperationId, planOperationId)
                .eq(HcGrindingFirstDetailDO::getRowUid, rowUid)
                .eq(HcGrindingFirstDetailDO::getDeleted, false)
                .orderByDesc(HcGrindingFirstDetailDO::getId)
                .last("LIMIT 1"));
    }

    @Delete("DELETE FROM mes_sfc_grinding_first_detail WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);

    @Update("""
            UPDATE mes_sfc_grinding_first_detail
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
