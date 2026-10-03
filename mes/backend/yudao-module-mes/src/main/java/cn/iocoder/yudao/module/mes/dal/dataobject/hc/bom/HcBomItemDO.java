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
 * BOM明细 DO
 */
@TableName("mes_md_bom_item")
@KeySequence("mes_md_bom_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcBomItemDO extends BaseDO {

    /** BOM主表ID */
    private Long bomId;

    /** BOM编码 */
    private String bomCode;

    /** 行号 */
    private Integer lineNo;

    /** 子件物料ID */
    private Long componentMaterialId;

    /** 子件物料编码 */
    private String componentMaterialCode;

    /** 子件物料名称 */
    private String componentMaterialName;

    /** 组件类型 */
    private String componentType;

    /** 基础用量 */
    private BigDecimal baseQty;

    /** 损耗率 */
    private BigDecimal lossRate;

    /** 供料方式 */
    private String supplyMode;

    /** 投放工序编码 */
    private String issueOperationCode;

    /** 投放工序名称 */
    private String issueOperationName;

    /** 用料命中分组编码 */
    private String consumeGroupCode;

    /** 是否必发 */
    private Boolean requiredFlag;

    /** 命中条件JSON */
    private String conditionJson;

    /** 单位 */
    private String uom;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
