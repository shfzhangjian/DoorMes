package cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplateDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplateSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo.HcFormTemplateSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateVersionDO;
import cn.iocoder.yudao.module.mes.service.hc.formtemplate.HcFormTemplateService;
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

@Tag(name = "???? - 表单模板")
@RestController
@RequestMapping("/mes/hc/base/form-template")
@Validated
public class HcFormTemplateController {

    @Resource
    private HcFormTemplateService hcFormTemplateService;

    @PostMapping("/create")
    @Operation(summary = "??表单模板")
    public CommonResult<Long> createHcFormTemplate(@Valid @RequestBody HcFormTemplateSaveReqVO createReqVO) {
        return success(hcFormTemplateService.createHcFormTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??表单模板")
    public CommonResult<Boolean> updateHcFormTemplate(@Valid @RequestBody HcFormTemplateSaveReqVO updateReqVO) {
        hcFormTemplateService.updateHcFormTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??表单模板")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcFormTemplate(@RequestParam("id") Long id) {
        hcFormTemplateService.deleteHcFormTemplate(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????表单模板")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcFormTemplateList(@RequestParam("ids") List<Long> ids) {
        hcFormTemplateService.deleteHcFormTemplateListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??表单模板??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcFormTemplateRespVO> getHcFormTemplate(@RequestParam("id") Long id) {
        HcFormTemplateDO entity = hcFormTemplateService.getHcFormTemplate(id);
        return success(BeanUtils.toBean(entity, HcFormTemplateRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??表单模板????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcFormTemplateDetailRespVO> getHcFormTemplateDetail(@RequestParam("id") Long id) {
        HcFormTemplateDO entity = hcFormTemplateService.getHcFormTemplate(id);
        HcFormTemplateDetailRespVO respVO = BeanUtils.toBean(entity, HcFormTemplateDetailRespVO.class);
        respVO.setTemplateVersions(hcFormTemplateService.getHcFormTemplateVersionListByParentId(id));
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??表单模板????")
    public CommonResult<List<HcFormTemplateSimpleRespVO>> getHcFormTemplateSimpleList() {
        List<HcFormTemplateDO> list = hcFormTemplateService.getHcFormTemplateSimpleList();
        return success(BeanUtils.toBean(list, HcFormTemplateSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??表单模板????")
    public CommonResult<Map<Long, String>> getHcFormTemplateSimpleMap() {
        List<HcFormTemplateDO> list = hcFormTemplateService.getHcFormTemplateSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getTemplateName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??表单模板????")
    public CommonResult<List<HcFormTemplateSelectOptionRespVO>> getHcFormTemplateSelectOptions() {
        List<HcFormTemplateDO> list = hcFormTemplateService.getHcFormTemplateSimpleList();
        List<HcFormTemplateSelectOptionRespVO> result = new ArrayList<>();
        for (HcFormTemplateDO item : list) {
            HcFormTemplateSelectOptionRespVO option = new HcFormTemplateSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getTemplateName());
            option.setCode(item.getTemplateCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??表单模板??")
    public CommonResult<List<HcFormTemplateRespVO>> getHcFormTemplateList(@Valid HcFormTemplatePageReqVO reqVO) {
        List<HcFormTemplateDO> list = hcFormTemplateService.getHcFormTemplateList(reqVO);
        return success(BeanUtils.toBean(list, HcFormTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??表单模板??")
    public CommonResult<PageResult<HcFormTemplateRespVO>> getHcFormTemplatePage(@Valid HcFormTemplatePageReqVO pageReqVO) {
        PageResult<HcFormTemplateDO> pageResult = hcFormTemplateService.getHcFormTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcFormTemplateRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??表单模板 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcFormTemplateExcel(@Valid HcFormTemplatePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcFormTemplateDO> list = hcFormTemplateService.getHcFormTemplatePage(pageReqVO).getList();
        ExcelUtils.write(response, "表单模板.xls", "??", HcFormTemplateRespVO.class, BeanUtils.toBean(list, HcFormTemplateRespVO.class));
    }

    @GetMapping("/mes_form_template_version/list-by-parent-id")
    @Operation(summary = "??模板版本??")
    @Parameter(name = "parentId", description = "??ID", required = true)
    public CommonResult<List<HcFormTemplateVersionDO>> getHcFormTemplateVersionListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcFormTemplateService.getHcFormTemplateVersionListByParentId(parentId));
    }

}