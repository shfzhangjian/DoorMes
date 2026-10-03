package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_iqc_sample")
@KeySequence("mes_qms_iqc_sample_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsIqcSampleDO extends BaseDO {

    @TableId
    private Long id;

    private Long iqcId;

    private Long iqcItemId;

    private String iqcNo;

    private Integer sampleSeq;

    private String sampleBarcode;

    private String rawValuesJson;

    private BigDecimal resultValue;

    private BigDecimal measuredValue;

    private String qualitativeValue;

    private LocalDate dateValue;

    private LocalDate evaluationDate;

    private String sampleResult;

    private String defectCode;

    private String defectName;

    private String remark;

    private Boolean recheckItemFlag;

    private Long tenantId;
}
