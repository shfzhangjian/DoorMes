package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleBoardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleBoardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleAllocationModeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleDailyCheckSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstAllocationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleFirstAllocationQuantityReviseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductRecordSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleMiddleProductSegmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleOriginalMotherTimeStampReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleReportTimeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSecondReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSegmentTimingStampReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleSourceBalanceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughFaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughSecondSegmentInspectionApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughSecondSegmentInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcStatisticsDataReviseReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportAbnormalPositionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.service.hc.processreport.HcRoughGrindingConsoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 磨皮操作看板")
@RestController
@RequestMapping("/mes/hc/execution/rough-grinding-console")
@Validated
public class HcRoughGrindingConsoleController {

    @Resource
    private HcRoughGrindingConsoleService hcRoughGrindingConsoleService;

    @GetMapping("/board")
    @Operation(summary = "获得磨皮操作看板")
    public CommonResult<HcRoughConsoleBoardRespVO> getBoard(@Valid HcRoughConsoleBoardReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.getBoard(reqVO));
    }

    @GetMapping("/fai/summary")
    @Operation(summary = "获得磨皮操作看板首检摘要")
    public CommonResult<HcRoughFaiRespVO> getFaiSummary(@RequestParam("planId") Long planId,
                                                        @RequestParam("planOperationId") Long planOperationId) {
        return success(hcRoughGrindingConsoleService.getFaiSummary(planId, planOperationId));
    }

    @PostMapping("/fai/apply")
    @Operation(summary = "提交磨皮操作看板首检申请")
    public CommonResult<HcRoughFaiRespVO> applyFai(@Valid @RequestBody HcRoughFaiApplyReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.applyFai(reqVO));
    }

    @GetMapping("/second-segment-inspection/summary")
    @Operation(summary = "获得磨皮操作看板二磨分段留样送检摘要")
    public CommonResult<HcRoughSecondSegmentInspectionRespVO> getSecondSegmentInspectionSummary(
            @RequestParam("secondDetailId") Long secondDetailId) {
        return success(hcRoughGrindingConsoleService.getSecondSegmentInspectionSummary(secondDetailId));
    }

    @PostMapping("/second-segment-inspection/apply")
    @Operation(summary = "提交磨皮操作看板二磨分段留样送检")
    public CommonResult<HcRoughSecondSegmentInspectionRespVO> applySecondSegmentInspection(
            @Valid @RequestBody HcRoughSecondSegmentInspectionApplyReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.applySecondSegmentInspection(reqVO));
    }

    @GetMapping("/abnormal-position/list")
    @Operation(summary = "按母料批号获得湿法和磨皮异常位置")
    public CommonResult<List<HcWetReportAbnormalPositionRespVO>> getAbnormalPositionList(
            @RequestParam("motherBatchNo") String motherBatchNo) {
        return success(hcRoughGrindingConsoleService.getAbnormalPositionsByMotherBatchNo(motherBatchNo));
    }

    @GetMapping("/equipment-options")
    @Operation(summary = "获得磨皮当前工序可选机台")
    public CommonResult<List<HcEquipmentSelectOptionRespVO>> getEquipmentOptions(
            @RequestParam(value = "planOperationId", required = false) Long planOperationId,
            @RequestParam(value = "workCenterId", required = false) Long workCenterId) {
        return success(hcRoughGrindingConsoleService.getEquipmentOptions(planOperationId, workCenterId));
    }

    @GetMapping("/daily-check/list")
    @Operation(summary = "获得磨皮设备每日点检清洁记录")
    public CommonResult<List<HcWetPassWorkRespVO>> getDailyCheckList(
            @RequestParam("equipmentId") Long equipmentId,
            @RequestParam(value = "recordDate", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate recordDate) {
        return success(hcRoughGrindingConsoleService.getDailyCheckList(equipmentId, recordDate));
    }

    @PostMapping("/daily-check/save")
    @Operation(summary = "保存磨皮设备每日点检清洁记录")
    public CommonResult<Long> saveDailyCheck(@Valid @RequestBody HcRoughConsoleDailyCheckSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.saveDailyCheck(reqVO));
    }

    @PostMapping("/start")
    @Operation(summary = "磨皮操作看板执行工单开工")
    public CommonResult<Long> startWorkOrder(@Valid @RequestBody HcRoughReportStartReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.startWorkOrder(reqVO));
    }

    @PostMapping("/switch-equipment")
    @Operation(summary = "磨皮操作看板切换工单设备")
    public CommonResult<Boolean> switchWorkOrderEquipment(@Valid @RequestBody HcRoughReportSwitchEquipmentReqVO reqVO) {
        hcRoughGrindingConsoleService.switchWorkOrderEquipment(reqVO);
        return success(true);
    }

    @PostMapping("/complete")
    @Operation(summary = "磨皮操作看板工单完工")
    public CommonResult<Long> completeWorkOrder(@Valid @RequestBody HcRoughReportSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.completeWorkOrder(reqVO));
    }

    @PostMapping("/daily-check/confirm")
    @Operation(summary = "确认磨皮设备每日点检清洁记录")
    public CommonResult<Long> confirmDailyCheck(@Valid @RequestBody HcRoughConsoleDailyCheckSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.confirmDailyCheck(reqVO));
    }

    @PostMapping("/consumable/replace")
    @Operation(summary = "磨皮看板耗材更换")
    public CommonResult<Long> replaceConsumable(@Valid @RequestBody HcRoughConsoleConsumableReplaceReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.replaceConsumable(reqVO));
    }

    @GetMapping("/source/scan")
    @Operation(summary = "扫码查询磨皮可加工来源")
    public CommonResult<HcRoughConsoleSourceBalanceRespVO> scanSource(@RequestParam("batchNo") String batchNo) {
        return success(hcRoughGrindingConsoleService.scanSource(batchNo));
    }

    @PostMapping("/first-report/save")
    @Operation(summary = "已停用：一次磨皮原有母批报工")
    public CommonResult<Long> saveFirstReport(@Valid @RequestBody HcRoughConsoleFirstReportSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.saveFirstReport(reqVO));
    }

    @PostMapping("/first-allocation/mode")
    @Operation(summary = "兼容入口：初始化一磨P/Q/R/S/不分段处理")
    public CommonResult<Long> saveFirstAllocationMode(
            @Valid @RequestBody HcRoughConsoleAllocationModeSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.saveFirstAllocationMode(reqVO));
    }

    @PostMapping("/first-allocation/save")
    @Operation(summary = "保存一磨前置分配加工单元")
    public CommonResult<Long> saveFirstAllocation(
            @Valid @RequestBody HcRoughConsoleFirstAllocationSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.saveFirstAllocation(reqVO));
    }

    @PostMapping("/first-allocation/quantity/revise")
    @Operation(summary = "修正一磨前置分配加工单元报工米数")
    public CommonResult<Long> reviseFirstAllocationQuantity(
            @Valid @RequestBody HcRoughConsoleFirstAllocationQuantityReviseReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.reviseFirstAllocationQuantity(reqVO));
    }

    @PostMapping("/statistics-data/revise")
    @Operation(summary = "修订磨皮母卷批次良品统计数据")
    public CommonResult<Long> reviseStatisticsData(@Valid @RequestBody HcStatisticsDataReviseReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.reviseStatisticsData(reqVO));
    }

    @PostMapping("/first-allocation/delete")
    @Operation(summary = "删除一磨前置分配加工单元")
    public CommonResult<Boolean> deleteFirstAllocation(@RequestParam("id") Long id) {
        hcRoughGrindingConsoleService.deleteFirstAllocation(id);
        return success(true);
    }

    @PostMapping("/second-report/save")
    @Operation(summary = "保存第二次磨皮看板报工")
    public CommonResult<Long> saveSecondReport(@Valid @RequestBody HcRoughConsoleSecondReportSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.saveSecondReport(reqVO));
    }

    @PostMapping("/segment-timing/stamp")
    @Operation(summary = "直接记录磨皮 P/Q/R/S 分段开工、完工时间")
    public CommonResult<Long> stampSegmentTiming(@Valid @RequestBody HcRoughConsoleSegmentTimingStampReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.stampSegmentTiming(reqVO));
    }

    @PostMapping("/original-mother/time/stamp")
    @Operation(summary = "已停用：原有母批一磨开工完工时间")
    public CommonResult<Long> stampOriginalMotherBatchTime(
            @Valid @RequestBody HcRoughConsoleOriginalMotherTimeStampReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.stampOriginalMotherBatchTime(reqVO));
    }

    @PostMapping("/report/time/update")
    @Operation(summary = "修改磨皮看板报工开始结束时间")
    public CommonResult<Long> updateReportTime(@Valid @RequestBody HcRoughConsoleReportTimeUpdateReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.updateReportTime(reqVO));
    }

    @PostMapping("/first-report/delete")
    @Operation(summary = "删除第一次磨皮看板报工")
    public CommonResult<Boolean> deleteFirstReport(@RequestParam("id") Long id) {
        hcRoughGrindingConsoleService.deleteFirstReport(id);
        return success(true);
    }

    @PostMapping("/second-report/delete")
    @Operation(summary = "删除第二次磨皮看板报工")
    public CommonResult<Boolean> deleteSecondReport(@RequestParam("id") Long id) {
        hcRoughGrindingConsoleService.deleteSecondReport(id);
        return success(true);
    }

    @PostMapping("/second-report/confirm")
    @Operation(summary = "扫码确认磨皮看板第二次磨皮记录")
    public CommonResult<Long> confirmSecondReport(@Valid @RequestBody HcRoughConsoleSecondReportConfirmReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.confirmSecondReport(reqVO));
    }

    @PostMapping("/second-report/mark-printed")
    @Operation(summary = "标记磨皮看板第二次磨皮记录已打印")
    public CommonResult<Long> markSecondReportPrinted(@Valid @RequestBody HcRoughConsoleSecondReportPrintReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.markSecondReportPrinted(reqVO));
    }

    @GetMapping("/middle-product-record/get")
    @Operation(summary = "获得磨皮报工中间品记录单")
    public CommonResult<HcRoughConsoleMiddleProductRecordRespVO> getMiddleProductRecord(@RequestParam("recordId") Long recordId) {
        return success(hcRoughGrindingConsoleService.getMiddleProductRecord(recordId));
    }

    @GetMapping("/middle-product-record/page")
    @Operation(summary = "分页获得磨皮中间品记录单业务记录")
    public CommonResult<PageResult<HcStationRecordRespVO>> getMiddleProductRecordPage(@Valid HcStationRecordPageReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.getMiddleProductRecordPage(reqVO));
    }

    @GetMapping("/middle-product-record/station")
    @Operation(summary = "按站点记录获得磨皮报工中间品记录单")
    public CommonResult<HcRoughConsoleMiddleProductRecordRespVO> getMiddleProductRecordByStationRecord(
            @RequestParam("stationRecordId") Long stationRecordId) {
        return success(hcRoughGrindingConsoleService.getMiddleProductRecordByStationRecord(stationRecordId));
    }

    @GetMapping("/middle-product-record/segment")
    @Operation(summary = "获得或初始化磨皮分段中间品记录单")
    public CommonResult<HcRoughConsoleMiddleProductRecordRespVO> getOrInitSegmentMiddleProductRecord(
            @Valid HcRoughConsoleMiddleProductSegmentReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.getOrInitSegmentMiddleProductRecord(reqVO));
    }

    @PostMapping("/middle-product-record/save")
    @Operation(summary = "保存磨皮报工中间品记录单")
    public CommonResult<Long> saveMiddleProductRecord(@Valid @RequestBody HcRoughConsoleMiddleProductRecordSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.saveMiddleProductRecord(reqVO));
    }

    @PostMapping("/middle-product-record/confirm")
    @Operation(summary = "确认磨皮报工中间品记录单")
    public CommonResult<Long> confirmMiddleProductRecord(@Valid @RequestBody HcRoughConsoleMiddleProductRecordSaveReqVO reqVO) {
        return success(hcRoughGrindingConsoleService.confirmMiddleProductRecord(reqVO));
    }
}
