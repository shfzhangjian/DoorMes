package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import lombok.Data;

@Data
public class SrmPerformanceQuarterEvaluationAvailableSupplierRespVO {

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String level;
    private String supplierSourceType;

}
