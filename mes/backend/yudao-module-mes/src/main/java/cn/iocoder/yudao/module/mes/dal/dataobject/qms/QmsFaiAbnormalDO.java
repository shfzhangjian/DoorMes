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

@TableName("mes_qms_fai_abnormal")
@KeySequence("mes_qms_fai_abnormal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsFaiAbnormalDO extends BaseDO {

    @TableId
    private Long id;

    private Long faiId;

    private String faiNo;

    private Long faiItemId;

    private Long sampleId;

    private String abnormalRole;

    private String defectCode;

    private String defectName;

    private String abnormalDesc;

    private String processStatus;

    private String actionRequired;

    private Long ncRecordId;

    private Long tenantId;
}
