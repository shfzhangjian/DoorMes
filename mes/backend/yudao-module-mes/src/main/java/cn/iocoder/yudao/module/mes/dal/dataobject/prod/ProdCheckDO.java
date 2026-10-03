// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/prod/ProdCheckDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.prod;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 生产检查 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_prod_check")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdCheckDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联排产ID
     */
    private Long scheduleId;

    /**
     * 检查类型(PRE_CHECK/SELF_CHECK/IPQC)
     */
    private String checkType;

    /**
     * 检查项目
     */
    private String checkItem;

    /**
     * 标准值
     */
    private String standardValue;

    /**
     * 实测值
     */
    private String actualValue;

    /**
     * 是否合格(1:是 0:否)
     * 🚨 架构师红线：强行纠偏 is_pass，Java 字段名为 pass
     */
    @TableField("is_pass")
    private Boolean pass;

    /**
     * 检查时间
     */
    private LocalDateTime checkTime;

    /**
     * 检查人
     */
    private String checkUser;

    /**
     * 备注
     */
    private String remark;

}
