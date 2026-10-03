package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterAdjustReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterInitializeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterPreviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleCounterSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleMatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleMatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRulePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleParseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleParseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleSimpleRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleCounterDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleSegmentDO;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleService;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleMatchContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
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

@Tag(name = "管理后台 - 批号规则")
@RestController
@RequestMapping("/mes/hc/base/lot-rule")
@Validated
public class HcLotRuleController {

    @Resource
    private HcLotRuleService hcLotRuleService;

    @PostMapping("/create")
    @Operation(summary = "新增批号规则")
    public CommonResult<Long> createHcLotRule(@Valid @RequestBody HcLotRuleSaveReqVO createReqVO) {
        return success(hcLotRuleService.createHcLotRule(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改批号规则")
    public CommonResult<Boolean> updateHcLotRule(@Valid @RequestBody HcLotRuleSaveReqVO updateReqVO) {
        hcLotRuleService.updateHcLotRule(updateReqVO);
        return success(true);
    }

    @PostMapping("/publish")
    @Operation(summary = "发布并启用批号规则")
    public CommonResult<Boolean> publishHcLotRule(@RequestParam("id") Long id) {
        hcLotRuleService.publishHcLotRule(id);
        return success(true);
    }

    @PostMapping("/disable")
    @Operation(summary = "停用批号规则")
    public CommonResult<Boolean> disableHcLotRule(@RequestParam("id") Long id) {
        hcLotRuleService.disableHcLotRule(id);
        return success(true);
    }

    @PostMapping("/copy-new-version")
    @Operation(summary = "复制为批号规则新版本")
    public CommonResult<Long> copyHcLotRuleAsNewVersion(@RequestParam("id") Long id) {
        return success(hcLotRuleService.copyAsNewVersion(id));
    }

    @PostMapping("/match")
    @Operation(summary = "测试批号规则匹配")
    public CommonResult<HcLotRuleMatchRespVO> matchHcLotRule(@Valid @RequestBody HcLotRuleMatchReqVO reqVO) {
        HcLotRuleDO rule = hcLotRuleService.matchEnabledRule(HcLotRuleMatchContext.builder()
                .bizType(reqVO.getBizType())
                .productCategoryCode(reqVO.getProductCategoryCode())
                .prodType(reqVO.getProdType())
                .generationTrigger(reqVO.getGenerationTrigger())
                .generationScope(reqVO.getGenerationScope())
                .modelCode(reqVO.getModelCode())
                .build());
        HcLotRuleMatchRespVO respVO = BeanUtils.toBean(rule, HcLotRuleMatchRespVO.class);
        respVO.setRuleId(rule.getId());
        respVO.setMessage("匹配成功：后续预览与正式开工将使用该规则版本");
        return success(respVO);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除批号规则")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<Boolean> deleteHcLotRule(@RequestParam("id") Long id) {
        hcLotRuleService.deleteHcLotRule(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除批号规则")
    @Parameter(name = "ids", description = "主键列表", required = true)
    public CommonResult<Boolean> deleteHcLotRuleList(@RequestParam("ids") List<Long> ids) {
        hcLotRuleService.deleteHcLotRuleListByIds(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得批号规则")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<HcLotRuleRespVO> getHcLotRule(@RequestParam("id") Long id) {
        HcLotRuleDO entity = hcLotRuleService.getHcLotRule(id);
        return success(BeanUtils.toBean(entity, HcLotRuleRespVO.class));
    }

    @GetMapping("/get-detail")
    @Operation(summary = "获得批号规则详情")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<HcLotRuleDetailRespVO> getHcLotRuleDetail(@RequestParam("id") Long id) {
        HcLotRuleDO entity = hcLotRuleService.getHcLotRule(id);
        HcLotRuleDetailRespVO respVO = BeanUtils.toBean(entity, HcLotRuleDetailRespVO.class);
        respVO.setLotRuleSegments(hcLotRuleService.getHcLotRuleSegmentListByParentId(id));
        return success(respVO);
    }

    @PostMapping("/generate")
    @Operation(summary = "生成批号/序列批次")
    public CommonResult<HcLotRuleGenerateRespVO> generateLotNo(@Valid @RequestBody HcLotRuleGenerateReqVO reqVO) {
        Map<String, Object> result = hcLotRuleService.generateLotNo(reqVO);
        HcLotRuleGenerateRespVO respVO = new HcLotRuleGenerateRespVO();
        respVO.setLotNo((String) result.get("lotNo"));
        Object currentSeq = result.get("currentSeq");
        if (currentSeq instanceof Number number) {
            respVO.setCurrentSeq(number.intValue());
        }
        Object nextSeq = result.get("nextSeq");
        if (nextSeq instanceof Number number) {
            respVO.setNextSeq(number.intValue());
        }
        Object segmentValues = result.get("segmentValues");
        if (segmentValues instanceof Map<?, ?> rawMap) {
            Map<String, String> parsedMap = new LinkedHashMap<>();
            rawMap.forEach((key, value) -> parsedMap.put(String.valueOf(key), value == null ? "" : String.valueOf(value)));
            respVO.setSegmentValues(parsedMap);
        }
        return success(respVO);
    }

    @PostMapping("/parse")
    @Operation(summary = "解析批号/序列批次")
    public CommonResult<HcLotRuleParseRespVO> parseLotNo(@Valid @RequestBody HcLotRuleParseReqVO reqVO) {
        HcLotRuleParseRespVO respVO = new HcLotRuleParseRespVO();
        respVO.setLotNo(reqVO.getLotNo());
        respVO.setSegmentValues(hcLotRuleService.parseLotNo(reqVO));
        return success(respVO);
    }

    @GetMapping("/counter/page")
    @Operation(summary = "获得批号规则流水台账分页")
    public CommonResult<PageResult<HcLotRuleCounterRespVO>> getHcLotRuleCounterPage(@Valid HcLotRuleCounterPageReqVO pageReqVO) {
        PageResult<HcLotRuleCounterDO> pageResult = hcLotRuleService.getHcLotRuleCounterPage(pageReqVO);
        List<HcLotRuleCounterRespVO> result = BeanUtils.toBean(pageResult.getList(), HcLotRuleCounterRespVO.class);
        result.forEach(item -> {
            item.setCurrentValue(item.getCurrentSeq());
            HcLotRuleDO rule = item.getRuleId() == null ? null : hcLotRuleService.getHcLotRule(item.getRuleId());
            item.setRuleName(rule == null ? null : rule.getRuleName());
        });
        return success(new PageResult<>(result, pageResult.getTotal()));
    }

    @GetMapping("/counter/list-by-rule-id")
    @Operation(summary = "获得批号规则流水记录列表")
    @Parameter(name = "ruleId", description = "规则ID", required = true)
    public CommonResult<List<HcLotRuleCounterRespVO>> getHcLotRuleCounterListByRuleId(@RequestParam("ruleId") Long ruleId) {
        HcLotRuleDO rule = hcLotRuleService.getHcLotRule(ruleId);
        List<HcLotRuleCounterDO> list = hcLotRuleService.getHcLotRuleCounterListByRuleId(ruleId);
        List<HcLotRuleCounterRespVO> result = BeanUtils.toBean(list, HcLotRuleCounterRespVO.class);
        result.forEach(item -> item.setRuleName(rule == null ? null : rule.getRuleName()));
        return success(result);
    }

    @GetMapping("/counter/summary-list")
    @Operation(summary = "获得规则当前年度流水摘要")
    @Parameter(name = "ruleId", description = "规则ID", required = true)
    public CommonResult<List<HcLotRuleCounterRespVO>> getHcLotRuleCounterSummaryList(
            @RequestParam("ruleId") Long ruleId,
            @RequestParam(value = "year", required = false) Integer year) {
        HcLotRuleDO rule = hcLotRuleService.getHcLotRule(ruleId);
        if (rule == null) {
            return success(List.of());
        }
        int statisticalYear = year == null ? LocalDate.now().getYear() : year;
        HcLotRuleCounterPreviewReqVO previewReqVO = new HcLotRuleCounterPreviewReqVO();
        previewReqVO.setRuleId(ruleId);
        previewReqVO.setYear(statisticalYear);
        HcLotRuleCounterPreviewRespVO preview = hcLotRuleService.previewNextLotRuleCounter(previewReqVO);
        HcLotRuleCounterDO counter = hcLotRuleService.getHcLotRuleCounterListByRuleId(ruleId).stream()
                .filter(item -> preview.getCounterKey().equals(item.getCounterKey()))
                .findFirst()
                .orElse(null);
        HcLotRuleCounterRespVO summary = counter == null
                ? new HcLotRuleCounterRespVO()
                : BeanUtils.toBean(counter, HcLotRuleCounterRespVO.class);
        summary.setRuleId(ruleId);
        summary.setRuleCode(rule.getRuleCode());
        summary.setRuleName(rule.getRuleName());
        summary.setCounterType(preview.getCounterType());
        summary.setStatisticalYear(statisticalYear);
        summary.setCounterKey(preview.getCounterKey());
        summary.setResetKey(preview.getResetKey());
        summary.setBizDimensionKey(preview.getBizDimensionKey());
        summary.setCurrentSeq(preview.getCurrentSeq());
        summary.setCurrentValue(preview.getCurrentSeq());
        summary.setNextSeq(preview.getNextSeq());
        summary.setNextLotNo(preview.getNextLotNo());
        summary.setInitialized(counter != null);
        summary.setWarning(preview.getWarning());
        return success(List.of(summary));
    }

    @PostMapping("/counter/create")
    @Operation(summary = "已废弃：不允许直接新增流水记录")
    public CommonResult<Long> createHcLotRuleCounter(@Valid @RequestBody HcLotRuleCounterSaveReqVO createReqVO) {
        return success(hcLotRuleService.createHcLotRuleCounter(createReqVO));
    }

    @PutMapping("/counter/update")
    @Operation(summary = "已废弃：不允许直接编辑流水记录")
    public CommonResult<Boolean> updateHcLotRuleCounter(@Valid @RequestBody HcLotRuleCounterSaveReqVO updateReqVO) {
        hcLotRuleService.updateHcLotRuleCounter(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/counter/delete")
    @Operation(summary = "已废弃：不允许删除流水记录")
    @Parameter(name = "id", description = "主键", required = true)
    public CommonResult<Boolean> deleteHcLotRuleCounter(@RequestParam("id") Long id) {
        hcLotRuleService.deleteHcLotRuleCounter(id);
        return success(true);
    }

    @PostMapping("/counter/initialize")
    @Operation(summary = "初始化批号规则年度流水")
    public CommonResult<HcLotRuleCounterRespVO> initializeHcLotRuleCounter(
            @Valid @RequestBody HcLotRuleCounterInitializeReqVO reqVO) {
        return success(toCounterRespVO(hcLotRuleService.initializeHcLotRuleCounter(reqVO)));
    }

    @PostMapping("/counter/adjust")
    @Operation(summary = "人工调整批号规则流水")
    public CommonResult<HcLotRuleCounterRespVO> adjustHcLotRuleCounter(
            @Valid @RequestBody HcLotRuleCounterAdjustReqVO reqVO) {
        return success(toCounterRespVO(hcLotRuleService.adjustHcLotRuleCounter(reqVO)));
    }

    @GetMapping("/counter/preview-next")
    @Operation(summary = "预览批号规则下一母批号")
    public CommonResult<HcLotRuleCounterPreviewRespVO> previewNextLotRuleCounter(
            @Valid HcLotRuleCounterPreviewReqVO reqVO) {
        return success(hcLotRuleService.previewNextLotRuleCounter(reqVO));
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获得批号规则精简列表")
    public CommonResult<List<HcLotRuleSimpleRespVO>> getHcLotRuleSimpleList() {
        List<HcLotRuleDO> list = hcLotRuleService.getHcLotRuleSimpleList();
        return success(BeanUtils.toBean(list, HcLotRuleSimpleRespVO.class));
    }

    @GetMapping("/simple-map")
    @Operation(summary = "获得批号规则精简映射")
    public CommonResult<Map<Long, String>> getHcLotRuleSimpleMap() {
        List<HcLotRuleDO> list = hcLotRuleService.getHcLotRuleSimpleList();
        Map<Long, String> result = new LinkedHashMap<>();
        list.forEach(item -> result.put(item.getId(), item.getRuleName()));
        return success(result);
    }

    @GetMapping("/select-options")
    @Operation(summary = "获得批号规则下拉选项")
    public CommonResult<List<HcLotRuleSelectOptionRespVO>> getHcLotRuleSelectOptions() {
        List<HcLotRuleDO> list = hcLotRuleService.getHcLotRuleSimpleList();
        List<HcLotRuleSelectOptionRespVO> result = new ArrayList<>();
        for (HcLotRuleDO item : list) {
            HcLotRuleSelectOptionRespVO option = new HcLotRuleSelectOptionRespVO();
            option.setValue(item.getId());
            option.setLabel(item.getRuleName());
            option.setCode(item.getRuleCode());
            option.setStatus(item.getStatus());
            result.add(option);
        }
        return success(result);
    }

    @GetMapping("/list")
    @Operation(summary = "获得批号规则列表")
    public CommonResult<List<HcLotRuleRespVO>> getHcLotRuleList(@Valid HcLotRulePageReqVO reqVO) {
        List<HcLotRuleDO> list = hcLotRuleService.getHcLotRuleList(reqVO);
        return success(BeanUtils.toBean(list, HcLotRuleRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得批号规则分页")
    public CommonResult<PageResult<HcLotRuleRespVO>> getHcLotRulePage(@Valid HcLotRulePageReqVO pageReqVO) {
        PageResult<HcLotRuleDO> pageResult = hcLotRuleService.getHcLotRulePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, HcLotRuleRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出批号规则 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportHcLotRuleExcel(@Valid HcLotRulePageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<HcLotRuleDO> list = hcLotRuleService.getHcLotRulePage(pageReqVO).getList();
        ExcelUtils.write(response, "批号规则.xls", "批号规则", HcLotRuleRespVO.class, BeanUtils.toBean(list, HcLotRuleRespVO.class));
    }

    @GetMapping("/mes_md_lot_rule_segment/list-by-parent-id")
    @Operation(summary = "获得批号规则分段列表")
    @Parameter(name = "parentId", description = "规则ID", required = true)
    public CommonResult<List<HcLotRuleSegmentDO>> getHcLotRuleSegmentListByParentId(@RequestParam("parentId") Long parentId) {
        return success(hcLotRuleService.getHcLotRuleSegmentListByParentId(parentId));
    }

    private HcLotRuleCounterRespVO toCounterRespVO(HcLotRuleCounterDO counter) {
        HcLotRuleCounterRespVO respVO = BeanUtils.toBean(counter, HcLotRuleCounterRespVO.class);
        respVO.setCurrentValue(respVO.getCurrentSeq());
        HcLotRuleDO rule = respVO.getRuleId() == null ? null : hcLotRuleService.getHcLotRule(respVO.getRuleId());
        respVO.setRuleName(rule == null ? null : rule.getRuleName());
        return respVO;
    }
}
