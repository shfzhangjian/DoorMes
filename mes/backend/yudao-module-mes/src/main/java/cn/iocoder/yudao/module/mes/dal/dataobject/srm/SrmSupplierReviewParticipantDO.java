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

@TableName("mes_srm_supplier_review_participant")
@KeySequence("mes_srm_supplier_review_participant_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierReviewParticipantDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long yearPlanId;
    private Long lineId;
    private Long monthPlanId;
    private Integer planYear;
    private Integer planMonth;
    private String relationType;
    private Long userId;
    private String userName;
    private String deptName;
    private Long tenantId;

}
