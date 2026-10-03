package cn.iocoder.yudao.module.mes.controller.admin.plan;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.mes.controller.admin.plan.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.plan.PlanDO;
import cn.iocoder.yudao.module.mes.service.plan.PlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 主生产计划")
@RestController
@RequestMapping("/mes/plan")
@Validated
public class PlanController {

    @Resource
    private PlanService planService;

    @PostMapping("/create")
    @Operation(summary = "创建主生产计划")
    public CommonResult<Long> createPlan(@Valid @RequestBody PlanCreateReqVO createReqVO) {
        return success(planService.createPlan(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新主生产计划")
    public CommonResult<Boolean> updatePlan(@Valid @RequestBody PlanUpdateReqVO updateReqVO) {
        planService.updatePlan(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除主生产计划")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<Boolean> deletePlan(@RequestParam("id") Long id) {
        planService.deletePlan(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得主生产计划")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    public CommonResult<PlanRespVO> getPlan(@RequestParam("id") Long id) {
        PlanDO plan = planService.getPlan(id);
        return success(BeanUtils.toBean(plan, PlanRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得主生产计划分页")
    public CommonResult<PageResult<PlanRespVO>> getPlanPage(@Valid PlanPageReqVO pageReqVO) {
        PageResult<PlanDO> pageResult = planService.getPlanPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, PlanRespVO.class));
    }
}
