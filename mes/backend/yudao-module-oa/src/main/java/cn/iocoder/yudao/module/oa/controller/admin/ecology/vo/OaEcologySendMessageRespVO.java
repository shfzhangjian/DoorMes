package cn.iocoder.yudao.module.oa.controller.admin.ecology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 泛微 OA 消息发送 Response VO")
@Data
public class OaEcologySendMessageRespVO {

    @Schema(description = "OA 接口是否返回成功")
    private Boolean success;

    @Schema(description = "OA 消息 ID")
    private String messageId;

    @Schema(description = "OA 返回码")
    private String code;

    @Schema(description = "OA 返回信息")
    private String message;

    @Schema(description = "HTTP 状态码")
    private Integer httpStatus;

    @Schema(description = "解析后的接收人")
    private OaEcologyUserRespVO resolvedReceiver;

    @Schema(description = "OA 原始响应")
    private String rawResponse;

}
