// 完整路径: cn.iocoder.yudao.module.mes.dal.dataobject.routeprocessparam.RouteProcessParamDO
package cn.iocoder.yudao.module.mes.dal.dataobject.routeprocessparam;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * MES工艺路线参数表 DO (配方)
 * 对应表: mes_route_process_param
 */
@TableName("mes_route_process_param")
@KeySequence("mes_route_process_param_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteProcessParamDO extends BaseDO {

    @TableId
    private Long id;
    /** 子表ID (核心外键) */
    private Long routeProcessId;
    /** 主表ID (冗余外键，方便查询) */
    private Long routeId;
    /** 工序ID (冗余外键) */
    private Long processId;

    /** 参数编码 */
    private String paramCode;
    /** 参数名称 */
    private String paramName;
    /** 参数类型 */
    private String paramType;

    /** 标准值 */
    private String standardValue;
    /** 下限 */
    private BigDecimal minValue;
    /** 上限 */
    private BigDecimal maxValue;
    /** 单位 */
    private String unit;

    /** * 是否关键质控点
     * 数据库字段: is_critical
     */
    @TableField("is_critical")
    private Boolean critical;

    /** 备注 */
    private String remark;
}
