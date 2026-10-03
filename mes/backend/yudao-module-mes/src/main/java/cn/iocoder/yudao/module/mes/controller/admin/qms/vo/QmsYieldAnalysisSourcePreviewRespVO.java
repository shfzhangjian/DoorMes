package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 良品率分析原始记录预览 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsYieldAnalysisSourcePreviewRespVO {

    private Boolean found;
    private String title;
    private String sourceKey;
    private String sourceTable;
    private Long sourceId;
    private String processCode;
    private String processName;
    private String planNo;
    private String motherRollNo;
    private String segmentNo;
    private String pieceNo;
    private String selfCheck;
    private String submissionResult;
    private String defectSummary;
    private String confirmTime;
    private List<FieldGroup> fieldGroups;
    private List<FieldItem> defectItems;
    private List<FieldItem> rawItems;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldGroup {

        private String title;
        private List<FieldItem> items;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldItem {

        private String label;
        private String value;
        private String valueType;
    }
}
