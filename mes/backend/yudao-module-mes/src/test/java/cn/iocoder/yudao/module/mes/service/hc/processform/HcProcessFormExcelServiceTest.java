package cn.iocoder.yudao.module.mes.service.hc.processform;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormLayoutExcelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormLayoutImportRespVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HcProcessFormExcelServiceTest {

    private final HcProcessFormExcelService service = new HcProcessFormExcelService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
    }

    @Test
    void shouldExportDateTimeAsNativeExcelValueWithSecondsFormat() throws Exception {
        HcProcessFormLayoutExcelReqVO layout = createDateTimeLayout("2026-06-17 15:41:27");

        byte[] data = service.buildLayoutWorkbook(layout);

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(data))) {
            Cell cell = workbook.getSheetAt(0).getRow(1).getCell(1);
            assertEquals(CellType.NUMERIC, cell.getCellType());
            assertEquals("yyyy-mm-dd hh:mm:ss", cell.getCellStyle().getDataFormatString());
            assertEquals(LocalDateTime.of(2026, 6, 17, 15, 41, 27),
                    cell.getLocalDateTimeCellValue().withNano(0));
        }
    }

    @Test
    void shouldImportNativeExcelDateTimeFromRawValueEvenWhenDisplayHidesSeconds() throws Exception {
        HcProcessFormLayoutExcelReqVO layout = createDateTimeLayout("2026-06-17 15:41:27");
        byte[] exported = service.buildLayoutWorkbook(layout);
        byte[] edited;
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(exported));
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Cell cell = workbook.getSheetAt(0).getRow(1).getCell(1);
            CellStyle hiddenSecondsStyle = workbook.createCellStyle();
            hiddenSecondsStyle.cloneStyleFrom(cell.getCellStyle());
            hiddenSecondsStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy/m/d h:mm"));
            cell.setCellStyle(hiddenSecondsStyle);
            workbook.write(outputStream);
            edited = outputStream.toByteArray();
        }

        HcProcessFormLayoutImportRespVO result = importWorkbook(edited, layout);

        assertEquals("2026-06-17 15:41:27", result.getHeaderValues().get(0).getValue());
    }

    @Test
    void shouldNormalizeLegacySlashMinuteTextAndReturnWarning() throws Exception {
        HcProcessFormLayoutExcelReqVO layout = createDateTimeLayout("");
        byte[] data = createTextHeaderWorkbook("2026/6/18 15:41");

        HcProcessFormLayoutImportRespVO result = importWorkbook(data, layout);

        assertEquals("2026-06-18 15:41:00", result.getHeaderValues().get(0).getValue());
        assertTrue(result.getMessages().stream().anyMatch(message -> message.contains("缺少秒")));
    }

    @Test
    void shouldRejectInvalidDateTimeText() throws Exception {
        HcProcessFormLayoutExcelReqVO layout = createDateTimeLayout("");
        byte[] data = createTextHeaderWorkbook("2026年错误时间");

        ServiceException exception = assertThrows(ServiceException.class, () -> importWorkbook(data, layout));

        assertTrue(exception.getMessage().contains("请使用 yyyy-MM-dd HH:mm:ss"));
    }

    private HcProcessFormLayoutExcelReqVO createDateTimeLayout(String value) {
        HcProcessFormLayoutExcelReqVO.HeaderItem headerItem = new HcProcessFormLayoutExcelReqVO.HeaderItem();
        headerItem.setLabel("入水洗槽时间");
        headerItem.setValue(value);
        headerItem.setValueType("DATETIME");
        headerItem.setEditable(true);
        headerItem.setBindField("headerData");
        headerItem.setBindKey("inWashTime");

        HcProcessFormLayoutExcelReqVO layout = new HcProcessFormLayoutExcelReqVO();
        layout.setTitle("湿法生产点检表");
        layout.setSheetName("湿法过站记录");
        layout.setHeaderItems(List.of(headerItem));
        return layout;
    }

    private HcProcessFormLayoutImportRespVO importWorkbook(byte[] data,
                                                            HcProcessFormLayoutExcelReqVO layout) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "湿法生产点检表.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", data);
        return service.importLayout(file, objectMapper.writeValueAsString(layout));
    }

    private byte[] createTextHeaderWorkbook(String value) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("湿法过站记录");
            sheet.createRow(0);
            Row row = sheet.createRow(1);
            row.createCell(1).setCellValue(value);
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }
}
