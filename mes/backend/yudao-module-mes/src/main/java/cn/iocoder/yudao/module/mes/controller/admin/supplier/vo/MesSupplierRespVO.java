package cn.iocoder.yudao.module.mes.controller.admin.supplier.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import cn.idev.excel.annotation.format.DateTimeFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import cn.idev.excel.annotation.*;

@Schema(description = "管理后台 - 供应商主数据 Response VO")
@Data
@ExcelIgnoreUnannotated
public class MesSupplierRespVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "23625")
    private Long id;

    @Schema(description = "供应商编码", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty(value = "供应商代码", index = 2)
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "王五")
    @ExcelProperty(value = "供应商名称", index = 1)
    private String supplierName;

    @Schema(description = "使用部门")
    @ExcelProperty(value = "使用部门", index = 0)
    private String usingDepartment;

    @Schema(description = "简称", example = "张三")
    private String shortName;

    @Schema(description = "联系人")
    @ExcelProperty(value = "联系人", index = 3)
    private String contactPerson;

    @Schema(description = "联系电话")
    @ExcelProperty(value = "联系电话", index = 4)
    private String contactPhone;

    @Schema(description = "电子邮箱")
    private String email;

    @Schema(description = "详细地址")
    @ExcelProperty(value = "供应商地址", index = 5)
    private String address;

    @Schema(description = "企业性质")
    @ExcelProperty(value = "企业性质", index = 6)
    private String companyNature;

    @Schema(description = "法定代表人")
    private String legalPerson;

    @Schema(description = "注册资本(万元)")
    private BigDecimal registeredCapital;

    @Schema(description = "成立日期", example = "2020-01-01")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate establishDate;

    @Schema(description = "产地")
    @ExcelProperty(value = "产地", index = 7)
    private String originPlace;

    @Schema(description = "原厂信息")
    @ExcelProperty(value = "原厂信息", index = 8)
    private String originalFactoryInfo;

    @Schema(description = "主营产品")
    private String mainProducts;

    @Schema(description = "提供/协作产品")
    @ExcelProperty(value = "提供/协作产品", index = 9)
    private String providedProduct;

    @Schema(description = "型号")
    @ExcelProperty(value = "型号", index = 10)
    private String model;

    @Schema(description = "物料代码")
    @ExcelProperty(value = "物料代码", index = 11)
    private String materialCode;

    @Schema(description = "适用产品")
    @ExcelProperty(value = "适用产品", index = 12)
    private String applicableProduct;

    @Schema(description = "导入日期", example = "2026-07-24")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @ExcelProperty(value = "导入日期", index = 13)
    @DateTimeFormat("yyyy-MM-dd")
    private LocalDate importDate;

    @Schema(description = "历史物料类别")
    private String materialCategory;

    @Schema(description = "物料等级")
    @ExcelProperty(value = "物料等级", index = 14)
    private String materialGrade;

    @Schema(description = "结算付款条件")
    private String paymentTerms;

    @Schema(description = "交货方式")
    private String deliveryMethod;

    @Schema(description = "供应商资源状态: PENDING(考察中), QUALIFIED(合格), UNQUALIFIED(不合格), FROZEN(冻结), ELIMINATED(淘汰), EXITED(退出)", requiredMode = Schema.RequiredMode.REQUIRED, example = "QUALIFIED")
    @ExcelProperty(value = "供应商状态", index = 15)
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

    @Schema(description = "乐观锁", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer version;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "当前用户对该供应商的查看权限：MASKED/FULL/EDIT")
    private String viewPermission;

    @Schema(description = "当前用户是否可编辑该供应商")
    private Boolean canEdit;

    @Schema(description = "已脱敏字段集合")
    private List<String> maskedFields;

}
