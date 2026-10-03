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

/**
 * 包装段 COA 送检样片占用记录。
 *
 * <p>一条待包装来源片只能作为 COA 样片送检一次；即使 FAI 被取消、驳回或判定 NG，
 * 该片也不回流待包装队列，复检需要选择新的来源片。</p>
 */
@TableName("mes_qms_packaging_coa_sample_claim")
@KeySequence("mes_qms_packaging_coa_sample_claim_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsPackagingCoaSampleClaimDO extends BaseDO {

    @TableId
    private Long id;

    private Long faiId;

    private String faiNo;

    private String segmentBatchNo;

    /** 来源类型：CUT_ROUND_REPORT / MANUAL_HISTORY。 */
    private String sourceType;

    private Long sourceRecordId;

    private String sampleBatchNo;

    private String materialCode;

    private String materialName;

    private String modelCode;

    private String fqcResult;

    private LocalDateTime claimTime;

    private Long tenantId;
}
