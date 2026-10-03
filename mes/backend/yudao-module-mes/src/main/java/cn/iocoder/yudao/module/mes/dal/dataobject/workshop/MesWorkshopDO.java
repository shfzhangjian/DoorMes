package cn.iocoder.yudao.module.mes.dal.dataobject.workshop;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * MES车间产线定义 DO
 *
 * @author 演示管理员
 */
@TableName("mes_workshop")
@KeySequence("mes_workshop_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesWorkshopDO extends BaseDO {

    public static final Long PARENT_ID_ROOT = 0L;

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 父节点ID
     */
    private Long parentId;
    /**
     * 编号
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 节点类型
     *
     */
    private Integer type;
    /**
     * 负责人
     */
    private String manager;
    /**
     * 面积(㎡)
     */
    private BigDecimal area;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态
     */
    private Integer status;


}
