package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcPlanOrderCascadeDeleteMapper {

    @Select("""
            <script>
            SELECT id
            FROM ${tableName}
            WHERE deleted = b'0'
              AND ${columnName} IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    List<Long> selectIdsByLongColumn(@Param("tableName") String tableName,
                                     @Param("columnName") String columnName,
                                     @Param("ids") Collection<Long> ids);

    @Select("""
            <script>
            SELECT id
            FROM mes_pp_plan_split_order
            WHERE deleted = b'0'
              AND (
                source_plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                </foreach>
                OR target_plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                </foreach>
              )
            </script>
            """)
    List<Long> selectPlanSplitOrderIdsByPlanIds(@Param("planIds") Collection<Long> planIds);

    @Update("""
            <script>
            UPDATE ${tableName}
            SET deleted = b'1',
                updater = #{updater},
                update_time = NOW()
            WHERE deleted = b'0'
              AND ${columnName} IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int logicalDeleteByLongColumn(@Param("tableName") String tableName,
                                  @Param("columnName") String columnName,
                                  @Param("ids") Collection<Long> ids,
                                  @Param("updater") String updater);

    @Delete("""
            <script>
            DELETE FROM ${tableName}
            WHERE ${columnName} IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
            </foreach>
            </script>
            """)
    int physicalDeleteByLongColumn(@Param("tableName") String tableName,
                                   @Param("columnName") String columnName,
                                   @Param("ids") Collection<Long> ids);

}
