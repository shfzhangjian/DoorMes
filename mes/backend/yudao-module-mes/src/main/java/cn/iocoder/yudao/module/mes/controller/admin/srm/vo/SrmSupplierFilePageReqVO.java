package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - SRM供应商协议资质分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SrmSupplierFilePageReqVO extends PageParam {

    @Schema(description = "供应商名称/代码")
    private String supplierInfo;

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String fileType;
    private String fileName;
    private String providedProduct;
    private String productModel;
    private String inspectionAgency;
    private String reportCode;
    private String fileStatus;
    private String standardCompliant;

    @Schema(description = "过期状态：EXPIRED-已过期，UNEXPIRED-未过期")
    private String expiryStatus;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] expiryDate;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
