package cn.iocoder.yudao.module.mes.controller.admin.hc.processreport.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "管理后台 - 配料报工不良明细 Request VO")
@Data
public class HcFormulaReportDefectReqVO {

    @Schema(description = "不良代码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String defectCode;

    @Schema(description = "不良名称")
    private String defectName;

    @Schema(description = "不良数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal defectQty;

    @Schema(description = "说明")
    private String remark;
}
