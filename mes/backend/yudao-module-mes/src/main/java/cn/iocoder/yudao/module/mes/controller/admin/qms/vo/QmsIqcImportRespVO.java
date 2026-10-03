package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - IQC检验项明细导入 Response VO")
@Data
public class QmsIqcImportRespVO {

    @Schema(description = "导入文件名")
    private String fileName;

    @Schema(description = "导入状态")
    private String status;

    @Schema(description = "是否仅预览")
    private Boolean previewOnly;

    @Schema(description = "总行数")
    private Integer totalCount;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failureCount;

    @Schema(description = "警告数量")
    private Integer warningCount;

    @Schema(description = "校验摘要")
    private String validateSummary;

    @Schema(description = "导入后刷新出的IQC详情")
    private QmsIqcRespVO record;

    @Schema(description = "导入提示")
    private List<String> messages;
}
