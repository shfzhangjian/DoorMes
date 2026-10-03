package cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess;

import lombok.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工序投料清单 DO
 * 对应表: mes_route_process_input
 */
@TableName("mes_route_process_input")
@KeySequence("mes_route_process_input_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteProcessInputDO extends BaseDO {

    @TableId
    private Long id;

    /** 工艺路线工序ID */
    private Long routeProcessId;

    /** 物料ID */
    private Long materialId;
    /** 物料编码 (冗余) */
    private String materialCode;
    /** 物料名称 (冗余) */
    private String materialName;

    /** 标准单位用量 */
    private BigDecimal standardQty;
    /** 单位 */
    private String unit;
    /** 工序级损耗率(%) */
    private BigDecimal lossRate;

    /** 备注 */
    private String remark;
}
