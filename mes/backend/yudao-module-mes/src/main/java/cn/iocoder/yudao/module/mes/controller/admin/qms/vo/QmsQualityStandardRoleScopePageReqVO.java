package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 角色检验标准范围分页查询 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsQualityStandardRoleScopePageReqVO extends PageParam {

    @Schema(description = "角色ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @Schema(description = "范围类型：INCOMING/PROCESS/FINISHED/PACKAGING/PROCESS_GLUE_BOARD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    @Schema(description = "型号关键字，匹配产品型号编码/名称、胶板型号、规格型号")
    private String modelKeyword;

    @Schema(description = "标准综合关键字，匹配标准名称/标准编号/物料编码，支持逗号或分号分隔多个关键字")
    private String standardKeyword;

    @Schema(description = "标准名称")
    private String standardName;

    @Schema(description = "标准编号")
    private String standardNo;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "工序编码或名称")
    private String processKeyword;
}
