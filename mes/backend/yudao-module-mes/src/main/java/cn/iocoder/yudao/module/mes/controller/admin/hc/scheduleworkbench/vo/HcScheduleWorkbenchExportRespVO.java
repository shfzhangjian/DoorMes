package cn.iocoder.yudao.module.mes.controller.admin.hc.scheduleworkbench.vo;

import java.util.List;
import java.util.Map;
import lombok.Data;

@Data
public class HcScheduleWorkbenchExportRespVO {

    private List<List<String>> head;

    private List<List<String>> rows;

    private Integer scheduleColumnStartIndex;

    private List<ExportRowStyle> rowStyles;

    @Data
    public static class ExportRowStyle {

        private Integer rowIndex;

        private Float rowHeightInPoints;

        private Map<Integer, ExportCellStyle> cellStyles;

    }

    @Data
    public static class ExportCellStyle {

        private List<ExportCellBlock> blocks;

    }

    @Data
    public static class ExportCellBlock {

        private String text;

        private String colorType;

    }

}
