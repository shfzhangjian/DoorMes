package cn.iocoder.yudao.module.mes.dal.mysql.hc.productionfactadjust;

import cn.iocoder.yudao.module.mes.controller.admin.hc.productionfactadjust.vo.HcProductionFactAdjustVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 换型产品快照调账的受控 SQL 集合。
 *
 * <p>所有 UPDATE 均以计划工序与分段批次作为范围，不允许客户端传入表名或字段名。</p>
 */
@Mapper
public interface HcProductionFactAdjustExecutionMapper {

    @Select("""
            SELECT p.id AS planId, p.plan_no AS planNo,
                   o.id AS planOperationId, o.op_code AS operationCode, o.op_name AS operationName
              FROM mes_pp_plan_order p
              JOIN mes_pp_plan_operation o ON o.plan_id = p.id AND o.deleted = 0
             WHERE p.deleted = 0 AND p.plan_no = #{planNo} AND o.id = #{planOperationId}
             LIMIT 1
            """)
    HcProductionFactAdjustScopeRow selectScope(@Param("planNo") String planNo,
                                                @Param("planOperationId") Long planOperationId);

    @Select("""
            SELECT MIN(r.model_code) AS modelCode,
                   MIN(r.material_code) AS materialCode,
                   MIN(r.material_name) AS materialName,
                   COUNT(DISTINCT CONCAT(COALESCE(r.model_code, ''), '|', COALESCE(r.material_code, ''))) AS variantCount
              FROM mes_sfc_adhesive2_report r
             WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo} AND r.report_status = 'SUBMITTED'
            """)
    HcProductionFactAdjustProductRow selectSourceProduct(@Param("planId") Long planId,
                                                          @Param("planOperationId") Long planOperationId,
                                                          @Param("segmentBatchNo") String segmentBatchNo);

    @Select("""
            SELECT pm.id AS productModelId, pm.model_code AS modelCode,
                   m.id AS materialId, m.material_code AS materialCode, m.material_name AS materialName,
                   m.spec_model AS specification
              FROM mes_md_product_model pm
              JOIN mes_md_product_model_material mm
                ON mm.model_id = pm.id AND mm.deleted = 0
              JOIN mes_md_material m
                ON m.id = mm.material_id AND m.deleted = 0
             WHERE pm.deleted = 0 AND pm.status = 'ENABLE'
               AND pm.model_code = #{modelCode} AND m.material_code = #{materialCode}
             LIMIT 1
            """)
    HcProductionFactAdjustProductRow selectTargetProduct(@Param("modelCode") String modelCode,
                                                          @Param("materialCode") String materialCode);

    @Select("""
            SELECT pm.id AS productModelId, pm.model_code AS modelCode,
                   m.id AS materialId, m.material_code AS materialCode, m.material_name AS materialName,
                   m.spec_model AS specification
              FROM mes_md_product_model pm
              JOIN mes_md_product_model_material mm ON mm.model_id = pm.id AND mm.deleted = 0
              JOIN mes_md_material m ON m.id = mm.material_id AND m.deleted = 0
             WHERE pm.deleted = 0 AND pm.status = 'ENABLE'
               AND (#{keyword} IS NULL OR #{keyword} = ''
                    OR pm.model_code LIKE CONCAT('%', #{keyword}, '%')
                    OR m.material_code LIKE CONCAT('%', #{keyword}, '%')
                    OR m.material_name LIKE CONCAT('%', #{keyword}, '%'))
             ORDER BY pm.model_code, mm.is_default DESC, m.material_code
             LIMIT 200
            """)
    List<HcProductionFactAdjustProductRow> selectTargetProductList(@Param("keyword") String keyword);

    @Select("""
            SELECT o.id, o.op_code AS opCode, o.op_name AS opName
              FROM mes_pp_plan_operation o
              JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
             WHERE o.deleted = 0 AND p.plan_no = #{planNo}
             ORDER BY o.op_seq, o.id
            """)
    List<HcProductionFactAdjustVO.OperationOptionRespVO> selectOperationOptionList(@Param("planNo") String planNo);

