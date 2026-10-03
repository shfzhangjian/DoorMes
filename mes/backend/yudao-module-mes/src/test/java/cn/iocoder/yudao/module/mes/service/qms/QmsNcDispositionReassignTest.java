package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.*;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class QmsNcDispositionReassignTest {
    @InjectMocks QmsNcDispositionServiceImpl service;
    @Mock QmsNcRecordMapper qmsNcRecordMapper;
    @Mock QmsNcDispositionExecutionMapper executionMapper;
    @Mock QmsNcDispositionScopeMapper scopeMapper;
    @Mock QmsNcWorkstationCommandMapper commandMapper;
    @Mock QmsNcReportGateMapper reportGateMapper;
    @Mock QmsNcRecordService ncRecordService;
    @Mock QmsNcPickQualificationService pickQualificationService;
    @Mock AdminUserApi adminUserApi;

    @ParameterizedTest
    @CsvSource({"4,1,1", "4,5,4", ",5,5"})
    void reassignUsesMainRecordQuantityWithoutChangingObjectScope(
            BigDecimal affectedQty, BigDecimal defectQty, BigDecimal expectedQty) {
        var ncr = QmsNcRecordDO.builder().id(82L).ncNo("NCR-20260922BCP002")
                .sourceType("SEMI_FINISHED").processName("湿法").lotNo("W26H158A")
                .finalDisposition("PICK").status("EXECUTION_ASSIGN").defectQty(defectQty).build();
        var execution = QmsNcDispositionExecutionDO.builder().id(91L).ncRecordId(82L)
                .scopeLevel("SEGMENT").affectedQty(affectedQty).selectedQty(affectedQty)
                .executionStatus("EXECUTING").build();
        var scopes = List.of("W26H158AS", "W26H158AR", "W26H158AQ", "W26H158AP").stream()
                .map(no -> QmsNcDispositionScopeDO.builder().segmentBatchNo(no).scopeLevel("SEGMENT")
                        .quantity(BigDecimal.ONE).executionResult("PENDING_PICK").build()).toList();
        when(qmsNcRecordMapper.selectByIdForUpdate(82L)).thenReturn(ncr);
        when(executionMapper.selectByNcRecordId(82L)).thenReturn(execution);
        when(executionMapper.selectById(91L)).thenReturn(execution);
        when(scopeMapper.selectListByExecutionId(91L)).thenReturn(scopes);
        when(pickQualificationService.isPick(ncr)).thenReturn(true);
        var user = new AdminUserRespDTO(); user.setId(184L); user.setNickname("执行人");
        when(adminUserApi.getUser(184L)).thenReturn(user);
        // 模拟主单现有数量校验，确保该公开入口实际传入可通过校验的数量。
        doAnswer(invocation -> {
            QmsNcRecordHandleReqVO handle = invocation.getArgument(0);
            assertTrue(handle.getStockDisposeQty().signum() > 0);
            assertTrue(handle.getStockDisposeQty().compareTo(defectQty) <= 0);
            return null;
        }).when(ncRecordService).handleNcRecord(any());
        var req = new QmsNcDispositionConfirmReqVO();
        req.setId(82L); req.setExecutionUserId(184L); req.setRemark("123");
        var response = service.confirmDispositionScope(req);
        var handle = ArgumentCaptor.forClass(QmsNcRecordHandleReqVO.class);
        verify(ncRecordService).handleNcRecord(handle.capture());
        assertEquals(expectedQty, handle.getValue().getStockDisposeQty());
        assertEquals(defectQty, ncr.getDefectQty());
        assertEquals(affectedQty, response.getAffectedQty());
        assertEquals(affectedQty, response.getSelectedQty());
        assertEquals(4, response.getScopes().size());
        assertTrue(response.getScopes().stream().allMatch(scope -> "PICK_QUALIFIED".equals(scope.getExecutionResult())));
        var update = ArgumentCaptor.forClass(QmsNcDispositionExecutionDO.class);
        verify(executionMapper, times(2)).updateById(update.capture());
        assertTrue(update.getAllValues().stream().anyMatch(item -> "COMPLETED".equals(item.getExecutionStatus())));
        assertTrue(update.getAllValues().stream().allMatch(item -> item.getAffectedQty() == null && item.getSelectedQty() == null));
        verify(scopeMapper, never()).insert(any(QmsNcDispositionScopeDO.class));
        verify(reportGateMapper).selectListByExecutionId(91L);
    }
}
