package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 粘胶1可视化加工分段开工/完工打点 Request VO")
@Data
public class HcAdhesiveSegmentTimingStampReqVO {

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotNull(message = "计划工序ID不能为空")
    private Long planOperationId;

    @NotNull(message = "来源二磨分段ID不能为空")
    private Long sourceGrindingSecondDetailId;

    /** START 或 END；实际时间仅由服务端生成。 */
    @NotBlank(message = "打点动作不能为空")
    private String action;

    private Long operatorId;

    private String operatorName;

}
