package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2CoaWithdrawReqVO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTimeLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTimeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaPassWorkSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFormulaReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2TailAssignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2TailBatchAssignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2TailSelectedAssignReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcOperationReportConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2AbnormalCategoryCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2FaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2FaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2RuntimeProductRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveFaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveFaiRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveCheckItemRespVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableCleanReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableMaterialRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotAbnormalLockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotFormRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotFormRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotIntermediateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotIntermediateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotMiddleLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotSegmentCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotProcessParamRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotProcessParamSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPlanChangeoverLogRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPlanChangeoverLogSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotScanGateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotFaiWithdrawReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveIntermediateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveIntermediateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2IntermediateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2IntermediateSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveAqcTaskFeedbackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveAqcTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveAqcTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardLossReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardStockSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsagePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsageRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsageSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSlicePrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSourceCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSourceRespVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcVisualAbnormalCategoryCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportStartReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportSwitchEquipmentReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportTaskPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughReportTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcStatisticsDataReviseReqVO;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcProcessReportService {

    List<HcFormulaReportTaskRespVO> getFormulaTaskList(HcFormulaReportTaskPageReqVO reqVO);

    Long startFormulaReport(HcFormulaReportStartReqVO reqVO);

    void switchFormulaEquipment(HcFormulaReportSwitchEquipmentReqVO reqVO);

    Long updateFormulaReportTime(HcFormulaReportTimeUpdateReqVO reqVO);

    Long reviseFormulaReportStatisticsData(HcStatisticsDataReviseReqVO reqVO);

    List<HcFormulaReportTimeLogRespVO> getFormulaReportTimeLogs(Long operationReportId);

    Long confirmFormulaReport(HcOperationReportConfirmReqVO reqVO);

    List<HcWetReportTaskRespVO> getWetTaskList(HcWetReportTaskPageReqVO reqVO);

    HcWetFaiRespVO getWetFaiSummary(Long planId, Long planOperationId);

    HcWetFaiRespVO applyWetFai(HcWetFaiApplyReqVO reqVO);

    HcWetWaterChangeApplyRespVO getActiveWetWaterChangeApply();

    HcWetWaterChangeApplyRespVO saveWetWaterChangeApply(HcWetWaterChangeApplySaveReqVO reqVO);

    List<HcWetPassWorkRespVO> getWetPassWorkList(Long planId, Long planOperationId);

    Long startWetReport(HcWetReportStartReqVO reqVO);

    void switchWetEquipment(HcWetReportSwitchEquipmentReqVO reqVO);

    Long updateWetReportTime(HcFormulaReportTimeUpdateReqVO reqVO);

    List<HcFormulaReportTimeLogRespVO> getWetReportTimeLogs(Long operationReportId);

    Long confirmWetReport(HcOperationReportConfirmReqVO reqVO);

    Long saveWetPassWork(HcWetPassWorkSaveReqVO reqVO);

    Long confirmWetPassWork(HcWetPassWorkSaveReqVO reqVO);

    List<HcWetReportAbnormalPositionRespVO> getWetReportAbnormalPositions(Long operationReportId, Long planOperationId);

    Long submitWetReport(HcWetReportSaveReqVO reqVO);

    Long reviseWetReportQuantity(HcWetReportQuantityReviseReqVO reqVO);

    Long reviseWetReportStatisticsData(HcStatisticsDataReviseReqVO reqVO);

    List<HcRoughReportTaskRespVO> getRoughTaskList(HcRoughReportTaskPageReqVO reqVO);

    Long startRoughReport(HcRoughReportStartReqVO reqVO);

    void switchRoughEquipment(HcRoughReportSwitchEquipmentReqVO reqVO);

    Long saveRoughProgress(HcRoughReportSaveReqVO reqVO);

    Long submitRoughReport(HcRoughReportSaveReqVO reqVO);

    List<HcAdhesiveReportTaskRespVO> getAdhesiveTaskList(HcAdhesiveReportTaskPageReqVO reqVO);

    HcAdhesiveFaiRespVO getAdhesiveFaiSummary(Long planId, Long planOperationId, Long sourceGrindingSecondDetailId);

    HcAdhesiveFaiRespVO applyAdhesiveFai(HcAdhesiveFaiApplyReqVO reqVO);

    Long startAdhesiveReport(HcAdhesiveReportStartReqVO reqVO);

    void switchAdhesiveEquipment(HcAdhesiveReportSwitchEquipmentReqVO reqVO);

    List<HcWetPassWorkRespVO> getAdhesivePassWorkList(Long planId, Long planOperationId, Long equipmentId, LocalDate recordDate);

    Long saveAdhesivePassWork(HcWetPassWorkSaveReqVO reqVO);

    Long confirmAdhesivePassWork(HcWetPassWorkSaveReqVO reqVO);

    List<HcAdhesiveCheckItemRespVO> getAdhesiveCheckTemplate();

    HcAdhesiveSourceRespVO scanAdhesiveSource(String batchNo, Long planOperationId,
                                              String sourceMode, String sourcePlanNo, String sourceMotherBatchNo);

    List<HcAdhesiveSourceRespVO> getAdhesiveSourceList(Long planId, Long planOperationId);

    Long stampAdhesiveSegmentTiming(HcAdhesiveSegmentTimingStampReqVO reqVO);

    List<HcAdhesiveReportRespVO> getAdhesiveReportList(Long planOperationId);

    Long saveAdhesiveReport(HcAdhesiveReportSaveTextReqVO reqVO);

    Long saveAdhesiveReport(HcAdhesiveReportSaveReqVO reqVO);

    Long updateAdhesiveReportTime(HcAdhesiveReportTimeUpdateReqVO reqVO);

    Long reviseAdhesiveReportStatisticsData(HcStatisticsDataReviseReqVO reqVO);

    Long confirmAdhesiveReport(HcAdhesiveReportConfirmReqVO reqVO);

    void markAdhesiveReportPrinted(HcAdhesiveReportPrintReqVO reqVO);

    void removeAdhesiveReport(Long id);

    Long submitAdhesiveReport(HcAdhesiveReportSubmitReqVO reqVO);

    Long completeAdhesiveSegment(HcAdhesiveSegmentCompleteReqVO reqVO);

    HcAdhesiveIntermediateRespVO getAdhesiveIntermediate(Long planId, Long planOperationId, Long adhesiveReportId, String batchNo);

    HcAdhesiveIntermediateRespVO getAdhesiveIntermediateById(Long recordId);

    PageResult<HcStationRecordRespVO> getAdhesiveIntermediateRecordPage(HcStationRecordPageReqVO reqVO);

    Long saveAdhesiveIntermediate(HcAdhesiveIntermediateSaveReqVO reqVO);

    Long confirmAdhesiveIntermediate(HcAdhesiveIntermediateSaveReqVO reqVO);

    Integer importAdhesiveIntermediate(Long recordId, MultipartFile file) throws Exception;

    void exportAdhesiveIntermediate(Long recordId, HttpServletResponse response) throws Exception;

    PageResult<HcAdhesiveGlueBoardStockRespVO> getAdhesiveGlueBoardStockPage(HcAdhesiveGlueBoardStockPageReqVO reqVO);

    PageResult<HcAdhesiveGlueBoardStockRespVO> getAdhesive2GlueBoardStockPage(HcAdhesiveGlueBoardStockPageReqVO reqVO);

    List<HcAdhesiveGlueBoardStockImportExcelVO> buildAdhesiveGlueBoardStockExportList(HcAdhesiveGlueBoardStockPageReqVO reqVO);

    HcAdhesiveGlueBoardStockImportRespVO importAdhesiveGlueBoardStockExcel(MultipartFile file, Boolean confirmClear) throws IOException;

    HcAdhesiveGlueBoardStockRespVO getAdhesiveGlueBoardStockByBatch(String glueBoardBatchNo, String glueBoardModel);

    HcAdhesiveGlueBoardStockRespVO getAdhesive2GlueBoardStockByBatch(String glueBoardBatchNo, String glueBoardModel);

    Long createAdhesiveGlueBoardStock(HcAdhesiveGlueBoardStockSaveReqVO reqVO);

    HcAdhesiveGlueBoardStockRespVO markAdhesiveGlueBoardStockPrinted(Long id);

    List<HcAdhesiveGlueBoardUsageRespVO> getAdhesiveGlueBoardUsageList(Long planOperationId);

    PageResult<HcAdhesiveGlueBoardUsageRespVO> getAdhesiveGlueBoardUsagePage(HcAdhesiveGlueBoardUsagePageReqVO reqVO);

    HcAdhesiveGlueBoardUsageRespVO getAdhesiveCurrentGlueBoardUsage(Long planOperationId, String glueBoardModel,
                                                                     String processCode);

    HcAdhesiveFaiRespVO getLatestAdhesiveGlueBoardFai(Long planOperationId, String sourceProductionBatchNo,
                                                       Long glueBoardStockId, String glueBoardBatchNo,
                                                       String glueBoardModel, String processCode);

    Long saveAdhesiveGlueBoardUsage(HcAdhesiveGlueBoardUsageSaveReqVO reqVO);

    Long saveAdhesive2GlueBoardUsage(HcAdhesiveGlueBoardUsageSaveReqVO reqVO);

    HcAdhesiveGlueBoardUsageRespVO reportAdhesiveGlueBoardLoss(HcAdhesiveGlueBoardLossReqVO reqVO);

    HcAdhesiveGlueBoardUsageRespVO returnAdhesiveGlueBoardUsage(Long id);

    HcAdhesiveAqcTaskRespVO submitAdhesiveAqcTask(HcAdhesiveAqcTaskSaveReqVO reqVO);

    HcAdhesiveAqcTaskRespVO getLatestAdhesiveReportAqcTask(Long planOperationId, String batchNo);

    HcAdhesiveAqcTaskRespVO feedbackAdhesiveAqcTask(HcAdhesiveAqcTaskFeedbackReqVO reqVO);

    void deductAdhesiveGlueBoardInspectionSample(Long glueBoardUsageId, Long glueBoardStockId,
                                                  String glueBoardBatchNo, BigDecimal sampleStartPosition,
                                                  BigDecimal sampleLength, LocalDateTime inspectionSubmitTime);

    void markAdhesiveReportsAbnormalByGlueBoardInspection(Long glueBoardStockId,
                                                           LocalDateTime inspectionSubmitTime, String reason);

    void releaseAdhesiveReportsAbnormalByGlueBoardInspection(Long glueBoardStockId);

    List<HcSlittingReportTaskRespVO> getSlittingTaskList(String taskStatus, String taskKeyword);

    List<HcSlittingSourceRespVO> getSlittingSourceList(Long planId, Long planOperationId, String productionBatchNo);

    HcSlittingSourceRespVO completeSlittingSource(HcSlittingSourceCompleteReqVO reqVO);

    List<HcSlittingSliceRespVO> getSlittingSliceList(Long planOperationId, LocalDate scanConfirmDate);

    List<HcSlittingSliceRespVO> generateSlittingSlices(HcSlittingSliceGenerateReqVO reqVO);

    void deleteSlittingSlice(Long id);

    void markSlittingSlicesPrinted(HcSlittingSlicePrintReqVO reqVO);

    HcSlittingSliceRespVO updateSlittingSliceContent(HcSlittingSliceConfirmReqVO reqVO);

    HcSlittingSliceRespVO confirmSlittingSlice(HcSlittingSliceConfirmReqVO reqVO);

    HcSlittingSliceRespVO correctSlittingSliceAbnormalCategory(HcVisualAbnormalCategoryCorrectReqVO reqVO);

    List<HcAdhesiveReportTaskRespVO> getPressSlotTaskList(HcAdhesiveReportTaskPageReqVO reqVO);

    HcWetFaiRespVO getPressSlotFaiSummary(Long planId, Long planOperationId, String motherBatchNo);

    List<HcWetFaiRespVO> getPressSlotFaiList(Long planId, Long planOperationId, String motherBatchNo);

    List<HcWetFaiRespVO> getPressSlotProcessCheckFaiList(Long planId, Long planOperationId, String motherBatchNo);

    HcPressSlotAbnormalLockRespVO getPressSlotActiveAbnormalLock(Long planId, Long planOperationId, String motherBatchNo);

    List<HcPressSlotAbnormalLockRespVO> getPressSlotAbnormalLockList(Long planId, Long planOperationId, String motherBatchNo);

    HcWetFaiRespVO applyPressSlotFai(HcWetFaiApplyReqVO reqVO);

    void withdrawPressSlotFai(HcPressSlotFaiWithdrawReqVO reqVO);

    HcPressSlotScanGateRespVO validatePressSlotFaiScan(Long planId, Long planOperationId, String motherBatchNo,
                                                       String productionBatchNo, String reportType);

    Long startPressSlotReport(HcAdhesiveReportStartReqVO reqVO);

    void switchPressSlotEquipment(HcAdhesiveReportSwitchEquipmentReqVO reqVO);

    List<HcWetPassWorkRespVO> getPressSlotPassWorkList(Long planId, Long planOperationId, Long equipmentId, LocalDate recordDate);

    Long savePressSlotPassWork(HcWetPassWorkSaveReqVO reqVO);

    Long confirmPressSlotPassWork(HcWetPassWorkSaveReqVO reqVO);

    List<HcAdhesiveCheckItemRespVO> getPressSlotCheckTemplate(String modelCode);

    cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO getPressSlotProductionCheckTemplate(String modelCode);

    HcAdhesiveSourceRespVO scanPressSlotSource(String batchNo, Boolean includeAbnormal);

    List<HcAdhesiveSourceRespVO> getPressSlotSourceList(Long planId, Long planOperationId);

    List<HcAdhesiveReportRespVO> getPressSlotReportList(Long planOperationId);

    List<HcAdhesiveReportRespVO> getPressSlotReportList(Long planOperationId, LocalDate scanConfirmDate);

    List<HcPressSlotConsumableRespVO> getPressSlotConsumableStatus(Long planOperationId, Long equipmentId);

    List<HcPressSlotConsumableMaterialRespVO> getPressSlotConsumableMaterialOptions(String consumableType, String keyword, Integer pageSize);

    Long replacePressSlotConsumable(HcPressSlotConsumableReplaceReqVO reqVO);

    Long cleanPressSlotConsumable(HcPressSlotConsumableCleanReqVO reqVO);

    Long savePressSlotReport(HcAdhesiveReportSaveReqVO reqVO);

    Long saveAndConfirmPressSlotReport(HcAdhesiveReportSaveConfirmReqVO reqVO);

    Long confirmPressSlotReport(HcAdhesiveReportConfirmReqVO reqVO);

    void setPressSlotReportMiddleType(Long reportId, String reportType);

    HcAdhesiveReportRespVO correctPressSlotReportAbnormalCategory(HcVisualAbnormalCategoryCorrectReqVO reqVO);

    void markPressSlotReportPrinted(HcAdhesiveReportPrintReqVO reqVO);

    HcPressSlotIntermediateRespVO getPressSlotIntermediate(Long planId, Long planOperationId,
                                                           java.time.LocalDate recordDate, Long id, String batchNo);

    Long savePressSlotIntermediate(HcPressSlotIntermediateSaveReqVO reqVO);

    Long confirmPressSlotIntermediate(HcPressSlotIntermediateSaveReqVO reqVO);

    List<HcPressSlotIntermediateRespVO> getPressSlotIntermediateList(Long planOperationId, String batchNo);

    List<HcPressSlotMiddleLedgerRespVO> getPressSlotMiddleLedgerList(Long planOperationId);

    Integer importPressSlotMiddleLedger(Long planId, Long planOperationId, MultipartFile file,
                                        String importAttachmentJson, Long recordId, String batchNo,
                                        java.time.LocalDate recordDate) throws Exception;

    void exportPressSlotMiddleLedger(Long planOperationId, Long recordId, String batchNo,
                                     HttpServletResponse response) throws Exception;

    List<HcPressSlotChangeoverInspectionRespVO> getPressSlotChangeoverInspectionList(Long planOperationId, String motherSegmentBatchNo);

    HcPressSlotChangeoverInspectionRespVO getLatestPressSlotChangeoverInspection(Long planOperationId);

    Long savePressSlotChangeoverInspection(HcPressSlotChangeoverInspectionSaveReqVO reqVO);

    List<HcPressSlotProcessParamRespVO> getPressSlotProcessParamList(Long planOperationId, LocalDate reportDate,
                                                                     String productionBatchNo, String motherBatchNo,
                                                                     String formType);

    PageResult<HcPressSlotFormRecordRespVO> getPressSlotFormRecordPage(HcPressSlotFormRecordPageReqVO reqVO);

    Long savePressSlotProcessParam(HcPressSlotProcessParamSaveReqVO reqVO);

    Integer importPressSlotProcessParams(Long planId, Long planOperationId, MultipartFile file,
                                         String importAttachmentJson, Long recordId, String motherBatchNo,
                                         String productionBatchNo, String formType, String inspectionScene,
                                         LocalDate recordDate) throws Exception;

    void exportPressSlotProcessParams(Long planOperationId, Long recordId, String motherBatchNo, String formType,
                                      HttpServletResponse response) throws Exception;

    Long confirmPressSlotProcessParam(HcPressSlotProcessParamSaveReqVO reqVO);

    Long submitPressSlotReport(HcAdhesiveReportSubmitReqVO reqVO);

    Long completePressSlotSegment(HcPressSlotSegmentCompleteReqVO reqVO);

    List<HcAdhesiveReportTaskRespVO> getAdhesive2TaskList(HcAdhesiveReportTaskPageReqVO reqVO);

    HcAdhesive2FaiRespVO getAdhesive2FaiSummary(Long planId, Long planOperationId);

    void withdrawAdhesive2Coa(HcAdhesive2CoaWithdrawReqVO reqVO);

    List<HcAdhesive2FaiRespVO> getAdhesive2CoaFaiList(Long planId, Long planOperationId, String motherBatchNo);

    List<HcAdhesive2FaiRespVO> getAdhesive2ProcessCheckFaiList(Long planId, Long planOperationId, String motherBatchNo);

    List<HcWetFaiRespVO> getAdhesive2SourcePressSlotFaiList(Long planId, Long planOperationId,
                                                            Long sourcePlanId, Long sourcePlanOperationId,
                                                            String motherBatchNo);

    List<HcWetFaiRespVO> getAdhesive2SourcePressSlotProcessCheckFaiList(Long planId, Long planOperationId,
                                                                        Long sourcePlanId, Long sourcePlanOperationId,
                                                                        String motherBatchNo);

    HcAdhesive2FaiRespVO applyAdhesive2Fai(HcAdhesive2FaiApplyReqVO reqVO);

    /**
     * 以粘胶2成品报工为来源提交 COA；未确认记录在同一事务内先扫码确认。
     *
     * @param reqVO 扫码确认信息
     * @return COA 送检摘要
     */
    HcAdhesive2FaiRespVO applyAdhesive2PostConfirmCoa(HcAdhesiveReportConfirmReqVO reqVO);

    HcAdhesive2RuntimeProductRespVO getAdhesive2RuntimeProductSnapshot(Long planId, Long planOperationId,
                                                                        String segmentBatchNo);

    Long startAdhesive2Report(HcAdhesiveReportStartReqVO reqVO);

    void switchAdhesive2Equipment(HcAdhesiveReportSwitchEquipmentReqVO reqVO);

    List<HcWetPassWorkRespVO> getAdhesive2PassWorkList(Long planId, Long planOperationId, Long equipmentId, LocalDate recordDate);

    Long saveAdhesive2PassWork(HcWetPassWorkSaveReqVO reqVO);

    Long confirmAdhesive2PassWork(HcWetPassWorkSaveReqVO reqVO);

    List<HcAdhesiveCheckItemRespVO> getAdhesive2CheckTemplate(String modelCode);

    HcAdhesiveSourceRespVO scanAdhesive2Source(String batchNo);

    List<HcAdhesiveSourceRespVO> getAdhesive2SourceList(Long planId, Long planOperationId, String sourceBatchNo);

    List<HcAdhesiveReportRespVO> getAdhesive2ReportList(Long planOperationId);

    List<HcAdhesiveReportRespVO> getAdhesive2ReportList(Long planOperationId, LocalDate scanConfirmDate);

    Long saveAdhesive2Report(HcAdhesiveReportSaveReqVO reqVO);

    List<Long> assignAdhesive2ReportTails(HcAdhesive2TailAssignReqVO reqVO);

    int assignAdhesive2TailForSelectedSources(HcAdhesive2TailSelectedAssignReqVO reqVO);

    int assignAdhesive2TailForAllSources(HcAdhesive2TailBatchAssignReqVO reqVO);

    Long saveAndConfirmAdhesive2Report(HcAdhesiveReportSaveConfirmReqVO reqVO);

    Long confirmAdhesive2Report(HcAdhesiveReportConfirmReqVO reqVO);

    void setAdhesive2ReportMiddleType(Long reportId, String reportType);

    HcAdhesiveReportRespVO correctAdhesive2ReportAbnormalCategory(HcAdhesive2AbnormalCategoryCorrectReqVO reqVO);

    void markAdhesive2ReportPrinted(HcAdhesiveReportPrintReqVO reqVO);

    Long submitAdhesive2Report(HcAdhesiveReportSubmitReqVO reqVO);

    Long completeAdhesive2Segment(HcAdhesiveSegmentCompleteReqVO reqVO);

    HcAdhesive2IntermediateRespVO getAdhesive2Intermediate(Long planId, Long planOperationId,
                                                           java.time.LocalDateTime recordDate, Long recordId, String batchNo);

    HcAdhesive2IntermediateRespVO getAdhesive2IntermediateById(Long recordId);

    PageResult<HcStationRecordRespVO> getAdhesive2IntermediateRecordPage(HcStationRecordPageReqVO reqVO);

    Long saveAdhesive2Intermediate(HcAdhesive2IntermediateSaveReqVO reqVO);

    List<HcAdhesive2IntermediateRespVO> getAdhesive2IntermediateList(Long planOperationId, String batchNo);

    List<HcPressSlotMiddleLedgerRespVO> getAdhesive2MiddleLedgerList(Long planOperationId);

    Integer importAdhesive2MiddleLedger(Long planId, Long planOperationId, MultipartFile file,
                                        String importAttachmentJson, Long recordId, String batchNo,
                                        java.time.LocalDateTime recordDate) throws Exception;

    void exportAdhesive2MiddleLedger(Long planOperationId, Long recordId, String batchNo,
                                     HttpServletResponse response) throws Exception;

    List<HcPressSlotProcessParamRespVO> getAdhesive2ProcessParamList(Long planOperationId, LocalDate reportDate,
                                                                     String productionBatchNo, String motherBatchNo,
                                                                     String formType);

    Integer importAdhesive2ProcessParams(Long planId, Long planOperationId, MultipartFile file,
                                         String importAttachmentJson, Long recordId, String motherBatchNo,
                                         String productionBatchNo, String formType, String inspectionScene,
                                         LocalDate recordDate, Long changeoverInstructionId) throws Exception;

    void exportAdhesive2ProcessParams(Long planOperationId, Long recordId, String motherBatchNo, String formType,
                                      HttpServletResponse response) throws Exception;

    Long saveAdhesive2ProcessParam(HcPressSlotProcessParamSaveReqVO reqVO);

    Long confirmAdhesive2ProcessParam(HcPressSlotProcessParamSaveReqVO reqVO);

    void deleteAdhesive2ProcessParam(Long planOperationId, Long recordId);

    List<HcPressSlotChangeoverInspectionRespVO> getAdhesive2ChangeoverInspectionList(Long planOperationId, String motherSegmentBatchNo);

    HcPressSlotChangeoverInspectionRespVO getLatestAdhesive2ChangeoverInspection(Long planOperationId);

    Long saveAdhesive2ChangeoverInspection(HcPressSlotChangeoverInspectionSaveReqVO reqVO);

    HcPlanChangeoverLogRespVO getLatestAdhesive2PlanChangeover(Long planOperationId);

    List<HcPlanChangeoverLogRespVO> getAdhesive2PlanChangeoverList(Long planOperationId);

    HcPlanChangeoverLogRespVO saveAdhesive2PlanChangeover(HcPlanChangeoverLogSaveReqVO reqVO);

    HcFormulaStationRuntimeService.MatchPreview previewFormulaStationMatch(String modelCode);

    List<HcFormulaPassWorkRespVO> getFormulaPassWorkList(Long planId, Long planOperationId);

    Long saveFormulaPassWork(HcFormulaPassWorkSaveReqVO reqVO);

    Long confirmFormulaPassWork(HcFormulaPassWorkSaveReqVO reqVO);

    Long submitFormulaReport(HcFormulaReportSaveReqVO reqVO);
}
