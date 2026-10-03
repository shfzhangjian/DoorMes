// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/route/QtimeRuleDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.route;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 工艺Q-Time约束规则 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_qtime_rule")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QtimeRuleDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联工艺路线ID
     */
    private Long routeId;

    /**
     * 起始工序ID
     */
    private Long fromProcessId;

    /**
     * 目标工序ID
     */
    private Long toProcessId;

    /**
     * 约束(MAX_STAY最大停滞, MIN_WAIT最小静置)
     */
    private String constraintType;

    /**
     * 时间阈值
     * 🚨 架构师红线：强约束使用 BigDecimal 防止精度丢失
     */
    private BigDecimal thresholdValue;

    /**
     * 时间单位(MINUTE, HOUR, DAY)
     */
    private String timeUnit;

    /**
     * 违规动作(BLOCK拦截, SCRAP报废, ALARM预警)
     */
    private String violationAction;

}
