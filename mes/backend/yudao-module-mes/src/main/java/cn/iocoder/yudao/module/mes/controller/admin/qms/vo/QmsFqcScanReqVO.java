package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Schema(description = "管理后台 - FQC扫码解析 Request VO")
@Data
public class QmsFqcScanReqVO {

    @Schema(description = "扫码内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "扫码内容不能为空")
    private String scanCode;

    @Schema(description = "扫码场景：LEDGER_TOOLBAR/WORKBENCH_HEADER/ITEM_OVERVIEW/ITEM_MODAL")
    private String scanScene;

    @Schema(description = "当前工作台 FQC 主单 ID；用于优先定位当前单据内检验项")
    private Long currentFqcId;

    @Schema(description = "客户端类型，例如 PC/PDA")
    private String clientType;

    @Schema(description = "终端或扫码设备编号")
    private String terminalCode;
}
