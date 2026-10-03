package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.discrete;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcDiscretePostProcessVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcDiscretePostProcessMapper {

    @Select("""
            <script>
            SELECT *
            FROM (
            SELECT
                CONCAT('S:', s.id) AS candidateKey,
                'STOCK' AS candidateType,
                '库存可用' AS candidateStatus,
                NULL AS sourceLockId,
                NULL AS sourceLockPlanId,
                NULL AS sourceLockPlanNo,
                NULL AS sourceLockOperationId,
                s.id AS stockId,
                s.stock_type AS stockType,
                s.source_type AS sourceType,
                s.source_table AS sourceTable,
                s.source_id AS sourceId,
                s.source_report_id AS sourceReportId,
                s.source_plan_id AS sourcePlanId,
                s.source_plan_no AS sourcePlanNo,
                s.source_plan_operation_id AS sourcePlanOperationId,
                s.source_batch_no AS sourceBatchNo,
                s.source_parent_batch_no AS sourceParentBatchNo,
                s.batch_no AS batchNo,
                s.material_id AS materialId,
                s.material_code AS materialCode,
                s.material_name AS materialName,
                s.recipe_code AS recipeCode,
                s.recipe_name AS recipeName,
                s.model_no AS modelNo,
                s.spec_size AS specSize,
                s.op_seq AS opSeq,
                s.op_code AS opCode,
                s.op_name AS opName,
                s.segment_code AS segmentCode,
                s.segment_name AS segmentName,
                s.thickness AS thickness,
                s.on_hand_qty AS onHandQty,
                s.available_qty AS availableQty,
                s.shareable_qty AS shareableQty,
                s.frozen_qty AS frozenQty,
                s.plan_locked_qty AS planLockedQty,
                s.quality_status AS qualityStatus,
                s.biz_status AS bizStatus,
                s.uom AS uom,
                s.location_code AS locationCode,
                s.location_name AS locationName,
                s.production_date AS productionDate,
                s.expiry_date AS expiryDate,
                s.last_txn_time AS lastTxnTime,
                COALESCE(
                    sl.scan_time,
                    ps.confirmer_time, ps.end_time, ps.recorder_time, ps.update_time, ps.create_time,
                    a2.confirmer_time, a2.end_time, a2.recorder_time, a2.update_time, a2.create_time,
                    cr.confirmer_time, cr.end_time, cr.recorder_time, cr.update_time, cr.create_time,
                    s.last_txn_time
                ) AS reportTime
            FROM mes_inv_stock s
            LEFT JOIN mes_sfc_slitting_slice_record sl
                   ON sl.id = COALESCE(s.source_report_id, s.source_id)
                  AND (s.source_table = 'mes_sfc_slitting_slice_record' OR s.source_type = 'SLITTING')
                  AND sl.deleted = b'0'
            LEFT JOIN mes_sfc_press_slot_report ps
                   ON ps.id = COALESCE(s.source_report_id, s.source_id)
                  AND (s.source_table = 'mes_sfc_press_slot_report' OR s.source_type = 'PRESS_SLOT')
                  AND ps.deleted = b'0'
            LEFT JOIN mes_sfc_adhesive2_report a2
                   ON a2.id = COALESCE(s.source_report_id, s.source_id)
                  AND (s.source_table = 'mes_sfc_adhesive2_report' OR s.source_type = 'ADHESIVE2')
                  AND a2.deleted = b'0'
            LEFT JOIN mes_sfc_cut_round_report cr
                   ON cr.id = COALESCE(s.source_report_id, s.source_id)
                  AND (s.source_table = 'mes_sfc_cut_round_report' OR s.source_type = 'CUT_ROUND')
                  AND cr.deleted = b'0'
            WHERE s.deleted = b'0'
              AND s.stock_type = 'WIP'
              AND s.available_qty &gt; 0
              <if test="req.sourceOpCode != null and req.sourceOpCode != ''">
                AND s.op_code = #{req.sourceOpCode}
              </if>
              <if test="req.sourceTypes != null and req.sourceTypes.size() &gt; 0">
                AND s.source_type IN
                <foreach collection="req.sourceTypes" item="sourceType" open="(" separator="," close=")">
                  #{sourceType}
                </foreach>
              </if>
              <if test="req.materialCode != null and req.materialCode != ''">
                AND s.material_code LIKE CONCAT('%', #{req.materialCode}, '%')
              </if>
              <if test="req.locationKeyword != null and req.locationKeyword != ''">
                AND (
                    s.location_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR s.location_name LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR s.warehouse_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR s.warehouse_name LIKE CONCAT('%', #{req.locationKeyword}, '%')
                )
              </if>
              <if test="req.modelNo != null and req.modelNo != ''">
                AND s.model_no LIKE CONCAT('%', #{req.modelNo}, '%')
              </if>
              <if test="req.specSize != null and req.specSize != ''">
                AND s.spec_size = #{req.specSize}
              </if>
              <if test="req.qualityStatus != null and req.qualityStatus != ''">
                AND s.quality_status = #{req.qualityStatus}
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (
                    s.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.source_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.source_parent_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.source_plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.model_no LIKE CONCAT('%', #{req.keyword}, '%')
                )
              </if>
            ) candidate
            WHERE 1 = 1
              <if test="req.reportDate != null">
                AND DATE(candidate.reportTime) = #{req.reportDate}
              </if>
            ORDER BY candidate.reportTime DESC, candidate.batchNo ASC, candidate.stockId ASC
            LIMIT #{limit}
            </script>
            """)
    List<HcDiscretePostProcessVO.StockRespVO> selectCandidateList(
            @Param("req") HcDiscretePostProcessVO.SourceQueryReqVO reqVO,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT *
            FROM (
            SELECT
                CONCAT('L:', l.id) AS candidateKey,
                'LOCK' AS candidateType,
                CONCAT(IFNULL(l.target_op_name, l.target_op_code), '未报工') AS candidateStatus,
                l.id AS sourceLockId,
                l.plan_id AS sourceLockPlanId,
                l.target_plan_no AS sourceLockPlanNo,
                l.plan_operation_id AS sourceLockOperationId,
                s.id AS stockId,
                COALESCE(l.stock_type, s.stock_type) AS stockType,
                COALESCE(l.source_type, s.source_type) AS sourceType,
                COALESCE(l.source_table, s.source_table) AS sourceTable,
                COALESCE(l.source_id, s.source_id) AS sourceId,
                s.source_report_id AS sourceReportId,
                COALESCE(l.source_plan_id, s.source_plan_id) AS sourcePlanId,
                COALESCE(l.source_plan_no, s.source_plan_no) AS sourcePlanNo,
                COALESCE(l.source_plan_operation_id, s.source_plan_operation_id) AS sourcePlanOperationId,
                COALESCE(l.source_batch_no, s.source_batch_no, s.batch_no) AS sourceBatchNo,
                COALESCE(s.source_parent_batch_no, l.source_batch_no, s.batch_no) AS sourceParentBatchNo,
                COALESCE(l.batch_no, l.source_batch_no, s.batch_no) AS batchNo,
                COALESCE(l.material_id, s.material_id) AS materialId,
                COALESCE(l.material_code, s.material_code) AS materialCode,
                COALESCE(l.material_name, s.material_name) AS materialName,
                COALESCE(l.recipe_code, s.recipe_code) AS recipeCode,
                s.recipe_name AS recipeName,
                COALESCE(l.model_no, s.model_no) AS modelNo,
                COALESCE(l.size_spec, s.spec_size) AS specSize,
                l.target_op_code AS opCode,
                CONCAT(IFNULL(l.target_op_name, l.target_op_code), '未报工') AS opName,
                COALESCE(l.segment_code, s.segment_code) AS segmentCode,
                COALESCE(l.segment_name, s.segment_name) AS segmentName,
                COALESCE(l.thickness, s.thickness) AS thickness,
                s.on_hand_qty AS onHandQty,
                l.remaining_qty AS availableQty,
                0 AS shareableQty,
                s.frozen_qty AS frozenQty,
                s.plan_locked_qty AS planLockedQty,
                s.quality_status AS qualityStatus,
                s.biz_status AS bizStatus,
                COALESCE(l.uom, s.uom) AS uom,
                COALESCE(l.location_code, s.location_code) AS locationCode,
                COALESCE(l.location_name, s.location_name) AS locationName,
                COALESCE(l.production_date, s.production_date) AS productionDate,
                COALESCE(l.expiry_date, s.expiry_date) AS expiryDate,
                s.last_txn_time AS lastTxnTime,
                COALESCE(
                    sl.scan_time,
                    ps.confirmer_time, ps.end_time, ps.recorder_time, ps.update_time, ps.create_time,
                    a2.confirmer_time, a2.end_time, a2.recorder_time, a2.update_time, a2.create_time,
                    cr.confirmer_time, cr.end_time, cr.recorder_time, cr.update_time, cr.create_time,
                    s.last_txn_time
                ) AS reportTime
            FROM mes_pp_plan_inv_lock l
            INNER JOIN mes_inv_stock s ON s.id = l.stock_id AND s.deleted = b'0'
            LEFT JOIN mes_sfc_slitting_slice_record sl
                   ON sl.id = COALESCE(l.source_id, s.source_report_id, s.source_id)
                  AND (COALESCE(l.source_table, s.source_table) = 'mes_sfc_slitting_slice_record'
                       OR COALESCE(l.source_type, s.source_type) = 'SLITTING')
                  AND sl.deleted = b'0'
            LEFT JOIN mes_sfc_press_slot_report ps
                   ON ps.id = COALESCE(l.source_id, s.source_report_id, s.source_id)
                  AND (COALESCE(l.source_table, s.source_table) = 'mes_sfc_press_slot_report'
                       OR COALESCE(l.source_type, s.source_type) = 'PRESS_SLOT')
                  AND ps.deleted = b'0'
            LEFT JOIN mes_sfc_adhesive2_report a2
                   ON a2.id = COALESCE(l.source_id, s.source_report_id, s.source_id)
                  AND (COALESCE(l.source_table, s.source_table) = 'mes_sfc_adhesive2_report'
                       OR COALESCE(l.source_type, s.source_type) = 'ADHESIVE2')
                  AND a2.deleted = b'0'
            LEFT JOIN mes_sfc_cut_round_report cr
                   ON cr.id = COALESCE(l.source_id, s.source_report_id, s.source_id)
                  AND (COALESCE(l.source_table, s.source_table) = 'mes_sfc_cut_round_report'
                       OR COALESCE(l.source_type, s.source_type) = 'CUT_ROUND')
                  AND cr.deleted = b'0'
            WHERE l.deleted = b'0'
              AND l.target_op_code = #{req.targetOpCode}
              AND l.lock_status = 'ACTIVE'
              AND l.consume_report_id IS NULL
              AND IFNULL(l.remaining_qty, 0) &gt; 0
              AND (l.lock_type IS NULL OR l.lock_type = 'WIP')
              AND (l.stock_type IS NULL OR l.stock_type = 'WIP')
              <if test="req.sourceOpCode != null and req.sourceOpCode != ''">
                AND (l.op_code = #{req.sourceOpCode} OR s.op_code = #{req.sourceOpCode})
              </if>
              <if test="req.sourceTypes != null and req.sourceTypes.size() &gt; 0">
                AND COALESCE(l.source_type, s.source_type) IN
                <foreach collection="req.sourceTypes" item="sourceType" open="(" separator="," close=")">
                  #{sourceType}
                </foreach>
              </if>
              <if test="req.materialCode != null and req.materialCode != ''">
                AND COALESCE(l.material_code, s.material_code) LIKE CONCAT('%', #{req.materialCode}, '%')
              </if>
              <if test="req.locationKeyword != null and req.locationKeyword != ''">
                AND (
                    COALESCE(l.location_code, s.location_code) LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR COALESCE(l.location_name, s.location_name) LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR s.warehouse_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR s.warehouse_name LIKE CONCAT('%', #{req.locationKeyword}, '%')
                )
              </if>
              <if test="req.modelNo != null and req.modelNo != ''">
                AND COALESCE(l.model_no, s.model_no) LIKE CONCAT('%', #{req.modelNo}, '%')
              </if>
              <if test="req.specSize != null and req.specSize != ''">
                AND COALESCE(l.size_spec, s.spec_size) = #{req.specSize}
              </if>
              <if test="req.qualityStatus != null and req.qualityStatus != ''">
                AND s.quality_status = #{req.qualityStatus}
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (
                    l.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR l.source_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR s.source_parent_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR l.source_plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR l.target_plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR COALESCE(l.material_code, s.material_code) LIKE CONCAT('%', #{req.keyword}, '%')
                    OR COALESCE(l.material_name, s.material_name) LIKE CONCAT('%', #{req.keyword}, '%')
                    OR COALESCE(l.model_no, s.model_no) LIKE CONCAT('%', #{req.keyword}, '%')
                )
              </if>
            ) candidate
            WHERE 1 = 1
              <if test="req.reportDate != null">
                AND DATE(candidate.reportTime) = #{req.reportDate}
              </if>
            ORDER BY candidate.reportTime DESC, candidate.batchNo ASC, candidate.sourceLockId ASC
            LIMIT #{limit}
            </script>
            """)
    List<HcDiscretePostProcessVO.StockRespVO> selectPendingLockCandidateList(
            @Param("req") HcDiscretePostProcessVO.SourceQueryReqVO reqVO,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT *
            FROM (
            SELECT
                CONCAT('N:', p.id) AS candidateKey,
                'NG_PIECE' AS candidateType,
                CASE p.status
                    WHEN 'WAIT_SHELF' THEN '待上架NG'
                    WHEN 'WAIT_FREEZE_SHELF' THEN '待冻结上架'
                    WHEN 'STORED' THEN 'NG库可用'
                    WHEN 'FROZEN' THEN '冻结可选'
                    ELSE p.status
                END AS candidateStatus,
                NULL AS sourceLockId,
                NULL AS sourceLockPlanId,
                NULL AS sourceLockPlanNo,
                NULL AS sourceLockOperationId,
                p.stock_id AS stockId,
                p.id AS ngPieceId,
                'NG_PIECE' AS stockType,
                p.process_type AS sourceType,
                p.source_table AS sourceTable,
                p.source_id AS sourceId,
                p.source_report_id AS sourceReportId,
                p.source_plan_id AS sourcePlanId,
                p.source_plan_no AS sourcePlanNo,
                p.source_plan_operation_id AS sourcePlanOperationId,
                p.source_batch_no AS sourceBatchNo,
                p.source_parent_batch_no AS sourceParentBatchNo,
                p.piece_no AS batchNo,
                p.material_id AS materialId,
                p.material_code AS materialCode,
                p.material_name AS materialName,
                NULL AS recipeCode,
                NULL AS recipeName,
                p.model_no AS modelNo,
                NULL AS specSize,
                CASE p.process_type
                    WHEN 'SLITTING' THEN 5
                    WHEN 'PRESS_SLOT' THEN 6
                    WHEN 'ADHESIVE2' THEN 7
                    ELSE NULL
                END AS opSeq,
                CASE p.process_type
                    WHEN 'SLITTING' THEN 'WC-SLIT'
                    WHEN 'PRESS_SLOT' THEN 'WC-GROOVE'
                    WHEN 'ADHESIVE2' THEN 'WC-ADH2'
                    ELSE p.process_type
                END AS opCode,
                p.process_name AS opName,
                NULL AS segmentCode,
                NULL AS segmentName,
                NULL AS thickness,
                p.piece_qty AS onHandQty,
                p.piece_qty AS availableQty,
                0 AS shareableQty,
                CASE WHEN p.status = 'FROZEN' THEN p.piece_qty ELSE 0 END AS frozenQty,
                0 AS planLockedQty,
                COALESCE(p.quality_result, 'NG') AS qualityStatus,
                p.status AS bizStatus,
                p.status AS ngStatus,
                CASE p.status
                    WHEN 'WAIT_SHELF' THEN '待上架'
                    WHEN 'WAIT_FREEZE_SHELF' THEN '待冻结上架'
                    WHEN 'STORED' THEN '已上架'
                    WHEN 'FROZEN' THEN '已冻结'
                    ELSE p.status
                END AS ngStatusText,
                p.entry_reason AS entryReason,
                p.quality_result AS qualityResult,
                'pcs' AS uom,
                p.current_location_code AS locationCode,
                p.current_location_name AS locationName,
                p.current_warehouse_code AS currentWarehouseCode,
                p.current_warehouse_name AS currentWarehouseName,
                p.current_location_code AS currentLocationCode,
                p.current_location_name AS currentLocationName,
                p.freeze_instruction_id AS freezeInstructionId,
                p.freeze_instruction_no AS freezeInstructionNo,
                NULL AS productionDate,
                NULL AS expiryDate,
                COALESCE(p.shelved_time, p.update_time, p.create_time) AS lastTxnTime,
                COALESCE(
                    sl.scan_time,
                    ps.confirmer_time, ps.end_time, ps.recorder_time, ps.update_time, ps.create_time,
                    a2.confirmer_time, a2.end_time, a2.recorder_time, a2.update_time, a2.create_time,
                    cr.confirmer_time, cr.end_time, cr.recorder_time, cr.update_time, cr.create_time,
                    p.shelved_time, p.update_time, p.create_time
                ) AS reportTime
            FROM mes_inv_ng_piece p
            LEFT JOIN mes_sfc_slitting_slice_record sl
                   ON sl.id = COALESCE(p.source_report_id, p.source_id)
                  AND (p.source_table = 'mes_sfc_slitting_slice_record' OR p.process_type = 'SLITTING')
                  AND sl.deleted = b'0'
            LEFT JOIN mes_sfc_press_slot_report ps
                   ON ps.id = COALESCE(p.source_report_id, p.source_id)
                  AND (p.source_table = 'mes_sfc_press_slot_report' OR p.process_type = 'PRESS_SLOT')
                  AND ps.deleted = b'0'
            LEFT JOIN mes_sfc_adhesive2_report a2
                   ON a2.id = COALESCE(p.source_report_id, p.source_id)
                  AND (p.source_table = 'mes_sfc_adhesive2_report' OR p.process_type = 'ADHESIVE2')
                  AND a2.deleted = b'0'
            LEFT JOIN mes_sfc_cut_round_report cr
                   ON cr.id = COALESCE(p.source_report_id, p.source_id)
                  AND (p.source_table = 'mes_sfc_cut_round_report' OR p.process_type = 'CUT_ROUND')
                  AND cr.deleted = b'0'
            WHERE p.deleted = b'0'
              AND p.status IN ('WAIT_SHELF', 'WAIT_FREEZE_SHELF', 'STORED', 'FROZEN')
              AND NOT EXISTS (
                    SELECT 1
                    FROM mes_pp_plan_inv_lock used_lock
                    WHERE used_lock.ng_piece_id = p.id
                      AND used_lock.deleted = b'0'
                      AND used_lock.lock_status IN ('ACTIVE', 'CONSUMED')
              )
              <if test="req.sourceTypes != null and req.sourceTypes.size() &gt; 0">
                AND p.process_type IN
                <foreach collection="req.sourceTypes" item="sourceType" open="(" separator="," close=")">
                  #{sourceType}
                </foreach>
              </if>
              <if test="req.ngStatuses != null and req.ngStatuses.size() &gt; 0">
                AND p.status IN
                <foreach collection="req.ngStatuses" item="ngStatus" open="(" separator="," close=")">
                  #{ngStatus}
                </foreach>
              </if>
              <if test="req.materialCode != null and req.materialCode != ''">
                AND p.material_code LIKE CONCAT('%', #{req.materialCode}, '%')
              </if>
              <if test="req.locationKeyword != null and req.locationKeyword != ''">
                AND (
                    p.current_location_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR p.current_location_name LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR p.current_warehouse_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                    OR p.current_warehouse_name LIKE CONCAT('%', #{req.locationKeyword}, '%')
                )
              </if>
              <if test="req.modelNo != null and req.modelNo != ''">
                AND p.model_no LIKE CONCAT('%', #{req.modelNo}, '%')
              </if>
              <if test="req.keyword != null and req.keyword != ''">
                AND (
                    p.piece_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.source_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.source_parent_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.source_plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.model_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.current_location_code LIKE CONCAT('%', #{req.keyword}, '%')
                    OR p.current_location_name LIKE CONCAT('%', #{req.keyword}, '%')
                )
              </if>
            ) candidate
            WHERE 1 = 1
              <if test="req.reportDate != null">
                AND DATE(candidate.reportTime) = #{req.reportDate}
              </if>
            ORDER BY candidate.reportTime DESC, candidate.batchNo ASC, candidate.ngPieceId ASC
            LIMIT #{limit}
            </script>
            """)
    List<HcDiscretePostProcessVO.StockRespVO> selectNgPieceCandidateList(
            @Param("req") HcDiscretePostProcessVO.SourceQueryReqVO reqVO,
            @Param("limit") int limit);

    @Select("""
            <script>
            SELECT
                po.id AS planId,
                po.plan_no AS planNo,
                po.plan_status AS planStatus,
                po.plan_mode AS planMode,
                op.id AS planOperationId,
                op.op_seq AS opSeq,
                op.op_code AS opCode,
                op.op_name AS opName,
                op.work_center_id AS workCenterId,
                op.work_center_code AS workCenterCode,
                op.work_center_name AS workCenterName,
                op.equipment_id AS equipmentId,
                op.equipment_code AS equipmentCode,
                op.equipment_name AS equipmentName,
                op.operation_status AS operationStatus,
                po.material_code AS materialCode,
                po.material_name AS materialName,
                po.model_code AS modelCode,
                po.size_spec AS sizeSpec,
                op.required_qty AS requiredQty,
                COUNT(l.id) AS sourceCount,
                IFNULL(SUM(CASE WHEN l.lock_status = 'ACTIVE' THEN 1 ELSE 0 END), 0) AS pendingCount,
                IFNULL(SUM(CASE WHEN l.lock_status = 'CONSUMED' THEN 1 ELSE 0 END), 0) AS finishedCount,
                IFNULL(SUM(CASE WHEN l.lock_status = 'RELEASED' THEN 1 ELSE 0 END), 0) AS releasedCount,
                CASE
                    WHEN COUNT(DISTINCT COALESCE(l.batch_no, l.source_batch_no)) &gt; 1 THEN '多批号加工'
                    ELSE MIN(COALESCE(l.batch_no, l.source_batch_no))
                END AS firstSourceBatchNo,
                GROUP_CONCAT(DISTINCT COALESCE(l.source_plan_no, '-') ORDER BY l.source_plan_no SEPARATOR '、') AS sourcePlanNos,
                MAX(l.consume_time) AS lastReportTime
            FROM mes_pp_plan_operation op
            INNER JOIN mes_pp_plan_order po ON po.id = op.plan_id AND po.deleted = b'0'
            INNER JOIN mes_pp_plan_inv_lock l ON l.plan_operation_id = op.id AND l.deleted = b'0'
            WHERE op.deleted = b'0'
              AND op.op_code = #{req.opCode}
              AND (
                po.plan_mode IN ('DISCRETE_POST', 'DISCRETE_POST_PROCESS')
                OR po.source_type IN ('DISCRETE_WIP', 'NG_INVENTORY')
                OR po.remark LIKE '%DISCRETE_POST_PROCESS%'
                OR l.remark LIKE '%DISCRETE_POST_PROCESS%'
              )
              <if test="req.keyword != null and req.keyword != ''">
                AND (
                    po.plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR l.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR l.source_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR l.source_plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                    OR po.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                    OR po.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                    OR po.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                )
              </if>
            GROUP BY po.id, po.plan_no, po.plan_status, po.plan_mode, op.id, op.op_seq, op.op_code,
                     op.op_name, op.work_center_id, op.work_center_code, op.work_center_name,
                     op.equipment_id, op.equipment_code, op.equipment_name,
                     op.operation_status, po.material_code, po.material_name, po.model_code,
                     po.size_spec, op.required_qty
            <choose>
              <when test="req.taskStatus == 'PENDING' or req.taskStatus == 'UNFINISHED'">
                HAVING IFNULL(SUM(CASE WHEN l.lock_status = 'ACTIVE' THEN 1 ELSE 0 END), 0) &gt; 0
              </when>
              <when test="req.taskStatus == 'COMPLETED' or req.taskStatus == 'FINISHED'">
                HAVING IFNULL(SUM(CASE WHEN l.lock_status = 'ACTIVE' THEN 1 ELSE 0 END), 0) = 0 AND COUNT(l.id) &gt; 0
              </when>
            </choose>
            ORDER BY
                CASE WHEN IFNULL(SUM(CASE WHEN l.lock_status = 'ACTIVE' THEN 1 ELSE 0 END), 0) &gt; 0 THEN 0 ELSE 1 END,
                po.plan_no DESC,
                op.op_seq ASC,
                op.id DESC
            </script>
            """)
    List<HcDiscretePostProcessVO.TaskRespVO> selectTaskList(
            @Param("req") HcDiscretePostProcessVO.TaskQueryReqVO reqVO);

    @Select("""
            <script>
            SELECT
                l.id AS lockId,
                l.plan_id AS planId,
                l.target_plan_no AS planNo,
                l.plan_operation_id AS planOperationId,
                l.target_op_code AS targetOpCode,
                l.target_op_name AS targetOpName,
                l.stock_id AS stockId,
                l.ng_piece_id AS ngPieceId,
                l.stock_type AS stockType,
                l.source_type AS sourceType,
                l.source_table AS sourceTable,
                l.source_id AS sourceId,
                l.source_report_id AS sourceReportId,
                l.source_plan_id AS sourcePlanId,
                l.source_plan_no AS sourcePlanNo,
                l.source_plan_operation_id AS sourcePlanOperationId,
                l.source_batch_no AS sourceBatchNo,
                COALESCE(ng.source_parent_batch_no, s.source_parent_batch_no, l.source_batch_no) AS sourceParentBatchNo,
                l.batch_no AS batchNo,
                COALESCE(l.batch_no, ng.piece_no, l.source_batch_no, s.batch_no) AS pieceNo,
                l.material_id AS materialId,
                l.material_code AS materialCode,
                l.material_name AS materialName,
                l.model_no AS modelNo,
                l.recipe_code AS recipeCode,
                l.size_spec AS sizeSpec,
                l.op_code AS opCode,
                l.op_name AS opName,
                l.lock_qty AS lockQty,
                l.consumed_qty AS consumedQty,
                l.released_qty AS releasedQty,
                l.remaining_qty AS remainingQty,
                l.lock_status AS lockStatus,
                ng.status AS ngStatus,
                CASE ng.status
                    WHEN 'WAIT_SHELF' THEN '待上架'
                    WHEN 'WAIT_FREEZE_SHELF' THEN '待冻结上架'
                    WHEN 'STORED' THEN '已上架'
                    WHEN 'FROZEN' THEN '已冻结'
                    WHEN 'RETURNED' THEN '已返工'
                    WHEN 'OUTBOUNDED' THEN '已出库'
                    WHEN 'SCRAPPED' THEN '已报废'
                    ELSE ng.status
                END AS ngStatusText,
                ng.current_warehouse_code AS currentWarehouseCode,
                ng.current_warehouse_name AS currentWarehouseName,
                ng.current_location_code AS currentLocationCode,
                ng.current_location_name AS currentLocationName,
                l.consume_report_id AS consumeReportId,
                l.consume_time AS consumeTime,
                l.consume_txn_no AS consumeTxnNo,
                out_stock.id AS outputStockId,
                out_stock.batch_no AS outputStockBatchNo,
                CASE
                    WHEN out_stock.id IS NOT NULL THEN 'POSTED'
                    WHEN l.lock_status = 'CONSUMED' THEN 'NOT_POSTED'
                    ELSE NULL
                END AS outputStockPostStatus,
                ins.id AS inspectionTaskId,
                ins.inspection_task_no AS inspectionTaskNo,
                ins.inspection_type AS inspectionType,
                ins.inspection_status AS inspectionStatus,
                ins.inspection_result AS inspectionResult,
                ins.report_time AS inspectionReportTime,
                ins.reporter_name AS inspectionReporterName,
                ins.inspection_time AS inspectionTime,
                l.remark AS remark
            FROM mes_pp_plan_inv_lock l
            LEFT JOIN mes_inv_stock s ON s.id = l.stock_id AND s.deleted = b'0'
            LEFT JOIN mes_inv_ng_piece ng ON ng.id = l.ng_piece_id AND ng.deleted = b'0'
            LEFT JOIN mes_inv_stock out_stock
                   ON out_stock.source_plan_id = l.plan_id
                  AND out_stock.source_plan_operation_id = l.plan_operation_id
                  AND out_stock.source_batch_no = COALESCE(l.batch_no, ng.piece_no, l.source_batch_no, s.batch_no)
                  AND out_stock.deleted = b'0'
            LEFT JOIN mes_sfc_discrete_post_process_inspection ins
                   ON ins.id = (
                        SELECT MAX(ins2.id)
                        FROM mes_sfc_discrete_post_process_inspection ins2
                        WHERE ins2.source_lock_id = l.id
                          AND ins2.deleted = b'0'
                   )
            WHERE l.deleted = b'0'
              AND l.plan_operation_id = #{planOperationId}
              <if test="taskStatus == 'PENDING' or taskStatus == 'UNFINISHED'">
                AND l.lock_status = 'ACTIVE'
              </if>
              <if test="taskStatus == 'COMPLETED' or taskStatus == 'FINISHED'">
                AND l.lock_status = 'CONSUMED'
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                    l.batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR ng.piece_no LIKE CONCAT('%', #{keyword}, '%')
                    OR l.source_batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR ng.source_parent_batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR s.source_parent_batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR l.source_plan_no LIKE CONCAT('%', #{keyword}, '%')
                    OR l.material_code LIKE CONCAT('%', #{keyword}, '%')
                    OR l.material_name LIKE CONCAT('%', #{keyword}, '%')
                    OR l.model_no LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY
                CASE l.lock_status WHEN 'ACTIVE' THEN 0 WHEN 'CONSUMED' THEN 1 ELSE 2 END,
                l.source_plan_no ASC,
                l.source_batch_no ASC,
                l.batch_no ASC,
                l.id ASC
            </script>
            """)
    List<HcDiscretePostProcessVO.SourceRespVO> selectSourceList(
            @Param("planOperationId") Long planOperationId,
            @Param("taskStatus") String taskStatus,
            @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT
                l.id AS lockId,
                l.plan_id AS planId,
                l.target_plan_no AS planNo,
                l.plan_operation_id AS planOperationId,
                l.target_op_code AS targetOpCode,
                l.target_op_name AS targetOpName,
                l.stock_id AS stockId,
                l.ng_piece_id AS ngPieceId,
                l.stock_type AS stockType,
                l.source_type AS sourceType,
                l.source_table AS sourceTable,
                l.source_id AS sourceId,
                l.source_report_id AS sourceReportId,
                l.source_plan_id AS sourcePlanId,
                l.source_plan_no AS sourcePlanNo,
                l.source_plan_operation_id AS sourcePlanOperationId,
                l.source_batch_no AS sourceBatchNo,
                COALESCE(ng.source_parent_batch_no, s.source_parent_batch_no, l.source_batch_no) AS sourceParentBatchNo,
                l.batch_no AS batchNo,
                COALESCE(l.batch_no, ng.piece_no, l.source_batch_no, s.batch_no) AS pieceNo,
                l.material_id AS materialId,
                l.material_code AS materialCode,
                l.material_name AS materialName,
                l.model_no AS modelNo,
                l.recipe_code AS recipeCode,
                l.size_spec AS sizeSpec,
                l.op_code AS opCode,
                l.op_name AS opName,
                l.lock_qty AS lockQty,
                l.consumed_qty AS consumedQty,
                l.released_qty AS releasedQty,
                l.remaining_qty AS remainingQty,
                l.lock_status AS lockStatus,
                ng.status AS ngStatus,
                CASE ng.status
                    WHEN 'WAIT_SHELF' THEN '待上架'
                    WHEN 'WAIT_FREEZE_SHELF' THEN '待冻结上架'
                    WHEN 'STORED' THEN '已上架'
                    WHEN 'FROZEN' THEN '已冻结'
                    WHEN 'RETURNED' THEN '已返工'
                    WHEN 'OUTBOUNDED' THEN '已出库'
                    WHEN 'SCRAPPED' THEN '已报废'
                    ELSE ng.status
                END AS ngStatusText,
                ng.current_warehouse_code AS currentWarehouseCode,
                ng.current_warehouse_name AS currentWarehouseName,
                ng.current_location_code AS currentLocationCode,
                ng.current_location_name AS currentLocationName,
                l.consume_report_id AS consumeReportId,
                l.consume_time AS consumeTime,
                l.consume_txn_no AS consumeTxnNo,
                out_stock.id AS outputStockId,
                out_stock.batch_no AS outputStockBatchNo,
                CASE
                    WHEN out_stock.id IS NOT NULL THEN 'POSTED'
                    WHEN l.lock_status = 'CONSUMED' THEN 'NOT_POSTED'
                    ELSE NULL
                END AS outputStockPostStatus,
                ins.id AS inspectionTaskId,
                ins.inspection_task_no AS inspectionTaskNo,
                ins.inspection_type AS inspectionType,
                ins.inspection_status AS inspectionStatus,
                ins.inspection_result AS inspectionResult,
                ins.report_time AS inspectionReportTime,
                ins.reporter_name AS inspectionReporterName,
                ins.inspection_time AS inspectionTime,
                ins.remark AS remark
            FROM mes_sfc_discrete_post_process_inspection ins
            INNER JOIN mes_pp_plan_inv_lock l ON l.id = ins.source_lock_id AND l.deleted = b'0'
            LEFT JOIN mes_inv_stock s ON s.id = l.stock_id AND s.deleted = b'0'
            LEFT JOIN mes_inv_ng_piece ng ON ng.id = l.ng_piece_id AND ng.deleted = b'0'
            LEFT JOIN mes_inv_stock out_stock
                   ON out_stock.source_plan_id = l.plan_id
                  AND out_stock.source_plan_operation_id = l.plan_operation_id
                  AND out_stock.source_batch_no = COALESCE(l.batch_no, ng.piece_no, l.source_batch_no, s.batch_no)
                  AND out_stock.deleted = b'0'
            WHERE ins.deleted = b'0'
              AND ins.plan_operation_id = #{planOperationId}
              <if test="inspectionType != null and inspectionType != ''">
                AND ins.inspection_type = #{inspectionType}
              </if>
              <if test="keyword != null and keyword != ''">
                AND (
                    ins.inspection_task_no LIKE CONCAT('%', #{keyword}, '%')
                    OR ins.source_batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR l.batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR ng.piece_no LIKE CONCAT('%', #{keyword}, '%')
                    OR l.source_batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR ng.source_parent_batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR s.source_parent_batch_no LIKE CONCAT('%', #{keyword}, '%')
                    OR l.source_plan_no LIKE CONCAT('%', #{keyword}, '%')
                    OR l.material_code LIKE CONCAT('%', #{keyword}, '%')
                    OR l.material_name LIKE CONCAT('%', #{keyword}, '%')
                    OR l.model_no LIKE CONCAT('%', #{keyword}, '%')
                )
              </if>
            ORDER BY ins.report_time DESC, ins.id DESC
            </script>
            """)
    List<HcDiscretePostProcessVO.SourceRespVO> selectInspectionSourceList(
            @Param("planOperationId") Long planOperationId,
            @Param("inspectionType") String inspectionType,
            @Param("keyword") String keyword);

    @Select("""
            SELECT
                l.id AS lockId,
                l.plan_id AS planId,
                l.target_plan_no AS planNo,
                l.plan_operation_id AS planOperationId,
                l.target_op_code AS targetOpCode,
                l.target_op_name AS targetOpName,
                l.stock_id AS stockId,
                l.ng_piece_id AS ngPieceId,
                l.stock_type AS stockType,
                l.source_type AS sourceType,
                l.source_table AS sourceTable,
                l.source_id AS sourceId,
                l.source_report_id AS sourceReportId,
                l.source_plan_id AS sourcePlanId,
                l.source_plan_no AS sourcePlanNo,
                l.source_plan_operation_id AS sourcePlanOperationId,
                l.source_batch_no AS sourceBatchNo,
                COALESCE(ng.source_parent_batch_no, s.source_parent_batch_no, l.source_batch_no) AS sourceParentBatchNo,
                l.batch_no AS batchNo,
                COALESCE(l.batch_no, ng.piece_no, l.source_batch_no, s.batch_no) AS pieceNo,
                l.material_id AS materialId,
                l.material_code AS materialCode,
                l.material_name AS materialName,
                l.model_no AS modelNo,
                l.recipe_code AS recipeCode,
                l.size_spec AS sizeSpec,
                l.op_code AS opCode,
                l.op_name AS opName,
                l.lock_qty AS lockQty,
                l.consumed_qty AS consumedQty,
                l.released_qty AS releasedQty,
                l.remaining_qty AS remainingQty,
                l.lock_status AS lockStatus,
                ng.status AS ngStatus,
                CASE ng.status
                    WHEN 'WAIT_SHELF' THEN '待上架'
                    WHEN 'WAIT_FREEZE_SHELF' THEN '待冻结上架'
                    WHEN 'STORED' THEN '已上架'
                    WHEN 'FROZEN' THEN '已冻结'
                    WHEN 'RETURNED' THEN '已返工'
                    WHEN 'OUTBOUNDED' THEN '已出库'
                    WHEN 'SCRAPPED' THEN '已报废'
                    ELSE ng.status
                END AS ngStatusText,
                ng.current_warehouse_code AS currentWarehouseCode,
                ng.current_warehouse_name AS currentWarehouseName,
                ng.current_location_code AS currentLocationCode,
                ng.current_location_name AS currentLocationName,
                l.consume_report_id AS consumeReportId,
                l.consume_time AS consumeTime,
                l.consume_txn_no AS consumeTxnNo,
                NULL AS outputStockId,
                NULL AS outputStockBatchNo,
                NULL AS outputStockPostStatus,
                NULL AS inspectionTaskId,
                NULL AS inspectionTaskNo,
                NULL AS inspectionType,
                NULL AS inspectionStatus,
                NULL AS inspectionResult,
                NULL AS inspectionReportTime,
                NULL AS inspectionReporterName,
                NULL AS inspectionTime,
                l.remark AS remark
            FROM mes_pp_plan_inv_lock l
            LEFT JOIN mes_inv_stock s ON s.id = l.stock_id AND s.deleted = b'0'
            LEFT JOIN mes_inv_ng_piece ng ON ng.id = l.ng_piece_id AND ng.deleted = b'0'
            WHERE l.deleted = b'0'
              AND l.plan_operation_id = #{planOperationId}
              AND (
                  l.id = #{pieceOrLockId}
                  OR l.batch_no = #{pieceNo}
                  OR ng.piece_no = #{pieceNo}
                  OR l.source_batch_no = #{pieceNo}
                  OR s.batch_no = #{pieceNo}
              )
            ORDER BY l.id DESC
            LIMIT 1
            """)
    HcDiscretePostProcessVO.SourceRespVO selectSourceByPiece(
            @Param("planOperationId") Long planOperationId,
            @Param("pieceNo") String pieceNo,
            @Param("pieceOrLockId") Long pieceOrLockId);

    @Select("""
            SELECT
                l.id AS lockId,
                l.plan_id AS planId,
                l.target_plan_no AS planNo,
                l.plan_operation_id AS planOperationId,
                l.target_op_code AS targetOpCode,
                l.target_op_name AS targetOpName,
                l.stock_id AS stockId,
                l.ng_piece_id AS ngPieceId,
                l.stock_type AS stockType,
                l.source_type AS sourceType,
                l.source_table AS sourceTable,
                l.source_id AS sourceId,
                l.source_report_id AS sourceReportId,
                l.source_plan_id AS sourcePlanId,
                l.source_plan_no AS sourcePlanNo,
                l.source_plan_operation_id AS sourcePlanOperationId,
                l.source_batch_no AS sourceBatchNo,
                COALESCE(ng.source_parent_batch_no, s.source_parent_batch_no, l.source_batch_no) AS sourceParentBatchNo,
                l.batch_no AS batchNo,
                COALESCE(l.batch_no, ng.piece_no, l.source_batch_no, s.batch_no) AS pieceNo,
                l.material_id AS materialId,
                l.material_code AS materialCode,
                l.material_name AS materialName,
                l.model_no AS modelNo,
                l.recipe_code AS recipeCode,
                l.size_spec AS sizeSpec,
                l.op_code AS opCode,
                l.op_name AS opName,
                l.lock_qty AS lockQty,
                l.consumed_qty AS consumedQty,
                l.released_qty AS releasedQty,
                l.remaining_qty AS remainingQty,
                l.lock_status AS lockStatus,
                ng.status AS ngStatus,
                CASE ng.status
                    WHEN 'WAIT_SHELF' THEN '待上架'
                    WHEN 'WAIT_FREEZE_SHELF' THEN '待冻结上架'
                    WHEN 'STORED' THEN '已上架'
                    WHEN 'FROZEN' THEN '已冻结'
                    WHEN 'RETURNED' THEN '已返工'
                    WHEN 'OUTBOUNDED' THEN '已出库'
                    WHEN 'SCRAPPED' THEN '已报废'
                    ELSE ng.status
                END AS ngStatusText,
                ng.current_warehouse_code AS currentWarehouseCode,
                ng.current_warehouse_name AS currentWarehouseName,
                ng.current_location_code AS currentLocationCode,
                ng.current_location_name AS currentLocationName,
                l.consume_report_id AS consumeReportId,
                l.consume_time AS consumeTime,
                l.consume_txn_no AS consumeTxnNo,
                out_stock.id AS outputStockId,
                out_stock.batch_no AS outputStockBatchNo,
                CASE
                    WHEN out_stock.id IS NOT NULL THEN 'POSTED'
                    WHEN l.lock_status = 'CONSUMED' THEN 'NOT_POSTED'
                    ELSE NULL
                END AS outputStockPostStatus,
                ins.id AS inspectionTaskId,
                ins.inspection_task_no AS inspectionTaskNo,
                ins.inspection_type AS inspectionType,
                ins.inspection_status AS inspectionStatus,
                ins.inspection_result AS inspectionResult,
                ins.report_time AS inspectionReportTime,
                ins.reporter_name AS inspectionReporterName,
                ins.inspection_time AS inspectionTime,
                l.remark AS remark
            FROM mes_pp_plan_inv_lock l
            INNER JOIN mes_pp_plan_order po ON po.id = l.plan_id AND po.deleted = b'0'
            LEFT JOIN mes_inv_stock s ON s.id = l.stock_id AND s.deleted = b'0'
            LEFT JOIN mes_inv_ng_piece ng ON ng.id = l.ng_piece_id AND ng.deleted = b'0'
            LEFT JOIN mes_inv_stock out_stock
                   ON out_stock.source_plan_id = l.plan_id
                  AND out_stock.source_plan_operation_id = l.plan_operation_id
                  AND out_stock.source_batch_no = COALESCE(l.batch_no, ng.piece_no, l.source_batch_no, s.batch_no)
                  AND out_stock.deleted = b'0'
            LEFT JOIN mes_sfc_discrete_post_process_inspection ins
                   ON ins.id = (
                        SELECT MAX(ins2.id)
                        FROM mes_sfc_discrete_post_process_inspection ins2
                        WHERE ins2.source_lock_id = l.id
                          AND ins2.deleted = b'0'
                   )
            WHERE l.deleted = b'0'
              AND l.target_op_code = #{targetOpCode}
              AND (
                po.plan_mode IN ('DISCRETE_POST', 'DISCRETE_POST_PROCESS')
                OR po.source_type IN ('DISCRETE_WIP', 'NG_INVENTORY')
                OR po.remark LIKE '%DISCRETE_POST_PROCESS%'
                OR l.remark LIKE '%DISCRETE_POST_PROCESS%'
              )
              AND (
                  l.id = #{pieceOrLockId}
                  OR l.batch_no = #{pieceNo}
                  OR ng.piece_no = #{pieceNo}
                  OR l.source_batch_no = #{pieceNo}
                  OR s.batch_no = #{pieceNo}
              )
            ORDER BY
                CASE l.lock_status WHEN 'ACTIVE' THEN 0 WHEN 'CONSUMED' THEN 1 ELSE 2 END,
                l.id DESC
            LIMIT 1
            """)
    HcDiscretePostProcessVO.SourceRespVO selectSourceByOpAndPiece(
            @Param("targetOpCode") String targetOpCode,
            @Param("pieceNo") String pieceNo,
            @Param("pieceOrLockId") Long pieceOrLockId);
}
