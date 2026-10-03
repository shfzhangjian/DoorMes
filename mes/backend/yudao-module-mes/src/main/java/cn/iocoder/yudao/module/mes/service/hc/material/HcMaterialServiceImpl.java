package cn.iocoder.yudao.module.mes.service.hc.material;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialBindingPreviewBomItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialBindingPreviewBomRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialBindingPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialExtAttrDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.route.HcRouteDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialExtAttrMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.material.HcMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.recipe.HcRecipeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.route.HcRouteMapper;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleService;
import cn.iocoder.yudao.module.mes.service.hc.modelrule.HcModelRuleService;
import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMATERIAL_MATERIALCODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMATERIAL_NOT_EXISTS;

@Service
@Validated
public class HcMaterialServiceImpl implements HcMaterialService {

    @Resource
    private HcMaterialMapper hcMaterialMapper;

    @Resource
    private HcMaterialExtAttrMapper hcMaterialExtAttrMapper;

    @Resource
    private HcRouteMapper hcRouteMapper;

    @Resource
    private HcRecipeMapper hcRecipeMapper;

    @Resource
    private HcBomMapper hcBomMapper;

    @Resource
    private HcBomItemMapper hcBomItemMapper;

    @Resource
    private HcLotRuleService hcLotRuleService;

    @Resource
    private HcModelRuleService hcModelRuleService;

