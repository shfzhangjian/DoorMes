package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_review_year_plan")
@KeySequence("mes_srm_supplier_review_year_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierReviewYearPlanDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String planNo;
    private Integer planYear;
    private String planTitle;
    private String completionSummary;
    private String preparedDept;
    private String preparedBy;
    private String confirmedBy;
    private String approvedBy;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
