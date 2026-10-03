package cn.iocoder.yudao.module.mes.dal.dataobject.workorder;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 生产工单表 DO
 * 对应表: mes_work_order
 */
@TableName("mes_work_order")
@KeySequence("mes_work_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesWorkOrderDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 工单编号
     */
    private String workOrderNo;

    /**
     * 工单类型: MTS, MTO, NPI, URG, REP
     */
    private String orderType;

    /**
     * 关联生产计划ID
     */
    private Long planId;
    /**
     * 关联生产计划编号（冗余）
     */
    private String planNo;

    /**
     * 关联销售订单ID (仅MTO模式)
     */
    private Long saleOrderId;
    /**
     * 关联销售订单编号 (仅MTO模式)-冗余
     */
    private String saleOrderNo;

    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 产品编码（冗余）
     */
    private String productCode;
    /**
     * 产品名称（冗余）
     */
    private String productName;
    /**
     * 产品规格（冗余）
     */
    private String productSpec;

    /**
     * 主生产车间ID
     */
    private Long workshopId;
    /**
     * 主生产车间名称（冗余）
     */
    private String workshopName;

    /**
     * 工艺路线ID
     */
    private Long routeId;
    /**
     * 工艺路线编码（冗余）
     */
    private String routeCode;
    /**
     * 工艺路线名称（冗余）
     */
    private String routeName;

    /**
     * 生产批次号 (Lot Number)
     */
    private String lotNo;

    /**
     * 排产数量
     */
    private BigDecimal quantity;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 要求完成日期
     */
    private LocalDate requestDate;

    /**
     * 状态: PENDING/DOING/DONE/CLOSE
     */
    private String status;

    /**
     * 排程优先级
     */
    private Integer priority;

    /**
     * 实际开工时间
     */
    private LocalDateTime realStartTime;
    /**
     * 实际完工时间
     */
    private LocalDateTime realEndTime;

    private String remark;
    private Integer sort;
    private Long tenantId;

    /**
     * 预占流水号-始
     */
    private Integer seqStart;
    /**
     * 预占流水号-止
     */
    private Integer seqEnd;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer version;
}
