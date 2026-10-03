package cn.iocoder.yudao.module.oa.controller.admin.ecology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 泛微 OA 消息发送 Request VO")
@Data
public class OaEcologySendMessageReqVO {

    @Schema(description = "消息标题", example = "MES测试消息")
    @NotBlank(message = "消息标题不能为空")
    private String title;

    @Schema(description = "消息内容", example = "这是一条来自 MES 的 EMobile10 测试消息")
    @NotBlank(message = "消息内容不能为空")
    private String text;

    @Schema(description = "接收人 OA 人员 ID")
    private String receiverEmployeeId;

    @Schema(description = "接收人团队标识")
    private String receiverTenantKey;

    @Schema(description = "接收人工号")
    private String receiverWorkCode;

    @Schema(description = "接收人显示名称，仅用于消息接口展示")
    private String receiverName;

    @Schema(description = "PC 打开地址")
    private String pcUrl;

    @Schema(description = "H5/App 打开地址")
    private String h5Url;

    @Schema(description = "事项 ID，用于后续标记已处理", example = "MES-MVP-001")
    private String entityId;

    @Schema(description = "事项名称", example = "MES消息MVP")
    private String entityName;

    @Schema(description = "是否待办消息")
    private Boolean todo;

    @Schema(description = "推送渠道：1 IM，2 云桥，3 邮件，4 短信")
    private List<Integer> channels;

    @Schema(description = "发送人 OA 人员 ID")
    private String senderEmployeeId;

    @Schema(description = "发送人团队标识")
    private String senderTenantKey;

    @Schema(description = "发送人姓名")
    private String senderName;

    @Schema(description = "发送人工号")
    private String senderWorkCode;

    @Schema(description = "事件 ID")
    private Integer eventId;

    @Schema(description = "模块 ID")
    private Integer moduleId;

}
