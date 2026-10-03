package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_review_status_log")
@KeySequence("mes_srm_supplier_review_status_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierReviewStatusLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long yearPlanId;
    private Long lineId;
    private Long monthPlanId;
    private Integer planYear;
    private Integer planMonth;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private String updateDescription;
    private Long operatorUserId;
    private String operatorUserName;
    private Long tenantId;

}
