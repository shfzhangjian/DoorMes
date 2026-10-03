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

@TableName("mes_qms_fqc_sheet_stat_result")
@KeySequence("mes_qms_fqc_sheet_stat_result_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcSheetStatResultDO extends BaseDO {

    @TableId
    private Long id;

    private Long fqcId;

    private Long fqcItemId;

    private Long templateId;

    private String sectionCode;

    private String metricCode;

    private String statFieldCode;

    private Integer sampleCount;

    private BigDecimal avgValue;

    private BigDecimal stdValue;

    private BigDecimal minValue;

    private BigDecimal maxValue;

    private BigDecimal avgMinLimit;

    private BigDecimal avgMaxLimit;

    private BigDecimal stdMinLimit;

    private BigDecimal stdMaxLimit;

    private String judgmentResult;

    private LocalDateTime calculateTime;

    private Long tenantId;
}
