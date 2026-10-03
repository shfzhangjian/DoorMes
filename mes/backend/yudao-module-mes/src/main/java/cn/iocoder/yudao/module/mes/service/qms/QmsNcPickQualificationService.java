package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionContextRespVO.ScopeCandidate;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsSampleAbnormalLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionScopeDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDispositionScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDispositionExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcReportGateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcWorkstationCommandMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionExecutionDO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordStockDisposeReqVO;
import java.time.LocalDateTime;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcPickMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace.HcBatchTraceFactDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

@Service
public class QmsNcPickQualificationService {
    @Resource private QmsNcPickMapper pickMapper;
    @Resource private QmsFaiOrderMapper faiMapper;
    @Resource private QmsNcDispositionScopeMapper scopeMapper;
    @Resource private QmsNcDispositionExecutionMapper executionMapper;
    @Resource private QmsNcReportGateMapper gateMapper;
    @Resource private QmsNcWorkstationCommandMapper commandMapper;

    public boolean supports(QmsNcRecordDO ncr) {
        return ncr != null && "FAI".equals(ncr.getSourceBizType())
                && ("湿法".equals(ncr.getProcessName()) || StrUtil.nullToEmpty(ncr.getProcessName()).contains("磨皮")
                    || "粘胶1".equals(ncr.getProcessName()));
    }

    public boolean isPick(QmsNcRecordDO ncr) {
        return supports(ncr) && "PICK".equals(ncr.getFinalDisposition());
    }

    public static boolean supportedFai(QmsFaiOrderDO fai) {
        return fai != null && Set.of("WET_REPORT", "ROUGH_GRINDING_REPORT", "ROUGH_GRINDING_SECOND_SEGMENT", "ADHESIVE_REPORT")
                .contains(StrUtil.nullToEmpty(fai.getSourceModule()));
    }

    public List<ScopeCandidate> candidates(QmsNcRecordDO ncr) {
        return candidates(ncr, false);
    }

