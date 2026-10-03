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
@TableName("mes_md_lot_rule")
@KeySequence("mes_md_lot_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcLotRuleDO extends BaseDO {

    /** 规则编码 */
    private String ruleCode;

    /** 规则名称 */
    private String ruleName;

    /** 业务对象类型 */
    private String bizType;

    /** 产品类别编码 */
    private String productCategoryCode;

    /** 生产类型 */
    private String prodType;

    /** 型号匹配方式 */
    private String modelMatchMode;

    /** 型号匹配值 */
    private String modelMatchValue;

    /** 规则优先级 */
    private Integer priority;

    /** 规则版本号 */
    private Integer versionNo;

    /** 流水组编码；为空时按规则编码独立流水 */
    private String counterGroupCode;

    /** 规则高级配置JSON */
    private String ruleConfigJson;

    /** 默认生成触发点 */
    private String generationTrigger;

    /** 默认生成粒度 */
    private String generationScope;

    /** 批次数量模型 */
    private String batchCardinality;

    /** 多批次展示策略 */
    private String displayPolicy;

    /** 规则模式 */
    private String ruleMode;

    /** 前缀 */
    private String prefix;

    /** 日期格式 */
    private String dateFormat;

    /** 年份编码模式 */
    private String yearCodeMode;

    /** 月份编码模式 */
    private String monthCodeMode;

    /** 流水长度 */
    private Integer seqLength;

    /** 流水起始值 */
    private Integer seqStart;

    /** 流水步长 */
    private Integer seqStep;

    /** 重置周期 */
    private String resetCycle;

    /** 抽检段规则 */
    private String sampleSegmentRule;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 是否允许预览 */
    private Boolean allowPreview;

    /** 是否允许解析 */
    private Boolean allowParse;

    /** 是否允许人工修改已预览号码 */
    private Boolean allowManualOverride;

    /** 主键ID */
    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

}
