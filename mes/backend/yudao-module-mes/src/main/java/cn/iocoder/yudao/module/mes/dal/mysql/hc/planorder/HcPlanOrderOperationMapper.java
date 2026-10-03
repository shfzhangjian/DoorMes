package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcPlanOrderOperationMapper extends BaseMapperX<HcPlanOrderOperationDO> {

    default List<HcPlanOrderOperationDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcPlanOrderOperationDO>()
                .eq(HcPlanOrderOperationDO::getPlanId, planId)
                .orderByAsc(HcPlanOrderOperationDO::getOpSeq)
                .orderByAsc(HcPlanOrderOperationDO::getSort)
                .orderByAsc(HcPlanOrderOperationDO::getId));
    }

    default HcPlanOrderOperationDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<HcPlanOrderOperationDO>()
                .eq(HcPlanOrderOperationDO::getId, id)
                .eq(HcPlanOrderOperationDO::getDeleted, false)
                .last("FOR UPDATE"));
    }

    default void deleteByPlanId(Long planId) {
        delete(new LambdaQueryWrapperX<HcPlanOrderOperationDO>().eq(HcPlanOrderOperationDO::getPlanId, planId));
    }

    default void deleteByPlanIds(Collection<Long> planIds) {
        delete(new LambdaQueryWrapperX<HcPlanOrderOperationDO>().in(HcPlanOrderOperationDO::getPlanId, planIds));
    }

    @Delete("DELETE FROM mes_pp_plan_operation WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);

    @Delete("""
            <script>
            DELETE FROM mes_pp_plan_operation
            WHERE plan_id IN
            <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
            </foreach>
            </script>
            """)
    int physicalDeleteByPlanIds(@Param("planIds") Collection<Long> planIds);

}
