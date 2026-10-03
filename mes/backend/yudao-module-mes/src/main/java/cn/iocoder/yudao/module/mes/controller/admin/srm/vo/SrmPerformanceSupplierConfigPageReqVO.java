package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceSupplierConfigPageReqVO extends PageParam {

    private String configNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Long templateId;
    private Long templateVersionId;
    private String status;

}
