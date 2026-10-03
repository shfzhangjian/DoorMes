package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 粘双面胶中间品记录 Response VO")
@Data
public class HcAdhesiveIntermediateRespVO {

    private Long stationFormId;
    private String stationFormCode;
    private String stationFormName;
    private List<String> thicknessLabels;
    private java.util.Map<String, String> formNotes;
    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long adhesiveReportId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime productionDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recordTime;

    private String modelCode;
    private String materialCode;
    private String batchNo;
    private BigDecimal processLength;
    private BigDecimal productWidthMm;
    private BigDecimal widthStart;
    private BigDecimal widthMiddle;
    private BigDecimal widthEnd;
    private String recorderName;
    private String confirmerName;
    private String recordStatus;
    private String remark;
    private String extraJson;
    private List<HcAdhesiveIntermediateDetailRespVO> details;
}
