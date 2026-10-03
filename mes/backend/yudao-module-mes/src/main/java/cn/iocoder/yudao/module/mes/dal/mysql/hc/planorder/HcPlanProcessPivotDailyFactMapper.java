package cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailySyncStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanProcessPivotDailyFactDO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcPlanProcessPivotDailyFactMapper extends BaseMapperX<HcPlanProcessPivotDailyFactDO> {

    record PivotFingerprint(String pivotKeyHash, String sourceContentHash) {
    }

    @Select("""
            SELECT *
            FROM mes_sfc_process_pivot_daily_fact
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
            ORDER BY plan_date DESC, plan_id DESC, segment_batch_no, id
            """)
    List<HcPlanProcessPivotDailyFactDO> selectActiveFacts(@Param("tenantId") Long tenantId);

    @Delete("""
            <script>
            DELETE FROM mes_sfc_process_pivot_daily_fact
            WHERE tenant_id = #{tenantId}
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            </script>
            """)
    int hardDeleteByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Select("""
            SELECT
              MAX(stat_date) AS latest_stat_date,
              MAX(sync_time) AS latest_sync_time,
              COUNT(1) AS settled_row_count
            FROM mes_sfc_process_pivot_daily_fact
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
            """)
    HcPlanProcessPivotDailySyncStatusRespVO selectSyncStatus(@Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT pivot_json
            FROM mes_sfc_process_pivot_daily_fact
            <where>
              tenant_id = #{tenantId}
              AND deleted = b'0'
              AND pivot_json IS NOT NULL
              <if test="req.planNo != null and req.planNo != ''">
                AND plan_no LIKE CONCAT('%', #{req.planNo}, '%')
              </if>
              <if test="req.planStatuses != null">
                AND plan_status IN
                <foreach collection="req.planStatuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="req.planStatuses == null">
                AND plan_status NOT IN ('CANCELLED', 'CANCELED', 'VOID')
              </if>
              <if test="req.planMode != null and req.planMode != ''">
                AND plan_mode = #{req.planMode}
              </if>
              <if test="req.sourceType != null and req.sourceType != ''">
                AND source_type = #{req.sourceType}
              </if>
              <if test="req.motherMaterialCode != null and req.motherMaterialCode != ''">
                AND mother_material_code LIKE CONCAT('%', #{req.motherMaterialCode}, '%')
              </if>
              <if test="req.motherModelCode != null and req.motherModelCode != ''">
                AND mother_model_code LIKE CONCAT('%', #{req.motherModelCode}, '%')
              </if>
              <if test="req.prodType != null and req.prodType != ''">
                AND prod_type = #{req.prodType}
              </if>
              <if test="req.categoryCode != null and req.categoryCode != ''">
                AND category_code = #{req.categoryCode}
              </if>
              <if test="req.modelCode != null and req.modelCode != ''">
                AND model_code = #{req.modelCode}
              </if>
              <if test="req.sizeSpec != null and req.sizeSpec != ''">
                AND size_spec = #{req.sizeSpec}
              </if>
              <if test="req.recipeCode != null and req.recipeCode != ''">
                AND recipe_code LIKE CONCAT('%', #{req.recipeCode}, '%')
              </if>
              <if test="req.planDateStart != null">
                AND plan_date &gt;= #{req.planDateStart}
              </if>
              <if test="req.planDateEnd != null">
                AND plan_date &lt;= #{req.planDateEnd}
              </if>
              <if test="req.productionStartDateStart != null">
                AND production_start_date &gt;= #{req.productionStartDateStart}
              </if>
              <if test="req.productionStartDateEnd != null">
                AND production_start_date &lt;= #{req.productionStartDateEnd}
              </if>
              <if test="req.productionEndDateStart != null">
                AND production_end_date &gt;= #{req.productionEndDateStart}
              </if>
              <if test="req.productionEndDateEnd != null">
                AND production_end_date &lt;= #{req.productionEndDateEnd}
              </if>
              <if test="req.createTimeStart != null">
                AND source_create_time &gt;= #{req.createTimeStart}
              </if>
              <if test="req.createTimeEnd != null">
                AND source_create_time &lt;= #{req.createTimeEnd}
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (
                  plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR material_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR model_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR model_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR sales_order_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR sales_order_erp_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR route_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR route_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR parent_production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR mother_roll_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR segment_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                )
              </if>
              <if test="req.materialKeyword != null and req.materialKeyword != ''">
                AND (
                  material_code LIKE CONCAT('%', #{req.materialKeyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.materialKeyword}, '%')
                )
              </if>
              <if test="req.salesOrderNo != null and req.salesOrderNo != ''">
                AND (
                  sales_order_no LIKE CONCAT('%', #{req.salesOrderNo}, '%')
                  OR sales_order_erp_no LIKE CONCAT('%', #{req.salesOrderNo}, '%')
                )
              </if>
              <if test="req.motherRollBatchNo != null and req.motherRollBatchNo != ''">
                AND (
                  parent_production_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR production_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR mother_roll_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                )
              </if>
              <if test="req.motherSegmentBatchNo != null and req.motherSegmentBatchNo != ''">
                AND segment_batch_no LIKE CONCAT('%', #{req.motherSegmentBatchNo}, '%')
              </if>
              <if test="req.routeKeyword != null and req.routeKeyword != ''">
                AND (
                  route_code LIKE CONCAT('%', #{req.routeKeyword}, '%')
                  OR route_name LIKE CONCAT('%', #{req.routeKeyword}, '%')
                )
              </if>
            </where>
            ORDER BY plan_date DESC, plan_id DESC, segment_batch_no, id
            <if test="limit != null and limit &gt; 0">
              LIMIT #{offset}, #{limit}
            </if>
            </script>
            """)
    List<String> selectPivotJsonList(@Param("tenantId") Long tenantId,
                                     @Param("req") HcPlanProcessPivotPageReqVO reqVO,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM mes_sfc_process_pivot_daily_fact
            <where>
              tenant_id = #{tenantId}
              AND deleted = b'0'
              AND pivot_json IS NOT NULL
              <if test="req.planNo != null and req.planNo != ''">
                AND plan_no LIKE CONCAT('%', #{req.planNo}, '%')
              </if>
              <if test="req.planStatuses != null">
                AND plan_status IN
                <foreach collection="req.planStatuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="req.planStatuses == null">
                AND plan_status NOT IN ('CANCELLED', 'CANCELED', 'VOID')
              </if>
              <if test="req.planMode != null and req.planMode != ''">
                AND plan_mode = #{req.planMode}
              </if>
              <if test="req.sourceType != null and req.sourceType != ''">
                AND source_type = #{req.sourceType}
              </if>
              <if test="req.motherMaterialCode != null and req.motherMaterialCode != ''">
                AND mother_material_code LIKE CONCAT('%', #{req.motherMaterialCode}, '%')
              </if>
              <if test="req.motherModelCode != null and req.motherModelCode != ''">
                AND mother_model_code LIKE CONCAT('%', #{req.motherModelCode}, '%')
              </if>
              <if test="req.prodType != null and req.prodType != ''">
                AND prod_type = #{req.prodType}
              </if>
              <if test="req.categoryCode != null and req.categoryCode != ''">
                AND category_code = #{req.categoryCode}
              </if>
              <if test="req.modelCode != null and req.modelCode != ''">
                AND model_code = #{req.modelCode}
              </if>
              <if test="req.sizeSpec != null and req.sizeSpec != ''">
                AND size_spec = #{req.sizeSpec}
              </if>
              <if test="req.recipeCode != null and req.recipeCode != ''">
                AND recipe_code LIKE CONCAT('%', #{req.recipeCode}, '%')
              </if>
              <if test="req.planDateStart != null">
                AND plan_date &gt;= #{req.planDateStart}
              </if>
              <if test="req.planDateEnd != null">
                AND plan_date &lt;= #{req.planDateEnd}
              </if>
              <if test="req.productionStartDateStart != null">
                AND production_start_date &gt;= #{req.productionStartDateStart}
              </if>
              <if test="req.productionStartDateEnd != null">
                AND production_start_date &lt;= #{req.productionStartDateEnd}
              </if>
              <if test="req.productionEndDateStart != null">
                AND production_end_date &gt;= #{req.productionEndDateStart}
              </if>
              <if test="req.productionEndDateEnd != null">
                AND production_end_date &lt;= #{req.productionEndDateEnd}
              </if>
              <if test="req.createTimeStart != null">
                AND source_create_time &gt;= #{req.createTimeStart}
              </if>
              <if test="req.createTimeEnd != null">
                AND source_create_time &lt;= #{req.createTimeEnd}
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (
                  plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR material_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR model_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR model_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR sales_order_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR sales_order_erp_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR route_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR route_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR parent_production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR mother_roll_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR segment_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                )
              </if>
              <if test="req.materialKeyword != null and req.materialKeyword != ''">
                AND (
                  material_code LIKE CONCAT('%', #{req.materialKeyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.materialKeyword}, '%')
                )
              </if>
              <if test="req.salesOrderNo != null and req.salesOrderNo != ''">
                AND (
                  sales_order_no LIKE CONCAT('%', #{req.salesOrderNo}, '%')
                  OR sales_order_erp_no LIKE CONCAT('%', #{req.salesOrderNo}, '%')
                )
              </if>
              <if test="req.motherRollBatchNo != null and req.motherRollBatchNo != ''">
                AND (
                  parent_production_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR production_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR mother_roll_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                )
              </if>
              <if test="req.motherSegmentBatchNo != null and req.motherSegmentBatchNo != ''">
                AND segment_batch_no LIKE CONCAT('%', #{req.motherSegmentBatchNo}, '%')
              </if>
              <if test="req.routeKeyword != null and req.routeKeyword != ''">
                AND (
                  route_code LIKE CONCAT('%', #{req.routeKeyword}, '%')
                  OR route_name LIKE CONCAT('%', #{req.routeKeyword}, '%')
                )
              </if>
            </where>
            </script>
            """)
    Long countPivotRows(@Param("tenantId") Long tenantId,
                        @Param("req") HcPlanProcessPivotPageReqVO reqVO);

    @Select("""
            <script>
            SELECT pivot_key_hash, source_content_hash
            FROM mes_sfc_process_pivot_daily_fact
            <where>
              tenant_id = #{tenantId}
              AND deleted = b'0'
              AND pivot_json IS NOT NULL
              <if test="req.planNo != null and req.planNo != ''">
                AND plan_no LIKE CONCAT('%', #{req.planNo}, '%')
              </if>
              <if test="req.planStatuses != null">
                AND plan_status IN
                <foreach collection="req.planStatuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="req.planStatuses == null">
                AND plan_status NOT IN ('CANCELLED', 'CANCELED', 'VOID')
              </if>
              <if test="req.planMode != null and req.planMode != ''">
                AND plan_mode = #{req.planMode}
              </if>
              <if test="req.sourceType != null and req.sourceType != ''">
                AND source_type = #{req.sourceType}
              </if>
              <if test="req.motherMaterialCode != null and req.motherMaterialCode != ''">
                AND mother_material_code LIKE CONCAT('%', #{req.motherMaterialCode}, '%')
              </if>
              <if test="req.motherModelCode != null and req.motherModelCode != ''">
                AND mother_model_code LIKE CONCAT('%', #{req.motherModelCode}, '%')
              </if>
              <if test="req.prodType != null and req.prodType != ''">
                AND prod_type = #{req.prodType}
              </if>
              <if test="req.categoryCode != null and req.categoryCode != ''">
                AND category_code = #{req.categoryCode}
              </if>
              <if test="req.modelCode != null and req.modelCode != ''">
                AND model_code = #{req.modelCode}
              </if>
              <if test="req.sizeSpec != null and req.sizeSpec != ''">
                AND size_spec = #{req.sizeSpec}
              </if>
              <if test="req.recipeCode != null and req.recipeCode != ''">
                AND recipe_code LIKE CONCAT('%', #{req.recipeCode}, '%')
              </if>
              <if test="req.planDateStart != null">
                AND plan_date &gt;= #{req.planDateStart}
              </if>
              <if test="req.planDateEnd != null">
                AND plan_date &lt;= #{req.planDateEnd}
              </if>
              <if test="req.productionStartDateStart != null">
                AND production_start_date &gt;= #{req.productionStartDateStart}
              </if>
              <if test="req.productionStartDateEnd != null">
                AND production_start_date &lt;= #{req.productionStartDateEnd}
              </if>
              <if test="req.productionEndDateStart != null">
                AND production_end_date &gt;= #{req.productionEndDateStart}
              </if>
              <if test="req.productionEndDateEnd != null">
                AND production_end_date &lt;= #{req.productionEndDateEnd}
              </if>
              <if test="req.createTimeStart != null">
                AND source_create_time &gt;= #{req.createTimeStart}
              </if>
              <if test="req.createTimeEnd != null">
                AND source_create_time &lt;= #{req.createTimeEnd}
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (
                  plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR material_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR model_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR model_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR sales_order_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR sales_order_erp_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR route_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR route_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR parent_production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR mother_roll_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR segment_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                )
              </if>
              <if test="req.materialKeyword != null and req.materialKeyword != ''">
                AND (
                  material_code LIKE CONCAT('%', #{req.materialKeyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.materialKeyword}, '%')
                )
              </if>
              <if test="req.salesOrderNo != null and req.salesOrderNo != ''">
                AND (
                  sales_order_no LIKE CONCAT('%', #{req.salesOrderNo}, '%')
                  OR sales_order_erp_no LIKE CONCAT('%', #{req.salesOrderNo}, '%')
                )
              </if>
              <if test="req.motherRollBatchNo != null and req.motherRollBatchNo != ''">
                AND (
                  parent_production_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR production_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR mother_roll_batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                  OR batch_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                )
              </if>
              <if test="req.motherSegmentBatchNo != null and req.motherSegmentBatchNo != ''">
                AND segment_batch_no LIKE CONCAT('%', #{req.motherSegmentBatchNo}, '%')
              </if>
              <if test="req.routeKeyword != null and req.routeKeyword != ''">
                AND (
                  route_code LIKE CONCAT('%', #{req.routeKeyword}, '%')
                  OR route_name LIKE CONCAT('%', #{req.routeKeyword}, '%')
                )
              </if>
            </where>
            </script>
            """)
    List<PivotFingerprint> selectFingerprints(@Param("tenantId") Long tenantId,
                                              @Param("req") HcPlanProcessPivotPageReqVO reqVO);
}
