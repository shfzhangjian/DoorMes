package cn.iocoder.yudao.module.mes.dal.dataobject.hc.productmodel;

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

@TableName("mes_md_product_model_material")
@KeySequence("mes_md_product_model_material_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProductModelMaterialDO extends BaseDO {

    @TableId
    private Long id;

    private Long modelId;
    private String modelCode;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private Boolean isDefault;
    private String remark;
    private Long tenantId;

}
