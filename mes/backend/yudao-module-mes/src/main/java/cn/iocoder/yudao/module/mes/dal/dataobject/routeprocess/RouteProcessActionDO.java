// 文件路径: backend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/routeprocess/RouteProcessActionDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.util.Map;

/**
 * 工序 SOP 动作定义表 DO
 * 对应表: mes_route_process_action
 */
@TableName(value = "mes_route_process_action", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteProcessActionDO extends BaseDO {

    @TableId
    private Long id;

    // ================== 核心关联 (Core Links) ==================

    /**
     * [L1] 关联工艺路线ID (Grandparent) [新增]
     * 作用：快速查询某条工艺路线下的所有动作
     */
    private Long routeId;

    /**
     * [L2] 关联工艺路线-工序节点ID (Parent)
     */
    private Long routeProcessId;

    /**
     * [L2] 关联标准工序ID (Context) [新增]
     * 作用：快速分析某个标准工序（如“涂布”）在不同工艺路线中的配置差异
     */
    private Long processId;

    // ================== 反范式快照 (Snapshot) ==================

    /**
     * 标准工序编码 (冗余)
     */
    private String processCode;

    /**
     * 标准工序名称 (冗余)
     */
    private String processName;

    // ================== 动作定义 (Definition) ==================

    /**
     * 动作代码 (ACT_HASH)
     */
    private String actionCode;

    /**
     * 动作名称 (如: 涂台工艺-参数记录)
     */
    private String actionName;

    /**
     * 触发时机: PRE_CHECK, DURING_PROCESS, POST_CHECK
     */
    private String triggerMoment;

    /**
     * 是否强制: 1=是
     */
    @TableField("is_mandatory")
    private Boolean mandatory;

    /**
     * 异常策略: BLOCK, WARN
     */
    private String errorStrategy;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 动作配置 (Vben Schema)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> actionConfig;

    /**
     * 数据映射 (Target Table/Field)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> dataMapping;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
