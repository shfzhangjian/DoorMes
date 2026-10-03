package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM样品需求新增/修改 Request VO")
@Data
public class SrmSampleRequestSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "申请编号（新增保存时由服务端生成）")
    private String requestNo;

    @Schema(description = "物料名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "物料名称不能为空")
    private String materialName;

    @Schema(description = "型号")
    private String materialModel;

    @Schema(description = "申请类型：NORMAL普通、URGENT紧急", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "申请类型不能为空")
    private String applyType;

    @Schema(description = "申请部门")
    private String applyDept;

    @Schema(description = "使用产品")
    private String usedProduct;

    @Schema(description = "需求数量")
    @DecimalMin(value = "0", inclusive = false, message = "需求数量必须大于 0")
    private BigDecimal requireQty;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate applyDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate requireDate;

    @Schema(description = "指定供应商类型：HAS有、NONE无指定厂家、OTHER其它")
    private String specifiedSupplierType;

    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String technicalRequirement;
    private String purchaseDifficulty;
    private String rdSampleNecessity;
    private String oaApprovalUrl;
    private Long projectLeaderUserId;
    private String projectLeaderUserName;
    private Long purchaseOwnerUserId;
    private String purchaseOwnerUserName;
    private Long finalApproverUserId;
    private String finalApproverUserName;
    private String remark;
    private Boolean directArchive;

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
