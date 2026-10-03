package cn.iocoder.yudao.module.mes.controller.admin.qms.measuretool.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 量检具新增申请流转 Request VO")
@Data
public class QmsMeasureToolApplyActionReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "主键ID不能为空")
    private Long id;

    @Schema(description = "审批人/处理人")
    private String operatorName;

    @Schema(description = "审批意见")
    @Size(max = 500, message = "审批意见不能超过500个字符")
    private String opinion;

}
