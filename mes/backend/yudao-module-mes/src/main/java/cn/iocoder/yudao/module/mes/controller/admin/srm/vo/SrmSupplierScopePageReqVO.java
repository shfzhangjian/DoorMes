package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - 供应商名录管理范围分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmSupplierScopePageReqVO extends PageParam {

    @Schema(description = "范围编号")
    private String scopeCode;

    @Schema(description = "分组名称")
    private String scopeName;

    @Schema(description = "状态：ENABLED/DISABLED")
    private String status;

}
