package cn.iocoder.yudao.module.mes.dal.dataobject.hc.material;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 物料主数据 DO
 */
@TableName("mes_md_material")
@KeySequence("mes_md_material_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcMaterialDO extends BaseDO {

    /** 物料编码 */
    private String materialCode;

    /** 物料名称 */
    private String materialName;

    /** 物料简称 */
    private String materialShortName;

    /** 物料类型 */
    private Integer materialType;

    /** 物料分类ID */
    private Long materialCategoryId;

    /** 物料分类名称 */
    private String materialCategoryName;

    /** 产品层级 */
    private String productLevel;

    /** 规格型号描述 */
    private String specModel;

    /** 物料编号规则ID */
    private Long materialCodeRuleId;

    /** 型号编码规则ID */
    private Long modelCodeRuleId;

    /** 产品型号ID */
    private Long productModelId;

    /** 型号编码 */
    private String modelCode;

    /** 产品型号名称 */
    private String productModelName;

    /** 型号段值快照 JSON */
    private String modelSegmentsJson;

    /** 基础单位ID */
    private Long baseUnitId;

    /** 基础单位符号 */
    private String baseUnitCode;

    /** 基础单位名称 */
    private String baseUnitName;

    /** 基础计量单位 */
    private String baseUom;

    /** 库存单位ID */
    private Long stockUnitId;

    /** 库存单位符号 */
    private String stockUnitCode;

    /** 库存单位名称 */
    private String stockUnitName;

    /** 库存单位 */
    private String stockUom;

    /** 生产单位ID */
    private Long produceUnitId;

    /** 生产单位符号 */
    private String produceUnitCode;

    /** 生产单位名称 */
    private String produceUnitName;

    /** 生产单位 */
    private String produceUom;

    /** 是否批次管理 */
    private Boolean batchManaged;

    /** 是否条码管理 */
    private Boolean barcodeManaged;

    /** 默认工艺路线ID */
    private Long defaultRouteId;

    /** 默认工艺路线编码 */
    private String defaultRouteCode;

    private String defaultRouteName;

    /** 默认配方ID */
    private Long defaultRecipeId;

    private String defaultRecipeCode;

    private String defaultRecipeName;

    private Long defaultBomId;

    private String defaultBomCode;

    private String defaultBomName;

    /** 质检模式 */
    private String qualityControlMode;

    /** 物料状态 */
    private Integer materialStatus;

    /** MES产品 */
    private Boolean mesSelectVisible;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
