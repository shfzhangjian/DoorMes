package cn.iocoder.yudao.module.mes.controller.admin.plan.saleorder.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 计划用销售订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PlanSaleOrderPageReqVO extends PageParam {

    @Schema(description = "销售订单号")
    private String orderNo;

    @Schema(description = "ERP编号")
    private String erpNo;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品规格")
    private String productSpec;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否只查询生产计划可挂接订单")
    private Boolean planSelectable;

}
