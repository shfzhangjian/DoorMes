package cn.iocoder.yudao.module.mes.dal.mysql.hc.sliceadjust;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.sliceadjust.HcSliceAdjustRecordDO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HcSliceAdjustMapper extends BaseMapperX<HcSliceAdjustRecordDO> {

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT r.slice_serial_no AS sliceNo,
                       COALESCE(NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, ''), NULLIF(r.slice_serial_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'SLITTING') AS processCode,
                       COALESCE(r.operation_name, '分切') AS processName,
                       10 AS stageSort,
                       'mes_sfc_slitting_slice_record' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       r.size_code AS modelCode,
                       r.scan_status AS statusText,
                       r.self_check AS resultText,
                       COALESCE(r.scan_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_slitting_slice_record r
                 WHERE r.deleted = 0 AND r.slice_serial_no IS NOT NULL AND r.slice_serial_no &lt;&gt; ''
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'PRESS_SLOT') AS processCode,
                       COALESCE(r.operation_name, '压槽') AS processName,
                       20 AS stageSort,
                       'mes_sfc_press_slot_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       r.report_status AS statusText,
                       r.self_check AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_press_slot_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IS NOT NULL AND r.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'ADHESIVE') AS processCode,
                       COALESCE(r.operation_name, '粘胶') AS processName,
                       30 AS stageSort,
                       'mes_sfc_adhesive_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       r.report_status AS statusText,
                       COALESCE(r.product_quality_status, r.self_check) AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_adhesive_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IS NOT NULL AND r.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'ADHESIVE2') AS processCode,
                       COALESCE(r.operation_name, '粘胶2') AS processName,
                       35 AS stageSort,
                       'mes_sfc_adhesive2_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       r.report_status AS statusText,
                       COALESCE(r.product_quality_status, r.self_check) AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_adhesive2_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IS NOT NULL AND r.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'CUT_ROUND') AS processCode,
                       COALESCE(r.operation_name, '裁切') AS processName,
                       40 AS stageSort,
                       'mes_sfc_cut_round_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       COALESCE(r.inspection_status, r.report_status) AS statusText,
                       COALESCE(r.inspection_result, r.self_check) AS resultText,
                       COALESCE(r.inspection_time, r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_cut_round_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IS NOT NULL AND r.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT d.production_batch_no AS sliceNo,
                       d.parent_production_batch_no AS segmentBatchNo,
                       'CUT_ROUND_FQC' AS processCode,
                       '裁切检验' AS processName,
                       45 AS stageSort,
                       'mes_sfc_cut_round_inspection_detail' AS sourceTable,
                       d.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       d.material_code AS materialCode,
                       d.material_name AS materialName,
                       d.model_code AS modelCode,
                       d.fqc_status AS statusText,
                       d.inspection_result AS resultText,
                       COALESCE(d.inspection_time, d.update_time, d.create_time) AS reportTime
                  FROM mes_sfc_cut_round_inspection_detail d
                 WHERE d.deleted = 0 AND d.production_batch_no IS NOT NULL AND d.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT d.production_batch_no AS sliceNo,
                       d.parent_production_batch_no AS segmentBatchNo,
                       'FQC_SUBMISSION' AS processCode,
                       'FQC送检' AS processName,
                       46 AS stageSort,
                       'mes_qms_fqc_submission_detail' AS sourceTable,
                       d.id AS sourceId,
                       d.plan_id AS planId,
                       d.plan_no AS planNo,
                       d.plan_operation_id AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       NULL AS statusText,
                       NULL AS resultText,
                       COALESCE(d.inspection_time, d.update_time, d.create_time) AS reportTime
                  FROM mes_qms_fqc_submission_detail d
                 WHERE d.deleted = 0 AND d.production_batch_no IS NOT NULL AND d.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT i.production_batch_no AS sliceNo,
                       i.parent_production_batch_no AS segmentBatchNo,
                       'FQC_ITEM' AS processCode,
                       'FQC检验项' AS processName,
                       47 AS stageSort,
                       'mes_qms_fqc_item' AS sourceTable,
                       i.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       i.input_status AS statusText,
                       COALESCE(i.qa_result, i.operator_result, i.item_result) AS resultText,
                       COALESCE(i.qa_time, i.operator_time, i.update_time, i.create_time) AS reportTime
                  FROM mes_qms_fqc_item i
                 WHERE i.deleted = 0 AND i.production_batch_no IS NOT NULL AND i.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT s.production_batch_no AS sliceNo,
                       s.parent_production_batch_no AS segmentBatchNo,
                       'FQC_SAMPLE' AS processCode,
                       'FQC样本' AS processName,
                       48 AS stageSort,
                       'mes_qms_fqc_sample' AS sourceTable,
                       s.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       NULL AS statusText,
                       s.sample_result AS resultText,
                       COALESCE(s.input_time, s.update_time, s.create_time) AS reportTime
                  FROM mes_qms_fqc_sample s
                 WHERE s.deleted = 0 AND s.production_batch_no IS NOT NULL AND s.production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT o.product_batch_no AS sliceNo,
                       o.batch_no AS segmentBatchNo,
                       'FQC_ORDER' AS processCode,
                       'FQC检验单' AS processName,
                       49 AS stageSort,
                       'mes_qms_fqc_order' AS sourceTable,
                       o.id AS sourceId,
                       o.plan_order_id AS planId,
                       o.work_order_no AS planNo,
                       NULL AS planOperationId,
                       o.operation_code AS operationCode,
                       o.operation_name AS operationName,
                       o.material_code AS materialCode,
                       o.material_name AS materialName,
                       o.product_model AS modelCode,
                       o.status AS statusText,
                       COALESCE(o.release_result, o.judgment) AS resultText,
                       COALESCE(o.release_time, o.qa_time, o.inspection_time, o.submission_time, o.update_time, o.create_time) AS reportTime
                  FROM mes_qms_fqc_order o
                 WHERE o.deleted = 0 AND o.product_batch_no IS NOT NULL AND o.product_batch_no &lt;&gt; ''
                UNION ALL
                SELECT s.matched_production_batch_no AS sliceNo,
                       NULL AS segmentBatchNo,
                       'FQC_SCAN' AS processCode,
                       'FQC扫码匹配' AS processName,
                       49 AS stageSort,
                       'mes_qms_fqc_scan_record' AS sourceTable,
                       s.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       s.match_result AS statusText,
                       NULL AS resultText,
                       COALESCE(s.scan_time, s.update_time, s.create_time) AS reportTime
                  FROM mes_qms_fqc_scan_record s
                 WHERE s.deleted = 0 AND s.matched_production_batch_no IS NOT NULL AND s.matched_production_batch_no &lt;&gt; ''
                UNION ALL
                SELECT i.slice_batch_no AS sliceNo,
                       COALESCE(NULLIF(u.batch_no, ''), NULLIF(i.production_batch_no, '')) AS segmentBatchNo,
                       'PACKAGING' AS processCode,
                       '内包装' AS processName,
                       50 AS stageSort,
                       'mes_sfc_inner_pack_unit_item' AS sourceTable,
                       i.id AS sourceId,
                       i.plan_id AS planId,
                       i.plan_no AS planNo,
                       i.plan_operation_id AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       u.unit_status AS statusText,
                       i.quality_status AS resultText,
                       COALESCE(i.scan_time, i.update_time, i.create_time) AS reportTime
                  FROM mes_sfc_inner_pack_unit_item i
                  LEFT JOIN mes_sfc_inner_pack_unit u ON u.deleted = 0 AND u.id = i.inner_unit_id
                 WHERE i.deleted = 0 AND i.slice_batch_no IS NOT NULL AND i.slice_batch_no &lt;&gt; ''
                UNION ALL
                SELECT s.slice_batch_no AS sliceNo,
                       s.batch_no AS segmentBatchNo,
                       'FG_STOCK' AS processCode,
                       '成品库存' AS processName,
                       60 AS stageSort,
                       'mes_inv_finished_stock' AS sourceTable,
                       s.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       s.material_code AS materialCode,
                       s.material_name AS materialName,
                       s.model_code AS modelCode,
                       s.stock_status AS statusText,
                       s.quality_status AS resultText,
                       COALESCE(s.inbound_time, s.update_time, s.create_time) AS reportTime
                  FROM mes_inv_finished_stock s
                 WHERE s.deleted = 0 AND s.slice_batch_no IS NOT NULL AND s.slice_batch_no &lt;&gt; ''
                UNION ALL
                SELECT p.actual_slice_batch_no AS sliceNo,
                       COALESCE(NULLIF(p.batch_no, ''), NULLIF(p.slice_batch_no, '')) AS segmentBatchNo,
                       'SHIPPING_PICK' AS processCode,
                       '发货拣配' AS processName,
                       70 AS stageSort,
                       'mes_inv_fg_shipping_pick_item' AS sourceTable,
                       p.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       p.lock_status AS statusText,
                       p.quality_status AS resultText,
                       COALESCE(p.lock_time, p.update_time, p.create_time) AS reportTime
                  FROM mes_inv_fg_shipping_pick_item p
                 WHERE p.deleted = 0 AND p.actual_slice_batch_no IS NOT NULL AND p.actual_slice_batch_no &lt;&gt; ''
                UNION ALL
                SELECT o.slice_batch_no AS sliceNo,
                       o.batch_no AS segmentBatchNo,
                       'FG_OUTBOUND' AS processCode,
                       '成品出库装箱' AS processName,
                       80 AS stageSort,
                       'mes_inv_fg_outbound_box_item' AS sourceTable,
                       o.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       NULL AS statusText,
                       o.quality_status AS resultText,
                       COALESCE(o.scan_time, o.update_time, o.create_time) AS reportTime
                  FROM mes_inv_fg_outbound_box_item o
                 WHERE o.deleted = 0 AND o.slice_batch_no IS NOT NULL AND o.slice_batch_no &lt;&gt; ''
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''), NULLIF(r.batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'OPERATION_REPORT') AS processCode,
                       COALESCE(r.operation_name, '通用报工') AS processName,
                       COALESCE(r.operation_seq, 0) AS stageSort,
                       'mes_sfc_operation_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.mother_model_code AS modelCode,
                       r.operation_status AS statusText,
                       NULL AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_operation_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IS NOT NULL AND r.production_batch_no &lt;&gt; ''
            ) t
            WHERE 1 = 1
            <if test="segmentBatchNo != null and segmentBatchNo != ''">
              AND (t.segmentBatchNo = #{segmentBatchNo}
                   OR t.segmentBatchNo LIKE CONCAT(#{segmentBatchNo}, '-J%'))
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                t.sliceNo LIKE CONCAT('%', #{keyword}, '%')
                OR t.segmentBatchNo LIKE CONCAT('%', #{keyword}, '%')
                OR t.planNo LIKE CONCAT('%', #{keyword}, '%')
                OR t.materialCode LIKE CONCAT('%', #{keyword}, '%')
                OR t.materialName LIKE CONCAT('%', #{keyword}, '%')
                OR t.modelCode LIKE CONCAT('%', #{keyword}, '%')
                OR t.processName LIKE CONCAT('%', #{keyword}, '%')
              )
            </if>
            ORDER BY t.segmentBatchNo DESC, t.sliceNo ASC, t.stageSort DESC, t.reportTime DESC
            LIMIT 500
            </script>
            """)
    List<HcSliceAdjustTraceRow> selectTraceRows(@Param("segmentBatchNo") String segmentBatchNo,
                                                @Param("keyword") String keyword);

    @Select("""
            <script>
            SELECT *
            FROM (
                SELECT r.slice_serial_no AS sliceNo,
                       COALESCE(NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, ''), NULLIF(r.slice_serial_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'SLITTING') AS processCode,
                       COALESCE(r.operation_name, '分切') AS processName,
                       10 AS stageSort,
                       'mes_sfc_slitting_slice_record' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       r.size_code AS modelCode,
                       r.scan_status AS statusText,
                       r.self_check AS resultText,
                       COALESCE(r.scan_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_slitting_slice_record r
                 WHERE r.deleted = 0 AND r.slice_serial_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'PRESS_SLOT') AS processCode,
                       COALESCE(r.operation_name, '压槽') AS processName,
                       20 AS stageSort,
                       'mes_sfc_press_slot_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       r.report_status AS statusText,
                       r.self_check AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_press_slot_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'ADHESIVE') AS processCode,
                       COALESCE(r.operation_name, '粘胶') AS processName,
                       30 AS stageSort,
                       'mes_sfc_adhesive_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       r.report_status AS statusText,
                       COALESCE(r.product_quality_status, r.self_check) AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_adhesive_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'ADHESIVE2') AS processCode,
                       COALESCE(r.operation_name, '粘胶2') AS processName,
                       35 AS stageSort,
                       'mes_sfc_adhesive2_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       r.report_status AS statusText,
                       COALESCE(r.product_quality_status, r.self_check) AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_adhesive2_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.source_production_batch_no, ''), NULLIF(r.source_batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'CUT_ROUND') AS processCode,
                       COALESCE(r.operation_name, '裁切') AS processName,
                       40 AS stageSort,
                       'mes_sfc_cut_round_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.model_code AS modelCode,
                       COALESCE(r.inspection_status, r.report_status) AS statusText,
                       COALESCE(r.inspection_result, r.self_check) AS resultText,
                       COALESCE(r.inspection_time, r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_cut_round_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT d.production_batch_no AS sliceNo,
                       d.parent_production_batch_no AS segmentBatchNo,
                       'CUT_ROUND_FQC' AS processCode,
                       '裁切检验' AS processName,
                       45 AS stageSort,
                       'mes_sfc_cut_round_inspection_detail' AS sourceTable,
                       d.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       d.material_code AS materialCode,
                       d.material_name AS materialName,
                       d.model_code AS modelCode,
                       d.fqc_status AS statusText,
                       d.inspection_result AS resultText,
                       COALESCE(d.inspection_time, d.update_time, d.create_time) AS reportTime
                  FROM mes_sfc_cut_round_inspection_detail d
                 WHERE d.deleted = 0 AND d.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT d.production_batch_no AS sliceNo,
                       d.parent_production_batch_no AS segmentBatchNo,
                       'FQC_SUBMISSION' AS processCode,
                       'FQC送检' AS processName,
                       46 AS stageSort,
                       'mes_qms_fqc_submission_detail' AS sourceTable,
                       d.id AS sourceId,
                       d.plan_id AS planId,
                       d.plan_no AS planNo,
                       d.plan_operation_id AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       NULL AS statusText,
                       NULL AS resultText,
                       COALESCE(d.inspection_time, d.update_time, d.create_time) AS reportTime
                  FROM mes_qms_fqc_submission_detail d
                 WHERE d.deleted = 0 AND d.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT i.production_batch_no AS sliceNo,
                       i.parent_production_batch_no AS segmentBatchNo,
                       'FQC_ITEM' AS processCode,
                       'FQC检验项' AS processName,
                       47 AS stageSort,
                       'mes_qms_fqc_item' AS sourceTable,
                       i.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       i.input_status AS statusText,
                       COALESCE(i.qa_result, i.operator_result, i.item_result) AS resultText,
                       COALESCE(i.qa_time, i.operator_time, i.update_time, i.create_time) AS reportTime
                  FROM mes_qms_fqc_item i
                 WHERE i.deleted = 0 AND i.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT s.production_batch_no AS sliceNo,
                       s.parent_production_batch_no AS segmentBatchNo,
                       'FQC_SAMPLE' AS processCode,
                       'FQC样本' AS processName,
                       48 AS stageSort,
                       'mes_qms_fqc_sample' AS sourceTable,
                       s.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       NULL AS statusText,
                       s.sample_result AS resultText,
                       COALESCE(s.input_time, s.update_time, s.create_time) AS reportTime
                  FROM mes_qms_fqc_sample s
                 WHERE s.deleted = 0 AND s.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT o.product_batch_no AS sliceNo,
                       o.batch_no AS segmentBatchNo,
                       'FQC_ORDER' AS processCode,
                       'FQC检验单' AS processName,
                       49 AS stageSort,
                       'mes_qms_fqc_order' AS sourceTable,
                       o.id AS sourceId,
                       o.plan_order_id AS planId,
                       o.work_order_no AS planNo,
                       NULL AS planOperationId,
                       o.operation_code AS operationCode,
                       o.operation_name AS operationName,
                       o.material_code AS materialCode,
                       o.material_name AS materialName,
                       o.product_model AS modelCode,
                       o.status AS statusText,
                       COALESCE(o.release_result, o.judgment) AS resultText,
                       COALESCE(o.release_time, o.qa_time, o.inspection_time, o.submission_time, o.update_time, o.create_time) AS reportTime
                  FROM mes_qms_fqc_order o
                 WHERE o.deleted = 0 AND o.product_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT s.matched_production_batch_no AS sliceNo,
                       NULL AS segmentBatchNo,
                       'FQC_SCAN' AS processCode,
                       'FQC扫码匹配' AS processName,
                       49 AS stageSort,
                       'mes_qms_fqc_scan_record' AS sourceTable,
                       s.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       s.match_result AS statusText,
                       NULL AS resultText,
                       COALESCE(s.scan_time, s.update_time, s.create_time) AS reportTime
                  FROM mes_qms_fqc_scan_record s
                 WHERE s.deleted = 0 AND s.matched_production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT i.slice_batch_no AS sliceNo,
                       COALESCE(NULLIF(u.batch_no, ''), NULLIF(i.production_batch_no, '')) AS segmentBatchNo,
                       'PACKAGING' AS processCode,
                       '内包装' AS processName,
                       50 AS stageSort,
                       'mes_sfc_inner_pack_unit_item' AS sourceTable,
                       i.id AS sourceId,
                       i.plan_id AS planId,
                       i.plan_no AS planNo,
                       i.plan_operation_id AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       u.unit_status AS statusText,
                       i.quality_status AS resultText,
                       COALESCE(i.scan_time, i.update_time, i.create_time) AS reportTime
                  FROM mes_sfc_inner_pack_unit_item i
                  LEFT JOIN mes_sfc_inner_pack_unit u ON u.deleted = 0 AND u.id = i.inner_unit_id
                 WHERE i.deleted = 0 AND i.slice_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT s.slice_batch_no AS sliceNo,
                       s.batch_no AS segmentBatchNo,
                       'FG_STOCK' AS processCode,
                       '成品库存' AS processName,
                       60 AS stageSort,
                       'mes_inv_finished_stock' AS sourceTable,
                       s.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       s.material_code AS materialCode,
                       s.material_name AS materialName,
                       s.model_code AS modelCode,
                       s.stock_status AS statusText,
                       s.quality_status AS resultText,
                       COALESCE(s.inbound_time, s.update_time, s.create_time) AS reportTime
                  FROM mes_inv_finished_stock s
                 WHERE s.deleted = 0 AND s.slice_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT p.actual_slice_batch_no AS sliceNo,
                       COALESCE(NULLIF(p.batch_no, ''), NULLIF(p.slice_batch_no, '')) AS segmentBatchNo,
                       'SHIPPING_PICK' AS processCode,
                       '发货拣配' AS processName,
                       70 AS stageSort,
                       'mes_inv_fg_shipping_pick_item' AS sourceTable,
                       p.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       p.lock_status AS statusText,
                       p.quality_status AS resultText,
                       COALESCE(p.lock_time, p.update_time, p.create_time) AS reportTime
                  FROM mes_inv_fg_shipping_pick_item p
                 WHERE p.deleted = 0 AND p.actual_slice_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT o.slice_batch_no AS sliceNo,
                       o.batch_no AS segmentBatchNo,
                       'FG_OUTBOUND' AS processCode,
                       '成品出库装箱' AS processName,
                       80 AS stageSort,
                       'mes_inv_fg_outbound_box_item' AS sourceTable,
                       o.id AS sourceId,
                       NULL AS planId,
                       NULL AS planNo,
                       NULL AS planOperationId,
                       NULL AS operationCode,
                       NULL AS operationName,
                       NULL AS materialCode,
                       NULL AS materialName,
                       NULL AS modelCode,
                       NULL AS statusText,
                       o.quality_status AS resultText,
                       COALESCE(o.scan_time, o.update_time, o.create_time) AS reportTime
                  FROM mes_inv_fg_outbound_box_item o
                 WHERE o.deleted = 0 AND o.slice_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
                UNION ALL
                SELECT r.production_batch_no AS sliceNo,
                       COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''), NULLIF(r.batch_no, '')) AS segmentBatchNo,
                       COALESCE(r.operation_code, 'OPERATION_REPORT') AS processCode,
                       COALESCE(r.operation_name, '通用报工') AS processName,
                       COALESCE(r.operation_seq, 0) AS stageSort,
                       'mes_sfc_operation_report' AS sourceTable,
                       r.id AS sourceId,
                       r.plan_id AS planId,
                       r.plan_no AS planNo,
                       r.plan_operation_id AS planOperationId,
                       r.operation_code AS operationCode,
                       r.operation_name AS operationName,
                       r.material_code AS materialCode,
                       r.material_name AS materialName,
                       r.mother_model_code AS modelCode,
                       r.operation_status AS statusText,
                       NULL AS resultText,
                       COALESCE(r.confirmer_time, r.end_time, r.update_time, r.create_time) AS reportTime
                  FROM mes_sfc_operation_report r
                 WHERE r.deleted = 0 AND r.production_batch_no IN (#{leftSliceNo}, #{rightSliceNo})
            ) t
            WHERE 1 = 1
            <if test="segmentBatchNo != null and segmentBatchNo != ''">
              AND (t.segmentBatchNo = #{segmentBatchNo}
                   OR t.segmentBatchNo LIKE CONCAT(#{segmentBatchNo}, '-J%'))
            </if>
            ORDER BY t.segmentBatchNo DESC, t.sliceNo ASC, t.stageSort DESC, t.reportTime DESC
            </script>
            """)
    List<HcSliceAdjustTraceRow> selectTraceRowsBySliceNos(@Param("segmentBatchNo") String segmentBatchNo,
                                                          @Param("leftSliceNo") String leftSliceNo,
                                                          @Param("rightSliceNo") String rightSliceNo);

    @Update("""
            UPDATE ${tableName}
               SET ${columnName} = #{newValue},
                   update_time = NOW()
             WHERE deleted = 0
               ${extraCondition}
               AND ${columnName} = #{oldValue}
            """)
    int updateColumnValue(@Param("tableName") String tableName,
                          @Param("columnName") String columnName,
                          @Param("extraCondition") String extraCondition,
                          @Param("oldValue") String oldValue,
                          @Param("newValue") String newValue);
}
