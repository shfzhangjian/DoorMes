package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.HcProcessReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance.HcLotInstanceMapper;
import cn.iocoder.yudao.module.mes.service.hc.inv.stock.HcInvStockService;
import cn.iocoder.yudao.module.mes.service.hc.productionbatch.HcRootBatchReservation;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderInventoryLockReqVO;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class HcPlanOrderWithdrawalTest {
    private final HcPlanOrderServiceImpl service = new HcPlanOrderServiceImpl();
    private final HcPlanOrderMapper plans = mock(HcPlanOrderMapper.class);
    private final HcPlanOrderOperationMapper operations = mock(HcPlanOrderOperationMapper.class);
    private final HcPlanOrderInventoryLockMapper locks = mock(HcPlanOrderInventoryLockMapper.class);
    private final HcProcessReportMapper reports = mock(HcProcessReportMapper.class);
    private final HcLotInstanceMapper lots = mock(HcLotInstanceMapper.class);
    private final HcPlanOrderOperationStatusLogMapper operationLogs = mock(HcPlanOrderOperationStatusLogMapper.class);
    private final HcPlanOrderStatusLogMapper statusLogs = mock(HcPlanOrderStatusLogMapper.class);
    private final HcInvStockService stocks = mock(HcInvStockService.class);
    private final cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper slices =
            mock(cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper.class);
    private HcPlanOrderDO plan;

    @BeforeEach
    void setup() {
        var assistant = new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "withdraw-test");
        for (Class<?> type : List.of(HcPlanOrderDO.class, HcPlanOrderInventoryLockDO.class, HcPlanOrderOperationDO.class)) {
            com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(assistant, type);
        }
        ReflectionTestUtils.setField(service, "hcSlittingSliceRecordMapper", slices);
        ReflectionTestUtils.setField(service, "hcPlanOrderMapper", plans);
        ReflectionTestUtils.setField(service, "hcPlanOrderOperationMapper", operations);
        ReflectionTestUtils.setField(service, "hcPlanOrderInventoryLockMapper", locks);
        ReflectionTestUtils.setField(service, "hcProcessReportMapper", reports);
        ReflectionTestUtils.setField(service, "hcLotInstanceMapper", lots);
        ReflectionTestUtils.setField(service, "hcPlanOrderOperationStatusLogMapper", operationLogs);
        ReflectionTestUtils.setField(service, "hcPlanOrderStatusLogMapper", statusLogs);
        ReflectionTestUtils.setField(service, "hcInvStockService", stocks);
        plan = HcPlanOrderDO.builder().id(1L).planNo("P1").planStatus("RELEASED")
                .batchNo("W26J171A").batchRuleId(10L)
                .productionBatchContextJson("{\"rootBatchReserved\":true,\"reservationDate\":\"2026-09-17\"}").build();
        when(plans.selectByIdForUpdate(1L)).thenReturn(plan);
        when(operations.selectListByPlanId(1L)).thenReturn(List.of(
                HcPlanOrderOperationDO.builder().id(2L).planId(1L).operationStatus("RELEASED").build()));
        when(locks.selectListByPlanId(1L)).thenReturn(List.of());
    }

    @Test
    void withdrawalKeepsPlanIdentityButClearsReservationAndRecordsHistory() {
        service.withdrawPlanOrder(1L);
        verify(plans).selectByIdForUpdate(1L);
        assertNull(plan.getBatchNo());
        assertNull(plan.getBatchRuleId());
        assertFalse(HcRootBatchReservation.isReserved(plan));
        assertTrue(HcRootBatchReservation.isReplanning(plan));
        assertNotNull(HcRootBatchReservation.context(plan).get("withdrawnReservations"));
        verify(plans).updateById(argThat((HcPlanOrderDO p) -> p.getId().equals(1L) && "DRAFT".equals(p.getPlanStatus())));
        verify(statusLogs).insert(any(HcPlanOrderStatusLogDO.class));
    }

    @Test
    void runningOperationCannotWithdraw() {
        when(operations.selectListByPlanId(1L)).thenReturn(List.of(
                HcPlanOrderOperationDO.builder().operationStatus("RUNNING").build()));
        assertThrows(RuntimeException.class, () -> service.withdrawPlanOrder(1L));
        verify(plans, never()).updateById(any(HcPlanOrderDO.class));
        assertTrue(HcRootBatchReservation.isReserved(plan));
    }

    @Test
    void reportBlocksWithdrawalEvenIfOperationStatusWasReset() {
        when(reports.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThrows(RuntimeException.class, () -> service.withdrawPlanOrder(1L));
        verifyNoInteractions(stocks);
    }

    @Test
    void generatedBatchBlocksWithdrawalEvenWithoutRunningStatus() {
        plan.setProductionBatchNo("W26J171A");
        assertThrows(RuntimeException.class, () -> service.withdrawPlanOrder(1L));
    }

    @Test
    void historicalStartAndConsumptionAlsoBlockWithdrawal() {
        when(operationLogs.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThrows(RuntimeException.class, () -> service.withdrawPlanOrder(1L));
        when(operationLogs.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(locks.selectListByPlanId(1L)).thenReturn(List.of(HcPlanOrderInventoryLockDO.builder()
                .consumedQty(BigDecimal.ONE).build()));
        assertThrows(RuntimeException.class, () -> service.withdrawPlanOrder(1L));
    }

    @Test
    void withdrawalReleasesStockAndRetainsPendingSelection() {
        HcPlanOrderInventoryLockDO lock = HcPlanOrderInventoryLockDO.builder().id(3L).planId(1L)
                .stockId(4L).lockType("WIP").lockStatus("ACTIVE").lockQty(BigDecimal.TEN)
                .remainingQty(BigDecimal.TEN).lockTxnNo("LOCK1").build();
        when(locks.selectListByPlanId(1L)).thenReturn(List.of(lock));
        service.withdrawPlanOrder(1L);
        verify(stocks).releasePlanLockedWip(eq(lock), eq(BigDecimal.TEN), anyString(), any(), any(), anyString());
        assertEquals("PENDING", lock.getLockStatus());
        assertNull(lock.getLockTxnNo());
    }

    @Test
    void withdrawnDraftDoesNotReserveInventoryButReleaseDoes() {
        HcPlanOrderInventoryLockDO lock = HcPlanOrderInventoryLockDO.builder().id(3L).planId(1L)
                .stockId(4L).lockType("WIP").lockStatus("PENDING").lockQty(BigDecimal.TEN)
                .remainingQty(BigDecimal.TEN).build();
        when(locks.selectListByPlanId(1L)).thenReturn(List.of(lock));
        plan.setProductionBatchContextJson("{\"replanAfterWithdrawal\":true}");
        plan.setPlanStatus("DRAFT");
        HcPlanOrderInventoryLockReqVO req = new HcPlanOrderInventoryLockReqVO();
        req.setId(3L); req.setStockId(4L); req.setLockType("WIP"); req.setLockQty(BigDecimal.TEN);
        req.setLockStatus("PENDING");
        ReflectionTestUtils.invokeMethod(service, "updateInventoryLocks", 1L, List.of(req), plan, List.of());
        verifyNoInteractions(stocks);
        plan.setPlanStatus("RELEASED");
        ReflectionTestUtils.invokeMethod(service, "updateInventoryLocks", 1L, List.of(req), plan, List.of());
        verify(stocks).lockPlanWip(any(), eq(BigDecimal.TEN), any(), any(), anyString(), anyString());
    }
    @Test
    void withdrawnDraftCanChangeProductRouteAndDateAndRecalculateQuantities() {
        plan.setPlanStatus("DRAFT");
        plan.setModelId(10L);
        plan.setMaterialId(11L);
        plan.setProductionBatchContextJson("{\"replanAfterWithdrawal\":true}");
        plan.setBatchNo(null);
        var batches = mock(cn.iocoder.yudao.module.mes.service.hc.productionbatch.HcProductionBatchService.class);
        ReflectionTestUtils.setField(service, "hcProductionBatchService", batches);
        var req = new cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderSaveReqVO();
        req.setId(1L); req.setPlanNo("P1"); req.setPlanStatus("DRAFT");
        req.setModelId(20L); req.setModelCode("NEW-MODEL"); req.setMaterialId(21L);
        req.setRouteId(30L); req.setProductionStartDate(java.time.LocalDate.of(2026, 10, 1));
        req.setTargetQty(BigDecimal.TEN); req.setFgDeductQty(BigDecimal.ONE);
        req.setNetPlanQty(new BigDecimal("999"));
        var op = new cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderOperationReqVO();
        op.setId(2L); op.setOpCode("OP-WET"); op.setOpName("湿法");
        op.setYieldRate(new BigDecimal("2")); op.setRequiredQty(new BigDecimal("999"));
        op.setOperationStatus("RUNNING");
        req.setOperations(new java.util.ArrayList<>(List.of(op)));
        req.setInventoryLocks(new java.util.ArrayList<>());
        service.updateHcPlanOrder(req);
        verify(plans).updateById(argThat((HcPlanOrderDO updated) ->
                Long.valueOf(20L).equals(updated.getModelId())
                && Long.valueOf(21L).equals(updated.getMaterialId())
                && Long.valueOf(30L).equals(updated.getRouteId())
                && java.time.LocalDate.of(2026, 10, 1).equals(updated.getProductionStartDate())));
        assertEquals(0, new BigDecimal("9").compareTo(req.getNetPlanQty()));
        assertEquals(0, new BigDecimal("18").compareTo(op.getRequiredQty()));
        assertEquals("NOT_RELEASED", op.getOperationStatus());
        verifyNoInteractions(stocks);
    }

    @Test
    void releasedPlanCannotBypassWithdrawalViaSave() {
        var req = new cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderSaveReqVO();
        req.setId(1L); req.setPlanNo("P1"); req.setPlanStatus("DRAFT");
        assertThrows(RuntimeException.class, () -> service.updateHcPlanOrder(req));
        verify(plans, never()).updateById(any(HcPlanOrderDO.class));
    }

    @Test
    void initializedSlittingTasksCannotBeOrphanedByWithdrawal() {
        when(slices.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThrows(RuntimeException.class, () -> service.withdrawPlanOrder(1L));
        verify(plans, never()).updateById(any(HcPlanOrderDO.class));
    }

}
