package cn.iocoder.yudao.module.mes.service.hc.processreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2.HcAdhesive2ReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableConsumeDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger.HcToolingConsumableLedgerDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive2.HcAdhesive2ReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableConsumeMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.toolingconsumableledger.HcToolingConsumableLedgerMapper;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HcAdhesiveProductionRecordServiceImplTest {

    @Mock
    private HcProductionRecordPadTypeResolver padTypeResolver;
    @Mock
    private cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService productionRecordRevisionService;

    @org.junit.jupiter.api.BeforeEach
    void prepareProjection() {
        when(productionRecordRevisionService.applyRevisions(any(), any())).thenAnswer(call -> call.getArgument(1));
        when(padTypeResolver.resolveByModelCodes(any())).thenReturn(Map.of());
        org.mockito.Mockito.lenient().when(padTypeResolver.matchesFilter(any(), any())).thenReturn(true);
    }

    @InjectMocks
    private HcAdhesiveProductionRecordServiceImpl service;

    @Mock
    private HcAdhesiveReportMapper adhesiveReportMapper;
    @Mock
    private HcAdhesive2ReportMapper adhesive2ReportMapper;
    @Mock
    private HcToolingConsumableConsumeMapper toolingConsumableConsumeMapper;
    @Mock
    private HcToolingConsumableLedgerMapper toolingConsumableLedgerMapper;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    void shouldSplitAdhesive2ByDayAndGlueBoardWithoutDuplicatingQuantities() {
        HcAdhesive2ReportDO firstDay = adhesive2Report(1L, LocalDate.of(2026, 7, 16),
                "W26G147AP001", "01.02.00001", "GLUE-A");
        HcAdhesive2ReportDO secondDay = adhesive2Report(2L, LocalDate.of(2026, 7, 17),
                "W26G147AP002", "01.02.00001", "GLUE-A");
        HcAdhesive2ReportDO anotherGlueBoard = adhesive2Report(3L, LocalDate.of(2026, 7, 17),
                "W26G147AP003", "01.02.00001", "GLUE-B");
        when(adhesive2ReportMapper.selectList(any())).thenReturn(
                List.of(firstDay, secondDay, anotherGlueBoard));

        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive2List(
                new HcAdhesiveProductionRecordPageReqVO());

        assertEquals(3, rows.size());
        HcAdhesiveProductionRecordRespVO glueA = rows.stream()
                .filter(row -> "GLUE-A".equals(row.getGlueBoardBatchNo())
                        && LocalDate.of(2026, 7, 17).equals(row.getReportDate()))
                .findFirst().orElseThrow();
        assertEquals("W26G147AP", glueA.getBatchNo());
        assertEquals(0, BigDecimal.ONE.compareTo(glueA.getInputQty()));
        assertEquals(0, BigDecimal.ONE.compareTo(glueA.getOutputQty()));
        assertEquals(0, new BigDecimal("1.200").compareTo(glueA.getGlueBoardConsumeQty()));
        assertEquals(LocalDate.of(2026, 7, 17), glueA.getReportDate());
    }

    @Test
    void shouldShowAdhesive1BeforeSegmentCompletion() {
        HcAdhesiveReportDO confirmedReport = HcAdhesiveReportDO.builder()
                .id(10L)
                .reportDate(LocalDate.of(2026, 7, 17))
                .modelCode("W33P0100")
                .materialCode("03.13.10053")
                .sourceProductionBatchNo("W26G147AP")
                .inputLength(BigDecimal.ONE)
                .outputLength(BigDecimal.ONE)
                .reportStatus("CONFIRMED")
                .build();
        when(adhesiveReportMapper.selectList(any())).thenReturn(List.of(confirmedReport));

        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive1List(
                new HcAdhesiveProductionRecordPageReqVO());

        assertEquals(1, rows.size());
    }

    @Test
    void shouldShowAdhesive2BeforeSegmentCompletion() {
        HcAdhesive2ReportDO confirmedReport = adhesive2Report(20L, LocalDate.of(2026, 7, 17),
                "W26G147AQ001", "01.02.00001", "GLUE-A");
        confirmedReport.setReportStatus("CONFIRMED");
        when(adhesive2ReportMapper.selectList(any())).thenReturn(List.of(confirmedReport));

        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive2List(
                new HcAdhesiveProductionRecordPageReqVO());

        assertEquals(1, rows.size());
    }

    @Test
    void shouldProjectRAndDSampleGlueBoardConsumesWithoutMassProductionReport() {
        HcToolingConsumableConsumeDO first = rndConsume(31L, new BigDecimal("12.500"),
                new BigDecimal("10.000"), new BigDecimal("8.000"), LocalDateTime.of(2026, 7, 18, 9, 30));
        HcToolingConsumableConsumeDO correction = rndConsume(32L, new BigDecimal("-2.500"),
                new BigDecimal("2.000"), new BigDecimal("1.000"), LocalDateTime.of(2026, 7, 18, 10, 30));
        when(toolingConsumableConsumeMapper.selectList(any())).thenReturn(List.of(first, correction));
        when(toolingConsumableLedgerMapper.selectBatchIds(any())).thenReturn(List.of(
                HcToolingConsumableLedgerDO.builder()
                        .id(41L)
                        .erpMaterialCode("01.02.00001")
                        .build()));

        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive1List(
                new HcAdhesiveProductionRecordPageReqVO());

        assertEquals(1, rows.size());
        HcAdhesiveProductionRecordRespVO row = rows.get(0);
        assertEquals("W33P0100", row.getModelCode());
        assertEquals("03.13.10053", row.getMaterialCode());
        assertEquals("RND-20260718-01", row.getBatchNo());
        assertEquals("01.02.00001", row.getGlueBoardMaterialCode());
        assertEquals("GB-RND-001", row.getGlueBoardBatchNo());
        assertEquals(0, new BigDecimal("10.000").compareTo(row.getGlueBoardConsumeQty()));
        assertEquals(0, new BigDecimal("8.000").compareTo(row.getInputQty()));
        assertEquals(0, new BigDecimal("7.000").compareTo(row.getOutputQty()));
        assertEquals("研发样品/手工消耗", row.getRecordSource());
        assertEquals(LocalDate.of(2026, 7, 18), row.getReportDate());
        assertEquals(LocalDateTime.of(2026, 7, 18, 10, 30), row.getRecordTime());
    }

    @Test
    void shouldResolveNumericCreatorToNicknameForAdhesive2RAndDSample() {
        HcToolingConsumableConsumeDO consume = rndConsume(41L, new BigDecimal("2.000"),
                new BigDecimal("2.000"), new BigDecimal("2.000"), LocalDateTime.of(2026, 8, 8, 10, 30));
        consume.setProcessCode("ADHESIVE2");
        consume.setCreator("164");
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(164L);
        user.setNickname("粘胶操作员");
        when(toolingConsumableConsumeMapper.selectList(any())).thenReturn(List.of(consume));
        when(toolingConsumableLedgerMapper.selectBatchIds(any())).thenReturn(List.of(
                HcToolingConsumableLedgerDO.builder().id(41L).erpMaterialCode("01.02.00001").build()));
        when(adminUserApi.getUserMap(any())).thenReturn(Map.of(164L, user));

        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive2List(
                new HcAdhesiveProductionRecordPageReqVO());

        assertEquals(1, rows.size());
        assertEquals("粘胶操作员", rows.get(0).getRecorderName());
        assertEquals("研发样品/手工消耗", rows.get(0).getRecordSource());
    }

    @Test
    void shouldKeepDailyRevisionIdStableWhenMoreReportsArrive() {
        HcAdhesive2ReportDO first = adhesive2Report(1L, LocalDate.of(2026, 9, 24), "P001", "GLUE", "A");
        HcAdhesive2ReportDO sameDay = adhesive2Report(99L, first.getReportDate(), "P002", "GLUE", "A");
        HcAdhesive2ReportDO nextDay = adhesive2Report(100L, first.getReportDate().plusDays(1), "P003", "GLUE", "A");
        when(adhesive2ReportMapper.selectList(any())).thenReturn(List.of(first), List.of(first, sameDay, nextDay));
        Long originalId = service.getAdhesive2List(new HcAdhesiveProductionRecordPageReqVO()).get(0).getId();
        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive2List(new HcAdhesiveProductionRecordPageReqVO());
        HcAdhesiveProductionRecordRespVO today = rows.stream().filter(r -> first.getReportDate().equals(r.getReportDate())).findFirst().orElseThrow();
        assertEquals(originalId, today.getId());
        assertEquals(0, new BigDecimal("2").compareTo(today.getOutputQty()));
        assertNotEquals(originalId, rows.stream().filter(r -> nextDay.getReportDate().equals(r.getReportDate())).findFirst().orElseThrow().getId());
        assertNotEquals(first.getId(), originalId);
        assertNotEquals(sameDay.getId(), today.getId());
    }

    @Test
    void shouldExcludeDraftsFromBothAdhesiveTables() {
        HcAdhesiveReportDO draft1 = HcAdhesiveReportDO.builder().reportStatus("DRAFT").build();
        HcAdhesive2ReportDO draft2 = adhesive2Report(1L, LocalDate.of(2026, 9, 24), "P001", "GLUE", "A");
        draft2.setReportStatus("DRAFT");
        when(adhesiveReportMapper.selectList(any())).thenReturn(List.of(draft1));
        when(adhesive2ReportMapper.selectList(any())).thenReturn(List.of(draft2));
        assertEquals(0, service.getAdhesive1List(new HcAdhesiveProductionRecordPageReqVO()).size());
        assertEquals(0, service.getAdhesive2List(new HcAdhesiveProductionRecordPageReqVO()).size());
    }

    @Test
    void shouldSplitAdhesive2SampleConsumesAcrossDays() {
        HcToolingConsumableConsumeDO first = rndConsume(1L, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.TEN,
                LocalDateTime.of(2026, 9, 24, 16, 0));
        HcToolingConsumableConsumeDO second = rndConsume(2L, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE,
                LocalDateTime.of(2026, 9, 25, 9, 0));
        when(toolingConsumableConsumeMapper.selectList(any())).thenReturn(List.of(first, second));
        when(toolingConsumableLedgerMapper.selectBatchIds(any())).thenReturn(List.of());
        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive2List(new HcAdhesiveProductionRecordPageReqVO());
        assertEquals(2, rows.size());
        assertEquals(0, new BigDecimal("11").compareTo(rows.stream().map(HcAdhesiveProductionRecordRespVO::getOutputQty).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void shouldKeepAdhesive1DailyQuantitiesAndIdentityAfterSegmentCompletion() {
        HcAdhesiveReportDO first = HcAdhesiveReportDO.builder().id(1L).reportDate(LocalDate.of(2026, 9, 24))
                .sourceProductionBatchNo("P").reportStatus("CONFIRMED").inputLength(new BigDecimal("60"))
                .outputLength(new BigDecimal("60")).recorderName("甲").build();
        HcAdhesiveReportDO next = HcAdhesiveReportDO.builder().id(2L).reportDate(first.getReportDate().plusDays(1))
                .sourceProductionBatchNo("P").reportStatus("CONFIRMED").inputLength(new BigDecimal("40"))
                .outputLength(new BigDecimal("40")).recorderName("乙").build();
        when(adhesiveReportMapper.selectList(any())).thenReturn(List.of(first));
        Long originalId = service.getAdhesive1List(new HcAdhesiveProductionRecordPageReqVO()).get(0).getId();
        first.setReportStatus("SUBMITTED");
        next.setReportStatus("SUBMITTED");
        first.setConfirmerTime(next.getReportDate().atTime(18, 0));
        when(adhesiveReportMapper.selectList(any())).thenReturn(List.of(first, next));
        List<HcAdhesiveProductionRecordRespVO> rows = service.getAdhesive1List(new HcAdhesiveProductionRecordPageReqVO());
        assertEquals(2, rows.size());
        HcAdhesiveProductionRecordRespVO yesterday = rows.stream().filter(r -> first.getReportDate().equals(r.getReportDate())).findFirst().orElseThrow();
        assertEquals(originalId, yesterday.getId());
        assertEquals("甲", yesterday.getRecorderName());
        assertEquals(0, new BigDecimal("60").compareTo(yesterday.getOutputQty()));
        assertEquals(0, new BigDecimal("100").compareTo(rows.stream().map(HcAdhesiveProductionRecordRespVO::getOutputQty).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private HcAdhesive2ReportDO adhesive2Report(Long id, LocalDate reportDate,
                                                String pieceBatchNo, String glueBoardMaterialCode,
                                                String glueBoardBatchNo) {
        return HcAdhesive2ReportDO.builder()
                .id(id)
                .reportDate(reportDate)
                .modelCode("W33P0100")
                .materialCode("03.13.10053")
                .productionBatchNo(pieceBatchNo)
                .sourceProductionBatchNo(pieceBatchNo)
                .parentProductionBatchNo("W26G147AP")
                .sourceBatchNo("W26G147AP")
                .inputLength(BigDecimal.ONE)
                .outputLength(BigDecimal.ONE)
                .glueBoardUseLength(new BigDecimal("1.200"))
                .glueBoardMaterialCode(glueBoardMaterialCode)
                .glueBoardBatchNo(glueBoardBatchNo)
                .recorderName("测试员")
                .reportStatus("SUBMITTED")
                .build();
    }

    private HcToolingConsumableConsumeDO rndConsume(Long id, BigDecimal qty, BigDecimal inputQty,
                                                     BigDecimal outputQty, LocalDateTime consumeTime) {
        HcToolingConsumableConsumeDO consume = HcToolingConsumableConsumeDO.builder()
                .id(id)
                .ledgerId(41L)
                .consumableType("GLUE_BOARD")
                .processCode("ADHESIVE1")
                .batchNo("GB-RND-001")
                .productModelCode("W33P0100")
                .productMaterialCode("03.13.10053")
                .productBatchNo("RND-20260718-01")
                .productInputQty(inputQty)
                .productOutputQty(outputQty)
                .consumeQty(qty)
                .consumeTime(consumeTime)
                .build();
        consume.setConsumeType(qty.signum() < 0 ? "CORRECTION" : "NORMAL");
        consume.setCreator("研发员");
        return consume;
    }

}
