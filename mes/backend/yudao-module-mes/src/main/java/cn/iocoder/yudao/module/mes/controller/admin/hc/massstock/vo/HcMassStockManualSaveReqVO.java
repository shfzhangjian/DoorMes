package cn.iocoder.yudao.module.mes.controller.admin.hc.massstock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - 量产备货库存人工维护 Request VO")
@Data
public class HcMassStockManualSaveReqVO {

    @Schema(description = "型号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "型号不能为空")
    @Size(max = 64, message = "型号长度不能超过64个字符")
    private String modelCode;

    @Schema(description = "母卷批号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "母卷批号不能为空")
    @Size(max = 64, message = "母卷批号长度不能超过64个字符")
    private String motherBatchNo;

    @Schema(description = "母卷段号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "母卷段号不能为空")
    @Size(max = 64, message = "母卷段号长度不能超过64个字符")
    private String motherSegmentBatchNo;

    @Schema(description = "SEM结果")
    @Size(max = 255, message = "SEM结果长度不能超过255个字符")
    private String semResult;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500个字符")
    private String remark;

}
