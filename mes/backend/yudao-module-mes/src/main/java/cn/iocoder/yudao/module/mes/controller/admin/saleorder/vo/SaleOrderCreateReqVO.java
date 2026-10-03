package cn.iocoder.yudao.module.mes.controller.admin.saleorder.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 销售订单创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SaleOrderCreateReqVO extends SaleOrderBaseVO {
}
