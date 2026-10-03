package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderStatusLogDO;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcPlanOrderStatusLogMapper extends BaseMapperX<HcPlanOrderStatusLogDO> {

    default List<HcPlanOrderStatusLogDO> selectListByPlanId(Long planId) {
        return selectList(new LambdaQueryWrapperX<HcPlanOrderStatusLogDO>()
                .eq(HcPlanOrderStatusLogDO::getPlanId, planId)
                .orderByDesc(HcPlanOrderStatusLogDO::getOperateTime)
                .orderByDesc(HcPlanOrderStatusLogDO::getId));
    }

    @Delete("DELETE FROM mes_pp_plan_order_status_log WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);

    @Delete("""
            <script>
            DELETE FROM mes_pp_plan_order_status_log
            WHERE plan_id IN
            <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
            </foreach>
            </script>
            """)
    int physicalDeleteByPlanIds(@Param("planIds") Collection<Long> planIds);

}
