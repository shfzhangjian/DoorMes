package cn.iocoder.yudao.module.mes.controller.admin.hc.bom;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomProductExportVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomProductImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomProductModelOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.bom.vo.HcBomSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomItemDO;
import cn.iocoder.yudao.module.mes.service.hc.bom.HcBomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - BOM")
@RestController
@RequestMapping("/mes/hc/base/bom")
@Validated
public class HcBomController {

    @Resource
    private HcBomService hcBomService;

    @PostMapping("/create")
    @Operation(summary = "新增BOM")
    public CommonResult<Long> createHcBom(@Valid @RequestBody HcBomSaveReqVO createReqVO) {
        return success(hcBomService.createHcBom(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改BOM")
    public CommonResult<Boolean> updateHcBom(@Valid @RequestBody HcBomSaveReqVO updateReqVO) {
        hcBomService.updateHcBom(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除BOM")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteHcBom(@RequestParam("id") Long id) {
        hcBomService.deleteHcBom(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除BOM")
    @Parameter(name = "ids", description = "编号列表", required = true)
    public CommonResult<Boolean> deleteHcBomList(@RequestParam("ids") List<Long> ids) {
        hcBomService.deleteHcBomListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得BOM")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcBomRespVO> getHcBom(@RequestParam("id") Long id) {
        HcBomDO entity = hcBomService.getHcBom(id);
        return success(BeanUtils.toBean(entity, HcBomRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得BOM详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcBomDetailRespVO> getHcBomDetail(@RequestParam("id") Long id) {
        HcBomDO entity = hcBomService.getHcBom(id);
        HcBomDetailRespVO respVO = BeanUtils.toBean(entity, HcBomDetailRespVO.class);
        respVO.setBomItems(hcBomService.getHcBomItemListByParentId(id));
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获得BOM精简列表")
    public CommonResult<List<HcBomSimpleRespVO>> getHcBomSimpleList() {
        List<HcBomDO> list = hcBomService.getHcBomSimpleList();
        return success(BeanUtils.toBean(list, HcBomSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "获得BOM精简映射")
    public CommonResult<Map<Long, String>> getHcBomSimpleMap() {
        List<HcBomDO> list = hcBomService.getHcBomSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getBomName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "获得BOM下拉选项")
    public CommonResult<List<HcBomSelectOptionRespVO>> getHcBomSelectOptions() {
        List<HcBomDO> list = hcBomService.getHcBomSimpleList();
        List<HcBomSelectOptionRespVO> result = new ArrayList<>();
        for (HcBomDO item : list) {
            HcBomSelectOptionRespVO option = new HcBomSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getBomName());
            option.setCode(item.getBomCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/select-options-by-material-id")
    @Operation(summary = "按产品物料获得BOM下拉选项")
    public CommonResult<List<HcBomSelectOptionRespVO>> getHcBomSelectOptionsByMaterialId(@RequestParam("materialId") Long materialId) {
        List<HcBomDO> list = hcBomService.getHcBomSimpleListByMaterialId(materialId);
        List<HcBomSelectOptionRespVO> result = new ArrayList<>();
        for (HcBomDO item : list) {
            HcBomSelectOptionRespVO option = new HcBomSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getBomName());
            option.setCode(item.getBomCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/product-model-options")
    @Operation(summary = "按BOM获得产品型号分组选项")
    public CommonResult<List<HcBomProductModelOptionRespVO>> getHcBomProductModelOptions(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "productMaterialId", required = false) Long productMaterialId) {
        List<HcBomDO> list = hcBomService.getHcBomProductModelOptionList(keyword, productMaterialId);
        Map<Long, HcBomProductModelOptionRespVO> resultMap = new LinkedHashMap<>();
        for (HcBomDO item : list) {
            if (item.getProductModelId() == null || !StringUtils.hasText(item.getProductModelCode())) {
                continue;
            }
            resultMap.computeIfAbsent(item.getProductModelId(), key -> {
                HcBomProductModelOptionRespVO option = new HcBomProductModelOptionRespVO();
                option.setValue(item.getProductModelId());
                option.setLabel(item.getProductModelCode());
                option.setProductModelId(item.getProductModelId());
                option.setProductModelCode(item.getProductModelCode());
                option.setProductModelName(item.getProductModelName());
                option.setCode(item.getProductModelCode());
                option.setModelName(item.getProductModelName());
                option.setBomType(item.getBomType());
                option.setStatus(item.getStatus());
                return option;
            });
        }
        return success(new ArrayList<>(resultMap.values()));
    }

    @GetMapping("/list")
    @Operation(summary = "获得BOM列表")
    public CommonResult<List<HcBomRespVO>> getHcBomList(@Valid HcBomPageReqVO reqVO) {
        List<HcBomDO> list = hcBomService.getHcBomList(reqVO);
        return success(BeanUtils.toBean(list, HcBomRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得BOM分页")
    public CommonResult<PageResult<HcBomRespVO>> getHcBomPage(@Valid HcBomPageReqVO pageReqVO) {
        PageResult<HcBomDO> pageResult = hcBomService.getHcBomPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcBomRespVO.class));
    }

    @PostMapping("/import-product-bom")
    @Operation(summary = "导入产品BOM Excel")
    public CommonResult<HcBomProductImportRespVO> importProductBom(@RequestParam("file") MultipartFile file,
                                                                   @RequestParam(value = "overwrite", required = false, defaultValue = "true") Boolean overwrite)
            throws IOException {
        return success(hcBomService.importProductBom(file, overwrite));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品BOM Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcBomExcel(@Valid HcBomPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcBomDO> list = hcBomService.getHcBomPage(pageReqVO).getList();
        list.sort(Comparator.comparing(HcBomDO::getId, Comparator.nullsLast(Long::compareTo)));
        ExcelUtils.write(response, "产品BOM.xlsx", "产品BOM", HcBomProductExportVO.class, buildProductBomExportList(list));
    }

    private List<HcBomProductExportVO> buildProductBomExportList(List<HcBomDO> list) {
        List<HcBomProductExportVO> result = new ArrayList<>();
        for (HcBomDO bom : list) {
            List<HcBomItemDO> bomItems = hcBomService.getHcBomItemListByParentId(bom.getId());
            HcBomProductExportVO exportVO = new HcBomProductExportVO();
            exportVO.setProductModelCode(bom.getProductModelCode());
            exportVO.setProductSpec(bom.getProductSpec());
            exportVO.setProductMaterialCode(bom.getProductMaterialCode());
            exportVO.setRoughGrindingMaterialCode(findProductBomItemCode(bomItems, "ROUGH_INTERMEDIATE", "WC-GRIND", "中间品"));
            exportVO.setAdhesive1IntermediateMaterialCode(findProductBomItemCode(bomItems, "ADH1_INTERMEDIATE", "WC-ADH1", "中间品"));
            exportVO.setAdhesive1AuxMaterialCode(findProductBomItemCode(bomItems, "ADH1_AUX_GLUE_BOARD", "WC-ADH1", "辅料"));
            exportVO.setAdhesive2AuxMaterialCode(findProductBomItemCode(bomItems, "ADH2_AUX_GLUE_BOARD", "WC-ADH2", "辅料"));
            result.add(exportVO);
        }
        return result;
    }

    private String findProductBomItemCode(List<HcBomItemDO> bomItems, String consumeGroupCode,
                                          String issueOperationCode, String componentType) {
        return bomItems.stream()
                .filter(item -> Objects.equals(item.getConsumeGroupCode(), consumeGroupCode))
                .map(HcBomItemDO::getComponentMaterialCode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElseGet(() -> bomItems.stream()
                        .filter(item -> Objects.equals(item.getIssueOperationCode(), issueOperationCode))
                        .filter(item -> Objects.equals(item.getComponentType(), componentType))
                        .map(HcBomItemDO::getComponentMaterialCode)
                        .filter(StringUtils::hasText)
                        .findFirst()
                        .orElse(null));
    }

    @GetMapping("/mes_md_bom_item/list-by-parent-id")
    @Operation(summary = "获得BOM明细列表")
    @Parameter(name = "parentId", description = "BOM ID", required = true)
    public CommonResult<List<HcBomItemDO>> getHcBomItemListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcBomService.getHcBomItemListByParentId(parentId));
    }

}
