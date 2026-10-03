package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;
import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - 粘双面胶报工记录保存 Request VO")
@Data
public class HcAdhesiveReportSaveReqVO {

    private Long id;

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotNull(message = "来源二次磨皮分段不能为空")
    private Long sourceGrindingSecondDetailId;

    private String sourceType;
    private String sourceMode;
    private String sourcePlanNo;
    private String sourceMotherBatchNo;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;

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

    @DecimalMin(value = "0", message = "投入米数不能为负数")
    private BigDecimal inputLength;

    @DecimalMin(value = "0", message = "加工起始位置不能为负数")
    private BigDecimal startPosition;

    @DecimalMin(value = "0", message = "加工结束位置不能为负数")
    private BigDecimal endPosition;

    @DecimalMin(value = "0", message = "加工损耗米数不能为负数")
    private BigDecimal lossLength;

    @DecimalMin(value = "0", message = "产出米数不能为负数")
    private BigDecimal outputLength;

    @DecimalMin(value = "0", message = "NAP留样米数不能为负数")
    private BigDecimal napSampleLength;

    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;
    private Long glueBoardUsageId;
    private BigDecimal glueBoardStartPosition;
    private BigDecimal glueBoardUseLength;
    private Long aqcTaskId;
    private String aqcStatus;
    private String productQualityStatus;
    private String qualityLockReason;
    private String selfCheck;
    private String defectCode;
    private String recorderName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime recorderTime;

    private String remark;
    private String extraJson;
    private List<HcAdhesiveCheckItemReqVO> checkItems;
    private List<HcWetReportAbnormalPositionSaveReqVO> abnormalPositions;
}
