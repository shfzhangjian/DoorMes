// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/tooling/ToolingLedgerDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.tooling;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * 工装模具台账与寿命管理 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_tooling_ledger")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolingLedgerDO extends BaseDO {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工装治具编号
     */
    private String toolingCode;

    /**
     * 治具名称
     */
    private String toolingName;

    /**
     * 最大使用寿命(次/小时)
     */
    private Integer maxLifeTimes;

    /**
     * 已使用寿命
     */
    private Integer usedLifeTimes;

    /**
     * 状态(IDLE空闲, IN_USE使用中, REPAIR修磨中, SCRAP报废)
     */
    private String status;

}
