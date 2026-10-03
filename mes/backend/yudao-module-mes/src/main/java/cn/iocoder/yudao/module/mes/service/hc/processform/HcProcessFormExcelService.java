package cn.iocoder.yudao.module.mes.service.hc.processform;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormLayoutExcelReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo.HcProcessFormLayoutImportRespVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Date1904Support;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

@Service
@Validated
public class HcProcessFormExcelService {

    private static final String VALUE_TYPE_DATE = "DATE";
    private static final String VALUE_TYPE_DATETIME = "DATETIME";
    private static final String VALUE_TYPE_NUMBER = "NUMBER";
    private static final String VALUE_TYPE_TEXT = "TEXT";
    private static final String EXCEL_DATE_FORMAT = "yyyy-mm-dd";
    private static final String EXCEL_DATETIME_FORMAT = "yyyy-mm-dd hh:mm:ss";
    private static final DateTimeFormatter DATE_OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Shanghai");
    private static final int MIN_VALID_YEAR = 2000;
    private static final List<DateTimeFormatter> DATETIME_WITH_SECONDS_FORMATTERS = List.of(
            strictFormatter("uuuu-MM-dd HH:mm:ss"),
            strictFormatter("uuuu-M-d H:mm:ss"),
            strictFormatter("uuuu/M/d H:mm:ss"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    private static final List<DateTimeFormatter> DATETIME_WITHOUT_SECONDS_FORMATTERS = List.of(
            strictFormatter("uuuu-MM-dd HH:mm"),
            strictFormatter("uuuu-M-d H:mm"),
            strictFormatter("uuuu/M/d H:mm"));
    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            strictFormatter("uuuu-MM-dd"),
            strictFormatter("uuuu-M-d"),
            strictFormatter("uuuu/M/d"));

    @Resource
    private ObjectMapper objectMapper;

    public byte[] buildLayoutWorkbook(HcProcessFormLayoutExcelReqVO reqVO) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(safeSheetName(firstNotBlank(reqVO.getSheetName(), "表单")));
            ExcelStyles styles = buildStyles(workbook);
            int maxColumns = resolveMaxColumns(reqVO);
            for (int index = 0; index < maxColumns; index++) {
                sheet.setColumnWidth(index, resolveColumnWidth(reqVO, index));
            }

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.setHeightInPoints(28);
            writeCell(titleRow, 0, firstNotBlank(reqVO.getTitle(), "工序表单"), styles.titleStyle);
            merge(sheet, 0, 0, 0, maxColumns - 1);

            int headerRows = writeHeaderItems(sheet, reqVO.getHeaderItems(), rowIndex, maxColumns, styles);
            rowIndex += headerRows;
            if (headerRows > 0) {
                rowIndex += 1;
            }

            Row detailTitleRow = sheet.createRow(rowIndex++);
            writeCell(detailTitleRow, 0, firstNotBlank(reqVO.getDetailTitle(), "明细"), styles.panelStyle);
            merge(sheet, detailTitleRow.getRowNum(), detailTitleRow.getRowNum(), 0, maxColumns - 1);

            Row tableHeaderRow = sheet.createRow(rowIndex++);
            if ("adhesive1-middle".equalsIgnoreCase(reqVO.getVisualMode())) {
                tableHeaderRow.setHeightInPoints(48);
            }
            for (int colIndex = 0; colIndex < maxColumns; colIndex++) {
                String title = colIndex < safeList(reqVO.getColumns()).size()
                        ? safeText(reqVO.getColumns().get(colIndex).getTitle()) : "";
                writeCell(tableHeaderRow, colIndex, title, styles.tableHeaderStyle);
            }
            sheet.createFreezePane(0, tableHeaderRow.getRowNum() + 1);

            for (HcProcessFormLayoutExcelReqVO.RowData rowData : safeList(reqVO.getRows())) {
                Row row = getOrCreateRow(sheet, rowIndex++);
                row.setHeightInPoints(24);
                for (HcProcessFormLayoutExcelReqVO.CellData cellData : safeList(rowData.getCells())) {
                    int colIndex = firstNonNull(cellData.getColIndex(), 0);
                    CellStyle style = Boolean.TRUE.equals(cellData.getEditable()) ? styles.editableStyle : styles.bodyStyle;
                    writeCell(row, colIndex, safeText(cellData.getText()), style);
                    int rowSpan = Math.max(firstNonNull(cellData.getRowSpan(), 1), 1);
                    int colSpan = Math.max(firstNonNull(cellData.getColSpan(), 1), 1);
                    if (rowSpan > 1 || colSpan > 1) {
                        applyMergedRegionStyle(sheet, row.getRowNum(), row.getRowNum() + rowSpan - 1,
                                colIndex, colIndex + colSpan - 1, style);
                    }
                }
            }

            for (String note : safeList(reqVO.getFooterNotes())) {
                if (StrUtil.isBlank(note)) {
                    continue;
                }
                Row row = sheet.createRow(rowIndex++);
                writeCell(row, 0, note, styles.footerStyle);
                merge(sheet, row.getRowNum(), row.getRowNum(), 0, maxColumns - 1);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    public HcProcessFormLayoutImportRespVO importLayout(MultipartFile file, String layoutJson) throws IOException {
        HcProcessFormLayoutExcelReqVO layout = objectMapper.readValue(layoutJson, HcProcessFormLayoutExcelReqVO.class);
        HcProcessFormLayoutImportRespVO respVO = new HcProcessFormLayoutImportRespVO();
        List<HcProcessFormLayoutImportRespVO.HeaderValue> headerValues = new ArrayList<>();
        List<HcProcessFormLayoutImportRespVO.CellValue> cellValues = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int maxColumns = resolveMaxColumns(layout);
        LayoutRows rows = resolveLayoutRows(layout, maxColumns);
        int bodyStartRow = firstNonNull(layout.getImportBodyStartRow(), rows.bodyStartRow);

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            boolean use1904Windowing = workbook instanceof Date1904Support date1904Support
                    && date1904Support.isDate1904();
            List<HcProcessFormLayoutExcelReqVO.HeaderItem> headerItems = safeList(layout.getHeaderItems());
            int pairsPerRow = Math.max(maxColumns / 2, 1);
            for (int index = 0; index < headerItems.size(); index++) {
                HcProcessFormLayoutExcelReqVO.HeaderItem item = headerItems.get(index);
                if (!Boolean.TRUE.equals(item.getEditable()) || StrUtil.isBlank(item.getBindKey())) {
                    continue;
                }
                int rowIndex = rows.headerStartRow + index / pairsPerRow;
                int colIndex = (index % pairsPerRow) * 2 + 1;
                HcProcessFormLayoutImportRespVO.HeaderValue value = new HcProcessFormLayoutImportRespVO.HeaderValue();
                value.setBindKey(item.getBindKey());
                value.setBindField(item.getBindField());
                TypedCellValue typedValue = readHeaderCell(sheet, formatter, formulaEvaluator, use1904Windowing,
                        rowIndex, colIndex, item);
                value.setValue(typedValue.value());
                if (typedValue.missingSeconds()) {
                    messages.add("警告：表头“" + safeText(item.getLabel()) + "”缺少秒，已按 "
                            + typedValue.value() + " 导入");
                }
                headerValues.add(value);
            }

            List<HcProcessFormLayoutExcelReqVO.RowData> bodyRows = safeList(layout.getRows());
            int importRowCount = resolveImportBodyRowCount(layout, sheet, formatter, bodyStartRow, maxColumns);
            for (int bodyRowIndex = 0; bodyRowIndex < importRowCount; bodyRowIndex++) {
                RowDataCells rowCells = new RowDataCells(resolveImportBodyRow(layout, bodyRows, bodyRowIndex));
                for (HcProcessFormLayoutExcelReqVO.CellData cellData : rowCells.cells) {
                    if (!Boolean.TRUE.equals(cellData.getEditable()) || StrUtil.isBlank(cellData.getBindKey())) {
                        continue;
                    }
                    int layoutColIndex = firstNonNull(cellData.getColIndex(), 0);
                    int readRowIndex = firstNonNull(cellData.getImportRowIndex(), bodyStartRow + bodyRowIndex);
                    int readColIndex = firstNonNull(cellData.getImportColIndex(), layoutColIndex);
                    HcProcessFormLayoutImportRespVO.CellValue value = new HcProcessFormLayoutImportRespVO.CellValue();
                    value.setBodyRowIndex(bodyRowIndex);
                    value.setColIndex(layoutColIndex);
                    value.setBindKey(isSemiLayout(layout) || hasSourceImport(layout) ? String.valueOf(bodyRowIndex) : cellData.getBindKey());
                    value.setBindField(cellData.getBindField());
                    value.setValue(readCell(sheet, formatter, readRowIndex, readColIndex));
                    cellValues.add(value);
                }
            }
        }

        messages.add("已解析 " + (headerValues.size() + cellValues.size()) + " 个可回填单元格");
        respVO.setHeaderValues(headerValues);
        respVO.setCellValues(cellValues);
        respVO.setMessages(messages);
        respVO.setTotalCellCount(headerValues.size() + cellValues.size());
        return respVO;
    }

    private int writeHeaderItems(Sheet sheet, List<HcProcessFormLayoutExcelReqVO.HeaderItem> items, int startRow,
                                 int maxColumns, ExcelStyles styles) {
        if (CollUtil.isEmpty(items)) {
            return 0;
        }
        int pairsPerRow = Math.max(maxColumns / 2, 1);
        int rowCount = (int) Math.ceil(items.size() * 1.0 / pairsPerRow);
        for (int rowOffset = 0; rowOffset < rowCount; rowOffset++) {
            Row row = sheet.createRow(startRow + rowOffset);
            row.setHeightInPoints(23);
            for (int pairIndex = 0; pairIndex < pairsPerRow; pairIndex++) {
                int itemIndex = rowOffset * pairsPerRow + pairIndex;
                int colIndex = pairIndex * 2;
                if (itemIndex >= items.size()) {
                    writeCell(row, colIndex, "", styles.headerLabelStyle);
                    if (colIndex + 1 < maxColumns) {
                        writeCell(row, colIndex + 1, "", styles.bodyStyle);
                    }
                    continue;
                }
                HcProcessFormLayoutExcelReqVO.HeaderItem item = items.get(itemIndex);
                writeCell(row, colIndex, safeText(item.getLabel()), styles.headerLabelStyle);
                if (colIndex + 1 < maxColumns) {
                    writeHeaderCell(row, colIndex + 1, item, styles);
                }
            }
        }
        return rowCount;
    }

    private int resolveMaxColumns(HcProcessFormLayoutExcelReqVO reqVO) {
        int maxColumns = Math.max(safeList(reqVO.getColumns()).size(), 1);
        for (HcProcessFormLayoutExcelReqVO.RowData row : safeList(reqVO.getRows())) {
            for (HcProcessFormLayoutExcelReqVO.CellData cell : safeList(row.getCells())) {
                int colIndex = firstNonNull(cell.getColIndex(), 0);
                int colSpan = Math.max(firstNonNull(cell.getColSpan(), 1), 1);
                maxColumns = Math.max(maxColumns, colIndex + colSpan);
            }
        }
        return Math.max(maxColumns, 4);
    }

    private int resolveColumnWidth(HcProcessFormLayoutExcelReqVO reqVO, int colIndex) {
        List<HcProcessFormLayoutExcelReqVO.Column> columns = safeList(reqVO.getColumns());
        int width = colIndex < columns.size() ? firstNonNull(columns.get(colIndex).getWidth(), 140) : 140;
        return Math.max(8, Math.min(width / 7, 42)) * 256;
    }

    private int resolveImportBodyRowCount(HcProcessFormLayoutExcelReqVO layout, Sheet sheet,
                                          DataFormatter formatter, int bodyStartRow, int maxColumns) {
        if (!isSemiLayout(layout) && !hasSourceImport(layout)) {
            return safeList(layout.getRows()).size();
        }
        int maxRows = firstNonNull(layout.getImportBodyMaxRows(), sheet.getLastRowNum() - bodyStartRow + 1);
        int endRow = Math.min(sheet.getLastRowNum(), bodyStartRow + Math.max(maxRows, 0) - 1);
        int lastNonBlankOffset = -1;
        for (int rowIndex = bodyStartRow; rowIndex <= endRow; rowIndex++) {
            if (isImportStopRow(layout, sheet, formatter, rowIndex, maxColumns)) {
                break;
            }
            if (hasAnyImportCellValue(layout, sheet, formatter, rowIndex, maxColumns)) {
                lastNonBlankOffset = rowIndex - bodyStartRow;
            }
        }
        return lastNonBlankOffset < 0 ? 0 : lastNonBlankOffset + 1;
    }

    private HcProcessFormLayoutExcelReqVO.RowData resolveImportBodyRow(HcProcessFormLayoutExcelReqVO layout,
                                                                       List<HcProcessFormLayoutExcelReqVO.RowData> rows,
                                                                       int bodyRowIndex) {
        if (bodyRowIndex < rows.size()) {
            return rows.get(bodyRowIndex);
        }
        if ((isSemiLayout(layout) || hasSourceImport(layout)) && !rows.isEmpty()) {
            return rows.get(rows.size() - 1);
        }
        return null;
    }

    private boolean hasAnyCellValue(Sheet sheet, DataFormatter formatter, int rowIndex, int maxColumns) {
        for (int colIndex = 0; colIndex < maxColumns; colIndex++) {
            if (StrUtil.isNotBlank(readCell(sheet, formatter, rowIndex, colIndex))) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyImportCellValue(HcProcessFormLayoutExcelReqVO layout, Sheet sheet, DataFormatter formatter,
                                          int rowIndex, int maxColumns) {
        boolean hasImportColumns = false;
        for (HcProcessFormLayoutExcelReqVO.RowData rowData : safeList(layout.getRows())) {
            for (HcProcessFormLayoutExcelReqVO.CellData cellData : safeList(rowData.getCells())) {
                if (!Boolean.TRUE.equals(cellData.getEditable()) || StrUtil.isBlank(cellData.getBindKey())
                        || cellData.getImportColIndex() == null) {
                    continue;
                }
                hasImportColumns = true;
                if (StrUtil.isNotBlank(readCell(sheet, formatter, rowIndex, cellData.getImportColIndex()))) {
                    return true;
                }
            }
        }
        return hasImportColumns ? false : hasAnyCellValue(sheet, formatter, rowIndex, maxColumns);
    }

    private boolean isImportStopRow(HcProcessFormLayoutExcelReqVO layout, Sheet sheet, DataFormatter formatter,
                                    int rowIndex, int maxColumns) {
        List<String> prefixes = new ArrayList<>();
        for (String prefix : safeList(layout.getImportStopPrefixes())) {
            if (StrUtil.isNotBlank(prefix)) {
                prefixes.add(prefix);
            }
        }
        if (prefixes.isEmpty()) {
            return false;
        }
        for (int colIndex = 0; colIndex < maxColumns; colIndex++) {
            String value = readCell(sheet, formatter, rowIndex, colIndex);
            if (StrUtil.isBlank(value)) {
                continue;
            }
            for (String prefix : prefixes) {
                if (StrUtil.startWith(value, prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasSourceImport(HcProcessFormLayoutExcelReqVO layout) {
        if (layout.getImportBodyStartRow() != null || layout.getImportBodyMaxRows() != null
                || CollUtil.isNotEmpty(layout.getImportStopPrefixes())) {
            return true;
        }
        for (HcProcessFormLayoutExcelReqVO.RowData rowData : safeList(layout.getRows())) {
            for (HcProcessFormLayoutExcelReqVO.CellData cellData : safeList(rowData.getCells())) {
                if (cellData.getImportRowIndex() != null || cellData.getImportColIndex() != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSemiLayout(HcProcessFormLayoutExcelReqVO layout) {
        return StrUtil.equalsIgnoreCase(layout.getVisualMode(), "wet-semi")
                || StrUtil.equalsIgnoreCase(layout.getVisualMode(), "adhesive1-middle");
    }

    private LayoutRows resolveLayoutRows(HcProcessFormLayoutExcelReqVO layout, int maxColumns) {
        int headerRows = CollUtil.isEmpty(layout.getHeaderItems())
                ? 0
                : (int) Math.ceil(layout.getHeaderItems().size() * 1.0 / Math.max(maxColumns / 2, 1));
        int rowIndex = 1 + headerRows;
        if (headerRows > 0) {
            rowIndex += 1;
        }
        rowIndex += 1;
        rowIndex += 1;
        return new LayoutRows(1, rowIndex);
    }

    private ExcelStyles buildStyles(Workbook workbook) {
        ExcelStyles styles = new ExcelStyles();
        styles.titleStyle = createStyle(workbook, true, HorizontalAlignment.CENTER, IndexedColors.WHITE.getIndex());
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 15);
        styles.titleStyle.setFont(titleFont);
        styles.panelStyle = createStyle(workbook, true, HorizontalAlignment.LEFT, IndexedColors.GREY_25_PERCENT.getIndex());
        styles.headerLabelStyle = createStyle(workbook, true, HorizontalAlignment.RIGHT, IndexedColors.GREY_25_PERCENT.getIndex());
        styles.tableHeaderStyle = createStyle(workbook, true, HorizontalAlignment.CENTER, IndexedColors.GREY_25_PERCENT.getIndex());
        styles.bodyStyle = createStyle(workbook, false, HorizontalAlignment.LEFT, IndexedColors.WHITE.getIndex());
        styles.editableStyle = createStyle(workbook, false, HorizontalAlignment.LEFT, IndexedColors.LEMON_CHIFFON.getIndex());
        styles.dateStyle = createDateStyle(workbook, false, IndexedColors.WHITE.getIndex(), EXCEL_DATE_FORMAT);
        styles.editableDateStyle = createDateStyle(workbook, false, IndexedColors.LEMON_CHIFFON.getIndex(), EXCEL_DATE_FORMAT);
        styles.dateTimeStyle = createDateStyle(workbook, false, IndexedColors.WHITE.getIndex(), EXCEL_DATETIME_FORMAT);
        styles.editableDateTimeStyle = createDateStyle(workbook, false, IndexedColors.LEMON_CHIFFON.getIndex(), EXCEL_DATETIME_FORMAT);
        styles.footerStyle = createStyle(workbook, false, HorizontalAlignment.LEFT, IndexedColors.WHITE.getIndex());
        styles.footerStyle.setWrapText(true);
        return styles;
    }

    private CellStyle createStyle(Workbook workbook, boolean bold, HorizontalAlignment horizontalAlignment, short fillColor) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(horizontalAlignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setWrapText(true);
        if (fillColor != IndexedColors.WHITE.getIndex()) {
            style.setFillForegroundColor(fillColor);
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        Font font = workbook.createFont();
        font.setBold(bold);
        style.setFont(font);
        return style;
    }

    private CellStyle createDateStyle(Workbook workbook, boolean bold, short fillColor, String dataFormat) {
        CellStyle style = createStyle(workbook, bold, HorizontalAlignment.LEFT, fillColor);
        style.setDataFormat(workbook.createDataFormat().getFormat(dataFormat));
        return style;
    }

    private void writeHeaderCell(Row row, int colIndex, HcProcessFormLayoutExcelReqVO.HeaderItem item,
                                 ExcelStyles styles) {
        String valueType = normalizeValueType(item.getValueType());
        boolean editable = Boolean.TRUE.equals(item.getEditable());
        String text = safeText(item.getValue()).trim();
        if (VALUE_TYPE_DATETIME.equals(valueType)) {
            CellStyle style = editable ? styles.editableDateTimeStyle : styles.dateTimeStyle;
            if (isEmptyDisplayValue(text)) {
                writeCell(row, colIndex, "", style);
                return;
            }
            DateTimeParseResult parsed = requireDateTime(text, item.getLabel());
            writeDateTimeCell(row, colIndex, parsed.value(), style);
            return;
        }
        if (VALUE_TYPE_DATE.equals(valueType)) {
            CellStyle style = editable ? styles.editableDateStyle : styles.dateStyle;
            if (isEmptyDisplayValue(text)) {
                writeCell(row, colIndex, "", style);
                return;
            }
            writeDateCell(row, colIndex, requireDate(text, item.getLabel()), style);
            return;
        }
        if (VALUE_TYPE_NUMBER.equals(valueType)) {
            CellStyle style = editable ? styles.editableStyle : styles.bodyStyle;
            if (isEmptyDisplayValue(text)) {
                writeCell(row, colIndex, "", style);
                return;
            }
            writeNumberCell(row, colIndex, requireNumber(text, item.getLabel()), style);
            return;
        }
        writeCell(row, colIndex, safeText(item.getValue()), editable ? styles.editableStyle : styles.bodyStyle);
    }

    private void writeDateTimeCell(Row row, int colIndex, LocalDateTime value, CellStyle style) {
        Cell cell = getOrCreateCell(row, colIndex);
        cell.setCellValue(value.withNano(0));
        cell.setCellStyle(style);
    }

    private void writeDateCell(Row row, int colIndex, LocalDate value, CellStyle style) {
        Cell cell = getOrCreateCell(row, colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void writeNumberCell(Row row, int colIndex, BigDecimal value, CellStyle style) {
        Cell cell = getOrCreateCell(row, colIndex);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    private void writeCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = getOrCreateCell(row, colIndex);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private Cell getOrCreateCell(Row row, int colIndex) {
        Cell cell = row.getCell(colIndex);
        return cell == null ? row.createCell(colIndex) : cell;
    }

    private void applyMergedRegionStyle(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol, CellStyle style) {
        for (int rowIndex = firstRow; rowIndex <= lastRow; rowIndex++) {
            Row row = getOrCreateRow(sheet, rowIndex);
            for (int colIndex = firstCol; colIndex <= lastCol; colIndex++) {
                Cell cell = row.getCell(colIndex);
                if (cell == null) {
                    cell = row.createCell(colIndex);
                }
                cell.setCellStyle(style);
            }
        }
        merge(sheet, firstRow, lastRow, firstCol, lastCol);
    }

    private Row getOrCreateRow(Sheet sheet, int rowIndex) {
        Row row = sheet.getRow(rowIndex);
        return row == null ? sheet.createRow(rowIndex) : row;
    }

    private void merge(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        if (lastRow > firstRow || lastCol > firstCol) {
            sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
        }
    }

    private String readCell(Sheet sheet, DataFormatter formatter, int rowIndex, int colIndex) {
        Row row = sheet.getRow(rowIndex);
        if (row == null) {
            return "";
        }
        Cell cell = row.getCell(colIndex);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private TypedCellValue readHeaderCell(Sheet sheet, DataFormatter formatter, FormulaEvaluator formulaEvaluator,
                                          boolean use1904Windowing,
                                          int rowIndex, int colIndex,
                                          HcProcessFormLayoutExcelReqVO.HeaderItem item) {
        Row row = sheet.getRow(rowIndex);
        Cell cell = row == null ? null : row.getCell(colIndex);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return new TypedCellValue("", false);
        }
        String valueType = normalizeValueType(item.getValueType());
        if (!VALUE_TYPE_DATE.equals(valueType) && !VALUE_TYPE_DATETIME.equals(valueType)) {
            return new TypedCellValue(formatter.formatCellValue(cell, formulaEvaluator).trim(), false);
        }
        CellType effectiveType = cell.getCellType();
        org.apache.poi.ss.usermodel.CellValue formulaValue = null;
        if (effectiveType == CellType.FORMULA) {
            formulaValue = formulaEvaluator.evaluate(cell);
            effectiveType = formulaValue == null ? CellType.BLANK : formulaValue.getCellType();
        }
        if (effectiveType == CellType.BLANK) {
            return new TypedCellValue("", false);
        }
        if (effectiveType == CellType.NUMERIC) {
            double numericValue = formulaValue == null ? cell.getNumericCellValue() : formulaValue.getNumberValue();
            LocalDateTime dateTime = readExcelDateTime(numericValue, use1904Windowing, item.getLabel());
            return VALUE_TYPE_DATE.equals(valueType)
                    ? new TypedCellValue(dateTime.toLocalDate().format(DATE_OUTPUT_FORMATTER), false)
                    : new TypedCellValue(dateTime.format(DATETIME_OUTPUT_FORMATTER), false);
        }
        String text = formulaValue != null && effectiveType == CellType.STRING
                ? formulaValue.getStringValue().trim()
                : formatter.formatCellValue(cell, formulaEvaluator).trim();
        if (text.isEmpty()) {
            return new TypedCellValue("", false);
        }
        if (VALUE_TYPE_DATE.equals(valueType)) {
            return new TypedCellValue(requireDate(text, item.getLabel()).format(DATE_OUTPUT_FORMATTER), false);
        }
        DateTimeParseResult parsed = requireDateTime(text, item.getLabel());
        return new TypedCellValue(parsed.value().format(DATETIME_OUTPUT_FORMATTER), parsed.missingSeconds());
    }

    private LocalDateTime readExcelDateTime(double numericValue, boolean use1904Windowing, String label) {
        if (!DateUtil.isValidExcelDate(numericValue)) {
            throw invalidDateTime(label, String.valueOf(numericValue));
        }
        LocalDateTime value = DateUtil.getLocalDateTime(numericValue, use1904Windowing, true);
        if (!isValidBusinessDateTime(value)) {
            throw invalidDateTime(label, String.valueOf(numericValue));
        }
        return value.withNano(0);
    }

    private DateTimeParseResult requireDateTime(String text, String label) {
        DateTimeParseResult parsed = parseDateTime(text);
        if (parsed == null) {
            throw invalidDateTime(label, text);
        }
        return parsed;
    }

    private DateTimeParseResult parseDateTime(String rawValue) {
        String text = StrUtil.trimToEmpty(rawValue);
        if (text.isEmpty()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATETIME_WITH_SECONDS_FORMATTERS) {
            try {
                LocalDateTime value = LocalDateTime.parse(text, formatter).withNano(0);
                if (isValidBusinessDateTime(value)) {
                    return new DateTimeParseResult(value, false);
                }
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一个兼容格式。
            }
        }
        for (DateTimeFormatter formatter : DATETIME_WITHOUT_SECONDS_FORMATTERS) {
            try {
                LocalDateTime value = LocalDateTime.parse(text, formatter).withNano(0);
                if (isValidBusinessDateTime(value)) {
                    return new DateTimeParseResult(value, true);
                }
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一个兼容格式。
            }
        }
        try {
            LocalDateTime value = OffsetDateTime.parse(text).atZoneSameInstant(DEFAULT_ZONE).toLocalDateTime().withNano(0);
            if (isValidBusinessDateTime(value)) {
                return new DateTimeParseResult(value, false);
            }
        } catch (DateTimeParseException ignored) {
            // 继续尝试 Instant。
        }
        try {
            LocalDateTime value = Instant.parse(text).atZone(DEFAULT_ZONE).toLocalDateTime().withNano(0);
            return isValidBusinessDateTime(value) ? new DateTimeParseResult(value, false) : null;
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private LocalDate requireDate(String text, String label) {
        String normalized = StrUtil.trimToEmpty(text);
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate value = LocalDate.parse(normalized, formatter);
                if (value.getYear() >= MIN_VALID_YEAR) {
                    return value;
                }
            } catch (DateTimeParseException ignored) {
                // 继续尝试下一个兼容格式。
            }
        }
        DateTimeParseResult dateTime = parseDateTime(normalized);
        if (dateTime != null) {
            return dateTime.value().toLocalDate();
        }
        throw new ServiceException(400, "Excel表头“" + safeText(label)
                + "”日期格式不正确：" + normalized + "，请使用 yyyy-MM-dd");
    }

    private ServiceException invalidDateTime(String label, String value) {
        return new ServiceException(400, "Excel表头“" + safeText(label) + "”时间格式不正确：" + value
                + "，请使用 yyyy-MM-dd HH:mm:ss");
    }

    private BigDecimal requireNumber(String text, String label) {
        try {
            return new BigDecimal(StrUtil.trimToEmpty(text));
        } catch (NumberFormatException ex) {
            throw new ServiceException(400, "Excel表头“" + safeText(label) + "”数值格式不正确：" + text);
        }
    }

    private boolean isValidBusinessDateTime(LocalDateTime value) {
        return value != null && value.getYear() >= MIN_VALID_YEAR;
    }

    private boolean isEmptyDisplayValue(String value) {
        return StrUtil.isBlank(value) || "-".equals(value.trim());
    }

    private String normalizeValueType(String valueType) {
        String normalized = StrUtil.blankToDefault(valueType, VALUE_TYPE_TEXT).trim().toUpperCase();
        if (VALUE_TYPE_DATE.equals(normalized) || VALUE_TYPE_DATETIME.equals(normalized)
                || VALUE_TYPE_NUMBER.equals(normalized)) {
            return normalized;
        }
        return VALUE_TYPE_TEXT;
    }

    private static DateTimeFormatter strictFormatter(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }

    private String safeSheetName(String sheetName) {
        String value = firstNotBlank(sheetName, "表单");
        return value.replaceAll("[\\\\/?*\\[\\]:]", "_").substring(0, Math.min(value.length(), 31));
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private String firstNotBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StrUtil.isNotBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private <T> T firstNonNull(T value, T defaultValue) {
        return value == null ? defaultValue : value;
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private static class ExcelStyles {
        private CellStyle titleStyle;
        private CellStyle panelStyle;
        private CellStyle headerLabelStyle;
        private CellStyle tableHeaderStyle;
        private CellStyle bodyStyle;
        private CellStyle editableStyle;
        private CellStyle dateStyle;
        private CellStyle editableDateStyle;
        private CellStyle dateTimeStyle;
        private CellStyle editableDateTimeStyle;
        private CellStyle footerStyle;
    }

    private record DateTimeParseResult(LocalDateTime value, boolean missingSeconds) {
    }

    private record TypedCellValue(String value, boolean missingSeconds) {
    }

    private record LayoutRows(int headerStartRow, int bodyStartRow) {
    }

    private static class RowDataCells {
        private final List<HcProcessFormLayoutExcelReqVO.CellData> cells;

        private RowDataCells(HcProcessFormLayoutExcelReqVO.RowData rowData) {
            this.cells = rowData == null || rowData.getCells() == null ? List.of() : rowData.getCells();
        }
    }
}
