package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - CMP软垫翘曲片号统计人工编辑 Request VO")
@Data
public class QmsCmpWarpageSliceStatUpdateReqVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "记录编号不能为空")
    private Long id;

    @Schema(description = "发货片号/客户片号")
    @Size(max = 128, message = "发货片号长度不能超过128个字符")
    private String customerSliceNo;

    @Schema(description = "翘曲高度实际值(mm)")
    @DecimalMin(value = "0", message = "翘曲高度不能小于0")
    private BigDecimal warpageValueMm;

    @Schema(description = "判定结果(OK/NG)")
    @Pattern(regexp = "^(OK|NG)?$", message = "判定结果只能为OK或NG")
    private String inspectionResult;
}
