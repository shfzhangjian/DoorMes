package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 磨皮看板二磨分段留样送检申请 Request VO")
@Data
public class HcRoughSecondSegmentInspectionApplyReqVO {

    @Schema(description = "二次磨皮明细ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "二次磨皮明细ID不能为空")
    private Long secondDetailId;

    @Schema(description = "送检人员")
    private String submitterName;

    @Schema(description = "留样送检米数")
    @DecimalMin(value = "0", inclusive = false, message = "留样送检米数必须大于0")
    private BigDecimal sampleLength;

    @Schema(description = "备注")
    private String remark;
}
