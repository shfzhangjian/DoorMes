package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class SrmPerformanceActualReportSaveReqVO {

    private Long id;
    private String reportNo;
    @NotNull(message = "请选择供应商")
    private Long supplierId;
    private String supplierCode;
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;
    private String supplierSourceType;
    private String periodType;
    @NotNull(message = "年度不能为空")
    private Integer evalYear;
    private Integer evalQuarter;
    private Integer evalMonth;
    private String remark;
    private Integer version;
    @Valid
    private List<Value> values;

    @Data
    public static class Value {
        private Long id;
        @NotEmpty(message = "来源指标编码不能为空")
        private String metricCode;
        @NotEmpty(message = "来源指标名称不能为空")
        private String metricName;
        private String valueType;
        private BigDecimal numericValue;
        private String textValue;
        private String unit;
        private String sourceNodeKey;
        private Boolean evidenceRequired;
        private String valueStatus;
        private String remark;
    }

}
