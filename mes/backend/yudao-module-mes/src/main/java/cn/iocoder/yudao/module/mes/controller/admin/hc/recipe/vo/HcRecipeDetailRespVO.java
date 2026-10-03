package cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeItemDO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 配方 Detail Response VO")
@Data
public class HcRecipeDetailRespVO extends HcRecipeRespVO {

    @Schema(description = "配方明细列表")
    private List<HcRecipeItemDO> recipeItems;

}