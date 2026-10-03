package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 裁切成品检验片级照片保存 Request VO")
@Data
public class QmsCutRoundFqcSubmissionDetailPhotoSaveReqVO {

    @Schema(description = "FQC主单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "FQC主单ID不能为空")
    private Long fqcId;

    @Schema(description = "送检明细ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "送检明细ID不能为空")
    private Long submissionDetailId;

    @Schema(description = "片级检验照片URL数组")
    private List<String> photoUrls;
}
