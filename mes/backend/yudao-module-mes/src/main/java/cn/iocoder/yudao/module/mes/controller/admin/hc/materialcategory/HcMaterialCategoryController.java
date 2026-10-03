package cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategoryDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategoryPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategoryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategorySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategorySelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategorySimpleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.materialcategory.vo.HcMaterialCategoryTreeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.materialcategory.HcMaterialCategoryDO;
import cn.iocoder.yudao.module.mes.service.hc.materialcategory.HcMaterialCategoryService;
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
import org.springframework.web.bind.annotation.*;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 物料分类")
@RestController
@RequestMapping("/mes/hc/base/material-category")
@Validated
public class HcMaterialCategoryController {

    @Resource
    private HcMaterialCategoryService hcMaterialCategoryService;

    @PostMapping("/create")
    @Operation(summary = "新增物料分类")
    public CommonResult<Long> createHcMaterialCategory(@Valid @RequestBody HcMaterialCategorySaveReqVO createReqVO) {
        return success(hcMaterialCategoryService.createHcMaterialCategory(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改物料分类")
    public CommonResult<Boolean> updateHcMaterialCategory(@Valid @RequestBody HcMaterialCategorySaveReqVO updateReqVO) {
        hcMaterialCategoryService.updateHcMaterialCategory(updateReqVO);
        return success(true);
    }

    @PutMapping("/move-up")
    @Operation(summary = "物料分类上移")
    public CommonResult<Boolean> moveUp(@RequestParam("id") Long id) {
        hcMaterialCategoryService.moveUp(id);
        return success(true);
    }

    @PutMapping("/move-down")
    @Operation(summary = "物料分类下移")
    public CommonResult<Boolean> moveDown(@RequestParam("id") Long id) {
        hcMaterialCategoryService.moveDown(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物料分类")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteHcMaterialCategory(@RequestParam("id") Long id) {
        hcMaterialCategoryService.deleteHcMaterialCategory(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除物料分类")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteHcMaterialCategoryList(@RequestParam("ids") List<Long> ids) {
        hcMaterialCategoryService.deleteHcMaterialCategoryListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得物料分类")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcMaterialCategoryRespVO> getHcMaterialCategory(@RequestParam("id") Long id) {
        HcMaterialCategoryDO entity = hcMaterialCategoryService.getHcMaterialCategory(id);
        return success(BeanUtils.toBean(entity, HcMaterialCategoryRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得物料分类详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcMaterialCategoryDetailRespVO> getHcMaterialCategoryDetail(@RequestParam("id") Long id) {
        HcMaterialCategoryDO entity = hcMaterialCategoryService.getHcMaterialCategory(id);
        return success(BeanUtils.toBean(entity, HcMaterialCategoryDetailRespVO.class));
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获得物料分类精简列表")
    public CommonResult<List<HcMaterialCategorySimpleRespVO>> getHcMaterialCategorySimpleList() {
        List<HcMaterialCategoryDO> list = hcMaterialCategoryService.getHcMaterialCategorySimpleList();
        return success(BeanUtils.toBean(list, HcMaterialCategorySimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "获得物料分类精简映射")
    public CommonResult<Map<Long, String>> getHcMaterialCategorySimpleMap() {
        List<HcMaterialCategoryDO> list = hcMaterialCategoryService.getHcMaterialCategorySimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getCategoryName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "获得物料分类下拉选项")
    public CommonResult<List<HcMaterialCategorySelectOptionRespVO>> getHcMaterialCategorySelectOptions() {
        List<HcMaterialCategoryDO> list = hcMaterialCategoryService.getHcMaterialCategorySimpleList();
        List<HcMaterialCategorySelectOptionRespVO> result = new ArrayList<>();
        for (HcMaterialCategoryDO item : list) {
            HcMaterialCategorySelectOptionRespVO option = new HcMaterialCategorySelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getCategoryName());
            option.setCode(item.getCategoryCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获得物料分类列表")
    public CommonResult<List<HcMaterialCategoryRespVO>> getHcMaterialCategoryList(@Valid HcMaterialCategoryPageReqVO reqVO) {
        List<HcMaterialCategoryDO> list = hcMaterialCategoryService.getHcMaterialCategoryList(reqVO);
        return success(BeanUtils.toBean(list, HcMaterialCategoryRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得物料分类分页")
    public CommonResult<PageResult<HcMaterialCategoryRespVO>> getHcMaterialCategoryPage(@Valid HcMaterialCategoryPageReqVO pageReqVO) {
        PageResult<HcMaterialCategoryDO> pageResult = hcMaterialCategoryService.getHcMaterialCategoryPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcMaterialCategoryRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出物料分类 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcMaterialCategoryExcel(@Valid HcMaterialCategoryPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcMaterialCategoryDO> list = hcMaterialCategoryService.getHcMaterialCategoryPage(pageReqVO).getList();
        ExcelUtils.write(response, "物料分类.xls", "物料分类", HcMaterialCategoryRespVO.class, BeanUtils.toBean(list, HcMaterialCategoryRespVO.class));
    }

    @GetMapping("/tree")
    @Operation(summary = "获得物料分类树")
    public CommonResult<List<HcMaterialCategoryTreeRespVO>> getHcMaterialCategoryTree() {
        List<HcMaterialCategoryDO> list = hcMaterialCategoryService.getHcMaterialCategorySimpleList();
        List<HcMaterialCategoryTreeRespVO> treeList = BeanUtils.toBean(list, HcMaterialCategoryTreeRespVO.class);
        Map<Long, HcMaterialCategoryTreeRespVO> nodeMap = new LinkedHashMap<>();
        treeList.forEach(node -> nodeMap.put(node.getId(), node));
        List<HcMaterialCategoryTreeRespVO> roots = new ArrayList<>();
        for (HcMaterialCategoryTreeRespVO node : treeList) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId <= 0 || !nodeMap.containsKey(parentId)) {
                roots.add(node);
                continue;
            }
            HcMaterialCategoryTreeRespVO parent = nodeMap.get(parentId);
            if (parent.getChildren() == null) {
                parent.setChildren(new ArrayList<>());
            }
            parent.getChildren().add(node);
        }
        return success(roots);
    }
}
