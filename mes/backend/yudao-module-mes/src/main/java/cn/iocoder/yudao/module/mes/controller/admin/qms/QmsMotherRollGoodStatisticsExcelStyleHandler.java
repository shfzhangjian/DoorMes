package cn.iocoder.yudao.module.mes.controller.admin.qms;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.RegionUtil;

/**
 * 母卷批次良品统计导出样式：合并第一行工序表头，并按业务合并基础列与工序数据列。
 */
public class QmsMotherRollGoodStatisticsExcelStyleHandler implements CellWriteHandler, SheetWriteHandler {

    private static final int HEAD_ROW_COUNT = 2;
    private static final int[] BASE_COLUMN_WIDTHS = {18, 14, 10, 12, 14, 10};
    private static final int STAGE_BATCH_COLUMN_WIDTH = 18;
    private static final int STAGE_VALUE_COLUMN_WIDTH = 10;

    private final List<List<String>> headRows;
    private final int columnCount;
    private final int baseColumnCount;
    private final int dataRowCount;
    private final List<RowMergeRange> baseColumnMergeRanges;
    private final List<CellMergeRange> dataColumnMergeRanges;
    private final Map<String, CellStyle> cellStyleCache = new HashMap<>();
    private final Map<String, Font> fontCache = new HashMap<>();
    private boolean topHeaderMerged;
    private boolean baseColumnRowsMerged;
    private boolean dataColumnRowsMerged;

    public QmsMotherRollGoodStatisticsExcelStyleHandler(List<List<String>> headRows, int baseColumnCount,
            int dataRowCount, List<RowMergeRange> baseColumnMergeRanges, List<CellMergeRange> dataColumnMergeRanges) {
        this.headRows = headRows;
        this.columnCount = headRows == null ? 0 : headRows.size();
        this.baseColumnCount = baseColumnCount;
        this.dataRowCount = dataRowCount;
        this.baseColumnMergeRanges = baseColumnMergeRanges == null ? List.of() : baseColumnMergeRanges;
        this.dataColumnMergeRanges = dataColumnMergeRanges == null ? List.of() : dataColumnMergeRanges;
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        sheet.createFreezePane(Math.max(0, baseColumnCount), HEAD_ROW_COUNT);
        sheet.setDefaultRowHeightInPoints(20F);
        for (int i = 0; i < columnCount; i++) {
            if (i < BASE_COLUMN_WIDTHS.length) {
                sheet.setColumnWidth(i, BASE_COLUMN_WIDTHS[i] * 256);
            } else {
                sheet.setColumnWidth(i, (i < baseColumnCount ? 12 : STAGE_VALUE_COLUMN_WIDTH) * 256);
            }
        }
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
            List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        Workbook workbook = cell.getSheet().getWorkbook();
        if (Boolean.TRUE.equals(isHead)) {
            cell.setCellStyle(style(workbook, "HEAD"));
            cell.getRow().setHeightInPoints(cell.getRowIndex() == 0 ? 24F : 22F);
            if (cell.getRowIndex() == HEAD_ROW_COUNT - 1 && cell.getColumnIndex() == columnCount - 1) {
                mergeTopHeader(cell.getSheet());
            }
            return;
        }
        cell.setCellStyle(style(workbook, cell.getColumnIndex() < baseColumnCount ? "BODY_TEXT" : "BODY_NUMBER"));
        if (cell.getRowIndex() == HEAD_ROW_COUNT + dataRowCount - 1
                && cell.getColumnIndex() == columnCount - 1) {
            mergeBaseColumnRows(cell.getSheet());
            mergeDataColumnRows(cell.getSheet());
        }
        if (head != null && head.getHeadNameList() != null
                && head.getHeadNameList().stream().anyMatch(name -> name != null && name.contains("批号"))) {
            cell.getSheet().setColumnWidth(cell.getColumnIndex(), STAGE_BATCH_COLUMN_WIDTH * 256);
        }
    }

