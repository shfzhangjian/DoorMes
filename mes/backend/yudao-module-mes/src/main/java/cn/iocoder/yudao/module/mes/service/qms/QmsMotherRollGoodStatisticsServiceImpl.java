package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsStageRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldTargetConfigDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsYieldTargetConfigMapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class QmsMotherRollGoodStatisticsServiceImpl implements QmsMotherRollGoodStatisticsService {

    private static final String LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String STOCK_TYPE_WIP = "WIP";
    private static final String PLAN_MODE_DISCRETE_POST = "DISCRETE_POST";
    private static final String SOURCE_TYPE_DISCRETE_NG = "NG_INVENTORY";
    private static final String SOURCE_TYPE_DISCRETE_WIP = "DISCRETE_WIP";
    private static final String DISCRETE_POST_PROCESS_MARK = "DISCRETE_POST_PROCESS";
    private static final int PIVOT_STAGE_QUERY_BATCH_SIZE = 200;
    private static final List<String> PIVOT_STAGE_CODES = List.of(
            "FORMULA", "WET", "GRINDING", "ADHESIVE1", "SLITTING",
            "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND", "SHIPPING_INSPECTION");
    private static final Set<String> PIVOT_PIECE_STAGE_CODES = Set.of(
            "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> PIVOT_SUBMITTED_REPORT_STAGE_CODES = Set.of(
            "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> MOTHER_OUTPUT_STAGE_CODES = Set.of(
            "GRINDING", "ADHESIVE1", "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> PIECE_THEORETICAL_OUTPUT_STAGE_CODES = Set.of(
            "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> PIECE_INPUT_YIELD_STAGE_CODES = Set.of(
            "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> RATED_LOSS_GOOD_YIELD_STAGE_CODES = Set.of(
            "PRESS_SLOT", "ADHESIVE2");
    private static final Set<String> COA_INSPECTION_DEDUCT_STAGE_CODES = Set.of(
            "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Set<String> GOOD_YIELD_RATE_STAGE_CODES = Set.of(
            "FORMULA", "WET", "GRINDING", "ADHESIVE1",
            "SLITTING", "PRESS_SLOT", "ADHESIVE2", "CUT_ROUND");
    private static final Pattern MOTHER_SEGMENT_BATCH_NO_PATTERN =
            Pattern.compile("^[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS]$");
    private static final Pattern SEGMENT_MARK_PATTERN =
            Pattern.compile("^[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]([PQRS])$");
    private static final Pattern PIECE_BATCH_NO_PATTERN =
            Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS])[0-9]{3}[A-Z]?$");
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
    private static final Map<String, String> TARGET_PROCESS_CODE_BY_STAGE = Map.of(
            "FORMULA", "FORMULA",
            "WET", "WET",
            "GRINDING", "ROUGH_GRINDING",
            "ADHESIVE1", "ADHESIVE1",
            "SLITTING", "SLITTING",
            "PRESS_SLOT", "PRESS_SLOT",
            "ADHESIVE2", "ADHESIVE2",
            "CUT_ROUND", "CUT_ROUND",
            "SHIPPING_INSPECTION", "FINAL_INSPECTION");

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;

    @Resource
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;

    @Resource
    private HcInvStockMapper hcInvStockMapper;

    @Resource
    private HcProcessReportMapper hcProcessReportMapper;

    @Resource
    private QmsYieldTargetConfigMapper qmsYieldTargetConfigMapper;

    @Override
    public PageResult<QmsMotherRollGoodStatisticsRespVO> getMotherRollGoodStatisticsPage(QmsMotherRollGoodStatisticsPageReqVO pageReqVO) {
        List<QmsMotherRollGoodStatisticsRespVO> list = getMotherRollGoodStatisticsList(pageReqVO);
        if (list.isEmpty()) {
            return PageResult.empty();
        }
        return paginatePivotRows(list, pageReqVO);
    }

    @Override
    public List<QmsMotherRollGoodStatisticsRespVO> getMotherRollGoodStatisticsList(QmsMotherRollGoodStatisticsPageReqVO pageReqVO) {
        List<HcPlanOrderDO> plans = hcPlanOrderMapper.selectList(pageReqVO);
        if (plans == null || plans.isEmpty()) {
            return List.of();
        }
        plans = plans.stream()
                .filter(plan -> !isDiscreteNgPostProcessPlan(plan))
                .toList();
        if (plans.isEmpty()) {
            return List.of();
        }
        List<Long> planIds = plans.stream().map(HcPlanOrderDO::getId).toList();
        Map<Long, PostProcessSourceInfo> postProcessSourceInfoMap = selectPostProcessSourceInfoMap(planIds);
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotStageRow>> stageRowsMap = selectPivotStageRowsMap(planIds);
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotPieceRow>> pieceRowsMap = selectPivotPieceRowsMap(planIds);
        Map<Long, List<HcProcessReportMapper.PlanProcessPivotInspectionRow>> inspectionRowsMap =
                selectPivotInspectionRowsMap(planIds);
        List<QmsMotherRollGoodStatisticsRespVO> list = plans.stream()
                .flatMap(plan -> {
                    PostProcessSourceInfo sourceInfo = postProcessSourceInfoMap.get(plan.getId());
                    List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows = stageRowsMap.get(plan.getId());
                    List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows = pieceRowsMap.get(plan.getId());
                    List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows =
                            inspectionRowsMap.get(plan.getId());
                    if (shouldSkipEmptyMotherRollPlaceholder(plan, sourceInfo, stageRows, pieceRows, inspectionRows)) {
                        return java.util.stream.Stream.<QmsMotherRollGoodStatisticsRespVO>empty();
                    }
                    List<QmsMotherRollGoodStatisticsRespVO> rows = buildPlanProcessPivotRespList(
                            plan, stageRows, pieceRows, inspectionRows);
                    fillPostProcessPlanInfo(rows, sourceInfo);
                    return rows.stream();
                })
                .toList();
        List<QmsYieldTargetConfigDO> enabledTargetConfigs = qmsYieldTargetConfigMapper.selectEnabledList().stream()
                .filter(this::hasAvailableTargetConfig)
                .toList();
        Map<String, TheoreticalOutputBase> theoreticalOutputBaseMap =
                buildTheoreticalOutputBaseMap(enabledTargetConfigs);
        Map<String, Integer> motherSegmentCountMap = buildMotherSegmentCountMap(list, theoreticalOutputBaseMap);
        if (StrUtil.isNotBlank(pageReqVO.getMotherSegmentBatchNo())) {
            String motherSegmentBatchNo = pageReqVO.getMotherSegmentBatchNo().trim();
            list = list.stream()
                    .filter(row -> StrUtil.containsIgnoreCase(row.getSegmentBatchNo(), motherSegmentBatchNo))
                    .toList();
        }
        list = aggregateMotherRollRows(list);
        applyMotherOutputAndTheoreticalTargets(list, motherSegmentCountMap, enabledTargetConfigs,
                theoreticalOutputBaseMap);
        return list;
    }

    private boolean shouldSkipEmptyMotherRollPlaceholder(
            HcPlanOrderDO plan,
            PostProcessSourceInfo sourceInfo,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows) {
        return !hasPlanMotherBatch(plan)
                && !hasEffectivePostProcessSource(sourceInfo)
                && isEmpty(stageRows)
                && isEmpty(pieceRows)
                && isEmpty(inspectionRows);
    }

    private boolean hasPlanMotherBatch(HcPlanOrderDO plan) {
        return plan != null && StrUtil.isNotBlank(firstNotBlank(
                plan.getParentProductionBatchNo(),
                plan.getProductionBatchNo(),
                plan.getBatchNo()));
    }

    private boolean hasEffectivePostProcessSource(PostProcessSourceInfo sourceInfo) {
        return sourceInfo != null && StrUtil.isNotBlank(firstNotBlank(
                sourceInfo.motherBatchNo(),
                sourceInfo.grindingSegmentBatchNo()));
    }

    private boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }

    private boolean isDiscreteNgPostProcessPlan(HcPlanOrderDO plan) {
        if (plan == null) {
            return false;
        }
        String planMode = StrUtil.trimToEmpty(plan.getPlanMode());
        String sourceType = StrUtil.trimToEmpty(plan.getSourceType());
        return PLAN_MODE_DISCRETE_POST.equalsIgnoreCase(planMode)
                || SOURCE_TYPE_DISCRETE_NG.equalsIgnoreCase(sourceType)
                || SOURCE_TYPE_DISCRETE_WIP.equalsIgnoreCase(sourceType)
                || containsIgnoreCase(plan.getRemark(), DISCRETE_POST_PROCESS_MARK)
                || containsIgnoreCase(plan.getRouteSnapshotJson(), DISCRETE_POST_PROCESS_MARK)
                || containsIgnoreCase(plan.getRouteSnapshotJson(), SOURCE_TYPE_DISCRETE_NG);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return StrUtil.isNotBlank(text)
                && StrUtil.isNotBlank(keyword)
                && text.toUpperCase(Locale.ROOT).contains(keyword.toUpperCase(Locale.ROOT));
    }

    private List<QmsMotherRollGoodStatisticsRespVO> aggregateMotherRollRows(
            List<QmsMotherRollGoodStatisticsRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        Map<String, QmsMotherRollGoodStatisticsRespVO> aggregateRows = new LinkedHashMap<>();
        Map<String, Map<String, Set<String>>> mergedStageKeys = new LinkedHashMap<>();
        for (QmsMotherRollGoodStatisticsRespVO source : rows) {
            String groupKey = motherRollGroupKey(source);
            String displayKey = motherRollDisplayKey(source);
            String aggregateKey = groupKey + "|" + displayKey;
            QmsMotherRollGoodStatisticsRespVO target = aggregateRows.computeIfAbsent(
                    aggregateKey, key -> createMotherRollAggregateRow(source, groupKey, displayKey));
            mergeMotherRollAggregateBase(target, source);
            Map<String, Set<String>> stageKeyMap =
                    mergedStageKeys.computeIfAbsent(aggregateKey, key -> new LinkedHashMap<>());
            mergeMotherRollAggregateStages(target, source, stageKeyMap);
        }
        List<QmsMotherRollGoodStatisticsRespVO> result = new ArrayList<>(aggregateRows.values());
        result.forEach(row -> {
            if (row.getStages() != null) {
                row.getStages().values().forEach(stage -> {
                    refreshPieceStageFromDetails(stage);
                    refreshInspectionStageFromDetails(stage);
                    refreshStageStatus(stage);
                });
                fillPivotSummary(row, row.getStages());
            }
        });
        result.sort(Comparator
                .comparing(this::motherRollGroupKey, Comparator.nullsLast(String::compareTo))
                .thenComparing(this::motherRollDisplayKey, Comparator.nullsLast(String::compareTo))
                .thenComparing(row -> firstNotBlank(row.getPivotRowKey(), ""), Comparator.nullsLast(String::compareTo)));
        return result;
    }

    private String motherRollGroupKey(QmsMotherRollGoodStatisticsRespVO row) {
        String motherBatchNo = firstNotBlank(motherBatchKey(row), "-");
        String modelSeriesCode = firstNotBlank(aggregateModelSeriesCode(row), "-");
        return motherBatchNo + "|" + modelSeriesCode;
    }

    private String motherRollDisplayKey(QmsMotherRollGoodStatisticsRespVO row) {
        return normalizeCode(firstNotBlank(row.getSegmentBatchNo(), row.getMotherRollBatchNo(), motherBatchKey(row), "-"));
    }

    private QmsMotherRollGoodStatisticsRespVO createMotherRollAggregateRow(
            QmsMotherRollGoodStatisticsRespVO source,
            String groupKey,
            String displayKey) {
        QmsMotherRollGoodStatisticsRespVO target = BeanUtils.toBean(source, QmsMotherRollGoodStatisticsRespVO.class);
        String modelSeriesCode = aggregateModelSeriesCode(source);
        String sizeSpec = aggregateSizeSpec(source);
        target.setId(null);
        target.setPlanNo(null);
        target.setPlanStatus(null);
        target.setPlanNoTagText(null);
        String motherBatchNo = firstNotBlank(motherBatchKey(source), source.getMotherRollBatchNo());
        target.setMotherRollBatchNo(motherBatchNo);
        target.setModelCode(modelSeriesCode);
        target.setModelSeriesCode(modelSeriesCode);
        target.setActualModelCode(modelSeriesCode);
        target.setSizeName(sizeSpec);
        target.setSizeSpec(sizeSpec);
        target.setActualSizeSpec(sizeSpec);
        target.setSegmentBatchNo(firstNotBlank(source.getSegmentBatchNo(), displayKey, motherBatchNo));
        target.setPivotRowKey("MOTHER_ROW|" + groupKey + "|" + displayKey);
        target.setPlanMergeKey("MOTHER|" + groupKey);
        target.setSegmentMergeKey("MOTHER_SEGMENT|" + groupKey + "|" + displayKey);
        target.setModelSizeMergeKey("MOTHER_MODEL_SIZE|" + groupKey);
        target.setVariationStartStageCode(null);
        target.setVariationStartStageName(null);
        target.setStageMergeKeys(buildMotherRollAggregateStageMergeKeys(groupKey, displayKey));
        target.setStages(initPivotStages());
        target.setTotalDefectQty(BigDecimal.ZERO);
        target.setLatestReportTime(null);
        return target;
    }

    private Map<String, String> buildMotherRollAggregateStageMergeKeys(String groupKey, String displayKey) {
        Map<String, String> mergeKeys = new LinkedHashMap<>();
        for (String stageCode : PIVOT_STAGE_CODES) {
            if (isPivotPlanLevelStage(stageCode)) {
                mergeKeys.put(stageCode, "MOTHER_STAGE|" + groupKey + "|" + stageCode);
            } else {
                mergeKeys.put(stageCode, "MOTHER_STAGE|" + groupKey + "|" + displayKey + "|" + stageCode);
            }
        }
        return mergeKeys;
    }

    private void mergeMotherRollAggregateBase(QmsMotherRollGoodStatisticsRespVO target,
                                              QmsMotherRollGoodStatisticsRespVO source) {
        target.setPostProcessFlag(Boolean.TRUE.equals(target.getPostProcessFlag())
                || Boolean.TRUE.equals(source.getPostProcessFlag()));
        target.setMaterialCode(mergeDistinctText(target.getMaterialCode(), source.getMaterialCode()));
        target.setMaterialName(mergeDistinctText(target.getMaterialName(), source.getMaterialName()));
        target.setMotherMaterialCode(mergeDistinctText(target.getMotherMaterialCode(), source.getMotherMaterialCode()));
        target.setMotherMaterialName(mergeDistinctText(target.getMotherMaterialName(), source.getMotherMaterialName()));
        target.setMotherModelCode(firstNotBlank(target.getMotherModelCode(), source.getMotherModelCode()));
        target.setMotherModelName(firstNotBlank(target.getMotherModelName(), source.getMotherModelName()));
        String mergedSizeSpec = mergeDistinctText(target.getActualSizeSpec(), aggregateSizeSpec(source));
        target.setSizeName(mergedSizeSpec);
        target.setSizeSpec(mergedSizeSpec);
        target.setActualSizeSpec(mergedSizeSpec);
        target.setBatchNo(mergeDistinctText(target.getBatchNo(), source.getBatchNo()));
        target.setProductionBatchNo(mergeDistinctText(target.getProductionBatchNo(), source.getProductionBatchNo()));
        target.setParentProductionBatchNo(mergeDistinctText(
                target.getParentProductionBatchNo(), source.getParentProductionBatchNo()));
        target.setSegmentBatchNo(mergeDistinctText(target.getSegmentBatchNo(), source.getSegmentBatchNo()));
        target.setLatestReportTime(latestTime(target.getLatestReportTime(), source.getLatestReportTime()));
    }

    private void mergeMotherRollAggregateStages(
            QmsMotherRollGoodStatisticsRespVO target,
            QmsMotherRollGoodStatisticsRespVO source,
            Map<String, Set<String>> mergedStageKeys) {
        if (source.getStages() == null || source.getStages().isEmpty()) {
            return;
        }
        for (String stageCode : PIVOT_STAGE_CODES) {
            QmsMotherRollGoodStatisticsStageRespVO sourceStage = source.getStages().get(stageCode);
            QmsMotherRollGoodStatisticsStageRespVO targetStage = target.getStages().get(stageCode);
            if (sourceStage == null || targetStage == null) {
                continue;
            }
            String contributionKey = aggregateStageContributionKey(source, stageCode);
            Set<String> stageKeys = mergedStageKeys.computeIfAbsent(stageCode, key -> new LinkedHashSet<>());
            if (stageKeys.add(contributionKey)) {
                mergeMotherRollAggregateStageValue(targetStage, sourceStage);
            }
            mergeAggregatePieceDetails(targetStage, sourceStage.getPieceDetails());
            mergeAggregateInspectionDetails(targetStage, sourceStage.getInspectionDetails());
        }
    }

    private String aggregateStageContributionKey(QmsMotherRollGoodStatisticsRespVO row, String stageCode) {
        return firstNotBlank(
                row.getStageMergeKeys() == null ? null : row.getStageMergeKeys().get(stageCode),
                row.getSegmentMergeKey() == null ? null : row.getSegmentMergeKey() + "|" + stageCode,
                row.getPivotRowKey() == null ? null : row.getPivotRowKey() + "|" + stageCode,
                String.valueOf(row.getId()) + "|" + stageCode,
                stageCode);
    }

    private void mergeMotherRollAggregateStageValue(QmsMotherRollGoodStatisticsStageRespVO target,
                                                    QmsMotherRollGoodStatisticsStageRespVO source) {
        target.setSourceBatchNos(mergeDistinctText(target.getSourceBatchNos(), source.getSourceBatchNos()));
        target.setOutputBatchNos(mergeDistinctText(target.getOutputBatchNos(), source.getOutputBatchNos()));
        target.setInputQty(zeroIfNull(target.getInputQty()).add(zeroIfNull(source.getInputQty())));
        target.setSegmentInputQty(zeroIfNull(target.getSegmentInputQty()).add(zeroIfNull(source.getSegmentInputQty())));
        target.setReportQty(zeroIfNull(target.getReportQty()).add(zeroIfNull(source.getReportQty())));
        target.setDoneQty(zeroIfNull(target.getDoneQty()).add(zeroIfNull(source.getDoneQty())));
        target.setPendingQty(zeroIfNull(target.getPendingQty()).add(zeroIfNull(source.getPendingQty())));
        target.setDefectQty(zeroIfNull(target.getDefectQty()).add(zeroIfNull(source.getDefectQty())));
        target.setSegmentDefectQty(zeroIfNull(target.getSegmentDefectQty()).add(zeroIfNull(source.getSegmentDefectQty())));
        target.setInspectionQty(zeroIfNull(target.getInspectionQty()).add(zeroIfNull(source.getInspectionQty())));
        target.setCoaInspectionQty(zeroIfNull(target.getCoaInspectionQty()).add(zeroIfNull(source.getCoaInspectionQty())));
        target.setGlueBoardInspectionQty(zeroIfNull(target.getGlueBoardInspectionQty()).add(zeroIfNull(source.getGlueBoardInspectionQty())));
        target.setInspectionNgQty(zeroIfNull(target.getInspectionNgQty()).add(zeroIfNull(source.getInspectionNgQty())));
        target.setCoaInspectionNgQty(zeroIfNull(target.getCoaInspectionNgQty()).add(zeroIfNull(source.getCoaInspectionNgQty())));
        target.setConfirmedQty(zeroIfNull(target.getConfirmedQty()).add(zeroIfNull(source.getConfirmedQty())));
        target.setLengthQty(zeroIfNull(target.getLengthQty()).add(zeroIfNull(source.getLengthQty())));
        target.setStartPosition(firstNonNull(target.getStartPosition(), source.getStartPosition()));
        target.setProcessLength(firstNonNull(target.getProcessLength(), source.getProcessLength()));
        target.setReportUnit(firstNotBlank(source.getReportUnit(), target.getReportUnit()));
        target.setPendingUnit(firstNotBlank(source.getPendingUnit(), target.getPendingUnit(), target.getReportUnit()));
        target.setLengthUnit(firstNotBlank(source.getLengthUnit(), target.getLengthUnit(), "m"));
        target.setLastReportTime(latestTime(target.getLastReportTime(), source.getLastReportTime()));
        target.setRemark(firstNotBlank(target.getRemark(), source.getRemark()));
    }

    private void mergeAggregatePieceDetails(
            QmsMotherRollGoodStatisticsStageRespVO target,
            List<QmsMotherRollGoodStatisticsPieceRespVO> sourceDetails) {
        if (sourceDetails == null || sourceDetails.isEmpty()) {
            return;
        }
        List<QmsMotherRollGoodStatisticsPieceRespVO> targetDetails =
                new ArrayList<>(target.getPieceDetails() == null ? List.of() : target.getPieceDetails());
        for (QmsMotherRollGoodStatisticsPieceRespVO sourceDetail : sourceDetails) {
            String detailKey = aggregatePieceDetailKey(sourceDetail);
            QmsMotherRollGoodStatisticsPieceRespVO targetDetail = targetDetails.stream()
                    .filter(item -> detailKey.equals(aggregatePieceDetailKey(item)))
                    .findFirst()
                    .orElse(null);
            if (targetDetail == null) {
                targetDetails.add(BeanUtils.toBean(sourceDetail, QmsMotherRollGoodStatisticsPieceRespVO.class));
            } else {
                mergeAggregatePieceDetail(targetDetail, sourceDetail);
            }
        }
        target.setPieceDetails(targetDetails);
    }

    private String aggregatePieceDetailKey(QmsMotherRollGoodStatisticsPieceRespVO item) {
        return normalizePivotPieceKey(firstNotBlank(
                item.getPieceNo(), item.getOutputBatchNo(), item.getSourceBatchNo(), "-"));
    }

    private void mergeAggregatePieceDetail(QmsMotherRollGoodStatisticsPieceRespVO target,
                                           QmsMotherRollGoodStatisticsPieceRespVO source) {
        target.setSourceBatchNo(firstNotBlank(target.getSourceBatchNo(), source.getSourceBatchNo()));
        target.setOutputBatchNo(firstNotBlank(target.getOutputBatchNo(), source.getOutputBatchNo(), source.getPieceNo()));
        target.setActualModelCode(firstNotBlank(target.getActualModelCode(), source.getActualModelCode()));
        target.setActualSizeSpec(firstNotBlank(target.getActualSizeSpec(), source.getActualSizeSpec()));
        target.setReportConfirmed(Boolean.TRUE.equals(target.getReportConfirmed())
                || Boolean.TRUE.equals(source.getReportConfirmed()));
        target.setDefectFlag(Boolean.TRUE.equals(target.getDefectFlag()) || Boolean.TRUE.equals(source.getDefectFlag()));
        target.setSourceSlittingOkFlag(Boolean.TRUE.equals(target.getSourceSlittingOkFlag())
                && Boolean.TRUE.equals(source.getSourceSlittingOkFlag()));
        target.setCoaFlag(Boolean.TRUE.equals(target.getCoaFlag()) || Boolean.TRUE.equals(source.getCoaFlag()));
        target.setLastReportTime(latestTime(target.getLastReportTime(), source.getLastReportTime()));
        target.setRemark(firstNotBlank(target.getRemark(), source.getRemark()));
        target.setStatus(resolvePieceStatus(target));
    }

    private void mergeAggregateInspectionDetails(
            QmsMotherRollGoodStatisticsStageRespVO target,
            List<QmsMotherRollGoodStatisticsInspectionRespVO> sourceDetails) {
        if (sourceDetails == null || sourceDetails.isEmpty()) {
            return;
        }
        List<QmsMotherRollGoodStatisticsInspectionRespVO> targetDetails =
                new ArrayList<>(target.getInspectionDetails() == null ? List.of() : target.getInspectionDetails());
        for (QmsMotherRollGoodStatisticsInspectionRespVO sourceDetail : sourceDetails) {
            String detailKey = aggregateInspectionDetailKey(sourceDetail);
            QmsMotherRollGoodStatisticsInspectionRespVO targetDetail = targetDetails.stream()
                    .filter(item -> detailKey.equals(aggregateInspectionDetailKey(item)))
                    .findFirst()
                    .orElse(null);
            if (targetDetail == null) {
                targetDetails.add(BeanUtils.toBean(sourceDetail, QmsMotherRollGoodStatisticsInspectionRespVO.class));
            } else {
                mergeAggregateInspectionDetail(targetDetail, sourceDetail);
            }
        }
        target.setInspectionDetails(targetDetails);
    }

    private String aggregateInspectionDetailKey(QmsMotherRollGoodStatisticsInspectionRespVO item) {
        return normalizeCode(firstNotBlank(
                item.getSourceType(), "-")) + "|" + firstNotBlank(
                item.getInspectionId() == null ? null : String.valueOf(item.getInspectionId()),
                item.getInspectionNo(),
                item.getProductBatchNo(),
                "-");
    }

    private void mergeAggregateInspectionDetail(QmsMotherRollGoodStatisticsInspectionRespVO target,
                                                QmsMotherRollGoodStatisticsInspectionRespVO source) {
        target.setProductBatchNo(firstNotBlank(target.getProductBatchNo(), source.getProductBatchNo()));
        target.setInspectionNo(firstNotBlank(target.getInspectionNo(), source.getInspectionNo()));
        target.setInspectionType(firstNotBlank(target.getInspectionType(), source.getInspectionType()));
        target.setSourceSlittingOkFlag(Boolean.TRUE.equals(target.getSourceSlittingOkFlag())
                && Boolean.TRUE.equals(source.getSourceSlittingOkFlag()));
        target.setCoaInspectionFlag(Boolean.TRUE.equals(target.getCoaInspectionFlag())
                || Boolean.TRUE.equals(source.getCoaInspectionFlag()));
        target.setFirstInspectionSampleFlag(Boolean.TRUE.equals(target.getFirstInspectionSampleFlag())
                || Boolean.TRUE.equals(source.getFirstInspectionSampleFlag()));
        target.setJudgment(firstNotBlank(target.getJudgment(), source.getJudgment()));
        target.setStatus(firstNotBlank(target.getStatus(), source.getStatus()));
        target.setInspectionTime(latestTime(target.getInspectionTime(), source.getInspectionTime()));
        target.setDefectSummary(firstNotBlank(target.getDefectSummary(), source.getDefectSummary()));
        target.setRemark(firstNotBlank(target.getRemark(), source.getRemark()));
    }

    private void refreshPieceStageFromDetails(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || !PIVOT_PIECE_STAGE_CODES.contains(stage.getStageCode())
                || stage.getPieceDetails() == null || stage.getPieceDetails().isEmpty()) {
            return;
        }
        List<QmsMotherRollGoodStatisticsPieceRespVO> details = stage.getPieceDetails().stream()
                .sorted(Comparator.comparing(QmsMotherRollGoodStatisticsPieceRespVO::getPieceNo,
                        Comparator.nullsLast(String::compareTo)))
                .toList();
        details.forEach(item -> item.setStatus(resolvePieceStatus(item)));
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
        stage.setPieceDetails(details);
        stage.setReportQty(BigDecimal.valueOf(details.size()));
        stage.setDoneQty(BigDecimal.valueOf(doneQty));
        stage.setDefectQty(BigDecimal.valueOf(defectQty));
        stage.setSegmentInputQty(BigDecimal.valueOf(doneQty + defectQty));
        stage.setSegmentDefectQty(BigDecimal.valueOf(defectQty));
        stage.setConfirmedQty(BigDecimal.valueOf(confirmedQty));
        stage.setPendingQty(piecePendingQty(Map.of(stage.getStageCode(), stage), stage.getStageCode()));
        stage.setReportUnit("片");
        stage.setPendingUnit("片");
        stage.setSourceBatchNos(firstNotBlank(joinDistinct(details.stream()
                .map(QmsMotherRollGoodStatisticsPieceRespVO::getSourceBatchNo)
                .toList()), stage.getSourceBatchNos()));
        stage.setOutputBatchNos(firstNotBlank(joinDistinct(details.stream()
                .map(QmsMotherRollGoodStatisticsPieceRespVO::getOutputBatchNo)
                .toList()), stage.getOutputBatchNos()));
        details.stream()
                .map(QmsMotherRollGoodStatisticsPieceRespVO::getLastReportTime)
                .filter(Objects::nonNull)
                .forEach(time -> stage.setLastReportTime(latestTime(stage.getLastReportTime(), time)));
    }

    private void refreshInspectionStageFromDetails(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || stage.getInspectionDetails() == null || stage.getInspectionDetails().isEmpty()) {
            return;
        }
        List<QmsMotherRollGoodStatisticsInspectionRespVO> details = stage.getInspectionDetails().stream()
                .sorted(Comparator
                        .comparing(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionNo,
                                Comparator.nullsLast(String::compareTo)))
                .toList();
        stage.setInspectionDetails(details);
        stage.setGlueBoardInspectionQty(details.stream()
                .filter(item -> isGlueBoardInspection(stage.getStageCode(), item))
                .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stage.setCoaInspectionQty(details.stream()
                .filter(item -> isCoaInspection(stage.getStageCode(), item))
                .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stage.setCoaInspectionNgQty(details.stream()
                .filter(item -> isCoaInspection(stage.getStageCode(), item))
                .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionNgQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stage.setInspectionQty(details.stream()
                .filter(item -> !isGlueBoardInspection(stage.getStageCode(), item))
                .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        stage.setProcessProductionInspectionQty(processLossQty(stage.getStageCode(), stage, details));
        stage.setInspectionNgQty(details.stream()
                .filter(item -> !isGlueBoardInspection(stage.getStageCode(), item))
                .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionNgQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        details.stream()
                .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionTime)
                .filter(Objects::nonNull)
                .forEach(time -> stage.setLastReportTime(latestTime(stage.getLastReportTime(), time)));
    }

    private boolean isGlueBoardInspection(String stageCode, QmsMotherRollGoodStatisticsInspectionRespVO item) {
        return "ADHESIVE2".equals(stageCode)
                && item != null
                && "GLUE_BOARD_FAI".equals(normalizeCode(item.getSourceType()));
    }

    private boolean isCoaInspection(String stageCode, QmsMotherRollGoodStatisticsInspectionRespVO item) {
        return COA_INSPECTION_DEDUCT_STAGE_CODES.contains(stageCode)
                && item != null
                && Boolean.TRUE.equals(item.getCoaInspectionFlag());
    }

    private String aggregateModelSeriesCode(QmsMotherRollGoodStatisticsRespVO row) {
        return modelCodePrefix(firstNotBlank(
                row.getActualModelCode(), row.getModelSeriesCode(), row.getModelCode(),
                row.getModelName(), row.getMotherModelCode(), row.getMotherModelName()));
    }

    private String aggregateSizeSpec(QmsMotherRollGoodStatisticsRespVO row) {
        String rawSize = firstNotBlank(row.getActualSizeSpec(), row.getSizeName(), row.getSizeSpec());
        return firstNotBlank(normalizePivotActualSize(rawSize), rawSize);
    }

    private void applyMotherOutputAndTheoreticalTargets(List<QmsMotherRollGoodStatisticsRespVO> rows,
                                                        Map<String, Integer> motherSegmentCountMap,
                                                        List<QmsYieldTargetConfigDO> enabledTargetConfigs,
                                                        Map<String, TheoreticalOutputBase> theoreticalOutputBaseMap) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, QmsYieldTargetConfigDO> targetConfigMap = enabledTargetConfigs.stream()
                .collect(Collectors.toMap(
                        config -> targetKey(config.getModelCode(), config.getProcessCode(),
                                defaultTargetSegmentCount(config.getModelCode(), config.getSegmentCount())),
                        config -> config,
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, LinkedHashMap<String, BigDecimal>> motherOutputContributions = new LinkedHashMap<>();
        Map<String, LinkedHashMap<String, BigDecimal>> inputQtyContributions = new LinkedHashMap<>();
        Map<String, LinkedHashMap<String, BigDecimal>> lengthQtyContributions = new LinkedHashMap<>();
        Map<String, LinkedHashMap<String, BigDecimal>> finalInspectionOutputContributions = new LinkedHashMap<>();
        Map<String, LinkedHashMap<String, BigDecimal>> processLossContributions = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> ratedLossSegmentContributions = new LinkedHashMap<>();
        for (QmsMotherRollGoodStatisticsRespVO row : rows) {
            String modelSeriesCode = modelCodePrefix(firstNotBlank(
                    row.getActualModelCode(), row.getModelCode(), row.getModelName(), row.getMotherModelCode()));
            row.setModelSeriesCode(modelSeriesCode);
            if (row.getStages() == null || row.getStages().isEmpty()) {
                continue;
            }
            for (String stageCode : PIVOT_STAGE_CODES) {
                QmsMotherRollGoodStatisticsStageRespVO stage = row.getStages().get(stageCode);
                if (stage == null) {
                    continue;
                }
                String targetProcessCode = TARGET_PROCESS_CODE_BY_STAGE.getOrDefault(stageCode, stageCode);
                Integer targetSegmentCount = resolveTargetSegmentCount(modelSeriesCode,
                        motherSegmentCountMap.get(motherSegmentKey(row)));
                stage.setTheoreticalOutputModelCode(modelSeriesCode);
                stage.setTheoreticalOutputProcessCode(targetProcessCode);
                stage.setTheoreticalOutputMergeKey("THEORY|" + motherOutputGroupKey(row, stageCode)
                        + "|" + StrUtil.blankToDefault(modelSeriesCode, "-") + "|" + targetSegmentCount);
                if (GOOD_YIELD_RATE_STAGE_CODES.contains(stageCode)) {
                    stage.setGoodYieldRateMergeKey("GOOD_YIELD|" + motherOutputGroupKey(row, stageCode)
                            + "|" + StrUtil.blankToDefault(modelSeriesCode, "-"));
                }
                boolean formulaModel = isFormulaTheoreticalOutputModel(modelSeriesCode, theoreticalOutputBaseMap);
                QmsYieldTargetConfigDO config = formulaModel ? null
                        : targetConfigMap.get(targetKey(modelSeriesCode, targetProcessCode, targetSegmentCount));
                if (formulaModel) {
                    stage.setTheoreticalOutputMatched(false);
                } else if (config != null) {
                    stage.setTheoreticalOutputQty(config.getTargetQualifiedQty());
                    stage.setTheoreticalOutputUnit(resolveTheoreticalOutputUnit(stageCode, config, stage));
                    stage.setTheoreticalOutputMatched(true);
                } else {
                    stage.setTheoreticalOutputMatched(false);
                }
                if (!MOTHER_OUTPUT_STAGE_CODES.contains(stageCode)) {
                    continue;
                }
                String motherOutputMergeKey = "MOTHER_OUTPUT|" + motherOutputGroupKey(row, stageCode);
                stage.setMotherOutputMergeKey(motherOutputMergeKey);
                stage.setMotherOutputUnit(stage.getReportUnit());
                String contributionKey = firstNotBlank(
                        row.getStageMergeKeys() == null ? null : row.getStageMergeKeys().get(stageCode),
                        row.getSegmentMergeKey(),
                        row.getPivotRowKey());
                motherOutputContributions
                        .computeIfAbsent(motherOutputMergeKey, key -> new LinkedHashMap<>())
                        .putIfAbsent(contributionKey, motherOutputContributionQty(stageCode, stage));
                String inputQtyMergeKey = "INPUT|" + motherOutputGroupKey(row, stageCode);
                stage.setInputQtyMergeKey(inputQtyMergeKey);
                inputQtyContributions
                        .computeIfAbsent(inputQtyMergeKey, key -> new LinkedHashMap<>())
                        .putIfAbsent(contributionKey, stageInputContributionQty(stageCode, stage));
                if ("SLITTING".equals(stageCode)) {
                    lengthQtyContributions
                            .computeIfAbsent("LENGTH|" + motherOutputGroupKey(row, stageCode),
                                    key -> new LinkedHashMap<>())
                            .putIfAbsent(contributionKey, zeroIfNull(stage.getLengthQty()));
                }
                if (RATED_LOSS_GOOD_YIELD_STAGE_CODES.contains(stageCode)) {
                    String processLossMergeKey = "PROCESS_LOSS|" + motherOutputGroupKey(row, stageCode);
                    processLossContributions
                            .computeIfAbsent(processLossMergeKey, key -> new LinkedHashMap<>())
                            .putIfAbsent(contributionKey, zeroIfNull(stage.getProcessProductionInspectionQty()));
                    String ratedLossMergeKey = "RATED_LOSS|" + motherOutputGroupKey(row, stageCode);
                    ratedLossSegmentContributions
                            .computeIfAbsent(ratedLossMergeKey, key -> new LinkedHashSet<>())
                            .addAll(ratedLossSegmentKeys(row, stage));
                }
                if ("CUT_ROUND".equals(stageCode)) {
                    String finalInspectionOutputMergeKey = "FINAL_INSPECTION_OUTPUT|" + motherOutputGroupKey(row, stageCode);
                    stage.setFinalInspectionOutputMergeKey(finalInspectionOutputMergeKey);
                    stage.setFinalInspectionOutputUnit("片");
                    stage.setFinalInspectionYieldRateMergeKey("FINAL_INSPECTION_YIELD|"
                            + motherOutputGroupKey(row, stageCode));
                    stage.setGoodTargetRateMergeKey("GOOD_TARGET|" + motherOutputGroupKey(row, stageCode)
                            + "|" + StrUtil.blankToDefault(modelSeriesCode, "-"));
                    finalInspectionOutputContributions
                            .computeIfAbsent(finalInspectionOutputMergeKey, key -> new LinkedHashMap<>())
                            .putIfAbsent(contributionKey, subtractNonNegative(
                                    subtractNonNegative(
                                            zeroIfNull(stage.getInspectionQty()), zeroIfNull(stage.getInspectionNgQty())),
                                    coaInspectionOkQty(stage)));
                }
            }
        }
        Map<String, BigDecimal> motherOutputQtyMap = motherOutputContributions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, BigDecimal> inputQtyMap = inputQtyContributions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, BigDecimal> lengthQtyMap = lengthQtyContributions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, BigDecimal> finalInspectionOutputQtyMap = finalInspectionOutputContributions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, BigDecimal> processLossQtyMap = processLossContributions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        Map<String, BigDecimal> ratedLossQtyMap = ratedLossSegmentContributions.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> BigDecimal.valueOf(Math.max(1, entry.getValue().size())),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        for (QmsMotherRollGoodStatisticsRespVO row : rows) {
            if (row.getStages() == null) {
                continue;
            }
            for (String stageCode : MOTHER_OUTPUT_STAGE_CODES) {
                QmsMotherRollGoodStatisticsStageRespVO stage = row.getStages().get(stageCode);
                if (stage == null || StrUtil.isBlank(stage.getMotherOutputMergeKey())) {
                    continue;
                }
                stage.setMotherOutputQty(motherOutputQtyMap.get(stage.getMotherOutputMergeKey()));
                if (PIECE_INPUT_YIELD_STAGE_CODES.contains(stageCode)
                        && StrUtil.isNotBlank(stage.getInputQtyMergeKey())) {
                    stage.setInputQty(inputQtyMap.get(stage.getInputQtyMergeKey()));
                }
                if ("SLITTING".equals(stageCode)) {
                    stage.setLengthQty(lengthQtyMap.get("LENGTH|" + motherOutputGroupKey(row, stageCode)));
                }
                if ("CUT_ROUND".equals(stageCode) && StrUtil.isNotBlank(stage.getFinalInspectionOutputMergeKey())) {
                    stage.setFinalInspectionOutputQty(
                            finalInspectionOutputQtyMap.get(stage.getFinalInspectionOutputMergeKey()));
                    stage.setFinalInspectionYieldRate(calculatePercentOrNull(
                            stage.getFinalInspectionOutputQty(), stage.getMotherOutputQty()));
                }
            }
            for (String stageCode : PIVOT_STAGE_CODES) {
                QmsMotherRollGoodStatisticsStageRespVO stage = row.getStages().get(stageCode);
                if (stage == null) {
                    continue;
                }
                String modelSeriesCode = modelCodePrefix(firstNotBlank(
                        row.getActualModelCode(), row.getModelCode(), row.getModelName(), row.getMotherModelCode()));
                if (!isFormulaTheoreticalOutputModel(modelSeriesCode, theoreticalOutputBaseMap)) {
                    continue;
                }
                Integer targetSegmentCount = resolveTargetSegmentCount(modelSeriesCode,
                        motherSegmentCountMap.get(motherSegmentKey(row)));
                BigDecimal formulaLengthQty = lengthQtyMap.get("LENGTH|" + motherOutputGroupKey(row, stageCode));
                if ("SLITTING".equals(stageCode)
                        && (formulaLengthQty == null || formulaLengthQty.compareTo(BigDecimal.ZERO) <= 0)) {
                    formulaLengthQty = motherOutputQtyMap.get("MOTHER_OUTPUT|" + motherOutputGroupKey(row, "ADHESIVE1"));
                }
                stage.setTheoreticalOutputMatched(applyFormulaTheoreticalOutput(
                        modelSeriesCode, targetSegmentCount, stageCode, stage, theoreticalOutputBaseMap,
                        inputQtyMap.get("INPUT|" + motherOutputGroupKey(row, stageCode)),
                        formulaLengthQty));
            }
        }
        for (QmsMotherRollGoodStatisticsRespVO row : rows) {
            if (row.getStages() == null) {
                continue;
            }
            for (String stageCode : GOOD_YIELD_RATE_STAGE_CODES) {
                QmsMotherRollGoodStatisticsStageRespVO stage = row.getStages().get(stageCode);
                BigDecimal mergedProcessLossQty = RATED_LOSS_GOOD_YIELD_STAGE_CODES.contains(stageCode)
                        ? processLossQtyMap.get("PROCESS_LOSS|" + motherOutputGroupKey(row, stageCode))
                        : null;
                BigDecimal ratedLossQty = RATED_LOSS_GOOD_YIELD_STAGE_CODES.contains(stageCode)
                        ? ratedLossQtyMap.get("RATED_LOSS|" + motherOutputGroupKey(row, stageCode))
                        : null;
                applyGoodYieldRate(row, modelCodePrefix(firstNotBlank(row.getActualModelCode(), row.getModelCode(),
                        row.getModelName(), row.getMotherModelCode())), stageCode, stage, mergedProcessLossQty,
                        ratedLossQty, theoreticalOutputBaseMap);
            }
            String modelSeriesCode = modelCodePrefix(firstNotBlank(
                    row.getActualModelCode(), row.getModelCode(), row.getModelName(), row.getMotherModelCode()));
            Integer targetSegmentCount = resolveTargetSegmentCount(modelSeriesCode,
                    motherSegmentCountMap.get(motherSegmentKey(row)));
            applyCutRoundGoodTargetRate(row.getStages().get("CUT_ROUND"), modelSeriesCode, targetSegmentCount,
                    theoreticalOutputBaseMap);
        }
    }

    private Map<String, TheoreticalOutputBase> buildTheoreticalOutputBaseMap(
            List<QmsYieldTargetConfigDO> enabledTargetConfigs) {
        Map<String, BigDecimal> formulaQtyMap = new LinkedHashMap<>();
        Map<String, BigDecimal> wetQtyMap = new LinkedHashMap<>();
        for (QmsYieldTargetConfigDO config : enabledTargetConfigs) {
            String modelCode = modelCodePrefix(config.getModelCode());
            String processCode = normalizeCode(config.getProcessCode());
            if (StrUtil.isBlank(modelCode)) {
                continue;
            }
            if ("FORMULA".equals(processCode)) {
                formulaQtyMap.putIfAbsent(modelCode, config.getTargetQualifiedQty());
            } else if ("WET".equals(processCode)) {
                wetQtyMap.putIfAbsent(modelCode, config.getTargetQualifiedQty());
            }
        }
        Map<String, TheoreticalOutputBase> result = new LinkedHashMap<>();
        wetQtyMap.forEach((modelCode, wetQty) ->
                result.put(modelCode, new TheoreticalOutputBase(formulaQtyMap.get(modelCode), wetQty)));
        return result;
    }

    private boolean isFormulaTheoreticalOutputModel(String modelCode,
                                                    Map<String, TheoreticalOutputBase> theoreticalOutputBaseMap) {
        return theoreticalOutputBaseMap.containsKey(modelCodePrefix(modelCode));
    }

    private boolean applyFormulaTheoreticalOutput(String modelCode, Integer segmentCount, String stageCode,
                                                  QmsMotherRollGoodStatisticsStageRespVO stage,
                                                  Map<String, TheoreticalOutputBase> theoreticalOutputBaseMap,
                                                  BigDecimal formulaInputQty,
                                                  BigDecimal formulaLengthQty) {
        TheoreticalOutputBase base = theoreticalOutputBaseMap.get(modelCodePrefix(modelCode));
        if (base == null || stage == null) {
            return false;
        }
        BigDecimal theoreticalOutputQty = formulaTheoreticalOutputQty(
                base, segmentCount, stageCode, stage, formulaInputQty, formulaLengthQty);
        if (theoreticalOutputQty == null || theoreticalOutputQty.compareTo(BigDecimal.ZERO) <= 0) {
            stage.setTheoreticalOutputQty(null);
            stage.setTheoreticalOutputUnit(resolveFormulaTheoreticalOutputUnit(stageCode, stage));
            return false;
        }
        stage.setTheoreticalOutputQty(theoreticalOutputQty);
        stage.setTheoreticalOutputUnit(resolveFormulaTheoreticalOutputUnit(stageCode, stage));
        return true;
    }

    private BigDecimal formulaTheoreticalOutputQty(TheoreticalOutputBase base, Integer segmentCount, String stageCode,
                                                   QmsMotherRollGoodStatisticsStageRespVO stage,
                                                   BigDecimal formulaInputQty,
                                                   BigDecimal formulaLengthQty) {
        if ("FORMULA".equals(stageCode)) {
            return base.formulaQty();
        }
        if ("WET".equals(stageCode)) {
            return base.wetQty();
        }
        if (segmentCount == null || segmentCount < 2 || segmentCount > 4) {
            return null;
        }
        BigDecimal segments = BigDecimal.valueOf(segmentCount);
        return switch (stageCode) {
            case "GRINDING" -> subtractNonNegative(formulaInputQty, BigDecimal.valueOf(3).multiply(segments));
            case "ADHESIVE1" -> subtractNonNegative(formulaInputQty, BigDecimal.valueOf(2).multiply(segments));
            case "SLITTING" -> formulaLengthQty == null || formulaLengthQty.compareTo(BigDecimal.ZERO) <= 0
                    ? null : formulaLengthQty.divide(new BigDecimal("0.85"), 0, RoundingMode.FLOOR);
            case "PRESS_SLOT", "CUT_ROUND" -> firstNonNull(formulaInputQty, stage.getInputQty());
            case "ADHESIVE2" -> subtractNonNegative(firstNonNull(formulaInputQty, stage.getInputQty()), segments);
            case "SHIPPING_INSPECTION" -> firstNonNull(formulaInputQty, stage.getInputQty(), stage.getInspectionQty());
            default -> null;
        };
    }

    private String resolveFormulaTheoreticalOutputUnit(String stageCode,
                                                       QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (PIECE_THEORETICAL_OUTPUT_STAGE_CODES.contains(stageCode) || "SHIPPING_INSPECTION".equals(stageCode)) {
            return "片";
        }
        return firstNotBlank(stage == null ? null : stage.getReportUnit(), "m");
    }

    private BigDecimal motherOutputContributionQty(String stageCode, QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null) {
            return BigDecimal.ZERO;
        }
        if (!PIVOT_PIECE_STAGE_CODES.contains(stageCode)) {
            return zeroIfNull(stage.getDoneQty());
        }
        return subtractNonNegative(stage.getDoneQty(),
                BigDecimal.valueOf(outputInspectionDeductPieceCount(stageCode, stage)));
    }

    private BigDecimal stageInputContributionQty(String stageCode, QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null) {
            return BigDecimal.ZERO;
        }
        if (PIECE_INPUT_YIELD_STAGE_CODES.contains(stageCode)) {
            return pieceInputContributionQty(stageCode, stage);
        }
        return zeroIfNull(stage.getInputQty());
    }

    private long inspectionPieceCount(QmsMotherRollGoodStatisticsStageRespVO stage) {
        return matchedInspectionPieceCount(stage, false, true);
    }

    private long outputInspectionDeductPieceCount(String stageCode, QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (COA_INSPECTION_DEDUCT_STAGE_CODES.contains(stageCode)) {
            return matchedInspectionPieceCount(stage, false, false);
        }
        return inspectionPieceCount(stage);
    }

    private BigDecimal pieceInputContributionQty(String stageCode, QmsMotherRollGoodStatisticsStageRespVO stage) {
        BigDecimal inputQty = zeroIfNull(stage.getDoneQty()).add(zeroIfNull(stage.getDefectQty()));
        if (!COA_INSPECTION_DEDUCT_STAGE_CODES.contains(stageCode)) {
            return inputQty;
        }
        return subtractNonNegative(inputQty, BigDecimal.valueOf(inputInspectionDeductPieceCount(stageCode, stage)));
    }

    private long inputInspectionDeductPieceCount(String stageCode,
                                                 QmsMotherRollGoodStatisticsStageRespVO stage) {
        if ("ADHESIVE2".equals(stageCode)) {
            return matchedNonCoaInspectionPieceCount(stage, false);
        }
        return matchedInspectionPieceCount(stage, false, false);
    }

    private BigDecimal coaInspectionOkQty(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null) {
            return BigDecimal.ZERO;
        }
        return subtractNonNegative(stage.getCoaInspectionQty(), stage.getCoaInspectionNgQty());
    }

    private long matchedInspectionPieceCount(QmsMotherRollGoodStatisticsStageRespVO stage,
                                            boolean coaOnly,
                                            boolean confirmedGoodOnly) {
        if (stage == null || stage.getInspectionDetails() == null || stage.getInspectionDetails().isEmpty()) {
            return 0;
        }
        Set<String> pieceKeys = inspectionDeductPieceKeys(stage, confirmedGoodOnly);
        return stage.getInspectionDetails().stream()
                .filter(item -> !coaOnly || isCoaInspection(stage.getStageCode(), item))
                .flatMap(this::inspectionPieceKeys)
                .filter(pieceKeys::contains)
                .distinct()
                .count();
    }

    private long matchedNonCoaInspectionPieceCount(QmsMotherRollGoodStatisticsStageRespVO stage,
                                                   boolean confirmedGoodOnly) {
        if (stage == null || stage.getInspectionDetails() == null || stage.getInspectionDetails().isEmpty()) {
            return 0;
        }
        Set<String> pieceKeys = inspectionDeductPieceKeys(stage, confirmedGoodOnly);
        return stage.getInspectionDetails().stream()
                .filter(item -> !isCoaInspection(stage.getStageCode(), item))
                .flatMap(this::inspectionPieceKeys)
                .filter(pieceKeys::contains)
                .distinct()
                .count();
    }

    private BigDecimal processLossQty(String stageCode,
                                      QmsMotherRollGoodStatisticsStageRespVO stage,
                                      List<QmsMotherRollGoodStatisticsInspectionRespVO> details) {
        if (!Set.of("PRESS_SLOT", "ADHESIVE2", "CUT_ROUND").contains(stageCode) || stage == null) {
            return BigDecimal.ZERO;
        }
        Map<String, String> processPieceAliases = new LinkedHashMap<>(processPieceAliasMap(stage));
        if ("PRESS_SLOT".equals(stageCode) && stage.getPieceDetails() != null) {
            Set<String> eligibleKeys = stage.getPieceDetails().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getSourceSlittingOkFlag()))
                    .map(this::aggregatePieceDetailKey).collect(Collectors.toSet());
            processPieceAliases.entrySet().removeIf(entry -> !eligibleKeys.contains(entry.getValue()));
        }
        Set<String> lossPieceKeys = new LinkedHashSet<>(processDefectPieceKeys(stage));
        if ("PRESS_SLOT".equals(stageCode)) {
            Set<String> eligibleKeys = stage.getPieceDetails() == null ? Set.of() : stage.getPieceDetails().stream()
                    .filter(item -> Boolean.TRUE.equals(item.getSourceSlittingOkFlag()))
                    .map(this::aggregatePieceDetailKey).collect(Collectors.toSet());
            lossPieceKeys.retainAll(eligibleKeys);
        }
        if (details != null) {
            for (QmsMotherRollGoodStatisticsInspectionRespVO item : details) {
                if (isGlueBoardInspection(stageCode, item)
                        || ("PRESS_SLOT".equals(stageCode) && !Boolean.TRUE.equals(item.getSourceSlittingOkFlag()))) {
                    continue;
                }
                Set<String> matchedKeys = inspectionPieceKeys(item)
                        .map(processPieceAliases::get)
                        .filter(this::isValidPivotPieceKey)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                lossPieceKeys.addAll(matchedKeys);
                // 首检样片不进入正常报工产出，但已完成的压槽送检仍消耗实际片号。
                // 仅用完整产品片号补充；不能将检验单号或分段批号当成一片。
                if (matchedKeys.isEmpty() && "PRESS_SLOT".equals(stageCode)
                        && Boolean.TRUE.equals(item.getFirstInspectionSampleFlag())
                        && "FAI".equals(normalizeCode(item.getSourceType()))
                        && "COMPLETED".equals(normalizeCode(item.getStatus()))) {
                    String samplePieceKey = normalizePivotPieceKey(item.getProductBatchNo());
                    if (samplePieceKey.matches("[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS][0-9]{3}[A-Z]?")) {
                        lossPieceKeys.add(samplePieceKey);
                    }
                }
            }
        }
        return BigDecimal.valueOf(lossPieceKeys.size());
    }

    private Set<String> processDefectPieceKeys(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || stage.getPieceDetails() == null || stage.getPieceDetails().isEmpty()) {
            return Set.of();
        }
        return stage.getPieceDetails().stream()
                .filter(item -> Boolean.TRUE.equals(item.getDefectFlag()))
                .map(this::aggregatePieceDetailKey)
                .filter(this::isValidPivotPieceKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, String> processPieceAliasMap(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage == null || stage.getPieceDetails() == null || stage.getPieceDetails().isEmpty()) {
            return Map.of();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (QmsMotherRollGoodStatisticsPieceRespVO item : stage.getPieceDetails()) {
            String pieceKey = aggregatePieceDetailKey(item);
            if (!isValidPivotPieceKey(pieceKey)) {
                continue;
            }
            result.putIfAbsent(pieceKey, pieceKey);
            java.util.stream.Stream.of(item.getPieceNo(), item.getOutputBatchNo(), item.getSourceBatchNo())
                    .map(this::normalizePivotPieceKey)
                    .filter(this::isValidPivotPieceKey)
                    .forEach(alias -> result.putIfAbsent(alias, pieceKey));
        }
        return result;
    }

    private Set<String> ratedLossSegmentKeys(QmsMotherRollGoodStatisticsRespVO row,
                                             QmsMotherRollGoodStatisticsStageRespVO stage) {
        Set<String> result = new LinkedHashSet<>();
        addRatedLossSegmentKey(result, row == null ? null : row.getSegmentBatchNo());
        if (stage != null && stage.getPieceDetails() != null) {
            for (QmsMotherRollGoodStatisticsPieceRespVO item : stage.getPieceDetails()) {
                addRatedLossSegmentKey(result, item.getSourceBatchNo());
                addRatedLossSegmentKey(result, item.getOutputBatchNo());
                addRatedLossSegmentKey(result, item.getPieceNo());
            }
        }
        if (result.isEmpty()) {
            result.add("FALLBACK");
        }
        return result;
    }

    private void addRatedLossSegmentKey(Set<String> target, String value) {
        String segmentBatchNo = normalizeGrindingSegmentBatchNo(value);
        if (MOTHER_SEGMENT_BATCH_NO_PATTERN.matcher(segmentBatchNo).matches()) {
            target.add(segmentBatchNo);
        }
    }

    private boolean isValidPivotPieceKey(String value) {
        return StrUtil.isNotBlank(value) && !"-".equals(value);
    }

    private Set<String> inspectionDeductPieceKeys(QmsMotherRollGoodStatisticsStageRespVO stage,
                                                  boolean confirmedGoodOnly) {
        if (stage == null || stage.getPieceDetails() == null || stage.getPieceDetails().isEmpty()) {
            return Set.of();
        }
        return stage.getPieceDetails().stream()
                .filter(item -> !confirmedGoodOnly || Boolean.TRUE.equals(item.getReportConfirmed()))
                .filter(item -> !confirmedGoodOnly || !Boolean.TRUE.equals(item.getDefectFlag()))
                .flatMap(item -> java.util.stream.Stream.of(
                        item.getPieceNo(), item.getOutputBatchNo(), item.getSourceBatchNo()))
                .map(this::normalizePivotPieceKey)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private java.util.stream.Stream<String> inspectionPieceKeys(QmsMotherRollGoodStatisticsInspectionRespVO item) {
        if (item == null) {
            return java.util.stream.Stream.empty();
        }
        return java.util.stream.Stream.of(item.getProductBatchNo(), item.getInspectionNo())
                .map(this::normalizePivotPieceKey)
                .filter(StrUtil::isNotBlank);
    }

    private void applyGoodYieldRate(QmsMotherRollGoodStatisticsRespVO row, String modelSeriesCode, String stageCode,
                                    QmsMotherRollGoodStatisticsStageRespVO stage,
                                    BigDecimal mergedProcessLossQty,
                                    BigDecimal ratedLossQty,
                                    Map<String, TheoreticalOutputBase> theoreticalOutputBaseMap) {
        if (stage != null && isFormulaTheoreticalOutputModel(modelSeriesCode, theoreticalOutputBaseMap)) {
            applyFormulaGoodYieldRate(row, stageCode, stage);
            return;
        }
        if (stage != null && RATED_LOSS_GOOD_YIELD_STAGE_CODES.contains(stageCode)) {
            applyRatedLossGoodYieldRate(stage, mergedProcessLossQty, ratedLossQty);
            return;
        }
        if (stage != null && PIECE_INPUT_YIELD_STAGE_CODES.contains(stageCode)) {
            applyPieceInputGoodYieldRate(stage);
            return;
        }
        if (stage == null || !Boolean.TRUE.equals(stage.getTheoreticalOutputMatched())
                || stage.getTheoreticalOutputQty() == null
                || stage.getTheoreticalOutputQty().compareTo(BigDecimal.ZERO) <= 0) {
            if (stage != null) {
                stage.setGoodYieldRate(null);
            }
            return;
        }
        stage.setGoodYieldRate(calculateGoodYieldRatePercent(
                goodYieldRateNumerator(row, stageCode, stage), stage.getTheoreticalOutputQty()));
    }

    private void applyFormulaGoodYieldRate(QmsMotherRollGoodStatisticsRespVO row, String stageCode,
                                           QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (!Boolean.TRUE.equals(stage.getTheoreticalOutputMatched())
                || stage.getTheoreticalOutputQty() == null
                || stage.getTheoreticalOutputQty().compareTo(BigDecimal.ZERO) <= 0) {
            stage.setGoodYieldRate(null);
            return;
        }
        stage.setGoodYieldRate(calculateGoodYieldRatePercent(
                goodYieldRateNumerator(row, stageCode, stage), stage.getTheoreticalOutputQty()));
    }

    private void applyPieceInputGoodYieldRate(QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (stage.getInputQty() == null || stage.getInputQty().compareTo(BigDecimal.ZERO) <= 0) {
            stage.setGoodYieldRate(null);
            return;
        }
        stage.setGoodYieldRate(calculateGoodYieldRatePercent(stage.getMotherOutputQty(), stage.getInputQty()));
    }

    private void applyRatedLossGoodYieldRate(QmsMotherRollGoodStatisticsStageRespVO stage,
                                             BigDecimal mergedProcessLossQty,
                                             BigDecimal ratedLossQty) {
        if (stage.getInputQty() == null || stage.getInputQty().compareTo(BigDecimal.ZERO) <= 0) {
            stage.setGoodYieldRate(null);
            return;
        }
        BigDecimal denominator = stage.getInputQty().subtract(firstNonNull(ratedLossQty, BigDecimal.ONE));
        if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
            stage.setGoodYieldRate(null);
            return;
        }
        BigDecimal numerator = stage.getInputQty().subtract(zeroIfNull(mergedProcessLossQty));
        stage.setGoodYieldRate(calculateGoodYieldRatePercent(numerator, denominator));
    }

    private void applyCutRoundGoodTargetRate(QmsMotherRollGoodStatisticsStageRespVO stage,
                                             String modelCode,
                                             Integer segmentCount,
                                             Map<String, TheoreticalOutputBase> theoreticalOutputBaseMap) {
        TheoreticalOutputBase base = theoreticalOutputBaseMap.get(modelCodePrefix(modelCode));
        BigDecimal finalTheoreticalOutputQty = formulaFinalTheoreticalOutputQty(base, segmentCount);
        if (stage == null || finalTheoreticalOutputQty == null
                || finalTheoreticalOutputQty.compareTo(BigDecimal.ZERO) <= 0) {
            if (stage != null) {
                stage.setGoodTargetRate(null);
            }
            return;
        }
        stage.setGoodTargetRate(calculateGoodYieldRatePercent(
                stage.getFinalInspectionOutputQty(), finalTheoreticalOutputQty));
    }

    private BigDecimal goodYieldRateNumerator(QmsMotherRollGoodStatisticsRespVO row, String stageCode,
                                              QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (MOTHER_OUTPUT_STAGE_CODES.contains(stageCode)) {
            return stage.getMotherOutputQty();
        }
        return stage.getDoneQty();
    }

    private BigDecimal formulaFinalTheoreticalOutputQty(TheoreticalOutputBase base, Integer segmentCount) {
        if (base == null || base.wetQty() == null || segmentCount == null || segmentCount < 2 || segmentCount > 4) {
            return null;
        }
        BigDecimal segments = BigDecimal.valueOf(segmentCount);
        BigDecimal adhesive1 = base.wetQty()
                .subtract(BigDecimal.valueOf(3).multiply(segments))
                .subtract(BigDecimal.valueOf(2).multiply(segments));
        if (adhesive1.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return adhesive1.divide(new BigDecimal("0.85"), 0, RoundingMode.FLOOR).subtract(segments);
    }

    private BigDecimal calculateGoodYieldRatePercent(BigDecimal numerator, BigDecimal denominator) {
        BigDecimal rate = zeroIfNull(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
        return rate.compareTo(BigDecimal.valueOf(100)) > 0 ? BigDecimal.valueOf(100) : rate;
    }

    private BigDecimal calculatePercentOrNull(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return zeroIfNull(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(denominator, 2, RoundingMode.HALF_UP);
    }

    private boolean hasAvailableTargetConfig(QmsYieldTargetConfigDO config) {
        return config != null
                && Objects.equals(config.getStatus(), 0)
                && config.getTargetQualifiedQty() != null
                && config.getTargetQualifiedQty().compareTo(BigDecimal.ZERO) > 0;
    }

    private String resolveTheoreticalOutputUnit(String stageCode, QmsYieldTargetConfigDO config,
                                                QmsMotherRollGoodStatisticsStageRespVO stage) {
        if (PIECE_THEORETICAL_OUTPUT_STAGE_CODES.contains(stageCode)) {
            return "片";
        }
        return firstNotBlank(config.getMeasureUnit(), stage.getReportUnit());
    }

    private String targetKey(String modelCode, String processCode, Integer segmentCount) {
        return modelCodePrefix(modelCode) + "|" + normalizeCode(processCode) + "|"
                + defaultTargetSegmentCount(modelCode, segmentCount);
    }

    private Map<String, Integer> buildMotherSegmentCountMap(List<QmsMotherRollGoodStatisticsRespVO> rows,
                                                            Map<String, TheoreticalOutputBase> theoreticalOutputBaseMap) {
        Map<String, Set<String>> segmentMarkMap = new LinkedHashMap<>();
        for (QmsMotherRollGoodStatisticsRespVO row : rows) {
            String modelSeriesCode = modelCodePrefix(firstNotBlank(
                    row.getActualModelCode(), row.getModelCode(), row.getModelName(), row.getMotherModelCode()));
            if (!isFormulaTheoreticalOutputModel(modelSeriesCode, theoreticalOutputBaseMap)) {
                continue;
            }
            String mark = extractSegmentMark(row.getSegmentBatchNo());
            if (StrUtil.isBlank(mark)) {
                continue;
            }
            segmentMarkMap.computeIfAbsent(motherSegmentKey(row), ignored -> new LinkedHashSet<>()).add(mark);
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        segmentMarkMap.forEach((key, marks) -> {
            if (marks.size() >= 2 && marks.size() <= 4) {
                result.put(key, marks.size());
            }
        });
        return result;
    }

    private Integer resolveTargetSegmentCount(String modelCode, Integer detectedSegmentCount) {
        return detectedSegmentCount != null && detectedSegmentCount >= 2 && detectedSegmentCount <= 4
                ? detectedSegmentCount : null;
    }

    private Integer defaultTargetSegmentCount(String modelCode, Integer segmentCount) {
        return 0;
    }

    private String motherSegmentKey(QmsMotherRollGoodStatisticsRespVO row) {
        return modelCodePrefix(firstNotBlank(row.getActualModelCode(), row.getModelCode(), row.getModelName(),
                row.getMotherModelCode())) + "|" + normalizeMotherBatchNo(firstNotBlank(
                row.getMotherRollBatchNo(), row.getParentProductionBatchNo(), row.getProductionBatchNo(),
                row.getBatchNo(), row.getSegmentBatchNo()));
    }

    private String extractSegmentMark(String segmentBatchNo) {
        String normalized = normalizeGrindingSegmentBatchNo(segmentBatchNo);
        Matcher matcher = SEGMENT_MARK_PATTERN.matcher(normalized);
        return matcher.matches() ? matcher.group(1).toUpperCase(Locale.ROOT) : "";
    }

    private String modelCodePrefix(String modelCode) {
        String text = normalizeCode(modelCode);
        return text.length() <= 3 ? text : text.substring(0, 3);
    }

    private String normalizeCode(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "").toUpperCase(Locale.ROOT);
    }

    private String normalizeMotherBatchText(String value) {
        String text = StrUtil.trim(value);
        if (StrUtil.isBlank(text)) {
            return null;
        }
        List<String> motherBatchNos = new ArrayList<>();
        for (String part : text.split("[,，;；\\s]+")) {
            String motherBatchNo = normalizeMotherBatchNo(part);
            if (StrUtil.isNotBlank(motherBatchNo) && !motherBatchNos.contains(motherBatchNo)) {
                motherBatchNos.add(motherBatchNo);
            }
        }
        return joinDistinctTexts(motherBatchNos);
    }

    private String normalizeMotherBatchNo(String value) {
        String segmentBatchNo = normalizeGrindingSegmentBatchNo(value);
        if (MOTHER_SEGMENT_BATCH_NO_PATTERN.matcher(segmentBatchNo).matches()) {
            return segmentBatchNo.substring(0, segmentBatchNo.length() - 1);
        }
        return segmentBatchNo;
    }

    private String normalizeGrindingSegmentBatchNo(String value) {
        String text = normalizeCode(value);
        if (StrUtil.isBlank(text)) {
            return "";
        }
        String batchNo = text.replaceFirst("-J[0-9]+$", "");
        Matcher matcher = PIECE_BATCH_NO_PATTERN.matcher(batchNo);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return batchNo;
    }

    private String motherOutputGroupKey(QmsMotherRollGoodStatisticsRespVO row, String stageCode) {
        return firstNotBlank(row.getPlanMergeKey(), row.getPlanNo(), String.valueOf(row.getId()), "-")
                + "|" + motherBatchKey(row)
                + "|" + stageCode;
    }

    private String motherBatchKey(QmsMotherRollGoodStatisticsRespVO row) {
        return firstNotBlank(normalizeMotherBatchText(firstNotBlank(
                row.getMotherRollBatchNo(),
                row.getParentProductionBatchNo(),
                row.getProductionBatchNo(),
                row.getBatchNo())), "");
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
        return postProcessLocks.stream()
                .filter(lock -> lock.getPlanId() != null)
                .collect(Collectors.groupingBy(HcPlanOrderInventoryLockDO::getPlanId, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        entry -> buildPostProcessSourceInfo(entry.getValue(), stockMap, sourcePlanMap),
                        (a, b) -> a, LinkedHashMap::new));
    }

    private boolean isPostProcessInventoryLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null) {
            return false;
        }
        String stockType = StrUtil.trim(lock.getStockType());
        if (!StrUtil.equalsIgnoreCase(STOCK_TYPE_WIP, stockType)) {
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

    private PostProcessSourceInfo buildPostProcessSourceInfo(List<HcPlanOrderInventoryLockDO> locks,
                                                             Map<Long, HcInvStockDO> stockMap,
                                                             Map<Long, HcPlanOrderDO> sourcePlanMap) {
        Set<String> motherBatchNos = new LinkedHashSet<>();
        Set<String> grindingSegmentBatchNos = new LinkedHashSet<>();
        for (HcPlanOrderInventoryLockDO lock : locks) {
            HcInvStockDO stock = lock.getStockId() == null ? null : stockMap.get(lock.getStockId());
            HcPlanOrderDO sourcePlan = lock.getSourcePlanId() == null ? null : sourcePlanMap.get(lock.getSourcePlanId());
            String motherBatchNo = firstNotBlank(
                    stock == null ? null : stock.getSourceParentBatchNo(),
                    sourcePlan == null ? null : sourcePlan.getParentProductionBatchNo(),
                    sourcePlan == null ? null : sourcePlan.getProductionBatchNo(),
                    sourcePlan == null ? null : sourcePlan.getBatchNo());
            addDistinctText(motherBatchNos, normalizeMotherBatchText(motherBatchNo));
            String grindingSegmentBatchNo = firstNotBlank(
                    normalizeGrindingSegmentBatchNo(stock == null ? null : stock.getSourceParentBatchNo()),
                    normalizeGrindingSegmentBatchNo(lock.getBatchNo()),
                    normalizeGrindingSegmentBatchNo(lock.getLotNo()),
                    normalizeGrindingSegmentBatchNo(stock == null ? null : stock.getBatchNo()),
                    normalizeGrindingSegmentBatchNo(stock == null ? null : stock.getSourceBatchNo()),
                    normalizeGrindingSegmentBatchNo(lock.getSourceBatchNo()));
            addDistinctText(grindingSegmentBatchNos, grindingSegmentBatchNo);
            addDistinctText(motherBatchNos, normalizeMotherBatchText(grindingSegmentBatchNo));
        }
        return new PostProcessSourceInfo(joinDistinctTexts(motherBatchNos), joinDistinctTexts(grindingSegmentBatchNos));
    }

    private void fillPostProcessPlanInfo(List<QmsMotherRollGoodStatisticsRespVO> rows, PostProcessSourceInfo sourceInfo) {
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
            String sourceMotherBatchNo = firstNotBlank(sourceInfo.motherBatchNo(),
                    normalizeMotherBatchText(sourceInfo.grindingSegmentBatchNo()));
            if (StrUtil.isBlank(row.getMotherRollBatchNo())) {
                row.setMotherRollBatchNo(sourceMotherBatchNo);
            }
            if (StrUtil.isBlank(row.getSegmentBatchNo())) {
                row.setSegmentBatchNo(firstNotBlank(sourceInfo.grindingSegmentBatchNo(), sourceMotherBatchNo));
            }
            fillPostProcessGrindingStage(row, sourceInfo);
        });
    }

    private void fillPostProcessGrindingStage(QmsMotherRollGoodStatisticsRespVO row, PostProcessSourceInfo sourceInfo) {
        if (row.getStages() == null || sourceInfo == null) {
            return;
        }
        QmsMotherRollGoodStatisticsStageRespVO grindingStage = row.getStages().get("GRINDING");
        if (grindingStage == null) {
            return;
        }
        if (StrUtil.isBlank(grindingStage.getSourceBatchNos())) {
            grindingStage.setSourceBatchNos(sourceInfo.motherBatchNo());
        }
        if (StrUtil.isBlank(grindingStage.getOutputBatchNos())) {
            grindingStage.setOutputBatchNos(sourceInfo.grindingSegmentBatchNo());
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

    private List<QmsMotherRollGoodStatisticsRespVO> buildPlanProcessPivotRespList(
            HcPlanOrderDO plan,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows) {
        boolean hasStageRows = stageRows != null && !stageRows.isEmpty();
        boolean hasPieceRows = pieceRows != null && !pieceRows.isEmpty();
        boolean hasInspectionRows = inspectionRows != null && !inspectionRows.isEmpty();
        if (!hasStageRows && !hasPieceRows && !hasInspectionRows) {
            return List.of(buildPlanProcessPivotResp(plan, null, null, null, null));
        }
        List<HcProcessReportMapper.PlanProcessPivotStageRow> safeStageRows =
                hasStageRows ? stageRows : List.of();
        List<HcProcessReportMapper.PlanProcessPivotPieceRow> safePieceRows =
                hasPieceRows ? pieceRows : List.of();
        List<HcProcessReportMapper.PlanProcessPivotInspectionRow> safeInspectionRows =
                hasInspectionRows ? inspectionRows : List.of();
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
        List<QmsMotherRollGoodStatisticsRespVO> result = new ArrayList<>();
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

    private QmsMotherRollGoodStatisticsRespVO buildPlanProcessPivotResp(
            HcPlanOrderDO plan,
            String segmentBatchNo,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows) {
        return buildPlanProcessPivotResp(plan, segmentBatchNo, stageRows, pieceRows, inspectionRows,
                PivotDisplayGroup.single(plan, segmentBatchNo));
    }

    private QmsMotherRollGoodStatisticsRespVO buildPlanProcessPivotResp(
            HcPlanOrderDO plan,
            String segmentBatchNo,
            List<HcProcessReportMapper.PlanProcessPivotStageRow> stageRows,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows,
            PivotDisplayGroup displayGroup) {
        QmsMotherRollGoodStatisticsRespVO respVO = BeanUtils.toBean(plan, QmsMotherRollGoodStatisticsRespVO.class);
        respVO.setMotherRollBatchNo(firstNotBlank(plan.getParentProductionBatchNo(),
                plan.getProductionBatchNo(), plan.getBatchNo()));
        respVO.setSegmentBatchNo(firstNotBlank(segmentBatchNo, respVO.getMotherRollBatchNo()));
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
        Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages = initPivotStages();
        if (stageRows != null) {
            stageRows.forEach(row -> mergePivotStage(stages, row));
        }
        fillPivotPieceDetails(stages, pieceRows);
        fillPivotInspectionDetails(stages, inspectionRows);
        fillPivotPendingQty(plan, stages);
        applyGrindingOutputFormula(stages);
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
                .filter(row -> "CUT_ROUND".equals(normalizeCode(row.stageCode())))
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
        if ("CUT_ROUND".equals(normalizeCode(row.stageCode()))) {
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

    private PageResult<QmsMotherRollGoodStatisticsRespVO> paginatePivotRows(
            List<QmsMotherRollGoodStatisticsRespVO> list, QmsMotherRollGoodStatisticsPageReqVO pageReqVO) {
        int pageNo = pageReqVO.getPageNo() == null ? 1 : pageReqVO.getPageNo();
        int pageSize = pageReqVO.getPageSize() == null ? 10 : pageReqVO.getPageSize();
        if (PageParam.PAGE_SIZE_NONE.equals(pageSize)) {
            return new PageResult<>(list, (long) list.size());
        }
        int fromIndex = Math.min((pageNo - 1) * pageSize, list.size());
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        return new PageResult<>(list.subList(fromIndex, toIndex), (long) list.size());
    }

    private Map<String, QmsMotherRollGoodStatisticsStageRespVO> initPivotStages() {
        Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages = new LinkedHashMap<>();
        for (String stageCode : PIVOT_STAGE_CODES) {
            QmsMotherRollGoodStatisticsStageRespVO stage = new QmsMotherRollGoodStatisticsStageRespVO();
            stage.setStageCode(stageCode);
            stage.setStageName(PIVOT_STAGE_NAMES.get(stageCode));
            stage.setStageStatus("NOT_STARTED");
            stage.setInputQty(BigDecimal.ZERO);
            stage.setReportQty(BigDecimal.ZERO);
            stage.setDoneQty(BigDecimal.ZERO);
            stage.setPendingQty(BigDecimal.ZERO);
            stage.setDefectQty(BigDecimal.ZERO);
            stage.setInspectionQty(BigDecimal.ZERO);
            stage.setProcessProductionInspectionQty(BigDecimal.ZERO);
            stage.setCoaInspectionQty(BigDecimal.ZERO);
            stage.setInspectionNgQty(BigDecimal.ZERO);
            stage.setCoaInspectionNgQty(BigDecimal.ZERO);
            stage.setConfirmedQty(BigDecimal.ZERO);
            stage.setLengthQty(BigDecimal.ZERO);
            stage.setReportUnit(resolveDefaultStageUnit(stageCode));
            stage.setPendingUnit(resolveDefaultStageUnit(stageCode));
            stage.setLengthUnit("m");
            stages.put(stageCode, stage);
        }
        return stages;
    }

    private void mergePivotStage(
            Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages,
            HcProcessReportMapper.PlanProcessPivotStageRow row) {
        QmsMotherRollGoodStatisticsStageRespVO stage = stages.get(row.stageCode());
        if (stage == null) {
            return;
        }
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
            Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages,
            List<HcProcessReportMapper.PlanProcessPivotPieceRow> pieceRows) {
        if (pieceRows == null || pieceRows.isEmpty()) {
            return;
        }
        Map<String, String> cutOutputSizeByPieceKey = buildPivotCutOutputSizeByPieceKey(pieceRows);
        Map<String, LinkedHashMap<String, QmsMotherRollGoodStatisticsPieceRespVO>> detailsByStage = new LinkedHashMap<>();
        for (HcProcessReportMapper.PlanProcessPivotPieceRow row : pieceRows) {
            if (!PIVOT_PIECE_STAGE_CODES.contains(row.stageCode()) || StrUtil.isBlank(row.pieceNo())) {
                continue;
            }
            QmsMotherRollGoodStatisticsStageRespVO stage = stages.get(row.stageCode());
            if (stage == null) {
                continue;
            }
            LinkedHashMap<String, QmsMotherRollGoodStatisticsPieceRespVO> stageDetails =
                    detailsByStage.computeIfAbsent(row.stageCode(), key -> new LinkedHashMap<>());
            String pieceKey = row.pieceNo().trim().toUpperCase(Locale.ROOT);
            QmsMotherRollGoodStatisticsPieceRespVO detail = stageDetails.computeIfAbsent(pieceKey, key -> {
                QmsMotherRollGoodStatisticsPieceRespVO item = new QmsMotherRollGoodStatisticsPieceRespVO();
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
            detail.setSourceSlittingOkFlag(!Boolean.FALSE.equals(detail.getSourceSlittingOkFlag())
                    && flag(row.sourceSlittingOkFlag()));
            detail.setCoaFlag(Boolean.TRUE.equals(detail.getCoaFlag()) || flag(row.coaFlag()));
            detail.setLastReportTime(latestTime(detail.getLastReportTime(), row.lastReportTime()));
            detail.setRemark(firstNotBlank(detail.getRemark(), row.remark()));
        }
        detailsByStage.forEach((stageCode, detailMap) -> {
            QmsMotherRollGoodStatisticsStageRespVO stage = stages.get(stageCode);
            if (stage == null) {
                return;
            }
            List<QmsMotherRollGoodStatisticsPieceRespVO> details = detailMap.values().stream()
                    .sorted(Comparator.comparing(QmsMotherRollGoodStatisticsPieceRespVO::getPieceNo,
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
            stage.setSegmentInputQty(BigDecimal.valueOf(doneQty + defectQty));
            stage.setSegmentDefectQty(BigDecimal.valueOf(defectQty));
            stage.setConfirmedQty(BigDecimal.valueOf(confirmedQty));
            stage.setReportUnit("片");
            stage.setPendingUnit("片");
            stage.setSourceBatchNos(firstNotBlank(joinDistinct(details.stream()
                    .map(QmsMotherRollGoodStatisticsPieceRespVO::getSourceBatchNo)
                    .toList()), stage.getSourceBatchNos()));
            stage.setOutputBatchNos(firstNotBlank(joinDistinct(details.stream()
                    .map(QmsMotherRollGoodStatisticsPieceRespVO::getOutputBatchNo)
                    .toList()), stage.getOutputBatchNos()));
            details.stream()
                    .map(QmsMotherRollGoodStatisticsPieceRespVO::getLastReportTime)
                    .filter(Objects::nonNull)
                    .forEach(time -> stage.setLastReportTime(latestTime(stage.getLastReportTime(), time)));
        });
    }

    private void fillPivotInspectionDetails(
            Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages,
            List<HcProcessReportMapper.PlanProcessPivotInspectionRow> inspectionRows) {
        if (inspectionRows == null || inspectionRows.isEmpty()) {
            return;
        }
        Map<String, LinkedHashMap<String, QmsMotherRollGoodStatisticsInspectionRespVO>> detailsByStage = new LinkedHashMap<>();
        for (HcProcessReportMapper.PlanProcessPivotInspectionRow row : inspectionRows) {
            if (stages.get(row.stageCode()) == null || row.inspectionId() == null || StrUtil.isBlank(row.sourceType())) {
                continue;
            }
            appendPivotInspectionDetail(detailsByStage, row, row.stageCode());
        }
        detailsByStage.forEach((stageCode, detailMap) -> {
            QmsMotherRollGoodStatisticsStageRespVO stage = stages.get(stageCode);
            if (stage == null) {
                return;
            }
            List<QmsMotherRollGoodStatisticsInspectionRespVO> details = detailMap.values().stream()
                    .sorted(Comparator
                            .comparing(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionTime,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                            .thenComparing(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionNo,
                                    Comparator.nullsLast(String::compareTo)))
                    .toList();
            stage.setInspectionDetails(details);
            stage.setGlueBoardInspectionQty(details.stream()
                    .filter(item -> isGlueBoardInspection(stageCode, item))
                    .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stage.setCoaInspectionQty(details.stream()
                    .filter(item -> isCoaInspection(stageCode, item))
                    .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stage.setCoaInspectionNgQty(details.stream()
                    .filter(item -> isCoaInspection(stageCode, item))
                    .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionNgQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stage.setInspectionQty(details.stream()
                    .filter(item -> !isGlueBoardInspection(stageCode, item))
                    .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            stage.setProcessProductionInspectionQty(processLossQty(stageCode, stage, details));
            stage.setInspectionNgQty(details.stream()
                    .filter(item -> !isGlueBoardInspection(stageCode, item))
                    .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionNgQty)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            details.stream()
                    .map(QmsMotherRollGoodStatisticsInspectionRespVO::getInspectionTime)
                    .filter(Objects::nonNull)
                    .forEach(time -> stage.setLastReportTime(latestTime(stage.getLastReportTime(), time)));
        });
    }

    private void appendPivotInspectionDetail(
            Map<String, LinkedHashMap<String, QmsMotherRollGoodStatisticsInspectionRespVO>> detailsByStage,
            HcProcessReportMapper.PlanProcessPivotInspectionRow row,
            String stageCode) {
        LinkedHashMap<String, QmsMotherRollGoodStatisticsInspectionRespVO> stageDetails =
                detailsByStage.computeIfAbsent(stageCode, key -> new LinkedHashMap<>());
        String detailKey = row.sourceType() + "|" + row.inspectionId();
        boolean existingDetail = stageDetails.containsKey(detailKey);
        QmsMotherRollGoodStatisticsInspectionRespVO detail = stageDetails.computeIfAbsent(detailKey, key -> {
            QmsMotherRollGoodStatisticsInspectionRespVO item = new QmsMotherRollGoodStatisticsInspectionRespVO();
            item.setStageCode(stageCode);
            item.setSourceType(row.sourceType());
            item.setInspectionId(row.inspectionId());
            item.setInspectionNo(row.inspectionNo());
            item.setInspectionType(row.inspectionType());
            item.setProductBatchNo(row.productBatchNo());
            item.setInspectionQty(BigDecimal.ZERO);
            item.setInspectionNgQty(BigDecimal.ZERO);
            item.setCoaInspectionFlag(false);
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
        detail.setSourceSlittingOkFlag(!Boolean.FALSE.equals(detail.getSourceSlittingOkFlag())
                && flag(row.sourceSlittingOkFlag()));
        detail.setCoaInspectionFlag(Boolean.TRUE.equals(detail.getCoaInspectionFlag())
                || flag(row.coaInspectionFlag()));
        detail.setFirstInspectionSampleFlag(Boolean.TRUE.equals(detail.getFirstInspectionSampleFlag())
                || flag(row.firstInspectionSampleFlag()));
        detail.setProductBatchNo(firstNotBlank(detail.getProductBatchNo(), row.productBatchNo()));
        detail.setInspectionNo(firstNotBlank(detail.getInspectionNo(), row.inspectionNo()));
        detail.setInspectionType(firstNotBlank(detail.getInspectionType(), row.inspectionType()));
        detail.setJudgment(firstNotBlank(detail.getJudgment(), row.judgment()));
        detail.setStatus(firstNotBlank(detail.getStatus(), row.status()));
        detail.setInspectionTime(latestTime(detail.getInspectionTime(), row.inspectionTime()));
        detail.setDefectSummary(firstNotBlank(detail.getDefectSummary(), row.defectSummary()));
        detail.setRemark(firstNotBlank(detail.getRemark(), row.remark()));
    }

    private String resolvePieceStatus(QmsMotherRollGoodStatisticsPieceRespVO item) {
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
        String stageCode = normalizeCode(row.stageCode());
        return row.lastReportTime() != null && PIVOT_SUBMITTED_REPORT_STAGE_CODES.contains(stageCode);
    }

    private void fillPivotPendingQty(HcPlanOrderDO plan, Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages) {
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

    private void applyGrindingOutputFormula(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages) {
        QmsMotherRollGoodStatisticsStageRespVO stage = stages.get("GRINDING");
        if (stage == null) {
            return;
        }
        BigDecimal outputQty = subtractNonNegative(
                subtractNonNegative(stage.getInputQty(), stage.getDefectQty()),
                stage.getInspectionQty());
        stage.setDoneQty(outputQty);
        stage.setLengthQty(outputQty);
    }

    private void fillPivotSummary(QmsMotherRollGoodStatisticsRespVO respVO, Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages) {
        BigDecimal totalDefectQty = BigDecimal.ZERO;
        LocalDateTime latestReportTime = null;
        for (QmsMotherRollGoodStatisticsStageRespVO stage : stages.values()) {
            totalDefectQty = totalDefectQty.add(zeroIfNull(stage.getDefectQty()));
            if (stage.getLastReportTime() != null
                    && (latestReportTime == null || stage.getLastReportTime().isAfter(latestReportTime))) {
                latestReportTime = stage.getLastReportTime();
            }
        }
        respVO.setTotalDefectQty(totalDefectQty);
        respVO.setLatestReportTime(latestReportTime);
    }

    private void refreshStageStatus(QmsMotherRollGoodStatisticsStageRespVO stage) {
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

    private void setPending(QmsMotherRollGoodStatisticsStageRespVO stage, BigDecimal pendingQty, String pendingUnit) {
        if (stage == null) {
            return;
        }
        stage.setPendingQty(zeroIfNull(pendingQty));
        stage.setPendingUnit(pendingUnit);
    }

    private BigDecimal inputQty(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getInputQty());
    }

    private BigDecimal reportQty(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getReportQty());
    }

    private BigDecimal doneQty(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getDoneQty());
    }

    private BigDecimal confirmedQty(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getConfirmedQty());
    }

    private BigDecimal defectQty(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getDefectQty());
    }

    private BigDecimal piecePendingQty(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String stageCode) {
        return subtractNonNegative(subtractNonNegative(reportQty(stages, stageCode), doneQty(stages, stageCode)),
                defectQty(stages, stageCode));
    }

    private BigDecimal lengthQty(Map<String, QmsMotherRollGoodStatisticsStageRespVO> stages, String stageCode) {
        return zeroIfNull(stages.get(stageCode).getLengthQty());
    }

    private BigDecimal subtractNonNegative(BigDecimal minuend, BigDecimal subtrahend) {
        BigDecimal value = zeroIfNull(minuend).subtract(zeroIfNull(subtrahend));
        return value.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : value;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
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

    private String mergeDistinctText(String first, String second) {
        List<String> values = new ArrayList<>();
        values.add(first);
        values.add(second);
        return joinDistinct(values);
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

    private record TheoreticalOutputBase(BigDecimal formulaQty, BigDecimal wetQty) {
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


    private record PostProcessSourceInfo(String motherBatchNo, String grindingSegmentBatchNo) {
    }

}
