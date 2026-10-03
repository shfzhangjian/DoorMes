// 文件路径: cn.iocoder.yudao.module.mes.dal.dataobject.material.MesMaterialDO
package cn.iocoder.yudao.module.mes.dal.dataobject.material;

import lombok.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * MES物料主数据 DO
 *
 * @author 资深测试开发工程师 (Fix)
 */
@TableName("mes_material")
@KeySequence("mes_material_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesMaterialDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;
    /**
     * 物料编码 (ERP码)
     */
    private String code;
    /**
     * 物料名称
     */
    private String name;
    /**
     * 物料分类（物料分类 (raw:原材料, semi:半成品, finish:成品, tool:辅料)）
     * 字典: mes_material_category
     */
    private String category;

    /**
     * 物料来源
     * 字典: mes_material_source (MAKE:自制, BUY:采购, OUTSOURCE:委外)
     */
    private String materialSource;

    /**
     * 材质牌号
     */
    private String materialGrade;
    /**
     * 规格型号
     */
    private String spec;
    /**
     * 计量单位
     */
    private String unit;
    /**
     * 单重 (kg)
     */
    private BigDecimal unitWeight;
    /**
     * 理论废品率 (%)
     */
    private BigDecimal scrapRate;

    /**
     * 图纸/规范附件路径
     */
    private String drawingUrl;

    /**
     * 采购/生产提前期(天)
     */
    private Integer leadTime;

    /**
     * 默认供应商关联ID
     */
    private Long supplierId;

    /**
     * 默认供应商名称(冗余)
     */
    private String supplierName;

    /**
     * 备注
     */
    private String remark;
    /**
     * 状态
     */
    private Integer status;

    private Long tenantId;
}
