package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.framework.mybatis.core.type.StringListTypeHandler;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName(value = "mes_qms_defect_code", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsDefectCodeDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 父级ID */
    private Long parentId;

    /** 缺陷代码 */
    private String code;

    /** 缺陷名称 */
    private String name;

    /** 节点类型 */
    private String type;

    /** 严重等级 */
    private String level;

    /** 参考缺陷图片 */
    @TableField(typeHandler = StringListTypeHandler.class)
    private List<String> referencePicUrls;

    /** 排序号 */
    private Integer sort;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 租户ID */
    private Long tenantId;
}
