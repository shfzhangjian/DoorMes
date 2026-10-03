package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName(value = "mes_qms_nc_restore_log", autoResultMap = true)
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcRestoreLogDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long ncRecordId;
    private String ncNo;
    private String documentType;
    private String sourceBizType;
    private Long sourceObjectId;
    private String sourceObjectNo;
    private Long sourceIqcId;
    private String sourceIqcNo;
    private String originalStatus;
    private String processInstanceIds;
    private String restoreReason;
    private Long restoreUserId;
    private String restoreUserName;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> restoreSummary;

    private Long tenantId;
}
