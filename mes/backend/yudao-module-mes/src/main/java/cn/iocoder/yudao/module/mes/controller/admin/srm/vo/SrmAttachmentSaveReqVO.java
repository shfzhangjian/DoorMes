package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM通用附件新增/修改 Request VO")
@Data
public class SrmAttachmentSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "业务类型不能为空")
    private String bizType;

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务ID不能为空")
    private Long bizId;

    @Schema(description = "文件名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件名称不能为空")
    private String fileName;

    @Schema(description = "文件地址", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "文件地址不能为空")
    private String fileUrl;

    @Schema(description = "附件分类", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "附件分类不能为空")
    private String attachmentCategory;

    private String fileType;
    private Long fileSize;
    private Long uploadUserId;
    private String uploadUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime uploadTime;

    private String remark;

}
