package cn.iocoder.yudao.module.mes.controller.admin.hc.material;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialBindingPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialCodeGenerateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.material.vo.HcMaterialSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.material.HcMaterialExtAttrDO;
import cn.iocoder.yudao.module.mes.service.hc.material.HcMaterialService;
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

@Tag(name = "管理后台 - 物料主数据")
@RestController
@RequestMapping("/mes/hc/base/material")
@Validated
public class HcMaterialController {

    @Resource
    private HcMaterialService hcMaterialService;

    @PostMapping("/create")
    @Operation(summary = "创建物料主数据")
    public CommonResult<Long> createHcMaterial(@Valid @RequestBody HcMaterialSaveReqVO createReqVO) {
        return success(hcMaterialService.createHcMaterial(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新物料主数据")
    public CommonResult<Boolean> updateHcMaterial(@Valid @RequestBody HcMaterialSaveReqVO updateReqVO) {
        hcMaterialService.updateHcMaterial(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除物料主数据")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<Boolean> deleteHcMaterial(@RequestParam("id") Long id) {
        hcMaterialService.deleteHcMaterial(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除物料主数据")
    @Parameter(name = "ids", description = "主键列表", required = true)
    public CommonResult<Boolean> deleteHcMaterialList(@RequestParam("ids") List<Long> ids) {
        hcMaterialService.deleteHcMaterialListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "查询物料主数据")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<HcMaterialRespVO> getHcMaterial(@RequestParam("id") Long id) {
        HcMaterialDO entity = hcMaterialService.getHcMaterial(id);
        return success(BeanUtils.toBean(entity, HcMaterialRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "查询物料主数据详情")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<HcMaterialDetailRespVO> getHcMaterialDetail(@RequestParam("id") Long id) {
        HcMaterialDO entity = hcMaterialService.getHcMaterial(id);
        HcMaterialDetailRespVO respVO = BeanUtils.toBean(entity, HcMaterialDetailRespVO.class);
        respVO.setMaterialExtAttrs(hcMaterialService.getHcMaterialExtAttrListByParentId(id));
        return success(respVO);
    }

    @PostMapping("/generate-material-code")
    @Operation(summary = "预生成物料编码")
    public CommonResult<HcMaterialCodeGenerateRespVO> generateMaterialCode(@RequestBody HcMaterialSaveReqVO reqVO) {
        HcMaterialCodeGenerateRespVO respVO = new HcMaterialCodeGenerateRespVO();
        respVO.setMaterialCode(hcMaterialService.generateMaterialCode(reqVO));
        return success(respVO);
    }

    @GetMapping("/binding-preview")
    @Operation(summary = "查询物料工艺挂接预览")
    @Parameter(name = "id", description = "物料ID", required = true)
    public CommonResult<HcMaterialBindingPreviewRespVO> getHcMaterialBindingPreview(@RequestParam("id") Long id) {
        return success(hcMaterialService.getHcMaterialBindingPreview(id));
    }

    @PostMapping("/binding-preview")
    @Operation(summary = "棰勮鐗╂枡宸ヨ壓鎸傛帴")
    public CommonResult<HcMaterialBindingPreviewRespVO> previewHcMaterialBinding(@RequestBody HcMaterialSaveReqVO reqVO) {
        return success(hcMaterialService.previewHcMaterialBinding(reqVO));
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "查询物料主数据精简列表")
    public CommonResult<List<HcMaterialSimpleRespVO>> getHcMaterialSimpleList() {
        List<HcMaterialDO> list = hcMaterialService.getHcMaterialSimpleList();
        return success(BeanUtils.toBean(list, HcMaterialSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "查询物料主数据名称映射")
    public CommonResult<Map<Long, String>> getHcMaterialSimpleMap() {
        List<HcMaterialDO> list = hcMaterialService.getHcMaterialSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getMaterialName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "查询物料主数据下拉选项")
    public CommonResult<List<HcMaterialSelectOptionRespVO>> getHcMaterialSelectOptions() {
        List<HcMaterialDO> list = hcMaterialService.getHcMaterialSimpleList();
        List<HcMaterialSelectOptionRespVO> result = new ArrayList<>();
        for (HcMaterialDO item : list) {
            HcMaterialSelectOptionRespVO option = new HcMaterialSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getMaterialName());
            option.setCode(item.getMaterialCode());
            option.setStatus(item.getMaterialStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "查询物料主数据列表")
    public CommonResult<List<HcMaterialRespVO>> getHcMaterialList(@Valid HcMaterialPageReqVO reqVO) {
        List<HcMaterialDO> list = hcMaterialService.getHcMaterialList(reqVO);
        return success(BeanUtils.toBean(list, HcMaterialRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "查询物料主数据分页")
    public CommonResult<PageResult<HcMaterialRespVO>> getHcMaterialPage(@Valid HcMaterialPageReqVO pageReqVO) {
        PageResult<HcMaterialDO> pageResult = hcMaterialService.getHcMaterialPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcMaterialRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出物料主数据 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcMaterialExcel(@Valid HcMaterialPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcMaterialDO> list = hcMaterialService.getHcMaterialPage(pageReqVO).getList();
        ExcelUtils.write(response, "物料主数据.xls", "数据", HcMaterialRespVO.class, BeanUtils.toBean(list, HcMaterialRespVO.class));
    }

    @GetMapping("/mes_md_material_ext_attr/list-by-parent-id")
    @Operation(summary = "查询物料扩展属性列表")
    @Parameter(name = "parentId", description = "物料ID", required = true)
    public CommonResult<List<HcMaterialExtAttrDO>> getHcMaterialExtAttrListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcMaterialService.getHcMaterialExtAttrListByParentId(parentId));
    }

}
