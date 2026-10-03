package cn.iocoder.yudao.module.mes.service.hc.productionbatch;

import cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo.HcLotRuleGenerateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule.HcLotRuleDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotrule.HcLotRuleMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.lotinstance.HcLotInstanceMapper;
import cn.iocoder.yudao.module.mes.service.hc.lotrule.HcLotRuleService;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineResolverService;
import cn.iocoder.yudao.module.mes.service.hc.workcenter.HcProductionLineContext;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcRootBatchReservationTest {
    private final HcProductionBatchServiceImpl service = spy(new HcProductionBatchServiceImpl());
    private final HcPlanOrderMapper plans = mock(HcPlanOrderMapper.class);
    private final HcPlanOrderOperationMapper operations = mock(HcPlanOrderOperationMapper.class);
    private final HcLotRuleMapper rules = mock(HcLotRuleMapper.class);
    private final HcLotRuleService generator = mock(HcLotRuleService.class);
    private final HcLotInstanceMapper instances = mock(HcLotInstanceMapper.class);
    private final HcProductionLineResolverService lines = mock(HcProductionLineResolverService.class);
    private final Map<Long, HcPlanOrderDO> saved = new LinkedHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(170);

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "hcPlanOrderMapper", plans);
        ReflectionTestUtils.setField(service, "hcPlanOrderOperationMapper", operations);
        ReflectionTestUtils.setField(service, "hcLotRuleMapper", rules);
        ReflectionTestUtils.setField(service, "hcLotRuleService", generator);
        ReflectionTestUtils.setField(service, "hcLotInstanceMapper", instances);
        ReflectionTestUtils.setField(service, "hcProductionLineResolverService", lines);
        when(plans.selectByIdForUpdate(anyLong())).thenAnswer(i -> saved.get(i.getArgument(0)));
        when(plans.updateById(any(HcPlanOrderDO.class))).thenAnswer(i -> {
            HcPlanOrderDO update = i.getArgument(0);
            HcPlanOrderDO plan = saved.get(update.getId());
            plan.setBatchNo(update.getBatchNo());
            plan.setProductionBatchContextJson(update.getProductionBatchContextJson());
            return 1;
        });
        when(rules.selectById(10L)).thenReturn(HcLotRuleDO.builder().id(10L)
                .ruleCode("WHITE-W33").versionNo(1).build());
        when(lines.resolveByWorkCenterId(20L)).thenReturn(
                HcProductionLineContext.builder().workCenterId(20L).batchLineCode("A").build());
        when(generator.generateLotNo(any())).thenAnswer(i -> {
            HcLotRuleGenerateReqVO request = i.getArgument(0);
            assertTrue(request.getConsumeSequence());
            int value = sequence.incrementAndGet();
            return Map.of("lotNo", "W26J" + value + "A", "segmentValues",
                    Map.of("ANNUAL_SEQ", String.valueOf(value), "YEAR", "26", "MONTH", "J", "LINE", "A"));
        });
    }

    private HcPlanOrderDO plan(long id, String status) {
        HcPlanOrderDO plan = HcPlanOrderDO.builder().id(id).planNo("P" + id).planStatus(status)
                .batchStatus("NOT_GEN").batchRuleId(10L).productionStartDate(LocalDate.of(2026, 9, 11)).build();
        saved.put(id, plan);
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder().id(id * 10).planId(id)
                .opSeq(1).sort(1).opCode("WC-MIX").opName("配料").workCenterId(20L)
                .operationStatus("RELEASED").build();
        when(operations.selectListByPlanId(id)).thenReturn(List.of(operation));
        when(operations.selectById(id * 10)).thenReturn(operation);
        return plan;
    }

    @Test
    void releaseTwoPlansAllocatesDistinctNumbersWithoutStartingProduction() {
        HcPlanOrderDO first = plan(1, "RELEASED");
        HcPlanOrderDO second = plan(2, "RELEASED");
        service.reserveRootBatchOnPlanRelease(1L);
        service.reserveRootBatchOnPlanRelease(2L);
        assertEquals("W26J171A", first.getBatchNo());
        assertEquals("W26J172A", second.getBatchNo());
        assertTrue(HcRootBatchReservation.isReserved(first));
        assertNull(first.getProductionBatchNo());
        assertEquals("NOT_GEN", first.getBatchStatus());
        verify(instances, never()).insert(any(cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotinstance.HcLotInstanceDO.class));
        verify(operations, never()).updateById(any(HcPlanOrderOperationDO.class));
    }

    @Test
    void repeatedReleaseIsIdempotentAndWithdrawnPlanCanChooseAutomaticNumber() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        service.reserveRootBatchOnPlanRelease(1L);
        service.reserveRootBatchOnPlanRelease(1L);
        assertEquals("W26J171A", plan.getBatchNo());
        plan.setProductionBatchContextJson(HcRootBatchReservation.withdrawnContext(plan));
        plan.setBatchNo(null);
        plan.setPlanStatus("DRAFT");
        service.reserveRootBatchOnPlanRelease(1L);
        assertFalse(HcRootBatchReservation.isReserved(plan));
        assertNotNull(HcRootBatchReservation.context(plan).get("withdrawnReservations"));
        plan.setPlanStatus("RELEASED");
        service.reserveRootBatchOnPlanRelease(1L);
        assertEquals("W26J172A", plan.getBatchNo());
        verify(generator, times(2)).generateLotNo(any());
    }

    @Test
    void withdrawnNumberCanBeSelectedAgainEvenWithLegacyRetiredHistory() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        plan.setBatchNo("W26J171A");
        plan.setProductionBatchContextJson("{\"retiredBatchNos\":[\"W26J171A\"],\"replanAfterWithdrawal\":true}");
        service.reserveRootBatchOnPlanRelease(1L);
        assertEquals("W26J171A", plan.getBatchNo());
        assertTrue(HcRootBatchReservation.isReserved(plan));
        verifyNoInteractions(generator);
    }

    @Test
    void selectedNumberStillRejectsCurrentOwner() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        plan.setBatchNo("W26J171A");
        when(plans.selectOne(any(Wrapper.class))).thenReturn(HcPlanOrderDO.builder().id(2L).build());
        assertThrows(RuntimeException.class, () -> service.reserveRootBatchOnPlanRelease(1L));
        verify(plans, never()).updateById(any(HcPlanOrderDO.class));
    }

    @Test
    void automaticNumberSkipsCurrentManualOccupancy() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        when(plans.selectOne(any(Wrapper.class)))
                .thenReturn(HcPlanOrderDO.builder().id(2L).batchNo("W26J171A").build(), null);
        service.reserveRootBatchOnPlanRelease(1L);
        assertEquals("W26J172A", plan.getBatchNo());
        verify(generator, times(2)).generateLotNo(any());
    }

    @Test
    void draftDoesNotConsumeSequence() {
        plan(1, "DRAFT");
        service.reserveRootBatchOnPlanRelease(1L);
        verifyNoInteractions(generator);
        verify(plans, never()).updateById(any(HcPlanOrderDO.class));
    }

    @Test
    void manualNumberIsReservedWithoutReplacingIt() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        plan.setBatchNo("W26J190A");
        service.reserveRootBatchOnPlanRelease(1L);
        assertEquals("W26J190A", plan.getBatchNo());
        assertTrue(HcRootBatchReservation.isReserved(plan));
        verifyNoInteractions(generator);
    }

    @Test
    void inventoryContinuationAndAlreadyGeneratedPlansAreUntouched() {
        HcPlanOrderDO inventory = plan(1, "RELEASED");
        inventory.setInventorySourceBatchNos("W26J100A");
        HcPlanOrderDO generated = plan(2, "RELEASED");
        generated.setProductionBatchNo("W26J101A");
        service.reserveRootBatchOnPlanRelease(1L);
        service.reserveRootBatchOnPlanRelease(2L);
        verifyNoInteractions(generator);
        verify(plans, never()).updateById(any(HcPlanOrderDO.class));
    }

    @Test
    void nonFormulaFirstOperationIsNotAssignedARootBatch() {
        plan(1, "RELEASED");
        when(operations.selectListByPlanId(1L)).thenReturn(List.of(HcPlanOrderOperationDO.builder()
                .id(10L).opSeq(1).opCode("WC-GRIND").opName("磨皮").build()));
        service.reserveRootBatchOnPlanRelease(1L);
        verifyNoInteractions(generator);
    }

    @Test
    void crossMonthStartUsesReservedNumberDateAndSequenceMetadata() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        service.reserveRootBatchOnPlanRelease(1L);
        doReturn(HcProductionBatchResult.builder().productionBatchNo(plan.getBatchNo()).build())
                .when(service).recordKnownBatch(any());
        HcProductionBatchRequest request = new HcProductionBatchRequest();
        request.setPlanId(1L);
        request.setPlanOperationId(10L);
        request.setBizDate(LocalDate.of(2026, 10, 1));
        service.generateRootBatchOnFormulaStart(request);
        ArgumentCaptor<HcProductionBatchRequest> captured = ArgumentCaptor.forClass(HcProductionBatchRequest.class);
        verify(service).recordKnownBatch(captured.capture());
        assertEquals("W26J171A", captured.getValue().getProductionBatchNo());
        assertEquals(LocalDate.of(2026, 9, 11), captured.getValue().getBizDate());
        assertEquals(171, captured.getValue().getAttributes().get("annualBatchSeq"));
        verify(generator, times(1)).generateLotNo(any());
    }

    @Test
    void formulaStartUsesPlanStartDateInsteadOfReportDateWhenNotReserved() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        doReturn(HcProductionBatchResult.builder().productionBatchNo("W26J171A").build())
                .when(service).recordKnownBatch(any());
        HcProductionBatchRequest request = new HcProductionBatchRequest();
        request.setPlanId(1L);
        request.setPlanOperationId(10L);
        request.setBizDate(LocalDate.of(2026, 10, 1));

        service.generateRootBatchOnFormulaStart(request);

        ArgumentCaptor<HcProductionBatchRequest> captured = ArgumentCaptor.forClass(HcProductionBatchRequest.class);
        verify(service).recordKnownBatch(captured.capture());
        assertEquals(LocalDate.of(2026, 9, 11), captured.getValue().getBizDate());
    }

    @Test
    void formulaStartRejectsPlanWithoutPlanStartDate() {
        HcPlanOrderDO plan = plan(1, "RELEASED");
        plan.setProductionStartDate(null);
        HcProductionBatchRequest request = new HcProductionBatchRequest();
        request.setPlanId(1L);
        request.setPlanOperationId(10L);
        request.setBizDate(LocalDate.of(2026, 10, 1));

        assertThrows(RuntimeException.class, () -> service.generateRootBatchOnFormulaStart(request));
        verify(service, never()).recordKnownBatch(any());
        verifyNoInteractions(generator);
    }

    @Test
    void conflictingReservationFailsInsteadOfSavingDuplicate() {
        plan(1, "RELEASED");
        when(plans.selectOne(any(Wrapper.class))).thenReturn(HcPlanOrderDO.builder().id(2L).build());
        assertThrows(RuntimeException.class, () -> service.reserveRootBatchOnPlanRelease(1L));
        verify(plans, never()).updateById(any(HcPlanOrderDO.class));
    }
}
