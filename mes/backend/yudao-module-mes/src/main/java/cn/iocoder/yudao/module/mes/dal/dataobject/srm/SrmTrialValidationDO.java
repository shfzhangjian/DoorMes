package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_trial_validation")
@KeySequence("mes_srm_trial_validation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmTrialValidationDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String trialNo;
    private Long sourceSampleEvaluationId;
    private String sourceSampleEvaluationNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String materialModel;
    private String materialBatchNo;
    private BigDecimal quantity;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long initiatorUserId;
    private String initiatorUserName;
    private LocalDateTime noticeTime;
    private Long trialExecutionUserId;
    private String trialExecutionUserName;
    private LocalDateTime trialExecutionTime;
    private String trialExecutionOpinion;
    private Long productionCompleteUserId;
    private String productionCompleteUserName;
    private LocalDateTime productionCompleteTime;
    private String productionCompleteOpinion;
    private Long archiveUserId;
    private String archiveUserName;
    private LocalDateTime archiveTime;
    private String archiveOpinion;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
