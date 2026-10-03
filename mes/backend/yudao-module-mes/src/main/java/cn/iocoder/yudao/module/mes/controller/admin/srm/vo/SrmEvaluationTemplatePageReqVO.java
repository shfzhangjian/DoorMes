package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 供应商评估模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmEvaluationTemplatePageReqVO extends PageParam {

    private String templateCode;
    private String templateName;
    private String sceneType;
    private String status;

}
