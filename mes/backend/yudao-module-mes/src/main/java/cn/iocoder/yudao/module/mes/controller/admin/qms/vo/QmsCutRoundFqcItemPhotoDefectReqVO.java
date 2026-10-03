package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验项目照片缺陷码 Request VO")
@Data
public class QmsCutRoundFqcItemPhotoDefectReqVO {

    @Schema(description = "缺陷代码ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "照片缺陷代码ID不能为空")
    private Long defectCodeId;

    @Schema(description = "显示顺序")
    private Integer sort;
}
