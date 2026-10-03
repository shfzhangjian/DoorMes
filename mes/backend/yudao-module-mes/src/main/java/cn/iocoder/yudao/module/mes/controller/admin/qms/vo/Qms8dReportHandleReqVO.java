package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - QMS 8D报告办理 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class Qms8dReportHandleReqVO extends Qms8dReportSaveReqVO {

    @Schema(description = "办理意见")
    private String opinion;

    @Schema(description = "下一处理人ID")
    private Long nextHandlerUserId;

    @Schema(description = "下一处理人名称")
    private String nextHandlerUserName;
}
