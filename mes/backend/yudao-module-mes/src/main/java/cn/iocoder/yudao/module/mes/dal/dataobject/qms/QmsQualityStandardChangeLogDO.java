package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_quality_standard_change_log")
@KeySequence("mes_qms_quality_standard_change_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsQualityStandardChangeLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long standardId;

    private String applyType;

    private String changeScope;

    private String itemKey;

    private String itemLabel;

    private String fieldName;

    private String fieldLabel;

    private String beforeValue;

    private String afterValue;

    private Long operatorId;

    private String operatorName;

    private LocalDateTime changeTime;

    private Long tenantId;
}
