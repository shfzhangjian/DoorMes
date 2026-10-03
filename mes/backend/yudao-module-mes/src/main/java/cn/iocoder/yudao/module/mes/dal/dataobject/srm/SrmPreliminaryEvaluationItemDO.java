package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_preliminary_evaluation_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPreliminaryEvaluationItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private Long templateItemId;
    private String groupCodeSnapshot;
    private String groupNameSnapshot;
    private Integer groupSort;
    private BigDecimal groupMaxScoreSnapshot;
    private String vetoOperatorSnapshot;
    private BigDecimal vetoScoreSnapshot;
    private String indicatorCodeSnapshot;
    private String indicatorNameSnapshot;
    private Integer indicatorSort;
    private String scoringRuleSnapshot;
    private BigDecimal maxScoreSnapshot;
    private String defaultDeptNamesSnapshot;
    private String scorerCandidateUserIds;
    private String scorerCandidateUserNames;
    private Boolean attachmentRequiredSnapshot;
    private Long scorerUserId;
    private String scorerUserName;
    private String scoreStatus;
    private BigDecimal actualScore;
    private String scoringDescription;
    private LocalDateTime actualScoreTime;
    private Long tenantId;

}
