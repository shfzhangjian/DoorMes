package cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotFirstInspectionSampleClaimMapper;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HcProcessReportMapper extends BaseMapperX<HcProcessReportDO> {

    record OperationDailyQtyRow(Long planOperationId, LocalDate reportDate, BigDecimal goodQty) {
    }

    record OperationLatestReportRow(
            Long planOperationId,
            LocalDate reportDate,
            java.time.LocalDateTime startTime,
            java.time.LocalDateTime endTime,
            String recorderName,
            String confirmerName,
            String remark) {
    }

    record PlanProcessPivotStageRow(
            Long planId,
            String segmentBatchNo,
            String stageCode,
            String stageName,
            String sourceBatchNos,
            String outputBatchNos,
            BigDecimal inputQty,
            BigDecimal doneQty,
            BigDecimal reportQty,
            BigDecimal defectQty,
            BigDecimal confirmedQty,
            BigDecimal lengthQty,
            BigDecimal startPosition,
            BigDecimal processLength,
            java.time.LocalDateTime lastReportTime,
            String reportUnit,
            String remark) {
    }

    record PlanProcessPivotStageStartRow(
            Long planId,
            String segmentBatchNo,
            String stageCode,
            java.time.LocalDateTime startTime) {
    }

    record PlanProcessPivotPieceRow(
            Long planId,
            String segmentBatchNo,
            String stageCode,
            String pieceNo,
            String sourceBatchNo,
            String outputBatchNo,
            String actualModelCode,
            String actualSizeSpec,
            Integer reportConfirmed,
            Integer defectFlag,
            Integer coaFlag,
            Integer sourceSlittingOkFlag,
            java.time.LocalDateTime lastReportTime,
            String remark) {
    }

    record PlanProcessPivotInspectionRow(
            Long planId,
            String segmentBatchNo,
            String stageCode,
            String sourceType,
            Long inspectionId,
            String inspectionNo,
            String inspectionType,
            String productBatchNo,
            BigDecimal inspectionQty,
            BigDecimal inspectionNgQty,
            Integer coaInspectionFlag,
            Integer firstInspectionSampleFlag,
            Integer sourceSlittingOkFlag,
            String judgment,
            String status,
            java.time.LocalDateTime inspectionTime,
            String defectSummary,
            String remark) {
    }

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              lr.id AS operationReportId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              o.op_name AS process,
              COALESCE(lr.mixer_equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.mixer_equipment_code, o.equipment_code) AS equipmentCode,
              o.work_center_id AS workCenterId,
              COALESCE(lr.mixer_equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              p.remark AS requirements,
              p.recipe_code AS recipeCode,
              p.recipe_name AS recipeName,
              lr.batching_no AS batchingNo,
              lr.feed_batch_no AS feedBatchNo,
              lr.feed_qty AS feedQty,
              lr.stir_start_time AS stirStartTime,
              lr.stir_end_time AS stirEndTime,
              lr.end_time AS endTime,
              lr.recorder_name AS recorderName,
              lr.recorder_time AS recorderTime,
              lr.confirmer_name AS confirmerName,
              lr.confirmer_time AS confirmerTime,
              lr.mixer_equipment_id AS mixerEquipmentId,
              lr.mixer_equipment_code AS mixerEquipmentCode,
              lr.mixer_equipment_name AS mixerEquipmentName,
              lr.foaming_equipment_id AS foamingEquipmentId,
              lr.foaming_equipment_code AS foamingEquipmentCode,
              lr.foaming_equipment_name AS foamingEquipmentName,
              lr.viscosity AS viscosity,
              lr.slurry_temperature AS slurryTemperature,
              lr.filter_batch_no AS filterBatchNo,
              lr.input_weight AS inputWeight,
              lr.batching_tank_no AS batchingTankNo,
              lr.defoaming_tank_no AS defoamingTankNo,
              lr.remark AS reportRemark
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN (
              SELECT
                latest.plan_operation_id,
                SUM(latest.good_qty) AS good_qty,
                SUM(latest.scrap_qty) AS scrap_qty,
                MIN(latest.start_time) AS first_start_time,
                COUNT(1) AS report_count
              FROM (
                SELECT r1.*
                FROM mes_sfc_operation_report r1
                INNER JOIN (
                  SELECT plan_operation_id, report_date, MAX(id) AS max_id
                  FROM mes_sfc_operation_report
                  WHERE deleted = 0
                    AND source_menu_code = 'FORMULA_REPORT'
                  GROUP BY plan_operation_id, report_date
                ) latest_daily ON latest_daily.max_id = r1.id
              ) latest
              GROUP BY latest.plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'FORMULA_REPORT'
            )
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND o.op_name = '配料'
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.model_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.model_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_model_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.material_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_material_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lr.production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR o.production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lr.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR o.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lr.parent_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR o.parent_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.parent_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%'))
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC
            </script>
            """)
    List<HcFormulaReportTaskRespVO> selectFormulaTaskList(@Param("taskStatus") String taskStatus,
                                                          @Param("taskKeyword") String taskKeyword,
                                                          @Param("productKeyword") String productKeyword,
                                                          @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                          @Param("motherModelKeyword") String motherModelKeyword);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              lr.id AS operationReportId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              o.work_center_id AS workCenterId,
              COALESCE(lr.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              lr.end_time AS endTime,
              p.remark AS requirements,
              lr.recorder_name AS recorderName,
              lr.recorder_time AS recorderTime,
              lr.confirmer_name AS confirmerName,
              lr.confirmer_time AS confirmerTime,
              lr.remark AS reportRemark,
              lr.extra_json AS extraJson,
              po.op_name AS previousOperationName,
              CASE
                WHEN po.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN po.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN po.operation_status = 'RELEASED' THEN 'PENDING'
                ELSE NULL
              END AS previousOperationStatus,
              plr.report_date AS previousProductionDate,
              plr.start_time AS previousStartTime,
              plr.end_time AS previousEndTime,
              plr.recorder_name AS previousRecorderName,
              plr.good_qty AS previousGoodQty,
              COALESCE(qf.id, lr.fai_id) AS faiId,
              COALESCE(qf.fai_no, lr.fai_no) AS faiNo,
              COALESCE(qf.status, lr.fai_status) AS faiStatus,
              COALESCE(qf.judgment, lr.fai_judgment) AS faiJudgment,
              COALESCE(qf.standard_id, lr.fai_standard_id) AS faiStandardId,
              COALESCE(qf.standard_no, lr.fai_standard_no) AS faiStandardNo,
              lr.fai_apply_time AS faiApplyTime,
              COALESCE(qf.update_time, lr.fai_return_time) AS faiReturnTime,
              COALESCE(qf.last_return_reason, lr.fai_reject_reason) AS faiRejectReason,
              qf.sample_length AS napSampleLength
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN mes_pp_plan_operation po ON po.id = (
              SELECT pox.id
              FROM mes_pp_plan_operation pox
              WHERE pox.deleted = 0
                AND pox.plan_id = o.plan_id
                AND pox.op_seq &lt; o.op_seq
              ORDER BY pox.op_seq DESC
              LIMIT 1
            )
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time,
                COUNT(1) AS report_count
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'WET_REPORT'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'WET_REPORT'
            )
            LEFT JOIN mes_sfc_operation_report plr ON plr.id = (
              SELECT MAX(pr.id)
              FROM mes_sfc_operation_report pr
              WHERE pr.deleted = 0
                AND pr.plan_operation_id = po.id
                AND pr.report_type = 'END'
            )
            LEFT JOIN mes_qms_fai_order qf ON qf.id = (
              SELECT MAX(f2.id)
              FROM mes_qms_fai_order f2
              WHERE f2.deleted = 0
                AND f2.source_module = 'WET_REPORT'
                AND f2.source_report_no = CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0'))
            )
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND o.op_name LIKE '湿法%'
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.model_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.model_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_model_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.material_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_material_code LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lr.production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR o.production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lr.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR o.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lr.parent_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR o.parent_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR p.parent_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%'))
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="productionDate != null">
                AND lr.report_date = #{productionDate}
              </if>
              <if test="equipmentId != null">
                AND COALESCE(lr.equipment_id, o.equipment_id) = #{equipmentId}
              </if>
              <if test="equipmentId == null and equipmentCode != null and equipmentCode != ''">
                AND COALESCE(lr.equipment_code, o.equipment_code) = #{equipmentCode}
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC
            </script>
            """)
    List<HcWetReportTaskRespVO> selectWetTaskList(@Param("taskStatus") String taskStatus,
                                                  @Param("taskKeyword") String taskKeyword,
                                                  @Param("productKeyword") String productKeyword,
                                                  @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                  @Param("motherModelKeyword") String motherModelKeyword,
                                                  @Param("productionDate") LocalDate productionDate,
                                                  @Param("equipmentId") Long equipmentId,
                                                  @Param("equipmentCode") String equipmentCode);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              o.work_center_id AS workCenterId,
              COALESCE(lr.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              lr.end_time AS endTime,
              p.remark AS requirements,
              lr.recorder_name AS recorderName,
              lr.recorder_time AS recorderTime,
              lr.confirmer_name AS confirmerName,
              lr.confirmer_time AS confirmerTime,
              lr.remark AS reportRemark,
              lr.extra_json AS extraJson,
              COALESCE(plr.good_qty, 0) AS motherLength,
              GREATEST(
                COALESCE(plr.good_qty, 0)
                  - COALESCE(NULLIF(gfd.first_process_length, 0), gr.first_process_length, 0)
                  - COALESCE(NULLIF(gfd.first_loss_length, 0), gr.first_loss_length, 0),
                0
              ) AS remainingLength,
              COALESCE(NULLIF(gfd.first_process_length, 0), gr.first_process_length, 0) AS firstGrindingProcessLength,
              COALESCE(NULLIF(gsd.second_process_length, 0), gr.second_process_length, 0) AS secondGrindingProcessLength,
              po.op_name AS previousOperationName,
              CASE
                WHEN po.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN po.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN po.operation_status = 'RELEASED' THEN 'PENDING'
                ELSE NULL
              END AS previousOperationStatus,
              plr.report_date AS previousProductionDate,
              plr.start_time AS previousStartTime,
              plr.end_time AS previousEndTime,
              plr.recorder_name AS previousRecorderName,
              plr.good_qty AS previousGoodQty
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN mes_pp_plan_operation po ON po.id = (
              SELECT pox.id
              FROM mes_pp_plan_operation pox
              WHERE pox.deleted = 0
                AND pox.plan_id = o.plan_id
                AND pox.op_seq &lt; o.op_seq
              ORDER BY pox.op_seq DESC
              LIMIT 1
            )
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'ROUGH_GRINDING_REPORT'
                AND report_type = 'END'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN (
              SELECT
                latest_grinding.plan_operation_id AS plan_operation_id,
                COALESCE(latest_grinding.first_process_length, 0) AS first_process_length,
                COALESCE(latest_grinding.first_loss_length, 0) AS first_loss_length,
                COALESCE(latest_grinding.second_process_length, 0) AS second_process_length
              FROM mes_sfc_grinding_report latest_grinding
              INNER JOIN (
                SELECT plan_operation_id, MAX(id) AS max_id
                FROM mes_sfc_grinding_report
                WHERE deleted = 0
                  AND report_type IN ('PROGRESS', 'END')
                GROUP BY plan_operation_id
              ) latest_grinding_ids ON latest_grinding_ids.max_id = latest_grinding.id
              WHERE latest_grinding.deleted = 0
            ) gr ON gr.plan_operation_id = o.id
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(COALESCE(process_length, 0)) AS first_process_length,
                SUM(COALESCE(loss_length, 0)) AS first_loss_length
              FROM mes_sfc_grinding_first_detail
              WHERE deleted = 0
                AND COALESCE(detail_status, 'SUBMITTED') IN ('SUBMITTED', 'CONFIRMED')
              GROUP BY plan_operation_id
            ) gfd ON gfd.plan_operation_id = o.id
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(COALESCE(process_length, 0)) AS second_process_length
              FROM mes_sfc_grinding_second_detail
              WHERE deleted = 0
                AND COALESCE(detail_status, 'SUBMITTED') IN ('SUBMITTED', 'CONFIRMED')
              GROUP BY plan_operation_id
            ) gsd ON gsd.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'ROUGH_GRINDING_REPORT'
            )
            LEFT JOIN mes_sfc_operation_report plr ON plr.id = (
              SELECT MAX(pr.id)
              FROM mes_sfc_operation_report pr
              WHERE pr.deleted = 0
                AND pr.plan_operation_id = po.id
                AND pr.report_type = 'END'
            )
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND (o.op_name LIKE '%磨皮%' OR o.op_name LIKE '%粗磨%')
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%'))
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="productionDate != null">
                AND lr.report_date = #{productionDate}
              </if>
              <if test="equipmentId != null">
                AND COALESCE(lr.equipment_id, o.equipment_id) = #{equipmentId}
              </if>
              <if test="equipmentCode != null and equipmentCode != ''">
                AND COALESCE(lr.equipment_code, o.equipment_code) = #{equipmentCode}
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC
            </script>
            """)
    List<HcRoughReportTaskRespVO> selectRoughTaskList(@Param("taskStatus") String taskStatus,
                                                      @Param("taskKeyword") String taskKeyword,
                                                      @Param("productKeyword") String productKeyword,
                                                      @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                      @Param("motherModelKeyword") String motherModelKeyword,
                                                      @Param("productionDate") LocalDate productionDate,
                                                      @Param("equipmentId") Long equipmentId,
                                                      @Param("equipmentCode") String equipmentCode);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              o.work_center_id AS workCenterId,
              COALESCE(lr.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              lr.end_time AS endTime,
              p.remark AS requirements,
              lr.recorder_name AS recorderName,
              lr.recorder_time AS recorderTime,
              lr.confirmer_name AS confirmerName,
              lr.confirmer_time AS confirmerTime,
              lr.remark AS reportRemark,
              lr.extra_json AS extraJson,
              src.source_batch_no AS sourceBatchNo,
              src.source_production_batch_no AS sourceProductionBatchNo,
              COALESCE(src.available_source_length, 0) AS availableSourceLength,
              COALESCE(src.confirmed_source_count, 0) AS confirmedSourceCount
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'ADHESIVE_REPORT'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'ADHESIVE_REPORT'
            )
            LEFT JOIN (
              SELECT
                plan_id,
                COALESCE(
                  MAX(NULLIF(mother_batch_no, '')),
                  MAX(NULLIF(parent_production_batch_no, '')),
                  CASE
                    WHEN RIGHT(MAX(production_batch_no), 1) IN ('P', 'Q', 'R', 'S')
                    THEN LEFT(MAX(production_batch_no), CHAR_LENGTH(MAX(production_batch_no)) - 1)
                    ELSE MAX(production_batch_no)
                  END
                ) AS source_batch_no,
                MAX(production_batch_no) AS source_production_batch_no,
                SUM(COALESCE(output_length, process_length, 0)) AS available_source_length,
                COUNT(1) AS confirmed_source_count
              FROM mes_sfc_grinding_second_detail
              WHERE deleted = 0
                AND confirm_status = 'CONFIRMED'
              GROUP BY plan_id
            ) src ON src.plan_id = p.id
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND (o.op_name LIKE '%粘双面胶%' OR o.op_name LIKE '%粘胶1%' OR o.op_name LIKE '%粘胶一%')
              AND o.op_name NOT LIKE '%粘胶2%'
              AND o.op_name NOT LIKE '%背胶%'
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              AND (
                COALESCE(src.confirmed_source_count, 0) > 0
                OR EXISTS (
                  SELECT 1
                  FROM mes_pp_plan_inv_lock il
                  WHERE il.deleted = 0
                    AND il.plan_operation_id = o.id
                    AND (il.lock_type = 'WIP' OR il.stock_type = 'WIP')
                    AND COALESCE(il.lock_status, '') NOT IN ('CONSUMED', 'CANCELLED', 'RELEASED')
                    AND (
                      COALESCE(il.lock_qty, 0)
                      - COALESCE(il.consumed_qty, 0)
                      - COALESCE(il.released_qty, 0)
                    ) > 0
                )
                OR EXISTS (
                  SELECT 1
                  FROM mes_pp_plan_inv_lock il
                  INNER JOIN mes_sfc_adhesive_report ar
                    ON ar.deleted = 0
                    AND ar.report_status IN ('CONFIRMED', 'SUBMITTED')
                    AND (ar.source_plan_lock_id = il.id OR ar.id = il.consume_report_id)
                  WHERE il.deleted = 0
                    AND il.plan_operation_id = o.id
                    AND ar.plan_id = p.id
                    AND ar.plan_operation_id = o.id
                    AND (il.lock_type = 'WIP' OR il.stock_type = 'WIP')
                    AND COALESCE(il.lock_status, '') = 'CONSUMED'
                )
              )
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR EXISTS (
                    SELECT 1
                    FROM mes_pp_plan_inv_lock il
                    WHERE il.deleted = 0
                      AND il.plan_operation_id = o.id
                      AND (il.lock_type = 'WIP' OR il.stock_type = 'WIP')
                      AND COALESCE(il.lock_status, '') NOT IN ('CONSUMED', 'CANCELLED')
                      AND (il.source_plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.lot_no LIKE CONCAT('%', #{taskKeyword}, '%'))
                  ))
                  OR EXISTS (
                    SELECT 1
                    FROM mes_pp_plan_inv_lock il
                    INNER JOIN mes_sfc_adhesive_report ar
                      ON ar.deleted = 0
                      AND ar.report_status IN ('CONFIRMED', 'SUBMITTED')
                      AND (ar.source_plan_lock_id = il.id OR ar.id = il.consume_report_id)
                    WHERE il.deleted = 0
                      AND il.plan_operation_id = o.id
                      AND ar.plan_id = p.id
                      AND ar.plan_operation_id = o.id
                      AND (il.lock_type = 'WIP' OR il.stock_type = 'WIP')
                      AND COALESCE(il.lock_status, '') = 'CONSUMED'
                      AND (il.source_plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.lot_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR ar.production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR ar.source_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%'))
                  )
                )
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="productionDate != null">
                AND lr.report_date = #{productionDate}
              </if>
              <if test="equipmentId != null">
                AND COALESCE(lr.equipment_id, o.equipment_id) = #{equipmentId}
              </if>
              <if test="equipmentCode != null and equipmentCode != ''">
                AND COALESCE(lr.equipment_code, o.equipment_code) = #{equipmentCode}
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC
            </script>
            """)
    List<HcAdhesiveReportTaskRespVO> selectAdhesiveTaskList(@Param("taskStatus") String taskStatus,
                                                            @Param("taskKeyword") String taskKeyword,
                                                            @Param("productKeyword") String productKeyword,
                                                            @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                            @Param("motherModelKeyword") String motherModelKeyword,
                                                            @Param("productionDate") LocalDate productionDate,
                                                            @Param("equipmentId") Long equipmentId,
                                                            @Param("equipmentCode") String equipmentCode);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0'), '-', COALESCE(src.source_batch_no, 'NO-SOURCE')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              COALESCE(seg_done.report_date, lr.report_date) AS productionDate,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              o.work_center_id AS workCenterId,
              COALESCE(lr.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN seg_done.id IS NOT NULL OR o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              COALESCE(seg_done.end_time, lr.end_time) AS endTime,
              p.remark AS requirements,
              COALESCE(seg_done.recorder_name, lr.recorder_name) AS recorderName,
              COALESCE(seg_done.recorder_time, lr.recorder_time) AS recorderTime,
              COALESCE(seg_done.confirmer_name, lr.confirmer_name) AS confirmerName,
              COALESCE(seg_done.confirmer_time, lr.confirmer_time) AS confirmerTime,
              COALESCE(seg_done.remark, lr.remark) AS reportRemark,
              COALESCE(seg_done.extra_json, lr.extra_json) AS extraJson,
              src.source_batch_no AS sourceBatchNo,
              src.source_batch_no AS sourceProductionBatchNo,
              COALESCE(src.available_source_length, 0) AS availableSourceLength,
              COALESCE(src.confirmed_source_count, 0) AS confirmedSourceCount
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'PRESS_SLOT_REPORT'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'PRESS_SLOT_REPORT'
            )
            LEFT JOIN (
              SELECT
                plan_id,
                source_segment_batch_no AS source_batch_no,
                MAX(source_production_ref_no) AS source_production_batch_no,
                SUM(available_source_length) AS available_source_length,
                COUNT(1) AS confirmed_source_count
              FROM (
                SELECT
                  slice_src.plan_id,
                  slice_src.source_production_ref_no,
                  slice_src.source_segment_batch_no,
                  CASE
                    WHEN UPPER(COALESCE(NULLIF(slice_src.self_check, ''), 'OK')) != 'OK' THEN 0
                    WHEN used.source_slitting_slice_id IS NULL THEN 1
                    ELSE 0
                  END AS available_source_length
                FROM (
                  SELECT
                    id AS source_slitting_slice_id,
                    plan_id,
                    self_check,
                    COALESCE(NULLIF(slice_serial_no, ''), NULLIF(source_production_batch_no, ''), NULLIF(source_batch_no, '')) AS source_production_ref_no,
                    CASE
                      WHEN slice_serial_no REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(slice_serial_no, CHAR_LENGTH(slice_serial_no) - 4)
                      WHEN slice_serial_no REGEXP '[PQRS][0-9][0-9][0-9]$' THEN LEFT(slice_serial_no, CHAR_LENGTH(slice_serial_no) - 3)
                      WHEN source_production_batch_no REGEXP '[PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(source_production_batch_no, '-J', 1)
                      WHEN source_production_batch_no REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(source_production_batch_no, CHAR_LENGTH(source_production_batch_no) - 4)
                      WHEN source_production_batch_no REGEXP '[PQRS][0-9][0-9][0-9]$' THEN LEFT(source_production_batch_no, CHAR_LENGTH(source_production_batch_no) - 3)
                      WHEN source_production_batch_no REGEXP '[PQRS]$' THEN source_production_batch_no
                      WHEN source_batch_no REGEXP '[PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(source_batch_no, '-J', 1)
                      WHEN source_batch_no REGEXP '[PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(source_batch_no, CHAR_LENGTH(source_batch_no) - 4)
                      WHEN source_batch_no REGEXP '[PQRS][0-9][0-9][0-9]$' THEN LEFT(source_batch_no, CHAR_LENGTH(source_batch_no) - 3)
                      WHEN source_batch_no REGEXP '[PQRS]$' THEN source_batch_no
                      ELSE NULL
                    END AS source_segment_batch_no
                  FROM mes_sfc_slitting_slice_record
                  WHERE deleted = 0
                    AND scan_status = 'CONFIRMED'
                ) slice_src
                LEFT JOIN (
                  SELECT
                    source_slitting_slice_id
                  FROM mes_sfc_press_slot_report
                  WHERE deleted = 0
                    AND source_slitting_slice_id IS NOT NULL
                    AND (
                      report_status IN ('CONFIRMED', 'SUBMITTED')
                      OR UPPER(COALESCE(self_check, '')) NOT IN ('', 'OK')
                      OR NULLIF(defect_code, '') IS NOT NULL
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"reporttype"%changeover%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"coaflag"%true%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"coaflag"%1%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"coaflag"%y%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"visualinspectionresult"%ng%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"selfcheck"%ng%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"feedbackresult"%ng%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"feedbackresult"%abnormal%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"inspectionresult"%ng%'
                      OR LOWER(COALESCE(extra_json, '')) LIKE '%"inspectionresult"%abnormal%'
                    )
                  GROUP BY source_slitting_slice_id
                  UNION
            """ + HcPressSlotFirstInspectionSampleClaimMapper.OCCUPIED_SOURCE_SQL + """

                ) used ON used.source_slitting_slice_id = slice_src.source_slitting_slice_id
              ) slice_src
              WHERE source_segment_batch_no IS NOT NULL
                AND source_segment_batch_no != ''
              GROUP BY plan_id, source_segment_batch_no
            ) src ON src.plan_id = p.id
            LEFT JOIN mes_sfc_operation_report seg_done ON seg_done.id = (
              SELECT MAX(sd.id)
              FROM mes_sfc_operation_report sd
              WHERE sd.deleted = 0
                AND sd.plan_operation_id = o.id
                AND sd.source_menu_code = 'PRESS_SLOT_SEGMENT_COMPLETE'
                AND sd.batch_no = src.source_batch_no
            )
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND (o.op_code = 'OP-PRESS-SLOT' OR o.op_name LIKE '%压槽%')
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              AND src.source_batch_no IS NOT NULL
              AND src.source_batch_no != ''
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%'))
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="productionDate != null">
                AND COALESCE(seg_done.report_date, lr.report_date) = #{productionDate}
              </if>
              <if test="equipmentId != null">
                AND COALESCE(lr.equipment_id, o.equipment_id) = #{equipmentId}
              </if>
              <if test="equipmentCode != null and equipmentCode != ''">
                AND COALESCE(lr.equipment_code, o.equipment_code) = #{equipmentCode}
              </if>
              <if test="taskStatus == 'PENDING'">
                AND seg_done.id IS NULL
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND seg_done.id IS NULL
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND (seg_done.id IS NOT NULL OR o.operation_status = 'FINISHED')
              </if>
              <if test="taskStatus == 'UNFINISHED'">
                AND seg_done.id IS NULL
                AND o.operation_status NOT IN ('FINISHED', 'CANCELLED', 'CANCELED')
              </if>
              <if test="taskStatus == 'PAUSED'">
                AND seg_done.id IS NULL
                AND o.operation_status = 'PAUSED'
              </if>
              <if test="taskStatus == 'CANCELLED'">
                AND seg_done.id IS NULL
                AND o.operation_status IN ('CANCELLED', 'CANCELED')
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC, src.source_batch_no ASC
            </script>
            """)
    List<HcAdhesiveReportTaskRespVO> selectPressSlotTaskList(@Param("taskStatus") String taskStatus,
                                                             @Param("taskKeyword") String taskKeyword,
                                                             @Param("productKeyword") String productKeyword,
                                                             @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                             @Param("motherModelKeyword") String motherModelKeyword,
                                                             @Param("productionDate") LocalDate productionDate,
                                                             @Param("equipmentId") Long equipmentId,
                                                             @Param("equipmentCode") String equipmentCode);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.category_code AS categoryCode,
              p.category_name AS categoryName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              o.work_center_id AS workCenterId,
              COALESCE(lr.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              lr.end_time AS endTime,
              p.remark AS requirements,
              lr.recorder_name AS recorderName,
              lr.recorder_time AS recorderTime,
              lr.confirmer_name AS confirmerName,
              lr.confirmer_time AS confirmerTime,
              lr.remark AS reportRemark,
              lr.extra_json AS extraJson,
              src.source_batch_no AS sourceBatchNo,
              src.source_production_batch_no AS sourceProductionBatchNo,
              COALESCE(src.available_source_length, 0) AS availableSourceLength,
              COALESCE(src.confirmed_source_count, 0) AS confirmedSourceCount
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'ADHESIVE2_REPORT'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'ADHESIVE2_REPORT'
            )
            LEFT JOIN (
              SELECT
                plan_id,
                source_segment_batch_no AS source_batch_no,
                MAX(production_batch_no) AS source_production_batch_no,
                SUM(available_source_length) AS available_source_length,
                COUNT(1) AS confirmed_source_count
              FROM (
                SELECT
                  ps_src.plan_id,
                  ps_src.production_batch_no,
                  ps_src.source_segment_batch_no,
                  CASE
                    WHEN ps_src.source_ng_flag = 1 OR COALESCE(coa.coa_count, 0) > 0 THEN 0
                    ELSE GREATEST(ps_src.source_length - COALESCE(used.used_source_length, 0), 0)
                  END AS available_source_length
                FROM (
                  SELECT
                    id AS source_press_slot_report_id,
                    plan_id,
                    production_batch_no,
                    COALESCE(output_length, input_length, 1) AS source_length,
                    CASE
                      WHEN UPPER(COALESCE(self_check, '')) IN ('NG', 'ABNORMAL', 'FAILED', 'FAIL', 'N', '不合格', '异常') THEN 1
                      WHEN COALESCE(defect_code, '') != '' THEN 1
                      WHEN UPPER(COALESCE(extra_json, '')) LIKE '%QUALITY_ABNORMAL%' THEN 1
                      WHEN UPPER(COALESCE(extra_json, '')) LIKE '%SOURCE NG%' THEN 1
                      WHEN COALESCE(extra_json, '') LIKE '%工序NG%' THEN 1
                      ELSE 0
                    END AS source_ng_flag,
                    CASE
                      WHEN source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN source_batch_no
                      WHEN source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(source_production_batch_no, CHAR_LENGTH(source_production_batch_no) - 3)
                      WHEN source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(source_production_batch_no, CHAR_LENGTH(source_production_batch_no) - 4)
                      WHEN production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(production_batch_no, CHAR_LENGTH(production_batch_no) - 3)
                      WHEN production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(production_batch_no, CHAR_LENGTH(production_batch_no) - 4)
                      WHEN parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN parent_production_batch_no
                      ELSE COALESCE(NULLIF(parent_production_batch_no, ''), NULLIF(source_batch_no, ''), NULLIF(source_production_batch_no, ''), NULLIF(production_batch_no, ''))
                    END AS source_segment_batch_no
                  FROM mes_sfc_press_slot_report
                  WHERE deleted = 0
                    AND report_status IN ('CONFIRMED', 'SUBMITTED')
                ) ps_src
                LEFT JOIN (
                  SELECT
                    source_press_slot_report_id,
                    SUM(COALESCE(input_length, output_length, 1)) AS used_source_length
                  FROM mes_sfc_adhesive2_report
                  WHERE deleted = 0
                    AND (
                      report_status IN ('CONFIRMED', 'SUBMITTED')
                      OR UPPER(COALESCE(self_check, '')) IN ('NG', 'ABNORMAL', 'FAILED', 'FAIL', 'N', '不合格', '异常')
                      OR COALESCE(defect_code, '') != ''
                      OR COALESCE(quality_lock_reason, '') != ''
                      OR UPPER(COALESCE(product_quality_status, '')) IN ('QUALITY_ABNORMAL', 'ABNORMAL', 'NG')
                      OR UPPER(COALESCE(extra_json, '')) LIKE '%QUALITY_ABNORMAL%'
                    )
                  GROUP BY source_press_slot_report_id
                ) used ON used.source_press_slot_report_id = ps_src.source_press_slot_report_id
                LEFT JOIN (
                  SELECT
                    source_report_id,
                    COUNT(1) AS coa_count
                  FROM mes_qms_fai_order
                  WHERE deleted = 0
                    AND source_module = 'ADHESIVE2_REPORT'
                    AND source_report_id IS NOT NULL
                    AND source_report_no LIKE '%-COA-%'
                    AND COALESCE(status, '') != 'CANCELED'
                  GROUP BY source_report_id
                ) coa ON coa.source_report_id = ps_src.source_press_slot_report_id
              ) ps_src
              GROUP BY plan_id, source_segment_batch_no
            ) src ON src.plan_id = p.id
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND (o.op_code = 'OP-ADHESIVE2' OR o.op_name LIKE '%粘胶2%' OR o.op_name LIKE '%背胶%')
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              AND COALESCE(src.confirmed_source_count, 0) > 0
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%'))
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="productionDate != null">
                AND lr.report_date = #{productionDate}
              </if>
              <if test="equipmentId != null">
                AND COALESCE(lr.equipment_id, o.equipment_id) = #{equipmentId}
              </if>
              <if test="equipmentCode != null and equipmentCode != ''">
                AND COALESCE(lr.equipment_code, o.equipment_code) = #{equipmentCode}
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
              <if test="taskStatus == 'PAUSED'">
                AND o.operation_status = 'PAUSED'
              </if>
              <if test="taskStatus == 'CANCELLED'">
                AND o.operation_status IN ('CANCELLED', 'CANCELED')
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC, src.source_batch_no ASC
            </script>
            """)
    List<HcAdhesiveReportTaskRespVO> selectAdhesive2TaskList(@Param("taskStatus") String taskStatus,
                                                             @Param("taskKeyword") String taskKeyword,
                                                             @Param("productKeyword") String productKeyword,
                                                             @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                             @Param("motherModelKeyword") String motherModelKeyword,
                                                             @Param("productionDate") LocalDate productionDate,
                                                             @Param("equipmentId") Long equipmentId,
                                                             @Param("equipmentCode") String equipmentCode);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0'), '-', COALESCE(src.source_batch_no, 'NO-SOURCE')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.category_code AS categoryCode,
              p.category_name AS categoryName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              COALESCE(src.source_batch_no, lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(src.source_batch_no, lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(src.source_batch_no, lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              o.work_center_id AS workCenterId,
              o.work_center_name AS workCenterName,
              COALESCE(lr.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              lr.end_time AS endTime,
              p.remark AS requirements,
              lr.recorder_name AS recorderName,
              lr.recorder_time AS recorderTime,
              lr.confirmer_name AS confirmerName,
              lr.confirmer_time AS confirmerTime,
              lr.remark AS reportRemark,
              lr.extra_json AS extraJson,
              src.source_batch_no AS sourceBatchNo,
              src.source_production_batch_no AS sourceProductionBatchNo,
              COALESCE(src.available_source_length, 0) AS availableSourceLength,
              COALESCE(src.confirmed_source_count, 0) AS confirmedSourceCount
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'CUT_ROUND_REPORT'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'CUT_ROUND_REPORT'
            )
            LEFT JOIN (
              SELECT
                plan_id,
                source_segment_batch_no AS source_batch_no,
                MAX(production_batch_no) AS source_production_batch_no,
                COUNT(1) AS confirmed_source_count,
                SUM(COALESCE(output_length, input_length, 1)) AS available_source_length
              FROM (
                SELECT
                  plan_id,
                  production_batch_no,
                  output_length,
                  input_length,
                  CASE
                    WHEN source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN source_batch_no
                    WHEN source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN source_production_batch_no
                    WHEN source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(source_production_batch_no, CHAR_LENGTH(source_production_batch_no) - 3)
                    WHEN source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(source_production_batch_no, CHAR_LENGTH(source_production_batch_no) - 4)
                    WHEN production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(production_batch_no, CHAR_LENGTH(production_batch_no) - 3)
                    WHEN production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(production_batch_no, CHAR_LENGTH(production_batch_no) - 4)
                    WHEN parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN parent_production_batch_no
                    ELSE COALESCE(NULLIF(parent_production_batch_no, ''), NULLIF(source_batch_no, ''), NULLIF(source_production_batch_no, ''), NULLIF(production_batch_no, ''))
                  END AS source_segment_batch_no
                FROM mes_sfc_adhesive2_report
                WHERE deleted = 0
                  AND report_status IN ('CONFIRMED', 'SUBMITTED')
                  AND NOT (
                    UPPER(COALESCE(self_check, '')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                    OR UPPER(COALESCE(self_check, '')) LIKE 'NG%'
                    OR UPPER(COALESCE(self_check, '')) LIKE '%=_NG%' ESCAPE '='
                    OR UPPER(COALESCE(self_check, '')) LIKE '%NG=_%' ESCAPE '='
                    OR UPPER(COALESCE(self_check, '')) LIKE '% NG%'
                    OR UPPER(COALESCE(self_check, '')) LIKE '%NG %'
                    OR TRIM(COALESCE(defect_code, '')) != ''
                    OR TRIM(COALESCE(quality_lock_reason, '')) != ''
                    OR UPPER(COALESCE(product_quality_status, '')) IN ('QUALITY_ABNORMAL', 'ABNORMAL', 'NG')
                  )
              ) adhesive2_src
              WHERE source_segment_batch_no IS NOT NULL
                AND source_segment_batch_no != ''
              GROUP BY plan_id, source_segment_batch_no
            ) src ON src.plan_id = p.id
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND (o.op_code = 'OP-CUT-ROUND' OR o.op_code = 'OP-CUT' OR o.op_name LIKE '%裁切%' OR o.op_name LIKE '%裁圆%')
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              AND COALESCE(src.confirmed_source_count, 0) > 0
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%'))
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="productionDate != null">
                AND lr.report_date = #{productionDate}
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
              <if test="taskStatus == 'PAUSED'">
                AND o.operation_status = 'PAUSED'
              </if>
              <if test="taskStatus == 'CANCELLED'">
                AND o.operation_status IN ('CANCELLED', 'CANCELED')
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC, src.source_batch_no ASC
            </script>
            """)
    List<HcAdhesiveReportTaskRespVO> selectCutRoundTaskList(@Param("taskStatus") String taskStatus,
                                                            @Param("taskKeyword") String taskKeyword,
                                                            @Param("productKeyword") String productKeyword,
                                                            @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                            @Param("motherModelKeyword") String motherModelKeyword,
                                                            @Param("productionDate") LocalDate productionDate);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              p.size_name AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              COALESCE(pr.production_batch_no, lr.production_batch_no, o.production_batch_no, p.production_batch_no,
                       pr.batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(pr.production_batch_no, lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              o.work_center_id AS workCenterId,
              o.work_center_name AS workCenterName,
              COALESCE(lr.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name, '片') AS uom,
              COALESCE(r.good_qty, pr.outer_piece_count, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              lr.end_time AS endTime,
              p.remark AS requirements,
              COALESCE(pr.recorder_name, lr.recorder_name) AS recorderName,
              COALESCE(pr.recorder_time, lr.recorder_time) AS recorderTime,
              COALESCE(pr.confirmer_name, lr.confirmer_name) AS confirmerName,
              COALESCE(pr.confirmer_time, lr.confirmer_time) AS confirmerTime,
              COALESCE(pr.remark, lr.remark) AS reportRemark,
              COALESCE(pr.extra_json, lr.extra_json) AS extraJson,
              src.source_batch_no AS sourceBatchNo,
              src.source_production_batch_no AS sourceProductionBatchNo,
              COALESCE(src.available_source_length, 0) AS availableSourceLength,
              COALESCE(src.confirmed_source_count, 0) AS confirmedSourceCount
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'PACKAGING_REPORT'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'PACKAGING_REPORT'
            )
            LEFT JOIN mes_sfc_pack_report pr ON pr.id = (
              SELECT MAX(pr2.id)
              FROM mes_sfc_pack_report pr2
              WHERE pr2.deleted = 0
                AND pr2.plan_operation_id = o.id
            )
            LEFT JOIN (
              SELECT
                plan_id,
                MAX(parent_production_batch_no) AS source_batch_no,
                MAX(production_batch_no) AS source_production_batch_no,
                COUNT(1) AS confirmed_source_count,
                COUNT(1) AS available_source_length
              FROM mes_sfc_cut_round_report
              WHERE deleted = 0
                AND report_status IN ('CONFIRMED', 'SUBMITTED')
              GROUP BY plan_id
            ) src ON src.plan_id = p.id
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND (o.op_code = 'OP-PACKAGING' OR o.op_code = 'OP-PACK' OR o.op_name LIKE '%包装%'
                   OR o.op_name LIKE '%内包%' OR o.op_name LIKE '%外包%')
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED')
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%'))
              </if>
              <if test="productKeyword != null and productKeyword != ''">
                AND (p.material_code LIKE CONCAT('%', #{productKeyword}, '%')
                  OR p.material_name LIKE CONCAT('%', #{productKeyword}, '%'))
              </if>
              <if test="motherMaterialKeyword != null and motherMaterialKeyword != ''">
                AND (p.mother_material_code LIKE CONCAT('%', #{motherMaterialKeyword}, '%')
                  OR p.mother_material_name LIKE CONCAT('%', #{motherMaterialKeyword}, '%'))
              </if>
              <if test="motherModelKeyword != null and motherModelKeyword != ''">
                AND (p.mother_model_code LIKE CONCAT('%', #{motherModelKeyword}, '%')
                  OR p.mother_model_name LIKE CONCAT('%', #{motherModelKeyword}, '%'))
              </if>
              <if test="productionDate != null">
                AND lr.report_date = #{productionDate}
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC
            </script>
            """)
    List<HcPackagingTaskRespVO> selectPackagingTaskList(@Param("taskStatus") String taskStatus,
                                                        @Param("taskKeyword") String taskKeyword,
                                                        @Param("productKeyword") String productKeyword,
                                                        @Param("motherMaterialKeyword") String motherMaterialKeyword,
                                                        @Param("motherModelKeyword") String motherModelKeyword,
                                                        @Param("productionDate") LocalDate productionDate);

    @Select("""
            <script>
            SELECT
              CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) AS id,
              p.id AS planId,
              o.id AS planOperationId,
              p.plan_no AS planNo,
              p.sales_order_erp_no AS erpOrderNo,
              p.source_type AS planType,
              p.material_name AS product,
              p.material_code AS materialCode,
              p.material_name AS productName,
              p.mother_material_code AS motherMaterialCode,
              p.mother_material_name AS motherMaterialName,
              COALESCE(NULLIF(p.size_name, ''), p.size_spec) AS spec,
              COALESCE(p.model_code, p.model_name) AS modelCode,
              COALESCE(p.mother_model_code, p.mother_model_name) AS motherModelCode,
              p.production_start_date AS productionStartDate,
              p.production_end_date AS productionEndDate,
              lr.report_date AS productionDate,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no, lr.batch_no, o.batch_no, p.batch_no) AS batchNo,
              COALESCE(lr.production_batch_no, o.production_batch_no, p.production_batch_no) AS productionBatchNo,
              COALESCE(lr.parent_production_batch_no, o.parent_production_batch_no, p.parent_production_batch_no) AS parentProductionBatchNo,
              o.op_name AS process,
              COALESCE(cut_op.work_center_id, o.work_center_id) AS workCenterId,
              COALESCE(cut_op.work_center_name, o.work_center_name) AS workCenterName,
              COALESCE(lr.equipment_id, cut_op.equipment_id, o.equipment_id) AS equipmentId,
              COALESCE(lr.equipment_code, cut_op.equipment_code, o.equipment_code) AS equipmentCode,
              COALESCE(lr.equipment_name, cut_op.equipment_name, o.equipment_name) AS equipmentName,
              o.required_qty AS planQty,
              COALESCE(o.uom, o.unit_code, o.unit_name) AS uom,
              COALESCE(r.good_qty, 0) AS goodQty,
              COALESCE(r.scrap_qty, 0) AS scrapQty,
              CASE
                WHEN o.operation_status = 'FINISHED' THEN 'COMPLETED'
                WHEN o.operation_status = 'RUNNING' THEN 'IN_PROGRESS'
                WHEN o.operation_status = 'RELEASED' THEN 'PENDING'
                WHEN o.operation_status = 'PAUSED' THEN 'PAUSED'
                WHEN o.operation_status IN ('CANCELLED', 'CANCELED') THEN 'CANCELLED'
                ELSE 'PENDING'
              END AS status,
              COALESCE(lr.start_time, r.first_start_time) AS startTime,
              lr.end_time AS endTime,
              p.remark AS requirements,
              lr.recorder_name AS recorderName,
              lr.recorder_time AS recorderTime,
              lr.confirmer_name AS confirmerName,
              lr.confirmer_time AS confirmerTime,
              lr.remark AS reportRemark,
              lr.extra_json AS extraJson,
              COALESCE(src.source_batch_no, lock_src.source_batch_no) AS sourceBatchNo,
              COALESCE(src.source_production_batch_no, lock_src.source_production_batch_no) AS sourceProductionBatchNo,
              COALESCE(src.available_source_length, lock_src.available_source_length, 0) AS availableSourceLength,
              COALESCE(src.confirmed_source_count, lock_src.confirmed_source_count, 0) AS confirmedSourceCount
            FROM mes_pp_plan_operation o
            INNER JOIN mes_pp_plan_order p ON p.id = o.plan_id AND p.deleted = 0
            LEFT JOIN (
              SELECT
                plan_operation_id,
                SUM(good_qty) AS good_qty,
                SUM(scrap_qty) AS scrap_qty,
                MIN(start_time) AS first_start_time
              FROM mes_sfc_operation_report
              WHERE deleted = 0
                AND source_menu_code = 'SLITTING_REPORT'
              GROUP BY plan_operation_id
            ) r ON r.plan_operation_id = o.id
            LEFT JOIN mes_sfc_operation_report lr ON lr.id = (
              SELECT MAX(r2.id)
              FROM mes_sfc_operation_report r2
              WHERE r2.deleted = 0
                AND r2.plan_operation_id = o.id
                AND r2.source_menu_code = 'SLITTING_REPORT'
            )
            LEFT JOIN mes_pp_plan_operation cut_op ON cut_op.id = (
              SELECT co.id
              FROM mes_pp_plan_operation co
              WHERE co.deleted = 0
                AND co.plan_id = p.id
                AND (co.op_code = 'OP-CUT' OR co.op_name LIKE '%裁切%')
              ORDER BY co.op_seq ASC
              LIMIT 1
            )
            LEFT JOIN (
              SELECT
                a.plan_id,
                so.id AS plan_operation_id,
                MAX(a.source_batch_no) AS source_batch_no,
                MAX(a.production_batch_no) AS source_production_batch_no,
                SUM(COALESCE(
                  a.slitting_remaining_length,
                  GREATEST(COALESCE(a.output_length, a.input_length, 0) - COALESCE(used.used_source_length, 0), 0)
                )) AS available_source_length,
                COUNT(1) AS confirmed_source_count
              FROM mes_sfc_adhesive_report a
              INNER JOIN mes_pp_plan_operation so
                ON so.plan_id = a.plan_id
               AND so.deleted = 0
               AND (so.op_code = 'OP-SLIT' OR so.op_name LIKE '%分切%')
              LEFT JOIN (
                SELECT
                  plan_operation_id,
                  source_adhesive_report_id,
                  SUM(GREATEST(COALESCE(
                    slice_length,
                    CASE
                      WHEN end_position IS NOT NULL AND start_position IS NOT NULL AND end_position > start_position
                        THEN end_position - start_position
                      ELSE 0
                    END
                  ), 0)) AS used_source_length
                FROM mes_sfc_slitting_slice_record
                WHERE deleted = 0
                  AND scan_status = 'CONFIRMED'
                GROUP BY plan_operation_id, source_adhesive_report_id
              ) used ON used.plan_operation_id = so.id
                    AND used.source_adhesive_report_id = a.id
              WHERE a.deleted = 0
                AND a.report_status IN ('CONFIRMED', 'SUBMITTED')
              GROUP BY a.plan_id, so.id
            ) src ON src.plan_id = p.id
                  AND src.plan_operation_id = o.id
            LEFT JOIN (
              SELECT
                il.plan_operation_id,
                MAX(COALESCE(NULLIF(ar.source_batch_no, ''), NULLIF(il.source_batch_no, ''), NULLIF(il.batch_no, ''), NULLIF(il.lot_no, ''))) AS source_batch_no,
                MAX(COALESCE(NULLIF(ar.production_batch_no, ''), NULLIF(ar.source_production_batch_no, ''), NULLIF(il.source_batch_no, ''), NULLIF(il.batch_no, ''), NULLIF(il.lot_no, ''))) AS source_production_batch_no,
                SUM(
                  COALESCE(il.lock_qty, 0)
                  - COALESCE(il.consumed_qty, 0)
                  - COALESCE(il.released_qty, 0)
                ) AS available_source_length,
                COUNT(1) AS confirmed_source_count
              FROM mes_pp_plan_inv_lock il
              LEFT JOIN mes_sfc_adhesive_report ar
                ON il.source_table = 'mes_sfc_adhesive_report'
               AND ar.id = il.source_id
               AND ar.deleted = 0
              WHERE il.deleted = 0
                AND (il.lock_type = 'WIP' OR il.stock_type = 'WIP')
                AND il.source_table = 'mes_sfc_adhesive_report'
                AND COALESCE(il.lock_status, '') NOT IN ('CONSUMED', 'CANCELLED', 'RELEASED')
                AND (
                  COALESCE(il.lock_qty, 0)
                  - COALESCE(il.consumed_qty, 0)
                  - COALESCE(il.released_qty, 0)
                ) > 0
              GROUP BY il.plan_operation_id
            ) lock_src ON lock_src.plan_operation_id = o.id
            WHERE o.deleted = 0
              AND p.plan_status = 'RELEASED'
              AND (o.op_code = 'OP-SLIT' OR o.op_name LIKE '%分切%')
              AND o.operation_status IN ('RELEASED', 'RUNNING', 'FINISHED', 'PAUSED', 'CANCELLED', 'CANCELED')
              <if test="taskKeyword != null and taskKeyword != ''">
                AND (p.plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR CONCAT(p.plan_no, '-', LPAD(o.op_seq, 2, '0')) LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR src.source_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lock_src.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR lock_src.source_production_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                  OR EXISTS (
                    SELECT 1
                    FROM mes_pp_plan_inv_lock il
                    WHERE il.deleted = 0
                      AND il.plan_operation_id = o.id
                      AND (il.lock_type = 'WIP' OR il.stock_type = 'WIP')
                      AND il.source_table = 'mes_sfc_adhesive_report'
                      AND COALESCE(il.lock_status, '') NOT IN ('CONSUMED', 'CANCELLED', 'RELEASED')
                      AND (
                        COALESCE(il.lock_qty, 0)
                        - COALESCE(il.consumed_qty, 0)
                        - COALESCE(il.released_qty, 0)
                      ) > 0
                      AND (il.source_plan_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.source_batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.batch_no LIKE CONCAT('%', #{taskKeyword}, '%')
                        OR il.lot_no LIKE CONCAT('%', #{taskKeyword}, '%'))
                  ))
              </if>
              <if test="taskStatus == 'PENDING'">
                AND o.operation_status = 'RELEASED'
              </if>
              <if test="taskStatus == 'IN_PROGRESS'">
                AND o.operation_status = 'RUNNING'
              </if>
              <if test="taskStatus == 'COMPLETED'">
                AND o.operation_status = 'FINISHED'
              </if>
              <if test="taskStatus == 'PAUSED'">
                AND o.operation_status = 'PAUSED'
              </if>
              <if test="taskStatus == 'CANCELLED'">
                AND o.operation_status IN ('CANCELLED', 'CANCELED')
              </if>
            ORDER BY p.plan_date DESC, p.id DESC, o.op_seq ASC
            </script>
            """)
    List<HcSlittingReportTaskRespVO> selectSlittingTaskList(@Param("taskStatus") String taskStatus,
                                                            @Param("taskKeyword") String taskKeyword);

    @Select("""
            <script>
            SELECT
              q.planId AS planId,
              q.segmentBatchNo AS segmentBatchNo,
              q.stageCode AS stageCode,
              MAX(q.stageName) AS stageName,
              GROUP_CONCAT(DISTINCT NULLIF(q.sourceBatchNos, '') ORDER BY q.sourceBatchNos SEPARATOR ',') AS sourceBatchNos,
              GROUP_CONCAT(DISTINCT NULLIF(q.outputBatchNos, '') ORDER BY q.outputBatchNos SEPARATOR ',') AS outputBatchNos,
              SUM(COALESCE(q.inputQty, 0)) AS inputQty,
              SUM(COALESCE(q.doneQty, 0)) AS doneQty,
              SUM(COALESCE(q.reportQty, 0)) AS reportQty,
              SUM(COALESCE(q.defectQty, 0)) AS defectQty,
              SUM(COALESCE(q.confirmedQty, 0)) AS confirmedQty,
              SUM(COALESCE(q.lengthQty, 0)) AS lengthQty,
              MIN(q.startPosition) AS startPosition,
              SUM(q.processLength) AS processLength,
              MAX(q.lastReportTime) AS lastReportTime,
              MAX(q.reportUnit) AS reportUnit,
              GROUP_CONCAT(DISTINCT NULLIF(q.remark, '') SEPARATOR '；') AS remark
            FROM (
              SELECT
                r.plan_id AS planId,
                CAST(NULL AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS segmentBatchNo,
                _utf8mb4'FORMULA' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'配料' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''), NULLIF(r.batch_no, '')) SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, ''), NULLIF(r.feed_batch_no, '')) SEPARATOR ',') AS outputBatchNos,
                SUM(COALESCE(r.feed_qty, 0)) AS inputQty,
                SUM(COALESCE(r.good_qty, 0)) AS doneQty,
                SUM(COALESCE(r.good_qty, 0)) AS reportQty,
                SUM(COALESCE(r.scrap_qty, 0)) AS defectQty,
                CAST(NULL AS DECIMAL(18,3)) AS confirmedQty,
                CAST(NULL AS DECIMAL(18,3)) AS lengthQty,
                CAST(NULL AS DECIMAL(18,3)) AS startPosition,
                CAST(NULL AS DECIMAL(18,3)) AS processLength,
                NULLIF(MAX(GREATEST(
                  IF(r.end_time >= '1972-01-01 00:00:00', r.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.confirmer_time >= '1972-01-01 00:00:00', r.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.recorder_time >= '1972-01-01 00:00:00', r.recorder_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.update_time >= '1972-01-01 00:00:00', r.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.create_time >= '1972-01-01 00:00:00', r.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                )), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                COALESCE(MAX(NULLIF(r.report_uom, '')), _utf8mb4'kg' COLLATE utf8mb4_unicode_ci) AS reportUnit,
                GROUP_CONCAT(DISTINCT NULLIF(r.remark, '') SEPARATOR '；') AS remark
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0
                AND r.source_menu_code = 'FORMULA_REPORT'
                AND r.report_type = 'END'
                AND r.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY r.plan_id
              UNION ALL
              SELECT
                r.plan_id AS planId,
                CAST(NULL AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS segmentBatchNo,
                _utf8mb4'WET' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'湿法' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(r.parent_production_batch_no, ''), NULLIF(r.parent_batch_no, ''), NULLIF(r.batch_no, '')) SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(r.production_batch_no, ''), NULLIF(r.batch_no, '')) SEPARATOR ',') AS outputBatchNos,
                SUM(COALESCE(r.feed_qty, 0)) AS inputQty,
                SUM(COALESCE(r.good_qty, 0)) AS doneQty,
                SUM(COALESCE(r.good_qty, 0)) AS reportQty,
                SUM(COALESCE(r.scrap_qty, 0)) AS defectQty,
                CAST(NULL AS DECIMAL(18,3)) AS confirmedQty,
                SUM(COALESCE(r.good_qty, 0)) AS lengthQty,
                CAST(NULL AS DECIMAL(18,3)) AS startPosition,
                CAST(NULL AS DECIMAL(18,3)) AS processLength,
                NULLIF(MAX(GREATEST(
                  IF(r.end_time >= '1972-01-01 00:00:00', r.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.confirmer_time >= '1972-01-01 00:00:00', r.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.recorder_time >= '1972-01-01 00:00:00', r.recorder_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.update_time >= '1972-01-01 00:00:00', r.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(r.create_time >= '1972-01-01 00:00:00', r.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                )), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                COALESCE(MAX(NULLIF(r.report_uom, '')), _utf8mb4'm' COLLATE utf8mb4_unicode_ci) AS reportUnit,
                GROUP_CONCAT(DISTINCT NULLIF(r.remark, '') SEPARATOR '；') AS remark
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0
                AND r.source_menu_code = 'WET_REPORT'
                AND r.report_type = 'END'
                AND r.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY r.plan_id
              UNION ALL
              SELECT
                g.planId,
                g.segmentBatchNo,
                _utf8mb4'GRINDING' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'磨皮' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT NULLIF(g.sourceBatchNo, '') SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT NULLIF(g.outputBatchNo, '') SEPARATOR ',') AS outputBatchNos,
                SUM(COALESCE(g.inputQty, 0)) AS inputQty,
                SUM(COALESCE(g.doneQty, 0)) AS doneQty,
                SUM(COALESCE(g.reportQty, 0)) AS reportQty,
                SUM(COALESCE(g.defectQty, 0)) AS defectQty,
                CAST(NULL AS DECIMAL(18,3)) AS confirmedQty,
                SUM(COALESCE(g.doneQty, 0)) AS lengthQty,
                MIN(g.startPosition) AS startPosition,
                SUM(g.processLength) AS processLength,
                MAX(g.lastReportTime) AS lastReportTime,
                _utf8mb4'm' COLLATE utf8mb4_unicode_ci AS reportUnit,
                GROUP_CONCAT(DISTINCT NULLIF(g.remark, '') SEPARATOR '；') AS remark
              FROM (
                SELECT
                  f.plan_id AS planId,
                  CASE WHEN fa.id IS NULL
                       THEN CAST(NULL AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
                       ELSE COALESCE(NULLIF(fa.production_batch_no, ''), NULLIF(fa.mother_batch_no, ''))
                  END AS segmentBatchNo,
                  COALESCE(NULLIF(fa.mother_batch_no, ''), NULLIF(f.source_production_batch_no, ''),
                           NULLIF(f.mother_batch_no, '')) AS sourceBatchNo,
                  CASE WHEN fa.id IS NULL
                       THEN CAST(NULL AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci
                       ELSE NULLIF(fa.production_batch_no, '')
                  END AS outputBatchNo,
                  COALESCE(f.output_length, fa.confirmed_length, 0) AS inputQty,
                  CAST(0 AS DECIMAL(18,3)) AS doneQty,
                  CAST(0 AS DECIMAL(18,3)) AS reportQty,
                  COALESCE(f.loss_length, 0) AS defectQty,
                  fa.start_position AS startPosition,
                  CAST(NULL AS DECIMAL(18,3)) AS processLength,
                  NULLIF(GREATEST(
                    IF(COALESCE(fa.end_time, f.end_time) >= '1972-01-01 00:00:00', COALESCE(fa.end_time, f.end_time), CAST('1972-01-01 00:00:00' AS DATETIME)),
                    IF(COALESCE(fa.start_time, f.start_time) >= '1972-01-01 00:00:00', COALESCE(fa.start_time, f.start_time), CAST('1972-01-01 00:00:00' AS DATETIME)),
                    IF(COALESCE(fa.update_time, f.update_time) >= '1972-01-01 00:00:00', COALESCE(fa.update_time, f.update_time), CAST('1972-01-01 00:00:00' AS DATETIME)),
                    IF(COALESCE(fa.create_time, f.create_time) >= '1972-01-01 00:00:00', COALESCE(fa.create_time, f.create_time), CAST('1972-01-01 00:00:00' AS DATETIME))
                  ), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                  CASE WHEN fa.id IS NULL THEN NULLIF(f.defect_code, '')
                       ELSE CONCAT('一磨加工单元：', COALESCE(NULLIF(fa.segment_mark, ''), 'NONE'),
                                   CASE WHEN NULLIF(f.defect_code, '') IS NULL THEN ''
                                        ELSE CONCAT('；', f.defect_code) END) END AS remark
                FROM mes_sfc_grinding_first_detail f
                LEFT JOIN mes_sfc_grinding_first_allocation_detail fa
                  ON fa.first_detail_id = f.id
                 AND fa.tenant_id = f.tenant_id
                 AND fa.deleted = 0
                 AND COALESCE(fa.detail_status, 'ACTIVE') != 'VOID'
                WHERE f.deleted = 0
                  AND f.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
                UNION ALL
                SELECT
                  s.plan_id AS planId,
                  CASE
                    WHEN s.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.production_batch_no
                    WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                    WHEN s.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.parent_production_batch_no
                    ELSE COALESCE(NULLIF(s.production_batch_no, ''), NULLIF(s.source_production_batch_no, ''), NULLIF(s.parent_production_batch_no, ''), NULLIF(s.mother_batch_no, ''))
                  END AS segmentBatchNo,
                  COALESCE(NULLIF(s.parent_production_batch_no, ''), NULLIF(s.source_production_batch_no, ''), NULLIF(s.mother_batch_no, '')) AS sourceBatchNo,
                  NULLIF(s.production_batch_no, '') AS outputBatchNo,
                  CAST(0 AS DECIMAL(18,3)) AS inputQty,
                  CASE
                    WHEN UPPER(COALESCE(NULLIF(TRIM(s.confirm_status), ''), NULLIF(TRIM(s.detail_status), ''), '')) = 'CONFIRMED' OR s.confirm_time IS NOT NULL
                    THEN COALESCE(s.process_length, s.output_length, 0)
                    ELSE CAST(0 AS DECIMAL(18,3))
                  END AS doneQty,
                  COALESCE(s.process_length, s.output_length, 0) AS reportQty,
                  COALESCE(s.loss_length, 0) AS defectQty,
                  s.start_position AS startPosition,
                  COALESCE(s.process_length, s.output_length, 0) AS processLength,
                  NULLIF(GREATEST(
                    IF(s.end_time >= '1972-01-01 00:00:00', s.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                    IF(s.confirm_time >= '1972-01-01 00:00:00', s.confirm_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                    IF(s.start_time >= '1972-01-01 00:00:00', s.start_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                    IF(s.update_time >= '1972-01-01 00:00:00', s.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                    IF(s.create_time >= '1972-01-01 00:00:00', s.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                  ), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                  NULLIF(s.defect_code, '') AS remark
                FROM mes_sfc_grinding_second_detail s
                WHERE s.deleted = 0
                  AND s.plan_id IN
                  <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                    #{planId}
                  </foreach>
              ) g
              GROUP BY g.planId, g.segmentBatchNo
              UNION ALL
              SELECT
                a.plan_id AS planId,
                CASE
                  WHEN a.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.source_batch_no
                  WHEN a.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.source_production_batch_no
                  WHEN a.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a.source_production_batch_no, '-J', 1)
                  WHEN a.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(a.source_production_batch_no, CHAR_LENGTH(a.source_production_batch_no) - 3)
                  WHEN a.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(a.source_production_batch_no, CHAR_LENGTH(a.source_production_batch_no) - 4)
                  WHEN a.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.production_batch_no
                  WHEN a.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a.production_batch_no, '-J', 1)
                  WHEN a.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a.parent_production_batch_no
                  ELSE COALESCE(NULLIF(a.source_production_batch_no, ''), NULLIF(a.source_batch_no, ''), NULLIF(a.parent_production_batch_no, ''), NULLIF(a.production_batch_no, ''))
                END AS segmentBatchNo,
                _utf8mb4'ADHESIVE1' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'粘胶1' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(a.source_production_batch_no, ''), NULLIF(a.source_batch_no, ''), NULLIF(a.parent_production_batch_no, '')) SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT CONCAT(
                  COALESCE(
                    NULLIF(a.source_production_batch_no, ''),
                    NULLIF(a.source_batch_no, ''),
                    NULLIF(a.parent_production_batch_no, ''),
                    CASE
                      WHEN a.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a.production_batch_no, '-J', 1)
                      ELSE NULLIF(a.production_batch_no, '')
                    END
                  ),
                  ' ',
                  ROUND(COALESCE(a.start_position, 0), 3),
                  '-',
                  ROUND(COALESCE(a.end_position, COALESCE(a.start_position, 0) + COALESCE(a.output_length, a.input_length, 0)), 3),
                  'm'
                ) SEPARATOR ',') AS outputBatchNos,
                SUM(COALESCE(a.input_length, 0)) AS inputQty,
                SUM(CASE
                  WHEN UPPER(COALESCE(NULLIF(TRIM(a.report_status), ''), '')) = 'SUBMITTED'
                  THEN COALESCE(a.output_length, 0)
                  ELSE CAST(0 AS DECIMAL(18,3))
                END) AS doneQty,
                SUM(COALESCE(a.output_length, 0)) AS reportQty,
                SUM(COALESCE(a.loss_length, 0)) AS defectQty,
                SUM(CASE
                  WHEN UPPER(COALESCE(NULLIF(TRIM(a.report_status), ''), '')) IN ('CONFIRMED', 'SUBMITTED')
                  THEN COALESCE(a.output_length, 0)
                  ELSE CAST(0 AS DECIMAL(18,3))
                END) AS confirmedQty,
                SUM(CASE
                  WHEN UPPER(COALESCE(NULLIF(TRIM(a.report_status), ''), '')) = 'SUBMITTED'
                  THEN COALESCE(a.output_length, 0)
                  ELSE CAST(0 AS DECIMAL(18,3))
                END) AS lengthQty,
                CAST(NULL AS DECIMAL(18,3)) AS startPosition,
                CAST(NULL AS DECIMAL(18,3)) AS processLength,
                NULLIF(MAX(GREATEST(
                  IF(a.end_time >= '1972-01-01 00:00:00', a.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a.confirmer_time >= '1972-01-01 00:00:00', a.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a.recorder_time >= '1972-01-01 00:00:00', a.recorder_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a.update_time >= '1972-01-01 00:00:00', a.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a.create_time >= '1972-01-01 00:00:00', a.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                )), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                _utf8mb4'm' COLLATE utf8mb4_unicode_ci AS reportUnit,
                GROUP_CONCAT(DISTINCT NULLIF(a.remark, '') SEPARATOR '；') AS remark
              FROM mes_sfc_adhesive_report a
              WHERE a.deleted = 0
                AND COALESCE(a.report_status, 'SUBMITTED') IN ('CONFIRMED', 'SUBMITTED')
                AND a.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY a.plan_id, segmentBatchNo
              UNION ALL
              SELECT
                s.plan_id AS planId,
                CASE
                  WHEN s.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_batch_no
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(s.source_production_batch_no, '-J', 1)
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.source_production_batch_no, CHAR_LENGTH(s.source_production_batch_no) - 3)
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.source_production_batch_no, CHAR_LENGTH(s.source_production_batch_no) - 4)
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 3)
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 4)
                  ELSE COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, ''), NULLIF(s.slice_serial_no, ''))
                END AS segmentBatchNo,
                _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'分切' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, '')) SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT NULLIF(s.slice_serial_no, '') SEPARATOR ',') AS outputBatchNos,
                CAST(NULL AS DECIMAL(18,3)) AS inputQty,
                CAST(SUM(CASE WHEN s.scan_status = 'CONFIRMED'
                  AND UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  AND UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) NOT LIKE 'NG%'
                  AND UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) NOT LIKE '%不合格%'
                  AND UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) NOT LIKE '%异常%'
                  AND LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) NOT LIKE '%"result":"ng"%'
                  AND LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                  THEN 1 ELSE 0 END) AS DECIMAL(18,3)) AS doneQty,
                CAST(COUNT(1) AS DECIMAL(18,3)) AS reportQty,
                CAST(0 AS DECIMAL(18,3)) AS defectQty,
                SUM(CASE WHEN s.scan_status = 'CONFIRMED' THEN 1 ELSE 0 END) AS confirmedQty,
                SUM(COALESCE(s.slice_length, 0)) AS lengthQty,
                CAST(NULL AS DECIMAL(18,3)) AS startPosition,
                CAST(NULL AS DECIMAL(18,3)) AS processLength,
                NULLIF(MAX(GREATEST(
                  IF(s.scan_time >= '1972-01-01 00:00:00', s.scan_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(s.update_time >= '1972-01-01 00:00:00', s.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(s.create_time >= '1972-01-01 00:00:00', s.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                )), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                _utf8mb4'片' COLLATE utf8mb4_unicode_ci AS reportUnit,
                CONVERT(CONCAT('已确认', SUM(CASE WHEN s.scan_status = 'CONFIRMED' THEN 1 ELSE 0 END),
                       '片；已分切长度', ROUND(SUM(COALESCE(s.slice_length, 0)), 3), 'm') USING utf8mb4)
                       COLLATE utf8mb4_unicode_ci AS remark
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY s.plan_id, segmentBatchNo
              UNION ALL
              SELECT
                p.plan_id AS planId,
                CASE
                  WHEN p.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_batch_no
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_production_batch_no
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(p.source_production_batch_no, '-J', 1)
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(p.source_production_batch_no, CHAR_LENGTH(p.source_production_batch_no) - 3)
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(p.source_production_batch_no, CHAR_LENGTH(p.source_production_batch_no) - 4)
                  WHEN p.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(p.production_batch_no, CHAR_LENGTH(p.production_batch_no) - 3)
                  WHEN p.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(p.production_batch_no, CHAR_LENGTH(p.production_batch_no) - 4)
                  WHEN p.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.parent_production_batch_no
                  ELSE COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''))
                END AS segmentBatchNo,
                _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'压槽' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, '')) SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, '')) SEPARATOR ',') AS outputBatchNos,
                CAST(COUNT(1) AS DECIMAL(18,3)) AS inputQty,
                CAST(SUM(CASE WHEN UPPER(COALESCE(p.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END) AS DECIMAL(18,3)) AS doneQty,
                CAST(COUNT(1) AS DECIMAL(18,3)) AS reportQty,
                CAST(0 AS DECIMAL(18,3)) AS defectQty,
                CAST(SUM(CASE WHEN UPPER(COALESCE(p.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END) AS DECIMAL(18,3)) AS confirmedQty,
                SUM(COALESCE(p.output_length, p.input_length, 0)) AS lengthQty,
                CAST(NULL AS DECIMAL(18,3)) AS startPosition,
                CAST(NULL AS DECIMAL(18,3)) AS processLength,
                NULLIF(MAX(GREATEST(
                  IF(p.end_time >= '1972-01-01 00:00:00', p.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.confirmer_time >= '1972-01-01 00:00:00', p.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.recorder_time >= '1972-01-01 00:00:00', p.recorder_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.update_time >= '1972-01-01 00:00:00', p.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.create_time >= '1972-01-01 00:00:00', p.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                )), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                _utf8mb4'片' COLLATE utf8mb4_unicode_ci AS reportUnit,
                GROUP_CONCAT(DISTINCT NULLIF(p.remark, '') SEPARATOR '；') AS remark
              FROM mes_sfc_press_slot_report p
              WHERE p.deleted = 0
                AND UPPER(COALESCE(p.report_status, 'SUBMITTED')) IN ('CONFIRMED', 'SUBMITTED')
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT LIKE '%异常%'
                AND COALESCE(NULLIF(TRIM(p.defect_code), ''), '') = ''
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"sourcengprocessname"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"visualinspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"changeover"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionsamplecategory":"first_inspection"%'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'PRESS_SLOT_REPORT'
                    AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                    AND (
                      fai.source_report_id = p.id
                      OR (
                        fai.plan_order_id = p.plan_id
                        AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(
                          NULLIF(p.production_batch_no, ''),
                          NULLIF(p.source_production_batch_no, ''),
                          NULLIF(p.source_batch_no, ''),
                          NULLIF(p.parent_production_batch_no, ''),
                          ''
                        )))
                      )
                    )
                )
                AND p.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY p.plan_id, segmentBatchNo
              UNION ALL
              SELECT
                a2.plan_id AS planId,
                CASE
                  WHEN a2.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_batch_no
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_production_batch_no
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(a2.source_production_batch_no, '-J', 1)
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(a2.source_production_batch_no, CHAR_LENGTH(a2.source_production_batch_no) - 3)
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(a2.source_production_batch_no, CHAR_LENGTH(a2.source_production_batch_no) - 4)
                  WHEN a2.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(a2.production_batch_no, CHAR_LENGTH(a2.production_batch_no) - 3)
                  WHEN a2.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(a2.production_batch_no, CHAR_LENGTH(a2.production_batch_no) - 4)
                  WHEN a2.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.parent_production_batch_no
                  ELSE COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''))
                END AS segmentBatchNo,
                _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'粘胶2' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, '')) SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, '')) SEPARATOR ',') AS outputBatchNos,
                CAST(COUNT(1) AS DECIMAL(18,3)) AS inputQty,
                CAST(SUM(CASE WHEN UPPER(COALESCE(a2.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END) AS DECIMAL(18,3)) AS doneQty,
                CAST(COUNT(1) AS DECIMAL(18,3)) AS reportQty,
                CAST(0 AS DECIMAL(18,3)) AS defectQty,
                CAST(SUM(CASE WHEN UPPER(COALESCE(a2.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END) AS DECIMAL(18,3)) AS confirmedQty,
                SUM(COALESCE(a2.output_length, a2.input_length, 0)) AS lengthQty,
                CAST(NULL AS DECIMAL(18,3)) AS startPosition,
                CAST(NULL AS DECIMAL(18,3)) AS processLength,
                NULLIF(MAX(GREATEST(
                  IF(a2.end_time >= '1972-01-01 00:00:00', a2.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.confirmer_time >= '1972-01-01 00:00:00', a2.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.recorder_time >= '1972-01-01 00:00:00', a2.recorder_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.update_time >= '1972-01-01 00:00:00', a2.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.create_time >= '1972-01-01 00:00:00', a2.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                )), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                _utf8mb4'片' COLLATE utf8mb4_unicode_ci AS reportUnit,
                GROUP_CONCAT(DISTINCT NULLIF(a2.remark, '') SEPARATOR '；') AS remark
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND UPPER(COALESCE(a2.report_status, 'SUBMITTED')) IN ('CONFIRMED', 'SUBMITTED')
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT LIKE '%异常%'
                AND COALESCE(NULLIF(TRIM(a2.defect_code), ''), '') = ''
                AND UPPER(COALESCE(NULLIF(TRIM(a2.product_quality_status), ''), 'NORMAL')) = 'NORMAL'
                AND COALESCE(NULLIF(TRIM(a2.quality_lock_reason), ''), '') = ''
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"sourcengprocessname"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"visualinspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'ADHESIVE2_REPORT'
                    AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                    AND (
                      fai.source_report_id = a2.id
                      OR (
                        fai.plan_order_id = a2.plan_id
                        AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(
                          NULLIF(a2.production_batch_no, ''),
                          NULLIF(a2.source_production_batch_no, ''),
                          NULLIF(a2.source_batch_no, ''),
                          NULLIF(a2.parent_production_batch_no, ''),
                          ''
                        )))
                      )
                    )
                )
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY a2.plan_id, segmentBatchNo
              UNION ALL
              SELECT
                c.plan_id AS planId,
                CASE
                  WHEN c.source_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_batch_no
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_production_batch_no
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$' THEN SUBSTRING_INDEX(c.source_production_batch_no, '-J', 1)
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(c.source_production_batch_no, CHAR_LENGTH(c.source_production_batch_no) - 3)
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(c.source_production_batch_no, CHAR_LENGTH(c.source_production_batch_no) - 4)
                  WHEN c.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(c.production_batch_no, CHAR_LENGTH(c.production_batch_no) - 3)
                  WHEN c.production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(c.production_batch_no, CHAR_LENGTH(c.production_batch_no) - 4)
                  WHEN c.parent_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.parent_production_batch_no
                  ELSE COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), NULLIF(c.production_batch_no, ''))
                END AS segmentBatchNo,
                _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'裁切' COLLATE utf8mb4_unicode_ci AS stageName,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, '')) SEPARATOR ',') AS sourceBatchNos,
                GROUP_CONCAT(DISTINCT COALESCE(NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, '')) SEPARATOR ',') AS outputBatchNos,
                CAST(COUNT(1) AS DECIMAL(18,3)) AS inputQty,
                CAST(SUM(CASE WHEN UPPER(COALESCE(c.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END) AS DECIMAL(18,3)) AS doneQty,
                CAST(COUNT(1) AS DECIMAL(18,3)) AS reportQty,
                CAST(0 AS DECIMAL(18,3)) AS defectQty,
                CAST(SUM(CASE WHEN UPPER(COALESCE(c.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END) AS DECIMAL(18,3)) AS confirmedQty,
                SUM(COALESCE(c.output_length, c.input_length, 0)) AS lengthQty,
                CAST(NULL AS DECIMAL(18,3)) AS startPosition,
                CAST(NULL AS DECIMAL(18,3)) AS processLength,
                NULLIF(MAX(GREATEST(
                  IF(c.end_time >= '1972-01-01 00:00:00', c.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.confirmer_time >= '1972-01-01 00:00:00', c.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.recorder_time >= '1972-01-01 00:00:00', c.recorder_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.update_time >= '1972-01-01 00:00:00', c.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.create_time >= '1972-01-01 00:00:00', c.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                )), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
                _utf8mb4'片' COLLATE utf8mb4_unicode_ci AS reportUnit,
                GROUP_CONCAT(DISTINCT NULLIF(c.remark, '') SEPARATOR '；') AS remark
              FROM mes_sfc_cut_round_report c
              WHERE c.deleted = 0
                AND UPPER(COALESCE(c.report_status, 'SUBMITTED')) IN ('CONFIRMED', 'SUBMITTED')
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT LIKE '%异常%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT LIKE '%异常%'
                AND COALESCE(NULLIF(TRIM(c.defect_code), ''), '') = ''
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"sourcengprocessname"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"visualinspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'CUT_ROUND_REPORT'
                    AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                    AND (
                      fai.source_report_id = c.id
                      OR (
                        fai.plan_order_id = c.plan_id
                        AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(
                          NULLIF(c.production_batch_no, ''),
                          NULLIF(c.source_production_batch_no, ''),
                          NULLIF(c.source_batch_no, ''),
                          NULLIF(c.parent_production_batch_no, ''),
                          ''
                        )))
                      )
                    )
                )
                AND c.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              GROUP BY c.plan_id, segmentBatchNo
            ) q
            GROUP BY q.planId, q.segmentBatchNo, q.stageCode
            ORDER BY q.planId, q.segmentBatchNo,
              FIELD(q.stageCode, 'FORMULA', 'WET', 'GRINDING', 'ADHESIVE1', 'SLITTING',
                    'PRESS_SLOT', 'ADHESIVE2', 'CUT_ROUND')
            </script>
            """)
    // planIds come from the tenant-scoped plan page query; this aggregate SQL uses MySQL COLLATE
    // syntax that the tenant interceptor's JSqlParser cannot parse.
    @InterceptorIgnore(tenantLine = "true")
    List<PlanProcessPivotStageRow> selectPlanProcessPivotStageRows(@Param("planIds") List<Long> planIds);

    @Select("""
            <script>
            SELECT
              q.planId AS planId,
              q.segmentBatchNo AS segmentBatchNo,
              q.stageCode AS stageCode,
              MIN(q.startTime) AS startTime
            FROM (
              SELECT
                r.plan_id AS planId,
                CAST(NULL AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS segmentBatchNo,
                _utf8mb4'WET' COLLATE utf8mb4_unicode_ci AS stageCode,
                r.start_time AS startTime
              FROM mes_sfc_operation_report r
              WHERE r.deleted = 0
                AND r.source_menu_code = 'WET_REPORT'
                AND r.start_time >= '1972-01-01 00:00:00'
                AND r.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT
                t.plan_id AS planId,
                CAST(CASE
                  WHEN t.segment_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN t.segment_batch_no
                  WHEN t.mother_batch_no IS NOT NULL AND t.segment_mark IN ('P', 'Q', 'R', 'S') THEN CONCAT(t.mother_batch_no, t.segment_mark)
                  ELSE NULLIF(t.segment_batch_no, '')
                END AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS segmentBatchNo,
                _utf8mb4'GRINDING' COLLATE utf8mb4_unicode_ci AS stageCode,
                t.start_time AS startTime
              FROM mes_sfc_grinding_segment_timing t
              WHERE t.deleted = 0
                AND t.start_time >= '1972-01-01 00:00:00'
                AND t.pass_type IN ('FIRST', 'SECOND')
                AND t.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT
                at.plan_id AS planId,
                CAST(CASE
                  WHEN at.segment_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN at.segment_batch_no
                  WHEN at.mother_batch_no IS NOT NULL AND at.segment_mark IN ('P', 'Q', 'R', 'S') THEN CONCAT(at.mother_batch_no, at.segment_mark)
                  ELSE NULLIF(at.segment_batch_no, '')
                END AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS segmentBatchNo,
                _utf8mb4'ADHESIVE1' COLLATE utf8mb4_unicode_ci AS stageCode,
                at.start_time AS startTime
              FROM mes_sfc_adhesive_segment_timing at
              WHERE at.deleted = 0
                AND at.start_time >= '1972-01-01 00:00:00'
                AND at.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT
                s.plan_id AS planId,
                CAST(CASE
                  WHEN UPPER(s.source_batch_no) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN UPPER(s.source_batch_no)
                  WHEN UPPER(s.source_production_batch_no) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN UPPER(s.source_production_batch_no)
                  ELSE NULLIF(s.source_batch_no, '')
                END AS CHAR CHARACTER SET utf8mb4) COLLATE utf8mb4_unicode_ci AS segmentBatchNo,
                _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci AS stageCode,
                NULLIF(LEAST(
                  IF(s.create_time >= '1972-01-01 00:00:00', s.create_time, CAST('9999-12-31 23:59:59' AS DATETIME)),
                  IF(s.last_print_time >= '1972-01-01 00:00:00', s.last_print_time, CAST('9999-12-31 23:59:59' AS DATETIME)),
                  IF(s.scan_time >= '1972-01-01 00:00:00', s.scan_time, CAST('9999-12-31 23:59:59' AS DATETIME))
                ), CAST('9999-12-31 23:59:59' AS DATETIME)) AS startTime
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
            ) q
            WHERE q.planId IS NOT NULL
              AND q.stageCode IS NOT NULL
              AND q.startTime IS NOT NULL
            GROUP BY q.planId, q.segmentBatchNo, q.stageCode
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<PlanProcessPivotStageStartRow> selectPlanProcessPivotStageStartRows(@Param("planIds") List<Long> planIds);

    @Select("""
            <script>
            SELECT
              q.planId AS planId,
              q.segmentBatchNo AS segmentBatchNo,
              q.stageCode AS stageCode,
              q.stageName AS stageName,
              GROUP_CONCAT(DISTINCT NULLIF(q.sourceBatchNo, '') SEPARATOR ',') AS sourceBatchNos,
              GROUP_CONCAT(DISTINCT NULLIF(q.outputBatchNo, '') SEPARATOR ',') AS outputBatchNos,
              CAST(0 AS DECIMAL(18,3)) AS inputQty,
              CAST(0 AS DECIMAL(18,3)) AS doneQty,
              CAST(0 AS DECIMAL(18,3)) AS reportQty,
              CAST(COUNT(DISTINCT q.pieceNo) AS DECIMAL(18,3)) AS defectQty,
              CAST(NULL AS DECIMAL(18,3)) AS confirmedQty,
              CAST(0 AS DECIMAL(18,3)) AS lengthQty,
              CAST(NULL AS DECIMAL(18,3)) AS startPosition,
              CAST(NULL AS DECIMAL(18,3)) AS processLength,
              NULLIF(MAX(q.lastReportTime), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
              _utf8mb4'片' COLLATE utf8mb4_unicode_ci AS reportUnit,
              _utf8mb4'工位自检NG' COLLATE utf8mb4_unicode_ci AS remark
            FROM (
              SELECT
                s.plan_id AS planId,
                CASE
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 3)
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 4)
                  ELSE COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, ''), NULLIF(s.slice_serial_no, ''))
                END AS segmentBatchNo,
                _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci AS stageCode,
                _utf8mb4'分切' COLLATE utf8mb4_unicode_ci AS stageName,
                COALESCE(NULLIF(s.slice_serial_no, ''), CONCAT('SLIT-', s.id)) AS pieceNo,
                COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, '')) AS sourceBatchNo,
                NULLIF(s.slice_serial_no, '') AS outputBatchNo,
                GREATEST(
                  IF(s.scan_time >= '1972-01-01 00:00:00', s.scan_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(s.update_time >= '1972-01-01 00:00:00', s.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(s.create_time >= '1972-01-01 00:00:00', s.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (
                  UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) LIKE '%异常%'
                  OR LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) LIKE '%"result":"ng"%'
                  OR LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                )
              UNION ALL
              SELECT
                p.plan_id AS planId,
                CASE
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_production_batch_no
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"adhesive2"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"adhesive2"%'
                    THEN _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cutround"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cutround"%'
                    THEN _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'分切' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"adhesive2"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"adhesive2"%'
                    THEN _utf8mb4'粘胶2' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cutround"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cutround"%'
                    THEN _utf8mb4'裁切' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'压槽' COLLATE utf8mb4_unicode_ci
                END AS stageName,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), CONCAT('PRESS-', p.id)) AS pieceNo,
                COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, '')) AS sourceBatchNo,
                COALESCE(NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, '')) AS outputBatchNo,
                GREATEST(
                  IF(p.end_time >= '1972-01-01 00:00:00', p.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.confirmer_time >= '1972-01-01 00:00:00', p.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.update_time >= '1972-01-01 00:00:00', p.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.create_time >= '1972-01-01 00:00:00', p.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime
              FROM mes_sfc_press_slot_report p
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = p.source_slitting_slice_id
              WHERE p.deleted = 0
                AND p.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (
                  UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) LIKE '%异常%'
                  OR COALESCE(NULLIF(TRIM(p.defect_code), ''), '') != ''
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                )
              UNION ALL
              SELECT
                a2.plan_id AS planId,
                CASE
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_production_batch_no
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press-slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press-slot"%'
                    THEN _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cutround"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cutround"%'
                    THEN _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'分切' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press-slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press-slot"%'
                    THEN _utf8mb4'压槽' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cutround"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cutround"%'
                    THEN _utf8mb4'裁切' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'粘胶2' COLLATE utf8mb4_unicode_ci
                END AS stageName,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), CONCAT('ADH2-', a2.id)) AS pieceNo,
                COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, '')) AS sourceBatchNo,
                COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, '')) AS outputBatchNo,
                GREATEST(
                  IF(a2.end_time >= '1972-01-01 00:00:00', a2.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.confirmer_time >= '1972-01-01 00:00:00', a2.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.update_time >= '1972-01-01 00:00:00', a2.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.create_time >= '1972-01-01 00:00:00', a2.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime
              FROM mes_sfc_adhesive2_report a2
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = a2.source_slitting_slice_id
              LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = 0 AND ps.id = a2.source_press_slot_report_id
              LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = 0 AND psl.id = ps.source_slitting_slice_id
              WHERE a2.deleted = 0
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (
                  UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) LIKE '%异常%'
                  OR COALESCE(NULLIF(TRIM(a2.defect_code), ''), '') != ''
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.product_quality_status), ''), 'NORMAL')) != 'NORMAL'
                  OR COALESCE(NULLIF(TRIM(a2.quality_lock_reason), ''), '') != ''
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                )
              UNION ALL
              SELECT
                c.plan_id AS planId,
                CASE
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_production_batch_no
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), NULLIF(c.production_batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press-slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press-slot"%'
                    THEN _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"adhesive2"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"adhesive2"%'
                    THEN _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'分切' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press-slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press-slot"%'
                    THEN _utf8mb4'压槽' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"adhesive2"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"adhesive2"%'
                    THEN _utf8mb4'粘胶2' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'裁切' COLLATE utf8mb4_unicode_ci
                END AS stageName,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), CONCAT('CUT-', c.id)) AS pieceNo,
                COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, '')) AS sourceBatchNo,
                COALESCE(NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, '')) AS outputBatchNo,
                GREATEST(
                  IF(c.end_time >= '1972-01-01 00:00:00', c.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.confirmer_time >= '1972-01-01 00:00:00', c.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.inspection_time >= '1972-01-01 00:00:00', c.inspection_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.update_time >= '1972-01-01 00:00:00', c.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.create_time >= '1972-01-01 00:00:00', c.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime
              FROM mes_sfc_cut_round_report c
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = c.source_slitting_slice_id
              LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = 0 AND ps.id = c.source_press_slot_report_id
              LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = 0 AND psl.id = ps.source_slitting_slice_id
              LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = 0 AND a2.id = c.source_adhesive2_report_id
              LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = 0 AND a2sl.id = a2.source_slitting_slice_id
              WHERE c.deleted = 0
                AND c.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (
                  UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) LIKE '%异常%'
                  OR COALESCE(NULLIF(TRIM(c.defect_code), ''), '') != ''
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                )
            ) q
            WHERE q.planId IS NOT NULL
              AND NULLIF(q.segmentBatchNo, '') IS NOT NULL
              AND NULLIF(q.pieceNo, '') IS NOT NULL
            GROUP BY q.planId, q.segmentBatchNo, q.stageCode, q.stageName
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<PlanProcessPivotStageRow> selectPlanProcessPivotStageDefectRows(@Param("planIds") List<Long> planIds);

    @Select("""
            <script>
            SELECT
              q.planId AS planId,
              q.segmentBatchNo AS segmentBatchNo,
              q.stageCode AS stageCode,
              q.pieceNo AS pieceNo,
              MAX(q.sourceBatchNo) AS sourceBatchNo,
              MAX(q.outputBatchNo) AS outputBatchNo,
              MAX(q.actualModelCode) AS actualModelCode,
              MAX(q.actualSizeSpec) AS actualSizeSpec,
              MAX(q.reportConfirmed) AS reportConfirmed,
              MAX(q.defectFlag) AS defectFlag,
              MAX(q.coaFlag) AS coaFlag,
              MIN(q.sourceSlittingOkFlag) AS sourceSlittingOkFlag,
              NULLIF(MAX(q.lastReportTime), CAST('1972-01-01 00:00:00' AS DATETIME)) AS lastReportTime,
              GROUP_CONCAT(DISTINCT NULLIF(q.remark, '') SEPARATOR '；') AS remark
            FROM (
              SELECT
                s.plan_id AS planId,
                CASE
                  WHEN s.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN s.source_production_batch_no
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 3)
                  WHEN s.slice_serial_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(s.slice_serial_no, CHAR_LENGTH(s.slice_serial_no) - 4)
                  ELSE COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, ''), NULLIF(s.slice_serial_no, ''))
                END AS segmentBatchNo,
                _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci AS stageCode,
                COALESCE(NULLIF(s.slice_serial_no, ''), CONCAT('SLIT-', s.id)) AS pieceNo,
                COALESCE(NULLIF(s.source_production_batch_no, ''), NULLIF(s.source_batch_no, '')) AS sourceBatchNo,
                NULLIF(s.slice_serial_no, '') AS outputBatchNo,
                NULL AS actualModelCode,
                COALESCE(NULLIF(s.size_name, ''), NULLIF(s.size_code, '')) AS actualSizeSpec,
                CASE WHEN UPPER(COALESCE(s.scan_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END AS reportConfirmed,
                CASE WHEN (
                  UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) LIKE '%异常%'
                  OR LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) LIKE '%"result":"ng"%'
                  OR LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                ) THEN 1 ELSE 0 END AS defectFlag,
                CASE WHEN EXISTS (
                  SELECT 1 FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.plan_order_id = s.plan_id
                    AND fai.source_module IN ('SLITTING', 'SLITTING_REPORT', 'SLITTING_SLICE')
                    AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(NULLIF(s.slice_serial_no, ''), CONCAT('SLIT-', s.id))))
                    AND UPPER(COALESCE(fai.source_report_no, '')) LIKE '%COA%'
                ) THEN 1 ELSE 0 END AS coaFlag,
                CASE WHEN (s.tenant_id) = s.tenant_id AND UPPER(TRIM(s.scan_status)) = 'CONFIRMED' AND UPPER(TRIM(s.self_check)) = 'OK' AND LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) NOT LIKE '%"result":"ng"%' AND LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%' THEN 1 ELSE 0 END AS sourceSlittingOkFlag,
                GREATEST(
                  IF(s.scan_time >= '1972-01-01 00:00:00', s.scan_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(s.update_time >= '1972-01-01 00:00:00', s.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(s.create_time >= '1972-01-01 00:00:00', s.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime,
                NULLIF(CONCAT_WS('；',
                  CASE
                    WHEN UPPER(COALESCE(NULLIF(TRIM(s.self_check), ''), 'OK')) NOT IN ('OK', 'PASS', 'Y', 'YES', 'TRUE')
                      THEN CONCAT(_utf8mb4'自检：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(s.self_check), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) LIKE '%"result":"ng"%'
                      OR LOWER(REPLACE(COALESCE(s.visual_result_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                      THEN CONCAT(_utf8mb4'目视自检：' COLLATE utf8mb4_unicode_ci, NULLIF(s.visual_result_json, ''))
                    ELSE NULL
                  END,
                  NULLIF(s.remark, '')
                ), '') AS remark
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT
                p.plan_id AS planId,
                CASE
                  WHEN p.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN p.source_production_batch_no
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), NULLIF(p.production_batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"adhesive2"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"adhesive2"%'
                    THEN _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cutround"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cutround"%'
                    THEN _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), CONCAT('PRESS-', p.id)) AS pieceNo,
                COALESCE(NULLIF(p.source_production_batch_no, ''), NULLIF(p.source_batch_no, '')) AS sourceBatchNo,
                COALESCE(NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, '')) AS outputBatchNo,
                NULLIF(p.model_code, '') AS actualModelCode,
                COALESCE(NULLIF(sl.size_name, ''), NULLIF(sl.size_code, '')) AS actualSizeSpec,
                CASE WHEN UPPER(COALESCE(p.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END AS reportConfirmed,
                CASE WHEN (
                  UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) LIKE '%异常%'
                  OR COALESCE(NULLIF(TRIM(p.defect_code), ''), '') != ''
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                  OR (
                    (
                      LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"preprocessselfcheckabnormal":true%'
                      OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributiontype":"pre_process_self_check"%'
                    )
                    AND (
                      LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                      OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    )
                  )
                ) THEN 1 ELSE 0 END AS defectFlag,
                CASE WHEN (
                  LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"coaflag":true%'
                  OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"coaflag":1%'
                  OR EXISTS (
                    SELECT 1 FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.plan_order_id = p.plan_id
                      AND fai.source_module = 'PRESS_SLOT_REPORT'
                      AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(p.production_batch_no, ''), NULLIF(p.parent_production_batch_no, ''), CONCAT('PRESS-', p.id))))
                      AND UPPER(COALESCE(fai.source_report_no, '')) LIKE '%COA%'
                  )
                ) THEN 1 ELSE 0 END AS coaFlag,
                CASE WHEN (sl.tenant_id) = p.tenant_id AND UPPER(TRIM(sl.scan_status)) = 'CONFIRMED' AND UPPER(TRIM(sl.self_check)) = 'OK' AND LOWER(REPLACE(COALESCE(sl.visual_result_json, ''), ' ', '')) NOT LIKE '%"result":"ng"%' AND LOWER(REPLACE(COALESCE(sl.visual_result_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%' THEN 1 ELSE 0 END AS sourceSlittingOkFlag,
                GREATEST(
                  IF(p.end_time >= '1972-01-01 00:00:00', p.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.confirmer_time >= '1972-01-01 00:00:00', p.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.update_time >= '1972-01-01 00:00:00', p.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(p.create_time >= '1972-01-01 00:00:00', p.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime,
                NULLIF(CONCAT_WS('；',
                  CASE
                    WHEN UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT IN ('OK', 'PASS', 'Y', 'YES', 'TRUE')
                      THEN CONCAT(_utf8mb4'自检：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(p.self_check), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN COALESCE(NULLIF(TRIM(p.defect_code), ''), '') != ''
                      THEN CONCAT(_utf8mb4'缺陷：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(p.defect_code), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                      OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                      OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                      OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                      THEN CONCAT(_utf8mb4'目视自检：' COLLATE utf8mb4_unicode_ci, NULLIF(p.extra_json, ''))
                    ELSE NULL
                  END,
                  NULLIF(p.remark, '')
                ), '') AS remark
              FROM mes_sfc_press_slot_report p
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = p.source_slitting_slice_id
              WHERE p.deleted = 0
                AND UPPER(COALESCE(p.report_status, 'SUBMITTED')) IN ('CONFIRMED', 'SUBMITTED')
                AND (
                  (
                    LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"changeover"%'
                    AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                    AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                    AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionsamplecategory":"first_inspection"%'
                  )
                  OR (
                    (
                      LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"preprocessselfcheckabnormal":true%'
                      OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributiontype":"pre_process_self_check"%'
                    )
                    AND (
                      LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                      OR LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    )
                  )
                )
                AND p.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT
                a2.plan_id AS planId,
                CASE
                  WHEN a2.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN a2.source_production_batch_no
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), NULLIF(a2.production_batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press-slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press-slot"%'
                    THEN _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"cutround"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut_round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cut-round"%'
                    OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"cutround"%'
                    THEN _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), CONCAT('ADH2-', a2.id)) AS pieceNo,
                COALESCE(NULLIF(a2.source_production_batch_no, ''), NULLIF(a2.source_batch_no, '')) AS sourceBatchNo,
                COALESCE(NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, '')) AS outputBatchNo,
                NULLIF(a2.model_code, '') AS actualModelCode,
                COALESCE(NULLIF(sl.size_name, ''), NULLIF(psl.size_name, ''), NULLIF(sl.size_code, ''), NULLIF(psl.size_code, '')) AS actualSizeSpec,
                CASE WHEN UPPER(COALESCE(a2.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END AS reportConfirmed,
                CASE WHEN (
                  UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) LIKE '%异常%'
                  OR COALESCE(NULLIF(TRIM(a2.defect_code), ''), '') != ''
                  OR UPPER(COALESCE(NULLIF(TRIM(a2.product_quality_status), ''), 'NORMAL')) != 'NORMAL'
                  OR COALESCE(NULLIF(TRIM(a2.quality_lock_reason), ''), '') != ''
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                ) THEN 1 ELSE 0 END AS defectFlag,
                CASE WHEN (
                  LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"coaflag":true%'
                  OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"coaflag":1%'
                  OR EXISTS (
                    SELECT 1 FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.plan_order_id = a2.plan_id
                      AND fai.source_module = 'ADHESIVE2_REPORT'
                      AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2.production_batch_no, ''), NULLIF(a2.parent_production_batch_no, ''), CONCAT('ADH2-', a2.id))))
                      AND UPPER(COALESCE(fai.source_report_no, '')) LIKE '%COA%'
                  )
                ) THEN 1 ELSE 0 END AS coaFlag,
                CASE WHEN (CASE WHEN sl.id IS NOT NULL THEN sl.tenant_id WHEN psl.id IS NOT NULL THEN psl.tenant_id END) = a2.tenant_id AND UPPER(TRIM(CASE WHEN sl.id IS NOT NULL THEN sl.scan_status WHEN psl.id IS NOT NULL THEN psl.scan_status END)) = 'CONFIRMED' AND UPPER(TRIM(CASE WHEN sl.id IS NOT NULL THEN sl.self_check WHEN psl.id IS NOT NULL THEN psl.self_check END)) = 'OK' AND LOWER(REPLACE(COALESCE(CASE WHEN sl.id IS NOT NULL THEN sl.visual_result_json WHEN psl.id IS NOT NULL THEN psl.visual_result_json END, ''), ' ', '')) NOT LIKE '%"result":"ng"%' AND LOWER(REPLACE(COALESCE(CASE WHEN sl.id IS NOT NULL THEN sl.visual_result_json WHEN psl.id IS NOT NULL THEN psl.visual_result_json END, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%' THEN 1 ELSE 0 END AS sourceSlittingOkFlag,
                GREATEST(
                  IF(a2.end_time >= '1972-01-01 00:00:00', a2.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.confirmer_time >= '1972-01-01 00:00:00', a2.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.update_time >= '1972-01-01 00:00:00', a2.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(a2.create_time >= '1972-01-01 00:00:00', a2.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime,
                NULLIF(CONCAT_WS('；',
                  CASE
                    WHEN UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT IN ('OK', 'PASS', 'Y', 'YES', 'TRUE')
                      THEN CONCAT(_utf8mb4'自检：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(a2.self_check), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN COALESCE(NULLIF(TRIM(a2.defect_code), ''), '') != ''
                      THEN CONCAT(_utf8mb4'缺陷：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(a2.defect_code), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN UPPER(COALESCE(NULLIF(TRIM(a2.product_quality_status), ''), 'NORMAL')) != 'NORMAL'
                      THEN CONCAT(_utf8mb4'质量状态：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(a2.product_quality_status), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN COALESCE(NULLIF(TRIM(a2.quality_lock_reason), ''), '') != ''
                      THEN CONCAT(_utf8mb4'锁定原因：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(a2.quality_lock_reason), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                      OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                      OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                      OR LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                      THEN CONCAT(_utf8mb4'目视自检：' COLLATE utf8mb4_unicode_ci, NULLIF(a2.extra_json, ''))
                    ELSE NULL
                  END,
                  NULLIF(a2.remark, '')
                ), '') AS remark
              FROM mes_sfc_adhesive2_report a2
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = a2.source_slitting_slice_id
              LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = 0 AND ps.id = a2.source_press_slot_report_id
              LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = 0 AND psl.id = ps.source_slitting_slice_id
              WHERE a2.deleted = 0
                AND UPPER(COALESCE(a2.report_status, 'SUBMITTED')) IN ('CONFIRMED', 'SUBMITTED')
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND a2.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT
                c.plan_id AS planId,
                CASE
                  WHEN c.source_production_batch_no REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN c.source_production_batch_no
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), NULLIF(c.production_batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"slitting"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"slitting"%'
                    THEN _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"press-slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press_slot"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"press-slot"%'
                    THEN _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci
                  WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"ngattributionprocesscode":"adhesive2"%'
                    OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"sourcengprocesscode":"adhesive2"%'
                    THEN _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), CONCAT('CUT-', c.id)) AS pieceNo,
                COALESCE(NULLIF(c.source_production_batch_no, ''), NULLIF(c.source_batch_no, '')) AS sourceBatchNo,
                COALESCE(NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, '')) AS outputBatchNo,
                NULLIF(c.model_code, '') AS actualModelCode,
                COALESCE(
                  CASE
                    WHEN JSON_VALID(COALESCE(c.extra_json, '')) THEN
                      NULLIF(JSON_UNQUOTE(JSON_EXTRACT(c.extra_json, '$.actualSizeRule')), '')
                    ELSE NULL
                  END,
                  CASE
                    WHEN UPPER(COALESCE(c.production_batch_no, '')) REGEXP '[PQRS][0-9][0-9][0-9]A$' THEN _utf8mb4'775mm' COLLATE utf8mb4_unicode_ci
                    WHEN UPPER(COALESCE(c.production_batch_no, '')) REGEXP '[PQRS][0-9][0-9][0-9]B$' THEN _utf8mb4'740mm' COLLATE utf8mb4_unicode_ci
                    ELSE NULL
                  END,
                  NULLIF(sl.size_name, ''), NULLIF(psl.size_name, ''), NULLIF(a2sl.size_name, ''),
                  NULLIF(sl.size_code, ''), NULLIF(psl.size_code, ''), NULLIF(a2sl.size_code, '')
                ) AS actualSizeSpec,
                CASE WHEN UPPER(COALESCE(c.report_status, '')) = 'CONFIRMED' THEN 1 ELSE 0 END AS reportConfirmed,
                CASE WHEN (
                  UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                  OR UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) LIKE 'NG%'
                  OR UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) LIKE '%不合格%'
                  OR UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) LIKE '%异常%'
                  OR COALESCE(NULLIF(TRIM(c.defect_code), ''), '') != ''
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                ) THEN 1 ELSE 0 END AS defectFlag,
                CASE WHEN (
                  LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"coaflag":true%'
                  OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"coaflag":1%'
                  OR EXISTS (
                    SELECT 1 FROM mes_qms_fai_order fai
                    WHERE fai.deleted = 0
                      AND fai.plan_order_id = c.plan_id
                      AND fai.source_module = 'CUT_ROUND_REPORT'
                      AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), CONCAT('CUT-', c.id))))
                      AND UPPER(COALESCE(fai.source_report_no, '')) LIKE '%COA%'
                  )
                  OR EXISTS (
                    SELECT 1 FROM mes_qms_fqc_order fqc
                    WHERE fqc.deleted = 0
                      AND fqc.plan_order_id = c.plan_id
                      AND fqc.source_module IN ('CUT_ROUND', 'CUT_ROUND_REPORT', 'CUT_ROUND_FQC')
                      AND UPPER(TRIM(COALESCE(NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, '')))) = UPPER(TRIM(COALESCE(NULLIF(sl.slice_serial_no, ''), NULLIF(psl.slice_serial_no, ''), NULLIF(a2sl.slice_serial_no, ''), NULLIF(c.production_batch_no, ''), NULLIF(c.parent_production_batch_no, ''), CONCAT('CUT-', c.id))))
                      AND UPPER(COALESCE(fqc.source_report_no, '')) LIKE '%COA%'
                  )
                ) THEN 1 ELSE 0 END AS coaFlag,
                CASE WHEN (CASE WHEN sl.id IS NOT NULL THEN sl.tenant_id WHEN psl.id IS NOT NULL THEN psl.tenant_id WHEN a2sl.id IS NOT NULL THEN a2sl.tenant_id END) = c.tenant_id AND UPPER(TRIM(CASE WHEN sl.id IS NOT NULL THEN sl.scan_status WHEN psl.id IS NOT NULL THEN psl.scan_status WHEN a2sl.id IS NOT NULL THEN a2sl.scan_status END)) = 'CONFIRMED' AND UPPER(TRIM(CASE WHEN sl.id IS NOT NULL THEN sl.self_check WHEN psl.id IS NOT NULL THEN psl.self_check WHEN a2sl.id IS NOT NULL THEN a2sl.self_check END)) = 'OK' AND LOWER(REPLACE(COALESCE(CASE WHEN sl.id IS NOT NULL THEN sl.visual_result_json WHEN psl.id IS NOT NULL THEN psl.visual_result_json WHEN a2sl.id IS NOT NULL THEN a2sl.visual_result_json END, ''), ' ', '')) NOT LIKE '%"result":"ng"%' AND LOWER(REPLACE(COALESCE(CASE WHEN sl.id IS NOT NULL THEN sl.visual_result_json WHEN psl.id IS NOT NULL THEN psl.visual_result_json WHEN a2sl.id IS NOT NULL THEN a2sl.visual_result_json END, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%' THEN 1 ELSE 0 END AS sourceSlittingOkFlag,
                GREATEST(
                  IF(c.end_time >= '1972-01-01 00:00:00', c.end_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.confirmer_time >= '1972-01-01 00:00:00', c.confirmer_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.inspection_time >= '1972-01-01 00:00:00', c.inspection_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.update_time >= '1972-01-01 00:00:00', c.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(c.create_time >= '1972-01-01 00:00:00', c.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ) AS lastReportTime,
                NULLIF(CONCAT_WS('；',
                  CASE
                    WHEN UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT IN ('OK', 'PASS', 'Y', 'YES', 'TRUE')
                      THEN CONCAT(_utf8mb4'自检：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(c.self_check), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN COALESCE(NULLIF(TRIM(c.defect_code), ''), '') != ''
                      THEN CONCAT(_utf8mb4'缺陷：' COLLATE utf8mb4_unicode_ci, NULLIF(TRIM(c.defect_code), ''))
                    ELSE NULL
                  END,
                  CASE
                    WHEN LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"visualinspectionresult":"ng"%'
                      OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"inspectionresult":"ng"%'
                      OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"selfcheck":"ng"%'
                      OR LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) LIKE '%"feedbackresult":"ng"%'
                      THEN CONCAT(_utf8mb4'目视自检：' COLLATE utf8mb4_unicode_ci, NULLIF(c.extra_json, ''))
                    ELSE NULL
                  END,
                  NULLIF(c.remark, '')
                ), '') AS remark
              FROM mes_sfc_cut_round_report c
              LEFT JOIN mes_sfc_slitting_slice_record sl ON sl.deleted = 0 AND sl.id = c.source_slitting_slice_id
              LEFT JOIN mes_sfc_press_slot_report ps ON ps.deleted = 0 AND ps.id = c.source_press_slot_report_id
              LEFT JOIN mes_sfc_slitting_slice_record psl ON psl.deleted = 0 AND psl.id = ps.source_slitting_slice_id
              LEFT JOIN mes_sfc_adhesive2_report a2 ON a2.deleted = 0 AND a2.id = c.source_adhesive2_report_id
              LEFT JOIN mes_sfc_slitting_slice_record a2sl ON a2sl.deleted = 0 AND a2sl.id = a2.source_slitting_slice_id
              WHERE c.deleted = 0
                AND UPPER(COALESCE(c.report_status, 'SUBMITTED')) IN ('CONFIRMED', 'SUBMITTED')
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND c.plan_id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
            ) q
            WHERE q.planId IS NOT NULL
              AND NULLIF(q.segmentBatchNo, '') IS NOT NULL
              AND NULLIF(q.pieceNo, '') IS NOT NULL
            GROUP BY q.planId, q.segmentBatchNo, q.stageCode, q.pieceNo
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<PlanProcessPivotPieceRow> selectPlanProcessPivotPieceRows(@Param("planIds") List<Long> planIds);

    @Select("""
            <script>
            SELECT *
            FROM (
              SELECT
                COALESCE(
                  CASE
                    WHEN fai.plan_order_id IN
                    <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                      #{planId}
                    </foreach>
                    THEN fai.plan_order_id
                    ELSE NULL
                  END,
                  fai_slit.plan_id,
                  fai_press.plan_id,
                  fai_a2.plan_id,
                  fai_cut.plan_id,
                  fai_plan.id
                ) AS planId,
                CASE
                  WHEN fai.source_module = 'GLUE_BOARD_FAI' AND NULLIF(fai.source_report_no, '') REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN NULLIF(fai.source_report_no, '')
                  WHEN fai.source_module = 'GLUE_BOARD_FAI' AND NULLIF(fai.source_report_no, '') REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(NULLIF(fai.source_report_no, ''), CHAR_LENGTH(NULLIF(fai.source_report_no, '')) - 3)
                  WHEN fai.source_module = 'GLUE_BOARD_FAI' AND NULLIF(fai.source_report_no, '') REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(NULLIF(fai.source_report_no, ''), CHAR_LENGTH(NULLIF(fai.source_report_no, '')) - 4)
                  WHEN COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, ''))
                  WHEN COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN fai.source_module = 'FORMULA_REPORT' OR fai.source_operation_name LIKE '%配料%' OR fai.operation_name LIKE '%配料%' THEN _utf8mb4'FORMULA' COLLATE utf8mb4_unicode_ci
                  WHEN fai.source_module = 'WET_REPORT' OR fai.source_operation_name LIKE '%湿法%' OR fai.operation_name LIKE '%湿法%' THEN _utf8mb4'WET' COLLATE utf8mb4_unicode_ci
                  WHEN fai.source_module IN ('ROUGH_GRINDING_REPORT', 'ROUGH_GRINDING_FIRST', 'ROUGH_GRINDING_FIRST_ALLOCATION', 'ROUGH_GRINDING_SECOND', 'ROUGH_GRINDING_SECOND_SEGMENT') OR fai.source_operation_name LIKE '%磨皮%' OR fai.operation_name LIKE '%磨皮%' OR fai.process_category = 'ROUGH_GRINDING' THEN _utf8mb4'GRINDING' COLLATE utf8mb4_unicode_ci
                  WHEN fai.source_module = 'ADHESIVE_REPORT' OR fai.source_operation_name LIKE '%粘胶1%' OR fai.operation_name LIKE '%粘胶1%' OR fai.process_category IN ('ADHESIVE1', 'GLUE_1') THEN _utf8mb4'ADHESIVE1' COLLATE utf8mb4_unicode_ci
                  WHEN fai.source_module IN ('SLITTING', 'SLITTING_REPORT', 'SLITTING_SLICE') OR fai.source_operation_name LIKE '%分切%' OR fai.operation_name LIKE '%分切%' THEN _utf8mb4'SLITTING' COLLATE utf8mb4_unicode_ci
                  WHEN fai.source_module = 'PRESS_SLOT_REPORT' OR fai.source_operation_name LIKE '%压槽%' OR fai.operation_name LIKE '%压槽%' THEN _utf8mb4'PRESS_SLOT' COLLATE utf8mb4_unicode_ci
                  WHEN fai.source_module = 'ADHESIVE2_REPORT' OR fai.source_operation_name LIKE '%粘胶2%' OR fai.operation_name LIKE '%粘胶2%' OR fai.process_category IN ('ADHESIVE2', 'GLUE_2') THEN _utf8mb4'ADHESIVE2' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                CASE
                  WHEN fai.source_module = 'GLUE_BOARD_FAI' THEN _utf8mb4'GLUE_BOARD_FAI' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'FAI' COLLATE utf8mb4_unicode_ci
                END AS sourceType,
                fai.id AS inspectionId,
                fai.fai_no AS inspectionNo,
                CASE
                  WHEN fai.source_module = 'GLUE_BOARD_FAI' THEN _utf8mb4'胶板检验' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'首件检验' COLLATE utf8mb4_unicode_ci
                END AS inspectionType,
                COALESCE(NULLIF(fai.product_batch_no, ''), NULLIF(fai.glue_plate_batch_no, ''), NULLIF(fai.source_report_no, '')) AS productBatchNo,
                CAST(COALESCE(fai.inspection_qty, 1) AS DECIMAL(18,3)) AS inspectionQty,
                CAST(CASE
                  WHEN UPPER(COALESCE(fai.judgment, '')) = 'NG'
                    OR UPPER(COALESCE(fai.status, '')) = 'REJECTED'
                    OR COALESCE(fai.abnormal_item_count, 0) > 0
                    THEN GREATEST(COALESCE(fai.abnormal_item_count, 0), 1)
                  ELSE 0
                END AS DECIMAL(18,3)) AS inspectionNgQty,
                CASE
                  WHEN UPPER(COALESCE(fai.source_report_no, '')) LIKE '%COA%'
                    OR UPPER(COALESCE(fai.remark, '')) LIKE '%COA%'
                    THEN 1
                  ELSE 0
                END AS coaInspectionFlag,
                CASE WHEN fai.source_module = 'PRESS_SLOT_REPORT' AND EXISTS (
                  SELECT 1 FROM mes_sfc_press_slot_report sample
                  WHERE sample.deleted = 0 AND sample.tenant_id = fai.tenant_id
                    AND sample.plan_id = fai.plan_order_id
                    AND UPPER(TRIM(sample.production_batch_no)) = UPPER(TRIM(fai.product_batch_no))
                    AND UPPER(sample.report_status) = 'CONFIRMED'
                    AND LOWER(REPLACE(COALESCE(sample.extra_json, ''), ' ', ''))
                        LIKE '%"inspectionsamplecategory":"first_inspection"%'
                ) THEN 1 ELSE 0 END AS firstInspectionSampleFlag,
                CASE WHEN fai.source_module = 'PRESS_SLOT_REPORT' AND EXISTS (
                  SELECT 1 FROM mes_sfc_press_slot_report loss_report
                  JOIN mes_sfc_slitting_slice_record loss_slice
                    ON loss_slice.id = loss_report.source_slitting_slice_id
                    AND loss_slice.deleted = 0 AND loss_slice.tenant_id = loss_report.tenant_id
                  WHERE loss_report.deleted = 0 AND loss_report.tenant_id = fai.tenant_id
                    AND loss_report.plan_id = fai.plan_order_id
                    AND UPPER(TRIM(loss_report.production_batch_no)) = UPPER(TRIM(fai.product_batch_no))
                    AND UPPER(TRIM(loss_slice.scan_status)) = 'CONFIRMED'
                    AND UPPER(TRIM(loss_slice.self_check)) = 'OK'
                    AND LOWER(REPLACE(COALESCE(loss_slice.visual_result_json, ''), ' ', '')) NOT LIKE '%"result":"ng"%'
                    AND LOWER(REPLACE(COALESCE(loss_slice.visual_result_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                ) THEN 1 ELSE 0 END AS sourceSlittingOkFlag,
                fai.judgment AS judgment,
                fai.status AS status,
                NULLIF(GREATEST(
                  IF(fai.inspection_time >= '1972-01-01 00:00:00', fai.inspection_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fai.submission_time >= '1972-01-01 00:00:00', fai.submission_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fai.qa_time >= '1972-01-01 00:00:00', fai.qa_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fai.update_time >= '1972-01-01 00:00:00', fai.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fai.create_time >= '1972-01-01 00:00:00', fai.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ), CAST('1972-01-01 00:00:00' AS DATETIME)) AS inspectionTime,
                (
                  SELECT GROUP_CONCAT(DISTINCT NULLIF(TRIM(COALESCE(fs.defect_name, fs.defect_code,
                    CASE WHEN UPPER(COALESCE(fs.sample_result, '')) = 'NG' THEN fs.metric_code END)), '') SEPARATOR '、')
                  FROM mes_qms_fai_sample fs
                  WHERE fs.deleted = 0
                    AND fs.fai_id = fai.id
                    AND (UPPER(COALESCE(fs.sample_result, '')) = 'NG'
                      OR COALESCE(NULLIF(fs.defect_name, ''), NULLIF(fs.defect_code, '')) IS NOT NULL)
                ) AS defectSummary,
                fai.remark AS remark
              FROM mes_qms_fai_order fai
              LEFT JOIN mes_sfc_slitting_slice_record fai_slit ON fai_slit.deleted = 0
                AND fai.source_module IN ('SLITTING', 'SLITTING_REPORT', 'SLITTING_SLICE')
                AND (
                  UPPER(TRIM(COALESCE(fai_slit.plan_no, ''))) = UPPER(TRIM(COALESCE(fai.work_order_no, '')))
                  OR UPPER(TRIM(COALESCE(fai.source_report_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(fai_slit.plan_no, ''))), '-%')
                )
                AND (
                  fai_slit.id = fai.source_report_id
                  OR UPPER(TRIM(COALESCE(fai_slit.slice_serial_no, ''))) = UPPER(TRIM(COALESCE(fai.product_batch_no, '')))
                )
              LEFT JOIN mes_sfc_press_slot_report fai_press ON fai_press.deleted = 0
                AND fai.source_module = 'PRESS_SLOT_REPORT'
                AND (
                  UPPER(TRIM(COALESCE(fai_press.plan_no, ''))) = UPPER(TRIM(COALESCE(fai.work_order_no, '')))
                  OR UPPER(TRIM(COALESCE(fai.source_report_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(fai_press.plan_no, ''))), '-%')
                )
                AND (
                  fai_press.id = fai.source_report_id
                  OR UPPER(TRIM(COALESCE(NULLIF(fai_press.production_batch_no, ''), NULLIF(fai_press.parent_production_batch_no, '')))) = UPPER(TRIM(COALESCE(fai.product_batch_no, '')))
                )
              LEFT JOIN mes_sfc_adhesive2_report fai_a2 ON fai_a2.deleted = 0
                AND fai.source_module = 'ADHESIVE2_REPORT'
                AND (
                  UPPER(TRIM(COALESCE(fai_a2.plan_no, ''))) = UPPER(TRIM(COALESCE(fai.work_order_no, '')))
                  OR UPPER(TRIM(COALESCE(fai.source_report_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(fai_a2.plan_no, ''))), '-%')
                )
                AND (
                  fai_a2.id = fai.source_report_id
                  OR UPPER(TRIM(COALESCE(NULLIF(fai_a2.production_batch_no, ''), NULLIF(fai_a2.parent_production_batch_no, '')))) = UPPER(TRIM(COALESCE(fai.product_batch_no, '')))
                )
              LEFT JOIN mes_sfc_cut_round_report fai_cut ON fai_cut.deleted = 0
                AND fai.source_module = 'CUT_ROUND_REPORT'
                AND (
                  UPPER(TRIM(COALESCE(fai_cut.plan_no, ''))) = UPPER(TRIM(COALESCE(fai.work_order_no, '')))
                  OR UPPER(TRIM(COALESCE(fai.source_report_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(fai_cut.plan_no, ''))), '-%')
                )
                AND (
                  fai_cut.id = fai.source_report_id
                  OR UPPER(TRIM(COALESCE(NULLIF(fai_cut.production_batch_no, ''), NULLIF(fai_cut.parent_production_batch_no, '')))) = UPPER(TRIM(COALESCE(fai.product_batch_no, '')))
                )
              LEFT JOIN mes_pp_plan_order fai_plan ON fai_plan.deleted = 0
                AND fai_plan.id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (
                  UPPER(TRIM(COALESCE(fai_plan.plan_no, ''))) = UPPER(TRIM(COALESCE(fai.work_order_no, '')))
                  OR UPPER(TRIM(COALESCE(fai_plan.plan_no, ''))) = UPPER(TRIM(COALESCE(fai.source_report_no, '')))
                  OR UPPER(TRIM(COALESCE(fai.source_report_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(fai_plan.plan_no, ''))), '-%')
                )
              WHERE fai.deleted = 0
                AND COALESCE(
                  CASE
                    WHEN fai.plan_order_id IN
                    <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                      #{planId}
                    </foreach>
                    THEN fai.plan_order_id
                    ELSE NULL
                  END,
                  fai_slit.plan_id,
                  fai_press.plan_id,
                  fai_a2.plan_id,
                  fai_cut.plan_id,
                  fai_plan.id
                ) IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
              UNION ALL
              SELECT
                COALESCE(
                  CASE
                    WHEN fqc.plan_order_id IN
                    <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                      #{planId}
                    </foreach>
                    THEN fqc.plan_order_id
                    ELSE NULL
                  END,
                  fqc_cut.plan_id,
                  fqc_plan.id
                ) AS planId,
                CASE
                  WHEN COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]$' THEN COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, ''))
                  WHEN COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$' THEN LEFT(COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, ''))) - 3)
                  WHEN COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, '')) REGEXP '^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$' THEN LEFT(COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, '')), CHAR_LENGTH(COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, ''))) - 4)
                  ELSE COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, ''), NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, ''))
                END AS segmentBatchNo,
                CASE
                  WHEN fqc.source_module = 'FG_SHIPPING_FQC' THEN _utf8mb4'SHIPPING_INSPECTION' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'CUT_ROUND' COLLATE utf8mb4_unicode_ci
                END AS stageCode,
                CASE
                  WHEN fqc.source_module = 'FG_SHIPPING_FQC' THEN _utf8mb4'FG_SHIPPING_FQC' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'CUT_ROUND_FQC' COLLATE utf8mb4_unicode_ci
                END AS sourceType,
                fqc.id AS inspectionId,
                fqc.fqc_no AS inspectionNo,
                CASE
                  WHEN fqc.source_module = 'FG_SHIPPING_FQC' THEN _utf8mb4'发货成品检验' COLLATE utf8mb4_unicode_ci
                  ELSE _utf8mb4'裁切成品检验' COLLATE utf8mb4_unicode_ci
                END AS inspectionType,
                COALESCE(NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, ''), NULLIF(fqc_ship_pick.batch_no, ''), NULLIF(fqc.source_report_no, '')) AS productBatchNo,
                CAST(COALESCE(fqc.produce_qty, fqc.sample_qty, fqc.submission_detail_count, 1) AS DECIMAL(18,3)) AS inspectionQty,
                CAST(CASE
                  WHEN COALESCE(fqc.ng_qty, 0) > 0 THEN COALESCE(fqc.ng_qty, 0)
                  WHEN UPPER(COALESCE(fqc.judgment, '')) = 'NG'
                    OR UPPER(COALESCE(fqc.status, '')) = 'REJECTED'
                    OR COALESCE(fqc.abnormal_item_count, 0) > 0
                    THEN GREATEST(COALESCE(fqc.abnormal_item_count, 0), 1)
                  ELSE 0
                END AS DECIMAL(18,3)) AS inspectionNgQty,
                CASE
                  WHEN UPPER(COALESCE(fqc.source_report_no, '')) LIKE '%COA%'
                    OR UPPER(COALESCE(fqc.remark, '')) LIKE '%COA%'
                    THEN 1
                  ELSE 0
                END AS coaInspectionFlag,
                0 AS firstInspectionSampleFlag,
                0 AS sourceSlittingOkFlag,
                fqc.judgment AS judgment,
                fqc.status AS status,
                NULLIF(GREATEST(
                  IF(fqc.inspection_time >= '1972-01-01 00:00:00', fqc.inspection_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fqc.submission_time >= '1972-01-01 00:00:00', fqc.submission_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fqc.qa_time >= '1972-01-01 00:00:00', fqc.qa_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fqc.update_time >= '1972-01-01 00:00:00', fqc.update_time, CAST('1972-01-01 00:00:00' AS DATETIME)),
                  IF(fqc.create_time >= '1972-01-01 00:00:00', fqc.create_time, CAST('1972-01-01 00:00:00' AS DATETIME))
                ), CAST('1972-01-01 00:00:00' AS DATETIME)) AS inspectionTime,
                NULLIF(CONCAT_WS('、',
                  (
                    SELECT GROUP_CONCAT(DISTINCT NULLIF(TRIM(COALESCE(fsd.defect_name, fsd.defect_code)), '') SEPARATOR '、')
                    FROM mes_qms_fqc_sample_defect fsd
                    WHERE fsd.deleted = 0
                      AND fsd.fqc_id = fqc.id
                  ),
                  (
                    SELECT GROUP_CONCAT(DISTINCT NULLIF(TRIM(COALESCE(fs.defect_name, fs.defect_code,
                      CASE WHEN UPPER(COALESCE(fs.sample_result, '')) = 'NG' THEN fs.metric_code END)), '') SEPARATOR '、')
                    FROM mes_qms_fqc_sample fs
                    WHERE fs.deleted = 0
                      AND fs.fqc_id = fqc.id
                      AND (UPPER(COALESCE(fs.sample_result, '')) = 'NG'
                        OR COALESCE(NULLIF(fs.defect_name, ''), NULLIF(fs.defect_code, '')) IS NOT NULL)
                  )
                ), '') AS defectSummary,
                fqc.remark AS remark
              FROM mes_qms_fqc_order fqc
              LEFT JOIN mes_inv_fg_shipping_pick_item fqc_ship_pick ON fqc_ship_pick.deleted = 0
                AND fqc.source_module = 'FG_SHIPPING_FQC'
                AND (
                  UPPER(TRIM(COALESCE(fqc_ship_pick.shipping_quality_no, ''))) = UPPER(TRIM(COALESCE(fqc.fqc_no, '')))
                  OR UPPER(TRIM(COALESCE(fqc_ship_pick.notice_no, ''))) = UPPER(TRIM(COALESCE(fqc.work_order_no, '')))
                  OR UPPER(TRIM(COALESCE(fqc_ship_pick.notice_no, ''))) = UPPER(TRIM(COALESCE(fqc.source_report_no, '')))
                  OR UPPER(TRIM(COALESCE(fqc_ship_pick.batch_no, ''))) = UPPER(TRIM(COALESCE(NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, ''), '')))
                )
              LEFT JOIN mes_sfc_cut_round_report fqc_cut ON fqc_cut.deleted = 0
                AND (
                  (
                    fqc.source_module != 'FG_SHIPPING_FQC'
                    AND (
                      UPPER(TRIM(COALESCE(fqc_cut.plan_no, ''))) = UPPER(TRIM(COALESCE(fqc.work_order_no, '')))
                      OR UPPER(TRIM(COALESCE(fqc.source_report_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(fqc_cut.plan_no, ''))), '-%')
                    )
                    AND (
                      fqc_cut.id = fqc.source_report_id
                      OR fqc_cut.inspection_task_id = fqc.source_report_id
                      OR UPPER(TRIM(COALESCE(fqc_cut.inspection_task_no, ''))) = UPPER(TRIM(COALESCE(fqc.source_report_no, '')))
                      OR UPPER(TRIM(COALESCE(NULLIF(fqc_cut.production_batch_no, ''), NULLIF(fqc_cut.parent_production_batch_no, '')))) = UPPER(TRIM(COALESCE(NULLIF(fqc.product_batch_no, ''), NULLIF(fqc.batch_no, ''))))
                    )
                  )
                  OR (
                    fqc.source_module = 'FG_SHIPPING_FQC'
                    AND (
                      (fqc_ship_pick.source_cut_round_report_id IS NOT NULL AND fqc_cut.id = fqc_ship_pick.source_cut_round_report_id)
                      OR UPPER(TRIM(COALESCE(fqc_cut.production_batch_no, ''))) = UPPER(TRIM(COALESCE(NULLIF(fqc_ship_pick.actual_slice_batch_no, ''), NULLIF(fqc_ship_pick.slice_batch_no, ''), '')))
                    )
                  )
                )
              LEFT JOIN mes_pp_plan_order fqc_plan ON fqc_plan.deleted = 0
                AND fqc_plan.id IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
                AND (
                  UPPER(TRIM(COALESCE(fqc_plan.plan_no, ''))) = UPPER(TRIM(COALESCE(fqc.work_order_no, '')))
                  OR UPPER(TRIM(COALESCE(fqc_plan.plan_no, ''))) = UPPER(TRIM(COALESCE(fqc.source_report_no, '')))
                  OR UPPER(TRIM(COALESCE(fqc.source_report_no, ''))) LIKE CONCAT(UPPER(TRIM(COALESCE(fqc_plan.plan_no, ''))), '-%')
                )
              WHERE fqc.deleted = 0
                AND fqc.source_module IN ('CUT_ROUND', 'CUT_ROUND_REPORT', 'CUT_ROUND_FQC', 'FG_SHIPPING_FQC')
                AND COALESCE(
                  CASE
                    WHEN fqc.plan_order_id IN
                    <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                      #{planId}
                    </foreach>
                    THEN fqc.plan_order_id
                    ELSE NULL
                  END,
                  fqc_cut.plan_id,
                  fqc_plan.id
                ) IN
                <foreach collection="planIds" item="planId" open="(" separator="," close=")">
                  #{planId}
                </foreach>
            ) q
            WHERE q.planId IS NOT NULL
              AND q.stageCode IS NOT NULL
            ORDER BY q.planId, q.segmentBatchNo,
              FIELD(q.stageCode, 'FORMULA', 'WET', 'GRINDING', 'ADHESIVE1', 'SLITTING',
                    'PRESS_SLOT', 'ADHESIVE2', 'CUT_ROUND', 'SHIPPING_INSPECTION'),
              q.inspectionTime DESC,
              q.inspectionId DESC
            </script>
            """)
    @InterceptorIgnore(tenantLine = "true")
    List<PlanProcessPivotInspectionRow> selectPlanProcessPivotInspectionRows(@Param("planIds") List<Long> planIds);

    @Select("""
            SELECT COALESCE(SUM(latest.good_qty), 0)
            FROM (
              SELECT r1.good_qty
              FROM mes_sfc_operation_report r1
              INNER JOIN (
                SELECT report_date, MAX(id) AS max_id
                FROM mes_sfc_operation_report
                WHERE deleted = 0
                  AND plan_operation_id = #{planOperationId}
                  AND source_menu_code = 'FORMULA_REPORT'
                  AND report_type = 'END'
                GROUP BY report_date
              ) latest_daily ON latest_daily.max_id = r1.id
            ) latest
            """)
    BigDecimal selectFormulaTotalGoodQty(@Param("planOperationId") Long planOperationId);

    @Select("""
            SELECT COALESCE(SUM(latest.good_qty), 0)
            FROM (
              SELECT r1.good_qty
              FROM mes_sfc_operation_report r1
              INNER JOIN (
                SELECT report_date, MAX(id) AS max_id
                FROM mes_sfc_operation_report
                WHERE deleted = 0
                  AND plan_operation_id = #{planOperationId}
                  AND source_menu_code = #{sourceMenuCode}
                  AND report_type = 'END'
                GROUP BY report_date
              ) latest_daily ON latest_daily.max_id = r1.id
            ) latest
            """)
    BigDecimal selectTotalGoodQtyBySource(@Param("planOperationId") Long planOperationId,
                                          @Param("sourceMenuCode") String sourceMenuCode);

    @Select("""
            SELECT
              q.planOperationId AS planOperationId,
              q.reportDate AS reportDate,
              SUM(q.goodQty) AS goodQty
            FROM (
              SELECT
                r1.plan_operation_id AS planOperationId,
                DATE(COALESCE(r1.end_time, r1.confirmer_time, r1.recorder_time, r1.update_time, r1.create_time, r1.report_date)) AS reportDate,
                r1.good_qty AS goodQty
              FROM mes_sfc_operation_report r1
              INNER JOIN (
                SELECT
                  plan_operation_id,
                  DATE(COALESCE(end_time, confirmer_time, recorder_time, update_time, create_time, report_date)) AS actual_report_date,
                  MAX(id) AS max_id
                FROM mes_sfc_operation_report
                WHERE deleted = 0
                  AND plan_id = #{planId}
                  AND report_type = 'END'
                  AND COALESCE(source_menu_code, '') != 'ROUGH_GRINDING_REPORT'
                GROUP BY plan_operation_id, actual_report_date
              ) latest_daily ON latest_daily.max_id = r1.id
              UNION ALL
              SELECT
                g1.plan_operation_id AS planOperationId,
                DATE(COALESCE(g1.end_time, g1.confirmer_time, g1.recorder_time, g1.update_time, g1.create_time, g1.report_date)) AS reportDate,
                COALESCE(g1.report_qty, 0) AS goodQty
              FROM mes_sfc_grinding_report g1
              INNER JOIN (
                SELECT
                  plan_operation_id,
                  DATE(COALESCE(end_time, confirmer_time, recorder_time, update_time, create_time, report_date)) AS actual_report_date,
                  MAX(id) AS max_id
                FROM mes_sfc_grinding_report
                WHERE deleted = 0
                  AND plan_id = #{planId}
                  AND report_type IN ('PROGRESS', 'END')
                GROUP BY plan_operation_id, actual_report_date
              ) latest_grinding_daily ON latest_grinding_daily.max_id = g1.id
              UNION ALL
              SELECT
                a1.plan_operation_id AS planOperationId,
                DATE(COALESCE(a1.end_time, a1.confirmer_time, a1.recorder_time, a1.update_time, a1.create_time, a1.report_date)) AS reportDate,
                COALESCE(a1.output_length, a1.input_length, 0) AS goodQty
              FROM mes_sfc_adhesive_report a1
              WHERE a1.deleted = 0
                AND a1.plan_id = #{planId}
                AND COALESCE(a1.report_status, 'SUBMITTED') IN ('CONFIRMED', 'SUBMITTED')
              UNION ALL
              SELECT
                s.plan_operation_id AS planOperationId,
                DATE(COALESCE(s.scan_time, s.update_time, s.create_time)) AS reportDate,
                1 AS goodQty
              FROM mes_sfc_slitting_slice_record s
              WHERE s.deleted = 0
                AND s.plan_id = #{planId}
                AND s.scan_status = 'CONFIRMED'
              UNION ALL
              SELECT
                p.plan_operation_id AS planOperationId,
                DATE(COALESCE(p.end_time, p.confirmer_time, p.recorder_time, p.update_time, p.create_time, p.report_date)) AS reportDate,
                1 AS goodQty
              FROM mes_sfc_press_slot_report p
              WHERE p.deleted = 0
                AND p.plan_id = #{planId}
                AND UPPER(COALESCE(p.report_status, '')) = 'CONFIRMED'
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(p.self_check), ''), 'OK')) NOT LIKE '%异常%'
                AND COALESCE(NULLIF(TRIM(p.defect_code), ''), '') = ''
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"sourcengprocessname"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"visualinspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"changeover"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND LOWER(REPLACE(COALESCE(p.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionsamplecategory":"first_inspection"%'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'PRESS_SLOT_REPORT'
                    AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                    AND (
                      fai.source_report_id = p.id
                      OR (
                        fai.plan_order_id = p.plan_id
                        AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(
                          NULLIF(p.production_batch_no, ''),
                          NULLIF(p.source_production_batch_no, ''),
                          NULLIF(p.source_batch_no, ''),
                          NULLIF(p.parent_production_batch_no, ''),
                          ''
                        )))
                      )
                    )
                )
              UNION ALL
              SELECT
                a2.plan_operation_id AS planOperationId,
                DATE(COALESCE(a2.end_time, a2.confirmer_time, a2.recorder_time, a2.update_time, a2.create_time, a2.report_date)) AS reportDate,
                1 AS goodQty
              FROM mes_sfc_adhesive2_report a2
              WHERE a2.deleted = 0
                AND a2.plan_id = #{planId}
                AND UPPER(COALESCE(a2.report_status, '')) = 'CONFIRMED'
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(a2.self_check), ''), 'OK')) NOT LIKE '%异常%'
                AND COALESCE(NULLIF(TRIM(a2.defect_code), ''), '') = ''
                AND UPPER(COALESCE(NULLIF(TRIM(a2.product_quality_status), ''), 'NORMAL')) = 'NORMAL'
                AND COALESCE(NULLIF(TRIM(a2.quality_lock_reason), ''), '') = ''
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"sourcengprocessname"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"visualinspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(a2.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'ADHESIVE2_REPORT'
                    AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                    AND (
                      fai.source_report_id = a2.id
                      OR (
                        fai.plan_order_id = a2.plan_id
                        AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(
                          NULLIF(a2.production_batch_no, ''),
                          NULLIF(a2.source_production_batch_no, ''),
                          NULLIF(a2.source_batch_no, ''),
                          NULLIF(a2.parent_production_batch_no, ''),
                          ''
                        )))
                      )
                    )
                )
              UNION ALL
              SELECT
                c.plan_operation_id AS planOperationId,
                DATE(COALESCE(c.end_time, c.confirmer_time, c.recorder_time, c.update_time, c.create_time, c.report_date)) AS reportDate,
                1 AS goodQty
              FROM mes_sfc_cut_round_report c
              WHERE c.deleted = 0
                AND c.plan_id = #{planId}
                AND UPPER(COALESCE(c.report_status, '')) = 'CONFIRMED'
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.self_check), ''), 'OK')) NOT LIKE '%异常%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT IN ('NG', 'N', 'FALSE', 'ABNORMAL', 'FAIL', 'FAILED', '不合格', '异常')
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT LIKE 'NG%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT LIKE '%不合格%'
                AND UPPER(COALESCE(NULLIF(TRIM(c.inspection_result), ''), 'OK')) NOT LIKE '%异常%'
                AND COALESCE(NULLIF(TRIM(c.defect_code), ''), '') = ''
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"sourcengprocessname"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"visualinspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionresult":"ng"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"selfcheck":"ng"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"reporttype":"process_check"%'
                AND LOWER(REPLACE(COALESCE(c.extra_json, ''), ' ', '')) NOT LIKE '%"inspectionscene":"process_check"%'
                AND NOT EXISTS (
                  SELECT 1
                  FROM mes_qms_fai_order fai
                  WHERE fai.deleted = 0
                    AND fai.source_module = 'CUT_ROUND_REPORT'
                    AND COALESCE(fai.source_report_no, '') NOT LIKE '%-COA-%'
                    AND (
                      fai.source_report_id = c.id
                      OR (
                        fai.plan_order_id = c.plan_id
                        AND UPPER(TRIM(COALESCE(fai.product_batch_no, ''))) = UPPER(TRIM(COALESCE(
                          NULLIF(c.production_batch_no, ''),
                          NULLIF(c.source_production_batch_no, ''),
                          NULLIF(c.source_batch_no, ''),
                          NULLIF(c.parent_production_batch_no, ''),
                          ''
                        )))
                      )
                    )
                )
            ) q
            WHERE q.reportDate IS NOT NULL
            GROUP BY q.planOperationId, q.reportDate
            """)
    List<OperationDailyQtyRow> selectOperationDailyGoodQtyByPlanId(@Param("planId") Long planId);

    @Select("""
            SELECT
              q.planOperationId AS planOperationId,
              q.reportDate AS reportDate,
              q.startTime AS startTime,
              q.endTime AS endTime,
              q.recorderName AS recorderName,
              q.confirmerName AS confirmerName,
              q.remark AS remark
            FROM (
              SELECT
                r.plan_operation_id AS planOperationId,
                r.report_date AS reportDate,
                r.start_time AS startTime,
                r.end_time AS endTime,
                r.recorder_name AS recorderName,
                r.confirmer_name AS confirmerName,
                r.remark AS remark
              FROM mes_sfc_operation_report r
              INNER JOIN (
                SELECT plan_operation_id, MAX(id) AS max_id
                FROM mes_sfc_operation_report
                WHERE deleted = 0
                  AND plan_id = #{planId}
                  AND report_type = 'END'
                  AND COALESCE(source_menu_code, '') != 'ROUGH_GRINDING_REPORT'
                GROUP BY plan_operation_id
              ) latest ON latest.max_id = r.id
              UNION ALL
              SELECT
                g.plan_operation_id AS planOperationId,
                g.report_date AS reportDate,
                g.start_time AS startTime,
                g.end_time AS endTime,
                g.recorder_name AS recorderName,
                g.confirmer_name AS confirmerName,
                CONCAT(
                  '一次磨皮总数：加工', COALESCE(g.first_process_length, 0), 'm、损耗', COALESCE(g.first_loss_length, 0),
                  'm、产出', COALESCE(g.first_output_length, 0), 'm、NAP留样', COALESCE(g.first_nap_sample_length, 0),
                  'm；二次磨皮总数：加工', COALESCE(g.second_process_length, 0), 'm、损耗', COALESCE(g.second_loss_length, 0),
                  'm、产出', COALESCE(g.second_output_length, 0), 'm、NAP留样', COALESCE(g.second_nap_sample_length, 0),
                  'm；报工量：', COALESCE(g.report_qty, 0), 'm',
                  CASE WHEN g.remark IS NULL OR g.remark = '' THEN '' ELSE CONCAT('；备注：', g.remark) END
                ) AS remark
              FROM mes_sfc_grinding_report g
              INNER JOIN (
                SELECT plan_operation_id, MAX(id) AS max_id
                FROM mes_sfc_grinding_report
                WHERE deleted = 0
                  AND plan_id = #{planId}
                  AND report_type IN ('PROGRESS', 'END')
                GROUP BY plan_operation_id
              ) latest_grinding ON latest_grinding.max_id = g.id
            ) q
            """)
    List<OperationLatestReportRow> selectOperationLatestReportByPlanId(@Param("planId") Long planId);
}
