package cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe;

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

/**
 * 配方 DO
 */
@TableName("mes_md_recipe_item")
@KeySequence("mes_md_recipe_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcRecipeItemDO extends BaseDO {

    /** 配方ID */
    private Long recipeId;

    /** 配方编码 */
    private String recipeCode;

    /** 行号 */
    private Integer lineNo;

    /** 投入物料ID */
    private Long materialId;

    /** 投入物料编码 */
    private String materialCode;

    /** 投入物料名称 */
    private String materialName;

    /** 投入类型 */
    private String materialType;

    /** 理论用量 */
    private BigDecimal theoryQty;

    /** 损耗率 */
    private BigDecimal lossRate;

    /** 单位 */
    private String uom;

    /** 是否关键控制料 */
    private Boolean keyControlFlag;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}