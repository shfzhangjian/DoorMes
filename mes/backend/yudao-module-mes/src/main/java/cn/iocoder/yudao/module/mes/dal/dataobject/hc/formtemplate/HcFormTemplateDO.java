package cn.iocoder.yudao.module.mes.dal.dataobject.hc.formtemplate;

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
 * 表单模板 DO
 */
@TableName("mes_form_template")
@KeySequence("mes_form_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFormTemplateDO extends BaseDO {

    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 模板类型 */
    private String templateType;

    /** 业务工序 */
    private String businessStage;

    /** 表单样式 */
    private String formStyle;

    /** 状态 */
    private String status;

    /** 备注 */
    private String remark;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}