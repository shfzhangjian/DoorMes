package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class SrmPerformanceActualReportBatchCreateReqVO {

    @NotEmpty(message = "请选择期间类型")
    private String periodType;
    @NotNull(message = "年度不能为空")
    private Integer evalYear;
    private Integer evalQuarter;
    private Integer evalMonth;
    @NotEmpty(message = "请选择供应商")
    private List<Long> supplierIds;

}
