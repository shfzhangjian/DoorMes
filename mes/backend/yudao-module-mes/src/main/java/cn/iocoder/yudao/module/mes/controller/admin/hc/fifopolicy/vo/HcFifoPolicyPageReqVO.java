package cn.iocoder.yudao.module.mes.controller.admin.hc.fifopolicy.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 先进先出策略分页 Request VO")
@Data
public class HcFifoPolicyPageReqVO extends PageParam {

    @Schema(description = "策略编码")
    private String policyCode;

    @Schema(description = "策略名称")
    private String policyName;

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "货主编码")
    private String ownerCode;

    @Schema(description = "适用范围")
    private String matchScope;

    @Schema(description = "出库规则")
    private String issueRule;

    @Schema(description = "优先字段")
    private String priorityFields;

    @Schema(description = "状态")
    private String status;

}