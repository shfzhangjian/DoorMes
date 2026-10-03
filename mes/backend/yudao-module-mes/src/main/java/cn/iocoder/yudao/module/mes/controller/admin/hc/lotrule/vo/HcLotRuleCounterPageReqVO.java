package cn.iocoder.yudao.module.mes.controller.admin.hc.lotrule.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 批号规则实例台账分页 Request VO")
@Data
public class HcLotRuleCounterPageReqVO extends PageParam {

    @Schema(description = "规则编码")
    private String ruleCode;

    @Schema(description = "计数器类型")
    private String counterType;

    @Schema(description = "业务维度键")
    private String bizDimensionKey;

    @Schema(description = "计数键")
    private String counterKey;

    @Schema(description = "重置键")
    private String resetKey;
}
