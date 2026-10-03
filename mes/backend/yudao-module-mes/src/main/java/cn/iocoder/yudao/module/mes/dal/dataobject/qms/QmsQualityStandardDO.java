package cn.iocoder.yudao.module.mes.dal.dataobject.qms;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@TableName("mes_qms_quality_standard")
@KeySequence("mes_qms_quality_standard_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsQualityStandardDO extends BaseDO {

    @TableId
    private Long id;

    private String standardNo;

    private String standardName;

    private String glueBoardModel;

    private Long materialId;

    private String materialCode;

    private String materialName;

    private String specification;

    private Long productModelId;

    private String productModelCode;

    private String productModelName;

    private String prodType;

    private String prodTypeName;

    private Long processId;

    private String processCode;

    private String processName;

    private String version;

    private String applyType;

    private Integer status;

    private Integer auditStatus;

    private Long auditorId;

    private String auditorName;

    private LocalDateTime auditTime;

    private LocalDateTime auditNotifyTime;

    private String remark;

    private Long tenantId;
}
