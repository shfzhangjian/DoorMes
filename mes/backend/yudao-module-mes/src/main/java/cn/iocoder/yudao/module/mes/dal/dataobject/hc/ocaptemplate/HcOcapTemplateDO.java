package cn.iocoder.yudao.module.mes.dal.dataobject.hc.ocaptemplate;

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
 * OCAP模板 DO
 */
@TableName("mes_ocap_template")
@KeySequence("mes_ocap_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcOcapTemplateDO extends BaseDO {

    /** OCAP编码 */
    private String ocapCode;

    /** OCAP名称 */
    private String ocapName;

    /** 业务工序 */
    private String businessStage;

    /** 触发项目编码 */
    private String triggerItemCode;

    /** 触发条件 */
    private String triggerCondition;

    /** 处置步骤 */
    private String actionSteps;

    /** 状态 */
    private String status;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}