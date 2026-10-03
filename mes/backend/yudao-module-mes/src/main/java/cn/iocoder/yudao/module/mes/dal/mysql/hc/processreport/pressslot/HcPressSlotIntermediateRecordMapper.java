package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotIntermediateRecordDO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPressSlotIntermediateRecordMapper extends BaseMapperX<HcPressSlotIntermediateRecordDO> {

    default HcPressSlotIntermediateRecordDO selectByPlanOperationIdAndDate(Long planOperationId, LocalDate recordDate) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotIntermediateRecordDO>()
                .eq(HcPressSlotIntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcPressSlotIntermediateRecordDO::getRecordDate, recordDate)
                .eq(HcPressSlotIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcPressSlotIntermediateRecordDO::getId)
                .last("LIMIT 1"));
    }

    default HcPressSlotIntermediateRecordDO selectByPlanOperationIdAndBatchNo(Long planOperationId, String batchNo) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotIntermediateRecordDO>()
                .eq(HcPressSlotIntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcPressSlotIntermediateRecordDO::getBatchNo, batchNo)
                .eq(HcPressSlotIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcPressSlotIntermediateRecordDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcPressSlotIntermediateRecordDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotIntermediateRecordDO>()
                .eq(HcPressSlotIntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcPressSlotIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcPressSlotIntermediateRecordDO::getRecordDate)
                .orderByDesc(HcPressSlotIntermediateRecordDO::getId));
    }

    default List<HcPressSlotIntermediateRecordDO> selectListByPlanOperationIdAndBatchNo(Long planOperationId, String batchNo) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotIntermediateRecordDO>()
                .eq(HcPressSlotIntermediateRecordDO::getPlanOperationId, planOperationId)
                .eq(HcPressSlotIntermediateRecordDO::getBatchNo, batchNo)
                .eq(HcPressSlotIntermediateRecordDO::getDeleted, false)
                .orderByDesc(HcPressSlotIntermediateRecordDO::getRecordDate)
                .orderByDesc(HcPressSlotIntermediateRecordDO::getId));
    }
}