    @Resource
    private HcProductModelMapper hcProductModelMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcMaterial(HcMaterialSaveReqVO createReqVO) {
        HcMaterialDO entity = BeanUtils.toBean(createReqVO, HcMaterialDO.class);
        normalizeMesSelectVisible(entity);
        normalizeUnitSnapshot(entity);
        fillMaterialCode(entity, createReqVO.getMaterialExtAttrs(), true);
        validateMaterialCodeUnique(null, entity.getMaterialCode());
        fillModelInfo(entity, createReqVO.getMaterialExtAttrs());
        hcMaterialMapper.insert(entity);
        fillDefaultRefFields(entity);
        hcMaterialMapper.updateById(entity);
        createHcMaterialExtAttrList(entity.getId(), entity.getMaterialCode(), createReqVO.getMaterialExtAttrs());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcMaterial(HcMaterialSaveReqVO updateReqVO) {
        validateHcMaterialExists(updateReqVO.getId());
        HcMaterialDO updateObj = BeanUtils.toBean(updateReqVO, HcMaterialDO.class);
        normalizeMesSelectVisible(updateObj);
        normalizeUnitSnapshot(updateObj);
        fillMaterialCode(updateObj, updateReqVO.getMaterialExtAttrs(), false);
        validateMaterialCodeUnique(updateReqVO.getId(), updateObj.getMaterialCode());
        fillModelInfo(updateObj, updateReqVO.getMaterialExtAttrs());
        fillDefaultRefFields(updateObj);
        hcMaterialMapper.updateById(updateObj);
        updateHcMaterialExtAttrList(updateReqVO.getId(), updateObj.getMaterialCode(), updateReqVO.getMaterialExtAttrs());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcMaterial(Long id) {
        validateHcMaterialExists(id);
        hcMaterialMapper.deleteById(id);
        hcMaterialExtAttrMapper.deleteByParentId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcMaterialListByIds(List<Long> ids) {
        hcMaterialMapper.deleteByIds(ids);
        hcMaterialExtAttrMapper.deleteByParentIds(ids);
    }

    @Override
    public String generateMaterialCode(HcMaterialSaveReqVO reqVO) {
        HcMaterialDO entity = BeanUtils.toBean(reqVO, HcMaterialDO.class);
        fillMaterialCode(entity, reqVO.getMaterialExtAttrs(), false);
        return entity.getMaterialCode();
    }

    private void validateHcMaterialExists(Long id) {
        if (hcMaterialMapper.selectById(id) == null) {
            throw exception(HCMATERIAL_NOT_EXISTS);
        }
    }

    private void normalizeMesSelectVisible(HcMaterialDO entity) {
        if (entity.getMesSelectVisible() == null) {
            entity.setMesSelectVisible(false);
        }
    }

    private void normalizeUnitSnapshot(HcMaterialDO entity) {
        if (!hasText(entity.getBaseUom())) {
            entity.setBaseUom(entity.getBaseUnitCode());
        }
        if (!hasText(entity.getStockUom())) {
            entity.setStockUom(entity.getStockUnitCode());
        }
        if (!hasText(entity.getProduceUom())) {
            entity.setProduceUom(entity.getProduceUnitCode());
        }
    }

    private void validateMaterialCodeUnique(Long id, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        HcMaterialDO entity = hcMaterialMapper.selectOne(new LambdaQueryWrapperX<HcMaterialDO>()
                .eq(HcMaterialDO::getMaterialCode, value)
                .neIfPresent(HcMaterialDO::getId, id));
        if (entity != null) {
            throw exception(HCMATERIAL_MATERIALCODE_EXISTS);
        }
    }

    private void fillMaterialCode(HcMaterialDO entity, List<HcMaterialExtAttrDO> extAttrs, boolean consumeSequence) {
        if (hasText(entity.getMaterialCode())) {
            entity.setMaterialCode(entity.getMaterialCode().trim());
            return;
        }
        if (entity.getMaterialCodeRuleId() == null) {
            throw invalidParamException("物料编码为空时必须选择物料编号规则");
        }
        HcLotRuleGenerateReqVO reqVO = new HcLotRuleGenerateReqVO();
        reqVO.setRuleId(entity.getMaterialCodeRuleId());
        reqVO.setConsumeSequence(consumeSequence);
        reqVO.setInputValues(buildMaterialCodeInputMap(entity, extAttrs));
        Object lotNo = hcLotRuleService.generateLotNo(reqVO).get("lotNo");
        if (lotNo == null || lotNo.toString().isBlank()) {
            throw invalidParamException("物料编号规则未生成有效的物料编码");
        }
        entity.setMaterialCode(lotNo.toString());
    }

    private Map<String, String> buildMaterialCodeInputMap(HcMaterialDO entity, List<HcMaterialExtAttrDO> extAttrs) {
        Map<String, String> inputMap = new LinkedHashMap<>();
        putIfHasText(inputMap, "productLevel", entity.getProductLevel());
        putIfHasText(inputMap, "product_level", entity.getProductLevel());
        putIfHasText(inputMap, "materialType", entity.getMaterialType());
        putIfHasText(inputMap, "material_type", entity.getMaterialType());
        putIfHasText(inputMap, "materialCategoryId", entity.getMaterialCategoryId());
        putIfHasText(inputMap, "material_category_id", entity.getMaterialCategoryId());
        putIfHasText(inputMap, "specModel", entity.getSpecModel());
        putIfHasText(inputMap, "spec_model", entity.getSpecModel());
        putIfHasText(inputMap, "modelCode", entity.getModelCode());
        putIfHasText(inputMap, "model_code", entity.getModelCode());
        putIfHasText(inputMap, "baseUom", entity.getBaseUom());
        putIfHasText(inputMap, "base_uom", entity.getBaseUom());
        if (extAttrs != null) {
            extAttrs.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> hasText(item.getAttrCode()))
                    .forEach(item -> inputMap.put(item.getAttrCode(), defaultString(item.getAttrValue())));
        }
        return inputMap;
    }

    private void putIfHasText(Map<String, String> inputMap, String key, Object value) {
        if (value == null) {
            return;
        }
        String text = String.valueOf(value).trim();
        if (!text.isEmpty()) {
            inputMap.put(key, text);
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private void fillDefaultRefFields(HcMaterialDO entity) {
        HcRouteDO route = entity.getDefaultRouteId() == null ? null : hcRouteMapper.selectById(entity.getDefaultRouteId());
        entity.setDefaultRouteCode(route == null ? null : route.getRouteCode());
        entity.setDefaultRouteName(route == null ? null : route.getRouteName());

        HcRecipeDO recipe = entity.getDefaultRecipeId() == null ? null : hcRecipeMapper.selectById(entity.getDefaultRecipeId());
        entity.setDefaultRecipeCode(recipe == null ? null : recipe.getRecipeCode());
        entity.setDefaultRecipeName(recipe == null ? null : recipe.getModelCode());

        HcBomDO bom = entity.getDefaultBomId() == null ? null : hcBomMapper.selectById(entity.getDefaultBomId());
        entity.setDefaultBomCode(bom == null ? null : bom.getBomCode());
        entity.setDefaultBomName(bom == null ? null : bom.getBomName());
    }

    private void fillModelInfo(HcMaterialDO entity, List<HcMaterialExtAttrDO> extAttrs) {
        if (entity.getProductModelId() != null) {
            HcProductModelDO productModel = hcProductModelMapper.selectById(entity.getProductModelId());
            if (productModel == null) {
                throw invalidParamException("产品型号不存在");
            }
            entity.setProductModelName(productModel.getModelName());
            entity.setModelCodeRuleId(productModel.getModelRuleId());
            entity.setModelCode(productModel.getModelCode());
            entity.setModelSegmentsJson(productModel.getSegmentSnapshotJson());
            entity.setDefaultRecipeId(productModel.getRecipeId());
            entity.setDefaultRecipeCode(productModel.getRecipeCode());
            entity.setDefaultRecipeName(productModel.getRecipeName());
            if (entity.getDefaultRouteId() == null) {
                entity.setDefaultRouteId(productModel.getDefaultRouteId());
                entity.setDefaultRouteCode(productModel.getDefaultRouteCode());
                entity.setDefaultRouteName(productModel.getDefaultRouteName());
            }
            return;
        }
        if (entity.getModelCodeRuleId() == null) {
            entity.setModelSegmentsJson(null);
            if (!hasText(entity.getModelCode())) {
                entity.setModelCode(null);
            }
            return;
        }
        List<HcModelRuleItemDO> items = hcModelRuleService.getHcModelRuleItemListByParentId(entity.getModelCodeRuleId());
        if (items == null || items.isEmpty()) {
            entity.setModelSegmentsJson(null);
            if (!hasText(entity.getModelCode())) {
                entity.setModelCode(null);
            }
            return;
        }
        List<HcModelRuleDictDO> dicts = hcModelRuleService.getHcModelRuleDictListByParentId(entity.getModelCodeRuleId());
        Map<String, String> inputMap = buildInputMap(entity, extAttrs);
        if (!hasText(entity.getModelCode())) {
            HcModelRuleGenerateReqVO reqVO = new HcModelRuleGenerateReqVO();
            reqVO.setModelRuleItems(items);
            reqVO.setModelRuleDicts(dicts);
            reqVO.setTestValues(inputMap);
            entity.setModelCode(hcModelRuleService.generateModelRuleCode(reqVO));
        }
        entity.setModelSegmentsJson(buildModelSegmentsJson(items, dicts, inputMap));
        if (entity.getDefaultRecipeId() == null) {
            HcRecipeDO suggestedRecipe = resolveSuggestedDefaultRecipe(items, dicts, inputMap);
            if (suggestedRecipe != null) {
                entity.setDefaultRecipeId(suggestedRecipe.getId());
            }
        }
        if (entity.getDefaultRouteId() == null) {
            HcRouteDO suggestedRoute = resolveSuggestedDefaultRoute(items, dicts, inputMap);
            if (suggestedRoute != null) {
                entity.setDefaultRouteId(suggestedRoute.getId());
            }
        }
    }

    private Map<String, String> buildInputMap(HcMaterialDO material, List<HcMaterialExtAttrDO> extAttrs) {
        Map<String, String> inputMap = new LinkedHashMap<>();
        if (extAttrs != null) {
            extAttrs.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> hasText(item.getAttrCode()))
                    .forEach(item -> inputMap.put(item.getAttrCode(), item.getAttrValue()));
        }
        if (material != null && hasText(material.getDefaultRecipeName())) {
            String recipeModelCode = material.getDefaultRecipeName().trim();
            inputMap.put("formula", recipeModelCode);
            inputMap.put("recipeModelCode", recipeModelCode);
            inputMap.put("recipe_model_code", recipeModelCode);
        }
        return inputMap;
    }

    private HcRecipeDO resolveSuggestedDefaultRecipe(List<HcModelRuleItemDO> items, List<HcModelRuleDictDO> dicts,
                                                     Map<String, String> inputMap) {
        Map<String, List<HcModelRuleDictDO>> dictMap = buildDictMap(dicts);
        return items.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .map(item -> resolveSuggestedRecipeCode(item, inputMap, dictMap.get(item.getItemCode())))
                .filter(this::hasText)
                .map(this::getRecipeByCode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private HcRouteDO resolveSuggestedDefaultRoute(List<HcModelRuleItemDO> items, List<HcModelRuleDictDO> dicts,
                                                   Map<String, String> inputMap) {
        Map<String, List<HcModelRuleDictDO>> dictMap = buildDictMap(dicts);
        return items.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .map(item -> resolveSuggestedRouteCode(item, inputMap, dictMap.get(item.getItemCode())))
                .filter(this::hasText)
                .map(this::getRouteByCode)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String resolveSuggestedRecipeCode(HcModelRuleItemDO item, Map<String, String> inputMap,
                                              List<HcModelRuleDictDO> dictOptions) {
        ModelSegmentSnapshot snapshot = buildModelSegmentSnapshot(item, inputMap, dictOptions);
        if (snapshot == null || !hasText(snapshot.getSegmentValue())) {
            return null;
        }
        if (dictOptions != null) {
            for (HcModelRuleDictDO dict : dictOptions) {
                if (dict == null) {
                    continue;
                }
                if (!Objects.equals(snapshot.getDictId(), dict.getId())
                        && !snapshot.getSegmentValue().equals(dict.getDictCode())
                        && !snapshot.getSegmentValue().equals(dict.getDictValue())) {
                    continue;
                }
                String recipeCode = extractExtString(dict.getExtAttrJson(), "recipeCode");
                if (hasText(recipeCode)) {
                    return recipeCode;
                }
            }
        }
        if ("formula".equals(item.getItemCode())) {
            return snapshot.getSegmentValue();
        }
        return null;
    }

    private String resolveSuggestedRouteCode(HcModelRuleItemDO item, Map<String, String> inputMap,
                                             List<HcModelRuleDictDO> dictOptions) {
        ModelSegmentSnapshot snapshot = buildModelSegmentSnapshot(item, inputMap, dictOptions);
        if (snapshot == null || !hasText(snapshot.getSegmentValue()) || dictOptions == null) {
            return null;
        }
        for (HcModelRuleDictDO dict : dictOptions) {
            if (dict == null) {
                continue;
            }
            if (!Objects.equals(snapshot.getDictId(), dict.getId())
                    && !snapshot.getSegmentValue().equals(dict.getDictCode())
                    && !snapshot.getSegmentValue().equals(dict.getDictValue())) {
                continue;
            }
            String routeCode = extractExtString(dict.getExtAttrJson(), "routeBranchCode");
            if (hasText(routeCode)) {
                return routeCode;
            }
        }
        return null;
    }

    private String extractExtString(String extAttrJson, String key) {
        if (!hasText(extAttrJson) || !hasText(key)) {
            return null;
        }
        Map<String, Object> extMap = JsonUtils.parseObjectQuietly(extAttrJson, new TypeReference<Map<String, Object>>() {});
        if (extMap == null) {
            return null;
        }
        Object value = extMap.get(key);
        return value == null ? null : String.valueOf(value).trim();
    }

    private HcRecipeDO getRecipeByCode(String recipeCode) {
        return hcRecipeMapper.selectOne(new LambdaQueryWrapperX<HcRecipeDO>()
                .eq(HcRecipeDO::getRecipeCode, recipeCode));
    }

    private HcRouteDO getRouteByCode(String routeCode) {
        return hcRouteMapper.selectOne(new LambdaQueryWrapperX<HcRouteDO>()
                .eq(HcRouteDO::getRouteCode, routeCode)
                .eq(HcRouteDO::getStatus, 1));
    }

    private Map<String, List<HcModelRuleDictDO>> buildDictMap(List<HcModelRuleDictDO> dicts) {
        return (dicts == null ? List.<HcModelRuleDictDO>of() : dicts).stream()
                .filter(Objects::nonNull)
                .filter(dict -> hasText(dict.getItemCode()))
                .collect(Collectors.groupingBy(HcModelRuleDictDO::getItemCode, LinkedHashMap::new, Collectors.toList()));
    }


    private List<String> resolveConsumeGroupCodes(HcMaterialDO material, Long ruleId, List<HcMaterialExtAttrDO> extAttrs) {
        if (ruleId == null) {
            return List.of();
        }
        List<HcModelRuleItemDO> items = hcModelRuleService.getHcModelRuleItemListByParentId(ruleId);
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        List<HcModelRuleDictDO> dicts = hcModelRuleService.getHcModelRuleDictListByParentId(ruleId);
        Map<String, List<HcModelRuleDictDO>> dictMap = buildDictMap(dicts);
        Map<String, String> inputMap = buildInputMap(material, extAttrs);
        return items.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .map(item -> resolveConsumeGroupCode(item, inputMap, dictMap.get(item.getItemCode())))
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private String resolveConsumeGroupCode(HcModelRuleItemDO item, Map<String, String> inputMap,
                                           List<HcModelRuleDictDO> dictOptions) {
        ModelSegmentSnapshot snapshot = buildModelSegmentSnapshot(item, inputMap, dictOptions);
        if (snapshot == null || !hasText(snapshot.getSegmentValue()) || dictOptions == null) {
            return null;
        }
        for (HcModelRuleDictDO dict : dictOptions) {
            if (dict == null) {
                continue;
            }
            if (!Objects.equals(snapshot.getDictId(), dict.getId())
                    && !snapshot.getSegmentValue().equals(dict.getDictCode())
                    && !snapshot.getSegmentValue().equals(dict.getDictValue())) {
                continue;
            }
            String consumeGroupCode = extractExtString(dict.getExtAttrJson(), "consumeGroupCode");
            if (hasText(consumeGroupCode)) {
                return consumeGroupCode;
            }
        }
        return null;
    }

    private Map<String, String> buildSegmentValueMap(HcMaterialDO material, Long ruleId, List<HcMaterialExtAttrDO> extAttrs) {
        if (ruleId == null) {
            return Map.of();
        }
        List<HcModelRuleItemDO> items = hcModelRuleService.getHcModelRuleItemListByParentId(ruleId);
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        List<HcModelRuleDictDO> dicts = hcModelRuleService.getHcModelRuleDictListByParentId(ruleId);
        Map<String, List<HcModelRuleDictDO>> dictMap = buildDictMap(dicts);
        Map<String, String> inputMap = buildInputMap(material, extAttrs);
        Map<String, String> result = new LinkedHashMap<>();
        items.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .map(item -> buildModelSegmentSnapshot(item, inputMap, dictMap.get(item.getItemCode())))
                .filter(Objects::nonNull)
                .filter(snapshot -> hasText(snapshot.getSegmentValue()))
                .forEach(snapshot -> result.put(snapshot.getItemCode(), snapshot.getSegmentValue()));
        return result;
    }

    private List<HcMaterialBindingPreviewBomRespVO> resolveBomCandidates(HcMaterialDO material, Long routeId, String routeCode,
                                                                         List<String> consumeGroupCodes,
                                                                         Map<String, String> segmentValueMap) {
        Set<String> groupCodeSet = new HashSet<>(consumeGroupCodes == null ? List.of() : consumeGroupCodes);
        List<HcBomDO> boms = hcBomMapper.selectList(new LambdaQueryWrapperX<HcBomDO>()
                .eq(HcBomDO::getStatus, 1)
                .orderByAsc(HcBomDO::getBomCode)
                .orderByDesc(HcBomDO::getId));
        return boms.stream()
                .filter(bom -> isBomMaterialCompatible(bom, material.getId()))
                .filter(bom -> isBomRouteCompatible(bom, routeId, routeCode))
                .map(bom -> buildBomPreview(bom, material.getId(), routeId, routeCode, groupCodeSet, segmentValueMap))
                .toList();
    }

    private HcMaterialBindingPreviewBomRespVO buildBomPreview(HcBomDO bom, Long materialId, Long routeId, String routeCode,
                                                              Set<String> consumeGroupCodes,
                                                              Map<String, String> segmentValueMap) {
        boolean materialMatched = isBomMaterialCompatible(bom, materialId);
        boolean routeMatched = isBomRouteCompatible(bom, routeId, routeCode);
        List<HcMaterialBindingPreviewBomItemRespVO> itemVOs = hcBomItemMapper.selectListByParentId(bom.getId()).stream()
                .map(item -> buildBomItemPreview(item, consumeGroupCodes, segmentValueMap))
                .toList();
        int matchedItemCount = (int) itemVOs.stream().filter(item -> Boolean.TRUE.equals(item.getMatched())).count();

        HcMaterialBindingPreviewBomRespVO vo = new HcMaterialBindingPreviewBomRespVO();
        vo.setBomId(bom.getId());
        vo.setBomCode(bom.getBomCode());
        vo.setBomName(bom.getBomName());
        vo.setBomType(bom.getBomType());
        vo.setStatus(bom.getStatus());
        vo.setProductMaterialId(bom.getProductMaterialId());
        vo.setProductMaterialCode(bom.getProductMaterialCode());
        vo.setRouteId(bom.getRouteId());
        vo.setRouteCode(bom.getRouteCode());
        vo.setMaterialMatched(materialMatched);
        vo.setRouteMatched(routeMatched);
        vo.setMatchedItemCount(matchedItemCount);
        vo.setMatched(itemVOs.isEmpty() || matchedItemCount > 0);
        vo.setBomItems(itemVOs);
        return vo;
    }

    private HcMaterialBindingPreviewBomItemRespVO buildBomItemPreview(HcBomItemDO item, Set<String> consumeGroupCodes,
                                                                      Map<String, String> segmentValueMap) {
        boolean consumeGroupMatched = !hasText(item.getConsumeGroupCode()) || consumeGroupCodes.contains(item.getConsumeGroupCode());
        boolean conditionMatched = matchConditionJson(item.getConditionJson(), segmentValueMap);

        HcMaterialBindingPreviewBomItemRespVO vo = new HcMaterialBindingPreviewBomItemRespVO();
        vo.setBomItemId(item.getId());
        vo.setLineNo(item.getLineNo());
        vo.setIssueOperationCode(item.getIssueOperationCode());
        vo.setComponentMaterialId(item.getComponentMaterialId());
        vo.setComponentMaterialCode(item.getComponentMaterialCode());
        vo.setComponentMaterialName(item.getComponentMaterialName());
        vo.setBaseQty(item.getBaseQty());
        vo.setLossRate(item.getLossRate());
        vo.setUom(item.getUom());
        vo.setConsumeGroupCode(item.getConsumeGroupCode());
        vo.setConditionJson(item.getConditionJson());
        vo.setRequiredFlag(item.getRequiredFlag());
        vo.setConsumeGroupMatched(consumeGroupMatched);
        vo.setConditionMatched(conditionMatched);
        vo.setMatched(consumeGroupMatched && conditionMatched);
        return vo;
    }

    private boolean isBomMaterialCompatible(HcBomDO bom, Long materialId) {
        return bom.getProductMaterialId() == null || Objects.equals(bom.getProductMaterialId(), materialId);
    }

    private boolean isBomRouteCompatible(HcBomDO bom, Long routeId, String routeCode) {
        if (bom.getRouteId() == null && !hasText(bom.getRouteCode())) {
            return true;
        }
        if (routeId != null && Objects.equals(bom.getRouteId(), routeId)) {
            return true;
        }
        return hasText(routeCode) && routeCode.equals(bom.getRouteCode());
    }

    private boolean matchConditionJson(String conditionJson, Map<String, String> segmentValueMap) {
        if (!hasText(conditionJson)) {
            return true;
        }
        Map<String, Object> conditionMap = JsonUtils.parseObjectQuietly(conditionJson, new TypeReference<Map<String, Object>>() {});
        if (conditionMap == null || conditionMap.isEmpty()) {
            return false;
        }
        for (Map.Entry<String, Object> entry : conditionMap.entrySet()) {
            if (!matchConditionValue(segmentValueMap.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean matchConditionValue(String actualValue, Object expectedValue) {
        if (expectedValue == null) {
            return true;
        }
        if (expectedValue instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).anyMatch(candidate -> Objects.equals(candidate, actualValue));
        }
        if (expectedValue instanceof Map<?, ?> mapValue) {
            Object eqValue = mapValue.get("eq");
            if (eqValue != null) {
                return matchConditionValue(actualValue, eqValue);
            }
            Object inValue = mapValue.get("in");
            if (inValue != null) {
                return matchConditionValue(actualValue, inValue);
            }
            return false;
        }
        return Objects.equals(String.valueOf(expectedValue), actualValue);
    }
    private String buildModelSegmentsJson(List<HcModelRuleItemDO> items, List<HcModelRuleDictDO> dicts, Map<String, String> inputMap) {
        Map<String, List<HcModelRuleDictDO>> dictMap = buildDictMap(dicts);
        List<ModelSegmentSnapshot> snapshots = items.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .map(item -> buildModelSegmentSnapshot(item, inputMap, dictMap.get(item.getItemCode())))
                .filter(Objects::nonNull)
                .toList();
        return snapshots.isEmpty() ? null : JsonUtils.toJsonString(snapshots);
    }

    private ModelSegmentSnapshot buildModelSegmentSnapshot(HcModelRuleItemDO item, Map<String, String> inputMap,
                                                           List<HcModelRuleDictDO> dictOptions) {
        if (!hasText(item.getItemCode()) || "skip".equals(item.getParseType())) {
            return null;
        }
        String rawInput = inputMap.getOrDefault(item.getItemCode(), "");
        String value = "fixed".equals(item.getParseType()) ? defaultIfBlank(rawInput, item.getFixedValue()) : rawInput;
        if (!hasText(value) && !Boolean.TRUE.equals(item.getRequiredFlag())) {
            return null;
        }
        ModelSegmentSnapshot snapshot = new ModelSegmentSnapshot();
        snapshot.setItemCode(item.getItemCode());
        snapshot.setItemName(hasText(item.getItemName()) ? item.getItemName() : item.getItemCode());
        snapshot.setSort(item.getSort());
        snapshot.setSegmentValue(value);
        snapshot.setSegmentText(resolveSegmentText(value, dictOptions));
        snapshot.setDictId(resolveDictId(value, dictOptions));
        return snapshot;
    }

    private String resolveSegmentText(String value, List<HcModelRuleDictDO> dictOptions) {
        if (!hasText(value) || dictOptions == null || dictOptions.isEmpty()) {
            return value;
        }
        return dictOptions.stream()
                .filter(Objects::nonNull)
                .filter(dict -> value.equals(dict.getDictCode()) || value.equals(dict.getDictValue()))
                .map(dict -> hasText(dict.getDictValue()) ? dict.getDictValue() : value)
                .findFirst()
                .orElse(value);
    }

    private Long resolveDictId(String value, List<HcModelRuleDictDO> dictOptions) {
        if (!hasText(value) || dictOptions == null || dictOptions.isEmpty()) {
            return null;
        }
        return dictOptions.stream()
                .filter(Objects::nonNull)
                .filter(dict -> value.equals(dict.getDictCode()) || value.equals(dict.getDictValue()))
                .map(HcModelRuleDictDO::getId)
                .findFirst()
                .orElse(null);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return hasText(value) ? value : defaultValue;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public HcMaterialBindingPreviewRespVO getHcMaterialBindingPreview(Long id) {
        HcMaterialDO material = hcMaterialMapper.selectById(id);
        if (material == null) {
            throw exception(HCMATERIAL_NOT_EXISTS);
        }
        List<HcMaterialExtAttrDO> extAttrs = hcMaterialExtAttrMapper.selectListByParentId(id);
        return buildBindingPreview(material, extAttrs);
    }

    @Override
    public HcMaterialBindingPreviewRespVO previewHcMaterialBinding(HcMaterialSaveReqVO reqVO) {
        HcMaterialDO material = BeanUtils.toBean(reqVO, HcMaterialDO.class);
        List<HcMaterialExtAttrDO> extAttrs = reqVO.getMaterialExtAttrs();
        return buildBindingPreview(material, extAttrs);
    }

    private HcMaterialBindingPreviewRespVO buildBindingPreview(HcMaterialDO currentMaterial, List<HcMaterialExtAttrDO> extAttrs) {
        HcMaterialDO displayMaterial = BeanUtils.toBean(currentMaterial, HcMaterialDO.class);
        fillDefaultRefFields(displayMaterial);
        HcMaterialDO previewMaterial = BeanUtils.toBean(displayMaterial, HcMaterialDO.class);
        fillModelInfo(previewMaterial, extAttrs);
        fillDefaultRefFields(previewMaterial);

        HcMaterialBindingPreviewRespVO respVO = new HcMaterialBindingPreviewRespVO();
        respVO.setMaterialId(previewMaterial.getId());
        respVO.setMaterialCode(previewMaterial.getMaterialCode());
        respVO.setMaterialName(previewMaterial.getMaterialName());
        respVO.setModelCodeRuleId(previewMaterial.getModelCodeRuleId());
        respVO.setModelCode(previewMaterial.getModelCode());
        respVO.setModelSegmentsJson(previewMaterial.getModelSegmentsJson());
        respVO.setDefaultRecipeId(displayMaterial.getDefaultRecipeId());
        respVO.setDefaultRecipeCode(displayMaterial.getDefaultRecipeCode());
        respVO.setDefaultRecipeName(displayMaterial.getDefaultRecipeName());
        respVO.setSuggestedRecipeId(previewMaterial.getDefaultRecipeId());
        respVO.setSuggestedRecipeCode(previewMaterial.getDefaultRecipeCode());
        respVO.setSuggestedRecipeName(previewMaterial.getDefaultRecipeName());
        respVO.setDefaultRouteId(displayMaterial.getDefaultRouteId());
        respVO.setDefaultRouteCode(displayMaterial.getDefaultRouteCode());
        respVO.setDefaultRouteName(displayMaterial.getDefaultRouteName());
        respVO.setSuggestedRouteId(previewMaterial.getDefaultRouteId());
        respVO.setSuggestedRouteCode(previewMaterial.getDefaultRouteCode());
        respVO.setSuggestedRouteName(previewMaterial.getDefaultRouteName());
        List<String> consumeGroupCodes = resolveConsumeGroupCodes(previewMaterial, previewMaterial.getModelCodeRuleId(), extAttrs);
        respVO.setConsumeGroupCodes(consumeGroupCodes);
        respVO.setBomCandidates(resolveBomCandidates(previewMaterial, previewMaterial.getDefaultRouteId(),
                previewMaterial.getDefaultRouteCode(), consumeGroupCodes,
                buildSegmentValueMap(previewMaterial, previewMaterial.getModelCodeRuleId(), extAttrs)));
        return respVO;
    }
    public HcMaterialDO getHcMaterial(Long id) {
        return hcMaterialMapper.selectById(id);
    }

    @Override
    public List<HcMaterialDO> getHcMaterialSimpleList() {
        LambdaQueryWrapperX<HcMaterialDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcMaterialDO::getMaterialStatus, 1);
        queryWrapper.orderByAsc(HcMaterialDO::getMaterialCode);
        queryWrapper.orderByDesc(HcMaterialDO::getId);
        return hcMaterialMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcMaterialDO> getHcMaterialList(HcMaterialPageReqVO reqVO) {
        return hcMaterialMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcMaterialDO> getHcMaterialPage(HcMaterialPageReqVO pageReqVO) {
        return hcMaterialMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcMaterialExtAttrDO> getHcMaterialExtAttrListByParentId(Long parentId) {
        return hcMaterialExtAttrMapper.selectListByParentId(parentId);
    }

    private void createHcMaterialExtAttrList(Long parentId, String materialCode, List<HcMaterialExtAttrDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setMaterialId(parentId);
            item.setMaterialCode(materialCode);
        });
        hcMaterialExtAttrMapper.insertBatch(list);
    }

    private void updateHcMaterialExtAttrList(Long parentId, String materialCode, List<HcMaterialExtAttrDO> list) {
        List<HcMaterialExtAttrDO> dbList = hcMaterialExtAttrMapper.selectListByParentId(parentId);
        if (list == null) {
            list = List.of();
        }

        Set<Long> reqIds = list.stream()
                .map(HcMaterialExtAttrDO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcMaterialExtAttrDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcMaterialExtAttrMapper.deleteBatch(HcMaterialExtAttrDO::getId, deleteIds);
        }

        List<HcMaterialExtAttrDO> updateList = list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .peek(item -> {
                    item.clean();
                    item.setMaterialId(parentId);
                    item.setMaterialCode(materialCode);
                })
                .toList();
        if (!updateList.isEmpty()) {
            hcMaterialExtAttrMapper.updateBatch(updateList);
        }

        List<HcMaterialExtAttrDO> createList = list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .peek(item -> {
                    item.clean();
                    item.setId(null);
                    item.setMaterialId(parentId);
                    item.setMaterialCode(materialCode);
                })
                .toList();
        if (!createList.isEmpty()) {
            hcMaterialExtAttrMapper.insertBatch(createList);
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ModelSegmentSnapshot {
        private String itemCode;
        private String itemName;
        private String segmentValue;
        private String segmentText;
        private Integer sort;
        private Long dictId;
    }

}
