// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/wms/WmsInboundDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.wms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 入库单(兼检验通知单) DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_wms_inbound")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsInboundDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 入库单号
     */
    private String inboundNo;

    /**
     * 类型: PURCHASE(采购)/PROD(生产完工)
     */
    private String type;

    /**
     * 来源单号
     */
    private String sourceNo;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 冗余:物料编码
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
     * 入库批次号
     */
    private String lotNo;

    /**
     * 计划数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal planQty;

    /**
     * 实际数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal actualQty;

    /**
     * 冗余:单位
     */
    private String unit;

    /**
     * 状态: CREATED/INSPECTING/DONE
     */
    private String status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer sort;

}
