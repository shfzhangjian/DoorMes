package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2PageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2SyncStatusRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldAnalysisSourceRow;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldDailyFactDO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface QmsYieldDailyFactMapper extends BaseMapperX<QmsYieldDailyFactDO> {

    @Select("""
            SELECT *
            FROM mes_qms_yield_daily_fact
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
            ORDER BY process_code, source_table, source_id, id
            """)
    List<QmsYieldDailyFactDO> selectActiveFacts(@Param("tenantId") Long tenantId);

    @Delete("""
            <script>
            DELETE FROM mes_qms_yield_daily_fact
            WHERE tenant_id = #{tenantId}
              AND id IN
              <foreach collection="ids" item="id" open="(" separator="," close=")">
                #{id}
              </foreach>
            </script>
            """)
    int hardDeleteByIds(@Param("tenantId") Long tenantId, @Param("ids") List<Long> ids);

    @Update("""
            UPDATE mes_qms_yield_daily_fact
            SET pivot_key = #{pivotKey},
                pivot_json = #{pivotJson},
                sync_batch_no = #{syncBatchNo},
                sync_time = #{syncTime},
                update_time = #{syncTime}
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
              AND pivot_key_hash = #{pivotKeyHash}
            """)
    int updatePivotSnapshot(@Param("tenantId") Long tenantId,
                            @Param("pivotKeyHash") String pivotKeyHash,
                            @Param("pivotKey") String pivotKey,
                            @Param("pivotJson") String pivotJson,
                            @Param("syncBatchNo") String syncBatchNo,
                            @Param("syncTime") LocalDateTime syncTime);

    @Select("""
            SELECT
              MAX(stat_date) AS latest_stat_date,
              MAX(sync_time) AS latest_sync_time,
              COUNT(DISTINCT CONCAT(process_code, '|', source_table, '|', source_id)) AS settled_source_count
            FROM mes_qms_yield_daily_fact
            WHERE tenant_id = #{tenantId}
              AND deleted = b'0'
            """)
    QmsYieldAnalysisV2SyncStatusRespVO selectSyncStatus(@Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT fact.pivot_json
            FROM mes_qms_yield_daily_fact fact
            INNER JOIN (
              SELECT MAX(id) AS fact_id, MAX(confirm_time) AS latest_confirm_time
              FROM mes_qms_yield_daily_fact
              <where>
                tenant_id = #{tenantId}
                AND deleted = b'0'
                AND pivot_json IS NOT NULL
                AND stat_date BETWEEN #{startDate} AND #{endDate}
                <if test="req.processCode != null and req.processCode != '' and req.processCode != 'ALL'">
                  AND process_code = #{req.processCode}
                </if>
                <if test="req.planNo != null and req.planNo != ''">
                  AND plan_no LIKE CONCAT('%', #{req.planNo}, '%')
                </if>
                <if test="req.motherRollBatchNo != null and req.motherRollBatchNo != ''">
                  AND mother_roll_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
                </if>
                <if test="req.motherSegmentBatchNo != null and req.motherSegmentBatchNo != ''">
                  AND segment_no LIKE CONCAT('%', #{req.motherSegmentBatchNo}, '%')
                </if>
                <if test="req.pieceNo != null and req.pieceNo != ''">
                  AND piece_no LIKE CONCAT('%', #{req.pieceNo}, '%')
                </if>
                <if test="req.modelCode != null and req.modelCode != ''">
                  AND model_code LIKE CONCAT('%', #{req.modelCode}, '%')
                </if>
                <if test="req.materialKeyword != null and req.materialKeyword != ''">
                  AND (material_code LIKE CONCAT('%', #{req.materialKeyword}, '%')
                    OR material_name LIKE CONCAT('%', #{req.materialKeyword}, '%')
                    OR model_code LIKE CONCAT('%', #{req.materialKeyword}, '%'))
                </if>
              </where>
              GROUP BY pivot_key_hash
              ORDER BY latest_confirm_time DESC, fact_id DESC
              <if test="limit != null and limit &gt; 0">
                LIMIT #{offset}, #{limit}
              </if>
            ) matched ON matched.fact_id = fact.id
            ORDER BY matched.latest_confirm_time DESC, matched.fact_id DESC
            </script>
            """)
    List<String> selectPivotJsonList(@Param("tenantId") Long tenantId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate,
                                     @Param("req") QmsYieldAnalysisV2PageReqVO reqVO,
                                     @Param("offset") Integer offset,
                                     @Param("limit") Integer limit);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT pivot_key_hash)
            FROM mes_qms_yield_daily_fact
            <where>
              tenant_id = #{tenantId}
              AND deleted = b'0'
              AND pivot_json IS NOT NULL
              AND stat_date BETWEEN #{startDate} AND #{endDate}
              <if test="req.processCode != null and req.processCode != '' and req.processCode != 'ALL'">
                AND process_code = #{req.processCode}
              </if>
              <if test="req.planNo != null and req.planNo != ''">
                AND plan_no LIKE CONCAT('%', #{req.planNo}, '%')
              </if>
              <if test="req.motherRollBatchNo != null and req.motherRollBatchNo != ''">
                AND mother_roll_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
              </if>
              <if test="req.motherSegmentBatchNo != null and req.motherSegmentBatchNo != ''">
                AND segment_no LIKE CONCAT('%', #{req.motherSegmentBatchNo}, '%')
              </if>
              <if test="req.pieceNo != null and req.pieceNo != ''">
                AND piece_no LIKE CONCAT('%', #{req.pieceNo}, '%')
              </if>
              <if test="req.modelCode != null and req.modelCode != ''">
                AND model_code LIKE CONCAT('%', #{req.modelCode}, '%')
              </if>
              <if test="req.materialKeyword != null and req.materialKeyword != ''">
                AND (material_code LIKE CONCAT('%', #{req.materialKeyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.materialKeyword}, '%')
                  OR model_code LIKE CONCAT('%', #{req.materialKeyword}, '%'))
              </if>
            </where>
            </script>
            """)
    Long countDistinctPivot(@Param("tenantId") Long tenantId,
                            @Param("startDate") LocalDate startDate,
                            @Param("endDate") LocalDate endDate,
                            @Param("req") QmsYieldAnalysisV2PageReqVO reqVO);

    @Select("""
            <script>
            SELECT
              process_code,
              MAX(process_name) AS process_name,
              source_table,
              source_id,
              MAX(plan_id) AS plan_id,
              MAX(plan_no) AS plan_no,
              MAX(plan_operation_id) AS plan_operation_id,
              MAX(mother_roll_no) AS mother_roll_no,
              MAX(segment_no) AS segment_no,
              MAX(piece_no) AS piece_no,
              MAX(material_code) AS material_code,
              MAX(material_name) AS material_name,
              MAX(model_code) AS model_code,
              MAX(input_qty) AS input_qty,
              MAX(output_good_qty) AS output_good_qty,
              MAX(output_ng_qty) AS output_ng_qty,
              MAX(self_check) AS self_check,
              MAX(defect_code) AS defect_code,
              MAX(visual_result_json) AS visual_result_json,
              MAX(extra_json) AS extra_json,
              MAX(submission_result) AS submission_result,
              MAX(report_status) AS report_status,
              MAX(confirm_time) AS confirm_time,
              MAX(inspection_id) AS inspection_id,
              MAX(inspection_no) AS inspection_no,
              MAX(inspection_source_type) AS inspection_source_type
            FROM mes_qms_yield_daily_fact
            <where>
              tenant_id = #{tenantId}
              AND deleted = b'0'
              AND stat_date BETWEEN #{startDate} AND #{endDate}
              <if test="req.processCode != null and req.processCode != '' and req.processCode != 'ALL'">
                AND process_code = #{req.processCode}
              </if>
              <if test="req.planNo != null and req.planNo != ''">
                AND plan_no LIKE CONCAT('%', #{req.planNo}, '%')
              </if>
              <if test="req.motherRollBatchNo != null and req.motherRollBatchNo != ''">
                AND mother_roll_no LIKE CONCAT('%', #{req.motherRollBatchNo}, '%')
              </if>
              <if test="req.motherSegmentBatchNo != null and req.motherSegmentBatchNo != ''">
                AND segment_no LIKE CONCAT('%', #{req.motherSegmentBatchNo}, '%')
              </if>
              <if test="req.pieceNo != null and req.pieceNo != ''">
                AND piece_no LIKE CONCAT('%', #{req.pieceNo}, '%')
              </if>
              <if test="req.modelCode != null and req.modelCode != ''">
                AND model_code LIKE CONCAT('%', #{req.modelCode}, '%')
              </if>
              <if test="req.materialKeyword != null and req.materialKeyword != ''">
                AND (material_code LIKE CONCAT('%', #{req.materialKeyword}, '%')
                  OR material_name LIKE CONCAT('%', #{req.materialKeyword}, '%')
                  OR model_code LIKE CONCAT('%', #{req.materialKeyword}, '%'))
              </if>
            </where>
            GROUP BY process_code, source_table, source_id
            ORDER BY MAX(confirm_time), source_table, source_id
            </script>
            """)
    List<QmsYieldAnalysisSourceRow> selectDistinctSourceRows(@Param("tenantId") Long tenantId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate,
                                                            @Param("req") QmsYieldAnalysisV2PageReqVO reqVO);
}
