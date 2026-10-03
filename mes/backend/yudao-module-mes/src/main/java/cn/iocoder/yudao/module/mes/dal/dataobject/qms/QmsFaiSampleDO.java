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

@TableName("mes_qms_fai_sample")
@KeySequence("mes_qms_fai_sample_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFaiSampleDO extends BaseDO {

    @TableId
    private Long id;

    private Long sheetCellId;

    private Long faiId;

    private Long faiItemId;

    private String faiNo;

    private String stepCode;

    private String metricCode;

    private String metricGroupCode;

    private String inputComponent;

    private String sampleRole;

    private Integer sampleSeq;

    private String samplePosition;

    private String rawValuesJson;

    private BigDecimal resultValue;

    private BigDecimal densityValue;

    private BigDecimal compressionRate;

    private BigDecimal compressionElasticityRate;

    private BigDecimal measuredValue;

    private String qualitativeValue;

    private String sampleResult;

    private String sheetSectionCode;

    private String sheetMetricCode;

    private Integer sampleGroupNo;

    private String sampleAxis;

    private Integer sampleColumnNo;

    private String importBatchNo;

    private String valueSource;

    private java.time.LocalDateTime inputTime;

    private String defectCode;

    private String defectName;

    private String remark;

    private Boolean recheckItemFlag;

    private Long tenantId;
}
