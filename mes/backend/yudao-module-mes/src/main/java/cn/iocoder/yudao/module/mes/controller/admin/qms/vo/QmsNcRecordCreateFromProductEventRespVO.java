package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 从产品异常事件生成 NCR Response VO")
@Data
public class QmsNcRecordCreateFromProductEventRespVO {

    @Schema(description = "产品异常事件来源类型")
    private String sourceType;

    @Schema(description = "检验单ID")
    private Long inspectionId;

    @Schema(description = "NCR ID")
    private Long ncRecordId;

    @Schema(description = "NCR单号")
    private String ncNo;

    @Schema(description = "是否已存在")
    private Boolean existed;
}
