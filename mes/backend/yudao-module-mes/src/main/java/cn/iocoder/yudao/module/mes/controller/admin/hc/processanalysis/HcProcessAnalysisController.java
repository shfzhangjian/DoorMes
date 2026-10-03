package cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisConfigRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisConfigSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processanalysis.vo.HcProcessAnalysisFieldRespVO;
import cn.iocoder.yudao.module.mes.service.hc.planorder.HcPlanOrderService;
import cn.iocoder.yudao.module.mes.service.hc.processanalysis.HcProcessAnalysisService;
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

@Tag(name = "管理后台 - 计划排程统计分析")
@RestController
@RequestMapping("/mes/hc/plan/process-analysis")
@Validated
public class HcProcessAnalysisController {

    @Resource
    private HcProcessAnalysisService hcProcessAnalysisService;
    @Resource
    private HcPlanOrderService hcPlanOrderService;

    @GetMapping("/field-catalog")
    @Operation(summary = "获取生产进度分析字段目录")
    public CommonResult<List<HcProcessAnalysisFieldRespVO>> getFieldCatalog() {
        return success(hcProcessAnalysisService.getFieldCatalog());
    }

    @GetMapping("/source-data")
    @Operation(summary = "按生产进度口径获取统计分析源数据")
    public CommonResult<List<HcPlanProcessPivotRespVO>> getSourceData(
            @Valid HcPlanProcessPivotPageReqVO reqVO) {
        return success(hcPlanOrderService.getPlanProcessPivotList(reqVO));
    }

    @GetMapping("/config/list")
    @Operation(summary = "获取当前用户可见的统计分析方案")
    public CommonResult<List<HcProcessAnalysisConfigRespVO>> getConfigList() {
        return success(hcProcessAnalysisService.getVisibleConfigList());
    }

    @GetMapping("/config/get")
    @Operation(summary = "获取统计分析方案")
    @Parameter(name = "id", description = "方案ID", required = true)
    public CommonResult<HcProcessAnalysisConfigRespVO> getConfig(@RequestParam("id") Long id) {
        return success(hcProcessAnalysisService.getVisibleConfig(id));
    }

    @PostMapping("/config/create")
    @Operation(summary = "创建统计分析方案")
    public CommonResult<Long> createConfig(
            @Valid @RequestBody HcProcessAnalysisConfigSaveReqVO reqVO) {
        return success(hcProcessAnalysisService.createConfig(reqVO));
    }

    @PutMapping("/config/update")
    @Operation(summary = "更新统计分析方案")
    public CommonResult<Boolean> updateConfig(
            @Valid @RequestBody HcProcessAnalysisConfigSaveReqVO reqVO) {
        hcProcessAnalysisService.updateConfig(reqVO);
        return success(true);
    }

    @DeleteMapping("/config/delete")
    @Operation(summary = "删除统计分析方案")
    @Parameter(name = "id", description = "方案ID", required = true)
    public CommonResult<Boolean> deleteConfig(@RequestParam("id") Long id) {
        hcProcessAnalysisService.deleteConfig(id);
        return success(true);
    }

}
