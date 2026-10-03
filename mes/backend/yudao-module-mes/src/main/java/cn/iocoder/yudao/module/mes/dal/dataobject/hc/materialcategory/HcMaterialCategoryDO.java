package cn.iocoder.yudao.module.mes.dal.dataobject.hc.materialcategory;

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
 * 物料分类 DO
 */
@TableName("mes_md_material_category")
@KeySequence("mes_md_material_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcMaterialCategoryDO extends BaseDO {

    /** 父级分类ID */
    private Long parentId;

    /** 分类编码 */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 分类编号路径 */
    private String categoryCodePath;

    /** 分类名称路径 */
    private String categoryNamePath;

    /** 兼容旧字段的分类路径 */
    private String categoryPath;

    /** 排序号 */
    private Integer sort;

    /** 备注 */
    private String remark;

    /** 状态 */
    private Integer status;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}