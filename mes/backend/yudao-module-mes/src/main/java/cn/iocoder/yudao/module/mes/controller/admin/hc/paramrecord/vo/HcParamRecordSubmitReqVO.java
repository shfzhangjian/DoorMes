package cn.iocoder.yudao.module.mes.controller.admin.hc.paramrecord.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 工艺参数采集提交 Request VO")
@Data
public class HcParamRecordSubmitReqVO {

    @Schema(description = "报工ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "报工ID不能为空")
    private Long reportId;

    @Schema(description = "报工单号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "报工单号不能为空")
    private String reportNo;

    @Schema(description = "参数项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "参数项列表不能为空")
    @Valid
    private List<Item> items;

    @Schema(description = "参数项")
    @Data
    public static class Item {

        @Schema(description = "参数编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "参数编码不能为空")
        private String paramCode;

        @Schema(description = "参数名称", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "参数名称不能为空")
        private String paramName;

        @Schema(description = "参数值", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "参数值不能为空")
        private String paramValue;

        @Schema(description = "数值型参数值")
        private BigDecimal valueNum;

        @Schema(description = "单位")
        private String uom;

        @Schema(description = "判定结果")
        private String judgeResult;
    }
}
