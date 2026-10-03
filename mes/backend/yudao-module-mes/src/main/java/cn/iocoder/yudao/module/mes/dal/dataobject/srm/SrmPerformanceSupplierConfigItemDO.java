package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_performance_supplier_config_item")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceSupplierConfigItemDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long configId;
    private Long templateId;
    private Long templateVersionId;
    private Long templateItemId;
    private String groupCodeSnapshot;
    private String groupNameSnapshot;
    private Integer groupSort;
    private BigDecimal groupMaxScoreSnapshot;
    private String indicatorCodeSnapshot;
    private String indicatorNameSnapshot;
    private Integer indicatorSort;
    private String scoringRuleSnapshot;
    private BigDecimal maxScoreSnapshot;
    private String defaultDeptNames;
    private String indicatorType;
    private BigDecimal targetValue;
    private String targetUnit;
    private BigDecimal redlineScore;
    private String scorerCandidateUserIds;
    private String scorerCandidateUserNames;
    private Long scorerUserId;
    private String scorerUserName;
    private String reporterCandidateUserIds;
    private String reporterCandidateUserNames;
    private Long reporterUserId;
    private String reporterUserName;
    private Long calcRuleId;
    private String remark;
    private Long tenantId;

}
