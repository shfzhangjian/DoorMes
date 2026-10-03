// 文件路径: src.main.java.cn.iocoder.yudao.module.mes.dal.dataobject.bomitem.BomItemDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.bomitem;

import lombok.*;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * 工艺BOM子项 DO
 *
 * @author 资深后端架构智能体 (安全升级自 演示管理员)
 */
@TableName("mes_bom_item")
@KeySequence("mes_bom_item_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BomItemDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * BOM主表ID
     */
    private Long bomId;
    /**
     * 组件物料ID (原材料/半成品)
     */
    private Long materialId;
    /**
     * 组件物料编码
     */
    private String materialCode;
    /**
     * 组件物料名称
     */
    private String materialName;
    /**
     * 组件规格型号
     */
    private String materialSpec;
    /**
     * 组件单位
     */
    private String materialUnit;

    /**
     * 🚨 架构师纠偏：标准消耗量(基于基准产出)
     * 已将错误的 quantity 替换为对齐底层 DDL 的 stdQuantity
     */
    private BigDecimal stdQuantity;

    /**
     * 🚨 架构师红线新增：配比分子
     */
    private BigDecimal numerator;

    /**
     * 🚨 架构师红线新增：配比分母
     */
    private BigDecimal denominator;

    /**
     * 损耗率(%)
     */
    private BigDecimal wastageRate;
    /**
     * 备注
     */
    private String remark;

}
