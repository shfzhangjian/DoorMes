package cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;
import cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate.HcFormTemplateVersionDO;

@Schema(description = "管理后台 - 表单模板新增/修改 Request VO")
@Data
public class HcFormTemplateSaveReqVO {

    @Schema(description = "模板编码")
    @NotBlank(message = "模板编码不能为空")
    private String templateCode;

    @Schema(description = "模板名称")
    @NotBlank(message = "模板名称不能为空")
    private String templateName;

    @Schema(description = "模板类型")
    @NotBlank(message = "模板类型不能为空")
    private String templateType;

    @Schema(description = "业务工序")
    @NotBlank(message = "业务工序不能为空")
    private String businessStage;

    @Schema(description = "表单样式")
    @NotBlank(message = "表单样式不能为空")
    private String formStyle;

    @Schema(description = "状态")
    @NotBlank(message = "状态不能为空")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "模板版本列表")
    private List<HcFormTemplateVersionDO> templateVersions;

}