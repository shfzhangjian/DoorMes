package cn.iocoder.yudao.module.mes.dal.dataobject.hc.recipe;

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

/**
 * 配方 DO
 */
@TableName("mes_md_recipe")
@KeySequence("mes_md_recipe_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcRecipeDO extends BaseDO {

    /** 配方编码 */
    private String recipeCode;

    /** 配方名称 */
    private String recipeName;

    /** 配方类型 */
    private String recipeType;

    /** 型号编号 */
    private String modelCode;

    /** 累计使用次数 */
    private Long usageCount;

    /** 历史兼容字段，可为空 */
    private Long productMaterialId;

    /** 历史兼容字段，可为空 */
    private String productMaterialCode;

    /** 历史兼容字段，可为空 */
    private String productMaterialName;

    /** 版本号 */
    private String versionNo;

    /** 标准固含 */
    private BigDecimal solidContentStd;

    /** 标准粘度 */
    private BigDecimal viscosityStd;

    /** 理论得率 */
    private BigDecimal yieldRate;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expireDate;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;
}
