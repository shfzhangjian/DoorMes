package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 各工序表单布局 Excel 导出 Request VO")
@Data
public class HcProcessFormLayoutExcelReqVO {

    @Schema(description = "导出文件名")
    private String fileName;

    @Schema(description = "Sheet 名称")
    private String sheetName;

    @Schema(description = "表单标题")
    private String title;

    @Schema(description = "明细标题")
    private String detailTitle;

    @Schema(description = "前端布局模式")
    private String visualMode;

    @Schema(description = "导入明细起始行，0 基索引；为空时按导出布局计算")
    private Integer importBodyStartRow;

    @Schema(description = "导入明细最大扫描行数")
    private Integer importBodyMaxRows;

    @Schema(description = "导入明细停止前缀，例如注：")
    private List<String> importStopPrefixes;

    @Schema(description = "头部信息")
    private List<HeaderItem> headerItems;

    @Schema(description = "表格列")
    private List<Column> columns;

    @Schema(description = "表格行")
    private List<RowData> rows;

    @Schema(description = "底部说明")
    private List<String> footerNotes;

    @Data
    public static class HeaderItem {

        private String label;
        private String value;
        private String valueType;
        private String bindKey;
        private String bindField;
        private Boolean editable;
    }

    @Data
    public static class Column {

        private String title;
        private Integer width;
    }

    @Data
    public static class RowData {

        private List<CellData> cells;
    }

    @Data
    public static class CellData {

        private Integer colIndex;
        private Integer rowSpan;
        private Integer colSpan;
        private String text;
        private String bindKey;
        private String bindField;
        private Boolean editable;
        private Integer importRowIndex;
        private Integer importColIndex;
    }
}
