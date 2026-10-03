package cn.iocoder.yudao.module.mes.dal.dataobject.saleorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 销售订单 DO
 */
@TableName("mes_sale_order")
@KeySequence("mes_sale_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesSaleOrderDO extends BaseDO {

    @TableId
    private Long id;
    /** 订单编号 */
    private String orderNo;
    /** 客户名称 */
    private String customerName;
    /** 产品ID */
    private Long productId;
    /** 产品编码(冗余) */
    private String productCode;
    /** 产品名称(冗余) */
    private String productName;
    /** 订单数量 */
    private BigDecimal quantity;
    /** 单位 */
    private String unit;
    /** 交货日期 */
    private LocalDate deliveryDate;
    /** 状态: PENDING/PLANNED/CLOSED */
    private String status;
    /** 备注 */
    private String remark;
    /** 排序 */
    private Integer sort;
    /** 租户编号 */
    private Long tenantId;
}
