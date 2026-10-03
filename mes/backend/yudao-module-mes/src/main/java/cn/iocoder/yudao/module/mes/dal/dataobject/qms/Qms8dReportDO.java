package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
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
import org.springframework.format.annotation.DateTimeFormat;

@TableName("mes_qms_8d_report")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Qms8dReportDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String reportNo;
    private String sourceType;
    private Long sourceId;
    private String sourceNo;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate issueDate;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate targetDate;

    private String currentStep;
    private String status;
    private String currentNodeCode;
    private String currentNodeName;
    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private Long initiatorUserId;
    private String initiatorUserName;
    private Long initiatorDeptId;
    private String initiatorDeptName;
    private String problemDesc;
    private String containmentAction;
    private Long containmentOwnerId;
    private String containmentOwnerName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate containmentDate;

    private String rootCauseCategory;
    private String rootCauseAnalysis;
    private String correctiveAction;
    private Long actionOwnerId;
    private String actionOwnerName;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate actionPlanDate;

    private String validationResult;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate validationDate;

    private Boolean updateSop;
    private Boolean updateFmea;
    private Boolean updateControlPlan;
    private String standardizeDesc;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime closeTime;

    private Long closeUserId;
    private String closeUserName;

    private Long tenantId;
}
