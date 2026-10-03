package cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class HcToolingConsumableCompletionTest {
    @InjectMocks private HcToolingConsumableLedgerServiceImpl service;
    @Mock private HcToolingConsumableLedgerMapper hcToolingConsumableLedgerMapper;
    @Mock private HcToolingConsumableConsumeMapper hcToolingConsumableConsumeMapper;
    @Mock private HcAdhesiveGlueBoardStockMapper hcAdhesiveGlueBoardStockMapper;
    @Mock private HcAdhesiveGlueBoardUsageMapper hcAdhesiveGlueBoardUsageMapper;

    private HcToolingConsumableLedgerDO ledger(String process) {
        return HcToolingConsumableLedgerDO.builder().id(1L).consumableType("GLUE_BOARD")
                .processCode(process).usageStatus("ACTIVE").receiveQty(new BigDecimal("100")).build();
    }
    private HcToolingConsumableLedgerMarkUsedUpReqVO request(String remaining) {
        HcToolingConsumableLedgerMarkUsedUpReqVO req = new HcToolingConsumableLedgerMarkUsedUpReqVO();
        req.setId(1L);
        req.setBalanceQty(remaining == null ? null : new BigDecimal(remaining));
        req.setUsedUpAuthUserName("测试操作人");
        return req;
    }
    private HcAdhesiveGlueBoardStockDO stock() {
        return HcAdhesiveGlueBoardStockDO.builder().id(10L).toolingLedgerId(1L)
                .receiveLength(new BigDecimal("100")).availableLength(new BigDecimal("20"))
                .usedLength(new BigDecimal("80")).lossLength(BigDecimal.ZERO)
                .extraJson("{\"sourceType\":\"TOOLING_CONSUMABLE_LEDGER\",\"toolingLedgerId\":1}").build();
    }
    private void mockStock(HcAdhesiveGlueBoardStockDO stock) {
        when(hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(1L)).thenReturn(stock);
        when(hcAdhesiveGlueBoardStockMapper.selectByIdForUpdate(10L)).thenReturn(stock);
    }

    @ParameterizedTest
    @CsvSource({"ADHESIVE1,10", "ADHESIVE2,10", "ADHESIVE,10", "ADHESIVE1,0", "ADHESIVE2,100", "ADHESIVE1,30"})
    void settleAndReadBackWithoutRewritingReportDetails(String process, String remaining) {
        HcToolingConsumableLedgerDO ledger = ledger(process);
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        mockStock(stock());
        when(hcAdhesiveGlueBoardUsageMapper.selectListByGlueBoardStockId(10L)).thenReturn(List.of());
        service.markLedgerUsedUp(request(remaining));
        ArgumentCaptor<HcToolingConsumableLedgerDO> ledgerUpdate = ArgumentCaptor.forClass(HcToolingConsumableLedgerDO.class);
        verify(hcToolingConsumableLedgerMapper).updateById(ledgerUpdate.capture());
        assertEquals(0, new BigDecimal("20").compareTo(ledgerUpdate.getValue().getUsedUpRemainQty()));
        assertTrue(ledgerUpdate.getValue().getUsedUpRemark().startsWith("[BALANCE_SETTLEMENT_V1]"));
        assertEquals("USED_UP", ledgerUpdate.getValue().getUsageStatus());
        ArgumentCaptor<HcAdhesiveGlueBoardStockDO> stockUpdate = ArgumentCaptor.forClass(HcAdhesiveGlueBoardStockDO.class);
        verify(hcAdhesiveGlueBoardStockMapper).updateById(stockUpdate.capture());
        HcAdhesiveGlueBoardStockDO saved = stockUpdate.getValue();
        assertEquals(0, new BigDecimal(remaining).compareTo(saved.getAvailableLength()));
        assertEquals(0, new BigDecimal("100").subtract(new BigDecimal(remaining)).compareTo(saved.getUsedLength()));
        assertTrue(saved.getExtraJson().contains("ledgerBalanceSettled"));
        verifyNoInteractions(hcToolingConsumableConsumeMapper);
        ledger.setUsageStatus("USED_UP");
        when(hcAdhesiveGlueBoardStockMapper.selectByToolingLedgerId(1L)).thenReturn(saved);
        when(hcToolingConsumableLedgerMapper.selectPage(any())).thenReturn(new PageResult<>(List.of(ledger), 1L));
        when(hcToolingConsumableConsumeMapper.selectListByLedgerIds(List.of(1L))).thenReturn(List.of());
        HcToolingConsumableLedgerDO result = service.getLedgerPage(new HcToolingConsumableLedgerPageReqVO()).getList().get(0);
        assertEquals(0, new BigDecimal(remaining).compareTo(result.getBalanceQty()));
        assertEquals(0, new BigDecimal("100").subtract(new BigDecimal(remaining)).compareTo(result.getConsumedQty()));
    }

    @Test
    void settleActiveUsageWithSamplingAndLoss() {
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger("ADHESIVE2"));
        HcAdhesiveGlueBoardStockDO stock = stock();
        stock.setLossLength(new BigDecimal("3"));
        mockStock(stock);
        HcAdhesiveGlueBoardUsageDO usage = HcAdhesiveGlueBoardUsageDO.builder().id(5L)
                .operationCode("ADHESIVE2").usageStatus("ACTIVE").receiveLength(new BigDecimal("100"))
                .aqcSampleLength(new BigDecimal("2")).lossLength(new BigDecimal("3"))
                .availableLength(new BigDecimal("20")).consumedLength(new BigDecimal("75")).build();
        when(hcAdhesiveGlueBoardUsageMapper.selectListByGlueBoardStockId(10L)).thenReturn(List.of(usage));
        service.markLedgerUsedUp(request("10"));
        ArgumentCaptor<HcAdhesiveGlueBoardUsageDO> update = ArgumentCaptor.forClass(HcAdhesiveGlueBoardUsageDO.class);
        verify(hcAdhesiveGlueBoardUsageMapper).updateById(update.capture());
        assertEquals(0, new BigDecimal("85").compareTo(update.getValue().getConsumedLength()));
        assertEquals(0, new BigDecimal("10").compareTo(update.getValue().getAvailableLength()));
        assertEquals("USED_UP", update.getValue().getUsageStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0.0001"})
    void rejectInvalidQuantityBeforeStockMutation(String remaining) {
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger("ADHESIVE1"));
        assertThrows(RuntimeException.class, () -> service.markLedgerUsedUp(request(remaining)));
        verifyNoInteractions(hcAdhesiveGlueBoardStockMapper);
    }

    @Test
    void rejectOverReceiveAndRepeatedCompletion() {
        HcToolingConsumableLedgerDO ledger = ledger("ADHESIVE1");
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        mockStock(stock());
        assertThrows(RuntimeException.class, () -> service.markLedgerUsedUp(request("101")));
        ledger.setUsageStatus("USED_UP");
        assertThrows(RuntimeException.class, () -> service.markLedgerUsedUp(request("10")));
        verify(hcToolingConsumableLedgerMapper, never()).updateById(any(HcToolingConsumableLedgerDO.class));
        verify(hcAdhesiveGlueBoardStockMapper, never()).updateById(any(HcAdhesiveGlueBoardStockDO.class));
    }

    @Test
    void rejectLegacyAdhesiveRequestAndDirectStatusChange() {
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger("ADHESIVE1"));
        HcToolingConsumableLedgerMarkUsedUpReqVO req = request(null);
        req.setUsedUpRemainQty(new BigDecimal("20"));
        assertThrows(RuntimeException.class, () -> service.markLedgerUsedUp(req));
        assertThrows(RuntimeException.class, () -> service.updateLedgerUsageStatus(1L, "USED_UP"));
        verifyNoInteractions(hcAdhesiveGlueBoardStockMapper);
    }

    @Test
    void completedStockCannotBeSelectedAgainEvenWithPositiveBalance() {
        HcAdhesiveGlueBoardStockDO stock = stock();
        stock.setAvailableLength(new BigDecimal("10"));
        stock.setExtraJson("{\"ledgerBalanceSettled\":true}");
        var reportService = new cn.iocoder.yudao.module.mes.service.hc.processreport.HcProcessReportServiceImpl();
        for (String process : List.of("ADHESIVE", "ADHESIVE2")) {
            Boolean allowed = org.springframework.test.util.ReflectionTestUtils.invokeMethod(reportService,
                    "isToolingLedgerGlueBoardStockAllowedForProcess", stock, process);
            assertEquals(Boolean.FALSE, allowed);
        }
    }

    @Test
    void completedLedgerCannotBeEditedOrDeleted() {
        HcToolingConsumableLedgerDO ledger = ledger("ADHESIVE1");
        ledger.setUsageStatus("USED_UP");
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        HcToolingConsumableLedgerSaveReqVO req = new HcToolingConsumableLedgerSaveReqVO();
        req.setId(1L);
        assertThrows(RuntimeException.class, () -> service.updateLedger(req));
        assertThrows(RuntimeException.class, () -> service.deleteLedger(1L));
        verifyNoInteractions(hcAdhesiveGlueBoardStockMapper);
    }

    @Test
    void preserveWetCompletionSemantics() {
        HcToolingConsumableLedgerDO ledger = ledger("WET");
        ledger.setConsumableType("PET");
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(1L)).thenReturn(ledger);
        HcToolingConsumableLedgerMarkUsedUpReqVO req = request(null);
        req.setUsedUpRemainQty(new BigDecimal("20"));
        req.setUsedUpActualDate(LocalDate.of(2026, 9, 22));
        service.markLedgerUsedUp(req);
        ArgumentCaptor<HcToolingConsumableLedgerDO> update = ArgumentCaptor.forClass(HcToolingConsumableLedgerDO.class);
        verify(hcToolingConsumableLedgerMapper).updateById(update.capture());
        assertEquals(req.getUsedUpRemainQty(), update.getValue().getUsedUpRemainQty());
        assertEquals(req.getUsedUpActualDate(), update.getValue().getUsedUpActualDate());
        verifyNoInteractions(hcAdhesiveGlueBoardStockMapper);
    }
}
