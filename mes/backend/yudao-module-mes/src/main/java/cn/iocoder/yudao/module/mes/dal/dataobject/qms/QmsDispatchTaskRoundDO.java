package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质量任务检验轮次历史。
 */
@TableName("mes_qms_dispatch_task_round")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsDispatchTaskRoundDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer roundNo;
    private String checkType;
    private String inspectionScene;
    private Long recheckDetailId;
    private Long sourceExecutionId;
    private String sourceExecutionNo;
    private Long executionId;
    private String executionNo;
    private String roundStatus;
    private String inspectionStatus;
    private String judgment;
    private String inspectorName;
    private LocalDateTime inspectionTime;
    private String returnReason;
    private Long returnedById;
    private String returnedByName;
    private LocalDateTime returnedTime;
    private Long confirmedById;
    private String confirmedByName;
    private LocalDateTime confirmedTime;
}
