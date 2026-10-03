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

@TableName("mes_qms_fqc_return_record")
@KeySequence("mes_qms_fqc_return_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcReturnRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long fqcId;

    private String fqcNo;

    private String returnStepCode;

    private String returnReason;

    private Long returnUserId;

    private String returnUserName;

    private LocalDateTime returnTime;

    private String beforeStatus;

    private String afterStatus;

    private Long tenantId;
}
