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

@TableName("mes_qms_environment_record")
@KeySequence("mes_qms_environment_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsEnvironmentRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String workshopCode;
    private String workshopName;
    private String recordMonth;
    private LocalDate recordDate;
    private BigDecimal temperatureValue;
    private BigDecimal humidityValue;
    private String temperatureStatus;
    private String humidityStatus;
    private String overallStatus;
    private Long recorderId;
    private String recorderUsername;
    private String recorderName;
    private LocalDateTime recordTime;
    private Long confirmerId;
    private String confirmerUsername;
    private String confirmerName;
    private LocalDateTime confirmTime;
    private String confirmStatus;
    private String remark;
    private Long tenantId;
}
