package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

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

@TableName("mes_qms_ipqc_sample")
@KeySequence("mes_qms_ipqc_sample_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsIpqcSampleDO extends BaseDO {

    @TableId
    private Long id;

    private Long ipqcId;
    private Long ipqcItemId;
    private String ipqcNo;
    private Integer sampleSeq;
    private String samplePosition;
    private BigDecimal measuredValue;
    private String qualitativeValue;
    private String sampleResult;
    private String defectCode;
    private String defectName;
    private String remark;
    private Long tenantId;
}
