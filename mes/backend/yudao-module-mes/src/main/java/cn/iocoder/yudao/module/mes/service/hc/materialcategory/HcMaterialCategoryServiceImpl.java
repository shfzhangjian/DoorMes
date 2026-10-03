package cn.iocoder.yudao.module.mes.service.hc.materialcategory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategoryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.materialcategory.HcMaterialCategoryDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.materialcategory.HcMaterialCategoryMapper;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMATERIALCATEGORY_CATEGORYCODE_EXISTS;
import static cn.iocoder.yudao.module.mes.enums.HcErrorCodeConstants.HCMATERIALCATEGORY_NOT_EXISTS;

@Service
@Validated
public class HcMaterialCategoryServiceImpl implements HcMaterialCategoryService {

    @Resource
    private HcMaterialCategoryMapper hcMaterialCategoryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createHcMaterialCategory(HcMaterialCategorySaveReqVO createReqVO) {
        validateCategoryCodeUnique(null, createReqVO.getCategoryCode());
        validateParent(createReqVO.getParentId(), null);
        HcMaterialCategoryDO entity = BeanUtils.toBean(createReqVO, HcMaterialCategoryDO.class);
        if (entity.getSort() == null || entity.getSort() <= 0) {
            entity.setSort(hcMaterialCategoryMapper.selectMaxSortByParentId(entity.getParentId()) + 1);
        }
        fillPathFields(entity);
        hcMaterialCategoryMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHcMaterialCategory(HcMaterialCategorySaveReqVO updateReqVO) {
        HcMaterialCategoryDO oldEntity = validateHcMaterialCategoryExists(updateReqVO.getId());
        validateCategoryCodeUnique(updateReqVO.getId(), updateReqVO.getCategoryCode());
        validateParent(updateReqVO.getParentId(), updateReqVO.getId());
        HcMaterialCategoryDO updateObj = BeanUtils.toBean(updateReqVO, HcMaterialCategoryDO.class);
        if (updateObj.getSort() == null || updateObj.getSort() <= 0) {
            updateObj.setSort(oldEntity.getSort());
        }
        fillPathFields(updateObj);
        hcMaterialCategoryMapper.updateById(updateObj);
        refreshChildrenPath(updateObj.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcMaterialCategory(Long id) {
        validateHcMaterialCategoryExists(id);
        hcMaterialCategoryMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteHcMaterialCategoryListByIds(List<Long> ids) {
        hcMaterialCategoryMapper.deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveUp(Long id) {
        HcMaterialCategoryDO current = validateHcMaterialCategoryExists(id);
        List<HcMaterialCategoryDO> siblings = hcMaterialCategoryMapper.selectListByParentId(current.getParentId());
        for (int i = 1; i < siblings.size(); i++) {
            if (Objects.equals(siblings.get(i).getId(), id)) {
                swapSort(siblings.get(i - 1), siblings.get(i));
                break;
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveDown(Long id) {
        HcMaterialCategoryDO current = validateHcMaterialCategoryExists(id);
        List<HcMaterialCategoryDO> siblings = hcMaterialCategoryMapper.selectListByParentId(current.getParentId());
        for (int i = 0; i < siblings.size() - 1; i++) {
            if (Objects.equals(siblings.get(i).getId(), id)) {
                swapSort(siblings.get(i), siblings.get(i + 1));
                break;
            }
        }
    }

    private void swapSort(HcMaterialCategoryDO left, HcMaterialCategoryDO right) {
        Integer leftSort = left.getSort() == null ? 0 : left.getSort();
        Integer rightSort = right.getSort() == null ? 0 : right.getSort();
        left.setSort(rightSort);
        right.setSort(leftSort);
        hcMaterialCategoryMapper.updateById(left);
        hcMaterialCategoryMapper.updateById(right);
    }

    private HcMaterialCategoryDO validateHcMaterialCategoryExists(Long id) {
        HcMaterialCategoryDO entity = hcMaterialCategoryMapper.selectById(id);
        if (entity == null) {
            throw exception(HCMATERIALCATEGORY_NOT_EXISTS);
        }
        return entity;
    }

    private void validateCategoryCodeUnique(Long id, String value) {
        if (value == null) {
            return;
        }
        HcMaterialCategoryDO entity = hcMaterialCategoryMapper.selectOne(new LambdaQueryWrapperX<HcMaterialCategoryDO>()
                .eq(HcMaterialCategoryDO::getCategoryCode, value)
                .neIfPresent(HcMaterialCategoryDO::getId, id));
        if (entity != null) {
            throw exception(HCMATERIALCATEGORY_CATEGORYCODE_EXISTS);
        }
    }

    private void validateParent(Long parentId, Long selfId) {
        if (parentId == null || parentId <= 0) {
            return;
        }
        if (selfId != null && Objects.equals(parentId, selfId)) {
            throw new IllegalArgumentException("父级分类不能选择自己");
        }
        HcMaterialCategoryDO parent = validateHcMaterialCategoryExists(parentId);
        while (parent != null && parent.getParentId() != null && parent.getParentId() > 0) {
            if (selfId != null && Objects.equals(parent.getParentId(), selfId)) {
                throw new IllegalArgumentException("父级分类不能选择当前节点的下级");
            }
            parent = hcMaterialCategoryMapper.selectById(parent.getParentId());
        }
    }

    private void fillPathFields(HcMaterialCategoryDO entity) {
        if (entity.getParentId() == null || entity.getParentId() <= 0) {
            entity.setCategoryCodePath(entity.getCategoryCode());
            entity.setCategoryNamePath(entity.getCategoryName());
            entity.setCategoryPath(entity.getCategoryName());
            return;
        }
        HcMaterialCategoryDO parent = validateHcMaterialCategoryExists(entity.getParentId());
        String codePath = isBlank(parent.getCategoryCodePath()) ? parent.getCategoryCode() : parent.getCategoryCodePath();
        String namePath = isBlank(parent.getCategoryNamePath()) ? parent.getCategoryName() : parent.getCategoryNamePath();
        entity.setCategoryCodePath(codePath + "/" + entity.getCategoryCode());
        entity.setCategoryNamePath(namePath + "/" + entity.getCategoryName());
        entity.setCategoryPath(entity.getCategoryNamePath());
    }

    private void refreshChildrenPath(Long parentId) {
        List<HcMaterialCategoryDO> children = hcMaterialCategoryMapper.selectListByParentId(parentId);
        for (HcMaterialCategoryDO child : children) {
            fillPathFields(child);
            hcMaterialCategoryMapper.updateById(child);
            refreshChildrenPath(child.getId());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public HcMaterialCategoryDO getHcMaterialCategory(Long id) {
        return hcMaterialCategoryMapper.selectById(id);
    }

    @Override
    public List<HcMaterialCategoryDO> getHcMaterialCategorySimpleList() {
        LambdaQueryWrapperX<HcMaterialCategoryDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.eq(HcMaterialCategoryDO::getStatus, 0);
        queryWrapper.orderByAsc(HcMaterialCategoryDO::getParentId);
        queryWrapper.orderByAsc(HcMaterialCategoryDO::getSort);
        queryWrapper.orderByAsc(HcMaterialCategoryDO::getId);
        return hcMaterialCategoryMapper.selectList(queryWrapper);
    }

    @Override
    public List<HcMaterialCategoryDO> getHcMaterialCategoryList(HcMaterialCategoryPageReqVO reqVO) {
        return hcMaterialCategoryMapper.selectList(reqVO);
    }

    @Override
    public PageResult<HcMaterialCategoryDO> getHcMaterialCategoryPage(HcMaterialCategoryPageReqVO pageReqVO) {
        return hcMaterialCategoryMapper.selectPage(pageReqVO);
    }
}