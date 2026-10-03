package cn.iocoder.yudao.module.mes.controller.admin.hc.formtemplate.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 表单模板分页 Request VO")
@Data
public class HcFormTemplatePageReqVO extends PageParam {

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "模板类型")
    private String templateType;

    @Schema(description = "业务工序")
    private String businessStage;

    @Schema(description = "表单样式")
    private String formStyle;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

}