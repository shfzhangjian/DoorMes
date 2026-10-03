package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceQuarterEvaluationPageReqVO extends PageParam {

    private String evaluationNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierInfo;
    private Integer evalYear;
    private Integer evalQuarter;
    private Long templateId;
    private Long templateVersionId;
    private String status;
    private Boolean todoOnly;
    private Set<Long> visibleEvaluationIds;

}
