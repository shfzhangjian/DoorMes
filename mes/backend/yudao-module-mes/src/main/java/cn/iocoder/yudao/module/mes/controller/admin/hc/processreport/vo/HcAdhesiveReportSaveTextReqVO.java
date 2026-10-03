package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶1报工记录保存 Request VO")
@Data
public class HcAdhesiveReportSaveTextReqVO {

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

    @Schema(description = "报工日期", example = "2026-06-24")
    private String reportDate;

    @Schema(description = "开始时间", example = "2026-06-24 08:00:00")
    private String startTime;

    @Schema(description = "结束时间", example = "2026-06-24 16:00:00")
    private String endTime;

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

    @Schema(description = "记录时间", example = "2026-06-24 16:00:00")
    private String recorderTime;

    private String remark;
    private String extraJson;
    private List<HcAdhesiveCheckItemReqVO> checkItems;
    private List<HcWetReportAbnormalPositionSaveReqVO> abnormalPositions;
}
