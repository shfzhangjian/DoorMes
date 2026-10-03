package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.module.mes.service.hc.productionbatch.HcProductionBatchService;
import cn.iocoder.yudao.module.mes.service.hc.productionbatch.HcRootBatchReservation;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.fasterxml.jackson.core.type.TypeReference;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inv.stock.vo.HcInvStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleParseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderBatchPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderBatchPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderInventoryLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderInventoryLockReleaseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderOperationStatusReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderStatusReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderWipCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderWipCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotStageRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance.HcLotInstanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationStatusLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderStatusLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processparam.HcProcessParamRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.saleorder.PlanSaleOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance.HcLotInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderCascadeDeleteMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationStatusLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderStatusLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.plan.saleorder.PlanSaleOrderMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleService;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleMatchContext;
import cn.iocoder.yudao.module.mes.service.hc.processparam.HcProcessParamRecordService;
import cn.iocoder.yudao.module.mes.service.hc.qtimeconfig.HcQtimeEvaluationService;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineContext;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineResolverService;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import jakarta.annotation.Resource;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPLANORDER_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPLANORDER_PLANNO_EXISTS;

@Service
@Validated
public class HcPlanOrderServiceImpl implements HcPlanOrderService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_RELEASED = "RELEASED";
    private static final String STATUS_PAUSED = "PAUSED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String LOCK_STATUS_CANCELLED = "CANCELLED";
    private static final String LOCK_STATUS_RELEASED = "RELEASED";
    private static final String STOCK_TYPE_WIP = "WIP";
    private static final String STOCK_TYPE_NG_PIECE = "NG_PIECE";
    private static final String PLAN_MODE_DISCRETE_POST = "DISCRETE_POST";
    private static final String SOURCE_TYPE_DISCRETE_NG = "NG_INVENTORY";
    private static final String SOURCE_TYPE_DISCRETE_WIP = "DISCRETE_WIP";
    private static final String DISCRETE_BATCH_PLACEHOLDER = "多批号加工";
    private static final String OP_STATUS_NOT_RELEASED = "NOT_RELEASED";
    private static final String OP_STATUS_RELEASED = "RELEASED";
    private static final String OP_STATUS_RUNNING = "RUNNING";
    private static final String OP_STATUS_PAUSED = "PAUSED";
    private static final String OP_STATUS_FINISHED = "FINISHED";
    private static final String OP_STATUS_CANCELLED = "CANCELLED";
    private static final String BATCH_STATUS_NOT_GEN = "NOT_GEN";
    private static final String BATCH_STATUS_GENERATED = "GENERATED";
    private static final String OP_CODE_FORMULA = "FORMULA";
    private static final Set<String> FRONT_PROCESS_OPERATION_CODES = Set.of(
            "FORMULA", "OP-FORMULA", "WET", "OP-WET", "GRINDING", "ROUGH_GRINDING", "ROUGH-GRINDING",
            "OP-GRINDING", "OP-GRINDING1", "OP-GRINDING2");
    private static final Set<String> POST_PROCESS_OPERATION_CODES = Set.of(
            "ADHESIVE1", "OP-ADHESIVE1", "SLIT", "SLITTING", "OP-SLIT", "OP-SLITTING",
            "PRESS_SLOT", "PRESS-SLOT", "OP-PRESS-SLOT", "ADHESIVE2", "OP-ADHESIVE2",
            "CUT_ROUND", "CUTROUND", "CUT", "OP-CUT", "OP-CUT-ROUND");
    private static final Pattern INVENTORY_MOTHER_BATCH_PREFIX_PATTERN =
            Pattern.compile("^([A-Z]\\d{2}[A-Z]\\d{3}[A-Z]).*");
    private static final DateTimeFormatter PLAN_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final int PLAN_NO_MAX_SEQUENCE = 999;
    private static final List<String> PIVOT_STAGE_CODES = List.of(
            "FORMULA", "WET", "GRINDING", "ADHESIVE1", "SLITTING",
            "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND", "SHIPPING_INSPECTION");
    private static final List<String> POST_PROCESS_INHERITED_FRONT_STAGE_CODES = List.of("FORMULA", "WET");
    private static final Set<String> PIVOT_PIECE_STAGE_CODES = Set.of(
            "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> PIVOT_SUBMITTED_REPORT_STAGE_CODES = Set.of(
            "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> DISCRETE_POST_PROCESS_STAGE_CODES = Set.of(
            "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final List<String> WIP_CANDIDATE_EXCLUDED_SOURCE_TYPES =
            List.of("CUT_ROUND", "CUTROUND");
    private static final List<String> WIP_CANDIDATE_EXCLUDED_OP_CODES =
            List.of("CUT_ROUND", "CUTROUND", "OP-CUT", "OP-CUT-ROUND");
    private static final List<String> WIP_CANDIDATE_EXCLUDED_OP_NAME_KEYWORDS = List.of("裁切");
    private static final int PIVOT_STAGE_QUERY_BATCH_SIZE = 200;
    private static final Map<String, String> PIVOT_STAGE_NAMES = Map.of(
            "FORMULA", "配料",
            "WET", "湿法",
            "GRINDING", "磨皮",
            "ADHESIVE1", "粘胶1",
            "SLITTING", "分切",
            "PRESS_SLOT", "压槽",
            "ADHESIVE2", "粘胶2",
            "CUT_ROUND", "裁切",
            "SHIPPING_INSPECTION", "发货检验");
    private static final List<CascadeTarget> PLAN_DIRECT_CASCADE_TARGETS = List.of(
            target("mes_qms_abnormal_lock", "plan_id"),
            target("mes_work_order", "plan_id"),
            target("mes_lot_instance", "plan_id"),
            target("mes_sfc_process_param_record", "plan_id"),
            target("mes_hc_process_form_record", "plan_id"),
            target("mes_pp_production_instruction", "plan_id"),
            target("mes_sfc_operation_report_defect", "plan_id"),
            target("mes_sfc_wet_report_abnormal_position", "plan_id"),
            target("mes_sfc_operation_report", "plan_id"),
            target("mes_sfc_adhesive_aqc_task", "plan_id"),
            target("mes_sfc_adhesive_check_detail", "plan_id"),
            target("mes_sfc_adhesive_glue_board_usage", "plan_id"),
            target("mes_sfc_adhesive_intermediate_detail", "plan_id"),
            target("mes_sfc_adhesive_intermediate_record", "plan_id"),
            target("mes_sfc_adhesive_report", "plan_id"),
            target("mes_sfc_adhesive2_check_detail", "plan_id"),
            target("mes_sfc_adhesive2_intermediate_detail", "plan_id"),
            target("mes_sfc_adhesive2_intermediate_record", "plan_id"),
            target("mes_sfc_adhesive2_report", "plan_id"),
            target("mes_sfc_cut_round_check_detail", "plan_id"),
            target("mes_sfc_cut_round_inspection_task", "plan_id"),
            target("mes_sfc_cut_round_report", "plan_id"),
            target("mes_md_cut_round_spare_record", "plan_id"),
            target("mes_sfc_equipment_consumable_event", "plan_id"),
            target("mes_sfc_grinding_first_detail", "plan_id"),
            target("mes_sfc_grinding_middle_product_record", "plan_id"),
            target("mes_sfc_grinding_report", "plan_id"),
            target("mes_sfc_grinding_second_detail", "plan_id"),
            target("mes_sfc_grinding_stock_ledger", "plan_id"),
            target("mes_sfc_slitting_slice_record", "plan_id"),
            target("mes_inv_fg_inbound_order", "plan_id"),
            target("mes_sfc_press_slot_abnormal_lock_item", "plan_id"),
            target("mes_sfc_press_slot_abnormal_lock", "plan_id"),
            target("mes_sfc_press_slot_changeover_inspection", "plan_id"),
            target("mes_sfc_press_slot_check_detail", "plan_id"),
            target("mes_sfc_press_slot_intermediate_detail", "plan_id"),
            target("mes_sfc_press_slot_intermediate_record", "plan_id"),
            target("mes_sfc_press_slot_report", "plan_id"),
            target("mes_md_press_slot_spare_record", "plan_id"),
            target("mes_sfc_inner_pack_unit", "plan_id"),
            target("mes_sfc_inner_pack_unit_item", "plan_id"),
            target("mes_sfc_label_print_log", "plan_id"),
            target("mes_sfc_outer_pack_box", "plan_id"),
            target("mes_sfc_outer_pack_box_item", "plan_id"),
            target("mes_sfc_pack_report", "plan_id"),
            target("mes_rd_research_task", "plan_id"),
            target("mes_sfc_station_record", "plan_id"),
            target("mes_qms_fai_order", "plan_order_id"),
            target("mes_qms_fqc_order", "plan_order_id"),
            target("mes_qms_ipqc_order", "plan_order_id"),
            target("mes_pp_plan_order_status_log", "plan_id"),
            target("mes_pp_plan_operation_status_log", "plan_id"),
            target("mes_pp_plan_inv_lock", "plan_id"),
            target("mes_pp_plan_operation", "plan_id"));
    private static final List<CascadeTarget> FAI_CASCADE_TARGETS = List.of(
            target("mes_qms_fai_abnormal", "fai_id"),
            target("mes_qms_fai_item", "fai_id"),
            target("mes_qms_fai_return_record", "fai_id"),
            target("mes_qms_fai_sample", "fai_id"),
            target("mes_qms_fai_sheet_cell_value", "fai_id"),
            target("mes_qms_fai_sheet_import_batch", "fai_id"),
            target("mes_qms_fai_sheet_stat_result", "fai_id"),
            target("mes_qms_fai_scan_record", "matched_fai_id"));
    private static final List<CascadeTarget> FQC_CASCADE_TARGETS = List.of(
            target("mes_qms_fqc_abnormal", "fqc_id"),
            target("mes_qms_fqc_item", "fqc_id"),
            target("mes_qms_fqc_return_record", "fqc_id"),
            target("mes_qms_fqc_sample_defect", "fqc_id"),
            target("mes_qms_fqc_sample", "fqc_id"),
            target("mes_qms_fqc_sheet_cell_value", "fqc_id"),
            target("mes_qms_fqc_sheet_import_batch", "fqc_id"),
            target("mes_qms_fqc_sheet_stat_result", "fqc_id"),
            target("mes_qms_fqc_shipping_detail", "fqc_id"),
            target("mes_qms_fqc_submission_detail", "fqc_id"),
            target("mes_qms_fqc_scan_record", "matched_fqc_id"));
    private static final List<CascadeTarget> IPQC_CASCADE_TARGETS = List.of(
            target("mes_qms_ipqc_abnormal", "ipqc_id"),
            target("mes_qms_ipqc_item", "ipqc_id"),
            target("mes_qms_ipqc_sample", "ipqc_id"));

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;

    @Resource
    private HcPlanOrderCascadeDeleteMapper hcPlanOrderCascadeDeleteMapper;

    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;

    @Resource
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;

    @Resource
    private HcPlanOrderOperationStatusLogMapper hcPlanOrderOperationStatusLogMapper;

    @Resource
    private HcPlanOrderStatusLogMapper hcPlanOrderStatusLogMapper;

    @Resource
    private PlanSaleOrderMapper planSaleOrderMapper;

    @Resource
    private HcMaterialMapper hcMaterialMapper;

    @Resource
    private HcProcessReportMapper hcProcessReportMapper;

    @Resource
    private HcLotInstanceMapper hcLotInstanceMapper;

    @Resource
    private HcLotRuleMapper hcLotRuleMapper;

    @Resource
    private HcInvStockMapper hcInvStockMapper;

    @Resource
    private HcInvStockService hcInvStockService;

    @Resource
    private HcProcessParamRecordService hcProcessParamRecordService;

    @Resource
    private HcLotRuleService hcLotRuleService;

    @Resource
    private HcProductionLineResolverService hcProductionLineResolverService;

    @Resource
    private HcQtimeEvaluationService hcQtimeEvaluationService;

    @Resource
    private HcProductionBatchService hcProductionBatchService;

    @Resource
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcPlanOrder(HcPlanOrderSaveReqVO createReqVO) {
        normalizeSaveReq(createReqVO, null);
        HcPlanOrderDO entity = BeanUtils.toBean(createReqVO, HcPlanOrderDO.class);
        synchronized (this) {
            entity.setPlanNo(StrUtil.isBlank(createReqVO.getPlanNo())
                    ? generateNextPlanNo()
                    : StrUtil.trim(createReqVO.getPlanNo()));
            validatePlanNoUnique(null, entity.getPlanNo());
            if (StrUtil.isNotBlank(createReqVO.getBatchNo())) {
                validateManualRootBatchUnique(createReqVO, createReqVO.getBatchNo());
            }
            hcPlanOrderMapper.insert(entity);
        }
        List<HcPlanOrderOperationDO> operations = createOperations(entity.getId(), entity, createReqVO.getOperations());
        createInventoryLocks(entity.getId(), entity, createReqVO.getInventoryLocks(), operations);
        hcProductionBatchService.reserveRootBatchOnPlanRelease(entity.getId());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcPlanOrder(HcPlanOrderSaveReqVO updateReqVO) {
        HcPlanOrderDO oldPlanOrder = hcPlanOrderMapper.selectByIdForUpdate(updateReqVO.getId());
        if (oldPlanOrder == null) {
            throw exception(HCPLANORDER_NOT_EXISTS);
        }
        if (StrUtil.isBlank(updateReqVO.getPlanNo())) {
            throw invalidParamException("计划单号不能为空");
        }
        if (!Objects.equals(oldPlanOrder.getPlanNo(), updateReqVO.getPlanNo())) {
            throw invalidParamException("重新排产保留原计划单号，不能修改计划单号");
        }
        validatePlanNoUnique(updateReqVO.getId(), updateReqVO.getPlanNo());
        if (!STATUS_DRAFT.equals(oldPlanOrder.getPlanStatus())) {
            throw invalidParamException("请先撤回未开工计划，再修改并重新下发");
        }
        assertPlanNotStarted(oldPlanOrder);
        // 兼容旧版已经撤回、但仍保留预约的草稿。
        if (HcRootBatchReservation.isReserved(oldPlanOrder)) {
            resetWithdrawnPlan(oldPlanOrder);
        }
        normalizeSaveReq(updateReqVO, oldPlanOrder);
        // 未执行草稿允许重选产品，所有依赖按当前请求重新校验。
        validateReservedRootBatchIdentity(oldPlanOrder, updateReqVO);
        HcPlanOrderDO updateObj = BeanUtils.toBean(updateReqVO, HcPlanOrderDO.class);
        updateObj.setProductionBatchContextJson(oldPlanOrder.getProductionBatchContextJson());
        if (STATUS_RELEASED.equals(updateObj.getPlanStatus())) updateObj.setReleasedAt(LocalDateTime.now());
        hcPlanOrderMapper.updateById(updateObj);
        // 可清空的选项必须显式写入 null，不能让旧路线/配方/批号在重排后残留。
        hcPlanOrderMapper.update(null, new LambdaUpdateWrapper<HcPlanOrderDO>()
                .eq(HcPlanOrderDO::getId, updateObj.getId())
                .set(HcPlanOrderDO::getBatchNo, updateObj.getBatchNo())
                .set(HcPlanOrderDO::getBatchRuleId, updateObj.getBatchRuleId())
                .set(HcPlanOrderDO::getBatchRuleCode, updateObj.getBatchRuleCode())
                .set(HcPlanOrderDO::getBatchRuleVersion, updateObj.getBatchRuleVersion())
                .set(HcPlanOrderDO::getRouteId, updateObj.getRouteId())
                .set(HcPlanOrderDO::getRouteCode, updateObj.getRouteCode())
                .set(HcPlanOrderDO::getRouteName, updateObj.getRouteName())
                .set(HcPlanOrderDO::getRouteVersion, updateObj.getRouteVersion())
                .set(HcPlanOrderDO::getRecipeId, updateObj.getRecipeId())
                .set(HcPlanOrderDO::getRecipeCode, updateObj.getRecipeCode())
                .set(HcPlanOrderDO::getRecipeName, updateObj.getRecipeName())
                .set(HcPlanOrderDO::getBomId, updateObj.getBomId())
                .set(HcPlanOrderDO::getBomVersion, updateObj.getBomVersion())
                .set(HcPlanOrderDO::getProductionEndDate, updateObj.getProductionEndDate())
                .set(HcPlanOrderDO::getSalesOrderId, updateObj.getSalesOrderId())
                .set(HcPlanOrderDO::getSalesOrderNo, updateObj.getSalesOrderNo())
                .set(HcPlanOrderDO::getSalesOrderLineNo, updateObj.getSalesOrderLineNo())
                .set(HcPlanOrderDO::getCustomerId, updateObj.getCustomerId())
                .set(HcPlanOrderDO::getMaterialId, updateObj.getMaterialId())
                .set(HcPlanOrderDO::getModelId, updateObj.getModelId())
                .set(HcPlanOrderDO::getMotherMaterialId, updateObj.getMotherMaterialId())
                .set(HcPlanOrderDO::getMotherModelId, updateObj.getMotherModelId()));
        List<HcPlanOrderOperationDO> operations = updateOperations(updateReqVO.getId(), updateObj, updateReqVO.getOperations());
        updateInventoryLocks(updateReqVO.getId(), updateReqVO.getInventoryLocks(), updateObj, operations);
        hcProductionBatchService.reserveRootBatchOnPlanRelease(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcPlanOrder(Long id) {
        HcPlanOrderDO planOrder = getRequiredPlanOrder(id);
        validateCancelledPlanBeforeDelete(List.of(planOrder));
        releaseInventoryLocks(hcPlanOrderInventoryLockMapper.selectListByPlanId(id), "删除作废生产计划释放挂接中间边库");
        deletePlanOrderCascade(List.of(id), false);
        hcPlanOrderMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcPlanOrderListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        List<HcPlanOrderDO> planOrders = getRequiredPlanOrders(ids);
        validateCancelledPlanBeforeDelete(planOrders);
        for (Long id : normalizedIds(ids)) {
            releaseInventoryLocks(hcPlanOrderInventoryLockMapper.selectListByPlanId(id), "批量删除生产计划释放挂接中间边库");
        }
        List<Long> planIds = planOrders.stream().map(HcPlanOrderDO::getId).toList();
        deletePlanOrderCascade(planIds, false);
        hcPlanOrderMapper.deleteBatchIds(planIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void destroyHcPlanOrder(Long id) {
        getRequiredPlanOrder(id);
        releaseInventoryLocks(hcPlanOrderInventoryLockMapper.selectListByPlanId(id), "销毁生产计划释放挂接中间边库");
        deletePlanOrderCascade(List.of(id), true);
        hcPlanOrderMapper.physicalDeleteById(id);
    }

    @Override
    public HcPlanOrderDO getHcPlanOrder(Long id) {
        return hcPlanOrderMapper.selectById(id);
    }

    @Override
    public List<HcPlanOrderDO> getHcPlanOrderList(HcPlanOrderPageReqVO reqVO) {
        return hcPlanOrderMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcPlanOrderDO> getHcPlanOrderPage(HcPlanOrderPageReqVO pageReqVO) {
        return hcPlanOrderMapper.selectPage(pageReqVO);
    }

    @Override
    public HcPlanOrderBatchPreviewRespVO previewRootBatchNo(HcPlanOrderBatchPreviewReqVO reqVO) {
        if (!isFormulaOperation(reqVO.getOpCode(), reqVO.getOpName())) {
            throw invalidParamException("只有从配料工序开始的计划允许预览产品批号");
        }
        LocalDate bizDate = reqVO.getProductionStartDate();
        if (bizDate == null) {
            throw invalidParamException("计划开始日期不能为空，无法预览母批批号");
        }
        HcLotRuleDO rule = Boolean.TRUE.equals(reqVO.getUseBoundRule()) && reqVO.getBatchRuleId() != null
                ? hcLotRuleService.getHcLotRule(reqVO.getBatchRuleId())
                : null;
        if (rule == null) {
            rule = hcLotRuleService.matchEnabledRule(HcLotRuleMatchContext.builder()
                    .bizType("FG_LOT")
                    .productCategoryCode(reqVO.getCategoryCode())
                    .prodType(reqVO.getProdType())
                    .generationTrigger("FORMULA_START")
                    .generationScope("PLAN_ROOT")
                    .modelCode(firstNotBlank(reqVO.getMotherModelCode(), reqVO.getModelCode(), reqVO.getMaterialCode()))
                    .build());
        }
        HcProductionLineContext lineContext = reqVO.getWorkCenterId() == null
                ? hcProductionLineResolverService.resolveByMotherModelCode(
                        firstNotBlank(reqVO.getMotherModelCode(), reqVO.getModelCode(), reqVO.getMaterialCode()))
                : hcProductionLineResolverService.resolveByWorkCenterId(reqVO.getWorkCenterId());
        HcLotRuleGenerateReqVO generateReq = new HcLotRuleGenerateReqVO();
        generateReq.setRuleId(rule.getId());
        generateReq.setBizDate(bizDate);
        generateReq.setConsumeSequence(false);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("batchLineCode", firstNotBlank(lineContext == null ? null : lineContext.getBatchLineCode(), "A"));
        values.put("lineCode", firstNotBlank(lineContext == null ? null : lineContext.getLineCode(), ""));
        values.put("modelCode", firstNotBlank(reqVO.getMotherModelCode(), reqVO.getModelCode(), reqVO.getMaterialCode()));
        generateReq.setInputValues(values);
        Map<String, Object> generated = hcLotRuleService.generateLotNo(generateReq);

        HcPlanOrderBatchPreviewRespVO respVO = new HcPlanOrderBatchPreviewRespVO();
        respVO.setBatchNo(String.valueOf(generated.get("lotNo")));
        respVO.setRuleId(rule.getId());
        respVO.setRuleCode(rule.getRuleCode());
        respVO.setRuleName(rule.getRuleName());
        respVO.setRuleVersion(rule.getVersionNo());
        respVO.setCurrentSeq((Integer) generated.get("currentSeq"));
        respVO.setNextSeq((Integer) generated.get("nextSeq"));
        respVO.setWarning("草稿仅预览，不占用流水；确认下发时锁定母批批号，配料开工沿用");
        return respVO;
    }

    @Override
    public PageResult<HcPlanProcessPivotRespVO> getPlanProcessPivotPage(HcPlanProcessPivotPageReqVO pageReqVO) {
        List<HcPlanProcessPivotRespVO> list = getPlanProcessPivotList(pageReqVO);
        if (list.isEmpty()) {
            return PageResult.empty();
        }
        return paginatePivotRows(list, pageReqVO);
    }

    @Override
    public List<HcPlanProcessPivotRespVO> getPlanProcessPivotList(HcPlanProcessPivotPageReqVO pageReqVO) {
        List<HcPlanOrderDO> plans = hcPlanOrderMapper.selectList(pageReqVO);
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        List<Long> planIds = plans.stream().map(HcPlanOrderDO::getId).toList();
        Map<Long, PostProcessSourceInfo> postProcessSourceInfoMap = selectPostProcessSourceInfoMap(planIds);
        Map<Long, List<String>> operationStageCodesMap = selectPivotOperationStageCodesMap(planIds);
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotStageRow>> stageRowsMap = selectPivotStageRowsMap(planIds);
        Map<Long, Map<String, LocalDateTime>> stageStartTimeMap = selectPivotStageStartTimeMap(
                resolvePivotQtimePlanIds(planIds, postProcessSourceInfoMap));
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotPieceRow>> pieceRowsMap = selectPivotPieceRowsMap(planIds);
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotInspectionRow>> inspectionRowsMap =
                selectPivotInspectionRowsMap(planIds);
        List<HcPlanProcessPivotRespVO> list = plans.stream()
                .flatMap(plan -> {
                    List<HcPlanProcessPivotRespVO> rows = buildPlanProcessPivotRespList(
                            plan, stageRowsMap.get(plan.getId()), pieceRowsMap.get(plan.getId()),
                            inspectionRowsMap.get(plan.getId()));
                    rows.forEach(row -> row.setOperationStageCodes(
                            operationStageCodesMap.getOrDefault(plan.getId(), List.of())));
                    fillPostProcessPlanInfo(rows, postProcessSourceInfoMap.get(plan.getId()));
                    return rows.stream();
                })
                .toList();
        fillPlanProcessPivotQtime(list, postProcessSourceInfoMap, stageStartTimeMap);
        if (StrUtil.isNotBlank(pageReqVO.getMotherSegmentBatchNo())) {
            String motherSegmentBatchNo = pageReqVO.getMotherSegmentBatchNo().trim();
            list = list.stream()
                    .filter(row -> StrUtil.containsIgnoreCase(row.getSegmentBatchNo(), motherSegmentBatchNo))
                    .toList();
        }
        return list;
    }

    private Map<Long, PostProcessSourceInfo> selectPostProcessSourceInfoMap(Collection<Long> planIds) {
        List<HcPlanOrderInventoryLockDO> postProcessLocks = hcPlanOrderInventoryLockMapper.selectListByPlanIds(planIds)
                .stream()
                .filter(this::isPostProcessInventoryLock)
                .toList();
        if (postProcessLocks.isEmpty()) {
            return Map.of();
        }
        Set<Long> stockIds = postProcessLocks.stream()
                .map(HcPlanOrderInventoryLockDO::getStockId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, HcInvStockDO> stockMap = stockIds.isEmpty()
                ? Map.of()
                : hcInvStockMapper.selectBatchIds(stockIds).stream()
                        .collect(Collectors.toMap(HcInvStockDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Set<Long> sourcePlanIds = postProcessLocks.stream()
                .map(HcPlanOrderInventoryLockDO::getSourcePlanId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, HcPlanOrderDO> sourcePlanMap = sourcePlanIds.isEmpty()
                ? Map.of()
                : hcPlanOrderMapper.selectBatchIds(sourcePlanIds).stream()
                        .collect(Collectors.toMap(HcPlanOrderDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotStageRow>> sourceStageRowsMap = sourcePlanIds.isEmpty()
                ? Map.of()
                : selectPivotStageRowsMap(new ArrayList<>(sourcePlanIds));
        return postProcessLocks.stream()
                .filter(lock -> lock.getPlanId() != null)
                .collect(Collectors.groupingBy(HcPlanOrderInventoryLockDO::getPlanId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> buildPostProcessSourceInfo(entry.getValue(), stockMap, sourcePlanMap, sourceStageRowsMap),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private boolean isPostProcessInventoryLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null) {
            return false;
        }
        String stockType = firstNotBlank(lock.getLockType(), lock.getStockType());
        if (!StrUtil.equalsIgnoreCase(STOCK_TYPE_WIP, stockType)
                && !StrUtil.equalsIgnoreCase(STOCK_TYPE_NG_PIECE, stockType)) {
            return false;
        }
        String lockStatus = StrUtil.trimToEmpty(lock.getLockStatus()).toUpperCase(Locale.ROOT);
        if (!LOCK_STATUS_ACTIVE.equals(lockStatus) && !LOCK_STATUS_CONSUMED.equals(lockStatus)) {
            return false;
        }
        if (lock.getPlanId() != null && lock.getSourcePlanId() != null) {
            return !Objects.equals(lock.getPlanId(), lock.getSourcePlanId());
        }
        String sourcePlanNo = StrUtil.trim(lock.getSourcePlanNo());
        String targetPlanNo = StrUtil.trim(lock.getTargetPlanNo());
        if (StrUtil.isNotBlank(sourcePlanNo) && StrUtil.isNotBlank(targetPlanNo)) {
            return !StrUtil.equalsIgnoreCase(sourcePlanNo, targetPlanNo);
        }
        return true;
    }

    private boolean isDiscretePostPlan(HcPlanOrderDO plan) {
        if (plan == null) {
            return false;
        }
        return PLAN_MODE_DISCRETE_POST.equalsIgnoreCase(StrUtil.trimToEmpty(plan.getPlanMode()))
                || SOURCE_TYPE_DISCRETE_NG.equalsIgnoreCase(StrUtil.trimToEmpty(plan.getSourceType()))
                || SOURCE_TYPE_DISCRETE_WIP.equalsIgnoreCase(StrUtil.trimToEmpty(plan.getSourceType()));
    }

    private boolean isDiscretePostRow(HcPlanProcessPivotRespVO row) {
        if (row == null) {
            return false;
        }
        return PLAN_MODE_DISCRETE_POST.equalsIgnoreCase(StrUtil.trimToEmpty(row.getPlanMode()))
                || SOURCE_TYPE_DISCRETE_NG.equalsIgnoreCase(StrUtil.trimToEmpty(row.getSourceType()))
                || SOURCE_TYPE_DISCRETE_WIP.equalsIgnoreCase(StrUtil.trimToEmpty(row.getSourceType()));
    }

    private Map<Long, List<String>> selectPivotOperationStageCodesMap(Collection<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectList(
                new LambdaQueryWrapperX<HcPlanOrderOperationDO>()
                        .in(HcPlanOrderOperationDO::getPlanId, planIds)
                        .orderByAsc(HcPlanOrderOperationDO::getPlanId)
                        .orderByAsc(HcPlanOrderOperationDO::getOpSeq)
                        .orderByAsc(HcPlanOrderOperationDO::getSort)
                        .orderByAsc(HcPlanOrderOperationDO::getId));
        if (operations == null || operations.isEmpty()) {
            return Map.of();
        }
        Map<Long, LinkedHashSet<String>> stageCodesByPlanId = new LinkedHashMap<>();
        for (HcPlanOrderOperationDO operation : operations) {
            String stageCode = normalizePivotOperationStageCode(operation.getOpCode(), operation.getOpName());
            if (StrUtil.isBlank(stageCode) || operation.getPlanId() == null) {
                continue;
            }
            stageCodesByPlanId.computeIfAbsent(operation.getPlanId(), key -> new LinkedHashSet<>()).add(stageCode);
        }
        Map<Long, List<String>> result = new LinkedHashMap<>();
        stageCodesByPlanId.forEach((planId, stageCodes) -> result.put(planId, new ArrayList<>(stageCodes)));
        return result;
    }

    private String normalizePivotOperationStageCode(String operationCode, String operationName) {
        String opCode = StrUtil.trimToEmpty(operationCode).toUpperCase(Locale.ROOT).replace('-', '_');
        String opName = StrUtil.trimToEmpty(operationName);
        if ("FORMULA".equals(opCode) || opCode.contains("FORMULA")
                || opName.contains("配方") || opName.contains("配料")) {
            return "FORMULA";
        }
        if ("WET".equals(opCode) || opCode.contains("WET") || opName.contains("湿法")) {
            return "WET";
        }
        if (opCode.contains("GRINDING") || opName.contains("磨皮") || opName.contains("粗磨")
                || opName.contains("二磨")) {
            return "GRINDING";
        }
        if (opCode.contains("ADHESIVE2") || opName.contains("粘胶2")
                || opName.contains("粘双面胶") || opName.contains("背胶")) {
            return "ADHESIVE2";
        }
        if (opCode.contains("ADHESIVE1") || opName.contains("粘胶1")) {
            return "ADHESIVE1";
        }
        if (opCode.contains("SLITTING") || opCode.contains("SLIT") || opName.contains("分切")) {
            return "SLITTING";
        }
        if (opCode.contains("PRESS_SLOT") || opName.contains("压槽")) {
            return "PRESS_SLOT";
        }
        if (opCode.contains("CUT_ROUND") || opCode.contains("CUTROUND") || "CUT".equals(opCode)
                || opCode.endsWith("_CUT") || opName.contains("裁切") || opName.contains("裁圆")) {
            return "CUT_ROUND";
        }
        if (opCode.contains("SHIPPING_INSPECTION") || opName.contains("发货检验")) {
            return "SHIPPING_INSPECTION";
        }
        return null;
    }

    private PostProcessSourceInfo buildPostProcessSourceInfo(List<HcPlanOrderInventoryLockDO> locks,
                                                             Map<Long, HcInvStockDO> stockMap,
                                                             Map<Long, HcPlanOrderDO> sourcePlanMap,
                                                             Map<Long, List<HcProcessReportMapper.PlanProcessPivotStageRow>> sourceStageRowsMap) {
        Set<String> motherBatchNos = new LinkedHashSet<>();
        Set<String> grindingSegmentBatchNos = new LinkedHashSet<>();
        Set<Long> inheritedSourcePlanIds = new LinkedHashSet<>();
        Set<Long> qtimeSourcePlanIds = new LinkedHashSet<>();
        Set<String> inheritedGrindingSegmentKeys = new LinkedHashSet<>();
        Map<String, Long> sourcePlanIdsByGrindingSegment = new LinkedHashMap<>();
        Map<String, HcPlanProcessPivotStageRespVO> inheritedFrontStages = new LinkedHashMap<>();
        Map<String, HcPlanProcessPivotStageRespVO> inheritedGrindingStagesBySegment = new LinkedHashMap<>();
        for (HcPlanOrderInventoryLockDO lock : locks) {
            HcInvStockDO stock = lock.getStockId() == null ? null : stockMap.get(lock.getStockId());
            HcPlanOrderDO sourcePlan = lock.getSourcePlanId() == null ? null : sourcePlanMap.get(lock.getSourcePlanId());
            String grindingSegmentBatchNo = resolvePostProcessGrindingSegmentBatchNo(lock, stock);
            addDistinctText(motherBatchNos, resolvePostProcessMotherBatchNo(lock, stock, sourcePlan));
            addDistinctText(grindingSegmentBatchNos, grindingSegmentBatchNo);
            List<HcProcessReportMapper.PlanProcessPivotStageRow> sourceRows = lock.getSourcePlanId() == null
                    ? List.of() : sourceStageRowsMap.get(lock.getSourcePlanId());
            if (lock.getSourcePlanId() != null) {
                qtimeSourcePlanIds.add(lock.getSourcePlanId());
            }
            if (lock.getSourcePlanId() != null && inheritedSourcePlanIds.add(lock.getSourcePlanId())) {
                inheritPostProcessFrontStageRows(inheritedFrontStages, sourceRows);
            }
            String grindingSegmentKey = normalizePostProcessSegmentKey(grindingSegmentBatchNo);
            if (lock.getSourcePlanId() != null && StrUtil.isNotBlank(grindingSegmentKey)) {
                sourcePlanIdsByGrindingSegment.putIfAbsent(grindingSegmentKey, lock.getSourcePlanId());
            }
            if (lock.getSourcePlanId() != null && StrUtil.isNotBlank(grindingSegmentKey)
                    && inheritedGrindingSegmentKeys.add(lock.getSourcePlanId() + "|" + grindingSegmentKey)) {
                inheritPostProcessGrindingStageRows(inheritedGrindingStagesBySegment, sourceRows, grindingSegmentBatchNo);
            }
        }
        return new PostProcessSourceInfo(
                joinDistinctTexts(motherBatchNos),
                joinDistinctTexts(grindingSegmentBatchNos),
                qtimeSourcePlanIds.stream().findFirst().orElse(null),
                sourcePlanIdsByGrindingSegment,
                inheritedFrontStages,
                inheritedGrindingStagesBySegment);
    }

    private void inheritPostProcessFrontStageRows(
            Map<String, HcPlanProcessPivotStageRespVO> inheritedFrontStages,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> sourceRows) {
        if (sourceRows == null || sourceRows.isEmpty()) {
            return;
        }
        sourceRows.stream()
                .filter(row -> POST_PROCESS_INHERITED_FRONT_STAGE_CODES.contains(row.stageCode()))
                .forEach(row -> {
                    inheritedFrontStages.computeIfAbsent(row.stageCode(), this::createPivotStage);
                    mergePivotStage(inheritedFrontStages, row);
                    refreshStageStatus(inheritedFrontStages.get(row.stageCode()));
                });
    }

    private void inheritPostProcessGrindingStageRows(
            Map<String, HcPlanProcessPivotStageRespVO> inheritedGrindingStagesBySegment,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> sourceRows,
            String grindingSegmentBatchNo) {
        String segmentKey = normalizePostProcessSegmentKey(grindingSegmentBatchNo);
        if (StrUtil.isBlank(segmentKey) || sourceRows == null || sourceRows.isEmpty()) {
            return;
        }
        for (HcProcessReportMapper.PlanProcessPivotStageRow row : sourceRows) {
            if (!"GRINDING".equals(row.stageCode())) {
                continue;
            }
            String rowSegmentKey = normalizePostProcessSegmentKey(
                    firstNotBlank(row.segmentBatchNo(), row.outputBatchNos(), row.sourceBatchNos()));
            if (!StrUtil.equals(segmentKey, rowSegmentKey)) {
                continue;
            }
            HcPlanProcessPivotStageRespVO stage = inheritedGrindingStagesBySegment
                    .computeIfAbsent(segmentKey, key -> createPivotStage("GRINDING"));
            mergePivotStage(stage, row);
            refreshStageStatus(stage);
        }
    }

    private String resolvePostProcessMotherBatchNo(HcPlanOrderInventoryLockDO lock, HcInvStockDO stock,
                                                   HcPlanOrderDO sourcePlan) {
        return firstNotBlank(
                sourcePlan == null ? null : sourcePlan.getParentProductionBatchNo(),
                sourcePlan == null ? null : sourcePlan.getProductionBatchNo(),
                sourcePlan == null ? null : sourcePlan.getBatchNo(),
                stock == null ? null : normalizePostProcessMotherBatchNo(stock.getSourceParentBatchNo()),
                lock == null ? null : normalizePostProcessMotherBatchNo(lock.getSourceBatchNo()),
                lock == null ? null : normalizePostProcessMotherBatchNo(lock.getBatchNo()),
                lock == null ? null : normalizePostProcessMotherBatchNo(lock.getLotNo()));
    }

    private String resolvePostProcessGrindingSegmentBatchNo(HcPlanOrderInventoryLockDO lock, HcInvStockDO stock) {
        return firstNotBlank(
                normalizePostProcessSegmentBatchNo(lock.getBatchNo()),
                stock == null ? null : normalizePostProcessSegmentBatchNo(stock.getSourceParentBatchNo()),
                normalizePostProcessSegmentBatchNo(lock.getSourceBatchNo()),
                stock == null ? null : normalizePostProcessSegmentBatchNo(stock.getSourceBatchNo()),
                stock == null ? null : normalizePostProcessSegmentBatchNo(stock.getBatchNo()),
                normalizePostProcessSegmentBatchNo(lock.getLotNo()));
    }

    private String normalizePostProcessSegmentBatchNo(String batchNo) {
        String text = StrUtil.trimToEmpty(batchNo);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        if (text.matches("^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS]-J[0-9]+$")) {
            return text.substring(0, text.indexOf("-J"));
        }
        if (text.matches("^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9][A-Z]$")) {
            return text.substring(0, text.length() - 4);
        }
        if (text.matches("^[A-Z][0-9][0-9][A-Z][0-9][0-9][0-9][A-Z][PQRS][0-9][0-9][0-9]$")) {
            return text.substring(0, text.length() - 3);
        }
        return text;
    }

    private String normalizePostProcessMotherBatchNo(String batchNo) {
        String segmentBatchNo = normalizePostProcessSegmentBatchNo(batchNo);
        if (StrUtil.isBlank(segmentBatchNo)) {
            return null;
        }
        Matcher matcher = INVENTORY_MOTHER_BATCH_PREFIX_PATTERN.matcher(segmentBatchNo.toUpperCase(Locale.ROOT));
        return matcher.matches() ? matcher.group(1) : segmentBatchNo;
    }

    private String normalizePostProcessSegmentKey(String batchNo) {
        String normalized = normalizePostProcessSegmentBatchNo(batchNo);
        return StrUtil.isBlank(normalized) ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private void fillPostProcessPlanInfo(List<HcPlanProcessPivotRespVO> rows, PostProcessSourceInfo sourceInfo) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        boolean postProcessFlag = sourceInfo != null;
        rows.forEach(row -> {
            row.setPostProcessFlag(postProcessFlag);
            row.setPlanNoTagText(postProcessFlag ? "后加工" : null);
            if (!postProcessFlag) {
                return;
            }
            String sourceMotherBatchNo = firstNotBlank(sourceInfo.motherBatchNo(), sourceInfo.grindingSegmentBatchNo());
            if (StrUtil.isBlank(row.getMotherRollBatchNo())) {
                row.setMotherRollBatchNo(sourceMotherBatchNo);
            }
            if (StrUtil.isBlank(row.getSegmentBatchNo())) {
                row.setSegmentBatchNo(firstNotBlank(sourceInfo.grindingSegmentBatchNo(), sourceMotherBatchNo));
            }
            if (isDiscretePostRow(row)) {
                row.setSegmentBatchNo(DISCRETE_BATCH_PLACEHOLDER);
                if (row.getStages() != null) {
                    fillPivotSummary(row, row.getStages());
                }
                return;
            }
            fillPostProcessFrontStages(row, sourceInfo);
            fillPostProcessGrindingStage(row, sourceInfo);
            if (row.getStages() != null) {
                fillPivotSummary(row, row.getStages());
            }
        });
    }

    private void fillPlanProcessPivotQtime(
            List<HcPlanProcessPivotRespVO> rows,
            Map<Long, PostProcessSourceInfo> postProcessSourceInfoMap,
            Map<Long, Map<String, LocalDateTime>> stageStartTimeMap) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        for (HcPlanProcessPivotRespVO row : rows) {
            if (row.getStages() == null) {
                continue;
            }
            if (isDiscretePostRow(row)) {
                continue;
            }
            HcPlanProcessPivotStageRespVO formulaStage = row.getStages().get("FORMULA");
            HcPlanProcessPivotStageRespVO wetStage = row.getStages().get("WET");
            HcPlanProcessPivotStageRespVO grindingStage = row.getStages().get("GRINDING");
            HcPlanProcessPivotStageRespVO adhesiveStage = row.getStages().get("ADHESIVE1");
            PostProcessSourceInfo sourceInfo = postProcessSourceInfoMap == null ? null : postProcessSourceInfoMap.get(row.getId());
            Long formulaWetPlanId = sourceInfo != null && sourceInfo.primarySourcePlanId() != null
                    ? sourceInfo.primarySourcePlanId() : row.getId();
            if (formulaStage != null && wetStage != null) {
                LocalDateTime wetStartTime = getPivotStageStartTime(stageStartTimeMap, formulaWetPlanId, "WET", null);
                wetStage.setQtime(hcQtimeEvaluationService.evaluateFormulaToWet(
                        formulaStage.getLastReportTime(), wetStartTime,
                        buildPivotQtimeCandidates(row, wetStage)));
            }
            if (wetStage != null && grindingStage != null) {
                Long grindingPlanId = resolvePivotGrindingQtimePlanId(row, sourceInfo);
                LocalDateTime grindingStartTime = getPivotStageStartTime(
                        stageStartTimeMap, grindingPlanId, "GRINDING", row.getSegmentBatchNo());
                grindingStage.setQtime(hcQtimeEvaluationService.evaluateWetToGrinding(
                        wetStage.getLastReportTime(), grindingStartTime,
                        buildPivotQtimeCandidates(row, grindingStage)));
            }
            if (grindingStage != null && adhesiveStage != null) {
                LocalDateTime adhesiveStartTime = getPivotStageStartTime(
                        stageStartTimeMap, row.getId(), "ADHESIVE1", row.getSegmentBatchNo());
                adhesiveStage.setQtime(hcQtimeEvaluationService.evaluateGrindingToAdhesive(
                        grindingStage.getLastReportTime(), adhesiveStartTime,
                        buildPivotQtimeCandidates(row, adhesiveStage)));
            }
        }
    }

    private Long resolvePivotGrindingQtimePlanId(HcPlanProcessPivotRespVO row, PostProcessSourceInfo sourceInfo) {
        if (sourceInfo == null) {
            return row.getId();
        }
        String segmentKey = normalizePostProcessSegmentKey(row.getSegmentBatchNo());
        if (StrUtil.isNotBlank(segmentKey)) {
            Long segmentSourcePlanId = sourceInfo.sourcePlanIdsByGrindingSegment().get(segmentKey);
            if (segmentSourcePlanId != null) {
                return segmentSourcePlanId;
            }
        }
        if (sourceInfo.sourcePlanIdsByGrindingSegment().size() == 1) {
            return sourceInfo.sourcePlanIdsByGrindingSegment().values().iterator().next();
        }
        return sourceInfo.primarySourcePlanId() == null ? row.getId() : sourceInfo.primarySourcePlanId();
    }

    private LocalDateTime getPivotStageStartTime(
            Map<Long, Map<String, LocalDateTime>> stageStartTimeMap,
            Long planId,
            String stageCode,
            String segmentBatchNo) {
        if (stageStartTimeMap == null || planId == null || StrUtil.isBlank(stageCode)) {
            return null;
        }
        Map<String, LocalDateTime> startTimes = stageStartTimeMap.get(planId);
        if (startTimes == null || startTimes.isEmpty()) {
            return null;
        }
        return startTimes.get(buildPivotStageStartKey(stageCode, segmentBatchNo));
    }

    private String buildPivotStageStartKey(String stageCode, String segmentBatchNo) {
        if ("GRINDING".equals(stageCode) || "ADHESIVE1".equals(stageCode) || "SLITTING".equals(stageCode)) {
            return stageCode + "|" + StrUtil.blankToDefault(normalizePostProcessSegmentKey(segmentBatchNo), "");
        }
        return stageCode;
    }

    private String[] buildPivotQtimeCandidates(
            HcPlanProcessPivotRespVO row,
            HcPlanProcessPivotStageRespVO stage) {
        return new String[]{
                row.getActualModelCode(),
                row.getModelCode(),
                row.getModelName(),
                row.getMotherModelCode(),
                row.getMotherModelName(),
                row.getSegmentBatchNo(),
                row.getMotherRollBatchNo(),
                row.getProductionBatchNo(),
                row.getParentProductionBatchNo(),
                row.getBatchNo(),
                stage == null ? null : stage.getSourceBatchNos(),
                stage == null ? null : stage.getOutputBatchNos()
        };
    }

    private void fillPostProcessFrontStages(HcPlanProcessPivotRespVO row, PostProcessSourceInfo sourceInfo) {
        if (row.getStages() == null || sourceInfo == null || sourceInfo.inheritedFrontStages().isEmpty()) {
            return;
        }
        for (String stageCode : POST_PROCESS_INHERITED_FRONT_STAGE_CODES) {
            HcPlanProcessPivotStageRespVO sourceStage = sourceInfo.inheritedFrontStages().get(stageCode);
            HcPlanProcessPivotStageRespVO targetStage = row.getStages().get(stageCode);
            if (sourceStage == null || targetStage == null || hasActualStageProgress(targetStage)) {
                continue;
            }
            copyPostProcessInheritedStage(targetStage, sourceStage);
            refreshStageStatus(targetStage);
        }
    }

    private boolean hasActualStageProgress(HcPlanProcessPivotStageRespVO stage) {
        return StrUtil.isNotBlank(stage.getSourceBatchNos())
                || StrUtil.isNotBlank(stage.getOutputBatchNos())
                || stage.getLastReportTime() != null
                || positive(stage.getInputQty())
                || positive(stage.getReportQty())
                || positive(stage.getDoneQty())
                || positive(stage.getDefectQty())
                || positive(stage.getInspectionQty())
                || positive(stage.getInspectionNgQty())
                || positive(stage.getConfirmedQty())
                || positive(stage.getLengthQty());
    }

    private boolean hasActualStageQuantityProgress(HcPlanProcessPivotStageRespVO stage) {
        return stage.getLastReportTime() != null
                || stage.getStartPosition() != null
                || positive(stage.getInputQty())
                || positive(stage.getReportQty())
                || positive(stage.getDoneQty())
                || positive(stage.getDefectQty())
                || positive(stage.getInspectionQty())
                || positive(stage.getInspectionNgQty())
                || positive(stage.getConfirmedQty())
                || positive(stage.getLengthQty())
                || positive(stage.getProcessLength());
    }

    private void copyPostProcessInheritedStage(
            HcPlanProcessPivotStageRespVO targetStage,
            HcPlanProcessPivotStageRespVO sourceStage) {
        targetStage.setSourceBatchNos(sourceStage.getSourceBatchNos());
        targetStage.setOutputBatchNos(sourceStage.getOutputBatchNos());
        targetStage.setInputQty(zeroIfNull(sourceStage.getInputQty()));
        targetStage.setReportQty(zeroIfNull(sourceStage.getReportQty()));
        targetStage.setDoneQty(zeroIfNull(sourceStage.getDoneQty()));
        targetStage.setPendingQty(zeroIfNull(sourceStage.getPendingQty()));
        targetStage.setPendingUnit(sourceStage.getPendingUnit());
        targetStage.setDefectQty(zeroIfNull(sourceStage.getDefectQty()));
        targetStage.setInspectionQty(zeroIfNull(sourceStage.getInspectionQty()));
        targetStage.setInspectionNgQty(zeroIfNull(sourceStage.getInspectionNgQty()));
        targetStage.setConfirmedQty(zeroIfNull(sourceStage.getConfirmedQty()));
        targetStage.setLengthQty(zeroIfNull(sourceStage.getLengthQty()));
        targetStage.setLengthUnit(sourceStage.getLengthUnit());
        targetStage.setReportUnit(sourceStage.getReportUnit());
        targetStage.setLastReportTime(sourceStage.getLastReportTime());
        targetStage.setRemark(sourceStage.getRemark());
    }

    private void fillPostProcessGrindingStage(HcPlanProcessPivotRespVO row, PostProcessSourceInfo sourceInfo) {
        if (row.getStages() == null || sourceInfo == null) {
            return;
        }
        HcPlanProcessPivotStageRespVO grindingStage = row.getStages().get("GRINDING");
        if (grindingStage == null) {
            return;
        }
        String segmentBatchNo = normalizePostProcessSegmentBatchNo(row.getSegmentBatchNo());
        HcPlanProcessPivotStageRespVO sourceStage = sourceInfo.inheritedGrindingStagesBySegment()
                .get(normalizePostProcessSegmentKey(segmentBatchNo));
        if (sourceStage == null && StrUtil.isBlank(segmentBatchNo)
                && sourceInfo.inheritedGrindingStagesBySegment().size() == 1) {
            sourceStage = sourceInfo.inheritedGrindingStagesBySegment().values().iterator().next();
        }
        if (sourceStage != null && !hasActualStageQuantityProgress(grindingStage)) {
            copyPostProcessInheritedStage(grindingStage, sourceStage);
            refreshStageStatus(grindingStage);
        }
        if (StrUtil.isBlank(grindingStage.getSourceBatchNos())) {
            grindingStage.setSourceBatchNos(sourceInfo.motherBatchNo());
        }
        if (StrUtil.isBlank(grindingStage.getOutputBatchNos())) {
            grindingStage.setOutputBatchNos(firstNotBlank(segmentBatchNo, sourceInfo.grindingSegmentBatchNo()));
        }
    }

    private void addDistinctText(Set<String> target, String value) {
        String text = StrUtil.trim(value);
        if (StrUtil.isNotBlank(text)) {
            target.add(text);
        }
    }

    private String joinDistinctTexts(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join("，", values);
    }

    @Override
    public PageResult<HcPlanOrderWipCandidateRespVO> getWipCandidatePage(
            HcPlanOrderWipCandidatePageReqVO pageReqVO) {
        HcInvStockPageReqVO stockReqVO = new HcInvStockPageReqVO();
        stockReqVO.setPageNo(pageReqVO.getPageNo());
        stockReqVO.setPageSize(pageReqVO.getPageSize());
        stockReqVO.setStockType(STOCK_TYPE_WIP);
        stockReqVO.setSourcePlanNo(StrUtil.trimToNull(pageReqVO.getSourcePlanNo()));
        stockReqVO.setSourceBatchNo(StrUtil.trimToNull(pageReqVO.getSourceBatchNo()));
        stockReqVO.setSourceParentBatchNo(StrUtil.trimToNull(pageReqVO.getSourceParentBatchNo()));
        stockReqVO.setBatchNo(StrUtil.trimToNull(pageReqVO.getBatchNo()));
        stockReqVO.setMaterialCode(StrUtil.trimToNull(pageReqVO.getMaterialCode()));
        stockReqVO.setModelNo(StrUtil.trimToNull(pageReqVO.getModelNo()));
        stockReqVO.setQualityStatus(StrUtil.trimToNull(pageReqVO.getQualityStatus()));
        stockReqVO.setOpSeq(resolveWipCandidateSourceOpSeq(pageReqVO));
        stockReqVO.setIncludeUnavailable(false);
        stockReqVO.setOnlyShareable(true);
        stockReqVO.setExcludeSourceTypes(WIP_CANDIDATE_EXCLUDED_SOURCE_TYPES);
        stockReqVO.setExcludeOpCodes(WIP_CANDIDATE_EXCLUDED_OP_CODES);
        stockReqVO.setExcludeOpNameKeywords(WIP_CANDIDATE_EXCLUDED_OP_NAME_KEYWORDS);

        PageResult<HcInvStockDO> stockPage = hcInvStockService.getInvStockPage(stockReqVO);
        List<HcInvStockDO> stockList = stockPage.getList();
        if (stockList == null || stockList.isEmpty()) {
            return new PageResult<>(List.of(), stockPage.getTotal());
        }
        return new PageResult<>(buildWipCandidateRespList(stockList), stockPage.getTotal());
    }

    private Integer resolveWipCandidateSourceOpSeq(HcPlanOrderWipCandidatePageReqVO reqVO) {
        if (reqVO.getSourceOpSeq() != null) {
            return reqVO.getSourceOpSeq();
        }
        if (reqVO.getTargetPlanId() == null) {
            return null;
        }
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(reqVO.getTargetPlanId());
        if (operations == null || operations.isEmpty()) {
            return null;
        }
        Integer targetOpSeq = reqVO.getTargetOpSeq();
        if (reqVO.getTargetOperationId() != null) {
            targetOpSeq = operations.stream()
                    .filter(item -> Objects.equals(item.getId(), reqVO.getTargetOperationId()))
                    .map(HcPlanOrderOperationDO::getOpSeq)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(targetOpSeq);
        }
        if (targetOpSeq == null && StrUtil.isNotBlank(reqVO.getTargetOpCode())) {
            targetOpSeq = operations.stream()
                    .filter(item -> StrUtil.equals(reqVO.getTargetOpCode(), item.getOpCode()))
                    .map(HcPlanOrderOperationDO::getOpSeq)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
        }
        if (targetOpSeq == null) {
            return null;
        }
        final Integer resolvedTargetOpSeq = targetOpSeq;
        return operations.stream()
                .map(HcPlanOrderOperationDO::getOpSeq)
                .filter(Objects::nonNull)
                .filter(opSeq -> opSeq < resolvedTargetOpSeq)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private List<HcPlanOrderWipCandidateRespVO> buildWipCandidateRespList(List<HcInvStockDO> stockList) {
        Set<Long> stockIds = stockList.stream()
                .map(HcInvStockDO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, List<HcPlanOrderInventoryLockDO>> lockMap = stockIds.isEmpty()
                ? Map.<Long, List<HcPlanOrderInventoryLockDO>>of()
                : hcPlanOrderInventoryLockMapper.selectListByStockIds(stockIds).stream()
                        .collect(Collectors.groupingBy(HcPlanOrderInventoryLockDO::getStockId,
                                LinkedHashMap::new, Collectors.toList()));
        Map<String, HcProcessParamRecordDO> thicknessParamMap = buildWipThicknessParamMap(stockList);
        return stockList.stream()
                .map(stock -> buildWipCandidateResp(stock, lockMap.get(stock.getId()), thicknessParamMap))
                .toList();
    }

    private HcPlanOrderWipCandidateRespVO buildWipCandidateResp(HcInvStockDO stock,
                                                                List<HcPlanOrderInventoryLockDO> locks,
                                                                Map<String, HcProcessParamRecordDO> thicknessParamMap) {
        HcPlanOrderWipCandidateRespVO respVO = BeanUtils.toBean(stock, HcPlanOrderWipCandidateRespVO.class);
        fillWipCandidateThickness(respVO, stock, thicknessParamMap);
        List<HcPlanOrderInventoryLockDO> stockLocks = locks == null ? List.of() : locks;
        BigDecimal lockedQty = stockLocks.stream()
                .map(HcPlanOrderInventoryLockDO::getLockQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal consumedQty = stockLocks.stream()
                .map(HcPlanOrderInventoryLockDO::getConsumedQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal releasedQty = stockLocks.stream()
                .map(HcPlanOrderInventoryLockDO::getReleasedQty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal lockRemainingQty = stockLocks.stream()
                .map(this::calculateRemainingLockQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        HcPlanOrderInventoryLockDO activeLock = stockLocks.stream()
                .filter(this::isActiveWipLock)
                .findFirst()
                .orElse(null);

        respVO.setStockStatus(resolveWipCandidateStockStatus(stock));
        respVO.setLockedQty(lockedQty);
        respVO.setConsumedQty(consumedQty);
        respVO.setReleasedQty(releasedQty);
        respVO.setLockRemainingQty(lockRemainingQty);
        if (activeLock != null) {
            respVO.setActiveLockId(activeLock.getId());
            respVO.setActiveLockStatus(firstNotBlank(activeLock.getLockStatus(), LOCK_STATUS_ACTIVE));
            respVO.setActiveLockTargetPlanNo(activeLock.getTargetPlanNo());
            respVO.setActiveLockTargetOpName(firstNotBlank(activeLock.getTargetOpName(), activeLock.getTargetOpCode()));
            respVO.setActiveLockRemainingQty(calculateRemainingLockQty(activeLock));
        }
        respVO.setTxnSummary(buildWipCandidateTxnSummary(respVO));
        return respVO;
    }

    private Map<String, HcProcessParamRecordDO> buildWipThicknessParamMap(List<HcInvStockDO> stockList) {
        Set<String> batchNos = new HashSet<>();
        Set<String> processCodes = new HashSet<>();
        for (HcInvStockDO stock : stockList) {
            addNotBlank(batchNos, stock.getBatchNo());
            addNotBlank(batchNos, stock.getSourceBatchNo());
            addNotBlank(processCodes, stock.getOpCode());
            addNotBlank(processCodes, stock.getSourceType());
        }
        List<HcProcessParamRecordDO> records = hcProcessParamRecordService.listByBatchNosAndProcessCodes(
                batchNos, processCodes, HcProcessParamRecordService.PARAM_CODE_THICKNESS);
        Map<String, HcProcessParamRecordDO> result = new LinkedHashMap<>();
        for (HcProcessParamRecordDO record : records) {
            String key = buildWipThicknessParamKey(record.getTenantId(), record.getBatchNo(), record.getProcessCode());
            result.putIfAbsent(key, record);
        }
        return result;
    }

    private void fillWipCandidateThickness(HcPlanOrderWipCandidateRespVO respVO,
                                           HcInvStockDO stock,
                                           Map<String, HcProcessParamRecordDO> thicknessParamMap) {
        HcProcessParamRecordDO param = findWipThicknessParam(stock, thicknessParamMap);
        if (param == null) {
            return;
        }
        BigDecimal thickness = param.getParamValueNum() != null
                ? param.getParamValueNum()
                : parseDecimal(param.getParamValue());
        if (thickness != null) {
            respVO.setThickness(thickness);
        }
    }

    private HcProcessParamRecordDO findWipThicknessParam(HcInvStockDO stock,
                                                        Map<String, HcProcessParamRecordDO> thicknessParamMap) {
        HcProcessParamRecordDO param = thicknessParamMap.get(buildWipThicknessParamKey(
                stock.getTenantId(), stock.getBatchNo(), stock.getOpCode()));
        if (param != null) {
            return param;
        }
        param = thicknessParamMap.get(buildWipThicknessParamKey(
                stock.getTenantId(), stock.getSourceBatchNo(), stock.getOpCode()));
        if (param != null) {
            return param;
        }
        param = thicknessParamMap.get(buildWipThicknessParamKey(
                stock.getTenantId(), stock.getBatchNo(), stock.getSourceType()));
        if (param != null) {
            return param;
        }
        return thicknessParamMap.get(buildWipThicknessParamKey(
                stock.getTenantId(), stock.getSourceBatchNo(), stock.getSourceType()));
    }

    private String buildWipThicknessParamKey(Long tenantId, String batchNo, String processCode) {
        return tenantId + "#" + StrUtil.trimToEmpty(batchNo) + "#" + StrUtil.trimToEmpty(processCode);
    }

    private void addNotBlank(Set<String> values, String value) {
        String trimmed = StrUtil.trim(value);
        if (StrUtil.isNotBlank(trimmed)) {
            values.add(trimmed);
        }
    }

    private BigDecimal parseDecimal(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String resolveWipCandidateStockStatus(HcInvStockDO stock) {
        if (defaultDecimal(stock.getOnHandQty()).compareTo(BigDecimal.ZERO) <= 0) {
            return "CONSUMED";
        }
        if (defaultDecimal(stock.getShareableQty()).compareTo(BigDecimal.ZERO) > 0) {
            return "AVAILABLE";
        }
        if (defaultDecimal(stock.getAvailableQty()).compareTo(BigDecimal.ZERO) > 0) {
            return "AVAILABLE";
        }
        if (defaultDecimal(stock.getPlanLockedQty()).compareTo(BigDecimal.ZERO) > 0) {
            return "LOCKED";
        }
        if (defaultDecimal(stock.getFrozenQty()).compareTo(BigDecimal.ZERO) > 0) {
            return "FROZEN";
        }
        return "AVAILABLE";
    }

    private String buildWipCandidateTxnSummary(HcPlanOrderWipCandidateRespVO row) {
        List<String> parts = new ArrayList<>();
        parts.add("可用量 " + formatCandidateQty(row.getShareableQty(), row.getUom()));
        parts.add("可用 " + formatCandidateQty(row.getAvailableQty(), row.getUom()));
        if (defaultDecimal(row.getPlanLockedQty()).compareTo(BigDecimal.ZERO) > 0) {
            parts.add("计划锁定 " + formatCandidateQty(row.getPlanLockedQty(), row.getUom()));
        }
        if (defaultDecimal(row.getConsumedQty()).compareTo(BigDecimal.ZERO) > 0) {
            parts.add("已消耗 " + formatCandidateQty(row.getConsumedQty(), row.getUom()));
        }
        if (defaultDecimal(row.getReleasedQty()).compareTo(BigDecimal.ZERO) > 0) {
            parts.add("已释放 " + formatCandidateQty(row.getReleasedQty(), row.getUom()));
        }
        if (defaultDecimal(row.getLockRemainingQty()).compareTo(BigDecimal.ZERO) > 0) {
            parts.add("锁定剩余 " + formatCandidateQty(row.getLockRemainingQty(), row.getUom()));
        }
        if (StrUtil.isNotBlank(row.getActiveLockTargetPlanNo())) {
            parts.add("活动锁 " + row.getActiveLockTargetPlanNo()
                    + "/" + firstNotBlank(row.getActiveLockTargetOpName(), "-"));
        }
        return String.join("；", parts);
    }

    private String formatCandidateQty(BigDecimal qty, String uom) {
        String value = defaultDecimal(qty).stripTrailingZeros().toPlainString();
        return StrUtil.isBlank(uom) ? value : value + " " + uom;
    }

    @Override
    public List<HcPlanOrderOperationDO> getOperationListByPlanId(Long planId) {
        return hcPlanOrderOperationMapper.selectListByPlanId(planId);
    }

    @Override
    public Map<Long, Map<String, BigDecimal>> getOperationDailyReportQtyMap(Long planId) {
        Map<Long, Map<String, BigDecimal>> result = new LinkedHashMap<>();
        hcProcessReportMapper.selectOperationDailyGoodQtyByPlanId(planId).forEach(item -> {
            if (item.planOperationId() == null || item.reportDate() == null) {
                return;
            }
            result.computeIfAbsent(item.planOperationId(), key -> new LinkedHashMap<>())
                    .put(item.reportDate().toString(), item.goodQty() == null ? BigDecimal.ZERO : item.goodQty());
        });
        return result;
    }

    @Override
    public Map<Long, HcProcessReportMapper.OperationLatestReportRow> getOperationLatestReportMap(Long planId) {
        Map<Long, HcProcessReportMapper.OperationLatestReportRow> result = new LinkedHashMap<>();
        hcProcessReportMapper.selectOperationLatestReportByPlanId(planId).forEach(item -> {
            if (item.planOperationId() == null) {
                return;
            }
            result.put(item.planOperationId(), item);
        });
        return result;
    }

    @Override
    public List<HcPlanOrderInventoryLockDO> getInventoryLockListByPlanId(Long planId) {
        return hcPlanOrderInventoryLockMapper.selectListByPlanId(planId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateOperationStatus(HcPlanOrderOperationStatusReqVO reqVO) {
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectBatchIds(reqVO.getOperationIds());
        if (operations.size() != reqVO.getOperationIds().size()) {
            throw invalidParamException("存在无效的计划工序");
        }
        LocalDateTime operateTime = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        for (HcPlanOrderOperationDO operation : operations) {
            String fromStatus = firstNotBlank(operation.getOperationStatus(), OP_STATUS_NOT_RELEASED);
            String toStatus = resolveNextOperationStatus(reqVO, fromStatus);
            HcPlanOrderOperationDO updateObj = new HcPlanOrderOperationDO();
            updateObj.setId(operation.getId());
            updateObj.setOperationStatus(toStatus);
            updateObj.setStatusOperatorId(operatorId);
            updateObj.setStatusOperatorName(operatorName);
            updateObj.setStatusOperateTime(operateTime);
            if (OP_STATUS_FINISHED.equals(toStatus)) {
                updateObj.setFinishTime(reqVO.getFinishTime() == null ? operateTime : reqVO.getFinishTime());
                updateObj.setFinishRemark(reqVO.getReasonRemark());
            } else if (OP_STATUS_PAUSED.equals(toStatus)) {
                updateObj.setPauseScope(firstNotBlank(reqVO.getPauseScope(), "DATES"));
                List<LocalDate> statusDates = normalizeStatusDates(reqVO);
                updateObj.setPauseStartDate(statusDates.isEmpty() ? null : statusDates.get(0));
                updateObj.setPauseEndDate(statusDates.isEmpty() ? null : statusDates.get(statusDates.size() - 1));
                updateObj.setPauseRemark(reqVO.getReasonRemark());
                updateObj.setStatusDateMarksJson(mergeStatusDateMarks(
                        operation.getStatusDateMarksJson(), statusDates, "PAUSE",
                        reqVO.getReasonRemark(), operatorName, operateTime));
            } else if (OP_STATUS_CANCELLED.equals(toStatus)) {
                updateObj.setCancelReason(reqVO.getReasonRemark());
            } else if (OP_STATUS_RUNNING.equals(toStatus) && "RESUME".equals(reqVO.getActionType())) {
                updateObj.setPauseRemark(reqVO.getReasonRemark());
                updateObj.setStatusDateMarksJson(mergeStatusDateMarks(
                        operation.getStatusDateMarksJson(), List.of(reqVO.getResumeDate()), "RESUME",
                        reqVO.getReasonRemark(), operatorName, operateTime));
            }
            hcPlanOrderOperationMapper.updateById(updateObj);

            HcPlanOrderOperationStatusLogDO logDO = new HcPlanOrderOperationStatusLogDO();
            logDO.setPlanId(operation.getPlanId());
            logDO.setPlanOperationId(operation.getId());
            logDO.setActionType(reqVO.getActionType());
            logDO.setFromStatus(fromStatus);
            logDO.setToStatus(toStatus);
            logDO.setPauseScope(reqVO.getPauseScope());
            logDO.setPauseStartDate(reqVO.getPauseStartDate());
            logDO.setPauseEndDate(reqVO.getPauseEndDate());
            logDO.setFinishTime(updateObj.getFinishTime());
            logDO.setReasonRemark(reqVO.getReasonRemark());
            logDO.setStatusDatesJson(buildStatusDatesJson(reqVO));
            logDO.setOperatorId(operatorId);
            logDO.setOperatorName(operatorName);
            logDO.setOperateTime(operateTime);
            hcPlanOrderOperationStatusLogMapper.insert(logDO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePlanStatus(HcPlanOrderStatusReqVO reqVO) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectByIdForUpdate(reqVO.getId());
        if (planOrder == null) {
            throw exception(HCPLANORDER_NOT_EXISTS);
        }
        String fromStatus = firstNotBlank(planOrder.getPlanStatus(), STATUS_DRAFT);
        String toStatus = normalizePlanStatus(reqVO.getPlanStatus());
        if (Objects.equals(fromStatus, toStatus)) {
            throw invalidParamException("目标状态与当前状态一致，无需重复设置");
        }
        if (STATUS_DRAFT.equals(toStatus) && STATUS_RELEASED.equals(fromStatus)) {
            withdrawPlanOrder(planOrder.getId());
            return;
        }
        if (STATUS_RELEASED.equals(toStatus) && STATUS_DRAFT.equals(fromStatus)) {
            assertPlanNotStarted(planOrder);
            if (HcRootBatchReservation.isReserved(planOrder)) resetWithdrawnPlan(planOrder);
            for (HcPlanOrderInventoryLockDO lock : hcPlanOrderInventoryLockMapper.selectListByPlanId(planOrder.getId())) {
                if (!"PENDING".equals(lock.getLockStatus())) continue;
                resetInventoryLockLifecycle(lock);
                hcPlanOrderInventoryLockMapper.updateById(lock);
                lockInventoryLocks(List.of(lock), "重新下发计划，重新锁定库存");
            }
        }
        LocalDateTime operateTime = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system");

        HcPlanOrderDO updateObj = new HcPlanOrderDO();
        updateObj.setId(planOrder.getId());
        updateObj.setPlanStatus(toStatus);
        updateObj.setStatusOperatorId(operatorId);
        updateObj.setStatusOperatorName(operatorName);
        updateObj.setStatusOperateTime(operateTime);
        updateObj.setStatusRemark(reqVO.getReasonRemark());
        if (STATUS_RELEASED.equals(toStatus)) {
            updateObj.setReleasedAt(operateTime);
        }
        hcPlanOrderMapper.updateById(updateObj);
        hcProductionBatchService.reserveRootBatchOnPlanRelease(planOrder.getId());

        syncPlanOperationStatusAfterPlanStatusChange(planOrder.getId(), toStatus, reqVO.getReasonRemark(),
                operateTime, operatorId, operatorName);
        if (STATUS_CANCELLED.equals(toStatus)) {
            releaseInventoryLocks(hcPlanOrderInventoryLockMapper.selectListByPlanId(planOrder.getId()),
                    "生产计划作废取消释放挂接中间边库");
        }

        HcPlanOrderStatusLogDO logDO = new HcPlanOrderStatusLogDO();
        logDO.setPlanId(planOrder.getId());
        logDO.setPlanNo(planOrder.getPlanNo());
        logDO.setActionType(resolvePlanStatusAction(toStatus));
        logDO.setFromStatus(fromStatus);
        logDO.setToStatus(toStatus);
        logDO.setReasonRemark(reqVO.getReasonRemark());
        logDO.setOperatorId(operatorId);
        logDO.setOperatorName(operatorName);
        logDO.setOperateTime(operateTime);
        hcPlanOrderStatusLogMapper.insert(logDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawPlanOrder(Long id) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectByIdForUpdate(id);
        if (planOrder == null) throw exception(HCPLANORDER_NOT_EXISTS);
        String fromStatus = firstNotBlank(planOrder.getPlanStatus(), STATUS_DRAFT);
        if (!STATUS_RELEASED.equals(fromStatus)) {
            throw invalidParamException("只有已下达状态的生产计划才能撤回");
        }
        assertPlanNotStarted(planOrder);
        resetWithdrawnPlan(planOrder);

        LocalDateTime operateTime = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        String reasonRemark = "撤回计划";

        HcPlanOrderDO updateObj = new HcPlanOrderDO();
        updateObj.setId(planOrder.getId());
        updateObj.setPlanStatus(STATUS_DRAFT);
        updateObj.setStatusOperatorId(operatorId);
        updateObj.setStatusOperatorName(operatorName);
        updateObj.setStatusOperateTime(operateTime);
        updateObj.setStatusRemark(reasonRemark);
        hcPlanOrderMapper.updateById(updateObj);

        syncPlanOperationStatusAfterPlanStatusChange(planOrder.getId(), STATUS_DRAFT, reasonRemark,
                operateTime, operatorId, operatorName);

        HcPlanOrderStatusLogDO logDO = new HcPlanOrderStatusLogDO();
        logDO.setPlanId(planOrder.getId());
        logDO.setPlanNo(planOrder.getPlanNo());
        logDO.setActionType("WITHDRAW");
        logDO.setFromStatus(fromStatus);
        logDO.setToStatus(STATUS_DRAFT);
        logDO.setReasonRemark(reasonRemark);
        logDO.setOperatorId(operatorId);
        logDO.setOperatorName(operatorName);
        logDO.setOperateTime(operateTime);
        hcPlanOrderStatusLogMapper.insert(logDO);
    }

    /** 持有计划行锁后检查真实执行证据，不能只依赖页面状态。 */
    private void assertPlanNotStarted(HcPlanOrderDO plan) {
        boolean executed = hcPlanOrderOperationMapper.selectListByPlanId(plan.getId()).stream()
                .anyMatch(op -> OP_STATUS_RUNNING.equals(op.getOperationStatus())
                        || OP_STATUS_FINISHED.equals(op.getOperationStatus()) || op.getFinishTime() != null
                        || StrUtil.isNotBlank(op.getProductionBatchNo()));
        Long lots = hcLotInstanceMapper.selectCount(new LambdaQueryWrapperX<HcLotInstanceDO>()
                .eq(HcLotInstanceDO::getPlanId, plan.getId()));
        Long startedLogs = hcPlanOrderOperationStatusLogMapper.selectCount(
                new LambdaQueryWrapperX<HcPlanOrderOperationStatusLogDO>()
                        .eq(HcPlanOrderOperationStatusLogDO::getPlanId, plan.getId())
                        .in(HcPlanOrderOperationStatusLogDO::getToStatus, OP_STATUS_RUNNING, OP_STATUS_FINISHED));
        // 分切没有独立“开工”按钮，初始化切片即进入执行，不能留下旧路线的片任务。
        Long slices = hcSlittingSliceRecordMapper.selectCount(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getPlanId, plan.getId()));
        boolean consumed = hcPlanOrderInventoryLockMapper.selectListByPlanId(plan.getId()).stream()
                .anyMatch(lock -> defaultDecimal(lock.getConsumedQty()).signum() > 0);
        if (executed || hasAnyOperationReport(plan.getId()) || consumed || (slices != null && slices > 0)
                || (lots != null && lots > 0) || (startedLogs != null && startedLogs > 0)
                || StrUtil.isNotBlank(plan.getProductionBatchNo())
                || BATCH_STATUS_GENERATED.equalsIgnoreCase(plan.getBatchStatus())) {
            throw invalidParamException("已有工序执行、初始化切片、报工或实际消耗，不能撤回或重新排产");
        }
    }

    private void resetWithdrawnPlan(HcPlanOrderDO plan) {
        String context = HcRootBatchReservation.withdrawnContext(plan);
        // updateById 默认忽略 null；这里必须显式清除旧预约，避免回载后沿用。
        hcPlanOrderMapper.update(null, new LambdaUpdateWrapper<HcPlanOrderDO>()
                .eq(HcPlanOrderDO::getId, plan.getId())
                .set(HcPlanOrderDO::getBatchNo, null)
                .set(HcPlanOrderDO::getBatchRuleId, null)
                .set(HcPlanOrderDO::getBatchRuleCode, null)
                .set(HcPlanOrderDO::getBatchRuleVersion, null)
                .set(HcPlanOrderDO::getReleasedAt, null)
                .set(HcPlanOrderDO::getBatchStatus, "NOT_GEN")
                .set(HcPlanOrderDO::getProductionBatchContextJson, context));
        plan.setBatchNo(null);
        plan.setBatchRuleId(null);
        plan.setBatchRuleCode(null);
        plan.setBatchRuleVersion(null);
        plan.setProductionBatchContextJson(context);
        List<HcPlanOrderInventoryLockDO> locks = hcPlanOrderInventoryLockMapper.selectListByPlanId(plan.getId());
        releaseInventoryLocks(locks, "计划撤回，释放库存占用，重新下发时重新校验");
        for (HcPlanOrderInventoryLockDO lock : locks) {
            if (!LOCK_STATUS_ACTIVE.equals(firstNotBlank(lock.getLockStatus(), LOCK_STATUS_ACTIVE)) && !"PENDING".equals(lock.getLockStatus())) continue;
            resetInventoryLockLifecycle(lock);
            lock.setLockStatus("PENDING");
            persistPendingInventoryLock(lock);
        }
    }

    private void persistPendingInventoryLock(HcPlanOrderInventoryLockDO lock) {
        hcPlanOrderInventoryLockMapper.update(null, new LambdaUpdateWrapper<HcPlanOrderInventoryLockDO>()
                .eq(HcPlanOrderInventoryLockDO::getId, lock.getId())
                .set(HcPlanOrderInventoryLockDO::getLockStatus, "PENDING")
                .set(HcPlanOrderInventoryLockDO::getConsumedQty, BigDecimal.ZERO)
                .set(HcPlanOrderInventoryLockDO::getReleasedQty, BigDecimal.ZERO)
                .set(HcPlanOrderInventoryLockDO::getRemainingQty, lock.getLockQty())
                .set(HcPlanOrderInventoryLockDO::getLockTxnNo, null)
                .set(HcPlanOrderInventoryLockDO::getConsumeTxnNo, null)
                .set(HcPlanOrderInventoryLockDO::getReleaseTxnNo, null)
                .set(HcPlanOrderInventoryLockDO::getReleaseTime, null)
                .set(HcPlanOrderInventoryLockDO::getReleaseReason, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseInventoryLock(HcPlanOrderInventoryLockReleaseReqVO reqVO) {
        HcPlanOrderInventoryLockDO lock = hcPlanOrderInventoryLockMapper.selectById(reqVO.getLockId());
        if (lock == null) {
            throw invalidParamException("计划利库挂接记录不存在，无法释放");
        }
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(lock.getPlanId());
        if (planOrder == null) {
            throw exception(HCPLANORDER_NOT_EXISTS);
        }
        if (!STATUS_RELEASED.equals(firstNotBlank(planOrder.getPlanStatus(), STATUS_DRAFT))) {
            throw invalidParamException("只有已下发的生产计划才能人工释放剩余中间品挂接");
        }
        String stockType = firstNotBlank(lock.getLockType(), lock.getStockType());
        if (!STOCK_TYPE_WIP.equalsIgnoreCase(stockType)) {
            throw invalidParamException("只有工序挂接的WIP中间品可以人工释放");
        }
        String lockStatus = firstNotBlank(lock.getLockStatus(), LOCK_STATUS_ACTIVE);
        if (!LOCK_STATUS_ACTIVE.equalsIgnoreCase(lockStatus)) {
            throw invalidParamException("只有有效挂接记录可以释放剩余中间品");
        }
        BigDecimal remainingQty = calculateRemainingLockQty(lock);
        if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("当前挂接记录没有可释放剩余量");
        }
        if (reqVO.getReleaseQty().compareTo(remainingQty) > 0) {
            throw invalidParamException("释放数量不能大于剩余量，当前剩余 " + remainingQty);
        }
        LocalDateTime operateTime = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        hcInvStockService.releasePlanLockedWip(lock, reqVO.getReleaseQty(),
                "计划员人工释放中间边库挂接；目标计划：" + firstNotBlank(lock.getTargetPlanNo(), planOrder.getPlanNo(), "-")
                        + "；目标工序：" + firstNotBlank(lock.getTargetOpName(), lock.getTargetOpCode(), "-")
                        + "；原因：" + reqVO.getReleaseReason(),
                operateTime, operatorId, operatorName);
    }

    @Override
    public List<HcPlanOrderStatusLogDO> getStatusLogListByPlanId(Long planId) {
        validateHcPlanOrderExists(planId);
        return hcPlanOrderStatusLogMapper.selectListByPlanId(planId);
    }

    private List<HcPlanProcessPivotRespVO> buildPlanProcessPivotRespList(
            HcPlanOrderDO plan,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows) {
        boolean hasStageRows = stageRows != null && !stageRows.isEmpty();
        boolean hasPieceRows = pieceRows != null && !pieceRows.isEmpty();
        boolean hasInspectionRows = inspectionRows != null && !inspectionRows.isEmpty();
        List<HcProcessReportMapper.PlanProcessPivotStageRow> safeStageRows =
                hasStageRows ? stageRows : List.of();
        List<HcProcessReportMapper.PlanProcessPivotPieceRow> safePieceRows =
                hasPieceRows ? pieceRows : List.of();
        List<HcProcessReportMapper.PlanProcessPivotInspectionRow> safeInspectionRows =
                hasInspectionRows ? inspectionRows : List.of();
        if (isDiscretePostPlan(plan)) {
            List<HcProcessReportMapper.PlanProcessPivotStageRow> discreteStageRows =
                    filterDiscretePostPivotStageRows(safeStageRows);
            return List.of(buildPlanProcessPivotResp(plan, DISCRETE_BATCH_PLACEHOLDER,
                    discreteStageRows, safePieceRows, safeInspectionRows,
                    PivotDisplayGroup.single(plan, DISCRETE_BATCH_PLACEHOLDER)));
        }
        if (!hasStageRows && !hasPieceRows && !hasInspectionRows) {
            return List.of(buildPlanProcessPivotResp(plan, null, null, null, null));
        }
        List<HcProcessReportMapper.PlanProcessPivotStageRow> planLevelRows = safeStageRows.stream()
                .filter(row -> StrUtil.isBlank(row.segmentBatchNo()))
                .filter(row -> "FORMULA".equals(row.stageCode()) || "WET".equals(row.stageCode()))
                .toList();
        List<HcProcessReportMapper.PlanProcessPivotInspectionRow> planLevelInspectionRows = safeInspectionRows.stream()
                .filter(row -> isPivotPlanLevelStage(row.stageCode()))
                .toList();
        Map<String, List<HcProcessReportMapper.PlanProcessPivotStageRow>> segmentRowsMap = safeStageRows.stream()
                .filter(row -> StrUtil.isNotBlank(row.segmentBatchNo()))
                .collect(Collectors.groupingBy(
                        HcProcessReportMapper.PlanProcessPivotStageRow::segmentBatchNo,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, List<HcProcessReportMapper.PlanProcessPivotPieceRow>> segmentPieceRowsMap = safePieceRows.stream()
                .filter(row -> StrUtil.isNotBlank(row.segmentBatchNo()))
                .collect(Collectors.groupingBy(
                        HcProcessReportMapper.PlanProcessPivotPieceRow::segmentBatchNo,
                        LinkedHashMap::new,
                        Collectors.toList()));
        Map<String, List<HcProcessReportMapper.PlanProcessPivotInspectionRow>> segmentInspectionRowsMap =
                safeInspectionRows.stream()
                        .filter(row -> StrUtil.isNotBlank(row.segmentBatchNo()))
                        .filter(row -> !isPivotPlanLevelStage(row.stageCode()))
                        .collect(Collectors.groupingBy(
                                HcProcessReportMapper.PlanProcessPivotInspectionRow::segmentBatchNo,
                                LinkedHashMap::new,
                                Collectors.toList()));
        if (segmentRowsMap.isEmpty() && segmentPieceRowsMap.isEmpty()) {
            String fallbackSegment = firstNotBlank(plan.getParentProductionBatchNo(), plan.getProductionBatchNo(), plan.getBatchNo());
            return List.of(buildPlanProcessPivotResp(plan, fallbackSegment, safeStageRows, safePieceRows,
                    safeInspectionRows));
        }
        List<HcPlanProcessPivotRespVO> result = new ArrayList<>();
        Set<String> segmentBatchNos = new java.util.LinkedHashSet<>();
        segmentBatchNos.addAll(segmentRowsMap.keySet());
        segmentBatchNos.addAll(segmentPieceRowsMap.keySet());
        segmentBatchNos.forEach(segmentBatchNo -> {
            List<HcProcessReportMapper.PlanProcessPivotStageRow> segmentRows =
                    segmentRowsMap.getOrDefault(segmentBatchNo, List.of());
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> segmentPieceRows =
                    segmentPieceRowsMap.getOrDefault(segmentBatchNo, List.of());
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> segmentInspectionRows =
                    segmentInspectionRowsMap.getOrDefault(segmentBatchNo, List.of());
            List<PivotDisplayGroup> displayGroups =
                    buildPivotDisplayGroups(plan, segmentBatchNo, segmentPieceRows);
            boolean hasSegmentPieceRows = !segmentPieceRows.isEmpty();
            List<HcProcessReportMapper.PlanProcessPivotStageRow> mergedRows = new ArrayList<>(planLevelRows);
            segmentRows.stream()
                    .filter(row -> !hasSegmentPieceRows || !PIVOT_PIECE_STAGE_CODES.contains(row.stageCode()))
                    .forEach(mergedRows::add);
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> mergedInspectionRows =
                    new ArrayList<>(planLevelInspectionRows);
            mergedInspectionRows.addAll(segmentInspectionRows);
            displayGroups.forEach(group -> result.add(buildPlanProcessPivotResp(
                    plan, segmentBatchNo, mergedRows,
                    filterPivotPieceRowsForDisplayGroup(segmentPieceRows, group), mergedInspectionRows, group)));
        });
        return result;
    }

    private List<HcProcessReportMapper.PlanProcessPivotStageRow> filterDiscretePostPivotStageRows(
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows) {
        if (stageRows == null || stageRows.isEmpty()) {
            return List.of();
        }
        return stageRows.stream()
                .filter(row -> row != null && DISCRETE_POST_PROCESS_STAGE_CODES.contains(
                        StrUtil.trimToEmpty(row.stageCode()).toUpperCase(Locale.ROOT)))
                .toList();
    }

    private HcPlanProcessPivotRespVO buildPlanProcessPivotResp(
            HcPlanOrderDO plan,
            String segmentBatchNo,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows) {
        return buildPlanProcessPivotResp(plan, segmentBatchNo, stageRows, pieceRows, inspectionRows,
                PivotDisplayGroup.single(plan, segmentBatchNo));
    }

    private HcPlanProcessPivotRespVO buildPlanProcessPivotResp(
            HcPlanOrderDO plan,
            String segmentBatchNo,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows,
            PivotDisplayGroup displayGroup) {
        HcPlanProcessPivotRespVO respVO = BeanUtils.toBean(plan, HcPlanProcessPivotRespVO.class);
        respVO.setMotherRollBatchNo(firstNotBlank(plan.getParentProductionBatchNo(),
                plan.getProductionBatchNo(), plan.getBatchNo()));
        respVO.setSegmentBatchNo(firstNotBlank(segmentBatchNo, respVO.getMotherRollBatchNo()));
        if (isDiscretePostPlan(plan)) {
            respVO.setMotherRollBatchNo(firstNotBlank(plan.getInventorySourceBatchNos(), respVO.getMotherRollBatchNo()));
            respVO.setSegmentBatchNo(DISCRETE_BATCH_PLACEHOLDER);
        }
        respVO.setActualModelCode(firstNotBlank(displayGroup.actualModelCode(), plan.getModelCode(), plan.getModelName()));
        respVO.setActualSizeSpec(firstNotBlank(displayGroup.actualSizeSpec(), plan.getSizeName(), plan.getSizeSpec()));
        respVO.setPivotRowKey(displayGroup.key());
        respVO.setPlanMergeKey("PLAN|" + planPivotKey(plan));
        respVO.setSegmentMergeKey("SEGMENT|" + planPivotKey(plan) + "|" + respVO.getSegmentBatchNo());
        respVO.setModelSizeMergeKey("MODEL_SIZE|" + planPivotKey(plan)
                + "|" + firstNotBlank(respVO.getActualModelCode(), "-")
                + "|" + firstNotBlank(respVO.getActualSizeSpec(), "-"));
        respVO.setVariationStartStageCode(displayGroup.variationStartStageCode());
        respVO.setVariationStartStageName(PIVOT_STAGE_NAMES.get(displayGroup.variationStartStageCode()));
        respVO.setStageMergeKeys(buildPivotStageMergeKeys(plan, respVO.getSegmentBatchNo(), displayGroup));
        Map<String, HcPlanProcessPivotStageRespVO> stages = initPivotStages();
        if (stageRows != null) {
            stageRows.forEach(row -> mergePivotStage(stages, row));
        }
        fillPivotPieceDetails(stages, pieceRows);
        fillPivotInspectionDetails(stages, inspectionRows);
        fillPivotPendingQty(plan, stages);
        fillPivotSummary(respVO, stages);
        respVO.setStages(stages);
        return respVO;
    }

    private List<PivotDisplayGroup> buildPivotDisplayGroups(
            HcPlanOrderDO plan,
            String segmentBatchNo,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows) {
        if (pieceRows == null || pieceRows.isEmpty()) {
            return List.of(PivotDisplayGroup.single(plan, segmentBatchNo));
        }
        String planModel = firstNotBlank(plan.getModelCode(), plan.getModelName());
        String planSize = firstNotBlank(plan.getSizeName(), plan.getSizeSpec());
        Map<String, PivotPieceIdentity> identityByPiece = new LinkedHashMap<>();
        pieceRows.stream()
                .filter(row -> StrUtil.isNotBlank(row.pieceNo()))
                .sorted(Comparator.comparingInt(row -> pivotStageOrder(row.stageCode())))
                .forEach(row -> {
                    String pieceKey = normalizePivotPieceKey(row.pieceNo());
                    String actualSize = normalizePivotActualSize(row.actualSizeSpec());
                    boolean hasActualIdentity =
                            StrUtil.isNotBlank(row.actualModelCode()) || StrUtil.isNotBlank(actualSize);
                    PivotPieceIdentity current = identityByPiece.computeIfAbsent(pieceKey,
                            key -> new PivotPieceIdentity(planModel, planSize));
                    if (hasActualIdentity) {
                        current.actualModelCode = firstNotBlank(row.actualModelCode(), current.actualModelCode, planModel);
                        current.actualSizeSpec = firstNotBlank(actualSize, current.actualSizeSpec, planSize);
                    }
                    current.stageIdentityKeys.put(pivotStageOrder(row.stageCode()), current.identityKey());
                });
        identityByPiece.values().forEach(identity -> identity.fillStageIdentityKeys(planModel, planSize));
        int firstSplitOrder = PIVOT_STAGE_CODES.size();
        for (int index = 0; index < PIVOT_STAGE_CODES.size(); index++) {
            final int stageOrder = index;
            long stageIdentityCount = identityByPiece.values().stream()
                    .map(identity -> identity.stageIdentityKeys.get(stageOrder))
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .count();
            if (stageIdentityCount > 1) {
                firstSplitOrder = index;
                break;
            }
        }
        Map<String, PivotDisplayGroup> groups = new LinkedHashMap<>();
        int splitOrder = firstSplitOrder >= PIVOT_STAGE_CODES.size()
                ? PIVOT_STAGE_CODES.size() - 1
                : firstSplitOrder;
        identityByPiece.forEach((pieceKey, identity) -> {
            String actualModel = firstNotBlank(identity.actualModelCode, planModel);
            String actualSize = firstNotBlank(identity.actualSizeSpec, planSize);
            String identityKey = actualModel + "|" + actualSize;
            PivotDisplayGroup group = groups.computeIfAbsent(identityKey,
                    key -> new PivotDisplayGroup(
                            "ROW|" + planPivotKey(plan) + "|" + firstNotBlank(segmentBatchNo, "-") + "|" + key,
                            actualModel,
                            actualSize,
                            PIVOT_STAGE_CODES.get(Math.min(splitOrder, PIVOT_STAGE_CODES.size() - 1)),
                            splitOrder,
                            new HashSet<>(),
                            false));
            group.pieceKeys().add(pieceKey);
        });
        if (groups.size() <= 1) {
            return List.of(PivotDisplayGroup.single(plan, segmentBatchNo));
        }
        groups.values().forEach(group -> group.setSplit(true));
        return new ArrayList<>(groups.values());
    }

    private List<HcProcessReportMapper.PlanProcessPivotPieceRow> filterPivotPieceRowsForDisplayGroup(
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            PivotDisplayGroup group) {
        if (pieceRows == null || pieceRows.isEmpty() || !group.split()) {
            return pieceRows;
        }
        return pieceRows.stream()
                .filter(row -> pivotStageOrder(row.stageCode()) < group.variationStartOrder()
                        || group.pieceKeys().contains(normalizePivotPieceKey(row.pieceNo())))
                .toList();
    }

    private Map<String, String> buildPivotStageMergeKeys(
            HcPlanOrderDO plan,
            String segmentBatchNo,
            PivotDisplayGroup group) {
        Map<String, String> mergeKeys = new LinkedHashMap<>();
        String planKey = planPivotKey(plan);
        for (String stageCode : PIVOT_STAGE_CODES) {
            String mergeKey;
            if (isPivotPlanLevelStage(stageCode)) {
                mergeKey = "PLAN|" + planKey + "|" + stageCode;
            } else if (!group.split() || pivotStageOrder(stageCode) < group.variationStartOrder()) {
                mergeKey = "SEGMENT|" + planKey + "|" + firstNotBlank(segmentBatchNo, "-") + "|" + stageCode;
            } else {
                mergeKey = "GROUP|" + group.key() + "|" + stageCode;
            }
            mergeKeys.put(stageCode, mergeKey);
        }
        return mergeKeys;
    }

    private boolean isPivotPlanLevelStage(String stageCode) {
        return "FORMULA".equals(stageCode) || "WET".equals(stageCode);
    }

    private int pivotStageOrder(String stageCode) {
        int index = PIVOT_STAGE_CODES.indexOf(stageCode);
        return index < 0 ? PIVOT_STAGE_CODES.size() : index;
    }

    private String normalizePivotPieceKey(String value) {
        return StrUtil.blankToDefault(value, "").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizePivotActualSize(String value) {
        String text = StrUtil.trimToEmpty(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        String normalized = text.toUpperCase(Locale.ROOT).replace(" ", "");
        if (Set.of("UNKNOWN", "UNCERTAIN", "NONE", "NA", "N/A", "NOT_SET", "UNSET",
                "不确定", "未确定", "未定义", "空", "-").contains(normalized)) {
            return null;
        }
        if ("A".equals(normalized) || normalized.contains("775")) {
            return "775mm";
        }
        if ("B".equals(normalized) || normalized.contains("740")) {
            return "740mm";
        }
        return text;
    }

    private Map<String, String> buildPivotCutOutputSizeByPieceKey(
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows) {
        Map<String, String> result = new LinkedHashMap<>();
        if (pieceRows == null || pieceRows.isEmpty()) {
            return result;
        }
        pieceRows.stream()
                .filter(row -> "CUT_ROUND".equals(StrUtil.trimToEmpty(row.stageCode()).toUpperCase(Locale.ROOT)))
                .filter(row -> StrUtil.isNotBlank(row.pieceNo()))
                .forEach(row -> {
                    String outputSize = normalizePivotActualSize(row.actualSizeSpec());
                    if (StrUtil.isNotBlank(outputSize)) {
                        result.putIfAbsent(normalizePivotPieceKey(row.pieceNo()), outputSize);
                    }
                });
        return result;
    }

    private String resolvePivotOutputActualSize(
            HcProcessReportMapper.PlanProcessPivotPieceRow row,
            Map<String, String> cutOutputSizeByPieceKey) {
        if (row == null) {
            return null;
        }
        String currentSize = normalizePivotActualSize(row.actualSizeSpec());
        if ("CUT_ROUND".equals(StrUtil.trimToEmpty(row.stageCode()).toUpperCase(Locale.ROOT))) {
            return currentSize;
        }
        return firstNotBlank(cutOutputSizeByPieceKey.get(normalizePivotPieceKey(row.pieceNo())), currentSize);
    }

    private String planPivotKey(HcPlanOrderDO plan) {
        return String.valueOf(firstNonNull(plan.getId(), plan.getPlanNo(), "-"));
    }

    private Map<Long, List<HcProcessReportMapper.PlanProcessPivotStageRow>> selectPivotStageRowsMap(List<Long> planIds) {
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotStageRow>> result = new LinkedHashMap<>();
        for (int start = 0; start < planIds.size(); start += PIVOT_STAGE_QUERY_BATCH_SIZE) {
            int end = Math.min(start + PIVOT_STAGE_QUERY_BATCH_SIZE, planIds.size());
            List<Long> batchPlanIds = planIds.subList(start, end);
            List<HcProcessReportMapper.PlanProcessPivotStageRow> rows =
                    new ArrayList<>(hcProcessReportMapper.selectPlanProcessPivotStageRows(batchPlanIds));
            rows.addAll(hcProcessReportMapper.selectPlanProcessPivotStageDefectRows(batchPlanIds));
            rows.stream()
                    .filter(item -> item.planId() != null && item.stageCode() != null)
                    .forEach(item -> result.computeIfAbsent(item.planId(), key -> new ArrayList<>()).add(item));
        }
        return result;
    }

    private List<Long> resolvePivotQtimePlanIds(
            List<Long> planIds,
            Map<Long, PostProcessSourceInfo> postProcessSourceInfoMap) {
        LinkedHashSet<Long> result = new LinkedHashSet<>(planIds);
        if (postProcessSourceInfoMap != null) {
            postProcessSourceInfoMap.values().stream()
                    .filter(Objects::nonNull)
                    .forEach(sourceInfo -> {
                        if (sourceInfo.primarySourcePlanId() != null) {
                            result.add(sourceInfo.primarySourcePlanId());
                        }
                        sourceInfo.sourcePlanIdsByGrindingSegment().values().stream()
                                .filter(Objects::nonNull)
                                .forEach(result::add);
                    });
        }
        return new ArrayList<>(result);
    }

    private Map<Long, Map<String, LocalDateTime>> selectPivotStageStartTimeMap(List<Long> planIds) {
        Map<Long, Map<String, LocalDateTime>> result = new LinkedHashMap<>();
        if (planIds == null || planIds.isEmpty()) {
            return result;
        }
        for (int start = 0; start < planIds.size(); start += PIVOT_STAGE_QUERY_BATCH_SIZE) {
            int end = Math.min(start + PIVOT_STAGE_QUERY_BATCH_SIZE, planIds.size());
            List<Long> batchPlanIds = planIds.subList(start, end);
            hcProcessReportMapper.selectPlanProcessPivotStageStartRows(batchPlanIds).stream()
                    .filter(item -> item.planId() != null && item.stageCode() != null && item.startTime() != null)
                    .forEach(item -> result.computeIfAbsent(item.planId(), key -> new LinkedHashMap<>())
                            .merge(buildPivotStageStartKey(item.stageCode(), item.segmentBatchNo()),
                                    item.startTime(), this::earliestTime));
        }
        return result;
    }

    private Map<Long, List<HcProcessReportMapper.PlanProcessPivotPieceRow>> selectPivotPieceRowsMap(List<Long> planIds) {
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotPieceRow>> result = new LinkedHashMap<>();
        for (int start = 0; start < planIds.size(); start += PIVOT_STAGE_QUERY_BATCH_SIZE) {
            int end = Math.min(start + PIVOT_STAGE_QUERY_BATCH_SIZE, planIds.size());
            List<Long> batchPlanIds = planIds.subList(start, end);
            hcProcessReportMapper.selectPlanProcessPivotPieceRows(batchPlanIds).stream()
                    .filter(item -> item.planId() != null && item.stageCode() != null)
                    .forEach(item -> result.computeIfAbsent(item.planId(), key -> new ArrayList<>()).add(item));
        }
        return result;
    }

    private Map<Long, List<HcProcessReportMapper.PlanProcessPivotInspectionRow>> selectPivotInspectionRowsMap(
            List<Long> planIds) {
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotInspectionRow>> result = new LinkedHashMap<>();
        for (int start = 0; start < planIds.size(); start += PIVOT_STAGE_QUERY_BATCH_SIZE) {
            int end = Math.min(start + PIVOT_STAGE_QUERY_BATCH_SIZE, planIds.size());
            List<Long> batchPlanIds = planIds.subList(start, end);
            hcProcessReportMapper.selectPlanProcessPivotInspectionRows(batchPlanIds).stream()
                    .filter(item -> item.planId() != null && item.stageCode() != null)
                    .forEach(item -> result.computeIfAbsent(item.planId(), key -> new ArrayList<>()).add(item));
        }
        return result;
    }

    private PageResult<HcPlanProcessPivotRespVO> paginatePivotRows(
            List<HcPlanProcessPivotRespVO> list, HcPlanProcessPivotPageReqVO pageReqVO) {
        int pageNo = pageReqVO.getPageNo() == null ? 1 : pageReqVO.getPageNo();
        int pageSize = pageReqVO.getPageSize() == null ? 10 : pageReqVO.getPageSize();
        if (PageParam.PAGE_SIZE_NONE.equals(pageSize)) {
            return new PageResult<>(list, (long) list.size());
        }
        int fromIndex = Math.min((pageNo - 1) * pageSize, list.size());
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return new PageResult<>(list.subList(fromIndex, toIndex), (long) list.size());
    }

    private Map<String, HcPlanProcessPivotStageRespVO> initPivotStages() {
        Map<String, HcPlanProcessPivotStageRespVO> stages = new LinkedHashMap<>();
        for (String stageCode : PIVOT_STAGE_CODES) {
            stages.put(stageCode, createPivotStage(stageCode));
        }
        return stages;
    }

    private HcPlanProcessPivotStageRespVO createPivotStage(String stageCode) {
        HcPlanProcessPivotStageRespVO stage = new HcPlanProcessPivotStageRespVO();
        stage.setStageCode(stageCode);
        stage.setStageName(PIVOT_STAGE_NAMES.get(stageCode));
        stage.setStageStatus("NOT_STARTED");
        stage.setInputQty(BigDecimal.ZERO);
        stage.setReportQty(BigDecimal.ZERO);
        stage.setDoneQty(BigDecimal.ZERO);
        stage.setPendingQty(BigDecimal.ZERO);
        stage.setDefectQty(BigDecimal.ZERO);
        stage.setInspectionQty(BigDecimal.ZERO);
        stage.setInspectionNgQty(BigDecimal.ZERO);
        stage.setConfirmedQty(BigDecimal.ZERO);
        stage.setLengthQty(BigDecimal.ZERO);
        stage.setReportUnit(resolveDefaultStageUnit(stageCode));
        stage.setPendingUnit(resolveDefaultStageUnit(stageCode));
        stage.setLengthUnit("m");
        return stage;
    }

    private void mergePivotStage(
            Map<String, HcPlanProcessPivotStageRespVO> stages,
            HcProcessReportMapper.PlanProcessPivotStageRow row) {
        HcPlanProcessPivotStageRespVO stage = stages.get(row.stageCode());
        if (stage == null) {
            return;
        }
        mergePivotStage(stage, row);
    }

    private void mergePivotStage(
            HcPlanProcessPivotStageRespVO stage,
            HcProcessReportMapper.PlanProcessPivotStageRow row) {
        stage.setSourceBatchNos(firstNotBlank(row.sourceBatchNos(), stage.getSourceBatchNos()));
        stage.setOutputBatchNos(firstNotBlank(row.outputBatchNos(), stage.getOutputBatchNos()));
        stage.setInputQty(zeroIfNull(stage.getInputQty()).add(zeroIfNull(row.inputQty())));
        stage.setReportQty(zeroIfNull(stage.getReportQty()).add(zeroIfNull(row.reportQty())));
        stage.setDoneQty(zeroIfNull(stage.getDoneQty()).add(zeroIfNull(row.doneQty())));
        stage.setDefectQty(zeroIfNull(stage.getDefectQty()).add(zeroIfNull(row.defectQty())));
        stage.setConfirmedQty(zeroIfNull(stage.getConfirmedQty()).add(zeroIfNull(row.confirmedQty())));
        stage.setLengthQty(zeroIfNull(stage.getLengthQty()).add(zeroIfNull(row.lengthQty())));
        stage.setStartPosition(row.startPosition() == null ? stage.getStartPosition() : row.startPosition());
        stage.setProcessLength(row.processLength() == null ? stage.getProcessLength() : row.processLength());
        stage.setReportUnit(firstNotBlank(row.reportUnit(), stage.getReportUnit()));
        stage.setPendingUnit(stage.getReportUnit());
        stage.setLengthUnit("m");
        stage.setLastReportTime(latestTime(stage.getLastReportTime(), row.lastReportTime()));
        stage.setRemark(firstNotBlank(stage.getRemark(), row.remark()));
    }

    private void fillPivotPieceDetails(
            Map<String, HcPlanProcessPivotStageRespVO> stages,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows) {
        if (pieceRows == null || pieceRows.isEmpty()) {
            return;
        }
        Map<String, String> cutOutputSizeByPieceKey = buildPivotCutOutputSizeByPieceKey(pieceRows);
        Map<String, LinkedHashMap<String, HcPlanProcessPivotPieceRespVO>> detailsByStage = new LinkedHashMap<>();
        for (HcProcessReportMapper.PlanProcessPivotPieceRow row : pieceRows) {
            if (!PIVOT_PIECE_STAGE_CODES.contains(row.stageCode()) || StrUtil.isBlank(row.pieceNo())) {
                continue;
            }
            HcPlanProcessPivotStageRespVO stage = stages.get(row.stageCode());
            if (stage == null) {
                continue;
            }
            LinkedHashMap<String, HcPlanProcessPivotPieceRespVO> stageDetails =
                    detailsByStage.computeIfAbsent(row.stageCode(), key -> new LinkedHashMap<>());
            String pieceKey = row.pieceNo().trim().toUpperCase(Locale.ROOT);
            HcPlanProcessPivotPieceRespVO detail = stageDetails.computeIfAbsent(pieceKey, key -> {
                HcPlanProcessPivotPieceRespVO item = new HcPlanProcessPivotPieceRespVO();
                item.setStageCode(row.stageCode());
                item.setPieceNo(row.pieceNo());
                item.setSourceBatchNo(row.sourceBatchNo());
                item.setOutputBatchNo(firstNotBlank(row.outputBatchNo(), row.pieceNo()));
                item.setActualModelCode(row.actualModelCode());
                item.setActualSizeSpec(normalizePivotActualSize(row.actualSizeSpec()));
                item.setOutputActualSizeSpec(resolvePivotOutputActualSize(row, cutOutputSizeByPieceKey));
                item.setReportConfirmed(false);
                item.setDefectFlag(false);
                item.setCoaFlag(false);
                return item;
            });
            detail.setSourceBatchNo(firstNotBlank(detail.getSourceBatchNo(), row.sourceBatchNo()));
            detail.setOutputBatchNo(firstNotBlank(detail.getOutputBatchNo(), row.outputBatchNo(), row.pieceNo()));
            detail.setActualModelCode(firstNotBlank(detail.getActualModelCode(), row.actualModelCode()));
            detail.setActualSizeSpec(firstNotBlank(detail.getActualSizeSpec(),
                    normalizePivotActualSize(row.actualSizeSpec())));
            detail.setOutputActualSizeSpec(firstNotBlank(detail.getOutputActualSizeSpec(),
                    resolvePivotOutputActualSize(row, cutOutputSizeByPieceKey)));
            detail.setReportConfirmed(Boolean.TRUE.equals(detail.getReportConfirmed())
                    || isPivotPieceReportCompleted(row));
            detail.setDefectFlag(Boolean.TRUE.equals(detail.getDefectFlag()) || flag(row.defectFlag()));
            detail.setCoaFlag(Boolean.TRUE.equals(detail.getCoaFlag()) || flag(row.coaFlag()));
            detail.setLastReportTime(latestTime(detail.getLastReportTime(), row.lastReportTime()));
            detail.setRemark(firstNotBlank(detail.getRemark(), row.remark()));
        }
        detailsByStage.forEach((stageCode, detailMap) -> {
            HcPlanProcessPivotStageRespVO stage = stages.get(stageCode);
            if (stage == null) {
                return;
            }
            List<HcPlanProcessPivotPieceRespVO> details = detailMap.values().stream()
                    .sorted(Comparator.comparing(HcPlanProcessPivotPieceRespVO::getPieceNo,
                            Comparator.nullsLast(String::compareTo)))
                    .toList();
            long doneQty = details.stream()
                    .filter(item -> !Boolean.TRUE.equals(item.getDefectFlag()))
                    .filter(item -> Boolean.TRUE.equals(item.getReportConfirmed()))
                    .count();
            long defectQty = details.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getDefectFlag()))
                    .count();
            long confirmedQty = details.stream()
                    .filter(item -> Boolean.TRUE.equals(item.getReportConfirmed()))
                    .count();
            details.forEach(item -> item.setStatus(resolvePieceStatus(item)));
            stage.setPieceDetails(details);
            stage.setReportQty(BigDecimal.valueOf(details.size()));
            stage.setDoneQty(BigDecimal.valueOf(doneQty));
            stage.setDefectQty(BigDecimal.valueOf(defectQty));
            stage.setConfirmedQty(BigDecimal.valueOf(confirmedQty));
            stage.setReportUnit("片");
            stage.setPendingUnit("片");
            stage.setSourceBatchNos(firstNotBlank(joinDistinct(details.stream()
                    .map(HcPlanProcessPivotPieceRespVO::getSourceBatchNo)
                    .toList()), stage.getSourceBatchNos()));
            stage.setOutputBatchNos(firstNotBlank(joinDistinct(details.stream()
                    .map(HcPlanProcessPivotPieceRespVO::getOutputBatchNo)
                    .toList()), stage.getOutputBatchNos()));
            details.stream()
                    .map(HcPlanProcessPivotPieceRespVO::getLastReportTime)
                    .filter(Objects::nonNull)
                    .forEach(time -> stage.setLastReportTime(latestTime(stage.getLastReportTime(), time)));
        });
    }

    private void fillPivotInspectionDetails(
            Map<String, HcPlanProcessPivotStageRespVO> stages,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows) {
        if (inspectionRows == null || inspectionRows.isEmpty()) {
            return;
        }
        Map<String, LinkedHashMap<String, HcPlanProcessPivotInspectionRespVO>> detailsByStage = new LinkedHashMap<>();
        for (HcProcessReportMapper.PlanProcessPivotInspectionRow row : inspectionRows) {
            HcPlanProcessPivotStageRespVO stage = stages.get(row.stageCode());
            if (stage == null || row.inspectionId() == null || StrUtil.isBlank(row.sourceType())) {
                continue;
            }
            LinkedHashMap<String, HcPlanProcessPivotInspectionRespVO> stageDetails =
                    detailsByStage.computeIfAbsent(row.stageCode(), key -> new LinkedHashMap<>());
            String detailKey = row.sourceType() + "|" + row.inspectionId();
            boolean existingDetail = stageDetails.containsKey(detailKey);
            HcPlanProcessPivotInspectionRespVO detail = stageDetails.computeIfAbsent(detailKey, key -> {
                HcPlanProcessPivotInspectionRespVO item = new HcPlanProcessPivotInspectionRespVO();
                item.setStageCode(row.stageCode());
                item.setSourceType(row.sourceType());
                item.setInspectionId(row.inspectionId());
                item.setInspectionNo(row.inspectionNo());
                item.setInspectionType(row.inspectionType());
                item.setProductBatchNo(row.productBatchNo());
                item.setInspectionQty(BigDecimal.ZERO);
                item.setInspectionNgQty(BigDecimal.ZERO);
                item.setJudgment(row.judgment());
                item.setStatus(row.status());
                item.setInspectionTime(row.inspectionTime());
                item.setDefectSummary(row.defectSummary());
                item.setRemark(row.remark());
                return item;
            });
            if (!existingDetail) {
                detail.setInspectionQty(zeroIfNull(detail.getInspectionQty()).add(zeroIfNull(row.inspectionQty())));
                detail.setInspectionNgQty(zeroIfNull(detail.getInspectionNgQty()).add(zeroIfNull(row.inspectionNgQty())));
            }
            detail.setProductBatchNo(firstNotBlank(detail.getProductBatchNo(), row.productBatchNo()));
            detail.setInspectionNo(firstNotBlank(detail.getInspectionNo(), row.inspectionNo()));
            detail.setInspectionType(firstNotBlank(detail.getInspectionType(), row.inspectionType()));
            detail.setJudgment(firstNotBlank(detail.getJudgment(), row.judgment()));
            detail.setStatus(firstNotBlank(detail.getStatus(), row.status()));
            detail.setInspectionTime(latestTime(detail.getInspectionTime(), row.inspectionTime()));
            detail.setDefectSummary(firstNotBlank(detail.getDefectSummary(), row.defectSummary()));
            detail.setRemark(firstNotBlank(detail.getRemark(), row.remark()));
        }
        detailsByStage.forEach((stageCode, detailMap) -> {
            HcPlanProcessPivotStageRespVO stage = stages.get(stageCode);
            if (stage == null) {
                return;
            }
            List<HcPlanProcessPivotInspectionRespVO> details = detailMap.values().stream()
                    .sorted(Comparator
                            .comparing(HcPlanProcessPivotInspectionRespVO::getInspectionTime,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(HcPlanProcessPivotInspectionRespVO::getInspectionNo,
                                    Comparator.nullsLast(String::compareTo)))
                    .toList();
            stage.setInspectionDetails(details);
            stage.setInspectionQty(details.stream()
                    .map(HcPlanProcessPivotInspectionRespVO::getInspectionQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stage.setInspectionNgQty(details.stream()
                    .map(HcPlanProcessPivotInspectionRespVO::getInspectionNgQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            details.stream()
                    .map(HcPlanProcessPivotInspectionRespVO::getInspectionTime)
                    .filter(Objects::nonNull)
                    .forEach(time -> stage.setLastReportTime(latestTime(stage.getLastReportTime(), time)));
        });
    }

    private String resolvePieceStatus(HcPlanProcessPivotPieceRespVO item) {
        if (Boolean.TRUE.equals(item.getDefectFlag())) {
            return "DEFECT";
        }
        if (Boolean.TRUE.equals(item.getReportConfirmed())) {
            return "DONE";
        }
        return "PENDING";
    }

    private boolean isPivotPieceReportCompleted(HcProcessReportMapper.PlanProcessPivotPieceRow row) {
        if (flag(row.reportConfirmed())) {
            return true;
        }
        String stageCode = StrUtil.trimToEmpty(row.stageCode()).toUpperCase(Locale.ROOT);
        return row.lastReportTime() != null && PIVOT_SUBMITTED_REPORT_STAGE_CODES.contains(stageCode);
    }

    private void fillPivotPendingQty(HcPlanOrderDO plan, Map<String, HcPlanProcessPivotStageRespVO> stages) {
        BigDecimal planQty = zeroIfNull(firstNonNull(plan.getNetPlanQty(), plan.getTargetQty()));
        setPending(stages.get("FORMULA"), subtractNonNegative(planQty, doneQty(stages, "FORMULA")), "kg");
        setPending(stages.get("WET"), subtractNonNegative(doneQty(stages, "FORMULA"), doneQty(stages, "WET")), "m");
        setPending(stages.get("GRINDING"),
                subtractNonNegative(inputQty(stages, "GRINDING"), doneQty(stages, "GRINDING")), "m");
        setPending(stages.get("ADHESIVE1"),
                subtractNonNegative(doneQty(stages, "GRINDING"), doneQty(stages, "ADHESIVE1")), "m");
        setPending(stages.get("SLITTING"),
                piecePendingQty(stages, "SLITTING"), "片");
        setPending(stages.get("PRESS_SLOT"),
                piecePendingQty(stages, "PRESS_SLOT"), "片");
        setPending(stages.get("ADHESIVE2"),
                piecePendingQty(stages, "ADHESIVE2"), "片");
        setPending(stages.get("CUT_ROUND"),
                piecePendingQty(stages, "CUT_ROUND"), "片");
        stages.values().forEach(this::refreshStageStatus);
    }

    private void fillPivotSummary(HcPlanProcessPivotRespVO respVO, Map<String, HcPlanProcessPivotStageRespVO> stages) {
        BigDecimal totalDefectQty = BigDecimal.ZERO;
        LocalDateTime latestReportTime = null;
        for (HcPlanProcessPivotStageRespVO stage : stages.values()) {
            totalDefectQty = totalDefectQty.add(zeroIfNull(stage.getDefectQty()));
            if (stage.getLastReportTime() != null
                    && (latestReportTime == null || stage.getLastReportTime().isAfter(latestReportTime))) {
                latestReportTime = stage.getLastReportTime();
            }
        }
        respVO.setTotalDefectQty(totalDefectQty);
        respVO.setLatestReportTime(latestReportTime);
    }

    private void refreshStageStatus(HcPlanProcessPivotStageRespVO stage) {
        if ("SHIPPING_INSPECTION".equals(stage.getStageCode())) {
            stage.setStageStatus(zeroIfNull(stage.getInspectionQty()).compareTo(BigDecimal.ZERO) > 0
                    ? "FINISHED" : "NOT_STARTED");
            return;
        }
        BigDecimal doneQty = zeroIfNull(stage.getDoneQty());
        BigDecimal pendingQty = zeroIfNull(stage.getPendingQty());
        if (doneQty.compareTo(BigDecimal.ZERO) <= 0 && pendingQty.compareTo(BigDecimal.ZERO) <= 0) {
            stage.setStageStatus("NOT_STARTED");
        } else if (doneQty.compareTo(BigDecimal.ZERO) <= 0) {
            stage.setStageStatus("PENDING");
        } else if (pendingQty.compareTo(BigDecimal.ZERO) > 0) {
            stage.setStageStatus("RUNNING");
        } else {
            stage.setStageStatus("FINISHED");
        }
    }

    private void setPending(HcPlanProcessPivotStageRespVO stage, BigDecimal pendingQty, String pendingUnit) {
        if (stage == null) {
            return;
        }
        stage.setPendingQty(zeroIfNull(pendingQty));
        stage.setPendingUnit(pendingUnit);
    }

    private BigDecimal inputQty(Map<String, HcPlanProcessPivotStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getInputQty());
    }

    private BigDecimal reportQty(Map<String, HcPlanProcessPivotStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getReportQty());
    }

    private BigDecimal doneQty(Map<String, HcPlanProcessPivotStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getDoneQty());
    }

    private BigDecimal confirmedQty(Map<String, HcPlanProcessPivotStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getConfirmedQty());
    }

    private BigDecimal defectQty(Map<String, HcPlanProcessPivotStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getDefectQty());
    }

    private BigDecimal piecePendingQty(Map<String, HcPlanProcessPivotStageRespVO> stages, String stageCode) {
        return subtractNonNegative(subtractNonNegative(reportQty(stages, stageCode), doneQty(stages, stageCode)),
                defectQty(stages, stageCode));
    }

    private BigDecimal lengthQty(Map<String, HcPlanProcessPivotStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getLengthQty());
    }

    private BigDecimal subtractNonNegative(BigDecimal minuend, BigDecimal subtrahend) {
        BigDecimal value = zeroIfNull(minuend).subtract(zeroIfNull(subtrahend));
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean positive(BigDecimal value) {
        return zeroIfNull(value).compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean flag(Integer value) {
        return value != null && value > 0;
    }

    private String joinDistinct(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> items = values.stream()
                .filter(StrUtil::isNotBlank)
                .map(String::trim)
                .distinct()
                .toList();
        return items.isEmpty() ? null : String.join(",", items);
    }

    private LocalDateTime latestTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return second.isAfter(first) ? second : first;
    }

    private LocalDateTime earliestTime(LocalDateTime first, LocalDateTime second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return second.isBefore(first) ? second : first;
    }

    private String resolveDefaultStageUnit(String stageCode) {
        if ("FORMULA".equals(stageCode)) {
            return "kg";
        }
        if ("SLITTING".equals(stageCode)
                || "PRESS_SLOT".equals(stageCode)
                || "ADHESIVE2".equals(stageCode)
                || "CUT_ROUND".equals(stageCode)
                || "SHIPPING_INSPECTION".equals(stageCode)) {
            return "片";
        }
        return "m";
    }

    private static CascadeTarget target(String tableName, String columnName) {
        return new CascadeTarget(tableName, columnName);
    }

    private void syncPlanOperationStatusAfterPlanStatusChange(Long planId, String toStatus, String reasonRemark,
                                                              LocalDateTime operateTime, Long operatorId,
                                                              String operatorName) {
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(planId);
        if (operations.isEmpty()) {
            return;
        }
        for (HcPlanOrderOperationDO operation : operations) {
            String fromStatus = firstNotBlank(operation.getOperationStatus(), OP_STATUS_NOT_RELEASED);
            String nextStatus = resolveOperationStatusForPlanStatus(toStatus, fromStatus);
            if (nextStatus == null || Objects.equals(fromStatus, nextStatus)) {
                continue;
            }
            HcPlanOrderOperationDO operationUpdate = new HcPlanOrderOperationDO();
            operationUpdate.setId(operation.getId());
            operationUpdate.setOperationStatus(nextStatus);
            operationUpdate.setStatusOperatorId(operatorId);
            operationUpdate.setStatusOperatorName(operatorName);
            operationUpdate.setStatusOperateTime(operateTime);
            if (OP_STATUS_PAUSED.equals(nextStatus)) {
                operationUpdate.setPauseRemark(reasonRemark);
            } else if (OP_STATUS_CANCELLED.equals(nextStatus)) {
                operationUpdate.setCancelReason(reasonRemark);
            }
            hcPlanOrderOperationMapper.updateById(operationUpdate);
        }
    }

    private String resolveOperationStatusForPlanStatus(String planStatus, String fromStatus) {
        if (OP_STATUS_FINISHED.equals(fromStatus) || OP_STATUS_CANCELLED.equals(fromStatus)) {
            return null;
        }
        if (STATUS_RELEASED.equals(planStatus)) {
            return OP_STATUS_PAUSED.equals(fromStatus) ? OP_STATUS_RELEASED
                    : OP_STATUS_NOT_RELEASED.equals(fromStatus) ? OP_STATUS_RELEASED : null;
        }
        if (STATUS_DRAFT.equals(planStatus)) {
            return OP_STATUS_NOT_RELEASED;
        }
        if (STATUS_PAUSED.equals(planStatus)) {
            return OP_STATUS_PAUSED;
        }
        if (STATUS_CANCELLED.equals(planStatus)) {
            return OP_STATUS_CANCELLED;
        }
        return null;
    }

    private HcPlanOrderDO getRequiredPlanOrder(Long id) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(id);
        if (planOrder == null) {
            throw exception(HCPLANORDER_NOT_EXISTS);
        }
        return planOrder;
    }

    private List<HcPlanOrderDO> getRequiredPlanOrders(Collection<Long> ids) {
        List<Long> planIds = normalizedIds(ids);
        if (planIds.isEmpty()) {
            return List.of();
        }
        List<HcPlanOrderDO> planOrders = hcPlanOrderMapper.selectBatchIds(planIds);
        if (planOrders.size() != planIds.size()) {
            throw exception(HCPLANORDER_NOT_EXISTS);
        }
        return planOrders;
    }

    private List<Long> normalizedIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void validateCancelledPlanBeforeDelete(List<HcPlanOrderDO> planOrders) {
        List<String> invalidPlanNos = planOrders.stream()
                .filter(item -> !STATUS_CANCELLED.equals(normalizeCancelledStatus(item.getPlanStatus())))
                .map(item -> firstNotBlank(item.getPlanNo(), String.valueOf(item.getId())))
                .toList();
        if (!invalidPlanNos.isEmpty()) {
            throw invalidParamException("只有作废取消状态的生产计划允许删除，请先作废计划：" + String.join("、", invalidPlanNos));
        }
    }

    private String normalizeCancelledStatus(String status) {
        String normalized = String.valueOf(status).trim().toUpperCase();
        return "CANCELED".equals(normalized) ? STATUS_CANCELLED : normalized;
    }

    private void deletePlanOrderCascade(List<Long> planIds, boolean physical) {
        List<Long> safePlanIds = normalizedIds(planIds);
        if (safePlanIds.isEmpty()) {
            return;
        }
        String updater = String.valueOf(SecurityFrameworkUtils.getLoginUserId());

        List<Long> processFormRecordIds = selectIds("mes_hc_process_form_record", "plan_id", safePlanIds);
        deleteTarget(target("mes_hc_process_form_record_item", "record_id"), processFormRecordIds, physical, updater);

        List<Long> stationRecordIds = selectIds("mes_sfc_station_record", "plan_id", safePlanIds);
        deleteTarget(target("mes_sfc_station_record_item", "record_id"), stationRecordIds, physical, updater);

        List<Long> productionInstructionIds = selectIds("mes_pp_production_instruction", "plan_id", safePlanIds);
        deleteTarget(target("mes_pp_production_instruction_recipient", "instruction_id"),
                productionInstructionIds, physical, updater);

        List<Long> splitOrderIds = hcPlanOrderCascadeDeleteMapper.selectPlanSplitOrderIdsByPlanIds(safePlanIds);
        deleteTarget(target("mes_pp_plan_split_detail", "split_order_id"), splitOrderIds, physical, updater);
        deleteTarget(target("mes_pp_plan_split_order", "id"), splitOrderIds, physical, updater);

        List<Long> grindingMiddleRecordIds = selectIds("mes_sfc_grinding_middle_product_record", "plan_id", safePlanIds);
        deleteTarget(target("mes_sfc_grinding_middle_product_detail", "record_id"),
                grindingMiddleRecordIds, physical, updater);

        List<Long> cutRoundInspectionTaskIds = selectIds("mes_sfc_cut_round_inspection_task", "plan_id", safePlanIds);
        deleteTarget(target("mes_sfc_cut_round_inspection_detail", "task_id"),
                cutRoundInspectionTaskIds, physical, updater);

        List<Long> inboundOrderIds = selectIds("mes_inv_fg_inbound_order", "plan_id", safePlanIds);
        deleteTarget(target("mes_inv_fg_inbound_order_item", "inbound_order_id"),
                inboundOrderIds, physical, updater);

        List<Long> faiIds = selectIds("mes_qms_fai_order", "plan_order_id", safePlanIds);
        deleteTargets(FAI_CASCADE_TARGETS, faiIds, physical, updater);

        List<Long> fqcIds = selectIds("mes_qms_fqc_order", "plan_order_id", safePlanIds);
        deleteTargets(FQC_CASCADE_TARGETS, fqcIds, physical, updater);

        List<Long> ipqcIds = selectIds("mes_qms_ipqc_order", "plan_order_id", safePlanIds);
        deleteTargets(IPQC_CASCADE_TARGETS, ipqcIds, physical, updater);

        deleteTargets(PLAN_DIRECT_CASCADE_TARGETS, safePlanIds, physical, updater);
    }

    private List<Long> selectIds(String tableName, String columnName, Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return hcPlanOrderCascadeDeleteMapper.selectIdsByLongColumn(tableName, columnName, ids);
    }

    private void deleteTargets(List<CascadeTarget> targets, Collection<Long> ids, boolean physical, String updater) {
        if (targets == null || targets.isEmpty() || ids == null || ids.isEmpty()) {
            return;
        }
        targets.forEach(target -> deleteTarget(target, ids, physical, updater));
    }

    private void deleteTarget(CascadeTarget target, Collection<Long> ids, boolean physical, String updater) {
        if (target == null || ids == null || ids.isEmpty()) {
            return;
        }
        if (physical) {
            hcPlanOrderCascadeDeleteMapper.physicalDeleteByLongColumn(target.tableName(), target.columnName(), ids);
        } else {
            hcPlanOrderCascadeDeleteMapper.logicalDeleteByLongColumn(target.tableName(), target.columnName(), ids, updater);
        }
    }

    private String normalizePlanStatus(String status) {
        String normalized = String.valueOf(status).trim().toUpperCase();
        if ("CANCELED".equals(normalized)) {
            normalized = STATUS_CANCELLED;
        }
        if (STATUS_DRAFT.equals(normalized)
                || STATUS_RELEASED.equals(normalized)
                || STATUS_PAUSED.equals(normalized)
                || STATUS_CLOSED.equals(normalized)
                || STATUS_CANCELLED.equals(normalized)) {
            return normalized;
        }
        throw invalidParamException("计划状态仅支持草稿、下达、暂停、关闭、作废取消");
    }

    private String resolvePlanStatusAction(String toStatus) {
        if (STATUS_RELEASED.equals(toStatus)) {
            return "RELEASE";
        }
        if (STATUS_PAUSED.equals(toStatus)) {
            return "PAUSE";
        }
        if (STATUS_CLOSED.equals(toStatus)) {
            return "CLOSE";
        }
        if (STATUS_CANCELLED.equals(toStatus)) {
            return "CANCEL";
        }
        return "SET_STATUS";
    }

    private record PostProcessSourceInfo(
            String motherBatchNo,
            String grindingSegmentBatchNo,
            Long primarySourcePlanId,
            Map<String, Long> sourcePlanIdsByGrindingSegment,
            Map<String, HcPlanProcessPivotStageRespVO> inheritedFrontStages,
            Map<String, HcPlanProcessPivotStageRespVO> inheritedGrindingStagesBySegment) {
    }

    private record CascadeTarget(String tableName, String columnName) {
    }

    private String resolveNextOperationStatus(HcPlanOrderOperationStatusReqVO reqVO, String fromStatus) {
        return switch (reqVO.getActionType()) {
            case "FINISH" -> {
                if (!OP_STATUS_RUNNING.equals(fromStatus) && !OP_STATUS_PAUSED.equals(fromStatus)) {
                    throw invalidParamException("只有执行中或暂停的工序可以完工");
                }
                if (StrUtil.isBlank(reqVO.getReasonRemark())) {
                    throw invalidParamException("完工备注不能为空");
                }
                yield OP_STATUS_FINISHED;
            }
            case "PAUSE" -> {
                if (!OP_STATUS_RUNNING.equals(fromStatus)) {
                    throw invalidParamException("只有执行中的工序可以暂停");
                }
                if (normalizeStatusDates(reqVO).isEmpty()) {
                    throw invalidParamException("请选择至少一个暂停日期");
                }
                if (StrUtil.isBlank(reqVO.getReasonRemark())) {
                    throw invalidParamException("暂停说明不能为空");
                }
                yield OP_STATUS_PAUSED;
            }
            case "RESUME" -> {
                if (!OP_STATUS_PAUSED.equals(fromStatus)) {
                    throw invalidParamException("只有暂停的工序可以复工");
                }
                if (reqVO.getResumeDate() == null) {
                    throw invalidParamException("请选择复工日期");
                }
                yield OP_STATUS_RUNNING;
            }
            case "CANCEL" -> {
                if (OP_STATUS_CANCELLED.equals(fromStatus) || OP_STATUS_FINISHED.equals(fromStatus)) {
                    throw invalidParamException("已取消或已完工的工序不能取消");
                }
                if (StrUtil.isBlank(reqVO.getReasonRemark())) {
                    throw invalidParamException("取消原因不能为空");
                }
                yield OP_STATUS_CANCELLED;
            }
            default -> throw invalidParamException("不支持的工序状态动作");
        };
    }

    private List<LocalDate> normalizeStatusDates(HcPlanOrderOperationStatusReqVO reqVO) {
        if (reqVO.getStatusDates() != null && !reqVO.getStatusDates().isEmpty()) {
            return reqVO.getStatusDates().stream().filter(Objects::nonNull).distinct().sorted().toList();
        }
        if (reqVO.getPauseStartDate() == null || reqVO.getPauseEndDate() == null) {
            return List.of();
        }
        if (reqVO.getPauseStartDate().isAfter(reqVO.getPauseEndDate())) {
            return List.of();
        }
        List<LocalDate> dates = new java.util.ArrayList<>();
        LocalDate cursor = reqVO.getPauseStartDate();
        while (!cursor.isAfter(reqVO.getPauseEndDate())) {
            dates.add(cursor);
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private String buildStatusDatesJson(HcPlanOrderOperationStatusReqVO reqVO) {
        List<LocalDate> dates = "RESUME".equals(reqVO.getActionType())
                ? List.of(reqVO.getResumeDate())
                : normalizeStatusDates(reqVO);
        return dates.isEmpty() ? null : JsonUtils.toJsonString(dates);
    }

    private String mergeStatusDateMarks(String oldJson, List<LocalDate> dates, String type,
                                        String remark, String operatorName, LocalDateTime operateTime) {
        Map<String, Object> marks = StrUtil.isBlank(oldJson)
                ? new LinkedHashMap<>()
                : JsonUtils.parseObjectQuietly(oldJson, new TypeReference<Map<String, Object>>() {});
        if (marks == null) {
            marks = new LinkedHashMap<>();
        }
        for (LocalDate date : dates) {
            if (date == null) {
                continue;
            }
            Map<String, Object> mark = new LinkedHashMap<>();
            mark.put("type", type);
            mark.put("remark", remark);
            mark.put("operatorName", operatorName);
            mark.put("operateTime", operateTime);
            marks.put(date.toString(), mark);
        }
        return marks.isEmpty() ? null : JsonUtils.toJsonString(marks);
    }

    private void validateHcPlanOrderExists(Long id) {
        if (hcPlanOrderMapper.selectById(id) == null) {
            throw exception(HCPLANORDER_NOT_EXISTS);
        }
    }

    private void validatePlanNoUnique(Long id, String planNo) {
        if (planNo == null) {
            return;
        }
        HcPlanOrderDO entity = hcPlanOrderMapper.selectOne(new LambdaQueryWrapperX<HcPlanOrderDO>()
                .eq(HcPlanOrderDO::getPlanNo, planNo)
                .neIfPresent(HcPlanOrderDO::getId, id));
        if (entity != null) {
            throw exception(HCPLANORDER_PLANNO_EXISTS);
        }
    }

    private String generateNextPlanNo() {
        String prefix = LocalDate.now().format(PLAN_NO_DATE_FORMATTER) + "-";
        int maxSequence = hcPlanOrderMapper.selectPlanNosByPrefixIncludeDeleted(prefix).stream()
                .mapToInt(planNo -> parsePlanNoSequence(planNo, prefix))
                .max()
                .orElse(0);
        if (maxSequence >= PLAN_NO_MAX_SEQUENCE) {
            throw invalidParamException("当天计划号流水已达到 999，请确认是否需要调整计划号规则");
        }
        return prefix + String.format("%03d", maxSequence + 1);
    }

    private int parsePlanNoSequence(String planNo, String prefix) {
        if (StrUtil.isBlank(planNo) || !planNo.startsWith(prefix)) {
            return 0;
        }
        String sequence = planNo.substring(prefix.length());
        if (sequence.length() != 3 || !sequence.chars().allMatch(Character::isDigit)) {
            return 0;
        }
        return Integer.parseInt(sequence);
    }

    private void normalizeSaveReq(HcPlanOrderSaveReqVO reqVO, HcPlanOrderDO oldPlanOrder) {
        if (reqVO.getPlanStatus() == null || reqVO.getPlanStatus().isBlank()) {
            reqVO.setPlanStatus(STATUS_DRAFT);
        }
        // 批次状态由服务端维护，编辑计划不能把已开工批次重置为未生成。
        reqVO.setBatchStatus(oldPlanOrder == null ? BATCH_STATUS_NOT_GEN
                : firstNotBlank(oldPlanOrder.getBatchStatus(), BATCH_STATUS_NOT_GEN));
        if (reqVO.getFgDeductQty() == null) {
            reqVO.setFgDeductQty(BigDecimal.ZERO);
        }
        if (reqVO.getTargetQty() != null) {
            reqVO.setNetPlanQty(reqVO.getTargetQty().subtract(reqVO.getFgDeductQty()).max(BigDecimal.ZERO));
        }
        if (reqVO.getOperationCount() == null) {
            reqVO.setOperationCount(reqVO.getOperations() == null ? 0 : reqVO.getOperations().size());
        }
        syncMotherSnapshotFromProduct(reqVO);
        normalizeJsonFields(reqVO);
        normalizePlanDates(reqVO);
        validateProductionDate(reqVO);
        validatePlanMinimalFields(reqVO);
        validateSaleOrderLink(reqVO);
        if (reqVO.getOperations() != null) {
            reqVO.setOperationCount(reqVO.getOperations().size());
            for (int index = 0; index < reqVO.getOperations().size(); index++) {
                HcPlanOrderOperationReqVO op = reqVO.getOperations().get(index);
                op.setOpSeq(index + 1);
                op.setSort(index);
                op.setOperationStatus(STATUS_RELEASED.equals(reqVO.getPlanStatus()) ? OP_STATUS_RELEASED : OP_STATUS_NOT_RELEASED);
                BigDecimal rate = op.getYieldRate() == null || op.getYieldRate().signum() == 0
                        ? BigDecimal.ONE : op.getYieldRate();
                op.setRequiredQty(defaultDecimal(reqVO.getNetPlanQty()).multiply(rate)
                        .setScale(6, java.math.RoundingMode.HALF_UP));
            }
        }
        normalizeOperations(reqVO.getOperations(), reqVO.getPlanStatus());
        normalizeAndValidateManualRootBatchNo(reqVO, oldPlanOrder);
        normalizeInventoryLocks(reqVO.getInventoryLocks());
        refreshExternalInventoryLockStats(reqVO);
        refreshPlanRedundantFields(reqVO);
    }

    private void refreshPlanRedundantFields(HcPlanOrderSaveReqVO reqVO) {
        List<HcPlanOrderOperationReqVO> operations = reqVO.getOperations() == null ? List.of() : reqVO.getOperations();
        reqVO.setFrontProcessFlag(operations.stream().anyMatch(this::isFrontProcessOperation));
        reqVO.setPostProcessFlag(operations.stream().anyMatch(this::isPostProcessOperation));
        reqVO.setInventorySourceBatchNos(buildInventorySourceBatchNos(reqVO.getInventoryLocks()));
    }

    private String buildInventorySourceBatchNos(List<HcPlanOrderInventoryLockReqVO> inventoryLocks) {
        if (inventoryLocks == null || inventoryLocks.isEmpty()) {
            return "";
        }
        LinkedHashSet<String> batchNos = new LinkedHashSet<>();
        inventoryLocks.stream()
                .filter(Objects::nonNull)
                .map(lock -> firstNotBlank(lock.getSourceBatchNo(), lock.getBatchNo(), lock.getLotNo()))
                .filter(StrUtil::isNotBlank)
                .map(this::normalizeInventoryMotherBatchNo)
                .filter(StrUtil::isNotBlank)
                .forEach(batchNos::add);
        return String.join("、", batchNos);
    }

    private String normalizeInventoryMotherBatchNo(String batchNo) {
        String normalized = StrUtil.trimToEmpty(batchNo).toUpperCase(Locale.ROOT).replace(" ", "");
        if (StrUtil.isBlank(normalized)) {
            return "";
        }
        int glueSuffixIndex = normalized.indexOf("-J");
        if (glueSuffixIndex > 0) {
            normalized = normalized.substring(0, glueSuffixIndex);
        }
        Matcher matcher = INVENTORY_MOTHER_BATCH_PREFIX_PATTERN.matcher(normalized);
        return matcher.matches() ? matcher.group(1) : normalized;
    }

    private boolean isFrontProcessOperation(HcPlanOrderOperationReqVO operation) {
        return operation != null && isFrontProcessOperation(operation.getOpCode(), operation.getOpName());
    }

    private boolean isPostProcessOperation(HcPlanOrderOperationReqVO operation) {
        return operation != null && isPostProcessOperation(operation.getOpCode(), operation.getOpName());
    }

    private boolean isFrontProcessOperation(String operationCode, String operationName) {
        String opCode = StrUtil.trimToEmpty(operationCode).toUpperCase(Locale.ROOT);
        String opName = StrUtil.trimToEmpty(operationName);
        return FRONT_PROCESS_OPERATION_CODES.contains(opCode)
                || opName.contains("配方")
                || opName.contains("配料")
                || opName.contains("湿法")
                || opName.contains("磨皮")
                || opName.contains("粗磨")
                || opName.contains("二磨");
    }

    private boolean isPostProcessOperation(String operationCode, String operationName) {
        String opCode = StrUtil.trimToEmpty(operationCode).toUpperCase(Locale.ROOT);
        String opName = StrUtil.trimToEmpty(operationName);
        return POST_PROCESS_OPERATION_CODES.contains(opCode)
                || opName.contains("粘胶1")
                || opName.contains("粘胶2")
                || opName.contains("粘双面胶")
                || opName.contains("背胶")
                || opName.contains("分切")
                || opName.contains("压槽")
                || opName.contains("裁切")
                || opName.contains("裁圆");
    }

    private void normalizePlanDates(HcPlanOrderSaveReqVO reqVO) {
        if (reqVO.getProductionStartDate() == null) {
            throw invalidParamException("计划开始日期不能为空");
        }
        reqVO.setPlanDate(reqVO.getProductionStartDate());
    }

    private void normalizeAndValidateManualRootBatchNo(HcPlanOrderSaveReqVO reqVO,
                                                        HcPlanOrderDO oldPlanOrder) {
        String manualBatchNo = StrUtil.trimToNull(reqVO.getBatchNo());
        if (HcRootBatchReservation.isReplanning(oldPlanOrder)) {
            reqVO.setBatchRuleId(null);
            reqVO.setBatchRuleCode(null);
            reqVO.setBatchRuleVersion(null);
        }
        if (HcRootBatchReservation.isReserved(oldPlanOrder) && StrUtil.isNotBlank(manualBatchNo)
                && !StrUtil.equalsIgnoreCase(manualBatchNo, oldPlanOrder.getBatchNo())) {
            throw invalidParamException("母批批号已在下发时锁定，请先撤回未开工计划，重新下发时重新计算");
        }
        if (shouldKeepExistingRootBatch(oldPlanOrder, manualBatchNo)) {
            keepExistingRootBatch(reqVO, oldPlanOrder);
            return;
        }
        HcPlanOrderOperationReqVO firstOperation = resolveFirstOperation(reqVO.getOperations());
        if (StrUtil.isBlank(manualBatchNo)) {
            reqVO.setBatchNo(null);
            if (isFormulaOperation(firstOperation)) {
                HcPlanOrderBatchPreviewRespVO preview = previewRootBatchNo(buildBatchPreviewReq(reqVO, firstOperation));
                if (StrUtil.isBlank(preview.getBatchNo())) {
                    throw invalidParamException("当前计划未能预览产品批号，请检查产品型号、工艺路线和批号规则");
                }
                applyRootBatchRuleSnapshot(reqVO, preview);
            }
            return;
        }
        manualBatchNo = manualBatchNo.toUpperCase(Locale.ROOT);
        reqVO.setBatchNo(manualBatchNo);
        if (!isFormulaOperation(firstOperation)) {
            throw invalidParamException("只有从配料工序开始的计划允许人工定义产品批号");
        }
        HcPlanOrderBatchPreviewRespVO preview = previewRootBatchNo(buildBatchPreviewReq(reqVO, firstOperation));
        validateManualRootBatchMatchesRule(reqVO, preview, manualBatchNo);
        validateManualRootBatchChange(reqVO, oldPlanOrder, manualBatchNo);
        validateManualRootBatchUnique(reqVO, manualBatchNo);
    }

    private boolean shouldKeepExistingRootBatch(HcPlanOrderDO oldPlanOrder, String requestBatchNo) {
        if (HcRootBatchReservation.isReplanning(oldPlanOrder)
                || (oldPlanOrder != null && STATUS_DRAFT.equals(oldPlanOrder.getPlanStatus())
                    && !HcRootBatchReservation.isReserved(oldPlanOrder))) return false;
        if (oldPlanOrder == null) {
            return false;
        }
        boolean hasExistingBatchState = StrUtil.isNotBlank(oldPlanOrder.getBatchNo())
                || StrUtil.isNotBlank(oldPlanOrder.getProductionBatchNo())
                || oldPlanOrder.getBatchRuleId() != null
                || StrUtil.isNotBlank(oldPlanOrder.getBatchRuleCode())
                || oldPlanOrder.getBatchRuleVersion() != null;
        if (!hasExistingBatchState) {
            return false;
        }
        if (StrUtil.isBlank(requestBatchNo)) {
            return true;
        }
        return StrUtil.equalsIgnoreCase(StrUtil.trim(oldPlanOrder.getBatchNo()), requestBatchNo)
                || StrUtil.equalsIgnoreCase(StrUtil.trim(oldPlanOrder.getProductionBatchNo()), requestBatchNo);
    }

    private void keepExistingRootBatch(HcPlanOrderSaveReqVO reqVO, HcPlanOrderDO oldPlanOrder) {
        reqVO.setBatchNo(oldPlanOrder.getBatchNo());
        reqVO.setBatchRuleId(oldPlanOrder.getBatchRuleId());
        reqVO.setBatchRuleCode(oldPlanOrder.getBatchRuleCode());
        reqVO.setBatchRuleVersion(oldPlanOrder.getBatchRuleVersion());
    }

    private void validateManualRootBatchMatchesRule(HcPlanOrderSaveReqVO reqVO,
                                                    HcPlanOrderBatchPreviewRespVO preview,
                                                    String manualBatchNo) {
        HcLotRuleDO rule = hcLotRuleMapper.selectById(preview.getRuleId());
        if (rule == null) {
            throw invalidParamException("当前匹配的批号规则不存在，请刷新后重试");
        }
        // 生产计划的未执行草稿允许选定批号；不修改全局规则的人工覆盖开关。
        // 仍按匹配规则解析格式，并在保存及正式下发时校验占用。
        HcLotRuleParseReqVO parseReq = new HcLotRuleParseReqVO();
        parseReq.setRuleId(rule.getId());
        parseReq.setLotNo(manualBatchNo);
        hcLotRuleService.parseLotNo(parseReq);
        applyRootBatchRuleSnapshot(reqVO, preview);
    }

    private void applyRootBatchRuleSnapshot(HcPlanOrderSaveReqVO reqVO,
                                            HcPlanOrderBatchPreviewRespVO preview) {
        reqVO.setBatchRuleId(preview.getRuleId());
        reqVO.setBatchRuleCode(preview.getRuleCode());
        reqVO.setBatchRuleVersion(preview.getRuleVersion());
    }

    private HcPlanOrderBatchPreviewReqVO buildBatchPreviewReq(HcPlanOrderSaveReqVO reqVO,
                                                               HcPlanOrderOperationReqVO firstOperation) {
        HcPlanOrderBatchPreviewReqVO previewReqVO = new HcPlanOrderBatchPreviewReqVO();
        previewReqVO.setId(reqVO.getId());
        previewReqVO.setPlanDate(reqVO.getPlanDate());
        previewReqVO.setProductionStartDate(reqVO.getProductionStartDate());
        previewReqVO.setMaterialCode(reqVO.getMaterialCode());
        previewReqVO.setCategoryCode(reqVO.getCategoryCode());
        previewReqVO.setProdType(reqVO.getProdType());
        previewReqVO.setModelCode(reqVO.getModelCode());
        previewReqVO.setMotherModelCode(reqVO.getMotherModelCode());
        previewReqVO.setBatchRuleId(reqVO.getBatchRuleId());
        previewReqVO.setBatchRuleCode(reqVO.getBatchRuleCode());
        if (firstOperation != null) {
            previewReqVO.setOpCode(firstOperation.getOpCode());
            previewReqVO.setOpName(firstOperation.getOpName());
            previewReqVO.setWorkCenterId(firstOperation.getWorkCenterId());
        }
        return previewReqVO;
    }

    private void validateManualRootBatchChange(HcPlanOrderSaveReqVO reqVO,
                                               HcPlanOrderDO oldPlan,
                                               String manualBatchNo) {
        if (reqVO.getId() == null || oldPlan == null) {
            return;
        }
        String oldBatchNo = StrUtil.trimToNull(oldPlan.getBatchNo());
        if (Objects.equals(oldBatchNo, manualBatchNo)) {
            return;
        }
        boolean generated = StrUtil.isNotBlank(oldPlan.getProductionBatchNo())
                || BATCH_STATUS_GENERATED.equalsIgnoreCase(oldPlan.getBatchStatus())
                || hasAnyOperationReport(oldPlan.getId());
        if (generated) {
            throw invalidParamException("生产批号已生成或计划已报工，不能修改人工产品批号");
        }
    }

    private void validateManualRootBatchUnique(HcPlanOrderSaveReqVO reqVO, String manualBatchNo) {
        Long samePlanCount = hcPlanOrderMapper.selectCount(new LambdaQueryWrapperX<HcPlanOrderDO>()
                .neIfPresent(HcPlanOrderDO::getId, reqVO.getId())
                .and(wrapper -> wrapper.eq(HcPlanOrderDO::getBatchNo, manualBatchNo)
                        .or()
                        .eq(HcPlanOrderDO::getProductionBatchNo, manualBatchNo)));
        if (samePlanCount != null && samePlanCount > 0) {
            throw invalidParamException("产品批号已被其他生产计划使用：" + manualBatchNo);
        }

        HcLotInstanceDO existedLot = hcLotInstanceMapper.selectOne(new LambdaQueryWrapperX<HcLotInstanceDO>()
                .and(wrapper -> wrapper.eq(HcLotInstanceDO::getLotNo, manualBatchNo)
                        .or()
                        .eq(HcLotInstanceDO::getProductionBatchNo, manualBatchNo))
                .last("LIMIT 1"));
        if (existedLot != null
                && (reqVO.getId() == null || !Objects.equals(existedLot.getPlanId(), reqVO.getId()))) {
            throw invalidParamException("产品批号已存在批次实例：" + manualBatchNo);
        }

        Long sameReportCount = hcProcessReportMapper.selectCount(new LambdaQueryWrapperX<HcProcessReportDO>()
                .neIfPresent(HcProcessReportDO::getPlanId, reqVO.getId())
                .and(wrapper -> wrapper.eq(HcProcessReportDO::getBatchNo, manualBatchNo)
                        .or()
                        .eq(HcProcessReportDO::getProductionBatchNo, manualBatchNo)));
        if (sameReportCount != null && sameReportCount > 0) {
            throw invalidParamException("产品批号已存在报工记录：" + manualBatchNo);
        }
    }

    private boolean hasAnyOperationReport(Long planId) {
        Long reportCount = hcProcessReportMapper.selectCount(new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getPlanId, planId));
        return reportCount != null && reportCount > 0;
    }

    private HcPlanOrderOperationReqVO resolveFirstOperation(List<HcPlanOrderOperationReqVO> operations) {
        if (operations == null || operations.isEmpty()) {
            return null;
        }
        return operations.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(item ->
                        item.getSort() == null
                                ? (item.getOpSeq() == null ? Integer.MAX_VALUE : item.getOpSeq())
                                : item.getSort()))
                .orElse(null);
    }

    private boolean isFormulaOperation(HcPlanOrderOperationReqVO operation) {
        if (operation == null) {
            return false;
        }
        return isFormulaOperation(operation.getOpCode(), operation.getOpName());
    }

    private boolean isFormulaOperation(String operationCode, String operationName) {
        String opCode = StrUtil.trimToEmpty(operationCode).toUpperCase(Locale.ROOT);
        String opName = StrUtil.trimToEmpty(operationName);
        return OP_CODE_FORMULA.equals(opCode) || "配料".equals(opName);
    }

    private void syncMotherSnapshotFromProduct(HcPlanOrderSaveReqVO reqVO) {
        reqVO.setMotherMaterialId(reqVO.getMaterialId());
        reqVO.setMotherMaterialCode(reqVO.getMaterialCode());
        reqVO.setMotherMaterialName(reqVO.getMaterialName());
        reqVO.setMotherModelId(reqVO.getModelId());
        reqVO.setMotherModelCode(reqVO.getModelCode());
        reqVO.setMotherModelName(reqVO.getModelName());
    }

    private void normalizeJsonFields(HcPlanOrderSaveReqVO reqVO) {
        if (reqVO.getRouteSnapshotJson() != null && reqVO.getRouteSnapshotJson().isBlank()) {
            reqVO.setRouteSnapshotJson(null);
        }
        if (reqVO.getSalesOrderSnapshotJson() != null && reqVO.getSalesOrderSnapshotJson().isBlank()) {
            reqVO.setSalesOrderSnapshotJson(null);
        }
    }

    private void validateProductionDate(HcPlanOrderSaveReqVO reqVO) {
        if (reqVO.getProductionStartDate() != null
                && reqVO.getProductionEndDate() != null
                && reqVO.getProductionStartDate().isAfter(reqVO.getProductionEndDate())) {
            throw invalidParamException("生产开始日期不能晚于生产结束日期");
        }
    }

    private void validatePlanMinimalFields(HcPlanOrderSaveReqVO reqVO) {
        if (reqVO.getOperations() == null || reqVO.getOperations().isEmpty()) {
            throw invalidParamException("请至少维护一条工位任务");
        }
        boolean hasFinishedModel = reqVO.getModelId() != null || StrUtil.isNotBlank(reqVO.getModelCode());
        boolean hasMotherModel = reqVO.getMotherModelId() != null || StrUtil.isNotBlank(reqVO.getMotherModelCode());
        if (!hasFinishedModel && !hasMotherModel) {
            throw invalidParamException("产品型号不能为空");
        }
    }

    private void validateReservedRootBatchIdentity(HcPlanOrderDO oldPlan, HcPlanOrderSaveReqVO request) {
        if (!HcRootBatchReservation.isReserved(oldPlan)) {
            return;
        }
        HcPlanOrderOperationReqVO first = resolveFirstOperation(request.getOperations());
        HcPlanOrderOperationDO oldFirst = hcPlanOrderOperationMapper.selectListByPlanId(oldPlan.getId()).stream()
                .min(Comparator.comparing(HcPlanOrderOperationDO::getOpSeq,
                        Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(HcPlanOrderOperationDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(HcPlanOrderOperationDO::getId))
                .orElse(null);
        if (!Objects.equals(oldPlan.getProductionStartDate(), request.getProductionStartDate())
                || !Objects.equals(oldPlan.getCategoryCode(), request.getCategoryCode())
                || !Objects.equals(oldPlan.getProdType(), request.getProdType())
                || !Objects.equals(oldPlan.getRouteId(), request.getRouteId())
                || !StrUtil.equals(StrUtil.trimToEmpty(oldPlan.getInventorySourceBatchNos()),
                        StrUtil.trimToEmpty(request.getInventorySourceBatchNos()))
                || !isFormulaOperation(first) || oldFirst == null
                || !Objects.equals(oldFirst.getWorkCenterId(), first.getWorkCenterId())) {
            throw invalidParamException("母批批号已锁定，不能修改计划开始日期、产品类别、生产类型、工艺路线、库存批次来源或配料工作中心；请先撤回未开工计划后修改并重新下发");
        }
    }

    private void validateSaleOrderLink(HcPlanOrderSaveReqVO reqVO) {
        if (reqVO.getSalesOrderId() == null) {
            return;
        }
        PlanSaleOrderDO saleOrder = planSaleOrderMapper.selectById(reqVO.getSalesOrderId());
        if (saleOrder == null) {
            throw invalidParamException("销售订单不存在");
        }
        if (!isPlanSelectableSaleOrderStatus(saleOrder.getStatus())) {
            throw invalidParamException("只能挂接已审核、未完成且未关闭的销售订单");
        }
        if (saleOrder.getRemainQty() == null || saleOrder.getRemainQty().compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("销售订单欠交量必须大于 0");
        }
        if (saleOrder.getMaterialId() == null || saleOrder.getMaterialCode() == null || saleOrder.getMaterialCode().isBlank()) {
            throw invalidParamException("销售订单未维护生产料号，不能直接生成排产计划");
        }
        HcMaterialDO material = hcMaterialMapper.selectById(saleOrder.getMaterialId());
        if (material == null || !Boolean.TRUE.equals(material.getMesSelectVisible())) {
            throw invalidParamException("该物料未设置为 MES产品，请先到物料主数据维护");
        }
        reqVO.setSourceType("SALES_ORDER");
        reqVO.setPlanMode("MTO");
        reqVO.setSalesOrderNo(saleOrder.getOrderNo());
        reqVO.setSalesOrderErpNo(saleOrder.getErpNo());
        reqVO.setSalesOrderLineNo(saleOrder.getOrderLineNo());
        reqVO.setCustomerId(saleOrder.getCustomerId());
        reqVO.setCustomerName(saleOrder.getCustomerName());
        reqVO.setOrderDueQty(saleOrder.getRemainQty());
        reqVO.setOrderDueUnitId(firstNonNull(saleOrder.getUnitId(), material.getBaseUnitId()));
        reqVO.setOrderDueUnitCode(firstNotBlank(saleOrder.getUnitCode(), saleOrder.getUnit(), material.getBaseUnitCode(), material.getBaseUom()));
        reqVO.setOrderDueUnitName(firstNotBlank(saleOrder.getUnitName(), material.getBaseUnitName()));
        reqVO.setTargetUnitId(firstNonNull(reqVO.getTargetUnitId(), reqVO.getOrderDueUnitId()));
        reqVO.setTargetUnitCode(firstNotBlank(reqVO.getTargetUnitCode(), reqVO.getOrderDueUnitCode(), reqVO.getTargetUom()));
        reqVO.setTargetUnitName(firstNotBlank(reqVO.getTargetUnitName(), reqVO.getOrderDueUnitName()));
        reqVO.setTargetUom(firstNotBlank(reqVO.getTargetUom(), reqVO.getTargetUnitCode(), reqVO.getOrderDueUnitCode()));
        reqVO.setSalesOrderDeliveryDate(saleOrder.getDeliveryDate());
    }

    private boolean isPlanSelectableSaleOrderStatus(String status) {
        return "APPROVED".equals(status) || "PART_PLANNED".equals(status);
    }

    private BigDecimal sumLockQty(List<HcPlanOrderInventoryLockReqVO> inventoryLocks,
                                  Long currentPlanId, String currentPlanNo) {
        if (inventoryLocks == null || inventoryLocks.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return inventoryLocks.stream()
                .filter(item -> isExternalSourceInventoryLock(item, currentPlanId, currentPlanNo))
                .map(HcPlanOrderInventoryLockReqVO::getLockQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void refreshExternalInventoryLockStats(HcPlanOrderSaveReqVO reqVO) {
        List<HcPlanOrderOperationReqVO> operations = reqVO.getOperations();
        List<HcPlanOrderInventoryLockReqVO> inventoryLocks = reqVO.getInventoryLocks();
        reqVO.setTotalLockQty(sumLockQty(inventoryLocks, reqVO.getId(), reqVO.getPlanNo()));
        if (operations == null || operations.isEmpty()) {
            return;
        }
        Map<String, String> operationKeyAliasMap = buildOperationKeyAliasMap(operations);
        Map<String, BigDecimal> lockQtyMap = new LinkedHashMap<>();
        if (inventoryLocks != null) {
            inventoryLocks.stream()
                    .filter(item -> item != null && !"FG".equals(item.getLockType()))
                    .filter(item -> isExternalSourceInventoryLock(item, reqVO.getId(), reqVO.getPlanNo()))
                    .forEach(item -> {
                        String operationKey = lockOperationKey(item);
                        if (operationKey != null) {
                            String primaryKey = operationKeyAliasMap.getOrDefault(operationKey, operationKey);
                            lockQtyMap.merge(primaryKey, defaultDecimal(item.getLockQty()), BigDecimal::add);
                        }
                    });
        }
        operations.forEach(operation -> {
            String operationKey = primaryOperationKey(operation);
            BigDecimal lockedQty = operationKey == null ? BigDecimal.ZERO : lockQtyMap.getOrDefault(operationKey, BigDecimal.ZERO);
            operation.setLockedQty(lockedQty);
            operation.setHasLock(lockedQty.compareTo(BigDecimal.ZERO) > 0);
        });
    }

    private Map<String, String> buildOperationKeyAliasMap(List<HcPlanOrderOperationReqVO> operations) {
        Map<String, String> aliasMap = new LinkedHashMap<>();
        for (HcPlanOrderOperationReqVO operation : operations) {
            String primaryKey = primaryOperationKey(operation);
            if (primaryKey == null) {
                continue;
            }
            lockOperationKeys(operation).forEach(key -> aliasMap.put(key, primaryKey));
        }
        return aliasMap;
    }

    private String lockOperationKey(HcPlanOrderInventoryLockReqVO lock) {
        if (lock.getPlanOperationId() != null && lock.getPlanOperationId() > 0) {
            return "ID:" + lock.getPlanOperationId();
        }
        if (StrUtil.isNotBlank(lock.getTargetOpCode())) {
            return "CODE:" + lock.getTargetOpCode().trim();
        }
        if (StrUtil.isNotBlank(lock.getTargetOpName())) {
            return "NAME:" + lock.getTargetOpName().trim();
        }
        return null;
    }

    private String primaryOperationKey(HcPlanOrderOperationReqVO operation) {
        if (operation.getId() != null && operation.getId() > 0) {
            return "ID:" + operation.getId();
        }
        if (StrUtil.isNotBlank(operation.getOpCode())) {
            return "CODE:" + operation.getOpCode().trim();
        }
        if (StrUtil.isNotBlank(operation.getOpName())) {
            return "NAME:" + operation.getOpName().trim();
        }
        return null;
    }

    private List<String> lockOperationKeys(HcPlanOrderOperationReqVO operation) {
        List<String> keys = new ArrayList<>(3);
        if (operation.getId() != null && operation.getId() > 0) {
            keys.add("ID:" + operation.getId());
        }
        if (StrUtil.isNotBlank(operation.getOpCode())) {
            keys.add("CODE:" + operation.getOpCode().trim());
        }
        if (StrUtil.isNotBlank(operation.getOpName())) {
            keys.add("NAME:" + operation.getOpName().trim());
        }
        return keys;
    }

    private boolean isExternalSourceInventoryLock(HcPlanOrderInventoryLockReqVO lock,
                                                  Long currentPlanId, String currentPlanNo) {
        if (lock == null) {
            return false;
        }
        if (StrUtil.isNotBlank(currentPlanNo) && StrUtil.isNotBlank(lock.getSourcePlanNo())) {
            return !StrUtil.equalsIgnoreCase(currentPlanNo.trim(), lock.getSourcePlanNo().trim());
        }
        if (currentPlanId != null && lock.getSourcePlanId() != null) {
            return !Objects.equals(currentPlanId, lock.getSourcePlanId());
        }
        return true;
    }

    private void normalizeOperations(List<HcPlanOrderOperationReqVO> operations, String planStatus) {
        if (operations == null) {
            return;
        }
        operations.forEach(item -> {
            if (item.getLockedQty() == null) {
                item.setLockedQty(BigDecimal.ZERO);
            }
            if (item.getDispatchQty() == null) {
                item.setDispatchQty(BigDecimal.ZERO);
            }
            if (item.getHasLock() == null) {
                item.setHasLock(Boolean.FALSE);
            }
            if (item.getOperationStatus() == null || item.getOperationStatus().isBlank()) {
                item.setOperationStatus(STATUS_RELEASED.equals(planStatus) ? OP_STATUS_RELEASED : OP_STATUS_NOT_RELEASED);
            } else if (STATUS_DRAFT.equals(item.getOperationStatus()) || "PLANNED".equals(item.getOperationStatus())) {
                item.setOperationStatus(STATUS_RELEASED.equals(planStatus) ? OP_STATUS_RELEASED : OP_STATUS_NOT_RELEASED);
            } else if (STATUS_RELEASED.equals(item.getOperationStatus())) {
                item.setOperationStatus(OP_STATUS_RELEASED);
            } else if ("DONE".equals(item.getOperationStatus())) {
                item.setOperationStatus("FINISHED");
            } else if (STATUS_RELEASED.equals(planStatus) && OP_STATUS_NOT_RELEASED.equals(item.getOperationStatus())) {
                item.setOperationStatus(OP_STATUS_RELEASED);
            }
            if (item.getSort() == null) {
                item.setSort(item.getOpSeq());
            }
            item.setUnitCode(firstNotBlank(item.getUnitCode(), item.getUom()));
            item.setUom(firstNotBlank(item.getUom(), item.getUnitCode()));
        });
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        if (values == null) {
            return null;
        }
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private void normalizeInventoryLocks(List<HcPlanOrderInventoryLockReqVO> inventoryLocks) {
        if (inventoryLocks == null) {
            return;
        }
        inventoryLocks.removeIf(item ->
                item == null ||
                (item.getStockId() == null && item.getNgPieceId() == null) ||
                item.getLockQty() == null);
        inventoryLocks.forEach(item -> {
            if (item.getLockStatus() == null || item.getLockStatus().isBlank()) {
                item.setLockStatus(LOCK_STATUS_ACTIVE);
            }
            if (item.getPlanOperationId() != null && item.getPlanOperationId() <= 0) {
                item.setPlanOperationId(null);
            }
            item.setMaterialCode(firstNotBlank(item.getMaterialCode(), ""));
            item.setMaterialName(firstNotBlank(item.getMaterialName(), ""));
            item.setUnitCode(firstNotBlank(item.getUnitCode(), item.getUom()));
            item.setUom(firstNotBlank(item.getUom(), item.getUnitCode()));
        });
    }

    private List<HcPlanOrderOperationDO> createOperations(Long planId, HcPlanOrderDO planOrder, List<HcPlanOrderOperationReqVO> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        List<HcPlanOrderOperationDO> createList = BeanUtils.toBean(list, HcPlanOrderOperationDO.class);
        createList.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setPlanId(planId);
            fillOperationPlanSnapshot(item, planOrder);
        });
        hcPlanOrderOperationMapper.insertBatch(createList);
        return hcPlanOrderOperationMapper.selectListByPlanId(planId);
    }

    private void createInventoryLocks(Long planId, HcPlanOrderDO planOrder,
                                      List<HcPlanOrderInventoryLockReqVO> list,
                                      List<HcPlanOrderOperationDO> operations) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<HcPlanOrderInventoryLockDO> createList = BeanUtils.toBean(list, HcPlanOrderInventoryLockDO.class);
        createList.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setPlanId(planId);
            fillInventoryLockSnapshot(item, planOrder, operations);
        });
        hcPlanOrderInventoryLockMapper.insertBatch(createList);
        lockInventoryLocks(createList, "创建生产计划挂接中间边库");
    }

    private List<HcPlanOrderOperationDO> updateOperations(Long planId, HcPlanOrderDO planOrder, List<HcPlanOrderOperationReqVO> list) {
        List<HcPlanOrderOperationDO> dbList = hcPlanOrderOperationMapper.selectListByPlanId(planId);
        if (list == null) {
            list = List.of();
        }

        Set<Long> reqIds = list.stream()
                .map(HcPlanOrderOperationReqVO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> existingIds = dbList.stream().map(HcPlanOrderOperationDO::getId).collect(Collectors.toSet());
        if (!existingIds.containsAll(reqIds)) throw invalidParamException("工位任务已变更或不属于当前计划，请刷新后重试");

        Set<Long> deleteIds = dbList.stream()
                .map(HcPlanOrderOperationDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcPlanOrderOperationMapper.deleteBatch(HcPlanOrderOperationDO::getId, deleteIds);
        }

        List<HcPlanOrderOperationDO> updateList = BeanUtils.toBean(list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .toList(), HcPlanOrderOperationDO.class);
        updateList.forEach(item -> {
            item.clean();
            item.setPlanId(planId);
            fillOperationPlanSnapshot(item, planOrder);
        });
        if (!updateList.isEmpty()) {
            hcPlanOrderOperationMapper.updateBatch(updateList);
        }

        List<HcPlanOrderOperationDO> createList = BeanUtils.toBean(list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .toList(), HcPlanOrderOperationDO.class);
        createList.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setPlanId(planId);
            fillOperationPlanSnapshot(item, planOrder);
        });
        if (!createList.isEmpty()) {
            hcPlanOrderOperationMapper.insertBatch(createList);
        }
        return hcPlanOrderOperationMapper.selectListByPlanId(planId);
    }

    private void fillOperationPlanSnapshot(HcPlanOrderOperationDO operation, HcPlanOrderDO planOrder) {
        if (operation == null || planOrder == null) {
            return;
        }
        operation.setMotherMaterialId(planOrder.getMotherMaterialId());
        operation.setMotherMaterialCode(planOrder.getMotherMaterialCode());
        operation.setMotherMaterialName(planOrder.getMotherMaterialName());
        operation.setMotherModelId(planOrder.getMotherModelId());
        operation.setMotherModelCode(planOrder.getMotherModelCode());
        operation.setMotherModelName(planOrder.getMotherModelName());
    }

    private void updateInventoryLocks(Long planId, List<HcPlanOrderInventoryLockReqVO> list,
                                      HcPlanOrderDO planOrder, List<HcPlanOrderOperationDO> operations) {
        List<HcPlanOrderInventoryLockDO> dbList = hcPlanOrderInventoryLockMapper.selectListByPlanId(planId);
        if (list == null) {
            list = List.of();
        }
        Map<Long, HcPlanOrderInventoryLockDO> dbMap = dbList.stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(HcPlanOrderInventoryLockDO::getId, item -> item, (a, b) -> a, LinkedHashMap::new));

        Set<Long> reqIds = list.stream()
                .map(HcPlanOrderInventoryLockReqVO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcPlanOrderInventoryLockDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            releaseInventoryLocks(deleteIds.stream()
                    .map(dbMap::get)
                    .filter(Objects::nonNull)
                    .toList(), "生产计划取消挂接中间边库");
            hcPlanOrderInventoryLockMapper.deleteBatch(HcPlanOrderInventoryLockDO::getId, deleteIds);
        }

        List<HcPlanOrderInventoryLockDO> relockList = new ArrayList<>();
        List<HcPlanOrderInventoryLockDO> updateList = BeanUtils.toBean(list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .toList(), HcPlanOrderInventoryLockDO.class);
        updateList.forEach(item -> {
            item.clean();
            item.setPlanId(planId);
            fillInventoryLockSnapshot(item, planOrder, operations);
            HcPlanOrderInventoryLockDO old = dbMap.get(item.getId());
            if (old == null) throw invalidParamException("库存挂接不属于当前计划，请刷新后重试");
            if (HcRootBatchReservation.isReplanning(planOrder) && STATUS_DRAFT.equals(planOrder.getPlanStatus())) {
                releaseInventoryLocks(List.of(old), "撤回草稿重新计算库存需求");
                resetInventoryLockLifecycle(item);
                item.setLockStatus("PENDING");
                persistPendingInventoryLock(item);
                return;
            }
            if (old != null && "PENDING".equals(old.getLockStatus())) {
                resetInventoryLockLifecycle(item);
                relockList.add(item);
                return;
            }
            if (needInventoryRelock(old, item)) {
                if (old != null && defaultDecimal(old.getConsumedQty()).compareTo(BigDecimal.ZERO) > 0) {
                    throw invalidParamException("已消耗的挂接中间品不能调整来源或锁定数量，请人工释放剩余量后重新建计划");
                }
                releaseInventoryLocks(List.of(old), "生产计划调整挂接中间边库");
                resetInventoryLockLifecycle(item);
                relockList.add(item);
            } else {
                keepInventoryLockLifecycle(item, old);
            }
        });
        if (!updateList.isEmpty()) {
            hcPlanOrderInventoryLockMapper.updateBatch(updateList);
        }
        lockInventoryLocks(relockList, "生产计划调整挂接中间边库");

        List<HcPlanOrderInventoryLockDO> createList = BeanUtils.toBean(list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .toList(), HcPlanOrderInventoryLockDO.class);
        createList.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setPlanId(planId);
            fillInventoryLockSnapshot(item, planOrder, operations);
            if (HcRootBatchReservation.isReplanning(planOrder) && STATUS_DRAFT.equals(planOrder.getPlanStatus())) {
                resetInventoryLockLifecycle(item);
                item.setLockStatus("PENDING");
            }
        });
        if (!createList.isEmpty()) {
            hcPlanOrderInventoryLockMapper.insertBatch(createList);
            lockInventoryLocks(createList, "生产计划新增挂接中间边库");
        }
    }

    private void fillInventoryLockSnapshot(HcPlanOrderInventoryLockDO lock, HcPlanOrderDO planOrder,
                                           List<HcPlanOrderOperationDO> operations) {
        if (lock == null) {
            return;
        }
        if (planOrder != null) {
            lock.setTargetPlanNo(firstNotBlank(lock.getTargetPlanNo(), planOrder.getPlanNo()));
        }
        HcPlanOrderOperationDO targetOperation = resolveTargetOperation(lock, operations);
        if (targetOperation != null) {
            lock.setPlanOperationId(targetOperation.getId());
            lock.setTargetOpCode(firstNotBlank(lock.getTargetOpCode(), targetOperation.getOpCode()));
            lock.setTargetOpName(firstNotBlank(lock.getTargetOpName(), targetOperation.getOpName()));
        }
        lock.setSourceBatchNo(firstNotBlank(lock.getSourceBatchNo(), lock.getBatchNo(), lock.getLotNo()));
        lock.setMaterialCode(firstNotBlank(lock.getMaterialCode(), ""));
        lock.setMaterialName(firstNotBlank(lock.getMaterialName(), ""));
        lock.setConsumedQty(defaultDecimal(lock.getConsumedQty()));
        lock.setReleasedQty(defaultDecimal(lock.getReleasedQty()));
        lock.setRemainingQty(calculateRemainingLockQty(lock));
        lock.setUnitCode(firstNotBlank(lock.getUnitCode(), lock.getUom()));
        lock.setUom(firstNotBlank(lock.getUom(), lock.getUnitCode()));
        if (StrUtil.isBlank(lock.getLockStatus())) {
            lock.setLockStatus(LOCK_STATUS_ACTIVE);
        }
    }

    private void lockInventoryLocks(List<HcPlanOrderInventoryLockDO> locks, String remark) {
        if (locks == null || locks.isEmpty()) {
            return;
        }
        LocalDateTime operateTime = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        for (HcPlanOrderInventoryLockDO lock : locks) {
            if (!isActiveWipLock(lock)) {
                continue;
            }
            hcInvStockService.lockPlanWip(lock, lock.getRemainingQty(), operateTime, operatorId, operatorName,
                    firstNotBlank(remark, "生产计划挂接中间边库") + "；目标计划：" + firstNotBlank(lock.getTargetPlanNo(), "-")
                            + "；目标工序：" + firstNotBlank(lock.getTargetOpName(), lock.getTargetOpCode(), "-"));
        }
    }

    private void releaseInventoryLocks(List<HcPlanOrderInventoryLockDO> locks, String reason) {
        if (locks == null || locks.isEmpty()) {
            return;
        }
        LocalDateTime operateTime = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        for (HcPlanOrderInventoryLockDO lock : locks) {
            if (!isActiveWipLock(lock)) {
                continue;
            }
            hcInvStockService.releasePlanLockedWip(lock, lock.getRemainingQty(),
                    firstNotBlank(reason, "释放计划挂接中间边库") + "；目标计划：" + firstNotBlank(lock.getTargetPlanNo(), "-")
                            + "；目标工序：" + firstNotBlank(lock.getTargetOpName(), lock.getTargetOpCode(), "-"),
                    operateTime, operatorId, operatorName);
        }
    }

    private boolean needInventoryRelock(HcPlanOrderInventoryLockDO oldLock, HcPlanOrderInventoryLockDO newLock) {
        if (oldLock == null || newLock == null) {
            return false;
        }
        boolean oldActiveWip = isActiveWipLock(oldLock);
        boolean newActiveWip = isActiveWipLock(newLock);
        if (oldActiveWip != newActiveWip) {
            return oldActiveWip || newActiveWip;
        }
        if (!oldActiveWip) {
            return false;
        }
        return StrUtil.isBlank(oldLock.getLockTxnNo())
                || !Objects.equals(oldLock.getStockId(), newLock.getStockId())
                || defaultDecimal(oldLock.getLockQty()).compareTo(defaultDecimal(newLock.getLockQty())) != 0;
    }

    private boolean isActiveWipLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null || lock.getStockId() == null) {
            return false;
        }
        String stockType = firstNotBlank(lock.getLockType(), lock.getStockType());
        if (!STOCK_TYPE_WIP.equalsIgnoreCase(stockType)) {
            return false;
        }
        String status = firstNotBlank(lock.getLockStatus(), LOCK_STATUS_ACTIVE);
        if ("PENDING".equalsIgnoreCase(status)
                || LOCK_STATUS_CONSUMED.equalsIgnoreCase(status)
                || LOCK_STATUS_CANCELLED.equalsIgnoreCase(status)
                || LOCK_STATUS_RELEASED.equalsIgnoreCase(status)) {
            return false;
        }
        return defaultDecimal(lock.getRemainingQty()).compareTo(BigDecimal.ZERO) > 0;
    }

    private void resetInventoryLockLifecycle(HcPlanOrderInventoryLockDO lock) {
        if (lock == null) {
            return;
        }
        lock.setConsumedQty(BigDecimal.ZERO);
        lock.setReleasedQty(BigDecimal.ZERO);
        lock.setRemainingQty(calculateRemainingLockQty(lock));
        lock.setConsumeReportId(null);
        lock.setConsumeTime(null);
        lock.setReleaseTime(null);
        lock.setReleaseReason(null);
        lock.setLockTxnNo(null);
        lock.setConsumeTxnNo(null);
        lock.setReleaseTxnNo(null);
        lock.setLockStatus(LOCK_STATUS_ACTIVE);
    }

    private void keepInventoryLockLifecycle(HcPlanOrderInventoryLockDO lock, HcPlanOrderInventoryLockDO oldLock) {
        if (lock == null || oldLock == null) {
            return;
        }
        lock.setConsumedQty(defaultDecimal(oldLock.getConsumedQty()));
        lock.setReleasedQty(defaultDecimal(oldLock.getReleasedQty()));
        lock.setRemainingQty(defaultDecimal(oldLock.getRemainingQty()));
        lock.setConsumeReportId(oldLock.getConsumeReportId());
        lock.setConsumeTime(oldLock.getConsumeTime());
        lock.setReleaseTime(oldLock.getReleaseTime());
        lock.setReleaseReason(oldLock.getReleaseReason());
        lock.setLockTxnNo(oldLock.getLockTxnNo());
        lock.setConsumeTxnNo(oldLock.getConsumeTxnNo());
        lock.setReleaseTxnNo(oldLock.getReleaseTxnNo());
        lock.setLockStatus(firstNotBlank(oldLock.getLockStatus(), lock.getLockStatus(), LOCK_STATUS_ACTIVE));
    }

    private HcPlanOrderOperationDO resolveTargetOperation(HcPlanOrderInventoryLockDO lock,
                                                          List<HcPlanOrderOperationDO> operations) {
        if (lock == null || operations == null || operations.isEmpty()) {
            return null;
        }
        if (lock.getPlanOperationId() != null && lock.getPlanOperationId() > 0) {
            HcPlanOrderOperationDO matched = operations.stream()
                    .filter(item -> Objects.equals(item.getId(), lock.getPlanOperationId()))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        if (StrUtil.isNotBlank(lock.getTargetOpCode())) {
            HcPlanOrderOperationDO matched = operations.stream()
                    .filter(item -> StrUtil.equals(lock.getTargetOpCode(), item.getOpCode()))
                    .findFirst()
                    .orElse(null);
            if (matched != null) {
                return matched;
            }
        }
        if (StrUtil.isNotBlank(lock.getTargetOpName())) {
            return operations.stream()
                    .filter(item -> StrUtil.equals(lock.getTargetOpName(), item.getOpName()))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private BigDecimal calculateRemainingLockQty(HcPlanOrderInventoryLockDO lock) {
        BigDecimal lockQty = defaultDecimal(lock.getLockQty());
        BigDecimal remainingQty = lockQty
                .subtract(defaultDecimal(lock.getConsumedQty()))
                .subtract(defaultDecimal(lock.getReleasedQty()));
        return remainingQty.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : remainingQty;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String firstNotBlankStatic(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static final class PivotPieceIdentity {
        private String actualModelCode;
        private String actualSizeSpec;
        private final Map<Integer, String> stageIdentityKeys = new LinkedHashMap<>();

        private PivotPieceIdentity(String actualModelCode, String actualSizeSpec) {
            this.actualModelCode = actualModelCode;
            this.actualSizeSpec = actualSizeSpec;
        }

        private String identityKey() {
            return firstNotBlankStatic(actualModelCode, "-") + "|" + firstNotBlankStatic(actualSizeSpec, "-");
        }

        private void fillStageIdentityKeys(String planModel, String planSize) {
            String carry = firstNotBlankStatic(planModel, "-") + "|" + firstNotBlankStatic(planSize, "-");
            for (int index = 0; index < PIVOT_STAGE_CODES.size(); index++) {
                carry = firstNotBlankStatic(stageIdentityKeys.get(index), carry);
                stageIdentityKeys.put(index, carry);
            }
        }
    }

    private static final class PivotDisplayGroup {
        private final String key;
        private final String actualModelCode;
        private final String actualSizeSpec;
        private String variationStartStageCode;
        private int variationStartOrder;
        private final Set<String> pieceKeys;
        private boolean split;

        private PivotDisplayGroup(String key, String actualModelCode, String actualSizeSpec,
                                  String variationStartStageCode, int variationStartOrder,
                                  Set<String> pieceKeys, boolean split) {
            this.key = key;
            this.actualModelCode = actualModelCode;
            this.actualSizeSpec = actualSizeSpec;
            this.variationStartStageCode = variationStartStageCode;
            this.variationStartOrder = variationStartOrder;
            this.pieceKeys = pieceKeys;
            this.split = split;
        }

        private static PivotDisplayGroup single(HcPlanOrderDO plan, String segmentBatchNo) {
            String actualModel = StrUtil.blankToDefault(plan.getModelCode(), plan.getModelName());
            String actualSize = StrUtil.blankToDefault(plan.getSizeName(), plan.getSizeSpec());
            return new PivotDisplayGroup(
                    "ROW|" + String.valueOf(plan.getId()) + "|" + StrUtil.blankToDefault(segmentBatchNo, "-"),
                    actualModel,
                    actualSize,
                    "CUT_ROUND",
                    PIVOT_STAGE_CODES.size(),
                    new HashSet<>(),
                    false);
        }

        private String key() {
            return key;
        }

        private String actualModelCode() {
            return actualModelCode;
        }

        private String actualSizeSpec() {
            return actualSizeSpec;
        }

        private String variationStartStageCode() {
            return variationStartStageCode;
        }

        private void setVariationStartStageCode(String variationStartStageCode) {
            this.variationStartStageCode = variationStartStageCode;
        }

        private int variationStartOrder() {
            return variationStartOrder;
        }

        private void setVariationStartOrder(int variationStartOrder) {
            this.variationStartOrder = variationStartOrder;
        }

        private Set<String> pieceKeys() {
            return pieceKeys;
        }

        private boolean split() {
            return split;
        }

        private void setSplit(boolean split) {
            this.split = split;
        }
    }

}
