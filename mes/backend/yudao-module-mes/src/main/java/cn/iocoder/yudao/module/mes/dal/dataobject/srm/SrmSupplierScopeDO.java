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
 * 供应商名录管理范围 DO
 */
@TableName("mes_srm_supplier_scope")
@KeySequence("mes_srm_supplier_scope_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierScopeDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 范围编号
     */
    private String scopeCode;

    /**
     * 分组名称
     */
    private String scopeName;

    /**
     * 状态：ENABLED/DISABLED
     */
    private String status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
