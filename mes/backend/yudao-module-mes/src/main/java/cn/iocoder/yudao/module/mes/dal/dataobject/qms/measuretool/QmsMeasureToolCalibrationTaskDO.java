package cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * QMS 量检具校准预警任务 DO
 */
@TableName("mes_qms_measure_tool_calibration_task")
@KeySequence("mes_qms_measure_tool_calibration_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolCalibrationTaskDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String taskNo;
    private Long ledgerId;
    private String toolCode;
    private String toolName;
    private Long categoryId;
    private String categoryName;
    private String usingDepartment;
    private String keeperName;
    private LocalDate dueDate;
    private Integer warningDays;
    private String warningStatus;
    private String taskStatus;
    private String sourceType;
    private LocalDateTime generatedTime;
    private LocalDateTime completedTime;
    private Long recordId;
    private String handlerName;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
