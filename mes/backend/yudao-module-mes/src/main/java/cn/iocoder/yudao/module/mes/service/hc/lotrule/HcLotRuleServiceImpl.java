package cn.iocoder.yudao.module.mes.service.hc.lotrule;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterInitializeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleParseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleCounterAdjustLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleCounterDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance.HcLotInstanceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleCounterAdjustLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleCounterMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleSegmentMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCLOTRULE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCLOTRULE_RULECODE_EXISTS;

@Service
@Validated
public class HcLotRuleServiceImpl implements HcLotRuleService {

    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 2;
    private static final String COUNTER_TYPE_ANNUAL_BATCH = "ANNUAL_BATCH";
    private static final String ADJUST_TYPE_INITIALIZE = "INITIALIZE";
    private static final String ADJUST_TYPE_MANUAL = "MANUAL";
    private static final String MONTH_CODE_MODE_A_TO_M = "A_TO_M";
    private static final String MONTH_CODE_MODE_MONTH_A_M = "MONTH_A_M";
    private static final String[] MONTH_CODES_SKIP_I =
            new String[]{"A", "B", "C", "D", "E", "F", "G", "H", "J", "K", "L", "M"};

    @Resource
    private HcLotRuleMapper hcLotRuleMapper;

    @Resource
    private HcLotRuleSegmentMapper hcLotRuleSegmentMapper;

    @Resource
    private HcLotRuleCounterMapper hcLotRuleCounterMapper;

    @Resource
    private HcLotRuleCounterAdjustLogMapper hcLotRuleCounterAdjustLogMapper;

