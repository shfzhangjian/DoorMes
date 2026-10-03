package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcRecordStockDisposeReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.*;
import cn.iocoder.yudao.module.mes.dal.mysql.qms.*;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.batchtrace.HcBatchTraceFactDTO;
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
class QmsNcPickQualificationTest {
    @InjectMocks QmsNcPickQualificationService service;
    @Mock QmsNcPickMapper pickMapper;
    @Mock QmsFaiOrderMapper faiMapper;
    @Mock QmsNcDispositionScopeMapper scopeMapper;
    @Mock QmsNcDispositionExecutionMapper executionMapper;
    @Mock QmsNcReportGateMapper gateMapper;
    @Mock QmsNcWorkstationCommandMapper commandMapper;

    @BeforeEach void tenant() { TenantContextHolder.setTenantId(1L); }
    @AfterEach void cleanup() { TenantContextHolder.clear(); }

    HcBatchTraceFactDTO fact(String no, String type, String status) {
        HcBatchTraceFactDTO f = new HcBatchTraceFactDTO();
        f.setId(11L); f.setProductionBatchNo(no); f.setSourceType(type); f.setReportStatus(status);
        f.setSourceTypeName(type); f.setOutputLength(new BigDecimal("20.5")); f.setReportType("END");
        return f;
    }
    QmsNcDispositionScopeDO scope(String level, String mother, String segment) {
        return QmsNcDispositionScopeDO.builder().id(3L).executionId(2L).scopeLevel(level)
                .objectKey(level + ":" + (segment == null ? mother : segment))
                .motherBatchNo(mother).segmentBatchNo(segment).quantity(BigDecimal.ONE)
                .sourceObjectId(11L).sourceObjectType("ROUGH_GRINDING_SECOND")
                .dispositionType("PICK").scopeRole("PICK_ALLOW").executionResult("PENDING_PICK").build();
    }
    @Test void wetRollWithoutSegmentsUsesActualRollAndCountsRollNotMetres() {
        var list = QmsNcPickQualificationService.buildCandidates("W26J169A", true,
                List.of(fact("W26J169A", "WET_REPORT", "RUNNING")));
        assertEquals(1, list.size()); assertEquals("MOTHER_BATCH", list.get(0).getScopeLevel());
        assertEquals("卷", list.get(0).getQuantityUnit()); assertEquals(BigDecimal.ONE, list.get(0).getQuantity());
        assertEquals(new BigDecimal("20.5"), list.get(0).getProductionLength());
    }
    @Test void wetAlreadyTransferredStillUsesOnlyMotherBatch() {
        var list = QmsNcPickQualificationService.buildCandidates("W26J169A", true, List.of(
                fact("W26J169A", "WET_REPORT", "RUNNING"),
                fact("W26J169AP", "ROUGH_GRINDING_FIRST_ALLOCATION", "SUBMITTED"),
                fact("W26J169AP", "ROUGH_GRINDING_SECOND", "CONFIRMED"),
                fact("W26J169AQ", "ROUGH_GRINDING_SECOND", "CONFIRMED")));
        assertEquals(List.of("MOTHER_BATCH:W26J169A"), list.stream().map(c -> c.getObjectKey()).toList());
    }
    @Test void roughInvalidSegmentsNeverFallBackToWholeRoll() {
        assertTrue(QmsNcPickQualificationService.buildCandidates("W26J169A", false, List.of(
                fact("W26J169A", "ROUGH_GRINDING_FIRST", "CONFIRMED"),
                fact("W26J169AP", "ROUGH_GRINDING_FIRST_ALLOCATION", "SUBMITTED"),
                fact("W26J169AP", "ROUGH_GRINDING_SECOND", "SCRAPPED"))).isEmpty());
    }
    @Test void segmentSourceCannotExpandToSiblingOrMother() {
        var list = QmsNcPickQualificationService.buildCandidates("W26J169AP", false, List.of(
                fact("W26J169A", "ROUGH_GRINDING_FIRST", "CONFIRMED"),
                fact("W26J169AQ", "ROUGH_GRINDING_SECOND", "CONFIRMED"),
                fact("W26J169AP", "ROUGH_GRINDING_SECOND", "CONFIRMED")));
        assertEquals(1, list.size()); assertEquals("W26J169AP", list.get(0).getSegmentBatchNo());
    }
    @Test void startPlaceholderIsNotAnActualRoll() {
        var f = fact("W26J169A", "WET_REPORT", "RELEASED"); f.setReportType("START");
        assertTrue(QmsNcPickQualificationService.buildCandidates("W26J169A", true, List.of(f)).isEmpty());
    }
    @Test void partialQualificationDoesNotReleaseWholeRollOrSibling() {
        var s = scope("SEGMENT", "W26J169A", "W26J169AP");
        assertTrue(QmsNcPickQualificationService.covers(s, "W26J169AP-J1"));
        assertFalse(QmsNcPickQualificationService.covers(s, "W26J169AQ"));
        assertFalse(QmsNcPickQualificationService.covers(s, "W26J169A"));
        assertFalse(QmsNcPickQualificationService.covers(s, "W26J169AP001"));
    }
    @Test void motherCoverageRequiresRealSegmentBoundary() {
        var s = scope("MOTHER_BATCH", "W26J169A", null);
        assertTrue(QmsNcPickQualificationService.covers(s, "W26J169AP"));
        assertFalse(QmsNcPickQualificationService.covers(s, "W26J169AOTHER"));
        assertFalse(QmsNcPickQualificationService.covers(s, "W26J169B"));
    }
    @Test void noConfirmationCannotMutate() {
        assertThrows(ServiceException.class, () -> service.confirm(new QmsNcRecordDO(), new QmsNcRecordStockDisposeReqVO()));
        verifyNoInteractions(scopeMapper, executionMapper, commandMapper);
    }
    @Test void sourceInspectionMismatchRejected() {
        var n = QmsNcRecordDO.builder().sourceId(1L).lotNo("W26J169A").build();
        var f = new QmsFaiOrderDO(); f.setSourceModule("WET_REPORT"); f.setTenantId(1L); f.setProductBatchNo("W26J169B");
        when(faiMapper.selectById(1L)).thenReturn(f);
        assertThrows(ServiceException.class, () -> service.candidates(n)); verifyNoInteractions(pickMapper);
    }
    @Test void anotherTenantNeverUsesQualification() {
        var f = new QmsFaiOrderDO(); f.setId(1L); f.setTenantId(2L); f.setJudgment("NG"); f.setSourceModule("WET_REPORT");
        assertFalse(service.isQualified(f, "W26J169A")); verifyNoInteractions(pickMapper);
    }
    @Test void qualificationBoundToExactInspection() {
        when(pickMapper.selectQualifiedScopes(1122L, 1L)).thenReturn(List.of(scope("SEGMENT", "W26J169A", "W26J169AP")));
        assertTrue(service.isQualified(1122L, "W26J169AP"));
        assertFalse(service.isQualified(1122L, "W26J169AQ"));
        assertFalse(service.isQualified(1123L, "W26J169AP"));
    }
    @Test void confirmedSelectedScopeBecomesQualifiedWithoutChangingFai() {
        var n = QmsNcRecordDO.builder().id(1L).sourceId(1122L).tenantId(1L).processName("湿法").lotNo("W26J169A").build();
        var f = new QmsFaiOrderDO(); f.setId(1122L); f.setSourceModule("WET_REPORT"); f.setTenantId(1L); f.setProductBatchNo("W26J169A"); f.setJudgment("NG");
        var e = QmsNcDispositionExecutionDO.builder().id(2L).scopeLevel("SEGMENT").build();
        var s = scope("SEGMENT", "W26J169A", "W26J169AP");
        when(executionMapper.selectByNcRecordId(1L)).thenReturn(e);
        when(scopeMapper.selectListByExecutionId(2L)).thenReturn(List.of(s));
        when(pickMapper.lockProductionObject("ROUGH_GRINDING_SECOND", 11L, 1L)).thenReturn(11L);
        when(faiMapper.selectById(1122L)).thenReturn(f);
        when(pickMapper.selectProductionObjects("W26J169A", 1L)).thenReturn(List.of(
                fact("W26J169AP", "ROUGH_GRINDING_SECOND", "CONFIRMED"),
                fact("W26J169AQ", "ROUGH_GRINDING_SECOND", "CONFIRMED")));
        when(commandMapper.selectByExecutionId(2L)).thenReturn(QmsNcWorkstationCommandDO.builder().id(4L).build());
        var req = new QmsNcRecordStockDisposeReqVO(); req.setConfirmPickQualified(true);
        service.confirm(n, req);
        assertEquals("PICK_QUALIFIED", s.getExecutionResult()); assertEquals("COMPLETED", e.getExecutionStatus());
        assertEquals("NG", f.getJudgment()); verify(faiMapper, never()).updateById(any(QmsFaiOrderDO.class));
        verify(scopeMapper, times(1)).updateById(any(QmsNcDispositionScopeDO.class));
    }
    @Test void mixedSourceSegmentsMustAllBeQualifiedForTheMotherLock() {
        var lock = QmsSampleAbnormalLockDO.builder().objectNo("W26J169A").abnormalInspectionId(1122L).build();
        when(pickMapper.selectQualifiedScopes(1122L, 1L)).thenReturn(List.of(scope("SEGMENT", "W26J169A", "W26J169AP")));
        assertTrue(service.isLockQualified(lock, List.of("W26J169A", "W26J169AP")));
        assertFalse(service.isLockQualified(lock, List.of("W26J169A", "W26J169AP", "W26J169AQ")));
        assertFalse(service.isLockQualified(lock, List.of("W26J169A")));
    }
    @Test void newerNgRecheckIsNotReleasedByOriginalNcr() {
        var lock = QmsSampleAbnormalLockDO.builder().objectNo("W26J169A").abnormalInspectionId(1122L)
                .recheckInspectionId(1123L).recheckResult("NG").build();
        assertFalse(service.isLockQualified(lock, List.of("W26J169AP")));
        verify(pickMapper).selectQualifiedScopes(1123L, 1L);
        verify(pickMapper, never()).selectQualifiedScopes(1122L, 1L);
    }

