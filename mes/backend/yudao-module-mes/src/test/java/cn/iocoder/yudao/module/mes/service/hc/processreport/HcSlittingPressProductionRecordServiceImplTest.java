package cn.iocoder.yudao.module.mes.service.hc.processreport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordPageReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcSlittingPressProductionRecordRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive.HcAdhesiveReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotReportDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.pressslot.HcPressSlotSpareRecordDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting.HcSlittingSliceRecordDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderOperationMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.planorder.HcPlanOrderMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.adhesive.HcAdhesiveReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotReportMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.pressslot.HcPressSlotSpareRecordMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.processreport.slitting.HcSlittingSliceRecordMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HcSlittingPressProductionRecordServiceImplTest {

    @Mock
    private HcProductionRecordPadTypeResolver padTypeResolver;

    @Mock
    private cn.iocoder.yudao.module.mes.service.hc.productionrecordrevision.HcProductionRecordRevisionService productionRecordRevisionService;

    @org.junit.jupiter.api.BeforeEach
    void preparePadTypeResolver() {
        when(recorderNameResolver.loadNames()).thenReturn(
                new HcProductionRecorderNameResolver.Names(java.util.Map.of("heliang", "何亮")));
        when(productionRecordRevisionService.applyRevisions(any(), any())).thenAnswer(call -> call.getArgument(1));
        when(padTypeResolver.resolveByModelCodes(any())).thenReturn(new java.util.HashMap<>());
        org.mockito.Mockito.lenient().when(padTypeResolver.matchesFilter(any(), any())).thenReturn(true);
    }

    @Mock
    private HcProductionRecorderNameResolver recorderNameResolver;

    @InjectMocks
    private HcSlittingPressProductionRecordServiceImpl service;

    @Mock
    private HcPressSlotReportMapper hcPressSlotReportMapper;
    @Mock
    private HcPlanOrderMapper hcPlanOrderMapper;
    @Mock
    private HcAdhesiveReportMapper hcAdhesiveReportMapper;
    @Mock
    private HcSlittingSliceRecordMapper hcSlittingSliceRecordMapper;
    @Mock
    private HcPlanOrderOperationMapper hcPlanOrderOperationMapper;
    @Mock
    private HcPressSlotSpareRecordMapper hcPressSlotSpareRecordMapper;

    @Test
    void shouldNormalizeNamesBeforeFilteringWithoutLosingQuantities() {
        HcPressSlotReportDO first = pressSlotReport(1L, "W26G147AP001");
        HcPressSlotReportDO second = pressSlotReport(2L, "W26G147AP002");
        first.setRecorderName("heliang");
        second.setRecorderName("何亮");
        when(hcPressSlotReportMapper.selectList(any())).thenReturn(List.of(first, second));
        when(hcPlanOrderOperationMapper.selectList(any())).thenReturn(List.of());
        for (String query : List.of("何亮", "heliang", "hel")) {
            HcSlittingPressProductionRecordPageReqVO req = new HcSlittingPressProductionRecordPageReqVO();
            req.setRecorderName(query);
            List<HcSlittingPressProductionRecordRespVO> rows = service.getList(req);
            assertEquals(1, rows.size());
            assertEquals("何亮", rows.get(0).getRecorderName());
            assertEquals(0, BigDecimal.valueOf(2).compareTo(rows.get(0).getPressSlotActualInputPcs()));
        }
        assertEquals("heliang", first.getRecorderName());
    }

    @Test
    void shouldShowConfirmedSegmentsWithoutCompletionMarker() {
        when(hcPressSlotReportMapper.selectList(any())).thenReturn(List.of(
                pressSlotReport(1L, "W26G147AP001"),
                pressSlotReport(2L, "W26G147AQ001")));
        when(hcPlanOrderOperationMapper.selectList(any())).thenReturn(List.of());

        List<HcSlittingPressProductionRecordRespVO> rows = service.getList(
                new HcSlittingPressProductionRecordPageReqVO());

        assertEquals(2, rows.size());
        assertEquals("W26G147AP", rows.get(0).getBatchNo());
        assertEquals(0, BigDecimal.ONE.compareTo(rows.get(0).getPressSlotActualInputPcs()));
        assertEquals(0, BigDecimal.ONE.compareTo(rows.get(0).getPressSlotOutputPcs()));
    }

    @Test
    void shouldShowSpareUseDaysForRndManualRecord() {
        LocalDateTime consumeTime = LocalDateTime.of(2026, 7, 18, 10, 0);
        HcPressSlotSpareRecordDO rollerManual = rndManualRecord(
                1L, "PRESS_ROLLER", "ROLLER-01", consumeTime, 100, 90);
        HcPressSlotSpareRecordDO bearingManual = rndManualRecord(
                2L, "BEARING", "BEARING-01", consumeTime, 100, 90);
        when(hcPressSlotReportMapper.selectList(any())).thenReturn(List.of());
        when(hcPressSlotSpareRecordMapper.selectList(any())).thenReturn(
                List.of(rollerManual, bearingManual),
                List.of(
                        spareBaseRecord(3L, "PRESS_ROLLER", "ROLLER-01", LocalDateTime.of(2026, 7, 11, 8, 0)),
                        spareBaseRecord(4L, "BEARING", "BEARING-01", LocalDateTime.of(2026, 7, 11, 8, 0)),
                        rollerManual,
                        bearingManual));

        List<HcSlittingPressProductionRecordRespVO> rows = service.getList(
                new HcSlittingPressProductionRecordPageReqVO());

        assertEquals(1, rows.size());
        assertEquals(7, rows.get(0).getRollerCleanUseDays());
        assertEquals(7, rows.get(0).getBearingReplaceUseDays());
    }

    @Test
    void shouldPreferActualMaintenanceOverLaterConfigForSpareUseDays() {
        LocalDateTime consumeTime = LocalDateTime.of(2026, 7, 29, 15, 30, 53);
        HcPressSlotSpareRecordDO rollerManual = rndManualRecord(
                1L, "PRESS_ROLLER", "ROLLER-01", consumeTime, 281, 280);
        HcPressSlotSpareRecordDO bearingManual = rndManualRecord(
                2L, "BEARING", "BEARING-01", consumeTime, 791, 790);
        when(hcPressSlotReportMapper.selectList(any())).thenReturn(List.of());
        when(hcPressSlotSpareRecordMapper.selectList(any())).thenReturn(
                List.of(rollerManual, bearingManual),
                List.of(
                        spareEventRecord(3L, "PRESS_ROLLER", "ROLLER-01", "CLEAN_RESET",
                                LocalDateTime.of(2026, 7, 6, 9, 0)),
                        spareEventRecord(4L, "BEARING", "BEARING-01", "REPLACE",
                                LocalDateTime.of(2026, 6, 23, 9, 0)),
                        spareBaseRecord(5L, "PRESS_ROLLER", "ROLLER-01", LocalDateTime.of(2026, 7, 8, 9, 0)),
                        spareBaseRecord(6L, "BEARING", "BEARING-01", LocalDateTime.of(2026, 7, 8, 9, 0)),
                        rollerManual,
                        bearingManual));

        List<HcSlittingPressProductionRecordRespVO> rows = service.getList(
                new HcSlittingPressProductionRecordPageReqVO());

        assertEquals(1, rows.size());
        assertEquals(23, rows.get(0).getRollerCleanUseDays());
        assertEquals(36, rows.get(0).getBearingReplaceUseDays());
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"WAIT_NG_SHELF", "NG_STORED"})
    void shouldSeparateSlittingNgAndActualPressSlotReportQuantities(String ngStatus) {
        HcSlittingSliceRecordDO okSlice = slittingSlice(1L, "W26G028AP001A", "OK", "POSTED");
        HcSlittingSliceRecordDO ngSlice = slittingSlice(2L, "W26G028AP002A", "OK", ngStatus);
        HcPressSlotReportDO okReport = pressSlotReport(1L, "W26G028AP001A");
        okReport.setParentProductionBatchNo("W26G028AP");
        okReport.setSourceSlittingSliceId(1L);
        okReport.setOutputStockPostStatus("POSTED");
        HcPressSlotReportDO ngReport = pressSlotReport(2L, "W26G028AP002A");
        ngReport.setParentProductionBatchNo("W26G028AP");
        ngReport.setSourceSlittingSliceId(2L);
        ngReport.setSelfCheck("OK");
        ngReport.setOutputStockPostStatus(ngStatus);

        when(hcPressSlotReportMapper.selectList(any())).thenReturn(List.of(okReport, ngReport));
        when(hcSlittingSliceRecordMapper.selectProductionRecordSlices(any())).thenReturn(List.of(okSlice, ngSlice));
        when(hcAdhesiveReportMapper.selectBatchIds(any())).thenReturn(List.of(HcAdhesiveReportDO.builder()
                .id(87L).modelCode("W33P0100").materialCode("03.13.10053").build()));
        when(hcSlittingSliceRecordMapper.selectList(any())).thenReturn(
                List.of(okSlice, ngSlice), List.of(okSlice, ngSlice));
        when(hcPlanOrderOperationMapper.selectList(any())).thenReturn(List.of());

        HcSlittingPressProductionRecordRespVO row = service.getList(
                new HcSlittingPressProductionRecordPageReqVO()).get(0);

        assertEquals(2, row.getSlittingOutputPcs());
        assertEquals(1, row.getSlittingNgPcs());
        assertEquals(0, BigDecimal.valueOf(2).compareTo(row.getPressSlotActualInputPcs()));
        assertEquals(0, BigDecimal.valueOf(2).compareTo(row.getPressSlotActualOutputPcs()));
        assertEquals(0, BigDecimal.ONE.compareTo(row.getPressSlotOutputPcs()));
    }

    @Test
    void shouldCountSlittingOnItsOwnDayAndPressingOnFollowingDay() {
        HcSlittingSliceRecordDO slice = slittingSlice(1L, "W26G028AP001A", "OK", "POSTED");
        HcPressSlotReportDO press = pressSlotReport(1L, "W26G028AP001A");
        press.setReportDate(LocalDate.of(2026, 7, 18));
        press.setSourceSlittingSliceId(1L);
        when(hcPressSlotReportMapper.selectList(any())).thenReturn(List.of(press));
        when(hcSlittingSliceRecordMapper.selectList(any())).thenReturn(List.of(slice));
        when(hcSlittingSliceRecordMapper.selectProductionRecordSlices(any())).thenReturn(List.of(slice));
        when(hcAdhesiveReportMapper.selectBatchIds(any())).thenReturn(List.of(HcAdhesiveReportDO.builder()
                .id(87L).modelCode("W33P0100").materialCode("03.13.10053").build()));
        List<HcSlittingPressProductionRecordRespVO> rows = service.getList(new HcSlittingPressProductionRecordPageReqVO());
        assertEquals(2, rows.size());
        HcSlittingPressProductionRecordRespVO first = rows.stream().filter(r -> slice.getScanTime().toLocalDate().equals(r.getReportDate())).findFirst().orElseThrow();
        HcSlittingPressProductionRecordRespVO second = rows.stream().filter(r -> press.getReportDate().equals(r.getReportDate())).findFirst().orElseThrow();
        assertEquals(1, first.getSlittingOutputPcs());
        assertEquals(0, new BigDecimal("0.8").compareTo(first.getSlittingInputM()));
        assertEquals(0, first.getPressSlotActualInputPcs().signum());
        assertEquals(0, second.getSlittingOutputPcs());
        assertEquals(0, second.getSlittingInputM().signum());
        assertEquals(0, BigDecimal.ONE.compareTo(second.getPressSlotActualInputPcs()));
    }

    @Test
    void shouldShowSlittingOnlyAndNeverAllocateWholeRollToUnknownDailyInput() {
        HcSlittingSliceRecordDO slice = slittingSlice(1L, "W26G028AP001A", "OK", "POSTED");
        slice.setSliceLength(null);
        when(hcSlittingSliceRecordMapper.selectProductionRecordSlices(any())).thenReturn(List.of(slice, slice));
        when(hcAdhesiveReportMapper.selectBatchIds(any())).thenReturn(List.of());
        List<HcSlittingPressProductionRecordRespVO> rows = service.getList(new HcSlittingPressProductionRecordPageReqVO());
        assertEquals(1, rows.size());
        assertEquals(1, rows.get(0).getSlittingOutputPcs());
        org.junit.jupiter.api.Assertions.assertNull(rows.get(0).getSlittingInputM());
        org.junit.jupiter.api.Assertions.assertTrue(rows.get(0).getRemark().contains("日投入待核实"));
    }

    @Test
    void shouldUseOriginalConsumptionDateAndRespectDailyFilters() {
        HcSlittingSliceRecordDO slice = slittingSlice(1L, "W26G028AP001A", "OK", "POSTED");
        slice.setSourceConsumeTime(LocalDateTime.of(2026, 7, 16, 23, 50));
        slice.setSourceConsumeQty(new BigDecimal("0.75"));
        when(hcSlittingSliceRecordMapper.selectProductionRecordSlices(any())).thenReturn(List.of(slice));
        when(hcAdhesiveReportMapper.selectBatchIds(any())).thenReturn(List.of());
        HcSlittingPressProductionRecordPageReqVO req = new HcSlittingPressProductionRecordPageReqVO();
        req.setReportDateStart(LocalDate.of(2026, 7, 16));
        req.setReportDateEnd(LocalDate.of(2026, 7, 16));
        HcSlittingPressProductionRecordRespVO row = service.getList(req).get(0);
        assertEquals(LocalDate.of(2026, 7, 16), row.getReportDate());
        assertEquals(0, new BigDecimal("0.75").compareTo(row.getSlittingInputM()));
        req.setReportDateStart(LocalDate.of(2026, 7, 17));
        req.setReportDateEnd(LocalDate.of(2026, 7, 17));
        assertEquals(0, service.getList(req).size());
    }

    @Test
    void shouldSeparateRndPadsAndFilterBeforePaginationWithoutModelOverride() {
        LocalDateTime time = LocalDateTime.of(2026, 9, 24, 10, 0);
        HcPressSlotSpareRecordDO white = rndManualRecord(901L, "PRESS_ROLLER", "R1", time, 100, 90);
        white.setPadType("WHITE_PAD");
        HcPressSlotSpareRecordDO black = rndManualRecord(902L, "PRESS_ROLLER", "R2", time, 200, 180);
        black.setPadType("BLACK_PAD");
        black.setRecordGroupNo("RND-002");
        when(hcPressSlotSpareRecordMapper.selectList(any())).thenReturn(List.of(white, black), List.of());
        when(padTypeResolver.resolveByModelCodes(any())).thenReturn(java.util.Map.of("RND-MODEL", "BLACK_PAD"));
        when(padTypeResolver.matchesFilter(any(), any())).thenAnswer(call ->
                call.getArgument(0) == null || call.getArgument(0).equals(call.getArgument(1)));
        HcSlittingPressProductionRecordPageReqVO req = new HcSlittingPressProductionRecordPageReqVO();
        List<HcSlittingPressProductionRecordRespVO> rows = service.getList(req);
        assertEquals(2, rows.size());
        assertEquals(0, rows.stream().filter(r -> "WHITE_PAD".equals(r.getPadType())).findFirst().orElseThrow()
                .getPressSlotActualInputPcs().compareTo(BigDecimal.valueOf(100)));
        when(hcPressSlotSpareRecordMapper.selectList(any())).thenReturn(List.of(white, black), List.of());
        req.setPadType("WHITE_PAD"); req.setPageNo(1); req.setPageSize(10);
        var page = service.getPage(req);
        assertEquals(1L, page.getTotal());
        assertEquals("WHITE_PAD", page.getList().get(0).getPadType());
    }

    private HcPressSlotReportDO pressSlotReport(Long id, String pieceBatchNo) {
        return HcPressSlotReportDO.builder()
                .id(id)
                .planOperationId(100L)
                .reportDate(LocalDate.of(2026, 7, 17))
                .modelCode("W33P0100")
                .materialCode("03.13.10053")
                .sourceProductionBatchNo(pieceBatchNo)
                .productionBatchNo(pieceBatchNo)
                .inputLength(BigDecimal.ONE)
                .outputLength(BigDecimal.ONE)
                .reportStatus("CONFIRMED")
                .recorderName("测试员")
                .build();
    }

    private HcPressSlotSpareRecordDO rndManualRecord(Long id, String spareType, String batchNo,
                                                      LocalDateTime eventTime, int inputPcs, int outputPcs) {
        return HcPressSlotSpareRecordDO.builder()
                .id(id)
                .equipmentId(99L)
                .spareType(spareType)
                .eventType("RND_MANUAL_USE")
                .recordSource("RND_MANUAL")
                .recordGroupNo("RND-001")
                .modelCode("RND-MODEL")
                .productionBatchNo("RND-BATCH-001")
                .afterBatchNo(batchNo)
                .beforeBatchNo(batchNo)
                .afterUseCount(100)
                .pressSlotInputPcs(BigDecimal.valueOf(inputPcs))
                .pressSlotOutputPcs(BigDecimal.valueOf(outputPcs))
                .eventTime(eventTime)
                .operatorName("研发员")
                .remark("研发消耗")
                .build();
    }

    private HcPressSlotSpareRecordDO spareBaseRecord(Long id, String spareType, String batchNo,
                                                      LocalDateTime eventTime) {
        return spareEventRecord(id, spareType, batchNo, "CONFIG", eventTime);
    }

    private HcPressSlotSpareRecordDO spareEventRecord(Long id, String spareType, String batchNo,
                                                       String eventType, LocalDateTime eventTime) {
        return HcPressSlotSpareRecordDO.builder()
                .id(id)
                .equipmentId(99L)
                .spareType(spareType)
                .eventType(eventType)
                .afterBatchNo(batchNo)
                .eventTime(eventTime)
                .build();
    }

    private HcSlittingSliceRecordDO slittingSlice(Long id, String pieceBatchNo, String selfCheck,
                                                   String outputStockPostStatus) {
        return HcSlittingSliceRecordDO.builder()
                .id(id)
                .sourceAdhesiveReportId(87L)
                .sourceBatchNo("W26G028AP")
                .sourceProductionBatchNo("W26G028AP-J1")
                .sourceLength(BigDecimal.valueOf(44))
                .sliceLength(new BigDecimal("0.8"))
                .scanTime(LocalDateTime.of(2026, 7, 17, 9, 0))
                .scannerName("分切员")
                .sliceSerialNo(pieceBatchNo)
                .scanStatus("CONFIRMED")
                .selfCheck(selfCheck)
                .outputStockPostStatus(outputStockPostStatus)
                .build();
    }
}
