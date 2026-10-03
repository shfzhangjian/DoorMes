package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_evaluation_template_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmEvaluationTemplateItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long versionId;
    private String groupCode;
    private String groupName;
    private Integer groupSort;
    private BigDecimal groupMaxScore;
    private String vetoOperator;
    private BigDecimal vetoScore;
    private String vetoResult;
    private String indicatorCode;
    private String indicatorName;
    private Integer indicatorSort;
    private String scoringRule;
    private BigDecimal maxScore;
    private String defaultDeptNames;
    private Long defaultScorerUserId;
    private String defaultScorerUserName;
    private String defaultScorerUserIds;
    private String defaultScorerUserNames;
    private Boolean attachmentRequired;
    private Long tenantId;

}
