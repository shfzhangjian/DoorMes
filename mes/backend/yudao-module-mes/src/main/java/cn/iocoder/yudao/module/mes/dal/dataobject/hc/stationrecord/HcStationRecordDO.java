package cn.iocoder.yudao.module.mes.dal.dataobject.hc.stationrecord;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_station_record")
@KeySequence("mes_sfc_station_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcStationRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationName;
    private Long formId;
    private String formCode;
    private String formName;
    private String triggerTimingCode;
    private String triggerTimingName;
    private String docStatus;
    private String resultStatus;
    private String inspectionResult;
    private Long equipmentId;
    private String equipmentName;
    private Long mixerEquipmentId;
    private String mixerEquipmentCode;
    private String mixerEquipmentName;
    private Long foamingEquipmentId;
    private String foamingEquipmentCode;
    private String foamingEquipmentName;
    private String recordUserName;
    private LocalDateTime recordTime;
    private String confirmUserName;
    private LocalDateTime confirmTime;
    private String headerDataJson;
    private Long sourceProcessFormRecordId;
    private String formRemark;
    private String confirmRemark;
    private Long tenantId;

    private String recordScope;
    private LocalDate recordDate;
    private String bizType;
    private Long bizId;
    private String equipmentCode;
    private String operationCode;
    private Long workCenterId;
    private String workCenterCode;
    private String workCenterName;
}
