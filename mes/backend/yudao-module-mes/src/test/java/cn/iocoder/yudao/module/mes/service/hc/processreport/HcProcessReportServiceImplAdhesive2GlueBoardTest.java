package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesive2FaiApplyReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveGlueBoardUsageSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardStockDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardStockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveGlueBoardUsageMapper;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplAdhesive2GlueBoardTest {

    @InjectMocks
    private HcProcessReportServiceImpl service;

    @Mock
    private HcAdhesiveGlueBoardUsageMapper hcAdhesiveGlueBoardUsageMapper;

    @Mock
    private HcAdhesiveGlueBoardStockMapper hcAdhesiveGlueBoardStockMapper;

    @Test
    void shouldAllowAdhesive2GlueBoardUsageWithoutLoadedPlan() {
        long stockId = 9001L;
        HcAdhesiveGlueBoardUsageSaveReqVO reqVO = new HcAdhesiveGlueBoardUsageSaveReqVO();
        reqVO.setGlueBoardStockId(stockId);
        reqVO.setGlueBoardMaterialCode("W220");
        reqVO.setGlueBoardBatchNo("GB-001");
        reqVO.setReceiveStartPosition(BigDecimal.ZERO);
        reqVO.setReceiveLength(new BigDecimal("20"));
        HcAdhesiveGlueBoardStockDO stock = HcAdhesiveGlueBoardStockDO.builder()
                .id(stockId)
                .tenantId(1L)
                .accessoryCategory("GLUE_BOARD")
                .glueBoardMaterialCode("W220")
                .glueBoardBatchNo("GB-001")
                .stockMeasureMode("LENGTH")
                .receiveStartPosition(BigDecimal.ZERO)
                .receiveLength(new BigDecimal("20"))
                .availableStartPosition(BigDecimal.ZERO)
                .availableLength(new BigDecimal("20"))
                .stockStatus("ACTIVE")
                .qualityStatus("NORMAL")
                .build();
        when(hcAdhesiveGlueBoardStockMapper.selectById(stockId)).thenReturn(stock);

        assertDoesNotThrow(() -> service.saveAdhesive2GlueBoardUsage(reqVO));
        verify(hcAdhesiveGlueBoardUsageMapper).insert(any(HcAdhesiveGlueBoardUsageDO.class));
    }

    @Test
    void shouldAllowActiveGlueBoardAcrossPlansOnSameEquipment() {
        HcAdhesiveGlueBoardUsageDO usage = activeUsage(101L, 11L, "ADH2-01");
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 11L, "ADH2-01");

        assertTrue(service.isGlueBoardUsageAllowedForReport(usage, operation, "ADHESIVE2"));
    }

    @Test
    void shouldRejectActiveGlueBoardFromDifferentEquipment() {
        HcAdhesiveGlueBoardUsageDO usage = activeUsage(101L, 11L, "ADH2-01");
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 12L, "ADH2-01");

        assertFalse(service.isGlueBoardUsageAllowedForReport(usage, operation, "ADHESIVE2"));
    }

    @Test
    void shouldMatchEquipmentCodeWhenEquipmentIdIsIncomplete() {
        HcAdhesiveGlueBoardUsageDO usage = activeUsage(101L, null, "ADH2-01");
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 12L, "ADH2-01");

        assertTrue(service.isGlueBoardUsageAllowedForReport(usage, operation, "ADHESIVE2"));
    }

    @Test
    void shouldRejectHistoricalUsageWithoutEquipmentEvenForSameOperation() {
        HcAdhesiveGlueBoardUsageDO usage = activeUsage(202L, null, null);
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 12L, "ADH2-01");

        assertFalse(service.isGlueBoardUsageAllowedForReport(usage, operation, "ADHESIVE2"));
    }

    @Test
    void shouldUseSameEquipmentGlueBoardAcrossPlansForAdhesive2CoaSnapshot() throws Exception {
        HcAdhesiveGlueBoardUsageDO usage = activeUsage(101L, 11L, "ADH2-01");
        usage.setId(1001L);
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 11L, "ADH2-01");
        HcAdhesive2FaiApplyReqVO reqVO = new HcAdhesive2FaiApplyReqVO();
        reqVO.setGlueBoardUsageId(1001L);
        when(hcAdhesiveGlueBoardUsageMapper.selectById(1001L)).thenReturn(usage);

        HcAdhesiveGlueBoardUsageDO result = resolveAdhesive2FaiGlueBoardUsageForSnapshot(reqVO, operation);

        assertEquals(usage, result);
    }

    @Test
    void shouldRejectDifferentEquipmentGlueBoardForAdhesive2CoaSnapshot() {
        HcAdhesiveGlueBoardUsageDO usage = activeUsage(101L, 11L, "ADH2-01");
        usage.setId(1002L);
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 12L, "ADH2-01");
        HcAdhesive2FaiApplyReqVO reqVO = new HcAdhesive2FaiApplyReqVO();
        reqVO.setGlueBoardUsageId(1002L);
        when(hcAdhesiveGlueBoardUsageMapper.selectById(1002L)).thenReturn(usage);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> resolveAdhesive2FaiGlueBoardUsageForSnapshot(reqVO, operation));

        assertTrue(exception.getCause() instanceof ServiceException);
        assertTrue(exception.getCause().getMessage().contains("胶板领用记录不存在或不属于当前粘胶2工序"));
    }

    @Test
    void shouldRejectInactiveGlueBoardForAdhesive2CoaSnapshot() {
        HcAdhesiveGlueBoardUsageDO usage = activeUsage(101L, 11L, "ADH2-01");
        usage.setId(1003L);
        usage.setUsageStatus("USED_UP");
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 11L, "ADH2-01");
        HcAdhesive2FaiApplyReqVO reqVO = new HcAdhesive2FaiApplyReqVO();
        reqVO.setGlueBoardUsageId(1003L);
        when(hcAdhesiveGlueBoardUsageMapper.selectById(1003L)).thenReturn(usage);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> resolveAdhesive2FaiGlueBoardUsageForSnapshot(reqVO, operation));

        assertTrue(exception.getCause() instanceof ServiceException);
        assertTrue(exception.getCause().getMessage().contains("胶板领用记录不存在或不属于当前粘胶2工序"));
    }

    @Test
    void shouldSelectOnlyCurrentEquipmentUsagesForReplacement() {
        HcPlanOrderOperationDO operation = adhesive2Operation(202L, 12L, "ADH2-02");
        HcAdhesiveGlueBoardUsageDO oldUsage = activeUsage(101L, 12L, "ADH2-02");
        when(hcAdhesiveGlueBoardUsageMapper.selectActiveAdhesive2ListByEquipment(12L, "ADH2-02"))
                .thenReturn(List.of(oldUsage));

        List<HcAdhesiveGlueBoardUsageDO> result =
                service.selectActiveAdhesive2GlueBoardUsagesForEquipment(operation);

        assertEquals(List.of(oldUsage), result);
        verify(hcAdhesiveGlueBoardUsageMapper).selectActiveAdhesive2ListByEquipment(12L, "ADH2-02");
        verify(hcAdhesiveGlueBoardUsageMapper, never()).selectActiveListByOperationScope("ADHESIVE2");
    }

    private HcAdhesiveGlueBoardUsageDO activeUsage(Long planOperationId, Long equipmentId, String equipmentCode) {
        return HcAdhesiveGlueBoardUsageDO.builder()
                .planOperationId(planOperationId)
                .operationCode("ADHESIVE2")
                .operationName("粘胶2")
                .equipmentId(equipmentId)
                .equipmentCode(equipmentCode)
                .usageStatus("ACTIVE")
                .build();
    }

    private HcPlanOrderOperationDO adhesive2Operation(Long id, Long equipmentId, String equipmentCode) {
        return HcPlanOrderOperationDO.builder()
                .id(id)
                .opCode("OP-ADHESIVE2")
                .opName("粘胶2")
                .equipmentId(equipmentId)
                .equipmentCode(equipmentCode)
                .build();
    }

    private HcAdhesiveGlueBoardUsageDO resolveAdhesive2FaiGlueBoardUsageForSnapshot(
            HcAdhesive2FaiApplyReqVO reqVO, HcPlanOrderOperationDO operation) throws Exception {
        Method method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "resolveAdhesive2FaiGlueBoardUsageForSnapshot",
                HcAdhesive2FaiApplyReqVO.class,
                HcPlanOrderOperationDO.class);
        method.setAccessible(true);
        return (HcAdhesiveGlueBoardUsageDO) method.invoke(service, reqVO, operation);
    }
}
