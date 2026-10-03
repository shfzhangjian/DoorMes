package cn.iocoder.yudao.module.mes.dal.dataobject.unit;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * MES计量单位 DO
 *
 * @author 演示管理员
 */
@TableName("mes_unit")
@KeySequence("mes_unit_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnitDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 单位符号
     */
    private String code;
    /**
     * 单位名称
     */
    private String name;
    /**
     * 维度
     *
     */
    private String category;
    /**
     * 基准单位
     */
    @TableField("is_base")
    private Boolean base;
    /**
     * 换算率
     */
    private BigDecimal ratio;
    /**
     * 保留小数位数
     */
    @TableField("`precision`")
    private Integer precision;
    /**
     * 状态
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;


}
