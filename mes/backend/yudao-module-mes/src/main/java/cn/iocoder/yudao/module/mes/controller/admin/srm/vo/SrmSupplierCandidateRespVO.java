package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商统一候选 Response VO")
@Data
public class SrmSupplierCandidateRespVO {

    private String candidateKey;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String usingDepartment;
    private String shortName;
    private String contactPerson;
    private String contactPhone;
    private String email;
    private String address;
    private String companyNature;
    private String legalPerson;
    private BigDecimal registeredCapital;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate establishDate;

    private String mainProducts;
    private String providedProduct;
    private String materialCode;
    private String model;
    private String applicableProduct;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate importDate;

    private String materialCategory;
    private String materialGrade;
    private String paymentTerms;
    private String deliveryMethod;
    private String originPlace;
    private String originalFactoryInfo;
    private String level;
    private String remark;
    private String sourceType;
    private Boolean unregisteredSupplier;
    private String status;
    private Long scopeId;
    private String scopeCode;
    private String scopeName;
    private String viewPermission;
    private Boolean canEdit;
    private List<String> maskedFields;
    private Long sourceSurveyId;
    private String sourceSurveyNo;

}