    @Resource
    private HcLotInstanceMapper hcLotInstanceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcLotRule(HcLotRuleSaveReqVO createReqVO) {
        HcLotRuleDO entity = BeanUtils.toBean(createReqVO, HcLotRuleDO.class);
        normalizeRuleDefaults(entity, true);
        if (trimToNull(entity.getRuleCode()) == null) {
            entity.setRuleCode(nextSimpleRuleCode(entity));
        }
        validateRuleCodeUnique(null, entity.getRuleCode());
        int requestedStatus = entity.getStatus() == null ? STATUS_DRAFT : entity.getStatus();
        // 先以草稿插入规则段，再按用户选择的“启用”状态执行完整发布校验。
        entity.setStatus(STATUS_DRAFT);
        hcLotRuleMapper.insert(entity);
        createHcLotRuleSegmentList(entity.getId(), createReqVO.getLotRuleSegments());
        if (requestedStatus == STATUS_ENABLED) {
            validatePublishable(entity);
            HcLotRuleDO update = new HcLotRuleDO();
            update.setId(entity.getId());
            update.setStatus(STATUS_ENABLED);
            hcLotRuleMapper.updateById(update);
        } else if (requestedStatus == STATUS_DISABLED) {
            HcLotRuleDO update = new HcLotRuleDO();
            update.setId(entity.getId());
            update.setStatus(STATUS_DISABLED);
            hcLotRuleMapper.updateById(update);
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcLotRule(HcLotRuleSaveReqVO updateReqVO) {
        HcLotRuleDO oldRule = getRequiredLotRule(updateReqVO.getId());
        validateRuleNotUsedForStructuralChange(oldRule);
        HcLotRuleDO updateObj = BeanUtils.toBean(updateReqVO, HcLotRuleDO.class);
        if (trimToNull(updateObj.getRuleCode()) == null) {
            updateObj.setRuleCode(oldRule.getRuleCode());
        }
        validateRuleCodeUnique(updateReqVO.getId(), updateObj.getRuleCode());
        normalizeRuleDefaults(updateObj, false);
        updateObj.setVersionNo(oldRule.getVersionNo());
        // 状态只能通过发布/停用动作切换，避免绕过规则段及冲突校验。
        updateObj.setStatus(oldRule.getStatus());
        hcLotRuleMapper.updateById(updateObj);
        updateHcLotRuleSegmentList(updateReqVO.getId(), updateReqVO.getLotRuleSegments());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcLotRule(Long id) {
        HcLotRuleDO rule = getRequiredLotRule(id);
        validateRuleNotUsed(rule);
        hcLotRuleMapper.deleteById(id);
        hcLotRuleSegmentMapper.deleteByParentId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcLotRuleListByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        ids.forEach(id -> validateRuleNotUsed(getRequiredLotRule(id)));
        hcLotRuleMapper.deleteByIds(ids);
        hcLotRuleSegmentMapper.deleteByParentIds(ids);
    }

    @Override
    public HcLotRuleDO getHcLotRule(Long id) {
        return hcLotRuleMapper.selectById(id);
    }

    @Override
    public HcLotRuleDO matchEnabledRule(HcLotRuleMatchContext context) {
        if (context == null || trimToNull(context.getBizType()) == null
                || trimToNull(context.getGenerationTrigger()) == null
                || trimToNull(context.getGenerationScope()) == null) {
            throw invalidParamException("规则匹配必须提供业务对象、生成时机和生成粒度");
        }
        List<MatchedRule> matches = hcLotRuleMapper.selectList(new LambdaQueryWrapperX<HcLotRuleDO>()
                        .eq(HcLotRuleDO::getStatus, STATUS_ENABLED)
                        .eq(HcLotRuleDO::getBizType, context.getBizType()))
                .stream()
                .filter(rule -> matchesContext(rule, context))
                .map(rule -> new MatchedRule(rule, calculateSpecificity(rule, context)))
                .sorted(Comparator.comparingInt(MatchedRule::specificity).reversed()
                        .thenComparing(item -> item.rule().getPriority() == null ? 0 : item.rule().getPriority(),
                                Comparator.reverseOrder())
                        .thenComparing(item -> item.rule().getId(), Comparator.reverseOrder()))
                .toList();
        if (matches.isEmpty()) {
            throw invalidParamException("未找到启用的批号规则：业务对象={}, 产品分类={}, 生产类型={}, 时机={}, 粒度={}, 型号={}",
                    context.getBizType(), context.getProductCategoryCode(), context.getProdType(),
                    context.getGenerationTrigger(), context.getGenerationScope(), context.getModelCode());
        }
        MatchedRule best = matches.get(0);
        long sameTopCount = matches.stream()
                .filter(item -> item.specificity() == best.specificity()
                        && Objects.equals(item.rule().getPriority(), best.rule().getPriority()))
                .count();
        if (sameTopCount > 1) {
            throw invalidParamException("批号规则冲突：存在 {} 条同等匹配精度和优先级的启用规则，请停用或调整优先级", sameTopCount);
        }
        return best.rule();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyAsNewVersion(Long id) {
        HcLotRuleDO source = getRequiredLotRule(id);
        int versionNo = (source.getVersionNo() == null ? 1 : source.getVersionNo()) + 1;
        HcLotRuleDO copied = BeanUtils.toBean(source, HcLotRuleDO.class);
        copied.clean();
        copied.setId(null);
        copied.setRuleCode(nextVersionRuleCode(source.getRuleCode(), versionNo));
        copied.setRuleName(source.getRuleName() + " V" + versionNo);
        copied.setVersionNo(versionNo);
        copied.setStatus(STATUS_DRAFT);
        hcLotRuleMapper.insert(copied);
        List<HcLotRuleSegmentDO> segments = getHcLotRuleSegmentListByParentId(source.getId());
        segments.forEach(segment -> {
            segment.clean();
            segment.setId(null);
            segment.setRuleId(copied.getId());
            segment.setRuleCode(copied.getRuleCode());
        });
        if (!segments.isEmpty()) {
            hcLotRuleSegmentMapper.insertBatch(segments);
        }
        return copied.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishHcLotRule(Long id) {
        HcLotRuleDO rule = getRequiredLotRule(id);
        validateRuleNotUsedForStructuralChange(rule);
        validatePublishable(rule);
        HcLotRuleDO update = new HcLotRuleDO();
        update.setId(id);
        update.setStatus(STATUS_ENABLED);
        hcLotRuleMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableHcLotRule(Long id) {
        getRequiredLotRule(id);
        HcLotRuleDO update = new HcLotRuleDO();
        update.setId(id);
        update.setStatus(STATUS_DISABLED);
        hcLotRuleMapper.updateById(update);
    }

    @Override
    public List<HcLotRuleDO> getHcLotRuleSimpleList() {
        LambdaQueryWrapperX<HcLotRuleDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcLotRuleDO::getStatus, 1);
        queryWrapper.orderByAsc(HcLotRuleDO::getRuleCode);
        queryWrapper.orderByDesc(HcLotRuleDO::getId);
        return hcLotRuleMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcLotRuleDO> getHcLotRuleList(HcLotRulePageReqVO reqVO) {
        return hcLotRuleMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcLotRuleDO> getHcLotRulePage(HcLotRulePageReqVO pageReqVO) {
        return hcLotRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcLotRuleSegmentDO> getHcLotRuleSegmentListByParentId(Long parentId) {
        return hcLotRuleSegmentMapper.selectListByParentId(parentId);
    }

    @Override
    public List<HcLotRuleCounterDO> getHcLotRuleCounterListByRuleId(Long ruleId) {
        HcLotRuleDO rule = getRequiredLotRule(ruleId);
        return hcLotRuleCounterMapper.selectListByRuleId(resolveCounterOwner(rule).ruleId());
    }

    @Override
    public PageResult<HcLotRuleCounterDO> getHcLotRuleCounterPage(HcLotRuleCounterPageReqVO pageReqVO) {
        return hcLotRuleCounterMapper.selectPage(pageReqVO);
    }

    @Override
    public Long createHcLotRuleCounter(HcLotRuleCounterSaveReqVO createReqVO) {
        throw invalidParamException("流水记录由系统自动创建；如需设置初始值，请使用“初始化流水”操作");
    }

    @Override
    public void updateHcLotRuleCounter(HcLotRuleCounterSaveReqVO updateReqVO) {
        throw invalidParamException("不支持直接编辑流水记录；请使用“调整流水”并填写原因");
    }

    @Override
    public void deleteHcLotRuleCounter(Long id) {
        throw invalidParamException("不支持删除流水记录；请通过规则停用或按业务流程处理");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcLotRuleCounterDO initializeHcLotRuleCounter(HcLotRuleCounterInitializeReqVO reqVO) {
        HcLotRuleDO rule = resolveCounterRule(reqVO.getRuleId(), reqVO.getRuleCode());
        Integer year = reqVO.getYear();
        String counterType = firstNotBlank(reqVO.getCounterType(), COUNTER_TYPE_ANNUAL_BATCH);
        String bizDimensionKey = firstNotBlank(reqVO.getBizDimensionKey(), String.valueOf(year));
        String counterKey = buildProductionCounterKey(rule.getRuleCode(), counterType, bizDimensionKey);
        validateSingleCounter(rule.getId(), counterKey);

        Integer maxUsedSeq = resolveMaxUsedSeq(rule, counterType, year);
        if (reqVO.getCurrentSeq() < maxUsedSeq) {
            throw invalidParamException("当前年度已生成最大流水为 {}，初始化值不能小于该值", maxUsedSeq);
        }

        HcLotRuleCounterDO counter = hcLotRuleCounterMapper.selectByRuleAndCounterKeyForUpdate(rule.getId(), counterKey);
        Integer beforeCurrentSeq = null;
        String beforeLastLotNo = null;
        if (counter == null) {
            counter = new HcLotRuleCounterDO();
            counter.setRuleId(rule.getId());
            counter.setRuleCode(rule.getRuleCode());
            counter.setCounterType(counterType);
            counter.setBizDimensionKey(bizDimensionKey);
            counter.setBizDimensionJson("{\"year\":" + year + "}");
            counter.setCounterKey(counterKey);
            counter.setResetKey(bizDimensionKey);
            counter.setCurrentSeq(reqVO.getCurrentSeq());
            counter.setLastLotNo(trimToNull(reqVO.getLastLotNo()));
            hcLotRuleCounterMapper.insert(counter);
        } else {
            beforeCurrentSeq = counter.getCurrentSeq();
            beforeLastLotNo = counter.getLastLotNo();
            if (toInt(counter.getCurrentSeq()) > 0 || maxUsedSeq > 0) {
                throw invalidParamException("该年度流水已存在消费记录，请使用人工设置功能调整");
            }
            counter.setCounterType(counterType);
            counter.setBizDimensionKey(bizDimensionKey);
            counter.setBizDimensionJson("{\"year\":" + year + "}");
            counter.setResetKey(bizDimensionKey);
            counter.setCurrentSeq(reqVO.getCurrentSeq());
            counter.setLastLotNo(trimToNull(reqVO.getLastLotNo()));
            hcLotRuleCounterMapper.updateById(counter);
        }
        saveCounterAdjustLog(counter, ADJUST_TYPE_INITIALIZE, beforeCurrentSeq, counter.getCurrentSeq(),
                beforeLastLotNo, counter.getLastLotNo(), reqVO.getReason());
        return counter;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HcLotRuleCounterDO adjustHcLotRuleCounter(HcLotRuleCounterAdjustReqVO reqVO) {
        HcLotRuleCounterDO counter = hcLotRuleCounterMapper.selectByIdForUpdate(reqVO.getCounterId());
        if (counter == null) {
            throw invalidParamException("流水记录不存在");
        }
        Integer oldSeq = toInt(counter.getCurrentSeq());
        if (!Objects.equals(oldSeq, reqVO.getOldCurrentSeq())) {
            throw invalidParamException("计数器已被其他人调整，当前值为 {}，请刷新后重试", oldSeq);
        }
        HcLotRuleDO rule = getRequiredLotRule(counter.getRuleId());
        Integer counterYear = resolveCounterYear(counter);
        Integer maxUsedSeq = resolveMaxUsedSeq(rule, counter.getCounterType(), counterYear);
        if (reqVO.getNewCurrentSeq() < maxUsedSeq) {
            throw invalidParamException("当前年度已生成最大流水为 {}，人工设置值不能小于该值", maxUsedSeq);
        }

        String beforeLastLotNo = counter.getLastLotNo();
        counter.setCurrentSeq(reqVO.getNewCurrentSeq());
        counter.setLastLotNo(trimToNull(reqVO.getLastLotNo()));
        fillCounterDimensionDefaults(counter);
        hcLotRuleCounterMapper.updateById(counter);
        saveCounterAdjustLog(counter, ADJUST_TYPE_MANUAL, oldSeq, counter.getCurrentSeq(),
                beforeLastLotNo, counter.getLastLotNo(), reqVO.getReason());
        return counter;
    }

    @Override
    public HcLotRuleCounterPreviewRespVO previewNextLotRuleCounter(HcLotRuleCounterPreviewReqVO reqVO) {
        HcLotRuleDO rule = resolveCounterRule(reqVO.getRuleId(), reqVO.getRuleCode());
        LocalDate bizDate = reqVO.getBizDate() == null ? LocalDate.now() : reqVO.getBizDate();
        Integer year = reqVO.getYear() == null ? bizDate.getYear() : reqVO.getYear();
        String counterType = firstNotBlank(reqVO.getCounterType(), COUNTER_TYPE_ANNUAL_BATCH);
        String bizDimensionKey = firstNotBlank(reqVO.getBizDimensionKey(), String.valueOf(year));
        String counterKey = buildProductionCounterKey(rule.getRuleCode(), counterType, bizDimensionKey);
        HcLotRuleCounterDO counter = hcLotRuleCounterMapper.selectByRuleAndCounterKey(rule.getId(), counterKey);
        int step = rule.getSeqStep() == null || rule.getSeqStep() <= 0 ? 1 : rule.getSeqStep();
        int start = rule.getSeqStart() == null || rule.getSeqStart() <= 0 ? 1 : rule.getSeqStart();
        int currentSeq = reqVO.getCurrentSeq() == null
                ? (counter == null ? start - step : toInt(counter.getCurrentSeq()))
                : reqVO.getCurrentSeq();
        int maxUsedSeq = resolveMaxUsedSeq(rule, counterType, year);
        currentSeq = Math.max(currentSeq, maxUsedSeq);
        int nextSeq = currentSeq + step;

        HcLotRuleCounterPreviewRespVO respVO = new HcLotRuleCounterPreviewRespVO();
        respVO.setRuleId(rule.getId());
        respVO.setRuleCode(rule.getRuleCode());
        respVO.setRuleName(rule.getRuleName());
        respVO.setCounterType(counterType);
        respVO.setBizDimensionKey(bizDimensionKey);
        respVO.setCounterKey(counterKey);
        respVO.setResetKey(bizDimensionKey);
        respVO.setCurrentSeq(currentSeq);
        respVO.setNextSeq(nextSeq);
        respVO.setMaxUsedSeq(maxUsedSeq);
        respVO.setNextLotNo(buildPreviewRootLotNo(rule, bizDate, reqVO, nextSeq));
        if (currentSeq < maxUsedSeq) {
            respVO.setWarning("当前设置值小于已生成最大流水，提交时会被拦截");
        }
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> generateLotNo(HcLotRuleGenerateReqVO reqVO) {
        HcLotRuleDO rule = resolveRule(reqVO);
        List<HcLotRuleSegmentDO> segments = resolveSegments(rule.getId(), reqVO.getLotRuleSegments());
        if (segments.isEmpty()) {
            throw invalidParamException("批号规则【{}】未配置分段，无法生成", rule.getRuleName());
        }
        if (Boolean.FALSE.equals(reqVO.getConsumeSequence()) && rule.getId() != null
                && Boolean.FALSE.equals(rule.getAllowPreview())) {
            throw invalidParamException("批号规则【{}】不允许预览生成", rule.getRuleName());
        }
        LocalDate bizDate = reqVO.getBizDate() == null ? LocalDate.now() : reqVO.getBizDate();
        boolean consumeSequence = Boolean.TRUE.equals(reqVO.getConsumeSequence());
        SequenceState sequence = resolveSequenceState(rule, segments, bizDate, reqVO.getInputValues(), consumeSequence);

        Map<String, String> segmentValues = new LinkedHashMap<>();
        StringBuilder lotNoBuilder = new StringBuilder();
        for (HcLotRuleSegmentDO segment : segments) {
            if (Boolean.FALSE.equals(segment.getEnabled())) {
                continue;
            }
            String value = resolveSegmentContent(rule, segment, reqVO.getInputValues(), bizDate, sequence.nextSeq());
            segmentValues.put(segment.getSegmentCode(), value);
            lotNoBuilder.append(value);
            if (segment.getDelimiter() != null) {
                lotNoBuilder.append(segment.getDelimiter());
            }
        }
        String lotNo = lotNoBuilder.toString();
        if (consumeSequence && sequence.counterKey() != null) {
            saveCounter(rule, sequence, lotNo);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lotNo", lotNo);
        result.put("currentSeq", sequence.currentSeq());
        result.put("nextSeq", sequence.nextSeq());
        result.put("segmentValues", segmentValues);
        return result;
    }

    @Override
    public Map<String, String> parseLotNo(HcLotRuleParseReqVO reqVO) {
        HcLotRuleDO rule = resolveRule(reqVO);
        if (rule.getId() != null && Boolean.FALSE.equals(rule.getAllowParse())) {
            throw invalidParamException("批号规则【{}】不允许解析", rule.getRuleName());
        }
        List<HcLotRuleSegmentDO> segments = resolveSegments(rule.getId(), reqVO.getLotRuleSegments());
        if (segments.isEmpty()) {
            throw invalidParamException("批号规则【{}】未配置分段，无法解析", rule.getRuleName());
        }
        String source = reqVO.getLotNo();
        int cursor = 0;
        Map<String, String> result = new LinkedHashMap<>();
        for (HcLotRuleSegmentDO segment : segments) {
            int length = resolveSegmentLength(rule, segment);
            if (length <= 0) {
                result.put(segment.getSegmentCode(), source.substring(cursor));
                return result;
            }
            if (cursor + length > source.length()) {
                throw invalidParamException("批号【{}】长度不足，无法解析字段【{}】", source, segment.getSegmentName());
            }
            String value = source.substring(cursor, cursor + length);
            result.put(segment.getSegmentCode(), value);
            cursor += length;
            if (segment.getDelimiter() != null && !segment.getDelimiter().isEmpty()) {
                if (!source.startsWith(segment.getDelimiter(), cursor)) {
                    throw invalidParamException("批号【{}】在字段【{}】后缺少分隔符【{}】",
                            source, segment.getSegmentName(), segment.getDelimiter());
                }
                cursor += segment.getDelimiter().length();
            }
        }
        if (cursor < source.length()) {
            throw invalidParamException("批号【{}】在规则【{}】解析完成后仍有剩余字符【{}】",
                    source, rule.getRuleName(), source.substring(cursor));
        }
        return result;
    }

    private void validateHcLotRuleExists(Long id) {
        if (hcLotRuleMapper.selectById(id) == null) {
            throw exception(HCLOTRULE_NOT_EXISTS);
        }
    }

    private HcLotRuleDO resolveCounterRule(HcLotRuleCounterSaveReqVO reqVO) {
        return resolveCounterRule(reqVO.getRuleId(), reqVO.getRuleCode());
    }

    private HcLotRuleDO resolveCounterRule(Long ruleId, String ruleCode) {
        if (ruleId != null) {
            return getRequiredLotRule(ruleId);
        }
        String normalizedRuleCode = trimToNull(ruleCode);
        if (normalizedRuleCode != null) {
            HcLotRuleDO rule = hcLotRuleMapper.selectOne(
                    new LambdaQueryWrapperX<HcLotRuleDO>()
                            .eq(HcLotRuleDO::getRuleCode, normalizedRuleCode)
                            .last("LIMIT 1"));
            if (rule != null) {
                return rule;
            }
        }
        throw invalidParamException("批号规则不存在，请检查规则编码");
    }

    private void fillCounterDimensionDefaults(HcLotRuleCounterDO counter) {
        counter.setRuleCode(trimToNull(counter.getRuleCode()));
        counter.setCounterType(trimToNull(counter.getCounterType()));
        counter.setBizDimensionKey(trimToNull(counter.getBizDimensionKey()));
        counter.setBizDimensionJson(trimToNull(counter.getBizDimensionJson()));
        counter.setCounterKey(trimToNull(counter.getCounterKey()));
        counter.setResetKey(trimToNull(counter.getResetKey()));
        counter.setLastLotNo(trimToNull(counter.getLastLotNo()));
        if (counter.getCounterKey() != null) {
            String[] parts = counter.getCounterKey().split(":");
            if (counter.getCounterType() == null && parts.length >= 3) {
                counter.setCounterType(parts[parts.length - 2]);
            }
            if (counter.getBizDimensionKey() == null && parts.length >= 3) {
                counter.setBizDimensionKey(parts[parts.length - 1]);
            }
        }
        if (counter.getBizDimensionKey() == null) {
            counter.setBizDimensionKey(counter.getResetKey());
        }
        if (counter.getResetKey() == null) {
            counter.setResetKey(counter.getBizDimensionKey());
        }
    }

    private String buildProductionCounterKey(String ruleCode, String counterType, String bizDimensionKey) {
        return ruleCode + ":" + counterType + ":" + bizDimensionKey;
    }

    private void validateSingleCounter(Long ruleId, String counterKey) {
        Long count = hcLotRuleCounterMapper.selectCountByRuleAndCounterKey(ruleId, counterKey);
        if (count != null && count > 1) {
            throw invalidParamException("同一规则和计数键存在 {} 条计数器记录，请先清理重复数据", count);
        }
    }

    private Integer resolveCounterYear(HcLotRuleCounterDO counter) {
        Integer year = parseYear(counter.getBizDimensionKey());
        if (year != null) {
            return year;
        }
        year = parseYear(counter.getResetKey());
        if (year != null) {
            return year;
        }
        String counterKey = counter.getCounterKey();
        if (counterKey != null) {
            int index = counterKey.lastIndexOf(':');
            if (index >= 0 && index + 1 < counterKey.length()) {
                return parseYear(counterKey.substring(index + 1));
            }
        }
        return null;
    }

    private Integer parseYear(String value) {
        if (value == null || !value.matches("^\\d{4}$")) {
            return null;
        }
        return Integer.parseInt(value);
    }

    private int resolveMaxUsedSeq(HcLotRuleDO rule, String counterType, Integer year) {
        if (year == null || !COUNTER_TYPE_ANNUAL_BATCH.equalsIgnoreCase(firstNotBlank(counterType, ""))) {
            return 0;
        }
        Integer maxSeq = hcLotInstanceMapper.selectMaxAnnualBatchSeqByRuleAndYear(
                rule.getId(), year, String.format("%02d", year % 100));
        return maxSeq == null ? 0 : maxSeq;
    }

    private String buildPreviewRootLotNo(HcLotRuleDO rule, LocalDate bizDate, HcLotRuleCounterPreviewReqVO reqVO,
                                         int nextSeq) {
        HcLotRuleGenerateReqVO generateReq = new HcLotRuleGenerateReqVO();
        generateReq.setRuleId(rule.getId());
        generateReq.setBizDate(bizDate);
        generateReq.setConsumeSequence(false);
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("typeCode", firstNotBlank(reqVO.getTypeCode(), ""));
        // 流水台账按规则汇总时没有具体生产计划可读取产线，使用默认产线码仅用于展示下一号；
        // 正式配方开工仍由实际计划产线传入，不会使用此默认值。
        inputs.put("batchLineCode", firstNotBlank(reqVO.getBatchLineCode(), "A"));
        generateReq.setInputValues(inputs);
        return String.valueOf(generateLotNo(generateReq).get("lotNo"));
    }

    private String resolveCounterDimension(HcLotRuleDO rule, HcLotRuleSegmentDO segment,
                                           LocalDate bizDate, Map<String, String> inputs) {
        String expression = firstNotBlank(segment.getCounterDimensionExpr(), rule.getResetCycle());
        return switch (expression.toUpperCase(Locale.ROOT)) {
            case "YEAR", "ANNUAL", "ANNUAL_BATCH" -> String.valueOf(bizDate.getYear());
            case "MONTH" -> bizDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
            case "DAY" -> bizDate.format(DateTimeFormatter.BASIC_ISO_DATE);
            case "NONE", "FOREVER" -> "FOREVER";
            default -> firstNotBlank(inputs == null ? null : inputs.get(expression), expression);
        };
    }

    private boolean matchesContext(HcLotRuleDO rule, HcLotRuleMatchContext context) {
        return matchesOptional(rule.getProductCategoryCode(), context.getProductCategoryCode())
                && matchesOptional(rule.getProdType(), context.getProdType())
                && equalsIgnoreCase(rule.getGenerationTrigger(), context.getGenerationTrigger())
                && equalsIgnoreCase(rule.getGenerationScope(), context.getGenerationScope())
                && matchesModel(rule, context.getModelCode());
    }

    private boolean matchesOptional(String ruleValue, String contextValue) {
        return trimToNull(ruleValue) == null || equalsIgnoreCase(ruleValue, contextValue);
    }

    private boolean matchesModel(HcLotRuleDO rule, String modelCode) {
        String mode = firstNotBlank(rule.getModelMatchMode(), "ALL").toUpperCase(Locale.ROOT);
        String expected = trimToNull(rule.getModelMatchValue());
        if ("ALL".equals(mode) || expected == null) {
            return true;
        }
        String actual = trimToNull(modelCode);
        if (actual == null) {
            return false;
        }
        return switch (mode) {
            case "EXACT", "PRODUCT", "MOTHER" -> equalsIgnoreCase(expected, actual);
            case "PREFIX" -> actual.toUpperCase(Locale.ROOT).startsWith(expected.toUpperCase(Locale.ROOT));
            case "LIST" -> List.of(expected.split(",")).stream()
                    .map(this::trimToNull)
                    .anyMatch(value -> equalsIgnoreCase(value, actual));
            default -> false;
        };
    }

    private int calculateSpecificity(HcLotRuleDO rule, HcLotRuleMatchContext context) {
        int score = 0;
        score += trimToNull(rule.getProductCategoryCode()) == null ? 0 : 1;
        score += trimToNull(rule.getProdType()) == null ? 0 : 1;
        String mode = firstNotBlank(rule.getModelMatchMode(), "ALL").toUpperCase(Locale.ROOT);
        score += switch (mode) {
            case "EXACT", "PRODUCT", "MOTHER", "LIST" -> 3;
            case "PREFIX" -> 2;
            default -> 0;
        };
        return score;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }

    private void normalizeRuleDefaults(HcLotRuleDO rule, boolean creating) {
        if (creating) {
            rule.setStatus(rule.getStatus() == null ? STATUS_DRAFT : rule.getStatus());
            rule.setVersionNo(rule.getVersionNo() == null ? 1 : rule.getVersionNo());
        }
        rule.setPriority(rule.getPriority() == null ? 100 : rule.getPriority());
        rule.setModelMatchMode(firstNotBlank(rule.getModelMatchMode(), "ALL").toUpperCase(Locale.ROOT));
        rule.setGenerationTrigger(firstNotBlank(rule.getGenerationTrigger(), "FORMULA_START").toUpperCase(Locale.ROOT));
        rule.setGenerationScope(firstNotBlank(rule.getGenerationScope(), "PLAN_ROOT").toUpperCase(Locale.ROOT));
        rule.setBatchCardinality(firstNotBlank(rule.getBatchCardinality(), "ONE").toUpperCase(Locale.ROOT));
        rule.setResetCycle(firstNotBlank(rule.getResetCycle(), "YEAR").toUpperCase(Locale.ROOT));
        rule.setCounterGroupCode(trimToNull(rule.getCounterGroupCode()));
        rule.setAllowPreview(rule.getAllowPreview() == null || rule.getAllowPreview());
        rule.setAllowParse(rule.getAllowParse() == null || rule.getAllowParse());
        rule.setAllowManualOverride(Boolean.TRUE.equals(rule.getAllowManualOverride()));
    }

    private void validatePublishable(HcLotRuleDO rule) {
        if (trimToNull(rule.getBizType()) == null || trimToNull(rule.getRuleCode()) == null
                || trimToNull(rule.getGenerationTrigger()) == null || trimToNull(rule.getGenerationScope()) == null) {
            throw invalidParamException("发布前必须维护规则编码、业务对象、生成时机和生成粒度");
        }
        if (getHcLotRuleSegmentListByParentId(rule.getId()).stream()
                .noneMatch(segment -> Boolean.TRUE.equals(segment.getEnabled())
                        && "SEQ".equalsIgnoreCase(segment.getSegmentType()))) {
            throw invalidParamException("发布前必须配置一个启用的流水号规则段");
        }
        long conflictCount = hcLotRuleMapper.selectList(new LambdaQueryWrapperX<HcLotRuleDO>()
                        .eq(HcLotRuleDO::getStatus, STATUS_ENABLED)
                        .ne(HcLotRuleDO::getId, rule.getId())
                        .eq(HcLotRuleDO::getBizType, rule.getBizType())
                        .eq(HcLotRuleDO::getProductCategoryCode, rule.getProductCategoryCode())
                        .eq(HcLotRuleDO::getProdType, rule.getProdType())
                        .eq(HcLotRuleDO::getGenerationTrigger, rule.getGenerationTrigger())
                        .eq(HcLotRuleDO::getGenerationScope, rule.getGenerationScope())
                        .eq(HcLotRuleDO::getModelMatchMode, rule.getModelMatchMode())
                        .eq(HcLotRuleDO::getModelMatchValue, rule.getModelMatchValue())
                        .eq(HcLotRuleDO::getPriority, rule.getPriority()))
                .size();
        if (conflictCount > 0) {
            throw invalidParamException("发布会造成规则冲突：已有同适用范围、匹配条件和优先级的启用规则");
        }
    }

    private void validateRuleNotUsed(HcLotRuleDO rule) {
        Long usedCount = hcLotInstanceMapper.selectCountByRuleId(rule.getId());
        if (usedCount != null && usedCount > 0) {
            throw invalidParamException("规则【{}】已被 {} 个批号实例引用，不能删除；请停用或复制为新版本", rule.getRuleName(), usedCount);
        }
    }

    private void validateRuleNotUsedForStructuralChange(HcLotRuleDO rule) {
        Long usedCount = hcLotInstanceMapper.selectCountByRuleId(rule.getId());
        if (usedCount != null && usedCount > 0) {
            throw invalidParamException("规则【{}】已被 {} 个批号实例引用，不能直接修改；请复制为新版本", rule.getRuleName(), usedCount);
        }
    }

    private String nextVersionRuleCode(String sourceRuleCode, int versionNo) {
        String base = sourceRuleCode == null ? "LOT-RULE" : sourceRuleCode.replaceAll("-V\\d+$", "");
        String candidate = base + "-V" + versionNo;
        while (hcLotRuleMapper.selectOne(new LambdaQueryWrapperX<HcLotRuleDO>()
                .eq(HcLotRuleDO::getRuleCode, candidate).last("LIMIT 1")) != null) {
            versionNo++;
            candidate = base + "-V" + versionNo;
        }
        return candidate;
    }

    private String nextSimpleRuleCode(HcLotRuleDO rule) {
        String category = firstNotBlank(rule.getProductCategoryCode(), "COMMON").toUpperCase(Locale.ROOT);
        String productionType = firstNotBlank(rule.getProdType(), "MASS").toUpperCase(Locale.ROOT);
        String base = "LOT-CMP-" + category.replace("_PAD", "") + "-" + productionType;
        int version = 1;
        String candidate;
        do {
            candidate = base + "-V" + version++;
        } while (hcLotRuleMapper.selectOne(new LambdaQueryWrapperX<HcLotRuleDO>()
                .eq(HcLotRuleDO::getRuleCode, candidate)
                .last("LIMIT 1")) != null);
        rule.setVersionNo(version - 1);
        return candidate;
    }

    private record MatchedRule(HcLotRuleDO rule, int specificity) {
    }

    private record SequenceState(Integer currentSeq, Integer nextSeq, HcLotRuleCounterDO counter,
                                 Long counterRuleId, String counterRuleCode,
                                 String counterType, String resetKey, String counterKey) {
    }

    private record CounterOwner(Long ruleId, String ruleCode) {
    }

    private void saveCounterAdjustLog(HcLotRuleCounterDO counter, String adjustType, Integer beforeCurrentSeq,
                                      Integer afterCurrentSeq, String beforeLastLotNo, String afterLastLotNo,
                                      String reason) {
        HcLotRuleCounterAdjustLogDO log = new HcLotRuleCounterAdjustLogDO();
        log.setCounterId(counter.getId());
        log.setRuleId(counter.getRuleId());
        log.setRuleCode(counter.getRuleCode());
        log.setCounterType(counter.getCounterType());
        log.setBizDimensionKey(counter.getBizDimensionKey());
        log.setCounterKey(counter.getCounterKey());
        log.setResetKey(counter.getResetKey());
        log.setBeforeCurrentSeq(beforeCurrentSeq);
        log.setAfterCurrentSeq(afterCurrentSeq);
        log.setBeforeLastLotNo(beforeLastLotNo);
        log.setAfterLastLotNo(afterLastLotNo);
        log.setAdjustType(adjustType);
        log.setReason(reason.trim());
        log.setOperatorId(SecurityFrameworkUtils.getLoginUserId());
        log.setOperatorName(resolveLoginUserName());
        log.setAdjustTime(LocalDateTime.now());
        hcLotRuleCounterAdjustLogMapper.insert(log);
    }

    private String resolveLoginUserName() {
        String nickname = SecurityFrameworkUtils.getLoginUserNickname();
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        return userId == null ? "系统" : String.valueOf(userId);
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int toInt(Integer value) {
        return value == null ? 0 : value;
    }

    private HcLotRuleDO getRequiredLotRule(Long ruleId) {
        HcLotRuleDO rule = hcLotRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw exception(HCLOTRULE_NOT_EXISTS);
        }
        return rule;
    }

    private void validateHcLotRuleCounterExists(Long id) {
        if (id == null || hcLotRuleCounterMapper.selectById(id) == null) {
            throw invalidParamException("流水记录不存在");
        }
    }

    private void validateRuleCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcLotRuleDO entity = hcLotRuleMapper.selectOne(
                new LambdaQueryWrapperX<HcLotRuleDO>()
                        .eq(HcLotRuleDO::getRuleCode, value)
                        .neIfPresent(HcLotRuleDO::getId, id));
        if (entity != null) {
            throw exception(HCLOTRULE_RULECODE_EXISTS);
        }
    }

    private void createHcLotRuleSegmentList(Long parentId, List<HcLotRuleSegmentDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        HcLotRuleDO rule = hcLotRuleMapper.selectById(parentId);
        list.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setRuleId(parentId);
            item.setRuleCode(rule == null ? null : rule.getRuleCode());
        });
        hcLotRuleSegmentMapper.insertBatch(list);
    }

    private void updateHcLotRuleSegmentList(Long parentId, List<HcLotRuleSegmentDO> list) {
        HcLotRuleDO rule = hcLotRuleMapper.selectById(parentId);
        List<HcLotRuleSegmentDO> dbList = hcLotRuleSegmentMapper.selectListByParentId(parentId);
        if (list == null) {
            list = List.of();
        }

        Set<Long> reqIds = list.stream()
                .map(HcLotRuleSegmentDO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcLotRuleSegmentDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcLotRuleSegmentMapper.deleteBatch(HcLotRuleSegmentDO::getId, deleteIds);
        }

        List<HcLotRuleSegmentDO> updateList = list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .peek(item -> {
                    item.clean();
                    item.setRuleId(parentId);
                    item.setRuleCode(rule == null ? null : rule.getRuleCode());
                })
                .toList();
        if (!updateList.isEmpty()) {
            hcLotRuleSegmentMapper.updateBatch(updateList);
        }

        List<HcLotRuleSegmentDO> createList = list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .peek(item -> {
                    item.clean();
                    item.setId(null);
                    item.setRuleId(parentId);
                    item.setRuleCode(rule == null ? null : rule.getRuleCode());
                })
                .toList();
        if (!createList.isEmpty()) {
            hcLotRuleSegmentMapper.insertBatch(createList);
        }
    }

    private HcLotRuleDO resolveRule(HcLotRuleGenerateReqVO reqVO) {
        HcLotRuleDO rule = resolveRule(reqVO.getRuleId(), reqVO.getRuleCode());
        if (rule != null) {
            return rule;
        }
        return buildTransientRule(reqVO.getRuleCode(), reqVO.getRuleName(), reqVO.getRuleMode(),
                reqVO.getYearCodeMode(), reqVO.getMonthCodeMode(), reqVO.getSeqLength(), reqVO.getSeqStart(),
                reqVO.getSeqStep(), reqVO.getResetCycle(), reqVO.getSampleSegmentRule());
    }

    private HcLotRuleDO resolveRule(HcLotRuleParseReqVO reqVO) {
        HcLotRuleDO rule = resolveRule(reqVO.getRuleId(), reqVO.getRuleCode());
        if (rule != null) {
            return rule;
        }
        return buildTransientRule(reqVO.getRuleCode(), reqVO.getRuleName(), "LOT",
                reqVO.getYearCodeMode(), reqVO.getMonthCodeMode(), reqVO.getSeqLength(), 1, 1, "MONTH", null);
    }

    private HcLotRuleDO resolveRule(Long ruleId, String ruleCode) {
        if (ruleId != null) {
            HcLotRuleDO rule = getHcLotRule(ruleId);
            if (rule != null) {
                return rule;
            }
        }
        if (ruleCode != null && !ruleCode.isBlank()) {
            HcLotRuleDO rule = hcLotRuleMapper.selectOne(
                    new LambdaQueryWrapperX<HcLotRuleDO>()
                            .eq(HcLotRuleDO::getRuleCode, ruleCode)
                            .last("LIMIT 1"));
            if (rule != null) {
                return rule;
            }
        }
        return null;
    }

    private List<HcLotRuleSegmentDO> resolveSegments(Long ruleId, List<HcLotRuleSegmentDO> segments) {
        List<HcLotRuleSegmentDO> source = (segments == null || segments.isEmpty())
                ? (ruleId == null ? List.of() : getHcLotRuleSegmentListByParentId(ruleId))
                : segments;
        return source.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .toList();
    }

    private String buildResetKey(HcLotRuleDO rule, LocalDate bizDate) {
        String resetCycle = rule.getResetCycle() == null ? "MONTH" : rule.getResetCycle().toUpperCase(Locale.ROOT);
        return switch (resetCycle) {
            case "YEAR" -> String.valueOf(bizDate.getYear());
            case "DAY" -> bizDate.format(DateTimeFormatter.BASIC_ISO_DATE);
            case "NONE" -> "FOREVER";
            default -> bizDate.format(DateTimeFormatter.ofPattern("yyyyMM"));
        };
    }

    private SequenceState resolveSequenceState(HcLotRuleDO rule, List<HcLotRuleSegmentDO> segments,
                                               LocalDate bizDate, Map<String, String> inputs, boolean consume) {
        HcLotRuleSegmentDO seqSegment = segments.stream()
                .filter(segment -> !Boolean.FALSE.equals(segment.getEnabled()))
                .filter(segment -> "SEQ".equalsIgnoreCase(segment.getSegmentType()))
                .findFirst()
                .orElse(null);
        if (seqSegment == null) {
            return new SequenceState(0, 0, null, null, null, null, null, null);
        }
        int start = rule.getSeqStart() == null || rule.getSeqStart() <= 0 ? 1 : rule.getSeqStart();
        int step = rule.getSeqStep() == null || rule.getSeqStep() <= 0 ? 1 : rule.getSeqStep();
        if (rule.getId() == null || rule.getId() <= 0) {
            return new SequenceState(start - step, start, null, null, null, null, null, null);
        }
        boolean legacyCounterKey = trimToNull(seqSegment.getCounterType()) == null
                && trimToNull(seqSegment.getCounterDimensionExpr()) == null;
        String counterType = firstNotBlank(seqSegment.getCounterType(), COUNTER_TYPE_ANNUAL_BATCH);
        String resetKey = resolveCounterDimension(rule, seqSegment, bizDate, inputs);
        // 旧规则没有显式的流水类型、维度，沿用历史 key，避免升级后把旧流水从 1 重新开始。
        String counterKey = legacyCounterKey
                ? rule.getRuleCode() + ":" + buildResetKey(rule, bizDate)
                : buildProductionCounterKey(firstNotBlank(rule.getCounterGroupCode(), rule.getRuleCode()),
                counterType, resetKey);
        CounterOwner counterOwner = resolveCounterOwner(rule);
        if (consume) {
            // 先锁流水归属规则，避免同一规则或同一流水组首次创建计数器时出现无行锁竞争。
            hcLotRuleMapper.selectByIdForUpdate(counterOwner.ruleId());
        }
        HcLotRuleCounterDO counter = consume
                ? hcLotRuleCounterMapper.selectByRuleAndCounterKeyForUpdate(counterOwner.ruleId(), counterKey)
                : hcLotRuleCounterMapper.selectByRuleAndCounterKey(counterOwner.ruleId(), counterKey);
        int currentSeq = counter == null ? start - step : toInt(counter.getCurrentSeq());
        Integer counterYear = parseYear(resetKey);
        currentSeq = Math.max(currentSeq, resolveMaxUsedSeq(rule, counterType, counterYear));
        return new SequenceState(currentSeq, currentSeq + step, counter, counterOwner.ruleId(), counterOwner.ruleCode(),
                counterType, resetKey, counterKey);
    }

    private void saveCounter(HcLotRuleDO rule, SequenceState sequence, String lotNo) {
        HcLotRuleCounterDO counter = sequence.counter();
        if (counter == null) {
            counter = HcLotRuleCounterDO.builder()
                    .ruleId(sequence.counterRuleId())
                    .ruleCode(sequence.counterRuleCode())
                    .counterType(sequence.counterType())
                    .bizDimensionKey(sequence.resetKey())
                    .counterKey(sequence.counterKey())
                    .resetKey(sequence.resetKey())
                    .currentSeq(sequence.nextSeq())
                    .lastLotNo(lotNo)
                    .build();
            hcLotRuleCounterMapper.insert(counter);
            return;
        }
        counter.setCounterType(sequence.counterType());
        counter.setBizDimensionKey(sequence.resetKey());
        counter.setResetKey(sequence.resetKey());
        counter.setCurrentSeq(sequence.nextSeq());
        counter.setLastLotNo(lotNo);
        hcLotRuleCounterMapper.updateById(counter);
    }

    private CounterOwner resolveCounterOwner(HcLotRuleDO rule) {
        String counterGroupCode = trimToNull(rule.getCounterGroupCode());
        if (counterGroupCode == null) {
            return new CounterOwner(rule.getId(), rule.getRuleCode());
        }
        HcLotRuleDO owner = hcLotRuleMapper.selectOne(new LambdaQueryWrapperX<HcLotRuleDO>()
                .eq(HcLotRuleDO::getCounterGroupCode, counterGroupCode)
                .orderByAsc(HcLotRuleDO::getId)
                .last("LIMIT 1"));
        return owner == null ? new CounterOwner(rule.getId(), rule.getRuleCode())
                : new CounterOwner(owner.getId(), owner.getRuleCode());
    }

    private String resolveSegmentContent(HcLotRuleDO rule, HcLotRuleSegmentDO segment, Map<String, String> inputValues,
                                         LocalDate bizDate, int seqValue) {
        String segmentType = segment.getSegmentType() == null ? "INPUT" : segment.getSegmentType().toUpperCase(Locale.ROOT);
        Map<String, String> inputs = inputValues == null ? Map.of() : inputValues;
        String rawValue = switch (segmentType) {
            case "FIXED" -> defaultString(segment.getSegmentValue());
            case "YEAR_CODE" -> formatYearCode(bizDate, segment.getSegmentValue(), rule.getYearCodeMode());
            case "MONTH_CODE" -> formatMonthCode(bizDate.getMonthValue(), rule.getMonthCodeMode());
            case "SEQ" -> padLeft(String.valueOf(seqValue), resolveSegmentLength(rule, segment));
            case "SAMPLE_CODE" -> formatSampleCode(inputs.get(firstNotBlank(segment.getSourceField(), segment.getSegmentCode())));
            case "CONTEXT", "CONTEXT_FIELD" -> defaultString(inputs.get(firstNotBlank(segment.getSourceField(), segment.getSegmentCode())));
            default -> defaultString(inputs.get(firstNotBlank(segment.getSourceField(), segment.getSegmentCode())));
        };
        validateSegmentValue(rule, segment, rawValue);
        return rawValue;
    }

    private void validateSegmentValue(HcLotRuleDO rule, HcLotRuleSegmentDO segment, String value) {
        int expectedLength = resolveSegmentLength(rule, segment);
        if (expectedLength <= 0) {
            return;
        }
        String segmentName = segment.getSegmentName() == null ? segment.getSegmentCode() : segment.getSegmentName();
        if (value == null || value.isBlank()) {
            throw invalidParamException("批号规则【{}】字段【{}】不能为空", rule.getRuleName(), segmentName);
        }
        if (value.length() != expectedLength) {
            throw invalidParamException("批号规则【{}】字段【{}】长度必须为 {} 位，当前为 {} 位",
                    rule.getRuleName(), segmentName, expectedLength, value.length());
        }
    }

    private int resolveSegmentLength(HcLotRuleDO rule, HcLotRuleSegmentDO segment) {
        if (segment.getSegmentLength() != null && segment.getSegmentLength() > 0) {
            return segment.getSegmentLength();
        }
        String type = segment.getSegmentType() == null ? "" : segment.getSegmentType().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "SEQ" -> rule.getSeqLength() == null ? 0 : rule.getSeqLength();
            case "MONTH_CODE", "SAMPLE_CODE" -> 1;
            case "YEAR_CODE" -> formatYearCode(LocalDate.now(), segment.getSegmentValue(), rule.getYearCodeMode()).length();
            case "FIXED" -> defaultString(segment.getSegmentValue()).length();
            default -> parseManualLength(segment.getSegmentValue());
        };
    }

    private String formatYearCode(LocalDate bizDate, String pattern, String defaultPattern) {
        String yy = String.format("%02d", bizDate.getYear() % 100);
        String actualPattern = (pattern == null || pattern.isBlank()) ? defaultPattern : pattern;
        if (actualPattern == null || actualPattern.isBlank()) {
            return yy;
        }
        return actualPattern.replace("yyyy", String.valueOf(bizDate.getYear())).replace("yy", yy);
    }

    private String formatMonthCode(int month, String monthCodeMode) {
        String[] monthCodes = resolveMonthCodes(monthCodeMode);
        if (month < 1 || month > 12) {
            throw new ServiceException(1008100038, "月份超出有效范围");
        }
        return monthCodes[month - 1];
    }

    private String formatSampleCode(String input) {
        if (input == null || input.isBlank()) {
            throw new ServiceException(1008100039, "抽样段次不能为空");
        }
        if (input.matches("^[P-Zp-z]$")) {
            return input.toUpperCase(Locale.ROOT);
        }
        int value = Integer.parseInt(input);
        String[] codes = {"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
        if (value < 1 || value > codes.length) {
            throw new ServiceException(1008100040, "抽样段次必须在 1 到 11 之间");
        }
        return codes[value - 1];
    }

    private String[] resolveMonthCodes(String monthCodeMode) {
        String normalizedMode = monthCodeMode == null ? "" : monthCodeMode.trim().toUpperCase(Locale.ROOT);
        if (normalizedMode.isBlank()
                || MONTH_CODE_MODE_A_TO_M.equals(normalizedMode)
                || MONTH_CODE_MODE_MONTH_A_M.equals(normalizedMode)) {
            return MONTH_CODES_SKIP_I;
        }
        if (normalizedMode.contains(",")) {
            String[] values = normalizedMode.split(",");
            if (values.length == 12) {
                return normalizeConfiguredMonthCodes(values);
            }
        }
        return MONTH_CODES_SKIP_I;
    }

    private String[] normalizeConfiguredMonthCodes(String[] values) {
        String[] monthCodes = new String[values.length];
        boolean containsI = false;
        boolean singleLetters = true;
        for (int i = 0; i < values.length; i++) {
            monthCodes[i] = values[i] == null ? "" : values[i].trim().toUpperCase(Locale.ROOT);
            containsI = containsI || "I".equals(monthCodes[i]);
            singleLetters = singleLetters && monthCodes[i].matches("^[A-Z]$");
        }
        return containsI && singleLetters ? MONTH_CODES_SKIP_I : monthCodes;
    }

    private int parseManualLength(String value) {
        if (value != null && value.matches("^\\d+$")) {
            return Integer.parseInt(value);
        }
        return 0;
    }

    private HcLotRuleDO buildTransientRule(String ruleCode, String ruleName, String ruleMode, String yearCodeMode,
                                           String monthCodeMode, Integer seqLength, Integer seqStart, Integer seqStep,
                                           String resetCycle, String sampleSegmentRule) {
        if (seqLength == null || seqLength <= 0) {
            throw invalidParamException("草稿规则未设置流水长度，无法生成或解析批号");
        }
        return HcLotRuleDO.builder()
                .id(0L)
                .ruleCode((ruleCode == null || ruleCode.isBlank()) ? "DRAFT_RULE" : ruleCode)
                .ruleName((ruleName == null || ruleName.isBlank()) ? "草稿批号规则" : ruleName)
                .ruleMode(ruleMode)
                .yearCodeMode(yearCodeMode)
                .monthCodeMode(monthCodeMode)
                .seqLength(seqLength)
                .seqStart(seqStart)
                .seqStep(seqStep)
                .resetCycle(resetCycle)
                .sampleSegmentRule(sampleSegmentRule)
                .build();
    }

    private String padLeft(String source, int length) {
        if (length <= 0) {
            return source;
        }
        return String.format("%1$" + length + "s", source).replace(' ', '0');
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
