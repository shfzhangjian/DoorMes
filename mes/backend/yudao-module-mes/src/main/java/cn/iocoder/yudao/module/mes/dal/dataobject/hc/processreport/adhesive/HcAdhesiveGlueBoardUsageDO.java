package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive;

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

@TableName("mes_sfc_adhesive_glue_board_usage")
@KeySequence("mes_sfc_adhesive_glue_board_usage_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesiveGlueBoardUsageDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long glueBoardStockId;
    private String glueBoardMaterialCode;
    private String glueBoardBatchNo;
    private String stockMeasureMode;
    private BigDecimal receiveStartPosition;
    private BigDecimal receiveLength;
    private BigDecimal receiveCount;
    private BigDecimal aqcSampleLength;
    private BigDecimal consumedLength;
    private BigDecimal consumedCount;
    private BigDecimal lossLength;
    private BigDecimal lossCount;
    private BigDecimal availableStartPosition;
    private BigDecimal availableLength;
    private BigDecimal availableCount;
    private BigDecimal returnedStartPosition;
    private BigDecimal returnedLength;
    private BigDecimal returnedCount;
    private String usageStatus;
    private String qualityStatus;
    private BigDecimal qualityLockStartPosition;
    private String qualityLockReason;
    private LocalDate recordDate;
    private Long recorderId;
    private String recorderName;
    private LocalDateTime recorderTime;
    private String extraJson;
    private Long tenantId;
}
