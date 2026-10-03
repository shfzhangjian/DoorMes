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

@TableName("mes_qms_fai_sheet_cell_value")
@KeySequence("mes_qms_fai_sheet_cell_value_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFaiSheetCellValueDO extends BaseDO {

    @TableId
    private Long id;

    private Long faiId;

    private Long faiItemId;

    private Long templateId;

    private String templateVersion;

    private String sectionCode;

    private String sectionName;

    private String metricCode;

    private String metricName;

    private String fieldCode;

    private String fieldName;

    private String fieldRole;

    private Integer rowNo;

    private Integer columnNo;

    private String axisCode;

    private Integer sampleNo;

    private String displayLabel;

    private String unit;

    private String rawValue;

    private BigDecimal numericValue;

    private String textValue;

    private String valueSource;

    private String cellStatus;

    private String judgmentResult;

    private String importBatchNo;

    private Long inputUserId;

    private String inputUserName;

    private LocalDateTime inputTime;

    private Long tenantId;
}
