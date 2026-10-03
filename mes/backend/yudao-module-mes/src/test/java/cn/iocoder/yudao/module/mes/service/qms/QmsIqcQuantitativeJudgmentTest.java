package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcStandardRespVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsQualityStandardItemDO;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QmsIqcQuantitativeJudgmentTest {
    private final QmsIqcServiceImpl service = new QmsIqcServiceImpl();

    @Test
    void realNineSamplesFailAverageEvenWithoutSingleLimits() {
        QmsIqcItemDO item = item(null, null, "537", "1113", 9);
        evaluate(item, samples("1053", "1192", "1258", "1211", "1380", "1183", "1577", "1562", "1467"));
        assertEquals("NG", item.getItemResult());
        assertEquals(new BigDecimal("1320.333333"), item.getAverageValue());
        assertTrue(item.getJudgmentReason().contains("超过平均值内控上限 1113"));
    }

    @Test
    void averageFailsAlthoughEverySingleSamplePasses() {
        QmsIqcItemDO item = item("465", "1185", "537", "1113", 2);
        evaluate(item, samples("1114", "1114"));
        assertEquals("NG", item.getItemResult());
        assertTrue(item.getJudgmentReason().startsWith("平均值"));
    }

    @Test
    void singleFailureCannotBeHiddenByPassingAverage() {
        QmsIqcItemDO item = item("465", "1185", "537", "1113", 2);
        evaluate(item, samples("1200", "800"));
        assertEquals("NG", item.getItemResult());
        assertTrue(item.getJudgmentReason().contains("样本1"));
    }

    @Test
    void inclusiveLimitsAndCoaValuesPass() {
        QmsIqcItemDO item = item("465", "1185", "825", "825", 2);
        evaluate(item, samples("465", "1185"));
        assertEquals("OK", item.getItemResult());
        item = item("465", "1185", "537", "1113", 3);
        evaluate(item, samples("731", "747", "793"));
        assertEquals("OK", item.getItemResult());
    }

    @Test
    void oneSidedLimitsWork() {
        QmsIqcItemDO item = item(null, null, "537", null, 1);
        evaluate(item, samples("536"));
        assertEquals("NG", item.getItemResult());
        item = item(null, "1185", null, null, 1);
        evaluate(item, samples("1185"));
        assertEquals("OK", item.getItemResult());
    }

    @Test
    void roundedAverageMustNotHideExceedance() {
        QmsIqcItemDO item = item(null, null, null, "1", 3);
        evaluate(item, samples("1", "1", "1.000001"));
        assertEquals(new BigDecimal("1.000000"), item.getAverageValue());
        assertEquals("NG", item.getItemResult());
    }

    @Test
    void missingRulesSkipInsteadOfTrustingClientOk() {
        QmsIqcItemDO item = item(null, null, null, null, 1);
        item.setStandardDesc("465-1185");
        List<QmsIqcSaveReqVO.IqcSample> samples = samples("1577");
        samples.get(0).setSampleResult("OK");
        evaluate(item, samples);
        assertEquals("SKIP", item.getItemResult());
        assertTrue(item.getJudgmentReason().contains("不参与合格判定"));
        assertEquals("SKIP", samples.get(0).getSampleResult());
    }

    @Test
    void missingOrDuplicateSamplesRemainPending() {
        QmsIqcItemDO item = item("0", "10", null, null, 2);
        evaluate(item, samples("1"));
        assertEquals("PENDING", item.getItemResult());
        List<QmsIqcSaveReqVO.IqcSample> samples = samples("1", "2");
        samples.get(1).setSampleSeq(1);
        evaluate(item, samples);
        assertEquals("PENDING", item.getItemResult());
        evaluate(item, List.of());
        assertEquals("PENDING", item.getItemResult());
    }

    @Test
    void nullValuesAndReversedLimitsRemainPending() {
        QmsIqcItemDO item = item("0", "10", null, null, 1);
        QmsIqcSaveReqVO.IqcSample sample = new QmsIqcSaveReqVO.IqcSample();
        sample.setSampleSeq(1);
        evaluate(item, List.of(sample));
        assertEquals("PENDING", item.getItemResult());
        item.setAvgMinLimit(new BigDecimal("10"));
        item.setAvgMaxLimit(BigDecimal.ONE);
        evaluate(item, samples("2"));
        assertEquals("PENDING", item.getItemResult());
        assertTrue(item.getJudgmentReason().contains("下限大于上限"));
    }

    @Test
    void correctedValuesClearPreviousAutomaticNgAndReason() {
        QmsIqcItemDO item = item("0", "10", null, "8", 1);
        List<QmsIqcSaveReqVO.IqcSample> samples = samples("11");
        evaluate(item, samples);
        assertEquals("NG", samples.get(0).getSampleResult());
        samples.get(0).setResultValue(new BigDecimal("7"));
        evaluate(item, samples);
        assertEquals("OK", item.getItemResult());
        assertEquals("OK", samples.get(0).getSampleResult());
        assertEquals("", item.getJudgmentReason());
    }

    @Test
    void standardResponseIncludesAverageLimits() {
        QmsQualityStandardItemDO source = new QmsQualityStandardItemDO();
        source.setAvgMinLimit(new BigDecimal("537"));
        source.setAvgMaxLimit(new BigDecimal("1113"));
        QmsIqcStandardRespVO.StandardItem response = ReflectionTestUtils.invokeMethod(service, "buildStandardItem", source);
        assertNotNull(response);
        assertEquals(source.getAvgMinLimit(), response.getAvgMinLimit());
        assertEquals(source.getAvgMaxLimit(), response.getAvgMaxLimit());
    }

    @Test
    void pendingItemCannotBeHiddenBehindAnotherNgOnSubmit() {
        QmsIqcItemDO ng = item(null, null, null, "1", 1);
        evaluate(ng, samples("2"));
        QmsIqcItemDO pending = item("0", "10", null, null, 2);
        evaluate(pending, samples("2"));
        assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "calculateFinalJudgment", List.of(ng, pending)));
    }

    @Test
    void real002SamplesSkipAndConfiguredAverageStillJudges() {
        QmsIqcItemDO item = item(null, null, null, null, 9);
        var values = samples("1494", "1231", "997", "990", "1273", "1062", "1279", "1279", "1144");
        evaluate(item, values);
        assertEquals("SKIP", item.getItemResult());
        assertEquals(new BigDecimal("1194.333333"), item.getAverageValue());
        assertTrue(values.stream().allMatch(sample -> "SKIP".equals(sample.getSampleResult())));
        item.setAvgMinLimit(new BigDecimal("863"));
        item.setAvgMaxLimit(new BigDecimal("1967"));
        evaluate(item, values);
        assertEquals("OK", item.getItemResult());
        assertEquals("", item.getJudgmentReason());
        assertTrue(values.stream().allMatch(sample -> "SKIP".equals(sample.getSampleResult())));
    }

    @Test
    void skippedItemDoesNotBlockOrHideOtherResults() {
        QmsIqcItemDO skipped = item(null, null, null, null, 1);
        evaluate(skipped, samples("2"));
        QmsIqcItemDO judged = item("0", "10", null, null, 1);
        evaluate(judged, samples("11"));
        assertEquals("NG", ReflectionTestUtils.invokeMethod(service, "calculateFinalJudgment", List.of(skipped, judged)));
        evaluate(judged, samples("10"));
        assertEquals("OK", ReflectionTestUtils.invokeMethod(service, "calculateFinalJudgment", List.of(skipped, judged)));
        assertEquals("SKIP", ReflectionTestUtils.invokeMethod(service, "calculateFinalJudgment", List.of(skipped)));
        var mapper = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper.class);
        ReflectionTestUtils.setField(service, "qmsIqcItemMapper", mapper);
        org.mockito.Mockito.when(mapper.selectListByIqcId(1L)).thenReturn(List.of(skipped));
        assertEquals("SKIP", ReflectionTestUtils.invokeMethod(service, "calculateCurrentJudgment", 1L));
    }

    @Test
    void missingStandardDoesNotExemptRequiredData() {
        QmsIqcItemDO item = item(null, null, null, null, 2);
        evaluate(item, samples("1"));
        assertEquals("PENDING", item.getItemResult());
        assertThrows(RuntimeException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "calculateFinalJudgment", List.of(item)));
    }

    @Test
    void createdSnapshotRetainsAverageLimits() {
        var mapper = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper.class);
        ReflectionTestUtils.setField(service, "qmsIqcItemMapper", mapper);
        var standard = QmsQualityStandardItemDO.builder().id(7241L).inspectionItem("180°剥离力")
                .itemType("QUANTITATIVE").avgMinLimit(new BigDecimal("537"))
                .avgMaxLimit(new BigDecimal("1113")).sampleSize(9).build();
        var order = cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO.builder().id(1L).build();
        ReflectionTestUtils.invokeMethod(service, "saveIqcStandardSnapshotDetails", order, List.of(standard), null);
        var capture = org.mockito.ArgumentCaptor.forClass(QmsIqcItemDO.class);
        org.mockito.Mockito.verify(mapper).insert(capture.capture());
        assertEquals(standard.getAvgMinLimit(), capture.getValue().getAvgMinLimit());
        assertEquals(standard.getAvgMaxLimit(), capture.getValue().getAvgMaxLimit());
    }

    @Test
    void persistedSamplesReplaceStaleOkBeforeFinalJudgment() {
        var itemMapper = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcItemMapper.class);
        var sampleMapper = org.mockito.Mockito.mock(cn.iocoder.yudao.module.mes.dal.mysql.qms.QmsIqcSampleMapper.class);
        ReflectionTestUtils.setField(service, "qmsIqcItemMapper", itemMapper);
        ReflectionTestUtils.setField(service, "qmsIqcSampleMapper", sampleMapper);
        QmsIqcItemDO item = item(null, null, "537", "1113", 1);
        item.setId(10L);
        item.setItemResult("OK");
        var sample = cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcSampleDO.builder()
                .id(20L).iqcItemId(10L).sampleSeq(1).resultValue(new BigDecimal("1320")).build();
        org.mockito.Mockito.when(sampleMapper.selectListByIqcId(1L)).thenReturn(List.of(sample));
        ReflectionTestUtils.invokeMethod(service, "recalculateStoredItems", 1L, List.of(item));
        assertEquals("NG", item.getItemResult());
        org.mockito.Mockito.verify(itemMapper).updateById(item);
        assertEquals("NG", ReflectionTestUtils.invokeMethod(service, "calculateFinalJudgment", List.of(item)));
        item.setAvgMinLimit(null);
        item.setAvgMaxLimit(null);
        ReflectionTestUtils.invokeMethod(service, "recalculateStoredItems", 1L, List.of(item));
        assertEquals("SKIP", item.getItemResult());
        assertEquals("SKIP", ReflectionTestUtils.invokeMethod(service, "calculateFinalJudgment", List.of(item)));
        assertEquals("不判定", ReflectionTestUtils.invokeMethod(service, "resultLabel", "SKIP"));
        assertEquals("不判定", ReflectionTestUtils.invokeMethod(service, "coaJudgmentLabel", "SKIP"));
    }

    @Test
    void dynamicExportContainsAverageLimitsAndFailureReason() throws Exception {
        QmsIqcItemDO item = item(null, null, "537", "1113", 1);
        item.setId(10L);
        evaluate(item, samples("1320"));
        var order = cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO.builder()
                .id(1L).iqcNo("IQC-TEST").build();
        byte[] bytes = new QmsIqcItemWorkbookService().buildWorkbook(order, List.of(item), java.util.Map.of());
        try (var workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            var formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            var values = new java.util.HashMap<String, String>();
            for (var cell : sheet.getRow(4)) {
                values.put(formatter.formatCellValue(cell), formatter.formatCellValue(sheet.getRow(5).getCell(cell.getColumnIndex())));
            }
            assertEquals("537 ～ 1113", values.get("平均值内控"));
            assertEquals("NG", values.get("项目判定"));
            assertTrue(values.get("判定原因").contains("超过平均值内控上限 1113"));
        }
    }

    private void evaluate(QmsIqcItemDO item, List<QmsIqcSaveReqVO.IqcSample> samples) {
        ReflectionTestUtils.invokeMethod(service, "applySampleStats", item, samples);
    }

    private QmsIqcItemDO item(String min, String max, String avgMin, String avgMax, int size) {
        return QmsIqcItemDO.builder().inspectionItem("180°剥离力").itemType("QUANTITATIVE")
                .minValueLimit(decimal(min)).maxValueLimit(decimal(max))
                .avgMinLimit(decimal(avgMin)).avgMaxLimit(decimal(avgMax)).sampleSize(size).unit("gf/25mm").build();
    }

    private BigDecimal decimal(String value) { return value == null ? null : new BigDecimal(value); }

    private List<QmsIqcSaveReqVO.IqcSample> samples(String... values) {
        List<QmsIqcSaveReqVO.IqcSample> samples = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            QmsIqcSaveReqVO.IqcSample sample = new QmsIqcSaveReqVO.IqcSample();
            sample.setSampleSeq(i + 1);
            sample.setResultValue(new BigDecimal(values[i]));
            samples.add(sample);
        }
        return samples;
    }
}
