package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.equipment.vo.HcEquipmentSelectOptionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleBoardReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleBoardRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleAllocationModeSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcRoughConsoleDailyCheckSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingProductionRecordRespVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationrecord.vo.HcStationRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetReportAbnormalPositionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkRespVO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import java.time.LocalDate;
import java.util.List;

public interface HcRoughGrindingConsoleService {

    HcRoughConsoleBoardRespVO getBoard(HcRoughConsoleBoardReqVO reqVO);

    PageResult<HcGrindingProductionRecordRespVO> getProductionRecordPage(HcGrindingProductionRecordPageReqVO reqVO);

    List<HcGrindingProductionRecordRespVO> getProductionRecordList(HcGrindingProductionRecordPageReqVO reqVO);

    HcRoughFaiRespVO getFaiSummary(Long planId, Long planOperationId);

    HcRoughFaiRespVO applyFai(HcRoughFaiApplyReqVO reqVO);

    HcRoughSecondSegmentInspectionRespVO getSecondSegmentInspectionSummary(Long secondDetailId);

    HcRoughSecondSegmentInspectionRespVO applySecondSegmentInspection(HcRoughSecondSegmentInspectionApplyReqVO reqVO);

    List<HcWetReportAbnormalPositionRespVO> getAbnormalPositionsByMotherBatchNo(String motherBatchNo);

    List<HcEquipmentSelectOptionRespVO> getEquipmentOptions(Long planOperationId, Long workCenterId);

    List<HcWetPassWorkRespVO> getDailyCheckList(Long equipmentId, LocalDate recordDate);

    Long saveDailyCheck(HcRoughConsoleDailyCheckSaveReqVO reqVO);

    Long confirmDailyCheck(HcRoughConsoleDailyCheckSaveReqVO reqVO);

    Long replaceConsumable(HcRoughConsoleConsumableReplaceReqVO reqVO);

    HcRoughConsoleSourceBalanceRespVO scanSource(String batchNo);

    Long saveFirstReport(HcRoughConsoleFirstReportSaveReqVO reqVO);

    Long saveFirstAllocationMode(HcRoughConsoleAllocationModeSaveReqVO reqVO);

    Long saveFirstAllocation(HcRoughConsoleFirstAllocationSaveReqVO reqVO);

    Long reviseFirstAllocationQuantity(HcRoughConsoleFirstAllocationQuantityReviseReqVO reqVO);

    Long reviseStatisticsData(HcStatisticsDataReviseReqVO reqVO);

    void deleteFirstAllocation(Long id);

    Long saveSecondReport(HcRoughConsoleSecondReportSaveReqVO reqVO);

    Long stampSegmentTiming(HcRoughConsoleSegmentTimingStampReqVO reqVO);

    Long stampOriginalMotherBatchTime(HcRoughConsoleOriginalMotherTimeStampReqVO reqVO);

    Long updateReportTime(HcRoughConsoleReportTimeUpdateReqVO reqVO);

    void deleteFirstReport(Long id);

    void deleteSecondReport(Long id);

    Long confirmSecondReport(HcRoughConsoleSecondReportConfirmReqVO reqVO);

    Long markSecondReportPrinted(HcRoughConsoleSecondReportPrintReqVO reqVO);

    HcRoughConsoleMiddleProductRecordRespVO getMiddleProductRecord(Long recordId);

    PageResult<HcStationRecordRespVO> getMiddleProductRecordPage(HcStationRecordPageReqVO reqVO);

    HcRoughConsoleMiddleProductRecordRespVO getMiddleProductRecordByStationRecord(Long stationRecordId);

    HcRoughConsoleMiddleProductRecordRespVO getOrInitSegmentMiddleProductRecord(HcRoughConsoleMiddleProductSegmentReqVO reqVO);

    Long saveMiddleProductRecord(HcRoughConsoleMiddleProductRecordSaveReqVO reqVO);

    Long confirmMiddleProductRecord(HcRoughConsoleMiddleProductRecordSaveReqVO reqVO);

    Long startWorkOrder(HcRoughReportStartReqVO reqVO);

    void switchWorkOrderEquipment(HcRoughReportSwitchEquipmentReqVO reqVO);

    Long completeWorkOrder(HcRoughReportSaveReqVO reqVO);
}
