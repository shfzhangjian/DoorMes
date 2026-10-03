package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotChangeoverInspectionDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPressSlotChangeoverInspectionMapper extends BaseMapperX<HcPressSlotChangeoverInspectionDO> {

    default List<HcPressSlotChangeoverInspectionDO> selectListByPlanOperation(Long planOperationId, String motherSegmentBatchNo) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotChangeoverInspectionDO>()
                .eq(HcPressSlotChangeoverInspectionDO::getPlanOperationId, planOperationId)
                .eqIfPresent(HcPressSlotChangeoverInspectionDO::getMotherSegmentBatchNo, motherSegmentBatchNo)
                .eq(HcPressSlotChangeoverInspectionDO::getDeleted, false)
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getSubmitTime)
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getId));
    }

    default HcPressSlotChangeoverInspectionDO selectLatestToday(Long planOperationId, LocalDateTime startTime, LocalDateTime endTime) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotChangeoverInspectionDO>()
                .eq(HcPressSlotChangeoverInspectionDO::getPlanOperationId, planOperationId)
                .between(HcPressSlotChangeoverInspectionDO::getSubmitTime, startTime, endTime)
                .eq(HcPressSlotChangeoverInspectionDO::getDeleted, false)
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getSubmitTime)
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getId)
                .last("LIMIT 1"));
    }

    default HcPressSlotChangeoverInspectionDO selectLatestTodayPressSlot(LocalDateTime startTime, LocalDateTime endTime) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotChangeoverInspectionDO>()
                .between(HcPressSlotChangeoverInspectionDO::getSubmitTime, startTime, endTime)
                .eq(HcPressSlotChangeoverInspectionDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .eq(HcPressSlotChangeoverInspectionDO::getOperationCode, "OP-PRESS-SLOT")
                        .or()
                        .like(HcPressSlotChangeoverInspectionDO::getOperationName, "压槽"))
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getSubmitTime)
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getId)
                .last("LIMIT 1"));
    }

    default HcPressSlotChangeoverInspectionDO selectLatestTodayAdhesive2(LocalDateTime startTime, LocalDateTime endTime) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotChangeoverInspectionDO>()
                .between(HcPressSlotChangeoverInspectionDO::getSubmitTime, startTime, endTime)
                .eq(HcPressSlotChangeoverInspectionDO::getDeleted, false)
                .and(wrapper -> wrapper
                        .eq(HcPressSlotChangeoverInspectionDO::getOperationCode, "OP-ADHESIVE2")
                        .or()
                        .like(HcPressSlotChangeoverInspectionDO::getOperationName, "粘胶2")
                        .or()
                        .like(HcPressSlotChangeoverInspectionDO::getOperationName, "背胶"))
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getSubmitTime)
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getId)
                .last("LIMIT 1"));
    }

    default HcPressSlotChangeoverInspectionDO selectActiveBySourceSlice(Long planOperationId,
                                                                         Long sourceSlittingSliceId,
                                                                         String pressSlotSliceNo) {
        if (sourceSlittingSliceId != null) {
            return selectOne(new LambdaQueryWrapperX<HcPressSlotChangeoverInspectionDO>()
                    .eq(HcPressSlotChangeoverInspectionDO::getPlanOperationId, planOperationId)
                    .eq(HcPressSlotChangeoverInspectionDO::getSourceSlittingSliceId, sourceSlittingSliceId)
                    .eq(HcPressSlotChangeoverInspectionDO::getDeleted, false)
                    .orderByDesc(HcPressSlotChangeoverInspectionDO::getId)
                    .last("LIMIT 1"));
        }
        if (pressSlotSliceNo == null || pressSlotSliceNo.trim().isEmpty()) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<HcPressSlotChangeoverInspectionDO>()
                .eq(HcPressSlotChangeoverInspectionDO::getPlanOperationId, planOperationId)
                .eq(HcPressSlotChangeoverInspectionDO::getPressSlotSliceNo, pressSlotSliceNo.trim())
                .eq(HcPressSlotChangeoverInspectionDO::getDeleted, false)
                .orderByDesc(HcPressSlotChangeoverInspectionDO::getId)
                .last("LIMIT 1"));
    }
}
