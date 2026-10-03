package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 磨皮看板第一次磨皮报工保存 Request VO")
@Data
public class HcRoughConsoleFirstReportSaveReqVO {

    private Long id;

    @NotNull(message = "计划ID不能为空")
    private Long planId;
    private String planNo;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;

    private String sourceType;
    @NotBlank(message = "母批号不能为空")
    private String motherBatchNo;
    private String sourcePlanNo;
    private Long sourcePlanId;
    private Long sourcePlanOperationId;
    private String sourceProductionBatchNo;
    private BigDecimal remainStartMeter;
    private BigDecimal remainLength;
    private BigDecimal availableBefore;
    private BigDecimal availableAfter;

    @DecimalMin(value = "0", message = "投入米数不能为负数")
    private BigDecimal processLength;
    @DecimalMin(value = "0", message = "加工损耗米数不能为负数")
    private BigDecimal lossLength;
    @DecimalMin(value = "0", message = "产出米数不能为负数")
    private BigDecimal outputLength;
    @DecimalMin(value = "0", message = "NAP留样米数不能为负数")
    private BigDecimal napSampleLength;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate reportDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime endTime;

    private String sandpaperBatchNo;
    private String guideClothBatchNo;
    private String currentSandpaperBatchNo;
    private String currentGuideClothBatchNo;
    private Boolean sandpaperChanged;
    private Boolean guideClothChanged;
    private String sandpaperReplaceReason;
    private String guideClothReplaceReason;
    private Long operatorId;
    private String operatorName;
    private String pressure;
    private String lineSpeed;
    private String rotationSpeed;
    private String meterCounter;
    private String grindingThickness;
    private String afterGrindingThickness;
    private String selfCheck;
    private String defectCode;
    private String remark;
    private List<HcWetReportAbnormalPositionSaveReqVO> abnormalPositions;
    private String processCheckHeaderDataJson;
    private List<HcWetPassWorkItemReqVO> processCheckDetails;
    private String middleProductHeaderDataJson;
    private List<HcRoughConsoleMiddleProductItemReqVO> middleProductDetails;
}
