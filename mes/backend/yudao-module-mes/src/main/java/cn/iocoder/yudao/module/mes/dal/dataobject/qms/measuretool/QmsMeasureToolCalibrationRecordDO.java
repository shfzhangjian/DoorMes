package cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * QMS 量检具校准记录台账 DO
 */
@TableName("mes_qms_measure_tool_calibration_record")
@KeySequence("mes_qms_measure_tool_calibration_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolCalibrationRecordDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String recordNo;
    private Long taskId;
    private Long ledgerId;
    private String toolCode;
    private String toolName;
    private Long categoryId;
    private String categoryName;
    private String usingDepartment;
    private String keeperName;
    private LocalDate calibrationDate;
    private String calibrationType;
    private String calibrationMethod;
    private String calibrationOrg;
    private String calibrator;
    private String calibrationResult;
    private String certificateNo;
    private String certificateAttachment;
    private LocalDate validUntil;
    private LocalDate nextCalibrationDate;
    private BigDecimal cost;
    private String sourceType;
    private Integer missedCount;
    private Integer overdueFlag;
    private LocalDate overdueDueDate;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
