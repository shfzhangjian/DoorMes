package cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * QMS 量检具台账 DO
 */
@TableName("mes_qms_measure_tool_ledger")
@KeySequence("mes_qms_measure_tool_ledger_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolLedgerDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String toolCode;
    private String bodyNo;
    private String toolName;
    private Long categoryId;
    private String categoryName;
    private String model;
    private String specification;
    private String accuracy;
    private String measureRange;
    private String manufacturer;
    private LocalDate purchaseDate;
    private Integer calibrationCycleMonths;
    private Integer warningDays;
    private String calibrationType;
    private LocalDate lastCalibrationDate;
    private LocalDate nextCalibrationDate;
    private String calibrationMethod;
    private String calibrationOrg;
    private String calibrator;
    private String calibrationResult;
    private String certificateNo;
    private String calibrationReport;
    private String usingDepartment;
    private String maintainerName;
    /** 兼容既有校准任务、记录快照字段，值与保养人保持一致。 */
    private String keeperName;
    private String storageLocation;
    private String status;
    private String calibrationStatus;
    private Integer msaEnabled;
    private Integer msaCycleMonths;
    private Integer msaWarningDays;
    private LocalDate lastMsaDate;
    private LocalDate nextMsaDate;
    private String msaStatus;
    private String msaResult;
    private String msaReport;
    private String msaAnalyst;
    private String responsiblePerson;
    /** 是否对外开放：0 否、1 是。 */
    private Integer externalOpen;
    private String remark;

    @TableField(exist = false)
    private String displayStatus;
    /** 基于下次校准日期实时计算，独立于量检具使用状态。 */
    @TableField(exist = false)
    private Boolean calibrationOverdue;
    @TableField(exist = false)
    private Integer calibrationMissedCount;
    @TableField(exist = false)
    private LocalDate recentCalibrationMissedDate;
    @TableField(exist = false)
    private Integer msaMissedCount;
    @TableField(exist = false)
    private LocalDate recentMsaMissedDate;

    @Version
    private Integer version;

    private Long tenantId;

}
