package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailyCompareRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailySyncRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotDailySyncStatusRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanProcessPivotDailyFactDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanProcessPivotDailyFactMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class HcPlanProcessPivotDailyServiceImpl implements HcPlanProcessPivotDailyService {

    private static final int INSERT_BATCH_SIZE = 500;
    private static final int COMPARE_SAMPLE_LIMIT = 8;
    private static final List<String> SYNC_PLAN_STATUSES = List.of(
            "DRAFT", "RELEASED", "PAUSED", "CLOSED", "CANCELLED", "CANCELED");

    @Resource
    private HcPlanOrderService hcPlanOrderService;
    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanProcessPivotDailyFactMapper hcPlanProcessPivotDailyFactMapper;
    @Resource
    private ObjectMapper objectMapper;

    @Override
    public PageResult<HcPlanProcessPivotRespVO> getPage(HcPlanProcessPivotPageReqVO pageReqVO) {
        HcPlanProcessPivotPageReqVO queryReq = normalizeQueryReq(pageReqVO);
        int pageNo = pageReqVO.getPageNo() == null ? 1 : Math.max(pageReqVO.getPageNo(), 1);
        int pageSize = pageReqVO.getPageSize() == null ? 20 : pageReqVO.getPageSize();
        Integer limit = PageParam.PAGE_SIZE_NONE.equals(pageSize) ? null : Math.max(pageSize, 1);
        int offset = limit == null ? 0 : (pageNo - 1) * limit;
        List<HcPlanProcessPivotRespVO> rows = readPivotRows(queryReq, offset, limit);
        Long total = limit == null ? (long) rows.size() : hcPlanProcessPivotDailyFactMapper.countPivotRows(
                TenantContextHolder.getTenantId(), queryReq);
        return new PageResult<>(rows, total == null ? 0L : total);
    }

    @Override
    public List<HcPlanProcessPivotRespVO> getList(HcPlanProcessPivotPageReqVO pageReqVO) {
        return readPivotRows(normalizeQueryReq(pageReqVO), 0, null);
    }

    @Override
    public HcPlanProcessPivotDailySyncStatusRespVO getSyncStatus() {
        HcPlanProcessPivotDailySyncStatusRespVO status = hcPlanProcessPivotDailyFactMapper.selectSyncStatus(
                TenantContextHolder.getTenantId());
        if (status == null) {
            status = new HcPlanProcessPivotDailySyncStatusRespVO();
            status.setSettledRowCount(0L);
        }
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcPlanProcessPivotDailySyncRespVO syncLatestSnapshot() {
        long startedAt = System.currentTimeMillis();
        Long tenantId = TenantContextHolder.getTenantId();
        LocalDateTime syncTime = LocalDateTime.now();
        String syncBatchNo = syncTime.toLocalDate() + "-" + IdUtil.fastSimpleUUID();

        Map<String, CurrentSnapshot> currentSnapshotMap = buildCurrentSnapshotMap(buildSyncReq());
        List<HcPlanProcessPivotDailyFactDO> existingFacts = hcPlanProcessPivotDailyFactMapper.selectActiveFacts(tenantId);
        if (existingFacts == null) {
            existingFacts = List.of();
        }
        Map<String, HcPlanProcessPivotDailyFactDO> existingFactMap = existingFacts.stream()
                .collect(Collectors.toMap(HcPlanProcessPivotDailyFactDO::getPivotKeyHash,
                        fact -> fact, (a, b) -> a, LinkedHashMap::new));

        int insertedRowCount = 0;
        int updatedRowCount = 0;
        int unchangedRowCount = 0;
        Set<String> currentHashes = new HashSet<>();
        List<Long> deleteIds = new ArrayList<>();
        List<HcPlanProcessPivotDailyFactDO> factsToInsert = new ArrayList<>();
        for (CurrentSnapshot snapshot : currentSnapshotMap.values()) {
            currentHashes.add(snapshot.pivotKeyHash());
            HcPlanProcessPivotDailyFactDO existing = existingFactMap.get(snapshot.pivotKeyHash());
            if (existing == null) {
                insertedRowCount++;
                factsToInsert.add(toFact(snapshot, tenantId, syncBatchNo, syncTime));
                continue;
            }
            if (!Objects.equals(existing.getSourceContentHash(), snapshot.sourceContentHash())) {
                updatedRowCount++;
                deleteIds.add(existing.getId());
                factsToInsert.add(toFact(snapshot, tenantId, syncBatchNo, syncTime));
            } else {
                unchangedRowCount++;
            }
        }

        for (HcPlanProcessPivotDailyFactDO existing : existingFacts) {
            if (!currentHashes.contains(existing.getPivotKeyHash()) && existing.getId() != null) {
                deleteIds.add(existing.getId());
            }
        }
        hardDeleteFacts(tenantId, deleteIds);
        if (!factsToInsert.isEmpty()) {
            hcPlanProcessPivotDailyFactMapper.insertBatch(factsToInsert, INSERT_BATCH_SIZE);
        }

        LocalDateTime completedAt = LocalDateTime.now();
        LocalDate startDate = currentSnapshotMap.values().stream()
                .map(CurrentSnapshot::statDate)
                .filter(Objects::nonNull)
                .min(LocalDate::compareTo)
                .orElse(null);
        LocalDate endDate = currentSnapshotMap.values().stream()
                .map(CurrentSnapshot::statDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        return HcPlanProcessPivotDailySyncRespVO.builder()
                .startDate(startDate)
                .endDate(endDate)
                .sourceRowCount(currentSnapshotMap.size())
                .alreadySettledRowCount(unchangedRowCount)
                .insertedRowCount(insertedRowCount)
                .updatedRowCount(updatedRowCount)
                .removedRowCount(deleteIds.size() - updatedRowCount)
                .snapshotRowCount(factsToInsert.size())
                .durationMs(System.currentTimeMillis() - startedAt)
                .syncTime(completedAt)
                .build();
    }

    @Override
    public HcPlanProcessPivotDailyCompareRespVO compareWithSource(HcPlanProcessPivotPageReqVO pageReqVO) {
        HcPlanProcessPivotPageReqVO queryReq = normalizeQueryReq(pageReqVO);
        Map<String, CurrentSnapshot> sourceMap = buildCurrentSnapshotMap(queryReq);
        List<HcPlanProcessPivotDailyFactMapper.PivotFingerprint> fingerprints =
                hcPlanProcessPivotDailyFactMapper.selectFingerprints(TenantContextHolder.getTenantId(), queryReq);
        Map<String, String> snapshotMap = new LinkedHashMap<>();
        if (fingerprints != null) {
            for (HcPlanProcessPivotDailyFactMapper.PivotFingerprint fingerprint : fingerprints) {
                snapshotMap.put(fingerprint.pivotKeyHash(), fingerprint.sourceContentHash());
            }
        }

        int matchedCount = 0;
        int missingCount = 0;
        int staleCount = 0;
        List<String> sampleMessages = new ArrayList<>();
        for (CurrentSnapshot source : sourceMap.values()) {
            String snapshotHash = snapshotMap.remove(source.pivotKeyHash());
            if (snapshotHash == null) {
                missingCount++;
                addCompareSample(sampleMessages, "日结缺失：" + source.label());
                continue;
            }
            if (!Objects.equals(snapshotHash, source.sourceContentHash())) {
                staleCount++;
                addCompareSample(sampleMessages, "日结过期：" + source.label());
                continue;
            }
            matchedCount++;
        }
        int extraCount = snapshotMap.size();
        for (String extraHash : snapshotMap.keySet()) {
            addCompareSample(sampleMessages, "日结多余：pivotKeyHash=" + extraHash);
        }

        return HcPlanProcessPivotDailyCompareRespVO.builder()
                .sourceRowCount(sourceMap.size())
                .snapshotRowCount(matchedCount + staleCount + extraCount)
                .matchedRowCount(matchedCount)
                .missingSnapshotCount(missingCount)
                .staleSnapshotCount(staleCount)
                .extraSnapshotCount(extraCount)
                .sampleMessages(sampleMessages)
                .checkedTime(LocalDateTime.now())
                .build();
    }

    private List<HcPlanProcessPivotRespVO> readPivotRows(HcPlanProcessPivotPageReqVO reqVO,
                                                          Integer offset,
                                                          Integer limit) {
        List<String> jsonRows = hcPlanProcessPivotDailyFactMapper.selectPivotJsonList(
                TenantContextHolder.getTenantId(), reqVO, offset, limit);
        if (jsonRows == null || jsonRows.isEmpty()) {
            return List.of();
        }
        return jsonRows.stream().map(this::readPivotJson).toList();
    }

    private HcPlanProcessPivotRespVO readPivotJson(String json) {
        try {
            return objectMapper.readValue(json, HcPlanProcessPivotRespVO.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生产进度日结快照解析失败，请重新同步日结数据", ex);
        }
    }

    private Map<String, CurrentSnapshot> buildCurrentSnapshotMap(HcPlanProcessPivotPageReqVO reqVO) {
        List<HcPlanProcessPivotRespVO> rows = hcPlanOrderService.getPlanProcessPivotList(reqVO);
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        Map<Long, HcPlanOrderDO> planMap = selectPlanMap(rows);
        Map<String, CurrentSnapshot> snapshotMap = new LinkedHashMap<>();
        for (HcPlanProcessPivotRespVO row : rows) {
            String pivotKey = buildPivotKey(row);
            String pivotKeyHash = DigestUtil.md5Hex(pivotKey);
            String pivotJson = writePivotJson(row);
            HcPlanOrderDO plan = row.getId() == null ? null : planMap.get(row.getId());
            String sourceContentHash = buildSourceContentHash(row, plan, pivotJson);
            snapshotMap.put(pivotKeyHash, new CurrentSnapshot(
                    row, plan, pivotKey, pivotKeyHash, pivotJson, sourceContentHash, resolveStatDate(row)));
        }
        return snapshotMap;
    }

    private Map<Long, HcPlanOrderDO> selectPlanMap(List<HcPlanProcessPivotRespVO> rows) {
        Set<Long> planIds = rows.stream()
                .map(HcPlanProcessPivotRespVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (planIds.isEmpty()) {
            return Map.of();
        }
        List<HcPlanOrderDO> plans = hcPlanOrderMapper.selectBatchIds(planIds);
        if (plans == null || plans.isEmpty()) {
            return Map.of();
        }
        return plans.stream().collect(Collectors.toMap(HcPlanOrderDO::getId, item -> item,
                (a, b) -> a, LinkedHashMap::new));
    }

    private HcPlanProcessPivotDailyFactDO toFact(CurrentSnapshot snapshot,
                                                 Long tenantId,
                                                 String syncBatchNo,
                                                 LocalDateTime syncTime) {
        HcPlanProcessPivotRespVO row = snapshot.row();
        HcPlanOrderDO plan = snapshot.plan();
        HcPlanProcessPivotDailyFactDO fact = new HcPlanProcessPivotDailyFactDO();
        fact.setStatDate(snapshot.statDate());
        fact.setPlanId(row.getId());
        fact.setPlanNo(row.getPlanNo());
        fact.setPlanStatus(row.getPlanStatus());
        fact.setPlanMode(plan == null ? null : plan.getPlanMode());
        fact.setSourceType(plan == null ? null : plan.getSourceType());
        fact.setSalesOrderNo(plan == null ? null : plan.getSalesOrderNo());
        fact.setSalesOrderErpNo(plan == null ? null : plan.getSalesOrderErpNo());
        fact.setPlanDate(row.getPlanDate());
        fact.setProductionStartDate(row.getProductionStartDate());
        fact.setProductionEndDate(row.getProductionEndDate());
        fact.setMaterialCode(row.getMaterialCode());
        fact.setMaterialName(row.getMaterialName());
        fact.setMotherMaterialCode(row.getMotherMaterialCode());
        fact.setMotherMaterialName(row.getMotherMaterialName());
        fact.setModelCode(row.getModelCode());
        fact.setModelName(row.getModelName());
        fact.setMotherModelCode(row.getMotherModelCode());
        fact.setMotherModelName(row.getMotherModelName());
        fact.setProdType(plan == null ? null : plan.getProdType());
        fact.setCategoryCode(plan == null ? null : plan.getCategoryCode());
        fact.setRecipeCode(plan == null ? null : plan.getRecipeCode());
        fact.setRouteCode(plan == null ? null : plan.getRouteCode());
        fact.setRouteName(plan == null ? null : plan.getRouteName());
        fact.setSizeSpec(row.getSizeSpec());
        fact.setSizeName(row.getSizeName());
        fact.setTargetQty(row.getTargetQty());
        fact.setNetPlanQty(row.getNetPlanQty());
        fact.setTargetUom(row.getTargetUom());
        fact.setBatchNo(row.getBatchNo());
        fact.setProductionBatchNo(row.getProductionBatchNo());
        fact.setParentProductionBatchNo(row.getParentProductionBatchNo());
        fact.setMotherRollBatchNo(row.getMotherRollBatchNo());
        fact.setSegmentBatchNo(row.getSegmentBatchNo());
        fact.setActualModelCode(row.getActualModelCode());
        fact.setActualSizeSpec(row.getActualSizeSpec());
        fact.setPostProcessFlag(row.getPostProcessFlag());
        fact.setTotalDefectQty(row.getTotalDefectQty());
        fact.setLatestReportTime(row.getLatestReportTime());
        fact.setSourceCreateTime(plan == null ? null : plan.getCreateTime());
        fact.setSourceUpdateTime(plan == null ? null : plan.getUpdateTime());
        fact.setPivotKey(limit(snapshot.pivotKey(), 500));
        fact.setPivotKeyHash(snapshot.pivotKeyHash());
        fact.setSourceContentHash(snapshot.sourceContentHash());
        fact.setPivotJson(snapshot.pivotJson());
        fact.setSyncBatchNo(syncBatchNo);
        fact.setSyncTime(syncTime);
        fact.setDeleted(false);
        fact.setTenantId(tenantId);
        return fact;
    }

    private String buildPivotKey(HcPlanProcessPivotRespVO row) {
        return firstNotBlank(row.getPivotRowKey(),
                safe(row.getId()) + "|" + safe(row.getPlanNo()) + "|" + safe(row.getMotherRollBatchNo())
                        + "|" + safe(row.getSegmentBatchNo()) + "|" + safe(row.getActualModelCode())
                        + "|" + safe(row.getActualSizeSpec()));
    }

    private String buildSourceContentHash(HcPlanProcessPivotRespVO row, HcPlanOrderDO plan, String pivotJson) {
        StringBuilder fingerprint = new StringBuilder(4096);
        appendFingerprint(fingerprint, pivotJson);
        appendFingerprint(fingerprint, plan == null ? null : plan.getPlanMode());
        appendFingerprint(fingerprint, plan == null ? null : plan.getSourceType());
        appendFingerprint(fingerprint, plan == null ? null : plan.getSalesOrderNo());
        appendFingerprint(fingerprint, plan == null ? null : plan.getSalesOrderErpNo());
        appendFingerprint(fingerprint, plan == null ? null : plan.getProdType());
        appendFingerprint(fingerprint, plan == null ? null : plan.getCategoryCode());
        appendFingerprint(fingerprint, plan == null ? null : plan.getRecipeCode());
        appendFingerprint(fingerprint, plan == null ? null : plan.getRouteCode());
        appendFingerprint(fingerprint, plan == null ? null : plan.getRouteName());
        appendFingerprint(fingerprint, plan == null ? null : plan.getCreateTime());
        appendFingerprint(fingerprint, plan == null ? null : plan.getUpdateTime());
        appendFingerprint(fingerprint, row.getSegmentBatchNo());
        appendFingerprint(fingerprint, row.getLatestReportTime());
        return DigestUtil.md5Hex(fingerprint.toString());
    }

    private void appendFingerprint(StringBuilder target, Object value) {
        String text = value == null ? "<NULL>" : String.valueOf(value);
        target.append(text.length()).append(':').append(text).append('|');
    }

    private String writePivotJson(HcPlanProcessPivotRespVO row) {
        try {
            return objectMapper.writeValueAsString(row);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("生产进度日结快照序列化失败", ex);
        }
    }

    private HcPlanProcessPivotPageReqVO buildSyncReq() {
        HcPlanProcessPivotPageReqVO reqVO = new HcPlanProcessPivotPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        reqVO.setPlanStatuses(SYNC_PLAN_STATUSES);
        return reqVO;
    }

    private HcPlanProcessPivotPageReqVO normalizeQueryReq(HcPlanProcessPivotPageReqVO source) {
        HcPlanProcessPivotPageReqVO target = BeanUtils.toBean(source, HcPlanProcessPivotPageReqVO.class);
        if (target == null) {
            target = new HcPlanProcessPivotPageReqVO();
        }
        target.setPlanStatuses(expandPlanStatusAliases(target.getPlanStatuses()));
        return target;
    }

    private List<String> expandPlanStatusAliases(Collection<String> planStatuses) {
        if (planStatuses == null || planStatuses.isEmpty()) {
            return null;
        }
        LinkedHashSet<String> statuses = new LinkedHashSet<>();
        for (String status : planStatuses) {
            if (StrUtil.isBlank(status)) {
                continue;
            }
            String normalized = status.trim().toUpperCase();
            statuses.add(normalized);
            if ("CANCELLED".equals(normalized)) {
                statuses.add("CANCELED");
            } else if ("CANCELED".equals(normalized)) {
                statuses.add("CANCELLED");
            }
        }
        return statuses.isEmpty() ? null : new ArrayList<>(statuses);
    }

    private void hardDeleteFacts(Long tenantId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (int start = 0; start < ids.size(); start += INSERT_BATCH_SIZE) {
            int end = Math.min(start + INSERT_BATCH_SIZE, ids.size());
            hcPlanProcessPivotDailyFactMapper.hardDeleteByIds(tenantId, ids.subList(start, end));
        }
    }

    private LocalDate resolveStatDate(HcPlanProcessPivotRespVO row) {
        if (row.getProductionStartDate() != null) {
            return row.getProductionStartDate();
        }
        if (row.getPlanDate() != null) {
            return row.getPlanDate();
        }
        if (row.getLatestReportTime() != null) {
            return row.getLatestReportTime().toLocalDate();
        }
        return LocalDate.now();
    }

    private void addCompareSample(List<String> samples, String message) {
        if (samples.size() < COMPARE_SAMPLE_LIMIT) {
            samples.add(message);
        }
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

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private record CurrentSnapshot(HcPlanProcessPivotRespVO row,
                                   HcPlanOrderDO plan,
                                   String pivotKey,
                                   String pivotKeyHash,
                                   String pivotJson,
                                   String sourceContentHash,
                                   LocalDate statDate) {

        private String label() {
            return "planNo=" + safe(row.getPlanNo()) + ", segment=" + safe(row.getSegmentBatchNo())
                    + ", key=" + pivotKeyHash;
        }

        private static String safe(Object value) {
            return value == null ? "" : String.valueOf(value);
        }
    }

}
