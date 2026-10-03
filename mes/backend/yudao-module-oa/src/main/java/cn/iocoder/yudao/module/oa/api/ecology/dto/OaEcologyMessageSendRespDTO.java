package cn.iocoder.yudao.module.oa.api.ecology.dto;

import lombok.Data;

/**
 * 泛微 OA 消息发送 Response DTO。
 */
@Data
public class OaEcologyMessageSendRespDTO {

    private Boolean success;

    private String messageId;

    private String code;

    private String message;

    private Integer httpStatus;

    private String rawResponse;

}
