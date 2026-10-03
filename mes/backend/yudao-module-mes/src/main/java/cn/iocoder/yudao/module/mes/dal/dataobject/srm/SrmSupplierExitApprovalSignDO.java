package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_exit_approval_sign")
@KeySequence("mes_srm_supplier_exit_approval_sign_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierExitApprovalSignDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long applyId;
    private String deptCode;
    private String deptName;
    private Long userId;
    private String userName;
    private String signStatus;
    private String signResult;
    private String signOpinion;
    private LocalDateTime signTime;
    private Long tenantId;

}
