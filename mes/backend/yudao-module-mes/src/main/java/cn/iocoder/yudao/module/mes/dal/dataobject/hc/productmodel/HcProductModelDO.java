package cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_md_product_model")
@KeySequence("mes_md_product_model_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProductModelDO extends BaseDO {

    @TableId
    private Long id;

    private String modelCode;
    private String modelName;
    private String modelAlias;
    private String modelLevel;
    private Long parentModelId;
    @TableField(exist = false)
    private String parentModelCode;
    @TableField(exist = false)
    private String parentModelName;
    private Long modelRuleId;
    private String modelRuleCode;
    private String modelRuleName;
    private String productType;
    private String prodType;
    private String prodTypeName;
    private Long categoryId;
    private String categoryCode;
    private String categoryName;
    private String sizeSpec;
    private String sizeName;
    private Integer retentionPeriodValue;
    private String retentionPeriodUnit;
    private Long recipeId;
    private String recipeCode;
    private String recipeName;
    private Long defaultRouteId;
    private String defaultRouteCode;
    private String defaultRouteName;
    private String segmentSnapshotJson;
    private String sourceType;
    private String status;
    private Boolean referencedFlag;
    private String remark;
    private Long tenantId;

}
