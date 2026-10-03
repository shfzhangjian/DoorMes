package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPressSlotReportMapper extends BaseMapperX<HcPressSlotReportDO> {

    default List<HcPressSlotReportDO> selectListByPlanOperationId(Long planOperationId) {
        return selectListByPlanOperationId(planOperationId, null, null);
    }

    default List<HcPressSlotReportDO> selectListByPlanOperationId(Long planOperationId,
                                                                  LocalDateTime scanConfirmStart,
                                                                  LocalDateTime scanConfirmEndExclusive) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getPlanOperationId, planOperationId)
                .geIfPresent(HcPressSlotReportDO::getConfirmerTime, scanConfirmStart)
                .ltIfPresent(HcPressSlotReportDO::getConfirmerTime, scanConfirmEndExclusive)
                .eq(HcPressSlotReportDO::getDeleted, false)
                .orderByDesc(HcPressSlotReportDO::getId));
    }

    default List<HcPressSlotReportDO> selectConfirmedListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getPlanOperationId, planOperationId)
                .in(HcPressSlotReportDO::getReportStatus, "CONFIRMED", "SUBMITTED")
                .eq(HcPressSlotReportDO::getDeleted, false)
                .orderByAsc(HcPressSlotReportDO::getConfirmerTime)
                .orderByAsc(HcPressSlotReportDO::getUpdateTime)
                .orderByAsc(HcPressSlotReportDO::getCreateTime)
                .orderByAsc(HcPressSlotReportDO::getId));
    }

    default List<HcPressSlotReportDO> selectListBySourceSliceId(Long sourceSlittingSliceId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getSourceSlittingSliceId, sourceSlittingSliceId)
                .eq(HcPressSlotReportDO::getDeleted, false)
                .orderByAsc(HcPressSlotReportDO::getId));
    }

    default List<HcPressSlotReportDO> selectListBySourceSliceIds(List<Long> sourceSlittingSliceIds) {
        if (sourceSlittingSliceIds == null || sourceSlittingSliceIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .in(HcPressSlotReportDO::getSourceSlittingSliceId, sourceSlittingSliceIds)
                .eq(HcPressSlotReportDO::getDeleted, false)
                .orderByDesc(HcPressSlotReportDO::getId));
    }

    default HcPressSlotReportDO selectConfirmedByProductionBatchNo(String productionBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getProductionBatchNo, productionBatchNo)
                .in(HcPressSlotReportDO::getReportStatus, "CONFIRMED", "SUBMITTED")
                .eq(HcPressSlotReportDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default HcPressSlotReportDO selectByProductionBatchNo(String productionBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getProductionBatchNo, productionBatchNo)
                .eq(HcPressSlotReportDO::getDeleted, false)
                .orderByDesc(HcPressSlotReportDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcPressSlotReportDO> selectConfirmedListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getPlanId, planId)
                .in(HcPressSlotReportDO::getReportStatus, "CONFIRMED", "SUBMITTED")
                .eq(HcPressSlotReportDO::getDeleted, false)
                .orderByAsc(HcPressSlotReportDO::getSourceProductionBatchNo)
                .orderByAsc(HcPressSlotReportDO::getProductionBatchNo)
                .orderByAsc(HcPressSlotReportDO::getId));
    }

    default List<HcPressSlotReportDO> selectAbnormalListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getPlanId, planId)
                .eq(HcPressSlotReportDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .in(HcPressSlotReportDO::getSelfCheck, "NG", "ABNORMAL", "FAILED", "不合格", "异常")
                        .or()
                        .isNotNull(HcPressSlotReportDO::getDefectCode)
                        .ne(HcPressSlotReportDO::getDefectCode, ""))
                .orderByAsc(HcPressSlotReportDO::getSourceProductionBatchNo)
                .orderByAsc(HcPressSlotReportDO::getProductionBatchNo)
                .orderByAsc(HcPressSlotReportDO::getId));
    }

    default List<HcPressSlotReportDO> selectConfirmedListByReportDate(LocalDate reportDate) {
        LocalDate date = reportDate == null ? LocalDate.now() : reportDate;
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime nextDayStart = dayStart.plusDays(1);
        return selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .in(HcPressSlotReportDO::getReportStatus, "CONFIRMED", "SUBMITTED")
                .eq(HcPressSlotReportDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .eq(HcPressSlotReportDO::getReportDate, date)
                        .or()
                        .ge(HcPressSlotReportDO::getConfirmerTime, dayStart)
                        .lt(HcPressSlotReportDO::getConfirmerTime, nextDayStart)
                        .or()
                        .ge(HcPressSlotReportDO::getUpdateTime, dayStart)
                        .lt(HcPressSlotReportDO::getUpdateTime, nextDayStart)
                        .or()
                        .ge(HcPressSlotReportDO::getCreateTime, dayStart)
                        .lt(HcPressSlotReportDO::getCreateTime, nextDayStart))
                .orderByDesc(HcPressSlotReportDO::getConfirmerTime)
                .orderByDesc(HcPressSlotReportDO::getUpdateTime)
                .orderByDesc(HcPressSlotReportDO::getId));
    }
}
