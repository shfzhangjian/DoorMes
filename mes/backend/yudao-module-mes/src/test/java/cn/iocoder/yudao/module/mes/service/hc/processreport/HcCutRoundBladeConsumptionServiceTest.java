package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotConsumableReplaceReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HcCutRoundBladeConsumptionServiceTest {
    @InjectMocks HcCutRoundBladeConsumptionService service;
    @Mock HcCutRoundSpareMapper spareMapper;
    @Mock HcCutRoundSpareRecordMapper recordMapper;
    @Mock HcToolingConsumableLedgerMapper ledgerMapper;
    @Mock HcToolingConsumableConsumeMapper consumeMapper;
    @BeforeEach void init() {
        TenantContextHolder.setTenantId(1L);
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "");
        assistant.setCurrentNamespace("blade-test");
        for (var clazz : new Class<?>[]{HcCutRoundSpareDO.class, HcCutRoundSpareRecordDO.class, HcToolingConsumableConsumeDO.class}) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, clazz);
        }
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    HcPressSlotConsumableReplaceReqVO request() {
        var req = new HcPressSlotConsumableReplaceReqVO();
        req.setRequestKey("blade-request-12345678"); req.setLedgerId(10L); req.setReplaceQuantity(BigDecimal.ONE);
        req.setConsumableType("CUTTING_BLADE"); req.setInitialUseCount(0);
        req.setReplaceTime(LocalDateTime.of(2026,9,23,10,0)); req.setReplaceReason("刀片磨损");
        req.setOperatorId(20L); req.setOperatorName("测试人员"); req.setPlanNo("P001");
        return req;
    }
    HcPlanOrderOperationDO operation() {
        var op = new HcPlanOrderOperationDO(); op.setId(2L); op.setTenantId(1L); op.setPlanId(3L); return op;
    }
    HcCutRoundSpareDO state() {
        return HcCutRoundSpareDO.builder().id(4L).tenantId(1L).equipmentId(5L).spareType("CUTTING_BLADE")
                .equipmentCode("CUT01").useCount(500).onlineQuantity(BigDecimal.ONE).build();
    }
    HcToolingConsumableLedgerDO ledger() {
        return HcToolingConsumableLedgerDO.builder().id(10L).tenantId(1L).processCode("CUT_ROUND")
                .consumableType("BLADE").usageStatus("ACTIVE").receiveQty(new BigDecimal("5"))
                .batchNo("B001").model("M1").erpMaterialCode("D001").build();
    }
    void stateReady() { when(spareMapper.selectForUpdate(5L,"CUTTING_BLADE")).thenReturn(state()); }
    void inventoryReady() {
        stateReady(); when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger());
        when(consumeMapper.sumConsumeQtyForUpdate(10L)).thenReturn(BigDecimal.ZERO);
    }
    @Test void replacementConsumesOneAndResetsLifeWithLinkedAudit() {
        inventoryReady(); var req=request();
        doAnswer(inv->{inv.<HcToolingConsumableConsumeDO>getArgument(0).setId(30L);return 1;})
                .when(consumeMapper).insert(any(HcToolingConsumableConsumeDO.class));
        assertEquals(4L,service.replace(req,operation(),5L));
        var consume=ArgumentCaptor.forClass(HcToolingConsumableConsumeDO.class);
        verify(consumeMapper).insert(consume.capture());
        assertEquals(BigDecimal.ONE,consume.getValue().getConsumeQty());
        assertEquals(req.getReplaceTime(),consume.getValue().getConsumeTime());
        assertEquals("CUT_ROUND_BLADE_REPLACE",consume.getValue().getConsumeSource());
        var record=ArgumentCaptor.forClass(HcCutRoundSpareRecordDO.class);verify(recordMapper).updateById(record.capture());
        assertEquals(30L,record.getValue().getConsumeId());assertEquals(10L,record.getValue().getLedgerId());
        assertEquals(new BigDecimal("4"),record.getValue().getAfterAvailableQuantity());
        assertEquals(500,record.getValue().getBeforeUseCount());assertEquals(0,record.getValue().getAfterUseCount());
        var update=ArgumentCaptor.forClass(HcCutRoundSpareDO.class);verify(spareMapper).updateById(update.capture());
        assertEquals(0,update.getValue().getUseCount());assertEquals(req.getReplaceTime(),update.getValue().getLastReplaceTime());
        assertEquals("B001",update.getValue().getBatchNo());
    }
    @Test void insufficientBalanceDoesNotChangeLifeOrWriteConsumption() {
        stateReady();when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger());
        when(consumeMapper.sumConsumeQtyForUpdate(10L)).thenReturn(new BigDecimal("5"));
        assertThrows(RuntimeException.class,()->service.replace(request(),operation(),5L));
        verify(consumeMapper,never()).insert(any(HcToolingConsumableConsumeDO.class));
        verify(spareMapper,never()).updateById(any(HcCutRoundSpareDO.class));
        verify(recordMapper,never()).updateById(any(HcCutRoundSpareRecordDO.class));
    }
    @Test void quantityLargerThanRemainingIsRejected() {
        inventoryReady();var req=request();req.setReplaceQuantity(new BigDecimal("6"));
        assertThrows(RuntimeException.class,()->service.replace(req,operation(),5L));
        verify(consumeMapper,never()).insert(any(HcToolingConsumableConsumeDO.class));
    }
    @Test void missingLedgerIsRejectedBeforeWrites() {
        var req=request();req.setLedgerId(null);assertThrows(RuntimeException.class,()->service.replace(req,operation(),5L));
        verifyNoInteractions(spareMapper,ledgerMapper,consumeMapper,recordMapper);
    }
    @Test void invalidQuantitiesAndInitialLifeRejected() {
        for (var qty:new BigDecimal[]{BigDecimal.ZERO,new BigDecimal("-1"),new BigDecimal("0.5")}) {
            var req=request();req.setReplaceQuantity(qty);assertThrows(RuntimeException.class,()->service.replace(req,operation(),5L));
        }
        var req=request();req.setInitialUseCount(5);assertThrows(RuntimeException.class,()->service.replace(req,operation(),5L));
        verifyNoInteractions(spareMapper,ledgerMapper,consumeMapper,recordMapper);
    }
    @Test void invalidRequestTimeAndReasonRejected() {
        var req=request();req.setRequestKey(null);var missingKey=req;assertThrows(RuntimeException.class,()->service.replace(missingKey,operation(),5L));
        req=request();req.setReplaceTime(null);var missingTime=req;assertThrows(RuntimeException.class,()->service.replace(missingTime,operation(),5L));
        req=request();req.setReplaceReason(" ");var missingReason=req;assertThrows(RuntimeException.class,()->service.replace(missingReason,operation(),5L));
    }
    @Test void wrongTenantProcessOrTypeRejected() {
        stateReady();
        for(int i=0;i<3;i++) {
            var ledger=ledger();if(i==0)ledger.setTenantId(2L);if(i==1)ledger.setProcessCode("PRESS_SLOT");if(i==2)ledger.setConsumableType("FELT");
            when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(ledger);
            assertThrows(RuntimeException.class,()->service.replace(request(),operation(),5L));
        }
        verify(consumeMapper,never()).insert(any(HcToolingConsumableConsumeDO.class));
    }
    @Test void completedAndReturnedLedgersRejected() {
        stateReady();for(var status:new String[]{"USED_UP","RETURNED"}) {
            var row=ledger();row.setUsageStatus(status);when(ledgerMapper.selectByIdForUpdate(10L)).thenReturn(row);
            assertThrows(RuntimeException.class,()->service.replace(request(),operation(),5L));
        }
        verifyNoInteractions(consumeMapper);
    }
    @Test void retryReturnsOriginalResultWithoutFurtherDeduction() {
        stateReady();var req=request();var old=HcCutRoundSpareRecordDO.builder().spareId(4L).equipmentId(5L).planOperationId(2L)
                .requestHash(cn.hutool.crypto.digest.DigestUtil.sha256Hex(cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(req))).build();
        doThrow(new org.springframework.dao.DuplicateKeyException("retry")).when(recordMapper).insert(any(HcCutRoundSpareRecordDO.class));
        when(recordMapper.byRequest(req.getRequestKey())).thenReturn(old);
        assertEquals(4L,service.replace(req,operation(),5L));verifyNoInteractions(ledgerMapper,consumeMapper);
        verify(spareMapper,never()).updateById(any(HcCutRoundSpareDO.class));
    }
    @Test void reusedKeyWithChangedContentRejected() {
        stateReady();doThrow(new org.springframework.dao.DuplicateKeyException("retry")).when(recordMapper).insert(any(HcCutRoundSpareRecordDO.class));
        when(recordMapper.byRequest(anyString())).thenReturn(HcCutRoundSpareRecordDO.builder().requestHash("different").build());
        assertThrows(RuntimeException.class,()->service.replace(request(),operation(),5L));verifyNoInteractions(ledgerMapper,consumeMapper);
    }
}
