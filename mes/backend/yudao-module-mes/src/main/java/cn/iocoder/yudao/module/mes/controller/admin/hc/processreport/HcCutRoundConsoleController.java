package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveCheckItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSaveConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveSegmentCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportSubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveSourceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundInspectionTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundInspectionTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcVisualAbnormalCategoryCorrectReqVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcCutRoundConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 裁切操作看板")
@RestController
@RequestMapping("/mes/hc/execution/formula-report/cut-round")
@Validated
public class HcCutRoundConsoleController {

    @Resource
    private HcCutRoundConsoleService hcCutRoundConsoleService;

    @GetMapping("/task-list")
    @Operation(summary = "获得裁切操作看板任务列表")
    public CommonResult<List<HcAdhesiveReportTaskRespVO>> getTaskList(@Valid HcAdhesiveReportTaskPageReqVO reqVO) {
        return success(hcCutRoundConsoleService.getTaskList(reqVO));
    }

    @PostMapping("/start")
    @Operation(summary = "执行裁切开工确认")
    public CommonResult<Long> start(@Valid @RequestBody HcAdhesiveReportStartReqVO reqVO) {
        return success(hcCutRoundConsoleService.start(reqVO));
    }

    @PostMapping("/switch-equipment")
    @Operation(summary = "切换裁切工位设备")
    public CommonResult<Boolean> switchEquipment(@Valid @RequestBody HcAdhesiveReportSwitchEquipmentReqVO reqVO) {
        hcCutRoundConsoleService.switchEquipment(reqVO);
        return success(true);
    }

