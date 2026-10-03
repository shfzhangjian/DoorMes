package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_ipqc_order")
@KeySequence("mes_qms_ipqc_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsIpqcOrderDO extends BaseDO {

    @TableId
    private Long id;

    private String ipqcNo;
    private String workOrderNo;
    private Long planOrderId;
    private String operationCode;
    private String operationName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String specification;
    private String inspectionType;
    private Long standardId;
    private String standardNo;
    private String standardVersion;
    private String status;
    private String judgment;
    private LocalDateTime scheduledTime;
    private LocalDateTime inspectionTime;
    private LocalDateTime nextInspectionTime;
    private Long inspectorId;
    private String inspectorName;
    private String controlAction;
    private String machineControlResult;
    private Long ncRecordId;
    private String remark;
    private Long tenantId;
}
