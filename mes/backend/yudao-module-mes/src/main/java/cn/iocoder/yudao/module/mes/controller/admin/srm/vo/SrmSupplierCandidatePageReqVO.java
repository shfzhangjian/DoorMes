package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - SRM供应商统一候选分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmSupplierCandidatePageReqVO extends PageParam {

    @Schema(description = "供应商名称/代码模糊检索")
    private String supplierInfo;

    @Schema(description = "供应商代码")
    private String supplierCode;

    @Schema(description = "供应商名称")
    private String supplierName;

    @Schema(description = "使用部门")
    private String usingDepartment;

    @Schema(description = "主营产品")
    private String mainProducts;

    @Schema(description = "物料代码")
    private String materialCode;

    @Schema(description = "产品型号")
    private String model;

    @Schema(description = "适用产品")
    private String applicableProduct;

    @Schema(description = "物料等级")
    private String materialGrade;

    @Schema(description = "评定级别")
    private String level;

    @Schema(description = "企业性质")
    private String companyNature;

    @Schema(description = "供应商名录管理范围ID")
    private Long scopeId;

    @Schema(description = "资源状态：PENDING考察中、QUALIFIED合格、UNQUALIFIED不合格、FROZEN冻结、ELIMINATED淘汰、EXITED退出")
    private String status;

    @Schema(description = "历史兼容参数；供应商候选统一读取 mes_supplier，考察中请使用 status=PENDING")
    private String sourceType;

    @Schema(description = "是否按供应商编号和名称去重")
    private Boolean distinctSupplier;

}