    private List<ScopeCandidate> candidates(QmsNcRecordDO ncr, boolean legacyWetSegments) {
        QmsFaiOrderDO fai = faiMapper.selectById(ncr.getSourceId());
        if (!supportedFai(fai) || !Objects.equals(fai.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !matchesProcess(ncr.getProcessName(), fai.getSourceModule())
                || !normalize(fai.getProductBatchNo()).equals(normalize(ncr.getLotNo()))) {
            throw exception(new ErrorCode(400, "NCR 来源检验与母卷/加工段不一致，请核对来源记录"));
        }
        String lot = normalize(ncr.getLotNo());
        List<HcBatchTraceFactDTO> facts = pickMapper.selectProductionObjects(lot, TenantContextHolder.getRequiredTenantId());
        if (legacyWetSegments) return buildLegacyWetCandidates(lot, facts);
        List<ScopeCandidate> result = buildCandidates(lot, fai.getSourceModule(), facts);
        // 粘胶1允许在尚无报工记录时对二磨来源段送检；仅使用该检验明确绑定的来源 ID 和段号。
        if (result.isEmpty() && "ADHESIVE_REPORT".equals(fai.getSourceModule())
                && facts.stream().noneMatch(f -> "ADHESIVE_REPORT".equals(f.getSourceType())
                    && normalize(f.getProductionBatchNo()).equals(lot))) {
            List<HcBatchTraceFactDTO> sources = facts.stream()
                    .filter(f -> "ROUGH_GRINDING_SECOND".equals(f.getSourceType()))
                    .filter(f -> lot.equals(normalize(f.getProductionBatchNo())))
                    .filter(f -> Objects.equals(fai.getSourceReportId(), f.getId()))
                    .filter(f -> StrUtil.nullToEmpty(fai.getSourceReportNo()).endsWith("-ADH-" + lot + "-" + f.getId()))
                    .filter(f -> "CONFIRMED".equalsIgnoreCase(f.getReportStatus()) || f.getConfirmerTime() != null)
                    .toList();
            result = buildCandidates(lot, "ROUGH_GRINDING_SECOND_SEGMENT", sources);
            result.forEach(c -> c.setLabel("粘胶1送检来源段 " + c.getSegmentBatchNo()));
        }
        return result;
    }

    private static boolean matchesProcess(String processName, String sourceModule) {
        if ("湿法".equals(processName)) return "WET_REPORT".equals(sourceModule);
        if ("粘胶1".equals(processName)) return "ADHESIVE_REPORT".equals(sourceModule);
        return StrUtil.nullToEmpty(processName).contains("磨皮")
                && Set.of("ROUGH_GRINDING_REPORT", "ROUGH_GRINDING_SECOND_SEGMENT").contains(sourceModule);
    }

    static List<ScopeCandidate> buildCandidates(String lot, boolean wet, List<HcBatchTraceFactDTO> facts) {
        return buildCandidates(lot, wet ? "WET_REPORT" : "ROUGH_GRINDING_REPORT", facts);
    }

    /** 处置粒度由异常工序决定，不随下游是否分段改变。 */
    static List<ScopeCandidate> buildCandidates(String lot, String sourceModule, List<HcBatchTraceFactDTO> facts) {
        boolean wet = "WET_REPORT".equals(sourceModule);
        boolean adhesive = "ADHESIVE_REPORT".equals(sourceModule);
        Map<String, ScopeCandidate> result = new LinkedHashMap<>();
        facts.stream().filter(f -> wet ? "WET_REPORT".equals(f.getSourceType())
                        : adhesive ? "ADHESIVE_REPORT".equals(f.getSourceType())
                        : StrUtil.nullToEmpty(f.getSourceType()).startsWith("ROUGH_GRINDING"))
                .sorted(Comparator.comparingInt((HcBatchTraceFactDTO f) ->
                                "ROUGH_GRINDING_SECOND".equals(f.getSourceType()) ? 2 : 1)
                        .thenComparing(HcBatchTraceFactDTO::getId))
                .forEach(fact -> {
                    String no = normalize(fact.getProductionBatchNo());
                    if (wet ? lot.length() != 8 || !lot.equals(no)
                            : !no.matches(".{8}[PQRS]") || (!no.equals(lot) && !isSegmentOf(lot, no))) return;
                    if ("START".equals(fact.getReportType())) return;
                    if (Set.of("VOID", "CANCELED", "CANCELLED", "SCRAPPED", "SCRAP")
                            .contains(StrUtil.nullToEmpty(fact.getReportStatus()).toUpperCase(Locale.ROOT))) {
                        result.remove(no);
                        return;
                    }
                    ScopeCandidate c = new ScopeCandidate();
                    c.setScopeLevel(wet ? "MOTHER_BATCH" : "SEGMENT");
                    c.setScopeLevelName(wet ? "母批" : "加工段");
                    c.setMotherBatchNo(wet ? no : no.substring(0, 8));
                    c.setSegmentBatchNo(wet ? null : no);
                    c.setObjectKey(c.getScopeLevel() + ":" + no);
                    c.setLabel(c.getScopeLevelName() + " " + no);
                    c.setSourceObjectNo(no);
                    c.setSourceObjectType(fact.getSourceType());
                    c.setSourceObjectId(fact.getId());
                    c.setQuantity(BigDecimal.ONE);
                    c.setQuantityUnit(wet ? "卷" : "段");
                    c.setProductionLength(fact.getOutputLength());
                    c.setCurrentStatus(fact.getReportStatus());
                    c.setCurrentStatusName(fact.getSourceTypeName() + " / " + StrUtil.nullToEmpty(fact.getReportStatus()));
                    c.setSelectable(true);
                    result.put(no, c);
                });
        return new ArrayList<>(result.values());
    }

    /** 仅兼容已保存的湿法分段快照，不用于新分派，避免把历史局部放行扩大为整母批。 */
    static List<ScopeCandidate> buildLegacyWetCandidates(String lot, List<HcBatchTraceFactDTO> facts) {
        facts = facts.stream().filter(f -> "WET_REPORT".equals(f.getSourceType())
                || StrUtil.nullToEmpty(f.getSourceType()).startsWith("ROUGH_GRINDING")).toList();
        Map<String, ScopeCandidate> segments = new LinkedHashMap<>();
        Map<String, ScopeCandidate> mothers = new LinkedHashMap<>();
        boolean hasSegments = facts.stream().map(f -> normalize(f.getProductionBatchNo()))
                .anyMatch(no -> (no.equals(lot) && no.matches(".{8}[PQRS]")) || isSegmentOf(lot, no));
        List<HcBatchTraceFactDTO> ordered = facts.stream().sorted(Comparator
                .comparingInt((HcBatchTraceFactDTO f) -> "ROUGH_GRINDING_SECOND".equals(f.getSourceType()) ? 40
                        : f.getSourceType().startsWith("ROUGH_GRINDING") ? 30 : 20)
                .thenComparing(HcBatchTraceFactDTO::getId)).toList();
        for (HcBatchTraceFactDTO fact : ordered) {
            String no = normalize(fact.getProductionBatchNo());
            if (!no.equals(lot) && !isSegmentOf(lot, no)) continue;
            boolean segment = no.matches(".{8}[PQRS]");
            if (Set.of("VOID", "CANCELED", "CANCELLED", "SCRAPPED", "SCRAP")
                    .contains(StrUtil.nullToEmpty(fact.getReportStatus()).toUpperCase(Locale.ROOT))) {
                segments.remove(no);
                mothers.remove(no);
                continue;
            }
            if (!segment && !"WET_REPORT".equals(fact.getSourceType())) continue;
            // 开工占位记录没有可确认的实际产出；已分配加工段可以参加挑选。
            if ("START".equals(fact.getReportType())) continue;
            ScopeCandidate c = new ScopeCandidate();
            c.setScopeLevel(segment ? "SEGMENT" : "MOTHER_BATCH");
            c.setScopeLevelName(segment ? "母批段" : "母批");
            c.setMotherBatchNo(segment ? no.substring(0, 8) : no);
            c.setSegmentBatchNo(segment ? no : null);
            c.setObjectKey(c.getScopeLevel() + ":" + no);
            c.setLabel(c.getScopeLevelName() + " " + no);
            c.setSourceObjectNo(no);
            c.setSourceObjectType(fact.getSourceType());
            c.setSourceObjectId(fact.getId());
            c.setQuantity(BigDecimal.ONE);
            c.setQuantityUnit(segment ? "段" : "卷");
            c.setProductionLength(fact.getOutputLength());
            c.setCurrentStatus(fact.getReportStatus());
            c.setCurrentStatusName(fact.getSourceTypeName() + " / " + StrUtil.nullToEmpty(fact.getReportStatus()));
            c.setSelectable(true);
            (segment ? segments : mothers).put(no, c);
        }
        // 已分段时只选择实际段，避免父子重复和整卷误放行。
        return new ArrayList<>((hasSegments ? segments : mothers).values());
    }

    public boolean requiresConfirmation(QmsNcRecordDO ncr) {
        QmsNcDispositionExecutionDO execution = executionMapper.selectByNcRecordId(ncr.getId());
        return isPick(ncr) && execution != null && Set.of("MOTHER_BATCH", "SEGMENT")
                .contains(StrUtil.nullToEmpty(execution.getScopeLevel()))
                && !"COMPLETED".equals(execution.getExecutionStatus());
    }

    /** 在 NCR 主单事务和行锁内执行；改判依据与执行结果、BPM 流转一起提交。 */
    public void confirm(QmsNcRecordDO ncr, QmsNcRecordStockDisposeReqVO req) {
        if (!Boolean.TRUE.equals(req.getConfirmPickQualified())) {
            throw exception(new ErrorCode(400, "请明确确认已选母卷/加工段挑选合格"));
        }
        if (StrUtil.isNotBlank(req.getStockDisposeStatus()) && !"DONE".equals(req.getStockDisposeStatus())) {
            throw exception(new ErrorCode(400, "挑选合格确认只能提交已完成结果"));
        }
        QmsNcDispositionExecutionDO execution = executionMapper.selectByNcRecordId(ncr.getId());
        List<QmsNcDispositionScopeDO> scopes = scopeMapper.selectListByExecutionId(execution.getId());
        if (scopes.isEmpty()) throw exception(new ErrorCode(400, "没有已确认的挑选范围"));
        scopes.stream().sorted(Comparator.comparing(QmsNcDispositionScopeDO::getSourceObjectType)
                .thenComparing(QmsNcDispositionScopeDO::getSourceObjectId)).forEach(scope -> {
            if (pickMapper.lockProductionObject(scope.getSourceObjectType(), scope.getSourceObjectId(), ncr.getTenantId()) == null) {
                throw exception(new ErrorCode(400, "挑选对象生产记录已失效，请重新核对范围"));
            }
        });
        Map<String, ScopeCandidate> available = new HashMap<>();
        boolean legacyWetSegments = "湿法".equals(ncr.getProcessName())
                && "SEGMENT".equals(execution.getScopeLevel());
        candidates(ncr, legacyWetSegments).forEach(c -> available.put(c.getObjectKey(), c));
        for (QmsNcDispositionScopeDO scope : scopes) {
            ScopeCandidate current = available.get(scope.getObjectKey());
            if (current == null
                    || !Objects.equals(current.getSourceObjectType(), scope.getSourceObjectType())
                    || !Objects.equals(current.getSourceObjectId(), scope.getSourceObjectId())
                    || !"PICK".equals(scope.getDispositionType())
                    || !"PICK_ALLOW".equals(scope.getScopeRole())) {
                throw exception(new ErrorCode(400, "挑选范围已变化，请核对母卷/加工段：" + scope.getObjectKey()));
            }
            if (pickMapper.countConflictingScopes(ncr.getId(), scope.getObjectKey(), scope.getMotherBatchNo(), ncr.getTenantId()) > 0) {
                throw exception(new ErrorCode(400, "对象存在其他报废/返工处置，不能覆盖：" + scope.getObjectKey()));
            }
        }
        for (QmsNcDispositionScopeDO scope : scopes) {
            scope.setExecutionResult("PICK_QUALIFIED");
            scopeMapper.updateById(scope);
        }
        execution.setExecutionStatus("COMPLETED");
        executionMapper.updateById(execution);
        LocalDateTime now = LocalDateTime.now();
        gateMapper.selectListByExecutionId(execution.getId()).forEach(gate -> {
            gate.setGateStatus("ALLOW");
            gate.setEffectiveTime(now);
            gateMapper.updateById(gate);
        });
        var command = commandMapper.selectByExecutionId(execution.getId());
        if (command == null) throw exception(new ErrorCode(400, "挑选处置指令不存在"));
        command.setCommandStatus("APPLIED");
        command.setAckTime(now);
        command.setApplyTime(now);
        command.setAckUser(ncr.getCurrentHandlerUserName());
        command.setAckMessage("执行人确认所选母卷/加工段挑选合格，原始检验结论保留");
        command.setApplyResult("PICK_QUALIFIED");
        commandMapper.updateById(command);
    }

    public boolean isLockQualified(QmsSampleAbnormalLockDO lock, Collection<String> objectNos) {
        String source = normalize(lock.getObjectNo());
        List<String> segments = objectNos.stream().map(QmsNcPickQualificationService::normalize)
                .filter(no -> isSegmentOf(source, no)).distinct().toList();
        List<String> targets = segments.isEmpty()
                ? objectNos.stream().map(QmsNcPickQualificationService::normalize).filter(source::equals).distinct().toList()
                : segments;
        Long faiId = "NG".equals(lock.getRecheckResult()) && lock.getRecheckInspectionId() != null
                ? lock.getRecheckInspectionId() : lock.getAbnormalInspectionId();
        return !targets.isEmpty() && targets.stream().allMatch(no -> isQualified(faiId, no));
    }

    public boolean isQualified(QmsFaiOrderDO fai, String objectNo) {
        return supportedFai(fai) && "NG".equals(fai.getJudgment())
                && Objects.equals(fai.getTenantId(), TenantContextHolder.getRequiredTenantId())
                && isQualified(fai.getId(), objectNo);
    }

    public boolean isQualified(Long faiId, String objectNo) {
        return qualification(faiId, objectNo) == Qualification.QUALIFIED;
    }

    public enum Qualification { NONE, PARTIAL, QUALIFIED }

    /** 仅汇总同一湿法检验的真实有效产出，不能由已选范围反推母卷全部合格。 */
    public Qualification qualification(Long faiId, String objectNo) {
        if (faiId == null || StrUtil.isBlank(objectNo)) return Qualification.NONE;
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<QmsNcDispositionScopeDO> scopes = pickMapper.selectQualifiedScopes(faiId, tenantId);
        if (scopes.isEmpty()) return Qualification.NONE;
        if (scopes.stream().anyMatch(scope -> covers(scope, objectNo))) return Qualification.QUALIFIED;
        QmsFaiOrderDO fai = faiMapper.selectById(faiId);
        String mother = normalize(objectNo);
        if (fai == null || !Objects.equals(fai.getTenantId(), tenantId)
                || !"WET_REPORT".equals(fai.getSourceModule()) || !"NG".equals(fai.getJudgment())
                || !mother.equals(normalize(fai.getProductBatchNo())) || mother.length() != 8) {
            return Qualification.NONE;
        }
        List<ScopeCandidate> actual = buildLegacyWetCandidates(mother,
                pickMapper.selectProductionObjects(mother, tenantId));
        if (actual.isEmpty()) return Qualification.NONE;
        long qualified = actual.stream().filter(candidate -> scopes.stream().anyMatch(scope ->
                covers(scope, firstObjectNo(candidate)))).count();
        if (qualified == actual.size()) return Qualification.QUALIFIED;
        return qualified > 0 ? Qualification.PARTIAL : Qualification.NONE;
    }

    private static String firstObjectNo(ScopeCandidate candidate) {
        return "SEGMENT".equals(candidate.getScopeLevel())
                ? candidate.getSegmentBatchNo() : candidate.getMotherBatchNo();
    }

    static boolean covers(QmsNcDispositionScopeDO scope, String objectNo) {
        String no = normalize(objectNo);
        if ("MOTHER_BATCH".equals(scope.getScopeLevel())) {
            String mother = normalize(scope.getMotherBatchNo());
            return no.equals(mother) || isSegmentOf(mother, no);
        }
        return "SEGMENT".equals(scope.getScopeLevel()) && no.equals(normalize(scope.getSegmentBatchNo()));
    }

    static boolean isSegmentOf(String mother, String segment) {
        return mother.length() == 8 && segment.length() == 9 && segment.startsWith(mother)
                && "PQRS".indexOf(segment.charAt(8)) >= 0;
    }

    static String normalize(String no) {
        return StrUtil.trimToEmpty(no).toUpperCase(Locale.ROOT).replaceFirst("-J[12]$", "");
    }
}
