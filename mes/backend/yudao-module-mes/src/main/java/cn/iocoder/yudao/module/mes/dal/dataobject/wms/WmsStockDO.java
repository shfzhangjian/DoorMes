// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/wms/WmsStockDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.wms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.math.BigDecimal;

/**
 * 实时库存表 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_wms_stock")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsStockDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 物料编码
     */
    private String materialCode;

    /**
     * 物料名称
     */
    private String materialName;

    /**
     * 冗余:物料规格
     */
    private String materialSpec;

    /**
     * 批次号
     */
    private String lotNo;

    /**
     * 仓库编码
     */
    private String warehouseCode;

    /**
     * 库存数量
     * 🚨 架构师红线：强约束使用 BigDecimal，不可使用 Double
     */
    private BigDecimal quantity;

    /**
     * 单位
     */
    private String unit;

    /**
     * 质量状态: PENDING/QUALIFIED/REJECTED
     */
    private String qualityStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 乐观锁版本号
     * 极其重要：防止库存超扣
     */
    @Version
    private Integer version;

}
