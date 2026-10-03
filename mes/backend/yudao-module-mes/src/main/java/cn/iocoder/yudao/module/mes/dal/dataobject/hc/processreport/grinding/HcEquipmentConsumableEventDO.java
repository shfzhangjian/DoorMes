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

@TableName("mes_sfc_equipment_consumable_event")
@KeySequence("mes_sfc_equipment_consumable_event_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcEquipmentConsumableEventDO extends BaseDO {

    @TableId
    private Long id;

    private Long stateId;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String processCode;
    private String processName;
    private String consumableType;
    private String eventType;
    private String recordSource;
    private Long guideClothRecordId;
    private Long planId;
    private String planNo;
    private String motherBatchNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String grindingStage;
    private Long grindingDetailId;
    private String bizType;
    private Long bizId;
    private String recordGroupNo;
    private String productModelCode;
    private String productMaterialCode;
    private String productBatchNo;
    private String petModel;
    private String petBatchNo;
    private BigDecimal wetInputKg;
    private BigDecimal wetOutputMeter;
    private Long stockId;
    private String beforeMaterialCode;
    private String beforeBatchNo;
    private String afterMaterialCode;
    private String afterBatchNo;
    private Integer beforeUseCount;
    private Integer afterUseCount;
    private Integer changeUseCount;
    private BigDecimal onlineQuantity;
    private BigDecimal offlineQuantity;
    private Integer finalUseCount;
    private BigDecimal beforeUsedLength;
    private BigDecimal afterUsedLength;
    private BigDecimal changeLength;
    private String replaceReason;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime eventTime;
    private String remark;
    private Long tenantId;
}
