package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * NCR 处置对象范围快照。
 */
@TableName("mes_qms_nc_disposition_scope")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcDispositionScopeDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;
    private String executionNo;
    private Long ncRecordId;
    private String ncNo;
    private String dispositionType;
    private String scopeLevel;
    private String objectKey;
    private String motherBatchNo;
    private String segmentBatchNo;
    private String pieceNo;
    private String sourceObjectType;
    private Long sourceObjectId;
    private String sourceObjectNo;
    private String scopeRole;
    private BigDecimal quantity;
    private String executionResult;
    private String targetWorkstationCode;
    private String remark;
    private Long tenantId;
}
