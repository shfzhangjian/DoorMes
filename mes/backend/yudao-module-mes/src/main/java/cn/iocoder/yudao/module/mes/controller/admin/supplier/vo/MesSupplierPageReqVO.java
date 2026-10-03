package cn.iocoder.yudao.module.mes.controller.admin.supplier.vo;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;
import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 供应商主数据分页 Request VO")
@Data
public class MesSupplierPageReqVO extends PageParam {

    @Schema(description = "供应商名称/代码")
    private String supplierInfo;

    @Schema(description = "供应商编码")
    private String supplierCode;

    @Schema(description = "供应商名称", example = "王五")
    private String supplierName;

    @Schema(description = "使用部门")
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
    private String address;

    @Schema(description = "企业性质")
    private String companyNature;

    @Schema(description = "法定代表人")
    private String legalPerson;

    @Schema(description = "注册资本(万元)")
    private BigDecimal registeredCapital;

    @Schema(description = "成立日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] establishDate;

    @Schema(description = "产地")
    private String originPlace;

    @Schema(description = "原厂信息")
    private String originalFactoryInfo;

    @Schema(description = "主营产品")
    private String mainProducts;

    @Schema(description = "提供/协作产品")
    private String providedProduct;

    @Schema(description = "型号")
    private String model;

    @Schema(description = "物料代码")
    private String materialCode;

    @Schema(description = "精确匹配物料代码，用于业务选择器")
    private String materialCodeExact;

    @Schema(description = "适用产品")
    private String applicableProduct;

    @Schema(description = "导入日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] importDate;

    @Schema(description = "历史物料类别")
    private String materialCategory;

    @Schema(description = "物料等级")
    private String materialGrade;

    @Schema(description = "结算付款条件")
    private String paymentTerms;

    @Schema(description = "交货方式")
    private String deliveryMethod;

    @Schema(description = "供应商资源状态: PENDING(考察中), QUALIFIED(合格), UNQUALIFIED(不合格), FROZEN(冻结), ELIMINATED(淘汰), EXITED(退出)", example = "QUALIFIED")
    private String status;

    @Schema(description = "供应商名录管理范围ID")
    private Long scopeId;

    @Schema(description = "供应商名录管理范围名称")
    private String scopeName;

    @Schema(description = "等级: A/B/C/D")
    private String level;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "乐观锁")
    private Integer version;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "当前用户可访问范围ID集合", hidden = true)
    private Collection<Long> accessibleScopeIds;

    @Schema(description = "是否启用供应商范围过滤", hidden = true)
    private Boolean scopeRestricted;

}
