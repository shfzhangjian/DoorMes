package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.grinding;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_grinding_stock_ledger")
@KeySequence("mes_sfc_grinding_stock_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcGrindingStockLedgerDO extends BaseDO {

    @TableId
    private Long id;

    private Long grindingReportId;
    private Long secondDetailId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String ledgerNo;
    private String ledgerStatus;
    private String sourceType;
    private String sourceRowUid;
    private String motherBatchNo;
    private String segmentNo;
    private BigDecimal startMeter;
    private BigDecimal length;
    private BigDecimal processLength;
    private BigDecimal lossLength;
    private BigDecimal outputLength;
    private BigDecimal napSampleLength;
    private String machineCode;
    private LocalDateTime outputTime;
    private String selfCheck;
    private BigDecimal thickness;
    private BigDecimal width;
    private String defectCode;
    private LocalDateTime storageTime;
    private Integer printCount;
    private String remark;
    private Long tenantId;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    private String sourceDetailType;
    private Long sourceDetailId;
    private String availableStatus;
    private Long lockedByPlanId;
    private Long lockedByOperationId;
    private Long lockedByReportId;
    private LocalDateTime confirmedTime;
}
