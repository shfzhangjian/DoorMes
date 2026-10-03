// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/job/JobActionHistoryDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.job;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 动态SOP作业执行历史 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_job_action_history")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobActionHistoryDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联派工细单ID
     */
    private Long subOrderId;

    /**
     * 关联SOP动作ID
     */
    private Long actionId;

    /**
     * 打卡执行人
     */
    private String operatorUser;

    /**
     * 实际执行时间
     */
    private LocalDateTime executeTime;

    /**
     * 实际采集结果(读数/扫码值)
     */
    private String collectValue;

    /**
     * 合规标识(1:合规, 0:异常)
     * 🚨 架构师红线：强行纠偏 is_compliant，Java 字段名为 compliant
     */
    @TableField("is_compliant")
    private Boolean compliant;

}
