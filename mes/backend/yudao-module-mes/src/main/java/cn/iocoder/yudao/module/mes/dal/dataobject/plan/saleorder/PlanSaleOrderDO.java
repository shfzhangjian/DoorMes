package cn.iocoder.yudao.module.mes.dal.dataobject.plan.saleorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_plan_sale_order")
@KeySequence("mes_plan_sale_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanSaleOrderDO extends BaseDO {

    @TableId
    private Long id;

    private String orderNo;
    private String erpNo;
    private String orderLineNo;
    private Long customerId;
    private String customerName;
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String modelCode;
    private String sizeSpec;
    private String sizeName;
    private BigDecimal quantity;
    private BigDecimal plannedQty;
    private BigDecimal remainQty;
    private Long unitId;
    private String unitCode;
    private String unitName;
    private String unit;
    private LocalDate deliveryDate;
    private String status;
    private String statusName;
    private Long auditorId;
    private String auditorName;
    private LocalDateTime auditTime;
    private String remark;
    private Long tenantId;

}