    private void mergeTopHeader(Sheet sheet) {
        if (topHeaderMerged || headRows == null || headRows.isEmpty()) {
            return;
        }
        topHeaderMerged = true;
        int start = 0;
        while (start < columnCount) {
            String title = topTitle(start);
            int end = start;
            while (end + 1 < columnCount && title.equals(topTitle(end + 1))) {
                end++;
            }
            if (end > start) {
                CellRangeAddress region = new CellRangeAddress(0, 0, start, end);
                sheet.addMergedRegion(region);
                applyRegionBorder(sheet, region);
            }
            start = end + 1;
        }
    }

    private void mergeBaseColumnRows(Sheet sheet) {
        if (baseColumnRowsMerged || baseColumnCount <= 0 || baseColumnMergeRanges.isEmpty()) {
            return;
        }
        baseColumnRowsMerged = true;
        for (RowMergeRange range : baseColumnMergeRanges) {
            int firstRow = HEAD_ROW_COUNT + range.firstDataRowIndex();
            int lastRow = HEAD_ROW_COUNT + range.lastDataRowIndex();
            if (lastRow <= firstRow) {
                continue;
            }
            for (int columnIndex = 0; columnIndex < baseColumnCount; columnIndex++) {
                CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, columnIndex, columnIndex);
                sheet.addMergedRegion(region);
                applyRegionBorder(sheet, region);
            }
        }
    }

    private void mergeDataColumnRows(Sheet sheet) {
        if (dataColumnRowsMerged || dataColumnMergeRanges.isEmpty()) {
            return;
        }
        dataColumnRowsMerged = true;
        for (CellMergeRange range : dataColumnMergeRanges) {
            int firstRow = HEAD_ROW_COUNT + range.firstDataRowIndex();
            int lastRow = HEAD_ROW_COUNT + range.lastDataRowIndex();
            if (lastRow <= firstRow || range.columnIndex() < 0 || range.columnIndex() >= columnCount) {
                continue;
            }
            CellRangeAddress region = new CellRangeAddress(firstRow, lastRow,
                    range.columnIndex(), range.columnIndex());
            sheet.addMergedRegion(region);
            applyRegionBorder(sheet, region);
        }
    }

    private String topTitle(int columnIndex) {
        List<String> names = headRows.get(columnIndex);
        if (names == null || names.isEmpty() || names.get(0) == null) {
            return "";
        }
        return names.get(0);
    }

    private void applyRegionBorder(Sheet sheet, CellRangeAddress region) {
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);
        RegionUtil.setTopBorderColor(IndexedColors.BLACK.getIndex(), region, sheet);
        RegionUtil.setBottomBorderColor(IndexedColors.BLACK.getIndex(), region, sheet);
        RegionUtil.setLeftBorderColor(IndexedColors.BLACK.getIndex(), region, sheet);
        RegionUtil.setRightBorderColor(IndexedColors.BLACK.getIndex(), region, sheet);
    }

    private CellStyle style(Workbook workbook, String type) {
        return cellStyleCache.computeIfAbsent(type, key -> {
            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.BLACK.getIndex());
            style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
            style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
            style.setRightBorderColor(IndexedColors.BLACK.getIndex());
            style.setVerticalAlignment(VerticalAlignment.CENTER);
            if ("HEAD".equals(key)) {
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFont(font(workbook, "HEAD"));
            } else if ("BODY_TEXT".equals(key)) {
                style.setAlignment(HorizontalAlignment.LEFT);
                style.setFont(font(workbook, "BODY"));
            } else {
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setFont(font(workbook, "BODY"));
            }
            return style;
        });
    }

    private Font font(Workbook workbook, String type) {
        return fontCache.computeIfAbsent(type, key -> {
            Font font = workbook.createFont();
            font.setFontName("Microsoft YaHei");
            font.setFontHeightInPoints((short) 10);
            if ("HEAD".equals(key)) {
                font.setBold(true);
            }
            font.setColor(IndexedColors.BLACK.getIndex());
            return font;
        });
    }

    public record RowMergeRange(int firstDataRowIndex, int lastDataRowIndex) {
    }

    public record CellMergeRange(int firstDataRowIndex, int lastDataRowIndex, int columnIndex) {
    }

}
