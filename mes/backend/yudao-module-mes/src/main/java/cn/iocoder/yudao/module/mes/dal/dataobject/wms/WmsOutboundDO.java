// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/wms/WmsOutboundDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.wms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * 出库/领料单 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_wms_outbound")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsOutboundDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 出库单号
     */
    private String outboundNo;

    /**
     * 类型: PRODUCTION(领料)/SALE(销售出库)
     */
    private String type;

    /**
     * 关联工单号
     */
    private String workOrderNo;

    /**
     * 物料ID
     */
    private Long materialId;

    /**
     * 冗余:物料编码
     */
    private String materialCode;

    /**
     * 冗余:物料名称
     */
    private String materialName;

    /**
     * 冗余:物料规格
     */
    private String materialSpec;

    /**
     * 出库数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal qty;

    /**
     * 冗余:单位
     */
    private String unit;

    /**
     * 状态: DONE
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
