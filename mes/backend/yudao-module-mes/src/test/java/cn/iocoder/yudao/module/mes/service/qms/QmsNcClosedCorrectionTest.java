package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcClosedCorrectionReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.*;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.nginventory.HcNgInventoryPieceDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging.HcInnerPackUnitDO;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.*;
import java.util.List;
import java.time.LocalDateTime;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class QmsNcClosedCorrectionTest {
    @InjectMocks QmsNcDispositionServiceImpl service;
    @Mock QmsNcRecordMapper qmsNcRecordMapper;
    @Mock QmsNcDispositionExecutionMapper executionMapper;
    @Mock QmsNcDispositionScopeMapper scopeMapper;
    @Mock QmsNcReportGateMapper reportGateMapper;
    @Mock QmsNcWorkstationCommandMapper commandMapper;
    @Mock QmsNcClosedCorrectionMapper correctionMapper;
    @Mock QmsNcFlowLogMapper correctionLogMapper;
    @Mock QmsCutRoundFqcService qmsCutRoundFqcService;
    QmsNcRecordDO ncr;
    QmsNcDispositionScopeDO first;
    QmsNcDispositionScopeDO second;
    QmsNcWorkstationCommandDO command;

    @BeforeEach void setup() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"),
                QmsNcReportGateDO.class);
        TenantContextHolder.setTenantId(1L);
        ncr = QmsNcRecordDO.builder().id(10L).tenantId(1L).status("CLOSED").ncNo("NCR-TEST").build();
        when(qmsNcRecordMapper.selectByIdForUpdate(10L)).thenReturn(ncr);
    }
    void execution() {
        when(executionMapper.selectByNcRecordId(10L)).thenReturn(QmsNcDispositionExecutionDO.builder().id(20L).build());
        first = QmsNcDispositionScopeDO.builder().id(30L).scopeLevel("PIECE").pieceNo("P001")
                .dispositionType("SCRAP").scopeRole("SELECTED").build();
        second = QmsNcDispositionScopeDO.builder().id(31L).scopeLevel("PIECE").pieceNo("P002")
                .dispositionType("PICK").scopeRole("PICK_ALLOW").build();
        when(scopeMapper.selectListByExecutionId(20L)).thenReturn(List.of(first, second));
    }
    void command() {
        command = QmsNcWorkstationCommandDO.builder().id(40L).commandVersion(1).build();
        when(commandMapper.selectByExecutionId(20L)).thenReturn(command);
    }
    QmsNcClosedCorrectionReqVO request(Long scope, String from, String to) {
        var req = new QmsNcClosedCorrectionReqVO(); req.setId(10L); req.setReason("处置更正");
        var change = new QmsNcClosedCorrectionReqVO.Change(); change.setScopeId(scope);
        change.setOriginalDisposition(from); change.setDispositionType(to); req.setChanges(List.of(change));
        return req;
    }
    @AfterEach void clear() { TenantContextHolder.clear(); }

    @Test void rejectsOpenRecord() {
        ncr.setStatus("EXECUTION_ASSIGN");
        assertThrows(ServiceException.class, () -> service.getClosedCorrection(10L));
        verifyNoInteractions(executionMapper);
    }
    @Test void rejectsOtherTenant() {
        ncr.setTenantId(2L);
        assertThrows(ServiceException.class, () -> service.getClosedCorrection(10L));
    }
    @Test void previewContainsOnlyActualChangesAndDoesNotWrite() {
        execution(); command();
        var result = service.previewClosedCorrection(request(30L, "SCRAP", "PICK"));
        assertEquals(1, result.getPieces().size());
        assertEquals("P001", result.getPieces().get(0).getPieceNo());
        assertNotNull(result.getPreviewToken());
        verify(scopeMapper, never()).updateById(any(QmsNcDispositionScopeDO.class));
        verifyNoInteractions(correctionLogMapper, qmsCutRoundFqcService);
    }
    @Test void rejectsShelvedEvenWhenReturnedToWaitShelf() {
        execution();
        when(correctionMapper.lockNgPieces(1L, "P001")).thenReturn(List.of(HcNgInventoryPieceDO.builder()
                .status("WAIT_SHELF").shelvedTime(LocalDateTime.of(2026, 9, 8, 10, 0)).build()));
        assertThrows(ServiceException.class, () -> service.previewClosedCorrection(request(30L,"SCRAP","PICK")));
    }
    @Test void packedButNotShelvedCanPreview() {
        execution(); command();
        when(correctionMapper.lockPackages(1L, "P001")).thenReturn(List.of(HcInnerPackUnitDO.builder().unitStatus("PACKED").build()));
        assertEquals(1, service.previewClosedCorrection(request(30L,"SCRAP","PICK")).getPieces().size());
    }
    @Test void rejectsShelfBetweenPreviewAndConfirmationWithoutWrites() {
        execution(); command();
        var req = request(30L,"SCRAP","PICK");
        req.setPreviewToken(service.previewClosedCorrection(req).getPreviewToken());
        when(correctionMapper.countShelvedStock(1L,"P001")).thenReturn(1);
        assertThrows(ServiceException.class, () -> service.saveClosedCorrection(req));
        verify(scopeMapper, never()).updateById(any(QmsNcDispositionScopeDO.class));
        verifyNoInteractions(correctionLogMapper, qmsCutRoundFqcService);
    }
    @Test void rejectsStaleCommandVersion() {
        execution(); command();
        var req = request(30L,"SCRAP","PICK");
        req.setPreviewToken(service.previewClosedCorrection(req).getPreviewToken());
        command.setCommandVersion(2);
        assertThrows(ServiceException.class, () -> service.saveClosedCorrection(req));
        verify(scopeMapper, never()).updateById(any(QmsNcDispositionScopeDO.class));
    }
    @Test void rejectsUnknownScopeAndChangedOriginal() {
        execution();
        assertThrows(ServiceException.class, () -> service.previewClosedCorrection(request(99L,"SCRAP","PICK")));
        assertThrows(ServiceException.class, () -> service.previewClosedCorrection(request(30L,"PICK","SCRAP")));
    }
    @Test void rejectsMissingPreviewToken() {
        execution(); command();
        assertThrows(ServiceException.class, () -> service.saveClosedCorrection(request(30L,"SCRAP","PICK")));
        verify(scopeMapper, never()).updateById(any(QmsNcDispositionScopeDO.class));
    }
    @Test void saveChangesOnlySelectedPieceAndPreservesClosure() {
        execution(); command();
        var req = request(30L,"SCRAP","PICK");
        req.setPreviewToken(service.previewClosedCorrection(req).getPreviewToken());
        service.saveClosedCorrection(req);
        verify(scopeMapper).updateById(first);
        verify(scopeMapper, never()).updateById(second);
        assertEquals("PICK", first.getDispositionType());
        assertEquals("PICK_ALLOW", first.getScopeRole());
        assertEquals("CLOSED", ncr.getStatus());
        verify(correctionMapper).updatePackageQuality(eq(1L), eq("P001"), eq("OK"), anyString());
        verify(correctionMapper).updatePendingStockQuality(eq(1L), eq("P001"), eq("OK"), anyString());
        verify(qmsCutRoundFqcService).applyNcrDispositionResult(eq(ncr), any(), eq(List.of(first)));
        verify(correctionLogMapper).insert(any(QmsNcFlowLogDO.class));
        assertEquals(2, command.getCommandVersion());
    }

}
