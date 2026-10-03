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

@TableName("mes_pp_production_instruction_changeover_piece")
@KeySequence("mes_pp_production_instruction_changeover_piece_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProductionInstructionChangeoverPieceDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long instructionId;
    private String instructionNo;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String segmentBatchNo;
    private String pieceNo;
    private Long adhesive2ReportId;
    private String actualModelCode;
    private String actualGlueBoardModel;
    private String actualGlueBoardBatchNo;
    private Long scanUserId;
    private String scanUserName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime scanTime;
    private String status;
    private String remark;

}
