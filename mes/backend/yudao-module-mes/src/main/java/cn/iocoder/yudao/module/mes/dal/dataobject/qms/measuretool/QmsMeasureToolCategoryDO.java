package cn.iocoder.yudao.module.mes.dal.dataobject.qms.measuretool;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
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
 * QMS 量检具分类 DO
 */
@TableName("mes_qms_measure_tool_category")
@KeySequence("mes_qms_measure_tool_category_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsMeasureToolCategoryDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long parentId;
    private String categoryCode;
    private String categoryName;
    private String description;
    private Integer status;
    private Integer sort;
    private Long tenantId;

}
