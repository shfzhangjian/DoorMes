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

@TableName("mes_qms_fqc_abnormal")
@KeySequence("mes_qms_fqc_abnormal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFqcAbnormalDO extends BaseDO {

    @TableId
    private Long id;

    private Long fqcId;
    private String fqcNo;
    private Long fqcItemId;
    private Long sampleId;
    private String abnormalRole;
    private String defectCode;
    private String defectName;
    private String abnormalDesc;
    private String processStatus;
    private String actionRequired;
    private Long ncRecordId;
    private String relatedNcrNo;
    private String dispositionStatus;
    private Long tenantId;
}
