package cn.iocoder.yudao.module.mes.controller.admin.hc.batchtrace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - HC 批次追溯查询 Request VO")
@Data
public class HcBatchTraceQueryReqVO {

    @Schema(description = "批次号，支持母批、分段批次、单片片号、裁切 A/B 片号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "批次号不能为空")
    private String batchNo;

}
