package cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateItemSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo.HcPrintFieldTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.printfieldtemplate.HcPrintFieldTemplateDO;
import cn.iocoder.yudao.module.mes.service.hc.printfieldtemplate.HcPrintFieldTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 打印字段模板")
@RestController
@RequestMapping("/mes/hc/execution/print-field-template")
@Validated
public class HcPrintFieldTemplateController {

    @Resource
    private HcPrintFieldTemplateService hcPrintFieldTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建打印字段模板")
    public CommonResult<Long> createHcPrintFieldTemplate(@Valid @RequestBody HcPrintFieldTemplateSaveReqVO createReqVO) {
        return success(hcPrintFieldTemplateService.createHcPrintFieldTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新打印字段模板")
    public CommonResult<Boolean> updateHcPrintFieldTemplate(@Valid @RequestBody HcPrintFieldTemplateSaveReqVO updateReqVO) {
        hcPrintFieldTemplateService.updateHcPrintFieldTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除打印字段模板")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deleteHcPrintFieldTemplate(@RequestParam("id") Long id) {
        hcPrintFieldTemplateService.deleteHcPrintFieldTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得打印字段模板")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcPrintFieldTemplateRespVO> getHcPrintFieldTemplate(@RequestParam("id") Long id) {
        HcPrintFieldTemplateDO entity = hcPrintFieldTemplateService.getHcPrintFieldTemplate(id);
        return success(BeanUtils.toBean(entity, HcPrintFieldTemplateRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得打印字段模板详情")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<HcPrintFieldTemplateRespVO> getHcPrintFieldTemplateDetail(@RequestParam("id") Long id) {
        return success(hcPrintFieldTemplateService.getHcPrintFieldTemplateDetail(id));
    }

    @GetMapping("/page")
    @Operation(summary = "获得打印字段模板分页")
    public CommonResult<PageResult<HcPrintFieldTemplateRespVO>> getHcPrintFieldTemplatePage(@Valid HcPrintFieldTemplatePageReqVO pageReqVO) {
        PageResult<HcPrintFieldTemplateDO> pageResult = hcPrintFieldTemplateService.getHcPrintFieldTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcPrintFieldTemplateRespVO.class));
    }

    @GetMapping("/items")
    @Operation(summary = "获得打印字段模板明细")
    @Parameter(name = "templateId", description = "模板编号", required = true)
    public CommonResult<List<HcPrintFieldTemplateItemRespVO>> getItems(@RequestParam("templateId") Long templateId) {
        return success(hcPrintFieldTemplateService.getItemsByTemplateId(templateId));
    }

    @PutMapping("/items")
    @Operation(summary = "更新打印字段模板明细")
    public CommonResult<Boolean> updateItems(@RequestParam("templateId") Long templateId,
            @Valid @RequestBody List<HcPrintFieldTemplateItemSaveReqVO> items) {
        hcPrintFieldTemplateService.updateItems(templateId, items);
        return success(true);
    }

    @GetMapping("/active")
    @Operation(summary = "获得启用的打印字段模板")
    public CommonResult<HcPrintFieldTemplateRespVO> getActiveTemplate(@RequestParam("templateCode") String templateCode) {
        return success(hcPrintFieldTemplateService.getActiveTemplate(templateCode));
    }

}
