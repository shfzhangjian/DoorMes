package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processparam;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 批次工序工艺参数记录 DO。
 */
@TableName("mes_sfc_process_param_record")
@KeySequence("mes_sfc_process_param_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProcessParamRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String batchNo;
    private String processCode;
    private String processName;
    private String paramCode;
    private String paramName;
    private String paramValue;
    private BigDecimal paramValueNum;
    private String uom;
    private LocalDateTime recordTime;
    private Long recorderId;
    private String recorderName;
    private String sourceType;
    private String sourceTable;
    private Long sourceId;
    private Long sourceDetailId;
    private String sourceFormCode;
    private String sourceFormName;
    private String remark;

}
