// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/workorder/MesWorkOrderActionDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.workorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 生产工单动作执行实绩表 DO
 * 对应数据库表: mes_work_order_action
 */
@TableName(value = "mes_work_order_action", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesWorkOrderActionDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    // ================== 1. 核心关联 (Link IDs) ==================

    /**
     * 关联主工单ID (Grandparent Context)
     */
    private Long workOrderId;

    /**
     * 关联派工细单ID (Execution Context)
     */
    private Long subOrderId;

    // ----- 工艺结构三层 ID (Route -> Process -> Action) -----

    /**
     * [L1] 关联工艺路线ID (Route)
     */
    private Long routeId;

    /**
     * [L2] 关联工艺路线-工序节点ID (RouteNode) [新增]
     * 对应 mes_route_process.id
     */
    private Long routeProcessId;

    /**
     * [L2] 关联标准工序ID (Process) [新增]
     * 对应 mes_process.id，用于跨工艺路线统计同一工序的通过率
     */
    private Long processId;

    /**
     * [L3] 关联动作定义ID (Action Template)
     * 指向 mes_route_process_action.id
     */
    private Long routeProcessActionId;

    // ------------------------------------------------------

    /**
     * 实际执行工位ID (Physical Station)
     */
    private Long stationId;

    // ================== 2. 反范式冗余 (Snapshot) ==================

    private String workOrderNo;
    private String subOrderNo;

    // 工艺快照
    private String routeCode;
    private String routeName;

    // 工序快照
    private String processCode;
    private String processName;

    // 工位快照
    private String stationCode;
    private String stationName;

    // ================== 3. 动作定义快照 (Schema Snapshot) ==================

    private String actionCode;
    private String actionName;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> actionConfig;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dataMapping;

    // ================== 4. 执行实绩 (Execution Model) ==================

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> actionValue;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> originalData;

    @TableField("is_pass")
    private Boolean pass;

    private String failureReason;

    // ================== 5. 审计信息 ==================

    private String executorUser;
    private String executorName;
    private LocalDateTime executeTime;
    private String remark;
}
