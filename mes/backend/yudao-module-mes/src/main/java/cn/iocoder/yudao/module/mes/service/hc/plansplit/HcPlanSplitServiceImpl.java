package cn.iocoder.yudao.module.mes.service.hc.plansplit;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderInventoryLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitCreateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitGraphRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.plansplit.HcPlanSplitDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.plansplit.HcPlanSplitOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.HcProcessReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstAllocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingFirstDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingSecondDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.plansplit.HcPlanSplitDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.plansplit.HcPlanSplitOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstAllocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingFirstDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingSecondDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.service.hc.nginventory.HcNgInventoryService;
import cn.iocoder.yudao.module.mes.service.hc.planorder.HcPlanOrderService;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcPlanSplitServiceImpl implements HcPlanSplitService {

    private static final String STATUS_CREATED = "CREATED";
    private static final String STATUS_PLAN_CREATED = "PLAN_CREATED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String REPORT_STATE_PLANNED_NOT_REPORTED = "PLANNED_NOT_REPORTED";
    private static final String REPORT_STATE_REPORTED_UNCONFIRMED = "REPORTED_UNCONFIRMED";
    private static final String REPORT_STATE_REPORTED_CONFIRMED = "REPORTED_CONFIRMED";
    private static final String STOCK_TYPE_WIP = "WIP";
    private static final String SPLIT_MODE_METER = "METER";
    private static final String SPLIT_MODE_PIECE = "PIECE";
    private static final String UNIT_METER = "m";
    private static final String UNIT_PCS = "pcs";
    private static final String TABLE_GRINDING_FIRST_ALLOCATION = "mes_sfc_grinding_first_allocation_detail";
    private static final String TABLE_GRINDING_FIRST_DETAIL = "mes_sfc_grinding_first_detail";
    private static final String TABLE_GRINDING_SECOND_DETAIL = "mes_sfc_grinding_second_detail";
    private static final String TABLE_ADHESIVE_REPORT = "mes_sfc_adhesive_report";
    private static final String TABLE_SLITTING_SLICE = "mes_sfc_slitting_slice_record";
    private static final String TABLE_PRESS_SLOT_REPORT = "mes_sfc_press_slot_report";
    private static final String TABLE_ADHESIVE2_REPORT = "mes_sfc_adhesive2_report";
    private static final String SOURCE_TYPE_SLITTING = "SLITTING";
    private static final String SOURCE_TYPE_PRESS_SLOT = "PRESS_SLOT";
    private static final String SOURCE_MENU_CODE_PRESS_SLOT = "PRESS_SLOT_REPORT";
    private static final String PROCESS_CODE_PRESS_SLOT = "PRESS_SLOT";
    private static final DateTimeFormatter SPLIT_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter REMARK_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern ADHESIVE_BATCH_SUFFIX_PATTERN = Pattern.compile("^(.*)-J\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern SLITTING_BATCH_SUFFIX_PATTERN = Pattern.compile("^(.*)-S\\d+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIECE_SERIAL_SUFFIX_PATTERN = Pattern.compile(
            "^([A-Z]\\d{2}[A-Z]\\d{3}[A-Z0-9]*[A-Z])\\d{3,}[AB]?$", Pattern.CASE_INSENSITIVE);

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcProcessReportMapper hcProcessReportMapper;
    @Resource
    private HcGrindingReportMapper hcGrindingReportMapper;
    @Resource
    private HcGrindingFirstDetailMapper hcGrindingFirstDetailMapper;
    @Resource
    private HcGrindingFirstAllocationMapper hcGrindingFirstAllocationMapper;
    @Resource
    private HcGrindingSecondDetailMapper hcGrindingSecondDetailMapper;
    @Resource
    private HcAdhesiveReportMapper hcAdhesiveReportMapper;
    @Resource
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;
    @Resource
    private HcPressSlotReportMapper hcPressSlotReportMapper;
    @Resource
    private HcAdhesive2ReportMapper hcAdhesive2ReportMapper;
    @Resource
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private HcNgInventoryService hcNgInventoryService;
    @Resource
    private HcInvStockMapper hcInvStockMapper;
    @Resource
    private HcPlanSplitOrderMapper hcPlanSplitOrderMapper;
    @Resource
    private HcPlanSplitDetailMapper hcPlanSplitDetailMapper;
    @Resource
    private HcPlanOrderService hcPlanOrderService;

    @Override
    public HcPlanSplitGraphRespVO getPlanSplitGraph(String planNo) {
        HcPlanOrderDO plan = getPlanByNo(planNo);
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(plan.getId());
        ReportFacts facts = loadFacts(plan);

        HcPlanSplitGraphRespVO graph = new HcPlanSplitGraphRespVO();
        graph.setPlanId(plan.getId());
        graph.setPlanNo(plan.getPlanNo());
        graph.setPlanDate(plan.getPlanDate());
        graph.setPlanStatus(plan.getPlanStatus());
        graph.setModelCode(plan.getModelCode());
        graph.setModelName(plan.getModelName());
        graph.setMaterialCode(plan.getMaterialCode());
        graph.setMaterialName(plan.getMaterialName());
        graph.setBatchNo(firstNotBlank(plan.getBatchNo(), plan.getProductionBatchNo(), plan.getParentProductionBatchNo()));
        graph.setTargetQty(plan.getTargetQty());
        graph.setTargetUom(firstNotBlank(plan.getTargetUom(), plan.getTargetUnitCode()));
        graph.setOperations(toOperationOptions(operations));
        graph.getNodes().addAll(buildOperationNodes(operations, facts));
        graph.getEdges().addAll(buildOperationEdges(operations));
        addSplitCandidateNodes(graph, operations, facts);
        return graph;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcPlanSplitCreateRespVO createPlanSplit(HcPlanSplitCreateReqVO reqVO) {
        HcPlanOrderDO sourcePlan = getPlanByNo(reqVO.getPlanNo());
        HcPlanSplitGraphRespVO graph = getPlanSplitGraph(reqVO.getPlanNo());
        HcPlanSplitGraphRespVO.Node node = graph.getNodes().stream()
                .filter(item -> StrUtil.equals(item.getNodeKey(), reqVO.getNodeKey()))
                .findFirst()
                .orElseThrow(() -> invalidParamException("拆批节点不存在，请刷新后重试"));
        if (!Boolean.TRUE.equals(node.getSplitable())) {
            throw invalidParamException("当前节点没有可拆余量，不能拆批");
        }
        BigDecimal splitQty = scale(defaultDecimal(reqVO.getSplitQty()));
        if (splitQty.compareTo(defaultDecimal(node.getRemainingQty())) > 0) {
            throw invalidParamException("拆出数量不能大于当前节点可拆余量 {}", node.getRemainingQty());
        }

        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(sourcePlan.getId());
        HcPlanOrderOperationDO startOperation = resolveStartOperation(reqVO, node, operations);
        HcPlanOrderOperationDO endOperation = resolveEndOperation(reqVO, operations);
        List<HcPlanOrderOperationDO> targetOperations = selectTargetOperations(operations, startOperation, endOperation);
        if (targetOperations.isEmpty()) {
            throw invalidParamException("未找到拆批新计划的工序范围，请检查开始/结束工序");
        }

        HcPlanSplitOrderDO splitOrder = buildSplitOrder(sourcePlan, node, startOperation, endOperation, reqVO, splitQty);
        hcPlanSplitOrderMapper.insert(splitOrder);

        List<HcPlanSplitDetailDO> details = buildSplitDetails(splitOrder.getId(), node, reqVO, splitQty);
        if (!details.isEmpty()) {
            hcPlanSplitDetailMapper.insertBatch(details);
        }

        HcPlanOrderSaveReqVO planReq = buildNewPlanReq(sourcePlan, targetOperations, splitQty, reqVO, details);
        Long newPlanId = hcPlanOrderService.createHcPlanOrder(planReq);
        HcPlanOrderDO newPlan = hcPlanOrderMapper.selectById(newPlanId);
        HcPlanOrderOperationDO newStartOperation = resolveNewPlanStartOperation(newPlanId, startOperation);
        applySplitTransferEffects(sourcePlan, startOperation, newPlan, newStartOperation, details, reqVO);
        splitOrder.setTargetPlanId(newPlanId);
        splitOrder.setTargetPlanNo(newPlan == null ? null : newPlan.getPlanNo());
        splitOrder.setSplitStatus(STATUS_PLAN_CREATED);
        hcPlanSplitOrderMapper.updateById(splitOrder);

        HcPlanSplitCreateRespVO respVO = new HcPlanSplitCreateRespVO();
        respVO.setSplitOrderId(splitOrder.getId());
        respVO.setNewPlanId(newPlanId);
        respVO.setNewPlanNo(newPlan == null ? null : newPlan.getPlanNo());
        respVO.setStatus(STATUS_PLAN_CREATED);
        respVO.setMessage("拆批成功，已生成新计划" + firstNotBlank(respVO.getNewPlanNo(), ""));
        return respVO;
    }

    private HcPlanOrderOperationDO resolveNewPlanStartOperation(Long newPlanId,
                                                                HcPlanOrderOperationDO sourceStartOperation) {
        if (newPlanId == null || sourceStartOperation == null) {
            return null;
        }
        return hcPlanOrderOperationMapper.selectListByPlanId(newPlanId).stream()
                .filter(item -> Objects.equals(item.getOpSeq(), sourceStartOperation.getOpSeq())
                        || (StrUtil.equals(item.getOpCode(), sourceStartOperation.getOpCode())
                        && StrUtil.equals(item.getOpName(), sourceStartOperation.getOpName())))
                .findFirst()
                .orElse(null);
    }

    private void applySplitTransferEffects(HcPlanOrderDO sourcePlan,
                                           HcPlanOrderOperationDO sourceStartOperation,
                                           HcPlanOrderDO newPlan,
                                           HcPlanOrderOperationDO newStartOperation,
                                           List<HcPlanSplitDetailDO> details,
                                           HcPlanSplitCreateReqVO reqVO) {
        if (sourcePlan == null || newPlan == null || details == null || details.isEmpty()) {
            return;
        }
        BigDecimal totalQty = details.stream()
                .map(HcPlanSplitDetailDO::getSplitQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String unit = firstNotBlank(reqVO.getUnit(), details.get(0).getUnit());
        String targetRemark = buildTargetTransferRemark(sourcePlan, totalQty, unit);
        HcPlanOrderDO planUpdate = new HcPlanOrderDO();
        planUpdate.setId(newPlan.getId());
        planUpdate.setRemark(appendRemark(newPlan.getRemark(), targetRemark));
        hcPlanOrderMapper.updateById(planUpdate);
        newPlan.setRemark(planUpdate.getRemark());
        markSourceOperationSplitOut(sourceStartOperation, newPlan, totalQty, unit);
        markNewStartOperationSource(newStartOperation, sourcePlan, sourceStartOperation, totalQty, unit);

        for (HcPlanSplitDetailDO detail : details) {
            if (detail == null || detail.getSourceId() == null) {
                continue;
            }
            if (StrUtil.equals(detail.getSourceTable(), TABLE_GRINDING_SECOND_DETAIL)) {
                appendGrindingSecondSplitRemark(sourcePlan, newPlan, detail, reqVO);
            } else if (StrUtil.equals(detail.getSourceTable(), TABLE_GRINDING_FIRST_DETAIL)) {
                appendGrindingFirstSplitRemark(sourcePlan, newPlan, detail, reqVO);
            } else if (StrUtil.equals(detail.getSourceTable(), TABLE_ADHESIVE_REPORT)) {
                applyAdhesiveReportSplitOut(sourcePlan, newPlan, detail, reqVO);
            } else if (StrUtil.equals(detail.getSourceTable(), TABLE_SLITTING_SLICE)) {
                transferSlittingSliceToNewPlan(sourcePlan, newPlan, newStartOperation, detail, reqVO);
            }
        }
    }

    private void markSourceOperationSplitOut(HcPlanOrderOperationDO sourceOperation,
                                             HcPlanOrderDO newPlan,
                                             BigDecimal splitQty,
                                             String unit) {
        if (sourceOperation == null || sourceOperation.getId() == null) {
            return;
        }
        HcPlanOrderOperationDO update = new HcPlanOrderOperationDO();
        update.setId(sourceOperation.getId());
        update.setSplitMark("SPLIT_OUT");
        update.setFinishRemark(appendRemark(sourceOperation.getFinishRemark(),
                "拆批转出 " + formatQtyUnit(splitQty, unit)
                        + " 至新计划 " + firstNotBlank(newPlan == null ? null : newPlan.getPlanNo(), "-")));
        hcPlanOrderOperationMapper.updateById(update);
    }

    private void markNewStartOperationSource(HcPlanOrderOperationDO newStartOperation,
                                             HcPlanOrderDO sourcePlan,
                                             HcPlanOrderOperationDO sourceOperation,
                                             BigDecimal splitQty,
                                             String unit) {
        if (newStartOperation == null || newStartOperation.getId() == null || sourcePlan == null) {
            return;
        }
        HcPlanOrderOperationDO update = new HcPlanOrderOperationDO();
        update.setId(newStartOperation.getId());
        update.setSplitMark("SPLIT_IN");
        update.setSourcePlanId(sourcePlan.getId());
        update.setSourcePlanNo(sourcePlan.getPlanNo());
        if (sourceOperation != null) {
            update.setSourcePlanOperationId(sourceOperation.getId());
            update.setSourceOperationCode(sourceOperation.getOpCode());
            update.setSourceOperationName(sourceOperation.getOpName());
        }
        update.setFinishRemark(appendRemark(newStartOperation.getFinishRemark(),
                "来源拆批计划 " + firstNotBlank(sourcePlan.getPlanNo(), "-")
                        + "，来源工序 " + firstNotBlank(sourceOperation == null ? null : sourceOperation.getOpName(), "-")
                        + "，转入 " + formatQtyUnit(splitQty, unit)));
        hcPlanOrderOperationMapper.updateById(update);
    }

    private void appendGrindingSecondSplitRemark(HcPlanOrderDO sourcePlan,
                                                 HcPlanOrderDO newPlan,
                                                 HcPlanSplitDetailDO splitDetail,
                                                 HcPlanSplitCreateReqVO reqVO) {
        HcGrindingSecondDetailDO detail = hcGrindingSecondDetailMapper.selectById(splitDetail.getSourceId());
        if (detail == null) {
            return;
        }
        HcGrindingSecondDetailDO update = new HcGrindingSecondDetailDO();
        update.setId(detail.getId());
        update.setRemark(appendRemark(detail.getRemark(), buildSourceTransferRemark(sourcePlan, newPlan,
                firstNonNull(splitDetail.getSplitQty(), BigDecimal.ZERO), firstNotBlank(reqVO.getUnit(), splitDetail.getUnit()),
                firstNonNull(detail.getOutputLength(), detail.getProcessLength(), splitDetail.getSplitQty()))));
        hcGrindingSecondDetailMapper.updateById(update);
    }

    private void appendGrindingFirstSplitRemark(HcPlanOrderDO sourcePlan,
                                                HcPlanOrderDO newPlan,
                                                HcPlanSplitDetailDO splitDetail,
                                                HcPlanSplitCreateReqVO reqVO) {
        HcGrindingFirstDetailDO detail = hcGrindingFirstDetailMapper.selectById(splitDetail.getSourceId());
        if (detail == null) {
            return;
        }
        HcGrindingFirstDetailDO update = new HcGrindingFirstDetailDO();
        update.setId(detail.getId());
        update.setRemark(appendRemark(detail.getRemark(), buildSourceTransferRemark(sourcePlan, newPlan,
                firstNonNull(splitDetail.getSplitQty(), BigDecimal.ZERO), firstNotBlank(reqVO.getUnit(), splitDetail.getUnit()),
                firstNonNull(detail.getOutputLength(), detail.getProcessLength(), splitDetail.getSplitQty()))));
        hcGrindingFirstDetailMapper.updateById(update);
    }

    private void applyAdhesiveReportSplitOut(HcPlanOrderDO sourcePlan,
                                             HcPlanOrderDO newPlan,
                                             HcPlanSplitDetailDO splitDetail,
                                             HcPlanSplitCreateReqVO reqVO) {
        HcAdhesiveReportDO report = hcAdhesiveReportMapper.selectById(splitDetail.getSourceId());
        if (report == null) {
            return;
        }
        BigDecimal splitQty = firstNonNull(splitDetail.getSplitQty(), BigDecimal.ZERO);
        HcAdhesiveReportDO update = new HcAdhesiveReportDO();
        update.setId(report.getId());
        update.setRemark(appendRemark(report.getRemark(), buildSourceTransferRemark(sourcePlan, newPlan,
                splitQty, firstNotBlank(reqVO.getUnit(), splitDetail.getUnit(), UNIT_METER),
                firstNonNull(report.getOutputLength(), report.getInputLength(), splitQty))));
        hcAdhesiveReportMapper.updateById(update);
        hcAdhesiveReportMapper.deductSlittingRemainingLength(report.getId(), splitQty);
    }

    private void transferSlittingSliceToNewPlan(HcPlanOrderDO sourcePlan,
                                                HcPlanOrderDO newPlan,
                                                HcPlanOrderOperationDO newStartOperation,
                                                HcPlanSplitDetailDO splitDetail,
                                                HcPlanSplitCreateReqVO reqVO) {
        if (newStartOperation == null) {
            throw invalidParamException("拆批新计划首工序不存在，无法转挂选中片号");
        }
        HcSlittingSliceRecordDO slice = hcSlittingSliceRecordMapper.selectById(splitDetail.getSourceId());
        if (slice == null) {
            return;
        }
        HcSlittingSliceRecordDO update = new HcSlittingSliceRecordDO();
        update.setId(slice.getId());
        update.setPlanId(newPlan.getId());
        update.setPlanNo(newPlan.getPlanNo());
        update.setPlanOperationId(newStartOperation.getId());
        update.setOperationCode(newStartOperation.getOpCode());
        update.setOperationName(newStartOperation.getOpName());
        update.setRemark(appendRemark(slice.getRemark(), buildSourceTransferRemark(sourcePlan, newPlan,
                firstNonNull(splitDetail.getSplitQty(), BigDecimal.ONE), firstNotBlank(reqVO.getUnit(), splitDetail.getUnit(), UNIT_PCS),
                BigDecimal.ONE)));
        hcSlittingSliceRecordMapper.updateById(update);
    }

    private String buildSourceTransferRemark(HcPlanOrderDO sourcePlan,
                                             HcPlanOrderDO newPlan,
                                             BigDecimal splitQty,
                                             String unit,
                                             BigDecimal sourceQty) {
        return "原计划 " + formatQtyUnit(sourceQty, unit) + "，"
                + LocalDateTime.now().format(REMARK_DATE_FORMATTER)
                + " 转出到新计划号 " + firstNotBlank(newPlan == null ? null : newPlan.getPlanNo(), "-")
                + "，型号 " + firstNotBlank(newPlan == null ? null : newPlan.getModelCode(), sourcePlan.getModelCode(), "-")
                + "，转出 " + formatQtyUnit(splitQty, unit);
    }

    private String buildTargetTransferRemark(HcPlanOrderDO sourcePlan,
                                             BigDecimal splitQty,
                                             String unit) {
        return LocalDateTime.now().format(REMARK_DATE_FORMATTER)
                + " 由计划号 " + firstNotBlank(sourcePlan.getPlanNo(), "-")
                + "，原型号 " + firstNotBlank(sourcePlan.getModelCode(), "-")
                + " 转入 " + formatQtyUnit(splitQty, unit)
                + " 继续加工";
    }

    private String appendRemark(String oldRemark, String appendText) {
        if (StrUtil.isBlank(appendText)) {
            return oldRemark;
        }
        if (StrUtil.isBlank(oldRemark)) {
            return appendText;
        }
        return oldRemark + "；" + appendText;
    }

    private String joinRemarkParts(String... parts) {
        List<String> values = new ArrayList<>();
        if (parts != null) {
            for (String part : parts) {
                String value = StrUtil.trimToNull(part);
                if (value != null) {
                    values.add(value);
                }
            }
        }
        return String.join("；", values);
    }

    private String formatQtyUnit(BigDecimal qty, String unit) {
        return scale(qty).stripTrailingZeros().toPlainString() + firstNotBlank(unit, "");
    }

    private HcPlanOrderDO getPlanByNo(String planNo) {
        String value = StrUtil.trimToNull(planNo);
        if (StrUtil.isBlank(value)) {
            throw invalidParamException("计划号不能为空");
        }
        HcPlanOrderDO plan = hcPlanOrderMapper.selectOne(new LambdaQueryWrapperX<HcPlanOrderDO>()
                .eq(HcPlanOrderDO::getPlanNo, value)
                .eq(HcPlanOrderDO::getDeleted, false)
                .orderByDesc(HcPlanOrderDO::getId)
                .last("LIMIT 1"));
        if (plan == null) {
            throw invalidParamException("计划号 {} 不存在", value);
        }
        return plan;
    }

    private ReportFacts loadFacts(HcPlanOrderDO plan) {
        Long planId = plan.getId();
        ReportFacts facts = new ReportFacts();
        facts.processReports = hcProcessReportMapper.selectList(new LambdaQueryWrapperX<HcProcessReportDO>()
                .eq(HcProcessReportDO::getPlanId, planId)
                .eq(HcProcessReportDO::getDeleted, false)
                .orderByAsc(HcProcessReportDO::getOperationSeq)
                .orderByAsc(HcProcessReportDO::getId));
        facts.grindingReports = hcGrindingReportMapper.selectList(new LambdaQueryWrapperX<HcGrindingReportDO>()
                .eq(HcGrindingReportDO::getPlanId, planId)
                .eq(HcGrindingReportDO::getDeleted, false)
                .orderByAsc(HcGrindingReportDO::getId));
        facts.grindingFirstDetails = hcGrindingFirstDetailMapper.selectList(new LambdaQueryWrapperX<HcGrindingFirstDetailDO>()
                .eq(HcGrindingFirstDetailDO::getPlanId, planId)
                .eq(HcGrindingFirstDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingFirstDetailDO::getId));
        facts.grindingFirstAllocations = hcGrindingFirstAllocationMapper.selectList(
                        new LambdaQueryWrapperX<HcGrindingFirstAllocationDO>()
                                .eq(HcGrindingFirstAllocationDO::getPlanId, planId)
                                .eq(HcGrindingFirstAllocationDO::getDeleted, false)
                                .orderByAsc(HcGrindingFirstAllocationDO::getStartPosition)
                                .orderByAsc(HcGrindingFirstAllocationDO::getId))
                .stream()
                .filter(item -> !"VOID".equalsIgnoreCase(item.getDetailStatus()))
                .toList();
        facts.grindingSecondDetails = hcGrindingSecondDetailMapper.selectList(new LambdaQueryWrapperX<HcGrindingSecondDetailDO>()
                .eq(HcGrindingSecondDetailDO::getPlanId, planId)
                .eq(HcGrindingSecondDetailDO::getDeleted, false)
                .orderByAsc(HcGrindingSecondDetailDO::getId));
        facts.adhesiveReports = hcAdhesiveReportMapper.selectList(new LambdaQueryWrapperX<HcAdhesiveReportDO>()
                .eq(HcAdhesiveReportDO::getPlanId, planId)
                .eq(HcAdhesiveReportDO::getDeleted, false)
                .orderByAsc(HcAdhesiveReportDO::getId));
        facts.slittingSlices = hcSlittingSliceRecordMapper.selectList(new LambdaQueryWrapperX<HcSlittingSliceRecordDO>()
                .eq(HcSlittingSliceRecordDO::getPlanId, planId)
                .eq(HcSlittingSliceRecordDO::getDeleted, false)
                .orderByAsc(HcSlittingSliceRecordDO::getId));
        facts.pressSlotReports = hcPressSlotReportMapper.selectList(new LambdaQueryWrapperX<HcPressSlotReportDO>()
                .eq(HcPressSlotReportDO::getPlanId, planId)
                .eq(HcPressSlotReportDO::getDeleted, false)
                .orderByAsc(HcPressSlotReportDO::getId));
        facts.adhesive2Reports = hcAdhesive2ReportMapper.selectList(new LambdaQueryWrapperX<HcAdhesive2ReportDO>()
                .eq(HcAdhesive2ReportDO::getPlanId, planId)
                .eq(HcAdhesive2ReportDO::getDeleted, false)
                .orderByAsc(HcAdhesive2ReportDO::getId));
        facts.cutRoundReports = hcCutRoundReportMapper.selectList(new LambdaQueryWrapperX<HcCutRoundReportDO>()
                .eq(HcCutRoundReportDO::getPlanId, planId)
                .eq(HcCutRoundReportDO::getDeleted, false)
                .orderByAsc(HcCutRoundReportDO::getId));
        facts.splitOrders = hcPlanSplitOrderMapper.selectListBySourcePlanId(planId);
        facts.inboundSplitOrders = hcPlanSplitOrderMapper.selectList(new LambdaQueryWrapperX<HcPlanSplitOrderDO>()
                .and(wrapper -> wrapper
                        .eq(HcPlanSplitOrderDO::getTargetPlanId, planId)
                        .or()
                        .eq(HcPlanSplitOrderDO::getTargetPlanNo, plan.getPlanNo()))
                .eq(HcPlanSplitOrderDO::getDeleted, false)
                .orderByDesc(HcPlanSplitOrderDO::getId));
        List<Long> effectiveSplitOrderIds = facts.splitOrders.stream()
                .filter(this::isEffectiveSplitOrder)
                .map(HcPlanSplitOrderDO::getId)
                .filter(Objects::nonNull)
                .toList();
        facts.splitDetails = effectiveSplitOrderIds.isEmpty()
                ? List.of()
                : hcPlanSplitDetailMapper.selectList(new LambdaQueryWrapperX<HcPlanSplitDetailDO>()
                .in(HcPlanSplitDetailDO::getSplitOrderId, effectiveSplitOrderIds)
                .eq(HcPlanSplitDetailDO::getDeleted, false)
                .orderByAsc(HcPlanSplitDetailDO::getId));
        return facts;
    }

    private List<HcPlanSplitGraphRespVO.OperationOption> toOperationOptions(List<HcPlanOrderOperationDO> operations) {
        return operations.stream().map(operation -> {
            HcPlanSplitGraphRespVO.OperationOption option = new HcPlanSplitGraphRespVO.OperationOption();
            option.setOperationId(operation.getId());
            option.setOpSeq(operation.getOpSeq());
            option.setOpCode(operation.getOpCode());
            option.setOpName(operation.getOpName());
            return option;
        }).toList();
    }

    private List<HcPlanSplitGraphRespVO.Node> buildOperationNodes(List<HcPlanOrderOperationDO> operations,
                                                                  ReportFacts facts) {
        return operations.stream().map(operation -> {
            HcPlanSplitGraphRespVO.Node node = new HcPlanSplitGraphRespVO.Node();
            node.setNodeKey("OP_" + operation.getId());
            node.setStageCode(firstNotBlank(operation.getOpCode(), "OP"));
            node.setStageName(operation.getOpName());
            node.setOperationId(operation.getId());
            node.setOpSeq(operation.getOpSeq());
            node.setOpCode(operation.getOpCode());
            node.setOpName(operation.getOpName());
            node.setOperationStatus(operation.getOperationStatus());
            node.setTotalQty(scale(defaultDecimal(operation.getRequiredQty())));
            node.setProcessedQty(scale(estimateOperationProcessed(operation, facts)));
            node.setRemainingQty(nonNegative(node.getTotalQty().subtract(node.getProcessedQty())));
            node.setUnit(firstNotBlank(operation.getUom(), operation.getUnitCode()));
            node.setSplitable(false);
            node.setSplitMode(null);
            boolean hasReport = hasOperationReport(operation, facts);
            boolean hasConfirmedReport = hasConfirmedOperationReport(operation, facts);
            node.setPlanned(true);
            node.setHasReport(hasReport);
            node.setHasConfirmedReport(hasConfirmedReport);
            node.setReportState(resolveReportState(hasReport, hasConfirmedReport));
            node.setRemark("计划工序节点，仅展示该工序报工进度；拆批请使用下方高亮的可拆节点。");
            applyInboundSplitInfo(node, operation, facts);
            return node;
        }).toList();
    }

    private List<HcPlanSplitGraphRespVO.Edge> buildOperationEdges(List<HcPlanOrderOperationDO> operations) {
        List<HcPlanSplitGraphRespVO.Edge> edges = new ArrayList<>();
        for (int i = 1; i < operations.size(); i++) {
            HcPlanSplitGraphRespVO.Edge edge = new HcPlanSplitGraphRespVO.Edge();
            edge.setFrom("OP_" + operations.get(i - 1).getId());
            edge.setTo("OP_" + operations.get(i).getId());
            edge.setLabel("下一工序");
            edges.add(edge);
        }
        return edges;
    }

    private BigDecimal estimateOperationProcessed(HcPlanOrderOperationDO operation, ReportFacts facts) {
        Long operationId = operation.getId();
        String opName = firstNotBlank(operation.getOpName(), "");
        if (containsAny(opName, "磨皮", "粗磨")) {
            BigDecimal second = sum(operationReports(facts.grindingSecondDetails,
                    HcGrindingSecondDetailDO::getPlanOperationId, operation), HcGrindingSecondDetailDO::getOutputLength);
            return second.compareTo(BigDecimal.ZERO) > 0
                    ? second
                    : sum(operationReports(facts.grindingReports, HcGrindingReportDO::getPlanOperationId, operation),
                    HcGrindingReportDO::getFirstOutputLength);
        }
        if (containsAny(opName, "粘胶1", "粘胶一")) {
            return sum(operationReports(facts.adhesiveReports, HcAdhesiveReportDO::getPlanOperationId, operation),
                    HcAdhesiveReportDO::getOutputLength);
        }
        if (containsAny(opName, "分切")) {
            return BigDecimal.valueOf(operationReports(facts.slittingSlices,
                    HcSlittingSliceRecordDO::getPlanOperationId, operation).size());
        }
        if (containsAny(opName, "压槽")) {
            return BigDecimal.valueOf(operationReports(facts.pressSlotReports,
                    HcPressSlotReportDO::getPlanOperationId, operation).size());
        }
        if (containsAny(opName, "粘胶2", "粘胶二")) {
            return BigDecimal.valueOf(operationReports(facts.adhesive2Reports,
                    HcAdhesive2ReportDO::getPlanOperationId, operation).size());
        }
        if (containsAny(opName, "裁切")) {
            return BigDecimal.valueOf(operationReports(facts.cutRoundReports,
                    HcCutRoundReportDO::getPlanOperationId, operation).size());
        }
        return sum(facts.processReports.stream()
                .filter(item -> Objects.equals(item.getPlanOperationId(), operationId))
                .toList(), HcProcessReportDO::getGoodQty);
    }

    private boolean hasOperationReport(HcPlanOrderOperationDO operation, ReportFacts facts) {
        String opName = firstNotBlank(operation.getOpName(), "");
        if (containsAny(opName, "磨皮", "粗磨")) {
            return !operationReports(facts.grindingReports, HcGrindingReportDO::getPlanOperationId, operation).isEmpty()
                    || !operationReports(facts.grindingSecondDetails, HcGrindingSecondDetailDO::getPlanOperationId,
                    operation).isEmpty();
        }
        if (containsAny(opName, "粘胶1", "粘胶一")) {
            return !operationReports(facts.adhesiveReports, HcAdhesiveReportDO::getPlanOperationId, operation).isEmpty();
        }
        if (containsAny(opName, "分切")) {
            return !operationReports(facts.slittingSlices, HcSlittingSliceRecordDO::getPlanOperationId, operation).isEmpty();
        }
        if (containsAny(opName, "压槽")) {
            return !operationReports(facts.pressSlotReports, HcPressSlotReportDO::getPlanOperationId, operation).isEmpty();
        }
        if (containsAny(opName, "粘胶2", "粘胶二")) {
            return !operationReports(facts.adhesive2Reports, HcAdhesive2ReportDO::getPlanOperationId, operation).isEmpty();
        }
        if (containsAny(opName, "裁切")) {
            return !operationReports(facts.cutRoundReports, HcCutRoundReportDO::getPlanOperationId, operation).isEmpty();
        }
        return !operationReports(facts.processReports, HcProcessReportDO::getPlanOperationId, operation).isEmpty();
    }

    private boolean hasConfirmedOperationReport(HcPlanOrderOperationDO operation, ReportFacts facts) {
        String opName = firstNotBlank(operation.getOpName(), "");
        if (containsAny(opName, "磨皮", "粗磨")) {
            boolean confirmedFirst = operationReports(facts.grindingReports, HcGrindingReportDO::getPlanOperationId,
                    operation).stream().anyMatch(item -> isConfirmedReport(item.getReportStatus(), item.getConfirmerTime()));
            boolean confirmedSecond = operationReports(facts.grindingSecondDetails,
                    HcGrindingSecondDetailDO::getPlanOperationId, operation).stream()
                    .anyMatch(item -> isConfirmedReport(firstNotBlank(item.getConfirmStatus(), item.getDetailStatus()),
                            item.getConfirmTime()));
            return confirmedFirst || confirmedSecond;
        }
        if (containsAny(opName, "粘胶1", "粘胶一")) {
            return operationReports(facts.adhesiveReports, HcAdhesiveReportDO::getPlanOperationId, operation).stream()
                    .anyMatch(item -> isConfirmedReport(item.getReportStatus(), item.getConfirmerTime()));
        }
        if (containsAny(opName, "分切")) {
            return operationReports(facts.slittingSlices, HcSlittingSliceRecordDO::getPlanOperationId, operation).stream()
                    .anyMatch(item -> isConfirmedStatus(item.getScanStatus()));
        }
        if (containsAny(opName, "压槽")) {
            return operationReports(facts.pressSlotReports, HcPressSlotReportDO::getPlanOperationId, operation).stream()
                    .anyMatch(item -> isConfirmedReport(item.getReportStatus(), item.getConfirmerTime()));
        }
        if (containsAny(opName, "粘胶2", "粘胶二")) {
            return operationReports(facts.adhesive2Reports, HcAdhesive2ReportDO::getPlanOperationId, operation).stream()
                    .anyMatch(item -> isConfirmedReport(item.getReportStatus(), item.getConfirmerTime()));
        }
        if (containsAny(opName, "裁切")) {
            return operationReports(facts.cutRoundReports, HcCutRoundReportDO::getPlanOperationId, operation).stream()
                    .anyMatch(item -> isConfirmedReport(item.getReportStatus(), item.getConfirmerTime()));
        }
        return operationReports(facts.processReports, HcProcessReportDO::getPlanOperationId, operation).stream()
                .anyMatch(item -> isConfirmedReport(item.getOperationStatus(), item.getConfirmerTime()));
    }

    private String resolveReportState(boolean hasReport, boolean hasConfirmedReport) {
        if (!hasReport) {
            return REPORT_STATE_PLANNED_NOT_REPORTED;
        }
        return hasConfirmedReport ? REPORT_STATE_REPORTED_CONFIRMED : REPORT_STATE_REPORTED_UNCONFIRMED;
    }

    private <T> List<T> operationReports(List<T> reports,
                                         Function<T, Long> operationIdGetter,
                                         HcPlanOrderOperationDO operation) {
        if (reports == null || reports.isEmpty()) {
            return List.of();
        }
        Long operationId = operation == null ? null : operation.getId();
        List<T> matched = operationId == null ? List.of() : reports.stream()
                .filter(item -> Objects.equals(operationIdGetter.apply(item), operationId))
                .toList();
        if (!matched.isEmpty()) {
            return matched;
        }
        boolean unbound = reports.stream().allMatch(item -> operationIdGetter.apply(item) == null);
        return unbound ? reports : List.of();
    }

    private boolean isConfirmedReport(String status, LocalDateTime confirmTime) {
        return isConfirmedStatus(status) || confirmTime != null;
    }

    private boolean isConfirmedStatus(String status) {
        return "CONFIRMED".equalsIgnoreCase(StrUtil.trimToEmpty(status));
    }

    private boolean isSubmittedAdhesiveReport(HcAdhesiveReportDO report) {
        if (report == null) {
            return false;
        }
        String status = StrUtil.blankToDefault(report.getReportStatus(), "SUBMITTED");
        return "SUBMITTED".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status);
    }

    private boolean isConfirmedPressSlotReport(HcPressSlotReportDO report) {
        return report != null && isConfirmedReport(report.getReportStatus(), report.getConfirmerTime());
    }

    private boolean isConfirmedAdhesive2Report(HcAdhesive2ReportDO report) {
        return report != null && isConfirmedReport(report.getReportStatus(), report.getConfirmerTime());
    }

    private boolean isConfirmedCutRoundReport(HcCutRoundReportDO report) {
        return report != null && isConfirmedReport(report.getReportStatus(), report.getConfirmerTime());
    }

    private BigDecimal resolveGrindingFirstOutputLength(HcGrindingFirstDetailDO detail) {
        if (detail == null) {
            return BigDecimal.ZERO;
        }
        return firstNonNull(detail.getOutputLength(), detail.getProcessLength(), BigDecimal.ZERO);
    }

    private BigDecimal resolveGrindingSecondOutputLength(HcGrindingSecondDetailDO detail) {
        if (detail == null) {
            return BigDecimal.ZERO;
        }
        return firstNonNull(detail.getOutputLength(), detail.getProcessLength(), BigDecimal.ZERO);
    }

    private BigDecimal resolveGrindingSecondProcessLength(HcGrindingSecondDetailDO detail) {
        if (detail == null) {
            return BigDecimal.ZERO;
        }
        return firstNonNull(detail.getProcessLength(), detail.getOutputLength(), BigDecimal.ZERO);
    }

    private BigDecimal resolveAdhesiveOutputLength(HcAdhesiveReportDO report) {
        return report == null ? BigDecimal.ZERO : defaultDecimal(report.getOutputLength());
    }

    private BigDecimal resolveAdhesiveSlittingRemainingLength(HcAdhesiveReportDO report,
                                                              Map<Long, BigDecimal> usedBySlitting,
                                                              Map<Long, BigDecimal> splitByAdhesiveReport) {
        if (report == null) {
            return BigDecimal.ZERO;
        }
        if (report.getSlittingRemainingLength() != null) {
            return nonNegative(report.getSlittingRemainingLength());
        }
        return nonNegative(resolveAdhesiveOutputLength(report)
                .subtract(defaultDecimal(usedBySlitting.get(report.getId())))
                .subtract(defaultDecimal(splitByAdhesiveReport.get(report.getId()))));
    }

    private BigDecimal resolveAdhesiveInputLength(HcAdhesiveReportDO report) {
        if (report == null) {
            return BigDecimal.ZERO;
        }
        return firstNonNull(report.getInputLength(), report.getOutputLength(), BigDecimal.ZERO);
    }

    private BigDecimal resolveSlittingSliceLength(HcSlittingSliceRecordDO slice) {
        if (slice == null) {
            return BigDecimal.ZERO;
        }
        if (slice.getSliceLength() != null && slice.getSliceLength().compareTo(BigDecimal.ZERO) > 0) {
            return slice.getSliceLength();
        }
        if (slice.getEndPosition() != null && slice.getStartPosition() != null
                && slice.getEndPosition().compareTo(slice.getStartPosition()) > 0) {
            return slice.getEndPosition().subtract(slice.getStartPosition());
        }
        return BigDecimal.ZERO;
    }

    private void addSplitCandidateNodes(HcPlanSplitGraphRespVO graph,
                                        List<HcPlanOrderOperationDO> operations,
                                        ReportFacts facts) {
        addRoughSecondCandidate(graph, findOperation(operations, "磨皮", "粗磨"), facts);
        addRoughSecondToAdhesiveCandidates(graph, findOperation(operations, "粘胶1", "粘胶一"), facts);
        addAdhesive1Candidates(graph, findOperation(operations, "粘胶1", "粘胶一"), facts);
        addSlittingStatusCandidates(graph, findOperation(operations, "分切"), facts);
        addPressSlotCandidates(graph, findOperation(operations, "压槽"), facts);
        addAdhesive2Candidates(graph, findOperation(operations, "粘胶2", "粘胶二"), facts);
        addCutRoundCandidates(graph, findOperation(operations, "裁切"), facts);
    }

    private void addRoughSecondCandidate(HcPlanSplitGraphRespVO graph,
                                         HcPlanOrderOperationDO operation,
                                         ReportFacts facts) {
        if (!facts.grindingFirstAllocations.isEmpty()) {
            addRoughSecondCandidateFromFirstAllocations(graph, operation, facts);
            return;
        }
        if (!facts.grindingFirstDetails.isEmpty()
                && facts.grindingSecondDetails.stream().allMatch(item -> item.getFirstDetailId() != null)) {
            addRoughSecondCandidateFromFirstDetails(graph, operation, facts);
            return;
        }
        BigDecimal firstOutput = sum(facts.grindingFirstDetails, this::resolveGrindingFirstOutputLength);
        if (firstOutput.compareTo(BigDecimal.ZERO) <= 0) {
            firstOutput = sum(facts.grindingReports, HcGrindingReportDO::getFirstOutputLength);
        }
        BigDecimal secondProcess = sum(facts.grindingSecondDetails, this::resolveGrindingSecondProcessLength);
        BigDecimal remaining = nonNegative(firstOutput.subtract(secondProcess));
        if (firstOutput.compareTo(BigDecimal.ZERO) <= 0 && secondProcess.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                "ROUGH_SECOND_REMAINING", "ROUGH_SECOND", "二磨未加工余量", operation,
                firstOutput, secondProcess, remaining, UNIT_METER, SPLIT_MODE_METER);
        node.setRemark("一磨产出米数减去二磨加工米数；该段未形成 WIP 库存时，只生成拆批新计划，不自动锁定库存。");
        applyInboundSplitInfo(node, operation, facts);
        graph.getNodes().add(node);
        addCandidateEdge(graph, operation, node);
    }

    /**
     * 新模式只按一磨前置分配主键核减二磨和已拆米数，不允许退回母批聚合。
     */
    private void addRoughSecondCandidateFromFirstAllocations(HcPlanSplitGraphRespVO graph,
                                                              HcPlanOrderOperationDO operation,
                                                              ReportFacts facts) {
        Map<Long, BigDecimal> secondOutputByAllocation = facts.grindingSecondDetails.stream()
                .filter(item -> item.getFirstAllocationId() != null)
                .collect(Collectors.groupingBy(HcGrindingSecondDetailDO::getFirstAllocationId,
                        Collectors.mapping(this::resolveGrindingSecondProcessLength,
                                Collectors.reducing(BigDecimal.ZERO, this::defaultDecimal, BigDecimal::add))));
        Map<Long, BigDecimal> splitByAllocation = splitQtyBySourceId(facts, TABLE_GRINDING_FIRST_ALLOCATION);
        BigDecimal total = facts.grindingFirstAllocations.stream()
                .map(HcGrindingFirstAllocationDO::getConfirmedLength)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal processed = BigDecimal.ZERO;
        HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                "ROUGH_SECOND_REMAINING", "ROUGH_SECOND", "二磨未加工余量", operation,
                total, BigDecimal.ZERO, BigDecimal.ZERO, UNIT_METER, SPLIT_MODE_METER);
        node.setSourceTable(TABLE_GRINDING_FIRST_ALLOCATION);
        node.setRemark("按一磨前置分配加工单元精确核减二磨加工和已拆出米数；新模式不按母批兜底。");
        for (HcGrindingFirstAllocationDO allocation : facts.grindingFirstAllocations) {
            BigDecimal allocationOutput = defaultDecimal(allocation.getConfirmedLength());
            BigDecimal allocationProcessed = defaultDecimal(secondOutputByAllocation.get(allocation.getId()))
                    .add(defaultDecimal(splitByAllocation.get(allocation.getId())));
            processed = processed.add(allocationProcessed);
            BigDecimal itemRemaining = nonNegative(allocationOutput.subtract(allocationProcessed));
            if (itemRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            node.getAvailableItems().add(item(allocation.getId(), null, TABLE_GRINDING_FIRST_ALLOCATION,
                    firstNotBlank(allocation.getSegmentMark(), "NONE"),
                    firstNotBlank(allocation.getProductionBatchNo(), allocation.getMotherBatchNo()),
                    allocation.getMotherBatchNo(), itemRemaining, UNIT_METER));
        }
        node.setProcessedQty(scale(processed));
        node.setRemainingQty(scale(node.getAvailableItems().stream()
                .map(HcPlanSplitGraphRespVO.Item::getQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        node.setSplitable(defaultDecimal(node.getRemainingQty()).compareTo(BigDecimal.ZERO) > 0);
        applyInboundSplitInfo(node, operation, facts);
        graph.getNodes().add(node);
        addCandidateEdge(graph, operation, node);
    }

    private void addRoughSecondCandidateFromFirstDetails(HcPlanSplitGraphRespVO graph,
                                                         HcPlanOrderOperationDO operation,
                                                         ReportFacts facts) {
        Map<Long, BigDecimal> secondOutputByFirstDetail = facts.grindingSecondDetails.stream()
                .filter(item -> item.getFirstDetailId() != null)
                .collect(Collectors.groupingBy(HcGrindingSecondDetailDO::getFirstDetailId,
                        Collectors.mapping(this::resolveGrindingSecondProcessLength,
                                Collectors.reducing(BigDecimal.ZERO, this::defaultDecimal, BigDecimal::add))));
        Map<Long, BigDecimal> splitByFirstDetail = splitQtyBySourceId(facts, TABLE_GRINDING_FIRST_DETAIL);
        BigDecimal total = sum(facts.grindingFirstDetails, this::resolveGrindingFirstOutputLength);
        BigDecimal processed = BigDecimal.ZERO;
        HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                "ROUGH_SECOND_REMAINING", "ROUGH_SECOND", "二磨未加工余量", operation,
                total, BigDecimal.ZERO, BigDecimal.ZERO, UNIT_METER, SPLIT_MODE_METER);
        node.setSourceTable(TABLE_GRINDING_FIRST_DETAIL);
        node.setRemark("一磨明细产出减去二磨加工和已拆出米数；未形成 WIP 库存时，只生成拆批新计划，不自动锁定库存。");
        for (HcGrindingFirstDetailDO detail : facts.grindingFirstDetails) {
            BigDecimal detailOutput = resolveGrindingFirstOutputLength(detail);
            BigDecimal detailProcessed = defaultDecimal(secondOutputByFirstDetail.get(detail.getId()))
                    .add(defaultDecimal(splitByFirstDetail.get(detail.getId())));
            processed = processed.add(detailProcessed);
            BigDecimal itemRemaining = nonNegative(detailOutput.subtract(detailProcessed));
            if (itemRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            node.getAvailableItems().add(item(detail.getId(), null, TABLE_GRINDING_FIRST_DETAIL,
                    firstNotBlank(detail.getMotherBatchNo(), detail.getSourceProductionBatchNo(), detail.getRowUid(), "一磨产出"),
                    normalizeSegmentBatchNo(detail.getMotherBatchNo(), detail.getSourceProductionBatchNo()),
                    detail.getSourceProductionBatchNo(), itemRemaining, UNIT_METER));
        }
        node.setProcessedQty(scale(processed));
        node.setRemainingQty(scale(node.getAvailableItems().stream()
                .map(HcPlanSplitGraphRespVO.Item::getQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)));
        node.setSplitable(defaultDecimal(node.getRemainingQty()).compareTo(BigDecimal.ZERO) > 0);
        applyInboundSplitInfo(node, operation, facts);
        graph.getNodes().add(node);
        addCandidateEdge(graph, operation, node);
    }

    private void addRoughSecondToAdhesiveCandidates(HcPlanSplitGraphRespVO graph,
                                                    HcPlanOrderOperationDO operation,
                                                    ReportFacts facts) {
        if (operation == null || operation.getPlanId() == null || operation.getId() == null) {
            return;
        }
        List<HcGrindingSecondDetailDO> sourceDetails = hcGrindingSecondDetailMapper.selectConfirmedListByPlanId(
                operation.getPlanId(), operation.getId());
        if (sourceDetails.isEmpty() && facts.grindingSecondDetails.isEmpty()) {
            return;
        }
        Map<String, List<HcGrindingSecondDetailDO>> allByBatch = facts.grindingSecondDetails.stream()
                .collect(Collectors.groupingBy(item -> firstNotBlank(normalizeSegmentBatchNo(item.getProductionBatchNo(),
                                item.getConfirmedBatchNo(), item.getMotherBatchNo()), "未分段"),
                        LinkedHashMap::new, Collectors.toList()));
        Map<String, List<HcGrindingSecondDetailDO>> confirmedByBatch = sourceDetails.stream()
                .collect(Collectors.groupingBy(item -> firstNotBlank(normalizeSegmentBatchNo(item.getProductionBatchNo(),
                                item.getConfirmedBatchNo(), item.getMotherBatchNo()), "未分段"),
                        LinkedHashMap::new, Collectors.toList()));
        Map<Long, BigDecimal> usedBySecondDetail = facts.adhesiveReports.stream()
                .filter(item -> item.getSourceGrindingSecondDetailId() != null)
                .collect(Collectors.groupingBy(HcAdhesiveReportDO::getSourceGrindingSecondDetailId,
                        Collectors.mapping(this::resolveAdhesiveInputLength,
                                Collectors.reducing(BigDecimal.ZERO, value -> defaultDecimal(value), BigDecimal::add))));
        Map<Long, BigDecimal> splitBySecondDetail = splitQtyBySourceId(facts, TABLE_GRINDING_SECOND_DETAIL);
        Set<String> batchNos = new LinkedHashSet<>();
        batchNos.addAll(allByBatch.keySet());
        batchNos.addAll(confirmedByBatch.keySet());
        batchNos.forEach(batch -> {
            List<HcGrindingSecondDetailDO> allDetails = allByBatch.getOrDefault(batch, List.of());
            List<HcGrindingSecondDetailDO> details = confirmedByBatch.getOrDefault(batch, List.of());
            if (details.isEmpty()) {
                BigDecimal pending = sum(allDetails, HcGrindingSecondDetailDO::getOutputLength);
                if (pending.compareTo(BigDecimal.ZERO) <= 0) {
                    return;
                }
                HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                        "ROUGH_SECOND_UNCONFIRMED:" + batch, "ROUGH_SECOND", "二磨未确认余量", operation,
                        pending, BigDecimal.ZERO, pending, UNIT_METER, SPLIT_MODE_METER);
                node.setBatchNo(batch);
                node.setSplitable(false);
                node.setBlockedReason("二磨报工未确认");
                node.setRemark("该二磨分段尚未确认，确认后才允许从2次磨皮节点拆批。");
                applyInboundSplitInfo(node, operation, facts);
                graph.getNodes().add(node);
                addCandidateEdge(graph, operation, node);
                return;
            }
            BigDecimal total = sum(details, HcGrindingSecondDetailDO::getOutputLength);
            BigDecimal processed = details.stream()
                    .map(item -> defaultDecimal(usedBySecondDetail.get(item.getId()))
                            .add(defaultDecimal(splitBySecondDetail.get(item.getId()))))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = nonNegative(total.subtract(processed));
            HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                    "ROUGH_SECOND_REMAINING:" + batch, "ROUGH_SECOND", "二磨未粘胶余量", operation,
                    total, processed, remaining, UNIT_METER, SPLIT_MODE_METER);
            node.setBatchNo(batch);
            node.setSourceTable(TABLE_GRINDING_SECOND_DETAIL);
            node.setRemark("二磨分段产出米数减去粘胶1加工米数和已拆出米数。");
            applyInboundSplitInfo(node, operation, facts);
            details.forEach(detail -> {
                BigDecimal itemRemaining = nonNegative(defaultDecimal(detail.getOutputLength())
                        .subtract(defaultDecimal(usedBySecondDetail.get(detail.getId())))
                        .subtract(defaultDecimal(splitBySecondDetail.get(detail.getId()))));
                if (itemRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    String segmentBatchNo = normalizeSegmentBatchNo(detail.getProductionBatchNo(),
                            detail.getConfirmedBatchNo(), detail.getMotherBatchNo());
                    node.getAvailableItems().add(item(detail.getId(), detail.getStockId(), TABLE_GRINDING_SECOND_DETAIL,
                            firstNotBlank(detail.getProductionBatchNo(), detail.getConfirmedBatchNo(), detail.getMotherBatchNo()),
                            segmentBatchNo,
                            detail.getSourceProductionBatchNo(), itemRemaining, UNIT_METER));
                }
            });
            graph.getNodes().add(node);
            addCandidateEdge(graph, operation, node);
        });
    }

    private void addAdhesive1Candidates(HcPlanSplitGraphRespVO graph,
                                        HcPlanOrderOperationDO operation,
                                        ReportFacts facts) {
        if (operation == null) {
            return;
        }
        List<HcAdhesiveReportDO> reports = facts.adhesiveReports.stream()
                .filter(this::isSubmittedAdhesiveReport)
                .toList();
        if (reports.isEmpty()) {
            return;
        }
        Map<Long, BigDecimal> usedBySlitting = facts.slittingSlices.stream()
                .filter(item -> item.getSourceAdhesiveReportId() != null)
                .filter(item -> isConfirmedStatus(item.getScanStatus()))
                .collect(Collectors.groupingBy(HcSlittingSliceRecordDO::getSourceAdhesiveReportId,
                        Collectors.mapping(this::resolveSlittingSliceLength,
                                Collectors.reducing(BigDecimal.ZERO, this::defaultDecimal, BigDecimal::add))));
        Map<Long, BigDecimal> splitByAdhesiveReport = splitQtyBySourceId(facts, TABLE_ADHESIVE_REPORT);
        Map<String, List<HcAdhesiveReportDO>> byBatch = reports.stream()
                .collect(Collectors.groupingBy(item -> firstNotBlank(normalizeSegmentBatchNo(item.getSourceProductionBatchNo(),
                                item.getSourceBatchNo(), item.getParentProductionBatchNo(), item.getProductionBatchNo()), "未分段"),
                        LinkedHashMap::new, Collectors.toList()));
        byBatch.forEach((batch, batchReports) -> {
            BigDecimal total = sum(batchReports, this::resolveAdhesiveOutputLength);
            BigDecimal remaining = batchReports.stream()
                    .map(item -> resolveAdhesiveSlittingRemainingLength(item, usedBySlitting, splitByAdhesiveReport))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal processed = nonNegative(total.subtract(remaining));
            HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                    "ADHESIVE1_REMAINING:" + batch, "ADHESIVE1", "粘胶1未加工余量", operation,
                    total, processed, remaining, UNIT_METER, SPLIT_MODE_METER);
            node.setBatchNo(batch);
            node.setSourceTable(TABLE_ADHESIVE_REPORT);
            node.setRemark("粘胶1分切剩余米数优先取分切剩余字段；历史空值回退为粘胶1产出米数减去分切已确认米数和已拆出米数。");
            applyInboundSplitInfo(node, operation, facts);
            batchReports.forEach(report -> {
                BigDecimal itemRemaining = resolveAdhesiveSlittingRemainingLength(report, usedBySlitting, splitByAdhesiveReport);
                if (itemRemaining.compareTo(BigDecimal.ZERO) > 0) {
                    String segmentBatchNo = normalizeSegmentBatchNo(report.getSourceProductionBatchNo(),
                            report.getSourceBatchNo(), report.getParentProductionBatchNo(), report.getProductionBatchNo());
                    node.getAvailableItems().add(item(report.getId(), report.getOutputStockId(), TABLE_ADHESIVE_REPORT,
                            firstNotBlank(report.getProductionBatchNo(), report.getSourceProductionBatchNo(),
                                    report.getSourceBatchNo(), "粘胶1产出"),
                            segmentBatchNo,
                            firstNotBlank(report.getSourceProductionBatchNo(), report.getSourceBatchNo(),
                                    report.getParentProductionBatchNo()), itemRemaining, UNIT_METER));
                }
            });
            graph.getNodes().add(node);
            addCandidateEdge(graph, operation, node);
        });
    }

    private void addSlittingStatusCandidates(HcPlanSplitGraphRespVO graph,
                                             HcPlanOrderOperationDO operation,
                                             ReportFacts facts) {
        if (operation == null) {
            return;
        }
        List<HcSlittingSliceRecordDO> slices = operationReports(facts.slittingSlices,
                HcSlittingSliceRecordDO::getPlanOperationId, operation);
        if (slices.isEmpty()) {
            return;
        }
        Map<String, List<HcSlittingSliceRecordDO>> byBatch = slices.stream()
                .collect(Collectors.groupingBy(item -> firstNotBlank(normalizeSegmentBatchNo(item.getSourceProductionBatchNo(),
                                item.getSourceBatchNo(), item.getSliceSerialNo()), "未分段"),
                        LinkedHashMap::new, Collectors.toList()));
        byBatch.forEach((batch, batchSlices) -> {
            long confirmedCount = batchSlices.stream()
                    .filter(item -> isConfirmedStatus(item.getScanStatus()))
                    .count();
            BigDecimal total = BigDecimal.valueOf(batchSlices.size());
            BigDecimal confirmed = BigDecimal.valueOf(confirmedCount);
            BigDecimal unconfirmed = total.subtract(confirmed);
            HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                    "SLITTING_STATUS:" + batch, "SLITTING", "分切报工状态", operation,
                    total, confirmed, unconfirmed, UNIT_PCS, SPLIT_MODE_PIECE);
            node.setBatchNo(batch);
            node.setSplitable(false);
            node.setSourceTable(TABLE_SLITTING_SLICE);
            node.setPlanned(true);
            node.setHasReport(true);
            node.setHasConfirmedReport(confirmedCount > 0);
            node.setReportState(unconfirmed.compareTo(BigDecimal.ZERO) > 0
                    ? REPORT_STATE_REPORTED_UNCONFIRMED
                    : REPORT_STATE_REPORTED_CONFIRMED);
            node.setRemark("当前批段分切报工状态，仅展示本批段切片数量。");
            applyInboundSplitInfo(node, operation, facts);
            graph.getNodes().add(node);
            addCandidateEdge(graph, operation, node);
        });
    }

    private void addPressSlotCandidates(HcPlanSplitGraphRespVO graph,
                                        HcPlanOrderOperationDO operation,
                                        ReportFacts facts) {
        if (operation == null || operation.getPlanId() == null) {
            return;
        }
        List<HcSlittingSliceRecordDO> sourceSlices = hcSlittingSliceRecordMapper.selectConfirmedListByPlanId(
                operation.getPlanId());
        if (sourceSlices.isEmpty()) {
            return;
        }
        Set<String> pressSlotInspectionPieceCodes = findPressSlotInspectionPieceCodes(sourceSlices.stream()
                .map(this::resolveSlittingPieceCode)
                .toList());
        List<HcSlittingSliceRecordDO> eligibleSlices = sourceSlices.stream()
                .filter(this::isEligibleSlittingPieceForPressSlotSplit)
                .filter(item -> !pressSlotInspectionPieceCodes.contains(resolveSlittingPieceCode(item)))
                .toList();
        if (eligibleSlices.isEmpty()) {
            return;
        }
        Set<Long> usedSliceIds = facts.pressSlotReports.stream()
                .filter(this::isConfirmedPressSlotReport)
                .map(HcPressSlotReportDO::getSourceSlittingSliceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> splitSliceIds = splitSourceIds(facts, TABLE_SLITTING_SLICE);
        Map<String, List<HcSlittingSliceRecordDO>> byBatch = eligibleSlices.stream()
                .collect(Collectors.groupingBy(item -> firstNotBlank(normalizeSegmentBatchNo(item.getSourceProductionBatchNo(),
                                item.getSourceBatchNo(), item.getSliceSerialNo()), "未分段"),
                        LinkedHashMap::new, Collectors.toList()));
        byBatch.forEach((batch, slices) -> {
            List<HcSlittingSliceRecordDO> available = slices.stream()
                    .filter(item -> !usedSliceIds.contains(item.getId()))
                    .filter(item -> !splitSliceIds.contains(item.getId()))
                    .toList();
            HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                    "PRESS_SLOT_REMAINING:" + batch, "PRESS_SLOT", "压槽未加工切片", operation,
                    BigDecimal.valueOf(slices.size()), BigDecimal.valueOf(slices.size() - available.size()),
                    BigDecimal.valueOf(available.size()), UNIT_PCS, SPLIT_MODE_PIECE);
            node.setBatchNo(batch);
            node.setSourceTable(TABLE_SLITTING_SLICE);
            node.setRemark("仅展示分切已确认、质量正常且未压槽确认的片号，可按片选择拆批。");
            applyInboundSplitInfo(node, operation, facts);
            available.forEach(slice -> node.getAvailableItems().add(item(slice.getId(), slice.getOutputStockId(),
                    TABLE_SLITTING_SLICE, firstNotBlank(slice.getSliceSerialNo(), slice.getSourceBatchNo()),
                    batch,
                    slice.getSourceProductionBatchNo(), BigDecimal.ONE, UNIT_PCS)));
            graph.getNodes().add(node);
            addCandidateEdge(graph, operation, node);
        });
    }

    private void addAdhesive2Candidates(HcPlanSplitGraphRespVO graph,
                                        HcPlanOrderOperationDO operation,
                                        ReportFacts facts) {
        Set<Long> usedPressIds = facts.adhesive2Reports.stream()
                .filter(this::isConfirmedAdhesive2Report)
                .map(HcAdhesive2ReportDO::getSourcePressSlotReportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> splitPressIds = splitSourceIds(facts, TABLE_PRESS_SLOT_REPORT);
        List<HcPressSlotReportDO> sourceReports = facts.pressSlotReports.stream()
                .filter(this::isConfirmedPressSlotReport)
                .toList();
        Set<String> pressSlotInspectionPieceCodes = findPressSlotInspectionPieceCodes(sourceReports.stream()
                .map(this::resolvePressSlotPieceCode)
                .toList());
        List<HcPressSlotReportDO> eligibleReports = sourceReports.stream()
                .filter(this::isEligiblePressSlotPieceForAdhesive2Split)
                .filter(item -> !pressSlotInspectionPieceCodes.contains(resolvePressSlotPieceCode(item)))
                .toList();
        if (eligibleReports.isEmpty()) {
            return;
        }
        Map<String, List<HcPressSlotReportDO>> byBatch = eligibleReports.stream()
                .collect(Collectors.groupingBy(item -> firstNotBlank(normalizeSegmentBatchNo(item.getSourceProductionBatchNo(),
                                item.getSourceBatchNo(), item.getProductionBatchNo()), "未分段"),
                        LinkedHashMap::new, Collectors.toList()));
        byBatch.forEach((batch, reports) -> {
            List<HcPressSlotReportDO> available = reports.stream()
                    .filter(item -> !usedPressIds.contains(item.getId()))
                    .filter(item -> !splitPressIds.contains(item.getId()))
                    .toList();
            HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                    "ADHESIVE2_REMAINING:" + batch, "ADHESIVE2", "粘胶2未加工切片", operation,
                    BigDecimal.valueOf(reports.size()), BigDecimal.valueOf(reports.size() - available.size()),
                    BigDecimal.valueOf(available.size()), UNIT_PCS, SPLIT_MODE_PIECE);
            node.setBatchNo(batch);
            node.setSourceTable(TABLE_PRESS_SLOT_REPORT);
            node.setRemark("仅展示压槽已确认、质量正常、非首检/过程加检且未粘胶2确认的片号，可按片选择拆批。");
            applyInboundSplitInfo(node, operation, facts);
            available.forEach(report -> node.getAvailableItems().add(item(report.getId(), report.getOutputStockId(),
                    TABLE_PRESS_SLOT_REPORT, firstNotBlank(report.getProductionBatchNo(), report.getSourceBatchNo()),
                    batch,
                    report.getSourceProductionBatchNo(), BigDecimal.ONE, UNIT_PCS)));
            graph.getNodes().add(node);
            addCandidateEdge(graph, operation, node);
        });
    }

    private boolean isEligibleSlittingPieceForPressSlotSplit(HcSlittingSliceRecordDO slice) {
        return slice != null
                && !isSlittingSliceNg(slice)
                && !hcNgInventoryService.isNgPieceManaged(SOURCE_TYPE_SLITTING, slice.getId());
    }

    private boolean isEligiblePressSlotPieceForAdhesive2Split(HcPressSlotReportDO report) {
        return report != null
                && !isPressSlotReportNg(report)
                && !hcNgInventoryService.isNgPieceManaged(SOURCE_TYPE_PRESS_SLOT, report.getId());
    }

    private Set<String> findPressSlotInspectionPieceCodes(Collection<String> pieceCodes) {
        List<String> normalizedCodes = pieceCodes == null ? List.of() : pieceCodes.stream()
                .map(this::normalizePieceCode)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (normalizedCodes.isEmpty()) {
            return Set.of();
        }
        return qmsFaiOrderMapper.selectListByProductBatchNos(normalizedCodes,
                        SOURCE_MENU_CODE_PRESS_SLOT, PROCESS_CODE_PRESS_SLOT)
                .stream()
                .map(QmsFaiOrderDO::getProductBatchNo)
                .map(this::normalizePieceCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
    }

    private String resolveSlittingPieceCode(HcSlittingSliceRecordDO slice) {
        return normalizePieceCode(slice == null ? null : firstNotBlank(slice.getSliceSerialNo(),
                slice.getSourceBatchNo(), slice.getSourceProductionBatchNo()));
    }

    private String resolvePressSlotPieceCode(HcPressSlotReportDO report) {
        return normalizePieceCode(report == null ? null : firstNotBlank(report.getProductionBatchNo(),
                report.getSourceBatchNo(), report.getSourceProductionBatchNo()));
    }

    private String normalizePieceCode(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private boolean isSlittingSliceNg(HcSlittingSliceRecordDO slice) {
        return slice != null && (isNgText(slice.getSelfCheck())
                || StrUtil.trimToEmpty(slice.getVisualResultJson()).toUpperCase(Locale.ROOT).contains("NG"));
    }

    private boolean isPressSlotReportNg(HcPressSlotReportDO report) {
        return report != null && (isNgText(report.getSelfCheck()) || StrUtil.isNotBlank(report.getDefectCode()));
    }

    private boolean isNgText(String value) {
        String text = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        return "NG".equals(text)
                || "ABNORMAL".equals(text)
                || "FAILED".equals(text)
                || "FAIL".equals(text)
                || "N".equals(text)
                || text.contains("不合格")
                || text.contains("异常");
    }

    private void addCutRoundCandidates(HcPlanSplitGraphRespVO graph,
                                       HcPlanOrderOperationDO operation,
                                       ReportFacts facts) {
        Set<Long> usedAdhesive2Ids = facts.cutRoundReports.stream()
                .filter(this::isConfirmedCutRoundReport)
                .map(HcCutRoundReportDO::getSourceAdhesive2ReportId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> splitAdhesive2Ids = splitSourceIds(facts, TABLE_ADHESIVE2_REPORT);
        List<HcAdhesive2ReportDO> sourceReports = facts.adhesive2Reports.stream()
                .filter(this::isConfirmedAdhesive2Report)
                .toList();
        Map<String, List<HcAdhesive2ReportDO>> byBatch = sourceReports.stream()
                .collect(Collectors.groupingBy(item -> firstNotBlank(normalizeSegmentBatchNo(item.getSourceProductionBatchNo(),
                                item.getSourceBatchNo(), item.getProductionBatchNo()), "未分段"),
                        LinkedHashMap::new, Collectors.toList()));
        byBatch.forEach((batch, reports) -> {
            List<HcAdhesive2ReportDO> available = reports.stream()
                    .filter(item -> !usedAdhesive2Ids.contains(item.getId()))
                    .filter(item -> !splitAdhesive2Ids.contains(item.getId()))
                    .toList();
            HcPlanSplitGraphRespVO.Node node = baseCandidateNode(
                    "CUT_ROUND_REMAINING:" + batch, "CUT_ROUND", "裁切未加工切片", operation,
                    BigDecimal.valueOf(reports.size()), BigDecimal.valueOf(reports.size() - available.size()),
                    BigDecimal.valueOf(available.size()), UNIT_PCS, SPLIT_MODE_PIECE);
            node.setBatchNo(batch);
            node.setSourceTable(TABLE_ADHESIVE2_REPORT);
            node.setRemark("粘胶2确认切片减去裁切确认切片，可按片选择拆批。");
            applyInboundSplitInfo(node, operation, facts);
            available.forEach(report -> node.getAvailableItems().add(item(report.getId(), report.getOutputStockId(),
                    TABLE_ADHESIVE2_REPORT, firstNotBlank(report.getProductionBatchNo(), report.getSourceBatchNo()),
                    batch,
                    report.getSourceProductionBatchNo(), BigDecimal.ONE, UNIT_PCS)));
            graph.getNodes().add(node);
            addCandidateEdge(graph, operation, node);
        });
    }

    private HcPlanSplitGraphRespVO.Node baseCandidateNode(String key,
                                                          String stageCode,
                                                          String stageName,
                                                          HcPlanOrderOperationDO operation,
                                                          BigDecimal totalQty,
                                                          BigDecimal processedQty,
                                                          BigDecimal remainingQty,
                                                          String unit,
                                                          String splitMode) {
        HcPlanSplitGraphRespVO.Node node = new HcPlanSplitGraphRespVO.Node();
        node.setNodeKey(key);
        node.setStageCode(stageCode);
        node.setStageName(stageName);
        if (operation != null) {
            node.setOperationId(operation.getId());
            node.setOpSeq(operation.getOpSeq());
            node.setOpCode(operation.getOpCode());
            node.setOpName(operation.getOpName());
            node.setOperationStatus(operation.getOperationStatus());
        }
        node.setTotalQty(scale(totalQty));
        node.setProcessedQty(scale(processedQty));
        node.setRemainingQty(scale(remainingQty));
        node.setUnit(unit);
        node.setSplitable(defaultDecimal(remainingQty).compareTo(BigDecimal.ZERO) > 0);
        node.setSplitMode(splitMode);
        node.setPlanned(operation != null);
        return node;
    }

    private void applyInboundSplitInfo(HcPlanSplitGraphRespVO.Node node,
                                       HcPlanOrderOperationDO operation,
                                       ReportFacts facts) {
        HcPlanSplitOrderDO splitOrder = findInboundSplitOrder(operation, facts);
        if (splitOrder == null) {
            return;
        }
        node.setTransferIn(true);
        node.setSourcePlanNo(splitOrder.getSourcePlanNo());
        node.setSourceOperationName(splitOrder.getSourceOperationName());
        node.setSourceSplitQty(scale(splitOrder.getSplitQty()));
        node.setSourceSplitUnit(firstNotBlank(splitOrder.getUnit(), node.getUnit()));
        node.setRemark(appendRemark(node.getRemark(),
                "利库转入：由计划 " + firstNotBlank(splitOrder.getSourcePlanNo(), "-")
                        + " 的 " + firstNotBlank(splitOrder.getSourceOperationName(), "-")
                        + " 拆出 " + formatQtyUnit(splitOrder.getSplitQty(),
                        firstNotBlank(splitOrder.getUnit(), node.getUnit()))));
    }

    private HcPlanSplitOrderDO findInboundSplitOrder(HcPlanOrderOperationDO operation, ReportFacts facts) {
        if (operation == null || facts == null || facts.inboundSplitOrders.isEmpty()) {
            return null;
        }
        String opCode = firstNotBlank(operation.getOpCode(), "");
        String opName = firstNotBlank(operation.getOpName(), "");
        return facts.inboundSplitOrders.stream()
                .filter(this::isEffectiveSplitOrder)
                .filter(item -> StrUtil.equals(item.getTargetStartOperationCode(), opCode)
                        || StrUtil.equals(item.getTargetStartOperationName(), opName)
                        || StrUtil.equals(item.getSourceOperationCode(), opCode)
                        || StrUtil.equals(item.getSourceOperationName(), opName))
                .findFirst()
                .orElse(null);
    }

    private void addCandidateEdge(HcPlanSplitGraphRespVO graph,
                                  HcPlanOrderOperationDO operation,
                                  HcPlanSplitGraphRespVO.Node node) {
        if (operation == null) {
            return;
        }
        HcPlanSplitGraphRespVO.Edge edge = new HcPlanSplitGraphRespVO.Edge();
        edge.setFrom("OP_" + operation.getId());
        edge.setTo(node.getNodeKey());
        edge.setLabel("余量");
        graph.getEdges().add(edge);
    }

    private HcPlanSplitGraphRespVO.Item item(Long sourceId,
                                             Long stockId,
                                             String sourceTable,
                                             String code,
                                             String batchNo,
                                             String sourceBatchNo,
                                             BigDecimal qty,
                                             String unit) {
        HcPlanSplitGraphRespVO.Item item = new HcPlanSplitGraphRespVO.Item();
        item.setSourceId(sourceId);
        item.setStockId(stockId);
        item.setSourceTable(sourceTable);
        item.setCode(code);
        item.setBatchNo(batchNo);
        item.setSourceBatchNo(sourceBatchNo);
        item.setQty(scale(qty));
        item.setUnit(unit);
        return item;
    }

    private HcPlanOrderOperationDO resolveStartOperation(HcPlanSplitCreateReqVO reqVO,
                                                         HcPlanSplitGraphRespVO.Node node,
                                                         List<HcPlanOrderOperationDO> operations) {
        Long startOperationId = reqVO.getStartOperationId() == null ? node.getOperationId() : reqVO.getStartOperationId();
        HcPlanOrderOperationDO operation = operations.stream()
                .filter(item -> Objects.equals(item.getId(), startOperationId))
                .findFirst()
                .orElse(null);
        if (operation == null) {
            throw invalidParamException("未找到拆批开始工序，请刷新后重试");
        }
        return operation;
    }

    private HcPlanOrderOperationDO resolveEndOperation(HcPlanSplitCreateReqVO reqVO,
                                                       List<HcPlanOrderOperationDO> operations) {
        if (reqVO.getEndOperationId() == null) {
            return null;
        }
        return operations.stream()
                .filter(item -> Objects.equals(item.getId(), reqVO.getEndOperationId()))
                .findFirst()
                .orElseThrow(() -> invalidParamException("未找到拆批结束工序，请刷新后重试"));
    }

    private List<HcPlanOrderOperationDO> selectTargetOperations(List<HcPlanOrderOperationDO> operations,
                                                               HcPlanOrderOperationDO startOperation,
                                                               HcPlanOrderOperationDO endOperation) {
        int startSeq = startOperation.getOpSeq() == null ? 0 : startOperation.getOpSeq();
        Integer endSeq = endOperation == null ? null : endOperation.getOpSeq();
        return operations.stream()
                .filter(item -> {
                    int seq = item.getOpSeq() == null ? 0 : item.getOpSeq();
                    return seq >= startSeq && (endSeq == null || seq <= endSeq);
                })
                .sorted(Comparator.comparing(item -> item.getOpSeq() == null ? 0 : item.getOpSeq()))
                .toList();
    }

    private HcPlanSplitOrderDO buildSplitOrder(HcPlanOrderDO sourcePlan,
                                               HcPlanSplitGraphRespVO.Node node,
                                               HcPlanOrderOperationDO startOperation,
                                               HcPlanOrderOperationDO endOperation,
                                               HcPlanSplitCreateReqVO reqVO,
                                               BigDecimal splitQty) {
        HcPlanSplitOrderDO order = new HcPlanSplitOrderDO();
        order.setSplitNo("CB" + LocalDateTime.now().format(SPLIT_NO_FORMATTER) + "-" + sourcePlan.getId());
        order.setSourcePlanId(sourcePlan.getId());
        order.setSourcePlanNo(sourcePlan.getPlanNo());
        order.setSourceOperationId(node.getOperationId());
        order.setSourceOperationCode(node.getOpCode());
        order.setSourceOperationName(node.getOpName());
        order.setSourceNodeKey(node.getNodeKey());
        order.setStageCode(node.getStageCode());
        order.setStageName(node.getStageName());
        order.setSplitMode(node.getSplitMode());
        order.setSplitQty(splitQty);
        order.setUnit(firstNotBlank(reqVO.getUnit(), node.getUnit()));
        order.setTargetStartOperationId(startOperation.getId());
        order.setTargetStartOperationCode(startOperation.getOpCode());
        order.setTargetStartOperationName(startOperation.getOpName());
        if (endOperation != null) {
            order.setTargetEndOperationId(endOperation.getId());
            order.setTargetEndOperationCode(endOperation.getOpCode());
            order.setTargetEndOperationName(endOperation.getOpName());
        }
        order.setTargetMaterialId(firstNonNull(reqVO.getTargetMaterialId(), sourcePlan.getMaterialId()));
        order.setTargetMaterialCode(firstNotBlank(reqVO.getTargetMaterialCode(), sourcePlan.getMaterialCode()));
        order.setTargetMaterialName(firstNotBlank(reqVO.getTargetMaterialName(), sourcePlan.getMaterialName()));
        order.setTargetModelId(firstNonNull(reqVO.getTargetModelId(), sourcePlan.getModelId()));
        order.setTargetModelCode(firstNotBlank(reqVO.getTargetModelCode(), sourcePlan.getModelCode()));
        order.setTargetModelName(firstNotBlank(reqVO.getTargetModelName(), sourcePlan.getModelName()));
        order.setInstructionText(reqVO.getInstructionText());
        order.setSplitStatus(STATUS_CREATED);
        order.setRemark(reqVO.getRemark());
        return order;
    }

    private List<HcPlanSplitDetailDO> buildSplitDetails(Long splitOrderId,
                                                        HcPlanSplitGraphRespVO.Node node,
                                                        HcPlanSplitCreateReqVO reqVO,
                                                        BigDecimal splitQty) {
        List<HcPlanSplitGraphRespVO.Item> availableItems = node.getAvailableItems() == null
                ? List.of()
                : node.getAvailableItems();
        Set<Long> selectedIds = reqVO.getSourceIds() == null ? Set.of() : new HashSet<>(reqVO.getSourceIds());
        List<HcPlanSplitGraphRespVO.Item> candidateItems = selectedIds.isEmpty()
                ? availableItems
                : availableItems.stream()
                .filter(item -> selectedIds.contains(item.getSourceId()))
                .toList();

        List<HcPlanSplitDetailDO> details = new ArrayList<>();
        BigDecimal remaining = splitQty;
        boolean pieceMode = SPLIT_MODE_PIECE.equalsIgnoreCase(node.getSplitMode());
        for (HcPlanSplitGraphRespVO.Item item : candidateItems) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal itemQty = pieceMode ? BigDecimal.ONE : defaultDecimal(item.getQty());
            BigDecimal useQty = pieceMode ? BigDecimal.ONE : itemQty.min(remaining);
            if (useQty.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            details.add(buildSplitDetail(splitOrderId, item, useQty, firstNotBlank(reqVO.getUnit(), node.getUnit())));
            remaining = remaining.subtract(useQty);
        }
        if (!availableItems.isEmpty() && remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw invalidParamException("选择的来源明细数量不足，剩余 {} {} 无法拆出", remaining, node.getUnit());
        }
        if (availableItems.isEmpty()) {
            HcPlanSplitGraphRespVO.Item item = item(null, null, node.getSourceTable(), node.getStageName(),
                    node.getBatchNo(), node.getSourceBatchNo(), splitQty, firstNotBlank(reqVO.getUnit(), node.getUnit()));
            details.add(buildSplitDetail(splitOrderId, item, splitQty, firstNotBlank(reqVO.getUnit(), node.getUnit())));
        }
        return details;
    }

    private HcPlanSplitDetailDO buildSplitDetail(Long splitOrderId,
                                                 HcPlanSplitGraphRespVO.Item item,
                                                 BigDecimal splitQty,
                                                 String unit) {
        HcPlanSplitDetailDO detail = new HcPlanSplitDetailDO();
        detail.setSplitOrderId(splitOrderId);
        detail.setSourceTable(item.getSourceTable());
        detail.setSourceId(item.getSourceId());
        detail.setStockId(item.getStockId());
        detail.setSourceBatchNo(item.getSourceBatchNo());
        detail.setSourceProductionBatchNo(item.getBatchNo());
        detail.setSourceCode(item.getCode());
        detail.setSplitQty(scale(splitQty));
        detail.setUnit(unit);
        detail.setDetailStatus(STATUS_CREATED);
        return detail;
    }

    private HcPlanOrderSaveReqVO buildNewPlanReq(HcPlanOrderDO sourcePlan,
                                                 List<HcPlanOrderOperationDO> targetOperations,
                                                 BigDecimal splitQty,
                                                 HcPlanSplitCreateReqVO reqVO,
                                                 List<HcPlanSplitDetailDO> details) {
        HcPlanOrderSaveReqVO planReq = BeanUtils.toBean(sourcePlan, HcPlanOrderSaveReqVO.class);
        planReq.setId(null);
        planReq.setPlanNo(null);
        planReq.setPlanStatus("DRAFT");
        planReq.setSourceType("PLAN_SPLIT");
        planReq.setSalesOrderId(null);
        planReq.setTargetQty(splitQty);
        planReq.setFgDeductQty(BigDecimal.ZERO);
        planReq.setNetPlanQty(splitQty);
        planReq.setBatchNo(null);
        planReq.setBatchRuleId(null);
        planReq.setBatchRuleCode(null);
        planReq.setRemark(joinRemarkParts(reqVO.getRemark(), "来源拆批计划：" + sourcePlan.getPlanNo()));
        applyTargetIdentity(planReq, reqVO);
        List<HcPlanOrderOperationReqVO> operationReqs = targetOperations.stream()
                .map(item -> toOperationReq(item, splitQty, reqVO.getInstructionText()))
                .toList();
        planReq.setOperations(operationReqs);
        planReq.setOperationCount(operationReqs.size());
        planReq.setInventoryLocks(buildInventoryLocks(sourcePlan, targetOperations.get(0), reqVO, details));
        return planReq;
    }

    private void applyTargetIdentity(HcPlanOrderSaveReqVO planReq, HcPlanSplitCreateReqVO reqVO) {
        if (reqVO.getTargetMaterialId() != null || StrUtil.isNotBlank(reqVO.getTargetMaterialCode())) {
            planReq.setMaterialId(reqVO.getTargetMaterialId());
            planReq.setMaterialCode(reqVO.getTargetMaterialCode());
            planReq.setMaterialName(reqVO.getTargetMaterialName());
        }
        if (reqVO.getTargetModelId() != null || StrUtil.isNotBlank(reqVO.getTargetModelCode())) {
            planReq.setModelId(reqVO.getTargetModelId());
            planReq.setModelCode(reqVO.getTargetModelCode());
            planReq.setModelName(reqVO.getTargetModelName());
        }
    }

    private HcPlanOrderOperationReqVO toOperationReq(HcPlanOrderOperationDO operation,
                                                     BigDecimal splitQty,
                                                     String instructionText) {
        HcPlanOrderOperationReqVO req = BeanUtils.toBean(operation, HcPlanOrderOperationReqVO.class);
        req.setId(null);
        req.setRequiredQty(splitQty);
        req.setDispatchQty(BigDecimal.ZERO);
        req.setLockedQty(BigDecimal.ZERO);
        req.setOperationStatus("DRAFT");
        req.setFinishTime(null);
        req.setFinishRemark(null);
        req.setStatusOperatorId(null);
        req.setStatusOperatorName(null);
        req.setStatusOperateTime(null);
        req.setStatusDateMarksJson(null);
        req.setSplitMark(null);
        req.setSourcePlanId(null);
        req.setSourcePlanNo(null);
        req.setSourcePlanOperationId(null);
        req.setSourceOperationCode(null);
        req.setSourceOperationName(null);
        req.setInstructionText(firstNotBlank(instructionText, operation.getInstructionText()));
        return req;
    }

    private List<HcPlanOrderInventoryLockReqVO> buildInventoryLocks(HcPlanOrderDO sourcePlan,
                                                                    HcPlanOrderOperationDO startOperation,
                                                                    HcPlanSplitCreateReqVO reqVO,
                                                                    List<HcPlanSplitDetailDO> details) {
        return details.stream()
                .filter(item -> item.getStockId() != null)
                .map(detail -> {
                    HcInvStockDO stock = hcInvStockMapper.selectById(detail.getStockId());
                    if (stock == null) {
                        throw invalidParamException("来源库存 {} 不存在，无法生成拆批计划锁定", detail.getStockId());
                    }
                    HcPlanOrderInventoryLockReqVO lock = new HcPlanOrderInventoryLockReqVO();
                    lock.setLockType(STOCK_TYPE_WIP);
                    lock.setStockType(STOCK_TYPE_WIP);
                    lock.setStockId(stock.getId());
                    lock.setSourceType(stock.getSourceType());
                    lock.setSourceTable(firstNotBlank(detail.getSourceTable(), stock.getSourceTable()));
                    lock.setSourceId(firstNonNull(detail.getSourceId(), stock.getSourceId()));
                    lock.setSourcePlanId(sourcePlan.getId());
                    lock.setSourcePlanNo(sourcePlan.getPlanNo());
                    lock.setSourcePlanOperationId(stock.getSourcePlanOperationId());
                    lock.setSourceBatchNo(firstNotBlank(detail.getSourceBatchNo(), stock.getSourceBatchNo(), stock.getBatchNo()));
                    lock.setBatchNo(firstNotBlank(detail.getSourceProductionBatchNo(), stock.getBatchNo(), stock.getSourceBatchNo()));
                    lock.setOpSeq(stock.getOpSeq());
                    lock.setOpCode(stock.getOpCode());
                    lock.setOpName(stock.getOpName());
                    lock.setSegmentCode(stock.getSegmentCode());
                    lock.setSegmentName(stock.getSegmentName());
                    lock.setMaterialId(firstNonNull(reqVO.getTargetMaterialId(), stock.getMaterialId()));
                    lock.setMaterialCode(firstNotBlank(reqVO.getTargetMaterialCode(), stock.getMaterialCode()));
                    lock.setMaterialName(firstNotBlank(reqVO.getTargetMaterialName(), stock.getMaterialName()));
                    lock.setModelNo(firstNotBlank(reqVO.getTargetModelCode(), stock.getModelNo()));
                    lock.setProductionDate(stock.getProductionDate());
                    lock.setExpiryDate(stock.getExpiryDate());
                    lock.setAvailableQty(stock.getAvailableQty());
                    lock.setLockQty(detail.getSplitQty());
                    lock.setConsumedQty(BigDecimal.ZERO);
                    lock.setReleasedQty(BigDecimal.ZERO);
                    lock.setRemainingQty(detail.getSplitQty());
                    lock.setTargetOpCode(startOperation.getOpCode());
                    lock.setTargetOpName(startOperation.getOpName());
                    lock.setUnitCode(firstNotBlank(detail.getUnit(), stock.getUom()));
                    lock.setUom(firstNotBlank(detail.getUnit(), stock.getUom()));
                    lock.setLockStatus(STATUS_ACTIVE);
                    lock.setRemark("PLAN_SPLIT；拆批生成新计划锁定来源库存；来源计划：" + sourcePlan.getPlanNo());
                    return lock;
                })
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private HcPlanOrderOperationDO findOperation(List<HcPlanOrderOperationDO> operations, String... keywords) {
        return operations.stream()
                .filter(item -> containsAny(firstNotBlank(item.getOpName(), item.getOpCode()), keywords))
                .findFirst()
                .orElse(null);
    }

    private boolean isEffectiveSplitOrder(HcPlanSplitOrderDO splitOrder) {
        return splitOrder != null
                && (splitOrder.getTargetPlanId() != null
                || StrUtil.equals(splitOrder.getSplitStatus(), STATUS_PLAN_CREATED));
    }

    private Set<Long> splitSourceIds(ReportFacts facts, String sourceTable) {
        return facts.splitDetails.stream()
                .filter(item -> StrUtil.equals(item.getSourceTable(), sourceTable))
                .map(HcPlanSplitDetailDO::getSourceId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Map<Long, BigDecimal> splitQtyBySourceId(ReportFacts facts, String sourceTable) {
        return facts.splitDetails.stream()
                .filter(item -> StrUtil.equals(item.getSourceTable(), sourceTable))
                .filter(item -> item.getSourceId() != null)
                .collect(Collectors.groupingBy(HcPlanSplitDetailDO::getSourceId,
                        Collectors.mapping(HcPlanSplitDetailDO::getSplitQty,
                                Collectors.reducing(BigDecimal.ZERO, this::defaultDecimal, BigDecimal::add))));
    }

    private boolean containsAny(String value, String... keywords) {
        if (StrUtil.isBlank(value)) {
            return false;
        }
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal sum(Collection<?> list, Function<Object, BigDecimal> getter) {
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @SuppressWarnings("unchecked")
    private <T> BigDecimal sum(List<T> list, Function<T, BigDecimal> getter) {
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return list.stream()
                .map(getter)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return defaultDecimal(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private BigDecimal nonNegative(BigDecimal value) {
        BigDecimal decimal = defaultDecimal(value);
        return decimal.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : decimal;
    }

    String normalizeSegmentBatchNo(String... values) {
        String value = firstNotBlank(values);
        if (StrUtil.isBlank(value)) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        Matcher adhesiveMatcher = ADHESIVE_BATCH_SUFFIX_PATTERN.matcher(normalized);
        if (adhesiveMatcher.matches()) {
            normalized = adhesiveMatcher.group(1);
        }
        Matcher slittingMatcher = SLITTING_BATCH_SUFFIX_PATTERN.matcher(normalized);
        if (slittingMatcher.matches()) {
            normalized = slittingMatcher.group(1);
        }
        Matcher pieceMatcher = PIECE_SERIAL_SUFFIX_PATTERN.matcher(normalized);
        if (pieceMatcher.matches()) {
            normalized = pieceMatcher.group(1);
        }
        return normalized;
    }

    private String firstNotBlank(String... values) {
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

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
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

    private static class ReportFacts {
        private List<HcProcessReportDO> processReports = List.of();
        private List<HcGrindingReportDO> grindingReports = List.of();
        private List<HcGrindingFirstDetailDO> grindingFirstDetails = List.of();
        private List<HcGrindingFirstAllocationDO> grindingFirstAllocations = List.of();
        private List<HcGrindingSecondDetailDO> grindingSecondDetails = List.of();
        private List<HcAdhesiveReportDO> adhesiveReports = List.of();
        private List<HcSlittingSliceRecordDO> slittingSlices = List.of();
        private List<HcPressSlotReportDO> pressSlotReports = List.of();
        private List<HcAdhesive2ReportDO> adhesive2Reports = List.of();
        private List<HcCutRoundReportDO> cutRoundReports = List.of();
        private List<HcPlanSplitOrderDO> splitOrders = List.of();
        private List<HcPlanSplitOrderDO> inboundSplitOrders = List.of();
        private List<HcPlanSplitDetailDO> splitDetails = List.of();
    }

}
