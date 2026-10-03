package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.packaging;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 发货需求单变更留痕。
 *
 * 保存变更前后业务快照及本次下游回退范围，避免用当前需求覆盖已经发生的业务事实后无法追溯。
 */
@TableName("mes_inv_fg_shipping_notice_change_log")
@KeySequence("mes_inv_fg_shipping_notice_change_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFgShippingNoticeChangeLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long noticeId;
    private String noticeNo;
    private Integer changeVersion;
    private String beforeStatus;
    private String afterStatus;
    private String changeReason;
    private String changedFields;
    private String downstreamSync;
    private String beforeSnapshotJson;
    private String afterSnapshotJson;
    private Long tenantId;
}
