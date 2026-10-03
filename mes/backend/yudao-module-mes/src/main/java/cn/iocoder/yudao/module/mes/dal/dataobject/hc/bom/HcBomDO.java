package cn.iocoder.yudao.module.mes.dal.dataobject.hc.bom;

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
 * BOM DO
 */
@TableName("mes_md_bom")
@KeySequence("mes_md_bom_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcBomDO extends BaseDO {

    /** BOM编码 */
    private String bomCode;

    /** BOM名称 */
    private String bomName;

    /** 产出物料ID */
    private Long productMaterialId;

    /** 产出物料编码 */
    private String productMaterialCode;

    /** 产出物料名称 */
    private String productMaterialName;

    /** 产品型号ID */
    private Long productModelId;

    /** 产品型号编码 */
    private String productModelCode;

    /** 产品型号名称 */
    private String productModelName;

    /** 成品规格/尺寸 */
    private String productSpec;

    /** 关联配方ID */
    private Long recipeId;

    /** 关联配方编码 */
    private String recipeCode;

    /** 关联配方名称 */
    private String recipeName;

    /** 关联工艺路线ID */
    private Long routeId;

    /** 关联工艺路线编码 */
    private String routeCode;

    /** 版本号 */
    private String versionNo;

    /** BOM类型 */
    private String bomType;

    /** 标准良率 */
    private BigDecimal yieldRate;

    /** 压槽连续作业加检数 */
    private Integer pressSlotContinuousCheckCount;

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
