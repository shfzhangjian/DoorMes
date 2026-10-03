package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldAnalysisSourceRow;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldAnalysisSourcePreviewRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QmsYieldAnalysisMapper {

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    CASE r.source_menu_code
                      WHEN 'FORMULA_REPORT' THEN 'FORMULA'
                      WHEN 'WET_REPORT' THEN 'WET'
                      ELSE r.source_menu_code
                    END AS process_code,
                    CASE r.source_menu_code
                      WHEN 'FORMULA_REPORT' THEN COALESCE(NULLIF(r.operation_name, ''), '配料')
                      WHEN 'WET_REPORT' THEN COALESCE(NULLIF(r.operation_name, ''), '湿法')
                      ELSE COALESCE(NULLIF(r.operation_name, ''), r.source_menu_code)
                    END AS process_name,
                    'mes_sfc_operation_report' AS source_table,
                    r.id AS source_id,
                    r.plan_id,
                    COALESCE(NULLIF(r.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    r.plan_operation_id,
                    COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                             NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, '')) AS mother_roll_no,
                    COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                             NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, '')) AS segment_no,
                    COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''), CONCAT('REPORT-', r.id)) AS piece_no,
                    COALESCE(NULLIF(r.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(r.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(r.mother_model_code, ''), NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, ''),
                             NULLIF(r.mother_model_name, '')) AS model_code,
                    COALESCE(NULLIF(r.feed_qty, 0), r.good_qty + r.scrap_qty, r.good_qty, 1) AS input_qty,
                    COALESCE(r.good_qty, 0) AS output_good_qty,
                    COALESCE(r.scrap_qty, 0) AS output_ng_qty,
                    NULL AS self_check,
                    NULL AS defect_code,
                    NULL AS visual_result_json,
                    r.extra_json,
                    COALESCE(NULLIF(r.fai_judgment, ''), NULLIF(r.fai_status, '')) AS submission_result,
                    r.operation_status AS report_status,
                    COALESCE(r.confirmer_time, r.end_time, r.recorder_time, r.update_time, r.create_time) AS confirm_time,
                    r.fai_id AS inspection_id,
                    r.fai_no AS inspection_no,
                    'FAI' AS inspection_source_type
                FROM mes_sfc_operation_report r
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = r.plan_id
                WHERE r.deleted = b'0'
                  AND r.report_type = 'END'
                  AND r.source_menu_code IN ('FORMULA_REPORT', 'WET_REPORT')
                  <if test="tenantId != null">
                  AND r.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'ROUGH_GRINDING' AS process_code,
                    '磨皮' AS process_name,
                    'mes_sfc_grinding_second_detail' AS source_table,
                    s.id AS source_id,
                    s.plan_id,
                    COALESCE(NULLIF(s.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    s.plan_operation_id,
                    COALESCE(NULLIF(s.parent_production_batch_no, ''), NULLIF(s.mother_batch_no, ''),
                             NULLIF(s.source_production_batch_no, ''), NULLIF(po.parent_production_batch_no, ''),
                             NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, '')) AS mother_roll_no,
                    COALESCE(NULLIF(s.production_batch_no, ''), NULLIF(s.confirmed_batch_no, ''),
                             NULLIF(s.source_production_batch_no, ''), NULLIF(s.mother_batch_no, '')) AS segment_no,
                    COALESCE(NULLIF(s.production_batch_no, ''), NULLIF(s.confirmed_batch_no, ''), CONCAT('GRIND2-', s.id)) AS piece_no,
                    COALESCE(NULLIF(po.material_code, ''), NULLIF(po.mother_material_code, '')) AS material_code,
                    COALESCE(NULLIF(po.material_name, ''), NULLIF(po.mother_material_name, '')) AS material_name,
                    COALESCE(NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                    COALESCE(NULLIF(s.process_length, 0), s.output_length + s.loss_length + s.nap_sample_length,
                             s.output_length, 1) AS input_qty,
                    COALESCE(s.output_length, 0) AS output_good_qty,
                    COALESCE(s.loss_length, 0) AS output_ng_qty,
                    s.self_check,
                    s.defect_code,
                    NULL AS visual_result_json,
                    NULL AS extra_json,
                    COALESCE(NULLIF(s.inspection_result, ''), NULLIF(s.inspection_status, '')) AS submission_result,
                    COALESCE(NULLIF(s.detail_status, ''), NULLIF(s.confirm_status, ''), NULLIF(s.row_status, '')) AS report_status,
                    COALESCE(s.confirm_time, s.end_time, s.update_time, s.create_time) AS confirm_time,
                    s.inspection_id,
                    s.inspection_no,
                    'FAI' AS inspection_source_type
                FROM mes_sfc_grinding_second_detail s
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = s.plan_id
                WHERE s.deleted = b'0'
                  AND COALESCE(s.detail_status, s.confirm_status, 'SUBMITTED') != 'VOID'
                  <if test="tenantId != null">
                  AND s.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'ADHESIVE1' AS process_code,
                    COALESCE(NULLIF(a.operation_name, ''), '粘胶1') AS process_name,
                    'mes_sfc_adhesive_report' AS source_table,
                    a.id AS source_id,
                    a.plan_id,
                    COALESCE(NULLIF(a.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    a.plan_operation_id,
                    COALESCE(NULLIF(a.parent_production_batch_no, ''), NULLIF(a.source_production_batch_no, ''),
                             NULLIF(a.source_batch_no, ''), NULLIF(po.parent_production_batch_no, ''),
                             NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, '')) AS mother_roll_no,
                    CASE
                      WHEN a.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a.production_batch_no, '-J', 1)
                      ELSE COALESCE(NULLIF(a.source_production_batch_no, ''), NULLIF(a.production_batch_no, ''),
                                    NULLIF(a.parent_production_batch_no, ''), NULLIF(a.source_batch_no, ''))
                    END AS segment_no,
                    COALESCE(NULLIF(a.production_batch_no, ''), CONCAT('ADH1-', a.id)) AS piece_no,
                    COALESCE(NULLIF(a.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(a.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(a.model_code, ''), NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                    COALESCE(NULLIF(a.input_length, 0), a.output_length + a.loss_length + a.nap_sample_length,
                             a.output_length, 1) AS input_qty,
                    COALESCE(a.output_length, 0) AS output_good_qty,
                    COALESCE(a.loss_length, 0) AS output_ng_qty,
                    a.self_check,
                    a.defect_code,
                    NULL AS visual_result_json,
                    a.extra_json,
                    COALESCE(NULLIF(a.fai_judgment, ''), NULLIF(a.fai_status, '')) AS submission_result,
                    a.report_status,
                    COALESCE(a.confirmer_time, a.end_time, a.recorder_time, a.update_time, a.create_time) AS confirm_time,
                    a.fai_id AS inspection_id,
                    a.fai_no AS inspection_no,
                    'FAI' AS inspection_source_type
                FROM mes_sfc_adhesive_report a
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = a.plan_id
                WHERE a.deleted = b'0'
                  AND COALESCE(a.report_status, 'CONFIRMED') != 'DRAFT'
                  <if test="tenantId != null">
                  AND a.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'SLITTING' AS process_code,
                    COALESCE(NULLIF(s.operation_name, ''), '分切') AS process_name,
                    'mes_sfc_slitting_slice_record' AS source_table,
                    s.id AS source_id,
                    s.plan_id,
                    COALESCE(NULLIF(s.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    s.plan_operation_id,
                    COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, '')) AS mother_roll_no,
                    CASE
                      WHEN s.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_batch_no
                      WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                      WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(s.source_production_batch_no, '-J', 1)
                      WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.source_production_batch_no, CHAR_LENGTH(s.source_production_batch_no) - 3)
                      WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.source_production_batch_no, CHAR_LENGTH(s.source_production_batch_no) - 4)
                      WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 3)
                      WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 4)
                      ELSE COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, ''), NULLIF(s.slice_serial_no, ''))
                    END AS segment_no,
                    COALESCE(NULLIF(s.slice_serial_no, ''), CONCAT('SLIT-', s.id)) AS piece_no,
                    po.material_code AS material_code,
                    po.material_name AS material_name,
                    COALESCE(NULLIF(a.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                    1 AS input_qty,
                    NULL AS output_good_qty,
                    NULL AS output_ng_qty,
                    s.self_check,
                    NULL AS defect_code,
                    s.visual_result_json,
                    NULL AS extra_json,
                    NULL AS submission_result,
                    s.scan_status AS report_status,
                    s.scan_time AS confirm_time,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    NULL AS inspection_source_type
                FROM mes_sfc_slitting_slice_record s
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = s.plan_id
                LEFT JOIN mes_sfc_adhesive_report a ON a.deleted = b'0' AND a.id = s.source_adhesive_report_id
                    AND a.tenant_id = s.tenant_id
                WHERE s.deleted = b'0'
                  AND s.scan_status = 'CONFIRMED'
                  <if test="tenantId != null">
                  AND s.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'PRESS_SLOT' AS process_code,
                    COALESCE(NULLIF(p.operation_name, ''), '压槽') AS process_name,
                    'mes_sfc_press_slot_report' AS source_table,
                    p.id AS source_id,
                    p.plan_id,
                    COALESCE(NULLIF(p.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    p.plan_operation_id,
                    COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''),
                             NULLIF(p.parent_production_batch_no, '')) AS mother_roll_no,
                    CASE
                      WHEN p.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_batch_no
                      WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_production_batch_no
                      WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(p.source_production_batch_no, '-J', 1)
                      WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(p.source_production_batch_no, CHAR_LENGTH(p.source_production_batch_no) - 3)
                      WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(p.source_production_batch_no, CHAR_LENGTH(p.source_production_batch_no) - 4)
                      WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 3)
                      WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 4)
                      WHEN p.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.parent_production_batch_no
                      ELSE COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''))
                    END AS segment_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), CONCAT('PRESS-', p.id)) AS piece_no,
                    COALESCE(NULLIF(p.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(p.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(p.model_code, ''), NULLIF(a.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                    1 AS input_qty,
                    NULL AS output_good_qty,
                    NULL AS output_ng_qty,
                    p.self_check,
                    p.defect_code,
                    NULL AS visual_result_json,
                    p.extra_json,
                    NULL AS submission_result,
                    p.report_status,
                    p.confirmer_time AS confirm_time,
                    (
                        SELECT fai.id
                        FROM mes_qms_fai_order fai
                        WHERE fai.deleted = 0
                          AND fai.source_module = 'PRESS_SLOT_REPORT'
                          AND fai.source_report_id = p.id
                          AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                          AND fai.tenant_id = p.tenant_id
                        ORDER BY fai.id DESC
                        LIMIT 1
                    ) AS inspection_id,
                    (
                        SELECT fai.fai_no
                        FROM mes_qms_fai_order fai
                        WHERE fai.deleted = 0
                          AND fai.source_module = 'PRESS_SLOT_REPORT'
                          AND fai.source_report_id = p.id
                          AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                          AND fai.tenant_id = p.tenant_id
                        ORDER BY fai.id DESC
                        LIMIT 1
                    ) AS inspection_no,
                    'FAI' AS inspection_source_type
                FROM mes_sfc_press_slot_report p
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = p.plan_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = p.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive_report a ON a.deleted = b'0' AND a.id = sl.source_adhesive_report_id
                    AND a.tenant_id = p.tenant_id
                WHERE p.deleted = b'0'
                  <if test="tenantId != null">
                  AND p.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'ADHESIVE2' AS process_code,
                    COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS process_name,
                    'mes_sfc_adhesive2_report' AS source_table,
                    a2.id AS source_id,
                    a2.plan_id,
                    COALESCE(NULLIF(a2.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    a2.plan_operation_id,
                    COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''),
                             NULLIF(a2.parent_production_batch_no, '')) AS mother_roll_no,
                    CASE
                      WHEN a2.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_batch_no
                      WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_production_batch_no
                      WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a2.source_production_batch_no, '-J', 1)
                      WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(a2.source_production_batch_no, CHAR_LENGTH(a2.source_production_batch_no) - 3)
                      WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(a2.source_production_batch_no, CHAR_LENGTH(a2.source_production_batch_no) - 4)
                      WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 3)
                      WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 4)
                      WHEN a2.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.parent_production_batch_no
                      ELSE COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''))
                    END AS segment_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), CONCAT('ADH2-', a2.id)) AS piece_no,
                    COALESCE(NULLIF(a2.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(a2.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''), NULLIF(sla.model_code, ''),
                             NULLIF(psla.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                    1 AS input_qty,
                    NULL AS output_good_qty,
                    NULL AS output_ng_qty,
                    a2.self_check,
                    a2.defect_code,
                    NULL AS visual_result_json,
                    a2.extra_json,
                    NULL AS submission_result,
                    a2.report_status,
                    a2.confirmer_time AS confirm_time,
                    (
                        SELECT fai.id
                        FROM mes_qms_fai_order fai
                        WHERE fai.deleted = 0
                          AND fai.source_module = 'ADHESIVE2_REPORT'
                          AND fai.source_report_id = a2.id
                          AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                          AND fai.tenant_id = a2.tenant_id
                        ORDER BY fai.id DESC
                        LIMIT 1
                    ) AS inspection_id,
                    (
                        SELECT fai.fai_no
                        FROM mes_qms_fai_order fai
                        WHERE fai.deleted = 0
                          AND fai.source_module = 'ADHESIVE2_REPORT'
                          AND fai.source_report_id = a2.id
                          AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                          AND fai.tenant_id = a2.tenant_id
                        ORDER BY fai.id DESC
                        LIMIT 1
                    ) AS inspection_no,
                    'FAI' AS inspection_source_type
                FROM mes_sfc_adhesive2_report a2
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = a2.plan_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = a2.source_slitting_slice_id
                LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = a2.source_press_slot_report_id
                LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive_report sla ON sla.deleted = b'0' AND sla.id = sl.source_adhesive_report_id
                    AND sla.tenant_id = a2.tenant_id
                LEFT JOIN mes_sfc_adhesive_report psla ON psla.deleted = b'0' AND psla.id = psl.source_adhesive_report_id
                    AND psla.tenant_id = a2.tenant_id
                WHERE a2.deleted = b'0'
                  <if test="tenantId != null">
                  AND a2.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'CUT_ROUND' AS process_code,
                    COALESCE(NULLIF(c.operation_name, ''), '裁切') AS process_name,
                    'mes_sfc_cut_round_report' AS source_table,
                    c.id AS source_id,
                    c.plan_id,
                    COALESCE(NULLIF(c.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    c.plan_operation_id,
                    COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''),
                             NULLIF(c.parent_production_batch_no, '')) AS mother_roll_no,
                    CASE
                      WHEN c.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_batch_no
                      WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_production_batch_no
                      WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(c.source_production_batch_no, '-J', 1)
                      WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(c.source_production_batch_no, CHAR_LENGTH(c.source_production_batch_no) - 3)
                      WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(c.source_production_batch_no, CHAR_LENGTH(c.source_production_batch_no) - 4)
                      WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 3)
                      WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 4)
                      WHEN c.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.parent_production_batch_no
                      ELSE COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''))
                    END AS segment_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), CONCAT('CUT-', c.id)) AS piece_no,
                    COALESCE(NULLIF(c.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(c.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(c.model_code, ''), NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''),
                             NULLIF(sla.model_code, ''), NULLIF(psla.model_code, ''), NULLIF(a2sla.model_code, ''),
                             NULLIF(po.model_code, '')) AS model_code,
                    1 AS input_qty,
                    NULL AS output_good_qty,
                    NULL AS output_ng_qty,
                    c.self_check,
                    c.defect_code,
                    NULL AS visual_result_json,
                    c.extra_json,
                    c.inspection_result AS submission_result,
                    c.report_status,
                    c.confirmer_time AS confirm_time,
                    COALESCE(
                        (
                            SELECT d.fqc_order_id
                            FROM mes_sfc_cut_round_inspection_detail d
                            WHERE d.deleted = 0
                              AND d.cut_round_report_id = c.id
                              AND d.fqc_order_id IS NOT NULL
                              AND d.tenant_id = c.tenant_id
                            ORDER BY d.id DESC
                            LIMIT 1
                        ),
                        (
                            SELECT sd.fqc_id
                            FROM mes_qms_fqc_submission_detail sd
                            WHERE sd.deleted = 0
                              AND sd.cut_round_report_id = c.id
                              AND sd.fqc_id IS NOT NULL
                              AND sd.tenant_id = c.tenant_id
                            ORDER BY sd.id DESC
                            LIMIT 1
                        ),
                        (
                            SELECT fqc.id
                            FROM mes_qms_fqc_order fqc
                            WHERE fqc.deleted = 0
                              AND fqc.source_module IN ('CUT_ROUND', 'CUT_ROUND_REPORT', 'CUT_ROUND_FQC')
                              AND fqc.tenant_id = c.tenant_id
                              AND (
                                fqc.source_report_id = c.id
                                OR fqc.source_report_id = c.inspection_task_id
                                OR (
                                  NULLIF(TRIM(c.inspection_task_no), '') IS NOT NULL
                                  AND UPPER(TRIM(COALESCE(fqc.source_report_no, ''))) = UPPER(TRIM(c.inspection_task_no))
                                )
                              )
                            ORDER BY fqc.id DESC
                            LIMIT 1
                        )
                    ) AS inspection_id,
                    COALESCE(
                        (
                            SELECT d.fqc_no
                            FROM mes_sfc_cut_round_inspection_detail d
                            WHERE d.deleted = 0
                              AND d.cut_round_report_id = c.id
                              AND NULLIF(d.fqc_no, '') IS NOT NULL
                              AND d.tenant_id = c.tenant_id
                            ORDER BY d.id DESC
                            LIMIT 1
                        ),
                        (
                            SELECT sd.fqc_no
                            FROM mes_qms_fqc_submission_detail sd
                            WHERE sd.deleted = 0
                              AND sd.cut_round_report_id = c.id
                              AND NULLIF(sd.fqc_no, '') IS NOT NULL
                              AND sd.tenant_id = c.tenant_id
                            ORDER BY sd.id DESC
                            LIMIT 1
                        ),
                        (
                            SELECT fqc.fqc_no
                            FROM mes_qms_fqc_order fqc
                            WHERE fqc.deleted = 0
                              AND fqc.source_module IN ('CUT_ROUND', 'CUT_ROUND_REPORT', 'CUT_ROUND_FQC')
                              AND fqc.tenant_id = c.tenant_id
                              AND (
                                fqc.source_report_id = c.id
                                OR fqc.source_report_id = c.inspection_task_id
                                OR (
                                  NULLIF(TRIM(c.inspection_task_no), '') IS NOT NULL
                                  AND UPPER(TRIM(COALESCE(fqc.source_report_no, ''))) = UPPER(TRIM(c.inspection_task_no))
                                )
                              )
                            ORDER BY fqc.id DESC
                            LIMIT 1
                        )
                    ) AS inspection_no,
                    'CUT_ROUND_FQC' AS inspection_source_type
                FROM mes_sfc_cut_round_report c
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = c.plan_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = c.source_slitting_slice_id
                LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = c.source_press_slot_report_id
                LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = b'0' AND a2.id = c.source_adhesive2_report_id
                LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = b'0' AND a2sl.id = a2.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive_report sla ON sla.deleted = b'0' AND sla.id = sl.source_adhesive_report_id
                    AND sla.tenant_id = c.tenant_id
                LEFT JOIN mes_sfc_adhesive_report psla ON psla.deleted = b'0' AND psla.id = psl.source_adhesive_report_id
                    AND psla.tenant_id = c.tenant_id
                LEFT JOIN mes_sfc_adhesive_report a2sla ON a2sla.deleted = b'0' AND a2sla.id = a2sl.source_adhesive_report_id
                    AND a2sla.tenant_id = c.tenant_id
                WHERE c.deleted = b'0'
                  <if test="tenantId != null">
                  AND c.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'FINAL_INSPECTION' AS process_code,
                    '终检' AS process_name,
                    'mes_qms_fqc_submission_detail' AS source_table,
                    sd.id AS source_id,
                    COALESCE(sd.plan_id, fo.plan_order_id, c.plan_id) AS plan_id,
                    COALESCE(NULLIF(sd.plan_no, ''), NULLIF(po.plan_no, ''), NULLIF(c.plan_no, '')) AS plan_no,
                    COALESCE(sd.plan_operation_id, c.plan_operation_id) AS plan_operation_id,
                    COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                             NULLIF(po.batch_no, ''), NULLIF(sd.parent_production_batch_no, ''),
                             NULLIF(c.parent_production_batch_no, '')) AS mother_roll_no,
                    CASE
                      WHEN sd.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN sd.parent_production_batch_no
                      WHEN c.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_batch_no
                      WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_production_batch_no
                      WHEN sd.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(sd.production_batch_no, CHAR_LENGTH(sd.production_batch_no) - 3)
                      WHEN sd.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(sd.production_batch_no, CHAR_LENGTH(sd.production_batch_no) - 4)
                      ELSE COALESCE(NULLIF(sd.parent_production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''),
                                    NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, ''))
                    END AS segment_no,
                    COALESCE(NULLIF(sd.production_batch_no, ''), NULLIF(c.production_batch_no, ''), CONCAT('FQC-', sd.id)) AS piece_no,
                    COALESCE(NULLIF(sd.material_code, ''), NULLIF(fo.material_code, ''), NULLIF(c.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(sd.material_name, ''), NULLIF(fo.material_name, ''), NULLIF(c.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(sd.model_code, ''), NULLIF(fo.product_model, ''), NULLIF(c.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                    1 AS input_qty,
                    CASE WHEN UPPER(COALESCE(sd.row_judgment, '')) = 'OK' THEN 1 ELSE 0 END AS output_good_qty,
                    CASE WHEN UPPER(COALESCE(sd.row_judgment, '')) = 'NG' THEN 1 ELSE 0 END AS output_ng_qty,
                    COALESCE(NULLIF(sd.row_judgment, ''), 'PENDING') AS self_check,
                    COALESCE(NULLIF(sd.defect_name, ''), NULLIF(sd.defect_code, '')) AS defect_code,
                    COALESCE(
                        (
                            SELECT JSON_ARRAYAGG(JSON_OBJECT('defectName', d.defect_name, 'result', 'NG'))
                            FROM mes_qms_fqc_sample_defect d
                            INNER JOIN mes_qms_fqc_sample s ON s.deleted = b'0' AND s.id = d.sample_id
                            WHERE d.deleted = b'0'
                              AND s.submission_detail_id = sd.id
                              AND d.tenant_id = sd.tenant_id
                              AND NULLIF(d.defect_name, '') IS NOT NULL
                        ),
                        (
                            SELECT JSON_ARRAYAGG(JSON_OBJECT('defectName', s.defect_name, 'result', 'NG'))
                            FROM mes_qms_fqc_sample s
                            WHERE s.deleted = b'0'
                              AND s.submission_detail_id = sd.id
                              AND s.tenant_id = sd.tenant_id
                              AND NULLIF(s.defect_name, '') IS NOT NULL
                        )
                    ) AS visual_result_json,
                    sd.quality_risk_snapshot_json AS extra_json,
                    COALESCE(NULLIF(sd.row_judgment, ''), NULLIF(fo.judgment, ''), NULLIF(fo.status, '')) AS submission_result,
                    fo.status AS report_status,
                    COALESCE(sd.inspection_time, fo.release_time, fo.inspection_time, fo.qa_time,
                             fo.submission_time, sd.update_time, sd.create_time) AS confirm_time,
                    fo.id AS inspection_id,
                    fo.fqc_no AS inspection_no,
                    'CUT_ROUND_FQC' AS inspection_source_type
                FROM mes_qms_fqc_submission_detail sd
                INNER JOIN mes_qms_fqc_order fo ON fo.deleted = b'0'
                    AND fo.id = sd.fqc_id
                    AND fo.source_module = 'CUT_ROUND_FQC'
                    AND fo.tenant_id = sd.tenant_id
                LEFT JOIN mes_sfc_cut_round_report c ON c.deleted = b'0'
                    AND c.id = sd.cut_round_report_id
                    AND c.tenant_id = sd.tenant_id
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0'
                    AND po.id = COALESCE(sd.plan_id, fo.plan_order_id, c.plan_id)
                WHERE sd.deleted = b'0'
                  <if test="tenantId != null">
                  AND sd.tenant_id = #{tenantId}
                  </if>
            ) source_rows
            WHERE confirm_time IS NOT NULL
              <if test="startTime != null">
              AND confirm_time &gt;= #{startTime}
              </if>
              <if test="endTime != null">
              AND confirm_time &lt; #{endTime}
              </if>
              <if test="processCode != null and processCode != '' and processCode != 'ALL'">
              AND process_code = #{processCode}
              </if>
            ORDER BY confirm_time DESC, process_code ASC, piece_no ASC
            </script>
            """)
    List<QmsYieldAnalysisSourceRow> selectSourceRows(@Param("tenantId") Long tenantId,
                                                     @Param("startTime") LocalDateTime startTime,
                                                     @Param("endTime") LocalDateTime endTime,
                                                     @Param("processCode") String processCode);

    @Select("""
            <script>
            SELECT
                CASE r.source_menu_code
                  WHEN 'FORMULA_REPORT' THEN 'FORMULA'
                  WHEN 'WET_REPORT' THEN 'WET'
                  ELSE r.source_menu_code
                END AS process_code,
                CASE r.source_menu_code
                  WHEN 'FORMULA_REPORT' THEN COALESCE(NULLIF(r.operation_name, ''), '配料')
                  WHEN 'WET_REPORT' THEN COALESCE(NULLIF(r.operation_name, ''), '湿法')
                  ELSE COALESCE(NULLIF(r.operation_name, ''), r.source_menu_code)
                END AS process_name,
                'mes_sfc_operation_report' AS source_table,
                r.id AS source_id,
                r.plan_id,
                COALESCE(NULLIF(r.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                r.plan_operation_id,
                COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                         NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, '')) AS mother_roll_no,
                COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                         NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, '')) AS segment_no,
                COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''), CONCAT('REPORT-', r.id)) AS piece_no,
                COALESCE(NULLIF(r.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                COALESCE(NULLIF(r.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                COALESCE(NULLIF(r.mother_model_code, ''), NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, ''),
                         NULLIF(r.mother_model_name, '')) AS model_code,
                COALESCE(NULLIF(r.feed_qty, 0), r.good_qty + r.scrap_qty, r.good_qty, 1) AS input_qty,
                COALESCE(r.good_qty, 0) AS output_good_qty,
                COALESCE(r.scrap_qty, 0) AS output_ng_qty,
                NULL AS self_check,
                NULL AS defect_code,
                NULL AS visual_result_json,
                r.extra_json,
                COALESCE(NULLIF(r.fai_judgment, ''), NULLIF(r.fai_status, ''), NULLIF(qf.judgment, ''), NULLIF(qf.status, '')) AS submission_result,
                r.operation_status AS report_status,
                COALESCE(r.confirmer_time, r.end_time, r.recorder_time, r.update_time, r.create_time) AS confirm_time,
                r.fai_id AS inspection_id,
                COALESCE(NULLIF(r.fai_no, ''), NULLIF(qf.fai_no, '')) AS inspection_no,
                'FAI' AS inspection_source_type,
                r.operation_code,
                r.operation_name,
                r.source_menu_code,
                r.report_type,
                r.report_uom,
                COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, '')) AS production_batch_no,
                r.parent_production_batch_no,
                r.report_date,
                r.start_time,
                r.end_time,
                r.feed_qty,
                r.good_qty,
                r.scrap_qty,
                r.output_stock_post_status,
                r.output_stock_post_time,
                COALESCE(NULLIF(r.fai_status, ''), NULLIF(qf.status, '')) AS inspection_status,
                COALESCE(NULLIF(r.fai_judgment, ''), NULLIF(qf.judgment, '')) AS inspection_result,
                r.recorder_name,
                r.recorder_time,
                r.confirmer_name,
                r.confirmer_time,
                r.remark
            FROM mes_sfc_operation_report r
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = r.plan_id
            LEFT JOIN mes_qms_fai_order qf ON qf.deleted = 0 AND qf.id = r.fai_id
            WHERE r.deleted = b'0'
              AND r.id = #{sourceId}
              AND r.source_menu_code IN ('FORMULA_REPORT', 'WET_REPORT')
              <if test="tenantId != null">
              AND r.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectOperationReportSourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                           @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT
                'ROUGH_GRINDING' AS process_code,
                '磨皮' AS process_name,
                'mes_sfc_grinding_second_detail' AS source_table,
                s.id AS source_id,
                s.plan_id,
                COALESCE(NULLIF(s.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                s.plan_operation_id,
                COALESCE(NULLIF(s.parent_production_batch_no, ''), NULLIF(s.mother_batch_no, ''),
                         NULLIF(s.source_production_batch_no, ''), NULLIF(po.parent_production_batch_no, ''),
                         NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, '')) AS mother_roll_no,
                COALESCE(NULLIF(s.production_batch_no, ''), NULLIF(s.confirmed_batch_no, ''),
                         NULLIF(s.source_production_batch_no, ''), NULLIF(s.mother_batch_no, '')) AS segment_no,
                COALESCE(NULLIF(s.production_batch_no, ''), NULLIF(s.confirmed_batch_no, ''), CONCAT('GRIND2-', s.id)) AS piece_no,
                COALESCE(NULLIF(po.material_code, ''), NULLIF(po.mother_material_code, '')) AS material_code,
                COALESCE(NULLIF(po.material_name, ''), NULLIF(po.mother_material_name, '')) AS material_name,
                COALESCE(NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                COALESCE(NULLIF(s.process_length, 0), s.output_length + s.loss_length + s.nap_sample_length,
                         s.output_length, 1) AS input_qty,
                COALESCE(s.output_length, 0) AS output_good_qty,
                COALESCE(s.loss_length, 0) AS output_ng_qty,
                s.self_check,
                s.defect_code,
                NULL AS visual_result_json,
                NULL AS extra_json,
                COALESCE(NULLIF(s.inspection_result, ''), NULLIF(s.inspection_status, '')) AS submission_result,
                COALESCE(NULLIF(s.detail_status, ''), NULLIF(s.confirm_status, ''), NULLIF(s.row_status, '')) AS report_status,
                COALESCE(s.confirm_time, s.end_time, s.update_time, s.create_time) AS confirm_time,
                s.inspection_id,
                s.inspection_no,
                'FAI' AS inspection_source_type,
                o.op_code AS operation_code,
                COALESCE(NULLIF(o.op_name, ''), '磨皮') AS operation_name,
                s.source_production_batch_no,
                s.production_batch_no,
                s.parent_production_batch_no,
                s.report_date,
                s.start_time,
                s.end_time,
                s.process_length AS input_length,
                s.start_position,
                NULL AS end_position,
                s.loss_length,
                s.output_length,
                s.nap_sample_length,
                s.inspection_status,
                s.inspection_result,
                s.confirm_operator_name AS confirmer_name,
                s.confirm_time AS confirmer_time,
                s.stock_post_status AS output_stock_post_status,
                s.stock_post_time AS output_stock_post_time,
                s.create_time AS recorder_time,
                s.remark
            FROM mes_sfc_grinding_second_detail s
            LEFT JOIN mes_pp_plan_operation o ON o.deleted = b'0' AND o.id = s.plan_operation_id
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = s.plan_id
            WHERE s.deleted = b'0'
              AND s.id = #{sourceId}
              <if test="tenantId != null">
              AND s.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectGrindingSecondSourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                          @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT
                'ADHESIVE1' AS process_code,
                COALESCE(NULLIF(a.operation_name, ''), '粘胶1') AS process_name,
                'mes_sfc_adhesive_report' AS source_table,
                a.id AS source_id,
                a.plan_id,
                COALESCE(NULLIF(a.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                a.plan_operation_id,
                COALESCE(NULLIF(a.parent_production_batch_no, ''), NULLIF(a.source_production_batch_no, ''),
                         NULLIF(a.source_batch_no, ''), NULLIF(po.parent_production_batch_no, ''),
                         NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, '')) AS mother_roll_no,
                CASE
                  WHEN a.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a.production_batch_no, '-J', 1)
                  ELSE COALESCE(NULLIF(a.source_production_batch_no, ''), NULLIF(a.production_batch_no, ''),
                                NULLIF(a.parent_production_batch_no, ''), NULLIF(a.source_batch_no, ''))
                END AS segment_no,
                COALESCE(NULLIF(a.production_batch_no, ''), CONCAT('ADH1-', a.id)) AS piece_no,
                COALESCE(NULLIF(a.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                COALESCE(NULLIF(a.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                COALESCE(NULLIF(a.model_code, ''), NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                COALESCE(NULLIF(a.input_length, 0), a.output_length + a.loss_length + a.nap_sample_length,
                         a.output_length, 1) AS input_qty,
                COALESCE(a.output_length, 0) AS output_good_qty,
                COALESCE(a.loss_length, 0) AS output_ng_qty,
                a.self_check,
                a.defect_code,
                NULL AS visual_result_json,
                a.extra_json,
                COALESCE(NULLIF(a.fai_judgment, ''), NULLIF(a.fai_status, '')) AS submission_result,
                a.report_status,
                COALESCE(a.confirmer_time, a.end_time, a.recorder_time, a.update_time, a.create_time) AS confirm_time,
                a.fai_id AS inspection_id,
                a.fai_no AS inspection_no,
                'FAI' AS inspection_source_type,
                a.operation_code,
                a.operation_name,
                a.source_batch_no,
                a.source_production_batch_no,
                a.production_batch_no,
                a.parent_production_batch_no,
                a.report_date,
                a.start_time,
                a.end_time,
                a.input_length,
                a.start_position,
                a.end_position,
                a.loss_length,
                a.output_length,
                a.nap_sample_length,
                a.output_stock_post_status,
                a.output_stock_post_time,
                a.product_quality_status,
                a.quality_lock_reason,
                a.fai_status AS inspection_status,
                a.fai_judgment AS inspection_result,
                a.recorder_name,
                a.recorder_time,
                a.confirmer_name,
                a.confirmer_time,
                a.remark
            FROM mes_sfc_adhesive_report a
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = a.plan_id
            WHERE a.deleted = b'0'
              AND a.id = #{sourceId}
              <if test="tenantId != null">
              AND a.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectAdhesive1SourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                     @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT
                'SLITTING' AS process_code,
                COALESCE(NULLIF(s.operation_name, ''), '分切') AS process_name,
                'mes_sfc_slitting_slice_record' AS source_table,
                s.id AS source_id,
                s.plan_id,
                COALESCE(NULLIF(s.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                s.plan_operation_id,
                COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, '')) AS mother_roll_no,
                CASE
                  WHEN s.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_batch_no
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(s.source_production_batch_no, '-J', 1)
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.source_production_batch_no, CHAR_LENGTH(s.source_production_batch_no) - 3)
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.source_production_batch_no, CHAR_LENGTH(s.source_production_batch_no) - 4)
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 3)
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 4)
                  ELSE COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, ''), NULLIF(s.slice_serial_no, ''))
                END AS segment_no,
                COALESCE(NULLIF(s.slice_serial_no, ''), CONCAT('SLIT-', s.id)) AS piece_no,
                po.material_code AS material_code,
                po.material_name AS material_name,
                COALESCE(NULLIF(a.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                s.self_check,
                NULL AS defect_code,
                s.visual_result_json,
                NULL AS extra_json,
                NULL AS submission_result,
                s.scan_status AS report_status,
                s.scan_time AS confirm_time,
                s.operation_code,
                s.operation_name,
                s.source_adhesive_report_id,
                s.source_stock_batch_no,
                s.source_lock_qty,
                s.source_consume_qty,
                s.source_consume_txn_no,
                s.source_consume_time,
                s.source_batch_no,
                s.source_production_batch_no,
                s.source_length,
                s.start_position,
                s.end_position,
                s.slice_length,
                s.cut_mode,
                s.slice_serial_no,
                s.slice_index,
                s.size_code,
                s.size_name,
                s.print_status,
                s.print_count,
                s.last_print_time,
                s.scan_status,
                s.scan_time,
                s.scanner_name,
                s.output_stock_post_status,
                s.output_stock_post_time,
                s.remark
            FROM mes_sfc_slitting_slice_record s
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = s.plan_id
            LEFT JOIN mes_sfc_adhesive_report a ON a.deleted = b'0' AND a.id = s.source_adhesive_report_id
                AND a.tenant_id = s.tenant_id
            WHERE s.deleted = b'0'
              AND s.id = #{sourceId}
              <if test="tenantId != null">
              AND s.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectSlittingSourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                    @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT
                'PRESS_SLOT' AS process_code,
                COALESCE(NULLIF(p.operation_name, ''), '压槽') AS process_name,
                'mes_sfc_press_slot_report' AS source_table,
                p.id AS source_id,
                p.plan_id,
                COALESCE(NULLIF(p.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                p.plan_operation_id,
                COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''),
                         NULLIF(p.parent_production_batch_no, '')) AS mother_roll_no,
                CASE
                  WHEN p.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_batch_no
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_production_batch_no
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(p.source_production_batch_no, '-J', 1)
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(p.source_production_batch_no, CHAR_LENGTH(p.source_production_batch_no) - 3)
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(p.source_production_batch_no, CHAR_LENGTH(p.source_production_batch_no) - 4)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 4)
                  WHEN p.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.parent_production_batch_no
                  ELSE COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''))
                END AS segment_no,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), CONCAT('PRESS-', p.id)) AS piece_no,
                COALESCE(NULLIF(p.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                COALESCE(NULLIF(p.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                COALESCE(NULLIF(p.model_code, ''), NULLIF(a.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                p.self_check,
                p.defect_code,
                NULL AS visual_result_json,
                p.extra_json,
                NULL AS submission_result,
                p.report_status,
                p.confirmer_time AS confirm_time,
                p.operation_code,
                p.operation_name,
                p.source_slitting_slice_id,
                sl.slice_serial_no AS source_slice_serial_no,
                p.source_stock_batch_no,
                p.source_lock_qty,
                p.source_consume_qty,
                p.source_consume_txn_no,
                p.source_consume_time,
                p.source_batch_no,
                p.source_production_batch_no,
                p.production_batch_no,
                p.parent_production_batch_no,
                p.report_date,
                p.start_time,
                p.end_time,
                p.input_length,
                p.start_position,
                p.end_position,
                p.loss_length,
                p.output_length,
                p.output_stock_post_status,
                p.output_stock_post_time,
                p.nap_sample_length,
                p.pressure_roller_material_code,
                p.pressure_roller_batch_no,
                p.bearing_material_code,
                p.bearing_batch_no,
                p.roller_clean_accumulated_pcs,
                p.bearing_replace_accumulated_pcs,
                p.recorder_name,
                p.recorder_time,
                p.confirmer_name,
                p.confirmer_time,
                p.remark
            FROM mes_sfc_press_slot_report p
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = p.plan_id
            LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = p.source_slitting_slice_id
            LEFT JOIN mes_sfc_adhesive_report a ON a.deleted = b'0' AND a.id = sl.source_adhesive_report_id
                AND a.tenant_id = p.tenant_id
            WHERE p.deleted = b'0'
              AND p.id = #{sourceId}
              <if test="tenantId != null">
              AND p.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectPressSlotSourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                     @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT
                'ADHESIVE2' AS process_code,
                COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS process_name,
                'mes_sfc_adhesive2_report' AS source_table,
                a2.id AS source_id,
                a2.plan_id,
                COALESCE(NULLIF(a2.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                a2.plan_operation_id,
                COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''),
                         NULLIF(a2.parent_production_batch_no, '')) AS mother_roll_no,
                CASE
                  WHEN a2.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_batch_no
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_production_batch_no
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a2.source_production_batch_no, '-J', 1)
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(a2.source_production_batch_no, CHAR_LENGTH(a2.source_production_batch_no) - 3)
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(a2.source_production_batch_no, CHAR_LENGTH(a2.source_production_batch_no) - 4)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 4)
                  WHEN a2.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.parent_production_batch_no
                  ELSE COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''))
                END AS segment_no,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), CONCAT('ADH2-', a2.id)) AS piece_no,
                COALESCE(NULLIF(a2.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                COALESCE(NULLIF(a2.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                COALESCE(NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''), NULLIF(sla.model_code, ''),
                         NULLIF(psla.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                a2.self_check,
                a2.defect_code,
                NULL AS visual_result_json,
                a2.extra_json,
                NULL AS submission_result,
                a2.report_status,
                a2.confirmer_time AS confirm_time,
                a2.operation_code,
                a2.operation_name,
                a2.source_press_slot_report_id,
                a2.source_slitting_slice_id,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, '')) AS source_slice_serial_no,
                ps.production_batch_no AS source_press_slot_production_batch_no,
                a2.source_stock_batch_no,
                a2.source_lock_qty,
                a2.source_consume_qty,
                a2.source_consume_txn_no,
                a2.source_consume_time,
                a2.source_batch_no,
                a2.source_production_batch_no,
                a2.production_batch_no,
                a2.parent_production_batch_no,
                a2.report_date,
                a2.start_time,
                a2.end_time,
                a2.input_length,
                a2.start_position,
                a2.end_position,
                a2.loss_length,
                a2.output_length,
                a2.output_stock_post_status,
                a2.output_stock_post_time,
                a2.nap_sample_length,
                a2.glue_board_model,
                a2.glue_board_material_code,
                a2.glue_board_batch_no,
                a2.glue_board_start_position,
                a2.glue_board_use_length,
                a2.product_quality_status,
                a2.quality_lock_reason,
                a2.recorder_name,
                a2.recorder_time,
                a2.confirmer_name,
                a2.confirmer_time,
                a2.remark
            FROM mes_sfc_adhesive2_report a2
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = a2.plan_id
            LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = a2.source_slitting_slice_id
            LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = a2.source_press_slot_report_id
            LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
            LEFT JOIN mes_sfc_adhesive_report sla ON sla.deleted = b'0' AND sla.id = sl.source_adhesive_report_id
                AND sla.tenant_id = a2.tenant_id
            LEFT JOIN mes_sfc_adhesive_report psla ON psla.deleted = b'0' AND psla.id = psl.source_adhesive_report_id
                AND psla.tenant_id = a2.tenant_id
            WHERE a2.deleted = b'0'
              AND a2.id = #{sourceId}
              <if test="tenantId != null">
              AND a2.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectAdhesive2SourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                     @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT
                'CUT_ROUND' AS process_code,
                COALESCE(NULLIF(c.operation_name, ''), '裁切') AS process_name,
                'mes_sfc_cut_round_report' AS source_table,
                c.id AS source_id,
                c.plan_id,
                COALESCE(NULLIF(c.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                c.plan_operation_id,
                COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''),
                         NULLIF(c.parent_production_batch_no, '')) AS mother_roll_no,
                CASE
                  WHEN c.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_batch_no
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_production_batch_no
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(c.source_production_batch_no, '-J', 1)
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(c.source_production_batch_no, CHAR_LENGTH(c.source_production_batch_no) - 3)
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(c.source_production_batch_no, CHAR_LENGTH(c.source_production_batch_no) - 4)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 4)
                  WHEN c.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.parent_production_batch_no
                  ELSE COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''))
                END AS segment_no,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), CONCAT('CUT-', c.id)) AS piece_no,
                COALESCE(NULLIF(c.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                COALESCE(NULLIF(c.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                COALESCE(NULLIF(c.model_code, ''), NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''),
                         NULLIF(sla.model_code, ''), NULLIF(psla.model_code, ''), NULLIF(a2sla.model_code, ''),
                         NULLIF(po.model_code, '')) AS model_code,
                c.self_check,
                c.defect_code,
                NULL AS visual_result_json,
                c.extra_json,
                c.inspection_result AS submission_result,
                c.report_status,
                c.confirmer_time AS confirm_time,
                c.operation_code,
                c.operation_name,
                c.source_adhesive2_report_id,
                c.source_press_slot_report_id,
                c.source_slitting_slice_id,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS source_slice_serial_no,
                ps.production_batch_no AS source_press_slot_production_batch_no,
                a2.production_batch_no AS source_adhesive2_production_batch_no,
                c.source_stock_batch_no,
                c.source_lock_qty,
                c.source_consume_qty,
                c.source_consume_txn_no,
                c.source_consume_time,
                c.source_batch_no,
                c.source_production_batch_no,
                c.production_batch_no,
                c.parent_production_batch_no,
                c.report_date,
                c.start_time,
                c.end_time,
                c.input_length,
                c.output_length,
                c.output_stock_post_status,
                c.output_stock_post_time,
                c.blade_material_code,
                c.blade_batch_no,
                c.felt_material_code,
                c.felt_batch_no,
                c.blade_use_count,
                c.felt_use_count,
                c.quality_risk_flag,
                c.quality_risk_snapshot_json,
                c.inspection_task_id,
                c.inspection_task_no,
                c.inspection_status,
                c.inspection_result,
                c.inspector_name,
                c.inspection_time,
                c.inspection_remark,
                c.recorder_name,
                c.recorder_time,
                c.confirmer_name,
                c.confirmer_time,
                c.remark
            FROM mes_sfc_cut_round_report c
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = c.plan_id
            LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = c.source_slitting_slice_id
            LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = c.source_press_slot_report_id
            LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
            LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = b'0' AND a2.id = c.source_adhesive2_report_id
            LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = b'0' AND a2sl.id = a2.source_slitting_slice_id
            LEFT JOIN mes_sfc_adhesive_report sla ON sla.deleted = b'0' AND sla.id = sl.source_adhesive_report_id
                AND sla.tenant_id = c.tenant_id
            LEFT JOIN mes_sfc_adhesive_report psla ON psla.deleted = b'0' AND psla.id = psl.source_adhesive_report_id
                AND psla.tenant_id = c.tenant_id
            LEFT JOIN mes_sfc_adhesive_report a2sla ON a2sla.deleted = b'0' AND a2sla.id = a2sl.source_adhesive_report_id
                AND a2sla.tenant_id = c.tenant_id
            WHERE c.deleted = b'0'
              AND c.id = #{sourceId}
              <if test="tenantId != null">
              AND c.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectCutRoundSourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                    @Param("sourceId") Long sourceId);

    @Select("""
            <script>
            SELECT
                'FINAL_INSPECTION' AS process_code,
                '终检' AS process_name,
                'mes_qms_fqc_submission_detail' AS source_table,
                sd.id AS source_id,
                COALESCE(sd.plan_id, fo.plan_order_id, c.plan_id) AS plan_id,
                COALESCE(NULLIF(sd.plan_no, ''), NULLIF(po.plan_no, ''), NULLIF(c.plan_no, '')) AS plan_no,
                COALESCE(sd.plan_operation_id, c.plan_operation_id) AS plan_operation_id,
                COALESCE(NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                         NULLIF(po.batch_no, ''), NULLIF(sd.parent_production_batch_no, ''),
                         NULLIF(c.parent_production_batch_no, '')) AS mother_roll_no,
                CASE
                  WHEN sd.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN sd.parent_production_batch_no
                  WHEN c.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_batch_no
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_production_batch_no
                  WHEN sd.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(sd.production_batch_no, CHAR_LENGTH(sd.production_batch_no) - 3)
                  WHEN sd.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(sd.production_batch_no, CHAR_LENGTH(sd.production_batch_no) - 4)
                  ELSE COALESCE(NULLIF(sd.parent_production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''),
                                NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, ''))
                END AS segment_no,
                COALESCE(NULLIF(sd.production_batch_no, ''), NULLIF(c.production_batch_no, ''), CONCAT('FQC-', sd.id)) AS piece_no,
                COALESCE(NULLIF(sd.material_code, ''), NULLIF(fo.material_code, ''), NULLIF(c.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                COALESCE(NULLIF(sd.material_name, ''), NULLIF(fo.material_name, ''), NULLIF(c.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                COALESCE(NULLIF(sd.model_code, ''), NULLIF(fo.product_model, ''), NULLIF(c.model_code, ''), NULLIF(po.model_code, '')) AS model_code,
                1 AS input_qty,
                CASE WHEN UPPER(COALESCE(sd.row_judgment, '')) = 'OK' THEN 1 ELSE 0 END AS output_good_qty,
                CASE WHEN UPPER(COALESCE(sd.row_judgment, '')) = 'NG' THEN 1 ELSE 0 END AS output_ng_qty,
                COALESCE(NULLIF(sd.row_judgment, ''), 'PENDING') AS self_check,
                COALESCE(NULLIF(sd.defect_name, ''), NULLIF(sd.defect_code, '')) AS defect_code,
                COALESCE(
                    (
                        SELECT JSON_ARRAYAGG(JSON_OBJECT('defectName', d.defect_name, 'result', 'NG'))
                        FROM mes_qms_fqc_sample_defect d
                        INNER JOIN mes_qms_fqc_sample s ON s.deleted = b'0' AND s.id = d.sample_id
                        WHERE d.deleted = b'0'
                          AND s.submission_detail_id = sd.id
                          AND d.tenant_id = sd.tenant_id
                          AND NULLIF(d.defect_name, '') IS NOT NULL
                    ),
                    (
                        SELECT JSON_ARRAYAGG(JSON_OBJECT('defectName', s.defect_name, 'result', 'NG'))
                        FROM mes_qms_fqc_sample s
                        WHERE s.deleted = b'0'
                          AND s.submission_detail_id = sd.id
                          AND s.tenant_id = sd.tenant_id
                          AND NULLIF(s.defect_name, '') IS NOT NULL
                    )
                ) AS visual_result_json,
                sd.quality_risk_snapshot_json AS extra_json,
                COALESCE(NULLIF(sd.row_judgment, ''), NULLIF(fo.judgment, ''), NULLIF(fo.status, '')) AS submission_result,
                fo.status AS report_status,
                COALESCE(sd.inspection_time, fo.release_time, fo.inspection_time, fo.qa_time,
                         fo.submission_time, sd.update_time, sd.create_time) AS confirm_time,
                fo.id AS inspection_id,
                fo.fqc_no AS inspection_no,
                'CUT_ROUND_FQC' AS inspection_source_type,
                sd.operation_code,
                COALESCE(NULLIF(sd.operation_name, ''), NULLIF(fo.operation_name, ''), '终检') AS operation_name,
                NULL AS source_press_slot_report_id,
                c.source_slitting_slice_id,
                c.source_production_batch_no AS source_slice_serial_no,
                c.source_batch_no,
                c.source_production_batch_no,
                sd.production_batch_no,
                sd.parent_production_batch_no,
                sd.size_rule AS size_name,
                sd.quality_risk_flag,
                sd.quality_risk_snapshot_json,
                sd.cut_round_inspection_task_id AS inspection_task_id,
                sd.cut_round_inspection_task_no AS inspection_task_no,
                fo.status AS inspection_status,
                sd.row_judgment AS inspection_result,
                COALESCE(NULLIF(sd.inspector_name, ''), NULLIF(fo.qa_inspector_name, ''), NULLIF(fo.inspector_name, '')) AS inspector_name,
                COALESCE(sd.inspection_time, fo.qa_time, fo.inspection_time, fo.release_time) AS inspection_time,
                COALESCE(NULLIF(sd.ng_reason, ''), NULLIF(sd.remark, ''), NULLIF(fo.remark, '')) AS inspection_remark,
                fo.submitter_name AS recorder_name,
                fo.submission_time AS recorder_time,
                fo.qa_inspector_name AS confirmer_name,
                fo.qa_time AS confirmer_time,
                sd.remark
            FROM mes_qms_fqc_submission_detail sd
            INNER JOIN mes_qms_fqc_order fo ON fo.deleted = b'0'
                AND fo.id = sd.fqc_id
                AND fo.source_module = 'CUT_ROUND_FQC'
                AND fo.tenant_id = sd.tenant_id
            LEFT JOIN mes_sfc_cut_round_report c ON c.deleted = b'0'
                AND c.id = sd.cut_round_report_id
                AND c.tenant_id = sd.tenant_id
            LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0'
                AND po.id = COALESCE(sd.plan_id, fo.plan_order_id, c.plan_id)
            WHERE sd.deleted = b'0'
              AND sd.id = #{sourceId}
              <if test="tenantId != null">
              AND sd.tenant_id = #{tenantId}
              </if>
            </script>
            """)
    QmsYieldAnalysisSourcePreviewRow selectFinalInspectionSourcePreviewRow(@Param("tenantId") Long tenantId,
                                                                          @Param("sourceId") Long sourceId);
}
