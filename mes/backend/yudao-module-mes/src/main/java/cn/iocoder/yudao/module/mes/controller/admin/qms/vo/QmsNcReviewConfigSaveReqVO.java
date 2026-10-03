package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - NCR评审会签配置保存 Request VO")
@Data
public class QmsNcReviewConfigSaveReqVO {

    private Long id;

    @Schema(description = "办理单位编码")
    private String unitCode;

    @Schema(description = "办理单位")
    @NotBlank(message = "办理单位不能为空")
    private String unitName;

    @Schema(description = "关联部门ID")
    private Long deptId;

    @Schema(description = "关联部门")
    private String deptName;

    @Schema(description = "默认办理人ID集合")
    private List<Long> handlerUserIds;

    @Schema(description = "默认办理人名称集合")
    private List<String> handlerUserNames;

    @Schema(description = "状态：0 启用，1 禁用")
    private Integer status;

    private Integer sort;
    private String remark;

    @Schema(description = "会签选项意见模板JSON")
    private String opinionTemplateJson;
}
