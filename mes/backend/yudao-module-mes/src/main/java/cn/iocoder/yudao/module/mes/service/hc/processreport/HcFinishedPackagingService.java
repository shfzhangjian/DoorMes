package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.BoxActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.CancelInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ConfirmInboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ConfirmInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.DownShelfInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.DirectOutboundPendingPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationGridRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationTreeWarehouseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLayerSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgRackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgWarehouseQualityScopeUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgWarehouseQualityScopeUpdateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgWarehouseSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgShippingBatchCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgShippingBatchCandidateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLocationDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLocationOverviewRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageBatchActionRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageRemarkUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundPackageSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundTaskRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ImportedStockDataUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InitInboundBoxesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InitOutboundBoxesReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InitOutboundBoxesFromNoticeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSlicePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InspectionSliceSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.LockInboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundRepackReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundRepackReturnRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundStockBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundStockBatchRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.MockInspectionCompleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.OutboundBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.OutboundOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageBoxPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeSummaryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxStockPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxStockRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxStockSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackagingManualPieceCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackagingManualPieceDeleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackagingManualPieceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelBatchPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelBatchQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelBatchQueryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PieceLabelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ScanInboundPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ScanOutboundPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeDeliverySubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeExcelImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeIssueReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeOutboundConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePackageConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidateSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeReturnPickReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeChangeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeSaveLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcFinishedPackagingService {
    /** 同步指定 COA 段尚未出库片；保留其它质量限制。 */
    void syncCoaFreezeForSegment(String segmentBatchNo);
    void syncCoaFreezeForFai(Long faiId);


    List<PackageBoxRespVO> getPackageBoxList(String boxType, String keyword);

    InboundBoxRespVO getInboundBoxDetail(Long id);

    OutboundBoxRespVO getOutboundBoxDetail(Long id);

    Long printPackageBox(PackageBoxPrintReqVO reqVO);

    List<FgLocationGridRespVO> getFgLocationGrid();

    List<FgLocationTreeWarehouseRespVO> getFgLocationTree();

    Long saveFgWarehouse(FgWarehouseSaveReqVO reqVO);

    FgWarehouseQualityScopeUpdateRespVO updateFgWarehouseQualityScope(
            FgWarehouseQualityScopeUpdateReqVO reqVO);

    Boolean deleteFgWarehouse(Long id);

    Long saveFgRack(FgRackSaveReqVO reqVO);

    Boolean deleteFgRack(Long id);

    Long saveFgLayer(FgLayerSaveReqVO reqVO);

    Boolean deleteFgLayer(Long id);

    Long saveFgLocation(FgLocationSaveReqVO reqVO);

    Boolean deleteFgLocation(Long id);

    FgLocationGridRespVO printFgLocation(FgLocationPrintReqVO reqVO);

    List<FgStockLocationOverviewRespVO> getFgStockLocationOverview();

    FgStockLocationDetailRespVO getFgStockLocationDetail(String locationCode);

    PageResult<FgStockLedgerRespVO> getFgStockLedgerPage(FgStockLedgerPageReqVO reqVO);

    FgStockLedgerRespVO updateImportedStockData(ImportedStockDataUpdateReqVO reqVO);

    PageResult<FgStockHistoryLedgerRespVO> getFgStockHistoryLedgerPage(FgStockHistoryLedgerPageReqVO reqVO);

    PageResult<PackageAuxStockRespVO> getPackageAuxStockPage(PackageAuxStockPageReqVO reqVO);

    Long savePackageAuxStock(PackageAuxStockSaveReqVO reqVO);

    List<PackageAuxStockRespVO> getPackageAuxAvailableList();

    PackageAuxStockRespVO printPackageAuxStock(BoxActionReqVO reqVO);

    PackageAuxConsumeRecordRespVO consumePackageAux(PackageAuxConsumeReqVO reqVO);

    List<PackageAuxConsumeSummaryRespVO> getPackageAuxTodaySummary(LocalDate recordDate);

    List<PackageAuxConsumeRecordRespVO> getPackageAuxTodayList(LocalDate recordDate);

    List<PackageAuxConsumeRecordRespVO> getPackageAuxBizList(String bizType, String bizNo);

    PageResult<InspectionSliceRespVO> getMockInspectionPage(InspectionSlicePageReqVO reqVO);

    Integer completeMockInspection(MockInspectionCompleteReqVO reqVO);

    PageResult<InspectionSliceRespVO> getCompletedInspectionPage(InspectionSlicePageReqVO reqVO);

    PageResult<InspectionSliceRespVO> getInboundWaitPiecePage(InspectionSlicePageReqVO reqVO);

    PageResult<InspectionSliceSegmentRespVO> getInboundWaitSegmentPage(InspectionSlicePageReqVO reqVO);

    List<InspectionSliceRespVO> getInboundWaitSegmentPieceList(InspectionSlicePageReqVO reqVO);

    InspectionSliceRespVO createPackagingManualPiece(PackagingManualPieceCreateReqVO reqVO);

    Boolean deletePackagingManualPiece(PackagingManualPieceDeleteReqVO reqVO);

    PackagingManualPieceImportRespVO importPackagingManualPieces(MultipartFile file, String operatorName)
            throws java.io.IOException;

    PageResult<PieceLabelRespVO> getPieceLabelCandidatePage(PieceLabelCandidatePageReqVO reqVO);

    PieceLabelRespVO getPieceLabel(String sliceBatchNo);

    PieceLabelBatchQueryRespVO getPieceLabels(PieceLabelBatchQueryReqVO reqVO);

    PieceLabelRespVO markPieceLabelPrinted(PieceLabelPrintedReqVO reqVO);

    List<PieceLabelRespVO> markPieceLabelsPrinted(PieceLabelBatchPrintedReqVO reqVO);

    List<InboundBoxRespVO> getInboundPackageList(String keyword);

    PageResult<InboundBoxRespVO> getInboundPackagePage(String keyword, String shelfStatus, String qualityStatus,
                                                        Integer pageNo, Integer pageSize);

    PageResult<InboundPackageSegmentRespVO> getInboundPackageSegmentPage(String keyword, String shelfStatus,
                                                                          String qualityStatus,
                                                                          Integer pageNo, Integer pageSize);

    List<InboundBoxRespVO> getInboundPackageSegmentPackageList(String keyword, String shelfStatus,
                                                                String qualityStatus, String segmentBatchNo);

    PageResult<InboundPackageSegmentRespVO> getInboundPackedSegmentPage(InspectionSlicePageReqVO reqVO);

    List<InboundBoxRespVO> getInboundPackedSegmentPackageList(InspectionSlicePageReqVO reqVO);

    InboundPackageBatchRespVO lockInboundPackage(LockInboundPackageReqVO reqVO);

    InboundBoxRespVO printInboundPackageCard(BoxActionReqVO reqVO);

    InboundBoxRespVO updateInboundPackageRemark(InboundPackageRemarkUpdateReqVO reqVO);

    Long cancelInboundPackageLock(BoxActionReqVO reqVO);

    InboundPackageBatchActionRespVO cancelInboundPackageLocks(CancelInboundPackageBatchReqVO reqVO);

    InboundBoxRespVO confirmInboundPackage(ConfirmInboundPackageReqVO reqVO);

    InboundPackageBatchActionRespVO confirmInboundPackages(ConfirmInboundPackageBatchReqVO reqVO);

    InboundBoxRespVO downShelfInboundPackage(BoxActionReqVO reqVO);

    InboundPackageBatchActionRespVO downShelfInboundPackages(DownShelfInboundPackageBatchReqVO reqVO);

    InboundBoxRespVO manualOutboundInboundPackage(ManualOutboundPackageReqVO reqVO);

    InboundPackageBatchActionRespVO manualOutboundInboundPackages(ManualOutboundPackageBatchReqVO reqVO);

    /**
     * 不合格品库存查询专用：将未上架的不合格成品包装整盒手工出库。
     */
    InboundPackageBatchActionRespVO manualOutboundPendingInboundPackages(ManualOutboundPackageBatchReqVO reqVO);

    /**
     * 合格品待上架包装直接出库；该动作不创建上架库存或库位占用。
     */
    InboundPackageBatchActionRespVO directOutboundPendingQualifiedInboundPackages(
            DirectOutboundPendingPackageBatchReqVO reqVO);

    ManualOutboundStockBatchRespVO manualOutboundFgStocks(ManualOutboundStockBatchReqVO reqVO);

    ManualOutboundRepackReturnRespVO returnManualOutboundStockForRepack(ManualOutboundRepackReturnReqVO reqVO);

    List<InboundTaskRespVO> getInboundTaskList(String keyword);

    List<HcPackagingSourceRespVO> getInboundSourceList(Long planId, String motherSegmentBatchNo);

    List<InboundBoxRespVO> getInboundBoxList(Long planOperationId, String motherSegmentBatchNo);

    List<Long> initInboundBoxes(InitInboundBoxesReqVO reqVO);

    Long scanInboundPiece(ScanInboundPieceReqVO reqVO);

    Long printInboundBox(BoxActionReqVO reqVO);

    Long confirmInboundBox(BoxActionReqVO reqVO);

    PageResult<ShippingNoticeRespVO> getShippingNoticePage(ShippingNoticePageReqVO reqVO);

    ShippingNoticeRespVO getShippingNotice(Long id);

    PageResult<FgStockLedgerRespVO> getShippingNoticeStockCandidatePage(FgStockLedgerPageReqVO reqVO);

    PageResult<FgShippingBatchCandidateRespVO> getShippingNoticeBatchCandidatePage(FgShippingBatchCandidatePageReqVO reqVO);

    PageResult<FgStockLedgerRespVO> getShippingNoticePickCandidatePage(ShippingNoticePickCandidatePageReqVO reqVO);

    PageResult<ShippingNoticePickCandidateSegmentRespVO> getShippingNoticePickCandidateSegmentPage(
            ShippingNoticePickCandidatePageReqVO reqVO);

    List<FgStockLedgerRespVO> getShippingNoticePickCandidateSegmentPieceList(
            ShippingNoticePickCandidatePageReqVO reqVO);

    ShippingNoticeRespVO saveAndLockShippingNotice(ShippingNoticeSaveLockReqVO reqVO);

    ShippingNoticeRespVO changeShippingNotice(ShippingNoticeChangeReqVO reqVO);

    ShippingNoticeExcelImportRespVO importShippingNoticeExcel(MultipartFile file, String fileUrl, String operatorName);

    ShippingNoticeRespVO issueShippingNotice(ShippingNoticeIssueReqVO reqVO);

    ShippingNoticeRespVO cancelShippingNotice(ShippingNoticeCancelReqVO reqVO);

    void deleteShippingNotice(Long id);

    List<ShippingNoticeRespVO> getOutboundShippingNoticeList(String keyword);

    List<ShippingOrderRespVO> getShippingOrderList(String keyword);

    OutboundOrderRespVO initOutboundBoxesFromNotice(InitOutboundBoxesFromNoticeReqVO reqVO);

    OutboundOrderRespVO initOutboundBoxes(InitOutboundBoxesReqVO reqVO);

    OutboundOrderRespVO getOutboundOrder(String outboundNo);

    OutboundOrderRespVO getOutboundOrderByNotice(Long sourceNoticeId);

    List<OutboundBoxRespVO> getOutboundBoxList(Long outboundOrderId);

    Long scanOutboundPiece(ScanOutboundPieceReqVO reqVO);

    Long printOutboundBox(BoxActionReqVO reqVO);

    Long confirmOutboundOrder(BoxActionReqVO reqVO);

    ShippingNoticeRespVO submitShippingNoticeDelivery(ShippingNoticeDeliverySubmitReqVO reqVO);

    ShippingNoticeRespVO pickShippingNotice(ShippingNoticePickReqVO reqVO);

    ShippingNoticeRespVO returnShippingNoticePick(ShippingNoticeReturnPickReqVO reqVO);

    void autoReturnShippingFqcNgPicks(Long noticeId, List<Long> pickItemIds, String fqcNo);

    ShippingNoticeRespVO confirmShippingNoticeForFqc(ShippingNoticeOutboundConfirmReqVO reqVO);

    ShippingNoticeRespVO inspectShippingNoticeItem(ShippingNoticeInspectionReqVO reqVO);

    ShippingNoticeRespVO packShippingNotice(ShippingNoticePackReqVO reqVO);

    ShippingNoticeRespVO pushShippingNoticeOqc(ShippingNoticePackageConfirmReqVO reqVO);

    ShippingNoticeRespVO completeShippingNotice(ShippingNoticePackageConfirmReqVO reqVO);
}
