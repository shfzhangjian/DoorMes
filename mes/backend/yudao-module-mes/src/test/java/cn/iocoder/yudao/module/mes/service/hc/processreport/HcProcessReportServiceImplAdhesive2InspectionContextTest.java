package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcPressSlotProcessParamSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.planorder.HcPlanOrderOperationDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform.HcProcessFormRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction.HcProductionInstructionDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordItemMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processform.HcProcessFormRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.productioninstruction.HcProductionInstructionMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcProcessReportServiceImplAdhesive2InspectionContextTest {

    private static final Long PLAN_ID = 1L;
    private static final Long OPERATION_ID = 894L;
    private static final String BATCH = "W26H163AP";
    private static final String MODEL = "W26P0300";
    private static final LocalDate DATE = LocalDate.of(2026, 9, 5);

    @InjectMocks
    private HcProcessReportServiceImpl service;
    @Mock
    private HcProductionInstructionMapper hcProductionInstructionMapper;
    @Mock
    private HcProcessFormRecordMapper hcProcessFormRecordMapper;
    @Mock
    private HcProcessFormRecordItemMapper hcProcessFormRecordItemMapper;
    @Mock
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Mock
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;

    @Test
    void shouldInferCurrentChangeoverForOldClientWithoutInstructionId() {
        HcProductionInstructionDO current = currentChangeover();
        when(hcProductionInstructionMapper.selectLatestEffectiveChangeover(PLAN_ID, OPERATION_ID, BATCH))
                .thenReturn(current);

        assertSame(current, service.resolveAdhesive2InspectionChangeover(PLAN_ID, OPERATION_ID, BATCH, null, null));
    }

    @Test
    void shouldRejectSubmittedPreviousInstructionId() {
        when(hcProductionInstructionMapper.selectLatestEffectiveChangeover(PLAN_ID, OPERATION_ID, BATCH))
                .thenReturn(currentChangeover());

        assertThrows(ServiceException.class,
                () -> service.resolveAdhesive2InspectionChangeover(PLAN_ID, OPERATION_ID, BATCH, 33L, null));
    }

    @Test
    void shouldNotMoveOldOrUnboundInspectionIntoNewChangeover() {
        when(hcProductionInstructionMapper.selectLatestEffectiveChangeover(PLAN_ID, OPERATION_ID, BATCH))
                .thenReturn(currentChangeover());

        for (Long oldInstructionId : new Long[] {33L, null}) {
            assertThrows(ServiceException.class, () -> service.resolveAdhesive2InspectionChangeover(
                    PLAN_ID, OPERATION_ID, BATCH, 34L, record(oldInstructionId)));
        }
    }

    @Test
    void shouldNotReuseBoundInspectionWhenNoChangeoverIsEffective() {
        assertThrows(ServiceException.class, () -> service.resolveAdhesive2InspectionChangeover(
                PLAN_ID, OPERATION_ID, BATCH, null, record(33L)));
        assertNull(service.resolveAdhesive2InspectionChangeover(PLAN_ID, OPERATION_ID, BATCH, null, record(null)));
    }

    @Test
    void shouldRejectMovingExistingInspectionAcrossSegment() {
        HcProcessFormRecordDO existing = record(null);
        existing.setBatchNo("W26H163AR");

        assertThrows(ServiceException.class, () -> service.resolveAdhesive2InspectionChangeover(
                PLAN_ID, OPERATION_ID, BATCH, null, existing));
    }

    @Test
    void shouldNotTreatPreviousConfirmedInspectionAsIdempotentConfirmation() {
        mockPlanAndCurrentChangeover();
        HcProcessFormRecordDO previous = record(33L);
        previous.setId(100L);
        previous.setRecordStatus("CONFIRMED");
        when(hcProcessFormRecordMapper.selectById(100L)).thenReturn(previous);
        HcPressSlotProcessParamSaveReqVO request = request();
        request.setId(100L);

        ServiceException error = assertThrows(ServiceException.class, () -> service.confirmAdhesive2ProcessParam(request));

        assertTrue(error.getMessage().contains("不属于当前换型"));
        verify(hcProcessFormRecordMapper, never()).updateById(any(HcProcessFormRecordDO.class));
    }

    @Test
    void shouldRejectPreviousModelBeforeLoadingOrSavingInspectionTemplate() {
        mockPlanAndCurrentChangeover();
        HcPressSlotProcessParamSaveReqVO request = request();
        request.setModelCode("W26P0200");

        ServiceException error = assertThrows(ServiceException.class, () -> service.saveAdhesive2ProcessParam(request));

        assertTrue(error.getMessage().contains("本次换型目标型号 " + MODEL));
        verify(hcProcessFormRecordMapper, never()).insert(any(HcProcessFormRecordDO.class));
    }

    @Test
    void shouldRejectPreviousMaterialBeforeSavingInspection() {
        mockPlanAndCurrentChangeover();
        HcPressSlotProcessParamSaveReqVO request = request();
        request.setMaterialCode("MAT-0200");

        ServiceException error = assertThrows(ServiceException.class, () -> service.saveAdhesive2ProcessParam(request));

        assertTrue(error.getMessage().contains("本次换型目标料号 MAT-0300"));
        verify(hcProcessFormRecordMapper, never()).insert(any(HcProcessFormRecordDO.class));
    }

    @Test
    void shouldSkipPreviousInspectionEvenWhenSameModelIsUsedAgain() {
        HcProcessFormRecordDO previous = record(33L);
        HcProcessFormRecordDO current = record(34L);
        when(hcProcessFormRecordMapper.selectOneByRecordNo("record-key")).thenReturn(previous);
        when(hcProcessFormRecordMapper.selectList(any())).thenReturn(List.of(previous, current));

        assertSame(current, service.findAdhesive2ProductionCheckRecord(
                "record-key", OPERATION_ID, DATE, BATCH, MODEL, 34L));
    }

    @Test
    void shouldRestrictFallbackByOperationDateModelBatchAndInstruction() {
        HcProcessFormRecordDO wrongOperation = record(34L);
        wrongOperation.setPlanOperationId(895L);
        HcProcessFormRecordDO wrongDate = record(34L);
        wrongDate.setRecordDate(DATE.minusDays(1));
        HcProcessFormRecordDO wrongModel = record(34L);
        wrongModel.setModelCode("W26P0200");
        HcProcessFormRecordDO wrongBatch = record(34L);
        wrongBatch.setBatchNo("W26H163AR");
        when(hcProcessFormRecordMapper.selectList(any())).thenReturn(
                List.of(wrongOperation, wrongDate, wrongModel, wrongBatch, record(null), record(33L)));

        assertNull(service.findAdhesive2ProductionCheckRecord("record-key", OPERATION_ID, DATE, BATCH, MODEL, 34L));
    }

    @Test
    void shouldPreserveUnboundLegacyDraftOnlyWithoutChangeover() {
        HcProcessFormRecordDO legacy = record(null);
        when(hcProcessFormRecordMapper.selectList(any())).thenReturn(List.of(record(33L), legacy));

        assertSame(legacy, service.findAdhesive2ProductionCheckRecord(
                "record-key", OPERATION_ID, DATE, BATCH, MODEL, null));
    }

    @Test
    void shouldRequireInspectionForEachChangeoverEvenAtSameTargetModel() {
        when(hcProcessFormRecordMapper.selectList(any())).thenReturn(List.of(record(null), record(33L)));

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.validateAdhesive2ChangeoverInspection(currentChangeover(), BATCH));

        assertEquals("请先提交本次换型工艺参数点检", error.getMessage());
    }

    @Test
    void shouldAllowSubmittedRecordedAndConfirmedInspectionOfCurrentChangeover() {
        HcProcessFormRecordDO current = record(34L);
        when(hcProcessFormRecordMapper.selectList(any())).thenReturn(List.of(current));

        for (String status : List.of("SUBMITTED", "RECORDED", "CONFIRMED")) {
            current.setRecordStatus(status);
            assertDoesNotThrow(() -> service.validateAdhesive2ChangeoverInspection(currentChangeover(), BATCH));
        }
        for (String status : List.of("DRAFT", "VOID")) {
            current.setRecordStatus(status);
            assertThrows(ServiceException.class,
                    () -> service.validateAdhesive2ChangeoverInspection(currentChangeover(), BATCH));
        }
    }

    @Test
    void shouldRejectWrongModelInspectionEvenWithCurrentInstructionId() {
        HcProcessFormRecordDO wrongModel = record(34L);
        wrongModel.setModelCode("W26P0200");
        when(hcProcessFormRecordMapper.selectList(any())).thenReturn(List.of(wrongModel));

        assertThrows(ServiceException.class,
                () -> service.validateAdhesive2ChangeoverInspection(currentChangeover(), BATCH));
    }

    @Test
    void shouldReturnHistoricalRecordsWithTheirOriginalInstructionId() {
        HcProcessFormRecordDO previous = record(33L);
        previous.setId(100L);
        HcProcessFormRecordDO current = record(34L);
        current.setId(101L);
        when(hcProcessFormRecordMapper.selectList(any())).thenReturn(List.of(current, previous));
        when(hcProcessFormRecordItemMapper.selectListByRecordId(any())).thenReturn(List.of());

        var records = service.getAdhesive2ProcessParamList(OPERATION_ID, DATE, null, BATCH, "PRODUCTION_CHECK");

        assertEquals(List.of(34L, 33L), records.stream().map(item -> item.getChangeoverInstructionId()).toList());
    }

    private HcProductionInstructionDO currentChangeover() {
        return HcProductionInstructionDO.builder().id(34L).planId(PLAN_ID).planOperationId(OPERATION_ID)
                .segmentBatchNo(BATCH).targetModelCode(MODEL).targetMaterialCode("MAT-0300")
                .executeStatus("EXECUTING").build();
    }

    private HcProcessFormRecordDO record(Long instructionId) {
        HcProcessFormRecordDO record = new HcProcessFormRecordDO();
        record.setPlanOperationId(OPERATION_ID);
        record.setProcessCode("ADHESIVE2");
        record.setFormType("PRODUCTION_CHECK");
        record.setBatchNo(BATCH);
        record.setModelCode(MODEL);
        record.setRecordDate(DATE);
        record.setRecordStatus("SUBMITTED");
        record.setHeaderDataJson(instructionId == null ? "{}" : "{\"changeoverInstructionId\":" + instructionId + "}");
        return record;
    }

    private HcPressSlotProcessParamSaveReqVO request() {
        HcPressSlotProcessParamSaveReqVO request = new HcPressSlotProcessParamSaveReqVO();
        request.setPlanId(PLAN_ID);
        request.setPlanOperationId(OPERATION_ID);
        request.setMotherBatchNo(BATCH);
        request.setModelCode(MODEL);
        return request;
    }

    private void mockPlanAndCurrentChangeover() {
        when(hcPlanOrderMapper.selectById(PLAN_ID)).thenReturn(HcPlanOrderDO.builder()
                .id(PLAN_ID).modelCode("W26P0100").build());
        when(hcPlanOrderOperationMapper.selectById(OPERATION_ID)).thenReturn(HcPlanOrderOperationDO.builder()
                .id(OPERATION_ID).planId(PLAN_ID).opCode("OP-ADHESIVE2").build());
        when(hcProductionInstructionMapper.selectLatestEffectiveChangeover(PLAN_ID, OPERATION_ID, BATCH))
                .thenReturn(currentChangeover());
    }
}
