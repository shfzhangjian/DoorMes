package cn.iocoder.yudao.module.mes.controller.admin.hc.plansplit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - HC 拆批创建 Request VO")
@Data
public class HcPlanSplitCreateReqVO {

    @Schema(description = "来源计划号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "来源计划号不能为空")
    private String planNo;

    @Schema(description = "图谱节点Key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "拆批节点不能为空")
    private String nodeKey;

    @Schema(description = "拆出数量", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "拆出数量不能为空")
    @DecimalMin(value = "0.000001", message = "拆出数量必须大于0")
    private BigDecimal splitQty;

    @Schema(description = "拆出单位")
    private String unit;

    @Schema(description = "选择来源明细ID；按片拆批时使用")
    private List<Long> sourceIds;

    @Schema(description = "新计划开始工序ID")
    private Long startOperationId;

    @Schema(description = "新计划结束工序ID")
    private Long endOperationId;

    @Schema(description = "目标物料ID")
    private Long targetMaterialId;

    @Schema(description = "目标物料编码")
    private String targetMaterialCode;

    @Schema(description = "目标物料名称")
    private String targetMaterialName;

    @Schema(description = "目标型号ID")
    private Long targetModelId;

    @Schema(description = "目标型号编码")
    private String targetModelCode;

    @Schema(description = "目标型号名称")
    private String targetModelName;

    @Schema(description = "加工要求")
    private String instructionText;

    @Schema(description = "备注")
    private String remark;

}
