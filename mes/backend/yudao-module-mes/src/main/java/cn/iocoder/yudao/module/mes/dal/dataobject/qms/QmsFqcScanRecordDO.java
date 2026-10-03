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

@TableName("mes_qms_fqc_scan_record")
@KeySequence("mes_qms_fqc_scan_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcScanRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String scanCode;

    private String scanTargetType;

    private String scanScene;

    private String matchResult;

    private Long matchedFqcId;

    private String matchedFqcNo;

    private Long matchedFqcItemId;

    private Long matchedSubmissionDetailId;

    private String matchedProductionBatchNo;

    private String matchedStepCode;

    private Integer candidateCount;

    private String candidateIds;

    private String blockedReason;

    private String openTarget;

    private Long scanUserId;

    private String scanUserName;

    private LocalDateTime scanTime;

    private String clientType;

    private String terminalCode;

    private Long tenantId;
}