    private void wetMother(List<QmsNcDispositionScopeDO> scopes, List<HcBatchTraceFactDTO> facts) {
        var fai = new QmsFaiOrderDO();
        fai.setId(1122L); fai.setTenantId(1L); fai.setSourceModule("WET_REPORT");
        fai.setProductBatchNo("W26J169A"); fai.setJudgment("NG");
        when(faiMapper.selectById(1122L)).thenReturn(fai);
        when(pickMapper.selectQualifiedScopes(1122L, 1L)).thenReturn(scopes);
        when(pickMapper.selectProductionObjects("W26J169A", 1L)).thenReturn(facts);
    }

    @Test void allActualSegmentsQualifyMotherAndMotherLockWithoutChangingInspection() {
        wetMother(List.of(scope("SEGMENT", "W26J169A", "W26J169AP"),
                        scope("SEGMENT", "W26J169A", "W26J169AQ")),
                List.of(fact("W26J169A", "WET_REPORT", "CONFIRMED"),
                        fact("W26J169AP", "ROUGH_GRINDING_SECOND", "CONFIRMED"),
                        fact("W26J169AQ", "ROUGH_GRINDING_SECOND", "CONFIRMED")));
        assertEquals(QmsNcPickQualificationService.Qualification.QUALIFIED, service.qualification(1122L, "W26J169A"));
        assertTrue(service.isLockQualified(QmsSampleAbnormalLockDO.builder().objectNo("W26J169A")
                .abnormalInspectionId(1122L).build(), List.of("W26J169A")));
        verify(faiMapper, never()).updateById(any(QmsFaiOrderDO.class));
    }

