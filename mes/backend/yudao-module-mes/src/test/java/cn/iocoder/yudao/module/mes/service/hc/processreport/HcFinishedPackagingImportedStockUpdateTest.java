package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.FgStockLedgerRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcFinishedPackagingVO.ImportedStockDataUpdateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFinishedStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcFgInboundOrderItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceCorrectionLogDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcPackagingManualPieceDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.bom.HcBomMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgShippingNoticeItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFinishedStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcFgInboundOrderItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcInnerPackUnitMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceCorrectionLogMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.packaging.HcPackagingManualPieceMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsPackagingCoaSampleClaimMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcFinishedPackagingImportedStockUpdateTest {

    @InjectMocks
    private HcFinishedPackagingServiceImpl service;

    @Mock
    private HcFinishedStockMapper hcFinishedStockMapper;
    @Mock
    private HcFgInboundOrderItemMapper hcFgInboundOrderItemMapper;
    @Mock
    private HcInnerPackUnitMapper hcInnerPackUnitMapper;
    @Mock
    private HcInnerPackUnitItemMapper hcInnerPackUnitItemMapper;
    @Mock
    private HcPackagingManualPieceMapper hcPackagingManualPieceMapper;
    @Mock
    private HcFgShippingNoticeItemMapper hcFgShippingNoticeItemMapper;
    @Mock
    private QmsPackagingCoaSampleClaimMapper hcPackagingCoaSampleClaimMapper;
    @Mock
    private HcBomMapper hcBomMapper;
    @Mock
    private HcPackagingManualPieceCorrectionLogMapper hcPackagingManualPieceCorrectionLogMapper;
    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;

    @Test
    void shouldCorrectSingleImportedStockAndKeepTransactionHistoryUntouched() {
        HcFinishedStockDO stock = stock();
        HcInnerPackUnitDO box = box();
        HcInnerPackUnitItemDO item = item();
        HcPackagingManualPieceDO manualPiece = manualPiece();
        ImportedStockDataUpdateReqVO reqVO = request();

        when(hcFinishedStockMapper.selectByIdForUpdate(stock.getId())).thenReturn(stock);
        when(hcFgShippingNoticeItemMapper.selectActiveLockedQtyByFinishedStockId(stock.getId())).thenReturn(0);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId())).thenReturn(List.of(item));
        when(hcPackagingManualPieceMapper.selectByIdForUpdate(manualPiece.getId())).thenReturn(manualPiece);
        when(hcPackagingManualPieceMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo())).thenReturn(List.of());
        when(hcInnerPackUnitItemMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo())).thenReturn(List.of());
        when(hcFinishedStockMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo())).thenReturn(List.of());
        when(hcPackagingCoaSampleClaimMapper.selectListBySource(eq("MANUAL_HISTORY"), eq(List.of(manualPiece.getId()))))
                .thenReturn(List.of());
        when(hcBomMapper.selectEnabledByMaterialCodeAndModelCode(reqVO.getMaterialCode(), reqVO.getModelCode()))
                .thenReturn(null);
        when(hcFgInboundOrderItemMapper.selectListByOuterBoxIdForUpdate(box.getId()))
                .thenReturn(List.of(HcFgInboundOrderItemDO.builder().id(501L).outerBoxId(box.getId()).build()));

        FgStockLedgerRespVO result = service.updateImportedStockData(reqVO);

        ArgumentCaptor<HcPackagingManualPieceDO> manualPieceCaptor =
                ArgumentCaptor.forClass(HcPackagingManualPieceDO.class);
        verify(hcPackagingManualPieceMapper).updateById(manualPieceCaptor.capture());
        verify(hcInnerPackUnitItemMapper).updateById(any(HcInnerPackUnitItemDO.class));
        ArgumentCaptor<HcInnerPackUnitDO> boxCaptor = ArgumentCaptor.forClass(HcInnerPackUnitDO.class);
        verify(hcInnerPackUnitMapper).updateById(boxCaptor.capture());
        ArgumentCaptor<HcFinishedStockDO> stockCaptor = ArgumentCaptor.forClass(HcFinishedStockDO.class);
        verify(hcFinishedStockMapper).updateById(stockCaptor.capture());
        ArgumentCaptor<HcFgInboundOrderItemDO> inboundItemCaptor =
                ArgumentCaptor.forClass(HcFgInboundOrderItemDO.class);
        verify(hcFgInboundOrderItemMapper).updateById(inboundItemCaptor.capture());
        ArgumentCaptor<HcPackagingManualPieceCorrectionLogDO> logCaptor =
                ArgumentCaptor.forClass(HcPackagingManualPieceCorrectionLogDO.class);
        verify(hcPackagingManualPieceCorrectionLogMapper).insert(logCaptor.capture());
        assertEquals("M-NEW", result.getMaterialCode());
        assertEquals("SLICE-NEW", result.getSliceBatchNo());
        assertEquals("SEG-NEW", result.getBatchNo());
        assertEquals("MODEL-NEW", result.getModelCode());
        assertEquals(LocalDate.of(2026, 8, 1), result.getProductionDate());
        assertEquals(LocalDate.of(2027, 5, 31), result.getExpiryDate());
        assertEquals("OK", result.getQualityStatus());
        assertTrue(result.getLabelReprintRequired());
        assertEquals("资料更正", logCaptor.getValue().getCorrectionReason());
        assertEquals("SEG-NEW", manualPieceCaptor.getValue().getSegmentBatchNo());
        assertEquals("SEG-NEW", boxCaptor.getValue().getBatchNo());
        assertEquals("SEG-NEW", stockCaptor.getValue().getBatchNo());
        assertEquals("SEG-NEW", inboundItemCaptor.getValue().getBatchNo());
        assertTrue(logCaptor.getValue().getBeforeDataJson().contains("M-OLD"));
        assertTrue(logCaptor.getValue().getAfterDataJson().contains("M-NEW"));
        assertTrue(logCaptor.getValue().getAfterDataJson().contains("SEG-NEW"));
    }

    @Test
    void shouldRejectMultiPiecePackageBeforeChangingAnySnapshot() {
        HcFinishedStockDO stock = stock();
        HcInnerPackUnitDO box = box();

        when(hcFinishedStockMapper.selectByIdForUpdate(stock.getId())).thenReturn(stock);
        when(hcFgShippingNoticeItemMapper.selectActiveLockedQtyByFinishedStockId(stock.getId())).thenReturn(0);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId()))
                .thenReturn(List.of(item(), HcInnerPackUnitItemDO.builder().id(303L).build()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.updateImportedStockData(request()));

        assertTrue(exception.getMessage().contains("多片包装"));
        verify(hcPackagingManualPieceMapper, never()).updateById(any(HcPackagingManualPieceDO.class));
        verify(hcFinishedStockMapper, never()).updateById(any(HcFinishedStockDO.class));
        verify(hcPackagingManualPieceCorrectionLogMapper, never())
                .insert(any(HcPackagingManualPieceCorrectionLogDO.class));
    }

    @Test
    void shouldRejectDuplicateSliceBatchNoBeforeChangingAnySnapshot() {
        HcFinishedStockDO stock = stock();
        HcInnerPackUnitDO box = box();
        HcInnerPackUnitItemDO item = item();
        HcPackagingManualPieceDO manualPiece = manualPiece();
        ImportedStockDataUpdateReqVO reqVO = request();

        when(hcFinishedStockMapper.selectByIdForUpdate(stock.getId())).thenReturn(stock);
        when(hcFgShippingNoticeItemMapper.selectActiveLockedQtyByFinishedStockId(stock.getId())).thenReturn(0);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId())).thenReturn(List.of(item));
        when(hcPackagingManualPieceMapper.selectByIdForUpdate(manualPiece.getId())).thenReturn(manualPiece);
        when(hcPackagingManualPieceMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo())).thenReturn(List.of());
        when(hcPackagingCoaSampleClaimMapper.selectListBySource(eq("MANUAL_HISTORY"), eq(List.of(manualPiece.getId()))))
                .thenReturn(List.of());
        when(hcPackagingManualPieceMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo()))
                .thenReturn(List.of(HcPackagingManualPieceDO.builder().id(999L).build()));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.updateImportedStockData(reqVO));

        assertTrue(exception.getMessage().contains("新片号已存在历史导入来源记录"));
        verify(hcPackagingManualPieceMapper, never()).updateById(any(HcPackagingManualPieceDO.class));
        verify(hcFinishedStockMapper, never()).updateById(any(HcFinishedStockDO.class));
        verify(hcPackagingManualPieceCorrectionLogMapper, never())
                .insert(any(HcPackagingManualPieceCorrectionLogDO.class));
    }

    @Test
    void shouldAllowEmptyCorrectionReason() {
        HcFinishedStockDO stock = stock();
        HcInnerPackUnitDO box = box();
        HcInnerPackUnitItemDO item = item();
        HcPackagingManualPieceDO manualPiece = manualPiece();
        ImportedStockDataUpdateReqVO reqVO = request();
        reqVO.setCorrectionReason("  ");

        when(hcFinishedStockMapper.selectByIdForUpdate(stock.getId())).thenReturn(stock);
        when(hcFgShippingNoticeItemMapper.selectActiveLockedQtyByFinishedStockId(stock.getId())).thenReturn(0);
        when(hcInnerPackUnitMapper.selectByUnitNoForUpdate(box.getInnerUnitNo())).thenReturn(box);
        when(hcInnerPackUnitItemMapper.selectListByInnerUnitIdForUpdate(box.getId())).thenReturn(List.of(item));
        when(hcPackagingManualPieceMapper.selectByIdForUpdate(manualPiece.getId())).thenReturn(manualPiece);
        when(hcPackagingManualPieceMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo())).thenReturn(List.of());
        when(hcInnerPackUnitItemMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo())).thenReturn(List.of());
        when(hcFinishedStockMapper.selectListBySliceBatchNoForUpdate(reqVO.getSliceBatchNo())).thenReturn(List.of());
        when(hcPackagingCoaSampleClaimMapper.selectListBySource(eq("MANUAL_HISTORY"), eq(List.of(manualPiece.getId()))))
                .thenReturn(List.of());
        when(hcBomMapper.selectEnabledByMaterialCodeAndModelCode(reqVO.getMaterialCode(), reqVO.getModelCode()))
                .thenReturn(null);

        service.updateImportedStockData(reqVO);

        ArgumentCaptor<HcPackagingManualPieceCorrectionLogDO> logCaptor =
                ArgumentCaptor.forClass(HcPackagingManualPieceCorrectionLogDO.class);
        verify(hcPackagingManualPieceCorrectionLogMapper).insert(logCaptor.capture());
        assertEquals("", logCaptor.getValue().getCorrectionReason());
    }

    private static ImportedStockDataUpdateReqVO request() {
        ImportedStockDataUpdateReqVO reqVO = new ImportedStockDataUpdateReqVO();
        reqVO.setStockId(201L);
        reqVO.setSliceBatchNo("SLICE-NEW");
        reqVO.setSegmentBatchNo("SEG-NEW");
        reqVO.setMaterialCode("M-NEW");
        reqVO.setModelCode("MODEL-NEW");
        reqVO.setProductionDate(LocalDate.of(2026, 8, 1));
        reqVO.setInspectionResult("OK");
        reqVO.setCoaInspectionResult("OK");
        reqVO.setRemark("新备注");
        reqVO.setCorrectionReason("资料更正");
        return reqVO;
    }

    private static HcFinishedStockDO stock() {
        return HcFinishedStockDO.builder()
                .id(201L).tenantId(1L).stockNo("FGS-001").innerUnitNo("IP-001")
                .sliceBatchNo("SLICE-001").materialCode("M-OLD").materialName("旧料号")
                .modelCode("MODEL-OLD").productSize("10X10").productionDate(LocalDate.of(2026, 7, 1))
                .expiryDate(LocalDate.of(2027, 4, 30)).qty(1).qualityStatus("NG").stockStatus("AVAILABLE")
                .build();
    }

    private static HcInnerPackUnitDO box() {
        return HcInnerPackUnitDO.builder()
                .id(101L).tenantId(1L).innerUnitNo("IP-001").unitStatus("INBOUNDED")
                .materialCode("M-OLD").modelCode("MODEL-OLD").productSize("10X10").build();
    }

    private static HcInnerPackUnitItemDO item() {
        return HcInnerPackUnitItemDO.builder()
                .id(301L).innerUnitId(101L).innerUnitNo("IP-001").sliceBatchNo("SLICE-001")
                .sourceType("MANUAL_HISTORY").sourceManualPieceId(401L).qualityStatus("NG").tenantId(1L).build();
    }

    private static HcPackagingManualPieceDO manualPiece() {
        return HcPackagingManualPieceDO.builder()
                .id(401L).tenantId(1L).sliceBatchNo("SLICE-001").segmentBatchNo("SEG-001")
                .innerUnitNo("IP-001").materialCode("M-OLD").materialName("旧料号")
                .modelCode("MODEL-OLD").productSize("10X10").productionDate(LocalDate.of(2026, 7, 1))
                .expiryDate(LocalDate.of(2027, 4, 30)).inspectionResult("NG").coaInspectionResult("OK")
                // 真实历史库存导入记录在来源片层会保留 PACKED，入库状态以库存/包装为准。
                .recordStatus("PACKED").printCount(1).printStatus("PRINTED").remark("旧备注").build();
    }
}
