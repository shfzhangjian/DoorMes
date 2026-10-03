package cn.iocoder.yudao.module.mes.dal.mysql.hc.testouterprint;

import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterCustomerProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.testouterprint.vo.HcTestOuterPackagePrintVO.TestOuterPieceRespVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcTestOuterPackagePrintMapper {

    @Select("""
            SELECT *
            FROM (
              SELECT
                'CUT_ROUND_REPORT' AS sourceType,
                cr.id AS sourceId,
                cr.id AS sourceCutRoundReportId,
                NULL AS sourceManualPieceId,
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
                END AS segmentBatchNo,
                cr.production_batch_no AS sliceBatchNo,
                cr.production_batch_no AS productionBatchNo,
                cr.parent_production_batch_no AS parentProductionBatchNo,
                cr.material_code AS materialCode,
                cr.material_name AS materialName,
                cr.model_code AS modelCode,
                COALESCE(fs.production_date, DATE(COALESCE(cr.confirmer_time, cr.end_time, cr.recorder_time, cr.create_time))) AS productionDate,
                COALESCE(fs.expiry_date,
                  DATE_SUB(DATE_ADD(DATE(COALESCE(cr.confirmer_time, cr.end_time, cr.recorder_time, cr.create_time)), INTERVAL 10 MONTH), INTERVAL 1 DAY)
                ) AS expiryDate,
                cr.inspection_task_no AS inspectionTaskNo,
                cr.inspection_result AS inspectionResult,
                'OK' AS coaInspectionResult,
                'OK' AS packagingQualityStatus,
                cr.inspection_time AS inspectionTime,
                cr.inspection_remark AS remark,
                COALESCE(cr.inspection_time, cr.confirmer_time, cr.end_time, cr.recorder_time, cr.create_time) AS sortTime
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
                AND cr.inspection_status = 'COMPLETED'
                AND cr.production_batch_no IS NOT NULL
                AND cr.production_batch_no != ''
                AND fs.id IS NULL
                AND packed.id IS NULL
                AND UPPER(COALESCE(cr.inspection_result, '')) != 'NG'
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
              UNION ALL
              SELECT
                'MANUAL_HISTORY' AS sourceType,
                mp.id AS sourceId,
                NULL AS sourceCutRoundReportId,
                mp.id AS sourceManualPieceId,
                mp.segment_batch_no AS segmentBatchNo,
                mp.slice_batch_no AS sliceBatchNo,
                mp.slice_batch_no AS productionBatchNo,
                mp.segment_batch_no AS parentProductionBatchNo,
                mp.material_code AS materialCode,
                mp.material_name AS materialName,
                mp.model_code AS modelCode,
                mp.production_date AS productionDate,
                mp.expiry_date AS expiryDate,
                NULL AS inspectionTaskNo,
                mp.inspection_result AS inspectionResult,
                mp.coa_inspection_result AS coaInspectionResult,
                'OK' AS packagingQualityStatus,
                mp.recorder_time AS inspectionTime,
                COALESCE(mp.remark, '历史片补录') AS remark,
                COALESCE(mp.recorder_time, mp.create_time) AS sortTime
              FROM mes_sfc_packaging_manual_piece mp
              WHERE mp.deleted = 0
                AND mp.record_status = 'WAIT_PACKAGING'
                AND mp.slice_batch_no IS NOT NULL
                AND mp.slice_batch_no != ''
                AND UPPER(COALESCE(mp.inspection_result, '')) != 'NG'
                AND UPPER(COALESCE(mp.coa_inspection_result, '')) != 'NG'
            ) candidate
            WHERE (#{keyword} IS NULL OR #{keyword} = ''
                OR candidate.segmentBatchNo LIKE CONCAT('%', #{keyword}, '%')
                OR candidate.sliceBatchNo LIKE CONCAT('%', #{keyword}, '%')
                OR candidate.materialCode LIKE CONCAT('%', #{keyword}, '%')
                OR candidate.materialName LIKE CONCAT('%', #{keyword}, '%')
                OR candidate.modelCode LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{segmentBatchNo} IS NULL OR #{segmentBatchNo} = ''
                OR candidate.segmentBatchNo = #{segmentBatchNo})
              AND (#{sliceBatchNosCsv} IS NULL OR #{sliceBatchNosCsv} = ''
                OR FIND_IN_SET(candidate.sliceBatchNo, #{sliceBatchNosCsv}) > 0)
            ORDER BY candidate.segmentBatchNo DESC, candidate.sortTime DESC, candidate.sliceBatchNo ASC
            LIMIT #{limit}
            """)
    List<TestOuterPieceRespVO> selectQualifiedWaitPieces(@Param("keyword") String keyword,
                                                         @Param("segmentBatchNo") String segmentBatchNo,
                                                         @Param("sliceBatchNosCsv") String sliceBatchNosCsv,
                                                         @Param("limit") int limit);

    @Select("""
            SELECT
              CONCAT(c.id, '-', p.id) AS rowKey,
              c.id AS customerInfoId,
              p.id AS productItemId,
              c.serial_no AS serialNo,
              p.source_row AS sourceRow,
              c.customer AS customer,
              COALESCE(NULLIF(p.product_type, ''), c.product_type) AS productType,
              COALESCE(NULLIF(p.size_mm, ''), c.size_mm) AS sizeMm,
              COALESCE(
                (SELECT b.design_id
                 FROM mes_hc_visual_print_label_binding b
                 WHERE b.deleted = 0
                   AND b.product_item_id = p.id
                   AND b.label_kind = #{labelKind}
                 ORDER BY b.id
                 LIMIT 1),
                (SELECT d.id
                 FROM mes_hc_visual_print_design d
                 WHERE d.deleted = 0
                   AND d.customer_info_id = c.id
                   AND d.product_item_id = p.id
                   AND d.label_kind = #{labelKind}
                 ORDER BY d.id
                 LIMIT 1),
                (SELECT d.id
                 FROM mes_hc_visual_print_design d
                 WHERE d.deleted = 0
                   AND d.customer_info_id = c.id
                   AND d.product_item_id IS NULL
                   AND d.label_kind = #{labelKind}
                 ORDER BY d.id
                 LIMIT 1)
              ) AS designId,
              CASE #{labelKind}
                WHEN 'padBack' THEN COALESCE(NULLIF(p.pad_back_label_image_id, ''), c.pad_back_label_image_id)
                WHEN 'cleanBag' THEN COALESCE(NULLIF(p.clean_bag_label_image_id, ''), c.clean_bag_label_image_id)
                WHEN 'boxFront' THEN COALESCE(NULLIF(p.box_front_label_image_id, ''), c.box_front_label_image_id)
                WHEN 'customerSide' THEN COALESCE(NULLIF(p.customer_side_label_image_id, ''), c.customer_side_label_image_id)
                ELSE NULL
              END AS labelImageId,
              CASE #{labelKind}
                WHEN 'padBack' THEN COALESCE(NULLIF(p.pad_back_label_image_file, ''), c.pad_back_label_image_file)
                WHEN 'cleanBag' THEN COALESCE(NULLIF(p.clean_bag_label_image_file, ''), c.clean_bag_label_image_file)
                WHEN 'boxFront' THEN COALESCE(NULLIF(p.box_front_label_image_file, ''), c.box_front_label_image_file)
                WHEN 'customerSide' THEN COALESCE(NULLIF(p.customer_side_label_image_file, ''), c.customer_side_label_image_file)
                ELSE NULL
              END AS labelImageFile,
              c.customer_side_size AS customerSideSize,
              c.customer_side_method AS customerSideMethod,
              c.shipping_method AS shippingMethod,
              c.need_paper_coa AS needPaperCoa,
              c.need_ecoa AS needEcoa,
              c.has_mark AS hasMark,
              c.delivery_note AS deliveryNote,
              c.shipment_file_package_method AS shipmentFilePackageMethod,
              c.customer_side_template AS customerSideTemplate,
              c.special_remark AS specialRemark
            FROM mes_hc_visual_print_customer_info c
            INNER JOIN mes_hc_visual_print_product_item p
              ON p.customer_info_id = c.id
             AND p.deleted = 0
            WHERE c.deleted = 0
              AND (c.status IS NULL OR c.status = 0)
              AND (p.status IS NULL OR p.status = 0)
              AND (#{keyword} IS NULL OR #{keyword} = ''
                OR c.serial_no LIKE CONCAT('%', #{keyword}, '%')
                OR c.customer LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(NULLIF(p.product_type, ''), c.product_type) LIKE CONCAT('%', #{keyword}, '%')
                OR COALESCE(NULLIF(p.size_mm, ''), c.size_mm) LIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY
              CASE WHEN c.serial_no REGEXP '^[0-9]+$' THEN CAST(c.serial_no AS UNSIGNED) ELSE 2147483647 END ASC,
              c.serial_no ASC,
              c.id ASC,
              p.source_row ASC,
              p.id ASC
            LIMIT #{limit}
            """)
    List<TestOuterCustomerProductRespVO> selectCustomerProductCandidates(@Param("keyword") String keyword,
                                                                         @Param("labelKind") String labelKind,
                                                                         @Param("limit") int limit);
}
