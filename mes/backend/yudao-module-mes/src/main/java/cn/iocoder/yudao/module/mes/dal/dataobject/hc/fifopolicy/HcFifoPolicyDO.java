package cn.iocoder.yudao.module.mes.dal.dataobject.hc.fifopolicy;

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
 * 先进先出策略 DO
 */
@TableName("mes_inv_fifo_policy")
@KeySequence("mes_inv_fifo_policy_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcFifoPolicyDO extends BaseDO {

    /** 策略编码 */
    private String policyCode;

    /** 策略名称 */
    private String policyName;

    /** 仓库编码 */
    private String warehouseCode;

    /** 仓库名称 */
    private String warehouseName;

    /** 货主编码 */
    private String ownerCode;

    /** 适用范围 */
    private String matchScope;

    /** 出库规则 */
    private String issueRule;

    /** 优先字段 */
    private String priorityFields;

    /** 状态 */
    private String status;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}