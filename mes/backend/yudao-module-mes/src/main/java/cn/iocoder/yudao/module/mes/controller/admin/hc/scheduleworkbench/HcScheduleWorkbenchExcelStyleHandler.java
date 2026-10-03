package cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench;

import cn.idev.excel.metadata.Head;
import cn.idev.excel.metadata.data.WriteCellData;
import cn.idev.excel.write.handler.CellWriteHandler;
import cn.idev.excel.write.handler.SheetWriteHandler;
import cn.idev.excel.write.metadata.holder.WriteSheetHolder;
import cn.idev.excel.write.metadata.holder.WriteTableHolder;
import cn.idev.excel.write.metadata.holder.WriteWorkbookHolder;
import cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo.HcScheduleWorkbenchExportRespVO;
import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.ShapeTypes;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.util.Units;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.usermodel.TextAlign;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFSimpleShape;
import org.apache.poi.xssf.usermodel.XSSFTextParagraph;
import org.apache.poi.xssf.usermodel.XSSFTextRun;

/**
 * 排程工作台导出样式：日期单元格用矩形形状模拟页面中的工序卡片。
 */
public class HcScheduleWorkbenchExcelStyleHandler implements CellWriteHandler, SheetWriteHandler {

    private static final int HEAD_ROW_COUNT = 1;
    private static final int DEFAULT_SCHEDULE_COLUMN_START_INDEX = 9;
    private static final int[] STATIC_COLUMN_WIDTHS = {8, 18, 24, 14, 14, 28, 14, 14, 14};
    private static final int SCHEDULE_COLUMN_WIDTH = 24;
    private static final int BLOCK_LEFT_PADDING_PX = 8;
    private static final int BLOCK_RIGHT_X_PX = 156;
    private static final int BLOCK_TOP_PADDING_PX = 5;
    private static final int BLOCK_HEIGHT_PX = 17;
    private static final int BLOCK_GAP_PX = 4;

    private final int scheduleColumnStartIndex;
    private final int columnCount;
    private final Map<Integer, HcScheduleWorkbenchExportRespVO.ExportRowStyle> rowStyleMap = new HashMap<>();
    private final Map<String, CellStyle> cellStyleCache = new HashMap<>();
    private final Map<String, Font> fontCache = new HashMap<>();

    public HcScheduleWorkbenchExcelStyleHandler(HcScheduleWorkbenchExportRespVO exportData) {
        this.scheduleColumnStartIndex = exportData.getScheduleColumnStartIndex() == null
                ? DEFAULT_SCHEDULE_COLUMN_START_INDEX : exportData.getScheduleColumnStartIndex();
        this.columnCount = exportData.getHead() == null ? this.scheduleColumnStartIndex : exportData.getHead().size();
        List<HcScheduleWorkbenchExportRespVO.ExportRowStyle> rowStyles = exportData.getRowStyles();
        if (rowStyles != null) {
            for (HcScheduleWorkbenchExportRespVO.ExportRowStyle rowStyle : rowStyles) {
                if (rowStyle.getRowIndex() != null) {
                    rowStyleMap.put(rowStyle.getRowIndex(), rowStyle);
                }
            }
        }
    }

    @Override
    public void afterSheetCreate(WriteWorkbookHolder writeWorkbookHolder, WriteSheetHolder writeSheetHolder) {
        Sheet sheet = writeSheetHolder.getSheet();
        sheet.createFreezePane(scheduleColumnStartIndex, HEAD_ROW_COUNT);
        sheet.setDefaultRowHeightInPoints(22F);
        for (int i = 0; i < Math.min(STATIC_COLUMN_WIDTHS.length, scheduleColumnStartIndex); i++) {
            sheet.setColumnWidth(i, STATIC_COLUMN_WIDTHS[i] * 256);
        }
        for (int i = scheduleColumnStartIndex; i < columnCount; i++) {
            sheet.setColumnWidth(i, SCHEDULE_COLUMN_WIDTH * 256);
        }
    }

