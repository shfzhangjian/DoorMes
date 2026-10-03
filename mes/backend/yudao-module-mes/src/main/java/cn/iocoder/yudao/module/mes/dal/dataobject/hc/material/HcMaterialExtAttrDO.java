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
@TableName("mes_md_material_ext_attr")
@KeySequence("mes_md_material_ext_attr_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcMaterialExtAttrDO extends BaseDO {

    /** 物料ID */
    private Long materialId;

    /** 物料编码 */
    private String materialCode;

    /** 属性编码 */
    private String attrCode;

    /** 属性名称 */
    private String attrName;

    /** 属性值 */
    private String attrValue;

    /** 值类型 */
    private String valueType;

    /** 排序号 */
    private Integer sort;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}