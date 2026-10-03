package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_preliminary_evaluation")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPreliminaryEvaluationDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String evaluationNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long templateId;
    private Long templateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionSnapshot;
    private BigDecimal totalScoreBaseline;
    private BigDecimal qualificationScoreSnapshot;
    private String status;
    private String processInstanceId;
    private Long initiatorId;
    private String initiatorName;
    private LocalDateTime sendTime;
    private LocalDateTime allScoredTime;
    private LocalDateTime calculatedTime;
    private BigDecimal totalScore;
    private String autoDecision;
    private Boolean vetoTriggered;
    private String vetoDescription;
    private String finalDecision;
    private String finalDescription;
    private Long generalManagerUserId;
    private String generalManagerUserName;
    private String generalManagerOpinion;
    private LocalDateTime generalManagerHandleTime;
    private Long publisherId;
    private String publisherName;
    private LocalDateTime publishTime;
    private String remark;
    @Version
    private Integer version;
    private Long tenantId;

}
