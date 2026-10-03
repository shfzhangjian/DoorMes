package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 磨皮看板第二次磨皮报工保存 Request VO")
@Data
public class HcRoughConsoleSecondReportSaveReqVO {
    private HcGrindingConsumptionVO consumption;


    private Long id;
    private Long firstDetailId;
    /** FIRST_ALLOCATED 模式下必须填写，对应一磨前置分配加工单元。 */
    private Long firstAllocationId;

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

    @NotBlank(message = "母批号不能为空")
    private String motherBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String sourceProductionBatchNo;
    private String segmentMark;
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
    /** 研发消耗米数，独立于固定损耗、NAP留样和异常米数。 */
    @DecimalMin(value = "0", message = "研发消耗米数不能为负数")
    @Digits(integer = 15, fraction = 3, message = "研发消耗米数最多保留三位小数")
    private BigDecimal researchConsumptionLength;
    @DecimalMin(value = "0", message = "当前磨皮起位置不能为负数")
    private BigDecimal startPosition;

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
    private String qualityThickness;
    private String qualityWidth;
    private BigDecimal grindingMeters;
    private String selfCheck;
    private String defectCode;
    private String remark;
    private List<HcWetReportAbnormalPositionSaveReqVO> abnormalPositions;

    @NotNull(message = "必须引用已确认的第二次磨皮工艺参数点检记录")
    private Long processFormRecordId;
    private String processCheckHeaderDataJson;
    private List<HcWetPassWorkItemReqVO> processCheckDetails;
    private String middleProductHeaderDataJson;
    private List<HcRoughConsoleMiddleProductItemReqVO> middleProductDetails;
}
