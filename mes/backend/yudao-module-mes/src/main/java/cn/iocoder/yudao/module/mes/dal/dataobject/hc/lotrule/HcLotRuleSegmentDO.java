package cn.iocoder.yudao.module.mes.dal.dataobject.hc.lotrule;

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
 * 批号规则 DO
 */
@TableName("mes_md_lot_rule_segment")
@KeySequence("mes_md_lot_rule_segment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcLotRuleSegmentDO extends BaseDO {

    /** 规则ID */
    private Long ruleId;

    /** 规则编码 */
    private String ruleCode;

    /** 片段编码 */
    private String segmentCode;

    /** 片段名称 */
    private String segmentName;

    /** 片段类型 */
    private String segmentType;

    /** 固定值或表达式 */
    private String segmentValue;

    /** 片段长度 */
    private Integer segmentLength;

    /** 计数器类型 */
    private String counterType;

    /** 计数维度表达式 */
    private String counterDimensionExpr;

    /** 上下文字段来源 */
    private String sourceField;

    /** 是否消耗计数器 */
    private Boolean consumeCounter;

    /** 取值策略 */
    private String valuePolicy;

    /** 排序 */
    private Integer sort;

    /** 分隔符 */
    private String delimiter;

    /** 是否启用 */
    private Boolean enabled;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