    @Select("""
            SELECT r.source_batch_no AS segmentBatchNo, COUNT(*) AS reportCount
              FROM mes_sfc_adhesive2_report r
             WHERE r.deleted = 0 AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no IS NOT NULL AND r.source_batch_no <> ''
             GROUP BY r.source_batch_no
             ORDER BY MAX(r.confirmer_time) DESC, r.source_batch_no
            """)
    List<HcProductionFactAdjustVO.SegmentOptionRespVO> selectSegmentOptionList(@Param("planOperationId") Long planOperationId);

    @Select("""
            SELECT i.id, i.instruction_no AS instructionNo, i.target_model_code AS targetModelCode,
                   i.target_material_code AS targetMaterialCode, i.target_qty AS targetQty,
                   i.completed_qty AS completedQty, i.execute_status AS executeStatus
              FROM mes_pp_production_instruction i
             WHERE i.deleted = 0 AND i.instruction_type = 'CHANGEOVER' AND i.status = 'CONFIRMED'
               AND i.plan_operation_id = #{planOperationId} AND i.segment_batch_no = #{segmentBatchNo}
             ORDER BY i.issued_time DESC, i.id DESC
            """)
    List<HcProductionFactAdjustVO.InstructionOptionRespVO> selectInstructionOptionList(
            @Param("planOperationId") Long planOperationId, @Param("segmentBatchNo") String segmentBatchNo);

    @Select("""
            SELECT
              (SELECT COUNT(*) FROM mes_sfc_adhesive2_report r
                WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo} AND r.report_status = 'SUBMITTED') AS adhesive2ReportCount,
              (SELECT COUNT(*) FROM mes_sfc_cut_round_report c
                JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                WHERE c.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}) AS cutRoundReportCount,
              (SELECT COUNT(*) FROM mes_inv_stock s WHERE s.deleted = 0 AND s.id IN (
                SELECT r.output_stock_id FROM mes_sfc_adhesive2_report r
                  WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                    AND r.source_batch_no = #{segmentBatchNo} AND r.output_stock_id IS NOT NULL
                UNION
                SELECT c.output_stock_id FROM mes_sfc_cut_round_report c
                  JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                  WHERE c.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                    AND r.source_batch_no = #{segmentBatchNo} AND c.output_stock_id IS NOT NULL
              )) AS outputStockCount,
              (SELECT COUNT(*) FROM mes_sfc_cut_round_inspection_detail d
                JOIN mes_sfc_cut_round_report c ON c.id = d.cut_round_report_id AND c.deleted = 0
                JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                WHERE d.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}) AS cutInspectionDetailCount,
              (SELECT COUNT(DISTINCT d.fqc_id) FROM mes_qms_fqc_submission_detail d
                JOIN mes_sfc_cut_round_report c ON c.id = d.cut_round_report_id AND c.deleted = 0
                JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                WHERE d.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}) AS fqcOrderCount,
              (SELECT COUNT(*) FROM mes_qms_fqc_submission_detail d
                JOIN mes_sfc_cut_round_report c ON c.id = d.cut_round_report_id AND c.deleted = 0
                JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                WHERE d.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}) AS fqcSubmissionDetailCount,
              (SELECT COUNT(*) FROM mes_qms_fai_order q WHERE q.deleted = 0 AND q.source_report_id IN (
                SELECT r.id FROM mes_sfc_adhesive2_report r WHERE r.deleted = 0 AND r.plan_id = #{planId}
                  AND r.plan_operation_id = #{planOperationId} AND r.source_batch_no = #{segmentBatchNo}
              )) AS faiOrderCount,
              (SELECT COUNT(*) FROM mes_hc_process_form_record f WHERE f.deleted = 0 AND f.process_code = 'ADHESIVE2'
                AND f.plan_id = #{planId} AND f.plan_operation_id = #{planOperationId} AND f.batch_no = #{segmentBatchNo}
                AND f.model_code IS NOT NULL AND f.model_code <> 'COMMON') AS processFormCount,
              (SELECT COUNT(*) FROM mes_sfc_inner_pack_unit_item i
                JOIN mes_sfc_cut_round_report c ON c.production_batch_no = i.production_batch_no AND c.deleted = 0
                JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                WHERE i.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}) AS packagingCount,
              (SELECT COUNT(*) FROM mes_inv_finished_stock f
                JOIN mes_sfc_cut_round_report c ON c.production_batch_no = f.slice_batch_no AND c.deleted = 0
                JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                WHERE f.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}) AS finishedStockCount,
              (SELECT COUNT(DISTINCT q.id) FROM mes_qms_fqc_order q
                JOIN mes_qms_fqc_submission_detail d ON d.fqc_id = q.id AND d.deleted = 0
                JOIN mes_sfc_cut_round_report c ON c.id = d.cut_round_report_id AND c.deleted = 0
                JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                WHERE q.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}
                  AND (q.status <> 'PENDING' OR q.judgment <> 'PENDING' OR q.completed_item_count > 0 OR q.sheet_locked = b'1')) AS unsafeFqcOrderCount,
              (SELECT COUNT(*) FROM mes_qms_fai_order q WHERE q.deleted = 0 AND q.source_report_id IN (
                SELECT r.id FROM mes_sfc_adhesive2_report r WHERE r.deleted = 0 AND r.plan_id = #{planId}
                  AND r.plan_operation_id = #{planOperationId} AND r.source_batch_no = #{segmentBatchNo}
              ) AND (q.status <> 'PENDING' OR q.judgment <> 'PENDING' OR q.completed_item_count > 0
                       OR q.required_item_count > 0 OR q.sheet_locked = b'1')) AS unsafeFaiOrderCount
            """)
    HcProductionFactAdjustImpactRow selectImpact(@Param("planId") Long planId,
                                                  @Param("planOperationId") Long planOperationId,
                                                  @Param("segmentBatchNo") String segmentBatchNo);

