package cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** 量检具 MSA 分析历史快照。 */
@TableName("mes_qms_measure_tool_msa_record")
@KeySequence("mes_qms_measure_tool_msa_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolMsaRecordDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;
    private Long ledgerId;
    private String toolCode;
    private String toolName;
    private Long categoryId;
    private String categoryName;
    private String usingDepartment;
    private String maintainerName;
    private LocalDate msaDate;
    private LocalDate nextMsaDate;
    private String msaResult;
    private String msaReport;
    private String analyst;
    private Integer missedCount;
    private Integer overdueFlag;
    private LocalDate overdueDueDate;
    private String sourceType;
    private String remark;
    private Integer version;
    private Long tenantId;
}
