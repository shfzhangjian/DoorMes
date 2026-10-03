package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcPackagingSourceMapper {

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
              CASE WHEN packed.id IS NULL THEN '待包装' ELSE '已内包' END AS packageStatus,
              cr.report_date AS reportDate,
              cr.confirmer_time AS confirmTime
            FROM mes_sfc_cut_round_report cr
            LEFT JOIN (
              SELECT source_cut_round_report_id, MAX(id) AS id
              FROM mes_sfc_inner_pack_unit_item
              WHERE deleted = 0
              GROUP BY source_cut_round_report_id
            ) packed ON packed.source_cut_round_report_id = cr.id
            WHERE cr.deleted = 0
              AND cr.plan_id = #{planId}
              AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
            ORDER BY cr.production_batch_no ASC, cr.id ASC
            """)
    List<HcPackagingSourceRespVO> selectConfirmedListByPlanId(@Param("planId") Long planId);

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
              CASE WHEN packed.id IS NULL THEN '待包装' ELSE '已内包' END AS packageStatus,
              cr.report_date AS reportDate,
              cr.confirmer_time AS confirmTime
            FROM mes_sfc_cut_round_report cr
            LEFT JOIN (
              SELECT source_cut_round_report_id, MAX(id) AS id
              FROM mes_sfc_inner_pack_unit_item
              WHERE deleted = 0
              GROUP BY source_cut_round_report_id
            ) packed ON packed.source_cut_round_report_id = cr.id
            WHERE cr.deleted = 0
              AND cr.production_batch_no = #{productionBatchNo}
              AND cr.report_status IN ('CONFIRMED', 'SUBMITTED')
            ORDER BY cr.id DESC
            LIMIT 1
            """)
    HcPackagingSourceRespVO selectConfirmedByProductionBatchNo(@Param("productionBatchNo") String productionBatchNo);
}
