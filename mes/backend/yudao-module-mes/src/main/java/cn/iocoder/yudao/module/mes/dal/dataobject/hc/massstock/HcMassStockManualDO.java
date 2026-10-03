package cn.iocoder.yudao.module.mes.dal.dataobject.hc.massstock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_pp_mass_stock_manual")
@KeySequence("mes_pp_mass_stock_manual_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcMassStockManualDO extends BaseDO {

    @TableId
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 型号 */
    private String modelCode;

    /** 母卷批号 */
    private String motherBatchNo;

    /** 母卷段号 */
    private String motherSegmentBatchNo;

    /** SEM结果 */
    private String semResult;

    /** 备注 */
    private String remark;

}
