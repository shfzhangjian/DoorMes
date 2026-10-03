package cn.iocoder.yudao.module.mes.dal.mysql.hc.recipe;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeItemDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface HcRecipeItemMapper extends BaseMapperX<HcRecipeItemDO> {

    default List<HcRecipeItemDO> selectListByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<HcRecipeItemDO>().eq(HcRecipeItemDO::getRecipeId, parentId).orderByAsc(HcRecipeItemDO::getId));
    }

    default void deleteByParentId(Long parentId) {
        delete(new LambdaQueryWrapperX<HcRecipeItemDO>().eq(HcRecipeItemDO::getRecipeId, parentId));
    }

    default void deleteByParentIds(Collection<Long> parentIds) {
        delete(new LambdaQueryWrapperX<HcRecipeItemDO>().in(HcRecipeItemDO::getRecipeId, parentIds));
    }
}