package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - NCR评审会签配置分页 Request VO")
@Data
public class QmsNcReviewConfigPageReqVO extends PageParam {

    @Schema(description = "办理单位")
    private String unitName;

    @Schema(description = "状态：0 启用，1 禁用")
    private Integer status;
}
