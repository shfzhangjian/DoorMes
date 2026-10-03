package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_cut_round_inspection_detail")
@KeySequence("mes_sfc_cut_round_inspection_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcCutRoundInspectionDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long taskId;
    private Long cutRoundReportId;
    private Integer seqNo;
    private String parentProductionBatchNo;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String sizeRule;
    private String productionBatchNo;
    /** 过程风险标记：NONE/ADHESIVE2_NG/CUT_ROUND_NG/BOTH_NG。 */
    private String qualityRiskFlag;
    /** 生成报检任务时固化的过程风险快照 JSON。 */
    private String qualityRiskSnapshotJson;
    private Long fqcOrderId;
    private String fqcNo;
    private String fqcStatus;
    private String fqcJudgment;
    private String inspectionResult;
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;
    private String remark;
    private Long tenantId;
}
