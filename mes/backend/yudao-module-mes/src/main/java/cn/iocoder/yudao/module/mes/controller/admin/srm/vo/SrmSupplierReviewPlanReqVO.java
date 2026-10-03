package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM供方年度评审计划 Request VO")
public class SrmSupplierReviewPlanReqVO {

    @Data
    public static class AddSupplierLine {

        @Schema(description = "计划年份", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "计划年份不能为空")
        private Integer planYear;

        @Schema(description = "供应商ID")
        private Long supplierId;

        @Schema(description = "供应商代码")
        private String supplierCode;

        @Schema(description = "供应商名称")
        private String supplierName;

        @Schema(description = "联系人")
        private String contactPerson;

        @Schema(description = "物料代码")
        private String materialCode;

        @Schema(description = "物料名称")
        private String materialName;

        @Schema(description = "型号")
        private String model;

        @Schema(description = "适用产品")
        private String applicableProduct;

        @Schema(description = "提供/协作产品")
        private String providedProduct;

    }

    @Data
    public static class UpdateLineContact {

        @Schema(description = "计划行ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "计划行ID不能为空")
        private Long id;

        @Schema(description = "联系人")
        private String contactPerson;

        @Schema(description = "备注")
        private String remark;

    }

    @Data
    public static class DeleteAnnualPlans {

        @Schema(description = "年度评审日历行ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "年度评审日历行不能为空")
        private Long lineId;

        @Schema(description = "计划年份", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "计划年份不能为空")
        private Integer planYear;

    }

    @Data
    public static class MonthSave {

        @Schema(description = "月计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "月计划ID不能为空")
        private Long id;

        @Schema(description = "具体计划评审月份计划说明")
        private String planDesc;

        @Schema(description = "牵头执行人ID")
        private Long leadUserId;

        @Schema(description = "牵头执行人")
        private String leadUserName;

        @Schema(description = "相关人员")
        private List<UserSnapshot> relatedUsers;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "审核日期")
        private LocalDate auditDate;

        @Schema(description = "审核类别：认证审核/年度审核/不定期审核")
        private String auditCategory;

        @Schema(description = "审核说明")
        private String auditDesc;

        @Schema(description = "审核附件（现场考察资料快照说明）")
        private String auditAttachment;

        @Schema(description = "审批人ID")
        private Long approverUserId;

        @Schema(description = "审批人")
        private String approverUserName;

        @Schema(description = "审批意见")
        private String approvalOpinion;

        @Schema(description = "备注说明")
        private String remark;

    }

    @Data
    public static class StatusAdjust {

        @Schema(description = "月计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "月计划ID不能为空")
        private Long id;

        @Schema(description = "执行状态", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "执行状态不能为空")
        private String executionStatus;

        @Schema(description = "状态备注", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "状态备注不能为空")
        private String statusRemark;

        @Schema(description = "更新说明")
        private String updateDescription;

    }

    @Data
    public static class SiteInspectionSave {

        @Schema(description = "月计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "月计划ID不能为空")
        private Long id;

        @Schema(description = "审核类别：认证审核/年度审核/不定期审核")
        private String auditCategory;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "审定日期")
        private LocalDate auditDate;

        @Schema(description = "审核说明")
        private String auditDesc;

        @Schema(description = "审核附件（现场考察资料快照说明）")
        private String auditAttachment;

    }

    @Data
    public static class ReplyCreate {

        @Schema(description = "月计划ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "月计划ID不能为空")
        private Long monthPlanId;

        @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        @Schema(description = "评审日期", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "评审日期不能为空")
        private LocalDate reviewDate;

        @Schema(description = "经办人")
        private String recorderUserName;

        @Schema(description = "评审结果说明", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "评审结果说明不能为空")
        private String reviewResult;

        @Schema(description = "备注")
        private String remark;

    }

    @Data
    public static class UserSnapshot {

        private Long id;
        private String name;
        private String deptName;

    }

    @Data
    public static class MonthQuery {

        @NotNull(message = "计划年份不能为空")
        private Integer planYear;

        @Min(value = 1, message = "月份必须在1到12之间")
        @Max(value = 12, message = "月份必须在1到12之间")
        private Integer planMonth;

    }

}
