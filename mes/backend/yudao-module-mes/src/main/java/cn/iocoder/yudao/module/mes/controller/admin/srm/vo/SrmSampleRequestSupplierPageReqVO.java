package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - SRM样品需求单供应商选择分页 Request VO")
@Data
public class SrmSampleRequestSupplierPageReqVO extends PageParam {

    @Schema(description = "供应商关键字，按供应商代码或名称模糊查询")
    private String keyword;

    @Schema(description = "供应商代码")
    private String supplierCode;

    @Schema(description = "供应商名称")
    private String supplierName;

}
