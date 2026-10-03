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

@TableName("mes_qms_fqc_sheet_field")
@KeySequence("mes_qms_fqc_sheet_field_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcSheetFieldDO extends BaseDO {

    @TableId
    private Long id;

    private Long templateId;

    private Long sectionId;

    private String metricCode;

    private String metricName;

    private String fieldCode;

    private String fieldName;

    private String fieldRole;

    private String unit;

    private String formulaExpr;

    private BigDecimal avgMinLimit;

    private BigDecimal avgMaxLimit;

    private BigDecimal stdMinLimit;

    private BigDecimal stdMaxLimit;

    private String excelColumn;

    private Integer sort;

    private Long tenantId;
}
