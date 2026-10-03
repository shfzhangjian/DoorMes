package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport.adhesive;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_adhesive_intermediate_detail")
@KeySequence("mes_sfc_adhesive_intermediate_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcAdhesiveIntermediateDetailDO extends BaseDO {

    @TableId
    private Long id;

    private Long intermediateRecordId;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private BigDecimal lengthMark;
    private BigDecimal leftThickness;
    private BigDecimal rightThickness;
    private String remark;
    private Integer sortNo;
    private Long tenantId;
}
