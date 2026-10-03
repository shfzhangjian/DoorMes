package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.tenant.core.util.TenantUtils;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.BoxActionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.CancelInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ConfirmInboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ConfirmInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.DownShelfInboundPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.DirectOutboundPendingPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationGridRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationPieceRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationTreeLayerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgLocationTreeRackRespVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundBoxItemRespVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.OutboundBoxItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.OutboundBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.OutboundOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageBoxPrintReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeRecordRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.PackageAuxConsumeItemReqVO;
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
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingManualPieceImportExcelVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ScanInboundPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ScanOutboundPieceReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeChangeReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeDeliveryItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeDeliverySubmitReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeAttachmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeExcelImportRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeIssueReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeInspectionReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeOutboundConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePackageConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePackReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidatePageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickCandidateSegmentRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickItemRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticePickReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeReturnPickReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeSaveItemReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeSaveLockReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingOrderRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPackagingSourceRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcLocationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgLayerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgRackDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.location.HcFgWarehouseDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom.HcBomDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundInspectionTaskDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgInboundOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgInboundOrderItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundBoxDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundBoxItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeAttachmentDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeChangeLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticePickItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingPieceEventLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcLabelPrintLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceCorrectionLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackageAuxConsumeRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackageAuxStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel.HcProductModelMaterialDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFaiOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsPackagingCoaSampleClaimDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa.QmsCoaReportShippingRelDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcLocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcFgLayerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcFgRackMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcFgWarehouseMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundInspectionTaskMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgInboundOrderItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgInboundOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgOutboundBoxItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgOutboundBoxMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgOutboundOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeAttachmentMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeChangeLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticePickItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedPackagingMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcLabelPrintLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceCorrectionLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackageAuxConsumeRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackageAuxStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productmodel.HcProductModelMaterialMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsPackagingCoaSampleClaimMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaReportShippingRelMapper;
import cn.iocoder.yudao.module.mes.service.hc.packagingevent.HcPackagingPieceEventLogService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import cn.iocoder.yudao.module.mes.service.qms.QmsFgShippingFqcService;
import cn.iocoder.yudao.module.mes.service.qms.QmsNcRecordService;
import cn.iocoder.yudao.module.mes.service.qms.QmsOqcService;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsOqcRespVO;
import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;

@Service
@Validated
@Slf4j
public class HcFinishedPackagingServiceImpl implements HcFinishedPackagingService {

    private static final String STATUS_WAITING_PIECE = "WAITING_PIECE";
    private static final String STATUS_PACKED = "PACKED";
    private static final String STATUS_INBOUND_LOCKED = "INBOUND_LOCKED";
    private static final String STATUS_INBOUNDED = "INBOUNDED";
    @Resource
    private cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingPieceEventLogMapper coaPieceEventMapper;

    private static final String QUALITY_FROZEN = "FROZEN";
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_ALLOCATED = "ALLOCATED";
    private static final String STATUS_OUTBOUND_LOCKED = "OUTBOUND_LOCKED";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_MANUAL_OUTBOUNDED = "MANUAL_OUTBOUNDED";
    private static final String INBOUND_PACKAGE_SHELF_STATUS_PENDING = "PENDING";
    private static final String INBOUND_PACKAGE_SHELF_STATUS_SHELVED = "SHELVED";
    private static final String SOURCE_CUT_ROUND_REPORT = "CUT_ROUND_REPORT";
    private static final String SOURCE_MANUAL_HISTORY = "MANUAL_HISTORY";
    private static final String MANUAL_PIECE_WAIT_PACKAGING = "WAIT_PACKAGING";
    private static final String MANUAL_PIECE_PACKED = "PACKED";
    private static final String MANUAL_PIECE_INBOUNDED = "INBOUNDED";
    private static final String LABEL_TYPE_PIECE = "PIECE";
    private static final String PICK_SOURCE_WAREHOUSE_STOCK = "WAREHOUSE_STOCK";
    private static final String PICK_SOURCE_PACKAGING_DIRECT = "PACKAGING_DIRECT";
    private static final int PICK_CANDIDATE_QUERY_BATCH_SIZE = 200;
    private static final String UNIDENTIFIED_PICK_SEGMENT_LABEL = "未识别分段批号";
    private static final String NOTICE_STATUS_DRAFT = "DRAFT";
    private static final String NOTICE_STATUS_LOCKED = "LOCKED";
    private static final String NOTICE_STATUS_SUBMITTED = "SUBMITTED";
    private static final String NOTICE_STATUS_PICKED = "PICKED";
    private static final String NOTICE_STATUS_SHIP_CONFIRMED = "SHIP_CONFIRMED";
    private static final String NOTICE_STATUS_INSPECTED = "INSPECTED";
    private static final String NOTICE_STATUS_OQC_INSPECTING = "OQC_INSPECTING";
    private static final String NOTICE_STATUS_OQC_PASSED = "OQC_PASSED";
    private static final String NOTICE_STATUS_OQC_REJECTED = "OQC_REJECTED";
    private static final String NOTICE_STATUS_PACKAGED = "PACKAGED";
    private static final String NOTICE_STATUS_OUTBOUND = "OUTBOUND";
    private static final String NOTICE_STATUS_SHIPPED = "SHIPPED";
    private static final String NOTICE_STATUS_CLOSED = "CLOSED";
    private static final String NOTICE_STATUS_CANCELLED = "CANCELLED";
    private static final List<String> NOTICE_EXECUTING_STATUSES = List.of(
            NOTICE_STATUS_SUBMITTED,
            NOTICE_STATUS_PICKED,
            NOTICE_STATUS_SHIP_CONFIRMED,
            NOTICE_STATUS_INSPECTED,
            NOTICE_STATUS_OQC_INSPECTING,
            NOTICE_STATUS_OQC_PASSED,
            NOTICE_STATUS_OQC_REJECTED,
            NOTICE_STATUS_PACKAGED,
            NOTICE_STATUS_LOCKED,
            NOTICE_STATUS_OUTBOUND);
    private static final List<String> NOTICE_RETURN_PICK_STATUSES = List.of(
            NOTICE_STATUS_SUBMITTED,
            NOTICE_STATUS_PICKED,
            NOTICE_STATUS_SHIP_CONFIRMED,
            NOTICE_STATUS_INSPECTED,
            NOTICE_STATUS_OQC_INSPECTING,
            NOTICE_STATUS_OQC_PASSED,
            NOTICE_STATUS_OQC_REJECTED,
            NOTICE_STATUS_PACKAGED,
            NOTICE_STATUS_LOCKED);
    private static final List<String> NOTICE_OUTBOUND_READY_STATUSES = List.of(
            NOTICE_STATUS_PACKAGED,
            NOTICE_STATUS_LOCKED,
            NOTICE_STATUS_OUTBOUND);
    private static final String PRODUCT_TYPE_MASS = "MASS";
    private static final String PRODUCT_TYPE_SAMPLE = "SAMPLE";
    private static final String PRODUCT_TYPE_RND = "RND";
    private static final String INSPECTION_STATUS_INSPECTING = "INSPECTING";
    private static final String INSPECTION_STATUS_COMPLETED = "COMPLETED";
    private static final String INSPECTION_RESULT_OK = "OK";
    private static final String INSPECTION_RESULT_NG = "NG";
    private static final String COA_RESULT_PENDING = "PENDING";
    private static final String COA_RESULT_UNKNOWN = "UNKNOWN";
    private static final String SOURCE_MENU_CODE_ADHESIVE2 = "ADHESIVE2_REPORT";
    private static final String FAI_STATUS_PENDING = "PENDING";
    private static final String FAI_STATUS_INSPECTING = "INSPECTING";
    private static final String FAI_STATUS_WAITING_QA = "WAITING_QA";
    private static final String FAI_STATUS_SUSPENDED = "SUSPENDED";
    private static final String FAI_STATUS_REWORKING = "REWORKING";
    private static final String FAI_STATUS_COMPLETED = "COMPLETED";
    private static final String FAI_STATUS_REJECTED = "REJECTED";
    private static final String FAI_STATUS_CANCELED = "CANCELED";
    private static final String FAI_JUDGMENT_PENDING = "PENDING";
    private static final String FG_LOCATION_SCENE = "PACKAGE_FG";
    private static final int FG_LOCATION_CODE_MAX_PART = 999;
    private static final String DEFAULT_LOCATION_TYPE = "成品区域";
    private static final String DEFAULT_LOCATION_STATUS = "启用";
    private static final String FG_LOCATION_QUALITY_SCOPE_QUALIFIED = "QUALIFIED";
    private static final String FG_LOCATION_QUALITY_SCOPE_QUARANTINE = "QUARANTINE";
    private static final String FG_LOCATION_QUALITY_SCOPE_UNASSIGNED = "UNASSIGNED";
    private static final String AUX_CATEGORY_PACKAGING_BAG = "PACKAGING_BAG";
    private static final String AUX_CATEGORY_PACKAGING_BAG_NAME = "包装袋";
    private static final String AUX_STOCK_MEASURE_COUNT = "COUNT";
    private static final String AUX_STOCK_ACTIVE = "ACTIVE";
    private static final String AUX_STOCK_USED_UP = "USED_UP";
    private static final String AUX_CONSUME_RECORDED = "RECORDED";
    private static final String AUX_BIZ_FG_INBOUND = "FG_INBOUND";
    private static final String AUX_BIZ_FG_INNER_PACKAGING = "FG_INNER_PACKAGING";
    private static final String AUX_BIZ_FG_SHIPPING_OUTER_PACKAGING = "FG_SHIPPING_OUTER_PACKAGING";
    private static final String TOOLING_LEDGER_USAGE_ACTIVE = "ACTIVE";
    private static final String TOOLING_CONSUME_TYPE_NORMAL = "NORMAL";
    private static final String DEFAULT_AUX_EDGE_WAREHOUSE_NAME = "包装辅材边库";
    private static final int INBOUND_PACKAGE_NO_LOCK_TIMEOUT_SECONDS = 10;
    private static final int MANUAL_PIECE_IMPORT_MAX_ROWS = 2000;
    private static final long MANUAL_PIECE_IMPORT_MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final String PACKAGING_PROCESS_CODE = "PACKAGING";
    private static final String PACKAGING_STARTUP_FORM_TYPE = "STARTUP_CHECK";
    private static final String PACKAGING_CLEANING_FORM_TYPE = "CLEANING_CHECK";
    private static final String PACKAGING_INNER_FORM_TYPE = "INNER_PACKAGING_CHECK";
    private static final String PACKAGING_OUTER_FORM_TYPE = "OUTER_PACKAGING_CHECK";
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter BUSINESS_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SHIPPING_NOTICE_ATTACHMENT_EXCEL_IMPORT = "EXCEL_IMPORT";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");
    private static final Pattern FG_LOCATION_CODE_PATTERN = Pattern.compile("^(?:[A-Z][A-Z0-9_-]{0,31}-)?(\\d+)-L(\\d+)-(\\d+)$");
    private static final Pattern FG_WAREHOUSE_CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{0,31}$");
    private static final Pattern EXCEL_DATE_TEXT_PATTERN = Pattern.compile("((?:19|20)\\d{2})\\D{0,3}(\\d{1,2})\\D{0,3}(\\d{1,2})");
    private static final Pattern LEADING_CODE_PATTERN = Pattern.compile("([A-Za-z0-9][A-Za-z0-9._/-]*)");
    private static final Pattern BRACKET_CODE_PATTERN = Pattern.compile("[（(]\\s*([^）)]+?)\\s*[）)]");

    @Resource
    private HcFinishedPackagingMapper hcFinishedPackagingMapper;
    @Resource
    private HcLocationMapper hcLocationMapper;
    @Resource
    private HcFgRackMapper hcFgRackMapper;
    @Resource
    private HcFgLayerMapper hcFgLayerMapper;
    @Resource
    private HcFgWarehouseMapper hcFgWarehouseMapper;
    @Resource
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Resource
    private HcCutRoundInspectionTaskMapper hcCutRoundInspectionTaskMapper;
    @Resource
    private HcCutRoundInspectionDetailMapper hcCutRoundInspectionDetailMapper;
    @Resource
    private HcProcessFormRecordMapper hcProcessFormRecordMapper;
    @Resource
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Resource
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Resource
    private HcInnerPackUnitMapper hcInnerPackUnitMapper;
    @Resource
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Resource
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Resource
    private HcPackagingPieceEventLogService hcPackagingPieceEventLogService;
    @Resource
    private HcPackagingManualPieceCorrectionLogMapper hcPackagingManualPieceCorrectionLogMapper;
    @Resource
    private HcLabelPrintLogMapper hcLabelPrintLogMapper;
    @Resource
    private HcFgInboundOrderMapper hcFgInboundOrderMapper;
    @Resource
    private HcFgInboundOrderItemMapper hcFgInboundOrderItemMapper;
    @Resource
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Resource
    private HcFinishedStockTxnLogMapper hcFinishedStockTxnLogMapper;
    @Resource
    private HcFgOutboundOrderMapper hcFgOutboundOrderMapper;
    @Resource
    private HcFgOutboundBoxMapper hcFgOutboundBoxMapper;
    @Resource
    private HcFgOutboundBoxItemMapper hcFgOutboundBoxItemMapper;
    @Resource
    private HcFgShippingNoticeMapper hcFgShippingNoticeMapper;
    @Resource
    private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Resource
    private HcFgShippingNoticeAttachmentMapper hcFgShippingNoticeAttachmentMapper;
    @Resource
    private HcFgShippingNoticeChangeLogMapper hcFgShippingNoticeChangeLogMapper;
    @Resource
    private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Resource
    private HcPackageAuxStockMapper hcPackageAuxStockMapper;
    @Resource
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Resource
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Resource
    private QmsPackagingCoaSampleClaimMapper qmsPackagingCoaSampleClaimMapper;
    @Resource
    private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;
    @Resource
    private QmsOqcOrderMapper qmsOqcOrderMapper;
    @Resource
    private QmsCoaReportMapper qmsCoaReportMapper;
    @Resource
    private QmsCoaReportShippingRelMapper qmsCoaReportShippingRelMapper;
    @Resource
    private HcPackageAuxConsumeRecordMapper hcPackageAuxConsumeRecordMapper;
    @Resource
    private HcToolingConsumableLedgerMapper hcToolingConsumableLedgerMapper;
    @Resource
    private HcToolingConsumableConsumeMapper hcToolingConsumableConsumeMapper;
    @Resource
    private QmsOqcService qmsOqcService;
    @Resource
    private QmsFgShippingFqcService qmsFgShippingFqcService;
    @Resource
    private QmsNcRecordService qmsNcRecordService;
    @Resource
    private HcProductModelMaterialMapper hcProductModelMaterialMapper;
    @Resource
    private HcBomMapper hcBomMapper;

    @Override
    public List<PackageBoxRespVO> getPackageBoxList(String boxType, String keyword) {
        return hcFinishedPackagingMapper.selectPackageBoxList(
                StrUtil.blankToDefault(StrUtil.trimToNull(boxType), "ALL").toUpperCase(),
                StrUtil.trimToNull(keyword));
    }

    @Override
    public InboundBoxRespVO getInboundBoxDetail(Long id) {
        return buildInboundBoxResp(getInboundBox(id));
    }

    @Override
    public OutboundBoxRespVO getOutboundBoxDetail(Long id) {
        return buildOutboundBoxResp(getOutboundBox(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long printPackageBox(PackageBoxPrintReqVO reqVO) {
        String boxType = StrUtil.trimToEmpty(reqVO.getBoxType()).toUpperCase();
        BoxActionReqVO action = new BoxActionReqVO();
        action.setId(reqVO.getId());
        action.setOperatorName(reqVO.getOperatorName());
        if ("INBOUND".equals(boxType)) {
            return printInboundBox(action);
        }
        if ("OUTBOUND".equals(boxType)) {
            return printOutboundBox(action);
        }
        throw invalidParamException("包装箱类型只允许成品入库或发货出库");
    }

    @Override
    public PageResult<InspectionSliceRespVO> getMockInspectionPage(InspectionSlicePageReqVO reqVO) {
        return getInspectionSlicePage(reqVO, INSPECTION_STATUS_INSPECTING);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer completeMockInspection(MockInspectionCompleteReqVO reqVO) {
        String result = normalizeInspectionResult(reqVO.getInspectionResult());
        List<Long> ids = reqVO.getCutRoundReportIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            throw invalidParamException("请选择待检测裁切片");
        }
        List<HcCutRoundReportDO> reports = hcCutRoundReportMapper.selectListByIds(ids);
        if (reports.size() != ids.size()) {
            throw invalidParamException("部分裁切片不存在，请刷新后重试");
        }
        List<HcCutRoundReportDO> invalidReports = reports.stream()
                .filter(item -> !INSPECTION_STATUS_INSPECTING.equalsIgnoreCase(StrUtil.blankToDefault(item.getInspectionStatus(), "")))
                .toList();
        if (!invalidReports.isEmpty()) {
            throw invalidParamException("只能批量完成待检测状态的裁切片");
        }

        LocalDateTime now = LocalDateTime.now();
        String inspectorName = firstNotBlank(reqVO.getInspectorName(), currentUserName());
        String remark = StrUtil.trimToNull(reqVO.getInspectionRemark());
        for (HcCutRoundReportDO report : reports) {
            HcCutRoundReportDO update = new HcCutRoundReportDO();
            update.setId(report.getId());
            update.setInspectionStatus(INSPECTION_STATUS_COMPLETED);
            update.setInspectionResult(result);
            update.setInspectorName(inspectorName);
            update.setInspectionTime(now);
            update.setInspectionRemark(remark);
            hcCutRoundReportMapper.updateById(update);
            hcPackagingPieceEventLogService.recordCutRoundFqcResult(report, result, inspectorName, now,
                    null, remark);

            for (HcCutRoundInspectionDetailDO detail : hcCutRoundInspectionDetailMapper.selectListByCutRoundReportId(report.getId())) {
                HcCutRoundInspectionDetailDO detailUpdate = new HcCutRoundInspectionDetailDO();
                detailUpdate.setId(detail.getId());
                detailUpdate.setInspectionResult(result);
                detailUpdate.setInspectorName(inspectorName);
                detailUpdate.setInspectionTime(now);
                detailUpdate.setRemark(remark);
                hcCutRoundInspectionDetailMapper.updateById(detailUpdate);
            }
        }

        reports.stream()
                .map(HcCutRoundReportDO::getInspectionTaskId)
                .filter(Objects::nonNull)
                .distinct()
                .forEach(this::refreshInspectionTaskStatus);
        return reports.size();
    }

    @Override
    public PageResult<InspectionSliceRespVO> getCompletedInspectionPage(InspectionSlicePageReqVO reqVO) {
        return getInspectionSlicePage(reqVO, INSPECTION_STATUS_COMPLETED);
    }

    @Override
    public PageResult<InspectionSliceRespVO> getInboundWaitPiecePage(InspectionSlicePageReqVO reqVO) {
        List<InspectionSliceRespVO> rows = listInboundWaitInspectionSlicesForGrouping(reqVO);
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(200, Math.max(1, reqVO.getPageSize()));
        int start = Math.min((pageNo - 1) * pageSize, rows.size());
        int end = Math.min(start + pageSize, rows.size());
        return new PageResult<>(rows.subList(start, end), (long) rows.size());
    }

    @Override
    public PageResult<InspectionSliceSegmentRespVO> getInboundWaitSegmentPage(InspectionSlicePageReqVO reqVO) {
        List<InspectionSliceRespVO> rows = listInboundWaitInspectionSlicesForGrouping(reqVO);
        if (rows.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        Map<String, List<InspectionSliceRespVO>> segmentRowsMap = new LinkedHashMap<>();
        for (InspectionSliceRespVO row : rows) {
            String segmentBatchNo = resolvePackageSegmentBatchNo(row.getParentProductionBatchNo(),
                    row.getCoaScopeBatchNo(), row.getProductionBatchNo(), row.getSliceBatchNo());
            row.setSegmentBatchNo(segmentBatchNo);
            if (StrUtil.isBlank(segmentBatchNo)) {
                continue;
            }
            segmentRowsMap.computeIfAbsent(segmentBatchNo, key -> new ArrayList<>()).add(row);
        }
        List<InspectionSliceSegmentRespVO> segments = segmentRowsMap.entrySet().stream()
                .map(entry -> buildInboundWaitSegmentResp(entry.getKey(), entry.getValue()))
                .toList();
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(200, Math.max(1, reqVO.getPageSize()));
        int start = Math.min((pageNo - 1) * pageSize, segments.size());
        int end = Math.min(start + pageSize, segments.size());
        return new PageResult<>(segments.subList(start, end), (long) segments.size());
    }

    @Override
    public List<InspectionSliceRespVO> getInboundWaitSegmentPieceList(InspectionSlicePageReqVO reqVO) {
        String segmentBatchNo = normalizeCoaSegmentBatchNo(reqVO.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            return Collections.emptyList();
        }
        reqVO.setSegmentBatchNo(segmentBatchNo);
        return listInboundWaitInspectionSlicesForGrouping(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InspectionSliceRespVO createPackagingManualPiece(PackagingManualPieceCreateReqVO reqVO) {
        HcPackagingManualPieceDO piece = preparePackagingManualPiece(reqVO);
        hcPackagingManualPieceMapper.insert(piece);
        hcPackagingPieceEventLogService.recordManualPieceEvent(piece, "PACKAGING_NG_ENTER", null,
                MANUAL_PIECE_WAIT_PACKAGING, piece.getRecorderTime(), piece.getRecorderName(), null,
                "历史补录不合格待包装片");
        return buildManualInspectionSlice(piece);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePackagingManualPiece(PackagingManualPieceDeleteReqVO reqVO) {
        HcPackagingManualPieceDO piece = hcPackagingManualPieceMapper.selectByIdForUpdate(reqVO.getId());
        if (piece == null) {
            throw invalidParamException("历史补录片不存在或已删除，请刷新后重试");
        }
        if (MANUAL_PIECE_PACKED.equals(piece.getRecordStatus())) {
            throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 已包装，请先在待上架列表撤销包装后再删除");
        }
        if (MANUAL_PIECE_INBOUNDED.equals(piece.getRecordStatus())) {
            throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 已入库，不能删除历史补录记录");
        }
        if (!MANUAL_PIECE_WAIT_PACKAGING.equals(piece.getRecordStatus())) {
            throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 当前状态不允许删除");
        }
        if (piece.getInnerUnitId() != null || StrUtil.isNotBlank(piece.getInnerUnitNo())
                || hcInnerPackUnitItemMapper.selectByManualPieceId(piece.getId()) != null
                || hcInnerPackUnitItemMapper.selectBySliceBatchNo(piece.getSliceBatchNo()) != null) {
            throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 已关联包装明细，请先撤销包装后再删除");
        }
        if (hcFinishedStockMapper.selectBySliceBatchNo(piece.getSliceBatchNo()) != null) {
            throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 已生成成品库存，不能删除历史补录记录");
        }
        String deleteReason = StrUtil.trim(reqVO.getDeleteReason());
        if (StrUtil.isBlank(deleteReason)) {
            throw invalidParamException("删除原因不能为空");
        }
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        String deleteUserName = currentUserName();
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String updater = loginUserId == null ? "" : String.valueOf(loginUserId);
        int updated = hcPackagingManualPieceMapper.voidWaitPackagingPiece(
                piece.getId(), deleteReason, deleteUserName, now, updater);
        if (updated != 1) {
            throw invalidParamException("片号状态已变化，删除失败，请刷新后重试");
        }
        hcPackagingPieceEventLogService.recordManualPieceEvent(piece, "PACKAGING_VOID", MANUAL_PIECE_WAIT_PACKAGING,
                "VOID", now, deleteUserName, null, deleteReason);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackagingManualPieceImportRespVO importPackagingManualPieces(MultipartFile file, String operatorName)
            throws IOException {
        PackagingManualPieceImportRespVO respVO = new PackagingManualPieceImportRespVO();
        if (file == null || file.isEmpty()) {
            addManualPieceImportFailure(respVO, "导入文件为空");
            return respVO;
        }
        String fileName = StrUtil.blankToDefault(file.getOriginalFilename(), "成品包装历史片导入.xlsx");
        String lowerFileName = fileName.toLowerCase(Locale.ROOT);
        if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
            addManualPieceImportFailure(respVO, "仅支持导入 .xlsx/.xls 文件");
            return respVO;
        }
        if (file.getSize() > MANUAL_PIECE_IMPORT_MAX_FILE_SIZE) {
            addManualPieceImportFailure(respVO, "导入文件不能超过5MB");
            return respVO;
        }

        List<HcPackagingManualPieceImportExcelVO> excelRows =
                ExcelUtils.read(file, HcPackagingManualPieceImportExcelVO.class);
        List<HcPackagingManualPieceDO> pieces = new ArrayList<>();
        Set<String> fileSliceBatchNos = new HashSet<>();
        for (int index = 0; index < excelRows.size(); index++) {
            HcPackagingManualPieceImportExcelVO excelRow = excelRows.get(index);
            if (isBlankManualPieceImportRow(excelRow)) {
                respVO.setSkippedRows(respVO.getSkippedRows() + 1);
                continue;
            }
            respVO.setTotalRows(respVO.getTotalRows() + 1);
            int rowNo = index + 2;
            if (respVO.getTotalRows() > MANUAL_PIECE_IMPORT_MAX_ROWS) {
                addManualPieceImportFailure(respVO,
                        String.format("有效数据超过%d行，请拆分文件后导入", MANUAL_PIECE_IMPORT_MAX_ROWS));
                break;
            }
            String sliceBatchNo = normalizeUpperText(excelRow.getSliceBatchNo());
            if (StrUtil.isBlank(sliceBatchNo)) {
                addManualPieceImportFailure(respVO, String.format("第%d行：片号不能为空", rowNo));
                continue;
            }
            if (!fileSliceBatchNos.add(sliceBatchNo)) {
                addManualPieceImportFailure(respVO, String.format("第%d行：同一文件内片号 %s 重复", rowNo, sliceBatchNo));
                continue;
            }
            LocalDate productionDate = parseManualPieceImportDate(
                    excelRow.getProductionDate(), rowNo, "生产日期", true, respVO);
            LocalDate expiryDate = parseManualPieceImportDate(
                    excelRow.getExpiryDate(), rowNo, "有效期", false, respVO);
            if (productionDate == null) {
                continue;
            }
            if (expiryDate == null && StrUtil.isBlank(excelRow.getExpiryDate())) {
                expiryDate = productionDate.plusMonths(10).minusDays(1);
            }
            if (expiryDate == null) {
                continue;
            }
            PackagingManualPieceCreateReqVO reqVO = new PackagingManualPieceCreateReqVO();
            reqVO.setSliceBatchNo(sliceBatchNo);
            reqVO.setSegmentBatchNo(normalizeUpperText(excelRow.getSegmentBatchNo()));
            reqVO.setModelCode(normalizeUpperText(excelRow.getModelCode()));
            reqVO.setMaterialCode(normalizeUpperText(excelRow.getMaterialCode()));
            reqVO.setProductionDate(productionDate);
            reqVO.setExpiryDate(expiryDate);
            reqVO.setInspectionResult(normalizeUpperText(excelRow.getInspectionResult()));
            reqVO.setCoaInspectionResult(normalizeUpperText(excelRow.getCoaInspectionResult()));
            reqVO.setBackfillReason(StrUtil.trimToEmpty(excelRow.getBackfillReason()));
            reqVO.setRecorderName(firstNotBlank(operatorName, currentUserName()));
            reqVO.setRemark(StrUtil.trimToNull(excelRow.getRemark()));
            try {
                validatePackagingManualPieceImportReq(reqVO);
                // 历史片批量导入仅补录既有实物追溯数据，不要求与当前启用产品 BOM 对齐。
                pieces.add(preparePackagingManualPiece(reqVO, false));
            } catch (RuntimeException ex) {
                addManualPieceImportFailure(respVO,
                        String.format("第%d行：%s", rowNo, firstNotBlank(ex.getMessage(), "校验失败")));
            }
        }
        if (respVO.getTotalRows() == 0) {
            addManualPieceImportFailure(respVO, "Excel未读取到有效历史片数据");
        }
        if (!respVO.getFailures().isEmpty()) {
            respVO.setFailureCount(respVO.getFailures().size());
            respVO.getMessages().add("导入校验未通过，未写入任何历史片数据");
            return respVO;
        }
        for (HcPackagingManualPieceDO piece : pieces) {
            hcPackagingManualPieceMapper.insert(piece);
        }
        respVO.setSuccessCount(pieces.size());
        respVO.getMessages().add(String.format("历史片导入完成：成功 %d 行", pieces.size()));
        return respVO;
    }

    private HcPackagingManualPieceDO preparePackagingManualPiece(PackagingManualPieceCreateReqVO reqVO) {
        return preparePackagingManualPiece(reqVO, true);
    }

    private HcPackagingManualPieceDO preparePackagingManualPiece(PackagingManualPieceCreateReqVO reqVO,
                                                                  boolean requireEnabledProductBom) {
        String sliceBatchNo = StrUtil.trimToEmpty(reqVO.getSliceBatchNo()).toUpperCase(Locale.ROOT);
        String segmentBatchNo = StrUtil.trimToEmpty(reqVO.getSegmentBatchNo()).toUpperCase(Locale.ROOT);
        String modelCode = StrUtil.trimToEmpty(reqVO.getModelCode()).toUpperCase(Locale.ROOT);
        String materialCode = StrUtil.trimToEmpty(reqVO.getMaterialCode()).toUpperCase(Locale.ROOT);
        validateManualPieceImportText(segmentBatchNo, "分段批号", 100, true);
        if (hcCutRoundReportMapper.selectByProductionBatchNo(sliceBatchNo) != null) {
            throw invalidParamException("片号 " + sliceBatchNo + " 已存在MES裁切报工，请直接从待包装列表处理");
        }
        if (hcPackagingManualPieceMapper.selectBySliceBatchNo(sliceBatchNo) != null) {
            throw invalidParamException("片号 " + sliceBatchNo + " 已存在历史补录记录，不能重复新增");
        }
        if (hcInnerPackUnitItemMapper.selectBySliceBatchNo(sliceBatchNo) != null) {
            throw invalidParamException("片号 " + sliceBatchNo + " 已完成包装，不能重复新增");
        }
        if (hcFinishedStockMapper.selectBySliceBatchNo(sliceBatchNo) != null) {
            throw invalidParamException("片号 " + sliceBatchNo + " 已存在成品库存，不能重复新增");
        }
        HcBomDO bom = requireEnabledProductBom
                ? hcBomMapper.selectEnabledByMaterialCodeAndModelCode(materialCode, modelCode)
                : null;
        if (requireEnabledProductBom && bom == null) {
            throw invalidParamException("产品料号 " + materialCode + " 与型号 " + modelCode + " 未匹配到启用的产品BOM");
        }
        LocalDate expectedExpiryDate = reqVO.getProductionDate().plusMonths(10).minusDays(1);
        if (!expectedExpiryDate.equals(reqVO.getExpiryDate())) {
            throw invalidParamException("有效期应为生产日期加10个月减1天：" + expectedExpiryDate);
        }
        String inspectionResult = normalizeManualInspectionResult(reqVO.getInspectionResult(), "裁切FQC");
        String coaInspectionResult = normalizeManualInspectionResult(reqVO.getCoaInspectionResult(), "COA送检");
        LocalDateTime now = LocalDateTime.now();
        HcPackagingManualPieceDO piece = HcPackagingManualPieceDO.builder()
                .tenantId(currentTenantId())
                .sliceBatchNo(sliceBatchNo)
                .segmentBatchNo(segmentBatchNo)
                .materialCode(materialCode)
                .materialName(bom == null ? materialCode : bom.getProductMaterialName())
                .modelCode(modelCode)
                .productSize(bom == null ? null : bom.getProductSpec())
                .productionDate(reqVO.getProductionDate())
                .expiryDate(reqVO.getExpiryDate())
                .inspectionResult(inspectionResult)
                .coaInspectionResult(coaInspectionResult)
                .recordStatus(MANUAL_PIECE_WAIT_PACKAGING)
                .printStatus("UNPRINTED")
                .printCount(0)
                .backfillReason(StrUtil.trim(reqVO.getBackfillReason()))
                .recorderName(firstNotBlank(reqVO.getRecorderName(), currentUserName()))
                .recorderTime(now)
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .build();
        return piece;
    }

    @Override
    public PageResult<PieceLabelRespVO> getPieceLabelCandidatePage(PieceLabelCandidatePageReqVO reqVO) {
        reqVO.setSliceBatchNo(normalizeUpperText(reqVO.getSliceBatchNo()));
        reqVO.setSourceType(normalizePieceLabelCandidateFilter(
                reqVO.getSourceType(), List.of(SOURCE_CUT_ROUND_REPORT, SOURCE_MANUAL_HISTORY), "数据来源"));
        reqVO.setPrintStatus(normalizePieceLabelCandidateFilter(
                reqVO.getPrintStatus(), List.of("UNPRINTED", "PRINTED"), "打印状态"));
        reqVO.setBusinessStatus(normalizePieceLabelCandidateFilter(
                reqVO.getBusinessStatus(), List.of("WAIT_PACKAGING", "PACKED", "IN_STOCK"), "业务状态"));
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(50, Math.max(1, reqVO.getPageSize()));
        int offset = (pageNo - 1) * pageSize;
        Long total = hcFinishedPackagingMapper.countPieceLabelCandidatePage(reqVO);
        if (total == null || total == 0L) {
            return PageResult.empty();
        }
        List<PieceLabelRespVO> refs = hcFinishedPackagingMapper.selectPieceLabelCandidatePage(
                reqVO, offset, pageSize);
        List<PieceLabelRespVO> labels = refs.stream()
                .map(this::buildPieceLabelByCandidateRef)
                .filter(Objects::nonNull)
                .toList();
        return new PageResult<>(labels, total);
    }

    private String normalizePieceLabelCandidateFilter(String value, List<String> allowedValues, String fieldName) {
        String normalized = normalizeUpperText(value);
        if (StrUtil.isBlank(normalized) || "ALL".equals(normalized)) {
            return null;
        }
        if (!allowedValues.contains(normalized)) {
            throw invalidParamException(fieldName + "不支持：" + normalized);
        }
        return normalized;
    }

    private PieceLabelRespVO buildPieceLabelByCandidateRef(PieceLabelRespVO ref) {
        if (ref == null || ref.getSourceId() == null) {
            return null;
        }
        if (SOURCE_CUT_ROUND_REPORT.equals(ref.getSourceType())) {
            HcCutRoundReportDO report = hcCutRoundReportMapper.selectById(ref.getSourceId());
            return report == null || Boolean.TRUE.equals(report.getDeleted()) ? null : buildCutRoundPieceLabel(report);
        }
        if (SOURCE_MANUAL_HISTORY.equals(ref.getSourceType())) {
            HcPackagingManualPieceDO piece = hcPackagingManualPieceMapper.selectById(ref.getSourceId());
            return piece == null || Boolean.TRUE.equals(piece.getDeleted()) ? null : buildManualPieceLabel(piece);
        }
        return null;
    }

    @Override
    public PieceLabelRespVO getPieceLabel(String sliceBatchNo) {
        String normalizedSliceBatchNo = StrUtil.trimToEmpty(sliceBatchNo).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(normalizedSliceBatchNo)) {
            throw invalidParamException("请输入片号");
        }
        PieceLabelRespVO label = findPieceLabel(normalizedSliceBatchNo);
        if (label != null) {
            return label;
        }
        throw invalidParamException("未找到片号 " + normalizedSliceBatchNo + "，请先新增历史片或核对片号");
    }

    @Override
    public PieceLabelBatchQueryRespVO getPieceLabels(PieceLabelBatchQueryReqVO reqVO) {
        PieceLabelBatchQueryRespVO respVO = new PieceLabelBatchQueryRespVO();
        Set<String> seen = new HashSet<>();
        for (String rawSliceBatchNo : reqVO.getSliceBatchNos()) {
            String sliceBatchNo = normalizeUpperText(rawSliceBatchNo);
            if (!seen.add(sliceBatchNo)) {
                respVO.getFailures().add("片号 " + sliceBatchNo + " 重复，已忽略");
                continue;
            }
            PieceLabelRespVO label = findPieceLabel(sliceBatchNo);
            if (label == null) {
                respVO.getFailures().add("未找到片号 " + sliceBatchNo);
                continue;
            }
            respVO.getLabels().add(label);
        }
        return respVO;
    }

    private PieceLabelRespVO findPieceLabel(String normalizedSliceBatchNo) {
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectByProductionBatchNo(normalizedSliceBatchNo);
        if (report != null) {
            return buildCutRoundPieceLabel(report);
        }
        HcPackagingManualPieceDO manualPiece = hcPackagingManualPieceMapper.selectBySliceBatchNo(normalizedSliceBatchNo);
        if (manualPiece != null) {
            return buildManualPieceLabel(manualPiece);
        }
        return null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PieceLabelRespVO markPieceLabelPrinted(PieceLabelPrintedReqVO reqVO) {
        return markPieceLabelPrintedInternal(reqVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PieceLabelRespVO> markPieceLabelsPrinted(PieceLabelBatchPrintedReqVO reqVO) {
        List<PieceLabelRespVO> labels = new ArrayList<>();
        Set<String> sourceKeys = new HashSet<>();
        for (PieceLabelPrintedReqVO item : reqVO.getItems()) {
            String sourceKey = normalizeUpperText(item.getSourceType()) + ":" + item.getSourceId();
            if (!sourceKeys.add(sourceKey)) {
                throw invalidParamException("同一片号打印记录不能重复回写：" + sourceKey);
            }
            labels.add(markPieceLabelPrintedInternal(item));
        }
        return labels;
    }

    private PieceLabelRespVO markPieceLabelPrintedInternal(PieceLabelPrintedReqVO reqVO) {
        String sourceType = StrUtil.trimToEmpty(reqVO.getSourceType()).toUpperCase(Locale.ROOT);
        LocalDateTime now = LocalDateTime.now();
        PieceLabelRespVO label;
        if (SOURCE_CUT_ROUND_REPORT.equals(sourceType)) {
            HcCutRoundReportDO report = hcCutRoundReportMapper.selectByIdForUpdate(reqVO.getSourceId());
            if (report == null) {
                throw invalidParamException("裁切报工片号不存在");
            }
            PiecePrintMeta meta = resolveCutRoundPiecePrintMeta(report);
            Map<String, Object> extra = parseExtraMap(report.getExtraJson());
            extra.put("printStatus", "已打印");
            extra.put("printCount", meta.printCount() + 1);
            extra.put("printTime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            HcCutRoundReportDO update = new HcCutRoundReportDO();
            update.setId(report.getId());
            update.setExtraJson(JsonUtils.toJsonString(extra));
            hcCutRoundReportMapper.updateById(update);
            label = buildCutRoundPieceLabel(hcCutRoundReportMapper.selectById(report.getId()));
        } else if (SOURCE_MANUAL_HISTORY.equals(sourceType)) {
            HcPackagingManualPieceDO piece = hcPackagingManualPieceMapper.selectByIdForUpdate(reqVO.getSourceId());
            if (piece == null) {
                throw invalidParamException("历史补录片号不存在");
            }
            int printCount = intValue(piece.getPrintCount());
            HcPackagingManualPieceDO update = new HcPackagingManualPieceDO();
            update.setId(piece.getId());
            update.setPrintStatus("PRINTED");
            update.setPrintCount(printCount + 1);
            update.setLastPrintTime(now);
            hcPackagingManualPieceMapper.updateById(update);
            label = buildManualPieceLabel(hcPackagingManualPieceMapper.selectById(piece.getId()));
        } else {
            throw invalidParamException("不支持的片号来源类型：" + sourceType);
        }
        hcLabelPrintLogMapper.insert(HcLabelPrintLogDO.builder()
                .tenantId(currentTenantId())
                .labelNo(label.getSliceBatchNo())
                .labelType(LABEL_TYPE_PIECE)
                .bizId(label.getSourceId())
                .bizNo(label.getSliceBatchNo())
                .planNo(label.getPlanNo())
                .printCount(label.getPrintCount())
                .printerName(StrUtil.trimToNull(reqVO.getPrinterName()))
                .printTime(now)
                .labelContentJson(StrUtil.trimToNull(reqVO.getLabelContentJson()))
                .build());
        return label;
    }

    @Override
    public List<InboundBoxRespVO> getInboundPackageList(String keyword) {
        return hcInnerPackUnitMapper.selectFgInboundPackageList(StrUtil.trimToNull(keyword)).stream()
                .map(this::buildInboundBoxResp)
                .filter(this::isVisibleInboundPackage)
                .toList();
    }

    @Override
    public PageResult<InboundBoxRespVO> getInboundPackagePage(String keyword, String shelfStatus,
                                                               String qualityStatus, Integer pageNo, Integer pageSize) {
        String normalizedShelfStatus = normalizeInboundPackageShelfStatus(shelfStatus);
        String normalizedQualityStatus = normalizeInboundPackageQualityStatus(qualityStatus);
        int normalizedPageNo = pageNo == null ? 1 : Math.max(1, pageNo);
        int normalizedPageSize = pageSize == null ? 20 : Math.min(200, Math.max(1, pageSize));
        int offset = (normalizedPageNo - 1) * normalizedPageSize;
        String normalizedKeyword = StrUtil.trimToNull(keyword);
        Long total = hcInnerPackUnitMapper.selectFgInboundPackagePageCount(
                normalizedKeyword, normalizedShelfStatus, normalizedQualityStatus);
        if (total == null || total <= 0) {
            return PageResult.empty();
        }
        List<HcInnerPackUnitDO> boxes = hcInnerPackUnitMapper
                .selectFgInboundPackagePage(normalizedKeyword, normalizedShelfStatus,
                        normalizedQualityStatus, offset, normalizedPageSize);
        List<InboundBoxRespVO> list = buildInboundBoxResps(boxes)
                .stream()
                .filter(this::isVisibleInboundPackage)
                .toList();
        fillInboundPackageItemQualityDetails(list);
        return new PageResult<>(list, total);
    }

    @Override
    public PageResult<InboundPackageSegmentRespVO> getInboundPackageSegmentPage(String keyword, String shelfStatus,
                                                                                 String qualityStatus, Integer pageNo,
                                                                                 Integer pageSize) {
        String normalizedQualityStatus = normalizeInboundPackageQualityStatus(qualityStatus);
        Map<String, List<InboundBoxRespVO>> segmentPackagesMap = groupInboundPackages(
                keyword, shelfStatus, normalizedQualityStatus);
        if (segmentPackagesMap.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<InboundPackageSegmentRespVO> segments = segmentPackagesMap.entrySet().stream()
                .map(entry -> buildInboundPackageSegmentResp(entry.getKey(), entry.getValue()))
                .toList();
        int normalizedPageNo = pageNo == null ? 1 : Math.max(1, pageNo);
        int normalizedPageSize = pageSize == null ? 20 : Math.min(200, Math.max(1, pageSize));
        int start = Math.min((normalizedPageNo - 1) * normalizedPageSize, segments.size());
        int end = Math.min(start + normalizedPageSize, segments.size());
        return new PageResult<>(segments.subList(start, end), (long) segments.size());
    }

    @Override
    public List<InboundBoxRespVO> getInboundPackageSegmentPackageList(String keyword, String shelfStatus,
                                                                       String qualityStatus, String segmentBatchNo) {
        String normalizedSegmentBatchNo = StrUtil.trimToNull(segmentBatchNo);
        if (normalizedSegmentBatchNo == null) {
            throw invalidParamException("分段批次不能为空");
        }
        return groupInboundPackages(keyword, shelfStatus, normalizeInboundPackageQualityStatus(qualityStatus))
                .getOrDefault(normalizedSegmentBatchNo, Collections.emptyList());
    }

    private String normalizeInboundPackageShelfStatus(String shelfStatus) {
        if (INBOUND_PACKAGE_SHELF_STATUS_PENDING.equalsIgnoreCase(StrUtil.trim(shelfStatus))) {
            return INBOUND_PACKAGE_SHELF_STATUS_PENDING;
        }
        if (INBOUND_PACKAGE_SHELF_STATUS_SHELVED.equalsIgnoreCase(StrUtil.trim(shelfStatus))) {
            return INBOUND_PACKAGE_SHELF_STATUS_SHELVED;
        }
        throw invalidParamException("上架状态仅支持 PENDING 或 SHELVED");
    }

    private String normalizeInboundPackageQualityStatus(String qualityStatus) {
        String normalized = StrUtil.trimToNull(qualityStatus);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!List.of(INSPECTION_RESULT_OK, INSPECTION_RESULT_NG, QUALITY_FROZEN).contains(normalized)) {
            throw invalidParamException("包装质量状态仅支持 OK、NG 或 FROZEN");
        }
        return normalized;
    }

    private Map<String, List<InboundBoxRespVO>> groupInboundPackages(String keyword, String shelfStatus,
                                                                       String qualityStatus) {
        String normalizedShelfStatus = normalizeInboundPackageShelfStatus(shelfStatus);
        Map<String, List<InboundBoxRespVO>> result = new LinkedHashMap<>();
        List<InboundBoxRespVO> boxes = buildInboundBoxResps(
                hcInnerPackUnitMapper.selectFgInboundPackageSegmentList(
                        StrUtil.trimToNull(keyword), normalizedShelfStatus, qualityStatus));
        boxes.stream()
                .filter(this::isVisibleInboundPackage)
                .forEach(box -> {
                    String segmentBatchNo = resolveInboundPackageSegmentBatchNo(box);
                    result.computeIfAbsent(segmentBatchNo, key -> new ArrayList<>()).add(box);
                });
        return result;
    }

    @Override
    public PageResult<InboundPackageSegmentRespVO> getInboundPackedSegmentPage(InspectionSlicePageReqVO reqVO) {
        Map<String, List<InboundBoxRespVO>> segmentPackagesMap = groupInboundPackedPackages(reqVO.getKeyword());
        if (segmentPackagesMap.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L);
        }
        List<InboundPackageSegmentRespVO> segments = segmentPackagesMap.entrySet().stream()
                .map(entry -> buildInboundPackageSegmentResp(entry.getKey(), entry.getValue()))
                .toList();
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(200, Math.max(1, reqVO.getPageSize()));
        int start = Math.min((pageNo - 1) * pageSize, segments.size());
        int end = Math.min(start + pageSize, segments.size());
        return new PageResult<>(segments.subList(start, end), (long) segments.size());
    }

    @Override
    public List<InboundBoxRespVO> getInboundPackedSegmentPackageList(InspectionSlicePageReqVO reqVO) {
        String segmentBatchNo = normalizeCoaSegmentBatchNo(reqVO.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            return Collections.emptyList();
        }
        return groupInboundPackedPackages(reqVO.getKeyword())
                .getOrDefault(segmentBatchNo, Collections.emptyList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundPackageBatchRespVO lockInboundPackage(LockInboundPackageReqVO reqVO) {
        List<Long> ids = safeIdList(reqVO.getCutRoundReportIds());
        List<Long> manualPieceIds = safeIdList(reqVO.getManualPieceIds());
        if (ids.isEmpty() && manualPieceIds.isEmpty()) {
            throw invalidParamException("请选择待包装片");
        }
        if (!ids.isEmpty() && !manualPieceIds.isEmpty()) {
            throw invalidParamException("历史补录片与正常报工片请分开包装");
        }
        assertPackagingDailyRecordsReady(LocalDate.now(BUSINESS_ZONE));
        LocalDateTime now = LocalDateTime.now();
        if (!manualPieceIds.isEmpty()) {
            return lockManualInboundPackages(reqVO, manualPieceIds, now);
        }
        return lockCutRoundInboundPackages(reqVO, ids, now);
    }

    private InboundPackageBatchRespVO lockCutRoundInboundPackages(LockInboundPackageReqVO reqVO,
                                                                   List<Long> ids,
                                                                   LocalDateTime now) {
        List<Long> distinctIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<HcCutRoundReportDO> reports = distinctIds.stream()
                .sorted()
                .map(hcCutRoundReportMapper::selectByIdForUpdate)
                .filter(Objects::nonNull)
                .toList();
        if (reports.size() != distinctIds.size()) {
            throw invalidParamException("部分待包装片不存在，请刷新后重试");
        }
        assertNoClaimedPackagingCoaSamples(SOURCE_CUT_ROUND_REPORT, distinctIds);
        validateSameInboundPackageSegment(reports);
        Map<String, CoaInspectionMeta> coaInspectionMap = buildCoaInspectionMapForCutRoundReports(reports);
        for (HcCutRoundReportDO report : reports) {
            if (!INSPECTION_STATUS_COMPLETED.equalsIgnoreCase(StrUtil.blankToDefault(report.getInspectionStatus(), ""))) {
                throw invalidParamException("片号 " + firstNotBlank(report.getProductionBatchNo(), String.valueOf(report.getId())) + " 检验未完成，不能包装");
            }
            if (!INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.trimToEmpty(report.getInspectionResult()))
                    && !INSPECTION_RESULT_NG.equalsIgnoreCase(StrUtil.trimToEmpty(report.getInspectionResult()))) {
                throw invalidParamException("片号 " + firstNotBlank(report.getProductionBatchNo(), String.valueOf(report.getId()))
                        + " 未取得有效FQC片级结论，不能包装");
            }
            if (hcInnerPackUnitItemMapper.selectByCutRoundReportId(report.getId()) != null) {
                throw invalidParamException("片号 " + report.getProductionBatchNo() + " 已生成包装单，不能重复包装");
            }
            if (hcFgShippingNoticePickItemMapper.selectActiveBySourceCutRoundReportId(report.getId()) != null) {
                throw invalidParamException("片号 " + report.getProductionBatchNo() + " 已被发货配货占用，不能重复包装");
            }
            HcFinishedStockDO stock = hcFinishedStockMapper.selectBySliceBatchNo(report.getProductionBatchNo());
            if (stock != null) {
                throw invalidParamException("片号 " + report.getProductionBatchNo() + " 已存在库存台账，不能重复包装");
            }
        }
        validateSameInboundPackageQuality(reports, coaInspectionMap);
        validateInboundPackagePieceCount(reports.size(),
                resolvePackagingPieceQualityStatus(reports.get(0), coaInspectionMap));

        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        LocalDate packageDate = now.toLocalDate();
        InboundPackageBatchRespVO response = executeWithInboundPackageNoLock(packageDate, () -> {
            List<String> packageNos = resolveInboundPackageNos(reqVO.getPackageNo(), reports.size(), packageDate);
            List<HcToolingConsumableConsumeDO> auxConsumeRecords = consumePackageAuxItems(
                    normalizeInboundPackageAuxConsumeItems(reqVO.getAuxConsumeItems()),
                    AUX_BIZ_FG_INNER_PACKAGING, buildInboundPackageBatchReference(packageNos), operatorName,
                    "成品内包装批量本次领用");
            String auxMaterialName = joinPackageAuxMaterialNames(auxConsumeRecords);
            for (int index = 0; index < reports.size(); index++) {
                HcCutRoundReportDO report = reports.get(index);
                String packageNo = packageNos.get(index);
                String qualityStatus = resolvePackagingPieceQualityStatus(report, coaInspectionMap);
                HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                        .tenantId(currentTenantId())
                        .innerUnitNo(packageNo)
                        .planId(report.getPlanId())
                        .planNo(report.getPlanNo())
                        .planOperationId(report.getPlanOperationId())
                        .packageSpec(1)
                        .targetQty(1)
                        .currentQty(1)
                        .materialCode(report.getMaterialCode())
                        .materialName(report.getMaterialName())
                        .modelCode(report.getModelCode())
                        .batchNo(firstNotBlank(report.getParentProductionBatchNo(), report.getSourceProductionBatchNo(), report.getProductionBatchNo()))
                        .packageDate(packageDate)
                        .labelNo(packageNo)
                        .unitStatus(STATUS_PACKED)
                        .printCount(0)
                        .lockUserName(operatorName)
                        .lockTime(now)
                        .backfillFlag(false)
                        .recorderName(operatorName)
                        .recorderTime(now)
                        .remark(reqVO.getRemark())
                        .extraJson(JsonUtils.toJsonString(Map.of(
                                "packagingConsumableName", auxMaterialName,
                                "source", "INSPECTION_COMPLETED")))
                        .build();
                hcInnerPackUnitMapper.insert(box);
                hcInnerPackUnitItemMapper.insert(HcInnerPackUnitItemDO.builder()
                        .tenantId(box.getTenantId())
                        .innerUnitId(box.getId())
                        .innerUnitNo(box.getInnerUnitNo())
                        .planId(report.getPlanId())
                        .planNo(report.getPlanNo())
                        .planOperationId(report.getPlanOperationId())
                        .sourceType(SOURCE_CUT_ROUND_REPORT)
                        .sourceCutRoundReportId(report.getId())
                        .sliceBatchNo(report.getProductionBatchNo())
                        .productionBatchNo(report.getProductionBatchNo())
                        .qualityStatus(qualityStatus)
                        .scanUserName(operatorName)
                        .scanTime(now)
                        .build());
                hcPackagingPieceEventLogService.recordCutRoundEvent(report, "PACKAGING_NG_EXIT",
                        MANUAL_PIECE_WAIT_PACKAGING, MANUAL_PIECE_PACKED, now, operatorName, packageNo,
                        "不合格待包装片完成内包装");
            }
            return buildInboundPackageBatchResp(packageNos);
        });
        registerProductNcrAutoCompleteAfterPackaging(reports.stream()
                .map(HcCutRoundReportDO::getProductionBatchNo)
                .toList());
        return response;
    }

    private InboundPackageBatchRespVO lockManualInboundPackages(LockInboundPackageReqVO reqVO,
                                                                 List<Long> manualPieceIds,
                                                                 LocalDateTime now) {
        List<HcPackagingManualPieceDO> pieces = manualPieceIds.stream()
                .sorted()
                .map(hcPackagingManualPieceMapper::selectByIdForUpdate)
                .filter(Objects::nonNull)
                .toList();
        if (pieces.size() != manualPieceIds.size()) {
            throw invalidParamException("部分历史待包装片不存在，请刷新后重试");
        }
        assertNoClaimedPackagingCoaSamples(SOURCE_MANUAL_HISTORY, manualPieceIds);
        Set<String> segments = new HashSet<>();
        Set<String> products = new HashSet<>();
        Set<String> qualities = new HashSet<>();
        Set<String> coaSegments = new HashSet<>();
        for (HcPackagingManualPieceDO piece : pieces) {
            String segmentBatchNo = normalizeCoaSegmentBatchNo(piece.getSegmentBatchNo());
            if (StrUtil.isNotBlank(segmentBatchNo)) {
                coaSegments.add(segmentBatchNo);
            }
        }
        Map<String, CoaInspectionMeta> coaInspectionMap = buildCoaInspectionMap(coaSegments);
        for (HcPackagingManualPieceDO piece : pieces) {
            if (!MANUAL_PIECE_WAIT_PACKAGING.equals(piece.getRecordStatus())) {
                throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 当前状态不允许包装");
            }
            if (hcInnerPackUnitItemMapper.selectByManualPieceId(piece.getId()) != null
                    || hcInnerPackUnitItemMapper.selectBySliceBatchNo(piece.getSliceBatchNo()) != null) {
                throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 已生成包装单，不能重复包装");
            }
            if (hcFinishedStockMapper.selectBySliceBatchNo(piece.getSliceBatchNo()) != null) {
                throw invalidParamException("片号 " + piece.getSliceBatchNo() + " 已存在成品库存，不能重复包装");
            }
            segments.add(StrUtil.blankToDefault(piece.getSegmentBatchNo(), "-"));
            products.add(piece.getMaterialCode() + "|" + piece.getModelCode());
            qualities.add(resolveManualPackagingQuality(piece,
                    coaInspectionMap.get(normalizeCoaSegmentBatchNo(piece.getSegmentBatchNo()))));
        }
        if (segments.size() != 1) {
            throw invalidParamException("不同分段的历史片不能包装在一起");
        }
        if (products.size() != 1) {
            throw invalidParamException("不同产品料号或型号的历史片不能包装在一起");
        }
        if (qualities.size() != 1) {
            throw invalidParamException("合格与不合格历史片不能包装在一起");
        }
        validateInboundPackagePieceCount(pieces.size(), qualities.iterator().next());
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        LocalDate packageDate = now.toLocalDate();
        InboundPackageBatchRespVO response = executeWithInboundPackageNoLock(packageDate, () -> {
            List<String> packageNos = resolveInboundPackageNos(reqVO.getPackageNo(), pieces.size(), packageDate);
            List<HcToolingConsumableConsumeDO> auxConsumeRecords = consumePackageAuxItems(
                    normalizeInboundPackageAuxConsumeItems(reqVO.getAuxConsumeItems()),
                    AUX_BIZ_FG_INNER_PACKAGING, buildInboundPackageBatchReference(packageNos), operatorName,
                    "历史成品内包装批量本次领用");
            String auxMaterialName = joinPackageAuxMaterialNames(auxConsumeRecords);
            for (int index = 0; index < pieces.size(); index++) {
                HcPackagingManualPieceDO piece = pieces.get(index);
                String packageNo = packageNos.get(index);
                HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                        .tenantId(currentTenantId())
                        .innerUnitNo(packageNo)
                        .packageSpec(1)
                        .targetQty(1)
                        .currentQty(1)
                        .materialCode(piece.getMaterialCode())
                        .materialName(piece.getMaterialName())
                        .modelCode(piece.getModelCode())
                        .batchNo(piece.getSegmentBatchNo())
                        .productSize(piece.getProductSize())
                        .packageDate(packageDate)
                        .labelNo(packageNo)
                        .unitStatus(STATUS_PACKED)
                        .printCount(0)
                        .lockUserName(operatorName)
                        .lockTime(now)
                        .backfillFlag(true)
                        .backfillReason(piece.getBackfillReason())
                        .recorderName(operatorName)
                        .recorderTime(now)
                        .remark(reqVO.getRemark())
                        .extraJson(JsonUtils.toJsonString(Map.of(
                                "packagingConsumableName", auxMaterialName,
                                "source", SOURCE_MANUAL_HISTORY)))
                        .build();
                hcInnerPackUnitMapper.insert(box);
                hcInnerPackUnitItemMapper.insert(HcInnerPackUnitItemDO.builder()
                        .tenantId(box.getTenantId())
                        .innerUnitId(box.getId())
                        .innerUnitNo(box.getInnerUnitNo())
                        .sourceType(SOURCE_MANUAL_HISTORY)
                        .sourceManualPieceId(piece.getId())
                        .sliceBatchNo(piece.getSliceBatchNo())
                        .productionBatchNo(piece.getSliceBatchNo())
                        .qualityStatus(resolveManualPackagingQuality(piece, coaInspectionMap.get(normalizeCoaSegmentBatchNo(piece.getSegmentBatchNo()))))
                        .scanUserName(operatorName)
                        .scanTime(now)
                        .build());
                HcPackagingManualPieceDO update = new HcPackagingManualPieceDO();
                update.setId(piece.getId());
                update.setRecordStatus(MANUAL_PIECE_PACKED);
                update.setInnerUnitId(box.getId());
                update.setInnerUnitNo(box.getInnerUnitNo());
                hcPackagingManualPieceMapper.updateById(update);
                hcPackagingPieceEventLogService.recordManualPieceEvent(piece, "PACKAGING_NG_EXIT",
                        MANUAL_PIECE_WAIT_PACKAGING, MANUAL_PIECE_PACKED, now, operatorName, packageNo,
                        "不合格待包装片完成内包装");
            }
            return buildInboundPackageBatchResp(packageNos);
        });
        registerProductNcrAutoCompleteAfterPackaging(pieces.stream()
                .map(HcPackagingManualPieceDO::getSliceBatchNo)
                .toList());
        return response;
    }

    private void registerProductNcrAutoCompleteAfterPackaging(Collection<String> pieceNos) {
        List<String> normalizedPieceNos = normalizePieceNos(pieceNos);
        if (normalizedPieceNos.isEmpty()) {
            return;
        }
        Long tenantId = TenantContextHolder.getTenantId();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            runProductNcrAutoCompleteAfterPackaging(tenantId, normalizedPieceNos);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                runProductNcrAutoCompleteAfterPackaging(tenantId, normalizedPieceNos);
            }
        });
    }

    private void runProductNcrAutoCompleteAfterPackaging(Long tenantId, List<String> pieceNos) {
        try {
            if (tenantId == null) {
                qmsNcRecordService.autoCompleteProductNcrAfterPackaging(pieceNos);
            } else {
                TenantUtils.execute(tenantId, () -> qmsNcRecordService.autoCompleteProductNcrAfterPackaging(pieceNos));
            }
        } catch (Exception ex) {
            log.warn("[runProductNcrAutoCompleteAfterPackaging][pieceNos({}) 包装完成后自动推进产品NCR失败]",
                    pieceNos, ex);
        }
    }

    private List<String> normalizePieceNos(Collection<String> pieceNos) {
        if (pieceNos == null || pieceNos.isEmpty()) {
            return List.of();
        }
        return pieceNos.stream()
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
    }

    private List<Long> safeIdList(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> id > 0)
                .distinct()
                .toList();
    }

    /**
     * 确认包装时保留操作员填写的本次领用量，并在后续扣减前按锁定台账复核实时余额。
     */
    private List<PackageAuxConsumeItemReqVO> normalizeInboundPackageAuxConsumeItems(
            List<PackageAuxConsumeItemReqVO> consumeItems) {
        if (consumeItems == null || consumeItems.isEmpty()) {
            throw invalidParamException("请至少选择一条包装辅材库存批次");
        }
        Set<Long> ledgerIds = new HashSet<>();
        List<PackageAuxConsumeItemReqVO> normalizedItems = new ArrayList<>();
        for (PackageAuxConsumeItemReqVO consumeItem : consumeItems) {
            if (consumeItem == null || consumeItem.getLedgerId() == null) {
                throw invalidParamException("包装辅材库存批次不能为空");
            }
            if (!ledgerIds.add(consumeItem.getLedgerId())) {
                throw invalidParamException("包装辅材库存批次不能重复选择");
            }
            BigDecimal consumeQty = positiveDecimal(consumeItem.getConsumeQty(), "包装辅材本次领用量必须大于0");
            PackageAuxConsumeItemReqVO normalizedItem = new PackageAuxConsumeItemReqVO();
            normalizedItem.setLedgerId(consumeItem.getLedgerId());
            normalizedItem.setConsumeQty(consumeQty);
            normalizedItems.add(normalizedItem);
        }
        return normalizedItems;
    }

    private InboundPackageBatchRespVO buildInboundPackageBatchResp(List<String> packageNos) {
        InboundPackageBatchRespVO respVO = new InboundPackageBatchRespVO();
        respVO.setPackageNos(packageNos);
        respVO.setPackageCount(packageNos.size());
        respVO.setPieceCount(packageNos.size());
        return respVO;
    }

    private String buildInboundPackageBatchReference(List<String> packageNos) {
        if (packageNos.size() == 1) {
            return packageNos.get(0);
        }
        return packageNos.get(0) + "~" + packageNos.get(packageNos.size() - 1)
                + "（共" + packageNos.size() + "包）";
    }

    /**
     * 锁覆盖“取最大流水号 + 创建包装单”整个区间，避免并发批量包装生成相同编号。
     */
    private <T> T executeWithInboundPackageNoLock(LocalDate packageDate, Supplier<T> action) {
        String lockName = "mes:fg:inbound-package:" + packageDate.format(DateTimeFormatter.BASIC_ISO_DATE);
        Integer lockResult = hcInnerPackUnitMapper.tryAcquireInboundPackageNoLock(
                lockName, INBOUND_PACKAGE_NO_LOCK_TIMEOUT_SECONDS);
        if (!Integer.valueOf(1).equals(lockResult)) {
            throw invalidParamException("包装编号生成繁忙，请稍后重试");
        }
        try {
            return action.get();
        } finally {
            hcInnerPackUnitMapper.releaseInboundPackageNoLock(lockName);
        }
    }

    private List<String> resolveInboundPackageNos(String requestedPackageNo, int pieceCount, LocalDate packageDate) {
        String packageNo = StrUtil.trimToEmpty(requestedPackageNo);
        if (pieceCount > 1 && StrUtil.isNotBlank(packageNo)) {
            throw invalidParamException("批量包装由系统自动生成连续包装编号，请勿手工填写");
        }
        if (StrUtil.isNotBlank(packageNo)) {
            if (hcInnerPackUnitMapper.selectByUnitNo(packageNo) != null) {
                throw invalidParamException("包装编号已存在，请重新填写");
            }
            return List.of(packageNo);
        }
        return nextInboundPackageNos(packageDate, pieceCount);
    }

    private void assertPackagingDailyRecordsReady(LocalDate recordDate) {
        List<String> missingRecords = new ArrayList<>();
        if (!hcProcessFormRecordMapper.existsByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_STARTUP_FORM_TYPE, recordDate)) {
            missingRecords.add("包装开机点检");
        }
        if (!hcProcessFormRecordMapper.existsByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_CLEANING_FORM_TYPE, recordDate)) {
            missingRecords.add("包装清洁保养");
        }
        if (!missingRecords.isEmpty()) {
            throw invalidParamException("今天尚未填写" + String.join("、", missingRecords) + "记录，不能确认包装");
        }
    }

    private void assertShippingPackagingDailyRecordsReady(LocalDate recordDate) {
        List<String> missingRecords = new ArrayList<>();
        if (!hcProcessFormRecordMapper.existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_STARTUP_FORM_TYPE, recordDate)) {
            missingRecords.add("包装开机点检");
        }
        if (!hcProcessFormRecordMapper.existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_CLEANING_FORM_TYPE, recordDate)) {
            missingRecords.add("包装清洁保养");
        }
        if (!hcProcessFormRecordMapper.existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_INNER_FORM_TYPE, recordDate)) {
            missingRecords.add("内包装点检");
        }
        if (!hcProcessFormRecordMapper.existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_OUTER_FORM_TYPE, recordDate)) {
            missingRecords.add("外包装点检");
        }
        if (!missingRecords.isEmpty()) {
            throw invalidParamException("今天尚未完成" + String.join("、", missingRecords)
                    + "，四类点检均须填写、确认且合格后才能进行发货包装");
        }
    }

    /**
     * OQC 发生在外包装前，只校验开机、清洁和内包装三个前置点检；外包装点检在实际外包装时校验。
     */
    private void assertShippingInnerPackagingDailyRecordsReady(LocalDate recordDate) {
        List<String> missingRecords = new ArrayList<>();
        if (!hcProcessFormRecordMapper.existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_STARTUP_FORM_TYPE, recordDate)) {
            missingRecords.add("包装开机点检");
        }
        if (!hcProcessFormRecordMapper.existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_CLEANING_FORM_TYPE, recordDate)) {
            missingRecords.add("包装清洁保养");
        }
        if (!hcProcessFormRecordMapper.existsConfirmedOkByProcessCodeAndFormTypeAndRecordDate(
                PACKAGING_PROCESS_CODE, PACKAGING_INNER_FORM_TYPE, recordDate)) {
            missingRecords.add("内包装点检");
        }
        if (!missingRecords.isEmpty()) {
            throw invalidParamException("今天尚未完成" + String.join("、", missingRecords)
                    + "，须填写、确认且合格后才能推送出货检验");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundBoxRespVO printInboundPackageCard(BoxActionReqVO reqVO) {
        HcInnerPackUnitDO box = getInboundBox(reqVO.getId());
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setLabelNo(firstNotBlank(box.getLabelNo(), box.getInnerUnitNo()));
        update.setPrintCount(intValue(box.getPrintCount()) + 1);
        update.setLastPrintTime(LocalDateTime.now());
        hcInnerPackUnitMapper.updateById(update);
        return buildInboundBoxResp(hcInnerPackUnitMapper.selectById(box.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundBoxRespVO updateInboundPackageRemark(InboundPackageRemarkUpdateReqVO reqVO) {
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByIdForUpdate(reqVO.getId());
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("包装盒不存在");
        }
        if (!List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED)
                .contains(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("只有待上架状态的包装单可以填写备注");
        }
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setRemark(StrUtil.trimToNull(reqVO.getRemark()));
        hcInnerPackUnitMapper.updateById(update);
        return buildInboundBoxResp(hcInnerPackUnitMapper.selectById(box.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long cancelInboundPackageLock(BoxActionReqVO reqVO) {
        return cancelInboundPackageLock(reqVO.getId(), reqVO.getOperatorName(), reqVO.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundPackageBatchActionRespVO cancelInboundPackageLocks(CancelInboundPackageBatchReqVO reqVO) {
        List<Long> packageIds = normalizeInboundPackageBatchIds(reqVO.getPackageIds());
        List<String> packageNos = new ArrayList<>();
        for (Long packageId : packageIds) {
            HcInnerPackUnitDO box = getInboundBox(packageId);
            packageNos.add(box.getInnerUnitNo());
            cancelInboundPackageLock(packageId, reqVO.getOperatorName(), reqVO.getReason());
        }
        return buildInboundPackageBatchActionResp(packageNos);
    }

    /**
     * 撤销单个待上架包装；由单条和批量入口共用，批量入口依赖外围事务保证原子性。
     */
    private Long cancelInboundPackageLock(Long packageId, String operatorName, String reason) {
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByIdForUpdate(packageId);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("包装盒不存在");
        }
        if (!List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED).contains(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("只有待上架状态的包装单可以撤销包装");
        }
        List<HcInnerPackUnitItemDO> items = hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId());
        List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectListByInnerUnitNo(box.getInnerUnitNo());
        if (stocks.stream().anyMatch(stock -> !STATUS_INBOUND_LOCKED.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), "")))) {
            throw invalidParamException("当前包装单已有片进入上架或出库流程，不能撤销包装");
        }
        if (!stocks.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            String actualOperatorName = firstNotBlank(operatorName, currentUserName());
            for (HcFinishedStockDO stock : stocks) {
                recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, "CANCELLED"),
                        "FG_PACKAGING_CANCEL", now, actualOperatorName, "FG_INBOUND_PACKAGE", box.getId(),
                        box.getInnerUnitNo(), reason);
            }
            hcFinishedStockMapper.deletePhysicallyByIds(stocks.stream().map(HcFinishedStockDO::getId).toList());
        }
        for (HcInnerPackUnitItemDO item : items) {
            if (item.getSourceManualPieceId() == null) {
                continue;
            }
            HcPackagingManualPieceDO manualPiece = hcPackagingManualPieceMapper.selectByIdForUpdate(item.getSourceManualPieceId());
            if (manualPiece == null) {
                continue;
            }
            hcPackagingManualPieceMapper.resetPackagingStatus(manualPiece.getId());
        }
        if (!items.isEmpty()) {
            hcInnerPackUnitItemMapper.deletePhysicallyByIds(items.stream().map(HcInnerPackUnitItemDO::getId).toList());
        }
        hcInnerPackUnitMapper.deletePhysicallyById(box.getId());
        refreshLocationOccupied(box.getLocationCode());
        return box.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundBoxRespVO confirmInboundPackage(ConfirmInboundPackageReqVO reqVO) {
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(StrUtil.trimToEmpty(reqVO.getPackageNo()));
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("包装编号不存在");
        }
        if (!List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED).contains(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("只有待上架状态的包装单可以确认上架");
        }
        List<HcInnerPackUnitItemDO> items = hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId());
        if (items.isEmpty()) {
            throw invalidParamException("当前包装单没有片号，不能确认上架");
        }
        if (StrUtil.isBlank(reqVO.getLocationCode()) && StrUtil.isBlank(box.getLocationCode())) {
            throw invalidParamException("请选择待上架库位");
        }
        String oldLocationCode = box.getLocationCode();
        HcLocationDO location = StrUtil.isBlank(reqVO.getLocationCode())
                ? getFgLocationByCodeForUpdate(box.getLocationCode())
                : getFgLocationByCodeForUpdate(reqVO.getLocationCode());

        List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectListByInnerUnitNo(box.getInnerUnitNo());
        int inboundPieceCount = stocks.isEmpty() ? items.size() : (int) stocks.stream()
                .filter(stock -> STATUS_INBOUND_LOCKED.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), "")))
                .mapToLong(this::stockPieceQty)
                .sum();
        if (!stocks.isEmpty() && inboundPieceCount <= 0) {
            throw invalidParamException("当前包装单没有可上架库存；若片号已发货锁定，请先到发货配货页面退回配货");
        }
        if (!Objects.equals(oldLocationCode, location.getLocationCode())) {
            assertInboundLocationQualityScope(location, resolvePackageQualityStatus(items));
            ensureLocationCapacity(location, inboundPieceCount);
        } else {
            assertInboundLocationQualityScope(location, resolvePackageQualityStatus(items));
        }

        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        String inboundNo = "FGI-" + box.getInnerUnitNo();
        HcFgInboundOrderDO inboundOrder = hcFgInboundOrderMapper.selectByInboundNo(inboundNo);
        if (inboundOrder == null) {
            inboundOrder = HcFgInboundOrderDO.builder()
                    .tenantId(box.getTenantId())
                    .inboundNo(inboundNo)
                    .planId(box.getPlanId())
                    .planNo(box.getPlanNo())
                    .planOperationId(box.getPlanOperationId())
                    .inboundStatus(STATUS_INBOUNDED)
                    .totalBoxCount(1)
                    .totalPieceCount(inboundPieceCount)
                    .warehouseCode(location.getWarehouseCode())
                    .warehouseName(location.getWarehouseName())
                    .locationCode(location.getLocationCode())
                    .locationName(location.getLocationName())
                    .recorderName(operatorName)
                    .recorderTime(now)
                    .inboundUserName(operatorName)
                    .inboundTime(now)
                    .remark(reqVO.getRemark())
                    .build();
            hcFgInboundOrderMapper.insert(inboundOrder);
            hcFgInboundOrderItemMapper.insert(HcFgInboundOrderItemDO.builder()
                    .tenantId(box.getTenantId())
                    .inboundOrderId(inboundOrder.getId())
                    .inboundNo(inboundNo)
                    .outerBoxId(box.getId())
                    .outerBoxNo(box.getInnerUnitNo())
                    .pieceQty(inboundPieceCount)
                    .materialCode(box.getMaterialCode())
                    .modelCode(box.getModelCode())
                    .batchNo(box.getBatchNo())
                    .qualityStatus(resolvePackageQualityStatus(items))
                    .build());
        } else {
            HcFgInboundOrderDO inboundOrderUpdate = new HcFgInboundOrderDO();
            inboundOrderUpdate.setId(inboundOrder.getId());
            inboundOrderUpdate.setInboundStatus(STATUS_INBOUNDED);
            inboundOrderUpdate.setTotalBoxCount(1);
            inboundOrderUpdate.setTotalPieceCount(inboundPieceCount);
            inboundOrderUpdate.setWarehouseCode(location.getWarehouseCode());
            inboundOrderUpdate.setWarehouseName(location.getWarehouseName());
            inboundOrderUpdate.setLocationCode(location.getLocationCode());
            inboundOrderUpdate.setLocationName(location.getLocationName());
            inboundOrderUpdate.setInboundUserName(operatorName);
            inboundOrderUpdate.setInboundTime(now);
            inboundOrderUpdate.setRemark(firstNotBlank(reqVO.getRemark(), inboundOrder.getRemark()));
            hcFgInboundOrderMapper.updateById(inboundOrderUpdate);
        }

        if (stocks.isEmpty()) {
            for (HcInnerPackUnitItemDO item : items) {
                LocalDate productionDate = resolvePackagingItemProductionDate(item);
                LocalDate expiryDate = resolvePackagingItemExpiryDate(item, productionDate);
                HcFinishedStockDO stock = HcFinishedStockDO.builder()
                        .tenantId(box.getTenantId())
                        .stockNo(nextNo("FGS"))
                        .outerBoxNo(box.getInnerUnitNo())
                        .innerUnitNo(box.getInnerUnitNo())
                        .sliceBatchNo(firstNotBlank(item.getSliceBatchNo(), item.getProductionBatchNo()))
                        .materialCode(box.getMaterialCode())
                        .materialName(box.getMaterialName())
                        .modelCode(box.getModelCode())
                        .batchNo(box.getBatchNo())
                        .productSize(box.getProductSize())
                        .productionDate(productionDate)
                        .expiryDate(expiryDate)
                        .qty(1)
                        .qualityStatus(firstNotBlank(item.getQualityStatus(), INSPECTION_RESULT_OK))
                        .warehouseCode(location.getWarehouseCode())
                        .warehouseName(location.getWarehouseName())
                        .locationCode(location.getLocationCode())
                        .locationName(location.getLocationName())
                        .stockStatus(STATUS_AVAILABLE)
                        .inboundNo(inboundNo)
                        .inboundTime(now)
                        .build();
                hcFinishedStockMapper.insert(stock);
                synchronizeStockCoaFreeze(stock);
                recordFinishedStockHistory(null, stock, "FG_INBOUND", now, operatorName,
                        "FG_INBOUND_ORDER", inboundOrder.getId(), inboundNo, reqVO.getRemark());
            }
        } else {
            List<HcFinishedStockDO> shelfableStocks = stocks.stream()
                    .filter(stock -> STATUS_INBOUND_LOCKED.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), "")))
                    .toList();
            if (shelfableStocks.isEmpty()) {
                throw invalidParamException("当前包装单没有可上架库存；若片号已发货锁定，请先到发货配货页面退回配货");
            }
            for (HcFinishedStockDO stock : stocks) {
                if (!STATUS_INBOUND_LOCKED.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), ""))) {
                    continue;
                }
                HcFinishedStockDO update = new HcFinishedStockDO();
                update.setId(stock.getId());
                update.setWarehouseCode(location.getWarehouseCode());
                update.setWarehouseName(location.getWarehouseName());
                update.setLocationCode(location.getLocationCode());
                update.setLocationName(location.getLocationName());
                update.setStockStatus(STATUS_AVAILABLE);
                update.setInboundNo(inboundNo);
                update.setInboundTime(now);
                hcFinishedStockMapper.updateById(update);
                HcFinishedStockDO afterStock = copyFinishedStockForHistory(stock, STATUS_AVAILABLE);
                afterStock.setWarehouseCode(location.getWarehouseCode());
                afterStock.setWarehouseName(location.getWarehouseName());
                afterStock.setLocationCode(location.getLocationCode());
                afterStock.setLocationName(location.getLocationName());
                afterStock.setInboundNo(inboundNo);
                afterStock.setInboundTime(now);
                recordFinishedStockHistory(stock, afterStock, "FG_INBOUND", now, operatorName,
                        "FG_INBOUND_ORDER", inboundOrder.getId(), inboundNo, reqVO.getRemark());
            }
        }

        for (HcInnerPackUnitItemDO item : items) {
            if (item.getSourceManualPieceId() == null) {
                continue;
            }
            HcPackagingManualPieceDO manualPieceUpdate = new HcPackagingManualPieceDO();
            manualPieceUpdate.setId(item.getSourceManualPieceId());
            manualPieceUpdate.setRecordStatus(MANUAL_PIECE_INBOUNDED);
            hcPackagingManualPieceMapper.updateById(manualPieceUpdate);
        }

        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setWarehouseCode(location.getWarehouseCode());
        update.setWarehouseName(location.getWarehouseName());
        update.setLocationCode(location.getLocationCode());
        update.setLocationName(location.getLocationName());
        update.setUnitStatus(STATUS_INBOUNDED);
        update.setInboundUserName(operatorName);
        update.setInboundTime(now);
        update.setActualWorkTime(now);
        hcInnerPackUnitMapper.updateById(update);
        refreshLocationOccupied(oldLocationCode);
        refreshLocationOccupied(location.getLocationCode());
        return buildInboundBoxResp(hcInnerPackUnitMapper.selectById(box.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundPackageBatchActionRespVO confirmInboundPackages(ConfirmInboundPackageBatchReqVO reqVO) {
        List<Long> packageIds = normalizeInboundPackageBatchIds(reqVO.getPackageIds());
        List<String> packageNos = new ArrayList<>();
        for (Long packageId : packageIds) {
            HcInnerPackUnitDO box = getInboundBox(packageId);
            ConfirmInboundPackageReqVO itemReqVO = new ConfirmInboundPackageReqVO();
            itemReqVO.setPackageNo(box.getInnerUnitNo());
            itemReqVO.setLocationCode(reqVO.getLocationCode());
            itemReqVO.setOperatorName(reqVO.getOperatorName());
            itemReqVO.setRemark(reqVO.getRemark());
            packageNos.add(confirmInboundPackage(itemReqVO).getBoxNo());
        }
        return buildInboundPackageBatchActionResp(packageNos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundBoxRespVO downShelfInboundPackage(BoxActionReqVO reqVO) {
        HcInnerPackUnitDO box = getInboundBox(reqVO.getId());
        if (!STATUS_INBOUNDED.equalsIgnoreCase(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("只有已上架包装单可以下架");
        }
        List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectListByInnerUnitNo(box.getInnerUnitNo());
        if (stocks.isEmpty()) {
            throw invalidParamException("当前包装单没有库存台账，不能下架");
        }
        if (stocks.stream().anyMatch(stock -> !STATUS_AVAILABLE.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), "")))) {
            throw invalidParamException("当前包装单存在发货锁定或已出库片，不能下架移库");
        }
        String oldLocationCode = box.getLocationCode();
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        for (HcFinishedStockDO stock : stocks) {
            HcFinishedStockDO update = new HcFinishedStockDO();
            update.setId(stock.getId());
            update.setWarehouseCode("");
            update.setWarehouseName("");
            update.setLocationCode("");
            update.setLocationName("");
            update.setStockStatus(STATUS_INBOUND_LOCKED);
            hcFinishedStockMapper.updateById(update);
            recordFinishedStockHistory(stock, copyFinishedStockOffShelf(stock, STATUS_INBOUND_LOCKED),
                    "FG_UNSHELF", now, operatorName, "FG_INBOUND_PACKAGE", box.getId(),
                    box.getInnerUnitNo(), reqVO.getReason());
        }
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setWarehouseCode("");
        update.setWarehouseName("");
        update.setLocationCode("");
        update.setLocationName("");
        update.setUnitStatus(STATUS_INBOUND_LOCKED);
        update.setInboundUserName(operatorName);
        update.setInboundTime(now);
        update.setRemark(firstNotBlank(reqVO.getReason(), box.getRemark()));
        hcInnerPackUnitMapper.updateById(update);
        refreshLocationOccupied(oldLocationCode);
        return buildInboundBoxResp(hcInnerPackUnitMapper.selectById(box.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundPackageBatchActionRespVO downShelfInboundPackages(DownShelfInboundPackageBatchReqVO reqVO) {
        List<Long> packageIds = normalizeInboundPackageBatchIds(reqVO.getPackageIds());
        List<String> packageNos = new ArrayList<>();
        for (Long packageId : packageIds) {
            BoxActionReqVO itemReqVO = new BoxActionReqVO();
            itemReqVO.setId(packageId);
            itemReqVO.setOperatorName(reqVO.getOperatorName());
            itemReqVO.setReason(reqVO.getReason());
            packageNos.add(downShelfInboundPackage(itemReqVO).getBoxNo());
        }
        return buildInboundPackageBatchActionResp(packageNos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundBoxRespVO manualOutboundInboundPackage(ManualOutboundPackageReqVO reqVO) {
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByIdForUpdate(reqVO.getId());
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("包装单不存在");
        }
        if (!STATUS_INBOUNDED.equalsIgnoreCase(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("只有已上架包装单可以手工出库");
        }
        List<HcInnerPackUnitItemDO> packageItems = hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId());
        if (INSPECTION_RESULT_NG.equals(resolvePackageQualityStatus(packageItems))) {
            throw invalidParamException("不合格成品不允许从正常成品库手工出库；请先下架后按不合格品受控处置流程办理");
        }
        List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo());
        if (stocks.isEmpty()) {
            throw invalidParamException("当前包装单没有库存台账，不能手工出库");
        }
        HcFinishedStockDO unavailableStock = stocks.stream()
                .filter(stock -> !STATUS_AVAILABLE.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), "")))
                .findFirst()
                .orElse(null);
        if (unavailableStock != null) {
            throw invalidParamException("片号 " + firstNotBlank(unavailableStock.getSliceBatchNo(), unavailableStock.getStockNo())
                    + " 当前不是可用在库状态，不能手工出库");
        }

        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        for (HcFinishedStockDO stock : stocks) {
            HcFinishedStockDO update = new HcFinishedStockDO();
            update.setId(stock.getId());
            update.setStockStatus(STATUS_SHIPPED);
            hcFinishedStockMapper.updateById(update);
            recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, STATUS_SHIPPED),
                    "FG_MANUAL_OUTBOUND", now, operatorName, "FG_INBOUND_PACKAGE", box.getId(),
                    box.getInnerUnitNo(), reqVO.getReason());
        }

        HcInnerPackUnitDO boxUpdate = new HcInnerPackUnitDO();
        boxUpdate.setId(box.getId());
        boxUpdate.setUnitStatus(STATUS_MANUAL_OUTBOUNDED);
        hcInnerPackUnitMapper.updateById(boxUpdate);
        refreshLocationOccupied(box.getLocationCode());
        return buildInboundBoxResp(hcInnerPackUnitMapper.selectById(box.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundPackageBatchActionRespVO manualOutboundInboundPackages(ManualOutboundPackageBatchReqVO reqVO) {
        List<Long> packageIds = normalizeInboundPackageBatchIds(reqVO.getPackageIds());
        List<String> packageNos = new ArrayList<>();
        for (Long packageId : packageIds) {
            ManualOutboundPackageReqVO itemReqVO = new ManualOutboundPackageReqVO();
            itemReqVO.setId(packageId);
            itemReqVO.setOperatorName(reqVO.getOperatorName());
            itemReqVO.setReason(reqVO.getReason());
            packageNos.add(manualOutboundInboundPackage(itemReqVO).getBoxNo());
        }
        return buildInboundPackageBatchActionResp(packageNos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundPackageBatchActionRespVO manualOutboundPendingInboundPackages(ManualOutboundPackageBatchReqVO reqVO) {
        List<Long> packageIds = normalizeInboundPackageBatchIds(reqVO.getPackageIds());
        List<String> packageNos = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        for (Long packageId : packageIds) {
            packageNos.add(manualOutboundPendingInboundPackage(packageId, reqVO.getReason(), now, operatorName));
        }
        return buildInboundPackageBatchActionResp(packageNos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InboundPackageBatchActionRespVO directOutboundPendingQualifiedInboundPackages(
            DirectOutboundPendingPackageBatchReqVO reqVO) {
        List<Long> packageIds = normalizeInboundPackageBatchIds(reqVO.getPackageIds());
        List<String> packageNos = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        for (Long packageId : packageIds) {
            packageNos.add(directOutboundPendingQualifiedInboundPackage(packageId, reqVO.getReason(), now, operatorName));
        }
        return buildInboundPackageBatchActionResp(packageNos);
    }

    /**
     * 待上架包装尚未形成可用库存，或仅保留了下架锁定库存；此处只允许不合格包装整盒出库，
     * 避免改变常规成品入库页“已上架方可手工出库”的业务语义。
     */
    private String manualOutboundPendingInboundPackage(Long packageId, String reason, LocalDateTime now,
                                                       String operatorName) {
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByIdForUpdate(packageId);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("包装单不存在或已失效，请刷新后重试");
        }
        String boxStatus = StrUtil.blankToDefault(box.getUnitStatus(), "");
        if (!List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED).contains(boxStatus)
                || StrUtil.isNotBlank(box.getLocationCode())) {
            throw invalidParamException("只有未上架的待上架不合格包装单可以手工出库：" + box.getInnerUnitNo());
        }
        List<HcInnerPackUnitItemDO> items = hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId());
        if (items.isEmpty()) {
            throw invalidParamException("当前包装单没有片号，不能手工出库：" + box.getInnerUnitNo());
        }
        if (items.stream().anyMatch(item -> !INSPECTION_RESULT_NG.equalsIgnoreCase(
                StrUtil.blankToDefault(item.getQualityStatus(), "")))) {
            throw invalidParamException("包装单包含非不合格片，不能从不合格品库存整盒出库：" + box.getInnerUnitNo());
        }
        List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo());
        if (STATUS_PACKED.equals(boxStatus) && !stocks.isEmpty()) {
            throw invalidParamException("待上架包装单存在异常库存台账，请先核对后再出库：" + box.getInnerUnitNo());
        }
        if (STATUS_INBOUND_LOCKED.equals(boxStatus)) {
            if (stocks.isEmpty() || stocks.size() != items.size()) {
                throw invalidParamException("下架待重新上架包装单的库存台账不完整，不能手工出库：" + box.getInnerUnitNo());
            }
            for (HcFinishedStockDO stock : stocks) {
                if (!STATUS_INBOUND_LOCKED.equals(StrUtil.blankToDefault(stock.getStockStatus(), ""))
                        || StrUtil.isNotBlank(stock.getLocationCode())) {
                    throw invalidParamException("包装单存在非待上架库存，不能从不合格品库存出库：" + box.getInnerUnitNo());
                }
                HcFinishedStockDO update = new HcFinishedStockDO();
                update.setId(stock.getId());
                update.setStockStatus(STATUS_SHIPPED);
                hcFinishedStockMapper.updateById(update);
                recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, STATUS_SHIPPED),
                        "FG_MANUAL_OUTBOUND", now, operatorName, "FG_INBOUND_PACKAGE", box.getId(),
                        box.getInnerUnitNo(), "未上架不合格成品包装出库：" + StrUtil.trimToEmpty(reason));
            }
        }
        syncPendingInboundPackageSourceOutbound(items, box, boxStatus, now, operatorName, reason,
                "PACKAGING_MANUAL_OUTBOUND", "未上架不合格成品包装出库");
        HcInnerPackUnitDO boxUpdate = new HcInnerPackUnitDO();
        boxUpdate.setId(box.getId());
        boxUpdate.setUnitStatus(STATUS_MANUAL_OUTBOUNDED);
        hcInnerPackUnitMapper.updateById(boxUpdate);
        return box.getInnerUnitNo();
    }

    /**
     * 合格品在包装完成后直接离场，不先形成可用成品库存；已下架形成的待上架库存则同步扣减。
     */
    private String directOutboundPendingQualifiedInboundPackage(Long packageId, String reason, LocalDateTime now,
                                                                String operatorName) {
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByIdForUpdate(packageId);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("包装单不存在或已失效，请刷新后重试");
        }
        String boxStatus = StrUtil.blankToDefault(box.getUnitStatus(), "");
        if (!List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED).contains(boxStatus)
                || StrUtil.isNotBlank(box.getLocationCode())) {
            throw invalidParamException("只有未上架的合格品待上架包装单可以直接出库：" + box.getInnerUnitNo());
        }
        List<HcInnerPackUnitItemDO> items = hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId());
        if (items.isEmpty()) {
            throw invalidParamException("当前包装单没有片号，不能直接出库：" + box.getInnerUnitNo());
        }
        if (items.stream().anyMatch(item -> !INSPECTION_RESULT_OK.equalsIgnoreCase(
                StrUtil.blankToDefault(item.getQualityStatus(), "")))) {
            throw invalidParamException("包装单包含非合格片，不能直接出库：" + box.getInnerUnitNo());
        }

        List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo());
        String outboundRemark = "合格品待上架包装直接出库：" + StrUtil.trimToEmpty(reason);
        if (STATUS_PACKED.equals(boxStatus)) {
            if (!stocks.isEmpty()) {
                throw invalidParamException("待上架包装单存在异常库存台账，请先核对后再直接出库：" + box.getInnerUnitNo());
            }
            for (HcInnerPackUnitItemDO item : items) {
                recordFinishedStockHistory(null, buildPendingQualifiedDirectOutboundSnapshot(box, item),
                        "FG_PACKAGE_DIRECT_OUTBOUND", now, operatorName, "FG_INBOUND_PACKAGE", box.getId(),
                        box.getInnerUnitNo(), outboundRemark);
            }
        } else {
            if (stocks.isEmpty() || stocks.size() != items.size()) {
                throw invalidParamException("下架待重新上架包装单的库存台账不完整，不能直接出库：" + box.getInnerUnitNo());
            }
            for (HcFinishedStockDO stock : stocks) {
                if (!STATUS_INBOUND_LOCKED.equals(StrUtil.blankToDefault(stock.getStockStatus(), ""))
                        || StrUtil.isNotBlank(stock.getLocationCode())
                        || items.stream().noneMatch(item -> matchesPackageItemStock(item, stock))) {
                    throw invalidParamException("包装单存在非待上架库存，不能直接出库：" + box.getInnerUnitNo());
                }
                HcFinishedStockDO update = new HcFinishedStockDO();
                update.setId(stock.getId());
                update.setStockStatus(STATUS_SHIPPED);
                hcFinishedStockMapper.updateById(update);
                recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, STATUS_SHIPPED),
                        "FG_PACKAGE_DIRECT_OUTBOUND", now, operatorName, "FG_INBOUND_PACKAGE", box.getId(),
                        box.getInnerUnitNo(), outboundRemark);
            }
        }

        syncPendingInboundPackageSourceOutbound(items, box, boxStatus, now, operatorName, reason,
                "PACKAGING_DIRECT_OUTBOUND", "合格品待上架包装直接出库");
        HcInnerPackUnitDO boxUpdate = new HcInnerPackUnitDO();
        boxUpdate.setId(box.getId());
        boxUpdate.setUnitStatus(STATUS_MANUAL_OUTBOUNDED);
        hcInnerPackUnitMapper.updateById(boxUpdate);
        return box.getInnerUnitNo();
    }

    private HcFinishedStockDO buildPendingQualifiedDirectOutboundSnapshot(HcInnerPackUnitDO box,
                                                                           HcInnerPackUnitItemDO item) {
        return HcFinishedStockDO.builder()
                .tenantId(box.getTenantId())
                .outerBoxNo(box.getInnerUnitNo())
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo(firstNotBlank(item.getSliceBatchNo(), item.getProductionBatchNo()))
                .materialCode(box.getMaterialCode())
                .materialName(box.getMaterialName())
                .modelCode(box.getModelCode())
                .batchNo(box.getBatchNo())
                .productSize(box.getProductSize())
                .qty(1)
                .qualityStatus(item.getQualityStatus())
                .stockStatus(STATUS_SHIPPED)
                .build();
    }

    private void syncPendingInboundPackageSourceOutbound(List<HcInnerPackUnitItemDO> items, HcInnerPackUnitDO box,
                                                         String beforeStatus, LocalDateTime now, String operatorName,
                                                         String reason, String eventType, String operationName) {
        String remark = operationName + "：" + StrUtil.trimToEmpty(reason);
        for (HcInnerPackUnitItemDO item : items) {
            if (item.getSourceManualPieceId() != null) {
                HcPackagingManualPieceDO manualPiece = hcPackagingManualPieceMapper
                        .selectByIdForUpdate(item.getSourceManualPieceId());
                if (manualPiece == null || Boolean.TRUE.equals(manualPiece.getDeleted())
                        || !List.of(MANUAL_PIECE_PACKED, MANUAL_PIECE_INBOUNDED)
                        .contains(StrUtil.blankToDefault(manualPiece.getRecordStatus(), ""))) {
                    throw invalidParamException("历史待包装来源已变化，不能办理包装出库："
                            + firstNotBlank(item.getSliceBatchNo(), item.getProductionBatchNo()));
                }
                HcPackagingManualPieceDO update = new HcPackagingManualPieceDO();
                update.setId(manualPiece.getId());
                update.setRecordStatus(STATUS_MANUAL_OUTBOUNDED);
                hcPackagingManualPieceMapper.updateById(update);
                hcPackagingPieceEventLogService.recordManualPieceEvent(manualPiece, eventType,
                        beforeStatus, STATUS_MANUAL_OUTBOUNDED, now, operatorName, box.getInnerUnitNo(), remark);
                continue;
            }
            if (item.getSourceCutRoundReportId() != null) {
                HcCutRoundReportDO report = hcCutRoundReportMapper.selectByIdForUpdate(item.getSourceCutRoundReportId());
                if (report == null || Boolean.TRUE.equals(report.getDeleted())) {
                    throw invalidParamException("裁切报工来源已变化，不能办理包装出库："
                            + firstNotBlank(item.getSliceBatchNo(), item.getProductionBatchNo()));
                }
                hcPackagingPieceEventLogService.recordCutRoundEvent(report, eventType,
                        beforeStatus, STATUS_MANUAL_OUTBOUNDED, now, operatorName, box.getInnerUnitNo(), remark);
                continue;
            }
            throw invalidParamException("包装片缺少可追溯来源，不能办理包装出库："
                    + firstNotBlank(item.getSliceBatchNo(), item.getProductionBatchNo()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManualOutboundStockBatchRespVO manualOutboundFgStocks(ManualOutboundStockBatchReqVO reqVO) {
        Set<Long> selectedStockIds = new LinkedHashSet<>(reqVO.getStockIds());
        if (selectedStockIds.size() != reqVO.getStockIds().size()) {
            throw invalidParamException("选中的库存片不能重复");
        }
        Map<String, List<Long>> selectedStockIdsByUnitNo = new LinkedHashMap<>();
        for (Long stockId : selectedStockIds) {
            HcFinishedStockDO stock = hcFinishedStockMapper.selectById(stockId);
            if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
                throw invalidParamException("库存片不存在或已失效，请刷新后重试");
            }
            String innerUnitNo = StrUtil.trimToNull(stock.getInnerUnitNo());
            if (innerUnitNo == null) {
                throw invalidParamException("片号 " + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo())
                        + " 未关联内包装，不能从成品库存批量出库");
            }
            selectedStockIdsByUnitNo.computeIfAbsent(innerUnitNo, key -> new ArrayList<>()).add(stockId);
        }

        List<String> unitNos = new ArrayList<>(selectedStockIdsByUnitNo.keySet());
        unitNos.sort(Comparator.naturalOrder());
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        Set<String> affectedLocationCodes = new LinkedHashSet<>();
        List<String> outboundSliceBatchNos = new ArrayList<>();
        int outboundStockCount = 0;

        for (String unitNo : unitNos) {
            HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(unitNo);
            if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
                throw invalidParamException("内包装 " + unitNo + " 不存在或已失效，请刷新后重试");
            }
            if (!STATUS_INBOUNDED.equalsIgnoreCase(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
                throw invalidParamException("内包装 " + unitNo + " 不是已上架状态，不能手工出库");
            }
            List<HcFinishedStockDO> packageStocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(unitNo);
            if (packageStocks.isEmpty()) {
                throw invalidParamException("内包装 " + unitNo + " 没有库存台账，不能手工出库");
            }
            Set<Long> selectedIdsInUnit = new HashSet<>(selectedStockIdsByUnitNo.get(unitNo));
            HcFinishedStockDO unavailableStock = packageStocks.stream()
                    .filter(stock -> !STATUS_AVAILABLE.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), "")))
                    .findFirst()
                    .orElse(null);
            if (unavailableStock != null) {
                throw invalidParamException("片号 " + firstNotBlank(unavailableStock.getSliceBatchNo(), unavailableStock.getStockNo())
                        + " 当前不是可用在库状态，不能手工出库");
            }
            List<HcFinishedStockDO> missingStocks = packageStocks.stream()
                    .filter(stock -> !selectedIdsInUnit.contains(stock.getId()))
                    .toList();
            if (!missingStocks.isEmpty()) {
                String missingSliceBatchNos = missingStocks.stream()
                        .map(stock -> firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()))
                        .filter(StrUtil::isNotBlank)
                        .limit(10)
                        .collect(java.util.stream.Collectors.joining("、"));
                throw invalidParamException("内包装 " + unitNo + " 必须勾选包内全部可用片号后才允许批量出库"
                        + (StrUtil.isBlank(missingSliceBatchNos) ? "" : "，未选择：" + missingSliceBatchNos));
            }
            if (selectedIdsInUnit.size() != packageStocks.size()) {
                throw invalidParamException("内包装 " + unitNo + " 的勾选库存与当前包装台账不一致，请刷新后重试");
            }

            for (HcFinishedStockDO stock : packageStocks) {
                HcFinishedStockDO update = new HcFinishedStockDO();
                update.setId(stock.getId());
                update.setStockStatus(STATUS_SHIPPED);
                hcFinishedStockMapper.updateById(update);
                recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, STATUS_SHIPPED),
                        "FG_MANUAL_OUTBOUND", now, operatorName, "FG_INBOUND_PACKAGE", box.getId(),
                        box.getInnerUnitNo(), reqVO.getReason());
                if (StrUtil.isNotBlank(stock.getLocationCode())) {
                    affectedLocationCodes.add(stock.getLocationCode());
                }
                outboundSliceBatchNos.add(firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
                outboundStockCount++;
            }

            HcInnerPackUnitDO boxUpdate = new HcInnerPackUnitDO();
            boxUpdate.setId(box.getId());
            boxUpdate.setUnitStatus(STATUS_MANUAL_OUTBOUNDED);
            hcInnerPackUnitMapper.updateById(boxUpdate);
            if (StrUtil.isNotBlank(box.getLocationCode())) {
                affectedLocationCodes.add(box.getLocationCode());
            }
        }
        affectedLocationCodes.forEach(this::refreshLocationOccupied);
        ManualOutboundStockBatchRespVO respVO = new ManualOutboundStockBatchRespVO();
        respVO.setStockCount(outboundStockCount);
        respVO.setPackageCount(unitNos.size());
        respVO.setSliceBatchNos(outboundSliceBatchNos);
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ManualOutboundRepackReturnRespVO returnManualOutboundStockForRepack(
            ManualOutboundRepackReturnReqVO reqVO) {
        if (!Boolean.TRUE.equals(reqVO.getPhysicalReturned())) {
            throw invalidParamException("请确认实物已退回且已拆开原包装");
        }
        HcFinishedStockTxnLogDO outboundTxn = hcFinishedStockTxnLogMapper.selectById(reqVO.getTxnLogId());
        if (!isManualOutboundTxn(outboundTxn)) {
            throw invalidParamException("仅允许从手工成品出库流水退回待重新包装");
        }
        String innerUnitNo = StrUtil.trimToNull(outboundTxn.getInnerUnitNo());
        if (innerUnitNo == null || outboundTxn.getFinishedStockId() == null) {
            throw invalidParamException("手工出库流水缺少库存或内包装关联，不能退回待重新包装");
        }
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(innerUnitNo);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("原内包装不存在或已失效，不能退回待重新包装");
        }
        if (outboundTxn.getRefDocId() != null && !Objects.equals(outboundTxn.getRefDocId(), box.getId())) {
            throw invalidParamException("手工出库流水与原内包装不匹配，不能退回待重新包装");
        }
        if (!STATUS_MANUAL_OUTBOUNDED.equalsIgnoreCase(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("原内包装当前不是手工出库状态，不能重复退回或拆包");
        }

        List<HcInnerPackUnitItemDO> packageItems = hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId());
        List<HcFinishedStockDO> packageStocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(innerUnitNo);
        HcFinishedStockDO stock = packageStocks.stream()
                .filter(item -> Objects.equals(item.getId(), outboundTxn.getFinishedStockId()))
                .findFirst()
                .orElse(null);
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())
                || !STATUS_SHIPPED.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), ""))) {
            throw invalidParamException("该片号当前不是可退回的手工出库状态，请刷新后重试");
        }
        if (!Objects.equals(innerUnitNo, StrUtil.trimToNull(stock.getInnerUnitNo()))) {
            throw invalidParamException("库存片与原内包装不匹配，不能退回待重新包装");
        }
        HcInnerPackUnitItemDO packItem = packageItems.stream()
                .filter(item -> matchesPackageItemStock(item, stock))
                .findFirst()
                .orElse(null);
        if (packItem == null) {
            throw invalidParamException("原内包装未找到该片号明细，不能退回待重新包装");
        }
        restoreReturnedPieceToPackagingWait(packItem, stock);

        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        String returnRemark = "手工成品出库退回待重新包装；原出库流水：" + outboundTxn.getTxnNo()
                + "；退回原因：" + reqVO.getReason().trim();
        hcInnerPackUnitItemMapper.deletePhysicallyByIds(List.of(packItem.getId()));
        recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, "RETURNED_FOR_REPACK"),
                "FG_PACKAGE_SPLIT_RETURN", now, operatorName, "FG_INBOUND_PACKAGE", box.getId(),
                box.getInnerUnitNo(), returnRemark);
        hcFinishedStockMapper.deletePhysicallyByIds(List.of(stock.getId()));

        int remainingPieceCount = Math.max(0, packageItems.size() - 1);
        if (remainingPieceCount == 0) {
            hcInnerPackUnitMapper.deletePhysicallyById(box.getId());
        } else {
            HcInnerPackUnitDO boxUpdate = new HcInnerPackUnitDO();
            boxUpdate.setId(box.getId());
            boxUpdate.setCurrentQty(remainingPieceCount);
            boxUpdate.setWarehouseCode("");
            boxUpdate.setWarehouseName("");
            boxUpdate.setLocationCode("");
            boxUpdate.setLocationName("");
            boxUpdate.setUnitStatus(STATUS_MANUAL_OUTBOUNDED);
            hcInnerPackUnitMapper.updateById(boxUpdate);
        }
        refreshLocationOccupied(box.getLocationCode());

        ManualOutboundRepackReturnRespVO respVO = new ManualOutboundRepackReturnRespVO();
        respVO.setSliceBatchNo(firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
        respVO.setSourceInnerUnitNo(box.getInnerUnitNo());
        respVO.setSourcePackageRemainingPieceCount(remainingPieceCount);
        return respVO;
    }

    @Override
    public List<FgLocationGridRespVO> getFgLocationGrid() {
        List<HcLocationDO> locationList = hcLocationMapper.selectListByBizScene(FG_LOCATION_SCENE);
        Map<Long, HcFgRackDO> rackMap = getFgRackMap(getFgWarehouseMap().keySet());
        Map<Long, HcFgLayerDO> layerMap = getFgLayerMap(rackMap.keySet());
        List<String> locationCodes = new ArrayList<>();
        locationList.forEach(location -> {
            if (StrUtil.isNotBlank(location.getLocationCode())) {
                locationCodes.add(location.getLocationCode());
            }
        });
        Map<String, List<HcFinishedStockDO>> stockMap = new LinkedHashMap<>();
        hcFinishedStockMapper.selectActiveListByLocationCodes(locationCodes).forEach(stock -> {
            if (StrUtil.isNotBlank(stock.getLocationCode())) {
                stockMap.computeIfAbsent(stock.getLocationCode(), key -> new ArrayList<>()).add(stock);
            }
        });
        return locationList.stream()
                .map(location -> buildFgLocationResp(location,
                        stockMap.getOrDefault(location.getLocationCode(), Collections.emptyList()),
                        rackMap.get(location.getRackId()), layerMap.get(location.getLayerId())))
                .toList();
    }

    @Override
    public List<FgLocationTreeWarehouseRespVO> getFgLocationTree() {
        Map<Long, HcFgWarehouseDO> warehouseMap = getFgWarehouseMap();
        Map<Long, HcFgRackDO> rackMap = getFgRackMap(warehouseMap.keySet());
        Map<Long, HcFgLayerDO> layerMap = getFgLayerMap(rackMap.keySet());
        Map<Long, List<FgLocationGridRespVO>> areasByLayerId = new LinkedHashMap<>();
        getFgLocationGrid().forEach(area -> {
            if (area.getLayerId() != null) {
                areasByLayerId.computeIfAbsent(area.getLayerId(), key -> new ArrayList<>()).add(area);
            }
        });
        Map<Long, List<HcFgLayerDO>> layersByRackId = new LinkedHashMap<>();
        layerMap.values().forEach(layer -> layersByRackId
                .computeIfAbsent(layer.getRackId(), key -> new ArrayList<>()).add(layer));
        return warehouseMap.values().stream().map(warehouse -> {
            FgLocationTreeWarehouseRespVO warehouseResp = new FgLocationTreeWarehouseRespVO();
            warehouseResp.setId(warehouse.getId());
            warehouseResp.setWarehouseCode(warehouse.getWarehouseCode());
            warehouseResp.setWarehouseName(warehouse.getWarehouseName());
            warehouseResp.setStatus(warehouse.getStatus());
            warehouseResp.setSortNo(warehouse.getSortNo());
            warehouseResp.setRemark(warehouse.getRemark());
            warehouseResp.setRacks(rackMap.values().stream()
                    .filter(rack -> Objects.equals(rack.getWarehouseId(), warehouse.getId()))
                    .map(rack -> buildFgLocationTreeRackResp(rack,
                            layersByRackId.getOrDefault(rack.getId(), Collections.emptyList()), areasByLayerId))
                    .toList());
            return warehouseResp;
        }).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFgWarehouse(FgWarehouseSaveReqVO reqVO) {
        HcFgWarehouseDO old = reqVO.getId() == null ? null : getFgWarehouse(reqVO.getId());
        String warehouseCode = StrUtil.trimToEmpty(reqVO.getWarehouseCode()).toUpperCase(Locale.ROOT);
        if (!FG_WAREHOUSE_CODE_PATTERN.matcher(warehouseCode).matches()) {
            throw invalidParamException("仓库编码仅支持大写字母、数字、下划线或连字符，且必须以字母开头");
        }
        HcFgWarehouseDO sameCode = hcFgWarehouseMapper.selectAnyByWarehouseCode(warehouseCode);
        if (sameCode != null && (old == null || !Objects.equals(sameCode.getId(), old.getId()))) {
            if (old == null && Boolean.TRUE.equals(sameCode.getDeleted())) {
                old = sameCode;
            } else {
                throw invalidParamException("仓库编码已存在");
            }
        }
        if (old != null && !Objects.equals(old.getWarehouseCode(), warehouseCode)
                && !hcFgRackMapper.selectListByWarehouseId(old.getId()).isEmpty()) {
            throw invalidParamException("仓库已有货架，不能修改仓库编码；请新建仓库后按空库位迁移");
        }
        String status = validateFgLocationStatus(reqVO.getStatus(), old == null ? null : old.getStatus());
        if (!DEFAULT_LOCATION_STATUS.equals(status) && old != null) {
            ensureFgAreasEmpty(hcLocationMapper.selectListByWarehouseId(old.getId()), "仓库");
        }
        boolean restoring = old != null && Boolean.TRUE.equals(old.getDeleted());
        HcFgWarehouseDO warehouse = old == null ? new HcFgWarehouseDO() : old;
        warehouse.setWarehouseCode(warehouseCode);
        warehouse.setWarehouseName(StrUtil.trim(reqVO.getWarehouseName()));
        warehouse.setStatus(status);
        warehouse.setSortNo(normalizeSortNo(reqVO.getSortNo(), old == null ? 0 : intValue(old.getSortNo())));
        warehouse.setRemark(StrUtil.trimToNull(reqVO.getRemark()));
        warehouse.setDeleted(false);
        if (old == null) {
            warehouse.setTenantId(currentTenantId());
            hcFgWarehouseMapper.insert(warehouse);
        } else {
            if (restoring && hcFgWarehouseMapper.restoreDeletedById(warehouse.getId()) != 1) {
                throw invalidParamException("历史仓库恢复失败，请刷新后重试");
            }
            hcFgWarehouseMapper.updateById(warehouse);
        }
        return warehouse.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FgWarehouseQualityScopeUpdateRespVO updateFgWarehouseQualityScope(
            FgWarehouseQualityScopeUpdateReqVO reqVO) {
        HcFgWarehouseDO warehouse = getFgWarehouse(reqVO.getWarehouseId());
        String qualityScope = normalizeFgLocationQualityScope(reqVO.getQualityScope());
        if (!List.of(FG_LOCATION_QUALITY_SCOPE_QUALIFIED, FG_LOCATION_QUALITY_SCOPE_QUARANTINE)
                .contains(qualityScope)) {
            throw invalidParamException("仓库质量用途仅支持合格品仓或不合格品隔离仓");
        }

        // 与上架流程使用相同的库位行锁，避免批量校验期间出现并发上架穿透质量隔离。
        List<HcLocationDO> areas = hcLocationMapper.selectListByWarehouseIdForUpdate(warehouse.getId());
        if (areas.isEmpty()) {
            throw invalidParamException("当前仓库尚未维护区域，请先新增货架、层和区域");
        }
        List<String> locationCodes = areas.stream()
                .map(HcLocationDO::getLocationCode)
                .filter(StrUtil::isNotBlank)
                .toList();
        List<HcFinishedStockDO> activeStocks = hcFinishedStockMapper
                .selectActiveListByLocationCodesForUpdate(locationCodes);
        String requiredQuality = FG_LOCATION_QUALITY_SCOPE_QUARANTINE.equals(qualityScope)
                ? INSPECTION_RESULT_NG : INSPECTION_RESULT_OK;
        List<HcFinishedStockDO> conflicts = activeStocks.stream()
                .filter(stock -> !requiredQuality.equalsIgnoreCase(StrUtil.trimToEmpty(stock.getQualityStatus())))
                .toList();
        if (!conflicts.isEmpty()) {
            List<String> samples = conflicts.stream()
                    .map(stock -> firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo(), "未知片号"))
                    .distinct()
                    .limit(5)
                    .toList();
            String targetText = FG_LOCATION_QUALITY_SCOPE_QUARANTINE.equals(qualityScope)
                    ? "不合格品隔离仓" : "合格品仓";
            throw invalidParamException(String.format(
                    "仓库不能设置为%s：存在%s条质量不匹配的在库记录，示例：%s",
                    targetText, conflicts.size(), StrUtil.join(",", samples)));
        }

        for (HcLocationDO area : areas) {
            if (qualityScope.equals(normalizeFgLocationQualityScope(area.getQualityScope()))) {
                continue;
            }
            HcLocationDO update = new HcLocationDO();
            update.setId(area.getId());
            update.setQualityScope(qualityScope);
            hcLocationMapper.updateById(update);
        }

        Set<String> occupiedLocationCodes = activeStocks.stream()
                .map(HcFinishedStockDO::getLocationCode)
                .filter(StrUtil::isNotBlank)
                .collect(java.util.stream.Collectors.toSet());
        FgWarehouseQualityScopeUpdateRespVO respVO = new FgWarehouseQualityScopeUpdateRespVO();
        respVO.setWarehouseId(warehouse.getId());
        respVO.setWarehouseCode(warehouse.getWarehouseCode());
        respVO.setWarehouseName(warehouse.getWarehouseName());
        respVO.setQualityScope(qualityScope);
        respVO.setLocationCount(areas.size());
        respVO.setOccupiedLocationCount(occupiedLocationCodes.size());
        respVO.setActivePieceCount(activeStocks.stream().mapToLong(this::stockPieceQty).sum());
        return respVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFgWarehouse(Long id) {
        HcFgWarehouseDO warehouse = getFgWarehouse(id);
        List<HcLocationDO> areas = hcLocationMapper.selectListByWarehouseId(warehouse.getId());
        ensureFgAreasEmpty(areas, "仓库");
        List<HcFgRackDO> racks = hcFgRackMapper.selectListByWarehouseId(warehouse.getId());
        List<HcFgLayerDO> layers = racks.isEmpty() ? Collections.emptyList()
                : hcFgLayerMapper.selectListByRackIds(racks.stream().map(HcFgRackDO::getId).toList());
        if (!areas.isEmpty()) {
            hcLocationMapper.deleteByIds(areas.stream().map(HcLocationDO::getId).toList());
        }
        if (!layers.isEmpty()) {
            hcFgLayerMapper.deleteByIds(layers.stream().map(HcFgLayerDO::getId).toList());
        }
        if (!racks.isEmpty()) {
            hcFgRackMapper.deleteByIds(racks.stream().map(HcFgRackDO::getId).toList());
        }
        hcFgWarehouseMapper.deleteById(warehouse.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFgRack(FgRackSaveReqVO reqVO) {
        HcFgWarehouseDO warehouse = getFgWarehouse(reqVO.getWarehouseId());
        if (!DEFAULT_LOCATION_STATUS.equals(warehouse.getStatus())) {
            throw invalidParamException("所选仓库已停用，不能维护货架");
        }
        HcFgRackDO old = reqVO.getId() == null ? null : getFgRack(reqVO.getId());
        int rackNo = validateFgLocationCodePart(reqVO.getRackNo(), "货架编号");
        if (old != null && !Objects.equals(old.getWarehouseId(), warehouse.getId())) {
            throw invalidParamException("已存在的货架不能更换所属仓库");
        }
        HcFgRackDO sameNo = hcFgRackMapper.selectAnyByWarehouseAndRackNo(warehouse.getId(), rackNo);
        if (sameNo != null && (old == null || !Objects.equals(sameNo.getId(), old.getId()))) {
            if (old == null && Boolean.TRUE.equals(sameNo.getDeleted())) {
                old = sameNo;
            } else {
                throw invalidParamException("货架编号已存在");
            }
        }
        boolean restoring = old != null && Boolean.TRUE.equals(old.getDeleted());
        if (old != null && !Objects.equals(old.getRackNo(), rackNo)
                && !hcFgLayerMapper.selectListByRackId(old.getId()).isEmpty()) {
            throw invalidParamException("货架已有层级，不能修改货架编号；如需调整请新建货架后迁移空区域");
        }
        HcFgRackDO rack = old == null ? new HcFgRackDO() : old;
        rack.setRackNo(rackNo);
        rack.setRackName(StrUtil.trim(reqVO.getRackName()));
        rack.setWarehouseId(warehouse.getId());
        rack.setWarehouseCode(warehouse.getWarehouseCode());
        rack.setWarehouseName(warehouse.getWarehouseName());
        rack.setStatus(validateFgLocationStatus(reqVO.getStatus(), old == null ? null : old.getStatus()));
        rack.setSortNo(normalizeSortNo(reqVO.getSortNo(), rackNo));
        rack.setRemark(StrUtil.trimToNull(reqVO.getRemark()));
        rack.setDeleted(false);
        if (old == null) {
            rack.setTenantId(currentTenantId());
            hcFgRackMapper.insert(rack);
        } else {
            if (restoring && hcFgRackMapper.restoreDeletedById(rack.getId()) != 1) {
                throw invalidParamException("历史货架恢复失败，请刷新后重试");
            }
            hcFgRackMapper.updateById(rack);
        }
        return rack.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFgRack(Long id) {
        HcFgRackDO rack = getFgRack(id);
        List<HcFgLayerDO> layers = hcFgLayerMapper.selectListByRackId(rack.getId());
        List<HcLocationDO> areas = hcLocationMapper.selectListByRackId(rack.getId());
        ensureFgAreasEmpty(areas, "货架");
        if (!areas.isEmpty()) {
            hcLocationMapper.deleteByIds(areas.stream().map(HcLocationDO::getId).toList());
        }
        if (!layers.isEmpty()) {
            hcFgLayerMapper.deleteByIds(layers.stream().map(HcFgLayerDO::getId).toList());
        }
        hcFgRackMapper.deleteById(rack.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFgLayer(FgLayerSaveReqVO reqVO) {
        HcFgRackDO rack = getFgRack(reqVO.getRackId());
        HcFgLayerDO old = reqVO.getId() == null ? null : getFgLayer(reqVO.getId());
        int layerNo = validateFgLocationCodePart(reqVO.getLayerNo(), "层号");
        if (old != null && !Objects.equals(old.getRackId(), rack.getId())) {
            throw invalidParamException("已存在的层不能更换所属货架");
        }
        HcFgLayerDO sameNo = hcFgLayerMapper.selectAnyByRackAndLayerNo(rack.getId(), layerNo);
        if (sameNo != null && (old == null || !Objects.equals(sameNo.getId(), old.getId()))) {
            if (old == null && Boolean.TRUE.equals(sameNo.getDeleted())) {
                old = sameNo;
            } else {
                throw invalidParamException("该货架下层号已存在");
            }
        }
        boolean restoring = old != null && Boolean.TRUE.equals(old.getDeleted());
        HcFgLayerDO layer = old == null ? new HcFgLayerDO() : old;
        layer.setRackId(rack.getId());
        layer.setLayerNo(layerNo);
        layer.setLayerName(StrUtil.trim(reqVO.getLayerName()));
        layer.setStatus(validateFgLocationStatus(reqVO.getStatus(), old == null ? null : old.getStatus()));
        layer.setSortNo(normalizeSortNo(reqVO.getSortNo(), layerNo));
        layer.setRemark(StrUtil.trimToNull(reqVO.getRemark()));
        layer.setDeleted(false);
        if (old == null) {
            layer.setTenantId(currentTenantId());
            hcFgLayerMapper.insert(layer);
        } else {
            if (restoring && hcFgLayerMapper.restoreDeletedById(layer.getId()) != 1) {
                throw invalidParamException("历史货架层恢复失败，请刷新后重试");
            }
            hcFgLayerMapper.updateById(layer);
        }
        return layer.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFgLayer(Long id) {
        HcFgLayerDO layer = getFgLayer(id);
        List<HcLocationDO> areas = hcLocationMapper.selectListByLayerId(layer.getId());
        ensureFgAreasEmpty(areas, "货架层");
        if (!areas.isEmpty()) {
            hcLocationMapper.deleteByIds(areas.stream().map(HcLocationDO::getId).toList());
        }
        hcFgLayerMapper.deleteById(layer.getId());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveFgLocation(FgLocationSaveReqVO reqVO) {
        HcLocationDO old = reqVO.getId() == null ? null : getFgLocation(reqVO.getId());
        HcFgRackDO rack = getFgRack(reqVO.getRackId());
        HcFgWarehouseDO warehouse = getFgWarehouse(rack.getWarehouseId());
        HcFgLayerDO layer = getFgLayer(reqVO.getLayerId());
        if (!Objects.equals(layer.getRackId(), rack.getId())) {
            throw invalidParamException("所选层不属于所选货架");
        }
        if (!DEFAULT_LOCATION_STATUS.equals(warehouse.getStatus())
                || !DEFAULT_LOCATION_STATUS.equals(rack.getStatus()) || !DEFAULT_LOCATION_STATUS.equals(layer.getStatus())) {
            throw invalidParamException("所选仓库、货架或层已停用，不能新增或调整区域");
        }
        int rackNo = rack.getRackNo();
        int layerNo = layer.getLayerNo();
        int areaNo = validateFgLocationCodePart(reqVO.getAreaNo(), "区域号");
        int gridNo = calculateFgLocationGridNo(rackNo, layerNo, areaNo);
        int capacityQty = positive(reqVO.getCapacityQty(), "库容量必须大于0");
        String locationCode = buildFgLocationCode(warehouse.getWarehouseCode(), rackNo, layerNo, areaNo);
        String defaultLocationName = buildFgLocationName(rack, layer, areaNo);
        String defaultPositionDesc = buildFgLocationPositionDesc(rack, layer, areaNo, capacityQty);
        String locationName = firstNotBlank(StrUtil.trimToNull(reqVO.getLocationName()),
                old == null ? null : old.getLocationName(), defaultLocationName);
        HcLocationDO codeUsed = hcLocationMapper.selectAnyIncludingDeletedByLocationCode(locationCode);
        if (codeUsed != null && (old == null || !Objects.equals(codeUsed.getId(), old.getId()))) {
            if (old == null && Boolean.TRUE.equals(codeUsed.getDeleted()) && FG_LOCATION_SCENE.equals(codeUsed.getBizScene())) {
                old = codeUsed;
            } else {
                throw invalidParamException("区域编号已存在");
            }
        }
        // gridNo 只承载历史展示排序。区级迁移保留了原托盘位的排序号，不能再作为货架-层-区的唯一标识，
        // 否则空分区会因历史排序号重叠而无法新增。区位唯一性以 locationCode 为准。

        long activeQty = old == null ? 0L : activePieceCount(old.getLocationCode());
        int storedOccupiedQty = old == null ? 0 : intValue(old.getOccupiedQty());
        int actualOccupiedQty = (int) Math.max(storedOccupiedQty, activeQty);
        if (capacityQty < actualOccupiedQty) {
            throw invalidParamException("库容量不能小于当前已占容量");
        }
        boolean physicalLocationChanged = old != null
                && (!Objects.equals(old.getWarehouseId(), warehouse.getId())
                || !Objects.equals(old.getGridNo(), gridNo) || !Objects.equals(old.getLocationCode(), locationCode));
        if (physicalLocationChanged && actualOccupiedQty > 0) {
            throw invalidParamException("当前库位已有库存，不能调整货架、层或分区；请先移库或出库");
        }
        String status = firstNotBlank(StrUtil.trimToNull(reqVO.getStatus()),
                old == null ? null : old.getStatus(), DEFAULT_LOCATION_STATUS);
        if (!DEFAULT_LOCATION_STATUS.equals(status) && !"停用".equals(status)) {
            throw invalidParamException("库位状态仅支持启用或停用");
        }
        if (!DEFAULT_LOCATION_STATUS.equals(status) && actualOccupiedQty > 0) {
            throw invalidParamException("已被占用的库位不能停用，请先完成移库或出库");
        }
        String qualityScope = normalizeFgLocationQualityScope(firstNotBlank(
                StrUtil.trimToNull(reqVO.getQualityScope()), old == null ? null : old.getQualityScope(),
                FG_LOCATION_QUALITY_SCOPE_QUALIFIED));
        if (old != null && actualOccupiedQty > 0
                && !Objects.equals(normalizeFgLocationQualityScope(old.getQualityScope()), qualityScope)) {
            throw invalidParamException("当前库位已有库存，不能调整质量用途；请先完成移库或出库");
        }

        boolean restoringDeletedLocation = old != null && Boolean.TRUE.equals(old.getDeleted());
        HcLocationDO location = old == null ? new HcLocationDO() : old;
        location.setBizScene(FG_LOCATION_SCENE);
        location.setGridNo(gridNo);
        location.setLocationCode(locationCode);
        location.setLocationName(locationName);
        location.setWarehouseId(warehouse.getId());
        location.setWarehouseCode(warehouse.getWarehouseCode());
        location.setWarehouseName(warehouse.getWarehouseName());
        location.setLocationType(DEFAULT_LOCATION_TYPE);
        location.setQualityScope(qualityScope);
        location.setPositionDesc(defaultPositionDesc);
        location.setRackId(rack.getId());
        location.setLayerId(layer.getId());
        location.setAreaNo(areaNo);
        location.setCapacityQty(capacityQty);
        location.setOccupiedQty(actualOccupiedQty);
        location.setQrCode(locationCode);
        location.setMixBatchFlag(reqVO.getMixBatchFlag() == null
                ? old != null && Boolean.TRUE.equals(old.getMixBatchFlag()) : reqVO.getMixBatchFlag());
        location.setMixModelFlag(reqVO.getMixModelFlag() == null
                ? old != null && Boolean.TRUE.equals(old.getMixModelFlag()) : reqVO.getMixModelFlag());
        location.setStatus(status);
        location.setDeleted(false);
        if (old == null) {
            location.setTenantId(currentTenantId());
            hcLocationMapper.insert(location);
        } else {
            if (restoringDeletedLocation && hcLocationMapper.restoreDeletedById(location.getId()) != 1) {
                throw invalidParamException("历史库位恢复失败，请刷新后重试");
            }
            hcLocationMapper.updateById(location);
        }
        return location.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFgLocation(Long id) {
        HcLocationDO location = getFgLocation(id);
        int occupiedQty = (int) activePieceCount(location.getLocationCode());
        if (occupiedQty > 0) {
            throw invalidParamException("当前库位已有库存片号，不能删除");
        }
        hcLocationMapper.deleteById(id);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FgLocationGridRespVO printFgLocation(FgLocationPrintReqVO reqVO) {
        HcLocationDO location = getFgLocation(reqVO.getId());
        if (StrUtil.isBlank(location.getQrCode())) {
            HcLocationDO update = new HcLocationDO();
            update.setId(location.getId());
            update.setQrCode(location.getLocationCode());
            hcLocationMapper.updateById(update);
            location.setQrCode(location.getLocationCode());
        }
        return buildFgLocationResp(location);
    }

    @Override
    public List<FgStockLocationOverviewRespVO> getFgStockLocationOverview() {
        return hcLocationMapper.selectListByBizScene(FG_LOCATION_SCENE).stream()
                .map(this::buildFgStockLocationOverviewResp)
                .toList();
    }

    @Override
    public FgStockLocationDetailRespVO getFgStockLocationDetail(String locationCode) {
        HcLocationDO location = getFgLocationByCode(locationCode);
        FgStockLedgerPageReqVO reqVO = new FgStockLedgerPageReqVO();
        reqVO.setLocationCode(location.getLocationCode());
        reqVO.setPageNo(1);
        reqVO.setPageSize(200);

        FgStockLocationDetailRespVO resp = new FgStockLocationDetailRespVO();
        resp.setLocation(buildFgStockLocationOverviewResp(location));
        resp.setPackages(hcInnerPackUnitMapper.selectStockListByLocationCode(location.getLocationCode()).stream()
                .map(this::buildInboundBoxResp)
                .toList());
        resp.setPieces(getFgStockLedgerPage(reqVO).getList());
        return resp;
    }

    @Override
    public PageResult<FgStockLedgerRespVO> getFgStockLedgerPage(FgStockLedgerPageReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 20 : reqVO.getPageSize();
        int offset = Math.max(0, (pageNo - 1) * pageSize);
        Long total = hcFinishedPackagingMapper.countFgStockLedgerPage(reqVO);
        if (total == null || total <= 0) {
            return PageResult.empty();
        }
        return new PageResult<>(
                hcFinishedPackagingMapper.selectFgStockLedgerPage(
                        reqVO, LocalDate.now(BUSINESS_ZONE), offset, pageSize),
                total);
    }

    /**
     * 更正已经上架的历史导入片快照。
     *
     * <p>只允许处理历史导入的单片包装。片号更正会同步来源片、内包装片明细和库存台账；
     * 数量、库位、状态和入库时间仍必须继续走片号调账、移库或出入库等对应业务动作。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public FgStockLedgerRespVO updateImportedStockData(ImportedStockDataUpdateReqVO reqVO) {
        HcFinishedStockDO stock = hcFinishedStockMapper.selectByIdForUpdate(reqVO.getStockId());
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            throw invalidParamException("成品库存不存在或已删除，请刷新后重试");
        }
        if (!List.of(STATUS_INBOUNDED, STATUS_AVAILABLE).contains(
                StrUtil.trimToEmpty(stock.getStockStatus()).toUpperCase(Locale.ROOT))) {
            throw invalidParamException("只有已上架且可用的成品库存允许更正历史导入数据");
        }
        if (StrUtil.isBlank(stock.getInnerUnitNo())) {
            throw invalidParamException("该成品库存未关联内包装，不能更正历史导入数据");
        }
        Integer activeLockedQty = hcFgShippingNoticeItemMapper.selectActiveLockedQtyByFinishedStockId(stock.getId());
        if (activeLockedQty != null && activeLockedQty > 0) {
            throw invalidParamException("该成品库存已被发货需求占用，不能修改；请先完成受控退回或解除锁定");
        }

        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(stock.getInnerUnitNo());
        if (box == null || Boolean.TRUE.equals(box.getDeleted())
                || !STATUS_INBOUNDED.equalsIgnoreCase(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("关联内包装不是已上架状态，不能更正历史导入数据");
        }
        List<HcInnerPackUnitItemDO> packageItems = hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId());
        if (packageItems.size() != 1) {
            throw invalidParamException("历史多片包装不支持单片更正，请按整包更正流程处理");
        }
        HcInnerPackUnitItemDO packItem = packageItems.get(0);
        if (!matchesPackageItemStock(packItem, stock) || packItem.getSourceManualPieceId() == null) {
            throw invalidParamException("该库存不是有效历史导入来源，不能使用数据更正");
        }
        String sourceType = StrUtil.trimToEmpty(packItem.getSourceType()).toUpperCase(Locale.ROOT);
        if (StrUtil.isNotBlank(sourceType) && !SOURCE_MANUAL_HISTORY.equals(sourceType)) {
            throw invalidParamException("只有历史导入来源的成品库存允许更正");
        }

        HcPackagingManualPieceDO manualPiece = hcPackagingManualPieceMapper
                .selectByIdForUpdate(packItem.getSourceManualPieceId());
        String manualPieceStatus = manualPiece == null ? ""
                : StrUtil.trimToEmpty(manualPiece.getRecordStatus()).toUpperCase(Locale.ROOT);
        if (manualPiece == null || Boolean.TRUE.equals(manualPiece.getDeleted())
                // 兼容旧版历史库存导入：来源片状态未随入库同步，仍为 PACKED。
                || !List.of(MANUAL_PIECE_PACKED, MANUAL_PIECE_INBOUNDED).contains(manualPieceStatus)
                || !StrUtil.equals(StrUtil.trim(manualPiece.getInnerUnitNo()), StrUtil.trim(box.getInnerUnitNo()))) {
            throw invalidParamException("历史导入来源已变化，不能更正；请刷新后重试");
        }
        if (!qmsPackagingCoaSampleClaimMapper.selectListBySource(SOURCE_MANUAL_HISTORY,
                List.of(manualPiece.getId())).isEmpty()) {
            throw invalidParamException("该历史导入片已被 COA 样片占用，不能更正；请按检验修订流程处理");
        }

        String sliceBatchNo = normalizeUpperText(reqVO.getSliceBatchNo());
        validateManualPieceImportText(sliceBatchNo, "片号", 100, true);
        validateImportedStockSliceBatchNoUnique(sliceBatchNo, manualPiece.getId(), packItem.getId(), stock.getId());
        String segmentBatchNo = normalizeUpperText(reqVO.getSegmentBatchNo());
        validateManualPieceImportText(segmentBatchNo, "分段批号", 100, true);

        String materialCode = normalizeUpperText(reqVO.getMaterialCode());
        String modelCode = normalizeUpperText(reqVO.getModelCode());
        validateManualPieceImportText(materialCode, "产品料号", 64, true);
        validateManualPieceImportText(modelCode, "产品型号", 64, true);
        LocalDate productionDate = reqVO.getProductionDate();
        if (productionDate.isBefore(LocalDate.of(2000, 1, 1))) {
            throw invalidParamException("生产日期必须是有效业务日期");
        }
        LocalDate expiryDate = productionDate.plusMonths(10).minusDays(1);
        String inspectionResult = normalizeManualInspectionResult(reqVO.getInspectionResult(), "裁切FQC");
        String coaInspectionResult = normalizeManualInspectionResult(reqVO.getCoaInspectionResult(), "COA送检");
        String remark = StrUtil.trimToNull(reqVO.getRemark());
        // 修改原因作为可选审计备注；空值统一持久化为空字符串，兼容审计表的非空列。
        String correctionReason = StrUtil.trimToEmpty(reqVO.getCorrectionReason());

        HcBomDO bom = hcBomMapper.selectEnabledByMaterialCodeAndModelCode(materialCode, modelCode);
        String materialName;
        if (bom == null) {
            materialName = Objects.equals(materialCode, manualPiece.getMaterialCode())
                    ? firstNotBlank(manualPiece.getMaterialName(), materialCode)
                    : materialCode;
        } else {
            materialName = firstNotBlank(bom.getProductMaterialName(), materialCode);
        }
        String productSize = bom == null
                ? manualPiece.getProductSize()
                : firstNotBlank(bom.getProductSpec(), manualPiece.getProductSize());

        HcPackagingManualPieceDO correctedManualPiece = BeanUtil.copyProperties(manualPiece,
                HcPackagingManualPieceDO.class);
        correctedManualPiece.setSliceBatchNo(sliceBatchNo);
        correctedManualPiece.setSegmentBatchNo(segmentBatchNo);
        correctedManualPiece.setMaterialCode(materialCode);
        correctedManualPiece.setMaterialName(materialName);
        correctedManualPiece.setModelCode(modelCode);
        correctedManualPiece.setProductSize(productSize);
        correctedManualPiece.setProductionDate(productionDate);
        correctedManualPiece.setExpiryDate(expiryDate);
        correctedManualPiece.setInspectionResult(inspectionResult);
        correctedManualPiece.setCoaInspectionResult(coaInspectionResult);
        correctedManualPiece.setRemark(remark);
        String qualityStatus = resolveManualPackagingQuality(correctedManualPiece,
                buildCoaInspectionMap(Set.of(normalizeCoaSegmentBatchNo(segmentBatchNo))).get(normalizeCoaSegmentBatchNo(segmentBatchNo)));
        boolean labelReprintRequired = manualPiece.getPrintCount() != null && manualPiece.getPrintCount() > 0
                && hasImportedStockCorrectionChanged(manualPiece, correctedManualPiece);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        String operatorName = currentUserName();

        Map<String, Object> beforeSnapshot = buildImportedStockCorrectionSnapshot(manualPiece, box, packItem, stock);

        HcPackagingManualPieceDO manualPieceUpdate = new HcPackagingManualPieceDO();
        manualPieceUpdate.setId(manualPiece.getId());
        manualPieceUpdate.setSliceBatchNo(sliceBatchNo);
        manualPieceUpdate.setSegmentBatchNo(segmentBatchNo);
        manualPieceUpdate.setMaterialCode(materialCode);
        manualPieceUpdate.setMaterialName(materialName);
        manualPieceUpdate.setModelCode(modelCode);
        manualPieceUpdate.setProductSize(productSize);
        manualPieceUpdate.setProductionDate(productionDate);
        manualPieceUpdate.setExpiryDate(expiryDate);
        manualPieceUpdate.setInspectionResult(inspectionResult);
        manualPieceUpdate.setCoaInspectionResult(coaInspectionResult);
        manualPieceUpdate.setRemark(remark);
        if (labelReprintRequired) {
            // 既有打印日志保留；当前打印状态复位，提示操作员按修正后信息补打标签。
            manualPieceUpdate.setPrintStatus("UNPRINTED");
        }
        hcPackagingManualPieceMapper.updateById(manualPieceUpdate);

        HcInnerPackUnitItemDO packItemUpdate = new HcInnerPackUnitItemDO();
        packItemUpdate.setId(packItem.getId());
        packItemUpdate.setSliceBatchNo(sliceBatchNo);
        packItemUpdate.setQualityStatus(qualityStatus);
        hcInnerPackUnitItemMapper.updateById(packItemUpdate);

        HcInnerPackUnitDO boxUpdate = new HcInnerPackUnitDO();
        boxUpdate.setId(box.getId());
        boxUpdate.setBatchNo(segmentBatchNo);
        boxUpdate.setMaterialCode(materialCode);
        boxUpdate.setMaterialName(materialName);
        boxUpdate.setModelCode(modelCode);
        boxUpdate.setProductSize(productSize);
        hcInnerPackUnitMapper.updateById(boxUpdate);

        // 入库单仍承载当前包装批号；同步后，库存、包装和入库追溯口径保持一致。
        hcFgInboundOrderItemMapper.selectListByOuterBoxIdForUpdate(box.getId()).forEach(inboundItem -> {
            HcFgInboundOrderItemDO inboundItemUpdate = new HcFgInboundOrderItemDO();
            inboundItemUpdate.setId(inboundItem.getId());
            inboundItemUpdate.setBatchNo(segmentBatchNo);
            hcFgInboundOrderItemMapper.updateById(inboundItemUpdate);
        });

        HcFinishedStockDO stockUpdate = new HcFinishedStockDO();
        stockUpdate.setId(stock.getId());
        stockUpdate.setBatchNo(segmentBatchNo);
        stockUpdate.setSliceBatchNo(sliceBatchNo);
        stockUpdate.setMaterialCode(materialCode);
        stockUpdate.setMaterialName(materialName);
        stockUpdate.setModelCode(modelCode);
        stockUpdate.setProductSize(productSize);
        stockUpdate.setProductionDate(productionDate);
        stockUpdate.setExpiryDate(expiryDate);
        stockUpdate.setQualityStatus(qualityStatus);
        hcFinishedStockMapper.updateById(stockUpdate);

        HcFinishedStockDO afterStock = BeanUtil.copyProperties(stock, HcFinishedStockDO.class);
        afterStock.setBatchNo(segmentBatchNo);
        afterStock.setSliceBatchNo(sliceBatchNo);
        afterStock.setMaterialCode(materialCode);
        afterStock.setMaterialName(materialName);
        afterStock.setModelCode(modelCode);
        afterStock.setProductSize(productSize);
        afterStock.setProductionDate(productionDate);
        afterStock.setExpiryDate(expiryDate);
        afterStock.setQualityStatus(qualityStatus);
        HcPackagingManualPieceDO afterManualPiece = BeanUtil.copyProperties(correctedManualPiece,
                HcPackagingManualPieceDO.class);
        if (labelReprintRequired) {
            afterManualPiece.setPrintStatus("UNPRINTED");
        }
        HcInnerPackUnitDO afterBox = BeanUtil.copyProperties(box, HcInnerPackUnitDO.class);
        afterBox.setBatchNo(segmentBatchNo);
        afterBox.setMaterialCode(materialCode);
        afterBox.setMaterialName(materialName);
        afterBox.setModelCode(modelCode);
        afterBox.setProductSize(productSize);
        HcInnerPackUnitItemDO afterPackItem = BeanUtil.copyProperties(packItem, HcInnerPackUnitItemDO.class);
        afterPackItem.setSliceBatchNo(sliceBatchNo);
        afterPackItem.setQualityStatus(qualityStatus);
        hcPackagingManualPieceCorrectionLogMapper.insert(HcPackagingManualPieceCorrectionLogDO.builder()
                .tenantId(stock.getTenantId())
                .manualPieceId(manualPiece.getId())
                .finishedStockId(stock.getId())
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .beforeDataJson(JsonUtils.toJsonString(beforeSnapshot))
                .afterDataJson(JsonUtils.toJsonString(buildImportedStockCorrectionSnapshot(
                        afterManualPiece, afterBox, afterPackItem, afterStock)))
                .correctionReason(correctionReason)
                .operatorName(operatorName)
                .correctionTime(now)
                .labelReprintRequired(labelReprintRequired)
                .build());

        synchronizeStockCoaFreeze(afterStock);
        FgStockLedgerRespVO respVO = BeanUtil.copyProperties(afterStock, FgStockLedgerRespVO.class);
        respVO.setPackageNo(firstNotBlank(afterStock.getInnerUnitNo(), afterStock.getOuterBoxNo()));
        respVO.setInnerUnitNo(afterStock.getInnerUnitNo());
        respVO.setSourceType(SOURCE_MANUAL_HISTORY);
        respVO.setSourceManualPieceId(afterManualPiece.getId());
        respVO.setImportedDataEditable(true);
        respVO.setLabelReprintRequired(labelReprintRequired);
        respVO.setInspectionResult(afterManualPiece.getInspectionResult());
        respVO.setCoaInspectionResult(afterManualPiece.getCoaInspectionResult());
        respVO.setRemark(afterManualPiece.getRemark());
        respVO.setInboundUserName(firstNotBlank(box.getInboundUserName(), box.getRecorderName()));
        return respVO;
    }

    private boolean hasImportedStockCorrectionChanged(HcPackagingManualPieceDO before,
                                                       HcPackagingManualPieceDO after) {
        return !Objects.equals(before.getSliceBatchNo(), after.getSliceBatchNo())
                || !Objects.equals(before.getSegmentBatchNo(), after.getSegmentBatchNo())
                || !Objects.equals(before.getMaterialCode(), after.getMaterialCode())
                || !Objects.equals(before.getMaterialName(), after.getMaterialName())
                || !Objects.equals(before.getModelCode(), after.getModelCode())
                || !Objects.equals(before.getProductSize(), after.getProductSize())
                || !Objects.equals(before.getProductionDate(), after.getProductionDate())
                || !Objects.equals(before.getExpiryDate(), after.getExpiryDate())
                || !Objects.equals(before.getInspectionResult(), after.getInspectionResult())
                || !Objects.equals(before.getCoaInspectionResult(), after.getCoaInspectionResult())
                || !Objects.equals(before.getRemark(), after.getRemark());
    }

    /**
     * 锁定并校验新片号，防止历史导入来源、包装明细或现有库存台账出现重复追溯编号。
     */
    private void validateImportedStockSliceBatchNoUnique(String sliceBatchNo, Long manualPieceId,
                                                          Long packItemId, Long stockId) {
        if (hcCutRoundReportMapper.selectByProductionBatchNo(sliceBatchNo) != null) {
            throw invalidParamException("新片号已存在MES裁切报工，不能重复使用：" + sliceBatchNo);
        }
        boolean manualPieceDuplicated = hcPackagingManualPieceMapper.selectListBySliceBatchNoForUpdate(sliceBatchNo)
                .stream().anyMatch(item -> !Objects.equals(item.getId(), manualPieceId));
        if (manualPieceDuplicated) {
            throw invalidParamException("新片号已存在历史导入来源记录，不能重复使用：" + sliceBatchNo);
        }
        boolean packItemDuplicated = hcInnerPackUnitItemMapper.selectListBySliceBatchNoForUpdate(sliceBatchNo)
                .stream().anyMatch(item -> !Objects.equals(item.getId(), packItemId));
        if (packItemDuplicated) {
            throw invalidParamException("新片号已存在包装片明细，不能重复使用：" + sliceBatchNo);
        }
        boolean stockDuplicated = hcFinishedStockMapper.selectListBySliceBatchNoForUpdate(sliceBatchNo)
                .stream().anyMatch(item -> !Objects.equals(item.getId(), stockId));
        if (stockDuplicated) {
            throw invalidParamException("新片号已存在成品库存，不能重复使用：" + sliceBatchNo);
        }
    }

    private Map<String, Object> buildImportedStockCorrectionSnapshot(HcPackagingManualPieceDO manualPiece,
                                                                      HcInnerPackUnitDO box,
                                                                      HcInnerPackUnitItemDO packItem,
                                                                      HcFinishedStockDO stock) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("sliceBatchNo", stock.getSliceBatchNo());
        snapshot.put("segmentBatchNo", manualPiece.getSegmentBatchNo());
        snapshot.put("innerUnitBatchNo", box.getBatchNo());
        snapshot.put("stockBatchNo", stock.getBatchNo());
        snapshot.put("materialCode", stock.getMaterialCode());
        snapshot.put("materialName", stock.getMaterialName());
        snapshot.put("modelCode", stock.getModelCode());
        snapshot.put("productSize", stock.getProductSize());
        snapshot.put("productionDate", stock.getProductionDate());
        snapshot.put("expiryDate", stock.getExpiryDate());
        snapshot.put("inspectionResult", manualPiece.getInspectionResult());
        snapshot.put("coaInspectionResult", manualPiece.getCoaInspectionResult());
        snapshot.put("qualityStatus", packItem.getQualityStatus());
        snapshot.put("remark", manualPiece.getRemark());
        snapshot.put("innerUnitNo", box.getInnerUnitNo());
        snapshot.put("stockNo", stock.getStockNo());
        return snapshot;
    }

    @Override
    public PageResult<FgStockHistoryLedgerRespVO> getFgStockHistoryLedgerPage(FgStockHistoryLedgerPageReqVO reqVO) {
        PageResult<HcFinishedStockTxnLogDO> pageResult = hcFinishedStockTxnLogMapper.selectHistoryLedgerPage(reqVO);
        return new PageResult<>(pageResult.getList().stream()
                .map(item -> {
                    FgStockHistoryLedgerRespVO respVO = BeanUtil.copyProperties(item, FgStockHistoryLedgerRespVO.class);
                    respVO.setManualOutboundRepackReturnable(isManualOutboundRepackReturnable(item));
                    return respVO;
                })
                .toList(), pageResult.getTotal());
    }

    @Override
    public PageResult<PackageAuxStockRespVO> getPackageAuxStockPage(PackageAuxStockPageReqVO reqVO) {
        PageResult<HcPackageAuxStockDO> pageResult = hcPackageAuxStockMapper.selectPage(reqVO);
        return new PageResult<>(
                pageResult.getList().stream().map(this::buildPackageAuxStockResp).toList(),
                pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePackageAuxStock(PackageAuxStockSaveReqVO reqVO) {
        String batchNo = StrUtil.trimToEmpty(reqVO.getBatchNo());
        if (StrUtil.isBlank(batchNo)) {
            throw invalidParamException("辅材批次不能为空");
        }
        HcPackageAuxStockDO old = reqVO.getId() == null ? null : getPackageAuxStock(reqVO.getId());
        HcPackageAuxStockDO sameBatch = hcPackageAuxStockMapper.selectByBatchNo(batchNo);
        if (sameBatch != null && (old == null || !Objects.equals(sameBatch.getId(), old.getId()))) {
            throw invalidParamException("辅材边库批次已存在，不能重复登记");
        }
        BigDecimal receiveQty = positiveDecimal(reqVO.getReceiveQty(), "入边库数量必须大于0");
        BigDecimal usedQty = old == null ? BigDecimal.ZERO : zeroIfNull(old.getUsedQty());
        if (receiveQty.compareTo(usedQty) < 0) {
            throw invalidParamException("入边库数量不能小于已消耗数量");
        }
        BigDecimal availableQty = receiveQty.subtract(usedQty);
        LocalDateTime now = LocalDateTime.now();
        HcPackageAuxStockDO stock = old == null ? new HcPackageAuxStockDO() : old;
        stock.setAuxCategory(firstNotBlank(reqVO.getAuxCategory(), AUX_CATEGORY_PACKAGING_BAG));
        stock.setAuxCategoryName(firstNotBlank(reqVO.getAuxCategoryName(), AUX_CATEGORY_PACKAGING_BAG_NAME));
        stock.setMaterialCode(StrUtil.trimToEmpty(reqVO.getMaterialCode()));
        stock.setMaterialName(StrUtil.trimToEmpty(reqVO.getMaterialName()));
        stock.setAuxSpec(StrUtil.trimToNull(reqVO.getAuxSpec()));
        stock.setBatchNo(batchNo);
        stock.setSourceWarehouseCode(StrUtil.trimToNull(reqVO.getSourceWarehouseCode()));
        stock.setSourceWarehouseName(StrUtil.trimToNull(reqVO.getSourceWarehouseName()));
        stock.setEdgeWarehouseCode(StrUtil.trimToNull(reqVO.getEdgeWarehouseCode()));
        stock.setEdgeWarehouseName(firstNotBlank(reqVO.getEdgeWarehouseName(), DEFAULT_AUX_EDGE_WAREHOUSE_NAME));
        stock.setStockMeasureMode(AUX_STOCK_MEASURE_COUNT);
        stock.setReceiveQty(receiveQty);
        stock.setUsedQty(usedQty);
        stock.setAvailableQty(availableQty);
        stock.setStockStatus(availableQty.compareTo(BigDecimal.ZERO) > 0 ? AUX_STOCK_ACTIVE : AUX_STOCK_USED_UP);
        stock.setReceiverId(SecurityFrameworkUtils.getLoginUserId());
        stock.setReceiverName(firstNotBlank(reqVO.getReceiverName(), currentUserName()));
        stock.setReceiveDate(reqVO.getReceiveDate() == null ? LocalDate.now() : reqVO.getReceiveDate());
        stock.setReceiveTime(reqVO.getReceiveTime() == null ? now : reqVO.getReceiveTime());
        stock.setErpTransferNo(StrUtil.trimToNull(reqVO.getErpTransferNo()));
        stock.setTransferQty(reqVO.getTransferQty());
        stock.setTransferUnit(firstNotBlank(reqVO.getTransferUnit(), "包"));
        stock.setUnpackQty(reqVO.getUnpackQty());
        stock.setUnpackUnit(firstNotBlank(reqVO.getUnpackUnit(), "个"));
        stock.setErpTransferStatus(firstNotBlank(old == null ? null : old.getErpTransferStatus(), "NOT_SYNCED"));
        stock.setRemark(StrUtil.trimToNull(reqVO.getRemark()));
        if (old == null) {
            stock.setPrintCount(0);
            stock.setTenantId(currentTenantId());
            hcPackageAuxStockMapper.insert(stock);
        } else {
            hcPackageAuxStockMapper.updateById(stock);
        }
        return stock.getId();
    }

    @Override
    public List<PackageAuxStockRespVO> getPackageAuxAvailableList() {
        return hcPackageAuxStockMapper.selectAvailableList().stream()
                .map(this::buildPackageAuxStockResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageAuxStockRespVO printPackageAuxStock(BoxActionReqVO reqVO) {
        HcPackageAuxStockDO stock = getPackageAuxStock(reqVO.getId());
        HcPackageAuxStockDO update = new HcPackageAuxStockDO();
        update.setId(stock.getId());
        update.setPrintCount(intValue(stock.getPrintCount()) + 1);
        update.setPrintTime(LocalDateTime.now());
        hcPackageAuxStockMapper.updateById(update);
        return buildPackageAuxStockResp(hcPackageAuxStockMapper.selectById(stock.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PackageAuxConsumeRecordRespVO consumePackageAux(PackageAuxConsumeReqVO reqVO) {
        BigDecimal consumeQty = positiveDecimal(reqVO.getConsumeQty(), "消耗数量必须大于0");
        String materialCode = StrUtil.trimToEmpty(reqVO.getMaterialCode());
        HcPackageAuxStockDO stock = resolvePackageAuxStock(reqVO, materialCode);
        if (stock != null) {
            stock = hcPackageAuxStockMapper.selectByIdForUpdate(stock.getId());
        }
        if (stock == null) {
            throw invalidParamException(StrUtil.isBlank(materialCode) ? "包装辅材边库批次不存在" : "耗材料号无可用边库库存");
        }
        if (!AUX_STOCK_ACTIVE.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), ""))) {
            throw invalidParamException("当前包装辅材批次不可用");
        }
        BigDecimal beforeAvailableQty = zeroIfNull(stock.getAvailableQty());
        if (beforeAvailableQty.compareTo(consumeQty) < 0) {
            throw invalidParamException("包装辅材余量不足，当前可用 " + beforeAvailableQty.stripTrailingZeros().toPlainString());
        }
        BigDecimal afterAvailableQty = beforeAvailableQty.subtract(consumeQty);
        LocalDateTime now = LocalDateTime.now();
        LocalDate recordDate = reqVO.getRecordDate() == null ? LocalDate.now() : reqVO.getRecordDate();
        String recorderName = firstNotBlank(reqVO.getRecorderName(), currentUserName());
        String bizType = firstNotBlank(reqVO.getBizType(), AUX_BIZ_FG_INBOUND);
        HcPackageAuxConsumeRecordDO record = HcPackageAuxConsumeRecordDO.builder()
                .tenantId(stock.getTenantId())
                .stockId(stock.getId())
                .auxCategory(stock.getAuxCategory())
                .auxCategoryName(stock.getAuxCategoryName())
                .materialCode(stock.getMaterialCode())
                .materialName(firstNotBlank(stock.getMaterialName(), reqVO.getMaterialName()))
                .auxSpec(firstNotBlank(stock.getAuxSpec(), reqVO.getAuxSpec(), reqVO.getModelCode()))
                .batchNo(stock.getBatchNo())
                .bizType(bizType)
                .bizNo(StrUtil.trimToNull(reqVO.getBizNo()))
                .consumeQty(consumeQty)
                .beforeAvailableQty(beforeAvailableQty)
                .afterAvailableQty(afterAvailableQty)
                .consumeStatus(AUX_CONSUME_RECORDED)
                .recordDate(recordDate)
                .recorderId(SecurityFrameworkUtils.getLoginUserId())
                .recorderName(recorderName)
                .recorderTime(now)
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .build();
        hcPackageAuxConsumeRecordMapper.insert(record);

        HcPackageAuxStockDO update = new HcPackageAuxStockDO();
        update.setId(stock.getId());
        update.setUsedQty(zeroIfNull(stock.getUsedQty()).add(consumeQty));
        update.setAvailableQty(afterAvailableQty);
        update.setStockStatus(afterAvailableQty.compareTo(BigDecimal.ZERO) > 0 ? AUX_STOCK_ACTIVE : AUX_STOCK_USED_UP);
        hcPackageAuxStockMapper.updateById(update);
        return buildPackageAuxConsumeRecordResp(record);
    }

    private List<HcToolingConsumableConsumeDO> consumePackageAuxItems(List<PackageAuxConsumeItemReqVO> consumeItems,
                                                                        String bizType, String bizNo, String recorderName,
                                                                        String remark) {
        if (consumeItems == null || consumeItems.isEmpty()) {
            throw invalidParamException("请至少选择一条包装辅材");
        }
        if (StrUtil.isBlank(bizType) || StrUtil.isBlank(bizNo)) {
            throw invalidParamException("包装辅材领用缺少业务类型或业务单号");
        }

        Map<Long, BigDecimal> consumeQtyByLedgerId = new LinkedHashMap<>();
        for (PackageAuxConsumeItemReqVO item : consumeItems) {
            if (item == null || item.getLedgerId() == null) {
                throw invalidParamException("包装工序耗材领用台账不能为空");
            }
            BigDecimal consumeQty = positiveDecimal(item.getConsumeQty(), "包装辅材领用数量必须大于0");
            consumeQtyByLedgerId.merge(item.getLedgerId(), consumeQty, BigDecimal::add);
        }

        LocalDateTime now = LocalDateTime.now();
        List<HcToolingConsumableConsumeDO> records = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : consumeQtyByLedgerId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .toList()) {
            Long ledgerId = entry.getKey();
            BigDecimal consumeQty = entry.getValue();
            HcToolingConsumableLedgerDO ledger = hcToolingConsumableLedgerMapper.selectByIdForUpdate(ledgerId);
            if (ledger == null) {
                throw invalidParamException("包装工序耗材领用台账不存在或已删除");
            }
            if (!PACKAGING_PROCESS_CODE.equalsIgnoreCase(StrUtil.trimToEmpty(ledger.getProcessCode()))) {
                throw invalidParamException("只能选择包装工序耗材领用台账中的批次");
            }
            if (!TOOLING_LEDGER_USAGE_ACTIVE.equalsIgnoreCase(StrUtil.trimToEmpty(ledger.getUsageStatus()))) {
                throw invalidParamException("包装耗材 " + firstNotBlank(ledger.getConsumableTypeName(), ledger.getBatchNo())
                        + " 已标记为用完，不能继续领用");
            }
            BigDecimal beforeAvailableQty = zeroIfNull(ledger.getReceiveQty()).subtract(
                    hcToolingConsumableConsumeMapper.sumConsumeQtyByLedgerId(ledgerId, null));
            if (beforeAvailableQty.compareTo(consumeQty) < 0) {
                throw invalidParamException("包装辅材 " + firstNotBlank(ledger.getConsumableTypeName(), ledger.getBatchNo())
                        + " 余量不足，当前可用 "
                        + beforeAvailableQty.stripTrailingZeros().toPlainString());
            }
            HcToolingConsumableConsumeDO record = HcToolingConsumableConsumeDO.builder()
                    .tenantId(ledger.getTenantId())
                    .ledgerId(ledger.getId())
                    .consumableType(ledger.getConsumableType())
                    .consumableTypeName(ledger.getConsumableTypeName())
                    .processCode(ledger.getProcessCode())
                    .processName(ledger.getProcessName())
                    .model(ledger.getModel())
                    .batchNo(ledger.getBatchNo())
                    .consumeQty(consumeQty)
                    .consumeTime(now)
                    .consumeSource(bizType)
                    .consumeType(TOOLING_CONSUME_TYPE_NORMAL)
                    .remark(remark + "；包装单号：" + bizNo + "；操作人：" + recorderName)
                    .build();
            hcToolingConsumableConsumeMapper.insert(record);
            records.add(record);
        }
        return records;
    }

    private String joinPackageAuxMaterialNames(List<HcToolingConsumableConsumeDO> records) {
        List<String> materialNames = records.stream()
                .map(HcToolingConsumableConsumeDO::getConsumableTypeName)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();
        if (materialNames.isEmpty()) {
            throw invalidParamException("包装辅材名称不能为空");
        }
        return String.join("、", materialNames);
    }

    @Override
    public List<PackageAuxConsumeSummaryRespVO> getPackageAuxTodaySummary(LocalDate recordDate) {
        LocalDate queryDate = recordDate == null ? LocalDate.now() : recordDate;
        Map<String, PackageAuxConsumeSummaryRespVO> summaryMap = new LinkedHashMap<>();
        for (HcPackageAuxConsumeRecordDO record : hcPackageAuxConsumeRecordMapper.selectListByRecordDate(queryDate)) {
            String key = record.getStockId() + "|" + record.getBatchNo();
            PackageAuxConsumeSummaryRespVO summary = summaryMap.computeIfAbsent(key, ignored -> {
                PackageAuxConsumeSummaryRespVO item = new PackageAuxConsumeSummaryRespVO();
                item.setAuxCategory(record.getAuxCategory());
                item.setAuxCategoryName(record.getAuxCategoryName());
                item.setMaterialCode(record.getMaterialCode());
                item.setMaterialName(record.getMaterialName());
                item.setAuxSpec(record.getAuxSpec());
                item.setBatchNo(record.getBatchNo());
                item.setTotalConsumeQty(BigDecimal.ZERO);
                item.setRecordCount(0);
                return item;
            });
            summary.setTotalConsumeQty(zeroIfNull(summary.getTotalConsumeQty()).add(zeroIfNull(record.getConsumeQty())));
            summary.setRecordCount(intValue(summary.getRecordCount()) + 1);
        }
        return new ArrayList<>(summaryMap.values());
    }

    @Override
    public List<PackageAuxConsumeRecordRespVO> getPackageAuxTodayList(LocalDate recordDate) {
        LocalDate queryDate = recordDate == null ? LocalDate.now() : recordDate;
        return hcPackageAuxConsumeRecordMapper.selectListByRecordDate(queryDate).stream()
                .map(this::buildPackageAuxConsumeRecordResp)
                .toList();
    }

    @Override
    public List<PackageAuxConsumeRecordRespVO> getPackageAuxBizList(String bizType, String bizNo) {
        String normalizedBizType = firstNotBlank(bizType, AUX_BIZ_FG_INBOUND);
        String normalizedBizNo = StrUtil.trimToNull(bizNo);
        if (StrUtil.isBlank(normalizedBizNo)) {
            return Collections.emptyList();
        }
        return hcPackageAuxConsumeRecordMapper.selectListByBiz(normalizedBizType, normalizedBizNo).stream()
                .map(this::buildPackageAuxConsumeRecordResp)
                .toList();
    }

    @Override
    public List<InboundTaskRespVO> getInboundTaskList(String keyword) {
        return hcFinishedPackagingMapper.selectInboundTaskList(StrUtil.trimToNull(keyword));
    }

    @Override
    public List<HcPackagingSourceRespVO> getInboundSourceList(Long planId, String motherSegmentBatchNo) {
        if (planId == null || StrUtil.isBlank(motherSegmentBatchNo)) {
            return Collections.emptyList();
        }
        List<HcPackagingSourceRespVO> sources = hcFinishedPackagingMapper.selectInboundSourceList(planId, motherSegmentBatchNo);
        fillPackagingSourceQualityStatus(sources);
        return sources;
    }

    @Override
    public List<InboundBoxRespVO> getInboundBoxList(Long planOperationId, String motherSegmentBatchNo) {
        if (planOperationId == null || StrUtil.isBlank(motherSegmentBatchNo)) {
            return Collections.emptyList();
        }
        return hcInnerPackUnitMapper.selectListByPlanOperationIdAndBatchNo(planOperationId, motherSegmentBatchNo).stream()
                .map(this::buildInboundBoxResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> initInboundBoxes(InitInboundBoxesReqVO reqVO) {
        HcPlanOrderDO planOrder = validatePlan(reqVO.getPlanId());
        HcPlanOrderOperationDO operation = validateOperation(planOrder, reqVO.getPlanOperationId());
        int boxCount = positive(reqVO.getBoxCount(), "包装盒数必须大于0");
        int packageSpec = positive(reqVO.getPackageSpec(), "一盒片数必须大于0");
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < boxCount; i++) {
            HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                    .tenantId(planOrder.getTenantId())
                    .innerUnitNo(nextNo("FGP"))
                    .planId(planOrder.getId())
                    .planNo(planOrder.getPlanNo())
                    .planOperationId(operation.getId())
                    .packageSpec(packageSpec)
                    .targetQty(packageSpec)
                    .currentQty(0)
                    .materialCode(planOrder.getMaterialCode())
                    .materialName(planOrder.getMaterialName())
                    .modelCode(firstNotBlank(planOrder.getModelCode(), planOrder.getModelName()))
                    .batchNo(reqVO.getMotherSegmentBatchNo())
                    .productSize(firstNotBlank(planOrder.getSizeName(), planOrder.getSizeSpec()))
                    .packageDate(LocalDate.now())
                    .unitStatus(STATUS_WAITING_PIECE)
                    .printCount(0)
                    .backfillFlag(false)
                    .recorderName(firstNotBlank(reqVO.getRecorderName(), currentUserName()))
                    .recorderTime(now)
                    .remark(reqVO.getRemark())
                    .build();
            hcInnerPackUnitMapper.insert(box);
            ids.add(box.getId());
        }
        return ids;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long scanInboundPiece(ScanInboundPieceReqVO reqVO) {
        HcInnerPackUnitDO box = getInboundBox(reqVO.getBoxId());
        if (!List.of(STATUS_WAITING_PIECE, STATUS_PACKED).contains(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            throw invalidParamException("当前包装盒状态不允许继续扫码");
        }
        if (intValue(box.getCurrentQty()) >= intValue(box.getTargetQty())) {
            throw invalidParamException("当前包装盒已满，不能继续加入成品片");
        }
        HcPackagingSourceRespVO source = hcFinishedPackagingMapper.selectInboundSource(
                box.getPlanId(), box.getBatchNo(), StrUtil.trimToEmpty(reqVO.getSliceBatchNo()));
        if (source == null) {
            throw invalidParamException("未在当前计划母卷分段下找到已确认裁切片号");
        }
        if (hcInnerPackUnitItemMapper.selectBySliceBatchNo(source.getProductionBatchNo()) != null) {
            throw invalidParamException("该成品片号已完成包装，不能重复扫描");
        }
        Map<String, CoaInspectionMeta> coaInspectionMap = buildCoaInspectionMapForPackagingSources(List.of(source));
        LocalDateTime now = LocalDateTime.now();
        HcInnerPackUnitItemDO item = HcInnerPackUnitItemDO.builder()
                .tenantId(box.getTenantId())
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .planId(box.getPlanId())
                .planNo(box.getPlanNo())
                .planOperationId(box.getPlanOperationId())
                .sourceType(SOURCE_CUT_ROUND_REPORT)
                .sourceCutRoundReportId(source.getSourceCutRoundReportId())
                .sliceBatchNo(source.getProductionBatchNo())
                .productionBatchNo(source.getProductionBatchNo())
                .qualityStatus(resolvePackagingSourceQualityStatus(source, coaInspectionMap))
                .scanUserName(firstNotBlank(reqVO.getScanUserName(), currentUserName()))
                .scanTime(now)
                .build();
        hcInnerPackUnitItemMapper.insert(item);
        refreshInboundBoxQty(box.getId());
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long printInboundBox(BoxActionReqVO reqVO) {
        HcInnerPackUnitDO box = getInboundBox(reqVO.getId());
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setLabelNo(firstNotBlank(box.getLabelNo(), "LBL-" + box.getInnerUnitNo()));
        update.setPrintCount(intValue(box.getPrintCount()) + 1);
        update.setLastPrintTime(LocalDateTime.now());
        hcInnerPackUnitMapper.updateById(update);
        return box.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmInboundBox(BoxActionReqVO reqVO) {
        HcInnerPackUnitDO box = getInboundBox(reqVO.getId());
        List<HcInnerPackUnitItemDO> items = hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId());
        if (items.isEmpty()) {
            throw invalidParamException("当前包装盒没有成品片，不能确认入库");
        }
        LocalDateTime now = LocalDateTime.now();
        String inboundNo = "FGI-" + box.getInnerUnitNo();
        HcFgInboundOrderDO inboundOrder = hcFgInboundOrderMapper.selectByInboundNo(inboundNo);
        if (inboundOrder == null) {
            inboundOrder = HcFgInboundOrderDO.builder()
                    .tenantId(box.getTenantId())
                    .inboundNo(inboundNo)
                    .planId(box.getPlanId())
                    .planNo(box.getPlanNo())
                    .planOperationId(box.getPlanOperationId())
                    .inboundStatus(STATUS_INBOUNDED)
                    .totalBoxCount(1)
                    .totalPieceCount(items.size())
                    .recorderName(currentUserName())
                    .recorderTime(now)
                    .inboundUserName(firstNotBlank(reqVO.getOperatorName(), currentUserName()))
                    .inboundTime(now)
                    .remark(reqVO.getReason())
                    .build();
            hcFgInboundOrderMapper.insert(inboundOrder);
            hcFgInboundOrderItemMapper.insert(HcFgInboundOrderItemDO.builder()
                    .tenantId(box.getTenantId())
                    .inboundOrderId(inboundOrder.getId())
                    .inboundNo(inboundNo)
                    .outerBoxId(box.getId())
                    .outerBoxNo(box.getInnerUnitNo())
                    .pieceQty(items.size())
                    .materialCode(box.getMaterialCode())
                    .modelCode(box.getModelCode())
                    .batchNo(box.getBatchNo())
                    .qualityStatus(resolvePackageQualityStatus(items))
                    .build());
        }
        for (HcInnerPackUnitItemDO item : items) {
            HcFinishedStockDO existed = hcFinishedStockMapper.selectBySliceBatchNo(item.getSliceBatchNo());
            if (existed == null) {
                HcFinishedStockDO stock = HcFinishedStockDO.builder()
                        .tenantId(box.getTenantId())
                        .stockNo(nextNo("FGS"))
                        .outerBoxNo(box.getInnerUnitNo())
                        .innerUnitNo(box.getInnerUnitNo())
                        .sliceBatchNo(item.getSliceBatchNo())
                        .materialCode(box.getMaterialCode())
                        .materialName(box.getMaterialName())
                        .modelCode(box.getModelCode())
                        .batchNo(box.getBatchNo())
                        .productSize(box.getProductSize())
                        .qty(1)
                        .qualityStatus(firstNotBlank(item.getQualityStatus(), INSPECTION_RESULT_OK))
                        .stockStatus(STATUS_AVAILABLE)
                        .inboundNo(inboundNo)
                        .inboundTime(now)
                        .build();
                hcFinishedStockMapper.insert(stock);
                synchronizeStockCoaFreeze(stock);
                recordFinishedStockHistory(null, stock, "FG_INBOUND", now,
                        firstNotBlank(reqVO.getOperatorName(), currentUserName()), "FG_INBOUND_ORDER",
                        inboundOrder.getId(), inboundNo, reqVO.getReason());
            }
        }
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setUnitStatus(STATUS_INBOUNDED);
        hcInnerPackUnitMapper.updateById(update);
        return inboundOrder.getId();
    }

    @Override
    public PageResult<ShippingNoticeRespVO> getShippingNoticePage(ShippingNoticePageReqVO reqVO) {
        PageResult<HcFgShippingNoticeDO> pageResult = hcFgShippingNoticeMapper.selectPage(reqVO);
        return new PageResult<>(
                pageResult.getList().stream().map(notice -> buildShippingNoticeResp(notice, false)).toList(),
                pageResult.getTotal());
    }

    @Override
    public ShippingNoticeRespVO getShippingNotice(Long id) {
        return buildShippingNoticeResp(getShippingNoticeOrThrow(id), true);
    }

    @Override
    public PageResult<FgStockLedgerRespVO> getShippingNoticeStockCandidatePage(FgStockLedgerPageReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 20 : Math.min(200, Math.max(1, reqVO.getPageSize()));
        int offset = Math.max(0, (pageNo - 1) * pageSize);
        Long total = hcFinishedPackagingMapper.countShippingNoticeStockCandidatePage(reqVO);
        if (total == null || total <= 0) {
            return PageResult.empty();
        }
        return new PageResult<>(
                hcFinishedPackagingMapper.selectShippingNoticeStockCandidatePage(reqVO, offset, pageSize),
                total);
    }

    @Override
    public PageResult<FgShippingBatchCandidateRespVO> getShippingNoticeBatchCandidatePage(FgShippingBatchCandidatePageReqVO reqVO) {
        int pageNo = reqVO.getPageNo() == null ? 1 : reqVO.getPageNo();
        int pageSize = reqVO.getPageSize() == null ? 20 : Math.min(200, Math.max(1, reqVO.getPageSize()));
        int offset = Math.max(0, (pageNo - 1) * pageSize);
        Long total = hcFinishedPackagingMapper.countShippingNoticeInternalItemCandidatePage(reqVO);
        if (total == null || total <= 0) {
            return PageResult.empty();
        }
        return new PageResult<>(
                hcFinishedPackagingMapper.selectShippingNoticeInternalItemCandidatePage(reqVO, offset, pageSize),
                total);
    }

    @Override
    public PageResult<FgStockLedgerRespVO> getShippingNoticePickCandidatePage(ShippingNoticePickCandidatePageReqVO reqVO) {
        prepareShippingNoticePickCandidateQuery(reqVO);
        List<FgStockLedgerRespVO> candidates = listAllShippingNoticePickCandidates(reqVO);
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(1, reqVO.getPageNo());
        int pageSize = reqVO.getPageSize() == null ? 20 : Math.min(200, Math.max(1, reqVO.getPageSize()));
        int start = (int) Math.min((long) (pageNo - 1) * pageSize, candidates.size());
        return new PageResult<>(candidates.subList(start, Math.min(start + pageSize, candidates.size())), (long) candidates.size());
    }

    @Override
    public PageResult<ShippingNoticePickCandidateSegmentRespVO> getShippingNoticePickCandidateSegmentPage(
            ShippingNoticePickCandidatePageReqVO reqVO) {
        prepareShippingNoticePickCandidateQuery(reqVO);
        List<ShippingNoticePickCandidateSegmentRespVO> segments = buildShippingNoticePickCandidateSegments(
                listAllShippingNoticePickCandidates(reqVO));
        int pageNo = reqVO.getPageNo() == null ? 1 : Math.max(1, reqVO.getPageNo());
        int pageSize = reqVO.getPageSize() == null ? 20 : Math.min(200, Math.max(1, reqVO.getPageSize()));
        long fromIndexValue = (long) (pageNo - 1) * pageSize;
        if (fromIndexValue >= segments.size()) {
            return new PageResult<>(Collections.emptyList(), (long) segments.size());
        }
        int fromIndex = (int) fromIndexValue;
        int toIndex = Math.min(segments.size(), fromIndex + pageSize);
        return new PageResult<>(new ArrayList<>(segments.subList(fromIndex, toIndex)), (long) segments.size());
    }

    @Override
    public List<FgStockLedgerRespVO> getShippingNoticePickCandidateSegmentPieceList(
            ShippingNoticePickCandidatePageReqVO reqVO) {
        prepareShippingNoticePickCandidateQuery(reqVO);
        if (StrUtil.isBlank(reqVO.getSegmentBatchNo())) {
            throw invalidParamException("分段批号不能为空");
        }
        String segmentKey = normalizeShippingPickSegmentKey(reqVO.getSegmentBatchNo());
        return listAllShippingNoticePickCandidates(reqVO).stream()
                .filter(row -> segmentKey.equals(normalizeShippingPickSegmentKey(row.getMaterialCode())))
                .toList();
    }

    private void prepareShippingNoticePickCandidateQuery(ShippingNoticePickCandidatePageReqVO reqVO) {
        HcFgShippingNoticeDO notice = getShippingNoticeOrThrow(reqVO.getNoticeId());
        if (!NOTICE_EXECUTING_STATUSES.contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有执行中的发货需求单允许查询配货库存");
        }
        if (StrUtil.isBlank(reqVO.getQualityStatus())) {
            reqVO.setQualityStatus(INSPECTION_RESULT_OK);
        }
    }

    private List<FgStockLedgerRespVO> listAllShippingNoticePickCandidates(
            ShippingNoticePickCandidatePageReqVO reqVO) {
        Long total = hcFinishedPackagingMapper.countShippingNoticePickCandidatePage(reqVO);
        if (total == null || total <= 0) {
            return Collections.emptyList();
        }
        List<FgStockLedgerRespVO> candidates = new ArrayList<>(Math.toIntExact(Math.min(total, Integer.MAX_VALUE)));
        for (int offset = 0; offset < total; offset += PICK_CANDIDATE_QUERY_BATCH_SIZE) {
            List<FgStockLedgerRespVO> rows = hcFinishedPackagingMapper.selectShippingNoticePickCandidatePage(
                    reqVO, offset, PICK_CANDIDATE_QUERY_BATCH_SIZE);
            if (rows == null || rows.isEmpty()) {
                break;
            }
            candidates.addAll(rows);
            if (rows.size() < PICK_CANDIDATE_QUERY_BATCH_SIZE) {
                break;
            }
        }
        Set<String> segments = candidates.stream().map(row -> normalizeCoaSegmentBatchNo(row.getSliceBatchNo()))
                .filter(StrUtil::isNotBlank).collect(java.util.stream.Collectors.toSet());
        Map<String, CoaInspectionMeta> coa = buildCoaInspectionMap(segments);
        return candidates.stream().filter(row -> {
            CoaInspectionMeta meta = coa.get(normalizeCoaSegmentBatchNo(row.getSliceBatchNo()));
            return meta == null || meta.isReleased();
        }).toList();
    }

    private List<ShippingNoticePickCandidateSegmentRespVO> buildShippingNoticePickCandidateSegments(
            List<FgStockLedgerRespVO> candidates) {
        Map<String, List<FgStockLedgerRespVO>> segmentMap = new LinkedHashMap<>();
        for (FgStockLedgerRespVO candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            segmentMap.computeIfAbsent(normalizeShippingPickSegmentKey(candidate.getMaterialCode()),
                    ignored -> new ArrayList<>()).add(candidate);
        }
        List<ShippingNoticePickCandidateSegmentRespVO> segments = new ArrayList<>(segmentMap.size());
        for (List<FgStockLedgerRespVO> rows : segmentMap.values()) {
            if (rows.isEmpty()) {
                continue;
            }
            FgStockLedgerRespVO first = rows.get(0);
            ShippingNoticePickCandidateSegmentRespVO segment = new ShippingNoticePickCandidateSegmentRespVO();
            segment.setSegmentBatchNo(StrUtil.blankToDefault(StrUtil.trim(first.getMaterialCode()),
                    UNIDENTIFIED_PICK_SEGMENT_LABEL));
            segment.setModelCode(first.getModelCode());
            segment.setTotalPieceCount(rows.size());
            segment.setWarehousePieceCount((int) rows.stream()
                    .filter(row -> !PICK_SOURCE_PACKAGING_DIRECT.equalsIgnoreCase(row.getCandidateType()))
                    .count());
            segment.setPackagingDirectPieceCount((int) rows.stream()
                    .filter(row -> PICK_SOURCE_PACKAGING_DIRECT.equalsIgnoreCase(row.getCandidateType()))
                    .count());
            segment.setSampleSliceBatchNos(rows.stream()
                    .map(FgStockLedgerRespVO::getSliceBatchNo)
                    .map(StrUtil::trimToNull)
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(3)
                    .toList());
            segments.add(segment);
        }
        return segments;
    }

    private String normalizeShippingPickSegmentKey(String segmentBatchNo) {
        return StrUtil.blankToDefault(StrUtil.trim(segmentBatchNo), UNIDENTIFIED_PICK_SEGMENT_LABEL)
                .toUpperCase(Locale.ROOT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO pickShippingNotice(ShippingNoticePickReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!List.of(NOTICE_STATUS_SUBMITTED, NOTICE_STATUS_PICKED).contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有已提交或已配货需求单允许配货领用");
        }
        List<HcFgShippingNoticeItemDO> noticeItems = selectShippingExecutionItems(notice);
        Map<Long, HcFgShippingNoticeItemDO> itemMap = new LinkedHashMap<>();
        noticeItems.forEach(item -> itemMap.put(item.getId(), item));
        Map<Long, Integer> noticeItemPickedCountMap = buildNoticeItemPickedCountMap(notice.getId());
        int requiredQty = intValue(firstPositive(notice.getRequiredShipQty(), notice.getNoticeQty()));
        validateShippingPickQuantity(countEffectiveShippingPickedQty(notice.getId()), reqVO.getItems().size(),
                requiredQty, Boolean.TRUE.equals(reqVO.getConfirmPicked()));
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        Map<String, DirectPickSource> directPickSourceMap = prepareDirectPickSources(
                reqVO.getItems(), notice, operatorName, now);
        Set<String> selectedCandidateKeys = new HashSet<>();
        Set<Long> selectedStockIds = new HashSet<>();
        List<HcFinishedStockDO> selectedStocks = new ArrayList<>();
        for (ShippingNoticePickItemReqVO pickItem : reqVO.getItems()) {
            String pickSourceType = resolvePickSourceType(pickItem);
            String candidateKey = resolvePickCandidateKey(pickItem, pickSourceType);
            if (!selectedCandidateKeys.add(candidateKey)) {
                throw invalidParamException("配货候选重复：" + candidateKey);
            }
            if (StrUtil.isNotBlank(pickItem.getCandidateKey())
                    && !matchesSubmittedCandidateKey(pickItem, candidateKey)) {
                throw invalidParamException("配货候选标识已变化，请刷新后重试：" + pickItem.getCandidateKey());
            }
            HcFgShippingNoticeItemDO noticeItem = pickItem.getNoticeItemId() == null
                    ? null : itemMap.get(pickItem.getNoticeItemId());
            if (pickItem.getNoticeItemId() != null && noticeItem == null) {
                throw invalidParamException("配货明细不属于当前发货需求单");
            }
            DirectPickSource directPickSource = PICK_SOURCE_PACKAGING_DIRECT.equals(pickSourceType)
                    ? directPickSourceMap.get(candidateKey) : null;
            if (PICK_SOURCE_PACKAGING_DIRECT.equals(pickSourceType) && directPickSource == null) {
                throw invalidParamException("包装直发候选已变化，请刷新后重试：" + candidateKey);
            }
            HcFinishedStockDO stock = directPickSource == null
                    ? lockWarehousePickStock(pickItem.getStockId(), selectedStockIds)
                    : directPickSource.stock();
            noticeItem = noticeItem == null ? findMatchedPickRequirement(noticeItems, stock, noticeItemPickedCountMap) : noticeItem;
            if (noticeItem == null) {
                throw invalidParamException("配货库存不满足发货明细固定条件：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            validateShippingNoticeItemPickCapacity(noticeItem, noticeItemPickedCountMap);
            if (!INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.blankToDefault(stock.getQualityStatus(), ""))) {
                throw invalidParamException("只允许选择检验合格库存：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            HcFgShippingNoticeItemDO activeLock = hcFgShippingNoticeItemMapper.selectActiveByFinishedStockId(stock.getId());
            if (activeLock != null) {
                throw invalidParamException("该片号已被其它发货需求单配货锁定：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            HcFgShippingNoticePickItemDO activePick = hcFgShippingNoticePickItemMapper.selectActiveByFinishedStockId(stock.getId());
            if (activePick != null) {
                throw invalidParamException("该片号已被其它发货需求单配货锁定：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            validatePickStockMatches(noticeItem, stock);
            if (PICK_SOURCE_WAREHOUSE_STOCK.equals(pickSourceType)) {
                selectedStocks.add(stock);
            }

            hcFgShippingNoticePickItemMapper.insert(HcFgShippingNoticePickItemDO.builder()
                    .tenantId(notice.getTenantId())
                    .noticeId(notice.getId())
                    .noticeNo(notice.getNoticeNo())
                    .sourceNoticeItemId(noticeItem.getId())
                    .pickSourceType(pickSourceType)
                    .sourceInnerPackItemId(directPickSource == null || directPickSource.packItem() == null
                            ? null : directPickSource.packItem().getId())
                    .sourceCutRoundReportId(directPickSource == null || directPickSource.cutRoundReport() == null
                            ? null : directPickSource.cutRoundReport().getId())
                    .finishedStockId(stock.getId())
                    .stockNo(stock.getStockNo())
                    .outerBoxNo(stock.getOuterBoxNo())
                    .innerUnitNo(stock.getInnerUnitNo())
                    .packageNo(firstNotBlank(stock.getInnerUnitNo(), stock.getOuterBoxNo()))
                    .sliceBatchNo(stock.getSliceBatchNo())
                    .actualSliceBatchNo(stock.getSliceBatchNo())
                    .batchNo(firstNotBlank(noticeItem.getCustomerProductBatchNo(), stock.getBatchNo()))
                    .internalModelCode(firstNotBlank(noticeItem.getInternalModelCode(), noticeItem.getCustomerModelCode(), stock.getModelCode()))
                    .internalItemCode(firstNotBlank(noticeItem.getInternalItemCode(), noticeItem.getCustomerSliceBatchNo()))
                    .customerProductBatchNo(PRODUCT_TYPE_SAMPLE.equals(notice.getProductType())
                            ? null : firstNotBlank(noticeItem.getCustomerProductBatchNo(), stock.getBatchNo()))
                    .materialCode(firstNotBlank(noticeItem.getMaterialCode(), stock.getMaterialCode()))
                    .materialName(firstNotBlank(noticeItem.getMaterialName(), stock.getMaterialName()))
                    .modelCode(firstNotBlank(noticeItem.getInternalModelCode(), stock.getModelCode()))
                    .productSize(stock.getProductSize())
                    .stockQty(Math.max(1, intValue(stock.getQty())))
                    .availableQty(1)
                    .lockedQty(1)
                    .qualityStatus(stock.getQualityStatus())
                    .warehouseCode(stock.getWarehouseCode())
                    .warehouseName(stock.getWarehouseName())
                    .locationCode(stock.getLocationCode())
                    .locationName(stock.getLocationName())
                    .inboundNo(stock.getInboundNo())
                    .inboundTime(stock.getInboundTime())
                    .actualShipQty(1)
                    .lockStatus(NOTICE_STATUS_PICKED)
                    .lockName(operatorName)
                    .lockTime(now)
                    .remark(StrUtil.trimToNull(pickItem.getRemark()))
                    .build());
            noticeItemPickedCountMap.merge(noticeItem.getId(), 1, Integer::sum);
        }
        downShelfPickedPackages(notice, selectedStocks, selectedStockIds, operatorName, now);
        finalizeDirectPickedPackages(notice, directPickSourceMap, operatorName, now);
        boolean confirmPicked = Boolean.TRUE.equals(reqVO.getConfirmPicked());
        refreshShippingNoticePickedStatus(notice.getId(), confirmPicked);
        return getShippingNotice(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO confirmShippingNoticeForFqc(ShippingNoticeOutboundConfirmReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!List.of(NOTICE_STATUS_PICKED, NOTICE_STATUS_SHIP_CONFIRMED).contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有已下架待出货确认的发货需求单允许推送发货成品检验");
        }
        List<HcFgShippingNoticePickItemDO> pickItems = hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(notice.getId());
        if (pickItems.isEmpty()) {
            throw invalidParamException("发货需求单没有已下架配货明细，不能确认出货");
        }
        int pickedQty = (int) pickItems.stream().filter(this::isEffectiveShippingPickedItem).count();
        int requiredQty = intValue(firstPositive(notice.getRequiredShipQty(), notice.getNoticeQty()));
        validateShippingPickQuantity(pickedQty, 0, requiredQty, true);
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        for (HcFgShippingNoticePickItemDO item : pickItems) {
            if (!NOTICE_STATUS_PICKED.equals(item.getLockStatus())) {
                continue;
            }
            HcFgShippingNoticePickItemDO updateItem = new HcFgShippingNoticePickItemDO();
            updateItem.setId(item.getId());
            updateItem.setLockStatus(NOTICE_STATUS_SHIP_CONFIRMED);
            updateItem.setRemark(appendBusinessRemark(item.getRemark(), StrUtil.trimToNull(reqVO.getRemark())));
            hcFgShippingNoticePickItemMapper.updateById(updateItem);
        }
        HcFgShippingNoticeDO updateNotice = new HcFgShippingNoticeDO();
        updateNotice.setId(notice.getId());
        updateNotice.setNoticeStatus(NOTICE_STATUS_SHIP_CONFIRMED);
        updateNotice.setOutboundConfirmName(operatorName);
        updateNotice.setOutboundConfirmTime(now);
        updateNotice.setOutboundConfirmRemark(StrUtil.trimToNull(reqVO.getRemark()));
        hcFgShippingNoticeMapper.updateById(updateNotice);
        qmsFgShippingFqcService.createFromShippingNotice(notice.getId());
        return getShippingNotice(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO returnShippingNoticePick(ShippingNoticeReturnPickReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!NOTICE_RETURN_PICK_STATUSES.contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有未出库、未发货、未关闭的发货需求单允许退回");
        }
        String returnReason = StrUtil.trimToNull(reqVO.getReason());
        if (StrUtil.isBlank(returnReason)) {
            throw invalidParamException("退回原因不能为空");
        }
        List<Long> pickItemIds = reqVO.getPickItemIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (pickItemIds.isEmpty()) {
            throw invalidParamException("请选择需要退回的配货明细");
        }
        List<HcFgShippingNoticePickItemDO> pickItems = hcFgShippingNoticePickItemMapper
                .selectActiveListByNoticeIdAndIds(notice.getId(), pickItemIds);
        if (pickItems.size() != pickItemIds.size()) {
            throw invalidParamException("部分配货明细不存在或已不可退回，请刷新后重试");
        }
        if (pickItems.stream().anyMatch(item -> NOTICE_STATUS_OUTBOUND.equals(item.getLockStatus())
                || NOTICE_STATUS_SHIPPED.equals(item.getLockStatus())
                || NOTICE_STATUS_CLOSED.equals(item.getLockStatus())
                || item.getShippedTime() != null)) {
            throw invalidParamException("已进入出库、已发货或已关闭的配货明细不能退回");
        }

        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        Set<String> innerUnitNos = new HashSet<>();
        Set<String> touchedLocationCodes = new HashSet<>();
        for (HcFgShippingNoticePickItemDO item : pickItems) {
            if (item.getFinishedStockId() == null) {
                continue;
            }
            HcFgShippingNoticeItemDO noticeItem = getReturnNoticeItemOrThrow(notice, item);
            validateReturnNoticeItem(item, noticeItem);
            if (!isShippingInspectionNgForReturn(item)) {
                throw invalidParamException("只有发货成品检验NG的配货明细允许退回：" + firstNotBlank(item.getActualSliceBatchNo(), item.getStockNo()));
            }

            HcFinishedStockDO stock = hcFinishedStockMapper.selectByIdForUpdate(item.getFinishedStockId());
            if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
                throw invalidParamException("配货库存不存在：" + firstNotBlank(item.getActualSliceBatchNo(), item.getStockNo()));
            }
            if (!STATUS_OUTBOUND_LOCKED.equals(stock.getStockStatus())) {
                throw invalidParamException("只有已下架并发货锁定的库存允许退回：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            if (StrUtil.isNotBlank(stock.getLocationCode())) {
                touchedLocationCodes.add(stock.getLocationCode());
            }
            if (StrUtil.isNotBlank(stock.getInnerUnitNo())) {
                innerUnitNos.add(stock.getInnerUnitNo());
            }
            HcInnerPackUnitItemDO packItem = findReturnInnerPackItem(item, stock);
            boolean manualSource = isManualPackagingSource(packItem);
            if (packItem != null) {
                restoreReturnedPieceToPackagingWait(packItem, stock, INSPECTION_RESULT_NG,
                        "发货成品检验NG退回待包装：" + returnReason, operatorName);
                hcInnerPackUnitItemMapper.deletePhysicallyByIds(List.of(packItem.getId()));
            }
            if (!manualSource) {
                appendReturnInspectionRemark(item.getSourceCutRoundReportId(), packItem, stock, notice,
                        operatorName, returnReason, now, INSPECTION_RESULT_NG);
            }
            recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, "RETURNED_NG"),
                    "FG_SHIPPING_RETURN", now, operatorName, "FG_SHIPPING_NOTICE", notice.getId(),
                    notice.getNoticeNo(), returnReason);
            hcFinishedStockMapper.deletePhysicallyByIds(List.of(stock.getId()));

            HcFgShippingNoticePickItemDO itemUpdate = new HcFgShippingNoticePickItemDO();
            itemUpdate.setId(item.getId());
            itemUpdate.setLockStatus(NOTICE_STATUS_CANCELLED);
            itemUpdate.setCancelName(operatorName);
            itemUpdate.setCancelTime(now);
            itemUpdate.setRemark(appendBusinessRemark(item.getRemark(), "退回原因：" + returnReason));
            hcFgShippingNoticePickItemMapper.updateById(itemUpdate);

            if (item.getSourceNoticeItemId() != null) {
                hcFgShippingNoticeItemMapper.clearReturnedPickActual(
                        item.getSourceNoticeItemId(), notice.getId(), NOTICE_STATUS_SUBMITTED, operatorName, now);
            }
        }
        innerUnitNos.forEach(this::refreshReturnedInnerPackage);
        refreshShippingNoticeAfterPickReturn(notice.getId());
        touchedLocationCodes.forEach(this::refreshLocationOccupied);
        return getShippingNotice(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoReturnShippingFqcNgPicks(Long noticeId, List<Long> pickItemIds, String fqcNo) {
        List<Long> validPickItemIds = pickItemIds == null ? List.of() : pickItemIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (noticeId == null || validPickItemIds.isEmpty()) {
            return;
        }
        ShippingNoticeReturnPickReqVO reqVO = new ShippingNoticeReturnPickReqVO();
        reqVO.setNoticeId(noticeId);
        reqVO.setPickItemIds(validPickItemIds);
        reqVO.setReason("发货成品检验单" + StrUtil.blankToDefault(fqcNo, "-")
                + "审核判定NG，系统自动退回不合格待包装区");
        returnShippingNoticePick(reqVO);
    }

    private boolean isShippingInspectionNgForReturn(HcFgShippingNoticePickItemDO item) {
        if (item == null) {
            return false;
        }
        return isShippingFqcNg(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO inspectShippingNoticeItem(ShippingNoticeInspectionReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!List.of(NOTICE_STATUS_SHIP_CONFIRMED, NOTICE_STATUS_INSPECTED).contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有已出货确认的需求单允许发货检验");
        }
        String actualSliceBatchNo = StrUtil.trimToEmpty(reqVO.getActualSliceBatchNo());
        HcFgShippingNoticePickItemDO item = hcFgShippingNoticePickItemMapper.selectByNoticeIdAndActualSliceBatchNo(notice.getId(), actualSliceBatchNo);
        if (item == null) {
            throw invalidParamException("扫码片号不属于当前发货需求单配货明细：" + actualSliceBatchNo);
        }
        HcFgShippingNoticeItemDO noticeItem = hcFgShippingNoticeItemMapper.selectById(reqVO.getNoticeItemId());
        if (noticeItem == null || Boolean.TRUE.equals(noticeItem.getDeleted())
                || !Objects.equals(noticeItem.getNoticeId(), notice.getId())) {
            throw invalidParamException("客户要求明细不属于当前发货需求单");
        }
        HcFgShippingNoticeItemDO duplicateActualItem = hcFgShippingNoticeItemMapper.selectByNoticeIdAndActualSliceBatchNo(notice.getId(), actualSliceBatchNo);
        if (duplicateActualItem != null && !Objects.equals(duplicateActualItem.getId(), noticeItem.getId())) {
            throw invalidParamException("该实际片号已关联其它客户要求明细：" + actualSliceBatchNo);
        }
        if (StrUtil.isNotBlank(noticeItem.getActualSliceBatchNo())
                && !actualSliceBatchNo.equalsIgnoreCase(noticeItem.getActualSliceBatchNo())) {
            throw invalidParamException("该客户要求明细已关联实际片号：" + noticeItem.getActualSliceBatchNo());
        }
        validatePickItemMatches(noticeItem, item);
        String result = normalizeInspectionResult(reqVO.getInspectionResult());
        LocalDateTime now = LocalDateTime.now();
        HcFgShippingNoticePickItemDO updateItem = new HcFgShippingNoticePickItemDO();
        updateItem.setId(item.getId());
        updateItem.setSourceNoticeItemId(noticeItem.getId());
        updateItem.setInternalModelCode(firstNotBlank(noticeItem.getInternalModelCode(), noticeItem.getCustomerModelCode(), item.getInternalModelCode()));
        updateItem.setInternalItemCode(firstNotBlank(noticeItem.getInternalItemCode(), noticeItem.getCustomerSliceBatchNo(), item.getInternalItemCode()));
        updateItem.setCustomerProductBatchNo(firstNotBlank(noticeItem.getCustomerProductBatchNo(), item.getCustomerProductBatchNo()));
        updateItem.setBatchNo(firstNotBlank(noticeItem.getCustomerProductBatchNo(), item.getBatchNo()));
        updateItem.setShippingInspectionResult(result);
        updateItem.setShippingInspectorName(firstNotBlank(reqVO.getInspectorName(), currentUserName()));
        updateItem.setShippingInspectionTime(now);
        updateItem.setShippingInspectionRemark(StrUtil.trimToNull(reqVO.getRemark()));
        updateItem.setQualityStatus(result);
        updateItem.setLockStatus(NOTICE_STATUS_INSPECTED);
        hcFgShippingNoticePickItemMapper.updateById(updateItem);

        HcFgShippingNoticeItemDO updateNoticeItem = new HcFgShippingNoticeItemDO();
        updateNoticeItem.setId(noticeItem.getId());
        updateNoticeItem.setActualFinishedStockId(item.getFinishedStockId());
        updateNoticeItem.setActualStockNo(item.getStockNo());
        updateNoticeItem.setOuterBoxNo(item.getOuterBoxNo());
        updateNoticeItem.setInnerUnitNo(item.getInnerUnitNo());
        updateNoticeItem.setPackageNo(item.getPackageNo());
        updateNoticeItem.setActualSliceBatchNo(actualSliceBatchNo);
        updateNoticeItem.setSliceBatchNo(firstNotBlank(noticeItem.getSliceBatchNo(), actualSliceBatchNo));
        updateNoticeItem.setBatchNo(firstNotBlank(noticeItem.getCustomerProductBatchNo(), item.getBatchNo()));
        updateNoticeItem.setMaterialCode(firstNotBlank(noticeItem.getMaterialCode(), item.getMaterialCode()));
        updateNoticeItem.setMaterialName(firstNotBlank(noticeItem.getMaterialName(), item.getMaterialName()));
        updateNoticeItem.setModelCode(firstNotBlank(noticeItem.getInternalModelCode(), item.getModelCode()));
        updateNoticeItem.setStockQty(firstPositive(noticeItem.getStockQty(), item.getStockQty(), 1));
        updateNoticeItem.setActualShipQty(1);
        updateNoticeItem.setQualityStatus(result);
        updateNoticeItem.setWarehouseCode(item.getWarehouseCode());
        updateNoticeItem.setWarehouseName(item.getWarehouseName());
        updateNoticeItem.setActualLocationCode(item.getLocationCode());
        updateNoticeItem.setActualLocationName(item.getLocationName());
        updateNoticeItem.setInboundNo(item.getInboundNo());
        updateNoticeItem.setInboundTime(item.getInboundTime());
        updateNoticeItem.setLockStatus(NOTICE_STATUS_INSPECTED);
        updateNoticeItem.setLockName(firstNotBlank(reqVO.getInspectorName(), currentUserName()));
        updateNoticeItem.setLockTime(now);
        updateNoticeItem.setShippingInspectorName(firstNotBlank(reqVO.getInspectorName(), currentUserName()));
        updateNoticeItem.setShippingInspectionTime(now);
        updateNoticeItem.setShippingInspectionResult(result);
        updateNoticeItem.setShippingInspectionRemark(StrUtil.trimToNull(reqVO.getRemark()));
        hcFgShippingNoticeItemMapper.updateById(updateNoticeItem);

        List<HcFgShippingNoticePickItemDO> items = hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(notice.getId());
        boolean allInspected = !items.isEmpty() && items.stream()
                .allMatch(row -> StrUtil.isNotBlank(firstNotBlank(row.getShippingInspectionResult(),
                        Objects.equals(row.getId(), item.getId()) ? result : null)));
        if (allInspected) {
            HcFgShippingNoticeDO updateNotice = new HcFgShippingNoticeDO();
            updateNotice.setId(notice.getId());
            updateNotice.setNoticeStatus(NOTICE_STATUS_INSPECTED);
            hcFgShippingNoticeMapper.updateById(updateNotice);
        }
        return getShippingNotice(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO packShippingNotice(ShippingNoticePackReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!List.of(NOTICE_STATUS_OQC_PASSED, NOTICE_STATUS_PACKAGED).contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有 OQC 检验合格的需求单允许外包装");
        }
        assertShippingPackagingDailyRecordsReady(LocalDate.now(BUSINESS_ZONE));
        Set<Long> selectedIds = new HashSet<>();
        if (reqVO.getNoticeItemIds() != null) {
            for (Long noticeItemId : reqVO.getNoticeItemIds()) {
                if (noticeItemId != null) {
                    selectedIds.add(noticeItemId);
                }
            }
        }
        if (selectedIds.isEmpty()) {
            throw invalidParamException("请选择客户批号明细");
        }
        List<HcFgShippingNoticeItemDO> noticeItems = selectShippingExecutionItems(notice);
        List<HcFgShippingNoticeItemDO> selectedItems = noticeItems.stream()
                .filter(item -> selectedIds.contains(item.getId()))
                .toList();
        if (selectedItems.size() != selectedIds.size()) {
            throw invalidParamException("选择的客户批号明细不存在或已失效");
        }
        for (HcFgShippingNoticeItemDO item : selectedItems) {
            if (StrUtil.isBlank(item.getActualSliceBatchNo())) {
                throw invalidParamException("客户批号表存在未关联实际片号的明细，不能打包：" + firstNotBlank(item.getInternalItemCode(), item.getPackageSliceNo()));
            }
            if (!isShippingFqcOk(item)) {
                throw invalidParamException("客户批号表存在未合格的发货成品检验明细，不能打包：" + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
            }
            if (item.getShippingPackageTime() != null) {
                throw invalidParamException("客户批号表明细已完成外包装，不能重复打包："
                        + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
            }
        }
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        String packageNo = nextNo("FGPK");
        String packageMethod = StrUtil.trimToEmpty(reqVO.getPackageMethod());
        List<HcToolingConsumableConsumeDO> auxConsumeRecords = consumePackageAuxItems(
                reqVO.getAuxConsumeItems(), AUX_BIZ_FG_SHIPPING_OUTER_PACKAGING, packageNo, operatorName,
                "发货外包装自动领用");
        String auxMaterialName = joinPackageAuxMaterialNames(auxConsumeRecords);
        String packageRemark = "包装方式：" + packageMethod + "；耗材名称：" + auxMaterialName;
        String extraRemark = StrUtil.trimToNull(reqVO.getRemark());
        if (StrUtil.isNotBlank(extraRemark)) {
            packageRemark = packageRemark + "；备注：" + extraRemark;
        }
        Set<String> actualSliceBatchNos = new HashSet<>();
        for (HcFgShippingNoticeItemDO item : selectedItems) {
            actualSliceBatchNos.add(item.getActualSliceBatchNo());
            HcFgShippingNoticeItemDO updateItem = new HcFgShippingNoticeItemDO();
            updateItem.setId(item.getId());
            updateItem.setOuterBoxNo(packageNo);
            updateItem.setPackageNo(packageNo);
            updateItem.setShippingPackageName(operatorName);
            updateItem.setShippingPackageTime(now);
            updateItem.setShippingPackageRemark(packageRemark);
            updateItem.setLockStatus(NOTICE_STATUS_PACKAGED);
            hcFgShippingNoticeItemMapper.updateById(updateItem);
        }
        List<HcFgShippingNoticePickItemDO> pickItems = hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(notice.getId());
        for (HcFgShippingNoticePickItemDO item : pickItems) {
            if (isShippingFqcNg(item)) {
                continue;
            }
            boolean matchedByNoticeItem = item.getSourceNoticeItemId() != null && selectedIds.contains(item.getSourceNoticeItemId());
            boolean matchedBySlice = StrUtil.isNotBlank(item.getActualSliceBatchNo()) && actualSliceBatchNos.contains(item.getActualSliceBatchNo());
            if (!matchedByNoticeItem && !matchedBySlice) {
                continue;
            }
            HcFgShippingNoticePickItemDO updateItem = new HcFgShippingNoticePickItemDO();
            updateItem.setId(item.getId());
            updateItem.setOuterBoxNo(packageNo);
            updateItem.setPackageNo(packageNo);
            updateItem.setShippingPackageName(operatorName);
            updateItem.setShippingPackageTime(now);
            updateItem.setShippingPackageRemark(packageRemark);
            updateItem.setLockStatus(NOTICE_STATUS_PACKAGED);
            hcFgShippingNoticePickItemMapper.updateById(updateItem);
        }
        HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
        noticeUpdate.setId(notice.getId());
        noticeUpdate.setNoticeStatus(NOTICE_STATUS_PACKAGED);
        hcFgShippingNoticeMapper.updateById(noticeUpdate);
        return getShippingNotice(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO pushShippingNoticeOqc(ShippingNoticePackageConfirmReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (NOTICE_STATUS_OQC_INSPECTING.equals(notice.getNoticeStatus())) {
            return getShippingNotice(notice.getId());
        }
        if (!NOTICE_STATUS_INSPECTED.equals(notice.getNoticeStatus())) {
            throw invalidParamException("只有内包装、发货成品检验均完成且合格的需求单允许推送 OQC");
        }
        assertShippingInnerPackagingDailyRecordsReady(LocalDate.now(BUSINESS_ZONE));
        List<HcFgShippingNoticePickItemDO> items = hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(notice.getId());
        if (items.isEmpty()) {
            throw invalidParamException("发货需求单没有配货领用明细");
        }
        List<HcFgShippingNoticeItemDO> noticeItems = selectShippingExecutionItems(notice);
        if (noticeItems.isEmpty()) {
            throw invalidParamException("发货需求单没有客户批号明细");
        }
        for (HcFgShippingNoticeItemDO item : noticeItems) {
            if (StrUtil.isBlank(item.getActualSliceBatchNo())) {
                throw invalidParamException("客户批号表存在未关联实际片号的明细，不能发送出货检验：" + firstNotBlank(item.getInternalItemCode(), item.getPackageSliceNo()));
            }
            if (!isShippingFqcOk(item)) {
                throw invalidParamException("客户批号表存在未合格的发货成品检验明细，不能发送出货检验：" + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
            }
            if (hasShippingOuterPackageTrace(item)) {
                throw invalidParamException("OQC 必须在外包装前推送，客户批号表存在已外包装明细："
                        + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
            }
        }
        for (HcFgShippingNoticePickItemDO item : items) {
            if (isShippingFqcNg(item)) {
                continue;
            }
            if (StrUtil.isBlank(item.getActualSliceBatchNo())) {
                throw invalidParamException("所有明细必须完成实际片号配货后才能包装");
            }
            if (!isShippingFqcOk(item)) {
                throw invalidParamException("所有明细必须发货成品检验合格后才能包装：" + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
            }
        }
        Long oqcId = qmsOqcService.createOqcFromShippingNoticeId(notice.getId(), null);
        QmsOqcRespVO oqc = qmsOqcService.getOqcResp(oqcId);
        for (HcFgShippingNoticePickItemDO item : items) {
            if (isShippingFqcNg(item)) {
                continue;
            }
            HcFgShippingNoticePickItemDO updateItem = new HcFgShippingNoticePickItemDO();
            updateItem.setId(item.getId());
            updateItem.setShippingQualityNo(oqc.getOqcNo());
            updateItem.setLockStatus(NOTICE_STATUS_OQC_INSPECTING);
            hcFgShippingNoticePickItemMapper.updateById(updateItem);
        }
        for (HcFgShippingNoticeItemDO item : noticeItems) {
            HcFgShippingNoticeItemDO updateItem = new HcFgShippingNoticeItemDO();
            updateItem.setId(item.getId());
            updateItem.setLockStatus(NOTICE_STATUS_OQC_INSPECTING);
            updateItem.setOqcOrderId(oqcId);
            updateItem.setShippingQualityNo(oqc.getOqcNo());
            updateItem.setOqcStatus(oqc.getStatus());
            hcFgShippingNoticeItemMapper.updateById(updateItem);
        }
        HcFgShippingNoticeDO updateNotice = new HcFgShippingNoticeDO();
        updateNotice.setId(notice.getId());
        updateNotice.setNoticeStatus(NOTICE_STATUS_OQC_INSPECTING);
        hcFgShippingNoticeMapper.updateById(updateNotice);
        return getShippingNotice(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO completeShippingNotice(ShippingNoticePackageConfirmReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getNoticeId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!NOTICE_STATUS_PACKAGED.equals(notice.getNoticeStatus())) {
            throw invalidParamException("只有完成 OQC 且全部完成外包装的需求单允许发货完成");
        }
        List<HcFgShippingNoticeItemDO> noticeItems = selectShippingExecutionItems(notice);
        if (noticeItems.isEmpty()) {
            throw invalidParamException("发货需求单没有客户批号明细");
        }
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        Set<String> locationCodes = new HashSet<>();
        for (HcFgShippingNoticeItemDO item : noticeItems) {
            assertShippingNoticeItemReadyForCompletion(item);
            Long finishedStockId = item.getActualFinishedStockId() != null
                    ? item.getActualFinishedStockId() : item.getFinishedStockId();
            HcFinishedStockDO stock = finishedStockId == null ? null : hcFinishedStockMapper.selectByIdForUpdate(finishedStockId);
            if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
                throw invalidParamException("实际发货库存不存在：" + firstNotBlank(item.getActualSliceBatchNo(), item.getSliceBatchNo()));
            }
            if (!STATUS_OUTBOUND_LOCKED.equals(stock.getStockStatus())) {
                throw invalidParamException("实际发货库存未处于出库锁定状态：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }

            HcFgShippingNoticeItemDO itemUpdate = new HcFgShippingNoticeItemDO();
            itemUpdate.setId(item.getId());
            itemUpdate.setLockStatus(NOTICE_STATUS_SHIPPED);
            itemUpdate.setShippedName(operatorName);
            itemUpdate.setShippedTime(now);
            hcFgShippingNoticeItemMapper.updateById(itemUpdate);

            HcFinishedStockDO stockUpdate = new HcFinishedStockDO();
            stockUpdate.setId(stock.getId());
            stockUpdate.setStockStatus(STATUS_SHIPPED);
            hcFinishedStockMapper.updateById(stockUpdate);
            recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, STATUS_SHIPPED),
                    "FG_SHIP", now, operatorName, "FG_SHIPPING_NOTICE", notice.getId(),
                    notice.getNoticeNo(), reqVO.getRemark());

            HcFgShippingNoticePickItemDO pickItem = hcFgShippingNoticePickItemMapper
                    .selectActiveByNoticeIdAndFinishedStockId(notice.getId(), stock.getId());
            if (pickItem != null) {
                HcFgShippingNoticePickItemDO pickUpdate = new HcFgShippingNoticePickItemDO();
                pickUpdate.setId(pickItem.getId());
                pickUpdate.setLockStatus(NOTICE_STATUS_SHIPPED);
                pickUpdate.setShippedName(operatorName);
                pickUpdate.setShippedTime(now);
                hcFgShippingNoticePickItemMapper.updateById(pickUpdate);
            }
            if (StrUtil.isNotBlank(stock.getLocationCode())) {
                locationCodes.add(stock.getLocationCode());
            }
        }
        HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
        noticeUpdate.setId(notice.getId());
        noticeUpdate.setNoticeStatus(NOTICE_STATUS_CLOSED);
        hcFgShippingNoticeMapper.updateById(noticeUpdate);
        locationCodes.forEach(this::refreshLocationOccupied);
        return getShippingNotice(notice.getId());
    }

    private void assertShippingNoticeItemReadyForCompletion(HcFgShippingNoticeItemDO item) {
        String packageNo = firstNotBlank(item.getPackageNo(), item.getOuterBoxNo());
        if (StrUtil.isBlank(packageNo) || item.getShippingPackageTime() == null || StrUtil.isBlank(item.getShippingPackageName())) {
            throw invalidParamException("客户批号表存在未完成外包装明细：" + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
        }
        if (item.getOqcOrderId() == null) {
            throw invalidParamException("客户批号表未关联 OQC 检验单：" + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
        }
        QmsOqcOrderDO oqcOrder = qmsOqcOrderMapper.selectById(item.getOqcOrderId());
        if (oqcOrder == null || !"COMPLETED".equals(oqcOrder.getStatus())
                || !INSPECTION_RESULT_OK.equals(oqcOrder.getJudgment())
                || !"ALLOW_SHIPMENT".equals(oqcOrder.getReleaseResult())) {
            throw invalidParamException("关联 OQC 未完成或未判定合格：" + firstNotBlank(item.getActualSliceBatchNo(), item.getInternalItemCode()));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO saveAndLockShippingNotice(ShippingNoticeSaveLockReqVO reqVO) {
        HcFgShippingNoticeDO oldNotice = null;
        if (reqVO.getId() != null) {
            oldNotice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getId());
            if (oldNotice == null || Boolean.TRUE.equals(oldNotice.getDeleted())) {
                throw invalidParamException("发货需求单不存在");
            }
            if (!NOTICE_STATUS_DRAFT.equals(oldNotice.getNoticeStatus())) {
                throw invalidParamException("只有草稿状态的发货需求单允许修改");
            }
        }
        String productType = normalizeProductType(reqVO.getProductType());
        List<ShippingNoticeSaveItemReqVO> reqItems = (reqVO.getItems() == null ? Collections.<ShippingNoticeSaveItemReqVO>emptyList() : reqVO.getItems())
                .stream()
                .filter(Objects::nonNull)
                .filter(this::hasShippingNoticeSaveItemValue)
                .toList();
        String noticeNo = firstNotBlank(StrUtil.trimToNull(reqVO.getNoticeNo()),
                oldNotice == null ? null : oldNotice.getNoticeNo(),
                nextShippingNoticeNo());
        HcFgShippingNoticeDO sameNoNotice = hcFgShippingNoticeMapper.selectByNoticeNo(noticeNo);
        if (sameNoNotice != null && (oldNotice == null || !Objects.equals(sameNoNotice.getId(), oldNotice.getId()))) {
            throw invalidParamException("发货需求单号已存在");
        }
        LocalDateTime now = LocalDateTime.now();
        String materialCode = StrUtil.trimToNull(reqVO.getMaterialCode());
        String materialName = StrUtil.trimToNull(reqVO.getMaterialName());
        String modelCode = firstNotBlank(StrUtil.trimToNull(reqVO.getModelCode()), StrUtil.trimToNull(reqVO.getExternalProductModel()));
        String productSize = StrUtil.trimToNull(reqVO.getProductSize());
        String requiredBatchNo = StrUtil.trimToNull(reqVO.getRequiredBatchNo());
        int noticeQty = PRODUCT_TYPE_SAMPLE.equals(productType)
                ? positive(reqVO.getRequiredShipQty() == null ? reqVO.getNoticeQty() : reqVO.getRequiredShipQty(), "样品数量必须大于0")
                : positive(firstPositive(reqVO.getNoticeQty(), reqVO.getRequiredShipQty(), reqItems.size()), "发货数量必须大于0");
        HcFgShippingNoticeDO notice = HcFgShippingNoticeDO.builder()
                .tenantId(currentTenantId())
                .noticeNo(noticeNo)
                .customerId(reqVO.getCustomerId())
                .customerCode(StrUtil.trimToNull(reqVO.getCustomerCode()))
                .customerName(StrUtil.trimToEmpty(reqVO.getCustomerName()))
                .productType(productType)
                .materialCode(materialCode)
                .materialName(materialName)
                .modelCode(modelCode)
                .productSize(productSize)
                .orderNo(StrUtil.trimToNull(reqVO.getOrderNo()))
                .erpOrderNo(StrUtil.trimToNull(reqVO.getErpOrderNo()))
                .shippingTime(reqVO.getShippingTime())
                .externalProductModel(StrUtil.trimToNull(reqVO.getExternalProductModel()))
                .externalProductCode(StrUtil.trimToNull(reqVO.getExternalProductCode()))
                .externalProductInfo(StrUtil.trimToNull(reqVO.getExternalProductInfo()))
                .requiredShipQty(PRODUCT_TYPE_SAMPLE.equals(productType) ? noticeQty : reqVO.getRequiredShipQty())
                .requiredSliceRange(StrUtil.trimToNull(reqVO.getRequiredSliceRange()))
                .requiredBatchNo(requiredBatchNo)
                .requiredProductionDate(reqVO.getRequiredProductionDate())
                .requiredExpiryDate(reqVO.getRequiredExpiryDate())
                .packingRequirement(StrUtil.trimToNull(reqVO.getPackingRequirement()))
                .shippingConfirmName(StrUtil.trimToNull(reqVO.getShippingConfirmName()))
                .noticeQty(noticeQty)
                .lockedQty(0)
                .noticeStatus(NOTICE_STATUS_DRAFT)
                .recorderName(firstNotBlank(reqVO.getRecorderName(), currentUserName()))
                .recorderTime(oldNotice == null || oldNotice.getRecorderTime() == null ? now : oldNotice.getRecorderTime())
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .build();
        if (oldNotice == null) {
            hcFgShippingNoticeMapper.insert(notice);
        } else {
            notice.setId(oldNotice.getId());
            notice.setTenantId(oldNotice.getTenantId());
            notice.setCancelName(null);
            notice.setCancelTime(null);
            notice.setCancelReason(null);
            hcFgShippingNoticeMapper.updateById(notice);
            hcFgShippingNoticeItemMapper.physicalDeleteByNoticeId(oldNotice.getId());
        }
        for (ShippingNoticeSaveItemReqVO itemReq : reqItems) {
            hcFgShippingNoticeItemMapper.insert(buildShippingNoticeDraftItem(notice, itemReq, materialCode, materialName, modelCode, productSize,
                    requiredBatchNo));
        }
        return buildShippingNoticeResp(notice, true);
    }

    /**
     * 变更未关闭的发货需求单。
     *
     * <p>仅 CLOSED 是业务终态。若本次变更会影响配货、检验、包装或出库依据，先将未关闭的下游执行
     * 数据作废并释放库存占用，再从“已下达待配货”重新执行；否则仅同步各下游单据的业务快照。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO changeShippingNotice(ShippingNoticeChangeReqVO reqVO) {
        HcFgShippingNoticeDO oldNotice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getId());
        if (oldNotice == null || Boolean.TRUE.equals(oldNotice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (NOTICE_STATUS_CLOSED.equals(oldNotice.getNoticeStatus())) {
            throw invalidParamException("发货需求单已完成出库关闭，不能修改");
        }
        int currentChangeVersion = intValue(oldNotice.getChangeVersion());
        if (!Objects.equals(currentChangeVersion, reqVO.getExpectedChangeVersion())) {
            throw invalidParamException("发货需求单已被其他用户修改，请刷新后重试");
        }

        String changeReason = StrUtil.trimToNull(reqVO.getChangeReason());
        if (StrUtil.isBlank(changeReason)) {
            throw invalidParamException("变更原因不能为空");
        }
        List<HcFgShippingNoticeItemDO> beforeItems = hcFgShippingNoticeItemMapper.selectListByNoticeId(oldNotice.getId());
        List<ShippingNoticeSaveItemReqVO> reqItems = (reqVO.getItems() == null
                ? Collections.<ShippingNoticeSaveItemReqVO>emptyList() : reqVO.getItems()).stream()
                .filter(Objects::nonNull)
                .filter(this::hasShippingNoticeSaveItemValue)
                .toList();
        if (reqItems.isEmpty() && !PRODUCT_TYPE_SAMPLE.equals(normalizeProductType(reqVO.getProductType()))) {
            throw invalidParamException("发货需求单至少需要保留一条客户批号明细");
        }

        String productType = normalizeProductType(reqVO.getProductType());
        String noticeNo = firstNotBlank(StrUtil.trimToNull(reqVO.getNoticeNo()), oldNotice.getNoticeNo());
        HcFgShippingNoticeDO sameNoNotice = hcFgShippingNoticeMapper.selectByNoticeNo(noticeNo);
        if (sameNoNotice != null && !Objects.equals(sameNoNotice.getId(), oldNotice.getId())) {
            throw invalidParamException("发货需求单号已存在");
        }
        String materialCode = StrUtil.trimToNull(reqVO.getMaterialCode());
        String materialName = StrUtil.trimToNull(reqVO.getMaterialName());
        String modelCode = firstNotBlank(StrUtil.trimToNull(reqVO.getModelCode()), StrUtil.trimToNull(reqVO.getExternalProductModel()));
        String productSize = StrUtil.trimToNull(reqVO.getProductSize());
        String requiredBatchNo = StrUtil.trimToNull(reqVO.getRequiredBatchNo());
        int noticeQty = PRODUCT_TYPE_SAMPLE.equals(productType)
                ? positive(reqVO.getRequiredShipQty() == null ? reqVO.getNoticeQty() : reqVO.getRequiredShipQty(), "样品数量必须大于0")
                : positive(firstPositive(reqVO.getNoticeQty(), reqVO.getRequiredShipQty(), reqItems.size()), "发货数量必须大于0");

        Map<Long, HcFgShippingNoticeItemDO> beforeItemMap = new LinkedHashMap<>();
        for (HcFgShippingNoticeItemDO item : beforeItems) {
            beforeItemMap.put(item.getId(), item);
        }
        validateShippingNoticeChangeItems(reqItems, beforeItemMap);
        boolean executionAffected = isShippingNoticeExecutionAffected(oldNotice, beforeItems, reqItems,
                productType, materialCode, materialName, modelCode, productSize, requiredBatchNo, noticeQty, reqVO);
        String nextNoticeStatus = NOTICE_STATUS_CANCELLED.equals(oldNotice.getNoticeStatus())
                ? NOTICE_STATUS_DRAFT : (executionAffected ? NOTICE_STATUS_SUBMITTED : oldNotice.getNoticeStatus());
        String resetItemStatus = executionAffected ? NOTICE_STATUS_SUBMITTED
                : (NOTICE_STATUS_CANCELLED.equals(oldNotice.getNoticeStatus()) ? NOTICE_STATUS_DRAFT : null);
        if (executionAffected) {
            resetShippingNoticeExecutionForChange(oldNotice, beforeItems, changeReason);
        }

        HcFgShippingNoticeDO noticeUpdate = HcFgShippingNoticeDO.builder()
                .id(oldNotice.getId())
                .noticeNo(noticeNo)
                .customerId(reqVO.getCustomerId())
                .customerCode(StrUtil.trimToNull(reqVO.getCustomerCode()))
                .customerName(StrUtil.trimToEmpty(reqVO.getCustomerName()))
                .productType(productType)
                .materialCode(materialCode)
                .materialName(materialName)
                .modelCode(modelCode)
                .productSize(productSize)
                .orderNo(StrUtil.trimToNull(reqVO.getOrderNo()))
                .erpOrderNo(StrUtil.trimToNull(reqVO.getErpOrderNo()))
                .shippingTime(reqVO.getShippingTime())
                .externalProductModel(StrUtil.trimToNull(reqVO.getExternalProductModel()))
                .externalProductCode(StrUtil.trimToNull(reqVO.getExternalProductCode()))
                .externalProductInfo(StrUtil.trimToNull(reqVO.getExternalProductInfo()))
                .requiredShipQty(PRODUCT_TYPE_SAMPLE.equals(productType) ? noticeQty : reqVO.getRequiredShipQty())
                .requiredSliceRange(StrUtil.trimToNull(reqVO.getRequiredSliceRange()))
                .requiredBatchNo(requiredBatchNo)
                .requiredProductionDate(reqVO.getRequiredProductionDate())
                .requiredExpiryDate(reqVO.getRequiredExpiryDate())
                .packingRequirement(StrUtil.trimToNull(reqVO.getPackingRequirement()))
                .shippingConfirmName(StrUtil.trimToNull(reqVO.getShippingConfirmName()))
                .noticeQty(noticeQty)
                .lockedQty(executionAffected ? 0 : oldNotice.getLockedQty())
                .noticeStatus(nextNoticeStatus)
                .recorderName(firstNotBlank(reqVO.getRecorderName(), oldNotice.getRecorderName(), currentUserName()))
                .recorderTime(oldNotice.getRecorderTime())
                .remark(StrUtil.trimToNull(reqVO.getRemark()))
                .changeVersion(currentChangeVersion + 1)
                .build();
        hcFgShippingNoticeMapper.updateById(noticeUpdate);

        Set<Long> submittedItemIds = new LinkedHashSet<>();
        for (ShippingNoticeSaveItemReqVO itemReq : reqItems) {
            HcFgShippingNoticeItemDO existingItem = itemReq.getId() == null ? null : beforeItemMap.get(itemReq.getId());
            if (existingItem == null) {
                HcFgShippingNoticeItemDO newItem = buildShippingNoticeDraftItem(noticeUpdate, itemReq,
                        materialCode, materialName, modelCode, productSize, requiredBatchNo);
                newItem.setLockStatus(nextNoticeStatus);
                hcFgShippingNoticeItemMapper.insert(newItem);
                submittedItemIds.add(newItem.getId());
            } else {
                updateShippingNoticeBusinessItem(existingItem, noticeUpdate, itemReq,
                        materialCode, materialName, modelCode, productSize, requiredBatchNo, resetItemStatus);
                submittedItemIds.add(existingItem.getId());
            }
        }
        for (HcFgShippingNoticeItemDO beforeItem : beforeItems) {
            if (submittedItemIds.contains(beforeItem.getId())) {
                continue;
            }
            HcFgShippingNoticeItemDO cancelUpdate = new HcFgShippingNoticeItemDO();
            cancelUpdate.setId(beforeItem.getId());
            cancelUpdate.setLockStatus(NOTICE_STATUS_CANCELLED);
            cancelUpdate.setCancelName(currentUserName());
            cancelUpdate.setCancelTime(LocalDateTime.now());
            cancelUpdate.setRemark(appendBusinessRemark(beforeItem.getRemark(), "需求变更后取消：" + changeReason));
            hcFgShippingNoticeItemMapper.updateById(cancelUpdate);
        }
        syncShippingNoticeAttachmentNo(oldNotice.getId(), noticeNo);

        HcFgShippingNoticeDO changedNotice = hcFgShippingNoticeMapper.selectById(oldNotice.getId());
        List<HcFgShippingNoticeItemDO> changedItems = hcFgShippingNoticeItemMapper.selectListByNoticeId(oldNotice.getId());
        if (executionAffected && PRODUCT_TYPE_SAMPLE.equals(changedNotice.getProductType())) {
            changedItems = prepareSampleExecutionItems(changedNotice, changedItems);
        }
        String downstreamSync = syncShippingNoticeDownstreamSnapshots(changedNotice, changedItems, executionAffected, changeReason);
        insertShippingNoticeChangeLog(oldNotice, beforeItems, changedNotice, changedItems, changeReason,
                executionAffected, downstreamSync);
        return getShippingNotice(oldNotice.getId());
    }

    private void validateShippingNoticeChangeItems(List<ShippingNoticeSaveItemReqVO> reqItems,
                                                    Map<Long, HcFgShippingNoticeItemDO> beforeItemMap) {
        Set<Long> seenItemIds = new HashSet<>();
        for (ShippingNoticeSaveItemReqVO item : reqItems) {
            if (item.getId() == null) {
                continue;
            }
            if (!beforeItemMap.containsKey(item.getId())) {
                throw invalidParamException("客户批号明细不属于当前发货需求单");
            }
            if (!seenItemIds.add(item.getId())) {
                throw invalidParamException("客户批号明细不能重复提交");
            }
        }
    }

    private boolean isShippingNoticeExecutionAffected(HcFgShippingNoticeDO oldNotice,
                                                       List<HcFgShippingNoticeItemDO> beforeItems,
                                                       List<ShippingNoticeSaveItemReqVO> reqItems,
                                                       String productType, String materialCode, String materialName, String modelCode,
                                                       String productSize, String requiredBatchNo, int noticeQty,
                                                       ShippingNoticeChangeReqVO reqVO) {
        if (NOTICE_STATUS_DRAFT.equals(oldNotice.getNoticeStatus()) || NOTICE_STATUS_CANCELLED.equals(oldNotice.getNoticeStatus())) {
            return false;
        }
        if (!Objects.equals(oldNotice.getProductType(), productType)
                || !Objects.equals(oldNotice.getMaterialCode(), materialCode)
                || !Objects.equals(oldNotice.getModelCode(), modelCode)
                || !Objects.equals(oldNotice.getProductSize(), productSize)
                || !Objects.equals(oldNotice.getRequiredBatchNo(), requiredBatchNo)
                || !Objects.equals(oldNotice.getRequiredShipQty(), reqVO.getRequiredShipQty())
                || !Objects.equals(oldNotice.getNoticeQty(), noticeQty)
                || !Objects.equals(oldNotice.getRequiredSliceRange(), StrUtil.trimToNull(reqVO.getRequiredSliceRange()))
                || !Objects.equals(oldNotice.getRequiredProductionDate(), reqVO.getRequiredProductionDate())
                || !Objects.equals(oldNotice.getRequiredExpiryDate(), reqVO.getRequiredExpiryDate())
                || !Objects.equals(oldNotice.getPackingRequirement(), StrUtil.trimToNull(reqVO.getPackingRequirement()))) {
            return true;
        }
        if (beforeItems.size() != reqItems.size()) {
            return true;
        }
        Map<Long, HcFgShippingNoticeItemDO> beforeItemMap = new LinkedHashMap<>();
        for (HcFgShippingNoticeItemDO item : beforeItems) {
            beforeItemMap.put(item.getId(), item);
        }
        for (ShippingNoticeSaveItemReqVO itemReq : reqItems) {
            HcFgShippingNoticeItemDO existing = itemReq.getId() == null ? null : beforeItemMap.get(itemReq.getId());
            if (existing == null || isShippingNoticeItemExecutionAffected(existing, itemReq, materialCode,
                    materialName, modelCode, productSize, requiredBatchNo)) {
                return true;
            }
        }
        return false;
    }

    private boolean isShippingNoticeItemExecutionAffected(HcFgShippingNoticeItemDO existing,
                                                           ShippingNoticeSaveItemReqVO itemReq,
                                                           String materialCode, String materialName, String modelCode,
                                                           String productSize, String requiredBatchNo) {
        HcFgShippingNoticeDO itemNotice = HcFgShippingNoticeDO.builder()
                .id(existing.getNoticeId())
                .noticeNo(existing.getNoticeNo())
                .tenantId(existing.getTenantId())
                .build();
        HcFgShippingNoticeItemDO candidate = buildShippingNoticeDraftItem(itemNotice, itemReq,
                materialCode, materialName, modelCode, productSize, requiredBatchNo);
        return !Objects.equals(existing.getLockedQty(), candidate.getLockedQty())
                || !Objects.equals(existing.getBatchNo(), candidate.getBatchNo())
                || !Objects.equals(existing.getInternalModelCode(), candidate.getInternalModelCode())
                || !Objects.equals(existing.getInternalItemCode(), candidate.getInternalItemCode())
                || !Objects.equals(existing.getPackageSliceNo(), candidate.getPackageSliceNo())
                || !Objects.equals(existing.getMaterialCode(), candidate.getMaterialCode())
                || !Objects.equals(existing.getModelCode(), candidate.getModelCode())
                || !Objects.equals(existing.getProductSize(), candidate.getProductSize());
    }

    private void updateShippingNoticeBusinessItem(HcFgShippingNoticeItemDO existingItem,
                                                   HcFgShippingNoticeDO notice,
                                                   ShippingNoticeSaveItemReqVO itemReq,
                                                   String materialCode, String materialName, String modelCode,
                                                   String productSize, String requiredBatchNo,
                                                   String resetItemStatus) {
        HcFgShippingNoticeItemDO source = buildShippingNoticeDraftItem(notice, itemReq,
                materialCode, materialName, modelCode, productSize, requiredBatchNo);
        HcFgShippingNoticeItemDO update = new HcFgShippingNoticeItemDO();
        update.setId(existingItem.getId());
        update.setNoticeNo(notice.getNoticeNo());
        update.setSliceBatchNo(source.getSliceBatchNo());
        update.setBatchNo(source.getBatchNo());
        update.setInternalModelCode(source.getInternalModelCode());
        update.setInternalItemCode(source.getInternalItemCode());
        update.setCustomerProductBatchNo(source.getCustomerProductBatchNo());
        update.setPackageSliceNo(source.getPackageSliceNo());
        update.setMaterialCode(source.getMaterialCode());
        update.setMaterialName(source.getMaterialName());
        update.setModelCode(source.getModelCode());
        update.setProductSize(source.getProductSize());
        update.setLockedQty(source.getLockedQty());
        update.setCustomerSliceBatchNo(source.getCustomerSliceBatchNo());
        update.setCustomerModelCode(source.getCustomerModelCode());
        update.setRemark(source.getRemark());
        if (resetItemStatus != null) {
            update.setLockStatus(resetItemStatus);
        }
        hcFgShippingNoticeItemMapper.updateById(update);
    }

    private void resetShippingNoticeExecutionForChange(HcFgShippingNoticeDO notice,
                                                        List<HcFgShippingNoticeItemDO> noticeItems,
                                                        String changeReason) {
        String resetRemark = "需求变更后作废，原因：" + changeReason;
        LocalDateTime now = LocalDateTime.now();
        Set<Long> stockIds = new LinkedHashSet<>();
        for (HcFgShippingNoticePickItemDO pickItem : hcFgShippingNoticePickItemMapper.selectListByNoticeId(notice.getId())) {
            if (!HcFgShippingNoticePickItemMapper.ACTIVE_STATUSES.contains(pickItem.getLockStatus())) {
                continue;
            }
            HcFgShippingNoticePickItemDO update = new HcFgShippingNoticePickItemDO();
            update.setId(pickItem.getId());
            update.setLockStatus(NOTICE_STATUS_CANCELLED);
            update.setCancelName(currentUserName());
            update.setCancelTime(now);
            update.setRemark(appendBusinessRemark(pickItem.getRemark(), resetRemark));
            hcFgShippingNoticePickItemMapper.updateById(update);
            if (pickItem.getFinishedStockId() != null) {
                stockIds.add(pickItem.getFinishedStockId());
            }
        }
        for (HcFgShippingNoticeItemDO noticeItem : noticeItems) {
            if (noticeItem.getFinishedStockId() != null) {
                stockIds.add(noticeItem.getFinishedStockId());
            }
            if (noticeItem.getActualFinishedStockId() != null) {
                stockIds.add(noticeItem.getActualFinishedStockId());
            }
            clearShippingNoticeItemExecution(noticeItem.getId());
        }
        for (QmsFqcOrderDO order : qmsFqcOrderMapper.selectListBySource(
                QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC, notice.getId())) {
            QmsFqcOrderDO update = new QmsFqcOrderDO();
            update.setId(order.getId());
            update.setStatus("CANCELED");
            update.setRemark(appendBusinessRemark(order.getRemark(), resetRemark));
            qmsFqcOrderMapper.updateById(update);
        }
        for (QmsOqcOrderDO order : qmsOqcOrderMapper.selectListByShippingNoticeId(notice.getId())) {
            QmsOqcOrderDO update = new QmsOqcOrderDO();
            update.setId(order.getId());
            update.setStatus("CANCELED");
            update.setRemark(appendBusinessRemark(order.getRemark(), resetRemark));
            qmsOqcOrderMapper.updateById(update);
        }
        for (QmsCoaReportDO report : qmsCoaReportMapper.selectListByShippingNoticeId(notice.getId())) {
            QmsCoaReportDO update = new QmsCoaReportDO();
            update.setId(report.getId());
            update.setReportStatus("VOIDED");
            update.setVoidedByName(currentUserName());
            update.setVoidedTime(now);
            update.setVoidReason(changeReason);
            update.setRemark(appendBusinessRemark(report.getRemark(), resetRemark));
            qmsCoaReportMapper.updateById(update);
            qmsCoaReportMapper.update(null, new LambdaUpdateWrapper<QmsCoaReportDO>()
                    .eq(QmsCoaReportDO::getId, report.getId())
                    .set(QmsCoaReportDO::getPrintSnapshotHtml, null)
                    .set(QmsCoaReportDO::getPrintSnapshotHash, null)
                    .set(QmsCoaReportDO::getPdfFileUrl, null)
                    .set(QmsCoaReportDO::getPdfFileHash, null)
                    .set(QmsCoaReportDO::getVerificationCode, null));
        }
        HcFgOutboundOrderDO outboundOrder = hcFgOutboundOrderMapper.selectBySourceNoticeId(notice.getId());
        if (outboundOrder != null) {
            for (HcFgOutboundBoxItemDO boxItem : hcFgOutboundBoxItemMapper.selectListByOutboundOrderId(outboundOrder.getId())) {
                if (boxItem.getFinishedStockId() != null) {
                    stockIds.add(boxItem.getFinishedStockId());
                }
                hcFgOutboundBoxItemMapper.deleteById(boxItem.getId());
            }
            for (HcFgOutboundBoxDO box : hcFgOutboundBoxMapper.selectListByOutboundOrderId(outboundOrder.getId())) {
                hcFgOutboundBoxMapper.deleteById(box.getId());
            }
            HcFgOutboundOrderDO update = new HcFgOutboundOrderDO();
            update.setId(outboundOrder.getId());
            update.setOutboundStatus("CANCELED");
            update.setBoxCount(0);
            update.setPieceCount(0);
            update.setRemark(appendBusinessRemark(outboundOrder.getRemark(), resetRemark));
            hcFgOutboundOrderMapper.updateById(update);
        }
        stockIds.forEach(this::refreshFinishedStockShippingLockStatus);
    }

    private void clearShippingNoticeItemExecution(Long itemId) {
        hcFgShippingNoticeItemMapper.update(null, new LambdaUpdateWrapper<HcFgShippingNoticeItemDO>()
                .eq(HcFgShippingNoticeItemDO::getId, itemId)
                .set(HcFgShippingNoticeItemDO::getFinishedStockId, null)
                .set(HcFgShippingNoticeItemDO::getActualFinishedStockId, null)
                .set(HcFgShippingNoticeItemDO::getStockNo, null)
                .set(HcFgShippingNoticeItemDO::getActualStockNo, null)
                .set(HcFgShippingNoticeItemDO::getOuterBoxNo, null)
                .set(HcFgShippingNoticeItemDO::getInnerUnitNo, null)
                .set(HcFgShippingNoticeItemDO::getPackageNo, null)
                .set(HcFgShippingNoticeItemDO::getActualSliceBatchNo, null)
                .set(HcFgShippingNoticeItemDO::getActualShipQty, 0)
                .set(HcFgShippingNoticeItemDO::getActualLocationCode, null)
                .set(HcFgShippingNoticeItemDO::getActualLocationName, null)
                .set(HcFgShippingNoticeItemDO::getOqcOrderId, null)
                .set(HcFgShippingNoticeItemDO::getOqcStatus, null)
                .set(HcFgShippingNoticeItemDO::getShippingQualityNo, null)
                .set(HcFgShippingNoticeItemDO::getShippingInspectorName, null)
                .set(HcFgShippingNoticeItemDO::getShippingInspectionResult, null)
                .set(HcFgShippingNoticeItemDO::getShippingInspectionRemark, null)
                .set(HcFgShippingNoticeItemDO::getShippingInspectionTime, null)
                .set(HcFgShippingNoticeItemDO::getShippingPackageName, null)
                .set(HcFgShippingNoticeItemDO::getShippingPackageTime, null)
                .set(HcFgShippingNoticeItemDO::getShippingPackageRemark, null)
                .set(HcFgShippingNoticeItemDO::getShippedName, null)
                .set(HcFgShippingNoticeItemDO::getShippedTime, null)
                .set(HcFgShippingNoticeItemDO::getLockStatus, NOTICE_STATUS_SUBMITTED));
    }

    private void syncShippingNoticeAttachmentNo(Long noticeId, String noticeNo) {
        for (HcFgShippingNoticeAttachmentDO attachment : hcFgShippingNoticeAttachmentMapper.selectListByNoticeId(noticeId)) {
            if (Objects.equals(attachment.getNoticeNo(), noticeNo)) {
                continue;
            }
            HcFgShippingNoticeAttachmentDO update = new HcFgShippingNoticeAttachmentDO();
            update.setId(attachment.getId());
            update.setNoticeNo(noticeNo);
            hcFgShippingNoticeAttachmentMapper.updateById(update);
        }
    }

    private String syncShippingNoticeDownstreamSnapshots(HcFgShippingNoticeDO notice,
                                                           List<HcFgShippingNoticeItemDO> noticeItems,
                                                           boolean executionRebuilt, String changeReason) {
        Map<Long, HcFgShippingNoticeItemDO> itemMap = new LinkedHashMap<>();
        for (HcFgShippingNoticeItemDO item : noticeItems) {
            itemMap.put(item.getId(), item);
        }
        int effectiveQty = noticeItems.stream()
                .filter(item -> !NOTICE_STATUS_CANCELLED.equals(item.getLockStatus()))
                .mapToInt(item -> intValue(item.getLockedQty()))
                .sum();
        String firstBatchNo = noticeItems.stream()
                .filter(item -> !NOTICE_STATUS_CANCELLED.equals(item.getLockStatus()))
                .map(item -> firstNotBlank(item.getCustomerProductBatchNo(), item.getBatchNo()))
                .filter(StrUtil::isNotBlank)
                .findFirst().orElse(notice.getRequiredBatchNo());
        for (HcFgShippingNoticePickItemDO pickItem : hcFgShippingNoticePickItemMapper.selectListByNoticeId(notice.getId())) {
            HcFgShippingNoticeItemDO sourceItem = itemMap.get(pickItem.getSourceNoticeItemId());
            HcFgShippingNoticePickItemDO update = new HcFgShippingNoticePickItemDO();
            update.setId(pickItem.getId());
            update.setNoticeNo(notice.getNoticeNo());
            if (sourceItem != null) {
                update.setBatchNo(sourceItem.getBatchNo());
                update.setInternalModelCode(sourceItem.getInternalModelCode());
                update.setInternalItemCode(sourceItem.getInternalItemCode());
                update.setCustomerProductBatchNo(sourceItem.getCustomerProductBatchNo());
                update.setMaterialCode(sourceItem.getMaterialCode());
                update.setMaterialName(sourceItem.getMaterialName());
                update.setModelCode(sourceItem.getModelCode());
                update.setProductSize(sourceItem.getProductSize());
                update.setLockedQty(sourceItem.getLockedQty());
            }
            hcFgShippingNoticePickItemMapper.updateById(update);
        }
        for (QmsFqcOrderDO order : qmsFqcOrderMapper.selectListBySource(
                QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC, notice.getId())) {
            QmsFqcOrderDO update = new QmsFqcOrderDO();
            update.setId(order.getId());
            update.setReportNo(notice.getNoticeNo());
            update.setWorkOrderNo(firstNotBlank(notice.getErpOrderNo(), notice.getOrderNo(), notice.getNoticeNo()));
            update.setSourceReportNo(notice.getNoticeNo());
            update.setMaterialCode(notice.getMaterialCode());
            update.setMaterialName(notice.getMaterialName());
            update.setSpecification(notice.getProductSize());
            update.setProductModel(notice.getModelCode());
            update.setProductBatchNo(firstBatchNo);
            update.setBatchNo(firstNotBlank(notice.getNoticeNo(), firstBatchNo));
            update.setProduceQty(BigDecimal.valueOf(effectiveQty));
            hcFgShippingFqcOrderSnapshotDetails(order, notice, itemMap);
            qmsFqcOrderMapper.updateById(update);
        }
        for (QmsOqcOrderDO order : qmsOqcOrderMapper.selectListByShippingNoticeId(notice.getId())) {
            QmsOqcOrderDO update = new QmsOqcOrderDO();
            update.setId(order.getId());
            update.setShippingNo(notice.getNoticeNo());
            update.setNoticeNo(notice.getNoticeNo());
            update.setCustomerId(notice.getCustomerId());
            update.setCustomerCode(notice.getCustomerCode());
            update.setCustomerName(notice.getCustomerName());
            update.setMaterialCode(notice.getMaterialCode());
            update.setMaterialName(notice.getMaterialName());
            update.setModelCode(notice.getModelCode());
            update.setSpecification(notice.getProductSize());
            update.setProductSize(notice.getProductSize());
            update.setBatchNo(notice.getNoticeNo());
            update.setCustomerBatchNo(firstBatchNo);
            update.setShippingQty(BigDecimal.valueOf(effectiveQty));
            update.setShippingPieceQty(BigDecimal.valueOf(effectiveQty));
            qmsOqcOrderMapper.updateById(update);
        }
        for (QmsCoaReportDO report : qmsCoaReportMapper.selectListByShippingNoticeId(notice.getId())) {
            QmsCoaReportDO update = new QmsCoaReportDO();
            update.setId(report.getId());
            update.setShippingNoticeNo(notice.getNoticeNo());
            update.setCustomerId(notice.getCustomerId());
            update.setCustomerCode(notice.getCustomerCode());
            update.setCustomerName(notice.getCustomerName());
            update.setCustomerProductCode(notice.getExternalProductCode());
            update.setCustomerProductName(notice.getExternalProductInfo());
            update.setSalesOrderNo(notice.getOrderNo());
            update.setErpOrderNo(notice.getErpOrderNo());
            update.setShippingTime(notice.getShippingTime());
            update.setMaterialCode(notice.getMaterialCode());
            update.setMaterialName(notice.getMaterialName());
            update.setProductModelCode(notice.getModelCode());
            update.setExternalProductCode(notice.getExternalProductCode());
            update.setExternalProductModel(notice.getExternalProductModel());
            update.setProductionBatchNo(notice.getRequiredBatchNo());
            update.setCustomerBatchNo(firstBatchNo);
            update.setManufactureDate(notice.getRequiredProductionDate());
            update.setExpiryDate(notice.getRequiredExpiryDate());
            update.setShippingQuantity(BigDecimal.valueOf(effectiveQty));
            update.setShippingItemCount((int) noticeItems.stream()
                    .filter(item -> !NOTICE_STATUS_CANCELLED.equals(item.getLockStatus())).count());
            if (executionRebuilt) {
                update.setRemark(appendBusinessRemark(report.getRemark(), "需求变更后待重新生成，原因：" + changeReason));
            }
            qmsCoaReportMapper.updateById(update);
            for (QmsCoaReportShippingRelDO rel : qmsCoaReportShippingRelMapper.selectListByReportId(report.getId())) {
                HcFgShippingNoticeItemDO sourceItem = itemMap.get(rel.getShippingNoticeItemId());
                QmsCoaReportShippingRelDO relUpdate = new QmsCoaReportShippingRelDO();
                relUpdate.setId(rel.getId());
                relUpdate.setShippingNoticeNo(notice.getNoticeNo());
                if (sourceItem != null) {
                    relUpdate.setCustomerBatchNo(sourceItem.getCustomerProductBatchNo());
                    relUpdate.setCustomerModelCode(sourceItem.getCustomerModelCode());
                    relUpdate.setMaterialCode(sourceItem.getMaterialCode());
                    relUpdate.setModelCode(sourceItem.getModelCode());
                    relUpdate.setShippingQuantity(BigDecimal.valueOf(intValue(sourceItem.getLockedQty())));
                }
                qmsCoaReportShippingRelMapper.updateById(relUpdate);
            }
        }
        HcFgOutboundOrderDO outboundOrder = hcFgOutboundOrderMapper.selectBySourceNoticeId(notice.getId());
        if (outboundOrder != null) {
            HcFgOutboundOrderDO update = new HcFgOutboundOrderDO();
            update.setId(outboundOrder.getId());
            update.setOutboundNo(notice.getNoticeNo());
            update.setShippingOrderNo(notice.getNoticeNo());
            update.setShippingNoticeNo(notice.getNoticeNo());
            update.setCustomerCode(notice.getCustomerCode());
            update.setCustomerName(notice.getCustomerName());
            update.setErpOrderNo(notice.getErpOrderNo());
            update.setOrderNo(notice.getOrderNo());
            update.setMaterialCode(notice.getMaterialCode());
            update.setMaterialName(notice.getMaterialName());
            update.setModelCode(notice.getModelCode());
            update.setProductType(notice.getProductType());
            update.setProductSize(notice.getProductSize());
            update.setShippingTime(notice.getShippingTime());
            update.setShipQty(effectiveQty);
            hcFgOutboundOrderMapper.updateById(update);
            for (HcFgOutboundBoxDO box : hcFgOutboundBoxMapper.selectListByOutboundOrderId(outboundOrder.getId())) {
                HcFgOutboundBoxDO boxUpdate = new HcFgOutboundBoxDO();
                boxUpdate.setId(box.getId());
                boxUpdate.setOutboundNo(notice.getNoticeNo());
                boxUpdate.setErpOrderNo(notice.getErpOrderNo());
                boxUpdate.setMaterialCode(notice.getMaterialCode());
                boxUpdate.setMaterialName(notice.getMaterialName());
                boxUpdate.setModelCode(notice.getModelCode());
                hcFgOutboundBoxMapper.updateById(boxUpdate);
            }
        }
        return executionRebuilt
                ? "配货库存、FQC、OQC、COA及未完成出库包装已作废并按新需求回退重建"
                : "配货、FQC、OQC、COA及出库单据的业务快照已同步";
    }

    private void hcFgShippingFqcOrderSnapshotDetails(QmsFqcOrderDO order, HcFgShippingNoticeDO notice,
                                                      Map<Long, HcFgShippingNoticeItemDO> itemMap) {
        for (QmsFqcShippingDetailDO detail : qmsFqcShippingDetailMapper.selectListByFqcId(order.getId())) {
            HcFgShippingNoticeItemDO sourceItem = itemMap.get(detail.getShippingNoticeItemId());
            QmsFqcShippingDetailDO update = new QmsFqcShippingDetailDO();
            update.setId(detail.getId());
            update.setShippingNoticeNo(notice.getNoticeNo());
            update.setCustomerId(notice.getCustomerId());
            update.setCustomerCode(notice.getCustomerCode());
            update.setCustomerName(notice.getCustomerName());
            update.setErpOrderNo(notice.getErpOrderNo());
            if (sourceItem != null) {
                update.setCustomerProductBatchNo(sourceItem.getCustomerProductBatchNo());
                update.setPackageSliceNo(sourceItem.getPackageSliceNo());
                update.setMaterialCode(sourceItem.getMaterialCode());
                update.setMaterialName(sourceItem.getMaterialName());
                update.setModelCode(sourceItem.getModelCode());
                update.setInternalItemCode(sourceItem.getInternalItemCode());
                update.setProductSize(sourceItem.getProductSize());
                update.setShippingQty(sourceItem.getLockedQty());
            }
            qmsFqcShippingDetailMapper.updateById(update);
        }
    }

    private void insertShippingNoticeChangeLog(HcFgShippingNoticeDO beforeNotice,
                                               List<HcFgShippingNoticeItemDO> beforeItems,
                                               HcFgShippingNoticeDO afterNotice,
                                               List<HcFgShippingNoticeItemDO> afterItems,
                                               String changeReason, boolean executionRebuilt,
                                               String downstreamSync) {
        hcFgShippingNoticeChangeLogMapper.insert(HcFgShippingNoticeChangeLogDO.builder()
                .tenantId(afterNotice.getTenantId())
                .noticeId(afterNotice.getId())
                .noticeNo(afterNotice.getNoticeNo())
                .changeVersion(afterNotice.getChangeVersion())
                .beforeStatus(beforeNotice.getNoticeStatus())
                .afterStatus(afterNotice.getNoticeStatus())
                .changeReason(changeReason)
                .changedFields(executionRebuilt ? "执行关键字段或客户批号明细" : "业务快照字段")
                .downstreamSync(downstreamSync)
                .beforeSnapshotJson(buildShippingNoticeChangeSnapshot(beforeNotice, beforeItems))
                .afterSnapshotJson(buildShippingNoticeChangeSnapshot(afterNotice, afterItems))
                .build());
    }

    private String buildShippingNoticeChangeSnapshot(HcFgShippingNoticeDO notice,
                                                      List<HcFgShippingNoticeItemDO> items) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("notice", notice);
        snapshot.put("items", items);
        return JsonUtils.toJsonString(snapshot);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeExcelImportRespVO importShippingNoticeExcel(MultipartFile file, String fileUrl, String operatorName) {
        if (file == null || file.isEmpty()) {
            throw invalidParamException("请上传Excel文件");
        }
        String fileName = normalizeExcelFileName(firstNotBlank(file.getOriginalFilename(), "发货需求单导入.xlsx"));
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx") && !fileName.toLowerCase(Locale.ROOT).endsWith(".xls")) {
            throw invalidParamException("仅支持导入 .xlsx/.xls 文件");
        }
        Long sameFileCount = hcFgShippingNoticeAttachmentMapper.countByImportFileName(
                SHIPPING_NOTICE_ATTACHMENT_EXCEL_IMPORT, fileName);
        if (sameFileCount != null && sameFileCount > 0) {
            throw invalidParamException("Excel文件[" + fileName + "]已导入，不能重复导入");
        }
        String attachmentUrl = StrUtil.trimToNull(fileUrl);
        if (StrUtil.isBlank(attachmentUrl)) {
            throw invalidParamException("原始Excel附件URL不能为空，请先上传原始文件");
        }
        List<ImportedShippingNoticeSheet> sheets = parseShippingNoticeExcel(file);
        List<Long> noticeIds = new ArrayList<>();
        for (ImportedShippingNoticeSheet sheet : sheets) {
            ShippingNoticeRespVO notice = saveAndLockShippingNotice(buildImportShippingNoticeReq(sheet, operatorName));
            insertShippingNoticeExcelAttachment(notice, fileName, attachmentUrl, file.getSize(), sheet);
            noticeIds.add(notice.getId());
        }
        ShippingNoticeExcelImportRespVO resp = new ShippingNoticeExcelImportRespVO();
        resp.setImportCount(noticeIds.size());
        resp.setFileName(fileName);
        resp.setFileUrl(attachmentUrl);
        resp.setNotices(noticeIds.stream().map(this::getShippingNotice).toList());
        return resp;
    }

    private List<ImportedShippingNoticeSheet> parseShippingNoticeExcel(MultipartFile file) {
        List<ImportedShippingNoticeSheet> sheets = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
                    continue;
                }
                sheets.add(parseShippingNoticeSheet(sheet, formatter));
            }
        } catch (Exception ex) {
            throw invalidParamException("Excel解析失败：" + firstNotBlank(ex.getMessage(), ex.getClass().getSimpleName()));
        }
        if (sheets.isEmpty()) {
            throw invalidParamException("Excel未识别到可导入的发货需求单Sheet");
        }
        for (int i = 0; i < sheets.size(); i++) {
            sheets.get(i).sheetIndex = i + 1;
            sheets.get(i).sheetTotal = sheets.size();
        }
        return sheets;
    }

    private ImportedShippingNoticeSheet parseShippingNoticeSheet(Sheet sheet, DataFormatter formatter) {
        String sheetName = sheet.getSheetName();
        String externalProductModel = cellText(sheet, formatter, 2, 1);
        String externalProductCode = cellText(sheet, formatter, 2, 3);
        String externalProductInfo = cellText(sheet, formatter, 3, 1);
        Integer requiredShipQty = parseFirstInteger(cellText(sheet, formatter, 3, 3));
        String requiredSliceRange = cellText(sheet, formatter, 4, 1);
        LocalDate requiredProductionDate = cellLocalDate(sheet, formatter, 4, 3);
        String requiredBatchNo = cellText(sheet, formatter, 5, 1);
        LocalDate requiredExpiryDate = cellLocalDate(sheet, formatter, 5, 3);
        String packingRequirement = cellText(sheet, formatter, 6, 1);
        String externalModelCode = extractProductModelCode(externalProductModel);
        String customerName = firstNotBlank(cellText(sheet, formatter, 6, 3), externalModelCode, "Excel导入客户");
        int detailHeaderRow = findShippingNoticeDetailHeaderRow(sheet, formatter);
        if (StrUtil.isBlank(externalProductModel) || requiredShipQty == null || detailHeaderRow < 0) {
            throw invalidParamException("Sheet[" + sheetName + "]不是有效的发货记录表结构");
        }
        List<ShippingNoticeSaveItemReqVO> items = new ArrayList<>();
        for (int rowIndex = detailHeaderRow + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            String internalItemCode = cellText(sheet, formatter, rowIndex, 0);
            String productBatchNo = cellText(sheet, formatter, rowIndex, 1);
            String packageSliceNo = cellText(sheet, formatter, rowIndex, 2);
            // 发货记录表的明细下方还有确认、备注和签字区；只有批号或包装片号存在时才是明细行。
            if (StrUtil.isBlank(productBatchNo) && StrUtil.isBlank(packageSliceNo)) {
                continue;
            }
            if (StrUtil.isBlank(internalItemCode)) {
                throw invalidParamException("Sheet[" + sheetName + "]第" + (rowIndex + 1) + "行内部编号不能为空");
            }
            ImportedInternalItemCode parsedInternal = parseImportedInternalItemCode(internalItemCode);
            if (StrUtil.isBlank(parsedInternal.internalModelCode)) {
                throw invalidParamException("Sheet[" + sheetName + "]第" + (rowIndex + 1) + "行内部编号无法识别型号：" + internalItemCode);
            }
            HcProductModelMaterialDO modelMaterial = selectImportModelMaterial(parsedInternal.internalModelCode);
            ShippingNoticeSaveItemReqVO item = new ShippingNoticeSaveItemReqVO();
            item.setInternalModelCode(parsedInternal.internalModelCode);
            item.setInternalItemCode(firstNotBlank(parsedInternal.internalItemCode, parsedInternal.rawText));
            item.setCustomerModelCode(parsedInternal.internalModelCode);
            item.setCustomerSliceBatchNo(firstNotBlank(parsedInternal.internalItemCode, parsedInternal.rawText));
            item.setCustomerProductBatchNo(firstNotBlank(StrUtil.trimToNull(productBatchNo), requiredBatchNo));
            item.setBatchNo(firstNotBlank(StrUtil.trimToNull(productBatchNo), requiredBatchNo));
            item.setPackageSliceNo(StrUtil.trimToNull(packageSliceNo));
            item.setMaterialCode(modelMaterial == null ? null : StrUtil.trimToNull(modelMaterial.getMaterialCode()));
            item.setMaterialName(modelMaterial == null ? null : StrUtil.trimToNull(modelMaterial.getMaterialName()));
            item.setModelCode(parsedInternal.internalModelCode);
            if (StrUtil.isNotBlank(parsedInternal.rawText) && !Objects.equals(parsedInternal.rawText,
                    formatImportedInternalItemCode(parsedInternal))) {
                item.setRemark("Excel内部编号：" + limitText(parsedInternal.rawText, 450));
            }
            item.setShipQty(1);
            items.add(item);
        }
        if (items.isEmpty()) {
            throw invalidParamException("Sheet[" + sheetName + "]未识别到发货明细");
        }
        ImportedShippingNoticeSheet imported = new ImportedShippingNoticeSheet();
        imported.sheetName = sheetName;
        imported.customerName = customerName;
        imported.externalProductModel = firstNotBlank(externalModelCode, externalProductModel);
        imported.externalProductCode = externalProductCode;
        imported.externalProductInfo = firstNotBlank(externalProductInfo, externalProductModel);
        imported.requiredShipQty = requiredShipQty;
        imported.requiredSliceRange = requiredSliceRange;
        imported.requiredBatchNo = requiredBatchNo;
        imported.requiredProductionDate = requiredProductionDate;
        imported.requiredExpiryDate = requiredExpiryDate;
        imported.packingRequirement = packingRequirement;
        imported.items = items;
        return imported;
    }

    private ShippingNoticeSaveLockReqVO buildImportShippingNoticeReq(ImportedShippingNoticeSheet sheet, String operatorName) {
        ShippingNoticeSaveLockReqVO reqVO = new ShippingNoticeSaveLockReqVO();
        reqVO.setCustomerName(sheet.customerName);
        reqVO.setProductType(PRODUCT_TYPE_MASS);
        reqVO.setMaterialCode(null);
        reqVO.setMaterialName(null);
        reqVO.setModelCode(extractProductModelCode(sheet.externalProductModel));
        reqVO.setProductSize(extractProductSize(sheet.externalProductModel));
        reqVO.setExternalProductModel(StrUtil.trimToNull(sheet.externalProductModel));
        reqVO.setExternalProductCode(StrUtil.trimToNull(sheet.externalProductCode));
        reqVO.setExternalProductInfo(StrUtil.trimToNull(sheet.externalProductInfo));
        reqVO.setRequiredShipQty(sheet.requiredShipQty);
        reqVO.setRequiredSliceRange(StrUtil.trimToNull(sheet.requiredSliceRange));
        reqVO.setRequiredBatchNo(StrUtil.trimToNull(sheet.requiredBatchNo));
        reqVO.setRequiredProductionDate(sheet.requiredProductionDate);
        reqVO.setRequiredExpiryDate(sheet.requiredExpiryDate);
        reqVO.setPackingRequirement(StrUtil.trimToNull(sheet.packingRequirement));
        reqVO.setNoticeQty(firstPositive(sheet.requiredShipQty, sheet.items.size()));
        reqVO.setRecorderName(firstNotBlank(operatorName, currentUserName()));
        reqVO.setRemark("Excel导入：" + sheet.sheetName);
        reqVO.setItems(sheet.items);
        return reqVO;
    }

    private String normalizeExcelFileName(String fileName) {
        String normalized = StrUtil.trimToNull(fileName);
        if (StrUtil.isBlank(normalized)) {
            return "发货需求单导入.xlsx";
        }
        int slashIndex = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        return slashIndex >= 0 && slashIndex + 1 < normalized.length()
                ? normalized.substring(slashIndex + 1)
                : normalized;
    }

    private void insertShippingNoticeExcelAttachment(ShippingNoticeRespVO notice, String fileName, String fileUrl, long fileSize,
                                                     ImportedShippingNoticeSheet sheet) {
        HcFgShippingNoticeAttachmentDO attachment = HcFgShippingNoticeAttachmentDO.builder()
                .tenantId(currentTenantId())
                .noticeId(notice.getId())
                .noticeNo(notice.getNoticeNo())
                .attachmentName(fileName)
                .attachmentUrl(fileUrl)
                .attachmentType(SHIPPING_NOTICE_ATTACHMENT_EXCEL_IMPORT)
                .sourceFileName(fileName)
                .sourceSheetName(sheet.sheetName)
                .sourceSheetIndex(sheet.sheetIndex)
                .sourceSheetTotal(sheet.sheetTotal)
                .fileSize(fileSize)
                .remark("Excel导入原始文件")
                .build();
        hcFgShippingNoticeAttachmentMapper.insert(attachment);
    }

    private int findShippingNoticeDetailHeaderRow(Sheet sheet, DataFormatter formatter) {
        for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            String first = cellText(sheet, formatter, rowIndex, 0);
            String second = cellText(sheet, formatter, rowIndex, 1);
            String third = cellText(sheet, formatter, rowIndex, 2);
            if ("内部编号".equals(first) && "产品批号".equals(second) && "包装片号".equals(third)) {
                return rowIndex;
            }
        }
        return -1;
    }

    private String cellText(Sheet sheet, DataFormatter formatter, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        return StrUtil.trimToNull(formatter.formatCellValue(cell));
    }

    private LocalDate cellLocalDate(Sheet sheet, DataFormatter formatter, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        Cell cell = row == null ? null : row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        String displayText = cellText(sheet, formatter, rowIndex, colIndex);
        LocalDate parsedText = tryParseLocalDateText(displayText);
        if (parsedText != null) {
            return parsedText;
        }
        if (isNumericDateCell(cell)) {
            return DateUtil.getLocalDateTime(cell.getNumericCellValue()).toLocalDate();
        }
        if (StrUtil.isBlank(displayText)) {
            return null;
        }
        throw invalidParamException("无法识别日期：" + displayText);
    }

    private boolean isNumericDateCell(Cell cell) {
        CellType cellType = cell.getCellType();
        if (cellType == CellType.FORMULA) {
            cellType = cell.getCachedFormulaResultType();
        }
        return cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell);
    }

    private LocalDate tryParseLocalDateText(String value) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        Matcher matcher = EXCEL_DATE_TEXT_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return LocalDate.of(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(3)));
            } catch (DateTimeException ignored) {
                return null;
            }
        }
        if (text.matches("\\d{5}(\\.0+)?")) {
            try {
                LocalDate serialDate = DateUtil.getLocalDateTime(Double.parseDouble(text)).toLocalDate();
                return serialDate.getYear() >= 2000 && serialDate.getYear() <= 2100 ? serialDate : null;
            } catch (RuntimeException ignored) {
                return null;
            }
        }
        String normalized = text.replace('年', '-').replace('月', '-').replace("日", "")
                .replace('.', '-').replace('/', '-').replace('\\', '-').replaceAll("\\s+", "");
        for (String pattern : List.of("uuuu-MM-dd", "uuuu-M-d")) {
            try {
                return LocalDate.parse(normalized, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
                // Try next supported date pattern.
            }
        }
        return null;
    }

    private Integer parseFirstInteger(String value) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        return matcher.find() ? Integer.valueOf(matcher.group()) : null;
    }

    private String extractProductModelCode(String value) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        return firstNotBlank(extractLeadingCode(text), StrUtil.trimToNull(text.split("[,，]", 2)[0]));
    }

    private String extractProductSize(String value) {
        String text = StrUtil.trimToNull(value);
        if (text == null || (!text.contains(",") && !text.contains("，"))) {
            return null;
        }
        String[] parts = text.split("[,，]");
        return parts.length == 0 ? null : StrUtil.trimToNull(parts[parts.length - 1]);
    }

    private String extractInternalModelCode(String value) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        return firstNotBlank(extractLeadingCode(text), StrUtil.trimToNull(text.split("[（(]", 2)[0]));
    }

    private ImportedInternalItemCode parseImportedInternalItemCode(String value) {
        String rawText = StrUtil.trimToNull(value);
        if (rawText == null) {
            return new ImportedInternalItemCode(null, null, null);
        }
        Matcher bracketMatcher = BRACKET_CODE_PATTERN.matcher(rawText);
        String internalItemCode = bracketMatcher.find() ? StrUtil.trimToNull(bracketMatcher.group(1)) : null;
        String internalModelCode = extractInternalModelCode(rawText);
        if (StrUtil.isBlank(internalModelCode) && StrUtil.isNotBlank(internalItemCode)) {
            internalModelCode = extractLeadingCode(internalItemCode);
        }
        if (StrUtil.isBlank(internalItemCode)) {
            Matcher codeMatcher = LEADING_CODE_PATTERN.matcher(rawText);
            if (codeMatcher.find() && codeMatcher.find()) {
                internalItemCode = StrUtil.trimToNull(codeMatcher.group(1));
            }
        }
        if (StrUtil.isBlank(internalItemCode) && StrUtil.isNotBlank(internalModelCode) && !Objects.equals(rawText, internalModelCode)) {
            internalItemCode = StrUtil.trimToNull(rawText.replaceFirst("^" + Pattern.quote(internalModelCode), ""));
        }
        return new ImportedInternalItemCode(limitText(internalModelCode, 100), limitText(internalItemCode, 100),
                limitText(rawText, 100));
    }

    private String formatImportedInternalItemCode(ImportedInternalItemCode itemCode) {
        if (itemCode == null) {
            return null;
        }
        if (StrUtil.isNotBlank(itemCode.internalModelCode) && StrUtil.isNotBlank(itemCode.internalItemCode)) {
            return itemCode.internalModelCode + "（" + itemCode.internalItemCode + "）";
        }
        return firstNotBlank(itemCode.internalModelCode, itemCode.internalItemCode, itemCode.rawText);
    }

    private HcProductModelMaterialDO selectImportModelMaterial(String modelCode) {
        String normalizedModelCode = StrUtil.trimToNull(modelCode);
        if (normalizedModelCode == null) {
            return null;
        }
        return hcProductModelMaterialMapper.selectFirstByModelCode(normalizedModelCode);
    }

    private String extractLeadingCode(String value) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            return null;
        }
        Matcher matcher = LEADING_CODE_PATTERN.matcher(text);
        return matcher.find() ? StrUtil.trimToNull(matcher.group(1)) : null;
    }

    private String limitText(String value, int maxLength) {
        String text = StrUtil.trimToNull(value);
        if (text == null || maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    private static class ImportedShippingNoticeSheet {
        private String sheetName;
        private String customerName;
        private String externalProductModel;
        private String externalProductCode;
        private String externalProductInfo;
        private Integer requiredShipQty;
        private String requiredSliceRange;
        private String requiredBatchNo;
        private LocalDate requiredProductionDate;
        private LocalDate requiredExpiryDate;
        private String packingRequirement;
        private Integer sheetIndex;
        private Integer sheetTotal;
        private List<ShippingNoticeSaveItemReqVO> items = Collections.emptyList();
    }

    private static class ImportedInternalItemCode {
        private final String internalModelCode;
        private final String internalItemCode;
        private final String rawText;

        private ImportedInternalItemCode(String internalModelCode, String internalItemCode, String rawText) {
            this.internalModelCode = internalModelCode;
            this.internalItemCode = internalItemCode;
            this.rawText = rawText;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO issueShippingNotice(ShippingNoticeIssueReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!NOTICE_STATUS_DRAFT.equals(notice.getNoticeStatus())) {
            throw invalidParamException("只有草稿状态的发货需求单允许下达执行");
        }
        List<HcFgShippingNoticeItemDO> items = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId());
        if (!PRODUCT_TYPE_SAMPLE.equals(notice.getProductType()) && items.isEmpty()) {
            throw invalidParamException("量产发货需求单下达前必须维护发货明细");
        }
        if (PRODUCT_TYPE_SAMPLE.equals(notice.getProductType())) {
            items = prepareSampleExecutionItems(notice, items);
        }
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        for (HcFgShippingNoticeItemDO item : items) {
            String internalItemCode = firstNotBlank(item.getInternalItemCode(), item.getCustomerSliceBatchNo());
            if (!PRODUCT_TYPE_SAMPLE.equals(notice.getProductType()) && StrUtil.isBlank(internalItemCode)) {
                throw invalidParamException("发货明细的内部批号不能为空");
            }
            HcFgShippingNoticeItemDO itemUpdate = new HcFgShippingNoticeItemDO();
            itemUpdate.setId(item.getId());
            itemUpdate.setLockStatus(NOTICE_STATUS_SUBMITTED);
            itemUpdate.setLockName(operatorName);
            itemUpdate.setLockTime(now);
            hcFgShippingNoticeItemMapper.updateById(itemUpdate);
        }
        HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
        noticeUpdate.setId(notice.getId());
        noticeUpdate.setNoticeStatus(NOTICE_STATUS_SUBMITTED);
        hcFgShippingNoticeMapper.updateById(noticeUpdate);
        return getShippingNotice(notice.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO cancelShippingNotice(ShippingNoticeCancelReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!NOTICE_EXECUTING_STATUSES.contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有执行中的发货需求单允许取消");
        }
        List<HcFgShippingNoticeItemDO> items = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId());
        List<HcFgShippingNoticePickItemDO> pickItems = hcFgShippingNoticePickItemMapper.selectListByNoticeId(notice.getId());
        if (items.stream().anyMatch(item -> NOTICE_STATUS_SHIPPED.equals(item.getLockStatus()) || item.getShippedTime() != null)
                || pickItems.stream().anyMatch(item -> NOTICE_STATUS_SHIPPED.equals(item.getLockStatus()) || item.getShippedTime() != null)) {
            throw invalidParamException("发货需求单已有出库完成明细，不能取消");
        }
        HcFgOutboundOrderDO outboundOrder = hcFgOutboundOrderMapper.selectBySourceNoticeId(notice.getId());
        if (outboundOrder != null && STATUS_SHIPPED.equalsIgnoreCase(
                StrUtil.blankToDefault(outboundOrder.getOutboundStatus(), ""))) {
            throw invalidParamException("发货需求单已有实际出库单，不能取消");
        }
        List<HcFgShippingNoticePickItemDO> activePickItems = pickItems.stream()
                .filter(item -> HcFgShippingNoticePickItemMapper.ACTIVE_STATUSES.contains(item.getLockStatus()))
                .sorted(Comparator.comparing(HcFgShippingNoticePickItemDO::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (!activePickItems.isEmpty() && !Boolean.TRUE.equals(reqVO.getPhysicalReturned())) {
            throw invalidParamException("请确认整单实物已退回包装工位且已拆除原包装");
        }
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        List<ShippingCancellationReturnContext> returnContexts = prepareShippingCancellationReturns(activePickItems);
        cancelShippingNoticeDownstreamDocuments(notice, outboundOrder, reqVO.getReason(), operatorName, now);

        Set<String> returnedInnerUnitNos = new LinkedHashSet<>();
        Set<String> touchedLocationCodes = new LinkedHashSet<>();
        for (ShippingCancellationReturnContext context : returnContexts) {
            returnShippingCancellationPickToPackagingWait(notice, context, reqVO.getReason(), operatorName, now,
                    returnedInnerUnitNos, touchedLocationCodes);
        }
        returnedInnerUnitNos.stream().filter(StrUtil::isNotBlank).sorted()
                .forEach(this::refreshReturnedInnerPackage);
        touchedLocationCodes.stream().filter(StrUtil::isNotBlank).sorted()
                .forEach(this::refreshLocationOccupied);

        for (HcFgShippingNoticeItemDO item : items) {
            HcFgShippingNoticeItemDO itemUpdate = new HcFgShippingNoticeItemDO();
            itemUpdate.setId(item.getId());
            itemUpdate.setLockStatus(NOTICE_STATUS_CANCELLED);
            itemUpdate.setCancelName(operatorName);
            itemUpdate.setCancelTime(now);
            itemUpdate.setRemark(appendBusinessRemark(item.getRemark(), "整单取消并退回待包装：" + reqVO.getReason()));
            hcFgShippingNoticeItemMapper.updateById(itemUpdate);
        }
        HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
        noticeUpdate.setId(notice.getId());
        noticeUpdate.setNoticeStatus(NOTICE_STATUS_CANCELLED);
        noticeUpdate.setLockedQty(0);
        noticeUpdate.setCancelName(operatorName);
        noticeUpdate.setCancelTime(now);
        noticeUpdate.setCancelReason(StrUtil.trimToNull(reqVO.getReason()));
        hcFgShippingNoticeMapper.updateById(noticeUpdate);
        return getShippingNotice(notice.getId());
    }

    private List<ShippingCancellationReturnContext> prepareShippingCancellationReturns(
            List<HcFgShippingNoticePickItemDO> activePickItems) {
        List<ShippingCancellationReturnContext> contexts = new ArrayList<>();
        Set<String> lockedInnerUnitNos = new HashSet<>();
        for (HcFgShippingNoticePickItemDO pickItem : activePickItems) {
            if (pickItem.getFinishedStockId() == null) {
                throw invalidParamException("发货配货明细缺少成品库存，不能整单退回："
                        + firstNotBlank(pickItem.getActualSliceBatchNo(), pickItem.getSliceBatchNo(), pickItem.getStockNo()));
            }
            HcFinishedStockDO stock = hcFinishedStockMapper.selectByIdForUpdate(pickItem.getFinishedStockId());
            if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
                throw invalidParamException("发货配货库存不存在，不能整单退回："
                        + firstNotBlank(pickItem.getActualSliceBatchNo(), pickItem.getSliceBatchNo(), pickItem.getStockNo()));
            }
            if (STATUS_SHIPPED.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), ""))) {
                throw invalidParamException("片号已实际发货，不能取消："
                        + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            if (!List.of(STATUS_OUTBOUND_LOCKED, STATUS_ALLOCATED)
                    .contains(StrUtil.blankToDefault(stock.getStockStatus(), ""))) {
                throw invalidParamException("发货配货库存状态已变化，不能整单退回："
                        + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            String innerUnitNo = firstNotBlank(stock.getInnerUnitNo(), pickItem.getInnerUnitNo(), pickItem.getPackageNo());
            if (StrUtil.isNotBlank(innerUnitNo) && lockedInnerUnitNos.add(innerUnitNo)) {
                HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(innerUnitNo);
                if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
                    throw invalidParamException("发货配货片原内包装不存在，不能退回待包装：" + innerUnitNo);
                }
            }
            HcInnerPackUnitItemDO packItem = findReturnInnerPackItem(pickItem, stock);
            if (StrUtil.isNotBlank(innerUnitNo) && packItem == null) {
                throw invalidParamException("发货配货片未找到原内包装明细，不能退回待包装："
                        + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            String effectiveInspectionResult = resolveEffectiveShippingCancellationInspectionResult(pickItem);
            String originalQualityStatus = normalizeReturnQualityStatus(firstNotBlank(stock.getQualityStatus(),
                    packItem == null ? null : packItem.getQualityStatus(), pickItem.getQualityStatus()));
            String returnQualityStatus = firstNotBlank(effectiveInspectionResult, originalQualityStatus);
            if (StrUtil.isBlank(returnQualityStatus)) {
                throw invalidParamException("片号缺少可追溯质量状态，不能退回待包装："
                        + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            if (packItem == null && findReturnCutRoundReport(pickItem.getSourceCutRoundReportId(), null, stock) == null) {
                throw invalidParamException("发货配货片未找到包装来源，不能退回待包装："
                        + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            contexts.add(new ShippingCancellationReturnContext(pickItem, stock, packItem,
                    originalQualityStatus, effectiveInspectionResult, returnQualityStatus));
        }
        return contexts;
    }

    private String resolveEffectiveShippingCancellationInspectionResult(HcFgShippingNoticePickItemDO pickItem) {
        QmsFqcShippingDetailDO detail = qmsFqcShippingDetailMapper.selectLatestByShippingPickItemId(pickItem.getId());
        if (detail != null) {
            String detailResult = normalizeReturnQualityStatus(detail.getRowJudgment());
            QmsFqcOrderDO fqcOrder = detail.getFqcId() == null ? null : qmsFqcOrderMapper.selectById(detail.getFqcId());
            if (StrUtil.isNotBlank(detailResult) && fqcOrder != null
                    && INSPECTION_STATUS_COMPLETED.equalsIgnoreCase(StrUtil.blankToDefault(fqcOrder.getStatus(), ""))) {
                return detailResult;
            }
            // 已生成新的 FQC 明细但尚未完成时，不能回退使用旧检验结果，按“未形成有效检测”保留原质量。
            return null;
        }
        if (pickItem.getShippingInspectionTime() != null) {
            return normalizeReturnQualityStatus(pickItem.getShippingInspectionResult());
        }
        return null;
    }

    private String normalizeReturnQualityStatus(String qualityStatus) {
        String normalized = StrUtil.trimToNull(qualityStatus);
        if (normalized == null) {
            return null;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        return List.of(INSPECTION_RESULT_OK, INSPECTION_RESULT_NG, QUALITY_FROZEN).contains(normalized) ? normalized : null;
    }

    private void returnShippingCancellationPickToPackagingWait(HcFgShippingNoticeDO notice,
                                                                ShippingCancellationReturnContext context,
                                                                String cancelReason,
                                                                String operatorName,
                                                                LocalDateTime now,
                                                                Set<String> returnedInnerUnitNos,
                                                                Set<String> touchedLocationCodes) {
        HcFgShippingNoticePickItemDO pickItem = context.pickItem();
        HcFinishedStockDO stock = context.stock();
        HcInnerPackUnitItemDO packItem = context.packItem();
        String sliceBatchNo = firstNotBlank(stock.getSliceBatchNo(), pickItem.getActualSliceBatchNo(),
                pickItem.getSliceBatchNo(), stock.getStockNo());
        String qualitySource = StrUtil.isNotBlank(context.effectiveInspectionResult())
                ? "发货成品检验" : "原有质量";
        String returnRemark = "发货需求单取消拆包退回待包装；片号：" + sliceBatchNo
                + "；原质量：" + context.originalQualityStatus()
                + "；退回质量：" + context.returnQualityStatus()
                + "；判定来源：" + qualitySource
                + "；取消原因：" + cancelReason;

        boolean manualSource = isManualPackagingSource(packItem);
        if (packItem != null) {
            restoreReturnedPieceToPackagingWait(packItem, stock,
                    context.returnQualityStatus(), returnRemark, operatorName);
            hcInnerPackUnitItemMapper.deletePhysicallyByIds(List.of(packItem.getId()));
            returnedInnerUnitNos.add(firstNotBlank(packItem.getInnerUnitNo(), stock.getInnerUnitNo(), pickItem.getInnerUnitNo()));
        }
        if (!manualSource) {
            appendReturnInspectionRemark(pickItem.getSourceCutRoundReportId(), packItem, stock, notice,
                    operatorName, returnRemark, now, context.returnQualityStatus());
        }

        HcFinishedStockDO afterStock = copyFinishedStockForHistory(stock, "RETURNED_FOR_REPACK");
        afterStock.setQualityStatus(context.returnQualityStatus());
        recordFinishedStockHistory(stock, afterStock, "FG_SHIPPING_CANCEL_RETURN", now, operatorName,
                "FG_SHIPPING_NOTICE", notice.getId(), notice.getNoticeNo(), returnRemark);
        hcFinishedStockMapper.deletePhysicallyByIds(List.of(stock.getId()));
        if (StrUtil.isNotBlank(stock.getLocationCode())) {
            touchedLocationCodes.add(stock.getLocationCode());
        }

        HcFgShippingNoticePickItemDO pickUpdate = new HcFgShippingNoticePickItemDO();
        pickUpdate.setId(pickItem.getId());
        pickUpdate.setLockStatus(NOTICE_STATUS_CANCELLED);
        pickUpdate.setCancelName(operatorName);
        pickUpdate.setCancelTime(now);
        pickUpdate.setQualityStatus(context.returnQualityStatus());
        pickUpdate.setRemark(appendBusinessRemark(pickItem.getRemark(), returnRemark));
        hcFgShippingNoticePickItemMapper.updateById(pickUpdate);
    }

    private boolean isManualPackagingSource(HcInnerPackUnitItemDO packItem) {
        if (packItem == null) {
            return false;
        }
        String sourceType = StrUtil.trimToEmpty(packItem.getSourceType()).toUpperCase(Locale.ROOT);
        return SOURCE_MANUAL_HISTORY.equals(sourceType)
                || StrUtil.isBlank(sourceType) && packItem.getSourceManualPieceId() != null;
    }

    private void cancelShippingNoticeDownstreamDocuments(HcFgShippingNoticeDO notice,
                                                          HcFgOutboundOrderDO outboundOrder,
                                                          String cancelReason,
                                                          String operatorName,
                                                          LocalDateTime now) {
        String cancelRemark = "发货需求单整单取消并退回待包装，原因：" + cancelReason;
        for (QmsFqcOrderDO order : qmsFqcOrderMapper.selectListBySource(
                QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC, notice.getId())) {
            QmsFqcOrderDO update = new QmsFqcOrderDO();
            update.setId(order.getId());
            if (!INSPECTION_STATUS_COMPLETED.equalsIgnoreCase(StrUtil.blankToDefault(order.getStatus(), ""))) {
                update.setStatus("CANCELED");
            }
            update.setRemark(appendBusinessRemark(order.getRemark(), cancelRemark));
            qmsFqcOrderMapper.updateById(update);
        }
        for (QmsOqcOrderDO order : qmsOqcOrderMapper.selectListByShippingNoticeId(notice.getId())) {
            QmsOqcOrderDO update = new QmsOqcOrderDO();
            update.setId(order.getId());
            String oqcStatus = StrUtil.blankToDefault(order.getStatus(), "");
            if (!List.of(INSPECTION_STATUS_COMPLETED, "REJECTED").contains(oqcStatus.toUpperCase(Locale.ROOT))) {
                update.setStatus("CANCELED");
            }
            update.setRemark(appendBusinessRemark(order.getRemark(), cancelRemark));
            qmsOqcOrderMapper.updateById(update);
        }
        for (QmsCoaReportDO report : qmsCoaReportMapper.selectListByShippingNoticeId(notice.getId())) {
            QmsCoaReportDO update = new QmsCoaReportDO();
            update.setId(report.getId());
            update.setReportStatus("VOIDED");
            update.setVoidedByName(operatorName);
            update.setVoidedTime(now);
            update.setVoidReason(cancelReason);
            update.setRemark(appendBusinessRemark(report.getRemark(), cancelRemark));
            qmsCoaReportMapper.updateById(update);
            qmsCoaReportMapper.update(null, new LambdaUpdateWrapper<QmsCoaReportDO>()
                    .eq(QmsCoaReportDO::getId, report.getId())
                    .set(QmsCoaReportDO::getPrintSnapshotHtml, null)
                    .set(QmsCoaReportDO::getPrintSnapshotHash, null)
                    .set(QmsCoaReportDO::getPdfFileUrl, null)
                    .set(QmsCoaReportDO::getPdfFileHash, null)
                    .set(QmsCoaReportDO::getVerificationCode, null));
        }
        if (outboundOrder == null) {
            return;
        }
        for (HcFgOutboundBoxItemDO boxItem : hcFgOutboundBoxItemMapper.selectListByOutboundOrderId(outboundOrder.getId())) {
            hcFgOutboundBoxItemMapper.deleteById(boxItem.getId());
        }
        for (HcFgOutboundBoxDO box : hcFgOutboundBoxMapper.selectListByOutboundOrderId(outboundOrder.getId())) {
            hcFgOutboundBoxMapper.deleteById(box.getId());
        }
        HcFgOutboundOrderDO update = new HcFgOutboundOrderDO();
        update.setId(outboundOrder.getId());
        update.setOutboundStatus("CANCELED");
        update.setBoxCount(0);
        update.setPieceCount(0);
        update.setRemark(appendBusinessRemark(outboundOrder.getRemark(), cancelRemark));
        hcFgOutboundOrderMapper.updateById(update);
    }

    private record ShippingCancellationReturnContext(HcFgShippingNoticePickItemDO pickItem,
                                                     HcFinishedStockDO stock,
                                                     HcInnerPackUnitItemDO packItem,
                                                     String originalQualityStatus,
                                                     String effectiveInspectionResult,
                                                     String returnQualityStatus) {
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShippingNotice(Long id) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(id);
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!NOTICE_STATUS_DRAFT.equals(notice.getNoticeStatus())) {
            throw invalidParamException("只有草稿状态的发货需求单允许删除");
        }
        List<HcFgShippingNoticeItemDO> items = hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId());
        boolean issued = items.stream().anyMatch(item ->
                !NOTICE_STATUS_DRAFT.equals(item.getLockStatus())
                        || item.getFinishedStockId() != null
                        || item.getActualFinishedStockId() != null
                        || StrUtil.isNotBlank(item.getActualSliceBatchNo()));
        if (issued) {
            throw invalidParamException("发货需求单已下达配货/检验/包装任务，不允许删除");
        }
        hcFgShippingNoticeItemMapper.physicalDeleteByNoticeId(notice.getId());
        hcFgShippingNoticePickItemMapper.physicalDeleteByNoticeId(notice.getId());
        hcFgShippingNoticeAttachmentMapper.physicalDeleteByNoticeId(notice.getId());
        hcFgShippingNoticeMapper.physicalDeleteById(notice.getId());
    }

    @Override
    public List<ShippingNoticeRespVO> getOutboundShippingNoticeList(String keyword) {
        return hcFgShippingNoticeMapper.selectOutboundNoticeList(StrUtil.trimToNull(keyword)).stream()
                .map(notice -> buildShippingNoticeResp(notice, false))
                .toList();
    }

    @Override
    public List<ShippingOrderRespVO> getShippingOrderList(String keyword) {
        return hcFinishedPackagingMapper.selectShippingOrderList(StrUtil.trimToNull(keyword));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrderRespVO initOutboundBoxesFromNotice(InitOutboundBoxesFromNoticeReqVO reqVO) {
        int boxCount = positive(reqVO.getBoxCount(), "包装盒数必须大于0");
        int packageSpec = positive(reqVO.getPackageSpec(), "一盒片数必须大于0");
        HcFgShippingNoticeDO notice = getShippingNoticeOrThrow(reqVO.getSourceNoticeId());
        if (!NOTICE_OUTBOUND_READY_STATUSES.contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有发货包装完成或出库中的发货需求单允许初始化发货包装盒");
        }
        List<HcFgShippingNoticePickItemDO> noticeItems = hcFgShippingNoticePickItemMapper.selectListByNoticeId(notice.getId()).stream()
                .filter(item -> NOTICE_OUTBOUND_READY_STATUSES.contains(item.getLockStatus()))
                .toList();
        if (noticeItems.isEmpty()) {
            throw invalidParamException("发货需求单没有可出库的锁定库存明细");
        }
        LocalDateTime now = LocalDateTime.now();
        HcFgOutboundOrderDO order = hcFgOutboundOrderMapper.selectBySourceNoticeId(notice.getId());
        if (order != null && STATUS_SHIPPED.equals(order.getOutboundStatus())) {
            throw invalidParamException("该发货需求单已完成出库，不能重复初始化");
        }
        if (order == null) {
            order = HcFgOutboundOrderDO.builder()
                    .tenantId(notice.getTenantId())
                    .outboundNo(notice.getNoticeNo())
                    .sourceNoticeId(notice.getId())
                    .shippingOrderNo(notice.getNoticeNo())
                    .shippingNoticeNo(notice.getNoticeNo())
                    .customerCode(notice.getCustomerCode())
                    .customerName(notice.getCustomerName())
                    .erpOrderNo(notice.getErpOrderNo())
                    .orderNo(notice.getOrderNo())
                    .materialCode(notice.getMaterialCode())
                    .materialName(notice.getMaterialName())
                    .modelCode(notice.getModelCode())
                    .productType(notice.getProductType())
                    .productSize(notice.getProductSize())
                    .shippingTime(notice.getShippingTime() == null ? now : notice.getShippingTime())
                    .shipQty(intValue(notice.getNoticeQty()))
                    .boxCount(0)
                    .pieceCount(0)
                    .outboundStatus("PACKING")
                    .recorderName(firstNotBlank(reqVO.getRecorderName(), currentUserName()))
                    .recorderTime(now)
                    .remark(reqVO.getRemark())
                    .build();
            hcFgOutboundOrderMapper.insert(order);
        }
        for (int i = 0; i < boxCount; i++) {
            hcFgOutboundBoxMapper.insert(HcFgOutboundBoxDO.builder()
                    .tenantId(order.getTenantId())
                    .outboundOrderId(order.getId())
                    .outboundNo(order.getOutboundNo())
                    .outboundBoxNo(nextNo("FGOBOX"))
                    .erpOrderNo(order.getErpOrderNo())
                    .materialCode(order.getMaterialCode())
                    .materialName(order.getMaterialName())
                    .modelCode(order.getModelCode())
                    .targetQty(packageSpec)
                    .currentQty(0)
                    .boxStatus(STATUS_WAITING_PIECE)
                    .printCount(0)
                    .recorderName(firstNotBlank(reqVO.getRecorderName(), currentUserName()))
                    .recorderTime(now)
                    .remark(reqVO.getRemark())
                    .build());
        }
        if (List.of(NOTICE_STATUS_PACKAGED, NOTICE_STATUS_LOCKED).contains(notice.getNoticeStatus())) {
            HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
            noticeUpdate.setId(notice.getId());
            noticeUpdate.setNoticeStatus(NOTICE_STATUS_OUTBOUND);
            hcFgShippingNoticeMapper.updateById(noticeUpdate);
        }
        for (HcFgShippingNoticePickItemDO item : noticeItems) {
            if (List.of(NOTICE_STATUS_PACKAGED, NOTICE_STATUS_LOCKED).contains(item.getLockStatus())) {
                HcFgShippingNoticePickItemDO itemUpdate = new HcFgShippingNoticePickItemDO();
                itemUpdate.setId(item.getId());
                itemUpdate.setLockStatus(NOTICE_STATUS_OUTBOUND);
                hcFgShippingNoticePickItemMapper.updateById(itemUpdate);
            }
        }
        refreshOutboundOrder(order.getId(), false);
        return buildOutboundOrderResp(hcFgOutboundOrderMapper.selectById(order.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OutboundOrderRespVO initOutboundBoxes(InitOutboundBoxesReqVO reqVO) {
        int boxCount = positive(reqVO.getBoxCount(), "包装盒数必须大于0");
        int packageSpec = positive(reqVO.getPackageSpec(), "一盒片数必须大于0");
        int shipQty = positive(reqVO.getShipQty(), "发货数量必须大于0");
        LocalDateTime now = LocalDateTime.now();
        String outboundNo = firstNotBlank(reqVO.getShippingOrderNo(), nextNo("FGO"));
        HcFgOutboundOrderDO order = hcFgOutboundOrderMapper.selectByOutboundNo(outboundNo);
        if (order == null) {
            order = HcFgOutboundOrderDO.builder()
                    .tenantId(currentTenantId())
                    .outboundNo(outboundNo)
                    .sourceSaleOrderId(reqVO.getSourceSaleOrderId())
                    .shippingOrderNo(reqVO.getShippingOrderNo())
                    .erpOrderNo(reqVO.getErpOrderNo())
                    .materialCode(reqVO.getMaterialCode())
                    .materialName(reqVO.getMaterialName())
                    .modelCode(reqVO.getModelCode())
                    .productSize(reqVO.getProductSize())
                    .shippingTime(reqVO.getShippingTime() == null ? now : reqVO.getShippingTime())
                    .shipQty(shipQty)
                    .boxCount(0)
                    .pieceCount(0)
                    .outboundStatus("PACKING")
                    .recorderName(firstNotBlank(reqVO.getRecorderName(), currentUserName()))
                    .recorderTime(now)
                    .remark(reqVO.getRemark())
                    .build();
            hcFgOutboundOrderMapper.insert(order);
        }
        for (int i = 0; i < boxCount; i++) {
            hcFgOutboundBoxMapper.insert(HcFgOutboundBoxDO.builder()
                    .tenantId(order.getTenantId())
                    .outboundOrderId(order.getId())
                    .outboundNo(order.getOutboundNo())
                    .outboundBoxNo(nextNo("FGOBOX"))
                    .erpOrderNo(order.getErpOrderNo())
                    .materialCode(order.getMaterialCode())
                    .materialName(order.getMaterialName())
                    .modelCode(order.getModelCode())
                    .targetQty(packageSpec)
                    .currentQty(0)
                    .boxStatus(STATUS_WAITING_PIECE)
                    .printCount(0)
                    .recorderName(firstNotBlank(reqVO.getRecorderName(), currentUserName()))
                    .recorderTime(now)
                    .remark(reqVO.getRemark())
                    .build());
        }
        refreshOutboundOrder(order.getId(), false);
        return buildOutboundOrderResp(hcFgOutboundOrderMapper.selectById(order.getId()));
    }

    @Override
    public OutboundOrderRespVO getOutboundOrder(String outboundNo) {
        HcFgOutboundOrderDO order = hcFgOutboundOrderMapper.selectByOutboundNo(StrUtil.trimToEmpty(outboundNo));
        return order == null ? null : buildOutboundOrderResp(order);
    }

    @Override
    public OutboundOrderRespVO getOutboundOrderByNotice(Long sourceNoticeId) {
        HcFgOutboundOrderDO order = sourceNoticeId == null ? null : hcFgOutboundOrderMapper.selectBySourceNoticeId(sourceNoticeId);
        return order == null ? null : buildOutboundOrderResp(order);
    }

    @Override
    public List<OutboundBoxRespVO> getOutboundBoxList(Long outboundOrderId) {
        if (outboundOrderId == null) {
            return Collections.emptyList();
        }
        return hcFgOutboundBoxMapper.selectListByOutboundOrderId(outboundOrderId).stream()
                .map(this::buildOutboundBoxResp)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long scanOutboundPiece(ScanOutboundPieceReqVO reqVO) {
        HcFgOutboundBoxDO box = getOutboundBox(reqVO.getBoxId());
        if (!List.of(STATUS_WAITING_PIECE, STATUS_PACKED).contains(StrUtil.blankToDefault(box.getBoxStatus(), ""))) {
            throw invalidParamException("当前发货包装盒状态不允许继续扫码");
        }
        if (intValue(box.getCurrentQty()) >= intValue(box.getTargetQty())) {
            throw invalidParamException("当前发货包装盒已满，不能继续加入成品片");
        }
        HcFgOutboundOrderDO order = hcFgOutboundOrderMapper.selectById(box.getOutboundOrderId());
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw invalidParamException("发货出库单不存在");
        }
        String sliceBatchNo = StrUtil.trimToEmpty(reqVO.getSliceBatchNo());
        HcFinishedStockDO stock;
        if (order.getSourceNoticeId() != null) {
            stock = hcFinishedStockMapper.selectBySliceBatchNo(sliceBatchNo);
            if (stock == null) {
                throw invalidParamException("未找到该成品库存片号");
            }
            if (hcFgOutboundBoxItemMapper.selectByFinishedStockId(stock.getId()) != null) {
                throw invalidParamException("该成品库存已被发货包装占用，不能重复扫描");
            }
            if (!STATUS_OUTBOUND_LOCKED.equals(stock.getStockStatus())) {
                throw invalidParamException("该成品片未处于当前发货需求单出库锁定状态");
            }
            HcFgShippingNoticePickItemDO noticeItem = hcFgShippingNoticePickItemMapper
                    .selectActiveByNoticeIdAndFinishedStockId(order.getSourceNoticeId(), stock.getId());
            if (noticeItem == null) {
                throw invalidParamException("该成品片不属于当前发货需求单，不能扫码出库");
            }
        } else {
            stock = hcFinishedStockMapper.selectAvailableBySliceBatchNo(sliceBatchNo);
            if (stock == null) {
                throw invalidParamException("未找到可出库的成品库存记录，请确认该片号已完成成品包装入库且未出库");
            }
            if (hcFgOutboundBoxItemMapper.selectByFinishedStockId(stock.getId()) != null) {
                throw invalidParamException("该成品库存已被发货包装占用，不能重复扫描");
            }
        }
        if (!INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.trimToEmpty(stock.getQualityStatus()))) {
            throw invalidParamException("只允许扫描合格成品库存进行发货包装："
                    + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
        }
        if (StrUtil.isNotBlank(box.getMaterialCode()) && StrUtil.isNotBlank(stock.getMaterialCode())
                && !Objects.equals(box.getMaterialCode(), stock.getMaterialCode())) {
            throw invalidParamException("成品片号料号与当前发货单不一致");
        }
        LocalDateTime now = LocalDateTime.now();
        HcFgOutboundBoxItemDO item = HcFgOutboundBoxItemDO.builder()
                .tenantId(box.getTenantId())
                .outboundBoxId(box.getId())
                .outboundBoxNo(box.getOutboundBoxNo())
                .outboundOrderId(box.getOutboundOrderId())
                .outboundNo(box.getOutboundNo())
                .finishedStockId(stock.getId())
                .inboundNo(stock.getInboundNo())
                .inboundBoxNo(stock.getOuterBoxNo())
                .inboundInnerUnitNo(stock.getInnerUnitNo())
                .sliceBatchNo(stock.getSliceBatchNo())
                .materialCode(stock.getMaterialCode())
                .modelCode(stock.getModelCode())
                .batchNo(stock.getBatchNo())
                .qualityStatus(stock.getQualityStatus())
                .scanUserName(firstNotBlank(reqVO.getScanUserName(), currentUserName()))
                .scanTime(now)
                .build();
        hcFgOutboundBoxItemMapper.insert(item);
        HcFinishedStockDO stockUpdate = new HcFinishedStockDO();
        stockUpdate.setId(stock.getId());
        stockUpdate.setStockStatus(STATUS_ALLOCATED);
        hcFinishedStockMapper.updateById(stockUpdate);
        recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, STATUS_ALLOCATED),
                "FG_OUTBOUND_ALLOCATE", now, firstNotBlank(reqVO.getScanUserName(), currentUserName()),
                "FG_OUTBOUND_ORDER", order.getId(), order.getOutboundNo(), null);
        refreshOutboundBoxQty(box.getId());
        refreshOutboundOrder(box.getOutboundOrderId(), false);
        return item.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long printOutboundBox(BoxActionReqVO reqVO) {
        HcFgOutboundBoxDO box = getOutboundBox(reqVO.getId());
        HcFgOutboundBoxDO update = new HcFgOutboundBoxDO();
        update.setId(box.getId());
        update.setLabelNo(firstNotBlank(box.getLabelNo(), "LBL-" + box.getOutboundBoxNo()));
        update.setPrintCount(intValue(box.getPrintCount()) + 1);
        update.setLastPrintTime(LocalDateTime.now());
        hcFgOutboundBoxMapper.updateById(update);
        return box.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long confirmOutboundOrder(BoxActionReqVO reqVO) {
        HcFgOutboundOrderDO order = hcFgOutboundOrderMapper.selectById(reqVO.getId());
        if (order == null || Boolean.TRUE.equals(order.getDeleted())) {
            throw invalidParamException("发货出库单不存在");
        }
        List<HcFgOutboundBoxItemDO> items = hcFgOutboundBoxItemMapper.selectListByOutboundOrderId(order.getId());
        if (items.isEmpty()) {
            throw invalidParamException("当前发货出库单没有扫码片号，不能确认出库");
        }
        if (order.getSourceNoticeId() != null && items.size() < intValue(order.getShipQty())) {
            throw invalidParamException("当前发货需求单锁定片尚未全部扫码包装，不能确认出库");
        }
        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        List<String> locationCodes = new ArrayList<>();
        for (HcFgOutboundBoxItemDO item : items) {
            HcFinishedStockDO stock = hcFinishedStockMapper.selectById(item.getFinishedStockId());
            if (stock != null && StrUtil.isNotBlank(stock.getLocationCode())) {
                locationCodes.add(stock.getLocationCode());
            }
            HcFinishedStockDO stockUpdate = new HcFinishedStockDO();
            stockUpdate.setId(item.getFinishedStockId());
            stockUpdate.setStockStatus(STATUS_SHIPPED);
            hcFinishedStockMapper.updateById(stockUpdate);
            if (stock != null) {
                recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, STATUS_SHIPPED),
                        "FG_SHIP", now, operatorName, "FG_OUTBOUND_ORDER", order.getId(),
                        order.getOutboundNo(), reqVO.getReason());
            }
            if (order.getSourceNoticeId() != null) {
                HcFgShippingNoticePickItemDO noticeItem = hcFgShippingNoticePickItemMapper
                        .selectActiveByNoticeIdAndFinishedStockId(order.getSourceNoticeId(), item.getFinishedStockId());
                if (noticeItem != null) {
                    HcFgShippingNoticePickItemDO noticeItemUpdate = new HcFgShippingNoticePickItemDO();
                    noticeItemUpdate.setId(noticeItem.getId());
                    noticeItemUpdate.setLockStatus(NOTICE_STATUS_SHIPPED);
                    noticeItemUpdate.setShippedName(operatorName);
                    noticeItemUpdate.setShippedTime(now);
                    hcFgShippingNoticePickItemMapper.updateById(noticeItemUpdate);
                    if (noticeItem.getSourceNoticeItemId() != null) {
                        HcFgShippingNoticeItemDO noticeDetailUpdate = new HcFgShippingNoticeItemDO();
                        noticeDetailUpdate.setId(noticeItem.getSourceNoticeItemId());
                        noticeDetailUpdate.setLockStatus(NOTICE_STATUS_SHIPPED);
                        noticeDetailUpdate.setShippedName(operatorName);
                        noticeDetailUpdate.setShippedTime(now);
                        hcFgShippingNoticeItemMapper.updateById(noticeDetailUpdate);
                    }
                }
            }
        }
        for (HcFgOutboundBoxDO box : hcFgOutboundBoxMapper.selectListByOutboundOrderId(order.getId())) {
            HcFgOutboundBoxDO boxUpdate = new HcFgOutboundBoxDO();
            boxUpdate.setId(box.getId());
            boxUpdate.setBoxStatus(STATUS_SHIPPED);
            hcFgOutboundBoxMapper.updateById(boxUpdate);
        }
        locationCodes.stream().filter(StrUtil::isNotBlank).distinct().forEach(this::refreshLocationOccupied);
        if (order.getSourceNoticeId() != null) {
            HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
            noticeUpdate.setId(order.getSourceNoticeId());
            noticeUpdate.setNoticeStatus(NOTICE_STATUS_CLOSED);
            hcFgShippingNoticeMapper.updateById(noticeUpdate);
        }
        refreshOutboundOrder(order.getId(), true);
        HcFgOutboundOrderDO confirm = new HcFgOutboundOrderDO();
        confirm.setId(order.getId());
        confirm.setConfirmerName(operatorName);
        confirm.setConfirmerTime(now);
        hcFgOutboundOrderMapper.updateById(confirm);
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShippingNoticeRespVO submitShippingNoticeDelivery(ShippingNoticeDeliverySubmitReqVO reqVO) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectByIdForUpdate(reqVO.getId());
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        if (!NOTICE_OUTBOUND_READY_STATUSES.contains(notice.getNoticeStatus())) {
            throw invalidParamException("只有发货包装完成或出库中的需求单允许提交发货");
        }

        Map<Long, ShippingNoticeDeliveryItemReqVO> submitItemMap = new LinkedHashMap<>();
        for (ShippingNoticeDeliveryItemReqVO itemReq : reqVO.getItems()) {
            submitItemMap.put(itemReq.getId(), itemReq);
        }

        List<HcFgShippingNoticeItemDO> noticeItems = selectShippingExecutionItems(notice).stream()
                .filter(item -> NOTICE_OUTBOUND_READY_STATUSES.contains(item.getLockStatus()))
                .toList();
        if (noticeItems.isEmpty()) {
            throw invalidParamException("当前发货需求单没有待发货明细");
        }

        LocalDateTime now = LocalDateTime.now();
        String operatorName = firstNotBlank(reqVO.getOperatorName(), currentUserName());
        Set<Long> actualStockIds = new HashSet<>();
        Set<String> locationCodes = new HashSet<>();

        for (HcFgShippingNoticeItemDO item : noticeItems) {
            ShippingNoticeDeliveryItemReqVO itemReq = submitItemMap.get(item.getId());
            if (itemReq == null) {
                throw invalidParamException("请维护全部待发货明细的实际发货信息");
            }

            String actualSliceBatchNo = StrUtil.trim(itemReq.getActualSliceBatchNo());
            if (StrUtil.isBlank(actualSliceBatchNo)) {
                throw invalidParamException("实际发货片号不能为空");
            }
            boolean sameSlice = actualSliceBatchNo.equalsIgnoreCase(StrUtil.blankToDefault(item.getSliceBatchNo(), ""));
            if (!sameSlice && StrUtil.isBlank(itemReq.getMismatchReason())) {
                throw invalidParamException("实际片号与计划片号不一致时必须填写原因：" + item.getSliceBatchNo());
            }

            Long plannedStockId = item.getFinishedStockId() == null ? item.getActualFinishedStockId() : item.getFinishedStockId();
            HcFinishedStockDO plannedStock = plannedStockId == null ? null : hcFinishedStockMapper.selectByIdForUpdate(plannedStockId);
            if (plannedStock == null || Boolean.TRUE.equals(plannedStock.getDeleted())) {
                throw invalidParamException("计划发货库存不存在：" + item.getSliceBatchNo());
            }
            HcFinishedStockDO actualStock = sameSlice
                    ? plannedStock
                    : hcFinishedStockMapper.selectBySliceBatchNoForUpdate(actualSliceBatchNo);
            if (actualStock == null || Boolean.TRUE.equals(actualStock.getDeleted())) {
                throw invalidParamException("实际发货片号不存在：" + actualSliceBatchNo);
            }
            assertStockCoaReleased(actualStock);
            if (STATUS_SHIPPED.equals(actualStock.getStockStatus())) {
                throw invalidParamException("实际发货片号已出库：" + actualSliceBatchNo);
            }
            if (!actualStockIds.add(actualStock.getId())) {
                throw invalidParamException("实际发货片号重复：" + actualSliceBatchNo);
            }

            HcFgShippingNoticeItemDO activeActualLock = hcFgShippingNoticeItemMapper.selectActiveByFinishedStockId(actualStock.getId());
            if (activeActualLock != null && !Objects.equals(activeActualLock.getId(), item.getId())) {
                throw invalidParamException("实际发货片号已被其它需求单锁定：" + actualSliceBatchNo);
            }
            if (!sameSlice && !STATUS_AVAILABLE.equals(actualStock.getStockStatus())) {
                throw invalidParamException("非计划实际片号必须处于可用库存状态：" + actualSliceBatchNo);
            }
            if (sameSlice && !STATUS_OUTBOUND_LOCKED.equals(plannedStock.getStockStatus())) {
                throw invalidParamException("计划发货片号未处于出库锁定状态：" + item.getSliceBatchNo());
            }

            HcFgShippingNoticeItemDO update = new HcFgShippingNoticeItemDO();
            update.setId(item.getId());
            update.setActualFinishedStockId(actualStock.getId());
            update.setActualStockNo(actualStock.getStockNo());
            update.setActualSliceBatchNo(actualStock.getSliceBatchNo());
            update.setActualLocationCode(actualStock.getLocationCode());
            update.setActualLocationName(actualStock.getLocationName());
            update.setActualShipQty(intValue(item.getLockedQty()) <= 0 ? 1 : item.getLockedQty());
            update.setCustomerSliceBatchNo(StrUtil.trimToNull(itemReq.getCustomerSliceBatchNo()));
            update.setCustomerModelCode(StrUtil.trimToNull(itemReq.getCustomerModelCode()));
            update.setShippingQualityNo(StrUtil.trimToNull(itemReq.getShippingQualityNo()));
            update.setShippingInspectorName(StrUtil.trimToNull(itemReq.getShippingInspectorName()));
            update.setMismatchReason(sameSlice ? null : StrUtil.trimToNull(itemReq.getMismatchReason()));
            update.setActualRemark(StrUtil.trimToNull(itemReq.getRemark()));
            update.setLockStatus(NOTICE_STATUS_SHIPPED);
            update.setShippedName(operatorName);
            update.setShippedTime(now);
            hcFgShippingNoticeItemMapper.updateById(update);

            HcFinishedStockDO actualStockUpdate = new HcFinishedStockDO();
            actualStockUpdate.setId(actualStock.getId());
            actualStockUpdate.setStockStatus(STATUS_SHIPPED);
            hcFinishedStockMapper.updateById(actualStockUpdate);
            recordFinishedStockHistory(actualStock, copyFinishedStockForHistory(actualStock, STATUS_SHIPPED),
                    "FG_SHIP", now, operatorName, "FG_SHIPPING_NOTICE", notice.getId(),
                    notice.getNoticeNo(), itemReq.getRemark());

            HcFgShippingNoticePickItemDO pickItem = hcFgShippingNoticePickItemMapper
                    .selectActiveByNoticeIdAndFinishedStockId(notice.getId(), actualStock.getId());
            if (pickItem != null) {
                HcFgShippingNoticePickItemDO pickItemUpdate = new HcFgShippingNoticePickItemDO();
                pickItemUpdate.setId(pickItem.getId());
                pickItemUpdate.setLockStatus(NOTICE_STATUS_SHIPPED);
                pickItemUpdate.setShippedName(operatorName);
                pickItemUpdate.setShippedTime(now);
                hcFgShippingNoticePickItemMapper.updateById(pickItemUpdate);
            }

            if (!sameSlice) {
                refreshFinishedStockShippingLockStatus(plannedStock.getId());
            }
            if (StrUtil.isNotBlank(plannedStock.getLocationCode())) {
                locationCodes.add(plannedStock.getLocationCode());
            }
            if (StrUtil.isNotBlank(actualStock.getLocationCode())) {
                locationCodes.add(actualStock.getLocationCode());
            }
        }

        HcFgShippingNoticeDO noticeUpdate = new HcFgShippingNoticeDO();
        noticeUpdate.setId(notice.getId());
        noticeUpdate.setNoticeStatus(NOTICE_STATUS_CLOSED);
        hcFgShippingNoticeMapper.updateById(noticeUpdate);
        locationCodes.forEach(this::refreshLocationOccupied);
        return getShippingNotice(notice.getId());
    }

    private PageResult<InspectionSliceRespVO> getInspectionSlicePage(InspectionSlicePageReqVO reqVO,
                                                                     String targetInspectionStatus) {
        reqVO.setKeyword(StrUtil.trimToNull(reqVO.getKeyword()));
        int pageNo = Math.max(1, reqVO.getPageNo());
        int pageSize = Math.min(200, Math.max(1, reqVO.getPageSize()));
        int offset = (pageNo - 1) * pageSize;
        Long total = hcFinishedPackagingMapper.countInspectionSlicePage(reqVO, targetInspectionStatus);
        if (total == null || total == 0L) {
            return PageResult.empty();
        }
        List<InspectionSliceRespVO> rows = hcFinishedPackagingMapper.selectInspectionSlicePage(reqVO, targetInspectionStatus, offset, pageSize);
        fillCoaInspectionInfo(rows);
        rows.forEach(row -> row.setSourceType(SOURCE_CUT_ROUND_REPORT));
        return new PageResult<>(rows, total);
    }

    private List<InspectionSliceRespVO> listInboundWaitInspectionSlicesForGrouping(InspectionSlicePageReqVO reqVO) {
        reqVO.setKeyword(StrUtil.trimToNull(reqVO.getKeyword()));
        reqVO.setStockStatus("WAIT_INBOUND");
        // COA 质量状态须在加载 FAI 复检链并得出最终结论后再筛选。
        // Mapper 中的旧 SQL 只能识别历史任一 NG，会将已复检 OK 的待包装片提前排除。
        InspectionSlicePageReqVO inspectionQueryReqVO = BeanUtil.copyProperties(reqVO, InspectionSlicePageReqVO.class);
        inspectionQueryReqVO.setPackagingQualityStatus(null);
        Long total = hcFinishedPackagingMapper.countInspectionSlicePage(inspectionQueryReqVO, INSPECTION_STATUS_COMPLETED);
        List<InspectionSliceRespVO> rows = new ArrayList<>();
        if (total != null && total > 0L) {
            int pageSize = 1000;
            for (int offset = 0; offset < total; offset += pageSize) {
                rows.addAll(hcFinishedPackagingMapper.selectInspectionSlicePage(
                        inspectionQueryReqVO, INSPECTION_STATUS_COMPLETED, offset, pageSize));
            }
            rows.forEach(row -> row.setSourceType(SOURCE_CUT_ROUND_REPORT));
        }
        rows.addAll(listManualInspectionSlices(reqVO));
        fillCoaInspectionInfo(rows);
        rows = filterClaimedPackagingCoaSamples(rows);
        rows = filterInspectionSlicesByPackagingQualityStatus(rows, reqVO.getPackagingQualityStatus());
        String segmentBatchNo = normalizeCoaSegmentBatchNo(reqVO.getSegmentBatchNo());
        if (StrUtil.isBlank(segmentBatchNo)) {
            return rows;
        }
        return rows.stream()
                .filter(row -> segmentBatchNo.equals(row.getSegmentBatchNo()))
                .toList();
    }

    private List<InspectionSliceRespVO> listManualInspectionSlices(InspectionSlicePageReqVO reqVO) {
        return hcPackagingManualPieceMapper.selectWaitPackagingList(reqVO.getKeyword()).stream()
                .filter(piece -> reqVO.getProductionDateStart() == null
                        || (piece.getProductionDate() != null && !piece.getProductionDate().isBefore(reqVO.getProductionDateStart())))
                .filter(piece -> reqVO.getProductionDateEnd() == null
                        || (piece.getProductionDate() != null && !piece.getProductionDate().isAfter(reqVO.getProductionDateEnd())))
                .filter(piece -> StrUtil.isBlank(reqVO.getInspectionResult())
                        || StrUtil.equalsIgnoreCase(reqVO.getInspectionResult(), piece.getInspectionResult()))
                .map(this::buildManualInspectionSlice)
                .toList();
    }

    private InspectionSliceRespVO buildManualInspectionSlice(HcPackagingManualPieceDO piece) {
        InspectionSliceRespVO row = new InspectionSliceRespVO();
        row.setSourceType(SOURCE_MANUAL_HISTORY);
        row.setSourceManualPieceId(piece.getId());
        row.setSliceBatchNo(piece.getSliceBatchNo());
        row.setProductionBatchNo(piece.getSliceBatchNo());
        row.setParentProductionBatchNo(piece.getSegmentBatchNo());
        row.setSegmentBatchNo(piece.getSegmentBatchNo());
        row.setCoaScopeBatchNo(piece.getSegmentBatchNo());
        row.setMaterialCode(piece.getMaterialCode());
        row.setMaterialName(piece.getMaterialName());
        row.setModelCode(piece.getModelCode());
        row.setProductionDate(piece.getProductionDate());
        row.setExpiryDate(piece.getExpiryDate());
        row.setInspectionStatus(INSPECTION_STATUS_COMPLETED);
        row.setInspectionResult(piece.getInspectionResult());
        row.setCoaInspectionStatus(INSPECTION_STATUS_COMPLETED);
        row.setCoaInspectionResult(piece.getCoaInspectionResult());
        row.setPackagingQualityStatus(resolveManualPackagingQuality(piece));
        row.setInspectionTime(piece.getRecorderTime());
        row.setRecorderName(piece.getRecorderName());
        row.setRecorderTime(piece.getRecorderTime());
        row.setPrintStatus(piece.getPrintStatus());
        row.setPrintCount(intValue(piece.getPrintCount()));
        row.setLastPrintTime(piece.getLastPrintTime());
        row.setStockStatus("WAIT_INBOUND");
        row.setInspectionRemark(firstNotBlank(piece.getRemark(), "历史片补录"));
        return row;
    }

    private String resolveManualPackagingQuality(HcPackagingManualPieceDO piece) {
        return isInspectionNg(piece.getInspectionResult()) ? INSPECTION_RESULT_NG : INSPECTION_RESULT_OK;
    }

    private String resolveManualPackagingQuality(HcPackagingManualPieceDO piece, CoaInspectionMeta coaInspectionMeta) {
        if (piece == null || isInspectionNg(piece.getInspectionResult())) {
            return INSPECTION_RESULT_NG;
        }
        if (coaInspectionMeta != null) {
            return coaInspectionMeta.isReleased() ? INSPECTION_RESULT_OK : QUALITY_FROZEN;
        }
        return resolveManualPackagingQuality(piece);
    }

    private void assertNoClaimedPackagingCoaSamples(String sourceType, Collection<Long> sourceRecordIds) {
        if (sourceRecordIds == null || sourceRecordIds.isEmpty()) {
            return;
        }
        List<QmsPackagingCoaSampleClaimDO> claims = qmsPackagingCoaSampleClaimMapper
                .selectListBySource(sourceType, sourceRecordIds);
        if (!claims.isEmpty()) {
            throw invalidParamException("所选片号已作为 COA 样片送检，不可继续包装；请刷新后选择其它片号");
        }
    }

    /** COA 样片是实物消耗，已占用后不能继续出现在任一待包装列表中。 */
    private List<InspectionSliceRespVO> filterClaimedPackagingCoaSamples(List<InspectionSliceRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return rows;
        }
        Set<Long> cutRoundIds = rows.stream()
                .filter(row -> SOURCE_CUT_ROUND_REPORT.equals(row.getSourceType()))
                .map(InspectionSliceRespVO::getSourceCutRoundReportId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> manualIds = rows.stream()
                .filter(row -> SOURCE_MANUAL_HISTORY.equals(row.getSourceType()))
                .map(InspectionSliceRespVO::getSourceManualPieceId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> claimedCutRoundIds = qmsPackagingCoaSampleClaimMapper
                .selectListBySource(SOURCE_CUT_ROUND_REPORT, cutRoundIds).stream()
                .map(QmsPackagingCoaSampleClaimDO::getSourceRecordId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> claimedManualIds = qmsPackagingCoaSampleClaimMapper
                .selectListBySource(SOURCE_MANUAL_HISTORY, manualIds).stream()
                .map(QmsPackagingCoaSampleClaimDO::getSourceRecordId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        return rows.stream()
                .filter(row -> !SOURCE_CUT_ROUND_REPORT.equals(row.getSourceType())
                        || !claimedCutRoundIds.contains(row.getSourceCutRoundReportId()))
                .filter(row -> !SOURCE_MANUAL_HISTORY.equals(row.getSourceType())
                        || !claimedManualIds.contains(row.getSourceManualPieceId()))
                .toList();
    }

    private boolean isBlankManualPieceImportRow(HcPackagingManualPieceImportExcelVO row) {
        return row == null || StrUtil.isAllBlank(row.getSliceBatchNo(), row.getSegmentBatchNo(), row.getModelCode(),
                row.getMaterialCode(),
                row.getProductionDate(), row.getExpiryDate(), row.getInspectionResult(),
                row.getCoaInspectionResult(), row.getBackfillReason(), row.getRemark());
    }

    private LocalDate parseManualPieceImportDate(String value, int rowNo, String fieldName, boolean required,
                                                 PackagingManualPieceImportRespVO respVO) {
        String text = StrUtil.trimToNull(value);
        if (text == null) {
            if (required) {
                addManualPieceImportFailure(respVO, String.format("第%d行：%s不能为空", rowNo, fieldName));
            }
            return null;
        }
        try {
            LocalDate date;
            if (text.matches("\\d+(?:\\.\\d+)?") && !text.matches("\\d{4}-?\\d{2}-?\\d{2}")) {
                date = DateUtil.getJavaDate(Double.parseDouble(text)).toInstant()
                        .atZone(BUSINESS_ZONE).toLocalDate();
            } else {
                String normalized = text.replace('/', '-').replace('.', '-');
                date = LocalDate.parse(normalized, DateTimeFormatter.ofPattern("yyyy-M-d"));
            }
            if (date.getYear() < 2000) {
                throw new DateTimeParseException("历史占位日期", text, 0);
            }
            return date;
        } catch (RuntimeException ex) {
            addManualPieceImportFailure(respVO,
                    String.format("第%d行：%s格式不正确，应为 yyyy-MM-dd 且年份不早于2000", rowNo, fieldName));
            return null;
        }
    }

    private void validatePackagingManualPieceImportReq(PackagingManualPieceCreateReqVO reqVO) {
        validateManualPieceImportText(reqVO.getSliceBatchNo(), "片号", 100, true);
        validateManualPieceImportText(reqVO.getSegmentBatchNo(), "分段批号", 100, true);
        validateManualPieceImportText(reqVO.getModelCode(), "产品型号", 64, true);
        validateManualPieceImportText(reqVO.getMaterialCode(), "产品料号", 64, true);
        validateManualPieceImportText(reqVO.getInspectionResult(), "裁切FQC结果", 16, true);
        validateManualPieceImportText(reqVO.getCoaInspectionResult(), "COA送检结果", 16, true);
        validateManualPieceImportText(reqVO.getBackfillReason(), "补录原因", 500, true);
        validateManualPieceImportText(reqVO.getRemark(), "备注", 500, false);
    }

    private void validateManualPieceImportText(String value, String fieldName, int maxLength, boolean required) {
        String text = StrUtil.trimToNull(value);
        if (required && text == null) {
            throw invalidParamException(fieldName + "不能为空");
        }
        if (text != null && text.length() > maxLength) {
            throw invalidParamException(fieldName + "不能超过" + maxLength + "个字符");
        }
    }

    private void addManualPieceImportFailure(PackagingManualPieceImportRespVO respVO, String message) {
        respVO.getFailures().add(message);
        respVO.setFailureCount(respVO.getFailures().size());
    }

    private String normalizeUpperText(String value) {
        return StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeManualInspectionResult(String value, String fieldName) {
        String result = StrUtil.trimToEmpty(value).toUpperCase(Locale.ROOT);
        if (!INSPECTION_RESULT_OK.equals(result) && !INSPECTION_RESULT_NG.equals(result)) {
            throw invalidParamException(fieldName + "结果仅支持OK或NG");
        }
        return result;
    }

    private LocalDate resolvePackagingItemProductionDate(HcInnerPackUnitItemDO item) {
        if (item.getSourceManualPieceId() != null) {
            HcPackagingManualPieceDO piece = hcPackagingManualPieceMapper.selectById(item.getSourceManualPieceId());
            return piece == null ? null : piece.getProductionDate();
        }
        if (item.getSourceCutRoundReportId() == null) {
            return null;
        }
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectById(item.getSourceCutRoundReportId());
        if (report == null) {
            return null;
        }
        return report.getConfirmerTime() == null ? report.getReportDate() : report.getConfirmerTime().toLocalDate();
    }

    private LocalDate resolvePackagingItemExpiryDate(HcInnerPackUnitItemDO item, LocalDate productionDate) {
        if (item.getSourceManualPieceId() != null) {
            HcPackagingManualPieceDO piece = hcPackagingManualPieceMapper.selectById(item.getSourceManualPieceId());
            if (piece != null && piece.getExpiryDate() != null) {
                return piece.getExpiryDate();
            }
        }
        return productionDate == null ? null : productionDate.plusMonths(10).minusDays(1);
    }

    private PieceLabelRespVO buildCutRoundPieceLabel(HcCutRoundReportDO report) {
        InspectionSliceRespVO inspection = new InspectionSliceRespVO();
        inspection.setParentProductionBatchNo(report.getParentProductionBatchNo());
        inspection.setProductionBatchNo(report.getProductionBatchNo());
        inspection.setSliceBatchNo(report.getProductionBatchNo());
        inspection.setInspectionResult(report.getInspectionResult());
        fillCoaInspectionInfo(List.of(inspection));
        LocalDate productionDate = report.getConfirmerTime() == null
                ? report.getReportDate() : report.getConfirmerTime().toLocalDate();
        PiecePrintMeta printMeta = resolveCutRoundPiecePrintMeta(report);
        HcFinishedStockDO stock = hcFinishedStockMapper.selectBySliceBatchNo(report.getProductionBatchNo());
        HcInnerPackUnitItemDO packItem = hcInnerPackUnitItemMapper.selectByCutRoundReportId(report.getId());
        PieceLabelRespVO label = new PieceLabelRespVO();
        label.setSourceType(SOURCE_CUT_ROUND_REPORT);
        label.setSourceId(report.getId());
        label.setSliceBatchNo(report.getProductionBatchNo());
        label.setSegmentBatchNo(resolvePackageSegmentBatchNo(report.getParentProductionBatchNo(),
                report.getSourceBatchNo(), report.getProductionBatchNo()));
        label.setPlanNo(report.getPlanNo());
        label.setMaterialCode(report.getMaterialCode());
        label.setMaterialName(report.getMaterialName());
        label.setModelCode(report.getModelCode());
        label.setProductionDate(productionDate);
        label.setExpiryDate(productionDate == null ? null : productionDate.plusMonths(10).minusDays(1));
        label.setInspectionResult(report.getInspectionResult());
        label.setCoaInspectionResult(inspection.getCoaInspectionResult());
        label.setQualityStatus(resolvePieceLabelQualityStatus(stock, packItem,
                inspection.getPackagingQualityStatus()));
        label.setRecorderName(firstNotBlank(report.getRecorderName(), report.getConfirmerName()));
        label.setWorkTime(firstNotNull(report.getEndTime(), report.getConfirmerTime(), report.getRecorderTime()));
        label.setPrintStatus(printMeta.printCount() > 0 ? "PRINTED" : "UNPRINTED");
        label.setPrintCount(printMeta.printCount());
        label.setLastPrintTime(printMeta.lastPrintTime());
        label.setRecordStatus(stock != null ? stock.getStockStatus()
                : packItem != null ? STATUS_PACKED : report.getReportStatus());
        label.setHistorical(false);
        return label;
    }

    private PieceLabelRespVO buildManualPieceLabel(HcPackagingManualPieceDO piece) {
        InspectionSliceRespVO inspection = buildManualInspectionSlice(piece);
        fillCoaInspectionInfo(List.of(inspection));
        HcFinishedStockDO stock = hcFinishedStockMapper.selectBySliceBatchNo(piece.getSliceBatchNo());
        HcInnerPackUnitItemDO packItem = hcInnerPackUnitItemMapper.selectByManualPieceId(piece.getId());
        PieceLabelRespVO label = new PieceLabelRespVO();
        label.setSourceType(SOURCE_MANUAL_HISTORY);
        label.setSourceId(piece.getId());
        label.setSliceBatchNo(piece.getSliceBatchNo());
        label.setSegmentBatchNo(piece.getSegmentBatchNo());
        label.setMaterialCode(piece.getMaterialCode());
        label.setMaterialName(piece.getMaterialName());
        label.setModelCode(piece.getModelCode());
        label.setProductionDate(piece.getProductionDate());
        label.setExpiryDate(piece.getExpiryDate());
        label.setInspectionResult(piece.getInspectionResult());
        label.setCoaInspectionResult(inspection.getCoaInspectionResult());
        label.setQualityStatus(resolvePieceLabelQualityStatus(stock, packItem, inspection.getPackagingQualityStatus()));
        label.setRecorderName(piece.getRecorderName());
        label.setWorkTime(piece.getRecorderTime());
        label.setPrintStatus(intValue(piece.getPrintCount()) > 0 ? "PRINTED" : "UNPRINTED");
        label.setPrintCount(intValue(piece.getPrintCount()));
        label.setLastPrintTime(piece.getLastPrintTime());
        label.setRecordStatus(stock == null ? piece.getRecordStatus() : stock.getStockStatus());
        label.setHistorical(true);
        return label;
    }

    /**
     * 标签表头必须随片号当前业务阶段的最终质量状态变化：库存优先、待上架包装次之、待包装按检验结论兜底。
     */
    private String resolvePieceLabelQualityStatus(HcFinishedStockDO stock,
                                                  HcInnerPackUnitItemDO packItem,
                                                  String sourceQualityStatus) {
        String qualityStatus = stock != null && StrUtil.isNotBlank(stock.getQualityStatus())
                ? stock.getQualityStatus()
                : packItem != null && StrUtil.isNotBlank(packItem.getQualityStatus())
                ? packItem.getQualityStatus() : sourceQualityStatus;
        String normalized = StrUtil.trimToEmpty(qualityStatus).toUpperCase(Locale.ROOT);
        if (INSPECTION_RESULT_OK.equals(normalized) || INSPECTION_RESULT_NG.equals(normalized) || QUALITY_FROZEN.equals(normalized)) {
            return normalized;
        }
        return null;
    }

    private PiecePrintMeta resolveCutRoundPiecePrintMeta(HcCutRoundReportDO report) {
        Map<String, Object> extra = parseExtraMap(report == null ? null : report.getExtraJson());
        int printCount = parseInteger(extra.get("printCount"));
        LocalDateTime lastPrintTime = parseBusinessDateTime(extra.get("printTime"));
        return new PiecePrintMeta(printCount, lastPrintTime);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseExtraMap(String extraJson) {
        if (StrUtil.isBlank(extraJson)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = JsonUtils.parseObject(extraJson, Map.class);
            return parsed == null ? new LinkedHashMap<>() : new LinkedHashMap<>(parsed);
        } catch (Exception ignored) {
            return new LinkedHashMap<>();
        }
    }

    private int parseInteger(Object value) {
        if (value instanceof Number number) {
            return Math.max(number.intValue(), 0);
        }
        try {
            return Math.max(Integer.parseInt(String.valueOf(value)), 0);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private LocalDateTime parseBusinessDateTime(Object value) {
        String text = StrUtil.trimToEmpty(String.valueOf(value == null ? "" : value));
        if (StrUtil.isBlank(text)) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, BUSINESS_DATETIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(text);
            } catch (DateTimeParseException ignoredAgain) {
                return null;
            }
        }
    }

    @SafeVarargs
    private final <T> T firstNotNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record PiecePrintMeta(int printCount, LocalDateTime lastPrintTime) {
    }

    private List<InspectionSliceRespVO> filterInspectionSlicesByPackagingQualityStatus(List<InspectionSliceRespVO> rows,
                                                                                      String packagingQualityStatus) {
        String status = StrUtil.trimToEmpty(packagingQualityStatus).toUpperCase(Locale.ROOT);
        if (!INSPECTION_RESULT_OK.equals(status) && !INSPECTION_RESULT_NG.equals(status) && !QUALITY_FROZEN.equals(status)) {
            return rows;
        }
        return rows.stream()
                .filter(row -> status.equals(row.getPackagingQualityStatus()))
                .toList();
    }

    private InspectionSliceSegmentRespVO buildInboundWaitSegmentResp(String segmentBatchNo,
                                                                     List<InspectionSliceRespVO> rows) {
        InspectionSliceRespVO first = rows.get(0);
        InspectionSliceSegmentRespVO resp = new InspectionSliceSegmentRespVO();
        resp.setSegmentBatchNo(segmentBatchNo);
        resp.setPlanId(first.getPlanId());
        resp.setPlanNo(first.getPlanNo());
        resp.setPlanOperationId(first.getPlanOperationId());
        resp.setMaterialCode(first.getMaterialCode());
        resp.setMaterialName(first.getMaterialName());
        resp.setModelCode(first.getModelCode());
        resp.setInspectionStatus(INSPECTION_STATUS_COMPLETED);
        resp.setStockStatus("WAIT_INBOUND");
        resp.setTotalPieceCount(rows.size());
        int ngCount = 0;
        LocalDate productionDateStart = null;
        LocalDate productionDateEnd = null;
        LocalDate expiryDate = null;
        List<String> sampleSliceBatchNos = new ArrayList<>();
        for (InspectionSliceRespVO row : rows) {
            if (INSPECTION_RESULT_NG.equals(row.getPackagingQualityStatus())) {
                ngCount++;
            }
            LocalDate productionDate = row.getProductionDate();
            if (productionDate != null) {
                if (productionDateStart == null || productionDate.isBefore(productionDateStart)) {
                    productionDateStart = productionDate;
                }
                if (productionDateEnd == null || productionDate.isAfter(productionDateEnd)) {
                    productionDateEnd = productionDate;
                }
            }
            if (expiryDate == null && row.getExpiryDate() != null) {
                expiryDate = row.getExpiryDate();
            }
            String sliceBatchNo = StrUtil.blankToDefault(row.getProductionBatchNo(), row.getSliceBatchNo());
            if (StrUtil.isNotBlank(sliceBatchNo) && sampleSliceBatchNos.size() < 3
                    && !sampleSliceBatchNos.contains(sliceBatchNo)) {
                sampleSliceBatchNos.add(sliceBatchNo);
            }
        }
        resp.setProductionDateStart(productionDateStart);
        resp.setProductionDateEnd(productionDateEnd);
        resp.setExpiryDate(expiryDate);
        resp.setNgPieceCount(ngCount);
        resp.setOkPieceCount((int) rows.stream().filter(row -> INSPECTION_RESULT_OK.equals(row.getPackagingQualityStatus())).count());
        resp.setPackagingQualityStatus(ngCount > 0 ? INSPECTION_RESULT_NG : rows.stream().anyMatch(row -> QUALITY_FROZEN.equals(row.getPackagingQualityStatus())) ? QUALITY_FROZEN : INSPECTION_RESULT_OK);
        resp.setSampleSliceBatchNos(sampleSliceBatchNos);
        resp.setSampleSliceBatchNo(sampleSliceBatchNos.isEmpty() ? null : sampleSliceBatchNos.get(0));
        return resp;
    }

    private void fillCoaInspectionInfo(List<InspectionSliceRespVO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, CoaInspectionMeta> coaInspectionMap = buildCoaInspectionMapForInspectionSlices(rows);
        for (InspectionSliceRespVO row : rows) {
            String scopeBatchNo = resolveCoaScopeBatchNo(row.getParentProductionBatchNo(), row.getProductionBatchNo());
            row.setCoaScopeBatchNo(scopeBatchNo);
            row.setSegmentBatchNo(resolvePackageSegmentBatchNo(row.getParentProductionBatchNo(), scopeBatchNo,
                    row.getProductionBatchNo(), row.getSliceBatchNo()));
            CoaInspectionMeta meta = coaInspectionMap.get(scopeBatchNo);
            if (meta == null) {
                row.setCoaInspectionResult(COA_RESULT_UNKNOWN);
                row.setCoaInspectionStatus(null);
            } else {
                row.setCoaInspectionResult(meta.result);
                row.setCoaInspectionStatus(meta.status);
                row.setCoaInspectionNo(meta.faiNo);
                row.setCoaSampleBatchNo(meta.sampleBatchNo);
                row.setCoaNgReason(meta.ngReason);
            }
            row.setPackagingQualityStatus(resolveInspectionSlicePackagingQualityStatus(row));
        }
    }

    private String resolveInspectionSlicePackagingQualityStatus(InspectionSliceRespVO row) {
        if (row == null) {
            return INSPECTION_RESULT_OK;
        }
        if (isInspectionNg(row.getInspectionResult())) {
            return INSPECTION_RESULT_NG;
        }
        return !hasCoaInspection(row) || isCoaInspectionReleased(row.getCoaInspectionStatus(), row.getCoaInspectionResult())
                ? INSPECTION_RESULT_OK : QUALITY_FROZEN;
    }

    private void fillPackagingSourceQualityStatus(List<HcPackagingSourceRespVO> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }
        Map<String, CoaInspectionMeta> coaInspectionMap = buildCoaInspectionMapForPackagingSources(sources);
        for (HcPackagingSourceRespVO source : sources) {
            source.setQualityStatus(resolvePackagingSourceQualityStatus(source, coaInspectionMap));
        }
    }

    private String resolvePackagingPieceQualityStatus(HcCutRoundReportDO report,
                                                       Map<String, CoaInspectionMeta> coaInspectionMap) {
        if (report == null) {
            return INSPECTION_RESULT_OK;
        }
        if (isInspectionNg(report.getInspectionResult())) {
            return INSPECTION_RESULT_NG;
        }
        String scopeBatchNo = resolveCoaScopeBatchNo(report);
        CoaInspectionMeta meta = coaInspectionMap == null ? null : coaInspectionMap.get(scopeBatchNo);
        return meta == null || meta.isReleased() ? INSPECTION_RESULT_OK : QUALITY_FROZEN;
    }

    private void validateSameInboundPackageSegment(List<HcCutRoundReportDO> reports) {
        Map<String, List<String>> segmentSliceMap = new LinkedHashMap<>();
        for (HcCutRoundReportDO report : reports) {
            String segmentNo = resolveCoaScopeBatchNo(report);
            String sliceBatchNo = firstNotBlank(report.getProductionBatchNo(), String.valueOf(report.getId()));
            segmentSliceMap.computeIfAbsent(segmentNo, key -> new ArrayList<>()).add(sliceBatchNo);
        }
        if (segmentSliceMap.size() <= 1) {
            return;
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : segmentSliceMap.entrySet()) {
            if (parts.size() >= 5) {
                break;
            }
            List<String> samples = entry.getValue().stream().limit(3).toList();
            String suffix = entry.getValue().size() > samples.size() ? "等" + entry.getValue().size() + "片" : "";
            parts.add(firstNotBlank(entry.getKey(), "-") + "：" + String.join("、", samples) + suffix);
        }
        throw invalidParamException("不同段号的片号不能包装在一起，请按段号分开包装。当前选择：" + String.join("；", parts));
    }

    private void validateSameInboundPackageQuality(List<HcCutRoundReportDO> reports,
                                                   Map<String, CoaInspectionMeta> coaInspectionMap) {
        Map<String, List<String>> qualitySliceMap = new LinkedHashMap<>();
        for (HcCutRoundReportDO report : reports) {
            String qualityStatus = resolvePackagingPieceQualityStatus(report, coaInspectionMap);
            String sliceBatchNo = firstNotBlank(report.getProductionBatchNo(), String.valueOf(report.getId()));
            qualitySliceMap.computeIfAbsent(qualityStatus, key -> new ArrayList<>()).add(sliceBatchNo);
        }
        if (qualitySliceMap.size() <= 1) {
            return;
        }
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : qualitySliceMap.entrySet()) {
            List<String> samples = entry.getValue().stream().limit(3).toList();
            String suffix = entry.getValue().size() > samples.size() ? "等" + entry.getValue().size() + "片" : "";
            parts.add(entry.getKey() + "：" + String.join("、", samples) + suffix);
        }
        throw invalidParamException("合格片、冻结片与不合格片不能混装，请分别在对应待包装段完成包装。当前选择：" + String.join("；", parts));
    }

    private void validateInboundPackagePieceCount(int pieceCount, String packageQualityStatus) {
        if (pieceCount <= 0) {
            throw invalidParamException("请选择待包装片");
        }
    }

    private String resolvePackagingSourceQualityStatus(HcPackagingSourceRespVO source,
                                                        Map<String, CoaInspectionMeta> coaInspectionMap) {
        if (source == null) {
            return INSPECTION_RESULT_OK;
        }
        if (isInspectionNg(source.getQualityStatus())) {
            return INSPECTION_RESULT_NG;
        }
        String scopeBatchNo = resolveCoaScopeBatchNo(source.getParentProductionBatchNo(), source.getProductionBatchNo());
        CoaInspectionMeta meta = coaInspectionMap == null ? null : coaInspectionMap.get(scopeBatchNo);
        return meta == null || meta.isReleased() ? INSPECTION_RESULT_OK : QUALITY_FROZEN;
    }

    private boolean hasCoaInspection(InspectionSliceRespVO row) {
        return row != null && (StrUtil.isNotBlank(row.getCoaInspectionStatus())
                || (StrUtil.isNotBlank(row.getCoaInspectionResult())
                && !COA_RESULT_UNKNOWN.equalsIgnoreCase(row.getCoaInspectionResult())));
    }

    private boolean isCoaInspectionReleased(String status, String result) {
        return FAI_STATUS_COMPLETED.equalsIgnoreCase(StrUtil.trimToEmpty(status))
                && INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.trimToEmpty(result));
    }

    private Map<String, CoaInspectionMeta> buildCoaInspectionMapForInspectionSlices(List<InspectionSliceRespVO> rows) {
        Set<String> segmentBatchNos = new HashSet<>();
        for (InspectionSliceRespVO row : rows) {
            String scopeBatchNo = resolveCoaScopeBatchNo(row.getParentProductionBatchNo(), row.getProductionBatchNo());
            if (StrUtil.isNotBlank(scopeBatchNo)) {
                segmentBatchNos.add(scopeBatchNo);
            }
        }
        return buildCoaInspectionMap(segmentBatchNos);
    }

    private Map<String, CoaInspectionMeta> buildCoaInspectionMapForCutRoundReports(List<HcCutRoundReportDO> reports) {
        Set<String> segmentBatchNos = new HashSet<>();
        for (HcCutRoundReportDO report : reports) {
            String scopeBatchNo = resolveCoaScopeBatchNo(report);
            if (StrUtil.isNotBlank(scopeBatchNo)) {
                segmentBatchNos.add(scopeBatchNo);
            }
        }
        return buildCoaInspectionMap(segmentBatchNos);
    }

    private Map<String, CoaInspectionMeta> buildCoaInspectionMapForPackagingSources(List<HcPackagingSourceRespVO> sources) {
        Set<String> segmentBatchNos = new HashSet<>();
        for (HcPackagingSourceRespVO source : sources) {
            String scopeBatchNo = resolveCoaScopeBatchNo(source.getParentProductionBatchNo(), source.getProductionBatchNo());
            if (StrUtil.isNotBlank(scopeBatchNo)) {
                segmentBatchNos.add(scopeBatchNo);
            }
        }
        return buildCoaInspectionMap(segmentBatchNos);
    }

    private Map<String, CoaInspectionMeta> buildCoaInspectionMap(Set<String> segmentBatchNos) {
        return buildCoaInspectionMap(segmentBatchNos, false);
    }

    private Map<String, CoaInspectionMeta> buildCoaInspectionMap(Set<String> segmentBatchNos, boolean lockForShipping) {
        if (segmentBatchNos == null || segmentBatchNos.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, CoaInspectionMeta> result = new LinkedHashMap<>();
        List<QmsFaiOrderDO> faiOrders = new ArrayList<>(qmsFaiOrderMapper
                .selectCoaListBySegmentBatchNos(SOURCE_MENU_CODE_ADHESIVE2, segmentBatchNos));
        faiOrders.addAll(qmsFaiOrderMapper.selectPackagingCoaListBySegmentBatchNos(segmentBatchNos));
        // 已删除的 COA 单仅保留历史追溯，不作为当前冻结依据。
        faiOrders.removeIf(row -> Boolean.TRUE.equals(row.getDeleted()));
        if (lockForShipping) {
            faiOrders = faiOrders.stream().sorted(Comparator.comparing(QmsFaiOrderDO::getId))
                    .map(row -> qmsFaiOrderMapper.selectCoaOrderForUpdate(row.getId()))
                    .filter(Objects::nonNull).filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        }
        faiOrders.sort(Comparator.comparing(QmsFaiOrderDO::getSubmissionTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(QmsFaiOrderDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        for (QmsFaiOrderDO faiOrder : selectEffectiveCoaFaiOrders(faiOrders)) {
            String scopeBatchNo = normalizeCoaSegmentBatchNo(faiOrder.getProductBatchNo());
            if (StrUtil.isBlank(scopeBatchNo) || !segmentBatchNos.contains(scopeBatchNo)) {
                continue;
            }
            result.computeIfAbsent(scopeBatchNo, key -> new CoaInspectionMeta()).accept(faiOrder);
        }
        return result;
    }

    /**
     * COA 复检单与原送检单共用同一复检链。包装质量应只取该链最新轮次的结论，
     * 否则历史 NG 会覆盖已完成且 OK 的复检结论。
     */
    private List<QmsFaiOrderDO> selectEffectiveCoaFaiOrders(List<QmsFaiOrderDO> faiOrders) {
        if (faiOrders == null || faiOrders.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, QmsFaiOrderDO> latestOrderByRecheckGroup = new LinkedHashMap<>();
        for (QmsFaiOrderDO faiOrder : faiOrders) {
            Long recheckGroupId = resolveRecheckGroupId(faiOrder);
            if (recheckGroupId == null) {
                continue;
            }
            QmsFaiOrderDO latestOrder = latestOrderByRecheckGroup.get(recheckGroupId);
            if (latestOrder == null || resolveRecheckRoundNo(faiOrder) > resolveRecheckRoundNo(latestOrder)) {
                latestOrderByRecheckGroup.put(recheckGroupId, faiOrder);
            }
        }
        if (latestOrderByRecheckGroup.isEmpty()) {
            return faiOrders;
        }
        List<QmsFaiOrderDO> effectiveOrders = new ArrayList<>();
        for (QmsFaiOrderDO faiOrder : faiOrders) {
            Long recheckGroupId = resolveRecheckGroupId(faiOrder);
            QmsFaiOrderDO latestOrder = recheckGroupId == null ? null : latestOrderByRecheckGroup.get(recheckGroupId);
            if (latestOrder == null || latestOrder == faiOrder
                    || (latestOrder.getId() != null && Objects.equals(latestOrder.getId(), faiOrder.getId()))) {
                effectiveOrders.add(faiOrder);
            }
        }
        return effectiveOrders;
    }

    private static Long resolveRecheckGroupId(QmsFaiOrderDO faiOrder) {
        if (faiOrder == null || faiOrder.getRecheckGroupId() == null || faiOrder.getRecheckGroupId() <= 0) {
            return null;
        }
        return faiOrder.getRecheckGroupId();
    }

    private static int resolveRecheckRoundNo(QmsFaiOrderDO faiOrder) {
        return faiOrder == null || faiOrder.getRecheckRoundNo() == null ? 0 : faiOrder.getRecheckRoundNo();
    }

    private String resolveCoaScopeBatchNo(HcCutRoundReportDO report) {
        if (report == null) {
            return "";
        }
        return chooseCoaScopeBatchNo(report.getParentProductionBatchNo(),
                report.getSourceProductionBatchNo(),
                report.getProductionBatchNo());
    }

    private String resolveCoaScopeBatchNo(String parentProductionBatchNo, String productionBatchNo) {
        return chooseCoaScopeBatchNo(parentProductionBatchNo, productionBatchNo);
    }

    private String resolvePackageSegmentBatchNo(String... batchNos) {
        return chooseCoaScopeBatchNo(batchNos);
    }

    private String chooseCoaScopeBatchNo(String... batchNos) {
        String fallback = "";
        if (batchNos == null) {
            return fallback;
        }
        for (String batchNo : batchNos) {
            String normalized = normalizeCoaSegmentBatchNo(batchNo);
            if (StrUtil.isBlank(normalized)) {
                continue;
            }
            if (StrUtil.isBlank(fallback)) {
                fallback = normalized;
            }
            if (isCoaSegmentBatchNo(normalized)) {
                return normalized;
            }
        }
        return fallback;
    }

    private boolean isCoaSegmentBatchNo(String batchNo) {
        return StrUtil.isNotBlank(batchNo) && batchNo.matches("^.+[PQRS]$");
    }

    private String normalizeCoaSegmentBatchNo(String batchNo) {
        String value = StrUtil.trimToEmpty(batchNo).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(value)) {
            return "";
        }
        value = value.replaceFirst("-J\\d+$", "");
        value = value.replaceFirst("-S\\d+$", "");
        value = value.replaceFirst("^(.+[PQRS])\\d{3}[A-Z]?$", "$1");
        return value;
    }

    private static String classifyCoaInspectionResult(QmsFaiOrderDO faiOrder) {
        if (faiOrder == null) {
            return COA_RESULT_UNKNOWN;
        }
        if (Boolean.TRUE.equals(faiOrder.getDeleted())) return COA_RESULT_PENDING;
        String status = StrUtil.trimToEmpty(faiOrder.getStatus()).toUpperCase(Locale.ROOT);
        String judgment = StrUtil.trimToEmpty(faiOrder.getJudgment()).toUpperCase(Locale.ROOT);
        if (INSPECTION_RESULT_NG.equals(judgment) || FAI_STATUS_REJECTED.equals(status)) {
            return INSPECTION_RESULT_NG;
        }
        if (FAI_STATUS_COMPLETED.equals(status) && INSPECTION_RESULT_OK.equals(judgment)) {
            return INSPECTION_RESULT_OK;
        }
        if (FAI_STATUS_CANCELED.equals(status)) {
            return COA_RESULT_PENDING;
        }
        if (FAI_STATUS_PENDING.equals(status)
                || FAI_STATUS_INSPECTING.equals(status)
                || FAI_STATUS_WAITING_QA.equals(status)
                || FAI_STATUS_SUSPENDED.equals(status)
                || FAI_STATUS_REWORKING.equals(status)
                || FAI_JUDGMENT_PENDING.equals(judgment)) {
            return COA_RESULT_PENDING;
        }
        return COA_RESULT_PENDING;
    }

    private static boolean isCoaInspectionNg(QmsFaiOrderDO faiOrder) {
        return INSPECTION_RESULT_NG.equals(classifyCoaInspectionResult(faiOrder));
    }

    private String normalizeInspectionResult(String result) {
        String normalized = StrUtil.trimToEmpty(result).toUpperCase();
        if (!List.of(INSPECTION_RESULT_OK, INSPECTION_RESULT_NG).contains(normalized)) {
            throw invalidParamException("检验结果只允许OK或NG");
        }
        return normalized;
    }

    private boolean isInspectionOk(String result) {
        return INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.trimToEmpty(result));
    }

    private boolean isInspectionNg(String result) {
        return INSPECTION_RESULT_NG.equalsIgnoreCase(StrUtil.trimToEmpty(result));
    }

    private boolean isShippingFqcOk(HcFgShippingNoticeItemDO item) {
        if (item == null) {
            return false;
        }
        QmsFqcShippingDetailDO detail = qmsFqcShippingDetailMapper.selectLatestByShippingNoticeItemId(item.getId());
        return isShippingFqcAlignedAndAudited(detail);
    }

    private boolean isShippingFqcOk(HcFgShippingNoticePickItemDO item) {
        if (item == null) {
            return false;
        }
        QmsFqcShippingDetailDO detail = qmsFqcShippingDetailMapper.selectLatestByShippingPickItemId(item.getId());
        return isShippingFqcAlignedAndAudited(detail);
    }

    private boolean isShippingFqcAlignedAndAudited(QmsFqcShippingDetailDO detail) {
        if (detail == null || !isInspectionOk(detail.getRowJudgment())) {
            return false;
        }
        QmsFqcOrderDO order = detail.getFqcId() == null ? null : qmsFqcOrderMapper.selectById(detail.getFqcId());
        if (order == null || !"COMPLETED".equals(order.getStatus())) {
            return false;
        }
        HcFgShippingNoticeDO notice = detail.getShippingNoticeId() == null ? null
                : hcFgShippingNoticeMapper.selectById(detail.getShippingNoticeId());
        if (notice != null && PRODUCT_TYPE_SAMPLE.equals(notice.getProductType())) {
            HcFgShippingNoticePickItemDO pick = detail.getShippingPickItemId() == null ? null
                    : hcFgShippingNoticePickItemMapper.selectById(detail.getShippingPickItemId());
            return pick != null && !Boolean.TRUE.equals(pick.getDeleted())
                    && HcFgShippingNoticePickItemMapper.ACTIVE_STATUSES.contains(pick.getLockStatus())
                    && Objects.equals(pick.getNoticeId(), notice.getId())
                    && detail.getShippingNoticeItemId() != null
                    && Objects.equals(pick.getSourceNoticeItemId(), detail.getShippingNoticeItemId())
                    && StrUtil.isNotBlank(detail.getActualSliceBatchNo())
                    && Objects.equals(pick.getActualSliceBatchNo(), detail.getActualSliceBatchNo());
        }
        return "ALIGNED".equals(detail.getAlignmentStatus());
    }

    private boolean isShippingFqcNg(HcFgShippingNoticePickItemDO item) {
        if (item == null) {
            return false;
        }
        QmsFqcShippingDetailDO detail = qmsFqcShippingDetailMapper.selectLatestByShippingPickItemId(item.getId());
        return detail != null && isInspectionNg(detail.getRowJudgment());
    }

    private boolean hasShippingOuterPackageTrace(HcFgShippingNoticeItemDO item) {
        return item != null && (item.getShippingPackageTime() != null
                || StrUtil.isNotBlank(item.getShippingPackageName()));
    }

    private boolean hasShippingNoticeSaveItemValue(ShippingNoticeSaveItemReqVO item) {
        return item.getId() != null || StrUtil.isNotBlank(item.getInternalModelCode())
                || StrUtil.isNotBlank(item.getInternalItemCode())
                || StrUtil.isNotBlank(item.getCustomerModelCode())
                || StrUtil.isNotBlank(item.getCustomerSliceBatchNo())
                || StrUtil.isNotBlank(item.getCustomerProductBatchNo())
                || StrUtil.isNotBlank(item.getPackageSliceNo())
                || StrUtil.isNotBlank(item.getSliceBatchNo())
                || StrUtil.isNotBlank(item.getBatchNo())
                || StrUtil.isNotBlank(item.getMaterialCode())
                || StrUtil.isNotBlank(item.getMaterialName())
                || StrUtil.isNotBlank(item.getModelCode())
                || StrUtil.isNotBlank(item.getRemark());
    }

    private List<HcFgShippingNoticeItemDO> selectShippingExecutionItems(HcFgShippingNoticeDO notice) {
        return hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId()).stream()
                .filter(item -> !PRODUCT_TYPE_SAMPLE.equals(notice.getProductType())
                        || !NOTICE_STATUS_CANCELLED.equals(item.getLockStatus())).toList();
    }

    /** 样品无需客户批号对齐；下达时准备逐片记录，配货后按实际库存关联。 */
    private List<HcFgShippingNoticeItemDO> prepareSampleExecutionItems(HcFgShippingNoticeDO notice,
                                                                     List<HcFgShippingNoticeItemDO> items) {
        if (!PRODUCT_TYPE_SAMPLE.equals(notice.getProductType())) {
            return items;
        }
        int quantity = positive(firstPositive(notice.getRequiredShipQty(), notice.getNoticeQty()), "样品数量必须大于0");
        List<HcFgShippingNoticeItemDO> activeItems = items.stream()
                .filter(item -> !NOTICE_STATUS_CANCELLED.equals(item.getLockStatus())).toList();
        int plannedQty = activeItems.stream().mapToInt(this::getNoticeItemRequiredQty).sum();
        if (!activeItems.isEmpty() && plannedQty != quantity) {
            throw invalidParamException("样品明细数量合计必须与样品数量一致");
        }
        List<HcFgShippingNoticeItemDO> result = new ArrayList<>();
        if (activeItems.isEmpty()) {
            for (int i = 0; i < quantity; i++) {
                HcFgShippingNoticeItemDO item = HcFgShippingNoticeItemDO.builder()
                        .tenantId(notice.getTenantId()).noticeId(notice.getId()).noticeNo(notice.getNoticeNo())
                        .lockedQty(1).lockStatus(notice.getNoticeStatus()).build();
                hcFgShippingNoticeItemMapper.insert(item);
                result.add(item);
            }
            return result;
        }
        for (HcFgShippingNoticeItemDO item : activeItems) {
            int itemQty = getNoticeItemRequiredQty(item);
            if (itemQty > 1) {
                if (item.getActualFinishedStockId() != null || StrUtil.isNotBlank(item.getActualSliceBatchNo())) {
                    throw invalidParamException("历史样品明细已关联实物，不能自动拆分，请先退回配货后调整");
                }
                item.setLockedQty(1);
                hcFgShippingNoticeItemMapper.updateById(item);
            }
            result.add(item);
            for (int i = 1; i < itemQty; i++) {
                HcFgShippingNoticeItemDO copy = HcFgShippingNoticeItemDO.builder()
                        .tenantId(notice.getTenantId()).noticeId(notice.getId()).noticeNo(notice.getNoticeNo())
                        .internalModelCode(item.getInternalModelCode()).internalItemCode(item.getInternalItemCode())
                        .customerModelCode(item.getCustomerModelCode()).customerSliceBatchNo(item.getCustomerSliceBatchNo())
                        .materialCode(item.getMaterialCode()).materialName(item.getMaterialName())
                        .modelCode(item.getModelCode()).productSize(item.getProductSize())
                        .lockedQty(1).lockStatus(item.getLockStatus()).remark(item.getRemark()).build();
                hcFgShippingNoticeItemMapper.insert(copy);
                result.add(copy);
            }
        }
        return result;
    }

    private HcFgShippingNoticeItemDO buildShippingNoticeDraftItem(HcFgShippingNoticeDO notice,
                                                                  ShippingNoticeSaveItemReqVO itemReq,
                                                                  String materialCode,
                                                                  String materialName,
                                                                  String modelCode,
                                                                  String productSize,
                                                                  String requiredBatchNo) {
        String internalItemCode = StrUtil.trimToNull(firstNotBlank(itemReq.getInternalItemCode(), itemReq.getCustomerSliceBatchNo()));
        String internalModelCode = StrUtil.trimToNull(firstNotBlank(itemReq.getInternalModelCode(), itemReq.getCustomerModelCode(),
                PRODUCT_TYPE_SAMPLE.equals(notice.getProductType()) ? null : modelCode));
        String storedModelCode = limitText(firstNotBlank(extractLeadingCode(internalModelCode), internalModelCode), 64);
        String productBatchNo = StrUtil.trimToNull(firstNotBlank(itemReq.getCustomerProductBatchNo(), itemReq.getBatchNo(),
                PRODUCT_TYPE_SAMPLE.equals(notice.getProductType()) ? null : requiredBatchNo));
        int itemQty = itemReq.getShipQty() == null || itemReq.getShipQty() <= 0 ? 1 : itemReq.getShipQty();
        return HcFgShippingNoticeItemDO.builder()
                .tenantId(notice.getTenantId())
                .noticeId(notice.getId())
                .noticeNo(notice.getNoticeNo())
                .sliceBatchNo(StrUtil.trimToNull(itemReq.getPackageSliceNo()))
                .batchNo(productBatchNo)
                .internalModelCode(internalModelCode)
                .internalItemCode(internalItemCode)
                .customerProductBatchNo(productBatchNo)
                .packageSliceNo(StrUtil.trimToNull(firstNotBlank(itemReq.getPackageSliceNo(), itemReq.getSliceBatchNo())))
                .materialCode(StrUtil.trimToNull(firstNotBlank(itemReq.getMaterialCode(), materialCode)))
                .materialName(StrUtil.trimToNull(firstNotBlank(itemReq.getMaterialName(), materialName)))
                .modelCode(storedModelCode)
                .productSize(productSize)
                .lockedQty(itemQty)
                .customerSliceBatchNo(internalItemCode)
                .customerModelCode(internalModelCode)
                .lockStatus(NOTICE_STATUS_DRAFT)
                .remark(StrUtil.trimToNull(itemReq.getRemark()))
                .build();
    }

    private String normalizeProductType(String productType) {
        String normalized = StrUtil.trimToEmpty(productType).toUpperCase();
        if (!List.of(PRODUCT_TYPE_MASS, PRODUCT_TYPE_SAMPLE, PRODUCT_TYPE_RND).contains(normalized)) {
            throw invalidParamException("产品类型只允许量产或样品");
        }
        return normalized;
    }

    private void refreshInspectionTaskStatus(Long taskId) {
        List<HcCutRoundInspectionDetailDO> details = hcCutRoundInspectionDetailMapper.selectListByTaskId(taskId);
        if (details.isEmpty() || details.stream().anyMatch(item -> StrUtil.isBlank(item.getInspectionResult()))) {
            return;
        }
        HcCutRoundInspectionTaskDO update = new HcCutRoundInspectionTaskDO();
        update.setId(taskId);
        update.setTaskStatus(INSPECTION_STATUS_COMPLETED);
        hcCutRoundInspectionTaskMapper.updateById(update);
    }

    private InboundBoxRespVO buildInboundBoxResp(HcInnerPackUnitDO box) {
        return buildInboundBoxResp(box,
                hcFinishedStockMapper.selectListByInnerUnitNo(box.getInnerUnitNo()),
                hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId()));
    }

    /**
     * 批量构建包装单响应，分类列表和段内展开均复用同一批量读取链路。
     *
     * <p>历史已上架包装较多时，逐包读取库存和片明细会产生 2N 次 SQL；此处固定为
     * “包装主表 + 包装片明细 + 成品库存”三次读取，再在内存中按包装单归并。</p>
     */
    private List<InboundBoxRespVO> buildInboundBoxResps(List<HcInnerPackUnitDO> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> boxIds = boxes.stream()
                .map(HcInnerPackUnitDO::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<String> innerUnitNos = boxes.stream()
                .map(HcInnerPackUnitDO::getInnerUnitNo)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .toList();

        Map<Long, List<HcInnerPackUnitItemDO>> itemsByBoxId = new LinkedHashMap<>();
        for (HcInnerPackUnitItemDO item : hcInnerPackUnitItemMapper.selectListByInnerUnitIds(boxIds)) {
            if (item.getInnerUnitId() != null) {
                itemsByBoxId.computeIfAbsent(item.getInnerUnitId(), key -> new ArrayList<>()).add(item);
            }
        }
        Map<String, List<HcFinishedStockDO>> stocksByInnerUnitNo = new LinkedHashMap<>();
        for (HcFinishedStockDO stock : hcFinishedStockMapper.selectListByInnerUnitNos(innerUnitNos)) {
            if (StrUtil.isNotBlank(stock.getInnerUnitNo())) {
                stocksByInnerUnitNo.computeIfAbsent(stock.getInnerUnitNo(), key -> new ArrayList<>()).add(stock);
            }
        }
        return boxes.stream()
                .map(box -> buildInboundBoxResp(box,
                        stocksByInnerUnitNo.getOrDefault(box.getInnerUnitNo(), Collections.emptyList()),
                        itemsByBoxId.getOrDefault(box.getId(), Collections.emptyList())))
                .toList();
    }

    private InboundBoxRespVO buildInboundBoxResp(HcInnerPackUnitDO box,
                                                  List<HcFinishedStockDO> stocks,
                                                  List<HcInnerPackUnitItemDO> packageItems) {
        InboundBoxRespVO resp = new InboundBoxRespVO();
        resp.setId(box.getId());
        resp.setBoxNo(box.getInnerUnitNo());
        resp.setPlanId(box.getPlanId());
        resp.setPlanNo(box.getPlanNo());
        resp.setPlanOperationId(box.getPlanOperationId());
        resp.setPackageSpec(box.getPackageSpec());
        resp.setTargetQty(box.getTargetQty());
        resp.setMaterialCode(box.getMaterialCode());
        resp.setMaterialName(box.getMaterialName());
        resp.setModelCode(box.getModelCode());
        resp.setMotherSegmentBatchNo(box.getBatchNo());
        resp.setWarehouseCode(box.getWarehouseCode());
        resp.setWarehouseName(box.getWarehouseName());
        resp.setLocationCode(box.getLocationCode());
        resp.setLocationName(box.getLocationName());
        resp.setLabelNo(box.getLabelNo());
        resp.setStatus(box.getUnitStatus());
        resp.setPrintCount(box.getPrintCount());
        resp.setLastPrintTime(box.getLastPrintTime());
        resp.setLockUserName(box.getLockUserName());
        resp.setLockTime(box.getLockTime());
        resp.setInboundUserName(box.getInboundUserName());
        resp.setInboundTime(box.getInboundTime());
        resp.setRecorderName(box.getRecorderName());
        resp.setRecorderTime(box.getRecorderTime());
        resp.setRemark(box.getRemark());
        resp.setExtraJson(box.getExtraJson());
        Set<String> hiddenSliceBatchNos = new HashSet<>();
        for (HcFinishedStockDO stock : stocks) {
            if (List.of(STATUS_OUTBOUND_LOCKED, STATUS_ALLOCATED, STATUS_SHIPPED)
                    .contains(StrUtil.blankToDefault(stock.getStockStatus(), ""))) {
                String sliceBatchNo = firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo());
                if (StrUtil.isNotBlank(sliceBatchNo)) {
                    hiddenSliceBatchNos.add(sliceBatchNo);
                }
            }
        }
        List<HcInnerPackUnitItemDO> visibleItems = packageItems.stream()
                .filter(item -> !hiddenSliceBatchNos.contains(firstNotBlank(item.getSliceBatchNo(), item.getProductionBatchNo())))
                .toList();
        if (!packageItems.isEmpty()) {
            HcInnerPackUnitItemDO firstItem = packageItems.get(0);
            LocalDate productionDate = resolvePackagingItemProductionDate(firstItem);
            resp.setExpiryDate(resolvePackagingItemExpiryDate(firstItem, productionDate));
        }
        resp.setPrintItems(packageItems.stream()
                .limit(2)
                .map(this::buildInboundBoxPrintItemResp)
                .toList());
        resp.setCurrentQty(visibleItems.size());
        resp.setInboundLockedQty((int) stocks.stream()
                .filter(stock -> STATUS_INBOUND_LOCKED.equals(StrUtil.blankToDefault(stock.getStockStatus(), "")))
                .count());
        resp.setOutboundLockedQty((int) stocks.stream()
                .filter(stock -> List.of(STATUS_OUTBOUND_LOCKED, STATUS_ALLOCATED).contains(StrUtil.blankToDefault(stock.getStockStatus(), "")))
                .count());
        resp.setQualityStatus(resolvePackageQualityStatus(packageItems));
        resp.setItems(visibleItems.stream()
                .map(this::buildInboundBoxItemResp)
                .toList());
        return resp;
    }

    private Map<String, List<InboundBoxRespVO>> groupInboundPackedPackages(String keyword) {
        Map<String, List<InboundBoxRespVO>> result = new LinkedHashMap<>();
        hcInnerPackUnitMapper.selectFgInboundPackedPackageSegmentList(StrUtil.trimToNull(keyword)).stream()
                .map(this::buildInboundBoxResp)
                .filter(this::isVisibleInboundPackage)
                .forEach(box -> {
                    String segmentBatchNo = resolveInboundPackageSegmentBatchNo(box);
                    result.computeIfAbsent(segmentBatchNo, key -> new ArrayList<>()).add(box);
                });
        return result;
    }

    private List<Long> normalizeInboundPackageBatchIds(List<Long> packageIds) {
        if (packageIds == null || packageIds.isEmpty()) {
            throw invalidParamException("请至少选择一个包装单");
        }
        LinkedHashSet<Long> distinctIds = new LinkedHashSet<>(packageIds);
        if (distinctIds.size() != packageIds.size() || distinctIds.contains(null)) {
            throw invalidParamException("选择的包装单不能重复或为空");
        }
        List<Long> normalizedIds = new ArrayList<>(distinctIds);
        normalizedIds.sort(Comparator.naturalOrder());
        return normalizedIds;
    }

    private InboundPackageBatchActionRespVO buildInboundPackageBatchActionResp(List<String> packageNos) {
        InboundPackageBatchActionRespVO respVO = new InboundPackageBatchActionRespVO();
        respVO.setPackageCount(packageNos.size());
        respVO.setPackageNos(packageNos);
        return respVO;
    }

    private String resolveInboundPackageSegmentBatchNo(InboundBoxRespVO box) {
        List<String> candidates = new ArrayList<>();
        candidates.add(box.getMotherSegmentBatchNo());
        if (box.getItems() != null) {
            box.getItems().forEach(item -> {
                candidates.add(item.getProductionBatchNo());
                candidates.add(item.getSliceBatchNo());
            });
        }
        String segmentBatchNo = resolvePackageSegmentBatchNo(candidates.toArray(String[]::new));
        return StrUtil.blankToDefault(segmentBatchNo, "未识别段批次");
    }

    private InboundPackageSegmentRespVO buildInboundPackageSegmentResp(String segmentBatchNo,
                                                                        List<InboundBoxRespVO> packages) {
        InboundBoxRespVO first = packages.get(0);
        InboundPackageSegmentRespVO resp = new InboundPackageSegmentRespVO();
        resp.setSegmentBatchNo(segmentBatchNo);
        resp.setPlanId(first.getPlanId());
        resp.setPlanNo(first.getPlanNo());
        resp.setPlanOperationId(first.getPlanOperationId());
        resp.setMaterialCode(first.getMaterialCode());
        resp.setMaterialName(first.getMaterialName());
        resp.setModelCode(first.getModelCode());
        resp.setQualityStatus(packages.stream().anyMatch(box -> INSPECTION_RESULT_NG.equalsIgnoreCase(
                StrUtil.blankToDefault(box.getQualityStatus(), ""))) ? INSPECTION_RESULT_NG
                : packages.stream().anyMatch(box -> QUALITY_FROZEN.equals(box.getQualityStatus())) ? QUALITY_FROZEN : INSPECTION_RESULT_OK);
        resp.setPackageCount(packages.size());
        resp.setTotalPieceCount(packages.stream().mapToInt(box -> intValue(box.getCurrentQty())).sum());
        List<String> sampleSliceBatchNos = new ArrayList<>();
        for (InboundBoxRespVO box : packages) {
            if (box.getItems() == null) {
                continue;
            }
            for (InboundBoxItemRespVO item : box.getItems()) {
                String sliceBatchNo = firstNotBlank(item.getProductionBatchNo(), item.getSliceBatchNo());
                if (StrUtil.isNotBlank(sliceBatchNo) && !sampleSliceBatchNos.contains(sliceBatchNo)) {
                    sampleSliceBatchNos.add(sliceBatchNo);
                }
                if (sampleSliceBatchNos.size() >= 3) {
                    break;
                }
            }
            if (sampleSliceBatchNos.size() >= 3) {
                break;
            }
        }
        resp.setSampleSliceBatchNos(sampleSliceBatchNos);
        return resp;
    }

    private boolean isVisibleInboundPackage(InboundBoxRespVO resp) {
        if (resp == null) {
            return false;
        }
        if (List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED).contains(StrUtil.blankToDefault(resp.getStatus(), ""))
                && intValue(resp.getCurrentQty()) <= 0) {
            return false;
        }
        return true;
    }

    private InboundBoxItemRespVO buildInboundBoxItemResp(HcInnerPackUnitItemDO item) {
        InboundBoxItemRespVO resp = new InboundBoxItemRespVO();
        resp.setId(item.getId());
        resp.setBoxId(item.getInnerUnitId());
        resp.setBoxNo(item.getInnerUnitNo());
        resp.setSourceType(item.getSourceType());
        resp.setSourceCutRoundReportId(item.getSourceCutRoundReportId());
        resp.setSourceManualPieceId(item.getSourceManualPieceId());
        resp.setSliceBatchNo(item.getSliceBatchNo());
        resp.setProductionBatchNo(item.getProductionBatchNo());
        resp.setQualityStatus(item.getQualityStatus());
        resp.setScanUserName(item.getScanUserName());
        resp.setScanTime(item.getScanTime());
        return resp;
    }

    /**
     * 待上架包装明细只持久化了包装综合质量状态。统一不合格品库存查询还需要展示包装前的
     * FQC、COA 与过程风险原因，因此按明细保留的来源 ID 批量回查来源质量信息。
     */
    private void fillInboundPackageItemQualityDetails(List<InboundBoxRespVO> boxes) {
        if (boxes == null || boxes.isEmpty()) {
            return;
        }
        List<InboundBoxItemRespVO> items = boxes.stream()
                .filter(Objects::nonNull)
                .map(InboundBoxRespVO::getItems)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .toList();
        if (items.isEmpty()) {
            return;
        }

        List<Long> cutRoundReportIds = items.stream()
                .map(InboundBoxItemRespVO::getSourceCutRoundReportId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<HcCutRoundReportDO> cutRoundReports = hcCutRoundReportMapper.selectListByIds(cutRoundReportIds);
        Map<Long, HcCutRoundReportDO> cutRoundReportMap = new LinkedHashMap<>();
        for (HcCutRoundReportDO report : cutRoundReports) {
            if (report != null && report.getId() != null) {
                cutRoundReportMap.put(report.getId(), report);
            }
        }
        Map<String, CoaInspectionMeta> coaInspectionMap =
                buildCoaInspectionMapForCutRoundReports(cutRoundReports);

        Map<Long, HcPackagingManualPieceDO> manualPieceMap = new LinkedHashMap<>();
        items.stream()
                .map(InboundBoxItemRespVO::getSourceManualPieceId)
                .filter(Objects::nonNull)
                .distinct()
                .map(hcPackagingManualPieceMapper::selectById)
                .filter(Objects::nonNull)
                .forEach(piece -> manualPieceMap.put(piece.getId(), piece));

        for (InboundBoxItemRespVO item : items) {
            HcCutRoundReportDO report = cutRoundReportMap.get(item.getSourceCutRoundReportId());
            if (report != null) {
                fillInboundPackageItemCutRoundQuality(item, report, coaInspectionMap);
            } else {
                HcPackagingManualPieceDO manualPiece = manualPieceMap.get(item.getSourceManualPieceId());
                if (manualPiece != null) {
                    fillInboundPackageItemManualQuality(item, manualPiece);
                }
            }
            item.setNgReason(buildInboundPackageItemNgReason(item));
        }
    }

    private void fillInboundPackageItemCutRoundQuality(InboundBoxItemRespVO item,
                                                        HcCutRoundReportDO report,
                                                        Map<String, CoaInspectionMeta> coaInspectionMap) {
        item.setInspectionResult(report.getInspectionResult());
        item.setInspectionRemark(report.getInspectionRemark());
        item.setQualityRiskFlag(report.getQualityRiskFlag());
        item.setQualityRiskSnapshotJson(report.getQualityRiskSnapshotJson());
        CoaInspectionMeta coaMeta = coaInspectionMap.get(resolveCoaScopeBatchNo(report));
        if (coaMeta == null) {
            return;
        }
        item.setCoaInspectionResult(coaMeta.result);
        item.setCoaInspectionStatus(coaMeta.status);
        item.setCoaNgReason(coaMeta.ngReason);
    }

    private void fillInboundPackageItemManualQuality(InboundBoxItemRespVO item,
                                                      HcPackagingManualPieceDO piece) {
        item.setInspectionResult(piece.getInspectionResult());
        item.setCoaInspectionResult(piece.getCoaInspectionResult());
        if (StrUtil.isNotBlank(piece.getCoaInspectionResult())) {
            item.setCoaInspectionStatus(INSPECTION_STATUS_COMPLETED);
        }
        String manualReason = firstNotBlank(piece.getRemark(), piece.getBackfillReason());
        if (isInspectionNg(piece.getInspectionResult())) {
            item.setInspectionRemark(manualReason);
        }
        if (isInspectionNg(piece.getCoaInspectionResult())) {
            item.setCoaNgReason(manualReason);
        }
    }

    private String buildInboundPackageItemNgReason(InboundBoxItemRespVO item) {
        List<String> reasons = new ArrayList<>();
        if (isInspectionNg(item.getInspectionResult())) {
            reasons.add("FQC：" + StrUtil.blankToDefault(item.getInspectionRemark(), "不合格"));
        }
        String coaResult = StrUtil.trimToEmpty(item.getCoaInspectionResult()).toUpperCase(Locale.ROOT);
        if (INSPECTION_RESULT_NG.equals(coaResult)) {
            reasons.add("COA：" + StrUtil.blankToDefault(item.getCoaNgReason(), "不合格"));
        } else if (INSPECTION_RESULT_NG.equalsIgnoreCase(item.getQualityStatus())
                && COA_RESULT_PENDING.equals(coaResult)) {
            reasons.add("COA：" + StrUtil.blankToDefault(item.getCoaNgReason(), "待检或未放行"));
        }
        if (StrUtil.isNotBlank(item.getQualityRiskFlag())
                && !"NONE".equalsIgnoreCase(item.getQualityRiskFlag())) {
            reasons.add("质量风险：" + item.getQualityRiskFlag());
        }
        if (reasons.isEmpty() && INSPECTION_RESULT_NG.equalsIgnoreCase(item.getQualityStatus())) {
            reasons.add("历史源数据未留存具体不合格原因");
        }
        return String.join("；", reasons);
    }

    /**
     * 为二维码标签构建片明细，日期沿用成品包装报工的有效期口径。
     */
    private InboundBoxItemRespVO buildInboundBoxPrintItemResp(HcInnerPackUnitItemDO item) {
        InboundBoxItemRespVO resp = buildInboundBoxItemResp(item);
        LocalDate productionDate = resolvePackagingItemProductionDate(item);
        resp.setExpiryDate(resolvePackagingItemExpiryDate(item, productionDate));
        return resp;
    }

    private OutboundOrderRespVO buildOutboundOrderResp(HcFgOutboundOrderDO order) {
        OutboundOrderRespVO resp = new OutboundOrderRespVO();
        resp.setId(order.getId());
        resp.setOutboundNo(order.getOutboundNo());
        resp.setSourceSaleOrderId(order.getSourceSaleOrderId());
        resp.setSourceNoticeId(order.getSourceNoticeId());
        resp.setShippingOrderNo(order.getShippingOrderNo());
        resp.setShippingNoticeNo(order.getShippingNoticeNo());
        resp.setCustomerCode(order.getCustomerCode());
        resp.setCustomerName(order.getCustomerName());
        resp.setErpOrderNo(order.getErpOrderNo());
        resp.setOrderNo(order.getOrderNo());
        resp.setMaterialCode(order.getMaterialCode());
        resp.setMaterialName(order.getMaterialName());
        resp.setModelCode(order.getModelCode());
        resp.setProductType(order.getProductType());
        resp.setProductSize(order.getProductSize());
        resp.setShippingTime(order.getShippingTime());
        resp.setShipQty(order.getShipQty());
        resp.setBoxCount(order.getBoxCount());
        resp.setPieceCount(order.getPieceCount());
        resp.setOutboundStatus(order.getOutboundStatus());
        resp.setBoxes(getOutboundBoxList(order.getId()));
        return resp;
    }

    private OutboundBoxRespVO buildOutboundBoxResp(HcFgOutboundBoxDO box) {
        OutboundBoxRespVO resp = new OutboundBoxRespVO();
        resp.setId(box.getId());
        resp.setOutboundOrderId(box.getOutboundOrderId());
        resp.setOutboundNo(box.getOutboundNo());
        resp.setBoxNo(box.getOutboundBoxNo());
        resp.setErpOrderNo(box.getErpOrderNo());
        resp.setMaterialCode(box.getMaterialCode());
        resp.setMaterialName(box.getMaterialName());
        resp.setModelCode(box.getModelCode());
        resp.setTargetQty(box.getTargetQty());
        resp.setCurrentQty(box.getCurrentQty());
        resp.setStatus(box.getBoxStatus());
        resp.setLabelNo(box.getLabelNo());
        resp.setPrintCount(box.getPrintCount());
        resp.setLastPrintTime(box.getLastPrintTime());
        resp.setItems(hcFgOutboundBoxItemMapper.selectListByOutboundBoxId(box.getId()).stream()
                .map(this::buildOutboundItemResp)
                .toList());
        return resp;
    }

    private OutboundBoxItemRespVO buildOutboundItemResp(HcFgOutboundBoxItemDO item) {
        OutboundBoxItemRespVO resp = new OutboundBoxItemRespVO();
        resp.setId(item.getId());
        resp.setOutboundBoxId(item.getOutboundBoxId());
        resp.setOutboundBoxNo(item.getOutboundBoxNo());
        resp.setFinishedStockId(item.getFinishedStockId());
        resp.setInboundNo(item.getInboundNo());
        resp.setInboundBoxNo(item.getInboundBoxNo());
        resp.setInboundInnerUnitNo(item.getInboundInnerUnitNo());
        resp.setSliceBatchNo(item.getSliceBatchNo());
        resp.setMaterialCode(item.getMaterialCode());
        resp.setModelCode(item.getModelCode());
        resp.setBatchNo(item.getBatchNo());
        resp.setQualityStatus(item.getQualityStatus());
        resp.setScanUserName(item.getScanUserName());
        resp.setScanTime(item.getScanTime());
        return resp;
    }

    private FgLocationGridRespVO buildFgLocationResp(HcLocationDO location) {
        List<HcFinishedStockDO> activeStocks = StrUtil.isBlank(location.getLocationCode())
                ? Collections.emptyList()
                : hcFinishedStockMapper.selectActiveListByLocationCodes(List.of(location.getLocationCode()));
        HcFgRackDO rack = location.getRackId() == null ? null : hcFgRackMapper.selectById(location.getRackId());
        HcFgLayerDO layer = location.getLayerId() == null ? null : hcFgLayerMapper.selectById(location.getLayerId());
        return buildFgLocationResp(location, activeStocks, rack, layer);
    }

    private FgLocationGridRespVO buildFgLocationResp(HcLocationDO location, List<HcFinishedStockDO> activeStocks) {
        HcFgRackDO rack = location.getRackId() == null ? null : hcFgRackMapper.selectById(location.getRackId());
        HcFgLayerDO layer = location.getLayerId() == null ? null : hcFgLayerMapper.selectById(location.getLayerId());
        return buildFgLocationResp(location, activeStocks, rack, layer);
    }

    private FgLocationGridRespVO buildFgLocationResp(HcLocationDO location, List<HcFinishedStockDO> activeStocks,
                                                      HcFgRackDO rack, HcFgLayerDO layer) {
        List<HcFinishedStockDO> currentActiveStocks = activeStocks == null ? Collections.emptyList() : activeStocks;
        long activePackageQty = currentActiveStocks.stream()
                .map(stock -> firstNotBlank(stock.getInnerUnitNo(), stock.getOuterBoxNo(), stock.getStockNo()))
                .filter(StrUtil::isNotBlank)
                .distinct()
                .count();
        if (activePackageQty == 0 && !currentActiveStocks.isEmpty()) {
            activePackageQty = currentActiveStocks.size();
        }
        int occupiedPieceQty = currentActiveStocks.stream()
                .mapToInt(stock -> Math.max(1, intValue(stock.getQty())))
                .sum();
        int occupiedQty = occupiedPieceQty;
        List<String> occupiedSliceBatchNos = currentActiveStocks.stream()
                .map(HcFinishedStockDO::getSliceBatchNo)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .sorted()
                .toList();
        int capacityQty = intValue(location.getCapacityQty());
        HcFinishedStockDO firstStock = currentActiveStocks.isEmpty() ? null : currentActiveStocks.get(0);
        FgLocationGridRespVO resp = new FgLocationGridRespVO();
        resp.setId(location.getId());
        resp.setWarehouseId(location.getWarehouseId());
        resp.setRackId(location.getRackId());
        resp.setRackNo(rack == null ? parseFgLocationCodePart(location.getLocationCode(), 1) : rack.getRackNo());
        resp.setRackName(rack == null ? null : rack.getRackName());
        resp.setLayerId(location.getLayerId());
        resp.setLayerNo(layer == null ? parseFgLocationCodePart(location.getLocationCode(), 2) : layer.getLayerNo());
        resp.setLayerName(layer == null ? null : layer.getLayerName());
        resp.setAreaNo(location.getAreaNo() == null ? parseFgLocationCodePart(location.getLocationCode(), 3) : location.getAreaNo());
        resp.setGridNo(location.getGridNo());
        resp.setLocationCode(location.getLocationCode());
        resp.setLocationName(location.getLocationName());
        resp.setWarehouseCode(location.getWarehouseCode());
        resp.setWarehouseName(location.getWarehouseName());
        resp.setLocationType(location.getLocationType());
        resp.setQualityScope(normalizeFgLocationQualityScope(location.getQualityScope()));
        resp.setPositionDesc(location.getPositionDesc());
        resp.setMixBatchFlag(location.getMixBatchFlag());
        resp.setMixModelFlag(location.getMixModelFlag());
        resp.setCapacityQty(capacityQty);
        resp.setOccupiedQty(occupiedQty);
        resp.setOccupiedPieceQty(occupiedPieceQty);
        resp.setOccupiedSliceBatchNos(occupiedSliceBatchNos);
        resp.setPieces(buildFgLocationPieces(currentActiveStocks));
        resp.setAvailableQty(Math.max(0, capacityQty - occupiedQty));
        resp.setStatus(location.getStatus());
        resp.setQrCode(firstNotBlank(location.getQrCode(), location.getLocationCode()));
        resp.setOccupied(occupiedQty > 0);
        if (firstStock != null) {
            resp.setOccupiedSliceBatchNo(firstStock.getSliceBatchNo());
            resp.setOccupiedStockNo(firstStock.getStockNo());
            resp.setOccupiedInnerUnitNo(firstStock.getInnerUnitNo());
        }
        resp.setDeletable(occupiedQty == 0);
        return resp;
    }

    private FgLocationTreeLayerRespVO buildFgLocationTreeLayerResp(HcFgLayerDO layer,
                                                                     List<FgLocationGridRespVO> areas) {
        FgLocationTreeLayerRespVO resp = new FgLocationTreeLayerRespVO();
        resp.setId(layer.getId());
        resp.setRackId(layer.getRackId());
        resp.setLayerNo(layer.getLayerNo());
        resp.setLayerName(layer.getLayerName());
        resp.setStatus(layer.getStatus());
        resp.setSortNo(layer.getSortNo());
        resp.setRemark(layer.getRemark());
        resp.setAreas(areas);
        return resp;
    }

    private FgLocationTreeRackRespVO buildFgLocationTreeRackResp(HcFgRackDO rack,
                                                                   List<HcFgLayerDO> layers,
                                                                   Map<Long, List<FgLocationGridRespVO>> areasByLayerId) {
        FgLocationTreeRackRespVO rackResp = new FgLocationTreeRackRespVO();
        rackResp.setId(rack.getId());
        rackResp.setWarehouseId(rack.getWarehouseId());
        rackResp.setWarehouseCode(rack.getWarehouseCode());
        rackResp.setWarehouseName(rack.getWarehouseName());
        rackResp.setRackNo(rack.getRackNo());
        rackResp.setRackName(rack.getRackName());
        rackResp.setStatus(rack.getStatus());
        rackResp.setSortNo(rack.getSortNo());
        rackResp.setRemark(rack.getRemark());
        rackResp.setLayers(layers.stream()
                .map(layer -> buildFgLocationTreeLayerResp(layer,
                        areasByLayerId.getOrDefault(layer.getId(), Collections.emptyList())))
                .toList());
        return rackResp;
    }

    private Map<Long, HcFgWarehouseDO> getFgWarehouseMap() {
        Map<Long, HcFgWarehouseDO> result = new LinkedHashMap<>();
        hcFgWarehouseMapper.selectListAll().forEach(warehouse -> result.put(warehouse.getId(), warehouse));
        return result;
    }

    private Map<Long, HcFgRackDO> getFgRackMap(java.util.Collection<Long> warehouseIds) {
        Map<Long, HcFgRackDO> result = new LinkedHashMap<>();
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return result;
        }
        warehouseIds.forEach(warehouseId -> hcFgRackMapper.selectListByWarehouseId(warehouseId)
                .forEach(rack -> result.put(rack.getId(), rack)));
        return result;
    }

    private Map<Long, HcFgLayerDO> getFgLayerMap(java.util.Collection<Long> rackIds) {
        Map<Long, HcFgLayerDO> result = new LinkedHashMap<>();
        if (rackIds == null || rackIds.isEmpty()) {
            return result;
        }
        hcFgLayerMapper.selectListByRackIds(rackIds).forEach(layer -> result.put(layer.getId(), layer));
        return result;
    }

    private List<FgLocationPieceRespVO> buildFgLocationPieces(List<HcFinishedStockDO> activeStocks) {
        if (activeStocks == null || activeStocks.isEmpty()) {
            return Collections.emptyList();
        }
        List<FgLocationPieceRespVO> pieces = new ArrayList<>();
        activeStocks.stream()
                .sorted(Comparator.comparing(HcFinishedStockDO::getId, Comparator.nullsLast(Long::compareTo)))
                .forEach(stock -> {
                    int pieceQty = Math.max(1, intValue(stock.getQty()));
                    for (int i = 0; i < pieceQty; i++) {
                        pieces.add(buildFgLocationPieceResp(stock));
                    }
                });
        return pieces;
    }

    private FgLocationPieceRespVO buildFgLocationPieceResp(HcFinishedStockDO stock) {
        FgLocationPieceRespVO resp = new FgLocationPieceRespVO();
        resp.setStockId(stock.getId());
        resp.setStockNo(stock.getStockNo());
        resp.setSliceBatchNo(stock.getSliceBatchNo());
        resp.setMaterialCode(stock.getMaterialCode());
        resp.setMaterialName(stock.getMaterialName());
        resp.setModelCode(stock.getModelCode());
        resp.setBatchNo(stock.getBatchNo());
        resp.setProductSize(stock.getProductSize());
        resp.setQty(stock.getQty());
        resp.setQualityStatus(stock.getQualityStatus());
        resp.setStockStatus(stock.getStockStatus());
        resp.setInboundTime(stock.getInboundTime());
        return resp;
    }

    private FgStockLocationOverviewRespVO buildFgStockLocationOverviewResp(HcLocationDO location) {
        int capacityQty = intValue(location.getCapacityQty());
        int pieceCount = (int) activePieceCount(location.getLocationCode());
        Long packageCount = hcFinishedStockMapper.selectActivePackageCountByLocationCode(location.getLocationCode());
        int currentPackageCount = packageCount == null ? 0 : packageCount.intValue();
        int occupiedQty = pieceCount;
        FgStockLocationOverviewRespVO resp = new FgStockLocationOverviewRespVO();
        resp.setId(location.getId());
        resp.setGridNo(location.getGridNo());
        resp.setLocationCode(location.getLocationCode());
        resp.setLocationName(location.getLocationName());
        resp.setWarehouseCode(location.getWarehouseCode());
        resp.setWarehouseName(location.getWarehouseName());
        resp.setPositionDesc(location.getPositionDesc());
        resp.setCapacityQty(capacityQty);
        resp.setOccupiedQty(occupiedQty);
        resp.setAvailableQty(Math.max(0, capacityQty - occupiedQty));
        resp.setPackageCount(currentPackageCount);
        resp.setPieceCount(pieceCount);
        resp.setPercent(capacityQty <= 0 ? 0 : Math.min(100, Math.round((occupiedQty * 100f) / capacityQty)));
        resp.setStatus(location.getStatus());
        return resp;
    }

    private PackageAuxStockRespVO buildPackageAuxStockResp(HcPackageAuxStockDO stock) {
        PackageAuxStockRespVO resp = new PackageAuxStockRespVO();
        resp.setId(stock.getId());
        resp.setAuxCategory(stock.getAuxCategory());
        resp.setAuxCategoryName(stock.getAuxCategoryName());
        resp.setMaterialCode(stock.getMaterialCode());
        resp.setMaterialName(stock.getMaterialName());
        resp.setAuxSpec(stock.getAuxSpec());
        resp.setBatchNo(stock.getBatchNo());
        resp.setSourceWarehouseCode(stock.getSourceWarehouseCode());
        resp.setSourceWarehouseName(stock.getSourceWarehouseName());
        resp.setEdgeWarehouseCode(stock.getEdgeWarehouseCode());
        resp.setEdgeWarehouseName(stock.getEdgeWarehouseName());
        resp.setStockMeasureMode(stock.getStockMeasureMode());
        resp.setReceiveQty(stock.getReceiveQty());
        resp.setUsedQty(stock.getUsedQty());
        resp.setAvailableQty(stock.getAvailableQty());
        resp.setStockStatus(stock.getStockStatus());
        resp.setReceiverName(stock.getReceiverName());
        resp.setReceiveDate(stock.getReceiveDate());
        resp.setReceiveTime(stock.getReceiveTime());
        resp.setPrintCount(stock.getPrintCount());
        resp.setPrintTime(stock.getPrintTime());
        resp.setErpTransferNo(stock.getErpTransferNo());
        resp.setTransferQty(stock.getTransferQty());
        resp.setTransferUnit(stock.getTransferUnit());
        resp.setUnpackQty(stock.getUnpackQty());
        resp.setUnpackUnit(stock.getUnpackUnit());
        resp.setErpTransferStatus(stock.getErpTransferStatus());
        resp.setRemark(stock.getRemark());
        return resp;
    }

    private PackageAuxConsumeRecordRespVO buildPackageAuxConsumeRecordResp(HcPackageAuxConsumeRecordDO record) {
        PackageAuxConsumeRecordRespVO resp = new PackageAuxConsumeRecordRespVO();
        resp.setId(record.getId());
        resp.setStockId(record.getStockId());
        resp.setAuxCategory(record.getAuxCategory());
        resp.setAuxCategoryName(record.getAuxCategoryName());
        resp.setMaterialCode(record.getMaterialCode());
        resp.setMaterialName(record.getMaterialName());
        resp.setAuxSpec(record.getAuxSpec());
        resp.setBatchNo(record.getBatchNo());
        resp.setBizType(record.getBizType());
        resp.setBizNo(record.getBizNo());
        resp.setConsumeQty(record.getConsumeQty());
        resp.setBeforeAvailableQty(record.getBeforeAvailableQty());
        resp.setAfterAvailableQty(record.getAfterAvailableQty());
        resp.setConsumeStatus(record.getConsumeStatus());
        resp.setRecordDate(record.getRecordDate());
        resp.setRecorderName(record.getRecorderName());
        resp.setRecorderTime(record.getRecorderTime());
        resp.setRemark(record.getRemark());
        return resp;
    }

    private ShippingNoticeRespVO buildShippingNoticeResp(HcFgShippingNoticeDO notice, boolean withItems) {
        ShippingNoticeRespVO resp = new ShippingNoticeRespVO();
        resp.setId(notice.getId());
        resp.setNoticeNo(notice.getNoticeNo());
        resp.setCustomerId(notice.getCustomerId());
        resp.setCustomerCode(notice.getCustomerCode());
        resp.setCustomerName(notice.getCustomerName());
        resp.setProductType(notice.getProductType());
        resp.setMaterialCode(notice.getMaterialCode());
        resp.setMaterialName(notice.getMaterialName());
        resp.setModelCode(notice.getModelCode());
        resp.setProductSize(notice.getProductSize());
        resp.setOrderNo(notice.getOrderNo());
        resp.setErpOrderNo(notice.getErpOrderNo());
        resp.setShippingTime(notice.getShippingTime());
        resp.setExternalProductModel(notice.getExternalProductModel());
        resp.setExternalProductCode(notice.getExternalProductCode());
        resp.setExternalProductInfo(notice.getExternalProductInfo());
        resp.setRequiredShipQty(notice.getRequiredShipQty());
        resp.setRequiredSliceRange(notice.getRequiredSliceRange());
        resp.setRequiredBatchNo(notice.getRequiredBatchNo());
        resp.setRequiredProductionDate(notice.getRequiredProductionDate());
        resp.setRequiredExpiryDate(notice.getRequiredExpiryDate());
        resp.setPackingRequirement(notice.getPackingRequirement());
        resp.setShippingConfirmName(notice.getShippingConfirmName());
        resp.setOutboundConfirmName(notice.getOutboundConfirmName());
        resp.setOutboundConfirmTime(notice.getOutboundConfirmTime());
        resp.setOutboundConfirmRemark(notice.getOutboundConfirmRemark());
        resp.setNoticeQty(notice.getNoticeQty());
        resp.setLockedQty(countEffectiveShippingPickedQty(notice.getId()));
        resp.setInspectionCompletedQty(hcFgShippingNoticeItemMapper.countShippingInspectionCompletedByNoticeId(notice.getId()));
        resp.setChangeVersion(intValue(notice.getChangeVersion()));
        resp.setNoticeStatus(notice.getNoticeStatus());
        resp.setRecorderName(notice.getRecorderName());
        resp.setRecorderTime(notice.getRecorderTime());
        HcFgShippingNoticeItemDO packageItem = hcFgShippingNoticeItemMapper.selectLatestPackagedByNoticeId(notice.getId());
        if (packageItem != null) {
            resp.setShippingPackageName(packageItem.getShippingPackageName());
            resp.setShippingPackageTime(packageItem.getShippingPackageTime());
        }
        resp.setCancelName(notice.getCancelName());
        resp.setCancelTime(notice.getCancelTime());
        resp.setCancelReason(notice.getCancelReason());
        resp.setRemark(notice.getRemark());
        if (withItems) {
            resp.setItems(hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId()).stream()
                    .filter(item -> !PRODUCT_TYPE_SAMPLE.equals(notice.getProductType())
                            || NOTICE_STATUS_CANCELLED.equals(notice.getNoticeStatus())
                            || !NOTICE_STATUS_CANCELLED.equals(item.getLockStatus()))
                    .map(this::buildShippingNoticeItemResp)
                    .toList());
            // 详情需保留已发货等历史领用记录；有效状态查询仅用于库存锁定和业务控制。
            resp.setPickItems(hcFgShippingNoticePickItemMapper.selectListByNoticeId(notice.getId()).stream()
                    .map(this::buildShippingNoticePickItemResp)
                    .toList());
            resp.setAttachments(hcFgShippingNoticeAttachmentMapper.selectListByNoticeId(notice.getId()).stream()
                    .map(this::buildShippingNoticeAttachmentResp)
                    .toList());
        }
        return resp;
    }

    private ShippingNoticeAttachmentRespVO buildShippingNoticeAttachmentResp(HcFgShippingNoticeAttachmentDO attachment) {
        ShippingNoticeAttachmentRespVO resp = new ShippingNoticeAttachmentRespVO();
        resp.setId(attachment.getId());
        resp.setNoticeId(attachment.getNoticeId());
        resp.setNoticeNo(attachment.getNoticeNo());
        resp.setAttachmentName(attachment.getAttachmentName());
        resp.setAttachmentUrl(attachment.getAttachmentUrl());
        resp.setAttachmentType(attachment.getAttachmentType());
        resp.setSourceFileName(firstNotBlank(attachment.getSourceFileName(), attachment.getAttachmentName()));
        resp.setSourceSheetName(attachment.getSourceSheetName());
        resp.setSourceSheetIndex(attachment.getSourceSheetIndex());
        resp.setSourceSheetTotal(attachment.getSourceSheetTotal());
        fillShippingNoticeAttachmentSheetNavigation(resp, attachment);
        resp.setFileSize(attachment.getFileSize());
        resp.setRemark(attachment.getRemark());
        resp.setCreateTime(attachment.getCreateTime());
        return resp;
    }

    private void fillShippingNoticeAttachmentSheetNavigation(ShippingNoticeAttachmentRespVO resp,
                                                             HcFgShippingNoticeAttachmentDO attachment) {
        if (!SHIPPING_NOTICE_ATTACHMENT_EXCEL_IMPORT.equals(attachment.getAttachmentType())) {
            return;
        }
        String sourceFileName = firstNotBlank(attachment.getSourceFileName(), attachment.getAttachmentName());
        if (StrUtil.isBlank(sourceFileName)) {
            return;
        }
        List<HcFgShippingNoticeAttachmentDO> importSheets = hcFgShippingNoticeAttachmentMapper
                .selectImportListByFileName(SHIPPING_NOTICE_ATTACHMENT_EXCEL_IMPORT, sourceFileName);
        if (importSheets.isEmpty()) {
            return;
        }
        int currentIndex = -1;
        for (int i = 0; i < importSheets.size(); i++) {
            HcFgShippingNoticeAttachmentDO item = importSheets.get(i);
            if (Objects.equals(item.getId(), attachment.getId())
                    || Objects.equals(item.getNoticeId(), attachment.getNoticeId())
                    && Objects.equals(item.getSourceSheetName(), attachment.getSourceSheetName())) {
                currentIndex = i;
                break;
            }
        }
        if (currentIndex < 0) {
            return;
        }
        resp.setSourceFileName(sourceFileName);
        resp.setSourceSheetIndex(firstPositive(attachment.getSourceSheetIndex(), currentIndex + 1));
        resp.setSourceSheetTotal(firstPositive(attachment.getSourceSheetTotal(), importSheets.size()));
        if (currentIndex > 0) {
            HcFgShippingNoticeAttachmentDO previous = importSheets.get(currentIndex - 1);
            resp.setPreviousNoticeId(previous.getNoticeId());
            resp.setPreviousSheetName(previous.getSourceSheetName());
        }
        if (currentIndex + 1 < importSheets.size()) {
            HcFgShippingNoticeAttachmentDO next = importSheets.get(currentIndex + 1);
            resp.setNextNoticeId(next.getNoticeId());
            resp.setNextSheetName(next.getSourceSheetName());
        }
    }

    private ShippingNoticeItemRespVO buildShippingNoticeItemResp(HcFgShippingNoticeItemDO item) {
        ShippingNoticeItemRespVO resp = new ShippingNoticeItemRespVO();
        resp.setId(item.getId());
        resp.setNoticeId(item.getNoticeId());
        resp.setNoticeNo(item.getNoticeNo());
        resp.setFinishedStockId(item.getFinishedStockId());
        resp.setActualFinishedStockId(item.getActualFinishedStockId());
        resp.setStockNo(item.getStockNo());
        resp.setActualStockNo(item.getActualStockNo());
        resp.setOuterBoxNo(item.getOuterBoxNo());
        resp.setInnerUnitNo(item.getInnerUnitNo());
        resp.setPackageNo(item.getPackageNo());
        resp.setSliceBatchNo(item.getSliceBatchNo());
        resp.setActualSliceBatchNo(item.getActualSliceBatchNo());
        resp.setBatchNo(item.getBatchNo());
        resp.setInternalModelCode(item.getInternalModelCode());
        resp.setInternalItemCode(item.getInternalItemCode());
        resp.setCustomerProductBatchNo(item.getCustomerProductBatchNo());
        resp.setPackageSliceNo(item.getPackageSliceNo());
        resp.setMaterialCode(item.getMaterialCode());
        resp.setMaterialName(item.getMaterialName());
        resp.setModelCode(item.getModelCode());
        resp.setProductSize(item.getProductSize());
        resp.setStockQty(item.getStockQty());
        resp.setAvailableQty(item.getAvailableQty());
        resp.setLockedQty(item.getLockedQty());
        resp.setQualityStatus(item.getQualityStatus());
        resp.setWarehouseCode(item.getWarehouseCode());
        resp.setWarehouseName(item.getWarehouseName());
        resp.setLocationCode(item.getLocationCode());
        resp.setLocationName(item.getLocationName());
        resp.setActualLocationCode(item.getActualLocationCode());
        resp.setActualLocationName(item.getActualLocationName());
        resp.setInboundNo(item.getInboundNo());
        resp.setInboundTime(item.getInboundTime());
        resp.setLockStatus(item.getLockStatus());
        resp.setLockName(item.getLockName());
        resp.setLockTime(item.getLockTime());
        resp.setCancelName(item.getCancelName());
        resp.setCancelTime(item.getCancelTime());
        resp.setActualShipQty(item.getActualShipQty());
        resp.setCustomerSliceBatchNo(item.getCustomerSliceBatchNo());
        resp.setCustomerModelCode(item.getCustomerModelCode());
        resp.setShippingQualityNo(item.getShippingQualityNo());
        resp.setOqcOrderId(item.getOqcOrderId());
        resp.setOqcStatus(item.getOqcStatus());
        refreshShippingOqcSnapshot(resp);
        QmsFqcShippingDetailDO latestShippingDetail = qmsFqcShippingDetailMapper.selectLatestByShippingNoticeItemId(item.getId());
        resp.setShippingQualityNo(firstNotBlank(latestShippingDetail == null ? null : latestShippingDetail.getFqcNo(),
                resp.getShippingQualityNo()));
        resp.setShippingInspectorName(latestShippingDetail == null ? null : latestShippingDetail.getInspectorName());
        resp.setShippingInspectionResult(latestShippingDetail == null ? null : latestShippingDetail.getRowJudgment());
        resp.setShippingInspectionRemark(latestShippingDetail == null ? null
                : firstNotBlank(latestShippingDetail.getNgReason(), latestShippingDetail.getRemark()));
        resp.setShippingInspectionTime(latestShippingDetail == null ? null : latestShippingDetail.getInspectionTime());
        resp.setShippingPackageName(item.getShippingPackageName());
        resp.setShippingPackageTime(item.getShippingPackageTime());
        resp.setShippingPackageRemark(item.getShippingPackageRemark());
        resp.setShippedName(item.getShippedName());
        resp.setShippedTime(item.getShippedTime());
        resp.setMismatchReason(item.getMismatchReason());
        resp.setActualRemark(item.getActualRemark());
        resp.setRemark(item.getRemark());
        return resp;
    }

    private void refreshShippingOqcSnapshot(ShippingNoticeItemRespVO resp) {
        if (resp.getOqcOrderId() == null) {
            return;
        }
        try {
            QmsOqcRespVO oqc = qmsOqcService.getOqcResp(resp.getOqcOrderId());
            if (oqc != null) {
                resp.setShippingQualityNo(firstNotBlank(oqc.getOqcNo(), resp.getShippingQualityNo()));
                resp.setOqcStatus(firstNotBlank(oqc.getStatus(), resp.getOqcStatus()));
            }
        } catch (RuntimeException ignored) {
            // OQC 单被异常清理时仍返回发货明细快照，避免包装页面无法打开。
        }
    }

    private ShippingNoticePickItemRespVO buildShippingNoticePickItemResp(HcFgShippingNoticePickItemDO item) {
        ShippingNoticePickItemRespVO resp = new ShippingNoticePickItemRespVO();
        resp.setId(item.getId());
        resp.setNoticeId(item.getNoticeId());
        resp.setNoticeNo(item.getNoticeNo());
        resp.setSourceNoticeItemId(item.getSourceNoticeItemId());
        resp.setPickSourceType(firstNotBlank(item.getPickSourceType(), PICK_SOURCE_WAREHOUSE_STOCK));
        resp.setSourceInnerPackItemId(item.getSourceInnerPackItemId());
        resp.setSourceCutRoundReportId(item.getSourceCutRoundReportId());
        resp.setFinishedStockId(item.getFinishedStockId());
        resp.setStockNo(item.getStockNo());
        resp.setOuterBoxNo(item.getOuterBoxNo());
        resp.setInnerUnitNo(item.getInnerUnitNo());
        resp.setPackageNo(item.getPackageNo());
        resp.setSliceBatchNo(item.getSliceBatchNo());
        resp.setActualSliceBatchNo(item.getActualSliceBatchNo());
        resp.setBatchNo(item.getBatchNo());
        resp.setInternalModelCode(item.getInternalModelCode());
        resp.setInternalItemCode(item.getInternalItemCode());
        resp.setCustomerProductBatchNo(item.getCustomerProductBatchNo());
        resp.setMaterialCode(item.getMaterialCode());
        resp.setMaterialName(item.getMaterialName());
        resp.setModelCode(item.getModelCode());
        resp.setProductSize(item.getProductSize());
        resp.setStockQty(item.getStockQty());
        resp.setAvailableQty(item.getAvailableQty());
        resp.setLockedQty(item.getLockedQty());
        resp.setQualityStatus(item.getQualityStatus());
        resp.setWarehouseCode(item.getWarehouseCode());
        resp.setWarehouseName(item.getWarehouseName());
        resp.setLocationCode(item.getLocationCode());
        resp.setLocationName(item.getLocationName());
        resp.setInboundNo(item.getInboundNo());
        resp.setInboundTime(item.getInboundTime());
        QmsFqcShippingDetailDO latestShippingDetail = qmsFqcShippingDetailMapper.selectLatestByShippingPickItemId(item.getId());
        String shippingInspectionResult = latestShippingDetail == null ? null : latestShippingDetail.getRowJudgment();
        LocalDateTime shippingInspectionTime = latestShippingDetail == null ? null : latestShippingDetail.getInspectionTime();
        String shippingInspectorName = latestShippingDetail == null ? null : latestShippingDetail.getInspectorName();
        String shippingInspectionRemark = latestShippingDetail == null ? null
                : firstNotBlank(latestShippingDetail.getNgReason(), latestShippingDetail.getRemark());
        boolean inspectionNg = isInspectionNg(shippingInspectionResult);
        resp.setLockStatus(inspectionNg && NOTICE_STATUS_PACKAGED.equals(item.getLockStatus())
                ? NOTICE_STATUS_INSPECTED : item.getLockStatus());
        resp.setLockName(item.getLockName());
        resp.setLockTime(item.getLockTime());
        resp.setCancelName(item.getCancelName());
        resp.setCancelTime(item.getCancelTime());
        resp.setActualShipQty(item.getActualShipQty());
        resp.setShippingQualityNo(firstNotBlank(latestShippingDetail == null ? null : latestShippingDetail.getFqcNo(),
                item.getShippingQualityNo()));
        resp.setShippingInspectorName(shippingInspectorName);
        resp.setShippingInspectionResult(shippingInspectionResult);
        resp.setShippingInspectionRemark(shippingInspectionRemark);
        resp.setShippingInspectionTime(shippingInspectionTime);
        if (!inspectionNg) {
            resp.setShippingPackageName(item.getShippingPackageName());
            resp.setShippingPackageTime(item.getShippingPackageTime());
            resp.setShippingPackageRemark(item.getShippingPackageRemark());
        }
        resp.setShippedName(item.getShippedName());
        resp.setShippedTime(item.getShippedTime());
        resp.setRemark(item.getRemark());
        return resp;
    }

    private String resolvePackageQualityStatus(List<HcInnerPackUnitItemDO> items) {
        if (items == null || items.isEmpty()) {
            return INSPECTION_RESULT_OK;
        }
        return items.stream().anyMatch(item -> INSPECTION_RESULT_NG.equalsIgnoreCase(StrUtil.blankToDefault(item.getQualityStatus(), "")))
                ? INSPECTION_RESULT_NG : items.stream().anyMatch(item -> QUALITY_FROZEN.equals(item.getQualityStatus()))
                ? QUALITY_FROZEN : INSPECTION_RESULT_OK;
    }

    private void refreshInboundBoxQty(Long boxId) {
        HcInnerPackUnitDO box = getInboundBox(boxId);
        int qty = hcInnerPackUnitItemMapper.selectListByInnerUnitId(boxId).size();
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(boxId);
        update.setCurrentQty(qty);
        update.setUnitStatus(qty >= intValue(box.getTargetQty()) ? STATUS_PACKED : STATUS_WAITING_PIECE);
        hcInnerPackUnitMapper.updateById(update);
    }

    private void refreshOutboundBoxQty(Long boxId) {
        HcFgOutboundBoxDO box = getOutboundBox(boxId);
        int qty = hcFgOutboundBoxItemMapper.selectListByOutboundBoxId(boxId).size();
        HcFgOutboundBoxDO update = new HcFgOutboundBoxDO();
        update.setId(boxId);
        update.setCurrentQty(qty);
        update.setBoxStatus(qty >= intValue(box.getTargetQty()) ? STATUS_PACKED : STATUS_WAITING_PIECE);
        hcFgOutboundBoxMapper.updateById(update);
    }

    private void refreshOutboundOrder(Long outboundOrderId, boolean shipped) {
        List<HcFgOutboundBoxDO> boxes = hcFgOutboundBoxMapper.selectListByOutboundOrderId(outboundOrderId);
        List<HcFgOutboundBoxItemDO> items = hcFgOutboundBoxItemMapper.selectListByOutboundOrderId(outboundOrderId);
        HcFgOutboundOrderDO update = new HcFgOutboundOrderDO();
        update.setId(outboundOrderId);
        update.setBoxCount(boxes.size());
        update.setPieceCount(items.size());
        update.setOutboundStatus(shipped ? STATUS_SHIPPED : "PACKING");
        hcFgOutboundOrderMapper.updateById(update);
    }

    private HcLocationDO getFgLocation(Long id) {
        HcLocationDO location = hcLocationMapper.selectById(id);
        if (location == null || Boolean.TRUE.equals(location.getDeleted()) || !FG_LOCATION_SCENE.equals(location.getBizScene())) {
            throw invalidParamException("包装成品库库位不存在");
        }
        return location;
    }

    private HcFgRackDO getFgRack(Long id) {
        HcFgRackDO rack = hcFgRackMapper.selectById(id);
        if (rack == null || Boolean.TRUE.equals(rack.getDeleted()) || rack.getWarehouseId() == null) {
            throw invalidParamException("包装成品库货架不存在");
        }
        return rack;
    }

    private HcFgWarehouseDO getFgWarehouse(Long id) {
        HcFgWarehouseDO warehouse = hcFgWarehouseMapper.selectById(id);
        if (warehouse == null || Boolean.TRUE.equals(warehouse.getDeleted())) {
            throw invalidParamException("包装成品仓库不存在");
        }
        return warehouse;
    }

    private HcFgLayerDO getFgLayer(Long id) {
        HcFgLayerDO layer = hcFgLayerMapper.selectById(id);
        if (layer == null || Boolean.TRUE.equals(layer.getDeleted())) {
            throw invalidParamException("包装成品库货架层不存在");
        }
        return layer;
    }

    private HcLocationDO getFgLocationByCode(String locationCode) {
        HcLocationDO location = hcLocationMapper.selectByLocationCode(StrUtil.trimToEmpty(locationCode));
        return validateFgLocationForInbound(location);
    }

    private HcLocationDO getFgLocationByCodeForUpdate(String locationCode) {
        HcLocationDO location = hcLocationMapper.selectByLocationCodeForUpdate(StrUtil.trimToEmpty(locationCode));
        return validateFgLocationForInbound(location);
    }

    private HcLocationDO validateFgLocationForInbound(HcLocationDO location) {
        if (location == null || Boolean.TRUE.equals(location.getDeleted()) || !FG_LOCATION_SCENE.equals(location.getBizScene())) {
            throw invalidParamException("包装成品库库位不存在");
        }
        if (!DEFAULT_LOCATION_STATUS.equals(location.getStatus())) {
            throw invalidParamException("当前库位未启用");
        }
        HcFgRackDO rack = getFgRack(location.getRackId());
        HcFgWarehouseDO warehouse = getFgWarehouse(rack.getWarehouseId());
        HcFgLayerDO layer = getFgLayer(location.getLayerId());
        if (!DEFAULT_LOCATION_STATUS.equals(warehouse.getStatus())
                || !DEFAULT_LOCATION_STATUS.equals(rack.getStatus()) || !DEFAULT_LOCATION_STATUS.equals(layer.getStatus())) {
            throw invalidParamException("当前仓库、货架或货架层未启用");
        }
        return location;
    }

    private void ensureFgAreasEmpty(List<HcLocationDO> areas, String nodeName) {
        List<String> occupiedCodes = areas.stream()
                .filter(area -> activePieceCount(area.getLocationCode()) > 0)
                .map(HcLocationDO::getLocationCode)
                .filter(StrUtil::isNotBlank)
                .toList();
        if (!occupiedCodes.isEmpty()) {
            throw invalidParamException(String.format("%s下仍有库存区域，不能删除：%s", nodeName,
                    StrUtil.join(",", occupiedCodes.stream().limit(5).toList())));
        }
    }

    private void ensureLocationCapacity(HcLocationDO location, int addQty) {
        int capacityQty = intValue(location.getCapacityQty());
        long occupiedQty = activePieceCount(location.getLocationCode());
        if (capacityQty <= 0 || occupiedQty + addQty > capacityQty) {
            long availableQty = Math.max(0L, capacityQty - occupiedQty);
            throw invalidParamException(String.format("区域容量不足：剩余%s片，本次上架%s片，请更换区域", availableQty, addQty));
        }
    }

    private String normalizeFgLocationQualityScope(String qualityScope) {
        String normalized = StrUtil.trimToNull(qualityScope);
        if (normalized == null) {
            return FG_LOCATION_QUALITY_SCOPE_UNASSIGNED;
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!List.of(FG_LOCATION_QUALITY_SCOPE_QUALIFIED, FG_LOCATION_QUALITY_SCOPE_QUARANTINE,
                FG_LOCATION_QUALITY_SCOPE_UNASSIGNED).contains(normalized)) {
            throw invalidParamException("包装成品库库位质量用途仅支持 QUALIFIED、QUARANTINE 或 UNASSIGNED");
        }
        return normalized;
    }

    private void assertInboundLocationQualityScope(HcLocationDO location, String packageQualityStatus) {
        String requiredScope = INSPECTION_RESULT_NG.equalsIgnoreCase(packageQualityStatus)
                ? FG_LOCATION_QUALITY_SCOPE_QUARANTINE : FG_LOCATION_QUALITY_SCOPE_QUALIFIED;
        String actualScope = normalizeFgLocationQualityScope(location.getQualityScope());
        if (requiredScope.equals(actualScope)) {
            return;
        }
        String packageQualityText = INSPECTION_RESULT_NG.equalsIgnoreCase(packageQualityStatus) ? "不合格" : "合格";
        String requiredScopeText = FG_LOCATION_QUALITY_SCOPE_QUARANTINE.equals(requiredScope)
                ? "不合格品隔离库位" : "合格品库位";
        throw invalidParamException(packageQualityText + "包装只允许上架到" + requiredScopeText
                + "；当前库位质量用途为 " + actualScope);
    }

    private void refreshLocationOccupied(String locationCode) {
        if (StrUtil.isBlank(locationCode)) {
            return;
        }
        HcLocationDO location = hcLocationMapper.selectByLocationCode(locationCode);
        if (location == null || Boolean.TRUE.equals(location.getDeleted()) || !FG_LOCATION_SCENE.equals(location.getBizScene())) {
            return;
        }
        HcLocationDO update = new HcLocationDO();
        update.setId(location.getId());
        update.setOccupiedQty((int) activePieceCount(locationCode));
        hcLocationMapper.updateById(update);
    }

    private HcInnerPackUnitDO getInboundBox(Long id) {
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectById(id);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("包装盒不存在");
        }
        return box;
    }

    private HcFgOutboundBoxDO getOutboundBox(Long id) {
        HcFgOutboundBoxDO box = hcFgOutboundBoxMapper.selectById(id);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            throw invalidParamException("发货包装盒不存在");
        }
        return box;
    }

    private HcPackageAuxStockDO getPackageAuxStock(Long id) {
        HcPackageAuxStockDO stock = hcPackageAuxStockMapper.selectById(id);
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            throw invalidParamException("包装辅材边库批次不存在");
        }
        return stock;
    }

    private HcPackageAuxStockDO resolvePackageAuxStock(PackageAuxConsumeReqVO reqVO, String materialCode) {
        if (reqVO.getStockId() != null) {
            return hcPackageAuxStockMapper.selectById(reqVO.getStockId());
        }
        String batchNo = StrUtil.trimToEmpty(reqVO.getBatchNo());
        if (StrUtil.isNotBlank(batchNo)) {
            return hcPackageAuxStockMapper.selectByBatchNo(batchNo);
        }
        if (StrUtil.isBlank(materialCode)) {
            return null;
        }
        return hcPackageAuxStockMapper.selectFirstAvailableByMaterialCode(materialCode);
    }

    private void refreshFinishedStockShippingLockStatus(Long finishedStockId) {
        if (finishedStockId == null) {
            return;
        }
        HcFinishedStockDO stock = hcFinishedStockMapper.selectById(finishedStockId);
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            return;
        }
        int activeLockedQty = intValue(hcFgShippingNoticeItemMapper.selectActiveLockedQtyByFinishedStockId(finishedStockId))
                + intValue(hcFgShippingNoticePickItemMapper.selectActiveLockedQtyByFinishedStockId(finishedStockId));
        String nextStatus = activeLockedQty > 0 ? STATUS_OUTBOUND_LOCKED
                : StrUtil.isBlank(stock.getLocationCode()) ? STATUS_INBOUND_LOCKED : STATUS_AVAILABLE;
        if (Objects.equals(stock.getStockStatus(), nextStatus)) {
            return;
        }
        HcFinishedStockDO update = new HcFinishedStockDO();
        update.setId(finishedStockId);
        update.setStockStatus(nextStatus);
        hcFinishedStockMapper.updateById(update);
        recordFinishedStockHistory(stock, copyFinishedStockForHistory(stock, nextStatus),
                STATUS_OUTBOUND_LOCKED.equals(nextStatus) ? "FG_OUTBOUND_LOCK" : "FG_OUTBOUND_UNLOCK",
                LocalDateTime.now(), currentUserName(), "FG_SHIPPING_NOTICE", null, null, "发货锁定状态刷新");
    }

    private HcFinishedStockDO copyFinishedStockForHistory(HcFinishedStockDO stock, String afterStockStatus) {
        if (stock == null) {
            return null;
        }
        HcFinishedStockDO copied = BeanUtil.copyProperties(stock, HcFinishedStockDO.class);
        copied.setStockStatus(afterStockStatus);
        return copied;
    }

    private HcFinishedStockDO copyFinishedStockOffShelf(HcFinishedStockDO stock, String afterStockStatus) {
        HcFinishedStockDO copied = copyFinishedStockForHistory(stock, afterStockStatus);
        if (copied == null) {
            return null;
        }
        copied.setWarehouseCode("");
        copied.setWarehouseName("");
        copied.setLocationCode("");
        copied.setLocationName("");
        return copied;
    }

    private void recordFinishedStockHistory(HcFinishedStockDO beforeStock,
                                            HcFinishedStockDO afterStock,
                                            String txnType,
                                            LocalDateTime txnTime,
                                            String operatorName,
                                            String refDocType,
                                            Long refDocId,
                                            String refDocNo,
                                            String remark) {
        if ("FG_SHIP".equals(txnType) || "FG_OUTBOUND_LOCK".equals(txnType)) {
            assertStockCoaReleased(beforeStock == null ? afterStock : beforeStock);
        }
        HcFinishedStockDO snapshot = afterStock == null ? beforeStock : afterStock;
        if (snapshot == null) {
            return;
        }
        LocalDateTime effectiveTime = txnTime == null ? LocalDateTime.now(BUSINESS_ZONE) : txnTime;
        Long finishedStockId = firstNonNullId(beforeStock, afterStock);
        String txnNoSuffix = finishedStockId == null
                ? UUID.randomUUID().toString().replace("-", "")
                : String.valueOf(finishedStockId);
        hcFinishedStockTxnLogMapper.insert(HcFinishedStockTxnLogDO.builder()
                .tenantId(snapshot.getTenantId())
                .finishedStockId(finishedStockId)
                .txnNo("FGH-" + effectiveTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                        + "-" + txnNoSuffix)
                .txnType(txnType)
                .txnTime(effectiveTime)
                .stockNo(snapshot.getStockNo())
                .outerBoxNo(snapshot.getOuterBoxNo())
                .innerUnitNo(snapshot.getInnerUnitNo())
                .sliceBatchNo(snapshot.getSliceBatchNo())
                .materialCode(snapshot.getMaterialCode())
                .materialName(snapshot.getMaterialName())
                .modelCode(snapshot.getModelCode())
                .batchNo(snapshot.getBatchNo())
                .qty(snapshot.getQty())
                .qualityStatus(snapshot.getQualityStatus())
                .warehouseCode(snapshot.getWarehouseCode())
                .warehouseName(snapshot.getWarehouseName())
                .locationCode(snapshot.getLocationCode())
                .locationName(snapshot.getLocationName())
                .beforeStockStatus(beforeStock == null ? null : beforeStock.getStockStatus())
                .afterStockStatus(afterStock == null ? null : afterStock.getStockStatus())
                .refDocType(refDocType)
                .refDocId(refDocId)
                .refDocNo(refDocNo)
                .operatorName(operatorName)
                .remark(StrUtil.trimToNull(remark))
                .build());
    }

    private Long firstNonNullId(HcFinishedStockDO beforeStock, HcFinishedStockDO afterStock) {
        return beforeStock == null ? afterStock == null ? null : afterStock.getId() : beforeStock.getId();
    }

    private HcFgShippingNoticeDO getShippingNoticeOrThrow(Long id) {
        HcFgShippingNoticeDO notice = hcFgShippingNoticeMapper.selectById(id);
        if (notice == null || Boolean.TRUE.equals(notice.getDeleted())) {
            throw invalidParamException("发货需求单不存在");
        }
        return notice;
    }

    private HcPlanOrderDO validatePlan(Long planId) {
        HcPlanOrderDO planOrder = hcPlanOrderMapper.selectById(planId);
        if (planOrder == null || Boolean.TRUE.equals(planOrder.getDeleted())) {
            throw invalidParamException("生产计划不存在");
        }
        return planOrder;
    }

    private HcPlanOrderOperationDO validateOperation(HcPlanOrderDO planOrder, Long planOperationId) {
        HcPlanOrderOperationDO operation = hcPlanOrderOperationMapper.selectById(planOperationId);
        if (operation == null || Boolean.TRUE.equals(operation.getDeleted()) || !Objects.equals(operation.getPlanId(), planOrder.getId())) {
            throw invalidParamException("包装工序不存在");
        }
        return operation;
    }

    private int calculateFgLocationGridNo(int rackNo, int layerNo, int areaNo) {
        return rackNo * 1_000_000 + layerNo * 1_000 + areaNo;
    }

    private String buildFgLocationCode(String warehouseCode, int rackNo, int layerNo, int areaNo) {
        return String.format("%s-%d-L%d-%d", warehouseCode, rackNo, layerNo, areaNo);
    }

    private String buildFgLocationName(HcFgRackDO rack, HcFgLayerDO layer, int areaNo) {
        return String.format("%s-%s-%d区", rack.getRackName(), layer.getLayerName(), areaNo);
    }

    private String buildFgLocationPositionDesc(HcFgRackDO rack, HcFgLayerDO layer, int areaNo, int capacityQty) {
        return String.format("%s(%d#)-%s(L%d)-%d区；区域容量%d片",
                rack.getRackName(), rack.getRackNo(), layer.getLayerName(), layer.getLayerNo(), areaNo, capacityQty);
    }

    private int validateFgLocationCodePart(Integer value, String fieldName) {
        if (value == null || value < 1 || value > FG_LOCATION_CODE_MAX_PART) {
            throw invalidParamException(fieldName + "仅支持1至" + FG_LOCATION_CODE_MAX_PART);
        }
        return value;
    }

    private String validateFgLocationStatus(String requestedStatus, String oldStatus) {
        String status = firstNotBlank(StrUtil.trimToNull(requestedStatus), oldStatus, DEFAULT_LOCATION_STATUS);
        if (!DEFAULT_LOCATION_STATUS.equals(status) && !"停用".equals(status)) {
            throw invalidParamException("状态仅支持启用或停用");
        }
        return status;
    }

    private int normalizeSortNo(Integer sortNo, int defaultValue) {
        if (sortNo == null) {
            return defaultValue;
        }
        if (sortNo < 0) {
            throw invalidParamException("排序号不能小于0");
        }
        return sortNo;
    }

    private Integer parseFgLocationCodePart(String locationCode, int group) {
        Matcher matcher = FG_LOCATION_CODE_PATTERN.matcher(StrUtil.trimToEmpty(locationCode));
        return matcher.matches() ? Integer.valueOf(matcher.group(group)) : null;
    }

    private int positive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw invalidParamException(message);
        }
        return value;
    }

    private int positiveOrZero(Integer value, String message) {
        if (value == null || value < 0) {
            throw invalidParamException(message);
        }
        return value;
    }

    private BigDecimal positiveDecimal(BigDecimal value, String message) {
        BigDecimal normalized = zeroIfNull(value);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw invalidParamException(message);
        }
        return normalized;
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int intValue(Integer value) {
        return value == null ? 0 : value;
    }

    private long activePieceCount(String locationCode) {
        if (StrUtil.isBlank(locationCode)) {
            return 0L;
        }
        Long count = hcFinishedStockMapper.selectActivePieceQtyByLocationCode(locationCode);
        return count == null ? 0L : count;
    }

    private long activePackageCount(String locationCode) {
        if (StrUtil.isBlank(locationCode)) {
            return 0L;
        }
        Long count = hcFinishedStockMapper.selectActivePackageCountByLocationCode(locationCode);
        return count == null ? 0L : count;
    }

    private long stockPieceQty(HcFinishedStockDO stock) {
        if (stock == null) {
            return 0L;
        }
        return Math.max(1, intValue(stock.getQty()));
    }

    private String nextNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }

    private List<String> nextInboundPackageNos(LocalDate packageDate, int packageCount) {
        if (packageCount <= 0) {
            throw invalidParamException("请选择待包装片");
        }
        String prefix = packageDate.format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        int serial = intValue(hcInnerPackUnitMapper.selectMaxPackageNoSerial(prefix)) + 1;
        List<String> packageNos = new ArrayList<>();
        for (int index = 0; index < packageCount; index++) {
            String packageNo = prefix + String.format("%02d", serial + index);
            while (hcInnerPackUnitMapper.selectByUnitNo(packageNo) != null) {
                serial++;
                packageNo = prefix + String.format("%02d", serial + index);
            }
            packageNos.add(packageNo);
        }
        return packageNos;
    }

    private String nextShippingNoticeNo() {
        String prefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + "-";
        int serial = intValue(hcFgShippingNoticeMapper.selectMaxNoticeNoSerial(prefix)) + 1;
        String noticeNo = prefix + String.format("%03d", serial);
        while (hcFgShippingNoticeMapper.selectByNoticeNo(noticeNo) != null) {
            serial++;
            noticeNo = prefix + String.format("%03d", serial);
        }
        return noticeNo;
    }

    private Integer firstPositive(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private String resolvePickSourceType(ShippingNoticePickItemReqVO pickItem) {
        if (pickItem == null) {
            throw invalidParamException("配货明细不能为空");
        }
        String sourceType = StrUtil.blankToDefault(StrUtil.trimToNull(pickItem.getCandidateType()),
                PICK_SOURCE_WAREHOUSE_STOCK).toUpperCase(Locale.ROOT);
        boolean hasStockSource = pickItem.getStockId() != null;
        boolean hasInnerPackSource = pickItem.getSourceInnerPackItemId() != null;
        boolean hasCutRoundSource = pickItem.getSourceCutRoundReportId() != null;
        if (PICK_SOURCE_WAREHOUSE_STOCK.equals(sourceType)) {
            if (!hasStockSource || hasInnerPackSource || hasCutRoundSource) {
                throw invalidParamException("普通库存配货必须且只能提供成品库存ID");
            }
            return sourceType;
        }
        if (PICK_SOURCE_PACKAGING_DIRECT.equals(sourceType)) {
            if (hasStockSource || hasInnerPackSource == hasCutRoundSource) {
                throw invalidParamException("包装直发必须且只能提供来源内包装明细ID或来源裁切报工ID之一");
            }
            return sourceType;
        }
        throw invalidParamException("不支持的配货候选来源：" + sourceType);
    }

    private String resolvePickCandidateKey(ShippingNoticePickItemReqVO pickItem, String pickSourceType) {
        if (PICK_SOURCE_WAREHOUSE_STOCK.equals(pickSourceType)) {
            return PICK_SOURCE_WAREHOUSE_STOCK + ":" + pickItem.getStockId();
        }
        if (pickItem.getSourceCutRoundReportId() != null) {
            return buildCutRoundDirectCandidateKey(pickItem.getSourceCutRoundReportId());
        }
        return buildInnerPackDirectCandidateKey(pickItem.getSourceInnerPackItemId());
    }

    private boolean matchesSubmittedCandidateKey(ShippingNoticePickItemReqVO pickItem, String canonicalCandidateKey) {
        String submittedCandidateKey = StrUtil.trim(pickItem.getCandidateKey());
        if (canonicalCandidateKey.equalsIgnoreCase(submittedCandidateKey)) {
            return true;
        }
        return pickItem.getSourceInnerPackItemId() != null
                && (PICK_SOURCE_PACKAGING_DIRECT + ":" + pickItem.getSourceInnerPackItemId())
                .equalsIgnoreCase(submittedCandidateKey);
    }

    private String buildInnerPackDirectCandidateKey(Long sourceInnerPackItemId) {
        return PICK_SOURCE_PACKAGING_DIRECT + ":INNER_PACK:" + sourceInnerPackItemId;
    }

    private String buildCutRoundDirectCandidateKey(Long sourceCutRoundReportId) {
        return PICK_SOURCE_PACKAGING_DIRECT + ":CUT_ROUND:" + sourceCutRoundReportId;
    }

    private HcFinishedStockDO lockWarehousePickStock(Long stockId, Set<Long> selectedStockIds) {
        HcFinishedStockDO stock = hcFinishedStockMapper.selectByIdForUpdate(stockId);
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())) {
            throw invalidParamException("选择的成品库存不存在");
        }
        assertStockCoaReleased(stock);
        if (!STATUS_AVAILABLE.equals(stock.getStockStatus())) {
            throw invalidParamException("只允许选择未发货的可用库存："
                    + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
        }
        if (!INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.trimToEmpty(stock.getQualityStatus()))) {
            throw invalidParamException("只允许选择合格成品库存进行配货："
                    + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
        }
        if (StrUtil.isBlank(stock.getInnerUnitNo())) {
            throw invalidParamException("选择的成品库存未关联包装单，不能按新流程配货下架："
                    + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
        }
        HcInnerPackUnitDO innerPackUnit = hcInnerPackUnitMapper.selectByUnitNoForUpdate(stock.getInnerUnitNo());
        if (innerPackUnit == null || Boolean.TRUE.equals(innerPackUnit.getDeleted())) {
            throw invalidParamException("选择的成品库存所属包装单不存在：" + stock.getInnerUnitNo());
        }
        if (!selectedStockIds.add(stock.getId())) {
            throw invalidParamException("配货片号重复：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
        }
        return stock;
    }

    private Map<String, DirectPickSource> prepareDirectPickSources(List<ShippingNoticePickItemReqVO> pickItems,
                                                                     HcFgShippingNoticeDO notice,
                                                                     String operatorName,
                                                                     LocalDateTime now) {
        Map<Long, HcInnerPackUnitItemDO> sourceSnapshotMap = new LinkedHashMap<>();
        Map<String, Set<Long>> sourceIdsByInnerUnitNo = new LinkedHashMap<>();
        Set<Long> cutRoundReportIds = new HashSet<>();
        for (ShippingNoticePickItemReqVO pickItem : pickItems) {
            if (!PICK_SOURCE_PACKAGING_DIRECT.equals(resolvePickSourceType(pickItem))) {
                continue;
            }
            if (pickItem.getSourceCutRoundReportId() != null) {
                if (!cutRoundReportIds.add(pickItem.getSourceCutRoundReportId())) {
                    throw invalidParamException("包装直发片号重复：" + pickItem.getSourceCutRoundReportId());
                }
                continue;
            }
            Long sourceItemId = pickItem.getSourceInnerPackItemId();
            if (sourceSnapshotMap.containsKey(sourceItemId)) {
                throw invalidParamException("包装直发片号重复：" + sourceItemId);
            }
            HcInnerPackUnitItemDO sourceItem = hcInnerPackUnitItemMapper.selectById(sourceItemId);
            if (sourceItem == null || Boolean.TRUE.equals(sourceItem.getDeleted())) {
                throw invalidParamException("包装直发来源明细不存在，请刷新后重试");
            }
            if (StrUtil.isBlank(sourceItem.getInnerUnitNo())) {
                throw invalidParamException("包装直发来源明细未关联包装单：" + sourceItemId);
            }
            sourceSnapshotMap.put(sourceItemId, sourceItem);
            sourceIdsByInnerUnitNo.computeIfAbsent(sourceItem.getInnerUnitNo(), key -> new HashSet<>()).add(sourceItemId);
        }
        if (sourceSnapshotMap.isEmpty() && cutRoundReportIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, DirectPickSource> result = new LinkedHashMap<>();
        List<String> innerUnitNos = sourceIdsByInnerUnitNo.keySet().stream().sorted().toList();
        for (String innerUnitNo : innerUnitNos) {
            HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(innerUnitNo);
            if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
                throw invalidParamException("包装直发来源包装单不存在：" + innerUnitNo);
            }
            if (!List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED)
                    .contains(StrUtil.blankToDefault(box.getUnitStatus(), ""))
                    || StrUtil.isNotBlank(box.getLocationCode())) {
                throw invalidParamException("只有未上架的待上架包装单允许包装直发：" + innerUnitNo);
            }
            List<HcInnerPackUnitItemDO> packageItems = hcInnerPackUnitItemMapper
                    .selectListByInnerUnitIdForUpdate(box.getId());
            Map<Long, HcInnerPackUnitItemDO> packageItemMap = new LinkedHashMap<>();
            packageItems.forEach(item -> packageItemMap.put(item.getId(), item));
            Set<Long> selectedSourceItemIds = sourceIdsByInnerUnitNo.get(innerUnitNo);
            for (HcInnerPackUnitItemDO packageItem : packageItems) {
                HcFgShippingNoticePickItemDO activePick = hcFgShippingNoticePickItemMapper
                        .selectActiveBySourceInnerPackItemId(packageItem.getId());
                if (activePick != null) {
                    throw invalidParamException("包装单已有片号被发货配货占用："
                            + firstNotBlank(packageItem.getSliceBatchNo(), packageItem.getProductionBatchNo()));
                }
            }
            List<HcFinishedStockDO> packageStocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(innerUnitNo);
            if (packageStocks.stream().anyMatch(stock -> !STATUS_INBOUND_LOCKED.equals(stock.getStockStatus())
                    || StrUtil.isNotBlank(stock.getLocationCode()))) {
                throw invalidParamException("包装单已有片进入上架或发货流程，不能包装直发：" + innerUnitNo);
            }

            for (Long sourceItemId : selectedSourceItemIds) {
                HcInnerPackUnitItemDO sourceItem = packageItemMap.get(sourceItemId);
                if (sourceItem == null || !Objects.equals(sourceItem.getInnerUnitId(), box.getId())) {
                    throw invalidParamException("包装直发来源明细已变化，请刷新后重试：" + sourceItemId);
                }
                if (!INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.blankToDefault(sourceItem.getQualityStatus(), ""))) {
                    throw invalidParamException("只有包装检验合格片允许直发："
                            + firstNotBlank(sourceItem.getSliceBatchNo(), sourceItem.getProductionBatchNo()));
                }
                String sliceBatchNo = firstNotBlank(sourceItem.getSliceBatchNo(), sourceItem.getProductionBatchNo());
                if (StrUtil.isBlank(sliceBatchNo)) {
                    throw invalidParamException("包装直发来源片号不能为空：" + sourceItemId);
                }
                List<HcFinishedStockDO> matchingStocks = hcFinishedStockMapper
                        .selectListBySliceBatchNoForUpdate(sliceBatchNo);
                if (matchingStocks.size() > 1) {
                    throw invalidParamException("片号存在重复库存台账，不能包装直发：" + sliceBatchNo);
                }
                HcFinishedStockDO stock = matchingStocks.isEmpty() ? null : matchingStocks.get(0);
                if (stock == null) {
                    stock = HcFinishedStockDO.builder()
                            .tenantId(box.getTenantId())
                            .stockNo(nextNo("FGS"))
                            .outerBoxNo(box.getInnerUnitNo())
                            .innerUnitNo(box.getInnerUnitNo())
                            .sliceBatchNo(sliceBatchNo)
                            .materialCode(box.getMaterialCode())
                            .materialName(box.getMaterialName())
                            .modelCode(box.getModelCode())
                            .batchNo(box.getBatchNo())
                            .productSize(box.getProductSize())
                            .qty(1)
                            .qualityStatus(sourceItem.getQualityStatus())
                            .stockStatus(STATUS_OUTBOUND_LOCKED)
                            .build();
                    hcFinishedStockMapper.insert(stock);
                synchronizeStockCoaFreeze(stock);
                    recordFinishedStockHistory(null, stock, "FG_OUTBOUND_LOCK", now, operatorName,
                            "FG_SHIPPING_NOTICE", notice.getId(), notice.getNoticeNo(), "包装直发配货");
                } else {
                    if (!Objects.equals(stock.getInnerUnitNo(), box.getInnerUnitNo())
                            || !STATUS_INBOUND_LOCKED.equals(stock.getStockStatus())
                            || StrUtil.isNotBlank(stock.getLocationCode())) {
                        throw invalidParamException("片号已有不可直发的库存台账：" + sliceBatchNo);
                    }
                    if (!INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.blankToDefault(stock.getQualityStatus(), ""))) {
                        throw invalidParamException("只有检验合格库存允许包装直发：" + sliceBatchNo);
                    }
                    HcFgShippingNoticeItemDO activeNoticeItem = hcFgShippingNoticeItemMapper
                            .selectActiveByFinishedStockId(stock.getId());
                    HcFgShippingNoticePickItemDO activePick = hcFgShippingNoticePickItemMapper
                            .selectActiveByFinishedStockId(stock.getId());
                    if (activeNoticeItem != null || activePick != null) {
                        throw invalidParamException("该片号已被其它发货需求单配货锁定：" + sliceBatchNo);
                    }
                    HcFinishedStockDO afterStock = copyFinishedStockOffShelf(stock, STATUS_OUTBOUND_LOCKED);
                    updateStockOffShelf(stock.getId(), STATUS_OUTBOUND_LOCKED);
                    recordFinishedStockHistory(stock, afterStock, "FG_OUTBOUND_LOCK", now, operatorName,
                            "FG_SHIPPING_NOTICE", notice.getId(), notice.getNoticeNo(), "包装直发配货");
                    stock.setWarehouseCode("");
                    stock.setWarehouseName("");
                    stock.setLocationCode("");
                    stock.setLocationName("");
                    stock.setStockStatus(STATUS_OUTBOUND_LOCKED);
                }
                String candidateKey = buildInnerPackDirectCandidateKey(sourceItemId);
                result.put(candidateKey, new DirectPickSource(box, sourceItem, null, stock));
            }
        }

        List<HcCutRoundReportDO> cutRoundReports = cutRoundReportIds.stream()
                .sorted()
                .map(hcCutRoundReportMapper::selectByIdForUpdate)
                .filter(Objects::nonNull)
                .toList();
        if (cutRoundReports.size() != cutRoundReportIds.size()) {
            throw invalidParamException("部分待包装直发片不存在，请刷新后重试");
        }
        Map<String, CoaInspectionMeta> coaInspectionMap = buildCoaInspectionMapForCutRoundReports(cutRoundReports);
        for (HcCutRoundReportDO report : cutRoundReports) {
            String sliceBatchNo = StrUtil.trimToNull(report.getProductionBatchNo());
            if (!List.of("CONFIRMED", "SUBMITTED")
                    .contains(StrUtil.blankToDefault(report.getReportStatus(), "").toUpperCase(Locale.ROOT))
                    || !INSPECTION_STATUS_COMPLETED.equalsIgnoreCase(
                    StrUtil.blankToDefault(report.getInspectionStatus(), ""))) {
                throw invalidParamException("只有已报工且检验完成的待包装片允许直发："
                        + firstNotBlank(sliceBatchNo, String.valueOf(report.getId())));
            }
            if (!INSPECTION_RESULT_OK.equals(resolvePackagingPieceQualityStatus(report, coaInspectionMap))) {
                throw invalidParamException("只有综合包装质量合格的待包装片允许直发："
                        + firstNotBlank(sliceBatchNo, String.valueOf(report.getId())));
            }
            if (StrUtil.isBlank(sliceBatchNo)) {
                throw invalidParamException("待包装直发来源片号不能为空：" + report.getId());
            }
            if (hcInnerPackUnitItemMapper.selectByCutRoundReportId(report.getId()) != null
                    || hcInnerPackUnitItemMapper.selectBySliceBatchNo(sliceBatchNo) != null) {
                throw invalidParamException("片号已生成包装单，请刷新后选择已包装直发候选：" + sliceBatchNo);
            }
            List<HcFinishedStockDO> matchingStocks = hcFinishedStockMapper.selectListBySliceBatchNoForUpdate(sliceBatchNo);
            if (!matchingStocks.isEmpty()) {
                throw invalidParamException("片号已存在库存台账，不能按待包装片直发：" + sliceBatchNo);
            }
            if (hcFgShippingNoticePickItemMapper.selectActiveBySourceCutRoundReportId(report.getId()) != null) {
                throw invalidParamException("该待包装片已被其它发货需求单配货锁定：" + sliceBatchNo);
            }
            HcPlanOrderDO planOrder = report.getPlanId() == null ? null : hcPlanOrderMapper.selectById(report.getPlanId());
            HcFinishedStockDO stock = HcFinishedStockDO.builder()
                    .tenantId(report.getTenantId())
                    .stockNo(nextNo("FGS"))
                    .outerBoxNo("")
                    .sliceBatchNo(sliceBatchNo)
                    .materialCode(report.getMaterialCode())
                    .materialName(report.getMaterialName())
                    .modelCode(report.getModelCode())
                    .batchNo(firstNotBlank(report.getParentProductionBatchNo(),
                            report.getSourceProductionBatchNo(), sliceBatchNo))
                    .productSize(planOrder == null ? null
                            : firstNotBlank(planOrder.getSizeName(), planOrder.getSizeSpec()))
                    .qty(1)
                    .qualityStatus(INSPECTION_RESULT_OK)
                    .stockStatus(STATUS_OUTBOUND_LOCKED)
                    .build();
            hcFinishedStockMapper.insert(stock);
                synchronizeStockCoaFreeze(stock);
            recordFinishedStockHistory(null, stock, "FG_OUTBOUND_LOCK", now, operatorName,
                    "FG_SHIPPING_NOTICE", notice.getId(), notice.getNoticeNo(), "待包装片直发配货");
            String candidateKey = buildCutRoundDirectCandidateKey(report.getId());
            result.put(candidateKey, new DirectPickSource(null, null, report, stock));
        }
        return result;
    }

    private void finalizeDirectPickedPackages(HcFgShippingNoticeDO notice,
                                              Map<String, DirectPickSource> directPickSourceMap,
                                              String operatorName,
                                              LocalDateTime now) {
        if (directPickSourceMap.isEmpty()) {
            return;
        }
        Map<String, Set<Long>> selectedItemIdsByInnerUnitNo = new LinkedHashMap<>();
        Map<String, HcInnerPackUnitDO> boxMap = new LinkedHashMap<>();
        directPickSourceMap.values().stream()
                .filter(source -> source.box() != null && source.packItem() != null)
                .forEach(source -> {
            String innerUnitNo = source.box().getInnerUnitNo();
            boxMap.put(innerUnitNo, source.box());
            selectedItemIdsByInnerUnitNo.computeIfAbsent(innerUnitNo, key -> new HashSet<>())
                    .add(source.packItem().getId());
        });
        String splitReturnReason = "发货包装直发拆包，未选片退回成品包装报工，需重新包装";
        for (String innerUnitNo : selectedItemIdsByInnerUnitNo.keySet().stream().sorted().toList()) {
            HcInnerPackUnitDO box = boxMap.get(innerUnitNo);
            Set<Long> selectedItemIds = selectedItemIdsByInnerUnitNo.get(innerUnitNo);
            List<HcInnerPackUnitItemDO> packageItems = hcInnerPackUnitItemMapper
                    .selectListByInnerUnitIdForUpdate(box.getId());
            List<HcFinishedStockDO> packageStocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(innerUnitNo);
            List<Long> returnedPackItemIds = new ArrayList<>();
            List<Long> returnedStockIds = new ArrayList<>();
            List<HcFinishedStockDO> returnedStocks = new ArrayList<>();
            for (HcInnerPackUnitItemDO packageItem : packageItems) {
                if (selectedItemIds.contains(packageItem.getId())) {
                    continue;
                }
                String sliceBatchNo = firstNotBlank(packageItem.getSliceBatchNo(), packageItem.getProductionBatchNo());
                HcFinishedStockDO stock = packageStocks.stream()
                        .filter(row -> Objects.equals(row.getSliceBatchNo(), sliceBatchNo))
                        .findFirst()
                        .orElse(null);
                if (stock != null && (!STATUS_INBOUND_LOCKED.equals(stock.getStockStatus())
                        || StrUtil.isNotBlank(stock.getLocationCode()))) {
                    throw invalidParamException("包装单未选片状态已变化，请刷新后重试：" + sliceBatchNo);
                }
                HcFinishedStockDO stockSnapshot = stock == null
                        ? buildDirectPickStockSnapshot(box, packageItem) : stock;
                appendReturnInspectionRemark(null, packageItem, stockSnapshot, notice, operatorName,
                        splitReturnReason, now, null);
                returnedPackItemIds.add(packageItem.getId());
                if (stock != null) {
                    returnedStockIds.add(stock.getId());
                    returnedStocks.add(stock);
                }
            }
            if (!returnedPackItemIds.isEmpty()) {
                hcInnerPackUnitItemMapper.deletePhysicallyByIds(returnedPackItemIds);
            }
            if (!returnedStockIds.isEmpty()) {
                returnedStocks.forEach(stock -> recordFinishedStockHistory(stock,
                        copyFinishedStockForHistory(stock, "RETURNED_FOR_REPACK"), "FG_PACKAGE_SPLIT_RETURN",
                        now, operatorName, "FG_SHIPPING_NOTICE", notice.getId(), notice.getNoticeNo(),
                        splitReturnReason));
                hcFinishedStockMapper.deletePhysicallyByIds(returnedStockIds);
            }
            updatePackageWaitingInbound(box);
            refreshReturnedInnerPackage(innerUnitNo);
        }
    }

    private HcFinishedStockDO buildDirectPickStockSnapshot(HcInnerPackUnitDO box,
                                                           HcInnerPackUnitItemDO packageItem) {
        return HcFinishedStockDO.builder()
                .stockNo(firstNotBlank(packageItem.getSliceBatchNo(), packageItem.getProductionBatchNo()))
                .outerBoxNo(box.getInnerUnitNo())
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo(firstNotBlank(packageItem.getSliceBatchNo(), packageItem.getProductionBatchNo()))
                .materialCode(box.getMaterialCode())
                .materialName(box.getMaterialName())
                .modelCode(box.getModelCode())
                .batchNo(box.getBatchNo())
                .productSize(box.getProductSize())
                .qty(1)
                .qualityStatus(packageItem.getQualityStatus())
                .stockStatus(STATUS_INBOUND_LOCKED)
                .build();
    }

    private record DirectPickSource(HcInnerPackUnitDO box,
                                    HcInnerPackUnitItemDO packItem,
                                    HcCutRoundReportDO cutRoundReport,
                                    HcFinishedStockDO stock) {
    }

    private void downShelfPickedPackages(HcFgShippingNoticeDO notice,
                                         List<HcFinishedStockDO> selectedStocks,
                                         Set<Long> selectedStockIds,
                                         String operatorName,
                                         LocalDateTime now) {
        Map<String, List<HcFinishedStockDO>> packageStockMap = new LinkedHashMap<>();
        for (HcFinishedStockDO stock : selectedStocks) {
            packageStockMap.computeIfAbsent(stock.getInnerUnitNo(), key -> new ArrayList<>()).add(stock);
        }
        String splitReturnReason = "发货配货拆箱剩余片退回成品包装报工，需重新包装上架";
        Set<String> touchedLocationCodes = new HashSet<>();
        for (String innerUnitNo : packageStockMap.keySet()) {
            HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(innerUnitNo);
            if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
                throw invalidParamException("包装单不存在：" + innerUnitNo);
            }
            if (StrUtil.isNotBlank(box.getLocationCode())) {
                touchedLocationCodes.add(box.getLocationCode());
            }
            List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(innerUnitNo);
            if (stocks.isEmpty()) {
                throw invalidParamException("包装单没有库存台账，不能配货下架：" + innerUnitNo);
            }
            List<Long> returnedStockIds = new ArrayList<>();
            List<Long> returnedPackItemIds = new ArrayList<>();
            List<HcFinishedStockDO> returnedStocks = new ArrayList<>();
            for (HcFinishedStockDO stock : stocks) {
                if (StrUtil.isNotBlank(stock.getLocationCode())) {
                    touchedLocationCodes.add(stock.getLocationCode());
                }
                String stockStatus = StrUtil.blankToDefault(stock.getStockStatus(), "");
                if (selectedStockIds.contains(stock.getId())) {
                    if (!STATUS_AVAILABLE.equals(stockStatus)) {
                        throw invalidParamException("配货片号状态已变化，请刷新后重试：" + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
                    }
                    HcFinishedStockDO afterStock = copyFinishedStockOffShelf(stock, STATUS_OUTBOUND_LOCKED);
                    updateStockOffShelf(stock.getId(), STATUS_OUTBOUND_LOCKED);
                    recordFinishedStockHistory(stock, afterStock, "FG_OUTBOUND_LOCK", now, operatorName,
                            "FG_SHIPPING_NOTICE", notice.getId(), notice.getNoticeNo(), "发货配货下架");
                    continue;
                }
                if (List.of(STATUS_AVAILABLE, STATUS_INBOUND_LOCKED).contains(stockStatus)) {
                    HcInnerPackUnitItemDO packItem = findInnerPackItem(stock);
                    appendReturnInspectionRemark(null, packItem, stock, notice, operatorName,
                            splitReturnReason, now, null);
                    if (packItem != null) {
                        returnedPackItemIds.add(packItem.getId());
                    }
                    returnedStockIds.add(stock.getId());
                    returnedStocks.add(stock);
                    continue;
                }
                if (STATUS_OUTBOUND_LOCKED.equals(stockStatus)) {
                    updateStockOffShelf(stock.getId(), STATUS_OUTBOUND_LOCKED);
                }
            }
            if (!returnedPackItemIds.isEmpty()) {
                hcInnerPackUnitItemMapper.deletePhysicallyByIds(returnedPackItemIds.stream().distinct().toList());
            }
            if (!returnedStockIds.isEmpty()) {
                returnedStocks.forEach(stock -> recordFinishedStockHistory(stock,
                        copyFinishedStockForHistory(stock, "RETURNED_FOR_REPACK"), "FG_PACKAGE_SPLIT_RETURN",
                        now, operatorName, "FG_SHIPPING_NOTICE", notice.getId(), notice.getNoticeNo(),
                        splitReturnReason));
                hcFinishedStockMapper.deletePhysicallyByIds(returnedStockIds.stream().distinct().toList());
            }
            updatePackageWaitingInbound(box);
            refreshReturnedInnerPackage(innerUnitNo);
        }
        touchedLocationCodes.forEach(this::refreshLocationOccupied);
    }

    private HcFgShippingNoticeItemDO getReturnNoticeItemOrThrow(HcFgShippingNoticeDO notice,
                                                                HcFgShippingNoticePickItemDO pickItem) {
        if (pickItem.getSourceNoticeItemId() == null) {
            return null;
        }
        HcFgShippingNoticeItemDO noticeItem = hcFgShippingNoticeItemMapper.selectById(pickItem.getSourceNoticeItemId());
        if (noticeItem == null || Boolean.TRUE.equals(noticeItem.getDeleted())
                || !Objects.equals(noticeItem.getNoticeId(), notice.getId())) {
            throw invalidParamException("退回配货来源明细不存在，请刷新后重试");
        }
        return noticeItem;
    }

    private void validateReturnNoticeItem(HcFgShippingNoticePickItemDO pickItem,
                                          HcFgShippingNoticeItemDO noticeItem) {
        if (noticeItem == null) {
            return;
        }
        String detailStatus = StrUtil.blankToDefault(noticeItem.getLockStatus(), "");
        if (List.of(NOTICE_STATUS_OUTBOUND, NOTICE_STATUS_SHIPPED, NOTICE_STATUS_CLOSED).contains(detailStatus)
                || noticeItem.getShippedTime() != null) {
            throw invalidParamException("已进入出库、已发货或已关闭的发货明细不能退回："
                    + firstNotBlank(pickItem.getActualSliceBatchNo(), noticeItem.getActualSliceBatchNo(), noticeItem.getPackageSliceNo()));
        }
        if (noticeItem.getOqcOrderId() != null
                || StrUtil.isNotBlank(noticeItem.getShippingQualityNo())
                || StrUtil.isNotBlank(noticeItem.getOqcStatus())) {
            throw invalidParamException("该片号已生成 OQC/出货检验单，不能退回；请先处理出货检验单："
                    + firstNotBlank(pickItem.getActualSliceBatchNo(), noticeItem.getActualSliceBatchNo(), noticeItem.getPackageSliceNo()));
        }
    }

    private HcInnerPackUnitItemDO findReturnInnerPackItem(HcFgShippingNoticePickItemDO pickItem, HcFinishedStockDO stock) {
        if (pickItem.getSourceInnerPackItemId() != null) {
            HcInnerPackUnitItemDO sourceItem = hcInnerPackUnitItemMapper.selectById(pickItem.getSourceInnerPackItemId());
            if (sourceItem != null && !Boolean.TRUE.equals(sourceItem.getDeleted())) {
                return sourceItem;
            }
        }
        String innerUnitNo = firstNotBlank(stock.getInnerUnitNo(), pickItem.getInnerUnitNo());
        String sliceBatchNo = firstNotBlank(stock.getSliceBatchNo(), pickItem.getActualSliceBatchNo(), pickItem.getSliceBatchNo());
        HcInnerPackUnitItemDO packItem = null;
        if (StrUtil.isNotBlank(innerUnitNo) && StrUtil.isNotBlank(sliceBatchNo)) {
            packItem = hcInnerPackUnitItemMapper.selectByInnerUnitNoAndSliceBatchNo(innerUnitNo, sliceBatchNo);
        }
        if (packItem == null && StrUtil.isNotBlank(sliceBatchNo)) {
            packItem = hcInnerPackUnitItemMapper.selectBySliceBatchNo(sliceBatchNo);
        }
        return packItem;
    }

    private HcInnerPackUnitItemDO findInnerPackItem(HcFinishedStockDO stock) {
        if (stock == null) {
            return null;
        }
        String innerUnitNo = stock.getInnerUnitNo();
        String sliceBatchNo = firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo());
        HcInnerPackUnitItemDO packItem = null;
        if (StrUtil.isNotBlank(innerUnitNo) && StrUtil.isNotBlank(sliceBatchNo)) {
            packItem = hcInnerPackUnitItemMapper.selectByInnerUnitNoAndSliceBatchNo(innerUnitNo, sliceBatchNo);
        }
        if (packItem == null && StrUtil.isNotBlank(sliceBatchNo)) {
            packItem = hcInnerPackUnitItemMapper.selectBySliceBatchNo(sliceBatchNo);
        }
        return packItem;
    }

    private void appendReturnInspectionRemark(Long sourceCutRoundReportId,
                                              HcInnerPackUnitItemDO packItem,
                                              HcFinishedStockDO stock,
                                              HcFgShippingNoticeDO notice,
                                              String operatorName,
                                              String returnReason,
                                              LocalDateTime now,
                                              String targetQualityStatus) {
        String sliceBatchNo = firstNotBlank(stock.getSliceBatchNo(), packItem == null ? null : packItem.getSliceBatchNo(),
                packItem == null ? null : packItem.getProductionBatchNo(), stock.getStockNo());
        HcCutRoundReportDO report = findReturnCutRoundReport(sourceCutRoundReportId, packItem, stock);
        if (report == null || Boolean.TRUE.equals(report.getDeleted())) {
            if (StrUtil.isNotBlank(targetQualityStatus)) {
                throw invalidParamException("退回片号未找到源裁切报工，不能回写待包装质量："
                        + firstNotBlank(sliceBatchNo, stock.getStockNo()));
            }
            return;
        }
        String returnRemark = "[发货退回 " + now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                + " " + operatorName + "] " + returnReason
                + "；发货需求单：" + firstNotBlank(notice.getNoticeNo(), String.valueOf(notice.getId()))
                + "；片号：" + firstNotBlank(sliceBatchNo, stock.getStockNo());
        HcCutRoundReportDO update = new HcCutRoundReportDO();
        update.setId(report.getId());
        if (StrUtil.isNotBlank(targetQualityStatus) && !QUALITY_FROZEN.equals(targetQualityStatus)) {
            update.setInspectionResult(targetQualityStatus);
        }
        update.setInspectionRemark(appendBusinessRemark(report.getInspectionRemark(), returnRemark));
        hcCutRoundReportMapper.updateById(update);
    }

    private HcCutRoundReportDO findReturnCutRoundReport(Long sourceCutRoundReportId,
                                                       HcInnerPackUnitItemDO packItem,
                                                       HcFinishedStockDO stock) {
        if (sourceCutRoundReportId != null) {
            HcCutRoundReportDO report = hcCutRoundReportMapper.selectById(sourceCutRoundReportId);
            if (report != null && !Boolean.TRUE.equals(report.getDeleted())) {
                return report;
            }
        }
        if (packItem != null && packItem.getSourceCutRoundReportId() != null) {
            HcCutRoundReportDO report = hcCutRoundReportMapper.selectById(packItem.getSourceCutRoundReportId());
            if (report != null && !Boolean.TRUE.equals(report.getDeleted())) {
                return report;
            }
        }
        List<String> batchNos = new ArrayList<>();
        addReturnBatchNo(batchNos, stock == null ? null : stock.getSliceBatchNo());
        addReturnBatchNo(batchNos, stock == null ? null : stock.getStockNo());
        addReturnBatchNo(batchNos, packItem == null ? null : packItem.getSliceBatchNo());
        addReturnBatchNo(batchNos, packItem == null ? null : packItem.getProductionBatchNo());
        for (String batchNo : batchNos) {
            HcCutRoundReportDO report = hcCutRoundReportMapper.selectByProductionBatchNo(batchNo);
            if (report != null && !Boolean.TRUE.equals(report.getDeleted())) {
                return report;
            }
            List<HcCutRoundReportDO> reports = hcCutRoundReportMapper.selectListByBatchNo(batchNo);
            if (!reports.isEmpty()) {
                return reports.get(0);
            }
        }
        return null;
    }

    /**
     * 判断一条库存历史是否仍对应可操作的手工成品出库事实。
     *
     * <p>历史台账是不可变审计记录，不能只根据流水类型开放操作；还需要确保该片、原内包装及
     * 包装明细仍处于本功能约定的实时状态，避免已经被其它流程处理的历史记录再次被退回。</p>
     */
    private boolean isManualOutboundRepackReturnable(HcFinishedStockTxnLogDO outboundTxn) {
        if (!isManualOutboundTxn(outboundTxn)
                || outboundTxn.getFinishedStockId() == null
                || StrUtil.isBlank(outboundTxn.getInnerUnitNo())) {
            return false;
        }
        HcFinishedStockDO stock = hcFinishedStockMapper.selectById(outboundTxn.getFinishedStockId());
        if (stock == null || Boolean.TRUE.equals(stock.getDeleted())
                || !STATUS_SHIPPED.equalsIgnoreCase(StrUtil.blankToDefault(stock.getStockStatus(), ""))
                || !StrUtil.equals(StrUtil.trim(outboundTxn.getInnerUnitNo()), StrUtil.trim(stock.getInnerUnitNo()))) {
            return false;
        }
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNo(outboundTxn.getInnerUnitNo());
        if (box == null || Boolean.TRUE.equals(box.getDeleted())
                || !STATUS_MANUAL_OUTBOUNDED.equalsIgnoreCase(StrUtil.blankToDefault(box.getUnitStatus(), ""))
                || (outboundTxn.getRefDocId() != null && !Objects.equals(outboundTxn.getRefDocId(), box.getId()))) {
            return false;
        }
        HcInnerPackUnitItemDO packItem = hcInnerPackUnitItemMapper.selectByInnerUnitNoAndSliceBatchNo(
                outboundTxn.getInnerUnitNo(), firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
        return packItem != null && matchesPackageItemStock(packItem, stock);
    }

    private boolean isManualOutboundTxn(HcFinishedStockTxnLogDO txn) {
        return txn != null && !Boolean.TRUE.equals(txn.getDeleted())
                && "FG_MANUAL_OUTBOUND".equalsIgnoreCase(StrUtil.blankToDefault(txn.getTxnType(), ""))
                && STATUS_SHIPPED.equalsIgnoreCase(StrUtil.blankToDefault(txn.getAfterStockStatus(), ""));
    }

    private boolean matchesPackageItemStock(HcInnerPackUnitItemDO packItem, HcFinishedStockDO stock) {
        if (packItem == null || stock == null) {
            return false;
        }
        String packageSliceBatchNo = firstNotBlank(packItem.getSliceBatchNo(), packItem.getProductionBatchNo());
        String stockSliceBatchNo = firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo());
        return StrUtil.isNotBlank(packageSliceBatchNo)
                && StrUtil.equals(StrUtil.trim(packageSliceBatchNo), StrUtil.trim(stockSliceBatchNo));
    }

    /**
     * 从现有内包装中剥离一片并恢复其包装来源，使其重新进入包装台的待包装数据集。
     *
     * <p>裁切来源只需移除库存、内包装明细即可重新满足待包装查询条件；历史补录来源还需显式
     * 解除片号与原内包装的关联，并回写为 {@code WAIT_PACKAGING}。</p>
     */
    private void restoreReturnedPieceToPackagingWait(HcInnerPackUnitItemDO packItem, HcFinishedStockDO stock) {
        restoreReturnedPieceToPackagingWait(packItem, stock, null, null, currentUserName());
    }

    private void restoreReturnedPieceToPackagingWait(HcInnerPackUnitItemDO packItem,
                                                       HcFinishedStockDO stock,
                                                       String targetQualityStatus,
                                                       String returnRemark,
                                                       String operatorName) {
        String sourceType = StrUtil.trimToEmpty(packItem.getSourceType()).toUpperCase(Locale.ROOT);
        // 兼容早期包装明细尚未落库 sourceType 的历史数据：有历史补录来源ID时按补录片处理，其余沿用裁切来源校验。
        if (StrUtil.isBlank(sourceType)) {
            sourceType = packItem.getSourceManualPieceId() == null ? SOURCE_CUT_ROUND_REPORT : SOURCE_MANUAL_HISTORY;
        }
        if (SOURCE_CUT_ROUND_REPORT.equals(sourceType)) {
            HcCutRoundReportDO report = findReturnCutRoundReport(packItem.getSourceCutRoundReportId(), packItem, stock);
            if (report == null || Boolean.TRUE.equals(report.getDeleted())) {
                throw invalidParamException("退回片号未找到有效裁切报工，不能转入待重新包装："
                        + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            return;
        }
        if (SOURCE_MANUAL_HISTORY.equals(sourceType)) {
            HcPackagingManualPieceDO manualPiece = hcPackagingManualPieceMapper
                    .selectByIdForUpdate(packItem.getSourceManualPieceId());
            if (manualPiece == null || Boolean.TRUE.equals(manualPiece.getDeleted())) {
                throw invalidParamException("退回片号未找到有效历史补录记录，不能转入待重新包装："
                        + firstNotBlank(stock.getSliceBatchNo(), stock.getStockNo()));
            }
            HcPackagingManualPieceDO update = new HcPackagingManualPieceDO();
            update.setId(manualPiece.getId());
            update.setRecordStatus(MANUAL_PIECE_WAIT_PACKAGING);
            update.setInnerUnitId(null);
            update.setInnerUnitNo(null);
            if (StrUtil.isNotBlank(targetQualityStatus) && !QUALITY_FROZEN.equals(targetQualityStatus)) {
                update.setInspectionResult(targetQualityStatus);
            }
            update.setRemark(appendBusinessRemark(manualPiece.getRemark(), returnRemark));
            hcPackagingManualPieceMapper.updateById(update);
            hcPackagingPieceEventLogService.recordManualPieceEvent(manualPiece, "PACKAGING_RETURN_TO_WAIT",
                    manualPiece.getRecordStatus(), MANUAL_PIECE_WAIT_PACKAGING, LocalDateTime.now(BUSINESS_ZONE),
                    firstNotBlank(operatorName, currentUserName()), stock.getStockNo(),
                    firstNotBlank(returnRemark, "成品出库退回待重新包装"));
            return;
        }
        throw invalidParamException("该片号来源类型不支持退回待重新包装：" + sourceType);
    }

    private void addReturnBatchNo(List<String> batchNos, String batchNo) {
        String normalized = StrUtil.trimToNull(batchNo);
        if (normalized != null && !batchNos.contains(normalized)) {
            batchNos.add(normalized);
        }
    }

    private void refreshReturnedInnerPackage(String innerUnitNo) {
        if (StrUtil.isBlank(innerUnitNo)) {
            return;
        }
        HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(innerUnitNo);
        if (box == null || Boolean.TRUE.equals(box.getDeleted())) {
            return;
        }
        List<HcInnerPackUnitItemDO> items = hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId());
        if (items.isEmpty()) {
            String oldLocationCode = box.getLocationCode();
            hcInnerPackUnitMapper.deletePhysicallyById(box.getId());
            refreshLocationOccupied(oldLocationCode);
            return;
        }
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setCurrentQty(items.size());
        if (!List.of(STATUS_PACKED, STATUS_INBOUND_LOCKED, STATUS_INBOUNDED)
                .contains(StrUtil.blankToDefault(box.getUnitStatus(), ""))) {
            update.setUnitStatus(STATUS_PACKED);
        }
        hcInnerPackUnitMapper.updateById(update);
    }

    private void updateStockOffShelf(Long stockId, String stockStatus) {
        HcFinishedStockDO update = new HcFinishedStockDO();
        update.setId(stockId);
        update.setWarehouseCode("");
        update.setWarehouseName("");
        update.setLocationCode("");
        update.setLocationName("");
        update.setStockStatus(stockStatus);
        hcFinishedStockMapper.updateById(update);
    }

    private void updatePackageWaitingInbound(HcInnerPackUnitDO box) {
        HcInnerPackUnitDO update = new HcInnerPackUnitDO();
        update.setId(box.getId());
        update.setWarehouseCode("");
        update.setWarehouseName("");
        update.setLocationCode("");
        update.setLocationName("");
        update.setUnitStatus(STATUS_INBOUND_LOCKED);
        hcInnerPackUnitMapper.updateById(update);
    }

    private void validatePickStockMatches(HcFgShippingNoticeItemDO noticeItem, HcFinishedStockDO stock) {
        String requiredModel = firstNotBlank(noticeItem.getInternalModelCode(), noticeItem.getCustomerModelCode(), noticeItem.getModelCode());
        if (StrUtil.isNotBlank(requiredModel) && StrUtil.isNotBlank(stock.getModelCode())
                && !requiredModel.equalsIgnoreCase(stock.getModelCode())) {
            throw invalidParamException("配货库存型号与客户要求内部型号不一致：" + stock.getSliceBatchNo());
        }
        String requiredPrefix = firstNotBlank(noticeItem.getInternalItemCode(), noticeItem.getCustomerSliceBatchNo());
        String stockSliceBatchNo = StrUtil.blankToDefault(stock.getSliceBatchNo(), "");
        if (StrUtil.isNotBlank(requiredPrefix) && !stockSliceBatchNo.toLowerCase().startsWith(requiredPrefix.toLowerCase())) {
            throw invalidParamException("配货库存片号不满足客户要求内部批号前缀：" + requiredPrefix);
        }
    }

    private void validatePickItemMatches(HcFgShippingNoticeItemDO noticeItem, HcFgShippingNoticePickItemDO pickItem) {
        String requiredModel = firstNotBlank(noticeItem.getInternalModelCode(), noticeItem.getCustomerModelCode(), noticeItem.getModelCode());
        String actualModel = firstNotBlank(pickItem.getModelCode(), pickItem.getInternalModelCode());
        if (StrUtil.isNotBlank(requiredModel) && StrUtil.isNotBlank(actualModel)
                && !requiredModel.equalsIgnoreCase(actualModel)) {
            throw invalidParamException("扫码片号型号与客户要求内部型号不一致：" + pickItem.getActualSliceBatchNo());
        }
        String requiredPrefix = firstNotBlank(noticeItem.getInternalItemCode(), noticeItem.getCustomerSliceBatchNo());
        String actualSliceBatchNo = StrUtil.blankToDefault(pickItem.getActualSliceBatchNo(), "");
        if (StrUtil.isNotBlank(requiredPrefix) && !actualSliceBatchNo.toLowerCase().startsWith(requiredPrefix.toLowerCase())) {
            throw invalidParamException("扫码片号不满足客户要求内部批号前缀：" + requiredPrefix);
        }
    }

    private Map<Long, Integer> buildNoticeItemPickedCountMap(Long noticeId) {
        Map<Long, Integer> countMap = new LinkedHashMap<>();
        hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(noticeId).forEach(item -> {
            if (item.getSourceNoticeItemId() != null
                    && !isInspectionNg(item.getShippingInspectionResult())) {
                countMap.merge(item.getSourceNoticeItemId(), 1, Integer::sum);
            }
        });
        return countMap;
    }

    private HcFgShippingNoticeItemDO findMatchedPickRequirement(List<HcFgShippingNoticeItemDO> noticeItems, HcFinishedStockDO stock,
                                                                Map<Long, Integer> noticeItemPickedCountMap) {
        if (noticeItems == null || noticeItems.isEmpty() || stock == null) {
            return null;
        }
        return noticeItems.stream()
                .filter(item -> matchesPickRequirement(item, stock))
                .filter(item -> getNoticeItemPickedCount(item, noticeItemPickedCountMap) < getNoticeItemRequiredQty(item))
                .findFirst()
                .orElse(null);
    }

    private int getNoticeItemPickedCount(HcFgShippingNoticeItemDO noticeItem, Map<Long, Integer> noticeItemPickedCountMap) {
        if (noticeItem == null || noticeItem.getId() == null || noticeItemPickedCountMap == null) {
            return 0;
        }
        return noticeItemPickedCountMap.getOrDefault(noticeItem.getId(), 0);
    }

    private int getNoticeItemRequiredQty(HcFgShippingNoticeItemDO noticeItem) {
        if (noticeItem == null) {
            return 1;
        }
        return Math.max(1, intValue(firstPositive(noticeItem.getLockedQty(), noticeItem.getActualShipQty(), noticeItem.getStockQty())));
    }

    private void validateShippingNoticeItemPickCapacity(HcFgShippingNoticeItemDO noticeItem,
                                                         Map<Long, Integer> noticeItemPickedCountMap) {
        int pickedQty = getNoticeItemPickedCount(noticeItem, noticeItemPickedCountMap);
        int requiredQty = getNoticeItemRequiredQty(noticeItem);
        if (pickedQty >= requiredQty) {
            throw invalidParamException("配货明细数量不能超过客户要求，当前 " + pickedQty + " / 要求 " + requiredQty);
        }
    }

    private void validateShippingPickQuantity(int pickedQty, int incomingQty, int requiredQty,
                                              boolean confirmPicked) {
        if (requiredQty <= 0) {
            throw invalidParamException("发货需求单未维护有效要求数量，不能配货");
        }
        int totalQty = pickedQty + incomingQty;
        if (totalQty > requiredQty) {
            throw invalidParamException("配货数量不能超过客户要求发货数量，当前 " + totalQty + " / 要求 " + requiredQty);
        }
        if (confirmPicked && totalQty != requiredQty) {
            throw invalidParamException("配货数量必须与客户要求发货数量一致，当前 " + totalQty + " / 要求 " + requiredQty);
        }
    }

    private boolean matchesPickRequirement(HcFgShippingNoticeItemDO noticeItem, HcFinishedStockDO stock) {
        String requiredModel = firstNotBlank(noticeItem.getInternalModelCode(), noticeItem.getCustomerModelCode(), noticeItem.getModelCode());
        if (StrUtil.isNotBlank(requiredModel) && StrUtil.isNotBlank(stock.getModelCode())
                && !requiredModel.equalsIgnoreCase(stock.getModelCode())) {
            return false;
        }
        String requiredPrefix = firstNotBlank(noticeItem.getInternalItemCode(), noticeItem.getCustomerSliceBatchNo());
        String stockSliceBatchNo = StrUtil.blankToDefault(stock.getSliceBatchNo(), "");
        return StrUtil.isBlank(requiredPrefix) || stockSliceBatchNo.toLowerCase().startsWith(requiredPrefix.toLowerCase());
    }

    private void refreshShippingNoticePickedStatus(Long noticeId, boolean confirmPicked) {
        HcFgShippingNoticeDO notice = getShippingNoticeOrThrow(noticeId);
        int pickedQty = countEffectiveShippingPickedQty(noticeId);
        HcFgShippingNoticeDO update = new HcFgShippingNoticeDO();
        update.setId(noticeId);
        update.setLockedQty(pickedQty);
        if (confirmPicked) {
            int requiredQty = intValue(firstPositive(notice.getRequiredShipQty(), notice.getNoticeQty()));
            validateShippingPickQuantity(pickedQty, 0, requiredQty, true);
            update.setNoticeStatus(NOTICE_STATUS_PICKED);
        }
        hcFgShippingNoticeMapper.updateById(update);
    }

    private void refreshShippingNoticeAfterPickReturn(Long noticeId) {
        int pickedQty = countEffectiveShippingPickedQty(noticeId);
        HcFgShippingNoticeDO update = new HcFgShippingNoticeDO();
        update.setId(noticeId);
        update.setLockedQty(pickedQty);
        update.setNoticeStatus(pickedQty > 0 ? NOTICE_STATUS_PICKED : NOTICE_STATUS_SUBMITTED);
        hcFgShippingNoticeMapper.updateById(update);
    }

    private int countEffectiveShippingPickedQty(Long noticeId) {
        if (noticeId == null) {
            return 0;
        }
        return (int) hcFgShippingNoticePickItemMapper.selectActiveListByNoticeId(noticeId).stream()
                .filter(this::isEffectiveShippingPickedItem)
                .count();
    }

    private boolean isEffectiveShippingPickedItem(HcFgShippingNoticePickItemDO item) {
        return item != null
                && item.getFinishedStockId() != null
                && !isInspectionNg(item.getShippingInspectionResult());
    }

    private String appendBusinessRemark(String oldRemark, String appendRemark) {
        String append = StrUtil.trimToNull(appendRemark);
        if (StrUtil.isBlank(append)) {
            return StrUtil.trimToNull(oldRemark);
        }
        String old = StrUtil.trimToNull(oldRemark);
        return StrUtil.isBlank(old) ? append : old + "；" + append;
    }

    private static class CoaInspectionMeta {
        private String result = COA_RESULT_UNKNOWN;
        private String status;
        private String faiNo;
        private String sampleBatchNo;
        private String ngReason;
        private boolean latestFilled;

        private void accept(QmsFaiOrderDO faiOrder) {
            if (faiOrder == null) {
                return;
            }
            if (!latestFilled) {
                fillFrom(faiOrder, classifyCoaInspectionResult(faiOrder));
                latestFilled = true;
            }
        }

        private boolean isReleased() {
            return FAI_STATUS_COMPLETED.equalsIgnoreCase(StrUtil.trimToEmpty(status))
                    && INSPECTION_RESULT_OK.equalsIgnoreCase(StrUtil.trimToEmpty(result));
        }

        private void fillFrom(QmsFaiOrderDO faiOrder, String result) {
            this.result = result;
            this.status = faiOrder.getStatus();
            this.faiNo = faiOrder.getFaiNo();
            this.sampleBatchNo = faiOrder.getProductBatchNo();
            this.ngReason = StrUtil.isNotBlank(faiOrder.getLastReturnReason())
                    ? faiOrder.getLastReturnReason()
                    : faiOrder.getRemark();
        }

    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncCoaFreezeForFai(Long faiId) {
        QmsFaiOrderDO fai = faiId == null ? null : qmsFaiOrderMapper.selectById(faiId);
        if (fai != null && ("PACKAGING_COA".equals(fai.getSourceModule())
                || (SOURCE_MENU_CODE_ADHESIVE2.equals(fai.getSourceModule())
                && StrUtil.contains(fai.getSourceReportNo(), "-COA-")))) {
            syncCoaFreezeForSegment(fai.getProductBatchNo());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncCoaFreezeForSegment(String segmentBatchNo) {
        String segment = normalizeCoaSegmentBatchNo(segmentBatchNo);
        if (StrUtil.isBlank(segment)) return;
        Map<String, CoaInspectionMeta> coa = buildCoaInspectionMap(Set.of(segment));
        CoaInspectionMeta meta = coa.get(segment);
        boolean frozen = meta != null && !meta.isReleased();
        recordWaitingCoaTransitions(segment);
        // 先按段号缩小范围，再核对真实来源段，不能用前缀直接认定归属。
        List<HcInnerPackUnitItemDO> items = hcInnerPackUnitItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HcInnerPackUnitItemDO>()
                        .likeRight(HcInnerPackUnitItemDO::getSliceBatchNo, segment));
        for (HcInnerPackUnitItemDO item : items) {
            HcInnerPackUnitDO box = hcInnerPackUnitMapper.selectByUnitNoForUpdate(item.getInnerUnitNo());
            if (box == null || List.of("MANUAL_OUTBOUNDED", STATUS_SHIPPED).contains(
                    StrUtil.blankToDefault(box.getUnitStatus(), ""))) continue;
            HcFinishedStockDO stock = hcFinishedStockMapper.selectBySliceBatchNo(item.getSliceBatchNo());
            if (stock != null && STATUS_SHIPPED.equals(stock.getStockStatus())) continue;
            String actualSegment = resolveCoaSegmentForSlice(item.getSliceBatchNo());
            if (!segment.equals(actualSegment)) continue;
            String oldQuality = item.getQualityStatus();
            // 非 COA 的 NG 不允许由 COA OK 覆盖。
            String quality = INSPECTION_RESULT_NG.equalsIgnoreCase(oldQuality)
                    ? INSPECTION_RESULT_NG : frozen ? QUALITY_FROZEN : INSPECTION_RESULT_OK;
            if (!Objects.equals(oldQuality, quality)) {
                HcInnerPackUnitItemDO update = new HcInnerPackUnitItemDO();
                update.setId(item.getId());
                update.setQualityStatus(quality);
                hcInnerPackUnitItemMapper.updateById(update);
                coaPieceEventMapper.insert(HcPackagingPieceEventLogDO.builder()
                        .tenantId(item.getTenantId()).eventNo("COAF-" + UUID.randomUUID())
                        .eventType(frozen ? "PACKAGING_COA_FREEZE" : "PACKAGING_COA_UNFREEZE")
                        .eventTime(LocalDateTime.now(BUSINESS_ZONE)).sourceType(item.getSourceType())
                        .sourceRecordId(item.getSourceCutRoundReportId() == null ? item.getSourceManualPieceId() : item.getSourceCutRoundReportId())
                        .sliceBatchNo(item.getSliceBatchNo()).segmentBatchNo(segment)
                        .qualityStatus(quality).beforeStatus(oldQuality).afterStatus(quality)
                        .coaResult(meta == null ? "UNKNOWN" : meta.result).ngRelated(false)
                        .innerUnitNo(item.getInnerUnitNo()).refDocNo(meta == null ? null : meta.faiNo)
                        .operatorName(currentUserName()).remark("COA变化同步，保留其它质量限制").build());
            }
        }
        List<HcFinishedStockDO> stocks = hcFinishedStockMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HcFinishedStockDO>()
                        .likeRight(HcFinishedStockDO::getSliceBatchNo, segment)
                        .ne(HcFinishedStockDO::getStockStatus, STATUS_SHIPPED)
                        .orderByAsc(HcFinishedStockDO::getId));
        for (HcFinishedStockDO row : stocks) {
            if (!segment.equals(resolveCoaSegmentForSlice(row.getSliceBatchNo()))) continue;
            HcFinishedStockDO locked = hcFinishedStockMapper.selectByIdForUpdate(row.getId());
            if (locked != null && !STATUS_SHIPPED.equals(locked.getStockStatus())) {
                applyStockCoaFreeze(locked, meta);
            }
        }
    }

    private void recordWaitingCoaTransitions(String segment) {
        InspectionSlicePageReqVO query = new InspectionSlicePageReqVO();
        query.setSegmentBatchNo(segment);
        for (InspectionSliceRespVO row : listInboundWaitInspectionSlicesForGrouping(query)) {
            String slice = firstNotBlank(row.getSliceBatchNo(), row.getProductionBatchNo());
            List<HcPackagingPieceEventLogDO> previous = coaPieceEventMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HcPackagingPieceEventLogDO>()
                            .eq(HcPackagingPieceEventLogDO::getSliceBatchNo, slice)
                            .in(HcPackagingPieceEventLogDO::getEventType, "PACKAGING_COA_FREEZE", "PACKAGING_COA_UNFREEZE")
                            .orderByDesc(HcPackagingPieceEventLogDO::getId).last("LIMIT 1"));
            String before = previous.isEmpty() ? "UNKNOWN" : previous.get(0).getAfterStatus();
            String after = row.getPackagingQualityStatus();
            if (INSPECTION_RESULT_NG.equals(after)) continue;
            if (Objects.equals(before, after)) continue;
            Long sourceRecordId = resolveWaitingCoaSourceRecordId(row);
            coaPieceEventMapper.insert(HcPackagingPieceEventLogDO.builder()
                    .tenantId(currentTenantId()).eventNo("COAF-" + UUID.randomUUID())
                    .eventType(QUALITY_FROZEN.equals(after) ? "PACKAGING_COA_FREEZE" : "PACKAGING_COA_UNFREEZE")
                    .eventTime(LocalDateTime.now(BUSINESS_ZONE)).sourceType(row.getSourceType())
                    .sourceRecordId(sourceRecordId).sliceBatchNo(slice).segmentBatchNo(segment).qualityStatus(after)
                    .beforeStatus(before).afterStatus(after).fqcResult(row.getInspectionResult())
                    .coaResult(row.getCoaInspectionResult()).ngRelated(false).refDocNo(row.getCoaInspectionNo())
                    .operatorName(currentUserName()).remark("待包装片COA分类变化，保留FQC限制").build());
        }
    }

    private Long resolveWaitingCoaSourceRecordId(InspectionSliceRespVO row) {
        Long sourceRecordId = SOURCE_CUT_ROUND_REPORT.equals(row.getSourceType())
                ? row.getSourceCutRoundReportId()
                : SOURCE_MANUAL_HISTORY.equals(row.getSourceType()) ? row.getSourceManualPieceId() : null;
        if (sourceRecordId == null || sourceRecordId <= 0) {
            throw invalidParamException("待包装片 " + firstNotBlank(row.getSliceBatchNo(), row.getProductionBatchNo())
                    + " 的来源记录异常（来源类型：" + row.getSourceType() + "），无法同步COA状态，请核查来源记录");
        }
        return sourceRecordId;
    }

    private String resolveCoaSegmentForSlice(String sliceBatchNo) {
        HcCutRoundReportDO report = hcCutRoundReportMapper.selectByProductionBatchNo(sliceBatchNo);
        if (report != null) return resolveCoaScopeBatchNo(report);
        HcInnerPackUnitItemDO item = hcInnerPackUnitItemMapper.selectBySliceBatchNo(sliceBatchNo);
        if (item != null && item.getSourceManualPieceId() != null) {
            HcPackagingManualPieceDO piece = hcPackagingManualPieceMapper.selectById(item.getSourceManualPieceId());
            if (piece != null) return normalizeCoaSegmentBatchNo(piece.getSegmentBatchNo());
        }
        return normalizeCoaSegmentBatchNo(sliceBatchNo);
    }

    private void synchronizeStockCoaFreeze(HcFinishedStockDO stock) {
        if (stock == null || STATUS_SHIPPED.equals(stock.getStockStatus())) return;
        String segment = resolveCoaSegmentForSlice(stock.getSliceBatchNo());
        CoaInspectionMeta meta = StrUtil.isBlank(segment) ? null : buildCoaInspectionMap(Set.of(segment)).get(segment);
        applyStockCoaFreeze(stock, meta);
    }

    private void applyStockCoaFreeze(HcFinishedStockDO stock, CoaInspectionMeta meta) {
        boolean frozen = meta != null && !meta.isReleased();
        String reason = frozen
                ? INSPECTION_RESULT_NG.equals(meta.result) ? "COA NG" : "COA待检测或未放行" : "";
        String quality = INSPECTION_RESULT_NG.equalsIgnoreCase(stock.getQualityStatus())
                ? INSPECTION_RESULT_NG : frozen ? QUALITY_FROZEN : INSPECTION_RESULT_OK;
        if (Objects.equals(stock.getCoaFrozen(), frozen)
                && Objects.equals(StrUtil.blankToDefault(stock.getCoaFreezeReason(), ""), reason)
                && Objects.equals(stock.getQualityStatus(), quality)) return;
        HcFinishedStockDO before = BeanUtil.copyProperties(stock, HcFinishedStockDO.class);
        HcFinishedStockDO update = new HcFinishedStockDO();
        update.setId(stock.getId());
        update.setCoaFrozen(frozen);
        update.setCoaFreezeReason(reason);
        update.setQualityStatus(quality);
        hcFinishedStockMapper.updateById(update);
        stock.setCoaFrozen(frozen);
        stock.setCoaFreezeReason(reason);
        stock.setQualityStatus(quality);
        recordFinishedStockHistory(before, stock, frozen ? "FG_COA_FREEZE" : "FG_COA_UNFREEZE",
                LocalDateTime.now(BUSINESS_ZONE), currentUserName(), "PACKAGING_COA", null,
                meta == null ? null : meta.faiNo,
                "COA冻结：" + before.getCoaFrozen() + " -> " + frozen + "；原因："
                        + firstNotBlank(reason, meta == null ? "无COA送检单，按正常处理" : "COA已完成且OK") + "；原质量：" + before.getQualityStatus()
                        + "；现质量：" + quality + "；COA状态：" + (meta == null ? "无送检单" : meta.status));
    }

    /** 所有客户发货入口共用实时 COA 门禁，不能仅依赖配货时的快照。 */
    private void assertStockCoaReleased(HcFinishedStockDO stock) {
        if (stock == null) throw invalidParamException("发货片号不存在");
        String segment = resolveCoaSegmentForSlice(stock.getSliceBatchNo());
        CoaInspectionMeta meta = StrUtil.isBlank(segment) ? null : buildCoaInspectionMap(Set.of(segment), true).get(segment);
        if (meta != null && !meta.isReleased()) {
            throw invalidParamException("片号 " + stock.getSliceBatchNo() + " 为冻结品（"
                    + (meta == null ? "未送检" : "COA未放行：" + meta.result) + "），允许手工出库，禁止发货");
        }
        if (INSPECTION_RESULT_NG.equalsIgnoreCase(stock.getQualityStatus())) {
            throw invalidParamException("片号 " + stock.getSliceBatchNo() + " 仍有其它质量限制，禁止发货");
        }
    }

    private String currentUserName() {
        return firstNotBlank(SecurityFrameworkUtils.getLoginUserNickname(), "系统");
    }

    private Long currentTenantId() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
