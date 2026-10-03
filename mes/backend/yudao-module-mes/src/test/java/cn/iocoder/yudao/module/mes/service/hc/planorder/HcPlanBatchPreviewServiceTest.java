package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderBatchPreviewReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderBatchPreviewRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import cn.iocoder.yudao.module.mes.dal.mysql.plan.PlanMapper;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HcPlanBatchPreviewServiceTest {
    private final HcPlanOrderMapper plans = mock(HcPlanOrderMapper.class);
    private final HcPlanOrderOperationMapper operations = mock(HcPlanOrderOperationMapper.class);
    private final HcPlanOrderService generator = mock(HcPlanOrderService.class);
    private final HcPlanBatchPreviewService service = new HcPlanBatchPreviewService();

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "hcPlanOrderMapper", plans);
        ReflectionTestUtils.setField(service, "hcPlanOrderOperationMapper", operations);
        ReflectionTestUtils.setField(service, "hcPlanOrderService", generator);
    }

    @Test
    void springContextInjectsHcMapperWhenLegacyPlanMapperProxyExists() {
        try (var context = new AnnotationConfigApplicationContext()) {
            Object legacyProxy = Proxy.newProxyInstance(PlanMapper.class.getClassLoader(),
                    new Class<?>[]{PlanMapper.class}, (proxy, method, args) -> null);
            context.getBeanFactory().registerSingleton("planMapper", legacyProxy);
            context.getBeanFactory().registerSingleton("hcPlanOrderMapper", plans);
            context.getBeanFactory().registerSingleton("hcPlanOrderOperationMapper", operations);
            context.getBeanFactory().registerSingleton("hcPlanOrderServiceImpl", generator);
            context.register(HcPlanBatchPreviewService.class);

            assertDoesNotThrow(context::refresh);
            var bean = context.getBean(HcPlanBatchPreviewService.class);
            assertSame(plans, ReflectionTestUtils.getField(bean, "hcPlanOrderMapper"));
            assertSame(operations, ReflectionTestUtils.getField(bean, "hcPlanOrderOperationMapper"));
            assertSame(generator, ReflectionTestUtils.getField(bean, "hcPlanOrderService"));
        }
    }

    @Test
    void previewsNewPlanUsingBoundRuleWithoutChangingStoredBatchFields() {
        HcPlanOrderDO plan = plan(119L);
        plan.setProductionStartDate(LocalDate.of(2026, 9, 13));
        plan.setBatchRuleId(992005L);
        plan.setBatchRuleCode("LOT-CMP-WHITE-W33-MASS-V1");
        plan.setModelCode("W33P0300");
        HcPlanOrderOperationDO first = operation(119L, "配料", "RELEASED");
        first.setWorkCenterId(7L);
        when(plans.selectBatchIds(List.of(119L))).thenReturn(List.of(plan));
        when(operations.selectList(any(Wrapper.class))).thenReturn(List.of(first));
        HcPlanOrderBatchPreviewRespVO result = new HcPlanOrderBatchPreviewRespVO();
        result.setBatchNo("W26J033A");
        when(generator.previewRootBatchNo(any())).thenReturn(result);

        assertEquals(Map.of(119L, "W26J033A"), service.previewMissingRootBatches(List.of(119L)));
        ArgumentCaptor<HcPlanOrderBatchPreviewReqVO> request = ArgumentCaptor.forClass(HcPlanOrderBatchPreviewReqVO.class);
        verify(generator).previewRootBatchNo(request.capture());
        assertEquals(992005L, request.getValue().getBatchRuleId());
        assertEquals(true, request.getValue().getUseBoundRule());
        assertEquals(plan.getProductionStartDate(), request.getValue().getProductionStartDate());
        assertEquals(7L, request.getValue().getWorkCenterId());
        assertNull(plan.getBatchNo());
        assertNull(plan.getProductionBatchNo());
        assertEquals("NOT_GEN", plan.getBatchStatus());
        verify(plans).selectBatchIds(List.of(119L));
        verify(operations).selectList(any(Wrapper.class));
        verifyNoMoreInteractions(plans, operations);
    }

    @Test
    void preservesManualGeneratedAndInventoryBatches() {
        var manual = plan(1L); manual.setBatchNo("MANUAL");
        var generated = plan(2L); generated.setProductionBatchNo("ACTUAL");
        var inventory = plan(3L); inventory.setInventorySourceBatchNos("SOURCE");
        var inconsistent = plan(4L); inconsistent.setBatchStatus("GENERATED");
        when(plans.selectBatchIds(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(manual, generated, inventory, inconsistent));
        assertTrue(service.previewMissingRootBatches(List.of(1L, 2L, 3L, 4L)).isEmpty());
        verifyNoInteractions(operations, generator);
    }

    @Test
    void skipsPostProcessAndAlreadyStartedPlans() {
        when(plans.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(plan(1L), plan(2L)));
        when(operations.selectList(any(Wrapper.class))).thenReturn(List.of(
                operation(1L, "粘胶1", "RELEASED"), operation(2L, "配料", "RUNNING")));
        assertTrue(service.previewMissingRootBatches(List.of(1L, 2L)).isEmpty());
        verifyNoInteractions(generator);
    }

    @Test
    void invalidHistoricalRuleDoesNotBlockOtherPlans() {
        when(plans.selectBatchIds(List.of(1L, 2L))).thenReturn(List.of(plan(1L), plan(2L)));
        when(operations.selectList(any(Wrapper.class))).thenReturn(List.of(
                operation(1L, "配料", "RELEASED"), operation(2L, "配料", "RELEASED")));
        var valid = new HcPlanOrderBatchPreviewRespVO(); valid.setBatchNo("VALID");
        when(generator.previewRootBatchNo(any())).thenThrow(new IllegalArgumentException("规则不存在")).thenReturn(valid);
        assertEquals(Map.of(2L, "VALID"), service.previewMissingRootBatches(List.of(1L, 2L)));
    }

    private HcPlanOrderDO plan(Long id) {
        return HcPlanOrderDO.builder().id(id).planNo("PLAN-" + id).batchStatus("NOT_GEN").build();
    }

    private HcPlanOrderOperationDO operation(Long planId, String name, String status) {
        return HcPlanOrderOperationDO.builder().planId(planId).opName(name)
                .opCode("WC-MIX").operationStatus(status).build();
    }
}
