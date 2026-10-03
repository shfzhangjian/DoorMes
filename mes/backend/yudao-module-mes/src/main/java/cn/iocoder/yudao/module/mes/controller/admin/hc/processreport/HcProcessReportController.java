package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2CoaWithdrawReqVO;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2TailAssignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2TailBatchAssignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2TailSelectedAssignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveFaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveAqcTaskFeedbackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveAqcTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveAqcTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveCheckItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardLossReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsageSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSaveConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSaveTextReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTimeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveSegmentCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveSegmentTimingStampReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableCleanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableMaterialRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPlanChangeoverLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPlanChangeoverLogSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotAbnormalLockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotFormRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotFormRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotIntermediateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotIntermediateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotMiddleLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotSegmentCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotProcessParamRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotProcessParamSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotScanGateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotFaiWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveIntermediateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveIntermediateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2IntermediateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2IntermediateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTimeLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTimeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcOperationReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcStatisticsDataReviseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2AbnormalCategoryCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2FaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2FaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2RuntimeProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcVisualAbnormalCategoryCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetFaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportAbnormalPositionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportQuantityReviseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetWaterChangeApplyRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetWaterChangeApplySaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSlicePrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSourceCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSourceRespVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcProcessReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 配料报工")
@RestController
@RequestMapping("/mes/hc/execution/formula-report")
@Validated
public class HcProcessReportController {

    @Resource
    private HcProcessReportService hcProcessReportService;

    @GetMapping("/task-list")
    @Operation(summary = "获得配料报工任务列表")
    public CommonResult<List<HcFormulaReportTaskRespVO>> getFormulaTaskList(@Valid HcFormulaReportTaskPageReqVO reqVO) {
        return success(hcProcessReportService.getFormulaTaskList(reqVO));
    }

    @PostMapping("/start")
    @Operation(summary = "执行配料开工确认")
    public CommonResult<Long> startFormulaReport(@Valid @RequestBody HcFormulaReportStartReqVO reqVO) {
        return success(hcProcessReportService.startFormulaReport(reqVO));
    }

    @PostMapping("/switch-equipment")
    @Operation(summary = "切换配料工位设备")
    public CommonResult<Boolean> switchFormulaEquipment(@Valid @RequestBody HcFormulaReportSwitchEquipmentReqVO reqVO) {
        hcProcessReportService.switchFormulaEquipment(reqVO);
        return success(true);
    }

    @PostMapping("/report/time/update")
    @Operation(summary = "修改配料报工开始结束时间")
    public CommonResult<Long> updateFormulaReportTime(@Valid @RequestBody HcFormulaReportTimeUpdateReqVO reqVO) {
        return success(hcProcessReportService.updateFormulaReportTime(reqVO));
    }

    @PostMapping("/report/statistics-data/revise")
    @Operation(summary = "修订配料母卷批次良品统计数据")
    public CommonResult<Long> reviseFormulaReportStatisticsData(
            @Valid @RequestBody HcStatisticsDataReviseReqVO reqVO) {
        return success(hcProcessReportService.reviseFormulaReportStatisticsData(reqVO));
    }

    @GetMapping("/report/time-log/list")
    @Operation(summary = "获得配料报工时间修改日志")
    public CommonResult<List<HcFormulaReportTimeLogRespVO>> getFormulaReportTimeLogs(
            @RequestParam("operationReportId") Long operationReportId) {
        return success(hcProcessReportService.getFormulaReportTimeLogs(operationReportId));
    }

    @GetMapping("/wet/task-list")
    @Operation(summary = "获得湿法报工任务列表")
    public CommonResult<List<HcWetReportTaskRespVO>> getWetTaskList(@Valid HcWetReportTaskPageReqVO reqVO) {
        return success(hcProcessReportService.getWetTaskList(reqVO));
    }

