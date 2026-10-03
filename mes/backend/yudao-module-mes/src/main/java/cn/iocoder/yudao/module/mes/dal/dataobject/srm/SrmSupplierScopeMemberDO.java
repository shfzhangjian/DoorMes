package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 供应商名录范围成员 DO
 */
@TableName("mes_srm_supplier_scope_member")
@KeySequence("mes_srm_supplier_scope_member_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierScopeMemberDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long scopeId;

    private Long userId;

    private String userName;

    /**
     * 供应商权限：MASKED 脱敏查看、FULL 查看全部字段、EDIT 可以编辑
     */
    private String permissionLevel;

    private Long tenantId;

}
