package cn.iocoder.yudao.module.mes.service.hc.nginventory;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.stock.HcInvStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.nginventory.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.stock.HcInvStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.inv.txn.HcInvTxnLogMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcNgFixedWarehouseTest {
    @InjectMocks HcNgInventoryServiceImpl service;
    @Mock HcNgInventoryPieceMapper pieceMapper;
    @Mock HcNgInventoryLocationMapper locationMapper;
    @Mock HcNgInventoryWarehouseMapper warehouseMapper;
    @Mock HcNgInventoryRackMapper rackMapper;
    @Mock HcInvStockMapper stockMapper;
    @Mock HcInvTxnLogMapper txnMapper;
    @Mock HcNgManualPieceMapper manualMapper;

    @Test void routesByAttributedProcessAndFreezeTakesPriority() {
        assertEquals("NG_SLITTING", HcNgFixedWarehousePolicy.warehouseCode("SLITTING", false));
        assertEquals("NG_PRESS_SLOT", HcNgFixedWarehousePolicy.warehouseCode("PRESS_SLOT", false));
        assertEquals("NG_FREEZE", HcNgFixedWarehousePolicy.warehouseCode("SLITTING", true));
        assertEquals("NG_FREEZE", HcNgFixedWarehousePolicy.warehouseCode("PRESS_SLOT", true));
        assertThrows(IllegalArgumentException.class, () -> HcNgFixedWarehousePolicy.warehouseCode(null, true));
        assertThrows(IllegalArgumentException.class, () -> HcNgFixedWarehousePolicy.warehouseCode("ADHESIVE2", false));
    }
    @Test void staleClientsCannotShelfUnshelfTransferOrEditLocations() {
        assertThrows(ServiceException.class, () -> service.shelf(null));
        assertThrows(ServiceException.class, () -> service.unshelf(null));
        assertThrows(ServiceException.class, () -> service.transfer(null));
        assertThrows(ServiceException.class, () -> service.saveWarehouse(null));
        assertThrows(ServiceException.class, () -> service.deleteWarehouse(1L));
        assertThrows(ServiceException.class, () -> service.saveRack(null));
        assertThrows(ServiceException.class, () -> service.deleteRack(1L));
        verifyNoInteractions(stockMapper, pieceMapper, locationMapper);
    }
    @Test void ngPieceDirectlyCreatesStockAndKeepsSourceBatch() { direct(false,"NG"); }
    @Test void frozenPieceIsNotMadeAvailable() { direct(true,"NG"); }
    @Test void qualifiedFrozenPieceRetainsQuality() { direct(true,"OK"); }
    private void direct(boolean frozen, String quality) {
        String code=HcNgFixedWarehousePolicy.warehouseCode("SLITTING", frozen);
        String key=HcNgFixedWarehousePolicy.locationKey("SLITTING", frozen);
        var target=HcNgInventoryLocationDO.builder().id(1L).warehouseId(1L).rackId(1L).locationNo(0)
                .locationKey(key).warehouseCode(code).warehouseName("固定库").locationName("固定库")
                .status("启用").capacityQty(0).build();
        when(locationMapper.selectByLocationKeyForUpdate(key)).thenReturn(target);
        var warehouse=new HcNgInventoryWarehouseDO(); warehouse.setId(1L); warehouse.setStatus("启用");
        when(warehouseMapper.selectById(1L)).thenReturn(warehouse);
        var rack=new HcNgInventoryRackDO(); rack.setId(1L); rack.setStatus("启用");
        when(rackMapper.selectById(1L)).thenReturn(rack);
        var piece=HcNgInventoryPieceDO.builder().sourceType("SLITTING").sourceId(10L).processType("SLITTING")
                .sourceBatchNo("SOURCE-SEGMENT").pieceNo("PIECE-001").padType("WHITE_PAD")
                .pieceQty(BigDecimal.ONE).qualityResult(quality).tenantId(1L).build();
        doAnswer(call -> { ((HcNgInventoryPieceDO)call.getArgument(0)).setId(20L); return 1; }).when(pieceMapper).insert(any(HcNgInventoryPieceDO.class));
        doAnswer(call -> { ((HcInvStockDO)call.getArgument(0)).setId(30L); return 1; }).when(stockMapper).insert(any(HcInvStockDO.class));
        ReflectionTestUtils.invokeMethod(service,"insertDirectPiece",piece,frozen);
        assertEquals(frozen?"FROZEN":"STORED",piece.getStatus());
        assertEquals(key,piece.getCurrentLocationCode());assertEquals(30L,piece.getStockId());
        verify(stockMapper).insert(argThat((HcInvStockDO st) -> "PIECE-001".equals(st.getBatchNo())
                && "SOURCE-SEGMENT".equals(st.getSourceBatchNo()) && st.getAvailableQty().signum()==0
                && quality.equals(st.getQualityStatus()) && st.getFrozenQty().intValue()==(frozen?1:0)));
        verify(txnMapper).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO.class));
        verify(locationMapper).updateById(argThat((HcNgInventoryLocationDO l) -> l.getOccupiedQty()==1));
    }
    @Test void missingFixedLocationFailsWithoutWritingStock() {
        var piece=HcNgInventoryPieceDO.builder().processType("PRESS_SLOT").build();
        assertThrows(ServiceException.class, () -> ReflectionTestUtils.invokeMethod(service,"insertDirectPiece",piece,false));
        verifyNoInteractions(stockMapper,txnMapper);
    }
    @Test void manualUnfreezeKeepsQuantityAndReturnsToAttributedWarehouse() {
        String freezeKey=HcNgFixedWarehousePolicy.locationKey("PRESS_SLOT",true);
        String normalKey=HcNgFixedWarehousePolicy.locationKey("PRESS_SLOT",false);
        var freeze=HcNgInventoryLocationDO.builder().id(1L).warehouseId(1L).rackId(1L).locationNo(0)
                .locationKey(freezeKey).warehouseCode("NG_FREEZE").status("启用").occupiedQty(1).build();
        var normal=HcNgInventoryLocationDO.builder().id(2L).warehouseId(2L).rackId(2L).locationNo(0)
                .locationKey(normalKey).warehouseCode("NG_PRESS_SLOT").locationName("压槽库").status("启用").occupiedQty(4).build();
        var piece=HcNgInventoryPieceDO.builder().id(10L).sourceType("MANUAL_HISTORY").sourceId(20L)
                .status("FROZEN").stockId(30L).currentLocationCode(freezeKey).processType("PRESS_SLOT")
                .pieceNo("HISTORY-001").sourceBatchNo("HISTORY").pieceQty(BigDecimal.ONE).qualityResult("NG").build();
        when(pieceMapper.selectByIdForUpdate(10L)).thenReturn(piece);
        when(manualMapper.selectByIdForUpdate(20L)).thenReturn(HcNgManualPieceDO.builder().id(20L).build());
        when(locationMapper.selectActiveListForUpdate()).thenReturn(List.of(freeze,normal));
        when(locationMapper.selectByLocationKeyForUpdate(normalKey)).thenReturn(normal);
        var warehouse=new HcNgInventoryWarehouseDO();warehouse.setStatus("启用");
        var rack=new HcNgInventoryRackDO();rack.setStatus("启用");
        when(warehouseMapper.selectById(2L)).thenReturn(warehouse);
        when(rackMapper.selectById(2L)).thenReturn(rack);
        var oldStock=HcInvStockDO.builder().id(30L).sourceId(10L).onHandQty(BigDecimal.ONE)
                .frozenQty(BigDecimal.ONE).qualityStatus("NG").build();
        when(stockMapper.selectByIdForUpdate(30L)).thenReturn(oldStock);
        doAnswer(call -> { ((HcInvStockDO)call.getArgument(0)).setId(31L);return 1; }).when(stockMapper).insert(any(HcInvStockDO.class));
        var req=new cn.iocoder.yudao.module.mes.controller.admin.hc.nginventory.vo.HcNgInventoryVO.NgManualPieceUnfreezeReqVO();
        req.setPieceId(10L);req.setUnfreezeReason("解除冻结，继续保留不合格库存");
        service.unfreezeManualPiece(req);
        assertEquals("STORED",piece.getStatus());assertEquals(normalKey,piece.getCurrentLocationCode());assertEquals(31L,piece.getStockId());
        assertEquals(BigDecimal.ZERO,oldStock.getOnHandQty());assertEquals(BigDecimal.ZERO,oldStock.getFrozenQty());
        verify(manualMapper).updateById(argThat((HcNgManualPieceDO m) -> "STORED".equals(m.getRecordStatus()) && "NORMAL".equals(m.getStorageTarget())));
        verify(locationMapper).updateById(argThat((HcNgInventoryLocationDO l) -> l.getId()==1L && l.getOccupiedQty()==0));
        verify(locationMapper).updateById(argThat((HcNgInventoryLocationDO l) -> l.getId()==2L && l.getOccupiedQty()==5));
        verify(txnMapper).insert(argThat((cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO t) -> "NG_TRANSFER_OUT".equals(t.getTxnType()) && t.getTxnQty().intValue()==-1));
        verify(txnMapper).insert(argThat((cn.iocoder.yudao.module.mes.dal.dataobject.hc.inv.txn.HcInvTxnLogDO t) -> "NG_TRANSFER_IN".equals(t.getTxnType()) && t.getTxnQty().intValue()==1));
    }

}
