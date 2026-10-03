package cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.inventoryanalysis.vo.HcInventoryAnalysisOverviewRespVO;
import cn.iocoder.yudao.module.mes.service.hc.inventoryanalysis.HcInventoryAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 库存分析报表")
@RestController
@RequestMapping("/mes/hc/inv/analysis")
@Validated
public class HcInventoryAnalysisController {

    @Resource
    private HcInventoryAnalysisService hcInventoryAnalysisService;

    @GetMapping("/overview")
    @Operation(summary = "查询库存分析报表总览")
    public CommonResult<HcInventoryAnalysisOverviewRespVO> getOverview(
            @Valid @ModelAttribute HcInventoryAnalysisOverviewReqVO reqVO) {
        return success(hcInventoryAnalysisService.getOverview(reqVO));
    }

}