    @Select("""
            SELECT r.id, r.production_batch_no AS productionBatchNo, r.model_code AS modelCode,
                   r.material_code AS materialCode, r.confirmer_time AS confirmerTime, r.tenant_id AS tenantId
              FROM mes_sfc_adhesive2_report r
             WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo} AND r.report_status = 'SUBMITTED'
             ORDER BY r.confirmer_time, r.id
            """)
    List<HcProductionFactAdjustAdhesive2Row> selectAdhesive2Rows(@Param("planId") Long planId,
                                                                  @Param("planOperationId") Long planOperationId,
                                                                  @Param("segmentBatchNo") String segmentBatchNo);

    @Select("""
            SELECT MAX(r.confirmer_time)
              FROM mes_sfc_adhesive2_report r
             WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo} AND r.report_status = 'SUBMITTED'
            """)
    LocalDateTime selectLatestAdhesive2ConfirmTime(@Param("planId") Long planId,
                                                    @Param("planOperationId") Long planOperationId,
                                                    @Param("segmentBatchNo") String segmentBatchNo);

    @Update("""
            UPDATE mes_sfc_adhesive2_report r
               SET r.model_code = #{target.modelCode}, r.material_code = #{target.materialCode},
                   r.material_name = #{target.materialName},
                   r.extra_json = JSON_SET(IF(JSON_VALID(r.extra_json), r.extra_json, JSON_OBJECT()),
                       '$.modelChanged', TRUE, '$.changeoverInstructionId', #{instructionId},
                       '$.changeoverInstructionNo', #{instructionNo}, '$.runtimeModelCode', #{target.modelCode},
                       '$.runtimeMaterialCode', #{target.materialCode}, '$.runtimeProductSpecification', #{target.specification},
                       '$.productionFactAdjustNo', #{adjustNo}),
                   r.remark = LEFT(CONCAT_WS('；', NULLIF(r.remark, ''), #{mark}), 500),
                   r.updater = #{actorName}, r.update_time = NOW()
             WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo} AND r.report_status = 'SUBMITTED'
            """)
    int updateAdhesive2Reports(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                               @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                               @Param("instructionId") Long instructionId, @Param("instructionNo") String instructionNo,
                               @Param("adjustNo") String adjustNo, @Param("mark") String mark, @Param("actorName") String actorName);

