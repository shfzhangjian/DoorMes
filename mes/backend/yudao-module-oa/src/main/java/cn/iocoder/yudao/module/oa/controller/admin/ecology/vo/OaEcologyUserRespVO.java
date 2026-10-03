package cn.iocoder.yudao.module.oa.controller.admin.ecology.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 泛微 OA 人员 Response VO")
@Data
public class OaEcologyUserRespVO {

    @Schema(description = "OA 人员 ID")
    private String employeeId;

    @Schema(description = "OA 用户账号 ID")
    private String userId;

    @Schema(description = "姓名")
    private String username;

    @Schema(description = "团队标识")
    private String tenantKey;

    @Schema(description = "工号")
    private String workCode;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "状态")
    private String status;

}
