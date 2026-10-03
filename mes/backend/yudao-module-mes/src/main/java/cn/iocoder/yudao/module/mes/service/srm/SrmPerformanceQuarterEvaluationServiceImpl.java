package cn.iocoder.yudao.module.mes.service.srm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationAvailableSupplierRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationBatchCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceQuarterEvaluationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.srm.vo.SrmPerformanceTrendRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.supplier.MesSupplierDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmEvaluationTemplateVersionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceActualReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceActualValueDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceCalcTraceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcNodeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceMetricCalcRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterEvalDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceQuarterSignDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.srm.SrmPerformanceSupplierConfigItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmEvaluationTemplateVersionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceActualReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceActualValueMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceCalcTraceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceMetricCalcNodeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceMetricCalcRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceQuarterEvalMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceQuarterItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceQuarterLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceQuarterSignMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceSupplierConfigItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.srm.SrmPerformanceSupplierConfigMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.supplier.MesSupplierMapper;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_EVALUATION_DUPLICATE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_EVALUATION_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_EVALUATION_NO_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_EVALUATION_STATUS_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_FORMULA_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_OPERATOR_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_SCORE_INCOMPLETE;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_SCORE_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_SCORER_ONLY;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_SCORER_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_SIGN_REQUIRED;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_SIGN_RESULT_INVALID;
import static cn.iocoder.yudao.module.mes.enums.ErrorCodeConstants.SRM_PERFORMANCE_QUARTER_SIGNER_ONLY;

