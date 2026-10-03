// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/trace/TraceabilityRecordDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.trace;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 全链路批次追溯谱系 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_traceability_record")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceabilityRecordDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联主工单ID
     */
    private Long workOrderId;

    /**
     * 关联派工细单ID
     */
    private Long subOrderId;

    /**
     * 动作(CONSUME消耗, PRODUCE产出, SPLIT拆批, MERGE合批)
     */
    private String actionType;

    /**
     * 投入/父批次号
     */
    private String inputLotNo;

    /**
     * 产出/子批次号
     */
    private String outputLotNo;

    /**
     * 投入数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal inputQty;

    /**
     * 产出数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal outputQty;

}
