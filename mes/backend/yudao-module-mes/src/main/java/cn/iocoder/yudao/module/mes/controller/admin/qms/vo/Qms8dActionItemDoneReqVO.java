package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - QMS 8D行动项完成 Request VO")
@Data
public class Qms8dActionItemDoneReqVO {

    @Schema(description = "行动项ID")
    @NotNull(message = "行动项ID不能为空")
    private Long itemId;

    @Schema(description = "完成说明")
    @NotBlank(message = "完成说明不能为空")
    private String finishDesc;

    @Schema(description = "验证结果")
    private String verificationResult;

    @Schema(description = "目标状态，默认 DONE")
    private String itemStatus;
}
