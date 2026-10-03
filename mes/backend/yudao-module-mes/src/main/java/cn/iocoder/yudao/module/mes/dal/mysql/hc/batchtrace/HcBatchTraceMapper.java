package cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcBatchTraceMapper {

    String TRACE_SELECT = """
            SELECT
              r.id AS id,
              r.source_menu_code AS sourceType,
              CASE r.source_menu_code
                WHEN 'FORMULA_REPORT' THEN '配料报工'
                WHEN 'WET_REPORT' THEN '湿法报工'
                ELSE r.source_menu_code
              END AS sourceTypeName,
              CONCAT(r.source_menu_code, '-', r.id) AS bizNo,
              r.plan_id AS planId,
              r.plan_no AS planNo,
              r.plan_operation_id AS planOperationId,
              r.operation_code AS operationCode,
              r.operation_name AS operationName,
              r.work_center_name AS workCenterName,
              r.equipment_code AS equipmentCode,
              r.equipment_name AS equipmentName,
              r.material_code AS materialCode,
              r.material_name AS materialName,
              COALESCE(NULLIF(r.mother_model_code, ''), NULLIF(r.mother_model_name, '')) AS modelCode,
              COALESCE(NULLIF(r.parent_batch_no, ''), NULLIF(r.feed_batch_no, ''), NULLIF(r.batch_no, '')) AS sourceBatchNo,
              COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, '')) AS productionBatchNo,
              r.parent_production_batch_no AS parentProductionBatchNo,
              r.report_date AS reportDate,
              r.start_time AS startTime,
              r.end_time AS endTime,
              COALESCE(r.good_qty, r.feed_qty, r.scrap_qty) AS reportQty,
              r.report_uom AS reportUom,
              r.report_type AS reportType,
              r.feed_qty AS inputLength,
              r.good_qty AS outputLength,
              r.scrap_qty AS lossLength,
              NULL AS napSampleLength,
              NULL AS startPosition,
              NULL AS endPosition,
              NULL AS processLength,
              NULL AS slittingRemainingLength,
              NULL AS glueBoardModel,
              NULL AS glueBoardBatchNo,
              NULL AS productQualityStatus,
              NULL AS qualityLockReason,
              NULL AS sizeCode,
              NULL AS sizeName,
              NULL AS coaFlag,
              r.operation_status AS reportStatus,
              r.recorder_name AS recorderName,
              r.recorder_time AS recorderTime,
              r.confirmer_name AS confirmerName,
              r.confirmer_time AS confirmerTime,
              COALESCE(NULLIF(r.fai_no, ''), qf.fai_no) AS inspectionNo,
              COALESCE(NULLIF(r.fai_status, ''), qf.status) AS inspectionStatus,
              COALESCE(NULLIF(r.fai_judgment, ''), qf.judgment) AS inspectionResult,
              COALESCE(r.fai_apply_time, qf.submission_time, qf.operator_time) AS inspectionApplyTime,
              COALESCE(r.fai_return_time, qf.qa_time, qf.release_time) AS inspectionReturnTime,
              COALESCE(NULLIF(r.fai_reject_reason, ''), qf.remark) AS inspectionRemark,
              NULL AS selfCheck,
              NULL AS defectCode,
              r.extra_json AS extraJson,
              r.remark AS remark,
              r.create_time AS createTime
            FROM mes_sfc_operation_report r
            LEFT JOIN mes_qms_fai_order qf
              ON qf.id = r.fai_id
             AND qf.deleted = 0
            WHERE r.deleted = 0
              AND r.tenant_id = #{tenantId}
              AND r.source_menu_code IN ('FORMULA_REPORT', 'WET_REPORT')
            UNION ALL
            SELECT
              COALESCE(fa.id, f.id) AS id,
              CASE WHEN fa.id IS NULL THEN 'ROUGH_GRINDING_FIRST'
                   ELSE 'ROUGH_GRINDING_FIRST_ALLOCATION' END AS sourceType,
              CASE WHEN fa.id IS NULL THEN '磨皮一磨记录'
                   ELSE '磨皮一磨加工单元' END AS sourceTypeName,
              CASE WHEN fa.id IS NULL THEN CONCAT('GRIND1-', f.id)
                   ELSE CONCAT('GRIND1A-', fa.id) END AS bizNo,
              f.plan_id AS planId,
              f.plan_no AS planNo,
              f.plan_operation_id AS planOperationId,
              o.op_code AS operationCode,
              COALESCE(NULLIF(o.op_name, ''), '磨皮') AS operationName,
              f.work_center_name AS workCenterName,
              f.equipment_code AS equipmentCode,
              f.equipment_name AS equipmentName,
              p.material_code AS materialCode,
              p.material_name AS materialName,
              COALESCE(p.model_code, p.mother_model_code) AS modelCode,
              COALESCE(NULLIF(fa.mother_batch_no, ''), NULLIF(f.mother_batch_no, ''),
                       NULLIF(f.source_production_batch_no, '')) AS sourceBatchNo,
              COALESCE(NULLIF(fa.production_batch_no, ''), NULLIF(f.source_production_batch_no, ''),
                       NULLIF(f.mother_batch_no, '')) AS productionBatchNo,
              COALESCE(NULLIF(fa.mother_batch_no, ''), NULLIF(f.mother_batch_no, ''),
                       NULLIF(f.source_production_batch_no, '')) AS parentProductionBatchNo,
              f.report_date AS reportDate,
              COALESCE(fa.start_time, f.start_time) AS startTime,
              COALESCE(fa.end_time, f.end_time) AS endTime,
              COALESCE(f.process_length, fa.confirmed_length) AS reportQty,
              'm' AS reportUom,
              NULL AS reportType,
              NULL AS inputLength,
              COALESCE(f.output_length, fa.confirmed_length) AS outputLength,
              f.loss_length AS lossLength,
              f.nap_sample_length AS napSampleLength,
              fa.start_position AS startPosition,
              CASE WHEN fa.id IS NULL OR fa.start_position IS NULL OR fa.confirmed_length IS NULL THEN NULL
                   ELSE fa.start_position + fa.confirmed_length END AS endPosition,
              COALESCE(f.process_length, fa.confirmed_length) AS processLength,
              NULL AS slittingRemainingLength,
              NULL AS glueBoardModel,
              NULL AS glueBoardBatchNo,
              NULL AS productQualityStatus,
              NULL AS qualityLockReason,
              NULL AS sizeCode,
              NULL AS sizeName,
              NULL AS coaFlag,
              COALESCE(NULLIF(fa.detail_status, ''), NULLIF(f.detail_status, ''), NULLIF(f.row_status, '')) AS reportStatus,
              fa.operator_name AS recorderName,
              COALESCE(fa.create_time, f.create_time) AS recorderTime,
              NULL AS confirmerName,
              COALESCE(fa.update_time, f.update_time) AS confirmerTime,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              f.self_check AS selfCheck,
              f.defect_code AS defectCode,
              CASE WHEN fa.id IS NULL THEN NULL
                   ELSE JSON_OBJECT('allocationMode', 'FIRST_ALLOCATED', 'segmentMark', fa.segment_mark,
                                    'firstDetailId', fa.first_detail_id) END AS extraJson,
              CASE WHEN fa.id IS NULL THEN f.remark
                   ELSE CONCAT('一磨加工单元：', COALESCE(NULLIF(fa.segment_mark, ''), 'NONE')) END AS remark,
              COALESCE(fa.create_time, f.create_time) AS createTime
            FROM mes_sfc_grinding_first_detail f
            LEFT JOIN mes_sfc_grinding_first_allocation_detail fa
              ON fa.first_detail_id = f.id
             AND fa.tenant_id = f.tenant_id
             AND fa.deleted = 0
             AND COALESCE(fa.detail_status, 'ACTIVE') != 'VOID'
            LEFT JOIN mes_pp_plan_operation o
              ON o.id = f.plan_operation_id
             AND o.deleted = 0
            LEFT JOIN mes_pp_plan_order p
              ON p.id = f.plan_id
             AND p.deleted = 0
            WHERE f.deleted = 0
              AND f.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              s.id AS id,
              'ROUGH_GRINDING_SECOND' AS sourceType,
              '磨皮二磨记录' AS sourceTypeName,
              CONCAT('GRIND2-', s.id) AS bizNo,
              s.plan_id AS planId,
              s.plan_no AS planNo,
              s.plan_operation_id AS planOperationId,
              o.op_code AS operationCode,
              COALESCE(NULLIF(o.op_name, ''), '磨皮') AS operationName,
              s.work_center_name AS workCenterName,
              s.equipment_code AS equipmentCode,
              s.equipment_name AS equipmentName,
              p.material_code AS materialCode,
              p.material_name AS materialName,
              COALESCE(p.model_code, p.mother_model_code) AS modelCode,
              COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.mother_batch_no, '')) AS sourceBatchNo,
              COALESCE(NULLIF(s.production_batch_no, ''), NULLIF(s.confirmed_batch_no, ''), NULLIF(s.mother_batch_no, '')) AS productionBatchNo,
              s.parent_production_batch_no AS parentProductionBatchNo,
              s.report_date AS reportDate,
              s.start_time AS startTime,
              s.end_time AS endTime,
              s.output_length AS reportQty,
              'm' AS reportUom,
              NULL AS reportType,
              NULL AS inputLength,
              s.output_length AS outputLength,
              s.loss_length AS lossLength,
              s.nap_sample_length AS napSampleLength,
              s.start_position AS startPosition,
              NULL AS endPosition,
              s.process_length AS processLength,
              NULL AS slittingRemainingLength,
              NULL AS glueBoardModel,
              NULL AS glueBoardBatchNo,
              NULL AS productQualityStatus,
              NULL AS qualityLockReason,
              NULL AS sizeCode,
              NULL AS sizeName,
              NULL AS coaFlag,
              COALESCE(NULLIF(s.detail_status, ''), NULLIF(s.confirm_status, ''), NULLIF(s.row_status, '')) AS reportStatus,
              NULL AS recorderName,
              s.create_time AS recorderTime,
              s.confirm_operator_name AS confirmerName,
              s.confirm_time AS confirmerTime,
              s.inspection_no AS inspectionNo,
              s.inspection_status AS inspectionStatus,
              s.inspection_result AS inspectionResult,
              s.inspection_apply_time AS inspectionApplyTime,
              s.inspection_return_time AS inspectionReturnTime,
              s.inspection_reject_reason AS inspectionRemark,
              s.self_check AS selfCheck,
              s.defect_code AS defectCode,
              NULL AS extraJson,
              s.remark AS remark,
              s.create_time AS createTime
            FROM mes_sfc_grinding_second_detail s
            LEFT JOIN mes_pp_plan_operation o
              ON o.id = s.plan_operation_id
             AND o.deleted = 0
            LEFT JOIN mes_pp_plan_order p
              ON p.id = s.plan_id
             AND p.deleted = 0
            WHERE s.deleted = 0
              AND s.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              a.id AS id,
              'ADHESIVE_REPORT' AS sourceType,
              '粘胶1报工' AS sourceTypeName,
              CONCAT('ADH1-', a.id) AS bizNo,
              a.plan_id AS planId,
              a.plan_no AS planNo,
              a.plan_operation_id AS planOperationId,
              a.operation_code AS operationCode,
              COALESCE(NULLIF(a.operation_name, ''), '粘胶1') AS operationName,
              NULL AS workCenterName,
              NULL AS equipmentCode,
              NULL AS equipmentName,
              a.material_code AS materialCode,
              a.material_name AS materialName,
              a.model_code AS modelCode,
              COALESCE(NULLIF(a.source_production_batch_no, ''), NULLIF(a.source_batch_no, '')) AS sourceBatchNo,
              a.production_batch_no AS productionBatchNo,
              a.parent_production_batch_no AS parentProductionBatchNo,
              a.report_date AS reportDate,
              a.start_time AS startTime,
              a.end_time AS endTime,
              COALESCE(a.output_length, a.input_length) AS reportQty,
              'm' AS reportUom,
              NULL AS reportType,
              a.input_length AS inputLength,
              a.output_length AS outputLength,
              a.loss_length AS lossLength,
              a.nap_sample_length AS napSampleLength,
              a.start_position AS startPosition,
              a.end_position AS endPosition,
              NULL AS processLength,
              a.slitting_remaining_length AS slittingRemainingLength,
              NULL AS glueBoardModel,
              a.glue_board_batch_no AS glueBoardBatchNo,
              a.product_quality_status AS productQualityStatus,
              a.quality_lock_reason AS qualityLockReason,
              NULL AS sizeCode,
              NULL AS sizeName,
              NULL AS coaFlag,
              a.report_status AS reportStatus,
              a.recorder_name AS recorderName,
              a.recorder_time AS recorderTime,
              a.confirmer_name AS confirmerName,
              a.confirmer_time AS confirmerTime,
              a.fai_no AS inspectionNo,
              a.fai_status AS inspectionStatus,
              a.fai_judgment AS inspectionResult,
              a.fai_apply_time AS inspectionApplyTime,
              a.fai_return_time AS inspectionReturnTime,
              a.fai_reject_reason AS inspectionRemark,
              a.self_check AS selfCheck,
              a.defect_code AS defectCode,
              a.extra_json AS extraJson,
              a.remark AS remark,
              a.create_time AS createTime
            FROM mes_sfc_adhesive_report a
            WHERE a.deleted = 0
              AND a.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              sl.id AS id,
              'SLITTING_SLICE' AS sourceType,
              '分切片号记录' AS sourceTypeName,
              sl.slice_serial_no AS bizNo,
              sl.plan_id AS planId,
              sl.plan_no AS planNo,
              sl.plan_operation_id AS planOperationId,
              sl.operation_code AS operationCode,
              COALESCE(NULLIF(sl.operation_name, ''), '分切') AS operationName,
              NULL AS workCenterName,
              NULL AS equipmentCode,
              NULL AS equipmentName,
              p.material_code AS materialCode,
              p.material_name AS materialName,
              COALESCE(p.model_code, p.mother_model_code) AS modelCode,
              COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(sl.source_batch_no, '')) AS sourceBatchNo,
              sl.slice_serial_no AS productionBatchNo,
              COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(sl.source_batch_no, '')) AS parentProductionBatchNo,
              DATE(COALESCE(sl.scan_time, sl.last_print_time, sl.create_time)) AS reportDate,
              NULL AS startTime,
              sl.scan_time AS endTime,
              sl.slice_length AS reportQty,
              NULL AS reportUom,
              NULL AS reportType,
              sl.source_length AS inputLength,
              sl.slice_length AS outputLength,
              NULL AS lossLength,
              NULL AS napSampleLength,
              sl.start_position AS startPosition,
              sl.end_position AS endPosition,
              sl.slice_length AS processLength,
              NULL AS slittingRemainingLength,
              NULL AS glueBoardModel,
              NULL AS glueBoardBatchNo,
              NULL AS productQualityStatus,
              NULL AS qualityLockReason,
              sl.size_code AS sizeCode,
              sl.size_name AS sizeName,
              NULL AS coaFlag,
              sl.scan_status AS reportStatus,
              sl.scanner_name AS recorderName,
              sl.scan_time AS recorderTime,
              sl.scanner_name AS confirmerName,
              sl.scan_time AS confirmerTime,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              sl.self_check AS selfCheck,
              NULL AS defectCode,
              sl.visual_result_json AS extraJson,
              sl.remark AS remark,
              sl.create_time AS createTime
            FROM mes_sfc_slitting_slice_record sl
            LEFT JOIN mes_pp_plan_order p
              ON p.id = sl.plan_id
             AND p.deleted = 0
            WHERE sl.deleted = 0
              AND sl.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              ps.id AS id,
              'PRESS_SLOT_REPORT' AS sourceType,
              '压槽报工' AS sourceTypeName,
              CONCAT('PRESS-', ps.id) AS bizNo,
              ps.plan_id AS planId,
              ps.plan_no AS planNo,
              ps.plan_operation_id AS planOperationId,
              ps.operation_code AS operationCode,
              COALESCE(NULLIF(ps.operation_name, ''), '压槽') AS operationName,
              NULL AS workCenterName,
              NULL AS equipmentCode,
              NULL AS equipmentName,
              ps.material_code AS materialCode,
              ps.material_name AS materialName,
              ps.model_code AS modelCode,
              COALESCE(NULLIF(ps.source_production_batch_no, ''), NULLIF(ps.source_batch_no, '')) AS sourceBatchNo,
              ps.production_batch_no AS productionBatchNo,
              ps.parent_production_batch_no AS parentProductionBatchNo,
              ps.report_date AS reportDate,
              ps.start_time AS startTime,
              ps.end_time AS endTime,
              COALESCE(ps.output_length, ps.input_length) AS reportQty,
              'pcs' AS reportUom,
              NULL AS reportType,
              ps.input_length AS inputLength,
              ps.output_length AS outputLength,
              ps.loss_length AS lossLength,
              ps.nap_sample_length AS napSampleLength,
              ps.start_position AS startPosition,
              ps.end_position AS endPosition,
              NULL AS processLength,
              NULL AS slittingRemainingLength,
              NULL AS glueBoardModel,
              NULL AS glueBoardBatchNo,
              NULL AS productQualityStatus,
              NULL AS qualityLockReason,
              NULL AS sizeCode,
              NULL AS sizeName,
              CASE
                WHEN LOWER(COALESCE(ps.extra_json, '')) LIKE '%"coaflag"%true%'
                  OR LOWER(COALESCE(ps.extra_json, '')) LIKE '%"coaflag"%1%'
                  OR LOWER(COALESCE(ps.extra_json, '')) LIKE '%"coaflag"%y%'
                THEN 'Y'
                ELSE NULL
              END AS coaFlag,
              ps.report_status AS reportStatus,
              ps.recorder_name AS recorderName,
              ps.recorder_time AS recorderTime,
              ps.confirmer_name AS confirmerName,
              ps.confirmer_time AS confirmerTime,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              ps.self_check AS selfCheck,
              ps.defect_code AS defectCode,
              ps.extra_json AS extraJson,
              ps.remark AS remark,
              ps.create_time AS createTime
            FROM mes_sfc_press_slot_report ps
            WHERE ps.deleted = 0
              AND ps.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              a2.id AS id,
              'ADHESIVE2_REPORT' AS sourceType,
              '粘胶2报工' AS sourceTypeName,
              CONCAT('ADH2-', a2.id) AS bizNo,
              a2.plan_id AS planId,
              a2.plan_no AS planNo,
              a2.plan_operation_id AS planOperationId,
              a2.operation_code AS operationCode,
              COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS operationName,
              NULL AS workCenterName,
              NULL AS equipmentCode,
              NULL AS equipmentName,
              a2.material_code AS materialCode,
              a2.material_name AS materialName,
              a2.model_code AS modelCode,
              COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, '')) AS sourceBatchNo,
              a2.production_batch_no AS productionBatchNo,
              a2.parent_production_batch_no AS parentProductionBatchNo,
              a2.report_date AS reportDate,
              a2.start_time AS startTime,
              a2.end_time AS endTime,
              COALESCE(a2.output_length, a2.input_length) AS reportQty,
              'pcs' AS reportUom,
              NULL AS reportType,
              a2.input_length AS inputLength,
              a2.output_length AS outputLength,
              a2.loss_length AS lossLength,
              a2.nap_sample_length AS napSampleLength,
              a2.start_position AS startPosition,
              a2.end_position AS endPosition,
              NULL AS processLength,
              NULL AS slittingRemainingLength,
              a2.glue_board_model AS glueBoardModel,
              a2.glue_board_batch_no AS glueBoardBatchNo,
              a2.product_quality_status AS productQualityStatus,
              a2.quality_lock_reason AS qualityLockReason,
              NULL AS sizeCode,
              NULL AS sizeName,
              CASE
                WHEN LOWER(COALESCE(a2.extra_json, '')) LIKE '%"coaflag"%true%'
                  OR LOWER(COALESCE(a2.extra_json, '')) LIKE '%"coaflag"%1%'
                  OR LOWER(COALESCE(a2.extra_json, '')) LIKE '%"coaflag"%y%'
                  OR LOWER(COALESCE(psa.extra_json, '')) LIKE '%"coaflag"%true%'
                  OR LOWER(COALESCE(psa.extra_json, '')) LIKE '%"coaflag"%1%'
                  OR LOWER(COALESCE(psa.extra_json, '')) LIKE '%"coaflag"%y%'
                THEN 'Y'
                ELSE NULL
              END AS coaFlag,
              a2.report_status AS reportStatus,
              a2.recorder_name AS recorderName,
              a2.recorder_time AS recorderTime,
              a2.confirmer_name AS confirmerName,
              a2.confirmer_time AS confirmerTime,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              a2.self_check AS selfCheck,
              a2.defect_code AS defectCode,
              a2.extra_json AS extraJson,
              a2.remark AS remark,
              a2.create_time AS createTime
            FROM mes_sfc_adhesive2_report a2
            LEFT JOIN mes_sfc_press_slot_report psa
              ON psa.id = a2.source_press_slot_report_id
             AND psa.deleted = 0
            WHERE a2.deleted = 0
              AND a2.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              cr.id AS id,
              'CUT_ROUND_REPORT' AS sourceType,
              '裁切报工' AS sourceTypeName,
              CONCAT('CUT-', cr.id) AS bizNo,
              cr.plan_id AS planId,
              cr.plan_no AS planNo,
              cr.plan_operation_id AS planOperationId,
              cr.operation_code AS operationCode,
              COALESCE(NULLIF(cr.operation_name, ''), '裁切') AS operationName,
              NULL AS workCenterName,
              NULL AS equipmentCode,
              NULL AS equipmentName,
              cr.material_code AS materialCode,
              cr.material_name AS materialName,
              cr.model_code AS modelCode,
              COALESCE(NULLIF(cr.source_production_batch_no, ''), NULLIF(cr.source_batch_no, '')) AS sourceBatchNo,
              cr.production_batch_no AS productionBatchNo,
              cr.parent_production_batch_no AS parentProductionBatchNo,
              cr.report_date AS reportDate,
              cr.start_time AS startTime,
              cr.end_time AS endTime,
              COALESCE(cr.output_length, cr.input_length) AS reportQty,
              'pcs' AS reportUom,
              NULL AS reportType,
              cr.input_length AS inputLength,
              cr.output_length AS outputLength,
              NULL AS lossLength,
              NULL AS napSampleLength,
              NULL AS startPosition,
              NULL AS endPosition,
              NULL AS processLength,
              NULL AS slittingRemainingLength,
              NULL AS glueBoardModel,
              NULL AS glueBoardBatchNo,
              NULL AS productQualityStatus,
              NULL AS qualityLockReason,
              NULL AS sizeCode,
              NULL AS sizeName,
              CASE
                WHEN LOWER(COALESCE(cr.extra_json, '')) LIKE '%"coaflag"%true%'
                  OR LOWER(COALESCE(cr.extra_json, '')) LIKE '%"coaflag"%1%'
                  OR LOWER(COALESCE(cr.extra_json, '')) LIKE '%"coaflag"%y%'
                THEN 'Y'
                ELSE NULL
              END AS coaFlag,
              cr.report_status AS reportStatus,
              cr.recorder_name AS recorderName,
              cr.recorder_time AS recorderTime,
              cr.confirmer_name AS confirmerName,
              cr.confirmer_time AS confirmerTime,
              cr.inspection_task_no AS inspectionNo,
              cr.inspection_status AS inspectionStatus,
              cr.inspection_result AS inspectionResult,
              cr.recorder_time AS inspectionApplyTime,
              cr.inspection_time AS inspectionReturnTime,
              cr.inspection_remark AS inspectionRemark,
              cr.self_check AS selfCheck,
              cr.defect_code AS defectCode,
              cr.extra_json AS extraJson,
              cr.remark AS remark,
              cr.create_time AS createTime
            FROM mes_sfc_cut_round_report cr
            WHERE cr.deleted = 0
              AND cr.tenant_id = #{tenantId}
            """;

    @Select("""
            <script>
            SELECT records.*
            FROM (
            """ + TRACE_SELECT + """
            ) records
            WHERE (
                records.bizNo IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                    #{batchNo}
                </foreach>
                OR records.sourceBatchNo IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                    #{batchNo}
                </foreach>
                OR records.productionBatchNo IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                    #{batchNo}
                </foreach>
                OR records.parentProductionBatchNo IN
                <foreach collection="batchNos" item="batchNo" open="(" separator="," close=")">
                    #{batchNo}
                </foreach>
                OR (#{branchPrefix} IS NOT NULL AND #{branchPrefix} != '' AND (
                    records.bizNo LIKE CONCAT(#{branchPrefix}, '%')
                    OR records.sourceBatchNo LIKE CONCAT(#{branchPrefix}, '%')
                    OR records.productionBatchNo LIKE CONCAT(#{branchPrefix}, '%')
                    OR records.parentProductionBatchNo LIKE CONCAT(#{branchPrefix}, '%')
                ))
            )
            ORDER BY
              CASE records.sourceType
                WHEN 'FORMULA_REPORT' THEN 10
                WHEN 'WET_REPORT' THEN 20
                WHEN 'ROUGH_GRINDING_FIRST' THEN 30
                WHEN 'ROUGH_GRINDING_FIRST_ALLOCATION' THEN 30
                WHEN 'ROUGH_GRINDING_SECOND' THEN 40
                WHEN 'ADHESIVE_REPORT' THEN 50
                WHEN 'SLITTING_SLICE' THEN 60
                WHEN 'PRESS_SLOT_REPORT' THEN 70
                WHEN 'ADHESIVE2_REPORT' THEN 80
                WHEN 'CUT_ROUND_REPORT' THEN 90
                ELSE 999
              END,
              COALESCE(records.confirmerTime, records.endTime, records.recorderTime, records.startTime, records.createTime),
              records.id
            LIMIT 800
            </script>
            """)
    List<HcBatchTraceFactDTO> selectTraceFacts(@Param("batchNos") List<String> batchNos,
                                               @Param("branchPrefix") String branchPrefix,
                                               @Param("tenantId") Long tenantId);

    @Select("""
            <script>
            SELECT *
            FROM (
              SELECT
                f.id AS sourceId,
                'ROUGH_GRINDING_FIRST' AS sourceType,
                'SANDPAPER' AS materialType,
                '一磨砂纸' AS materialTypeName,
                NULL AS materialCode,
                NULL AS materialName,
                COALESCE(NULLIF(f.current_sandpaper_batch_no, ''), NULLIF(f.sandpaper_batch_no, '')) AS batchNo,
                CONVERT(CONCAT('累计寿命 ', COALESCE(CAST(f.sandpaper_life AS CHAR), '-'), ' m；累计天数 ', COALESCE(CAST(f.sandpaper_life_days AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_grinding_first_detail' AS sourceTable
              FROM mes_sfc_grinding_first_detail f
              WHERE f.deleted = 0
                AND f.tenant_id = #{tenantId}
                AND CONCAT('ROUGH_GRINDING_FIRST:', f.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                f.id AS sourceId,
                'ROUGH_GRINDING_FIRST' AS sourceType,
                'GUIDE_CLOTH' AS materialType,
                '一磨导布' AS materialTypeName,
                NULL AS materialCode,
                NULL AS materialName,
                f.current_guide_cloth_batch_no AS batchNo,
                CONVERT(CONCAT('导布状态ID ', COALESCE(CAST(f.guide_cloth_state_id AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_grinding_first_detail' AS sourceTable
              FROM mes_sfc_grinding_first_detail f
              WHERE f.deleted = 0
                AND f.tenant_id = #{tenantId}
                AND CONCAT('ROUGH_GRINDING_FIRST:', f.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                a.id AS sourceId,
                'ROUGH_GRINDING_FIRST_ALLOCATION' AS sourceType,
                'SANDPAPER' AS materialType,
                '一磨加工单元砂纸' AS materialTypeName,
                NULL AS materialCode,
                NULL AS materialName,
                a.sandpaper_batch_no AS batchNo,
                CONVERT(CONCAT('加工单元 ', COALESCE(NULLIF(a.segment_mark, ''), 'NONE'),
                       '；砂纸状态ID ', COALESCE(CAST(a.sandpaper_state_id AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_grinding_first_allocation_detail' AS sourceTable
              FROM mes_sfc_grinding_first_allocation_detail a
              WHERE a.deleted = 0
                AND a.tenant_id = #{tenantId}
                AND CONCAT('ROUGH_GRINDING_FIRST_ALLOCATION:', a.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                a.id AS sourceId,
                'ROUGH_GRINDING_FIRST_ALLOCATION' AS sourceType,
                'GUIDE_CLOTH' AS materialType,
                '一磨加工单元导布' AS materialTypeName,
                NULL AS materialCode,
                NULL AS materialName,
                a.guide_cloth_batch_no AS batchNo,
                CONVERT(CONCAT('加工单元 ', COALESCE(NULLIF(a.segment_mark, ''), 'NONE'),
                       '；导布状态ID ', COALESCE(CAST(a.guide_cloth_state_id AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_grinding_first_allocation_detail' AS sourceTable
              FROM mes_sfc_grinding_first_allocation_detail a
              WHERE a.deleted = 0
                AND a.tenant_id = #{tenantId}
                AND CONCAT('ROUGH_GRINDING_FIRST_ALLOCATION:', a.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                s.id AS sourceId,
                'ROUGH_GRINDING_SECOND' AS sourceType,
                'SANDPAPER' AS materialType,
                '二磨砂纸' AS materialTypeName,
                NULL AS materialCode,
                NULL AS materialName,
                COALESCE(NULLIF(s.current_sandpaper_batch_no, ''), NULLIF(s.sandpaper_batch_no, '')) AS batchNo,
                CONVERT(CONCAT('累计寿命 ', COALESCE(CAST(s.sandpaper_life AS CHAR), '-'), ' m；累计天数 ', COALESCE(CAST(s.sandpaper_life_days AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_grinding_second_detail' AS sourceTable
              FROM mes_sfc_grinding_second_detail s
              WHERE s.deleted = 0
                AND s.tenant_id = #{tenantId}
                AND CONCAT('ROUGH_GRINDING_SECOND:', s.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                s.id AS sourceId,
                'ROUGH_GRINDING_SECOND' AS sourceType,
                'GUIDE_CLOTH' AS materialType,
                '二磨导布' AS materialTypeName,
                NULL AS materialCode,
                NULL AS materialName,
                s.current_guide_cloth_batch_no AS batchNo,
                CONVERT(CONCAT('导布状态ID ', COALESCE(CAST(s.guide_cloth_state_id AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_grinding_second_detail' AS sourceTable
              FROM mes_sfc_grinding_second_detail s
              WHERE s.deleted = 0
                AND s.tenant_id = #{tenantId}
                AND CONCAT('ROUGH_GRINDING_SECOND:', s.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                a.id AS sourceId,
                'ADHESIVE_REPORT' AS sourceType,
                'GLUE_BOARD' AS materialType,
                '粘胶1胶板' AS materialTypeName,
                a.glue_board_material_code AS materialCode,
                NULL AS materialName,
                a.glue_board_batch_no AS batchNo,
                CONVERT(CONCAT('领用ID ', COALESCE(CAST(a.glue_board_usage_id AS CHAR), '-'), '；起位置 ', COALESCE(CAST(a.glue_board_start_position AS CHAR), '-'), ' m；使用 ', COALESCE(CAST(a.glue_board_use_length AS CHAR), '-'), ' m') USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_adhesive_report' AS sourceTable
              FROM mes_sfc_adhesive_report a
              WHERE a.deleted = 0
                AND a.tenant_id = #{tenantId}
                AND CONCAT('ADHESIVE_REPORT:', a.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                ps.id AS sourceId,
                'PRESS_SLOT_REPORT' AS sourceType,
                'PRESSURE_ROLLER' AS materialType,
                '压槽压辊' AS materialTypeName,
                ps.pressure_roller_material_code AS materialCode,
                NULL AS materialName,
                ps.pressure_roller_batch_no AS batchNo,
                CONVERT(CONCAT('压辊清洁累计片数 ', COALESCE(CAST(ps.roller_clean_accumulated_pcs AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_press_slot_report' AS sourceTable
              FROM mes_sfc_press_slot_report ps
              WHERE ps.deleted = 0
                AND ps.tenant_id = #{tenantId}
                AND CONCAT('PRESS_SLOT_REPORT:', ps.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                ps.id AS sourceId,
                'PRESS_SLOT_REPORT' AS sourceType,
                'BEARING' AS materialType,
                '压槽轴承' AS materialTypeName,
                ps.bearing_material_code AS materialCode,
                NULL AS materialName,
                ps.bearing_batch_no AS batchNo,
                CONVERT(CONCAT('轴承更换累计片数 ', COALESCE(CAST(ps.bearing_replace_accumulated_pcs AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_press_slot_report' AS sourceTable
              FROM mes_sfc_press_slot_report ps
              WHERE ps.deleted = 0
                AND ps.tenant_id = #{tenantId}
                AND CONCAT('PRESS_SLOT_REPORT:', ps.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                a2.id AS sourceId,
                'ADHESIVE2_REPORT' AS sourceType,
                'GLUE_BOARD' AS materialType,
                '粘胶2胶板' AS materialTypeName,
                a2.glue_board_material_code AS materialCode,
                a2.glue_board_model AS materialName,
                a2.glue_board_batch_no AS batchNo,
                CONVERT(CONCAT('领用ID ', COALESCE(CAST(a2.glue_board_usage_id AS CHAR), '-'), '；起位置 ', COALESCE(CAST(a2.glue_board_start_position AS CHAR), '-'), ' m；使用 ', COALESCE(CAST(a2.glue_board_use_length AS CHAR), '-'), ' m') USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_adhesive2_report' AS sourceTable
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND a2.tenant_id = #{tenantId}
                AND CONCAT('ADHESIVE2_REPORT:', a2.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                cr.id AS sourceId,
                'CUT_ROUND_REPORT' AS sourceType,
                'BLADE' AS materialType,
                '裁切刀片' AS materialTypeName,
                cr.blade_material_code AS materialCode,
                NULL AS materialName,
                cr.blade_batch_no AS batchNo,
                CONVERT(CONCAT('刀片使用次数 ', COALESCE(CAST(cr.blade_use_count AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_cut_round_report' AS sourceTable
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0
                AND cr.tenant_id = #{tenantId}
                AND CONCAT('CUT_ROUND_REPORT:', cr.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
              UNION ALL
              SELECT
                cr.id AS sourceId,
                'CUT_ROUND_REPORT' AS sourceType,
                'FELT' AS materialType,
                '裁切毛毡' AS materialTypeName,
                cr.felt_material_code AS materialCode,
                NULL AS materialName,
                cr.felt_batch_no AS batchNo,
                CONVERT(CONCAT('毛毡使用次数 ', COALESCE(CAST(cr.felt_use_count AS CHAR), '-')) USING utf8mb4) COLLATE utf8mb4_bin AS usageInfo,
                'mes_sfc_cut_round_report' AS sourceTable
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0
                AND cr.tenant_id = #{tenantId}
                AND CONCAT('CUT_ROUND_REPORT:', cr.id) IN
                <foreach collection="sourceKeys" item="sourceKey" open="(" separator="," close=")">
                    #{sourceKey}
                </foreach>
            ) aux
            </script>
            """)
    List<HcBatchTraceAuxiliaryDTO> selectAuxiliaryBySourceKeys(@Param("sourceKeys") List<String> sourceKeys,
                                                               @Param("tenantId") Long tenantId);

}
