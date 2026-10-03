package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderInventoryLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderStatusReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcDiscretePostProcessVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryLocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderInventoryLockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.discrete.HcDiscretePostProcessInspectionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn.HcInvTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcNgInventoryLocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.HcNgInventoryPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.discrete.HcDiscretePostProcessInspectionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.discrete.HcDiscretePostProcessMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.dto.HcWipOutputPostReq;
import cn.iocoder.yudao.module.mes.service.hc.nginventory.HcNgInventoryService;
import cn.iocoder.yudao.module.mes.service.hc.planorder.HcPlanOrderService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcDiscretePostProcessServiceImpl implements HcDiscretePostProcessService {

    private static final String PLAN_MODE_DISCRETE = "DISCRETE_POST";
    private static final String SOURCE_TYPE_DISCRETE = "NG_INVENTORY";
    private static final String PROD_TYPE_TRIAL_PROCESS = "TRIAL_PROCESS";
    private static final String PROD_TYPE_TRIAL_PROCESS_NAME = "试验加工";
    private static final String SOURCE_POOL_NG = "NG";
    private static final String SOURCE_POOL_WIP = "WIP";
    private static final String SOURCE_POOL_ALL = "ALL";
    private static final String LOCK_MARK = "DISCRETE_POST_PROCESS";
    private static final String CANDIDATE_TYPE_LOCK = "LOCK";
    private static final String CANDIDATE_TYPE_STOCK = "STOCK";
    private static final String CANDIDATE_TYPE_NG_PIECE = "NG_PIECE";
    private static final String STOCK_TYPE_WIP = "WIP";
    private static final String STOCK_TYPE_NG_PIECE = "NG_PIECE";
    private static final String STATUS_RELEASED = "RELEASED";
    private static final String STATUS_WAIT_SHELF = "WAIT_SHELF";
    private static final String STATUS_WAIT_FREEZE_SHELF = "WAIT_FREEZE_SHELF";
    private static final String STATUS_STORED = "STORED";
    private static final String STATUS_FROZEN = "FROZEN";
    private static final String STATUS_REWORKING = "REWORKING";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final String OP_STATUS_FINISHED = "FINISHED";
    private static final String LOCK_STATUS_ACTIVE = "ACTIVE";
    private static final String LOCK_STATUS_CONSUMED = "CONSUMED";
    private static final String REPORT_STATUS_CONFIRMED = "CONFIRMED";
    private static final String STOCK_POST_STATUS_POSTED = "POSTED";
    private static final String STOCK_POST_STATUS_WAIT_NG_SHELF = "WAIT_NG_SHELF";
    private static final String BIZ_STATUS_NG_EMPTY = "NG_EMPTY";
    private static final String SOURCE_TABLE_SLITTING_SLICE = "mes_sfc_slitting_slice_record";
    private static final String SOURCE_TABLE_PRESS_SLOT_REPORT = "mes_sfc_press_slot_report";
    private static final String SOURCE_TABLE_ADHESIVE2_REPORT = "mes_sfc_adhesive2_report";
    private static final String SOURCE_TABLE_CUT_ROUND_REPORT = "mes_sfc_cut_round_report";
    private static final String SOURCE_TABLE_NG_PIECE = "mes_inv_ng_piece";
    private static final String SOURCE_TYPE_NG_INVENTORY = "NG_INVENTORY";
    private static final String TXN_TYPE_NG_REWORK_PICK = "NG_REWORK_PICK";
    private static final String TXN_TYPE_NG_REWORK_OUT = "NG_REWORK_OUT";
    private static final int DEFAULT_CANDIDATE_LIMIT = 200;
    private static final int MAX_CANDIDATE_LIMIT = 1000;
    private static final DateTimeFormatter PLAN_NO_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter TASK_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter TXN_NO_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<OperationConfig> OPERATION_CONFIGS = List.of(
            new OperationConfig("WC-GROOVE", "压槽", 6, "SLITTING", "WC-SLIT", "分切",
                    "PRESS_SLOT", SOURCE_TABLE_PRESS_SLOT_REPORT),
            new OperationConfig("WC-ADH2", "粘胶2", 7, "PRESS_SLOT", "WC-GROOVE", "压槽",
                    "ADHESIVE2", SOURCE_TABLE_ADHESIVE2_REPORT),
            new OperationConfig("WC-CUT", "裁切", 8, "ADHESIVE2", "WC-ADH2", "粘胶2",
                    "CUT_ROUND", SOURCE_TABLE_CUT_ROUND_REPORT)
    );
    private static final Map<String, OperationConfig> CONFIG_BY_OP_CODE = OPERATION_CONFIGS.stream()
            .collect(Collectors.toMap(OperationConfig::opCode, Function.identity()));

    @Resource
    private HcDiscretePostProcessMapper hcDiscretePostProcessMapper;
    @Resource
    private HcDiscretePostProcessInspectionMapper hcDiscretePostProcessInspectionMapper;
    @Resource
    private HcInvStockMapper hcInvStockMapper;
    @Resource
    private HcInvTxnLogMapper hcInvTxnLogMapper;
    @Resource
    private HcNgInventoryPieceMapper hcNgInventoryPieceMapper;
    @Resource
    private HcNgInventoryLocationMapper hcNgInventoryLocationMapper;
    @Resource
    private HcInvStockService hcInvStockService;
    @Resource
    private HcPlanOrderService hcPlanOrderService;
    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;
    @Resource
    private HcPressSlotReportMapper hcPressSlotReportMapper;
    @Resource
    private HcAdhesive2ReportMapper hcAdhesive2ReportMapper;
    @Resource
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Resource
    private HcNgInventoryService hcNgInventoryService;

    @Override
    public List<HcDiscretePostProcessVO.StockRespVO> getCandidateList(HcDiscretePostProcessVO.SourceQueryReqVO reqVO) {
        HcDiscretePostProcessVO.SourceQueryReqVO query = reqVO == null
                ? new HcDiscretePostProcessVO.SourceQueryReqVO() : reqVO;
        String sourcePool = normalizeSourcePool(query.getSourcePool());
        String targetOpCode = normalizeOpCode(query.getTargetOpCode());
        if (StrUtil.isNotBlank(targetOpCode)) {
            query.setTargetOpCode(targetOpCode);
            OperationConfig config = requiredConfig(targetOpCode);
            if (StrUtil.isBlank(query.getSourceOpCode())) {
                query.setSourceOpCode(config.sourceOpCode());
            }
            if (query.getSourceTypes() == null || query.getSourceTypes().isEmpty()) {
                query.setSourceTypes(List.of(config.sourceType()));
            }
        }
        int limit = query.getLimit() == null ? DEFAULT_CANDIDATE_LIMIT : query.getLimit();
        limit = Math.max(1, Math.min(limit, MAX_CANDIDATE_LIMIT));
        List<HcDiscretePostProcessVO.StockRespVO> candidates = new ArrayList<>();
        if (SOURCE_POOL_NG.equals(sourcePool) || SOURCE_POOL_ALL.equals(sourcePool)) {
            candidates.addAll(hcDiscretePostProcessMapper.selectNgPieceCandidateList(query, limit));
        }
        if (SOURCE_POOL_WIP.equals(sourcePool) || SOURCE_POOL_ALL.equals(sourcePool)) {
            if (StrUtil.isBlank(query.getQualityStatus())) {
                query.setQualityStatus("合格");
            }
            candidates.addAll(hcDiscretePostProcessMapper.selectCandidateList(query, limit));
            if (StrUtil.isNotBlank(query.getTargetOpCode())) {
                candidates.addAll(hcDiscretePostProcessMapper.selectPendingLockCandidateList(query, limit));
            }
        }
        return candidates.stream()
                .sorted(Comparator
                        .comparing(HcDiscretePostProcessVO.StockRespVO::getReportTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(row -> row.getBatchNo() == null ? "" : row.getBatchNo())
                        .thenComparing(row -> row.getCandidateKey() == null ? "" : row.getCandidateKey()))
                .limit(limit)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createPlan(HcDiscretePostProcessVO.CreatePlanReqVO reqVO) {
        List<OperationConfig> operations = normalizeOperationConfigs(reqVO.getOperationCodes());
        OperationConfig firstOperation = operations.get(0);
        List<SelectedSource> sources = loadAndValidateSources(reqVO.getNgPieceIds(), reqVO.getStockIds(),
                reqVO.getSourceLockIds(), firstOperation);
        String selectedModelPrefix = resolveSelectedModelPrefix(sources.get(0));
        String planModel = normalizeManualPlanModel(reqVO.getPlanModel(), selectedModelPrefix);
        String executionRequirement = normalizeManualText(reqVO.getExecutionRequirement());
        if (StrUtil.isBlank(executionRequirement)) {
            throw invalidParamException("执行要求不能为空");
        }
        BigDecimal targetQty = sources.stream()
                .map(SelectedSource::qty)
                .map(this::defaultDecimal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        HcPlanOrderSaveReqVO planReq = new HcPlanOrderSaveReqVO();
        planReq.setPlanMode(PLAN_MODE_DISCRETE);
        planReq.setPlanNo(generateNextDiscretePlanNo());
        planReq.setSourceType(SOURCE_TYPE_DISCRETE);
        planReq.setPlanStatus("DRAFT");
        planReq.setProductionStartDate(firstNonNull(reqVO.getProductionStartDate(), LocalDate.now()));
        planReq.setProductionEndDate(firstNonNull(reqVO.getProductionEndDate(), planReq.getProductionStartDate()));
        planReq.setMaterialId(resolveSelectedMaterialId(sources.get(0)));
        planReq.setMaterialCode(firstNotBlank(resolveSelectedMaterialCode(sources.get(0)), ""));
        planReq.setMaterialName(firstNotBlank(resolveSelectedMaterialName(sources.get(0)), ""));
        planReq.setModelCode(planModel);
        planReq.setModelName(planModel);
        planReq.setSizeSpec(firstNotBlank(resolveSelectedSpecSize(sources.get(0)), "多规格"));
        planReq.setSizeName(firstNotBlank(resolveSelectedSpecSize(sources.get(0)), "多规格"));
        planReq.setRecipeCode(resolveSelectedRecipeCode(sources.get(0)));
        planReq.setRecipeName(resolveSelectedRecipeName(sources.get(0)));
        planReq.setProdType(PROD_TYPE_TRIAL_PROCESS);
        planReq.setProdTypeName(PROD_TYPE_TRIAL_PROCESS_NAME);
        planReq.setTargetQty(targetQty);
        planReq.setTargetUnitCode("pcs");
        planReq.setTargetUnitName("片");
        planReq.setTargetUom("pcs");
        planReq.setFgDeductQty(BigDecimal.ZERO);
        planReq.setNetPlanQty(targetQty);
        planReq.setOperationCount(operations.size());
        planReq.setFrontProcessFlag(Boolean.FALSE);
        planReq.setPostProcessFlag(Boolean.TRUE);
        planReq.setRemark(executionRequirement);
        planReq.setRouteSnapshotJson(buildDiscreteRouteSnapshotJson(sources, operations, selectedModelPrefix,
                planModel, executionRequirement, reqVO.getRemark()));
        planReq.setOperations(buildPlanOperations(operations, targetQty, executionRequirement));
        planReq.setInventoryLocks(buildInitialLocks(sources, firstOperation));

        releaseTakenOverLocks(sources, firstOperation);
        Long planId = hcPlanOrderService.createHcPlanOrder(planReq);
        if (Boolean.TRUE.equals(reqVO.getReleaseNow())) {
            HcPlanOrderStatusReqVO statusReq = new HcPlanOrderStatusReqVO();
            statusReq.setId(planId);
            statusReq.setPlanStatus(STATUS_RELEASED);
            statusReq.setReasonRemark("离散后加工计划创建后自动下达");
            hcPlanOrderService.updatePlanStatus(statusReq);
        }
        pickupNgPiecesForPlan(planId);
        return planId;
    }

    @Override
    public List<HcDiscretePostProcessVO.TaskRespVO> getTaskList(HcDiscretePostProcessVO.TaskQueryReqVO reqVO) {
        reqVO.setOpCode(normalizeOpCode(reqVO.getOpCode()));
        requiredConfig(reqVO.getOpCode());
        reqVO.setTaskStatus(normalizeTaskStatus(reqVO.getTaskStatus()));
        return hcDiscretePostProcessMapper.selectTaskList(reqVO);
    }

    @Override
    public List<HcDiscretePostProcessVO.SourceRespVO> getSourceList(Long planOperationId, String taskStatus, String keyword) {
        if (planOperationId == null) {
            throw invalidParamException("计划工序ID不能为空");
        }
        return hcDiscretePostProcessMapper.selectSourceList(planOperationId, normalizeTaskStatus(taskStatus), StrUtil.trimToNull(keyword));
    }

    @Override
    public HcDiscretePostProcessVO.SourceRespVO scanSource(Long planOperationId, String pieceNo) {
        if (planOperationId == null) {
            throw invalidParamException("计划工序ID不能为空");
        }
        String normalizedPieceNo = StrUtil.trimToNull(pieceNo);
        if (StrUtil.isBlank(normalizedPieceNo)) {
            throw invalidParamException("扫码片号不能为空");
        }
        Long lockId = parseLong(normalizedPieceNo);
        HcDiscretePostProcessVO.SourceRespVO source =
                hcDiscretePostProcessMapper.selectSourceByPiece(planOperationId, normalizedPieceNo, lockId);
        if (source == null) {
            throw invalidParamException("当前计划工序下未找到片号：" + normalizedPieceNo);
        }
        return source;
    }

    @Override
    public HcDiscretePostProcessVO.SourceRespVO scanSourceByTargetOp(String targetOpCode, String pieceNo) {
        String normalizedOpCode = normalizeOpCode(targetOpCode);
        requiredConfig(normalizedOpCode);
        String normalizedPieceNo = StrUtil.trimToNull(pieceNo);
        if (StrUtil.isBlank(normalizedPieceNo)) {
            throw invalidParamException("扫码片号不能为空");
        }
        Long lockId = parseLong(normalizedPieceNo);
        HcDiscretePostProcessVO.SourceRespVO source =
                hcDiscretePostProcessMapper.selectSourceByOpAndPiece(normalizedOpCode, normalizedPieceNo, lockId);
        if (source == null) {
            throw invalidParamException("当前离散" + requiredConfig(normalizedOpCode).opName()
                    + "任务下未找到片号：" + normalizedPieceNo);
        }
        return source;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcDiscretePostProcessVO.SourceRespVO report(HcDiscretePostProcessVO.ReportReqVO reqVO) {
        HcPlanOrderOperationDO snapshot = hcPlanOrderOperationMapper.selectById(reqVO.getPlanOperationId());
        if (snapshot == null) throw invalidParamException("计划工序不存在");
        HcPlanOrderDO plan = hcPlanOrderMapper.selectByIdForUpdate(snapshot.getPlanId());
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectByIdForUpdate(reqVO.getPlanOperationId());
        if (operation == null || Boolean.TRUE.equals(operation.getDeleted())) {
            throw invalidParamException("计划工序不存在");
        }
        OperationConfig config = requiredConfig(operation.getOpCode());
        if (plan == null || Boolean.TRUE.equals(plan.getDeleted())) {
            throw invalidParamException("生产计划不存在");
        }
        if (!STATUS_RELEASED.equals(firstNotBlank(plan.getPlanStatus(), "DRAFT"))) {
            throw invalidParamException("离散后加工计划未下达，不能报工");
        }
        HcPlanOrderInventoryLockDO lock = resolveReportLock(reqVO, operation);
        validateSourceForOperation(config, lock);
        BigDecimal consumeQty = resolveReportQty(reqVO, lock);
        boolean ngResult = isNgResult(reqVO);
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(reqVO.getConfirmerName(), reqVO.getRecorderName(),
                SecurityFrameworkUtils.getLoginUserNickname(), "system");
        Long reportId = insertReport(config, plan, operation, lock, consumeQty, ngResult, reqVO, now, operatorId, operatorName);
        ConsumptionResult consumption = consumeSourceForReport(lock, consumeQty, config, reportId, plan, now,
                operatorId, operatorName);
        updateReportConsumeInfo(config, reportId, lock, consumeQty, consumption);
        if (ngResult) {
            markReportNg(config, reportId, plan, operation, now);
        } else {
            HcInvStockDO outputStock = postOutputWip(config, plan, operation, lock, reportId, consumeQty, reqVO, now, operatorId, operatorName);
            updateReportOutputInfo(config, reportId, outputStock);
            autoLockNextOperation(plan, operation, outputStock, config, now, operatorId, operatorName);
        }
        refreshOperationFinishedIfNeeded(operation, now, operatorId, operatorName);
        return getSourceList(operation.getId(), "ALL", null).stream()
                .filter(item -> Objects.equals(item.getLockId(), lock.getId()))
                .findFirst()
                .orElseGet(() -> scanSource(operation.getId(), resolvePieceNo(lock)));
    }

    @Override
    public List<HcDiscretePostProcessVO.SourceRespVO> getInspectionTaskList(Long planOperationId,
                                                                            String inspectionType,
                                                                            String keyword) {
        if (planOperationId == null) {
            throw invalidParamException("计划工序ID不能为空");
        }
        return hcDiscretePostProcessMapper.selectInspectionSourceList(
                planOperationId,
                normalizeInspectionType(inspectionType, null),
                StrUtil.trimToNull(keyword));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcDiscretePostProcessVO.InspectionRespVO createInspectionTask(HcDiscretePostProcessVO.InspectionReqVO reqVO) {
        HcPlanOrderInventoryLockDO lock = hcPlanOrderInventoryLockMapper.selectByIdForUpdate(reqVO.getLockId());
        if (lock == null || Boolean.TRUE.equals(lock.getDeleted())) {
            throw invalidParamException("来源锁定明细不存在");
        }
        if (lock.getConsumeReportId() == null || !LOCK_STATUS_CONSUMED.equals(firstNotBlank(lock.getLockStatus(), ""))) {
            throw invalidParamException("请先完成该片号报工后再送检");
        }
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(lock.getPlanOperationId());
        HcPlanOrderDO plan = operation == null ? null : hcPlanOrderMapper.selectById(operation.getPlanId());
        if (operation == null || plan == null) {
            throw invalidParamException("送检对应的计划或工序不存在");
        }
        OperationConfig config = requiredConfig(operation.getOpCode());
        String inspectionType = normalizeInspectionType(reqVO.getInspectionType(), "PROCESS");
        HcDiscretePostProcessInspectionDO existed = hcDiscretePostProcessInspectionMapper.selectLatestByLockIdAndType(lock.getId(), inspectionType);
        if (existed != null && !"CANCELLED".equals(firstNotBlank(existed.getInspectionStatus(), ""))) {
            syncCutRoundInspectionIfNeeded(config, lock, existed);
            return buildInspectionResp(existed);
        }

        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        HcDiscretePostProcessInspectionDO task = new HcDiscretePostProcessInspectionDO();
        task.setTenantId(firstNonNull(plan.getTenantId(), lock.getTenantId(), 0L));
        task.setInspectionTaskNo("DPI-" + TASK_NO_TIME_FORMATTER.format(now) + "-" + lock.getId());
        task.setPlanId(plan.getId());
        task.setPlanNo(plan.getPlanNo());
        task.setPlanOperationId(operation.getId());
        task.setOperationCode(operation.getOpCode());
        task.setOperationName(operation.getOpName());
        task.setSourceLockId(lock.getId());
        task.setSourceStockId(lock.getStockId());
        task.setSourceBatchNo(resolvePieceNo(lock));
        task.setSourceParentBatchNo(lock.getSourceBatchNo());
        task.setSourcePlanNo(lock.getSourcePlanNo());
        task.setReportSourceType(config.reportSourceType());
        task.setReportSourceId(lock.getConsumeReportId());
        task.setInspectionType(inspectionType);
        task.setInspectionStatus("WAIT_INSPECTION");
        task.setReportDate(now.toLocalDate());
        task.setReportTime(now);
        task.setReporterName(operatorName);
        task.setRemark(reqVO.getRemark());
        task.setCreator(operatorId == null ? "" : String.valueOf(operatorId));
        task.setUpdater(operatorId == null ? "" : String.valueOf(operatorId));
        hcDiscretePostProcessInspectionMapper.insert(task);
        syncCutRoundInspectionIfNeeded(config, lock, task);
        return buildInspectionResp(task);
    }

    private List<SelectedSource> loadAndValidateSources(List<Long> ngPieceIds, List<Long> stockIds,
                                                        List<Long> sourceLockIds,
                                                        OperationConfig firstOperation) {
        List<SelectedSource> sources = new ArrayList<>();
        loadAndValidateNgPieces(ngPieceIds, firstOperation).forEach(piece ->
                sources.add(new SelectedSource(null, null, piece, defaultDecimal(piece.getPieceQty()))));
        loadAndValidateStocks(stockIds, firstOperation).forEach(stock ->
                sources.add(new SelectedSource(stock, null, null, defaultDecimal(stock.getAvailableQty()))));
        sources.addAll(loadAndValidatePendingLocks(sourceLockIds, firstOperation));
        if (sources.isEmpty()) {
            throw invalidParamException("请选择来源片号");
        }
        validateDistinctSources(sources);
        validateSameProductIdentity(sources);
        return sources;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<HcNgInventoryPieceDO> loadAndValidateNgPieces(List<Long> ngPieceIds, OperationConfig firstOperation) {
        List<Long> normalizedIds = normalizeIds(ngPieceIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        List<HcNgInventoryPieceDO> pieces = hcNgInventoryPieceMapper.selectListByIdsForUpdate(normalizedIds);
        Map<Long, HcNgInventoryPieceDO> pieceMap = pieces.stream()
                .filter(item -> item != null && item.getId() != null)
                .collect(Collectors.toMap(HcNgInventoryPieceDO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<HcPlanOrderInventoryLockDO> usedLocks =
                hcPlanOrderInventoryLockMapper.selectUsedListByNgPieceIdsForUpdate(normalizedIds);
        if (!usedLocks.isEmpty()) {
            String usedPieceText = usedLocks.stream()
                    .map(HcPlanOrderInventoryLockDO::getBatchNo)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .limit(5)
                    .collect(Collectors.joining("、"));
            throw invalidParamException("来源NG片号已被离散后加工计划占用：" + firstNotBlank(usedPieceText, "-"));
        }
        List<HcNgInventoryPieceDO> orderedPieces = new ArrayList<>();
        for (Long id : normalizedIds) {
            HcNgInventoryPieceDO piece = pieceMap.get(id);
            if (piece == null || Boolean.TRUE.equals(piece.getDeleted())) {
                throw invalidParamException("来源NG片号不存在或已删除：" + id);
            }
            validateNgPieceCandidate(piece, firstOperation);
            orderedPieces.add(piece);
        }
        return orderedPieces;
    }

    private List<HcInvStockDO> loadAndValidateStocks(List<Long> stockIds, OperationConfig firstOperation) {
        List<Long> normalizedIds = normalizeIds(stockIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        List<HcInvStockDO> stocks = hcInvStockMapper.selectBatchIds(normalizedIds);
        Map<Long, HcInvStockDO> stockMap = stocks.stream()
                .filter(item -> item != null && item.getId() != null)
                .collect(Collectors.toMap(HcInvStockDO::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<HcInvStockDO> orderedStocks = new ArrayList<>();
        for (Long id : normalizedIds) {
            HcInvStockDO stock = stockMap.get(id);
            if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
                throw invalidParamException("来源库存不存在或已删除：" + id);
            }
            validateCandidateStock(stock, firstOperation);
            orderedStocks.add(stock);
        }
        return orderedStocks;
    }

    private List<SelectedSource> loadAndValidatePendingLocks(List<Long> sourceLockIds, OperationConfig firstOperation) {
        List<Long> normalizedIds = normalizeIds(sourceLockIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }
        List<SelectedSource> sources = new ArrayList<>();
        for (Long lockId : normalizedIds) {
            HcPlanOrderInventoryLockDO sourceLock = hcPlanOrderInventoryLockMapper.selectByIdForUpdate(lockId);
            if (sourceLock == null || Boolean.TRUE.equals(sourceLock.getDeleted())) {
                throw invalidParamException("来源未报工锁定明细不存在：" + lockId);
            }
            if (sourceLock.getStockId() == null) {
                throw invalidParamException("来源未报工锁定明细缺少库存ID：" + lockId);
            }
            HcInvStockDO stock = hcInvStockMapper.selectByIdForUpdate(sourceLock.getStockId());
            if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
                throw invalidParamException("来源未报工锁定片号库存不存在：" + resolveLockPieceNo(sourceLock));
            }
            validatePendingSourceLock(sourceLock, stock, firstOperation);
            sources.add(new SelectedSource(stock, sourceLock, null, resolveLockRemainingQty(sourceLock)));
        }
        return sources;
    }

    private void validateNgPieceCandidate(HcNgInventoryPieceDO piece, OperationConfig firstOperation) {
        String status = firstNotBlank(piece.getStatus(), "");
        if (!equalsAnyIgnoreCase(status, STATUS_WAIT_SHELF, STATUS_WAIT_FREEZE_SHELF, STATUS_STORED, STATUS_FROZEN)) {
            throw invalidParamException("来源NG片号状态不可派工：" + firstNotBlank(piece.getPieceNo(), "-")
                    + "，当前状态：" + firstNotBlank(status, "-"));
        }
        if (!equalsIgnoreCase(firstOperation.sourceType(), piece.getProcessType())) {
            throw invalidParamException(firstOperation.opName() + "离散计划只能选择"
                    + firstOperation.sourceOpName() + "产生的NG片号，当前片号："
                    + firstNotBlank(piece.getPieceNo(), "-"));
        }
        if (defaultDecimal(piece.getPieceQty()).compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("来源NG片号数量无效：" + firstNotBlank(piece.getPieceNo(), "-"));
        }
    }

    private void validateCandidateStock(HcInvStockDO stock, OperationConfig firstOperation) {
        if (!STOCK_TYPE_WIP.equalsIgnoreCase(firstNotBlank(stock.getStockType(), ""))) {
            throw invalidParamException("只能选择 WIP 中间品库存：" + stock.getBatchNo());
        }
        if (defaultDecimal(stock.getAvailableQty()).compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("来源片号没有可用数量：" + stock.getBatchNo());
        }
        if (!"合格".equals(firstNotBlank(stock.getQualityStatus(), "合格"))) {
            throw invalidParamException("只能选择质量状态为合格的中间品：" + stock.getBatchNo());
        }
        if (!equalsIgnoreCase(firstOperation.sourceType(), stock.getSourceType())
                || !equalsIgnoreCase(firstOperation.sourceOpCode(), stock.getOpCode())) {
            throw invalidParamException(firstOperation.opName() + "离散计划只能选择"
                    + firstOperation.sourceOpName() + "产出的中间品，当前片号："
                    + firstNotBlank(stock.getBatchNo(), "-"));
        }
    }

    private void validatePendingSourceLock(HcPlanOrderInventoryLockDO lock, HcInvStockDO stock,
                                           OperationConfig firstOperation) {
        String pieceNo = resolveLockPieceNo(lock);
        if (StrUtil.isNotBlank(lock.getLockType()) && !STOCK_TYPE_WIP.equalsIgnoreCase(lock.getLockType())) {
            throw invalidParamException("来源未报工锁定不是 WIP 中间品：" + pieceNo);
        }
        if (StrUtil.isNotBlank(lock.getStockType()) && !STOCK_TYPE_WIP.equalsIgnoreCase(lock.getStockType())) {
            throw invalidParamException("来源未报工锁定库存类型不是 WIP：" + pieceNo);
        }
        if (!STOCK_TYPE_WIP.equalsIgnoreCase(firstNotBlank(stock.getStockType(), ""))) {
            throw invalidParamException("来源未报工锁定片号库存不是 WIP：" + pieceNo);
        }
        if (!LOCK_STATUS_ACTIVE.equals(firstNotBlank(lock.getLockStatus(), ""))) {
            throw invalidParamException("只能选择首工序未报工的 ACTIVE 锁定片号：" + pieceNo);
        }
        if (lock.getConsumeReportId() != null) {
            throw invalidParamException("来源片号已报工，不能作为首工序未报工来源：" + pieceNo);
        }
        if (resolveLockRemainingQty(lock).compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("来源未报工锁定片号没有剩余数量：" + pieceNo);
        }
        if (!equalsIgnoreCase(firstOperation.opCode(), lock.getTargetOpCode())) {
            throw invalidParamException("来源未报工锁定片号必须处于" + firstOperation.opName()
                    + "待报工，当前目标工序：" + firstNotBlank(lock.getTargetOpName(), lock.getTargetOpCode(), "-"));
        }
        String sourceType = firstNotBlank(lock.getSourceType(), stock.getSourceType());
        String sourceOpCode = firstNotBlank(lock.getOpCode(), stock.getOpCode());
        if (!equalsIgnoreCase(firstOperation.sourceType(), sourceType)
                || !equalsIgnoreCase(firstOperation.sourceOpCode(), sourceOpCode)) {
            throw invalidParamException(firstOperation.opName() + "离散计划只能选择"
                    + firstOperation.sourceOpName() + "库存或" + firstOperation.opName()
                    + "未报工片号，当前片号：" + pieceNo);
        }
        if (!"合格".equals(firstNotBlank(stock.getQualityStatus(), "合格"))) {
            throw invalidParamException("只能选择质量状态为合格的首工序未报工片号：" + pieceNo);
        }
        if (firstNonNull(lock.getSourceId(), stock.getSourceId()) == null) {
            throw invalidParamException("来源未报工片号缺少来源报工ID，无法追溯：" + pieceNo);
        }
    }

    private void validateDistinctSources(List<SelectedSource> sources) {
        Set<Long> stockIds = new HashSet<>();
        Set<Long> ngPieceIds = new HashSet<>();
        Set<String> pieceNos = new HashSet<>();
        for (SelectedSource source : sources) {
            Long stockId = source.stock() == null ? null : source.stock().getId();
            if (stockId != null && !stockIds.add(stockId)) {
                throw invalidParamException("来源片号重复选择，库存ID：" + stockId);
            }
            Long ngPieceId = source.ngPiece() == null ? null : source.ngPiece().getId();
            if (ngPieceId != null && !ngPieceIds.add(ngPieceId)) {
                throw invalidParamException("来源NG片号重复选择，NG片号ID：" + ngPieceId);
            }
            String pieceNo = resolveSelectedPieceNo(source);
            if (StrUtil.isNotBlank(pieceNo) && !pieceNos.add(pieceNo)) {
                throw invalidParamException("来源片号重复选择：" + pieceNo);
            }
        }
    }

    private void validateSameProductIdentity(List<SelectedSource> sources) {
        SelectedSource first = sources.get(0);
        for (SelectedSource source : sources) {
            String pieceNo = resolveSelectedPieceNo(source);
            assertSameIfPresent("型号前三位", resolveSelectedModelPrefix(first), resolveSelectedModelPrefix(source), pieceNo);
        }
    }

    private List<OperationConfig> normalizeOperationConfigs(List<String> operationCodes) {
        if (operationCodes == null || operationCodes.isEmpty()) {
            throw invalidParamException("请选择离散后加工工序");
        }
        Set<String> normalizedCodes = operationCodes.stream()
                .map(this::normalizeOpCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (normalizedCodes.isEmpty()) {
            throw invalidParamException("请选择有效的离散后加工工序");
        }
        List<OperationConfig> configs = normalizedCodes.stream()
                .map(this::requiredConfig)
                .sorted(Comparator.comparing(OperationConfig::opSeq))
                .toList();
        ensureContinuousOperationChain(configs);
        return configs;
    }

    private void ensureContinuousOperationChain(List<OperationConfig> configs) {
        Set<String> selected = configs.stream().map(OperationConfig::opCode).collect(Collectors.toCollection(HashSet::new));
        int minSeq = configs.get(0).opSeq();
        int maxSeq = configs.get(configs.size() - 1).opSeq();
        List<String> missing = OPERATION_CONFIGS.stream()
                .filter(config -> config.opSeq() >= minSeq && config.opSeq() <= maxSeq)
                .filter(config -> !selected.contains(config.opCode()))
                .map(OperationConfig::opName)
                .toList();
        if (!missing.isEmpty()) {
            throw invalidParamException("离散后加工工序必须连续，请补选：" + String.join("、", missing));
        }
    }

    private List<HcPlanOrderOperationReqVO> buildPlanOperations(List<OperationConfig> operations, BigDecimal targetQty,
                                                                String executionRequirement) {
        return operations.stream().map(config -> {
            HcPlanOrderOperationReqVO operation = new HcPlanOrderOperationReqVO();
            operation.setOpSeq(config.opSeq());
            operation.setOpCode(config.opCode());
            operation.setOpName(config.opName());
            operation.setWorkCenterCode(config.opCode());
            operation.setWorkCenterName(config.opName());
            operation.setYieldRate(BigDecimal.ONE);
            operation.setRequiredQty(targetQty);
            operation.setLockedQty(BigDecimal.ZERO);
            operation.setDispatchQty(BigDecimal.ZERO);
            operation.setUnitCode("pcs");
            operation.setUnitName("片");
            operation.setUom("pcs");
            operation.setInstructionText(firstNotBlank(executionRequirement, "离散后加工计划，按片号扫码报工"));
            operation.setHasLock(Boolean.FALSE);
            operation.setSort(config.opSeq());
            return operation;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<HcPlanOrderInventoryLockReqVO> buildInitialLocks(List<SelectedSource> sources, OperationConfig firstOperation) {
        return sources.stream().map(source -> {
            if (source.ngPiece() != null) {
                return buildNgPieceInitialLock(source, firstOperation);
            }
            HcInvStockDO stock = source.stock();
            HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
            BigDecimal lockQty = defaultDecimal(source.qty());
            HcPlanOrderInventoryLockReqVO lock = new HcPlanOrderInventoryLockReqVO();
            lock.setTargetOpCode(firstOperation.opCode());
            lock.setTargetOpName(firstOperation.opName());
            lock.setLockType(STOCK_TYPE_WIP);
            lock.setStockId(stock.getId());
            lock.setStockType(firstNotBlank(sourceLock == null ? null : sourceLock.getStockType(), stock.getStockType()));
            lock.setSourceType(firstNotBlank(sourceLock == null ? null : sourceLock.getSourceType(), stock.getSourceType()));
            lock.setSourceTable(firstNotBlank(sourceLock == null ? null : sourceLock.getSourceTable(), stock.getSourceTable()));
            lock.setSourceId(firstNonNull(sourceLock == null ? null : sourceLock.getSourceId(), stock.getSourceId()));
            lock.setSourceReportId(firstNonNull(sourceLock == null ? null : sourceLock.getSourceReportId(), stock.getSourceReportId()));
            lock.setSourcePlanId(firstNonNull(sourceLock == null ? null : sourceLock.getSourcePlanId(), stock.getSourcePlanId()));
            lock.setSourcePlanNo(firstNotBlank(sourceLock == null ? null : sourceLock.getSourcePlanNo(), stock.getSourcePlanNo()));
            lock.setSourcePlanOperationId(firstNonNull(sourceLock == null ? null : sourceLock.getSourcePlanOperationId(),
                    stock.getSourcePlanOperationId()));
            lock.setSourceBatchNo(resolveSelectedPieceNo(source));
            lock.setOpSeq(firstNonNull(sourceLock == null ? null : sourceLock.getOpSeq(), stock.getOpSeq()));
            lock.setOpCode(firstNotBlank(sourceLock == null ? null : sourceLock.getOpCode(), stock.getOpCode()));
            lock.setOpName(firstNotBlank(sourceLock == null ? null : sourceLock.getOpName(), stock.getOpName()));
            lock.setSegmentCode(firstNotBlank(sourceLock == null ? null : sourceLock.getSegmentCode(), stock.getSegmentCode()));
            lock.setSegmentName(firstNotBlank(sourceLock == null ? null : sourceLock.getSegmentName(), stock.getSegmentName()));
            lock.setThickness(firstNonNull(sourceLock == null ? null : sourceLock.getThickness(), stock.getThickness()));
            lock.setLotNo(resolveSelectedPieceNo(source));
            lock.setBatchNo(resolveSelectedPieceNo(source));
            lock.setMaterialId(firstNonNull(sourceLock == null ? null : sourceLock.getMaterialId(), stock.getMaterialId()));
            lock.setMaterialCode(resolveSelectedMaterialCode(source));
            lock.setMaterialName(firstNotBlank(sourceLock == null ? null : sourceLock.getMaterialName(), stock.getMaterialName()));
            lock.setModelNo(resolveSelectedModelNo(source));
            lock.setRecipeCode(firstNotBlank(sourceLock == null ? null : sourceLock.getRecipeCode(), stock.getRecipeCode()));
            lock.setSizeSpec(resolveSelectedSpecSize(source));
            lock.setProductionDate(firstNonNull(sourceLock == null ? null : sourceLock.getProductionDate(), stock.getProductionDate()));
            lock.setExpiryDate(firstNonNull(sourceLock == null ? null : sourceLock.getExpiryDate(), stock.getExpiryDate()));
            lock.setLocationCode(firstNotBlank(sourceLock == null ? null : sourceLock.getLocationCode(), stock.getLocationCode()));
            lock.setLocationName(firstNotBlank(sourceLock == null ? null : sourceLock.getLocationName(), stock.getLocationName()));
            lock.setOwnerId(firstNonNull(sourceLock == null ? null : sourceLock.getOwnerId(), stock.getOwnerId()));
            lock.setOwnerCode(firstNotBlank(sourceLock == null ? null : sourceLock.getOwnerCode(), stock.getOwnerCode()));
            lock.setOwnerName(firstNotBlank(sourceLock == null ? null : sourceLock.getOwnerName(), stock.getOwnerName()));
            lock.setAvailableQty(lockQty);
            lock.setLockQty(lockQty);
            lock.setConsumedQty(BigDecimal.ZERO);
            lock.setReleasedQty(BigDecimal.ZERO);
            lock.setRemainingQty(lockQty);
            lock.setUnitCode(firstNotBlank(sourceLock == null ? null : sourceLock.getUnitCode(), sourceLock == null ? null : sourceLock.getUom(),
                    stock.getUom(), "pcs"));
            lock.setUnitName("片");
            lock.setUom(firstNotBlank(sourceLock == null ? null : sourceLock.getUom(), sourceLock == null ? null : sourceLock.getUnitCode(),
                    stock.getUom(), "pcs"));
            lock.setLockStatus(LOCK_STATUS_ACTIVE);
            lock.setRemark(LOCK_MARK + "；离散后加工指定片号锁定；来源母批："
                    + firstNotBlank(stock.getSourceParentBatchNo(), "-")
                    + (sourceLock == null ? "" : "；接管原未报工锁定：" + sourceLock.getId()
                    + "；原计划：" + firstNotBlank(sourceLock.getTargetPlanNo(), "-")));
            return lock;
        }).collect(Collectors.toCollection(ArrayList::new));
    }

    private HcPlanOrderInventoryLockReqVO buildNgPieceInitialLock(SelectedSource source, OperationConfig firstOperation) {
        HcNgInventoryPieceDO piece = source.ngPiece();
        BigDecimal lockQty = defaultDecimal(source.qty());
        HcPlanOrderInventoryLockReqVO lock = new HcPlanOrderInventoryLockReqVO();
        lock.setTargetOpCode(firstOperation.opCode());
        lock.setTargetOpName(firstOperation.opName());
        lock.setLockType(STOCK_TYPE_NG_PIECE);
        lock.setStockId(piece.getStockId());
        lock.setNgPieceId(piece.getId());
        lock.setStockType(STOCK_TYPE_NG_PIECE);
        lock.setSourceType(piece.getProcessType());
        lock.setSourceTable(piece.getSourceTable());
        lock.setSourceId(piece.getSourceId());
        lock.setSourceReportId(piece.getSourceReportId());
        lock.setSourcePlanId(piece.getSourcePlanId());
        lock.setSourcePlanNo(piece.getSourcePlanNo());
        lock.setSourcePlanOperationId(piece.getSourcePlanOperationId());
        lock.setSourceBatchNo(firstNotBlank(piece.getSourceParentBatchNo(), piece.getSourceBatchNo(), piece.getPieceNo()));
        lock.setOpSeq(resolveNgSourceOpSeq(piece.getProcessType()));
        lock.setOpCode(resolveNgSourceOpCode(piece.getProcessType()));
        lock.setOpName(firstNotBlank(piece.getProcessName(), firstOperation.sourceOpName()));
        lock.setLotNo(piece.getPieceNo());
        lock.setBatchNo(piece.getPieceNo());
        lock.setMaterialId(piece.getMaterialId());
        lock.setMaterialCode(piece.getMaterialCode());
        lock.setMaterialName(piece.getMaterialName());
        lock.setModelNo(piece.getModelNo());
        lock.setLocationCode(piece.getCurrentLocationCode());
        lock.setLocationName(piece.getCurrentLocationName());
        lock.setAvailableQty(lockQty);
        lock.setLockQty(lockQty);
        lock.setConsumedQty(BigDecimal.ZERO);
        lock.setReleasedQty(BigDecimal.ZERO);
        lock.setRemainingQty(lockQty);
        lock.setUnitCode("pcs");
        lock.setUnitName("片");
        lock.setUom("pcs");
        lock.setLockStatus(LOCK_STATUS_ACTIVE);
        lock.setRemark(LOCK_MARK + "；NG库片号锁定；NG状态："
                + firstNotBlank(piece.getStatus(), "-")
                + "；原分段批号：" + firstNotBlank(piece.getSourceParentBatchNo(), piece.getSourceBatchNo(), "-")
                + "；原计划：" + firstNotBlank(piece.getSourcePlanNo(), "-"));
        return lock;
    }

    private void releaseTakenOverLocks(List<SelectedSource> sources, OperationConfig firstOperation) {
        List<SelectedSource> lockSources = sources.stream()
                .filter(source -> source.sourceLock() != null)
                .toList();
        if (lockSources.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        for (SelectedSource source : lockSources) {
            HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
            hcInvStockService.releasePlanLockedWip(sourceLock, source.qty(),
                    "离散后加工接管首工序未报工片号；片号：" + resolveSelectedPieceNo(source)
                            + "；首工序：" + firstOperation.opName()
                            + "；原计划：" + firstNotBlank(sourceLock.getTargetPlanNo(), "-"),
                    now, operatorId, operatorName);
        }
    }

    private void pickupNgPiecesForPlan(Long planId) {
        List<HcPlanOrderInventoryLockDO> ngLocks = hcPlanOrderInventoryLockMapper.selectListByPlanId(planId).stream()
                .filter(this::isNgPieceLock)
                .filter(lock -> LOCK_STATUS_ACTIVE.equals(firstNotBlank(lock.getLockStatus(), LOCK_STATUS_ACTIVE)))
                .filter(lock -> defaultDecimal(lock.getRemainingQty()).compareTo(BigDecimal.ZERO) > 0)
                .filter(lock -> StrUtil.isBlank(lock.getLockTxnNo()))
                .toList();
        if (ngLocks.isEmpty()) {
            return;
        }
        HcPlanOrderDO plan = hcPlanOrderMapper.selectById(planId);
        if (plan == null || Boolean.TRUE.equals(plan.getDeleted())) {
            throw invalidParamException("离散后加工计划不存在，无法下架NG片号");
        }
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = SecurityFrameworkUtils.getLoginUserId();
        String operatorName = firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "system");
        for (HcPlanOrderInventoryLockDO ngLock : ngLocks) {
            HcPlanOrderInventoryLockDO lock = hcPlanOrderInventoryLockMapper.selectByIdForUpdate(ngLock.getId());
            if (lock == null || !isNgPieceLock(lock) || StrUtil.isNotBlank(lock.getLockTxnNo())) {
                continue;
            }
            OperationConfig config = requiredConfig(lock.getTargetOpCode());
            HcNgInventoryPieceDO piece = loadNgPieceForLock(lock, "下架");
            validateNgPieceCandidate(piece, config);
            BigDecimal pickQty = resolveNgPickupQty(lock, piece);
            String txnNo = "NGREWORKPICK-" + TXN_NO_TIME_FORMATTER.format(now) + "-" + lock.getId();
            HcInvStockDO stock = piece.getStockId() == null ? null : hcInvStockMapper.selectByIdForUpdate(piece.getStockId());
            boolean physicalShelf = equalsAnyIgnoreCase(piece.getStatus(), STATUS_STORED, STATUS_FROZEN);
            if (physicalShelf && stock == null) {
                throw invalidParamException("NG库片号库存余额不存在，不能下架：" + firstNotBlank(piece.getPieceNo(), "-"));
            }
            if (stock != null) {
                pickupNgPieceStock(stock, piece, pickQty, txnNo, config, plan, lock, now, operatorId, operatorName);
                decreaseNgLocationOccupiedIfNeeded(piece);
            } else {
                insertNgPieceLogicalTxnLog(piece, pickQty, txnNo, TXN_TYPE_NG_REWORK_PICK, config, plan, lock, now,
                        operatorId, operatorName,
                        "待上架NG片号转离散后加工计划；原NG状态：" + firstNotBlank(piece.getStatus(), "-"));
            }
            updateNgPiecePicked(piece, now, operatorId);
            HcPlanOrderInventoryLockDO updateLock = new HcPlanOrderInventoryLockDO();
            updateLock.setId(lock.getId());
            updateLock.setLockTxnNo(txnNo);
            updateLock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
            updateLock.setUpdateTime(now);
            hcPlanOrderInventoryLockMapper.updateById(updateLock);
        }
    }

    private HcNgInventoryPieceDO loadNgPieceForLock(HcPlanOrderInventoryLockDO lock, String actionName) {
        if (lock.getNgPieceId() == null) {
            throw invalidParamException("NG库片号锁定缺少逐片ID，无法" + actionName + "：" + resolvePieceNo(lock));
        }
        HcNgInventoryPieceDO piece = hcNgInventoryPieceMapper.selectByIdForUpdate(lock.getNgPieceId());
        if (piece == null || Boolean.TRUE.equals(piece.getDeleted())) {
            throw invalidParamException("NG库片号不存在或已删除，无法" + actionName + "：" + resolvePieceNo(lock));
        }
        return piece;
    }

    private BigDecimal resolveNgPickupQty(HcPlanOrderInventoryLockDO lock, HcNgInventoryPieceDO piece) {
        BigDecimal pickQty = defaultDecimal(lock.getRemainingQty());
        if (pickQty.compareTo(BigDecimal.ZERO) <= 0) {
            pickQty = defaultDecimal(lock.getLockQty());
        }
        BigDecimal pieceQty = defaultDecimal(piece.getPieceQty());
        if (pickQty.compareTo(BigDecimal.ZERO) <= 0 || pieceQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("NG库片号数量无效：" + firstNotBlank(piece.getPieceNo(), "-"));
        }
        if (pieceQty.compareTo(pickQty) < 0) {
            throw invalidParamException("NG库片号数量不足，片号：" + firstNotBlank(piece.getPieceNo(), "-")
                    + "，库存 " + pieceQty + "，本次 " + pickQty);
        }
        return pickQty;
    }

    private BigDecimal resolveLockRemainingQty(HcPlanOrderInventoryLockDO lock) {
        BigDecimal remainingQty = defaultDecimal(lock.getRemainingQty());
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            return remainingQty;
        }
        return defaultDecimal(lock.getLockQty())
                .subtract(defaultDecimal(lock.getConsumedQty()))
                .subtract(defaultDecimal(lock.getReleasedQty()))
                .max(BigDecimal.ZERO);
    }

    private String resolveSelectedPieceNo(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        HcNgInventoryPieceDO ngPiece = source.ngPiece();
        if (ngPiece != null) {
            return firstNotBlank(ngPiece.getPieceNo(), ngPiece.getSourceBatchNo());
        }
        if (sourceLock != null) {
            return firstNotBlank(sourceLock.getBatchNo(), sourceLock.getSourceBatchNo(), sourceLock.getLotNo(),
                    stock == null ? null : stock.getBatchNo(), stock == null ? null : stock.getSourceBatchNo());
        }
        return firstNotBlank(stock == null ? null : stock.getBatchNo(), stock == null ? null : stock.getSourceBatchNo());
    }

    private String resolveLockPieceNo(HcPlanOrderInventoryLockDO lock) {
        if (lock == null) {
            return "-";
        }
        return firstNotBlank(lock.getBatchNo(), lock.getSourceBatchNo(), lock.getLotNo(), "-");
    }

    private String resolveSelectedMaterialCode(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        HcNgInventoryPieceDO ngPiece = source.ngPiece();
        return firstNotBlank(sourceLock == null ? null : sourceLock.getMaterialCode(),
                ngPiece == null ? null : ngPiece.getMaterialCode(),
                stock == null ? null : stock.getMaterialCode());
    }

    private Long resolveSelectedMaterialId(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        HcNgInventoryPieceDO ngPiece = source.ngPiece();
        return firstNonNull(sourceLock == null ? null : sourceLock.getMaterialId(),
                ngPiece == null ? null : ngPiece.getMaterialId(),
                stock == null ? null : stock.getMaterialId());
    }

    private String resolveSelectedMaterialName(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        HcNgInventoryPieceDO ngPiece = source.ngPiece();
        return firstNotBlank(sourceLock == null ? null : sourceLock.getMaterialName(),
                ngPiece == null ? null : ngPiece.getMaterialName(),
                stock == null ? null : stock.getMaterialName());
    }

    private String resolveSelectedModelNo(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        HcNgInventoryPieceDO ngPiece = source.ngPiece();
        return firstNotBlank(sourceLock == null ? null : sourceLock.getModelNo(),
                ngPiece == null ? null : ngPiece.getModelNo(),
                stock == null ? null : stock.getModelNo());
    }

    private String resolveSelectedModelPrefix(SelectedSource source) {
        String modelNo = StrUtil.trimToEmpty(resolveSelectedModelNo(source)).toUpperCase(Locale.ROOT);
        if (modelNo.length() <= 3) {
            return modelNo;
        }
        return modelNo.substring(0, 3);
    }

    private String resolveSelectedSpecSize(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        return firstNotBlank(sourceLock == null ? null : sourceLock.getSizeSpec(),
                stock == null ? null : stock.getSpecSize());
    }

    private String resolveSelectedRecipeCode(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        return firstNotBlank(sourceLock == null ? null : sourceLock.getRecipeCode(),
                stock == null ? null : stock.getRecipeCode());
    }

    private String resolveSelectedRecipeName(SelectedSource source) {
        HcInvStockDO stock = source.stock();
        return stock == null ? null : stock.getRecipeName();
    }

    private HcPlanOrderInventoryLockDO resolveReportLock(HcDiscretePostProcessVO.ReportReqVO reqVO,
                                                         HcPlanOrderOperationDO operation) {
        HcPlanOrderInventoryLockDO lock;
        if (reqVO.getLockId() != null) {
            lock = hcPlanOrderInventoryLockMapper.selectByIdForUpdate(reqVO.getLockId());
        } else {
            HcDiscretePostProcessVO.SourceRespVO source = scanSource(operation.getId(), reqVO.getPieceNo());
            lock = hcPlanOrderInventoryLockMapper.selectByIdForUpdate(source.getLockId());
        }
        if (lock == null || Boolean.TRUE.equals(lock.getDeleted())) {
            throw invalidParamException("来源锁定明细不存在");
        }
        if (!Objects.equals(operation.getId(), lock.getPlanOperationId())) {
            throw invalidParamException("来源片号不属于当前计划工序");
        }
        if (!LOCK_STATUS_ACTIVE.equals(firstNotBlank(lock.getLockStatus(), LOCK_STATUS_ACTIVE))) {
            throw invalidParamException("来源片号已处理或已释放，不能重复报工：" + resolvePieceNo(lock));
        }
        if (lock.getConsumeReportId() != null) {
            throw invalidParamException("来源片号已有报工记录，不能重复报工：" + resolvePieceNo(lock));
        }
        return lock;
    }

    private Long insertReport(OperationConfig config, HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                              HcPlanOrderInventoryLockDO lock, BigDecimal consumeQty, boolean ngResult,
                              HcDiscretePostProcessVO.ReportReqVO reqVO, LocalDateTime now,
                              Long operatorId, String operatorName) {
        return switch (config.opCode()) {
            case "WC-GROOVE" -> insertPressSlotReport(plan, operation, lock, consumeQty, ngResult, reqVO, now, operatorId, operatorName);
            case "WC-ADH2" -> insertAdhesive2Report(plan, operation, lock, consumeQty, ngResult, reqVO, now, operatorId, operatorName);
            case "WC-CUT" -> insertCutRoundReport(plan, operation, lock, consumeQty, ngResult, reqVO, now, operatorId, operatorName);
            default -> throw invalidParamException("不支持的离散后加工工序：" + config.opCode());
        };
    }

    private Long insertPressSlotReport(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                       HcPlanOrderInventoryLockDO lock, BigDecimal consumeQty, boolean ngResult,
                                       HcDiscretePostProcessVO.ReportReqVO reqVO, LocalDateTime now,
                                       Long operatorId, String operatorName) {
        HcPressSlotReportDO report = new HcPressSlotReportDO();
        fillReportCommon(plan, operation, lock, consumeQty, ngResult, reqVO, now, operatorId, operatorName, report);
        report.setSourceSlittingSliceId(resolveOriginalReportId(lock, SOURCE_TABLE_SLITTING_SLICE));
        report.setStartPosition(BigDecimal.ZERO);
        report.setEndPosition(consumeQty);
        hcPressSlotReportMapper.insert(report);
        return report.getId();
    }

    private Long insertAdhesive2Report(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                       HcPlanOrderInventoryLockDO lock, BigDecimal consumeQty, boolean ngResult,
                                       HcDiscretePostProcessVO.ReportReqVO reqVO, LocalDateTime now,
                                       Long operatorId, String operatorName) {
        HcAdhesive2ReportDO report = new HcAdhesive2ReportDO();
        fillReportCommon(plan, operation, lock, consumeQty, ngResult, reqVO, now, operatorId, operatorName, report);
        report.setSourcePressSlotReportId(resolveOriginalReportId(lock, SOURCE_TABLE_PRESS_SLOT_REPORT));
        report.setSourceSlittingSliceId(null);
        report.setStartPosition(BigDecimal.ZERO);
        report.setEndPosition(consumeQty);
        report.setActualSizeRule(reqVO.getActualSizeRule());
        report.setActualSizeSuffix(reqVO.getActualSizeSuffix());
        report.setProductQualityStatus(ngResult ? "QUALITY_ABNORMAL" : "NORMAL");
        report.setQualityLockReason(ngResult ? firstNotBlank(reqVO.getRemark(), "离散后加工粘胶2报工NG") : null);
        hcAdhesive2ReportMapper.insert(report);
        return report.getId();
    }

    private Long insertCutRoundReport(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                      HcPlanOrderInventoryLockDO lock, BigDecimal consumeQty, boolean ngResult,
                                      HcDiscretePostProcessVO.ReportReqVO reqVO, LocalDateTime now,
                                      Long operatorId, String operatorName) {
        HcCutRoundReportDO report = new HcCutRoundReportDO();
        fillReportCommon(plan, operation, lock, consumeQty, ngResult, reqVO, now, operatorId, operatorName, report);
        report.setSourceAdhesive2ReportId(resolveOriginalReportId(lock, SOURCE_TABLE_ADHESIVE2_REPORT));
        report.setQualityRiskFlag(ngResult ? "CUT_ROUND_NG" : "NONE");
        report.setQualityRiskSnapshotJson(JsonUtils.toJsonString(Map.of(
                "reportMode", LOCK_MARK,
                "selfCheck", firstNotBlank(reqVO.getSelfCheck(), ngResult ? "NG" : "OK"),
                "defectCode", firstNotBlank(reqVO.getDefectCode(), ""),
                "recordTime", DATETIME_FORMATTER.format(now)
        )));
        hcCutRoundReportMapper.insert(report);
        return report.getId();
    }

    private void fillReportCommon(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                  HcPlanOrderInventoryLockDO lock, BigDecimal consumeQty, boolean ngResult,
                                  HcDiscretePostProcessVO.ReportReqVO reqVO, LocalDateTime now,
                                  Long operatorId, String operatorName, Object report) {
        String pieceNo = resolvePieceNo(lock);
        BigDecimal outputQty = ngResult ? BigDecimal.ZERO : consumeQty;
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("reportMode", LOCK_MARK);
        extra.put("sourceLockId", lock.getId());
        extra.put("sourceStockId", lock.getStockId());
        extra.put("ngPieceId", lock.getNgPieceId());
        extra.put("sourcePlanNo", firstNotBlank(lock.getSourcePlanNo(), ""));
        extra.put("sourceBatchNo", firstNotBlank(lock.getSourceBatchNo(), ""));
        extra.put("pieceNo", firstNotBlank(pieceNo, ""));
        extra.put("recordTime", DATETIME_FORMATTER.format(now));

        if (report instanceof HcPressSlotReportDO target) {
            target.setPlanId(plan.getId());
            target.setPlanNo(plan.getPlanNo());
            target.setPlanOperationId(operation.getId());
            target.setOperationCode(operation.getOpCode());
            target.setOperationName(operation.getOpName());
            target.setSourceStockId(lock.getStockId());
            target.setSourcePlanLockId(lock.getId());
            target.setSourceStockBatchNo(pieceNo);
            target.setSourceLockQty(defaultDecimal(lock.getLockQty()));
            target.setSourceBatchNo(lock.getSourceBatchNo());
            target.setSourceProductionBatchNo(pieceNo);
            target.setProductionBatchNo(pieceNo);
            target.setParentProductionBatchNo(firstNotBlank(lock.getSourceBatchNo(), pieceNo));
            target.setMaterialCode(firstNotBlank(lock.getMaterialCode(), plan.getMaterialCode()));
            target.setMaterialName(firstNotBlank(lock.getMaterialName(), plan.getMaterialName()));
            target.setModelCode(firstNotBlank(lock.getModelNo(), plan.getModelCode(), plan.getModelName()));
            target.setReportDate(now.toLocalDate());
            target.setStartTime(now);
            target.setEndTime(now);
            target.setInputLength(firstNonNull(reqVO.getInputQty(), consumeQty));
            target.setLossLength(firstNonNull(reqVO.getLossQty(), ngResult ? consumeQty : BigDecimal.ZERO));
            target.setOutputLength(ngResult ? BigDecimal.ZERO : firstNonNull(reqVO.getOutputQty(), outputQty));
            target.setSelfCheck(resolveSelfCheck(reqVO, ngResult));
            target.setDefectCode(reqVO.getDefectCode());
            target.setReportStatus(REPORT_STATUS_CONFIRMED);
            target.setRecorderName(firstNotBlank(reqVO.getRecorderName(), operatorName));
            target.setRecorderTime(now);
            target.setConfirmerName(operatorName);
            target.setConfirmerTime(now);
            target.setRemark(reqVO.getRemark());
            target.setExtraJson(JsonUtils.toJsonString(extra));
            fillAudit(target, now, operatorId, firstNonNull(plan.getTenantId(), lock.getTenantId(), 0L));
        } else if (report instanceof HcAdhesive2ReportDO target) {
            target.setPlanId(plan.getId());
            target.setPlanNo(plan.getPlanNo());
            target.setPlanOperationId(operation.getId());
            target.setOperationCode(operation.getOpCode());
            target.setOperationName(operation.getOpName());
            target.setSourceStockId(lock.getStockId());
            target.setSourcePlanLockId(lock.getId());
            target.setSourceStockBatchNo(pieceNo);
            target.setSourceLockQty(defaultDecimal(lock.getLockQty()));
            target.setSourceBatchNo(lock.getSourceBatchNo());
            target.setSourceProductionBatchNo(pieceNo);
            target.setProductionBatchNo(pieceNo);
            target.setParentProductionBatchNo(firstNotBlank(lock.getSourceBatchNo(), pieceNo));
            target.setMaterialCode(firstNotBlank(lock.getMaterialCode(), plan.getMaterialCode()));
            target.setMaterialName(firstNotBlank(lock.getMaterialName(), plan.getMaterialName()));
            target.setModelCode(firstNotBlank(lock.getModelNo(), plan.getModelCode(), plan.getModelName()));
            target.setReportDate(now.toLocalDate());
            target.setStartTime(now);
            target.setEndTime(now);
            target.setInputLength(firstNonNull(reqVO.getInputQty(), consumeQty));
            target.setLossLength(firstNonNull(reqVO.getLossQty(), ngResult ? consumeQty : BigDecimal.ZERO));
            target.setOutputLength(ngResult ? BigDecimal.ZERO : firstNonNull(reqVO.getOutputQty(), outputQty));
            target.setSelfCheck(resolveSelfCheck(reqVO, ngResult));
            target.setDefectCode(reqVO.getDefectCode());
            target.setReportStatus(REPORT_STATUS_CONFIRMED);
            target.setRecorderName(firstNotBlank(reqVO.getRecorderName(), operatorName));
            target.setRecorderTime(now);
            target.setConfirmerName(operatorName);
            target.setConfirmerTime(now);
            target.setRemark(reqVO.getRemark());
            target.setExtraJson(JsonUtils.toJsonString(extra));
            fillAudit(target, now, operatorId, firstNonNull(plan.getTenantId(), lock.getTenantId(), 0L));
        } else if (report instanceof HcCutRoundReportDO target) {
            target.setPlanId(plan.getId());
            target.setPlanNo(plan.getPlanNo());
            target.setPlanOperationId(operation.getId());
            target.setOperationCode(operation.getOpCode());
            target.setOperationName(operation.getOpName());
            target.setSourceStockId(lock.getStockId());
            target.setSourcePlanLockId(lock.getId());
            target.setSourceStockBatchNo(pieceNo);
            target.setSourceLockQty(defaultDecimal(lock.getLockQty()));
            target.setSourceBatchNo(lock.getSourceBatchNo());
            target.setSourceProductionBatchNo(pieceNo);
            target.setProductionBatchNo(pieceNo);
            target.setParentProductionBatchNo(firstNotBlank(lock.getSourceBatchNo(), pieceNo));
            target.setMaterialCode(firstNotBlank(lock.getMaterialCode(), plan.getMaterialCode()));
            target.setMaterialName(firstNotBlank(lock.getMaterialName(), plan.getMaterialName()));
            target.setModelCode(firstNotBlank(lock.getModelNo(), plan.getModelCode(), plan.getModelName()));
            target.setReportDate(now.toLocalDate());
            target.setStartTime(now);
            target.setEndTime(now);
            target.setInputLength(firstNonNull(reqVO.getInputQty(), consumeQty));
            target.setOutputLength(ngResult ? BigDecimal.ZERO : firstNonNull(reqVO.getOutputQty(), outputQty));
            target.setSelfCheck(resolveSelfCheck(reqVO, ngResult));
            target.setDefectCode(reqVO.getDefectCode());
            target.setReportStatus(REPORT_STATUS_CONFIRMED);
            target.setRecorderName(firstNotBlank(reqVO.getRecorderName(), operatorName));
            target.setRecorderTime(now);
            target.setConfirmerName(operatorName);
            target.setConfirmerTime(now);
            target.setRemark(reqVO.getRemark());
            target.setExtraJson(JsonUtils.toJsonString(extra));
            fillAudit(target, now, operatorId, firstNonNull(plan.getTenantId(), lock.getTenantId(), 0L));
        }
    }

    private Long resolveOriginalReportId(HcPlanOrderInventoryLockDO lock, String expectedSourceTable) {
        if (lock == null || !equalsIgnoreCase(expectedSourceTable, lock.getSourceTable())) {
            return null;
        }
        return firstNonNull(lock.getSourceReportId(), lock.getSourceId());
    }

    private HcInvStockDO postOutputWip(OperationConfig config, HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                       HcPlanOrderInventoryLockDO lock, Long reportId, BigDecimal outputQty,
                                       HcDiscretePostProcessVO.ReportReqVO reqVO, LocalDateTime now,
                                       Long operatorId, String operatorName) {
        return hcInvStockService.postProcessOutputWip(HcWipOutputPostReq.builder()
                .plan(plan)
                .operation(operation)
                .sourceType(config.reportSourceType())
                .sourceTable(config.reportSourceTable())
                .sourceId(reportId)
                .sourceReportId(reportId)
                .batchNo(resolvePieceNo(lock))
                .parentBatchNo(firstNotBlank(lock.getSourceBatchNo(), resolvePieceNo(lock)))
                .outputQty(outputQty)
                .uom("pcs")
                .materialId(lock.getMaterialId())
                .materialCode(lock.getMaterialCode())
                .materialName(lock.getMaterialName())
                .modelNo(firstNotBlank(lock.getModelNo(), plan.getModelCode(), plan.getModelName()))
                .specSize(firstNotBlank(lock.getSizeSpec(), plan.getSizeSpec()))
                .qualityStatus("合格")
                .bizStatus(resolveBizStatus(plan))
                .businessRemark("离散后加工" + config.opName() + "报工产出；来源片号：" + firstNotBlank(resolvePieceNo(lock), "-"))
                .txnRemark("离散后加工" + config.opName() + "扫码确认自动入中间边库")
                .postTime(now)
                .operatorId(operatorId)
                .operatorName(operatorName)
                .build());
    }

    private void autoLockNextOperation(HcPlanOrderDO plan, HcPlanOrderOperationDO currentOperation, HcInvStockDO outputStock,
                                       OperationConfig currentConfig, LocalDateTime now, Long operatorId, String operatorName) {
        List<HcPlanOrderOperationDO> operations = hcPlanOrderOperationMapper.selectListByPlanId(plan.getId());
        HcPlanOrderOperationDO nextOperation = operations.stream()
                .filter(item -> item.getOpSeq() != null && currentOperation.getOpSeq() != null
                        && item.getOpSeq() > currentOperation.getOpSeq())
                .min(Comparator.comparing(HcPlanOrderOperationDO::getOpSeq))
                .orElse(null);
        if (nextOperation == null || outputStock == null || defaultDecimal(outputStock.getAvailableQty()).compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        OperationConfig nextConfig = requiredConfig(nextOperation.getOpCode());
        HcPlanOrderInventoryLockDO lock = new HcPlanOrderInventoryLockDO();
        lock.setTenantId(firstNonNull(plan.getTenantId(), outputStock.getTenantId()));
        lock.setPlanId(plan.getId());
        lock.setTargetPlanNo(plan.getPlanNo());
        lock.setPlanOperationId(nextOperation.getId());
        lock.setTargetOpCode(nextOperation.getOpCode());
        lock.setTargetOpName(nextOperation.getOpName());
        lock.setLockType(STOCK_TYPE_WIP);
        lock.setStockId(outputStock.getId());
        lock.setStockType(outputStock.getStockType());
        lock.setSourceType(outputStock.getSourceType());
        lock.setSourceTable(outputStock.getSourceTable());
        lock.setSourceId(outputStock.getSourceId());
        lock.setSourcePlanId(outputStock.getSourcePlanId());
        lock.setSourcePlanNo(outputStock.getSourcePlanNo());
        lock.setSourcePlanOperationId(outputStock.getSourcePlanOperationId());
        lock.setSourceBatchNo(outputStock.getBatchNo());
        lock.setOpSeq(outputStock.getOpSeq());
        lock.setOpCode(outputStock.getOpCode());
        lock.setOpName(outputStock.getOpName());
        lock.setSegmentCode(outputStock.getSegmentCode());
        lock.setSegmentName(outputStock.getSegmentName());
        lock.setThickness(outputStock.getThickness());
        lock.setLotNo(outputStock.getBatchNo());
        lock.setBatchNo(outputStock.getBatchNo());
        lock.setMaterialId(outputStock.getMaterialId());
        lock.setMaterialCode(outputStock.getMaterialCode());
        lock.setMaterialName(outputStock.getMaterialName());
        lock.setModelNo(outputStock.getModelNo());
        lock.setRecipeCode(outputStock.getRecipeCode());
        lock.setSizeSpec(outputStock.getSpecSize());
        lock.setProductionDate(outputStock.getProductionDate());
        lock.setExpiryDate(outputStock.getExpiryDate());
        lock.setLocationCode(outputStock.getLocationCode());
        lock.setLocationName(outputStock.getLocationName());
        lock.setOwnerId(outputStock.getOwnerId());
        lock.setOwnerCode(outputStock.getOwnerCode());
        lock.setOwnerName(outputStock.getOwnerName());
        lock.setAvailableQty(outputStock.getAvailableQty());
        lock.setLockQty(outputStock.getAvailableQty());
        lock.setConsumedQty(BigDecimal.ZERO);
        lock.setReleasedQty(BigDecimal.ZERO);
        lock.setRemainingQty(outputStock.getAvailableQty());
        lock.setUnitCode(firstNotBlank(outputStock.getUom(), "pcs"));
        lock.setUnitName("片");
        lock.setUom(firstNotBlank(outputStock.getUom(), "pcs"));
        lock.setLockStatus(LOCK_STATUS_ACTIVE);
        lock.setRemark(LOCK_MARK + "；离散后加工工序间自动锁定；来源工序：" + currentConfig.opName()
                + "；目标工序：" + nextConfig.opName());
        lock.setCreator(operatorId == null ? "" : String.valueOf(operatorId));
        lock.setUpdater(operatorId == null ? "" : String.valueOf(operatorId));
        hcPlanOrderInventoryLockMapper.insert(lock);
        hcInvStockService.lockPlanWip(lock, outputStock.getAvailableQty(), now, operatorId, operatorName,
                "离散后加工工序间自动锁定；目标计划：" + firstNotBlank(plan.getPlanNo(), "-")
                        + "；目标工序：" + nextConfig.opName());
    }

    private ConsumptionResult consumeSourceForReport(HcPlanOrderInventoryLockDO lock, BigDecimal consumeQty,
                                                     OperationConfig config, Long reportId, HcPlanOrderDO plan,
                                                     LocalDateTime now, Long operatorId, String operatorName) {
        if (isNgPieceLock(lock)) {
            return consumeNgPieceForReport(lock, consumeQty, config, reportId, plan, now, operatorId, operatorName);
        }
        HcInvStockDO consumedStock = hcInvStockService.consumePlanLockedWip(
                lock, consumeQty, config.reportSourceType(), reportId, resolvePieceNo(lock), now,
                operatorId, operatorName,
                "离散后加工扫码报工消耗来源片号；目标计划：" + firstNotBlank(plan.getPlanNo(), "-")
                        + "；目标工序：" + config.opName());
        return new ConsumptionResult(consumedStock.getLastTxnNo(), consumedStock.getLastTxnTime());
    }

    private ConsumptionResult consumeNgPieceForReport(HcPlanOrderInventoryLockDO lock, BigDecimal consumeQty,
                                                      OperationConfig config, Long reportId, HcPlanOrderDO plan,
                                                      LocalDateTime now, Long operatorId, String operatorName) {
        HcNgInventoryPieceDO piece = loadNgPieceForLock(lock, "报工");
        validateNgPieceForReport(piece, config);
        BigDecimal normalizedConsumeQty = defaultDecimal(consumeQty);
        BigDecimal pieceQty = defaultDecimal(piece.getPieceQty());
        if (pieceQty.compareTo(normalizedConsumeQty) < 0) {
            throw invalidParamException("NG库片号数量不足，片号：" + firstNotBlank(piece.getPieceNo(), "-")
                    + "，库存 " + pieceQty + "，本次 " + normalizedConsumeQty);
        }

        boolean pickedForPlan = StrUtil.isNotBlank(lock.getLockTxnNo());
        String txnNo = pickedForPlan
                ? lock.getLockTxnNo()
                : "NGREWORKOUT-" + TXN_NO_TIME_FORMATTER.format(now) + "-" + firstNonNull(reportId, lock.getId());
        HcInvStockDO stock = piece.getStockId() == null ? null : hcInvStockMapper.selectByIdForUpdate(piece.getStockId());
        if (!pickedForPlan && piece.getStockId() != null && stock == null) {
            throw invalidParamException("NG库片号库存余额不存在，无法报工：" + firstNotBlank(piece.getPieceNo(), "-"));
        }
        if (!pickedForPlan && stock != null) {
            consumeNgPieceStock(stock, piece, normalizedConsumeQty, txnNo, config, reportId, plan, lock,
                    now, operatorId, operatorName);
            decreaseNgLocationOccupiedIfNeeded(piece);
        } else if (!pickedForPlan) {
            insertNgPieceLogicalTxnLog(piece, normalizedConsumeQty, txnNo, TXN_TYPE_NG_REWORK_OUT, config, plan, lock,
                    now, operatorId, operatorName,
                    "离散后加工扫码报工，从待上架NG片号转返工加工；原NG状态：" + firstNotBlank(piece.getStatus(), "-"));
        }

        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal consumedQty = defaultDecimal(lock.getConsumedQty()).add(normalizedConsumeQty);
        BigDecimal releasedQty = defaultDecimal(lock.getReleasedQty());
        BigDecimal remainingQty = defaultDecimal(lock.getLockQty()).subtract(consumedQty).subtract(releasedQty).max(zero);
        HcPlanOrderInventoryLockDO updateLock = new HcPlanOrderInventoryLockDO();
        updateLock.setId(lock.getId());
        updateLock.setConsumedQty(consumedQty);
        updateLock.setReleasedQty(releasedQty);
        updateLock.setRemainingQty(remainingQty);
        updateLock.setConsumeReportId(reportId);
        updateLock.setConsumeTime(now);
        updateLock.setConsumeTxnNo(txnNo);
        updateLock.setLockStatus(remainingQty.compareTo(BigDecimal.ZERO) <= 0 ? LOCK_STATUS_CONSUMED : LOCK_STATUS_ACTIVE);
        updateLock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcPlanOrderInventoryLockMapper.updateById(updateLock);
        lock.setConsumedQty(consumedQty);
        lock.setReleasedQty(releasedQty);
        lock.setRemainingQty(remainingQty);
        lock.setConsumeReportId(reportId);
        lock.setConsumeTime(now);
        lock.setConsumeTxnNo(txnNo);
        lock.setLockStatus(updateLock.getLockStatus());

        hcNgInventoryPieceMapper.update(null, new LambdaUpdateWrapper<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getId, piece.getId())
                .set(HcNgInventoryPieceDO::getStatus, STATUS_RETURNED)
                .set(HcNgInventoryPieceDO::getStockId, null)
                .set(HcNgInventoryPieceDO::getCurrentWarehouseCode, null)
                .set(HcNgInventoryPieceDO::getCurrentWarehouseName, null)
                .set(HcNgInventoryPieceDO::getCurrentLocationCode, null)
                .set(HcNgInventoryPieceDO::getCurrentLocationName, null)
                .set(HcNgInventoryPieceDO::getUpdateTime, now)
                .set(HcNgInventoryPieceDO::getUpdater, operatorId == null ? null : String.valueOf(operatorId)));
        return new ConsumptionResult(txnNo, now);
    }

    private void pickupNgPieceStock(HcInvStockDO stock, HcNgInventoryPieceDO piece, BigDecimal pickQty,
                                    String txnNo, OperationConfig config, HcPlanOrderDO plan,
                                    HcPlanOrderInventoryLockDO lock, LocalDateTime now,
                                    Long operatorId, String operatorName) {
        BigDecimal beforeQty = defaultDecimal(stock.getOnHandQty());
        BigDecimal beforeAvailableQty = defaultDecimal(stock.getAvailableQty());
        BigDecimal beforeFrozenQty = defaultDecimal(stock.getFrozenQty());
        BigDecimal beforePlanLockedQty = defaultDecimal(stock.getPlanLockedQty());
        BigDecimal beforeShareableQty = defaultDecimal(stock.getShareableQty());
        if (beforeQty.compareTo(pickQty) < 0) {
            throw invalidParamException("NG库库存数量不足，片号：" + firstNotBlank(piece.getPieceNo(), "-")
                    + "，库存 " + beforeQty + "，本次 " + pickQty);
        }
        BigDecimal afterQty = beforeQty.subtract(pickQty).max(BigDecimal.ZERO);
        BigDecimal availableConsumeQty = minDecimal(beforeAvailableQty, pickQty);
        BigDecimal frozenConsumeQty = STATUS_FROZEN.equals(firstNotBlank(piece.getStatus(), ""))
                ? minDecimal(beforeFrozenQty, pickQty) : BigDecimal.ZERO;
        BigDecimal planLockedConsumeQty = minDecimal(beforePlanLockedQty, pickQty);
        BigDecimal shareableConsumeQty = minDecimal(beforeShareableQty, pickQty);
        BigDecimal afterAvailableQty = beforeAvailableQty.subtract(availableConsumeQty).max(BigDecimal.ZERO);
        BigDecimal afterFrozenQty = beforeFrozenQty.subtract(frozenConsumeQty).max(BigDecimal.ZERO);
        BigDecimal afterPlanLockedQty = beforePlanLockedQty.subtract(planLockedConsumeQty).max(BigDecimal.ZERO);

        HcInvStockDO updateStock = new HcInvStockDO();
        updateStock.setId(stock.getId());
        updateStock.setOnHandQty(afterQty);
        updateStock.setAvailableQty(afterAvailableQty);
        updateStock.setShareableQty(beforeShareableQty.subtract(shareableConsumeQty).max(BigDecimal.ZERO));
        updateStock.setFrozenQty(afterFrozenQty);
        updateStock.setPlanLockedQty(afterPlanLockedQty);
        updateStock.setBizStatus(afterQty.compareTo(BigDecimal.ZERO) == 0 ? BIZ_STATUS_NG_EMPTY : stock.getBizStatus());
        updateStock.setLastTxnNo(txnNo);
        updateStock.setLastTxnTime(now);
        updateStock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvStockMapper.updateById(updateStock);

        insertNgPieceStockTxnLog(stock, piece, pickQty, txnNo, TXN_TYPE_NG_REWORK_PICK, config, plan, lock,
                now, operatorId, operatorName, beforeQty, afterQty, beforeAvailableQty, afterAvailableQty,
                beforeFrozenQty, afterFrozenQty, beforePlanLockedQty, afterPlanLockedQty,
                "离散后加工计划下达，从NG库下架转加工；原NG状态：" + firstNotBlank(piece.getStatus(), "-"));
    }

    private void consumeNgPieceStock(HcInvStockDO stock, HcNgInventoryPieceDO piece, BigDecimal consumeQty,
                                     String txnNo, OperationConfig config, Long reportId, HcPlanOrderDO plan,
                                     HcPlanOrderInventoryLockDO lock, LocalDateTime now,
                                     Long operatorId, String operatorName) {
        BigDecimal beforeQty = defaultDecimal(stock.getOnHandQty());
        BigDecimal beforeAvailableQty = defaultDecimal(stock.getAvailableQty());
        BigDecimal beforeFrozenQty = defaultDecimal(stock.getFrozenQty());
        BigDecimal beforePlanLockedQty = defaultDecimal(stock.getPlanLockedQty());
        if (beforeQty.compareTo(consumeQty) < 0) {
            throw invalidParamException("NG库库存数量不足，片号：" + firstNotBlank(piece.getPieceNo(), "-")
                    + "，库存 " + beforeQty + "，本次 " + consumeQty);
        }
        BigDecimal afterQty = beforeQty.subtract(consumeQty).max(BigDecimal.ZERO);
        BigDecimal frozenConsumeQty = STATUS_FROZEN.equals(firstNotBlank(piece.getStatus(), ""))
                ? minDecimal(beforeFrozenQty, consumeQty) : BigDecimal.ZERO;
        BigDecimal afterFrozenQty = beforeFrozenQty.subtract(frozenConsumeQty).max(BigDecimal.ZERO);

        HcInvStockDO updateStock = new HcInvStockDO();
        updateStock.setId(stock.getId());
        updateStock.setOnHandQty(afterQty);
        updateStock.setAvailableQty(BigDecimal.ZERO);
        updateStock.setShareableQty(BigDecimal.ZERO);
        updateStock.setFrozenQty(afterFrozenQty);
        updateStock.setPlanLockedQty(BigDecimal.ZERO);
        updateStock.setBizStatus(afterQty.compareTo(BigDecimal.ZERO) == 0 ? BIZ_STATUS_NG_EMPTY : stock.getBizStatus());
        updateStock.setLastTxnNo(txnNo);
        updateStock.setLastTxnTime(now);
        updateStock.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvStockMapper.updateById(updateStock);

        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(firstNonNull(stock.getTenantId(), piece.getTenantId(), lock.getTenantId()))
                .stockId(stock.getId())
                .stockType(firstNotBlank(stock.getStockType(), STOCK_TYPE_WIP))
                .txnNo(txnNo)
                .txnType(TXN_TYPE_NG_REWORK_OUT)
                .txnTime(now)
                .warehouseCode(stock.getWarehouseCode())
                .warehouseName(stock.getWarehouseName())
                .locationCode(stock.getLocationCode())
                .materialId(firstNonNull(stock.getMaterialId(), piece.getMaterialId()))
                .materialCode(firstNotBlank(stock.getMaterialCode(), piece.getMaterialCode(), lock.getMaterialCode()))
                .materialName(firstNotBlank(stock.getMaterialName(), piece.getMaterialName(), lock.getMaterialName()))
                .modelNo(firstNotBlank(stock.getModelNo(), piece.getModelNo(), lock.getModelNo()))
                .batchNo(firstNotBlank(piece.getPieceNo(), stock.getBatchNo(), lock.getBatchNo()))
                .txnQty(consumeQty.negate())
                .beforeQty(beforeQty)
                .afterQty(afterQty)
                .beforeAvailableQty(beforeAvailableQty)
                .afterAvailableQty(BigDecimal.ZERO)
                .beforeFrozenQty(beforeFrozenQty)
                .afterFrozenQty(afterFrozenQty)
                .beforePlanLockedQty(beforePlanLockedQty)
                .afterPlanLockedQty(BigDecimal.ZERO)
                .uom("pcs")
                .refDocType(config.reportSourceType())
                .refDocId(reportId)
                .refDocNo(resolvePieceNo(lock))
                .sourceType(SOURCE_TYPE_NG_INVENTORY)
                .sourceTable(SOURCE_TABLE_NG_PIECE)
                .sourceId(piece.getId())
                .sourceBatchNo(firstNotBlank(piece.getPieceNo(), piece.getSourceBatchNo(), lock.getBatchNo()))
                .sourcePlanId(piece.getSourcePlanId())
                .sourcePlanNo(piece.getSourcePlanNo())
                .sourcePlanOperationId(piece.getSourcePlanOperationId())
                .targetPlanId(plan.getId())
                .targetPlanNo(plan.getPlanNo())
                .targetPlanOperationId(lock.getPlanOperationId())
                .targetOpCode(config.opCode())
                .targetOpName(config.opName())
                .creatorName(firstNotBlank(operatorName, "系统"))
                .remark("离散后加工扫码报工，从NG库转返工加工；原NG状态："
                        + firstNotBlank(piece.getStatus(), "-"))
                .build();
        txnLog.setCreateTime(now);
        txnLog.setUpdateTime(now);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);
    }

    private void insertNgPieceStockTxnLog(HcInvStockDO stock, HcNgInventoryPieceDO piece, BigDecimal txnQtyAbs,
                                          String txnNo, String txnType, OperationConfig config, HcPlanOrderDO plan,
                                          HcPlanOrderInventoryLockDO lock, LocalDateTime now,
                                          Long operatorId, String operatorName,
                                          BigDecimal beforeQty, BigDecimal afterQty,
                                          BigDecimal beforeAvailableQty, BigDecimal afterAvailableQty,
                                          BigDecimal beforeFrozenQty, BigDecimal afterFrozenQty,
                                          BigDecimal beforePlanLockedQty, BigDecimal afterPlanLockedQty,
                                          String remark) {
        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(firstNonNull(stock.getTenantId(), piece.getTenantId(), lock.getTenantId()))
                .stockId(stock.getId())
                .stockType(firstNotBlank(stock.getStockType(), STOCK_TYPE_WIP))
                .txnNo(txnNo)
                .txnType(txnType)
                .txnTime(now)
                .warehouseCode(firstNotBlank(stock.getWarehouseCode(), piece.getCurrentWarehouseCode(), "NG-WAREHOUSE"))
                .warehouseName(firstNotBlank(stock.getWarehouseName(), piece.getCurrentWarehouseName(), "不合格品库"))
                .locationCode(firstNotBlank(stock.getLocationCode(), piece.getCurrentLocationCode()))
                .materialId(firstNonNull(stock.getMaterialId(), piece.getMaterialId()))
                .materialCode(firstNotBlank(stock.getMaterialCode(), piece.getMaterialCode(), lock.getMaterialCode(), ""))
                .materialName(firstNotBlank(stock.getMaterialName(), piece.getMaterialName(), lock.getMaterialName(), ""))
                .modelNo(firstNotBlank(stock.getModelNo(), piece.getModelNo(), lock.getModelNo()))
                .batchNo(firstNotBlank(piece.getPieceNo(), stock.getBatchNo(), lock.getBatchNo(), ""))
                .txnQty(defaultDecimal(txnQtyAbs).negate())
                .beforeQty(defaultDecimal(beforeQty))
                .afterQty(defaultDecimal(afterQty))
                .beforeAvailableQty(defaultDecimal(beforeAvailableQty))
                .afterAvailableQty(defaultDecimal(afterAvailableQty))
                .beforeFrozenQty(defaultDecimal(beforeFrozenQty))
                .afterFrozenQty(defaultDecimal(afterFrozenQty))
                .beforePlanLockedQty(defaultDecimal(beforePlanLockedQty))
                .afterPlanLockedQty(defaultDecimal(afterPlanLockedQty))
                .uom("pcs")
                .refDocType("DISCRETE_POST_PLAN")
                .refDocId(plan.getId())
                .refDocNo(plan.getPlanNo())
                .sourceType(SOURCE_TYPE_NG_INVENTORY)
                .sourceTable(SOURCE_TABLE_NG_PIECE)
                .sourceId(piece.getId())
                .sourceBatchNo(firstNotBlank(piece.getPieceNo(), piece.getSourceBatchNo(), lock.getBatchNo()))
                .sourcePlanId(piece.getSourcePlanId())
                .sourcePlanNo(piece.getSourcePlanNo())
                .sourcePlanOperationId(piece.getSourcePlanOperationId())
                .targetPlanId(plan.getId())
                .targetPlanNo(plan.getPlanNo())
                .targetPlanOperationId(lock.getPlanOperationId())
                .targetOpCode(config.opCode())
                .targetOpName(config.opName())
                .creatorName(firstNotBlank(operatorName, "系统"))
                .remark(remark)
                .build();
        txnLog.setCreateTime(now);
        txnLog.setUpdateTime(now);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);
    }

    private void insertNgPieceLogicalTxnLog(HcNgInventoryPieceDO piece, BigDecimal txnQtyAbs,
                                            String txnNo, String txnType, OperationConfig config, HcPlanOrderDO plan,
                                            HcPlanOrderInventoryLockDO lock, LocalDateTime now,
                                            Long operatorId, String operatorName, String remark) {
        BigDecimal qty = defaultDecimal(txnQtyAbs);
        HcInvTxnLogDO txnLog = HcInvTxnLogDO.builder()
                .tenantId(firstNonNull(piece.getTenantId(), lock.getTenantId(), plan.getTenantId()))
                .stockId(null)
                .stockType(STOCK_TYPE_NG_PIECE)
                .txnNo(txnNo)
                .txnType(txnType)
                .txnTime(now)
                .warehouseCode(resolveVirtualNgWarehouseCode(piece))
                .warehouseName(resolveVirtualNgWarehouseName(piece))
                .locationCode(resolveVirtualNgLocationCode(piece))
                .materialId(piece.getMaterialId())
                .materialCode(firstNotBlank(piece.getMaterialCode(), lock.getMaterialCode(), ""))
                .materialName(firstNotBlank(piece.getMaterialName(), lock.getMaterialName(), ""))
                .modelNo(firstNotBlank(piece.getModelNo(), lock.getModelNo(), plan.getModelCode()))
                .batchNo(firstNotBlank(piece.getPieceNo(), lock.getBatchNo(), piece.getSourceBatchNo(), ""))
                .txnQty(qty.negate())
                .beforeQty(qty)
                .afterQty(BigDecimal.ZERO)
                .beforeAvailableQty(BigDecimal.ZERO)
                .afterAvailableQty(BigDecimal.ZERO)
                .beforeFrozenQty(BigDecimal.ZERO)
                .afterFrozenQty(BigDecimal.ZERO)
                .beforePlanLockedQty(BigDecimal.ZERO)
                .afterPlanLockedQty(BigDecimal.ZERO)
                .uom("pcs")
                .refDocType("DISCRETE_POST_PLAN")
                .refDocId(plan.getId())
                .refDocNo(plan.getPlanNo())
                .sourceType(SOURCE_TYPE_NG_INVENTORY)
                .sourceTable(SOURCE_TABLE_NG_PIECE)
                .sourceId(piece.getId())
                .sourceBatchNo(firstNotBlank(piece.getPieceNo(), piece.getSourceBatchNo(), lock.getBatchNo()))
                .sourcePlanId(piece.getSourcePlanId())
                .sourcePlanNo(piece.getSourcePlanNo())
                .sourcePlanOperationId(piece.getSourcePlanOperationId())
                .targetPlanId(plan.getId())
                .targetPlanNo(plan.getPlanNo())
                .targetPlanOperationId(lock.getPlanOperationId())
                .targetOpCode(config.opCode())
                .targetOpName(config.opName())
                .creatorName(firstNotBlank(operatorName, "系统"))
                .remark(remark)
                .build();
        txnLog.setCreateTime(now);
        txnLog.setUpdateTime(now);
        txnLog.setCreator(operatorId == null ? null : String.valueOf(operatorId));
        txnLog.setUpdater(operatorId == null ? null : String.valueOf(operatorId));
        hcInvTxnLogMapper.insert(txnLog);
    }

    private void updateNgPiecePicked(HcNgInventoryPieceDO piece, LocalDateTime now, Long operatorId) {
        hcNgInventoryPieceMapper.update(null, new LambdaUpdateWrapper<HcNgInventoryPieceDO>()
                .eq(HcNgInventoryPieceDO::getId, piece.getId())
                .set(HcNgInventoryPieceDO::getStatus, STATUS_REWORKING)
                .set(HcNgInventoryPieceDO::getStockId, null)
                .set(HcNgInventoryPieceDO::getCurrentWarehouseCode, null)
                .set(HcNgInventoryPieceDO::getCurrentWarehouseName, null)
                .set(HcNgInventoryPieceDO::getCurrentLocationCode, null)
                .set(HcNgInventoryPieceDO::getCurrentLocationName, null)
                .set(HcNgInventoryPieceDO::getUpdateTime, now)
                .set(HcNgInventoryPieceDO::getUpdater, operatorId == null ? null : String.valueOf(operatorId)));
    }

    private String resolveVirtualNgWarehouseCode(HcNgInventoryPieceDO piece) {
        if (STATUS_WAIT_FREEZE_SHELF.equals(firstNotBlank(piece.getStatus(), ""))) {
            return "NG-WAIT-FREEZE";
        }
        return "NG-WAIT-SHELF";
    }

    private String resolveVirtualNgWarehouseName(HcNgInventoryPieceDO piece) {
        if (STATUS_WAIT_FREEZE_SHELF.equals(firstNotBlank(piece.getStatus(), ""))) {
            return "不合格品待冻结上架区";
        }
        return "不合格品待上架区";
    }

    private String resolveVirtualNgLocationCode(HcNgInventoryPieceDO piece) {
        return firstNotBlank(piece.getCurrentLocationCode(), piece.getStatus(), "WAIT_SHELF");
    }

    private void validateNgPieceForReport(HcNgInventoryPieceDO piece, OperationConfig config) {
        String status = firstNotBlank(piece.getStatus(), "");
        if (!equalsAnyIgnoreCase(status, STATUS_WAIT_SHELF, STATUS_WAIT_FREEZE_SHELF,
                STATUS_STORED, STATUS_FROZEN, STATUS_REWORKING)) {
            throw invalidParamException("NG库片号状态不可报工：" + firstNotBlank(piece.getPieceNo(), "-")
                    + "，当前状态：" + firstNotBlank(status, "-"));
        }
        if (!equalsIgnoreCase(config.sourceType(), piece.getProcessType())) {
            throw invalidParamException(config.opName() + "只能接收" + config.sourceOpName()
                    + "产生的NG片号，当前片号：" + firstNotBlank(piece.getPieceNo(), "-"));
        }
    }

    private void decreaseNgLocationOccupiedIfNeeded(HcNgInventoryPieceDO piece) {
        if (!equalsAnyIgnoreCase(piece.getStatus(), STATUS_STORED, STATUS_FROZEN)
                || StrUtil.isBlank(piece.getCurrentLocationCode())) {
            return;
        }
        HcNgInventoryLocationDO location =
                hcNgInventoryLocationMapper.selectByLocationKeyForUpdate(piece.getCurrentLocationCode());
        if (location == null) {
            return;
        }
        int occupiedQty = location.getOccupiedQty() == null ? 0 : location.getOccupiedQty();
        int pieceQty = defaultDecimal(piece.getPieceQty()).max(BigDecimal.ONE).intValue();
        HcNgInventoryLocationDO update = new HcNgInventoryLocationDO();
        update.setId(location.getId());
        update.setOccupiedQty(Math.max(0, occupiedQty - pieceQty));
        hcNgInventoryLocationMapper.updateById(update);
    }

    private void updateReportConsumeInfo(OperationConfig config, Long reportId, HcPlanOrderInventoryLockDO lock,
                                         BigDecimal consumeQty, ConsumptionResult consumption) {
        switch (config.opCode()) {
            case "WC-GROOVE" -> {
                HcPressSlotReportDO update = new HcPressSlotReportDO();
                update.setId(reportId);
                update.setSourceStockId(lock.getStockId());
                update.setSourcePlanLockId(lock.getId());
                update.setSourceStockBatchNo(resolvePieceNo(lock));
                update.setSourceLockQty(defaultDecimal(lock.getLockQty()));
                update.setSourceConsumeQty(consumeQty);
                update.setSourceConsumeTxnNo(consumption.txnNo());
                update.setSourceConsumeTime(consumption.txnTime());
                hcPressSlotReportMapper.updateById(update);
            }
            case "WC-ADH2" -> {
                HcAdhesive2ReportDO update = new HcAdhesive2ReportDO();
                update.setId(reportId);
                update.setSourceStockId(lock.getStockId());
                update.setSourcePlanLockId(lock.getId());
                update.setSourceStockBatchNo(resolvePieceNo(lock));
                update.setSourceLockQty(defaultDecimal(lock.getLockQty()));
                update.setSourceConsumeQty(consumeQty);
                update.setSourceConsumeTxnNo(consumption.txnNo());
                update.setSourceConsumeTime(consumption.txnTime());
                hcAdhesive2ReportMapper.updateById(update);
            }
            case "WC-CUT" -> {
                HcCutRoundReportDO update = new HcCutRoundReportDO();
                update.setId(reportId);
                update.setSourceStockId(lock.getStockId());
                update.setSourcePlanLockId(lock.getId());
                update.setSourceStockBatchNo(resolvePieceNo(lock));
                update.setSourceLockQty(defaultDecimal(lock.getLockQty()));
                update.setSourceConsumeQty(consumeQty);
                update.setSourceConsumeTxnNo(consumption.txnNo());
                update.setSourceConsumeTime(consumption.txnTime());
                hcCutRoundReportMapper.updateById(update);
            }
            default -> throw invalidParamException("不支持的离散后加工工序：" + config.opCode());
        }
    }

    private void updateReportOutputInfo(OperationConfig config, Long reportId, HcInvStockDO outputStock) {
        switch (config.opCode()) {
            case "WC-GROOVE" -> {
                HcPressSlotReportDO update = new HcPressSlotReportDO();
                update.setId(reportId);
                update.setOutputStockId(outputStock.getId());
                update.setOutputStockPostStatus(STOCK_POST_STATUS_POSTED);
                update.setOutputStockPostTime(outputStock.getLastTxnTime());
                update.setOutputStockPostMessage("离散后加工压槽报工自动入中间边库");
                hcPressSlotReportMapper.updateById(update);
            }
            case "WC-ADH2" -> {
                HcAdhesive2ReportDO update = new HcAdhesive2ReportDO();
                update.setId(reportId);
                update.setOutputStockId(outputStock.getId());
                update.setOutputStockPostStatus(STOCK_POST_STATUS_POSTED);
                update.setOutputStockPostTime(outputStock.getLastTxnTime());
                update.setOutputStockPostMessage("离散后加工粘胶2报工自动入中间边库");
                hcAdhesive2ReportMapper.updateById(update);
            }
            case "WC-CUT" -> {
                HcCutRoundReportDO update = new HcCutRoundReportDO();
                update.setId(reportId);
                update.setOutputStockId(outputStock.getId());
                update.setOutputStockPostStatus(STOCK_POST_STATUS_POSTED);
                update.setOutputStockPostTime(outputStock.getLastTxnTime());
                update.setOutputStockPostMessage("离散后加工裁切报工自动入中间边库");
                hcCutRoundReportMapper.updateById(update);
            }
            default -> throw invalidParamException("不支持的离散后加工工序：" + config.opCode());
        }
    }

    private void markReportNg(OperationConfig config, Long reportId, HcPlanOrderDO plan,
                              HcPlanOrderOperationDO operation, LocalDateTime now) {
        switch (config.opCode()) {
            case "WC-GROOVE" -> {
                HcPressSlotReportDO update = new HcPressSlotReportDO();
                update.setId(reportId);
                update.setOutputStockPostStatus("NG_STORED");
                update.setOutputStockPostTime(now);
                update.setOutputStockPostMessage("离散后加工压槽报工NG，已自动入压槽库");
                hcPressSlotReportMapper.updateById(update);
                HcPressSlotReportDO report = hcPressSlotReportMapper.selectById(reportId);
                if (report != null) {
                    hcNgInventoryService.registerPressSlotNgPiece(report, plan, operation);
                }
            }
            case "WC-ADH2" -> {
                HcAdhesive2ReportDO update = new HcAdhesive2ReportDO();
                update.setId(reportId);
                update.setOutputStockPostStatus(STOCK_POST_STATUS_WAIT_NG_SHELF);
                update.setOutputStockPostTime(now);
                update.setOutputStockPostMessage("离散后加工粘胶2报工NG，未进入后续WIP");
                hcAdhesive2ReportMapper.updateById(update);
            }
            case "WC-CUT" -> {
                HcCutRoundReportDO update = new HcCutRoundReportDO();
                update.setId(reportId);
                update.setOutputStockPostStatus(STOCK_POST_STATUS_WAIT_NG_SHELF);
                update.setOutputStockPostTime(now);
                update.setOutputStockPostMessage("离散后加工裁切报工NG，未进入后续WIP");
                hcCutRoundReportMapper.updateById(update);
            }
            default -> throw invalidParamException("不支持的离散后加工工序：" + config.opCode());
        }
    }

    private void refreshOperationFinishedIfNeeded(HcPlanOrderOperationDO operation, LocalDateTime now,
                                                  Long operatorId, String operatorName) {
        List<HcPlanOrderInventoryLockDO> locks =
                hcPlanOrderInventoryLockMapper.selectListByPlanOperationId(operation.getId());
        boolean hasActive = locks.stream()
                .anyMatch(lock -> LOCK_STATUS_ACTIVE.equals(firstNotBlank(lock.getLockStatus(), LOCK_STATUS_ACTIVE))
                        && defaultDecimal(lock.getRemainingQty()).compareTo(BigDecimal.ZERO) > 0);
        if (hasActive) {
            return;
        }
        HcPlanOrderOperationDO update = new HcPlanOrderOperationDO();
        update.setId(operation.getId());
        update.setOperationStatus(OP_STATUS_FINISHED);
        update.setFinishTime(now);
        update.setFinishRemark("离散后加工片号全部处理完成");
        update.setStatusOperatorId(operatorId);
        update.setStatusOperatorName(operatorName);
        update.setStatusOperateTime(now);
        hcPlanOrderOperationMapper.updateById(update);
    }

    private void syncCutRoundInspectionIfNeeded(OperationConfig config, HcPlanOrderInventoryLockDO lock,
                                                HcDiscretePostProcessInspectionDO task) {
        if (!"WC-CUT".equals(config.opCode())) {
            return;
        }
        HcCutRoundReportDO update = new HcCutRoundReportDO();
        update.setId(lock.getConsumeReportId());
        update.setInspectionTaskId(task.getId());
        update.setInspectionTaskNo(task.getInspectionTaskNo());
        update.setInspectionStatus(task.getInspectionStatus());
        hcCutRoundReportMapper.updateById(update);
    }

    private HcDiscretePostProcessVO.InspectionRespVO buildInspectionResp(HcDiscretePostProcessInspectionDO task) {
        HcDiscretePostProcessVO.InspectionRespVO respVO = new HcDiscretePostProcessVO.InspectionRespVO();
        respVO.setId(task.getId());
        respVO.setInspectionTaskNo(task.getInspectionTaskNo());
        respVO.setInspectionStatus(task.getInspectionStatus());
        return respVO;
    }

    private void validateSourceForOperation(OperationConfig config, HcPlanOrderInventoryLockDO lock) {
        if (!equalsIgnoreCase(config.sourceOpCode(), lock.getOpCode())) {
            throw invalidParamException(config.opName() + "只能接收" + config.sourceOpName()
                    + "来源片号，当前来源工序：" + firstNotBlank(lock.getOpName(), lock.getOpCode(), "-"));
        }
        if (!isNgPieceLock(lock) && !equalsIgnoreCase(config.sourceType(), lock.getSourceType())) {
            throw invalidParamException(config.opName() + "只能接收" + config.sourceOpName()
                    + "来源片号，当前来源：" + firstNotBlank(lock.getSourceType(), "-"));
        }
        if (!isNgPieceLock(lock) && lock.getSourceId() == null) {
            throw invalidParamException("来源片号缺少来源报工ID，无法报工：" + resolvePieceNo(lock));
        }
        if (isNgPieceLock(lock) && lock.getNgPieceId() == null) {
            throw invalidParamException("来源片号缺少NG逐片ID，无法报工：" + resolvePieceNo(lock));
        }
    }

    private BigDecimal resolveReportQty(HcDiscretePostProcessVO.ReportReqVO reqVO, HcPlanOrderInventoryLockDO lock) {
        BigDecimal qty = firstNonNull(reqVO.getOutputQty(), reqVO.getInputQty(), BigDecimal.ONE);
        qty = defaultDecimal(qty);
        if (qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException("报工数量必须大于0");
        }
        BigDecimal remainingQty = defaultDecimal(lock.getRemainingQty());
        if (remainingQty.compareTo(qty) < 0) {
            throw invalidParamException("来源片号剩余锁定数量不足，剩余 " + remainingQty + "，本次报工 " + qty);
        }
        return qty;
    }

    private boolean isNgResult(HcDiscretePostProcessVO.ReportReqVO reqVO) {
        String result = firstNotBlank(reqVO.getReportResult(), reqVO.getSelfCheck(), "");
        return equalsAnyIgnoreCase(result, "NG", "ABNORMAL", "FAILED", "不合格", "异常")
                || StrUtil.isNotBlank(reqVO.getDefectCode());
    }

    private String resolveSelfCheck(HcDiscretePostProcessVO.ReportReqVO reqVO, boolean ngResult) {
        return firstNotBlank(reqVO.getSelfCheck(), ngResult ? "NG" : "OK");
    }

    private OperationConfig requiredConfig(String opCode) {
        String normalized = normalizeOpCode(opCode);
        OperationConfig config = CONFIG_BY_OP_CODE.get(normalized);
        if (config == null) {
            throw invalidParamException("离散后加工仅支持压槽、粘胶2、裁切");
        }
        return config;
    }

    private String normalizeOpCode(String opCode) {
        return StrUtil.trimToEmpty(opCode).toUpperCase(Locale.ROOT);
    }

    private String normalizeTaskStatus(String status) {
        return StrUtil.blankToDefault(status, "ALL").trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeSourcePool(String sourcePool) {
        String normalized = StrUtil.blankToDefault(sourcePool, SOURCE_POOL_NG).trim().toUpperCase(Locale.ROOT);
        if (SOURCE_POOL_WIP.equals(normalized) || SOURCE_POOL_ALL.equals(normalized)) {
            return normalized;
        }
        return SOURCE_POOL_NG;
    }

    private String normalizeInspectionType(String inspectionType, String defaultValue) {
        String normalized = StrUtil.trimToNull(inspectionType);
        if (normalized == null) {
            return defaultValue;
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String resolvePieceNo(HcPlanOrderInventoryLockDO lock) {
        return firstNotBlank(lock.getBatchNo(), lock.getSourceBatchNo(), lock.getLotNo());
    }

    private boolean isNgPieceLock(HcPlanOrderInventoryLockDO lock) {
        if (lock == null) {
            return false;
        }
        return lock.getNgPieceId() != null
                || equalsIgnoreCase(STOCK_TYPE_NG_PIECE, lock.getLockType())
                || equalsIgnoreCase(STOCK_TYPE_NG_PIECE, lock.getStockType());
    }

    private String buildPlanRemark(List<SelectedSource> sources, List<OperationConfig> operations) {
        String operationText = operations.stream().map(OperationConfig::opName).collect(Collectors.joining("->"));
        String modelPrefix = resolveSelectedModelPrefix(sources.get(0));
        String sourceText = sources.stream()
                .map(source -> firstNotBlank(resolveSelectedSourceParentBatchNo(source), resolveSelectedPieceNo(source)))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .limit(8)
                .collect(Collectors.joining("、"));
        return "；" + LOCK_MARK + "；型号前三位：" + firstNotBlank(modelPrefix, "-")
                + "；多批号加工；工序：" + operationText + "；来源：" + sourceText;
    }

    private String buildDiscreteRouteSnapshotJson(List<SelectedSource> sources, List<OperationConfig> operations,
                                                  String selectedModelPrefix, String planModel,
                                                  String executionRequirement, String remark) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("reportMode", LOCK_MARK);
        snapshot.put("planMode", PLAN_MODE_DISCRETE);
        snapshot.put("sourceType", SOURCE_TYPE_DISCRETE);
        snapshot.put("prodType", PROD_TYPE_TRIAL_PROCESS);
        snapshot.put("prodTypeName", PROD_TYPE_TRIAL_PROCESS_NAME);
        snapshot.put("planModel", planModel);
        snapshot.put("modelPrefix", selectedModelPrefix);
        snapshot.put("executionRequirement", executionRequirement);
        snapshot.put("remark", normalizeManualText(remark));
        snapshot.put("operations", operations.stream()
                .map(config -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("opCode", config.opCode());
                    item.put("opName", config.opName());
                    item.put("sourceType", config.sourceType());
                    item.put("sourceOpName", config.sourceOpName());
                    return item;
                })
                .toList());
        snapshot.put("sources", sources.stream()
                .map(source -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("pieceNo", resolveSelectedPieceNo(source));
                    item.put("modelNo", resolveSelectedModelNo(source));
                    item.put("sourceParentBatchNo", resolveSelectedSourceParentBatchNo(source));
                    item.put("sourcePlanNo", resolveSelectedSourcePlanNo(source));
                    return item;
                })
                .toList());
        return JsonUtils.toJsonString(snapshot);
    }

    private String normalizeManualPlanModel(String planModel, String selectedModelPrefix) {
        String normalized = normalizeManualText(planModel);
        if (StrUtil.isBlank(normalized)) {
            throw invalidParamException("计划型号不能为空");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        String planPrefix = normalized.length() <= 3 ? normalized : normalized.substring(0, 3);
        if (StrUtil.isNotBlank(selectedModelPrefix) && StrUtil.isNotBlank(planPrefix)
                && !equalsIgnoreCase(selectedModelPrefix, planPrefix)) {
            throw invalidParamException("计划型号前三位必须与已选片号一致，当前已选型号前三位："
                    + selectedModelPrefix + "，计划型号：" + normalized);
        }
        return normalized;
    }

    private String normalizeManualText(String value) {
        String text = StrUtil.trimToEmpty(value);
        if (StrUtil.isBlank(text) || "null".equalsIgnoreCase(text)) {
            return "";
        }
        return text;
    }

    private String generateNextDiscretePlanNo() {
        String prefix = LocalDate.now().format(PLAN_NO_DATE_FORMATTER) + "-D";
        int maxSequence = hcPlanOrderMapper.selectPlanNosByPrefixIncludeDeleted(prefix).stream()
                .mapToInt(planNo -> parseDiscretePlanNoSequence(planNo, prefix))
                .max()
                .orElse(0);
        return prefix + String.format("%03d", maxSequence + 1);
    }

    private int parseDiscretePlanNoSequence(String planNo, String prefix) {
        if (StrUtil.isBlank(planNo) || !planNo.startsWith(prefix)) {
            return 0;
        }
        String sequence = planNo.substring(prefix.length());
        if (StrUtil.isBlank(sequence) || !sequence.chars().allMatch(Character::isDigit)) {
            return 0;
        }
        try {
            return Integer.parseInt(sequence);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private String resolveProdType(SelectedSource source) {
        HcInvStockDO stock = source.stock();
        String bizStatus = stock == null ? "" : StrUtil.trimToEmpty(stock.getBizStatus());
        return bizStatus.contains("研发") ? "RND_TRIAL" : "MASS";
    }

    private String resolveBizStatus(HcPlanOrderDO plan) {
        String prodType = StrUtil.trimToEmpty(plan.getProdType());
        String prodTypeName = StrUtil.trimToEmpty(plan.getProdTypeName());
        return "RND_TRIAL".equalsIgnoreCase(prodType) || prodTypeName.contains("研发") ? "研发" : "量产";
    }

    private void assertSameIfPresent(String label, String firstValue, String currentValue, String batchNo) {
        if (StrUtil.isBlank(firstValue) || StrUtil.isBlank(currentValue)) {
            return;
        }
        if (!equalsIgnoreCase(firstValue, currentValue)) {
            throw invalidParamException("离散后加工同一计划只能混合型号前三位相同的片号，"
                    + label + "不一致，片号：" + firstNotBlank(batchNo, "-"));
        }
    }

    private String resolveSelectedSourceParentBatchNo(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        HcNgInventoryPieceDO ngPiece = source.ngPiece();
        return firstNotBlank(ngPiece == null ? null : ngPiece.getSourceParentBatchNo(),
                ngPiece == null ? null : ngPiece.getSourceBatchNo(),
                sourceLock == null ? null : sourceLock.getSourceBatchNo(),
                stock == null ? null : stock.getSourceParentBatchNo(),
                stock == null ? null : stock.getSourceBatchNo());
    }

    private String resolveSelectedSourcePlanNo(SelectedSource source) {
        HcPlanOrderInventoryLockDO sourceLock = source.sourceLock();
        HcInvStockDO stock = source.stock();
        HcNgInventoryPieceDO ngPiece = source.ngPiece();
        return firstNotBlank(ngPiece == null ? null : ngPiece.getSourcePlanNo(),
                sourceLock == null ? null : sourceLock.getSourcePlanNo(),
                sourceLock == null ? null : sourceLock.getTargetPlanNo(),
                stock == null ? null : stock.getSourcePlanNo());
    }

    private Integer resolveNgSourceOpSeq(String processType) {
        return switch (StrUtil.trimToEmpty(processType).toUpperCase(Locale.ROOT)) {
            case "SLITTING" -> 5;
            case "PRESS_SLOT" -> 6;
            case "ADHESIVE2" -> 7;
            default -> null;
        };
    }

    private String resolveNgSourceOpCode(String processType) {
        return switch (StrUtil.trimToEmpty(processType).toUpperCase(Locale.ROOT)) {
            case "SLITTING" -> "WC-SLIT";
            case "PRESS_SLOT" -> "WC-GROOVE";
            case "ADHESIVE2" -> "WC-ADH2";
            default -> processType;
        };
    }

    private boolean equalsIgnoreCase(String a, String b) {
        return StrUtil.equalsIgnoreCase(StrUtil.trim(a), StrUtil.trim(b));
    }

    private boolean equalsAnyIgnoreCase(String value, String... candidates) {
        for (String candidate : candidates) {
            if (equalsIgnoreCase(value, candidate)) {
                return true;
            }
        }
        return false;
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal minDecimal(BigDecimal left, BigDecimal right) {
        BigDecimal normalizedLeft = defaultDecimal(left);
        BigDecimal normalizedRight = defaultDecimal(right);
        return normalizedLeft.compareTo(normalizedRight) <= 0 ? normalizedLeft : normalizedRight;
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
            if (StrUtil.isNotBlank(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private void fillAudit(HcPressSlotReportDO report, LocalDateTime now, Long operatorId, Long tenantId) {
        report.setTenantId(tenantId);
        report.setCreateTime(now);
        report.setUpdateTime(now);
        report.setCreator(operatorId == null ? "" : String.valueOf(operatorId));
        report.setUpdater(operatorId == null ? "" : String.valueOf(operatorId));
    }

    private void fillAudit(HcAdhesive2ReportDO report, LocalDateTime now, Long operatorId, Long tenantId) {
        report.setTenantId(tenantId);
        report.setCreateTime(now);
        report.setUpdateTime(now);
        report.setCreator(operatorId == null ? "" : String.valueOf(operatorId));
        report.setUpdater(operatorId == null ? "" : String.valueOf(operatorId));
    }

    private void fillAudit(HcCutRoundReportDO report, LocalDateTime now, Long operatorId, Long tenantId) {
        report.setTenantId(tenantId);
        report.setCreateTime(now);
        report.setUpdateTime(now);
        report.setCreator(operatorId == null ? "" : String.valueOf(operatorId));
        report.setUpdater(operatorId == null ? "" : String.valueOf(operatorId));
    }

    private record SelectedSource(HcInvStockDO stock, HcPlanOrderInventoryLockDO sourceLock,
                                  HcNgInventoryPieceDO ngPiece, BigDecimal qty) {
    }

    private record ConsumptionResult(String txnNo, LocalDateTime txnTime) {
    }

    private record OperationConfig(String opCode, String opName, int opSeq, String sourceType,
                                   String sourceOpCode, String sourceOpName, String reportSourceType,
                                   String reportSourceTable) {
    }
}
