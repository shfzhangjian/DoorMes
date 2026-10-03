// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/batch/BatchRuleDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.batch;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

/**
 * MES批次号生成规则 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_batch_rule")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchRuleDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联物料ID
     */
    private Long productId;

    /**
     * 冗余:产品编码
     */
    private String productCode;

    /**
     * 冗余:产品名称
     */
    private String productName;

    /**
     * 规则编码
     */
    private String ruleCode;

    /**
     * 类型: NEW(新生成), INHERIT(继承)
     */
    private String ruleType;

    /**
     * 前缀 (如 MR-)
     */
    private String prefix;

    /**
     * 日期格式
     */
    private String dateFmt;

    /**
     * 流水号长度
     */
    private Integer seqLen;

    /**
     * 当前流水号值 (最大已用值，分配时累加)
     */
    private Integer currentVal;

    /**
     * 重置周期: DAY(按日)/MONTH(按月)/NEVER(不重置)
     */
    private String resetCycle;

    /**
     * 继承分隔符
     */
    private String separator;

    /**
     * 继承后的分段流水长度
     */
    private Integer inheritSuffixLen;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态 (0:启用, 1:禁用)
     */
    private Integer status;


    /**
     * 乐观锁版本号
     * 用于 BatchRuleServiceImpl 中的 CAS 重试机制
     */
    @Version
    private Integer version;

}
