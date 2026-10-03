package cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleGenerateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.modelrule.vo.HcModelRuleSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleDictDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule.HcModelRuleItemDO;
import cn.iocoder.yudao.module.mes.service.hc.modelrule.HcModelRuleService;
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

@Tag(name = "???? - 型号编码规则")
@RestController
@RequestMapping("/mes/hc/base/model-rule")
@Validated
public class HcModelRuleController {

    @Resource
    private HcModelRuleService hcModelRuleService;

    @PostMapping("/create")
    @Operation(summary = "??型号编码规则")
    public CommonResult<Long> createHcModelRule(@Valid @RequestBody HcModelRuleSaveReqVO createReqVO) {
        return success(hcModelRuleService.createHcModelRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "??型号编码规则")
    public CommonResult<Boolean> updateHcModelRule(@Valid @RequestBody HcModelRuleSaveReqVO updateReqVO) {
        hcModelRuleService.updateHcModelRule(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "??型号编码规则")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<Boolean> deleteHcModelRule(@RequestParam("id") Long id) {
        hcModelRuleService.deleteHcModelRule(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "????型号编码规则")
    @Parameter(name = "ids", description = "??", required = true)
    public CommonResult<Boolean> deleteHcModelRuleList(@RequestParam("ids") List<Long> ids) {
        hcModelRuleService.deleteHcModelRuleListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "??型号编码规则??")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcModelRuleRespVO> getHcModelRule(@RequestParam("id") Long id) {
        HcModelRuleDO entity = hcModelRuleService.getHcModelRule(id);
        return success(BeanUtils.toBean(entity, HcModelRuleRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "??型号编码规则????")
    @Parameter(name = "id", description = "??", required = true)
    public CommonResult<HcModelRuleDetailRespVO> getHcModelRuleDetail(@RequestParam("id") Long id) {
        HcModelRuleDO entity = hcModelRuleService.getHcModelRule(id);
        HcModelRuleDetailRespVO respVO = BeanUtils.toBean(entity, HcModelRuleDetailRespVO.class);
        respVO.setModelRuleItems(hcModelRuleService.getHcModelRuleItemListByParentId(id));
        respVO.setModelRuleDicts(hcModelRuleService.getHcModelRuleDictListByParentId(id));
        return success(respVO);
    }

    @PostMapping("/generate-code")
    @Operation(summary = "测试生成型号编码")
    public CommonResult<HcModelRuleGenerateRespVO> generateModelRuleCode(@Valid @RequestBody HcModelRuleGenerateReqVO reqVO) {
        HcModelRuleGenerateRespVO respVO = new HcModelRuleGenerateRespVO();
        respVO.setGeneratedCode(hcModelRuleService.generateModelRuleCode(reqVO));
        return success(respVO);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "??型号编码规则????")
    public CommonResult<List<HcModelRuleSimpleRespVO>> getHcModelRuleSimpleList() {
        List<HcModelRuleDO> list = hcModelRuleService.getHcModelRuleSimpleList();
        return success(BeanUtils.toBean(list, HcModelRuleSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "??型号编码规则????")
    public CommonResult<Map<Long, String>> getHcModelRuleSimpleMap() {
        List<HcModelRuleDO> list = hcModelRuleService.getHcModelRuleSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getRuleName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "??型号编码规则????")
    public CommonResult<List<HcModelRuleSelectOptionRespVO>> getHcModelRuleSelectOptions() {
        List<HcModelRuleDO> list = hcModelRuleService.getHcModelRuleSimpleList();
        List<HcModelRuleSelectOptionRespVO> result = new ArrayList<>();
        for (HcModelRuleDO item : list) {
            HcModelRuleSelectOptionRespVO option = new HcModelRuleSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getRuleName());
            option.setCode(item.getRuleCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "??型号编码规则??")
    public CommonResult<List<HcModelRuleRespVO>> getHcModelRuleList(@Valid HcModelRulePageReqVO reqVO) {
        List<HcModelRuleDO> list = hcModelRuleService.getHcModelRuleList(reqVO);
        return success(BeanUtils.toBean(list, HcModelRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "??型号编码规则??")
    public CommonResult<PageResult<HcModelRuleRespVO>> getHcModelRulePage(@Valid HcModelRulePageReqVO pageReqVO) {
        PageResult<HcModelRuleDO> pageResult = hcModelRuleService.getHcModelRulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcModelRuleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "??型号编码规则 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcModelRuleExcel(@Valid HcModelRulePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcModelRuleDO> list = hcModelRuleService.getHcModelRulePage(pageReqVO).getList();
        ExcelUtils.write(response, "型号编码规则.xls", "??", HcModelRuleRespVO.class, BeanUtils.toBean(list, HcModelRuleRespVO.class));
    }

    @GetMapping("/mes_md_model_rule_item/list-by-parent-id")
    @Operation(summary = "??型号规则字段??")
    @Parameter(name = "parentId", description = "??ID", required = true)
    public CommonResult<List<HcModelRuleItemDO>> getHcModelRuleItemListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcModelRuleService.getHcModelRuleItemListByParentId(parentId));
    }

    @GetMapping("/mes_md_model_rule_dict/list-by-parent-id")
    @Operation(summary = "??型号规则字典??")
    @Parameter(name = "parentId", description = "??ID", required = true)
    public CommonResult<List<HcModelRuleDictDO>> getHcModelRuleDictListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcModelRuleService.getHcModelRuleDictListByParentId(parentId));
    }

}
