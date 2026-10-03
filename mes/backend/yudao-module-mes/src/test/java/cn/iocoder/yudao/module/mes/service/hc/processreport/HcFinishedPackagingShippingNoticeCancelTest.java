package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeCancelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ShippingNoticeRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundBoxDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundBoxItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgOutboundOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticeItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgShippingNoticePickItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockTxnLogDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgOutboundBoxItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgOutboundBoxMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgOutboundOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticePickItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockTxnLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFqcShippingDetailMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsOqcOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.coa.QmsCoaReportMapper;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsFqcShippingDetailDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsOqcOrderDO;
import cn.iocoder.yudao.module.mes.service.hc.packagingevent.HcPackagingPieceEventLogService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingShippingNoticeCancelTest {

    @Spy
    @InjectMocks
    private HcFinishedPackagingServiceImpl service;

    @Mock
    private HcFgShippingNoticeMapper hcFgShippingNoticeMapper;
    @Mock
    private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Mock
    private HcFgShippingNoticePickItemMapper hcFgShippingNoticePickItemMapper;
    @Mock
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Mock
    private HcFinishedStockTxnLogMapper hcFinishedStockTxnLogMapper;
    @Mock
    private HcInnerPackUnitMapper hcInnerPackUnitMapper;
    @Mock
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Mock
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock
    private HcPackagingPieceEventLogService hcPackagingPieceEventLogService;
    @Mock
    private QmsFqcShippingDetailMapper qmsFqcShippingDetailMapper;
    @Mock
    private QmsFqcOrderMapper qmsFqcOrderMapper;
    @Mock
    private QmsOqcOrderMapper qmsOqcOrderMapper;
    @Mock
    private QmsCoaReportMapper qmsCoaReportMapper;
    @Mock
    private HcFgOutboundOrderMapper hcFgOutboundOrderMapper;
    @Mock
    private HcFgOutboundBoxMapper hcFgOutboundBoxMapper;
    @Mock
    private HcFgOutboundBoxItemMapper hcFgOutboundBoxItemMapper;

    @Test
    void shouldRequirePhysicalReturnConfirmationBeforeChangingAnyInventory() {
        HcFgShippingNoticeDO notice = notice(101L);
        HcFgShippingNoticePickItemDO pickItem = pick(201L, 301L, 401L, "SLICE-001");
        ShippingNoticeCancelReqVO reqVO = cancelRequest(notice.getId(), false);

        when(hcFgShippingNoticeMapper.selectByIdForUpdate(notice.getId())).thenReturn(notice);
        when(hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId())).thenReturn(List.of());
        when(hcFgShippingNoticePickItemMapper.selectListByNoticeId(notice.getId())).thenReturn(List.of(pickItem));
        when(hcFgOutboundOrderMapper.selectBySourceNoticeId(notice.getId())).thenReturn(null);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.cancelShippingNotice(reqVO));

        assertTrue(exception.getMessage().contains("实物已退回包装工位"));
        verify(hcFinishedStockMapper, never()).selectByIdForUpdate(any());
        verify(hcFgShippingNoticeMapper, never()).updateById(any(HcFgShippingNoticeDO.class));
    }

    @Test
    void shouldReturnAllPiecesToPackagingWaitByEffectiveFqcResultAndVoidDownstreamDocuments() {
        HcFgShippingNoticeDO notice = notice(102L);
        HcFgShippingNoticeItemDO noticeItem = HcFgShippingNoticeItemDO.builder()
                .id(103L)
                .noticeId(notice.getId())
                .lockStatus("OQC_INSPECTING")
                .build();
        HcFgShippingNoticePickItemDO okPick = pick(211L, 311L, 411L, "SLICE-OK");
        HcFgShippingNoticePickItemDO ngPick = pick(212L, 312L, 412L, "SLICE-NG");
        HcFgShippingNoticePickItemDO unfinishedPick = pick(213L, 313L, 413L, "SLICE-UNFINISHED");
        HcFinishedStockDO okStock = stock(okPick, "OK");
        HcFinishedStockDO ngStock = stock(ngPick, "OK");
        HcFinishedStockDO unfinishedStock = stock(unfinishedPick, "NG");
        HcCutRoundReportDO okReport = report(okPick);
        HcCutRoundReportDO ngReport = report(ngPick);
        HcCutRoundReportDO unfinishedReport = report(unfinishedPick);

        QmsFqcOrderDO completedOkOrder = fqcOrder(701L, "COMPLETED");
        QmsFqcOrderDO completedNgOrder = fqcOrder(702L, "COMPLETED");
        QmsFqcOrderDO unfinishedOrder = fqcOrder(703L, "INSPECTING");
        QmsFqcShippingDetailDO okDetail = fqcDetail(okPick, completedOkOrder, "OK");
        QmsFqcShippingDetailDO ngDetail = fqcDetail(ngPick, completedNgOrder, "NG");
        QmsFqcShippingDetailDO unfinishedDetail = fqcDetail(unfinishedPick, unfinishedOrder, "OK");
        QmsOqcOrderDO oqcOrder = QmsOqcOrderDO.builder().id(801L).status("INSPECTING").build();
        QmsOqcOrderDO completedOqcOrder = QmsOqcOrderDO.builder().id(802L).status("COMPLETED").build();
        HcFgOutboundOrderDO outboundOrder = HcFgOutboundOrderDO.builder()
                .id(1001L)
                .outboundStatus("PACKING")
                .boxCount(1)
                .pieceCount(1)
                .build();
        HcFgOutboundBoxDO outboundBox = HcFgOutboundBoxDO.builder()
                .id(1002L)
                .outboundOrderId(outboundOrder.getId())
                .build();
        HcFgOutboundBoxItemDO outboundBoxItem = HcFgOutboundBoxItemDO.builder()
                .id(1003L)
                .outboundOrderId(outboundOrder.getId())
                .outboundBoxId(outboundBox.getId())
                .finishedStockId(okStock.getId())
                .build();
        ShippingNoticeRespVO response = new ShippingNoticeRespVO();
        response.setId(notice.getId());

        when(hcFgShippingNoticeMapper.selectByIdForUpdate(notice.getId())).thenReturn(notice);
        when(hcFgShippingNoticeItemMapper.selectListByNoticeId(notice.getId())).thenReturn(List.of(noticeItem));
        when(hcFgShippingNoticePickItemMapper.selectListByNoticeId(notice.getId()))
                .thenReturn(List.of(okPick, ngPick, unfinishedPick));
        when(hcFgOutboundOrderMapper.selectBySourceNoticeId(notice.getId())).thenReturn(outboundOrder);
        when(hcFinishedStockMapper.selectByIdForUpdate(okStock.getId())).thenReturn(okStock);
        when(hcFinishedStockMapper.selectByIdForUpdate(ngStock.getId())).thenReturn(ngStock);
        when(hcFinishedStockMapper.selectByIdForUpdate(unfinishedStock.getId())).thenReturn(unfinishedStock);
        when(hcCutRoundReportMapper.selectById(okReport.getId())).thenReturn(okReport);
        when(hcCutRoundReportMapper.selectById(ngReport.getId())).thenReturn(ngReport);
        when(hcCutRoundReportMapper.selectById(unfinishedReport.getId())).thenReturn(unfinishedReport);
        when(qmsFqcShippingDetailMapper.selectLatestByShippingPickItemId(okPick.getId())).thenReturn(okDetail);
        when(qmsFqcShippingDetailMapper.selectLatestByShippingPickItemId(ngPick.getId())).thenReturn(ngDetail);
        when(qmsFqcShippingDetailMapper.selectLatestByShippingPickItemId(unfinishedPick.getId()))
                .thenReturn(unfinishedDetail);
        when(qmsFqcOrderMapper.selectById(completedOkOrder.getId())).thenReturn(completedOkOrder);
        when(qmsFqcOrderMapper.selectById(completedNgOrder.getId())).thenReturn(completedNgOrder);
        when(qmsFqcOrderMapper.selectById(unfinishedOrder.getId())).thenReturn(unfinishedOrder);
        when(qmsFqcOrderMapper.selectListBySource(QmsFqcOrderMapper.SOURCE_MODULE_FG_SHIPPING_FQC,
                notice.getId())).thenReturn(List.of(completedOkOrder, completedNgOrder, unfinishedOrder));
        when(qmsOqcOrderMapper.selectListByShippingNoticeId(notice.getId()))
                .thenReturn(List.of(oqcOrder, completedOqcOrder));
        when(qmsCoaReportMapper.selectListByShippingNoticeId(notice.getId())).thenReturn(List.of());
        when(hcFgOutboundBoxMapper.selectListByOutboundOrderId(outboundOrder.getId()))
                .thenReturn(List.of(outboundBox));
        when(hcFgOutboundBoxItemMapper.selectListByOutboundOrderId(outboundOrder.getId()))
                .thenReturn(List.of(outboundBoxItem));
        doReturn(response).when(service).getShippingNotice(notice.getId());

        ShippingNoticeRespVO result = service.cancelShippingNotice(cancelRequest(notice.getId(), true));

        assertEquals(notice.getId(), result.getId());
        verify(hcFinishedStockMapper, times(3)).deletePhysicallyByIds(argThat(ids -> ids.size() == 1));

        ArgumentCaptor<HcFinishedStockTxnLogDO> historyCaptor = ArgumentCaptor.forClass(HcFinishedStockTxnLogDO.class);
        verify(hcFinishedStockTxnLogMapper, times(3)).insert(historyCaptor.capture());
        Map<String, HcFinishedStockTxnLogDO> historyBySlice = historyCaptor.getAllValues().stream()
                .collect(Collectors.toMap(HcFinishedStockTxnLogDO::getSliceBatchNo, Function.identity()));
        assertEquals("OK", historyBySlice.get(okStock.getSliceBatchNo()).getQualityStatus());
        assertEquals("NG", historyBySlice.get(ngStock.getSliceBatchNo()).getQualityStatus());
        assertEquals("NG", historyBySlice.get(unfinishedStock.getSliceBatchNo()).getQualityStatus());
        assertTrue(historyBySlice.values().stream()
                .allMatch(history -> "FG_SHIPPING_CANCEL_RETURN".equals(history.getTxnType())
                        && "RETURNED_FOR_REPACK".equals(history.getAfterStockStatus())));

        ArgumentCaptor<HcCutRoundReportDO> reportCaptor = ArgumentCaptor.forClass(HcCutRoundReportDO.class);
        verify(hcCutRoundReportMapper, times(3)).updateById(reportCaptor.capture());
        Map<Long, HcCutRoundReportDO> reportUpdateMap = reportCaptor.getAllValues().stream()
                .collect(Collectors.toMap(HcCutRoundReportDO::getId, Function.identity()));
        assertEquals("OK", reportUpdateMap.get(okReport.getId()).getInspectionResult());
        assertEquals("NG", reportUpdateMap.get(ngReport.getId()).getInspectionResult());
        assertEquals("NG", reportUpdateMap.get(unfinishedReport.getId()).getInspectionResult());

        ArgumentCaptor<HcFgShippingNoticePickItemDO> pickCaptor =
                ArgumentCaptor.forClass(HcFgShippingNoticePickItemDO.class);
        verify(hcFgShippingNoticePickItemMapper, times(3)).updateById(pickCaptor.capture());
        Map<Long, HcFgShippingNoticePickItemDO> pickUpdateMap = pickCaptor.getAllValues().stream()
                .collect(Collectors.toMap(HcFgShippingNoticePickItemDO::getId, Function.identity()));
        assertEquals("OK", pickUpdateMap.get(okPick.getId()).getQualityStatus());
        assertEquals("NG", pickUpdateMap.get(ngPick.getId()).getQualityStatus());
        assertEquals("NG", pickUpdateMap.get(unfinishedPick.getId()).getQualityStatus());
        assertTrue(pickUpdateMap.values().stream()
                .allMatch(update -> "CANCELLED".equals(update.getLockStatus())));

        ArgumentCaptor<QmsFqcOrderDO> fqcUpdateCaptor = ArgumentCaptor.forClass(QmsFqcOrderDO.class);
        verify(qmsFqcOrderMapper, times(3)).updateById(fqcUpdateCaptor.capture());
        Map<Long, QmsFqcOrderDO> fqcUpdateMap = fqcUpdateCaptor.getAllValues().stream()
                .collect(Collectors.toMap(QmsFqcOrderDO::getId, Function.identity()));
        assertNull(fqcUpdateMap.get(completedOkOrder.getId()).getStatus());
        assertNull(fqcUpdateMap.get(completedNgOrder.getId()).getStatus());
        assertEquals("CANCELED", fqcUpdateMap.get(unfinishedOrder.getId()).getStatus());
        ArgumentCaptor<QmsOqcOrderDO> oqcUpdateCaptor = ArgumentCaptor.forClass(QmsOqcOrderDO.class);
        verify(qmsOqcOrderMapper, times(2)).updateById(oqcUpdateCaptor.capture());
        Map<Long, QmsOqcOrderDO> oqcUpdateMap = oqcUpdateCaptor.getAllValues().stream()
                .collect(Collectors.toMap(QmsOqcOrderDO::getId, Function.identity()));
        assertEquals("CANCELED", oqcUpdateMap.get(oqcOrder.getId()).getStatus());
        assertNull(oqcUpdateMap.get(completedOqcOrder.getId()).getStatus());
        verify(hcFgOutboundBoxItemMapper).deleteById(outboundBoxItem.getId());
        verify(hcFgOutboundBoxMapper).deleteById(outboundBox.getId());
        verify(hcFgOutboundOrderMapper).updateById(argThat((HcFgOutboundOrderDO update) ->
                "CANCELED".equals(update.getOutboundStatus())
                && Integer.valueOf(0).equals(update.getBoxCount())
                && Integer.valueOf(0).equals(update.getPieceCount())));
        verify(hcFgShippingNoticeMapper).updateById(argThat((HcFgShippingNoticeDO update) ->
                "CANCELLED".equals(update.getNoticeStatus())
                && Integer.valueOf(0).equals(update.getLockedQty())));
    }

    private HcFgShippingNoticeDO notice(Long id) {
        return HcFgShippingNoticeDO.builder()
                .id(id)
                .noticeNo("SN-" + id)
                .noticeStatus("OQC_INSPECTING")
                .lockedQty(3)
                .build();
    }

    private HcFgShippingNoticePickItemDO pick(Long id, Long sourceReportId, Long stockId, String sliceBatchNo) {
        return HcFgShippingNoticePickItemDO.builder()
                .id(id)
                .noticeId(102L)
                .sourceCutRoundReportId(sourceReportId)
                .finishedStockId(stockId)
                .stockNo("FGS-" + stockId)
                .sliceBatchNo(sliceBatchNo)
                .actualSliceBatchNo(sliceBatchNo)
                .qualityStatus("OK")
                .lockStatus("OQC_INSPECTING")
                .build();
    }

    private HcFinishedStockDO stock(HcFgShippingNoticePickItemDO pick, String qualityStatus) {
        return HcFinishedStockDO.builder()
                .id(pick.getFinishedStockId())
                .stockNo(pick.getStockNo())
                .sliceBatchNo(pick.getSliceBatchNo())
                .qualityStatus(qualityStatus)
                .stockStatus("OUTBOUND_LOCKED")
                .qty(1)
                .build();
    }

    private HcCutRoundReportDO report(HcFgShippingNoticePickItemDO pick) {
        return HcCutRoundReportDO.builder()
                .id(pick.getSourceCutRoundReportId())
                .productionBatchNo(pick.getSliceBatchNo())
                .inspectionStatus("COMPLETED")
                .inspectionResult("OK")
                .build();
    }

    private QmsFqcOrderDO fqcOrder(Long id, String status) {
        return QmsFqcOrderDO.builder()
                .id(id)
                .fqcNo("FQC-" + id)
                .status(status)
                .build();
    }

    private QmsFqcShippingDetailDO fqcDetail(HcFgShippingNoticePickItemDO pick,
                                               QmsFqcOrderDO order,
                                               String result) {
        return QmsFqcShippingDetailDO.builder()
                .id(order.getId() + 1000)
                .fqcId(order.getId())
                .shippingPickItemId(pick.getId())
                .actualSliceBatchNo(pick.getActualSliceBatchNo())
                .rowJudgment(result)
                .build();
    }

    private ShippingNoticeCancelReqVO cancelRequest(Long noticeId, boolean physicalReturned) {
        ShippingNoticeCancelReqVO reqVO = new ShippingNoticeCancelReqVO();
        reqVO.setId(noticeId);
        reqVO.setReason("客户取消发货");
        reqVO.setOperatorName("发货员A");
        reqVO.setPhysicalReturned(physicalReturned);
        return reqVO;
    }
}
