package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NCR 处置执行单。
 *
 * <p>一张 NCR 只允许生成一张执行单，最终处置类型也只允许一个。</p>
 */
@TableName("mes_qms_nc_disposition_execution")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcDispositionExecutionDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String executionNo;
    private Long ncRecordId;
    private String ncNo;
    private String dispositionType;
    private String ngProcessCode;
    private String ngProcessName;
    private String targetWorkstationCode;
    private String targetWorkstationName;
    private String scopeLevel;
    private BigDecimal affectedQty;
    private BigDecimal selectedQty;
    private BigDecimal derivedScrapQty;
    private String executionStatus;
    private Long confirmUserId;
    private String confirmUserName;
    private LocalDateTime confirmTime;
    private Long executionUserId;
    private String executionUserName;
    private String remark;
    private String extraJson;
    private Long tenantId;
}
