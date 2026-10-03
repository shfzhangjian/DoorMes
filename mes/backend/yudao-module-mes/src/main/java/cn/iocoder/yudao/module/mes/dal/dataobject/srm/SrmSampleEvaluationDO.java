package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_sample_evaluation")
@KeySequence("mes_srm_sample_evaluation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSampleEvaluationDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String evaluationNo;
    private Long sampleRequestId;
    private String sampleRequestNo;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String materialName;
    private String materialModel;
    private BigDecimal sampleQty;
    private LocalDate evaluationDate;
    private Integer sampleSendCount;
    private String verificationTypes;
    private String inspectionTypes;
    private String applyDept;
    private LocalDate applyDate;
    private Long approvedByUserId;
    private String approvedByName;
    private Long initiatorUserId;
    private String initiatorUserName;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long assignedInspectorUserId;
    private String assignedInspectorUserName;
    private Long assignedInspectorDeptId;
    private String assignedInspectorDeptName;
    private LocalDateTime assignedTime;
    private Long reporterUserId;
    private String reporterUserName;
    private LocalDateTime reportTime;
    private Long confirmUserId;
    private String confirmUserName;
    private String confirmResult;
    private String confirmOpinion;
    private LocalDateTime confirmTime;
    private String initiatorDecisionOpinion;
    private Long finalApproverUserId;
    private String finalApproverUserName;
    private String finalApproverOpinion;
    private LocalDateTime finalApproverHandleTime;
    private String archiveOpinion;
    private LocalDateTime archiveTime;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
