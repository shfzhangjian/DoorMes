// 文件路径: src/main/java/cn/iocoder/yudao/module/mes/dal/dataobject/prod/ProdFeedDO.java
package cn.iocoder.yudao.module.mes.dal.dataobject.prod;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 生产投料 DO
 *
 * @author 资深后端架构智能体
 */
@TableName("mes_prod_feed")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdFeedDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联排产ID
     */
    private Long scheduleId;

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
     * 投料批次号
     */
    private String lotNo;

    /**
     * 投料数量
     * 🚨 架构师红线：强约束使用 BigDecimal
     */
    private BigDecimal feedQty;

    /**
     * 冗余:单位
     */
    private String unit;

    /**
     * 投料时间
     */
    private LocalDateTime feedTime;

    /**
     * 操作员
     */
    private String operator;

    /**
     * 备注
     */
    private String remark;

    private Long tenantId;

}
