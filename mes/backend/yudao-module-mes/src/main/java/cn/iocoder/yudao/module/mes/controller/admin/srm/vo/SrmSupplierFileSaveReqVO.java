package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM供应商协议资质新增/修改 Request VO")
@Data
public class SrmSupplierFileSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    private Long supplierId;
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;

    @Schema(description = "档案类型", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "档案类型不能为空")
    private String fileType;

    @Schema(description = "档案名称/摘要", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "档案名称不能为空")
    private String fileName;

    @Schema(description = "供应产品")
    private String providedProduct;

    @Schema(description = "产品型号")
    private String productModel;

    @Schema(description = "检测机构")
    private String inspectionAgency;

    @Schema(description = "报告编码")
    private String reportCode;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate effectDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiryDate;

    private String fileStatus;

    @Min(value = 0, message = "提前预警天数不能小于0")
    private Integer warningDays;

    @Schema(description = "是否符合标准：Y-是，N-否")
    private String standardCompliant;

    @Schema(description = "有效期月数快照")
    private Integer validityMonths;

    @Schema(description = "过期日期计算规则")
    private String expiryRule;

    @Schema(description = "动态字段JSON")
    private String payloadJson;

    private String attachmentName;
    private String attachmentUrl;
    private String sourceBizType;
    private Long sourceBizId;
    private String remark;

}
