package cn.iocoder.yudao.module.mes.service.hc.researchtask;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskCodePreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskCodePreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskRouteOperationReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.researchtask.vo.HcResearchTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelSegmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.researchtask.HcResearchTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.modelrule.HcModelRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelSegmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.researchtask.HcResearchTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.route.HcRouteMapper;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
public class HcResearchTaskServiceImpl implements HcResearchTaskService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_CONFIRMED = "CONFIRMED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";
    private static final String SOURCE_TYPE_RD_ARCHIVED = "RD_ARCHIVED";
    private static final String DEFAULT_BATCH_RULE_CODE = "LOT-RD-PROCESS-DATE";
    private static final DateTimeFormatter TASK_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    @Resource
    private HcResearchTaskMapper hcResearchTaskMapper;
    @Resource
    private HcRouteMapper hcRouteMapper;
    @Resource
    private HcProductModelMapper hcProductModelMapper;
    @Resource
    private HcProductModelSegmentMapper hcProductModelSegmentMapper;
    @Resource
    private HcModelRuleMapper hcModelRuleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createResearchTask(HcResearchTaskSaveReqVO createReqVO) {
        HcResearchTaskDO entity = BeanUtils.toBean(createReqVO, HcResearchTaskDO.class);
        entity.setId(null);
        entity.setTaskNo(StrUtil.blankToDefault(entity.getTaskNo(), generateTaskNo()));
        entity.setTaskStatus(StrUtil.blankToDefault(entity.getTaskStatus(), STATUS_DRAFT));
        entity.setResearchDate(entity.getResearchDate() == null ? LocalDate.now() : entity.getResearchDate());
        fillIssueDefaults(entity, null);
        fillNormalizedRuleFields(entity, null);
        fillReferenceSnapshots(entity);
        fillUsageCounts(entity);
        entity.setSnapshotJson(buildTaskSnapshotJson(entity, createReqVO.getRouteOperations()));
        hcResearchTaskMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateResearchTask(HcResearchTaskSaveReqVO updateReqVO) {
        HcResearchTaskDO db = validateExists(updateReqVO.getId());
        if (STATUS_ARCHIVED.equals(db.getTaskStatus())) {
            throw invalidParamException("研发型号已归档，不能再修改");
        }
        HcResearchTaskDO updateObj = BeanUtils.toBean(updateReqVO, HcResearchTaskDO.class);
        updateObj.setTaskStatus(StrUtil.blankToDefault(updateObj.getTaskStatus(), db.getTaskStatus()));
        updateObj.setResearchDate(updateObj.getResearchDate() == null ? db.getResearchDate() : updateObj.getResearchDate());
        fillIssueDefaults(updateObj, db);
        fillNormalizedRuleFields(updateObj, updateReqVO.getId());
        fillReferenceSnapshots(updateObj);
        fillUsageCounts(updateObj);
        updateObj.setSnapshotJson(buildTaskSnapshotJson(updateObj, updateReqVO.getRouteOperations()));
        hcResearchTaskMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResearchTask(Long id) {
        HcResearchTaskDO entity = validateExists(id);
        if (!STATUS_DRAFT.equals(entity.getTaskStatus())) {
            throw invalidParamException("只有草稿状态的研发型号可以删除");
        }
        hcResearchTaskMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteResearchTaskListByIds(List<Long> ids) {
        for (Long id : ids) {
            deleteResearchTask(id);
        }
    }

    @Override
    public HcResearchTaskDO getResearchTask(Long id) {
        return validateExists(id);
    }

    @Override
    public PageResult<HcResearchTaskDO> getResearchTaskPage(HcResearchTaskPageReqVO pageReqVO) {
        return hcResearchTaskMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcResearchTaskDO> getResearchTaskList(HcResearchTaskPageReqVO reqVO) {
        return hcResearchTaskMapper.selectList(reqVO);
    }

    @Override
    public HcResearchTaskCodePreviewRespVO previewModelCode(HcResearchTaskCodePreviewReqVO reqVO) {
        String productClassCode = normalizeProductClassCode(reqVO.getProductClassCode());
        String formulaCode = normalizeFixedCode(reqVO.getBaseFormulaCode(), 2, true, "基准配方编码");
        String wetCode = normalizeFixedCode(reqVO.getWetProcessCode(), 1, false, "湿法工艺编码");
        String grindingCode = normalizeFixedCode(reqVO.getGrindingProcessCode(), 1, false, "磨皮工艺编码");
        String postCode = normalizeFixedCode(reqVO.getPostProcessCode(), 2, true, "后工艺编码");
        int usageCount = toInt(hcResearchTaskMapper.countByCombination(
                reqVO.getId(), productClassCode, formulaCode, wetCode, grindingCode, postCode));
        int reuseSeq = usageCount + 1;
        String modelCode = buildModelCode(productClassCode, formulaCode, wetCode, grindingCode, postCode, reuseSeq);
        return new HcResearchTaskCodePreviewRespVO(
                modelCode,
                modelCode,
                reuseSeq,
                usageCount,
                countBy(HcResearchTaskDO::getBaseFormulaCode, formulaCode, reqVO.getId()),
                countBy(HcResearchTaskDO::getWetProcessCode, wetCode, reqVO.getId()),
                countBy(HcResearchTaskDO::getGrindingProcessCode, grindingCode, reqVO.getId()),
                countBy(HcResearchTaskDO::getPostProcessCode, postCode, reqVO.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmResearchTask(Long id) {
        HcResearchTaskDO entity = validateExists(id);
        if (!STATUS_DRAFT.equals(entity.getTaskStatus())) {
            throw invalidParamException("只有草稿状态的研发型号可以确认");
        }
        HcResearchTaskDO updateObj = new HcResearchTaskDO();
        updateObj.setId(id);
        updateObj.setTaskStatus(STATUS_CONFIRMED);
        hcResearchTaskMapper.updateById(updateObj);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long archiveToProductModel(Long id) {
        HcResearchTaskDO task = validateExists(id);
        if (!STATUS_CONFIRMED.equals(task.getTaskStatus()) && !STATUS_ARCHIVED.equals(task.getTaskStatus())) {
            throw invalidParamException("请先确认研发型号，再转入型号字典");
        }
        HcProductModelDO productModel = hcProductModelMapper.selectByModelCode(task.getRdModelCode());
        if (productModel == null) {
            productModel = createProductModel(task);
        }
        HcResearchTaskDO updateObj = new HcResearchTaskDO();
        updateObj.setId(task.getId());
        updateObj.setTaskStatus(STATUS_ARCHIVED);
        updateObj.setArchivedModelId(productModel.getId());
        updateObj.setArchivedModelCode(productModel.getModelCode());
        updateObj.setArchivedTime(LocalDateTime.now());
        hcResearchTaskMapper.updateById(updateObj);
        return productModel.getId();
    }

    private HcResearchTaskDO validateExists(Long id) {
        if (id == null) {
            throw invalidParamException("研发型号不存在");
        }
        HcResearchTaskDO entity = hcResearchTaskMapper.selectById(id);
        if (entity == null) {
            throw invalidParamException("研发型号不存在");
        }
        return entity;
    }

    private void fillNormalizedRuleFields(HcResearchTaskDO entity, Long excludeId) {
        String productClassCode = normalizeProductClassCode(entity.getProductClassCode());
        String formulaCode = normalizeFixedCode(entity.getBaseFormulaCode(), 2, true, "基准配方编码");
        String wetCode = normalizeFixedCode(entity.getWetProcessCode(), 1, false, "湿法工艺编码");
        String grindingCode = normalizeFixedCode(entity.getGrindingProcessCode(), 1, false, "磨皮工艺编码");
        String postCode = normalizeFixedCode(entity.getPostProcessCode(), 2, true, "后工艺编码");
        int reuseSeq = toInt(hcResearchTaskMapper.countByCombination(
                excludeId, productClassCode, formulaCode, wetCode, grindingCode, postCode)) + 1;
        String modelCode = buildModelCode(productClassCode, formulaCode, wetCode, grindingCode, postCode, reuseSeq);

        HcResearchTaskDO exists = hcResearchTaskMapper.selectOne(new LambdaQueryWrapperX<HcResearchTaskDO>()
                .eq(HcResearchTaskDO::getRdModelCode, modelCode)
                .neIfPresent(HcResearchTaskDO::getId, excludeId));
        if (exists != null) {
            throw invalidParamException("研发型号编码 {} 已存在，请刷新后重试", modelCode);
        }

        entity.setProductClassCode(productClassCode);
        entity.setProductClassName(resolveProductClassName());
        entity.setBatchTypeCode(resolveBatchTypeCode());
        entity.setBaseFormulaCode(formulaCode);
        entity.setBaseFormulaName(StrUtil.blankToDefault(entity.getBaseFormulaName(), formulaCode));
        entity.setWetProcessCode(wetCode);
        entity.setWetProcessName(StrUtil.blankToDefault(entity.getWetProcessName(), "湿法工艺" + wetCode));
        entity.setGrindingProcessCode(grindingCode);
        entity.setGrindingProcessName(StrUtil.blankToDefault(entity.getGrindingProcessName(), "磨皮工艺" + grindingCode));
        entity.setPostProcessCode(postCode);
        entity.setPostProcessName(StrUtil.blankToDefault(entity.getPostProcessName(), "后工艺" + postCode));
        entity.setReuseSeq(reuseSeq);
        entity.setRdModelCode(modelCode);
        entity.setDisplayModelCode(modelCode);
        entity.setBatchRuleCode(StrUtil.blankToDefault(entity.getBatchRuleCode(), DEFAULT_BATCH_RULE_CODE));
        entity.setTargetUom(StrUtil.blankToDefault(entity.getTargetUom(), "PCS"));
    }

    private void fillIssueDefaults(HcResearchTaskDO entity, HcResearchTaskDO db) {
        if (entity.getIssueUserId() == null) {
            entity.setIssueUserId(db == null || db.getIssueUserId() == null
                    ? SecurityFrameworkUtils.getLoginUserId()
                    : db.getIssueUserId());
        }
        if (StrUtil.isBlank(entity.getIssueUserName())) {
            String defaultName = db == null ? null : db.getIssueUserName();
            entity.setIssueUserName(StrUtil.blankToDefault(
                    defaultName,
                    StrUtil.blankToDefault(SecurityFrameworkUtils.getLoginUserNickname(), "system")));
        }
        if (entity.getIssueTime() == null) {
            entity.setIssueTime(db == null || db.getIssueTime() == null ? LocalDateTime.now() : db.getIssueTime());
        }
    }

    private void fillReferenceSnapshots(HcResearchTaskDO entity) {
        if (entity.getRouteId() != null) {
            HcRouteDO route = hcRouteMapper.selectById(entity.getRouteId());
            if (route != null) {
                entity.setRouteCode(route.getRouteCode());
                entity.setRouteName(route.getRouteName());
                entity.setRouteVersion(route.getVersionNo());
            }
        }
    }

    private void fillUsageCounts(HcResearchTaskDO entity) {
        entity.setFormulaUsageCount(countBy(HcResearchTaskDO::getBaseFormulaCode, entity.getBaseFormulaCode(), entity.getId()));
        entity.setWetUsageCount(countBy(HcResearchTaskDO::getWetProcessCode, entity.getWetProcessCode(), entity.getId()));
        entity.setGrindingUsageCount(countBy(HcResearchTaskDO::getGrindingProcessCode, entity.getGrindingProcessCode(), entity.getId()));
        entity.setPostUsageCount(countBy(HcResearchTaskDO::getPostProcessCode, entity.getPostProcessCode(), entity.getId()));
        entity.setCombinationUsageCount(toInt(hcResearchTaskMapper.countByCombination(
                entity.getId(),
                entity.getProductClassCode(),
                entity.getBaseFormulaCode(),
                entity.getWetProcessCode(),
                entity.getGrindingProcessCode(),
                entity.getPostProcessCode())) + 1);
    }

    private int countBy(com.baomidou.mybatisplus.core.toolkit.support.SFunction<HcResearchTaskDO, ?> column,
            String value, Long excludeId) {
        if (StrUtil.isBlank(value)) {
            return 0;
        }
        return toInt(hcResearchTaskMapper.selectCount(new LambdaQueryWrapperX<HcResearchTaskDO>()
                .eq(column, value)
                .neIfPresent(HcResearchTaskDO::getId, excludeId))) + 1;
    }

    private HcProductModelDO createProductModel(HcResearchTaskDO task) {
        HcModelRuleDO rule = resolveModelRule();
        HcProductModelDO model = new HcProductModelDO();
        model.setModelCode(task.getRdModelCode());
        model.setModelName(task.getRdModelCode());
        model.setModelAlias(task.getDisplayModelCode());
        model.setModelRuleId(rule == null ? 0L : rule.getId());
        model.setModelRuleCode(rule == null ? "MODEL-RD" : rule.getRuleCode());
        model.setModelRuleName(rule == null ? "研发型号规则" : rule.getRuleName());
        model.setProductType("FG");
        model.setProdType("RND_TRIAL");
        model.setProdTypeName("研发试制");
        model.setCategoryCode(resolveCategoryCode());
        model.setCategoryName(task.getProductClassName());
        model.setDefaultRouteId(task.getRouteId());
        model.setDefaultRouteCode(task.getRouteCode());
        model.setDefaultRouteName(task.getRouteName());
        model.setSegmentSnapshotJson(JsonUtils.toJsonString(buildModelSegmentSnapshots(task)));
        model.setSourceType(SOURCE_TYPE_RD_ARCHIVED);
        model.setStatus("ENABLE");
        model.setReferencedFlag(false);
        model.setRemark("由研发管理确认归档；研发任务号：" + task.getTaskNo());
        model.setTenantId(task.getTenantId());
        hcProductModelMapper.insert(model);
        createProductModelSegments(model, task);
        return model;
    }

    private HcModelRuleDO resolveModelRule() {
        HcModelRuleDO rule = hcModelRuleMapper.selectOne(HcModelRuleDO::getRuleCode, "MODEL-RD");
        if (rule != null) {
            return rule;
        }
        return hcModelRuleMapper.selectOne(HcModelRuleDO::getRuleCode, "MODEL-WHITE");
    }

    private void createProductModelSegments(HcProductModelDO model, HcResearchTaskDO task) {
        List<Map<String, Object>> snapshots = buildModelSegmentSnapshots(task);
        int sort = 1;
        for (Map<String, Object> snapshot : snapshots) {
            HcProductModelSegmentDO segment = new HcProductModelSegmentDO();
            segment.setModelId(model.getId());
            segment.setModelCode(model.getModelCode());
            segment.setRuleId(model.getModelRuleId());
            segment.setRuleCode(model.getModelRuleCode());
            segment.setItemCode(String.valueOf(snapshot.get("itemCode")));
            segment.setItemName(String.valueOf(snapshot.get("itemName")));
            segment.setSegmentValue(String.valueOf(snapshot.get("segmentValue")));
            segment.setSegmentText(String.valueOf(snapshot.get("segmentText")));
            segment.setSort(sort++);
            segment.setTenantId(task.getTenantId());
            hcProductModelSegmentMapper.insert(segment);
        }
    }

    private List<Map<String, Object>> buildModelSegmentSnapshots(HcResearchTaskDO task) {
        List<Map<String, Object>> segments = new ArrayList<>();
        segments.add(segment("product_class", "型号类型", task.getProductClassCode(), task.getProductClassName()));
        segments.add(segment("formula", "基准配方", task.getBaseFormulaCode(), task.getBaseFormulaName()));
        segments.add(segment("wet_process", "湿法工艺", task.getWetProcessCode(), task.getWetProcessName()));
        segments.add(segment("grinding_process", "磨皮工艺", task.getGrindingProcessCode(), task.getGrindingProcessName()));
        segments.add(segment("post_process", "后工艺", task.getPostProcessCode(), task.getPostProcessName()));
        segments.add(segment("reuse_seq", "重复序号", String.valueOf(task.getReuseSeq()), "R" + task.getReuseSeq()));
        return segments;
    }

    private Map<String, Object> segment(String itemCode, String itemName, String segmentValue, String segmentText) {
        Map<String, Object> segment = new LinkedHashMap<>();
        segment.put("itemCode", itemCode);
        segment.put("itemName", itemName);
        segment.put("segmentValue", StrUtil.blankToDefault(segmentValue, ""));
        segment.put("segmentText", StrUtil.blankToDefault(segmentText, segmentValue));
        return segment;
    }

    private String buildTaskSnapshotJson(HcResearchTaskDO entity, List<HcResearchTaskRouteOperationReqVO> routeOperations) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("rule", "RD/RA + formula(2) + wet(1) + grinding(1) + post(2) + optional -R(n)");
        snapshot.put("rdModelCode", entity.getRdModelCode());
        snapshot.put("reuseSeq", entity.getReuseSeq());
        snapshot.put("issueUserName", entity.getIssueUserName());
        snapshot.put("issueTime", entity.getIssueTime());
        snapshot.put("batchTypeCode", entity.getBatchTypeCode());
        snapshot.put("batchRuleCode", entity.getBatchRuleCode());
        snapshot.put("segments", buildModelSegmentSnapshots(entity));
        snapshot.put("routeOperations", routeOperations == null ? List.of() : routeOperations);
        return JsonUtils.toJsonString(snapshot);
    }

    private String normalizeProductClassCode(String value) {
        String code = normalizeCode(value);
        if (!Objects.equals(code, "RD") && !Objects.equals(code, "RA")) {
            throw invalidParamException("型号类型只允许 RD(黑垫) 或 RA(黑垫)");
        }
        return code;
    }

    private String normalizeFixedCode(String value, int length, boolean leftPadNumber, String fieldName) {
        String code = normalizeCode(value);
        if (leftPadNumber && code.matches("\\d+") && code.length() < length) {
            code = StrUtil.padPre(code, length, '0');
        }
        if (code.length() != length) {
            throw invalidParamException("{}长度必须为{}位", fieldName, length);
        }
        return code;
    }

    private String normalizeCode(String value) {
        String code = StrUtil.trimToEmpty(value).toUpperCase();
        if (StrUtil.isBlank(code)) {
            throw invalidParamException("编码不能为空");
        }
        return code;
    }

    private String buildModelCode(String productClassCode, String formulaCode, String wetCode,
            String grindingCode, String postCode, int reuseSeq) {
        String base = productClassCode + formulaCode + wetCode + grindingCode + postCode;
        return reuseSeq <= 1 ? base : base + "-R" + reuseSeq;
    }

    private String resolveProductClassName() {
        return "CMP黑垫";
    }

    private String resolveBatchTypeCode() {
        return "C";
    }

    private String resolveCategoryCode() {
        return "BLACK_PAD";
    }

    private String generateTaskNo() {
        return "RD-TASK-" + LocalDateTime.now().format(TASK_NO_FORMATTER);
    }

    private int toInt(Long value) {
        return value == null ? 0 : Math.toIntExact(value);
    }

}
