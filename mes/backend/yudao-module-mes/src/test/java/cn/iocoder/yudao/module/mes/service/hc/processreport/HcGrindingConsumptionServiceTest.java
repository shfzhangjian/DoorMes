package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcGrindingConsumptionVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding.HcGrindingConsumptionDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.grinding.HcGrindingConsumptionMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger.HcToolingConsumableLedgerService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcGrindingConsumptionServiceTest {
    @InjectMocks HcGrindingConsumptionService service;
    @Mock HcGrindingConsumptionMapper mapper;
    @Mock HcToolingConsumableLedgerMapper ledgerMapper;
    @Mock HcToolingConsumableConsumeMapper consumeMapper;
    @Mock HcToolingConsumableLedgerService ledgerService;
    private final LocalDateTime time = LocalDateTime.of(2026, 9, 21, 10, 20, 30);
    @BeforeEach void tenant() {
        TenantContextHolder.setTenantId(1L);
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "");
        assistant.setCurrentNamespace("grinding-consumption-test");
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, HcGrindingConsumptionDO.class);
    }
    @AfterEach void clearTenant() { TenantContextHolder.clear(); }

    private HcGrindingConsumptionVO value() {
        var value = new HcGrindingConsumptionVO();
        value.setRequestKey("grinding-request-123456");
        value.setSandpaperLedgerId(10L);
        value.setSandpaperQty(new BigDecimal("2.500"));
        return value;
    }
    private HcGrindingConsumptionDO booking() {
        var row = new HcGrindingConsumptionDO();
        row.setId(100L); row.setTenantId(1L); row.setSourceType("FIRST"); row.setSourceId(20L); row.setCancelled(false);
        return row;
    }
    private HcToolingConsumableLedgerDO ledger() {
        return HcToolingConsumableLedgerDO.builder().id(10L).tenantId(1L).processCode("ROUGH_GRINDING")
                .consumableType("SANDPAPER").batchNo("SP001").uomName("张").receiveQty(new BigDecimal("10"))
                .usageStatus("ACTIVE").build();
    }
    private void finish(HcGrindingConsumptionDO row, HcGrindingConsumptionVO value) {
        service.finish(row, 20L, 30L, value, true, "SP001", false, null, time, "P001", "B001");
    }
    @Test void replacementBooksExactPhysicalQuantityAndUnitOnce() {
        when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger());
        when(ledgerService.createConsume(any())).thenReturn(50L);
        var row = booking(); var value = value(); finish(row, value);
        var capture = ArgumentCaptor.forClass(HcToolingConsumableConsumeSaveReqVO.class);
        verify(ledgerService).createConsume(capture.capture());
        assertEquals(new BigDecimal("2.500"), capture.getValue().getConsumeQty());
        assertEquals(time, capture.getValue().getConsumeTime());
        assertEquals(10L, capture.getValue().getLedgerId());
        assertEquals("张", value.getSandpaperUnit());
        assertEquals(50L, row.getSandpaperConsumeId());
        assertEquals(20L, row.getSourceId()); assertEquals(30L, row.getResultId());
    }
    @Test void bothMaterialsBookSeparately() {
        var value = value(); value.setGuideClothLedgerId(11L); value.setGuideClothQty(new BigDecimal("1.2"));
        var guide = ledger(); guide.setId(11L); guide.setConsumableType("GUIDE_CLOTH"); guide.setBatchNo("GC001"); guide.setUomName("米");
        when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger());
        when(ledgerMapper.selectByIdForUpdate(11L)).thenReturn(guide);
        when(ledgerService.createConsume(any())).thenReturn(50L, 51L);
        var row = booking(); service.finish(row, 20L, 20L, value, true, "SP001", true, "GC001", time, null, "B001");
        verify(ledgerService, times(2)).createConsume(any());
        assertEquals(51L, row.getGuideClothConsumeId()); assertEquals("米", value.getGuideClothUnit());
    }
    @Test void missingQuantityAndUnselectedLedgerRejected() {
        var value = value(); value.setSandpaperQty(null);
        assertThrows(RuntimeException.class, () -> finish(booking(), value));
        assertThrows(RuntimeException.class, () -> finish(null, null));
        verifyNoInteractions(ledgerService);
    }
    @Test void nonReplacementCannotInjectConsumption() {
        assertThrows(RuntimeException.class, () -> service.finish(booking(), 20L, 20L, value(), false, "SP001", false, null, time, null, null));
        verifyNoInteractions(ledgerService);
    }
    @Test void wrongProcessTypeBatchAndTenantRejected() {
        for (int i = 0; i < 4; i++) {
            var ledger = ledger();
            if (i == 0) ledger.setProcessCode("WET");
            if (i == 1) ledger.setConsumableType("GUIDE_CLOTH");
            if (i == 2) ledger.setBatchNo("SP002");
            if (i == 3) ledger.setTenantId(2L);
            when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger);
            assertThrows(RuntimeException.class, () -> finish(booking(), value()));
        }
        verifyNoInteractions(ledgerService);
    }
    @Test void insufficientBalancePropagatesWithoutCompletingBooking() {
        when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger());
        when(ledgerService.createConsume(any())).thenThrow(new IllegalArgumentException("余额不足"));
        assertThrows(IllegalArgumentException.class, () -> finish(booking(), value()));
        verify(mapper, never()).updateById(any(HcGrindingConsumptionDO.class));
    }
    @Test void sameRequestReturnsOriginalResultWithoutAnotherConsume() {
        var value = value(); var row = booking(); row.setResultId(30L);
        row.setRequestHash(DigestUtil.sha256Hex(JsonUtils.toJsonString(value)));
        when(mapper.insert(any(HcGrindingConsumptionDO.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(mapper.byRequest(value.getRequestKey())).thenReturn(row);
        assertEquals(30L, service.begin("FIRST", value, value).getResultId());
        verifyNoInteractions(ledgerService);
    }
    @Test void changedRetryAndCancelledRetryRejected() {
        var value = value(); var row = booking(); row.setRequestHash("different");
        when(mapper.insert(any(HcGrindingConsumptionDO.class))).thenThrow(new DuplicateKeyException("duplicate"));
        when(mapper.byRequest(value.getRequestKey())).thenReturn(row);
        assertThrows(RuntimeException.class, () -> service.begin("FIRST", value, value));
        row.setRequestHash(DigestUtil.sha256Hex(JsonUtils.toJsonString(value))); row.setCancelled(true);
        assertThrows(RuntimeException.class, () -> service.begin("FIRST", value, value));
    }
    @Test void cancellationReversesOnceEvenForUsedUpLedger() {
        var row = booking(); row.setSandpaperConsumeId(50L);
        when(mapper.bySource("FIRST", 20L, true)).thenReturn(row);
        when(consumeMapper.selectById(50L)).thenReturn(HcToolingConsumableConsumeDO.builder().id(50L).ledgerId(10L).build());
        var ledger = ledger(); ledger.setUsageStatus("USED_UP"); when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger);
        service.cancel("FIRST", 20L); service.cancel("FIRST", 20L);
        verify(consumeMapper).deleteById(50L); assertTrue(row.getCancelled());
    }
    @Test void returnedLedgerBlocksCancellation() {
        var row = booking(); row.setSandpaperConsumeId(50L);
        when(mapper.bySource("FIRST", 20L, true)).thenReturn(row);
        when(consumeMapper.selectById(50L)).thenReturn(HcToolingConsumableConsumeDO.builder().id(50L).ledgerId(10L).build());
        var ledger = ledger(); ledger.setUsageStatus("RETURNED"); when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger);
        assertThrows(RuntimeException.class, () -> service.cancel("FIRST", 20L));
        verify(consumeMapper, never()).deleteById(anyLong());
    }
    @Test void revisionChangesExistingQuantityInsteadOfAppending() {
        var row = booking(); row.setSandpaperConsumeId(50L);
        when(mapper.bySource("MANUAL", 20L, true)).thenReturn(row);
        var consume = HcToolingConsumableConsumeDO.builder().id(50L).ledgerId(10L).consumeQty(new BigDecimal("2")).build();
        when(consumeMapper.selectById(50L)).thenReturn(consume);
        when(consumeMapper.sumConsumeQtyByLedgerId(10L, 50L)).thenReturn(new BigDecimal("5"));
        var ledger = ledger(); ledger.setUsageStatus("USED_UP"); when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger);
        var value = value(); value.setSandpaperQty(new BigDecimal("3"));
        service.revise("MANUAL", 20L, value, true, "SP001", false, null, time, null, "B001");
        assertEquals(new BigDecimal("3"), consume.getConsumeQty());
        verify(consumeMapper).updateById(consume); verifyNoInteractions(ledgerService);
        value.setSandpaperQty(new BigDecimal("6"));
        assertThrows(RuntimeException.class, () -> service.revise("MANUAL", 20L, value, true, "SP001", false, null, time, null, "B001"));
    }
    @Test void legacyWithoutConsumptionIsNotBackfilled() {
        service.revise("MANUAL", 20L, null, true, "SP001", false, null, time, null, "B001");
        verifyNoInteractions(ledgerService, consumeMapper);
    }
}