    @Test void uncoveredActualSegmentKeepsMotherPartialAndSiblingNg() {
        wetMother(List.of(scope("SEGMENT", "W26J169A", "W26J169AP")),
                List.of(fact("W26J169AP", "ROUGH_GRINDING_SECOND", "CONFIRMED"),
                        fact("W26J169AQ", "ROUGH_GRINDING_FIRST_ALLOCATION", "SUBMITTED")));
        assertEquals(QmsNcPickQualificationService.Qualification.PARTIAL, service.qualification(1122L, "W26J169A"));
        assertFalse(service.isQualified(1122L, "W26J169A"));
        assertTrue(service.isQualified(1122L, "W26J169AP"));
        assertFalse(service.isQualified(1122L, "W26J169AQ"));
    }

    @Test void fourSelectedLettersCannotSubstituteForActualProductionEvidence() {
        wetMother(List.of(scope("SEGMENT", "W26J169A", "W26J169AP"),
                scope("SEGMENT", "W26J169A", "W26J169AQ"), scope("SEGMENT", "W26J169A", "W26J169AR"),
                scope("SEGMENT", "W26J169A", "W26J169AS")), List.of());
        assertFalse(service.isQualified(1122L, "W26J169A"));
    }

    @Test void staleScopesDoNotCountAsPartialQualificationOfDifferentActualSegments() {
        wetMother(List.of(scope("SEGMENT", "W26J169A", "W26J169AP")),
                List.of(fact("W26J169AQ", "ROUGH_GRINDING_SECOND", "CONFIRMED")));
        assertEquals(QmsNcPickQualificationService.Qualification.NONE, service.qualification(1122L, "W26J169A"));
    }

