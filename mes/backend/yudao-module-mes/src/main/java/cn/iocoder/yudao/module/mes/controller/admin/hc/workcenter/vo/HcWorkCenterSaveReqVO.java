package cn.iocoder.yudao.module.mes.controller.admin.hc.workcenter.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 工作中心新增/修改 Request VO")
@Data
public class HcWorkCenterSaveReqVO {

    @Schema(description = "工作中心编码")
    @NotBlank(message = "工作中心编码不能为空")
    private String wcCode;

    @Schema(description = "工作中心名称")
    @NotBlank(message = "工作中心名称不能为空")
    private String wcName;

    @Schema(description = "工序名称，兼容历史字段 process_stage")
    private String processStage;

    @Schema(description = "标准工序ID")
    @NotNull(message = "工序不能为空")
    private Long processId;

    @Schema(description = "标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    private String processName;

    @Schema(description = "产线编码")
    private String lineCode;

    @Schema(description = "产线名称")
    private String lineName;

    @Schema(description = "产线短码")
    private String lineShortCode;

    @Schema(description = "批次号产线码")
    private String batchLineCode;

    @Schema(description = "产线排序")
    private Integer lineSort;

    @Schema(description = "绑定工位 IP，多个用逗号、分号或换行分隔")
    private String terminalIps;

    @Schema(description = "标准小时产能")
    private BigDecimal capacityPerHour;

    @Schema(description = "产能单位")
    private String capacityUom;

    @Schema(description = "默认班制")
    private String defaultShiftMode;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

}
