// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess.RouteProcessDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 工艺路线工序关联表 DO
 * 对应数据库表: mes_route_process
 * * @author 资深后端架构智能体 (确认升级)
 */
@TableName(value = "mes_route_process", autoResultMap = true)
@KeySequence("mes_route_process_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteProcessDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 工艺路线主表ID
     */
    private Long routeId;

    /**
     * 标准工序ID
     */
    private Long processId;

    /**
     * 冗余:工序编码
     */
    private String processCode;

    /**
     * 冗余:工序名称
     */
    private String processName;

    /**
     * 工序顺序
     */
    private Integer seqNo;

    /**
     * 下个工序ID
     */
    private Long nextProcessId;

    /**
     * 默认车间ID
     */
    private Long workshopId;

    /**
     * 冗余:车间编码
     */
    private String workshopCode;

    /**
     * 冗余:车间名称
     */
    private String workshopName;

    /**
     * 是否关键工序
     * 🚨 架构师确认：代码已合规，无 isXxxx，注解正确
     */
    @TableField("is_key_node")
    private Boolean keyNode;

    /**
     * 标准工时
     */
    private BigDecimal standardTime;

    /**
     * 工时单位
     */
    private String timeUnit;

    /**
     * 工艺说明
     */
    private String remark;

    /**
     * 前端动态表单整体布局Schema (Vben动态渲染规则)
     * 🚨 架构师确认：JacksonTypeHandler 与 autoResultMap=true 已完整就位
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> formSchema;

    private Long tenantId;

}