    @Test void wetCannotUseDownstreamSegmentAsMotherEvidence() {
        assertTrue(QmsNcPickQualificationService.buildCandidates("W26J169A", true,
                List.of(fact("W26J169AP", "ROUGH_GRINDING_SECOND", "CONFIRMED"))).isEmpty());
        assertTrue(QmsNcPickQualificationService.buildCandidates("W26J169AP", true,
                List.of(fact("W26J169AP", "WET_REPORT", "CONFIRMED"))).isEmpty());
    }

    @Test void downstreamScrapDoesNotReplaceWetMotherObject() {
        var rows = QmsNcPickQualificationService.buildCandidates("W26J169A", true, List.of(
                fact("W26J169A", "WET_REPORT", "CONFIRMED"),
                fact("W26J169AP", "ROUGH_GRINDING_SECOND", "SCRAPPED")));
        assertEquals(List.of("MOTHER_BATCH:W26J169A"), rows.stream().map(c -> c.getObjectKey()).toList());
    }

    @Test void roughWithoutActualSegmentsCannotOfferMotherBatch() {
        assertTrue(QmsNcPickQualificationService.buildCandidates("W26J169A", false,
                List.of(fact("W26J169A", "ROUGH_GRINDING_FIRST", "CONFIRMED"))).isEmpty());
    }

    @Test void adhesiveOnlyUsesItsOwnSegmentRecordsAndNormalizesPassSuffix() {
        var rows = QmsNcPickQualificationService.buildCandidates("W26J169AP", "ADHESIVE_REPORT", List.of(
                fact("W26J169A", "ADHESIVE_REPORT", "CONFIRMED"),
                fact("W26J169AP", "ROUGH_GRINDING_SECOND", "CONFIRMED"),
                fact("W26J169AQ", "ADHESIVE_REPORT", "CONFIRMED"),
                fact("W26J169AP-J1", "ADHESIVE_REPORT", "CONFIRMED")));
        assertEquals(1, rows.size());
        assertEquals("SEGMENT:W26J169AP", rows.get(0).getObjectKey());
        assertEquals("ADHESIVE_REPORT", rows.get(0).getSourceObjectType());
    }

    @Test void adhesiveCannotBorrowRoughInspection() {
        var n = QmsNcRecordDO.builder().sourceBizType("FAI").sourceId(1L).processName("粘胶1")
                .finalDisposition("PICK").lotNo("W26J169AP").build();
        assertTrue(service.isPick(n));
        var f = new QmsFaiOrderDO(); f.setSourceModule("ROUGH_GRINDING_SECOND_SEGMENT");
        f.setTenantId(1L); f.setProductBatchNo("W26J169AP");
        when(faiMapper.selectById(1L)).thenReturn(f);
        assertThrows(ServiceException.class, () -> service.candidates(n));
        verifyNoInteractions(pickMapper);
    }

    @Test void adhesiveSourceCanSelectSegmentWithoutPieces() {
        var n = QmsNcRecordDO.builder().sourceBizType("FAI").sourceId(1L).processName("粘胶1")
                .finalDisposition("PICK").lotNo("W26J169AP-J1").build();
        var f = new QmsFaiOrderDO(); f.setSourceModule("ADHESIVE_REPORT");
        f.setTenantId(1L); f.setProductBatchNo("W26J169AP");
        when(faiMapper.selectById(1L)).thenReturn(f);
        when(pickMapper.selectProductionObjects("W26J169AP", 1L)).thenReturn(
                List.of(fact("W26J169AP-J1", "ADHESIVE_REPORT", "CONFIRMED")));
        assertEquals("SEGMENT:W26J169AP", service.candidates(n).get(0).getObjectKey());
    }

