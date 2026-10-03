package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_srm_sample_evaluation_assignment_history")
@Data
@EqualsAndHashCode(callSuper = true)
public class SrmSampleEvaluationAssignmentHistoryDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long evaluationId;
    private Long projectId;
    private String projectCode;
    private String projectName;
    private Long initiatorUserId;
    private String initiatorUserName;
    private Long initiatorDeptId;
    private String initiatorDeptName;
    private Long inspectorUserId;
    private String inspectorUserName;
    private Long inspectorDeptId;
    private String inspectorDeptName;
    private LocalDateTime assignTime;
    private Long tenantId;

}
