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

@TableName("mes_sfc_operation_report_defect")
@KeySequence("mes_sfc_operation_report_defect_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProcessReportDefectDO extends BaseDO {

    @TableId
    private Long id;

    private Long tenantId;
    private Long reportId;
    private Long planId;
    private Long planOperationId;
    private String defectCode;
    private String defectName;
    private BigDecimal defectQty;
    private String remark;
}
