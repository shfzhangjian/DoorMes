package cn.iocoder.yudao.module.mes.service.hc.recipe;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeItemDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.recipe.HcRecipeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.recipe.HcRecipeMapper;
import jakarta.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCRECIPE_NOT_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCRECIPE_RECIPECODE_EXISTS;

@Service
@Validated
public class HcRecipeServiceImpl implements HcRecipeService {

    @Resource
    private HcRecipeMapper hcRecipeMapper;

    @Resource
    private HcRecipeItemMapper hcRecipeItemMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcRecipe(HcRecipeSaveReqVO createReqVO) {
        validateRecipeCodeUnique(null, createReqVO.getRecipeCode());
        HcRecipeDO entity = BeanUtils.toBean(createReqVO, HcRecipeDO.class);
        normalizeIndependentRecipe(entity);
        hcRecipeMapper.insert(entity);
        createHcRecipeItemList(entity.getId(), entity.getRecipeCode(), createReqVO.getRecipeItems());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcRecipe(HcRecipeSaveReqVO updateReqVO) {
        validateHcRecipeExists(updateReqVO.getId());
        validateRecipeCodeUnique(updateReqVO.getId(), updateReqVO.getRecipeCode());
        HcRecipeDO updateObj = BeanUtils.toBean(updateReqVO, HcRecipeDO.class);
        normalizeIndependentRecipe(updateObj);
        hcRecipeMapper.updateById(updateObj);
        updateHcRecipeItemList(updateReqVO.getId(), updateObj.getRecipeCode(), updateReqVO.getRecipeItems());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcRecipe(Long id) {
        validateHcRecipeExists(id);
        hcRecipeMapper.deleteById(id);
        hcRecipeItemMapper.deleteByParentId(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcRecipeListByIds(List<Long> ids) {
        hcRecipeMapper.deleteByIds(ids);
        hcRecipeItemMapper.deleteByParentIds(ids);
    }

    private void validateHcRecipeExists(Long id) {
        if (hcRecipeMapper.selectById(id) == null) {
            throw exception(HCRECIPE_NOT_EXISTS);
        }
    }

    private void validateRecipeCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcRecipeDO entity = hcRecipeMapper.selectOne(new LambdaQueryWrapperX<HcRecipeDO>()
                .eq(HcRecipeDO::getRecipeCode, value)
                .neIfPresent(HcRecipeDO::getId, id));
        if (entity != null) {
            throw exception(HCRECIPE_RECIPECODE_EXISTS);
        }
    }

    private void normalizeIndependentRecipe(HcRecipeDO entity) {
        if (entity == null) {
            return;
        }
        if (!hasText(entity.getProductMaterialCode())) {
            entity.setProductMaterialCode(null);
        }
        if (!hasText(entity.getProductMaterialName())) {
            entity.setProductMaterialName(null);
        }
        if (entity.getProductMaterialId() == null) {
            entity.setProductMaterialCode(null);
            entity.setProductMaterialName(null);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    @Override
    public HcRecipeDO getHcRecipe(Long id) {
        return hcRecipeMapper.selectById(id);
    }

    @Override
    public List<HcRecipeDO> getHcRecipeSimpleList() {
        LambdaQueryWrapperX<HcRecipeDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcRecipeDO::getStatus, 1);
        queryWrapper.orderByAsc(HcRecipeDO::getRecipeCode);
        queryWrapper.orderByDesc(HcRecipeDO::getId);
        return hcRecipeMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcRecipeDO> getHcRecipeSimpleListByMaterialId(Long materialId) {
        return getHcRecipeSimpleList();
    }

    @Override
    public List<HcRecipeDO> getHcRecipeList(HcRecipePageReqVO reqVO) {
        return hcRecipeMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcRecipeDO> getHcRecipePage(HcRecipePageReqVO pageReqVO) {
        return hcRecipeMapper.selectPage(pageReqVO);
    }

    @Override
    public List<HcRecipeItemDO> getHcRecipeItemListByParentId(Long parentId) {
        return hcRecipeItemMapper.selectListByParentId(parentId);
    }

    private void createHcRecipeItemList(Long parentId, String recipeCode, List<HcRecipeItemDO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        list.forEach(item -> {
            item.clean();
            item.setId(null);
            item.setRecipeId(parentId);
            item.setRecipeCode(recipeCode);
        });
        hcRecipeItemMapper.insertBatch(list);
    }

    private void updateHcRecipeItemList(Long parentId, String recipeCode, List<HcRecipeItemDO> list) {
        List<HcRecipeItemDO> dbList = hcRecipeItemMapper.selectListByParentId(parentId);
        if (list == null) {
            list = List.of();
        }

        Set<Long> reqIds = list.stream()
                .map(HcRecipeItemDO::getId)
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .collect(Collectors.toCollection(HashSet::new));

        Set<Long> deleteIds = dbList.stream()
                .map(HcRecipeItemDO::getId)
                .filter(id -> !reqIds.contains(id))
                .collect(Collectors.toSet());
        if (!deleteIds.isEmpty()) {
            hcRecipeItemMapper.deleteBatch(HcRecipeItemDO::getId, deleteIds);
        }

        List<HcRecipeItemDO> updateList = list.stream()
                .filter(item -> item.getId() != null && item.getId() > 0)
                .peek(item -> {
                    item.clean();
                    item.setRecipeId(parentId);
                    item.setRecipeCode(recipeCode);
                })
                .toList();
        if (!updateList.isEmpty()) {
            hcRecipeItemMapper.updateBatch(updateList);
        }

        List<HcRecipeItemDO> createList = list.stream()
                .filter(item -> item.getId() == null || item.getId() <= 0)
                .peek(item -> {
                    item.clean();
                    item.setId(null);
                    item.setRecipeId(parentId);
                    item.setRecipeCode(recipeCode);
                })
                .toList();
        if (!createList.isEmpty()) {
            hcRecipeItemMapper.insertBatch(createList);
        }
    }
}
