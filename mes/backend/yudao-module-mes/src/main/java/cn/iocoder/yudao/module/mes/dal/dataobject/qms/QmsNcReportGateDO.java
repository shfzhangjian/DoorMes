package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NCR 报工准入门禁。
 */
@TableName("mes_qms_nc_report_gate")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcReportGateDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;
    private String executionNo;
    private Long ncRecordId;
    private String ncNo;
    private String dispositionType;
    private String scopeLevel;
    private String objectKey;
    private String ngProcessCode;
    private String targetWorkstationCode;
    private String gateStatus;
    private String effectMode;
    private Boolean concessionFlag;
    private LocalDateTime effectiveTime;
    private Long tenantId;
}
