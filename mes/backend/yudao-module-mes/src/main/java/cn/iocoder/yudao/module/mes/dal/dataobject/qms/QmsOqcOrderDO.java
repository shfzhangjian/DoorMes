package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_oqc_order")
@KeySequence("mes_qms_oqc_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsOqcOrderDO extends BaseDO {

    @TableId
    private Long id;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String productType;
    private String oqcNo;
    private Long shippingNoticeId;
    private Long shippingNoticeItemId;
    private String shippingNo;
    private String noticeNo;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String modelCode;
    private String productSize;
    private String batchNo;
    private String customerBatchNo;
    private BigDecimal shippingQty;
    private BigDecimal shippingBoxQty;
    private BigDecimal shippingPieceQty;
    private String unitCode;
    private String unitName;
    private String aqlStandard;
    private Integer sampleQty;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    private String status;
    private String judgment;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime inspectionTime;
    private Long qaInspectorId;
    private String qaInspectorName;
    private LocalDateTime qaTime;
    private String releaseResult;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime releaseTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditNotifyTime;
    private String relatedNcrNo;
    private String ncrStatus;
    private String entryMode;
    private String entryLayout;
    private Integer entryProgress;
    private Integer requiredItemCount;
    private Integer completedItemCount;
    private Integer abnormalItemCount;
    private LocalDateTime lastSaveTime;
    private LocalDateTime lastCalculateTime;
    private Boolean sheetLocked;
    private String remark;
    private Boolean rejectFlag;
    private Boolean recheckFlag;
    private Long recheckGroupId;
    private Integer recheckRoundNo;
    private Long rejectPrevInspectionId;
    private String rejectPrevInspectionNo;
    private Long rejectNextInspectionId;
    private String rejectNextInspectionNo;
    private Long rejectRootInspectionId;
    private String rejectRootInspectionNo;
    private String rejectRecheckResult;
    private LocalDateTime rejectRecheckTime;
    private String rejectReason;
    private LocalDateTime rejectTime;
    private Long rejectUserId;
    private String rejectUserName;
    private Long tenantId;
}
