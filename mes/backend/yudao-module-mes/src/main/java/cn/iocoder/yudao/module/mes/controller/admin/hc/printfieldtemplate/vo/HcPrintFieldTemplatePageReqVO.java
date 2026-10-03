package cn.iocoder.yudao.module.mes.controller.admin.hc.printfieldtemplate.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 打印字段模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HcPrintFieldTemplatePageReqVO extends PageParam {

    @Schema(description = "模板编码")
    private String templateCode;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "单据类型")
    private String documentType;

    @Schema(description = "状态")
    private Integer status;

}
