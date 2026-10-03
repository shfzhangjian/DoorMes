package cn.iocoder.yudao.module.mes.dal.dataobject.routeprocess;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工序SOP作业指导书 DO
 * 对应表: mes_route_process_sop
 */
@TableName("mes_route_process_sop")
@KeySequence("mes_route_process_sop_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteProcessSopDO extends BaseDO {

    @TableId
    private Long id;

    /** 工艺路线工序ID */
    private Long routeProcessId;

    /** 文件编号 */
    private String docCode;
    /** 文件名称 */
    private String docName;
    /** 文件路径/URL */
    private String docUrl;
    /** 版本 */
    private String version;

    /**
     * 是否关键文件(强制阅读)
     * 数据库: is_critical
     */
    @TableField("is_critical")
    private Boolean critical;
}
