package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationStatusLogDO;
import java.util.Collection;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HcPlanOrderOperationStatusLogMapper extends BaseMapperX<HcPlanOrderOperationStatusLogDO> {

    @Delete("DELETE FROM mes_pp_plan_operation_status_log WHERE plan_id = #{planId}")
    int physicalDeleteByPlanId(@Param("planId") Long planId);

    @Delete("""
            <script>
            DELETE FROM mes_pp_plan_operation_status_log
            WHERE plan_id IN
            <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
            </foreach>
            </script>
            """)
    int physicalDeleteByPlanIds(@Param("planIds") Collection<Long> planIds);

}
