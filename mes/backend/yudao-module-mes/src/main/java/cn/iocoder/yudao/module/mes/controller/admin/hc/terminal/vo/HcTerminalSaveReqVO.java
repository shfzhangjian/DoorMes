package cn.iocoder.yudao.module.mes.controller.admin.hc.terminal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - 终端工位新增/修改 Request VO")
@Data
public class HcTerminalSaveReqVO {

    @Schema(description = "终端编码")
    @NotBlank(message = "终端编码不能为空")
    private String terminalCode;

    @Schema(description = "终端名称")
    @NotBlank(message = "终端名称不能为空")
    private String terminalName;

    @Schema(description = "工作中心ID")
    @NotNull(message = "工作中心ID不能为空")
    private Long workCenterId;

    @Schema(description = "工作中心编码")
    @NotBlank(message = "工作中心编码不能为空")
    private String workCenterCode;

    @Schema(description = "终端模式")
    @NotBlank(message = "终端模式不能为空")
    private String terminalMode;

    @Schema(description = "状态")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "主键ID")
    private Long id;

}