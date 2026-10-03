package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceActualReportPageReqVO extends PageParam {

    private String reportNo;
    private String supplierInfo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String periodType;
    private Integer evalYear;
    private Integer evalQuarter;
    private Integer evalMonth;
    private String status;
    private Long reporterUserId;

}
