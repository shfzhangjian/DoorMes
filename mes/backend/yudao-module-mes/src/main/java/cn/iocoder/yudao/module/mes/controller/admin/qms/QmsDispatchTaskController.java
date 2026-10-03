package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCandidateItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskDispatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskProcessSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskRecheckReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskResultSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourcePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskStandardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsDispatchTaskWizardCreateReqVO;
import cn.iocoder.yudao.module.mes.service.qms.QmsDispatchTaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 质量任务中心")
@RestController
@RequestMapping("/mes/quality/task-center")
@Validated
public class QmsDispatchTaskController {

    @Resource
    private QmsDispatchTaskService taskService;

    @PostMapping("/create-from-execution")
    @Operation(summary = "关联原检验记录创建质量任务")
    public CommonResult<Long> createFromExecution(@Valid @RequestBody QmsDispatchTaskCreateReqVO reqVO) {
        return success(taskService.createFromExecution(reqVO));
    }

    @PostMapping("/wizard-create")
    @Operation(summary = "向导生成检验单、质量任务并发起流程")
    public CommonResult<Long> createAndDispatch(@Valid @RequestBody QmsDispatchTaskWizardCreateReqVO reqVO) {
        return success(taskService.createAndDispatch(reqVO));
    }

    @PutMapping("/dispatch")
    @Operation(summary = "分派质量任务")
    public CommonResult<Boolean> dispatch(@Valid @RequestBody QmsDispatchTaskDispatchReqVO reqVO) {
        taskService.dispatch(reqVO);
        return success(true);
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消任务中心任务，不取消原检验单")
    public CommonResult<Boolean> cancel(@Valid @RequestBody QmsDispatchTaskCancelReqVO reqVO) {
        taskService.cancel(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得质量任务详情")
    @Parameter(name = "id", description = "任务ID", required = true)
    public CommonResult<QmsDispatchTaskRespVO> get(@RequestParam("id") Long id) {
        return success(taskService.get(id));
    }

    @GetMapping("/detail")
    @Operation(summary = "获得质量任务 ERP 详情")
    public CommonResult<QmsDispatchTaskDetailRespVO> getDetail(@RequestParam("id") Long id) {
        return success(taskService.getDetail(id));
    }

    @PutMapping("/result/save")
    @Operation(summary = "保存质量任务检验结果")
    public CommonResult<Boolean> saveResult(@Valid @RequestBody QmsDispatchTaskResultSaveReqVO reqVO) {
        taskService.saveResult(reqVO);
        return success(true);
    }

    @PutMapping("/process/submit")
    @Operation(summary = "提交质量任务当前流程节点")
    public CommonResult<Boolean> submitProcessNode(
            @Valid @RequestBody QmsDispatchTaskProcessSubmitReqVO reqVO) {
        taskService.submitProcessNode(reqVO);
        return success(true);
    }

    @PutMapping("/process/recheck")
    @Operation(summary = "结果确认退回重检并生成下一轮检验单")
    public CommonResult<Boolean> returnForRecheck(
            @Valid @RequestBody QmsDispatchTaskRecheckReqVO reqVO) {
        taskService.returnForRecheck(reqVO);
        return success(true);
    }

    @GetMapping("/source-page")
    @Operation(summary = "获得复检来源检验记录分页")
    public CommonResult<PageResult<QmsDispatchTaskSourceRespVO>> getSourcePage(
            @Valid QmsDispatchTaskSourcePageReqVO reqVO) {
        return success(taskService.getSourcePage(reqVO));
    }

    @GetMapping("/source-items")
    @Operation(summary = "获得复检来源检验项目")
    public CommonResult<List<QmsDispatchTaskCandidateItemRespVO>> getSourceItems(
            @RequestParam("checkType") String checkType,
            @RequestParam("executionId") Long executionId) {
        return success(taskService.getSourceItems(checkType, executionId));
    }

    @GetMapping("/source-item-tree")
    @Operation(summary = "获得复检来源检验项目层级")
    public CommonResult<List<QmsDispatchTaskCandidateItemRespVO>> getSourceItemTree(
            @RequestParam("checkType") String checkType,
            @RequestParam("executionId") Long executionId) {
        return success(taskService.getSourceItemTree(checkType, executionId));
    }

    @GetMapping("/standard-page")
    @Operation(summary = "获得加检适用标准分页")
    public CommonResult<PageResult<QmsDispatchTaskStandardRespVO>> getStandardPage(
            @Valid QmsDispatchTaskStandardPageReqVO reqVO) {
        return success(taskService.getStandardPage(reqVO));
    }

    @GetMapping("/standard-items")
    @Operation(summary = "获得标准检验项目")
    public CommonResult<List<QmsDispatchTaskCandidateItemRespVO>> getStandardItems(
            @RequestParam("standardId") Long standardId) {
        return success(taskService.getStandardItems(standardId));
    }

    @GetMapping("/page")
    @Operation(summary = "获得质量任务分页")
    public CommonResult<PageResult<QmsDispatchTaskRespVO>> getPage(@Valid QmsDispatchTaskPageReqVO reqVO) {
        return success(taskService.getPage(reqVO));
    }

    @GetMapping("/logs")
    @Operation(summary = "获得质量任务操作历史")
    public CommonResult<List<QmsDispatchTaskLogRespVO>> getLogs(@RequestParam("taskId") Long taskId) {
        return success(taskService.getLogs(taskId));
    }
}
