package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_review_month_plan")
@KeySequence("mes_srm_supplier_review_month_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierReviewMonthPlanDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long yearPlanId;
    private Long lineId;
    private Integer planYear;
    private Integer planMonth;
    private Boolean plannedFlag;
    private String executionStatus;
    private String planDesc;
    private Long leadUserId;
    private String leadUserName;
    private String relatedUserIds;
    private String relatedUserNames;
    private LocalDate auditDate;
    /** 审核类别：认证审核/年度审核/不定期审核（字典 mes_srm_site_inspection_audit_category） */
    private String auditCategory;
    /** 审核说明 */
    private String auditDesc;
    /** 审核附件（现场考察资料快照说明） */
    private String auditAttachment;
    private Long approverUserId;
    private String approverUserName;
    private String approvalOpinion;
    private LocalDateTime approvalTime;
    private String statusRemark;
    private String updateDescription;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
