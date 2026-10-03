package cn.iocoder.yudao.module.mes.controller.admin.hc.ocaptemplate.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - OCAP模板分页 Request VO")
@Data
public class HcOcapTemplatePageReqVO extends PageParam {

    @Schema(description = "OCAP编码")
    private String ocapCode;

    @Schema(description = "OCAP名称")
    private String ocapName;

    @Schema(description = "业务工序")
    private String businessStage;

    @Schema(description = "触发项目编码")
    private String triggerItemCode;

    @Schema(description = "触发条件")
    private String triggerCondition;

    @Schema(description = "处置步骤")
    private String actionSteps;

    @Schema(description = "状态")
    private String status;

}