    @Update("""
            UPDATE mes_sfc_cut_round_report c
              JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
               SET c.model_code = #{target.modelCode}, c.material_code = #{target.materialCode},
                   c.material_name = #{target.materialName},
                   c.extra_json = JSON_SET(IF(JSON_VALID(c.extra_json), c.extra_json, JSON_OBJECT()),
                       '$.modelChanged', TRUE, '$.changeoverInstructionId', #{instructionId},
                       '$.changeoverInstructionNo', #{instructionNo}, '$.runtimeModelCode', #{target.modelCode},
                       '$.runtimeMaterialCode', #{target.materialCode}, '$.runtimeProductSpecification', #{target.specification},
                       '$.productionFactAdjustNo', #{adjustNo}),
                   c.remark = LEFT(CONCAT_WS('；', NULLIF(c.remark, ''), #{mark}), 500),
                   c.updater = #{actorName}, c.update_time = NOW()
             WHERE c.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo}
            """)
    int updateCutRoundReports(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                              @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                              @Param("instructionId") Long instructionId, @Param("instructionNo") String instructionNo,
                              @Param("adjustNo") String adjustNo, @Param("mark") String mark, @Param("actorName") String actorName);

    @Update("""
            UPDATE mes_inv_stock s
               SET s.material_id = #{target.materialId}, s.material_code = #{target.materialCode},
                   s.material_name = #{target.materialName}, s.model_no = #{target.modelCode},
                   s.spec_size = #{target.specification},
                   s.business_remark = LEFT(CONCAT_WS('；', NULLIF(s.business_remark, ''), #{mark}), 300),
                   s.updater = #{actorName}, s.update_time = NOW()
             WHERE s.deleted = 0 AND s.id IN (
                 SELECT r.output_stock_id FROM mes_sfc_adhesive2_report r
                  WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                    AND r.source_batch_no = #{segmentBatchNo} AND r.output_stock_id IS NOT NULL
                 UNION
                 SELECT c.output_stock_id FROM mes_sfc_cut_round_report c
                  JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
                  WHERE c.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                    AND r.source_batch_no = #{segmentBatchNo} AND c.output_stock_id IS NOT NULL
             )
            """)
    int updateOutputStocks(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                           @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                           @Param("mark") String mark, @Param("actorName") String actorName);

    @Update("""
            UPDATE mes_sfc_cut_round_inspection_detail d
              JOIN mes_sfc_cut_round_report c ON c.id = d.cut_round_report_id AND c.deleted = 0
              JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
               SET d.model_code = #{target.modelCode}, d.material_code = #{target.materialCode},
                   d.material_name = #{target.materialName},
                   d.remark = LEFT(CONCAT_WS('；', NULLIF(d.remark, ''), #{mark}), 500),
                   d.updater = #{actorName}, d.update_time = NOW()
             WHERE d.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo}
            """)
    int updateCutInspectionDetails(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                                   @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                                   @Param("mark") String mark, @Param("actorName") String actorName);

    @Update("""
            UPDATE mes_qms_fqc_submission_detail d
              JOIN mes_sfc_cut_round_report c ON c.id = d.cut_round_report_id AND c.deleted = 0
              JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
               SET d.model_code = #{target.modelCode}, d.material_code = #{target.materialCode},
                   d.material_name = #{target.materialName},
                   d.remark = LEFT(CONCAT_WS('；', NULLIF(d.remark, ''), #{mark}), 500),
                   d.updater = #{actorName}, d.update_time = NOW()
             WHERE d.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo} AND d.row_judgment = 'PENDING'
            """)
    int updateFqcSubmissionDetails(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                                   @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                                   @Param("mark") String mark, @Param("actorName") String actorName);

    @Update("""
            UPDATE mes_qms_fqc_order q
              JOIN mes_qms_fqc_submission_detail d ON d.fqc_id = q.id AND d.deleted = 0
              JOIN mes_sfc_cut_round_report c ON c.id = d.cut_round_report_id AND c.deleted = 0
              JOIN mes_sfc_adhesive2_report r ON r.id = c.source_adhesive2_report_id AND r.deleted = 0
               SET q.material_id = #{target.materialId}, q.material_code = #{target.materialCode},
                   q.material_name = #{target.materialName}, q.specification = #{target.specification},
                   q.product_model = #{target.modelCode},
                   q.remark = LEFT(CONCAT_WS('；', NULLIF(q.remark, ''), #{mark}), 500),
                   q.updater = #{actorName}, q.update_time = NOW()
             WHERE q.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo} AND q.status = 'PENDING' AND q.judgment = 'PENDING'
               AND q.completed_item_count = 0 AND q.sheet_locked = b'0'
            """)
    int updateFqcOrders(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                        @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                        @Param("mark") String mark, @Param("actorName") String actorName);

