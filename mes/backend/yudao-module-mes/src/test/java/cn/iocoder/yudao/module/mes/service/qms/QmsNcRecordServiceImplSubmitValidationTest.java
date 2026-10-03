package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.bpm.api.task.BpmProcessInstanceApi;
import cn.iocoder.yudao.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.iocoder.yudao.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsNcRecordMapper;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class QmsNcRecordServiceImplSubmitValidationTest {

    private final QmsNcRecordServiceImpl service = new QmsNcRecordServiceImpl();

    @Test
    void shouldAllowManualDefectNameWithoutDefectCode() throws Exception {
        QmsNcRecordDO record = buildValidRecord();
        record.setDefectCode(null);
        record.setDefectName("人工填写的缺陷名称");

        assertDoesNotThrow(() -> invokeValidateSubmitRequired(record));
    }

    @Test
    void shouldRejectWhenDefectNameIsBlankDuringQualityConfirmation() throws Exception {
        QmsNcRecordDO record = buildValidRecord();
        record.setDefectCode("DC-001");
        record.setDefectName(" ");

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> invokeValidateQualityConfirmation(record));
        assertInstanceOf(ServiceException.class, exception.getCause());
    }

    @Test
    void shouldAllowRawMaterialRegistrationWithoutQualityConclusion() throws Exception {
        QmsNcRecordDO record = buildValidRecord();
        record.setSourceType("RAW_MATERIAL");
        record.setDefectCode(null);
        record.setDefectName(null);
        record.setNcLevel(null);

        assertDoesNotThrow(() -> invokeValidateSubmitRequired(record));
    }

    @Test
    void shouldRejectRawMaterialRegistrationWithoutDescription() throws Exception {
        QmsNcRecordDO record = buildValidRecord();
        record.setSourceType("RAW_MATERIAL");
        record.setDefectName(null);
        record.setNcLevel(null);
        record.setNcDescription(" ");

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> invokeValidateSubmitRequired(record));
        assertInstanceOf(ServiceException.class, exception.getCause());
    }

    @Test
    void shouldRejectRepeatedFinalApprovalRoute() throws Exception {
        QmsNcRecordDO record = buildValidRecord();
        record.setSourceType("RAW_MATERIAL");
        record.setFinalApproveTime(LocalDateTime.now());

        InvocationTargetException exception = assertThrows(InvocationTargetException.class,
                () -> invokeValidateQualityTransferRoute(record, "FINAL_APPROVAL"));
        assertInstanceOf(ServiceException.class, exception.getCause());
    }

    @Test
    void shouldAllowExecutionRouteAfterFinalApproval() throws Exception {
        QmsNcRecordDO record = buildValidRecord();
        record.setSourceType("RAW_MATERIAL");
        record.setFinalApproveTime(LocalDateTime.now());

        assertDoesNotThrow(() -> invokeValidateQualityTransferRoute(record, "DISPOSITION_EXECUTION"));
    }

    @Test
    void shouldInitializeProductTransferRouteWhenStartingProcess() throws Exception {
        CapturingProcessInstanceApi processInstanceApi = new CapturingProcessInstanceApi();
        injectProcessInstanceApi(processInstanceApi);
        QmsNcRecordDO record = buildValidRecord();
        record.setId(6L);
        record.setNcNo("NCR-PRODUCT-001");

        assertEquals("process-instance-id", invokeStartNcrBpmProcess(record));
        assertEquals("", processInstanceApi.createReqDTO.getVariables().get("productTransferRoute"));
    }

    @Test
    void shouldInitializeRawMaterialTransferRouteWhenStartingProcess() throws Exception {
        CapturingProcessInstanceApi processInstanceApi = new CapturingProcessInstanceApi();
        injectProcessInstanceApi(processInstanceApi);
        QmsNcRecordDO record = buildValidRecord();
        record.setId(7L);
        record.setNcNo("NCR-RAW-001");
        record.setSourceType("RAW_MATERIAL");

        assertEquals("process-instance-id", invokeStartNcrBpmProcess(record));
        assertEquals("", processInstanceApi.createReqDTO.getVariables().get("rawMaterialTransferRoute"));
    }

    @Test
    void shouldBackfillMissingTransferRouteForRunningProcess() throws Exception {
        CapturingProcessInstanceApi processInstanceApi = new CapturingProcessInstanceApi();
        injectProcessInstanceApi(processInstanceApi);
        QmsNcRecordDO record = buildValidRecord();
        record.setProcessInstanceId("process-instance-id");

        invokeEnsureNcrTransferRouteVariable(record);

        assertEquals(Map.of("productTransferRoute", ""), processInstanceApi.updatedVariables);
    }

    @Test
    void shouldPersistClearFinalFieldsWhenWithdrawBackToFinalApproval() throws Exception {
        QmsNcRecordMapper mapper = mock(QmsNcRecordMapper.class);
        injectNcRecordMapper(mapper);

        invokePersistClearedBusinessDataAfterWithdraw(30L, "FINAL_APPROVAL");

        verify(mapper).clearFinalFields(30L);
        verify(mapper).clearStockDisposeFields(30L);
        verify(mapper).clearCloseFields(30L);
        verify(mapper).clearEffectConfirmFields(30L);
    }

    @Test
    void shouldKeepFinalFieldsWhenWithdrawBackToExecutionAssign() throws Exception {
        QmsNcRecordMapper mapper = mock(QmsNcRecordMapper.class);
        injectNcRecordMapper(mapper);

        invokePersistClearedBusinessDataAfterWithdraw(30L, "EXECUTION_ASSIGN");

        verify(mapper, never()).clearFinalFields(30L);
        verify(mapper).clearStockDisposeFields(30L);
        verify(mapper).clearCloseFields(30L);
        verify(mapper).clearEffectConfirmFields(30L);
    }

    private QmsNcRecordDO buildValidRecord() {
        return QmsNcRecordDO.builder()
                .lotNo("W26G143AR070")
                .defectQty(BigDecimal.ONE)
                .defectName("检验项不合格")
                .ncLevel("MINOR")
                .ncDescription("人工填写的不合格说明")
                .build();
    }

    private void invokeValidateSubmitRequired(QmsNcRecordDO record) throws Exception {
        Method method = QmsNcRecordServiceImpl.class
                .getDeclaredMethod("validateSubmitRequired", QmsNcRecordDO.class);
        method.setAccessible(true);
        method.invoke(service, record);
    }

    private void invokeValidateQualityConfirmation(QmsNcRecordDO record) throws Exception {
        Method method = QmsNcRecordServiceImpl.class
                .getDeclaredMethod("validateQualityConfirmation", QmsNcRecordDO.class);
        method.setAccessible(true);
        method.invoke(service, record);
    }

    private void invokeValidateQualityTransferRoute(QmsNcRecordDO record, String route) throws Exception {
        Method method = QmsNcRecordServiceImpl.class
                .getDeclaredMethod("validateQualityTransferRoute", QmsNcRecordDO.class, String.class);
        method.setAccessible(true);
        method.invoke(service, record, route);
    }

    private String invokeStartNcrBpmProcess(QmsNcRecordDO record) throws Exception {
        Method method = QmsNcRecordServiceImpl.class
                .getDeclaredMethod("startNcrBpmProcess", QmsNcRecordDO.class, List.class, Long.class);
        method.setAccessible(true);
        return (String) method.invoke(service, record, List.of(), 181L);
    }

    private void invokeEnsureNcrTransferRouteVariable(QmsNcRecordDO record) throws Exception {
        Method method = QmsNcRecordServiceImpl.class
                .getDeclaredMethod("ensureNcrTransferRouteVariable", QmsNcRecordDO.class);
        method.setAccessible(true);
        method.invoke(service, record);
    }

    private void injectProcessInstanceApi(BpmProcessInstanceApi processInstanceApi) throws Exception {
        Field field = QmsNcRecordServiceImpl.class.getDeclaredField("bpmProcessInstanceApi");
        field.setAccessible(true);
        field.set(service, processInstanceApi);
    }

    private void injectNcRecordMapper(QmsNcRecordMapper mapper) throws Exception {
        Field field = QmsNcRecordServiceImpl.class.getDeclaredField("qmsNcRecordMapper");
        field.setAccessible(true);
        field.set(service, mapper);
    }

    private void invokePersistClearedBusinessDataAfterWithdraw(Long ncRecordId, String targetStatus) throws Exception {
        Method method = QmsNcRecordServiceImpl.class
                .getDeclaredMethod("persistClearedBusinessDataAfterWithdraw", Long.class, String.class);
        method.setAccessible(true);
        method.invoke(service, ncRecordId, targetStatus);
    }

    private static final class CapturingProcessInstanceApi implements BpmProcessInstanceApi {

        private BpmProcessInstanceCreateReqDTO createReqDTO;
        private Map<String, Object> updatedVariables;

        @Override
        public String createProcessInstance(Long userId, BpmProcessInstanceCreateReqDTO reqDTO) {
            this.createReqDTO = reqDTO;
            return "process-instance-id";
        }

        @Override
        public Integer getProcessInstanceStatus(String id) {
            return BpmProcessInstanceStatusEnum.RUNNING.getStatus();
        }

        @Override
        public Map<String, Object> getProcessInstanceVariables(String id) {
            return Map.of();
        }

        @Override
        public void updateProcessInstanceVariables(String id, Map<String, Object> variables) {
            this.updatedVariables = variables;
        }
    }
}
