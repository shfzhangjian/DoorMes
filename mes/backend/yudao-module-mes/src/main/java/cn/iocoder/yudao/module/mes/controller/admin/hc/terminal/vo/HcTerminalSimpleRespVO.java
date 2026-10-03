package cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 终端工位 Simple Response VO")
@Data
public class HcTerminalSimpleRespVO {

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "终端编码")
    private String terminalCode;

    @Schema(description = "终端名称")
    private String terminalName;

    @Schema(description = "状态")
    private Integer status;

}