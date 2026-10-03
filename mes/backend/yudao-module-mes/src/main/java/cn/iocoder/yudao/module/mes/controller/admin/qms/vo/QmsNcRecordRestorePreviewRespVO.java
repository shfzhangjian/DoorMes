package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Schema(description = "管理后台 - NCR 还原预览 Response VO")
@Data
@Builder
public class QmsNcRecordRestorePreviewRespVO {

    @Schema(description = "NCR ID")
    private Long id;

    @Schema(description = "NCR 单号")
    private String ncNo;

    @Schema(description = "单据类型")
    private String documentType;

    @Schema(description = "当前状态")
    private String status;

    @Schema(description = "来源业务类型")
    private String sourceBizType;

    @Schema(description = "来源单号")
    private String sourceObjectNo;

    @Schema(description = "风险提醒")
    private List<String> warnings;

    @Schema(description = "是否存在风险提醒")
    private Boolean hasWarnings;
}
