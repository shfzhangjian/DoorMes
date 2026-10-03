package cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitCreateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo.HcPlanSplitGraphRespVO;
import cn.iocoder.yudao.module.mes.service.hc.plansplit.HcPlanSplitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - HC 拆批管理")
@RestController
@RequestMapping("/mes/hc/execution/plan-split")
@Validated
public class HcPlanSplitController {

    @Resource
    private HcPlanSplitService hcPlanSplitService;

    @GetMapping("/graph")
    @Operation(summary = "查询计划拆批图谱")
    public CommonResult<HcPlanSplitGraphRespVO> getPlanSplitGraph(@RequestParam("planNo") String planNo) {
        return success(hcPlanSplitService.getPlanSplitGraph(planNo));
    }

    @PostMapping("/split")
    @Operation(summary = "创建拆批计划")
    public CommonResult<HcPlanSplitCreateRespVO> createPlanSplit(@Valid @RequestBody HcPlanSplitCreateReqVO reqVO) {
        return success(hcPlanSplitService.createPlanSplit(reqVO));
    }

}
