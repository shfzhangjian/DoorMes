package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - FAI扫码解析 Request VO")
@Data
public class QmsFaiScanReqVO {

    @Schema(description = "扫码内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "扫码内容不能为空")
    private String scanCode;

    @Schema(description = "扫码场景：LEDGER_TOOLBAR/WORKBENCH_HEADER/ITEM_OVERVIEW/ITEM_MODAL")
    private String scanScene;

    @Schema(description = "当前工作台 FAI 主单 ID；用于优先定位当前单据内检验项")
    private Long currentFaiId;

    @Schema(description = "限定来源模块；例如 GLUE_BOARD_FAI 仅扫码胶板检验单")
    private String sourceModule;

    @Schema(description = "客户端类型，例如 PC/PDA")
    private String clientType;

    @Schema(description = "终端或扫码设备编号")
    private String terminalCode;
}
