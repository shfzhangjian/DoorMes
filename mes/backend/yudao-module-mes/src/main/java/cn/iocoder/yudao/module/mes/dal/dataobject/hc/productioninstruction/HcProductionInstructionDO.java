package cn.iocoder.yudao.module.mes.dal.dataobject.hc.productioninstruction;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_pp_production_instruction")
@KeySequence("mes_pp_production_instruction_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProductionInstructionDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;

    private String instructionNo;
    private String instructionBatchNo;
    private Long parentInstructionId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private Long processId;
    private String processCode;
    private String processName;
    private String operationCode;
    private String operationName;
    private String batchNo;
    private String productionBatchNo;
    private String segmentBatchNo;
    private String instructionType;
    private String scopeType;
    private String instructionContent;
    private String beforeMaterialCode;
    private String targetMaterialCode;
    private String beforeModelCode;
    private String targetModelCode;
    private Integer targetQty;
    private Integer completedQty;
    private String executeStatus;
    private Boolean autoRestoreFlag;
    private Long executeUserId;
    private String executeUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime executeStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime executeEndTime;

    private Long issuerId;
    private String issuerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime issuedTime;

    private Long confirmerId;
    private String confirmerName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime confirmTime;

    private Long revokedBy;
    private String revokedByName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime revokedTime;
    private String revokeReason;

    private String status;
    private String remark;

}
