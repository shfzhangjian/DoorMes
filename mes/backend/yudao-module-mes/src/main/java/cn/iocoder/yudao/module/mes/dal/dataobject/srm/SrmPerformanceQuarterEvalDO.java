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

@TableName("mes_srm_performance_quarter_eval")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmPerformanceQuarterEvalDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String evaluationNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String supplierSourceType;
    private Integer evalYear;
    private Integer evalQuarter;
    private Long templateId;
    private Long templateVersionId;
    private String templateCodeSnapshot;
    private String templateNameSnapshot;
    private String templateVersionSnapshot;
    private BigDecimal totalScoreBaseline;
    private BigDecimal qualificationScoreSnapshot;
    private String status;
    private Long initiatorId;
    private String initiatorName;
    private LocalDateTime generatedTime;
    private LocalDateTime autoCalculatedTime;
    private LocalDateTime sendTime;
    private LocalDateTime allScoredTime;
    private LocalDateTime calculatedTime;
    private LocalDateTime signStartTime;
    private LocalDateTime archivedTime;
    private BigDecimal autoScore;
    private BigDecimal manualScore;
    private BigDecimal totalScore;
    private String evalGrade;
    private Boolean redlineTriggered;
    private String redlineDescription;
    private String finalOpinion;
    private String remark;
    @Version
    private Integer version;
    private Long tenantId;

}
