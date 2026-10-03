package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcAdhesive2ReportMapper extends BaseMapperX<HcAdhesive2ReportDO> {

    default HcAdhesive2ReportDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getId, id)
                .last("FOR UPDATE"));
    }

    default List<HcAdhesive2ReportDO> selectListByPlanOperationId(Long planOperationId) {
        return selectListByPlanOperationId(planOperationId, null, null);
    }

    default List<HcAdhesive2ReportDO> selectListByPlanOperationId(Long planOperationId,
                                                                  LocalDateTime scanConfirmStart,
                                                                  LocalDateTime scanConfirmEndExclusive) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getPlanOperationId, planOperationId)
                .geIfPresent(HcAdhesive2ReportDO::getConfirmerTime, scanConfirmStart)
                .ltIfPresent(HcAdhesive2ReportDO::getConfirmerTime, scanConfirmEndExclusive)
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .orderByDesc(HcAdhesive2ReportDO::getId));
    }

    default List<HcAdhesive2ReportDO> selectListBySourcePressSlotReportId(Long sourcePressSlotReportId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getSourcePressSlotReportId, sourcePressSlotReportId)
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .orderByAsc(HcAdhesive2ReportDO::getId));
    }

    default List<HcAdhesive2ReportDO> selectListBySourcePressSlotReportIds(List<Long> sourcePressSlotReportIds) {
        if (sourcePressSlotReportIds == null || sourcePressSlotReportIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .in(HcAdhesive2ReportDO::getSourcePressSlotReportId, sourcePressSlotReportIds)
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .orderByDesc(HcAdhesive2ReportDO::getId));
    }

    default List<HcAdhesive2ReportDO> selectListByGlueBoardUsageId(Long glueBoardUsageId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getGlueBoardUsageId, glueBoardUsageId)
                .eq(HcAdhesive2ReportDO::getDeleted, false));
    }

    @Update("""
            UPDATE mes_sfc_adhesive2_report
            SET product_quality_status = 'NORMAL',
                quality_lock_reason = NULL
            WHERE id = #{id}
              AND deleted = 0
              AND product_quality_status IN ('QUALITY_ABNORMAL', 'LOCKED', 'ABNORMAL', 'NG')
              AND quality_lock_reason LIKE '胶板检验不合格%'
            """)
    int releaseGlueBoardInspectionQualityLock(@Param("id") Long id);

    default HcAdhesive2ReportDO selectByProductionBatchNo(String productionBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getProductionBatchNo, productionBatchNo)
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcAdhesive2ReportDO selectConfirmedByProductionBatchNo(String productionBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getProductionBatchNo, productionBatchNo)
                .in(HcAdhesive2ReportDO::getReportStatus, "CONFIRMED", "SUBMITTED")
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    /**
     * 查询可由后工序接收的不合格粘胶2片。NG 片不以粘胶2的确认状态作为前提，
     * 由后工序在扫码确认时固化来源风险与不良项目。
     */
    default HcAdhesive2ReportDO selectAbnormalByProductionBatchNo(String productionBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getProductionBatchNo, productionBatchNo)
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .in(HcAdhesive2ReportDO::getSelfCheck, "NG", "ABNORMAL", "FAILED", "不合格", "异常")
                        .or()
                        .isNotNull(HcAdhesive2ReportDO::getDefectCode)
                        .ne(HcAdhesive2ReportDO::getDefectCode, "")
                        .or()
                        .isNotNull(HcAdhesive2ReportDO::getQualityLockReason)
                        .ne(HcAdhesive2ReportDO::getQualityLockReason, "")
                        .or()
                        .in(HcAdhesive2ReportDO::getProductQualityStatus, "QUALITY_ABNORMAL", "ABNORMAL", "NG"))
                .orderByDesc(HcAdhesive2ReportDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcAdhesive2ReportDO> selectConfirmedListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getPlanId, planId)
                .in(HcAdhesive2ReportDO::getReportStatus, "CONFIRMED", "SUBMITTED")
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .orderByAsc(HcAdhesive2ReportDO::getSourceBatchNo)
                .orderByAsc(HcAdhesive2ReportDO::getProductionBatchNo)
                .orderByAsc(HcAdhesive2ReportDO::getId));
    }

    default List<HcAdhesive2ReportDO> selectAbnormalListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getPlanId, planId)
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .in(HcAdhesive2ReportDO::getSelfCheck, "NG", "ABNORMAL", "FAILED", "不合格", "异常")
                        .or()
                        .isNotNull(HcAdhesive2ReportDO::getDefectCode)
                        .ne(HcAdhesive2ReportDO::getDefectCode, "")
                        .or()
                        .isNotNull(HcAdhesive2ReportDO::getQualityLockReason)
                        .ne(HcAdhesive2ReportDO::getQualityLockReason, "")
                        .or()
                        .in(HcAdhesive2ReportDO::getProductQualityStatus, "QUALITY_ABNORMAL", "ABNORMAL", "NG"))
                .orderByAsc(HcAdhesive2ReportDO::getSourceBatchNo)
                .orderByAsc(HcAdhesive2ReportDO::getProductionBatchNo)
                .orderByAsc(HcAdhesive2ReportDO::getId));
    }
}
