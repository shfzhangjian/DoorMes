package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

@Schema(description = "管理后台 - SRM供应商基本情况调查 Response VO")
@Data
public class SrmSurveyRespVO {

    private Long id;
    private String surveyNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Boolean unregisteredSupplier;
    private String supplierSourceType;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate surveyDate;

    private Long surveyLeaderId;
    private String surveyLeaderName;
    private String conclusion;
    private String nature;
    private String registerAddress;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate establishDate;

    private String legalPerson;
    private BigDecimal registeredCapital;
    private String contactName;
    private String contactPhone;
    private BigDecimal area;
    private Integer employeeCount;
    private String industryRank;
    private BigDecimal rdRatio;
    private BigDecimal qaRatio;
    private String annualCapacity;
    private String mainBrand;
    private BigDecimal coopYears;
    private String capitalScale;
    private String agentDelivery;
    private BigDecimal score;
    private String status;
    private Long applicantId;
    private String applicantName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime applyTime;

    private String extraJson;
    private String remark;
    private Integer version;
    private List<Review> reviews;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    @Data
    public static class Review {

        private Long id;
        private Long surveyId;
        private String reviewProject;
        private String reviewDept;
        private String reviewResult;
        private String reviewOpinion;
        private Long reviewerId;
        private String reviewerName;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime reviewTime;

        private Integer sort;

    }

}
