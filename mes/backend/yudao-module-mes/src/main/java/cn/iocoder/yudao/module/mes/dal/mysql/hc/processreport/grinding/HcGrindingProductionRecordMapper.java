package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordRespVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcGrindingProductionRecordMapper {

    @Select("""
            <script>
            SELECT records.*
            FROM (
                SELECT
                    base.detailId,
                    base.passType,
                    base.reportDate,
                    base.modelCode,
                    base.materialCode,
                    base.batchNo,
                    base.sourceProductionBatchNo,
                    base.inputLength,
                    base.outputLength,
                    base.passName,
                    COALESCE(sand_use.after_used_length, base.detailSandpaperLife) AS sandpaperLife,
                    COALESCE(NULLIF(sand_use.after_batch_no, ''), NULLIF(base.detailSandpaperBatchNo, '')) AS sandpaperBatchNo,
                    (
                        SELECT guide_use.after_use_count
                        FROM mes_sfc_equipment_consumable_event guide_use
                        WHERE guide_use.deleted = 0
                          AND guide_use.biz_type = CONCAT('GRINDING_', base.passType)
                          AND guide_use.biz_id = base.detailId
                          AND guide_use.consumable_type = 'GUIDE_CLOTH'
                          AND guide_use.event_type = 'USE'
                        ORDER BY guide_use.event_time DESC, guide_use.id DESC
                        LIMIT 1
                    ) AS guideClothLife,
                    COALESCE(
                        (
                            SELECT NULLIF(guide_use.after_batch_no, '')
                            FROM mes_sfc_equipment_consumable_event guide_use
                            WHERE guide_use.deleted = 0
                              AND guide_use.biz_type = CONCAT('GRINDING_', base.passType)
                              AND guide_use.biz_id = base.detailId
                              AND guide_use.consumable_type = 'GUIDE_CLOTH'
                              AND guide_use.event_type = 'USE'
                            ORDER BY guide_use.event_time DESC, guide_use.id DESC
                            LIMIT 1
                        ),
                        NULLIF(base.detailGuideClothBatchNo, '')
                    ) AS guideClothBatchNo,
                    (
                        SELECT GROUP_CONCAT(
                            CASE replace_event.consumable_type
                                WHEN 'SANDPAPER' THEN CONCAT('砂纸：', replace_event.replace_reason)
                                WHEN 'GUIDE_CLOTH' THEN CONCAT('导布：', replace_event.replace_reason)
                                ELSE replace_event.replace_reason
                            END
                            ORDER BY replace_event.id
                            SEPARATOR '；'
                        )
                        FROM mes_sfc_equipment_consumable_event replace_event
                        WHERE replace_event.deleted = 0
                          AND replace_event.biz_type = CONCAT('GRINDING_', base.passType)
                          AND replace_event.biz_id = base.detailId
                          AND replace_event.event_type = 'REPLACE'
                          AND replace_event.replace_reason IS NOT NULL
                          AND replace_event.replace_reason != ''
                    ) AS replaceReason,
                    COALESCE(
                        NULLIF(sand_use.operator_name, ''),
                        (
                            SELECT NULLIF(guide_use.operator_name, '')
                            FROM mes_sfc_equipment_consumable_event guide_use
                            WHERE guide_use.deleted = 0
                              AND guide_use.biz_type = CONCAT('GRINDING_', base.passType)
                              AND guide_use.biz_id = base.detailId
                              AND guide_use.consumable_type = 'GUIDE_CLOTH'
                              AND guide_use.event_type = 'USE'
                            ORDER BY guide_use.event_time DESC, guide_use.id DESC
                            LIMIT 1
                        ),
                        NULLIF(base.recorderName, ''),
                        NULLIF(base.creatorName, '')
                    ) AS recorderName,
                    base.remark,
                    sand_use.id AS sandpaperEventId
                FROM (
                    SELECT
                        first_detail.id AS detailId,
                        'FIRST' AS passType,
                        first_detail.report_date AS reportDate,
                        COALESCE(NULLIF(report.mother_model_code, ''), NULLIF(plan_order.mother_model_code, ''),
                                 NULLIF(plan_operation.mother_model_code, ''), NULLIF(plan_order.model_code, '')) AS modelCode,
                        COALESCE(NULLIF(report.mother_material_code, ''), NULLIF(plan_order.mother_material_code, ''),
                                 NULLIF(plan_operation.mother_material_code, ''), NULLIF(plan_order.material_code, '')) AS materialCode,
                        first_detail.mother_batch_no AS batchNo,
                        first_detail.source_production_batch_no AS sourceProductionBatchNo,
                        first_detail.process_length AS inputLength,
                        first_detail.output_length AS outputLength,
                        '一次' AS passName,
                        first_detail.sandpaper_life AS detailSandpaperLife,
                        first_detail.sandpaper_batch_no AS detailSandpaperBatchNo,
                        first_detail.current_guide_cloth_batch_no AS detailGuideClothBatchNo,
                        report.recorder_name AS recorderName,
                        first_detail.creator AS creatorName,
                        first_detail.remark AS remark
                    FROM mes_sfc_grinding_first_detail first_detail
                    LEFT JOIN mes_sfc_grinding_report report
                           ON report.id = first_detail.grinding_report_id
                          AND report.deleted = 0
                    LEFT JOIN mes_pp_plan_order plan_order
                           ON plan_order.id = first_detail.plan_id
                          AND plan_order.deleted = 0
                    LEFT JOIN mes_pp_plan_operation plan_operation
                           ON plan_operation.id = first_detail.plan_operation_id
                          AND plan_operation.deleted = 0
                    WHERE first_detail.deleted = 0
                    UNION ALL
                    SELECT
                        second_detail.id AS detailId,
                        'SECOND' AS passType,
                        second_detail.report_date AS reportDate,
                        COALESCE(NULLIF(report.mother_model_code, ''), NULLIF(plan_order.mother_model_code, ''),
                                 NULLIF(plan_operation.mother_model_code, ''), NULLIF(plan_order.model_code, '')) AS modelCode,
                        COALESCE(NULLIF(report.mother_material_code, ''), NULLIF(plan_order.mother_material_code, ''),
                                 NULLIF(plan_operation.mother_material_code, ''), NULLIF(plan_order.material_code, '')) AS materialCode,
                        COALESCE(NULLIF(second_detail.production_batch_no, ''), NULLIF(second_detail.mother_batch_no, '')) AS batchNo,
                        second_detail.source_production_batch_no AS sourceProductionBatchNo,
                        second_detail.process_length AS inputLength,
                        second_detail.output_length AS outputLength,
                        '二次' AS passName,
                        second_detail.sandpaper_life AS detailSandpaperLife,
                        second_detail.sandpaper_batch_no AS detailSandpaperBatchNo,
                        second_detail.current_guide_cloth_batch_no AS detailGuideClothBatchNo,
                        report.recorder_name AS recorderName,
                        second_detail.creator AS creatorName,
                        second_detail.remark AS remark
                    FROM mes_sfc_grinding_second_detail second_detail
                    LEFT JOIN mes_sfc_grinding_report report
                           ON report.id = second_detail.grinding_report_id
                          AND report.deleted = 0
                    LEFT JOIN mes_pp_plan_order plan_order
                           ON plan_order.id = second_detail.plan_id
                          AND plan_order.deleted = 0
                    LEFT JOIN mes_pp_plan_operation plan_operation
                           ON plan_operation.id = second_detail.plan_operation_id
                          AND plan_operation.deleted = 0
                    WHERE second_detail.deleted = 0
                ) base
                LEFT JOIN mes_sfc_equipment_consumable_event sand_use
                       ON sand_use.deleted = 0
                      AND sand_use.biz_type = CONCAT('GRINDING_', base.passType)
                      AND sand_use.biz_id = base.detailId
                      AND sand_use.consumable_type = 'SANDPAPER'
                      AND sand_use.event_type = 'USE'
            ) records
            WHERE 1 = 1
            <if test="reqVO.reportDateStart != null">
              AND records.reportDate &gt;= #{reqVO.reportDateStart}
            </if>
            <if test="reqVO.reportDateEnd != null">
              AND records.reportDate &lt;= #{reqVO.reportDateEnd}
            </if>
            <if test="reqVO.modelCode != null and reqVO.modelCode != ''">
              AND records.modelCode LIKE CONCAT('%', #{reqVO.modelCode}, '%')
            </if>
            <if test="reqVO.materialCode != null and reqVO.materialCode != ''">
              AND records.materialCode LIKE CONCAT('%', #{reqVO.materialCode}, '%')
            </if>
            <if test="reqVO.batchNo != null and reqVO.batchNo != ''">
              AND (
                records.batchNo LIKE CONCAT('%', #{reqVO.batchNo}, '%')
                OR records.sourceProductionBatchNo LIKE CONCAT('%', #{reqVO.batchNo}, '%')
              )
            </if>
            <if test="reqVO.grindingPass != null and reqVO.grindingPass != ''">
              AND records.passType = #{reqVO.grindingPass}
            </if>
            <if test="reqVO.recorderName != null and reqVO.recorderName != ''">
              AND records.recorderName LIKE CONCAT('%', #{reqVO.recorderName}, '%')
            </if>
            ORDER BY records.reportDate,
                     records.modelCode,
                     records.materialCode,
                     records.batchNo,
                     records.passType,
                     records.detailId,
                     records.sandpaperEventId
            </script>
            """)
    List<HcGrindingProductionRecordRespVO> selectProductionRecordList(
            @Param("reqVO") HcGrindingProductionRecordPageReqVO reqVO);

}
