package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 供应商字段脱敏配置 DO
 */
@TableName("mes_srm_supplier_mask_field")
@KeySequence("mes_srm_supplier_mask_field_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierMaskFieldDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String fieldKey;

    private String fieldLabel;

    private Boolean maskEnabled;

    private Integer sort;

    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