    @GetMapping("/pass-work/list")
    @Operation(summary = "获得裁切开机前准备列表")
    public CommonResult<List<HcWetPassWorkRespVO>> getPassWorkList(@RequestParam("planId") Long planId,
                                                                   @RequestParam("planOperationId") Long planOperationId,
                                                                   @RequestParam(value = "equipmentId", required = false) Long equipmentId,
                                                                   @RequestParam(value = "recordDate", required = false)
                                                                   @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcCutRoundConsoleService.getPassWorkList(planId, planOperationId, equipmentId, recordDate));
    }

    @PostMapping("/pass-work/save")
    @Operation(summary = "保存裁切开机前准备")
    public CommonResult<Long> savePassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcCutRoundConsoleService.savePassWork(reqVO));
    }

    @PostMapping("/pass-work/confirm")
    @Operation(summary = "确认裁切开机前准备")
    public CommonResult<Long> confirmPassWork(@Valid @RequestBody HcWetPassWorkSaveReqVO reqVO) {
        return success(hcCutRoundConsoleService.confirmPassWork(reqVO));
    }

    @GetMapping("/check-template")
    @Operation(summary = "获得裁切首检/点检模板")
    public CommonResult<List<HcAdhesiveCheckItemRespVO>> getCheckTemplate(
            @RequestParam(value = "modelCode", required = false) String modelCode) {
        return success(hcCutRoundConsoleService.getCheckTemplate(modelCode));
    }

    @GetMapping("/source/scan")
    @Operation(summary = "扫码查询裁切来源粘胶2片号")
    public CommonResult<HcAdhesiveSourceRespVO> scanSource(@RequestParam("batchNo") String batchNo) {
        return success(hcCutRoundConsoleService.scanSource(batchNo));
    }

    @GetMapping("/source/list")
    @Operation(summary = "获得裁切可加工来源")
    public CommonResult<List<HcAdhesiveSourceRespVO>> getSourceList(@RequestParam("planId") Long planId,
                                                                    @RequestParam("planOperationId") Long planOperationId,
                                                                    @RequestParam(value = "sourceBatchNo", required = false) String sourceBatchNo) {
        return success(hcCutRoundConsoleService.getSourceList(planId, planOperationId, sourceBatchNo));
    }

    @GetMapping("/report/list")
    @Operation(summary = "获得裁切报工记录")
    public CommonResult<List<HcAdhesiveReportRespVO>> getReportList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "scanConfirmDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate scanConfirmDate) {
        return success(hcCutRoundConsoleService.getReportList(planOperationId, scanConfirmDate));
    }

    @GetMapping("/report/get-by-batch")
    @Operation(summary = "按片号获得裁切报工记录")
    public CommonResult<HcAdhesiveReportRespVO> getReportByBatchNo(@RequestParam("batchNo") String batchNo,
                                                                   @RequestParam(value = "planNo", required = false) String planNo) {
        return success(hcCutRoundConsoleService.getReportByBatchNo(batchNo, planNo));
    }

    @GetMapping("/inspection-task/list")
    @Operation(summary = "获得裁切产品报检单列表")
    public CommonResult<List<HcCutRoundInspectionTaskRespVO>> getInspectionTaskList(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcCutRoundConsoleService.getInspectionTaskList(planOperationId));
    }

    @PostMapping("/inspection-task/create")
    @Operation(summary = "创建裁切产品报检单")
    public CommonResult<Long> createInspectionTask(@Valid @RequestBody HcCutRoundInspectionTaskSaveReqVO reqVO) {
        return success(hcCutRoundConsoleService.createInspectionTask(reqVO));
    }

    @PostMapping("/report/save")
    @Operation(summary = "保存裁切报工记录")
    public CommonResult<Long> saveReport(@Valid @RequestBody HcAdhesiveReportSaveReqVO reqVO) {
        return success(hcCutRoundConsoleService.saveReport(reqVO));
    }

    @PostMapping("/report/save-confirm")
    @Operation(summary = "保存并扫码确认裁切报工记录")
    public CommonResult<Long> saveAndConfirmReport(@Valid @RequestBody HcAdhesiveReportSaveConfirmReqVO reqVO) {
        return success(hcCutRoundConsoleService.saveAndConfirmReport(reqVO));
    }

    @PostMapping("/report/confirm")
    @Operation(summary = "扫码确认裁切报工记录")
    public CommonResult<Long> confirmReport(@Valid @RequestBody HcAdhesiveReportConfirmReqVO reqVO) {
        return success(hcCutRoundConsoleService.confirmReport(reqVO));
    }

    @PostMapping("/report/mark-printed")
    @Operation(summary = "标记裁切报工记录已打印")
    public CommonResult<Boolean> markReportPrinted(@Valid @RequestBody HcAdhesiveReportPrintReqVO reqVO) {
        hcCutRoundConsoleService.markReportPrinted(reqVO);
        return success(true);
    }

    @PostMapping("/report/abnormal-category/correct")
    @Operation(summary = "修正裁切报工外观异常类别")
    public CommonResult<HcAdhesiveReportRespVO> correctReportAbnormalCategory(
            @Valid @RequestBody HcVisualAbnormalCategoryCorrectReqVO reqVO) {
        return success(hcCutRoundConsoleService.correctReportAbnormalCategory(reqVO));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交裁切工单完工")
    public CommonResult<Long> submit(@Valid @RequestBody HcAdhesiveReportSubmitReqVO reqVO) {
        return success(hcCutRoundConsoleService.submit(reqVO));
    }

    @PostMapping("/segment/complete")
    @Operation(summary = "裁切当前分段完工")
    public CommonResult<Long> completeSegment(@Valid @RequestBody HcAdhesiveSegmentCompleteReqVO reqVO) {
        return success(hcCutRoundConsoleService.completeSegment(reqVO));
    }

    @GetMapping("/consumable/status")
    @Operation(summary = "获得裁切刀片/毛毡寿命状态")
    public CommonResult<List<HcPressSlotConsumableRespVO>> getConsumableStatus(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "equipmentId", required = false) Long equipmentId) {
        return success(hcCutRoundConsoleService.getConsumableStatus(planOperationId, equipmentId));
    }

    @GetMapping("/consumable/stock/get-by-batch")
    @Operation(summary = "按批号获得裁切耗材边库批次")
    public CommonResult<HcAdhesiveGlueBoardStockRespVO> getConsumableStockByBatch(
            @RequestParam("consumableType") String consumableType,
            @RequestParam("batchNo") String batchNo) {
        return success(hcCutRoundConsoleService.getConsumableStockByBatch(consumableType, batchNo));
    }

    @PostMapping("/consumable/replace")
    @Operation(summary = "更换裁切刀片/毛毡")
    public CommonResult<Long> replaceConsumable(@Valid @RequestBody HcPressSlotConsumableReplaceReqVO reqVO) {
        return success(hcCutRoundConsoleService.replaceConsumable(reqVO));
    }

    @GetMapping("/changeover/latest")
    @Operation(summary = "获得裁切最新工艺参数点检记录")
    public CommonResult<HcPressSlotChangeoverInspectionRespVO> getLatestChangeover(
            @RequestParam("planOperationId") Long planOperationId) {
        return success(hcCutRoundConsoleService.getLatestChangeover(planOperationId));
    }

    @GetMapping("/changeover/list")
    @Operation(summary = "获得裁切工艺参数点检记录列表")
    public CommonResult<List<HcPressSlotChangeoverInspectionRespVO>> getChangeoverList(
            @RequestParam("planOperationId") Long planOperationId,
            @RequestParam(value = "motherSegmentBatchNo", required = false) String motherSegmentBatchNo) {
        return success(hcCutRoundConsoleService.getChangeoverList(planOperationId, motherSegmentBatchNo));
    }

    @PostMapping("/changeover/save")
    @Operation(summary = "保存裁切工艺参数点检记录")
    public CommonResult<Long> saveChangeover(@Valid @RequestBody HcPressSlotChangeoverInspectionSaveReqVO reqVO) {
        return success(hcCutRoundConsoleService.saveChangeover(reqVO));
    }

    @DeleteMapping("/changeover/delete")
    @Operation(summary = "删除裁切工艺参数点检记录")
    public CommonResult<Boolean> deleteChangeover(@RequestParam("recordId") Long recordId,
                                                  @RequestParam("planOperationId") Long planOperationId) {
        hcCutRoundConsoleService.deleteChangeover(planOperationId, recordId);
        return success(true);
    }
}
