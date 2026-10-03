package cn.iocoder.yudao.module.mes.dal.dataobject.process;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工序-可用工位关联 DO
 * 对应表: mes_process_station
 */
@TableName("mes_process_station")
@KeySequence("mes_process_station_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessStationDO extends BaseDO {

    @TableId
    private Long id;

    /** 工序ID */
    private Long processId;

    /** 工位/设备ID */
    private Long stationId;
    /** 工位编码 (冗余) */
    private String stationCode;
    /** 工位名称 (冗余) */
    private String stationName;

    /**
     * 是否默认工位
     * 数据库: is_default
     */
    @TableField("is_default")
    private Boolean defaultStatus;

    /** 显示顺序 */
    private Integer sort;
    /** 状态 */
    private Integer status;
    /** 备注 */
    private String remark;
}
