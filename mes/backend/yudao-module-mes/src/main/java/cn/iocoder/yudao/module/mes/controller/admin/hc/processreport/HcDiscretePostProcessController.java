package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcDiscretePostProcessVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcDiscretePostProcessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "Admin - HC 离散后加工工作台")
@RestController
@RequestMapping("/mes/hc/execution/discrete-post-process")
@Validated
public class HcDiscretePostProcessController {

    @Resource
    private HcDiscretePostProcessService hcDiscretePostProcessService;

    @GetMapping("/candidate/list")
    @Operation(summary = "查询可用于离散后加工计划的中间品片号")
    public CommonResult<List<HcDiscretePostProcessVO.StockRespVO>> getCandidateList(
            @Valid HcDiscretePostProcessVO.SourceQueryReqVO reqVO) {
        return success(hcDiscretePostProcessService.getCandidateList(reqVO));
    }

    @PostMapping("/plan/create")
    @Operation(summary = "创建离散后加工计划并锁定选中片号")
    public CommonResult<Long> createPlan(@Valid @RequestBody HcDiscretePostProcessVO.CreatePlanReqVO reqVO) {
        return success(hcDiscretePostProcessService.createPlan(reqVO));
    }

    @GetMapping("/task-list")
    @Operation(summary = "查询离散后加工本工序计划列表")
    public CommonResult<List<HcDiscretePostProcessVO.TaskRespVO>> getTaskList(
            @Valid HcDiscretePostProcessVO.TaskQueryReqVO reqVO) {
        return success(hcDiscretePostProcessService.getTaskList(reqVO));
    }

    @GetMapping("/source/list")
    @Operation(summary = "查询离散后加工计划工序下的片号列表")
    @Parameter(name = "planOperationId", required = true)
    public CommonResult<List<HcDiscretePostProcessVO.SourceRespVO>> getSourceList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "taskStatus", required = false) String taskStatus,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(hcDiscretePostProcessService.getSourceList(planOperationId, taskStatus, keyword));
    }

    @GetMapping("/source/scan")
    @Operation(summary = "按计划工序和片号扫码定位离散后加工来源")
    public CommonResult<HcDiscretePostProcessVO.SourceRespVO> scanSource(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam("pieceNo") String pieceNo) {
        return success(hcDiscretePostProcessService.scanSource(planOperationId, pieceNo));
    }

    @GetMapping("/source/scan-by-op")
    @Operation(summary = "按目标工序和片号扫码定位离散后加工来源")
    public CommonResult<HcDiscretePostProcessVO.SourceRespVO> scanSourceByOp(
            @RequestParam("targetOpCode") String targetOpCode,
            @RequestParam("pieceNo") String pieceNo) {
        return success(hcDiscretePostProcessService.scanSourceByTargetOp(targetOpCode, pieceNo));
    }

    @PostMapping("/report/confirm")
    @Operation(summary = "离散后加工扫码确认报工")
    public CommonResult<HcDiscretePostProcessVO.SourceRespVO> report(
            @Valid @RequestBody HcDiscretePostProcessVO.ReportReqVO reqVO) {
        return success(hcDiscretePostProcessService.report(reqVO));
    }

    @GetMapping("/inspection-task/list")
    @Operation(summary = "查询离散后加工送检记录")
    @Parameter(name = "planOperationId", required = true)
    public CommonResult<List<HcDiscretePostProcessVO.SourceRespVO>> getInspectionTaskList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "inspectionType", required = false) String inspectionType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return success(hcDiscretePostProcessService.getInspectionTaskList(planOperationId, inspectionType, keyword));
    }

    @PostMapping("/inspection-task/create")
    @Operation(summary = "离散后加工送检")
    public CommonResult<HcDiscretePostProcessVO.InspectionRespVO> createInspectionTask(
            @Valid @RequestBody HcDiscretePostProcessVO.InspectionReqVO reqVO) {
        return success(hcDiscretePostProcessService.createInspectionTask(reqVO));
    }
}
