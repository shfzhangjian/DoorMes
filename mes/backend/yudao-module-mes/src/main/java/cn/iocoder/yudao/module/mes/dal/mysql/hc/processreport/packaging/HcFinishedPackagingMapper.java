package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSlicePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgShippingBatchCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgShippingBatchCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcFinishedPackagingMapper {

    @Select("""
            SELECT *
            FROM (
              SELECT
                'INBOUND' AS boxType,
                '成品包装入库' AS boxTypeName,
                u.id AS id,
                u.inner_unit_no AS boxNo,
                CONCAT('FGI-', u.inner_unit_no) AS bizNo,
                u.plan_no AS sourceNo,
                u.batch_no AS motherSegmentBatchNo,
                u.material_code AS materialCode,
                u.material_name AS materialName,
                u.model_code AS modelCode,
                u.target_qty AS targetQty,
                u.current_qty AS currentQty,
                u.unit_status AS status,
                u.label_no AS labelNo,
                u.print_count AS printCount,
                u.last_print_time AS lastPrintTime,
                u.recorder_name AS recorderName,
                u.recorder_time AS recorderTime,
                u.remark AS remark,
                u.create_time AS createTime
              FROM mes_sfc_inner_pack_unit u
              WHERE u.deleted = 0
                AND (#{boxType} IS NULL OR #{boxType} = '' OR #{boxType} = 'ALL' OR #{boxType} = 'INBOUND')
                AND (#{keyword} IS NULL OR #{keyword} = ''
                  OR u.inner_unit_no LIKE CONCAT('%', #{keyword}, '%')
                  OR u.plan_no LIKE CONCAT('%', #{keyword}, '%')
                  OR u.batch_no LIKE CONCAT('%', #{keyword}, '%')
                  OR u.material_code LIKE CONCAT('%', #{keyword}, '%')
                  OR u.material_name LIKE CONCAT('%', #{keyword}, '%')
                  OR u.model_code LIKE CONCAT('%', #{keyword}, '%'))
              UNION ALL
              SELECT
                'OUTBOUND' AS boxType,
                '发货包装出库' AS boxTypeName,
                b.id AS id,
                b.outbound_box_no AS boxNo,
                b.outbound_no AS bizNo,
                COALESCE(o.shipping_order_no, b.outbound_no) AS sourceNo,
                NULL AS motherSegmentBatchNo,
                b.material_code AS materialCode,
                b.material_name AS materialName,
                b.model_code AS modelCode,
                b.target_qty AS targetQty,
                b.current_qty AS currentQty,
                b.box_status AS status,
                b.label_no AS labelNo,
                b.print_count AS printCount,
                b.last_print_time AS lastPrintTime,
                b.recorder_name AS recorderName,
                b.recorder_time AS recorderTime,
                b.remark AS remark,
                b.create_time AS createTime
              FROM mes_inv_fg_outbound_box b
              LEFT JOIN mes_inv_fg_outbound_order o
                ON o.id = b.outbound_order_id
               AND o.deleted = 0
              WHERE b.deleted = 0
                AND (#{boxType} IS NULL OR #{boxType} = '' OR #{boxType} = 'ALL' OR #{boxType} = 'OUTBOUND')
                AND (#{keyword} IS NULL OR #{keyword} = ''
                  OR b.outbound_box_no LIKE CONCAT('%', #{keyword}, '%')
                  OR b.outbound_no LIKE CONCAT('%', #{keyword}, '%')
                  OR o.shipping_order_no LIKE CONCAT('%', #{keyword}, '%')
                  OR b.erp_order_no LIKE CONCAT('%', #{keyword}, '%')
                  OR b.material_code LIKE CONCAT('%', #{keyword}, '%')
                  OR b.material_name LIKE CONCAT('%', #{keyword}, '%')
                  OR b.model_code LIKE CONCAT('%', #{keyword}, '%'))
            ) box_ledger
            ORDER BY createTime DESC, id DESC
            LIMIT 500
            """)
    List<PackageBoxRespVO> selectPackageBoxList(@Param("boxType") String boxType,
                                                @Param("keyword") String keyword);

    @Select("""
            SELECT COUNT(1)
            FROM (
              SELECT
                'CUT_ROUND_REPORT' AS sourceType,
                cr.id AS sourceId,
                cr.production_batch_no AS sliceBatchNo,
                CASE
                  WHEN COALESCE(cr.extra_json, '') REGEXP '"printCount"[[:space:]]*:[[:space:]]*[1-9][0-9]*'
                    THEN 'PRINTED'
                  ELSE 'UNPRINTED'
                END AS printStatus,
                CASE
                  WHEN EXISTS (
                    SELECT 1 FROM mes_inv_finished_stock fs
                    WHERE fs.slice_batch_no = cr.production_batch_no AND fs.deleted = 0
                  ) THEN 'IN_STOCK'
                  WHEN EXISTS (
                    SELECT 1 FROM mes_sfc_inner_pack_unit_item pi
                    WHERE pi.source_cut_round_report_id = cr.id AND pi.deleted = 0
                  ) THEN 'PACKED'
                  ELSE 'WAIT_PACKAGING'
                END AS businessStatus,
                COALESCE(cr.confirmer_time, cr.end_time, cr.recorder_time, cr.create_time) AS sortTime
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0
                AND cr.report_status IN ('SUBMITTED', 'CONFIRMED')
                AND cr.production_batch_no IS NOT NULL
                AND cr.production_batch_no != ''
              UNION ALL
              SELECT
                'MANUAL_HISTORY' AS sourceType,
                mp.id AS sourceId,
                mp.slice_batch_no AS sliceBatchNo,
                CASE WHEN COALESCE(mp.print_count, 0) > 0 THEN 'PRINTED' ELSE 'UNPRINTED' END AS printStatus,
                CASE
                  WHEN mp.record_status = 'INBOUNDED' OR EXISTS (
                    SELECT 1 FROM mes_inv_finished_stock fs
                    WHERE fs.slice_batch_no = mp.slice_batch_no AND fs.deleted = 0
                  ) THEN 'IN_STOCK'
                  WHEN mp.record_status = 'PACKED' OR mp.inner_unit_id IS NOT NULL THEN 'PACKED'
                  ELSE 'WAIT_PACKAGING'
                END AS businessStatus,
                COALESCE(mp.recorder_time, mp.create_time) AS sortTime
              FROM mes_sfc_packaging_manual_piece mp
              WHERE mp.deleted = 0
                AND mp.record_status != 'VOID'
                AND mp.slice_batch_no IS NOT NULL
                AND mp.slice_batch_no != ''
            ) candidate
            WHERE (#{req.sourceType} IS NULL OR #{req.sourceType} = '' OR candidate.sourceType = #{req.sourceType})
              AND (#{req.printStatus} IS NULL OR #{req.printStatus} = '' OR candidate.printStatus = #{req.printStatus})
              AND (#{req.businessStatus} IS NULL OR #{req.businessStatus} = '' OR candidate.businessStatus = #{req.businessStatus})
              AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                OR candidate.sliceBatchNo LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
            """)
    Long countPieceLabelCandidatePage(@Param("req") PieceLabelCandidatePageReqVO reqVO);

    @Select("""
            SELECT candidate.sourceType, candidate.sourceId
            FROM (
              SELECT
                'CUT_ROUND_REPORT' AS sourceType,
                cr.id AS sourceId,
                cr.production_batch_no AS sliceBatchNo,
                CASE
                  WHEN COALESCE(cr.extra_json, '') REGEXP '"printCount"[[:space:]]*:[[:space:]]*[1-9][0-9]*'
                    THEN 'PRINTED'
                  ELSE 'UNPRINTED'
                END AS printStatus,
                CASE
                  WHEN EXISTS (
                    SELECT 1 FROM mes_inv_finished_stock fs
                    WHERE fs.slice_batch_no = cr.production_batch_no AND fs.deleted = 0
                  ) THEN 'IN_STOCK'
                  WHEN EXISTS (
                    SELECT 1 FROM mes_sfc_inner_pack_unit_item pi
                    WHERE pi.source_cut_round_report_id = cr.id AND pi.deleted = 0
                  ) THEN 'PACKED'
                  ELSE 'WAIT_PACKAGING'
                END AS businessStatus,
                COALESCE(cr.confirmer_time, cr.end_time, cr.recorder_time, cr.create_time) AS sortTime
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0
                AND cr.report_status IN ('SUBMITTED', 'CONFIRMED')
                AND cr.production_batch_no IS NOT NULL
                AND cr.production_batch_no != ''
              UNION ALL
              SELECT
                'MANUAL_HISTORY' AS sourceType,
                mp.id AS sourceId,
                mp.slice_batch_no AS sliceBatchNo,
                CASE WHEN COALESCE(mp.print_count, 0) > 0 THEN 'PRINTED' ELSE 'UNPRINTED' END AS printStatus,
                CASE
                  WHEN mp.record_status = 'INBOUNDED' OR EXISTS (
                    SELECT 1 FROM mes_inv_finished_stock fs
                    WHERE fs.slice_batch_no = mp.slice_batch_no AND fs.deleted = 0
                  ) THEN 'IN_STOCK'
                  WHEN mp.record_status = 'PACKED' OR mp.inner_unit_id IS NOT NULL THEN 'PACKED'
                  ELSE 'WAIT_PACKAGING'
                END AS businessStatus,
                COALESCE(mp.recorder_time, mp.create_time) AS sortTime
              FROM mes_sfc_packaging_manual_piece mp
              WHERE mp.deleted = 0
                AND mp.record_status != 'VOID'
                AND mp.slice_batch_no IS NOT NULL
                AND mp.slice_batch_no != ''
            ) candidate
            WHERE (#{req.sourceType} IS NULL OR #{req.sourceType} = '' OR candidate.sourceType = #{req.sourceType})
              AND (#{req.printStatus} IS NULL OR #{req.printStatus} = '' OR candidate.printStatus = #{req.printStatus})
              AND (#{req.businessStatus} IS NULL OR #{req.businessStatus} = '' OR candidate.businessStatus = #{req.businessStatus})
              AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                OR candidate.sliceBatchNo LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
            ORDER BY CASE WHEN candidate.printStatus = 'UNPRINTED' THEN 0 ELSE 1 END,
                     candidate.sortTime DESC,
                     candidate.sourceId DESC
            LIMIT #{offset}, #{pageSize}
            """)
    List<PieceLabelRespVO> selectPieceLabelCandidatePage(@Param("req") PieceLabelCandidatePageReqVO reqVO,
                                                          @Param("offset") int offset,
                                                          @Param("pageSize") int pageSize);

    @Select("""
            SELECT COUNT(1)
            FROM mes_sfc_cut_round_report cr
            LEFT JOIN mes_inv_finished_stock fs
              ON fs.slice_batch_no = cr.production_batch_no
             AND fs.deleted = 0
            LEFT JOIN (
              SELECT source_cut_round_report_id, MIN(id) AS id
              FROM mes_sfc_inner_pack_unit_item
              WHERE deleted = 0
              GROUP BY source_cut_round_report_id
            ) packed ON packed.source_cut_round_report_id = cr.id
            WHERE cr.deleted = 0
              AND cr.inspection_status = #{targetInspectionStatus}
              AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                OR cr.inspection_task_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.parent_production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.model_code LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.inspectionTaskNo} IS NULL OR #{req.inspectionTaskNo} = ''
                OR cr.inspection_task_no LIKE CONCAT('%', #{req.inspectionTaskNo}, '%'))
              AND (#{req.parentProductionBatchNo} IS NULL OR #{req.parentProductionBatchNo} = ''
                OR cr.parent_production_batch_no LIKE CONCAT('%', #{req.parentProductionBatchNo}, '%'))
              AND (#{req.productionBatchNo} IS NULL OR #{req.productionBatchNo} = ''
                OR cr.production_batch_no LIKE CONCAT('%', #{req.productionBatchNo}, '%'))
              AND (#{req.productionDateStart} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) >= #{req.productionDateStart})
              AND (#{req.productionDateEnd} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) <= #{req.productionDateEnd})
              AND (#{req.inspectionResult} IS NULL OR #{req.inspectionResult} = ''
                OR cr.inspection_result = #{req.inspectionResult})
              AND (#{req.packagingQualityStatus} IS NULL OR #{req.packagingQualityStatus} = ''
                OR (UPPER(#{req.packagingQualityStatus}) = 'NG' AND (
                  UPPER(COALESCE(cr.inspection_result, '')) = 'NG'
                  OR EXISTS (
                    SELECT 1
                    FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND fai.source_report_no LIKE '%-COA-%'
                      AND UPPER(TRIM(fai.product_batch_no)) LIKE CONCAT(
                        CASE
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-J[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-J', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-S[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-S', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 4)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 3)
                          ELSE UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))
                        END,
                        '%')
                      AND (UPPER(COALESCE(fai.judgment, '')) = 'NG'
                        OR UPPER(COALESCE(fai.status, '')) = 'REJECTED')
                  )
                ))
                OR (UPPER(#{req.packagingQualityStatus}) = 'OK' AND (
                  UPPER(COALESCE(cr.inspection_result, '')) != 'NG'
                  AND NOT EXISTS (
                    SELECT 1
                    FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND fai.source_report_no LIKE '%-COA-%'
                      AND UPPER(TRIM(fai.product_batch_no)) LIKE CONCAT(
                        CASE
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-J[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-J', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-S[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-S', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 4)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 3)
                          ELSE UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))
                        END,
                        '%')
                      AND (UPPER(COALESCE(fai.judgment, '')) = 'NG'
                        OR UPPER(COALESCE(fai.status, '')) = 'REJECTED')
                  )
                )))
              AND (#{req.locationKeyword} IS NULL OR #{req.locationKeyword} = ''
                OR fs.location_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                OR fs.location_name LIKE CONCAT('%', #{req.locationKeyword}, '%'))
              AND (#{req.stockStatus} IS NULL OR #{req.stockStatus} = ''
                OR (#{req.stockStatus} = 'WAIT_INBOUND' AND fs.id IS NULL AND packed.id IS NULL)
                OR (#{req.stockStatus} = 'INBOUNDED' AND fs.stock_status IN ('INBOUNDED', 'AVAILABLE'))
                OR (#{req.stockStatus} = 'OUTBOUND_LOCKED' AND fs.stock_status IN ('OUTBOUND_LOCKED', 'ALLOCATED'))
                OR fs.stock_status = #{req.stockStatus})
            """)
    Long countInspectionSlicePage(@Param("req") InspectionSlicePageReqVO reqVO,
                                  @Param("targetInspectionStatus") String targetInspectionStatus);

    @Select("""
            SELECT
              cr.id AS sourceCutRoundReportId,
              cr.plan_id AS planId,
              cr.plan_no AS planNo,
              cr.plan_operation_id AS planOperationId,
              cr.parent_production_batch_no AS parentProductionBatchNo,
              cr.inspection_task_no AS inspectionTaskNo,
              cr.production_batch_no AS sliceBatchNo,
              cr.production_batch_no AS productionBatchNo,
              cr.material_code AS materialCode,
              cr.material_name AS materialName,
              cr.model_code AS modelCode,
              COALESCE(fs.production_date, DATE(cr.confirmer_time)) AS productionDate,
              COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) AS expiryDate,
              cr.inspection_status AS inspectionStatus,
              cr.inspection_result AS inspectionResult,
              CASE
                WHEN UPPER(COALESCE(cr.inspection_result, '')) = 'NG'
                  OR EXISTS (
                    SELECT 1
                    FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND fai.source_report_no LIKE '%-COA-%'
                      AND UPPER(TRIM(fai.product_batch_no)) LIKE CONCAT(
                        CASE
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-J[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-J', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-S[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-S', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 4)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 3)
                          ELSE UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))
                        END,
                        '%')
                      AND (UPPER(COALESCE(fai.judgment, '')) = 'NG'
                        OR UPPER(COALESCE(fai.status, '')) = 'REJECTED')
                  )
                  THEN 'NG'
                ELSE 'OK'
              END AS packagingQualityStatus,
              cr.quality_risk_flag AS qualityRiskFlag,
              cr.quality_risk_snapshot_json AS qualityRiskSnapshotJson,
              cr.inspection_remark AS inspectionRemark,
              cr.inspector_name AS inspectorName,
              cr.inspection_time AS inspectionTime,
              CASE
                WHEN fs.id IS NULL THEN 'WAIT_INBOUND'
                WHEN fs.stock_status = 'AVAILABLE' THEN 'INBOUNDED'
                WHEN fs.stock_status = 'ALLOCATED' THEN 'OUTBOUND_LOCKED'
                ELSE fs.stock_status
              END AS stockStatus,
              fs.stock_no AS stockNo,
              fs.location_code AS currentLocationCode,
              fs.location_name AS currentLocationName,
              COALESCE(fs.inbound_time, io.inbound_time) AS inboundTime,
              io.inbound_user_name AS inboundUserName,
              obi.outbound_box_no AS outboundLocation,
              oo.confirmer_time AS outboundTime,
              oo.confirmer_name AS outboundRecorderName,
              NULL AS outboundQualityNo
            FROM mes_sfc_cut_round_report cr
            LEFT JOIN mes_inv_finished_stock fs
              ON fs.slice_batch_no = cr.production_batch_no
             AND fs.deleted = 0
            LEFT JOIN (
              SELECT source_cut_round_report_id, MIN(id) AS id
              FROM mes_sfc_inner_pack_unit_item
              WHERE deleted = 0
              GROUP BY source_cut_round_report_id
            ) packed ON packed.source_cut_round_report_id = cr.id
            LEFT JOIN mes_inv_fg_inbound_order io
              ON io.inbound_no = fs.inbound_no
             AND io.deleted = 0
            LEFT JOIN mes_inv_fg_outbound_box_item obi
              ON obi.finished_stock_id = fs.id
             AND obi.deleted = 0
            LEFT JOIN mes_inv_fg_outbound_order oo
              ON oo.id = obi.outbound_order_id
             AND oo.deleted = 0
            WHERE cr.deleted = 0
              AND cr.inspection_status = #{targetInspectionStatus}
              AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                OR cr.inspection_task_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.parent_production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.production_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR cr.model_code LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.inspectionTaskNo} IS NULL OR #{req.inspectionTaskNo} = ''
                OR cr.inspection_task_no LIKE CONCAT('%', #{req.inspectionTaskNo}, '%'))
              AND (#{req.parentProductionBatchNo} IS NULL OR #{req.parentProductionBatchNo} = ''
                OR cr.parent_production_batch_no LIKE CONCAT('%', #{req.parentProductionBatchNo}, '%'))
              AND (#{req.productionBatchNo} IS NULL OR #{req.productionBatchNo} = ''
                OR cr.production_batch_no LIKE CONCAT('%', #{req.productionBatchNo}, '%'))
              AND (#{req.productionDateStart} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) >= #{req.productionDateStart})
              AND (#{req.productionDateEnd} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) <= #{req.productionDateEnd})
              AND (#{req.inspectionResult} IS NULL OR #{req.inspectionResult} = ''
                OR cr.inspection_result = #{req.inspectionResult})
              AND (#{req.packagingQualityStatus} IS NULL OR #{req.packagingQualityStatus} = ''
                OR (UPPER(#{req.packagingQualityStatus}) = 'NG' AND (
                  UPPER(COALESCE(cr.inspection_result, '')) = 'NG'
                  OR EXISTS (
                    SELECT 1
                    FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND fai.source_report_no LIKE '%-COA-%'
                      AND UPPER(TRIM(fai.product_batch_no)) LIKE CONCAT(
                        CASE
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-J[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-J', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-S[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-S', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 4)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 3)
                          ELSE UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))
                        END,
                        '%')
                      AND (UPPER(COALESCE(fai.judgment, '')) = 'NG'
                        OR UPPER(COALESCE(fai.status, '')) = 'REJECTED')
                  )
                ))
                OR (UPPER(#{req.packagingQualityStatus}) = 'OK' AND (
                  UPPER(COALESCE(cr.inspection_result, '')) != 'NG'
                  AND NOT EXISTS (
                    SELECT 1
                    FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND fai.source_report_no LIKE '%-COA-%'
                      AND UPPER(TRIM(fai.product_batch_no)) LIKE CONCAT(
                        CASE
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-J[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-J', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '-S[0-9]+$'
                            THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))), '-S', 1)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 4)
                          WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9]$'
                            THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no))),
                                      CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))) - 3)
                          ELSE UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), cr.production_batch_no)))
                        END,
                        '%')
                      AND (UPPER(COALESCE(fai.judgment, '')) = 'NG'
                        OR UPPER(COALESCE(fai.status, '')) = 'REJECTED')
                  )
                )))
              AND (#{req.locationKeyword} IS NULL OR #{req.locationKeyword} = ''
                OR fs.location_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                OR fs.location_name LIKE CONCAT('%', #{req.locationKeyword}, '%'))
              AND (#{req.stockStatus} IS NULL OR #{req.stockStatus} = ''
                OR (#{req.stockStatus} = 'WAIT_INBOUND' AND fs.id IS NULL AND packed.id IS NULL)
                OR (#{req.stockStatus} = 'INBOUNDED' AND fs.stock_status IN ('INBOUNDED', 'AVAILABLE'))
                OR (#{req.stockStatus} = 'OUTBOUND_LOCKED' AND fs.stock_status IN ('OUTBOUND_LOCKED', 'ALLOCATED'))
                OR fs.stock_status = #{req.stockStatus})
            ORDER BY cr.inspection_time IS NULL ASC, cr.inspection_time DESC, cr.id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<InspectionSliceRespVO> selectInspectionSlicePage(@Param("req") InspectionSlicePageReqVO reqVO,
                                                          @Param("targetInspectionStatus") String targetInspectionStatus,
                                                          @Param("offset") Integer offset,
                                                          @Param("limit") Integer limit);

    @Select("""
            SELECT COUNT(1)
            FROM mes_inv_finished_stock fs
            LEFT JOIN mes_sfc_inner_pack_unit u
              ON u.inner_unit_no = fs.inner_unit_no
             AND u.deleted = 0
            LEFT JOIN mes_sfc_cut_round_report cr
              ON cr.production_batch_no = fs.slice_batch_no
             AND cr.deleted = 0
            LEFT JOIN mes_sfc_press_slot_report ps
              ON ps.id = cr.source_press_slot_report_id
             AND ps.deleted = 0
            WHERE fs.deleted = 0
              AND fs.stock_status IN ('INBOUNDED', 'AVAILABLE')
              AND fs.location_code IS NOT NULL
              AND fs.location_code != ''
              AND COALESCE(fs.qty, 0) > 0
              AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                OR fs.stock_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.inner_unit_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.outer_box_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.location_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.location_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.inbound_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR u.inner_unit_no LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.locationCode} IS NULL OR #{req.locationCode} = ''
                OR fs.location_code = #{req.locationCode})
              AND (#{req.locationKeyword} IS NULL OR #{req.locationKeyword} = ''
                OR fs.location_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                OR fs.location_name LIKE CONCAT('%', #{req.locationKeyword}, '%'))
              AND (#{req.packageNo} IS NULL OR #{req.packageNo} = ''
                OR fs.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR fs.outer_box_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR u.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%'))
              AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
              AND (#{req.sliceBatchNoPrefix} IS NULL OR #{req.sliceBatchNoPrefix} = ''
                OR fs.slice_batch_no LIKE CONCAT(#{req.sliceBatchNoPrefix}, '%'))
              AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                OR fs.batch_no LIKE CONCAT('%', #{req.batchNo}, '%'))
              AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                OR fs.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
              AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
              AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                OR (#{req.qualityStatus} = 'FROZEN' AND fs.coa_frozen = 1)
                OR (#{req.qualityStatus} != 'FROZEN' AND fs.quality_status = #{req.qualityStatus}))
              AND (#{req.productionDateStart} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) >= #{req.productionDateStart})
              AND (#{req.productionDateEnd} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) <= #{req.productionDateEnd})
              AND (#{req.inboundDateStart} IS NULL OR DATE(fs.inbound_time) >= #{req.inboundDateStart})
              AND (#{req.inboundDateEnd} IS NULL OR DATE(fs.inbound_time) <= #{req.inboundDateEnd})
              AND (#{req.stockStatus} IS NULL OR #{req.stockStatus} = ''
                OR (#{req.stockStatus} = 'INBOUNDED' AND fs.stock_status IN ('INBOUNDED', 'AVAILABLE'))
                OR (#{req.stockStatus} = 'OUTBOUND_LOCKED' AND fs.stock_status IN ('OUTBOUND_LOCKED', 'ALLOCATED'))
                OR fs.stock_status = #{req.stockStatus})
            """)
    Long countFgStockLedgerPage(@Param("req") FgStockLedgerPageReqVO reqVO);

    @Select("""
            SELECT
              fs.id AS id,
              fs.stock_no AS stockNo,
              COALESCE(NULLIF(i.source_type, ''), CASE
                WHEN i.source_manual_piece_id IS NOT NULL THEN 'MANUAL_HISTORY'
                ELSE NULL
              END) AS sourceType,
              i.source_manual_piece_id AS sourceManualPieceId,
              CASE
                WHEN i.source_manual_piece_id IS NOT NULL
                 AND COALESCE(NULLIF(i.source_type, ''), 'MANUAL_HISTORY') = 'MANUAL_HISTORY'
                 -- 历史库存导入的来源片在旧链路中会停留在 PACKED；库存/包装已上架时同样允许更正。
                 AND mp.record_status IN ('PACKED', 'INBOUNDED')
                 AND fs.stock_status IN ('INBOUNDED', 'AVAILABLE')
                 AND COALESCE(lock_sum.locked_qty, 0) = 0
                 AND (SELECT COUNT(1)
                      FROM mes_sfc_inner_pack_unit_item editable_item
                      WHERE editable_item.inner_unit_no = fs.inner_unit_no
                        AND editable_item.deleted = b'0') = 1
                THEN TRUE ELSE FALSE
              END AS importedDataEditable,
              CASE WHEN COALESCE(mp.print_count, 0) > 0 THEN TRUE ELSE FALSE END AS labelReprintRequired,
              COALESCE(fs.inner_unit_no, fs.outer_box_no) AS packageNo,
              fs.inner_unit_no AS innerUnitNo,
              fs.slice_batch_no AS sliceBatchNo,
              fs.material_code AS materialCode,
              fs.material_name AS materialName,
              fs.model_code AS modelCode,
              fs.batch_no AS batchNo,
              mp.inspection_result AS inspectionResult,
              mp.coa_inspection_result AS coaInspectionResult,
              mp.remark AS remark,
              COALESCE(fs.production_date, DATE(cr.confirmer_time)) AS productionDate,
              COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) AS expiryDate,
              fs.product_size AS productSize,
              fs.qty AS qty,
              COALESCE(lock_sum.locked_qty, 0) AS lockedQty,
              CASE WHEN fs.coa_frozen = 1 OR fs.quality_status != 'OK' THEN 0 ELSE GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) END AS availableQty,
              fs.quality_status AS qualityStatus,
              fs.coa_frozen AS coaFrozen, fs.coa_freeze_reason AS coaFreezeReason,
              CASE WHEN fs.coa_frozen = 1 THEN COALESCE(fs.qty, 1) ELSE 0 END AS frozenQty,
              fs.warehouse_code AS warehouseCode,
              fs.warehouse_name AS warehouseName,
              fs.location_code AS locationCode,
              fs.location_name AS locationName,
              fs.stock_status AS stockStatus,
              fs.inbound_no AS inboundNo,
              fs.inbound_time AS inboundTime,
              COALESCE(u.inbound_user_name, u.recorder_name) AS inboundUserName
            FROM mes_inv_finished_stock fs
            LEFT JOIN mes_sfc_inner_pack_unit u
              ON u.inner_unit_no = fs.inner_unit_no
             AND u.deleted = 0
            LEFT JOIN mes_sfc_inner_pack_unit_item i
              ON i.inner_unit_no = fs.inner_unit_no
             AND i.deleted = 0
             AND (i.slice_batch_no = fs.slice_batch_no OR i.production_batch_no = fs.slice_batch_no)
            LEFT JOIN mes_sfc_packaging_manual_piece mp
              ON mp.id = i.source_manual_piece_id
             AND mp.deleted = 0
            LEFT JOIN mes_sfc_cut_round_report cr
              ON cr.production_batch_no = fs.slice_batch_no
             AND cr.deleted = 0
            LEFT JOIN mes_sfc_press_slot_report ps
              ON ps.id = cr.source_press_slot_report_id
             AND ps.deleted = 0
            LEFT JOIN (
              SELECT finished_stock_id, SUM(COALESCE(locked_qty, 1)) AS locked_qty
              FROM mes_inv_fg_shipping_notice_item
              WHERE deleted = 0
                AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
              GROUP BY finished_stock_id
            ) lock_sum ON lock_sum.finished_stock_id = fs.id
            WHERE fs.deleted = 0
              AND fs.stock_status IN ('INBOUNDED', 'AVAILABLE')
              AND fs.location_code IS NOT NULL
              AND fs.location_code != ''
              AND COALESCE(fs.qty, 0) > 0
              AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                OR fs.stock_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.inner_unit_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.outer_box_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.location_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.location_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.inbound_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR u.inner_unit_no LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.locationCode} IS NULL OR #{req.locationCode} = ''
                OR fs.location_code = #{req.locationCode})
              AND (#{req.locationKeyword} IS NULL OR #{req.locationKeyword} = ''
                OR fs.location_code LIKE CONCAT('%', #{req.locationKeyword}, '%')
                OR fs.location_name LIKE CONCAT('%', #{req.locationKeyword}, '%'))
              AND (#{req.packageNo} IS NULL OR #{req.packageNo} = ''
                OR fs.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR fs.outer_box_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR u.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%'))
              AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
              AND (#{req.sliceBatchNoPrefix} IS NULL OR #{req.sliceBatchNoPrefix} = ''
                OR fs.slice_batch_no LIKE CONCAT(#{req.sliceBatchNoPrefix}, '%'))
              AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                OR fs.batch_no LIKE CONCAT('%', #{req.batchNo}, '%'))
              AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                OR fs.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
              AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
              AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                OR (#{req.qualityStatus} = 'FROZEN' AND fs.coa_frozen = 1)
                OR (#{req.qualityStatus} != 'FROZEN' AND fs.quality_status = #{req.qualityStatus}))
              AND (#{req.productionDateStart} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) >= #{req.productionDateStart})
              AND (#{req.productionDateEnd} IS NULL
                OR COALESCE(fs.production_date, DATE(cr.confirmer_time)) <= #{req.productionDateEnd})
              AND (#{req.inboundDateStart} IS NULL OR DATE(fs.inbound_time) >= #{req.inboundDateStart})
              AND (#{req.inboundDateEnd} IS NULL OR DATE(fs.inbound_time) <= #{req.inboundDateEnd})
              AND (#{req.stockStatus} IS NULL OR #{req.stockStatus} = ''
                OR (#{req.stockStatus} = 'INBOUNDED' AND fs.stock_status IN ('INBOUNDED', 'AVAILABLE'))
                OR (#{req.stockStatus} = 'OUTBOUND_LOCKED' AND fs.stock_status IN ('OUTBOUND_LOCKED', 'ALLOCATED'))
                OR fs.stock_status = #{req.stockStatus})
            ORDER BY
              CASE
                WHEN COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) >= '2000-01-01'
                 AND COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) < #{businessDate}
                THEN 0
                WHEN fs.coa_frozen = 1 THEN 1
                ELSE 2
              END ASC,
              CASE
                WHEN COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) >= '2000-01-01'
                 AND COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) < #{businessDate}
                THEN COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY))
              END ASC,
              fs.inbound_time DESC,
              fs.id DESC
            LIMIT #{offset}, #{limit}
            """)
    List<FgStockLedgerRespVO> selectFgStockLedgerPage(@Param("req") FgStockLedgerPageReqVO reqVO,
                                                      @Param("businessDate") LocalDate businessDate,
                                                      @Param("offset") Integer offset,
                                                      @Param("limit") Integer limit);

    @Select("""
            SELECT COUNT(1)
            FROM mes_inv_finished_stock fs
            LEFT JOIN mes_sfc_inner_pack_unit u
              ON u.inner_unit_no = fs.inner_unit_no
             AND u.deleted = 0
            LEFT JOIN (
              SELECT finished_stock_id, SUM(COALESCE(locked_qty, 1)) AS locked_qty
              FROM mes_inv_fg_shipping_notice_item
              WHERE deleted = 0
                AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
              GROUP BY finished_stock_id
            ) lock_sum ON lock_sum.finished_stock_id = fs.id
            WHERE fs.deleted = 0
              AND fs.stock_status IN ('AVAILABLE', 'OUTBOUND_LOCKED')
              AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
              AND (#{req.locationCode} IS NULL OR #{req.locationCode} = ''
                OR fs.location_code = #{req.locationCode})
              AND (#{req.packageNo} IS NULL OR #{req.packageNo} = ''
                OR fs.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR fs.outer_box_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR u.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%'))
              AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
              AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                OR fs.batch_no LIKE CONCAT('%', #{req.batchNo}, '%'))
              AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                OR fs.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
              AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
              AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                OR (#{req.qualityStatus} = 'FROZEN' AND fs.coa_frozen = 1)
                OR (#{req.qualityStatus} != 'FROZEN' AND fs.quality_status = #{req.qualityStatus}))
              AND (#{req.inboundDateStart} IS NULL OR DATE(fs.inbound_time) >= #{req.inboundDateStart})
              AND (#{req.inboundDateEnd} IS NULL OR DATE(fs.inbound_time) <= #{req.inboundDateEnd})
            """)
    Long countShippingNoticeStockCandidatePage(@Param("req") FgStockLedgerPageReqVO reqVO);

    @Select("""
            SELECT
              fs.id AS id,
              fs.stock_no AS stockNo,
              COALESCE(fs.inner_unit_no, fs.outer_box_no) AS packageNo,
              fs.inner_unit_no AS innerUnitNo,
              fs.slice_batch_no AS sliceBatchNo,
              fs.material_code AS materialCode,
              fs.material_name AS materialName,
              fs.model_code AS modelCode,
              fs.batch_no AS batchNo,
              fs.product_size AS productSize,
              fs.qty AS qty,
              COALESCE(lock_sum.locked_qty, 0) AS lockedQty,
              CASE WHEN fs.coa_frozen = 1 OR fs.quality_status != 'OK' THEN 0 ELSE GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) END AS availableQty,
              fs.quality_status AS qualityStatus,
              fs.warehouse_code AS warehouseCode,
              fs.warehouse_name AS warehouseName,
              fs.location_code AS locationCode,
              fs.location_name AS locationName,
              fs.stock_status AS stockStatus,
              fs.inbound_no AS inboundNo,
              fs.inbound_time AS inboundTime,
              COALESCE(u.inbound_user_name, u.recorder_name) AS inboundUserName
            FROM mes_inv_finished_stock fs
            LEFT JOIN mes_sfc_inner_pack_unit u
              ON u.inner_unit_no = fs.inner_unit_no
             AND u.deleted = 0
            LEFT JOIN (
              SELECT finished_stock_id, SUM(COALESCE(locked_qty, 1)) AS locked_qty
              FROM mes_inv_fg_shipping_notice_item
              WHERE deleted = 0
                AND lock_status IN ('LOCKED', 'OUTBOUND')
              GROUP BY finished_stock_id
            ) lock_sum ON lock_sum.finished_stock_id = fs.id
            WHERE fs.deleted = 0
              AND fs.stock_status IN ('AVAILABLE', 'OUTBOUND_LOCKED')
              AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
              AND (#{req.locationCode} IS NULL OR #{req.locationCode} = ''
                OR fs.location_code = #{req.locationCode})
              AND (#{req.packageNo} IS NULL OR #{req.packageNo} = ''
                OR fs.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR fs.outer_box_no LIKE CONCAT('%', #{req.packageNo}, '%')
                OR u.inner_unit_no LIKE CONCAT('%', #{req.packageNo}, '%'))
              AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
              AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                OR fs.batch_no LIKE CONCAT('%', #{req.batchNo}, '%'))
              AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                OR fs.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
              AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
              AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                OR (#{req.qualityStatus} = 'FROZEN' AND fs.coa_frozen = 1)
                OR (#{req.qualityStatus} != 'FROZEN' AND fs.quality_status = #{req.qualityStatus}))
              AND (#{req.inboundDateStart} IS NULL OR DATE(fs.inbound_time) >= #{req.inboundDateStart})
              AND (#{req.inboundDateEnd} IS NULL OR DATE(fs.inbound_time) <= #{req.inboundDateEnd})
            ORDER BY fs.inbound_time ASC, fs.id ASC
            LIMIT #{offset}, #{limit}
            """)
    List<FgStockLedgerRespVO> selectShippingNoticeStockCandidatePage(@Param("req") FgStockLedgerPageReqVO reqVO,
                                                                     @Param("offset") Integer offset,
                                                                     @Param("limit") Integer limit);

    @Select("""
            SELECT COUNT(1)
            FROM (
              SELECT CONCAT('WAREHOUSE_STOCK:', fs.id) AS candidate_key
              FROM mes_inv_finished_stock fs
              LEFT JOIN (
                SELECT finished_stock_id, SUM(locked_qty) AS locked_qty
                FROM (
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_notice_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  UNION ALL
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_pick_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                ) locked
                GROUP BY finished_stock_id
              ) lock_sum ON lock_sum.finished_stock_id = fs.id
              WHERE fs.deleted = 0
                AND fs.stock_status = 'AVAILABLE' AND fs.coa_frozen = 0
                AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
                AND EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_notice_item ni
                  WHERE ni.notice_id = #{req.noticeId}
                    AND ni.deleted = 0
                    AND (ni.internal_model_code IS NULL OR ni.internal_model_code = ''
                      OR fs.model_code = ni.internal_model_code)
                    AND (ni.internal_item_code IS NULL OR ni.internal_item_code = ''
                      OR fs.slice_batch_no LIKE CONCAT(ni.internal_item_code, '%'))
                )
                AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                  OR fs.slice_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                  OR (#{req.qualityStatus} = 'FROZEN' AND fs.coa_frozen = 1)
                OR (#{req.qualityStatus} != 'FROZEN' AND fs.quality_status = #{req.qualityStatus}))
              UNION ALL
              SELECT CONCAT('PACKAGING_DIRECT:INNER_PACK:', pi.id) AS candidate_key
              FROM mes_sfc_inner_pack_unit_item pi
              INNER JOIN mes_sfc_inner_pack_unit u
                ON u.id = pi.inner_unit_id
               AND u.deleted = 0
              WHERE COALESCE(#{req.includePackagingDirect}, FALSE) = TRUE
                AND pi.deleted = 0
                AND COALESCE(pi.source_type, 'CUT_ROUND_REPORT') = 'CUT_ROUND_REPORT'
                AND u.unit_status IN ('PACKED', 'INBOUND_LOCKED')
                AND (u.location_code IS NULL OR u.location_code = '')
                AND pi.quality_status = 'OK'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock occupied_stock
                  WHERE occupied_stock.deleted = 0
                    AND occupied_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                    AND (COALESCE(occupied_stock.inner_unit_no, '') <> COALESCE(u.inner_unit_no, '')
                      OR occupied_stock.stock_status <> 'INBOUND_LOCKED'
                      OR COALESCE(occupied_stock.location_code, '') <> ''
                      OR COALESCE(occupied_stock.quality_status, '') <> 'OK')
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock package_stock
                  WHERE package_stock.deleted = 0
                    AND package_stock.inner_unit_no = u.inner_unit_no
                    AND (package_stock.stock_status <> 'INBOUND_LOCKED'
                      OR COALESCE(package_stock.location_code, '') <> '')
                )
                AND (
                  SELECT COUNT(1)
                  FROM mes_inv_finished_stock reusable_stock
                  WHERE reusable_stock.deleted = 0
                    AND reusable_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                ) <= 1
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock locked_stock
                  INNER JOIN mes_inv_fg_shipping_notice_item locked_notice_item
                    ON locked_notice_item.finished_stock_id = locked_stock.id
                   AND locked_notice_item.deleted = 0
                   AND locked_notice_item.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  WHERE locked_stock.deleted = 0
                    AND locked_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock locked_stock
                  INNER JOIN mes_inv_fg_shipping_pick_item locked_pick_item
                    ON locked_pick_item.finished_stock_id = locked_stock.id
                   AND locked_pick_item.deleted = 0
                   AND locked_pick_item.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  WHERE locked_stock.deleted = 0
                    AND locked_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item active_pick
                  WHERE active_pick.source_inner_pack_item_id = pi.id
                    AND active_pick.deleted = 0
                    AND active_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item sibling_pick
                  INNER JOIN mes_sfc_inner_pack_unit_item sibling_item
                    ON sibling_item.id = sibling_pick.source_inner_pack_item_id
                   AND sibling_item.inner_unit_id = u.id
                   AND sibling_item.deleted = 0
                  WHERE sibling_pick.deleted = 0
                    AND sibling_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
                AND EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_notice_item ni
                  WHERE ni.notice_id = #{req.noticeId}
                    AND ni.deleted = 0
                    AND (ni.internal_model_code IS NULL OR ni.internal_model_code = ''
                      OR u.model_code = ni.internal_model_code)
                    AND (ni.internal_item_code IS NULL OR ni.internal_item_code = ''
                      OR COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                        LIKE CONCAT(ni.internal_item_code, '%'))
                )
                AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                  OR COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                    LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR u.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                  OR pi.quality_status = #{req.qualityStatus})
              UNION ALL
              SELECT CONCAT('PACKAGING_DIRECT:CUT_ROUND:', cr.id) AS candidate_key
              FROM mes_sfc_cut_round_report cr
              WHERE COALESCE(#{req.includePackagingDirect}, FALSE) = TRUE
                AND cr.deleted = 0
                AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
                AND cr.inspection_status = 'COMPLETED'
                AND UPPER(COALESCE(cr.inspection_result, '')) <> 'NG'
                AND UPPER(COALESCE(cr.self_check, '')) <> 'NG'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_sfc_inner_pack_unit_item packed_item
                  WHERE packed_item.deleted = 0
                    AND (packed_item.source_cut_round_report_id = cr.id
                      OR COALESCE(NULLIF(packed_item.slice_batch_no, ''), packed_item.production_batch_no) = cr.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock existing_stock
                  WHERE existing_stock.deleted = 0
                    AND existing_stock.slice_batch_no = cr.production_batch_no
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item active_pick
                  WHERE active_pick.source_cut_round_report_id = cr.id
                    AND active_pick.deleted = 0
                    AND active_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
                AND EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_notice_item ni
                  WHERE ni.notice_id = #{req.noticeId}
                    AND ni.deleted = 0
                    AND (ni.internal_model_code IS NULL OR ni.internal_model_code = ''
                      OR cr.model_code = ni.internal_model_code)
                    AND (ni.internal_item_code IS NULL OR ni.internal_item_code = ''
                      OR cr.production_batch_no LIKE CONCAT(ni.internal_item_code, '%'))
                )
                AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                  OR cr.production_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR cr.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                  OR UPPER(#{req.qualityStatus}) = 'OK')
            ) candidates
            """)
    Long countShippingNoticePickCandidatePage(@Param("req") ShippingNoticePickCandidatePageReqVO reqVO);

    @Select("""
            SELECT
              candidateType,
              candidateKey,
              sourceInnerPackItemId,
              sourceCutRoundReportId,
              id,
              stockNo,
              packageNo,
              innerUnitNo,
              sliceBatchNo,
              materialCode,
              materialName,
              modelCode,
              batchNo,
              inspectionTaskNo,
              productionDate,
              expiryDate,
              inspectionStatus,
              inspectionResult,
              productSize,
              qty,
              lockedQty,
              availableQty,
              qualityStatus,
              warehouseCode,
              warehouseName,
              locationCode,
              locationName,
              stockStatus,
              inboundNo,
              inboundTime,
              inboundUserName,
              outboundLocation,
              outboundTime,
              outboundRecorderName,
              outboundQualityNo
            FROM (
              SELECT
                'WAREHOUSE_STOCK' AS candidateType,
                CONCAT('WAREHOUSE_STOCK:', fs.id) AS candidateKey,
                NULL AS sourceInnerPackItemId,
                NULL AS sourceCutRoundReportId,
                fs.id AS id,
                fs.stock_no AS stockNo,
                COALESCE(fs.inner_unit_no, fs.outer_box_no) AS packageNo,
                fs.inner_unit_no AS innerUnitNo,
                fs.slice_batch_no AS sliceBatchNo,
                fs.material_code AS materialCode,
                fs.material_name AS materialName,
                fs.model_code AS modelCode,
                fs.batch_no AS batchNo,
                cr.inspection_task_no AS inspectionTaskNo,
                COALESCE(fs.production_date, DATE(cr.confirmer_time)) AS productionDate,
                COALESCE(fs.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) AS expiryDate,
                cr.inspection_status AS inspectionStatus,
                cr.inspection_result AS inspectionResult,
                fs.product_size AS productSize,
                fs.qty AS qty,
                COALESCE(lock_sum.locked_qty, 0) AS lockedQty,
                CASE WHEN fs.coa_frozen = 1 OR fs.quality_status != 'OK' THEN 0 ELSE GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) END AS availableQty,
                fs.quality_status AS qualityStatus,
                fs.warehouse_code AS warehouseCode,
                fs.warehouse_name AS warehouseName,
                fs.location_code AS locationCode,
                fs.location_name AS locationName,
                fs.stock_status AS stockStatus,
                fs.inbound_no AS inboundNo,
                fs.inbound_time AS inboundTime,
                COALESCE(u.inbound_user_name, u.recorder_name) AS inboundUserName,
                NULL AS outboundLocation,
                NULL AS outboundTime,
                NULL AS outboundRecorderName,
                NULL AS outboundQualityNo,
                fs.inbound_time AS sortTime,
                fs.id AS sortId
              FROM mes_inv_finished_stock fs
              LEFT JOIN mes_sfc_inner_pack_unit u
                ON u.inner_unit_no = fs.inner_unit_no
               AND u.deleted = 0
              LEFT JOIN mes_sfc_cut_round_report cr
                ON cr.id = (
                  SELECT MAX(latest_cr.id)
                  FROM mes_sfc_cut_round_report latest_cr
                  WHERE latest_cr.production_batch_no = fs.slice_batch_no
                    AND latest_cr.deleted = 0
                )
              LEFT JOIN (
                SELECT finished_stock_id, SUM(locked_qty) AS locked_qty
                FROM (
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_notice_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  UNION ALL
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_pick_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                ) locked
                GROUP BY finished_stock_id
              ) lock_sum ON lock_sum.finished_stock_id = fs.id
              WHERE fs.deleted = 0
                AND fs.stock_status = 'AVAILABLE' AND fs.coa_frozen = 0
                AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
                AND EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_notice_item ni
                  WHERE ni.notice_id = #{req.noticeId}
                    AND ni.deleted = 0
                    AND (ni.internal_model_code IS NULL OR ni.internal_model_code = ''
                      OR fs.model_code = ni.internal_model_code)
                    AND (ni.internal_item_code IS NULL OR ni.internal_item_code = ''
                      OR fs.slice_batch_no LIKE CONCAT(ni.internal_item_code, '%'))
                )
                AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                  OR fs.slice_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                  OR (#{req.qualityStatus} = 'FROZEN' AND fs.coa_frozen = 1)
                OR (#{req.qualityStatus} != 'FROZEN' AND fs.quality_status = #{req.qualityStatus}))
              UNION ALL
              SELECT
                'PACKAGING_DIRECT' AS candidateType,
                CONCAT('PACKAGING_DIRECT:INNER_PACK:', pi.id) AS candidateKey,
                pi.id AS sourceInnerPackItemId,
                NULL AS sourceCutRoundReportId,
                NULL AS id,
                reusable_stock.stock_no AS stockNo,
                u.inner_unit_no AS packageNo,
                u.inner_unit_no AS innerUnitNo,
                COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no) AS sliceBatchNo,
                u.material_code AS materialCode,
                u.material_name AS materialName,
                u.model_code AS modelCode,
                u.batch_no AS batchNo,
                cr.inspection_task_no AS inspectionTaskNo,
                COALESCE(reusable_stock.production_date, DATE(cr.confirmer_time)) AS productionDate,
                COALESCE(reusable_stock.expiry_date, DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY)) AS expiryDate,
                cr.inspection_status AS inspectionStatus,
                cr.inspection_result AS inspectionResult,
                u.product_size AS productSize,
                1 AS qty,
                0 AS lockedQty,
                1 AS availableQty,
                pi.quality_status AS qualityStatus,
                NULL AS warehouseCode,
                NULL AS warehouseName,
                NULL AS locationCode,
                NULL AS locationName,
                u.unit_status AS stockStatus,
                reusable_stock.inbound_no AS inboundNo,
                reusable_stock.inbound_time AS inboundTime,
                NULL AS inboundUserName,
                NULL AS outboundLocation,
                NULL AS outboundTime,
                NULL AS outboundRecorderName,
                NULL AS outboundQualityNo,
                COALESCE(pi.scan_time, u.lock_time, u.create_time) AS sortTime,
                pi.id AS sortId
              FROM mes_sfc_inner_pack_unit_item pi
              INNER JOIN mes_sfc_inner_pack_unit u
                ON u.id = pi.inner_unit_id
               AND u.deleted = 0
              LEFT JOIN mes_sfc_cut_round_report cr
                ON cr.id = pi.source_cut_round_report_id
               AND cr.deleted = 0
              LEFT JOIN mes_inv_finished_stock reusable_stock
                ON reusable_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
               AND reusable_stock.inner_unit_no = u.inner_unit_no
               AND reusable_stock.stock_status = 'INBOUND_LOCKED'
               AND COALESCE(reusable_stock.location_code, '') = ''
               AND reusable_stock.deleted = 0
              WHERE COALESCE(#{req.includePackagingDirect}, FALSE) = TRUE
                AND pi.deleted = 0
                AND COALESCE(pi.source_type, 'CUT_ROUND_REPORT') = 'CUT_ROUND_REPORT'
                AND u.unit_status IN ('PACKED', 'INBOUND_LOCKED')
                AND (u.location_code IS NULL OR u.location_code = '')
                AND pi.quality_status = 'OK'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock occupied_stock
                  WHERE occupied_stock.deleted = 0
                    AND occupied_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                    AND (COALESCE(occupied_stock.inner_unit_no, '') <> COALESCE(u.inner_unit_no, '')
                      OR occupied_stock.stock_status <> 'INBOUND_LOCKED'
                      OR COALESCE(occupied_stock.location_code, '') <> ''
                      OR COALESCE(occupied_stock.quality_status, '') <> 'OK')
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock package_stock
                  WHERE package_stock.deleted = 0
                    AND package_stock.inner_unit_no = u.inner_unit_no
                    AND (package_stock.stock_status <> 'INBOUND_LOCKED'
                      OR COALESCE(package_stock.location_code, '') <> '')
                )
                AND (
                  SELECT COUNT(1)
                  FROM mes_inv_finished_stock reusable_stock_count
                  WHERE reusable_stock_count.deleted = 0
                    AND reusable_stock_count.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                ) <= 1
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock locked_stock
                  INNER JOIN mes_inv_fg_shipping_notice_item locked_notice_item
                    ON locked_notice_item.finished_stock_id = locked_stock.id
                   AND locked_notice_item.deleted = 0
                   AND locked_notice_item.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  WHERE locked_stock.deleted = 0
                    AND locked_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock locked_stock
                  INNER JOIN mes_inv_fg_shipping_pick_item locked_pick_item
                    ON locked_pick_item.finished_stock_id = locked_stock.id
                   AND locked_pick_item.deleted = 0
                   AND locked_pick_item.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  WHERE locked_stock.deleted = 0
                    AND locked_stock.slice_batch_no = COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item active_pick
                  WHERE active_pick.source_inner_pack_item_id = pi.id
                    AND active_pick.deleted = 0
                    AND active_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item sibling_pick
                  INNER JOIN mes_sfc_inner_pack_unit_item sibling_item
                    ON sibling_item.id = sibling_pick.source_inner_pack_item_id
                   AND sibling_item.inner_unit_id = u.id
                   AND sibling_item.deleted = 0
                  WHERE sibling_pick.deleted = 0
                    AND sibling_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
                AND EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_notice_item ni
                  WHERE ni.notice_id = #{req.noticeId}
                    AND ni.deleted = 0
                    AND (ni.internal_model_code IS NULL OR ni.internal_model_code = ''
                      OR u.model_code = ni.internal_model_code)
                    AND (ni.internal_item_code IS NULL OR ni.internal_item_code = ''
                      OR COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                        LIKE CONCAT(ni.internal_item_code, '%'))
                )
                AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                  OR COALESCE(NULLIF(pi.slice_batch_no, ''), pi.production_batch_no)
                    LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR u.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                  OR pi.quality_status = #{req.qualityStatus})
              UNION ALL
              SELECT
                'PACKAGING_DIRECT' AS candidateType,
                CONCAT('PACKAGING_DIRECT:CUT_ROUND:', cr.id) AS candidateKey,
                NULL AS sourceInnerPackItemId,
                cr.id AS sourceCutRoundReportId,
                NULL AS id,
                NULL AS stockNo,
                NULL AS packageNo,
                NULL AS innerUnitNo,
                cr.production_batch_no AS sliceBatchNo,
                cr.material_code AS materialCode,
                cr.material_name AS materialName,
                cr.model_code AS modelCode,
                COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no) AS batchNo,
                cr.inspection_task_no AS inspectionTaskNo,
                DATE(cr.confirmer_time) AS productionDate,
                DATE_SUB(DATE_ADD(DATE(cr.confirmer_time), INTERVAL 10 MONTH), INTERVAL 1 DAY) AS expiryDate,
                cr.inspection_status AS inspectionStatus,
                cr.inspection_result AS inspectionResult,
                COALESCE(po.size_name, po.size_spec) AS productSize,
                1 AS qty,
                0 AS lockedQty,
                1 AS availableQty,
                'OK' AS qualityStatus,
                NULL AS warehouseCode,
                NULL AS warehouseName,
                NULL AS locationCode,
                NULL AS locationName,
                'PACKAGING_READY' AS stockStatus,
                NULL AS inboundNo,
                NULL AS inboundTime,
                NULL AS inboundUserName,
                NULL AS outboundLocation,
                NULL AS outboundTime,
                NULL AS outboundRecorderName,
                NULL AS outboundQualityNo,
                COALESCE(cr.confirmer_time, cr.inspection_time, cr.create_time) AS sortTime,
                cr.id AS sortId
              FROM mes_sfc_cut_round_report cr
              LEFT JOIN mes_pp_plan_order po
                ON po.id = cr.plan_id
               AND po.deleted = 0
              WHERE COALESCE(#{req.includePackagingDirect}, FALSE) = TRUE
                AND cr.deleted = 0
                AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
                AND cr.inspection_status = 'COMPLETED'
                AND UPPER(COALESCE(cr.inspection_result, '')) <> 'NG'
                AND UPPER(COALESCE(cr.self_check, '')) <> 'NG'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_sfc_inner_pack_unit_item packed_item
                  WHERE packed_item.deleted = 0
                    AND (packed_item.source_cut_round_report_id = cr.id
                      OR COALESCE(NULLIF(packed_item.slice_batch_no, ''), packed_item.production_batch_no) = cr.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock existing_stock
                  WHERE existing_stock.deleted = 0
                    AND existing_stock.slice_batch_no = cr.production_batch_no
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item active_pick
                  WHERE active_pick.source_cut_round_report_id = cr.id
                    AND active_pick.deleted = 0
                    AND active_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
                AND EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_notice_item ni
                  WHERE ni.notice_id = #{req.noticeId}
                    AND ni.deleted = 0
                    AND (ni.internal_model_code IS NULL OR ni.internal_model_code = ''
                      OR cr.model_code = ni.internal_model_code)
                    AND (ni.internal_item_code IS NULL OR ni.internal_item_code = ''
                      OR cr.production_batch_no LIKE CONCAT(ni.internal_item_code, '%'))
                )
                AND (#{req.sliceBatchNo} IS NULL OR #{req.sliceBatchNo} = ''
                  OR cr.production_batch_no LIKE CONCAT('%', #{req.sliceBatchNo}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR cr.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.qualityStatus} IS NULL OR #{req.qualityStatus} = ''
                  OR UPPER(#{req.qualityStatus}) = 'OK')
            ) candidates
            ORDER BY sortTime ASC, candidateType ASC, sortId ASC
            LIMIT #{offset}, #{limit}
            """)
    List<FgStockLedgerRespVO> selectShippingNoticePickCandidatePage(@Param("req") ShippingNoticePickCandidatePageReqVO reqVO,
                                                                    @Param("offset") Integer offset,
                                                                    @Param("limit") Integer limit);

    @Select("""
            SELECT COUNT(1)
            FROM (
              SELECT
                fs.model_code,
                COALESCE(NULLIF(fs.batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(fs.slice_batch_no, ''),
                  NULLIF(cr.production_batch_no, '')) AS batch_no,
                fs.material_code
              FROM mes_inv_finished_stock fs
              LEFT JOIN mes_sfc_cut_round_report cr
                ON cr.production_batch_no = fs.slice_batch_no
               AND cr.deleted = 0
              LEFT JOIN (
                SELECT finished_stock_id, SUM(locked_qty) AS locked_qty
                FROM (
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_notice_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  UNION ALL
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_pick_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                ) locked
                GROUP BY finished_stock_id
              ) lock_sum ON lock_sum.finished_stock_id = fs.id
              WHERE fs.deleted = 0
                AND fs.stock_status = 'AVAILABLE' AND fs.coa_frozen = 0
                AND fs.quality_status = 'OK' AND fs.coa_frozen = 0
                AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
                AND COALESCE(NULLIF(fs.batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(fs.slice_batch_no, ''),
                  NULLIF(cr.production_batch_no, '')) IS NOT NULL
                AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                  OR fs.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR fs.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR fs.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR fs.slice_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR COALESCE(NULLIF(fs.batch_no, ''),
                    NULLIF(cr.parent_production_batch_no, ''),
                    NULLIF(cr.source_production_batch_no, ''),
                    NULLIF(fs.slice_batch_no, ''),
                    NULLIF(cr.production_batch_no, '')) LIKE CONCAT('%', #{req.keyword}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                  OR fs.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
                AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                  OR fs.slice_batch_no LIKE CONCAT('%', #{req.batchNo}, '%')
                  OR COALESCE(NULLIF(fs.batch_no, ''),
                    NULLIF(cr.parent_production_batch_no, ''),
                    NULLIF(cr.source_production_batch_no, ''),
                    NULLIF(fs.slice_batch_no, ''),
                    NULLIF(cr.production_batch_no, '')) LIKE CONCAT('%', #{req.batchNo}, '%'))
              GROUP BY fs.model_code,
                COALESCE(NULLIF(fs.batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(fs.slice_batch_no, ''),
                  NULLIF(cr.production_batch_no, '')),
                fs.material_code
            ) t
            """)
    Long countShippingNoticeBatchCandidatePage(@Param("req") FgShippingBatchCandidatePageReqVO reqVO);

    @Select("""
            SELECT
              fs.model_code AS modelCode,
              COALESCE(NULLIF(fs.batch_no, ''),
                NULLIF(cr.parent_production_batch_no, ''),
                NULLIF(cr.source_production_batch_no, ''),
                NULLIF(fs.slice_batch_no, ''),
                NULLIF(cr.production_batch_no, '')) AS batchNo,
              fs.material_code AS materialCode,
              MAX(fs.material_name) AS materialName,
              SUM(GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0)) AS availableQty,
              SUM(COALESCE(lock_sum.locked_qty, 0)) AS lockedQty,
              SUM(COALESCE(fs.qty, 1)) AS totalQty
            FROM mes_inv_finished_stock fs
            LEFT JOIN mes_sfc_cut_round_report cr
              ON cr.production_batch_no = fs.slice_batch_no
             AND cr.deleted = 0
            LEFT JOIN (
              SELECT finished_stock_id, SUM(locked_qty) AS locked_qty
              FROM (
                SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                FROM mes_inv_fg_shipping_notice_item
                WHERE deleted = 0
                  AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                UNION ALL
                SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                FROM mes_inv_fg_shipping_pick_item
                WHERE deleted = 0
                  AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
              ) locked
              GROUP BY finished_stock_id
            ) lock_sum ON lock_sum.finished_stock_id = fs.id
            WHERE fs.deleted = 0
              AND fs.stock_status = 'AVAILABLE' AND fs.coa_frozen = 0
              AND fs.quality_status = 'OK' AND fs.coa_frozen = 0
              AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
              AND COALESCE(NULLIF(fs.batch_no, ''),
                NULLIF(cr.parent_production_batch_no, ''),
                NULLIF(cr.source_production_batch_no, ''),
                NULLIF(fs.slice_batch_no, ''),
                NULLIF(cr.production_batch_no, '')) IS NOT NULL
              AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                OR fs.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR COALESCE(NULLIF(fs.batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(fs.slice_batch_no, ''),
                  NULLIF(cr.production_batch_no, '')) LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                OR fs.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
              AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                OR fs.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
              AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                OR fs.slice_batch_no LIKE CONCAT('%', #{req.batchNo}, '%')
                OR COALESCE(NULLIF(fs.batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(fs.slice_batch_no, ''),
                  NULLIF(cr.production_batch_no, '')) LIKE CONCAT('%', #{req.batchNo}, '%'))
            GROUP BY fs.model_code,
              COALESCE(NULLIF(fs.batch_no, ''),
                NULLIF(cr.parent_production_batch_no, ''),
                NULLIF(cr.source_production_batch_no, ''),
                NULLIF(fs.slice_batch_no, ''),
                NULLIF(cr.production_batch_no, '')),
              fs.material_code
            HAVING availableQty > 0
            ORDER BY COALESCE(MAX(fs.inbound_time), MAX(cr.inspection_time)) DESC, batchNo DESC
            LIMIT #{offset}, #{limit}
            """)
    List<FgShippingBatchCandidateRespVO> selectShippingNoticeBatchCandidatePage(@Param("req") FgShippingBatchCandidatePageReqVO reqVO,
                                                                                @Param("offset") Integer offset,
                                                                                @Param("limit") Integer limit);

    @Select("""
            WITH candidates AS (
              SELECT
                fs.model_code,
                COALESCE(NULLIF(fs.batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(fs.slice_batch_no, ''),
                  NULLIF(cr.production_batch_no, '')) AS batch_no,
                fs.material_code,
                fs.material_name,
                fs.slice_batch_no,
                GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) AS available_qty,
                COALESCE(lock_sum.locked_qty, 0) AS locked_qty,
                COALESCE(fs.qty, 1) AS total_qty,
                GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) AS stock_available_qty,
                0 AS packaging_ready_qty,
                COALESCE(fs.inbound_time, cr.inspection_time, fs.create_time) AS sort_time
              FROM mes_inv_finished_stock fs
              LEFT JOIN mes_sfc_cut_round_report cr
                ON cr.production_batch_no = fs.slice_batch_no
               AND cr.deleted = 0
              LEFT JOIN (
                SELECT finished_stock_id, SUM(locked_qty) AS locked_qty
                FROM (
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_notice_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  UNION ALL
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_pick_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                ) locked
                GROUP BY finished_stock_id
              ) lock_sum ON lock_sum.finished_stock_id = fs.id
              WHERE fs.deleted = 0
                AND fs.stock_status = 'AVAILABLE' AND fs.coa_frozen = 0
                AND fs.quality_status = 'OK' AND fs.coa_frozen = 0
                AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
              UNION ALL
              SELECT
                cr.model_code,
                COALESCE(NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no) AS batch_no,
                cr.material_code,
                cr.material_name,
                cr.production_batch_no AS slice_batch_no,
                1 AS available_qty,
                0 AS locked_qty,
                1 AS total_qty,
                0 AS stock_available_qty,
                1 AS packaging_ready_qty,
                COALESCE(cr.confirmer_time, cr.inspection_time, cr.create_time) AS sort_time
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0
                AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
                AND cr.inspection_status = 'COMPLETED'
                AND UPPER(COALESCE(cr.inspection_result, '')) <> 'NG'
                AND UPPER(COALESCE(cr.self_check, '')) <> 'NG'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'ADHESIVE2_REPORT'
                    AND fai.source_report_no LIKE '%-COA-%'
                    AND UPPER(TRIM(fai.product_batch_no)) LIKE CONCAT(
                      CASE
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '-J[0-9]+$'
                          THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))), '-J', 1)
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '-S[0-9]+$'
                          THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))), '-S', 1)
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$'
                          THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))),
                                    CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no)))) - 4)
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9]$'
                          THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))),
                                    CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no)))) - 3)
                        ELSE UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no)))
                      END,
                      '%')
                    AND (UPPER(COALESCE(fai.judgment, '')) = 'NG'
                      OR UPPER(COALESCE(fai.status, '')) = 'REJECTED')
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_sfc_inner_pack_unit_item packed_item
                  WHERE packed_item.deleted = 0
                    AND (packed_item.source_cut_round_report_id = cr.id
                      OR COALESCE(NULLIF(packed_item.slice_batch_no, ''), packed_item.production_batch_no) = cr.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock existing_stock
                  WHERE existing_stock.deleted = 0
                    AND existing_stock.slice_batch_no = cr.production_batch_no
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item active_pick
                  WHERE active_pick.source_cut_round_report_id = cr.id
                    AND active_pick.deleted = 0
                    AND active_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
            )
            SELECT COUNT(1)
            FROM (
              SELECT c.model_code, c.batch_no, c.material_code
              FROM candidates c
              WHERE c.batch_no IS NOT NULL
                AND c.batch_no <> ''
                AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                  OR c.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR c.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                  OR c.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                  OR c.slice_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                  OR c.batch_no LIKE CONCAT('%', #{req.keyword}, '%'))
                AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                  OR c.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
                AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                  OR c.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
                AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                  OR c.slice_batch_no LIKE CONCAT('%', #{req.batchNo}, '%')
                  OR c.batch_no LIKE CONCAT('%', #{req.batchNo}, '%'))
              GROUP BY c.model_code, c.batch_no, c.material_code
            ) grouped_candidates
            """)
    Long countShippingNoticeInternalItemCandidatePage(@Param("req") FgShippingBatchCandidatePageReqVO reqVO);

    @Select("""
            WITH candidates AS (
              SELECT
                fs.model_code,
                COALESCE(NULLIF(fs.batch_no, ''),
                  NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''),
                  NULLIF(fs.slice_batch_no, ''),
                  NULLIF(cr.production_batch_no, '')) AS batch_no,
                fs.material_code,
                fs.material_name,
                fs.slice_batch_no,
                GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) AS available_qty,
                COALESCE(lock_sum.locked_qty, 0) AS locked_qty,
                COALESCE(fs.qty, 1) AS total_qty,
                GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) AS stock_available_qty,
                0 AS packaging_ready_qty,
                COALESCE(fs.inbound_time, cr.inspection_time, fs.create_time) AS sort_time
              FROM mes_inv_finished_stock fs
              LEFT JOIN mes_sfc_cut_round_report cr
                ON cr.production_batch_no = fs.slice_batch_no
               AND cr.deleted = 0
              LEFT JOIN (
                SELECT finished_stock_id, SUM(locked_qty) AS locked_qty
                FROM (
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_notice_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                  UNION ALL
                  SELECT finished_stock_id, COALESCE(locked_qty, 1) AS locked_qty
                  FROM mes_inv_fg_shipping_pick_item
                  WHERE deleted = 0
                    AND lock_status IN ('PICKED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                ) locked
                GROUP BY finished_stock_id
              ) lock_sum ON lock_sum.finished_stock_id = fs.id
              WHERE fs.deleted = 0
                AND fs.stock_status = 'AVAILABLE' AND fs.coa_frozen = 0
                AND fs.quality_status = 'OK' AND fs.coa_frozen = 0
                AND GREATEST(COALESCE(fs.qty, 1) - COALESCE(lock_sum.locked_qty, 0), 0) > 0
              UNION ALL
              SELECT
                cr.model_code,
                COALESCE(NULLIF(cr.parent_production_batch_no, ''),
                  NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no) AS batch_no,
                cr.material_code,
                cr.material_name,
                cr.production_batch_no AS slice_batch_no,
                1 AS available_qty,
                0 AS locked_qty,
                1 AS total_qty,
                0 AS stock_available_qty,
                1 AS packaging_ready_qty,
                COALESCE(cr.confirmer_time, cr.inspection_time, cr.create_time) AS sort_time
              FROM mes_sfc_cut_round_report cr
              WHERE cr.deleted = 0
                AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
                AND cr.inspection_status = 'COMPLETED'
                AND UPPER(COALESCE(cr.inspection_result, '')) <> 'NG'
                AND UPPER(COALESCE(cr.self_check, '')) <> 'NG'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'ADHESIVE2_REPORT'
                    AND fai.source_report_no LIKE '%-COA-%'
                    AND UPPER(TRIM(fai.product_batch_no)) LIKE CONCAT(
                      CASE
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '-J[0-9]+$'
                          THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))), '-J', 1)
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '-S[0-9]+$'
                          THEN SUBSTRING_INDEX(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))), '-S', 1)
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$'
                          THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))),
                                    CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no)))) - 4)
                        WHEN UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))) REGEXP '[PQRS][0-9][0-9][0-9]$'
                          THEN LEFT(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no))),
                                    CHAR_LENGTH(UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no)))) - 3)
                        ELSE UPPER(TRIM(COALESCE(NULLIF(cr.parent_production_batch_no, ''), NULLIF(cr.source_production_batch_no, ''), cr.production_batch_no)))
                      END,
                      '%')
                    AND (UPPER(COALESCE(fai.judgment, '')) = 'NG'
                      OR UPPER(COALESCE(fai.status, '')) = 'REJECTED')
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_sfc_inner_pack_unit_item packed_item
                  WHERE packed_item.deleted = 0
                    AND (packed_item.source_cut_round_report_id = cr.id
                      OR COALESCE(NULLIF(packed_item.slice_batch_no, ''), packed_item.production_batch_no) = cr.production_batch_no)
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_finished_stock existing_stock
                  WHERE existing_stock.deleted = 0
                    AND existing_stock.slice_batch_no = cr.production_batch_no
                )
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_inv_fg_shipping_pick_item active_pick
                  WHERE active_pick.source_cut_round_report_id = cr.id
                    AND active_pick.deleted = 0
                    AND active_pick.lock_status IN ('PICKED', 'SHIP_CONFIRMED', 'INSPECTED', 'PACKAGED', 'LOCKED', 'OUTBOUND')
                )
            )
            SELECT
              c.model_code AS modelCode,
              c.batch_no AS batchNo,
              c.material_code AS materialCode,
              MAX(c.material_name) AS materialName,
              SUM(c.available_qty) AS availableQty,
              SUM(c.stock_available_qty) AS stockAvailableQty,
              SUM(c.packaging_ready_qty) AS packagingReadyQty,
              SUM(c.locked_qty) AS lockedQty,
              SUM(c.total_qty) AS totalQty
            FROM candidates c
            WHERE c.batch_no IS NOT NULL
              AND c.batch_no <> ''
              AND (#{req.keyword} IS NULL OR #{req.keyword} = ''
                OR c.model_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR c.material_code LIKE CONCAT('%', #{req.keyword}, '%')
                OR c.material_name LIKE CONCAT('%', #{req.keyword}, '%')
                OR c.slice_batch_no LIKE CONCAT('%', #{req.keyword}, '%')
                OR c.batch_no LIKE CONCAT('%', #{req.keyword}, '%'))
              AND (#{req.modelCode} IS NULL OR #{req.modelCode} = ''
                OR c.model_code LIKE CONCAT('%', #{req.modelCode}, '%'))
              AND (#{req.materialCode} IS NULL OR #{req.materialCode} = ''
                OR c.material_code LIKE CONCAT('%', #{req.materialCode}, '%'))
              AND (#{req.batchNo} IS NULL OR #{req.batchNo} = ''
                OR c.slice_batch_no LIKE CONCAT('%', #{req.batchNo}, '%')
                OR c.batch_no LIKE CONCAT('%', #{req.batchNo}, '%'))
            GROUP BY c.model_code, c.batch_no, c.material_code
            HAVING availableQty > 0
            ORDER BY MAX(c.sort_time) DESC, batchNo DESC
            LIMIT #{offset}, #{limit}
            """)
    List<FgShippingBatchCandidateRespVO> selectShippingNoticeInternalItemCandidatePage(@Param("req") FgShippingBatchCandidatePageReqVO reqVO,
                                                                                        @Param("offset") Integer offset,
                                                                                        @Param("limit") Integer limit);

    @Select("""
            SELECT
              cr.plan_id AS planId,
              cr.plan_no AS planNo,
              pack_op.id AS planOperationId,
              COALESCE(cr.parent_production_batch_no, cr.source_production_batch_no, cr.production_batch_no) AS motherSegmentBatchNo,
              MAX(cr.material_code) AS materialCode,
              MAX(cr.material_name) AS materialName,
              MAX(cr.model_code) AS modelCode,
              COUNT(1) AS totalPieceCount,
              SUM(CASE WHEN packed.id IS NULL THEN 0 ELSE 1 END) AS packedPieceCount,
              SUM(CASE WHEN packed.id IS NULL THEN 1 ELSE 0 END) AS waitPackPieceCount
            FROM mes_sfc_cut_round_report cr
            LEFT JOIN (
              SELECT plan_id, MIN(id) AS id
              FROM mes_pp_plan_operation
              WHERE deleted = 0
                AND (op_code IN ('OP-PACK-IN', 'OP-PACKAGING', 'OP-PACK')
                  OR op_name LIKE '%包装%'
                  OR op_name LIKE '%内包%')
              GROUP BY plan_id
            ) pack_idx ON pack_idx.plan_id = cr.plan_id
            LEFT JOIN mes_pp_plan_operation pack_op ON pack_op.id = pack_idx.id
            LEFT JOIN mes_sfc_inner_pack_unit_item packed
              ON packed.source_cut_round_report_id = cr.id
             AND packed.deleted = 0
            WHERE cr.deleted = 0
              AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR cr.plan_no LIKE CONCAT('%', #{keyword}, '%')
                OR cr.parent_production_batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR cr.source_production_batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR cr.production_batch_no LIKE CONCAT('%', #{keyword}, '%')
                OR cr.model_code LIKE CONCAT('%', #{keyword}, '%')
                OR cr.material_code LIKE CONCAT('%', #{keyword}, '%'))
            GROUP BY cr.plan_id, cr.plan_no, pack_op.id,
              COALESCE(cr.parent_production_batch_no, cr.source_production_batch_no, cr.production_batch_no)
            HAVING waitPackPieceCount > 0
            ORDER BY MAX(cr.confirmer_time) DESC, cr.plan_no DESC
            """)
    List<InboundTaskRespVO> selectInboundTaskList(@Param("keyword") String keyword);

    @Select("""
            SELECT
              cr.id AS sourceCutRoundReportId,
              cr.plan_id AS planId,
              cr.plan_no AS planNo,
              cr.plan_operation_id AS planOperationId,
              cr.production_batch_no AS sliceBatchNo,
              cr.production_batch_no AS productionBatchNo,
              cr.parent_production_batch_no AS parentProductionBatchNo,
              cr.material_code AS materialCode,
              cr.material_name AS materialName,
              cr.model_code AS modelCode,
              COALESCE(cr.inspection_result, cr.self_check, 'OK') AS qualityStatus,
              cr.report_status AS reportStatus,
              CASE WHEN packed.id IS NULL THEN '待包装' ELSE '已包装' END AS packageStatus,
              cr.report_date AS reportDate,
              cr.confirmer_time AS confirmTime
            FROM mes_sfc_cut_round_report cr
            LEFT JOIN mes_sfc_inner_pack_unit_item packed
              ON packed.source_cut_round_report_id = cr.id
             AND packed.deleted = 0
            WHERE cr.deleted = 0
              AND cr.plan_id = #{planId}
              AND COALESCE(cr.parent_production_batch_no, cr.source_production_batch_no, cr.production_batch_no) = #{motherSegmentBatchNo}
              AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
            ORDER BY cr.production_batch_no ASC, cr.id ASC
            """)
    List<HcPackagingSourceRespVO> selectInboundSourceList(@Param("planId") Long planId,
                                                          @Param("motherSegmentBatchNo") String motherSegmentBatchNo);

    @Select("""
            SELECT
              cr.id AS sourceCutRoundReportId,
              cr.plan_id AS planId,
              cr.plan_no AS planNo,
              cr.plan_operation_id AS planOperationId,
              cr.production_batch_no AS sliceBatchNo,
              cr.production_batch_no AS productionBatchNo,
              cr.parent_production_batch_no AS parentProductionBatchNo,
              cr.material_code AS materialCode,
              cr.material_name AS materialName,
              cr.model_code AS modelCode,
              COALESCE(cr.inspection_result, cr.self_check, 'OK') AS qualityStatus,
              cr.report_status AS reportStatus,
              CASE WHEN packed.id IS NULL THEN '待包装' ELSE '已包装' END AS packageStatus,
              cr.report_date AS reportDate,
              cr.confirmer_time AS confirmTime
            FROM mes_sfc_cut_round_report cr
            LEFT JOIN mes_sfc_inner_pack_unit_item packed
              ON packed.source_cut_round_report_id = cr.id
             AND packed.deleted = 0
            WHERE cr.deleted = 0
              AND cr.plan_id = #{planId}
              AND COALESCE(cr.parent_production_batch_no, cr.source_production_batch_no, cr.production_batch_no) = #{motherSegmentBatchNo}
              AND cr.production_batch_no = #{sliceBatchNo}
              AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
            ORDER BY cr.id DESC
            LIMIT 1
            """)
    HcPackagingSourceRespVO selectInboundSource(@Param("planId") Long planId,
                                                @Param("motherSegmentBatchNo") String motherSegmentBatchNo,
                                                @Param("sliceBatchNo") String sliceBatchNo);

    @Select("""
            SELECT
              so.id AS sourceSaleOrderId,
              so.order_no AS shippingOrderNo,
              so.erp_no AS erpOrderNo,
              so.material_code AS materialCode,
              so.material_name AS materialName,
              so.model_code AS modelCode,
              COALESCE(so.size_name, so.size_spec, so.product_spec) AS productSize,
              CAST(COALESCE(so.remain_qty, so.quantity, 0) AS SIGNED) AS shipQty,
              CASE WHEN so.delivery_date IS NULL THEN NULL ELSE CAST(CONCAT(so.delivery_date, ' 00:00:00') AS DATETIME) END AS shippingTime
            FROM mes_plan_sale_order so
            WHERE so.deleted = 0
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR so.order_no LIKE CONCAT('%', #{keyword}, '%')
                OR so.erp_no LIKE CONCAT('%', #{keyword}, '%')
                OR so.material_code LIKE CONCAT('%', #{keyword}, '%')
                OR so.model_code LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY so.delivery_date ASC, so.id DESC
            LIMIT 100
            """)
    List<ShippingOrderRespVO> selectShippingOrderList(@Param("keyword") String keyword);
}
