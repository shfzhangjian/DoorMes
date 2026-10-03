package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 质量任务候选检验项 Response VO")
@Data
public class QmsDispatchTaskCandidateItemRespVO {

    private Long id;
    private Long sourceItemId;
    private List<Long> sourceItemIds;
    private Long executionItemId;
    private Long standardItemId;
    private String nodeKey;
    private String nodeType;
    private Boolean selectable;
    private String positionCode;
    private String positionName;
    private Integer sampleGroupNo;
    private String pieceNo;
    private String inspectionItem;
    private String standardDesc;
    private String unit;
    private String itemType;
    private String inspectionMethod;
    private String testTool;
    private String templateParams;
    private Integer sampleSize;
    private BigDecimal avgMinLimit;
    private BigDecimal avgMaxLimit;
    private BigDecimal stdMinLimit;
    private BigDecimal stdMaxLimit;
    private BigDecimal minValueLimit;
    private BigDecimal maxValueLimit;
    private BigDecimal averageValue;
    private BigDecimal standardDeviation;
    private String defectCode;
    private String defectName;
    private BigDecimal measuredValue;
    private BigDecimal resultValue;
    private String qualitativeValue;
    private String measuredData;
    private String result;
    private String currentResult;
    private Integer sort;
    private Integer pieceCount;
    private Integer okPieceCount;
    private Integer ngPieceCount;
    private Integer pendingPieceCount;
    private List<QmsDispatchTaskCandidateItemRespVO> children;
}
