package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmPerformanceSupplierConfigSaveReqVO {

    private Long id;
    private String configNo;
    @NotNull(message = "请选择供应商")
    private Long supplierId;
    private String supplierCode;
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;
    private String supplierSourceType;
    @NotNull(message = "请选择季度评价模板")
    private Long currentTemplateVersionId;
    private String status;
    private String remark;
    private Integer version;

}
