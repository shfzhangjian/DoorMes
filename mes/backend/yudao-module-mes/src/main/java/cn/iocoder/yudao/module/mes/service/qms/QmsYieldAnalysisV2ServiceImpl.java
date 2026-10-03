package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsMotherRollGoodStatisticsStageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2PageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2SyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsYieldAnalysisV2SyncStatusRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldAnalysisSourceRow;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsYieldDailyFactDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsYieldAnalysisMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsYieldDailyFactMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class QmsYieldAnalysisV2ServiceImpl implements QmsYieldAnalysisV2Service {

    private static final int INSERT_BATCH_SIZE = 500;
    private static final LocalDateTime MIN_VALID_BUSINESS_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);
    private static final Pattern ADHESIVE_SEGMENT_PATTERN = Pattern.compile("^(.+)-J[0-9]+$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PIECE_PATTERN = Pattern.compile("^([A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z][PQRS])[0-9]{3}[A-Z]?$",
            Pattern.CASE_INSENSITIVE);

    @Resource
    private QmsMotherRollGoodStatisticsService qmsMotherRollGoodStatisticsService;
    @Resource
    private QmsYieldAnalysisService qmsYieldAnalysisService;
    @Resource
    private QmsYieldAnalysisMapper qmsYieldAnalysisMapper;
    @Resource
    private QmsYieldDailyFactMapper qmsYieldDailyFactMapper;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public PageResult<QmsMotherRollGoodStatisticsRespVO> getPage(QmsYieldAnalysisV2PageReqVO reqVO) {
        DateRange dateRange = resolveDateRange(reqVO);
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(reqVO.getPageNo(), 1);
        int pageSize = reqVO.getPageSize() == null ? 10 : reqVO.getPageSize();
        Integer limit = PageParam.PAGE_SIZE_NONE.equals(pageSize) ? null : Math.max(pageSize, 1);
        int offset = limit == null ? 0 : (pageNo - 1) * limit;
        List<QmsMotherRollGoodStatisticsRespVO> rows = readPivotRows(reqVO, dateRange, offset, limit);
        Long total = limit == null ? (long) rows.size() : qmsYieldDailyFactMapper.countDistinctPivot(
                TenantContextHolder.getTenantId(), dateRange.startDate(), dateRange.endDate(), reqVO);
        return new PageResult<>(rows, total == null ? 0L : total);
    }

    @Override
    public List<QmsMotherRollGoodStatisticsRespVO> getList(QmsYieldAnalysisV2PageReqVO reqVO) {
        DateRange dateRange = resolveDateRange(reqVO);
        return readPivotRows(reqVO, dateRange, 0, null);
    }

    @Override
    public QmsYieldAnalysisRespVO getOverview(QmsYieldAnalysisV2PageReqVO reqVO) {
        DateRange dateRange = resolveDateRange(reqVO);
        List<QmsYieldAnalysisSourceRow> sourceRows = qmsYieldDailyFactMapper.selectDistinctSourceRows(
                TenantContextHolder.getTenantId(), dateRange.startDate(), dateRange.endDate(), reqVO);
        return qmsYieldAnalysisService.getOverviewFromSourceRows(toYieldAnalysisReqVO(reqVO), sourceRows);
    }

    @Override
    public PageResult<QmsYieldAnalysisDetailRespVO> getDetailPage(QmsYieldAnalysisV2PageReqVO reqVO) {
        DateRange dateRange = resolveDateRange(reqVO);
        List<QmsYieldAnalysisSourceRow> sourceRows = qmsYieldDailyFactMapper.selectDistinctSourceRows(
                TenantContextHolder.getTenantId(), dateRange.startDate(), dateRange.endDate(), reqVO);
        PageResult<QmsYieldAnalysisDetailRespVO> result = qmsYieldAnalysisService.getDetailPageFromSourceRows(
                toYieldAnalysisReqVO(reqVO), sourceRows);
        enrichDetailReportInfo(result.getList(), readPivotRows(reqVO, dateRange, 0, null));
        return result;
    }

    @Override
    public List<String> getTargetModelOptions() {
        return qmsYieldAnalysisService.getTargetModelOptions();
    }

    @Override
    public QmsYieldAnalysisV2SyncStatusRespVO getSyncStatus() {
        QmsYieldAnalysisV2SyncStatusRespVO status = qmsYieldDailyFactMapper.selectSyncStatus(
                TenantContextHolder.getTenantId());
        if (status == null) {
            status = new QmsYieldAnalysisV2SyncStatusRespVO();
            status.setSettledSourceCount(0L);
        }
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsYieldAnalysisV2SyncRespVO syncUnsettledHistory() {
        long startedAt = System.currentTimeMillis();
        Long tenantId = TenantContextHolder.getTenantId();
        LocalDateTime syncTime = LocalDateTime.now();
        String syncBatchNo = syncTime.toLocalDate() + "-" + IdUtil.fastSimpleUUID();

        // 物化结果保存业务表的最新状态，不排除当天记录。页面统计仍按 confirm_time 归属日期。
        List<QmsYieldAnalysisSourceRow> loadedSourceRows = qmsYieldAnalysisMapper.selectSourceRows(
                tenantId, null, null, "ALL");
        if (loadedSourceRows == null) {
            loadedSourceRows = List.of();
        }
        Map<String, QmsYieldAnalysisSourceRow> currentSourceMap = new LinkedHashMap<>();
        for (QmsYieldAnalysisSourceRow sourceRow : loadedSourceRows) {
            currentSourceMap.put(buildSourceKey(sourceRow), sourceRow);
        }

        List<QmsYieldDailyFactDO> existingFacts = qmsYieldDailyFactMapper.selectActiveFacts(tenantId);
        if (existingFacts == null) {
            existingFacts = List.of();
        }
        Map<String, List<QmsYieldDailyFactDO>> existingFactMap = new LinkedHashMap<>();
        for (QmsYieldDailyFactDO fact : existingFacts) {
            existingFactMap.computeIfAbsent(buildSourceKey(fact), key -> new ArrayList<>()).add(fact);
        }

        QmsMotherRollGoodStatisticsPageReqVO motherReqVO = new QmsMotherRollGoodStatisticsPageReqVO();
        motherReqVO.setPageNo(1);
        motherReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsMotherRollGoodStatisticsRespVO> motherRows = currentSourceMap.isEmpty()
                ? List.of() : qmsMotherRollGoodStatisticsService.getMotherRollGoodStatisticsList(motherReqVO);
        if (motherRows == null) {
            motherRows = List.of();
        }
        List<PivotSnapshot> pivotSnapshots = buildPivotSnapshots(motherRows);
        Map<String, PivotSnapshot> pivotSnapshotMap = new LinkedHashMap<>();
        for (PivotSnapshot snapshot : pivotSnapshots) {
            pivotSnapshotMap.put(snapshot.hash(), snapshot);
        }

        Map<String, SourceMaterialization> changedSources = new LinkedHashMap<>();
        int insertedSourceCount = 0;
        int updatedSourceCount = 0;
        int unchangedSourceCount = 0;
        for (Map.Entry<String, QmsYieldAnalysisSourceRow> entry : currentSourceMap.entrySet()) {
            String sourceKey = entry.getKey();
            QmsYieldAnalysisSourceRow sourceRow = entry.getValue();
            String contentHash = buildSourceContentHash(sourceRow);
            List<PivotSnapshot> expectedPivots = resolvePivotSnapshots(sourceRow, pivotSnapshots);
            List<QmsYieldDailyFactDO> sourceFacts = existingFactMap.get(sourceKey);
            if (sourceFacts == null || sourceFacts.isEmpty()) {
                changedSources.put(sourceKey, new SourceMaterialization(sourceRow, contentHash, expectedPivots));
                insertedSourceCount++;
                continue;
            }
            if (!matchesMaterializedState(sourceFacts, contentHash, expectedPivots)) {
                changedSources.put(sourceKey, new SourceMaterialization(sourceRow, contentHash, expectedPivots));
                updatedSourceCount++;
            } else {
                unchangedSourceCount++;
            }
        }

        Set<String> removedSourceKeys = new HashSet<>(existingFactMap.keySet());
        removedSourceKeys.removeAll(currentSourceMap.keySet());

        Set<Long> deleteIdSet = new HashSet<>();
        for (String sourceKey : changedSources.keySet()) {
            collectFactIds(existingFactMap.get(sourceKey), deleteIdSet);
        }
        for (String sourceKey : removedSourceKeys) {
            collectFactIds(existingFactMap.get(sourceKey), deleteIdSet);
        }

        // 先找出未变来源上需刷新的母卷透视快照，避免状态变了但表格仍读到旧 pivot_json。
        Set<String> refreshPivotHashes = new HashSet<>();
        for (QmsYieldDailyFactDO fact : existingFacts) {
            if (deleteIdSet.contains(fact.getId())) {
                continue;
            }
            PivotSnapshot currentPivot = pivotSnapshotMap.get(fact.getPivotKeyHash());
            if (currentPivot != null && !Objects.equals(currentPivot.json(), fact.getPivotJson())) {
                refreshPivotHashes.add(currentPivot.hash());
            }
        }

        hardDeleteFacts(tenantId, new ArrayList<>(deleteIdSet));

        List<QmsYieldDailyFactDO> facts = new ArrayList<>();
        for (SourceMaterialization source : changedSources.values()) {
            for (PivotSnapshot pivot : source.pivots()) {
                facts.add(toDailyFact(source.row(), source.contentHash(), pivot.key(), pivot.hash(), pivot.json(),
                        tenantId, syncBatchNo, syncTime, syncTime.toLocalDate()));
            }
        }
        if (!facts.isEmpty()) {
            qmsYieldDailyFactMapper.insertBatch(facts, INSERT_BATCH_SIZE);
        }

        for (String pivotHash : refreshPivotHashes) {
            PivotSnapshot pivot = pivotSnapshotMap.get(pivotHash);
            qmsYieldDailyFactMapper.updatePivotSnapshot(tenantId, pivot.hash(), pivot.key(), pivot.json(),
                    syncBatchNo, syncTime);
        }

        LocalDateTime completedAt = LocalDateTime.now();
        LocalDate firstStatDate = currentSourceMap.values().stream()
                .map(QmsYieldAnalysisSourceRow::getConfirmTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate latestStatDate = currentSourceMap.values().stream()
                .map(QmsYieldAnalysisSourceRow::getConfirmTime)
                .filter(Objects::nonNull)
                .map(LocalDateTime::toLocalDate)
                .max(LocalDate::compareTo)
                .orElse(null);
        return QmsYieldAnalysisV2SyncRespVO.builder()
                .startDate(firstStatDate)
                .endDate(latestStatDate)
                .sourceRecordCount(currentSourceMap.size())
                .alreadySettledRecordCount(unchangedSourceCount)
                .insertedSourceCount(insertedSourceCount)
                .updatedSourceCount(updatedSourceCount)
                .removedSourceCount(removedSourceKeys.size())
                .snapshotRecordCount(facts.size())
                .pivotRowCount(pivotSnapshots.size())
                .refreshedPivotRowCount(refreshPivotHashes.size())
                .durationMs(System.currentTimeMillis() - startedAt)
                .syncTime(completedAt)
                .build();
    }

    private String buildSourceKey(QmsYieldAnalysisSourceRow row) {
        return normalizeProcessCode(row.getProcessCode()) + "|" + safe(row.getSourceTable()) + "|" + row.getSourceId();
    }

    private String buildSourceKey(QmsYieldDailyFactDO fact) {
        return normalizeProcessCode(fact.getProcessCode()) + "|" + safe(fact.getSourceTable()) + "|" + fact.getSourceId();
    }

    private List<PivotSnapshot> buildPivotSnapshots(List<QmsMotherRollGoodStatisticsRespVO> motherRows) {
        Map<String, PivotSnapshot> snapshots = new LinkedHashMap<>();
        for (QmsMotherRollGoodStatisticsRespVO motherRow : motherRows) {
            String pivotKey = buildPivotKey(motherRow);
            String pivotHash = DigestUtil.md5Hex(pivotKey);
            snapshots.put(pivotHash, new PivotSnapshot(motherRow, pivotKey, pivotHash, writePivotJson(motherRow)));
        }
        return new ArrayList<>(snapshots.values());
    }

    private List<PivotSnapshot> resolvePivotSnapshots(QmsYieldAnalysisSourceRow sourceRow,
                                                       List<PivotSnapshot> pivotSnapshots) {
        List<PivotSnapshot> matched = new ArrayList<>();
        for (PivotSnapshot snapshot : pivotSnapshots) {
            if (matchesMotherRow(snapshot.row(), sourceRow)) {
                matched.add(snapshot);
            }
        }
        if (!matched.isEmpty()) {
            return matched;
        }
        String pivotKey = "SOURCE_ONLY|" + safe(sourceRow.getProcessCode()) + "|"
                + safe(sourceRow.getSourceTable()) + "|" + sourceRow.getSourceId();
        return List.of(new PivotSnapshot(null, pivotKey, DigestUtil.md5Hex(pivotKey), null));
    }

    private boolean matchesMaterializedState(List<QmsYieldDailyFactDO> facts,
                                             String sourceContentHash,
                                             List<PivotSnapshot> expectedPivots) {
        Set<String> expectedPivotHashes = new HashSet<>();
        for (PivotSnapshot pivot : expectedPivots) {
            expectedPivotHashes.add(pivot.hash());
        }
        Set<String> actualPivotHashes = new HashSet<>();
        for (QmsYieldDailyFactDO fact : facts) {
            if (!Objects.equals(sourceContentHash, fact.getSourceContentHash())) {
                return false;
            }
            actualPivotHashes.add(fact.getPivotKeyHash());
        }
        return facts.size() == actualPivotHashes.size() && expectedPivotHashes.equals(actualPivotHashes);
    }

    private void collectFactIds(List<QmsYieldDailyFactDO> facts, Set<Long> target) {
        if (facts == null) {
            return;
        }
        for (QmsYieldDailyFactDO fact : facts) {
            if (fact.getId() != null) {
                target.add(fact.getId());
            }
        }
    }

    private void hardDeleteFacts(Long tenantId, List<Long> ids) {
        for (int start = 0; start < ids.size(); start += INSERT_BATCH_SIZE) {
            int end = Math.min(start + INSERT_BATCH_SIZE, ids.size());
            qmsYieldDailyFactMapper.hardDeleteByIds(tenantId, ids.subList(start, end));
        }
    }

    private String buildSourceContentHash(QmsYieldAnalysisSourceRow row) {
        StringBuilder fingerprint = new StringBuilder(1024);
        appendFingerprint(fingerprint, normalizeProcessCode(row.getProcessCode()));
        appendFingerprint(fingerprint, row.getProcessName());
        appendFingerprint(fingerprint, row.getSourceTable());
        appendFingerprint(fingerprint, row.getSourceId());
        appendFingerprint(fingerprint, row.getPlanId());
        appendFingerprint(fingerprint, row.getPlanNo());
        appendFingerprint(fingerprint, row.getPlanOperationId());
        appendFingerprint(fingerprint, row.getMotherRollNo());
        appendFingerprint(fingerprint, row.getSegmentNo());
        appendFingerprint(fingerprint, row.getPieceNo());
        appendFingerprint(fingerprint, row.getMaterialCode());
        appendFingerprint(fingerprint, row.getMaterialName());
        appendFingerprint(fingerprint, row.getModelCode());
        appendFingerprint(fingerprint, row.getInputQty());
        appendFingerprint(fingerprint, row.getOutputGoodQty());
        appendFingerprint(fingerprint, row.getOutputNgQty());
        appendFingerprint(fingerprint, row.getSelfCheck());
        appendFingerprint(fingerprint, row.getDefectCode());
        appendFingerprint(fingerprint, row.getVisualResultJson());
        appendFingerprint(fingerprint, row.getExtraJson());
        appendFingerprint(fingerprint, row.getSubmissionResult());
        appendFingerprint(fingerprint, row.getReportStatus());
        appendFingerprint(fingerprint, row.getConfirmTime());
        appendFingerprint(fingerprint, row.getInspectionId());
        appendFingerprint(fingerprint, row.getInspectionNo());
        appendFingerprint(fingerprint, row.getInspectionSourceType());
        return DigestUtil.md5Hex(fingerprint.toString());
    }

    private void appendFingerprint(StringBuilder target, Object value) {
        String text = value == null ? "<NULL>" : String.valueOf(value);
        target.append(text.length()).append(':').append(text).append('|');
    }

    private List<QmsMotherRollGoodStatisticsRespVO> readPivotRows(QmsYieldAnalysisV2PageReqVO reqVO,
                                                                   DateRange dateRange,
                                                                   Integer offset,
                                                                   Integer limit) {
        List<String> jsonRows = qmsYieldDailyFactMapper.selectPivotJsonList(
                TenantContextHolder.getTenantId(), dateRange.startDate(), dateRange.endDate(), reqVO, offset, limit);
        if (jsonRows == null || jsonRows.isEmpty()) {
            return List.of();
        }
        return jsonRows.stream().map(this::readPivotJson).toList();
    }

    private QmsMotherRollGoodStatisticsRespVO readPivotJson(String json) {
        try {
            QmsMotherRollGoodStatisticsRespVO row = objectMapper.readValue(json, QmsMotherRollGoodStatisticsRespVO.class);
            sanitizeInvalidReportTimes(row);
            return row;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("良品率分析日结快照解析失败，请重新同步日结数据", ex);
        }
    }

    private void sanitizeInvalidReportTimes(QmsMotherRollGoodStatisticsRespVO row) {
        if (row == null || row.getStages() == null) {
            return;
        }
        for (QmsMotherRollGoodStatisticsStageRespVO stage : row.getStages().values()) {
            if (stage == null) {
                continue;
            }
            if (isInvalidBusinessTime(stage.getLastReportTime())) {
                stage.setLastReportTime(null);
            }
            if (stage.getPieceDetails() == null) {
                continue;
            }
            for (QmsMotherRollGoodStatisticsPieceRespVO piece : stage.getPieceDetails()) {
                if (piece != null && isInvalidBusinessTime(piece.getLastReportTime())) {
                    piece.setLastReportTime(null);
                }
            }
        }
    }

    private boolean isInvalidBusinessTime(LocalDateTime value) {
        return value != null && value.isBefore(MIN_VALID_BUSINESS_TIME);
    }

    private String writePivotJson(QmsMotherRollGoodStatisticsRespVO row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("母卷批次良品统计快照序列化失败", ex);
        }
    }

    private QmsYieldDailyFactDO toDailyFact(QmsYieldAnalysisSourceRow source,
                                            String sourceContentHash,
                                            String pivotKey,
                                            String pivotKeyHash,
                                            String pivotJson,
                                            Long tenantId,
                                            String syncBatchNo,
                                            LocalDateTime syncTime,
                                            LocalDate fallbackDate) {
        QmsYieldDailyFactDO fact = new QmsYieldDailyFactDO();
        fact.setStatDate(source.getConfirmTime() == null ? fallbackDate : source.getConfirmTime().toLocalDate());
        fact.setProcessCode(normalizeProcessCode(source.getProcessCode()));
        fact.setProcessName(source.getProcessName());
        fact.setSourceTable(source.getSourceTable());
        fact.setSourceId(source.getSourceId());
        fact.setPlanId(source.getPlanId());
        fact.setPlanNo(source.getPlanNo());
        fact.setPlanOperationId(source.getPlanOperationId());
        fact.setMotherRollNo(source.getMotherRollNo());
        fact.setSegmentNo(source.getSegmentNo());
        fact.setPieceNo(source.getPieceNo());
        fact.setMaterialCode(source.getMaterialCode());
        fact.setMaterialName(source.getMaterialName());
        fact.setModelCode(source.getModelCode());
        fact.setInputQty(source.getInputQty());
        fact.setOutputGoodQty(source.getOutputGoodQty());
        fact.setOutputNgQty(source.getOutputNgQty());
        fact.setSelfCheck(source.getSelfCheck());
        fact.setDefectCode(limit(source.getDefectCode(), 255));
        fact.setVisualResultJson(source.getVisualResultJson());
        fact.setExtraJson(source.getExtraJson());
        fact.setSubmissionResult(source.getSubmissionResult());
        fact.setReportStatus(source.getReportStatus());
        fact.setConfirmTime(source.getConfirmTime());
        fact.setInspectionId(source.getInspectionId());
        fact.setInspectionNo(source.getInspectionNo());
        fact.setInspectionSourceType(source.getInspectionSourceType());
        fact.setSourceContentHash(sourceContentHash);
        fact.setPivotKey(limit(pivotKey, 500));
        fact.setPivotKeyHash(pivotKeyHash);
        fact.setPivotJson(pivotJson);
        fact.setSyncBatchNo(syncBatchNo);
        fact.setSyncTime(syncTime);
        fact.setDeleted(false);
        fact.setTenantId(tenantId);
        return fact;
    }

    private String buildPivotKey(QmsMotherRollGoodStatisticsRespVO row) {
        return firstNotBlank(row.getPivotRowKey(),
                safe(row.getId()) + "|" + safe(row.getPlanNo()) + "|" + safe(row.getMotherRollBatchNo())
                        + "|" + safe(row.getSegmentBatchNo()) + "|" + safe(row.getActualModelCode()));
    }

    private QmsYieldAnalysisReqVO toYieldAnalysisReqVO(QmsYieldAnalysisV2PageReqVO source) {
        QmsYieldAnalysisReqVO target = new QmsYieldAnalysisReqVO();
        target.setStartDate(source.getActualReportDateStart());
        target.setEndDate(source.getActualReportDateEnd());
        target.setProcessCode(normalizeProcessCode(source.getProcessCode()));
        target.setPlanNo(source.getPlanNo());
        target.setMotherRollNo(source.getMotherRollBatchNo());
        target.setSegmentNo(source.getMotherSegmentBatchNo());
        target.setPieceNo(source.getPieceNo());
        target.setModelCode(source.getModelCode());
        target.setMaterialKeyword(source.getMaterialKeyword());
        target.setMetricKey(source.getMetricKey());
        target.setPageNo(source.getPageNo());
        target.setPageSize(source.getPageSize());
        return target;
    }

    private DateRange resolveDateRange(QmsYieldAnalysisV2PageReqVO reqVO) {
        LocalDate defaultDate = LocalDate.now().minusDays(1);
        LocalDate startDate = reqVO.getActualReportDateStart() == null
                ? defaultDate : reqVO.getActualReportDateStart();
        LocalDate endDate = reqVO.getActualReportDateEnd() == null
                ? startDate : reqVO.getActualReportDateEnd();
        if (endDate.isBefore(startDate)) {
            endDate = startDate;
        }
        return new DateRange(startDate, endDate, startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay());
    }

    private boolean matchesMotherRow(QmsMotherRollGoodStatisticsRespVO row, QmsYieldAnalysisSourceRow actual) {
        boolean samePlan = Objects.equals(row.getId(), actual.getPlanId())
                || equalsIgnoreCase(row.getPlanNo(), actual.getPlanNo());
        boolean sameMother = equalsBatch(row.getMotherRollBatchNo(), actual.getMotherRollNo());
        if (!samePlan && !sameMother) {
            return false;
        }
        if (isPlanLevelProcess(actual.getProcessCode())) {
            return true;
        }
        String actualSegment = firstNotBlank(normalizeBatch(actual.getSegmentNo()),
                normalizeBatch(actual.getPieceNo()));
        if (StrUtil.isBlank(actualSegment)) {
            return true;
        }
        String rowSegments = safe(row.getSegmentBatchNo());
        for (String rowSegment : rowSegments.split("[,，;；\\s]+")) {
            if (equalsBatch(rowSegment, actualSegment)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlanLevelProcess(String processCode) {
        String normalized = normalizeProcessCode(processCode);
        return "FORMULA".equals(normalized) || "WET".equals(normalized);
    }

    private void enrichDetailReportInfo(List<QmsYieldAnalysisDetailRespVO> details,
                                        List<QmsMotherRollGoodStatisticsRespVO> pivotRows) {
        if (details == null || details.isEmpty() || pivotRows == null || pivotRows.isEmpty()) {
            return;
        }
        for (QmsYieldAnalysisDetailRespVO detail : details) {
            QmsMotherRollGoodStatisticsRespVO fallbackRow = null;
            QmsMotherRollGoodStatisticsPieceRespVO matchedPiece = null;
            for (QmsMotherRollGoodStatisticsRespVO pivotRow : pivotRows) {
                if (!matchesDetailPivot(pivotRow, detail)) {
                    continue;
                }
                if (fallbackRow == null) {
                    fallbackRow = pivotRow;
                }
                QmsMotherRollGoodStatisticsStageRespVO stage = resolveDetailStage(pivotRow, detail.getProcessCode());
                matchedPiece = findDetailPiece(stage, detail.getPieceNo());
                if (matchedPiece != null) {
                    fallbackRow = pivotRow;
                    break;
                }
            }
            if (fallbackRow == null) {
                continue;
            }
            String plannedSize = firstNotBlank(
                    matchedPiece == null ? null : matchedPiece.getActualSizeSpec(),
                    fallbackRow.getActualSizeSpec(), fallbackRow.getSizeName(), fallbackRow.getSizeSpec(), "-");
            detail.setSizeSpec(plannedSize);
            detail.setActualSizeSpec(firstNotBlank(
                    matchedPiece == null ? null : matchedPiece.getOutputActualSizeSpec(), plannedSize));
            detail.setActualReportModelCode(firstNotBlank(
                    matchedPiece == null ? null : matchedPiece.getActualModelCode(),
                    fallbackRow.getActualModelCode(), fallbackRow.getModelCode(), detail.getModelCode(), "-"));
        }
    }

    private boolean matchesDetailPivot(QmsMotherRollGoodStatisticsRespVO row,
                                       QmsYieldAnalysisDetailRespVO detail) {
        if (StrUtil.isNotBlank(detail.getMotherRollNo())
                && StrUtil.isNotBlank(row.getMotherRollBatchNo())
                && !equalsBatch(row.getMotherRollBatchNo(), detail.getMotherRollNo())) {
            return false;
        }
        if (isPlanLevelProcess(detail.getProcessCode()) || StrUtil.isBlank(detail.getSegmentNo())) {
            return true;
        }
        for (String segment : safe(row.getSegmentBatchNo()).split("[,，;；\\s]+")) {
            if (equalsBatch(segment, detail.getSegmentNo())) {
                return true;
            }
        }
        return false;
    }

    private QmsMotherRollGoodStatisticsStageRespVO resolveDetailStage(
            QmsMotherRollGoodStatisticsRespVO row, String processCode) {
        if (row.getStages() == null || row.getStages().isEmpty()) {
            return null;
        }
        String normalized = normalizeProcessCode(processCode);
        String stageCode = switch (normalized) {
            case "ROUGH_GRINDING" -> "GRINDING";
            case "FINAL_INSPECTION" -> "SHIPPING_INSPECTION";
            default -> normalized;
        };
        return row.getStages().get(stageCode);
    }

    private QmsMotherRollGoodStatisticsPieceRespVO findDetailPiece(
            QmsMotherRollGoodStatisticsStageRespVO stage, String pieceNo) {
        if (stage == null || stage.getPieceDetails() == null || StrUtil.isBlank(pieceNo)) {
            return null;
        }
        return stage.getPieceDetails().stream()
                .filter(piece -> equalsIgnoreCase(piece.getPieceNo(), pieceNo)
                        || equalsIgnoreCase(piece.getOutputBatchNo(), pieceNo)
                        || equalsIgnoreCase(piece.getSourceBatchNo(), pieceNo))
                .findFirst()
                .orElse(null);
    }

    private String normalizeProcessCode(String value) {
        String normalized = StrUtil.blankToDefault(value, "ALL").trim().toUpperCase(Locale.ROOT);
        if ("GRINDING".equals(normalized)) {
            return "ROUGH_GRINDING";
        }
        if ("SHIPPING_INSPECTION".equals(normalized)) {
            return "FINAL_INSPECTION";
        }
        return normalized;
    }

    private String normalizeBatch(String value) {
        String normalized = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(normalized)) {
            return null;
        }
        Matcher adhesiveMatcher = ADHESIVE_SEGMENT_PATTERN.matcher(normalized);
        if (adhesiveMatcher.matches()) {
            normalized = adhesiveMatcher.group(1);
        }
        Matcher pieceMatcher = PIECE_PATTERN.matcher(normalized);
        return pieceMatcher.matches() ? pieceMatcher.group(1) : normalized;
    }

    private boolean equalsBatch(String first, String second) {
        String left = normalizeBatch(first);
        String right = normalizeBatch(second);
        return StrUtil.isNotBlank(left) && StrUtil.isNotBlank(right) && left.equalsIgnoreCase(right);
    }

    private boolean equalsIgnoreCase(String first, String second) {
        return StrUtil.isNotBlank(first) && StrUtil.isNotBlank(second) && first.trim().equalsIgnoreCase(second.trim());
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String limit(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private record DateRange(LocalDate startDate, LocalDate endDate,
                             LocalDateTime startTime, LocalDateTime endExclusiveTime) {
    }

    private record PivotSnapshot(QmsMotherRollGoodStatisticsRespVO row, String key, String hash, String json) {
    }

    private record SourceMaterialization(QmsYieldAnalysisSourceRow row, String contentHash,
                                         List<PivotSnapshot> pivots) {
    }
}
