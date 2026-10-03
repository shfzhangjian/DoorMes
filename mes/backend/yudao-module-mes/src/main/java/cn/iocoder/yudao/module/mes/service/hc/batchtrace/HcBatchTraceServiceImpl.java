package cn.iocoder.yudao.module.mes.service.hc.batchtrace;

import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace.HcBatchTraceAuxiliaryDTO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace.HcBatchTraceFactDTO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace.HcBatchTraceMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processparam.HcProcessParamRecordDO;
import cn.iocoder.yudao.module.mes.service.hc.processparam.HcProcessParamRecordService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcBatchTraceServiceImpl implements HcBatchTraceService {

    private static final Pattern LAST_THREE_DIGITS = Pattern.compile("\\d{3}$");
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final Set<String> COMPLETION_REPORT_COLLAPSE_SOURCE_TYPES = Set.of(
            "FORMULA_REPORT", "WET_REPORT"
    );

    private static final List<ProcessDefinition> PROCESS_DEFINITIONS = List.of(
            new ProcessDefinition("FORMULA", "配料", 10, Set.of("FORMULA_REPORT")),
            new ProcessDefinition("WET", "湿法", 20, Set.of("WET_REPORT")),
            new ProcessDefinition("ROUGH_GRINDING_FIRST", "磨皮一磨", 30,
                    Set.of("ROUGH_GRINDING_FIRST", "ROUGH_GRINDING_FIRST_ALLOCATION")),
            new ProcessDefinition("ROUGH_GRINDING_SECOND", "磨皮二磨", 40, Set.of("ROUGH_GRINDING_SECOND")),
            new ProcessDefinition("ADHESIVE1", "粘胶1", 50, Set.of("ADHESIVE_REPORT")),
            new ProcessDefinition("SLITTING", "分切", 60, Set.of("SLITTING_SLICE")),
            new ProcessDefinition("PRESS_SLOT", "压槽", 70, Set.of("PRESS_SLOT_REPORT")),
            new ProcessDefinition("ADHESIVE2", "粘胶2", 80, Set.of("ADHESIVE2_REPORT")),
            new ProcessDefinition("CUT", "裁切", 90, Set.of("CUT_ROUND_REPORT"))
    );

    private static final Map<String, ProcessDefinition> PROCESS_BY_SOURCE_TYPE = PROCESS_DEFINITIONS.stream()
            .flatMap(process -> process.sourceTypes().stream().map(sourceType -> Map.entry(sourceType, process)))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    @Resource
    private HcBatchTraceMapper batchTraceMapper;

    @Resource
    private HcProcessParamRecordService hcProcessParamRecordService;

    @Override
    public HcBatchTraceRespVO getTrace(HcBatchTraceQueryReqVO reqVO) {
        BatchKeys batchKeys = BatchKeys.parse(reqVO.getBatchNo());
        Long tenantId = currentTenantId();
        List<HcBatchTraceFactDTO> facts = batchTraceMapper.selectTraceFacts(batchKeys.exactBatchNos(), batchKeys.branchPrefix, tenantId);
        facts = filterFactsForBatch(batchKeys, facts);
        facts = filterStartFacts(facts);
        facts = collapseCompletionReportFacts(facts);
        List<HcBatchTraceAuxiliaryDTO> auxiliaryRows = selectAuxiliaryRows(facts, tenantId);

        Map<String, List<HcBatchTraceRespVO.AuxiliaryItemRespVO>> auxiliaryByTimelineKey = buildAuxiliaryMap(auxiliaryRows);
        List<HcBatchTraceRespVO.TimelineNodeRespVO> timelineNodes = facts.stream()
                .map(fact -> buildTimelineNode(fact, auxiliaryByTimelineKey.getOrDefault(sourceKey(fact), List.of())))
                .sorted(Comparator.comparingInt(HcBatchTraceRespVO.TimelineNodeRespVO::getProcessOrder)
                        .thenComparing(node -> node.getEventTime() == null ? LocalDateTime.MIN : node.getEventTime()))
                .toList();
        Map<String, List<HcBatchTraceRespVO.ProcessParamItemRespVO>> processParamMap = buildProcessParamMap(facts, tenantId);
        for (HcBatchTraceRespVO.TimelineNodeRespVO node : timelineNodes) {
            node.setProcessParams(processParamMap.getOrDefault(node.getKey(), List.of()));
        }

        HcBatchTraceRespVO respVO = new HcBatchTraceRespVO();
        respVO.setOverview(buildOverview(batchKeys, timelineNodes));
        respVO.setTimelineNodes(timelineNodes);
        respVO.setTreeNodes(buildTree(batchKeys, facts));
        respVO.setQualityEvents(timelineNodes.stream()
                .flatMap(node -> node.getQualityItems().stream())
                .toList());
        respVO.setAuxiliaryEvents(timelineNodes.stream()
                .flatMap(node -> node.getAuxiliaryItems().stream())
                .toList());
        respVO.setProcessParamEvents(timelineNodes.stream()
                .flatMap(node -> node.getProcessParams().stream())
                .toList());
        return respVO;
    }

    private List<HcBatchTraceAuxiliaryDTO> selectAuxiliaryRows(List<HcBatchTraceFactDTO> facts, Long tenantId) {
        List<String> sourceKeys = facts.stream()
                .map(this::sourceKey)
                .filter(this::isNotBlank)
                .distinct()
                .toList();
        if (sourceKeys.isEmpty()) {
            return List.of();
        }
        return batchTraceMapper.selectAuxiliaryBySourceKeys(sourceKeys, tenantId);
    }

    private Map<String, List<HcBatchTraceRespVO.AuxiliaryItemRespVO>> buildAuxiliaryMap(List<HcBatchTraceAuxiliaryDTO> rows) {
        Map<String, List<HcBatchTraceRespVO.AuxiliaryItemRespVO>> result = new LinkedHashMap<>();
        for (HcBatchTraceAuxiliaryDTO row : rows) {
            HcBatchTraceRespVO.AuxiliaryItemRespVO item = buildAuxiliaryItem(row);
            if (isEmptyAuxiliary(item)) {
                continue;
            }
            String timelineKey = sourceKey(row.getSourceType(), row.getSourceId());
            item.setTimelineKey(timelineKey);
            result.computeIfAbsent(timelineKey, key -> new ArrayList<>()).add(item);
        }
        return result;
    }

    private Map<String, List<HcBatchTraceRespVO.ProcessParamItemRespVO>> buildProcessParamMap(List<HcBatchTraceFactDTO> facts,
                                                                                              Long tenantId) {
        Set<String> batchNos = new LinkedHashSet<>();
        Set<String> processCodes = new LinkedHashSet<>();
        for (HcBatchTraceFactDTO fact : facts) {
            collectProcessParamBatchNos(fact, batchNos);
            collectProcessParamCodes(fact, processCodes);
        }
        if (batchNos.isEmpty() || processCodes.isEmpty()) {
            return Map.of();
        }
        List<HcProcessParamRecordDO> records = hcProcessParamRecordService.listByBatchNosAndProcessCodes(batchNos, processCodes, null);
        Map<String, List<HcProcessParamRecordDO>> recordsByKey = new LinkedHashMap<>();
        for (HcProcessParamRecordDO record : records) {
            if (tenantId != null && record.getTenantId() != null && !Objects.equals(tenantId, record.getTenantId())) {
                continue;
            }
            recordsByKey.computeIfAbsent(processParamLookupKey(record.getTenantId(), record.getBatchNo(), record.getProcessCode()),
                    key -> new ArrayList<>()).add(record);
        }
        Map<String, List<HcBatchTraceRespVO.ProcessParamItemRespVO>> result = new LinkedHashMap<>();
        for (HcBatchTraceFactDTO fact : facts) {
            LinkedHashMap<String, HcBatchTraceRespVO.ProcessParamItemRespVO> items = new LinkedHashMap<>();
            for (String batchNo : processParamCandidateBatchNos(fact)) {
                for (String processCode : processParamCandidateCodes(fact)) {
                    List<HcProcessParamRecordDO> matched = recordsByKey.get(processParamLookupKey(tenantId, batchNo, processCode));
                    if (matched == null || matched.isEmpty()) {
                        continue;
                    }
                    for (HcProcessParamRecordDO record : matched) {
                        String itemKey = firstNotBlank(record.getParamCode(), record.getParamName());
                        if (isNotBlank(itemKey)) {
                            items.putIfAbsent(itemKey, buildProcessParamItem(sourceKey(fact), record));
                        }
                    }
                }
            }
            if (!items.isEmpty()) {
                result.put(sourceKey(fact), new ArrayList<>(items.values()));
            }
        }
        return result;
    }

    private void collectProcessParamBatchNos(HcBatchTraceFactDTO fact, Set<String> batchNos) {
        processParamCandidateBatchNos(fact).forEach(batchNos::add);
    }

    private void collectProcessParamCodes(HcBatchTraceFactDTO fact, Set<String> processCodes) {
        processParamCandidateCodes(fact).forEach(processCodes::add);
    }

    private List<String> processParamCandidateBatchNos(HcBatchTraceFactDTO fact) {
        return Stream.of(fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getParentProductionBatchNo())
                .map(value -> firstNotBlank(value))
                .filter(this::isNotBlank)
                .distinct()
                .toList();
    }

    private List<String> processParamCandidateCodes(HcBatchTraceFactDTO fact) {
        ProcessDefinition process = processOf(fact.getSourceType());
        return Stream.of(fact.getOperationCode(), process.code(), fact.getSourceType())
                .map(value -> firstNotBlank(value))
                .filter(this::isNotBlank)
                .distinct()
                .toList();
    }

    private String processParamLookupKey(Long tenantId, String batchNo, String processCode) {
        return tenantId + "#" + normalize(batchNo) + "#" + normalize(processCode);
    }

    private HcBatchTraceRespVO.ProcessParamItemRespVO buildProcessParamItem(String timelineKey,
                                                                           HcProcessParamRecordDO record) {
        HcBatchTraceRespVO.ProcessParamItemRespVO item = new HcBatchTraceRespVO.ProcessParamItemRespVO();
        item.setTimelineKey(timelineKey);
        item.setParamCode(record.getParamCode());
        item.setParamName(record.getParamName());
        item.setParamValue(record.getParamValue());
        item.setParamValueNum(record.getParamValueNum());
        item.setUom(record.getUom());
        item.setRecordTime(record.getRecordTime());
        item.setRecorderName(record.getRecorderName());
        item.setSourceFormName(record.getSourceFormName());
        item.setRemark(record.getRemark());
        return item;
    }

    private HcBatchTraceRespVO.TimelineNodeRespVO buildTimelineNode(HcBatchTraceFactDTO fact,
                                                                   List<HcBatchTraceRespVO.AuxiliaryItemRespVO> sqlAuxiliaryItems) {
        ProcessDefinition process = processOf(fact.getSourceType());
        String timelineKey = sourceKey(fact);
        String status = resolveStatus(fact);
        HcBatchTraceRespVO.TimelineNodeRespVO node = new HcBatchTraceRespVO.TimelineNodeRespVO();
        node.setKey(timelineKey);
        node.setSourceType(fact.getSourceType());
        node.setSourceId(fact.getId());
        node.setProcessCode(process.code());
        node.setProcessName(firstNotBlank(fact.getOperationName(), process.name(), fact.getSourceTypeName()));
        node.setProcessOrder(process.order());
        node.setBatchNo(firstNotBlank(fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getBizNo()));
        node.setSourceBatchNo(fact.getSourceBatchNo());
        node.setParentBatchNo(fact.getParentProductionBatchNo());
        node.setStatus(status);
        node.setStatusText(statusText(status));
        node.setEventTime(eventTime(fact));
        node.setSummary(buildSummary(fact, status));
        node.setReportNo(fact.getBizNo());
        node.setReportStatus(fact.getReportStatus());
        node.setPlanNo(fact.getPlanNo());
        node.setWorkCenterName(fact.getWorkCenterName());
        node.setEquipmentName(firstNotBlank(fact.getEquipmentName(), fact.getEquipmentCode()));
        node.setRecorderName(fact.getRecorderName());
        node.setConfirmerName(fact.getConfirmerName());
        node.setDetails(buildDetails(fact));
        node.setQualityItems(buildQualityItems(timelineKey, fact));

        List<HcBatchTraceRespVO.AuxiliaryItemRespVO> auxiliaryItems = new ArrayList<>(sqlAuxiliaryItems);
        auxiliaryItems.addAll(buildWetAuxiliaryItems(timelineKey, fact));
        node.setAuxiliaryItems(auxiliaryItems);
        return node;
    }

    private List<HcBatchTraceFactDTO> filterFactsForBatch(BatchKeys batchKeys, List<HcBatchTraceFactDTO> facts) {
        if (!batchKeys.singleLineTrace()) {
            return facts;
        }
        Set<String> adhesiveSourceBatchNos = facts.stream()
                .filter(fact -> "SLITTING_SLICE".equals(fact.getSourceType()))
                .filter(fact -> equalsAny(batchKeys.sliceBatchNo, fact.getProductionBatchNo(), fact.getBizNo()))
                .flatMap(fact -> Stream.of(fact.getSourceBatchNo(), fact.getParentProductionBatchNo()))
                .map(this::normalize)
                .filter(this::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return facts.stream()
                .filter(fact -> includeSingleLineFact(batchKeys, adhesiveSourceBatchNos, fact))
                .toList();
    }

    private boolean includeSingleLineFact(BatchKeys batchKeys, Set<String> adhesiveSourceBatchNos, HcBatchTraceFactDTO fact) {
        String sourceType = fact.getSourceType();
        String rootBatchNo = batchKeys.rootBatchNo;
        String segmentBatchNo = batchKeys.segmentBatchNo;
        String sliceBatchNo = batchKeys.sliceBatchNo;
        String finalBatchNo = batchKeys.finalBatchNo;
        return switch (sourceType) {
            case "FORMULA_REPORT", "WET_REPORT", "ROUGH_GRINDING_FIRST" ->
                    equalsAny(rootBatchNo, fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getParentProductionBatchNo());
            case "ROUGH_GRINDING_FIRST_ALLOCATION" ->
                    equalsAny(segmentBatchNo, fact.getProductionBatchNo(), fact.getBizNo());
            case "ROUGH_GRINDING_SECOND" ->
                    equalsAny(segmentBatchNo, fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getParentProductionBatchNo());
            case "ADHESIVE_REPORT" -> includeAdhesive1Fact(segmentBatchNo, adhesiveSourceBatchNos, fact);
            case "SLITTING_SLICE" -> equalsAny(sliceBatchNo, fact.getProductionBatchNo(), fact.getBizNo());
            case "PRESS_SLOT_REPORT", "ADHESIVE2_REPORT" ->
                    equalsAny(sliceBatchNo, fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getParentProductionBatchNo(), fact.getBizNo());
            case "CUT_ROUND_REPORT" -> includeCutFact(sliceBatchNo, finalBatchNo, fact);
            default ->
                    equalsAny(sliceBatchNo, fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getParentProductionBatchNo(), fact.getBizNo());
        };
    }

    private boolean includeAdhesive1Fact(String segmentBatchNo, Set<String> adhesiveSourceBatchNos, HcBatchTraceFactDTO fact) {
        if (!adhesiveSourceBatchNos.isEmpty()) {
            return containsAnyValue(adhesiveSourceBatchNos, fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getParentProductionBatchNo());
        }
        return equalsAny(segmentBatchNo, fact.getProductionBatchNo(), fact.getSourceBatchNo(), fact.getParentProductionBatchNo())
                || startsWithAny(fact.getProductionBatchNo(), segmentBatchNo + "-J")
                || startsWithAny(fact.getSourceBatchNo(), segmentBatchNo + "-J")
                || startsWithAny(fact.getParentProductionBatchNo(), segmentBatchNo + "-J");
    }

    private boolean includeCutFact(String sliceBatchNo, String finalBatchNo, HcBatchTraceFactDTO fact) {
        if (isNotBlank(finalBatchNo)) {
            return equalsAny(finalBatchNo, fact.getProductionBatchNo(), fact.getBizNo());
        }
        return equalsAny(sliceBatchNo, fact.getSourceBatchNo(), fact.getParentProductionBatchNo(), fact.getBizNo())
                || startsWithAny(fact.getProductionBatchNo(), sliceBatchNo);
    }

    private List<HcBatchTraceFactDTO> filterStartFacts(List<HcBatchTraceFactDTO> facts) {
        return facts.stream()
                .filter(fact -> !"START".equalsIgnoreCase(firstNotBlank(fact.getReportType())))
                .toList();
    }

    private List<HcBatchTraceFactDTO> collapseCompletionReportFacts(List<HcBatchTraceFactDTO> facts) {
        LinkedHashMap<String, HcBatchTraceFactDTO> collapsed = new LinkedHashMap<>();
        List<HcBatchTraceFactDTO> result = new ArrayList<>();
        for (HcBatchTraceFactDTO fact : facts) {
            if (!COMPLETION_REPORT_COLLAPSE_SOURCE_TYPES.contains(fact.getSourceType())) {
                result.add(fact);
                continue;
            }
            String key = completionReportGroupKey(fact);
            HcBatchTraceFactDTO exists = collapsed.get(key);
            collapsed.put(key, chooseRepresentativeFact(exists, fact));
        }
        result.addAll(collapsed.values());
        return result;
    }

    private String completionReportGroupKey(HcBatchTraceFactDTO fact) {
        return fact.getSourceType() + ":" + normalize(firstNotBlank(fact.getProductionBatchNo(),
                fact.getSourceBatchNo(), fact.getParentProductionBatchNo(), fact.getPlanNo()));
    }

    private HcBatchTraceFactDTO chooseRepresentativeFact(HcBatchTraceFactDTO exists, HcBatchTraceFactDTO candidate) {
        if (exists == null) {
            return candidate;
        }
        int existsScore = representativeScore(exists);
        int candidateScore = representativeScore(candidate);
        if (candidateScore != existsScore) {
            return candidateScore > existsScore ? candidate : exists;
        }
        LocalDateTime existsTime = eventTime(exists);
        LocalDateTime candidateTime = eventTime(candidate);
        if (existsTime == null) {
            return candidateTime == null ? exists : candidate;
        }
        if (candidateTime == null) {
            return exists;
        }
        return candidateTime.isAfter(existsTime) ? candidate : exists;
    }

    private int representativeScore(HcBatchTraceFactDTO fact) {
        int score = 0;
        if ("END".equalsIgnoreCase(firstNotBlank(fact.getReportType()))) {
            score += 30;
        }
        String status = upper(fact.getReportStatus());
        if (!status.contains("RUNNING") && !status.contains("DRAFT") && !status.contains("PENDING")
                && !status.contains("CREATED") && !status.contains("UNCONFIRMED")) {
            score += 20;
        }
        if (isNotBlank(fact.getInspectionNo()) || isNotBlank(fact.getInspectionStatus()) || isNotBlank(fact.getInspectionResult())) {
            score += 10;
        }
        if (fact.getConfirmerTime() != null || isNotBlank(fact.getConfirmerName())) {
            score += 5;
        }
        if (fact.getEndTime() != null) {
            score += 3;
        }
        return score;
    }

    private HcBatchTraceRespVO.OverviewRespVO buildOverview(BatchKeys batchKeys,
                                                           List<HcBatchTraceRespVO.TimelineNodeRespVO> timelineNodes) {
        HcBatchTraceRespVO.OverviewRespVO overview = new HcBatchTraceRespVO.OverviewRespVO();
        overview.setInputBatchNo(batchKeys.inputBatchNo);
        overview.setNormalizedBatchNo(batchKeys.normalizedBatchNo);
        overview.setBatchType(batchKeys.batchType);
        overview.setBatchTypeName(batchTypeName(batchKeys.batchType));
        overview.setRootBatchNo(batchKeys.rootBatchNo);
        overview.setSegmentBatchNo(batchKeys.segmentBatchNo);
        overview.setSliceBatchNo(batchKeys.sliceBatchNo);
        overview.setFinalBatchNo(batchKeys.finalBatchNo);
        overview.setFactCount(timelineNodes.size());
        overview.setBranchCount(countBranches(timelineNodes));

        HcBatchTraceRespVO.TimelineNodeRespVO current = timelineNodes.stream()
                .max(Comparator.comparingInt(HcBatchTraceRespVO.TimelineNodeRespVO::getProcessOrder)
                        .thenComparing(node -> node.getEventTime() == null ? LocalDateTime.MIN : node.getEventTime()))
                .orElse(null);
        if (current == null) {
            overview.setCurrentStatus("PENDING");
            overview.setCurrentStatusText("未命中报工");
            return overview;
        }
        overview.setCurrentProcessCode(current.getProcessCode());
        overview.setCurrentProcessName(current.getProcessName());
        overview.setCurrentStatus(current.getStatus());
        overview.setCurrentStatusText(current.getStatusText());
        return overview;
    }

    private List<HcBatchTraceRespVO.TreeNodeRespVO> buildTree(BatchKeys batchKeys, List<HcBatchTraceFactDTO> facts) {
        LinkedHashMap<String, HcBatchTraceRespVO.TreeNodeRespVO> roots = new LinkedHashMap<>();
        if (isNotBlank(batchKeys.rootBatchNo)) {
            roots.put(batchKeys.rootBatchNo, newTreeNode("ROOT", batchKeys.rootBatchNo));
        }

        LinkedHashSet<String> batchNos = new LinkedHashSet<>();
        if (isNotBlank(batchKeys.normalizedBatchNo)) {
            batchNos.add(batchKeys.normalizedBatchNo);
        }
        for (HcBatchTraceFactDTO fact : facts) {
            collectFactBatchNos(fact, batchNos);
        }
        for (String batchNo : batchNos) {
            addBatchToTree(roots, BatchKeys.parse(batchNo));
        }
        List<HcBatchTraceRespVO.TreeNodeRespVO> rootNodes = new ArrayList<>(roots.values());
        rootNodes.forEach(node -> fillTreeStatus(node, facts));
        return rootNodes;
    }

    private void addBatchToTree(LinkedHashMap<String, HcBatchTraceRespVO.TreeNodeRespVO> roots, BatchKeys keys) {
        if (!isNotBlank(keys.rootBatchNo)) {
            roots.putIfAbsent(keys.normalizedBatchNo, newTreeNode(keys.batchType, keys.normalizedBatchNo));
            return;
        }
        HcBatchTraceRespVO.TreeNodeRespVO root = roots.computeIfAbsent(keys.rootBatchNo, batchNo -> newTreeNode("ROOT", batchNo));
        if (!isNotBlank(keys.segmentBatchNo)) {
            return;
        }
        HcBatchTraceRespVO.TreeNodeRespVO segment = getOrCreateChild(root, "SEGMENT", keys.segmentBatchNo);
        if (!isNotBlank(keys.sliceBatchNo)) {
            return;
        }
        HcBatchTraceRespVO.TreeNodeRespVO slice = getOrCreateChild(segment, "SLICE", keys.sliceBatchNo);
        if (isNotBlank(keys.finalBatchNo)) {
            getOrCreateChild(slice, "FINAL", keys.finalBatchNo);
        }
    }

    private HcBatchTraceRespVO.TreeNodeRespVO getOrCreateChild(HcBatchTraceRespVO.TreeNodeRespVO parent,
                                                              String nodeType,
                                                              String batchNo) {
        for (HcBatchTraceRespVO.TreeNodeRespVO child : parent.getChildren()) {
            if (Objects.equals(child.getBatchNo(), batchNo)) {
                return child;
            }
        }
        HcBatchTraceRespVO.TreeNodeRespVO child = newTreeNode(nodeType, batchNo);
        parent.getChildren().add(child);
        return child;
    }

    private HcBatchTraceRespVO.TreeNodeRespVO newTreeNode(String nodeType, String batchNo) {
        HcBatchTraceRespVO.TreeNodeRespVO node = new HcBatchTraceRespVO.TreeNodeRespVO();
        node.setKey(nodeType + ":" + batchNo);
        node.setNodeType(nodeType);
        node.setBatchNo(batchNo);
        node.setTitle(nodeTypeName(nodeType) + " " + batchNo);
        node.setStatus("PENDING");
        node.setStatusText(statusText("PENDING"));
        return node;
    }

    private void fillTreeStatus(HcBatchTraceRespVO.TreeNodeRespVO node, List<HcBatchTraceFactDTO> facts) {
        String status = "PENDING";
        for (HcBatchTraceFactDTO fact : facts) {
            if (matchesNode(fact, node.getBatchNo(), node.getNodeType())) {
                status = mergeStatus(status, resolveStatus(fact));
            }
        }
        for (HcBatchTraceRespVO.TreeNodeRespVO child : node.getChildren()) {
            fillTreeStatus(child, facts);
            status = mergeStatus(status, child.getStatus());
        }
        node.setStatus(status);
        node.setStatusText(statusText(status));
    }

    private boolean matchesNode(HcBatchTraceFactDTO fact, String batchNo, String nodeType) {
        if (!isNotBlank(batchNo)) {
            return false;
        }
        Set<String> values = new HashSet<>();
        collectFactBatchNos(fact, values);
        for (String value : values) {
            if (Objects.equals(value, batchNo)) {
                return true;
            }
            if (("ROOT".equals(nodeType) || "SEGMENT".equals(nodeType) || "SLICE".equals(nodeType))
                    && value.startsWith(batchNo)) {
                return true;
            }
        }
        return false;
    }

    private void collectFactBatchNos(HcBatchTraceFactDTO fact, Set<String> batchNos) {
        addIfBatchNo(batchNos, fact.getBizNo());
        addIfBatchNo(batchNos, fact.getSourceBatchNo());
        addIfBatchNo(batchNos, fact.getProductionBatchNo());
        addIfBatchNo(batchNos, fact.getParentProductionBatchNo());
    }

    private HcBatchTraceRespVO.AuxiliaryItemRespVO buildAuxiliaryItem(HcBatchTraceAuxiliaryDTO row) {
        HcBatchTraceRespVO.AuxiliaryItemRespVO item = new HcBatchTraceRespVO.AuxiliaryItemRespVO();
        item.setMaterialType(row.getMaterialType());
        item.setMaterialTypeName(row.getMaterialTypeName());
        item.setMaterialCode(row.getMaterialCode());
        item.setMaterialName(row.getMaterialName());
        item.setBatchNo(row.getBatchNo());
        item.setUsageInfo(row.getUsageInfo());
        item.setSourceTable(row.getSourceTable());
        return item;
    }

    private List<HcBatchTraceRespVO.AuxiliaryItemRespVO> buildWetAuxiliaryItems(String timelineKey, HcBatchTraceFactDTO fact) {
        if (!"WET_REPORT".equals(fact.getSourceType()) || !isNotBlank(fact.getExtraJson())) {
            return List.of();
        }
        Map<String, Object> extra = JsonUtils.parseObjectQuietly(fact.getExtraJson(), MAP_TYPE);
        if (extra == null || extra.isEmpty()) {
            return List.of();
        }
        List<HcBatchTraceRespVO.AuxiliaryItemRespVO> items = new ArrayList<>();
        addWetAuxiliary(items, timelineKey, "PET", "PET", value(extra, "petModel"), value(extra, "petBatchNo"),
                "mes_sfc_operation_report.extra_json", "");
        addWetAuxiliary(items, timelineKey, "GUIDE_CLOTH", "湿法导布", null, value(extra, "guideClothBatchNo"),
                "mes_sfc_operation_report.extra_json",
                joinUsage("累计次数", value(extra, "guideClothUseCount"),
                        "本次更换", value(extra, "guideClothChanged"),
                        "更换原因", value(extra, "guideClothChangeReason"),
                        "累计长度", value(extra, "guideClothUsedLength")));
        return items;
    }

    private void addWetAuxiliary(List<HcBatchTraceRespVO.AuxiliaryItemRespVO> items,
                                 String timelineKey,
                                 String materialType,
                                 String materialTypeName,
                                 String materialName,
                                 String batchNo,
                                 String sourceTable,
                                 String usageInfo) {
        if (!isNotBlank(materialName) && !isNotBlank(batchNo) && !isNotBlank(usageInfo)) {
            return;
        }
        HcBatchTraceRespVO.AuxiliaryItemRespVO item = new HcBatchTraceRespVO.AuxiliaryItemRespVO();
        item.setTimelineKey(timelineKey);
        item.setMaterialType(materialType);
        item.setMaterialTypeName(materialTypeName);
        item.setMaterialName(materialName);
        item.setBatchNo(batchNo);
        item.setUsageInfo(usageInfo);
        item.setSourceTable(sourceTable);
        items.add(item);
    }

    private List<HcBatchTraceRespVO.QualityItemRespVO> buildQualityItems(String timelineKey, HcBatchTraceFactDTO fact) {
        List<HcBatchTraceRespVO.QualityItemRespVO> items = new ArrayList<>();
        if (isNotBlank(fact.getInspectionNo()) || isNotBlank(fact.getInspectionStatus()) || isNotBlank(fact.getInspectionResult())) {
            HcBatchTraceRespVO.QualityItemRespVO item = new HcBatchTraceRespVO.QualityItemRespVO();
            item.setTimelineKey(timelineKey);
            item.setInspectionNo(firstNotBlank(fact.getInspectionNo(), "检验事件"));
            item.setInspectionType(firstNotBlank(fact.getSourceTypeName(), fact.getOperationName()));
            item.setStatus(fact.getInspectionStatus());
            item.setResult(fact.getInspectionResult());
            item.setEventTime(firstNotNull(fact.getInspectionReturnTime(), fact.getInspectionApplyTime(), eventTime(fact)));
            item.setRemark(fact.getInspectionRemark());
            items.add(item);
        }
        if (isNotBlank(fact.getSelfCheck())) {
            HcBatchTraceRespVO.QualityItemRespVO item = new HcBatchTraceRespVO.QualityItemRespVO();
            item.setTimelineKey(timelineKey);
            item.setInspectionNo("自检");
            item.setInspectionType("过程自检");
            item.setResult(fact.getSelfCheck());
            item.setEventTime(eventTime(fact));
            item.setRemark(fact.getDefectCode());
            items.add(item);
        } else if (isNotBlank(fact.getDefectCode())) {
            HcBatchTraceRespVO.QualityItemRespVO item = new HcBatchTraceRespVO.QualityItemRespVO();
            item.setTimelineKey(timelineKey);
            item.setInspectionNo("缺陷位置");
            item.setInspectionType("过程缺陷");
            item.setResult(fact.getDefectCode());
            item.setEventTime(eventTime(fact));
            item.setRemark(fact.getRemark());
            items.add(item);
        }
        return items;
    }

    private Map<String, String> buildDetails(HcBatchTraceFactDTO fact) {
        LinkedHashMap<String, String> details = new LinkedHashMap<>();
        Map<String, Object> extra = extraMap(fact);
        putDetail(details, "报工时间", formatDateTime(eventTime(fact)));
        switch (fact.getSourceType()) {
            case "FORMULA_REPORT" -> putDetail(details, "报工量", formatQty(fact.getReportQty(), fact.getReportUom()));
            case "WET_REPORT" -> {
                putDetail(details, "收卷米", firstNotBlank(formatMeter(fact.getOutputLength()),
                        meterValue(extra, "receiveLength")));
                putDetail(details, "固定损耗", firstNotBlank(formatMeter(fact.getLossLength()),
                        meterValue(extra, "printLossLength")));
                putDetail(details, "送检米", firstNotBlank(formatMeter(fact.getNapSampleLength()),
                        meterValue(extra, "napSampleLength")));
                putDetail(details, "异常位置", firstNotBlank(fact.getDefectCode(), fact.getInspectionRemark(), fact.getRemark()));
            }
            case "ROUGH_GRINDING_FIRST", "ROUGH_GRINDING_FIRST_ALLOCATION" -> {
                if ("ROUGH_GRINDING_FIRST_ALLOCATION".equals(fact.getSourceType())) {
                    putDetail(details, "加工单元", firstNotBlank(value(extra, "segmentMark"), "NONE"));
                    putDetail(details, "加工位置", formatPositionRange(fact.getStartPosition(), fact.getEndPosition()));
                    putDetail(details, "分配米数", formatMeter(fact.getProcessLength()));
                }
                putDetail(details, "收卷米", formatMeter(firstNotNull(fact.getOutputLength(), fact.getReportQty())));
                putDetail(details, "固定损耗", formatMeter(fact.getLossLength()));
                putDetail(details, "送检米", formatMeter(fact.getNapSampleLength()));
                putDetail(details, "异常位置", firstNotBlank(fact.getDefectCode(), fact.getRemark()));
            }
            case "ROUGH_GRINDING_SECOND" -> {
                putDetail(details, "报工米", formatMeter(firstNotNull(fact.getOutputLength(), fact.getReportQty())));
                putDetail(details, "固定损耗", formatMeter(fact.getLossLength()));
                putDetail(details, "送检米", formatMeter(fact.getNapSampleLength()));
                putDetail(details, "异常位置", firstNotBlank(fact.getDefectCode(), fact.getRemark()));
                putDetail(details, "加工位置", formatPositionRange(fact.getStartPosition(), fact.getEndPosition()));
                putDetail(details, "加工长度", formatMeter(firstNotNull(fact.getProcessLength(), fact.getReportQty())));
            }
            case "ADHESIVE_REPORT" -> {
                putDetail(details, "胶板批号", fact.getGlueBoardBatchNo());
                putDetail(details, "固定损耗", formatMeter(fact.getLossLength()));
                putDetail(details, "留样送检", formatMeter(fact.getNapSampleLength()));
                putDetail(details, "实际加工", formatMeter(firstNotNull(fact.getOutputLength(), fact.getReportQty())));
                putDetail(details, "剩余米", formatRemainingLength(fact.getSlittingRemainingLength()));
            }
            case "SLITTING_SLICE" -> {
                putDetail(details, "尺寸", firstNotBlank(fact.getSizeName(), fact.getSizeCode(), formatDecimal(fact.getReportQty())));
                putDetail(details, "外观检验结果", qualityText(firstNotBlank(visualResult(extra), fact.getSelfCheck())));
            }
            case "PRESS_SLOT_REPORT" -> {
                // 压槽节点只展示报工时间。
            }
            case "ADHESIVE2_REPORT" -> {
                putDetail(details, "胶板型号", firstNotBlank(fact.getGlueBoardModel(), value(extra, "glueBoardModel")));
                putDetail(details, "胶板批次", fact.getGlueBoardBatchNo());
                putDetail(details, "作业类型", firstNotBlank(value(extra, "operationType"), value(extra, "workType"),
                        value(extra, "jobType"), value(extra, "taskType"), value(extra, "inspectionScene")));
                putDetail(details, "COA标记", coaText(firstNotBlank(fact.getCoaFlag(), value(extra, "coaFlag"))));
            }
            case "CUT_ROUND_REPORT" -> {
                putDetail(details, "最终胶板尺寸", firstNotBlank(value(extra, "actualSizeRule"),
                        value(extra, "finalGlueBoardSize"), value(extra, "productSize"), value(extra, "sizeName")));
                putDetail(details, "外观检验结果", qualityText(firstNotBlank(visualResult(extra), fact.getSelfCheck())));
                putDetail(details, "成品检验结果", qualityText(firstNotBlank(fact.getInspectionResult(), fact.getInspectionStatus())));
            }
            default -> putDetail(details, "数量", formatQty(fact.getReportQty(), fact.getReportUom()));
        }
        return details;
    }

    private void putDetail(Map<String, String> details, String label, String value) {
        if (isNotBlank(value)) {
            details.put(label, value);
        }
    }

    private Map<String, Object> extraMap(HcBatchTraceFactDTO fact) {
        if (fact == null || !isNotBlank(fact.getExtraJson())) {
            return Map.of();
        }
        Map<String, Object> extra = JsonUtils.parseObjectQuietly(fact.getExtraJson(), MAP_TYPE);
        return extra == null ? Map.of() : extra;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.toString().replace('T', ' ');
    }

    private String formatMeter(BigDecimal value) {
        return formatQty(value, "m");
    }

    private String meterValue(Map<String, Object> extra, String key) {
        String value = value(extra, key);
        if (!isNotBlank(value)) {
            return "";
        }
        String text = value.trim();
        return text.matches("-?\\d+(\\.\\d+)?") ? text + " m" : text;
    }

    private String formatRemainingLength(BigDecimal value) {
        if (value == null) {
            return "";
        }
        String text = formatMeter(value);
        return value.compareTo(BigDecimal.ZERO) == 0 ? text + "（已用完）" : text;
    }

    private String formatPositionRange(BigDecimal start, BigDecimal end) {
        String startText = formatDecimal(start);
        String endText = formatDecimal(end);
        if (isNotBlank(startText) && isNotBlank(endText)) {
            return startText + " - " + endText;
        }
        return firstNotBlank(startText, endText);
    }

    private String formatDecimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private String visualResult(Map<String, Object> extra) {
        return firstNotBlank(value(extra, "visualInspectionResult"), value(extra, "appearanceResult"),
                value(extra, "visualResult"), value(extra, "inspectionResult"), value(extra, "result"));
    }

    private String qualityText(String value) {
        String text = firstNotBlank(value);
        if (!isNotBlank(text)) {
            return "";
        }
        return switch (upper(text)) {
            case "OK", "PASS", "APPROVED" -> "合格";
            case "NG", "FAIL", "FAILED", "REJECTED" -> "不合格";
            case "PENDING" -> "待检验";
            case "INSPECTING" -> "检验中";
            case "COMPLETED" -> "已完成";
            default -> text;
        };
    }

    private String coaText(String value) {
        String text = firstNotBlank(value);
        if (!isNotBlank(text)) {
            return "";
        }
        return isTruthyText(text) ? "是" : text;
    }

    private boolean isTruthyText(String value) {
        String text = normalize(value);
        return "Y".equals(text) || "YES".equals(text) || "TRUE".equals(text) || "1".equals(text)
                || "是".equals(text) || "COA".equals(text);
    }

    private String resolveStatus(HcBatchTraceFactDTO fact) {
        if ("END".equalsIgnoreCase(firstNotBlank(fact.getReportType()))) {
            return "COMPLETED";
        }
        String status = upper(fact.getReportStatus());
        if (status.contains("DRAFT") || status.contains("RUNNING") || status.contains("PROCESSING")
                || status.contains("PENDING") || status.contains("UNCONFIRMED") || status.contains("CREATED")) {
            return "CURRENT";
        }
        return "COMPLETED";
    }

    private String mergeStatus(String current, String next) {
        if ("CURRENT".equals(current) || "CURRENT".equals(next)) {
            return "CURRENT";
        }
        if ("COMPLETED".equals(current) || "COMPLETED".equals(next)) {
            return "COMPLETED";
        }
        return "PENDING";
    }

    private String buildSummary(HcBatchTraceFactDTO fact, String status) {
        List<String> parts = new ArrayList<>();
        parts.add(statusText(status));
        String batchNo = firstNotBlank(fact.getProductionBatchNo(), fact.getSourceBatchNo());
        if (isNotBlank(batchNo)) {
            parts.add(batchNo);
        }
        String measure = summaryMeasure(fact);
        if (isNotBlank(measure)) {
            parts.add(measure);
        }
        if (isNotBlank(fact.getInspectionResult())) {
            parts.add("检验 " + fact.getInspectionResult());
        }
        return String.join("，", parts);
    }

    private String summaryMeasure(HcBatchTraceFactDTO fact) {
        if ("SLITTING_SLICE".equals(fact.getSourceType())) {
            String size = firstNotBlank(fact.getSizeName(), fact.getSizeCode(), formatDecimal(fact.getReportQty()));
            return isNotBlank(size) ? "尺寸 " + size : "";
        }
        if ("PRESS_SLOT_REPORT".equals(fact.getSourceType())) {
            return "";
        }
        return formatQty(fact.getReportQty(), fact.getReportUom());
    }

    private Integer countBranches(List<HcBatchTraceRespVO.TimelineNodeRespVO> timelineNodes) {
        return (int) timelineNodes.stream()
                .map(HcBatchTraceRespVO.TimelineNodeRespVO::getBatchNo)
                .filter(this::isNotBlank)
                .filter(batchNo -> LAST_THREE_DIGITS.matcher(removeFinalSuffix(batchNo)).find())
                .distinct()
                .count();
    }

    private ProcessDefinition processOf(String sourceType) {
        return PROCESS_BY_SOURCE_TYPE.getOrDefault(sourceType, new ProcessDefinition(sourceType, sourceType, 999, Set.of(sourceType)));
    }

    private LocalDateTime eventTime(HcBatchTraceFactDTO fact) {
        return firstNotNull(fact.getConfirmerTime(), fact.getEndTime(), fact.getRecorderTime(), fact.getStartTime(), fact.getCreateTime());
    }

    private String sourceKey(HcBatchTraceFactDTO fact) {
        return sourceKey(fact.getSourceType(), fact.getId());
    }

    private String sourceKey(String sourceType, Long sourceId) {
        if (!isNotBlank(sourceType) || sourceId == null) {
            return "";
        }
        return sourceType + ":" + sourceId;
    }

    private boolean isEmptyAuxiliary(HcBatchTraceRespVO.AuxiliaryItemRespVO item) {
        return !isNotBlank(item.getMaterialCode()) && !isNotBlank(item.getMaterialName())
                && !isNotBlank(item.getBatchNo()) && !isNotBlank(item.getUsageInfo());
    }

    private void addIfBatchNo(Set<String> batchNos, String value) {
        String batchNo = normalize(value);
        if (!isNotBlank(batchNo) || batchNo.contains(":") || batchNo.contains("-")) {
            return;
        }
        if (batchNo.length() >= 8) {
            batchNos.add(batchNo);
        }
    }

    private String batchTypeName(String batchType) {
        return switch (batchType) {
            case "ROOT" -> "母批";
            case "SEGMENT" -> "分段批次";
            case "SLICE" -> "单片片号";
            case "FINAL" -> "裁切片号";
            default -> "未知批次";
        };
    }

    private String nodeTypeName(String nodeType) {
        return batchTypeName(nodeType);
    }

    private String statusText(String status) {
        return switch (status) {
            case "CURRENT" -> "加工中";
            case "COMPLETED" -> "已完成";
            case "PENDING" -> "待加工";
            default -> firstNotBlank(status, "-");
        };
    }

    private String formatQty(BigDecimal qty, String uom) {
        if (qty == null) {
            return "";
        }
        String value = qty.stripTrailingZeros().toPlainString();
        return isNotBlank(uom) ? value + " " + uom : value;
    }

    private String value(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private String joinUsage(String... pairs) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (isNotBlank(pairs[i + 1])) {
                values.add(pairs[i] + " " + pairs[i + 1]);
            }
        }
        return String.join("；", values);
    }

    private String joinNonBlank(String delimiter, String... values) {
        return String.join(delimiter, Arrays.stream(values)
                .filter(this::isNotBlank)
                .toList());
    }

    private boolean equalsAny(String target, String... values) {
        String normalizedTarget = normalize(target);
        if (!isNotBlank(normalizedTarget) || values == null) {
            return false;
        }
        for (String value : values) {
            if (Objects.equals(normalizedTarget, normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyValue(Set<String> targets, String... values) {
        if (targets == null || targets.isEmpty() || values == null) {
            return false;
        }
        for (String value : values) {
            if (targets.contains(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithAny(String value, String prefix) {
        String normalizedValue = normalize(value);
        String normalizedPrefix = normalize(prefix);
        return isNotBlank(normalizedValue) && isNotBlank(normalizedPrefix) && normalizedValue.startsWith(normalizedPrefix);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (isNotBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String upper(String value) {
        return value == null ? "" : value.toUpperCase(Locale.ROOT);
    }

    private boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String removeFinalSuffix(String batchNo) {
        String value = normalize(batchNo);
        if (value.length() >= 12 && (value.endsWith("A") || value.endsWith("B"))
                && Character.isDigit(value.charAt(value.length() - 2))
                && LAST_THREE_DIGITS.matcher(value.substring(0, value.length() - 1)).find()) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record ProcessDefinition(String code, String name, int order, Set<String> sourceTypes) {
    }

    private static class BatchKeys {
        private final String inputBatchNo;
        private final String normalizedBatchNo;
        private final String batchType;
        private final String rootBatchNo;
        private final String segmentBatchNo;
        private final String sliceBatchNo;
        private final String finalBatchNo;
        private final String branchPrefix;

        private BatchKeys(String inputBatchNo,
                          String normalizedBatchNo,
                          String batchType,
                          String rootBatchNo,
                          String segmentBatchNo,
                          String sliceBatchNo,
                          String finalBatchNo,
                          String branchPrefix) {
            this.inputBatchNo = inputBatchNo;
            this.normalizedBatchNo = normalizedBatchNo;
            this.batchType = batchType;
            this.rootBatchNo = rootBatchNo;
            this.segmentBatchNo = segmentBatchNo;
            this.sliceBatchNo = sliceBatchNo;
            this.finalBatchNo = finalBatchNo;
            this.branchPrefix = branchPrefix;
        }

        private static BatchKeys parse(String rawBatchNo) {
            String input = rawBatchNo == null ? "" : rawBatchNo.trim();
            String normalized = input.replace(" ", "").toUpperCase(Locale.ROOT);
            String withoutGlueSuffix = removeGlueSuffix(normalized);
            String finalBatchNo = "";
            String sliceBatchNo = "";
            String segmentBatchNo = "";
            String rootBatchNo = "";
            String batchType = "UNKNOWN";

            String base = withoutGlueSuffix;
            if (base.length() >= 12 && (base.endsWith("A") || base.endsWith("B"))
                    && Character.isDigit(base.charAt(base.length() - 2))
                    && LAST_THREE_DIGITS.matcher(base.substring(0, base.length() - 1)).find()) {
                finalBatchNo = base;
                sliceBatchNo = base.substring(0, base.length() - 1);
                batchType = "FINAL";
            } else if (base.length() >= 11 && LAST_THREE_DIGITS.matcher(base).find()) {
                sliceBatchNo = base;
                batchType = "SLICE";
            }

            if (!sliceBatchNo.isEmpty() && sliceBatchNo.length() > 3) {
                segmentBatchNo = sliceBatchNo.substring(0, sliceBatchNo.length() - 3);
            } else if (base.length() >= 9 && isLikelySegment(base)) {
                segmentBatchNo = base;
                batchType = "SEGMENT";
            }

            if (!segmentBatchNo.isEmpty() && segmentBatchNo.length() >= 8) {
                rootBatchNo = segmentBatchNo.substring(0, 8);
            } else if (base.length() >= 8) {
                rootBatchNo = base.substring(0, 8);
                if ("UNKNOWN".equals(batchType)) {
                    batchType = base.length() == 8 ? "ROOT" : "SEGMENT";
                    if ("SEGMENT".equals(batchType)) {
                        segmentBatchNo = base;
                    }
                }
            }

            if ("UNKNOWN".equals(batchType)) {
                batchType = "ROOT";
            }

            String branchPrefix = switch (batchType) {
                case "ROOT" -> rootBatchNo;
                case "SEGMENT" -> segmentBatchNo;
                case "SLICE", "FINAL" -> sliceBatchNo;
                default -> normalized;
            };
            return new BatchKeys(input, normalized, batchType, rootBatchNo, segmentBatchNo, sliceBatchNo, finalBatchNo, branchPrefix);
        }

        private List<String> exactBatchNos() {
            LinkedHashSet<String> values = new LinkedHashSet<>();
            add(values, normalizedBatchNo);
            add(values, removeGlueSuffix(normalizedBatchNo));
            add(values, rootBatchNo);
            add(values, segmentBatchNo);
            add(values, sliceBatchNo);
            add(values, finalBatchNo);
            return new ArrayList<>(values);
        }

        private boolean singleLineTrace() {
            return "SEGMENT".equals(batchType) || "SLICE".equals(batchType) || "FINAL".equals(batchType);
        }

        private static void add(Set<String> values, String value) {
            if (value != null && !value.trim().isEmpty()) {
                values.add(value);
            }
        }

        private static boolean isLikelySegment(String value) {
            return value.length() >= 9 && Character.isLetter(value.charAt(value.length() - 1));
        }

        private static String removeGlueSuffix(String value) {
            if (value == null) {
                return "";
            }
            int suffixIndex = value.indexOf("-J");
            return suffixIndex > 0 ? value.substring(0, suffixIndex) : value;
        }
    }

}
