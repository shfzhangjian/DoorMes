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
 * NCR 发往工序报工工作台的指令。
 */
@TableName("mes_qms_nc_workstation_command")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsNcWorkstationCommandDO extends BaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String commandNo;
    private Long executionId;
    private String executionNo;
    private Long ncRecordId;
    private String ncNo;
    private String dispositionType;
    private String commandType;
    private String targetWorkstationCode;
    private String effectMode;
    private String scopeSnapshotJson;
    private String commandStatus;
    private Integer commandVersion;
    private String idempotencyKey;
    private LocalDateTime dispatchTime;
    private LocalDateTime ackTime;
    private String ackUser;
    private String ackMessage;
    private LocalDateTime applyTime;
    private String applyResult;
    private Long tenantId;
}
