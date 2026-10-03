package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import lombok.Data;

@Data
public class SrmPerformanceActualReportAvailableSupplierRespVO {

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private Long configId;
    private Long templateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionNoSnapshot;

}
