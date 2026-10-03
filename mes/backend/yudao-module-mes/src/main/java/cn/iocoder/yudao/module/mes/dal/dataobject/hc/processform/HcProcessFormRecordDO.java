package cn.iocoder.yudao.module.mes.dal.dataobject.hc.processform;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_hc_process_form_record")
@KeySequence("mes_hc_process_form_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HcProcessFormRecordDO extends BaseDO {

    @TableId
    private Long id;

    private String recordNo;
    private Long templateId;
    private Long versionId;
    private String templateCode;
    private String templateName;
    private String processCode;
    private String processName;
    private String modelCode;
    private String modelName;
    private String formType;
    private String formTypeName;
    private LocalDate recordDate;
    private Long planId;
    private String planNo;
    private Long planOperationId;
    private String batchNo;
    private Long equipmentId;
    private String equipmentCode;
    private String equipmentName;
    private String recordStatus;
    private String resultStatus;
    private Long fillUserId;
    private String fillUserName;
    private LocalDateTime fillTime;
    private Long confirmUserId;
    private String confirmUserName;
    private LocalDateTime confirmTime;
    private String headerDataJson;
    private String contextJson;
    private String remark;
    private Long tenantId;
}