    @Override
    public void afterCellDispose(WriteSheetHolder writeSheetHolder, WriteTableHolder writeTableHolder,
            List<WriteCellData<?>> cellDataList, Cell cell, Head head, Integer relativeRowIndex, Boolean isHead) {
        Workbook workbook = cell.getSheet().getWorkbook();
        if (Boolean.TRUE.equals(isHead)) {
            cell.setCellStyle(style(workbook, "HEAD"));
            cell.getRow().setHeightInPoints(24F);
            return;
        }

        int dataRowIndex = cell.getRowIndex() - HEAD_ROW_COUNT;
        boolean scheduleCell = cell.getColumnIndex() >= scheduleColumnStartIndex;
        cell.setCellStyle(style(workbook, scheduleCell ? "SCHEDULE" : "BODY"));

        HcScheduleWorkbenchExportRespVO.ExportRowStyle rowStyle = rowStyleMap.get(dataRowIndex);
        if (rowStyle == null) {
            return;
        }
        applyRowHeight(cell.getRow(), rowStyle.getRowHeightInPoints());
        HcScheduleWorkbenchExportRespVO.ExportCellStyle exportCellStyle =
                rowStyle.getCellStyles() == null ? null : rowStyle.getCellStyles().get(cell.getColumnIndex());
        if (exportCellStyle == null || exportCellStyle.getBlocks() == null || exportCellStyle.getBlocks().isEmpty()) {
            return;
        }
        if (drawBlockShapes(cell, exportCellStyle.getBlocks())) {
            cell.setCellValue("");
            return;
        }
        cell.setCellValue(richText(workbook, exportCellStyle.getBlocks()));
    }

    private void applyRowHeight(Row row, Float rowHeightInPoints) {
        if (rowHeightInPoints == null) {
            return;
        }
        row.setHeightInPoints(Math.max(row.getHeightInPoints(), rowHeightInPoints));
    }

    private RichTextString richText(Workbook workbook,
            List<HcScheduleWorkbenchExportRespVO.ExportCellBlock> blocks) {
        StringBuilder text = new StringBuilder();
        Map<Integer, String> blockColorRanges = new HashMap<>();
        for (HcScheduleWorkbenchExportRespVO.ExportCellBlock block : blocks) {
            if (text.length() > 0) {
                text.append('\n');
            }
            int rangeStart = text.length();
            text.append("■■");
            blockColorRanges.put(rangeStart, block.getColorType());
            text.append(' ').append(block.getText());
        }

        RichTextString richText = workbook.getCreationHelper().createRichTextString(text.toString());
        richText.applyFont(font(workbook, "TEXT"));
        for (Map.Entry<Integer, String> entry : blockColorRanges.entrySet()) {
            richText.applyFont(entry.getKey(), entry.getKey() + 2, font(workbook, entry.getValue()));
        }
        return richText;
    }

    private boolean drawBlockShapes(Cell cell, List<HcScheduleWorkbenchExportRespVO.ExportCellBlock> blocks) {
        XSSFDrawing drawing = drawingPatriarch(cell.getSheet());
        if (drawing == null) {
            return false;
        }
        int columnIndex = cell.getColumnIndex();
        int rowIndex = cell.getRowIndex();
        for (int i = 0; i < blocks.size(); i++) {
            HcScheduleWorkbenchExportRespVO.ExportCellBlock block = blocks.get(i);
            int top = BLOCK_TOP_PADDING_PX + i * (BLOCK_HEIGHT_PX + BLOCK_GAP_PX);
            XSSFClientAnchor anchor = new XSSFClientAnchor(
                    Units.pixelToEMU(BLOCK_LEFT_PADDING_PX),
                    Units.pixelToEMU(top),
                    Units.pixelToEMU(BLOCK_RIGHT_X_PX),
                    Units.pixelToEMU(top + BLOCK_HEIGHT_PX),
                    columnIndex,
                    rowIndex,
                    columnIndex,
                    rowIndex);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            XSSFSimpleShape shape = drawing.createSimpleShape(anchor);
            RgbColor fillColor = rgbColor(block.getColorType());
            RgbColor lineColor = borderRgbColor(block.getColorType());
            shape.setShapeType(ShapeTypes.RECT);
            shape.setFillColor(fillColor.red(), fillColor.green(), fillColor.blue());
            shape.setLineStyleColor(lineColor.red(), lineColor.green(), lineColor.blue());
            shape.setLineWidth(0.75D);
            shape.setLeftInset(1.5D);
            shape.setRightInset(1.5D);
            shape.setTopInset(0.1D);
            shape.setBottomInset(0.1D);
            shape.setVerticalAlignment(VerticalAlignment.CENTER);
            shape.setWordWrap(false);
            shape.setText(block.getText());
            applyShapeTextStyle(shape, textColor(block.getColorType()));
        }
        return true;
    }

