package cn.iocoder.yudao.module.mes.dal.dataobject.hc.modelrule;

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
 * 型号编码规则 DO
 */
@TableName("mes_md_model_rule_item")
@KeySequence("mes_md_model_rule_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcModelRuleItemDO extends BaseDO {

    /** 规则主表ID */
    private Long ruleId;

    /** 规则编码 */
    private String ruleCode;

    /** 字段编码 */
    private String itemCode;

    /** 字段名称 */
    private String itemName;

    /** 解析方式 */
    private String parseType;

    /** 段长度 */
    private Integer segmentLength;

    /** 固定字符值 */
    private String fixedValue;

    /** 数据来源类型 */
    private String dataSourceType;

    /** 排序 */
    private Integer sort;

    /** 是否必选段 */
    private Boolean requiredFlag;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}