package cn.iocoder.yudao.module.mes.service.hc.recipe;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeItemDO;

import java.util.List;

public interface HcRecipeService {
    Long createHcRecipe(HcRecipeSaveReqVO createReqVO);
    void updateHcRecipe(HcRecipeSaveReqVO updateReqVO);
    void deleteHcRecipe(Long id);
    void deleteHcRecipeListByIds(List<Long> ids);
    HcRecipeDO getHcRecipe(Long id);
    List<HcRecipeDO> getHcRecipeSimpleList();
    List<HcRecipeDO> getHcRecipeSimpleListByMaterialId(Long materialId);
    List<HcRecipeDO> getHcRecipeList(HcRecipePageReqVO reqVO);
    PageResult<HcRecipeDO> getHcRecipePage(HcRecipePageReqVO pageReqVO);
    List<HcRecipeItemDO> getHcRecipeItemListByParentId(Long parentId);
}