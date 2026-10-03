package cn.iocoder.yudao.module.oa.framework.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛微 Ecology / EMobile OpenAPI 配置。
 */
@Component
@ConfigurationProperties(prefix = "yudao.oa.ecology")
@Data
public class OaEcologyProperties {

    private Boolean enabled = false;

    private String origin = "http://111.38.46.238:20600";

    private String openApiOrigin = "http://111.38.46.238:20600/papi/openapi";

    private String appKey;

    private String appSecret;

    private String corpId;

    private String authorizationCode;

    private String accessToken;

    private String senderTenantKey;

    private String senderEmployeeId = "10000";

    private String senderName = "MES消息";

    private String senderWorkCode;

    private Integer eventId = 8;

    private Integer moduleId = 111;

    private List<Integer> channels = new ArrayList<>(List.of(1));

    private Boolean todo = false;

    private Boolean outMessage = false;

}
