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

@TableName("mes_qms_product_event_recheck_detail")
@KeySequence("mes_qms_product_event_recheck_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsProductEventRecheckDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long groupId;

    private String sourceType;

    private Long dispatchTaskRoundId;

    private Integer roundNo;

    private Long inspectionId;

    private String inspectionNo;

    private Long prevInspectionId;

    private String prevInspectionNo;

    private String inspectionStatus;

    private String inspectionJudgment;

    private String rejectReason;

    private Long rejectUserId;

    private String rejectUserName;

    private LocalDateTime rejectTime;

    private LocalDateTime resultTime;

    private Long tenantId;
}
