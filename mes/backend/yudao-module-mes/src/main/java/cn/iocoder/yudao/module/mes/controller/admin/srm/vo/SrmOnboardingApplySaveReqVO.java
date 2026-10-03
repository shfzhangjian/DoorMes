package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM供应商导入申请新增/修改 Request VO")
@Data
public class SrmOnboardingApplySaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "导入申请单号（新增保存时由服务端生成）")
    private String applyNo;

    private Long supplierId;
    private String supplierCode;

    @Schema(description = "意向供应商", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "意向供应商不能为空")
    private String supplierName;

    @Schema(description = "寻源物料/品类", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "物料名称不能为空")
    private String materialName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料型号")
    private String materialModel;

    @Schema(description = "适用产品")
    private String applicableProduct;

    @Schema(description = "导入类型：NEW全新物料、REPLACE替代现有物料、CUSTOMER_SPECIFIED客户指定物料")
    @NotEmpty(message = "导入类型不能为空")
    private String importType;

    @Schema(description = "被替代旧物料编码")
    private String replacedMaterialCode;

    @Schema(description = "被替代旧物料名称")
    private String replacedMaterialName;

    @Schema(description = "客户指定物料编码")
    private String customerMaterialCode;

    @Schema(description = "客户指定物料名称")
    private String customerMaterialName;

    private String applyReason;

    @Schema(description = "供应商优势说明")
    private String supplierAdvantageDesc;

    @Schema(description = "其他补充说明")
    private String supplementDesc;

    @Schema(description = "物料编码系统创建完成")
    private Boolean materialCodeCreated;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "物料编码创建完成日期")
    private LocalDate materialCodeCompleteDate;

    @Schema(description = "供应商录入合格供方清单完成")
    private Boolean supplierRosterCreated;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "供应商录入合格供方清单完成日期")
    private LocalDate supplierRosterCompleteDate;

    private String specReq;
    private String natureRequirement;
    private String certRequirement;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long useDeptReviewerUserId;
    private String useDeptReviewerUserName;
    private Long qualityReviewerUserId;
    private String qualityReviewerUserName;
    private Long techReviewerUserId;
    private String techReviewerUserName;
    private Long purchaseReviewerUserId;
    private String purchaseReviewerUserName;
    private Long generalManagerUserId;
    private String generalManagerUserName;
    private Long applicantId;
    private String applicantName;
    private String applyDept;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime applyTime;

    private String remark;

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
