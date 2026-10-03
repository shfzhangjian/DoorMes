package cn.iocoder.yudao.module.mes.dal.mysql.hc.scanpreview;

import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scanpreview.vo.HcScanPreviewRecordRespVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcScanPreviewMapper {

    String BASE_SELECT = """
            SELECT
              r.id AS id,
              r.id AS operationReportId,
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
              r.operation_status AS reportStatus,
              r.recorder_name AS recorderName,
              r.recorder_time AS recorderTime,
              r.confirmer_name AS confirmerName,
              r.confirmer_time AS confirmerTime,
              NULL AS printStatus,
              NULL AS printCount,
              NULL AS lastPrintTime,
              NULL AS scanStatus,
              COALESCE(NULLIF(r.fai_no, ''), qf.fai_no) AS inspectionNo,
              COALESCE(NULLIF(r.fai_status, ''), qf.status) AS inspectionStatus,
              COALESCE(NULLIF(r.fai_judgment, ''), qf.judgment) AS inspectionResult,
              COALESCE(r.fai_apply_time, qf.submission_time, qf.operator_time) AS inspectionApplyTime,
              COALESCE(r.fai_return_time, qf.qa_time, qf.release_time) AS inspectionReturnTime,
              COALESCE(NULLIF(r.fai_reject_reason, ''), qf.remark) AS inspectionRemark,
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
              NULL AS operationReportId,
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
              COALESCE(NULLIF(fa.detail_status, ''), NULLIF(f.detail_status, ''), NULLIF(f.row_status, '')) AS reportStatus,
              fa.operator_name AS recorderName,
              COALESCE(fa.create_time, f.create_time) AS recorderTime,
              NULL AS confirmerName,
              COALESCE(fa.update_time, f.update_time) AS confirmerTime,
              NULL AS printStatus,
              NULL AS printCount,
              NULL AS lastPrintTime,
              NULL AS scanStatus,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              CASE WHEN fa.id IS NULL THEN f.remark
                   ELSE CONCAT('一磨加工单元：', COALESCE(NULLIF(fa.segment_mark, ''), 'NONE')) END AS remark,
              COALESCE(fa.create_time, f.create_time) AS createTime
            FROM mes_sfc_grinding_first_detail f
            LEFT JOIN mes_sfc_grinding_first_allocation_detail fa
              ON fa.first_detail_id = f.id
             AND fa.tenant_id = f.tenant_id
             AND fa.deleted = 0
             AND COALESCE(fa.detail_status, 'ACTIVE') <> 'VOID'
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
              NULL AS operationReportId,
              'ROUGH_GRINDING_SECOND' AS sourceType,
              '磨皮二磨记录' AS sourceTypeName,
              CONCAT('GRIND2-', s.id) AS bizNo,
              s.plan_id AS planId,
              s.plan_no AS planNo,
              s.plan_operation_id AS planOperationId,
              o.op_code AS operationCode,
              COALESCE(NULLIF(o.op_name, ''), '磨皮') AS operationName,
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
              COALESCE(NULLIF(s.detail_status, ''), NULLIF(s.confirm_status, ''), NULLIF(s.row_status, '')) AS reportStatus,
              NULL AS recorderName,
              s.create_time AS recorderTime,
              s.confirm_operator_name AS confirmerName,
              s.confirm_time AS confirmerTime,
              s.print_status AS printStatus,
              s.print_count AS printCount,
              s.last_print_time AS lastPrintTime,
              s.confirm_status AS scanStatus,
              s.inspection_no AS inspectionNo,
              s.inspection_status AS inspectionStatus,
              s.inspection_result AS inspectionResult,
              s.inspection_apply_time AS inspectionApplyTime,
              s.inspection_return_time AS inspectionReturnTime,
              s.inspection_reject_reason AS inspectionRemark,
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
              NULL AS operationReportId,
              'ADHESIVE_REPORT' AS sourceType,
              '粘胶1报工' AS sourceTypeName,
              CONCAT('ADH1-', a.id) AS bizNo,
              a.plan_id AS planId,
              a.plan_no AS planNo,
              a.plan_operation_id AS planOperationId,
              a.operation_code AS operationCode,
              COALESCE(NULLIF(a.operation_name, ''), '粘胶1') AS operationName,
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
              a.report_status AS reportStatus,
              a.recorder_name AS recorderName,
              a.recorder_time AS recorderTime,
              a.confirmer_name AS confirmerName,
              a.confirmer_time AS confirmerTime,
              NULL AS printStatus,
              NULL AS printCount,
              NULL AS lastPrintTime,
              a.report_status AS scanStatus,
              a.fai_no AS inspectionNo,
              a.fai_status AS inspectionStatus,
              a.fai_judgment AS inspectionResult,
              a.fai_apply_time AS inspectionApplyTime,
              a.fai_return_time AS inspectionReturnTime,
              a.fai_reject_reason AS inspectionRemark,
              a.remark AS remark,
              a.create_time AS createTime
            FROM mes_sfc_adhesive_report a
            WHERE a.deleted = 0
              AND a.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              sl.id AS id,
              NULL AS operationReportId,
              'SLITTING_SLICE' AS sourceType,
              '分切片号记录' AS sourceTypeName,
              sl.slice_serial_no AS bizNo,
              sl.plan_id AS planId,
              sl.plan_no AS planNo,
              sl.plan_operation_id AS planOperationId,
              sl.operation_code AS operationCode,
              COALESCE(NULLIF(sl.operation_name, ''), '分切') AS operationName,
              p.material_code AS materialCode,
              p.material_name AS materialName,
              COALESCE(p.model_code, p.mother_model_code) AS modelCode,
              COALESCE(NULLIF(sl.source_production_batch_no, ''), NULLIF(sl.source_batch_no, '')) AS sourceBatchNo,
              sl.slice_serial_no AS productionBatchNo,
              sl.source_batch_no AS parentProductionBatchNo,
              DATE(COALESCE(sl.scan_time, sl.last_print_time, sl.create_time)) AS reportDate,
              NULL AS startTime,
              sl.scan_time AS endTime,
              sl.slice_length AS reportQty,
              'pcs' AS reportUom,
              sl.scan_status AS reportStatus,
              sl.scanner_name AS recorderName,
              sl.scan_time AS recorderTime,
              sl.scanner_name AS confirmerName,
              sl.scan_time AS confirmerTime,
              sl.print_status AS printStatus,
              sl.print_count AS printCount,
              sl.last_print_time AS lastPrintTime,
              sl.scan_status AS scanStatus,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
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
              NULL AS operationReportId,
              'PRESS_SLOT_REPORT' AS sourceType,
              '压槽报工' AS sourceTypeName,
              CONCAT('PRESS-', ps.id) AS bizNo,
              ps.plan_id AS planId,
              ps.plan_no AS planNo,
              ps.plan_operation_id AS planOperationId,
              ps.operation_code AS operationCode,
              COALESCE(NULLIF(ps.operation_name, ''), '压槽') AS operationName,
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
              ps.report_status AS reportStatus,
              ps.recorder_name AS recorderName,
              ps.recorder_time AS recorderTime,
              ps.confirmer_name AS confirmerName,
              ps.confirmer_time AS confirmerTime,
              NULL AS printStatus,
              NULL AS printCount,
              NULL AS lastPrintTime,
              ps.report_status AS scanStatus,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              ps.remark AS remark,
              ps.create_time AS createTime
            FROM mes_sfc_press_slot_report ps
            WHERE ps.deleted = 0
              AND ps.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              a2.id AS id,
              NULL AS operationReportId,
              'ADHESIVE2_REPORT' AS sourceType,
              '粘胶2报工' AS sourceTypeName,
              CONCAT('ADH2-', a2.id) AS bizNo,
              a2.plan_id AS planId,
              a2.plan_no AS planNo,
              a2.plan_operation_id AS planOperationId,
              a2.operation_code AS operationCode,
              COALESCE(NULLIF(a2.operation_name, ''), '粘胶2') AS operationName,
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
              a2.report_status AS reportStatus,
              a2.recorder_name AS recorderName,
              a2.recorder_time AS recorderTime,
              a2.confirmer_name AS confirmerName,
              a2.confirmer_time AS confirmerTime,
              NULL AS printStatus,
              NULL AS printCount,
              NULL AS lastPrintTime,
              a2.report_status AS scanStatus,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              a2.remark AS remark,
              a2.create_time AS createTime
            FROM mes_sfc_adhesive2_report a2
            WHERE a2.deleted = 0
              AND a2.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              cr.id AS id,
              NULL AS operationReportId,
              'CUT_ROUND_REPORT' AS sourceType,
              '裁切报工' AS sourceTypeName,
              CONCAT('CUT-', cr.id) AS bizNo,
              cr.plan_id AS planId,
              cr.plan_no AS planNo,
              cr.plan_operation_id AS planOperationId,
              cr.operation_code AS operationCode,
              COALESCE(NULLIF(cr.operation_name, ''), '裁切') AS operationName,
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
              cr.report_status AS reportStatus,
              cr.recorder_name AS recorderName,
              cr.recorder_time AS recorderTime,
              cr.confirmer_name AS confirmerName,
              cr.confirmer_time AS confirmerTime,
              NULL AS printStatus,
              NULL AS printCount,
              NULL AS lastPrintTime,
              cr.report_status AS scanStatus,
              cr.inspection_task_no AS inspectionNo,
              cr.inspection_status AS inspectionStatus,
              cr.inspection_result AS inspectionResult,
              cr.recorder_time AS inspectionApplyTime,
              cr.inspection_time AS inspectionReturnTime,
              cr.inspection_remark AS inspectionRemark,
              cr.remark AS remark,
              cr.create_time AS createTime
            FROM mes_sfc_cut_round_report cr
            WHERE cr.deleted = 0
              AND cr.tenant_id = #{tenantId}
            UNION ALL
            SELECT
              pk.id AS id,
              NULL AS operationReportId,
              'PACKAGING_REPORT' AS sourceType,
              '包装报工' AS sourceTypeName,
              CONCAT('PACK-', pk.id) AS bizNo,
              pk.plan_id AS planId,
              pk.plan_no AS planNo,
              pk.plan_operation_id AS planOperationId,
              pk.operation_code AS operationCode,
              COALESCE(NULLIF(pk.operation_name, ''), '包装') AS operationName,
              pk.material_code AS materialCode,
              pk.material_name AS materialName,
              pk.model_code AS modelCode,
              pk.batch_no AS sourceBatchNo,
              COALESCE(NULLIF(pk.production_batch_no, ''), NULLIF(pk.batch_no, '')) AS productionBatchNo,
              pk.batch_no AS parentProductionBatchNo,
              pk.package_date AS reportDate,
              NULL AS startTime,
              pk.confirmer_time AS endTime,
              COALESCE(pk.inbound_piece_count, pk.outer_piece_count, pk.inner_piece_count) AS reportQty,
              'pcs' AS reportUom,
              pk.report_status AS reportStatus,
              pk.recorder_name AS recorderName,
              pk.recorder_time AS recorderTime,
              pk.confirmer_name AS confirmerName,
              pk.confirmer_time AS confirmerTime,
              NULL AS printStatus,
              NULL AS printCount,
              NULL AS lastPrintTime,
              pk.report_status AS scanStatus,
              NULL AS inspectionNo,
              NULL AS inspectionStatus,
              NULL AS inspectionResult,
              NULL AS inspectionApplyTime,
              NULL AS inspectionReturnTime,
              NULL AS inspectionRemark,
              pk.remark AS remark,
              pk.create_time AS createTime
            FROM mes_sfc_pack_report pk
            WHERE pk.deleted = 0
              AND pk.tenant_id = #{tenantId}
            """;

    String FILTER_SQL = """
            WHERE (#{req.keyword} IS NULL OR #{req.keyword} = ''
                OR records.bizNo LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.planNo LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.operationName LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.materialCode LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.materialName LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.modelCode LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.sourceBatchNo LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.productionBatchNo LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.parentProductionBatchNo LIKE CONCAT('%', #{req.keyword}, '%')
                OR records.inspectionNo LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.sourceType} IS NULL OR #{req.sourceType} = ''
                OR records.sourceType = #{req.sourceType})
              AND (#{req.operationName} IS NULL OR #{req.operationName} = ''
                OR records.operationName LIKE CONCAT('%', #{req.operationName}, '%'))
              AND (#{req.reportDateStart} IS NULL OR records.reportDate >= #{req.reportDateStart})
              AND (#{req.reportDateEnd} IS NULL OR records.reportDate <= #{req.reportDateEnd})
              AND (#{req.onlyWithInspection} IS NULL OR #{req.onlyWithInspection} = 0
                OR (records.inspectionNo IS NOT NULL AND records.inspectionNo != ''))
            """;

    @Select("SELECT COUNT(1) FROM (" + BASE_SELECT + ") records " + FILTER_SQL)
    Long countPage(@Param("req") HcScanPreviewPageReqVO reqVO,
                   @Param("tenantId") Long tenantId);

    @Select("SELECT records.* FROM (" + BASE_SELECT + ") records " + FILTER_SQL + """
            ORDER BY COALESCE(records.confirmerTime, records.recorderTime, records.endTime, records.startTime, records.createTime) DESC,
                     records.id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<HcScanPreviewRecordRespVO> selectPage(@Param("req") HcScanPreviewPageReqVO reqVO,
                                               @Param("tenantId") Long tenantId,
                                               @Param("offset") Integer offset,
                                               @Param("limit") Integer limit);

}
