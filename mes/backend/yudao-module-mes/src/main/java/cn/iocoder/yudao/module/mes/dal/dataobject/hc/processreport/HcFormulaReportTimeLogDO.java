package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_operation_report_time_log")
@KeySequence("mes_sfc_operation_report_time_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFormulaReportTimeLogDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long operationReportId;
    private Long planId;
    private Long planOperationId;
    private String planNo;
    private String sourceMenuCode;
    private LocalDate beforeReportDate;
    private LocalDate afterReportDate;
    private LocalDateTime beforeStartTime;
    private LocalDateTime afterStartTime;
    private LocalDateTime beforeEndTime;
    private LocalDateTime afterEndTime;
    private Integer beforeReportMinutes;
    private Integer afterReportMinutes;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime changeTime;

}
