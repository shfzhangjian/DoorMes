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

@TableName("mes_srm_sample_request")
@KeySequence("mes_srm_sample_request_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SrmSampleRequestDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String requestNo;
    private String materialName;
    private String materialModel;
    private String applyType;
    private String applyDept;
    private String usedProduct;
    private BigDecimal requireQty;
    /**
     * 样品评价送检次数累计
     */
    private Integer sampleEvaluationCount;
    private Long applicantId;
    private String applicantName;
    private LocalDate applyDate;
    private LocalDate requireDate;
    private String specifiedSupplierType;
    private Long supplierId;
    private String supplierCode;
    private String supplierName;
    private String technicalRequirement;
    private String purchaseDifficulty;
    private String rdSampleNecessity;
    private String oaApprovalUrl;
    private String status;
    private String currentNodeName;
    private String processInstanceId;
    private Long projectLeaderUserId;
    private String projectLeaderUserName;
    private String projectLeaderOpinion;
    private LocalDateTime projectLeaderHandleTime;
    private Long purchaseOwnerUserId;
    private String purchaseOwnerUserName;
    private String purchaseOwnerOpinion;
    private LocalDateTime purchaseOwnerHandleTime;
    private Long finalApproverUserId;
    private String finalApproverUserName;
    private String finalApproverOpinion;
    private LocalDateTime finalApproverHandleTime;
    private String archiveOpinion;
    private LocalDateTime archiveTime;
    private String remark;

    @Version
    private Integer version;

    private Long tenantId;

}
