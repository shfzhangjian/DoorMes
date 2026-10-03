package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

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

@TableName("mes_qms_oqc_sample")
@KeySequence("mes_qms_oqc_sample_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsOqcSampleDO extends BaseDO {

    @TableId
    private Long id;

    private Long oqcId;
    private Long oqcItemId;
    private String oqcNo;
    private String sampleRole;
    private Integer sampleSeq;
    private String samplePosition;
    private String rawValuesJson;
    private BigDecimal resultValue;
    private BigDecimal measuredValue;
    private String qualitativeValue;
    private String sampleResult;
    private Integer sampleGroupNo;
    private String valueSource;
    private LocalDateTime inputTime;
    private String remark;
    private Boolean recheckItemFlag;
    private Long tenantId;
}
