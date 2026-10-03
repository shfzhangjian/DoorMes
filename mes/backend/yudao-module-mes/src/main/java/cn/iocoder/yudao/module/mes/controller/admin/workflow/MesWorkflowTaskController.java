package cn.iocoder.yudao.module.mes.controller.admin.workflow;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskPageReqVO;
import cn.iocoder.yudao.module.bpm.controller.admin.task.vo.task.BpmTaskRespVO;
import cn.iocoder.yudao.module.mes.service.workflow.MesWorkflowTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - MES流程工作台")
@RestController
@RequestMapping("/mes/workflow/task")
@Validated
public class MesWorkflowTaskController {

    @Resource
    private MesWorkflowTaskService workflowTaskService;

    @GetMapping("todo-page")
    @Operation(summary = "获取 MES 工作台待办任务分页")
    public CommonResult<PageResult<BpmTaskRespVO>> getTodoPage(@Valid BpmTaskPageReqVO pageVO) {
        return success(workflowTaskService.getTodoPage(pageVO));
    }

    @GetMapping("done-page")
    @Operation(summary = "获取 MES 工作台已办任务分页")
    public CommonResult<PageResult<BpmTaskRespVO>> getDonePage(@Valid BpmTaskPageReqVO pageVO) {
        return success(workflowTaskService.getDonePage(pageVO));
    }

}
