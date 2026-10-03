package cn.iocoder.yudao.module.mes.service.hc.nginventory;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationGridRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationStockReportReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationStockReportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgLocationTreeWarehouseRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceCreateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceDeleteReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceUnfreezeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceUpdateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualOutboundReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.UnqualifiedHistoryLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPiecePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchPrintedReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchQueryReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelBatchQueryRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceLabelRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgPieceSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgScrapReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgRackSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgShelfReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgTransferReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgUnshelfReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgWarehouseSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface HcNgInventoryService {

    void registerSlittingNgPiece(HcSlittingSliceRecordDO source, HcPlanOrderDO plan, HcPlanOrderOperationDO operation);

    void deleteSlittingWaitShelfNgPiece(Long sourceId);

    void registerSlittingNgPieceAttributedByPressSlot(HcSlittingSliceRecordDO source,
                                                       HcPressSlotReportDO detectionReport,
                                                       HcPlanOrderDO plan,
                                                       HcPlanOrderOperationDO operation);

    void registerPressSlotNgPiece(HcPressSlotReportDO source, HcPlanOrderDO plan, HcPlanOrderOperationDO operation);

    void registerPressSlotNgPieceAttributedByAdhesive2(HcPressSlotReportDO source,
                                                        HcAdhesive2ReportDO detectionReport,
                                                        HcPlanOrderDO plan,
                                                        HcPlanOrderOperationDO operation);

    HcProductionInstructionDO getEffectiveFreezeInstruction(Long planId, Long planOperationId);

    void registerSlittingFrozenPiece(HcSlittingSliceRecordDO source, HcPlanOrderDO plan,
                                    HcPlanOrderOperationDO operation, HcProductionInstructionDO instruction);

    void registerPressSlotFrozenPiece(HcPressSlotReportDO source, HcPlanOrderDO plan,
                                     HcPlanOrderOperationDO operation, HcProductionInstructionDO instruction);

    void autoUnfreezeByInstruction(HcProductionInstructionDO instruction);

    boolean isNgPieceManaged(String sourceType, Long sourceId);

    PageResult<NgPieceRespVO> getWaitShelfPage(NgPiecePageReqVO reqVO);

    PageResult<NgPieceRespVO> getWaitFreezeShelfPage(NgPiecePageReqVO reqVO);

    PageResult<NgPieceSegmentRespVO> getWaitShelfSegmentPage(NgPiecePageReqVO reqVO);

    List<NgPieceRespVO> getWaitShelfSegmentPieceList(NgPiecePageReqVO reqVO);

    PageResult<NgPieceSegmentRespVO> getWaitFreezeShelfSegmentPage(NgPiecePageReqVO reqVO);

    List<NgPieceRespVO> getWaitFreezeShelfSegmentPieceList(NgPiecePageReqVO reqVO);

    PageResult<NgPieceRespVO> getPiecePage(NgPiecePageReqVO reqVO);

    PageResult<NgHistoryLedgerRespVO> getHistoryLedgerPage(NgHistoryLedgerPageReqVO reqVO);

    PageResult<UnqualifiedHistoryLedgerRespVO> getUnqualifiedHistoryLedgerPage(
            UnqualifiedHistoryLedgerPageReqVO reqVO);

    PageResult<NgPieceSegmentRespVO> getInventorySegmentPage(NgPiecePageReqVO reqVO);

    List<NgPieceRespVO> getInventorySegmentPieceList(NgPiecePageReqVO reqVO);

    NgPieceLabelBatchQueryRespVO getPieceLabels(NgPieceLabelBatchQueryReqVO reqVO);

    List<NgPieceLabelRespVO> markPieceLabelsPrinted(NgPieceLabelBatchPrintedReqVO reqVO);

    List<NgLocationGridRespVO> getLocationGrid();

    List<NgLocationStockReportRespVO> getLocationStockReport(NgLocationStockReportReqVO reqVO);

    List<NgLocationTreeWarehouseRespVO> getLocationTree();

    Long saveWarehouse(NgWarehouseSaveReqVO reqVO);

    Boolean deleteWarehouse(Long id);

    Long saveRack(NgRackSaveReqVO reqVO);

    Boolean deleteRack(Long id);

    void shelf(NgShelfReqVO reqVO);

    void unshelf(NgUnshelfReqVO reqVO);

    void manualOutbound(NgManualOutboundReqVO reqVO);

    void transfer(NgTransferReqVO reqVO);

    void scrap(NgScrapReqVO reqVO);

    NgPieceRespVO createManualPiece(NgManualPieceCreateReqVO reqVO);

    NgPieceRespVO updateManualPiece(NgManualPieceUpdateReqVO reqVO);

    void deleteManualPiece(NgManualPieceDeleteReqVO reqVO);

    NgManualPieceImportRespVO importManualPieces(MultipartFile file) throws java.io.IOException;

    void unfreezeManualPiece(NgManualPieceUnfreezeReqVO reqVO);
}
