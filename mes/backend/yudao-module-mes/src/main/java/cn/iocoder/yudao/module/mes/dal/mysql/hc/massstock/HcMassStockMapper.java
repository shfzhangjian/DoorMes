package cn.iocoder.yudao.module.mes.dal.mysql.hc.massstock;

import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockGoodStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo.HcMassStockShippingDetailRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HcMassStockMapper {

    String SOURCE_ROWS_SQL = """
            SELECT r.tenant_id AS tenantId,
                   COALESCE(NULLIF(r.mother_model_code, ''), NULLIF(r.mother_model_name, '')) AS modelCode,
                   COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''),
                            NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(wet_sd.confirmed_batch_no, ''), NULLIF(wet_sd.production_batch_no, ''),
                            NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                            NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, '')) AS rawBatchNo,
                   COALESCE(NULLIF(r.good_qty, 0), r.feed_qty, 0) AS wetMeter,
                   0 AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   0 AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   NULL AS earliestProductionDate,
                   NULL AS latestProductionDate,
                   COALESCE(NULLIF(r.fai_judgment, ''), q.judgment) AS hcNapResult,
                   NULL AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_sfc_operation_report r
              LEFT JOIN mes_qms_fai_order q ON q.id = r.fai_id AND q.deleted = b'0'
              LEFT JOIN mes_sfc_grinding_second_detail wet_sd
                     ON wet_sd.deleted = b'0'
                    AND wet_sd.tenant_id = r.tenant_id
                    AND (wet_sd.mother_batch_no = COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                                                           NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''))
                         OR wet_sd.parent_production_batch_no = COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                                                                         NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''))
                         OR wet_sd.source_production_batch_no = COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''),
                                                                         NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, '')))
             WHERE r.deleted = b'0'
               AND r.tenant_id = #{tenantId}
               AND r.source_menu_code = 'WET_REPORT'
            UNION ALL
            SELECT fd.tenant_id AS tenantId,
                   COALESCE(NULLIF(p.model_code, ''), NULLIF(p.mother_model_code, ''), NULLIF(p.model_name, '')) AS modelCode,
                   COALESCE(NULLIF(fa.mother_batch_no, ''), NULLIF(fd.mother_batch_no, ''),
                            NULLIF(fd.source_production_batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(sd.confirmed_batch_no, ''), NULLIF(sd.production_batch_no, ''),
                            NULLIF(fa.production_batch_no, ''),
                            NULLIF(fd.source_production_batch_no, ''), NULLIF(fd.mother_batch_no, '')) AS rawBatchNo,
                   0 AS wetMeter,
                   CASE WHEN fa.id IS NOT NULL THEN COALESCE(fa.confirmed_length, 0)
                        WHEN sd.id IS NULL THEN COALESCE(fd.process_length, 0)
                        ELSE COALESCE(NULLIF(sd.process_length, 0), fd.process_length, 0) END AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   0 AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   fd.report_date AS earliestProductionDate,
                   fd.report_date AS latestProductionDate,
                   NULL AS hcNapResult,
                   NULL AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_sfc_grinding_first_detail fd
              LEFT JOIN mes_sfc_grinding_first_allocation_detail fa
                     ON fa.deleted = b'0'
                    AND fa.tenant_id = fd.tenant_id
                    AND fa.first_detail_id = fd.id
                    AND COALESCE(fa.detail_status, 'ACTIVE') <> 'VOID'
              LEFT JOIN mes_sfc_grinding_second_detail sd
                     ON sd.deleted = b'0'
                    AND sd.tenant_id = fd.tenant_id
                    AND (
                        (fa.id IS NOT NULL AND sd.first_allocation_id = fa.id)
                        OR (
                            fa.id IS NULL
                            AND sd.first_allocation_id IS NULL
                            AND (
                                sd.first_detail_id = fd.id
                                OR (
                                    sd.first_detail_id IS NULL
                                    AND (
                                        sd.mother_batch_no = COALESCE(NULLIF(fd.source_production_batch_no, ''), NULLIF(fd.mother_batch_no, ''))
                                        OR sd.parent_production_batch_no = COALESCE(NULLIF(fd.source_production_batch_no, ''), NULLIF(fd.mother_batch_no, ''))
                                        OR sd.source_production_batch_no = COALESCE(NULLIF(fd.source_production_batch_no, ''), NULLIF(fd.mother_batch_no, ''))
                                    )
                                )
                            )
                        )
                    )
              LEFT JOIN mes_pp_plan_order p ON p.id = fd.plan_id AND p.deleted = b'0'
             WHERE fd.deleted = b'0'
               AND fd.tenant_id = #{tenantId}
            UNION ALL
            SELECT sd.tenant_id AS tenantId,
                   COALESCE(NULLIF(p.model_code, ''), NULLIF(p.mother_model_code, ''), NULLIF(p.model_name, '')) AS modelCode,
                   COALESCE(NULLIF(sd.mother_batch_no, ''), NULLIF(sd.parent_production_batch_no, ''),
                            NULLIF(sd.source_production_batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(sd.confirmed_batch_no, ''), NULLIF(sd.production_batch_no, ''),
                            NULLIF(sd.parent_production_batch_no, ''), NULLIF(sd.source_production_batch_no, ''),
                            NULLIF(sd.mother_batch_no, '')) AS rawBatchNo,
                   0 AS wetMeter,
                   0 AS grindingFirstMeter,
                   COALESCE(sd.output_length, 0) AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   0 AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   sd.report_date AS earliestProductionDate,
                   sd.report_date AS latestProductionDate,
                   NULL AS hcNapResult,
                   sd.inspection_result AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_sfc_grinding_second_detail sd
              LEFT JOIN mes_pp_plan_order p ON p.id = sd.plan_id AND p.deleted = b'0'
             WHERE sd.deleted = b'0'
               AND sd.tenant_id = #{tenantId}
            UNION ALL
            SELECT a.tenant_id AS tenantId,
                   a.model_code AS modelCode,
                   COALESCE(NULLIF(a.parent_production_batch_no, ''), NULLIF(a.source_production_batch_no, ''),
                            NULLIF(a.source_batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(a.production_batch_no, ''), NULLIF(a.source_production_batch_no, ''),
                            NULLIF(a.parent_production_batch_no, ''), NULLIF(a.source_batch_no, '')) AS rawBatchNo,
                   0 AS wetMeter,
                   0 AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   COALESCE(NULLIF(a.slitting_remaining_length, 0),
                            GREATEST(COALESCE(a.input_length, 0) - COALESCE(a.output_length, 0) - COALESCE(a.loss_length, 0), 0),
                            0) AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   0 AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   a.report_date AS earliestProductionDate,
                   a.report_date AS latestProductionDate,
                   NULL AS hcNapResult,
                   NULL AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_sfc_adhesive_report a
             WHERE a.deleted = b'0'
               AND a.tenant_id = #{tenantId}
               AND a.report_status <> 'DRAFT'
            UNION ALL
            SELECT ps.tenant_id AS tenantId,
                   ps.model_code AS modelCode,
                   COALESCE(NULLIF(ps.parent_production_batch_no, ''), NULLIF(ps.source_production_batch_no, ''),
                            NULLIF(ps.source_batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(ps.production_batch_no, ''), NULLIF(ps.source_production_batch_no, ''),
                            NULLIF(ps.parent_production_batch_no, ''), NULLIF(ps.source_batch_no, '')) AS rawBatchNo,
                   0 AS wetMeter,
                   0 AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   COALESCE(NULLIF(ps.output_length, 0), 1) AS pressSlotQty,
                   0 AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   ps.report_date AS earliestProductionDate,
                   ps.report_date AS latestProductionDate,
                   NULL AS hcNapResult,
                   NULL AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_sfc_press_slot_report ps
             WHERE ps.deleted = b'0'
               AND ps.tenant_id = #{tenantId}
               AND ps.report_status <> 'DRAFT'
            UNION ALL
            SELECT a2.tenant_id AS tenantId,
                   a2.model_code AS modelCode,
                   COALESCE(NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.source_production_batch_no, ''),
                            NULLIF(a2.source_batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''),
                            NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, '')) AS rawBatchNo,
                   0 AS wetMeter,
                   0 AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   COALESCE(NULLIF(a2.output_length, 0), 1) AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   a2.report_date AS earliestProductionDate,
                   a2.report_date AS latestProductionDate,
                   NULL AS hcNapResult,
                   NULL AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_sfc_adhesive2_report a2
             WHERE a2.deleted = b'0'
               AND a2.tenant_id = #{tenantId}
               AND a2.report_status <> 'DRAFT'
            UNION ALL
            SELECT cr.tenant_id AS tenantId,
                   cr.model_code AS modelCode,
                   COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''),
                            NULLIF(cr.source_batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(cr.production_batch_no, ''), NULLIF(cr.parent_production_batch_no, ''),
                            NULLIF(cr.source_production_batch_no, ''), NULLIF(cr.source_batch_no, '')) AS rawBatchNo,
                   0 AS wetMeter,
                   0 AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   0 AS adhesive2Qty,
                   COALESCE(NULLIF(cr.output_length, 0), 1) AS cutRoundQty,
                   CASE WHEN cr.inspection_result IS NULL OR cr.inspection_result = ''
                         OR (cr.inspection_status IS NOT NULL AND cr.inspection_status NOT IN ('COMPLETED', 'FINISHED', 'DONE'))
                        THEN COALESCE(NULLIF(cr.output_length, 0), 1) ELSE 0 END AS pendingInspectionQty,
                   CASE WHEN cr.inspection_result = 'OK' THEN COALESCE(NULLIF(cr.output_length, 0), 1) ELSE 0 END AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   cr.report_date AS earliestProductionDate,
                   cr.report_date AS latestProductionDate,
                   NULL AS hcNapResult,
                   NULL AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_sfc_cut_round_report cr
             WHERE cr.deleted = b'0'
               AND cr.tenant_id = #{tenantId}
               AND cr.report_status <> 'DRAFT'
            UNION ALL
            SELECT fs.tenant_id AS tenantId,
                   fs.model_code AS modelCode,
                   COALESCE(NULLIF(fs.batch_no, ''), NULLIF(fs.slice_batch_no, '')) AS rawMotherBatchNo,
                   COALESCE(NULLIF(fs.slice_batch_no, ''), NULLIF(fs.batch_no, '')) AS rawBatchNo,
                   0 AS wetMeter,
                   0 AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   0 AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   CASE WHEN fs.quality_status IN ('OK', '合格') THEN COALESCE(fs.qty, 0) ELSE 0 END AS goodStockQty,
                   DATE(fs.inbound_time) AS earliestProductionDate,
                   DATE(fs.inbound_time) AS latestProductionDate,
                   NULL AS hcNapResult,
                   NULL AS csNapResult,
                   NULL AS shippingCoaResult
              FROM mes_inv_finished_stock fs
             WHERE fs.deleted = b'0'
               AND fs.tenant_id = #{tenantId}
               AND fs.stock_status NOT IN ('CANCELLED', 'CANCELED', 'OUTBOUND', 'SHIPPED')
            UNION ALL
            SELECT q.tenant_id AS tenantId,
                   q.product_model AS modelCode,
                   q.product_batch_no AS rawMotherBatchNo,
                   q.product_batch_no AS rawBatchNo,
                   0 AS wetMeter,
                   0 AS grindingFirstMeter,
                   0 AS grindingSecondMeter,
                   0 AS adhesive1UnprocessedMeter,
                   0 AS pressSlotQty,
                   0 AS adhesive2Qty,
                   0 AS cutRoundQty,
                   0 AS pendingInspectionQty,
                   0 AS qualifiedInspectionStockQty,
                   0 AS goodStockQty,
                   DATE(q.submission_time) AS earliestProductionDate,
                   DATE(q.qa_time) AS latestProductionDate,
                   NULL AS hcNapResult,
                   NULL AS csNapResult,
                   q.judgment AS shippingCoaResult
              FROM mes_qms_fai_order q
             WHERE q.deleted = b'0'
               AND q.tenant_id = #{tenantId}
               AND q.source_module = 'ADHESIVE2_REPORT'
               AND q.source_report_no LIKE '%-COA-%'
            """;

    String NORMALIZED_ROWS_SQL = """
            SELECT normalized.tenantId,
                   normalized.modelCode,
                   CASE
                       WHEN normalized.cleanMotherBatchNo IS NOT NULL AND normalized.cleanMotherBatchNo <> ''
                           THEN LEFT(normalized.cleanMotherBatchNo, 8)
                       WHEN normalized.motherSegmentBatchNo IS NOT NULL AND normalized.motherSegmentBatchNo <> ''
                           THEN LEFT(normalized.motherSegmentBatchNo, 8)
                       ELSE normalized.motherSegmentBatchNo
                   END AS motherBatchNo,
                   normalized.motherSegmentBatchNo,
                   normalized.wetMeter,
                   normalized.grindingFirstMeter,
                   normalized.grindingSecondMeter,
                   normalized.adhesive1UnprocessedMeter,
                   normalized.pressSlotQty,
                   normalized.adhesive2Qty,
                   normalized.cutRoundQty,
                   normalized.pendingInspectionQty,
                   normalized.qualifiedInspectionStockQty,
                   normalized.goodStockQty,
                   normalized.earliestProductionDate,
                   normalized.latestProductionDate,
                   normalized.hcNapResult,
                   normalized.csNapResult,
                   normalized.shippingCoaResult
              FROM (
                    SELECT cleaned.tenantId,
                           cleaned.modelCode,
                           CASE
                               WHEN cleaned.cleanMotherBatchNo REGEXP '[0-9][0-9][0-9][AB]$' AND LENGTH(cleaned.cleanMotherBatchNo) >= 12
                                   THEN LEFT(cleaned.cleanMotherBatchNo, LENGTH(cleaned.cleanMotherBatchNo) - 4)
                               WHEN cleaned.cleanMotherBatchNo REGEXP '[0-9][0-9][0-9]$' AND LENGTH(cleaned.cleanMotherBatchNo) >= 11
                                   THEN LEFT(cleaned.cleanMotherBatchNo, LENGTH(cleaned.cleanMotherBatchNo) - 3)
                               ELSE cleaned.cleanMotherBatchNo
                           END AS cleanMotherBatchNo,
                           CASE
                               WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9][AB]$' AND LENGTH(cleaned.cleanBatchNo) >= 12
                                   THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 4)
                               WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9]$' AND LENGTH(cleaned.cleanBatchNo) >= 11
                                   THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 3)
                               ELSE cleaned.cleanBatchNo
                           END AS motherSegmentBatchNo,
                           cleaned.wetMeter,
                           cleaned.grindingFirstMeter,
                           cleaned.grindingSecondMeter,
                           cleaned.adhesive1UnprocessedMeter,
                           cleaned.pressSlotQty,
                           cleaned.adhesive2Qty,
                           cleaned.cutRoundQty,
                           cleaned.pendingInspectionQty,
                           cleaned.qualifiedInspectionStockQty,
                           cleaned.goodStockQty,
                           cleaned.earliestProductionDate,
                           cleaned.latestProductionDate,
                           cleaned.hcNapResult,
                           cleaned.csNapResult,
                           cleaned.shippingCoaResult
                      FROM (
                            SELECT raw.tenantId,
                                   TRIM(COALESCE(raw.modelCode, '')) AS modelCode,
                                   SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(raw.rawMotherBatchNo, '')), ' ', '')), '-J', 1) AS cleanMotherBatchNo,
                                   SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(raw.rawBatchNo, '')), ' ', '')), '-J', 1) AS cleanBatchNo,
                                   raw.wetMeter,
                                   raw.grindingFirstMeter,
                                   raw.grindingSecondMeter,
                                   raw.adhesive1UnprocessedMeter,
                                   raw.pressSlotQty,
                                   raw.adhesive2Qty,
                                   raw.cutRoundQty,
                                   raw.pendingInspectionQty,
                                   raw.qualifiedInspectionStockQty,
                                   raw.goodStockQty,
                                   raw.earliestProductionDate,
                                   raw.latestProductionDate,
                                   raw.hcNapResult,
                                   raw.csNapResult,
                                   raw.shippingCoaResult
                              FROM (
                                   """ + SOURCE_ROWS_SQL + """
                                   ) raw
                           ) cleaned
                   ) normalized
             WHERE normalized.modelCode <> ''
               AND normalized.motherSegmentBatchNo IS NOT NULL
               AND normalized.motherSegmentBatchNo <> ''
            """;

    String SEGMENT_DEMAND_SQL = """
            SELECT segment.modelCode,
                   LEFT(segment.motherSegmentBatchNo, 8) AS motherBatchNo,
                   segment.motherSegmentBatchNo,
                   SUM(segment.demandStockQty) AS demandStockQty,
                   SUM(segment.shippingPickedQty) AS shippingPickedQty,
                   SUM(segment.shippingInspectionQty) AS shippingInspectionQty
              FROM (
                    SELECT normalized.modelCode,
                           CASE
                               WHEN normalized.cleanBatchNo REGEXP '[0-9][0-9][0-9][AB]$'
                                    AND LENGTH(normalized.cleanBatchNo) >= 12
                                   THEN LEFT(normalized.cleanBatchNo, LENGTH(normalized.cleanBatchNo) - 4)
                               WHEN normalized.cleanBatchNo REGEXP '[0-9][0-9][0-9]$'
                                    AND LENGTH(normalized.cleanBatchNo) >= 11
                                   THEN LEFT(normalized.cleanBatchNo, LENGTH(normalized.cleanBatchNo) - 3)
                               ELSE normalized.cleanBatchNo
                           END AS motherSegmentBatchNo,
                           normalized.demandStockQty,
                           normalized.shippingPickedQty,
                           normalized.shippingInspectionQty
                      FROM (
                            SELECT raw.modelCode,
                                   SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(raw.rawBatchNo, '')), ' ', '')), '-J', 1) AS cleanBatchNo,
                                   raw.demandStockQty,
                                   raw.shippingPickedQty,
                                   raw.shippingInspectionQty
                              FROM (
                                    SELECT TRIM(COALESCE(i.model_code, n.model_code, '')) AS modelCode,
                                           COALESCE(NULLIF(i.actual_slice_batch_no, ''), NULLIF(i.slice_batch_no, ''),
                                                    NULLIF(i.batch_no, ''), NULLIF(n.required_batch_no, '')) AS rawBatchNo,
                                           COALESCE(NULLIF(i.actual_ship_qty, 0), NULLIF(i.stock_qty, 0),
                                                    NULLIF(i.available_qty, 0), NULLIF(i.locked_qty, 0), 1) AS demandStockQty,
                                           0 AS shippingPickedQty,
                                           0 AS shippingInspectionQty
                                      FROM mes_inv_fg_shipping_notice_item i
                                      LEFT JOIN mes_inv_fg_shipping_notice n ON n.id = i.notice_id AND n.deleted = b'0'
                                     WHERE i.deleted = b'0'
                                       AND i.tenant_id = #{tenantId}
                                       AND (n.product_type IN ('MASS', '量产') OR n.product_type IS NULL OR n.product_type = '')
                                       AND (n.notice_status IS NULL OR n.notice_status NOT IN ('CANCELLED', 'CANCELED', 'CLOSED', 'SHIPPED'))
                                    UNION ALL
                                    SELECT TRIM(COALESCE(n.model_code, '')) AS modelCode,
                                           n.required_batch_no AS rawBatchNo,
                                           COALESCE(NULLIF(n.required_ship_qty, 0), NULLIF(n.notice_qty, 0), 0) AS demandStockQty,
                                           0 AS shippingPickedQty,
                                           0 AS shippingInspectionQty
                                      FROM mes_inv_fg_shipping_notice n
                                     WHERE n.deleted = b'0'
                                       AND n.tenant_id = #{tenantId}
                                       AND (n.product_type IN ('MASS', '量产') OR n.product_type IS NULL OR n.product_type = '')
                                       AND n.notice_status NOT IN ('CANCELLED', 'CANCELED', 'CLOSED', 'SHIPPED')
                                       AND n.required_batch_no IS NOT NULL
                                       AND n.required_batch_no <> ''
                                       AND NOT EXISTS (
                                           SELECT 1
                                             FROM mes_inv_fg_shipping_notice_item i2
                                            WHERE i2.deleted = b'0'
                                              AND i2.tenant_id = n.tenant_id
                                              AND i2.notice_id = n.id
                                            LIMIT 1
                                       )
                                    UNION ALL
                                    SELECT TRIM(COALESCE(p.model_code, n.model_code, '')) AS modelCode,
                                           COALESCE(NULLIF(p.actual_slice_batch_no, ''), NULLIF(p.slice_batch_no, ''),
                                                    NULLIF(p.batch_no, '')) AS rawBatchNo,
                                           0 AS demandStockQty,
                                           COALESCE(NULLIF(p.actual_ship_qty, 0), NULLIF(p.stock_qty, 0),
                                                    NULLIF(p.available_qty, 0), NULLIF(p.locked_qty, 0), 1) AS shippingPickedQty,
                                           CASE WHEN p.shipping_inspection_result IS NOT NULL OR p.shipping_inspection_time IS NOT NULL
                                                THEN COALESCE(NULLIF(p.actual_ship_qty, 0), NULLIF(p.stock_qty, 0),
                                                              NULLIF(p.available_qty, 0), NULLIF(p.locked_qty, 0), 1)
                                                ELSE 0 END AS shippingInspectionQty
                                      FROM mes_inv_fg_shipping_pick_item p
                                      LEFT JOIN mes_inv_fg_shipping_notice n ON n.id = p.notice_id AND n.deleted = b'0'
                                     WHERE p.deleted = b'0'
                                       AND p.tenant_id = #{tenantId}
                                       AND (n.product_type IN ('MASS', '量产') OR n.product_type IS NULL OR n.product_type = '')
                                       AND (p.lock_status IS NULL OR p.lock_status NOT IN ('CANCELLED', 'CANCELED'))
                                   ) raw
                           ) normalized
                   ) segment
             WHERE segment.modelCode <> ''
               AND segment.motherSegmentBatchNo IS NOT NULL
               AND segment.motherSegmentBatchNo <> ''
               AND LENGTH(segment.motherSegmentBatchNo) >= 8
             GROUP BY segment.modelCode, LEFT(segment.motherSegmentBatchNo, 8), segment.motherSegmentBatchNo
            """;

    String AGGREGATE_SQL = """
            SELECT CONCAT(agg.modelCode, '|', agg.motherBatchNo, '|', agg.motherSegmentBatchNo) AS rowKey,
                   agg.modelCode,
                   agg.motherBatchNo,
                   agg.motherSegmentBatchNo,
                   agg.wetMeter,
                   agg.grindingFirstMeter,
                   agg.grindingSecondMeter,
                   agg.hcNapResult,
                   agg.csNapResult,
                   agg.shippingCoaResult,
                   manual.sem_result AS semResult,
                   agg.adhesive1UnprocessedMeter,
                   agg.pressSlotQty,
                   agg.adhesive2Qty,
                   agg.cutRoundQty,
                   agg.pendingInspectionQty,
                   agg.qualifiedInspectionStockQty,
                   agg.goodStockQty,
                   agg.earliestProductionDate,
                   agg.latestProductionDate,
                   COALESCE(demand.demandStockQty, 0) AS demandStockQty,
                   COALESCE(demand.shippingPickedQty, 0) AS shippingPickedQty,
                   COALESCE(demand.shippingInspectionQty, 0) AS shippingInspectionQty,
                   manual.remark AS remark,
                   manual.update_time AS manualUpdateTime
              FROM (
                    SELECT COALESCE(MAX(CASE WHEN LENGTH(norm.modelCode) > 4 THEN norm.modelCode ELSE NULL END),
                                    MAX(norm.modelCode)) AS modelCode,
                           norm.motherBatchNo,
                           norm.motherSegmentBatchNo,
                           SUM(norm.wetMeter) AS wetMeter,
                           SUM(norm.grindingFirstMeter) AS grindingFirstMeter,
                           SUM(norm.grindingSecondMeter) AS grindingSecondMeter,
                           SUM(norm.adhesive1UnprocessedMeter) AS adhesive1UnprocessedMeter,
                           SUM(norm.pressSlotQty) AS pressSlotQty,
                           SUM(norm.adhesive2Qty) AS adhesive2Qty,
                           SUM(norm.cutRoundQty) AS cutRoundQty,
                           SUM(norm.pendingInspectionQty) AS pendingInspectionQty,
                           SUM(norm.qualifiedInspectionStockQty) AS qualifiedInspectionStockQty,
                           SUM(norm.goodStockQty) AS goodStockQty,
                           MIN(norm.earliestProductionDate) AS earliestProductionDate,
                           MAX(norm.latestProductionDate) AS latestProductionDate,
                           NULLIF(GROUP_CONCAT(DISTINCT NULLIF(norm.hcNapResult, '') ORDER BY norm.hcNapResult SEPARATOR '/'), '') AS hcNapResult,
                           NULLIF(GROUP_CONCAT(DISTINCT NULLIF(norm.csNapResult, '') ORDER BY norm.csNapResult SEPARATOR '/'), '') AS csNapResult,
                           NULLIF(GROUP_CONCAT(DISTINCT NULLIF(norm.shippingCoaResult, '') ORDER BY norm.shippingCoaResult SEPARATOR '/'), '') AS shippingCoaResult
                      FROM (
                           """ + NORMALIZED_ROWS_SQL + """
                           ) norm
                     WHERE norm.motherBatchNo IS NOT NULL
                       AND norm.motherBatchNo <> ''
                     GROUP BY norm.motherBatchNo, norm.motherSegmentBatchNo
                   ) agg
              LEFT JOIN mes_pp_mass_stock_manual manual
                     ON manual.deleted = b'0'
                    AND manual.tenant_id = #{tenantId}
                    AND manual.model_code = agg.modelCode
                    AND manual.mother_batch_no = agg.motherBatchNo
                    AND manual.mother_segment_batch_no = agg.motherSegmentBatchNo
              LEFT JOIN (
                    """ + SEGMENT_DEMAND_SQL + """
                   ) demand ON demand.motherBatchNo = agg.motherBatchNo
                            AND demand.motherSegmentBatchNo = agg.motherSegmentBatchNo
                            AND (demand.modelCode = agg.modelCode
                                 OR demand.modelCode LIKE CONCAT(agg.modelCode, '%')
                                 OR agg.modelCode LIKE CONCAT(demand.modelCode, '%'))
            """;

    String FILTER_SQL = """
            WHERE (#{req.keyword} IS NULL OR #{req.keyword} = ''
                    OR t.modelCode LIKE CONCAT('%', #{req.keyword}, '%')
                    OR t.motherBatchNo LIKE CONCAT('%', #{req.keyword}, '%')
                    OR t.motherSegmentBatchNo LIKE CONCAT('%', #{req.keyword}, '%')
                    OR t.semResult LIKE CONCAT('%', #{req.keyword}, '%')
                    OR t.remark LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                    OR t.modelCode LIKE CONCAT('%', #{req.modelCode}, '%'))
              AND (#{req.motherBatchNo} IS NULL OR #{req.motherBatchNo} = ''
                    OR t.motherBatchNo LIKE CONCAT('%', #{req.motherBatchNo}, '%'))
              AND (#{req.motherSegmentBatchNo} IS NULL OR #{req.motherSegmentBatchNo} = ''
                    OR t.motherSegmentBatchNo LIKE CONCAT('%', #{req.motherSegmentBatchNo}, '%'))
              AND (#{req.onlyAdhesive2} IS NULL OR #{req.onlyAdhesive2} = false
                    OR COALESCE(t.adhesive2Qty, 0) > 0)
            """;

    String SHIPPING_DETAIL_FILTER_SQL = """
            WHERE detail.motherSegmentBatchNo = #{motherSegmentBatchNo}
              AND detail.motherBatchNo = #{motherBatchNo}
              AND (#{modelCode} IS NULL OR #{modelCode} = ''
                    OR detail.modelCode IS NULL OR detail.modelCode = ''
                    OR detail.modelCode = #{modelCode}
                    OR detail.modelCode LIKE CONCAT(#{modelCode}, '%')
                    OR #{modelCode} LIKE CONCAT(detail.modelCode, '%'))
            """;

    String DEMAND_DETAIL_SQL = """
            SELECT detail.*
              FROM (
                    SELECT normalized.id,
                           normalized.noticeId,
                           normalized.noticeNo,
                           normalized.sourceType,
                           normalized.customerName,
                           normalized.modelCode,
                           LEFT(normalized.motherSegmentBatchNo, 8) AS motherBatchNo,
                           normalized.motherSegmentBatchNo,
                           normalized.requiredBatchNo,
                           normalized.requiredSliceRange,
                           normalized.batchNo,
                           normalized.sliceBatchNo,
                           normalized.actualSliceBatchNo,
                           normalized.customerProductBatchNo,
                           normalized.internalItemCode,
                           normalized.qty,
                           normalized.lockStatus,
                           normalized.qualityStatus,
                           normalized.shippingInspectionResult,
                           normalized.shippingInspectorName,
                           normalized.shippingInspectionTime,
                           normalized.warehouseName,
                           normalized.locationCode,
                           normalized.locationName,
                           normalized.stockNo,
                           normalized.remark
                      FROM (
                            SELECT cleaned.id,
                                   cleaned.noticeId,
                                   cleaned.noticeNo,
                                   cleaned.sourceType,
                                   cleaned.customerName,
                                   cleaned.modelCode,
                                   CASE
                                       WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9][AB]$'
                                            AND LENGTH(cleaned.cleanBatchNo) >= 12
                                           THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 4)
                                       WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9]$'
                                            AND LENGTH(cleaned.cleanBatchNo) >= 11
                                           THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 3)
                                       ELSE cleaned.cleanBatchNo
                                   END AS motherSegmentBatchNo,
                                   cleaned.requiredBatchNo,
                                   cleaned.requiredSliceRange,
                                   cleaned.batchNo,
                                   cleaned.sliceBatchNo,
                                   cleaned.actualSliceBatchNo,
                                   cleaned.customerProductBatchNo,
                                   cleaned.internalItemCode,
                                   cleaned.qty,
                                   cleaned.lockStatus,
                                   cleaned.qualityStatus,
                                   cleaned.shippingInspectionResult,
                                   cleaned.shippingInspectorName,
                                   cleaned.shippingInspectionTime,
                                   cleaned.warehouseName,
                                   cleaned.locationCode,
                                   cleaned.locationName,
                                   cleaned.stockNo,
                                   cleaned.remark
                              FROM (
                                    SELECT raw.*,
                                           SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(raw.rawBatchNo, '')), ' ', '')), '-J', 1) AS cleanBatchNo
                                      FROM (
                                            SELECT i.id,
                                                   i.notice_id AS noticeId,
                                                   COALESCE(NULLIF(i.notice_no, ''), n.notice_no) AS noticeNo,
                                                   'DEMAND' AS sourceType,
                                                   n.customer_name AS customerName,
                                                   TRIM(COALESCE(i.model_code, n.model_code, '')) AS modelCode,
                                                   n.required_batch_no AS requiredBatchNo,
                                                   n.required_slice_range AS requiredSliceRange,
                                                   i.batch_no AS batchNo,
                                                   i.slice_batch_no AS sliceBatchNo,
                                                   i.actual_slice_batch_no AS actualSliceBatchNo,
                                                   i.customer_product_batch_no AS customerProductBatchNo,
                                                   i.internal_item_code AS internalItemCode,
                                                   COALESCE(NULLIF(i.actual_ship_qty, 0), NULLIF(i.stock_qty, 0),
                                                            NULLIF(i.available_qty, 0), NULLIF(i.locked_qty, 0), 1) AS qty,
                                                   i.lock_status AS lockStatus,
                                                   i.quality_status AS qualityStatus,
                                                   i.shipping_inspection_result AS shippingInspectionResult,
                                                   i.shipping_inspector_name AS shippingInspectorName,
                                                   i.shipping_inspection_time AS shippingInspectionTime,
                                                   i.warehouse_name AS warehouseName,
                                                   COALESCE(NULLIF(i.actual_location_code, ''), i.location_code) AS locationCode,
                                                   COALESCE(NULLIF(i.actual_location_name, ''), i.location_name) AS locationName,
                                                   COALESCE(NULLIF(i.actual_stock_no, ''), i.stock_no) AS stockNo,
                                                   COALESCE(NULLIF(i.actual_remark, ''), NULLIF(i.remark, ''), n.remark) AS remark,
                                                   COALESCE(NULLIF(i.actual_slice_batch_no, ''), NULLIF(i.slice_batch_no, ''),
                                                            NULLIF(i.batch_no, ''), NULLIF(n.required_batch_no, '')) AS rawBatchNo
                                              FROM mes_inv_fg_shipping_notice_item i
                                              LEFT JOIN mes_inv_fg_shipping_notice n ON n.id = i.notice_id AND n.deleted = b'0'
                                             WHERE i.deleted = b'0'
                                               AND i.tenant_id = #{tenantId}
                                               AND (n.product_type IN ('MASS', '量产') OR n.product_type IS NULL OR n.product_type = '')
                                               AND (n.notice_status IS NULL OR n.notice_status NOT IN ('CANCELLED', 'CANCELED', 'CLOSED', 'SHIPPED'))
                                            UNION ALL
                                            SELECT n.id,
                                                   n.id AS noticeId,
                                                   n.notice_no AS noticeNo,
                                                   'DEMAND' AS sourceType,
                                                   n.customer_name AS customerName,
                                                   TRIM(COALESCE(n.model_code, '')) AS modelCode,
                                                   n.required_batch_no AS requiredBatchNo,
                                                   n.required_slice_range AS requiredSliceRange,
                                                   NULL AS batchNo,
                                                   NULL AS sliceBatchNo,
                                                   NULL AS actualSliceBatchNo,
                                                   NULL AS customerProductBatchNo,
                                                   NULL AS internalItemCode,
                                                   COALESCE(NULLIF(n.required_ship_qty, 0), NULLIF(n.notice_qty, 0), 0) AS qty,
                                                   n.notice_status AS lockStatus,
                                                   NULL AS qualityStatus,
                                                   NULL AS shippingInspectionResult,
                                                   NULL AS shippingInspectorName,
                                                   NULL AS shippingInspectionTime,
                                                   NULL AS warehouseName,
                                                   NULL AS locationCode,
                                                   NULL AS locationName,
                                                   NULL AS stockNo,
                                                   n.remark AS remark,
                                                   n.required_batch_no AS rawBatchNo
                                              FROM mes_inv_fg_shipping_notice n
                                             WHERE n.deleted = b'0'
                                               AND n.tenant_id = #{tenantId}
                                               AND (n.product_type IN ('MASS', '量产') OR n.product_type IS NULL OR n.product_type = '')
                                               AND n.notice_status NOT IN ('CANCELLED', 'CANCELED', 'CLOSED', 'SHIPPED')
                                               AND n.required_batch_no IS NOT NULL
                                               AND n.required_batch_no <> ''
                                               AND NOT EXISTS (
                                                   SELECT 1
                                                     FROM mes_inv_fg_shipping_notice_item i2
                                                    WHERE i2.deleted = b'0'
                                                      AND i2.tenant_id = n.tenant_id
                                                      AND i2.notice_id = n.id
                                                    LIMIT 1
                                               )
                                           ) raw
                                   ) cleaned
                           ) normalized
                     WHERE normalized.motherSegmentBatchNo IS NOT NULL
                       AND normalized.motherSegmentBatchNo <> ''
                   ) detail
            """ + SHIPPING_DETAIL_FILTER_SQL + """
             ORDER BY detail.noticeNo ASC, detail.actualSliceBatchNo ASC, detail.sliceBatchNo ASC, detail.id ASC
            """;

    String PICKED_DETAIL_SQL = """
            SELECT detail.*
              FROM (
                    SELECT normalized.id,
                           normalized.noticeId,
                           normalized.noticeNo,
                           normalized.sourceType,
                           normalized.customerName,
                           normalized.modelCode,
                           LEFT(normalized.motherSegmentBatchNo, 8) AS motherBatchNo,
                           normalized.motherSegmentBatchNo,
                           normalized.requiredBatchNo,
                           normalized.requiredSliceRange,
                           normalized.batchNo,
                           normalized.sliceBatchNo,
                           normalized.actualSliceBatchNo,
                           normalized.customerProductBatchNo,
                           normalized.internalItemCode,
                           normalized.qty,
                           normalized.lockStatus,
                           normalized.qualityStatus,
                           normalized.shippingInspectionResult,
                           normalized.shippingInspectorName,
                           normalized.shippingInspectionTime,
                           normalized.warehouseName,
                           normalized.locationCode,
                           normalized.locationName,
                           normalized.stockNo,
                           normalized.remark
                      FROM (
                            SELECT cleaned.id,
                                   cleaned.noticeId,
                                   cleaned.noticeNo,
                                   cleaned.sourceType,
                                   cleaned.customerName,
                                   cleaned.modelCode,
                                   CASE
                                       WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9][AB]$'
                                            AND LENGTH(cleaned.cleanBatchNo) >= 12
                                           THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 4)
                                       WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9]$'
                                            AND LENGTH(cleaned.cleanBatchNo) >= 11
                                           THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 3)
                                       ELSE cleaned.cleanBatchNo
                                   END AS motherSegmentBatchNo,
                                   cleaned.requiredBatchNo,
                                   cleaned.requiredSliceRange,
                                   cleaned.batchNo,
                                   cleaned.sliceBatchNo,
                                   cleaned.actualSliceBatchNo,
                                   cleaned.customerProductBatchNo,
                                   cleaned.internalItemCode,
                                   cleaned.qty,
                                   cleaned.lockStatus,
                                   cleaned.qualityStatus,
                                   cleaned.shippingInspectionResult,
                                   cleaned.shippingInspectorName,
                                   cleaned.shippingInspectionTime,
                                   cleaned.warehouseName,
                                   cleaned.locationCode,
                                   cleaned.locationName,
                                   cleaned.stockNo,
                                   cleaned.remark
                              FROM (
                                    SELECT p.id,
                                           p.notice_id AS noticeId,
                                           COALESCE(NULLIF(p.notice_no, ''), n.notice_no) AS noticeNo,
                                           'PICKED' AS sourceType,
                                           n.customer_name AS customerName,
                                           TRIM(COALESCE(p.model_code, n.model_code, '')) AS modelCode,
                                           n.required_batch_no AS requiredBatchNo,
                                           n.required_slice_range AS requiredSliceRange,
                                           p.batch_no AS batchNo,
                                           p.slice_batch_no AS sliceBatchNo,
                                           p.actual_slice_batch_no AS actualSliceBatchNo,
                                           p.customer_product_batch_no AS customerProductBatchNo,
                                           p.internal_item_code AS internalItemCode,
                                           COALESCE(NULLIF(p.actual_ship_qty, 0), NULLIF(p.stock_qty, 0),
                                                    NULLIF(p.available_qty, 0), NULLIF(p.locked_qty, 0), 1) AS qty,
                                           p.lock_status AS lockStatus,
                                           p.quality_status AS qualityStatus,
                                           p.shipping_inspection_result AS shippingInspectionResult,
                                           p.shipping_inspector_name AS shippingInspectorName,
                                           p.shipping_inspection_time AS shippingInspectionTime,
                                           p.warehouse_name AS warehouseName,
                                           p.location_code AS locationCode,
                                           p.location_name AS locationName,
                                           p.stock_no AS stockNo,
                                           COALESCE(NULLIF(p.remark, ''), n.remark) AS remark,
                                           COALESCE(NULLIF(p.actual_slice_batch_no, ''), NULLIF(p.slice_batch_no, ''),
                                                    NULLIF(p.batch_no, '')) AS rawBatchNo,
                                           SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(p.actual_slice_batch_no, ''),
                                                    NULLIF(p.slice_batch_no, ''), NULLIF(p.batch_no, ''), '')), ' ', '')), '-J', 1) AS cleanBatchNo
                                      FROM mes_inv_fg_shipping_pick_item p
                                      LEFT JOIN mes_inv_fg_shipping_notice n ON n.id = p.notice_id AND n.deleted = b'0'
                                     WHERE p.deleted = b'0'
                                       AND p.tenant_id = #{tenantId}
                                       AND (n.product_type IN ('MASS', '量产') OR n.product_type IS NULL OR n.product_type = '')
                                       AND (p.lock_status IS NULL OR p.lock_status NOT IN ('CANCELLED', 'CANCELED'))
                                   ) cleaned
                           ) normalized
                     WHERE normalized.motherSegmentBatchNo IS NOT NULL
                       AND normalized.motherSegmentBatchNo <> ''
                   ) detail
            """ + SHIPPING_DETAIL_FILTER_SQL + """
             ORDER BY detail.noticeNo ASC, detail.actualSliceBatchNo ASC, detail.sliceBatchNo ASC, detail.id ASC
            """;

    String INSPECTION_DETAIL_SQL = """
            SELECT detail.*
              FROM (
                    SELECT normalized.id,
                           normalized.noticeId,
                           normalized.noticeNo,
                           normalized.sourceType,
                           normalized.customerName,
                           normalized.modelCode,
                           LEFT(normalized.motherSegmentBatchNo, 8) AS motherBatchNo,
                           normalized.motherSegmentBatchNo,
                           normalized.requiredBatchNo,
                           normalized.requiredSliceRange,
                           normalized.batchNo,
                           normalized.sliceBatchNo,
                           normalized.actualSliceBatchNo,
                           normalized.customerProductBatchNo,
                           normalized.internalItemCode,
                           normalized.qty,
                           normalized.lockStatus,
                           normalized.qualityStatus,
                           normalized.shippingInspectionResult,
                           normalized.shippingInspectorName,
                           normalized.shippingInspectionTime,
                           normalized.warehouseName,
                           normalized.locationCode,
                           normalized.locationName,
                           normalized.stockNo,
                           normalized.remark
                      FROM (
                            SELECT cleaned.id,
                                   cleaned.noticeId,
                                   cleaned.noticeNo,
                                   cleaned.sourceType,
                                   cleaned.customerName,
                                   cleaned.modelCode,
                                   CASE
                                       WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9][AB]$'
                                            AND LENGTH(cleaned.cleanBatchNo) >= 12
                                           THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 4)
                                       WHEN cleaned.cleanBatchNo REGEXP '[0-9][0-9][0-9]$'
                                            AND LENGTH(cleaned.cleanBatchNo) >= 11
                                           THEN LEFT(cleaned.cleanBatchNo, LENGTH(cleaned.cleanBatchNo) - 3)
                                       ELSE cleaned.cleanBatchNo
                                   END AS motherSegmentBatchNo,
                                   cleaned.requiredBatchNo,
                                   cleaned.requiredSliceRange,
                                   cleaned.batchNo,
                                   cleaned.sliceBatchNo,
                                   cleaned.actualSliceBatchNo,
                                   cleaned.customerProductBatchNo,
                                   cleaned.internalItemCode,
                                   cleaned.qty,
                                   cleaned.lockStatus,
                                   cleaned.qualityStatus,
                                   cleaned.shippingInspectionResult,
                                   cleaned.shippingInspectorName,
                                   cleaned.shippingInspectionTime,
                                   cleaned.warehouseName,
                                   cleaned.locationCode,
                                   cleaned.locationName,
                                   cleaned.stockNo,
                                   cleaned.remark
                              FROM (
                                    SELECT p.id,
                                           p.notice_id AS noticeId,
                                           COALESCE(NULLIF(p.notice_no, ''), n.notice_no) AS noticeNo,
                                           'INSPECTION' AS sourceType,
                                           n.customer_name AS customerName,
                                           TRIM(COALESCE(p.model_code, n.model_code, '')) AS modelCode,
                                           n.required_batch_no AS requiredBatchNo,
                                           n.required_slice_range AS requiredSliceRange,
                                           p.batch_no AS batchNo,
                                           p.slice_batch_no AS sliceBatchNo,
                                           p.actual_slice_batch_no AS actualSliceBatchNo,
                                           p.customer_product_batch_no AS customerProductBatchNo,
                                           p.internal_item_code AS internalItemCode,
                                           COALESCE(NULLIF(p.actual_ship_qty, 0), NULLIF(p.stock_qty, 0),
                                                    NULLIF(p.available_qty, 0), NULLIF(p.locked_qty, 0), 1) AS qty,
                                           p.lock_status AS lockStatus,
                                           p.quality_status AS qualityStatus,
                                           p.shipping_inspection_result AS shippingInspectionResult,
                                           p.shipping_inspector_name AS shippingInspectorName,
                                           p.shipping_inspection_time AS shippingInspectionTime,
                                           p.warehouse_name AS warehouseName,
                                           p.location_code AS locationCode,
                                           p.location_name AS locationName,
                                           p.stock_no AS stockNo,
                                           COALESCE(NULLIF(p.remark, ''), n.remark) AS remark,
                                           COALESCE(NULLIF(p.actual_slice_batch_no, ''), NULLIF(p.slice_batch_no, ''),
                                                    NULLIF(p.batch_no, '')) AS rawBatchNo,
                                           SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(p.actual_slice_batch_no, ''),
                                                    NULLIF(p.slice_batch_no, ''), NULLIF(p.batch_no, ''), '')), ' ', '')), '-J', 1) AS cleanBatchNo
                                      FROM mes_inv_fg_shipping_pick_item p
                                      LEFT JOIN mes_inv_fg_shipping_notice n ON n.id = p.notice_id AND n.deleted = b'0'
                                     WHERE p.deleted = b'0'
                                       AND p.tenant_id = #{tenantId}
                                       AND (n.product_type IN ('MASS', '量产') OR n.product_type IS NULL OR n.product_type = '')
                                       AND (p.lock_status IS NULL OR p.lock_status NOT IN ('CANCELLED', 'CANCELED'))
                                       AND (p.shipping_inspection_result IS NOT NULL OR p.shipping_inspection_time IS NOT NULL)
                                   ) cleaned
                           ) normalized
                     WHERE normalized.motherSegmentBatchNo IS NOT NULL
                       AND normalized.motherSegmentBatchNo <> ''
                   ) detail
            """ + SHIPPING_DETAIL_FILTER_SQL + """
             ORDER BY detail.noticeNo ASC, detail.actualSliceBatchNo ASC, detail.sliceBatchNo ASC, detail.id ASC
            """;

    @Select("SELECT COUNT(1) FROM (" + AGGREGATE_SQL + ") t " + FILTER_SQL)
    Long selectCount(@Param("tenantId") Long tenantId, @Param("req") HcMassStockPageReqVO reqVO);

    @Select("SELECT t.* FROM (" + AGGREGATE_SQL + ") t " + FILTER_SQL
            + " ORDER BY t.modelCode ASC, t.motherBatchNo ASC, t.motherSegmentBatchNo ASC LIMIT #{offset}, #{pageSize}")
    List<HcMassStockRespVO> selectPage(@Param("tenantId") Long tenantId,
                                       @Param("req") HcMassStockPageReqVO reqVO,
                                       @Param("offset") int offset,
                                       @Param("pageSize") int pageSize);

    @Select("SELECT t.* FROM (" + AGGREGATE_SQL + ") t " + FILTER_SQL
            + " ORDER BY t.modelCode ASC, t.motherBatchNo ASC, t.motherSegmentBatchNo ASC")
    List<HcMassStockRespVO> selectList(@Param("tenantId") Long tenantId,
                                       @Param("req") HcMassStockPageReqVO reqVO);

    @Select(DEMAND_DETAIL_SQL)
    List<HcMassStockShippingDetailRespVO> selectDemandDetailList(@Param("tenantId") Long tenantId,
                                                                 @Param("modelCode") String modelCode,
                                                                 @Param("motherBatchNo") String motherBatchNo,
                                                                 @Param("motherSegmentBatchNo") String motherSegmentBatchNo);

    @Select(PICKED_DETAIL_SQL)
    List<HcMassStockShippingDetailRespVO> selectPickedDetailList(@Param("tenantId") Long tenantId,
                                                                 @Param("modelCode") String modelCode,
                                                                 @Param("motherBatchNo") String motherBatchNo,
                                                                 @Param("motherSegmentBatchNo") String motherSegmentBatchNo);

    @Select(INSPECTION_DETAIL_SQL)
    List<HcMassStockShippingDetailRespVO> selectInspectionDetailList(@Param("tenantId") Long tenantId,
                                                                     @Param("modelCode") String modelCode,
                                                                     @Param("motherBatchNo") String motherBatchNo,
                                                                     @Param("motherSegmentBatchNo") String motherSegmentBatchNo);

    @Select("""
            SELECT fs.id,
                   fs.stock_no AS stockNo,
                   fs.outer_box_no AS outerBoxNo,
                   fs.inner_unit_no AS innerUnitNo,
                   fs.slice_batch_no AS sliceBatchNo,
                   fs.model_code AS modelCode,
                   fs.batch_no AS batchNo,
                   fs.qty,
                   fs.quality_status AS qualityStatus,
                   fs.warehouse_name AS warehouseName,
                   fs.location_code AS locationCode,
                   fs.location_name AS locationName,
                   fs.inbound_time AS inboundTime
              FROM mes_inv_finished_stock fs
             WHERE fs.deleted = b'0'
               AND fs.tenant_id = #{tenantId}
               AND fs.quality_status IN ('OK', '合格')
               AND fs.stock_status NOT IN ('CANCELLED', 'CANCELED', 'OUTBOUND', 'SHIPPED')
               AND (#{modelCode} IS NULL OR #{modelCode} = ''
                    OR fs.model_code = #{modelCode}
                    OR fs.model_code LIKE CONCAT(#{modelCode}, '%'))
               AND (
                    CASE
                        WHEN SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1)
                             REGEXP '[0-9][0-9][0-9][AB]$'
                             AND LENGTH(SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1)) >= 12
                            THEN LEFT(SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1),
                                      LENGTH(SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1)) - 4)
                        WHEN SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1)
                             REGEXP '[0-9][0-9][0-9]$'
                             AND LENGTH(SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1)) >= 11
                            THEN LEFT(SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1),
                                      LENGTH(SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1)) - 3)
                        ELSE SUBSTRING_INDEX(UPPER(REPLACE(TRIM(COALESCE(NULLIF(fs.slice_batch_no, ''), fs.batch_no, '')), ' ', '')), '-J', 1)
                    END
               ) = #{motherSegmentBatchNo}
               AND LEFT(#{motherSegmentBatchNo}, 8) = #{motherBatchNo}
             ORDER BY fs.inbound_time ASC, fs.slice_batch_no ASC, fs.stock_no ASC
            """)
    List<HcMassStockGoodStockRespVO> selectGoodStockList(@Param("tenantId") Long tenantId,
                                                         @Param("modelCode") String modelCode,
                                                         @Param("motherBatchNo") String motherBatchNo,
                                                         @Param("motherSegmentBatchNo") String motherSegmentBatchNo);

}
