package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_supplier_review_plan_line")
@KeySequence("mes_srm_supplier_review_plan_line_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSupplierReviewPlanLineDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long yearPlanId;
    private Integer planYear;
    private Integer rowNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String contactPerson;
    private String materialCode;
    private String materialName;
    private String model;
    private String applicableProduct;
    private String providedProduct;
    private String completionStatus;
    private LocalDate latestAuditDate;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
