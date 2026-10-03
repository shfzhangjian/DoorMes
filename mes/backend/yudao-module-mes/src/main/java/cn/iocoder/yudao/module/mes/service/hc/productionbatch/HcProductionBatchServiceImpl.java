package cn.iocoder.yudao.module.mes.service.hc.productionbatch;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleParseReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance.HcLotInstanceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance.HcLotInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineContext;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineResolverService;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleMatchContext;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleService;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
public class HcProductionBatchServiceImpl implements HcProductionBatchService {

    private static final String STATUS_GENERATED = "GENERATED";
    private static final String BATCH_STATUS_GENERATED = "GENERATED";
    private static final String STAGE_ROOT = "ROOT";
    private static final String STAGE_OPERATION = "OPERATION";
    private static final String TRIGGER_FORMULA_START = "FORMULA_START";
    private static final String SCOPE_PLAN_ROOT = "PLAN_ROOT";
    private static final String SOURCE_FORMULA_REPORT = "FORMULA_REPORT";
    private static final String SOURCE_TABLE_PLAN_OPERATION = "mes_pp_plan_operation";

    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;

    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;

    @Resource
    private HcLotRuleMapper hcLotRuleMapper;

    @Resource
    private HcLotInstanceMapper hcLotInstanceMapper;

    @Resource
    private HcProductionLineResolverService hcProductionLineResolverService;

