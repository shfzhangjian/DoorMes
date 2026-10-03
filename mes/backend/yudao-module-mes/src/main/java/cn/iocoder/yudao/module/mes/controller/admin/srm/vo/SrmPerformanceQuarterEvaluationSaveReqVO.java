package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SrmPerformanceQuarterEvaluationSaveReqVO {

    private Long id;
    private String evaluationNo;
    @NotNull(message = "请选择供应商")
    private Long supplierId;
    private String supplierCode;
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;
    private String supplierSourceType;
    @NotNull(message = "年度不能为空")
    private Integer evalYear;
    @NotNull(message = "季度不能为空")
    private Integer evalQuarter;
    @NotNull(message = "请选择季度评价模板版本")
    private Long templateVersionId;
    private String remark;
    private Integer version;

}