    @GetMapping("/wet/fai/summary")
    @Operation(summary = "获得湿法报工首检摘要")
    public CommonResult<HcWetFaiRespVO> getWetFaiSummary(@RequestParam("planId") Long planId,
                                                         @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getWetFaiSummary(planId, planOperationId));
    }

    @PostMapping("/wet/fai/apply")
    @Operation(summary = "提交湿法报工首检申请")
    public CommonResult<HcWetFaiRespVO> applyWetFai(@Valid @RequestBody HcWetFaiApplyReqVO reqVO) {
        return success(hcProcessReportService.applyWetFai(reqVO));
    }

    @GetMapping("/wet/water-change/active")
    @Operation(summary = "获得湿法未过期换水申请")
    public CommonResult<HcWetWaterChangeApplyRespVO> getActiveWetWaterChangeApply() {
        return success(hcProcessReportService.getActiveWetWaterChangeApply());
    }

    @PostMapping("/wet/water-change/apply")
    @Operation(summary = "提交湿法换水申请")
    public CommonResult<HcWetWaterChangeApplyRespVO> saveWetWaterChangeApply(
            @Valid @RequestBody HcWetWaterChangeApplySaveReqVO reqVO) {
        return success(hcProcessReportService.saveWetWaterChangeApply(reqVO));
    }

    @GetMapping("/wet/pass-work/list")
    @Operation(summary = "获得湿法过站工作列表")
    public CommonResult<List<HcWetPassWorkRespVO>> getWetPassWorkList(@RequestParam("planId") Long planId,
                                                                      @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getWetPassWorkList(planId, planOperationId));
    }

    @PostMapping("/wet/start")
    @Operation(summary = "执行湿法开工确认")
    public CommonResult<Long> startWetReport(@Valid @RequestBody HcWetReportStartReqVO reqVO) {
        return success(hcProcessReportService.startWetReport(reqVO));
    }

    @PostMapping("/wet/switch-equipment")
    @Operation(summary = "切换湿法工位设备")
    public CommonResult<Boolean> switchWetEquipment(@Valid @RequestBody HcWetReportSwitchEquipmentReqVO reqVO) {
        hcProcessReportService.switchWetEquipment(reqVO);
        return success(true);
    }

    @PostMapping("/wet/report/time/update")
    @Operation(summary = "修改湿法报工开始结束时间")
    public CommonResult<Long> updateWetReportTime(@Valid @RequestBody HcFormulaReportTimeUpdateReqVO reqVO) {
        return success(hcProcessReportService.updateWetReportTime(reqVO));
    }

    @PostMapping("/wet/report/quantity/revise")
    @Operation(summary = "修正湿法报工收卷米数")
    public CommonResult<Long> reviseWetReportQuantity(@Valid @RequestBody HcWetReportQuantityReviseReqVO reqVO) {
        return success(hcProcessReportService.reviseWetReportQuantity(reqVO));
    }

    @PostMapping("/wet/report/statistics-data/revise")
    @Operation(summary = "修订湿法母卷批次良品统计数据")
    public CommonResult<Long> reviseWetReportStatisticsData(
            @Valid @RequestBody HcStatisticsDataReviseReqVO reqVO) {
        return success(hcProcessReportService.reviseWetReportStatisticsData(reqVO));
    }

    @GetMapping("/wet/report/time-log/list")
    @Operation(summary = "获得湿法报工时间修改日志")
    public CommonResult<List<HcFormulaReportTimeLogRespVO>> getWetReportTimeLogs(
            @RequestParam("operationReportId") Long operationReportId) {
        return success(hcProcessReportService.getWetReportTimeLogs(operationReportId));
    }

    @PostMapping("/wet/report/confirm")
    @Operation(summary = "确认湿法报工记录")
    public CommonResult<Long> confirmWetReport(@Valid @RequestBody HcOperationReportConfirmReqVO reqVO) {
        return success(hcProcessReportService.confirmWetReport(reqVO));
    }

    @PostMapping("/wet/pass-work/save")
    @Operation(summary = "保存湿法过站工作")
    public CommonResult<Long> saveWetPassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.saveWetPassWork(reqVO));
    }

    @PostMapping("/wet/pass-work/confirm")
    @Operation(summary = "确认湿法过站工作")
    public CommonResult<Long> confirmWetPassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmWetPassWork(reqVO));
    }

    @PostMapping("/wet/submit")
    @Operation(summary = "提交湿法报工")
    public CommonResult<Long> submitWetReport(@Valid @RequestBody HcWetReportSaveReqVO reqVO) {
        return success(hcProcessReportService.submitWetReport(reqVO));
    }

    @GetMapping("/wet/abnormal-position/list")
    @Operation(summary = "获得湿法报工异常位置明细")
    public CommonResult<List<HcWetReportAbnormalPositionRespVO>> getWetReportAbnormalPositions(
            @RequestParam(value = "operationReportId", required = false) Long operationReportId,
            @RequestParam(value = "planOperationId", required = false) Long planOperationId) {
        return success(hcProcessReportService.getWetReportAbnormalPositions(operationReportId, planOperationId));
    }

    @GetMapping("/rough-grinding/task-list")
    @Operation(summary = "获得磨皮报工任务列表")
    public CommonResult<List<HcRoughReportTaskRespVO>> getRoughTaskList(@Valid HcRoughReportTaskPageReqVO reqVO) {
        return success(hcProcessReportService.getRoughTaskList(reqVO));
    }

    @PostMapping("/rough-grinding/start")
    @Operation(summary = "执行磨皮开工确认")
    public CommonResult<Long> startRoughReport(@Valid @RequestBody HcRoughReportStartReqVO reqVO) {
        return success(hcProcessReportService.startRoughReport(reqVO));
    }

    @PostMapping("/rough-grinding/switch-equipment")
    @Operation(summary = "切换磨皮工位设备")
    public CommonResult<Boolean> switchRoughEquipment(@Valid @RequestBody HcRoughReportSwitchEquipmentReqVO reqVO) {
        hcProcessReportService.switchRoughEquipment(reqVO);
        return success(true);
    }

    @PostMapping("/rough-grinding/save-progress")
    @Operation(summary = "保存磨皮过程记录")
    public CommonResult<Long> saveRoughProgress(@Valid @RequestBody HcRoughReportSaveReqVO reqVO) {
        return success(hcProcessReportService.saveRoughProgress(reqVO));
    }

    @PostMapping("/rough-grinding/submit")
    @Operation(summary = "提交磨皮报工")
    public CommonResult<Long> submitRoughReport(@Valid @RequestBody HcRoughReportSaveReqVO reqVO) {
        return success(hcProcessReportService.submitRoughReport(reqVO));
    }

    @GetMapping("/adhesive/task-list")
    @Operation(summary = "获得粘胶1报工任务列表")
    public CommonResult<List<HcAdhesiveReportTaskRespVO>> getAdhesiveTaskList(@Valid HcAdhesiveReportTaskPageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesiveTaskList(reqVO));
    }

    @GetMapping("/adhesive/fai/summary")
    @Operation(summary = "获得粘胶1 FAI 首检摘要")
    public CommonResult<HcAdhesiveFaiRespVO> getAdhesiveFaiSummary(@RequestParam("planId") Long planId,
                                                                   @RequestParam("planOperationId") Long planOperationId,
                                                                   @RequestParam(value = "sourceGrindingSecondDetailId", required = false) Long sourceGrindingSecondDetailId) {
        return success(hcProcessReportService.getAdhesiveFaiSummary(planId, planOperationId, sourceGrindingSecondDetailId));
    }

    @PostMapping("/adhesive/fai/apply")
    @Operation(summary = "提交粘胶1 FAI 首检申请")
    public CommonResult<HcAdhesiveFaiRespVO> applyAdhesiveFai(@Valid @RequestBody HcAdhesiveFaiApplyReqVO reqVO) {
        return success(hcProcessReportService.applyAdhesiveFai(reqVO));
    }

    @PostMapping("/adhesive/start")
    @Operation(summary = "执行粘胶1开工确认")
    public CommonResult<Long> startAdhesiveReport(@Valid @RequestBody HcAdhesiveReportStartReqVO reqVO) {
        return success(hcProcessReportService.startAdhesiveReport(reqVO));
    }

    @PostMapping("/adhesive/switch-equipment")
    @Operation(summary = "切换粘胶1工位设备")
    public CommonResult<Boolean> switchAdhesiveEquipment(@Valid @RequestBody HcAdhesiveReportSwitchEquipmentReqVO reqVO) {
        hcProcessReportService.switchAdhesiveEquipment(reqVO);
        return success(true);
    }

    @GetMapping("/adhesive/pass-work/list")
    @Operation(summary = "获得粘胶1开机前准备列表")
    public CommonResult<List<HcWetPassWorkRespVO>> getAdhesivePassWorkList(@RequestParam("planId") Long planId,
                                                                           @RequestParam("planOperationId") Long planOperationId,
                                                                           @RequestParam(value = "equipmentId", required = false) Long equipmentId,
                                                                           @RequestParam(value = "recordDate", required = false)
                                                                           @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcProcessReportService.getAdhesivePassWorkList(planId, planOperationId, equipmentId, recordDate));
    }

    @PostMapping("/adhesive/pass-work/save")
    @Operation(summary = "保存粘胶1开机前准备")
    public CommonResult<Long> saveAdhesivePassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesivePassWork(reqVO));
    }

    @PostMapping("/adhesive/pass-work/confirm")
    @Operation(summary = "确认粘胶1开机前准备")
    public CommonResult<Long> confirmAdhesivePassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmAdhesivePassWork(reqVO));
    }

    @GetMapping("/adhesive/check-template")
    @Operation(summary = "获得粘胶1报工点检模板")
    public CommonResult<List<HcAdhesiveCheckItemRespVO>> getAdhesiveCheckTemplate() {
        return success(hcProcessReportService.getAdhesiveCheckTemplate());
    }

    @GetMapping("/adhesive/source/scan")
    @Operation(summary = "扫码查询粘胶1来源第二次磨皮批次")
    public CommonResult<HcAdhesiveSourceRespVO> scanAdhesiveSource(@RequestParam("batchNo") String batchNo,
                                                                   @RequestParam(value = "planOperationId", required = false) Long planOperationId,
                                                                   @RequestParam(value = "sourceMode", required = false) String sourceMode,
                                                                   @RequestParam(value = "sourcePlanNo", required = false) String sourcePlanNo,
                                                                   @RequestParam(value = "sourceMotherBatchNo", required = false) String sourceMotherBatchNo) {
        return success(hcProcessReportService.scanAdhesiveSource(batchNo, planOperationId,
                sourceMode, sourcePlanNo, sourceMotherBatchNo));
    }

    @GetMapping("/adhesive/source/list")
    @Operation(summary = "获得粘胶1可加工来源二次磨皮批次")
    public CommonResult<List<HcAdhesiveSourceRespVO>> getAdhesiveSourceList(@RequestParam("planId") Long planId,
                                                                            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getAdhesiveSourceList(planId, planOperationId));
    }

    @PostMapping("/adhesive/segment-timing/stamp")
    @Operation(summary = "记录粘胶1可视化加工分段开工或完工时间")
    public CommonResult<Long> stampAdhesiveSegmentTiming(
            @Valid @RequestBody HcAdhesiveSegmentTimingStampReqVO reqVO) {
        return success(hcProcessReportService.stampAdhesiveSegmentTiming(reqVO));
    }

    @GetMapping("/adhesive/report/list")
    @Operation(summary = "获得粘胶1报工记录")
    public CommonResult<List<HcAdhesiveReportRespVO>> getAdhesiveReportList(@RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getAdhesiveReportList(planOperationId));
    }

    @GetMapping("/adhesive/glue-board-stock/page")
    @Operation(summary = "分页获得粘胶1胶板边库库存")
    public CommonResult<PageResult<HcAdhesiveGlueBoardStockRespVO>> getAdhesiveGlueBoardStockPage(
            @Valid HcAdhesiveGlueBoardStockPageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesiveGlueBoardStockPage(reqVO));
    }

    @GetMapping("/adhesive/glue-board-stock/export-excel")
    @Operation(summary = "导出粘胶胶板边库当前库存 Excel")
    @ApiAccessLog(operateType = EXPORT)
    public void exportAdhesiveGlueBoardStockExcel(@Valid HcAdhesiveGlueBoardStockPageReqVO reqVO,
                                                  HttpServletResponse response) throws IOException {
        reqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        ExcelUtils.write(response, "胶板边库当前库存.xlsx", "当前库存",
                HcAdhesiveGlueBoardStockImportExcelVO.class,
                hcProcessReportService.buildAdhesiveGlueBoardStockExportList(reqVO));
    }

    @PostMapping("/adhesive/glue-board-stock/import")
    @Operation(summary = "导入初始化粘胶胶板边库当前库存")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<HcAdhesiveGlueBoardStockImportRespVO> importAdhesiveGlueBoardStock(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "confirmClear", required = false) Boolean confirmClear) throws IOException {
        return success(hcProcessReportService.importAdhesiveGlueBoardStockExcel(file, confirmClear));
    }

    @GetMapping("/adhesive/glue-board-stock/get-by-batch")
    @Operation(summary = "按批号获得粘胶1胶板边库库存")
    public CommonResult<HcAdhesiveGlueBoardStockRespVO> getAdhesiveGlueBoardStockByBatch(
            @RequestParam("glueBoardBatchNo") String glueBoardBatchNo,
            @RequestParam(value = "glueBoardModel", required = false) String glueBoardModel) {
        return success(hcProcessReportService.getAdhesiveGlueBoardStockByBatch(glueBoardBatchNo, glueBoardModel));
    }

    @PostMapping("/adhesive/glue-board-stock/create")
    @Operation(summary = "新建粘胶1胶板边库领料")
    public CommonResult<Long> createAdhesiveGlueBoardStock(@Valid @RequestBody HcAdhesiveGlueBoardStockSaveReqVO reqVO) {
        return success(hcProcessReportService.createAdhesiveGlueBoardStock(reqVO));
    }

    @PostMapping("/adhesive/glue-board-stock/mark-printed")
    @Operation(summary = "标记粘胶1胶板边库二维码已打印")
    public CommonResult<HcAdhesiveGlueBoardStockRespVO> markAdhesiveGlueBoardStockPrinted(@RequestParam("id") Long id) {
        return success(hcProcessReportService.markAdhesiveGlueBoardStockPrinted(id));
    }

    @GetMapping("/adhesive/glue-board/list")
    @Operation(summary = "获得粘胶1胶板领用记录")
    public CommonResult<List<HcAdhesiveGlueBoardUsageRespVO>> getAdhesiveGlueBoardUsageList(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getAdhesiveGlueBoardUsageList(planOperationId));
    }

    @GetMapping("/adhesive/glue-board/page")
    @Operation(summary = "分页获得粘胶1胶板边料台账")
    public CommonResult<PageResult<HcAdhesiveGlueBoardUsageRespVO>> getAdhesiveGlueBoardUsagePage(
            @Valid HcAdhesiveGlueBoardUsagePageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesiveGlueBoardUsagePage(reqVO));
    }

    @GetMapping("/adhesive/glue-board/current")
    @Operation(summary = "获得粘胶1当前胶板领用记录")
    public CommonResult<HcAdhesiveGlueBoardUsageRespVO> getAdhesiveCurrentGlueBoardUsage(
            @RequestParam(value = "planOperationId", required = false) Long planOperationId,
            @RequestParam(value = "glueBoardModel", required = false) String glueBoardModel) {
        return success(hcProcessReportService.getAdhesiveCurrentGlueBoardUsage(planOperationId, glueBoardModel, "ADHESIVE"));
    }

    @GetMapping("/adhesive/glue-board-fai/latest")
    @Operation(summary = "获得粘胶1当前分段胶板最后送检记录")
    public CommonResult<HcAdhesiveFaiRespVO> getAdhesiveLatestGlueBoardFai(
            @RequestParam(value = "planOperationId", required = false) Long planOperationId,
            @RequestParam(value = "sourceProductionBatchNo", required = false) String sourceProductionBatchNo,
            @RequestParam(value = "glueBoardStockId", required = false) Long glueBoardStockId,
            @RequestParam(value = "glueBoardBatchNo", required = false) String glueBoardBatchNo,
            @RequestParam(value = "glueBoardModel", required = false) String glueBoardModel) {
        return success(hcProcessReportService.getLatestAdhesiveGlueBoardFai(
                planOperationId, sourceProductionBatchNo, glueBoardStockId, glueBoardBatchNo, glueBoardModel, "ADHESIVE"));
    }

    @PostMapping("/adhesive/glue-board/save")
    @Operation(summary = "保存粘胶1胶板领用记录")
    public CommonResult<Long> saveAdhesiveGlueBoardUsage(@Valid @RequestBody HcAdhesiveGlueBoardUsageSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesiveGlueBoardUsage(reqVO));
    }

    @PostMapping("/adhesive/glue-board/loss")
    @Operation(summary = "报备粘胶1胶板损耗")
    public CommonResult<HcAdhesiveGlueBoardUsageRespVO> reportAdhesiveGlueBoardLoss(
            @Valid @RequestBody HcAdhesiveGlueBoardLossReqVO reqVO) {
        return success(hcProcessReportService.reportAdhesiveGlueBoardLoss(reqVO));
    }

    @PostMapping("/adhesive/glue-board/return")
    @Operation(summary = "退回释放粘胶1胶板领用")
    public CommonResult<HcAdhesiveGlueBoardUsageRespVO> returnAdhesiveGlueBoardUsage(@RequestParam("id") Long id) {
        return success(hcProcessReportService.returnAdhesiveGlueBoardUsage(id));
    }

    @PostMapping("/adhesive/aqc/submit")
    @Operation(summary = "提交粘胶1粘胶检验任务")
    public CommonResult<HcAdhesiveAqcTaskRespVO> submitAdhesiveAqcTask(@Valid @RequestBody HcAdhesiveAqcTaskSaveReqVO reqVO) {
        return success(hcProcessReportService.submitAdhesiveAqcTask(reqVO));
    }

    @GetMapping("/adhesive/aqc/report/latest")
    @Operation(summary = "按母卷批次获得粘胶1母卷检验任务")
    public CommonResult<HcAdhesiveAqcTaskRespVO> getLatestAdhesiveReportAqcTask(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam("batchNo") String batchNo) {
        return success(hcProcessReportService.getLatestAdhesiveReportAqcTask(planOperationId, batchNo));
    }

    @PostMapping("/adhesive/aqc/feedback")
    @Operation(summary = "反馈粘胶1粘胶检验结果")
    public CommonResult<HcAdhesiveAqcTaskRespVO> feedbackAdhesiveAqcTask(@Valid @RequestBody HcAdhesiveAqcTaskFeedbackReqVO reqVO) {
        return success(hcProcessReportService.feedbackAdhesiveAqcTask(reqVO));
    }

    @PostMapping("/adhesive/report/save")
    @Operation(summary = "保存粘胶1报工记录")
    public CommonResult<Long> saveAdhesiveReport(@Valid @RequestBody HcAdhesiveReportSaveTextReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesiveReport(reqVO));
    }

    @PostMapping("/adhesive/report/time/update")
    @Operation(summary = "修改粘胶1报工开始结束时间")
    public CommonResult<Long> updateAdhesiveReportTime(@Valid @RequestBody HcAdhesiveReportTimeUpdateReqVO reqVO) {
        return success(hcProcessReportService.updateAdhesiveReportTime(reqVO));
    }

    @PostMapping("/adhesive/report/statistics-data/revise")
    @Operation(summary = "修订粘胶1母卷批次良品统计数据")
    public CommonResult<Long> reviseAdhesiveReportStatisticsData(
            @Valid @RequestBody HcStatisticsDataReviseReqVO reqVO) {
        return success(hcProcessReportService.reviseAdhesiveReportStatisticsData(reqVO));
    }

    @PostMapping("/adhesive/report/confirm")
    @Operation(summary = "扫码确认粘胶1报工记录")
    public CommonResult<Long> confirmAdhesiveReport(@Valid @RequestBody HcAdhesiveReportConfirmReqVO reqVO) {
        return success(hcProcessReportService.confirmAdhesiveReport(reqVO));
    }

    @PostMapping("/adhesive/report/mark-printed")
    @Operation(summary = "标记粘胶1报工记录已打印")
    public CommonResult<Boolean> markAdhesiveReportPrinted(@Valid @RequestBody HcAdhesiveReportPrintReqVO reqVO) {
        hcProcessReportService.markAdhesiveReportPrinted(reqVO);
        return success(true);
    }

    @DeleteMapping("/adhesive/report/delete")
    @Operation(summary = "删除粘胶1报工记录")
    public CommonResult<Boolean> removeAdhesiveReport(@RequestParam("id") Long id) {
        hcProcessReportService.removeAdhesiveReport(id);
        return success(true);
    }

    @PostMapping("/adhesive/submit")
    @Operation(summary = "提交粘胶1过站报工")
    public CommonResult<Long> submitAdhesiveReport(@Valid @RequestBody HcAdhesiveReportSubmitReqVO reqVO) {
        return success(hcProcessReportService.submitAdhesiveReport(reqVO));
    }

    @PostMapping("/adhesive/segment/complete")
    @Operation(summary = "粘胶1当前分段完成")
    public CommonResult<Long> completeAdhesiveSegment(@Valid @RequestBody HcAdhesiveSegmentCompleteReqVO reqVO) {
        return success(hcProcessReportService.completeAdhesiveSegment(reqVO));
    }

    @GetMapping("/adhesive/intermediate/get")
    @Operation(summary = "获得粘胶1中间品记录")
    public CommonResult<HcAdhesiveIntermediateRespVO> getAdhesiveIntermediate(@RequestParam("planId") Long planId,
                                                                              @RequestParam("planOperationId") Long planOperationId,
                                                                              @RequestParam(value = "adhesiveReportId", required = false) Long adhesiveReportId,
                                                                              @RequestParam(value = "batchNo", required = false) String batchNo) {
        return success(hcProcessReportService.getAdhesiveIntermediate(planId, planOperationId, adhesiveReportId, batchNo));
    }

    @GetMapping("/adhesive/intermediate/record")
    @Operation(summary = "按记录 ID 获得粘胶1中间品记录")
    public CommonResult<HcAdhesiveIntermediateRespVO> getAdhesiveIntermediateById(@RequestParam("recordId") Long recordId) {
        return success(hcProcessReportService.getAdhesiveIntermediateById(recordId));
    }

    @GetMapping("/adhesive/intermediate/page")
    @Operation(summary = "分页获得粘胶1中间品记录业务记录")
    public CommonResult<PageResult<HcStationRecordRespVO>> getAdhesiveIntermediateRecordPage(@Valid HcStationRecordPageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesiveIntermediateRecordPage(reqVO));
    }

    @PostMapping("/adhesive/intermediate/save")
    @Operation(summary = "保存粘胶1中间品记录")
    public CommonResult<Long> saveAdhesiveIntermediate(@Valid @RequestBody HcAdhesiveIntermediateSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesiveIntermediate(reqVO));
    }

    @PostMapping("/adhesive/intermediate/confirm")
    @Operation(summary = "确认粘胶1中间品记录")
    public CommonResult<Long> confirmAdhesiveIntermediate(@Valid @RequestBody HcAdhesiveIntermediateSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmAdhesiveIntermediate(reqVO));
    }

    @PostMapping("/adhesive/intermediate/import")
    @Operation(summary = "导入粘胶1中间品记录")
    public CommonResult<Integer> importAdhesiveIntermediate(@RequestParam("recordId") Long recordId,
                                                            @RequestParam("file") MultipartFile file) throws Exception {
        return success(hcProcessReportService.importAdhesiveIntermediate(recordId, file));
    }

    @GetMapping("/adhesive/intermediate/export")
    @Operation(summary = "导出粘胶1中间品记录")
    public void exportAdhesiveIntermediate(@RequestParam("recordId") Long recordId,
                                           HttpServletResponse response) throws Exception {
        hcProcessReportService.exportAdhesiveIntermediate(recordId, response);
    }

    @GetMapping("/adhesive2/fai/summary")
    @Operation(summary = "获得粘胶2 FAI 工艺参数点检摘要")
    public CommonResult<HcAdhesive2FaiRespVO> getAdhesive2FaiSummary(@RequestParam("planId") Long planId,
                                                                     @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getAdhesive2FaiSummary(planId, planOperationId));
    }

    @GetMapping("/adhesive2/fai/coa-list")
    @Operation(summary = "获得粘胶2 COA 送检单据列表")
    public CommonResult<List<HcAdhesive2FaiRespVO>> getAdhesive2CoaFaiList(@RequestParam("planId") Long planId,
                                                                           @RequestParam("planOperationId") Long planOperationId,
                                                                           @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getAdhesive2CoaFaiList(planId, planOperationId, motherBatchNo));
    }

    @GetMapping("/adhesive2/fai/process-check-list")
    @Operation(summary = "获得粘胶2过程加检 FAI 单据列表")
    public CommonResult<List<HcAdhesive2FaiRespVO>> getAdhesive2ProcessCheckFaiList(@RequestParam("planId") Long planId,
                                                                                    @RequestParam("planOperationId") Long planOperationId,
                                                                                    @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getAdhesive2ProcessCheckFaiList(planId, planOperationId, motherBatchNo));
    }

    @GetMapping("/adhesive2/source-press-slot/fai/list")
    @Operation(summary = "获得粘胶2来源压槽首检列表")
    public CommonResult<List<HcWetFaiRespVO>> getAdhesive2SourcePressSlotFaiList(
            @RequestParam("planId") Long planId,
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam("sourcePlanId") Long sourcePlanId,
            @RequestParam("sourcePlanOperationId") Long sourcePlanOperationId,
            @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getAdhesive2SourcePressSlotFaiList(
                planId, planOperationId, sourcePlanId, sourcePlanOperationId, motherBatchNo));
    }

    @GetMapping("/adhesive2/source-press-slot/fai/process-check-list")
    @Operation(summary = "获得粘胶2来源压槽过程加检列表")
    public CommonResult<List<HcWetFaiRespVO>> getAdhesive2SourcePressSlotProcessCheckFaiList(
            @RequestParam("planId") Long planId,
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam("sourcePlanId") Long sourcePlanId,
            @RequestParam("sourcePlanOperationId") Long sourcePlanOperationId,
            @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getAdhesive2SourcePressSlotProcessCheckFaiList(
                planId, planOperationId, sourcePlanId, sourcePlanOperationId, motherBatchNo));
    }

    @PostMapping("/adhesive2/fai/apply")
    @Operation(summary = "提交粘胶2 FAI 工艺参数点检申请")
    public CommonResult<HcAdhesive2FaiRespVO> applyAdhesive2Fai(@Valid @RequestBody HcAdhesive2FaiApplyReqVO reqVO) {
        return success(hcProcessReportService.applyAdhesive2Fai(reqVO));
    }

    @PostMapping("/adhesive2/fai/coa-withdraw")
    @Operation(summary = "撤回未处理的粘胶2成品COA送检单")
    public CommonResult<Boolean> withdrawAdhesive2Coa(@Valid @RequestBody HcAdhesive2CoaWithdrawReqVO reqVO) {
        hcProcessReportService.withdrawAdhesive2Coa(reqVO);
        return success(true);
    }

    @PostMapping("/adhesive2/fai/post-confirm-coa")
    @Operation(summary = "粘胶2扫码确认后提交成品 COA 送检")
    public CommonResult<HcAdhesive2FaiRespVO> applyAdhesive2PostConfirmCoa(
            @Valid @RequestBody HcAdhesiveReportConfirmReqVO reqVO) {
        return success(hcProcessReportService.applyAdhesive2PostConfirmCoa(reqVO));
    }

    @GetMapping("/adhesive2/runtime-product-snapshot")
    @Operation(summary = "获得粘胶2当前实际产品快照")
    public CommonResult<HcAdhesive2RuntimeProductRespVO> getAdhesive2RuntimeProductSnapshot(
            @RequestParam("planId") Long planId,
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "segmentBatchNo", required = false) String segmentBatchNo) {
        return success(hcProcessReportService.getAdhesive2RuntimeProductSnapshot(planId, planOperationId, segmentBatchNo));
    }

    @GetMapping("/adhesive2/task-list")
    @Operation(summary = "获得粘胶2操作看板任务列表")
    public CommonResult<List<HcAdhesiveReportTaskRespVO>> getAdhesive2TaskList(@Valid HcAdhesiveReportTaskPageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesive2TaskList(reqVO));
    }

    @PostMapping("/adhesive2/start")
    @Operation(summary = "执行粘胶2开工确认")
    public CommonResult<Long> startAdhesive2Report(@Valid @RequestBody HcAdhesiveReportStartReqVO reqVO) {
        return success(hcProcessReportService.startAdhesive2Report(reqVO));
    }

    @PostMapping("/adhesive2/switch-equipment")
    @Operation(summary = "切换粘胶2操作看板设备")
    public CommonResult<Boolean> switchAdhesive2Equipment(@Valid @RequestBody HcAdhesiveReportSwitchEquipmentReqVO reqVO) {
        hcProcessReportService.switchAdhesive2Equipment(reqVO);
        return success(true);
    }

    @GetMapping("/adhesive2/pass-work/list")
    @Operation(summary = "获得粘胶2加工前点检清洁列表")
    public CommonResult<List<HcWetPassWorkRespVO>> getAdhesive2PassWorkList(@RequestParam("planId") Long planId,
                                                                            @RequestParam("planOperationId") Long planOperationId,
                                                                            @RequestParam(value = "equipmentId", required = false) Long equipmentId,
                                                                            @RequestParam(value = "recordDate", required = false)
                                                                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcProcessReportService.getAdhesive2PassWorkList(planId, planOperationId, equipmentId, recordDate));
    }

    @PostMapping("/adhesive2/pass-work/save")
    @Operation(summary = "保存粘胶2加工前点检清洁")
    public CommonResult<Long> saveAdhesive2PassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesive2PassWork(reqVO));
    }

    @PostMapping("/adhesive2/pass-work/confirm")
    @Operation(summary = "确认粘胶2加工前点检清洁")
    public CommonResult<Long> confirmAdhesive2PassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmAdhesive2PassWork(reqVO));
    }

    @GetMapping("/adhesive2/check-template")
    @Operation(summary = "获得粘胶2点检模板")
    public CommonResult<List<HcAdhesiveCheckItemRespVO>> getAdhesive2CheckTemplate(
            @RequestParam(value = "modelCode", required = false) String modelCode) {
        return success(hcProcessReportService.getAdhesive2CheckTemplate(modelCode));
    }

    @GetMapping("/adhesive2/source/scan")
    @Operation(summary = "扫码查询粘胶2来源压槽片号")
    public CommonResult<HcAdhesiveSourceRespVO> scanAdhesive2Source(@RequestParam("batchNo") String batchNo) {
        return success(hcProcessReportService.scanAdhesive2Source(batchNo));
    }

    @GetMapping("/adhesive2/source/list")
    @Operation(summary = "获得粘胶2可加工来源压槽片号")
    public CommonResult<List<HcAdhesiveSourceRespVO>> getAdhesive2SourceList(@RequestParam("planId") Long planId,
                                                                             @RequestParam("planOperationId") Long planOperationId,
                                                                             @RequestParam(value = "sourceBatchNo", required = false) String sourceBatchNo) {
        return success(hcProcessReportService.getAdhesive2SourceList(planId, planOperationId, sourceBatchNo));
    }

    @GetMapping("/adhesive2/report/list")
    @Operation(summary = "获得粘胶2报工记录")
    public CommonResult<List<HcAdhesiveReportRespVO>> getAdhesive2ReportList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "scanConfirmDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scanConfirmDate) {
        return success(hcProcessReportService.getAdhesive2ReportList(planOperationId, scanConfirmDate));
    }

    @GetMapping("/adhesive2/glue-board-stock/page")
    @Operation(summary = "分页获得粘胶2胶板边库库存")
    public CommonResult<PageResult<HcAdhesiveGlueBoardStockRespVO>> getAdhesive2GlueBoardStockPage(
            @Valid HcAdhesiveGlueBoardStockPageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesive2GlueBoardStockPage(reqVO));
    }

    @GetMapping("/adhesive2/glue-board/current")
    @Operation(summary = "获得粘胶2当前胶板领用记录")
    public CommonResult<HcAdhesiveGlueBoardUsageRespVO> getAdhesive2CurrentGlueBoardUsage(
            @RequestParam(value = "planOperationId", required = false) Long planOperationId,
            @RequestParam(value = "glueBoardModel", required = false) String glueBoardModel) {
        return success(hcProcessReportService.getAdhesiveCurrentGlueBoardUsage(planOperationId, glueBoardModel, "ADHESIVE2"));
    }

    @GetMapping("/adhesive2/glue-board-fai/latest")
    @Operation(summary = "获得粘胶2当前分段胶板最后送检记录")
    public CommonResult<HcAdhesiveFaiRespVO> getAdhesive2LatestGlueBoardFai(
            @RequestParam(value = "planOperationId", required = false) Long planOperationId,
            @RequestParam(value = "sourceProductionBatchNo", required = false) String sourceProductionBatchNo,
            @RequestParam(value = "glueBoardStockId", required = false) Long glueBoardStockId,
            @RequestParam(value = "glueBoardBatchNo", required = false) String glueBoardBatchNo,
            @RequestParam(value = "glueBoardModel", required = false) String glueBoardModel) {
        return success(hcProcessReportService.getLatestAdhesiveGlueBoardFai(
                planOperationId, sourceProductionBatchNo, glueBoardStockId, glueBoardBatchNo, glueBoardModel, "ADHESIVE2"));
    }

    @GetMapping("/adhesive2/glue-board-stock/get-by-batch")
    @Operation(summary = "按批号获得粘胶2胶板边库库存")
    public CommonResult<HcAdhesiveGlueBoardStockRespVO> getAdhesive2GlueBoardStockByBatch(
            @RequestParam("glueBoardBatchNo") String glueBoardBatchNo,
            @RequestParam(value = "glueBoardModel", required = false) String glueBoardModel) {
        return success(hcProcessReportService.getAdhesive2GlueBoardStockByBatch(glueBoardBatchNo, glueBoardModel));
    }

    @PostMapping("/adhesive2/glue-board-stock/create")
    @Operation(summary = "新建粘胶2胶板边库领料")
    public CommonResult<Long> createAdhesive2GlueBoardStock(@Valid @RequestBody HcAdhesiveGlueBoardStockSaveReqVO reqVO) {
        return success(hcProcessReportService.createAdhesiveGlueBoardStock(reqVO));
    }

    @PostMapping("/adhesive2/glue-board-stock/mark-printed")
    @Operation(summary = "标记粘胶2胶板边库二维码已打印")
    public CommonResult<HcAdhesiveGlueBoardStockRespVO> markAdhesive2GlueBoardStockPrinted(@RequestParam("id") Long id) {
        return success(hcProcessReportService.markAdhesiveGlueBoardStockPrinted(id));
    }

    @GetMapping("/adhesive2/glue-board/list")
    @Operation(summary = "获得粘胶2胶板领用记录")
    public CommonResult<List<HcAdhesiveGlueBoardUsageRespVO>> getAdhesive2GlueBoardUsageList(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getAdhesiveGlueBoardUsageList(planOperationId));
    }

    @GetMapping("/adhesive2/glue-board/page")
    @Operation(summary = "分页获得粘胶2胶板边料台账")
    public CommonResult<PageResult<HcAdhesiveGlueBoardUsageRespVO>> getAdhesive2GlueBoardUsagePage(
            @Valid HcAdhesiveGlueBoardUsagePageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesiveGlueBoardUsagePage(reqVO));
    }

    @PostMapping("/adhesive2/glue-board/save")
    @Operation(summary = "保存粘胶2胶板领用记录")
    public CommonResult<Long> saveAdhesive2GlueBoardUsage(@Valid @RequestBody HcAdhesiveGlueBoardUsageSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesive2GlueBoardUsage(reqVO));
    }

    @PostMapping("/adhesive2/glue-board/loss")
    @Operation(summary = "报备粘胶2胶板损耗")
    public CommonResult<HcAdhesiveGlueBoardUsageRespVO> reportAdhesive2GlueBoardLoss(
            @Valid @RequestBody HcAdhesiveGlueBoardLossReqVO reqVO) {
        return success(hcProcessReportService.reportAdhesiveGlueBoardLoss(reqVO));
    }

    @PostMapping("/adhesive2/glue-board/return")
    @Operation(summary = "退回释放粘胶2胶板领用")
    public CommonResult<HcAdhesiveGlueBoardUsageRespVO> returnAdhesive2GlueBoardUsage(@RequestParam("id") Long id) {
        return success(hcProcessReportService.returnAdhesiveGlueBoardUsage(id));
    }

    @PostMapping("/adhesive2/aqc/submit")
    @Operation(summary = "提交粘胶2AQC首检任务")
    public CommonResult<HcAdhesiveAqcTaskRespVO> submitAdhesive2AqcTask(@Valid @RequestBody HcAdhesiveAqcTaskSaveReqVO reqVO) {
        return success(hcProcessReportService.submitAdhesiveAqcTask(reqVO));
    }

    @PostMapping("/adhesive2/aqc/feedback")
    @Operation(summary = "反馈粘胶2AQC首检结果")
    public CommonResult<HcAdhesiveAqcTaskRespVO> feedbackAdhesive2AqcTask(@Valid @RequestBody HcAdhesiveAqcTaskFeedbackReqVO reqVO) {
        return success(hcProcessReportService.feedbackAdhesiveAqcTask(reqVO));
    }

    @PostMapping("/adhesive2/report/save")
    @Operation(summary = "保存粘胶2报工记录")
    public CommonResult<Long> saveAdhesive2Report(@Valid @RequestBody HcAdhesiveReportSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesive2Report(reqVO));
    }

    @PostMapping("/adhesive2/report/tail-assign")
    @Operation(summary = "批量选择粘胶2报工片号尾号")
    public CommonResult<List<Long>> assignAdhesive2ReportTails(@Valid @RequestBody HcAdhesive2TailAssignReqVO reqVO) {
        return success(hcProcessReportService.assignAdhesive2ReportTails(reqVO));
    }

    @PostMapping("/adhesive2/tail-selection/apply-selected")
    @Operation(summary = "按片选择粘胶2来源片尾号")
    public CommonResult<Integer> assignAdhesive2TailForSelectedSources(
            @Valid @RequestBody HcAdhesive2TailSelectedAssignReqVO reqVO) {
        return success(hcProcessReportService.assignAdhesive2TailForSelectedSources(reqVO));
    }

    @PostMapping("/adhesive2/tail-selection/apply-all")
    @Operation(summary = "统一选择粘胶2全部来源片尾号")
    public CommonResult<Integer> assignAdhesive2TailForAllSources(
            @Valid @RequestBody HcAdhesive2TailBatchAssignReqVO reqVO) {
        return success(hcProcessReportService.assignAdhesive2TailForAllSources(reqVO));
    }

    @PostMapping("/adhesive2/report/save-confirm")
    @Operation(summary = "保存并扫码确认粘胶2报工记录")
    public CommonResult<Long> saveAndConfirmAdhesive2Report(
            @Valid @RequestBody HcAdhesiveReportSaveConfirmReqVO reqVO) {
        return success(hcProcessReportService.saveAndConfirmAdhesive2Report(reqVO));
    }

    @PostMapping("/adhesive2/report/confirm")
    @Operation(summary = "扫码确认粘胶2报工记录")
    public CommonResult<Long> confirmAdhesive2Report(@Valid @RequestBody HcAdhesiveReportConfirmReqVO reqVO) {
        return success(hcProcessReportService.confirmAdhesive2Report(reqVO));
    }

    @PostMapping("/adhesive2/report/set-middle-type")
    @Operation(summary = "设置粘胶2报工记录为中间品段位")
    public CommonResult<Boolean> setAdhesive2ReportMiddleType(@RequestParam("id") Long id,
                                                              @RequestParam("reportType") String reportType) {
        hcProcessReportService.setAdhesive2ReportMiddleType(id, reportType);
        return success(true);
    }

    @PostMapping("/adhesive2/report/abnormal-category/correct")
    @Operation(summary = "修正粘胶2报工外观异常类别")
    public CommonResult<HcAdhesiveReportRespVO> correctAdhesive2ReportAbnormalCategory(
            @Valid @RequestBody HcAdhesive2AbnormalCategoryCorrectReqVO reqVO) {
        return success(hcProcessReportService.correctAdhesive2ReportAbnormalCategory(reqVO));
    }

    @PostMapping("/adhesive2/report/mark-printed")
    @Operation(summary = "标记粘胶2报工记录已打印")
    public CommonResult<Boolean> markAdhesive2ReportPrinted(@Valid @RequestBody HcAdhesiveReportPrintReqVO reqVO) {
        hcProcessReportService.markAdhesive2ReportPrinted(reqVO);
        return success(true);
    }

    @PostMapping("/adhesive2/submit")
    @Operation(summary = "提交粘胶2过站报工")
    public CommonResult<Long> submitAdhesive2Report(@Valid @RequestBody HcAdhesiveReportSubmitReqVO reqVO) {
        return success(hcProcessReportService.submitAdhesive2Report(reqVO));
    }

    @PostMapping("/adhesive2/segment/complete")
    @Operation(summary = "粘胶2当前分段完成")
    public CommonResult<Long> completeAdhesive2Segment(@Valid @RequestBody HcAdhesiveSegmentCompleteReqVO reqVO) {
        return success(hcProcessReportService.completeAdhesive2Segment(reqVO));
    }

    @GetMapping("/adhesive2/intermediate/get")
    @Operation(summary = "获得粘胶2中间品记录")
    public CommonResult<HcAdhesive2IntermediateRespVO> getAdhesive2Intermediate(@RequestParam("planId") Long planId,
                                                                                @RequestParam("planOperationId") Long planOperationId,
                                                                                @RequestParam(value = "recordDate", required = false)
                                                                                @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime recordDate,
                                                                                @RequestParam(value = "id", required = false) Long recordId,
                                                                                @RequestParam(value = "batchNo", required = false) String batchNo) {
        return success(hcProcessReportService.getAdhesive2Intermediate(planId, planOperationId, recordDate, recordId, batchNo));
    }

    @GetMapping("/adhesive2/intermediate/record")
    @Operation(summary = "按记录 ID 获得粘胶2中间品记录")
    public CommonResult<HcAdhesive2IntermediateRespVO> getAdhesive2IntermediateById(@RequestParam("recordId") Long recordId) {
        return success(hcProcessReportService.getAdhesive2IntermediateById(recordId));
    }

    @GetMapping("/adhesive2/intermediate/page")
    @Operation(summary = "分页获得粘胶2中间品记录业务记录")
    public CommonResult<PageResult<HcStationRecordRespVO>> getAdhesive2IntermediateRecordPage(@Valid HcStationRecordPageReqVO reqVO) {
        return success(hcProcessReportService.getAdhesive2IntermediateRecordPage(reqVO));
    }

    @PostMapping("/adhesive2/intermediate/save")
    @Operation(summary = "保存粘胶2中间品记录")
    public CommonResult<Long> saveAdhesive2Intermediate(@Valid @RequestBody HcAdhesive2IntermediateSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesive2Intermediate(reqVO));
    }

    @GetMapping("/adhesive2/intermediate/list")
    @Operation(summary = "获得粘胶2中间品记录列表")
    public CommonResult<List<HcAdhesive2IntermediateRespVO>> getAdhesive2IntermediateList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "batchNo", required = false) String batchNo) {
        return success(hcProcessReportService.getAdhesive2IntermediateList(planOperationId, batchNo));
    }

    @GetMapping("/adhesive2/middle-ledger/list")
    @Operation(summary = "获得粘胶2中间品台账")
    public CommonResult<List<HcPressSlotMiddleLedgerRespVO>> getAdhesive2MiddleLedgerList(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getAdhesive2MiddleLedgerList(planOperationId));
    }

    @PostMapping("/adhesive2/middle-ledger/import")
    @Operation(summary = "导入粘胶2中间品台账")
    public CommonResult<Integer> importAdhesive2MiddleLedger(@RequestParam("planId") Long planId,
                                                             @RequestParam("planOperationId") Long planOperationId,
                                                             @RequestParam("file") MultipartFile file,
                                                             @RequestParam(value = "importAttachment", required = false) String importAttachmentJson,
                                                             @RequestParam(value = "recordId", required = false) Long recordId,
                                                             @RequestParam(value = "batchNo", required = false) String batchNo,
                                                             @RequestParam(value = "recordDate", required = false)
                                                             @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime recordDate) throws Exception {
        return success(hcProcessReportService.importAdhesive2MiddleLedger(planId, planOperationId, file,
                importAttachmentJson, recordId, batchNo, recordDate));
    }

    @GetMapping("/adhesive2/middle-ledger/export")
    @Operation(summary = "导出粘胶2中间品台账")
    public void exportAdhesive2MiddleLedger(@RequestParam("planOperationId") Long planOperationId,
                                            @RequestParam(value = "recordId", required = false) Long recordId,
                                            @RequestParam(value = "batchNo", required = false) String batchNo,
                                            HttpServletResponse response) throws Exception {
        hcProcessReportService.exportAdhesive2MiddleLedger(planOperationId, recordId, batchNo, response);
    }

    @GetMapping("/adhesive2/process-param/list")
    @Operation(summary = "获得粘胶2动态工艺参数点检记录")
    public CommonResult<List<HcPressSlotProcessParamRespVO>> getAdhesive2ProcessParamList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "reportDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reportDate,
            @RequestParam(value = "productionBatchNo", required = false) String productionBatchNo,
            @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo,
            @RequestParam(value = "formType", required = false) String formType) {
        return success(hcProcessReportService.getAdhesive2ProcessParamList(planOperationId, reportDate,
                productionBatchNo, motherBatchNo, formType));
    }

    @PostMapping("/adhesive2/process-param/import")
    @Operation(summary = "导入粘胶2动态工艺参数点检记录")
    public CommonResult<Integer> importAdhesive2ProcessParams(@RequestParam("planId") Long planId,
                                                              @RequestParam("planOperationId") Long planOperationId,
                                                              @RequestParam("file") MultipartFile file,
                                                              @RequestParam(value = "importAttachment", required = false) String importAttachmentJson,
                                                              @RequestParam(value = "recordId", required = false) Long recordId,
                                                              @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo,
                                                              @RequestParam(value = "productionBatchNo", required = false) String productionBatchNo,
                                                              @RequestParam(value = "formType", required = false) String formType,
                                                              @RequestParam(value = "inspectionScene", required = false) String inspectionScene,
                                                              @RequestParam(value = "recordDate", required = false)
                                                              @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate,
                                                              @RequestParam(value = "changeoverInstructionId", required = false) Long changeoverInstructionId) throws Exception {
        return success(hcProcessReportService.importAdhesive2ProcessParams(planId, planOperationId, file,
                importAttachmentJson, recordId, motherBatchNo, productionBatchNo, formType, inspectionScene, recordDate,
                changeoverInstructionId));
    }

    @PostMapping("/adhesive2/process-param/save")
    @Operation(summary = "保存粘胶2动态工艺参数点检记录")
    public CommonResult<Long> saveAdhesive2ProcessParam(@Valid @RequestBody HcPressSlotProcessParamSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesive2ProcessParam(reqVO));
    }

    @PostMapping("/adhesive2/process-param/confirm")
    @Operation(summary = "确认粘胶2动态工艺参数点检记录")
    public CommonResult<Long> confirmAdhesive2ProcessParam(@Valid @RequestBody HcPressSlotProcessParamSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmAdhesive2ProcessParam(reqVO));
    }

    @DeleteMapping("/adhesive2/process-param/delete")
    @Operation(summary = "删除粘胶2动态工艺参数点检记录")
    public CommonResult<Boolean> deleteAdhesive2ProcessParam(@RequestParam("planOperationId") Long planOperationId,
                                                             @RequestParam("recordId") Long recordId) {
        hcProcessReportService.deleteAdhesive2ProcessParam(planOperationId, recordId);
        return success(true);
    }

    @GetMapping("/adhesive2/process-param/export")
    @Operation(summary = "导出粘胶2动态工艺参数点检记录")
    public void exportAdhesive2ProcessParams(@RequestParam("planOperationId") Long planOperationId,
                                             @RequestParam(value = "recordId", required = false) Long recordId,
                                             @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo,
                                             @RequestParam(value = "formType", required = false) String formType,
                                             HttpServletResponse response) throws Exception {
        hcProcessReportService.exportAdhesive2ProcessParams(planOperationId, recordId, motherBatchNo, formType, response);
    }

    @GetMapping("/adhesive2/changeover/latest")
    @Operation(summary = "获得粘胶2当天最新工艺参数点检记录")
    public CommonResult<HcPressSlotChangeoverInspectionRespVO> getLatestAdhesive2ChangeoverInspection(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getLatestAdhesive2ChangeoverInspection(planOperationId));
    }

    @GetMapping("/adhesive2/changeover/list")
    @Operation(summary = "获得粘胶2工艺参数点检记录列表")
    public CommonResult<List<HcPressSlotChangeoverInspectionRespVO>> getAdhesive2ChangeoverInspectionList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "motherSegmentBatchNo", required = false) String motherSegmentBatchNo) {
        return success(hcProcessReportService.getAdhesive2ChangeoverInspectionList(planOperationId, motherSegmentBatchNo));
    }

    @PostMapping("/adhesive2/changeover/save")
    @Operation(summary = "保存粘胶2工艺参数点检记录")
    public CommonResult<Long> saveAdhesive2ChangeoverInspection(
            @Valid @RequestBody HcPressSlotChangeoverInspectionSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesive2ChangeoverInspection(reqVO));
    }

    @GetMapping("/adhesive2/plan-changeover/latest")
    @Operation(summary = "获得粘胶2计划最新换型日志")
    public CommonResult<HcPlanChangeoverLogRespVO> getLatestAdhesive2PlanChangeover(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getLatestAdhesive2PlanChangeover(planOperationId));
    }

    @GetMapping("/adhesive2/plan-changeover/list")
    @Operation(summary = "获得粘胶2计划换型日志列表")
    public CommonResult<List<HcPlanChangeoverLogRespVO>> getAdhesive2PlanChangeoverList(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getAdhesive2PlanChangeoverList(planOperationId));
    }

    @PostMapping("/adhesive2/plan-changeover/save")
    @Operation(summary = "保存粘胶2计划换型日志")
    public CommonResult<HcPlanChangeoverLogRespVO> saveAdhesive2PlanChangeover(
            @Valid @RequestBody HcPlanChangeoverLogSaveReqVO reqVO) {
        return success(hcProcessReportService.saveAdhesive2PlanChangeover(reqVO));
    }

    @GetMapping("/press-slot/task-list")
    @Operation(summary = "获得压槽操作看板任务列表")
    public CommonResult<List<HcAdhesiveReportTaskRespVO>> getPressSlotTaskList(@Valid HcAdhesiveReportTaskPageReqVO reqVO) {
        return success(hcProcessReportService.getPressSlotTaskList(reqVO));
    }

    @GetMapping("/press-slot/form-record/page")
    @Operation(summary = "分页获得压槽表单填写记录")
    public CommonResult<PageResult<HcPressSlotFormRecordRespVO>> getPressSlotFormRecordPage(
            @Valid HcPressSlotFormRecordPageReqVO reqVO) {
        return success(hcProcessReportService.getPressSlotFormRecordPage(reqVO));
    }

    @GetMapping("/press-slot/fai/summary")
    @Operation(summary = "获得压槽操作看板首检摘要")
    public CommonResult<HcWetFaiRespVO> getPressSlotFaiSummary(@RequestParam("planId") Long planId,
                                                               @RequestParam("planOperationId") Long planOperationId,
                                                               @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getPressSlotFaiSummary(planId, planOperationId, motherBatchNo));
    }

    @GetMapping("/press-slot/fai/list")
    @Operation(summary = "获得压槽操作看板首检列表")
    public CommonResult<List<HcWetFaiRespVO>> getPressSlotFaiList(@RequestParam("planId") Long planId,
                                                                  @RequestParam("planOperationId") Long planOperationId,
                                                                  @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getPressSlotFaiList(planId, planOperationId, motherBatchNo));
    }

    @GetMapping("/press-slot/fai/process-check-list")
    @Operation(summary = "获得压槽操作看板过程加检记录")
    public CommonResult<List<HcWetFaiRespVO>> getPressSlotProcessCheckFaiList(@RequestParam("planId") Long planId,
                                                                              @RequestParam("planOperationId") Long planOperationId,
                                                                              @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getPressSlotProcessCheckFaiList(planId, planOperationId, motherBatchNo));
    }

    @GetMapping("/press-slot/abnormal-lock/active")
    @Operation(summary = "获得压槽操作看板活动异常锁定摘要")
    public CommonResult<HcPressSlotAbnormalLockRespVO> getPressSlotActiveAbnormalLock(@RequestParam("planId") Long planId,
                                                                                      @RequestParam("planOperationId") Long planOperationId,
                                                                                      @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getPressSlotActiveAbnormalLock(planId, planOperationId, motherBatchNo));
    }

    @GetMapping("/press-slot/abnormal-lock/list")
    @Operation(summary = "获得压槽操作看板异常锁定记录")
    public CommonResult<List<HcPressSlotAbnormalLockRespVO>> getPressSlotAbnormalLockList(@RequestParam("planId") Long planId,
                                                                                          @RequestParam("planOperationId") Long planOperationId,
                                                                                          @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo) {
        return success(hcProcessReportService.getPressSlotAbnormalLockList(planId, planOperationId, motherBatchNo));
    }

    @GetMapping("/press-slot/fai/validate-scan")
    @Operation(summary = "校验压槽扫码前首检/过程加检连续性")
    public CommonResult<HcPressSlotScanGateRespVO> validatePressSlotFaiScan(@RequestParam("planId") Long planId,
                                                                            @RequestParam("planOperationId") Long planOperationId,
                                                                            @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo,
                                                                            @RequestParam(value = "productionBatchNo", required = false) String productionBatchNo,
                                                                            @RequestParam(value = "reportType", required = false) String reportType) {
        return success(hcProcessReportService.validatePressSlotFaiScan(planId, planOperationId,
                motherBatchNo, productionBatchNo, reportType));
    }

    @PostMapping("/press-slot/fai/apply")
    @Operation(summary = "提交压槽操作看板首检申请")
    public CommonResult<HcWetFaiRespVO> applyPressSlotFai(@Valid @RequestBody HcWetFaiApplyReqVO reqVO) {
        return success(hcProcessReportService.applyPressSlotFai(reqVO));
    }

    @PostMapping("/press-slot/fai/withdraw")
    @Operation(summary = "撤回未处理的压槽首检或过程加检送检单")
    public CommonResult<Boolean> withdrawPressSlotFai(@Valid @RequestBody HcPressSlotFaiWithdrawReqVO reqVO) {
        hcProcessReportService.withdrawPressSlotFai(reqVO);
        return success(true);
    }

    @PostMapping("/press-slot/start")
    @Operation(summary = "执行压槽开工确认")
    public CommonResult<Long> startPressSlotReport(@Valid @RequestBody HcAdhesiveReportStartReqVO reqVO) {
        return success(hcProcessReportService.startPressSlotReport(reqVO));
    }

    @PostMapping("/press-slot/switch-equipment")
    @Operation(summary = "切换压槽操作看板设备")
    public CommonResult<Boolean> switchPressSlotEquipment(@Valid @RequestBody HcAdhesiveReportSwitchEquipmentReqVO reqVO) {
        hcProcessReportService.switchPressSlotEquipment(reqVO);
        return success(true);
    }

    @GetMapping("/press-slot/pass-work/list")
    @Operation(summary = "获得压槽开机前准备列表")
    public CommonResult<List<HcWetPassWorkRespVO>> getPressSlotPassWorkList(@RequestParam("planId") Long planId,
                                                                            @RequestParam("planOperationId") Long planOperationId,
                                                                            @RequestParam(value = "equipmentId", required = false) Long equipmentId,
                                                                            @RequestParam(value = "recordDate", required = false)
                                                                            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcProcessReportService.getPressSlotPassWorkList(planId, planOperationId, equipmentId, recordDate));
    }

    @PostMapping("/press-slot/pass-work/save")
    @Operation(summary = "保存压槽开机前准备")
    public CommonResult<Long> savePressSlotPassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.savePressSlotPassWork(reqVO));
    }

    @PostMapping("/press-slot/pass-work/confirm")
    @Operation(summary = "确认压槽开机前准备")
    public CommonResult<Long> confirmPressSlotPassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmPressSlotPassWork(reqVO));
    }

    @GetMapping("/press-slot/production-check-template")
    @Operation(summary = "按型号前缀解析压槽生产点检正式模板")
    public CommonResult<cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO> getPressSlotProductionCheckTemplate(
            @RequestParam("modelCode") String modelCode) {
        return success(hcProcessReportService.getPressSlotProductionCheckTemplate(modelCode));
    }

    @GetMapping("/press-slot/check-template")
    @Operation(summary = "获得压槽报工点检模板")
    public CommonResult<List<HcAdhesiveCheckItemRespVO>> getPressSlotCheckTemplate(
            @RequestParam(value = "modelCode", required = false) String modelCode) {
        return success(hcProcessReportService.getPressSlotCheckTemplate(modelCode));
    }

    @GetMapping("/press-slot/source/scan")
    @Operation(summary = "扫码查询压槽来源分切片号")
    public CommonResult<HcAdhesiveSourceRespVO> scanPressSlotSource(
            @RequestParam("batchNo") String batchNo,
            @RequestParam(value = "includeAbnormal", required = false) Boolean includeAbnormal) {
        return success(hcProcessReportService.scanPressSlotSource(batchNo, includeAbnormal));
    }

    @GetMapping("/press-slot/source/list")
    @Operation(summary = "获得压槽可加工来源分切片号")
    public CommonResult<List<HcAdhesiveSourceRespVO>> getPressSlotSourceList(@RequestParam("planId") Long planId,
                                                                             @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getPressSlotSourceList(planId, planOperationId));
    }

    @GetMapping("/press-slot/report/list")
    @Operation(summary = "获得压槽报工记录")
    public CommonResult<List<HcAdhesiveReportRespVO>> getPressSlotReportList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "scanConfirmDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scanConfirmDate) {
        return success(hcProcessReportService.getPressSlotReportList(planOperationId, scanConfirmDate));
    }

    @GetMapping("/press-slot/consumable/status")
    @Operation(summary = "获得压槽工位压辊/轴承挂接状态")
    public CommonResult<List<HcPressSlotConsumableRespVO>> getPressSlotConsumableStatus(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "equipmentId", required = false) Long equipmentId) {
        return success(hcProcessReportService.getPressSlotConsumableStatus(planOperationId, equipmentId));
    }

    @GetMapping("/press-slot/consumable/material-options")
    @Operation(summary = "获得压槽工位压辊/轴承 ERP 相似物料")
    public CommonResult<List<HcPressSlotConsumableMaterialRespVO>> getPressSlotConsumableMaterialOptions(
            @RequestParam("consumableType") String consumableType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return success(hcProcessReportService.getPressSlotConsumableMaterialOptions(consumableType, keyword, pageSize));
    }

    @PostMapping("/press-slot/consumable/replace")
    @Operation(summary = "压槽工位压辊/轴承更换")
    public CommonResult<Long> replacePressSlotConsumable(@Valid @RequestBody HcPressSlotConsumableReplaceReqVO reqVO) {
        return success(hcProcessReportService.replacePressSlotConsumable(reqVO));
    }

    @PostMapping("/press-slot/consumable/clean")
    @Operation(summary = "压槽工位压辊清洗复位")
    public CommonResult<Long> cleanPressSlotConsumable(@Valid @RequestBody HcPressSlotConsumableCleanReqVO reqVO) {
        return success(hcProcessReportService.cleanPressSlotConsumable(reqVO));
    }

    @PostMapping("/press-slot/aqc/submit")
    @Operation(summary = "提交压槽首检任务")
    public CommonResult<HcAdhesiveAqcTaskRespVO> submitPressSlotAqcTask(@Valid @RequestBody HcAdhesiveAqcTaskSaveReqVO reqVO) {
        return success(hcProcessReportService.submitAdhesiveAqcTask(reqVO));
    }

    @PostMapping("/press-slot/report/save")
    @Operation(summary = "保存压槽报工记录")
    public CommonResult<Long> savePressSlotReport(@Valid @RequestBody HcAdhesiveReportSaveReqVO reqVO) {
        return success(hcProcessReportService.savePressSlotReport(reqVO));
    }

    @PostMapping("/press-slot/report/save-confirm")
    @Operation(summary = "保存并扫码确认压槽报工记录")
    public CommonResult<Long> saveAndConfirmPressSlotReport(@Valid @RequestBody HcAdhesiveReportSaveConfirmReqVO reqVO) {
        return success(hcProcessReportService.saveAndConfirmPressSlotReport(reqVO));
    }

    @PostMapping("/press-slot/report/confirm")
    @Operation(summary = "扫码确认压槽报工记录")
    public CommonResult<Long> confirmPressSlotReport(@Valid @RequestBody HcAdhesiveReportConfirmReqVO reqVO) {
        return success(hcProcessReportService.confirmPressSlotReport(reqVO));
    }

    @PostMapping("/press-slot/report/set-middle-type")
    @Operation(summary = "设置压槽报工记录为中间品段位")
    public CommonResult<Boolean> setPressSlotReportMiddleType(@RequestParam("id") Long id,
                                                              @RequestParam("reportType") String reportType) {
        hcProcessReportService.setPressSlotReportMiddleType(id, reportType);
        return success(true);
    }

    @PostMapping("/press-slot/report/abnormal-category/correct")
    @Operation(summary = "修正压槽报工外观异常类别")
    public CommonResult<HcAdhesiveReportRespVO> correctPressSlotReportAbnormalCategory(
            @Valid @RequestBody HcVisualAbnormalCategoryCorrectReqVO reqVO) {
        return success(hcProcessReportService.correctPressSlotReportAbnormalCategory(reqVO));
    }

    @PostMapping("/press-slot/report/mark-printed")
    @Operation(summary = "标记压槽报工记录已打印")
    public CommonResult<Boolean> markPressSlotReportPrinted(@Valid @RequestBody HcAdhesiveReportPrintReqVO reqVO) {
        hcProcessReportService.markPressSlotReportPrinted(reqVO);
        return success(true);
    }

    @PostMapping("/press-slot/submit")
    @Operation(summary = "提交压槽过站报工")
    public CommonResult<Long> submitPressSlotReport(@Valid @RequestBody HcAdhesiveReportSubmitReqVO reqVO) {
        return success(hcProcessReportService.submitPressSlotReport(reqVO));
    }

    @PostMapping("/press-slot/segment/complete")
    @Operation(summary = "压槽当前分段完工")
    public CommonResult<Long> completePressSlotSegment(@Valid @RequestBody HcPressSlotSegmentCompleteReqVO reqVO) {
        return success(hcProcessReportService.completePressSlotSegment(reqVO));
    }

    @GetMapping("/press-slot/intermediate/get")
    @Operation(summary = "获得压槽中间品记录")
    public CommonResult<HcPressSlotIntermediateRespVO> getPressSlotIntermediate(@RequestParam(value = "id", required = false) Long id,
                                                                               @RequestParam("planId") Long planId,
                                                                               @RequestParam("planOperationId") Long planOperationId,
                                                                               @RequestParam(value = "batchNo", required = false) String batchNo,
                                                                               @RequestParam(value = "recordDate", required = false)
                                                                               @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcProcessReportService.getPressSlotIntermediate(planId, planOperationId, recordDate, id, batchNo));
    }

    @GetMapping("/press-slot/intermediate/list")
    @Operation(summary = "获得压槽中间品记录列表")
    public CommonResult<List<HcPressSlotIntermediateRespVO>> getPressSlotIntermediateList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "batchNo", required = false) String batchNo) {
        return success(hcProcessReportService.getPressSlotIntermediateList(planOperationId, batchNo));
    }

    @PostMapping("/press-slot/intermediate/save")
    @Operation(summary = "保存压槽中间品记录")
    public CommonResult<Long> savePressSlotIntermediate(@Valid @RequestBody HcPressSlotIntermediateSaveReqVO reqVO) {
        return success(hcProcessReportService.savePressSlotIntermediate(reqVO));
    }

    @PostMapping("/press-slot/intermediate/confirm")
    @Operation(summary = "确认压槽中间品记录")
    public CommonResult<Long> confirmPressSlotIntermediate(@Valid @RequestBody HcPressSlotIntermediateSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmPressSlotIntermediate(reqVO));
    }

    @GetMapping("/press-slot/middle-ledger/list")
    @Operation(summary = "获得压槽中间品台账")
    public CommonResult<List<HcPressSlotMiddleLedgerRespVO>> getPressSlotMiddleLedgerList(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getPressSlotMiddleLedgerList(planOperationId));
    }

    @PostMapping("/press-slot/middle-ledger/import")
    @Operation(summary = "导入压槽中间品台账")
    public CommonResult<Integer> importPressSlotMiddleLedger(@RequestParam("planId") Long planId,
                                                             @RequestParam("planOperationId") Long planOperationId,
                                                             @RequestParam("file") MultipartFile file,
                                                             @RequestParam(value = "importAttachment", required = false) String importAttachmentJson,
                                                             @RequestParam(value = "recordId", required = false) Long recordId,
                                                             @RequestParam(value = "batchNo", required = false) String batchNo,
                                                             @RequestParam(value = "recordDate", required = false)
                                                             @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate)
            throws Exception {
        return success(hcProcessReportService.importPressSlotMiddleLedger(planId, planOperationId, file,
                importAttachmentJson, recordId, batchNo, recordDate));
    }

    @GetMapping("/press-slot/middle-ledger/export")
    @Operation(summary = "导出压槽中间品台账")
    public void exportPressSlotMiddleLedger(@RequestParam("planOperationId") Long planOperationId,
                                            @RequestParam(value = "recordId", required = false) Long recordId,
                                            @RequestParam(value = "batchNo", required = false) String batchNo,
                                            HttpServletResponse response) throws Exception {
        hcProcessReportService.exportPressSlotMiddleLedger(planOperationId, recordId, batchNo, response);
    }

    @GetMapping("/press-slot/changeover/latest")
    @Operation(summary = "获得压槽当天最新首检记录")
    public CommonResult<HcPressSlotChangeoverInspectionRespVO> getLatestPressSlotChangeoverInspection(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getLatestPressSlotChangeoverInspection(planOperationId));
    }

    @GetMapping("/press-slot/changeover/list")
    @Operation(summary = "获得压槽首检记录列表")
    public CommonResult<List<HcPressSlotChangeoverInspectionRespVO>> getPressSlotChangeoverInspectionList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "motherSegmentBatchNo", required = false) String motherSegmentBatchNo) {
        return success(hcProcessReportService.getPressSlotChangeoverInspectionList(planOperationId, motherSegmentBatchNo));
    }

    @PostMapping("/press-slot/changeover/save")
    @Operation(summary = "保存压槽首检记录")
    public CommonResult<Long> savePressSlotChangeoverInspection(
            @Valid @RequestBody HcPressSlotChangeoverInspectionSaveReqVO reqVO) {
        return success(hcProcessReportService.savePressSlotChangeoverInspection(reqVO));
    }

    @GetMapping("/press-slot/process-param/list")
    @Operation(summary = "获得CMP压槽工艺参数记录表")
    public CommonResult<List<HcPressSlotProcessParamRespVO>> getPressSlotProcessParamList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "reportDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate reportDate,
            @RequestParam(value = "productionBatchNo", required = false) String productionBatchNo,
            @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo,
            @RequestParam(value = "formType", required = false) String formType) {
        return success(hcProcessReportService.getPressSlotProcessParamList(planOperationId, reportDate,
                productionBatchNo, motherBatchNo, formType));
    }

    @PostMapping("/press-slot/process-param/import")
    @Operation(summary = "导入CMP压槽工艺参数记录表")
    public CommonResult<Integer> importPressSlotProcessParams(@RequestParam("planId") Long planId,
                                                              @RequestParam("planOperationId") Long planOperationId,
                                                              @RequestParam("file") MultipartFile file,
                                                              @RequestParam(value = "importAttachment", required = false) String importAttachmentJson,
                                                              @RequestParam(value = "recordId", required = false) Long recordId,
                                                              @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo,
                                                              @RequestParam(value = "productionBatchNo", required = false) String productionBatchNo,
                                                              @RequestParam(value = "formType", required = false) String formType,
                                                              @RequestParam(value = "inspectionScene", required = false) String inspectionScene,
                                                              @RequestParam(value = "recordDate", required = false)
                                                              @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate)
            throws Exception {
        return success(hcProcessReportService.importPressSlotProcessParams(planId, planOperationId, file,
                importAttachmentJson, recordId, motherBatchNo, productionBatchNo, formType, inspectionScene, recordDate));
    }

    @PostMapping("/press-slot/process-param/save")
    @Operation(summary = "保存CMP压槽工艺参数记录")
    public CommonResult<Long> savePressSlotProcessParam(@Valid @RequestBody HcPressSlotProcessParamSaveReqVO reqVO) {
        return success(hcProcessReportService.savePressSlotProcessParam(reqVO));
    }

    @PostMapping("/press-slot/process-param/confirm")
    @Operation(summary = "确认CMP压槽工艺参数记录")
    public CommonResult<Long> confirmPressSlotProcessParam(@Valid @RequestBody HcPressSlotProcessParamSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmPressSlotProcessParam(reqVO));
    }

    @GetMapping("/press-slot/process-param/export")
    @Operation(summary = "导出CMP压槽工艺参数记录表")
    public void exportPressSlotProcessParams(@RequestParam("planOperationId") Long planOperationId,
                                             @RequestParam(value = "recordId", required = false) Long recordId,
                                             @RequestParam(value = "motherBatchNo", required = false) String motherBatchNo,
                                             @RequestParam(value = "formType", required = false) String formType,
                                             HttpServletResponse response) throws Exception {
        hcProcessReportService.exportPressSlotProcessParams(planOperationId, recordId, motherBatchNo, formType, response);
    }

    private HcAdhesiveCheckItemRespVO pressSlotCheckItem(String category, String itemName, String standardValue, Integer sortNo) {
        HcAdhesiveCheckItemRespVO item = new HcAdhesiveCheckItemRespVO();
        item.setItemCategory(category);
        item.setItemName(itemName);
        item.setStandardValue(standardValue);
        item.setSortNo(sortNo);
        return item;
    }

    @GetMapping("/slitting/task-list")
    @Operation(summary = "获得分切操作看板任务列表")
    public CommonResult<List<HcSlittingReportTaskRespVO>> getSlittingTaskList(
            @RequestParam(value = "taskStatus", required = false) String taskStatus,
            @RequestParam(value = "taskKeyword", required = false) String taskKeyword) {
        return success(hcProcessReportService.getSlittingTaskList(taskStatus, taskKeyword));
    }

    @GetMapping("/slitting/source/list")
    @Operation(summary = "获得分切来源粘胶分段列表")
    public CommonResult<List<HcSlittingSourceRespVO>> getSlittingSourceList(@RequestParam("planId") Long planId,
                                                                            @RequestParam("planOperationId") Long planOperationId,
                                                                            @RequestParam(value = "productionBatchNo", required = false) String productionBatchNo) {
        return success(hcProcessReportService.getSlittingSourceList(planId, planOperationId, productionBatchNo));
    }

    @PostMapping("/slitting/source/complete")
    @Operation(summary = "标记分切来源段切片完成")
    public CommonResult<HcSlittingSourceRespVO> completeSlittingSource(@Valid @RequestBody HcSlittingSourceCompleteReqVO reqVO) {
        return success(hcProcessReportService.completeSlittingSource(reqVO));
    }

    @GetMapping("/slitting/slice/list")
    @Operation(summary = "获得分切切片记录列表")
    public CommonResult<List<HcSlittingSliceRespVO>> getSlittingSliceList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "scanConfirmDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scanConfirmDate) {
        return success(hcProcessReportService.getSlittingSliceList(planOperationId, scanConfirmDate));
    }

    @PostMapping("/slitting/slice/generate")
    @Operation(summary = "生成分切切片记录")
    public CommonResult<List<HcSlittingSliceRespVO>> generateSlittingSlices(@Valid @RequestBody HcSlittingSliceGenerateReqVO reqVO) {
        return success(hcProcessReportService.generateSlittingSlices(reqVO));
    }

    @DeleteMapping("/slitting/slice/delete")
    @Operation(summary = "删除未确认的分切切片记录")
    public CommonResult<Boolean> deleteSlittingSlice(@RequestParam("id") Long id) {
        hcProcessReportService.deleteSlittingSlice(id);
        return success(true);
    }

    @PostMapping("/slitting/slice/mark-printed")
    @Operation(summary = "标记分切切片已打印")
    public CommonResult<Boolean> markSlittingSlicesPrinted(@Valid @RequestBody HcSlittingSlicePrintReqVO reqVO) {
        hcProcessReportService.markSlittingSlicesPrinted(reqVO);
        return success(true);
    }

    @PostMapping("/slitting/slice/update-content")
    @Operation(summary = "修改分切切片目视自检内容")
    public CommonResult<HcSlittingSliceRespVO> updateSlittingSliceContent(@Valid @RequestBody HcSlittingSliceConfirmReqVO reqVO) {
        return success(hcProcessReportService.updateSlittingSliceContent(reqVO));
    }

    @PostMapping("/slitting/slice/confirm")
    @Operation(summary = "扫码确认分切切片")
    public CommonResult<HcSlittingSliceRespVO> confirmSlittingSlice(@Valid @RequestBody HcSlittingSliceConfirmReqVO reqVO) {
        return success(hcProcessReportService.confirmSlittingSlice(reqVO));
    }

    @PostMapping("/slitting/slice/abnormal-category/correct")
    @Operation(summary = "修正分切切片外观异常类别")
    public CommonResult<HcSlittingSliceRespVO> correctSlittingSliceAbnormalCategory(
            @Valid @RequestBody HcVisualAbnormalCategoryCorrectReqVO reqVO) {
        return success(hcProcessReportService.correctSlittingSliceAbnormalCategory(reqVO));
    }

    @GetMapping("/pass-work/match-preview")
    @Operation(summary = "预览已保存配料模板的型号匹配结果")
    public CommonResult<cn.iocoder.yudao.module.mes.service.hc.processreport.HcFormulaStationRuntimeService.MatchPreview> previewFormulaStationMatch(@RequestParam("modelCode") String modelCode) {
        return success(hcProcessReportService.previewFormulaStationMatch(modelCode));
    }

    @GetMapping("/pass-work/list")
    @Operation(summary = "获得配料过站工作列表")
    public CommonResult<List<HcFormulaPassWorkRespVO>> getFormulaPassWorkList(@RequestParam("planId") Long planId,
                                                                              @RequestParam("planOperationId") Long planOperationId) {
        return success(hcProcessReportService.getFormulaPassWorkList(planId, planOperationId));
    }

    @PostMapping("/pass-work/save")
    @Operation(summary = "保存配料过站工作")
    public CommonResult<Long> saveFormulaPassWork(@Valid @RequestBody HcFormulaPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.saveFormulaPassWork(reqVO));
    }

    @PostMapping("/pass-work/confirm")
    @Operation(summary = "确认配料过站工作")
    public CommonResult<Long> confirmFormulaPassWork(@Valid @RequestBody HcFormulaPassWorkSaveReqVO reqVO) {
        return success(hcProcessReportService.confirmFormulaPassWork(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交配料报工")
    public CommonResult<Long> submitFormulaReport(@Valid @RequestBody HcFormulaReportSaveReqVO reqVO) {
        return success(hcProcessReportService.submitFormulaReport(reqVO));
    }

    @PostMapping("/report/confirm")
    @Operation(summary = "确认配料报工记录")
    public CommonResult<Long> confirmFormulaReport(@Valid @RequestBody HcOperationReportConfirmReqVO reqVO) {
        return success(hcProcessReportService.confirmFormulaReport(reqVO));
    }
}
