// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.dal.dataobject.process.ProcessDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.process;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

/**
 * MES标准工序 DO
 *
 * @author 资深后端架构智能体 (升级自 演示管理员)
 */
@TableName("mes_process")
@KeySequence("mes_process_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 工序编码
     */
    private String code;

    /**
     * 工序名称
     */
    private String name;

    /**
     * 默认车间ID
     */
    private Long workshopId;

    /**
     * 车间编码
     */
    private String workshopCode;

    /**
     * 车间名称
     */
    private String workshopName;

    /**
     * 绑定工位
     * 🚨 架构师红线注入：强行剥离 is_ 前缀，显式映射数据库字段 is_bind_station
     */
    @TableField("is_bind_station")
    private Boolean bindStation;

    /**
     * 工序类型
     */
    private String processType;

    /**
     * 备注
     */
    private String remark;

    /**
     * 状态
     */
    private Integer status;

    private Long tenantId;

}
