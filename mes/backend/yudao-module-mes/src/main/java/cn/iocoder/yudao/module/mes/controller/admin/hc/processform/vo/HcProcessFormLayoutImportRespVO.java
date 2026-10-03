package cn.iocoder.yudao.module.mes.controller.admin.hc.processform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 各工序表单布局 Excel 导入 Response VO")
@Data
public class HcProcessFormLayoutImportRespVO {

    @Schema(description = "头部回填值")
    private List<HeaderValue> headerValues;

    @Schema(description = "明细回填值")
    private List<CellValue> cellValues;

    @Schema(description = "解析消息")
    private List<String> messages;

    @Schema(description = "解析单元格数量")
    private Integer totalCellCount;

    @Data
    public static class HeaderValue {

        private String bindKey;
        private String bindField;
        private String value;
    }

    @Data
    public static class CellValue {

        private Integer bodyRowIndex;
        private Integer colIndex;
        private String bindKey;
        private String bindField;
        private String value;
    }
}
