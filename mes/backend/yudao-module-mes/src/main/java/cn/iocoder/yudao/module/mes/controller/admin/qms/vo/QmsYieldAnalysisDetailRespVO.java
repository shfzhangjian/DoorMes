package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - 良品率分析片号详情 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsYieldAnalysisDetailRespVO {

    private String groupKey;
    private String sourceKey;
    private String sourceTable;
    private Long sourceId;
    private String sourceRoute;
    private Long inspectionId;
    private String inspectionNo;
    private String inspectionSourceType;
    private String processCode;
    private String processName;
    private String planNo;
    private String motherRollNo;
    private String segmentNo;
    private String pieceNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String sizeSpec;
    private String actualSizeSpec;
    private String actualReportModelCode;
    private String selfCheck;
    private String submissionResult;
    private BigDecimal inputCount;
    private BigDecimal outputGoodCount;
    private BigDecimal outputNgCount;
    private Integer inspectionCount;
    private Integer selfCheckNgCount;
    private Integer submissionNgCount;
    private BigDecimal ngCount;
    private BigDecimal yieldRate;
    private Integer blackDotCount;
    private Integer blueDotCount;
    private Integer yellowDotCount;
    private Integer redDotCount;
    private Integer pinholeCount;
    private Integer stripeCount;
    private Integer wrinkleCount;
    private Integer waveCount;
    private Integer otherCount;
    private String defectSummary;
    private String confirmTime;
}
