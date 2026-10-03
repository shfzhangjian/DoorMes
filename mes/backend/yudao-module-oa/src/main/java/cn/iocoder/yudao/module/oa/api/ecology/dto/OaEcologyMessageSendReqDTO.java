package cn.iocoder.yudao.module.oa.api.ecology.dto;

import lombok.Data;

import java.util.List;

/**
 * 泛微 OA 消息发送 Request DTO。
 */
@Data
public class OaEcologyMessageSendReqDTO {

    private String title;

    private String text;

    private String receiverEmployeeId;

    private String receiverTenantKey;

    private String receiverWorkCode;

    private String receiverName;

    private String pcUrl;

    private String h5Url;

    private String entityId;

    private String entityName;

    private Boolean todo;

    private List<Integer> channels;

    private String senderEmployeeId;

    private String senderTenantKey;

    private String senderName;

    private String senderWorkCode;

    private Integer eventId;

    private Integer moduleId;

}
