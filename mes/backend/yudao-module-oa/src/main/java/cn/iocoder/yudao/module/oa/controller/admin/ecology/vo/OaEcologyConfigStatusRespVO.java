package cn.iocoder.yudao.module.oa.controller.admin.ecology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 泛微 OA 配置状态 Response VO")
@Data
public class OaEcologyConfigStatusRespVO {

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "OA 主域名")
    private String origin;

    @Schema(description = "OpenAPI 域名")
    private String openApiOrigin;

    @Schema(description = "是否配置 appKey")
    private Boolean appKeyConfigured;

    @Schema(description = "是否配置 appSecret")
    private Boolean appSecretConfigured;

    @Schema(description = "是否配置 corpId")
    private Boolean corpIdConfigured;

    @Schema(description = "是否配置临时 accessToken")
    private Boolean accessTokenConfigured;

    @Schema(description = "默认发送人团队标识")
    private String senderTenantKey;

    @Schema(description = "默认发送人员工 ID")
    private String senderEmployeeId;

    @Schema(description = "默认发送人名称")
    private String senderName;

    @Schema(description = "默认事件 ID")
    private Integer eventId;

    @Schema(description = "默认模块 ID")
    private Integer moduleId;

    @Schema(description = "默认消息渠道")
    private List<Integer> channels;

}
