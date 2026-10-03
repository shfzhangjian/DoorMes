package cn.iocoder.yudao.module.mes.controller.admin.hc.productionreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanProcessPivotRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.productionreport.vo.HcProcessReportOverviewDetailRespVO;
import cn.iocoder.yudao.module.mes.service.hc.planorder.HcPlanOrderService;
import cn.iocoder.yudao.module.mes.service.hc.productionreport.HcProcessReportOverviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/** 工序报工综合报表；所有接口均为只读查询。 */
@Tag(name = "管理后台 - 工序报工综合报表")
@RestController
@RequestMapping("/mes/hc/plan/production-report")
@Validated
public class HcProcessReportOverviewController {

    @Resource
    private HcPlanOrderService planOrderService;
    @Resource
    private HcProcessReportOverviewService overviewService;

    @GetMapping("/page")
    @Operation(summary = "获得工序报工综合报表分页")
    public CommonResult<cn.iocoder.yudao.framework.common.pojo.PageResult<HcPlanProcessPivotRespVO>> getPage(
            @Valid HcPlanProcessPivotPageReqVO reqVO) {
        return success(planOrderService.getPlanProcessPivotPage(reqVO));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得计划的工序报工、表单、质量和流转详情")
    @Parameter(name = "planId", description = "生产计划ID", required = true)
    public CommonResult<HcProcessReportOverviewDetailRespVO> getDetail(@RequestParam("planId") Long planId) {
        return success(overviewService.getDetail(planId));
    }
}
