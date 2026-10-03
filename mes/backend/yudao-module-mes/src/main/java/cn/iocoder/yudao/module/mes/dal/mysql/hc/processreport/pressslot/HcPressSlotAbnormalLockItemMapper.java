package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotAbnormalLockItemDO;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPressSlotAbnormalLockItemMapper extends BaseMapperX<HcPressSlotAbnormalLockItemDO> {

    default HcPressSlotAbnormalLockItemDO selectByLockIdAndReportId(Long lockId, Long reportId) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotAbnormalLockItemDO>()
                .eq(HcPressSlotAbnormalLockItemDO::getLockId, lockId)
                .eq(HcPressSlotAbnormalLockItemDO::getReportId, reportId)
                .eq(HcPressSlotAbnormalLockItemDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    /**
     * 重复键冲突后的当前读，避免事务快照看不到并发事务刚写入的片号明细。
     */
    default HcPressSlotAbnormalLockItemDO selectByLockIdAndReportIdForUpdate(Long lockId, Long reportId) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotAbnormalLockItemDO>()
                .eq(HcPressSlotAbnormalLockItemDO::getLockId, lockId)
                .eq(HcPressSlotAbnormalLockItemDO::getReportId, reportId)
                .eq(HcPressSlotAbnormalLockItemDO::getDeleted, false)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default HcPressSlotAbnormalLockItemDO selectActiveByReportId(Long reportId) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotAbnormalLockItemDO>()
                .eq(HcPressSlotAbnormalLockItemDO::getReportId, reportId)
                .eq(HcPressSlotAbnormalLockItemDO::getLockStatus, "LOCKED")
                .eq(HcPressSlotAbnormalLockItemDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    default List<HcPressSlotAbnormalLockItemDO> selectListByLockId(Long lockId) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotAbnormalLockItemDO>()
                .eq(HcPressSlotAbnormalLockItemDO::getLockId, lockId)
                .eq(HcPressSlotAbnormalLockItemDO::getDeleted, false)
                .orderByAsc(HcPressSlotAbnormalLockItemDO::getConfirmTime)
                .orderByAsc(HcPressSlotAbnormalLockItemDO::getId));
    }

    default List<HcPressSlotAbnormalLockItemDO> selectListByLockIds(Collection<Long> lockIds) {
        if (lockIds == null || lockIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<HcPressSlotAbnormalLockItemDO>()
                .in(HcPressSlotAbnormalLockItemDO::getLockId, lockIds)
                .eq(HcPressSlotAbnormalLockItemDO::getDeleted, false)
                .orderByAsc(HcPressSlotAbnormalLockItemDO::getConfirmTime)
                .orderByAsc(HcPressSlotAbnormalLockItemDO::getId));
    }
}
