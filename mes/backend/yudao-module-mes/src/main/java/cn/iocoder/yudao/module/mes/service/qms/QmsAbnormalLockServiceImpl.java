package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsAbnormalLockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsAbnormalLockRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsAbnormalLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIpqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsAbnormalLockImpactDTO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsAbnormalLockImpactMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsAbnormalLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIpqcOrderMapper;
import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class QmsAbnormalLockServiceImpl implements QmsAbnormalLockService {

    private static final String JUDGMENT_NG = "NG";
    private static final String LOCK_STATUS_LOCKED = "LOCKED";
    private static final String SOURCE_TYPE_FAI = "FAI";
    private static final String SOURCE_TYPE_IPQC = "IPQC";
    private static final String SOURCE_TYPE_FQC = "FQC";
    private static final String SOURCE_MODULE_GLUE_BOARD_FAI = "GLUE_BOARD_FAI";
    private static final String OBJECT_INSPECTION_ORDER = "INSPECTION_ORDER";
    private static final String OBJECT_SOURCE_BATCH = "SOURCE_BATCH";
    private static final String OBJECT_LOT_INSTANCE = "LOT_INSTANCE";
    private static final String OBJECT_GLUE_BOARD_REPORT = "GLUE_BOARD_REPORT";
    private static final String SCOPE_SOURCE_BATCH = "SOURCE_BATCH";
    private static final String SCOPE_DOWNSTREAM_BATCH = "DOWNSTREAM_BATCH";
    private static final String SCOPE_GLUE_BOARD = "GLUE_BOARD";
    private static final Pattern LAST_THREE_DIGITS = Pattern.compile("\\d{3}$");

    @Resource
    private QmsAbnormalLockMapper abnormalLockMapper;
    @Resource
    private QmsAbnormalLockImpactMapper abnormalLockImpactMapper;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsIpqcOrderMapper qmsIpqcOrderMapper;
    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromFai(Long faiId) {
        QmsFaiOrderDO order = qmsFaiOrderMapper.selectById(faiId);
        if (order == null || !JUDGMENT_NG.equals(order.getJudgment())) {
            return;
        }
        LockContext context = new LockContext();
        context.lockSourceType = SOURCE_MODULE_GLUE_BOARD_FAI.equals(order.getSourceModule()) ? SOURCE_MODULE_GLUE_BOARD_FAI : SOURCE_TYPE_FAI;
        context.inspectionOrderType = SOURCE_TYPE_FAI;
        context.inspectionOrderId = order.getId();
        context.inspectionOrderNo = order.getFaiNo();
        context.inspectionStatus = order.getStatus();
        context.inspectionResult = order.getJudgment();
        context.sourceModule = order.getSourceModule();
        context.sourceReportId = order.getSourceReportId();
        context.sourceReportNo = order.getSourceReportNo();
        context.sourceOperationCode = firstNotBlank(order.getSourceOperationCode(), order.getOperationCode());
        context.sourceOperationName = firstNotBlank(order.getSourceOperationName(), order.getOperationName());
        context.planId = order.getPlanOrderId();
        context.planNo = order.getWorkOrderNo();
        context.planOperationId = null;
        context.operationCode = order.getOperationCode();
        context.operationName = order.getOperationName();
        context.inspectionSubmitTime = firstNonNull(order.getSubmissionTime(), order.getOperatorTime(), order.getCreateTime());
        context.ngConfirmTime = firstNonNull(order.getQaTime(), order.getReleaseTime(), order.getOperatorTime(), order.getUpdateTime(), LocalDateTime.now());
        context.tenantId = firstNonNull(order.getTenantId(), currentTenantId());
        context.glueBoardStockId = order.getGlueBoardStockId();
        context.glueBoardBatchNo = order.getGluePlateBatchNo();
        addBatch(context.seedBatchNos, order.getProductBatchNo(), order.getSourceReportNo());
        syncLocks(context);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromIpqc(Long ipqcId) {
        QmsIpqcOrderDO order = qmsIpqcOrderMapper.selectById(ipqcId);
        if (order == null || !JUDGMENT_NG.equals(order.getJudgment())) {
            return;
        }
        LockContext context = new LockContext();
        context.lockSourceType = SOURCE_TYPE_IPQC;
        context.inspectionOrderType = SOURCE_TYPE_IPQC;
        context.inspectionOrderId = order.getId();
        context.inspectionOrderNo = order.getIpqcNo();
        context.inspectionStatus = order.getStatus();
        context.inspectionResult = order.getJudgment();
        context.planId = order.getPlanOrderId();
        context.planNo = order.getWorkOrderNo();
        context.operationCode = order.getOperationCode();
        context.operationName = order.getOperationName();
        context.inspectionSubmitTime = firstNonNull(order.getScheduledTime(), order.getCreateTime());
        context.ngConfirmTime = firstNonNull(order.getInspectionTime(), order.getUpdateTime(), LocalDateTime.now());
        context.tenantId = firstNonNull(order.getTenantId(), currentTenantId());
        syncLocks(context);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncFromFqc(Long fqcId) {
        QmsFqcOrderDO order = qmsFqcOrderMapper.selectById(fqcId);
        if (order == null || !JUDGMENT_NG.equals(order.getJudgment())) {
            return;
        }
        LockContext context = new LockContext();
        context.lockSourceType = SOURCE_TYPE_FQC;
        context.inspectionOrderType = SOURCE_TYPE_FQC;
        context.inspectionOrderId = order.getId();
        context.inspectionOrderNo = order.getFqcNo();
        context.inspectionStatus = order.getStatus();
        context.inspectionResult = order.getJudgment();
        context.sourceModule = order.getSourceModule();
        context.sourceReportId = order.getSourceReportId();
        context.sourceReportNo = order.getSourceReportNo();
        context.sourceOperationCode = firstNotBlank(order.getSourceOperationCode(), order.getOperationCode());
        context.sourceOperationName = firstNotBlank(order.getSourceOperationName(), order.getOperationName());
        context.planId = order.getPlanOrderId();
        context.planNo = order.getWorkOrderNo();
        context.operationCode = order.getOperationCode();
        context.operationName = order.getOperationName();
        context.inspectionSubmitTime = firstNonNull(order.getSubmissionTime(), order.getInspectionTime(), order.getCreateTime());
        context.ngConfirmTime = firstNonNull(order.getQaTime(), order.getReleaseTime(), order.getInspectionTime(), order.getUpdateTime(), LocalDateTime.now());
        context.tenantId = firstNonNull(order.getTenantId(), currentTenantId());
        addBatch(context.seedBatchNos, order.getProductBatchNo(), order.getBatchNo(), order.getSourceReportNo());
        syncLocks(context);
    }

    @Override
    public PageResult<QmsAbnormalLockRespVO> getPage(QmsAbnormalLockPageReqVO reqVO) {
        PageResult<QmsAbnormalLockDO> pageResult = abnormalLockMapper.selectPage(reqVO);
        return BeanUtils.toBean(pageResult, QmsAbnormalLockRespVO.class);
    }

    private void syncLocks(LockContext context) {
        List<QmsAbnormalLockImpactDTO> impacts = collectImpacts(context);
        String lockNo = generateLockNo(context.inspectionOrderType);
        LocalDateTime now = LocalDateTime.now();
        for (QmsAbnormalLockImpactDTO impact : impacts) {
            QmsAbnormalLockDO lock = buildLock(context, impact, lockNo, now);
            if (abnormalLockMapper.selectByUniqueKey(lock.getInspectionOrderType(),
                    lock.getInspectionOrderId(), lock.getTenantId(), lock.getAffectedSourceKey()) != null) {
                continue;
            }
            abnormalLockMapper.insert(lock);
        }
    }

    private List<QmsAbnormalLockImpactDTO> collectImpacts(LockContext context) {
        LinkedHashMap<String, QmsAbnormalLockImpactDTO> impacts = new LinkedHashMap<>();
        Set<String> batchNos = new LinkedHashSet<>(context.seedBatchNos);
        if (batchNos.isEmpty() && !StringUtils.hasText(context.glueBoardBatchNo)) {
            putImpact(impacts, buildInspectionOrderImpact(context));
        }
        for (String batchNo : batchNos) {
            putImpact(impacts, buildSourceBatchImpact(context, batchNo));
        }

        if (StringUtils.hasText(context.glueBoardBatchNo) || context.glueBoardUsageId != null) {
            putImpact(impacts, buildGlueBoardSourceImpact(context));
            List<QmsAbnormalLockImpactDTO> glueImpacts = abnormalLockImpactMapper.selectGlueBoardImpacts(
                    context.glueBoardBatchNo, context.glueBoardUsageId, context.tenantId, context.inspectionSubmitTime);
            for (QmsAbnormalLockImpactDTO impact : glueImpacts) {
                putImpact(impacts, impact);
                addBatch(batchNos, impact.getAffectedBatchNo(), impact.getProductionBatchNo(), impact.getSourceBatchNo(),
                        impact.getParentProductionBatchNo());
            }
        }

        for (int i = 0; i < 6 && !batchNos.isEmpty(); i++) {
            String branchPrefix = resolveBranchPrefix(batchNos);
            List<QmsAbnormalLockImpactDTO> lotImpacts = abnormalLockImpactMapper.selectLotImpacts(
                    batchNos, branchPrefix, context.tenantId, context.inspectionSubmitTime);
            int beforeSize = batchNos.size();
            for (QmsAbnormalLockImpactDTO impact : lotImpacts) {
                putImpact(impacts, impact);
                addBatch(batchNos, impact.getAffectedBatchNo(), impact.getProductionBatchNo(), impact.getSourceBatchNo(),
                        impact.getParentProductionBatchNo());
            }
            if (batchNos.size() == beforeSize) {
                break;
            }
        }
        return new ArrayList<>(impacts.values());
    }

    private QmsAbnormalLockDO buildLock(LockContext context, QmsAbnormalLockImpactDTO impact,
                                        String lockNo, LocalDateTime lockTime) {
        String affectedBatchNo = firstNotBlank(impact.getAffectedBatchNo(), impact.getProductionBatchNo(),
                impact.getSourceBatchNo(), firstBatch(context.seedBatchNos), context.glueBoardBatchNo);
        QmsAbnormalLockDO lock = new QmsAbnormalLockDO();
        lock.setLockNo(lockNo);
        lock.setLockSourceType(context.lockSourceType);
        lock.setInspectionOrderType(context.inspectionOrderType);
        lock.setInspectionOrderId(context.inspectionOrderId);
        lock.setInspectionOrderNo(context.inspectionOrderNo);
        lock.setInspectionStatus(context.inspectionStatus);
        lock.setInspectionResult(context.inspectionResult);
        lock.setSourceModule(context.sourceModule);
        lock.setSourceReportId(context.sourceReportId);
        lock.setSourceReportNo(context.sourceReportNo);
        lock.setSourceOperationCode(context.sourceOperationCode);
        lock.setSourceOperationName(context.sourceOperationName);
        lock.setLockScope(resolveLockScope(impact));
        lock.setAffectedObjectType(resolveAffectedObjectType(impact));
        lock.setAffectedSourceTable(firstNotBlank(impact.getSourceTable(), context.inspectionOrderType));
        lock.setAffectedSourceId(impact.getSourceId());
        lock.setAffectedSourceKey(firstNotBlank(impact.getSourceKey(),
                lock.getAffectedSourceTable() + ":" + (impact.getSourceId() == null ? "-" : impact.getSourceId())
                        + ":" + affectedBatchNo));
        lock.setRootBatchNo(firstNotBlank(firstBatch(context.seedBatchNos), impact.getParentProductionBatchNo(),
                impact.getSourceBatchNo(), affectedBatchNo));
        lock.setSourceBatchNo(firstNotBlank(impact.getSourceBatchNo(), firstBatch(context.seedBatchNos)));
        lock.setProductionBatchNo(impact.getProductionBatchNo());
        lock.setParentProductionBatchNo(impact.getParentProductionBatchNo());
        lock.setAffectedBatchNo(affectedBatchNo);
        lock.setPlanId(firstNonNull(impact.getPlanId(), context.planId));
        lock.setPlanNo(firstNotBlank(impact.getPlanNo(), context.planNo));
        lock.setPlanOperationId(firstNonNull(impact.getPlanOperationId(), context.planOperationId));
        lock.setOperationCode(firstNotBlank(impact.getOperationCode(), context.operationCode));
        lock.setOperationName(firstNotBlank(impact.getOperationName(), context.operationName));
        lock.setProcessOrder(impact.getProcessOrder());
        lock.setGlueBoardStockId(context.glueBoardStockId);
        lock.setGlueBoardUsageId(firstNonNull(impact.getGlueBoardUsageId(), context.glueBoardUsageId));
        lock.setGlueBoardBatchNo(firstNotBlank(impact.getGlueBoardBatchNo(), context.glueBoardBatchNo));
        lock.setGlueBoardStartPosition(impact.getGlueBoardStartPosition());
        lock.setGlueBoardUseLength(impact.getGlueBoardUseLength());
        lock.setInspectionSubmitTime(context.inspectionSubmitTime);
        lock.setNgConfirmTime(context.ngConfirmTime);
        lock.setLockTime(lockTime);
        lock.setLockStatus(LOCK_STATUS_LOCKED);
        lock.setLockReason(buildLockReason(context, affectedBatchNo));
        lock.setTraceSnapshotJson(buildSnapshot(context, impact));
        lock.setTenantId(context.tenantId);
        return lock;
    }

    private QmsAbnormalLockImpactDTO buildInspectionOrderImpact(LockContext context) {
        QmsAbnormalLockImpactDTO impact = new QmsAbnormalLockImpactDTO();
        impact.setImpactSourceType(OBJECT_INSPECTION_ORDER);
        impact.setSourceTable(context.inspectionOrderType);
        impact.setSourceId(context.inspectionOrderId);
        impact.setSourceKey(context.inspectionOrderType + ":" + context.inspectionOrderId + ":ORDER");
        impact.setPlanId(context.planId);
        impact.setPlanNo(context.planNo);
        impact.setPlanOperationId(context.planOperationId);
        impact.setOperationCode(context.operationCode);
        impact.setOperationName(context.operationName);
        impact.setEventTime(context.ngConfirmTime);
        return impact;
    }

    private QmsAbnormalLockImpactDTO buildSourceBatchImpact(LockContext context, String batchNo) {
        QmsAbnormalLockImpactDTO impact = new QmsAbnormalLockImpactDTO();
        impact.setImpactSourceType(OBJECT_SOURCE_BATCH);
        impact.setSourceTable(context.inspectionOrderType);
        impact.setSourceId(context.inspectionOrderId);
        impact.setSourceKey(context.inspectionOrderType + ":" + context.inspectionOrderId + ":SOURCE_BATCH:" + batchNo);
        impact.setAffectedBatchNo(batchNo);
        impact.setSourceBatchNo(batchNo);
        impact.setProductionBatchNo(batchNo);
        impact.setPlanId(context.planId);
        impact.setPlanNo(context.planNo);
        impact.setPlanOperationId(context.planOperationId);
        impact.setOperationCode(context.operationCode);
        impact.setOperationName(context.operationName);
        impact.setEventTime(context.inspectionSubmitTime);
        return impact;
    }

    private QmsAbnormalLockImpactDTO buildGlueBoardSourceImpact(LockContext context) {
        QmsAbnormalLockImpactDTO impact = new QmsAbnormalLockImpactDTO();
        impact.setImpactSourceType(SCOPE_GLUE_BOARD);
        impact.setSourceTable("GLUE_BOARD");
        impact.setSourceId(context.glueBoardStockId);
        impact.setSourceKey(context.inspectionOrderType + ":" + context.inspectionOrderId + ":GLUE_BOARD:"
                + firstNotBlank(context.glueBoardBatchNo, String.valueOf(context.glueBoardStockId)));
        impact.setAffectedBatchNo(context.glueBoardBatchNo);
        impact.setGlueBoardBatchNo(context.glueBoardBatchNo);
        impact.setGlueBoardUsageId(context.glueBoardUsageId);
        impact.setEventTime(context.inspectionSubmitTime);
        return impact;
    }

    private void putImpact(Map<String, QmsAbnormalLockImpactDTO> impacts, QmsAbnormalLockImpactDTO impact) {
        if (impact == null) {
            return;
        }
        String key = firstNotBlank(impact.getSourceKey(),
                impact.getSourceTable() + ":" + impact.getSourceId() + ":" + impact.getAffectedBatchNo());
        if (!StringUtils.hasText(key)) {
            return;
        }
        impacts.putIfAbsent(key, impact);
    }

    private String resolveLockScope(QmsAbnormalLockImpactDTO impact) {
        if (SCOPE_GLUE_BOARD.equals(impact.getImpactSourceType())
                || "ADHESIVE1_GLUE_BOARD".equals(impact.getImpactSourceType())
                || "ADHESIVE2_GLUE_BOARD".equals(impact.getImpactSourceType())) {
            return SCOPE_GLUE_BOARD;
        }
        if (OBJECT_SOURCE_BATCH.equals(impact.getImpactSourceType())) {
            return SCOPE_SOURCE_BATCH;
        }
        return SCOPE_DOWNSTREAM_BATCH;
    }

    private String resolveAffectedObjectType(QmsAbnormalLockImpactDTO impact) {
        if (OBJECT_SOURCE_BATCH.equals(impact.getImpactSourceType())) {
            return OBJECT_SOURCE_BATCH;
        }
        if (SCOPE_GLUE_BOARD.equals(impact.getImpactSourceType())
                || "ADHESIVE1_GLUE_BOARD".equals(impact.getImpactSourceType())
                || "ADHESIVE2_GLUE_BOARD".equals(impact.getImpactSourceType())) {
            return OBJECT_GLUE_BOARD_REPORT;
        }
        if (OBJECT_INSPECTION_ORDER.equals(impact.getImpactSourceType())) {
            return OBJECT_INSPECTION_ORDER;
        }
        return OBJECT_LOT_INSTANCE;
    }

    private String buildLockReason(LockContext context, String affectedBatchNo) {
        String orderNo = firstNotBlank(context.inspectionOrderNo, String.valueOf(context.inspectionOrderId));
        String batchText = StringUtils.hasText(affectedBatchNo) ? "，受影响批次：" + affectedBatchNo : "";
        return context.inspectionOrderType + "检验单 " + orderNo + " 已确认NG" + batchText;
    }

    private String buildSnapshot(LockContext context, QmsAbnormalLockImpactDTO impact) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("inspectionOrderType", context.inspectionOrderType);
        snapshot.put("inspectionOrderId", context.inspectionOrderId);
        snapshot.put("inspectionOrderNo", context.inspectionOrderNo);
        snapshot.put("inspectionSubmitTime", context.inspectionSubmitTime);
        snapshot.put("ngConfirmTime", context.ngConfirmTime);
        snapshot.put("seedBatchNos", context.seedBatchNos);
        snapshot.put("impactSourceType", impact.getImpactSourceType());
        snapshot.put("impactSourceTable", impact.getSourceTable());
        snapshot.put("impactSourceId", impact.getSourceId());
        snapshot.put("impactEventTime", impact.getEventTime());
        snapshot.put("sourceSnapshot", impact.getSnapshotJson());
        return JsonUtils.toJsonString(snapshot);
    }

    private String generateLockNo(String orderType) {
        return "QLOCK-" + orderType + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + (System.currentTimeMillis() % 1000);
    }

    private void addBatch(Collection<String> target, String... values) {
        if (values == null) {
            return;
        }
        for (String value : values) {
            if (StringUtils.hasText(value) && !"-".equals(value.trim())) {
                target.add(value.trim());
            }
        }
    }

    private String firstBatch(Collection<String> batchNos) {
        if (batchNos == null) {
            return "";
        }
        return batchNos.stream().filter(StringUtils::hasText).findFirst().orElse("");
    }

    private String resolveBranchPrefix(Collection<String> batchNos) {
        if (batchNos == null) {
            return null;
        }
        return batchNos.stream()
                .map(this::deriveBranchPrefix)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String deriveBranchPrefix(String batchNo) {
        if (!StringUtils.hasText(batchNo)) {
            return null;
        }
        String value = batchNo.trim();
        int dashIndex = value.indexOf('-');
        if (dashIndex > 0) {
            return value.substring(0, dashIndex);
        }
        if (value.length() > 3 && LAST_THREE_DIGITS.matcher(value.substring(value.length() - 3)).matches()) {
            return value.substring(0, value.length() - 3);
        }
        return value;
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
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
            return "";
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private static class LockContext {
        private String lockSourceType;
        private String inspectionOrderType;
        private Long inspectionOrderId;
        private String inspectionOrderNo;
        private String inspectionStatus;
        private String inspectionResult;
        private String sourceModule;
        private Long sourceReportId;
        private String sourceReportNo;
        private String sourceOperationCode;
        private String sourceOperationName;
        private Long planId;
        private String planNo;
        private Long planOperationId;
        private String operationCode;
        private String operationName;
        private LocalDateTime inspectionSubmitTime;
        private LocalDateTime ngConfirmTime;
        private Long glueBoardStockId;
        private Long glueBoardUsageId;
        private String glueBoardBatchNo;
        private Long tenantId;
        private final Set<String> seedBatchNos = new LinkedHashSet<>();
    }
}
