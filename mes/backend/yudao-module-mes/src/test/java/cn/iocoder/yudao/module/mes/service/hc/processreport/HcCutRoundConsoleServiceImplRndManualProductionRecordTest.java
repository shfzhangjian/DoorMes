package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcCutRoundProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.cutround.HcCutRoundSpareRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround.HcCutRoundReportDO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HcCutRoundConsoleServiceImplRndManualProductionRecordTest {

    @Mock
    private cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService productionRecordRevisionService;

    @org.junit.jupiter.api.BeforeEach
    void prepareProjection() {
        org.mockito.Mockito.lenient().doReturn(java.util.Map.of("MODEL", "WHITE_PAD"))
                .when(productionRecordPadTypeResolver).resolveByModelCodes(any());
        when(productionRecordRevisionService.applyRevisions(any(), any())).thenAnswer(call -> call.getArgument(1));
    }

    @org.mockito.Spy
    private HcProductionRecordPadTypeResolver productionRecordPadTypeResolver = new HcProductionRecordPadTypeResolver();

    @InjectMocks
    private HcCutRoundConsoleServiceImpl service;

    @Mock
    private HcCutRoundReportMapper hcCutRoundReportMapper;
    @Mock
    private HcCutRoundSpareRecordMapper hcCutRoundSpareRecordMapper;

    @Test
    void shouldProjectOneProductionRecordForBladeAndFeltInSameManualGroup() {
        LocalDateTime consumeTime = LocalDateTime.of(2026, 7, 18, 10, 0);
        when(hcCutRoundReportMapper.selectProductionRecordList(any())).thenReturn(List.of());
        when(hcCutRoundSpareRecordMapper.selectList(any())).thenReturn(List.of(
                manualRecord(1L, "CUTTING_BLADE", consumeTime, 110, null),
                manualRecord(2L, "CUTTING_FELT", consumeTime, 210, 7)));

        List<HcCutRoundProductionRecordRespVO> rows = service.getProductionRecordList(
                new HcCutRoundProductionRecordPageReqVO());

        assertEquals(1, rows.size());
        HcCutRoundProductionRecordRespVO row = rows.get(0);
        org.junit.jupiter.api.Assertions.assertNull(row.getPadType());
        assertEquals("RND-MODEL", row.getModelCode());
        assertEquals("RND-BATCH-001", row.getProductionBatchNo());
        assertEquals("775", row.getCutSizeMm());
        assertEquals(0, BigDecimal.TEN.compareTo(row.getInputQty()));
        assertEquals(0, BigDecimal.valueOf(9).compareTo(row.getOutputQty()));
        assertEquals(110, row.getBladeUseCount());
        assertEquals(210, row.getFeltUseCount());
        assertEquals(7, row.getFeltUseDays());
        assertEquals("研发手工备件登记", row.getRecordSource());
        assertEquals("研发手工备件登记：研发裁切消耗", row.getRemark());
    }

    @Test
    void shouldSplitConfirmedProductionByBusinessDateAndSize() {
        HcCutRoundReportDO first = report(1L, LocalDate.of(2026, 9, 24), "775", 60);
        HcCutRoundReportDO second = report(2L, first.getReportDate().plusDays(1), "775", 40);
        HcCutRoundReportDO otherSize = report(3L, second.getReportDate(), "740", 10);
        when(hcCutRoundReportMapper.selectProductionRecordList(any())).thenReturn(List.of(first, second, otherSize));
        List<HcCutRoundProductionRecordRespVO> rows = service.getProductionRecordList(new HcCutRoundProductionRecordPageReqVO());
        assertEquals(3, rows.size());
        assertEquals(0, new BigDecimal("110").compareTo(rows.stream().map(HcCutRoundProductionRecordRespVO::getOutputQty).reduce(BigDecimal.ZERO, BigDecimal::add)));
        HcCutRoundProductionRecordRespVO day1 = rows.stream().filter(r -> first.getReportDate().equals(r.getReportDate())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("60").compareTo(day1.getOutputQty()));
        assertEquals(first.getReportDate(), day1.getReportDate());
        assertEquals("WHITE_PAD", day1.getPadType());
        assertEquals(3, rows.stream().map(HcCutRoundProductionRecordRespVO::getId).distinct().count());
    }

    @Test
    void shouldNotMergeManualRecordsAcrossDaysEvenWhenGroupNumberIsReused() {
        when(hcCutRoundSpareRecordMapper.selectList(any())).thenReturn(List.of(
                manualRecord(1L, "CUTTING_BLADE", LocalDateTime.of(2026, 9, 24, 18, 0), 10, null),
                manualRecord(2L, "CUTTING_BLADE", LocalDateTime.of(2026, 9, 25, 9, 0), 20, null)));
        List<HcCutRoundProductionRecordRespVO> rows = service.getProductionRecordList(new HcCutRoundProductionRecordPageReqVO());
        assertEquals(2, rows.size());
        assertEquals(0, new BigDecimal("18").compareTo(rows.stream().map(HcCutRoundProductionRecordRespVO::getOutputQty).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    @Test
    void shouldKeepExplicitRndPadTypesSeparateAndCountEachGroupOnce() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 24, 10, 0);
        HcCutRoundSpareRecordDO blackBlade = manualRecord(1L, "CUTTING_BLADE", time, 10, null);
        HcCutRoundSpareRecordDO blackFelt = manualRecord(2L, "CUTTING_FELT", time, 10, 1);
        HcCutRoundSpareRecordDO whiteBlade = manualRecord(3L, "CUTTING_BLADE", time, 20, null);
        HcCutRoundSpareRecordDO whiteFelt = manualRecord(4L, "CUTTING_FELT", time, 20, 1);
        blackBlade.setPadType("BLACK_PAD");
        blackFelt.setPadType("BLACK_PAD");
        whiteBlade.setPadType("WHITE_PAD");
        whiteFelt.setPadType("WHITE_PAD");
        whiteBlade.setRecordGroupNo("RND-002");
        whiteFelt.setRecordGroupNo("RND-002");
        when(hcCutRoundSpareRecordMapper.selectList(any())).thenReturn(List.of(blackBlade, blackFelt, whiteBlade, whiteFelt));
        // 即使型号主数据是白垫，研发明确选择的黑垫也必须保留。
        org.mockito.Mockito.doReturn(java.util.Map.of("RND-MODEL", "WHITE_PAD"))
                .when(productionRecordPadTypeResolver).resolveByModelCodes(any());
        List<HcCutRoundProductionRecordRespVO> rows = service.getProductionRecordList(new HcCutRoundProductionRecordPageReqVO());
        assertEquals(2, rows.size());
        assertEquals(2, rows.stream().map(HcCutRoundProductionRecordRespVO::getId).distinct().count());
        assertEquals(java.util.Set.of("BLACK_PAD", "WHITE_PAD"), rows.stream().map(HcCutRoundProductionRecordRespVO::getPadType).collect(java.util.stream.Collectors.toSet()));
        assertEquals(0, new BigDecimal("18").compareTo(rows.stream().map(HcCutRoundProductionRecordRespVO::getOutputQty).reduce(BigDecimal.ZERO, BigDecimal::add)));
        HcCutRoundProductionRecordPageReqVO filter = new HcCutRoundProductionRecordPageReqVO();
        filter.setPadType("BLACK_PAD");
        assertEquals("BLACK_PAD", service.getProductionRecordList(filter).get(0).getPadType());
        assertEquals(1, service.getProductionRecordList(filter).size());
    }

    @Test
    void shouldFilterBeforePaginationAndKeepUnknownRecordsUnclassified() {
        when(hcCutRoundReportMapper.selectProductionRecordList(any())).thenReturn(List.of(
                report(1L, LocalDate.of(2026, 9, 24), "775", 8),
                report(2L, LocalDate.of(2026, 9, 25), "775", 9)));
        when(hcCutRoundSpareRecordMapper.selectList(any())).thenReturn(List.of(
                manualRecord(3L, "CUTTING_BLADE", LocalDateTime.of(2026, 9, 24, 10, 0), 10, null)));
        HcCutRoundProductionRecordPageReqVO filter = new HcCutRoundProductionRecordPageReqVO();
        filter.setPadType("WHITE_PAD");
        filter.setPageSize(1);
        filter.setPageNo(2);
        var page = service.getProductionRecordPage(filter);
        assertEquals(2L, page.getTotal());
        assertEquals(1, page.getList().size());
        assertEquals("WHITE_PAD", page.getList().get(0).getPadType());
        assertEquals(2, service.getProductionRecordList(filter).size());
        filter.setPadType("UNCLASSIFIED");
        assertEquals(1, service.getProductionRecordList(filter).size());
        org.junit.jupiter.api.Assertions.assertNull(service.getProductionRecordList(filter).get(0).getPadType());
        filter.setPadType("BLACK_PAD");
        assertEquals(0, service.getProductionRecordPage(filter).getTotal());
    }

    @Test
    void shouldResolveLegacyRndOnlyFromKnownModel() {
        HcCutRoundSpareRecordDO record = manualRecord(1L, "CUTTING_BLADE", LocalDateTime.of(2026, 9, 24, 10, 0), 10, null);
        record.setModelCode("MODEL");
        when(hcCutRoundSpareRecordMapper.selectList(any())).thenReturn(List.of(record));
        assertEquals("WHITE_PAD", service.getProductionRecordList(new HcCutRoundProductionRecordPageReqVO()).get(0).getPadType());
    }

    @Test
    void shouldRejectInvalidPadFilter() {
        HcCutRoundProductionRecordPageReqVO filter = new HcCutRoundProductionRecordPageReqVO();
        filter.setPadType("COMMON");
        // 此请求在查询与修订之前拒绝。
        org.mockito.Mockito.reset(productionRecordRevisionService);
        org.junit.jupiter.api.Assertions.assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,
                () -> service.getProductionRecordList(filter));
        org.mockito.Mockito.verifyNoInteractions(hcCutRoundReportMapper, hcCutRoundSpareRecordMapper);
    }

    private HcCutRoundReportDO report(Long id, LocalDate date, String size, int qty) {
        return HcCutRoundReportDO.builder().id(id).reportDate(date).modelCode("MODEL")
                .parentProductionBatchNo("W26G147AP").reportStatus("CONFIRMED")
                .inputLength(BigDecimal.valueOf(qty)).outputLength(BigDecimal.valueOf(qty))
                .extraJson("{\"actualSizeRule\":\"" + size + "mm\"}")
                .endTime(date.plusDays(1).atTime(0, 5)).build();
    }

    private HcCutRoundSpareRecordDO manualRecord(Long id, String spareType, LocalDateTime eventTime,
                                                  int afterUseCount, Integer feltUseDays) {
        return HcCutRoundSpareRecordDO.builder()
                .id(id)
                .spareType(spareType)
                .eventType("RND_MANUAL_USE")
                .recordSource("RND_MANUAL")
                .recordGroupNo("RND-001")
                .modelCode("RND-MODEL")
                .productionBatchNo("RND-BATCH-001")
                .cutSizeMm("775")
                .cutInputPcs(BigDecimal.TEN)
                .cutOutputPcs(BigDecimal.valueOf(9))
                .afterUseCount(afterUseCount)
                .feltUseDays(feltUseDays)
                .eventTime(eventTime)
                .operatorName("研发员")
                .remark("研发裁切消耗")
                .build();
    }
}
