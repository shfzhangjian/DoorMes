package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
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
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcVisualAbnormalCategoryCorrectReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundInspectionTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundInspectionTaskSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotChangeoverInspectionSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcWetPassWorkSaveReqVO;
import java.time.LocalDate;
import java.util.List;

public interface HcCutRoundConsoleService {

    List<HcAdhesiveReportTaskRespVO> getTaskList(HcAdhesiveReportTaskPageReqVO reqVO);

    Long start(HcAdhesiveReportStartReqVO reqVO);

    void switchEquipment(HcAdhesiveReportSwitchEquipmentReqVO reqVO);

    List<HcWetPassWorkRespVO> getPassWorkList(Long planId, Long planOperationId, Long equipmentId, LocalDate recordDate);

    Long savePassWork(HcWetPassWorkSaveReqVO reqVO);

    Long confirmPassWork(HcWetPassWorkSaveReqVO reqVO);

    List<HcAdhesiveCheckItemRespVO> getCheckTemplate(String modelCode);

    HcAdhesiveSourceRespVO scanSource(String batchNo);

    List<HcAdhesiveSourceRespVO> getSourceList(Long planId, Long planOperationId, String sourceBatchNo);

    List<HcAdhesiveReportRespVO> getReportList(Long planOperationId);

    List<HcAdhesiveReportRespVO> getReportList(Long planOperationId, LocalDate scanConfirmDate);

    HcAdhesiveReportRespVO getReportByBatchNo(String batchNo, String planNo);

    PageResult<HcCutRoundProductionRecordRespVO> getProductionRecordPage(HcCutRoundProductionRecordPageReqVO reqVO);

    List<HcCutRoundProductionRecordRespVO> getProductionRecordList(HcCutRoundProductionRecordPageReqVO reqVO);

    List<HcCutRoundInspectionTaskRespVO> getInspectionTaskList(Long planOperationId);

    Long createInspectionTask(HcCutRoundInspectionTaskSaveReqVO reqVO);

    Long saveReport(HcAdhesiveReportSaveReqVO reqVO);

    Long saveAndConfirmReport(HcAdhesiveReportSaveConfirmReqVO reqVO);

    Long confirmReport(HcAdhesiveReportConfirmReqVO reqVO);

    void markReportPrinted(HcAdhesiveReportPrintReqVO reqVO);

    HcAdhesiveReportRespVO correctReportAbnormalCategory(HcVisualAbnormalCategoryCorrectReqVO reqVO);

    Long submit(HcAdhesiveReportSubmitReqVO reqVO);

    Long completeSegment(HcAdhesiveSegmentCompleteReqVO reqVO);

    List<HcPressSlotConsumableRespVO> getConsumableStatus(Long planOperationId, Long equipmentId);

    HcAdhesiveGlueBoardStockRespVO getConsumableStockByBatch(String consumableType, String batchNo);

    Long replaceConsumable(HcPressSlotConsumableReplaceReqVO reqVO);

    HcPressSlotChangeoverInspectionRespVO getLatestChangeover(Long planOperationId);

    List<HcPressSlotChangeoverInspectionRespVO> getChangeoverList(Long planOperationId, String motherSegmentBatchNo);

    Long saveChangeover(HcPressSlotChangeoverInspectionSaveReqVO reqVO);

    void deleteChangeover(Long planOperationId, Long recordId);
}
