package cn.iocoder.yudao.module.mes.controller.admin.hc.recipe;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipeDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipeSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.recipe.vo.HcRecipeSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe.HcRecipeItemDO;
import cn.iocoder.yudao.module.mes.service.hc.recipe.HcRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 配方")
@RestController
@RequestMapping("/mes/hc/base/recipe")
@Validated
public class HcRecipeController {

    @Resource
    private HcRecipeService hcRecipeService;

    @PostMapping("/create")
    @Operation(summary = "创建配方")
    public CommonResult<Long> createHcRecipe(@Valid @RequestBody HcRecipeSaveReqVO createReqVO) {
        return success(hcRecipeService.createHcRecipe(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新配方")
    public CommonResult<Boolean> updateHcRecipe(@Valid @RequestBody HcRecipeSaveReqVO updateReqVO) {
        hcRecipeService.updateHcRecipe(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除配方")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteHcRecipe(@RequestParam("id") Long id) {
        hcRecipeService.deleteHcRecipe(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除配方")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteHcRecipeList(@RequestParam("ids") List<Long> ids) {
        hcRecipeService.deleteHcRecipeListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取配方")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcRecipeRespVO> getHcRecipe(@RequestParam("id") Long id) {
        HcRecipeDO entity = hcRecipeService.getHcRecipe(id);
        return success(BeanUtils.toBean(entity, HcRecipeRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获取配方详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcRecipeDetailRespVO> getHcRecipeDetail(@RequestParam("id") Long id) {
        HcRecipeDO entity = hcRecipeService.getHcRecipe(id);
        HcRecipeDetailRespVO respVO = BeanUtils.toBean(entity, HcRecipeDetailRespVO.class);
        respVO.setRecipeItems(hcRecipeService.getHcRecipeItemListByParentId(id));
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取配方精简列表")
    public CommonResult<List<HcRecipeSimpleRespVO>> getHcRecipeSimpleList() {
        List<HcRecipeDO> list = hcRecipeService.getHcRecipeSimpleList();
        return success(BeanUtils.toBean(list, HcRecipeSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "获取配方精简映射")
    public CommonResult<Map<Long, String>> getHcRecipeSimpleMap() {
        List<HcRecipeDO> list = hcRecipeService.getHcRecipeSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getRecipeName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "获取配方下拉选项")
    public CommonResult<List<HcRecipeSelectOptionRespVO>> getHcRecipeSelectOptions() {
        List<HcRecipeDO> list = hcRecipeService.getHcRecipeSimpleList();
        List<HcRecipeSelectOptionRespVO> result = new ArrayList<>();
        for (HcRecipeDO item : list) {
            HcRecipeSelectOptionRespVO option = new HcRecipeSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getModelCode() == null || item.getModelCode().isBlank() ? item.getRecipeName() : item.getModelCode());
            option.setCode(item.getRecipeCode());
            option.setModelCode(item.getModelCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/select-options-by-material-id")
    @Operation(summary = "按物料获取配方下拉选项")
    public CommonResult<List<HcRecipeSelectOptionRespVO>> getHcRecipeSelectOptionsByMaterialId(@RequestParam("materialId") Long materialId) {
        List<HcRecipeDO> list = hcRecipeService.getHcRecipeSimpleListByMaterialId(materialId);
        List<HcRecipeSelectOptionRespVO> result = new ArrayList<>();
        for (HcRecipeDO item : list) {
            HcRecipeSelectOptionRespVO option = new HcRecipeSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getModelCode() == null || item.getModelCode().isBlank() ? item.getRecipeName() : item.getModelCode());
            option.setCode(item.getRecipeCode());
            option.setModelCode(item.getModelCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获取配方列表")
    public CommonResult<List<HcRecipeRespVO>> getHcRecipeList(@Valid HcRecipePageReqVO reqVO) {
        List<HcRecipeDO> list = hcRecipeService.getHcRecipeList(reqVO);
        return success(BeanUtils.toBean(list, HcRecipeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获取配方分页")
    public CommonResult<PageResult<HcRecipeRespVO>> getHcRecipePage(@Valid HcRecipePageReqVO pageReqVO) {
        PageResult<HcRecipeDO> pageResult = hcRecipeService.getHcRecipePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcRecipeRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出配方 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcRecipeExcel(@Valid HcRecipePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcRecipeDO> list = hcRecipeService.getHcRecipePage(pageReqVO).getList();
        ExcelUtils.write(response, "配方.xls", "配方", HcRecipeRespVO.class, BeanUtils.toBean(list, HcRecipeRespVO.class));
    }

    @GetMapping("/mes_md_recipe_item/list-by-parent-id")
    @Operation(summary = "获取配方明细列表")
    @Parameter(name = "parentId", description = "配方ID", required = true)
    public CommonResult<List<HcRecipeItemDO>> getHcRecipeItemListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcRecipeService.getHcRecipeItemListByParentId(parentId));
    }
}
