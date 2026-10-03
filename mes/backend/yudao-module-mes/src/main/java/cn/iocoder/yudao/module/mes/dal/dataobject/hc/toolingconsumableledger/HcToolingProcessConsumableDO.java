package cn.iocoder.yudao.module.mes.dal.dataobject.hc.toolingconsumableledger;

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

@TableName("mes_md_tooling_process_consumable")
@KeySequence("mes_md_tooling_process_consumable_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcToolingProcessConsumableDO extends BaseDO {

    @TableId
    private Long id;

    private String processCode;
    private String processName;
    private String consumableType;
    private String consumableTypeName;
    private String defaultErpMaterialCode;
    private String defaultBatchNo;
    private Long defaultUomId;
    private String defaultUomCode;
    private String defaultUomName;
    private Integer status;
    private String remark;
    private Long tenantId;
}
