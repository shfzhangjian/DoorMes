package cn.iocoder.yudao.module.mes.dal.dataobject.hc.researchtask;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_rd_research_task")
@KeySequence("mes_rd_research_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcResearchTaskDO extends BaseDO {

    @TableId
    private Long id;

    private String taskNo;
    private String rdModelCode;
    private String displayModelCode;
    private String taskStatus;
    private LocalDate researchDate;
    private Long issueUserId;
    private String issueUserName;
    private LocalDateTime issueTime;
    private String productClassCode;
    private String productClassName;
    private String batchTypeCode;
    private String baseFormulaCode;
    private String baseFormulaName;
    private String wetProcessCode;
    private String wetProcessName;
    private String grindingProcessCode;
    private String grindingProcessName;
    private String postProcessCode;
    private String postProcessName;
    private Integer reuseSeq;
    private Long routeId;
    private String routeCode;
    private String routeName;
    private String routeVersion;
    private String batchRuleCode;
    private BigDecimal targetQty;
    private String targetUom;
    private String taskPurpose;
    private Integer formulaUsageCount;
    private Integer wetUsageCount;
    private Integer grindingUsageCount;
    private Integer postUsageCount;
    private Integer combinationUsageCount;
    private Long archivedModelId;
    private String archivedModelCode;
    private LocalDateTime archivedTime;
    private Long planId;
    private String planNo;
    private String snapshotJson;
    private String remark;
    private Long tenantId;

}
