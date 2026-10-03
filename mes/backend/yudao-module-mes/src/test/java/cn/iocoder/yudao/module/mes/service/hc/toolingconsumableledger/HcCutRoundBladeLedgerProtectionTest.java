package cn.iocoder.yudao.module.mes.service.hc.toolingconsumableledger;
import cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo.HcToolingConsumableConsumeSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcCutRoundBladeLedgerProtectionTest {
    @InjectMocks HcToolingConsumableLedgerServiceImpl service;
    @Mock HcToolingConsumableConsumeMapper hcToolingConsumableConsumeMapper;
    @Mock HcToolingConsumableLedgerMapper hcToolingConsumableLedgerMapper;
    HcToolingConsumableConsumeDO consumption() {
        return HcToolingConsumableConsumeDO.builder().id(20L).ledgerId(10L)
                .consumeSource("CUT_ROUND_BLADE_REPLACE").consumeQty(BigDecimal.ONE).build();
    }
    @Test void automaticallyCreatedConsumptionCannotBeEdited() {
        when(hcToolingConsumableConsumeMapper.selectById(20L)).thenReturn(consumption());
        var req=new HcToolingConsumableConsumeSaveReqVO();req.setId(20L);
        assertThrows(RuntimeException.class,()->service.updateConsume(req));
        verify(hcToolingConsumableConsumeMapper,never()).updateById(any(HcToolingConsumableConsumeDO.class));
    }
    @Test void automaticallyCreatedConsumptionCannotBeDeleted() {
        when(hcToolingConsumableConsumeMapper.selectById(20L)).thenReturn(consumption());
        assertThrows(RuntimeException.class,()->service.deleteConsume(20L));
        verify(hcToolingConsumableConsumeMapper,never()).deleteById(20L);
    }
    @Test void ledgerDeletionCannotEraseReplacementTrace() {
        when(hcToolingConsumableLedgerMapper.selectByIdForUpdate(10L)).thenReturn(HcToolingConsumableLedgerDO.builder()
                .id(10L).consumableType("BLADE").processCode("CUT_ROUND").usageStatus("ACTIVE").build());
        when(hcToolingConsumableConsumeMapper.selectListForUpdate(10L)).thenReturn(List.of(consumption()));
        assertThrows(RuntimeException.class,()->service.deleteLedger(10L));
        verify(hcToolingConsumableConsumeMapper,never()).deleteByLedgerId(10L);
        verify(hcToolingConsumableLedgerMapper,never()).deleteById(10L);
    }
    @Test void linkedLedgerCannotBeReplenishedByEditingOriginalReceipt() {
        var old=HcToolingConsumableLedgerDO.builder().id(10L).consumableType("BLADE").receiveQty(BigDecimal.ONE).build();
        var next=HcToolingConsumableLedgerDO.builder().id(10L).consumableType("BLADE").receiveQty(BigDecimal.TEN).build();
        when(hcToolingConsumableConsumeMapper.selectListForUpdate(10L)).thenReturn(List.of(consumption()));
        assertThrows(RuntimeException.class,()->ReflectionTestUtils.invokeMethod(service,"assertBladeLedgerUnchanged",old,next));
    }
}