@Service
@Validated
public class SrmPerformanceQuarterEvaluationServiceImpl implements SrmPerformanceQuarterEvaluationService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PENDING_DATA = "PENDING_DATA";
    private static final String STATUS_SCORING = "SCORING";
    private static final String STATUS_PENDING_CALCULATION = "PENDING_CALCULATION";
    private static final String STATUS_PENDING_SIGN = "PENDING_SIGN";
    private static final String STATUS_SIGNING = "SIGNING";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String TEMPLATE_STATUS_ENABLED = "ENABLED";
    private static final String VERSION_STATUS_PUBLISHED = "PUBLISHED";
    private static final String SCENE_QUARTER = "QUARTER";
    private static final String INDICATOR_MANUAL = "MANUAL_SCORE";
    private static final String INDICATOR_CALCULATED = "CALCULATED_SCORE";
    private static final String INDICATOR_MIXED = "MIXED";
    private static final String SCORE_PENDING = "PENDING";
    private static final String SCORE_AUTO_COMPLETED = "AUTO_COMPLETED";
    private static final String SCORE_COMPLETED = "COMPLETED";
    private static final String SCORE_ADJUSTED = "ADJUSTED";
    private static final String DATA_PENDING = "PENDING";
    private static final String DATA_READY = "READY";
    private static final String SIGN_PENDING = "PENDING";
    private static final String SIGN_COMPLETED = "COMPLETED";
    private static final String SIGN_PASS = "PASS";
    private static final String SIGN_FAIL = "FAIL";
    private static final String EVALUATION_ADMIN_ROLE = "srm_evaluation_admin";
    private static final String SUPER_ADMIN_ROLE = "super_admin";
    private static final String HC_ADMIN_USERNAME = "hcadmin";
    private static final String HC_ADMIN_NICKNAME = "禾臣管理员";
    private static final String ACTUAL_VALUE_ATTACHMENT_BIZ_TYPE = "SRM_PERFORMANCE_ACTUAL_VALUE";
    private static final String TREND_MONTHLY_SCORE_CODE = "__MONTHLY_SCORE__";
    private static final String METRIC_DELIVERY_TOTAL = "DELIVERY_TOTAL_BATCH";
    private static final String METRIC_DELIVERY_ONTIME = "DELIVERY_ONTIME_BATCH";
    private static final String METRIC_EXTRA_FREIGHT = "EXTRA_FREIGHT_COUNT";
    private static final String METRIC_IQC_TOTAL = "IQC_INSPECTION_LOT_COUNT";
    private static final String METRIC_IQC_QUALIFIED = "IQC_QUALIFIED_LOT_COUNT";
    private static final String METRIC_ONLINE_TOTAL = "ONLINE_TOTAL_COUNT";
    private static final String METRIC_ONLINE_GOOD = "ONLINE_GOOD_COUNT";
    private static final String METRIC_ABNORMAL_COUNT = "QUALITY_ABNORMAL_COUNT";
    private static final String METRIC_TIMELY_REPLY = "QUALITY_TIMELY_CLOSE_COUNT";

    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Resource
    private SrmPerformanceQuarterEvalMapper evaluationMapper;
    @Resource
    private SrmPerformanceQuarterItemMapper itemMapper;
    @Resource
    private SrmPerformanceQuarterSignMapper signMapper;
    @Resource
    private SrmPerformanceQuarterLogMapper logMapper;
    @Resource
    private SrmPerformanceCalcTraceMapper traceMapper;
    @Resource
    private SrmPerformanceSupplierConfigMapper supplierConfigMapper;
    @Resource
    private MesSupplierMapper supplierMapper;
    @Resource
    private SrmPerformanceSupplierConfigItemMapper supplierConfigItemMapper;
    @Resource
    private SrmPerformanceMetricCalcRuleMapper calcRuleMapper;
    @Resource
    private SrmPerformanceMetricCalcNodeMapper calcNodeMapper;
    @Resource
    private SrmPerformanceActualReportMapper actualReportMapper;
    @Resource
    private SrmPerformanceActualValueMapper actualValueMapper;
    @Resource
    private SrmAttachmentMapper attachmentMapper;
    @Resource
    private SrmEvaluationTemplateMapper templateMapper;
    @Resource
    private SrmEvaluationTemplateVersionMapper versionMapper;
    @Resource
    private SrmEvaluationTemplateItemMapper templateItemMapper;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createEvaluation(SrmPerformanceQuarterEvaluationSaveReqVO reqVO) {
        String evaluationNo = StrUtil.blankToDefault(StrUtil.trim(reqVO.getEvaluationNo()), buildEvaluationNo());
        if (evaluationMapper.selectByEvaluationNo(evaluationNo) != null) {
            throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_NO_EXISTS);
        }
        TemplateSnapshot template = validatePublishedQuarterTemplate(reqVO.getTemplateVersionId());
        SrmPerformanceQuarterEvalDO sameEvaluation = evaluationMapper.selectBySupplierAndPeriod(reqVO.getSupplierId(),
                reqVO.getEvalYear(), reqVO.getEvalQuarter());
        if (sameEvaluation != null) {
            throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_DUPLICATE);
        }
        UserSnapshot user = currentUser();
        SrmPerformanceQuarterEvalDO evaluation = new SrmPerformanceQuarterEvalDO();
        copyEditableFields(reqVO, evaluation, evaluationNo);
        applyTemplateSnapshot(evaluation, template);
        evaluation.setStatus(STATUS_DRAFT);
        evaluation.setInitiatorId(user.id());
        evaluation.setInitiatorName(user.name());
        evaluation.setGeneratedTime(LocalDateTime.now());
        evaluation.setRedlineTriggered(Boolean.FALSE);
        evaluation.setVersion(0);
        evaluationMapper.insert(evaluation);
        createEvaluationItems(evaluation, template);
        writeLog(evaluation.getId(), "CREATE", null, STATUS_DRAFT, "生成供应商季度评价单",
                Map.of("supplierId", evaluation.getSupplierId(), "templateVersionId", template.version().getId(),
                        "evalYear", evaluation.getEvalYear(), "evalQuarter", evaluation.getEvalQuarter()));
        return evaluation.getId();
    }

    @Override
    public List<SrmPerformanceQuarterEvaluationAvailableSupplierRespVO> getAvailableSuppliers(
            Integer evalYear, Integer evalQuarter, Long templateVersionId, String supplierInfo, String level) {
        String supplierKeyword = StrUtil.trim(supplierInfo);
        String levelFilter = StrUtil.trim(level);
        LambdaQueryWrapperX<MesSupplierDO> supplierQuery = new LambdaQueryWrapperX<>();
        supplierQuery.eqIfPresent(MesSupplierDO::getLevel, StrUtil.isBlank(levelFilter) ? null : levelFilter);
        if (StrUtil.isNotBlank(supplierKeyword)) {
            supplierQuery.and(query -> query.like(MesSupplierDO::getSupplierCode, supplierKeyword)
                    .or()
                    .like(MesSupplierDO::getSupplierName, supplierKeyword));
        }
        supplierQuery.orderByAsc(MesSupplierDO::getSupplierName);
        supplierQuery.orderByDesc(MesSupplierDO::getId);
        List<SrmPerformanceQuarterEvalDO> existingEvaluations = evaluationMapper.selectList(
                new LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO>()
                        .eq(SrmPerformanceQuarterEvalDO::getEvalYear, evalYear)
                        .eq(SrmPerformanceQuarterEvalDO::getEvalQuarter, evalQuarter));
        Set<Long> existingSupplierIds = existingEvaluations.stream()
                .map(SrmPerformanceQuarterEvalDO::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> existingSupplierBusinessKeys = existingEvaluations.stream()
                .map(this::buildEvaluationSupplierBusinessKey)
                .collect(Collectors.toSet());
        return supplierMapper.selectList(supplierQuery).stream()
                .filter(supplier -> supplier.getId() != null)
                .filter(supplier -> !existingSupplierIds.contains(supplier.getId()))
                .filter(supplier -> !existingSupplierBusinessKeys.contains(buildSupplierBusinessKey(supplier)))
                .collect(Collectors.toMap(this::buildAvailableSupplierGroupKey, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new))
                .values().stream()
                .map(this::toAvailableSupplierResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> batchCreateEvaluations(SrmPerformanceQuarterEvaluationBatchCreateReqVO reqVO) {
        TemplateSnapshot template = validatePublishedQuarterTemplate(reqVO.getTemplateVersionId());
        Long versionId = template.version().getId();
        Set<Long> supplierIds = reqVO.getSupplierIds().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (CollUtil.isEmpty(supplierIds)) {
            return List.of();
        }
        Map<Long, MesSupplierDO> supplierMap = supplierMapper.selectList(new LambdaQueryWrapperX<MesSupplierDO>()
                        .in(MesSupplierDO::getId, supplierIds)).stream()
                .filter(supplier -> supplier.getId() != null)
                .collect(Collectors.toMap(MesSupplierDO::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        List<SrmPerformanceQuarterEvalDO> existingEvaluations = evaluationMapper.selectList(
                new LambdaQueryWrapperX<SrmPerformanceQuarterEvalDO>()
                        .eq(SrmPerformanceQuarterEvalDO::getEvalYear, reqVO.getEvalYear())
                        .eq(SrmPerformanceQuarterEvalDO::getEvalQuarter, reqVO.getEvalQuarter()));
        Set<Long> existingSupplierIds = existingEvaluations.stream()
                .map(SrmPerformanceQuarterEvalDO::getSupplierId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<String> existingSupplierBusinessKeys = existingEvaluations.stream()
                .map(this::buildEvaluationSupplierBusinessKey)
                .collect(Collectors.toSet());
        List<Long> ids = new ArrayList<>();
        for (Long supplierId : supplierIds) {
            MesSupplierDO supplier = supplierMap.get(supplierId);
            if (supplier == null
                    || existingSupplierIds.contains(supplierId)
                    || existingSupplierBusinessKeys.contains(buildSupplierBusinessKey(supplier))) {
                continue;
            }
            SrmPerformanceQuarterEvaluationSaveReqVO saveReqVO = new SrmPerformanceQuarterEvaluationSaveReqVO();
            saveReqVO.setSupplierId(supplier.getId());
            saveReqVO.setSupplierCode(supplier.getSupplierCode());
            saveReqVO.setSupplierName(supplier.getSupplierName());
            saveReqVO.setSupplierSourceType("REGISTERED");
            saveReqVO.setEvalYear(reqVO.getEvalYear());
            saveReqVO.setEvalQuarter(reqVO.getEvalQuarter());
            saveReqVO.setTemplateVersionId(versionId);
            ids.add(createEvaluation(saveReqVO));
            existingSupplierIds.add(supplierId);
            existingSupplierBusinessKeys.add(buildSupplierBusinessKey(supplier));
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEvaluation(SrmPerformanceQuarterEvaluationSaveReqVO reqVO) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(reqVO.getId());
        assertMaintainer(evaluation);
        assertAnyStatus(evaluation, STATUS_DRAFT, STATUS_PENDING_DATA);
        String evaluationNo = StrUtil.blankToDefault(StrUtil.trim(reqVO.getEvaluationNo()), evaluation.getEvaluationNo());
        SrmPerformanceQuarterEvalDO sameNo = evaluationMapper.selectByEvaluationNo(evaluationNo);
        if (sameNo != null && !Objects.equals(sameNo.getId(), evaluation.getId())) {
            throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_NO_EXISTS);
        }
        boolean templateChanged = !Objects.equals(evaluation.getTemplateVersionId(), reqVO.getTemplateVersionId());
        boolean supplierPeriodChanged = !Objects.equals(evaluation.getSupplierId(), reqVO.getSupplierId())
                || !Objects.equals(evaluation.getEvalYear(), reqVO.getEvalYear())
                || !Objects.equals(evaluation.getEvalQuarter(), reqVO.getEvalQuarter());
        copyEditableFields(reqVO, evaluation, evaluationNo);
        if (supplierPeriodChanged || templateChanged) {
            SrmPerformanceQuarterEvalDO sameEvaluation = evaluationMapper.selectBySupplierAndPeriod(reqVO.getSupplierId(),
                    reqVO.getEvalYear(), reqVO.getEvalQuarter());
            if (sameEvaluation != null && !Objects.equals(sameEvaluation.getId(), evaluation.getId())) {
                throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_DUPLICATE);
            }
        }
        if (templateChanged) {
            TemplateSnapshot template = validatePublishedQuarterTemplate(reqVO.getTemplateVersionId());
            applyTemplateSnapshot(evaluation, template);
            itemMapper.deleteByEvaluationId(evaluation.getId());
            traceMapper.deleteByEvaluationId(evaluation.getId());
            createEvaluationItems(evaluation, template);
        }
        evaluationMapper.updateById(evaluation);
        writeLog(evaluation.getId(), "UPDATE", evaluation.getStatus(), evaluation.getStatus(),
                "更新供应商季度评价单草稿", Map.of("templateVersionId", evaluation.getTemplateVersionId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEvaluation(Long id) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(id);
        assertMaintainer(evaluation);
        assertAnyStatus(evaluation, STATUS_DRAFT, STATUS_PENDING_DATA);
        itemMapper.deleteByEvaluationId(id);
        traceMapper.deleteByEvaluationId(id);
        signMapper.deleteByEvaluationId(id);
        logMapper.deleteByEvaluationId(id);
        evaluationMapper.deleteById(id);
    }

    @Override
    public SrmPerformanceQuarterEvaluationRespVO getEvaluation(Long id) {
        return buildResp(validateEvaluation(id), true);
    }

    @Override
    public PageResult<SrmPerformanceQuarterEvaluationRespVO> getEvaluationPage(
            SrmPerformanceQuarterEvaluationPageReqVO reqVO) {
        if (Boolean.TRUE.equals(reqVO.getTodoOnly())) {
            Long userId = currentUser().id();
            Set<Long> ids = new LinkedHashSet<>();
            itemMapper.selectListByScorerId(userId, SCORE_PENDING).stream()
                    .map(SrmPerformanceQuarterItemDO::getEvaluationId).forEach(ids::add);
            itemMapper.selectListByScorerId(userId, null).stream()
                    .filter(item -> INDICATOR_MIXED.equals(item.getIndicatorTypeSnapshot())
                            && SCORE_AUTO_COMPLETED.equals(item.getScoreStatus()))
                    .map(SrmPerformanceQuarterItemDO::getEvaluationId).forEach(ids::add);
            signMapper.selectPendingByUserId(userId).stream()
                    .map(SrmPerformanceQuarterSignDO::getEvaluationId).forEach(ids::add);
            reqVO.setVisibleEvaluationIds(ids);
        }
        PageResult<SrmPerformanceQuarterEvalDO> page = evaluationMapper.selectPage(reqVO);
        return new PageResult<>(page.getList().stream().map(item -> buildResp(item, false)).toList(), page.getTotal());
    }

    @Override
    public SrmPerformanceTrendRespVO getPerformanceTrend(Long evaluationId, Long supplierId, Integer evalYear,
                                                         String indicatorCodes) {
        SrmPerformanceQuarterEvalDO evaluation = evaluationId == null ? null : validateEvaluation(evaluationId);
        if (evaluation != null) {
            supplierId = evaluation.getSupplierId();
            evalYear = evalYear == null ? evaluation.getEvalYear() : evalYear;
        }
        TrendSource source = loadTrendSource(evaluation, supplierId);
        Integer finalEvalYear = evalYear == null ? LocalDateTime.now().getYear() : evalYear;
        List<String> selectedCodes = parseTrendIndicatorCodes(indicatorCodes, source.items());
        MonthValueContext context = loadMonthValueContext(source.supplierId(), finalEvalYear);
        Map<String, TrendItem> itemMap = source.items().stream()
                .collect(Collectors.toMap(TrendItem::indicatorCode, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        LinkedHashMap<String, SrmPerformanceTrendRespVO.Panel> panelMap = new LinkedHashMap<>();
        for (String selectedCode : selectedCodes) {
            SrmPerformanceTrendRespVO.Panel panel;
            if (TREND_MONTHLY_SCORE_CODE.equals(selectedCode)) {
                panel = buildMonthlyScorePanel(context);
            } else {
                TrendItem item = itemMap.get(selectedCode);
                if (item == null || !isTrendItem(item)) {
                    continue;
                }
                panel = buildMetricTrendPanel(item, context);
            }
            if (panel != null) {
                panelMap.putIfAbsent(trendPanelUniqueKey(panel), panel);
            }
        }
        SrmPerformanceTrendRespVO resp = new SrmPerformanceTrendRespVO();
        resp.setSupplierId(source.supplierId());
        resp.setSupplierCode(source.supplierCode());
        resp.setSupplierName(source.supplierName());
        resp.setEvalYear(finalEvalYear);
        resp.setTree(buildTrendTree(source.items()));
        resp.setSelectedIndicatorCodes(selectedCodes);
        resp.setPanels(new ArrayList<>(panelMap.values()));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pullActuals(Long id) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(id);
        assertMaintainer(evaluation);
        assertAnyStatus(evaluation, STATUS_DRAFT, STATUS_PENDING_DATA, STATUS_SCORING);
        List<SrmPerformanceQuarterItemDO> items = itemMapper.selectListByEvaluationId(id);
        ActualContext actualContext = loadActualContext(evaluation);
        List<String> missingMessages = new ArrayList<>();
        BigDecimal autoScore = BigDecimal.ZERO;
        for (SrmPerformanceQuarterItemDO item : items) {
            if (!isCalculatedItem(item)) {
                continue;
            }
            CalcResult result = calculateItem(evaluation, item, actualContext, missingMessages);
            if (result.ready()) {
                item.setCalcActualValue(result.actualValue());
                item.setCalcScore(result.score());
                if (INDICATOR_CALCULATED.equals(item.getIndicatorTypeSnapshot())) {
                    item.setFinalScore(result.score());
                    item.setScoreStatus(SCORE_AUTO_COMPLETED);
                } else if (INDICATOR_MIXED.equals(item.getIndicatorTypeSnapshot())
                        && SCORE_PENDING.equals(item.getScoreStatus())) {
                    item.setFinalScore(result.score());
                    item.setScoreStatus(SCORE_AUTO_COMPLETED);
                }
                item.setDataStatus(DATA_READY);
                autoScore = autoScore.add(valueOrZero(item.getCalcScore()));
            } else {
                item.setDataStatus(DATA_PENDING);
                item.setScoreStatus(SCORE_PENDING);
                item.setCalcActualValue(null);
                item.setCalcScore(null);
                item.setFinalScore(null);
            }
            itemMapper.updateById(item);
        }
        String fromStatus = evaluation.getStatus();
        evaluation.setAutoScore(autoScore);
        evaluation.setAutoCalculatedTime(LocalDateTime.now());
        if (CollUtil.isNotEmpty(missingMessages)) {
            evaluation.setStatus(STATUS_PENDING_DATA);
            evaluationMapper.updateById(evaluation);
            writeLog(id, "PULL_ACTUALS", fromStatus, STATUS_PENDING_DATA,
                    "拉取实际值存在缺失，请补齐确认后重算", missingMessages);
            return;
        }
        evaluation.setStatus(hasManualPending(items) ? STATUS_SCORING : STATUS_PENDING_CALCULATION);
        evaluationMapper.updateById(evaluation);
        writeLog(id, "PULL_ACTUALS", fromStatus, evaluation.getStatus(), "拉取实际值并完成计算类指标计算",
                Map.of("autoScore", autoScore));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendForScoring(Long id) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(id);
        assertMaintainer(evaluation);
        assertAnyStatus(evaluation, STATUS_DRAFT, STATUS_SCORING);
        List<SrmPerformanceQuarterItemDO> items = itemMapper.selectListByEvaluationId(id);
        if (items.stream().filter(this::isManualItem).anyMatch(item -> item.getScorerUserId() == null)) {
            throw exception(SRM_PERFORMANCE_QUARTER_SCORER_REQUIRED);
        }
        if (items.stream().filter(this::isCalculatedItem).anyMatch(item -> !DATA_READY.equals(item.getDataStatus()))) {
            evaluation.setStatus(STATUS_PENDING_DATA);
            evaluationMapper.updateById(evaluation);
            throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_STATUS_INVALID);
        }
        String fromStatus = evaluation.getStatus();
        evaluation.setStatus(hasManualPending(items) ? STATUS_SCORING : STATUS_PENDING_CALCULATION);
        evaluation.setSendTime(LocalDateTime.now());
        evaluationMapper.updateById(evaluation);
        writeLog(id, "SEND_SCORING", fromStatus, evaluation.getStatus(), "发送季度评价人工评分",
                Map.of("manualItemCount", items.stream().filter(this::isManualItem).count()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitScore(SrmPerformanceQuarterEvaluationActionReqVO.Score reqVO) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertStatus(evaluation, STATUS_SCORING);
        Long userId = currentUser().id();
        List<SrmPerformanceQuarterItemDO> allItems = itemMapper.selectListByEvaluationId(evaluation.getId());
        Map<Long, SrmPerformanceQuarterItemDO> myItemMap = allItems.stream()
                .filter(this::isManualItem)
                .filter(item -> Objects.equals(item.getScorerUserId(), userId))
                .collect(Collectors.toMap(SrmPerformanceQuarterItemDO::getId, Function.identity()));
        if (myItemMap.isEmpty()) {
            throw exception(SRM_PERFORMANCE_QUARTER_SCORER_ONLY);
        }
        LocalDateTime now = LocalDateTime.now();
        for (SrmPerformanceQuarterEvaluationActionReqVO.ScoreItem scoreItem : reqVO.getItems()) {
            SrmPerformanceQuarterItemDO item = myItemMap.get(scoreItem.getItemId());
            if (item == null) {
                throw exception(SRM_PERFORMANCE_QUARTER_SCORER_ONLY);
            }
            if (scoreItem.getManualScore().compareTo(valueOrZero(item.getMaxScoreSnapshot())) > 0) {
                throw exception(SRM_PERFORMANCE_QUARTER_SCORE_INVALID, item.getIndicatorNameSnapshot());
            }
            item.setManualScore(scoreItem.getManualScore());
            item.setFinalScore(scoreItem.getManualScore());
            item.setScoringDescription(StrUtil.trim(scoreItem.getScoringDescription()));
            item.setActualScoreTime(now);
            item.setScoreStatus(SCORE_COMPLETED);
            itemMapper.updateById(item);
        }
        List<SrmPerformanceQuarterItemDO> refreshedItems = itemMapper.selectListByEvaluationId(evaluation.getId());
        if (refreshedItems.stream().filter(this::isManualItem).allMatch(item -> SCORE_COMPLETED.equals(item.getScoreStatus()))) {
            evaluation.setStatus(STATUS_PENDING_CALCULATION);
            evaluation.setAllScoredTime(now);
            evaluationMapper.updateById(evaluation);
        }
        writeLog(evaluation.getId(), "SCORE", STATUS_SCORING, evaluation.getStatus(), "提交季度评价人工评分",
                Map.of("userId", userId, "itemCount", reqVO.getItems().size()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateTotal(Long id) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(id);
        assertMaintainer(evaluation);
        assertAnyStatus(evaluation, STATUS_PENDING_CALCULATION, STATUS_SCORING);
        List<SrmPerformanceQuarterItemDO> items = itemMapper.selectListByEvaluationId(id);
        if (items.stream().anyMatch(item -> item.getFinalScore() == null
                || (isManualItem(item) && !SCORE_COMPLETED.equals(item.getScoreStatus())))) {
            throw exception(SRM_PERFORMANCE_QUARTER_SCORE_INCOMPLETE);
        }
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal autoScore = BigDecimal.ZERO;
        BigDecimal manualScore = BigDecimal.ZERO;
        List<String> redlineMessages = new ArrayList<>();
        for (SrmPerformanceQuarterItemDO item : items) {
            BigDecimal finalScore = valueOrZero(item.getFinalScore());
            totalScore = totalScore.add(finalScore);
            if (isCalculatedItem(item)) {
                autoScore = autoScore.add(valueOrZero(item.getCalcScore()));
            }
            if (isManualItem(item)) {
                manualScore = manualScore.add(valueOrZero(item.getManualScore()));
            }
            if (item.getRedlineScoreSnapshot() != null && finalScore.compareTo(item.getRedlineScoreSnapshot()) < 0) {
                redlineMessages.add(item.getIndicatorNameSnapshot() + "低于红线分值"
                        + decimalText(item.getRedlineScoreSnapshot()));
            }
        }
        String fromStatus = evaluation.getStatus();
        evaluation.setTotalScore(totalScore);
        evaluation.setAutoScore(autoScore);
        evaluation.setManualScore(manualScore);
        evaluation.setEvalGrade(resolveGrade(totalScore));
        evaluation.setRedlineTriggered(CollUtil.isNotEmpty(redlineMessages));
        evaluation.setRedlineDescription(CollUtil.isEmpty(redlineMessages) ? null : String.join("；", redlineMessages));
        evaluation.setCalculatedTime(LocalDateTime.now());
        evaluation.setStatus(STATUS_PENDING_SIGN);
        evaluationMapper.updateById(evaluation);
        writeLog(id, "CALCULATE_TOTAL", fromStatus, STATUS_PENDING_SIGN, "计算季度总分与等级",
                Map.of("totalScore", totalScore, "evalGrade", evaluation.getEvalGrade(),
                        "redlineTriggered", evaluation.getRedlineTriggered()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startSign(SrmPerformanceQuarterEvaluationActionReqVO.StartSign reqVO) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertMaintainer(evaluation);
        assertStatus(evaluation, STATUS_PENDING_SIGN);
        if (CollUtil.isEmpty(reqVO.getSigners())) {
            throw exception(SRM_PERFORMANCE_QUARTER_SIGN_REQUIRED);
        }
        List<Long> userIds = reqVO.getSigners().stream().map(SrmPerformanceQuarterEvaluationActionReqVO.Signer::getUserId)
                .filter(Objects::nonNull).distinct().toList();
        if (CollUtil.isEmpty(userIds)) {
            throw exception(SRM_PERFORMANCE_QUARTER_SIGN_REQUIRED);
        }
        adminUserApi.validateUserList(userIds);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> deptIds = userMap.values().stream().map(AdminUserRespDTO::getDeptId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Map.of() : deptApi.getDeptMap(deptIds);
        signMapper.deleteByEvaluationId(evaluation.getId());
        Map<Long, SrmPerformanceQuarterEvaluationActionReqVO.Signer> reqSignerMap = reqVO.getSigners().stream()
                .collect(Collectors.toMap(SrmPerformanceQuarterEvaluationActionReqVO.Signer::getUserId,
                        Function.identity(), (left, right) -> right, LinkedHashMap::new));
        for (Long userId : reqSignerMap.keySet()) {
            AdminUserRespDTO user = userMap.get(userId);
            SrmPerformanceQuarterEvaluationActionReqVO.Signer reqSigner = reqSignerMap.get(userId);
            SrmPerformanceQuarterSignDO sign = new SrmPerformanceQuarterSignDO();
            sign.setEvaluationId(evaluation.getId());
            sign.setDeptCode(StrUtil.trim(reqSigner.getDeptCode()));
            sign.setDeptName(StrUtil.blankToDefault(StrUtil.trim(reqSigner.getDeptName()),
                    resolveDeptName(user, deptMap)));
            sign.setUserId(userId);
            sign.setUserName(StrUtil.blankToDefault(StrUtil.trim(reqSigner.getUserName()), userName(user)));
            sign.setSignStatus(SIGN_PENDING);
            signMapper.insert(sign);
        }
        String fromStatus = evaluation.getStatus();
        evaluation.setStatus(STATUS_SIGNING);
        evaluation.setSignStartTime(LocalDateTime.now());
        evaluationMapper.updateById(evaluation);
        writeLog(evaluation.getId(), "START_SIGN", fromStatus, STATUS_SIGNING, "发起季度评价部门会签",
                Map.of("signUserIds", userIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSign(SrmPerformanceQuarterEvaluationActionReqVO.Sign reqVO) {
        SrmPerformanceQuarterEvalDO evaluation = validateEvaluation(reqVO.getEvaluationId());
        assertStatus(evaluation, STATUS_SIGNING);
        if (!SIGN_PASS.equals(reqVO.getSignResult()) && !SIGN_FAIL.equals(reqVO.getSignResult())) {
            throw exception(SRM_PERFORMANCE_QUARTER_SIGN_RESULT_INVALID);
        }
        Long userId = currentUser().id();
        List<SrmPerformanceQuarterSignDO> signs = signMapper.selectListByEvaluationId(evaluation.getId());
        SrmPerformanceQuarterSignDO mySign = signs.stream()
                .filter(sign -> Objects.equals(sign.getUserId(), userId) && SIGN_PENDING.equals(sign.getSignStatus()))
                .findFirst().orElseThrow(() -> exception(SRM_PERFORMANCE_QUARTER_SIGNER_ONLY));
        mySign.setSignStatus(SIGN_COMPLETED);
        mySign.setSignResult(reqVO.getSignResult());
        mySign.setSignOpinion(StrUtil.trim(reqVO.getSignOpinion()));
        mySign.setSignTime(LocalDateTime.now());
        signMapper.updateById(mySign);
        signs = signMapper.selectListByEvaluationId(evaluation.getId());
        String fromStatus = evaluation.getStatus();
        if (signs.stream().anyMatch(sign -> SIGN_FAIL.equals(sign.getSignResult()))) {
            evaluation.setStatus(STATUS_REJECTED);
            evaluationMapper.updateById(evaluation);
        } else if (signs.stream().allMatch(sign -> SIGN_COMPLETED.equals(sign.getSignStatus()))) {
            evaluation.setStatus(STATUS_ARCHIVED);
            evaluation.setArchivedTime(LocalDateTime.now());
            evaluationMapper.updateById(evaluation);
        }
        writeLog(evaluation.getId(), "SIGN", fromStatus, evaluation.getStatus(), "提交季度评价会签意见",
                Map.of("signResult", reqVO.getSignResult(), "userId", userId));
    }

    private TrendSource loadTrendSource(SrmPerformanceQuarterEvalDO evaluation, Long supplierId) {
        if (evaluation != null) {
            List<TrendItem> items = itemMapper.selectListByEvaluationId(evaluation.getId()).stream()
                    .map(this::toTrendItem)
                    .toList();
            return new TrendSource(evaluation.getSupplierId(), evaluation.getSupplierCode(),
                    evaluation.getSupplierName(), items);
        }
        SrmPerformanceSupplierConfigDO config;
        if (supplierId == null) {
            config = supplierConfigMapper.selectEnabledList().stream().findFirst().orElse(null);
        } else {
            config = supplierConfigMapper.selectEnabledBySupplierId(supplierId);
        }
        if (config != null) {
            List<TrendItem> items = supplierConfigItemMapper
                    .selectListByConfigAndTemplateVersion(config.getId(), config.getCurrentTemplateVersionId())
                    .stream()
                    .map(this::toTrendItem)
                    .toList();
            return new TrendSource(config.getSupplierId(), config.getSupplierCode(), config.getSupplierName(), items);
        }
        MesSupplierDO supplier = supplierId == null ? null : supplierMapper.selectById(supplierId);
        return new TrendSource(supplierId, supplier == null ? null : supplier.getSupplierCode(),
                supplier == null ? null : supplier.getSupplierName(), List.of());
    }

    private List<String> parseTrendIndicatorCodes(String indicatorCodes, List<TrendItem> items) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        if (StrUtil.isNotBlank(indicatorCodes)) {
            for (String code : indicatorCodes.split(",")) {
                String trimmed = StrUtil.trim(code);
                if (StrUtil.isNotBlank(trimmed)) {
                    result.add(trimmed);
                }
            }
        }
        if (CollUtil.isEmpty(result)) {
            result.add(TREND_MONTHLY_SCORE_CODE);
            items.stream()
                    .filter(this::isTrendItem)
                    .map(TrendItem::indicatorCode)
                    .forEach(result::add);
        }
        return new ArrayList<>(result);
    }

    private MonthValueContext loadMonthValueContext(Long supplierId, Integer evalYear) {
        if (supplierId == null || evalYear == null) {
            return new MonthValueContext(Map.of(), Map.of());
        }
        List<SrmPerformanceActualReportDO> reports = actualReportMapper
                .selectMonthlyBySupplierYear(supplierId, evalYear);
        Map<String, List<BigDecimal>> valuesByMetric = new HashMap<>();
        if (CollUtil.isNotEmpty(reports)) {
            Map<Long, SrmPerformanceActualReportDO> reportMap = reports.stream()
                    .filter(report -> report.getId() != null)
                    .collect(Collectors.toMap(SrmPerformanceActualReportDO::getId, Function.identity(),
                            (left, right) -> right, LinkedHashMap::new));
            List<SrmPerformanceActualValueDO> values = actualValueMapper.selectListByReportIds(
                    new ArrayList<>(reportMap.keySet()));
            for (SrmPerformanceActualValueDO value : values) {
                SrmPerformanceActualReportDO report = reportMap.get(value.getReportId());
                if (report == null || report.getEvalMonth() == null
                        || report.getEvalMonth() < 1 || report.getEvalMonth() > 12
                        || StrUtil.isBlank(value.getMetricCode()) || value.getNumericValue() == null) {
                    continue;
                }
                List<BigDecimal> monthValues = valuesByMetric.computeIfAbsent(value.getMetricCode(),
                        key -> emptyMonthNumbers());
                int monthIndex = report.getEvalMonth() - 1;
                BigDecimal currentValue = monthValues.get(monthIndex);
                monthValues.set(monthIndex, value.getNumericValue().add(valueOrZero(currentValue)));
            }
        }
        Map<Integer, SrmPerformanceQuarterEvalDO> evaluationsByQuarter = new LinkedHashMap<>();
        evaluationMapper.selectListBySupplierYear(supplierId, evalYear).forEach(item -> {
            Integer quarter = item.getEvalQuarter();
            if (quarter != null && quarter >= 1 && quarter <= 4 && !evaluationsByQuarter.containsKey(quarter)) {
                evaluationsByQuarter.put(quarter, item);
            }
        });
        return new MonthValueContext(valuesByMetric, evaluationsByQuarter);
    }

    private List<SrmPerformanceTrendRespVO.TreeNode> buildTrendTree(List<TrendItem> items) {
        List<SrmPerformanceTrendRespVO.TreeNode> roots = new ArrayList<>();
        SrmPerformanceTrendRespVO.TreeNode scoreGroup = trendTreeNode("group:score", "月度考核推移",
                null, null, null, null, Boolean.FALSE);
        scoreGroup.setChildren(List.of(trendTreeNode("indicator:" + TREND_MONTHLY_SCORE_CODE,
                "月度考核推移图", TREND_MONTHLY_SCORE_CODE, "月度考核推移图", "总分趋势",
                "SCORE", Boolean.TRUE)));
        roots.add(scoreGroup);
        LinkedHashMap<String, SrmPerformanceTrendRespVO.TreeNode> groupMap = new LinkedHashMap<>();
        items.stream().filter(this::isTrendItem).forEach(item -> {
            String groupKey = StrUtil.blankToDefault(item.groupName(), "未分组");
            SrmPerformanceTrendRespVO.TreeNode group = groupMap.computeIfAbsent(groupKey,
                    key -> trendTreeNode("group:" + groupKey, groupKey, null, null, groupKey, null, Boolean.FALSE));
            List<SrmPerformanceTrendRespVO.TreeNode> children = group.getChildren();
            if (children == null) {
                children = new ArrayList<>();
                group.setChildren(children);
            }
            children.add(trendTreeNode("indicator:" + item.indicatorCode(), item.indicatorName(),
                    item.indicatorCode(), item.indicatorName(), item.groupName(),
                    resolveTrendChartType(item), Boolean.TRUE));
        });
        roots.addAll(groupMap.values());
        return roots;
    }

    private SrmPerformanceTrendRespVO.TreeNode trendTreeNode(String key, String title, String indicatorCode,
                                                            String indicatorName, String groupName, String chartType,
                                                            Boolean selectable) {
        SrmPerformanceTrendRespVO.TreeNode node = new SrmPerformanceTrendRespVO.TreeNode();
        node.setKey(key);
        node.setTitle(title);
        node.setIndicatorCode(indicatorCode);
        node.setIndicatorName(indicatorName);
        node.setGroupName(groupName);
        node.setChartType(chartType);
        node.setSelectable(selectable);
        return node;
    }

    private SrmPerformanceTrendRespVO.Panel buildMetricTrendPanel(TrendItem item, MonthValueContext context) {
        String chartType = resolveTrendChartType(item);
        if ("DELIVERY".equals(chartType)) {
            return buildDeliveryPanel(context);
        }
        if ("LAR".equals(chartType)) {
            return buildLarPanel(context);
        }
        if ("ONLINE".equals(chartType)) {
            return buildOnlinePanel(context);
        }
        if ("ABNORMAL_REPLY".equals(chartType)) {
            return buildAbnormalReplyPanel(context);
        }
        return buildGenericPanel(item, context);
    }

    private SrmPerformanceTrendRespVO.Panel buildDeliveryPanel(MonthValueContext context) {
        List<BigDecimal> deliveryTotal = monthlyMetricValues(context, METRIC_DELIVERY_TOTAL);
        List<BigDecimal> deliveryOntime = monthlyMetricValues(context, METRIC_DELIVERY_ONTIME);
        List<BigDecimal> freight = monthlyMetricValues(context, METRIC_EXTRA_FREIGHT);
        List<BigDecimal> rate = dividePercent(deliveryOntime, deliveryTotal);
        List<BigDecimal> target = fixedMonthlyValues(BigDecimal.valueOf(100));
        SrmPerformanceTrendRespVO.Panel panel = trendPanel("KPI-D-DELIVERY", "交付能力", "交付考核",
                "DELIVERY", "综合");
        panel.setSeries(List.of(
                trendSeries("交付总批次", "bar", "批", Boolean.FALSE, deliveryTotal),
                trendSeries("按时交付批", "bar", "批", Boolean.FALSE, deliveryOntime),
                trendSeries("超额运费次数", "line", "次", Boolean.FALSE, freight)));
        panel.setRows(List.of(
                trendRow("交付总批次", "批", Boolean.FALSE, deliveryTotal, sumSummary(deliveryTotal, false)),
                trendRow("按时交付批", "批", Boolean.FALSE, deliveryOntime, sumSummary(deliveryOntime, false)),
                trendRow("目标值", "%", Boolean.TRUE, target, "100.0%"),
                trendRow("批次达成率", "%", Boolean.TRUE, rate, ratioSummary(deliveryOntime, deliveryTotal)),
                trendRow("超额运费次数", "次", Boolean.FALSE, freight, sumSummary(freight, false))));
        return panel;
    }

    private SrmPerformanceTrendRespVO.Panel buildLarPanel(MonthValueContext context) {
        List<BigDecimal> total = monthlyMetricValues(context, METRIC_IQC_TOTAL);
        List<BigDecimal> qualified = monthlyMetricValues(context, METRIC_IQC_QUALIFIED);
        List<BigDecimal> rate = dividePercent(qualified, total);
        SrmPerformanceTrendRespVO.Panel panel = trendPanel("KPI-Q-LAR", "入料合格率", "质量考核",
                "LAR", "综合");
        panel.setSeries(List.of(
                trendSeries("检验总批次", "bar", "批", Boolean.FALSE, total),
                trendSeries("合格批次数", "bar", "批", Boolean.FALSE, qualified),
                trendSeries("入料合格率", "line", "%", Boolean.TRUE, rate)));
        panel.setRows(List.of(
                trendRow("检验总批次", "批", Boolean.FALSE, total, sumSummary(total, false)),
                trendRow("合格批次数", "批", Boolean.FALSE, qualified, sumSummary(qualified, false)),
                trendRow("入料合格率", "%", Boolean.TRUE, rate, ratioSummary(qualified, total))));
        return panel;
    }

    private SrmPerformanceTrendRespVO.Panel buildOnlinePanel(MonthValueContext context) {
        List<BigDecimal> total = monthlyMetricValues(context, METRIC_ONLINE_TOTAL);
        List<BigDecimal> good = monthlyMetricValues(context, METRIC_ONLINE_GOOD);
        List<BigDecimal> bad = subtract(total, good);
        List<BigDecimal> rate = dividePercent(good, total);
        SrmPerformanceTrendRespVO.Panel panel = trendPanel("KPI-Q-ONLINE", "上线合格率", "质量考核",
                "ONLINE", "综合");
        panel.setSeries(List.of(
                trendSeries("上线总数", "bar", "批", Boolean.FALSE, total),
                trendSeries("上线不良总数", "bar", "批", Boolean.FALSE, bad),
                trendSeries("上线合格率", "line", "%", Boolean.TRUE, rate)));
        panel.setRows(List.of(
                trendRow("上线总数", "批", Boolean.FALSE, total, sumSummary(total, false)),
                trendRow("上线不良总数", "批", Boolean.FALSE, bad, sumSummary(bad, false)),
                trendRow("上线合格率", "%", Boolean.TRUE, rate, ratioSummary(good, total))));
        return panel;
    }

    private SrmPerformanceTrendRespVO.Panel buildAbnormalReplyPanel(MonthValueContext context) {
        List<BigDecimal> abnormal = monthlyMetricValues(context, METRIC_ABNORMAL_COUNT);
        List<BigDecimal> timely = monthlyMetricValues(context, METRIC_TIMELY_REPLY);
        List<BigDecimal> rate = dividePercent(timely, abnormal);
        SrmPerformanceTrendRespVO.Panel panel = trendPanel("KPI-Q-IMPROVE", "异常回复率", "质量考核",
                "ABNORMAL_REPLY", "平均");
        panel.setSeries(List.of(
                trendSeries("投诉次数", "bar", "次", Boolean.FALSE, abnormal),
                trendSeries("及时回复次数", "bar", "次", Boolean.FALSE, timely),
                trendSeries("异常回复率", "line", "%", Boolean.TRUE, rate)));
        panel.setRows(List.of(
                trendRow("投诉次数", "次", Boolean.FALSE, abnormal, averageSummary(abnormal, false)),
                trendRow("及时回复次数", "次", Boolean.FALSE, timely, averageSummary(timely, false)),
                trendRow("异常回复率", "%", Boolean.TRUE, rate, averageSummary(rate, true))));
        return panel;
    }

    private SrmPerformanceTrendRespVO.Panel buildMonthlyScorePanel(MonthValueContext context) {
        List<BigDecimal> scores = emptyMonthNumbers();
        List<String> grades = emptyMonthTexts();
        context.evaluationsByQuarter().forEach((quarter, evaluation) -> {
            int monthIndex = quarter * 3 - 1;
            scores.set(monthIndex, evaluation.getTotalScore());
            grades.set(monthIndex, StrUtil.blankToDefault(evaluation.getEvalGrade(), "-"));
        });
        SrmPerformanceTrendRespVO.Panel panel = trendPanel(TREND_MONTHLY_SCORE_CODE, "月度考核推移图",
                "总分趋势", "SCORE", "平均");
        panel.setSeries(List.of(trendSeries("得分数", "line", "分", Boolean.FALSE, scores)));
        panel.setRows(List.of(
                trendRow("得分数", "分", Boolean.FALSE, scores, averageSummary(scores, false)),
                trendTextRow("评价", grades, "-")));
        return panel;
    }

    private SrmPerformanceTrendRespVO.Panel buildGenericPanel(TrendItem item, MonthValueContext context) {
        List<SrmPerformanceMetricCalcNodeDO> nodes = item.calcRuleId() == null ? List.of()
                : calcNodeMapper.selectListByRuleId(item.calcRuleId()).stream()
                .filter(node -> "SOURCE_METRIC".equals(node.getNodeType()))
                .toList();
        if (CollUtil.isEmpty(nodes)) {
            SrmPerformanceMetricCalcNodeDO node = new SrmPerformanceMetricCalcNodeDO();
            node.setSourceMetricCode(item.indicatorCode());
            node.setSourceMetricName(item.indicatorName());
            node.setUnit(item.targetUnit());
            nodes = List.of(node);
        }
        List<SrmPerformanceTrendRespVO.Series> series = new ArrayList<>();
        List<SrmPerformanceTrendRespVO.Row> rows = new ArrayList<>();
        for (int index = 0; index < nodes.size(); index += 1) {
            SrmPerformanceMetricCalcNodeDO node = nodes.get(index);
            List<BigDecimal> values = monthlyMetricValues(context,
                    StrUtil.blankToDefault(node.getSourceMetricCode(), item.indicatorCode()));
            String name = StrUtil.blankToDefault(node.getSourceMetricName(), item.indicatorName());
            String unit = StrUtil.blankToDefault(node.getUnit(), item.targetUnit());
            series.add(trendSeries(name, index < 2 ? "bar" : "line", unit, Boolean.FALSE, values));
            rows.add(trendRow(name, unit, Boolean.FALSE, values, sumSummary(values, false)));
        }
        SrmPerformanceTrendRespVO.Panel panel = trendPanel(item.indicatorCode(), item.indicatorName(),
                item.groupName(), "GENERIC", "综合");
        panel.setSeries(series);
        panel.setRows(rows);
        return panel;
    }

    private SrmPerformanceTrendRespVO.Panel trendPanel(String indicatorCode, String indicatorName,
                                                       String groupName, String chartType, String summaryLabel) {
        SrmPerformanceTrendRespVO.Panel panel = new SrmPerformanceTrendRespVO.Panel();
        panel.setIndicatorCode(indicatorCode);
        panel.setIndicatorName(indicatorName);
        panel.setGroupName(groupName);
        panel.setChartType(chartType);
        panel.setTitle(indicatorName);
        panel.setSummaryLabel(summaryLabel);
        panel.setMonths(monthLabels());
        return panel;
    }

    private SrmPerformanceTrendRespVO.Series trendSeries(String name, String type, String unit, Boolean percent,
                                                        List<BigDecimal> values) {
        SrmPerformanceTrendRespVO.Series series = new SrmPerformanceTrendRespVO.Series();
        series.setName(name);
        series.setType(type);
        series.setUnit(unit);
        series.setPercent(percent);
        series.setValues(values);
        return series;
    }

    private SrmPerformanceTrendRespVO.Row trendRow(String label, String unit, Boolean percent,
                                                  List<BigDecimal> values, String summary) {
        SrmPerformanceTrendRespVO.Row row = new SrmPerformanceTrendRespVO.Row();
        row.setLabel(label);
        row.setUnit(unit);
        row.setPercent(percent);
        row.setValues(values.stream().map(value -> formatTrendCell(value, Boolean.TRUE.equals(percent))).toList());
        row.setSummary(summary);
        return row;
    }

    private SrmPerformanceTrendRespVO.Row trendTextRow(String label, List<String> values, String summary) {
        SrmPerformanceTrendRespVO.Row row = new SrmPerformanceTrendRespVO.Row();
        row.setLabel(label);
        row.setUnit(null);
        row.setPercent(Boolean.FALSE);
        row.setValues(values);
        row.setSummary(summary);
        return row;
    }

    private String trendPanelUniqueKey(SrmPerformanceTrendRespVO.Panel panel) {
        return "DELIVERY".equals(panel.getChartType()) ? panel.getChartType() : panel.getIndicatorCode();
    }

    private String resolveTrendChartType(TrendItem item) {
        String code = StrUtil.blankToDefault(item.indicatorCode(), "");
        String name = StrUtil.blankToDefault(item.indicatorName(), "");
        if ("KPI-D-DELIVERY".equals(code) || "KPI-D-FREIGHT".equals(code)
                || containsAny(name, "交付", "交期", "运费")) {
            return "DELIVERY";
        }
        if ("KPI-Q-LAR".equals(code) || containsAny(name, "入料合格", "到料合格")) {
            return "LAR";
        }
        if ("KPI-Q-ONLINE".equals(code) || containsAny(name, "上线合格", "使用效果")) {
            return "ONLINE";
        }
        if ("KPI-Q-IMPROVE".equals(code) || containsAny(name, "异常回复", "品质改善")) {
            return "ABNORMAL_REPLY";
        }
        return "GENERIC";
    }

    private boolean isTrendItem(TrendItem item) {
        return INDICATOR_CALCULATED.equals(item.indicatorType()) || INDICATOR_MIXED.equals(item.indicatorType())
                || item.calcRuleId() != null;
    }

    private TrendItem toTrendItem(SrmPerformanceQuarterItemDO item) {
        return new TrendItem(item.getIndicatorCodeSnapshot(), item.getIndicatorNameSnapshot(),
                item.getGroupCodeSnapshot(), item.getGroupNameSnapshot(), item.getGroupSort(), item.getIndicatorSort(),
                item.getScoringRuleSnapshot(), item.getMaxScoreSnapshot(), item.getTargetValueSnapshot(),
                item.getTargetUnitSnapshot(), item.getIndicatorTypeSnapshot(), item.getCalcRuleId());
    }

    private TrendItem toTrendItem(SrmPerformanceSupplierConfigItemDO item) {
        return new TrendItem(item.getIndicatorCodeSnapshot(), item.getIndicatorNameSnapshot(),
                item.getGroupCodeSnapshot(), item.getGroupNameSnapshot(), item.getGroupSort(), item.getIndicatorSort(),
                item.getScoringRuleSnapshot(), item.getMaxScoreSnapshot(), item.getTargetValue(),
                item.getTargetUnit(), item.getIndicatorType(), item.getCalcRuleId());
    }

    private List<BigDecimal> monthlyMetricValues(MonthValueContext context, String metricCode) {
        List<BigDecimal> values = context.valuesByMetricCode().get(metricCode);
        return values == null ? emptyMonthNumbers() : new ArrayList<>(values);
    }

    private List<BigDecimal> fixedMonthlyValues(BigDecimal value) {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 12; i += 1) {
            values.add(value);
        }
        return values;
    }

    private List<BigDecimal> emptyMonthNumbers() {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 12; i += 1) {
            values.add(null);
        }
        return values;
    }

    private List<String> emptyMonthTexts() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 12; i += 1) {
            values.add("-");
        }
        return values;
    }

    private List<String> monthLabels() {
        List<String> months = new ArrayList<>();
        for (int i = 1; i <= 12; i += 1) {
            months.add(i < 10 ? "0" + i : String.valueOf(i));
        }
        return months;
    }

    private List<BigDecimal> dividePercent(List<BigDecimal> numerator, List<BigDecimal> denominator) {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 12; i += 1) {
            BigDecimal base = denominator.get(i);
            BigDecimal top = numerator.get(i);
            if (base == null || base.compareTo(BigDecimal.ZERO) == 0 || top == null) {
                values.add(null);
            } else {
                values.add(top.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP));
            }
        }
        return values;
    }

    private List<BigDecimal> subtract(List<BigDecimal> left, List<BigDecimal> right) {
        List<BigDecimal> values = new ArrayList<>();
        for (int i = 0; i < 12; i += 1) {
            BigDecimal leftValue = left.get(i);
            BigDecimal rightValue = right.get(i);
            if (leftValue == null && rightValue == null) {
                values.add(null);
            } else {
                values.add(valueOrZero(leftValue).subtract(valueOrZero(rightValue)));
            }
        }
        return values;
    }

    private String sumSummary(List<BigDecimal> values, boolean percent) {
        BigDecimal sum = values.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        return formatTrendCell(sum, percent);
    }

    private String ratioSummary(List<BigDecimal> numerator, List<BigDecimal> denominator) {
        BigDecimal top = numerator.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal base = denominator.stream().filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (base.compareTo(BigDecimal.ZERO) == 0) {
            return "-";
        }
        return formatTrendCell(top.multiply(BigDecimal.valueOf(100)).divide(base, 2, RoundingMode.HALF_UP), true);
    }

    private String averageSummary(List<BigDecimal> values, boolean percent) {
        List<BigDecimal> filledValues = values.stream().filter(Objects::nonNull).toList();
        if (CollUtil.isEmpty(filledValues)) {
            return "-";
        }
        BigDecimal sum = filledValues.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return formatTrendCell(sum.divide(BigDecimal.valueOf(filledValues.size()), 2, RoundingMode.HALF_UP), percent);
    }

    private String formatTrendCell(BigDecimal value, boolean percent) {
        if (value == null) {
            return "-";
        }
        BigDecimal normalized = value.setScale(percent ? 1 : 2, RoundingMode.HALF_UP).stripTrailingZeros();
        return normalized.toPlainString() + (percent ? "%" : "");
    }

    private void createEvaluationItems(SrmPerformanceQuarterEvalDO evaluation, TemplateSnapshot template) {
        SrmPerformanceSupplierConfigDO config = supplierConfigMapper
                .selectBySupplierAndTemplateVersion(evaluation.getSupplierId(), template.version().getId());
        Map<Long, SrmPerformanceSupplierConfigItemDO> configItemMap = Map.of();
        if (config != null && "ENABLED".equals(config.getStatus())) {
            configItemMap = supplierConfigItemMapper
                    .selectListByConfigAndTemplateVersion(config.getId(), template.version().getId()).stream()
                    .collect(Collectors.toMap(SrmPerformanceSupplierConfigItemDO::getTemplateItemId,
                            Function.identity(), (left, right) -> right));
        }
        for (SrmEvaluationTemplateItemDO templateItem : template.items()) {
            SrmPerformanceSupplierConfigItemDO configItem = configItemMap.get(templateItem.getId());
            SrmPerformanceQuarterItemDO item = new SrmPerformanceQuarterItemDO();
            copyTemplateItemSnapshot(templateItem, item);
            if (configItem == null) {
                applyTemplateDefaultPeople(templateItem, item);
                item.setIndicatorTypeSnapshot(defaultIndicatorType(templateItem));
            } else {
                applySupplierConfigItem(configItem, item);
            }
            if (isCalculatedItem(item)) {
                SrmPerformanceMetricCalcRuleDO rule = configItem == null ? null
                        : calcRuleMapper.selectByConfigItemId(configItem.getId());
                item.setCalcRuleId(rule == null ? null : rule.getId());
                item.setDataStatus(DATA_PENDING);
                item.setScoreStatus(SCORE_PENDING);
            } else {
                item.setDataStatus(DATA_READY);
                item.setScoreStatus(SCORE_PENDING);
            }
            item.setEvaluationId(evaluation.getId());
            itemMapper.insert(item);
        }
    }

    private CalcResult calculateItem(SrmPerformanceQuarterEvalDO evaluation, SrmPerformanceQuarterItemDO item,
                                     ActualContext actualContext, List<String> missingMessages) {
        traceMapper.deleteByEvaluationItemId(item.getId());
        SrmPerformanceMetricCalcRuleDO rule = item.getCalcRuleId() == null ? null : calcRuleMapper.selectById(item.getCalcRuleId());
        List<SrmPerformanceMetricCalcNodeDO> nodes = rule == null ? List.of() : calcNodeMapper.selectListByRuleId(rule.getId());
        if (CollUtil.isEmpty(nodes)) {
            nodes = List.of(defaultSourceNode(item));
        }
        Map<String, BigDecimal> variables = new HashMap<>();
        List<String> itemMissingMessages = new ArrayList<>();
        for (SrmPerformanceMetricCalcNodeDO node : nodes) {
            if (!"SOURCE_METRIC".equals(node.getNodeType())) {
                continue;
            }
            String metricCode = StrUtil.blankToDefault(node.getSourceMetricCode(), item.getIndicatorCodeSnapshot());
            List<ActualValueSnapshot> values = actualContext.valuesByMetricCode()
                    .getOrDefault(metricCode, List.of());
            if (CollUtil.isEmpty(values)) {
                if (!"ZERO".equals(rule == null ? null : rule.getMissingPolicy())
                        && (node.getRequiredFlag() == null || Boolean.TRUE.equals(node.getRequiredFlag()))) {
                    itemMissingMessages.add(item.getIndicatorNameSnapshot() + "/" + metricCode);
                }
                variables.put(node.getNodeKey(), BigDecimal.ZERO);
                writeMissingTrace(evaluation, item, rule, node, metricCode);
                continue;
            }
            BigDecimal aggregateValue = aggregate(values, node.getAggregateMethod());
            variables.put(node.getNodeKey(), aggregateValue);
            for (ActualValueSnapshot value : values) {
                writeSourceTrace(evaluation, item, rule, node, value);
            }
            writeAggregateTrace(evaluation, item, rule, node, aggregateValue);
        }
        if (CollUtil.isNotEmpty(itemMissingMessages)) {
            missingMessages.addAll(itemMissingMessages);
            return new CalcResult(false, null, null);
        }
        try {
            BigDecimal actualValue = evaluateFormula(rule == null ? null : rule.getFormulaExpr(), variables,
                    firstVariableValue(variables));
            variables.put("result", actualValue);
            variables.put("maxScore", valueOrZero(item.getMaxScoreSnapshot()));
            variables.put("targetValue", valueOrZero(item.getTargetValueSnapshot()));
            BigDecimal score = evaluateScore(rule == null ? null : rule.getScoreFormulaExpr(), variables,
                    actualValue, item);
            writeResultTrace(evaluation, item, rule, actualValue, score);
            return new CalcResult(true, actualValue, score);
        } catch (RuntimeException ex) {
            throw exception(SRM_PERFORMANCE_QUARTER_FORMULA_INVALID, item.getIndicatorNameSnapshot() + ":" + ex.getMessage());
        }
    }

    private ActualContext loadActualContext(SrmPerformanceQuarterEvalDO evaluation) {
        List<SrmPerformanceActualReportDO> reports = actualReportMapper
                .selectConfirmedByQuarter(evaluation.getSupplierId(), evaluation.getEvalYear(), evaluation.getEvalQuarter());
        if (CollUtil.isEmpty(reports)) {
            return new ActualContext(Map.of(), Map.of());
        }
        Map<Long, SrmPerformanceActualReportDO> reportMap = reports.stream()
                .collect(Collectors.toMap(SrmPerformanceActualReportDO::getId, Function.identity()));
        List<SrmPerformanceActualValueDO> values = actualValueMapper.selectListByReportIds(new ArrayList<>(reportMap.keySet()));
        Map<String, List<ActualValueSnapshot>> valuesByMetricCode = new HashMap<>();
        for (SrmPerformanceActualValueDO value : values) {
            SrmPerformanceActualReportDO report = reportMap.get(value.getReportId());
            if (report == null || value.getNumericValue() == null) {
                continue;
            }
            ActualValueSnapshot snapshot = new ActualValueSnapshot(report, value,
                    attachmentMapper.selectByBiz(ACTUAL_VALUE_ATTACHMENT_BIZ_TYPE, value.getId(), false));
            valuesByMetricCode.computeIfAbsent(value.getMetricCode(), key -> new ArrayList<>()).add(snapshot);
        }
        return new ActualContext(reportMap, valuesByMetricCode);
    }

    private void copyTemplateItemSnapshot(SrmEvaluationTemplateItemDO templateItem, SrmPerformanceQuarterItemDO item) {
        item.setTemplateItemId(templateItem.getId());
        item.setGroupCodeSnapshot(templateItem.getGroupCode());
        item.setGroupNameSnapshot(templateItem.getGroupName());
        item.setGroupSort(templateItem.getGroupSort());
        item.setGroupMaxScoreSnapshot(templateItem.getGroupMaxScore());
        item.setVetoOperatorSnapshot(templateItem.getVetoOperator());
        item.setVetoScoreSnapshot(templateItem.getVetoScore());
        item.setVetoResultSnapshot(templateItem.getVetoResult());
        item.setIndicatorCodeSnapshot(templateItem.getIndicatorCode());
        item.setIndicatorNameSnapshot(templateItem.getIndicatorName());
        item.setIndicatorSort(templateItem.getIndicatorSort());
        item.setScoringRuleSnapshot(templateItem.getScoringRule());
        item.setMaxScoreSnapshot(templateItem.getMaxScore());
        item.setDefaultDeptNamesSnapshot(templateItem.getDefaultDeptNames());
        item.setAttachmentRequiredSnapshot(templateItem.getAttachmentRequired());
    }

    private void applyTemplateDefaultPeople(SrmEvaluationTemplateItemDO templateItem, SrmPerformanceQuarterItemDO item) {
        item.setScorerCandidateUserIds(templateItem.getDefaultScorerUserIds());
        item.setScorerCandidateUserNames(templateItem.getDefaultScorerUserNames());
        item.setScorerUserId(templateItem.getDefaultScorerUserId());
        item.setScorerUserName(templateItem.getDefaultScorerUserName());
    }

    private void applySupplierConfigItem(SrmPerformanceSupplierConfigItemDO configItem, SrmPerformanceQuarterItemDO item) {
        item.setIndicatorTypeSnapshot(StrUtil.blankToDefault(configItem.getIndicatorType(), INDICATOR_MANUAL));
        item.setTargetValueSnapshot(configItem.getTargetValue());
        item.setTargetUnitSnapshot(configItem.getTargetUnit());
        item.setRedlineScoreSnapshot(configItem.getRedlineScore());
        item.setDefaultDeptNamesSnapshot(StrUtil.blankToDefault(configItem.getDefaultDeptNames(),
                item.getDefaultDeptNamesSnapshot()));
        item.setScorerCandidateUserIds(configItem.getScorerCandidateUserIds());
        item.setScorerCandidateUserNames(configItem.getScorerCandidateUserNames());
        item.setScorerUserId(configItem.getScorerUserId());
        item.setScorerUserName(configItem.getScorerUserName());
        item.setReporterCandidateUserIds(configItem.getReporterCandidateUserIds());
        item.setReporterCandidateUserNames(configItem.getReporterCandidateUserNames());
        item.setReporterUserId(configItem.getReporterUserId());
        item.setReporterUserName(configItem.getReporterUserName());
    }

    private SrmPerformanceQuarterEvaluationRespVO buildResp(SrmPerformanceQuarterEvalDO evaluation, boolean includeDetail) {
        SrmPerformanceQuarterEvaluationRespVO resp =
                BeanUtils.toBean(evaluation, SrmPerformanceQuarterEvaluationRespVO.class);
        resp.setTotalScoreDisplay(decimalText(evaluation.getTotalScore()));
        UserSnapshot user = currentUser();
        boolean admin = isEvaluationAdmin(user.id());
        boolean initiator = Objects.equals(user.id(), evaluation.getInitiatorId());
        List<SrmPerformanceQuarterItemDO> items = includeDetail ? itemMapper.selectListByEvaluationId(evaluation.getId()) : List.of();
        List<SrmPerformanceQuarterSignDO> signs = includeDetail ? signMapper.selectListByEvaluationId(evaluation.getId()) : List.of();
        boolean scorer = items.stream().anyMatch(item -> Objects.equals(item.getScorerUserId(), user.id()));
        boolean signer = signs.stream().anyMatch(sign -> Objects.equals(sign.getUserId(), user.id()));
        resp.setViewerScope(admin ? "ADMIN" : initiator ? "INITIATOR" : scorer ? "SCORER" : signer ? "SIGNER" : "VIEWER");
        resp.setCanMaintain((admin || initiator) && !List.of(STATUS_SIGNING, STATUS_ARCHIVED).contains(evaluation.getStatus()));
        resp.setCanPullActuals(Boolean.TRUE.equals(resp.getCanMaintain())
                && List.of(STATUS_DRAFT, STATUS_PENDING_DATA, STATUS_SCORING).contains(evaluation.getStatus()));
        resp.setCanScore(STATUS_SCORING.equals(evaluation.getStatus()) && items.stream()
                .anyMatch(item -> Objects.equals(item.getScorerUserId(), user.id()) && isManualItem(item)
                        && !SCORE_COMPLETED.equals(item.getScoreStatus())));
        resp.setCanCalculate(Boolean.TRUE.equals(resp.getCanMaintain())
                && STATUS_PENDING_CALCULATION.equals(evaluation.getStatus()));
        resp.setCanStartSign(Boolean.TRUE.equals(resp.getCanMaintain()) && STATUS_PENDING_SIGN.equals(evaluation.getStatus()));
        resp.setCanSign(STATUS_SIGNING.equals(evaluation.getStatus()) && signs.stream()
                .anyMatch(sign -> Objects.equals(sign.getUserId(), user.id()) && SIGN_PENDING.equals(sign.getSignStatus())));
        if (includeDetail) {
            resp.setItems(items.stream().map(item -> toItemResp(item, user.id())).toList());
            resp.setTraces(BeanUtils.toBean(traceMapper.selectListByEvaluationId(evaluation.getId()),
                    SrmPerformanceQuarterEvaluationRespVO.Trace.class));
            resp.setSigns(signs.stream().map(sign -> toSignResp(sign, user.id())).toList());
            resp.setLogs(BeanUtils.toBean(logMapper.selectListByEvaluationId(evaluation.getId()),
                    SrmPerformanceQuarterEvaluationRespVO.Log.class));
        }
        return resp;
    }

    private SrmPerformanceQuarterEvaluationRespVO.Item toItemResp(SrmPerformanceQuarterItemDO item, Long userId) {
        SrmPerformanceQuarterEvaluationRespVO.Item resp =
                BeanUtils.toBean(item, SrmPerformanceQuarterEvaluationRespVO.Item.class);
        resp.setScorerUserNameDisplay(StrUtil.blankToDefault(item.getScorerUserName(), item.getScorerCandidateUserNames()));
        resp.setFinalScoreDisplay(decimalText(item.getFinalScore()));
        resp.setCurrentUserItem(Objects.equals(item.getScorerUserId(), userId));
        return resp;
    }

    private SrmPerformanceQuarterEvaluationRespVO.Sign toSignResp(SrmPerformanceQuarterSignDO sign, Long userId) {
        SrmPerformanceQuarterEvaluationRespVO.Sign resp =
                BeanUtils.toBean(sign, SrmPerformanceQuarterEvaluationRespVO.Sign.class);
        resp.setCurrentUserSign(Objects.equals(sign.getUserId(), userId) && SIGN_PENDING.equals(sign.getSignStatus()));
        return resp;
    }

    private TemplateSnapshot validatePublishedQuarterTemplate(Long versionId) {
        SrmEvaluationTemplateVersionDO version = versionId == null ? null : versionMapper.selectById(versionId);
        if (version == null) {
            throw exception(SRM_EVALUATION_TEMPLATE_VERSION_NOT_EXISTS);
        }
        if (!VERSION_STATUS_PUBLISHED.equals(version.getStatus())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        SrmEvaluationTemplateDO template = templateMapper.selectById(version.getTemplateId());
        if (template == null || !TEMPLATE_STATUS_ENABLED.equals(template.getStatus())
                || !SCENE_QUARTER.equals(template.getSceneType())) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        List<SrmEvaluationTemplateItemDO> items = templateItemMapper.selectListByVersionId(versionId);
        if (CollUtil.isEmpty(items)) {
            throw exception(SRM_EVALUATION_TEMPLATE_STATUS_INVALID);
        }
        return new TemplateSnapshot(template, version, items);
    }

    private void applyTemplateSnapshot(SrmPerformanceQuarterEvalDO evaluation, TemplateSnapshot template) {
        evaluation.setTemplateId(template.template().getId());
        evaluation.setTemplateVersionId(template.version().getId());
        evaluation.setTemplateCodeSnapshot(template.template().getTemplateCode());
        evaluation.setTemplateNameSnapshot(template.template().getTemplateName());
        evaluation.setTemplateVersionSnapshot(template.version().getVersionNo());
        evaluation.setTotalScoreBaseline(template.version().getTotalScore());
        evaluation.setQualificationScoreSnapshot(template.version().getQualificationScore());
    }

    private void copyEditableFields(SrmPerformanceQuarterEvaluationSaveReqVO reqVO,
                                    SrmPerformanceQuarterEvalDO evaluation,
                                    String evaluationNo) {
        evaluation.setEvaluationNo(evaluationNo);
        evaluation.setSupplierId(reqVO.getSupplierId());
        evaluation.setSupplierCode(StrUtil.trim(reqVO.getSupplierCode()));
        evaluation.setSupplierName(StrUtil.trim(reqVO.getSupplierName()));
        evaluation.setSupplierSourceType(StrUtil.blankToDefault(reqVO.getSupplierSourceType(), "REGISTERED"));
        evaluation.setEvalYear(reqVO.getEvalYear());
        evaluation.setEvalQuarter(reqVO.getEvalQuarter());
        evaluation.setRemark(StrUtil.trim(reqVO.getRemark()));
    }

    private SrmPerformanceQuarterEvaluationAvailableSupplierRespVO toAvailableSupplierResp(MesSupplierDO supplier) {
        SrmPerformanceQuarterEvaluationAvailableSupplierRespVO resp =
                new SrmPerformanceQuarterEvaluationAvailableSupplierRespVO();
        resp.setSupplierId(supplier.getId());
        resp.setSupplierCode(supplier.getSupplierCode());
        resp.setSupplierName(supplier.getSupplierName());
        resp.setLevel(supplier.getLevel());
        resp.setSupplierSourceType("REGISTERED");
        return resp;
    }

    private String buildAvailableSupplierGroupKey(MesSupplierDO supplier) {
        return String.join("|",
                cleanKeyPart(supplier.getSupplierCode()),
                cleanKeyPart(supplier.getSupplierName()),
                cleanKeyPart(supplier.getLevel()));
    }

    private String buildSupplierBusinessKey(MesSupplierDO supplier) {
        return String.join("|",
                cleanKeyPart(supplier.getSupplierCode()),
                cleanKeyPart(supplier.getSupplierName()));
    }

    private String buildEvaluationSupplierBusinessKey(SrmPerformanceQuarterEvalDO evaluation) {
        return String.join("|",
                cleanKeyPart(evaluation.getSupplierCode()),
                cleanKeyPart(evaluation.getSupplierName()));
    }

    private String cleanKeyPart(String value) {
        return StrUtil.blankToDefault(StrUtil.trim(value), "");
    }

    private BigDecimal evaluateFormula(String formulaExpr, Map<String, BigDecimal> variables, BigDecimal defaultValue) {
        if (StrUtil.isBlank(formulaExpr)) {
            return defaultValue == null ? BigDecimal.ZERO : defaultValue;
        }
        StandardEvaluationContext context = new StandardEvaluationContext();
        variables.forEach(context::setVariable);
        Object value = expressionParser.parseExpression(formulaExpr).getValue(context);
        return toBigDecimal(value);
    }

    private BigDecimal evaluateScore(String scoreFormulaExpr, Map<String, BigDecimal> variables,
                                     BigDecimal actualValue, SrmPerformanceQuarterItemDO item) {
        BigDecimal maxScore = valueOrZero(item.getMaxScoreSnapshot());
        BigDecimal score;
        if (StrUtil.isNotBlank(scoreFormulaExpr)) {
            score = evaluateFormula(scoreFormulaExpr, variables, BigDecimal.ZERO);
        } else if (item.getTargetValueSnapshot() != null && item.getTargetValueSnapshot().compareTo(BigDecimal.ZERO) > 0) {
            score = actualValue.multiply(maxScore).divide(item.getTargetValueSnapshot(), 4, RoundingMode.HALF_UP);
        } else {
            score = actualValue;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (score.compareTo(maxScore) > 0) {
            return maxScore;
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal aggregate(List<ActualValueSnapshot> values, String aggregateMethod) {
        if ("AVG".equals(aggregateMethod)) {
            return values.stream().map(item -> item.value().getNumericValue()).reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
        }
        if ("MAX".equals(aggregateMethod)) {
            return values.stream().map(item -> item.value().getNumericValue()).max(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        }
        if ("MIN".equals(aggregateMethod)) {
            return values.stream().map(item -> item.value().getNumericValue()).min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);
        }
        return values.stream().map(item -> item.value().getNumericValue()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void writeSourceTrace(SrmPerformanceQuarterEvalDO evaluation, SrmPerformanceQuarterItemDO item,
                                  SrmPerformanceMetricCalcRuleDO rule, SrmPerformanceMetricCalcNodeDO node,
                                  ActualValueSnapshot value) {
        SrmPerformanceCalcTraceDO trace = new SrmPerformanceCalcTraceDO();
        fillTraceBase(trace, evaluation, item, rule, node);
        trace.setSourceReportId(value.report().getId());
        trace.setSourceValueId(value.value().getId());
        trace.setSourceMetricCode(value.value().getMetricCode());
        trace.setSourceMetricName(value.value().getMetricName());
        trace.setPeriodYear(value.report().getEvalYear());
        trace.setPeriodQuarter(value.report().getEvalQuarter());
        trace.setPeriodMonth(value.report().getEvalMonth());
        trace.setRawValue(value.value().getNumericValue());
        trace.setNormalizedValue(value.value().getNumericValue());
        trace.setUnit(value.value().getUnit());
        trace.setAttachmentCount(value.attachments().size());
        trace.setReporterUserId(value.report().getReporterUserId());
        trace.setReporterUserName(value.report().getReporterUserName());
        trace.setReportTime(value.report().getConfirmTime());
        trace.setResultFlag(DATA_READY);
        traceMapper.insert(trace);
    }

    private void writeAggregateTrace(SrmPerformanceQuarterEvalDO evaluation, SrmPerformanceQuarterItemDO item,
                                     SrmPerformanceMetricCalcRuleDO rule, SrmPerformanceMetricCalcNodeDO node,
                                     BigDecimal aggregateValue) {
        SrmPerformanceCalcTraceDO trace = new SrmPerformanceCalcTraceDO();
        fillTraceBase(trace, evaluation, item, rule, node);
        trace.setNodeType("AGGREGATE");
        trace.setRawValue(aggregateValue);
        trace.setNormalizedValue(aggregateValue);
        trace.setUnit(node.getUnit());
        trace.setResultFlag(DATA_READY);
        trace.setResultMessage("来源指标汇总");
        traceMapper.insert(trace);
    }

    private void writeResultTrace(SrmPerformanceQuarterEvalDO evaluation, SrmPerformanceQuarterItemDO item,
                                  SrmPerformanceMetricCalcRuleDO rule, BigDecimal actualValue, BigDecimal score) {
        SrmPerformanceCalcTraceDO trace = new SrmPerformanceCalcTraceDO();
        trace.setEvaluationId(evaluation.getId());
        trace.setEvaluationItemId(item.getId());
        trace.setCalcRuleId(rule == null ? null : rule.getId());
        trace.setNodeKey("result");
        trace.setNodeType("RESULT");
        trace.setDisplayName(item.getIndicatorNameSnapshot());
        trace.setFormulaExpr(rule == null ? null : rule.getFormulaExpr());
        trace.setPeriodYear(evaluation.getEvalYear());
        trace.setPeriodQuarter(evaluation.getEvalQuarter());
        trace.setRawValue(actualValue);
        trace.setNormalizedValue(score);
        trace.setUnit(item.getTargetUnitSnapshot());
        trace.setResultFlag(DATA_READY);
        trace.setResultMessage("计算结果/得分");
        traceMapper.insert(trace);
    }

    private void writeMissingTrace(SrmPerformanceQuarterEvalDO evaluation, SrmPerformanceQuarterItemDO item,
                                   SrmPerformanceMetricCalcRuleDO rule, SrmPerformanceMetricCalcNodeDO node,
                                   String metricCode) {
        SrmPerformanceCalcTraceDO trace = new SrmPerformanceCalcTraceDO();
        fillTraceBase(trace, evaluation, item, rule, node);
        trace.setSourceMetricCode(metricCode);
        trace.setPeriodYear(evaluation.getEvalYear());
        trace.setPeriodQuarter(evaluation.getEvalQuarter());
        trace.setResultFlag(DATA_PENDING);
        trace.setResultMessage("未找到已确认实际值");
        traceMapper.insert(trace);
    }

    private void fillTraceBase(SrmPerformanceCalcTraceDO trace, SrmPerformanceQuarterEvalDO evaluation,
                               SrmPerformanceQuarterItemDO item, SrmPerformanceMetricCalcRuleDO rule,
                               SrmPerformanceMetricCalcNodeDO node) {
        trace.setEvaluationId(evaluation.getId());
        trace.setEvaluationItemId(item.getId());
        trace.setCalcRuleId(rule == null ? null : rule.getId());
        trace.setNodeKey(node.getNodeKey());
        trace.setParentNodeKey(node.getParentNodeId() == null ? null : String.valueOf(node.getParentNodeId()));
        trace.setNodeType(node.getNodeType());
        trace.setDisplayName(node.getNodeName());
        trace.setFormulaExpr(rule == null ? null : rule.getFormulaExpr());
    }

    private SrmPerformanceMetricCalcNodeDO defaultSourceNode(SrmPerformanceQuarterItemDO item) {
        SrmPerformanceMetricCalcNodeDO node = new SrmPerformanceMetricCalcNodeDO();
        node.setNodeKey(item.getIndicatorCodeSnapshot());
        node.setNodeName(item.getIndicatorNameSnapshot());
        node.setNodeType("SOURCE_METRIC");
        node.setSourceMetricCode(item.getIndicatorCodeSnapshot());
        node.setSourceMetricName(item.getIndicatorNameSnapshot());
        node.setAggregateMethod("SUM");
        node.setUnit(item.getTargetUnitSnapshot());
        node.setRequiredFlag(Boolean.TRUE);
        return node;
    }

    private boolean isCalculatedItem(SrmPerformanceQuarterItemDO item) {
        return INDICATOR_CALCULATED.equals(item.getIndicatorTypeSnapshot())
                || INDICATOR_MIXED.equals(item.getIndicatorTypeSnapshot());
    }

    private boolean isManualItem(SrmPerformanceQuarterItemDO item) {
        return INDICATOR_MANUAL.equals(item.getIndicatorTypeSnapshot())
                || INDICATOR_MIXED.equals(item.getIndicatorTypeSnapshot());
    }

    private boolean hasManualPending(List<SrmPerformanceQuarterItemDO> items) {
        return items.stream().anyMatch(item -> isManualItem(item) && !SCORE_COMPLETED.equals(item.getScoreStatus()));
    }

    private String defaultIndicatorType(SrmEvaluationTemplateItemDO templateItem) {
        String text = StrUtil.blankToDefault(templateItem.getScoringRule(), "") + " "
                + StrUtil.blankToDefault(templateItem.getIndicatorName(), "");
        if (containsAny(text, "计算", "实际值", "合格率", "达成率", "回复率", "次数")) {
            return INDICATOR_CALCULATED;
        }
        return INDICATOR_MANUAL;
    }

    private boolean containsAny(String text, String... keywords) {
        if (StrUtil.isBlank(text)) {
            return false;
        }
        for (String keyword : keywords) {
            if (StrUtil.isNotBlank(keyword) && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private SrmPerformanceQuarterEvalDO validateEvaluation(Long id) {
        SrmPerformanceQuarterEvalDO evaluation = id == null ? null : evaluationMapper.selectById(id);
        if (evaluation == null) {
            throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_NOT_EXISTS);
        }
        return evaluation;
    }

    private void assertMaintainer(SrmPerformanceQuarterEvalDO evaluation) {
        Long userId = currentUser().id();
        if (!Objects.equals(evaluation.getInitiatorId(), userId) && !isEvaluationAdmin(userId)) {
            throw exception(SRM_PERFORMANCE_QUARTER_OPERATOR_ONLY);
        }
    }

    private void assertStatus(SrmPerformanceQuarterEvalDO evaluation, String expected) {
        if (!expected.equals(evaluation.getStatus())) {
            throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_STATUS_INVALID);
        }
    }

    private void assertAnyStatus(SrmPerformanceQuarterEvalDO evaluation, String... expectedStatuses) {
        for (String expected : expectedStatuses) {
            if (expected.equals(evaluation.getStatus())) {
                return;
            }
        }
        throw exception(SRM_PERFORMANCE_QUARTER_EVALUATION_STATUS_INVALID);
    }

    private boolean isEvaluationAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        if (permissionApi.hasAnyRoles(userId, SUPER_ADMIN_ROLE, EVALUATION_ADMIN_ROLE)) {
            return true;
        }
        AdminUserRespDTO user = adminUserApi.getUser(userId);
        return user != null && (HC_ADMIN_USERNAME.equalsIgnoreCase(String.valueOf(user.getUsername()))
                || HC_ADMIN_NICKNAME.equals(user.getNickname()));
    }

    private UserSnapshot currentUser() {
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String userName = SecurityFrameworkUtils.getLoginUserNickname();
        if (userId != null && StrUtil.isBlank(userName)) {
            AdminUserRespDTO user = adminUserApi.getUser(userId);
            userName = user == null ? null : user.getNickname();
        }
        return new UserSnapshot(userId, StrUtil.blankToDefault(userName, "系统用户"));
    }

    private void writeLog(Long evaluationId, String action, String fromStatus, String toStatus,
                          String description, Object detail) {
        UserSnapshot user = currentUser();
        SrmPerformanceQuarterLogDO log = new SrmPerformanceQuarterLogDO();
        log.setEvaluationId(evaluationId);
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setActionDescription(description);
        log.setOperatorId(user.id());
        log.setOperatorName(user.name());
        log.setDetailJson(detail == null ? null : JsonUtils.toJsonString(detail));
        logMapper.insert(log);
    }

    private String resolveGrade(BigDecimal totalScore) {
        if (totalScore == null) {
            return null;
        }
        if (totalScore.compareTo(BigDecimal.valueOf(90)) >= 0) {
            return "A";
        }
        if (totalScore.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "B";
        }
        if (totalScore.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "C";
        }
        return "D";
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal firstVariableValue(Map<String, BigDecimal> variables) {
        return variables.values().stream().findFirst().orElse(BigDecimal.ZERO);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String decimalText(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String buildEvaluationNo() {
        return "SPQ-" + DateUtil.format(DateUtil.date(), "yyyyMMddHHmmssSSS") + "-"
                + IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
    }

    private String resolveDeptName(AdminUserRespDTO user, Map<Long, DeptRespDTO> deptMap) {
        if (user == null || user.getDeptId() == null) {
            return null;
        }
        DeptRespDTO dept = deptMap.get(user.getDeptId());
        return dept == null ? null : dept.getName();
    }

    private String userName(AdminUserRespDTO user) {
        return user == null ? null : StrUtil.blankToDefault(user.getNickname(), user.getUsername());
    }

    private record UserSnapshot(Long id, String name) {
    }

    private record TemplateSnapshot(SrmEvaluationTemplateDO template,
                                    SrmEvaluationTemplateVersionDO version,
                                    List<SrmEvaluationTemplateItemDO> items) {
    }

    private record TrendSource(Long supplierId, String supplierCode, String supplierName, List<TrendItem> items) {
    }

    private record TrendItem(String indicatorCode, String indicatorName, String groupCode, String groupName,
                             Integer groupSort, Integer indicatorSort, String scoringRule, BigDecimal maxScore,
                             BigDecimal targetValue, String targetUnit, String indicatorType, Long calcRuleId) {
    }

    private record MonthValueContext(Map<String, List<BigDecimal>> valuesByMetricCode,
                                     Map<Integer, SrmPerformanceQuarterEvalDO> evaluationsByQuarter) {
    }

    private record ActualContext(Map<Long, SrmPerformanceActualReportDO> reportMap,
                                 Map<String, List<ActualValueSnapshot>> valuesByMetricCode) {
    }

    private record ActualValueSnapshot(SrmPerformanceActualReportDO report,
                                       SrmPerformanceActualValueDO value,
                                       List<SrmAttachmentDO> attachments) {
    }

    private record CalcResult(boolean ready, BigDecimal actualValue, BigDecimal score) {
    }

}
