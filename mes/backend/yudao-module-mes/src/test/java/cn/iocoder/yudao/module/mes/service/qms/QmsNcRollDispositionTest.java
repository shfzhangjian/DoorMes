package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.*;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.*;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class QmsNcRollDispositionTest {
    @InjectMocks QmsNcDispositionServiceImpl service;
    @Mock QmsNcRecordMapper qmsNcRecordMapper;
    @Mock QmsNcDispositionExecutionMapper executionMapper;
    @Mock QmsNcDispositionScopeMapper scopeMapper;
    @Mock QmsNcReportGateMapper reportGateMapper;
    @Mock QmsNcWorkstationCommandMapper commandMapper;
    @Mock QmsNcRelationMapper relationMapper;
    @Mock QmsNcPickQualificationService pickQualificationService;
    @Mock QmsNcRecordService ncRecordService;
    @Mock QmsCutRoundFqcService qmsCutRoundFqcService;
    @Mock AdminUserApi adminUserApi;
    QmsNcRecordDO ncr;
    @BeforeEach void setup() {
        TenantContextHolder.setTenantId(1L);
        ncr = QmsNcRecordDO.builder().id(82L).tenantId(1L).ncNo("NCR-20260918BCP001")
                .sourceType("SEMI_FINISHED").sourceBizType("FAI").sourceId(1122L)
                .processName("湿法").lotNo("W26J169A").finalDisposition("PICK")
                .status("EXECUTION_ASSIGN").defectQty(BigDecimal.ONE).build();
        when(pickQualificationService.supports(ncr)).thenReturn(true);
        when(pickQualificationService.isPick(ncr)).thenReturn(true);
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }
    @Test void emptyCandidatesStillReturnHeaderAndReason() {
        when(qmsNcRecordMapper.selectById(82L)).thenReturn(ncr);
        when(pickQualificationService.candidates(ncr)).thenReturn(List.of());
        var response = service.getDispositionContext(82L);
        assertEquals("湿法", response.getNgProcessName()); assertEquals("PICK", response.getFinalDisposition());
        assertEquals("W26J169A", response.getSourceLotNo()); assertNotNull(response.getEmptyReason());
        assertTrue(response.getCandidates().isEmpty()); assertNull(response.getDefaultScopeLevel());
    }
    QmsNcDispositionContextRespVO.ScopeCandidate candidate(String no) {
        var c = new QmsNcDispositionContextRespVO.ScopeCandidate();
        c.setScopeLevel("SEGMENT"); c.setObjectKey("SEGMENT:" + no);
        c.setMotherBatchNo("W26J169A"); c.setSegmentBatchNo(no); c.setQuantity(BigDecimal.ONE);
        c.setSourceObjectId(243L); c.setSourceObjectType("ROUGH_GRINDING_SECOND"); c.setSourceObjectNo(no);
        return c;
    }
    @Test void assigningOneOfTwoSegmentsImmediatelyQualifiesSelectedSegmentOnly() {
        when(qmsNcRecordMapper.selectByIdForUpdate(82L)).thenReturn(ncr);
        when(pickQualificationService.candidates(ncr)).thenReturn(List.of(candidate("W26J169AP"), candidate("W26J169AQ")));
        var user = new AdminUserRespDTO(); user.setId(184L); user.setNickname("执行人");
        when(adminUserApi.getUser(184L)).thenReturn(user);
        var req = new QmsNcDispositionConfirmReqVO(); req.setId(82L); req.setScopeLevel("SEGMENT");
        req.setSelectedObjectKeys(List.of("SEGMENT:W26J169AP")); req.setExecutionUserId(184L);
        var response = service.confirmDispositionScope(req);
        assertEquals("COMPLETED", response.getExecutionStatus()); assertEquals("APPLIED", response.getCommandStatus());
        assertEquals(BigDecimal.ZERO, response.getDerivedScrapQty()); assertEquals(1, response.getScopes().size());
        assertEquals("W26J169AP", response.getScopes().get(0).getSegmentBatchNo());
        assertEquals("PICK_QUALIFIED", response.getScopes().get(0).getExecutionResult());
        ArgumentCaptor<QmsNcReportGateDO> gates = ArgumentCaptor.forClass(QmsNcReportGateDO.class);
        verify(reportGateMapper).insert(gates.capture());
        assertEquals("ALLOW", gates.getValue().getGateStatus()); assertNotNull(gates.getValue().getEffectiveTime());
        verify(scopeMapper, times(1)).insert(any(QmsNcDispositionScopeDO.class));
        verify(pickQualificationService, never()).confirm(any(), any());
    }
}
