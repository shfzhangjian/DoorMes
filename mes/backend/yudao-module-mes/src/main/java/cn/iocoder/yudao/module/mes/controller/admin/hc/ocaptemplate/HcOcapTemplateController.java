package cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplateDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplateSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo.HcOcapTemplateSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.ocaptemplate.HcOcapTemplateDO;
import cn.iocoder.yudao.module.mes.service.hc.ocaptemplate.HcOcapTemplateService;
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

@Tag(name = "???? - OCAP模板")
@RestController
@RequestMapping("/mes/hc/base/ocap-template")
@Validated
public class HcOcapTemplateController {

    @Resource
    private HcOcapTemplateService hcOcapTemplateService;

    @PostMapping("/create")
    @Operation(summary = "??OCAP模板")
    public CommonResult<Long> createHcOcapTemplate(@Valid @RequestBody HcOcapTemplateSaveReqVO createReqVO) {
        return success(hcOcapTemplateService.createHcOcapTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??OCAP模板")
    public CommonResult<Boolean> updateHcOcapTemplate(@Valid @RequestBody HcOcapTemplateSaveReqVO updateReqVO) {
        hcOcapTemplateService.updateHcOcapTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??OCAP模板")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcOcapTemplate(@RequestParam("id") Long id) {
        hcOcapTemplateService.deleteHcOcapTemplate(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????OCAP模板")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcOcapTemplateList(@RequestParam("ids") List<Long> ids) {
        hcOcapTemplateService.deleteHcOcapTemplateListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??OCAP模板??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcOcapTemplateRespVO> getHcOcapTemplate(@RequestParam("id") Long id) {
        HcOcapTemplateDO entity = hcOcapTemplateService.getHcOcapTemplate(id);
        return success(BeanUtils.toBean(entity, HcOcapTemplateRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??OCAP模板????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcOcapTemplateDetailRespVO> getHcOcapTemplateDetail(@RequestParam("id") Long id) {
        HcOcapTemplateDO entity = hcOcapTemplateService.getHcOcapTemplate(id);
        HcOcapTemplateDetailRespVO respVO = BeanUtils.toBean(entity, HcOcapTemplateDetailRespVO.class);
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??OCAP模板????")
    public CommonResult<List<HcOcapTemplateSimpleRespVO>> getHcOcapTemplateSimpleList() {
        List<HcOcapTemplateDO> list = hcOcapTemplateService.getHcOcapTemplateSimpleList();
        return success(BeanUtils.toBean(list, HcOcapTemplateSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??OCAP模板????")
    public CommonResult<Map<Long, String>> getHcOcapTemplateSimpleMap() {
        List<HcOcapTemplateDO> list = hcOcapTemplateService.getHcOcapTemplateSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getOcapName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??OCAP模板????")
    public CommonResult<List<HcOcapTemplateSelectOptionRespVO>> getHcOcapTemplateSelectOptions() {
        List<HcOcapTemplateDO> list = hcOcapTemplateService.getHcOcapTemplateSimpleList();
        List<HcOcapTemplateSelectOptionRespVO> result = new ArrayList<>();
        for (HcOcapTemplateDO item : list) {
            HcOcapTemplateSelectOptionRespVO option = new HcOcapTemplateSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getOcapName());
            option.setCode(item.getOcapCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??OCAP模板??")
    public CommonResult<List<HcOcapTemplateRespVO>> getHcOcapTemplateList(@Valid HcOcapTemplatePageReqVO reqVO) {
        List<HcOcapTemplateDO> list = hcOcapTemplateService.getHcOcapTemplateList(reqVO);
        return success(BeanUtils.toBean(list, HcOcapTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??OCAP模板??")
    public CommonResult<PageResult<HcOcapTemplateRespVO>> getHcOcapTemplatePage(@Valid HcOcapTemplatePageReqVO pageReqVO) {
        PageResult<HcOcapTemplateDO> pageResult = hcOcapTemplateService.getHcOcapTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcOcapTemplateRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??OCAP模板 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcOcapTemplateExcel(@Valid HcOcapTemplatePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcOcapTemplateDO> list = hcOcapTemplateService.getHcOcapTemplatePage(pageReqVO).getList();
        ExcelUtils.write(response, "OCAP模板.xls", "??", HcOcapTemplateRespVO.class, BeanUtils.toBean(list, HcOcapTemplateRespVO.class));
    }

}