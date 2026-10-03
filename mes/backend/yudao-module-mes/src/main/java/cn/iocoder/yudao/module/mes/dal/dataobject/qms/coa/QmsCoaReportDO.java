package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_report")
@KeySequence("mes_qms_coa_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaReportDO extends BaseDO {

    @TableId
    private Long id;
    private String coaNo;
    private Integer revisionNo;
    private Long previousReportId;
    private Long rootReportId;
    private Long templateId;
    private String templateCode;
    private String templateName;
    private String templateVersion;
    private String languageType;
    private String reportTitle;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private String customerProductCode;
    private String customerProductName;
    private Long shippingNoticeId;
    private String shippingNoticeNo;
    private String salesOrderNo;
    private String erpOrderNo;
    private LocalDateTime shippingTime;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Long productModelId;
    private String productModelCode;
    private String productModelName;
    private String externalProductCode;
    private String externalProductModel;
    private String productionBatchNo;
    private String customerBatchNo;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private String productSize;
    private String shelfLife;
    private LocalDate issueDate;
    private BigDecimal shippingQuantity;
    private String shippingUnit;
    private Integer shippingItemCount;
    private String reportStatus;
    /** Flowable 流程实例；草稿阶段为空，提交后由 COA 三步流程维护。 */
    private String processInstanceId;
    private String overallResult;
    private Boolean dataCompleteFlag;
    private Integer sourceFaiCount;
    private Integer reportItemCount;
    private Integer requiredIncompleteCount;
    private Integer correctedItemCount;
    private String sourceBatchTraceJson;
    private String dataSnapshotHash;
    private String printSnapshotHtml;
    private String printSnapshotHash;
    private String pdfFileUrl;
    private String pdfFileHash;
    private String verificationCode;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewTime;
    private String reviewOpinion;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime inspectorTime;
    private Long confirmerId;
    private String confirmerName;
    private LocalDateTime confirmTime;
    private String confirmOpinion;
    private Long issuedById;
    private String issuedByName;
    private LocalDateTime issuedTime;
    private Long voidedById;
    private String voidedByName;
    private LocalDateTime voidedTime;
    private String voidReason;
    private String footerStatement;
    private String remark;
    private Long tenantId;
}