    @Resource
    private HcLotRuleService hcLotRuleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserveRootBatchOnPlanRelease(Long planId) {
        HcPlanOrderDO plan = hcPlanOrderMapper.selectByIdForUpdate(planId);
        if (plan == null) {
            throw invalidParamException("生产计划不存在，无法确定母批批号");
        }
        if (!"RELEASED".equalsIgnoreCase(plan.getPlanStatus())
                || StrUtil.isNotBlank(plan.getProductionBatchNo())
                || HcRootBatchReservation.isReserved(plan)
                || StrUtil.isNotBlank(plan.getParentProductionBatchNo())
                || StrUtil.isNotBlank(plan.getInventorySourceBatchNos())) {
            return;
        }
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectListByPlanId(planId).stream()
                .min(java.util.Comparator.comparing(HcPlanOrderOperationDO::getOpSeq,
                                java.util.Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(HcPlanOrderOperationDO::getSort,
                                java.util.Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(HcPlanOrderOperationDO::getId))
                .orElse(null);
        if (operation == null || !("FORMULA".equalsIgnoreCase(StrUtil.trim(operation.getOpCode()))
                || "配料".equals(StrUtil.trim(operation.getOpName())))) {
            return;
        }
        LocalDate bizDate = plan.getProductionStartDate();
        if (bizDate == null) {
            throw invalidParamException("确认母批批号前请填写计划开始日期");
        }
        HcLotRuleDO rule = resolveRequiredRootRule(plan, new HcProductionBatchRequest());
        RootBatchParts parts = null;
        String batchNo = StrUtil.trimToNull(plan.getBatchNo());
        if (batchNo == null) {
            // 人工选号不倒推流水；自动取号跳过实际占用，避免每次事务回滚都卡在同一号。
            for (int attempt = 0; attempt < 1000; attempt++) {
                parts = buildRootBatch(rule, plan, operation, bizDate);
                batchNo = parts.lotNo();
                try {
                    validateRootBatchAvailable(planId, batchNo);
                    break;
                } catch (cn.iocoder.yudao.framework.common.exception.ServiceException conflict) {
                    batchNo = null;
                }
            }
            if (batchNo == null) throw invalidParamException("连续生成的母批号均被占用，请检查批号流水设置");
        } else {
            // 人工指定值冲突时直接报错，不悄悄改成另一个批号。
            validateRootBatchAvailable(planId, batchNo);
        }
        Map<String, Object> context = new LinkedHashMap<>(HcRootBatchReservation.context(plan));
        context.put("rootBatchReserved", true);
        context.put("replanAfterWithdrawal", false);
        context.put("reservationDate", bizDate.toString());
        context.put("reservedBatchNo", batchNo);
        context.put("reservationAttributes", buildRootAttributes(plan, operation, parts, rule, null));
        HcPlanOrderDO update = new HcPlanOrderDO();
        update.setId(planId);
        update.setBatchNo(batchNo);
        update.setBatchRuleId(rule.getId());
        update.setBatchRuleCode(rule.getRuleCode());
        update.setBatchRuleVersion(rule.getVersionNo());
        update.setProductionBatchContextJson(JsonUtils.toJsonString(context));
        hcPlanOrderMapper.updateById(update);
    }

    private void validateRootBatchAvailable(Long planId, String batchNo) {
        // 当前读：避免同一事务中的预览快照漏掉其他事务刚提交的预约。
        HcPlanOrderDO other = hcPlanOrderMapper.selectOne(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HcPlanOrderDO>()
                        .ne(HcPlanOrderDO::getId, planId)
                        .eq(HcPlanOrderDO::getBatchNo, batchNo)
                        .last("LIMIT 1 FOR UPDATE"));
        if (other != null) {
            throw invalidParamException("母批批号已被其他计划占用：" + batchNo);
        }
        Long otherGenerated = hcPlanOrderMapper.selectCount(
                new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HcPlanOrderDO>()
                        .ne(HcPlanOrderDO::getId, planId)
                        .eq(HcPlanOrderDO::getProductionBatchNo, batchNo));
        if (otherGenerated != null && otherGenerated > 0) {
            throw invalidParamException("母批批号已被其他计划使用：" + batchNo);
        }
        HcLotInstanceDO instance = hcLotInstanceMapper.selectByLotNo(batchNo);
        if (instance != null && !Objects.equals(instance.getPlanId(), planId)) {
            throw invalidParamException("母批批号已存在其他计划的批次实例：" + batchNo);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcProductionBatchResult generateRootBatchOnFormulaStart(HcProductionBatchRequest request) {
        if (request == null || request.getPlanId() == null || request.getPlanOperationId() == null) {
            throw invalidParamException("配方开工生成生产批号时，计划和计划工序不能为空");
        }
        HcPlanOrderDO plan = hcPlanOrderMapper.selectByIdForUpdate(request.getPlanId());
        if (plan == null) {
            throw invalidParamException("生产计划不存在，无法生成生产批号");
        }
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(request.getPlanOperationId());
        if (operation == null) {
            throw invalidParamException("计划工序不存在，无法生成生产批号");
        }

        LocalDateTime generatedTime = request.getGeneratedTime() == null ? LocalDateTime.now() : request.getGeneratedTime();
        LocalDate bizDate = resolveRootBatchBizDate(plan);
        HcProductionBatchRequest recordRequest = copyRequest(request);
        recordRequest.setGeneratedTime(generatedTime);
        recordRequest.setBizDate(bizDate);
        recordRequest.setGenerationTrigger(firstNotBlank(request.getGenerationTrigger(), TRIGGER_FORMULA_START));
        recordRequest.setGenerationScope(firstNotBlank(request.getGenerationScope(), SCOPE_PLAN_ROOT));
        recordRequest.setGenerateSource(firstNotBlank(request.getGenerateSource(), SOURCE_FORMULA_REPORT));
        recordRequest.setBatchStage(firstNotBlank(request.getBatchStage(), STAGE_ROOT));
        recordRequest.setSourceTable(firstNotBlank(request.getSourceTable(), SOURCE_TABLE_PLAN_OPERATION));
        recordRequest.setSourceId(request.getSourceId() == null ? operation.getId() : request.getSourceId());
        recordRequest.setIdempotentKey(firstNotBlank(request.getIdempotentKey(), buildRootIdempotentKey(plan.getId())));
        recordRequest.setUpdatePlanSnapshot(true);

        String existingBatchNo = firstNotBlank(plan.getProductionBatchNo(), plan.getBatchNo());
        HcLotRuleDO rule = resolveRequiredRootRule(plan, request);
        RootBatchParts parts = null;
        if (StrUtil.isNotBlank(existingBatchNo)) {
            existingBatchNo = existingBatchNo.trim();
            recordRequest.setProductionBatchNo(existingBatchNo);
            recordRequest.setParentProductionBatchNo(null);
            recordRequest.setRuleId(rule.getId());
            recordRequest.setRuleCode(rule.getRuleCode());
            recordRequest.setSampleCode(request.getSampleCode());
        } else {
            parts = buildRootBatch(rule, plan, operation, bizDate);
            recordRequest.setProductionBatchNo(parts.lotNo());
            recordRequest.setParentProductionBatchNo(null);
            recordRequest.setRuleId(rule.getId());
            recordRequest.setRuleCode(rule.getRuleCode());
            recordRequest.setSampleCode(request.getSampleCode());
        }
        validateRootBatchAvailable(plan.getId(), recordRequest.getProductionBatchNo());
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (HcRootBatchReservation.isReserved(plan)) {
            Object reserved = HcRootBatchReservation.context(plan).get("reservationAttributes");
            if (reserved instanceof Map<?, ?> values) {
                values.forEach((key, value) -> attributes.put(String.valueOf(key), value));
            }
        }
        if (recordRequest.getAttributes() != null) {
            attributes.putAll(recordRequest.getAttributes());
        }
        recordRequest.setAttributes(buildRootAttributes(plan, operation, parts, rule, attributes));
        return recordKnownBatch(recordRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcProductionBatchResult recordKnownBatch(HcProductionBatchRequest request) {
        validateRecordRequest(request);
        HcPlanOrderDO plan = hcPlanOrderMapper.selectById(request.getPlanId());
        if (plan == null) {
            throw invalidParamException("生产计划不存在，无法记录生产批号");
        }
        HcPlanOrderOperationDO operation = request.getPlanOperationId() == null
                ? null
                : hcPlanOrderOperationMapper.selectById(request.getPlanOperationId());
        if (request.getPlanOperationId() != null && operation == null) {
            throw invalidParamException("计划工序不存在，无法记录生产批号");
        }

        String idempotentKey = firstNotBlank(request.getIdempotentKey(), buildIdempotentKey(request));
        HcLotInstanceDO existed = hcLotInstanceMapper.selectByIdempotentKey(idempotentKey);
        if (existed != null) {
            return HcProductionBatchResult.builder()
                    .instanceId(existed.getId())
                    .productionBatchNo(existed.getProductionBatchNo())
                    .parentProductionBatchNo(existed.getParentProductionBatchNo())
                    .ruleId(existed.getRuleId())
                    .ruleCode(existed.getRuleCode())
                    .sampleCode(existed.getSampleCode())
                    .contextJson(existed.getContextJson())
                    .idempotentKey(idempotentKey)
                    .newlyCreated(false)
                    .build();
        }

        HcLotRuleDO rule = resolveRule(plan, request);
        HcProductionLineContext lineContext = resolveLineContext(operation, plan);
        LocalDateTime generatedTime = request.getGeneratedTime() == null ? LocalDateTime.now() : request.getGeneratedTime();
        LocalDate bizDate = request.getBizDate() == null ? generatedTime.toLocalDate() : request.getBizDate();
        List<HcLotRuleSegmentDO> ruleSegments = rule == null
                ? List.of()
                : hcLotRuleService.getHcLotRuleSegmentListByParentId(rule.getId());
        Map<String, String> segmentValues = resolveSegmentValues(rule, ruleSegments, request);
        Map<String, Object> ruleFormatSnapshot = buildRuleFormatSnapshot(rule, ruleSegments);
        Map<String, Object> context = buildContext(plan, operation, request, lineContext);
        String contextJson = JsonUtils.toJsonString(context);
        HcLotInstanceDO existedLot = hcLotInstanceMapper.selectByLotNo(request.getProductionBatchNo());
        if (existedLot != null) {
            if (existedLot.getPlanId() != null && !Objects.equals(existedLot.getPlanId(), plan.getId())) {
                throw invalidParamException("生产批号已被其他计划使用：" + request.getProductionBatchNo());
            }
            if (Boolean.TRUE.equals(request.getUpdatePlanSnapshot())) {
                updatePlanAndOperationSnapshot(plan, operation, request, rule, context);
            }
            return HcProductionBatchResult.builder()
                    .instanceId(existedLot.getId())
                    .productionBatchNo(firstNotBlank(existedLot.getProductionBatchNo(), existedLot.getLotNo()))
                    .parentProductionBatchNo(existedLot.getParentProductionBatchNo())
                    .ruleId(existedLot.getRuleId())
                    .ruleCode(existedLot.getRuleCode())
                    .sampleCode(firstNotBlank(existedLot.getSampleCode(), request.getSampleCode()))
                    .contextJson(firstNotBlank(existedLot.getContextJson(), contextJson))
                    .idempotentKey(idempotentKey)
                    .newlyCreated(false)
                    .build();
        }

        HcLotInstanceDO instance = HcLotInstanceDO.builder()
                .ruleId(rule == null ? request.getRuleId() : rule.getId())
                .ruleCode(rule == null ? request.getRuleCode() : rule.getRuleCode())
                .ruleVersion(rule == null ? null : rule.getVersionNo())
                .bizType(rule == null ? null : rule.getBizType())
                .productCategoryCode(firstNotBlank(rule == null ? null : rule.getProductCategoryCode(), plan.getCategoryCode()))
                .prodType(firstNotBlank(rule == null ? null : rule.getProdType(), plan.getProdType()))
                .modelCode(firstNotBlank(plan.getMotherModelCode(), plan.getModelCode(), plan.getMaterialCode()))
                .lotNo(request.getProductionBatchNo())
                .productionBatchNo(request.getProductionBatchNo())
                .parentProductionBatchNo(request.getParentProductionBatchNo())
                .batchLevel(firstNotBlank(request.getBatchStage(), STAGE_OPERATION))
                .parentLotNo(request.getParentProductionBatchNo())
                .planId(plan.getId())
                .planNo(plan.getPlanNo())
                .planOperationId(operation == null ? null : operation.getId())
                .operationName(operation == null ? null : operation.getOpName())
                .operationCode(operation == null ? null : operation.getOpCode())
                .operationSeq(operation == null ? null : operation.getOpSeq())
                .workCenterId(lineContext.getWorkCenterId())
                .workCenterCode(lineContext.getWorkCenterCode())
                .workCenterName(lineContext.getWorkCenterName())
                .materialId(plan.getMaterialId())
                .materialCode(plan.getMaterialCode())
                .materialName(plan.getMaterialName())
                .lineCode(lineContext.getLineCode())
                .lineName(lineContext.getLineName())
                .lineShortCode(lineContext.getLineShortCode())
                .batchLineCode(firstNotBlank(segmentValues.get("LINE"), lineContext.getBatchLineCode()))
                .sampleCode(request.getSampleCode())
                .yearCode(segmentValues.get("YEAR"))
                .monthCode(segmentValues.get("MONTH"))
                .annualBatchSeq(parseInteger(segmentValues.get("ANNUAL_SEQ")))
                .contextJson(contextJson)
                .instanceStatus(STATUS_GENERATED)
                .generateSource(request.getGenerateSource())
                .batchStage(firstNotBlank(request.getBatchStage(), STAGE_OPERATION))
                .bizDate(bizDate)
                .sourceTable(request.getSourceTable())
                .sourceId(request.getSourceId())
                .sourceDetailKey(request.getSourceDetailKey())
                .generationTrigger(request.getGenerationTrigger())
                .generationScope(request.getGenerationScope())
                .idempotentKey(idempotentKey)
                .operatorId(request.getOperatorId())
                .operatorName(request.getOperatorName())
                .attributeJson(JsonUtils.toJsonString(request.getAttributes()))
                .ruleFormatSnapshotJson(ruleFormatSnapshot.isEmpty() ? null : JsonUtils.toJsonString(ruleFormatSnapshot))
                .segmentValuesJson(segmentValues.isEmpty() ? null : JsonUtils.toJsonString(segmentValues))
                .generatedTime(generatedTime)
                .tenantId(plan.getTenantId())
                .build();
        hcLotInstanceMapper.insert(instance);
        if (Boolean.TRUE.equals(request.getUpdatePlanSnapshot())) {
            updatePlanAndOperationSnapshot(plan, operation, request, rule, context);
        }
        return HcProductionBatchResult.builder()
                .instanceId(instance.getId())
                .productionBatchNo(instance.getProductionBatchNo())
                .parentProductionBatchNo(instance.getParentProductionBatchNo())
                .ruleId(instance.getRuleId())
                .ruleCode(instance.getRuleCode())
                .sampleCode(instance.getSampleCode())
                .contextJson(contextJson)
                .idempotentKey(idempotentKey)
                .newlyCreated(true)
                .build();
    }

    private HcProductionBatchRequest copyRequest(HcProductionBatchRequest source) {
        HcProductionBatchRequest target = new HcProductionBatchRequest();
        target.setPlanId(source.getPlanId());
        target.setPlanOperationId(source.getPlanOperationId());
        target.setProductionBatchNo(source.getProductionBatchNo());
        target.setParentProductionBatchNo(source.getParentProductionBatchNo());
        target.setRuleId(source.getRuleId());
        target.setRuleCode(source.getRuleCode());
        target.setSampleCode(source.getSampleCode());
        target.setGenerationTrigger(source.getGenerationTrigger());
        target.setGenerationScope(source.getGenerationScope());
        target.setGenerateSource(source.getGenerateSource());
        target.setBatchStage(source.getBatchStage());
        target.setBizDate(source.getBizDate());
        target.setGeneratedTime(source.getGeneratedTime());
        target.setSourceTable(source.getSourceTable());
        target.setSourceId(source.getSourceId());
        target.setSourceDetailKey(source.getSourceDetailKey());
        target.setIdempotentKey(source.getIdempotentKey());
        target.setOperatorId(source.getOperatorId());
        target.setOperatorName(source.getOperatorName());
        target.setAttributes(source.getAttributes());
        target.setUpdatePlanSnapshot(source.getUpdatePlanSnapshot());
        return target;
    }

    private void validateRecordRequest(HcProductionBatchRequest request) {
        if (request == null || request.getPlanId() == null) {
            throw invalidParamException("生产计划不能为空");
        }
        if (StrUtil.isBlank(request.getProductionBatchNo())) {
            throw invalidParamException("生产批号不能为空");
        }
    }

    private HcLotRuleDO resolveRule(HcPlanOrderDO plan, HcProductionBatchRequest request) {
        // 已绑定到计划的规则版本具有最高优先级，开工请求不得改写预览时选定的规则。
        Long ruleId = firstNonNull(plan.getProductionBatchRuleId(), plan.getBatchRuleId(), request.getRuleId());
        if (ruleId != null) {
            HcLotRuleDO rule = hcLotRuleMapper.selectById(ruleId);
            if (rule != null) {
                return rule;
            }
        }
        String ruleCode = firstNotBlank(plan.getProductionBatchRuleCode(), plan.getBatchRuleCode(), request.getRuleCode());
        if (StrUtil.isBlank(ruleCode)) {
            return null;
        }
        return hcLotRuleMapper.selectOne(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<HcLotRuleDO>()
                .eq(HcLotRuleDO::getRuleCode, ruleCode)
                .last("LIMIT 1"));
    }

    private HcLotRuleDO resolveRequiredRootRule(HcPlanOrderDO plan, HcProductionBatchRequest request) {
        HcLotRuleDO rule = resolveRule(plan, request);
        if (rule != null) {
            return rule;
        }
        return hcLotRuleService.matchEnabledRule(HcLotRuleMatchContext.builder()
                .bizType("FG_LOT")
                .productCategoryCode(plan.getCategoryCode())
                .prodType(plan.getProdType())
                .generationTrigger(TRIGGER_FORMULA_START)
                .generationScope(SCOPE_PLAN_ROOT)
                .modelCode(firstNotBlank(plan.getMotherModelCode(), plan.getModelCode(), plan.getMaterialCode()))
                .build());
    }

    /**
     * 母批批号的年月必须来自计划开始日期；下发预留后使用已冻结的预约日期。
     * 报工日期、开工时间和服务器当前日期不能参与母批批号生成。
     */
    private LocalDate resolveRootBatchBizDate(HcPlanOrderDO plan) {
        if (HcRootBatchReservation.isReserved(plan)) {
            Object reservationDate = HcRootBatchReservation.context(plan).get("reservationDate");
            if (reservationDate == null || StrUtil.isBlank(String.valueOf(reservationDate))) {
                throw invalidParamException("母批批号预约日期缺失，无法生成生产批号");
            }
            try {
                return LocalDate.parse(String.valueOf(reservationDate));
            } catch (java.time.format.DateTimeParseException ex) {
                throw invalidParamException("母批批号预约日期格式无效：{}", reservationDate);
            }
        }
        LocalDate productionStartDate = plan.getProductionStartDate();
        if (productionStartDate == null) {
            throw invalidParamException("计划开始日期不能为空，无法生成生产批号");
        }
        return productionStartDate;
    }

    private RootBatchParts buildRootBatch(HcLotRuleDO rule, HcPlanOrderDO plan,
                                          HcPlanOrderOperationDO operation, LocalDate bizDate) {
        HcProductionLineContext lineContext = resolveLineContext(operation, plan);
        HcLotRuleGenerateReqVO generateReq = new HcLotRuleGenerateReqVO();
        generateReq.setRuleId(rule.getId());
        generateReq.setBizDate(bizDate);
        generateReq.setConsumeSequence(true);
        Map<String, String> values = new LinkedHashMap<>();
        values.put("batchLineCode", firstNotBlank(lineContext.getBatchLineCode(), "A"));
        values.put("lineCode", firstNotBlank(lineContext.getLineCode(), ""));
        values.put("modelCode", firstNotBlank(plan.getMotherModelCode(), plan.getModelCode(), plan.getMaterialCode()));
        generateReq.setInputValues(values);
        Map<String, Object> generated = hcLotRuleService.generateLotNo(generateReq);
        Map<String, String> segmentValues = toStringMap(generated.get("segmentValues"));
        return new RootBatchParts(
                String.valueOf(generated.get("lotNo")),
                segmentValues.get("PAD_PREFIX"),
                segmentValues.get("YEAR"),
                segmentValues.get("MONTH"),
                firstNotBlank(segmentValues.get("LINE"), values.get("batchLineCode")),
                parseInteger(segmentValues.get("ANNUAL_SEQ")),
                segmentValues);
    }

    private Map<String, Object> buildRootAttributes(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                                    RootBatchParts parts, HcLotRuleDO rule,
                                                    Map<String, Object> originalAttributes) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (originalAttributes != null) {
            attributes.putAll(originalAttributes);
        }
        attributes.put("rootBatch", true);
        attributes.put("planNo", plan.getPlanNo());
        attributes.put("operationCode", operation.getOpCode());
        attributes.put("operationName", operation.getOpName());
        attributes.put("ruleCode", rule == null ? null : rule.getRuleCode());
        attributes.put("ruleVersion", rule == null ? null : rule.getVersionNo());
        if (parts != null) {
            attributes.put("typeCode", parts.typeCode());
            attributes.put("yearCode", parts.yearCode());
            attributes.put("monthCode", parts.monthCode());
            attributes.put("lineCode", parts.lineCode());
            attributes.put("batchGranularity", "MOTHER_ROLL");
            attributes.put("partialBatch", true);
            attributes.put("missingSampleCode", true);
            attributes.put("missingSliceSeq", true);
            attributes.put("missingAdhesiveCode", true);
            attributes.put("annualBatchSeq", parts.annualSeq());
            attributes.put("segmentValues", parts.segmentValues());
        }
        return attributes;
    }

    private Map<String, String> resolveSegmentValues(HcLotRuleDO rule,
                                                     List<HcLotRuleSegmentDO> ruleSegments,
                                                     HcProductionBatchRequest request) {
        Map<String, String> recordedValues = toStringMap(
                request.getAttributes() == null ? null : request.getAttributes().get("segmentValues"));
        if (!recordedValues.isEmpty() || rule == null || ruleSegments.isEmpty()) {
            return recordedValues;
        }
        try {
            HcLotRuleParseReqVO parseReq = new HcLotRuleParseReqVO();
            parseReq.setRuleId(rule.getId());
            parseReq.setLotNo(request.getProductionBatchNo());
            return hcLotRuleService.parseLotNo(parseReq);
        } catch (RuntimeException ignored) {
            // 历史人工批号可能不满足新规则长度；该情况保留批号本身，避免阻断已有业务。
            return Map.of();
        }
    }

    private Map<String, Object> buildRuleFormatSnapshot(HcLotRuleDO rule,
                                                         List<HcLotRuleSegmentDO> ruleSegments) {
        if (rule == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("ruleCode", rule.getRuleCode());
        snapshot.put("ruleName", rule.getRuleName());
        snapshot.put("versionNo", rule.getVersionNo());
        snapshot.put("bizType", rule.getBizType());
        snapshot.put("productCategoryCode", rule.getProductCategoryCode());
        snapshot.put("prodType", rule.getProdType());
        snapshot.put("generationTrigger", rule.getGenerationTrigger());
        snapshot.put("generationScope", rule.getGenerationScope());
        snapshot.put("yearCodeMode", rule.getYearCodeMode());
        snapshot.put("monthCodeMode", rule.getMonthCodeMode());
        snapshot.put("seqLength", rule.getSeqLength());
        snapshot.put("resetCycle", rule.getResetCycle());
        List<Map<String, Object>> segments = new ArrayList<>();
        List<String> formatParts = new ArrayList<>();
        for (HcLotRuleSegmentDO segment : ruleSegments) {
            if (Boolean.FALSE.equals(segment.getEnabled())) {
                continue;
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("code", segment.getSegmentCode());
            item.put("name", segment.getSegmentName());
            item.put("type", segment.getSegmentType());
            item.put("value", segment.getSegmentValue());
            item.put("length", segment.getSegmentLength());
            item.put("sourceField", segment.getSourceField());
            item.put("sort", segment.getSort());
            segments.add(item);
            formatParts.add(describeSegment(rule, segment));
        }
        snapshot.put("segments", segments);
        snapshot.put("formatSummary", String.join(" + ", formatParts));
        return snapshot;
    }

    private String describeSegment(HcLotRuleDO rule, HcLotRuleSegmentDO segment) {
        String type = segment.getSegmentType() == null ? "" : segment.getSegmentType().toUpperCase();
        return switch (type) {
            case "FIXED" -> "固定前缀 " + firstNotBlank(segment.getSegmentValue(), "-");
            case "YEAR_CODE" -> "两位年份";
            case "MONTH_CODE" -> "月码";
            case "SEQ" -> (segment.getSegmentLength() == null ? rule.getSeqLength() : segment.getSegmentLength()) + "位年度流水";
            case "CONTEXT_FIELD" -> "产线码";
            default -> firstNotBlank(segment.getSegmentName(), segment.getSegmentCode(), "规则段");
        };
    }

    private Map<String, String> toStringMap(Object source) {
        if (!(source instanceof Map<?, ?> rawMap)) {
            return new LinkedHashMap<>();
        }
        Map<String, String> result = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> result.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
        return result;
    }

    private Integer parseInteger(String value) {
        if (value == null || !value.matches("^\\d+$")) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private HcProductionLineContext resolveLineContext(HcPlanOrderOperationDO operation, HcPlanOrderDO plan) {
        if (operation != null && operation.getWorkCenterId() != null) {
            return hcProductionLineResolverService.resolveByWorkCenterId(operation.getWorkCenterId());
        }
        return hcProductionLineResolverService.resolveByMotherModelCode(firstNotBlank(plan.getMotherModelCode(), plan.getModelCode()));
    }

    private Map<String, Object> buildContext(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                             HcProductionBatchRequest request, HcProductionLineContext lineContext) {
        Map<String, Object> context = new LinkedHashMap<>(HcRootBatchReservation.context(plan));
        context.put("planId", plan.getId());
        context.put("planNo", plan.getPlanNo());
        context.put("planOperationId", operation == null ? null : operation.getId());
        context.put("operationCode", operation == null ? null : operation.getOpCode());
        context.put("operationName", operation == null ? null : operation.getOpName());
        context.put("productionBatchNo", request.getProductionBatchNo());
        context.put("parentProductionBatchNo", request.getParentProductionBatchNo());
        context.put("lineCode", lineContext.getLineCode());
        context.put("lineName", lineContext.getLineName());
        context.put("lineShortCode", lineContext.getLineShortCode());
        context.put("batchLineCode", lineContext.getBatchLineCode());
        context.put("sampleCode", request.getSampleCode());
        context.put("sourceTable", request.getSourceTable());
        context.put("sourceId", request.getSourceId());
        context.put("sourceDetailKey", request.getSourceDetailKey());
        context.put("attributes", request.getAttributes());
        return context;
    }

    private void updatePlanAndOperationSnapshot(HcPlanOrderDO plan, HcPlanOrderOperationDO operation,
                                                HcProductionBatchRequest request, HcLotRuleDO rule,
                                                Map<String, Object> context) {
        LocalDateTime generatedTime = request.getGeneratedTime() == null ? LocalDateTime.now() : request.getGeneratedTime();
        HcPlanOrderDO planUpdate = new HcPlanOrderDO();
        planUpdate.setId(plan.getId());
        planUpdate.setProductionBatchNo(request.getProductionBatchNo());
        planUpdate.setParentProductionBatchNo(request.getParentProductionBatchNo());
        planUpdate.setProductionBatchRuleId(rule == null ? request.getRuleId() : rule.getId());
        planUpdate.setProductionBatchRuleCode(rule == null ? request.getRuleCode() : rule.getRuleCode());
        planUpdate.setProductionBatchRuleVersion(rule == null ? null : rule.getVersionNo());
        planUpdate.setProductionBatchContextJson(JsonUtils.toJsonString(context));
        planUpdate.setBatchStatus(BATCH_STATUS_GENERATED);
        planUpdate.setBatchGeneratedTime(generatedTime);
        if (plan.getBatchRuleId() == null) {
            planUpdate.setBatchRuleId(rule == null ? request.getRuleId() : rule.getId());
        }
        if (StrUtil.isBlank(plan.getBatchRuleCode())) {
            planUpdate.setBatchRuleCode(rule == null ? request.getRuleCode() : rule.getRuleCode());
        }
        if (plan.getBatchRuleVersion() == null && rule != null) {
            planUpdate.setBatchRuleVersion(rule.getVersionNo());
        }
        if (StrUtil.isBlank(plan.getBatchNo())) {
            planUpdate.setBatchNo(request.getProductionBatchNo());
        }
        hcPlanOrderMapper.updateById(planUpdate);

        if (operation == null) {
            return;
        }
        HcPlanOrderOperationDO operationUpdate = new HcPlanOrderOperationDO();
        operationUpdate.setId(operation.getId());
        operationUpdate.setProductionBatchNo(request.getProductionBatchNo());
        operationUpdate.setParentProductionBatchNo(request.getParentProductionBatchNo());
        operationUpdate.setProductionBatchRuleId(rule == null ? request.getRuleId() : rule.getId());
        operationUpdate.setProductionBatchRuleCode(rule == null ? request.getRuleCode() : rule.getRuleCode());
        operationUpdate.setProductionBatchRuleVersion(rule == null ? null : rule.getVersionNo());
        operationUpdate.setProductionBatchContextJson(JsonUtils.toJsonString(context));
        if (StrUtil.isBlank(operation.getBatchNo())) {
            operationUpdate.setBatchNo(request.getProductionBatchNo());
        }
        if (StrUtil.isBlank(operation.getParentBatchNo())) {
            operationUpdate.setParentBatchNo(request.getParentProductionBatchNo());
        }
        if (StrUtil.isBlank(operation.getSampleCode())) {
            operationUpdate.setSampleCode(request.getSampleCode());
        }
        hcPlanOrderOperationMapper.updateById(operationUpdate);
    }

    private String buildRootIdempotentKey(Long planId) {
        return "PROD_BATCH_ROOT:" + planId;
    }

    private String buildIdempotentKey(HcProductionBatchRequest request) {
        return String.join(":",
                "PROD_BATCH",
                String.valueOf(request.getPlanId()),
                String.valueOf(request.getPlanOperationId()),
                firstNotBlank(request.getGenerationTrigger(), "-"),
                firstNotBlank(request.getGenerationScope(), "-"),
                firstNotBlank(request.getSourceTable(), "-"),
                String.valueOf(request.getSourceId()),
                firstNotBlank(request.getSourceDetailKey(), "-"),
                request.getProductionBatchNo());
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

    private record RootBatchParts(
            String lotNo,
            String typeCode,
            String yearCode,
            String monthCode,
            String lineCode,
            Integer annualSeq,
            Map<String, String> segmentValues) {
    }
}
