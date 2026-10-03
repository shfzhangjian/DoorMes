package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.cutround;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_cut_round_inspection_task")
@KeySequence("mes_sfc_cut_round_inspection_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcCutRoundInspectionTaskDO extends BaseDO {

    @TableId
    private Long id;

    private String taskNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String operationCode;
    private String operationName;
    private String reportProcess;
    private String receiveLocation;
    private LocalDate reportDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime reportTime;
    private String reporterName;
    private String receiverName;
    private String priorityLevel;
    private LocalDate expectedFinishDate;
    private String taskStatus;
    private Integer detailCount;
    private String remark;
    private Long tenantId;
}
