package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_correction_log")
@KeySequence("mes_qms_coa_correction_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaCorrectionLogDO extends BaseDO {

    @TableId
    private Long id;
    private Long reportId;
    private String coaNo;
    private Integer revisionNo;
    private Long reportItemId;
    private String metricCode;
    private String itemName;
    private String sourceValue;
    private String beforeActualValue;
    private String afterActualValue;
    private String beforeDisplayValue;
    private String afterDisplayValue;
    private String beforeResult;
    private String afterResult;
    private String correctionReason;
    private Long correctorId;
    private String correctorName;
    private LocalDateTime correctionTime;
    private Long sourceFaiId;
    private String sourceFaiNo;
    private Long sourceFaiItemId;
    private Long tenantId;
}
