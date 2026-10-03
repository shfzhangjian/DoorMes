package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_cmp_warpage_slice_stat")
@KeySequence("mes_qms_cmp_warpage_slice_stat_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsCmpWarpageSliceStatDO extends BaseDO {

    @TableId
    private Long id;

    private LocalDate recordDate;
    private String modelCode;
    private String parentBatchNo;
    private String segmentSliceNo;
    private String productionSliceNo;
    private String customerSliceNo;
    private String customerCode;
    private String customerName;
    private String shippingNoticeNo;
    private BigDecimal warpageValueMm;
    private String inspectionResult;
    private Boolean manualOverride;
    private String sourceActualValue;
    private String sourceItemResult;
    private Long sourceFqcId;
    private String sourceFqcNo;
    private Long sourceSubmissionDetailId;
    private Long sourceFqcItemId;
    private Long shippingFqcDetailId;
    private LocalDateTime lastSyncTime;
    private String syncMessage;
    private String remark;
    private Long tenantId;
}
