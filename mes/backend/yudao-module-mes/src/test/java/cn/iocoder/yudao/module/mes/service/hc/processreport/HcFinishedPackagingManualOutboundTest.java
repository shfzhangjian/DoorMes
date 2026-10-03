package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.InboundBoxRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.DirectOutboundPendingPackageBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundPackageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundRepackReturnReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundRepackReturnRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundStockBatchReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ManualOutboundStockBatchRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.location.HcLocationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.service.hc.packagingevent.HcPackagingPieceEventLogService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingManualOutboundTest {

    @InjectMocks
    private HcFinishedPackagingServiceImpl service;

    @Mock
    private HcInnerPackUnitMapper hcInnerPackUnitMapper;
    @Mock
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Mock
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Mock
    private HcFinishedStockTxnLogMapper hcFinishedStockTxnLogMapper;
    @Mock
    private HcLocationMapper hcLocationMapper;
    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Mock
    private HcPackagingPieceEventLogService hcPackagingPieceEventLogService;

    @Test
    void shouldMarkEveryAvailablePieceAsManualOutboundAndWriteHistory() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(101L)
                .tenantId(1L)
                .innerUnitNo("IP-20260818-001")
                .unitStatus("INBOUNDED")
                .warehouseCode("FG")
                .warehouseName("成品仓")
                .locationCode("FG-1-L1-1")
                .locationName("1号库位")
                .build();
        HcInnerPackUnitDO outboundBox = HcInnerPackUnitDO.builder()
                .id(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .unitStatus("MANUAL_OUTBOUNDED")
                .warehouseCode(box.getWarehouseCode())
                .warehouseName(box.getWarehouseName())
                .locationCode(box.getLocationCode())
                .locationName(box.getLocationName())
                .build();
        HcFinishedStockDO stock = HcFinishedStockDO.builder()
                .id(201L)
                .tenantId(1L)
                .stockNo("FGS-001")
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo("SLICE-001")
                .qty(1)
                .stockStatus("AVAILABLE")
                .warehouseCode(box.getWarehouseCode())
                .warehouseName(box.getWarehouseName())
                .locationCode(box.getLocationCode())
                .locationName(box.getLocationName())
                .build();
        ManualOutboundPackageReqVO reqVO = new ManualOutboundPackageReqVO();
        reqVO.setId(box.getId());
        reqVO.setOperatorName("仓管员A");
        reqVO.setReason("现场手工领用");

        when(hcInnerPackUnitMapper.selectByIdForUpdate(box.getId())).thenReturn(box);
        when(hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(List.of(stock));
        when(hcInnerPackUnitMapper.selectById(box.getId())).thenReturn(outboundBox);
        when(hcFinishedStockMapper.selectListByInnerUnitNo(box.getInnerUnitNo())).thenReturn(List.of(stock));
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitId(box.getId())).thenReturn(List.of());

        InboundBoxRespVO result = service.manualOutboundInboundPackage(reqVO);

        verify(hcFinishedStockMapper).updateById(argThat((HcFinishedStockDO update) -> update.getId().equals(stock.getId())
                && "SHIPPED".equals(update.getStockStatus())));
        verify(hcInnerPackUnitMapper).updateById(argThat((HcInnerPackUnitDO update) -> update.getId().equals(box.getId())
                && "MANUAL_OUTBOUNDED".equals(update.getUnitStatus())));
        ArgumentCaptor<HcFinishedStockTxnLogDO> historyCaptor = ArgumentCaptor.forClass(HcFinishedStockTxnLogDO.class);
        verify(hcFinishedStockTxnLogMapper).insert(historyCaptor.capture());
        assertEquals("FG_MANUAL_OUTBOUND", historyCaptor.getValue().getTxnType());
        assertEquals("SHIPPED", historyCaptor.getValue().getAfterStockStatus());
        assertEquals("现场手工领用", historyCaptor.getValue().getRemark());
        assertEquals("MANUAL_OUTBOUNDED", result.getStatus());
    }

    @Test
    void shouldRejectBatchOutboundWhenHistoricalMultiPiecePackageIsNotSelectedCompletely() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(102L)
                .tenantId(1L)
                .innerUnitNo("IP-HISTORY-002")
                .unitStatus("INBOUNDED")
                .build();
        HcFinishedStockDO selectedStock = HcFinishedStockDO.builder()
                .id(202L)
                .tenantId(1L)
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo("HISTORY-SLICE-001")
                .stockStatus("AVAILABLE")
                .build();
        HcFinishedStockDO unselectedStock = HcFinishedStockDO.builder()
                .id(203L)
                .tenantId(1L)
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo("HISTORY-SLICE-002")
                .stockStatus("AVAILABLE")
                .build();
        ManualOutboundStockBatchReqVO reqVO = new ManualOutboundStockBatchReqVO();
        reqVO.setStockIds(List.of(selectedStock.getId()));
        reqVO.setReason("历史包装手工出库");

        when(hcFinishedStockMapper.selectById(selectedStock.getId())).thenReturn(selectedStock);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo()))
                .thenReturn(List.of(selectedStock, unselectedStock));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.manualOutboundFgStocks(reqVO));

        assertTrue(exception.getMessage().contains("必须勾选包内全部可用片号"));
        assertTrue(exception.getMessage().contains(unselectedStock.getSliceBatchNo()));
        verify(hcFinishedStockMapper, never()).updateById(argThat((HcFinishedStockDO update) ->
                update.getId().equals(selectedStock.getId())));
        verify(hcInnerPackUnitMapper, never()).updateById(argThat((HcInnerPackUnitDO update) ->
                update.getId().equals(box.getId())));
    }

    @Test
    void shouldBatchOutboundAfterEveryPieceInPackageIsSelected() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(103L)
                .tenantId(1L)
                .innerUnitNo("IP-HISTORY-003")
                .unitStatus("INBOUNDED")
                .locationCode("FG-1-L1-1")
                .build();
        HcFinishedStockDO firstStock = HcFinishedStockDO.builder()
                .id(204L)
                .tenantId(1L)
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo("HISTORY-SLICE-003")
                .stockStatus("AVAILABLE")
                .locationCode(box.getLocationCode())
                .build();
        HcFinishedStockDO secondStock = HcFinishedStockDO.builder()
                .id(205L)
                .tenantId(1L)
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo("HISTORY-SLICE-004")
                .stockStatus("AVAILABLE")
                .locationCode(box.getLocationCode())
                .build();
        ManualOutboundStockBatchReqVO reqVO = new ManualOutboundStockBatchReqVO();
        reqVO.setStockIds(List.of(firstStock.getId(), secondStock.getId()));
        reqVO.setOperatorName("仓管员B");
        reqVO.setReason("历史包装整包出库");

        when(hcFinishedStockMapper.selectById(firstStock.getId())).thenReturn(firstStock);
        when(hcFinishedStockMapper.selectById(secondStock.getId())).thenReturn(secondStock);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo()))
                .thenReturn(List.of(firstStock, secondStock));

        ManualOutboundStockBatchRespVO result = service.manualOutboundFgStocks(reqVO);

        verify(hcFinishedStockMapper).updateById(argThat((HcFinishedStockDO update) -> update.getId().equals(firstStock.getId())
                && "SHIPPED".equals(update.getStockStatus())));
        verify(hcFinishedStockMapper).updateById(argThat((HcFinishedStockDO update) -> update.getId().equals(secondStock.getId())
                && "SHIPPED".equals(update.getStockStatus())));
        verify(hcInnerPackUnitMapper).updateById(argThat((HcInnerPackUnitDO update) -> update.getId().equals(box.getId())
                && "MANUAL_OUTBOUNDED".equals(update.getUnitStatus())));
        assertEquals(2, result.getStockCount());
        assertEquals(1, result.getPackageCount());
        assertEquals(List.of(firstStock.getSliceBatchNo(), secondStock.getSliceBatchNo()), result.getSliceBatchNos());
    }

    @Test
    void shouldReturnOneManualOutboundPieceToPackagingWaitAndKeepRemainingPackagePieces() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(104L)
                .tenantId(1L)
                .innerUnitNo("IP-HISTORY-RETURN-001")
                .unitStatus("MANUAL_OUTBOUNDED")
                .locationCode("FG-1-L1-1")
                .build();
        HcFinishedStockTxnLogDO outboundTxn = HcFinishedStockTxnLogDO.builder()
                .id(301L)
                .tenantId(1L)
                .finishedStockId(401L)
                .txnNo("FGH-MANUAL-001")
                .txnType("FG_MANUAL_OUTBOUND")
                .afterStockStatus("SHIPPED")
                .innerUnitNo(box.getInnerUnitNo())
                .refDocId(box.getId())
                .build();
        HcFinishedStockDO returnedStock = HcFinishedStockDO.builder()
                .id(401L)
                .tenantId(1L)
                .stockNo("FGS-RETURN-001")
                .sliceBatchNo("SLICE-RETURN-001")
                .innerUnitNo(box.getInnerUnitNo())
                .stockStatus("SHIPPED")
                .build();
        HcFinishedStockDO remainingStock = HcFinishedStockDO.builder()
                .id(402L)
                .tenantId(1L)
                .stockNo("FGS-RETURN-002")
                .sliceBatchNo("SLICE-RETURN-002")
                .innerUnitNo(box.getInnerUnitNo())
                .stockStatus("SHIPPED")
                .build();
        HcInnerPackUnitItemDO returnedItem = HcInnerPackUnitItemDO.builder()
                .id(501L)
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .sourceType("CUT_ROUND_REPORT")
                .sourceCutRoundReportId(701L)
                .sliceBatchNo(returnedStock.getSliceBatchNo())
                .build();
        HcInnerPackUnitItemDO remainingItem = HcInnerPackUnitItemDO.builder()
                .id(502L)
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .sourceType("CUT_ROUND_REPORT")
                .sourceCutRoundReportId(702L)
                .sliceBatchNo(remainingStock.getSliceBatchNo())
                .build();
        ManualOutboundRepackReturnReqVO reqVO = new ManualOutboundRepackReturnReqVO();
        reqVO.setTxnLogId(outboundTxn.getId());
        reqVO.setReason("客户临时退回，需要重新包装");
        reqVO.setPhysicalReturned(true);
        reqVO.setOperatorName("仓管员C");

        when(hcFinishedStockTxnLogMapper.selectById(outboundTxn.getId())).thenReturn(outboundTxn);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId()))
                .thenReturn(List.of(returnedItem, remainingItem));
        when(hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo()))
                .thenReturn(List.of(returnedStock, remainingStock));
        when(hcCutRoundReportMapper.selectById(returnedItem.getSourceCutRoundReportId()))
                .thenReturn(HcCutRoundReportDO.builder().id(returnedItem.getSourceCutRoundReportId()).build());

        ManualOutboundRepackReturnRespVO result = service.returnManualOutboundStockForRepack(reqVO);

        verify(hcInnerPackUnitItemMapper).deletePhysicallyByIds(List.of(returnedItem.getId()));
        verify(hcFinishedStockMapper).deletePhysicallyByIds(List.of(returnedStock.getId()));
        verify(hcInnerPackUnitMapper).updateById(argThat((HcInnerPackUnitDO update) -> update.getId().equals(box.getId())
                && Integer.valueOf(1).equals(update.getCurrentQty())
                && "MANUAL_OUTBOUNDED".equals(update.getUnitStatus())
                && "".equals(update.getLocationCode())));
        verify(hcInnerPackUnitMapper, never()).deletePhysicallyById(box.getId());
        ArgumentCaptor<HcFinishedStockTxnLogDO> historyCaptor = ArgumentCaptor.forClass(HcFinishedStockTxnLogDO.class);
        verify(hcFinishedStockTxnLogMapper).insert(historyCaptor.capture());
        assertEquals("FG_PACKAGE_SPLIT_RETURN", historyCaptor.getValue().getTxnType());
        assertEquals("RETURNED_FOR_REPACK", historyCaptor.getValue().getAfterStockStatus());
        assertTrue(historyCaptor.getValue().getRemark().contains("客户临时退回"));
        assertEquals(returnedStock.getSliceBatchNo(), result.getSliceBatchNo());
        assertEquals(box.getInnerUnitNo(), result.getSourceInnerUnitNo());
        assertEquals(1, result.getSourcePackageRemainingPieceCount());
    }

    @Test
    void shouldResetManualHistoryPieceAndDeleteEmptyOriginalPackageWhenReturningForRepack() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(105L)
                .tenantId(1L)
                .innerUnitNo("IP-HISTORY-RETURN-002")
                .unitStatus("MANUAL_OUTBOUNDED")
                .build();
        HcFinishedStockTxnLogDO outboundTxn = HcFinishedStockTxnLogDO.builder()
                .id(302L)
                .tenantId(1L)
                .finishedStockId(403L)
                .txnNo("FGH-MANUAL-002")
                .txnType("FG_MANUAL_OUTBOUND")
                .afterStockStatus("SHIPPED")
                .innerUnitNo(box.getInnerUnitNo())
                .refDocId(box.getId())
                .build();
        HcFinishedStockDO returnedStock = HcFinishedStockDO.builder()
                .id(403L)
                .tenantId(1L)
                .stockNo("FGS-RETURN-003")
                .sliceBatchNo("SLICE-RETURN-003")
                .innerUnitNo(box.getInnerUnitNo())
                .stockStatus("SHIPPED")
                .build();
        HcInnerPackUnitItemDO returnedItem = HcInnerPackUnitItemDO.builder()
                .id(503L)
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .sourceType("MANUAL_HISTORY")
                .sourceManualPieceId(801L)
                .sliceBatchNo(returnedStock.getSliceBatchNo())
                .build();
        HcPackagingManualPieceDO manualPiece = HcPackagingManualPieceDO.builder()
                .id(801L)
                .sliceBatchNo(returnedStock.getSliceBatchNo())
                .recordStatus("INBOUNDED")
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .build();
        ManualOutboundRepackReturnReqVO reqVO = new ManualOutboundRepackReturnReqVO();
        reqVO.setTxnLogId(outboundTxn.getId());
        reqVO.setReason("拆包后重新配套包装");
        reqVO.setPhysicalReturned(true);

        when(hcFinishedStockTxnLogMapper.selectById(outboundTxn.getId())).thenReturn(outboundTxn);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId())).thenReturn(List.of(returnedItem));
        when(hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(List.of(returnedStock));
        when(hcPackagingManualPieceMapper.selectByIdForUpdate(manualPiece.getId())).thenReturn(manualPiece);

        ManualOutboundRepackReturnRespVO result = service.returnManualOutboundStockForRepack(reqVO);

        verify(hcPackagingManualPieceMapper).updateById(argThat((HcPackagingManualPieceDO update) -> update.getId().equals(manualPiece.getId())
                && "WAIT_PACKAGING".equals(update.getRecordStatus())
                && update.getInnerUnitId() == null
                && update.getInnerUnitNo() == null));
        verify(hcInnerPackUnitMapper).deletePhysicallyById(box.getId());
        verify(hcInnerPackUnitItemMapper).deletePhysicallyByIds(List.of(returnedItem.getId()));
        verify(hcFinishedStockMapper).deletePhysicallyByIds(List.of(returnedStock.getId()));
        assertEquals(0, result.getSourcePackageRemainingPieceCount());
    }

    @Test
    void shouldDirectOutboundQualifiedPackedPackageAndWriteHistorySnapshot() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(106L)
                .tenantId(1L)
                .innerUnitNo("IP-DIRECT-OK-001")
                .unitStatus("PACKED")
                .materialCode("MAT-001")
                .materialName("抛光垫")
                .modelCode("MODEL-001")
                .batchNo("BATCH-001")
                .productSize("20寸")
                .build();
        HcInnerPackUnitItemDO item = HcInnerPackUnitItemDO.builder()
                .id(601L)
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .sourceCutRoundReportId(701L)
                .sliceBatchNo("SLICE-DIRECT-001")
                .qualityStatus("OK")
                .build();
        HcCutRoundReportDO report = HcCutRoundReportDO.builder().id(item.getSourceCutRoundReportId()).build();
        DirectOutboundPendingPackageBatchReqVO reqVO = new DirectOutboundPendingPackageBatchReqVO();
        reqVO.setPackageIds(List.of(box.getId()));
        reqVO.setOperatorName("仓管员D");
        reqVO.setReason("客户现场直发");

        when(hcInnerPackUnitMapper.selectByIdForUpdate(box.getId())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId())).thenReturn(List.of(item));
        when(hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(List.of());
        when(hcCutRoundReportMapper.selectByIdForUpdate(item.getSourceCutRoundReportId())).thenReturn(report);

        service.directOutboundPendingQualifiedInboundPackages(reqVO);

        verify(hcInnerPackUnitMapper).updateById(argThat((HcInnerPackUnitDO update) -> update.getId().equals(box.getId())
                && "MANUAL_OUTBOUNDED".equals(update.getUnitStatus())));
        ArgumentCaptor<HcFinishedStockTxnLogDO> historyCaptor = ArgumentCaptor.forClass(HcFinishedStockTxnLogDO.class);
        verify(hcFinishedStockTxnLogMapper).insert(historyCaptor.capture());
        HcFinishedStockTxnLogDO history = historyCaptor.getValue();
        assertEquals("FG_PACKAGE_DIRECT_OUTBOUND", history.getTxnType());
        assertEquals("SHIPPED", history.getAfterStockStatus());
        assertNull(history.getFinishedStockId());
        assertEquals(item.getSliceBatchNo(), history.getSliceBatchNo());
        assertTrue(history.getRemark().contains("客户现场直发"));
        verify(hcPackagingPieceEventLogService).recordCutRoundEvent(eq(report),
                eq("PACKAGING_DIRECT_OUTBOUND"), eq("PACKED"), eq("MANUAL_OUTBOUNDED"), any(),
                eq("仓管员D"), eq(box.getInnerUnitNo()), contains("客户现场直发"));
    }

    @Test
    void shouldDirectOutboundQualifiedInboundLockedPackageAndCloseExistingStock() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(107L)
                .tenantId(1L)
                .innerUnitNo("IP-DIRECT-OK-002")
                .unitStatus("INBOUND_LOCKED")
                .build();
        HcInnerPackUnitItemDO item = HcInnerPackUnitItemDO.builder()
                .id(602L)
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .sourceCutRoundReportId(702L)
                .sliceBatchNo("SLICE-DIRECT-002")
                .qualityStatus("OK")
                .build();
        HcFinishedStockDO stock = HcFinishedStockDO.builder()
                .id(801L)
                .tenantId(1L)
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo(item.getSliceBatchNo())
                .qty(1)
                .qualityStatus("OK")
                .stockStatus("INBOUND_LOCKED")
                .build();
        HcCutRoundReportDO report = HcCutRoundReportDO.builder().id(item.getSourceCutRoundReportId()).build();
        DirectOutboundPendingPackageBatchReqVO reqVO = new DirectOutboundPendingPackageBatchReqVO();
        reqVO.setPackageIds(List.of(box.getId()));
        reqVO.setOperatorName("仓管员E");
        reqVO.setReason("返工后直接出库");

        when(hcInnerPackUnitMapper.selectByIdForUpdate(box.getId())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId())).thenReturn(List.of(item));
        when(hcFinishedStockMapper.selectListByInnerUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(List.of(stock));
        when(hcCutRoundReportMapper.selectByIdForUpdate(item.getSourceCutRoundReportId())).thenReturn(report);

        service.directOutboundPendingQualifiedInboundPackages(reqVO);

        verify(hcFinishedStockMapper).updateById(argThat((HcFinishedStockDO update) -> update.getId().equals(stock.getId())
                && "SHIPPED".equals(update.getStockStatus())));
        ArgumentCaptor<HcFinishedStockTxnLogDO> historyCaptor = ArgumentCaptor.forClass(HcFinishedStockTxnLogDO.class);
        verify(hcFinishedStockTxnLogMapper).insert(historyCaptor.capture());
        assertEquals("FG_PACKAGE_DIRECT_OUTBOUND", historyCaptor.getValue().getTxnType());
        assertEquals(stock.getId(), historyCaptor.getValue().getFinishedStockId());
    }

    @Test
    void shouldRejectDirectOutboundWhenPendingPackageContainsNgPiece() {
        HcInnerPackUnitDO box = HcInnerPackUnitDO.builder()
                .id(108L)
                .tenantId(1L)
                .innerUnitNo("IP-DIRECT-NG-001")
                .unitStatus("PACKED")
                .build();
        HcInnerPackUnitItemDO ngItem = HcInnerPackUnitItemDO.builder()
                .id(603L)
                .innerUnitId(box.getId())
                .innerUnitNo(box.getInnerUnitNo())
                .sliceBatchNo("SLICE-DIRECT-NG-001")
                .qualityStatus("NG")
                .build();
        DirectOutboundPendingPackageBatchReqVO reqVO = new DirectOutboundPendingPackageBatchReqVO();
        reqVO.setPackageIds(List.of(box.getId()));
        reqVO.setReason("不应允许");

        when(hcInnerPackUnitMapper.selectByIdForUpdate(box.getId())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId())).thenReturn(List.of(ngItem));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.directOutboundPendingQualifiedInboundPackages(reqVO));

        assertTrue(exception.getMessage().contains("非合格片"));
        verify(hcFinishedStockTxnLogMapper, never()).insert(any(HcFinishedStockTxnLogDO.class));
        verify(hcInnerPackUnitMapper, never()).updateById(argThat((HcInnerPackUnitDO update) -> update.getId().equals(box.getId())));
    }
}
