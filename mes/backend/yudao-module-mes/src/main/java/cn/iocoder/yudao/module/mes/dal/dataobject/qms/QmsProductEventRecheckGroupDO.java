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

@TableName("mes_qms_product_event_recheck_group")
@KeySequence("mes_qms_product_event_recheck_group_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsProductEventRecheckGroupDO extends BaseDO {

    @TableId
    private Long id;

    private String sourceType;

    private Long dispatchTaskId;

    private Long rootInspectionId;

    private String rootInspectionNo;

    private Long latestInspectionId;

    private String latestInspectionNo;

    private Integer latestRoundNo;

    private String latestStatus;

    private String latestJudgment;

    private String chainStatus;

    private Integer totalRecheckCount;

    private LocalDateTime latestResultTime;

    private String lastRejectReason;

    private Long lastRejectUserId;

    private String lastRejectUserName;

    private LocalDateTime lastRejectTime;

    private Long tenantId;
}
