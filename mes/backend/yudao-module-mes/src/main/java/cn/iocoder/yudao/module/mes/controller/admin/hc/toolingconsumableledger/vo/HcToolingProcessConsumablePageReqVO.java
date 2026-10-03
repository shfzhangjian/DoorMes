package cn.iocoder.yudao.module.mes.controller.admin.hc.toolingconsumableledger.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 工序耗材字典分页 Request VO")
@Data
public class HcToolingProcessConsumablePageReqVO extends PageParam {

    @Schema(description = "工序编码")
    private String processCode;

    @Schema(description = "耗材种类编码")
    private String consumableType;

    @Schema(description = "默认 ERP 料号")
    private String defaultErpMaterialCode;

    @Schema(description = "状态")
    private Integer status;
}
