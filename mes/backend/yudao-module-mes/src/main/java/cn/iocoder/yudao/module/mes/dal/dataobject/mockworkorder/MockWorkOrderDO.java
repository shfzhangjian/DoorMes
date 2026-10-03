package cn.iocoder.yudao.module.mes.dal.dataobject.mockworkorder;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 模拟生产工单表（用于AI大模型MCP调用测试） DO
 *
 * @author 演示管理员
 */
@TableName("mes_mock_work_order")
@KeySequence("mes_mock_work_order_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockWorkOrderDO extends BaseDO {

    /**
     * 工单流水号
     */
    @TableId
    private Long id;
    /**
     * 生产工单号
     */
    private String orderNo;
    /**
     * 产品名称
     */
    private String productName;
    /**
     * 排产数量
     */
    private Integer quantity;
    /**
     * 工单状态
     *
     * 枚举 {@link TODO mes_order_status 对应的类}
     */
    private String status;
    /**
     * 备注
     */
    private String remark;


}
