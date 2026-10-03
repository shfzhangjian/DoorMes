package cn.iocoder.yudao.module.mes.service.hc.productmodel;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelMaterialReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSegmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelSegmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelSegmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.recipe.HcRecipeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.route.HcRouteMapper;
import cn.iocoder.yudao.module.mes.service.hc.modelrule.HcModelRuleService;
import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRODUCTMODEL_MODELCODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRODUCTMODEL_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRODUCTMODEL_REFERENCED_DELETE_DENIED;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCPRODUCTMODEL_REFERENCED_LOCKED;

@Service
@Validated
public class HcProductModelServiceImpl implements HcProductModelService {

    private static final String MODEL_LEVEL_FAMILY = "FAMILY";
    private static final String MODEL_LEVEL_MODEL = "MODEL";
    private static final String STATUS_ENABLE = "ENABLE";
    private static final String RETENTION_UNIT_DAY = "DAY";
    private static final String RETENTION_UNIT_MONTH = "MONTH";

    @Resource
    private HcProductModelMapper productModelMapper;
    @Resource
    private HcProductModelSegmentMapper productModelSegmentMapper;
    @Resource
    private HcProductModelMaterialMapper productModelMaterialMapper;
    @Resource
    private HcModelRuleService modelRuleService;
    @Resource
    private HcMaterialMapper materialMapper;
    @Resource
    private HcPlanOrderMapper planOrderMapper;
    @Resource
    private HcRecipeMapper recipeMapper;
    @Resource
    private HcRouteMapper hcRouteMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcProductModel(HcProductModelSaveReqVO createReqVO) {
        validateModelCodeUnique(null, createReqVO.getModelCode());
        HcModelRuleDO rule = validateAndGetModelRule(createReqVO.getModelRuleId());
        HcProductModelDO entity = BeanUtils.toBean(createReqVO, HcProductModelDO.class);
        validateAndNormalizeHierarchy(entity);
        normalizeRetentionPeriod(entity);
        fillReferenceSnapshot(entity);
        fillRuleSnapshot(entity, rule);
        entity.setStatus(StrUtil.blankToDefault(entity.getStatus(), "ENABLE"));
        entity.setSourceType(StrUtil.blankToDefault(entity.getSourceType(), "RULE_GENERATED"));
        entity.setReferencedFlag(false);
        entity.setSegmentSnapshotJson(buildSegmentSnapshotJson(createReqVO.getModelSegments()));
        productModelMapper.insert(entity);
        createSegmentList(entity, createReqVO.getModelSegments());
        createMaterialList(entity, createReqVO.getModelMaterials());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcProductModel(HcProductModelSaveReqVO updateReqVO) {
        HcProductModelDO db = validateExists(updateReqVO.getId());
        validateModelCodeUnique(updateReqVO.getId(), updateReqVO.getModelCode());
        boolean referenced = isReferenced(db);
        if (referenced && isLockedFieldChanged(db, updateReqVO)) {
            throw exception(HCPRODUCTMODEL_REFERENCED_LOCKED);
        }
        HcModelRuleDO rule = validateAndGetModelRule(updateReqVO.getModelRuleId());
        HcProductModelDO updateObj = BeanUtils.toBean(updateReqVO, HcProductModelDO.class);
        validateAndNormalizeHierarchy(updateObj);
        normalizeRetentionPeriod(updateObj);
        fillReferenceSnapshot(updateObj);
        fillRuleSnapshot(updateObj, rule);
        updateObj.setReferencedFlag(referenced);
        updateObj.setSegmentSnapshotJson(buildSegmentSnapshotJson(updateReqVO.getModelSegments()));
        productModelMapper.updateById(updateObj);
        productModelSegmentMapper.deleteByModelId(updateReqVO.getId());
        productModelMaterialMapper.deleteByModelId(updateReqVO.getId());
        createSegmentList(updateObj, updateReqVO.getModelSegments());
        createMaterialList(updateObj, updateReqVO.getModelMaterials());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcProductModel(Long id) {
        HcProductModelDO entity = validateExists(id);
        if (isReferenced(entity)) {
            throw exception(HCPRODUCTMODEL_REFERENCED_DELETE_DENIED);
        }
        productModelMapper.deleteById(id);
        productModelSegmentMapper.deleteByModelId(id);
        productModelMaterialMapper.deleteByModelId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcProductModelListByIds(List<Long> ids) {
        for (Long id : ids) {
            deleteHcProductModel(id);
        }
    }

    @Override
    public HcProductModelDO getHcProductModel(Long id) {
        return fillHierarchyDisplay(fillReferencedFlag(productModelMapper.selectById(id)));
    }

    @Override
    public HcProductModelDO getHcProductModelByCode(String modelCode) {
        if (StrUtil.isBlank(modelCode)) {
            return null;
        }
        return fillHierarchyDisplay(productModelMapper.selectByModelCode(modelCode.trim()));
    }

    @Override
    public PageResult<HcProductModelDO> getHcProductModelPage(HcProductModelPageReqVO pageReqVO) {
        PageResult<HcProductModelDO> pageResult = productModelMapper.selectPage(pageReqVO);
        pageResult.getList().forEach(item -> fillHierarchyDisplay(fillReferencedFlag(item)));
        return pageResult;
    }

    @Override
    public List<HcProductModelDO> getHcProductModelList(HcProductModelPageReqVO reqVO) {
        List<HcProductModelDO> list = productModelMapper.selectList(reqVO);
        list.forEach(item -> fillHierarchyDisplay(fillReferencedFlag(item)));
        return list;
    }

    @Override
    public List<HcProductModelSegmentDO> getSegmentListByModelId(Long modelId) {
        return productModelSegmentMapper.selectListByModelId(modelId);
    }

    @Override
    public List<HcProductModelMaterialDO> getMaterialListByModelId(Long modelId) {
        return productModelMaterialMapper.selectListByModelId(modelId);
    }

    @Override
    public String generateModelCode(HcProductModelGenerateReqVO reqVO) {
        HcModelRuleDO rule = validateAndGetModelRule(reqVO.getModelRuleId());
        HcModelRuleGenerateReqVO generateReqVO = new HcModelRuleGenerateReqVO();
        generateReqVO.setModelRuleItems(modelRuleService.getHcModelRuleItemListByParentId(rule.getId()));
        generateReqVO.setModelRuleDicts(modelRuleService.getHcModelRuleDictListByParentId(rule.getId()));
        generateReqVO.setTestValues(reqVO.getSegmentValues());
        return modelRuleService.generateModelRuleCode(generateReqVO);
    }

    @Override
    public List<HcProductModelSelectOptionRespVO> getSelectOptions(String keyword, String modelLevel) {
        LambdaQueryWrapperX<HcProductModelDO> query = new LambdaQueryWrapperX<>();
        query.eq(HcProductModelDO::getStatus, STATUS_ENABLE);
        query.eqIfPresent(HcProductModelDO::getModelLevel, normalizeOptionalModelLevel(modelLevel));
        if (StrUtil.isNotBlank(keyword)) {
            query.and(wrapper -> wrapper.like(HcProductModelDO::getModelCode, keyword)
                    .or()
                    .like(HcProductModelDO::getModelName, keyword));
        }
        query.orderByAsc(HcProductModelDO::getModelCode);
        query.orderByDesc(HcProductModelDO::getId);
        query.last("LIMIT 200");
        return productModelMapper.selectList(query).stream().map(item -> {
            fillHierarchyDisplay(item);
            HcProductModelSelectOptionRespVO option = BeanUtils.toBean(item, HcProductModelSelectOptionRespVO.class);
            option.setValue(item.getId());
            option.setLabel(MODEL_LEVEL_FAMILY.equals(item.getModelLevel())
                    ? item.getModelCode() + "（系列）"
                    : item.getModelCode());
            option.setCode(item.getModelCode());
            return option;
        }).toList();
    }

    private String normalizeOptionalModelLevel(String modelLevel) {
        if (StrUtil.isBlank(modelLevel)) {
            return null;
        }
        String normalized = modelLevel.trim().toUpperCase(Locale.ROOT);
        if (!MODEL_LEVEL_FAMILY.equals(normalized) && !MODEL_LEVEL_MODEL.equals(normalized)) {
            throw invalidParamException("型号层级仅支持 FAMILY 或 MODEL");
        }
        return normalized;
    }

    private void validateAndNormalizeHierarchy(HcProductModelDO entity) {
        String modelLevel = normalizeOptionalModelLevel(entity.getModelLevel());
        entity.setModelLevel(StrUtil.blankToDefault(modelLevel, MODEL_LEVEL_MODEL));
        if (MODEL_LEVEL_FAMILY.equals(entity.getModelLevel())) {
            entity.setParentModelId(null);
            return;
        }
        if (entity.getParentModelId() == null) {
            return;
        }
        if (Objects.equals(entity.getId(), entity.getParentModelId())) {
            throw invalidParamException("产品型号不能将自己设置为所属系列");
        }
        HcProductModelDO parent = productModelMapper.selectById(entity.getParentModelId());
        if (parent == null) {
            throw invalidParamException("所属系列型号不存在");
        }
        if (!MODEL_LEVEL_FAMILY.equals(parent.getModelLevel())) {
            throw invalidParamException("所属系列必须选择系列型号");
        }
        if (!STATUS_ENABLE.equals(parent.getStatus())) {
            throw invalidParamException("所属系列型号未启用");
        }
    }

    private HcProductModelDO fillHierarchyDisplay(HcProductModelDO entity) {
        if (entity == null) {
            return null;
        }
        entity.setModelLevel(StrUtil.blankToDefault(entity.getModelLevel(), MODEL_LEVEL_MODEL));
        if (entity.getParentModelId() == null) {
            entity.setParentModelCode(null);
            entity.setParentModelName(null);
            return entity;
        }
        HcProductModelDO parent = productModelMapper.selectById(entity.getParentModelId());
        if (parent != null) {
            entity.setParentModelCode(parent.getModelCode());
            entity.setParentModelName(parent.getModelName());
        }
        return entity;
    }

    private void normalizeRetentionPeriod(HcProductModelDO entity) {
        Integer periodValue = entity.getRetentionPeriodValue();
        String periodUnit = normalizeRetentionPeriodUnit(entity.getRetentionPeriodUnit());
        if (periodValue == null && StrUtil.isBlank(periodUnit)) {
            entity.setRetentionPeriodUnit(null);
            return;
        }
        if (periodValue == null || periodValue <= 0) {
            throw invalidParamException("留样时长必须大于 0");
        }
        if (StrUtil.isBlank(periodUnit)) {
            throw invalidParamException("请选择留样单位");
        }
        entity.setRetentionPeriodUnit(periodUnit);
    }

    private String normalizeRetentionPeriodUnit(String periodUnit) {
        if (StrUtil.isBlank(periodUnit)) {
            return "";
        }
        String text = periodUnit.trim();
        String normalized = text.toUpperCase(Locale.ROOT);
        if ("天".equals(text) || "DAYS".equals(normalized) || RETENTION_UNIT_DAY.equals(normalized)) {
            return RETENTION_UNIT_DAY;
        }
        if ("月".equals(text) || "MONTHS".equals(normalized) || RETENTION_UNIT_MONTH.equals(normalized)) {
            return RETENTION_UNIT_MONTH;
        }
        throw invalidParamException("留样单位仅支持天或月");
    }

    private HcProductModelDO validateExists(Long id) {
        HcProductModelDO entity = productModelMapper.selectById(id);
        if (entity == null) {
            throw exception(HCPRODUCTMODEL_NOT_EXISTS);
        }
        return entity;
    }

    private HcModelRuleDO validateAndGetModelRule(Long id) {
        HcModelRuleDO rule = modelRuleService.getHcModelRule(id);
        if (rule == null) {
            throw exception(HCMODELRULE_NOT_EXISTS);
        }
        return rule;
    }

    private void validateModelCodeUnique(Long id, String modelCode) {
        HcProductModelDO entity = productModelMapper.selectOne(new LambdaQueryWrapperX<HcProductModelDO>()
                .eq(HcProductModelDO::getModelCode, modelCode)
                .neIfPresent(HcProductModelDO::getId, id));
        if (entity != null) {
            throw exception(HCPRODUCTMODEL_MODELCODE_EXISTS);
        }
    }

    private void fillRuleSnapshot(HcProductModelDO entity, HcModelRuleDO rule) {
        entity.setModelRuleId(rule.getId());
        entity.setModelRuleCode(rule.getRuleCode());
        entity.setModelRuleName(rule.getRuleName());
    }

    private void fillReferenceSnapshot(HcProductModelDO entity) {
        if (entity.getRecipeId() != null) {
            HcRecipeDO recipe = recipeMapper.selectById(entity.getRecipeId());
            if (recipe != null) {
                entity.setRecipeCode(recipe.getRecipeCode());
                entity.setRecipeName(recipe.getRecipeName());
            }
        } else {
            entity.setRecipeCode(null);
            entity.setRecipeName(null);
        }
        if (entity.getDefaultRouteId() != null) {
            HcRouteDO route = hcRouteMapper.selectById(entity.getDefaultRouteId());
            if (route != null) {
                entity.setDefaultRouteCode(route.getRouteCode());
                entity.setDefaultRouteName(route.getRouteName());
            }
        } else {
            entity.setDefaultRouteCode(null);
            entity.setDefaultRouteName(null);
        }
    }

    private String buildSegmentSnapshotJson(List<HcProductModelSegmentReqVO> segments) {
        if (segments == null || segments.isEmpty()) {
            return null;
        }
        return JsonUtils.toJsonString(segments);
    }

    private void createSegmentList(HcProductModelDO parent, List<HcProductModelSegmentReqVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<HcProductModelSegmentDO> rows = list.stream()
                .filter(item -> StrUtil.isNotBlank(item.getItemCode()))
                .map(item -> {
                    HcProductModelSegmentDO row = BeanUtils.toBean(item, HcProductModelSegmentDO.class);
                    row.setId(null);
                    row.setModelId(parent.getId());
                    row.setModelCode(parent.getModelCode());
                    row.setRuleId(parent.getModelRuleId());
                    row.setRuleCode(parent.getModelRuleCode());
                    return row;
                }).toList();
        if (!rows.isEmpty()) {
            productModelSegmentMapper.insertBatch(rows);
        }
    }

    private void createMaterialList(HcProductModelDO parent, List<HcProductModelMaterialReqVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<HcProductModelMaterialDO> rows = list.stream()
                .filter(item -> item.getMaterialId() != null)
                .map(item -> {
                    HcProductModelMaterialDO row = BeanUtils.toBean(item, HcProductModelMaterialDO.class);
                    row.setId(null);
                    row.setModelId(parent.getId());
                    row.setModelCode(parent.getModelCode());
                    row.setIsDefault(Boolean.TRUE.equals(row.getIsDefault()));
                    return row;
                }).toList();
        if (!rows.isEmpty()) {
            productModelMaterialMapper.insertBatch(rows);
        }
    }

    private boolean isReferenced(HcProductModelDO entity) {
        Long materialCount = materialMapper.selectCount(new LambdaQueryWrapperX<HcMaterialDO>()
                .eq(HcMaterialDO::getProductModelId, entity.getId()));
        Long planCount = planOrderMapper.selectCount(new LambdaQueryWrapperX<HcPlanOrderDO>()
                .and(wrapper -> wrapper.eq(HcPlanOrderDO::getModelId, entity.getId())
                        .or()
                        .eq(HcPlanOrderDO::getModelCode, entity.getModelCode())));
        return materialCount > 0 || planCount > 0;
    }

    private HcProductModelDO fillReferencedFlag(HcProductModelDO entity) {
        if (entity == null || entity.getId() == null) {
            return entity;
        }
        entity.setReferencedFlag(isReferenced(entity));
        return entity;
    }

    private boolean isLockedFieldChanged(HcProductModelDO db, HcProductModelSaveReqVO reqVO) {
        return !Objects.equals(db.getModelCode(), reqVO.getModelCode())
                || !Objects.equals(db.getModelName(), reqVO.getModelName())
                || !Objects.equals(db.getModelAlias(), reqVO.getModelAlias())
                || !Objects.equals(StrUtil.blankToDefault(db.getModelLevel(), MODEL_LEVEL_MODEL),
                StrUtil.blankToDefault(normalizeOptionalModelLevel(reqVO.getModelLevel()), MODEL_LEVEL_MODEL))
                || !Objects.equals(db.getParentModelId(), reqVO.getParentModelId())
                || !Objects.equals(db.getModelRuleId(), reqVO.getModelRuleId())
                || !Objects.equals(db.getProdType(), reqVO.getProdType())
                || !Objects.equals(db.getCategoryCode(), reqVO.getCategoryCode())
                || !Objects.equals(db.getRecipeId(), reqVO.getRecipeId())
                || !Objects.equals(db.getSizeSpec(), reqVO.getSizeSpec())
                || !Objects.equals(db.getDefaultRouteId(), reqVO.getDefaultRouteId())
                || !Objects.equals(db.getSourceType(), reqVO.getSourceType())
                || !Objects.equals(db.getStatus(), reqVO.getStatus())
                || !Objects.equals(db.getRemark(), reqVO.getRemark())
                || !Objects.equals(db.getSegmentSnapshotJson(), buildSegmentSnapshotJson(reqVO.getModelSegments()))
                || !Objects.equals(buildMaterialSnapshotJson(getMaterialListByModelId(db.getId())),
                buildMaterialSnapshotJsonReq(reqVO.getModelMaterials()));
    }

    private String buildMaterialSnapshotJson(List<HcProductModelMaterialDO> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<String> snapshot = new ArrayList<>(list.stream()
                .map(item -> String.join("|",
                        String.valueOf(item.getMaterialId()),
                        StrUtil.blankToDefault(item.getMaterialCode(), ""),
                        StrUtil.blankToDefault(item.getMaterialName(), ""),
                        String.valueOf(Boolean.TRUE.equals(item.getIsDefault()))))
                .sorted()
                .toList());
        return JsonUtils.toJsonString(snapshot);
    }

    private String buildMaterialSnapshotJsonReq(List<HcProductModelMaterialReqVO> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        List<String> snapshot = new ArrayList<>(list.stream()
                .filter(item -> item.getMaterialId() != null || StrUtil.isNotBlank(item.getMaterialCode()))
                .map(item -> String.join("|",
                        String.valueOf(item.getMaterialId()),
                        StrUtil.blankToDefault(item.getMaterialCode(), ""),
                        StrUtil.blankToDefault(item.getMaterialName(), ""),
                        String.valueOf(Boolean.TRUE.equals(item.getIsDefault()))))
                .sorted(Comparator.naturalOrder())
                .toList());
        return JsonUtils.toJsonString(snapshot);
    }

}
