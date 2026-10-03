package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processreport;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_sfc_wet_report_abnormal_position")
@KeySequence("mes_sfc_wet_report_abnormal_position_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcWetReportAbnormalPositionDO extends BaseDO {

    @TableId
    private Long id;

    private Long operationReportId;
    private Long planId;
    private Long planOperationId;
    private String planNo;
    private String sourceMenuCode;
    private String processStage;
    private String operationCode;
    private String operationName;
    private String batchNo;
    private String productionBatchNo;
    private String sourcePlanNo;
    private String sourceRowUid;
    private Long sourceDetailId;
    private String positionText;
    private BigDecimal abnormalLength;
    private String remark;
    private Integer sortOrder;
    private Long tenantId;
}