    private XSSFDrawing drawingPatriarch(Sheet sheet) {
        if (sheet instanceof XSSFSheet xssfSheet) {
            return xssfSheet.createDrawingPatriarch();
        }
        if (sheet instanceof SXSSFSheet sxssfSheet) {
            XSSFDrawing drawing = sxssfSheet.getDrawingPatriarch();
            if (drawing == null) {
                sxssfSheet.createDrawingPatriarch();
                drawing = sxssfSheet.getDrawingPatriarch();
            }
            return drawing;
        }
        return null;
    }

    private void applyShapeTextStyle(XSSFSimpleShape shape, Color textColor) {
        for (XSSFTextParagraph paragraph : shape.getTextParagraphs()) {
            paragraph.setTextAlign(TextAlign.CENTER);
            for (XSSFTextRun run : paragraph.getTextRuns()) {
                run.setFont("Microsoft YaHei");
                run.setFontSize(9D);
                run.setBold(true);
                run.setFontColor(textColor);
            }
        }
    }

    private CellStyle style(Workbook workbook, String type) {
        return cellStyleCache.computeIfAbsent(type, key -> {
            CellStyle style = workbook.createCellStyle();
            style.setWrapText(true);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            if ("HEAD".equals(key)) {
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
                style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                style.setFont(font(workbook, "HEAD"));
            } else if ("SCHEDULE".equals(key)) {
                style.setAlignment(HorizontalAlignment.LEFT);
                style.setVerticalAlignment(VerticalAlignment.TOP);
                style.setFont(font(workbook, "TEXT"));
            } else {
                style.setAlignment(HorizontalAlignment.CENTER);
                style.setVerticalAlignment(VerticalAlignment.CENTER);
                style.setFont(font(workbook, "TEXT"));
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
            font.setColor(color(key));
            return font;
        });
    }

    private short color(String type) {
        return switch (String.valueOf(type)) {
            case "COMPLETED" -> IndexedColors.GREEN.getIndex();
            case "MAINTENANCE" -> IndexedColors.BLUE.getIndex();
            case "PAUSED" -> IndexedColors.RED.getIndex();
            case "PROCESSING" -> IndexedColors.YELLOW.getIndex();
            case "PLANNED" -> IndexedColors.GREY_50_PERCENT.getIndex();
            default -> IndexedColors.BLACK.getIndex();
        };
    }

    private RgbColor rgbColor(String type) {
        return switch (String.valueOf(type)) {
            case "COMPLETED" -> new RgbColor(45, 142, 49);
            case "MAINTENANCE" -> new RgbColor(63, 109, 204);
            case "PAUSED" -> new RgbColor(238, 0, 0);
            case "PROCESSING" -> new RgbColor(255, 242, 0);
            case "PLANNED" -> new RgbColor(166, 166, 166);
            default -> new RgbColor(226, 232, 240);
        };
    }

    private RgbColor borderRgbColor(String type) {
        return switch (String.valueOf(type)) {
            case "COMPLETED" -> new RgbColor(28, 113, 35);
            case "MAINTENANCE" -> new RgbColor(45, 86, 178);
            case "PAUSED" -> new RgbColor(190, 0, 0);
            case "PROCESSING" -> new RgbColor(205, 174, 0);
            case "PLANNED" -> new RgbColor(120, 120, 120);
            default -> new RgbColor(148, 163, 184);
        };
    }

    private Color textColor(String type) {
        return switch (String.valueOf(type)) {
            case "PROCESSING", "PLANNED" -> new Color(17, 24, 39);
            default -> Color.WHITE;
        };
    }

    private record RgbColor(int red, int green, int blue) {
    }

}
