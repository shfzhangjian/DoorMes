package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮一磨前置分配保存 Request VO")
@Data
public class HcRoughConsoleFirstAllocationSaveReqVO {
    private HcGrindingConsumptionVO consumption;


    @NotNull(message = "计划ID不能为空")
    private Long planId;
    private String planNo;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotNull(message = "设备不能为空")
    private Long equipmentId;

    @NotBlank(message = "母批号不能为空")
    private String motherBatchNo;

    @NotBlank(message = "加工单元不能为空")
    private String segmentMark;

    @NotNull(message = "起米位置不能为空")
    @DecimalMin(value = "0", message = "起米位置不能为负数")
    private BigDecimal startPosition;

    @NotNull(message = "确认加工米数不能为空")
    @DecimalMin(value = "0.001", message = "确认加工米数必须大于0")
    private BigDecimal confirmedLength;

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

    private String remark;

    @NotNull(message = "必须引用已确认的一次磨皮工艺参数点检记录")
    private Long processFormRecordId;
    private String processCheckHeaderDataJson;
    private List<HcWetPassWorkItemReqVO> processCheckDetails;
}
