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
 * QMS 量检具新增申请 DO
 */
@TableName("mes_qms_measure_tool_apply")
@KeySequence("mes_qms_measure_tool_apply_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolApplyDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String applyNo;
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
    private String usingDepartment;
    private String keeperName;
    private String storageLocation;
    private String applyDepartment;
    private String applicantName;
    private String applyReason;
    private String status;
    private String approvalOpinion;
    private String approvedBy;
    private LocalDateTime approvedTime;
    private Long ledgerId;
    private String assignedToolCode;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
