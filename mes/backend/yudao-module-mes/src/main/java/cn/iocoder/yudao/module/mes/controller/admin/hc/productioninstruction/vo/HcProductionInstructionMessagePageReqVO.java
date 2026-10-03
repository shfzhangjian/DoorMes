package cn.iocoder.yudao.module.mes.controller.admin.hc.productioninstruction.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 工序生产指令消息分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class HcProductionInstructionMessagePageReqVO extends PageParam {

    @Schema(description = "计划 ID")
    private Long planId;

    @Schema(description = "计划工序 ID")
    private Long planOperationId;

    @Schema(description = "计划号")
    private String planNo;

    @Schema(description = "批次号")
    private String batchNo;

    @Schema(description = "标准工序编码")
    private String processCode;

    @Schema(description = "标准工序名称")
    private String processName;

    @Schema(description = "计划工序编码")
    private String operationCode;

    @Schema(description = "计划工序名称")
    private String operationName;

    @Schema(description = "指令类型：DAILY/PAUSE/RESUME/CANCEL")
    private String instructionType;

    @Schema(description = "指令状态：ISSUED/CONFIRMED/REVOKED")
    private String status;

    @Schema(description = "阅读状态：兼容旧版参数，工序消息模式忽略")
    private String readStatus;

    @Schema(description = "关键词")
    private String keyword;

}
