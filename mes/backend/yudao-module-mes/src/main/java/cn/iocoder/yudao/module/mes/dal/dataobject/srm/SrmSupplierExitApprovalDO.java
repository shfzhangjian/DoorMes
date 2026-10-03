package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_exit_approval")
@KeySequence("mes_srm_supplier_exit_approval_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierExitApprovalDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String exitNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String materialName;
    private String materialCode;
    private String materialModel;
    private Boolean reasonQualityDeliveryService;
    private Boolean reasonSupplierInitiated;
    private Boolean reasonBusinessAdjustment;
    private Boolean reasonOther;
    private String reasonOtherText;
    private String exitReasonDesc;
    private String replacementSupplierStatus;
    private String replacementSupplierName;
    private String stockStatus;
    private String remainingStockDesc;
    private Boolean stockDisposalReturn;
    private Boolean stockDisposalScrap;
    private Boolean stockDisposalConsume;
    private Boolean stockDisposalOther;
    private String stockDisposalOtherText;
    private Boolean contractPaymentCleared;
    private BigDecimal unpaidAmount;
    private BigDecimal uninvoicedAmount;
    private String businessRiskImpact;
    private String impactDesc;
    private Boolean materialCodeCreated;
    private LocalDate materialCodeCompleteDate;
    private Boolean supplierRosterCreated;
    private LocalDate supplierRosterCompleteDate;
    private String specReq;
    private String natureRequirement;
    private String certRequirement;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long purchaseHandlerUserId;
    private String purchaseHandlerUserName;
    private String purchaseIntakeOpinion;
    private LocalDateTime purchaseIntakeTime;
    private Long useDeptReviewerUserId;
    private String useDeptReviewerUserName;
    private String useDeptResult;
    private String useDeptOpinion;
    private LocalDateTime useDeptHandleTime;
    private Long qualityReviewerUserId;
    private String qualityReviewerUserName;
    private String qualityResult;
    private String qualityOpinion;
    private LocalDateTime qualityHandleTime;
    private Long techReviewerUserId;
    private String techReviewerUserName;
    private String techResult;
    private String techOpinion;
    private LocalDateTime techHandleTime;
    private Long purchaseReviewerUserId;
    private String purchaseReviewerUserName;
    private String purchaseResult;
    private String purchaseOpinion;
    private LocalDateTime purchaseHandleTime;
    private Long generalManagerUserId;
    private String generalManagerUserName;
    private String generalManagerResult;
    private String generalManagerOpinion;
    private LocalDateTime generalManagerHandleTime;
    private Long materialEntryUserId;
    private String materialEntryUserName;
    private LocalDate materialEntryRequiredDate;
    private String materialEntryCode;
    private String materialEntryOpinion;
    private LocalDateTime materialEntryHandleTime;
    private Long supplierRosterEntryUserId;
    private String supplierRosterEntryUserName;
    private LocalDate supplierRosterEntryRequiredDate;
    private String supplierRosterEntryCode;
    private String supplierRosterEntryOpinion;
    private LocalDateTime supplierRosterEntryHandleTime;
    private Long applicantId;
    private String applicantName;
    private String applyDept;
    private LocalDateTime applyTime;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
