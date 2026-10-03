package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceConfirmReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingSliceGenerateReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsSampleAbnormalLockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderInventoryLockMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsFaiOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsSampleAbnormalLockMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.stream.IntStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplSlittingDeleteTest {

    @Mock
    private HcUpstreamSampleLockService upstreamSampleLockService;

    @InjectMocks
    private HcProcessReportServiceImpl service;

    @Mock
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;
    @Mock
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Mock
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Mock
    private HcPlanOrderInventoryLockMapper hcPlanOrderInventoryLockMapper;
    @Mock
    private HcAdhesiveReportMapper hcAdhesiveReportMapper;
    @Mock
    private QmsFaiOrderMapper qmsFaiOrderMapper;
    @Mock
    private QmsSampleAbnormalLockMapper qmsSampleAbnormalLockMapper;
    @Mock
    private HcPressSlotReportMapper hcPressSlotReportMapper;

    @org.junit.jupiter.api.BeforeEach
    void releasedPlanForSliceActions() {
        lenient().when(hcPlanOrderMapper.selectByIdForUpdate(1L))
                .thenReturn(HcPlanOrderDO.builder().id(1L).planStatus("RELEASED").build());
    }

    @Test
    void shouldSoftDeleteUnconfirmedSlice() {
        HcSlittingSliceRecordDO slice = HcSlittingSliceRecordDO.builder().planId(1L)
                .id(11L)
                .planOperationId(101L)
                .sourceAdhesiveReportId(201L)
                .sliceSerialNo("W26G163AP001")
                .scanStatus("UNCONFIRMED")
                .build();
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder()
                .id(101L)
                .operationStatus("RUNNING")
                .build();
        HcAdhesiveReportDO sourceReport = HcAdhesiveReportDO.builder()
                .id(201L)
                .extraJson("{}")
                .build();
        when(hcSlittingSliceRecordMapper.selectByIdForUpdate(11L)).thenReturn(slice);
        when(hcPlanOrderOperationMapper.selectById(101L)).thenReturn(operation);
        when(hcAdhesiveReportMapper.selectById(201L)).thenReturn(sourceReport);
        when(qmsFaiOrderMapper.selectListByProductBatchNos(
                List.of("W26G163AP001"), "PRESS_SLOT_REPORT", "PRESS_SLOT")).thenReturn(List.of());
        when(hcPressSlotReportMapper.selectListBySourceSliceId(11L)).thenReturn(List.of());
        when(hcSlittingSliceRecordMapper.deleteById(11L)).thenReturn(1);

        service.deleteSlittingSlice(11L);

        verify(hcSlittingSliceRecordMapper).deleteById(11L);
    }

    @Test
    void shouldRejectDeletingConfirmedSlice() {
        HcSlittingSliceRecordDO slice = HcSlittingSliceRecordDO.builder().planId(1L)
                .id(12L)
                .scanStatus("CONFIRMED")
                .build();
        when(hcSlittingSliceRecordMapper.selectByIdForUpdate(12L)).thenReturn(slice);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.deleteSlittingSlice(12L));

        assertTrue(exception.getMessage().contains("已确认入账"));
        verify(hcSlittingSliceRecordMapper, never()).deleteById(12L);
    }

    @Test
    void shouldReuseDeletedSliceSerialNoInSameSegment() {
        assertManualSliceCanReuseDeletedSerialNo("P", "W26G163AP020");
    }

    @Test
    void shouldReuseDeletedSliceSerialNoInAnotherSegment() {
        assertManualSliceCanReuseDeletedSerialNo("Q", "W26G163AQ020");
    }

    @Test
    void shouldRejectConfirmingSliceWhenLoadedSourceGetsAdhesiveSampleNgLock() {
        HcSlittingSliceRecordDO slice = HcSlittingSliceRecordDO.builder().planId(1L)
                .id(1984L)
                .planOperationId(778L)
                .sourceAdhesiveReportId(103L)
                .sliceSerialNo("W26H154AQ001B")
                .scanStatus("UNCONFIRMED")
                .sizeCode("740")
                .build();
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder()
                .id(778L)
                .operationStatus("RUNNING")
                .build();
        HcAdhesiveReportDO sourceReport = HcAdhesiveReportDO.builder()
                .id(103L)
                .tenantId(1L)
                .sourceBatchNo("W26H154A")
                .productionBatchNo("W26H154AQ-J1")
                .reportStatus("CONFIRMED")
                .extraJson("{}")
                .build();
        QmsSampleAbnormalLockDO lock = QmsSampleAbnormalLockDO.builder()
                .id(3L)
                .lockNo("QSAL-20260806235320-672")
                .lockStatus("LOCKED")
                .sourceProcessCode("ADHESIVE1")
                .sourceProcessName("粘胶1")
                .objectType("SEGMENT")
                .objectNo("W26H154AQ")
                .abnormalResult("NG")
                .abnormalFeedbackTime(LocalDateTime.of(2026, 8, 6, 23, 53, 20))
                .lockReason("当前分段 W26H154AQ 因2026-08-06 23:53:20，粘胶1 留样送检NG异常，锁定不允许继续报工，等待复检确认后继续。")
                .build();
        HcSlittingSliceConfirmReqVO request = new HcSlittingSliceConfirmReqVO();
        request.setId(1984L);
        request.setScannedSliceNo("W26H154AQ001B");
        request.setSizeCode("740");
        request.setSelfCheck("OK");

        when(hcSlittingSliceRecordMapper.selectByIdForUpdate(1984L)).thenReturn(slice);
        when(hcPlanOrderOperationMapper.selectById(778L)).thenReturn(operation);
        when(hcAdhesiveReportMapper.selectById(103L)).thenReturn(sourceReport);
        when(qmsSampleAbnormalLockMapper.selectLatestEffectiveLocked(any(), any(), any(), any()))
                .thenAnswer(invocation ->
                        "ADHESIVE1".equals(invocation.getArgument(0))
                                && "SEGMENT".equals(invocation.getArgument(1))
                                && "W26H154AQ".equals(invocation.getArgument(2))
                                && Long.valueOf(1L).equals(invocation.getArgument(3)) ? lock : null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.confirmSlittingSlice(request));

        assertTrue(exception.getMessage().contains("粘胶1 留样送检NG异常"));
        verify(hcSlittingSliceRecordMapper, never()).updateById(any(HcSlittingSliceRecordDO.class));
        verify(hcAdhesiveReportMapper, never()).deductSlittingRemainingLength(any(), any());
    }

    @Test
    void wetAndGrindingLocksMustNoLongerBlockSlitting() throws Exception {
        var source = HcAdhesiveReportDO.builder().tenantId(1L)
                .sourceBatchNo("W26H154A").productionBatchNo("W26H154AQ-J1").build();
        var method = HcProcessReportServiceImpl.class.getDeclaredMethod(
                "validateSlittingSampleAbnormalUnlocked", HcAdhesiveReportDO.class, String.class);
        method.setAccessible(true);
        method.invoke(service, source, "扫码确认分切");
        verify(qmsSampleAbnormalLockMapper).selectLatestEffectiveLocked("ADHESIVE1", "SEGMENT", "W26H154AQ", 1L);
        org.mockito.Mockito.verifyNoMoreInteractions(qmsSampleAbnormalLockMapper);
    }

    private void assertManualSliceCanReuseDeletedSerialNo(String targetSegment, String expectedSliceSerialNo) {
        HcPlanOrderDO planOrder = HcPlanOrderDO.builder().planStatus("RELEASED")
                .id(1L)
                .tenantId(1L)
                .planNo("PLAN-001")
                .build();
        HcPlanOrderOperationDO operation = HcPlanOrderOperationDO.builder()
                .id(101L)
                .planId(1L)
                .opCode("OP-SLIT")
                .operationStatus("RUNNING")
                .build();
        HcAdhesiveReportDO sourceReport = HcAdhesiveReportDO.builder()
                .id(201L)
                .planId(1L)
                .reportStatus("CONFIRMED")
                .sourceBatchNo("W26G163A")
                .productionBatchNo("W26G163A" + targetSegment)
                .outputLength(BigDecimal.valueOf(20))
                .extraJson("{}")
                .build();
        List<HcSlittingSliceRecordDO> activeSlices = IntStream.rangeClosed(1, 19)
                .mapToObj(index -> HcSlittingSliceRecordDO.builder().planId(1L)
                        .planOperationId(101L)
                        .sourceBatchNo("W26G163A")
                        .sliceSerialNo("W26G163AP" + String.format("%03d", index))
                        .sliceIndex(index)
                        .scanStatus("UNCONFIRMED")
                        .build())
                .toList();
        HcSlittingSliceRecordDO deletedP20 = HcSlittingSliceRecordDO.builder().planId(1L)
                .planOperationId(101L)
                .sourceBatchNo("W26G163A")
                .sliceSerialNo("W26G163AP020")
                .sliceIndex(20)
                .build();
        HcSlittingSliceGenerateReqVO request = new HcSlittingSliceGenerateReqVO();
        request.setPlanId(1L);
        request.setPlanOperationId(101L);
        request.setSourceAdhesiveReportId(201L);
        request.setSliceCount(1);
        request.setStartSerialNo(20);
        request.setCutMode("MANUAL");

        when(hcPlanOrderMapper.selectByIdForUpdate(1L)).thenReturn(planOrder);
        when(hcPlanOrderOperationMapper.selectById(101L)).thenReturn(operation);
        when(hcAdhesiveReportMapper.selectById(201L)).thenReturn(sourceReport);
        when(hcPlanOrderInventoryLockMapper.selectListByPlanOperationId(101L)).thenReturn(List.of());
        when(hcSlittingSliceRecordMapper.selectListByPlanOperationId(101L)).thenReturn(activeSlices);
        lenient().when(hcSlittingSliceRecordMapper.selectHistoryListByPlanOperationId(101L))
                .thenReturn(List.of(deletedP20));
        when(hcSlittingSliceRecordMapper.selectListBySourceId(101L, 201L)).thenReturn(List.of());

        service.generateSlittingSlices(request);

        ArgumentCaptor<Collection<HcSlittingSliceRecordDO>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(hcSlittingSliceRecordMapper).insertBatch(captor.capture());
        assertEquals(expectedSliceSerialNo, captor.getValue().iterator().next().getSliceSerialNo());
        verify(hcSlittingSliceRecordMapper, never()).selectHistoryListByPlanOperationId(101L);
    }
}
