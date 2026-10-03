package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotAbnormalLockDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HcPressSlotAbnormalLockMapper extends BaseMapperX<HcPressSlotAbnormalLockDO> {

    default HcPressSlotAbnormalLockDO selectByAbnormalFaiId(Long abnormalFaiId) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotAbnormalLockDO>()
                .eq(HcPressSlotAbnormalLockDO::getAbnormalFaiId, abnormalFaiId)
                .eq(HcPressSlotAbnormalLockDO::getDeleted, false)
                .last("LIMIT 1"));
    }

    /**
     * 重复键冲突后的当前读。
     *
     * <p>普通查询在 MySQL REPEATABLE READ 下可能继续读取冲突前的事务快照，
     * 看不到并发事务刚提交的锁定记录；FOR UPDATE 使用当前读，确保能够取得唯一键对应记录。</p>
     */
    default HcPressSlotAbnormalLockDO selectByAbnormalFaiIdForUpdate(Long abnormalFaiId) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotAbnormalLockDO>()
                .eq(HcPressSlotAbnormalLockDO::getAbnormalFaiId, abnormalFaiId)
                .eq(HcPressSlotAbnormalLockDO::getDeleted, false)
                .last("LIMIT 1 FOR UPDATE"));
    }

    default HcPressSlotAbnormalLockDO selectLatestActive(Long planOperationId, String motherBatchNo) {
        return selectOne(new LambdaQueryWrapperX<HcPressSlotAbnormalLockDO>()
                .eq(HcPressSlotAbnormalLockDO::getPlanOperationId, planOperationId)
                .eqIfPresent(HcPressSlotAbnormalLockDO::getMotherBatchNo, motherBatchNo)
                .eq(HcPressSlotAbnormalLockDO::getLockStatus, "LOCKED")
                .eq(HcPressSlotAbnormalLockDO::getDeleted, false)
                .orderByDesc(HcPressSlotAbnormalLockDO::getLockStartTime)
                .orderByDesc(HcPressSlotAbnormalLockDO::getId)
                .last("LIMIT 1"));
    }

    default List<HcPressSlotAbnormalLockDO> selectListByPlanOperation(Long planOperationId, String motherBatchNo) {
        return selectList(new LambdaQueryWrapperX<HcPressSlotAbnormalLockDO>()
                .eq(HcPressSlotAbnormalLockDO::getPlanOperationId, planOperationId)
                .eqIfPresent(HcPressSlotAbnormalLockDO::getMotherBatchNo, motherBatchNo)
                .eq(HcPressSlotAbnormalLockDO::getDeleted, false)
                .orderByDesc(HcPressSlotAbnormalLockDO::getLockStartTime)
                .orderByDesc(HcPressSlotAbnormalLockDO::getId));
    }
}
