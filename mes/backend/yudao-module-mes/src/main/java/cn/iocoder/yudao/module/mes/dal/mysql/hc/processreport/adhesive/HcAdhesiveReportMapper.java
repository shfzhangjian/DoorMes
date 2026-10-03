package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcAdhesiveReportMapper extends BaseMapperX<HcAdhesiveReportDO> {

    default List<HcAdhesiveReportDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveReportDO>()
                .eq(HcAdhesiveReportDO::getPlanOperationId, planOperationId)
                .eq(HcAdhesiveReportDO::getDeleted, false)
                .orderByDesc(HcAdhesiveReportDO::getId));
    }

    default List<HcAdhesiveReportDO> selectListBySourceDetailId(Long sourceGrindingSecondDetailId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveReportDO>()
                .eq(HcAdhesiveReportDO::getSourceGrindingSecondDetailId, sourceGrindingSecondDetailId)
                .eq(HcAdhesiveReportDO::getDeleted, false)
                .orderByAsc(HcAdhesiveReportDO::getStartPosition)
                .orderByAsc(HcAdhesiveReportDO::getId));
    }

    default List<HcAdhesiveReportDO> selectSubmittedListByPlanId(Long planId) {
        return selectSubmittedListByPlanId(planId, null);
    }

    default List<HcAdhesiveReportDO> selectSubmittedListByPlanId(Long planId, String productionBatchNo) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveReportDO>()
                .eq(HcAdhesiveReportDO::getPlanId, planId)
                .eqIfPresent(HcAdhesiveReportDO::getProductionBatchNo, productionBatchNo)
                .in(HcAdhesiveReportDO::getReportStatus, "CONFIRMED", "SUBMITTED")
                .eq(HcAdhesiveReportDO::getDeleted, false)
                .orderByAsc(HcAdhesiveReportDO::getSourceProductionBatchNo)
                .orderByAsc(HcAdhesiveReportDO::getId));
    }

    default List<HcAdhesiveReportDO> selectListByGlueBoardUsageId(Long glueBoardUsageId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesiveReportDO>()
                .eq(HcAdhesiveReportDO::getGlueBoardUsageId, glueBoardUsageId)
                .eq(HcAdhesiveReportDO::getDeleted, false)
                .orderByAsc(HcAdhesiveReportDO::getGlueBoardStartPosition)
                .orderByAsc(HcAdhesiveReportDO::getId));
    }

    @Update("""
            UPDATE mes_sfc_adhesive_report
            SET product_quality_status = 'NORMAL',
                quality_lock_reason = NULL
            WHERE id = #{id}
              AND deleted = 0
              AND product_quality_status IN ('QUALITY_ABNORMAL', 'LOCKED', 'ABNORMAL', 'NG')
              AND quality_lock_reason LIKE '胶板检验不合格%'
            """)
    int releaseGlueBoardInspectionQualityLock(@Param("id") Long id);

    @Update("""
            UPDATE mes_sfc_adhesive_report
            SET slitting_remaining_length = #{remainingLength}
            WHERE id = #{id}
              AND deleted = 0
              AND slitting_remaining_length IS NULL
            """)
    int initializeSlittingRemainingLengthIfAbsent(@Param("id") Long id,
                                                  @Param("remainingLength") BigDecimal remainingLength);

    @Update("""
            UPDATE mes_sfc_adhesive_report
            SET slitting_remaining_length = GREATEST(
              COALESCE(slitting_remaining_length, COALESCE(output_length, input_length, 0)) - #{deductLength},
              0
            )
            WHERE id = #{id}
              AND deleted = 0
            """)
    int deductSlittingRemainingLength(@Param("id") Long id,
                                      @Param("deductLength") BigDecimal deductLength);
}
