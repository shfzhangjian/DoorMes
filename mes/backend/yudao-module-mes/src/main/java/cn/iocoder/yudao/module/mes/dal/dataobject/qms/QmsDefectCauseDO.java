package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_defect_cause")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsDefectCauseDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 缺陷项ID */
    private Long defectCodeId;

    /** 缺陷代码 */
    private String defectCode;

    /** 缺陷名称 */
    private String defectName;

    /** 原因代码 */
    private String reasonCode;

    /** 发生原因名称 */
    private String reasonName;

    /** 原因分类 */
    private String reasonType;

    /** 原因说明 */
    private String reasonDesc;

    /** 排序号 */
    private Integer sort;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 租户ID */
    private Long tenantId;
}
