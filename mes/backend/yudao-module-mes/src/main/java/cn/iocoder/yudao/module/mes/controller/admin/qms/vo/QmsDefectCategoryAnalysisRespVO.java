package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 缺陷分类分析 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsDefectCategoryAnalysisRespVO {

    private Overview overview;
    private String groupLevel;
    private List<DefectColumn> defectColumns;
    private List<PivotRow> rows;
    private List<ParetoRow> paretoRows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Overview {

        private Integer totalCount;
        private Integer selfCheckCount;
        private Integer submissionCount;
        private Integer motherRollCount;
        private Integer segmentCount;
        private Integer pieceCount;
        private String primaryDefectCategory;
        private Integer primaryDefectCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DefectColumn {

        private String key;
        private String label;
        private Integer totalCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PivotRow {

        private String groupKey;
        private String modelSeriesCode;
        private String modelCode;
        private String motherRollBatchNo;
        private String segmentBatchNo;
        private Integer totalCount;
        private Map<String, Integer> defectCounts;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParetoRow {

        private String defectCategory;
        private Integer defectCount;
        private BigDecimal ratio;
        private BigDecimal cumulativeRatio;
    }

    @Schema(description = "缺陷分类分析明细 Response VO")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailRow {

        private String eventKey;
        private String eventSource;
        private String processCode;
        private String processName;
        private String inspectionType;
        private String inspectionTypeName;
        private String planNo;
        private String motherRollBatchNo;
        private String segmentBatchNo;
        private String scanConfirmPieceNo;
        private String processPieceNo;
        private String scanConfirmTime;
        private String inspectionTime;
        private String eventTime;
        private String modelSeriesCode;
        private String modelCode;
        private String materialCode;
        private String materialName;
        private String inspectionCategory;
        private String defectCode;
        private String defectName;
        private String defectLevel;
        private String inspectorName;
        private String inspectionNo;
        private String checkResult;
        private String remark;
        private Integer defectCount;
        private String sourceTable;
        private Long sourceId;
        private Long inspectionId;
    }
}