    @Test void wetMotherConfirmationAndAdhesiveSegmentConfirmationKeepOriginalNg() {
        confirmNewScope("湿法", "WET_REPORT", "W26J169A", "MOTHER_BATCH");
        assertTrue(service.isQualified(1122L, "W26J169AQ"));
        assertFalse(service.isQualified(1123L, "W26J169AQ"));
        confirmNewScope("粘胶1", "ADHESIVE_REPORT", "W26J169AP", "SEGMENT");
        assertTrue(service.isQualified(1122L, "W26J169AP-J1"));
        assertFalse(service.isQualified(1122L, "W26J169AQ"));
    }

    private void confirmNewScope(String process, String module, String lot, String level) {
        var n = QmsNcRecordDO.builder().id(1L).sourceId(1122L).tenantId(1L)
                .processName(process).lotNo(lot).build();
        var f = new QmsFaiOrderDO(); f.setId(1122L); f.setSourceModule(module);
        f.setTenantId(1L); f.setProductBatchNo(lot); f.setJudgment("NG");
        var e = QmsNcDispositionExecutionDO.builder().id(2L).scopeLevel(level).build();
        var s = scope(level, "W26J169A", "SEGMENT".equals(level) ? lot : null); s.setSourceObjectType(module);
        when(executionMapper.selectByNcRecordId(1L)).thenReturn(e);
        when(scopeMapper.selectListByExecutionId(2L)).thenReturn(List.of(s));
        when(pickMapper.lockProductionObject(module, 11L, 1L)).thenReturn(11L);
        when(faiMapper.selectById(1122L)).thenReturn(f);
        when(pickMapper.selectProductionObjects(lot, 1L)).thenReturn(List.of(fact(lot, module, "CONFIRMED")));
        when(commandMapper.selectByExecutionId(2L)).thenReturn(QmsNcWorkstationCommandDO.builder().id(4L).build());
        var req = new QmsNcRecordStockDisposeReqVO(); req.setConfirmPickQualified(true);
        service.confirm(n, req);
        when(pickMapper.selectQualifiedScopes(1122L, 1L)).thenReturn(List.of(s));
        assertTrue(service.isQualified(f, lot));
        assertEquals("PICK_QUALIFIED", s.getExecutionResult());
        assertEquals("COMPLETED", e.getExecutionStatus());
        assertEquals("NG", f.getJudgment());
        verify(faiMapper, never()).updateById(any(QmsFaiOrderDO.class));
    }

    @Test void adhesiveWithoutReportUsesOnlyExplicitConfirmedFaiSourceSegment() {
        assertEquals(1, adhesiveSourceCandidates(11L, "PLAN-05-ADH-W26J169AP-11", "CONFIRMED", false).size());
    }

    @Test void adhesiveWithoutReportRejectsUnrelatedOrUnconfirmedSource() {
        assertTrue(adhesiveSourceCandidates(12L, "PLAN-05-ADH-W26J169AP-11", "CONFIRMED", false).isEmpty());
        assertTrue(adhesiveSourceCandidates(11L, "PLAN-05-ADH-W26J169AQ-11", "CONFIRMED", false).isEmpty());
        assertTrue(adhesiveSourceCandidates(11L, "PLAN-05-ADH-W26J169AP-11", "DRAFT", false).isEmpty());
    }

    @Test void canceledAdhesiveReportCannotFallBackToUpstreamSegment() {
        assertTrue(adhesiveSourceCandidates(11L, "PLAN-05-ADH-W26J169AP-11", "CONFIRMED", true).isEmpty());
    }

    private List<cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsNcDispositionContextRespVO.ScopeCandidate>
            adhesiveSourceCandidates(Long sourceId, String sourceNo, String status, boolean canceledReport) {
        var n = QmsNcRecordDO.builder().sourceId(1L).processName("粘胶1").lotNo("W26J169AP").build();
        var f = new QmsFaiOrderDO(); f.setSourceModule("ADHESIVE_REPORT");
        f.setTenantId(1L); f.setProductBatchNo("W26J169AP");
        f.setSourceReportId(sourceId); f.setSourceReportNo(sourceNo);
        when(faiMapper.selectById(1L)).thenReturn(f);
        var source = fact("W26J169AP", "ROUGH_GRINDING_SECOND", status);
        when(pickMapper.selectProductionObjects("W26J169AP", 1L)).thenReturn(canceledReport
                ? List.of(source, fact("W26J169AP-J1", "ADHESIVE_REPORT", "CANCELED")) : List.of(source));
        return service.candidates(n);
    }

}
