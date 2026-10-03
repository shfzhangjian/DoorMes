package cn.iocoder.yudao.module.mes.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FAI 泛微 OA 通知配置。
 */
@Component
@ConfigurationProperties(prefix = "yudao.mes.qms.fai.oa-notify")
@Data
public class MesFaiOaNotifyProperties {

    /**
     * 是否启用 FAI OA 消息通知。
     */
    private Boolean enabled = false;

    /**
     * 新检验单通知接收角色 ID。
     */
    private Long newOrderRoleId = 180L;

    /**
     * 检验审批人角色 ID。默认沿用现有质量主管角色。
     */
    private Long auditRoleId = 161L;

    /**
     * 首件检验页面地址。
     */
    private String faiPageUrl = "http://192.168.2.21:8080/mes/quality/in-process/fai";

}
