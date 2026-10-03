package cn.iocoder.yudao.module.mes.controller.admin.srm.vo;

import cn.iocoder.yudao.module.mes.framework.jackson.MesLocalDateTimeDeserializer;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

@Schema(description = "管理后台 - SRM供方退出审批新增/修改 Request VO")
@Data
public class SrmSupplierExitApprovalSaveReqVO {

    public interface Update {
    }

    @Schema(description = "主键ID")
    @NotNull(groups = Update.class, message = "主键ID不能为空")
    private Long id;

    @Schema(description = "退出审批单号（新增保存时由服务端生成）")
    private String exitNo;

    private Long supplierId;
    private String supplierCode;

    @Schema(description = "供应商名称（由合格供方带出）")
    private String supplierName;

    @Schema(description = "物料名称（由合格供方物料带出）")
    private String materialName;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料型号")
    private String materialModel;

    @Schema(description = "质量/交期/服务持续性不满足要求")
    private Boolean reasonQualityDeliveryService;

    @Schema(description = "供应商主动退出")
    private Boolean reasonSupplierInitiated;

    @Schema(description = "公司业务调整")
    private Boolean reasonBusinessAdjustment;

    @Schema(description = "其他退出原因")
    private Boolean reasonOther;

    @Schema(description = "其他退出原因说明")
    private String reasonOtherText;

    @Schema(description = "退出原因详细说明")
    private String exitReasonDesc;

    @Schema(description = "替代供应商状态：CONFIRMED已确定、UNCONFIRMED未确定")
    private String replacementSupplierStatus;

    @Schema(description = "替代供应商名称")
    private String replacementSupplierName;

    @Schema(description = "库存状态：REMAINING剩余库存、NONE无剩余库存")
    private String stockStatus;

    @Schema(description = "剩余库存说明")
    private String remainingStockDesc;

    @Schema(description = "库存处理方式-需退换货")
    private Boolean stockDisposalReturn;

    @Schema(description = "库存处理方式-报废处理")
    private Boolean stockDisposalScrap;

    @Schema(description = "库存处理方式-消耗完毕")
    private Boolean stockDisposalConsume;

    @Schema(description = "库存处理方式-其他")
    private Boolean stockDisposalOther;

    @Schema(description = "库存处理方式其他说明")
    private String stockDisposalOtherText;

    @Schema(description = "票款全部结清")
    private Boolean contractPaymentCleared;

    @Schema(description = "尚有未结货款")
    private BigDecimal unpaidAmount;

    @Schema(description = "尚有未开发票")
    private BigDecimal uninvoicedAmount;

    @Schema(description = "其他业务/生产风险影响")
    private String businessRiskImpact;

    @Schema(description = "退出影响评估详细说明")
    private String impactDesc;

    @Schema(description = "合格供方物料清单移除完成")
    private Boolean materialCodeCreated;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "合格供方物料清单移除完成日期")
    private LocalDate materialCodeCompleteDate;

    @Schema(description = "库存/账务处理完成")
    private Boolean supplierRosterCreated;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "库存/账务处理完成日期")
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

    @Schema(description = "乐观锁（更新时必填，新增由服务端初始化）")
    @NotNull(groups = Update.class, message = "乐观锁不能为空")
    private Integer version;

}
