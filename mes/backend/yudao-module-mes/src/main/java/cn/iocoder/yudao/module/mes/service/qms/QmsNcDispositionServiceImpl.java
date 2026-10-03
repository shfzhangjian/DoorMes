package cn.iocoder.yudao.module.mes.service.qms;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcClosedCorrectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcClosedCorrectionRespVO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcClosedCorrectionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcFlowLogMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcFlowLogDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo.HcBatchTraceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionContextRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionExecutionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordHandleReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsProductAbnormalEventDetailRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotAbnormalLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotAbnormalLockItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionExecutionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcDispositionScopeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRelationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcReportGateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcWorkstationCommandDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotAbnormalLockItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotAbnormalLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDispositionExecutionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcDispositionScopeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRelationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcReportGateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcWorkstationCommandMapper;
import cn.iocoder.yudao.module.mes.service.hc.batchtrace.HcBatchTraceService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

/**
 * 产品 NCR 处置执行 V2：范围确认、执行单、门禁和工作台指令。
 *
 * <p>范围确认后自动生成并应用工作台指令，避免执行步骤再次等待人工下达。</p>
 */
@Service
@Validated
public class QmsNcDispositionServiceImpl implements QmsNcDispositionService {

    private static final ErrorCode NCR_NOT_EXISTS = new ErrorCode(1008100171, "NCR 不存在");
    private static final ErrorCode PRODUCT_NCR_REQUIRED = new ErrorCode(1008100172, "原物料 NCR 暂不适用产品处置 V2");
    private static final ErrorCode STATUS_INVALID = new ErrorCode(1008100173, "只有终审完成/处置分派状态可以确认处置范围");
    private static final ErrorCode DISPOSITION_INVALID = new ErrorCode(1008100174, "终审处置措施无效：{}");
    private static final ErrorCode SCOPE_LEVEL_INVALID = new ErrorCode(1008100175, "产品处置范围仅支持片号选择：{}");
    private static final ErrorCode SCOPE_OBJECT_INVALID = new ErrorCode(1008100176, "所选对象不在当前 NCR 原始影响范围内：{}");
    private static final ErrorCode EXECUTOR_INVALID = new ErrorCode(1008100177, "处置执行人不存在或已失效");
    private static final ErrorCode RECUT_PROCESS_INVALID = new ErrorCode(1008100179, "改切只允许用于裁切工序 NCR");

    private static final String STATUS_EXECUTION_ASSIGN = "EXECUTION_ASSIGN";
    private static final String TRANSFER_ROUTE_EXECUTION = "DISPOSITION_EXECUTION";
    private static final String SOURCE_TYPE_RAW_MATERIAL = "RAW_MATERIAL";
    private static final Set<String> DISPOSITIONS = Set.of("PICK", "REWORK", "RECUT", "SCRAP", "CONCESSION");
    private static final Set<String> SCOPE_LEVELS = Set.of("MOTHER_BATCH", "SEGMENT", "PIECE");
    private static final Map<String, Integer> PROCESS_ORDER_BY_TARGET = Map.ofEntries(
            Map.entry("FORMULA", 10),
            Map.entry("WET", 20),
            Map.entry("ROUGH_GRINDING", 40),
            Map.entry("ROUGH_GRINDING_FIRST", 30),
            Map.entry("ROUGH_GRINDING_SECOND", 40),
            Map.entry("ADHESIVE1", 50),
            Map.entry("SLITTING", 60),
            Map.entry("PRESS_SLOT", 70),
            Map.entry("ADHESIVE2", 80),
            Map.entry("CUT", 90),
            Map.entry("CUT_ROUND", 90)
    );

    @Resource
    private QmsNcRecordMapper qmsNcRecordMapper;
    @Resource
    private QmsNcPickQualificationService pickQualificationService;
    @Resource
    private QmsNcDispositionExecutionMapper executionMapper;
    @Resource
    private QmsNcDispositionScopeMapper scopeMapper;
    @Resource
    private QmsNcReportGateMapper reportGateMapper;
    @Resource
    private QmsNcWorkstationCommandMapper commandMapper;
    @Resource
    private QmsNcRelationMapper relationMapper;
    @Resource
    private HcPressSlotAbnormalLockMapper pressSlotAbnormalLockMapper;
    @Resource
    private HcPressSlotAbnormalLockItemMapper pressSlotAbnormalLockItemMapper;
    @Resource
    private QmsProductAbnormalEventService productAbnormalEventService;
    @Resource
    private HcBatchTraceService batchTraceService;
    @Resource
    private QmsNcRecordService ncRecordService;
    @Resource
    private QmsCutRoundFqcService qmsCutRoundFqcService;
    @Resource
    private AdminUserApi adminUserApi;

    @Resource
    private QmsNcClosedCorrectionMapper correctionMapper;
    @Resource
    private QmsNcFlowLogMapper correctionLogMapper;

    private cn.iocoder.yudao.framework.common.exception.ServiceException correctionError(String message) {
        return exception(new ErrorCode(400, message));
    }

    private QmsNcRecordDO lockClosedProduct(Long id) {
        QmsNcRecordDO ncr = validateProductNcr(qmsNcRecordMapper.selectByIdForUpdate(id));
        if (!Objects.equals(ncr.getTenantId(), TenantContextHolder.getRequiredTenantId())
                || !"CLOSED".equals(ncr.getStatus())) {
            throw correctionError("仅允许修改当前租户已关闭的产品不合格处置单");
        }
        return ncr;
    }

    private String currentDisposition(QmsNcDispositionScopeDO scope) {
        return "PICK_OUTSIDE_SCRAP".equals(scope.getScopeRole()) ? "SCRAP" : scope.getDispositionType();
    }

