package cn.iocoder.yudao.module.mes.service.qms;

import cn.iocoder.yudao.module.mes.controller.admin.qms.vo.QmsIqcSaveReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcItemDO;
import cn.iocoder.yudao.module.mes.dal.dataobject.qms.QmsIqcOrderDO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QmsIqcItemWorkbookServiceTest {

    private final QmsIqcItemWorkbookService service = new QmsIqcItemWorkbookService();

    @Test
    void shouldImportAllIqcEntryTemplateTypes() throws Exception {
        QmsIqcOrderDO order = buildOrder();
        List<QmsIqcItemDO> items = buildItems();
        Map<Long, Map<String, String>> values = new LinkedHashMap<>();
        values.put(101L, Map.of("厚度", "1.25", "重量", "2.50"));
        values.put(102L, Map.of("T1", "10", "T2", "8", "T3", "9"));
        values.put(103L, Map.of("速度", "12.5", "压力", "3.2"));
        values.put(104L, Map.of("日期", "2026-01-01"));
        values.put(105L, Map.of("定性判定", "合格"));
        values.put(106L, Map.of("实测值", "6.8"));

        MockMultipartFile file = buildEditedWorkbook(order, items, values);

        assertTrue(service.supports(file));
        QmsIqcItemWorkbookService.ItemImportPlan plan = service.buildImportPlan(
                order, items, Collections.emptyMap(), file, false, true);

        assertEquals(6, plan.totalCount);
        assertEquals(6, plan.successCount);
        assertEquals(0, plan.failureCount);
        Map<Long, QmsIqcSaveReqVO.IqcSample> importedSamples = plan.validRows.stream()
                .collect(Collectors.toMap(row -> row.itemId, row -> row.sample));
        assertTrue(importedSamples.get(101L).getRawValuesJson().contains("\"thicknessMm\":1.25"));
        assertTrue(importedSamples.get(102L).getRawValuesJson().contains("\"t3Mm\":9"));
        assertTrue(importedSamples.get(103L).getRawValuesJson().contains("\"speed\":12.5"));
        assertEquals(LocalDate.of(2026, 1, 1), importedSamples.get(104L).getDateValue());
        assertEquals("OK", importedSamples.get(105L).getQualitativeValue());
        assertEquals("6.8", importedSamples.get(106L).getMeasuredValue().toPlainString());
    }

    @Test
    void shouldRejectIncompleteStructuredFields() throws Exception {
        QmsIqcOrderDO order = buildOrder();
        List<QmsIqcItemDO> items = List.of(buildItems().get(0));
        MockMultipartFile file = buildEditedWorkbook(order, items,
                Map.of(101L, Map.of("厚度", "1.25")));

        QmsIqcItemWorkbookService.ItemImportPlan plan = service.buildImportPlan(
                order, items, Collections.emptyMap(), file, false, true);

        assertEquals(1, plan.failureCount);
        assertEquals(0, plan.successCount);
        assertTrue(plan.messages.stream().anyMatch(message -> message.contains("重量")));
    }

    private QmsIqcOrderDO buildOrder() {
        return QmsIqcOrderDO.builder()
                .id(10L)
                .iqcNo("IQC-20260803-001")
                .receiptNo("REC-001")
                .materialCode("MAT-001")
                .materialName("测试物料")
                .supplierName("测试供应商")
                .batchNo("BATCH-001")
                .standardNo("STD-001")
                .standardVersion("1.0")
                .build();
    }

    private List<QmsIqcItemDO> buildItems() {
        return List.of(
                buildItem(101L, "密度", "QUANTITATIVE", "DENSITY_CALC", null),
                buildItem(102L, "压缩率", "QUANTITATIVE", "COMPRESSION_CALC", null),
                buildItem(103L, "自定义计算", "QUANTITATIVE", "CUSTOM",
                        "{\"sampleSize\":1,\"dataRule\":{\"inputFields\":["
                                + "{\"code\":\"speed\",\"name\":\"速度\",\"required\":true},"
                                + "{\"code\":\"pressure\",\"name\":\"压力\",\"required\":true}],"
                                + "\"resultFields\":[{\"code\":\"result\",\"formula\":\"speed * pressure\"}]}}"),
                buildItem(104L, "生产日期", "DATE", null, null),
                buildItem(105L, "外观", "QUALITATIVE", null, null),
                buildItem(106L, "厚度单值", "QUANTITATIVE", "SINGLE_VALUE", null));
    }

    private QmsIqcItemDO buildItem(Long id, String name, String itemType,
                                   String valueTemplate, String templateParams) {
        return QmsIqcItemDO.builder()
                .id(id)
                .iqcId(10L)
                .iqcNo("IQC-20260803-001")
                .inspectionItem(name)
                .itemType(itemType)
                .valueTemplate(valueTemplate)
                .templateParams(templateParams)
                .sampleSize(1)
                .expiryDays("DATE".equals(itemType) ? 365 : null)
                .build();
    }

    private MockMultipartFile buildEditedWorkbook(QmsIqcOrderDO order, List<QmsIqcItemDO> items,
                                                   Map<Long, Map<String, String>> values) throws Exception {
        byte[] template = service.buildWorkbook(order, items, Collections.emptyMap());
        byte[] edited;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(template));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.getSheet("检验项明细");
            Row header = sheet.getRow(4);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columnMap = new LinkedHashMap<>();
            for (int index = 0; index < header.getLastCellNum(); index++) {
                columnMap.put(formatter.formatCellValue(header.getCell(index)).trim(), index);
            }
            int itemIdColumn = columnMap.get("检验项 ID");
            for (int rowIndex = 5; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                Long itemId = Long.valueOf(formatter.formatCellValue(row.getCell(itemIdColumn)));
                Map<String, String> rowValues = values.get(itemId);
                if (rowValues == null) {
                    continue;
                }
                rowValues.forEach((headerName, value) ->
                        row.createCell(columnMap.get(headerName)).setCellValue(value));
            }
            workbook.write(outputStream);
            edited = outputStream.toByteArray();
        }
        return new MockMultipartFile("file", "IQC检验项明细.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", edited);
    }
}
