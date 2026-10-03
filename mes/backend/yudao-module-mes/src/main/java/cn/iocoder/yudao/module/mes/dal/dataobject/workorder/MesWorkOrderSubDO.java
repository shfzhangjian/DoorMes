package cn.iocoder.yudao.module.mes.dal.dataobject.workorder;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工序排程与实绩 DO
 * 对应表: mes_work_order_sub
 */
@TableName("mes_work_order_sub")
@KeySequence("mes_work_order_sub_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesWorkOrderSubDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 派工细单号
     */
    private String subOrderNo;

    /**
     * 工单ID
     */
    private Long workOrderId;
    /**
     * 冗余:工单号
     */
    private String workOrderNo;

    // ========== 产品冗余字段 ==========
    private Long productId;
    private String productCode;
    private String productName;
    private String productSpec;

    // ========== 工艺路径信息 ==========
    /**
     * 所属工艺路线-工序ID
     */
    private Long routeProcessId;

    private Long routeId;
    private String routeCode; // 冗余

    private Long processId;
    private String processCode; // 冗余
    private String processName; // 冗余

    private Integer seqNo;

    // ========== 资源工位 ==========
    private Long stationId;
    private String stationName;
    private String stationCode;

    // ========== 计划信息 ==========
    private LocalDate planDate;
    private LocalDateTime startTime; // 计划开始
    private LocalDateTime endTime;   // 计划结束
    private BigDecimal planQty;

    // ========== 实绩信息 ==========
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private BigDecimal actualQty;
    private BigDecimal goodQty;
    private BigDecimal scrapQty;
    private BigDecimal sampleQty;

    private String operatorUser;

    /**
     * 状态: PENDING, DOING, SUSPENDED, FROZEN, DONE
     */
    private String status;

    /**
     * 锁定原因 (QTIME/QUALITY/EQUIPMENT)
     * 用于增强可观测性
     */
    private String lockedBy;

    /**
     * 乐观锁版本号
     * 防止多人同时操作同一工序单导致数据覆盖
     */
    @Version
    private Integer version;

    private String suspendReason;

    private String remark;
    private Long tenantId;
}
