package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 质量任务自有样本结果。
 */
@TableName("mes_qms_dispatch_task_sample_result")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsDispatchTaskSampleResultDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long taskItemId;
    private Integer roundNo;
    private Integer sampleSeq;
    private String pieceNo;
    private BigDecimal measuredValue;
    private String qualitativeValue;
    private String result;
    private Long inspectorId;
    private String inspectorName;
    private LocalDateTime inspectionTime;
}
