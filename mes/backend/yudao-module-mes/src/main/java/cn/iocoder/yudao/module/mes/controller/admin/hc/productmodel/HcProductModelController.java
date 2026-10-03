package cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelGenerateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelMaterialRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productmodel.vo.HcProductModelSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelDO;
import cn.iocoder.yudao.module.mes.service.hc.productmodel.HcProductModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
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

@Tag(name = "管理后台 - 产品型号字典")
@RestController
@RequestMapping("/mes/hc/base/product-model")
@Validated
public class HcProductModelController {

    @Resource
    private HcProductModelService productModelService;

    @PostMapping("/create")
    @Operation(summary = "创建产品型号字典")
    public CommonResult<Long> createHcProductModel(@Valid @RequestBody HcProductModelSaveReqVO createReqVO) {
        return success(productModelService.createHcProductModel(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新产品型号字典")
    public CommonResult<Boolean> updateHcProductModel(@Valid @RequestBody HcProductModelSaveReqVO updateReqVO) {
        productModelService.updateHcProductModel(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除产品型号字典")
    @Parameter(name = "id", description = "主键ID", required = true)
    public CommonResult<Boolean> deleteHcProductModel(@RequestParam("id") Long id) {
        productModelService.deleteHcProductModel(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除产品型号字典")
    @Parameter(name = "ids", description = "主键ID列表", required = true)
    public CommonResult<Boolean> deleteHcProductModelList(@RequestParam("ids") List<Long> ids) {
        productModelService.deleteHcProductModelListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得产品型号字典")
    public CommonResult<HcProductModelRespVO> getHcProductModel(@RequestParam("id") Long id) {
        return success(BeanUtils.toBean(productModelService.getHcProductModel(id), HcProductModelRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得产品型号字典详情")
    public CommonResult<HcProductModelDetailRespVO> getHcProductModelDetail(@RequestParam("id") Long id) {
        HcProductModelDO entity = productModelService.getHcProductModel(id);
        HcProductModelDetailRespVO respVO = BeanUtils.toBean(entity, HcProductModelDetailRespVO.class);
        respVO.setModelSegments(BeanUtils.toBean(productModelService.getSegmentListByModelId(id), HcProductModelSegmentRespVO.class));
        respVO.setModelMaterials(BeanUtils.toBean(productModelService.getMaterialListByModelId(id), HcProductModelMaterialRespVO.class));
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得产品型号字典分页")
    public CommonResult<PageResult<HcProductModelRespVO>> getHcProductModelPage(@Valid HcProductModelPageReqVO pageReqVO) {
        PageResult<HcProductModelDO> pageResult = productModelService.getHcProductModelPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcProductModelRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得产品型号字典列表")
    public CommonResult<List<HcProductModelRespVO>> getHcProductModelList(@Valid HcProductModelPageReqVO reqVO) {
        return success(BeanUtils.toBean(productModelService.getHcProductModelList(reqVO), HcProductModelRespVO.class));
    }

    @GetMapping("/select-options")
    @Operation(summary = "获得产品型号选择项")
    public CommonResult<List<HcProductModelSelectOptionRespVO>> getSelectOptions(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "modelLevel", required = false) String modelLevel) {
        return success(productModelService.getSelectOptions(keyword, modelLevel));
    }

    @PostMapping("/generate-code")
    @Operation(summary = "根据规则段值生成产品型号编码")
    public CommonResult<HcProductModelGenerateRespVO> generateModelCode(@Valid @RequestBody HcProductModelGenerateReqVO reqVO) {
        HcProductModelGenerateRespVO respVO = new HcProductModelGenerateRespVO();
        respVO.setGeneratedCode(productModelService.generateModelCode(reqVO));
        return success(respVO);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品型号字典 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcProductModelExcel(@Valid HcProductModelPageReqVO pageReqVO,
                                          HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcProductModelDO> list = productModelService.getHcProductModelPage(pageReqVO).getList();
        ExcelUtils.write(response, "产品型号字典.xls", "产品型号字典", HcProductModelRespVO.class, BeanUtils.toBean(list, HcProductModelRespVO.class));
    }

}
