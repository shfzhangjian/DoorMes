package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Schema(description = "管理后台 - SRM通用附件版本更新 Request VO")
@Data
public class SrmAttachmentVersionUpdateReqVO {

    @Schema(description = "当前最新附件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "当前附件ID不能为空")
    private Long sourceAttachmentId;

    @Schema(description = "附件分类", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "附件分类不能为空")
    private String attachmentCategory;

    @Schema(description = "新文件名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "新文件名称不能为空")
    private String fileName;

    @Schema(description = "新文件地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "新文件地址不能为空")
    private String fileUrl;

    private String fileType;
    private Long fileSize;

    @Schema(description = "更新说明", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "更新说明不能为空")
    @Size(max = 500, message = "更新说明不能超过500个字符")
    private String updateDescription;

}
