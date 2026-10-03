package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.slitting;

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

@TableName("mes_sfc_slitting_slice_record")
@KeySequence("mes_sfc_slitting_slice_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcSlittingSliceRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long sourceAdhesiveReportId;
    private Long sourceStockId;
    private Long sourcePlanLockId;
    private String sourceStockBatchNo;
    private BigDecimal sourceLockQty;
    private BigDecimal sourceConsumeQty;
    private String sourceConsumeTxnNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime sourceConsumeTime;
    private String sourceBatchNo;
    private String sourceProductionBatchNo;
    private BigDecimal sourceLength;
    private BigDecimal startPosition;
    private BigDecimal endPosition;
    private BigDecimal sliceLength;
    private String cutMode;
    private String sliceSerialNo;
    private Long outputStockId;
    private String outputStockPostStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime outputStockPostTime;
    private String outputStockPostMessage;
    private Integer sliceIndex;
    private String sizeCode;
    private String sizeName;
    private String printStatus;
    private Integer printCount;
    private LocalDateTime lastPrintTime;
    private String scanStatus;
    private LocalDateTime scanTime;
    private Long scannerId;
    private String scannerName;
    private String visualResultJson;
    private String selfCheck;
    private String remark;
    private Long tenantId;
}
