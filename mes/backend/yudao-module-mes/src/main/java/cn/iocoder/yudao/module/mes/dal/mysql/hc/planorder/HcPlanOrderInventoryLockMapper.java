package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcPlanOrderInventoryLockMapper extends BaseMapperX<HcPlanOrderInventoryLockDO> {

    default List<HcPlanOrderInventoryLockDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .eq(HcPlanOrderInventoryLockDO::getPlanId, planId)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .orderByAsc(HcPlanOrderInventoryLockDO::getPlanOperationId)
                .orderByAsc(HcPlanOrderInventoryLockDO::getId));
    }

    default List<HcPlanOrderInventoryLockDO> selectListByPlanIds(Collection<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .in(HcPlanOrderInventoryLockDO::getPlanId, planIds)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .orderByAsc(HcPlanOrderInventoryLockDO::getPlanId)
                .orderByAsc(HcPlanOrderInventoryLockDO::getPlanOperationId)
                .orderByAsc(HcPlanOrderInventoryLockDO::getId));
    }

    default List<HcPlanOrderInventoryLockDO> selectListByPlanOperationId(Long planOperationId) {
        return selectList(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .eq(HcPlanOrderInventoryLockDO::getPlanOperationId, planOperationId)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .orderByAsc(HcPlanOrderInventoryLockDO::getSourcePlanNo)
                .orderByAsc(HcPlanOrderInventoryLockDO::getSourceBatchNo)
                .orderByAsc(HcPlanOrderInventoryLockDO::getId));
    }

    default List<HcPlanOrderInventoryLockDO> selectListBySource(String sourceTable, Long sourceId) {
        return selectList(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .eq(HcPlanOrderInventoryLockDO::getSourceTable, sourceTable)
                .eq(HcPlanOrderInventoryLockDO::getSourceId, sourceId)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .orderByDesc(HcPlanOrderInventoryLockDO::getId));
    }

    default List<HcPlanOrderInventoryLockDO> selectListByStockId(Long stockId) {
        return selectList(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .eq(HcPlanOrderInventoryLockDO::getStockId, stockId)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .orderByDesc(HcPlanOrderInventoryLockDO::getId));
    }

    default List<HcPlanOrderInventoryLockDO> selectListByStockIds(Collection<Long> stockIds) {
        if (stockIds == null || stockIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .in(HcPlanOrderInventoryLockDO::getStockId, stockIds)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .orderByDesc(HcPlanOrderInventoryLockDO::getId));
    }

    default List<HcPlanOrderInventoryLockDO> selectUsedListByNgPieceIdsForUpdate(Collection<Long> ngPieceIds) {
        if (ngPieceIds == null || ngPieceIds.isEmpty()) {
            return List.of();
        }
        return selectList(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .in(HcPlanOrderInventoryLockDO::getNgPieceId, ngPieceIds)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .in(HcPlanOrderInventoryLockDO::getLockStatus, List.of("ACTIVE", "CONSUMED"))
                .last("FOR UPDATE"));
    }

    default HcPlanOrderInventoryLockDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>()
                .eq(HcPlanOrderInventoryLockDO::getId, id)
                .eq(HcPlanOrderInventoryLockDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default void deleteByPlanId(Long planId) {
        delete(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>().eq(HcPlanOrderInventoryLockDO::getPlanId, planId));
    }

    default void deleteByPlanIds(Collection<Long> planIds) {
        delete(new LambdaQueryWrapperX<HcPlanOrderInventoryLockDO>().in(HcPlanOrderInventoryLockDO::getPlanId, planIds));
    }

    @Delete("DELETE FROM mes_pp_plan_inv_lock WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);

    @Delete("""
            <script>
            DELETE FROM mes_pp_plan_inv_lock
            WHERE plan_id IN
            <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
            </foreach>
            </script>
            """)
    int physicalDeleteByPlanIds(@Param("planIds") Collection<Long> planIds);

}
