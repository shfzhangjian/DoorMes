package cn.iocoder.yudao.module.mes.dal.mysql.hc.scheduleworkbench;

import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchReqVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcScheduleWorkbenchMapper {

    record PlanRow(
            Long planId,
            String planNo,
            LocalDate planDate,
            String planMode,
            String sourceType,
            String prodType,
            String planStatus,
            String modelCode,
            String requirement,
            String sizeSpec,
            BigDecimal quantity,
            LocalDate dueDate,
            Boolean frontProcessFlag,
            Boolean postProcessFlag,
            String batchNo,
            String productionBatchNo,
            String motherRollNo,
            String inventorySourceBatchNos,
            String splitPlanNos) {
    }

    record ScheduleOperationRow(
            Long planId,
            Long planOperationId,
            LocalDate scheduleDate,
            Integer opSeq,
            String operationName,
            String operationStatus,
            String workCenterName,
            String equipmentName) {
    }

    record FactRow(
            Long planId,
            Long planOperationId,
            LocalDate reportDate,
            String operationName,
            String factSource,
            String segmentBatchNo,
            BigDecimal inputQty,
            String inputUnit,
            BigDecimal reportQty,
            String reportUnit,
            BigDecimal pendingQty,
            String pendingUnit,
            BigDecimal lossNgQty,
            String lossNgUnit,
            Integer recordCount,
            Integer ngQty,
            Boolean coaFlag,
            Boolean coaNgFlag,
            Boolean completedFlag,
            String equipmentNames,
            LocalDateTime lastReportTime,
            String glueBoardModel,
            String firstInspectionResult) {
    }

    record LastOperationReportRow(
            Long planId,
            String operationName,
            BigDecimal reportQty,
            String unit) {
    }

    record ProgressQuantityRow(
            Long planId,
            BigDecimal wetRollQty,
            BigDecimal secondGrindingQty,
            BigDecimal slittingConfirmedQty,
            BigDecimal adhesive2Qty,
            Integer adhesive2ChangeoverCount,
            BigDecimal pickedQty) {
    }

    record ModelChangeoverSummaryRow(
            Long planId,
            String planValue,
            String actualValue,
            Integer qty) {
    }

    record CutRoundSizeSourceRow(
            Long planId,
            Long reportId,
            String planValue,
            String productionBatchNo,
            String extraJson) {
    }

    record ChangeoverDetailRow(
            Long planId,
            Long sourceReportId,
            String sourceOperation,
            String planValue,
            String actualValue,
            String actualSuffix,
            String productionBatchNo,
            String glueBoardModel,
            String reporterName,
            LocalDateTime reportTime,
            String reportStatus,
            String extraJson) {
    }

    record OperationCompletionRow(Long planId, Integer totalCount, Integer finishedCount) {
    }

    record PlanOperationScopeRow(Long planId, Integer opSeq, String operationName, String operationCode) {
    }

    record SegmentProgressRow(Long planId, String segmentBatchNo, String sourceType, Integer sourceCount) {
    }

    record NgSummaryRow(String operationName, Integer ngQty) {
    }

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM mes_pp_plan_order p
            WHERE p.deleted = 0
              AND (
                (
                  COALESCE(p.production_start_date, p.plan_date) &lt;= #{req.endDate}
                  AND COALESCE(p.production_end_date, p.production_start_date, p.plan_date) &gt;= #{req.startDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_operation_report r
                  WHERE r.deleted = 0
                    AND r.plan_id = p.id
                    AND (
                      r.report_type = 'END'
                      OR (
                        r.start_time IS NOT NULL
                        AND (r.operation_name IN ('配料', '配方', '湿法')
                          OR r.source_menu_code IN ('FORMULA_REPORT', 'WET_REPORT', 'ROUGH_GRINDING_REPORT', 'ADHESIVE_REPORT'))
                      )
                    )
                    AND DATE(COALESCE(r.end_time, r.confirmer_time, r.recorder_time, r.start_time,
                      r.update_time, r.create_time, r.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_grinding_first_detail f
                  WHERE f.deleted = 0
                    AND f.plan_id = p.id
                    AND (f.start_time IS NOT NULL OR f.end_time IS NOT NULL)
                    AND DATE(COALESCE(f.end_time, f.start_time, f.update_time, f.create_time, f.report_date))
                      BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_grinding_second_detail s
                  WHERE s.deleted = 0
                    AND s.plan_id = p.id
                    AND (s.start_time IS NOT NULL OR s.end_time IS NOT NULL OR s.confirm_time IS NOT NULL)
                    AND DATE(COALESCE(s.end_time, s.confirm_time, s.start_time, s.update_time, s.create_time,
                      s.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_adhesive_report a
                  WHERE a.deleted = 0
                    AND a.plan_id = p.id
                    AND (a.report_status IN ('CONFIRMED', 'COMPLETED') OR a.confirmer_time IS NOT NULL
                      OR a.end_time IS NOT NULL)
                    AND DATE(COALESCE(a.end_time, a.confirmer_time, a.recorder_time, a.update_time, a.create_time,
                      a.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_slitting_slice_record sr
                  WHERE sr.deleted = 0
                    AND sr.plan_id = p.id
                    AND (sr.scan_status IN ('CONFIRMED', 'COMPLETED') OR sr.scan_time IS NOT NULL)
                    AND DATE(COALESCE(sr.scan_time, sr.update_time, sr.create_time))
                      BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_press_slot_report pr
                  WHERE pr.deleted = 0
                    AND pr.plan_id = p.id
                    AND (pr.report_status IN ('CONFIRMED', 'COMPLETED') OR pr.confirmer_time IS NOT NULL
                      OR pr.end_time IS NOT NULL)
                    AND DATE(COALESCE(pr.end_time, pr.confirmer_time, pr.recorder_time, pr.update_time, pr.create_time,
                      pr.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_adhesive2_report ar
                  WHERE ar.deleted = 0
                    AND ar.plan_id = p.id
                    AND (ar.report_status IN ('CONFIRMED', 'COMPLETED') OR ar.confirmer_time IS NOT NULL
                      OR ar.end_time IS NOT NULL)
                    AND DATE(COALESCE(ar.end_time, ar.confirmer_time, ar.recorder_time, ar.update_time, ar.create_time,
                      ar.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_cut_round_report cr
                  WHERE cr.deleted = 0
                    AND cr.plan_id = p.id
                    AND (cr.report_status IN ('CONFIRMED', 'COMPLETED') OR cr.inspection_time IS NOT NULL
                      OR cr.end_time IS NOT NULL)
                    AND DATE(COALESCE(cr.end_time, cr.inspection_time, cr.recorder_time, cr.update_time, cr.create_time,
                      cr.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_inner_pack_unit_item ip
                  LEFT JOIN mes_sfc_outer_pack_box_item obi ON obi.deleted = 0
                    AND (obi.inner_unit_id = ip.inner_unit_id OR obi.inner_unit_no = ip.inner_unit_no)
                  WHERE ip.deleted = 0
                    AND ip.plan_id = p.id
                    AND DATE(COALESCE(obi.scan_time, ip.scan_time, ip.update_time, ip.create_time))
                      BETWEEN #{req.startDate} AND #{req.endDate}
                )
              )
              <if test="req.keyword != null and req.keyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.model_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.parent_production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.inventory_source_batch_nos LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.remark LIKE CONCAT('%', #{req.keyword}, '%'))
              </if>
              <if test="req.planStatuses != null and req.planStatuses.size() &gt; 0">
                AND p.plan_status IN
                <foreach collection="req.planStatuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="req.modelCode != null and req.modelCode != ''">
                AND p.model_code = #{req.modelCode}
              </if>
              <if test="req.sizeSpec != null and req.sizeSpec != ''">
                AND p.size_spec = #{req.sizeSpec}
              </if>
            </script>
            """)
    BigDecimal selectScheduleTotal(@Param("req") HcScheduleWorkbenchReqVO reqVO);

    @Select("""
            SELECT COUNT(DISTINCT equipment_key)
            FROM (
              SELECT COALESCE(
                       CONVERT(CAST(equipment_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                       CONVERT(NULLIF(equipment_code, '') USING utf8mb4) COLLATE utf8mb4_unicode_ci
                     ) AS equipment_key
              FROM mes_sfc_operation_report
              WHERE deleted = 0 AND start_time IS NOT NULL AND DATE(start_time) = #{today}
              UNION ALL
              SELECT COALESCE(
                       CONVERT(CAST(o.equipment_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                       CONVERT(NULLIF(o.equipment_code, '') USING utf8mb4) COLLATE utf8mb4_unicode_ci
                     )
              FROM mes_sfc_adhesive_report a
              LEFT JOIN mes_pp_plan_operation o ON o.id = a.plan_operation_id AND o.deleted = 0
              WHERE a.deleted = 0 AND a.start_time IS NOT NULL AND DATE(a.start_time) = #{today}
              UNION ALL
              SELECT COALESCE(
                       CONVERT(CAST(o.equipment_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                       CONVERT(NULLIF(o.equipment_code, '') USING utf8mb4) COLLATE utf8mb4_unicode_ci
                     )
              FROM mes_sfc_press_slot_report ps
              LEFT JOIN mes_pp_plan_operation o ON o.id = ps.plan_operation_id AND o.deleted = 0
              WHERE ps.deleted = 0 AND ps.start_time IS NOT NULL AND DATE(ps.start_time) = #{today}
              UNION ALL
              SELECT COALESCE(
                       CONVERT(CAST(o.equipment_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                       CONVERT(NULLIF(o.equipment_code, '') USING utf8mb4) COLLATE utf8mb4_unicode_ci
                     )
              FROM mes_sfc_adhesive2_report a2
              LEFT JOIN mes_pp_plan_operation o ON o.id = a2.plan_operation_id AND o.deleted = 0
              WHERE a2.deleted = 0 AND a2.start_time IS NOT NULL AND DATE(a2.start_time) = #{today}
              UNION ALL
              SELECT COALESCE(
                       CONVERT(CAST(o.equipment_id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                       CONVERT(NULLIF(o.equipment_code, '') USING utf8mb4) COLLATE utf8mb4_unicode_ci
                     )
              FROM mes_sfc_cut_round_report cr
              LEFT JOIN mes_pp_plan_operation o ON o.id = cr.plan_operation_id AND o.deleted = 0
              WHERE cr.deleted = 0 AND cr.start_time IS NOT NULL AND DATE(cr.start_time) = #{today}
              UNION ALL
              SELECT COALESCE(
                       CONVERT(CAST(id AS CHAR) USING utf8mb4) COLLATE utf8mb4_unicode_ci,
                       CONVERT(NULLIF(equipment_code, '') USING utf8mb4) COLLATE utf8mb4_unicode_ci
                     )
              FROM mes_md_equipment
              WHERE deleted = 0
                AND work_status = 'PRODUCING'
                AND current_start_time IS NOT NULL
                AND DATE(current_start_time) = #{today}
            ) t
            WHERE equipment_key IS NOT NULL AND equipment_key != ''
            """)
    BigDecimal selectTodayStartedEquipmentCount(@Param("today") LocalDate today);

    @Select("""
            SELECT COALESCE(SUM(r.good_qty), 0)
            FROM mes_sfc_operation_report r
            WHERE r.deleted = 0
              AND r.report_type = 'END'
              AND (r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT')
              AND DATE(COALESCE(r.end_time, r.confirmer_time, r.recorder_time, r.update_time, r.create_time, r.report_date))
                  BETWEEN #{startDate} AND #{endDate}
            """)
    BigDecimal selectWetReportQty(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COALESCE(COUNT(1), 0)
            FROM mes_sfc_slitting_slice_record s
            WHERE s.deleted = 0
              AND s.scan_status = 'CONFIRMED'
              AND DATE(COALESCE(s.scan_time, s.update_time, s.create_time)) BETWEEN #{startDate} AND #{endDate}
            """)
    BigDecimal selectSlittingConfirmedCount(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COALESCE(COUNT(1), 0)
            FROM mes_sfc_adhesive2_report a2
            WHERE a2.deleted = 0
              AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
              AND DATE(COALESCE(a2.end_time, a2.confirmer_time, a2.recorder_time, a2.update_time, a2.create_time, a2.report_date))
                  BETWEEN #{startDate} AND #{endDate}
            """)
    BigDecimal selectAdhesive2PieceCount(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(ng_qty), 0)
            FROM (
              SELECT COUNT(1) AS ng_qty
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0
                AND DATE(COALESCE(r.end_time, r.confirmer_time, r.recorder_time, r.update_time, r.create_time, r.report_date))
                    BETWEEN #{startDate} AND #{endDate}
                AND EXISTS (SELECT 1 FROM mes_sfc_operation_report_defect d WHERE d.deleted = 0 AND d.report_id = r.id)
              UNION ALL
              SELECT COUNT(1)
              FROM mes_sfc_adhesive_report a
              WHERE a.deleted = 0 AND UPPER(COALESCE(a.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND DATE(COALESCE(a.end_time, a.confirmer_time, a.recorder_time, a.update_time, a.create_time, a.report_date))
                    BETWEEN #{startDate} AND #{endDate}
                AND ((a.self_check IS NOT NULL AND a.self_check != 'OK') OR NULLIF(a.defect_code, '') IS NOT NULL
                  OR a.product_quality_status != 'NORMAL')
              UNION ALL
              SELECT COUNT(1)
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0 AND s.scan_status = 'CONFIRMED'
                AND DATE(COALESCE(s.scan_time, s.update_time, s.create_time)) BETWEEN #{startDate} AND #{endDate}
                AND s.self_check IS NOT NULL AND s.self_check != 'OK'
              UNION ALL
              SELECT COUNT(1)
              FROM mes_sfc_press_slot_report ps
              WHERE ps.deleted = 0 AND ps.report_status = 'CONFIRMED'
                AND DATE(COALESCE(ps.end_time, ps.confirmer_time, ps.recorder_time, ps.update_time, ps.create_time, ps.report_date))
                    BETWEEN #{startDate} AND #{endDate}
                AND ((ps.self_check IS NOT NULL AND ps.self_check != 'OK') OR NULLIF(ps.defect_code, '') IS NOT NULL)
              UNION ALL
              SELECT COUNT(1)
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0 AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND DATE(COALESCE(a2.end_time, a2.confirmer_time, a2.recorder_time, a2.update_time, a2.create_time, a2.report_date))
                    BETWEEN #{startDate} AND #{endDate}
                AND ((a2.self_check IS NOT NULL AND a2.self_check != 'OK') OR NULLIF(a2.defect_code, '') IS NOT NULL
                  OR a2.product_quality_status != 'NORMAL')
              UNION ALL
              SELECT COUNT(1)
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0 AND cr.report_status = 'CONFIRMED'
                AND DATE(COALESCE(cr.end_time, cr.inspection_time, cr.recorder_time, cr.update_time, cr.create_time, cr.report_date))
                    BETWEEN #{startDate} AND #{endDate}
                AND ((cr.self_check IS NOT NULL AND cr.self_check != 'OK') OR NULLIF(cr.defect_code, '') IS NOT NULL)
            ) t
            """)
    BigDecimal selectNgCount(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(ship_qty), 0)
            FROM mes_inv_fg_outbound_order
            WHERE deleted = 0
              AND outbound_status = 'SHIPPED'
              AND shipping_time IS NOT NULL
              AND DATE(shipping_time) BETWEEN #{startDate} AND #{endDate}
            """)
    BigDecimal selectShippedQty(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("""
            <script>
            SELECT
              p.id AS planId,
              p.plan_no AS planNo,
              COALESCE(p.production_start_date, p.plan_date) AS planDate,
              p.plan_mode AS planMode,
              p.source_type AS sourceType,
              p.prod_type AS prodType,
              p.plan_status AS planStatus,
              COALESCE(NULLIF(p.model_code, ''), p.model_name) AS modelCode,
              p.remark AS requirement,
              COALESCE(NULLIF(p.size_spec, ''), p.size_name) AS sizeSpec,
              COALESCE(NULLIF(p.target_qty, 0), p.net_plan_qty, p.order_due_qty, 0) AS quantity,
              COALESCE(p.sales_order_delivery_date, p.production_end_date) AS dueDate,
              COALESCE(p.front_process_flag, 0) AS frontProcessFlag,
              COALESCE(p.post_process_flag, 0) AS postProcessFlag,
              p.batch_no AS batchNo,
              p.production_batch_no AS productionBatchNo,
              COALESCE(NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(p.batch_no, '')) AS motherRollNo,
              p.inventory_source_batch_nos AS inventorySourceBatchNos,
              CONCAT_WS('、',
                (
                  SELECT GROUP_CONCAT(DISTINCT NULLIF(so.target_plan_no, '') ORDER BY so.target_plan_no SEPARATOR '、')
                  FROM mes_pp_plan_split_order so
                  WHERE so.deleted = 0
                    AND so.source_plan_id = p.id
                    AND (so.target_plan_id IS NOT NULL OR so.split_status = 'PLAN_CREATED')
                ),
                (
                  SELECT GROUP_CONCAT(DISTINCT NULLIF(so.source_plan_no, '') ORDER BY so.source_plan_no SEPARATOR '、')
                  FROM mes_pp_plan_split_order so
                  WHERE so.deleted = 0
                    AND so.target_plan_id = p.id
                    AND (so.target_plan_id IS NOT NULL OR so.split_status = 'PLAN_CREATED')
                )
              ) AS splitPlanNos
            FROM mes_pp_plan_order p
            WHERE p.deleted = 0
              AND (
                (
                  COALESCE(p.production_start_date, p.plan_date) &lt;= #{req.endDate}
                  AND COALESCE(p.production_end_date, p.production_start_date, p.plan_date) &gt;= #{req.startDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_operation_report r
                  WHERE r.deleted = 0
                    AND r.plan_id = p.id
                    AND (
                      r.report_type = 'END'
                      OR (
                        r.start_time IS NOT NULL
                        AND (r.operation_name IN ('配料', '配方', '湿法')
                          OR r.source_menu_code IN ('FORMULA_REPORT', 'WET_REPORT', 'ROUGH_GRINDING_REPORT', 'ADHESIVE_REPORT'))
                      )
                    )
                    AND DATE(COALESCE(r.end_time, r.confirmer_time, r.recorder_time, r.start_time,
                      r.update_time, r.create_time, r.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_grinding_first_detail f
                  WHERE f.deleted = 0
                    AND f.plan_id = p.id
                    AND (f.start_time IS NOT NULL OR f.end_time IS NOT NULL)
                    AND DATE(COALESCE(f.end_time, f.start_time, f.update_time, f.create_time, f.report_date))
                      BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_grinding_second_detail s
                  WHERE s.deleted = 0
                    AND s.plan_id = p.id
                    AND (s.start_time IS NOT NULL OR s.end_time IS NOT NULL OR s.confirm_time IS NOT NULL)
                    AND DATE(COALESCE(s.end_time, s.confirm_time, s.start_time, s.update_time, s.create_time,
                      s.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_adhesive_report a
                  WHERE a.deleted = 0
                    AND a.plan_id = p.id
                    AND (a.report_status IN ('CONFIRMED', 'COMPLETED') OR a.confirmer_time IS NOT NULL
                      OR a.end_time IS NOT NULL)
                    AND DATE(COALESCE(a.end_time, a.confirmer_time, a.recorder_time, a.update_time, a.create_time,
                      a.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_slitting_slice_record sr
                  WHERE sr.deleted = 0
                    AND sr.plan_id = p.id
                    AND (sr.scan_status IN ('CONFIRMED', 'COMPLETED') OR sr.scan_time IS NOT NULL)
                    AND DATE(COALESCE(sr.scan_time, sr.update_time, sr.create_time))
                      BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_press_slot_report pr
                  WHERE pr.deleted = 0
                    AND pr.plan_id = p.id
                    AND (pr.report_status IN ('CONFIRMED', 'COMPLETED') OR pr.confirmer_time IS NOT NULL
                      OR pr.end_time IS NOT NULL)
                    AND DATE(COALESCE(pr.end_time, pr.confirmer_time, pr.recorder_time, pr.update_time, pr.create_time,
                      pr.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_adhesive2_report ar
                  WHERE ar.deleted = 0
                    AND ar.plan_id = p.id
                    AND (ar.report_status IN ('CONFIRMED', 'COMPLETED') OR ar.confirmer_time IS NOT NULL
                      OR ar.end_time IS NOT NULL)
                    AND DATE(COALESCE(ar.end_time, ar.confirmer_time, ar.recorder_time, ar.update_time, ar.create_time,
                      ar.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_cut_round_report cr
                  WHERE cr.deleted = 0
                    AND cr.plan_id = p.id
                    AND (cr.report_status IN ('CONFIRMED', 'COMPLETED') OR cr.inspection_time IS NOT NULL
                      OR cr.end_time IS NOT NULL)
                    AND DATE(COALESCE(cr.end_time, cr.inspection_time, cr.recorder_time, cr.update_time, cr.create_time,
                      cr.report_date)) BETWEEN #{req.startDate} AND #{req.endDate}
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_sfc_inner_pack_unit_item ip
                  LEFT JOIN mes_sfc_outer_pack_box_item obi ON obi.deleted = 0
                    AND (obi.inner_unit_id = ip.inner_unit_id OR obi.inner_unit_no = ip.inner_unit_no)
                  WHERE ip.deleted = 0
                    AND ip.plan_id = p.id
                    AND DATE(COALESCE(obi.scan_time, ip.scan_time, ip.update_time, ip.create_time))
                      BETWEEN #{req.startDate} AND #{req.endDate}
                )
              )
              <if test="req.keyword != null and req.keyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.model_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.parent_production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.inventory_source_batch_nos LIKE CONCAT('%', #{req.keyword}, '%')
                  OR p.remark LIKE CONCAT('%', #{req.keyword}, '%'))
              </if>
              <if test="req.planStatuses != null and req.planStatuses.size() &gt; 0">
                AND p.plan_status IN
                <foreach collection="req.planStatuses" item="status" open="(" separator="," close=")">
                  #{status}
                </foreach>
              </if>
              <if test="req.modelCode != null and req.modelCode != ''">
                AND p.model_code = #{req.modelCode}
              </if>
              <if test="req.sizeSpec != null and req.sizeSpec != ''">
                AND p.size_spec = #{req.sizeSpec}
              </if>
            ORDER BY COALESCE(p.production_start_date, p.plan_date) ASC, p.plan_no ASC
            LIMIT 200
            </script>
            """)
    List<PlanRow> selectPlanRows(@Param("req") HcScheduleWorkbenchReqVO reqVO);

    @Select("""
            <script>
            SELECT *
            FROM (
              SELECT
                p.id AS planId,
                o.id AS planOperationId,
                CASE
                  WHEN COALESCE(p.production_start_date, p.plan_date) IS NULL THEN p.plan_date
                  ELSE DATE_ADD(COALESCE(p.production_start_date, p.plan_date), INTERVAL (GREATEST(o.op_seq, 1) - 1) DAY)
                END AS scheduleDate,
                o.op_seq AS opSeq,
                o.op_name AS operationName,
                o.operation_status AS operationStatus,
                o.work_center_name AS workCenterName,
                o.equipment_name AS equipmentName
              FROM mes_pp_plan_operation o
              INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
              WHERE o.deleted = 0
                AND p.deleted = 0
                AND p.id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                <if test="operationName != null and operationName != ''">
                  AND (o.op_name = #{operationName}
                    OR (#{operationName} IN ('磨皮1', '磨皮2') AND o.op_name = '磨皮')
                    OR (#{operationName} IN ('包装入库', '包装出库') AND (o.op_name LIKE '%包装%' OR o.op_name LIKE '%内包%')))
                </if>
            ) t
            WHERE t.scheduleDate BETWEEN #{startDate} AND #{endDate}
            ORDER BY t.planId ASC, t.scheduleDate ASC, t.opSeq ASC
            </script>
            """)
    List<ScheduleOperationRow> selectScheduleOperationRows(@Param("planIds") Collection<Long> planIds,
                                                           @Param("startDate") LocalDate startDate,
                                                           @Param("endDate") LocalDate endDate,
                                                           @Param("operationName") String operationName);

    @Select("""
            <script>
            SELECT
              o.plan_id AS planId,
              COALESCE(o.op_seq, 0) AS opSeq,
              o.op_name AS operationName,
              o.op_code AS operationCode
            FROM mes_pp_plan_operation o
            WHERE o.deleted = 0
              AND o.plan_id IN
              <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
              </foreach>
            ORDER BY o.plan_id ASC, COALESCE(o.op_seq, 0) ASC, o.id ASC
            </script>
            """)
    List<PlanOperationScopeRow> selectPlanOperationScopeRows(@Param("planIds") Collection<Long> planIds);

    @Select("""
            <script>
            SELECT
              plan_scope.plan_id AS planId,
              GREATEST(COALESCE(actual_units.total_count, 0), COALESCE(planned_units.planned_count, 0)) AS totalCount,
              LEAST(COALESCE(actual_units.finished_count, 0),
                GREATEST(COALESCE(actual_units.total_count, 0), COALESCE(planned_units.planned_count, 0))) AS finishedCount
            FROM (
              <foreach collection="planIds" item="planId" separator=" UNION ALL ">
                SELECT #{planId} AS plan_id
              </foreach>
            ) plan_scope
            LEFT JOIN (
              SELECT o.plan_id, COUNT(1) AS planned_count
              FROM mes_pp_plan_operation o
              WHERE o.deleted = 0
                AND o.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (
                  UPPER(COALESCE(o.op_code, '')) IN (
                    'OP-FORMULA', 'OP-WET', 'OP-GRINDING', 'OP-GRINDING1', 'OP-GRINDING2',
                    'OP-ADHESIVE1', 'OP-SLIT', 'OP-SLITTING', 'OP-PRESS-SLOT',
                    'OP-ADHESIVE2', 'OP-CUT', 'OP-CUT-ROUND'
                  )
                  OR o.op_name IN ('配料', '湿法', '磨皮', '磨皮1', '磨皮2', '粘胶1', '粘胶2', '分切', '压槽', '裁切', '裁圆')
                  OR o.op_name LIKE '%粘胶1%'
                  OR o.op_name LIKE '%粘胶2%'
                  OR o.op_name LIKE '%粘双面胶%'
                  OR o.op_name LIKE '%背胶%'
                  OR o.op_name LIKE '%分切%'
                  OR o.op_name LIKE '%压槽%'
                  OR o.op_name LIKE '%裁切%'
                  OR o.op_name LIKE '%裁圆%'
                )
              GROUP BY o.plan_id
            ) planned_units ON planned_units.plan_id = plan_scope.plan_id
            LEFT JOIN (
              SELECT
                completion_units.plan_id AS plan_id,
                COUNT(1) AS total_count,
                SUM(completion_units.completed) AS finished_count
              FROM (
                SELECT raw_units.plan_id, raw_units.unit_key, MIN(raw_units.completed) AS completed
                FROM (
                SELECT r.plan_id, 'FORMULA' AS unit_key, 1 AS completed
                FROM mes_sfc_operation_report r
                WHERE r.deleted = 0
                  AND r.report_type = 'END'
                  AND (r.operation_name = '配料' OR r.source_menu_code = 'FORMULA_REPORT')
                  AND r.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY r.plan_id
                UNION ALL
                SELECT r.plan_id, 'WET' AS unit_key, 1 AS completed
                FROM mes_sfc_operation_report r
                WHERE r.deleted = 0
                  AND r.report_type = 'END'
                  AND (r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT')
                  AND r.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY r.plan_id
                UNION ALL
                SELECT f.plan_id,
                       CONCAT('GRINDING1:', CAST(f.id AS CHAR)) AS unit_key,
                       CASE WHEN UPPER(COALESCE(f.detail_status, 'SUBMITTED')) IN ('SUBMITTED', 'CONFIRMED') THEN 1 ELSE 0 END AS completed
                FROM mes_sfc_grinding_first_detail f
                WHERE f.deleted = 0
                  AND COALESCE(f.detail_status, 'SUBMITTED') != 'VOID'
                  AND f.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                UNION ALL
                SELECT s.plan_id,
                       CONCAT('GRINDING2:', CAST(s.id AS CHAR)) AS unit_key,
                       CASE WHEN UPPER(COALESCE(s.detail_status, s.confirm_status, 'SUBMITTED')) IN ('SUBMITTED', 'CONFIRMED') THEN 1 ELSE 0 END AS completed
                FROM mes_sfc_grinding_second_detail s
                WHERE s.deleted = 0
                  AND COALESCE(s.detail_status, s.confirm_status, 'SUBMITTED') != 'VOID'
                  AND s.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                UNION ALL
                SELECT a.plan_id,
                       CONCAT('ADHESIVE1:', COALESCE(CAST(a.source_grinding_second_detail_id AS CHAR),
                         CONCAT('LOCK-', CAST(a.source_plan_lock_id AS CHAR)), CAST(a.id AS CHAR))) AS unit_key,
                       CASE WHEN SUM(CASE WHEN UPPER(COALESCE(a.report_status, '')) IN ('SUBMITTED', 'COMPLETED') THEN 0 ELSE 1 END) = 0 THEN 1 ELSE 0 END AS completed
                FROM mes_sfc_adhesive_report a
                WHERE a.deleted = 0
                  AND UPPER(COALESCE(a.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                  AND a.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY a.plan_id, CONCAT('ADHESIVE1:', COALESCE(CAST(a.source_grinding_second_detail_id AS CHAR),
                  CONCAT('LOCK-', CAST(a.source_plan_lock_id AS CHAR)), CAST(a.id AS CHAR)))
                UNION ALL
                SELECT src.plan_id,
                       CONCAT('SLITTING:', CAST(src.id AS CHAR)) AS unit_key,
                       CASE WHEN SUM(CASE
                         WHEN LOWER(REPLACE(COALESCE(src.extra_json, ''), ' ', '')) LIKE '%"slittingsourcestatus":"completed"%'
                           OR LOWER(REPLACE(COALESCE(src.extra_json, ''), ' ', '')) LIKE '%"slittingsourcecompleted":true%'
                         THEN 0 ELSE 1 END) = 0 THEN 1 ELSE 0 END AS completed
                FROM mes_sfc_adhesive_report src
                WHERE src.deleted = 0
                  AND UPPER(COALESCE(src.report_status, '')) IN ('CONFIRMED', 'SUBMITTED')
                  AND src.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY src.plan_id, CONCAT('SLITTING:', CAST(src.id AS CHAR))
                UNION ALL
                SELECT ps.plan_id,
                       CONCAT('PRESS_SLOT:', MD5(CONVERT(COALESCE(NULLIF(ps.source_batch_no, ''), NULLIF(ps.source_production_batch_no, ''),
                         NULLIF(ps.parent_production_batch_no, ''), NULLIF(ps.production_batch_no, ''), CAST(ps.id AS CHAR)) USING utf8mb4))) AS unit_key,
                       CASE WHEN SUM(CASE WHEN UPPER(COALESCE(ps.report_status, '')) = 'SUBMITTED' THEN 0 ELSE 1 END) = 0 THEN 1 ELSE 0 END AS completed
                FROM mes_sfc_press_slot_report ps
                WHERE ps.deleted = 0
                  AND UPPER(COALESCE(ps.report_status, '')) IN ('CONFIRMED', 'SUBMITTED')
                  AND ps.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY ps.plan_id, CONCAT('PRESS_SLOT:', MD5(CONVERT(COALESCE(NULLIF(ps.source_batch_no, ''), NULLIF(ps.source_production_batch_no, ''),
                  NULLIF(ps.parent_production_batch_no, ''), NULLIF(ps.production_batch_no, ''), CAST(ps.id AS CHAR)) USING utf8mb4)))
                UNION ALL
                SELECT a2.plan_id,
                       CONCAT('ADHESIVE2:', MD5(CONVERT(COALESCE(NULLIF(a2.source_batch_no, ''), NULLIF(a2.source_production_batch_no, ''),
                         NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''), CAST(a2.id AS CHAR)) USING utf8mb4))) AS unit_key,
                       CASE WHEN SUM(CASE WHEN UPPER(COALESCE(a2.report_status, '')) IN ('SUBMITTED', 'COMPLETED') THEN 0 ELSE 1 END) = 0 THEN 1 ELSE 0 END AS completed
                FROM mes_sfc_adhesive2_report a2
                WHERE a2.deleted = 0
                  AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                  AND a2.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY a2.plan_id, CONCAT('ADHESIVE2:', MD5(CONVERT(COALESCE(NULLIF(a2.source_batch_no, ''), NULLIF(a2.source_production_batch_no, ''),
                  NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''), CAST(a2.id AS CHAR)) USING utf8mb4)))
                UNION ALL
                SELECT cr.plan_id,
                       CONCAT('CUT_ROUND:', MD5(CONVERT(COALESCE(NULLIF(cr.source_batch_no, ''), NULLIF(cr.source_production_batch_no, ''),
                         NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.production_batch_no, ''), CAST(cr.id AS CHAR)) USING utf8mb4))) AS unit_key,
                       CASE WHEN SUM(CASE WHEN UPPER(COALESCE(cr.report_status, '')) = 'SUBMITTED' THEN 0 ELSE 1 END) = 0 THEN 1 ELSE 0 END AS completed
                FROM mes_sfc_cut_round_report cr
                WHERE cr.deleted = 0
                  AND UPPER(COALESCE(cr.report_status, '')) IN ('CONFIRMED', 'SUBMITTED')
                  AND cr.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY cr.plan_id, CONCAT('CUT_ROUND:', MD5(CONVERT(COALESCE(NULLIF(cr.source_batch_no, ''), NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.production_batch_no, ''), CAST(cr.id AS CHAR)) USING utf8mb4)))
                ) raw_units
                GROUP BY raw_units.plan_id, raw_units.unit_key
              ) completion_units
              GROUP BY completion_units.plan_id
            ) actual_units ON actual_units.plan_id = plan_scope.plan_id
            </script>
            """)
    List<OperationCompletionRow> selectOperationCompletionRows(@Param("planIds") Collection<Long> planIds);

    @Select("""
            <script>
            SELECT
              source_rows.plan_id AS planId,
              NULLIF(source_rows.segment_batch_no, '') AS segmentBatchNo,
              source_rows.source_type AS sourceType,
              SUM(source_rows.source_count) AS sourceCount
            FROM (
              SELECT d.plan_id,
                     COALESCE(NULLIF(d.confirmed_batch_no, ''), NULLIF(d.production_batch_no, ''),
                       NULLIF(d.parent_production_batch_no, ''), CONCAT('SECOND-', d.id)) AS segment_batch_no,
                     'SECOND_SEGMENT' AS source_type,
                     COUNT(DISTINCT d.id) AS source_count
              FROM mes_sfc_grinding_second_detail d
              LEFT JOIN (
                SELECT sd.source_id, SUM(COALESCE(sd.split_qty, 0)) AS split_qty
                FROM mes_pp_plan_split_detail sd
                INNER JOIN mes_pp_plan_split_order so ON so.id = sd.split_order_id AND so.deleted = 0
                WHERE sd.deleted = 0
                  AND sd.source_table = 'mes_sfc_grinding_second_detail'
                  AND so.source_plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                  AND (so.target_plan_id IS NOT NULL OR so.split_status = 'PLAN_CREATED')
                GROUP BY sd.source_id
              ) split_out ON split_out.source_id = d.id
              WHERE d.deleted = 0
                AND COALESCE(d.detail_status, d.confirm_status, 'SUBMITTED') != 'VOID'
                AND d.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (COALESCE(split_out.split_qty, 0) = 0
                  OR COALESCE(d.output_length, d.process_length, 1) &gt; COALESCE(split_out.split_qty, 0))
              GROUP BY d.plan_id, COALESCE(NULLIF(d.confirmed_batch_no, ''), NULLIF(d.production_batch_no, ''),
                NULLIF(d.parent_production_batch_no, ''), CONCAT('SECOND-', d.id))
              UNION ALL
              SELECT d.plan_id,
                     COALESCE(NULLIF(d.confirmed_batch_no, ''), NULLIF(d.production_batch_no, ''),
                       NULLIF(d.parent_production_batch_no, ''), CONCAT('SECOND-', d.id)),
                     'SECOND_COMPLETED',
                     COUNT(DISTINCT d.id)
              FROM mes_sfc_grinding_second_detail d
              WHERE d.deleted = 0
                AND (UPPER(COALESCE(d.detail_status, '')) = 'CONFIRMED'
                  OR UPPER(COALESCE(d.confirm_status, '')) = 'CONFIRMED')
                AND d.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY d.plan_id, COALESCE(NULLIF(d.confirmed_batch_no, ''), NULLIF(d.production_batch_no, ''),
                NULLIF(d.parent_production_batch_no, ''), CONCAT('SECOND-', d.id))
              UNION ALL
              SELECT so.target_plan_id,
                     COALESCE(NULLIF(sd.source_production_batch_no, ''), NULLIF(sd.source_batch_no, ''),
                       NULLIF(d.confirmed_batch_no, ''), NULLIF(d.production_batch_no, ''), CONCAT('SPLIT-', sd.id)),
                     'SECOND_SEGMENT',
                     COUNT(DISTINCT sd.id)
              FROM mes_pp_plan_split_detail sd
              INNER JOIN mes_pp_plan_split_order so ON so.id = sd.split_order_id AND so.deleted = 0
              LEFT JOIN mes_sfc_grinding_second_detail d
                ON sd.source_table = 'mes_sfc_grinding_second_detail' AND d.id = sd.source_id AND d.deleted = 0
              WHERE sd.deleted = 0
                AND so.target_plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (so.target_plan_id IS NOT NULL OR so.split_status = 'PLAN_CREATED')
                AND sd.source_table IN ('mes_sfc_grinding_second_detail', 'mes_sfc_adhesive_report',
                  'mes_sfc_slitting_slice_record', 'mes_sfc_press_slot_report', 'mes_sfc_adhesive2_report')
              GROUP BY so.target_plan_id, COALESCE(NULLIF(sd.source_production_batch_no, ''), NULLIF(sd.source_batch_no, ''),
                NULLIF(d.confirmed_batch_no, ''), NULLIF(d.production_batch_no, ''), CONCAT('SPLIT-', sd.id))
              UNION ALL
              SELECT a.plan_id,
                     COALESCE(NULLIF(a.production_batch_no, ''), NULLIF(a.source_production_batch_no, ''),
                       NULLIF(a.source_batch_no, ''), NULLIF(a.parent_production_batch_no, ''), CONCAT('ADH1-', a.id)),
                     'ADHESIVE1_COMPLETED',
                     COUNT(DISTINCT a.id)
              FROM mes_sfc_adhesive_report a
              WHERE a.deleted = 0
                AND UPPER(COALESCE(a.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND a.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY a.plan_id, COALESCE(NULLIF(a.production_batch_no, ''), NULLIF(a.source_production_batch_no, ''),
                NULLIF(a.source_batch_no, ''), NULLIF(a.parent_production_batch_no, ''), CONCAT('ADH1-', a.id))
              UNION ALL
              SELECT s.plan_id,
                     COALESCE(NULLIF(s.slice_serial_no, ''), NULLIF(s.source_production_batch_no, ''),
                       NULLIF(s.source_batch_no, ''), CONCAT('SLIT-', s.id)),
                     'SLITTING_CONFIRMED',
                     COUNT(DISTINCT s.id)
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND UPPER(COALESCE(s.scan_status, '')) = 'CONFIRMED'
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY s.plan_id, COALESCE(NULLIF(s.slice_serial_no, ''), NULLIF(s.source_production_batch_no, ''),
                NULLIF(s.source_batch_no, ''), CONCAT('SLIT-', s.id))
              UNION ALL
              SELECT ps.plan_id,
                     COALESCE(NULLIF(ps.production_batch_no, ''), NULLIF(ps.source_production_batch_no, ''),
                       NULLIF(ps.source_batch_no, ''), NULLIF(ps.parent_production_batch_no, ''), CONCAT('PRESS-', ps.id)),
                     'PRESS_COMPLETED',
                     COUNT(DISTINCT COALESCE(ps.source_slitting_slice_id, ps.id))
              FROM mes_sfc_press_slot_report ps
              WHERE ps.deleted = 0
                AND UPPER(COALESCE(ps.report_status, '')) = 'CONFIRMED'
                AND ps.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY ps.plan_id, COALESCE(NULLIF(ps.production_batch_no, ''), NULLIF(ps.source_production_batch_no, ''),
                NULLIF(ps.source_batch_no, ''), NULLIF(ps.parent_production_batch_no, ''), CONCAT('PRESS-', ps.id))
              UNION ALL
              SELECT a2.plan_id,
                     COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(a2.source_production_batch_no, ''),
                       NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), CONCAT('ADH2-', a2.id)),
                     'ADHESIVE2_COMPLETED',
                     COUNT(DISTINCT COALESCE(a2.source_slitting_slice_id, a2.source_press_slot_report_id, a2.id))
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND (
                  UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                  OR EXISTS (
                    SELECT 1
                    FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.plan_order_id = a2.plan_id
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND (fai.source_report_id = a2.id OR UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(
                        NULLIF(a2.production_batch_no, ''), NULLIF(a2.source_production_batch_no, ''),
                        NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, '')
                      ))), '%'))
                  )
                )
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY a2.plan_id, COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(a2.source_production_batch_no, ''),
                NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), CONCAT('ADH2-', a2.id))
              UNION ALL
              SELECT cr.plan_id,
                     COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_batch_no, ''),
                       NULLIF(cr.source_production_batch_no, ''), NULLIF(cr.production_batch_no, ''), CONCAT('CUT-', cr.id)),
                     'CUT_SEGMENT_FINISHED',
                     COUNT(DISTINCT cr.id)
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0
                AND UPPER(COALESCE(cr.report_status, '')) = 'SUBMITTED'
                AND cr.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY cr.plan_id, COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_batch_no, ''),
                NULLIF(cr.source_production_batch_no, ''), NULLIF(cr.production_batch_no, ''), CONCAT('CUT-', cr.id))
              UNION ALL
              SELECT s.plan_id,
                     COALESCE(NULLIF(s.slice_serial_no, ''), NULLIF(s.source_production_batch_no, ''),
                       NULLIF(s.source_batch_no, ''), CONCAT('SLIT-', s.id)),
                     'PIECE_DENOMINATOR',
                     1
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND (
                  UPPER(COALESCE(s.scan_status, '')) = 'CONFIRMED'
                  OR (s.self_check IS NOT NULL AND UPPER(s.self_check) != 'OK')
                )
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT s.plan_id,
                     COALESCE(NULLIF(s.slice_serial_no, ''), NULLIF(s.source_production_batch_no, ''),
                       NULLIF(s.source_batch_no, ''), CONCAT('SLIT-', s.id)),
                     'PIECE_NUMERATOR',
                     1
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.self_check IS NOT NULL
                AND UPPER(s.self_check) != 'OK'
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT ps.plan_id,
                     COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(ps.production_batch_no, ''),
                       NULLIF(ps.source_production_batch_no, ''), NULLIF(ps.source_batch_no, ''),
                       NULLIF(ps.parent_production_batch_no, ''), CONCAT('PRESS-', ps.id)),
                     'PIECE_NUMERATOR',
                     1
              FROM mes_sfc_press_slot_report ps
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = ps.source_slitting_slice_id
              WHERE ps.deleted = 0
                AND (
                  UPPER(COALESCE(ps.report_status, '')) = 'CONFIRMED'
                  OR (ps.self_check IS NOT NULL AND UPPER(ps.self_check) != 'OK')
                  OR NULLIF(ps.defect_code, '') IS NOT NULL
                )
                AND ps.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT fai.plan_order_id,
                     NULLIF(fai.product_batch_no, ''),
                     'PIECE_NUMERATOR',
                     1
              FROM mes_qms_fai_order fai
              WHERE fai.deleted = 0
                AND fai.source_module = 'PRESS_SLOT_REPORT'
                AND NULLIF(fai.product_batch_no, '') IS NOT NULL
                AND fai.plan_order_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT a2.plan_id,
                     COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''),
                       NULLIF(a2.production_batch_no, ''), NULLIF(a2.source_production_batch_no, ''),
                       NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''),
                       CONCAT('ADH2-', a2.id)),
                     'PIECE_NUMERATOR',
                     1
              FROM mes_sfc_adhesive2_report a2
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = a2.source_slitting_slice_id
              LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = 0 AND ps.id = a2.source_press_slot_report_id
              LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = 0 AND psl.id = ps.source_slitting_slice_id
              WHERE a2.deleted = 0
                AND (
                  UPPER(COALESCE(a2.report_status, '')) = 'CONFIRMED'
                  OR (a2.self_check IS NOT NULL AND UPPER(a2.self_check) != 'OK')
                  OR NULLIF(a2.defect_code, '') IS NOT NULL
                  OR (a2.product_quality_status IS NOT NULL AND UPPER(a2.product_quality_status) != 'NORMAL')
                )
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT fai.plan_order_id,
                     NULLIF(fai.product_batch_no, ''),
                     'PIECE_NUMERATOR',
                     1
              FROM mes_qms_fai_order fai
              WHERE fai.deleted = 0
                AND fai.source_module = 'ADHESIVE2_REPORT'
                AND NULLIF(fai.product_batch_no, '') IS NOT NULL
                AND fai.plan_order_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT cr.plan_id,
                     COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''),
                       NULLIF(a2sl.slice_serial_no, ''), NULLIF(cr.production_batch_no, ''),
                       NULLIF(cr.source_production_batch_no, ''), NULLIF(cr.source_batch_no, ''),
                       NULLIF(cr.parent_production_batch_no, ''), CONCAT('CUT-', cr.id)),
                     'PIECE_NUMERATOR',
                     1
              FROM mes_sfc_cut_round_report cr
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = cr.source_slitting_slice_id
              LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = 0 AND ps.id = cr.source_press_slot_report_id
              LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = 0 AND psl.id = ps.source_slitting_slice_id
              LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = 0 AND a2.id = cr.source_adhesive2_report_id
              LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = 0 AND a2sl.id = a2.source_slitting_slice_id
              WHERE cr.deleted = 0
                AND (
                  UPPER(COALESCE(cr.report_status, '')) = 'CONFIRMED'
                  OR UPPER(COALESCE(cr.inspection_status, '')) = 'COMPLETED'
                  OR cr.inspection_time IS NOT NULL
                  OR (cr.self_check IS NOT NULL AND UPPER(cr.self_check) != 'OK')
                  OR NULLIF(cr.defect_code, '') IS NOT NULL
                  OR UPPER(COALESCE(cr.inspection_result, '')) = 'NG'
                )
                AND cr.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT so.source_plan_id,
                     COALESCE(NULLIF(sd.source_production_batch_no, ''), NULLIF(sd.source_batch_no, ''), CONCAT('SPLIT-OUT-', sd.id)),
                     'SPLIT_OUT_PIECE',
                     COUNT(DISTINCT sd.id)
              FROM mes_pp_plan_split_detail sd
              INNER JOIN mes_pp_plan_split_order so ON so.id = sd.split_order_id AND so.deleted = 0
              WHERE sd.deleted = 0
                AND so.source_plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (so.target_plan_id IS NOT NULL OR so.split_status = 'PLAN_CREATED')
                AND sd.source_table IN ('mes_sfc_press_slot_report', 'mes_sfc_adhesive2_report')
              GROUP BY so.source_plan_id, COALESCE(NULLIF(sd.source_production_batch_no, ''), NULLIF(sd.source_batch_no, ''), CONCAT('SPLIT-OUT-', sd.id))
              UNION ALL
              SELECT so.target_plan_id,
                     COALESCE(NULLIF(sd.source_production_batch_no, ''), NULLIF(sd.source_batch_no, ''), CONCAT('SPLIT-IN-', sd.id)),
                     CASE
                       WHEN sd.source_table = 'mes_sfc_press_slot_report' THEN 'SPLIT_IN_PRESS_PIECE'
                       WHEN sd.source_table = 'mes_sfc_adhesive2_report' THEN 'SPLIT_IN_ADHESIVE2_PIECE'
                       ELSE 'SPLIT_IN_PIECE'
                     END,
                     COUNT(DISTINCT sd.id)
              FROM mes_pp_plan_split_detail sd
              INNER JOIN mes_pp_plan_split_order so ON so.id = sd.split_order_id AND so.deleted = 0
              WHERE sd.deleted = 0
                AND so.target_plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (so.target_plan_id IS NOT NULL OR so.split_status = 'PLAN_CREATED')
                AND sd.source_table IN ('mes_sfc_press_slot_report', 'mes_sfc_adhesive2_report')
              GROUP BY so.target_plan_id, COALESCE(NULLIF(sd.source_production_batch_no, ''), NULLIF(sd.source_batch_no, ''), CONCAT('SPLIT-IN-', sd.id)),
                CASE
                  WHEN sd.source_table = 'mes_sfc_press_slot_report' THEN 'SPLIT_IN_PRESS_PIECE'
                  WHEN sd.source_table = 'mes_sfc_adhesive2_report' THEN 'SPLIT_IN_ADHESIVE2_PIECE'
                  ELSE 'SPLIT_IN_PIECE'
                END
            ) source_rows
            WHERE source_rows.plan_id IS NOT NULL
              AND NULLIF(source_rows.segment_batch_no, '') IS NOT NULL
            GROUP BY source_rows.plan_id, source_rows.segment_batch_no, source_rows.source_type
            </script>
            """)
    List<SegmentProgressRow> selectSegmentProgressRows(@Param("planIds") Collection<Long> planIds);

    @Select("""
            <script>
            SELECT
              plan_id AS planId,
              plan_operation_id AS planOperationId,
              report_date AS reportDate,
              operation_name AS operationName,
              fact_source AS factSource,
              NULLIF(segment_batch_no, '') AS segmentBatchNo,
              CASE
                WHEN operation_name = '湿法' THEN MAX(input_qty)
                ELSE SUM(input_qty)
              END AS inputQty,
              MAX(input_unit) AS inputUnit,
              SUM(report_qty) AS reportQty,
              MAX(report_unit) AS reportUnit,
              GREATEST(SUM(pending_qty), 0) AS pendingQty,
              MAX(pending_unit) AS pendingUnit,
              SUM(loss_ng_qty) AS lossNgQty,
              MAX(loss_ng_unit) AS lossNgUnit,
              SUM(record_count) AS recordCount,
              SUM(ng_qty) AS ngQty,
              CASE
                WHEN MAX(CASE
                  WHEN COALESCE(t.coa_flag, 0) > 0
                  THEN 1 ELSE 0 END) > 0
                THEN TRUE ELSE FALSE
              END AS coaFlag,
              CASE
                WHEN MAX(CASE
                      WHEN COALESCE(t.coa_flag, 0) > 0
                   AND NULLIF(t.segment_batch_no, '') IS NOT NULL
                   AND EXISTS (
                    SELECT 1
                    FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND fai.source_report_no LIKE '%-COA-%'
                      AND fai.plan_order_id = t.plan_id
                      AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) LIKE CONCAT(UPPER(TRIM(t.segment_batch_no)), '%')
                      AND (fai.judgment = 'NG' OR fai.status = 'REJECTED')
                   )
                  THEN 1 ELSE 0 END) > 0
                THEN TRUE ELSE FALSE
              END AS coaNgFlag,
              CASE
                WHEN operation_name IN ('配料', '湿法', '磨皮1', '磨皮2', '粘胶1')
                  THEN CASE WHEN MAX(COALESCE(t.completed_flag, 0)) > 0 THEN TRUE ELSE FALSE END
                ELSE CASE WHEN MIN(COALESCE(t.completed_flag, 0)) > 0 THEN TRUE ELSE FALSE END
              END AS completedFlag,
              GROUP_CONCAT(DISTINCT NULLIF(equipment_name, '') ORDER BY equipment_name SEPARATOR '、') AS equipmentNames,
              MAX(last_report_time) AS lastReportTime,
              GROUP_CONCAT(DISTINCT NULLIF(glue_board_model, '') ORDER BY glue_board_model SEPARATOR '、') AS glueBoardModel,
              GROUP_CONCAT(DISTINCT NULLIF(first_inspection_result, '') ORDER BY first_inspection_result SEPARATOR '、') AS firstInspectionResult
            FROM (
              SELECT r.plan_id, r.plan_operation_id,
                     DATE(COALESCE(r.end_time, r.confirmer_time, r.recorder_time, r.start_time, r.update_time, r.create_time, r.report_date)) AS report_date,
                     CASE
                       WHEN r.operation_name = '配料' OR r.source_menu_code = 'FORMULA_REPORT' THEN '配料'
                       WHEN r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT' THEN '湿法'
                       WHEN r.report_type = 'START' AND r.source_menu_code = 'ROUGH_GRINDING_REPORT' THEN '磨皮1'
                       WHEN r.report_type = 'START' AND r.source_menu_code = 'ADHESIVE_REPORT' THEN '粘胶1'
                       ELSE r.operation_name
                     END AS operation_name,
                     CASE
                       WHEN r.report_type = 'START' AND r.source_menu_code = 'ROUGH_GRINDING_REPORT' THEN '磨皮1'
                       WHEN r.report_type = 'START' AND r.source_menu_code = 'ADHESIVE_REPORT' THEN '粘胶1'
                       ELSE '统一报工'
                     END AS fact_source,
                     COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''), NULLIF(r.batch_no, '')) AS segment_batch_no,
                     CASE
                       WHEN r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT'
                         THEN COALESCE((
                           SELECT SUM(COALESCE(fr.feed_qty, fr.good_qty, 0))
                           FROM mes_sfc_operation_report fr
                           WHERE fr.deleted = 0
                             AND fr.report_type = 'END'
                             AND fr.plan_id = r.plan_id
                             AND (fr.operation_name = '配料' OR fr.source_menu_code = 'FORMULA_REPORT')
                         ), 0)
                       ELSE 0
                     END AS input_qty,
                     CASE
                       WHEN r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT' THEN 'kg'
                       ELSE ''
                     END AS input_unit,
                     CASE
                       WHEN r.operation_name = '配料' OR r.source_menu_code = 'FORMULA_REPORT'
                         THEN COALESCE(r.feed_qty, r.good_qty, 0)
                       ELSE COALESCE(r.good_qty, 0)
                     END AS report_qty,
                     CASE
                       WHEN r.operation_name = '配料' OR r.source_menu_code = 'FORMULA_REPORT' THEN 'kg'
                       WHEN r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT' THEN 'm'
                       ELSE ''
                     END AS report_unit,
                     0 AS pending_qty,
                     '' AS pending_unit,
                     CASE
                       WHEN r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT'
                         THEN COALESCE(r.scrap_qty, 0)
                       ELSE 0
                     END AS loss_ng_qty,
                     CASE
                       WHEN r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT' THEN 'm'
                       ELSE ''
                     END AS loss_ng_unit,
                     1 AS record_count,
                     CASE WHEN EXISTS (SELECT 1 FROM mes_sfc_operation_report_defect d WHERE d.deleted = 0 AND d.report_id = r.id)
                        THEN 1 ELSE 0 END AS ng_qty,
                     0 AS coa_flag,
                     CASE WHEN r.report_type = 'END' OR r.end_time IS NOT NULL OR r.confirmer_time IS NOT NULL THEN 1 ELSE 0 END AS completed_flag,
                     r.equipment_name,
                     COALESCE(r.end_time, r.confirmer_time, r.recorder_time, r.start_time, r.update_time, r.create_time) AS last_report_time,
                     NULL AS glue_board_model,
                     COALESCE((
                       SELECT COALESCE(NULLIF(fai.judgment, ''), NULLIF(fai.status, ''))
                       FROM mes_qms_fai_order fai
                       WHERE fai.deleted = 0
                         AND (fai.id = r.fai_id
                           OR (fai.source_module = r.source_menu_code AND fai.source_report_id = r.id))
                       ORDER BY fai.id DESC
                       LIMIT 1
                     ), CASE WHEN r.fai_id IS NOT NULL
                       THEN COALESCE(NULLIF(r.fai_judgment, ''), NULLIF(r.fai_status, ''))
                       ELSE NULL END) AS first_inspection_result
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0
                AND (
                  r.report_type = 'END'
                  OR (
                    r.start_time IS NOT NULL
                    AND (r.operation_name IN ('配料', '配方', '湿法')
                       OR r.source_menu_code IN ('FORMULA_REPORT', 'WET_REPORT', 'ROUGH_GRINDING_REPORT', 'ADHESIVE_REPORT'))
                  )
                )
                AND COALESCE(r.source_menu_code, '') != 'ROUGH_GRINDING_REPORT'
                AND NOT (
                  (
                    r.report_type = 'START'
                    AND r.source_menu_code = 'ROUGH_GRINDING_REPORT'
                    AND EXISTS (
                      SELECT 1
                      FROM mes_sfc_grinding_first_detail f2
                      WHERE f2.deleted = 0
                        AND f2.plan_id = r.plan_id
                        AND (f2.plan_operation_id = r.plan_operation_id OR r.plan_operation_id IS NULL)
                        AND COALESCE(f2.detail_status, 'SUBMITTED') != 'VOID'
                        AND (f2.start_time IS NOT NULL OR f2.end_time IS NOT NULL)
                        AND DATE(COALESCE(f2.end_time, f2.start_time, f2.update_time, f2.create_time, f2.report_date))
                          = DATE(COALESCE(r.start_time, r.update_time, r.create_time, r.report_date))
                    )
                  )
                  OR (
                    r.report_type = 'START'
                    AND r.source_menu_code = 'ADHESIVE_REPORT'
                    AND EXISTS (
                      SELECT 1
                      FROM mes_sfc_adhesive_report a2
                      WHERE a2.deleted = 0
                        AND a2.plan_id = r.plan_id
                        AND (a2.plan_operation_id = r.plan_operation_id OR r.plan_operation_id IS NULL)
                        AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                        AND DATE(COALESCE(a2.end_time, a2.confirmer_time, a2.recorder_time, a2.start_time,
                          a2.update_time, a2.create_time, a2.report_date))
                          = DATE(COALESCE(r.start_time, r.update_time, r.create_time, r.report_date))
                    )
                  )
                )
                AND r.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT f.plan_id, f.plan_operation_id,
                     DATE(COALESCE(fa.end_time, fa.start_time, f.end_time, f.start_time,
                                   fa.update_time, f.update_time, fa.create_time, f.create_time, f.report_date)),
                     '磨皮1', '磨皮1',
                     COALESCE(NULLIF(fa.production_batch_no, ''), NULLIF(f.source_production_batch_no, ''),
                              NULLIF(f.mother_batch_no, '')),
                     COALESCE(f.process_length, fa.confirmed_length, 0), 'm',
                     COALESCE(f.output_length, fa.confirmed_length, 0), 'm',
                     GREATEST(COALESCE(f.process_length, fa.confirmed_length, 0)
                       - COALESCE(f.output_length, fa.confirmed_length, 0)
                       - COALESCE(f.loss_length, 0) - COALESCE(f.nap_sample_length, 0), 0), 'm',
                     COALESCE(f.loss_length, 0), 'm',
                     1,
                     CASE WHEN (f.self_check IS NOT NULL AND f.self_check != 'OK')
                               OR NULLIF(f.defect_code, '') IS NOT NULL
                          THEN 1 ELSE 0 END,
                     0,
                     CASE WHEN COALESCE(fa.end_time, f.end_time) IS NOT NULL THEN 1 ELSE 0 END,
                     f.equipment_name, COALESCE(fa.end_time, fa.start_time, f.end_time, f.start_time,
                                                fa.update_time, f.update_time, fa.create_time, f.create_time),
                     NULL,
                     NULL
              FROM mes_sfc_grinding_first_detail f
              LEFT JOIN mes_sfc_grinding_first_allocation_detail fa
                ON fa.first_detail_id = f.id
               AND fa.tenant_id = f.tenant_id
               AND fa.deleted = 0
               AND COALESCE(fa.detail_status, 'ACTIVE') != 'VOID'
              WHERE f.deleted = 0
                AND COALESCE(f.detail_status, 'SUBMITTED') != 'VOID'
                AND (fa.start_time IS NOT NULL OR fa.end_time IS NOT NULL
                     OR f.start_time IS NOT NULL OR f.end_time IS NOT NULL)
                AND f.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT s.plan_id, s.plan_operation_id,
                     DATE(COALESCE(s.end_time, s.confirm_time, s.start_time, s.update_time, s.create_time, s.report_date)),
                     '磨皮2', '磨皮2',
                     CASE
                       WHEN s.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.production_batch_no
                       WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                       WHEN s.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.parent_production_batch_no
                       ELSE COALESCE(NULLIF(s.production_batch_no, ''), NULLIF(s.source_production_batch_no, ''), NULLIF(s.parent_production_batch_no, ''), NULLIF(s.mother_batch_no, ''))
                     END,
                     COALESCE(s.process_length, 0), 'm',
                     COALESCE(s.output_length, 0), 'm',
                     GREATEST(COALESCE(s.process_length, 0) - COALESCE(s.output_length, 0)
                       - COALESCE(s.loss_length, 0) - COALESCE(s.nap_sample_length, 0), 0), 'm',
                     COALESCE(s.loss_length, 0), 'm',
                     1,
                     CASE WHEN (s.self_check IS NOT NULL AND s.self_check != 'OK') OR NULLIF(s.defect_code, '') IS NOT NULL THEN 1 ELSE 0 END,
                     0,
                     CASE WHEN s.end_time IS NOT NULL OR s.confirm_time IS NOT NULL THEN 1 ELSE 0 END,
                     s.equipment_name, COALESCE(s.end_time, s.confirm_time, s.start_time, s.update_time, s.create_time),
                     NULL,
                     CASE WHEN s.inspection_id IS NOT NULL
                       THEN COALESCE(NULLIF(s.inspection_result, ''), NULLIF(s.inspection_status, ''))
                       ELSE NULL END
              FROM mes_sfc_grinding_second_detail s
              WHERE s.deleted = 0
                AND COALESCE(s.detail_status, s.confirm_status, 'SUBMITTED') != 'VOID'
                AND (s.start_time IS NOT NULL OR s.end_time IS NOT NULL OR s.confirm_time IS NOT NULL)
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT a.plan_id, a.plan_operation_id,
                     DATE(COALESCE(a.end_time, a.confirmer_time, a.recorder_time, a.update_time, a.create_time, a.report_date)),
                     '粘胶1', '粘胶1',
                     CASE
                       WHEN a.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.source_batch_no
                       WHEN a.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.source_production_batch_no
                       WHEN a.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a.source_production_batch_no, '-J', 1)
                       WHEN a.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.production_batch_no
                       WHEN a.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.parent_production_batch_no
                       ELSE COALESCE(NULLIF(a.source_production_batch_no, ''), NULLIF(a.source_batch_no, ''), NULLIF(a.parent_production_batch_no, ''), NULLIF(a.production_batch_no, ''))
                     END,
                     COALESCE(a.input_length, a.output_length, 0), 'm',
                     COALESCE(a.output_length, 0), 'm',
                     GREATEST(COALESCE(a.input_length, a.output_length, 0) - COALESCE(a.output_length, 0)
                       - COALESCE(a.loss_length, 0) - COALESCE(a.nap_sample_length, 0), 0), 'm',
                     COALESCE(a.loss_length, 0), 'm',
                     1,
                     CASE WHEN (a.self_check IS NOT NULL AND a.self_check != 'OK') OR NULLIF(a.defect_code, '') IS NOT NULL
                        OR a.product_quality_status != 'NORMAL' THEN 1 ELSE 0 END,
                     0,
                     CASE WHEN UPPER(COALESCE(a.report_status, '')) IN ('SUBMITTED', 'COMPLETED') THEN 1 ELSE 0 END,
                     NULL, COALESCE(a.end_time, a.confirmer_time, a.recorder_time, a.update_time, a.create_time),
                     (
                       SELECT NULLIF(st.glue_board_model, '')
                       FROM mes_sfc_adhesive_glue_board_usage u
                       LEFT JOIN mes_sfc_adhesive_glue_board_stock st ON st.id = u.glue_board_stock_id AND st.deleted = 0
                       WHERE u.deleted = 0 AND u.id = a.glue_board_usage_id
                       LIMIT 1
                     ),
                     COALESCE((
                       SELECT COALESCE(NULLIF(fai.judgment, ''), NULLIF(fai.status, ''))
                       FROM mes_qms_fai_order fai
                       WHERE fai.deleted = 0
                         AND (fai.id = a.fai_id
                           OR (fai.source_module = 'ADHESIVE_REPORT' AND fai.source_report_id = a.id))
                       ORDER BY fai.id DESC
                       LIMIT 1
                     ), CASE WHEN a.fai_id IS NOT NULL
                       THEN COALESCE(NULLIF(a.fai_judgment, ''), NULLIF(a.fai_status, ''))
                       ELSE NULL END)
              FROM mes_sfc_adhesive_report a
              WHERE a.deleted = 0
                AND UPPER(COALESCE(a.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND a.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT s.plan_id, s.plan_operation_id,
                     DATE(COALESCE(s.scan_time, s.update_time, s.create_time)),
                     '分切', '分切',
                     CASE
                       WHEN s.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_batch_no
                       WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                       WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(s.source_production_batch_no, '-J', 1)
                       WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(s.slice_serial_no, CASE WHEN s.slice_serial_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(s.slice_serial_no) - 4 ELSE CHAR_LENGTH(s.slice_serial_no) - 3 END)
                       ELSE COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, ''), NULLIF(s.slice_serial_no, ''))
                     END,
                     COALESCE(s.slice_length, 0), 'm',
                     CASE WHEN s.scan_status = 'CONFIRMED' THEN 1 ELSE 0 END, '片',
                     CASE WHEN s.scan_status = 'CONFIRMED' THEN 0 ELSE 1 END, '片',
                     CASE WHEN s.self_check IS NOT NULL AND s.self_check != 'OK' THEN 1 ELSE 0 END, '片',
                     1,
                     CASE WHEN s.self_check IS NOT NULL AND s.self_check != 'OK' THEN 1 ELSE 0 END,
                     0,
                     CASE WHEN s.scan_status = 'CONFIRMED' THEN 1 ELSE 0 END,
                     NULL, COALESCE(s.scan_time, s.update_time, s.create_time),
                     NULL,
                     NULL
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT ps.plan_id, ps.plan_operation_id,
                     DATE(COALESCE(ps.end_time, ps.confirmer_time, ps.recorder_time, ps.update_time, ps.create_time, ps.report_date)),
                     '压槽', '压槽',
                     CASE
                       WHEN ps.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN ps.source_batch_no
                       WHEN ps.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN ps.source_production_batch_no
                       WHEN ps.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(ps.source_production_batch_no, '-J', 1)
                       WHEN ps.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(ps.production_batch_no, CASE WHEN ps.production_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(ps.production_batch_no) - 4 ELSE CHAR_LENGTH(ps.production_batch_no) - 3 END)
                       WHEN ps.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN ps.parent_production_batch_no
                       ELSE COALESCE(NULLIF(ps.source_production_batch_no, ''), NULLIF(ps.source_batch_no, ''), NULLIF(ps.parent_production_batch_no, ''), NULLIF(ps.production_batch_no, ''))
                     END,
                     1, '片',
                     1, '片',
                     0, '片',
                     CASE WHEN (ps.self_check IS NOT NULL AND ps.self_check != 'OK') OR NULLIF(ps.defect_code, '') IS NOT NULL THEN 1 ELSE 0 END, '片',
                     1,
                     CASE WHEN (ps.self_check IS NOT NULL AND ps.self_check != 'OK') OR NULLIF(ps.defect_code, '') IS NOT NULL THEN 1 ELSE 0 END,
                     CASE
                       WHEN LOWER(REPLACE(COALESCE(ps.extra_json, ''), ' ', '')) LIKE '%"coaflag":true%'
                         OR LOWER(REPLACE(COALESCE(ps.extra_json, ''), ' ', '')) LIKE '%"coaflag":"true"%'
                         OR LOWER(REPLACE(COALESCE(ps.extra_json, ''), ' ', '')) LIKE '%"coaflag":"y"%'
                         OR LOWER(REPLACE(COALESCE(ps.extra_json, ''), ' ', '')) LIKE '%"coaflag":1%'
                      THEN 1 ELSE 0 END,
                     1,
                     NULL, COALESCE(ps.end_time, ps.confirmer_time, ps.recorder_time, ps.update_time, ps.create_time),
                     NULL,
                     (
                       SELECT COALESCE(NULLIF(fai.judgment, ''), NULLIF(fai.status, ''))
                       FROM mes_qms_fai_order fai
                       WHERE fai.deleted = 0
                         AND fai.source_module = 'PRESS_SLOT_REPORT'
                         AND fai.source_report_id = ps.id
                       ORDER BY fai.id DESC
                       LIMIT 1
                     )
              FROM mes_sfc_press_slot_report ps
              WHERE ps.deleted = 0 AND ps.report_status = 'CONFIRMED'
                AND ps.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT a2.plan_id, a2.plan_operation_id,
                     DATE(COALESCE(a2.end_time, a2.confirmer_time, a2.recorder_time, a2.update_time, a2.create_time, a2.report_date)),
                     '粘胶2', '粘胶2',
                     CASE
                       WHEN a2.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_batch_no
                       WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_production_batch_no
                       WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a2.source_production_batch_no, '-J', 1)
                       WHEN a2.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(a2.production_batch_no, CASE WHEN a2.production_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(a2.production_batch_no) - 4 ELSE CHAR_LENGTH(a2.production_batch_no) - 3 END)
                       WHEN a2.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.parent_production_batch_no
                       ELSE COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''))
                     END,
                     1, '片',
                     1, '片',
                     0, '片',
                     CASE WHEN (a2.self_check IS NOT NULL AND a2.self_check != 'OK') OR NULLIF(a2.defect_code, '') IS NOT NULL
                        OR a2.product_quality_status != 'NORMAL' THEN 1 ELSE 0 END, '片',
                     1,
                     CASE WHEN (a2.self_check IS NOT NULL AND a2.self_check != 'OK') OR NULLIF(a2.defect_code, '') IS NOT NULL
                        OR a2.product_quality_status != 'NORMAL' THEN 1 ELSE 0 END,
                     CASE
                       WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"coaflag":true%'
                         OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"coaflag":"true"%'
                         OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"coaflag":"y"%'
                         OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"coaflag":1%'
                         OR EXISTS (
                           SELECT 1
                           FROM mes_qms_fai_order fai
                           WHERE fai.deleted = 0
                             AND fai.source_module = 'ADHESIVE2_REPORT'
                             AND fai.source_report_no LIKE '%-COA-%'
                             AND fai.plan_order_id = a2.plan_id
                             AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(
                               NULLIF(a2.source_batch_no, ''),
                               NULLIF(a2.parent_production_batch_no, ''),
                               NULLIF(LEFT(a2.production_batch_no, GREATEST(CASE WHEN a2.production_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(a2.production_batch_no) - 4 ELSE CHAR_LENGTH(a2.production_batch_no) - 3 END, 0)), ''),
                             NULLIF(a2.source_production_batch_no, '')
                           ))), '%')
                         )
                      THEN 1 ELSE 0 END,
                     CASE WHEN UPPER(COALESCE(a2.report_status, '')) IN ('SUBMITTED', 'COMPLETED') THEN 1 ELSE 0 END,
                     NULL, COALESCE(a2.end_time, a2.confirmer_time, a2.recorder_time, a2.update_time, a2.create_time),
                     NULLIF(a2.glue_board_model, ''),
                     (
                       SELECT COALESCE(NULLIF(fai.judgment, ''), NULLIF(fai.status, ''))
                       FROM mes_qms_fai_order fai
                       WHERE fai.deleted = 0
                         AND fai.source_module = 'ADHESIVE2_REPORT'
                         AND (fai.source_report_id = a2.id
                           OR (
                             fai.plan_order_id = a2.plan_id
                             AND (fai.source_report_no LIKE '%-PROCESS-CHECK-%' OR fai.source_report_no LIKE '%-COA-%')
                             AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(
                               NULLIF(a2.source_batch_no, ''),
                               NULLIF(a2.parent_production_batch_no, ''),
                               NULLIF(LEFT(a2.production_batch_no, GREATEST(CASE WHEN a2.production_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(a2.production_batch_no) - 4 ELSE CHAR_LENGTH(a2.production_batch_no) - 3 END, 0)), ''),
                               NULLIF(a2.source_production_batch_no, '')
                             ))), '%')
                           ))
                       ORDER BY CASE WHEN fai.judgment = 'NG' OR fai.status = 'REJECTED' THEN 0 ELSE 1 END, fai.id DESC
                       LIMIT 1
                     )
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT cr.plan_id, cr.plan_operation_id,
                     DATE(COALESCE(cr.end_time, cr.inspection_time, cr.recorder_time, cr.update_time, cr.create_time, cr.report_date)),
                     '裁切', '裁切',
                     CASE
                       WHEN cr.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN cr.source_batch_no
                       WHEN cr.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN cr.source_production_batch_no
                       WHEN cr.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(cr.source_production_batch_no, '-J', 1)
                       WHEN cr.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(cr.production_batch_no, CASE WHEN cr.production_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(cr.production_batch_no) - 4 ELSE CHAR_LENGTH(cr.production_batch_no) - 3 END)
                       WHEN cr.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN cr.parent_production_batch_no
                       ELSE COALESCE(NULLIF(cr.source_production_batch_no, ''), NULLIF(cr.source_batch_no, ''), NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.production_batch_no, ''))
                     END,
                     1, '片',
                     1, '片',
                     0, '片',
                     CASE WHEN (cr.self_check IS NOT NULL AND cr.self_check != 'OK') OR NULLIF(cr.defect_code, '') IS NOT NULL THEN 1 ELSE 0 END, '片',
                     1,
                     CASE WHEN (cr.self_check IS NOT NULL AND cr.self_check != 'OK') OR NULLIF(cr.defect_code, '') IS NOT NULL THEN 1 ELSE 0 END,
                     CASE
                       WHEN LOWER(REPLACE(COALESCE(cr.extra_json, ''), ' ', '')) LIKE '%"coaflag":true%'
                         OR LOWER(REPLACE(COALESCE(cr.extra_json, ''), ' ', '')) LIKE '%"coaflag":"true"%'
                         OR LOWER(REPLACE(COALESCE(cr.extra_json, ''), ' ', '')) LIKE '%"coaflag":"y"%'
                         OR LOWER(REPLACE(COALESCE(cr.extra_json, ''), ' ', '')) LIKE '%"coaflag":1%'
                     THEN 1 ELSE 0 END,
                     1,
                     NULL, COALESCE(cr.end_time, cr.inspection_time, cr.recorder_time, cr.update_time, cr.create_time),
                     NULL,
                     CASE WHEN cr.inspection_task_id IS NOT NULL
                       THEN COALESCE(NULLIF(cr.inspection_result, ''), NULLIF(cr.inspection_status, ''))
                       ELSE NULL END
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0 AND cr.report_status = 'CONFIRMED'
                AND cr.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT pkg.plan_id, pkg.plan_operation_id, pkg.report_date,
                     '包装入库', '内包装',
                     pkg.segment_batch_no,
                     pkg.total_piece_count, '片',
                     pkg.packed_piece_count, '片',
                     pkg.wait_pack_piece_count, '片',
                     0, '',
                     pkg.packed_piece_count,
                     0,
                     0,
                     CASE WHEN pkg.wait_pack_piece_count &lt;= 0 THEN 1 ELSE 0 END,
                     NULL,
                     pkg.last_report_time,
                     NULL,
                     NULL
              FROM (
                SELECT
                  cr.plan_id,
                  MAX(cr.plan_operation_id) AS plan_operation_id,
                  DATE(MAX(COALESCE(packed.scan_time, cr.confirmer_time, cr.update_time, cr.create_time))) AS report_date,
                  COALESCE(cr.parent_production_batch_no, cr.source_production_batch_no, cr.production_batch_no) AS segment_batch_no,
                  COUNT(DISTINCT cr.id) AS total_piece_count,
                  SUM(CASE WHEN packed.id IS NULL THEN 0 ELSE 1 END) AS packed_piece_count,
                  SUM(CASE WHEN packed.id IS NULL THEN 1 ELSE 0 END) AS wait_pack_piece_count,
                  MAX(COALESCE(packed.scan_time, cr.confirmer_time, cr.update_time, cr.create_time)) AS last_report_time
                FROM mes_sfc_cut_round_report cr
                LEFT JOIN mes_sfc_inner_pack_unit_item packed
                  ON packed.source_cut_round_report_id = cr.id
                 AND packed.deleted = 0
                WHERE cr.deleted = 0
                  AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
                  AND cr.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY cr.plan_id, COALESCE(cr.parent_production_batch_no, cr.source_production_batch_no, cr.production_batch_no)
              ) pkg
              UNION ALL
              SELECT outb.plan_id, outb.plan_operation_id, outb.report_date,
                     '包装出库', '外包装',
                     outb.segment_batch_no,
                     outb.inner_piece_count, '片',
                     outb.outbound_piece_count, '片',
                     outb.wait_outer_piece_count, '片',
                     0, '',
                     outb.outbound_piece_count,
                     0,
                     0,
                     CASE WHEN outb.wait_outer_piece_count &lt;= 0 THEN 1 ELSE 0 END,
                     NULL,
                     outb.last_report_time,
                     NULL,
                     NULL
              FROM (
                SELECT
                  ip.plan_id,
                  MAX(ip.plan_operation_id) AS plan_operation_id,
                  DATE(MAX(COALESCE(obi.scan_time, ip.scan_time, ip.update_time, ip.create_time))) AS report_date,
                  CASE
                    WHEN ip.slice_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(ip.slice_batch_no, CASE WHEN ip.slice_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(ip.slice_batch_no) - 4 ELSE CHAR_LENGTH(ip.slice_batch_no) - 3 END)
                    WHEN ip.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(ip.production_batch_no, CASE WHEN ip.production_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(ip.production_batch_no) - 4 ELSE CHAR_LENGTH(ip.production_batch_no) - 3 END)
                    ELSE COALESCE(NULLIF(ip.production_batch_no, ''), NULLIF(ip.slice_batch_no, ''))
                  END AS segment_batch_no,
                  COUNT(DISTINCT ip.id) AS inner_piece_count,
                  COUNT(DISTINCT CASE WHEN obi.id IS NULL THEN NULL ELSE ip.id END) AS outbound_piece_count,
                  GREATEST(COUNT(DISTINCT ip.id) - COUNT(DISTINCT CASE WHEN obi.id IS NULL THEN NULL ELSE ip.id END), 0) AS wait_outer_piece_count,
                  MAX(COALESCE(obi.scan_time, ip.scan_time, ip.update_time, ip.create_time)) AS last_report_time
                FROM mes_sfc_inner_pack_unit_item ip
                LEFT JOIN mes_sfc_outer_pack_box_item obi
                  ON obi.deleted = 0
                 AND (obi.inner_unit_no = ip.inner_unit_no OR obi.inner_unit_id = ip.inner_unit_id)
                WHERE ip.deleted = 0
                  AND ip.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY ip.plan_id,
                  CASE
                    WHEN ip.slice_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(ip.slice_batch_no, CASE WHEN ip.slice_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(ip.slice_batch_no) - 4 ELSE CHAR_LENGTH(ip.slice_batch_no) - 3 END)
                    WHEN ip.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]' THEN LEFT(ip.production_batch_no, CASE WHEN ip.production_batch_no REGEXP '[A-Z]$' THEN CHAR_LENGTH(ip.production_batch_no) - 4 ELSE CHAR_LENGTH(ip.production_batch_no) - 3 END)
                    ELSE COALESCE(NULLIF(ip.production_batch_no, ''), NULLIF(ip.slice_batch_no, ''))
                  END
              ) outb
            ) t
            WHERE t.report_date BETWEEN #{startDate} AND #{endDate}
              <if test="operationName != null and operationName != ''">
                AND (t.operation_name = #{operationName}
                  OR (#{operationName} = '磨皮' AND t.operation_name IN ('磨皮1', '磨皮2'))
                  OR (#{operationName} = '包装' AND t.operation_name IN ('包装入库', '包装出库')))
              </if>
            GROUP BY plan_id, plan_operation_id, report_date, operation_name, fact_source, segment_batch_no
            ORDER BY plan_id ASC, report_date ASC, operation_name ASC
            </script>
            """)
    List<FactRow> selectFactRows(@Param("planIds") Collection<Long> planIds,
                                 @Param("startDate") LocalDate startDate,
                                 @Param("endDate") LocalDate endDate,
                                 @Param("operationName") String operationName);

    @Select("""
            <script>
            SELECT
              lo.plan_id AS planId,
              lo.operation_name AS operationName,
              COALESCE(SUM(f.report_qty), 0) AS reportQty,
              CASE
                WHEN lo.operation_name LIKE '%湿法%' OR lo.operation_name LIKE '%磨皮%' OR lo.operation_name LIKE '%粗磨%' THEN 'm'
                ELSE '片'
              END AS unit
            FROM (
              SELECT o.plan_id, o.id AS plan_operation_id, o.op_name AS operation_name
              FROM mes_pp_plan_operation o
              INNER JOIN (
                SELECT plan_id, MAX(COALESCE(op_seq, 0)) AS max_op_seq
                FROM mes_pp_plan_operation
                WHERE deleted = 0
                  AND plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                GROUP BY plan_id
              ) last_seq ON last_seq.plan_id = o.plan_id AND last_seq.max_op_seq = COALESCE(o.op_seq, 0)
              WHERE o.deleted = 0
                AND o.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
            ) lo
            LEFT JOIN (
              SELECT r.plan_id, r.plan_operation_id, r.operation_name,
                     CASE
                       WHEN r.operation_name = '配料' OR r.source_menu_code = 'FORMULA_REPORT'
                         THEN COALESCE(r.feed_qty, r.good_qty, 0)
                       WHEN r.operation_name LIKE '%湿法%' OR r.operation_name LIKE '%磨皮%' OR r.operation_name LIKE '%粗磨%'
                         THEN COALESCE(r.good_qty, 0)
                       ELSE 1
                     END AS report_qty
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0 AND r.report_type = 'END'
                AND r.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT a.plan_id, a.plan_operation_id, a.operation_name, 1
              FROM mes_sfc_adhesive_report a
              WHERE a.deleted = 0
                AND UPPER(COALESCE(a.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND a.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT s.plan_id, s.plan_operation_id, s.operation_name, 1
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0 AND s.scan_status = 'CONFIRMED'
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT ps.plan_id, ps.plan_operation_id, ps.operation_name, 1
              FROM mes_sfc_press_slot_report ps
              WHERE ps.deleted = 0 AND ps.report_status = 'CONFIRMED'
                AND ps.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT a2.plan_id, a2.plan_operation_id, a2.operation_name, 1
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT cr.plan_id, cr.plan_operation_id, cr.operation_name, 1
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0 AND cr.report_status = 'CONFIRMED'
                AND cr.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
            ) f ON f.plan_id = lo.plan_id
              AND (f.plan_operation_id = lo.plan_operation_id OR f.operation_name = lo.operation_name)
            GROUP BY lo.plan_id, lo.operation_name
            </script>
            """)
    List<LastOperationReportRow> selectLastOperationReportRows(@Param("planIds") Collection<Long> planIds);

    @Select("""
            <script>
            SELECT
              p.id AS planId,
              COALESCE(wet.wet_roll_qty, 0) AS wetRollQty,
              COALESCE(grinding.second_grinding_qty, 0) AS secondGrindingQty,
              COALESCE(slitting.slitting_confirmed_qty, 0) AS slittingConfirmedQty,
              COALESCE(adhesive2.adhesive2_qty, 0) AS adhesive2Qty,
              COALESCE(changeover.changeover_count, 0) AS adhesive2ChangeoverCount,
              COALESCE(picked.picked_qty, 0) AS pickedQty
            FROM mes_pp_plan_order p
            LEFT JOIN (
              SELECT r.plan_id, SUM(COALESCE(r.good_qty, 0)) AS wet_roll_qty
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0
                AND r.report_type = 'END'
                AND (r.operation_name = '湿法' OR r.source_menu_code = 'WET_REPORT')
                AND r.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY r.plan_id
            ) wet ON wet.plan_id = p.id
            LEFT JOIN (
              SELECT d.plan_id, SUM(COALESCE(d.output_length, 0)) AS second_grinding_qty
              FROM mes_sfc_grinding_second_detail d
              WHERE d.deleted = 0
                AND d.confirm_status = 'CONFIRMED'
                AND d.detail_status = 'CONFIRMED'
                AND d.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY d.plan_id
            ) grinding ON grinding.plan_id = p.id
            LEFT JOIN (
              SELECT s.plan_id, COUNT(1) AS slitting_confirmed_qty
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.scan_status = 'CONFIRMED'
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY s.plan_id
            ) slitting ON slitting.plan_id = p.id
            LEFT JOIN (
              SELECT a2.plan_id, COUNT(1) AS adhesive2_qty
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY a2.plan_id
            ) adhesive2 ON adhesive2.plan_id = p.id
            LEFT JOIN (
              SELECT c.plan_id, COUNT(1) AS changeover_count
              FROM mes_sfc_press_slot_changeover_inspection c
              WHERE c.deleted = 0
                AND (c.operation_name = '粘胶2' OR c.operation_code IN ('WC-ADH2', 'OP-ADHESIVE2'))
                AND c.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY c.plan_id
            ) changeover ON changeover.plan_id = p.id
            LEFT JOIN (
              SELECT
                p2.id AS plan_id,
                SUM(COALESCE(pi.actual_ship_qty, pi.stock_qty, 0)) AS picked_qty
              FROM mes_pp_plan_order p2
              INNER JOIN mes_inv_fg_shipping_pick_item pi ON pi.deleted = 0
                AND COALESCE(NULLIF(pi.actual_slice_batch_no, ''), NULLIF(pi.slice_batch_no, ''))
                  LIKE CONCAT(COALESCE(NULLIF(p2.production_batch_no, ''), NULLIF(p2.batch_no, '')), 'P%')
              WHERE p2.deleted = 0
                AND COALESCE(NULLIF(p2.production_batch_no, ''), NULLIF(p2.batch_no, '')) IS NOT NULL
                AND p2.id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY p2.id
            ) picked ON picked.plan_id = p.id
            WHERE p.deleted = 0
              AND p.id IN
              <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
              </foreach>
            </script>
            """)
    List<ProgressQuantityRow> selectProgressQuantityRows(@Param("planIds") Collection<Long> planIds);

    @Select("""
            <script>
            SELECT
              a2.plan_id AS planId,
              TRIM(COALESCE(NULLIF(p.model_code, ''), p.model_name, '')) AS planValue,
              TRIM(a2.model_code) AS actualValue,
              COUNT(1) AS qty
            FROM mes_sfc_adhesive2_report a2
            INNER JOIN mes_pp_plan_order p ON p.id = a2.plan_id AND p.deleted = 0
            WHERE a2.deleted = 0
              AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
              AND a2.plan_id IN
              <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
              </foreach>
              AND NULLIF(TRIM(COALESCE(a2.model_code, '')), '') IS NOT NULL
              AND TRIM(a2.model_code) != TRIM(COALESCE(NULLIF(p.model_code, ''), p.model_name, ''))
            GROUP BY a2.plan_id, TRIM(COALESCE(NULLIF(p.model_code, ''), p.model_name, '')), TRIM(a2.model_code)
            ORDER BY a2.plan_id ASC, COUNT(1) DESC, TRIM(a2.model_code) ASC
            </script>
            """)
    List<ModelChangeoverSummaryRow> selectAdhesive2ModelChangeoverSummaryRows(
            @Param("planIds") Collection<Long> planIds);

    @Select("""
            <script>
            SELECT
              cr.plan_id AS planId,
              cr.id AS reportId,
              TRIM(COALESCE(NULLIF(p.size_spec, ''), p.size_name, '')) AS planValue,
              cr.production_batch_no AS productionBatchNo,
              cr.extra_json AS extraJson
            FROM mes_sfc_cut_round_report cr
            INNER JOIN mes_pp_plan_order p ON p.id = cr.plan_id AND p.deleted = 0
            WHERE cr.deleted = 0
              AND UPPER(COALESCE(cr.report_status, '')) = 'CONFIRMED'
              AND cr.plan_id IN
              <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                #{planId}
              </foreach>
            ORDER BY cr.plan_id ASC, cr.id ASC
            </script>
            """)
    List<CutRoundSizeSourceRow> selectCutRoundSizeSourceRows(@Param("planIds") Collection<Long> planIds);

    @Select("""
            SELECT
              a2.plan_id AS planId,
              a2.id AS sourceReportId,
              COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS sourceOperation,
              TRIM(COALESCE(NULLIF(p.model_code, ''), p.model_name, '')) AS planValue,
              TRIM(a2.model_code) AS actualValue,
              NULL AS actualSuffix,
              a2.production_batch_no AS productionBatchNo,
              a2.glue_board_model AS glueBoardModel,
              COALESCE(NULLIF(a2.confirmer_name, ''), NULLIF(a2.recorder_name, '')) AS reporterName,
              COALESCE(a2.confirmer_time, a2.end_time, a2.recorder_time, a2.update_time, a2.create_time) AS reportTime,
              a2.report_status AS reportStatus,
              a2.extra_json AS extraJson
            FROM mes_sfc_adhesive2_report a2
            INNER JOIN mes_pp_plan_order p ON p.id = a2.plan_id AND p.deleted = 0
            WHERE a2.deleted = 0
              AND UPPER(COALESCE(a2.report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
              AND a2.plan_id = #{planId}
              AND NULLIF(TRIM(COALESCE(a2.model_code, '')), '') IS NOT NULL
              AND TRIM(a2.model_code) != TRIM(COALESCE(NULLIF(p.model_code, ''), p.model_name, ''))
            ORDER BY COALESCE(a2.confirmer_time, a2.end_time, a2.recorder_time, a2.update_time, a2.create_time) DESC,
              a2.id DESC
            """)
    List<ChangeoverDetailRow> selectAdhesive2ModelChangeoverDetailRows(@Param("planId") Long planId);

    @Select("""
            SELECT
              cr.plan_id AS planId,
              cr.id AS sourceReportId,
              COALESCE(NULLIF(cr.operation_name, ''), '裁切') AS sourceOperation,
              TRIM(COALESCE(NULLIF(p.size_spec, ''), p.size_name, '')) AS planValue,
              NULL AS actualValue,
              NULL AS actualSuffix,
              cr.production_batch_no AS productionBatchNo,
              NULL AS glueBoardModel,
              COALESCE(NULLIF(cr.confirmer_name, ''), NULLIF(cr.recorder_name, '')) AS reporterName,
              COALESCE(cr.confirmer_time, cr.end_time, cr.inspection_time, cr.recorder_time, cr.update_time, cr.create_time) AS reportTime,
              cr.report_status AS reportStatus,
              cr.extra_json AS extraJson
            FROM mes_sfc_cut_round_report cr
            INNER JOIN mes_pp_plan_order p ON p.id = cr.plan_id AND p.deleted = 0
            WHERE cr.deleted = 0
              AND UPPER(COALESCE(cr.report_status, '')) = 'CONFIRMED'
              AND cr.plan_id = #{planId}
            ORDER BY COALESCE(cr.confirmer_time, cr.end_time, cr.inspection_time, cr.recorder_time, cr.update_time,
              cr.create_time) DESC, cr.id DESC
            """)
    List<ChangeoverDetailRow> selectCutRoundSizeChangeoverDetailRows(@Param("planId") Long planId);

    @Select("""
            SELECT operation_name AS operationName, SUM(ng_qty) AS ngQty
            FROM (
              SELECT r.operation_name, COUNT(1) AS ng_qty
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0
                AND DATE(COALESCE(r.end_time, r.confirmer_time, r.recorder_time, r.update_time, r.create_time, r.report_date))
                    BETWEEN #{startDate} AND #{endDate}
                AND EXISTS (SELECT 1 FROM mes_sfc_operation_report_defect d WHERE d.deleted = 0 AND d.report_id = r.id)
              GROUP BY r.operation_name
              UNION ALL
              SELECT operation_name, COUNT(1) FROM mes_sfc_adhesive_report
              WHERE deleted = 0 AND UPPER(COALESCE(report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND DATE(COALESCE(end_time, confirmer_time, recorder_time, update_time, create_time, report_date)) BETWEEN #{startDate} AND #{endDate}
                AND ((self_check IS NOT NULL AND self_check != 'OK') OR NULLIF(defect_code, '') IS NOT NULL
                  OR product_quality_status != 'NORMAL')
              GROUP BY operation_name
              UNION ALL
              SELECT operation_name, COUNT(1) FROM mes_sfc_slitting_slice_record
              WHERE deleted = 0 AND scan_status = 'CONFIRMED'
                AND DATE(COALESCE(scan_time, update_time, create_time)) BETWEEN #{startDate} AND #{endDate}
                AND self_check IS NOT NULL AND self_check != 'OK'
              GROUP BY operation_name
              UNION ALL
              SELECT operation_name, COUNT(1) FROM mes_sfc_press_slot_report
              WHERE deleted = 0 AND report_status = 'CONFIRMED'
                AND DATE(COALESCE(end_time, confirmer_time, recorder_time, update_time, create_time, report_date)) BETWEEN #{startDate} AND #{endDate}
                AND ((self_check IS NOT NULL AND self_check != 'OK') OR NULLIF(defect_code, '') IS NOT NULL)
              GROUP BY operation_name
              UNION ALL
              SELECT operation_name, COUNT(1) FROM mes_sfc_adhesive2_report
              WHERE deleted = 0 AND UPPER(COALESCE(report_status, '')) IN ('CONFIRMED', 'SUBMITTED', 'COMPLETED')
                AND DATE(COALESCE(end_time, confirmer_time, recorder_time, update_time, create_time, report_date)) BETWEEN #{startDate} AND #{endDate}
                AND ((self_check IS NOT NULL AND self_check != 'OK') OR NULLIF(defect_code, '') IS NOT NULL
                  OR product_quality_status != 'NORMAL')
              GROUP BY operation_name
              UNION ALL
              SELECT operation_name, COUNT(1) FROM mes_sfc_cut_round_report
              WHERE deleted = 0 AND report_status = 'CONFIRMED'
                AND DATE(COALESCE(end_time, inspection_time, recorder_time, update_time, create_time, report_date)) BETWEEN #{startDate} AND #{endDate}
                AND ((self_check IS NOT NULL AND self_check != 'OK') OR NULLIF(defect_code, '') IS NOT NULL)
              GROUP BY operation_name
            ) t
            GROUP BY operation_name
            ORDER BY ngQty DESC, operationName ASC
            """)
    List<NgSummaryRow> selectNgSummaryRows(@Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate);

}
