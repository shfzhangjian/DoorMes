package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@TableName("mes_qms_inspection_standard_switch_log")
@KeySequence("mes_qms_inspection_standard_switch_log_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsInspectionStandardSwitchLogDO extends BaseDO {

    @TableId
    private Long id;

    private String inspectionType;

    private Long inspectionId;

    private String inspectionNo;

    private Long oldStandardId;

    private String oldStandardNo;

    private String oldContentHash;

    private Long newStandardId;

    private String newStandardNo;

    private String newContentHash;

    private String oldSnapshotJson;

    private String switchReason;

    private Long operatorId;

    private String operatorName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime switchTime;

    private Long tenantId;
}
