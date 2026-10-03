package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板第一次磨皮记录 Response VO")
@Data
public class HcRoughConsoleFirstReportRespVO {
    private HcGrindingConsumptionVO consumption;


    private Long id;
    private Long grindingReportId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String rowUid;
    private String sourceType;
    private String motherBatchNo;
    private String sourcePlanNo;
    private String sourceProductionBatchNo;
    private BigDecimal remainStartMeter;
    private BigDecimal remainLength;
    private BigDecimal processLength;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal napSampleLength;
    private BigDecimal availableBefore;
    private BigDecimal availableAfter;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    private String sandpaperBatchNo;
    private String pressure;
    private String lineSpeed;
    private String rotationSpeed;
    private String meterCounter;
    private String grindingThickness;
    private String afterGrindingThickness;
    private String selfCheck;
    private String defectCode;
    private String detailStatus;
    private String remark;
    private Long middleProductRecordId;
    private String middleProductStatus;
    private String middleProductRecorder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime middleProductRecordTime;

    private Integer middleProductGeneratedLength;

    /** 一磨 P/Q/R/S 段批号和开工/完工时间。 */
    private List<HcRoughConsoleSegmentTimingRespVO> segmentTimings;
}
