package cn.iocoder.yudao.module.mes.controller.admin.qms.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 发货客户批号对齐分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsFgShippingAlignmentPageReqVO extends PageParam {

    @Schema(description = "关键字：发货通知单、客户、ERP订单、物料编码或产品型号")
    private String keyword;

}
