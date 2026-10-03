package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QmsAbnormalLockImpactMapper {

    @Select("""
            <script>
            SELECT
              COALESCE(l.source_id, l.id) AS sourceId,
              'LOT_INSTANCE' AS impactSourceType,
              COALESCE(NULLIF(l.source_table, ''), 'mes_lot_instance') AS sourceTable,
              CONCAT(COALESCE(NULLIF(l.source_table, ''), 'mes_lot_instance'), ':', COALESCE(l.source_id, l.id), ':',
                COALESCE(NULLIF(l.production_batch_no, ''), NULLIF(l.lot_no, ''))) AS sourceKey,
              COALESCE(NULLIF(l.production_batch_no, ''), NULLIF(l.lot_no, '')) AS affectedBatchNo,
              l.parent_production_batch_no AS sourceBatchNo,
              l.production_batch_no AS productionBatchNo,
              l.parent_production_batch_no AS parentProductionBatchNo,
              l.plan_id AS planId,
              l.plan_no AS planNo,
              l.plan_operation_id AS planOperationId,
              l.operation_code AS operationCode,
              l.operation_name AS operationName,
              l.operation_seq AS processOrder,
              NULL AS glueBoardUsageId,
              NULL AS glueBoardBatchNo,
              NULL AS glueBoardStartPosition,
              NULL AS glueBoardUseLength,
              COALESCE(l.generated_time, l.create_time) AS eventTime,
              l.attribute_json AS snapshotJson
            FROM mes_lot_instance l
            WHERE l.deleted = 0
              AND l.tenant_id = #{tenantId}
              AND (
                l.lot_no IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                  #{batchNo}
                </foreach>
                OR l.production_batch_no IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                  #{batchNo}
                </foreach>
                OR l.parent_lot_no IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                  #{batchNo}
                </foreach>
                OR l.parent_production_batch_no IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                  #{batchNo}
                </foreach>
                OR (#{branchPrefix} IS NOT NULL AND #{branchPrefix} != '' AND (
                  l.lot_no LIKE CONCAT(#{branchPrefix}, '%')
                  OR l.production_batch_no LIKE CONCAT(#{branchPrefix}, '%')
                  OR l.parent_lot_no LIKE CONCAT(#{branchPrefix}, '%')
                  OR l.parent_production_batch_no LIKE CONCAT(#{branchPrefix}, '%')
                ))
              )
              AND (#{sinceTime} IS NULL OR COALESCE(l.generated_time, l.create_time) IS NULL OR COALESCE(l.generated_time, l.create_time) &gt;= #{sinceTime})
            ORDER BY COALESCE(l.operation_seq, 999), COALESCE(l.generated_time, l.create_time), l.id
            LIMIT 2000
            </script>
            """)
    List<QmsAbnormalLockImpactDTO> selectLotImpacts(@Param("batchNos") Collection<String> batchNos,
                                                    @Param("branchPrefix") String branchPrefix,
                                                    @Param("tenantId") Long tenantId,
                                                    @Param("sinceTime") LocalDateTime sinceTime);

    @Select("""
            <script>
            SELECT *
            FROM (
              SELECT
                a.id AS sourceId,
                'ADHESIVE1_GLUE_BOARD' AS impactSourceType,
                'mes_sfc_adhesive_report' AS sourceTable,
                CONCAT('ADHESIVE1_GLUE_BOARD:', a.id) AS sourceKey,
                a.production_batch_no AS affectedBatchNo,
                COALESCE(NULLIF(a.source_production_batch_no, ''), NULLIF(a.source_batch_no, '')) AS sourceBatchNo,
                a.production_batch_no AS productionBatchNo,
                a.parent_production_batch_no AS parentProductionBatchNo,
                a.plan_id AS planId,
                a.plan_no AS planNo,
                a.plan_operation_id AS planOperationId,
                a.operation_code AS operationCode,
                COALESCE(NULLIF(a.operation_name, ''), '粘胶1') AS operationName,
                50 AS processOrder,
                a.glue_board_usage_id AS glueBoardUsageId,
                a.glue_board_batch_no AS glueBoardBatchNo,
                a.glue_board_start_position AS glueBoardStartPosition,
                a.glue_board_use_length AS glueBoardUseLength,
                COALESCE(a.confirmer_time, a.end_time, a.recorder_time, a.start_time, a.create_time) AS eventTime,
                a.extra_json AS snapshotJson
              FROM mes_sfc_adhesive_report a
              WHERE a.deleted = 0
                AND a.tenant_id = #{tenantId}
              UNION ALL
              SELECT
                a2.id AS sourceId,
                'ADHESIVE2_GLUE_BOARD' AS impactSourceType,
                'mes_sfc_adhesive2_report' AS sourceTable,
                CONCAT('ADHESIVE2_GLUE_BOARD:', a2.id) AS sourceKey,
                a2.production_batch_no AS affectedBatchNo,
                COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, '')) AS sourceBatchNo,
                a2.production_batch_no AS productionBatchNo,
                a2.parent_production_batch_no AS parentProductionBatchNo,
                a2.plan_id AS planId,
                a2.plan_no AS planNo,
                a2.plan_operation_id AS planOperationId,
                a2.operation_code AS operationCode,
                COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS operationName,
                80 AS processOrder,
                a2.glue_board_usage_id AS glueBoardUsageId,
                a2.glue_board_batch_no AS glueBoardBatchNo,
                a2.glue_board_start_position AS glueBoardStartPosition,
                a2.glue_board_use_length AS glueBoardUseLength,
                COALESCE(a2.confirmer_time, a2.end_time, a2.recorder_time, a2.start_time, a2.create_time) AS eventTime,
                a2.extra_json AS snapshotJson
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND a2.tenant_id = #{tenantId}
            ) glue_rows
            WHERE (
              (#{glueBoardUsageId} IS NOT NULL AND glue_rows.glueBoardUsageId = #{glueBoardUsageId})
              OR (#{glueBoardBatchNo} IS NOT NULL AND #{glueBoardBatchNo} != '' AND glue_rows.glueBoardBatchNo = #{glueBoardBatchNo})
            )
              AND (#{sinceTime} IS NULL OR glue_rows.eventTime IS NULL OR glue_rows.eventTime &gt;= #{sinceTime})
            ORDER BY glue_rows.processOrder, glue_rows.eventTime, glue_rows.sourceId
            LIMIT 1000
            </script>
            """)
    List<QmsAbnormalLockImpactDTO> selectGlueBoardImpacts(@Param("glueBoardBatchNo") String glueBoardBatchNo,
                                                          @Param("glueBoardUsageId") Long glueBoardUsageId,
                                                          @Param("tenantId") Long tenantId,
                                                          @Param("sinceTime") LocalDateTime sinceTime);
}
