package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 压槽中间品记录保存 Request VO")
@Data
public class HcPressSlotIntermediateSaveReqVO {

    private Long id;

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    private String planNo;

    @NotNull(message = "计划工序ID不能为空")
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
    private String recordStatus;
    private String remark;
    private String extraJson;
    private Map<String, Object> importAttachment;
    private List<HcPressSlotIntermediateDetailReqVO> details;
}
