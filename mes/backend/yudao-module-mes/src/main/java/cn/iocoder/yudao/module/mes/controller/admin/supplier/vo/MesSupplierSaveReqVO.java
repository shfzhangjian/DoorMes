package cn.iocoder.yudao.module.mes.controller.admin.supplier.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - 供应商主数据新增/修改 Request VO")
@Data
public class MesSupplierSaveReqVO {

    /**
     * 更新校验分组：新增由服务端初始化版本号，更新必须携带主键和版本号。
     */
    public interface Update {
    }

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "23625")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "供应商编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "供应商编码不能为空")
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;

    @Schema(description = "使用部门")
    @Size(max = 64, message = "使用部门不能超过64个字符")
    private String usingDepartment;

    @Schema(description = "简称", example = "张三")
    private String shortName;

    @Schema(description = "联系人")
    private String contactPerson;

    @Schema(description = "联系电话")
    private String contactPhone;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "详细地址")
    @Size(max = 255, message = "供应商地址不能超过255个字符")
    private String address;

    @Schema(description = "企业性质")
    @Size(max = 64, message = "企业性质不能超过64个字符")
    private String companyNature;

    @Schema(description = "法定代表人")
    @Size(max = 64, message = "法定代表人不能超过64个字符")
    private String legalPerson;

    @Schema(description = "注册资本(万元)")
    private BigDecimal registeredCapital;

    @Schema(description = "成立日期", example = "2020-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate establishDate;

    @Schema(description = "产地")
    @Size(max = 128, message = "产地不能超过128个字符")
    private String originPlace;

    @Schema(description = "原厂信息")
    @Size(max = 255, message = "原厂信息不能超过255个字符")
    private String originalFactoryInfo;

    @Schema(description = "主营产品")
    @Size(max = 255, message = "主营产品不能超过255个字符")
    private String mainProducts;

    @Schema(description = "提供/协作产品")
    @Size(max = 255, message = "提供/协作产品不能超过255个字符")
    private String providedProduct;

    @Schema(description = "型号")
    @Size(max = 128, message = "型号不能超过128个字符")
    private String model;

    @Schema(description = "物料代码")
    @Size(max = 64, message = "物料代码不能超过64个字符")
    private String materialCode;

    @Schema(description = "适用产品")
    @Size(max = 255, message = "适用产品不能超过255个字符")
    private String applicableProduct;

    @Schema(description = "导入日期", example = "2026-07-24")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate importDate;

    @Schema(description = "历史物料类别")
    @Size(max = 64, message = "历史物料类别不能超过64个字符")
    private String materialCategory;

    @Schema(description = "物料等级")
    @Size(max = 64, message = "物料等级不能超过64个字符")
    @Pattern(regexp = "A|B|C|D", message = "物料等级只能为 A、B、C 或 D")
    private String materialGrade;

    @Schema(description = "结算付款条件")
    @Size(max = 128, message = "结算付款条件不能超过128个字符")
    private String paymentTerms;

    @Schema(description = "交货方式")
    @Size(max = 128, message = "交货方式不能超过128个字符")
    private String deliveryMethod;

    @Schema(description = "供应商资源状态: PENDING(考察中), QUALIFIED(合格), UNQUALIFIED(不合格), FROZEN(冻结), ELIMINATED(淘汰), EXITED(退出)", requiredMode = Schema.RequiredMode.REQUIRED, example = "QUALIFIED")
    @NotEmpty(message = "供应商状态不能为空")
    @Pattern(regexp = "PENDING|QUALIFIED|UNQUALIFIED|FROZEN|ELIMINATED|EXITED", message = "供应商状态只能为 PENDING、QUALIFIED、UNQUALIFIED、FROZEN、ELIMINATED 或 EXITED")
    private String status;

    @Schema(description = "来源基本情况调查表ID")
    private Long sourceSurveyId;

    @Schema(description = "来源基本情况调查表编号")
    private String sourceSurveyNo;

    @Schema(description = "登记人ID")
    private Long registrarId;

    @Schema(description = "登记人")
    private String registrarName;

    @Schema(description = "初始化说明")
    private String initializationReason;

    @Schema(description = "供应商名录管理范围ID")
    private Long scopeId;

    @Schema(description = "供应商名录管理范围编号")
    private String scopeCode;

    @Schema(description = "供应商名录管理范围名称")
    private String scopeName;

    @Schema(description = "等级: A/B/C/D")
    private String level;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
