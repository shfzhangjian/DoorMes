package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 质量任务中心操作历史。
 */
@TableName("mes_qms_dispatch_task_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsDispatchTaskLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String actionType;
    private String actionDesc;
    private String beforeStatus;
    private String afterStatus;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime actionTime;
    private String details;
}
