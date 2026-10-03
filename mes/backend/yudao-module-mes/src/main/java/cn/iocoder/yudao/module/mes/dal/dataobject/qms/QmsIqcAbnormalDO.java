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

@TableName("mes_qms_iqc_abnormal")
@KeySequence("mes_qms_iqc_abnormal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsIqcAbnormalDO extends BaseDO {

    @TableId
    private Long id;

    private Long iqcId;

    private String iqcNo;

    private Long iqcItemId;

    private Long sampleId;

    private String sampleBarcode;

    private String defectCode;

    private String defectName;

    private String abnormalDesc;

    private String suggestedFlow;

    private String processStatus;

    private Long ncRecordId;

    private Long tenantId;
}
