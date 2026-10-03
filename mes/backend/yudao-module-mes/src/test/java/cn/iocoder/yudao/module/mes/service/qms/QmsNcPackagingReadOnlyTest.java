package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsNcRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.*;
import org.flowable.engine.RuntimeService;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QmsNcPackagingReadOnlyTest {
    QmsNcRecordServiceImpl service = new QmsNcRecordServiceImpl();
    QmsNcRecordMapper records = mock(QmsNcRecordMapper.class);
    QmsNcPackagingFlowMapper pieces = mock(QmsNcPackagingFlowMapper.class);
    RuntimeService runtime = mock(RuntimeService.class);
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ReflectionTestUtils.setField(service, "qmsNcRecordMapper", records);
        ReflectionTestUtils.setField(service, "packagingFlowMapper", pieces);
        ReflectionTestUtils.setField(service, "runtimeService", runtime);
        ReflectionTestUtils.setField(service, "qmsNcMrbReviewMapper", mock(QmsNcMrbReviewMapper.class));
        ReflectionTestUtils.setField(service, "qmsNcDefectMapper", mock(QmsNcDefectMapper.class));
        ReflectionTestUtils.setField(service, "qmsNcRelationMapper", mock(QmsNcRelationMapper.class));
        ReflectionTestUtils.setField(service, "qmsNcDispositionNotifyMapper", mock(QmsNcDispositionNotifyMapper.class));
        ReflectionTestUtils.setField(service, "qmsNcFlowLogMapper", mock(QmsNcFlowLogMapper.class));
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void legacyStateIsReadWithoutBpmSyncOrWrites() {
        when(records.selectById(1L)).thenReturn(QmsNcRecordDO.builder().id(1L).tenantId(1L)
                .status("REVIEW_ASSIGN").sourceType("FINISHED").processInstanceId("legacy-process").build());
        var result = service.getPackagingFlowRecord(1L);
        assertEquals("REVIEW_ASSIGN", result.getStatus());
        assertFalse(result.getCanHandle());
        verify(records, times(2)).selectById(1L);
        verifyNoMoreInteractions(records);
        verifyNoInteractions(runtime, pieces);
    }
    @Test void piecesQueryNeverUsesMutableDetailRead() {
        when(records.selectById(1L)).thenReturn(QmsNcRecordDO.builder().id(1L).tenantId(1L).build());
        when(pieces.selectPieces(1L,1L)).thenReturn(List.of());
        assertTrue(service.getPackagingFlowPieces(1L).isEmpty());
        verify(records).selectById(1L);
        verify(pieces).selectPieces(1L,1L);
        verifyNoMoreInteractions(records, pieces);
        verifyNoInteractions(runtime);
    }
    @Test void rejectsRawMaterialAndOtherTenant() {
        when(records.selectById(1L)).thenReturn(QmsNcRecordDO.builder().id(1L).tenantId(1L).sourceType("RAW_MATERIAL").build());
        assertThrows(ServiceException.class, () -> service.getPackagingFlowRecord(1L));
        when(records.selectById(1L)).thenReturn(QmsNcRecordDO.builder().id(1L).tenantId(2L).build());
        assertThrows(ServiceException.class, () -> service.getPackagingFlowPieces(1L));
        verifyNoInteractions(runtime, pieces);
    }
}