    @Update("""
            UPDATE mes_qms_fai_order q
               SET q.material_id = #{target.materialId}, q.material_code = #{target.materialCode},
                   q.material_name = #{target.materialName}, q.specification = #{target.specification},
                   q.product_model_id = #{target.productModelId}, q.product_model = #{target.modelCode},
                   q.matched_model_id = #{target.productModelId}, q.matched_model_code = #{target.modelCode},
                   q.remark = LEFT(CONCAT_WS('；', NULLIF(q.remark, ''), #{mark}), 500),
                   q.updater = #{actorName}, q.update_time = NOW()
             WHERE q.deleted = 0 AND q.source_report_id IN (
               SELECT r.id FROM mes_sfc_adhesive2_report r
                WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
                  AND r.source_batch_no = #{segmentBatchNo}
             ) AND q.status = 'PENDING' AND q.judgment = 'PENDING' AND q.completed_item_count = 0
               AND q.required_item_count = 0 AND q.sheet_locked = b'0'
            """)
    int updateFaiOrders(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                        @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                        @Param("mark") String mark, @Param("actorName") String actorName);

    @Update("""
            UPDATE mes_hc_process_form_record f
               SET f.model_code = #{target.modelCode}, f.model_name = #{target.modelCode},
                   f.header_data_json = JSON_SET(IF(JSON_VALID(f.header_data_json), f.header_data_json, JSON_OBJECT()),
                       '$.modelCode', #{target.modelCode}, '$.materialCode', #{target.materialCode},
                       '$.productSpecification', #{target.specification}, '$.productionFactAdjustNo', #{adjustNo}),
                   f.remark = LEFT(CONCAT_WS('；', NULLIF(f.remark, ''), #{mark}), 500),
                   f.updater = #{actorName}, f.update_time = NOW()
             WHERE f.deleted = 0 AND f.process_code = 'ADHESIVE2' AND f.plan_id = #{planId}
               AND f.plan_operation_id = #{planOperationId} AND f.batch_no = #{segmentBatchNo}
               AND f.model_code IS NOT NULL AND f.model_code <> 'COMMON'
            """)
    int updateProcessForms(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                           @Param("segmentBatchNo") String segmentBatchNo, @Param("target") HcProductionFactAdjustProductRow target,
                           @Param("adjustNo") String adjustNo, @Param("mark") String mark, @Param("actorName") String actorName);

    @Insert("""
            INSERT INTO mes_pp_production_instruction_changeover_piece (
                tenant_id, instruction_id, instruction_no, plan_id, plan_no, plan_operation_id, segment_batch_no,
                piece_no, adhesive2_report_id, actual_model_code, actual_glue_board_model, actual_glue_board_batch_no,
                scan_user_id, scan_user_name, scan_time, status, remark, creator, create_time, updater, update_time, deleted
            )
            SELECT r.tenant_id, i.id, i.instruction_no, r.plan_id, r.plan_no, r.plan_operation_id, r.source_batch_no,
                   r.production_batch_no, r.id, #{target.modelCode}, r.glue_board_model, r.glue_board_batch_no,
                   i.execute_user_id, i.execute_user_name, r.confirmer_time, 'DONE', #{mark}, #{actorName}, NOW(), #{actorName}, NOW(), b'0'
              FROM mes_sfc_adhesive2_report r
              JOIN mes_pp_production_instruction i ON i.id = #{instructionId} AND i.deleted = 0
              LEFT JOIN mes_pp_production_instruction_changeover_piece p
                ON p.instruction_id = i.id AND p.piece_no = r.production_batch_no AND p.deleted = 0
             WHERE r.deleted = 0 AND r.plan_id = #{planId} AND r.plan_operation_id = #{planOperationId}
               AND r.source_batch_no = #{segmentBatchNo} AND r.report_status = 'SUBMITTED' AND p.id IS NULL
            """)
    int insertChangeoverPieces(@Param("planId") Long planId, @Param("planOperationId") Long planOperationId,
                               @Param("segmentBatchNo") String segmentBatchNo, @Param("instructionId") Long instructionId,
                               @Param("target") HcProductionFactAdjustProductRow target, @Param("mark") String mark,
                               @Param("actorName") String actorName);
}
