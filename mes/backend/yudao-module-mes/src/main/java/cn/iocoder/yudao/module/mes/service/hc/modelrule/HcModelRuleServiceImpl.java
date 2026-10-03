package cn.iocoder.yudao.module.mes.service.hc.modelrule;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleGenerateReqVO;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.modelrule.HcModelRuleDictMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.modelrule.HcModelRuleItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.modelrule.HcModelRuleMapper;
import jakarta.annotation.Resource;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_GENERATE_INVALID_DICT_VALUE;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_GENERATE_ITEMS_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_GENERATE_ITEM_CODE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_GENERATE_LENGTH_MISMATCH;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_GENERATE_REQUIRED_VALUE_EMPTY;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMODELRULE_RULECODE_EXISTS;

@Service
@Validated
public class HcModelRuleServiceImpl implements HcModelRuleService {

    @Resource
    private HcModelRuleMapper hcModelRuleMapper;

    @Resource
    private HcModelRuleItemMapper hcModelRuleItemMapper;

    @Resource
    private HcModelRuleDictMapper hcModelRuleDictMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcModelRule(HcModelRuleSaveReqVO createReqVO) {
        validateRuleCodeUnique(null, createReqVO.getRuleCode());
        HcModelRuleDO entity = BeanUtils.toBean(createReqVO, HcModelRuleDO.class);
        hcModelRuleMapper.insert(entity);
        createHcModelRuleItemList(entity.getId(), createReqVO.getRuleCode(), createReqVO.getModelRuleItems());
        createHcModelRuleDictList(entity.getId(), createReqVO.getModelRuleDicts());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcModelRule(HcModelRuleSaveReqVO updateReqVO) {
        validateHcModelRuleExists(updateReqVO.getId());
        validateRuleCodeUnique(updateReqVO.getId(), updateReqVO.getRuleCode());
        HcModelRuleDO updateObj = BeanUtils.toBean(updateReqVO, HcModelRuleDO.class);
        hcModelRuleMapper.updateById(updateObj);
        updateHcModelRuleItemList(updateReqVO.getId(), updateReqVO.getRuleCode(), updateReqVO.getModelRuleItems());
        updateHcModelRuleDictList(updateReqVO.getId(), updateReqVO.getModelRuleDicts());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcModelRule(Long id) {
        validateHcModelRuleExists(id);
        hcModelRuleMapper.deleteById(id);
        hcModelRuleItemMapper.deleteByParentId(id);
        hcModelRuleDictMapper.deleteByParentId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcModelRuleListByIds(List<Long> ids) {
        hcModelRuleMapper.deleteByIds(ids);
        hcModelRuleItemMapper.deleteByParentIds(ids);
        hcModelRuleDictMapper.deleteByParentIds(ids);
    }

    private void validateHcModelRuleExists(Long id) {
        if (hcModelRuleMapper.selectById(id) == null) {
            throw exception(HCMODELRULE_NOT_EXISTS);
        }
    }

    private void validateRuleCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcModelRuleDO entity = hcModelRuleMapper.selectOne(new LambdaQueryWrapperX<HcModelRuleDO>()
                .eq(HcModelRuleDO::getRuleCode, value)
                .neIfPresent(HcModelRuleDO::getId, id));
        if (entity != null) {
            throw exception(HCMODELRULE_RULECODE_EXISTS);
        }
    }

    @Override
    public HcModelRuleDO getHcModelRule(Long id) {
        return hcModelRuleMapper.selectById(id);
    }

    @Override
    public List<HcModelRuleDO> getHcModelRuleSimpleList() {
        LambdaQueryWrapperX<HcModelRuleDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcModelRuleDO::getStatus, 1);
        queryWrapper.orderByAsc(HcModelRuleDO::getRuleCode);
        queryWrapper.orderByDesc(HcModelRuleDO::getId);
        return hcModelRuleMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcModelRuleDO> getHcModelRuleList(HcModelRulePageReqVO reqVO) {
        return hcModelRuleMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcModelRuleDO> getHcModelRulePage(HcModelRulePageReqVO pageReqVO) {
        return hcModelRuleMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcModelRuleItemDO> getHcModelRuleItemListByParentId(Long parentId) {
        return hcModelRuleItemMapper.selectListByParentId(parentId);
    }

    private void createHcModelRuleItemList(Long parentId, String ruleCode, List<HcModelRuleItemDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setRuleId(parentId);
            item.setRuleCode(ruleCode);
        });
        hcModelRuleItemMapper.insertBatch(list);
    }

    private void updateHcModelRuleItemList(Long parentId, String ruleCode, List<HcModelRuleItemDO> list) {
        List<HcModelRuleItemDO> dbList = hcModelRuleItemMapper.selectListByParentId(parentId);
        if (list == null) {
            list = List.of();
        }

        Set<Long> reqIds = list.stream()
                .map(HcModelRuleItemDO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcModelRuleItemDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcModelRuleItemMapper.deleteBatch(HcModelRuleItemDO::getId, deleteIds);
        }

        List<HcModelRuleItemDO> updateList = list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .peek(item -> {
                    item.clean();
                    item.setRuleId(parentId);
                    item.setRuleCode(ruleCode);
                })
                .toList();
        if (!updateList.isEmpty()) {
            hcModelRuleItemMapper.updateBatch(updateList);
        }

        List<HcModelRuleItemDO> createList = list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .peek(item -> {
                    item.clean();
                    item.setId(null);
                    item.setRuleId(parentId);
                    item.setRuleCode(ruleCode);
                })
                .toList();
        if (!createList.isEmpty()) {
            hcModelRuleItemMapper.insertBatch(createList);
        }
    }

    @Override
    public List<HcModelRuleDictDO> getHcModelRuleDictListByParentId(Long parentId) {
        return hcModelRuleDictMapper.selectListByParentId(parentId);
    }

    @Override
    public String generateModelRuleCode(HcModelRuleGenerateReqVO reqVO) {
        if (reqVO.getModelRuleItems() == null || reqVO.getModelRuleItems().isEmpty()) {
            throw exception(HCMODELRULE_GENERATE_ITEMS_EMPTY);
        }
        Map<String, String> inputMap = reqVO.getTestValues() == null ? Map.of() : reqVO.getTestValues();
        Map<String, Set<String>> dictValueMap = buildDictValueMap(reqVO.getModelRuleDicts());
        return reqVO.getModelRuleItems().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(item -> item.getSort() == null ? Integer.MAX_VALUE : item.getSort()))
                .map(item -> resolveSegmentValue(item, inputMap, dictValueMap))
                .collect(Collectors.joining());
    }

    private void createHcModelRuleDictList(Long parentId, List<HcModelRuleDictDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<String, Long> itemIdMap = buildItemIdMap(parentId);
        list.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setRuleId(parentId);
            item.setRuleItemId(resolveRuleItemId(item, itemIdMap));
        });
        hcModelRuleDictMapper.insertBatch(list);
    }

    private void updateHcModelRuleDictList(Long parentId, List<HcModelRuleDictDO> list) {
        Map<String, Long> itemIdMap = buildItemIdMap(parentId);
        List<HcModelRuleDictDO> dbList = hcModelRuleDictMapper.selectListByParentId(parentId);
        if (list == null) {
            list = List.of();
        }

        Set<Long> reqIds = list.stream()
                .map(HcModelRuleDictDO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcModelRuleDictDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcModelRuleDictMapper.deleteBatch(HcModelRuleDictDO::getId, deleteIds);
        }

        List<HcModelRuleDictDO> updateList = list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .peek(item -> {
                    item.clean();
                    item.setRuleId(parentId);
                    item.setRuleItemId(resolveRuleItemId(item, itemIdMap));
                })
                .toList();
        if (!updateList.isEmpty()) {
            hcModelRuleDictMapper.updateBatch(updateList);
        }

        List<HcModelRuleDictDO> createList = list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .peek(item -> {
                    item.clean();
                    item.setId(null);
                    item.setRuleId(parentId);
                    item.setRuleItemId(resolveRuleItemId(item, itemIdMap));
                })
                .toList();
        if (!createList.isEmpty()) {
            hcModelRuleDictMapper.insertBatch(createList);
        }
    }

    private Map<String, Long> buildItemIdMap(Long parentId) {
        return hcModelRuleItemMapper.selectListByParentId(parentId).stream()
                .filter(item -> item.getItemCode() != null)
                .collect(Collectors.toMap(HcModelRuleItemDO::getItemCode, HcModelRuleItemDO::getId,
                        (left, right) -> left));
    }

    private Long resolveRuleItemId(HcModelRuleDictDO item, Map<String, Long> itemIdMap) {
        if (item.getRuleItemId() != null) {
            return item.getRuleItemId();
        }
        Long ruleItemId = itemIdMap.get(item.getItemCode());
        if (ruleItemId == null) {
            throw new IllegalArgumentException("型号规则字典缺少对应字段编码: " + item.getItemCode());
        }
        return ruleItemId;
    }

    private String resolveSegmentValue(HcModelRuleItemDO item, Map<String, String> inputMap,
                                       Map<String, Set<String>> dictValueMap) {
        if (item.getItemCode() == null || item.getItemCode().isBlank()) {
            throw exception(HCMODELRULE_GENERATE_ITEM_CODE_EMPTY);
        }
        if ("skip".equals(item.getParseType())) {
            return "";
        }
        String itemName = item.getItemName() == null || item.getItemName().isBlank() ? item.getItemCode() : item.getItemName();
        String input = inputMap.getOrDefault(item.getItemCode(), "");
        String value = "fixed".equals(item.getParseType()) ? defaultIfBlank(input, item.getFixedValue()) : input;
        if (value == null) {
            value = "";
        }
        if (Boolean.TRUE.equals(item.getRequiredFlag()) && value.isBlank()) {
            throw exception(HCMODELRULE_GENERATE_REQUIRED_VALUE_EMPTY, itemName);
        }
        if ("dict".equals(item.getDataSourceType()) && !value.isBlank()) {
            Set<String> dictValues = dictValueMap.getOrDefault(item.getItemCode(), Set.of());
            if (!dictValues.contains(value)) {
                throw exception(HCMODELRULE_GENERATE_INVALID_DICT_VALUE, itemName, value);
            }
        }
        Integer segmentLength = item.getSegmentLength();
        if (segmentLength != null && segmentLength > 0 && !value.isBlank() && value.length() != segmentLength) {
            throw exception(HCMODELRULE_GENERATE_LENGTH_MISMATCH, itemName, segmentLength, value.length());
        }
        return value;
    }

    private Map<String, Set<String>> buildDictValueMap(List<HcModelRuleDictDO> dictList) {
        if (dictList == null || dictList.isEmpty()) {
            return Map.of();
        }
        return dictList.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getItemCode() != null && !item.getItemCode().isBlank())
                .collect(Collectors.groupingBy(HcModelRuleDictDO::getItemCode,
                        Collectors.flatMapping(item -> buildDictCandidates(item).stream(), Collectors.toSet())));
    }

    private Set<String> buildDictCandidates(HcModelRuleDictDO item) {
        return java.util.stream.Stream.of(item.getDictCode(), item.getDictValue())
                .filter(Objects::nonNull)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toSet());
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
