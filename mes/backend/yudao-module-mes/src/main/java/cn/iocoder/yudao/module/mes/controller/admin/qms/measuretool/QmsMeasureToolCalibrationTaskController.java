package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskBatchConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationDueHintRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCandidateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo.QmsMeasureToolCalibrationTaskRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool.QmsMeasureToolCalibrationTaskDO;
import cn.iocoder.yudao.module.mes.service.qms.measuretool.QmsMeasureToolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 量检具校准预警任务")
@RestController
@RequestMapping("/mes/quality/measure-tool/calibration-task")
@Validated
public class QmsMeasureToolCalibrationTaskController {

    @Resource
    private QmsMeasureToolService measureToolService;

    @PostMapping("/generate")
    @Operation(summary = "生成量检具校准预警任务")
    public CommonResult<Integer> generateCalibrationTasks(@Valid @RequestBody QmsMeasureToolCalibrationTaskGenerateReqVO reqVO) {
        return success(measureToolService.generateCalibrationTasks(reqVO));
    }

    @GetMapping("/candidates")
    @Operation(summary = "获得月度待计量候选台账")
    public CommonResult<List<QmsMeasureToolCalibrationTaskCandidateRespVO>> getCalibrationTaskCandidates(
            @Valid QmsMeasureToolCalibrationTaskCandidateReqVO reqVO) {
        return success(measureToolService.getCalibrationTaskCandidates(reqVO));
    }

    @GetMapping("/due-hint")
    @Operation(summary = "获得月度待计量提醒数量")
    public CommonResult<QmsMeasureToolCalibrationDueHintRespVO> getCalibrationDueHint(
            @Valid QmsMeasureToolCalibrationTaskCandidateReqVO reqVO) {
        return success(measureToolService.getCalibrationDueHint(reqVO));
    }

    @PutMapping("/cancel")
    @Operation(summary = "取消量检具校准预警任务")
    public CommonResult<Boolean> cancelCalibrationTask(@Valid @RequestBody QmsMeasureToolCalibrationTaskCancelReqVO reqVO) {
        measureToolService.cancelCalibrationTask(reqVO);
        return success(true);
    }

    @PutMapping("/batch-confirm")
    @Operation(summary = "批量确认量检具校准任务")
    public CommonResult<Integer> batchConfirmCalibrationTasks(
            @Valid @RequestBody QmsMeasureToolCalibrationTaskBatchConfirmReqVO reqVO) {
        return success(measureToolService.batchConfirmCalibrationTasks(reqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得量检具校准预警任务")
    @Parameter(name = "id", description = "编号", required = true)
    public CommonResult<QmsMeasureToolCalibrationTaskRespVO> getCalibrationTask(@RequestParam("id") Long id) {
        QmsMeasureToolCalibrationTaskDO task = measureToolService.getCalibrationTask(id);
        return success(BeanUtils.toBean(task, QmsMeasureToolCalibrationTaskRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得量检具校准预警任务分页")
    public CommonResult<PageResult<QmsMeasureToolCalibrationTaskRespVO>> getCalibrationTaskPage(
            @Valid QmsMeasureToolCalibrationTaskPageReqVO reqVO) {
        PageResult<QmsMeasureToolCalibrationTaskDO> pageResult = measureToolService.getCalibrationTaskPage(reqVO);
        return success(BeanUtils.toBean(pageResult, QmsMeasureToolCalibrationTaskRespVO.class));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出量检具校准预警任务 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportCalibrationTaskExcel(@Valid QmsMeasureToolCalibrationTaskPageReqVO reqVO,
                                           HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<QmsMeasureToolCalibrationTaskDO> list = measureToolService.getCalibrationTaskPage(reqVO).getList();
        ExcelUtils.write(response, "量检具校准预警任务.xls", "数据", QmsMeasureToolCalibrationTaskRespVO.class,
                BeanUtils.toBean(list, QmsMeasureToolCalibrationTaskRespVO.class));
    }

}
