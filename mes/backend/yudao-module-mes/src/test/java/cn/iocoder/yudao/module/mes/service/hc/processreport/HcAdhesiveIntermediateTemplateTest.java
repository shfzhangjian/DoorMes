package cn.iocoder.yudao.module.mes.service.hc.processreport;

import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveIntermediateRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo.HcAdhesiveIntermediateDetailRespVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.stationform.vo.HcStationFormSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationform.HcStationFormDO;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormMapper;
import cn.iocoder.yudao.module.mes.dal.mysql.hc.stationform.HcStationFormItemMapper;
import cn.iocoder.yudao.module.mes.service.hc.stationform.HcStationFormServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HcAdhesiveIntermediateTemplateTest {
    @Test
    void excelRoundTripKeepsTitlesAndDoesNotImportFooterAsMeasurement() throws Exception {
        var excel = new cn.iocoder.yudao.module.mes.service.hc.processform.HcProcessFormExcelService();
        ReflectionTestUtils.setField(excel, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
        var layout = cn.iocoder.yudao.framework.common.util.json.JsonUtils.parseObject("""
                {"title":"W26粘胶1中间品","sheetName":"中间品","visualMode":"adhesive1-middle",
                 "headerItems":[{"label":"型号","value":"W26P0100","editable":true,"bindKey":"modelCode"}],
                 "columns":[{"title":"长度/m"},{"title":"左1.300±0.050mm"},{"title":"右1.300±0.050mm"},{"title":"备注"}],
                 "rows":[{"cells":[{"colIndex":0,"text":"1","editable":true,"bindKey":"0","bindField":"lengthMark"},
                    {"colIndex":1,"text":"1.301","editable":true,"bindKey":"0","bindField":"leftThickness"}]}],
                 "footerNotes":["填写要求：每米测量","版权声明：原表说明"],
                 "importStopPrefixes":["填写要求：","修订信息：","版权声明："]}
                """, cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormLayoutExcelReqVO.class);
        byte[] bytes = excel.buildLayoutWorkbook(layout);
        try (var workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(new java.io.ByteArrayInputStream(bytes))) {
            assertEquals("左1.300±0.050mm", workbook.getSheetAt(0).getRow(4).getCell(1).getStringCellValue());
        }
        var file = new org.springframework.mock.web.MockMultipartFile("file", "middle.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", bytes);
        var imported = excel.importLayout(file, cn.iocoder.yudao.framework.common.util.json.JsonUtils.toJsonString(layout));
        assertEquals(2, imported.getCellValues().size());
        assertEquals("1.301", imported.getCellValues().get(1).getValue());
        assertEquals("W26P0100", imported.getHeaderValues().get(0).getValue());
    }

    @Test
    void usesModelSpecificTitlesAndPreservesBusinessValuesAndSignatures() {
        HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();
        HcStationFormMapper forms = mock(HcStationFormMapper.class);
        HcStationFormItemMapper items = mock(HcStationFormItemMapper.class);
        ReflectionTestUtils.setField(service, "hcStationFormMapper", forms);
        ReflectionTestUtils.setField(service, "hcStationFormItemMapper", items);
        HcStationFormDO w26 = form(83L, "W26P0100", "W26左", "W26右");
        HcStationFormDO w33 = form(84L, "W33P0100", "W33左", "W33右");
        w26.setPresetHeaderDataJson("{\"batchNo\":\"模板批号\",\"processLength\":50,\"productWidthMm\":1200,\"widthStart\":999,\"recorder\":\"预设记录人\",\"confirmer\":\"预设确认人\",\"fillInstructions\":\"每米测量\"}");
        when(forms.selectEnabledByProcess("ADHESIVE")).thenReturn(List.of(w33, w26));
        when(items.selectByFormId(anyLong())).thenReturn(List.of());
        HcAdhesiveIntermediateRespVO record = new HcAdhesiveIntermediateRespVO();
        record.setId(123L);
        record.setModelCode("W26P0100"); // 指定型号只做精确匹配
        record.setBatchNo("实际批号");
        record.setProcessLength(new BigDecimal("80"));
        record.setWidthStart(BigDecimal.ZERO);
        record.setRecordStatus("CONFIRMED");
        ReflectionTestUtils.invokeMethod(service, "applyAdhesiveIntermediateTemplate", record);
        assertEquals(83L, record.getStationFormId());
        assertEquals(List.of("W26左", "W26右"), record.getThicknessLabels());
        assertEquals("实际批号", record.getBatchNo());
        assertEquals(new BigDecimal("80"), record.getProcessLength());
        assertEquals(BigDecimal.ZERO, record.getWidthStart());
        assertEquals(new BigDecimal("1200"), record.getProductWidthMm());
        assertEquals("每米测量", record.getFormNotes().get("fillInstructions"));
        assertEquals("CONFIRMED", record.getRecordStatus());
        assertNull(record.getRecorderName());
        assertNull(record.getConfirmerName());
        record.setModelCode("W33P0100");
        ReflectionTestUtils.invokeMethod(service, "applyAdhesiveIntermediateTemplate", record);
        assertEquals(List.of("W33左", "W33右"), record.getThicknessLabels());
    }

    @Test
    void exactModelNeverLeaksToSeriesAndExplicitPrefixProvidesFallback() {
        HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();
        HcStationFormMapper forms = mock(HcStationFormMapper.class);
        ReflectionTestUtils.setField(service, "hcStationFormMapper", forms);
        HcStationFormDO exact = form(84L, "W33P0100", "左", "右");
        exact.setSchemaJson("{\"modelScope\":\"MODEL\",\"modelCode\":\"W33P0100\",\"modelPrefix\":\"W33P\",\"processFormType\":\"INTERMEDIATE_RECORD\"}");
        when(forms.selectEnabledByProcess("ADHESIVE")).thenReturn(List.of(exact));
        assertNull(ReflectionTestUtils.invokeMethod(service, "resolveAdhesiveIntermediateStationFormConfig", "W33P0300"));
        HcStationFormDO prefix = form(184L, "W33P", "左", "右");
        prefix.setSchemaJson("{\"modelScope\":\"PREFIX\",\"modelPrefix\":\"W33P\",\"processFormType\":\"INTERMEDIATE_RECORD\"}");
        when(forms.selectEnabledByProcess("ADHESIVE")).thenReturn(List.of(prefix, exact));
        assertSame(exact, ReflectionTestUtils.invokeMethod(service, "resolveAdhesiveIntermediateStationFormConfig", "W33P0100"));
        assertSame(prefix, ReflectionTestUtils.invokeMethod(service, "resolveAdhesiveIntermediateStationFormConfig", "W33P0300"));
        assertNull(ReflectionTestUtils.invokeMethod(service, "resolveAdhesiveIntermediateStationFormConfig", "W26P0300"));
    }

    @Test
    void generatesMeterRowsWithFallbackAndCap() {
        HcProcessReportServiceImpl service = new HcProcessReportServiceImpl();
        for (String length : new String[] { "0", "-1", "80", "80.1", "900" }) {
            List<HcAdhesiveIntermediateDetailRespVO> rows = ReflectionTestUtils.invokeMethod(service,
                    "buildDefaultAdhesiveIntermediateDetails", new BigDecimal(length));
            int expected = switch (length) { case "80" -> 80; case "80.1" -> 81; case "900" -> 600; default -> 50; };
            assertEquals(expected, rows.size());
            assertEquals(BigDecimal.valueOf(expected), rows.get(expected - 1).getLengthMark());
        }
    }

    @Test
    void savingColumnConfigurationRetainsLegacyItemsAndRejectsBlankTitles() {
        HcStationFormServiceImpl service = new HcStationFormServiceImpl();
        HcStationFormMapper forms = mock(HcStationFormMapper.class);
        HcStationFormItemMapper items = mock(HcStationFormItemMapper.class);
        ReflectionTestUtils.setField(service, "hcStationFormMapper", forms);
        ReflectionTestUtils.setField(service, "hcStationFormItemMapper", items);
        when(forms.selectById(83L)).thenReturn(form(83L, "W26P0100", "左", "右"));
        HcStationFormSaveReqVO request = new HcStationFormSaveReqVO();
        request.setId(83L);
        request.setProcessCode("ADHESIVE");
        request.setFormCode("HCPF_ADHESIVE1_INTERMEDIATE_RECORD_W26P0100_TEST");
        request.setFormName("粘胶1中间品记录表");
        request.setSchemaJson("{\"thicknessLabels\":[\"左\",\"右\"]}");
        service.updateHcStationForm(request);
        verifyNoInteractions(items);
        request.setSchemaJson("{\"thicknessLabels\":[\"左\",\" \"]}");
        assertThrows(RuntimeException.class, () -> service.updateHcStationForm(request));
    }

    private HcStationFormDO form(Long id, String model, String left, String right) {
        return HcStationFormDO.builder().id(id).processCode("ADHESIVE")
                .formCode("HCPF_ADHESIVE1_INTERMEDIATE_RECORD_" + model).formName(model + "中间品记录表")
                .schemaJson("{\"modelCode\":\"" + model + "\",\"processFormType\":\"INTERMEDIATE_RECORD\",\"thicknessLabels\":[\"" + left + "\",\"" + right + "\"]}")
                .build();
    }
}
