package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName(value = "mes_qms_fqc_submission_detail", autoResultMap = true)
@KeySequence("mes_qms_fqc_submission_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcSubmissionDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long fqcId;
    private String fqcNo;
    private Long cutRoundInspectionTaskId;
    private String cutRoundInspectionTaskNo;
    private Long cutRoundInspectionDetailId;
    private Long cutRoundReportId;
    private Integer seqNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String sizeRule;
    private String productionBatchNo;
    private String parentProductionBatchNo;
    /** 过程风险标记：NONE/ADHESIVE2_NG/CUT_ROUND_NG/BOTH_NG。 */
    private String qualityRiskFlag;
    /** 裁切报检任务固化的过程风险快照 JSON。 */
    private String qualityRiskSnapshotJson;
    /** 片级检验照片 URL 数组。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> photoUrls;
    private String rowJudgment;
    private String defectCode;
    private String defectName;
    private String ngReason;
    private Long inspectorId;
    private String inspectorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime inspectionTime;
    private String remark;
    private Boolean recheckDetailFlag;
    private Long tenantId;
}
