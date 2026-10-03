package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround;

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

@TableName("mes_md_cut_round_spare_record")
@KeySequence("mes_md_cut_round_spare_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcCutRoundSpareRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long ledgerId;
    private Long consumeId;
    private BigDecimal replaceQuantity;
    private String requestKey;
    private String requestHash;
    private Long spareId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
    private String spareType;
    private String eventType;
    private String recordSource;
    private Long planId;
    private String planNo;
    private String motherBatchNo;
    private String productionBatchNo;
    private String modelCode;
    /** 研发登记垫型快照；历史记录为空时仅允许按型号主数据识别。 */
    private String padType;
    private String cutSizeMm;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String bizType;
    private Long bizId;
    private String recordGroupNo;
    private String beforeMaterialCode;
    private String beforeBatchNo;
    private String afterMaterialCode;
    private String afterBatchNo;
    private Integer beforeUseCount;
    private Integer afterUseCount;
    private Integer changeUseCount;
    private BigDecimal cutInputPcs;
    private BigDecimal cutOutputPcs;
    private Integer feltUseDays;
    private BigDecimal beforeUsedLength;
    private BigDecimal afterUsedLength;
    private BigDecimal changeLength;
    private BigDecimal beforeAvailableQuantity;
    private BigDecimal afterAvailableQuantity;
    private BigDecimal changeQuantity;
    private BigDecimal onlineQuantity;
    private BigDecimal offlineQuantity;
    private Integer finalUseCount;
    private String replaceReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime eventTime;
    private String remark;
    private Long tenantId;
}
