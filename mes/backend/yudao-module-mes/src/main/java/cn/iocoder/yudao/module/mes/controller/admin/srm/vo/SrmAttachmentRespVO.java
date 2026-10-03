package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

@Schema(description = "管理后台 - SRM通用附件 Response VO")
@Data
public class SrmAttachmentRespVO {

    private Long id;
    private String bizType;
    private Long bizId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String attachmentCategory;
    private String versionGroupNo;
    private Integer versionNo;
    private Long previousAttachmentId;
    private Boolean latestVersion;
    private Long uploadUserId;
    private String uploadUserName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime uploadTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime versionTime;

    private String updateDescription;
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

}
