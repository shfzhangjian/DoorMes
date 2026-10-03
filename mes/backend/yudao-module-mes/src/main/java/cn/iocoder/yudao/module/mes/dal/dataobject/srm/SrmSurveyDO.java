package cn.iocoder.yudao.module.mes.dal.dataobject.srm;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_srm_survey")
@KeySequence("mes_srm_survey_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSurveyDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String surveyNo;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private Boolean unregisteredSupplier;
    private String supplierSourceType;
    private LocalDate surveyDate;
    private Long surveyLeaderId;
    private String surveyLeaderName;
    private String conclusion;
    private String nature;
    private String registerAddress;
    private LocalDate establishDate;
    private String legalPerson;
    private BigDecimal registeredCapital;
    private String contactName;
    private String contactPhone;
    private BigDecimal area;
    private Integer employeeCount;
    private String industryRank;
    private BigDecimal rdRatio;
    private BigDecimal qaRatio;
    private String annualCapacity;
    private String mainBrand;
    private BigDecimal coopYears;
    private String capitalScale;
    private String agentDelivery;
    private BigDecimal score;
    private String status;
    private Long applicantId;
    private String applicantName;
    private LocalDateTime applyTime;
    private String extraJson;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
