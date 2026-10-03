package cn.iocoder.yudao.module.mes.service.hc.planorder;

import cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HcPlanOrderServiceImplBatchEditTest {

    private final HcPlanOrderServiceImpl service = new HcPlanOrderServiceImpl();

    @Test
    void shouldKeepExistingBatchWhenEditingOtherPlanFields() {
        HcPlanOrderDO oldPlan = HcPlanOrderDO.builder()
                .id(1001L)
                .batchNo("B26I001A")
                .batchRuleId(2001L)
                .batchRuleCode("LOT-CMP-BLACK-MASS-V1")
                .batchRuleVersion(1)
                .build();
        HcPlanOrderSaveReqVO request = new HcPlanOrderSaveReqVO();
        request.setId(oldPlan.getId());
        request.setBatchNo(oldPlan.getBatchNo());

        ReflectionTestUtils.invokeMethod(service,
                "normalizeAndValidateManualRootBatchNo", request, oldPlan);

        assertEquals(oldPlan.getBatchNo(), request.getBatchNo());
        assertEquals(oldPlan.getBatchRuleId(), request.getBatchRuleId());
        assertEquals(oldPlan.getBatchRuleCode(), request.getBatchRuleCode());
        assertEquals(oldPlan.getBatchRuleVersion(), request.getBatchRuleVersion());
    }

    @Test
    void shouldKeepExistingBatchSnapshotWhenClientOmitsBatchNo() {
        HcPlanOrderDO oldPlan = HcPlanOrderDO.builder()
                .id(1002L)
                .batchNo("W26I002A")
                .batchRuleId(2002L)
                .batchRuleCode("LOT-CMP-WHITE-MASS-V2")
                .batchRuleVersion(2)
                .build();
        HcPlanOrderSaveReqVO request = new HcPlanOrderSaveReqVO();
        request.setId(oldPlan.getId());

        ReflectionTestUtils.invokeMethod(service,
                "normalizeAndValidateManualRootBatchNo", request, oldPlan);

        assertEquals(oldPlan.getBatchNo(), request.getBatchNo());
        assertEquals(oldPlan.getBatchRuleId(), request.getBatchRuleId());
        assertEquals(oldPlan.getBatchRuleCode(), request.getBatchRuleCode());
        assertEquals(oldPlan.getBatchRuleVersion(), request.getBatchRuleVersion());
    }

    @Test
    void shouldNotCopyGeneratedBatchIntoPlanPreviewBatchField() {
        HcPlanOrderDO oldPlan = HcPlanOrderDO.builder()
                .id(1003L)
                .productionBatchNo("B26I003A")
                .batchRuleId(2003L)
                .batchRuleCode("LOT-CMP-BLACK-MASS-V1")
                .batchRuleVersion(1)
                .build();
        HcPlanOrderSaveReqVO request = new HcPlanOrderSaveReqVO();
        request.setId(oldPlan.getId());
        request.setBatchNo(oldPlan.getProductionBatchNo());

        ReflectionTestUtils.invokeMethod(service,
                "normalizeAndValidateManualRootBatchNo", request, oldPlan);

        assertNull(request.getBatchNo());
        assertEquals(oldPlan.getBatchRuleId(), request.getBatchRuleId());
        assertEquals(oldPlan.getBatchRuleCode(), request.getBatchRuleCode());
        assertEquals(oldPlan.getBatchRuleVersion(), request.getBatchRuleVersion());
    }
    @Test
    void activeReservationCannotBeManuallyOverwrittenWithoutReset() {
        HcPlanOrderDO oldPlan = HcPlanOrderDO.builder().id(1L).planStatus("DRAFT")
                .batchNo("W26J171A").productionBatchContextJson("{\"rootBatchReserved\":true}").build();
        HcPlanOrderSaveReqVO request = new HcPlanOrderSaveReqVO();
        request.setBatchNo("W26J172A");
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "normalizeAndValidateManualRootBatchNo", request, oldPlan));
    }

    @Test
    void omittedReservedBatchIsPreservedOnOrdinaryEdit() {
        HcPlanOrderDO oldPlan = HcPlanOrderDO.builder().id(1L).batchNo("W26J171A")
                .productionBatchContextJson("{\"rootBatchReserved\":true}")
                .batchRuleId(10L).batchRuleVersion(1).build();
        HcPlanOrderSaveReqVO request = new HcPlanOrderSaveReqVO();
        ReflectionTestUtils.invokeMethod(service, "normalizeAndValidateManualRootBatchNo", request, oldPlan);
        assertEquals("W26J171A", request.getBatchNo());
        assertEquals(10L, request.getBatchRuleId());
    }

    @Test
    void reservedBatchRejectsStartDateChangeButAllowsEndDateChange() {
        var mapper = org.mockito.Mockito.mock(
                cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper.class);
        ReflectionTestUtils.setField(service, "hcPlanOrderOperationMapper", mapper);
        var operation = cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO.builder()
                .id(10L).opSeq(1).opName("配料").workCenterId(20L).build();
        org.mockito.Mockito.when(mapper.selectListByPlanId(1L)).thenReturn(java.util.List.of(operation));
        HcPlanOrderDO oldPlan = HcPlanOrderDO.builder().id(1L)
                .productionStartDate(java.time.LocalDate.of(2026, 9, 11))
                .productionBatchContextJson("{\"rootBatchReserved\":true}").build();
        HcPlanOrderSaveReqVO request = new HcPlanOrderSaveReqVO();
        var first = new cn.iocoder.yudao.module.mes.controller.admin.hc.planorder.vo.HcPlanOrderOperationReqVO();
        first.setOpName("配料");
        first.setWorkCenterId(20L);
        request.setOperations(java.util.List.of(first));
        request.setProductionStartDate(oldPlan.getProductionStartDate());
        request.setProductionEndDate(java.time.LocalDate.of(2026, 10, 1));
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service,
                "validateReservedRootBatchIdentity", oldPlan, request));
        request.setProductionStartDate(java.time.LocalDate.of(2026, 10, 1));
        assertThrows(RuntimeException.class, () -> ReflectionTestUtils.invokeMethod(service,
                "validateReservedRootBatchIdentity", oldPlan, request));
    }

}
