package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_quarter_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceQuarterItemDO extends BaseDO {

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
    private String vetoResultSnapshot;
    private String indicatorCodeSnapshot;
    private String indicatorNameSnapshot;
    private Integer indicatorSort;
    private String scoringRuleSnapshot;
    private BigDecimal maxScoreSnapshot;
    private String defaultDeptNamesSnapshot;
    private Boolean attachmentRequiredSnapshot;
    private String indicatorTypeSnapshot;
    private BigDecimal targetValueSnapshot;
    private String targetUnitSnapshot;
    private BigDecimal redlineScoreSnapshot;
    private String scorerCandidateUserIds;
    private String scorerCandidateUserNames;
    private Long scorerUserId;
    private String scorerUserName;
    private String reporterCandidateUserIds;
    private String reporterCandidateUserNames;
    private Long reporterUserId;
    private String reporterUserName;
    private Long calcRuleId;
    private BigDecimal calcActualValue;
    private BigDecimal calcScore;
    private BigDecimal manualScore;
    private BigDecimal finalScore;
    private String scoreStatus;
    private String scoringDescription;
    private LocalDateTime actualScoreTime;
    private String dataStatus;
    private Long tenantId;

}
