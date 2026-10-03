package cn.iocoder.yudao.module.mes.dal.dataobject.plan;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("mes_plan")
@KeySequence("mes_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanDO extends BaseDO {

    @TableId
    private Long id;
    /** 计划单号(PLN-xxx) */
    private String planNo;
    /** 来源: SALE(订单)/STOCK(备库) */
    private String fromSource;
    /** 关联销售订单ID */
    private Long sourceId;
    /** 产品ID */
    private Long productId;
    /** 产品编码(冗余) */
    private String productCode;
    /** 产品名称(冗余) */
    private String productName;
    /** 选用BOM版本 */
    private Long bomId;
    /** 选用工艺路线 */
    private Long routeId;
    /** 计划生产数量 */
    private BigDecimal quantity;
    /** 计划开始日期 */
    private LocalDate startDate;
    /** 计划结束日期 */
    private LocalDate endDate;
    /** 优先级 */
    private Integer priority;
    /** 状态: CREATED/RELEASED/COMPLETED */
    private String status;
    /** 备注 */
    private String remark;
    /** 排序 */
    private Integer sort;
    /** 租户编号 */
    private Long tenantId;
}
