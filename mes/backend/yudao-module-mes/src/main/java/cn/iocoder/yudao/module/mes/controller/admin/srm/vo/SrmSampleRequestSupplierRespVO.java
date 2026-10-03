package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - SRM样品需求单供应商选择 Response VO")
@Data
public class SrmSampleRequestSupplierRespVO {

    @Schema(description = "供应商选择行唯一键")
    private String supplierKey;

    @Schema(description = "供应商主数据ID")
    private Long supplierId;

    @Schema(description = "供应商代码")
    private String supplierCode;

    @Schema(description = "供应商名称")
    private String supplierName;

}
