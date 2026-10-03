package cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 终端工位分页 Request VO")
@Data
public class HcTerminalPageReqVO extends PageParam {

    @Schema(description = "终端编码")
    private String terminalCode;

    @Schema(description = "终端名称")
    private String terminalName;

    @Schema(description = "工作中心ID")
    private Long workCenterId;

    @Schema(description = "工作中心编码")
    private String workCenterCode;

    @Schema(description = "终端模式")
    private String terminalMode;

    @Schema(description = "状态")
    private Integer status;

}