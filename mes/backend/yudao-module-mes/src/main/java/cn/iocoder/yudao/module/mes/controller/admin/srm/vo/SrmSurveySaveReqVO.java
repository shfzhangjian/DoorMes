package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM供应商基本情况调查新增/修改 Request VO")
@Data
public class SrmSurveySaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "调查表单号；服务端保存时生成，更新时不可修改")
    private String surveyNo;

    private Long supplierId;
    private String supplierCode;

    @Schema(description = "供应商名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "供应商名称不能为空")
    private String supplierName;

    @Schema(description = "未入库供应商标记", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "未入库供应商标记不能为空")
    private Boolean unregisteredSupplier;

    @Schema(description = "供应商来源类型：统一供应商主表")
    private String supplierSourceType;

    @Schema(description = "调查日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "调查日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate surveyDate;

    private Long surveyLeaderId;
    private String surveyLeaderName;
    private String conclusion;

    @Schema(description = "确认在未找到同名供应商时初始化为考察中供应商资源")
    private Boolean confirmInitializePending;

    private String nature;
    private String registerAddress;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
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
    @Schema(description = "登记人ID；服务端按当前登录人生成")
    private Long applicantId;

    @Schema(description = "登记人；服务端按当前登录人生成")
    private String applicantName;

    @Schema(description = "调查表登记时间；服务端生成，更新时保持原值")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
    private LocalDateTime applyTime;

    private String extraJson;
    private String remark;

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

    @Valid
    private List<Review> reviews;

    @Data
    public static class Review {

        private Long id;
        private Long surveyId;

        @NotEmpty(message = "评审项目不能为空")
        private String reviewProject;

        private String reviewDept;
        private String reviewResult;
        private String reviewOpinion;
        private Long reviewerId;
        private String reviewerName;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonDeserialize(using = MesLocalDateTimeDeserializer.class)
        private LocalDateTime reviewTime;

        private Integer sort;

    }

}
