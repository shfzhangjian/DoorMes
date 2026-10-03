package cn.iocoder.yudao.module.mes.dal.dataobject.qms.coa;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("mes_qms_coa_template_item")
@KeySequence("mes_qms_coa_template_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class QmsCoaTemplateItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long templateId;
    private String templateCode;
    private String templateVersion;
    private String metricCode;
    private String itemGroup;
    private String itemNameCn;
    private String itemNameEn;
    private Long sourceProcessId;
    private String sourceProcessCode;
    private String sourceProcessName;
    private Long sourceStandardId;
    private String sourceStandardNo;
    private String sourceStandardVersion;
    private Long sourceStandardItemId;
    private String sourceInspectionItem;
    private String sourceItemType;
    private String valueSourceType;
    private String sourceStandardApplyType;
    private String valueStrategy;
    private String fixedValue;
    private String specSource;
    private String specText;
    private String coaSpecText;
    private BigDecimal targetValue;
    private BigDecimal lowerLimit;
    private BigDecimal upperLimit;
    private String unit;
    private String inspectionMethod;
    private Integer decimalPlaces;
    private Boolean requiredFlag;
    private Boolean allowCorrectionFlag;
    private Boolean coaDisplayFlag;
    private Integer sortNo;
    private String remark;
    private Long tenantId;
}
