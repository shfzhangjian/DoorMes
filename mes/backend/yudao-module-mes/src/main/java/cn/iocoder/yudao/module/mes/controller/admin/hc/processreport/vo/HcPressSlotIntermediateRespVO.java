package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 压槽中间品记录 Response VO")
@Data
public class HcPressSlotIntermediateRespVO {

    private Long id;
    private Long planId;
    private String planNo;
    private Long planOperationId;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate recordDate;

    private String modelCode;
    private String materialCode;
    private String batchNo;
    private String templateCode;
    private String templateName;
    private String sourceExcel;
    private String sourceSheet;
    private String slotDepthStandard;
    private String thicknessStandard;
    private String thicknessHeaderText;
    private Integer thicknessColumnCount;
    private Integer thicknessIntervalCm;
    private Boolean showGlueBoardModel;
    private Long stationFormId;
    private String stationFormCode;
    private String stationFormName;
    private String stationFormDisplayName;
    private String stationFormSchemaJson;
    private String stationFormProcessCode;
    private String glueBoardModel;
    private String glueBoardWidthMm;
    private String glueBoardAdhesionGf;
    private String firstSampleSliceNo;
    private String frontSliceNo;
    private String middleSliceNo;
    private String endSliceNo;
    private BigDecimal inputQty;
    private BigDecimal outputQty;
    private BigDecimal firstSlotDepthMin;
    private BigDecimal firstSlotDepthMax;
    private BigDecimal firstSlotDepthAvg;
    private BigDecimal firstSlotDepthXMin;
    private BigDecimal firstSlotDepthXMax;
    private BigDecimal firstSlotDepthXAvg;
    private BigDecimal firstSlotDepthYMin;
    private BigDecimal firstSlotDepthYMax;
    private BigDecimal firstSlotDepthYAvg;
    private String recorderName;
    private String confirmerName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime fillTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    private String recordStatus;
    private String remark;
    private String extraJson;
    private Map<String, Object> importAttachment;
    private List<HcPressSlotIntermediateDetailRespVO> details;
}
