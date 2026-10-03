package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_ipqc_item")
@KeySequence("mes_qms_ipqc_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsIpqcItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long ipqcId;
    private String ipqcNo;
    private Long standardItemId;
    private String category;
    private String inspectionItem;
    private String itemType;
    private BigDecimal targetValue;
    private String standardDesc;
    private String inspectionMethod;
    private String testFrequencyJudgement;
    private String testTool;
    private Integer sampleSize;
    private BigDecimal minValueLimit;
    private BigDecimal maxValueLimit;
    private BigDecimal maxValue;
    private BigDecimal minValue;
    private BigDecimal averageValue;
    private String itemResult;
    @TableField("is_spc")
    private Boolean isSpc;
    private Integer sort;
    private Long tenantId;
}
