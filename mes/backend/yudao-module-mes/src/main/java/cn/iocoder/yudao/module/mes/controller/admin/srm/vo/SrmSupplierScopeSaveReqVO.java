package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - 供应商名录管理范围新增/修改 Request VO")
@Data
public class SrmSupplierScopeSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "范围ID不能为空")
    private Long id;

    @Schema(description = "范围编号，新增时服务端自动生成")
    private String scopeCode;

    @Schema(description = "分组名称")
    @NotEmpty(message = "分组名称不能为空")
    @Size(max = 100, message = "分组名称不能超过100个字符")
    private String scopeName;

    @Schema(description = "状态：ENABLED/DISABLED")
    @Pattern(regexp = "ENABLED|DISABLED", message = "范围状态只能为 ENABLED 或 DISABLED")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    @Schema(description = "乐观锁")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

    @Valid
    private List<Member> members;

    @Data
    public static class Member {

        @NotNull(message = "成员用户不能为空")
        private Long userId;

        private String userName;

        @NotEmpty(message = "成员权限不能为空")
        @Pattern(regexp = "MASKED|FULL|EDIT", message = "成员权限只能为 MASKED、FULL 或 EDIT")
        private String permissionLevel;

    }

}