    private String correctionBlockedReason(QmsNcDispositionScopeDO scope, Long tenantId) {
        if (!"PIECE".equals(scope.getScopeLevel()) || StrUtil.isBlank(scope.getPieceNo())) {
            return "缺少明确片号，不能更正";
        }
        // 不使用“当前待上架”代替“从未上架”：下架会清空库位，但保留上架历史。
        var ngPieces = correctionMapper.lockNgPieces(tenantId, scope.getPieceNo());
        if (ngPieces.stream().anyMatch(piece -> piece.getShelvedTime() != null || piece.getStockId() != null
                || StrUtil.isNotBlank(piece.getCurrentLocationCode())
                || Set.of("STORED", "FROZEN").contains(StrUtil.nullToEmpty(piece.getStatus())))) {
            return "该片号已上架或曾经上架";
        }
        var packages = correctionMapper.lockPackages(tenantId, scope.getPieceNo());
        if (packages.stream().anyMatch(box -> box.getInboundTime() != null
                || "INBOUNDED".equals(box.getUnitStatus()))) {
            return "该片号所在包装已上架或曾经上架";
        }
        if (correctionMapper.countShelvedStock(tenantId, scope.getPieceNo()) > 0) {
            return "该片号已有上架库存记录";
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsNcClosedCorrectionRespVO getClosedCorrection(Long id) {
        QmsNcRecordDO ncr = lockClosedProduct(id);
        QmsNcDispositionExecutionDO execution = executionMapper.selectByNcRecordId(id);
        if (execution == null) {
            throw correctionError("该单没有处置片号快照，无法直接更正；请先核实历史处置范围");
        }
        QmsNcClosedCorrectionRespVO response = new QmsNcClosedCorrectionRespVO();
        response.setId(id);
        response.setNcNo(ncr.getNcNo());
        response.setPieces(scopeMapper.selectListByExecutionId(execution.getId()).stream().map(scope -> {
            QmsNcClosedCorrectionRespVO.Piece piece = new QmsNcClosedCorrectionRespVO.Piece();
            piece.setScopeId(scope.getId());
            piece.setPieceNo(scope.getPieceNo());
            piece.setOriginalDisposition(currentDisposition(scope));
            piece.setDispositionType(currentDisposition(scope));
            piece.setBlockedReason(correctionBlockedReason(scope, ncr.getTenantId()));
            piece.setEditable(piece.getBlockedReason() == null);
            return piece;
        }).toList());
        return response;
    }

    private QmsNcClosedCorrectionRespVO buildCorrectionPreview(QmsNcClosedCorrectionReqVO reqVO,
                                                               QmsNcRecordDO ncr,
                                                               QmsNcDispositionExecutionDO execution) {
        if (execution == null) {
            throw correctionError("该单没有处置片号快照，不能更正");
        }
        Map<Long, QmsNcDispositionScopeDO> byId = scopeMapper.selectListByExecutionId(execution.getId()).stream()
                .collect(Collectors.toMap(QmsNcDispositionScopeDO::getId, item -> item));
        Set<Long> seen = new LinkedHashSet<>();
        List<QmsNcClosedCorrectionRespVO.Piece> changes = new ArrayList<>();
        for (var change : reqVO.getChanges().stream().sorted(Comparator.comparing(QmsNcClosedCorrectionReqVO.Change::getScopeId)).toList()) {
            QmsNcDispositionScopeDO scope = byId.get(change.getScopeId());
            if (scope == null || !seen.add(change.getScopeId())) {
                throw correctionError("片号范围无效或重复，请刷新后重试");
            }
            if (!Set.of("PICK", "SCRAP").contains(change.getDispositionType())) {
                throw correctionError("修改后处置仅允许挑选或报废");
            }
            if (!Objects.equals(currentDisposition(scope), change.getOriginalDisposition())) {
                throw correctionError("片号 " + scope.getPieceNo() + " 处置已变化，请刷新后重新确认");
            }
            if (Objects.equals(currentDisposition(scope), change.getDispositionType())) {
                continue;
            }
            String blocked = correctionBlockedReason(scope, ncr.getTenantId());
            if (blocked != null) {
                throw correctionError("片号 " + scope.getPieceNo() + "：" + blocked);
            }
            QmsNcClosedCorrectionRespVO.Piece piece = new QmsNcClosedCorrectionRespVO.Piece();
            piece.setScopeId(scope.getId());
            piece.setPieceNo(scope.getPieceNo());
            piece.setOriginalDisposition(currentDisposition(scope));
            piece.setDispositionType(change.getDispositionType());
            piece.setEditable(true);
            changes.add(piece);
        }
        if (changes.isEmpty()) {
            throw correctionError("没有实际需要修改的片号");
        }
        QmsNcWorkstationCommandDO command = commandMapper.selectByExecutionId(execution.getId());
        if (command == null) {
            throw correctionError("处置指令缺失，请先核实历史数据");
        }
        QmsNcClosedCorrectionRespVO response = new QmsNcClosedCorrectionRespVO();
        response.setId(ncr.getId());
        response.setNcNo(ncr.getNcNo());
        response.setPieces(changes);
        response.setPreviewToken(DigestUtil.sha256Hex(ncr.getTenantId() + ":" + ncr.getId() + ":"
                + command.getCommandVersion() + ":" + reqVO.getReason().trim() + ":" + JsonUtils.toJsonString(changes)));
        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsNcClosedCorrectionRespVO previewClosedCorrection(QmsNcClosedCorrectionReqVO reqVO) {
        QmsNcRecordDO ncr = lockClosedProduct(reqVO.getId());
        return buildCorrectionPreview(reqVO, ncr, executionMapper.selectByNcRecordId(ncr.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveClosedCorrection(QmsNcClosedCorrectionReqVO reqVO) {
        QmsNcRecordDO ncr = lockClosedProduct(reqVO.getId());
        QmsNcDispositionExecutionDO execution = executionMapper.selectByNcRecordId(ncr.getId());
        QmsNcClosedCorrectionRespVO preview = buildCorrectionPreview(reqVO, ncr, execution);
        if (!Objects.equals(preview.getPreviewToken(), reqVO.getPreviewToken())) {
            throw correctionError("变更清单已变化或尚未预览，请重新确认后保存");
        }
        List<QmsNcDispositionScopeDO> scopes = scopeMapper.selectListByExecutionId(execution.getId());
        Map<Long, QmsNcClosedCorrectionRespVO.Piece> changes = preview.getPieces().stream()
                .collect(Collectors.toMap(QmsNcClosedCorrectionRespVO.Piece::getScopeId, item -> item));
        List<QmsNcDispositionScopeDO> changedScopes = new ArrayList<>();
        String operator = String.valueOf(SecurityFrameworkUtils.getLoginUserId());
        LocalDateTime now = LocalDateTime.now();
        QmsNcWorkstationCommandDO command = commandMapper.selectByExecutionId(execution.getId());
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("changes", preview.getPieces());
        audit.put("previousCommandSnapshot", command.getScopeSnapshotJson());
        audit.put("previousFinalDisposition", ncr.getFinalDisposition());
        for (QmsNcDispositionScopeDO scope : scopes) {
            var change = changes.get(scope.getId());
            if (change == null) continue;
            scope.setDispositionType(change.getDispositionType());
            scope.setScopeRole("PICK".equals(change.getDispositionType()) ? "PICK_ALLOW" : "SELECTED");
            scope.setRemark(StrUtil.blankToDefault(scope.getRemark(), "") + "；关闭后更正：" + reqVO.getReason().trim());
            scopeMapper.updateById(scope);
            changedScopes.add(scope);
            reportGateMapper.update(null, new LambdaUpdateWrapper<QmsNcReportGateDO>()
                    .eq(QmsNcReportGateDO::getExecutionId, execution.getId())
                    .eq(QmsNcReportGateDO::getObjectKey, scope.getObjectKey())
                    .set(QmsNcReportGateDO::getDispositionType, scope.getDispositionType())
                    .set(QmsNcReportGateDO::getConcessionFlag, false)
                    .set(QmsNcReportGateDO::getEffectMode, resolveEffectMode(scope.getDispositionType(), scope.getScopeRole())));
            String quality = "PICK".equals(scope.getDispositionType()) ? "OK" : "NG";
            correctionMapper.updatePackageQuality(ncr.getTenantId(), scope.getPieceNo(), quality, operator);
            correctionMapper.updatePendingStockQuality(ncr.getTenantId(), scope.getPieceNo(), quality, operator);
        }
        execution.setDispositionType(scopes.stream().map(this::currentDisposition).distinct().sorted().collect(Collectors.joining(",")));
        execution.setSelectedQty(scopes.stream().filter(item -> !"PICK_OUTSIDE_SCRAP".equals(item.getScopeRole()))
                .map(item -> item.getQuantity() == null ? BigDecimal.ONE : item.getQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add));
        execution.setDerivedScrapQty(scopes.stream().filter(item -> "PICK_OUTSIDE_SCRAP".equals(item.getScopeRole()))
                .map(item -> item.getQuantity() == null ? BigDecimal.ONE : item.getQuantity()).reduce(BigDecimal.ZERO, BigDecimal::add));
        executionMapper.updateById(execution);
        Map<String, Object> snapshot = JsonUtils.parseObject(command.getScopeSnapshotJson(), Map.class);
        if (snapshot == null) snapshot = new LinkedHashMap<>();
        snapshot.put("dispositionType", execution.getDispositionType());
        snapshot.put("scopes", scopes.stream().map(this::buildCommandScopeSnapshot).toList());
        command.setScopeSnapshotJson(JsonUtils.toJsonString(snapshot));
        command.setDispositionType(execution.getDispositionType());
        command.setCommandType(resolveCommandType(execution.getDispositionType()));
        command.setEffectMode(resolveEffectMode(execution.getDispositionType(), "SELECTED"));
        command.setCommandVersion(command.getCommandVersion() == null ? 2 : command.getCommandVersion() + 1);
        commandMapper.updateById(command);
        QmsNcRecordDO update = new QmsNcRecordDO();
        update.setId(ncr.getId());
        update.setFinalDisposition(execution.getDispositionType());
        update.setMrbDecision(execution.getDispositionType());
        update.setFinalDisposeDescription(buildLegacyDisposeDescription(execution, scopes));
        qmsNcRecordMapper.updateById(update);
        qmsCutRoundFqcService.applyNcrDispositionResult(ncr, execution, changedScopes);
        correctionLogMapper.insert(QmsNcFlowLogDO.builder()
                .ncRecordId(ncr.getId()).ncNo(ncr.getNcNo()).actionCode("CLOSED_PIECE_CORRECTION")
                .actionName("关闭后修改片号处置").fromStatus("CLOSED").toStatus("CLOSED")
                .fromNodeCode(ncr.getCurrentNodeCode()).toNodeCode(ncr.getCurrentNodeCode())
                .fromNodeName(ncr.getCurrentNodeName()).toNodeName(ncr.getCurrentNodeName())
                .opinion(reqVO.getReason().trim()).handlerUserId(SecurityFrameworkUtils.getLoginUserId())
                .handlerUserName(currentUserName()).handleTime(now).businessSnapshot(audit)
                .tenantId(ncr.getTenantId()).build());
    }

    @Override
    public QmsNcDispositionContextRespVO getDispositionContext(Long ncRecordId) {
        QmsNcRecordDO ncr = validateProductNcr(ncRecordId);
        QmsNcDispositionExecutionDO existing = executionMapper.selectByNcRecordId(ncr.getId());
        List<QmsNcDispositionContextRespVO.ScopeCandidate> candidates =
                existing != null
                        ? buildCandidatesFromExecution(existing.getId()) : buildScopeCandidates(ncr);
        ProcessTarget processTarget = resolveProcessTarget(ncr);
        QmsNcDispositionContextRespVO respVO = new QmsNcDispositionContextRespVO();
        respVO.setNcRecordId(ncr.getId());
        respVO.setNcNo(ncr.getNcNo());
        respVO.setFinalDisposition(normalizeUpper(ncr.getFinalDisposition()));
        respVO.setFinalDispositionName(dispositionName(ncr.getFinalDisposition()));
        respVO.setNgProcessCode(processTarget.processCode());
        respVO.setNgProcessName(processTarget.processName());
        respVO.setTargetWorkstationCode(processTarget.workstationCode());
        respVO.setTargetWorkstationName(processTarget.workstationName());
        respVO.setAffectedQty(ncr.getDefectQty());
        respVO.setSourceLotNo(ncr.getLotNo());
        respVO.setCandidates(candidates);
        respVO.setPickQualification(pickQualificationService.isPick(ncr)
                && (existing == null || !"PIECE".equals(existing.getScopeLevel())));
        if (candidates.isEmpty()) {
            respVO.setEmptyReason(pickQualificationService.supports(ncr)
                    ? ("WET".equals(processTarget.processCode())
                        ? "未找到来源湿法母批的有效生产记录；湿法仅按母批处置，不选择 P/Q/R/S 段"
                        : "未找到来源工序的有效 P/Q/R/S 加工段记录；磨皮和粘胶1仅按实际加工段处置")
                    : "当前来源范围没有可处置片号：可能尚未生成片号或已被后续工序确认");
        }
        List<String> availableLevels = candidates.stream()
                .map(QmsNcDispositionContextRespVO.ScopeCandidate::getScopeLevel)
                .distinct()
                .sorted(Comparator.comparingInt(this::scopeLevelOrder))
                .toList();
        respVO.setAvailableScopeLevels(availableLevels);
        respVO.setDefaultScopeLevel(availableLevels.contains("PIECE") ? "PIECE" : availableLevels.isEmpty() ? null : availableLevels.get(0));
        respVO.setCandidateSourceDescription(Boolean.TRUE.equals(respVO.getPickQualification())
                ? (existing != null ? "沿用已保存的处置范围；"
                        : "WET".equals(processTarget.processCode()) ? "湿法按母批处置；" : "磨皮/粘胶1按 P/Q/R/S 加工段处置；")
                        + "范围确认/执行分派完成后立即按所选对象改判合格；其他异常保持原状态"
                : buildCandidateSourceDescription(candidates));
        if (existing != null) {
            respVO.setExistingExecution(buildExecutionResp(existing));
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QmsNcDispositionExecutionRespVO confirmDispositionScope(QmsNcDispositionConfirmReqVO reqVO) {
        QmsNcRecordDO ncr = validateProductNcr(qmsNcRecordMapper.selectByIdForUpdate(reqVO.getId()));
        QmsNcDispositionExecutionDO existing = executionMapper.selectByNcRecordId(ncr.getId());
        if (existing != null) {
            return reassignExistingExecution(ncr, existing, reqVO);
        }
        if (!STATUS_EXECUTION_ASSIGN.equals(ncr.getStatus())) {
            throw exception(STATUS_INVALID);
        }
        String dispositionType = normalizeDispositionString(ncr.getFinalDisposition());
        Set<String> allowedDispositionTypes = parseDispositionSet(dispositionType);
        if (CollUtil.isEmpty(allowedDispositionTypes)) {
            throw exception(DISPOSITION_INVALID, ncr.getFinalDisposition());
        }
        List<String> invalidDispositionTypes = allowedDispositionTypes.stream()
                .filter(item -> !DISPOSITIONS.contains(item))
                .toList();
        if (CollUtil.isNotEmpty(invalidDispositionTypes)) {
            throw exception(DISPOSITION_INVALID, String.join("、", invalidDispositionTypes));
        }
        String scopeLevel = normalizeUpper(reqVO.getScopeLevel());
        if (!SCOPE_LEVELS.contains(scopeLevel)) {
            throw exception(SCOPE_LEVEL_INVALID, reqVO.getScopeLevel());
        }
        ProcessTarget target = resolveProcessTarget(ncr);
        if (allowedDispositionTypes.contains("RECUT") && !"CUT_ROUND".equals(target.workstationCode())) {
            throw exception(RECUT_PROCESS_INVALID);
        }
        AdminUserRespDTO executor = adminUserApi.getUser(reqVO.getExecutionUserId());
        if (executor == null) {
            throw exception(EXECUTOR_INVALID);
        }
        String executorName = firstNotBlank(reqVO.getExecutionUserName(), executor.getNickname(),
                String.valueOf(reqVO.getExecutionUserId()));

        List<QmsNcDispositionContextRespVO.ScopeCandidate> levelCandidates = buildScopeCandidates(ncr).stream()
                .filter(candidate -> scopeLevel.equals(candidate.getScopeLevel()))
                .toList();
        if (CollUtil.isEmpty(levelCandidates)) {
            throw exception(SCOPE_LEVEL_INVALID, scopeLevel);
        }
        Set<String> selectedKeys = reqVO.getSelectedObjectKeys().stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (selectedKeys.isEmpty()) {
            throw exception(SCOPE_OBJECT_INVALID, "未选择有效对象");
        }
        Map<String, QmsNcDispositionContextRespVO.ScopeCandidate> candidateByKey = levelCandidates.stream()
                .collect(Collectors.toMap(QmsNcDispositionContextRespVO.ScopeCandidate::getObjectKey,
                        candidate -> candidate, (left, right) -> left, LinkedHashMap::new));
        List<String> invalidKeys = selectedKeys.stream().filter(key -> !candidateByKey.containsKey(key)).toList();
        if (CollUtil.isNotEmpty(invalidKeys)) {
            throw exception(SCOPE_OBJECT_INVALID, String.join("、", invalidKeys));
        }

        List<QmsNcDispositionContextRespVO.ScopeCandidate> selectedCandidates = selectedKeys.stream()
                .map(candidateByKey::get)
                .filter(Objects::nonNull)
                .toList();
        Map<String, QmsNcDispositionConfirmReqVO.ScopeRemark> scopeRemarkByKey = buildScopeRemarkMap(reqVO);
        boolean rollPick = pickQualificationService.isPick(ncr) && !"PIECE".equals(scopeLevel);
        boolean legacyPickOnly = !rollPick && allowedDispositionTypes.size() == 1 && allowedDispositionTypes.contains("PICK");
        BigDecimal affectedQty = sumQuantity(legacyPickOnly ? levelCandidates : selectedCandidates);
        BigDecimal selectedQty = sumQuantity(selectedCandidates);
        BigDecimal derivedScrapQty = legacyPickOnly
                ? affectedQty.subtract(selectedQty).max(BigDecimal.ZERO) : BigDecimal.ZERO;
        LocalDateTime now = LocalDateTime.now();
        Long tenantId = firstNonNull(ncr.getTenantId(), TenantContextHolder.getRequiredTenantId());
        String executionNo = buildExecutionNo(ncr.getNcNo());
        String extraJson = rollPick ? JsonUtils.toJsonString(Map.of(
                "pickQualification", true, "scopeSnapshot", selectedCandidates)) : buildExtraJson(reqVO);

        QmsNcDispositionExecutionDO execution = QmsNcDispositionExecutionDO.builder()
                .executionNo(executionNo)
                .ncRecordId(ncr.getId())
                .ncNo(ncr.getNcNo())
                .dispositionType(dispositionType)
                .ngProcessCode(target.processCode())
                .ngProcessName(target.processName())
                .targetWorkstationCode(target.workstationCode())
                .targetWorkstationName(target.workstationName())
                .scopeLevel(scopeLevel)
                .affectedQty(affectedQty)
                .selectedQty(selectedQty)
                .derivedScrapQty(derivedScrapQty)
                .executionStatus(rollPick ? "COMPLETED" : "APPLIED")
                .confirmUserId(SecurityFrameworkUtils.getLoginUserId())
                .confirmUserName(currentUserName())
                .confirmTime(now)
                .executionUserId(reqVO.getExecutionUserId())
                .executionUserName(executorName)
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .extraJson(extraJson)
                .tenantId(tenantId)
                .build();
        executionMapper.insert(execution);

        List<QmsNcDispositionContextRespVO.ScopeCandidate> snapshottedCandidates =
                legacyPickOnly ? levelCandidates : selectedCandidates;
        List<QmsNcDispositionScopeDO> scopes = new ArrayList<>();
        for (QmsNcDispositionContextRespVO.ScopeCandidate candidate : snapshottedCandidates) {
            boolean selected = selectedKeys.contains(candidate.getObjectKey());
            String scopeRole = legacyPickOnly
                    ? (selected ? "PICK_ALLOW" : "PICK_OUTSIDE_SCRAP") : (rollPick ? "PICK_ALLOW" : "SELECTED");
            QmsNcDispositionConfirmReqVO.ScopeRemark scopeRemark = scopeRemarkByKey.get(candidate.getObjectKey());
            String requestedScopeDisposition = normalizeUpper(scopeRemark == null ? null : scopeRemark.getDispositionType());
            String scopeDisposition = selected
                    ? firstNotBlank(requestedScopeDisposition,
                    allowedDispositionTypes.size() == 1 ? allowedDispositionTypes.iterator().next() : null)
                    : "SCRAP";
            if (selected && !allowedDispositionTypes.contains(scopeDisposition)) {
                throw exception(DISPOSITION_INVALID, firstNotBlank(scopeDisposition, candidate.getPieceNo(), candidate.getObjectKey()));
            }
            QmsNcDispositionScopeDO scope = buildScope(execution, candidate, scopeRole, target, tenantId,
                    selected && scopeRemark != null ? scopeRemark.getRemark() : null,
                    firstNotBlank(scopeDisposition, legacyPickOnly ? "PICK" : dispositionType));
            if (rollPick) scope.setExecutionResult("PICK_QUALIFIED");
            scopeMapper.insert(scope);
            scopes.add(scope);
            QmsNcReportGateDO gate = buildAppliedGate(execution, scope, target, tenantId, now);
            if (rollPick) {
                gate.setGateStatus("ALLOW");
            }
            reportGateMapper.insert(gate);
        }

        QmsNcWorkstationCommandDO command = buildAppliedCommand(execution, scopes, reqVO, target, tenantId, now);
        if (rollPick) {
            command.setCommandStatus("APPLIED");
            command.setAckTime(now);
            command.setAckUser(currentUserName());
            command.setAckMessage("范围确认/执行分派完成即确认所选母卷/加工段挑选合格，原始检验结论保留");
            command.setApplyTime(now);
            command.setApplyResult("PICK_QUALIFIED");
        }
        commandMapper.insert(command);
        relationMapper.insert(QmsNcRelationDO.builder()
                .ncRecordId(ncr.getId())
                .ncNo(ncr.getNcNo())
                .relationType("DISPOSITION_EXECUTION")
                .relatedObjectId(execution.getId())
                .relatedObjectNo(execution.getExecutionNo())
                .relatedObjectName(dispositionName(dispositionType) + "执行单")
                .relationStatus(rollPick ? "COMPLETED" : "APPLIED")
                .primaryFlag(true)
                .relationTime(now)
                .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                .relationUserName(currentUserName())
                .remark(reqVO.getRemark())
                .tenantId(tenantId)
                .build());
        relationMapper.insert(QmsNcRelationDO.builder()
                .ncRecordId(ncr.getId())
                .ncNo(ncr.getNcNo())
                .relationType("WORKSTATION_COMMAND")
                .relatedObjectId(command.getId())
                .relatedObjectNo(command.getCommandNo())
                .relatedObjectName(target.workstationName() + "自动下达指令")
                .relationStatus("APPLIED")
                .primaryFlag(false)
                .relationTime(now)
                .relationUserId(SecurityFrameworkUtils.getLoginUserId())
                .relationUserName(currentUserName())
                .remark(rollPick ? "范围确认/执行分派完成即改判合格，原始检验NG保留" : "产品NCR处置范围确认后自动下达并应用工作台指令")
                .tenantId(tenantId)
                .build());

        qmsCutRoundFqcService.applyNcrDispositionResult(ncr, execution, scopes);

        QmsNcRecordHandleReqVO handleReqVO = new QmsNcRecordHandleReqVO();
        handleReqVO.setId(ncr.getId());
        handleReqVO.setOpinion(reqVO.getRemark());
        handleReqVO.setNextHandlerUserId(reqVO.getExecutionUserId());
        handleReqVO.setNextHandlerUserName(executorName);
        handleReqVO.setStockDisposeQty(resolveLegacyDisposeQty(ncr, affectedQty));
        handleReqVO.setFinalDisposeDescription(buildLegacyDisposeDescription(execution, scopes));
        handleReqVO.setDispositionNotifyUserIds(reqVO.getDispositionNotifyUserIds());
        handleReqVO.setDispositionNotifyUserNames(reqVO.getDispositionNotifyUserNames());
        handleReqVO.setTransferRoute(TRANSFER_ROUTE_EXECUTION);
        ncRecordService.handleNcRecord(handleReqVO);
        return buildExecutionResp(execution, command, scopes);
    }

    private QmsNcDispositionExecutionRespVO reassignExistingExecution(
            QmsNcRecordDO ncr, QmsNcDispositionExecutionDO existing, QmsNcDispositionConfirmReqVO reqVO) {
        if (!STATUS_EXECUTION_ASSIGN.equals(ncr.getStatus())) {
            return buildExecutionResp(existing);
        }
        AdminUserRespDTO executor = adminUserApi.getUser(reqVO.getExecutionUserId());
        if (executor == null) {
            throw exception(EXECUTOR_INVALID);
        }
        String executorName = firstNotBlank(reqVO.getExecutionUserName(), executor.getNickname(),
                String.valueOf(reqVO.getExecutionUserId()));
        QmsNcDispositionExecutionDO updateExecution = new QmsNcDispositionExecutionDO();
        updateExecution.setId(existing.getId());
        updateExecution.setExecutionUserId(reqVO.getExecutionUserId());
        updateExecution.setExecutionUserName(executorName);
        updateExecution.setRemark(firstNotBlank(StrUtil.trim(reqVO.getRemark()), existing.getRemark()));
        executionMapper.updateById(updateExecution);
        applyExistingRollPickIfNeeded(ncr, existing);

        QmsNcRecordHandleReqVO handleReqVO = new QmsNcRecordHandleReqVO();
        handleReqVO.setId(ncr.getId());
        handleReqVO.setOpinion(firstNotBlank(reqVO.getRemark(), "复检退回后重新分派执行人"));
        handleReqVO.setNextHandlerUserId(reqVO.getExecutionUserId());
        handleReqVO.setNextHandlerUserName(executorName);
        // 对象数量可能按段/卷计数，重新分派与首次分派保持相同的主单数量口径。
        handleReqVO.setStockDisposeQty(resolveLegacyDisposeQty(ncr,
                firstNonNull(existing.getAffectedQty(), ncr.getDefectQty())));
        handleReqVO.setFinalDisposeDescription(firstNotBlank(ncr.getFinalDisposeDescription(), existing.getRemark(),
                "沿用已确认产品处置范围，重新分派执行人"));
        handleReqVO.setDispositionNotifyUserIds(reqVO.getDispositionNotifyUserIds());
        handleReqVO.setDispositionNotifyUserNames(reqVO.getDispositionNotifyUserNames());
        handleReqVO.setTransferRoute(TRANSFER_ROUTE_EXECUTION);
        ncRecordService.handleNcRecord(handleReqVO);
        return buildExecutionResp(executionMapper.selectById(existing.getId()));
    }

    private void applyExistingRollPickIfNeeded(QmsNcRecordDO ncr, QmsNcDispositionExecutionDO existing) {
        if (!pickQualificationService.isPick(ncr) || "PIECE".equals(existing.getScopeLevel())) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (!"COMPLETED".equals(existing.getExecutionStatus())) {
            executionMapper.updateById(QmsNcDispositionExecutionDO.builder()
                    .id(existing.getId())
                    .executionStatus("COMPLETED")
                    .build());
            existing.setExecutionStatus("COMPLETED");
        }
        for (QmsNcDispositionScopeDO scope : scopeMapper.selectListByExecutionId(existing.getId())) {
            if (!"PICK_QUALIFIED".equals(scope.getExecutionResult())) {
                scopeMapper.updateById(QmsNcDispositionScopeDO.builder()
                        .id(scope.getId())
                        .executionResult("PICK_QUALIFIED")
                        .build());
                scope.setExecutionResult("PICK_QUALIFIED");
            }
        }
        for (QmsNcReportGateDO gate : reportGateMapper.selectListByExecutionId(existing.getId())) {
            if (!"ALLOW".equals(gate.getGateStatus()) || gate.getEffectiveTime() == null) {
                LocalDateTime effectiveTime = firstNonNull(gate.getEffectiveTime(), now);
                reportGateMapper.updateById(QmsNcReportGateDO.builder()
                        .id(gate.getId())
                        .gateStatus("ALLOW")
                        .effectiveTime(effectiveTime)
                        .build());
                gate.setGateStatus("ALLOW");
                gate.setEffectiveTime(effectiveTime);
            }
        }
        QmsNcWorkstationCommandDO command = commandMapper.selectByExecutionId(existing.getId());
        if (command != null && (!"APPLIED".equals(command.getCommandStatus())
                || !"PICK_QUALIFIED".equals(command.getApplyResult()))) {
            commandMapper.updateById(QmsNcWorkstationCommandDO.builder()
                    .id(command.getId())
                    .commandStatus("APPLIED")
                    .ackTime(firstNonNull(command.getAckTime(), now))
                    .ackUser(firstNotBlank(command.getAckUser(), currentUserName()))
                    .ackMessage(firstNotBlank(command.getAckMessage(), "范围确认/执行分派完成即确认挑选合格"))
                    .applyTime(firstNonNull(command.getApplyTime(), now))
                    .applyResult("PICK_QUALIFIED")
                    .build());
        }
    }

    private QmsNcRecordDO validateProductNcr(Long ncRecordId) {
        return validateProductNcr(qmsNcRecordMapper.selectById(ncRecordId));
    }

    private QmsNcRecordDO validateProductNcr(QmsNcRecordDO ncr) {
        if (ncr == null) {
            throw exception(NCR_NOT_EXISTS);
        }
        if (SOURCE_TYPE_RAW_MATERIAL.equalsIgnoreCase(StrUtil.blankToDefault(ncr.getSourceType(), ""))) {
            throw exception(PRODUCT_NCR_REQUIRED);
        }
        return ncr;
    }

    private List<QmsNcDispositionContextRespVO.ScopeCandidate> buildScopeCandidates(QmsNcRecordDO ncr) {
        if (pickQualificationService.supports(ncr)) {
            if (!pickQualificationService.isPick(ncr)) {
                throw exception(new ErrorCode(400, "湿法母批、磨皮/粘胶1加工段当前仅支持挑选合格处置"));
            }
            return pickQualificationService.candidates(ncr);
        }
        ProcessTarget target = resolveProcessTarget(ncr);
        HcBatchTraceRespVO trace = getBatchTrace(ncr.getLotNo());
        Map<String, TraceCandidate> traceCandidates = collectTraceCandidates(trace);
        Set<String> explicitPieces = collectExplicitAffectedPieces(ncr);
        Map<String, QmsNcDispositionContextRespVO.ScopeCandidate> result = new LinkedHashMap<>();

        for (TraceCandidate traceCandidate : traceCandidates.values()) {
            if (!"PIECE".equals(traceCandidate.scopeLevel()) || StrUtil.isBlank(traceCandidate.pieceNo())) {
                continue;
            }
            if (!isSelectableCurrentProcessPiece(trace, traceCandidate.pieceNo(), target)) {
                continue;
            }
            putCandidate(result, buildCandidate(traceCandidate, ncr, "CURRENT_PROCESS_AVAILABLE_PIECE"));
        }

        Map<String, TraceCandidate> tracePieceByNo = traceCandidates.values().stream()
                .filter(candidate -> "PIECE".equals(candidate.scopeLevel()))
                .collect(Collectors.toMap(candidate -> normalizeBatchNo(candidate.pieceNo()),
                        candidate -> candidate, (left, right) -> left));
        for (String explicitPiece : explicitPieces) {
            TraceCandidate traced = tracePieceByNo.get(normalizeBatchNo(explicitPiece));
            if (traced != null && !isSelectableCurrentProcessPiece(trace, traced.pieceNo(), target)) {
                continue;
            }
            if (traced == null && isPieceConfirmedByLaterProcess(trace, explicitPiece, target)) {
                continue;
            }
            if (traced == null) {
                String root = resolveRootBatchNo(trace, ncr.getLotNo());
                String segment = resolveSegmentBatchNo(trace, ncr.getLotNo());
                traced = new TraceCandidate("PIECE", root, segment, explicitPiece, "异常对象 " + explicitPiece,
                        "NG", "检验不合格/异常锁定");
            }
            putCandidate(result, buildCandidate(traced, ncr, "NCR_AFFECTED_OBJECT"));
        }

        return result.values().stream()
                .sorted(Comparator.comparingInt(
                                (QmsNcDispositionContextRespVO.ScopeCandidate candidate) ->
                                        scopeLevelOrder(candidate.getScopeLevel()))
                        .thenComparing(QmsNcDispositionContextRespVO.ScopeCandidate::getObjectKey))
                .toList();
    }

    private boolean isSelectableCurrentProcessPiece(HcBatchTraceRespVO trace, String pieceNo, ProcessTarget target) {
        if (StrUtil.isBlank(pieceNo)) {
            return false;
        }
        if (isPieceConfirmedByLaterProcess(trace, pieceNo, target)) {
            return false;
        }
        int targetOrder = processOrder(target.processCode());
        if (targetOrder >= 999 || trace == null || CollUtil.isEmpty(trace.getTimelineNodes())) {
            return true;
        }
        Integer earliestOrder = trace.getTimelineNodes().stream()
                .filter(node -> matchesPieceNo(pieceNo, node))
                .map(HcBatchTraceRespVO.TimelineNodeRespVO::getProcessOrder)
                .filter(Objects::nonNull)
                .min(Integer::compareTo)
                .orElse(null);
        return earliestOrder == null || earliestOrder <= targetOrder;
    }

    private boolean isPieceConfirmedByLaterProcess(HcBatchTraceRespVO trace, String pieceNo, ProcessTarget target) {
        if (trace == null || CollUtil.isEmpty(trace.getTimelineNodes()) || StrUtil.isBlank(pieceNo)) {
            return false;
        }
        int targetOrder = processOrder(target.processCode());
        if (targetOrder >= 999) {
            return false;
        }
        return trace.getTimelineNodes().stream()
                .filter(node -> node.getProcessOrder() != null && node.getProcessOrder() > targetOrder)
                .filter(this::isConfirmedTimelineNode)
                .anyMatch(node -> matchesPieceNo(pieceNo, node));
    }

    private boolean isConfirmedTimelineNode(HcBatchTraceRespVO.TimelineNodeRespVO node) {
        String status = normalizeUpper(firstNotBlank(node.getStatus(), node.getReportStatus()));
        return status.contains("CONFIRM")
                || status.contains("COMPLET")
                || "OK".equals(status)
                || "PASS".equals(status)
                || "APPROVED".equals(status)
                || StrUtil.isNotBlank(node.getConfirmerName());
    }

    private boolean matchesPieceNo(String pieceNo, HcBatchTraceRespVO.TimelineNodeRespVO node) {
        String normalizedPiece = normalizeBatchNo(pieceNo);
        if (StrUtil.isBlank(normalizedPiece) || node == null) {
            return false;
        }
        for (String value : new String[] {node.getBatchNo(), node.getSourceBatchNo(), node.getParentBatchNo()}) {
            String normalizedValue = normalizeBatchNo(value);
            if (StrUtil.isBlank(normalizedValue)) {
                continue;
            }
            if (Objects.equals(normalizedValue, normalizedPiece)) {
                return true;
            }
            if (normalizedValue.length() > normalizedPiece.length()
                    && normalizedValue.startsWith(normalizedPiece)) {
                return true;
            }
        }
        return false;
    }

    private List<QmsNcDispositionContextRespVO.ScopeCandidate> buildCandidatesFromExecution(Long executionId) {
        return scopeMapper.selectListByExecutionId(executionId).stream().map(scope -> {
            QmsNcDispositionContextRespVO.ScopeCandidate candidate =
                    new QmsNcDispositionContextRespVO.ScopeCandidate();
            candidate.setObjectKey(scope.getObjectKey());
            candidate.setScopeLevel(scope.getScopeLevel());
            candidate.setScopeLevelName(scopeLevelName(scope.getScopeLevel()));
            candidate.setMotherBatchNo(scope.getMotherBatchNo());
            candidate.setSegmentBatchNo(scope.getSegmentBatchNo());
            candidate.setPieceNo(scope.getPieceNo());
            candidate.setSourceObjectType(scope.getSourceObjectType());
            candidate.setSourceObjectId(scope.getSourceObjectId());
            candidate.setSourceObjectNo(scope.getSourceObjectNo());
            candidate.setLabel(scopeLevelName(scope.getScopeLevel()) + " "
                    + firstNotBlank(scope.getPieceNo(), scope.getSegmentBatchNo(), scope.getMotherBatchNo()));
            candidate.setQuantity(scope.getQuantity());
            candidate.setQuantityUnit("MOTHER_BATCH".equals(scope.getScopeLevel()) ? "卷"
                    : "SEGMENT".equals(scope.getScopeLevel()) ? "段" : "片");
            candidate.setCurrentStatus(scope.getExecutionResult());
            candidate.setCurrentStatusName(scope.getExecutionResult());
            candidate.setSelectable(false);
            return candidate;
        }).toList();
    }

    private HcBatchTraceRespVO getBatchTrace(String lotNo) {
        if (StrUtil.isBlank(lotNo)) {
            return null;
        }
        HcBatchTraceQueryReqVO query = new HcBatchTraceQueryReqVO();
        query.setBatchNo(lotNo);
        try {
            return batchTraceService.getTrace(query);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private Map<String, TraceCandidate> collectTraceCandidates(HcBatchTraceRespVO trace) {
        Map<String, TraceCandidate> result = new LinkedHashMap<>();
        if (trace == null || CollUtil.isEmpty(trace.getTreeNodes())) {
            return result;
        }
        for (HcBatchTraceRespVO.TreeNodeRespVO root : trace.getTreeNodes()) {
            String rootBatchNo = root.getBatchNo();
            putTraceCandidate(result, new TraceCandidate("MOTHER_BATCH", rootBatchNo, null, null,
                    "母批 " + rootBatchNo, root.getStatus(), root.getStatusText()));
            for (HcBatchTraceRespVO.TreeNodeRespVO segment : root.getChildren()) {
                String segmentBatchNo = segment.getBatchNo();
                putTraceCandidate(result, new TraceCandidate("SEGMENT", rootBatchNo, segmentBatchNo, null,
                        "母批段 " + segmentBatchNo, segment.getStatus(), segment.getStatusText()));
                for (HcBatchTraceRespVO.TreeNodeRespVO piece : segment.getChildren()) {
                    String pieceNo = piece.getBatchNo();
                    putTraceCandidate(result, new TraceCandidate("PIECE", rootBatchNo, segmentBatchNo, pieceNo,
                            "片号 " + pieceNo, piece.getStatus(), piece.getStatusText()));
                    for (HcBatchTraceRespVO.TreeNodeRespVO finalPiece : piece.getChildren()) {
                        putTraceCandidate(result, new TraceCandidate("PIECE", rootBatchNo, segmentBatchNo,
                                finalPiece.getBatchNo(), "裁切片号 " + finalPiece.getBatchNo(),
                                finalPiece.getStatus(), finalPiece.getStatusText()));
                    }
                }
            }
        }
        return result;
    }

    private Set<String> collectExplicitAffectedPieces(QmsNcRecordDO ncr) {
        Set<String> pieces = new LinkedHashSet<>();
        HcPressSlotAbnormalLockDO abnormalLock = ncr.getSourceId() == null ? null
                : pressSlotAbnormalLockMapper.selectByAbnormalFaiId(ncr.getSourceId());
        if (abnormalLock != null) {
            for (HcPressSlotAbnormalLockItemDO item :
                    pressSlotAbnormalLockItemMapper.selectListByLockId(abnormalLock.getId())) {
                addBatchNo(pieces, item.getProductionBatchNo());
            }
        }
        if (ncr.getSourceId() != null && StrUtil.isNotBlank(ncr.getSourceBizType())) {
            try {
                QmsProductAbnormalEventDetailRespVO detail =
                        productAbnormalEventService.getDetail(ncr.getSourceBizType(), ncr.getSourceId());
                if (detail != null && CollUtil.isNotEmpty(detail.getAbnormalItems())) {
                    detail.getAbnormalItems().forEach(item -> addBatchNo(pieces, item.getTargetNo()));
                }
            } catch (RuntimeException ignored) {
                // 来源详情可能已归档；继续使用 NCR 与批次追溯快照。
            }
        }
        return pieces;
    }

    private QmsNcDispositionContextRespVO.ScopeCandidate buildCandidate(
            TraceCandidate source, QmsNcRecordDO ncr, String sourceObjectType) {
        QmsNcDispositionContextRespVO.ScopeCandidate candidate =
                new QmsNcDispositionContextRespVO.ScopeCandidate();
        candidate.setScopeLevel(source.scopeLevel());
        candidate.setScopeLevelName(scopeLevelName(source.scopeLevel()));
        candidate.setMotherBatchNo(source.motherBatchNo());
        candidate.setSegmentBatchNo(source.segmentBatchNo());
        candidate.setPieceNo(source.pieceNo());
        String objectNo = firstNotBlank(source.pieceNo(), source.segmentBatchNo(), source.motherBatchNo());
        candidate.setObjectKey(source.scopeLevel() + ":" + objectNo);
        candidate.setLabel(source.label());
        candidate.setSourceObjectType(sourceObjectType);
        candidate.setSourceObjectId(ncr.getSourceId());
        candidate.setSourceObjectNo(objectNo);
        candidate.setQuantity(resolveCandidateQuantity(source, ncr));
        candidate.setQuantityUnit("片");
        candidate.setCurrentStatus(source.status());
        candidate.setCurrentStatusName(source.statusName());
        candidate.setSelectable(true);
        return candidate;
    }

    private BigDecimal resolveCandidateQuantity(TraceCandidate candidate, QmsNcRecordDO ncr) {
        if ("PIECE".equals(candidate.scopeLevel())) {
            return BigDecimal.ONE;
        }
        if ("SEGMENT".equals(candidate.scopeLevel())) {
            return BigDecimal.ONE;
        }
        return ncr.getDefectQty() != null && ncr.getDefectQty().compareTo(BigDecimal.ZERO) > 0
                ? ncr.getDefectQty() : BigDecimal.ONE;
    }

    private QmsNcDispositionScopeDO buildScope(QmsNcDispositionExecutionDO execution,
                                                QmsNcDispositionContextRespVO.ScopeCandidate candidate,
                                                String scopeRole, ProcessTarget target, Long tenantId,
                                                String scopeRemark,
                                                String dispositionType) {
        return QmsNcDispositionScopeDO.builder()
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .ncRecordId(execution.getNcRecordId())
                .ncNo(execution.getNcNo())
                .dispositionType(firstNotBlank(dispositionType, execution.getDispositionType()))
                .scopeLevel(candidate.getScopeLevel())
                .objectKey(candidate.getObjectKey())
                .motherBatchNo(candidate.getMotherBatchNo())
                .segmentBatchNo(candidate.getSegmentBatchNo())
                .pieceNo(candidate.getPieceNo())
                .sourceObjectType(candidate.getSourceObjectType())
                .sourceObjectId(candidate.getSourceObjectId())
                .sourceObjectNo(candidate.getSourceObjectNo())
                .scopeRole(scopeRole)
                .quantity(candidate.getQuantity())
                .executionResult("APPLIED")
                .targetWorkstationCode(target.workstationCode())
                .remark(StrUtil.trimToNull(scopeRemark))
                .tenantId(tenantId)
                .build();
    }

    private QmsNcReportGateDO buildAppliedGate(QmsNcDispositionExecutionDO execution,
                                                QmsNcDispositionScopeDO scope,
                                                ProcessTarget target, Long tenantId,
                                                LocalDateTime applyTime) {
        String scopeDisposition = firstNotBlank(scope.getDispositionType(), execution.getDispositionType());
        return QmsNcReportGateDO.builder()
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .ncRecordId(execution.getNcRecordId())
                .ncNo(execution.getNcNo())
                .dispositionType(scopeDisposition)
                .scopeLevel(scope.getScopeLevel())
                .objectKey(scope.getObjectKey())
                .ngProcessCode(target.processCode())
                .targetWorkstationCode(target.workstationCode())
                .gateStatus("APPLIED")
                .effectMode(resolveEffectMode(scopeDisposition, scope.getScopeRole()))
                .concessionFlag("CONCESSION".equals(scopeDisposition))
                .effectiveTime(applyTime)
                .tenantId(tenantId)
                .build();
    }

    private QmsNcWorkstationCommandDO buildAppliedCommand(QmsNcDispositionExecutionDO execution,
                                                           List<QmsNcDispositionScopeDO> scopes,
                                                           QmsNcDispositionConfirmReqVO reqVO,
                                                           ProcessTarget target, Long tenantId,
                                                           LocalDateTime applyTime) {
        String commandNo = "NCRCMD-" + normalizeNo(execution.getNcNo()) + "-01";
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("executionNo", execution.getExecutionNo());
        snapshot.put("dispositionType", execution.getDispositionType());
        snapshot.put("ngProcessCode", execution.getNgProcessCode());
        snapshot.put("scopeLevel", execution.getScopeLevel());
        snapshot.put("recutTargetSize", reqVO.getRecutTargetSize());
        snapshot.put("recutTolerance", reqVO.getRecutTolerance());
        snapshot.put("recutQty", reqVO.getRecutQty());
        snapshot.put("concessionReason", reqVO.getConcessionReason());
        snapshot.put("scopes", scopes.stream().map(this::buildCommandScopeSnapshot).toList());
        return QmsNcWorkstationCommandDO.builder()
                .commandNo(commandNo)
                .executionId(execution.getId())
                .executionNo(execution.getExecutionNo())
                .ncRecordId(execution.getNcRecordId())
                .ncNo(execution.getNcNo())
                .dispositionType(execution.getDispositionType())
                .commandType(resolveCommandType(execution.getDispositionType()))
                .targetWorkstationCode(target.workstationCode())
                .effectMode(resolveEffectMode(execution.getDispositionType(), "SELECTED"))
                .scopeSnapshotJson(JsonUtils.toJsonString(snapshot))
                .commandStatus("APPLIED")
                .commandVersion(1)
                .idempotencyKey("NCR:" + execution.getNcRecordId() + ":" + execution.getDispositionType() + ":V1")
                .dispatchTime(applyTime)
                .ackTime(applyTime)
                .ackUser(currentUserName())
                .ackMessage("产品NCR处置范围确认后自动下达并应用")
                .applyTime(applyTime)
                .applyResult("APPLIED")
                .tenantId(tenantId)
                .build();
    }

    private QmsNcDispositionExecutionRespVO buildExecutionResp(QmsNcDispositionExecutionDO execution) {
        QmsNcWorkstationCommandDO command = commandMapper.selectByExecutionId(execution.getId());
        return buildExecutionResp(execution, command, scopeMapper.selectListByExecutionId(execution.getId()));
    }

    private QmsNcDispositionExecutionRespVO buildExecutionResp(QmsNcDispositionExecutionDO execution,
                                                                QmsNcWorkstationCommandDO command,
                                                                List<QmsNcDispositionScopeDO> scopes) {
        QmsNcDispositionExecutionRespVO respVO = new QmsNcDispositionExecutionRespVO();
        respVO.setId(execution.getId());
        respVO.setExecutionNo(execution.getExecutionNo());
        respVO.setNcRecordId(execution.getNcRecordId());
        respVO.setNcNo(execution.getNcNo());
        respVO.setDispositionType(execution.getDispositionType());
        respVO.setNgProcessCode(execution.getNgProcessCode());
        respVO.setNgProcessName(execution.getNgProcessName());
        respVO.setTargetWorkstationCode(execution.getTargetWorkstationCode());
        respVO.setTargetWorkstationName(execution.getTargetWorkstationName());
        respVO.setScopeLevel(execution.getScopeLevel());
        respVO.setAffectedQty(execution.getAffectedQty());
        respVO.setSelectedQty(execution.getSelectedQty());
        respVO.setDerivedScrapQty(execution.getDerivedScrapQty());
        respVO.setExecutionStatus(execution.getExecutionStatus());
        respVO.setConfirmUserId(execution.getConfirmUserId());
        respVO.setConfirmUserName(execution.getConfirmUserName());
        respVO.setConfirmTime(execution.getConfirmTime());
        respVO.setExecutionUserId(execution.getExecutionUserId());
        respVO.setExecutionUserName(execution.getExecutionUserName());
        respVO.setRemark(execution.getRemark());
        if (command != null) {
            respVO.setCommandNo(command.getCommandNo());
            respVO.setCommandStatus(command.getCommandStatus());
        }
        respVO.setScopes(scopes.stream().map(this::buildScopeResp).toList());
        return respVO;
    }

    private QmsNcDispositionExecutionRespVO.ScopeItem buildScopeResp(QmsNcDispositionScopeDO scope) {
        QmsNcDispositionExecutionRespVO.ScopeItem item = new QmsNcDispositionExecutionRespVO.ScopeItem();
        item.setObjectKey(scope.getObjectKey());
        item.setScopeLevel(scope.getScopeLevel());
        item.setDispositionType(scope.getDispositionType());
        item.setMotherBatchNo(scope.getMotherBatchNo());
        item.setSegmentBatchNo(scope.getSegmentBatchNo());
        item.setPieceNo(scope.getPieceNo());
        item.setScopeRole(scope.getScopeRole());
        item.setQuantity(scope.getQuantity());
        item.setExecutionResult(scope.getExecutionResult());
        item.setRemark(scope.getRemark());
        return item;
    }

    private Map<String, QmsNcDispositionConfirmReqVO.ScopeRemark> buildScopeRemarkMap(QmsNcDispositionConfirmReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getScopeRemarks())) {
            return Map.of();
        }
        return reqVO.getScopeRemarks().stream()
                .filter(item -> item != null && StrUtil.isNotBlank(item.getObjectKey()))
                .collect(Collectors.toMap(item -> StrUtil.trim(item.getObjectKey()),
                        item -> item, (left, right) -> right, LinkedHashMap::new));
    }

    private Map<String, Object> buildCommandScopeSnapshot(QmsNcDispositionScopeDO scope) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("objectKey", scope.getObjectKey());
        item.put("dispositionType", scope.getDispositionType());
        item.put("scopeRole", scope.getScopeRole());
        item.put("quantity", scope.getQuantity());
        item.put("remark", scope.getRemark());
        return item;
    }

    private String buildExtraJson(QmsNcDispositionConfirmReqVO reqVO) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("recutTargetSize", reqVO.getRecutTargetSize());
        extra.put("recutTolerance", reqVO.getRecutTolerance());
        extra.put("recutQty", reqVO.getRecutQty());
        extra.put("concessionReason", reqVO.getConcessionReason());
        return JsonUtils.toJsonString(extra);
    }

    private String buildLegacyDisposeDescription(QmsNcDispositionExecutionDO execution,
                                                  List<QmsNcDispositionScopeDO> scopes) {
        long selectedCount = scopes.stream().filter(scope -> !"PICK_OUTSIDE_SCRAP".equals(scope.getScopeRole())).count();
        long outsideCount = scopes.stream().filter(scope -> "PICK_OUTSIDE_SCRAP".equals(scope.getScopeRole())).count();
        String summary = execution.getExecutionNo() + "；" + scopeLevelName(execution.getScopeLevel())
                + "选中" + selectedCount + "项";
        if (outsideCount > 0) {
            summary += "；挑选范围外待报废" + outsideCount + "项";
        }
        Map<String, Long> dispositionCounts = scopes.stream()
                .collect(Collectors.groupingBy(scope -> firstNotBlank(scope.getDispositionType(),
                                execution.getDispositionType(), "-"),
                        LinkedHashMap::new, Collectors.counting()));
        if (!dispositionCounts.isEmpty()) {
            summary += "；处置：" + dispositionCounts.entrySet().stream()
                    .map(entry -> dispositionName(entry.getKey()) + entry.getValue() + "项")
                    .collect(Collectors.joining("、"));
        }
        return summary + "；" + execution.getRemark();
    }

    private BigDecimal resolveLegacyDisposeQty(QmsNcRecordDO ncr, BigDecimal affectedQty) {
        BigDecimal fallback = affectedQty == null || affectedQty.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ONE : affectedQty;
        if (ncr.getDefectQty() != null && ncr.getDefectQty().compareTo(BigDecimal.ZERO) > 0) {
            return fallback.min(ncr.getDefectQty());
        }
        return fallback;
    }

    private ProcessTarget resolveProcessTarget(QmsNcRecordDO ncr) {
        String name = firstNotBlank(ncr.getProcessName(), ncr.getSourceBizTypeName(), ncr.getSourceBizType());
        String upper = normalizeUpper(name);
        if (name.contains("配料") || upper.contains("FORMULA")) {
            return new ProcessTarget("FORMULA", "配料", "FORMULA", "配料报工工作台");
        }
        if (name.contains("湿法") || upper.contains("WET")) {
            return new ProcessTarget("WET", "湿法", "WET", "湿法报工工作台");
        }
        if (name.contains("磨皮") || upper.contains("ROUGH_GRINDING")) {
            return new ProcessTarget("ROUGH_GRINDING", "磨皮", "ROUGH_GRINDING", "磨皮操作看板");
        }
        if (name.contains("粘胶2") || upper.contains("ADHESIVE2")) {
            return new ProcessTarget("ADHESIVE2", "粘胶2", "ADHESIVE2", "粘胶2报工工作台");
        }
        if (name.contains("粘胶") || upper.contains("ADHESIVE")) {
            return new ProcessTarget("ADHESIVE1", "粘胶1", "ADHESIVE1", "粘胶1报工工作台");
        }
        if (name.contains("分切") || upper.contains("SLITTING")) {
            return new ProcessTarget("SLITTING", "分切", "SLITTING", "分切报工工作台");
        }
        if (name.contains("压槽") || upper.contains("PRESS_SLOT")) {
            return new ProcessTarget("PRESS_SLOT", "压槽", "PRESS_SLOT", "压槽报工工作台");
        }
        if (name.contains("裁切") || upper.contains("CUT_ROUND")) {
            return new ProcessTarget("CUT_ROUND", "裁切", "CUT_ROUND", "裁切报工工作台");
        }
        return new ProcessTarget(firstNotBlank(ncr.getSourceBizType(), "QUALITY_NCR"), name,
                "QUALITY_NCR", "质量处置工作台");
    }

    private String resolveCommandType(String dispositionType) {
        if (parseDispositionSet(dispositionType).size() > 1) {
            return "NCR_MIXED_DISPOSITION";
        }
        return switch (dispositionType) {
            case "PICK" -> "UNLOCK_SELECTED_AND_SCRAP_OUTSIDE";
            case "REWORK" -> "INVALIDATE_AND_REOPEN_NG_PROCESS";
            case "RECUT" -> "ISSUE_RECUT_INSTRUCTION";
            case "SCRAP" -> "SCRAP_SELECTED_SCOPE";
            case "CONCESSION" -> "CONCESSION_UNLOCK";
            default -> "NCR_DISPOSITION";
        };
    }

    private String resolveEffectMode(String dispositionType, String scopeRole) {
        if (parseDispositionSet(dispositionType).size() > 1) {
            return "MIXED_BY_SCOPE";
        }
        if ("SCRAP".equals(dispositionType) || "PICK_OUTSIDE_SCRAP".equals(scopeRole)) {
            return "ON_COMMAND_APPLY";
        }
        return "AFTER_WORKSTATION_ACK";
    }

    private String buildCandidateSourceDescription(
            List<QmsNcDispositionContextRespVO.ScopeCandidate> candidates) {
        Set<String> sources = candidates.stream()
                .map(QmsNcDispositionContextRespVO.ScopeCandidate::getSourceObjectType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> names = new ArrayList<>();
        if (sources.contains("BATCH_TRACE")) {
            names.add("批次追溯树");
        }
        if (sources.contains("CURRENT_PROCESS_AVAILABLE_PIECE")) {
            names.add("当前工序未被后续确认片号");
        }
        if (sources.contains("NCR_AFFECTED_OBJECT")) {
            names.add("检验不合格/异常锁定对象");
        }
        if (sources.contains("NCR_LOT")) {
            names.add("NCR来源批次");
        }
        return String.join(" + ", names);
    }

    private BigDecimal sumQuantity(List<QmsNcDispositionContextRespVO.ScopeCandidate> candidates) {
        return candidates.stream().map(QmsNcDispositionContextRespVO.ScopeCandidate::getQuantity)
                .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String buildExecutionNo(String ncNo) {
        return "NCREX-" + normalizeNo(ncNo);
    }

    private String normalizeNo(String value) {
        String normalized = StrUtil.blankToDefault(value, String.valueOf(System.currentTimeMillis()))
                .replaceAll("[^A-Za-z0-9_-]", "_");
        return normalized.length() <= 48 ? normalized : normalized.substring(normalized.length() - 48);
    }

    private void putCandidate(Map<String, QmsNcDispositionContextRespVO.ScopeCandidate> result,
                              QmsNcDispositionContextRespVO.ScopeCandidate candidate) {
        result.putIfAbsent(candidate.getObjectKey(), candidate);
    }

    private void putTraceCandidate(Map<String, TraceCandidate> result, TraceCandidate candidate) {
        String objectNo = firstNotBlank(candidate.pieceNo(), candidate.segmentBatchNo(), candidate.motherBatchNo());
        if (StrUtil.isNotBlank(objectNo)) {
            result.putIfAbsent(candidate.scopeLevel() + ":" + objectNo, candidate);
        }
    }

    private void addBatchNo(Set<String> values, String value) {
        String normalized = normalizeBatchNo(value);
        if (StrUtil.isNotBlank(normalized)) {
            values.add(normalized);
        }
    }

    private String normalizeBatchNo(String value) {
        return StrUtil.trimToEmpty(value).replace(" ", "").toUpperCase(Locale.ROOT);
    }

    private String resolveRootBatchNo(HcBatchTraceRespVO trace, String fallback) {
        if (trace != null && trace.getOverview() != null) {
            return firstNotBlank(trace.getOverview().getRootBatchNo(), fallback);
        }
        return fallback;
    }

    private String resolveSegmentBatchNo(HcBatchTraceRespVO trace, String fallback) {
        if (trace != null && trace.getOverview() != null) {
            return firstNotBlank(trace.getOverview().getSegmentBatchNo());
        }
        return "";
    }

    private int scopeLevelOrder(String scopeLevel) {
        return switch (scopeLevel) {
            case "MOTHER_BATCH" -> 1;
            case "SEGMENT" -> 2;
            case "PIECE" -> 3;
            default -> 9;
        };
    }

    private int processOrder(String processCode) {
        return PROCESS_ORDER_BY_TARGET.getOrDefault(normalizeUpper(processCode), 999);
    }

    private String scopeLevelName(String scopeLevel) {
        return switch (normalizeUpper(scopeLevel)) {
            case "MOTHER_BATCH" -> "母批";
            case "SEGMENT" -> "母批段";
            case "PIECE" -> "片号";
            default -> scopeLevel;
        };
    }

    private String dispositionName(String dispositionType) {
        Set<String> values = parseDispositionSet(dispositionType);
        if (values.size() > 1) {
            return values.stream().map(this::singleDispositionName).collect(Collectors.joining("、"));
        }
        return singleDispositionName(values.stream().findFirst().orElse(dispositionType));
    }

    private String singleDispositionName(String dispositionType) {
        return switch (normalizeUpper(dispositionType)) {
            case "PICK" -> "挑选";
            case "REWORK" -> "返工";
            case "RECUT" -> "改切";
            case "SCRAP" -> "报废";
            case "CONCESSION" -> "特采";
            default -> StrUtil.blankToDefault(dispositionType, "-");
        };
    }

    private String normalizeDispositionString(String value) {
        return parseDispositionSet(value).stream().collect(Collectors.joining(","));
    }

    private Set<String> parseDispositionSet(String value) {
        if (StrUtil.isBlank(value)) {
            return Set.of();
        }
        return Arrays.stream(StrUtil.trimToEmpty(value).split(","))
                .map(this::normalizeUpper)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeUpper(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private String currentUserName() {
        return StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return StrUtil.trim(value);
            }
        }
        return "";
    }

    private record TraceCandidate(String scopeLevel, String motherBatchNo, String segmentBatchNo, String pieceNo,
                                  String label, String status, String statusName) {
    }

    private record ProcessTarget(String processCode, String processName, String workstationCode,
                                 String workstationName) {
    }
}
