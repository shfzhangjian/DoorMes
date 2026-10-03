package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive2;

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

@TableName("mes_sfc_adhesive2_intermediate_record")
@KeySequence("mes_sfc_adhesive2_intermediate_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesive2IntermediateRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long planId;
    private String planNo;
    private Long planOperationId;
    private LocalDateTime recordDate;
    private String modelCode;
    private String materialCode;
    private String batchNo;
    private String frontSliceNo;
    private String middleSliceNo;
    private String endSliceNo;
    private BigDecimal inputQty;
    private BigDecimal outputQty;
    private String glueBoardModel;
    private String recorderName;
    private String confirmerName;
    private String recordStatus;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
