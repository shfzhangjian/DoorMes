// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.dal.dataobject.bom.BomDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.bom;

import lombok.*;
import java.util.*;
import java.time.LocalDateTime;
import java.math.BigDecimal; // 🚨 架构师补齐：引入高精度数值类型
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工艺BOM主表 DO
 *
 * @author 资深后端架构智能体 (安全升级自 演示管理员)
 */
@TableName("mes_bom")
@KeySequence("mes_bom_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 所属物料
     */
    private Long productId;
    /**
     * 父件物料编码
     */
    private String productCode;
    /**
     * 父件物料名称
     */
    private String productName;
    /**
     * 父件规格型号
     */
    private String productSpec;
    /**
     * 父件单位
     */
    private String productUnit;
    /**
     * 版本号
     */
    private String version;

    /**
     * 🚨 架构师红线新增：基准产出数量 (核心配比基石)
     */
    private BigDecimal baseQuantity;

    /**
     * 🚨 架构师红线新增：基准产出单位
     */
    private String baseUnit;

    /**
     * 是否当前版本
     * 🚨 架构师红线防呆：强行剥离 is_ 前缀，显式指定底层字段名
     */
    @TableField("is_active")
    private Boolean active;

    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     */
    private Integer status;

}
