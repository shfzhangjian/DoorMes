package cn.iocoder.yudao.module.mes.dal.mysql.qms;

import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsDefectCategoryAnalysisEventRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface QmsDefectCategoryAnalysisMapper {

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT
                    'SLITTING_VISUAL' AS event_source,
                    'SLITTING' AS process_code,
                    COALESCE(NULLIF(s.operation_name, ''), '分切') AS process_name,
                    'SELF_CHECK' AS inspection_type,
                    'mes_sfc_slitting_slice_record' AS source_table,
                    s.id AS source_id,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    COALESCE(NULLIF(s.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(s.source_batch_no, ''), NULLIF(po.parent_production_batch_no, ''),
                             NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, ''),
                             NULLIF(s.slice_serial_no, '')) AS segment_batch_no,
                    NULLIF(s.slice_serial_no, '') AS scan_confirm_piece_no,
                    NULLIF(s.slice_serial_no, '') AS process_piece_no,
                    COALESCE(NULLIF(po.material_code, ''), NULLIF(a.material_code, '')) AS material_code,
                    COALESCE(NULLIF(po.material_name, ''), NULLIF(a.material_name, '')) AS material_name,
                    COALESCE(NULLIF(a.model_code, ''), NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                    NULL AS defect_code,
                    NULL AS defect_name,
                    NULL AS defect_level,
                    NULL AS inspection_category,
                    NULLIF(s.scanner_name, '') AS inspector_name,
                    NULLIF(s.self_check, '') AS check_result,
                    NULL AS remark,
                    s.visual_result_json AS visual_result_json,
                    s.scan_time AS scan_confirm_time,
                    s.scan_time AS inspection_time,
                    s.scan_time AS event_time,
                    1 AS quantity
                FROM mes_sfc_slitting_slice_record s
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = s.plan_id
                LEFT JOIN mes_sfc_adhesive_report a ON a.deleted = b'0' AND a.id = s.source_adhesive_report_id
                    AND a.tenant_id = s.tenant_id
                WHERE s.deleted = b'0'
                  AND s.scan_status = 'CONFIRMED'
                  AND (UPPER(COALESCE(s.self_check, '')) = 'NG' OR NULLIF(s.visual_result_json, '') IS NOT NULL)
                  <if test="tenantId != null">
                  AND s.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'PRESS_SLOT_SELF_CHECK' AS event_source,
                    'PRESS_SLOT' AS process_code,
                    COALESCE(NULLIF(p.operation_name, ''), '压槽') AS process_name,
                    'SELF_CHECK' AS inspection_type,
                    'mes_sfc_press_slot_report' AS source_table,
                    p.id AS source_id,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    COALESCE(NULLIF(p.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(p.source_batch_no, ''),
                             NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                             NULLIF(po.batch_no, ''), NULLIF(p.parent_production_batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(p.source_production_batch_no, ''),
                             NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''),
                             NULLIF(sl.slice_serial_no, '')) AS segment_batch_no,
                    NULLIF(sl.slice_serial_no, '') AS scan_confirm_piece_no,
                    COALESCE(NULLIF(p.production_batch_no, ''), NULLIF(sl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(p.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(p.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(p.model_code, ''), NULLIF(a.model_code, ''), NULLIF(po.model_code, ''),
                             NULLIF(po.mother_model_code, '')) AS model_code,
                    NULLIF(p.defect_code, '') AS defect_code,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(p.defect_code, '')) AS defect_name,
                    NULLIF(dc.level, '') AS defect_level,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(p.defect_code, '')) AS inspection_category,
                    COALESCE(NULLIF(p.confirmer_name, ''), NULLIF(p.recorder_name, '')) AS inspector_name,
                    NULLIF(p.self_check, '') AS check_result,
                    NULLIF(p.remark, '') AS remark,
                    p.extra_json AS visual_result_json,
                    sl.scan_time AS scan_confirm_time,
                    COALESCE(p.confirmer_time, p.recorder_time, sl.scan_time) AS inspection_time,
                    COALESCE(p.confirmer_time, p.recorder_time, sl.scan_time) AS event_time,
                    1 AS quantity
                FROM mes_sfc_press_slot_report p
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = p.plan_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = p.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive_report a ON a.deleted = b'0' AND a.id = sl.source_adhesive_report_id
                    AND a.tenant_id = p.tenant_id
                LEFT JOIN mes_qms_defect_code dc ON dc.deleted = b'0'
                    AND dc.code = p.defect_code
                    AND dc.tenant_id = p.tenant_id
                WHERE p.deleted = b'0'
                  AND (
                    NULLIF(p.defect_code, '') IS NOT NULL
                    OR (
                      UPPER(COALESCE(p.self_check, '')) = 'NG'
                      AND NOT EXISTS (
                        SELECT 1
                        FROM mes_sfc_press_slot_check_detail cd
                        WHERE cd.deleted = b'0'
                          AND cd.press_slot_report_id = p.id
                          AND cd.tenant_id = p.tenant_id
                          AND UPPER(COALESCE(cd.check_result, '')) IN ('NG', 'FAIL', 'FAILED', 'ABNORMAL')
                      )
                    )
                  )
                  <if test="tenantId != null">
                  AND p.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'ADHESIVE2_SELF_CHECK' AS event_source,
                    'ADHESIVE2' AS process_code,
                    COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS process_name,
                    'SELF_CHECK' AS inspection_type,
                    'mes_sfc_adhesive2_report' AS source_table,
                    a2.id AS source_id,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    COALESCE(NULLIF(a2.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(psl.source_batch_no, ''), NULLIF(a2.source_batch_no, ''),
                             NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                             NULLIF(po.batch_no, ''), NULLIF(a2.parent_production_batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(psl.source_production_batch_no, ''),
                             NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''),
                             NULLIF(a2.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, '')) AS segment_batch_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, '')) AS scan_confirm_piece_no,
                    COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''),
                             NULLIF(psl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(a2.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(a2.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''), NULLIF(sla.model_code, ''),
                             NULLIF(psla.model_code, ''), NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                    NULLIF(a2.defect_code, '') AS defect_code,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(a2.defect_code, '')) AS defect_name,
                    NULLIF(dc.level, '') AS defect_level,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(a2.defect_code, '')) AS inspection_category,
                    COALESCE(NULLIF(a2.confirmer_name, ''), NULLIF(a2.recorder_name, '')) AS inspector_name,
                    NULLIF(a2.self_check, '') AS check_result,
                    NULLIF(a2.remark, '') AS remark,
                    a2.extra_json AS visual_result_json,
                    COALESCE(sl.scan_time, psl.scan_time) AS scan_confirm_time,
                    COALESCE(a2.confirmer_time, a2.recorder_time, sl.scan_time, psl.scan_time) AS inspection_time,
                    COALESCE(a2.confirmer_time, a2.recorder_time, sl.scan_time, psl.scan_time) AS event_time,
                    1 AS quantity
                FROM mes_sfc_adhesive2_report a2
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = a2.plan_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = a2.source_slitting_slice_id
                LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = a2.source_press_slot_report_id
                LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive_report sla ON sla.deleted = b'0' AND sla.id = sl.source_adhesive_report_id
                    AND sla.tenant_id = a2.tenant_id
                LEFT JOIN mes_sfc_adhesive_report psla ON psla.deleted = b'0' AND psla.id = psl.source_adhesive_report_id
                    AND psla.tenant_id = a2.tenant_id
                LEFT JOIN mes_qms_defect_code dc ON dc.deleted = b'0'
                    AND dc.code = a2.defect_code
                    AND dc.tenant_id = a2.tenant_id
                WHERE a2.deleted = b'0'
                  AND (
                    NULLIF(a2.defect_code, '') IS NOT NULL
                    OR (
                      UPPER(COALESCE(a2.self_check, '')) = 'NG'
                      AND NOT EXISTS (
                        SELECT 1
                        FROM mes_sfc_adhesive2_check_detail cd
                        WHERE cd.deleted = b'0'
                          AND cd.adhesive2_report_id = a2.id
                          AND cd.tenant_id = a2.tenant_id
                          AND UPPER(COALESCE(cd.check_result, '')) IN ('NG', 'FAIL', 'FAILED', 'ABNORMAL')
                      )
                    )
                  )
                  <if test="tenantId != null">
                  AND a2.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'CUT_ROUND_SELF_CHECK' AS event_source,
                    'CUT_ROUND' AS process_code,
                    COALESCE(NULLIF(c.operation_name, ''), '裁切') AS process_name,
                    'SELF_CHECK' AS inspection_type,
                    'mes_sfc_cut_round_report' AS source_table,
                    c.id AS source_id,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    COALESCE(NULLIF(c.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(psl.source_batch_no, ''), NULLIF(a2sl.source_batch_no, ''),
                             NULLIF(c.source_batch_no, ''), NULLIF(po.parent_production_batch_no, ''),
                             NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''), NULLIF(c.parent_production_batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(psl.source_production_batch_no, ''),
                             NULLIF(a2sl.source_production_batch_no, ''), NULLIF(c.source_production_batch_no, ''),
                             NULLIF(c.parent_production_batch_no, ''), NULLIF(c.production_batch_no, ''),
                             NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS segment_batch_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS scan_confirm_piece_no,
                    COALESCE(NULLIF(c.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''),
                             NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(c.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(c.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(c.model_code, ''), NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''),
                             NULLIF(sla.model_code, ''), NULLIF(psla.model_code, ''), NULLIF(a2sla.model_code, ''),
                             NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                    NULLIF(c.defect_code, '') AS defect_code,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(c.defect_code, '')) AS defect_name,
                    NULLIF(dc.level, '') AS defect_level,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(c.defect_code, '')) AS inspection_category,
                    COALESCE(NULLIF(c.confirmer_name, ''), NULLIF(c.recorder_name, ''), NULLIF(c.inspector_name, '')) AS inspector_name,
                    NULLIF(c.self_check, '') AS check_result,
                    NULLIF(c.remark, '') AS remark,
                    c.extra_json AS visual_result_json,
                    COALESCE(sl.scan_time, psl.scan_time, a2sl.scan_time) AS scan_confirm_time,
                    COALESCE(c.confirmer_time, c.recorder_time, c.inspection_time, sl.scan_time, psl.scan_time, a2sl.scan_time) AS inspection_time,
                    COALESCE(c.confirmer_time, c.recorder_time, c.inspection_time, sl.scan_time, psl.scan_time, a2sl.scan_time) AS event_time,
                    1 AS quantity
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
                LEFT JOIN mes_qms_defect_code dc ON dc.deleted = b'0'
                    AND dc.code = c.defect_code
                    AND dc.tenant_id = c.tenant_id
                WHERE c.deleted = b'0'
                  AND (
                    NULLIF(c.defect_code, '') IS NOT NULL
                    OR (
                      UPPER(COALESCE(c.self_check, '')) = 'NG'
                      AND NOT EXISTS (
                        SELECT 1
                        FROM mes_sfc_cut_round_check_detail cd
                        WHERE cd.deleted = b'0'
                          AND cd.cut_round_report_id = c.id
                          AND cd.tenant_id = c.tenant_id
                          AND UPPER(COALESCE(cd.check_result, '')) IN ('NG', 'FAIL', 'FAILED', 'ABNORMAL')
                      )
                    )
                  )
                  <if test="tenantId != null">
                  AND c.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'PRESS_SLOT_CHECK_DETAIL' AS event_source,
                    'PRESS_SLOT' AS process_code,
                    COALESCE(NULLIF(p.operation_name, ''), '压槽') AS process_name,
                    'SELF_CHECK' AS inspection_type,
                    'mes_sfc_press_slot_check_detail' AS source_table,
                    cd.id AS source_id,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    COALESCE(NULLIF(cd.plan_no, ''), NULLIF(p.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(p.source_batch_no, ''),
                             NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                             NULLIF(po.batch_no, ''), NULLIF(p.parent_production_batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(p.source_production_batch_no, ''),
                             NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''),
                             NULLIF(sl.slice_serial_no, '')) AS segment_batch_no,
                    NULLIF(sl.slice_serial_no, '') AS scan_confirm_piece_no,
                    COALESCE(NULLIF(p.production_batch_no, ''), NULLIF(sl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(p.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(p.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(p.model_code, ''), NULLIF(a.model_code, ''), NULLIF(po.model_code, ''),
                             NULLIF(po.mother_model_code, '')) AS model_code,
                    NULL AS defect_code,
                    NULL AS defect_name,
                    NULL AS defect_level,
                    COALESCE(NULLIF(cd.item_name, ''), NULLIF(cd.item_category, ''), '未分类自检异常') AS inspection_category,
                    COALESCE(NULLIF(p.confirmer_name, ''), NULLIF(p.recorder_name, '')) AS inspector_name,
                    NULLIF(cd.check_result, '') AS check_result,
                    NULLIF(cd.abnormal_remark, '') AS remark,
                    NULL AS visual_result_json,
                    sl.scan_time AS scan_confirm_time,
                    COALESCE(cd.update_time, cd.create_time, p.confirmer_time, p.recorder_time, sl.scan_time) AS inspection_time,
                    COALESCE(cd.update_time, cd.create_time, p.confirmer_time, p.recorder_time, sl.scan_time) AS event_time,
                    1 AS quantity
                FROM mes_sfc_press_slot_check_detail cd
                INNER JOIN mes_sfc_press_slot_report p ON p.deleted = b'0' AND p.id = cd.press_slot_report_id
                    AND p.tenant_id = cd.tenant_id
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = p.plan_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = p.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive_report a ON a.deleted = b'0' AND a.id = sl.source_adhesive_report_id
                    AND a.tenant_id = p.tenant_id
                WHERE cd.deleted = b'0'
                  AND UPPER(COALESCE(cd.check_result, '')) IN ('NG', 'FAIL', 'FAILED', 'ABNORMAL')
                  <if test="tenantId != null">
                  AND cd.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'ADHESIVE2_CHECK_DETAIL' AS event_source,
                    'ADHESIVE2' AS process_code,
                    COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS process_name,
                    'SELF_CHECK' AS inspection_type,
                    'mes_sfc_adhesive2_check_detail' AS source_table,
                    cd.id AS source_id,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    COALESCE(NULLIF(cd.plan_no, ''), NULLIF(a2.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(psl.source_batch_no, ''), NULLIF(a2.source_batch_no, ''),
                             NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                             NULLIF(po.batch_no, ''), NULLIF(a2.parent_production_batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(psl.source_production_batch_no, ''),
                             NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''),
                             NULLIF(a2.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, '')) AS segment_batch_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, '')) AS scan_confirm_piece_no,
                    COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''),
                             NULLIF(psl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(a2.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(a2.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''), NULLIF(sla.model_code, ''),
                             NULLIF(psla.model_code, ''), NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                    NULL AS defect_code,
                    NULL AS defect_name,
                    NULL AS defect_level,
                    COALESCE(NULLIF(cd.item_name, ''), NULLIF(cd.item_category, ''), '未分类自检异常') AS inspection_category,
                    COALESCE(NULLIF(a2.confirmer_name, ''), NULLIF(a2.recorder_name, '')) AS inspector_name,
                    NULLIF(cd.check_result, '') AS check_result,
                    NULLIF(cd.abnormal_remark, '') AS remark,
                    NULL AS visual_result_json,
                    COALESCE(sl.scan_time, psl.scan_time) AS scan_confirm_time,
                    COALESCE(cd.update_time, cd.create_time, a2.confirmer_time, a2.recorder_time, sl.scan_time, psl.scan_time) AS inspection_time,
                    COALESCE(cd.update_time, cd.create_time, a2.confirmer_time, a2.recorder_time, sl.scan_time, psl.scan_time) AS event_time,
                    1 AS quantity
                FROM mes_sfc_adhesive2_check_detail cd
                INNER JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = b'0' AND a2.id = cd.adhesive2_report_id
                    AND a2.tenant_id = cd.tenant_id
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0' AND po.id = a2.plan_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = a2.source_slitting_slice_id
                LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = a2.source_press_slot_report_id
                LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive_report sla ON sla.deleted = b'0' AND sla.id = sl.source_adhesive_report_id
                    AND sla.tenant_id = a2.tenant_id
                LEFT JOIN mes_sfc_adhesive_report psla ON psla.deleted = b'0' AND psla.id = psl.source_adhesive_report_id
                    AND psla.tenant_id = a2.tenant_id
                WHERE cd.deleted = b'0'
                  AND UPPER(COALESCE(cd.check_result, '')) IN ('NG', 'FAIL', 'FAILED', 'ABNORMAL')
                  <if test="tenantId != null">
                  AND cd.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'CUT_ROUND_CHECK_DETAIL' AS event_source,
                    'CUT_ROUND' AS process_code,
                    COALESCE(NULLIF(c.operation_name, ''), '裁切') AS process_name,
                    'SELF_CHECK' AS inspection_type,
                    'mes_sfc_cut_round_check_detail' AS source_table,
                    cd.id AS source_id,
                    NULL AS inspection_id,
                    NULL AS inspection_no,
                    COALESCE(NULLIF(cd.plan_no, ''), NULLIF(c.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(psl.source_batch_no, ''), NULLIF(a2sl.source_batch_no, ''),
                             NULLIF(c.source_batch_no, ''), NULLIF(po.parent_production_batch_no, ''),
                             NULLIF(po.production_batch_no, ''), NULLIF(po.batch_no, ''), NULLIF(c.parent_production_batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(psl.source_production_batch_no, ''),
                             NULLIF(a2sl.source_production_batch_no, ''), NULLIF(c.source_production_batch_no, ''),
                             NULLIF(c.parent_production_batch_no, ''), NULLIF(c.production_batch_no, ''),
                             NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS segment_batch_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS scan_confirm_piece_no,
                    COALESCE(NULLIF(c.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''),
                             NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(c.material_code, ''), NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(c.material_name, ''), NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(c.model_code, ''), NULLIF(a2.model_code, ''), NULLIF(ps.model_code, ''),
                             NULLIF(sla.model_code, ''), NULLIF(psla.model_code, ''), NULLIF(a2sla.model_code, ''),
                             NULLIF(po.model_code, ''), NULLIF(po.mother_model_code, '')) AS model_code,
                    NULL AS defect_code,
                    NULL AS defect_name,
                    NULL AS defect_level,
                    COALESCE(NULLIF(cd.item_name, ''), NULLIF(cd.item_category, ''), '未分类自检异常') AS inspection_category,
                    COALESCE(NULLIF(c.confirmer_name, ''), NULLIF(c.recorder_name, ''), NULLIF(c.inspector_name, '')) AS inspector_name,
                    NULLIF(cd.check_result, '') AS check_result,
                    NULLIF(cd.abnormal_remark, '') AS remark,
                    NULL AS visual_result_json,
                    COALESCE(sl.scan_time, psl.scan_time, a2sl.scan_time) AS scan_confirm_time,
                    COALESCE(cd.update_time, cd.create_time, c.confirmer_time, c.recorder_time, c.inspection_time,
                             sl.scan_time, psl.scan_time, a2sl.scan_time) AS inspection_time,
                    COALESCE(cd.update_time, cd.create_time, c.confirmer_time, c.recorder_time, c.inspection_time,
                             sl.scan_time, psl.scan_time, a2sl.scan_time) AS event_time,
                    1 AS quantity
                FROM mes_sfc_cut_round_check_detail cd
                INNER JOIN mes_sfc_cut_round_report c ON c.deleted = b'0' AND c.id = cd.cut_round_report_id
                    AND c.tenant_id = cd.tenant_id
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
                WHERE cd.deleted = b'0'
                  AND UPPER(COALESCE(cd.check_result, '')) IN ('NG', 'FAIL', 'FAILED', 'ABNORMAL')
                  <if test="tenantId != null">
                  AND cd.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'FAI_ITEM' AS event_source,
                    CASE
                      WHEN fo.source_module = 'ADHESIVE2_REPORT' OR fo.source_operation_name LIKE '%粘胶2%' THEN 'ADHESIVE2'
                      ELSE 'PRESS_SLOT'
                    END AS process_code,
                    CASE
                      WHEN fo.source_module = 'ADHESIVE2_REPORT' OR fo.source_operation_name LIKE '%粘胶2%' THEN '粘胶2'
                      ELSE '压槽'
                    END AS process_name,
                    'SUBMISSION' AS inspection_type,
                    'mes_qms_fai_item' AS source_table,
                    fi.id AS source_id,
                    fo.id AS inspection_id,
                    fo.fai_no AS inspection_no,
                    COALESCE(NULLIF(p.plan_no, ''), NULLIF(a2.plan_no, ''), NULLIF(fo.work_order_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(a2sl.source_batch_no, ''), NULLIF(psl.source_batch_no, ''),
                             NULLIF(p.source_batch_no, ''), NULLIF(a2.source_batch_no, ''), NULLIF(fo.product_batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(a2sl.source_production_batch_no, ''),
                             NULLIF(psl.source_production_batch_no, ''), NULLIF(p.source_production_batch_no, ''),
                             NULLIF(a2.source_production_batch_no, ''), NULLIF(fo.product_batch_no, '')) AS segment_batch_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''),
                             NULLIF(psl.slice_serial_no, ''), NULLIF(fo.product_batch_no, '')) AS scan_confirm_piece_no,
                    COALESCE(NULLIF(p.production_batch_no, ''), NULLIF(a2.production_batch_no, ''),
                             NULLIF(fo.product_batch_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(p.material_code, ''), NULLIF(a2.material_code, ''), NULLIF(fo.material_code, '')) AS material_code,
                    COALESCE(NULLIF(p.material_name, ''), NULLIF(a2.material_name, ''), NULLIF(fo.material_name, '')) AS material_name,
                    COALESCE(NULLIF(p.model_code, ''), NULLIF(a2.model_code, ''), NULLIF(fo.product_model, '')) AS model_code,
                    NULL AS defect_code,
                    NULL AS defect_name,
                    NULL AS defect_level,
                    COALESCE(NULLIF(fi.inspection_item, ''), '未分类FAI异常') AS inspection_category,
                    COALESCE(NULLIF(fi.qa_inspector_name, ''), NULLIF(fo.qa_inspector_name, ''),
                             NULLIF(fi.operator_name, ''), NULLIF(fo.submitter_name, '')) AS inspector_name,
                    COALESCE(NULLIF(fi.qa_result, ''), NULLIF(fi.operator_result, ''), NULLIF(fo.judgment, '')) AS check_result,
                    NULL AS remark,
                    NULL AS visual_result_json,
                    COALESCE(sl.scan_time, a2sl.scan_time, psl.scan_time) AS scan_confirm_time,
                    COALESCE(fi.qa_time, fi.operator_time, fo.qa_time, fo.inspection_time, fo.submission_time) AS inspection_time,
                    COALESCE(fi.qa_time, fi.operator_time, fo.qa_time, fo.inspection_time, fo.submission_time) AS event_time,
                    1 AS quantity
                FROM mes_qms_fai_item fi
                INNER JOIN mes_qms_fai_order fo ON fo.deleted = b'0' AND fo.id = fi.fai_id
                    AND fo.tenant_id = fi.tenant_id
                LEFT JOIN mes_sfc_press_slot_report p ON p.deleted = b'0'
                    AND fo.source_module = 'PRESS_SLOT_REPORT'
                    AND p.id = fo.source_report_id
                    AND p.tenant_id = fo.tenant_id
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = p.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = b'0'
                    AND fo.source_module = 'ADHESIVE2_REPORT'
                    AND a2.id = fo.source_report_id
                    AND a2.tenant_id = fo.tenant_id
                LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = b'0' AND a2sl.id = a2.source_slitting_slice_id
                LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = a2.source_press_slot_report_id
                LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
                WHERE fi.deleted = b'0'
                  AND UPPER(COALESCE(NULLIF(fi.qa_result, ''), NULLIF(fi.operator_result, ''), '')) = 'NG'
                  AND (
                    fo.source_module IN ('PRESS_SLOT_REPORT', 'ADHESIVE2_REPORT')
                    OR fo.source_operation_name LIKE '%压槽%'
                    OR fo.source_operation_name LIKE '%粘胶2%'
                  )
                  <if test="tenantId != null">
                  AND fi.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'FQC_DEFECT' AS event_source,
                    'CUT_ROUND' AS process_code,
                    '裁切' AS process_name,
                    'FINAL_INSPECTION' AS inspection_type,
                    'mes_qms_fqc_sample_defect' AS source_table,
                    d.id AS source_id,
                    fo.id AS inspection_id,
                    fo.fqc_no AS inspection_no,
                    COALESCE(NULLIF(sd.plan_no, ''), NULLIF(c.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(psl.source_batch_no, ''), NULLIF(a2sl.source_batch_no, ''),
                             NULLIF(c.source_batch_no, ''), NULLIF(sd.parent_production_batch_no, ''),
                             NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                             NULLIF(po.batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(psl.source_production_batch_no, ''),
                             NULLIF(a2sl.source_production_batch_no, ''), NULLIF(c.source_production_batch_no, ''),
                             NULLIF(sd.parent_production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''),
                             NULLIF(sd.production_batch_no, ''), NULLIF(c.production_batch_no, '')) AS segment_batch_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS scan_confirm_piece_no,
                    COALESCE(NULLIF(sd.production_batch_no, ''), NULLIF(c.production_batch_no, ''),
                             NULLIF(s.production_batch_no, ''), NULLIF(sl.slice_serial_no, ''),
                             NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(sd.material_code, ''), NULLIF(c.material_code, ''), NULLIF(fo.material_code, ''),
                             NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(sd.material_name, ''), NULLIF(c.material_name, ''), NULLIF(fo.material_name, ''),
                             NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(sd.model_code, ''), NULLIF(c.model_code, ''), NULLIF(a2.model_code, ''),
                             NULLIF(ps.model_code, ''), NULLIF(fo.product_model, ''), NULLIF(po.model_code, ''),
                             NULLIF(po.mother_model_code, '')) AS model_code,
                    NULLIF(d.defect_code, '') AS defect_code,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(d.defect_name, ''), NULLIF(d.defect_code, '')) AS defect_name,
                    COALESCE(NULLIF(dc.level, ''), NULLIF(d.defect_level, '')) AS defect_level,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(d.defect_name, ''), NULLIF(d.defect_code, ''), '未分类终检异常') AS inspection_category,
                    COALESCE(NULLIF(sd.inspector_name, ''), NULLIF(fo.qa_inspector_name, ''),
                             NULLIF(fo.inspector_name, ''), NULLIF(fo.submitter_name, '')) AS inspector_name,
                    COALESCE(NULLIF(sd.row_judgment, ''), NULLIF(fo.judgment, '')) AS check_result,
                    COALESCE(NULLIF(sd.ng_reason, ''), NULLIF(sd.remark, '')) AS remark,
                    NULL AS visual_result_json,
                    COALESCE(sl.scan_time, psl.scan_time, a2sl.scan_time) AS scan_confirm_time,
                    COALESCE(sd.inspection_time, fo.qa_time, fo.inspection_time, fo.submission_time, d.create_time) AS inspection_time,
                    COALESCE(sd.inspection_time, fo.qa_time, fo.inspection_time, fo.submission_time, d.create_time) AS event_time,
                    1 AS quantity
                FROM mes_qms_fqc_sample_defect d
                INNER JOIN mes_qms_fqc_sample s ON s.deleted = b'0' AND s.id = d.sample_id
                    AND s.tenant_id = d.tenant_id
                INNER JOIN mes_qms_fqc_submission_detail sd ON sd.deleted = b'0' AND sd.id = s.submission_detail_id
                    AND sd.tenant_id = d.tenant_id
                INNER JOIN mes_qms_fqc_order fo ON fo.deleted = b'0' AND fo.id = d.fqc_id
                    AND fo.tenant_id = d.tenant_id
                LEFT JOIN mes_sfc_cut_round_report c ON c.deleted = b'0'
                    AND c.id = sd.cut_round_report_id
                    AND c.tenant_id = sd.tenant_id
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0'
                    AND po.id = COALESCE(sd.plan_id, fo.plan_order_id, c.plan_id)
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = c.source_slitting_slice_id
                LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = c.source_press_slot_report_id
                LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = b'0' AND a2.id = c.source_adhesive2_report_id
                LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = b'0' AND a2sl.id = a2.source_slitting_slice_id
                LEFT JOIN mes_qms_defect_code dc ON dc.deleted = b'0'
                    AND dc.type = 'ITEM'
                    AND (
                      (d.defect_code_id IS NOT NULL AND dc.id = d.defect_code_id)
                      OR (d.defect_code_id IS NULL AND dc.code = d.defect_code)
                    )
                    AND dc.tenant_id = d.tenant_id
                WHERE d.deleted = b'0'
                  AND fo.source_module = 'CUT_ROUND_FQC'
                  <if test="tenantId != null">
                  AND d.tenant_id = #{tenantId}
                  </if>
                UNION ALL
                SELECT
                    'FQC_SUBMISSION_DETAIL_NG' AS event_source,
                    'CUT_ROUND' AS process_code,
                    '裁切' AS process_name,
                    'FINAL_INSPECTION' AS inspection_type,
                    'mes_qms_fqc_submission_detail' AS source_table,
                    sd.id AS source_id,
                    fo.id AS inspection_id,
                    fo.fqc_no AS inspection_no,
                    COALESCE(NULLIF(sd.plan_no, ''), NULLIF(c.plan_no, ''), NULLIF(po.plan_no, '')) AS plan_no,
                    COALESCE(NULLIF(sl.source_batch_no, ''), NULLIF(psl.source_batch_no, ''), NULLIF(a2sl.source_batch_no, ''),
                             NULLIF(c.source_batch_no, ''), NULLIF(sd.parent_production_batch_no, ''),
                             NULLIF(po.parent_production_batch_no, ''), NULLIF(po.production_batch_no, ''),
                             NULLIF(po.batch_no, '')) AS mother_roll_batch_no,
                    COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(psl.source_production_batch_no, ''),
                             NULLIF(a2sl.source_production_batch_no, ''), NULLIF(c.source_production_batch_no, ''),
                             NULLIF(sd.parent_production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''),
                             NULLIF(sd.production_batch_no, ''), NULLIF(c.production_batch_no, '')) AS segment_batch_no,
                    COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, '')) AS scan_confirm_piece_no,
                    COALESCE(NULLIF(sd.production_batch_no, ''), NULLIF(c.production_batch_no, ''),
                             NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''),
                             NULLIF(a2sl.slice_serial_no, '')) AS process_piece_no,
                    COALESCE(NULLIF(sd.material_code, ''), NULLIF(c.material_code, ''), NULLIF(fo.material_code, ''),
                             NULLIF(po.material_code, '')) AS material_code,
                    COALESCE(NULLIF(sd.material_name, ''), NULLIF(c.material_name, ''), NULLIF(fo.material_name, ''),
                             NULLIF(po.material_name, '')) AS material_name,
                    COALESCE(NULLIF(sd.model_code, ''), NULLIF(c.model_code, ''), NULLIF(a2.model_code, ''),
                             NULLIF(ps.model_code, ''), NULLIF(fo.product_model, ''), NULLIF(po.model_code, ''),
                             NULLIF(po.mother_model_code, '')) AS model_code,
                    NULLIF(sd.defect_code, '') AS defect_code,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(sd.defect_name, ''), NULLIF(sd.defect_code, '')) AS defect_name,
                    NULLIF(dc.level, '') AS defect_level,
                    COALESCE(NULLIF(dc.name, ''), NULLIF(sd.defect_name, ''), NULLIF(sd.defect_code, ''), '未分类终检异常') AS inspection_category,
                    COALESCE(NULLIF(sd.inspector_name, ''), NULLIF(fo.qa_inspector_name, ''),
                             NULLIF(fo.inspector_name, ''), NULLIF(fo.submitter_name, '')) AS inspector_name,
                    COALESCE(NULLIF(sd.row_judgment, ''), NULLIF(fo.judgment, '')) AS check_result,
                    COALESCE(NULLIF(sd.ng_reason, ''), NULLIF(sd.remark, '')) AS remark,
                    NULL AS visual_result_json,
                    COALESCE(sl.scan_time, psl.scan_time, a2sl.scan_time) AS scan_confirm_time,
                    COALESCE(sd.inspection_time, fo.qa_time, fo.inspection_time, fo.submission_time, sd.update_time, sd.create_time) AS inspection_time,
                    COALESCE(sd.inspection_time, fo.qa_time, fo.inspection_time, fo.submission_time, sd.update_time, sd.create_time) AS event_time,
                    1 AS quantity
                FROM mes_qms_fqc_submission_detail sd
                INNER JOIN mes_qms_fqc_order fo ON fo.deleted = b'0' AND fo.id = sd.fqc_id
                    AND fo.tenant_id = sd.tenant_id
                LEFT JOIN mes_sfc_cut_round_report c ON c.deleted = b'0'
                    AND c.id = sd.cut_round_report_id
                    AND c.tenant_id = sd.tenant_id
                LEFT JOIN mes_pp_plan_order po ON po.deleted = b'0'
                    AND po.id = COALESCE(sd.plan_id, fo.plan_order_id, c.plan_id)
                LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = b'0' AND sl.id = c.source_slitting_slice_id
                LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = b'0' AND ps.id = c.source_press_slot_report_id
                LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = b'0' AND psl.id = ps.source_slitting_slice_id
                LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = b'0' AND a2.id = c.source_adhesive2_report_id
                LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = b'0' AND a2sl.id = a2.source_slitting_slice_id
                LEFT JOIN mes_qms_defect_code dc ON dc.deleted = b'0'
                    AND dc.type = 'ITEM'
                    AND dc.code = sd.defect_code
                    AND dc.tenant_id = sd.tenant_id
                WHERE sd.deleted = b'0'
                  AND fo.source_module = 'CUT_ROUND_FQC'
                  AND UPPER(COALESCE(sd.row_judgment, '')) = 'NG'
                  AND (NULLIF(sd.defect_code, '') IS NOT NULL OR NULLIF(sd.defect_name, '') IS NOT NULL)
                  AND NOT EXISTS (
                    SELECT 1
                    FROM mes_qms_fqc_sample_defect sd_defect
                    INNER JOIN mes_qms_fqc_sample sd_sample ON sd_sample.deleted = b'0'
                        AND sd_sample.id = sd_defect.sample_id
                        AND sd_sample.tenant_id = sd_defect.tenant_id
                    WHERE sd_defect.deleted = b'0'
                      AND sd_defect.tenant_id = sd.tenant_id
                      AND sd_sample.submission_detail_id = sd.id
                  )
                  <if test="tenantId != null">
                  AND sd.tenant_id = #{tenantId}
                  </if>
            ) event_rows
            WHERE event_time IS NOT NULL
              <if test="startTime != null">
              AND event_time &gt;= #{startTime}
              </if>
              <if test="endTime != null">
              AND event_time &lt; #{endTime}
              </if>
              <if test="processCode != null and processCode != '' and processCode != 'ALL'">
              AND process_code = #{processCode}
              </if>
              <if test="inspectionType != null and inspectionType != '' and inspectionType != 'ALL'">
              AND inspection_type = #{inspectionType}
              </if>
            ORDER BY event_time DESC, process_code ASC, scan_confirm_piece_no ASC, source_id ASC
            </script>
            """)
    List<QmsDefectCategoryAnalysisEventRow> selectEventRows(@Param("tenantId") Long tenantId,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime,
                                                            @Param("processCode") String processCode,
                                                            @Param("inspectionType") String inspectionType);
}
